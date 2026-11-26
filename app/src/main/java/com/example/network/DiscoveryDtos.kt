package com.example.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean = false,
    val message: String? = null,
    @SerialName("error_code") val errorCode: String? = null,
    val data: T? = null
)

// ==================== HOME FEED & CAMPAIGNS ====================

@Serializable
data class CampaignAssetDto(
    @SerialName("asset_id") val assetId: Int = 0,
    @SerialName("asset_type") val assetType: String = "image",
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("mobile_media_url") val mobileMediaUrl: String? = null,
    @SerialName("animation_key") val animationKey: String = "none",
    val title: String = "",
    val subtitle: String = ""
)

@Serializable
data class CampaignDto(
    @SerialName("campaign_id") val campaignId: Int = 0,
    val slug: String = "",
    val title: String = "",
    val subtitle: String = "",
    @SerialName("festival_key") val festivalKey: String = "seasonal",
    @SerialName("animation_key") val animationKey: String = "none",
    @SerialName("animation_duration_ms") val animationDurationMs: Int = 2400,
    @SerialName("primary_color") val primaryColor: String = "#D4AF37",
    @SerialName("accent_color") val accentColor: String = "#FFFFFF",
    @SerialName("background_color") val backgroundColor: String = "#0B0B0E",
    @SerialName("banner_image_url") val bannerImageUrl: String? = null,
    @SerialName("mobile_banner_url") val mobileBannerUrl: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("cta_text") val ctaText: String = "",
    @SerialName("cta_link") val ctaLink: String = "",
    val assets: List<CampaignAssetDto> = emptyList()
)

@Serializable
data class HomeSectionItemDto(
    @SerialName("item_id") val itemId: Int = 0,
    val title: String = "",
    val subtitle: String = "",
    @SerialName("badge_text") val badgeText: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("mobile_image_url") val mobileImageUrl: String? = null,
    @SerialName("theme_key") val themeKey: String = "dark_editorial",
    @SerialName("animation_key") val animationKey: String = "smooth_reveal",
    @SerialName("target_type") val targetType: String = "category",
    @SerialName("target_value") val targetValue: String = "",
    @SerialName("target_id") val targetId: Int? = null
)

@Serializable
data class HomeSectionDto(
    @SerialName("section_id") val sectionId: Int = 0,
    @SerialName("section_key") val sectionKey: String = "",
    @SerialName("section_type") val sectionType: String = "product_shelf",
    val eyebrow: String = "",
    val title: String = "",
    val subtitle: String = "",
    @SerialName("animation_key") val animationKey: String = "smooth_reveal",
    @SerialName("theme_key") val themeKey: String = "dark_editorial",
    @SerialName("cta_text") val ctaText: String = "",
    @SerialName("cta_link") val ctaLink: String = "",
    @SerialName("display_order") val displayOrder: Int = 0,
    val items: List<HomeSectionItemDto> = emptyList(),
    val products: List<ProductDto> = emptyList()
)

@Serializable
data class HomePromoBannerDto(
    val id: Int = 0,
    val title: String = "",
    val subtitle: String = "",
    @SerialName("badge_text") val badgeText: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("cta_text") val ctaText: String = "",
    @SerialName("cta_link") val ctaLink: String = ""
)

@Serializable
data class HomeSiteSettingsDto(
    @SerialName("free_shipping_minimum") val freeShippingMinimum: Double = 0.0,
    @SerialName("shipping_charge") val shippingCharge: Double = 0.0,
    @SerialName("enable_cod") val enableCod: Boolean = true,
    @SerialName("enable_online_payment") val enableOnlinePayment: Boolean = true,
    val currency: String = "INR"
)

@Serializable
data class HomeEditorialStoryDto(
    val id: Int = 0,
    val title: String = "",
    val slug: String = "",
    val excerpt: String = "",
    @SerialName("featured_image") val featuredImage: String? = null,
    @SerialName("published_at") val publishedAt: String = "",
    @SerialName("read_time") val readTime: String = ""
)

@Serializable
data class FeedDiagnosticsDto(
    @SerialName("missing_tables") val missingTables: List<String> = emptyList(),
    @SerialName("empty_modules") val emptyModules: List<String> = emptyList()
)

