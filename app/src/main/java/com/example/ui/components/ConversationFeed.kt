package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConversationMessage
import com.example.model.MessageRole
import com.example.model.ToolStatus
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GreenActive
import com.example.ui.theme.RoseWarm
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletElectric
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConversationFeed(
    messages: List<ConversationMessage>,
    partialTranscript: String,
    onReplayAudio: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new message or partial input
    LaunchedEffect(messages.size, partialTranscript) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .testTag("conversation_feed"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            MessageItem(
                message = message,
                onReplayAudio = onReplayAudio
            )
        }

        // Live streaming speech input from user
        if (partialTranscript.isNotBlank()) {
            item(key = "partial_transcript_item") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceElevated.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CyanNeon)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = partialTranscript,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageItem(
    message: ConversationMessage,
    onReplayAudio: (String) -> Unit
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeString = timeFormat.format(Date(message.timestamp))

    when (message.role) {
        MessageRole.USER -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_message_${message.id}"),
                horizontalAlignment = Alignment.End
            ) {
                // Emotion badge if detected
                message.emotion?.let { emotion ->
                    if (emotion != com.example.model.Emotion.NEUTRAL) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = emotion.color.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, emotion.color.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = "Mood: ${emotion.label}",
                                color = emotion.color,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = VioletElectric.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VioletElectric.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = message.text,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = timeString,
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }

        MessageRole.ZORNIA -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("zornia_message_${message.id}"),
                horizontalArrangement = Arrangement.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = SurfaceElevated.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(CyanNeon)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Zornia",
                                    color = CyanNeon,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (message.isAcknowledgement) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• Instant Response",
                                        color = GreenActive,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onReplayAudio(message.text) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Replay audio",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = message.text,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        )

                        // If message included a tool execution
                        message.toolExecution?.let { tool ->
                            Spacer(modifier = Modifier.height(8.dp))
                            ToolExecutionCard(tool = tool)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = timeString,
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }

        MessageRole.TOOL -> {
            message.toolExecution?.let { tool ->
                ToolExecutionCard(
                    tool = tool,
                    modifier = Modifier.fillMaxWidth(0.95f)
                )
            }
        }

        MessageRole.SYSTEM -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Text(
                        text = message.text,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ToolExecutionCard(
    tool: com.example.model.ToolExecution,
    modifier: Modifier = Modifier
) {
    val isSuccess = tool.status == ToolStatus.SUCCESS
    val borderColor = if (isSuccess) GreenActive else RoseWarm

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceElevated
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
        modifier = modifier.testTag("tool_card_${tool.toolName}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = tool.toolName,
                tint = borderColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Phone Action: ${tool.toolName}",
                    color = borderColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tool.userFriendlyMessage.ifBlank { tool.errorMessage ?: "Executed" },
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            }
        }
    }
}
