package com.example.network

import android.content.Context
import com.example.data.GuardStateBus
import com.example.data.LogLevel
import com.example.data.PortalAction
import com.example.data.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed interface PortalExecutionResult {
    data class Success(val method: String, val detail: String) : PortalExecutionResult
    data class DispatchedToWebView(val requestId: Long) : PortalExecutionResult
    data class Failure(val reason: String, val backoffSeconds: Int = 0) : PortalExecutionResult
}

/**
 * Dual-method LPB Piso WiFi portal controller targeting http://10.0.0.1:
 *  - Method A: Fast OkHttp GET (2.5s timeout) over WiFi-bound SocketFactory + Regex HTML link parser
 *              with /index.php?pause=1 or ?resume=1 fallback.
 *  - Method B: Hidden 1x1 WebView DOM JS injection fallback for custom/JS-heavy LPB templates.
 *
 * Includes Smart Retry with exponential backoff:
 *  - Brief in-call exponential micro-retry (1s -> 2s) if 10.0.0.1 has transient packet loss.
 *  - Cross-cycle exponential backoff (4s -> 8s -> 16s -> 32s -> 64s -> 120s cap) when unreachable,
 *    preventing battery drain from repeated background requests.
 */
class LpbPortalController(context: Context) {

    private val wifiBinder = WifiNetworkBinder(context)

