package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.AssistantMode
import com.example.model.VoiceState
import com.example.ui.ZorniaViewModel
import com.example.ui.components.ControlsBar
import com.example.ui.components.ConversationFeed
import com.example.ui.components.MemorySheet
import com.example.ui.components.ModeSelector
import com.example.ui.components.OrbVisualizer
import com.example.ui.components.SettingsSheet
import com.example.ui.components.ToolsDialog
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BackgroundObsidian
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CompanionGlow
import com.example.ui.theme.CompanionPrimary
import com.example.ui.theme.CompanionSurface
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GreenActive
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.RoseWarm
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletElectric

class MainActivity : ComponentActivity() {

    private val viewModel: ZorniaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                ZorniaMainScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ZorniaMainScreen(viewModel: ZorniaViewModel) {
    val context = LocalContext.current

    val messages by viewModel.messages.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()
    val voiceConfig by viewModel.voiceConfig.collectAsState()
    val rmsAmplitude by viewModel.rmsAmplitude.collectAsState()
    val partialTranscript by viewModel.partialTranscript.collectAsState()
    val hasMicPermission by viewModel.hasMicPermission.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showMemorySheet by remember { mutableStateOf(false) }
    var showToolsDialog by remember { mutableStateOf(false) }

    // Check & request RECORD_AUDIO permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setMicPermissionGranted(isGranted)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setMicPermissionGranted(granted)
    }

    val onMicAction = {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            viewModel.onMicTapped()
        }
    }

    val isCompanion = currentMode == AssistantMode.COMPANION
    val bgBrush = if (isCompanion) {
        Brush.verticalGradient(
            colors = listOf(CompanionSurface, BackgroundObsidian, BackgroundObsidian)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(SurfaceDark, BackgroundObsidian, BackgroundObsidian)
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("zornia_scaffold"),
        containerColor = BackgroundObsidian
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top App Bar
                ZorniaTopBar(
                    mode = currentMode,
                    voiceState = voiceState,
                    isOnline = isOnline,
                    onOpenTools = { showToolsDialog = true },
                    onOpenMemory = { showMemorySheet = true },
                    onOpenSettings = { showSettingsSheet = true },
                    onClearHistory = { viewModel.clearHistory() }
                )

                // Assistant Modes Selector
                ModeSelector(
                    selectedMode = currentMode,
                    onModeSelected = { viewModel.setMode(it) }
                )

                // Microphone Permission Alert Banner if not granted
                if (!hasMicPermission) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("mic_permission_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Microphone Permission Needed",
                                    color = AmberWarning,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Zornia needs mic access for real-time voice conversations.",
                                    color = TextPrimary,
                                    fontSize = 11.sp
                                )
                            }
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Allow", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Center AI Voice Orb Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OrbVisualizer(
                        voiceState = voiceState,
                        mode = currentMode,
                        rmsAmplitude = rmsAmplitude,
                        onClick = onMicAction,
                        size = 175.dp
                    )
                }

                // Voice Status State Indicator Label
                VoiceStateIndicator(
                    voiceState = voiceState,
                    mode = currentMode,
                    onInterrupt = { viewModel.interruptSpeaking() }
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Real-time Conversation Feed
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    ConversationFeed(
                        messages = messages,
                        partialTranscript = partialTranscript,
                        onReplayAudio = { text ->
                            viewModel.handleUserInput("Repeat: $text")
                        }
                    )
                }

                // Bottom Controls Bar (Mic button, Interrupt button, Keyboard text input, quick prompts)
                ControlsBar(
                    voiceState = voiceState,
                    mode = currentMode,
                    onMicTapped = onMicAction,
                    onInterruptTapped = { viewModel.interruptSpeaking() },
                    onSendText = { text -> viewModel.handleUserInput(text) }
                )
            }
        }
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        SettingsSheet(
            config = voiceConfig,
            isApiKeyConfigured = viewModel.isGeminiConfigured(),
            currentApiKey = viewModel.getActiveApiKey(),
            onSaveApiKey = { key -> viewModel.saveCustomApiKey(key) },
            onClearApiKey = { viewModel.clearCustomApiKey() },
            onConfigChanged = { updated -> viewModel.updateVoiceConfig(updated) },
            onDismiss = { showSettingsSheet = false }
        )
    }

    // Memory Bottom Sheet
    if (showMemorySheet) {
        MemorySheet(
            memoryManager = viewModel.memoryManager,
            onDismiss = { showMemorySheet = false }
        )
    }

    // Phone Tools Dialog
    if (showToolsDialog) {
        ToolsDialog(
            onTriggerTest = { query -> viewModel.handleUserInput(query) },
            onDismiss = { showToolsDialog = false }
        )
    }
}

@Composable
fun ZorniaTopBar(
    mode: AssistantMode,
    voiceState: VoiceState,
    isOnline: Boolean,
    onOpenTools: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearHistory: () -> Unit
) {
    val isCompanion = mode == AssistantMode.COMPANION
    val brandColor = if (isCompanion) CompanionPrimary else CyanNeon

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo & Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(brandColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCompanion) Icons.Default.AutoAwesome else Icons.Default.GraphicEq,
                    contentDescription = "Zornia Logo",
                    tint = brandColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ZORNIA",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) GreenActive else AmberWarning)
                    )
                }
                Text(
                    text = if (isCompanion) "Companion Persona" else "Next-Gen Live Voice Agent",
                    color = if (isCompanion) CompanionGlow else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Action Icons
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onOpenTools,
                modifier = Modifier.testTag("topbar_tools_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = "Phone Tools",
                    tint = TextSecondary
                )
            }

            IconButton(
                onClick = onOpenMemory,
                modifier = Modifier.testTag("topbar_memory_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "Memory Vault",
                    tint = TextSecondary
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("topbar_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Voice & AI Settings",
                    tint = TextSecondary
                )
            }

            IconButton(
                onClick = onClearHistory,
                modifier = Modifier.testTag("topbar_clear_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Conversation",
                    tint = TextSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun VoiceStateIndicator(
    voiceState: VoiceState,
    mode: AssistantMode,
    onInterrupt: () -> Unit
) {
    val stateColor by animateColorAsState(
        targetValue = when (voiceState) {
            VoiceState.LISTENING -> GreenActive
            VoiceState.SPEAKING -> VioletElectric
            VoiceState.THINKING -> CyanNeon
            VoiceState.INTERRUPTED -> RoseWarm
            VoiceState.ERROR -> AmberWarning
            VoiceState.IDLE -> TextSecondary
        },
        label = "StateColorAnimation"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = stateColor.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, stateColor.copy(alpha = 0.35f)),
        modifier = Modifier.testTag("voice_state_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(stateColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (voiceState) {
                    VoiceState.LISTENING -> "LISTENING • SPEAK NOW"
                    VoiceState.THINKING -> "PROCESSING LIVE STREAM..."
                    VoiceState.SPEAKING -> "ZORNIA SPEAKING (TAP TO INTERRUPT)"
                    VoiceState.INTERRUPTED -> "INTERRUPTED"
                    VoiceState.ERROR -> "CONNECTION RETRY"
                    VoiceState.IDLE -> "READY • TAP TO TALK"
                },
                color = stateColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
