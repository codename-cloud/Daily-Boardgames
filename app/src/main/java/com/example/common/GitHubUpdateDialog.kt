package com.example.common

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OutfitFontFamily

@Composable
fun GitHubUpdateDialog(
    isOledMode: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings by AppSettingsStore.settings.collectAsStateWithLifecycle()
    val updateState by GitHubUpdateManager.uiState.collectAsStateWithLifecycle()
    val colors = rememberArcadeThemeColors(isOledMode)

    val configuredRepo = settings.githubRepo
    var browserLaunchError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.dialogSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB).copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "GitHub Updates",
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = colors.primaryText
                    )
                    Text(
                        text = GitHubUpdateManager.currentVersionSummary(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.5.sp,
                        color = colors.secondaryText
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("github_update_dialog"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = configuredRepo.ifBlank { "Configured automatically by GitHub Actions" },
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("GitHub Repository") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (configuredRepo.isNotBlank()) colors.primaryText else colors.secondaryText,
                        disabledBorderColor = colors.cardBorder,
                        disabledLabelColor = colors.secondaryText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("github_repo_input")
                )

                Button(
                    onClick = {
                        browserLaunchError = null
                        GitHubUpdateManager.checkForUpdates(configuredRepo)
                    },
                    enabled = configuredRepo.isNotBlank() && updateState.status !is GitHubCheckStatus.Checking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("check_github_updates_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(18.dp)
                    )
                    Text(
                        text = if (updateState.status is GitHubCheckStatus.Checking) {
                            "Checking GitHub..."
                        } else {
                            "Check for Updates"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }

                when (val status = updateState.status) {
                    is GitHubCheckStatus.Idle -> {
                        UpdateStatusCard(
                            isOledMode = isOledMode,
                            colors = colors,
                            borderColor = colors.cardBorder
                        ) {
                            Text(
                                text = if (configuredRepo.isNotBlank()) {
                                    "Linked to $configuredRepo via the GitHub Actions APK workflow. Tap Check for Updates to check for new builds."
                                } else {
                                    "Repository is locked to the GitHub Actions build. Install an APK built by the GitHub workflow to check for updates."
                                },
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                color = colors.secondaryText
                            )
                        }
                    }

                    is GitHubCheckStatus.Checking -> {
                        UpdateStatusCard(
                            isOledMode = isOledMode,
                            colors = colors,
                            borderColor = Color(0xFF3B82F6).copy(alpha = 0.5f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Checking latest GitHub release & workflow runs...",
                                    fontSize = 12.5.sp,
                                    color = colors.primaryText
                                )
                            }
                        }
                    }

                    is GitHubCheckStatus.UpToDate -> {
                        UpdateStatusCard(
                            isOledMode = isOledMode,
                            colors = colors,
                            borderColor = Color(0xFF16A34A).copy(alpha = 0.6f)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF22C55E),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "You're up to date!",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = colors.primaryText
                                        )
                                        Text(
                                            text = "Latest on ${status.repo}: ${status.latestLabel}",
                                            fontSize = 12.sp,
                                            color = colors.secondaryText
                                        )
                                    }
                                }

                                val upToDateEntries = status.changelogEntries.ifEmpty {
                                    status.changelogItems.map { ChangelogEntry(it) }
                                }
                                if (upToDateEntries.isNotEmpty()) {
                                    ChangelogListSection(
                                        heading = "Latest Build Changes",
                                        items = upToDateEntries,
                                        colors = colors,
                                        isOledMode = isOledMode
                                    )
                                }
                            }
                        }
                    }

                    is GitHubCheckStatus.UpdateAvailable -> {
                        val build = status.build
                        UpdateStatusCard(
                            isOledMode = isOledMode,
                            colors = colors,
                            borderColor = Color(0xFF2563EB)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "New Build Available: ${build.tagOrVersion}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = colors.primaryText
                                        )
                                        Text(
                                            text = build.title,
                                            fontSize = 12.sp,
                                            color = colors.secondaryText
                                        )
                                    }
                                }

                                val displayItems = build.changelogEntries.ifEmpty {
                                    build.changelogItems.map { ChangelogEntry(it) }.ifEmpty {
                                        build.notes.takeIf { it.isNotBlank() }
                                            ?.let { listOf(ChangelogEntry(it)) }
                                            .orEmpty()
                                    }
                                }
                                if (displayItems.isNotEmpty()) {
                                    ChangelogListSection(
                                        heading = "What's New",
                                        items = displayItems,
                                        colors = colors,
                                        isOledMode = isOledMode
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                val targetUrl = build.apkDownloadUrl ?: build.browserUrl
                                Button(
                                    onClick = {
                                        val opened = GitHubUpdateManager.openExternalUrl(context, targetUrl)
                                        if (!opened) {
                                            browserLaunchError = "No browser app found to open $targetUrl"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF16A34A)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("open_github_update_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .size(16.dp)
                                    )
                                    Text(
                                        text = if (build.apkDownloadUrl != null) {
                                            "Download APK in Browser"
                                        } else {
                                            "Open Workflow Build in Browser"
                                        },
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (build.apkDownloadUrl != null && build.browserUrl != build.apkDownloadUrl) {
                                    OutlinedButton(
                                        onClick = {
                                            val opened = GitHubUpdateManager.openExternalUrl(context, build.browserUrl)
                                            if (!opened) {
                                                browserLaunchError = "No browser app found to open ${build.browserUrl}"
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("view_github_release_button"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("View Release Page on GitHub")
                                    }
                                }
                            }
                        }
                    }

                    is GitHubCheckStatus.Error -> {
                        UpdateStatusCard(
                            isOledMode = isOledMode,
                            colors = colors,
                            borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = status.message,
                                    fontSize = 12.5.sp,
                                    color = colors.primaryText
                                )
                            }
                        }
                    }
                }

                browserLaunchError?.let { err ->
                    Text(
                        text = err,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "Opens the GitHub release or workflow APK link in your browser so updates stay compliant with Google Play Creator Program rules.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = colors.secondaryText
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_github_update_dialog")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ChangelogListSection(
    heading: String,
    items: List<ChangelogEntry>,
    colors: ArcadeThemeColors,
    isOledMode: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isOledMode) Color(0xFF121826) else colors.cardBackground.copy(alpha = 0.7f))
            .border(1.dp, colors.cardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("github_update_changelog"),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = heading,
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = colors.primaryText
        )
        items.forEach { entry ->
            val isSubBullet = entry.indentLevel > 0
            Row(
                modifier = Modifier.padding(start = if (isSubBullet) 16.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = if (isSubBullet) "◦" else "•",
                    fontSize = if (isSubBullet) 11.5.sp else 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSubBullet) Color(0xFF60A5FA).copy(alpha = 0.75f) else Color(0xFF60A5FA)
                )
                Text(
                    text = entry.text,
                    fontSize = if (isSubBullet) 11.5.sp else 12.sp,
                    lineHeight = if (isSubBullet) 16.5.sp else 17.sp,
                    color = if (isSubBullet) colors.secondaryText else colors.primaryText.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun UpdateStatusCard(
    isOledMode: Boolean,
    colors: ArcadeThemeColors,
    borderColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isOledMode) Color(0xFF0A0D14) else colors.screenBackground.copy(alpha = 0.7f))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        content()
    }
}
