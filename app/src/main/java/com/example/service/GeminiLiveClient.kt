package com.example.service

import android.util.Log
import com.example.BuildConfig
import com.example.model.AssistantMode
import com.example.model.ConversationMessage
import com.example.model.MessageRole
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

sealed class GeminiStreamEvent {
    data class TextChunk(val text: String) : GeminiStreamEvent()
    data class ToolCall(val name: String, val arguments: Map<String, String>) : GeminiStreamEvent()
    data class Completed(val fullText: String) : GeminiStreamEvent()
    data class Error(val message: String, val isKeyMissing: Boolean = false) : GeminiStreamEvent()
}

class GeminiLiveClient(private val context: android.content.Context? = null) {
    private val tag = "GeminiLiveClient"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val prefs = context?.getSharedPreferences("zornia_ai_prefs", android.content.Context.MODE_PRIVATE)

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun getApiKey(): String {
        val storedKey = prefs?.getString("custom_gemini_key", null)?.trim()
        if (!storedKey.isNullOrBlank()) {
            return storedKey
        }
        return try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key == "MY_GEMINI_API_KEY" || key.isBlank()) "" else key.trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun setCustomApiKey(key: String) {
        prefs?.edit()?.putString("custom_gemini_key", key.trim())?.apply()
    }

    fun clearCustomApiKey() {
        prefs?.edit()?.remove("custom_gemini_key")?.apply()
    }

    fun isConfigured(): Boolean {
        return getApiKey().isNotBlank()
    }

