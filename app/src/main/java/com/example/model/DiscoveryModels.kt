package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class CampaignAsset(
    val assetId: Int = 0,
    val assetType: String = "image",
    val mediaUrl: String? = null,
    val mobileMediaUrl: String? = null,
    val animationKey: String = "none",
    val title: String = "",
    val subtitle: String = ""
)

data class CampaignItem(
    val campaignId: Int = 0,
    val slug: String = "",
    val title: String = "",
    val subtitle: String = "",
    val festivalKey: String = "seasonal",
    val animationKey: String = "none",
    val animationDurationMs: Int = 2400,
    val primaryColor: String = "#D4AF37",
    val accentColor: String = "#FFFFFF",
    val backgroundColor: String = "#0B0B0E",
    val bannerImageUrl: String? = null,
    val mobileBannerUrl: String? = null,
    val videoUrl: String? = null,
    val ctaText: String = "",
    val ctaLink: String = "",
    val assets: List<CampaignAsset> = emptyList()
)

data class HomeSectionItem(
    val itemId: Int = 0,
    val title: String = "",
    val subtitle: String = "",
    val badgeText: String = "",
    val imageUrl: String? = null,
    val mobileImageUrl: String? = null,
    val themeKey: String = "dark_editorial",
    val animationKey: String = "smooth_reveal",
    val targetType: String = "category",
    val targetValue: String = "",
    val targetId: Int? = null
)

data class HomeSection(
    val sectionId: Int = 0,
    val sectionKey: String = "",
    val sectionType: String = "product_shelf",
    val eyebrow: String = "",
    val title: String = "",
    val subtitle: String = "",
    val animationKey: String = "smooth_reveal",
    val themeKey: String = "dark_editorial",
    val ctaText: String = "",
    val ctaLink: String = "",
    val displayOrder: Int = 0,
    val items: List<HomeSectionItem> = emptyList(),
    val products: List<Product> = emptyList()
)

data class FeedDiagnostics(
    val missingTables: List<String> = emptyList(),
    val emptyModules: List<String> = emptyList()
) {
    val hasWarnings: Boolean get() = missingTables.isNotEmpty() || emptyModules.isNotEmpty()
}

data class HomeFeedBundle(
    val heroSlides: List<HeroSlide> = emptyList(),
    val activeCampaign: CampaignItem? = null,
    val campaigns: List<CampaignItem> = emptyList(),
    val promoBanners: List<PromoBanner> = emptyList(),
    val freeShippingMinimum: Double = 0.0,
    val categories: List<Category> = emptyList(),
    val brands: List<BrandProfileItem> = emptyList(),
    val styleWorlds: List<StyleWorld> = emptyList(),
    val sections: List<HomeSection> = emptyList(),
    val newArrivals: List<Product> = emptyList(),
    val bestsellers: List<Product> = emptyList(),
    val trendingProducts: List<Product> = emptyList(),
    val mostWishlisted: List<Product> = emptyList(),
    val budget999: List<Product> = emptyList(),
    val budget1999: List<Product> = emptyList(),
    val sponsoredProducts: List<Product> = emptyList(),
    val recentlyViewed: List<Product> = emptyList(),
    val editorialStories: List<BlogPost> = emptyList(),
    val previewVideos: List<VideoReel> = emptyList(),
    val discoveryProducts: List<Product> = emptyList(),
    val diagnostics: FeedDiagnostics = FeedDiagnostics()
)

data class BrandProfileItem(
    val brandId: Int,
    val name: String,
    val slug: String,
    val description: String = "",
    val tagline: String = "",
    val story: String = "",
    val logoUrl: String? = null,
    val coverImageUrl: String? = null,
    val mobileCoverUrl: String? = null,
    val brandVideoUrl: String? = null,
    val originCountry: String = "",
    val isFeatured: Boolean = false,
    val isSponsored: Boolean = false,
    val isNew: Boolean = false,
    val productCount: Int = 0
)

