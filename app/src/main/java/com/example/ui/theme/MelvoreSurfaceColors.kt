package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.example.model.DesignManifest
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Single semantic color surface layer for every Melvore screen and component.
 * No UI composable outside the allow-listed theme files may hardcode raw dark hex
 * literals or `Color.Black`/`Color.White`; every surface, text, border, scrim, and
 * glass tint comes from [LocalMelvoreSurfaces].
 */
@Immutable
data class MelvoreSurfaceColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val border: Color,
    val scrim: Color,
    val imageOverlay: Color,
    val onImageOverlay: Color,
    val glassTint: Color,
    val accent: Color,
    val onAccent: Color,
    val success: Color,
    val sale: Color,
    val isDark: Boolean = false,
    val isLuxeRoute: Boolean = false
)

val LocalMelvoreSurfaces = staticCompositionLocalOf {
    buildDefaultLightSurfaceColors()
}

private val INK_DARK = Color(0xFF09090B)
private val INK_LIGHT = Color(0xFFFAFAFA)
private val PAPER_LIGHT_BG = Color(0xFFFAF9F6)
private val PAPER_LIGHT_SURFACE = Color(0xFFFFFFFF)
private val PAPER_LIGHT_ELEVATED = Color(0xFFF3F1EC)
private val DARK_CANVAS_BG = Color(0xFF0B0B10)
private val DARK_CANVAS_SURFACE = Color(0xFF14141C)
private val DARK_CANVAS_ELEVATED = Color(0xFF1D1D28)

fun buildDefaultLightSurfaceColors(): MelvoreSurfaceColors = MelvoreSurfaceColors(
    background = PAPER_LIGHT_BG,
    surface = PAPER_LIGHT_SURFACE,
    surfaceElevated = PAPER_LIGHT_ELEVATED,
    onSurface = INK_DARK,
    onSurfaceMuted = Color(0xFF52525B),
    border = Color(0xFFE4E4E7),
    scrim = Color(0x5209090B),
    imageOverlay = Color(0xA809090B),
    onImageOverlay = Color(0xFFFFFFFF),
    glassTint = Color(0xD9FFFFFF),
    accent = Color(0xFF1D4ED8),
    onAccent = Color(0xFFFFFFFF),
    success = Color(0xFF15803D),
    sale = Color(0xFFB91C1C),
    isDark = false,
    isLuxeRoute = false
)

/**
 * WCAG 2.1 contrast ratio between [foreground] and [background].
 * Composites translucent foregrounds over [background] before calculating luminance.
 */
fun contrastRatio(foreground: Color, background: Color): Float {
    val opaqueBg = if (background.alpha < 1f) background.compositeOver(PAPER_LIGHT_SURFACE) else background
    val opaqueFg = if (foreground.alpha < 1f) foreground.compositeOver(opaqueBg) else foreground
    val l1 = opaqueFg.luminance()
    val l2 = opaqueBg.luminance()
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}

/**
 * Ensures [foreground] has at least [minRatio] WCAG contrast against [background]
 * (default 4.5:1 for normal text). Preserves hue/tone when possible by shifting
 * luminance toward high-contrast dark or light ink before falling back to pure ink.
 */
fun ensureContrast(
    foreground: Color,
    background: Color,
    minRatio: Float = 4.5f
): Color {
    if (contrastRatio(foreground, background) >= minRatio) {
        return foreground
    }
    val opaqueBg = if (background.alpha < 1f) background.compositeOver(PAPER_LIGHT_SURFACE) else background
    val darkContrast = contrastRatio(INK_DARK, opaqueBg)
    val lightContrast = contrastRatio(INK_LIGHT, opaqueBg)
    val targetInk = if (darkContrast >= lightContrast) INK_DARK else INK_LIGHT

    val opaqueFg = if (foreground.alpha < 1f) foreground.compositeOver(opaqueBg) else foreground
    // Progressive hue-preserving blend toward target ink
    for (step in 1..10) {
        val fraction = step / 10f
        val candidate = lerp(opaqueFg, targetInk, fraction)
        if (contrastRatio(candidate, opaqueBg) >= minRatio) {
            return candidate
        }
    }
    return targetInk
}

/**
 * Derives a light-mode canvas color (>= [minLuminance], default 94%) tinted subtly with
 * the world's accent so dark worlds (Goth, Grunge, Y2K, Party) stay crisp and cohesive
 * on Home / Shop / Bag / Discover in light mode without producing "mixed black" patches.
 */
fun deriveLightWorldBackground(
    accent: Color,
    basePaper: Color = PAPER_LIGHT_BG,
    minLuminance: Float = 0.94f
): Color {
    var tinted = lerp(basePaper, accent.copy(alpha = 1f), 0.035f)
    if (tinted.luminance() >= minLuminance) return tinted
    for (step in 1..10) {
        val lifted = lerp(tinted, Color.White, step / 10f)
        if (lifted.luminance() >= minLuminance) return lifted
    }
    return PAPER_LIGHT_SURFACE
}

/**
 * Light-mode surface guard: prevents any non-media surface in light mode from dropping
 * below [minLuminance] (default 12% per spec, lifted to clean light surface).
 */
fun enforceLightModeSurfaceFloor(
    color: Color,
    fallbackLight: Color = PAPER_LIGHT_SURFACE,
    minLuminance: Float = 0.12f
): Color {
    val opaque = color.copy(alpha = 1f)
    return if (opaque.luminance() < minLuminance) fallbackLight else opaque
}

fun Color.toHexRgbString(): String {
    val r = (red * 255f).roundToInt().coerceIn(0, 255)
    val g = (green * 255f).roundToInt().coerceIn(0, 255)
    val b = (blue * 255f).roundToInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}

