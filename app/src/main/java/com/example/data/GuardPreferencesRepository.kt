package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val Context.guardDataStore: DataStore<Preferences> by preferencesDataStore(name = "lpb_guard_prefs")

class GuardPreferencesRepository(private val context: Context) {

    private object Keys {
        val PAUSE_THRESHOLD = intPreferencesKey("pause_threshold_dbm")
        val RESUME_THRESHOLD = intPreferencesKey("resume_threshold_dbm")
        val AUTO_RESUME_ENABLED = booleanPreferencesKey("auto_resume_enabled")
        val PORTAL_BASE_URL = stringPreferencesKey("portal_base_url")
        val PAUSE_EXPIRY_MINUTES = intPreferencesKey("pause_expiry_minutes")
        val USE_WEBVIEW_FALLBACK = booleanPreferencesKey("use_webview_fallback")
        val SIMPLE_MODE_ENABLED = booleanPreferencesKey("simple_mode_enabled")

        val MONTHLY_SAVINGS_JSON = stringPreferencesKey("monthly_savings_json")
        val ACTIVE_PAUSE_STARTED_AT_MS = longPreferencesKey("active_pause_started_at_ms")
        val ACTIVE_PAUSE_MONTH_KEY = stringPreferencesKey("active_pause_month_key")
        val ACTIVE_PAUSE_WAS_AUTO = booleanPreferencesKey("active_pause_was_auto")
    }

    val settingsFlow: Flow<GuardSettings> = context.guardDataStore.data.map { prefs ->
        GuardSettings(
            pauseThresholdDbm = prefs[Keys.PAUSE_THRESHOLD] ?: -75,
            resumeThresholdDbm = prefs[Keys.RESUME_THRESHOLD] ?: -65,
            autoResumeEnabled = prefs[Keys.AUTO_RESUME_ENABLED] ?: true,
            portalBaseUrl = prefs[Keys.PORTAL_BASE_URL] ?: "http://10.0.0.1",
            pauseExpiryMinutes = prefs[Keys.PAUSE_EXPIRY_MINUTES] ?: 60,
            useWebViewFallback = prefs[Keys.USE_WEBVIEW_FALLBACK] ?: true,
            simpleModeEnabled = prefs[Keys.SIMPLE_MODE_ENABLED] ?: false
        )
    }

    val savingsFlow: Flow<SavingsSummaryState> = context.guardDataStore.data.map { prefs ->
        SavingsSummaryState(
            monthsByKey = decodeMonthlySavings(prefs[Keys.MONTHLY_SAVINGS_JSON].orEmpty()),
            activePauseStartedAtMs = prefs[Keys.ACTIVE_PAUSE_STARTED_AT_MS] ?: 0L,
            activePauseMonthKey = prefs[Keys.ACTIVE_PAUSE_MONTH_KEY].orEmpty(),
            activePauseWasAuto = prefs[Keys.ACTIVE_PAUSE_WAS_AUTO] ?: false
        )
    }

    suspend fun setPauseThresholdDbm(value: Int) {
        val clamped = value.coerceIn(-90, -50)
        context.guardDataStore.edit { it[Keys.PAUSE_THRESHOLD] = clamped }
    }

    suspend fun setResumeThresholdDbm(value: Int) {
        val clamped = value.coerceIn(-80, -40)
        context.guardDataStore.edit { it[Keys.RESUME_THRESHOLD] = clamped }
    }

    suspend fun setAutoResumeEnabled(enabled: Boolean) {
        context.guardDataStore.edit { it[Keys.AUTO_RESUME_ENABLED] = enabled }
    }

    suspend fun setPortalBaseUrl(url: String) {
        val normalized = normalizePortalUrl(url)
        context.guardDataStore.edit { it[Keys.PORTAL_BASE_URL] = normalized }
    }

