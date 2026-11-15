package com.example.network

import kotlin.math.min

data class PortalRetryState(
    val consecutiveFailures: Int = 0,
    val nextRetryAtMs: Long = 0L,
    val lastBackoffMs: Long = 0L,
    val lastFailureReason: String = ""
) {
    fun remainingCooldownSeconds(nowMs: Long): Int {
        if (nextRetryAtMs <= 0L || nowMs >= nextRetryAtMs) return 0
        return ((nextRetryAtMs - nowMs + 999L) / 1000L).toInt().coerceAtLeast(1)
    }

    val isBackingOff: Boolean
        get() = consecutiveFailures > 0 && nextRetryAtMs > 0L
}

/**
 * Battery-aware exponential backoff policy for LPB Piso WiFi portal (10.0.0.1) communication.
 *
 * Schedule after consecutive unreachable cycles:
 *  - Failure 1 -> 4s cooldown
 *  - Failure 2 -> 8s cooldown
 *  - Failure 3 -> 16s cooldown
 *  - Failure 4 -> 32s cooldown
 *  - Failure 5 -> 64s cooldown
 *  - Failure 6+ -> capped at 120s (2 minutes)
 *
 * Resets immediately on any successful portal command, network/SSID change, or user-triggered retry.
 */
class PortalRetryPolicy(
    private val baseDelayMs: Long = DEFAULT_BASE_BACKOFF_MS,
    private val maxDelayMs: Long = DEFAULT_MAX_BACKOFF_MS,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    companion object {
        const val DEFAULT_BASE_BACKOFF_MS = 4_000L
        const val DEFAULT_MAX_BACKOFF_MS = 120_000L
        const val IN_CALL_RETRY_BASE_MS = 1_000L
        const val IN_CALL_RETRY_MAX_MS = 4_000L

        fun computeBackoffMs(
            failureCount: Int,
            baseDelayMs: Long = DEFAULT_BASE_BACKOFF_MS,
            maxDelayMs: Long = DEFAULT_MAX_BACKOFF_MS
        ): Long {
            if (failureCount <= 0) return 0L
            val shift = (failureCount - 1).coerceAtMost(20)
            val multiplier = 1L shl shift
            val raw = if (baseDelayMs > Long.MAX_VALUE / multiplier) {
                maxDelayMs
            } else {
                baseDelayMs * multiplier
            }
            return min(maxDelayMs, raw)
        }
    }

    private val lock = Any()
    private var state = PortalRetryState()

    fun currentState(): PortalRetryState = synchronized(lock) { state }

    fun shouldAttemptAutoRetry(nowMs: Long = clock()): Boolean = synchronized(lock) {
        state.nextRetryAtMs <= 0L || nowMs >= state.nextRetryAtMs
    }

    fun remainingCooldownSeconds(nowMs: Long = clock()): Int = synchronized(lock) {
        state.remainingCooldownSeconds(nowMs)
    }

    fun recordSuccess(): PortalRetryState = synchronized(lock) {
        state = PortalRetryState()
        state
    }

    fun recordFailure(
        reason: String,
        nowMs: Long = clock()
    ): PortalRetryState = synchronized(lock) {
        val nextCount = state.consecutiveFailures + 1
        val delayMs = computeBackoffMs(
            failureCount = nextCount,
            baseDelayMs = baseDelayMs,
            maxDelayMs = maxDelayMs
        )
        state = PortalRetryState(
            consecutiveFailures = nextCount,
            nextRetryAtMs = nowMs + delayMs,
            lastBackoffMs = delayMs,
            lastFailureReason = reason
        )
        state
    }

    fun reset(): PortalRetryState = synchronized(lock) {
        state = PortalRetryState()
        state
    }
}
