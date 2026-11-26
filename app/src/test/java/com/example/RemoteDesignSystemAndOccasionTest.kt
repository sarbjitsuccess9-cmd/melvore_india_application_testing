package com.example

import com.example.data.RemoteDesignManifestManager
import com.example.data.SecureSessionStore
import com.example.model.DesignManifest
import com.example.model.DesignManifestSupport
import com.example.model.FashionWorldTheme
import com.example.model.OccasionScheduleEvaluator
import com.example.model.OccasionTheme
import com.example.model.ScheduledOccasionSpec
import com.example.ui.components.RemoteDesignRegistry
import com.example.ui.theme.FashionWorldThemeResolver
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RemoteDesignSystemAndOccasionTest {

    private val istZone: ZoneId = ZoneId.of("Asia/Kolkata")

    private fun resolveWorkspaceFile(relativePath: String): File {
        val direct = File(relativePath)
        if (direct.exists()) return direct
        val parentRelative = File("../$relativePath")
        if (parentRelative.exists()) return parentRelative
        error("Could not locate workspace file: $relativePath (cwd=${File(".").absolutePath})")
    }

    @Test
    fun occasionSchedulerActivatesExactlyEightDaysBeforeInAsiaKolkataAndDeactivatesAfterEvent() {
        val diwali = ScheduledOccasionSpec(
            occasionId = 1,
            slug = "diwali-glow",
            name = "Diwali Luminous Edit",
            eventDate = "2026-10-20",
            activationDaysBefore = 8,
            timezone = "Asia/Kolkata",
            regionCountry = "India",
            priority = 20,
            isActive = true
        )

        // 1. Just before midnight 9 days prior -> inactive
        val beforeWindow = ZonedDateTime.of(2026, 10, 11, 23, 59, 59, 0, istZone)
        val beforeResult = OccasionScheduleEvaluator.evaluateAt(listOf(diwali), beforeWindow, country = "India")
        assertNull(beforeResult.winner)
        assertTrue(beforeResult.activeMatches.isEmpty())

        // 2. One second into the 8th day before (2026-10-12 00:00:01 IST) -> active
        val startWindow = ZonedDateTime.of(2026, 10, 12, 0, 0, 1, 0, istZone)
        val startResult = OccasionScheduleEvaluator.evaluateAt(listOf(diwali), startWindow, country = "India")
        assertEquals("diwali-glow", startResult.winner?.slug)
        assertEquals(2026, startResult.computedWindowStartIst?.year)
        assertEquals(10, startResult.computedWindowStartIst?.monthValue)
        assertEquals(12, startResult.computedWindowStartIst?.dayOfMonth)

        // 3. Final second of event_date (2026-10-20 23:59:59 IST) -> still active
        val endWindow = ZonedDateTime.of(2026, 10, 20, 23, 59, 59, 0, istZone)
        val endResult = OccasionScheduleEvaluator.evaluateAt(listOf(diwali), endWindow, country = "India")
        assertEquals("diwali-glow", endResult.winner?.slug)

        // 4. Automatically deactivates after midnight (2026-10-21 00:00:01 IST)
        val afterWindow = ZonedDateTime.of(2026, 10, 21, 0, 0, 1, 0, istZone)
        val afterResult = OccasionScheduleEvaluator.evaluateAt(listOf(diwali), afterWindow, country = "India")
        assertNull(afterResult.winner)
    }

    @Test
    fun occasionSchedulerRespectsActiveFlagRegionalSpecificityAndPriorityTieBreaking() {
        val inactiveOccasion = ScheduledOccasionSpec(
            occasionId = 1,
            slug = "disabled-festival",
            name = "Disabled Festival",
            eventDate = "2026-10-20",
            activationDaysBefore = 8,
            timezone = "Asia/Kolkata",
            regionCountry = "ALL",
            priority = 999,
            isActive = false
        )
        val nationalOccasion = ScheduledOccasionSpec(
            occasionId = 2,
            slug = "national-festive",
            name = "National Festive",
            eventDate = "2026-10-20",
            activationDaysBefore = 8,
            timezone = "Asia/Kolkata",
            regionCountry = "India",
            regionState = "",
            regionCity = "",
            priority = 10,
            isActive = true
        )
        val maharashtraOccasion = ScheduledOccasionSpec(
            occasionId = 3,
            slug = "mumbai-atelier-week",
            name = "Maharashtra Couture",
            eventDate = "2026-10-20",
            activationDaysBefore = 8,
            timezone = "Asia/Kolkata",
            regionCountry = "India",
            regionState = "Maharashtra",
            regionCity = "",
            priority = 10, // same priority as national, more specific (+2 state vs +1 country)
            isActive = true
        )
        val highPriorityGlobal = ScheduledOccasionSpec(
            occasionId = 4,
            slug = "global-headline",
            name = "Global Headline",
            eventDate = "2026-10-20",
            activationDaysBefore = 8,
            timezone = "Asia/Kolkata",
            regionCountry = "ALL",
            priority = 50,
            isActive = true
        )

        val insideTime = ZonedDateTime.of(2026, 10, 15, 12, 0, 0, 0, istZone)

        // Disabled festival never wins; equal priority prefers Maharashtra over National when user is in Maharashtra
        val maharashtraResult = OccasionScheduleEvaluator.evaluateAt(
            occasions = listOf(inactiveOccasion, nationalOccasion, maharashtraOccasion),
            evaluationTime = insideTime,
            country = "India",
            state = "Maharashtra"
        )
        assertEquals("mumbai-atelier-week", maharashtraResult.winner?.slug)

        // User in Delhi does not match Maharashtra filter -> National wins cleanly
        val delhiResult = OccasionScheduleEvaluator.evaluateAt(
            occasions = listOf(inactiveOccasion, nationalOccasion, maharashtraOccasion),
            evaluationTime = insideTime,
            country = "India",
            state = "Delhi"
        )
        assertEquals("national-festive", delhiResult.winner?.slug)

        // Overlapping higher priority wins across equal windows
        val priorityResult = OccasionScheduleEvaluator.evaluateAt(
            occasions = listOf(maharashtraOccasion, highPriorityGlobal),
            evaluationTime = insideTime,
            country = "India",
            state = "Maharashtra"
        )
        assertEquals("global-headline", priorityResult.winner?.slug)
    }

    @Test
    fun occasionBlendingPreservesFashionWorldLayoutTypographyAndIsolationGuards() {
        val liveJson = resolveWorkspaceFile("php_api/design-system/live/manifest.json").readText()
        val liveManifest = DesignManifestSupport.json.decodeFromString<DesignManifest>(liveJson)

        val diwaliOccasion = OccasionTheme(
            occasionId = 1,
            slug = "diwali-glow",
            name = "Diwali Luminous Edit",
            motifKey = "diya_flame",
            primaryColor = "#D97706",
            accentColor = "#F59E0B",
            fontStyle = "editorial_serif"
        )

        val worlds = listOf(
            FashionWorldTheme(1, "y2k", "Y2K", mode = "dark", fontStyle = "futuristic", backgroundColor = "#0F0A1F", accentColor = "#22D3EE"),
            FashionWorldTheme(2, "goth", "Goth", mode = "dark", fontStyle = "gothic", backgroundColor = "#070709", accentColor = "#DC2626"),
            FashionWorldTheme(3, "coquette", "Coquette", mode = "light", fontStyle = "editorial_serif", backgroundColor = "#FFF5F7", accentColor = "#EC4899"),
            FashionWorldTheme(4, "streetwear", "Streetwear", mode = "dark", fontStyle = "editorial_sans", backgroundColor = "#111214", accentColor = "#F97316"),
            FashionWorldTheme(5, "k-fashion", "K-Fashion", mode = "light", fontStyle = "k_fashion", backgroundColor = "#F6F7FB", accentColor = "#4F46E5")
        )

        val defaultJson = resolveWorkspaceFile("php_api/design-system/default/manifest.json").readText()
        val defaultManifest = DesignManifestSupport.json.decodeFromString<DesignManifest>(defaultJson)

        // Inside each world with active occasion: world identity/font/background stays intact, occasion metadata is preserved
        for (world in worlds) {
            val blendedLive = FashionWorldThemeResolver.resolveWithManifest(
                manifest = liveManifest,
                activeOccasion = diwaliOccasion,
                selectedWorld = world
            )
            val expectedLiveBg = liveManifest.worldOverride(world.slug)?.kit?.palette?.background
                ?.takeIf { it.isNotBlank() } ?: world.backgroundColor
            assertTrue("World ${world.slug} must stay active", blendedLive.isWorldActive)
            assertEquals(world.slug, blendedLive.worldSlug)
            assertEquals(world.fontStyle, blendedLive.fontStyle)
            assertEquals(expectedLiveBg, blendedLive.backgroundHex)
            assertNotEquals("Occasion must never overwrite world background", "#1F1209", blendedLive.backgroundHex)
            assertEquals("diwali-glow", blendedLive.occasionSlug)
            assertEquals("Diwali Luminous Edit", blendedLive.occasionName)

            // Default v1.0.0 manifest (no world kit) preserves DB world.backgroundColor directly
            val blendedDefault = FashionWorldThemeResolver.resolveWithManifest(
                manifest = defaultManifest,
                activeOccasion = diwaliOccasion,
                selectedWorld = world
            )
            assertEquals(world.backgroundColor, blendedDefault.backgroundHex)
        }

        // Checkout/Auth always stay legible light editorial from manifest tokens
        val checkoutTheme = FashionWorldThemeResolver.resolveWithManifest(
            manifest = liveManifest,
            activeOccasion = diwaliOccasion,
            selectedWorld = worlds.first(),
            isNeutralCheckoutOrAuthRoute = true
        )
        assertFalse(checkoutTheme.isWorldActive)
        assertFalse(checkoutTheme.isDark)
        assertEquals("#FAF7F2", checkoutTheme.backgroundHex)

        // Luxe remains strictly isolated obsidian/gold
        val luxeTheme = FashionWorldThemeResolver.resolveWithManifest(
            manifest = liveManifest,
            activeOccasion = diwaliOccasion,
            selectedWorld = worlds.first(),
            isLuxeRoute = true
        )
        assertTrue(luxeTheme.isLuxeIsolated)
        assertFalse(luxeTheme.isWorldActive)
        assertEquals("#050507", luxeTheme.backgroundHex)
    }

    @Test
    fun defaultAndLiveManifestsAndBundledAssetParseSwapSectionsAndCoverIconRegistry() {
        val defaultFile = resolveWorkspaceFile("php_api/design-system/default/manifest.json")
        val liveFile = resolveWorkspaceFile("php_api/design-system/live/manifest.json")
        val bundledAsset = resolveWorkspaceFile("app/src/main/assets/design_system_default.json")
        val iconRegistryFile = resolveWorkspaceFile("php_api/design-system/assets/icons/icon-registry.json")

        val defaultRaw = defaultFile.readText()
        val liveRaw = liveFile.readText()
        val bundledRaw = bundledAsset.readText()

        assertEquals("Bundled Android asset must match server default manifest", defaultRaw, bundledRaw)

        val defaultManifest = DesignManifestSupport.json.decodeFromString<DesignManifest>(defaultRaw)
        val liveManifest = DesignManifestSupport.json.decodeFromString<DesignManifest>(liveRaw)

        assertEquals("default", defaultManifest.slot)
        assertEquals("live", liveManifest.slot)
        assertEquals("flat_editorial", defaultManifest.navigation.bottomBarVariant)
        assertEquals("floating_dock", liveManifest.navigation.bottomBarVariant)
        assertEquals("circular_story", defaultManifest.components.categoryStrip.variant)
        assertEquals("portrait_tiles", liveManifest.components.categoryStrip.variant)

        // Home section order swaps cleanly between Default (0) and Live (1)
        val defaultHomeOrder = RemoteDesignRegistry.resolveOrderedHomeSectionIds(defaultManifest)
        val liveHomeOrder = RemoteDesignRegistry.resolveOrderedHomeSectionIds(liveManifest)
        assertEquals("search_bar", defaultHomeOrder.first())
        assertEquals("occasion_banner", liveHomeOrder[0])
        assertEquals("hero_carousel", liveHomeOrder[1])
        assertNotEquals(defaultHomeOrder, liveHomeOrder)

        // StyleWorld per-world section order swaps cleanly
        assertEquals(
            listOf("world_hero", "world_promo_film", "vibe_satisfaction", "world_subcultures", "world_products", "world_brands"),
            RemoteDesignRegistry.resolveStyleWorldSectionOrder(defaultManifest, "y2k")
        )
        assertEquals(
            listOf("world_hero", "vibe_satisfaction", "world_promo_film", "world_subcultures", "world_products", "world_brands"),
            RemoteDesignRegistry.resolveStyleWorldSectionOrder(liveManifest, "y2k")
        )

        // Verify all registered icon keys in icon-registry.json resolve without exception
        val registryObj = DesignManifestSupport.json.decodeFromString<JsonObject>(iconRegistryFile.readText())
        val iconsJson = registryObj["icons"].toString()
        val keyRegex = Regex("\"key\"\\s*:\\s*\"([^\"]+)\"")
        val keys = keyRegex.findAll(iconsJson).map { it.groupValues[1] }.toList()
        assertTrue(keys.size >= 20)
        keys.forEach { key ->
            assertNotNull("Icon vector must resolve for key=$key", RemoteDesignRegistry.iconForKey(key))
        }
    }

    @Test
    fun manifestManagerVerifiesSha256RejectsIncompatibleSchemaOrCorruptJsonAndRollsBackSafely() {
        val context = RuntimeEnvironment.getApplication()
        val store = SecureSessionStore(context)
        val manager = RemoteDesignManifestManager(
            context = context,
            sessionStore = store,
            appSchemaVersion = "1.0.0"
        )

        val liveRaw = resolveWorkspaceFile("php_api/design-system/live/manifest.json").readText()
        val liveJsonObj = DesignManifestSupport.json.decodeFromString<JsonObject>(liveRaw)

        // Canonical SHA-256 is deterministic across repeated calls and key order changes
        val checksumA = DesignManifestSupport.sha256Canonical(liveJsonObj)
        val checksumB = DesignManifestSupport.sha256Canonical(liveJsonObj)
        assertEquals(64, checksumA.length)
        assertEquals(checksumA, checksumB)

        // Valid live manifest with matching checksum adopts activeDesign=1 and caches LKG
        val adopted = manager.validateAndAdopt(
            rawManifestJson = liveRaw,
            activeDesign = 1,
            expectedChecksum = checksumA,
            persistIfValid = true
        )
        assertFalse(adopted.usedFallback)
        assertEquals(1, adopted.activeDesign)
        assertEquals("live", adopted.slot)
        assertEquals("melvore_live_worlds_v2", adopted.manifest.manifestId)

        // Mismatched checksum triggers safe automatic rollback to valid LKG (activeDesign=1)
        val checksumMismatch = manager.validateAndAdopt(
            rawManifestJson = liveRaw,
            activeDesign = 1,
            expectedChecksum = "0000000000000000000000000000000000000000000000000000000000000000"
        )
        assertTrue(checksumMismatch.usedFallback)
        assertTrue(checksumMismatch.fallbackReason.orEmpty().contains("checksum", ignoreCase = true))
        assertEquals("melvore_live_worlds_v2", checksumMismatch.manifest.manifestId)

        // Manifest requiring min_app_schema_version 9.9.0 is rejected and rolled back cleanly
        val futureSchemaRaw = liveRaw.replace("\"min_app_schema_version\": \"1.0.0\"", "\"min_app_schema_version\": \"9.9.0\"")
        val futureOutcome = manager.validateAndAdopt(rawManifestJson = futureSchemaRaw, activeDesign = 1)
        assertTrue(futureOutcome.usedFallback)
        assertTrue(futureOutcome.fallbackReason.orEmpty().contains("min_app_schema_version"))

        // Corrupt JSON or invalid hex or missing pages.home falls back cleanly without crashing
        val corruptOutcome = manager.validateAndAdopt(rawManifestJson = "{ invalid_json", activeDesign = 1)
        assertTrue(corruptOutcome.usedFallback)

        val invalidHexRaw = liveRaw.replace("\"#FAF7F2\"", "\"NOT_HEX\"")
        val hexOutcome = manager.validateAndAdopt(rawManifestJson = invalidHexRaw, activeDesign = 1)
        assertTrue(hexOutcome.usedFallback)
        assertTrue(hexOutcome.fallbackReason.orEmpty().contains("Invalid hex", ignoreCase = true))

        // Clear LKG cache and ensure fresh manager falls back gracefully to bundled default (slot=default)
        store.saveLastKnownGoodManifest("", 0, "", "")
        val fallbackToBundled = manager.validateAndAdopt(rawManifestJson = "{ broken", activeDesign = 1)
        assertTrue(fallbackToBundled.usedFallback)
        assertEquals(0, fallbackToBundled.activeDesign)
        assertEquals("default", fallbackToBundled.slot)
        assertEquals("melvore_default_issue_v1", fallbackToBundled.manifest.manifestId)
    }

    @Test
    fun liveManifestV120ParsesAll12WorldKitsPixelArcadeAnd12OccasionKitsAndResolvesSvgUrls() {
        val liveRaw = resolveWorkspaceFile("php_api/design-system/live/manifest.json").readText()
        val defaultRaw = resolveWorkspaceFile("php_api/design-system/default/manifest.json").readText()

        val liveManifest = DesignManifestSupport.json.decodeFromString<DesignManifest>(liveRaw)
        val defaultManifest = DesignManifestSupport.json.decodeFromString<DesignManifest>(defaultRaw)

        // v1.2.0 metadata & counts
        assertEquals("melvore_live_worlds_v2", liveManifest.manifestId)
        assertEquals("1.2.0", liveManifest.designVersion)
        assertEquals(12, liveManifest.worlds.size)
        assertEquals(12, liveManifest.occasionOverrides.occasions.size)

        // Verify Pixel Arcade world kit parses completely
        val pixelOverride = liveManifest.worldOverride("pixel")
        assertNotNull("pixel world override must exist", pixelOverride)
        assertEquals("pixel_block", pixelOverride!!.buttonStyle)
        assertEquals(0, pixelOverride.cardRadiusDp)

        val pixelKit = pixelOverride.kit
        assertNotNull("pixel kit must parse", pixelKit)
        assertEquals("1.2.0", pixelKit!!.version)
        assertEquals("pixel_block", pixelKit.buttonSkin)
        assertEquals("Silkscreen", pixelKit.fonts)
        assertEquals("/design-system/assets/images/pixel-arcade-hero.svg", pixelKit.heroImage)
        assertEquals(true, pixelKit.typography?.pixelText)
        assertEquals("pixel", pixelKit.typography?.fontRoles?.heading)
        assertEquals("clean_sans", pixelKit.typography?.fontRoles?.body)
        assertEquals(listOf("START", "MAP", "TV", "VIP", "BAG"), pixelKit.tabLabels)
        assertEquals(8, pixelKit.budget?.maxAmbientParticles)
        assertEquals(5, pixelKit.icons.size)
        assertTrue(pixelKit.svg.button.endsWith("/worlds/pixel/button.svg"))
        assertTrue(pixelKit.svg.cardFrame.endsWith("/worlds/pixel/card_frame.svg"))
        assertTrue(pixelKit.svg.loader.endsWith("/worlds/pixel/loader.svg"))

        // Resolve Pixel Arcade theme and effective nav tabs with remote SVG icon URLs
        val pixelWorld = FashionWorldTheme(
            12,
            "pixel",
            "Pixel Arcade",
            mode = "dark",
            fontStyle = "pixel",
            backgroundColor = "#120B2E",
            accentColor = "#FFD23F"
        )
        val resolvedPixel = FashionWorldThemeResolver.resolveWithManifest(
            manifest = liveManifest,
            activeOccasion = null,
            selectedWorld = pixelWorld
        )
        assertTrue(resolvedPixel.isWorldActive)
        assertTrue(resolvedPixel.pixelText)
        assertEquals(0, resolvedPixel.cardRadiusDp)
        assertEquals("pixel_block", resolvedPixel.buttonStyle)
        assertEquals("#120B2E", resolvedPixel.backgroundHex)
        assertEquals(0, RemoteDesignRegistry.resolveProductCardRadiusDp(liveManifest, resolvedPixel))

        val effectiveTabs = RemoteDesignRegistry.resolveEffectiveNavTabs(
            manifest = liveManifest,
            worldTheme = resolvedPixel,
            apiBaseUrl = "https://app.melvore.co.in/"
        )
        assertEquals(listOf("START", "MAP", "TV", "VIP", "BAG"), effectiveTabs.map { it.label })
        effectiveTabs.forEach { tab ->
            assertNotNull("Icon SVG URL missing for ${tab.route}", tab.iconSvgUrl)
            assertNotNull("Active icon SVG URL missing for ${tab.route}", tab.activeIconSvgUrl)
            assertTrue(tab.iconSvgUrl!!.startsWith("https://app.melvore.co.in/design-system/assets/svg/worlds/pixel/icons/"))
            assertTrue(tab.activeIconSvgUrl!!.endsWith("_active.svg"))
        }

        // Asset URL resolver edge cases
        assertEquals(
            "https://app.melvore.co.in/design-system/assets/svg/worlds/pixel/button.svg",
            RemoteDesignRegistry.resolveAssetUrl("/design-system/assets/svg/worlds/pixel/button.svg", "https://app.melvore.co.in/")
        )
        assertEquals(
            "https://cdn.example.com/custom.svg",
            RemoteDesignRegistry.resolveAssetUrl("https://cdn.example.com/custom.svg", "https://app.melvore.co.in")
        )
        assertNull(RemoteDesignRegistry.resolveAssetUrl("   ", "https://app.melvore.co.in"))

        // All 12 occasion kits have all 5 SVG paths populated
        liveManifest.occasionOverrides.occasions.forEach { (slug, kit) ->
            assertTrue("Occasion $slug missing banner.svg", kit.svg.banner.endsWith("/banner.svg"))
            assertTrue("Occasion $slug missing button.svg", kit.svg.button.endsWith("/button.svg"))
            assertTrue("Occasion $slug missing button_ghost.svg", kit.svg.buttonGhost.endsWith("/button_ghost.svg"))
            assertTrue("Occasion $slug missing divider.svg", kit.svg.divider.endsWith("/divider.svg"))
            assertTrue("Occasion $slug missing corner.svg", kit.svg.corner.endsWith("/corner.svg"))
        }

        // Backward compatibility: v1.0.0 default manifest has null kits and empty occasions map
        assertTrue(defaultManifest.occasionOverrides.occasions.isEmpty())
        defaultManifest.worlds.values.forEach { worldOverride ->
            assertNull(worldOverride.kit)
        }
    }

    @Test
    fun productionServerDesignSyncMergesAll12WorldsAliasesOccasionsAndResolvesEverySvg() {
        val liveRaw = resolveWorkspaceFile("php_api/design-system/live/manifest.json").readText()
        val liveManifest = DesignManifestSupport.json.decodeFromString<DesignManifest>(liveRaw)

        // 1. Semver comparison for live server upgrades
        assertTrue(DesignManifestSupport.isVersionNewer("1.2.0", "1.1.0"))
        assertTrue(DesignManifestSupport.isVersionNewer("1.2.1", "1.2.0"))
        assertFalse(DesignManifestSupport.isVersionNewer("1.1.0", "1.2.0"))
        assertFalse(DesignManifestSupport.isVersionNewer("1.2.0", "1.2.0"))

        // 2. Occasion slug alias resolution maps DB slugs cleanly onto the 12 server SVG kits
        assertEquals("diya_flame", liveManifest.occasionOverrides.occasionKit("diwali-2026")?.motif)
        assertEquals("christmas_tree", liveManifest.occasionOverrides.occasionKit("holiday-gala-2026")?.motif)
        assertEquals("pumpkin", liveManifest.occasionOverrides.occasionKit("halloween-2026")?.motif)
        assertEquals("firework", liveManifest.occasionOverrides.occasionKit("new-year-2027")?.motif)
        assertEquals("heart_arrow", liveManifest.occasionOverrides.occasionKit("valentine-week")?.motif)
        assertEquals("snowflake", liveManifest.occasionOverrides.occasionKit("winter-solstice")?.motif)
        assertEquals("eid_lantern", liveManifest.occasionOverrides.occasionKit("eid-ul-fitr")?.motif)
        assertEquals("wedding_paisley", liveManifest.occasionOverrides.occasionKit("wedding-couture")?.motif)

        // 3. Merging a 2-world DB list with live manifest yields all 12 worlds while preserving DB IDs
        val partialDbWorlds = listOf(
            FashionWorldTheme(styleWorldId = 101, slug = "y2k", name = "Y2K Chrome DB", displayOrder = 1),
            FashionWorldTheme(styleWorldId = 102, slug = "goth", name = "Goth DB", displayOrder = 2)
        )
        val merged = com.example.data.DiscoveryRepository.mergeManifestWorlds(partialDbWorlds, liveManifest)
        assertEquals(12, merged.size)
        val y2kMerged = merged.first { it.slug == "y2k" }
        assertEquals(101, y2kMerged.styleWorldId)
        assertEquals("Y2K Chrome DB", y2kMerged.name)
        val gothMerged = merged.first { it.slug == "goth" }
        assertEquals(102, gothMerged.styleWorldId)
        assertEquals("/design-system/assets/images/goth-nocturne-hero.svg", gothMerged.coverImageUrl)

        val pixelMerged = merged.firstOrNull { it.slug == "pixel" }
        assertNotNull("Pixel world must be synthesized from live manifest", pixelMerged)
        assertEquals("/design-system/assets/images/pixel-arcade-hero.svg", pixelMerged!!.coverImageUrl)
        assertEquals("#FFD23F", pixelMerged.accentColor)
        assertEquals("pixel_block", pixelMerged.buttonStyle)

        // 4. Every one of the 12 world kits resolves all 9 SVG assets & 5x2 icon SVGs against production server
        val prodBase = "https://app.melvore.co.in"
        liveManifest.worlds.forEach { (slug, spec) ->
            val kit = spec.kit
            assertNotNull("World $slug must have a kit", kit)
            val allSvgPaths = listOf(
                kit!!.svg.button,
                kit.svg.buttonGhost,
                kit.svg.navbar,
                kit.svg.navIndicator,
                kit.svg.slider,
                kit.svg.pager,
                kit.svg.divider,
                kit.svg.cardFrame,
                kit.svg.loader
            )
            allSvgPaths.forEach { rel ->
                val resolved = RemoteDesignRegistry.resolveAssetUrl(rel, prodBase)
                assertNotNull("World $slug has blank SVG path", resolved)
                assertTrue("World $slug SVG URL must start with production server base: $resolved", resolved!!.startsWith("$prodBase/design-system/"))
            }
            assertEquals("World $slug must define 5 nav icon pairs", 5, kit.icons.size)
            kit.icons.values.forEach { pair ->
                assertTrue(RemoteDesignRegistry.resolveAssetUrl(pair.icon, prodBase)!!.startsWith("$prodBase/design-system/"))
                assertTrue(RemoteDesignRegistry.resolveAssetUrl(pair.activeIcon, prodBase)!!.endsWith("_active.svg"))
            }
        }
    }
}
