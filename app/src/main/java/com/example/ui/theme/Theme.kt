package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val GuardDarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = AbyssBlack,
    primaryContainer = SlateCardElevated,
    onPrimaryContainer = TextPrimary,
    secondary = PureWhite,
    onSecondary = AbyssBlack,
    secondaryContainer = SlateCardElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = TextSecondary,
    onTertiary = AbyssBlack,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = SlateCard,
    onSurface = TextPrimary,
    surfaceVariant = SlateCardElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SlateCard,
    surfaceContainerHigh = SlateCardElevated,
    outline = SlateBorder,
    outlineVariant = SlateBorder,
    error = RoseDanger,
    onError = AbyssBlack,
    errorContainer = RoseContainer,
    onErrorContainer = TextPrimary
)

val GuardShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LpbGuardTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = GuardDarkColorScheme,
        typography = Typography,
        shapes = GuardShapes,
        content = content
    )
}