data class BrandDetailBundle(
    val brand: BrandProfileItem,
    val featuredProducts: List<Product> = emptyList(),
    val newArrivals: List<Product> = emptyList(),
    val bestsellers: List<Product> = emptyList(),
    val sponsoredProducts: List<Product> = emptyList()
)

@Serializable
data class RunwayModelSpec(
    val name: String = "",
    val role: String = "",
    val palette: List<String> = emptyList(),
    val styleNote: String = "",
    val silhouette: String = "cargo_baby_tee",
    val spriteUrl: String? = null
)

@Serializable
data class RunwayConfig(
    val enabled: Boolean = false,
    val title: String = "PIXEL RUNWAY SHOWCASE",
    val subtitle: String = "Live 8-bit chrome silhouettes styled from this universe",
    val animationPreset: String = "pixel_walk_loop",
    val speed: Float = 1.0f,
    val backgroundHex: String = "#160D2D",
    val displayPosition: String = "after_promo",
    val collectionSlug: String = "",
    val artworkUrl: String? = null,
    val models: List<RunwayModelSpec> = emptyList()
)

data class StyleWorldFeedback(
    val feedbackId: Int = 0,
    val styleWorldId: Int = 0,
    val worldSlug: String = "",
    val satisfactionScore: Int = 0,
    val feedbackLabel: String = "",
    val couponCode: String? = null,
    val submittedAt: String = ""
)

data class UnlockedCoupon(
    val couponId: Int = 0,
    val code: String = "",
    val discountType: String = "percentage",
    val discountValue: Double = 0.0,
    val discountSummary: String = "",
    val minOrderAmount: Double = 0.0,
    val maxDiscountAmount: Double? = null,
    val validTo: String? = null,
    val conditionsText: String = ""
)

data class StyleWorldSatisfactionStatus(
    val hasSubmitted: Boolean = false,
    val feedback: StyleWorldFeedback? = null
)

data class StyleWorldFeedbackResult(
    val hasSubmitted: Boolean = false,
    val alreadySubmitted: Boolean = false,
    val submittedWorldIds: List<Int> = emptyList(),
    val submittedWorldSlugs: List<String> = emptyList(),
    val feedback: StyleWorldFeedback? = null,
    val coupon: UnlockedCoupon? = null
)

data class StyleWorld(
    val styleWorldId: Int,
    val slug: String,
    val name: String,
    val tagline: String = "",
    val description: String = "",
    val themeKey: String = "dark_editorial",
    val animationKey: String = "smooth_reveal",
    val fontStyle: String = "editorial_serif",
    val buttonStyle: String = "editorial_pill",
    val primaryColor: String = "#FFFFFF",
    val accentColor: String = "#C8CCD4",
    val backgroundColor: String = "#0A0A0D",
    val coverImageUrl: String? = null,
    val mobileImageUrl: String? = null,
    val videoUrl: String? = null,
    val audioTrackKey: String = "none",
    val audioUrl: String? = null,
    val audioLoop: Boolean = true,
    val audioVolume: Float = 0.45f,
    val promoVideoUrl: String? = null,
    val promoVideoPosterUrl: String? = null,
    val promoVideoIsSponsored: Boolean = false,
    val runwayConfig: RunwayConfig? = null,
    val hasSubmittedFeedback: Boolean = false,
    val productCount: Int = 0
)

data class StyleWorldDetail(
    val styleWorld: StyleWorld,
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val satisfactionStatus: StyleWorldSatisfactionStatus? = null
)

