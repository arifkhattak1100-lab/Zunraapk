package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AssistantMode
import com.example.model.VoiceState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CompanionGlow
import com.example.ui.theme.CompanionPrimary
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanNeonLight
import com.example.ui.theme.RoseNeon
import com.example.ui.theme.VioletElectric
import com.example.ui.theme.VioletGlow
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OrbVisualizer(
    voiceState: VoiceState,
    mode: AssistantMode,
    rmsAmplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbInfinite")

    // Continuous smooth rotation for orbital elements
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Breathing pulse for idle & thinking
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbBreathing"
    )

    // Amplitude smoothing animatable
    val animatedAmp = remember { Animatable(0f) }
    LaunchedEffect(rmsAmplitude) {
        animatedAmp.animateTo(
            targetValue = rmsAmplitude,
            animationSpec = tween(durationMillis = 100, easing = LinearEasing)
        )
    }

    // Determine primary & secondary colors based on Mode
    val isCompanion = mode == AssistantMode.COMPANION
    val primaryColor = if (isCompanion) CompanionPrimary else CyanNeon
    val secondaryColor = if (isCompanion) CompanionGlow else VioletElectric
    val accentColor = if (isCompanion) RoseNeon else VioletGlow

    Box(
        modifier = modifier
            .size(size)
            .testTag("ai_voice_orb")
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.58f

            val ampBonus = animatedAmp.value * 28f
            val currentRadius = when (voiceState) {
                VoiceState.LISTENING -> (baseRadius * 1.05f) + ampBonus
                VoiceState.SPEAKING -> (baseRadius * 1.02f) + ampBonus
                VoiceState.THINKING -> baseRadius * breathingPulse
                VoiceState.IDLE -> baseRadius * breathingPulse
                VoiceState.INTERRUPTED -> baseRadius * 0.95f
                VoiceState.ERROR -> baseRadius
            }

            // 1. Outermost Ambient Glow Ring
            val outerGlowRadius = currentRadius * 1.45f
            val outerColor = when (voiceState) {
                VoiceState.ERROR -> AmberWarning.copy(alpha = 0.15f)
                VoiceState.LISTENING -> primaryColor.copy(alpha = 0.35f + (animatedAmp.value * 0.3f))
                VoiceState.SPEAKING -> secondaryColor.copy(alpha = 0.30f + (animatedAmp.value * 0.3f))
                else -> primaryColor.copy(alpha = 0.18f)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(outerColor, Color.Transparent),
                    center = centerOffset,
                    radius = outerGlowRadius
                ),
                radius = outerGlowRadius,
                center = centerOffset
            )

            // 2. Ripple frequency rings for Listening & Speaking states
            if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) {
                for (i in 1..3) {
                    val rippleRadius = currentRadius + (i * 14f) + (animatedAmp.value * i * 10f)
                    val rippleAlpha = (0.6f / i) * (0.4f + animatedAmp.value * 0.6f)
                    drawCircle(
                        color = if (voiceState == VoiceState.LISTENING) primaryColor.copy(alpha = rippleAlpha) else secondaryColor.copy(alpha = rippleAlpha),
                        radius = rippleRadius,
                        center = centerOffset,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // 3. Orbital Rotating Particle Rings
            val ringRadius = currentRadius * 1.18f
            val rotRad = Math.toRadians(rotation.toDouble())
            drawCircle(
                color = secondaryColor.copy(alpha = 0.35f),
                radius = ringRadius,
                center = centerOffset,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Rotating orbital particle dots
            val numDots = 4
            for (i in 0 until numDots) {
                val angle = rotRad + (i * (2 * Math.PI / numDots))
                val dotX = centerOffset.x + (ringRadius * cos(angle)).toFloat()
                val dotY = centerOffset.y + (ringRadius * sin(angle)).toFloat()
                drawCircle(
                    color = primaryColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }

            // 4. Central Holographic Core Gradient
            val coreColors = when (voiceState) {
                VoiceState.ERROR -> listOf(AmberWarning, Color(0xFFB45309), Color(0xFF1E1005))
                VoiceState.THINKING -> listOf(CyanNeonLight, secondaryColor, accentColor, Color(0xFF070B16))
                VoiceState.SPEAKING -> listOf(Color.White, accentColor, primaryColor, Color(0xFF0F081C))
                VoiceState.LISTENING -> listOf(Color.White, primaryColor, CyanNeonLight, Color(0xFF051221))
                else -> listOf(CyanNeonLight, primaryColor, secondaryColor, Color(0xFF070C18))
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = coreColors,
                    center = centerOffset.copy(
                        x = centerOffset.x - (currentRadius * 0.2f),
                        y = centerOffset.y - (currentRadius * 0.25f)
                    ),
                    radius = currentRadius
                ),
                radius = currentRadius,
                center = centerOffset
            )

            // 5. Specular highlight inner shimmer
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = currentRadius * 0.22f,
                center = centerOffset.copy(
                    x = centerOffset.x - (currentRadius * 0.35f),
                    y = centerOffset.y - (currentRadius * 0.35f)
                )
            )
        }
    }
}
