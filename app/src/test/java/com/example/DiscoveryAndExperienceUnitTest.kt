package com.example

import com.example.model.ColorVariant
import com.example.model.LocalPreferences
import com.example.model.Product
import com.example.model.ProductVariation
import com.example.model.SearchFilterState
import com.example.model.VideoReel
import com.example.model.requiresVariantSelection
import com.example.network.BrandProfileDto
import com.example.network.CampaignDto
import com.example.network.DeliveryCheckDataDto
import com.example.network.DiscoveryMappers.toDomain
import com.example.network.EliteHomeDataDto
import com.example.network.ElitePackDetailDataDto
import com.example.network.ElitePackDto
import com.example.network.ElitePackItemDto
import com.example.network.FeedDiagnosticsDto
import com.example.network.HelpFlowDataDto
import com.example.network.HelpFlowDto
import com.example.network.HelpFlowNodeDto
import com.example.network.HelpFlowOptionDto
import com.example.network.HelpTopicDto
import com.example.network.HomeEditorialStoryDto
import com.example.network.HomeFeedDataDto
import com.example.network.HomePromoBannerDto
import com.example.network.HomeSectionDto
import com.example.network.HomeSectionItemDto
import com.example.network.HomeSiteSettingsDto
import com.example.network.PaginationDto
import com.example.network.ProductDto
import com.example.network.ProductVideoMediaDto
import com.example.network.ReviewDto
import com.example.network.ReviewEligibilityItemDto
import com.example.network.ReviewListDataDto
import com.example.network.ReviewMediaDto
import com.example.network.StyleWorldDto
import com.example.network.VideoDiagnosticsDto
import com.example.network.VideoReelDto
import com.example.network.VideosFeedDataDto
import com.example.ui.components.MelvoreAnimationKey
import com.example.ui.theme.MelvoreBrandFontFamily
import com.example.ui.theme.MelvoreFuturisticFontFamily
import com.example.ui.theme.MelvoreGothicFontFamily
import com.example.ui.theme.MelvorePixelFontFamily
import com.example.ui.theme.MelvoreSerifFontFamily
import com.example.ui.theme.fontFamilyForStyleKey
import com.example.ui.theme.styleWorldVisualSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryAndExperienceUnitTest {

    @Test
    fun animationAllowlistSafelyMapsKnownKeysAndDegradesUnknownToNone() {
        assertEquals(MelvoreAnimationKey.DIWALI_GLOW, MelvoreAnimationKey.fromBackendKey("diwali_glow"))
        assertEquals(MelvoreAnimationKey.Y2K_CHROME, MelvoreAnimationKey.fromBackendKey("y2k-chrome"))
        assertEquals(MelvoreAnimationKey.METALLIC_SHINE, MelvoreAnimationKey.fromBackendKey(" METALLIC_SHINE "))
        assertEquals(MelvoreAnimationKey.NONE, MelvoreAnimationKey.fromBackendKey("<script>alert(1)</script>"))
        assertEquals(MelvoreAnimationKey.NONE, MelvoreAnimationKey.fromBackendKey(null))
    }

    @Test
    fun defaultPreferencesUseLightStorefrontTheme() {
        val prefs = LocalPreferences()
        assertEquals("light", prefs.themeMode)
        assertEquals("₹", prefs.currencyOverride)
    }

    @Test
    fun requiresVariantSelectionDetectsMultiOptionAndVariations() {
        val singleItem = Product(
            id = 1,
            name = "Minimal Scarf",
            slug = "minimal-scarf",
            category = "Accessories",
            regularPrice = 999.0,
            colors = listOf(ColorVariant("Black", "#000000")),
            sizes = listOf("One Size")
        )
        assertFalse(requiresVariantSelection(singleItem))

        val multiSize = singleItem.copy(sizes = listOf("S", "M", "L"))
        assertTrue(requiresVariantSelection(multiSize))

        val multiColor = singleItem.copy(
            colors = listOf(ColorVariant("Black", "#000000"), ColorVariant("Ivory", "#F8F8F6"))
        )
        assertTrue(requiresVariantSelection(multiColor))

        val withVariations = singleItem.copy(
            variations = listOf(ProductVariation(id = 10, color = "Black", size = "M", stockQuantity = 4))
        )
        assertTrue(requiresVariantSelection(withVariations))
    }

    @Test
    fun fontFamilyMapsPerStyleUniverseKey() {
        assertEquals(MelvoreFuturisticFontFamily, fontFamilyForStyleKey("y2k"))
        assertEquals(MelvoreGothicFontFamily, fontFamilyForStyleKey("gothic"))
        assertEquals(MelvoreSerifFontFamily, fontFamilyForStyleKey("coquette"))
    }

    @Test
    fun searchFilterStateTracksActiveFilterCount() {
        val clean = SearchFilterState()
        assertEquals(0, clean.activeCount)

        val filtered = SearchFilterState(
            categorySlug = "jackets",
            size = "M",
            maxPrice = 2499.0,
            sort = "price_asc"
        )
        assertEquals(4, filtered.activeCount)
    }

    @Test
    fun homeFeedMapperMapsCampaignsSectionsAndSponsoredExpressProducts() {
        val dto = HomeFeedDataDto(
            activeCampaign = CampaignDto(
                campaignId = 9,
                slug = "diwali-lights-2026",
                title = "Diwali Edit",
                animationKey = "diwali_glow",
                ctaText = "Explore Festival"
            ),
            sections = listOf(
                HomeSectionDto(
                    sectionId = 1,
                    sectionKey = "aesthetic_worlds",
                    title = "Shop by Vibe",
                    items = listOf(
                        HomeSectionItemDto(
                            itemId = 10,
                            title = "Korean Minimal",
                            targetType = "style_world",
                            targetValue = "korean-minimal"
                        )
                    )
                )
            ),
            discoveryProducts = listOf(
                ProductDto(
                    productId = 404,
                    name = "Obsidian Moto Jacket",
                    regularPrice = 4999.0,
                    salePrice = 3499.0,
                    isSponsored = true,
                    expressDelivery = true,
                    videos = listOf(
                        ProductVideoMediaDto(url = "assets/uploads/video/moto.mp4")
                    )
                )
            )
        )

        val domain = dto.toDomain()
        assertEquals("Diwali Edit", domain.activeCampaign?.title)
        assertEquals(1, domain.sections.size)
        assertEquals("korean-minimal", domain.sections.first().items.first().targetValue)

        val prod = domain.discoveryProducts.first()
        assertEquals(404, prod.id)
        assertTrue(prod.isSponsored)
        assertTrue(prod.expressDelivery)
        assertTrue(prod.videoUrls.first().startsWith("https://"))
    }

    @Test
    fun homeFeedMapperMapsEnrichedStorefrontShelvesBannersAndSettings() {
        val sampleProd = ProductDto(productId = 101, name = "Cropped Tee", regularPrice = 899.0)
        val dto = HomeFeedDataDto(
            promoBanners = listOf(
                HomePromoBannerDto(
                    id = 2,
                    title = "Complimentary Express Shipping",
                    badgeText = "MEMBER PERK",
                    ctaText = "Shop New",
                    ctaLink = "/shop.php?collection=new-arrivals"
                )
            ),
            siteSettings = HomeSiteSettingsDto(freeShippingMinimum = 1999.0),
            brands = listOf(BrandProfileDto(brandId = 8, name = "Koma Seoul", slug = "koma-seoul")),
            styleWorlds = listOf(StyleWorldDto(styleWorldId = 5, slug = "k-fashion", name = "K-Fashion")),
            newArrivals = listOf(sampleProd),
            bestsellers = listOf(sampleProd),
            trendingProducts = listOf(sampleProd),
            mostWishlisted = listOf(sampleProd),
            budget999 = listOf(sampleProd),
            budget1999 = listOf(sampleProd),
            sponsoredProducts = listOf(sampleProd),
            recentlyViewed = listOf(sampleProd),
            editorialStories = listOf(
                HomeEditorialStoryDto(id = 3, title = "Winter Layering Notes", slug = "winter-layering-notes", excerpt = "Sharp wool and leather pairings")
            ),
            previewVideos = listOf(VideoReelDto(videoId = 14, title = "Seoul Runway Look", videoUrl = "uploads/v.mp4"))
        )

        val domain = dto.toDomain()
        assertEquals(1, domain.promoBanners.size)
        assertEquals(1999.0, domain.freeShippingMinimum, 0.01)
        assertEquals("Koma Seoul", domain.brands.first().name)
        assertEquals("k-fashion", domain.styleWorlds.first().slug)
        assertEquals(1, domain.newArrivals.size)
        assertEquals(1, domain.bestsellers.size)
        assertEquals(1, domain.trendingProducts.size)
        assertEquals(1, domain.mostWishlisted.size)
        assertEquals(1, domain.budget999.size)
        assertEquals(1, domain.budget1999.size)
        assertEquals(1, domain.sponsoredProducts.size)
        assertEquals(1, domain.recentlyViewed.size)
        assertEquals("Winter Layering Notes", domain.editorialStories.first().title)
        assertEquals(1, domain.previewVideos.size)
    }

    @Test
    fun eliteStyleVideoReviewAndHelpMappersConvertCleanly() {
        val elite = EliteHomeDataDto(
            heroPacks = listOf(
                ElitePackDto(
                    packId = 5,
                    brandName = "DIOR",
                    title = "Noir Tailoring",
                    isHero = true,
                    isSponsored = true
                )
            ),
            packs = listOf(
                ElitePackDto(
                    packId = 6,
                    brandName = "HERMES",
                    title = "Silk & Saddle",
                    items = listOf(
                        ElitePackItemDto(
                            itemId = 11,
                            title = "Carré 90",
                            brand = BrandProfileDto(brandId = 2, name = "Hermes", slug = "hermes")
                        )
                    )
                )
            )
        ).toDomain()
        assertEquals(1, elite.heroPacks.size)
        assertEquals("HERMES", elite.packs.first().brandName)

        val style = StyleWorldDto(
            styleWorldId = 3,
            slug = "y2k-chrome",
            name = "Y2K Chrome",
            fontStyle = "y2k",
            animationKey = "y2k_chrome"
        ).toDomain()
        assertEquals("y2k", style.fontStyle)

        val reel = VideoReelDto(
            videoId = 22,
            title = "Tokyo Drop",
            videoUrl = "uploads/videos/tokyo.mp4",
            isSponsored = true,
            isLiked = true,
            likesCount = 128
        ).toDomain()
        assertTrue(reel.isLiked)
        assertEquals(128, reel.likesCount)

        val reviews = ReviewListDataDto(
            reviews = listOf(
                ReviewDto(
                    reviewId = 71,
                    userName = "Aanya",
                    rating = 5,
                    title = "Crisp drape",
                    comment = "Matches studio lighting",
                    helpfulCount = 14,
                    hasVotedHelpful = true,
                    media = listOf(ReviewMediaDto(url = "uploads/reviews/rev1.jpg"))
                )
            ),
            eligibleOrderItems = listOf(
                ReviewEligibilityItemDto(
                    orderItemId = 900,
                    orderId = 300,
                    orderNumber = "MEL-2026-1001",
                    productName = "Structured Blazer"
                )
            ),
            pagination = PaginationDto(total = 1)
        ).toDomain()
        assertTrue(reviews.reviews.first().hasVotedHelpful)
        assertTrue(reviews.reviews.first().mediaUrls.first().startsWith("https://"))
        assertEquals(1, reviews.eligibleOrderItems.size)
        assertEquals(1, reviews.total)

        val helpDetail = helpFlowSample().toDomain()
        assertNotNull(helpDetail)
        assertNotNull(helpDetail?.flow)
        assertEquals(1, helpDetail?.flow?.nodes?.size)

        val service = DeliveryCheckDataDto(
            pincode = "110001",
            city = "New Delhi",
            state = "Delhi",
            isServiceable = true,
            expressAvailable = true,
            codAvailable = true
        ).toDomain()
        assertTrue(service.expressAvailable)
        assertEquals("New Delhi", service.city)
    }

    @Test
    fun videoReelValidatesPlayableStreamUrlsStrictly() {
        assertTrue(
            VideoReel(videoId = 1, videoUrl = "https://app.melvore.co.in/uploads/videos/look1.mp4").hasValidStreamUrl()
        )
        assertTrue(
            VideoReel(videoId = 2, videoUrl = "http://cdn.melvore.co.in/stream/reel.m3u8").hasValidStreamUrl()
        )
        assertFalse(VideoReel(videoId = 3, videoUrl = null).hasValidStreamUrl())
        assertFalse(VideoReel(videoId = 4, videoUrl = "   ").hasValidStreamUrl())
        assertFalse(VideoReel(videoId = 5, videoUrl = "uploads/relative/look.mp4").hasValidStreamUrl())
        assertFalse(VideoReel(videoId = 6, videoUrl = "https://app.melvore.co.in/index.php").hasValidStreamUrl())
        assertFalse(VideoReel(videoId = 7, videoUrl = "https://app.melvore.co.in/404.html").hasValidStreamUrl())
    }

    @Test
    fun videosFeedMapperPreservesPaginationHasMoreAndDiagnostics() {
        val pageDto = VideosFeedDataDto(
            videos = listOf(
                VideoReelDto(videoId = 91, title = "Leather Trench Motion", videoUrl = "uploads/videos/trench.mp4")
            ),
            pagination = PaginationDto(page = 1, totalPages = 3, total = 24),
            diagnostics = VideoDiagnosticsDto(
                tableExists = true,
                activeCount = 26,
                invalidUrlCount = 2,
                reason = "filtered_invalid_streams"
            )
        ).toDomain()

        assertEquals(1, pageDto.videos.size)
        assertTrue(pageDto.videos.first().hasValidStreamUrl())
        assertEquals(1, pageDto.page)
        assertEquals(3, pageDto.totalPages)
        assertTrue(pageDto.hasMore)
        assertTrue(pageDto.tableExists)
        assertEquals(2, pageDto.invalidUrlCount)
        assertEquals("filtered_invalid_streams", pageDto.diagnosticsReason)
    }

    @Test
    fun elitePackDetailMapperMapsStoryAndRelatedPacks() {
        val detail = ElitePackDetailDataDto(
            pack = ElitePackDto(
                packId = 12,
                slug = "leather-atelier",
                brandName = "MELVORE LUXE",
                title = "The Leather Atelier",
                story = "Full-grain structured outerwear crafted for evening silhouettes."
            ),
            products = listOf(ProductDto(productId = 501, name = "Sculpted Leather Coat", regularPrice = 8999.0)),
            relatedPacks = listOf(
                ElitePackDto(packId = 13, slug = "after-dark-archive", title = "After Dark Archive")
            )
        ).toDomain()

        assertNotNull(detail)
        assertEquals("Full-grain structured outerwear crafted for evening silhouettes.", detail?.pack?.story)
        assertEquals(1, detail?.products?.size)
        assertEquals(1, detail?.relatedPacks?.size)
        assertEquals("after-dark-archive", detail?.relatedPacks?.first()?.slug)
    }

    @Test
    fun homeFeedDiagnosticsReportWarningsOnlyWhenTablesOrModulesMissing() {
        val clean = HomeFeedDataDto().toDomain()
        assertFalse(clean.diagnostics.hasWarnings)

        val warn = HomeFeedDataDto(
            diagnostics = FeedDiagnosticsDto(
                missingTables = listOf("app_videos"),
                emptyModules = listOf("promo_banners")
            )
        ).toDomain()
        assertTrue(warn.diagnostics.hasWarnings)
        assertEquals(listOf("app_videos"), warn.diagnostics.missingTables)
        assertEquals(listOf("promo_banners"), warn.diagnostics.emptyModules)
    }

    @Test
    fun styleWorldVisualSpecsProvideDistinctIdentitiesAcrossAesthetics() {
        val y2k = styleWorldVisualSpec("y2k-chrome")
        val goth = styleWorldVisualSpec("gothic-noir")
        val coquette = styleWorldVisualSpec("coquette-edit")
        val street = styleWorldVisualSpec("streetwear")
        val kFashion = styleWorldVisualSpec("k-fashion")

        assertEquals(MelvorePixelFontFamily, y2k.fontFamily)
        assertEquals(MelvoreGothicFontFamily, goth.fontFamily)
        assertEquals(MelvoreSerifFontFamily, coquette.fontFamily)
        assertEquals(MelvoreFuturisticFontFamily, street.fontFamily)
        assertEquals(MelvoreBrandFontFamily, kFashion.fontFamily)

        assertNotEquals(y2k.containerColor, coquette.containerColor)
        assertNotEquals(goth.containerColor, kFashion.containerColor)
        assertNotEquals(y2k.vibeLabel, goth.vibeLabel)
        assertNotEquals(coquette.vibeLabel, street.vibeLabel)
    }

    private fun helpFlowSample(): HelpFlowDataDto = HelpFlowDataDto(
        topic = HelpTopicDto(
            topicId = 4,
            slug = "damaged-item",
            title = "Damaged item received",
            requiresOrder = true
        ),
        flow = HelpFlowDto(
            flowId = 12,
            title = "Damaged Item Check",
            startNodeId = 100,
            nodes = listOf(
                HelpFlowNodeDto(
                    nodeId = 100,
                    title = "Inspect packaging",
                    requiresOrderSelect = true,
                    allowsPhotoUpload = true,
                    options = listOf(
                        HelpFlowOptionDto(
                            optionId = 1,
                            label = "Open replacement ticket",
                            escalateToTicket = true
                        )
                    )
                )
            )
        )
    )
}
