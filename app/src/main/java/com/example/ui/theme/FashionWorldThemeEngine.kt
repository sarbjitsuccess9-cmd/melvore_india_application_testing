package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.BuildConfig
import com.example.model.CampaignItem
import com.example.model.DesignKit
import com.example.model.DesignManifest
import com.example.model.FashionWorldContext
import com.example.model.FashionWorldTheme
import com.example.model.OccasionTheme
import com.example.model.RunwayConfig
import com.example.model.SmartOffer
import com.example.model.ThemeTokenSpec
import com.example.model.WorldKit
import java.util.Locale

@Immutable
data class ResolvedFashionWorldTheme(
    val sourceLayer: String = "default",
    val themeKey: String = "melvore_light",
    val worldSlug: String? = null,
    val worldName: String = "",
    val occasionSlug: String? = null,
    val occasionName: String = "",
    val occasionButtonStyle: String = "",
    val isDark: Boolean = false,
    val backgroundHex: String = "#FAF9F6",
    val surfaceHex: String = "#FFFFFF",
    val surfaceElevatedHex: String = "#F3F1EC",
    val primaryHex: String = "#09090B",
    val accentHex: String = "#1D4ED8",
    val secondaryAccentHex: String = "#B68B2E",
    val textPrimaryHex: String = "#09090B",
    val textSecondaryHex: String = "#52525B",
    val borderHex: String = "#E4E4E7",
    val fontStyle: String = "editorial_serif",
    val buttonStyle: String = "sharp",
    val cardRadiusDp: Int = 10,
    val borderWidthDp: Float = 1f,
    val ambientPreset: String = "none",
    val audioTrackKey: String = "none",
    val audioUrl: String? = null,
    val audioLoop: Boolean = true,
    val audioVolume: Float = 0.45f,
    val promoVideoUrl: String? = null,
    val promoVideoPosterUrl: String? = null,
    val promoVideoIsSponsored: Boolean = false,
    val runwayConfig: RunwayConfig? = null,
    val hasSubmittedFeedback: Boolean = false,
    val sfxProfile: String = "soft",
    val animationKey: String = "smooth_reveal",
    val motifKey: String = "none",
    val motionIntensity: Float = 0.65f,
    val motionPreset: String = "",
    val pixelText: Boolean = false,
    val isWorldActive: Boolean = false,
    val isScrollPreview: Boolean = false,
    val isLuxeIsolated: Boolean = false
) {
    val resolvedActionButtonStyle: String
        get() = resolveWorldButtonStyle(
            worldButtonStyle = buttonStyle,
            worldSlug = worldSlug,
            occasionButtonStyle = occasionButtonStyle,
            occasionSlug = occasionSlug,
            isWorldActive = isWorldActive
        )
}

fun resolveWorldButtonStyle(
    worldButtonStyle: String?,
    worldSlug: String?,
    occasionButtonStyle: String?,
    occasionSlug: String?,
    isWorldActive: Boolean = !worldSlug.isNullOrBlank()
): String {
    val occBtn = occasionButtonStyle?.trim()?.lowercase(Locale.ROOT).orEmpty()
    val wBtn = worldButtonStyle?.trim()?.lowercase(Locale.ROOT).orEmpty()
    val wSlug = worldSlug?.trim()?.lowercase(Locale.ROOT).orEmpty()
    val oSlug = occasionSlug?.trim()?.lowercase(Locale.ROOT).orEmpty()

    if (isWorldActive) {
        if (wBtn.isNotBlank() && wBtn != "sharp" && wBtn != "editorial_pill" && wBtn != "pill") {
            return wBtn
        }
        return when {
            wSlug.contains("pixel") -> "pixel_block"
            wSlug.contains("y2k") -> "y2k_pixel_chrome"
            wSlug.contains("goth") -> "goth_fang"
            wSlug.contains("coquette") -> "coquette_ribbon"
            wSlug.contains("street") -> "streetwear_utility"
            wSlug.contains("k-fashion") || wSlug.contains("k_fashion") || wSlug.contains("seoul") -> "kfashion_minimal"
            wBtn.isNotBlank() -> wBtn
            else -> "default_minimal"
        }
    }

    if (occBtn.isNotBlank()) return occBtn
    if (oSlug.contains("halloween")) return "halloween_pumpkin"
    if (oSlug.contains("diwali")) return "diwali_gold"
    return when {
        wBtn.isNotBlank() -> wBtn
        else -> "default_minimal"
    }
}

val LocalFashionWorldTheme = staticCompositionLocalOf { ResolvedFashionWorldTheme() }
val LocalMelvoreDesignManifest = staticCompositionLocalOf { DesignManifest() }
val LocalMelvoreApiBaseUrl = staticCompositionLocalOf { BuildConfig.MELVORE_API_BASE }
val LocalDesignKit = staticCompositionLocalOf { DesignKit() }
val LocalWorld = staticCompositionLocalOf<WorldKit?> { null }

