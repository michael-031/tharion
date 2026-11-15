package com.example.domain

import com.example.data.SessionStatus

sealed interface GuardDecision {
    data object NoOp : GuardDecision
    data class TriggerPause(val rssiDbm: Int) : GuardDecision
    data class TriggerResume(val rssiDbm: Int) : GuardDecision
}

/**
 * Deterministic 2-strike RSSI debounce engine.
 * Prevents false Auto-Pause / Auto-Resume triggers caused by single-sample RF jitter.
 */
class RssiGuardEngine {
    var pauseStrikes: Int = 0
        private set

    var resumeStrikes: Int = 0
        private set

    fun reset() {
        pauseStrikes = 0
        resumeStrikes = 0
    }

    fun evaluate(
        rssiDbm: Int?,
        isWifiConnected: Boolean,
        currentStatus: SessionStatus,
        pauseThresholdDbm: Int,
        resumeThresholdDbm: Int,
        autoResumeEnabled: Boolean
    ): GuardDecision {
        if (!isWifiConnected || rssiDbm == null) {
            reset()
            return GuardDecision.NoOp
        }

        return when (currentStatus) {
            SessionStatus.ACTIVE -> {
                resumeStrikes = 0
                if (rssiDbm <= pauseThresholdDbm) {
                    pauseStrikes += 1
                    if (pauseStrikes >= 2) {
                        pauseStrikes = 0
                        GuardDecision.TriggerPause(rssiDbm)
                    } else {
                        GuardDecision.NoOp
                    }
                } else {
                    pauseStrikes = 0
                    GuardDecision.NoOp
                }
            }

            SessionStatus.PAUSED -> {
                pauseStrikes = 0
                if (autoResumeEnabled && rssiDbm >= resumeThresholdDbm) {
                    resumeStrikes += 1
                    if (resumeStrikes >= 2) {
                        resumeStrikes = 0
                        GuardDecision.TriggerResume(rssiDbm)
                    } else {
                        GuardDecision.NoOp
                    }
                } else {
                    resumeStrikes = 0
                    GuardDecision.NoOp
                }
            }

            SessionStatus.DISCONNECTED -> {
                reset()
                GuardDecision.NoOp
            }
        }
    }
}
