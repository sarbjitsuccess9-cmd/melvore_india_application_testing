package com.example

import com.example.data.SecureSessionStore
import com.example.network.BootstrapDataDto
import com.example.network.CategoryDto
import com.example.network.DiscoveryMappers.toDomain
import com.example.network.DtoMappers.toDomain
import com.example.network.ElitePackItemDto
import com.example.network.ForYouDataDto
import com.example.network.ProductDto
import com.example.network.ProductRecommendationsDataDto
import com.example.network.StyleWorldFeedbackResponseDataDto
import com.example.ui.components.shouldShowVibeSatisfactionSection
import com.example.ui.effects.shouldPlayWorldAmbientAudio
import com.example.ui.theme.resolveWorldButtonStyle
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImmersiveWorldsUpgradeTest {

    private lateinit var store: SecureSessionStore
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Before
    fun setUp() {
        val ctx = RuntimeEnvironment.getApplication()
        store = SecureSessionStore(ctx)
        store.clearAuthToken()
    }

    @Test
    fun vibeSatisfactionHidesOnlyAfterConfirmedSubmissionAndIsolatesActors() {
        // Never hide merely from visiting or opening; show until confirmed submission
        assertTrue(shouldShowVibeSatisfactionSection(hasSubmittedBackendOrLocal = false, justUnlockedResult = null))

        // After submission returns coupon, keep showing reward unlock banner while justUnlockedResult != null
        val unlocked = json.decodeFromString(
            StyleWorldFeedbackResponseDataDto.serializer(),
            """
            {
              "has_submitted": true,
              "already_submitted": false,
              "submitted_world_ids": [1],
              "submitted_world_slugs": ["y2k"],
              "feedback": {
                "feedback_id": 9,
                "style_world_id": 1,
                "world_slug": "y2k",
                "satisfaction_score": 4,
                "feedback_label": "Absolutely obsessed!",
                "coupon_code": "MELVOREY2K15"
              },
              "coupon": {
                "coupon_id": 22,
                "code": "MELVOREY2K15",
                "discount_type": "percentage",
                "discount_value": 15.0,
                "discount_summary": "15% OFF",
                "conditions_text": "Valid on orders above ₹999"
              }
            }
            """.trimIndent()
        ).toDomain()

        assertTrue(shouldShowVibeSatisfactionSection(hasSubmittedBackendOrLocal = true, justUnlockedResult = unlocked))
        // Once reward card dismissed on subsequent visits, questionnaire remains hidden permanently
        assertFalse(shouldShowVibeSatisfactionSection(hasSubmittedBackendOrLocal = true, justUnlockedResult = null))

        // Actor-scoped SecureSessionStore persistence
        store.recordSubmittedFeedbackWorld(worldId = 1, worldSlug = "y2k")
        assertTrue(store.hasSubmittedFeedbackWorld(1, "y2k"))
        assertFalse(store.hasSubmittedFeedbackWorld(2, "goth"))

        // Logging in as authenticated account isolates feedback flags by actor
        store.saveAuthToken("jwt_token_user_42")
        assertFalse(store.hasSubmittedFeedbackWorld(1, "y2k"))
        store.syncSubmittedWorldsFromBootstrap(worldIds = listOf(2), worldSlugs = listOf("goth"))
        assertTrue(store.hasSubmittedFeedbackWorld(2, "goth"))
        assertFalse(store.hasSubmittedFeedbackWorld(1, "y2k"))
    }

    @Test
    fun ambientAudioGatingRespectsConsentGlobalMuteBackgroundAndVideoAudio() {
        // 1. Blocked before consent
        assertFalse(
            shouldPlayWorldAmbientAudio(
                consentAnswered = false,
                ambientAudioEnabled = true,
                isWorldOrOccasionActive = true,
                isAppForeground = true,
                isVideoPlayingAudio = false,
                audioUrl = "https://cdn.melvore.co.in/audio/y2k.mp3",
                audioTrackKey = "synth_dream"
            )
        )

        // 2. Global mute respected across worlds (even when track/url present)
        assertFalse(
            shouldPlayWorldAmbientAudio(
                consentAnswered = true,
                ambientAudioEnabled = false,
                isWorldOrOccasionActive = true,
                isAppForeground = true,
                isVideoPlayingAudio = false,
                audioUrl = "https://cdn.melvore.co.in/audio/goth.mp3",
                audioTrackKey = "velvet_cello"
            )
        )

        // 3. Pauses in background
        assertFalse(
            shouldPlayWorldAmbientAudio(
                consentAnswered = true,
                ambientAudioEnabled = true,
                isWorldOrOccasionActive = true,
                isAppForeground = false,
                isVideoPlayingAudio = false,
                audioUrl = "https://cdn.melvore.co.in/audio/y2k.mp3",
                audioTrackKey = "synth_dream"
            )
        )

        // 4. Pauses while unmuted video plays audio
        assertFalse(
            shouldPlayWorldAmbientAudio(
                consentAnswered = true,
                ambientAudioEnabled = true,
                isWorldOrOccasionActive = true,
                isAppForeground = true,
                isVideoPlayingAudio = true,
                audioUrl = "https://cdn.melvore.co.in/audio/y2k.mp3",
                audioTrackKey = "synth_dream"
            )
        )

        // 5. Plays when all conditions hold (either DB audioUrl or procedural trackKey)
        assertTrue(
            shouldPlayWorldAmbientAudio(
                consentAnswered = true,
                ambientAudioEnabled = true,
                isWorldOrOccasionActive = true,
                isAppForeground = true,
                isVideoPlayingAudio = false,
                audioUrl = "https://cdn.melvore.co.in/audio/y2k.mp3",
                audioTrackKey = "none"
            )
        )
        assertTrue(
            shouldPlayWorldAmbientAudio(
                consentAnswered = true,
                ambientAudioEnabled = true,
                isWorldOrOccasionActive = true,
                isAppForeground = true,
                isVideoPlayingAudio = false,
                audioUrl = null,
                audioTrackKey = "synth_dream"
            )
        )
    }

    @Test
    fun worldAndOccasionButtonStylesResolveAccurately() {
        assertEquals(
            "y2k_pixel_chrome",
            resolveWorldButtonStyle("y2k_pixel_chrome", "y2k", "diwali_gold", "diwali-glow", isWorldActive = true)
        )
        assertEquals(
            "goth_fang",
            resolveWorldButtonStyle("sharp", "goth", "", "", isWorldActive = true)
        )
        assertEquals(
            "coquette_ribbon",
            resolveWorldButtonStyle("editorial_pill", "coquette", "", "", isWorldActive = true)
        )
        assertEquals(
            "streetwear_utility",
            resolveWorldButtonStyle("sharp", "streetwear", "", "", isWorldActive = true)
        )
        assertEquals(
            "kfashion_minimal",
            resolveWorldButtonStyle("sharp", "k-fashion", "", "", isWorldActive = true)
        )
        assertEquals(
            "halloween_pumpkin",
            resolveWorldButtonStyle("", null, "halloween_pumpkin", "halloween-noir", isWorldActive = false)
        )
        assertEquals(
            "diwali_gold",
            resolveWorldButtonStyle("", null, "", "diwali-luminous", isWorldActive = false)
        )
        assertEquals(
            "default_minimal",
            resolveWorldButtonStyle("", null, "", null, isWorldActive = false)
        )
    }

    @Test
    fun elitePackItemPreservesProductImageAndNeverUsesCollectionBannerOverride() {
        val dto = ElitePackItemDto(
            itemId = 77,
            itemType = "product",
            title = "Nocturne Corset Blazer",
            imageUrl = "https://cdn.melvore.co.in/banners/luxe_hero_banner.jpg",
            product = ProductDto(
                productId = 501,
                name = "Nocturne Corset Blazer",
                slug = "nocturne-corset-blazer",
                primaryImage = "https://cdn.melvore.co.in/products/corset_blazer.jpg"
            )
        )
        val mapped = dto.toDomain()
        assertNotNull(mapped.product)
        assertEquals("https://cdn.melvore.co.in/products/corset_blazer.jpg", mapped.imageUrl)
        assertEquals("https://cdn.melvore.co.in/products/corset_blazer.jpg", mapped.product?.imageUrl)
    }

    @Test
    fun categoryAndRecommendationDtosDeserializeAndMapCleanly() {
        val catDto = json.decodeFromString(
            CategoryDto.serializer(),
            """
            {
              "category_id": 12,
              "name": "Jackets",
              "slug": "jackets",
              "cover_image_url": "https://cdn.melvore.co.in/cats/jackets.jpg",
              "motif_key": "leather_stitch",
              "editorial_badge": "HERO OUTERWEAR",
              "editorial_subtitle": "Biker & Moto",
              "accent_hex": "#DC2626",
              "bg_hex": "#181214",
              "tile_span": "wide",
              "display_order": 2,
              "product_count": 18
            }
            """.trimIndent()
        ).toDomain()

        assertEquals("HERO OUTERWEAR", catDto.editorialBadge)
        assertEquals("leather_stitch", catDto.motifKey)
        assertEquals("wide", catDto.tileSpan)
        assertEquals(18, catDto.itemCount)

        val recs = json.decodeFromString(
            ProductRecommendationsDataDto.serializer(),
            """
            {
              "world_slug": "y2k",
              "similar_products": [
                { "id": 101, "name": "Chrome Micro Skirt", "slug": "chrome-micro-skirt", "regular_price": 1499.0 }
              ],
              "more_from_brand": []
            }
            """.trimIndent()
        )
        assertEquals("y2k", recs.worldSlug)
        assertEquals(1, recs.similarProducts.size)

        val forYou = json.decodeFromString(
            ForYouDataDto.serializer(),
            """
            {
              "is_personalized": true,
              "title": "Curated For You",
              "preferred_worlds": [
                { "style_world_id": 1, "slug": "y2k", "name": "Y2K Chrome" }
              ],
              "preferred_categories": [
                { "category_id": 12, "name": "Jackets", "slug": "jackets", "affinity_score": 18.5 }
              ],
              "preferred_styles": ["y2k", "streetwear"],
              "recently_explored_worlds": [
                { "style_world_id": 2, "slug": "goth", "name": "Goth Noir" }
              ]
            }
            """.trimIndent()
        ).toDomain()
        assertEquals(1, forYou.preferredWorlds.size)
        assertEquals("Jackets", forYou.preferredCategories.first().name)
        assertEquals(listOf("y2k", "streetwear"), forYou.preferredStyles)

        val bootstrap = json.decodeFromString(
            BootstrapDataDto.serializer(),
            """
            {
              "config_version": "v4",
              "submitted_world_ids": [1, 3],
              "submitted_world_slugs": ["y2k", "coquette"]
            }
            """.trimIndent()
        ).toDomain()
        assertEquals(listOf(1, 3), bootstrap.submittedWorldIds)
        assertEquals(listOf("y2k", "coquette"), bootstrap.submittedWorldSlugs)
    }

    @Test
    fun runwayConfigDisabledByDefaultAndRespectsToggle() {
        val disabledDto = com.example.network.RunwayConfigDto(enabled = false, title = "Y2K Edit", speed = 1.4f)
        val disabled = disabledDto.toDomain()
        assertFalse(disabled.enabled)
        assertEquals(1.4f, disabled.speed)

        val enabledDto = com.example.network.RunwayConfigDto(enabled = true, title = "Y2K Chrome", speed = 1.2f)
        val enabled = enabledDto.toDomain()
        assertTrue(enabled.enabled)
        assertEquals("Y2K Chrome", enabled.title)
        assertEquals(1.2f, enabled.speed)
    }
}