fun parseHexColor(hex: String?, fallback: Color): Color {
    val clean = hex?.trim()?.removePrefix("#").orEmpty()
    if (clean.isEmpty()) return fallback
    return when (clean.length) {
        6 -> {
            val rgb = clean.toLongOrNull(16) ?: return fallback
            Color(0xFF000000L or rgb)
        }
        8 -> {
            val argb = clean.toLongOrNull(16) ?: return fallback
            Color(argb)
        }
        else -> fallback
    }
}

fun contrastOnColor(background: Color): Color {
    return if (background.luminance() < 0.42f) Color(0xFFFAFAFA) else Color(0xFF09090B)
}

object FashionWorldThemeResolver {

    val LUXE_ISOLATED_THEME = ResolvedFashionWorldTheme(
        sourceLayer = "luxe_isolated",
        themeKey = "luxe_obsidian_gold",
        worldSlug = null,
        worldName = "MELVORE LUXE",
        isDark = true,
        backgroundHex = "#050507",
        surfaceHex = "#0D0D10",
        surfaceElevatedHex = "#16161C",
        primaryHex = "#FFFFFF",
        accentHex = "#D4AF37",
        secondaryAccentHex = "#C8CCD4",
        textPrimaryHex = "#FAFAFA",
        textSecondaryHex = "#A1A1AA",
        borderHex = "#33FFFFFF",
        fontStyle = "editorial_serif",
        buttonStyle = "sharp",
        cardRadiusDp = 6,
        borderWidthDp = 1f,
        ambientPreset = "none",
        audioTrackKey = "none",
        sfxProfile = "crystal",
        animationKey = "metallic_shine",
        motifKey = "atelier_frame",
        motionIntensity = 0.55f,
        isWorldActive = false,
        isScrollPreview = false,
        isLuxeIsolated = true
    )