    private fun buildWifiBoundClient(): Pair<OkHttpClient, Boolean> {
        val wifiNetwork = wifiBinder.bindProcessToWifi()
        val builder = OkHttpClient.Builder()
            .connectTimeout(2500, TimeUnit.MILLISECONDS)
            .readTimeout(2500, TimeUnit.MILLISECONDS)
            .writeTimeout(2500, TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(true)

        if (wifiNetwork != null) {
            builder.socketFactory(wifiNetwork.socketFactory)
        }
        return builder.build() to (wifiNetwork != null)
    }

    suspend fun executeAction(
        portalBaseUrl: String,
        action: PortalAction,
        useWebViewFallback: Boolean,
        isAuto: Boolean = false
    ): PortalExecutionResult = withContext(Dispatchers.IO) {
        val cleanBase = portalBaseUrl.trim().trimEnd('/').ifEmpty { "http://10.0.0.1" }
        val simConfig = GuardStateBus.simulationConfig.value
        val retryPolicy = GuardStateBus.portalRetryPolicy

        if (isAuto && !retryPolicy.shouldAttemptAutoRetry()) {
            val remaining = retryPolicy.remainingCooldownSeconds()
            GuardStateBus.syncRetryTelemetry()
            return@withContext PortalExecutionResult.Failure(
                reason = "Smart retry cooling down (${remaining}s left) to save battery.",
                backoffSeconds = remaining
            )
        }

        if (!isAuto) {
            // Manual user action bypasses previous cooldown so user gets immediate feedback
            retryPolicy.reset()
            GuardStateBus.syncRetryTelemetry()
        }

        if (simConfig.enabled) {
            GuardStateBus.updateTelemetry {
                it.copy(
                    isWifiBound = true,
                    isExecutingPortalCommand = true,
                    isSimulated = true
                )
            }
            delay(250L)

            if (simConfig.simulateUnreachablePortal) {
                val failureState = retryPolicy.recordFailure("Simulated 10.0.0.1 unreachable")
                val backoffSec = (failureState.lastBackoffMs / 1000L).toInt()
                GuardStateBus.syncRetryTelemetry()
                GuardStateBus.updateTelemetry {
                    it.copy(
                        isExecutingPortalCommand = false,
                        lastMethodUsed = "Backoff ${backoffSec}s (#${failureState.consecutiveFailures})"
                    )
                }
                return@withContext PortalExecutionResult.Failure(
                    reason = "[SIM] $cleanBase unreachable. Smart retry backing off ${backoffSec}s (attempt #${failureState.consecutiveFailures}).",
                    backoffSeconds = backoffSec
                )
            }

            val mockLpbHtml = """
                <html><body>
                  <a href="/index.php?token=sim8821&amp;pause=1">Pause Time</a>
                  <a href="/index.php?token=sim8821&amp;resume=1">Resume Time</a>
                </body></html>
            """.trimIndent()
            val parsedUrls = LpbHtmlParser.extractActionUrls(mockLpbHtml, cleanBase, action)
            val targetUrl = parsedUrls.firstOrNull() ?: "$cleanBase/index.php?${action.name.lowercase()}=1"
            retryPolicy.recordSuccess()
            GuardStateBus.syncRetryTelemetry()
            GuardStateBus.updateTelemetry {
                it.copy(
                    isExecutingPortalCommand = false,
                    lastMethodUsed = "SIMULATED: Method A"
                )
            }
            return@withContext PortalExecutionResult.Success(
                method = "SIMULATED",
                detail = "[SIM] Parsed LPB link & fired ${action.label} -> $targetUrl"
            )
        }

        val (client, isBound) = buildWifiBoundClient()

        GuardStateBus.updateTelemetry {
            it.copy(
                isWifiBound = isBound,
                isExecutingPortalCommand = true,
                isSimulated = false
            )
        }

        if (!isBound) {
            GuardStateBus.appendLog(
                "No active WiFi interface found for binding; attempting direct $cleanBase request.",
                LogLevel.WARN
            )
        }

        try {
            val maxInCallAttempts = if (isAuto) 2 else 3
            var portalHtml: String? = null
            var hitUrl = ""
            var methodASucceeded = false
            var anyEndpointResponse = false

            for (attempt in 1..maxInCallAttempts) {
                if (attempt > 1) {
                    val microDelayMs = PortalRetryPolicy.computeBackoffMs(
                        failureCount = attempt - 1,
                        baseDelayMs = PortalRetryPolicy.IN_CALL_RETRY_BASE_MS,
                        maxDelayMs = PortalRetryPolicy.IN_CALL_RETRY_MAX_MS
                    )
                    GuardStateBus.appendLog(
                        "Endpoint $cleanBase timed out; smart retry #$attempt in ${microDelayMs / 1000}s...",
                        LogLevel.INFO
                    )
                    delay(microDelayMs)
                }

                // Method A Step 1: Fetch portal homepage HTML (2.5s timeout)
                portalHtml = fetchHtml(client, cleanBase)
                if (portalHtml != null) {
                    anyEndpointResponse = true
                }

                val candidateUrls = if (portalHtml != null) {
                    LpbHtmlParser.extractActionUrls(portalHtml, cleanBase, action)
                } else {
                    listOf(
                        "$cleanBase/index.php?${action.name.lowercase()}=1",
                        "$cleanBase/?${action.name.lowercase()}=1"
                    )
                }

                val parsedExplicitLink = portalHtml != null && candidateUrls.size > 2

                for (url in candidateUrls) {
                    val responseHtml = fetchHtml(client, url)
                    if (responseHtml != null) {
                        anyEndpointResponse = true
                        val inferred = LpbHtmlParser.inferSessionStatus(responseHtml)
                        val targetMatches = when (action) {
                            PortalAction.PAUSE -> inferred == SessionStatus.PAUSED
                            PortalAction.RESUME -> inferred == SessionStatus.ACTIVE
                        }
                        if (targetMatches || (parsedExplicitLink && url == candidateUrls.first())) {
                            methodASucceeded = true
                            hitUrl = url
                            break
                        }
                        if (hitUrl.isEmpty()) {
                            hitUrl = url
                        }
                    }
                }

                // Stop retrying immediately if endpoint responded or Method A succeeded
                if (methodASucceeded || anyEndpointResponse) {
                    break
                }
            }

            if (methodASucceeded) {
                retryPolicy.recordSuccess()
                GuardStateBus.syncRetryTelemetry()
                val msg = "Method A (${action.label}) succeeded via $hitUrl"
                GuardStateBus.updateTelemetry {
                    it.copy(
                        isExecutingPortalCommand = false,
                        lastMethodUsed = "Method A: HTTP Regex"
                    )
                }
                return@withContext PortalExecutionResult.Success("Method A", msg)
            }

            // Only invoke WebView fallback if the portal host responded over HTTP,
            // avoiding battery-draining WebView loads when 10.0.0.1 is completely unreachable.
            if (anyEndpointResponse && useWebViewFallback) {
                GuardStateBus.appendLog(
                    "Method A inconclusive on $cleanBase — invoking Method B (Hidden WebView JS)...",
                    LogLevel.INFO
                )
                val reqId = GuardStateBus.dispatchWebViewFallback(cleanBase, action)
                retryPolicy.recordSuccess()
                GuardStateBus.syncRetryTelemetry()
                GuardStateBus.updateTelemetry {
                    it.copy(
                        isExecutingPortalCommand = false,
                        lastMethodUsed = "Method B: WebView JS"
                    )
                }
                return@withContext PortalExecutionResult.DispatchedToWebView(reqId)
            }

            if (hitUrl.isNotEmpty()) {
                retryPolicy.recordSuccess()
                GuardStateBus.syncRetryTelemetry()
                GuardStateBus.updateTelemetry {
                    it.copy(
                        isExecutingPortalCommand = false,
                        lastMethodUsed = "Method A: Fallback Query"
                    )
                }
                return@withContext PortalExecutionResult.Success(
                    "Method A (Fallback)",
                    "Sent fallback ${action.label} request to $hitUrl"
                )
            }

            // Completely unreachable after smart in-call retries -> exponential cross-cycle backoff
            val failureState = retryPolicy.recordFailure("Portal $cleanBase unreachable")
            val backoffSec = (failureState.lastBackoffMs / 1000L).toInt()
            GuardStateBus.syncRetryTelemetry()
            GuardStateBus.updateTelemetry {
                it.copy(
                    isExecutingPortalCommand = false,
                    lastMethodUsed = "Backoff ${backoffSec}s (#${failureState.consecutiveFailures})"
                )
            }
            PortalExecutionResult.Failure(
                reason = "Portal $cleanBase unreachable. Smart retry backing off for ${backoffSec}s (attempt #${failureState.consecutiveFailures}) to save battery.",
                backoffSeconds = backoffSec
            )
        } catch (e: Exception) {
            val failureState = retryPolicy.recordFailure(e.message ?: "Unknown portal error")
            val backoffSec = (failureState.lastBackoffMs / 1000L).toInt()
            GuardStateBus.syncRetryTelemetry()
            GuardStateBus.updateTelemetry {
                it.copy(
                    isExecutingPortalCommand = false,
                    lastMethodUsed = "Backoff ${backoffSec}s (#${failureState.consecutiveFailures})"
                )
            }
            PortalExecutionResult.Failure(
                reason = "${e.message ?: "Portal unreachable"}. Backing off ${backoffSec}s (attempt #${failureState.consecutiveFailures}).",
                backoffSeconds = backoffSec
            )
        }
    }

    private fun fetchHtml(client: OkHttpClient, url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Cache-Control", "no-cache")
            .header("Connection", "close")
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string() ?: ""
                } else {
                    null
                }
            }
        } catch (_: IOException) {
            null
        }
    }
}
