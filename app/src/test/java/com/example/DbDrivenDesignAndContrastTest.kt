package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.data.RemoteDesignManifestManager
import com.example.model.DesignColorTokens
import com.example.model.DesignManifest
import com.example.model.DesignManifestSupport
import com.example.model.DesignTokens
import com.example.model.FashionWorldTheme
import com.example.model.KitDimensionSpec
import com.example.model.ManifestSource
import com.example.model.OccasionTheme
import com.example.model.ThemeTokenSpec
import com.example.ui.theme.FashionWorldThemeResolver
import com.example.ui.theme.buildMelvoreSurfaceColors
import com.example.ui.theme.contrastRatio
import com.example.ui.theme.deriveLightWorldBackground
import com.example.ui.theme.ensureContrast
import java.io.File
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DbDrivenDesignAndContrastTest {

    @Test
    fun tolerantPerSectionParsingKeepsValidSectionsWhenOneSectionIsMalformed() {
        val manager = RemoteDesignManifestManager(context = null, sessionStore = null)
        // `components` is completely broken (wrong JSON type), one world entry is broken,
        // while `tokens`, `navigation`, and valid world `goth` are valid.
        val malformedJson = """
            {
              "manifest_id": "melvore_live_1_6",
              "design_version": "1.6.0",
              "schema_version": "1.6.0",
              "tokens": {
                "colors": {
                  "mode": "light",
                  "background": "#FBF8F3",
                  "surface": "#FFFFFF",
                  "accent": "#C81E4B",
                  "text_primary": "#111111"
                },
                "radii": {
                  "card_dp": 16,
                  "button_dp": 14
                }
              },
              "components": "THIS_IS_MALFORMED_NOT_AN_OBJECT",
              "worlds": {
                "broken_world": 12345,
                "goth": {
                  "template": "editorial_noir",
                  "font_style": "gothic",
                  "card_radius_dp": 4
                }
              },
              "pages": {
                "home": {
                  "sections": [
                    { "key": "hero", "title": "DB Hero", "enabled": true, "order": 1 }
                  ]
                }
              }
            }
        """.trimIndent()

        val outcome = manager.validateAndAdopt(
            rawManifestJson = malformedJson,
            activeDesign = 1,
            persistIfValid = false,
            etag = "\"etag-160\"",
            assetBaseUrl = "https://melvore.example.com",
            manifestHash = "abc123hash",
            sourceHint = ManifestSource.REMOTE
        )

        assertEquals(ManifestSource.REMOTE, outcome.source)
        assertEquals("1.6.0", outcome.manifestVersion)
        assertEquals("\"etag-160\"", outcome.etag)
        assertEquals("https://melvore.example.com", outcome.assetBaseUrl)
        // Valid tokens and worlds survived!
        assertEquals("#C81E4B", outcome.manifest.tokens.colors.accent)
        assertEquals(16, outcome.manifest.tokens.radii.cardDp)
        assertNotNull(outcome.manifest.worlds["goth"])
        assertEquals(4, outcome.manifest.worlds["goth"]?.cardRadiusDp)
        // Malformed sections recorded warnings instead of silently discarding the whole manifest
        assertTrue(
            "Expected tolerant parsing warnings for malformed components and broken_world, got: ${outcome.warnings}",
            outcome.warnings.any { it.contains("components", ignoreCase = true) } &&
                outcome.warnings.any { it.contains("broken_world", ignoreCase = true) }
        )
    }

    @Test
    fun kitDimensionSpecSupportsBothArrayAndObjectFormats() {
        val fromArray = DesignManifestSupport.json.decodeFromString<KitDimensionSpec>("[320, 180]")
        assertEquals(320, fromArray.width)
        assertEquals(180, fromArray.height)

        val fromObject = DesignManifestSupport.json.decodeFromString<KitDimensionSpec>("""{"width": 640, "height": 360}""")
        assertEquals(640, fromObject.width)
        assertEquals(360, fromObject.height)
    }

    @Test
    fun themePriorityOrderIsOccasionOverWorldOverManifestOverDefault() {
        val manifest = DesignManifest(
            manifestId = "remote_160",
            designVersion = "1.6.0",
            tokens = DesignTokens(
                colors = DesignColorTokens(
                    mode = "light",
                    background = "#FAF9F6",
                    surface = "#FFFFFF",
                    accent = "#1D4ED8"
                )
            )
        )
        val world = FashionWorldTheme(
            slug = "k-fashion",
            name = "K-Fashion Seoul",
            mode = "light",
            accentColor = "#2563EB",
            backgroundColor = "#F3F5F9"
        )
        val occasion = OccasionTheme(
            slug = "diwali",
            name = "Diwali Edit",
            accentColor = "#D97706",
            primaryColor = "#92400E"
        )

        // 1. Default manifest only
        val manifestOnly = FashionWorldThemeResolver.resolveWithPrecedence(
            manifest = manifest,
            fallbackTheme = ThemeTokenSpec(),
            activeOccasion = null,
            selectedWorld = null,
            preferDarkMode = false
        )
        assertEquals("#1D4ED8", manifestOnly.accentHex)

        // 2. Selected world overrides manifest default
        val withWorld = FashionWorldThemeResolver.resolveWithPrecedence(
            manifest = manifest,
            fallbackTheme = ThemeTokenSpec(),
            activeOccasion = null,
            selectedWorld = world,
            preferDarkMode = false
        )
        assertEquals("k-fashion", withWorld.worldSlug)
        assertEquals("#2563EB", withWorld.accentHex)

        // 3. Active occasion overrides world accent & metadata (highest priority)
        val withOccasionAndWorld = FashionWorldThemeResolver.resolveWithPrecedence(
            manifest = manifest,
            fallbackTheme = ThemeTokenSpec(),
            activeOccasion = occasion,
            selectedWorld = world,
            preferDarkMode = false
        )
        assertEquals("diwali", withOccasionAndWorld.occasionSlug)
        assertEquals("#D97706", withOccasionAndWorld.accentHex)
    }

    @Test
    fun darkWorldDerivesLightVariantOutsideStyleWorldInLightModeAndStaysDarkOnStyleWorld() {
        val manifest = DesignManifest(designVersion = "1.6.0")
        val gothWorld = FashionWorldTheme(
            slug = "goth",
            name = "Goth & Dark Romance",
            mode = "dark",
            backgroundColor = "#070709",
            surfaceColor = "#111116",
            surfaceElevated = "#191920",
            accentColor = "#DC2626",
            textPrimary = "#FAFAFA"
        )

        // On Home in Light Mode: MUST NOT leak black surfaces (luminance must be >= 94%)
        val homeLight = FashionWorldThemeResolver.resolveWithPrecedence(
            manifest = manifest,
            selectedWorld = gothWorld,
            preferDarkMode = false,
            isLuxeRoute = false,
            isImmersiveWorldRoute = false
        )
        assertFalse("Home in light mode must stay light even when Goth world is selected", homeLight.isDark)
        val homeSurfaces = buildMelvoreSurfaceColors(
            resolvedTheme = homeLight,
            manifest = manifest,
            darkTheme = false
        )
        assertTrue(
            "Expected Home light-derived background luminance >= 0.90, was ${homeSurfaces.background.luminance()}",
            homeSurfaces.background.luminance() >= 0.90f
        )
        assertTrue(
            "Expected Home surface luminance >= 0.90, was ${homeSurfaces.surface.luminance()}",
            homeSurfaces.surface.luminance() >= 0.90f
        )
        assertEquals("#DC2626", homeLight.accentHex)

        // Inside StyleWorldRoute: honors world's dark aesthetic
        val styleWorldImmersive = FashionWorldThemeResolver.resolveWithPrecedence(
            manifest = manifest,
            selectedWorld = gothWorld,
            preferDarkMode = false,
            isLuxeRoute = false,
            isImmersiveWorldRoute = true
        )
        assertTrue("StyleWorldRoute should honor Goth world's dark mode", styleWorldImmersive.isDark)

        // On Luxe route: always isolated dark obsidian
        val luxe = FashionWorldThemeResolver.resolveWithPrecedence(
            manifest = manifest,
            selectedWorld = gothWorld,
            preferDarkMode = false,
            isLuxeRoute = true,
            isImmersiveWorldRoute = false
        )
        assertTrue(luxe.isLuxeIsolated)
        assertTrue(luxe.isDark)
    }

    @Test
    fun deriveLightWorldBackgroundMeetsLuminanceFloor() {
        val derived = deriveLightWorldBackground(Color(0xFFDC2626))
        assertTrue(
            "Derived light world background should have luminance >= 0.94, got ${derived.luminance()}",
            derived.luminance() >= 0.94f
        )
    }

    @Test
    fun ensureContrastGuaranteesWcagAaRatioAcrossProblematicPairs() {
        val pairs = listOf(
            Color(0xFFFFFFFF) to Color(0xFFFAF9F6), // light text on light paper
            Color(0xFF111111) to Color(0xFF0B0B10), // dark text on dark canvas
            Color(0xFFD4AF37) to Color(0xFFFFFFFF), // gold text on white
            Color(0xFF94A3B8) to Color(0xFFF3F1EC), // muted slate on light warm surface
            Color(0xFF475569) to Color(0xFF14141C)  // dim slate on dark card
        )

        pairs.forEach { (fg, bg) ->
            val guarded = ensureContrast(fg, bg, minRatio = 4.5f)
            val ratio = contrastRatio(guarded, bg)
            assertTrue(
                "Expected contrastRatio >= 4.5f for fg=$fg on bg=$bg, got $ratio",
                ratio >= 4.5f
            )
        }
    }

    @Test
    fun noForbiddenDarkHexLiteralsOrMixedBlackBranchesInUiSources() {
        val candidates = listOf(
            File("src/main/java/com/example/ui"),
            File("app/src/main/java/com/example/ui")
        )
        val uiRoot = candidates.firstOrNull { it.isDirectory }
            ?: error("Could not locate ui source directory from ${File(".").absolutePath}")

        val allowedThemeFiles = setOf(
            "Theme.kt",
            "MelvoreSurfaceColors.kt",
            "FashionWorldThemeEngine.kt"
        )

        val forbiddenPattern = Regex(
            """Color\(0x[0-9A-Fa-f]{2}0[0-9A-Fa-f]{5}\)|Color\(0xFF1[0-9A-Fa-f]{5}\)|Color\.Black|darkEditorialBg|obsidianCard|\bisLuxeDark\b"""
        )

        val violations = mutableListOf<String>()
        uiRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name !in allowedThemeFiles }
            .forEach { file ->
                file.readLines().forEachIndexed { idx, line ->
                    val trimmed = line.trim()
                    // Ignore pure comment lines
                    if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                        return@forEachIndexed
                    }
                    // Ignore DesignAuditScreen's sample gothWorld fixture definition
                    if (file.name == "DesignAuditScreen.kt" && trimmed.contains("backgroundColor = \"#")) {
                        return@forEachIndexed
                    }
                    val match = forbiddenPattern.find(line)
                    if (match != null) {
                        violations += "${file.name}:${idx + 1} -> ${match.value} ($trimmed)"
                    }
                }
            }

        assertTrue(
            "Found forbidden raw dark hex / mixed-black tokens outside theme allow-list:\n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }
}
