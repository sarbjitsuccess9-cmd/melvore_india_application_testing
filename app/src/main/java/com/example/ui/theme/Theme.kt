package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@OptIn(ExperimentalTextApi::class)
val MelvoreBrandFontFamily = FontFamily(
    Font(resId = R.font.jost, weight = FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(resId = R.font.jost, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(resId = R.font.jost, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(resId = R.font.jost, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(resId = R.font.jost, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(resId = R.font.jost, weight = FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800)))
)

@OptIn(ExperimentalTextApi::class)
val MelvoreBodyFontFamily = FontFamily(
    Font(resId = R.font.plus_jakarta_sans, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(resId = R.font.plus_jakarta_sans, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(resId = R.font.plus_jakarta_sans, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(resId = R.font.plus_jakarta_sans, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

@OptIn(ExperimentalTextApi::class)
val MelvoreSerifFontFamily = FontFamily(
    Font(resId = R.font.playfair_display, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(resId = R.font.playfair_display, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(resId = R.font.playfair_display, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

@OptIn(ExperimentalTextApi::class)
val MelvoreFuturisticFontFamily = FontFamily(
    Font(resId = R.font.space_grotesk, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(resId = R.font.space_grotesk, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(resId = R.font.space_grotesk, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

@OptIn(ExperimentalTextApi::class)
val MelvoreGothicFontFamily = FontFamily(
    Font(resId = R.font.cinzel, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(resId = R.font.cinzel, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(resId = R.font.cinzel, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

val MelvoreMinimalSansFontFamily = FontFamily(
    Font(resId = R.font.didact_gothic, weight = FontWeight.Normal)
)

val MelvorePixelFontFamily = FontFamily(
    Font(resId = R.font.silkscreen, weight = FontWeight.Normal),
    Font(resId = R.font.silkscreen, weight = FontWeight.Bold)
)

fun fontFamilyForStyleKey(fontStyle: String): FontFamily = when (fontStyle.lowercase()) {
    "pixel", "y2k_pixel", "silkscreen", "arcade_pixel" -> MelvorePixelFontFamily
    "y2k", "futuristic", "space_grotesk", "street", "streetwear", "grunge" -> MelvoreFuturisticFontFamily
    "gothic", "goth", "cinzel", "dark_luxe" -> MelvoreGothicFontFamily
    "coquette", "editorial_serif", "playfair", "old_money", "quiet_luxury", "party_wear" -> MelvoreSerifFontFamily
    "k_fashion", "k-fashion", "minimal", "clean_sans" -> MelvoreBrandFontFamily
    else -> MelvoreBrandFontFamily
}

/**
 * Art-directed visual personality per Style World so Y2K, Goth, Coquette, Streetwear,
 * K-Fashion, Quiet Luxury, Minimal, Grunge, Old Money, Casual, and Party Wear each feel distinct.
 */
data class StyleWorldVisualSpec(
    val containerColor: Color,
    val titleColor: Color,
    val badgeBgColor: Color,
    val badgeTextColor: Color,
    val borderColor: Color,
    val scrimTopAlpha: Float,
    val scrimBottomColor: Color,
    val fontFamily: FontFamily,
    val shape: Shape,
    val cardWidth: Dp,
    val cardHeight: Dp,
    val vibeLabel: String
)

fun styleWorldVisualSpec(slugOrTheme: String): StyleWorldVisualSpec {
    val key = slugOrTheme.lowercase().replace("-", "_").trim()
    return when {
        key.contains("pixel") -> StyleWorldVisualSpec(
            containerColor = Color(0xFF120B2E),
            titleColor = Color.White,
            badgeBgColor = Color(0xFFFFD23F),
            badgeTextColor = Color(0xFF120B2E),
            borderColor = Color(0xFFFF3D81),
            scrimTopAlpha = 0.14f,
            scrimBottomColor = Color(0xF0120B2E),
            fontFamily = MelvorePixelFontFamily,
            shape = RoundedCornerShape(0.dp),
            cardWidth = 178.dp,
            cardHeight = 236.dp,
            vibeLabel = "PIXEL ARCADE · 8-BIT"
        )
        key.contains("y2k") -> StyleWorldVisualSpec(
            containerColor = Color(0xFF130D24),
            titleColor = Color.White,
            badgeBgColor = Color(0xFFFF3D9A),
            badgeTextColor = Color.White,
            borderColor = Color(0xFF22D3EE),
            scrimTopAlpha = 0.08f,
            scrimBottomColor = Color(0xEB130D24),
            fontFamily = MelvorePixelFontFamily,
            shape = RoundedCornerShape(12.dp),
            cardWidth = 178.dp,
            cardHeight = 236.dp,
            vibeLabel = "CHROME PIXEL · 2000s"
        )
        key.contains("goth") -> StyleWorldVisualSpec(
            containerColor = Color(0xFF070709),
            titleColor = Color(0xFFF4F4F5),
            badgeBgColor = Color(0xFF7F1D1D),
            badgeTextColor = Color(0xFFFECACA),
            borderColor = Color(0xFF991B1B),
            scrimTopAlpha = 0.25f,
            scrimBottomColor = Color(0xF5050507),
            fontFamily = MelvoreGothicFontFamily,
            shape = RoundedCornerShape(2.dp),
            cardWidth = 172.dp,
            cardHeight = 244.dp,
            vibeLabel = "DARK ROMANCE · NOIR"
        )
        key.contains("coquette") -> StyleWorldVisualSpec(
            containerColor = Color(0xFFFFF1F5),
            titleColor = Color(0xFF3F1D2B),
            badgeBgColor = Color(0xFFFCE7F3),
            badgeTextColor = Color(0xFFBE185D),
            borderColor = Color(0xFFF9A8D4),
            scrimTopAlpha = 0.04f,
            scrimBottomColor = Color(0xEEFFF5F8),
            fontFamily = MelvoreSerifFontFamily,
            shape = RoundedCornerShape(22.dp),
            cardWidth = 176.dp,
            cardHeight = 232.dp,
            vibeLabel = "SOFT BLUSH · POETIC"
        )
        key.contains("street") -> StyleWorldVisualSpec(
            containerColor = Color(0xFF111318),
            titleColor = Color.White,
            badgeBgColor = Color(0xFF2563EB),
            badgeTextColor = Color.White,
            borderColor = Color(0xFF3B82F6),
            scrimTopAlpha = 0.12f,
            scrimBottomColor = Color(0xF00B0F19),
            fontFamily = MelvoreFuturisticFontFamily,
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 2.dp, bottomEnd = 14.dp, bottomStart = 2.dp),
            cardWidth = 186.dp,
            cardHeight = 236.dp,
            vibeLabel = "URBAN DROP · BOXY"
        )
        key.contains("k_fashion") || key.contains("korean") || key.contains("seoul") -> StyleWorldVisualSpec(
            containerColor = Color(0xFFF3F5F9),
            titleColor = Color(0xFF0F172A),
            badgeBgColor = Color(0xFF0F172A),
            badgeTextColor = Color.White,
            borderColor = Color(0xFFCBD5E1),
            scrimTopAlpha = 0.03f,
            scrimBottomColor = Color(0xEEF8FAFC),
            fontFamily = MelvoreBrandFontFamily,
            shape = RoundedCornerShape(12.dp),
            cardWidth = 176.dp,
            cardHeight = 236.dp,
            vibeLabel = "SEOUL CLEAN · LAYERED"
        )
        key.contains("quiet") || key.contains("old_money") -> StyleWorldVisualSpec(
            containerColor = Color(0xFFF6F2EA),
            titleColor = Color(0xFF1C1917),
            badgeBgColor = Color(0xFFE7DFD0),
            badgeTextColor = Color(0xFF57472A),
            borderColor = Color(0xFFD6C9B2),
            scrimTopAlpha = 0.04f,
            scrimBottomColor = Color(0xEEF7F3EB),
            fontFamily = MelvoreSerifFontFamily,
            shape = RoundedCornerShape(6.dp),
            cardWidth = 180.dp,
            cardHeight = 240.dp,
            vibeLabel = "ATELIER TAILORING"
        )
        key.contains("grunge") -> StyleWorldVisualSpec(
            containerColor = Color(0xFF181512),
            titleColor = Color(0xFFF5F5F4),
            badgeBgColor = Color(0xFFB45309),
            badgeTextColor = Color.White,
            borderColor = Color(0xFF78350F),
            scrimTopAlpha = 0.2f,
            scrimBottomColor = Color(0xF214110E),
            fontFamily = MelvoreFuturisticFontFamily,
            shape = RoundedCornerShape(4.dp),
            cardWidth = 176.dp,
            cardHeight = 238.dp,
            vibeLabel = "RAW WASH · MOTO"
        )
        key.contains("party") -> StyleWorldVisualSpec(
            containerColor = Color(0xFF0E0A16),
            titleColor = Color.White,
            badgeBgColor = Color(0xFFD4AF37),
            badgeTextColor = Color.Black,
            borderColor = Color(0xFFD4AF37),
            scrimTopAlpha = 0.16f,
            scrimBottomColor = Color(0xF20B0812),
            fontFamily = MelvoreSerifFontFamily,
            shape = RoundedCornerShape(10.dp),
            cardWidth = 180.dp,
            cardHeight = 240.dp,
            vibeLabel = "AFTER HOURS · SHIMMER"
        )
        else -> StyleWorldVisualSpec(
            containerColor = Color(0xFFFAF9F6),
            titleColor = Color(0xFF09090B),
            badgeBgColor = Color(0xFF18181B),
            badgeTextColor = Color.White,
            borderColor = Color(0x24000000),
            scrimTopAlpha = 0.05f,
            scrimBottomColor = Color(0xEEFAF9F6),
            fontFamily = MelvoreBrandFontFamily,
            shape = RoundedCornerShape(10.dp),
            cardWidth = 174.dp,
            cardHeight = 232.dp,
            vibeLabel = "CURATED AESTHETIC"
        )
    }
}

data class MelvoreExtendedColors(
    val isLuxeDark: Boolean,
    val muted: Color,
    val warmIvory: Color,
    val softChampagne: Color,
    val borderSubtle: Color,
    val accentNavy: Color,
    val emerald: Color,
    val amber: Color,
    val crimson: Color,
    val glassSurface: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val darkEditorialBg: Color,
    val chromeSilver: Color,
    val electricBlue: Color,
    val goldAccent: Color,
    val obsidianCard: Color,
    val campaignCobalt: Color,
    val campaignBerry: Color,
    val campaignSun: Color,
    val campaignPlum: Color
)

val LocalMelvoreColors = staticCompositionLocalOf {
    MelvoreExtendedColors(
        isLuxeDark = false,
        muted = Color(0xFF5A5A64),
        warmIvory = Color(0xFFF6F3EE),
        softChampagne = Color(0xFFEBE4D6),
        borderSubtle = Color(0x18000000),
        accentNavy = Color(0xFF141414),
        emerald = Color(0xFF047857),
        amber = Color(0xFFB45309),
        crimson = Color(0xFFB91C1C),
        glassSurface = Color(0xDEFFFFFF),
        glassBorder = Color(0x28000000),
        glassHighlight = Color(0xCCFFFFFF),
        darkEditorialBg = Color(0xFF050507),
        chromeSilver = Color(0xFF6B7280),
        electricBlue = Color(0xFF2563EB),
        goldAccent = Color(0xFFB68B2E),
        obsidianCard = Color(0xFF121216),
        campaignCobalt = Color(0xFF1D4ED8),
        campaignBerry = Color(0xFFB91C1C),
        campaignSun = Color(0xFFB45309),
        campaignPlum = Color(0xFF27272A)
    )
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF09090B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF18181B),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF27272A),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFAF9F6),
    onBackground = Color(0xFF09090B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF09090B),
    surfaceVariant = Color(0xFFF3F1EC),
    onSurfaceVariant = Color(0xFF52525B),
    outline = Color(0x1E000000),
    error = Color(0xFFB91C1C),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF050507),
    primaryContainer = Color(0xFFE4E4E7),
    onPrimaryContainer = Color(0xFF050507),
    secondary = Color(0xFFC8CCD4),
    onSecondary = Color(0xFF050507),
    background = Color(0xFF050507),
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF0D0D10),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF16161C),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0x28FFFFFF),
    error = Color(0xFFEF4444),
    onError = Color.Black
)

val MelvoreTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = MelvoreBrandFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 31.sp,
        lineHeight = 35.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = MelvoreBrandFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = MelvoreBrandFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = MelvoreBrandFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = MelvoreBodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = MelvoreBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = MelvoreBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = MelvoreBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = MelvoreBodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = MelvoreBodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = MelvoreBodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp
    )
)

val MelvoreShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun MelvoreTheme(
    darkTheme: Boolean,
    dynamicTheme: ResolvedFashionWorldTheme? = null,
    content: @Composable () -> Unit
) {
    val manifest = LocalMelvoreDesignManifest.current
    val useDynamicColors = dynamicTheme != null
    val surfaces = buildMelvoreSurfaceColors(
        resolvedTheme = dynamicTheme,
        manifest = manifest,
        darkTheme = darkTheme
    )
    val effectiveDark = surfaces.isDark
    val baseScheme = if (effectiveDark) DarkColorScheme else LightColorScheme
    val finalScheme = if (useDynamicColors && dynamicTheme != null) {
        val bg = surfaces.background
        val surface = surfaces.surface
        val surfaceVar = surfaces.surfaceElevated
        val primary = parseHexColor(dynamicTheme.primaryHex, baseScheme.primary)
        val safePrimary = ensureContrast(primary, bg, minRatio = 3.0f)
        val onPrimary = ensureContrast(contrastOnColor(safePrimary), safePrimary, minRatio = 4.5f)
        val secondary = surfaces.accent
        val onSecondary = surfaces.onAccent
        val onBg = surfaces.onSurface
        val onSurfaceVar = surfaces.onSurfaceMuted
        val border = surfaces.border
        if (effectiveDark) {
            darkColorScheme(
                primary = safePrimary,
                onPrimary = onPrimary,
                primaryContainer = surfaceVar,
                onPrimaryContainer = onBg,
                secondary = secondary,
                onSecondary = onSecondary,
                background = bg,
                onBackground = onBg,
                surface = surface,
                onSurface = onBg,
                surfaceVariant = surfaceVar,
                onSurfaceVariant = onSurfaceVar,
                outline = border,
                error = surfaces.sale,
                onError = ensureContrast(Color(0xFFFFFFFF), surfaces.sale, minRatio = 4.5f)
            )
        } else {
            lightColorScheme(
                primary = safePrimary,
                onPrimary = onPrimary,
                primaryContainer = surfaceVar,
                onPrimaryContainer = onBg,
                secondary = secondary,
                onSecondary = onSecondary,
                background = bg,
                onBackground = onBg,
                surface = surface,
                onSurface = onBg,
                surfaceVariant = surfaceVar,
                onSurfaceVariant = onSurfaceVar,
                outline = border,
                error = surfaces.sale,
                onError = ensureContrast(Color(0xFFFFFFFF), surfaces.sale, minRatio = 4.5f)
            )
        }
    } else {
        baseScheme
    }

    val accentColor = surfaces.accent
    val secondaryAccentColor = if (useDynamicColors && dynamicTheme != null) {
        parseHexColor(dynamicTheme.secondaryAccentHex, if (effectiveDark) Color(0xFFC8CCD4) else Color(0xFFB68B2E))
    } else {
        if (effectiveDark) Color(0xFFC8CCD4) else Color(0xFFB68B2E)
    }

    // Legacy extended colors are now strictly derived from MelvoreSurfaceColors so
    // light screens never accidentally receive #050507 or #121216 surfaces.
    val extended = MelvoreExtendedColors(
        isLuxeDark = surfaces.isLuxeRoute,
        muted = surfaces.onSurfaceMuted,
        warmIvory = surfaces.surfaceElevated,
        softChampagne = if (effectiveDark) surfaces.surfaceElevated else Color(0xFFEBE4D6),
        borderSubtle = surfaces.border,
        accentNavy = finalScheme.primary,
        emerald = surfaces.success,
        amber = if (effectiveDark) Color(0xFFFBBF24) else Color(0xFFB45309),
        crimson = surfaces.sale,
        glassSurface = surfaces.glassTint,
        glassBorder = accentColor.copy(alpha = if (effectiveDark) 0.34f else 0.24f),
        glassHighlight = if (effectiveDark) accentColor.copy(alpha = 0.25f) else surfaces.surface.copy(alpha = 0.96f),
        darkEditorialBg = surfaces.background,
        chromeSilver = secondaryAccentColor,
        electricBlue = accentColor,
        goldAccent = if (effectiveDark) accentColor else secondaryAccentColor,
        obsidianCard = surfaces.surface,
        campaignCobalt = accentColor,
        campaignBerry = surfaces.sale,
        campaignSun = secondaryAccentColor,
        campaignPlum = if (effectiveDark) surfaces.onSurface else finalScheme.primary
    )

    val activeWorldKit = if (dynamicTheme?.isWorldActive == true) {
        manifest.worldOverride(dynamicTheme.worldSlug.orEmpty())?.kit
    } else {
        null
    }
    val radius = (dynamicTheme?.cardRadiusDp ?: manifest.tokens.radii.cardDp).coerceIn(0, 24)
    val dynamicShapes = if (useDynamicColors && dynamicTheme != null) {
        if (radius == 0) {
            Shapes(
                extraSmall = RoundedCornerShape(0.dp),
                small = RoundedCornerShape(0.dp),
                medium = RoundedCornerShape(0.dp),
                large = RoundedCornerShape(0.dp),
                extraLarge = RoundedCornerShape(4.dp)
            )
        } else {
            Shapes(
                extraSmall = RoundedCornerShape((radius / 4).coerceAtLeast(2).dp),
                small = RoundedCornerShape((radius / 2).coerceAtLeast(2).dp),
                medium = RoundedCornerShape(radius.dp),
                large = RoundedCornerShape((radius + 4).dp),
                extraLarge = RoundedCornerShape((radius + 8).dp)
            )
        }
    } else {
        MelvoreShapes
    }

    val kitTypography = activeWorldKit?.typography
    val isPixelTypography = kitTypography?.pixelText == true || dynamicTheme?.pixelText == true
    val headingScale = (kitTypography?.headingScale ?: manifest.tokens.typography.headingScale).coerceIn(0.80f, 1.25f)
    val displayFontKey = kitTypography?.fontRoles?.heading?.takeIf { it.isNotBlank() }
        ?: kitTypography?.displayFont?.takeIf { it.isNotBlank() }
        ?: dynamicTheme?.fontStyle?.takeIf { it.isNotBlank() }
        ?: manifest.tokens.typography.displayFont
    val designKit = LocalDesignKit.current
    val remoteFontUrl = activeWorldKit?.fonts?.takeIf { it.isNotBlank() }?.let { designKit.url(it) }
    val displayFamily = rememberRemoteOrBundledFontFamily(
        remoteFontUrl = remoteFontUrl,
        fallbackStyleKey = displayFontKey
    )
    val bodyFontKey = kitTypography?.fontRoles?.body?.takeIf { it.isNotBlank() }
        ?: kitTypography?.bodyFont?.takeIf { it.isNotBlank() }
        ?: manifest.tokens.typography.bodyFont
    val bodyFamily = when (bodyFontKey.lowercase()) {
        "minimal" -> MelvoreMinimalSansFontFamily
        else -> MelvoreBodyFontFamily
    }
    val labelFamily = if (isPixelTypography) {
        fontFamilyForStyleKey(kitTypography?.fontRoles?.navigation?.ifBlank { "pixel" } ?: "pixel")
    } else {
        bodyFamily
    }
    val dynamicTypography = MelvoreTypography.copy(
        displaySmall = MelvoreTypography.displaySmall.copy(
            fontFamily = displayFamily,
            fontSize = (MelvoreTypography.displaySmall.fontSize.value * headingScale).sp
        ),
        headlineMedium = MelvoreTypography.headlineMedium.copy(
            fontFamily = displayFamily,
            fontSize = (MelvoreTypography.headlineMedium.fontSize.value * headingScale).sp
        ),
        headlineSmall = MelvoreTypography.headlineSmall.copy(
            fontFamily = displayFamily,
            fontSize = (MelvoreTypography.headlineSmall.fontSize.value * headingScale).sp
        ),
        titleLarge = MelvoreTypography.titleLarge.copy(
            fontFamily = displayFamily,
            fontSize = (MelvoreTypography.titleLarge.fontSize.value * if (isPixelTypography) 0.90f else 1f).sp
        ),
        bodyLarge = MelvoreTypography.bodyLarge.copy(fontFamily = bodyFamily),
        bodyMedium = MelvoreTypography.bodyMedium.copy(fontFamily = bodyFamily),
        bodySmall = MelvoreTypography.bodySmall.copy(fontFamily = bodyFamily),
        labelLarge = MelvoreTypography.labelLarge.copy(
            fontFamily = labelFamily,
            fontSize = if (isPixelTypography) 11.sp else MelvoreTypography.labelLarge.fontSize
        ),
        labelMedium = MelvoreTypography.labelMedium.copy(
            fontFamily = labelFamily,
            fontSize = if (isPixelTypography) 10.sp else MelvoreTypography.labelMedium.fontSize
        ),
        labelSmall = MelvoreTypography.labelSmall.copy(
            fontFamily = labelFamily,
            fontSize = if (isPixelTypography) 9.sp else MelvoreTypography.labelSmall.fontSize
        )
    )

    CompositionLocalProvider(
        LocalMelvoreSurfaces provides surfaces,
        LocalMelvoreColors provides extended
    ) {
        MaterialTheme(
            colorScheme = finalScheme,
            typography = dynamicTypography,
            shapes = dynamicShapes,
            content = content
        )
    }
}

