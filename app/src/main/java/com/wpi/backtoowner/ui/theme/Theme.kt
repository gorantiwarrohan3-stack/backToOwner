package com.wpi.backtoowner.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightWpiColorScheme = lightColorScheme(
    primary = WpiCrimson,
    onPrimary = WpiOnCrimson,
    primaryContainer = WpiCrimsonContainer,
    onPrimaryContainer = WpiOnCrimsonContainer,
    secondary = WpiGrey,
    onSecondary = Color.White,
    secondaryContainer = WpiGreyContainer,
    onSecondaryContainer = WpiOnGreyContainer,
    tertiary = WpiGrey,
    onTertiary = Color.White,
    background = WpiBackground,
    onBackground = WpiOnGrey,
    surface = WpiSurface,
    onSurface = WpiOnGrey,
    surfaceVariant = WpiSurfaceVariant,
    onSurfaceVariant = WpiOnGreyContainer,
    outline = WpiGrey,
)

/** Dark theme: maroon accent, black surfaces, white/grey text (no pink primaries). */
private val DarkWpiColorScheme = darkColorScheme(
    primary = WpiCrimson,
    onPrimary = Color.White,
    primaryContainer = WpiHeaderMaroon,
    onPrimaryContainer = Color.White,
    secondary = WpiGrey,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2C2C2C),
    onSecondaryContainer = Color(0xFFE8E8E8),
    tertiary = WpiGrey,
    onTertiary = Color.White,
    background = WpiBlack,
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFB8B8B8),
    outline = WpiGrey,
)

@Composable
fun BackToOwnerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkWpiColorScheme
        else -> LightWpiColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
