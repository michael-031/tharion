package com.example

import com.example.data.PortalAction
import com.example.data.SessionStatus
import com.example.domain.GuardDecision
import com.example.domain.RssiGuardEngine
import com.example.network.LpbHtmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LpbGuardUnitTests {

    @Test
    fun `two-strike debounce triggers Auto-Pause only after 2 consecutive weak samples`() {
        val engine = RssiGuardEngine()

        // Single drop below -75 dBm -> Strike 1 only, no trigger
        val first = engine.evaluate(
            rssiDbm = -76,
            isWifiConnected = true,
            currentStatus = SessionStatus.ACTIVE,
            pauseThresholdDbm = -75,
            resumeThresholdDbm = -65,
            autoResumeEnabled = true
        )
        assertEquals(GuardDecision.NoOp, first)
        assertEquals(1, engine.pauseStrikes)

        // Second consecutive drop -> triggers Pause
        val second = engine.evaluate(
            rssiDbm = -78,
            isWifiConnected = true,
            currentStatus = SessionStatus.ACTIVE,
            pauseThresholdDbm = -75,
            resumeThresholdDbm = -65,
            autoResumeEnabled = true
        )
        assertEquals(GuardDecision.TriggerPause(-78), second)
        assertEquals(0, engine.pauseStrikes)
    }

    @Test
    fun `single-sample jitter resets pause strikes when signal recovers`() {
        val engine = RssiGuardEngine()

        engine.evaluate(-77, true, SessionStatus.ACTIVE, -75, -65, true)
        assertEquals(1, engine.pauseStrikes)

        val recovery = engine.evaluate(-70, true, SessionStatus.ACTIVE, -75, -65, true)
        assertEquals(GuardDecision.NoOp, recovery)
        assertEquals(0, engine.pauseStrikes)
    }

    @Test
    fun `two-strike debounce triggers Auto-Resume when enabled and signal is strong`() {
        val engine = RssiGuardEngine()

        val first = engine.evaluate(-64, true, SessionStatus.PAUSED, -75, -65, true)
        assertEquals(GuardDecision.NoOp, first)
        assertEquals(1, engine.resumeStrikes)

        val second = engine.evaluate(-62, true, SessionStatus.PAUSED, -75, -65, true)
        assertEquals(GuardDecision.TriggerResume(-62), second)
        assertEquals(0, engine.resumeStrikes)
    }

    @Test
    fun `auto-resume never triggers when disabled`() {
        val engine = RssiGuardEngine()

        assertEquals(GuardDecision.NoOp, engine.evaluate(-55, true, SessionStatus.PAUSED, -75, -65, false))
        assertEquals(GuardDecision.NoOp, engine.evaluate(-55, true, SessionStatus.PAUSED, -75, -65, false))
        assertEquals(0, engine.resumeStrikes)
    }

    @Test
    fun `html parser extracts dynamic LPB pause and resume links with fallbacks`() {
        val html = """
            <html><body>
              <div class="controls">
                <a href="/index.php?token=abc123&amp;pause=1" class="btn btn-warning">Pause Time</a>
                <button onclick="location.href='http://10.0.0.1/resume?id=99'">Resume Time</button>
              </div>
            </body></html>
        """.trimIndent()

        val pauseUrls = LpbHtmlParser.extractActionUrls(html, "http://10.0.0.1", PortalAction.PAUSE)
        assertEquals("http://10.0.0.1/index.php?token=abc123&pause=1", pauseUrls.first())
        assertTrue(pauseUrls.contains("http://10.0.0.1/index.php?pause=1"))

        val resumeUrls = LpbHtmlParser.extractActionUrls(html, "http://10.0.0.1", PortalAction.RESUME)
        assertEquals("http://10.0.0.1/resume?id=99", resumeUrls.first())
        assertTrue(resumeUrls.contains("http://10.0.0.1/index.php?resume=1"))
    }

    @Test
    fun `rssi 60s sliding window retains only readings from the trailing 60 seconds`() {
        val now = 100_000L
        val existing = listOf(
            com.example.data.RssiSample(timestampMs = now - 65_000L, rssiDbm = -80), // expired (> 60s)
            com.example.data.RssiSample(timestampMs = now - 30_000L, rssiDbm = -68)  // kept
        )
        val updated = com.example.data.GuardStateBus.trimAndAppendSample(
            existing = existing,
            rssiDbm = -64,
            isConnected = true,
            nowMs = now
        )
        assertEquals(2, updated.size)
        assertEquals(-68, updated[0].rssiDbm)
        assertEquals(-64, updated[1].rssiDbm)
    }

    @Test
    fun `monthly savings duration formatting and JSON roundtrip preserve local stats`() {
        assertEquals("0m", com.example.data.GuardPreferencesRepository.formatSavedDuration(0L))
        assertEquals("45s", com.example.data.GuardPreferencesRepository.formatSavedDuration(45L))
        assertEquals("14m", com.example.data.GuardPreferencesRepository.formatSavedDuration(840L))
        assertEquals("2h 15m", com.example.data.GuardPreferencesRepository.formatSavedDuration(8100L))

        val original = mapOf(
            "2026-11" to com.example.data.MonthlySavingsRecord(
                monthKey = "2026-11",
                totalSavedSeconds = 5400L,
                autoPauseCount = 4,
                totalPauseCount = 5,
                longestPauseSeconds = 2100L
            )
        )
        val encoded = com.example.data.GuardPreferencesRepository.encodeMonthlySavings(original)
        val decoded = com.example.data.GuardPreferencesRepository.decodeMonthlySavings(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `portal smart retry backs off exponentially up to 120s cap and resets on success`() {
        var fakeNow = 10_000L
        val policy = com.example.network.PortalRetryPolicy(
            baseDelayMs = 4_000L,
            maxDelayMs = 120_000L,
            clock = { fakeNow }
        )

        assertTrue(policy.shouldAttemptAutoRetry())

        // Failure 1 -> 4s
        val f1 = policy.recordFailure("Timeout")
        assertEquals(1, f1.consecutiveFailures)
        assertEquals(4_000L, f1.lastBackoffMs)
        assertEquals(false, policy.shouldAttemptAutoRetry())
        assertEquals(4, policy.remainingCooldownSeconds())

        // Failure 2 -> 8s
        fakeNow += 4_000L
        assertTrue(policy.shouldAttemptAutoRetry())
        val f2 = policy.recordFailure("Timeout")
        assertEquals(8_000L, f2.lastBackoffMs)

        // Failure 3 -> 16s, 4 -> 32s, 5 -> 64s, 6 -> 120s (capped), 7 -> 120s (capped)
        assertEquals(16_000L, com.example.network.PortalRetryPolicy.computeBackoffMs(3))
        assertEquals(32_000L, com.example.network.PortalRetryPolicy.computeBackoffMs(4))
        assertEquals(64_000L, com.example.network.PortalRetryPolicy.computeBackoffMs(5))
        assertEquals(120_000L, com.example.network.PortalRetryPolicy.computeBackoffMs(6))
        assertEquals(120_000L, com.example.network.PortalRetryPolicy.computeBackoffMs(10))

        // Success resets immediately
        val resetState = policy.recordSuccess()
        assertEquals(0, resetState.consecutiveFailures)
        assertEquals(0L, resetState.lastBackoffMs)
        assertTrue(policy.shouldAttemptAutoRetry())
    }

    @Test
    fun `oem battery helper detects Samsung, Xiaomi, OPPO, Vivo and Stock manufacturers`() {
        assertEquals(
            com.example.ui.components.OemBrand.SAMSUNG,
            com.example.ui.components.OemBatteryGuideCatalog.detectOemBrand("samsung", "galaxy")
        )
        assertEquals(
            com.example.ui.components.OemBrand.XIAOMI,
            com.example.ui.components.OemBatteryGuideCatalog.detectOemBrand("Xiaomi", "Redmi")
        )
        assertEquals(
            com.example.ui.components.OemBrand.XIAOMI,
            com.example.ui.components.OemBatteryGuideCatalog.detectOemBrand("POCO", "poco")
        )
        assertEquals(
            com.example.ui.components.OemBrand.OPPO_REALME,
            com.example.ui.components.OemBatteryGuideCatalog.detectOemBrand("OPPO", "realme")
        )
        assertEquals(
            com.example.ui.components.OemBrand.VIVO,
            com.example.ui.components.OemBatteryGuideCatalog.detectOemBrand("vivo", "iQOO")
        )
        assertEquals(
            com.example.ui.components.OemBrand.STOCK,
            com.example.ui.components.OemBatteryGuideCatalog.detectOemBrand("Google", "Pixel 8")
        )

        com.example.ui.components.OemBrand.entries.forEach { brand ->
            assertTrue(com.example.ui.components.OemBatteryGuideCatalog.stepsFor(brand).isNotEmpty())
        }
    }
}
