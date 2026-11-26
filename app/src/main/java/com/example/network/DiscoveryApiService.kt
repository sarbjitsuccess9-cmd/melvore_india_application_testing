package com.example.network

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface DiscoveryApiService {

    @GET("api/home/feed.php")
    suspend fun getHomeFeed(
        @Query("country") country: String = "India",
        @Query("state") state: String = "",
        @Query("city") city: String = "",
        @Query("pincode") pincode: String = ""
    ): ApiEnvelope<HomeFeedDataDto>

    @GET("api/brands/list.php")
    suspend fun getBrandsDirectory(
        @Query("tab") tab: String = "all"
    ): ApiEnvelope<BrandListDataDto>

    @GET("api/brands/details.php")
    suspend fun getBrandDetails(
        @Query("id") brandId: Int? = null,
        @Query("slug") slug: String? = null
    ): ApiEnvelope<BrandDetailsDataDto>

    @POST("api/brands/view.php")
    suspend fun recordBrandView(@Body payload: Map<String, Int>): ApiMessageResponse

    @GET("api/styles/list.php")
    suspend fun getStyleWorlds(): ApiEnvelope<StyleWorldsListDataDto>

    @GET("api/styles/details.php")
    suspend fun getStyleWorldDetails(
        @Query("id") styleWorldId: Int? = null,
        @Query("slug") slug: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 24
    ): ApiEnvelope<StyleWorldDetailsDataDto>

    @POST("api/styles/signal.php")
    suspend fun recordStyleSignal(@Body payload: StyleSignalRequest): ApiMessageResponse

    @GET("api/styles/feedback.php")
    suspend fun getStyleWorldFeedback(
        @Query("style_world_id") styleWorldId: Int? = null,
        @Query("slug") slug: String? = null
    ): ApiEnvelope<StyleWorldFeedbackResponseDataDto>

    @POST("api/styles/feedback.php")
    suspend fun submitStyleWorldFeedback(@Body payload: StyleWorldFeedbackPayload): ApiEnvelope<StyleWorldFeedbackResponseDataDto>

    @GET("api/videos/feed.php")
    suspend fun getVideosFeed(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 12,
        @Query("brand_id") brandId: Int? = null,
        @Query("style_world_id") styleWorldId: Int? = null
    ): ApiEnvelope<VideosFeedDataDto>

    @POST("api/videos/event.php")
    suspend fun recordVideoEvent(@Body payload: VideoEventRequest): ApiEnvelope<VideoEventResponseDto>

    @GET("api/elite/home.php")
    suspend fun getEliteHome(): ApiEnvelope<EliteHomeDataDto>

    @GET("api/elite/pack.php")
    suspend fun getElitePack(
        @Query("id") packId: Int? = null,
        @Query("slug") slug: String? = null
    ): ApiEnvelope<ElitePackDetailDataDto>

    @GET("api/personalization/for_you.php")
    suspend fun getForYou(
        @Query("limit") limit: Int = 16
    ): ApiEnvelope<ForYouDataDto>

    @GET("api/products/recommendations.php")
    suspend fun getProductRecommendations(
        @Query("product_id") productId: Int,
        @Query("world_slug") worldSlug: String? = null,
        @Query("limit") limit: Int = 10
    ): ApiEnvelope<ProductRecommendationsDataDto>

    @GET("api/search/query.php")
    suspend fun searchCatalog(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 24,
        @Query("brand_id") brandId: Int? = null,
        @Query("category") category: String? = null,
        @Query("size") size: String? = null,
        @Query("color") color: String? = null,
        @Query("min_price") minPrice: Double? = null,
        @Query("max_price") maxPrice: Double? = null,
        @Query("sort") sort: String = "relevance"
    ): ApiEnvelope<SearchQueryDataDto>

    @GET("api/search/suggestions.php")
    suspend fun getSearchSuggestions(
        @Query("q") query: String
    ): ApiEnvelope<SearchSuggestionsDataDto>

    @GET("api/delivery/check.php")
    suspend fun checkDeliveryServiceability(
        @Query("pincode") pincode: String,
        @Query("product_id") productId: Int? = null
    ): ApiEnvelope<DeliveryCheckDataDto>

    @GET("api/reviews/list.php")
    suspend fun getProductReviews(
        @Query("product_id") productId: Int,
        @Query("page") page: Int = 1
    ): ApiEnvelope<ReviewListDataDto>

    @POST("api/reviews/request_otp.php")
    suspend fun requestReviewOtp(@Body payload: RequestReviewOtpPayload): ApiEnvelope<RequestReviewOtpDataDto>

    @POST("api/reviews/verify_otp.php")
    suspend fun verifyReviewOtp(@Body payload: VerifyReviewOtpPayload): ApiEnvelope<VerifyReviewOtpDataDto>

    @POST("api/reviews/create.php")
    suspend fun createVerifiedReview(@Body payload: CreateVerifiedReviewPayload): ApiMessageResponse

    @POST("api/reviews/helpful.php")
    suspend fun toggleReviewHelpful(@Body payload: HelpfulReviewPayload): ApiEnvelope<HelpfulReviewDataDto>

    @Multipart
    @POST("api/reviews/upload_media.php")
    suspend fun uploadReviewMedia(@Part image: MultipartBody.Part): ApiEnvelope<MediaUploadDataDto>

    @GET("api/help/topics.php")
    suspend fun getHelpTopics(): ApiEnvelope<HelpTopicsDataDto>

    @GET("api/help/flow.php")
    suspend fun getHelpFlow(
        @Query("topic_id") topicId: Int? = null,
        @Query("slug") slug: String? = null
    ): ApiEnvelope<HelpFlowDataDto>

    @POST("api/help/session.php")
    suspend fun submitHelpSession(@Body payload: HelpSessionRequest): ApiEnvelope<HelpSessionDataDto>

    @Multipart
    @POST("api/support/upload_media.php")
    suspend fun uploadSupportMedia(@Part image: MultipartBody.Part): ApiEnvelope<MediaUploadDataDto>

    @GET("api/bootstrap.php")
    suspend fun getBootstrapConfig(
        @Query("config_version") configVersion: String = "",
        @Query("country") country: String = "India",
        @Query("state") state: String = "",
        @Query("city") city: String = ""
    ): ApiEnvelope<BootstrapDataDto>

    @GET("api/discovery/world_map.php")
    suspend fun getWorldMapOrigins(
        @Query("world_slug") worldSlug: String? = null
    ): ApiEnvelope<WorldMapDataDto>

    @GET("api/offers/eligible.php")
    suspend fun getEligibleOffers(
        @Query("world_slug") worldSlug: String? = null,
        @Query("cart_subtotal") cartSubtotal: Double = 0.0
    ): ApiEnvelope<EligibleOffersDataDto>

    @POST("api/offers/impression.php")
    suspend fun recordOfferImpression(@Body payload: OfferImpressionPayload): ApiMessageResponse

    @GET("api/users/preferences.php")
    suspend fun getUserPreferences(): ApiEnvelope<UserDynamicPreferencesDto>

    @POST("api/users/preferences.php")
    suspend fun saveUserPreferences(@Body payload: UserDynamicPreferencesDto): ApiEnvelope<UserDynamicPreferencesDto>

    @GET("api/design/manifest.php")
    suspend fun getDesignManifest(
        @retrofit2.http.Header("If-None-Match") ifNoneMatch: String? = null,
        @Query("slot") slot: String = "live",
        @Query("schema_version") schemaVersion: String = "1.0.0",
        @Query("country") country: String = "India",
        @Query("state") state: String = "",
        @Query("city") city: String = "",
        @Query("world_slug") worldSlug: String = ""
    ): retrofit2.Response<ApiEnvelope<DesignManifestResponseDto>>

    @GET("api/design/manifest.php")
    suspend fun getDesignManifestRaw(
        @retrofit2.http.Header("If-None-Match") ifNoneMatch: String? = null,
        @Query("slot") slot: String = "live",
        @Query("schema_version") schemaVersion: String = "1.0.0",
        @Query("country") country: String = "India",
        @Query("state") state: String = "",
        @Query("city") city: String = "",
        @Query("world_slug") worldSlug: String = ""
    ): retrofit2.Response<okhttp3.ResponseBody>

    @GET("api/themes/config.php")
    suspend fun getThemesConfig(
        @Query("country") country: String = "India",
        @Query("state") state: String = "",
        @Query("city") city: String = ""
    ): ApiEnvelope<ThemesConfigDataDto>

    @GET("api/themes/active.php")
    suspend fun getActiveTheme(
        @Query("world_slug") worldSlug: String = "",
        @Query("country") country: String = "India",
        @Query("state") state: String = "",
        @Query("city") city: String = ""
    ): ApiEnvelope<ActiveThemeDataDto>

    @GET("api/occasions/active.php")
    suspend fun getActiveOccasions(
        @Query("country") country: String = "India",
        @Query("state") state: String = "",
        @Query("city") city: String = ""
    ): ApiEnvelope<OccasionsListDataDto>

    @GET("api/occasions/list.php")
    suspend fun getOccasionsList(): ApiEnvelope<OccasionsListDataDto>

    @GET("api/campaigns/active.php")
    suspend fun getActiveCampaigns(
        @Query("country") country: String = "India",
        @Query("state") state: String = "",
        @Query("city") city: String = "",
        @Query("pincode") pincode: String = ""
    ): ApiEnvelope<ActiveCampaignsDataDto>

    @GET("api/design/version.php")
    suspend fun getDesignVersion(): ApiEnvelope<DesignVersionDataDto>

    @GET("design-system/live/manifest.json")
    suspend fun getStaticLiveManifest(): com.example.model.DesignManifest

    @GET("design-system/live/manifest.json")
    suspend fun getStaticLiveManifestRaw(): retrofit2.Response<okhttp3.ResponseBody>

    @GET("design-system/default/manifest.json")
    suspend fun getStaticDefaultManifest(): com.example.model.DesignManifest

    @GET("index.php")
    suspend fun getIndexMetadata(): retrofit2.Response<okhttp3.ResponseBody>

    @GET("api/campaigns/details.php")
    suspend fun getCampaignDetails(
        @Query("id") campaignId: Int? = null,
        @Query("slug") slug: String? = null
    ): ApiEnvelope<CampaignDto>

    @GET("api/discovery/feed.php")
    suspend fun getDiscoveryFeed(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 24
    ): ApiEnvelope<HomeFeedDataDto>

    @GET("api/categories/list.php")
    suspend fun getCategoriesList(): CategoriesResponse

    @GET("api/categories/details.php")
    suspend fun getCategoryDetails(
        @Query("id") categoryId: Int? = null,
        @Query("slug") slug: String? = null
    ): ApiEnvelope<ProductsListDataDto>

    @GET("api/products/recently_viewed.php")
    suspend fun getRecentlyViewedProducts(
        @Query("limit") limit: Int = 16
    ): ApiEnvelope<ProductsListDataDto>

    @GET("api/videos/details.php")
    suspend fun getVideoDetails(
        @Query("id") videoId: Int
    ): ApiEnvelope<VideoReelDto>
}