@Serializable
data class HomeFeedDataDto(
    @SerialName("hero_slides") val heroSlides: List<HeroSlideDto> = emptyList(),
    @SerialName("active_campaign") val activeCampaign: CampaignDto? = null,
    val campaigns: List<CampaignDto> = emptyList(),
    @SerialName("promo_banners") val promoBanners: List<HomePromoBannerDto> = emptyList(),
    @SerialName("site_settings") val siteSettings: HomeSiteSettingsDto? = null,
    val categories: List<CategoryDto> = emptyList(),
    val brands: List<BrandProfileDto> = emptyList(),
    @SerialName("style_worlds") val styleWorlds: List<StyleWorldDto> = emptyList(),
    val sections: List<HomeSectionDto> = emptyList(),
    @SerialName("new_arrivals") val newArrivals: List<ProductDto> = emptyList(),
    val bestsellers: List<ProductDto> = emptyList(),
    @SerialName("trending_products") val trendingProducts: List<ProductDto> = emptyList(),
    @SerialName("most_wishlisted") val mostWishlisted: List<ProductDto> = emptyList(),
    @SerialName("budget_999") val budget999: List<ProductDto> = emptyList(),
    @SerialName("budget_1999") val budget1999: List<ProductDto> = emptyList(),
    @SerialName("sponsored_products") val sponsoredProducts: List<ProductDto> = emptyList(),
    @SerialName("recently_viewed") val recentlyViewed: List<ProductDto> = emptyList(),
    @SerialName("editorial_stories") val editorialStories: List<HomeEditorialStoryDto> = emptyList(),
    @SerialName("preview_videos") val previewVideos: List<VideoReelDto> = emptyList(),
    @SerialName("discovery_products") val discoveryProducts: List<ProductDto> = emptyList(),
    val diagnostics: FeedDiagnosticsDto? = null
)

// ==================== BRANDS ====================

@Serializable
data class BrandProfileDto(
    @SerialName("brand_id") val brandId: Int = 0,
    val name: String = "",
    val slug: String = "",
    val description: String = "",
    val tagline: String = "",
    val story: String = "",
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("mobile_cover_url") val mobileCoverUrl: String? = null,
    @SerialName("brand_video_url") val brandVideoUrl: String? = null,
    @SerialName("origin_country") val originCountry: String = "",
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("is_sponsored") val isSponsored: Boolean = false,
    @SerialName("is_new") val isNew: Boolean = false,
    @SerialName("product_count") val productCount: Int = 0
)

@Serializable
data class BrandListDataDto(
    val tab: String = "all",
    val brands: List<BrandProfileDto> = emptyList()
)

@Serializable
data class BrandDetailsDataDto(
    val brand: BrandProfileDto? = null,
    @SerialName("featured_products") val featuredProducts: List<ProductDto> = emptyList(),
    @SerialName("new_arrivals") val newArrivals: List<ProductDto> = emptyList(),
    val bestsellers: List<ProductDto> = emptyList(),
    @SerialName("sponsored_products") val sponsoredProducts: List<ProductDto> = emptyList()
)

// ==================== STYLE WORLDS ====================

@Serializable
data class RunwayModelDto(
    val name: String = "",
    val role: String = "",
    val palette: List<String> = emptyList(),
    @SerialName("style_note") val styleNote: String = "",
    val silhouette: String = "cargo_baby_tee",
    @SerialName("sprite_url") val spriteUrl: String? = null
)

@Serializable
data class RunwayConfigDto(
    val enabled: Boolean = false,
    val title: String = "PIXEL RUNWAY SHOWCASE",
    val subtitle: String = "Live 8-bit chrome silhouettes styled from this universe",
    @SerialName("animation_preset") val animationPreset: String = "pixel_walk_loop",
    val speed: Float = 1.0f,
    @SerialName("background_hex") val backgroundHex: String = "#160D2D",
    @SerialName("display_position") val displayPosition: String = "after_promo",
    @SerialName("collection_slug") val collectionSlug: String = "",
    @SerialName("artwork_url") val artworkUrl: String? = null,
    val models: List<RunwayModelDto> = emptyList()
)

@Serializable
data class StyleWorldDto(
    @SerialName("style_world_id") val styleWorldId: Int = 0,
    val slug: String = "",
    val name: String = "",
    val tagline: String = "",
    val description: String = "",
    @SerialName("theme_key") val themeKey: String = "dark_editorial",
    @SerialName("animation_key") val animationKey: String = "smooth_reveal",
    @SerialName("font_style") val fontStyle: String = "editorial_serif",
    @SerialName("button_style") val buttonStyle: String = "editorial_pill",
    @SerialName("primary_color") val primaryColor: String = "#FFFFFF",
    @SerialName("accent_color") val accentColor: String = "#C8CCD4",
    @SerialName("background_color") val backgroundColor: String = "#0A0A0D",
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("mobile_image_url") val mobileImageUrl: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("audio_track_key") val audioTrackKey: String = "none",
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("audio_loop") val audioLoop: Boolean = true,
    @SerialName("audio_volume") val audioVolume: Float = 0.45f,
    @SerialName("promo_video_url") val promoVideoUrl: String? = null,
    @SerialName("promo_video_poster_url") val promoVideoPosterUrl: String? = null,
    @SerialName("promo_video_is_sponsored") val promoVideoIsSponsored: Boolean = false,
    @SerialName("runway_config") val runwayConfig: RunwayConfigDto? = null,
    @SerialName("has_submitted_feedback") val hasSubmittedFeedback: Boolean = false,
    @SerialName("product_count") val productCount: Int = 0
)

@Serializable
data class StyleWorldsListDataDto(
    @SerialName("style_worlds") val styleWorlds: List<StyleWorldDto> = emptyList()
)

