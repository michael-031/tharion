package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrackGray

data class StatCell(val value: String, val caption: String)

/**
 * 2x2 Hairline-divided stat grid for compact Home status telemetry.
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
 * Horizontal progress bar row for threshold tuning in Settings.
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
