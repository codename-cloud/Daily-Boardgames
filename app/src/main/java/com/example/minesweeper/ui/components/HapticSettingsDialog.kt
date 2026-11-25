package com.example.minesweeper.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minesweeper.haptic.HapticStrength

@Composable
fun HapticSettingsDialog(
    isHapticsEnabled: Boolean,
    isFlagHapticsEnabled: Boolean = true,
    hapticStrength: HapticStrength = HapticStrength.MEDIUM,
    isOledMode: Boolean,
    onToggleMasterHaptics: (Boolean) -> Unit,
    onToggleFlagHaptics: (Boolean) -> Unit = {},
    onSelectHapticStrength: (HapticStrength) -> Unit = {},
    onTestFlagHaptic: () -> Unit = {},
    onToggleOledMode: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    isSoundEnabled: Boolean? = null,
    onToggleSound: ((Boolean) -> Unit)? = null,
    showFlagHapticsSection: Boolean = true,
    onOpenTutorial: (() -> Unit)? = null,
    gameOptionsContent: (@Composable () -> Unit)? = null
) {
    val dialogContainer = if (isOledMode) Color(0xFF121212) else MaterialTheme.colorScheme.surface
    val titleColor = if (isOledMode) Color.White else MaterialTheme.colorScheme.onSurface
    val subTextColor = if (isOledMode) Color(0xFF94A3B8) else MaterialTheme.colorScheme.onSurfaceVariant

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogContainer,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Game Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = titleColor
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("haptic_settings_dialog")
            ) {
                if (gameOptionsContent != null) {
                    gameOptionsContent()
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                }

                // Theme Section: OLED Pure Black Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "OLED Pure Black Mode",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = titleColor
                        )
                        Text(
                            text = "Pure #000000 theme for OLED screens",
                            fontSize = 12.sp,
                            color = subTextColor
                        )
                    }
                    Switch(
                        checked = isOledMode,
                        onCheckedChange = onToggleOledMode,
                        modifier = Modifier.testTag("oled_mode_switch")
                    )
                }

                if (isSoundEnabled != null && onToggleSound != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Sound Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Sound Effects",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = titleColor
                            )
                            Text(
                                text = "Play audio cues for moves and wins",
                                fontSize = 12.sp,
                                color = subTextColor
                            )
                        }
                        Switch(
                            checked = isSoundEnabled,
                            onCheckedChange = onToggleSound,
                            modifier = Modifier.testTag("sound_enabled_switch")
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Master Haptics Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Vibrations & Haptics",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = titleColor
                        )
                        Text(
                            text = "Enable all tactile feedback",
                            fontSize = 12.sp,
                            color = subTextColor
                        )
                    }
                    Switch(
                        checked = isHapticsEnabled,
                        onCheckedChange = onToggleMasterHaptics,
                        modifier = Modifier.testTag("master_haptics_switch")
                    )
                }

                if (showFlagHapticsSection) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Flag Placement Haptic Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Flag Placement Haptic",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = if (isHapticsEnabled) titleColor else titleColor.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "Vibrate when planting or removing a flag",
                                fontSize = 12.sp,
                                color = if (isHapticsEnabled) subTextColor else subTextColor.copy(alpha = 0.35f)
                            )
                        }
                        Switch(
                            checked = isFlagHapticsEnabled,
                            onCheckedChange = onToggleFlagHaptics,
                            enabled = isHapticsEnabled,
                            modifier = Modifier.testTag("flag_haptics_switch")
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Vibration Strength Selector
                    Text(
                        text = "Vibration Strength",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isHapticsEnabled && isFlagHapticsEnabled) titleColor else titleColor.copy(alpha = 0.4f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOledMode) Color(0xFF1E1E1E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            HapticStrength.values().forEach { strength ->
                                val isSelected = hapticStrength == strength
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            enabled = isHapticsEnabled && isFlagHapticsEnabled,
                                            onClick = { onSelectHapticStrength(strength) }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onSelectHapticStrength(strength) },
                                        enabled = isHapticsEnabled && isFlagHapticsEnabled,
                                        modifier = Modifier.testTag("haptic_strength_${strength.name.lowercase()}")
                                    )
                                    Text(
                                        text = strength.label,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(start = 6.dp),
                                        color = if (isHapticsEnabled && isFlagHapticsEnabled) {
                                            titleColor
                                        } else {
                                            titleColor.copy(alpha = 0.4f)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onTestFlagHaptic,
                        enabled = isHapticsEnabled && isFlagHapticsEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_flag_haptic_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Test Flag Vibration (${hapticStrength.label})")
                    }
                }

                if (onOpenTutorial != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    OutlinedButton(
                        onClick = onOpenTutorial,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("watch_tutorial_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = "Watch Tutorial",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_haptic_settings_button")
            ) {
                Text("Done")
            }
        }
    )
}
