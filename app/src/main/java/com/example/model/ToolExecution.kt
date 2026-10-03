package com.example.model

enum class ToolStatus {
    PENDING,
    EXECUTING,
    SUCCESS,
    FAILED,
    REQUIRES_CONFIRMATION
}

data class ToolExecution(
    val toolName: String,
    val arguments: Map<String, String>,
    val status: ToolStatus = ToolStatus.PENDING,
    val userFriendlyMessage: String = "",
    val errorMessage: String? = null
)
