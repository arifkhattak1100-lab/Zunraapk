package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantMode
import com.example.ui.theme.CompanionPrimary
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GreenActive
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.VioletElectric

@Composable
fun ModeSelector(
    selectedMode: AssistantMode,
    onModeSelected: (AssistantMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistantMode.values().forEach { mode ->
            val isSelected = mode == selectedMode
            val activeColor = when (mode) {
                AssistantMode.NORMAL -> CyanNeon
                AssistantMode.COMPANION -> CompanionPrimary
                AssistantMode.FOCUS -> GreenActive
                AssistantMode.HANDS_FREE -> VioletElectric
                AssistantMode.PRIVATE -> Color(0xFFFBBF24)
            }

            val icon = when (mode) {
                AssistantMode.NORMAL -> Icons.Default.Psychology
                AssistantMode.COMPANION -> Icons.Default.Favorite
                AssistantMode.FOCUS -> Icons.Default.Bolt
                AssistantMode.HANDS_FREE -> Icons.Default.GraphicEq
                AssistantMode.PRIVATE -> Icons.Default.Lock
            }

            FilterChip(
                selected = isSelected,
                onClick = { onModeSelected(mode) },
                label = {
                    Text(
                        text = mode.title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = mode.title,
                        tint = if (isSelected) activeColor else Color.Gray
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SurfaceElevated.copy(alpha = 0.5f),
                    labelColor = Color.White.copy(alpha = 0.8f),
                    selectedContainerColor = activeColor.copy(alpha = 0.2f),
                    selectedLabelColor = activeColor
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) activeColor else Color.White.copy(alpha = 0.12f)
                ),
                modifier = Modifier.testTag("mode_chip_${mode.name.lowercase()}")
            )
        }
    }
}
