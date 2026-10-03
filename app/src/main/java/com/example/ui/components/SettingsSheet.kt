package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VoiceConfig
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GreenActive
import com.example.ui.theme.RoseWarm
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletElectric

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    config: VoiceConfig,
    isApiKeyConfigured: Boolean,
    currentApiKey: String = "",
    onSaveApiKey: ((String) -> Unit)? = null,
    onClearApiKey: (() -> Unit)? = null,
    onConfigChanged: (VoiceConfig) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var pitch by remember { mutableFloatStateOf(config.pitch) }
    var speed by remember { mutableFloatStateOf(config.speed) }
    var persona by remember { mutableStateOf(config.persona) }
    var targetModel by remember { mutableStateOf(config.targetModel) }
    var memoryEnabled by remember { mutableStateOf(config.memoryEnabled) }
    var handsFreeContinuous by remember { mutableStateOf(config.handsFreeContinuous) }

    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var showKeyEditor by remember { mutableStateOf(!isApiKeyConfigured) }
    var keySavedToast by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Zornia Voice & AI Engine",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Status & API Key Banner
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceElevated
                ),
                border = BorderStroke(
                    1.dp,
                    if (isApiKeyConfigured) GreenActive.copy(alpha = 0.5f) else AmberWarning.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isApiKeyConfigured) Icons.Default.Check else Icons.Default.Warning,
                            contentDescription = "API Status",
                            tint = if (isApiKeyConfigured) GreenActive else AmberWarning,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isApiKeyConfigured) "Gemini Live AI Connected" else "API Key Configuration",
                                color = if (isApiKeyConfigured) GreenActive else AmberWarning,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isApiKeyConfigured)
                                    "Live streaming audio session & tool layer ready."
                                else
                                    "Enter your key below or configure via AI Studio Secrets.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = { showKeyEditor = !showKeyEditor }) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Edit API Key",
                                tint = CyanNeon
                            )
                        }
                    }

                    if (showKeyEditor) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it
                                keySavedToast = false
                            },
                            label = { Text("Gemini API Key") },
                            placeholder = { Text("Paste AI Studio API key...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input"),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                onSaveApiKey?.invoke(apiKeyInput.trim())
                                keySavedToast = true
                            })
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (apiKeyInput.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        apiKeyInput = ""
                                        onClearApiKey?.invoke()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseWarm),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text("Clear", fontSize = 12.sp)
                                }
                            }
                            Button(
                                onClick = {
                                    onSaveApiKey?.invoke(apiKeyInput.trim())
                                    keySavedToast = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save Key", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (keySavedToast) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Key updated successfully.",
                                color = GreenActive,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Dedicated Voice Personality Selector
            Text(
                text = "Zornia Voice Personality",
                color = CyanNeon,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            val personas = listOf(
                "Warm & Natural" to "Gentle, expressive, conversational tone (Default)",
                "Gentle & Calm" to "Soft, soothing, stress-relieving presence",
                "Bright & Playful" to "Upbeat, energetic, enthusiastic delivery",
                "Professional" to "Clear, structured, polished efficiency"
            )

            personas.forEach { (name, desc) ->
                val isSelected = persona == name
                Surface(
                    onClick = {
                        persona = name
                        when (name) {
                            "Warm & Natural" -> { pitch = 1.05f; speed = 1.02f }
                            "Gentle & Calm" -> { pitch = 0.96f; speed = 0.95f }
                            "Bright & Playful" -> { pitch = 1.15f; speed = 1.08f }
                            "Professional" -> { pitch = 1.0f; speed = 1.0f }
                        }
                        onConfigChanged(
                            config.copy(
                                persona = name,
                                pitch = pitch,
                                speed = speed,
                                targetModel = targetModel,
                                memoryEnabled = memoryEnabled,
                                handsFreeContinuous = handsFreeContinuous
                            )
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) CyanNeon.copy(alpha = 0.12f) else SurfaceElevated,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) CyanNeon.copy(alpha = 0.6f) else Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .testTag("persona_option_$name")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) CyanNeon else TextSecondary.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = name,
                                color = if (isSelected) CyanNeon else TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = desc,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Voice Pitch Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Voice Pitch", color = TextPrimary, fontSize = 13.sp)
                Text("%.2fx".format(pitch), color = CyanNeon, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = pitch,
                onValueChange = {
                    pitch = it
                    onConfigChanged(config.copy(pitch = it))
                },
                valueRange = 0.75f..1.35f,
                colors = SliderDefaults.colors(
                    thumbColor = CyanNeon,
                    activeTrackColor = CyanNeon,
                    inactiveTrackColor = SurfaceElevated
                )
            )

            // Voice Speed Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Speech Cadence", color = TextPrimary, fontSize = 13.sp)
                Text("%.2fx".format(speed), color = CyanNeon, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = speed,
                onValueChange = {
                    speed = it
                    onConfigChanged(config.copy(speed = it))
                },
                valueRange = 0.8f..1.4f,
                colors = SliderDefaults.colors(
                    thumbColor = CyanNeon,
                    activeTrackColor = CyanNeon,
                    inactiveTrackColor = SurfaceElevated
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Gemini AI Model Selector
            Text(
                text = "Target AI Model",
                color = CyanNeon,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            val models = listOf(
                "gemini-3.5-flash" to "Ultra-fast streaming conversational intelligence (Default)",
                "gemini-3.8-live" to "Next-Gen real-time bidirectional WebSocket live streaming"
            )

            models.forEach { (modelId, desc) ->
                val isSelected = targetModel == modelId
                Surface(
                    onClick = {
                        targetModel = modelId
                        onConfigChanged(config.copy(targetModel = modelId))
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) VioletElectric.copy(alpha = 0.12f) else SurfaceElevated,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) VioletElectric.copy(alpha = 0.6f) else Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .testTag("model_option_$modelId")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) VioletElectric else TextSecondary.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = modelId,
                                color = if (isSelected) VioletElectric else TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = desc,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hands-Free Continuous Mode Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hands-Free Continuous Listening", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Auto-re-engages microphone when Zornia finishes speaking", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = handsFreeContinuous,
                    onCheckedChange = {
                        handsFreeContinuous = it
                        onConfigChanged(config.copy(handsFreeContinuous = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = CyanNeon
                    )
                )
            }

            // Memory Vault Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Persistent Memory", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Retains your name, preferences, and personalized context", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = memoryEnabled,
                    onCheckedChange = {
                        memoryEnabled = it
                        onConfigChanged(config.copy(memoryEnabled = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = VioletElectric
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Done Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_done_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