@Serializable
data class StyleWorldFeedbackDto(
    @SerialName("feedback_id") val feedbackId: Int = 0,
    @SerialName("style_world_id") val styleWorldId: Int = 0,
    @SerialName("world_slug") val worldSlug: String = "",
    @SerialName("satisfaction_score") val satisfactionScore: Int = 0,
    @SerialName("feedback_label") val feedbackLabel: String = "",
    @SerialName("coupon_code") val couponCode: String? = null,
    @SerialName("submitted_at") val submittedAt: String = ""
)

@Serializable
data class UnlockedCouponDto(
    @SerialName("coupon_id") val couponId: Int = 0,
    val code: String = "",
    @SerialName("discount_type") val discountType: String = "percentage",
    @SerialName("discount_value") val discountValue: Double = 0.0,
    @SerialName("discount_summary") val discountSummary: String = "",
    @SerialName("min_order_amount") val minOrderAmount: Double = 0.0,
    @SerialName("max_discount_amount") val maxDiscountAmount: Double? = null,
    @SerialName("valid_to") val validTo: String? = null,
    @SerialName("conditions_text") val conditionsText: String = ""
)

@Serializable
data class StyleWorldSatisfactionStatusDto(
    @SerialName("has_submitted") val hasSubmitted: Boolean = false,
    val feedback: StyleWorldFeedbackDto? = null
)

@Serializable
data class StyleWorldFeedbackPayload(
    @SerialName("style_world_id") val styleWorldId: Int,
    @SerialName("world_slug") val worldSlug: String = "",
    @SerialName("satisfaction_score") val satisfactionScore: Int
)

@Serializable
data class StyleWorldFeedbackResponseDataDto(
    @SerialName("has_submitted") val hasSubmitted: Boolean = false,
    @SerialName("already_submitted") val alreadySubmitted: Boolean = false,
    @SerialName("submitted_world_ids") val submittedWorldIds: List<Int> = emptyList(),
    @SerialName("submitted_world_slugs") val submittedWorldSlugs: List<String> = emptyList(),
    val feedback: StyleWorldFeedbackDto? = null,
    val coupon: UnlockedCouponDto? = null
)

@Serializable
data class StyleWorldDetailsDataDto(
    @SerialName("style_world") val styleWorld: StyleWorldDto? = null,
    @SerialName("satisfaction_status") val satisfactionStatus: StyleWorldSatisfactionStatusDto? = null,
    val categories: List<CategoryDto> = emptyList(),
    val products: List<ProductDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto()
)

@Serializable
data class StyleSignalRequest(
    @SerialName("style_world_id") val styleWorldId: Int? = null,
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("category_id") val categoryId: Int? = null,
    @SerialName("signal_type") val signalType: String = "style_tap"
)

// ==================== VIDEOS / REELS ====================

@Serializable
data class VideoReelDto(
    @SerialName("video_id") val videoId: Int = 0,
    val title: String = "",
    val caption: String = "",
    @SerialName("creator_name") val creatorName: String = "",
    @SerialName("creator_handle") val creatorHandle: String = "",
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
    @SerialName("aspect_ratio") val aspectRatio: String = "9:16",
    @SerialName("is_sponsored") val isSponsored: Boolean = false,
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("brand_id") val brandId: Int? = null,
    @SerialName("brand_name") val brandName: String? = null,
    @SerialName("brand_slug") val brandSlug: String? = null,
    @SerialName("brand_logo") val brandLogo: String? = null,
    @SerialName("product_id") val productId: Int? = null,
    val product: ProductDto? = null,
    @SerialName("views_count") val viewsCount: Int = 0,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("saves_count") val savesCount: Int = 0,
    @SerialName("shares_count") val sharesCount: Int = 0,
    @SerialName("is_liked") val isLiked: Boolean = false,
    @SerialName("is_saved") val isSaved: Boolean = false
)

@Serializable
data class VideoDiagnosticsDto(
    @SerialName("table_exists") val tableExists: Boolean = true,
    @SerialName("active_count") val activeCount: Int = 0,
    @SerialName("invalid_url_count") val invalidUrlCount: Int = 0,
    val reason: String = ""
)

@Serializable
data class VideosFeedDataDto(
    val videos: List<VideoReelDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto(),
    val diagnostics: VideoDiagnosticsDto? = null
)

@Serializable
data class VideoEventRequest(
    @SerialName("video_id") val videoId: Int,
    @SerialName("event_type") val eventType: String,
    @SerialName("watch_seconds") val watchSeconds: Int = 0
)

@Serializable
data class VideoEventResponseDto(
    @SerialName("video_id") val videoId: Int = 0,
    @SerialName("event_type") val eventType: String = "",
    @SerialName("views_count") val viewsCount: Int = 0,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("saves_count") val savesCount: Int = 0,
    @SerialName("shares_count") val sharesCount: Int = 0
)

// ==================== ELITE PACKS ====================

@Serializable
data class ElitePackItemDto(
    @SerialName("item_id") val itemId: Int = 0,
    @SerialName("item_type") val itemType: String = "product",
    val title: String = "",
    val subtitle: String = "",
    @SerialName("badge_text") val badgeText: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("is_sponsored") val isSponsored: Boolean = false,
    val product: ProductDto? = null,
    val brand: BrandProfileDto? = null
)

