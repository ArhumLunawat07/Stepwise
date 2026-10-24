package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalHighContrast = staticCompositionLocalOf { false }
val LocalTextScale = staticCompositionLocalOf { 1.0f }

private val StandardLightColorScheme = lightColorScheme(
    primary = StepwiseTealPrimary,
    onPrimary = Color.White,
    primaryContainer = StepwiseTealContainer,
    onPrimaryContainer = StepwiseOnTealContainer,
    secondary = StepwiseTealDark,
    onSecondary = Color.White,
    background = StepwiseOatmealBg,
    onBackground = StepwiseTextPrimary,
    surface = StepwiseSurfaceWarm,
    onSurface = StepwiseTextPrimary,
    surfaceVariant = StepwiseSurfaceVariant,
    onSurfaceVariant = StepwiseTextSecondary,
    outline = StepwiseBorder,
    outlineVariant = Color(0xFFD3C9BD)
)

private val HighContrastColorScheme = lightColorScheme(
    primary = HighContrastPrimary,
    onPrimary = HighContrastOnPrimary,
    primaryContainer = Color(0xFFDCE5FF),
    onPrimaryContainer = Color(0xFF001550),
    secondary = Color(0xFF000000),
    onSecondary = Color.White,
    background = HighContrastBg,
    onBackground = HighContrastTextPrimary,
    surface = HighContrastSurface,
    onSurface = HighContrastTextPrimary,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = HighContrastTextPrimary,
    outline = HighContrastBorder,
    outlineVariant = Color(0xFF333333)
)

private val StandardDarkColorScheme = darkColorScheme(
    primary = Color(0xFF4DB6AC),
    onPrimary = Color(0xFF003737),
    primaryContainer = Color(0xFF004F4F),
    onPrimaryContainer = Color(0xFF80CBC4),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF003737),
    background = Color(0xFF14191E),
    onBackground = Color(0xFFE6E9EE),
    surface = Color(0xFF1B2228),
    onSurface = Color(0xFFE6E9EE),
    surfaceVariant = Color(0xFF263038),
    onSurfaceVariant = Color(0xFFCBD2DA),
    outline = Color(0xFF3D4B56)
)

@Composable
fun StepwiseTheme(
    isHighContrast: Boolean = false,
    textScale: Float = 1.0f,
    reducedMotion: Boolean = false,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isHighContrast -> HighContrastColorScheme
        darkTheme -> StandardDarkColorScheme
        else -> StandardLightColorScheme
    }

    val typography = createScaledTypography(textScale)

    CompositionLocalProvider(
        LocalReducedMotion provides reducedMotion,
        LocalHighContrast provides isHighContrast,
        LocalTextScale provides textScale
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