    fun resolve(
        defaultTheme: ThemeTokenSpec = ThemeTokenSpec(),
        activeOccasion: OccasionTheme? = null,
        selectedWorld: FashionWorldTheme? = null,
        scrollPreviewWorld: FashionWorldTheme? = null,
        activeCampaign: CampaignItem? = null,
        isLuxeRoute: Boolean = false,
        isNeutralCheckoutOrAuthRoute: Boolean = false
    ): ResolvedFashionWorldTheme {
        // Strict isolation: Luxe route never leaks playful world styling; non-Luxe routes never leak Luxe obsidian.
        if (isLuxeRoute) {
            return LUXE_ISOLATED_THEME
        }

        // Baseline Default Melvore Layer
        var resolved = ResolvedFashionWorldTheme(
            sourceLayer = "default",
            themeKey = defaultTheme.themeKey.ifBlank { "melvore_light" },
            isDark = defaultTheme.mode.equals("dark", ignoreCase = true),
            backgroundHex = defaultTheme.backgroundColor.ifBlank { "#FAF9F6" },
            surfaceHex = defaultTheme.surfaceColor.ifBlank { "#FFFFFF" },
            surfaceElevatedHex = defaultTheme.surfaceElevated.ifBlank { "#F3F1EC" },
            primaryHex = defaultTheme.primaryColor.ifBlank { "#09090B" },
            accentHex = defaultTheme.accentColor.ifBlank { "#1D4ED8" },
            secondaryAccentHex = defaultTheme.secondaryAccent.ifBlank { "#B68B2E" },
            textPrimaryHex = defaultTheme.textPrimary.ifBlank { "#09090B" },
            textSecondaryHex = defaultTheme.textSecondary.ifBlank { "#52525B" },
            borderHex = defaultTheme.borderColor.ifBlank { "#E4E4E7" },
            fontStyle = defaultTheme.fontStyle.ifBlank { "editorial_serif" },
            buttonStyle = defaultTheme.buttonStyle.ifBlank { "sharp" },
            cardRadiusDp = defaultTheme.cardRadiusDp.coerceIn(2, 28),
            borderWidthDp = defaultTheme.borderWidthDp.coerceIn(0.5f, 3f),
            ambientPreset = defaultTheme.ambientPreset,
            audioTrackKey = defaultTheme.audioTrackKey,
            motionIntensity = defaultTheme.motionIntensity.coerceIn(0f, 1f),
            isWorldActive = false,
            isScrollPreview = false,
            isLuxeIsolated = false
        )

        // Checkout & Auth keep clean readable light editorial mode for trust & contrast.
        if (isNeutralCheckoutOrAuthRoute) {
            return resolved
        }

        // Layer 2: Active Occasion / Festival overlay on non-world browsing
        if (activeOccasion != null && selectedWorld == null) {
            resolved = resolved.copy(
                sourceLayer = "occasion",
                occasionSlug = activeOccasion.slug,
                occasionName = activeOccasion.name,
                occasionButtonStyle = activeOccasion.buttonStyle,
                accentHex = activeOccasion.accentColor.ifBlank { resolved.accentHex },
                secondaryAccentHex = activeOccasion.primaryColor.ifBlank { resolved.secondaryAccentHex },
                fontStyle = activeOccasion.fontStyle.ifBlank { resolved.fontStyle },
                buttonStyle = activeOccasion.buttonStyle.ifBlank { resolved.buttonStyle },
                motifKey = activeOccasion.motifKey.ifBlank { "confetti" },
                audioTrackKey = activeOccasion.audioTrackKey.ifBlank { "none" },
                audioUrl = activeOccasion.audioUrl,
                audioLoop = activeOccasion.audioLoop
            )
        }

        // Layer 3: Scroll Preview (subtle chrome & accent tint on HomeScreen while scrolling past a World showcase)
        if (selectedWorld == null && scrollPreviewWorld != null) {
            resolved = resolved.copy(
                sourceLayer = "scroll_preview",
                worldSlug = scrollPreviewWorld.slug,
                worldName = scrollPreviewWorld.name,
                accentHex = scrollPreviewWorld.accentColor.ifBlank { resolved.accentHex },
                secondaryAccentHex = scrollPreviewWorld.secondaryAccent.ifBlank { resolved.secondaryAccentHex },
                fontStyle = scrollPreviewWorld.fontStyle.ifBlank { resolved.fontStyle },
                ambientPreset = scrollPreviewWorld.ambientPreset,
                animationKey = scrollPreviewWorld.animationKey,
                isScrollPreview = true,
                isWorldActive = false
            )
        }

        // Layer 4: Selected Fashion World (full immersive takeover inside StyleWorld & persisted navigation stack)
        if (selectedWorld != null) {
            val isDarkWorld = selectedWorld.mode.equals("dark", ignoreCase = true)
            val resolvedWorldAudioUrl = selectedWorld.audioUrl ?: activeOccasion?.audioUrl
            val resolvedWorldAudioLoop = if (selectedWorld.audioUrl != null) selectedWorld.audioLoop else (activeOccasion?.audioLoop ?: true)
            resolved = resolved.copy(
                sourceLayer = "fashion_world",
                themeKey = selectedWorld.themeKey.ifBlank { resolved.themeKey },
                worldSlug = selectedWorld.slug,
                worldName = selectedWorld.name,
                occasionSlug = activeOccasion?.slug,
                occasionName = activeOccasion?.name.orEmpty(),
                occasionButtonStyle = activeOccasion?.buttonStyle.orEmpty(),
                isDark = isDarkWorld,
                backgroundHex = selectedWorld.backgroundColor.ifBlank { if (isDarkWorld) "#0B0B10" else "#FAF9F6" },
                surfaceHex = selectedWorld.surfaceColor.ifBlank { if (isDarkWorld) "#14141C" else "#FFFFFF" },
                surfaceElevatedHex = selectedWorld.surfaceElevated.ifBlank { if (isDarkWorld) "#1E1E28" else "#F3F1EC" },
                primaryHex = selectedWorld.primaryColor.ifBlank { if (isDarkWorld) "#FFFFFF" else "#09090B" },
                accentHex = selectedWorld.accentColor.ifBlank { "#2563EB" },
                secondaryAccentHex = selectedWorld.secondaryAccent.ifBlank { resolved.secondaryAccentHex },
                textPrimaryHex = selectedWorld.textPrimary.ifBlank { if (isDarkWorld) "#FAFAFA" else "#09090B" },
                textSecondaryHex = selectedWorld.textSecondary.ifBlank { if (isDarkWorld) "#A1A1AA" else "#52525B" },
                borderHex = selectedWorld.borderColor.ifBlank { if (isDarkWorld) "#2E2E3A" else "#E4E4E7" },
                fontStyle = selectedWorld.fontStyle.ifBlank { "editorial_serif" },
                buttonStyle = selectedWorld.buttonStyle.ifBlank { "sharp" },
                cardRadiusDp = selectedWorld.cardRadiusDp.coerceIn(0, 28),
                borderWidthDp = selectedWorld.borderWidthDp.coerceIn(0.5f, 3f),
                ambientPreset = selectedWorld.ambientPreset,
                audioTrackKey = selectedWorld.audioTrackKey,
                audioUrl = resolvedWorldAudioUrl,
                audioLoop = resolvedWorldAudioLoop,
                audioVolume = selectedWorld.audioVolume.coerceIn(0.05f, 1.0f),
                promoVideoUrl = selectedWorld.promoVideoUrl,
                promoVideoPosterUrl = selectedWorld.promoVideoPosterUrl,
                promoVideoIsSponsored = selectedWorld.promoVideoIsSponsored,
                runwayConfig = selectedWorld.runwayConfig,
                hasSubmittedFeedback = selectedWorld.hasSubmittedFeedback,
                sfxProfile = selectedWorld.sfxProfile,
                animationKey = selectedWorld.animationKey,
                motifKey = selectedWorld.ambientPreset,
                isWorldActive = true,
                isScrollPreview = false,
                isLuxeIsolated = false
            )
        }

        // Layer 5: Campaign subtle accent refinement when no explicit world override is active
        if (activeCampaign != null && selectedWorld == null && activeCampaign.accentColor.isNotBlank()) {
            resolved = resolved.copy(
                secondaryAccentHex = activeCampaign.primaryColor.ifBlank { resolved.secondaryAccentHex }
            )
        }

        return resolved
    }