data class VideoReel(
    val videoId: Int,
    val title: String = "",
    val caption: String = "",
    val creatorName: String = "",
    val creatorHandle: String = "",
    val videoUrl: String? = null,
    val posterUrl: String? = null,
    val durationSeconds: Int = 0,
    val aspectRatio: String = "9:16",
    val isSponsored: Boolean = false,
    val isFeatured: Boolean = false,
    val brandId: Int? = null,
    val brandName: String? = null,
    val brandSlug: String? = null,
    val brandLogo: String? = null,
    val productId: Int? = null,
    val product: Product? = null,
    val viewsCount: Int = 0,
    val likesCount: Int = 0,
    val savesCount: Int = 0,
    val sharesCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false
) {
    fun hasValidStreamUrl(): Boolean {
        val raw = videoUrl?.trim().orEmpty()
        if (raw.isEmpty()) return false
        val lower = raw.lowercase()
        if (!lower.startsWith("https://") && !lower.startsWith("http://")) return false
        if (lower.endsWith(".html") || lower.endsWith(".php")) return false
        return true
    }
}

data class VideosFeedPage(
    val videos: List<VideoReel> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val tableExists: Boolean = true,
    val invalidUrlCount: Int = 0,
    val diagnosticsReason: String = ""
) {
    val hasMore: Boolean get() = page < totalPages
}

data class ElitePackItem(
    val itemId: Int,
    val itemType: String = "product",
    val title: String = "",
    val subtitle: String = "",
    val badgeText: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val isSponsored: Boolean = false,
    val product: Product? = null,
    val brand: BrandProfileItem? = null
)

data class ElitePack(
    val packId: Int,
    val slug: String = "",
    val sectionKey: String = "",
    val brandName: String = "",
    val eyebrow: String = "",
    val title: String = "",
    val subtitle: String = "",
    val story: String = "",
    val ctaText: String = "",
    val ctaLink: String = "",
    val heroImageUrl: String? = null,
    val mobileImageUrl: String? = null,
    val videoUrl: String? = null,
    val animationKey: String = "metallic_shine",
    val accentColor: String = "#C8CCD4",
    val backgroundColor: String = "#000000",
    val layoutType: String = "editorial_grid",
    val featuredProductId: Int? = null,
    val isHero: Boolean = false,
    val isSponsored: Boolean = false,
    val items: List<ElitePackItem> = emptyList()
)

data class EliteHomeBundle(
    val heroPacks: List<ElitePack> = emptyList(),
    val packs: List<ElitePack> = emptyList(),
    val eliteBrands: List<BrandProfileItem> = emptyList(),
    val sponsoredProducts: List<Product> = emptyList(),
    val eliteVideos: List<VideoReel> = emptyList()
)

data class ElitePackDetailBundle(
    val pack: ElitePack,
    val products: List<Product> = emptyList(),
    val brands: List<BrandProfileItem> = emptyList(),
    val relatedPacks: List<ElitePack> = emptyList()
)

data class PreferredCategoryScore(
    val categoryId: Int,
    val name: String,
    val slug: String,
    val imageUrl: String? = null,
    val affinityScore: Double = 0.0
)

data class ForYouBundle(
    val isPersonalized: Boolean = false,
    val title: String = "Trending & Editorial Picks",
    val subtitle: String = "",
    val products: List<Product> = emptyList(),
    val recentBrands: List<BrandProfileItem> = emptyList(),
    val favouriteStyles: List<StyleWorld> = emptyList(),
    val preferredWorlds: List<StyleWorld> = emptyList(),
    val avoidedWorlds: List<StyleWorld> = emptyList(),
    val preferredCategories: List<PreferredCategoryScore> = emptyList(),
    val preferredStyles: List<String> = emptyList(),
    val recentlyExploredWorlds: List<StyleWorld> = emptyList()
)

data class SearchFilterState(
    val brandId: Int? = null,
    val categorySlug: String? = null,
    val size: String? = null,
    val color: String? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val sort: String = "relevance"
) {
    val activeCount: Int
        get() = listOfNotNull(brandId, categorySlug, size, color, minPrice, maxPrice).size +
            if (sort != "relevance") 1 else 0
}

data class SearchResultBundle(
    val query: String = "",
    val products: List<Product> = emptyList(),
    val page: Int = 1,
    val total: Int = 0,
    val totalPages: Int = 1
)

