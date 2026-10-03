package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AssistantMode
import com.example.model.Emotion
import com.example.model.ToolStatus
import com.example.service.AndroidActionManager
import com.example.service.MemoryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Zornia", appName)
    }

    @Test
    fun `emotion detection detects emotional cues correctly`() {
        val happyEmotion = Emotion.detectFromText("I am so happy and had an awesome day!")
        assertEquals(Emotion.HAPPY, happyEmotion)

        val stressedEmotion = Emotion.detectFromText("I am under so much stress with this deadline")
        assertEquals(Emotion.STRESSED, stressedEmotion)

        val sadEmotion = Emotion.detectFromText("I had a miserable and sad day")
        assertEquals(Emotion.SAD, sadEmotion)

        val neutralEmotion = Emotion.detectFromText("What is the speed of light?")
        assertEquals(Emotion.NEUTRAL, neutralEmotion)
    }

    @Test
    fun `memory manager saves and retrieves items and prevents secret storage`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val memoryManager = MemoryManager(context)

        // Save preference
        val added = memoryManager.addMemory("User Preference", "Preferred Name", "Alex")
        assertTrue(added)

        val memories = memoryManager.memories.value
        val alexItem = memories.find { it.key == "Preferred Name" }
        assertNotNull(alexItem)
        assertEquals("Alex", alexItem?.value)

        // Sensitive password security check
        val blocked = memoryManager.addMemory("Secret", "User Password", "super_secret_123")
        assertFalse("Passwords and secrets must never be saved to memory", blocked)

        // Delete test
        alexItem?.id?.let { id ->
            memoryManager.deleteMemory(id)
            val updated = memoryManager.memories.value
            assertTrue(updated.none { it.id == id })
        }
    }

    @Test
    fun `android action manager handles safe battery and timer tools`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val actionManager = AndroidActionManager(context)

        val batteryResult = actionManager.executeTool("get_battery_status", emptyMap())
        assertEquals(ToolStatus.SUCCESS, batteryResult.status)
        assertTrue(batteryResult.userFriendlyMessage.isNotEmpty())

        val timerResult = actionManager.executeTool("set_timer", mapOf("seconds" to "120", "label" to "Tea"))
        assertNotNull(timerResult)
    }
}