    /**
     * Unified Central Remote Design System resolver obeying strict layer priority:
     *   1. Bundled Default / Active Manifest Slot (0 Default vs 1 Live)
     *   2. Selected Fashion World (preserves world's own layout, typography, colors & chrome!)
     *   3. Active 8-day-before Occasion overlay (subtle festive accent overlay; never erases world layout)
     *   4. Route guards (Luxe obsidian isolated; Checkout/Auth clean legible surface)
     */
    fun resolveWithManifest(
        manifest: DesignManifest,
        fallbackTheme: ThemeTokenSpec = ThemeTokenSpec(),
        activeOccasion: OccasionTheme? = null,
        selectedWorld: FashionWorldTheme? = null,
        scrollPreviewWorld: FashionWorldTheme? = null,
        activeCampaign: CampaignItem? = null,
        isLuxeRoute: Boolean = false,
        isNeutralCheckoutOrAuthRoute: Boolean = false
    ): ResolvedFashionWorldTheme {
        val c = manifest.tokens.colors
        val t = manifest.tokens.typography
        val r = manifest.tokens.radii
        val m = manifest.tokens.motion
        val btn = manifest.components.button

        val manifestBase = fallbackTheme.copy(
            themeKey = manifest.manifestId.ifBlank { fallbackTheme.themeKey },
            mode = c.mode.ifBlank { fallbackTheme.mode },
            backgroundColor = c.background.ifBlank { fallbackTheme.backgroundColor },
            surfaceColor = c.surface.ifBlank { fallbackTheme.surfaceColor },
            surfaceElevated = c.surfaceElevated.ifBlank { fallbackTheme.surfaceElevated },
            primaryColor = c.primary.ifBlank { fallbackTheme.primaryColor },
            accentColor = c.accent.ifBlank { fallbackTheme.accentColor },
            secondaryAccent = c.secondaryAccent.ifBlank { fallbackTheme.secondaryAccent },
            textPrimary = c.textPrimary.ifBlank { fallbackTheme.textPrimary },
            textSecondary = c.textSecondary.ifBlank { fallbackTheme.textSecondary },
            borderColor = c.border.ifBlank { fallbackTheme.borderColor },
            fontStyle = t.displayFont.ifBlank { fallbackTheme.fontStyle },
            buttonStyle = btn.primaryVariant.ifBlank { fallbackTheme.buttonStyle },
            cardRadiusDp = r.cardDp.coerceIn(2, 28),
            borderWidthDp = r.borderWidthDp.toFloat().coerceIn(0.5f, 3f),
            motionIntensity = when (m.intensity.lowercase(Locale.ROOT)) {
                "none" -> 0f
                "expressive" -> 0.9f
                else -> 0.65f
            }
        )

        var resolved = resolve(
            defaultTheme = manifestBase,
            activeOccasion = activeOccasion,
            selectedWorld = selectedWorld,
            scrollPreviewWorld = scrollPreviewWorld,
            activeCampaign = activeCampaign,
            isLuxeRoute = isLuxeRoute,
            isNeutralCheckoutOrAuthRoute = isNeutralCheckoutOrAuthRoute
        )

        if (isLuxeRoute || isNeutralCheckoutOrAuthRoute) {
            return resolved
        }

        // Apply v1.2.0 remote World Kit overrides (palette, typography, pixel_text, button_skin, radii, motion_preset)
        if (selectedWorld != null) {
            val worldOverride = manifest.worldOverride(selectedWorld.slug)
            val kit = worldOverride?.kit
            if (kit != null) {
                val pal = kit.palette
                val kitIsDark = when (pal?.mode?.lowercase(Locale.ROOT)) {
                    "dark" -> true
                    "light" -> false
                    else -> resolved.isDark
                }
                resolved = resolved.copy(
                    isDark = kitIsDark,
                    backgroundHex = pal?.background?.takeIf { it.isNotBlank() } ?: resolved.backgroundHex,
                    surfaceHex = pal?.surface?.takeIf { it.isNotBlank() } ?: resolved.surfaceHex,
                    textPrimaryHex = pal?.textPrimary?.takeIf { it.isNotBlank() } ?: resolved.textPrimaryHex,
                    textSecondaryHex = pal?.textSecondary?.takeIf { it.isNotBlank() } ?: resolved.textSecondaryHex,
                    accentHex = pal?.accent?.takeIf { it.isNotBlank() } ?: resolved.accentHex,
                    secondaryAccentHex = pal?.secondaryAccent?.takeIf { it.isNotBlank() } ?: resolved.secondaryAccentHex,
                    borderHex = pal?.border?.takeIf { it.isNotBlank() } ?: resolved.borderHex,
                    fontStyle = kit.typography?.displayFont?.takeIf { it.isNotBlank() }
                        ?: worldOverride.fontStyle.takeIf { it.isNotBlank() }
                        ?: resolved.fontStyle,
                    buttonStyle = kit.buttonSkin.takeIf { it.isNotBlank() }
                        ?: worldOverride.buttonStyle.takeIf { it.isNotBlank() }
                        ?: resolved.buttonStyle,
                    cardRadiusDp = (kit.components?.productCard?.cornerRadiusDp ?: worldOverride.cardRadiusDp).coerceIn(0, 28),
                    motionPreset = kit.motionPreset.ifBlank { resolved.motionPreset },
                    pixelText = kit.typography?.pixelText == true
                )
            } else if (worldOverride != null) {
                resolved = resolved.copy(
                    fontStyle = worldOverride.fontStyle.takeIf { it.isNotBlank() } ?: resolved.fontStyle,
                    buttonStyle = worldOverride.buttonStyle.takeIf { it.isNotBlank() } ?: resolved.buttonStyle,
                    cardRadiusDp = worldOverride.cardRadiusDp.coerceIn(0, 28)
                )
            }
        } else if (activeOccasion != null) {
            // Apply v1.2.0 remote Occasion Kit accent & motif refinements on default browsing
            val occKit = manifest.occasionOverrides.occasionKit(activeOccasion.slug)
            if (occKit != null) {
                resolved = resolved.copy(
                    accentHex = occKit.colors.accent.takeIf { it.isNotBlank() } ?: resolved.accentHex,
                    secondaryAccentHex = occKit.colors.primary.takeIf { it.isNotBlank() } ?: resolved.secondaryAccentHex,
                    motifKey = occKit.motif.takeIf { it.isNotBlank() } ?: resolved.motifKey,
                    motionPreset = occKit.motionPreset.takeIf { it.isNotBlank() } ?: resolved.motionPreset
                )
            }
        }

        // When inside a Fashion World while an Occasion is active, preserve world's layout/typography
        // and carry active occasion metadata so UI can render a subtle festive emblem without breaking the world!
        if (selectedWorld != null && activeOccasion != null) {
            val occKit = manifest.occasionOverrides.occasionKit(activeOccasion.slug)
            resolved = resolved.copy(
                occasionSlug = activeOccasion.slug,
                occasionName = activeOccasion.name,
                secondaryAccentHex = occKit?.colors?.accent?.takeIf { it.isNotBlank() }
                    ?: activeOccasion.accentColor.ifBlank { resolved.secondaryAccentHex }
            )
        }
        return resolved
    }

