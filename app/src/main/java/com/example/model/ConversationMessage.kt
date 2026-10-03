package com.example.model

enum class MessageRole {
    USER,
    ZORNIA,
    SYSTEM,
    TOOL
}

data class ConversationMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: MessageRole,
    val text: String,
    val emotion: Emotion? = null,
    val toolExecution: ToolExecution? = null,
    val isStreaming: Boolean = false,
    val isAcknowledgement: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
