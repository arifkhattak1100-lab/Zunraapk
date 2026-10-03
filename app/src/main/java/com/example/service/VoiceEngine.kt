package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.VoiceConfig
import com.example.model.VoiceState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class VoiceEngine(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val tag = "VoiceEngine"

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _rmsAmplitude = MutableStateFlow(0f)
    val rmsAmplitude: StateFlow<Float> = _rmsAmplitude.asStateFlow()

    private val _userTranscriptFlow = MutableSharedFlow<String>(replay = 0)
    val userTranscriptFlow: SharedFlow<String> = _userTranscriptFlow.asSharedFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _speakingFinished = MutableSharedFlow<Unit>(replay = 0)
    val speakingFinished: SharedFlow<Unit> = _speakingFinished.asSharedFlow()

    private var currentConfig = VoiceConfig()
    private var speakingAmplitudeJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        initTts()
    }

    private fun initTts() {
        try {
            textToSpeech = TextToSpeech(context) { status ->
                try {
                    if (status == TextToSpeech.SUCCESS) {
                        isTtsInitialized = true
                        applyVoiceConfig(currentConfig)
                        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                _voiceState.value = VoiceState.SPEAKING
                                startSpeakingAmplitudeSimulation()
                            }

                            override fun onDone(utteranceId: String?) {
                                stopSpeakingAmplitude()
                                _voiceState.value = VoiceState.IDLE
                                coroutineScope.launch {
                                    _speakingFinished.emit(Unit)
                                }
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                stopSpeakingAmplitude()
                                _voiceState.value = VoiceState.IDLE
                            }

                            override fun onError(utteranceId: String?, errorCode: Int) {
                                stopSpeakingAmplitude()
                                _voiceState.value = VoiceState.IDLE
                            }
                        })
                    } else {
                        Log.w(tag, "TTS Initialization status: $status")
                    }
                } catch (e: Throwable) {
                    Log.w(tag, "Error setting up TTS listener: ${e.message}")
                }
            }
        } catch (e: Throwable) {
            Log.w(tag, "TextToSpeech constructor error: ${e.message}")
        }
    }

    fun updateConfig(config: VoiceConfig) {
        currentConfig = config
        if (isTtsInitialized) {
            applyVoiceConfig(config)
        }
    }

    private fun applyVoiceConfig(config: VoiceConfig) {
        try {
            textToSpeech?.let { tts ->
                tts.setPitch(config.pitch)
                tts.setSpeechRate(config.speed)

                // Select an expressive adult female voice if available on the device
                try {
                    val availableVoices = tts.voices
                    if (!availableVoices.isNullOrEmpty()) {
                        val matchingVoice = availableVoices.firstOrNull { voice ->
                            val name = voice.name.lowercase()
                            voice.locale.language == "en" &&
                                    (name.contains("female") || name.contains("en-us-x-sfg") || name.contains("woman"))
                        } ?: availableVoices.firstOrNull { it.locale.language == "en" }
                        matchingVoice?.let { tts.voice = it }
                    }
                } catch (e: Throwable) {
                    tts.language = Locale.US
                }
            }
        } catch (e: Throwable) {
            Log.w(tag, "Error applying voice config: ${e.message}")
        }
    }

    fun startListening() {
        mainHandler.post {
            // Stop any ongoing speech playback immediately (Barge-In)
            stopSpeaking()

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _voiceState.value = VoiceState.ERROR
                return@post
            }

            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentConfig.speechRecognitionLanguage)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                _partialTranscript.value = ""
                _voiceState.value = VoiceState.LISTENING
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(tag, "Failed to start listening", e)
                _voiceState.value = VoiceState.IDLE
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(tag, "Error stopping listener", e)
            }
            if (_voiceState.value == VoiceState.LISTENING) {
                _voiceState.value = VoiceState.IDLE
            }
            _rmsAmplitude.value = 0f
        }
    }

    /**
     * Immediate Barge-in: stops audio playback instantly when user taps or speaks.
     */
    fun stopSpeaking() {
        stopSpeakingAmplitude()
        textToSpeech?.stop()
        if (_voiceState.value == VoiceState.SPEAKING) {
            _voiceState.value = VoiceState.INTERRUPTED
            mainHandler.postDelayed({
                if (_voiceState.value == VoiceState.INTERRUPTED) {
                    _voiceState.value = VoiceState.IDLE
                }
            }, 300)
        }
    }

    fun speak(text: String, isAcknowledgement: Boolean = false) {
        if (!isTtsInitialized || text.isBlank()) return

        mainHandler.post {
            // If already speaking and this is not a short acknowledgement, replace playback
            textToSpeech?.stop()
            _voiceState.value = VoiceState.SPEAKING

            val cleanText = cleanTextForSpeech(text)
            val utteranceId = "zornia_utt_${System.currentTimeMillis()}"
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            textToSpeech?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
    }

    private fun cleanTextForSpeech(text: String): String {
        // Strip markdown asterisks, bold markers, brackets, code blocks for ultra-natural voice delivery
        return text
            .replace(Regex("```[\\s\\S]*?```"), "Code omitted.")
            .replace(Regex("`.*?`"), "")
            .replace(Regex("[*#_~]"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .trim()
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _voiceState.value = VoiceState.LISTENING
            }

            override fun onBeginningOfSpeech() {
                // Instant barge-in: user began speaking! Stop any ongoing assistant output
                stopSpeaking()
                _voiceState.value = VoiceState.LISTENING
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Normalize rmsdB (-2 to ~10 dB) to 0.0 .. 1.0
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _rmsAmplitude.value = normalized
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _voiceState.value = VoiceState.THINKING
                _rmsAmplitude.value = 0f
            }

            override fun onError(error: Int) {
                _rmsAmplitude.value = 0f
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that. Could you repeat?"
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network issue while listening."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                    else -> null
                }
                Log.d(tag, "SpeechRecognizer error: $error ($errorMessage)")
                _voiceState.value = VoiceState.IDLE
            }

            override fun onResults(results: Bundle?) {
                _rmsAmplitude.value = 0f
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull()?.trim() ?: ""
                _partialTranscript.value = ""

                if (recognizedText.isNotEmpty()) {
                    coroutineScope.launch {
                        _userTranscriptFlow.emit(recognizedText)
                    }
                } else {
                    _voiceState.value = VoiceState.IDLE
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()?.trim() ?: ""
                if (partial.isNotEmpty()) {
                    _partialTranscript.value = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun startSpeakingAmplitudeSimulation() {
        speakingAmplitudeJob?.cancel()
        speakingAmplitudeJob = coroutineScope.launch(Dispatchers.Default) {
            var phase = 0f
            while (isActive && _voiceState.value == VoiceState.SPEAKING) {
                phase += 0.35f
                val amp = (Math.sin(phase.toDouble()).toFloat() * 0.4f + 0.5f).coerceIn(0.1f, 0.95f)
                _rmsAmplitude.value = amp
                delay(60)
            }
        }
    }

    private fun stopSpeakingAmplitude() {
        speakingAmplitudeJob?.cancel()
        speakingAmplitudeJob = null
        _rmsAmplitude.value = 0f
    }

    fun destroy() {
        stopSpeakingAmplitude()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        speechRecognizer?.destroy()
    }
}
