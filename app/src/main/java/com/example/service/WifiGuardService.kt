package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.GuardPreferencesRepository
import com.example.data.GuardSettings
import com.example.data.GuardStateBus
import com.example.data.LogLevel
import com.example.data.PortalAction
import com.example.data.SessionStatus
import com.example.domain.GuardDecision
import com.example.domain.RssiGuardEngine
import com.example.network.LpbPortalController
import com.example.network.PortalExecutionResult
import com.example.network.WifiNetworkBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WifiGuardService : Service() {

    companion object {
        const val ACTION_START = "com.example.action.START_GUARD"
        const val ACTION_STOP = "com.example.action.STOP_GUARD"
        const val ACTION_MANUAL_PAUSE = "com.example.action.MANUAL_PAUSE"
        const val ACTION_MANUAL_RESUME = "com.example.action.MANUAL_RESUME"

        const val EXTRA_FROM_TILE = "com.example.extra.FROM_TILE"

        fun start(context: Context, fromTile: Boolean = false) {
            val intent = Intent(context, WifiGuardService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_FROM_TILE, fromTile)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WifiGuardService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var prefsRepo: GuardPreferencesRepository
    private lateinit var telemetryReader: WifiTelemetryReader
    private lateinit var portalController: LpbPortalController
    private lateinit var wifiBinder: WifiNetworkBinder
    private lateinit var feedbackHelper: DeviceFeedbackHelper
    private lateinit var notificationManager: NotificationManager

    private val guardEngine = RssiGuardEngine()
    private var currentSettings = GuardSettings()
    private var wakeLock: PowerManager.WakeLock? = null
    private var pollingJob: Job? = null
    private var countdownJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        prefsRepo = GuardPreferencesRepository(applicationContext)
        telemetryReader = WifiTelemetryReader(applicationContext)
        portalController = LpbPortalController(applicationContext)
        wifiBinder = WifiNetworkBinder(applicationContext)
        feedbackHelper = DeviceFeedbackHelper(applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        feedbackHelper.ensureNotificationChannels()

        serviceScope.launch {
            prefsRepo.settingsFlow.collect { settings ->
                currentSettings = settings
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopGuardCleanly()
                return START_NOT_STICKY
            }

            ACTION_MANUAL_PAUSE -> {
                serviceScope.launch { triggerPortalAction(PortalAction.PAUSE, isAuto = false) }
            }

            ACTION_MANUAL_RESUME -> {
                serviceScope.launch { triggerPortalAction(PortalAction.RESUME, isAuto = false) }
            }

            ACTION_START, null -> {
                val fromTile = intent?.getBooleanExtra(EXTRA_FROM_TILE, false) == true
                startGuardForeground(fromTile = fromTile)
            }
        }
        return START_STICKY
    }

    private fun startGuardForeground(fromTile: Boolean = false) {
        if (!telemetryReader.hasLocationPermission()) {
            GuardStateBus.appendLog(
                "Cannot start Foreground RSSI Guard without Location permission.",
                LogLevel.ERROR
            )
            GuardStateBus.updateTelemetry { it.copy(isGuardRunning = false) }
            EltharionTileService.requestTileUpdate(applicationContext)
            stopSelf()
            return
        }

        val initialNotification = buildGuardNotification(
            ssid = GuardStateBus.telemetry.value.ssid,
            rssiDbm = GuardStateBus.telemetry.value.rssiDbm,
            status = GuardStateBus.telemetry.value.sessionStatus,
            countdownSeconds = GuardStateBus.telemetry.value.pauseCountdownSeconds
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    DeviceFeedbackHelper.GUARD_NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(DeviceFeedbackHelper.GUARD_NOTIFICATION_ID, initialNotification)
            }
        } catch (se: SecurityException) {
            GuardStateBus.appendLog(
                "Foreground location service blocked by OS: ${se.message}",
                LogLevel.ERROR
            )
            GuardStateBus.updateTelemetry { it.copy(isGuardRunning = false) }
            EltharionTileService.requestTileUpdate(applicationContext)
            if (fromTile) {
                feedbackHelper.sendTapToStartGuardNotification()
            }
            stopSelf()
            return
        }

        acquireWakeLock()

        if (pollingJob?.isActive == true) {
            EltharionTileService.requestTileUpdate(applicationContext)
            return
        }

        serviceScope.launch {
            currentSettings = prefsRepo.settingsFlow.first()
        }

        GuardStateBus.updateTelemetry {
            val initialSession = if (it.sessionStatus == SessionStatus.DISCONNECTED) {
                SessionStatus.ACTIVE
            } else {
                it.sessionStatus
            }
            it.copy(
                isGuardRunning = true,
                sessionStatus = initialSession
            )
        }
        EltharionTileService.requestTileUpdate(applicationContext)
        GuardStateBus.appendLog(
            "RSSI Guard started (2s polling, threshold ${currentSettings.pauseThresholdDbm} dBm, 2-strike debounce).",
            LogLevel.SUCCESS
        )

        if (!feedbackHelper.isIgnoringBatteryOptimizations()) {
            val sent = feedbackHelper.sendBatteryOptimizationGuideNotification()
            if (sent) {
                GuardStateBus.appendLog(
                    "Sent notification: Disable battery optimization for Eltharion (Unrestricted) for reliable background service.",
                    LogLevel.INFO
                )
            }
        }

        startPollingLoop()
    }

    private fun startPollingLoop() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            while (isActive) {
                val sim = GuardStateBus.simulationConfig.value
                val reading = if (sim.enabled) {
                    WifiReading(
                        isConnected = true,
                        ssid = "LPB_PisoWiFi_SIM",
                        bssid = "02:10:00:01:5A:FE",
                        rssiDbm = sim.rssiDbm
                    )
                } else {
                    telemetryReader.readCurrentWifi()
                }
                val isBound = if (sim.enabled) true else if (reading.isConnected) wifiBinder.bindProcessToWifi() != null else false

                val prevTelemetry = GuardStateBus.telemetry.value
                val previousStatus = prevTelemetry.sessionStatus
                val effectiveStatus = when {
                    !reading.isConnected -> SessionStatus.DISCONNECTED
                    previousStatus == SessionStatus.DISCONNECTED -> SessionStatus.ACTIVE
                    else -> previousStatus
                }

                // Reset retry backoff immediately if WiFi freshly connected or switched SSID
                if (reading.isConnected && (!prevTelemetry.isWifiConnected || prevTelemetry.ssid != reading.ssid)) {
                    GuardStateBus.resetPortalRetry()
                }

                val canAutoContactPortal = GuardStateBus.portalRetryPolicy.shouldAttemptAutoRetry()
                val decision = if (canAutoContactPortal) {
                    guardEngine.evaluate(
                        rssiDbm = reading.rssiDbm,
                        isWifiConnected = reading.isConnected,
                        currentStatus = effectiveStatus,
                        pauseThresholdDbm = currentSettings.pauseThresholdDbm,
                        resumeThresholdDbm = currentSettings.resumeThresholdDbm,
                        autoResumeEnabled = currentSettings.autoResumeEnabled
                    )
                } else {
                    guardEngine.reset()
                    GuardDecision.NoOp
                }

                GuardStateBus.updateTelemetry {
                    val nextSamples = GuardStateBus.trimAndAppendSample(
                        existing = it.rssiSamples,
                        rssiDbm = reading.rssiDbm,
                        isConnected = reading.isConnected
                    )
                    it.copy(
                        ssid = reading.ssid,
                        bssid = reading.bssid,
                        rssiDbm = reading.rssiDbm,
                        isWifiConnected = reading.isConnected,
                        isWifiBound = isBound,
                        sessionStatus = effectiveStatus,
                        pauseStrikes = guardEngine.pauseStrikes,
                        resumeStrikes = guardEngine.resumeStrikes,
                        isSimulated = sim.enabled,
                        rssiSamples = nextSamples,
                        rssiHistory = nextSamples.map { sample -> sample.rssiDbm }
                    )
                }
                GuardStateBus.syncRetryTelemetry()
                if (effectiveStatus != previousStatus || reading.isConnected != prevTelemetry.isWifiConnected) {
                    EltharionTileService.requestTileUpdate(applicationContext)
                }

                if (canAutoContactPortal) {
                    if (guardEngine.pauseStrikes == 1 && reading.rssiDbm != null) {
                        GuardStateBus.appendLog(
                            "Strike 1/2: Signal dropped to ${reading.rssiDbm} dBm (<= ${currentSettings.pauseThresholdDbm} dBm).",
                            LogLevel.WARN
                        )
                    } else if (guardEngine.resumeStrikes == 1 && reading.rssiDbm != null) {
                        GuardStateBus.appendLog(
                            "Resume Strike 1/2: Signal improved to ${reading.rssiDbm} dBm (>= ${currentSettings.resumeThresholdDbm} dBm).",
                            LogLevel.INFO
                        )
                    }

                    when (decision) {
                        is GuardDecision.TriggerPause -> {
                            GuardStateBus.appendLog(
                                "Strike 2/2 at ${decision.rssiDbm} dBm! Triggering Auto-Pause on ${currentSettings.portalBaseUrl}...",
                                LogLevel.WARN
                            )
                            triggerPortalAction(PortalAction.PAUSE, isAuto = true)
                        }

                        is GuardDecision.TriggerResume -> {
                            GuardStateBus.appendLog(
                                "Strike 2/2 at ${decision.rssiDbm} dBm! Triggering Auto-Resume on ${currentSettings.portalBaseUrl}...",
                                LogLevel.SUCCESS
                            )
                            triggerPortalAction(PortalAction.RESUME, isAuto = true)
                        }

                        GuardDecision.NoOp -> Unit
                    }
                }

                val snap = GuardStateBus.telemetry.value
                notificationManager.notify(
                    DeviceFeedbackHelper.GUARD_NOTIFICATION_ID,
                    buildGuardNotification(
                        ssid = snap.ssid,
                        rssiDbm = snap.rssiDbm,
                        status = snap.sessionStatus,
                        countdownSeconds = snap.pauseCountdownSeconds
                    )
                )

                delay(2000L)
            }
        }
    }

    private suspend fun triggerPortalAction(action: PortalAction, isAuto: Boolean) {
        val result = portalController.executeAction(
            portalBaseUrl = currentSettings.portalBaseUrl,
            action = action,
            useWebViewFallback = currentSettings.useWebViewFallback,
            isAuto = isAuto
        )

        when (result) {
            is PortalExecutionResult.Success -> {
                GuardStateBus.appendLog(result.detail, LogLevel.SUCCESS)
                applySessionTransition(action, isAuto)
            }

            is PortalExecutionResult.DispatchedToWebView -> {
                // Method B dispatched; apply optimistic transition + haptic if auto-triggered
                GuardStateBus.appendLog(
                    "Method B (WebView JS #${result.requestId}) dispatched for ${action.label}.",
                    LogLevel.INFO
                )
                applySessionTransition(action, isAuto)
            }

            is PortalExecutionResult.Failure -> {
                GuardStateBus.appendLog(
                    "${action.label} failed: ${result.reason}",
                    if (result.backoffSeconds > 0) LogLevel.WARN else LogLevel.ERROR
                )
            }
        }
    }

    private fun applySessionTransition(action: PortalAction, isAuto: Boolean) {
        when (action) {
            PortalAction.PAUSE -> {
                if (isAuto) {
                    feedbackHelper.vibrateDouble()
                }
                val limitSeconds = currentSettings.pauseExpiryMinutes * 60
                serviceScope.launch {
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
                startPauseCountdownTimer()
            }

            PortalAction.RESUME -> {
                if (isAuto) {
                    feedbackHelper.vibrateSingle()
                }
                val limitSeconds = currentSettings.pauseExpiryMinutes * 60
                serviceScope.launch {
                    prefsRepo.recordPauseEnded(maxLimitSeconds = limitSeconds)
                }
                countdownJob?.cancel()
                GuardStateBus.updateTelemetry {
                    it.copy(
                        sessionStatus = SessionStatus.ACTIVE,
                        pauseCountdownSeconds = 0,
                        expiryWarningSent = false
                    )
                }
            }
        }
        EltharionTileService.requestTileUpdate(applicationContext)
    }

    private fun startPauseCountdownTimer() {
        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                val current = GuardStateBus.telemetry.value
                if (current.sessionStatus != SessionStatus.PAUSED) {
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
                    prefsRepo.recordPauseEnded(maxLimitSeconds = currentSettings.pauseExpiryMinutes * 60)
                    GuardStateBus.appendLog(
                        "LPB Pause Expiration limit (${currentSettings.pauseExpiryMinutes}m) reached.",
                        LogLevel.ERROR
                    )
                    break
                }
            }
        }
    }

    private fun buildGuardNotification(
        ssid: String,
        rssiDbm: Int?,
        status: SessionStatus,
        countdownSeconds: Int
    ): Notification {
        val rssiText = rssiDbm?.let { "$it dBm" } ?: "-- dBm"
        val statusText = if (status == SessionStatus.PAUSED && countdownSeconds > 0) {
            val mins = countdownSeconds / 60
            val secs = countdownSeconds % 60
            "PAUSED • %02d:%02d left".format(mins, secs)
        } else {
            status.label
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, WifiGuardService::class.java).apply { action = ACTION_MANUAL_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, WifiGuardService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, DeviceFeedbackHelper.CHANNEL_GUARD_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("$ssid | $rssiText | $statusText")
            .setContentText("Auto-Pause <= ${currentSettings.pauseThresholdDbm} dBm • ${currentSettings.portalBaseUrl}")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Pause Now", pauseIntent)
            .addAction(0, "Stop Guard", stopIntent)
            .build()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LpbGuard::RssiWakeLock").apply {
            setReferenceCounted(false)
            acquire(6 * 60 * 60 * 1000L) // 6h safety cap
        }
    }

    private fun stopGuardCleanly() {
        pollingJob?.cancel()
        countdownJob?.cancel()
        guardEngine.reset()
        wifiBinder.clearProcessBinding()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
        GuardStateBus.updateTelemetry {
            it.copy(
                isGuardRunning = false,
                isWifiBound = false,
                pauseStrikes = 0,
                resumeStrikes = 0
            )
        }
        GuardStateBus.resetPortalRetry()
        EltharionTileService.requestTileUpdate(applicationContext)
        GuardStateBus.appendLog("RSSI Guard stopped.", LogLevel.INFO)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        countdownJob?.cancel()
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
        GuardStateBus.updateTelemetry { it.copy(isGuardRunning = false) }
        EltharionTileService.requestTileUpdate(applicationContext)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