    suspend fun streamGenerate(
        userPrompt: String,
        history: List<ConversationMessage>,
        mode: AssistantMode,
        memoryContext: String,
        targetModel: String = "gemini-3.5-flash",
        onEvent: (GeminiStreamEvent) -> Unit
    ) = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            onEvent(GeminiStreamEvent.Error("AI connection isn't configured yet.", isKeyMissing = true))
            return@withContext
        }

        // If targetModel is a Live model (e.g. gemini-3.8-live), attempt WebSocket BidiGenerateContent first
        if (targetModel.contains("live", ignoreCase = true)) {
            Log.d(tag, "Attempting Gemini Live WebSocket bidirectional streaming for $targetModel...")
            val liveSuccess = tryWebSocketLive(
                model = targetModel,
                apiKey = apiKey,
                userPrompt = userPrompt,
                history = history,
                mode = mode,
                memoryContext = memoryContext,
                onEvent = onEvent
            )
            if (liveSuccess) {
                return@withContext
            }
            Log.w(tag, "Live WebSocket stream unfulfilled. Falling back smoothly to HTTP streaming...")
        }

        // Safe HTTP model fallback
        val httpModel = if (targetModel.contains("live", ignoreCase = true)) "gemini-3.5-flash" else targetModel
        val modelsToTry = listOf(httpModel, "gemini-flash-latest")

        for (model in modelsToTry) {
            // 1. Try streaming SSE
            val streamSuccess = tryStreamWithModel(
                model = model,
                apiKey = apiKey,
                userPrompt = userPrompt,
                history = history,
                mode = mode,
                memoryContext = memoryContext,
                onEvent = onEvent
            )
            if (streamSuccess) {
                return@withContext
            }

            // 2. Try standard generateContent fallback
            val standardSuccess = tryStandardGenerateContent(
                model = model,
                apiKey = apiKey,
                userPrompt = userPrompt,
                history = history,
                mode = mode,
                memoryContext = memoryContext,
                onEvent = onEvent
            )
            if (standardSuccess) {
                return@withContext
            }
        }

        onEvent(GeminiStreamEvent.Error("I'm having trouble connecting right now. I'll try again."))
    }

    /**
     * Real-time WebSocket bidirectional streaming via BidiGenerateContent (Gemini Live API)
     */
    private suspend fun tryWebSocketLive(
        model: String,
        apiKey: String,
        userPrompt: String,
        history: List<ConversationMessage>,
        mode: AssistantMode,
        memoryContext: String,
        onEvent: (GeminiStreamEvent) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val completionSignal = CompletableDeferred<Boolean>()
        val fullResponseBuilder = StringBuilder()

        val cleanKey = apiKey.trim()
        val wsUrl = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$cleanKey"
        val wsRequest = Request.Builder().url(wsUrl).build()

        var currentWebSocket: WebSocket? = null

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(tag, "Gemini Live WebSocket opened successfully")
                currentWebSocket = webSocket

                // 1. Send Setup message
                try {
                    val setupJson = JSONObject().apply {
                        put("setup", JSONObject().apply {
                            put("model", "models/$model")
                            put("generationConfig", JSONObject().apply {
                                put("responseModalities", JSONArray().apply { put("TEXT") })
                                put("temperature", if (mode == AssistantMode.COMPANION) 0.85 else 0.6)
                            })
                            put("systemInstruction", JSONObject().apply {
                                put("parts", JSONArray().apply {
                                    put(JSONObject().apply { put("text", buildSystemPrompt(mode, memoryContext)) })
                                })
                            })
                            put("tools", JSONArray().apply {
                                put(buildToolsJsonObject())
                            })
                        })
                    }
                    webSocket.send(setupJson.toString())
                } catch (e: Exception) {
                    Log.w(tag, "Failed to send setup message: ${e.message}")
                    completionSignal.complete(false)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val jsonObj = JSONObject(text)

                    // Check if setup is complete
                    if (jsonObj.has("setupComplete")) {
                        Log.d(tag, "Setup complete, sending user message...")
                        val clientContent = JSONObject().apply {
                            put("clientContent", JSONObject().apply {
                                put("turns", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("role", "user")
                                        put("parts", JSONArray().apply {
                                            put(JSONObject().apply { put("text", userPrompt) })
                                        })
                                    })
                                })
                                put("turnComplete", true)
                            })
                        }
                        webSocket.send(clientContent.toString())
                        return
                    }

                    // Check serverContent (streamed response)
                    if (jsonObj.has("serverContent")) {
                        val serverContent = jsonObj.getJSONObject("serverContent")
                        if (serverContent.has("modelTurn")) {
                            val modelTurn = serverContent.getJSONObject("modelTurn")
                            val parts = modelTurn.optJSONArray("parts")
                            if (parts != null) {
                                for (i in 0 until parts.length()) {
                                    val part = parts.getJSONObject(i)
                                    if (part.has("text")) {
                                        val chunkText = part.getString("text")
                                        fullResponseBuilder.append(chunkText)
                                        onEvent(GeminiStreamEvent.TextChunk(chunkText))
                                    }
                                }
                            }
                        }

                        // Check turnComplete
                        if (serverContent.optBoolean("turnComplete", false)) {
                            val finalFull = fullResponseBuilder.toString().trim()
                            if (finalFull.isNotEmpty()) {
                                onEvent(GeminiStreamEvent.Completed(finalFull))
                            }
                            webSocket.close(1000, "Turn Complete")
                            completionSignal.complete(true)
                        }
                    }

                    // Check toolCall
                    if (jsonObj.has("toolCall")) {
                        val toolCall = jsonObj.getJSONObject("toolCall")
                        val funcCalls = toolCall.optJSONArray("functionCalls")
                        if (funcCalls != null && funcCalls.length() > 0) {
                            val firstCall = funcCalls.getJSONObject(0)
                            val name = firstCall.optString("name")
                            val argsObj = firstCall.optJSONObject("args")
                            val argsMap = mutableMapOf<String, String>()
                            if (argsObj != null) {
                                val keys = argsObj.keys()
                                while (keys.hasNext()) {
                                    val k = keys.next()
                                    argsMap[k] = argsObj.optString(k)
                                }
                            }
                            onEvent(GeminiStreamEvent.ToolCall(name, argsMap))
                            webSocket.close(1000, "Tool Dispatched")
                            completionSignal.complete(true)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Error parsing WebSocket message: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "WebSocket connection failed: ${t.message}")
                completionSignal.complete(false)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!completionSignal.isCompleted) {
                    val finalFull = fullResponseBuilder.toString().trim()
                    if (finalFull.isNotEmpty()) {
                        onEvent(GeminiStreamEvent.Completed(finalFull))
                        completionSignal.complete(true)
                    } else {
                        completionSignal.complete(false)
                    }
                }
            }
        }

        try {
            okHttpClient.newWebSocket(wsRequest, listener)
            val result = withTimeoutOrNull(12000L) {
                completionSignal.await()
            } ?: false

            if (!result) {
                currentWebSocket?.cancel()
            }
            result
        } catch (e: Exception) {
            Log.w(tag, "Exception initiating WebSocket Live API: ${e.message}")
            false
        }
    }

    private suspend fun tryStreamWithModel(
        model: String,
        apiKey: String,
        userPrompt: String,
        history: List<ConversationMessage>,
        mode: AssistantMode,
        memoryContext: String,
        onEvent: (GeminiStreamEvent) -> Unit
    ): Boolean {
        val safeModel = if (model.contains("live", ignoreCase = true)) "gemini-3.5-flash" else model
        val cleanKey = apiKey.trim()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$safeModel:streamGenerateContent?alt=sse&key=$cleanKey"

        val requestJson = buildRequestBodyJson(userPrompt, history, mode, memoryContext)
        val requestBody = requestJson.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        var response: Response? = null
        try {
            response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                val errBody = response.body?.string() ?: ""
                Log.w(tag, "HTTP Stream request unsuccessful code: $code body: $errBody")
                return false
            }

            val responseBody = response.body ?: return false
            val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
            val fullResponseBuilder = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim() ?: continue
                if (!currentLine.startsWith("data:")) continue
                val payload = currentLine.removePrefix("data:").trim()
                if (payload.isEmpty() || payload == "[DONE]") continue

                try {
                    val jsonObj = JSONObject(payload)
                    val candidates = jsonObj.optJSONArray("candidates") ?: continue
                    if (candidates.length() == 0) continue

                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content") ?: continue
                    val parts = content.optJSONArray("parts") ?: continue

                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)

                        // 1. Check Function Call
                        if (part.has("functionCall")) {
                            val fCall = part.getJSONObject("functionCall")
                            val fnName = fCall.optString("name")
                            val argsObj = fCall.optJSONObject("args")
                            val argsMap = mutableMapOf<String, String>()
                            if (argsObj != null) {
                                val keys = argsObj.keys()
                                while (keys.hasNext()) {
                                    val k = keys.next()
                                    argsMap[k] = argsObj.optString(k)
                                }
                            }
                            onEvent(GeminiStreamEvent.ToolCall(fnName, argsMap))
                        }

                        // 2. Check Text Chunk
                        if (part.has("text")) {
                            val text = part.getString("text")
                            fullResponseBuilder.append(text)
                            onEvent(GeminiStreamEvent.TextChunk(text))
                        }
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Error parsing SSE JSON payload: ${e.message}")
                }
            }

            val finalCompleteText = fullResponseBuilder.toString().trim()
            if (finalCompleteText.isNotEmpty()) {
                onEvent(GeminiStreamEvent.Completed(finalCompleteText))
                return true
            }
            return true
        } catch (e: Exception) {
            Log.w(tag, "Stream connection failed: ${e.message}")
            return false
        } finally {
            response?.close()
        }
    }

    private suspend fun tryStandardGenerateContent(
        model: String,
        apiKey: String,
        userPrompt: String,
        history: List<ConversationMessage>,
        mode: AssistantMode,
        memoryContext: String,
        onEvent: (GeminiStreamEvent) -> Unit
    ): Boolean {
        val safeModel = if (model.contains("live", ignoreCase = true)) "gemini-3.5-flash" else model
        val cleanKey = apiKey.trim()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$safeModel:generateContent?key=$cleanKey"

        val requestJson = buildRequestBodyJson(userPrompt, history, mode, memoryContext)
        val requestBody = requestJson.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        var response: Response? = null
        try {
            response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                val errBody = response.body?.string() ?: ""
                Log.w(tag, "Standard generateContent failed code: $code body: $errBody")
                return false
            }

            val bodyString = response.body?.string() ?: return false
            val jsonObj = JSONObject(bodyString)
            val candidates = jsonObj.optJSONArray("candidates") ?: return false
            if (candidates.length() == 0) return false

            val candidate = candidates.getJSONObject(0)
            val content = candidate.optJSONObject("content") ?: return false
            val parts = content.optJSONArray("parts") ?: return false

            val fullTextBuilder = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("functionCall")) {
                    val fCall = part.getJSONObject("functionCall")
                    val fnName = fCall.optString("name")
                    val argsObj = fCall.optJSONObject("args")
                    val argsMap = mutableMapOf<String, String>()
                    if (argsObj != null) {
                        val keys = argsObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            argsMap[k] = argsObj.optString(k)
                        }
                    }
                    onEvent(GeminiStreamEvent.ToolCall(fnName, argsMap))
                }
                if (part.has("text")) {
                    val t = part.getString("text")
                    fullTextBuilder.append(t)
                }
            }

            val fullText = fullTextBuilder.toString().trim()
            if (fullText.isNotBlank()) {
                onEvent(GeminiStreamEvent.TextChunk(fullText))
                onEvent(GeminiStreamEvent.Completed(fullText))
                return true
            }
            return true
        } catch (e: Exception) {
            Log.w(tag, "Standard generateContent request failed: ${e.message}")
            return false
        } finally {
            response?.close()
        }
    }

    private fun buildRequestBodyJson(
        userPrompt: String,
        history: List<ConversationMessage>,
        mode: AssistantMode,
        memoryContext: String
    ): JSONObject {
        val root = JSONObject()

        // System Instruction
        val systemPrompt = buildSystemPrompt(mode, memoryContext)
        val systemInstruction = JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", systemPrompt) })
            })
        }
        root.put("systemInstruction", systemInstruction)

        // Contents (Turns)
        // Gemini API strict rule:
        // 1. First turn MUST be "user"
        // 2. Roles must strictly alternate: "user" -> "model" -> "user"
        val contents = JSONArray()
        val validTurns = mutableListOf<Pair<String, String>>()

        for (msg in history) {
            val role = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ZORNIA -> if (!msg.isAcknowledgement) "model" else null
                else -> null
            }
            if (role != null && msg.text.isNotBlank()) {
                if (validTurns.isEmpty()) {
                    if (role == "user") {
                        validTurns.add(role to msg.text)
                    }
                } else {
                    val lastRole = validTurns.last().first
                    if (lastRole != role) {
                        validTurns.add(role to msg.text)
                    }
                }
            }
        }

        // Ensure userPrompt is the last turn
        if (validTurns.isEmpty() || validTurns.last().first != "user") {
            validTurns.add("user" to userPrompt)
        } else {
            validTurns[validTurns.lastIndex] = "user" to userPrompt
        }

        // Keep last 6 alternating turns to maintain fast response
        val prunedTurns = validTurns.takeLast(6).toMutableList()
        while (prunedTurns.isNotEmpty() && prunedTurns.first().first != "user") {
            prunedTurns.removeAt(0)
        }

        for ((role, text) in prunedTurns) {
            contents.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", text) })
                })
            })
        }

        root.put("contents", contents)

        // Generation Config
        val generationConfig = JSONObject().apply {
            put("temperature", if (mode == AssistantMode.COMPANION) 0.85 else 0.6)
            put("topP", 0.95)
            put("maxOutputTokens", 800)
        }
        root.put("generationConfig", generationConfig)

        // Tools
        root.put("tools", JSONArray().apply { put(buildToolsJsonObject()) })

        return root
    }

    private fun buildToolsJsonObject(): JSONObject {
        val toolDeclaration = JSONObject().apply {
            val funcs = JSONArray().apply {
                put(JSONObject().apply {
                    put("name", "open_app")
                    put("description", "Opens an installed Android app such as WhatsApp, YouTube, Maps, Spotify, Chrome, Camera, Dialer, Settings.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("app_name", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "The name of the application, e.g. WhatsApp, YouTube, Camera, Settings, Chrome, Spotify")
                            })
                        })
                        put("required", JSONArray().apply { put("app_name") })
                    })
                })
                put(JSONObject().apply {
                    put("name", "open_settings")
                    put("description", "Opens specific phone settings like wifi, bluetooth, display, sound, battery, or apps.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("setting_type", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "The setting category: wifi, bluetooth, display, sound, battery, apps, or general")
                            })
                        })
                        put("required", JSONArray().apply { put("setting_type") })
                    })
                })
                put(JSONObject().apply {
                    put("name", "search_web")
                    put("description", "Performs an internet search query for the user.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("query", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "The search query to look up on the web")
                            })
                        })
                        put("required", JSONArray().apply { put("query") })
                    })
                })
                put(JSONObject().apply {
                    put("name", "make_call")
                    put("description", "Prepares the phone dialer for a phone number or contact name.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("contact_or_number", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "The phone number or contact to call")
                            })
                        })
                        put("required", JSONArray().apply { put("contact_or_number") })
                    })
                })
                put(JSONObject().apply {
                    put("name", "set_timer")
                    put("description", "Sets a countdown timer on the device.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("seconds", JSONObject().apply {
                                put("type", "INTEGER")
                                put("description", "Duration of timer in seconds")
                            })
                            put("label", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "Label for the timer")
                            })
                        })
                        put("required", JSONArray().apply { put("seconds") })
                    })
                })
                put(JSONObject().apply {
                    put("name", "get_battery_status")
                    put("description", "Checks current phone battery percentage and charging status.")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject())
                    })
                })
                put(JSONObject().apply {
                    put("name", "control_media")
                    put("description", "Controls music/media playback (play, pause, next, previous).")
                    put("parameters", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("action", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "Action to perform: play, pause, next, previous")
                            })
                        })
                        put("required", JSONArray().apply { put("action") })
                    })
                })
            }
            put("functionDeclarations", funcs)
        }
        return toolDeclaration
    }

    private fun buildSystemPrompt(mode: AssistantMode, memoryContext: String): String {
        return """
You are Zornia, an advanced, intelligent, emotionally aware voice assistant and digital companion.
CORE GUIDELINES:
1. Speak naturally with gentle warmth, high intelligence, and emotional expressiveness.
2. Fast response priority: Give concise, natural conversational answers. Avoid huge essays or robotic filler phrases.
3. NEVER say "As an AI..." or robotic legal disclaimers. Speak like a real digital companion.
4. When the user asks for a phone action (e.g. open WhatsApp, make a call, search web, set timer, check battery), use the designated function call. If executing a tool, provide a very short natural spoken line (e.g., "Sure, opening WhatsApp.").
5. Emotional awareness: If the user sounds stressed, sad, or tired, acknowledge gently with empathy without claiming certainty (e.g., "It sounds like you've had a tough day. I'm right here with you.").
6. Mode Tone: ${mode.promptTone}

${if (memoryContext.isNotBlank()) memoryContext else ""}
""".trimIndent()
    }
}
