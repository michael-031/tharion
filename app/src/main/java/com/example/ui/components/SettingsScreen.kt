package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GuardUiState
import com.example.ui.theme.AbyssBlack
import com.example.ui.theme.AmberCaution
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    uiState: GuardUiState,
    onAdjustPauseThreshold: (Int) -> Unit,
    onSetPauseThreshold: (Int) -> Unit,
    onAdjustResumeThreshold: (Int) -> Unit,
    onToggleAutoResume: (Boolean) -> Unit,
    onToggleWebViewFallback: (Boolean) -> Unit,
    onOpenPortalConfig: () -> Unit,
    onOpenBackgroundPriority: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onAddQuickTile: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val settings = uiState.settings
    val pauseFraction = ((settings.pauseThresholdDbm + 95f) / 50f).coerceIn(0.1f, 0.95f)
    val resumeFraction = ((settings.resumeThresholdDbm + 95f) / 50f).coerceIn(0.1f, 0.95f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .auroraDarkBackdrop()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("settings_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
        ) {
            // Top bar with Back navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PureWhite
                    )
                }
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Auto-pause distance, portal & background protection",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            HorizontalDivider(color = SlateBorder, thickness = 1.dp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Section 1: When to Auto-Pause
                Text(
                    text = stringResource(R.string.threshold_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pick how far you can walk before WiFi pauses automatically:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DistancePresetChip(
                            label = "Close",
                            sublabel = "Short range",
                            selected = settings.pauseThresholdDbm >= -70,
                            onClick = { onSetPauseThreshold(-68) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preset_distance_close")
                        )
                        DistancePresetChip(
                            label = "Balanced",
                            sublabel = "Recommended",
                            selected = settings.pauseThresholdDbm in -78..-71,
                            onClick = { onSetPauseThreshold(-75) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preset_distance_balanced")
                        )
                        DistancePresetChip(
                            label = "Far",
                            sublabel = "Max range",
                            selected = settings.pauseThresholdDbm <= -79,
                            onClick = { onSetPauseThreshold(-82) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("preset_distance_far")
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InsightProgressBarRow(
                        title = "Pause Trigger (${dbmToSignalPercent(settings.pauseThresholdDbm)}% • ${settings.pauseThresholdDbm} dBm)",
                        leftCaption = "Far away (-90 dBm)",
                        rightCaption = "Very close (-50 dBm)",
                        fraction = pauseFraction
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fine-tune distance",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { onAdjustPauseThreshold(-2) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .background(SlateCardElevated, CircleShape)
                                    .border(1.dp, SlateBorder, CircleShape)
                                    .testTag("threshold_minus_button")
                            ) {
                                Icon(Icons.Default.Remove, "Allow farther distance", tint = PureWhite)
                            }
                            IconButton(
                                onClick = { onAdjustPauseThreshold(+2) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .background(SlateCardElevated, CircleShape)
                                    .border(1.dp, SlateBorder, CircleShape)
                                    .testTag("threshold_plus_button")
                            ) {
                                Icon(Icons.Default.Add, "Pause sooner", tint = PureWhite)
                            }
                        }
                    }
                }

                HorizontalDivider(color = SlateBorder, thickness = 1.dp)

                // Section 2: Auto-Resume When Back
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.auto_resume_label),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.auto_resume_desc, settings.resumeThresholdDbm),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = settings.autoResumeEnabled,
                            onCheckedChange = onToggleAutoResume,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AbyssBlack,
                                checkedTrackColor = PureWhite,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SlateCardElevated
                            ),
                            modifier = Modifier.testTag("auto_resume_switch")
                        )
                    }

                    AnimatedVisibility(visible = settings.autoResumeEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            InsightProgressBarRow(
                                title = "Resume Signal (${dbmToSignalPercent(settings.resumeThresholdDbm)}% • ${settings.resumeThresholdDbm} dBm)",
                                leftCaption = "Weak (-80 dBm)",
                                rightCaption = "Very strong (-40 dBm)",
                                fraction = resumeFraction
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onAdjustResumeThreshold(-2) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .background(SlateCardElevated, CircleShape)
                                        .border(1.dp, SlateBorder, CircleShape)
                                        .testTag("resume_threshold_minus")
                                ) {
                                    Icon(Icons.Default.Remove, "Decrease Resume Threshold", tint = PureWhite)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { onAdjustResumeThreshold(+2) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .background(SlateCardElevated, CircleShape)
                                        .border(1.dp, SlateBorder, CircleShape)
                                        .testTag("resume_threshold_plus")
                                ) {
                                    Icon(Icons.Default.Add, "Increase Resume Threshold", tint = PureWhite)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = SlateBorder, thickness = 1.dp)

                // Section 3: Piso WiFi Portal & Pause Timer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SlateCardElevated)
                        .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Piso WiFi Portal & Expiry",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SlateCard)
                            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenPortalConfig)
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("edit_portal_config_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Portal Address & Max Pause",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text = "${settings.portalBaseUrl} • ${settings.pauseExpiryMinutes}m limit",
                                style = MaterialTheme.typography.labelMedium,
                                color = PureWhite
                            )
                        }
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Backup Portal Clicker",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Hidden browser fallback for custom Piso WiFi portals",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = settings.useWebViewFallback,
                            onCheckedChange = onToggleWebViewFallback,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AbyssBlack,
                                checkedTrackColor = PureWhite,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SlateCard
                            ),
                            modifier = Modifier.testTag("webview_fallback_switch")
                        )
                    }
                }

                // Section 4: Quick Settings Shortcut
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SlateCardElevated)
                        .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.quick_tile_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.quick_tile_card_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Button(
                        onClick = onAddQuickTile,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureWhite,
                            contentColor = AbyssBlack
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("add_quick_tile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.quick_tile_add_button),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Section 5: Background Reliability & Battery Helper
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SlateCard)
                        .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenBackgroundPriority)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.bg_priority_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (uiState.isBatteryOptimizationIgnored) {
                                stringResource(R.string.bg_priority_status_unrestricted)
                            } else {
                                stringResource(R.string.bg_priority_status_optimized)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isBatteryOptimizationIgnored) PureWhite else AmberCaution,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (uiState.isBatteryOptimizationIgnored) {
                            "Battery optimization is disabled for Eltharion. Background WiFi monitoring stays active."
                        } else {
                            "Set Eltharion to Unrestricted in Android Battery settings so the OS doesn't stop Auto-Pause."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenBackgroundPriority,
                            border = BorderStroke(1.dp, SlateBorder),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("rules_open_background_priority_button")
                        ) {
                            Text(
                                text = "Battery Helper",
                                style = MaterialTheme.typography.labelLarge,
                                color = PureWhite
                            )
                        }
                        Button(
                            onClick = onOpenBatterySettings,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PureWhite,
                                contentColor = AbyssBlack
                            ),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("open_battery_settings_button")
                        ) {
                            Text(
                                text = "Battery Settings",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun DistancePresetChip(
    label: String,
    sublabel: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) PureWhite else SlateCardElevated)
            .border(1.dp, if (selected) PureWhite else SlateBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) AbyssBlack else TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = sublabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (selected) SlateCard else TextMuted,
            maxLines = 1
        )
    }
}
