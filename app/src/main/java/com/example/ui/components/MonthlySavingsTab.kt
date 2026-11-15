package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GuardPreferencesRepository
import com.example.data.GuardUiState
import com.example.data.MonthlySavingsRecord
import com.example.data.SessionStatus
import com.example.ui.theme.AbyssBlack
import com.example.ui.theme.AmberCaution
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrackGray
import kotlinx.coroutines.delay

@Composable
fun MonthlySavingsTab(
    uiState: GuardUiState,
    onResetMonthlySavings: () -> Unit
) {
    val savings = uiState.savings
    val isCurrentlyPaused = uiState.telemetry.sessionStatus == SessionStatus.PAUSED &&
        savings.activePauseStartedAtMs > 0L

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isCurrentlyPaused, savings.activePauseStartedAtMs) {
        nowMs = System.currentTimeMillis()
        while (isCurrentlyPaused) {
            delay(1000L)
            nowMs = System.currentTimeMillis()
        }
    }

    val monthKeys = remember(nowMs / 60_000L) {
        GuardPreferencesRepository.recentMonthKeys(count = 6, nowMs = nowMs)
    }
    var selectedMonthIndex by rememberSaveable { mutableIntStateOf(monthKeys.lastIndex) }
    val safeIndex = selectedMonthIndex.coerceIn(0, monthKeys.lastIndex)
    val selectedMonthKey = monthKeys[safeIndex]
    val selectedMonthTitle = GuardPreferencesRepository.formatMonthTitle(selectedMonthKey)

    val maxLimitSeconds = (uiState.settings.pauseExpiryMinutes * 60).coerceAtLeast(60).toLong()
    val liveElapsedSeconds = if (isCurrentlyPaused && savings.activePauseStartedAtMs > 0L) {
        ((nowMs - savings.activePauseStartedAtMs) / 1000L).coerceIn(0L, maxLimitSeconds)
    } else {
        0L
    }
    val activeMonthKey = savings.activePauseMonthKey.ifBlank { monthKeys.last() }

    fun effectiveRecordFor(monthKey: String): MonthlySavingsRecord {
        val base = savings.monthsByKey[monthKey] ?: MonthlySavingsRecord(monthKey = monthKey)
        return if (liveElapsedSeconds > 0L && monthKey == activeMonthKey) {
            base.copy(
                totalSavedSeconds = base.totalSavedSeconds + liveElapsedSeconds,
                longestPauseSeconds = maxOf(base.longestPauseSeconds, liveElapsedSeconds)
            )
        } else {
            base
        }
    }

    val record = effectiveRecordFor(selectedMonthKey)
    val avgSavedSeconds = if (record.totalPauseCount > 0) {
        record.totalSavedSeconds / record.totalPauseCount
    } else {
        0L
    }

    var showResetConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("monthly_savings_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Month Switcher Header (< November 2026 >) matching Screen 2 style
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedMonthTitle,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.testTag("savings_month_title")
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { if (safeIndex > 0) selectedMonthIndex = safeIndex - 1 },
                    enabled = safeIndex > 0,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("savings_prev_month_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Month",
                        tint = if (safeIndex > 0) TextPrimary else TextMuted
                    )
                }
                IconButton(
                    onClick = { if (safeIndex < monthKeys.lastIndex) selectedMonthIndex = safeIndex + 1 },
                    enabled = safeIndex < monthKeys.lastIndex,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("savings_next_month_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Month",
                        tint = if (safeIndex < monthKeys.lastIndex) TextPrimary else TextMuted
                    )
                }
            }
        }

        // Hero Time Saved Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SlateCard)
                .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 22.dp)
                .testTag("savings_hero_card"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassBottom,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "TOTAL TIME SAVED",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.0.sp),
                    color = TextSecondary
                )
            }

            Text(
                text = GuardPreferencesRepository.formatSavedDuration(record.totalSavedSeconds),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp),
                color = PureWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("savings_total_duration_text")
            )

            Text(
                text = when {
                    record.totalSavedSeconds > 0L && record.autoPauseCount > 0 ->
                        "Saved across ${record.autoPauseCount} auto-paused session${if (record.autoPauseCount == 1) "" else "s"} in $selectedMonthTitle"
                    record.totalSavedSeconds > 0L ->
                        "Saved across ${record.totalPauseCount} paused session${if (record.totalPauseCount == 1) "" else "s"} in $selectedMonthTitle"
                    else ->
                        "Every minute paused while you walk away stays saved for later."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            if (isCurrentlyPaused && selectedMonthKey == activeMonthKey) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(SlateCardElevated)
                        .border(1.dp, AmberCaution.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(AmberCaution, CircleShape)
                    )
                    Text(
                        text = "Counting live pause right now",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberCaution
                    )
                }
            }
        }

        // 2x2 Hairline Stat Grid
        HairlineStatGrid(
            topLeft = StatCell(
                value = "${record.autoPauseCount}",
                caption = "Auto-Pauses"
            ),
            topRight = StatCell(
                value = "${record.totalPauseCount}",
                caption = "All Pauses"
            ),
            bottomLeft = StatCell(
                value = GuardPreferencesRepository.formatSavedDuration(avgSavedSeconds),
                caption = "Avg Per Pause"
            ),
            bottomRight = StatCell(
                value = GuardPreferencesRepository.formatSavedDuration(record.longestPauseSeconds),
                caption = "Longest Pause"
            )
        )

        // 6-Month Bar Summary Card
        val monthTotals = monthKeys.map { key ->
            key to effectiveRecordFor(key).totalSavedSeconds
        }
        MonthlySavingsBarChart(
            monthTotals = monthTotals,
            selectedMonthKey = selectedMonthKey,
            onSelectMonthKey = { key ->
                val idx = monthKeys.indexOf(key)
                if (idx >= 0) selectedMonthIndex = idx
            }
        )

        // Empty guidance or Local Privacy + Reset row
        if (savings.monthsByKey.isEmpty() && liveElapsedSeconds == 0L) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SlateCardElevated)
                    .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "How time saved is counted",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Whenever Eltharion pauses your Piso WiFi session (or you tap Pause Now), it tracks how long your session stays safely paused until you resume.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stored locally on this device",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            if (savings.monthsByKey.isNotEmpty()) {
                TextButton(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("reset_savings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Reset Monthly Summary",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", color = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = SlateCard,
            title = {
                Text(
                    text = "Reset Monthly Summary?",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "This will clear all saved time records stored on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetMonthlySavings()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoseDanger,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier.testTag("confirm_reset_savings_button")
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MonthlySavingsBarChart(
    monthTotals: List<Pair<String, Long>>,
    selectedMonthKey: String,
    onSelectMonthKey: (String) -> Unit
) {
    val maxSeconds = monthTotals.maxOfOrNull { it.second }?.coerceAtLeast(60L) ?: 60L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateCard)
            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("monthly_savings_bar_chart"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "6-Month Overview",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = "Tap a month",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            monthTotals.forEach { (monthKey, seconds) ->
                val isSelected = monthKey == selectedMonthKey
                val fraction = if (seconds > 0L) {
                    (seconds.toFloat() / maxSeconds.toFloat()).coerceIn(0.12f, 1f)
                } else {
                    0.06f
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectMonthKey(monthKey) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (seconds > 0L) GuardPreferencesRepository.formatSavedDuration(seconds) else "0m",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isSelected) PureWhite else TextMuted,
                        maxLines = 1
                    )

                    Canvas(
                        modifier = Modifier
                            .width(22.dp)
                            .height(74.dp)
                    ) {
                        val barWidth = size.width
                        val radius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        drawRoundRect(
                            color = TrackGray,
                            topLeft = Offset.Zero,
                            size = size,
                            cornerRadius = radius
                        )
                        val barHeight = size.height * fraction
                        drawRoundRect(
                            color = when {
                                seconds == 0L && isSelected -> PureWhite.copy(alpha = 0.35f)
                                isSelected -> PureWhite
                                seconds > 0L -> TextSecondary
                                else -> TrackGray
                            },
                            topLeft = Offset(0f, size.height - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = radius
                        )
                    }

                    Text(
                        text = GuardPreferencesRepository.formatMonthShort(monthKey),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) PureWhite else TextMuted
                    )
                }
            }
        }
    }
}
