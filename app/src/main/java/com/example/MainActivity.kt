package com.example

import android.Manifest
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.GuardStateBus
import com.example.data.LogLevel
import com.example.service.EltharionTileService
import com.example.ui.DashboardScreen
import com.example.ui.GuardViewModel
import com.example.ui.theme.LpbGuardTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_BATTERY_SETTINGS = "com.example.extra.OPEN_BATTERY_SETTINGS"
        const val EXTRA_AUTO_START_GUARD = "com.example.extra.AUTO_START_GUARD"
    }

    private var pendingAutoStartFromTile by mutableStateOf(false)
    private var showLegacyTileGuide by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            LpbGuardTheme {
                val viewModel: GuardViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshPermissions()
                            EltharionTileService.requestTileUpdate(applicationContext)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    viewModel.refreshPermissions()
                    if (pendingAutoStartFromTile && hasAnyLocationPermission()) {
                        pendingAutoStartFromTile = false
                        viewModel.startGuardService()
                    }
                }

                val requestRequiredPermissions = {
                    val permissions = buildList {
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray()
                    permissionLauncher.launch(permissions)
                }

                LaunchedEffect(Unit) {
                    viewModel.refreshPermissions()
                    if (!uiState.hasLocationPermission || !uiState.hasNotificationPermission) {
                        requestRequiredPermissions()
                    }
                }

                LaunchedEffect(pendingAutoStartFromTile, uiState.hasLocationPermission) {
                    if (pendingAutoStartFromTile) {
                        if (hasAnyLocationPermission()) {
                            pendingAutoStartFromTile = false
                            viewModel.startGuardService()
                        } else {
                            requestRequiredPermissions()
                        }
                    }
                }

                DashboardScreen(
                    uiState = uiState,
                    showQuickTileGuideDialog = showLegacyTileGuide,
                    onDismissQuickTileGuideDialog = { showLegacyTileGuide = false },
                    onAddQuickTile = ::requestAddQuickSettingsTile,
                    onRefreshPermissions = viewModel::refreshPermissions,
                    onRequestPermissions = requestRequiredPermissions,
                    onOpenSettings = ::openAppDetailsSettings,
                    onOpenBatterySettings = ::openBatteryOptimizationSettings,
                    onOpenOemAutostart = ::openOemAutostartSettings,
                    onSendBatteryGuideNotification = viewModel::sendBatteryOptimizationGuide,
                    onStartGuard = {
                        if (!uiState.hasLocationPermission) {
                            requestRequiredPermissions()
                        } else {
                            viewModel.startGuardService()
                        }
                    },
                    onStopGuard = viewModel::stopGuardService,
                    onTestPause = viewModel::testPause,
                    onTestResume = viewModel::testResume,
                    onAdjustPauseThreshold = viewModel::adjustPauseThreshold,
                    onSetPauseThreshold = viewModel::setPauseThreshold,
                    onAdjustResumeThreshold = viewModel::adjustResumeThreshold,
                    onToggleAutoResume = viewModel::toggleAutoResume,
                    onToggleWebViewFallback = viewModel::toggleWebViewFallback,
                    onRetryPortalNow = viewModel::retryPortalNow,
                    onUpdatePortalUrl = viewModel::updatePortalUrl,
                    onUpdateExpiryMinutes = viewModel::updateExpiryMinutes,
                    onClearLogs = viewModel::clearLogs
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(EXTRA_OPEN_BATTERY_SETTINGS, false)) {
            intent.removeExtra(EXTRA_OPEN_BATTERY_SETTINGS)
            openBatteryOptimizationSettings()
        }
        if (intent.getBooleanExtra(EXTRA_AUTO_START_GUARD, false)) {
            intent.removeExtra(EXTRA_AUTO_START_GUARD)
            pendingAutoStartFromTile = true
        }
    }

    private fun hasAnyLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun requestAddQuickSettingsTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val statusBarManager = getSystemService(StatusBarManager::class.java)
            if (statusBarManager != null) {
                try {
                    statusBarManager.requestAddTileService(
                        ComponentName(this, EltharionTileService::class.java),
                        getString(R.string.quick_tile_label),
                        Icon.createWithResource(this, R.drawable.ic_qs_eltharion),
                        mainExecutor
                    ) { result ->
                        val message = when (result) {
                            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                                "Eltharion tile added to Quick Settings!"
                            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                                "Eltharion is already in your Quick Settings."
                            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
                                "Tile not added. You can add it anytime from Quick Settings -> Edit."
                            else ->
                                "Swipe down twice and tap Edit to add Eltharion."
                        }
                        GuardStateBus.appendLog(message, LogLevel.INFO)
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                    return
                } catch (_: Exception) {
                }
            }
        }
        showLegacyTileGuide = true
    }

    private fun openAppDetailsSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        startActivity(intent)
    }

    private fun openBatteryOptimizationSettings() {
        try {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (_: Exception) {
            openAppDetailsSettings()
        }
    }

    private fun openOemAutostartSettings() {
        val candidates = listOf(
            ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
            ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
        )
        for (component in candidates) {
            try {
                startActivity(Intent().setComponent(component))
                return
            } catch (_: Exception) {
            }
        }
        openAppDetailsSettings()
    }
}
