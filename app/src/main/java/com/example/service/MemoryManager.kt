package com.example.service

import android.content.Context
import android.content.SharedPreferences
import com.example.model.MemoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class MemoryManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("zornia_memory", Context.MODE_PRIVATE)
    private val memoryKey = "stored_memories"
    private val enabledKey = "memory_enabled"

    private val _memories = MutableStateFlow<List<MemoryItem>>(emptyList())
    val memories: StateFlow<List<MemoryItem>> = _memories.asStateFlow()

    private val _isMemoryEnabled = MutableStateFlow(prefs.getBoolean(enabledKey, true))
    val isMemoryEnabled: StateFlow<Boolean> = _isMemoryEnabled.asStateFlow()

    init {
        loadMemories()
    }

    fun setMemoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(enabledKey, enabled).apply()
        _isMemoryEnabled.value = enabled
    }

    private fun loadMemories() {
        val rawJson = prefs.getString(memoryKey, null)
        if (rawJson.isNullOrBlank()) {
            // Seed a few initial friendly starter preferences
            val defaultList = listOf(
                MemoryItem(
                    id = "pref_assistant_name",
                    category = "Assistant Preference",
                    key = "Assistant Name",
                    value = "Zornia"
                ),
                MemoryItem(
                    id = "pref_greeting_style",
                    category = "User Preference",
                    key = "Conversation Style",
                    value = "Natural, warm, fast, and supportive"
                )
            )
            saveMemoriesInternal(defaultList)
            _memories.value = defaultList
            return
        }

        try {
            val jsonArray = JSONArray(rawJson)
            val items = mutableListOf<MemoryItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                items.add(
                    MemoryItem(
                        id = obj.getString("id"),
                        category = obj.optString("category", "General"),
                        key = obj.getString("key"),
                        value = obj.getString("value"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            _memories.value = items
        } catch (e: Exception) {
            _memories.value = emptyList()
        }
    }

    fun addMemory(category: String, key: String, value: String): Boolean {
        if (!_isMemoryEnabled.value) return false

        // Security check: Never store passwords, tokens, or secret keys
        val combined = "$key $value".lowercase()
        val forbidden = listOf("password", "token", "api_key", "secret", "cvv", "credit card", "private key")
        if (forbidden.any { combined.contains(it) }) {
            return false
        }

        val current = _memories.value.toMutableList()
        // If key already exists in same category, update it
        val existingIndex = current.indexOfFirst { it.category.equals(category, true) && it.key.equals(key, true) }
        val newItem = MemoryItem(
            id = java.util.UUID.randomUUID().toString(),
            category = category,
            key = key.trim(),
            value = value.trim()
        )

        if (existingIndex >= 0) {
            current[existingIndex] = newItem
        } else {
            current.add(newItem)
        }

        saveMemoriesInternal(current)
        _memories.value = current
        return true
    }

    fun deleteMemory(id: String) {
        val updated = _memories.value.filterNot { it.id == id }
        saveMemoriesInternal(updated)
        _memories.value = updated
    }

    fun clearAllMemories() {
        saveMemoriesInternal(emptyList())
        _memories.value = emptyList()
    }

    fun getMemorySummaryPrompt(): String {
        if (!_isMemoryEnabled.value || _memories.value.isEmpty()) return ""
        val sb = StringBuilder("Known User Context & Preferences:\n")
        _memories.value.forEach { item ->
            sb.append("- [${item.category}] ${item.key}: ${item.value}\n")
        }
        return sb.toString().trim()
    }

    private fun saveMemoriesInternal(list: List<MemoryItem>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("category", item.category)
                put("key", item.key)
                put("value", item.value)
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(memoryKey, jsonArray.toString()).apply()
    }
}
