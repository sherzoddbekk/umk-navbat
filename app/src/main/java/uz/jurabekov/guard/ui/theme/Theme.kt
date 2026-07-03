package uz.jurabekov.guard.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Primary600,
    onPrimary = Neutral0,
    primaryContainer = Primary100,
    onPrimaryContainer = Primary800,

    secondary = Accent500,
    onSecondary = Neutral0,
    secondaryContainer = Accent100,
    onSecondaryContainer = Accent600,

    tertiary = Success500,
    onTertiary = Neutral0,

    background = Neutral50,
    onBackground = Neutral900,

    surface = Neutral0,
    onSurface = Neutral900,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral500,

    outline = Neutral200,
    outlineVariant = Neutral300,

    error = Error500,
    onError = Neutral0,
    errorContainer = Error100,
    onErrorContainer = Error500
)

private val DarkColors = darkColorScheme(
    primary = PrimaryGradTop,
    onPrimary = Neutral0,
    primaryContainer = Primary800,
    onPrimaryContainer = Primary100,

    secondary = Accent500,
    onSecondary = Neutral900,
    secondaryContainer = Accent600,
    onSecondaryContainer = Accent100,

    background = DarkBackground,
    onBackground = DarkOnSurface,

    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkElevated,
    onSurfaceVariant = DarkOnSurfaceMuted,

    outline = DarkElevated,

    error = Error500,
    onError = Neutral0
)

@Composable
fun GuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Brand ranglarini saqlash uchun dynamicColor o'chirilgan.
    // Agar Material You'ni xohlasangiz: dynamicColor = true
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
