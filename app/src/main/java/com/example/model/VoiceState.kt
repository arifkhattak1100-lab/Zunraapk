package com.example.model

enum class VoiceState(
    val displayName: String
) {
    IDLE("Ready"),
    LISTENING("Listening..."),
    THINKING("Thinking..."),
    SPEAKING("Speaking"),
    INTERRUPTED("Interrupted"),
    ERROR("Connection Error")
}
