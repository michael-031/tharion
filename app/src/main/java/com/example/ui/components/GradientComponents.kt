package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AbyssBlack
import com.example.ui.theme.GlassBottom
import com.example.ui.theme.GlassTop
import com.example.ui.theme.MidnightIndigo
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SlateBorder

/**
 * Matte charcoal vertical gradient matching the design sample surface tone.
 */
fun Modifier.auroraDarkBackdrop(): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(MidnightIndigo, ObsidianBg, AbyssBlack)
        )
    )
}

/**
 * Clean matte dark surface panel with subtle hairline border.
 */
@Composable
fun GradientGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    topColor: Color = GlassTop,
    bottomColor: Color = GlassBottom,
    borderBrush: Brush = Brush.verticalGradient(
        colors = listOf(SlateBorder, SlateBorder.copy(alpha = 0.65f))
    ),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(colors = listOf(topColor, bottomColor)),
                shape = shape
            )
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun GradientActionButton(
    onClick: () -> Unit,
    gradient: Brush,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(999.dp),
    borderColor: Color = SlateBorder,
    content: @Composable RowScope.() -> Unit
) {
    val effectiveAlpha = if (enabled) 1f else 0.45f
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(brush = gradient, alpha = effectiveAlpha)
            .border(1.dp, borderColor.copy(alpha = borderColor.alpha * effectiveAlpha), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}
