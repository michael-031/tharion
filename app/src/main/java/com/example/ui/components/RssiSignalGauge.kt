package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberCaution
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrackGray
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun dbmToSignalPercent(rssiDbm: Int): Int {
    return (((rssiDbm + 95f) / 50f) * 100f).roundToInt().coerceIn(0, 100)
}

/**
 * Circular ring gauge styled directly after Screen 1 of the design sample:
 * thick dark track (#2E333B), crisp pure white sweep arc from 12 o'clock,
 * and a hollow dark circular thumb knob with a white border at the arc tip.
 */
@Composable
fun RssiSignalGauge(
    rssiDbm: Int?,
    isWifiConnected: Boolean,
    pauseThresholdDbm: Int,
    pauseStrikes: Int,
    resumeStrikes: Int,
    modifier: Modifier = Modifier,
    gaugeSize: Dp = 226.dp
) {
    val minDbm = -100f
    val maxDbm = -30f
    val rawFraction = if (isWifiConnected && rssiDbm != null) {
        ((rssiDbm.toFloat() - minDbm) / (maxDbm - minDbm)).coerceIn(0.06f, 0.96f)
    } else {
        0f
    }

    val animatedFraction by animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "rssi_ring_sweep"
    )

    val thresholdFraction = ((pauseThresholdDbm.toFloat() - minDbm) / (maxDbm - minDbm)).coerceIn(0f, 1f)

    val statusText = when {
        !isWifiConnected || rssiDbm == null -> "NOT CONNECTED"
        rssiDbm <= pauseThresholdDbm -> "TOO FAR • PAUSING"
        rssiDbm <= pauseThresholdDbm + 8 -> "GETTING WEAK"
        else -> "GOOD SIGNAL"
    }

    val statusColor = when {
        !isWifiConnected || rssiDbm == null -> TextMuted
        rssiDbm <= pauseThresholdDbm -> RoseDanger
        rssiDbm <= pauseThresholdDbm + 8 -> AmberCaution
        else -> TextSecondary
    }

    val signalPct = if (isWifiConnected && rssiDbm != null) dbmToSignalPercent(rssiDbm) else null
    val isLarge = gaugeSize >= 250.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(gaugeSize),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = if (isLarge) 26.dp.toPx() else 22.dp.toPx()
                val inset = strokePx / 2f + 10.dp.toPx()
                val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                val topLeft = Offset(inset, inset)
                val radius = arcSize.width / 2f
                val startAngle = -90f
                val sweepAngle = 360f * animatedFraction

                // Full 360 dark track
                drawArc(
                    color = TrackGray,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx)
                )

                // Threshold notch mark on the track
                val thresholdRad = ((startAngle + 360f * thresholdFraction) * PI / 180.0)
                drawLine(
                    color = SlateBorder.copy(alpha = 0.9f),
                    start = Offset(
                        center.x + ((radius - strokePx * 0.45f) * cos(thresholdRad)).toFloat(),
                        center.y + ((radius - strokePx * 0.45f) * sin(thresholdRad)).toFloat()
                    ),
                    end = Offset(
                        center.x + ((radius + strokePx * 0.45f) * cos(thresholdRad)).toFloat(),
                        center.y + ((radius + strokePx * 0.45f) * sin(thresholdRad)).toFloat()
                    ),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Pure white active sweep arc + hollow dark thumb knob
                if (animatedFraction > 0f) {
                    drawArc(
                        color = PureWhite,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )

                    val endRad = ((startAngle + sweepAngle) * PI / 180.0)
                    val knobCenter = Offset(
                        x = center.x + (radius * cos(endRad)).toFloat(),
                        y = center.y + (radius * sin(endRad)).toFloat()
                    )
                    val knobRadius = strokePx * 0.42f
                    drawCircle(
                        color = ObsidianBg,
                        radius = knobRadius,
                        center = knobCenter
                    )
                    drawCircle(
                        color = PureWhite,
                        radius = knobRadius,
                        center = knobCenter,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (signalPct != null) "$signalPct%" else "--",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = if (isLarge) 56.sp else 46.sp
                    ),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isWifiConnected && rssiDbm != null) "$rssiDbm dBm" else "No WiFi",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.0.sp),
                    color = statusColor
                )
            }
        }

        if (pauseStrikes > 0 || resumeStrikes > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pauseStrikes > 0) {
                    Text(
                        text = "Weak signal check $pauseStrikes/2 — pausing soon",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                        color = RoseDanger
                    )
                }
                if (resumeStrikes > 0) {
                    Text(
                        text = "Strong signal check $resumeStrikes/2 — resuming soon",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                        color = PureWhite
                    )
                }
            }
        }
    }
}
