package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

fun createScaledTypography(scaleFactor: Float): Typography {
    fun TextUnit.scale(): TextUnit = (this.value * scaleFactor).sp

    return Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp.scale(),
            lineHeight = 40.sp.scale(),
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp.scale(),
            lineHeight = 34.sp.scale(),
            letterSpacing = (-0.25).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp.scale(),
            lineHeight = 32.sp.scale()
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp.scale(),
            lineHeight = 28.sp.scale()
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp.scale(),
            lineHeight = 26.sp.scale()
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp.scale(),
            lineHeight = 24.sp.scale()
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp.scale(),
            lineHeight = 25.sp.scale(),
            letterSpacing = 0.2.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 14.5.sp.scale(),
            lineHeight = 22.sp.scale(),
            letterSpacing = 0.2.sp
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp.scale(),
            lineHeight = 20.sp.scale()
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp.scale(),
            lineHeight = 18.sp.scale()
        )
    )
}

val DefaultTypography = createScaledTypography(1.0f)