    /**
     * Unified theme resolver with strict precedence (highest first):
     *   1. Active Occasion override (festival skin / active occasion tab)
     *   2. Selected Fashion World (12 worlds style layer)
     *   3. Remote manifest default tokens (respecting `tokens.colors.dark` when [preferDarkMode] is true)
     *   4. Bundled default fallback
     *
     * World mode rule: a world's `mode: "dark"` is honored ONLY when [preferDarkMode] is true OR
     * [isImmersiveWorldRoute] is true (inside that world's dedicated StyleWorldRoute). On Home /
     * Shop / Bag / Discover in light mode, a crisp light variant (>= 94% luminance background) is
     * derived while keeping the world's accent, typography, radii, and kit art.
     */
    fun resolveWithPrecedence(
        manifest: DesignManifest,
        fallbackTheme: ThemeTokenSpec = ThemeTokenSpec(),
        activeOccasion: OccasionTheme? = null,
        selectedWorld: FashionWorldTheme? = null,
        scrollPreviewWorld: FashionWorldTheme? = null,
        activeCampaign: CampaignItem? = null,
        preferDarkMode: Boolean = false,
        isLuxeRoute: Boolean = false,
        isNeutralCheckoutOrAuthRoute: Boolean = false,
        isImmersiveWorldRoute: Boolean = false
    ): ResolvedFashionWorldTheme {
        if (isLuxeRoute) return LUXE_ISOLATED_THEME

        val lightColors = manifest.tokens.colors
        val darkColors = lightColors.dark
        val useDarkTokens = preferDarkMode || lightColors.mode.equals("dark", ignoreCase = true)

        val baseTheme = if (useDarkTokens && darkColors != null) {
            fallbackTheme.copy(
                themeKey = manifest.manifestId.ifBlank { fallbackTheme.themeKey },
                mode = "dark",
                backgroundColor = darkColors.background,
                surfaceColor = darkColors.surface,
                surfaceElevated = darkColors.surfaceElevated,
                primaryColor = darkColors.primary,
                accentColor = darkColors.accent,
                secondaryAccent = darkColors.secondaryAccent,
                textPrimary = darkColors.textPrimary,
                textSecondary = darkColors.textSecondary,
                borderColor = darkColors.border,
                fontStyle = manifest.tokens.typography.displayFont.ifBlank { fallbackTheme.fontStyle },
                buttonStyle = manifest.components.button.primaryVariant.ifBlank { fallbackTheme.buttonStyle },
                cardRadiusDp = manifest.tokens.radii.cardDp.coerceIn(2, 28)
            )
        } else if (useDarkTokens) {
            fallbackTheme.copy(
                themeKey = manifest.manifestId.ifBlank { fallbackTheme.themeKey },
                mode = "dark",
                backgroundColor = "#0B0B10",
                surfaceColor = "#14141C",
                surfaceElevated = "#1D1D28",
                primaryColor = "#FFFFFF",
                accentColor = lightColors.accent.ifBlank { "#D4AF37" },
                secondaryAccent = lightColors.secondaryAccent.ifBlank { "#94A3B8" },
                textPrimary = "#FAFAFA",
                textSecondary = "#A1A1AA",
                borderColor = "#272732",
                fontStyle = manifest.tokens.typography.displayFont.ifBlank { fallbackTheme.fontStyle },
                buttonStyle = manifest.components.button.primaryVariant.ifBlank { fallbackTheme.buttonStyle },
                cardRadiusDp = manifest.tokens.radii.cardDp.coerceIn(2, 28)
            )
        } else {
            fallbackTheme.copy(
                themeKey = manifest.manifestId.ifBlank { fallbackTheme.themeKey },
                mode = "light",
                backgroundColor = lightColors.background.ifBlank { fallbackTheme.backgroundColor },
                surfaceColor = lightColors.surface.ifBlank { fallbackTheme.surfaceColor },
                surfaceElevated = lightColors.surfaceElevated.ifBlank { fallbackTheme.surfaceElevated },
                primaryColor = lightColors.primary.ifBlank { fallbackTheme.primaryColor },
                accentColor = lightColors.accent.ifBlank { fallbackTheme.accentColor },
                secondaryAccent = lightColors.secondaryAccent.ifBlank { fallbackTheme.secondaryAccent },
                textPrimary = lightColors.textPrimary.ifBlank { fallbackTheme.textPrimary },
                textSecondary = lightColors.textSecondary.ifBlank { fallbackTheme.textSecondary },
                borderColor = lightColors.border.ifBlank { fallbackTheme.borderColor },
                fontStyle = manifest.tokens.typography.displayFont.ifBlank { fallbackTheme.fontStyle },
                buttonStyle = manifest.components.button.primaryVariant.ifBlank { fallbackTheme.buttonStyle },
                cardRadiusDp = manifest.tokens.radii.cardDp.coerceIn(2, 28)
            )
        }

        // Layer 1 (lowest): Default manifest theme
        var resolved = resolve(
            defaultTheme = baseTheme,
            activeOccasion = null,
            selectedWorld = null,
            scrollPreviewWorld = scrollPreviewWorld,
            activeCampaign = activeCampaign,
            isLuxeRoute = false,
            isNeutralCheckoutOrAuthRoute = isNeutralCheckoutOrAuthRoute
        )
        if (isNeutralCheckoutOrAuthRoute) {
            return if (!preferDarkMode) {
                resolved.copy(
                    isDark = false,
                    backgroundHex = lightColors.background.ifBlank { "#FAF9F6" },
                    surfaceHex = lightColors.surface.ifBlank { "#FFFFFF" },
                    surfaceElevatedHex = lightColors.surfaceElevated.ifBlank { "#F3F1EC" },
                    primaryHex = lightColors.primary.ifBlank { "#09090B" },
                    textPrimaryHex = lightColors.textPrimary.ifBlank { "#09090B" },
                    textSecondaryHex = lightColors.textSecondary.ifBlank { "#52525B" },
                    borderHex = lightColors.border.ifBlank { "#E4E4E7" }
                )
            } else {
                resolved
            }
        }

        // Layer 2 (middle): Selected fashion world
        val validWorld = selectedWorld?.takeIf { it.slug.isNotBlank() }
        if (validWorld != null) {
            resolved = resolveWithManifest(
                manifest = manifest,
                fallbackTheme = baseTheme,
                activeOccasion = null,
                selectedWorld = validWorld,
                scrollPreviewWorld = null,
                activeCampaign = activeCampaign,
                isLuxeRoute = false,
                isNeutralCheckoutOrAuthRoute = false
            )
            // Respect light mode outside the dedicated StyleWorldRoute: derive a >= 94% luminance
            // light background while keeping the world's accent, typography, radii, and kit art.
            if (!preferDarkMode && !isImmersiveWorldRoute && resolved.isDark) {
                val accentColor = parseHexColor(resolved.accentHex, Color(0xFF1D4ED8))
                val derivedBg = deriveLightWorldBackground(
                    accent = accentColor,
                    basePaper = parseHexColor(lightColors.background, Color(0xFFFAF9F6)),
                    minLuminance = 0.94f
                )
                val derivedElevated = deriveLightWorldBackground(
                    accent = accentColor,
                    basePaper = parseHexColor(lightColors.surfaceElevated, Color(0xFFF3F1EC)),
                    minLuminance = 0.91f
                )
                resolved = resolved.copy(
                    isDark = false,
                    backgroundHex = derivedBg.toHexRgbString(),
                    surfaceHex = lightColors.surface.ifBlank { "#FFFFFF" },
                    surfaceElevatedHex = derivedElevated.toHexRgbString(),
                    primaryHex = lightColors.primary.ifBlank { "#09090B" },
                    textPrimaryHex = lightColors.textPrimary.ifBlank { "#09090B" },
                    textSecondaryHex = lightColors.textSecondary.ifBlank { "#52525B" },
                    borderHex = lightColors.border.ifBlank { "#E4E4E7" }
                )
            } else if (preferDarkMode && !isImmersiveWorldRoute && !resolved.isDark) {
                // In dark mode on general routes, keep dark canvas while preserving world accent & typography.
                resolved = resolved.copy(
                    isDark = true,
                    backgroundHex = darkColors?.background ?: "#0B0B10",
                    surfaceHex = darkColors?.surface ?: "#14141C",
                    surfaceElevatedHex = darkColors?.surfaceElevated ?: "#1D1D28",
                    primaryHex = darkColors?.primary ?: "#FFFFFF",
                    textPrimaryHex = darkColors?.textPrimary ?: "#FAFAFA",
                    textSecondaryHex = darkColors?.textSecondary ?: "#A1A1AA",
                    borderHex = darkColors?.border ?: "#272732"
                )
            }
        }

        // Layer 3 (highest): Active Occasion override takes precedence over world and default.
        // Inside an immersive StyleWorldRoute, preserve the world's own background/layout and
        // carry occasion accents/metadata so the world stays recognizable.
        if (activeOccasion != null && activeOccasion.slug.isNotBlank()) {
            val occKit = manifest.occasionOverrides.occasionKit(activeOccasion.slug)
            val occAccent = occKit?.colors?.accent?.takeIf { it.isNotBlank() }
                ?: activeOccasion.accentColor.takeIf { it.isNotBlank() }
                ?: resolved.accentHex
            val occPrimary = occKit?.colors?.primary?.takeIf { it.isNotBlank() }
                ?: activeOccasion.primaryColor.takeIf { it.isNotBlank() }
                ?: resolved.primaryHex
            val occBg = occKit?.colors?.background?.takeIf { it.isNotBlank() }
                ?: activeOccasion.backgroundColor.takeIf { it.isNotBlank() }

            if (isImmersiveWorldRoute && validWorld != null) {
                resolved = resolved.copy(
                    occasionSlug = activeOccasion.slug,
                    occasionName = activeOccasion.name,
                    occasionButtonStyle = activeOccasion.buttonStyle,
                    secondaryAccentHex = occAccent
                )
            } else {
                val safeOccBg = when {
                    preferDarkMode -> resolved.backgroundHex
                    occBg != null -> {
                        val parsed = parseHexColor(occBg, Color(0xFFFAF9F6))
                        if (parsed.luminance() < 0.70f) {
                            deriveLightWorldBackground(parseHexColor(occAccent, Color(0xFFD97706))).toHexRgbString()
                        } else {
                            occBg
                        }
                    }
                    else -> resolved.backgroundHex
                }
                resolved = resolved.copy(
                    sourceLayer = "occasion",
                    occasionSlug = activeOccasion.slug,
                    occasionName = activeOccasion.name,
                    occasionButtonStyle = activeOccasion.buttonStyle,
                    primaryHex = occPrimary,
                    accentHex = occAccent,
                    secondaryAccentHex = occPrimary,
                    backgroundHex = safeOccBg,
                    motifKey = occKit?.motif?.takeIf { it.isNotBlank() }
                        ?: activeOccasion.motifKey.ifBlank { resolved.motifKey },
                    motionPreset = occKit?.motionPreset?.takeIf { it.isNotBlank() } ?: resolved.motionPreset,
                    buttonStyle = activeOccasion.buttonStyle.ifBlank { resolved.buttonStyle },
                    fontStyle = activeOccasion.fontStyle.ifBlank { resolved.fontStyle }
                )
            }
        }

        return resolved
    }
}

