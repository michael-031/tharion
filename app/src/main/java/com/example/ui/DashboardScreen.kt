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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.ui.components.InsightProgressBarRow
import com.example.ui.components.MiniRssiTrendChart60s
import com.example.ui.components.MonthlySavingsTab
import com.example.ui.components.QuickHelpSheet
import com.example.ui.components.RssiHistoryLineChart
import com.example.ui.components.RssiSignalGauge
import com.example.ui.components.SegmentedPillToggle
import com.example.ui.components.SignalZoneConcentricRings
import com.example.ui.components.SimulationBanner
import com.example.ui.components.SimulationLabCard
import com.example.ui.components.StatCell
import com.example.ui.components.auroraDarkBackdrop
import com.example.ui.components.dbmToSignalPercent
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
    GUARD("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_guard"),
    SAVINGS("Saved", Icons.Filled.HourglassBottom, Icons.Outlined.HourglassEmpty, "nav_savings"),
    TELEMETRY("Signal", Icons.Filled.ShowChart, Icons.Outlined.ShowChart, "nav_telemetry"),
    RULES("Settings", Icons.Filled.Tune, Icons.Outlined.Tune, "nav_rules"),
    LOGS("History", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "nav_logs")
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
    onToggleSimpleMode: (Boolean) -> Unit,
    onToggleSimulationMode: (Boolean) -> Unit,
    onSetSimulatedRssi: (Int) -> Unit,
    onToggleSimulateUnreachable: (Boolean) -> Unit = {},
    onRetryPortalNow: () -> Unit = {},
    onFastForwardExpiry: () -> Unit,
    onUpdatePortalUrl: (String) -> Unit,
    onUpdateExpiryMinutes: (Int) -> Unit,
    onClearLogs: () -> Unit,
    onResetMonthlySavings: () -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showPortalConfigDialog by remember { mutableStateOf(false) }
    var showBackgroundPrioritySheet by remember { mutableStateOf(false) }
    var showQuickHelpSheet by remember { mutableStateOf(false) }
    val isSimpleMode = uiState.settings.simpleModeEnabled

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
                    if (!isWide && !isSimpleMode) {
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
                    if (isWide && !isSimpleMode) {
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
                                targetState = if (isSimpleMode) -1 else selectedTabIndex,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "tab_content"
                            ) { tabIndex ->
                                when (tabIndex) {
                                    -1 -> SimpleModeScreen(
                                        uiState = uiState,
                                        onToggleSimpleMode = onToggleSimpleMode,
                                        onRequestPermissions = onRequestPermissions,
                                        onOpenSettings = onOpenSettings,
                                        onPauseResumeClick = {
                                            if (uiState.telemetry.sessionStatus == SessionStatus.PAUSED) {
                                                onTestResume()
                                            } else {
                                                onTestPause()
                                            }
                                        },
                                        onToggleGuard = {
                                            if (uiState.telemetry.isGuardRunning) {
                                                onStopGuard()
                                            } else {
                                                onStartGuard()
                                            }
                                        }
                                    )

                                    0 -> GuardHomeTab(
                                        uiState = uiState,
                                        onOpenConfig = { showPortalConfigDialog = true },
                                        onOpenBackgroundPriority = { showBackgroundPrioritySheet = true },
                                        onOpenQuickHelp = { showQuickHelpSheet = true },
                                        onToggleSimpleMode = onToggleSimpleMode,
                                        onRequestPermissions = onRequestPermissions,
                                        onOpenSettings = onOpenSettings,
                                        onOpenBatterySettings = onOpenBatterySettings,
                                        onRetryPortalNow = onRetryPortalNow,
                                        onStartGuard = onStartGuard,
                                        onStopGuard = onStopGuard,
                                        onTestPause = onTestPause,
                                        onTestResume = onTestResume
                                    )

                                    1 -> MonthlySavingsTab(
                                        uiState = uiState,
                                        onResetMonthlySavings = onResetMonthlySavings
                                    )

                                    2 -> TelemetryChartTab(
                                        uiState = uiState,
                                        onToggleSimulationMode = onToggleSimulationMode,
                                        onSetSimulatedRssi = onSetSimulatedRssi,
                                        onToggleSimulateUnreachable = onToggleSimulateUnreachable,
                                        onRetryPortalNow = onRetryPortalNow,
                                        onFastForwardExpiry = onFastForwardExpiry
                                    )

                                    3 -> RulesInsightsTab(
                                        uiState = uiState,
                                        onAdjustPauseThreshold = onAdjustPauseThreshold,
                                        onSetPauseThreshold = onSetPauseThreshold,
                                        onAdjustResumeThreshold = onAdjustResumeThreshold,
                                        onToggleAutoResume = onToggleAutoResume,
                                        onToggleWebViewFallback = onToggleWebViewFallback,
                                        onToggleSimpleMode = onToggleSimpleMode,
                                        onOpenPortalConfig = { showPortalConfigDialog = true },
                                        onOpenBackgroundPriority = { showBackgroundPrioritySheet = true },
                                        onOpenBatterySettings = onOpenBatterySettings,
                                        onAddQuickTile = onAddQuickTile
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

    if (showBackgroundPrioritySheet) {
        BatteryOptimizationHelperScreen(
            isBatteryOptimizationIgnored = uiState.isBatteryOptimizationIgnored,
            hasNotificationPermission = uiState.hasNotificationPermission,
            hasLocationPermission = uiState.hasLocationPermission,
            onRefreshStatus = onRefreshPermissions,
            onOpenBatterySettings = onOpenBatterySettings,
            onOpenAppSettings = onOpenSettings,
            onOpenOemAutostart = onOpenOemAutostart,
            onSendGuideNotification = onSendBatteryGuideNotification,
            onBack = { showBackgroundPrioritySheet = false }
        )
    }

    if (showQuickHelpSheet) {
        QuickHelpSheet(
            onTryPracticeMode = {
                onToggleSimulationMode(true)
                selectedTabIndex = 2
            },
            onOpenBackgroundPriority = { showBackgroundPrioritySheet = true },
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
                .height(60.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GuardTab.entries.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                IconButton(
                    onClick = { onSelect(index) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag(tab.tag)
                ) {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title,
                        tint = if (selected) PureWhite else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * TAB 1: Screen 1 Aesthetic — Circular White Ring Gauge, Circular Guard Toggle Button,
 * 2x2 Hairline Stat Grid, and Pill Action Controls.
 */
@Composable
private fun SimpleModeScreen(
    uiState: GuardUiState,
    onToggleSimpleMode: (Boolean) -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onPauseResumeClick: () -> Unit,
    onToggleGuard: () -> Unit
) {
    val telemetry = uiState.telemetry
    val status = telemetry.sessionStatus
    val isPaused = status == SessionStatus.PAUSED
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
    val countdownStr = if (isPaused && telemetry.pauseCountdownSeconds > 0) {
        "%02d:%02d left".format(telemetry.pauseCountdownSeconds / 60, telemetry.pauseCountdownSeconds % 60)
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header: Status + Simple Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .background(SlateCardElevated, RoundedCornerShape(999.dp))
                    .border(1.dp, SlateBorder, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("simple_connection_status_badge"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }

            SimpleModeToggleChip(
                enabled = true,
                onToggle = { onToggleSimpleMode(false) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // App Title & Current Connection Status
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Text(
                text = when {
                    !telemetry.isWifiConnected -> "Not connected to WiFi"
                    countdownStr != null -> "${telemetry.ssid} • $countdownStr"
                    else -> "Connected to ${telemetry.ssid}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPaused) AmberCaution else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("simple_connection_subtitle")
            )
        }

        PermissionCallout(
            visible = !uiState.simulation.enabled && !uiState.hasLocationPermission,
            hasLocation = uiState.hasLocationPermission,
            onRequestPermissions = onRequestPermissions,
            onOpenSettings = onOpenSettings
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Large Signal Strength Indicator
        RssiSignalGauge(
            rssiDbm = telemetry.rssiDbm,
            isWifiConnected = telemetry.isWifiConnected,
            pauseThresholdDbm = uiState.settings.pauseThresholdDbm,
            pauseStrikes = telemetry.pauseStrikes,
            resumeStrikes = telemetry.resumeStrikes,
            gaugeSize = 264.dp,
            modifier = Modifier.testTag("simple_signal_gauge")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Prominent Pause / Resume Primary Action + Secondary Auto-Guard Toggle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPauseResumeClick,
                enabled = !telemetry.isExecutingPortalCommand,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = AbyssBlack
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("simple_pause_resume_button")
            ) {
                if (telemetry.isExecutingPortalCommand) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = AbyssBlack
                    )
                } else {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isPaused) "Resume WiFi Time" else "Pause WiFi Time",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            OutlinedButton(
                onClick = onToggleGuard,
                border = BorderStroke(
                    1.dp,
                    if (telemetry.isGuardRunning) PureWhite.copy(alpha = 0.7f) else SlateBorder
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("simple_auto_guard_button")
            ) {
                Icon(
                    imageVector = if (telemetry.isGuardRunning) Icons.Default.Check else Icons.Default.Shield,
                    contentDescription = null,
                    tint = PureWhite,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (telemetry.isGuardRunning) "Auto-Pause Protection: ON" else "Turn On Auto-Pause",
                    style = MaterialTheme.typography.labelLarge,
                    color = PureWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SimpleModeToggleChip(
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (enabled) PureWhite else SlateCardElevated)
            .border(1.dp, if (enabled) PureWhite else SlateBorder, RoundedCornerShape(999.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("simple_mode_toggle"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = if (enabled) "Simple: ON" else "Simple",
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) AbyssBlack else TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * TAB 1: Screen 1 Aesthetic — Circular White Ring Gauge, Circular Guard Toggle Button,
 * 2x2 Hairline Stat Grid, and Pill Action Controls.
 */
@Composable
private fun GuardHomeTab(
    uiState: GuardUiState,
    onOpenConfig: () -> Unit,
    onOpenBackgroundPriority: () -> Unit,
    onOpenQuickHelp: () -> Unit,
    onToggleSimpleMode: (Boolean) -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
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
        // Top minimal header matching Screen 1 (status badge + Simple + Help + Priority + settings)
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SimpleModeToggleChip(
                    enabled = false,
                    onToggle = { onToggleSimpleMode(true) }
                )

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
                    onClick = onOpenConfig,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .background(SlateCardElevated, CircleShape)
                        .border(1.dp, SlateBorder, CircleShape)
                        .testTag("portal_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Portal Settings",
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

        SimulationBanner(visible = uiState.simulation.enabled)

        PermissionCallout(
            visible = !uiState.simulation.enabled && (!uiState.hasLocationPermission || !uiState.hasNotificationPermission),
            hasLocation = uiState.hasLocationPermission,
            onRequestPermissions = onRequestPermissions,
            onOpenSettings = onOpenSettings
        )

        BatteryOptimizationBanner(
            visible = !uiState.isBatteryOptimizationIgnored,
            onOpenBackgroundPriority = onOpenBackgroundPriority,
            onOpenBatterySettings = onOpenBatterySettings
        )

        SmartRetryBanner(
            retryAttempt = telemetry.portalRetryAttempt,
            nextRetrySeconds = telemetry.portalNextRetrySeconds,
            portalUrl = uiState.settings.portalBaseUrl,
            onRetryNow = onRetryPortalNow
        )

        PauseCountdownCard(uiState = uiState)

        // Hero Circular Ring Gauge (Screen 1)
        RssiSignalGauge(
            rssiDbm = telemetry.rssiDbm,
            isWifiConnected = telemetry.isWifiConnected,
            pauseThresholdDbm = uiState.settings.pauseThresholdDbm,
            pauseStrikes = telemetry.pauseStrikes,
            resumeStrikes = telemetry.resumeStrikes
        )

        // Circular Toggle Button matching Screen 1's "Done" button
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

        // 2x2 Hairline Stat Grid (Screen 2 style)
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

        // Compact 60-second RSSI trend line chart (local device memory only)
        MiniRssiTrendChart60s(
            samples = telemetry.rssiSamples,
            pauseThresholdDbm = uiState.settings.pauseThresholdDbm
        )

        // Manual Pause / Resume Monochrome Pill Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onTestPause,
                enabled = !telemetry.isExecutingPortalCommand,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = AbyssBlack
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
                enabled = !telemetry.isExecutingPortalCommand,
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("test_resume_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PureWhite, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.test_resume), style = MaterialTheme.typography.labelLarge, color = PureWhite)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

/**
 * TAB 2: Screen 2 Aesthetic — Segmented Pill ("Live | Practice"),
 * Line Chart with White Floating Tooltip, 2x2 Hairline Stat Grid, and Practice Controls.
 */
@Composable
private fun TelemetryChartTab(
    uiState: GuardUiState,
    onToggleSimulationMode: (Boolean) -> Unit,
    onSetSimulatedRssi: (Int) -> Unit,
    onToggleSimulateUnreachable: (Boolean) -> Unit,
    onRetryPortalNow: () -> Unit,
    onFastForwardExpiry: () -> Unit
) {
    val selectedSegment = if (uiState.simulation.enabled) 1 else 0
    val history = uiState.telemetry.rssiHistory
    val avgPct = if (history.isNotEmpty()) "${dbmToSignalPercent(history.average().toInt())}%" else "--"
    val minPct = if (history.isNotEmpty()) "${dbmToSignalPercent(history.min())}%" else "--"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SegmentedPillToggle(
            options = listOf("Live", "Practice"),
            selectedIndex = selectedSegment,
            onSelect = { idx -> onToggleSimulationMode(idx == 1) }
        )

        SmartRetryBanner(
            retryAttempt = uiState.telemetry.portalRetryAttempt,
            nextRetrySeconds = uiState.telemetry.portalNextRetrySeconds,
            portalUrl = uiState.settings.portalBaseUrl,
            onRetryNow = onRetryPortalNow
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (uiState.simulation.enabled) "Practice Signal" else "Live Signal",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                text = if (uiState.telemetry.isWifiConnected) "Updates every 2s" else "Waiting for WiFi",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        RssiHistoryLineChart(
            history = history,
            pauseThresholdDbm = uiState.settings.pauseThresholdDbm
        )

        HairlineStatGrid(
            topLeft = StatCell(value = avgPct, caption = "Avg Signal"),
            topRight = StatCell(value = minPct, caption = "Weakest"),
            bottomLeft = StatCell(value = "${uiState.telemetry.pauseStrikes}/2", caption = "Pause Checks"),
            bottomRight = StatCell(value = "${uiState.telemetry.resumeStrikes}/2", caption = "Resume Checks")
        )

        SimulationLabCard(
            simulation = uiState.simulation,
            sessionStatus = uiState.telemetry.sessionStatus,
            pauseThresholdDbm = uiState.settings.pauseThresholdDbm,
            onToggleSimulation = onToggleSimulationMode,
            onSetSimulatedRssi = onSetSimulatedRssi,
            onToggleSimulateUnreachable = onToggleSimulateUnreachable,
            onFastForwardExpiry = onFastForwardExpiry
        )

        Spacer(modifier = Modifier.height(6.dp))
    }
}

/**
 * TAB 3: Screen 3 Aesthetic — Concentric 3-Ring Signal Zone Distribution +
 * Insight Horizontal Bars & Automation Controls.
 */
@Composable
private fun RulesInsightsTab(
    uiState: GuardUiState,
    onAdjustPauseThreshold: (Int) -> Unit,
    onSetPauseThreshold: (Int) -> Unit,
    onAdjustResumeThreshold: (Int) -> Unit,
    onToggleAutoResume: (Boolean) -> Unit,
    onToggleWebViewFallback: (Boolean) -> Unit,
    onToggleSimpleMode: (Boolean) -> Unit,
    onOpenPortalConfig: () -> Unit,
    onOpenBackgroundPriority: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onAddQuickTile: () -> Unit
) {
    val settings = uiState.settings
    val pauseFraction = ((settings.pauseThresholdDbm + 95f) / 50f).coerceIn(0.1f, 0.95f)
    val resumeFraction = ((settings.resumeThresholdDbm + 95f) / 50f).coerceIn(0.1f, 0.95f)
    var showAdvancedSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Simple Mode Toggle Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SlateCardElevated)
                .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Simple Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Hide charts, logs & settings—show only Pause/Resume and signal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Switch(
                checked = settings.simpleModeEnabled,
                onCheckedChange = onToggleSimpleMode,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AbyssBlack,
                    checkedTrackColor = PureWhite,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = SlateCard
                ),
                modifier = Modifier.testTag("settings_simple_mode_switch")
            )
        }

        // Quick Settings Tile Card (one-tap toggle from Android top shade)
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

        Text(
            text = "Signal Summary",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        SignalZoneConcentricRings(
            history = uiState.telemetry.rssiHistory,
            pauseThresholdDbm = settings.pauseThresholdDbm
        )

        HorizontalDivider(color = SlateBorder, thickness = 1.dp)

        Text(
            text = stringResource(R.string.threshold_title),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        // One-Tap Distance Presets for non-tech-savvy users
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

        // Fine adjustment row
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

        // Auto-Resume Row
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

        // Collapsible Advanced Portal Settings so casual users aren't overwhelmed
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SlateCardElevated)
                .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAdvancedSettings = !showAdvancedSettings }
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("advanced_portal_toggle"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Advanced Portal Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${settings.portalBaseUrl} • Expiry ${settings.pauseExpiryMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
                Icon(
                    imageVector = if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (showAdvancedSettings) "Hide Advanced Settings" else "Show Advanced Settings",
                    tint = TextSecondary
                )
            }

            AnimatedVisibility(visible = showAdvancedSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = SlateBorder, thickness = 1.dp)

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
                                text = "Uses a hidden browser fallback if your Piso WiFi uses a custom page",
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SlateCard)
                            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenPortalConfig)
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Piso WiFi Portal Address",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Text(
                                text = settings.portalBaseUrl,
                                style = MaterialTheme.typography.labelMedium,
                                color = PureWhite
                            )
                        }
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelMedium,
                            color = PureWhite
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = SlateBorder, thickness = 1.dp)

        // Background Reliability & Battery Optimization Guide
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
                    "Battery optimization is disabled for Eltharion. Background RSSI monitoring stays active."
                } else {
                    "Set Eltharion to Unrestricted in Android Battery settings so the OS doesn't pause RSSI checks."
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

        Spacer(modifier = Modifier.height(6.dp))
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

/**
 * TAB 4: Clean Monochrome Activity History
 */
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
                    label = { Text("LPB Portal Gateway URL") },
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
