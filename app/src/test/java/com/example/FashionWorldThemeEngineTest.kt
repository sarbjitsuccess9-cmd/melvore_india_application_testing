package com.example

import androidx.compose.ui.graphics.Color
import com.example.data.DiscoveryRepository
import com.example.data.SecureSessionStore
import com.example.model.BootstrapConfigBundle
import com.example.model.FashionWorldContext
import com.example.model.FashionWorldTheme
import com.example.model.LocalPreferences
import com.example.model.OccasionTheme
import com.example.model.SmartOffer
import com.example.model.WorldMapOriginPin
import com.example.network.BootstrapDataDto
import com.example.network.DiscoveryMappers.toDomain
import com.example.network.MelvoreApiClient
import com.example.network.WorldMapOriginDto
import com.example.ui.theme.CampaignExperienceController
import com.example.ui.theme.FashionWorldThemeResolver
import com.example.ui.theme.FashionWorldTransitionController
import com.example.ui.theme.contrastOnColor
import com.example.ui.theme.parseHexColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FashionWorldThemeEngineTest {

    private val y2kWorld = FashionWorldTheme(
        styleWorldId = 1,
        slug = "y2k",
        name = "Y2K Chrome",
        themeKey = "y2k_chrome",
        mode = "dark",
        animationKey = "y2k_chrome",
        fontStyle = "futuristic",
        primaryColor = "#F472B6",
        accentColor = "#22D3EE",
        secondaryAccent = "#C084FC",
        backgroundColor = "#0F0A1F",
        surfaceColor = "#1B1233",
        textPrimary = "#F8FAFC",
        textSecondary = "#CBD5E1",
        ambientPreset = "pixel_chrome",
        audioTrackKey = "synth_dream",
        sfxProfile = "cyber"
    )

    private val gothWorld = FashionWorldTheme(
        styleWorldId = 2,
        slug = "goth",
        name = "Goth & Dark Romance",
        themeKey = "goth_noir",
        mode = "dark",
        animationKey = "noir_vignette",
        fontStyle = "gothic",
        primaryColor = "#F4F4F5",
        accentColor = "#DC2626",
        backgroundColor = "#070709",
        surfaceColor = "#111116",
        ambientPreset = "gothic_mist",
        audioTrackKey = "velvet_cello",
        sfxProfile = "velvet"
    )

    private val diwaliOccasion = OccasionTheme(
        occasionId = 10,
        slug = "diwali-glow",
        name = "Diwali Luminous Edit",
        motifKey = "diya_flame",
        primaryColor = "#D97706",
        accentColor = "#F59E0B",
        backgroundColor = "#FEF3C7",
        fontStyle = "editorial_serif"
    )

    @Test
    fun themeResolverEnforcesLayerPriorityAndLuxeIsolation() {
        // 1. Default layer
        val defaultResolved = FashionWorldThemeResolver.resolve()
        assertEquals("default", defaultResolved.sourceLayer)
        assertFalse(defaultResolved.isDark)
        assertFalse(defaultResolved.isWorldActive)
        assertFalse(defaultResolved.isLuxeIsolated)

        // 2. Occasion layer overlays default
        val occasionResolved = FashionWorldThemeResolver.resolve(activeOccasion = diwaliOccasion)
        assertEquals("occasion", occasionResolved.sourceLayer)
        assertEquals("diwali-glow", occasionResolved.occasionSlug)
        assertEquals("#F59E0B", occasionResolved.accentHex)

        // 3. Scroll preview overlays occasion temporarily without locking full world
        val scrollResolved = FashionWorldThemeResolver.resolve(
            activeOccasion = diwaliOccasion,
            scrollPreviewWorld = y2kWorld
        )
        assertEquals("scroll_preview", scrollResolved.sourceLayer)
        assertEquals("y2k", scrollResolved.worldSlug)
        assertTrue(scrollResolved.isScrollPreview)
        assertFalse(scrollResolved.isWorldActive)

        // 4. Selected Fashion World overrides scroll preview & occasion
        val selectedResolved = FashionWorldThemeResolver.resolve(
            activeOccasion = diwaliOccasion,
            scrollPreviewWorld = gothWorld,
            selectedWorld = y2kWorld
        )
        assertEquals("fashion_world", selectedResolved.sourceLayer)
        assertEquals("y2k", selectedResolved.worldSlug)
        assertTrue(selectedResolved.isWorldActive)
        assertTrue(selectedResolved.isDark)
        assertEquals("synth_dream", selectedResolved.audioTrackKey)

        // 5. Luxe Isolation overrides ANY active world
        val luxeResolved = FashionWorldThemeResolver.resolve(
            activeOccasion = diwaliOccasion,
            selectedWorld = y2kWorld,
            isLuxeRoute = true
        )
        assertEquals("luxe_isolated", luxeResolved.sourceLayer)
        assertTrue(luxeResolved.isLuxeIsolated)
        assertFalse(luxeResolved.isWorldActive)
        assertEquals("#050507", luxeResolved.backgroundHex)

        // 6. Checkout/Auth stays neutral readable light mode
        val checkoutResolved = FashionWorldThemeResolver.resolve(
            activeOccasion = diwaliOccasion,
            selectedWorld = gothWorld,
            isNeutralCheckoutOrAuthRoute = true
        )
        assertEquals("default", checkoutResolved.sourceLayer)
        assertFalse(checkoutResolved.isWorldActive)
        assertFalse(checkoutResolved.isDark)
    }

    @Test
    fun colorParsingAndContrastSelectionHandleMalformedAndExtremeColors() {
        val fallback = Color(0xFF123456)
        assertEquals(Color(0xFF1D4ED8), parseHexColor("#1D4ED8", fallback))
        assertEquals(Color(0x801D4ED8), parseHexColor("801D4ED8", fallback))
        assertEquals(fallback, parseHexColor("not-a-hex", fallback))
        assertEquals(fallback, parseHexColor(null, fallback))

        assertEquals(Color(0xFFFAFAFA), contrastOnColor(Color(0xFF070709)))
        assertEquals(Color(0xFF09090B), contrastOnColor(Color(0xFFFFF1F5)))
    }

    @Test
    fun navigationContextPersistsWorldAcrossProductAndShopThenExitsOnHomeOrLuxe() {
        val initial = FashionWorldContext()
        val inY2k = FashionWorldTransitionController.enterWorld(initial, "y2k", worldId = 1)
        assertTrue(inY2k.isInsideDedicatedWorld)
        assertEquals("y2k", inY2k.activeWorldSlug)

        // Persist across browse funnel routes
        val onProduct = FashionWorldTransitionController.onRouteChanged(inY2k, "ProductDetailsRoute")
        assertEquals("y2k", onProduct.activeWorldSlug)

        val onShop = FashionWorldTransitionController.onRouteChanged(onProduct, "ShopRoute")
        assertEquals("y2k", onShop.activeWorldSlug)

        val onBrand = FashionWorldTransitionController.onRouteChanged(onShop, "BrandDetailRoute")
        assertEquals("y2k", onBrand.activeWorldSlug)

        // Entering Elite / Luxe strictly isolates & clears world context
        val onLuxe = FashionWorldTransitionController.onRouteChanged(onBrand, "EliteRoute")
        assertFalse(onLuxe.isInsideDedicatedWorld)
        assertNull(onLuxe.activeWorldSlug)

        // Returning to Home exits dedicated world cleanly
        val reEntered = FashionWorldTransitionController.enterWorld(initial, "goth", worldId = 2)
        val onHome = FashionWorldTransitionController.onRouteChanged(reEntered, "HomeRoute")
        assertFalse(onHome.isInsideDedicatedWorld)
        assertNull(onHome.activeWorldSlug)

        // Scroll preview cannot overwrite an active dedicated world
        val blockedPreview = FashionWorldTransitionController.updateScrollPreview(reEntered, "coquette")
        assertNull(blockedPreview.scrollPreviewWorldSlug)
        assertEquals("goth", blockedPreview.activeWorldSlug)
    }

    @Test
    fun smartOfferControllerRespectsWorldSpecificityThresholdsCapsAndSuppressedRoutes() {
        val context = RuntimeEnvironment.getApplication()
        val store = SecureSessionStore(context)
        store.clearOfferStatesForTest()

        val generalOffer = SmartOffer(
            offerId = 1,
            offerKey = "welcome_10",
            title = "10% Off First Drop",
            couponCode = "MELVORE10",
            minCartValue = 0.0,
            maxImpressionsPerSession = 2,
            cooldownHours = 12,
            priority = 10
        )
        val y2kOffer = SmartOffer(
            offerId = 2,
            offerKey = "y2k_bundle",
            title = "Y2K Chrome Bonus",
            couponCode = "Y2K15",
            worldSlug = "y2k",
            minCartValue = 1500.0,
            maxImpressionsPerSession = 1,
            cooldownHours = 24,
            priority = 5
        )

        // Below cart minimum -> falls back to generalOffer
        val lowCartMatch = CampaignExperienceController.selectEligibleOffer(
            offers = listOf(generalOffer, y2kOffer),
            activeWorldSlug = "y2k",
            activeOccasionSlug = null,
            cartSubtotal = 999.0,
            simpleRouteName = "StyleWorldRoute",
            shownInSessionById = emptyMap(),
            canShowByPersistence = { o, count -> store.canShowOffer(o.offerId, o.maxImpressionsPerSession, count) }
        )
        assertEquals(1, lowCartMatch?.offerId)

        // Above cart minimum -> selects world-specific offer over general offer
        val highCartMatch = CampaignExperienceController.selectEligibleOffer(
            offers = listOf(generalOffer, y2kOffer),
            activeWorldSlug = "y2k",
            activeOccasionSlug = null,
            cartSubtotal = 2000.0,
            simpleRouteName = "ProductDetailsRoute",
            shownInSessionById = emptyMap(),
            canShowByPersistence = { o, count -> store.canShowOffer(o.offerId, o.maxImpressionsPerSession, count) }
        )
        assertEquals(2, highCartMatch?.offerId)

        // Checkout / Auth suppress all offers cleanly
        assertNull(
            CampaignExperienceController.selectEligibleOffer(
                offers = listOf(generalOffer, y2kOffer),
                activeWorldSlug = "y2k",
                activeOccasionSlug = null,
                cartSubtotal = 2000.0,
                simpleRouteName = "CheckoutRoute",
                shownInSessionById = emptyMap(),
                canShowByPersistence = { o, count -> store.canShowOffer(o.offerId, o.maxImpressionsPerSession, count) }
            )
        )
        assertNull(
            CampaignExperienceController.selectEligibleOffer(
                offers = listOf(generalOffer, y2kOffer),
                activeWorldSlug = "y2k",
                activeOccasionSlug = null,
                cartSubtotal = 2000.0,
                simpleRouteName = "AuthRoute",
                shownInSessionById = emptyMap(),
                canShowByPersistence = { o, count -> store.canShowOffer(o.offerId, o.maxImpressionsPerSession, count) }
            )
        )

        // Recording cooldown prevents re-showing offer within cooldown window
        store.recordOfferCooldown(y2kOffer.offerId, cooldownHours = 24)
        assertFalse(store.canShowOffer(y2kOffer.offerId, maxPerSession = 2, sessionShownCount = 0))
        store.clearOfferStatesForTest()
    }

    @Test
    fun offlineBootstrapRoundtripAndVerifiedMapOriginFilteringAndAudioDefaultsWorkCleanly() {
        val context = RuntimeEnvironment.getApplication()
        val store = SecureSessionStore(context)
        val apiClient = MelvoreApiClient(store)
        val repo = DiscoveryRepository(context, apiClient, store)

        // Sound strictly OFF by default until user explicitly consents
        val defaultPrefs = LocalPreferences()
        assertFalse(defaultPrefs.ambientAudioEnabled)
        assertFalse(defaultPrefs.uiSfxEnabled)
        assertFalse(defaultPrefs.reducedMotion)
        assertFalse(defaultPrefs.audioConsentAnswered)

        // Offline fallback provides non-empty curated editorial worlds
        val offlineBundle = repo.defaultOfflineBootstrap()
        assertTrue(offlineBundle.fashionWorlds.size >= 5)

        // Bootstrap DTO mapper strictly drops unverified pins
        val bootstrapDomain = BootstrapDataDto(
            configVersion = "dto-v1",
            worldMapOrigins = listOf(
                WorldMapOriginDto(
                    originId = 1,
                    brandId = 10,
                    brandName = "Seoul Studio",
                    brandSlug = "seoul-studio",
                    city = "Seoul",
                    country = "South Korea",
                    isVerified = true
                ),
                WorldMapOriginDto(
                    originId = 2,
                    brandId = 11,
                    brandName = "Unverified Placeholder",
                    brandSlug = "unverified-placeholder",
                    city = "Nowhere",
                    country = "Test",
                    isVerified = false
                )
            )
        ).toDomain()
        assertEquals(1, bootstrapDomain.worldMapOrigins.size)
        assertEquals("Seoul Studio", bootstrapDomain.worldMapOrigins.first().brandName)

        // Bootstrap cache roundtrip preserves config version and verified pins only
        val bundleToCache = BootstrapConfigBundle(
            configVersion = "test-v42",
            fashionWorlds = listOf(y2kWorld),
            worldMapOrigins = listOf(
                WorldMapOriginPin(originId = 1, brandId = 10, brandName = "Verified House", city = "Tokyo", isVerified = true),
                WorldMapOriginPin(originId = 2, brandId = 11, brandName = "Hidden House", city = "Paris", isVerified = false)
            )
        )
        val serialized = apiClient.json.encodeToString(BootstrapConfigBundle.serializer(), bundleToCache)
        store.saveBootstrapConfigJson("test-v42", serialized)
        val cached = repo.loadCachedBootstrap()
        assertNotNull(cached)
        assertEquals("test-v42", cached?.configVersion)
        assertEquals(1, cached?.worldMapOrigins?.size)
        assertEquals("Verified House", cached?.worldMapOrigins?.first()?.brandName)
    }
}
