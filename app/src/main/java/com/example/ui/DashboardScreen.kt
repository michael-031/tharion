package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ActivityLogEntry
import com.example.data.GuardUiState
import com.example.data.LogLevel
import com.example.data.SessionStatus
import com.example.ui.components.BatteryOptimizationHelperScreen
import com.example.ui.components.HairlineStatGrid
import com.example.ui.components.HiddenPortalWebView
import com.example.ui.components.QuickHelpSheet
import com.example.ui.components.RssiSignalGauge
import com.example.ui.components.SettingsScreen
import com.example.ui.components.StatCell
import com.example.ui.components.auroraDarkBackdrop
import com.example.ui.theme.AbyssBlack
import com.example.ui.theme.AmberCaution
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrackGray

private enum class GuardTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_guard"),
    LOGS("Activity", Icons.Filled.History, Icons.Outlined.History, "nav_logs")
}

@Composable
fun DashboardScreen(
    uiState: GuardUiState,
    showQuickTileGuideDialog: Boolean = false,
    onDismissQuickTileGuideDialog: () -> Unit = {},
    onAddQuickTile: () -> Unit = {},
    onRefreshPermissions: () -> Unit = {},
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenOemAutostart: () -> Unit = {},
    onSendBatteryGuideNotification: () -> Unit,
    onStartGuard: () -> Unit,
    onStopGuard: () -> Unit,
    onTestPause: () -> Unit,
    onTestResume: () -> Unit,
    onAdjustPauseThreshold: (Int) -> Unit,
    onSetPauseThreshold: (Int) -> Unit,
    onAdjustResumeThreshold: (Int) -> Unit,
    onToggleAutoResume: (Boolean) -> Unit,
    onToggleWebViewFallback: (Boolean) -> Unit,
    onRetryPortalNow: () -> Unit = {},
    onUpdatePortalUrl: (String) -> Unit,
    onUpdateExpiryMinutes: (Int) -> Unit,
    onClearLogs: () -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showSettingsScreen by rememberSaveable { mutableStateOf(false) }
    var showPortalConfigDialog by remember { mutableStateOf(false) }
    var showBatteryHelperScreen by rememberSaveable { mutableStateOf(false) }
    var showQuickHelpSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .auroraDarkBackdrop()
    ) {
        // Hidden 1dp x 1dp Method B WebView JS Injector
        HiddenPortalWebView()

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 720.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!isWide) {
                        MinimalBottomBar(
                            selectedIndex = selectedTabIndex,
                            onSelect = { selectedTabIndex = it }
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWide) {
                        NavigationRail(
                            containerColor = AbyssBlack,
                            contentColor = PureWhite
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            GuardTab.entries.forEachIndexed { index, tab ->
                                val selected = index == selectedTabIndex
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { selectedTabIndex = index },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = { Text(tab.title) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = PureWhite,
                                        selectedTextColor = PureWhite,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted,
                                        indicatorColor = SlateCardElevated
                                    ),
                                    modifier = Modifier.testTag(tab.tag)
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 620.dp)
                        ) {
                            AnimatedContent(
                                targetState = selectedTabIndex,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "tab_content"
                            ) { tabIndex ->
                                when (tabIndex) {
                                    0 -> GuardHomeTab(
                                        uiState = uiState,
                                        onOpenAppSettingsScreen = { showSettingsScreen = true },
                                        onOpenBatteryHelper = { showBatteryHelperScreen = true },
                                        onOpenQuickHelp = { showQuickHelpSheet = true },
                                        onRequestPermissions = onRequestPermissions,
                                        onOpenSystemAppSettings = onOpenSettings,
                                        onOpenBatterySettings = onOpenBatterySettings,
                                        onRetryPortalNow = onRetryPortalNow,
                                        onStartGuard = onStartGuard,
                                        onStopGuard = onStopGuard,
                                        onTestPause = onTestPause,
                                        onTestResume = onTestResume
                                    )

                                    else -> ActivityLogsTab(
                                        logs = uiState.logs,
                                        portalUrl = uiState.settings.portalBaseUrl,
                                        onClearLogs = onClearLogs
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSettingsScreen) {
        SettingsScreen(
            uiState = uiState,
            onAdjustPauseThreshold = onAdjustPauseThreshold,
            onSetPauseThreshold = onSetPauseThreshold,
            onAdjustResumeThreshold = onAdjustResumeThreshold,
            onToggleAutoResume = onToggleAutoResume,
            onToggleWebViewFallback = onToggleWebViewFallback,
            onOpenPortalConfig = { showPortalConfigDialog = true },
            onOpenBackgroundPriority = { showBatteryHelperScreen = true },
            onOpenBatterySettings = onOpenBatterySettings,
            onAddQuickTile = onAddQuickTile,
            onBack = { showSettingsScreen = false }
        )
    }

    if (showBatteryHelperScreen) {
        BatteryOptimizationHelperScreen(
            isBatteryOptimizationIgnored = uiState.isBatteryOptimizationIgnored,
            hasNotificationPermission = uiState.hasNotificationPermission,
            hasLocationPermission = uiState.hasLocationPermission,
            onRefreshStatus = onRefreshPermissions,
            onOpenBatterySettings = onOpenBatterySettings,
            onOpenAppSettings = onOpenSettings,
            onOpenOemAutostart = onOpenOemAutostart,
            onSendGuideNotification = onSendBatteryGuideNotification,
            onBack = { showBatteryHelperScreen = false }
        )
    }

    if (showPortalConfigDialog) {
        PortalConfigDialog(
            currentUrl = uiState.settings.portalBaseUrl,
            currentExpiryMinutes = uiState.settings.pauseExpiryMinutes,
            onDismiss = { showPortalConfigDialog = false },
            onSave = { url, mins ->
                onUpdatePortalUrl(url)
                onUpdateExpiryMinutes(mins)
                showPortalConfigDialog = false
            }
        )
    }

    if (showQuickHelpSheet) {
        QuickHelpSheet(
            onOpenBackgroundPriority = { showBatteryHelperScreen = true },
            onAddQuickTile = onAddQuickTile,
            onDismiss = { showQuickHelpSheet = false }
        )
    }

    if (showQuickTileGuideDialog) {
        QuickTileGuideDialog(onDismiss = onDismissQuickTileGuideDialog)
    }
}

@Composable
private fun MinimalBottomBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AbyssBlack)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = SlateBorder, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GuardTab.entries.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) SlateCardElevated else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (selected) SlateBorder else Color.Transparent,
                            shape = RoundedCornerShape(999.dp)
                        )
                        .clickable { onSelect(index) }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .testTag(tab.tag),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title,
                        tint = if (selected) PureWhite else TextMuted,
                        modifier = Modifier.size(21.dp)
                    )
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                        color = if (selected) PureWhite else TextMuted,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun GuardHomeTab(
    uiState: GuardUiState,
    onOpenAppSettingsScreen: () -> Unit,
    onOpenBatteryHelper: () -> Unit,
    onOpenQuickHelp: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenSystemAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onRetryPortalNow: () -> Unit,
    onStartGuard: () -> Unit,
    onStopGuard: () -> Unit,
    onTestPause: () -> Unit,
    onTestResume: () -> Unit
) {
    val telemetry = uiState.telemetry
    val isRunning = telemetry.isGuardRunning
    val status = telemetry.sessionStatus
    val statusColor = when (status) {
        SessionStatus.ACTIVE -> PureWhite
        SessionStatus.PAUSED -> AmberCaution
        SessionStatus.DISCONNECTED -> TextMuted
    }
    val statusLabel = when (status) {
        SessionStatus.ACTIVE -> "TIME RUNNING"
        SessionStatus.PAUSED -> "TIME PAUSED"
        SessionStatus.DISCONNECTED -> "NO WIFI"
    }
    val pausePresetLabel = when {
        uiState.settings.pauseThresholdDbm >= -70 -> "Close"
        uiState.settings.pauseThresholdDbm >= -78 -> "Balanced"
        else -> "Far"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Clean header: Session Status pill + Help & Settings buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .background(SlateCardElevated, RoundedCornerShape(999.dp))
                    .border(1.dp, SlateBorder, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("session_status_badge"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(statusColor, CircleShape)
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onOpenQuickHelp,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .background(SlateCardElevated, CircleShape)
                        .border(1.dp, SlateBorder, CircleShape)
                        .testTag("help_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "How It Works",
                        tint = TextPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onOpenAppSettingsScreen,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .background(SlateCardElevated, CircleShape)
                        .border(1.dp, SlateBorder, CircleShape)
                        .testTag("portal_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (telemetry.isWifiConnected) {
                    "WiFi: ${telemetry.ssid}"
                } else {
                    "Not connected to WiFi"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("connected_ssid_text")
            )
        }

        PermissionCallout(
            visible = !uiState.hasLocationPermission || !uiState.hasNotificationPermission,
            hasLocation = uiState.hasLocationPermission,
            onRequestPermissions = onRequestPermissions,
            onOpenSettings = onOpenSystemAppSettings
        )

        BatteryOptimizationBanner(
            visible = !uiState.isBatteryOptimizationIgnored,
            onOpenBackgroundPriority = onOpenBatteryHelper,
            onOpenBatterySettings = onOpenBatterySettings
        )

        SmartRetryBanner(
            retryAttempt = telemetry.portalRetryAttempt,
            nextRetrySeconds = telemetry.portalNextRetrySeconds,
            portalUrl = uiState.settings.portalBaseUrl,
            onRetryNow = onRetryPortalNow
        )

        PauseCountdownCard(uiState = uiState)

        // Hero Circular Signal Strength Ring
        RssiSignalGauge(
            rssiDbm = telemetry.rssiDbm,
            isWifiConnected = telemetry.isWifiConnected,
            pauseThresholdDbm = uiState.settings.pauseThresholdDbm,
            pauseStrikes = telemetry.pauseStrikes,
            resumeStrikes = telemetry.resumeStrikes
        )

        // Main Auto-Pause Toggle Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) PureWhite else SlateCardElevated)
                    .border(1.5.dp, if (isRunning) PureWhite else SlateBorder, CircleShape)
                    .clickable(onClick = if (isRunning) onStopGuard else onStartGuard)
                    .testTag("start_guard_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Check else Icons.Default.Shield,
                    contentDescription = if (isRunning) stringResource(R.string.guard_stop) else stringResource(R.string.guard_start),
                    tint = if (isRunning) AbyssBlack else PureWhite,
                    modifier = Modifier.size(23.dp)
                )
            }
            Text(
                text = if (isRunning) "Protection On • Tap to Stop" else stringResource(R.string.guard_start),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                color = if (isRunning) PureWhite else TextPrimary
            )
            Text(
                text = if (isRunning) "Watching your WiFi signal in the background" else "Auto-pauses when you walk away",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        // Compact 2x2 Status Grid
        val countdownStr = if (telemetry.sessionStatus == SessionStatus.PAUSED && telemetry.pauseCountdownSeconds > 0) {
            "%02d:%02d".format(telemetry.pauseCountdownSeconds / 60, telemetry.pauseCountdownSeconds % 60)
        } else {
            "${uiState.settings.pauseExpiryMinutes}m"
        }

        HairlineStatGrid(
            topLeft = StatCell(
                value = pausePresetLabel,
                caption = "Pause Distance"
            ),
            topRight = StatCell(
                value = if (telemetry.isWifiBound) "Ready" else "Waiting",
                caption = "Piso WiFi Link"
            ),
            bottomLeft = StatCell(
                value = if (uiState.settings.autoResumeEnabled) "When Back" else "Off",
                caption = "Auto-Resume"
            ),
            bottomRight = StatCell(
                value = countdownStr,
                caption = if (telemetry.sessionStatus == SessionStatus.PAUSED) "Pause Left" else "Max Pause"
            )
        )

        // Manual Pause / Resume Controls (only active when connected to WiFi and Protection is ON)
        val canUseManualControls = telemetry.isWifiConnected && isRunning && !telemetry.isExecutingPortalCommand
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onTestPause,
                    enabled = canUseManualControls,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = AbyssBlack,
                        disabledContainerColor = SlateCardElevated,
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("test_pause_button")
                ) {
                    if (telemetry.isExecutingPortalCommand) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AbyssBlack)
                    } else {
                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.test_pause), style = MaterialTheme.typography.labelLarge)
                    }
                }

                OutlinedButton(
                    onClick = onTestResume,
                    enabled = canUseManualControls,
                    border = BorderStroke(
                        1.dp,
                        if (canUseManualControls) SlateBorder else SlateBorder.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("test_resume_button")
                ) {
                    val resumeTint = if (canUseManualControls) PureWhite else TextMuted
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = resumeTint, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.test_resume), style = MaterialTheme.typography.labelLarge, color = resumeTint)
                }
            }

            if (!telemetry.isWifiConnected || !isRunning) {
                Text(
                    text = when {
                        !telemetry.isWifiConnected -> "Connect to WiFi and turn on Protection to pause or resume"
                        else -> "Turn on Protection above to enable Pause & Resume"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun ActivityLogsTab(
    logs: List<ActivityLogEntry>,
    portalUrl: String,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.activity_log_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Portal: $portalUrl",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            TextButton(
                onClick = onClearLogs,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("clear_log_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Clear History",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.clear_log), color = TextSecondary)
            }
        }

        HorizontalDivider(color = SlateBorder, thickness = 1.dp)

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No activity yet. Tap Protect My WiFi on Home to track pauses and resumes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("activity_log_list")
            ) {
                items(logs, key = { it.id }) { entry ->
                    val color = when (entry.level) {
                        LogLevel.SUCCESS -> PureWhite
                        LogLevel.WARN -> AmberCaution
                        LogLevel.ERROR -> RoseDanger
                        LogLevel.INFO -> TextSecondary
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = entry.timestamp,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text = entry.message,
                                style = MaterialTheme.typography.labelSmall,
                                color = color
                            )
                        }
                        HorizontalDivider(color = SlateBorder.copy(alpha = 0.45f), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryOptimizationBanner(
    visible: Boolean,
    onOpenBackgroundPriority: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SlateCard)
                .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                .clickable(onClick = onOpenBackgroundPriority)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Background Priority Needed",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "RECOMMENDED",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberCaution
                )
            }
            Text(
                text = "Disable battery optimization for Eltharion so background WiFi monitoring stays persistent.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenBackgroundPriority,
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("home_open_background_priority_button")
                ) {
                    Text("Battery Helper", style = MaterialTheme.typography.labelMedium, color = PureWhite)
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
                        .testTag("home_open_battery_settings_button")
                ) {
                    Text("Battery Settings", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun PermissionCallout(
    visible: Boolean,
    hasLocation: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SlateCardElevated)
                .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = "Permission Warning",
                    tint = AmberCaution
                )
                Text(
                    text = stringResource(R.string.permission_banner_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            Text(
                text = stringResource(R.string.permission_banner_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRequestPermissions,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = AbyssBlack
                    ),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("grant_permissions_button")
                ) {
                    Text(
                        text = if (hasLocation) "Allow Notifications" else stringResource(R.string.grant_permissions),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                OutlinedButton(
                    onClick = onOpenSettings,
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("open_app_settings_button")
                ) {
                    Text(
                        text = stringResource(R.string.open_settings),
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun PauseCountdownCard(uiState: GuardUiState) {
    val telemetry = uiState.telemetry
    val isPaused = telemetry.sessionStatus == SessionStatus.PAUSED && telemetry.pauseCountdownSeconds > 0
    val remaining = telemetry.pauseCountdownSeconds
    val total = telemetry.totalPauseLimitSeconds.coerceAtLeast(1)
    val progress = (remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val mins = remaining / 60
    val secs = remaining % 60
    val isUrgent = remaining <= 300
    val accent = if (isUrgent) RoseDanger else PureWhite

    AnimatedVisibility(
        visible = isPaused,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SlateCardElevated)
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                .padding(14.dp)
                .testTag("pause_countdown_card"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Pause Expiration Timer",
                        tint = accent
                    )
                    Column {
                        Text(
                            text = "Pause Expiration Timer",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isUrgent) "URGENT: < 5 min remaining" else "5-min expiry alert armed",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUrgent) RoseDanger else TextSecondary
                        )
                    }
                }

                Text(
                    text = "%02d:%02d".format(mins, secs),
                    style = MaterialTheme.typography.headlineMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = accent,
                trackColor = TrackGray
            )
        }
    }
}

@Composable
private fun PortalConfigDialog(
    currentUrl: String,
    currentExpiryMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var urlInput by remember { mutableStateOf(currentUrl) }
    var minutesInput by remember { mutableStateOf(currentExpiryMinutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = {
            Text(
                text = "Portal & Expiry Settings",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Piso WiFi Portal URL") },
                    placeholder = { Text("http://10.0.0.1") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("portal_url_input")
                )
                OutlinedTextField(
                    value = minutesInput,
                    onValueChange = { txt -> minutesInput = txt.filter { it.isDigit() } },
                    label = { Text("Pause Expiry Limit (Minutes)") },
                    placeholder = { Text("60") },
                    supportingText = {
                        Text(
                            text = "Matches your Piso WiFi vendo's max pause duration. Eltharion warns you 5 min before paused time expires.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expiry_minutes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = minutesInput.toIntOrNull()?.coerceIn(10, 240) ?: 60
                    onSave(urlInput, mins)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = AbyssBlack),
                modifier = Modifier.testTag("save_portal_config_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun QuickTileGuideDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateCard,
        title = {
            Text(
                text = "Add to Quick Settings",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "1. Swipe down twice from the top of your screen to open Quick Settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = "2. Tap the Edit (pencil) button.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = "3. Find \"Eltharion\" and drag it next to Internet & Bluetooth.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = AbyssBlack),
                modifier = Modifier.testTag("quick_tile_guide_ok_button")
            ) {
                Text("Got it")
            }
        }
    )
}

@Composable
private fun SmartRetryBanner(
    retryAttempt: Int,
    nextRetrySeconds: Int,
    portalUrl: String,
    onRetryNow: () -> Unit
) {
    AnimatedVisibility(
        visible = retryAttempt > 0 && nextRetrySeconds > 0,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SlateCard)
                .border(1.dp, AmberCaution.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("smart_retry_banner"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Smart Retry Backoff • Attempt #$retryAttempt",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberCaution
                )
                Text(
                    text = "$portalUrl unreachable • next check in ${nextRetrySeconds}s to save battery",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            TextButton(
                onClick = onRetryNow,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("retry_portal_now_button")
            ) {
                Text(
                    text = "Retry Now",
                    style = MaterialTheme.typography.labelMedium,
                    color = PureWhite
                )
            }
        }
    }
}
