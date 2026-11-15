package com.example.data

import com.example.network.PortalRetryPolicy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Process-wide reactive telemetry and activity log bus shared between
 * WifiGuardService, LpbPortalController, HiddenPortalWebView, and GuardViewModel.
 */
object GuardStateBus {
    private const val MAX_LOGS = 150
    private val logIdGen = AtomicLong(1L)
    private val webRequestIdGen = AtomicLong(1L)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    val portalRetryPolicy = PortalRetryPolicy()

    private val _telemetry = MutableStateFlow(TelemetrySnapshot())
    val telemetry: StateFlow<TelemetrySnapshot> = _telemetry.asStateFlow()

    private val _logs = MutableStateFlow<List<ActivityLogEntry>>(emptyList())
    val logs: StateFlow<List<ActivityLogEntry>> = _logs.asStateFlow()

    private val _webViewCommands = MutableSharedFlow<WebViewFallbackRequest>(
        replay = 0,
        extraBufferCapacity = 8
    )
    val webViewCommands: SharedFlow<WebViewFallbackRequest> = _webViewCommands.asSharedFlow()

    private val _simulationConfig = MutableStateFlow(SimulationConfig())
    val simulationConfig: StateFlow<SimulationConfig> = _simulationConfig.asStateFlow()

    fun setSimulationEnabled(enabled: Boolean) {
        _simulationConfig.update { it.copy(enabled = enabled) }
    }

    fun setSimulatedRssiDbm(rssiDbm: Int) {
        _simulationConfig.update { it.copy(rssiDbm = rssiDbm.coerceIn(-95, -40)) }
    }

    fun setSimulateUnreachablePortal(unreachable: Boolean) {
        _simulationConfig.update { it.copy(simulateUnreachablePortal = unreachable) }
        if (!unreachable) {
            resetPortalRetry()
        }
    }

    fun syncRetryTelemetry(nowMs: Long = System.currentTimeMillis()) {
        val state = portalRetryPolicy.currentState()
        val remaining = state.remainingCooldownSeconds(nowMs)
        val backoffSec = (state.lastBackoffMs / 1000L).toInt()
        _telemetry.update {
            it.copy(
                portalRetryAttempt = state.consecutiveFailures,
                portalNextRetrySeconds = remaining,
                portalBackoffSeconds = backoffSec
            )
        }
    }

    fun resetPortalRetry() {
        portalRetryPolicy.reset()
        syncRetryTelemetry()
    }

    fun updateTelemetry(transform: (TelemetrySnapshot) -> TelemetrySnapshot) {
        _telemetry.update(transform)
    }

    /**
     * Appends a local-only RSSI sample and retains only readings from the last 60 seconds.
     */
    fun trimAndAppendSample(
        existing: List<RssiSample>,
        rssiDbm: Int?,
        isConnected: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): List<RssiSample> {
        val cutoff = nowMs - 60_000L
        val recent = existing.filter { it.timestampMs >= cutoff }
        return if (isConnected && rssiDbm != null) {
            (recent + RssiSample(timestampMs = nowMs, rssiDbm = rssiDbm)).takeLast(60)
        } else {
            recent
        }
    }

    fun appendLog(message: String, level: LogLevel = LogLevel.INFO) {
        val timestamp = synchronized(timeFormat) { timeFormat.format(Date()) }
        val entry = ActivityLogEntry(
            id = logIdGen.getAndIncrement(),
            timestamp = timestamp,
            message = message,
            level = level
        )
        _logs.update { current ->
            (listOf(entry) + current).take(MAX_LOGS)
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun dispatchWebViewFallback(portalBaseUrl: String, action: PortalAction): Long {
        val id = webRequestIdGen.getAndIncrement()
        _webViewCommands.tryEmit(
            WebViewFallbackRequest(
                requestId = id,
                portalBaseUrl = portalBaseUrl,
                action = action
            )
        )
        return id
    }
}