/**
 * Builds [MelvoreSurfaceColors] from the resolved theme + manifest tokens + Luxe flag,
 * applying both the light-mode luminance floor (>= 12% for all surfaces) and WCAG 4.5:1
 * contrast protection on every foreground/background pair.
 */
fun buildMelvoreSurfaceColors(
    resolvedTheme: ResolvedFashionWorldTheme?,
    manifest: DesignManifest,
    darkTheme: Boolean
): MelvoreSurfaceColors {
    val isLuxe = resolvedTheme?.isLuxeIsolated == true
    val effectiveDark = darkTheme || isLuxe || (resolvedTheme?.isDark == true)
    val manifestColors = manifest.tokens.colors
    val darkManifest = manifestColors.dark

    val rawBg = parseHexColor(
        resolvedTheme?.backgroundHex,
        if (effectiveDark) parseHexColor(darkManifest?.background, DARK_CANVAS_BG)
        else parseHexColor(manifestColors.background, PAPER_LIGHT_BG)
    )
    val rawSurface = parseHexColor(
        resolvedTheme?.surfaceHex,
        if (effectiveDark) parseHexColor(darkManifest?.surface, DARK_CANVAS_SURFACE)
        else parseHexColor(manifestColors.surface, PAPER_LIGHT_SURFACE)
    )
    val rawSurfaceElevated = parseHexColor(
        resolvedTheme?.surfaceElevatedHex,
        if (effectiveDark) parseHexColor(darkManifest?.surfaceElevated, DARK_CANVAS_ELEVATED)
        else parseHexColor(manifestColors.surfaceElevated, PAPER_LIGHT_ELEVATED)
    )
    val rawAccent = parseHexColor(
        resolvedTheme?.accentHex,
        if (effectiveDark) parseHexColor(darkManifest?.accent, Color(0xFFD4AF37))
        else parseHexColor(manifestColors.accent, Color(0xFF1D4ED8))
    )
    val rawOnSurface = parseHexColor(
        resolvedTheme?.textPrimaryHex,
        if (effectiveDark) INK_LIGHT else INK_DARK
    )
    val rawOnSurfaceMuted = parseHexColor(
        resolvedTheme?.textSecondaryHex,
        if (effectiveDark) Color(0xFFA1A1AA) else Color(0xFF52525B)
    )
    val rawBorder = parseHexColor(
        resolvedTheme?.borderHex,
        if (effectiveDark) Color(0xFF272732) else Color(0xFFE4E4E7)
    )
    val rawSuccess = parseHexColor(
        if (effectiveDark) darkManifest?.successAccent else manifestColors.successAccent,
        if (effectiveDark) Color(0xFF34D399) else Color(0xFF15803D)
    )
    val rawSale = parseHexColor(
        if (effectiveDark) darkManifest?.saleAccent else manifestColors.saleAccent,
        if (effectiveDark) Color(0xFFF87171) else Color(0xFFB91C1C)
    )

    // In light mode, enforce the surface luminance guard so no dark surface ever leaks in.
    val bg = if (!effectiveDark) {
        if (rawBg.luminance() < 0.70f) deriveLightWorldBackground(rawAccent, PAPER_LIGHT_BG)
        else enforceLightModeSurfaceFloor(rawBg, PAPER_LIGHT_BG)
    } else {
        rawBg
    }
    val surface = if (!effectiveDark) {
        if (rawSurface.luminance() < 0.70f) PAPER_LIGHT_SURFACE
        else enforceLightModeSurfaceFloor(rawSurface, PAPER_LIGHT_SURFACE)
    } else {
        rawSurface
    }
    val surfaceElevated = if (!effectiveDark) {
        if (rawSurfaceElevated.luminance() < 0.65f) lerp(PAPER_LIGHT_ELEVATED, rawAccent, 0.04f)
        else enforceLightModeSurfaceFloor(rawSurfaceElevated, PAPER_LIGHT_ELEVATED)
    } else {
        rawSurfaceElevated
    }

    val onSurface = ensureContrast(rawOnSurface, surface, minRatio = 4.5f)
    val onSurfaceMuted = ensureContrast(rawOnSurfaceMuted, surface, minRatio = 4.5f)
    val onAccent = ensureContrast(
        foreground = if (rawAccent.luminance() < 0.42f) INK_LIGHT else INK_DARK,
        background = rawAccent,
        minRatio = 4.5f
    )
    val border = if (!effectiveDark) {
        if (rawBorder.luminance() < 0.12f) Color(0x2209090B) else rawBorder
    } else {
        rawBorder
    }
    val scrim = if (effectiveDark) Color(0x88050507) else Color(0x4709090B)
    val imageOverlay = Color(0xB309090B)
    val onImageOverlay = Color(0xFFFFFFFF)
    val glassTint = if (effectiveDark) {
        lerp(surface, rawAccent, 0.06f).copy(alpha = 0.78f)
    } else {
        lerp(PAPER_LIGHT_SURFACE, bg, 0.25f).copy(alpha = 0.82f)
    }

    return MelvoreSurfaceColors(
        background = bg,
        surface = surface,
        surfaceElevated = surfaceElevated,
        onSurface = onSurface,
        onSurfaceMuted = onSurfaceMuted,
        border = border,
        scrim = scrim,
        imageOverlay = imageOverlay,
        onImageOverlay = onImageOverlay,
        glassTint = glassTint,
        accent = rawAccent,
        onAccent = onAccent,
        success = rawSuccess,
        sale = rawSale,
        isDark = effectiveDark,
        isLuxeRoute = isLuxe
    )
}