    suspend fun setPauseExpiryMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(10, 240)
        context.guardDataStore.edit { it[Keys.PAUSE_EXPIRY_MINUTES] = clamped }
    }

    suspend fun setUseWebViewFallback(enabled: Boolean) {
        context.guardDataStore.edit { it[Keys.USE_WEBVIEW_FALLBACK] = enabled }
    }

    suspend fun setSimpleModeEnabled(enabled: Boolean) {
        context.guardDataStore.edit { it[Keys.SIMPLE_MODE_ENABLED] = enabled }
    }

    suspend fun recordPauseStarted(
        isAuto: Boolean,
        maxLimitSeconds: Int,
        nowMs: Long = System.currentTimeMillis()
    ) {
        context.guardDataStore.edit { prefs ->
            val map = decodeMonthlySavings(prefs[Keys.MONTHLY_SAVINGS_JSON].orEmpty()).toMutableMap()

            // Finalize any previous unclosed pause before starting a new one
            val prevStart = prefs[Keys.ACTIVE_PAUSE_STARTED_AT_MS] ?: 0L
            if (prevStart > 0L) {
                val prevKey = prefs[Keys.ACTIVE_PAUSE_MONTH_KEY].orEmpty().ifBlank { currentMonthKey(prevStart) }
                val elapsed = ((nowMs - prevStart) / 1000L).coerceIn(0L, maxLimitSeconds.coerceAtLeast(60).toLong())
                val prevRecord = map[prevKey] ?: MonthlySavingsRecord(monthKey = prevKey)
                map[prevKey] = prevRecord.copy(
                    totalSavedSeconds = prevRecord.totalSavedSeconds + elapsed,
                    longestPauseSeconds = maxOf(prevRecord.longestPauseSeconds, elapsed)
                )
            }

            val monthKey = currentMonthKey(nowMs)
            val current = map[monthKey] ?: MonthlySavingsRecord(monthKey = monthKey)
            map[monthKey] = current.copy(
                autoPauseCount = current.autoPauseCount + if (isAuto) 1 else 0,
                totalPauseCount = current.totalPauseCount + 1
            )

            prefs[Keys.MONTHLY_SAVINGS_JSON] = encodeMonthlySavings(map)
            prefs[Keys.ACTIVE_PAUSE_STARTED_AT_MS] = nowMs
            prefs[Keys.ACTIVE_PAUSE_MONTH_KEY] = monthKey
            prefs[Keys.ACTIVE_PAUSE_WAS_AUTO] = isAuto
        }
    }

    suspend fun recordPauseEnded(
        maxLimitSeconds: Int,
        nowMs: Long = System.currentTimeMillis()
    ) {
        context.guardDataStore.edit { prefs ->
            val startedAt = prefs[Keys.ACTIVE_PAUSE_STARTED_AT_MS] ?: 0L
            if (startedAt <= 0L) return@edit

            val monthKey = prefs[Keys.ACTIVE_PAUSE_MONTH_KEY].orEmpty().ifBlank { currentMonthKey(startedAt) }
            val elapsed = ((nowMs - startedAt) / 1000L).coerceIn(0L, maxLimitSeconds.coerceAtLeast(60).toLong())

            val map = decodeMonthlySavings(prefs[Keys.MONTHLY_SAVINGS_JSON].orEmpty()).toMutableMap()
            val current = map[monthKey] ?: MonthlySavingsRecord(monthKey = monthKey)
            map[monthKey] = current.copy(
                totalSavedSeconds = current.totalSavedSeconds + elapsed,
                longestPauseSeconds = maxOf(current.longestPauseSeconds, elapsed)
            )

            prefs[Keys.MONTHLY_SAVINGS_JSON] = encodeMonthlySavings(map)
            prefs[Keys.ACTIVE_PAUSE_STARTED_AT_MS] = 0L
            prefs[Keys.ACTIVE_PAUSE_MONTH_KEY] = ""
            prefs[Keys.ACTIVE_PAUSE_WAS_AUTO] = false
        }
    }

    suspend fun clearMonthlySavings() {
        context.guardDataStore.edit { prefs ->
            prefs.remove(Keys.MONTHLY_SAVINGS_JSON)
            prefs[Keys.ACTIVE_PAUSE_STARTED_AT_MS] = 0L
            prefs[Keys.ACTIVE_PAUSE_MONTH_KEY] = ""
            prefs[Keys.ACTIVE_PAUSE_WAS_AUTO] = false
        }
    }

    companion object {
        fun normalizePortalUrl(raw: String): String {
            val trimmed = raw.trim().trimEnd('/')
            if (trimmed.isEmpty()) return "http://10.0.0.1"
            return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "http://$trimmed"
            }
        }

        fun currentMonthKey(nowMs: Long = System.currentTimeMillis()): String {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            return "%04d-%02d".format(Locale.US, year, month)
        }

        fun recentMonthKeys(count: Int = 6, nowMs: Long = System.currentTimeMillis()): List<String> {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMs
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val result = mutableListOf<String>()
            for (offset in (count - 1) downTo 0) {
                val c = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
                val y = c.get(Calendar.YEAR)
                val m = c.get(Calendar.MONTH) + 1
                result += "%04d-%02d".format(Locale.US, y, m)
            }
            return result
        }

        fun formatMonthTitle(monthKey: String): String {
            val parts = monthKey.split("-")
            if (parts.size != 2) return monthKey
            val year = parts[0].toIntOrNull() ?: return monthKey
            val month = parts[1].toIntOrNull()?.minus(1) ?: return monthKey
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
        }

        fun formatMonthShort(monthKey: String): String {
            val parts = monthKey.split("-")
            if (parts.size != 2) return monthKey
            val year = parts[0].toIntOrNull() ?: return monthKey
            val month = parts[1].toIntOrNull()?.minus(1) ?: return monthKey
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            return SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
        }

        fun formatSavedDuration(totalSeconds: Long): String {
            val safe = totalSeconds.coerceAtLeast(0L)
            val hours = safe / 3600L
            val mins = (safe % 3600L) / 60L
            val secs = safe % 60L
            return when {
                hours > 0L && mins > 0L -> "${hours}h ${mins}m"
                hours > 0L -> "${hours}h"
                mins > 0L -> "${mins}m"
                secs > 0L -> "${secs}s"
                else -> "0m"
            }
        }

        fun encodeMonthlySavings(map: Map<String, MonthlySavingsRecord>): String {
            return map.entries
                .sortedByDescending { it.key }
                .take(12)
                .joinToString(separator = ";") { (key, record) ->
                    "${key}=${record.totalSavedSeconds}|${record.autoPauseCount}|${record.totalPauseCount}|${record.longestPauseSeconds}"
                }
        }

        fun decodeMonthlySavings(raw: String): Map<String, MonthlySavingsRecord> {
            if (raw.isBlank()) return emptyMap()
            return try {
                val out = linkedMapOf<String, MonthlySavingsRecord>()
                raw.split(";").forEach { entry ->
                    val kv = entry.split("=")
                    if (kv.size != 2) return@forEach
                    val key = kv[0].trim()
                    val fields = kv[1].split("|")
                    if (key.isEmpty() || fields.size < 4) return@forEach
                    out[key] = MonthlySavingsRecord(
                        monthKey = key,
                        totalSavedSeconds = (fields[0].toLongOrNull() ?: 0L).coerceAtLeast(0L),
                        autoPauseCount = (fields[1].toIntOrNull() ?: 0).coerceAtLeast(0),
                        totalPauseCount = (fields[2].toIntOrNull() ?: 0).coerceAtLeast(0),
                        longestPauseSeconds = (fields[3].toLongOrNull() ?: 0L).coerceAtLeast(0L)
                    )
                }
                out
            } catch (_: Exception) {
                emptyMap()
            }
        }
    }
}