@Serializable
data class ElitePackDto(
    @SerialName("pack_id") val packId: Int = 0,
    val slug: String = "",
    @SerialName("section_key") val sectionKey: String = "",
    @SerialName("brand_name") val brandName: String = "",
    val eyebrow: String = "",
    val title: String = "",
    val subtitle: String = "",
    val story: String = "",
    @SerialName("cta_text") val ctaText: String = "",
    @SerialName("cta_link") val ctaLink: String = "",
    @SerialName("hero_image_url") val heroImageUrl: String? = null,
    @SerialName("mobile_image_url") val mobileImageUrl: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("animation_key") val animationKey: String = "metallic_shine",
    @SerialName("accent_color") val accentColor: String = "#C8CCD4",
    @SerialName("background_color") val backgroundColor: String = "#000000",
    @SerialName("layout_type") val layoutType: String = "editorial_grid",
    @SerialName("featured_product_id") val featuredProductId: Int? = null,
    @SerialName("is_hero") val isHero: Boolean = false,
    @SerialName("is_sponsored") val isSponsored: Boolean = false,
    val items: List<ElitePackItemDto> = emptyList()
)

@Serializable
data class EliteHomeDataDto(
    @SerialName("hero_packs") val heroPacks: List<ElitePackDto> = emptyList(),
    val packs: List<ElitePackDto> = emptyList(),
    @SerialName("elite_brands") val eliteBrands: List<BrandProfileDto> = emptyList(),
    @SerialName("sponsored_products") val sponsoredProducts: List<ProductDto> = emptyList(),
    @SerialName("elite_videos") val eliteVideos: List<VideoReelDto> = emptyList()
)

@Serializable
data class ElitePackDetailDataDto(
    val pack: ElitePackDto? = null,
    val products: List<ProductDto> = emptyList(),
    val brands: List<BrandProfileDto> = emptyList(),
    @SerialName("related_packs") val relatedPacks: List<ElitePackDto> = emptyList()
)

// ==================== PERSONALIZATION & SEARCH ====================

@Serializable
data class PreferredCategoryScoreDto(
    @SerialName("category_id") val categoryId: Int = 0,
    val name: String = "",
    val slug: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("affinity_score") val affinityScore: Double = 0.0
)

@Serializable
data class ForYouDataDto(
    @SerialName("is_personalized") val isPersonalized: Boolean = false,
    val title: String = "Trending & Editorial Picks",
    val subtitle: String = "",
    val products: List<ProductDto> = emptyList(),
    @SerialName("recent_brands") val recentBrands: List<BrandProfileDto> = emptyList(),
    @SerialName("favourite_styles") val favouriteStyles: List<StyleWorldDto> = emptyList(),
    @SerialName("preferred_worlds") val preferredWorlds: List<StyleWorldDto> = emptyList(),
    @SerialName("avoided_worlds") val avoidedWorlds: List<StyleWorldDto> = emptyList(),
    @SerialName("preferred_categories") val preferredCategories: List<PreferredCategoryScoreDto> = emptyList(),
    @SerialName("preferred_styles") val preferredStyles: List<String> = emptyList(),
    @SerialName("recently_explored_worlds") val recentlyExploredWorlds: List<StyleWorldDto> = emptyList()
)

@Serializable
data class ProductRecommendationsDataDto(
    @SerialName("world_slug") val worldSlug: String? = null,
    val products: List<ProductDto> = emptyList(),
    @SerialName("similar_products") val similarProducts: List<ProductDto> = emptyList(),
    @SerialName("more_from_brand") val moreFromBrand: List<ProductDto> = emptyList()
)

@Serializable
data class SearchQueryDataDto(
    val query: String = "",
    val products: List<ProductDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto()
)

@Serializable
data class SearchSuggestionsDataDto(
    val suggestions: List<String> = emptyList(),
    val trending: List<String> = emptyList(),
    val recent: List<String> = emptyList()
)

// ==================== DELIVERY, REVIEWS OTP & GUIDED HELP ====================

@Serializable
data class DeliveryCheckDataDto(
    val pincode: String = "",
    val city: String = "",
    val state: String = "",
    @SerialName("is_serviceable") val isServiceable: Boolean = true,
    @SerialName("express_available") val expressAvailable: Boolean = false,
    @SerialName("cod_available") val codAvailable: Boolean = true,
    @SerialName("estimated_days_min") val estimatedDaysMin: Int = 4,
    @SerialName("estimated_days_max") val estimatedDaysMax: Int = 7,
    @SerialName("delivery_message") val deliveryMessage: String = ""
)

@Serializable
data class ReviewEligibilityItemDto(
    @SerialName("order_item_id") val orderItemId: Int = 0,
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("order_number") val orderNumber: String = "",
    @SerialName("product_name") val productName: String = "",
    @SerialName("ordered_at") val orderedAt: String = ""
)

