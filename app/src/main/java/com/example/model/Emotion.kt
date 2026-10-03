package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CompanionPrimary
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GreenActive
import com.example.ui.theme.RoseSoft
import com.example.ui.theme.RoseWarm
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletElectric
import com.example.ui.theme.VioletGlow

enum class Emotion(
    val label: String,
    val color: Color,
    val description: String,
    val empatheticCue: String
) {
    HAPPY("Happy", GreenActive, "Positive and energized", "I'm so glad to hear that!"),
    SAD("Sad", VioletElectric, "Feeling low or sorrowful", "I'm here with you. What's on your mind?"),
    STRESSED("Stressed", AmberWarning, "Overwhelmed or pressured", "Take a slow breath. We can take this step by step."),
    ANGRY("Frustrated", RoseWarm, "Annoyed or agitated", "I understand your frustration. Let's fix this together."),
    CONFUSED("Puzzled", VioletGlow, "Needs clarity or guidance", "Let's break this down simply."),
    EXCITED("Excited", CyanNeon, "Thrilled and eager", "That's wonderful! Tell me more!"),
    LONELY("Reflective", RoseSoft, "Seeking company and warmth", "I'm always glad to talk with you. You're not alone."),
    TIRED("Exhausted", TextSecondary, "Low energy or drained", "You've been doing a lot. Be gentle with yourself today."),
    NEUTRAL("Neutral", CyanNeon, "Balanced conversation", "");

    companion object {
        fun detectFromText(text: String): Emotion {
            val lower = text.lowercase()
            return when {
                lower.contains("happy") || lower.contains("great") || lower.contains("awesome") ||
                        lower.contains("wonderful") || lower.contains("good day") || lower.contains("love this") -> HAPPY
                lower.contains("sad") || lower.contains("crying") || lower.contains("depressed") ||
                        lower.contains("unhappy") || lower.contains("heartbroken") || lower.contains("miserable") -> SAD
                lower.contains("stress") || lower.contains("overwhelm") || lower.contains("anxious") ||
                        lower.contains("panic") || lower.contains("too much work") || lower.contains("deadline") -> STRESSED
                lower.contains("angry") || lower.contains("mad") || lower.contains("hate") ||
                        lower.contains("annoyed") || lower.contains("pissed") || lower.contains("furious") -> ANGRY
                lower.contains("confused") || lower.contains("don't understand") || lower.contains("what do you mean") ||
                        lower.contains("lost") || lower.contains("puzzled") -> CONFUSED
                lower.contains("excited") || lower.contains("can't wait") || lower.contains("thrilled") ||
                        lower.contains("hyped") || lower.contains("yay") || lower.contains("woohoo") -> EXCITED
                lower.contains("lonely") || lower.contains("alone") || lower.contains("nobody") ||
                        lower.contains("miss everyone") -> LONELY
                lower.contains("tired") || lower.contains("exhausted") || lower.contains("sleepy") ||
                        lower.contains("drained") || lower.contains("burnout") -> TIRED
                else -> NEUTRAL
            }
        }
    }
}
