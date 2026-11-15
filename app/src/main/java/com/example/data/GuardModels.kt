package com.example.data

enum class SessionStatus(val label: String) {
    ACTIVE("ACTIVE"),
    PAUSED("PAUSED"),
    DISCONNECTED("OFFLINE")
}

enum class LogLevel {
    INFO,
    SUCCESS,
    WARN,
    ERROR
}

enum class PortalAction(val label: String) {
    PAUSE("Pause"),
    RESUME("Resume")
}

data class ActivityLogEntry(
    val id: Long,
    val timestamp: String,
    val message: String,
    val level: LogLevel
)

data class TelemetrySnapshot(
    val ssid: String = "Not Connected",
    val bssid: String = "--:--:--:--:--:--",
    val rssiDbm: Int? = null,
    val isWifiConnected: Boolean = false,
    val isWifiBound: Boolean = false,
    val isGuardRunning: Boolean = false,
    val sessionStatus: SessionStatus = SessionStatus.DISCONNECTED,
    val pauseStrikes: Int = 0,
    val resumeStrikes: Int = 0,
    val pauseCountdownSeconds: Int = 0,
    val totalPauseLimitSeconds: Int = 3600,
    val expiryWarningSent: Boolean = false,
    val isExecutingPortalCommand: Boolean = false,
    val lastMethodUsed: String = "Idle",
    val isSimulated: Boolean = false,
    val rssiHistory: List<Int> = emptyList(),
    val rssiSamples: List<RssiSample> = emptyList(),
    val portalRetryAttempt: Int = 0,
    val portalNextRetrySeconds: Int = 0,
    val portalBackoffSeconds: Int = 0
)

data class RssiSample(
    val timestampMs: Long,
    val rssiDbm: Int
)

data class SimulationConfig(
    val enabled: Boolean = false,
    val rssiDbm: Int = -58,
    val simulateUnreachablePortal: Boolean = false
)

data class GuardSettings(
    val pauseThresholdDbm: Int = -75,
    val resumeThresholdDbm: Int = -65,
    val autoResumeEnabled: Boolean = true,
    val portalBaseUrl: String = "http://10.0.0.1",
    val pauseExpiryMinutes: Int = 60,
    val useWebViewFallback: Boolean = true,
    val simpleModeEnabled: Boolean = false
)

data class MonthlySavingsRecord(
    val monthKey: String,
    val totalSavedSeconds: Long = 0L,
    val autoPauseCount: Int = 0,
    val totalPauseCount: Int = 0,
    val longestPauseSeconds: Long = 0L
)

data class SavingsSummaryState(
    val monthsByKey: Map<String, MonthlySavingsRecord> = emptyMap(),
    val activePauseStartedAtMs: Long = 0L,
    val activePauseMonthKey: String = "",
    val activePauseWasAuto: Boolean = false
)

data class GuardUiState(
    val telemetry: TelemetrySnapshot = TelemetrySnapshot(),
    val settings: GuardSettings = GuardSettings(),
    val savings: SavingsSummaryState = SavingsSummaryState(),
    val simulation: SimulationConfig = SimulationConfig(),
    val logs: List<ActivityLogEntry> = emptyList(),
    val hasLocationPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val isBatteryOptimizationIgnored: Boolean = false
)

data class WebViewFallbackRequest(
    val requestId: Long,
    val portalBaseUrl: String,
    val action: PortalAction
)
