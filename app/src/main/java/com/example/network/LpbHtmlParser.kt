package com.example.network

import com.example.data.PortalAction
import com.example.data.SessionStatus

/**
 * Parses LPB Piso WiFi captive portal HTML (http://10.0.0.1) to extract
 * dynamic session action links ($pausesessionlink / $resumesessionlink)
 * and builds deterministic fallback URLs.
 */
object LpbHtmlParser {

    // Matches <a ... href="URL" ...>...Pause Time...</a> (multi-line & case-insensitive)
    private val anchorHrefRegex = Regex(
        pattern = """<a\b[^>]*\bhref\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    // Matches onclick="location.href='URL'" or window.location='URL' inside buttons/inputs
    private val onclickLocationRegex = Regex(
        pattern = """<(?:button|input|a)\b[^>]*\bonclick\s*=\s*["'][^"']*(?:location(?:\.href)?\s*=\s*|open\s*\(\s*)['"]([^'"]+)['"][^"']*["'][^>]*>(.*?)(?:</(?:button|a)>|>)""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    // Matches direct PHP/query action URLs mentioning pause or resume
    private val directQueryRegex = Regex(
        pattern = """["']((?:https?://[^"'\s]+)?/?[^"'\s]*\b(?:pause|resume)[^"'\s]*)["']""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val htmlTagStripRegex = Regex("<[^>]*>")

    fun extractActionUrls(
        html: String,
        baseUrl: String,
        action: PortalAction
    ): List<String> {
        val cleanBase = baseUrl.trim().trimEnd('/')
        val keywords = when (action) {
            PortalAction.PAUSE -> listOf("pause time", "pause session", "pause")
            PortalAction.RESUME -> listOf("resume time", "resume session", "resume")
        }

        val candidates = linkedSetOf<String>()

        // 1. Anchor tags wrapping "Pause Time" or "Resume Time"
        anchorHrefRegex.findAll(html).forEach { match ->
            val href = match.groupValues[1].trim()
            val innerText = match.groupValues[2].replace(htmlTagStripRegex, " ").lowercase()
            if (href.isNotEmpty() && !href.startsWith("javascript:", ignoreCase = true) && !href.startsWith("#")) {
                if (keywords.any { innerText.contains(it) } ||
                    href.lowercase().contains(action.name.lowercase())
                ) {
                    resolveUrl(cleanBase, href)?.let { candidates.add(it) }
                }
            }
        }

        // 2. Buttons / inputs with inline onclick redirects
        onclickLocationRegex.findAll(html).forEach { match ->
            val targetUrl = match.groupValues[1].trim()
            val wholeMatch = match.groupValues[0].lowercase()
            if (targetUrl.isNotEmpty() && keywords.any { wholeMatch.contains(it) }) {
                resolveUrl(cleanBase, targetUrl)?.let { candidates.add(it) }
            }
        }

        // 3. Direct embedded link patterns (e.g., JS variables holding $pausesessionlink)
        directQueryRegex.findAll(html).forEach { match ->
            val url = match.groupValues[1].trim()
            val lower = url.lowercase()
            val matchesAction = when (action) {
                PortalAction.PAUSE -> lower.contains("pause") && !lower.contains("resume")
                PortalAction.RESUME -> lower.contains("resume") && !lower.contains("pause")
            }
            if (matchesAction && !lower.endsWith(".js") && !lower.endsWith(".css")) {
                resolveUrl(cleanBase, url)?.let { candidates.add(it) }
            }
        }

        // 4. Standard LPB fallback endpoints
        when (action) {
            PortalAction.PAUSE -> {
                candidates.add("$cleanBase/index.php?pause=1")
                candidates.add("$cleanBase/?pause=1")
            }
            PortalAction.RESUME -> {
                candidates.add("$cleanBase/index.php?resume=1")
                candidates.add("$cleanBase/?resume=1")
            }
        }

        return candidates.toList()
    }

    fun inferSessionStatus(html: String): SessionStatus? {
        val text = html.replace(htmlTagStripRegex, " ").lowercase()
        val hasPauseButton = text.contains("pause time") || text.contains("pause session")
        val hasResumeButton = text.contains("resume time") || text.contains("resume session") || text.contains("time is paused")
        return when {
            hasResumeButton && !hasPauseButton -> SessionStatus.PAUSED
            hasPauseButton && !hasResumeButton -> SessionStatus.ACTIVE
            else -> null
        }
    }

    internal fun resolveUrl(baseUrl: String, rawHref: String): String? {
        val href = rawHref.trim().replace("&amp;", "&")
        if (href.isEmpty()) return null
        return when {
            href.startsWith("http://", ignoreCase = true) ||
                href.startsWith("https://", ignoreCase = true) -> href
            href.startsWith("/") -> "$baseUrl$href"
            href.startsWith("?") -> "$baseUrl/$href"
            else -> "$baseUrl/$href"
        }
    }
}
