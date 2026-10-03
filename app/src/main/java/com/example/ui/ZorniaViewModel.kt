package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AssistantMode
import com.example.model.ConversationMessage
import com.example.model.Emotion
import com.example.model.MessageRole
import com.example.model.ToolExecution
import com.example.model.ToolStatus
import com.example.model.VoiceConfig
import com.example.model.VoiceState
import com.example.service.AndroidActionManager
import com.example.service.GeminiLiveClient
import com.example.service.GeminiStreamEvent
import com.example.service.MemoryManager
import com.example.service.VoiceEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ZorniaViewModel(application: Application) : AndroidViewModel(application) {

    private val voiceEngine = VoiceEngine(application, viewModelScope)
    private val geminiClient = GeminiLiveClient(application)
    private val actionManager = AndroidActionManager(application)
    val memoryManager = MemoryManager(application)

    private val _messages = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val messages: StateFlow<List<ConversationMessage>> = _messages.asStateFlow()

    private val _currentMode = MutableStateFlow(AssistantMode.NORMAL)
    val currentMode: StateFlow<AssistantMode> = _currentMode.asStateFlow()

    private val _voiceConfig = MutableStateFlow(VoiceConfig())
    val voiceConfig: StateFlow<VoiceConfig> = _voiceConfig.asStateFlow()

    private val _hasMicPermission = MutableStateFlow(false)
    val hasMicPermission: StateFlow<Boolean> = _hasMicPermission.asStateFlow()

    val voiceState: StateFlow<VoiceState> = voiceEngine.voiceState
    val rmsAmplitude: StateFlow<Float> = voiceEngine.rmsAmplitude
    val partialTranscript: StateFlow<String> = voiceEngine.partialTranscript

    private val _isOnline = MutableStateFlow(checkNetworkAvailable())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var currentAiJob: Job? = null
    private var handsFreeJob: Job? = null

    init {
        // Starter welcoming greeting from Zornia
        _messages.value = listOf(
            ConversationMessage(
                role = MessageRole.ZORNIA,
                text = "Hello! I'm Zornia, your voice companion. Tap the orb or speak anytime to begin.",
                emotion = Emotion.HAPPY
            )
        )

        // Observe speech recognition results
        viewModelScope.launch {
            voiceEngine.userTranscriptFlow.collect { text ->
                if (text.isNotBlank()) {
                    handleUserInput(text)
                }
            }
        }

        // Observe when speaking finishes for Hands-Free continuous conversation
        viewModelScope.launch {
            voiceEngine.speakingFinished.collect {
                if (_currentMode.value == AssistantMode.HANDS_FREE && _hasMicPermission.value) {
                    handsFreeJob?.cancel()
                    handsFreeJob = viewModelScope.launch {
                        delay(_voiceConfig.value.autoListenDelayMs)
                        if (voiceEngine.voiceState.value == VoiceState.IDLE) {
                            voiceEngine.startListening()
                        }
                    }
                }
            }
        }
    }

    fun setMicPermissionGranted(granted: Boolean) {
        _hasMicPermission.value = granted
    }

    fun setMode(mode: AssistantMode) {
        _currentMode.value = mode
        val modeAnnouncement = when (mode) {
            AssistantMode.NORMAL -> "Switched to Normal mode."
            AssistantMode.COMPANION -> "Companion mode activated. I'm here for you."
            AssistantMode.FOCUS -> "Focus mode active. Let's get things done."
            AssistantMode.HANDS_FREE -> "Hands-Free conversation enabled."
            AssistantMode.PRIVATE -> "Private mode active. Session will stay ephemeral."
        }
        addSystemMessage(modeAnnouncement)
        voiceEngine.speak(modeAnnouncement)
    }

    fun updateVoiceConfig(config: VoiceConfig) {
        _voiceConfig.value = config
        voiceEngine.updateConfig(config)
        memoryManager.setMemoryEnabled(config.memoryEnabled)
    }

    fun onMicTapped() {
        if (!_hasMicPermission.value) {
            val msg = "I need microphone permission before I can listen."
            addSystemMessage(msg)
            voiceEngine.speak(msg)
            return
        }

        when (voiceEngine.voiceState.value) {
            VoiceState.LISTENING -> {
                voiceEngine.stopListening()
            }
            VoiceState.SPEAKING -> {
                // Instant Barge-In
                interruptSpeaking()
            }
            VoiceState.THINKING -> {
                currentAiJob?.cancel()
                voiceEngine.stopListening()
            }
            else -> {
                voiceEngine.startListening()
            }
        }
    }

    /**
     * Instant Barge-In / Interruption: immediately ceases audio and active thinking
     */
    fun interruptSpeaking() {
        currentAiJob?.cancel()
        currentAiJob = null
        handsFreeJob?.cancel()
        voiceEngine.stopSpeaking()
        addSystemMessage("Audio interrupted.")
    }

    fun handleUserInput(rawText: String) {
        val userText = rawText.trim()
        if (userText.isEmpty()) return

        // 1. Detect emotional cues
        val detectedEmotion = Emotion.detectFromText(userText)

        // 2. Add user message
        val userMsg = ConversationMessage(
            role = MessageRole.USER,
            text = userText,
            emotion = detectedEmotion
        )
        _messages.value = _messages.value + userMsg

        // Check network connection
        _isOnline.value = checkNetworkAvailable()

        // 3. Fast-Response Priority: check for instant local commands or direct acknowledgements
        if (handleImmediateLocalPatterns(userText)) {
            return
        }

        // If offline, gracefully notify
        if (!_isOnline.value) {
            val offlineMsg = "I'm having trouble connecting right now. I'll try again."
            addZorniaMessage(offlineMsg, Emotion.NEUTRAL)
            voiceEngine.speak(offlineMsg)
            return
        }

        // 4. Send to Gemini Live / Streaming
        processAiResponse(userText, detectedEmotion)
    }

    private fun handleImmediateLocalPatterns(input: String): Boolean {
        val lower = input.lowercase().trim()

        // Instant response examples specified in prompt:
        if (lower == "zornia, are you there?" || lower == "are you there?" || lower == "zornia are you there") {
            val reply = "Yes, I'm here. What do you need?"
            addZorniaMessage(reply, Emotion.HAPPY, isAck = true)
            voiceEngine.speak(reply)
            return true
        }

        if (lower == "zornia, help me." || lower == "zornia help me" || lower == "help me") {
            val reply = "I'm here. Tell me what's going on."
            val emo = if (_currentMode.value == AssistantMode.COMPANION) Emotion.SAD else Emotion.STRESSED
            addZorniaMessage(reply, emo, isAck = true)
            voiceEngine.speak(reply)
            return true
        }

        // Direct local app triggers for instant zero-latency launching
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("zornia open ")) {
            val target = lower.removePrefix("zornia open ").removePrefix("open ").removePrefix("launch ").trim()
            if (target.isNotBlank()) {
                val fastAck = "Sure, opening $target."
                voiceEngine.speak(fastAck, isAcknowledgement = true)
                val execution = actionManager.executeTool("open_app", mapOf("app_name" to target))
                addZorniaMessage(fastAck, Emotion.NEUTRAL, execution = execution)
                return true
            }
        }

        if (lower.contains("battery") || lower == "check battery" || lower == "how is the battery") {
            val execution = actionManager.executeTool("get_battery_status", emptyMap())
            val reply = execution.userFriendlyMessage
            addZorniaMessage(reply, Emotion.NEUTRAL, execution = execution)
            voiceEngine.speak(reply)
            return true
        }

        return false
    }

    private fun processAiResponse(userText: String, detectedEmotion: Emotion) {
        currentAiJob?.cancel()
        currentAiJob = viewModelScope.launch {
            // Immediate fast-acknowledgement for complex queries if not companion mode
            if (userText.length > 50 && _currentMode.value != AssistantMode.FOCUS) {
                val quickNod = if (detectedEmotion == Emotion.STRESSED || detectedEmotion == Emotion.SAD) {
                    "I hear you. Let me think with you."
                } else {
                    "On it."
                }
                voiceEngine.speak(quickNod, isAcknowledgement = true)
            }

            var streamingMessageId: String? = null
            val fullTextBuilder = StringBuilder()
            var spokenFirstSentence = false

            val memoryContext = if (_currentMode.value != AssistantMode.PRIVATE) {
                memoryManager.getMemorySummaryPrompt()
            } else ""

            geminiClient.streamGenerate(
                userPrompt = userText,
                history = _messages.value,
                mode = _currentMode.value,
                memoryContext = memoryContext,
                targetModel = _voiceConfig.value.targetModel
            ) { event ->
                when (event) {
                    is GeminiStreamEvent.TextChunk -> {
                        fullTextBuilder.append(event.text)
                        val currentFull = fullTextBuilder.toString()

                        // First sentence fast speech streaming: start speaking as soon as first clause completes
                        if (!spokenFirstSentence && (currentFull.contains(".") || currentFull.contains("!") || currentFull.contains("?"))) {
                            val firstChunk = currentFull.split(Regex("[.!?]")).firstOrNull()?.trim() ?: ""
                            if (firstChunk.isNotBlank()) {
                                spokenFirstSentence = true
                                voiceEngine.speak(firstChunk)
                            }
                        }

                        // Update or insert streaming message in UI
                        if (streamingMessageId == null) {
                            val newMsg = ConversationMessage(
                                role = MessageRole.ZORNIA,
                                text = currentFull,
                                isStreaming = true,
                                emotion = detectedEmotion
                            )
                            streamingMessageId = newMsg.id
                            _messages.value = _messages.value + newMsg
                        } else {
                            _messages.value = _messages.value.map { msg ->
                                if (msg.id == streamingMessageId) {
                                    msg.copy(text = currentFull)
                                } else msg
                            }
                        }
                    }

                    is GeminiStreamEvent.ToolCall -> {
                        val execution = actionManager.executeTool(event.name, event.arguments)
                        val toolResponseText = execution.userFriendlyMessage.ifBlank { "Action completed." }

                        val toolMsg = ConversationMessage(
                            role = MessageRole.TOOL,
                            text = toolResponseText,
                            toolExecution = execution
                        )
                        _messages.value = _messages.value + toolMsg
                        voiceEngine.speak(toolResponseText)
                    }

                    is GeminiStreamEvent.Completed -> {
                        val finalText = event.fullText
                        if (streamingMessageId != null) {
                            _messages.value = _messages.value.map { msg ->
                                if (msg.id == streamingMessageId) {
                                    msg.copy(text = finalText, isStreaming = false)
                                } else msg
                            }
                        } else {
                            addZorniaMessage(finalText, detectedEmotion)
                        }

                        // If we didn't start speaking chunks earlier, speak full text now
                        if (!spokenFirstSentence) {
                            voiceEngine.speak(finalText)
                        }

                        // Auto-extract friendly memory facts if memory enabled and not private
                        if (_voiceConfig.value.memoryEnabled && _currentMode.value != AssistantMode.PRIVATE) {
                            checkForMemoryLearning(userText)
                        }
                    }

                    is GeminiStreamEvent.Error -> {
                        val errMessage = event.message
                        addZorniaMessage(errMessage, Emotion.NEUTRAL)
                        voiceEngine.speak(errMessage)
                    }
                }
            }
        }
    }

    private fun checkForMemoryLearning(userText: String) {
        val lower = userText.lowercase()
        if (lower.startsWith("my name is ") || lower.startsWith("call me ")) {
            val name = userText.substringAfter("is ").substringAfter("me ").trim()
            memoryManager.addMemory("User Preference", "Preferred Name", name)
        } else if (lower.startsWith("remember that ") || lower.startsWith("note that ")) {
            val note = userText.removePrefix("remember that ").removePrefix("note that ").trim()
            memoryManager.addMemory("Personal Note", "User Note", note)
        }
    }

    private fun addZorniaMessage(
        text: String,
        emotion: Emotion? = null,
        execution: ToolExecution? = null,
        isAck: Boolean = false
    ) {
        val msg = ConversationMessage(
            role = MessageRole.ZORNIA,
            text = text,
            emotion = emotion,
            toolExecution = execution,
            isAcknowledgement = isAck
        )
        _messages.value = _messages.value + msg
    }

    private fun addSystemMessage(text: String) {
        val msg = ConversationMessage(
            role = MessageRole.SYSTEM,
            text = text
        )
        _messages.value = _messages.value + msg
    }

    fun clearHistory() {
        _messages.value = listOf(
            ConversationMessage(
                role = MessageRole.ZORNIA,
                text = "Conversation cleared. Ready whenever you are.",
                emotion = Emotion.NEUTRAL
            )
        )
    }

    fun isGeminiConfigured(): Boolean {
        return geminiClient.isConfigured()
    }

    fun getActiveApiKey(): String {
        return geminiClient.getApiKey()
    }

    fun saveCustomApiKey(key: String) {
        geminiClient.setCustomApiKey(key)
    }

    fun clearCustomApiKey() {
        geminiClient.clearCustomApiKey()
    }

    private fun checkNetworkAvailable(): Boolean {
        return try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return true
            val capabilities = cm.getNetworkCapabilities(network) ?: return true
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentAiJob?.cancel()
        handsFreeJob?.cancel()
        voiceEngine.destroy()
    }
}