data class SearchSuggestionsBundle(
    val suggestions: List<String> = emptyList(),
    val trending: List<String> = emptyList(),
    val recent: List<String> = emptyList()
)

data class DeliveryServiceability(
    val pincode: String,
    val city: String = "",
    val state: String = "",
    val isServiceable: Boolean = true,
    val expressAvailable: Boolean = false,
    val codAvailable: Boolean = true,
    val estimatedDaysMin: Int = 4,
    val estimatedDaysMax: Int = 7,
    val deliveryMessage: String = ""
)

data class ReviewEligibilityItem(
    val orderItemId: Int,
    val orderId: Int,
    val orderNumber: String,
    val productName: String,
    val orderedAt: String = ""
)

data class ReviewListBundle(
    val reviews: List<ProductReview> = emptyList(),
    val eligibleOrderItems: List<ReviewEligibilityItem> = emptyList(),
    val total: Int = 0
)

data class ReviewOtpRequestState(
    val otpRequestId: Int,
    val orderItemId: Int,
    val productId: Int,
    val sentTo: String = "",
    val expiresInSeconds: Int = 600,
    val resendCooldownSeconds: Int = 60
)

data class HelpTopic(
    val topicId: Int,
    val slug: String,
    val title: String,
    val subtitle: String = "",
    val iconKey: String = "help",
    val category: String = "general",
    val requiresOrder: Boolean = false
)

data class HelpFlowOption(
    val optionId: Int,
    val label: String,
    val nextNodeId: Int? = null,
    val actionKey: String = "",
    val escalateToTicket: Boolean = false
)

data class HelpFlowNode(
    val nodeId: Int,
    val nodeKey: String = "",
    val nodeType: String = "question",
    val title: String,
    val bodyText: String = "",
    val policyText: String = "",
    val requiresOrderSelect: Boolean = false,
    val requiresItemSelect: Boolean = false,
    val allowsPhotoUpload: Boolean = false,
    val allowsNoteInput: Boolean = false,
    val isTerminal: Boolean = false,
    val canEscalateTicket: Boolean = true,
    val options: List<HelpFlowOption> = emptyList()
)

data class HelpFlow(
    val flowId: Int,
    val title: String,
    val startNodeId: Int? = null,
    val nodes: List<HelpFlowNode> = emptyList()
)

data class HelpFlowDetail(
    val topic: HelpTopic,
    val flow: HelpFlow? = null
)

data class HelpOrderItemSnapshot(
    val orderItemId: Int,
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double
)

data class HelpOrderReport(
    val orderId: Int,
    val orderNumber: String,
    val orderStatus: String,
    val paymentStatus: String,
    val paymentMethod: String,
    val trackingNumber: String? = null,
    val courierName: String? = null,
    val trackingUrl: String? = null,
    val estimatedDeliveryDate: String? = null,
    val returnEligible: Boolean = false,
    val cancelEligible: Boolean = false,
    val statusExplanation: String = "",
    val policySummary: String = "",
    val items: List<HelpOrderItemSnapshot> = emptyList()
)

data class HelpTicketCreated(
    val ticketId: Int,
    val ticketNumber: String,
    val status: String,
    val note: String
)

data class HelpSessionResult(
    val sessionToken: String = "",
    val orderReport: HelpOrderReport? = null,
    val ticket: HelpTicketCreated? = null
)

// ==================== DYNAMIC FASHION WORLDS & THEME ENGINE ====================

