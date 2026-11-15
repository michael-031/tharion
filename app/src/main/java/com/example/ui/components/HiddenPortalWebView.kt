package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.GuardStateBus
import com.example.data.LogLevel
import com.example.data.PortalAction
import com.example.data.WebViewFallbackRequest
import com.example.network.WifiNetworkBinder

/**
 * Method B Fallback: Hidden 1dp x 1dp WebView pointed at http://10.0.0.1.
 * Injects DOM JavaScript to find and .click() any <a>, <button>, or <input>
 * matching "pause time" or "resume" for custom/modified LPB portal templates.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun HiddenPortalWebView() {
    val context = LocalContext.current
    val wifiBinder = remember { WifiNetworkBinder(context) }
    var activeRequest by remember { mutableStateOf<WebViewFallbackRequest?>(null) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = false
            settings.blockNetworkImage = true
        }
    }

    DisposableEffect(webView) {
        val bridge = object {
            @JavascriptInterface
            fun onResult(requestId: Long, found: Boolean, detail: String) {
                if (found) {
                    GuardStateBus.appendLog(
                        "Method B (WebView JS #$requestId): $detail",
                        LogLevel.SUCCESS
                    )
                } else {
                    GuardStateBus.appendLog(
                        "Method B (WebView JS #$requestId): $detail",
                        LogLevel.WARN
                    )
                }
            }
        }

        webView.addJavascriptInterface(bridge, "LpbBridge")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val req = activeRequest ?: return
                val targetKeywords = when (req.action) {
                    PortalAction.PAUSE -> "['pause time', 'pause session', 'pause']"
                    PortalAction.RESUME -> "['resume time', 'resume session', 'resume']"
                }

                val js = """
                    (function() {
                      try {
                        var keywords = $targetKeywords;
                        var nodes = document.querySelectorAll('a, button, input[type="button"], input[type="submit"]');
                        for (var i = 0; i < nodes.length; i++) {
                          var el = nodes[i];
                          var txt = ((el.innerText || el.textContent || el.value || '') + ' ' + (el.getAttribute('href') || '')).toLowerCase();
                          for (var k = 0; k < keywords.length; k++) {
                            if (txt.indexOf(keywords[k]) !== -1) {
                              el.click();
                              LpbBridge.onResult(${req.requestId}, true, 'Clicked DOM element: ' + (el.innerText || el.value || el.tagName).trim());
                              return;
                            }
                          }
                        }
                        LpbBridge.onResult(${req.requestId}, false, 'No matching button/link found on ' + window.location.href);
                      } catch (err) {
                        LpbBridge.onResult(${req.requestId}, false, 'JS error: ' + err.message);
                      }
                    })();
                """.trimIndent()

                view?.evaluateJavascript(js, null)
                activeRequest = null
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                val req = activeRequest
                if (req != null && request?.isForMainFrame == true) {
                    GuardStateBus.appendLog(
                        "Method B (WebView JS #${req.requestId}) load error: ${error?.description}",
                        LogLevel.ERROR
                    )
                    activeRequest = null
                }
            }
        }

        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    LaunchedEffect(Unit) {
        GuardStateBus.webViewCommands.collect { request ->
            wifiBinder.bindProcessToWifi()
            activeRequest = request
            webView.loadUrl(request.portalBaseUrl)
        }
    }

    AndroidView(
        factory = { webView },
        modifier = Modifier
            .size(1.dp)
            .alpha(0f)
    )
}