@Serializable
data class ReviewListDataDto(
    val reviews: List<ReviewDto> = emptyList(),
    @SerialName("eligible_order_items") val eligibleOrderItems: List<ReviewEligibilityItemDto> = emptyList(),
    val pagination: PaginationDto = PaginationDto()
)

@Serializable
data class RequestReviewOtpPayload(
    @SerialName("order_item_id") val orderItemId: Int
)

@Serializable
data class RequestReviewOtpDataDto(
    @SerialName("otp_request_id") val otpRequestId: Int = 0,
    @SerialName("order_item_id") val orderItemId: Int = 0,
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("expires_in_seconds") val expiresInSeconds: Int = 600,
    @SerialName("resend_cooldown_seconds") val resendCooldownSeconds: Int = 60,
    @SerialName("sent_to") val sentTo: String = ""
)

@Serializable
data class VerifyReviewOtpPayload(
    @SerialName("otp_request_id") val otpRequestId: Int,
    val otp: String
)

@Serializable
data class VerifyReviewOtpDataDto(
    @SerialName("otp_request_id") val otpRequestId: Int = 0,
    val verified: Boolean = false,
    @SerialName("order_item_id") val orderItemId: Int = 0,
    @SerialName("product_id") val productId: Int = 0
)

@Serializable
data class CreateVerifiedReviewPayload(
    @SerialName("order_item_id") val orderItemId: Int,
    @SerialName("otp_request_id") val otpRequestId: Int,
    val rating: Int,
    val title: String,
    val comment: String,
    @SerialName("media_urls") val mediaUrls: List<String> = emptyList()
)

@Serializable
data class HelpfulReviewPayload(
    @SerialName("review_id") val reviewId: Int
)

@Serializable
data class HelpfulReviewDataDto(
    @SerialName("review_id") val reviewId: Int = 0,
    @SerialName("helpful_count") val helpfulCount: Int = 0,
    @SerialName("has_voted_helpful") val hasVotedHelpful: Boolean = false
)

@Serializable
data class MediaUploadDataDto(
    @SerialName("media_url") val mediaUrl: String = "",
    @SerialName("relative_path") val relativePath: String = ""
)

@Serializable
data class HelpTopicDto(
    @SerialName("topic_id") val topicId: Int = 0,
    val slug: String = "",
    val title: String = "",
    val subtitle: String = "",
    @SerialName("icon_key") val iconKey: String = "help",
    val category: String = "general",
    @SerialName("requires_order") val requiresOrder: Boolean = false
)

@Serializable
data class HelpTopicsDataDto(
    val topics: List<HelpTopicDto> = emptyList(),
    val faqs: List<FaqDto> = emptyList()
)

@Serializable
data class HelpFlowOptionDto(
    @SerialName("option_id") val optionId: Int = 0,
    val label: String = "",
    @SerialName("next_node_id") val nextNodeId: Int? = null,
    @SerialName("action_key") val actionKey: String = "",
    @SerialName("escalate_to_ticket") val escalateToTicket: Boolean = false
)

@Serializable
data class HelpFlowNodeDto(
    @SerialName("node_id") val nodeId: Int = 0,
    @SerialName("node_key") val nodeKey: String = "",
    @SerialName("node_type") val nodeType: String = "question",
    val title: String = "",
    @SerialName("body_text") val bodyText: String = "",
    @SerialName("policy_text") val policyText: String = "",
    @SerialName("requires_order_select") val requiresOrderSelect: Boolean = false,
    @SerialName("requires_item_select") val requiresItemSelect: Boolean = false,
    @SerialName("allows_photo_upload") val allowsPhotoUpload: Boolean = false,
    @SerialName("allows_note_input") val allowsNoteInput: Boolean = false,
    @SerialName("is_terminal") val isTerminal: Boolean = false,
    @SerialName("can_escalate_ticket") val canEscalateTicket: Boolean = true,
    val options: List<HelpFlowOptionDto> = emptyList()
)

@Serializable
data class HelpFlowDto(
    @SerialName("flow_id") val flowId: Int = 0,
    val title: String = "",
    @SerialName("start_node_id") val startNodeId: Int? = null,
    val nodes: List<HelpFlowNodeDto> = emptyList()
)

@Serializable
data class HelpFlowDataDto(
    val topic: HelpTopicDto? = null,
    val flow: HelpFlowDto? = null
)

@Serializable
data class HelpSessionRequest(
    val action: String = "evaluate",
    @SerialName("topic_id") val topicId: Int = 0,
    @SerialName("flow_id") val flowId: Int = 0,
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("order_item_id") val orderItemId: Int = 0,
    @SerialName("node_id") val nodeId: Int = 0,
    @SerialName("option_id") val optionId: Int = 0,
    val subject: String = "",
    val category: String = "order_issue",
    val note: String = "",
    @SerialName("evidence_urls") val evidenceUrls: List<String> = emptyList(),
    @SerialName("session_token") val sessionToken: String = ""
)

