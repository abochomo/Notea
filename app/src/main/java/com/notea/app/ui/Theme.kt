package com.notea.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.notea.app.data.AppSettings

data class Palette(val key: String, val name: String, val seed: Color)

/** Paletas disponibles. «dynamic» usa los colores del fondo de pantalla (Android 12+). */
val Palettes: List<Palette> = listOf(
    Palette("indigo", "Índigo", Color(0xFF4F5BD5)),
    Palette("ocean", "Océano", Color(0xFF0E7C9B)),
    Palette("emerald", "Esmeralda", Color(0xFF1E8E5A)),
    Palette("sunset", "Atardecer", Color(0xFFD9622B)),
    Palette("rose", "Rosa", Color(0xFFC2417B)),
    Palette("lavender", "Lavanda", Color(0xFF7A52C7)),
    Palette("graphite", "Grafito", Color(0xFF5A6470)),
    Palette("dynamic", "Dinámico", Color(0xFF6750A4)),
)

val dynamicColorAvailable: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

val LocalDark = staticCompositionLocalOf { false }

/** Colores fijos por tipo de elemento, iguales en todas las paletas para reconocerlos de un vistazo. */
data class TypeColors(val note: Color, val reminder: Color, val routine: Color)

private val LightTypeColors = TypeColors(Color(0xFF2F7BEA), Color(0xFFE08A00), Color(0xFF8B4FE8))
private val DarkTypeColors = TypeColors(Color(0xFF6AA8FF), Color(0xFFFFB84D), Color(0xFFB794FF))

val typeColors: TypeColors
    @Composable get() = if (LocalDark.current) DarkTypeColors else LightTypeColors

private fun hsl(h: Float, s: Float, l: Float): Color {
    val hue = ((h % 360f) + 360f) % 360f
    return Color(ColorUtils.HSLToColor(floatArrayOf(hue, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))))
}

/** Genera un esquema Material 3 completo a partir de un color semilla. */
fun schemeFromSeed(seed: Color, dark: Boolean): ColorScheme {
    val out = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), out)
    val h = out[0]
    val s = out[1].coerceAtMost(0.85f)
    val s2 = s * 0.45f
    val h3 = h + 55f
    val s3 = s * 0.7f
    val ns = (s * 0.22f).coerceAtMost(0.16f)

    return if (!dark) {
        lightColorScheme(
            primary = hsl(h, s, 0.42f),
            onPrimary = Color.White,
            primaryContainer = hsl(h, s, 0.90f),
            onPrimaryContainer = hsl(h, s, 0.14f),
            inversePrimary = hsl(h, s, 0.80f),
            secondary = hsl(h, s2, 0.40f),
            onSecondary = Color.White,
            secondaryContainer = hsl(h, s2, 0.89f),
            onSecondaryContainer = hsl(h, s2, 0.14f),
            tertiary = hsl(h3, s3, 0.40f),
            onTertiary = Color.White,
            tertiaryContainer = hsl(h3, s3, 0.90f),
            onTertiaryContainer = hsl(h3, s3, 0.14f),
            background = hsl(h, ns, 0.985f),
            onBackground = hsl(h, ns, 0.10f),
            surface = hsl(h, ns, 0.985f),
            onSurface = hsl(h, ns, 0.10f),
            surfaceVariant = hsl(h, ns, 0.90f),
            onSurfaceVariant = hsl(h, ns * 0.7f, 0.30f),
            surfaceTint = hsl(h, s, 0.42f),
            inverseSurface = hsl(h, ns, 0.19f),
            inverseOnSurface = hsl(h, ns, 0.95f),
            outline = hsl(h, ns * 0.6f, 0.47f),
            outlineVariant = hsl(h, ns, 0.80f),
            surfaceBright = hsl(h, ns, 0.985f),
            surfaceDim = hsl(h, ns, 0.87f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = hsl(h, ns, 0.965f),
            surfaceContainer = hsl(h, ns, 0.945f),
            surfaceContainerHigh = hsl(h, ns, 0.925f),
            surfaceContainerHighest = hsl(h, ns, 0.90f),
        )
    } else {
        darkColorScheme(
            primary = hsl(h, s, 0.78f),
            onPrimary = hsl(h, s, 0.16f),
            primaryContainer = hsl(h, s, 0.30f),
            onPrimaryContainer = hsl(h, s, 0.92f),
            inversePrimary = hsl(h, s, 0.42f),
            secondary = hsl(h, s2, 0.78f),
            onSecondary = hsl(h, s2, 0.16f),
            secondaryContainer = hsl(h, s2, 0.27f),
            onSecondaryContainer = hsl(h, s2, 0.90f),
            tertiary = hsl(h3, s3, 0.78f),
            onTertiary = hsl(h3, s3, 0.16f),
            tertiaryContainer = hsl(h3, s3, 0.28f),
            onTertiaryContainer = hsl(h3, s3, 0.90f),
            background = hsl(h, ns, 0.075f),
            onBackground = hsl(h, ns, 0.90f),
            surface = hsl(h, ns, 0.075f),
            onSurface = hsl(h, ns, 0.90f),
            surfaceVariant = hsl(h, ns, 0.27f),
            onSurfaceVariant = hsl(h, ns, 0.78f),
            surfaceTint = hsl(h, s, 0.78f),
            inverseSurface = hsl(h, ns, 0.90f),
            inverseOnSurface = hsl(h, ns, 0.18f),
            outline = hsl(h, ns * 0.6f, 0.57f),
            outlineVariant = hsl(h, ns, 0.28f),
            surfaceBright = hsl(h, ns, 0.22f),
            surfaceDim = hsl(h, ns, 0.075f),
            surfaceContainerLowest = hsl(h, ns, 0.05f),
            surfaceContainerLow = hsl(h, ns, 0.105f),
            surfaceContainer = hsl(h, ns, 0.125f),
            surfaceContainerHigh = hsl(h, ns, 0.165f),
            surfaceContainerHighest = hsl(h, ns, 0.205f),
        )
    }
}

private fun ColorScheme.amoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
)

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun NoteaTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val ctx = LocalContext.current
    val palette = Palettes.firstOrNull { it.key == settings.palette } ?: Palettes.first()
    var scheme = if (palette.key == "dynamic" && dynamicColorAvailable) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        schemeFromSeed(palette.seed, dark)
    }
    if (dark && settings.amoled) scheme = scheme.amoled()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalDark provides dark) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
