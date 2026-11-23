package com.example.minesweeper.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
    isFlagHapticsEnabled: Boolean,
    hapticStrength: HapticStrength,
    isOledMode: Boolean,
    onToggleMasterHaptics: (Boolean) -> Unit,
    onToggleFlagHaptics: (Boolean) -> Unit,
    onSelectHapticStrength: (HapticStrength) -> Unit,
    onTestFlagHaptic: () -> Unit,
    onToggleOledMode: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isOledMode) Color(0xFF121212) else MaterialTheme.colorScheme.surface,
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
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("haptic_settings_dialog")
            ) {
                // OLED Mode Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "OLED Pure Black Mode",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Pure #000000 background for OLED screens (board colors unchanged)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isOledMode,
                        onCheckedChange = onToggleOledMode,
                        modifier = Modifier.testTag("oled_mode_switch")
                    )
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vibrations & Haptics",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Enable all tactile feedback",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isHapticsEnabled,
                        onCheckedChange = onToggleMasterHaptics,
                        modifier = Modifier.testTag("master_haptics_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Flag Placement Haptic Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Flag Placement Haptic",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = if (isHapticsEnabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "Vibrate when planting or removing a flag",
                            fontSize = 12.sp,
                            color = if (isHapticsEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
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
                    color = if (isHapticsEnabled && isFlagHapticsEnabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
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
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
                                        Color.Unspecified
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Test Flag Vibration Button
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