@Serializable
data class HelpOrderItemSnapshotDto(
    @SerialName("order_item_id") val orderItemId: Int = 0,
    @SerialName("product_id") val productId: Int = 0,
    @SerialName("product_name") val productName: String = "",
    val quantity: Int = 1,
    @SerialName("unit_price") val unitPrice: Double = 0.0
)

@Serializable
data class HelpOrderReportDto(
    @SerialName("order_id") val orderId: Int = 0,
    @SerialName("order_number") val orderNumber: String = "",
    @SerialName("order_status") val orderStatus: String = "",
    @SerialName("payment_status") val paymentStatus: String = "",
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("tracking_number") val trackingNumber: String? = null,
    @SerialName("courier_name") val courierName: String? = null,
    @SerialName("tracking_url") val trackingUrl: String? = null,
    @SerialName("estimated_delivery_date") val estimatedDeliveryDate: String? = null,
    @SerialName("return_eligible") val returnEligible: Boolean = false,
    @SerialName("cancel_eligible") val cancelEligible: Boolean = false,
    @SerialName("status_explanation") val statusExplanation: String = "",
    @SerialName("policy_summary") val policySummary: String = "",
    val items: List<HelpOrderItemSnapshotDto> = emptyList()
)

@Serializable
data class HelpTicketCreatedDto(
    @SerialName("ticket_id") val ticketId: Int = 0,
    @SerialName("ticket_number") val ticketNumber: String = "",
    val status: String = "",
    val note: String = ""
)

@Serializable
data class HelpSessionDataDto(
    @SerialName("session_token") val sessionToken: String = "",
    @SerialName("order_report") val orderReport: HelpOrderReportDto? = null,
    val ticket: HelpTicketCreatedDto? = null
)

// ==================== DYNAMIC BOOTSTRAP, WORLDS, MAP & OFFERS ====================

@Serializable
data class ThemeTokensDto(
    @SerialName("theme_key") val themeKey: String = "melvore_light",
    val name: String = "Melvore Editorial Light",
    val mode: String = "light",
    @SerialName("background_color") val backgroundColor: String = "#FAF9F6",
    @SerialName("surface_color") val surfaceColor: String = "#FFFFFF",
    @SerialName("surface_elevated") val surfaceElevated: String = "#F3F1EC",
    @SerialName("primary_color") val primaryColor: String = "#09090B",
    @SerialName("accent_color") val accentColor: String = "#1D4ED8",
    @SerialName("secondary_accent") val secondaryAccent: String = "#B68B2E",
    @SerialName("text_primary") val textPrimary: String = "#09090B",
    @SerialName("text_secondary") val textSecondary: String = "#52525B",
    @SerialName("border_color") val borderColor: String = "#E4E4E7",
    @SerialName("font_style") val fontStyle: String = "editorial_serif",
    @SerialName("button_style") val buttonStyle: String = "sharp",
    @SerialName("card_radius_dp") val cardRadiusDp: Int = 10,
    @SerialName("border_width_dp") val borderWidthDp: Float = 1f,
    @SerialName("ambient_preset") val ambientPreset: String = "none",
    @SerialName("audio_track_key") val audioTrackKey: String = "none",
    @SerialName("motion_intensity") val motionIntensity: Float = 0.65f
)

@Serializable
data class OccasionDto(
    @SerialName("occasion_id") val occasionId: Int = 0,
    val slug: String = "",
    val name: String = "",
    val eyebrow: String = "",
    val headline: String = "",
    val subtitle: String = "",
    @SerialName("occasion_type") val occasionType: String = "seasonal",
    @SerialName("motif_key") val motifKey: String = "none",
    @SerialName("button_style") val buttonStyle: String = "",
    @SerialName("primary_color") val primaryColor: String = "#D4AF37",
    @SerialName("accent_color") val accentColor: String = "#B45309",
    @SerialName("background_color") val backgroundColor: String = "#FAF5EB",
    @SerialName("font_style") val fontStyle: String = "editorial_serif",
    @SerialName("audio_track_key") val audioTrackKey: String = "none",
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("audio_loop") val audioLoop: Boolean = true,
    @SerialName("cta_text") val ctaText: String = "",
    @SerialName("cta_collection_slug") val ctaCollectionSlug: String = "",
    @SerialName("banner_image_url") val bannerImageUrl: String? = null,
    val priority: Int = 0,
    @SerialName("event_date") val eventDate: String? = null,
    @SerialName("activation_days_before") val activationDaysBefore: Int = 8,
    @SerialName("activation_start_at") val activationStartAt: String? = null,
    @SerialName("activation_end_at") val activationEndAt: String? = null,
    val timezone: String = "Asia/Kolkata",
    @SerialName("region_country") val regionCountry: String = "ALL",
    @SerialName("region_state") val regionState: String = "",
    @SerialName("region_city") val regionCity: String = "",
    @SerialName("start_date") val startDate: String = "",
    @SerialName("end_date") val endDate: String = ""
)