object FashionWorldTransitionController {

    private val PERSISTENT_ROUTES = setOf(
        "StyleWorldRoute",
        "ProductDetailsRoute",
        "ShopRoute",
        "CategoriesRoute",
        "BrandDetailRoute",
        "VideosRoute",
        "DiscoverRoute",
        "BagRoute"
    )

    fun enterWorld(
        current: FashionWorldContext,
        worldSlug: String,
        worldId: Int? = null
    ): FashionWorldContext {
        val cleanSlug = worldSlug.trim().lowercase(Locale.ROOT)
        if (cleanSlug.isBlank() || cleanSlug == "luxe" || cleanSlug == "elite") {
            return exitWorld()
        }
        val updatedStack = if (current.activeWorldSlug == cleanSlug) {
            current.backStackSlugs
        } else {
            (current.backStackSlugs + listOfNotNull(current.activeWorldSlug)).distinct().takeLast(6)
        }
        return current.copy(
            activeWorldSlug = cleanSlug,
            activeWorldId = worldId ?: current.activeWorldId,
            scrollPreviewWorldSlug = null,
            backStackSlugs = updatedStack,
            isLockedToWorld = true
        )
    }

    fun exitWorld(): FashionWorldContext = FashionWorldContext()

    fun popOrExit(current: FashionWorldContext): FashionWorldContext {
        val previous = current.backStackSlugs.lastOrNull()
        return if (previous != null) {
            current.copy(
                activeWorldSlug = previous,
                activeWorldId = null,
                backStackSlugs = current.backStackSlugs.dropLast(1),
                isLockedToWorld = true
            )
        } else {
            exitWorld()
        }
    }

