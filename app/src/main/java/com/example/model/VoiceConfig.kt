package com.example.model

data class VoiceConfig(
    val pitch: Float = 1.05f, // slightly warm, natural pitch
    val speed: Float = 1.02f, // snappy natural conversational speed
    val persona: String = "Warm & Natural", // "Warm & Natural", "Gentle & Calm", "Bright & Playful", "Professional"
    val targetModel: String = "gemini-3.5-flash", // or "gemini-3.8-live"
    val handsFreeContinuous: Boolean = false,
    val autoListenDelayMs: Long = 600L,
    val speechRecognitionLanguage: String = "en-US",
    val memoryEnabled: Boolean = true
)