@Serializable
data class FashionWorldThemeDto(
    @SerialName("style_world_id") val styleWorldId: Int = 0,
    val slug: String = "",
    val name: String = "",
    val tagline: String = "",
    val description: String = "",
    @SerialName("theme_key") val themeKey: String = "melvore_light",
    val mode: String = "light",
    @SerialName("animation_key") val animationKey: String = "smooth_reveal",
    @SerialName("font_style") val fontStyle: String = "editorial_serif",
    @SerialName("button_style") val buttonStyle: String = "sharp",
    @SerialName("card_radius_dp") val cardRadiusDp: Int = 10,
    @SerialName("border_width_dp") val borderWidthDp: Float = 1f,
    @SerialName("primary_color") val primaryColor: String = "#09090B",
    @SerialName("accent_color") val accentColor: String = "#2563EB",
    @SerialName("secondary_accent") val secondaryAccent: String = "#B68B2E",
    @SerialName("background_color") val backgroundColor: String = "#FAF9F6",
    @SerialName("surface_color") val surfaceColor: String = "#FFFFFF",
    @SerialName("surface_elevated") val surfaceElevated: String = "#F3F1EC",
    @SerialName("text_primary") val textPrimary: String = "#09090B",
    @SerialName("text_secondary") val textSecondary: String = "#52525B",
    @SerialName("border_color") val borderColor: String = "#E4E4E7",
    @SerialName("ambient_preset") val ambientPreset: String = "none",
    @SerialName("audio_track_key") val audioTrackKey: String = "none",
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("audio_loop") val audioLoop: Boolean = true,
    @SerialName("audio_volume") val audioVolume: Float = 0.45f,
    @SerialName("sfx_profile") val sfxProfile: String = "soft",
    @SerialName("hero_layout") val heroLayout: String = "editorial_split",
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    @SerialName("mobile_image_url") val mobileImageUrl: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    @SerialName("promo_video_url") val promoVideoUrl: String? = null,
    @SerialName("promo_video_poster_url") val promoVideoPosterUrl: String? = null,
    @SerialName("promo_video_is_sponsored") val promoVideoIsSponsored: Boolean = false,
    @SerialName("runway_config") val runwayConfig: RunwayConfigDto? = null,
    @SerialName("has_submitted_feedback") val hasSubmittedFeedback: Boolean = false,
    @SerialName("display_order") val displayOrder: Int = 0,
    @SerialName("version_tag") val versionTag: String = "1"
)