    fun updateScrollPreview(
        current: FashionWorldContext,
        previewWorldSlug: String?
    ): FashionWorldContext {
        if (current.isInsideDedicatedWorld) return current
        val clean = previewWorldSlug?.trim()?.lowercase(Locale.ROOT)?.takeIf {
            it.isNotBlank() && it != "luxe" && it != "elite"
        }
        return if (current.scrollPreviewWorldSlug == clean) current
        else current.copy(scrollPreviewWorldSlug = clean)
    }

    fun onRouteChanged(current: FashionWorldContext, simpleRouteName: String): FashionWorldContext {
        return when {
            simpleRouteName == "EliteRoute" || simpleRouteName == "ElitePackRoute" -> {
                // Luxe is strictly isolated from playful worlds.
                exitWorld()
            }
            simpleRouteName == "HomeRoute" -> {
                // Returning to Home exits any temporary scroll preview; keeps active world ONLY if user didn't explicitly return home.
                exitWorld()
            }
            simpleRouteName in PERSISTENT_ROUTES -> current
            else -> current
        }
    }

    fun shouldPersistWorldForRoute(simpleRouteName: String): Boolean = simpleRouteName in PERSISTENT_ROUTES
}

object CampaignExperienceController {

    private val SUPPRESSED_ROUTES = setOf(
        "CheckoutRoute",
        "AuthRoute",
        "OrderTrackingRoute",
        "HelpSupportRoute"
    )