@Serializable
data class ThemeTokenSpec(
    val themeKey: String = "melvore_light",
    val name: String = "Melvore Editorial Light",
    val mode: String = "light",
    val backgroundColor: String = "#FAF9F6",
    val surfaceColor: String = "#FFFFFF",
    val surfaceElevated: String = "#F3F1EC",
    val primaryColor: String = "#09090B",
    val accentColor: String = "#1D4ED8",
    val secondaryAccent: String = "#B68B2E",
    val textPrimary: String = "#09090B",
    val textSecondary: String = "#52525B",
    val borderColor: String = "#E4E4E7",
    val fontStyle: String = "editorial_serif",
    val buttonStyle: String = "sharp",
    val cardRadiusDp: Int = 10,
    val borderWidthDp: Float = 1f,
    val ambientPreset: String = "none",
    val audioTrackKey: String = "none",
    val motionIntensity: Float = 0.65f
)

@Serializable
data class OccasionTheme(
    val occasionId: Int = 0,
    val slug: String = "",
    val name: String = "",
    val eyebrow: String = "",
    val headline: String = "",
    val subtitle: String = "",
    val occasionType: String = "seasonal",
    val motifKey: String = "none",
    val buttonStyle: String = "",
    val primaryColor: String = "#D4AF37",
    val accentColor: String = "#B45309",
    val backgroundColor: String = "#FAF5EB",
    val fontStyle: String = "editorial_serif",
    val audioTrackKey: String = "none",
    val audioUrl: String? = null,
    val audioLoop: Boolean = true,
    val ctaText: String = "",
    val ctaCollectionSlug: String = "",
    val bannerImageUrl: String? = null,
    val priority: Int = 0,
    val eventDate: String? = null,
    val activationDaysBefore: Int = 8,
    val activationStartAt: String? = null,
    val activationEndAt: String? = null,
    val timezone: String = "Asia/Kolkata",
    val regionCountry: String = "ALL",
    val regionState: String = "",
    val regionCity: String = "",
    val startDate: String = "",
    val endDate: String = ""
)

@Serializable
data class FashionWorldTheme(
    val styleWorldId: Int = 0,
    val slug: String = "",
    val name: String = "",
    val tagline: String = "",
    val description: String = "",
    val themeKey: String = "melvore_light",
    val mode: String = "light",
    val animationKey: String = "smooth_reveal",
    val fontStyle: String = "editorial_serif",
    val buttonStyle: String = "sharp",
    val cardRadiusDp: Int = 10,
    val borderWidthDp: Float = 1f,
    val primaryColor: String = "#09090B",
    val accentColor: String = "#2563EB",
    val secondaryAccent: String = "#B68B2E",
    val backgroundColor: String = "#FAF9F6",
    val surfaceColor: String = "#FFFFFF",
    val surfaceElevated: String = "#F3F1EC",
    val textPrimary: String = "#09090B",
    val textSecondary: String = "#52525B",
    val borderColor: String = "#E4E4E7",
    val ambientPreset: String = "none",
    val audioTrackKey: String = "none",
    val audioUrl: String? = null,
    val audioLoop: Boolean = true,
    val audioVolume: Float = 0.45f,
    val sfxProfile: String = "soft",
    val heroLayout: String = "editorial_split",
    val coverImageUrl: String? = null,
    val mobileImageUrl: String? = null,
    val videoUrl: String? = null,
    val promoVideoUrl: String? = null,
    val promoVideoPosterUrl: String? = null,
    val promoVideoIsSponsored: Boolean = false,
    val runwayConfig: RunwayConfig? = null,
    val hasSubmittedFeedback: Boolean = false,
    val displayOrder: Int = 0,
    val versionTag: String = "1"
) {
    fun toStyleWorld(productCount: Int = 0): StyleWorld = StyleWorld(
        styleWorldId = styleWorldId,
        slug = slug,
        name = name,
        tagline = tagline,
        description = description,
        themeKey = themeKey,
        animationKey = animationKey,
        fontStyle = fontStyle,
        buttonStyle = buttonStyle,
        primaryColor = primaryColor,
        accentColor = accentColor,
        backgroundColor = backgroundColor,
        coverImageUrl = coverImageUrl,
        mobileImageUrl = mobileImageUrl,
        videoUrl = videoUrl,
        audioTrackKey = audioTrackKey,
        audioUrl = audioUrl,
        audioLoop = audioLoop,
        audioVolume = audioVolume,
        promoVideoUrl = promoVideoUrl,
        promoVideoPosterUrl = promoVideoPosterUrl,
        promoVideoIsSponsored = promoVideoIsSponsored,
        runwayConfig = runwayConfig,
        hasSubmittedFeedback = hasSubmittedFeedback,
        productCount = productCount
    )
}

