package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MemoryItem
import com.example.service.MemoryManager
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.RoseWarm
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletElectric

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorySheet(
    memoryManager: MemoryManager,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val memories by memoryManager.memories.collectAsState()
    val isEnabled by memoryManager.isMemoryEnabled.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("User Preference") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .testTag("memory_bottom_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Zornia Memory",
                        tint = CyanNeon,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Zornia Memory Vault",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${memories.size} remembered items",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Enable/Disable Memory Toggle Row
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEnabled) "Memory Enabled" else "Memory Disabled",
                            color = if (isEnabled) CyanNeon else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Secure local memory of non-sensitive preferences & facts.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { memoryManager.setMemoryEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CyanNeon,
                            checkedTrackColor = CyanNeon.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions bar: Add custom memory, Clear all
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showAddDialog = true },
                    enabled = isEnabled,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletElectric),
                    modifier = Modifier.testTag("add_memory_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Memory", fontSize = 13.sp)
                }

                if (memories.isNotEmpty()) {
                    TextButton(
                        onClick = { memoryManager.clearAllMemories() },
                        colors = ButtonDefaults.textButtonColors(contentColor = RoseWarm),
                        modifier = Modifier.testTag("clear_all_memory_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear All", fontSize = 13.sp)
                    }
                }
            }

            // Quick Add Inline Dialog
            if (showAddDialog) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
                    border = BorderStroke(1.dp, VioletElectric.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "New Memory Entry",
                            color = CyanNeon,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newKey,
                            onValueChange = { newKey = it },
                            label = { Text("Topic / Key (e.g. Favorite Drink, Call me)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = BorderSubtle
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newValue,
                            onValueChange = { newValue = it },
                            label = { Text("Details (e.g. Green tea with honey)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = BorderSubtle
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showAddDialog = false }) {
                                Text("Cancel", color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newKey.isNotBlank() && newValue.isNotBlank()) {
                                        memoryManager.addMemory(newCategory, newKey, newValue)
                                        newKey = ""
                                        newValue = ""
                                        showAddDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                            ) {
                                Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Memory List
            if (memories.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceElevated.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No saved memories yet",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Say 'Remember that my name is Alex' or add items manually.",
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(memories, key = { it.id }) { item ->
                        MemoryListItem(
                            item = item,
                            onDelete = { memoryManager.deleteMemory(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryListItem(
    item: MemoryItem,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth().testTag("memory_item_${item.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.category.uppercase(),
                    color = CyanNeon,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.key,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.value,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete memory",
                    tint = TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