    fun isOfferSuppressedOnRoute(simpleRouteName: String): Boolean = simpleRouteName in SUPPRESSED_ROUTES

    fun selectEligibleOffer(
        offers: List<SmartOffer>,
        activeWorldSlug: String?,
        activeOccasionSlug: String?,
        cartSubtotal: Double,
        simpleRouteName: String,
        shownInSessionById: Map<Int, Int>,
        canShowByPersistence: (SmartOffer, Int) -> Boolean
    ): SmartOffer? {
        if (isOfferSuppressedOnRoute(simpleRouteName) || offers.isEmpty()) return null

        val cleanWorld = activeWorldSlug?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val cleanOccasion = activeOccasionSlug?.trim()?.lowercase(Locale.ROOT).orEmpty()

        return offers
            .asSequence()
            .filter { offer ->
                val shownCount = shownInSessionById[offer.offerId] ?: 0
                val worldMatch = offer.worldSlug.isBlank() || offer.worldSlug.equals(cleanWorld, ignoreCase = true)
                val occasionMatch = offer.occasionSlug.isBlank() || offer.occasionSlug.equals(cleanOccasion, ignoreCase = true)
                val cartMatch = cartSubtotal >= offer.minCartValue || offer.minCartValue <= 0.0
                worldMatch && occasionMatch && cartMatch && canShowByPersistence(offer, shownCount)
            }
            .sortedWith(
                compareByDescending<SmartOffer> {
                    when {
                        cleanWorld.isNotBlank() && it.worldSlug.equals(cleanWorld, ignoreCase = true) -> 3
                        cleanOccasion.isNotBlank() && it.occasionSlug.equals(cleanOccasion, ignoreCase = true) -> 2
                        else -> 1
                    }
                }.thenByDescending { it.priority }
            )
            .firstOrNull()
    }
}

@Composable
fun MelvoreDynamicTheme(
    resolvedTheme: ResolvedFashionWorldTheme,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val manifest = LocalMelvoreDesignManifest.current
    val durationMs = if (reducedMotion) 0 else manifest.tokens.motion.durationMs.coerceIn(150, 450)
    val bg by animateColorAsState(
        targetValue = parseHexColor(resolvedTheme.backgroundHex, if (resolvedTheme.isDark) Color(0xFF0B0B10) else Color(0xFFFAF9F6)),
        animationSpec = tween(durationMillis = durationMs),
        label = "dynamicBg"
    )
    val surface by animateColorAsState(
        targetValue = parseHexColor(resolvedTheme.surfaceHex, if (resolvedTheme.isDark) Color(0xFF14141C) else Color(0xFFFFFFFF)),
        animationSpec = tween(durationMillis = durationMs),
        label = "dynamicSurface"
    )
    val accent by animateColorAsState(
        targetValue = parseHexColor(resolvedTheme.accentHex, Color(0xFF1D4ED8)),
        animationSpec = tween(durationMillis = durationMs),
        label = "dynamicAccent"
    )
    val animated = resolvedTheme.copy(
        backgroundHex = bg.toHexRgbString(),
        surfaceHex = surface.toHexRgbString(),
        accentHex = accent.toHexRgbString()
    )
    CompositionLocalProvider(LocalFashionWorldTheme provides animated) {
        MelvoreTheme(
            darkTheme = animated.isDark,
            dynamicTheme = animated,
            content = content
        )
    }
}