@Serializable
data class WorldVectorAsset(
    val assetId: Int = 0,
    val assetKey: String = "",
    val worldSlug: String = "",
    val occasionSlug: String = "",
    val vectorType: String = "editorial_grid",
    val layerRole: String = "accent",
    val primaryHex: String = "#C8CCD4",
    val secondaryHex: String = "#FFFFFF",
    val opacity: Float = 0.35f,
    val motionPattern: String = "float_gentle",
    val imageUrl: String? = null
)

@Serializable
data class WorldMapOriginPin(
    val originId: Int = 0,
    val brandId: Int = 0,
    val brandName: String = "",
    val brandSlug: String = "",
    val brandLogo: String? = null,
    val coverImageUrl: String? = null,
    val tagline: String = "",
    val story: String = "",
    val city: String = "",
    val country: String = "",
    val countryCode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val worldSlug: String = "",
    val productCount: Int = 0,
    val isVerified: Boolean = false
)

@Serializable
data class SmartOffer(
    val offerId: Int = 0,
    val offerKey: String = "",
    val title: String = "",
    val subtitle: String = "",
    val badgeText: String = "",
    val couponCode: String = "",
    val worldSlug: String = "",
    val occasionSlug: String = "",
    val minCartValue: Double = 0.0,
    val maxImpressionsPerSession: Int = 2,
    val cooldownHours: Int = 12,
    val priority: Int = 0,
    val accentHex: String = "#1D4ED8",
    val ctaText: String = "Apply Code"
)

@Serializable
data class UserDynamicPreferences(
    val ambientAudioEnabled: Boolean = false,
    val uiSfxEnabled: Boolean = false,
    val reducedMotion: Boolean = false,
    val videoAutoplayMuted: Boolean = true,
    val dataSaverMode: Boolean = false,
    val audioConsentAnswered: Boolean = false,
    val preferredWorldSlug: String? = null
)

@Serializable
data class BootstrapConfigBundle(
    val configVersion: String = "default-v1",
    val serverTimeUtc: String = "",
    val activeDesign: Int = 0,
    val designSlot: String = "default",
    val designVersion: String = "1.0.0",
    val designChecksum: String = "",
    val designManifest: DesignManifest? = null,
    val defaultTheme: ThemeTokenSpec = ThemeTokenSpec(),
    val activeOccasion: OccasionTheme? = null,
    val occasions: List<OccasionTheme> = emptyList(),
    val fashionWorlds: List<FashionWorldTheme> = emptyList(),
    val submittedWorldIds: List<Int> = emptyList(),
    val submittedWorldSlugs: List<String> = emptyList(),
    val worldAssets: List<WorldVectorAsset> = emptyList(),
    val worldMapOrigins: List<WorldMapOriginPin> = emptyList(),
    val smartOffers: List<SmartOffer> = emptyList(),
    val userPreferences: UserDynamicPreferences? = null
)

@Serializable
data class FashionWorldContext(
    val activeWorldSlug: String? = null,
    val activeWorldId: Int? = null,
    val scrollPreviewWorldSlug: String? = null,
    val backStackSlugs: List<String> = emptyList(),
    val isLockedToWorld: Boolean = false
) {
    val effectiveWorldSlug: String?
        get() = activeWorldSlug ?: scrollPreviewWorldSlug

    val isInsideDedicatedWorld: Boolean
        get() = !activeWorldSlug.isNullOrBlank()
}

