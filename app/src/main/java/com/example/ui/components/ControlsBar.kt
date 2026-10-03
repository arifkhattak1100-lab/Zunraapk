package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantMode
import com.example.model.VoiceState
import com.example.ui.theme.CompanionPrimary
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GreenActive
import com.example.ui.theme.RoseWarm
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletElectric

@Composable
fun ControlsBar(
    voiceState: VoiceState,
    mode: AssistantMode,
    onMicTapped: () -> Unit,
    onInterruptTapped: () -> Unit,
    onSendText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTextInput by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "ControlsPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val isCompanion = mode == AssistantMode.COMPANION
    val activeAccent = if (isCompanion) CompanionPrimary else CyanNeon

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Quick Prompts Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = if (isCompanion) {
                listOf(
                    "I had a hard day...",
                    "Are you there?",
                    "What makes you happy?",
                    "Tell me something comforting",
                    "Open WhatsApp",
                    "Check battery"
                )
            } else {
                listOf(
                    "Are you there?",
                    "Open WhatsApp",
                    "Help me plan today",
                    "Set 5 min timer",
                    "Check battery",
                    "Open YouTube",
                    "Turn on Bluetooth settings"
                )
            }

            suggestions.forEach { prompt ->
                SuggestionChip(
                    onClick = { onSendText(prompt) },
                    label = { Text(text = prompt, fontSize = 12.sp, color = TextPrimary) },
                    shape = RoundedCornerShape(16.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = SurfaceElevated.copy(alpha = 0.6f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.testTag("suggestion_${prompt.take(10).replace(" ", "_")}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Optional Expandable Text Input (for quiet environments)
        AnimatedVisibility(visible = showTextInput) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ask or command Zornia...", color = TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = activeAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("text_input_field")
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onSendText(textInput)
                            textInput = ""
                            showTextInput = false
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(activeAccent)
                        .testTag("send_text_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send text",
                        tint = Color.Black
                    )
                }
            }
        }

        // Main Bottom Controls Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text Keyboard Toggle
            IconButton(
                onClick = { showTextInput = !showTextInput },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
                    .testTag("keyboard_toggle_button")
            ) {
                Icon(
                    imageVector = if (showTextInput) Icons.Default.Close else Icons.Default.Keyboard,
                    contentDescription = "Toggle Keyboard input",
                    tint = if (showTextInput) activeAccent else TextSecondary
                )
            }

            // Central Glowing Mic / Voice Action Button
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Pulse halo when listening or speaking
                if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                if (voiceState == VoiceState.LISTENING) GreenActive.copy(alpha = 0.25f)
                                else VioletElectric.copy(alpha = 0.25f)
                            )
                    )
                }

                Button(
                    onClick = onMicTapped,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (voiceState) {
                            VoiceState.LISTENING -> GreenActive
                            VoiceState.SPEAKING -> VioletElectric
                            VoiceState.THINKING -> activeAccent
                            VoiceState.ERROR -> RoseWarm
                            else -> activeAccent
                        }
                    ),
                    modifier = Modifier
                        .size(64.dp)
                        .testTag("main_mic_button")
                ) {
                    when (voiceState) {
                        VoiceState.LISTENING -> {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Listening, tap to stop",
                                tint = Color.Black,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        VoiceState.SPEAKING -> {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Speaking, tap to interrupt",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        VoiceState.THINKING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = Color.Black,
                                strokeWidth = 2.5.dp
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Start talking",
                                tint = Color.Black,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }

            // Quick Interrupt / Barge-In Button (highlighted when speaking)
            IconButton(
                onClick = onInterruptTapped,
                enabled = voiceState == VoiceState.SPEAKING || voiceState == VoiceState.LISTENING,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (voiceState == VoiceState.SPEAKING) RoseWarm.copy(alpha = 0.3f)
                        else SurfaceElevated
                    )
                    .testTag("interrupt_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Interrupt",
                    tint = if (voiceState == VoiceState.SPEAKING) RoseWarm else TextSecondary.copy(alpha = 0.5f)
                )
            }
        }
    }
}
