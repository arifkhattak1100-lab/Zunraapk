package com.example.model

data class MemoryItem(
    val id: String,
    val category: String, // "Preference", "Fact", "Instruction", "Routine"
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)
