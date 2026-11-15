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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RssiSample
import com.example.ui.theme.AbyssBlack
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrackGray

/**
 * Monochrome pill segmented toggle matching "Weekly | Monthly" in Screen 2 of the sample.
 */
@Composable
fun SegmentedPillToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(SlateCardElevated)
            .border(1.dp, SlateBorder, RoundedCornerShape(999.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (selected) PureWhite else Color.Transparent)
                    .clickable { onSelect(index) }
                    .heightIn(min = 36.dp)
                    .padding(horizontal = 18.dp, vertical = 7.dp)
                    .testTag("segment_${label.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    color = if (selected) AbyssBlack else TextSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Clean monochrome RSSI line chart with vertical dashed gridlines,
 * white polyline, ringed point highlight, and crisp white floating tooltip card.
 */
@Composable
fun RssiHistoryLineChart(
    history: List<Int>,
    pauseThresholdDbm: Int,
    modifier: Modifier = Modifier
) {
    val samples = if (history.size >= 2) history else listOf(-60, -61, -59, -63, -62, history.lastOrNull() ?: -62)
    val latest = samples.last()
    val latestPct = dbmToSignalPercent(latest)
    val margin = latest - pauseThresholdDbm
    val marginLabel = if (margin > 0) "$latest dBm • Safe" else "$latest dBm • Pause zone"

    Box(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val minVal = -95f
                    val maxVal = -35f
                    val gridCols = 6
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                    // Vertical dashed gridlines + horizontal baseline
                    for (i in 0..gridCols) {
                        val x = size.width * (i.toFloat() / gridCols.toFloat())
                        drawLine(
                            color = SlateBorder.copy(alpha = 0.55f),
                            start = Offset(x, 8.dp.toPx()),
                            end = Offset(x, size.height - 8.dp.toPx()),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }

                    // Threshold horizontal reference line
                    val threshNorm = ((pauseThresholdDbm - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                    val threshY = size.height - 12.dp.toPx() - threshNorm * (size.height - 24.dp.toPx())
                    drawLine(
                        color = TextMuted.copy(alpha = 0.65f),
                        start = Offset(0f, threshY),
                        end = Offset(size.width, threshY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )

                    // White polyline
                    val path = Path()
                    val points = samples.mapIndexed { idx, value ->
                        val x = if (samples.size == 1) size.width / 2f else {
                            size.width * (idx.toFloat() / (samples.size - 1).toFloat())
                        }
                        val norm = ((value.toFloat() - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                        val y = size.height - 12.dp.toPx() - norm * (size.height - 24.dp.toPx())
                        Offset(x, y)
                    }

                    points.forEachIndexed { idx, pt ->
                        if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                    }

                    drawPath(
                        path = path,
                        color = PureWhite,
                        style = Stroke(
                            width = 2.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Highlight last sample with hollow ringed dot
                    val lastPt = points.last()
                    drawCircle(
                        color = PureWhite.copy(alpha = 0.25f),
                        radius = 10.dp.toPx(),
                        center = lastPt
                    )
                    drawCircle(
                        color = ObsidianBg,
                        radius = 5.dp.toPx(),
                        center = lastPt
                    )
                    drawCircle(
                        color = PureWhite,
                        radius = 5.dp.toPx(),
                        center = lastPt,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }

                // Crisp white floating tooltip card (matching Screen 2 tooltip)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 12.dp)
                        .background(PureWhite, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SIGNAL NOW",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextMuted
                    )
                    Text(
                        text = "$latestPct%",
                        style = MaterialTheme.typography.titleMedium,
                        color = AbyssBlack,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = marginLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = SlateCard
                    )
                }
            }

            // Sample index ticks (60s window)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("-60s", "-45s", "-30s", "-15s", "Now").forEach { tick ->
                    Text(
                        text = tick,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextMuted
                    )
                }
            }
        }
    }
}

/**
 * Compact 60-second RSSI trend line chart displayed directly on the main dashboard.
 * Uses local-only device readings stored in memory over the trailing 60-second window.
 */
@Composable
fun MiniRssiTrendChart60s(
    samples: List<RssiSample>,
    pauseThresholdDbm: Int,
    modifier: Modifier = Modifier
) {
    val minDbm = samples.minOfOrNull { it.rssiDbm }
    val maxDbm = samples.maxOfOrNull { it.rssiDbm }
    val rangeLabel = if (minDbm != null && maxDbm != null) {
        "${dbmToSignalPercent(minDbm)}% – ${dbmToSignalPercent(maxDbm)}%"
    } else {
        "On-device only"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SlateCard)
            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("mini_rssi_trend_chart"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Last 60s WiFi Signal",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = rangeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        if (samples.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Waiting for live WiFi readings…",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
            ) {
                val minVal = -95f
                val maxVal = -35f
                val verticalPad = 8.dp.toPx()
                val usableHeight = (size.height - verticalPad * 2f).coerceAtLeast(1f)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)

                // Subtle vertical grid dividers at -60s, -30s, Now
                listOf(0f, 0.5f, 1f).forEach { xFrac ->
                    val x = size.width * xFrac
                    drawLine(
                        color = SlateBorder.copy(alpha = 0.45f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                // Auto-Pause threshold horizontal reference line
                val threshNorm = ((pauseThresholdDbm - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                val threshY = size.height - verticalPad - threshNorm * usableHeight
                drawLine(
                    color = RoseDanger.copy(alpha = 0.55f),
                    start = Offset(0f, threshY),
                    end = Offset(size.width, threshY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )

                val latestTs = samples.last().timestampMs
                val startTs = latestTs - 60_000L
                val points = if (samples.size == 1) {
                    val norm = ((samples.first().rssiDbm - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                    val y = size.height - verticalPad - norm * usableHeight
                    listOf(Offset(size.width * 0.85f, y), Offset(size.width, y))
                } else {
                    samples.map { sample ->
                        val xFrac = ((sample.timestampMs - startTs).toFloat() / 60_000f).coerceIn(0f, 1f)
                        val norm = ((sample.rssiDbm.toFloat() - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                        Offset(
                            x = xFrac * size.width,
                            y = size.height - verticalPad - norm * usableHeight
                        )
                    }
                }

                val linePath = Path().apply {
                    points.forEachIndexed { index, pt ->
                        if (index == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                }

                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(points.last().x, size.height)
                    lineTo(points.first().x, size.height)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(PureWhite.copy(alpha = 0.16f), Color.Transparent)
                    )
                )

                drawPath(
                    path = linePath,
                    color = PureWhite,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                val lastPt = points.last()
                drawCircle(
                    color = SlateCard,
                    radius = 4.dp.toPx(),
                    center = lastPt
                )
                drawCircle(
                    color = PureWhite,
                    radius = 4.dp.toPx(),
                    center = lastPt,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "-60s",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextMuted
            )
            Text(
                text = "Auto-pause line ($pauseThresholdDbm dBm)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextMuted
            )
            Text(
                text = "Now",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextSecondary
            )
        }
    }
}

data class StatCell(val value: String, val caption: String)

/**
 * 2x2 Hairline-divided stat grid matching Screen 2 of the design sample.
 */
@Composable
fun HairlineStatGrid(
    topLeft: StatCell,
    topRight: StatCell,
    bottomLeft: StatCell,
    bottomRight: StatCell,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = SlateBorder, thickness = 1.dp)
        Row(modifier = Modifier.fillMaxWidth()) {
            StatGridCell(cell = topLeft, modifier = Modifier.weight(1f))
            VerticalDivider(color = SlateBorder, thickness = 1.dp, modifier = Modifier.height(74.dp))
            StatGridCell(cell = topRight, modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = SlateBorder, thickness = 1.dp)
        Row(modifier = Modifier.fillMaxWidth()) {
            StatGridCell(cell = bottomLeft, modifier = Modifier.weight(1f))
            VerticalDivider(color = SlateBorder, thickness = 1.dp, modifier = Modifier.height(74.dp))
            StatGridCell(cell = bottomRight, modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = SlateBorder, thickness = 1.dp)
    }
}

@Composable
private fun StatGridCell(
    cell: StatCell,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(74.dp)
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = cell.value,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = cell.caption.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.9.sp
            ),
            color = TextMuted,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * 3-ring concentric signal distribution gauge matching "Mood level" in Screen 3 of the sample.
 */
@Composable
fun SignalZoneConcentricRings(
    history: List<Int>,
    pauseThresholdDbm: Int,
    modifier: Modifier = Modifier
) {
    val samples = if (history.isNotEmpty()) history else listOf(-62)
    val total = samples.size.toFloat()
    val strongCount = samples.count { it > pauseThresholdDbm + 7 }
    val cautionCount = samples.count { it in (pauseThresholdDbm + 1)..(pauseThresholdDbm + 7) }
    val triggerCount = samples.count { it <= pauseThresholdDbm }

    val strongPct = ((strongCount / total) * 100).toInt()
    val cautionPct = ((cautionCount / total) * 100).toInt()
    val triggerPct = (100 - strongPct - cautionPct).coerceAtLeast(0)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Canvas(modifier = Modifier.size(134.dp)) {
            val strokePx = 9.dp.toPx()
            val gapPx = 7.dp.toPx()

            val radiiFractions = listOf(
                strongPct / 100f to PureWhite,
                cautionPct / 100f to Color(0xFFB0B6C0),
                triggerPct / 100f to Color(0xFF7B828E)
            )

            radiiFractions.forEachIndexed { ringIndex, (frac, arcColor) ->
                val inset = strokePx / 2f + ringIndex * (strokePx + gapPx)
                val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                val topLeft = Offset(inset, inset)

                drawArc(
                    color = TrackGray,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx)
                )

                if (frac > 0f) {
                    drawArc(
                        color = arcColor,
                        startAngle = -90f,
                        sweepAngle = 360f * frac.coerceIn(0.06f, 1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ZoneLegendItem(
                pct = "$strongPct%",
                caption = "GOOD SIGNAL",
                dotColor = PureWhite
            )
            ZoneLegendItem(
                pct = "$cautionPct%",
                caption = "GETTING WEAK",
                dotColor = TextSecondary
            )
            ZoneLegendItem(
                pct = "$triggerPct%",
                caption = "TOO FAR (PAUSE)",
                dotColor = TextMuted
            )
        }
    }
}

@Composable
private fun ZoneLegendItem(
    pct: String,
    caption: String,
    dotColor: Color
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(dotColor, CircleShape)
        )
        Column {
            Text(
                text = pct,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = 0.7.sp
                ),
                color = TextMuted
            )
        }
    }
}

/**
 * Horizontal progress insight row matching "Habits insights" in Screen 3 of the sample.
 */
@Composable
fun InsightProgressBarRow(
    title: String,
    leftCaption: String,
    rightCaption: String,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val clamped = fraction.coerceIn(0.05f, 1f)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(TrackGray)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(PureWhite)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = leftCaption,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextMuted
            )
            Text(
                text = rightCaption,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextSecondary
            )
        }
    }
}
