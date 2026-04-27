package com.wardcompanion.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightScheme = lightColorScheme(
    primary       = Color(0xFF0F6B5C),  // calm teal — easy on tired eyes
    onPrimary     = Color.White,
    secondary     = Color(0xFF3F5F4F),
    tertiary      = Color(0xFFA75F1E),
    background    = Color(0xFFF7F8F7),
    surface       = Color(0xFFFFFFFF),
    error         = Color(0xFFB3261E),
)

private val DarkScheme = darkColorScheme(
    primary       = Color(0xFF6CD2BB),
    onPrimary     = Color(0xFF003830),
    secondary     = Color(0xFFB3CCBF),
    tertiary      = Color(0xFFFFB778),
    background    = Color(0xFF101413),
    surface       = Color(0xFF1A1F1D),
    error         = Color(0xFFF2B8B5),
)

@Composable
fun WardCompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkScheme
        else      -> LightScheme
    }
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
