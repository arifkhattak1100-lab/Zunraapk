package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GreenActive
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletElectric

data class ToolCapability(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val isReady: Boolean,
    val testQuery: String? = null
)

@Composable
fun ToolsDialog(
    onTriggerTest: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val tools = listOf(
        ToolCapability(
            name = "App Launcher",
            description = "Opens WhatsApp, YouTube, Maps, Camera, Spotify, Chrome, Settings, etc.",
            icon = Icons.Default.Widgets,
            isReady = true,
            testQuery = "Open WhatsApp"
        ),
        ToolCapability(
            name = "Phone Calling & Dialer",
            description = "Prepares phone dialer for contacts and numbers safely.",
            icon = Icons.Default.Phone,
            isReady = true,
            testQuery = "Call 555-0199"
        ),
        ToolCapability(
            name = "System Settings",
            description = "Quick navigation to Wi-Fi, Bluetooth, Display, Sound, Battery.",
            icon = Icons.Default.Settings,
            isReady = true,
            testQuery = "Open Bluetooth settings"
        ),
        ToolCapability(
            name = "Web Research",
            description = "Queries the web for live search topics.",
            icon = Icons.Default.Search,
            isReady = true,
            testQuery = "Search web for space telescope discoveries"
        ),
        ToolCapability(
            name = "Timers & Alarms",
            description = "Hands-free countdown timer & clock alarm configuration.",
            icon = Icons.Default.Alarm,
            isReady = true,
            testQuery = "Set 5 min timer"
        ),
        ToolCapability(
            name = "Battery & Power Monitor",
            description = "Reads active battery percentage & charging telemetry.",
            icon = Icons.Default.BatteryChargingFull,
            isReady = true,
            testQuery = "Check battery"
        ),
        ToolCapability(
            name = "Media Playback Control",
            description = "Dispatches play, pause, next track key events.",
            icon = Icons.Default.PlayArrow,
            isReady = true,
            testQuery = "Pause music"
        ),
        ToolCapability(
            name = "Vision & Camera AI",
            description = "Real-time multimodal camera understanding (Architecture planned)",
            icon = Icons.Default.Videocam,
            isReady = false
        ),
        ToolCapability(
            name = "Smart Home & IoT",
            description = "Matter / Home Assistant voice gateway (Architecture planned)",
            icon = Icons.Default.Home,
            isReady = false
        ),
        ToolCapability(
            name = "Wearable & Watch Sync",
            description = "Wear OS live companion synchronization (Architecture planned)",
            icon = Icons.Default.Watch,
            isReady = false
        ),
        ToolCapability(
            name = "Automotive / Vehicle UI",
            description = "Android Auto companion audio profile (Architecture planned)",
            icon = Icons.Default.DirectionsCar,
            isReady = false
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Phone Tools & Actions",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().testTag("tools_capabilities_dialog")) {
                Text(
                    text = "Zornia executes verified Android Intents through a safe tool layer. No arbitrary shell execution is permitted.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tools) { tool ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                            border = BorderStroke(
                                1.dp,
                                if (tool.isReady) BorderSubtle else Color.White.copy(alpha = 0.05f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = tool.name,
                                    tint = if (tool.isReady) CyanNeon else TextSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tool.name,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (tool.isReady) GreenActive.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f)
                                        ) {
                                            Text(
                                                text = if (tool.isReady) "READY" else "FUTURE",
                                                color = if (tool.isReady) GreenActive else TextSecondary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = tool.description,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                if (tool.isReady && tool.testQuery != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    TextButton(
                                        onClick = {
                                            onTriggerTest(tool.testQuery)
                                            onDismiss()
                                        }
                                    ) {
                                        Text("Test", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = CyanNeon)
            }
        }
    )
}
