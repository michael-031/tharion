package com.example.ui

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GuardPreferencesRepository
import com.example.data.GuardSettings
import com.example.data.GuardStateBus
import com.example.data.GuardUiState
import com.example.data.LogLevel
import com.example.data.PortalAction
import com.example.data.SessionStatus
import com.example.domain.GuardDecision
import com.example.domain.RssiGuardEngine
import com.example.network.LpbPortalController
import com.example.network.PortalExecutionResult
import com.example.network.WifiNetworkBinder
import com.example.service.DeviceFeedbackHelper
import com.example.service.WifiGuardService
import com.example.service.WifiTelemetryReader
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GuardViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefsRepo = GuardPreferencesRepository(appContext)
    private val telemetryReader = WifiTelemetryReader(appContext)
    private val portalController = LpbPortalController(appContext)
    private val wifiBinder = WifiNetworkBinder(appContext)
    private val feedbackHelper = DeviceFeedbackHelper(appContext)
    private val simGuardEngine = RssiGuardEngine()

    private val _permissionState = MutableStateFlow(readPermissions())
    private var localCountdownJob: Job? = null
    private var localSimGuardJob: Job? = null

    val uiState: StateFlow<GuardUiState> = combine(
        GuardStateBus.telemetry,
        combine(prefsRepo.settingsFlow, prefsRepo.savingsFlow, ::Pair),
        GuardStateBus.simulationConfig,
        GuardStateBus.logs,
        _permissionState
    ) { telemetry, (settings, savings), simulation, logs, perms ->
        GuardUiState(
            telemetry = telemetry,
            settings = settings,
            savings = savings,
            simulation = simulation,
            logs = logs,
            hasLocationPermission = perms.first,
            hasNotificationPermission = perms.second,
            isBatteryOptimizationIgnored = perms.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GuardUiState(
            settings = GuardSettings(),
            hasLocationPermission = _permissionState.value.first,
            hasNotificationPermission = _permissionState.value.second,
            isBatteryOptimizationIgnored = _permissionState.value.third
        )
    )

    init {
        if (GuardStateBus.logs.value.isEmpty()) {
            GuardStateBus.appendLog(
                "Eltharion is ready. Connect to Piso WiFi and tap Protect My WiFi.",
                LogLevel.INFO
            )
        }
        startForegroundPreviewPolling()
    }

    private fun readPermissions(): Triple<Boolean, Boolean, Boolean> {
        val locationGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val notificationGranted = feedbackHelper.hasNotificationPermission()
        val batteryUnrestricted = feedbackHelper.isIgnoringBatteryOptimizations()
        return Triple(locationGranted, notificationGranted, batteryUnrestricted)
    }

    fun refreshPermissions() {
        _permissionState.value = readPermissions()
    }

    fun sendBatteryOptimizationGuide() {
        refreshPermissions()
        val sent = feedbackHelper.sendBatteryOptimizationGuideNotification()
        if (sent) {
            GuardStateBus.appendLog(
                "Battery optimization guide notification posted. Tap it to set Eltharion to Unrestricted.",
                LogLevel.INFO
            )
        } else {
            GuardStateBus.appendLog(
                "Grant Notification permission to display the battery optimization guide notification.",
                LogLevel.WARN
            )
        }
    }

    private fun startForegroundPreviewPolling() {
        viewModelScope.launch {
            while (isActive) {
                if (!GuardStateBus.telemetry.value.isGuardRunning) {
                    val sim = GuardStateBus.simulationConfig.value
                    if (sim.enabled) {
                        GuardStateBus.updateTelemetry { current ->
                            val nextStatus = if (current.sessionStatus == SessionStatus.DISCONNECTED) {
                                SessionStatus.ACTIVE
                            } else {
                                current.sessionStatus
                            }
                            val nextSamples = GuardStateBus.trimAndAppendSample(
                                existing = current.rssiSamples,
                                rssiDbm = sim.rssiDbm,
                                isConnected = true
                            )
                            current.copy(
                                ssid = "LPB_PisoWiFi_SIM",
                                bssid = "02:10:00:01:5A:FE",
                                rssiDbm = sim.rssiDbm,
                                isWifiConnected = true,
                                isWifiBound = true,
                                sessionStatus = nextStatus,
                                isSimulated = true,
                                rssiSamples = nextSamples,
                                rssiHistory = nextSamples.map { it.rssiDbm }
                            )
                        }
                    } else {
                        val reading = telemetryReader.readCurrentWifi()
                        val bound = if (reading.isConnected) wifiBinder.bindProcessToWifi() != null else false
                        GuardStateBus.updateTelemetry { current ->
                            val nextStatus = when {
                                !reading.isConnected -> SessionStatus.DISCONNECTED
                                current.sessionStatus == SessionStatus.DISCONNECTED -> SessionStatus.ACTIVE
                                else -> current.sessionStatus
                            }
                            val nextSamples = GuardStateBus.trimAndAppendSample(
                                existing = current.rssiSamples,
                                rssiDbm = reading.rssiDbm,
                                isConnected = reading.isConnected
                            )
                            current.copy(
                                ssid = reading.ssid,
                                bssid = reading.bssid,
                                rssiDbm = reading.rssiDbm,
                                isWifiConnected = reading.isConnected,
                                isWifiBound = bound,
                                sessionStatus = nextStatus,
                                isSimulated = false,
                                rssiSamples = nextSamples,
                                rssiHistory = nextSamples.map { it.rssiDbm }
                            )
                        }
                    }
                    GuardStateBus.syncRetryTelemetry()
                }
                delay(2000L)
            }
        }
    }

    fun toggleSimulationMode(enabled: Boolean) {
        GuardStateBus.setSimulationEnabled(enabled)
        simGuardEngine.reset()
        if (enabled) {
            val simRssi = GuardStateBus.simulationConfig.value.rssiDbm
            GuardStateBus.updateTelemetry {
                it.copy(
                    ssid = "LPB_PisoWiFi_SIM",
                    bssid = "02:10:00:01:5A:FE",
                    rssiDbm = simRssi,
                    isWifiConnected = true,
                    isWifiBound = true,
                    sessionStatus = if (it.sessionStatus == SessionStatus.DISCONNECTED) SessionStatus.ACTIVE else it.sessionStatus,
                    isSimulated = true
                )
            }
            GuardStateBus.appendLog(
                "Simulation Mode ENABLED. Using virtual LPB portal & adjustable RSSI ($simRssi dBm).",
                LogLevel.WARN
            )
        } else {
            if (localSimGuardJob?.isActive == true) {
                localSimGuardJob?.cancel()
                GuardStateBus.updateTelemetry {
                    it.copy(
                        isGuardRunning = false,
                        pauseStrikes = 0,
                        resumeStrikes = 0,
                        isSimulated = false
                    )
                }
            } else {
                GuardStateBus.updateTelemetry { it.copy(isSimulated = false) }
            }
            GuardStateBus.appendLog("Simulation Mode DISABLED. Returned to live WiFi telemetry.", LogLevel.INFO)
        }
    }

    fun setSimulatedRssi(rssiDbm: Int) {
        val clamped = rssiDbm.coerceIn(-95, -40)
        GuardStateBus.setSimulatedRssiDbm(clamped)
        if (GuardStateBus.simulationConfig.value.enabled) {
            GuardStateBus.updateTelemetry { it.copy(rssiDbm = clamped) }
        }
    }

    fun toggleSimulateUnreachablePortal(unreachable: Boolean) {
        GuardStateBus.setSimulateUnreachablePortal(unreachable)
        GuardStateBus.appendLog(
            if (unreachable) {
                "[SIM] Unreachable 10.0.0.1 mode ENABLED to test Smart Retry backoff."
            } else {
                "[SIM] Unreachable 10.0.0.1 mode DISABLED; Smart Retry reset."
            },
            LogLevel.INFO
        )
    }

    fun retryPortalNow() {
        GuardStateBus.resetPortalRetry()
        GuardStateBus.appendLog("Smart Retry cooldown cleared manually — ready for immediate portal check.", LogLevel.INFO)
    }

    fun fastForwardExpiryForTest() {
        if (GuardStateBus.telemetry.value.sessionStatus == SessionStatus.PAUSED) {
            GuardStateBus.updateTelemetry {
                it.copy(
                    pauseCountdownSeconds = 305,
                    expiryWarningSent = false
                )
            }
            GuardStateBus.appendLog(
                "[SIM] Fast-forwarded pause timer to 05:05 (5-min expiry alert will fire in 5s).",
                LogLevel.WARN
            )
        }
    }

    fun startGuardService() {
        refreshPermissions()
        val simEnabled = GuardStateBus.simulationConfig.value.enabled
        if (_permissionState.value.first) {
            localCountdownJob?.cancel()
            localSimGuardJob?.cancel()
            WifiGuardService.start(appContext)
            return
        }

        if (simEnabled) {
            startInAppSimulationGuard()
        } else {
            GuardStateBus.appendLog(
                "Location permission is required before starting the RSSI Guard service (or enable Simulation Mode).",
                LogLevel.WARN
            )
        }
    }

    private fun startInAppSimulationGuard() {
        localSimGuardJob?.cancel()
        simGuardEngine.reset()
        GuardStateBus.updateTelemetry {
            it.copy(
                isGuardRunning = true,
                isSimulated = true,
                sessionStatus = if (it.sessionStatus == SessionStatus.DISCONNECTED) SessionStatus.ACTIVE else it.sessionStatus
            )
        }
        GuardStateBus.appendLog(
            "[SIM] In-app RSSI Guard started (2s polling, 2-strike debounce).",
            LogLevel.SUCCESS
        )

        localSimGuardJob = viewModelScope.launch {
            while (isActive) {
                val sim = GuardStateBus.simulationConfig.value
                val settings = uiState.value.settings
                if (!sim.enabled) break

                val currentStatus = GuardStateBus.telemetry.value.sessionStatus.let {
                    if (it == SessionStatus.DISCONNECTED) SessionStatus.ACTIVE else it
                }

                val canAutoContact = GuardStateBus.portalRetryPolicy.shouldAttemptAutoRetry()
                val decision = if (canAutoContact) {
                    simGuardEngine.evaluate(
                        rssiDbm = sim.rssiDbm,
                        isWifiConnected = true,
                        currentStatus = currentStatus,
                        pauseThresholdDbm = settings.pauseThresholdDbm,
                        resumeThresholdDbm = settings.resumeThresholdDbm,
                        autoResumeEnabled = settings.autoResumeEnabled
                    )
                } else {
                    simGuardEngine.reset()
                    GuardDecision.NoOp
                }

                GuardStateBus.updateTelemetry {
                    val nextSamples = GuardStateBus.trimAndAppendSample(
                        existing = it.rssiSamples,
                        rssiDbm = sim.rssiDbm,
                        isConnected = true
                    )
                    it.copy(
                        ssid = "LPB_PisoWiFi_SIM",
                        bssid = "02:10:00:01:5A:FE",
                        rssiDbm = sim.rssiDbm,
                        isWifiConnected = true,
                        isWifiBound = true,
                        sessionStatus = currentStatus,
                        pauseStrikes = simGuardEngine.pauseStrikes,
                        resumeStrikes = simGuardEngine.resumeStrikes,
                        isSimulated = true,
                        rssiSamples = nextSamples,
                        rssiHistory = nextSamples.map { sample -> sample.rssiDbm }
                    )
                }
                GuardStateBus.syncRetryTelemetry()

                if (canAutoContact) {
                    if (simGuardEngine.pauseStrikes == 1) {
                        GuardStateBus.appendLog(
                            "[SIM] Strike 1/2: Signal dropped to ${sim.rssiDbm} dBm (<= ${settings.pauseThresholdDbm} dBm).",
                            LogLevel.WARN
                        )
                    } else if (simGuardEngine.resumeStrikes == 1) {
                        GuardStateBus.appendLog(
                            "[SIM] Resume Strike 1/2: Signal improved to ${sim.rssiDbm} dBm (>= ${settings.resumeThresholdDbm} dBm).",
                            LogLevel.INFO
                        )
                    }

                    when (decision) {
                        is GuardDecision.TriggerPause -> {
                            GuardStateBus.appendLog(
                                "[SIM] Strike 2/2 at ${decision.rssiDbm} dBm! Triggering Auto-Pause...",
                                LogLevel.WARN
                            )
                            executeManualPortalAction(PortalAction.PAUSE, isAuto = true)
                        }
                        is GuardDecision.TriggerResume -> {
                            GuardStateBus.appendLog(
                                "[SIM] Strike 2/2 at ${decision.rssiDbm} dBm! Triggering Auto-Resume...",
                                LogLevel.SUCCESS
                            )
                            executeManualPortalAction(PortalAction.RESUME, isAuto = true)
                        }
                        GuardDecision.NoOp -> Unit
                    }
                }

                delay(2000L)
            }
        }
    }

    fun stopGuardService() {
        if (localSimGuardJob?.isActive == true) {
            localSimGuardJob?.cancel()
            simGuardEngine.reset()
            GuardStateBus.updateTelemetry {
                it.copy(
                    isGuardRunning = false,
                    pauseStrikes = 0,
                    resumeStrikes = 0
                )
            }
            GuardStateBus.appendLog("[SIM] In-app RSSI Guard stopped.", LogLevel.INFO)
        } else {
            WifiGuardService.stop(appContext)
        }
    }

    fun testPause() {
        if (GuardStateBus.telemetry.value.isGuardRunning && localSimGuardJob?.isActive != true) {
            GuardStateBus.appendLog("Manual Test Pause requested...", LogLevel.INFO)
            val intent = Intent(appContext, WifiGuardService::class.java).apply {
                action = WifiGuardService.ACTION_MANUAL_PAUSE
            }
            appContext.startService(intent)
        } else {
            executeManualPortalAction(PortalAction.PAUSE)
        }
    }

    fun testResume() {
        if (GuardStateBus.telemetry.value.isGuardRunning && localSimGuardJob?.isActive != true) {
            GuardStateBus.appendLog("Manual Test Resume requested...", LogLevel.INFO)
            val intent = Intent(appContext, WifiGuardService::class.java).apply {
                action = WifiGuardService.ACTION_MANUAL_RESUME
            }
            appContext.startService(intent)
        } else {
            executeManualPortalAction(PortalAction.RESUME)
        }
    }

    private fun executeManualPortalAction(action: PortalAction, isAuto: Boolean = false) {
        val settings = uiState.value.settings
        GuardStateBus.appendLog(
            "${if (isAuto) "Auto-" else ""}${action.label} -> ${settings.portalBaseUrl}...",
            LogLevel.INFO
        )
        viewModelScope.launch {
            val result = portalController.executeAction(
                portalBaseUrl = settings.portalBaseUrl,
                action = action,
                useWebViewFallback = settings.useWebViewFallback,
                isAuto = isAuto
            )
            when (result) {
                is PortalExecutionResult.Success -> {
                    GuardStateBus.appendLog(result.detail, LogLevel.SUCCESS)
                    applyLocalTransition(action, settings, isAuto)
                }

                is PortalExecutionResult.DispatchedToWebView -> {
                    GuardStateBus.appendLog(
                        "Method B (WebView JS #${result.requestId}) dispatched for ${action.label}.",
                        LogLevel.INFO
                    )
                    applyLocalTransition(action, settings, isAuto)
                }

                is PortalExecutionResult.Failure -> {
                    GuardStateBus.appendLog(
                        "${action.label} failed: ${result.reason}",
                        if (result.backoffSeconds > 0) LogLevel.WARN else LogLevel.ERROR
                    )
                }
            }
        }
    }

    private fun applyLocalTransition(action: PortalAction, settings: GuardSettings, isAuto: Boolean) {
        when (action) {
            PortalAction.PAUSE -> {
                feedbackHelper.vibrateDouble()
                val limitSeconds = settings.pauseExpiryMinutes * 60
                viewModelScope.launch {
                    prefsRepo.recordPauseStarted(isAuto = isAuto, maxLimitSeconds = limitSeconds)
                }
                GuardStateBus.updateTelemetry {
                    it.copy(
                        sessionStatus = SessionStatus.PAUSED,
                        pauseCountdownSeconds = limitSeconds,
                        totalPauseLimitSeconds = limitSeconds,
                        expiryWarningSent = false
                    )
                }
                startLocalCountdown(settings.pauseExpiryMinutes)
            }

            PortalAction.RESUME -> {
                feedbackHelper.vibrateSingle()
                val limitSeconds = settings.pauseExpiryMinutes * 60
                viewModelScope.launch {
                    prefsRepo.recordPauseEnded(maxLimitSeconds = limitSeconds)
                }
                localCountdownJob?.cancel()
                GuardStateBus.updateTelemetry {
                    it.copy(
                        sessionStatus = SessionStatus.ACTIVE,
                        pauseCountdownSeconds = 0,
                        expiryWarningSent = false
                    )
                }
            }
        }
    }

    private fun startLocalCountdown(limitMinutes: Int) {
        localCountdownJob?.cancel()
        localCountdownJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val current = GuardStateBus.telemetry.value
                val serviceRunning = current.isGuardRunning && localSimGuardJob?.isActive != true
                if (serviceRunning || current.sessionStatus != SessionStatus.PAUSED) {
                    break
                }
                val nextSeconds = (current.pauseCountdownSeconds - 1).coerceAtLeast(0)
                val shouldAlert = !current.expiryWarningSent && nextSeconds in 1..300
                if (shouldAlert) {
                    val minsLeft = ((nextSeconds + 59) / 60).coerceAtLeast(1)
                    feedbackHelper.sendExpiryWarningNotification(minsLeft)
                    GuardStateBus.appendLog(
                        "ALERT: Paused LPB session expires in $minsLeft minute(s)!",
                        LogLevel.WARN
                    )
                }
                GuardStateBus.updateTelemetry {
                    it.copy(
                        pauseCountdownSeconds = nextSeconds,
                        expiryWarningSent = it.expiryWarningSent || shouldAlert
                    )
                }
                if (nextSeconds == 0) {
                    prefsRepo.recordPauseEnded(maxLimitSeconds = limitMinutes * 60)
                    GuardStateBus.appendLog(
                        "LPB Pause Expiration limit (${limitMinutes}m) reached.",
                        LogLevel.ERROR
                    )
                    break
                }
            }
        }
    }

    fun adjustPauseThreshold(deltaDbm: Int) {
        viewModelScope.launch {
            val next = (uiState.value.settings.pauseThresholdDbm + deltaDbm).coerceIn(-90, -50)
            prefsRepo.setPauseThresholdDbm(next)
            GuardStateBus.appendLog("Auto-Pause trigger updated to $next dBm.", LogLevel.INFO)
        }
    }

    fun setPauseThreshold(targetDbm: Int) {
        viewModelScope.launch {
            val clamped = targetDbm.coerceIn(-90, -50)
            prefsRepo.setPauseThresholdDbm(clamped)
            val label = when {
                clamped >= -70 -> "Close"
                clamped >= -78 -> "Balanced"
                else -> "Far"
            }
            GuardStateBus.appendLog("Pause distance preset set to $label ($clamped dBm).", LogLevel.INFO)
        }
    }

    fun adjustResumeThreshold(deltaDbm: Int) {
        viewModelScope.launch {
            val next = (uiState.value.settings.resumeThresholdDbm + deltaDbm).coerceIn(-80, -40)
            prefsRepo.setResumeThresholdDbm(next)
            GuardStateBus.appendLog("Auto-Resume threshold set to $next dBm.", LogLevel.INFO)
        }
    }

    fun toggleAutoResume(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAutoResumeEnabled(enabled)
            GuardStateBus.appendLog(
                "Auto-Resume ${if (enabled) "enabled" else "disabled"}.",
                LogLevel.INFO
            )
        }
    }

    fun toggleWebViewFallback(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setUseWebViewFallback(enabled)
            GuardStateBus.appendLog(
                "Method B (Hidden WebView JS fallback) ${if (enabled) "enabled" else "disabled"}.",
                LogLevel.INFO
            )
        }
    }

    fun toggleSimpleMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setSimpleModeEnabled(enabled)
            GuardStateBus.appendLog(
                if (enabled) "Simple Mode enabled." else "Full Mode enabled.",
                LogLevel.INFO
            )
        }
    }

    fun updatePortalUrl(url: String) {
        viewModelScope.launch {
            prefsRepo.setPortalBaseUrl(url)
            GuardStateBus.resetPortalRetry()
            val normalized = GuardPreferencesRepository.normalizePortalUrl(url)
            GuardStateBus.appendLog("Portal URL updated to $normalized", LogLevel.INFO)
        }
    }

    fun updateExpiryMinutes(minutes: Int) {
        viewModelScope.launch {
            prefsRepo.setPauseExpiryMinutes(minutes)
            GuardStateBus.appendLog("Pause expiry timer limit set to $minutes min.", LogLevel.INFO)
        }
    }

    fun clearLogs() {
        GuardStateBus.clearLogs()
    }

    fun resetMonthlySavings() {
        viewModelScope.launch {
            prefsRepo.clearMonthlySavings()
            GuardStateBus.appendLog("Monthly time-saved stats reset on this device.", LogLevel.INFO)
        }
    }
}