@Serializable
data class WorldVectorAssetDto(
    @SerialName("asset_id") val assetId: Int = 0,
    @SerialName("asset_key") val assetKey: String = "",
    @SerialName("world_slug") val worldSlug: String = "",
    @SerialName("occasion_slug") val occasionSlug: String = "",
    @SerialName("vector_type") val vectorType: String = "editorial_grid",
    @SerialName("layer_role") val layerRole: String = "accent",
    @SerialName("primary_hex") val primaryHex: String = "#C8CCD4",
    @SerialName("secondary_hex") val secondaryHex: String = "#FFFFFF",
    val opacity: Float = 0.35f,
    @SerialName("motion_pattern") val motionPattern: String = "float_gentle",
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class WorldMapOriginDto(
    @SerialName("origin_id") val originId: Int = 0,
    @SerialName("brand_id") val brandId: Int = 0,
    @SerialName("brand_name") val brandName: String = "",
    @SerialName("brand_slug") val brandSlug: String = "",
    @SerialName("brand_logo") val brandLogo: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    val tagline: String = "",
    val story: String = "",
    val city: String = "",
    val country: String = "",
    @SerialName("country_code") val countryCode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @SerialName("world_slug") val worldSlug: String = "",
    @SerialName("product_count") val productCount: Int = 0,
    @SerialName("is_verified") val isVerified: Boolean = false
)

@Serializable
data class WorldMapDataDto(
    val pins: List<WorldMapOriginDto> = emptyList(),
    val count: Int = 0
)

@Serializable
data class SmartOfferDto(
    @SerialName("offer_id") val offerId: Int = 0,
    @SerialName("offer_key") val offerKey: String = "",
    val title: String = "",
    val subtitle: String = "",
    @SerialName("badge_text") val badgeText: String = "",
    @SerialName("coupon_code") val couponCode: String = "",
    @SerialName("world_slug") val worldSlug: String = "",
    @SerialName("occasion_slug") val occasionSlug: String = "",
    @SerialName("min_cart_value") val minCartValue: Double = 0.0,
    @SerialName("max_impressions_per_session") val maxImpressionsPerSession: Int = 2,
    @SerialName("cooldown_hours") val cooldownHours: Int = 12,
    val priority: Int = 0,
    @SerialName("accent_hex") val accentHex: String = "#1D4ED8",
    @SerialName("cta_text") val ctaText: String = "Apply Code"
)

@Serializable
data class EligibleOffersDataDto(
    val offers: List<SmartOfferDto> = emptyList(),
    @SerialName("cart_subtotal") val cartSubtotal: Double = 0.0
)

@Serializable
data class OfferImpressionPayload(
    @SerialName("offer_id") val offerId: Int,
    @SerialName("action_type") val actionType: String
)

@Serializable
data class UserDynamicPreferencesDto(
    @SerialName("ambient_audio_enabled") val ambientAudioEnabled: Boolean = false,
    @SerialName("ui_sfx_enabled") val uiSfxEnabled: Boolean = false,
    @SerialName("reduced_motion") val reducedMotion: Boolean = false,
    @SerialName("video_autoplay_muted") val videoAutoplayMuted: Boolean = true,
    @SerialName("data_saver_mode") val dataSaverMode: Boolean = false,
    @SerialName("audio_consent_answered") val audioConsentAnswered: Boolean = false,
    @SerialName("preferred_world_slug") val preferredWorldSlug: String? = null
)

@Serializable
data class BootstrapDataDto(
    @SerialName("config_version") val configVersion: String = "default-v1",
    val unchanged: Boolean = false,
    @SerialName("server_time_utc") val serverTimeUtc: String = "",
    @SerialName("server_time_ist") val serverTimeIst: String = "",
    @SerialName("active_design") val activeDesign: Int = 0,
    @SerialName("design_slot") val designSlot: String = "default",
    @SerialName("design_version") val designVersion: String = "1.0.0",
    @SerialName("design_checksum") val designChecksum: String = "",
    @SerialName("design_manifest") val designManifest: com.example.model.DesignManifest? = null,
    @SerialName("default_theme") val defaultTheme: ThemeTokensDto = ThemeTokensDto(),
    @SerialName("active_occasion") val activeOccasion: OccasionDto? = null,
    val occasions: List<OccasionDto> = emptyList(),
    @SerialName("fashion_worlds") val fashionWorlds: List<FashionWorldThemeDto> = emptyList(),
    @SerialName("submitted_world_ids") val submittedWorldIds: List<Int> = emptyList(),
    @SerialName("submitted_world_slugs") val submittedWorldSlugs: List<String> = emptyList(),
    @SerialName("world_assets") val worldAssets: List<WorldVectorAssetDto> = emptyList(),
    @SerialName("world_map_origins") val worldMapOrigins: List<WorldMapOriginDto> = emptyList(),
    @SerialName("smart_offers") val smartOffers: List<SmartOfferDto> = emptyList(),
    @SerialName("user_preferences") val userPreferences: UserDynamicPreferencesDto? = null
)

@Serializable
data class DesignManifestResponseDto(
    @SerialName("active_design") val activeDesign: Int = 0,
    val slot: String = "default",
    @SerialName("design_version") val designVersion: String = "1.0.0",
    @SerialName("schema_version") val schemaVersion: String = "1.0.0",
    @SerialName("min_app_schema_version") val minAppSchemaVersion: String = "1.0.0",
    @SerialName("checksum_sha256") val checksumSha256: String = "",
    @SerialName("manifest_hash") val manifestHash: String = "",
    @SerialName("asset_base_url") val assetBaseUrl: String = "",
    val etag: String = "",
    val unchanged: Boolean = false,
    @SerialName("fallback_used") val fallbackUsed: Boolean = false,
    @SerialName("fallback_reason") val fallbackReason: String? = null,
    @SerialName("active_occasion") val activeOccasion: OccasionDto? = null,
    val manifest: com.example.model.DesignManifest? = null
)

@Serializable
data class DesignVersionDataDto(
    @SerialName("active_design") val activeDesign: Int = 0,
    val slot: String = "default",
    @SerialName("design_version") val designVersion: String = "1.0.0",
    @SerialName("checksum_sha256") val checksumSha256: String = "",
    @SerialName("config_version") val configVersion: String = "",
    @SerialName("active_occasion_slug") val activeOccasionSlug: String? = null
)

@Serializable
data class ThemesConfigDataDto(
    @SerialName("config_version") val configVersion: String = "",
    @SerialName("default_theme") val defaultTheme: ThemeTokensDto = ThemeTokensDto(),
    val worlds: List<FashionWorldThemeDto> = emptyList(),
    @SerialName("fashion_worlds") val fashionWorlds: List<FashionWorldThemeDto> = emptyList(),
    val occasions: List<OccasionDto> = emptyList()
)

@Serializable
data class ActiveThemeDataDto(
    @SerialName("config_version") val configVersion: String = "",
    @SerialName("active_design") val activeDesign: Int = 0,
    @SerialName("design_slot") val designSlot: String = "default",
    @SerialName("design_version") val designVersion: String = "1.0.0",
    @SerialName("default_theme") val defaultTheme: ThemeTokensDto = ThemeTokensDto(),
    @SerialName("active_occasion") val activeOccasion: OccasionDto? = null,
    @SerialName("selected_world") val selectedWorld: FashionWorldThemeDto? = null
)

@Serializable
data class OccasionsListDataDto(
    @SerialName("active_occasion") val activeOccasion: OccasionDto? = null,
    val occasions: List<OccasionDto> = emptyList()
)

@Serializable
data class ActiveCampaignsDataDto(
    @SerialName("active_campaign") val activeCampaign: CampaignDto? = null,
    val campaigns: List<CampaignDto> = emptyList()
)


