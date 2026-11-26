package com.example.data

import android.content.Context
import android.net.Uri
import com.example.model.BootstrapConfigBundle
import com.example.model.BrandDetailBundle
import com.example.model.BrandProfileItem
import com.example.model.DeliveryServiceability
import com.example.model.EliteHomeBundle
import com.example.model.ElitePackDetailBundle
import com.example.model.FaqItem
import com.example.model.FashionWorldTheme
import com.example.model.ForYouBundle
import com.example.model.HelpFlowDetail
import com.example.model.HelpSessionResult
import com.example.model.HelpTopic
import com.example.model.HomeFeedBundle
import com.example.model.Product
import com.example.model.ReviewListBundle
import com.example.model.ReviewOtpRequestState
import com.example.model.SearchFilterState
import com.example.model.SearchResultBundle
import com.example.model.SearchSuggestionsBundle
import com.example.model.SmartOffer
import com.example.model.StyleWorld
import com.example.model.StyleWorldDetail
import com.example.model.StyleWorldFeedbackResult
import com.example.model.ThemeTokenSpec
import com.example.model.UserDynamicPreferences
import com.example.model.VideoReel
import com.example.model.VideosFeedPage
import com.example.model.WorldMapOriginPin
import com.example.network.ApiResult
import com.example.network.CreateVerifiedReviewPayload
import com.example.network.DiscoveryMappers.toDomain
import com.example.network.DiscoveryMappers.toDto
import com.example.network.DtoMappers.toDomain
import com.example.network.HelpSessionRequest
import com.example.network.HelpfulReviewPayload
import com.example.network.MelvoreApiClient
import com.example.network.OfferImpressionPayload
import com.example.network.RequestReviewOtpPayload
import com.example.network.StyleSignalRequest
import com.example.network.StyleWorldFeedbackPayload
import com.example.network.VerifyReviewOtpPayload
import com.example.network.VideoEventRequest
import com.example.data.local.ProductCacheRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class DiscoveryRepository(
    private val context: Context,
    private val apiClient: MelvoreApiClient,
    private val sessionStore: SecureSessionStore,
    private val productCache: ProductCacheRepository? = null
) {

    private val manifestManager = RemoteDesignManifestManager(
        context = context.applicationContext,
        sessionStore = sessionStore
    )

    fun initialManifestOutcome(): ManifestResolutionOutcome =
        manifestManager.loadInitialManifest()

    fun savedPincode(): String = sessionStore.getDeliveryPincode()
    fun savedState(): String = sessionStore.getDeliveryState()
    fun savedCity(): String = sessionStore.getDeliveryCity()

    fun updateRegion(pincode: String, state: String = "", city: String = "") {
        sessionStore.saveDeliveryPincode(pincode, state, city)
    }

    fun recentSearches(): List<String> = sessionStore.getRecentSearches()
    fun recordLocalRecentSearch(query: String) = sessionStore.addRecentSearch(query)

    fun loadCachedBootstrap(): BootstrapConfigBundle? {
        val raw = sessionStore.getBootstrapConfigJson() ?: return null
        return runCatching {
            val decoded = apiClient.json.decodeFromString<BootstrapConfigBundle>(raw)
            decoded.copy(worldMapOrigins = decoded.worldMapOrigins.filter { it.isVerified })
        }.getOrNull()
    }

    fun defaultOfflineBootstrap(): BootstrapConfigBundle {
        val initialOutcome = initialManifestOutcome()
        val initialManifest = initialOutcome.manifest
        return loadCachedBootstrap()?.let { cached ->
            val manifest = cached.designManifest ?: initialManifest
            cached.copy(
                designManifest = manifest,
                fashionWorlds = mergeManifestWorlds(
                    dbWorlds = cached.fashionWorlds,
                    manifest = manifest
                )
            )
        } ?: BootstrapConfigBundle(
            configVersion = "offline-baseline-v1",
            activeDesign = initialOutcome.activeDesign,
            designSlot = initialOutcome.slot,
            designVersion = initialManifest.designVersion,
            designManifest = initialManifest,
            defaultTheme = ThemeTokenSpec(),
            fashionWorlds = mergeManifestWorlds(emptyList(), initialManifest)
        )
    }

    fun preferredWorldSlug(): String? = sessionStore.getPreferredWorldSlug()

    fun savePreferredWorldSlug(slug: String?) = sessionStore.savePreferredWorldSlug(slug)

    fun defaultAssetBaseUrl(): String = sessionStore.getApiBaseUrl().trimEnd('/')

    /**
     * Fetches the active remote design manifest from `api/design/manifest.php` using conditional
     * `If-None-Match` ETag headers ONLY when a valid cached manifest already exists locally.
     * - First install / empty cache: sends NO `If-None-Match`.
     * - HTTP 304 with valid cache: returns cached manifest (`ManifestSource.CACHED`).
     * - HTTP 304 with missing cache: retries once unconditionally without `If-None-Match`.
     * - HTTP 200 with `success: false` or HTTP 5xx: logs clearly, tries `design-system/live/manifest.json`
     *   (`ManifestSource.STATIC_LIVE_FILE`), and surfaces `ApiResult.Error` if both fail (never swallows 500).
     */
    suspend fun fetchDesignManifest(
        worldSlug: String? = null,
        slot: String = "live",
        forceRefresh: Boolean = false
    ): ApiResult<ManifestResolutionOutcome> {
        val hasValidCachedManifest = !sessionStore.getLastKnownGoodManifestJson().isNullOrBlank()
        val cachedEtag = if (!forceRefresh && hasValidCachedManifest) {
            sessionStore.getLastKnownGoodEtag().takeIf { it.isNotBlank() }
        } else {
            null
        }

        suspend fun requestApi(etagHeader: String?) = runCatching {
            apiClient.discoveryService().getDesignManifestRaw(
                ifNoneMatch = etagHeader,
                slot = slot,
                country = "India",
                state = sessionStore.getDeliveryState(),
                city = sessionStore.getDeliveryCity(),
                worldSlug = worldSlug.orEmpty()
            )
        }

        var rawCall = requestApi(cachedEtag)
        var httpResp = rawCall.getOrNull()

        if (httpResp != null && httpResp.code() == 304) {
            if (hasValidCachedManifest) {
                val cachedOutcome = manifestManager.loadInitialManifest().copy(
                    source = com.example.model.ManifestSource.CACHED
                )
                sessionStore.recordEndpointUpdated("design_manifest")
                runCatching {
                    android.util.Log.i(
                        "DiscoveryRepository",
                        "Design manifest branch=cached (HTTP 304, version=${cachedOutcome.manifestVersion}, etag=${cachedOutcome.etag})"
                    )
                }
                return ApiResult.Success(cachedOutcome)
            } else {
                // Server returned 304 even though we have no cached body; retry once unconditionally
                runCatching {
                    android.util.Log.w(
                        "DiscoveryRepository",
                        "HTTP 304 received with empty local manifest cache; retrying without If-None-Match"
                    )
                }
                rawCall = requestApi(null)
                httpResp = rawCall.getOrNull()
            }
        }

        val httpStatus = httpResp?.code()
        var apiErrorMsg: String? = null
        var apiErrorStatus: Int? = httpStatus

        val responseEtag = httpResp?.headers()?.get("ETag")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            .orEmpty()

        val rawBodyStr = runCatching {
            httpResp?.takeIf { it.isSuccessful }?.body()?.string()
        }.getOrNull()

        if (httpResp != null && !httpResp.isSuccessful) {
            val errSnippet = runCatching { httpResp.errorBody()?.string()?.take(200) }.getOrNull().orEmpty()
            apiErrorMsg = "api/design/manifest.php failed with HTTP $httpStatus" +
                if (errSnippet.isNotBlank()) ": $errSnippet" else ""
            runCatching { android.util.Log.w("DiscoveryRepository", apiErrorMsg) }
        }

        val rootObj = rawBodyStr?.let { body ->
            runCatching { apiClient.json.decodeFromString<kotlinx.serialization.json.JsonObject>(body) }.getOrNull()
        }

        val explicitSuccess = (rootObj?.get("success") as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.lowercase()?.let { s ->
                when (s) {
                    "true", "1" -> true
                    "false", "0" -> false
                    else -> null
                }
            }
        if (explicitSuccess == false) {
            val serverMsg = (rootObj?.get("message") as? kotlinx.serialization.json.JsonPrimitive)?.content?.trim().orEmpty()
            apiErrorMsg = serverMsg.ifBlank { "api/design/manifest.php returned success=false" }
            apiErrorStatus = httpStatus ?: 200
            runCatching { android.util.Log.w("DiscoveryRepository", "Design manifest API success=false: $apiErrorMsg") }
        }

        // Support both `{ "success": true, "data": { ... } }` envelope AND flat top-level keys
        val dataObj: kotlinx.serialization.json.JsonObject? = if (explicitSuccess == false) {
            null
        } else {
            rootObj?.let { root ->
                val dataEl = root["data"]
                if (dataEl is kotlinx.serialization.json.JsonObject) dataEl else root
            }
        }

        val unchanged = dataObj?.get("unchanged")?.let {
            runCatching { it.toString().equals("true", ignoreCase = true) }.getOrDefault(false)
        } ?: false
        if (unchanged && hasValidCachedManifest) {
            val cachedOutcome = manifestManager.loadInitialManifest().copy(
                source = com.example.model.ManifestSource.CACHED
            )
            sessionStore.recordEndpointUpdated("design_manifest")
            runCatching {
                android.util.Log.i(
                    "DiscoveryRepository",
                    "Design manifest branch=cached (unchanged=true, version=${cachedOutcome.manifestVersion})"
                )
            }
            return ApiResult.Success(cachedOutcome)
        }

        fun strField(key: String): String =
            (dataObj?.get(key) as? kotlinx.serialization.json.JsonPrimitive)?.content?.trim().orEmpty()

        val effectiveEtag = responseEtag.ifBlank { strField("etag") }
        val apiActiveDesign = (dataObj?.get("active_design") as? kotlinx.serialization.json.JsonPrimitive)
            ?.content?.toIntOrNull() ?: if (slot == "live") 1 else 0
        val resolvedAssetBase = strField("asset_base_url").ifBlank { defaultAssetBaseUrl() }
        val expectedChecksum = strField("checksum_sha256").takeIf { it.isNotBlank() }
        val resolvedHash = strField("manifest_hash").ifBlank { expectedChecksum.orEmpty() }

        val manifestJsonObj: kotlinx.serialization.json.JsonObject? = when (val m = dataObj?.get("manifest")) {
            is kotlinx.serialization.json.JsonObject -> m
            else -> if (dataObj?.containsKey("tokens") == true && dataObj.containsKey("pages")) dataObj else null
        }

        val apiOutcome = manifestJsonObj?.let { mObj ->
            manifestManager.validateAndAdoptObject(
                rawJsonObject = mObj,
                activeDesign = apiActiveDesign,
                expectedChecksum = expectedChecksum,
                persistIfValid = true,
                etag = effectiveEtag,
                assetBaseUrl = resolvedAssetBase,
                manifestHash = resolvedHash,
                sourceHint = com.example.model.ManifestSource.REMOTE
            )
        }

        val apiHasKits = apiOutcome?.manifest?.worlds?.values?.any { it.kit != null } == true
        if (apiOutcome != null && (apiActiveDesign == 0 || apiHasKits)) {
            sessionStore.recordEndpointUpdated("design_manifest")
            runCatching {
                android.util.Log.i(
                    "DiscoveryRepository",
                    "Design manifest branch=remote (version=${apiOutcome.manifestVersion}, etag=${apiOutcome.etag}, kits=${apiOutcome.manifest.worldKitCount()}, warnings=${apiOutcome.warnings.size})"
                )
            }
            return ApiResult.Success(apiOutcome)
        }

        // Direct fallback to static design-system/live/manifest.json hosted on the server
        val staticCall = runCatching {
            val staticResp = apiClient.discoveryService().getStaticLiveManifestRaw()
            val staticEtag = staticResp.headers()["ETag"]?.trim().orEmpty()
            val body = staticResp.takeIf { it.isSuccessful }?.body()?.string()
            val obj = body?.let { apiClient.json.decodeFromString<kotlinx.serialization.json.JsonObject>(it) }
            obj to staticEtag
        }
        val (staticLiveObj, staticEtag) = staticCall.getOrNull() ?: (null to "")

        if (staticLiveObj != null) {
            val staticOutcome = manifestManager.validateAndAdoptObject(
                rawJsonObject = staticLiveObj,
                activeDesign = 1,
                persistIfValid = true,
                etag = effectiveEtag.ifBlank { staticEtag },
                assetBaseUrl = resolvedAssetBase,
                manifestHash = resolvedHash,
                sourceHint = com.example.model.ManifestSource.STATIC_LIVE_FILE
            )
            if (apiOutcome == null ||
                com.example.model.DesignManifestSupport.isVersionNewer(
                    staticOutcome.manifest.designVersion,
                    apiOutcome.manifest.designVersion
                ) ||
                (!apiHasKits && staticOutcome.manifest.worlds.values.any { it.kit != null })
            ) {
                sessionStore.recordEndpointUpdated("design_manifest")
                runCatching {
                    android.util.Log.w(
                        "DiscoveryRepository",
                        "Design manifest branch=static-live-file (version=${staticOutcome.manifestVersion}, apiErr=${apiErrorMsg ?: "no kits in API payload"})"
                    )
                }
                return ApiResult.Success(
                    staticOutcome.copy(
                        usedFallback = true,
                        fallbackReason = staticOutcome.fallbackReason
                            ?: apiErrorMsg
                            ?: "Loaded from static design-system/live/manifest.json fallback"
                    )
                )
            }
        }

        if (apiOutcome != null) {
            sessionStore.recordEndpointUpdated("design_manifest")
            return ApiResult.Success(apiOutcome)
        }

        val finalError = apiErrorMsg
            ?: rawCall.exceptionOrNull()?.message
            ?: "Unable to fetch design manifest from API or static live file"
        runCatching {
            android.util.Log.w("DiscoveryRepository", "Design manifest branch=error: $finalError")
        }
        return ApiResult.Error(
            message = finalError,
            httpStatus = apiErrorStatus
        )
    }

    suspend fun fetchDesignKit(
        worldSlug: String? = null,
        forceRefresh: Boolean = false
    ): ApiResult<com.example.model.DesignKit> {
        return when (val res = fetchDesignManifest(worldSlug = worldSlug, forceRefresh = forceRefresh)) {
            is ApiResult.Success -> ApiResult.Success(res.data.toDesignKit(defaultAssetBaseUrl()))
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchThemesConfig(): ApiResult<List<FashionWorldTheme>> {
        return when (val res = apiClient.safeDiscoveryCall {
            it.getThemesConfig(
                country = "India",
                state = sessionStore.getDeliveryState(),
                city = sessionStore.getDeliveryCity()
            )
        }) {
            is ApiResult.Success -> {
                val dto = res.data.data
                val rawWorlds = (dto?.worlds.orEmpty().ifEmpty { dto?.fashionWorlds.orEmpty() })
                    .map { it.toDomain() }
                val merged = mergeManifestWorlds(rawWorlds, initialManifestOutcome().manifest)
                sessionStore.recordEndpointUpdated("themes_config")
                ApiResult.Success(merged)
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchActiveTheme(worldSlug: String? = null): ApiResult<Pair<com.example.model.OccasionTheme?, FashionWorldTheme?>> {
        return when (val res = apiClient.safeDiscoveryCall {
            it.getActiveTheme(
                worldSlug = worldSlug.orEmpty(),
                country = "India",
                state = sessionStore.getDeliveryState(),
                city = sessionStore.getDeliveryCity()
            )
        }) {
            is ApiResult.Success -> {
                val dto = res.data.data
                sessionStore.recordEndpointUpdated("themes_active")
                ApiResult.Success(dto?.activeOccasion?.toDomain() to dto?.selectedWorld?.toDomain())
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchActiveOccasions(): ApiResult<List<com.example.model.OccasionTheme>> {
        return when (val res = apiClient.safeDiscoveryCall {
            it.getActiveOccasions(
                country = "India",
                state = sessionStore.getDeliveryState(),
                city = sessionStore.getDeliveryCity()
            )
        }) {
            is ApiResult.Success -> {
                val dto = res.data.data
                val list = dto?.occasions?.map { it.toDomain() }.orEmpty()
                val combined = if (list.isEmpty() && dto?.activeOccasion != null) {
                    listOf(dto.activeOccasion.toDomain())
                } else {
                    list
                }
                sessionStore.recordEndpointUpdated("occasions_active")
                ApiResult.Success(combined)
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchAllOccasions(): ApiResult<List<com.example.model.OccasionTheme>> {
        return when (val res = apiClient.safeDiscoveryCall { it.getOccasionsList() }) {
            is ApiResult.Success -> {
                sessionStore.recordEndpointUpdated("occasions_list")
                ApiResult.Success(res.data.data?.occasions?.map { it.toDomain() }.orEmpty())
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchActiveCampaigns(): ApiResult<List<com.example.model.CampaignItem>> {
        return when (val res = apiClient.safeDiscoveryCall {
            it.getActiveCampaigns(
                country = "India",
                state = sessionStore.getDeliveryState(),
                city = sessionStore.getDeliveryCity(),
                pincode = sessionStore.getDeliveryPincode()
            )
        }) {
            is ApiResult.Success -> {
                sessionStore.recordEndpointUpdated("campaigns_active")
                ApiResult.Success(res.data.data?.campaigns?.map { it.toDomain() }.orEmpty())
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchBootstrapConfig(forceRefresh: Boolean = false): ApiResult<BootstrapConfigBundle> {
        val cached = loadCachedBootstrap()
        val versionQuery = if (forceRefresh || cached == null) "" else sessionStore.getBootstrapConfigVersion()
        return when (val res = apiClient.safeDiscoveryCall {
            it.getBootstrapConfig(
                configVersion = versionQuery,
                country = "India",
                state = sessionStore.getDeliveryState(),
                city = sessionStore.getDeliveryCity()
            )
        }) {
            is ApiResult.Success -> {
                val dto = res.data.data
                if (dto == null) {
                    cached?.let { ApiResult.Success(it) }
                        ?: ApiResult.Error(res.data.message ?: "Bootstrap config unavailable")
                } else if (dto.unchanged && cached != null) {
                    val refreshed = cached.copy(
                        userPreferences = dto.userPreferences?.toDomain() ?: cached.userPreferences,
                        submittedWorldIds = dto.submittedWorldIds.ifEmpty { cached.submittedWorldIds },
                        submittedWorldSlugs = dto.submittedWorldSlugs.ifEmpty { cached.submittedWorldSlugs }
                    )
                    sessionStore.syncSubmittedWorldsFromBootstrap(
                        worldIds = refreshed.submittedWorldIds,
                        worldSlugs = refreshed.submittedWorldSlugs,
                        replaceExisting = dto.submittedWorldIds.isNotEmpty() || dto.submittedWorldSlugs.isNotEmpty()
                    )
                    ApiResult.Success(refreshed)
                } else {
                    val rawMapped = dto.toDomain()
                    val bootstrapManifest = rawMapped.designManifest
                    val bootstrapHasKits = bootstrapManifest?.worlds?.values?.any { it.kit != null } == true

                    val manifestOutcome = when {
                        bootstrapManifest != null && (rawMapped.activeDesign == 0 || bootstrapHasKits) -> {
                            manifestManager.validateAndAdopt(
                                remoteManifest = bootstrapManifest,
                                activeDesign = rawMapped.activeDesign,
                                persistIfValid = true
                            )
                        }
                        else -> {
                            // Try direct server manifest (api/design/manifest.php -> design-system/live/manifest.json)
                            val fallbackRes = fetchDesignManifest()
                            if (fallbackRes is ApiResult.Success) {
                                fallbackRes.data
                            } else if (bootstrapManifest != null) {
                                manifestManager.validateAndAdopt(
                                    remoteManifest = bootstrapManifest,
                                    activeDesign = rawMapped.activeDesign,
                                    persistIfValid = true
                                )
                            } else {
                                manifestManager.loadInitialManifest()
                            }
                        }
                    }

                    val mergedWorlds = mergeManifestWorlds(rawMapped.fashionWorlds, manifestOutcome.manifest)
                    val mapped = rawMapped.copy(
                        activeDesign = manifestOutcome.activeDesign,
                        designSlot = manifestOutcome.slot,
                        designVersion = manifestOutcome.manifest.designVersion,
                        designManifest = manifestOutcome.manifest,
                        fashionWorlds = mergedWorlds
                    )
                    sessionStore.syncSubmittedWorldsFromBootstrap(
                        worldIds = mapped.submittedWorldIds,
                        worldSlugs = mapped.submittedWorldSlugs,
                        replaceExisting = true
                    )
                    runCatching {
                        val serialized = apiClient.json.encodeToString(BootstrapConfigBundle.serializer(), mapped)
                        sessionStore.saveBootstrapConfigJson(mapped.configVersion, serialized)
                    }
                    ApiResult.Success(mapped)
                }
            }
            is ApiResult.Error -> {
                // Even if api/bootstrap.php failed, still try static design-system/live/manifest.json once if online
                val staticLive = (apiClient.safeDiscoveryCall { it.getStaticLiveManifest() } as? ApiResult.Success)?.data
                if (staticLive != null) {
                    val outcome = manifestManager.validateAndAdopt(
                        remoteManifest = staticLive,
                        activeDesign = 1,
                        persistIfValid = true,
                        sourceHint = com.example.model.ManifestSource.STATIC_LIVE_FILE
                    )
                    val base = cached ?: defaultOfflineBootstrap()
                    val upgraded = base.copy(
                        activeDesign = outcome.activeDesign,
                        designSlot = outcome.slot,
                        designVersion = outcome.manifest.designVersion,
                        designManifest = outcome.manifest,
                        fashionWorlds = mergeManifestWorlds(base.fashionWorlds, outcome.manifest)
                    )
                    ApiResult.Success(upgraded)
                } else if (cached != null) {
                    runCatching {
                        android.util.Log.w("DiscoveryRepository", "Bootstrap API failed (${res.message}); using cached bootstrap.")
                    }
                    ApiResult.Success(cached)
                } else {
                    res
                }
            }
        }
    }

    suspend fun fetchWorldMapOrigins(worldSlug: String? = null): ApiResult<List<WorldMapOriginPin>> {
        return when (val res = apiClient.safeDiscoveryCall { it.getWorldMapOrigins(worldSlug) }) {
            is ApiResult.Success -> {
                val verified = res.data.data?.pins?.map { it.toDomain() }?.filter { it.isVerified }.orEmpty()
                ApiResult.Success(verified)
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchEligibleOffers(worldSlug: String?, cartSubtotal: Double): ApiResult<List<SmartOffer>> {
        return when (val res = apiClient.safeDiscoveryCall { it.getEligibleOffers(worldSlug, cartSubtotal) }) {
            is ApiResult.Success -> ApiResult.Success(res.data.data?.offers?.map { it.toDomain() }.orEmpty())
            is ApiResult.Error -> res
        }
    }

    suspend fun recordOfferImpression(offerId: Int, actionType: String) {
        if (offerId <= 0) return
        apiClient.safeDiscoveryCall {
            it.recordOfferImpression(OfferImpressionPayload(offerId = offerId, actionType = actionType))
        }
    }

    suspend fun fetchUserDynamicPreferences(): ApiResult<UserDynamicPreferences> {
        return when (val res = apiClient.safeDiscoveryCall { it.getUserPreferences() }) {
            is ApiResult.Success -> {
                val mapped = res.data.data?.toDomain()
                if (mapped != null) ApiResult.Success(mapped)
                else ApiResult.Error("Preferences unavailable")
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun saveUserDynamicPreferences(prefs: UserDynamicPreferences): ApiResult<UserDynamicPreferences> {
        return when (val res = apiClient.safeDiscoveryCall { it.saveUserPreferences(prefs.toDto()) }) {
            is ApiResult.Success -> ApiResult.Success(res.data.data?.toDomain() ?: prefs)
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchHomeFeed(): ApiResult<HomeFeedBundle> {
        return when (val res = apiClient.safeDiscoveryCall {
            it.getHomeFeed(
                country = "India",
                state = sessionStore.getDeliveryState(),
                city = sessionStore.getDeliveryCity(),
                pincode = sessionStore.getDeliveryPincode()
            )
        }) {
            is ApiResult.Success -> {
                val data = res.data.data
                if (data != null) {
                    val mapped = data.toDomain()
                    sessionStore.recordEndpointUpdated("home_feed")
                    runCatching {
                        val allFeedProducts = (
                            mapped.discoveryProducts +
                                mapped.newArrivals +
                                mapped.bestsellers +
                                mapped.trendingProducts +
                                mapped.mostWishlisted +
                                mapped.budget999 +
                                mapped.budget1999 +
                                mapped.sponsoredProducts +
                                mapped.recentlyViewed +
                                mapped.sections.flatMap { sec -> sec.products }
                            ).distinctBy { it.id }
                        if (allFeedProducts.isNotEmpty()) {
                            productCache?.cacheProducts(allFeedProducts, isDetailed = false)
                        }
                        if (mapped.categories.isNotEmpty()) {
                            productCache?.cacheCategories(mapped.categories)
                        }
                    }
                    ApiResult.Success(mapped)
                } else {
                    ApiResult.Error(res.data.message ?: "Homepage feed unavailable")
                }
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchBrands(tab: String = "all"): ApiResult<List<BrandProfileItem>> {
        return when (val res = apiClient.safeDiscoveryCall { it.getBrandsDirectory(tab) }) {
            is ApiResult.Success -> ApiResult.Success(res.data.data?.brands?.map { it.toDomain() }.orEmpty())
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchBrandDetails(brandId: Int?, slug: String?): ApiResult<BrandDetailBundle> {
        return when (val res = apiClient.safeDiscoveryCall { it.getBrandDetails(brandId, slug) }) {
            is ApiResult.Success -> {
                val mapped = res.data.data?.toDomain()
                if (mapped != null) {
                    runCatching {
                        val brandProducts = (
                            mapped.featuredProducts +
                                mapped.newArrivals +
                                mapped.bestsellers +
                                mapped.sponsoredProducts
                            ).distinctBy { it.id }
                        if (brandProducts.isNotEmpty()) {
                            productCache?.cacheProducts(brandProducts, isDetailed = false)
                        }
                    }
                    ApiResult.Success(mapped)
                } else {
                    ApiResult.Error(res.data.message ?: "Brand details unavailable")
                }
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun recordBrandView(brandId: Int) {
        apiClient.safeDiscoveryCall { it.recordBrandView(mapOf("brand_id" to brandId)) }
    }

    suspend fun fetchStyleWorlds(): ApiResult<List<StyleWorld>> {
        return when (val res = apiClient.safeDiscoveryCall { it.getStyleWorlds() }) {
            is ApiResult.Success -> ApiResult.Success(res.data.data?.styleWorlds?.map { it.toDomain() }.orEmpty())
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchStyleWorldDetails(styleId: Int?, slug: String?, page: Int = 1): ApiResult<StyleWorldDetail> {
        return when (val res = apiClient.safeDiscoveryCall { it.getStyleWorldDetails(styleId, slug, page) }) {
            is ApiResult.Success -> {
                val mapped = res.data.data?.toDomain()
                if (mapped != null) {
                    if (mapped.styleWorld.hasSubmittedFeedback || mapped.satisfactionStatus?.hasSubmitted == true) {
                        sessionStore.recordSubmittedFeedbackWorld(
                            worldId = mapped.styleWorld.styleWorldId,
                            worldSlug = mapped.styleWorld.slug
                        )
                    }
                    runCatching {
                        if (mapped.products.isNotEmpty()) {
                            productCache?.cacheProducts(mapped.products, isDetailed = false)
                        }
                    }
                    ApiResult.Success(mapped)
                } else {
                    buildOfflineWorldDetailFallback(styleId, slug)
                        ?: ApiResult.Error(res.data.message ?: "Style world unavailable")
                }
            }
            is ApiResult.Error -> {
                buildOfflineWorldDetailFallback(styleId, slug) ?: res
            }
        }
    }

    private suspend fun buildOfflineWorldDetailFallback(styleId: Int?, slug: String?): ApiResult<StyleWorldDetail>? {
        val cleanSlug = slug?.trim()?.lowercase().orEmpty()
        val cachedBoot = loadCachedBootstrap() ?: defaultOfflineBootstrap()
        val matchedWorld = cachedBoot.fashionWorlds.firstOrNull { w ->
            (cleanSlug.isNotBlank() && w.slug.equals(cleanSlug, ignoreCase = true)) ||
                (styleId != null && styleId > 0 && w.styleWorldId == styleId)
        } ?: return null

        val cachedProducts = runCatching {
            productCache?.queryCachedProducts(limit = 24).orEmpty()
        }.getOrDefault(emptyList())

        val styleWorld = StyleWorld(
            styleWorldId = matchedWorld.styleWorldId,
            slug = matchedWorld.slug,
            name = matchedWorld.name,
            tagline = matchedWorld.tagline,
            description = matchedWorld.description,
            themeKey = matchedWorld.themeKey,
            animationKey = matchedWorld.animationKey,
            fontStyle = matchedWorld.fontStyle,
            buttonStyle = matchedWorld.buttonStyle,
            primaryColor = matchedWorld.primaryColor,
            accentColor = matchedWorld.accentColor,
            backgroundColor = matchedWorld.backgroundColor,
            coverImageUrl = matchedWorld.coverImageUrl,
            mobileImageUrl = matchedWorld.mobileImageUrl,
            videoUrl = matchedWorld.videoUrl,
            promoVideoUrl = matchedWorld.promoVideoUrl,
            promoVideoPosterUrl = matchedWorld.promoVideoPosterUrl,
            audioUrl = matchedWorld.audioUrl,
            audioLoop = matchedWorld.audioLoop,
            audioVolume = matchedWorld.audioVolume,
            productCount = cachedProducts.size,
            hasSubmittedFeedback = hasSubmittedWorldFeedbackLocally(matchedWorld.styleWorldId, matchedWorld.slug)
        )
        return ApiResult.Success(
            StyleWorldDetail(
                styleWorld = styleWorld,
                products = cachedProducts
            )
        )
    }

    companion object {
        /**
         * Merges DB-backed FashionWorldTheme items with any worlds declared in the active
         * DesignManifest (e.g., all 12 v1.2.0 server worlds including Pixel Arcade, Grunge,
         * Old Money, Quiet Luxury, Minimal, Casual, Party Wear). DB rows always take priority
         * and preserve their IDs and display order; missing cover images are backfilled from kit.heroImage.
         */
        fun mergeManifestWorlds(
            dbWorlds: List<FashionWorldTheme>,
            manifest: com.example.model.DesignManifest?
        ): List<FashionWorldTheme> {
            val manifestWorlds = manifest?.worlds.orEmpty()
            if (manifestWorlds.isEmpty()) return dbWorlds

            val enrichedDb = dbWorlds.map { w ->
                val slug = w.slug.trim().lowercase()
                val override = manifestWorlds[slug]
                val kit = override?.kit
                if (kit == null) {
                    w
                } else {
                    val hero = kit.heroImage.takeIf { it.isNotBlank() }
                    w.copy(
                        coverImageUrl = w.coverImageUrl?.takeIf { it.isNotBlank() } ?: hero,
                        mobileImageUrl = w.mobileImageUrl?.takeIf { it.isNotBlank() } ?: hero,
                        buttonStyle = kit.buttonSkin.takeIf { it.isNotBlank() } ?: w.buttonStyle
                    )
                }
            }

            val existingSlugs = enrichedDb.map { it.slug.trim().lowercase() }.toSet()
            var nextOrder = (enrichedDb.maxOfOrNull { it.displayOrder } ?: enrichedDb.size) + 1
            val synthesized = manifestWorlds.entries.mapNotNull { (rawSlug, spec) ->
                val slug = rawSlug.trim().lowercase()
                if (slug.isBlank() || slug in existingSlugs) return@mapNotNull null
                val kit = spec.kit
                val pal = kit?.palette
                val typo = kit?.typography
                val displayName = slug.split("-", "_")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { part ->
                        if (part.equals("y2k", ignoreCase = true)) "Y2K"
                        else part.replaceFirstChar { it.uppercase() }
                    }
                val mode = pal?.mode?.lowercase()?.takeIf { it == "light" || it == "dark" } ?: "dark"
                val hero = kit?.heroImage?.takeIf { it.isNotBlank() }
                FashionWorldTheme(
                    styleWorldId = 0,
                    slug = slug,
                    name = displayName,
                    tagline = spec.template.replace('_', ' ').replaceFirstChar { it.uppercase() },
                    themeKey = spec.template,
                    mode = mode,
                    animationKey = kit?.motionPreset?.ifBlank { "smooth_reveal" } ?: "smooth_reveal",
                    fontStyle = typo?.displayFont?.ifBlank { spec.fontStyle } ?: spec.fontStyle,
                    buttonStyle = kit?.buttonSkin?.ifBlank { spec.buttonStyle } ?: spec.buttonStyle,
                    cardRadiusDp = spec.cardRadiusDp,
                    primaryColor = pal?.textPrimary?.ifBlank { if (mode == "light") "#09090B" else "#FFFFFF" }
                        ?: if (mode == "light") "#09090B" else "#FFFFFF",
                    accentColor = pal?.accent?.ifBlank { "#D4AF37" } ?: "#D4AF37",
                    secondaryAccent = pal?.secondaryAccent?.ifBlank { "#94A3B8" } ?: "#94A3B8",
                    backgroundColor = pal?.background?.ifBlank { if (mode == "light") "#FAF8F5" else "#0E0D1B" }
                        ?: if (mode == "light") "#FAF8F5" else "#0E0D1B",
                    surfaceColor = pal?.surface?.ifBlank { if (mode == "light") "#FFFFFF" else "#181632" }
                        ?: if (mode == "light") "#FFFFFF" else "#181632",
                    surfaceElevated = pal?.surface?.ifBlank { if (mode == "light") "#F4F4F5" else "#232042" }
                        ?: if (mode == "light") "#F4F4F5" else "#232042",
                    textPrimary = pal?.textPrimary?.ifBlank { if (mode == "light") "#09090B" else "#FFFFFF" }
                        ?: if (mode == "light") "#09090B" else "#FFFFFF",
                    textSecondary = pal?.textSecondary?.ifBlank { "#94A3B8" } ?: "#94A3B8",
                    borderColor = pal?.border?.ifBlank { "#334155" } ?: "#334155",
                    ambientPreset = slug,
                    coverImageUrl = hero,
                    mobileImageUrl = hero,
                    displayOrder = nextOrder++
                )
            }
            return enrichedDb + synthesized
        }
    }

    suspend fun fetchStyleWorldFeedback(styleWorldId: Int? = null, worldSlug: String? = null): ApiResult<StyleWorldFeedbackResult> {
        return when (val res = apiClient.safeDiscoveryCall { it.getStyleWorldFeedback(styleWorldId, worldSlug) }) {
            is ApiResult.Success -> {
                val mapped = res.data.data?.toDomain() ?: StyleWorldFeedbackResult()
                sessionStore.syncSubmittedWorldsFromBootstrap(
                    worldIds = mapped.submittedWorldIds,
                    worldSlugs = mapped.submittedWorldSlugs
                )
                if (mapped.hasSubmitted && mapped.feedback != null) {
                    sessionStore.recordSubmittedFeedbackWorld(
                        worldId = mapped.feedback.styleWorldId,
                        worldSlug = mapped.feedback.worldSlug
                    )
                }
                ApiResult.Success(mapped)
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun submitStyleWorldFeedback(
        styleWorldId: Int,
        worldSlug: String,
        satisfactionScore: Int
    ): ApiResult<StyleWorldFeedbackResult> {
        return when (val res = apiClient.safeDiscoveryCall {
            it.submitStyleWorldFeedback(
                StyleWorldFeedbackPayload(
                    styleWorldId = styleWorldId,
                    worldSlug = worldSlug,
                    satisfactionScore = satisfactionScore
                )
            )
        }) {
            is ApiResult.Success -> {
                val mapped = res.data.data?.toDomain() ?: StyleWorldFeedbackResult()
                if (mapped.hasSubmitted) {
                    sessionStore.recordSubmittedFeedbackWorld(
                        worldId = mapped.feedback?.styleWorldId ?: styleWorldId,
                        worldSlug = mapped.feedback?.worldSlug ?: worldSlug
                    )
                }
                ApiResult.Success(mapped)
            }
            is ApiResult.Error -> res
        }
    }

    fun hasSubmittedWorldFeedbackLocally(worldId: Int, worldSlug: String): Boolean =
        sessionStore.hasSubmittedFeedbackWorld(worldId, worldSlug)

    suspend fun recordStyleSignal(
        styleWorldId: Int? = null,
        productId: Int? = null,
        categoryId: Int? = null,
        signalType: String = "style_tap"
    ) {
        apiClient.safeDiscoveryCall {
            it.recordStyleSignal(StyleSignalRequest(styleWorldId, productId, categoryId, signalType))
        }
    }

    suspend fun fetchVideosFeed(page: Int = 1, brandId: Int? = null, styleWorldId: Int? = null): ApiResult<List<VideoReel>> {
        return when (val res = fetchVideosFeedPage(page = page, brandId = brandId, styleWorldId = styleWorldId)) {
            is ApiResult.Success -> ApiResult.Success(res.data.videos)
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchVideosFeedPage(page: Int = 1, brandId: Int? = null, styleWorldId: Int? = null): ApiResult<VideosFeedPage> {
        return when (val res = apiClient.safeDiscoveryCall { it.getVideosFeed(page = page, brandId = brandId, styleWorldId = styleWorldId) }) {
            is ApiResult.Success -> {
                val mapped = res.data.data?.toDomain() ?: VideosFeedPage(page = page)
                ApiResult.Success(mapped)
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun recordVideoEvent(videoId: Int, eventType: String, watchSeconds: Int = 0): ApiResult<Triple<Int, Int, Int>> {
        return when (val res = apiClient.safeDiscoveryCall { it.recordVideoEvent(VideoEventRequest(videoId, eventType, watchSeconds)) }) {
            is ApiResult.Success -> {
                val d = res.data.data
                ApiResult.Success(Triple(d?.likesCount ?: 0, d?.savesCount ?: 0, d?.sharesCount ?: 0))
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchEliteHome(): ApiResult<EliteHomeBundle> {
        return when (val res = apiClient.safeDiscoveryCall { it.getEliteHome() }) {
            is ApiResult.Success -> ApiResult.Success(res.data.data?.toDomain() ?: EliteHomeBundle())
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchElitePack(packId: Int?, slug: String?): ApiResult<ElitePackDetailBundle> {
        return when (val res = apiClient.safeDiscoveryCall { it.getElitePack(packId, slug) }) {
            is ApiResult.Success -> {
                val mapped = res.data.data?.toDomain()
                if (mapped != null) ApiResult.Success(mapped)
                else ApiResult.Error(res.data.message ?: "Elite pack unavailable")
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchForYou(): ApiResult<ForYouBundle> {
        return when (val res = apiClient.safeDiscoveryCall { it.getForYou() }) {
            is ApiResult.Success -> {
                val mapped = res.data.data?.toDomain() ?: ForYouBundle()
                runCatching {
                    if (mapped.products.isNotEmpty()) {
                        productCache?.cacheProducts(mapped.products, isDetailed = false)
                    }
                }
                ApiResult.Success(mapped)
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchProductRecommendations(
        productId: Int,
        worldSlug: String? = null
    ): ApiResult<Pair<List<Product>, List<Product>>> {
        val cleanSlug = worldSlug?.trim()?.takeIf { it.isNotBlank() }
        return when (val res = apiClient.safeDiscoveryCall {
            it.getProductRecommendations(productId = productId, worldSlug = cleanSlug)
        }) {
            is ApiResult.Success -> {
                val d = res.data.data
                val primary = (d?.products?.takeIf { it.isNotEmpty() } ?: d?.similarProducts.orEmpty())
                    .map { it.toDomain() }
                val secondary = d?.moreFromBrand?.map { it.toDomain() }.orEmpty()
                runCatching {
                    val combined = (primary + secondary).distinctBy { it.id }
                    if (combined.isNotEmpty()) {
                        productCache?.cacheProducts(combined, isDetailed = false)
                    }
                }
                ApiResult.Success(primary to secondary)
            }
            is ApiResult.Error -> {
                val cached = runCatching {
                    val target = productCache?.getCachedProductById(productId)
                    val sameCategory = productCache?.queryCachedProducts(
                        category = target?.categorySlug?.ifBlank { target.category },
                        limit = 10
                    ).orEmpty().filter { it.id != productId }
                    val sameBrand = target?.brandId?.let { bid ->
                        productCache?.queryCachedProducts(brandId = bid, limit = 8)
                            .orEmpty()
                            .filter { it.id != productId }
                    }.orEmpty()
                    sameCategory to sameBrand
                }.getOrNull()
                if (cached != null && (cached.first.isNotEmpty() || cached.second.isNotEmpty())) {
                    ApiResult.Success(cached)
                } else {
                    res
                }
            }
        }
    }

    suspend fun searchCatalog(query: String, filters: SearchFilterState, page: Int = 1): ApiResult<SearchResultBundle> {
        if (query.trim().length >= 2) {
            sessionStore.addRecentSearch(query.trim())
        }
        return when (val res = apiClient.safeDiscoveryCall {
            it.searchCatalog(
                query = query,
                page = page,
                brandId = filters.brandId,
                category = filters.categorySlug,
                size = filters.size,
                color = filters.color,
                minPrice = filters.minPrice,
                maxPrice = filters.maxPrice,
                sort = filters.sort
            )
        }) {
            is ApiResult.Success -> {
                val mapped = res.data.data?.toDomain() ?: SearchResultBundle(query = query)
                runCatching {
                    if (mapped.products.isNotEmpty()) {
                        productCache?.cacheProducts(mapped.products, isDetailed = false)
                    }
                }
                ApiResult.Success(mapped)
            }
            is ApiResult.Error -> {
                val cacheRepo = productCache
                val hasCached = runCatching { cacheRepo?.hasCachedProducts() == true }.getOrDefault(false)
                if (cacheRepo != null && hasCached) {
                    val offlineMatches = runCatching {
                        cacheRepo.queryCachedProducts(
                            category = filters.categorySlug,
                            brandId = filters.brandId,
                            search = query,
                            sort = filters.sort,
                            minPrice = filters.minPrice,
                            maxPrice = filters.maxPrice,
                            limit = 60
                        )
                    }.getOrDefault(emptyList())
                    ApiResult.Success(
                        SearchResultBundle(
                            query = query,
                            products = offlineMatches,
                            page = 1,
                            total = offlineMatches.size,
                            totalPages = 1
                        )
                    )
                } else {
                    res
                }
            }
        }
    }

    suspend fun fetchSearchSuggestions(query: String): ApiResult<SearchSuggestionsBundle> {
        return when (val res = apiClient.safeDiscoveryCall { it.getSearchSuggestions(query) }) {
            is ApiResult.Success -> {
                val d = res.data.data
                val mergedRecent = ((d?.recent.orEmpty()) + sessionStore.getRecentSearches()).distinct().take(8)
                ApiResult.Success(
                    SearchSuggestionsBundle(
                        suggestions = d?.suggestions.orEmpty(),
                        trending = d?.trending.orEmpty(),
                        recent = mergedRecent
                    )
                )
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun checkDeliveryServiceability(pincode: String, productId: Int? = null): ApiResult<DeliveryServiceability> {
        return when (val res = apiClient.safeDiscoveryCall { it.checkDeliveryServiceability(pincode, productId) }) {
            is ApiResult.Success -> {
                val d = res.data.data?.toDomain()
                if (d != null) {
                    if (d.isServiceable) {
                        sessionStore.saveDeliveryPincode(d.pincode, d.state, d.city)
                    }
                    ApiResult.Success(d)
                } else {
                    ApiResult.Error("Serviceability check unavailable")
                }
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchProductReviews(productId: Int, page: Int = 1): ApiResult<ReviewListBundle> {
        return when (val res = apiClient.safeDiscoveryCall { it.getProductReviews(productId, page) }) {
            is ApiResult.Success -> ApiResult.Success(res.data.data?.toDomain() ?: ReviewListBundle())
            is ApiResult.Error -> res
        }
    }

    suspend fun requestReviewOtp(orderItemId: Int): ApiResult<ReviewOtpRequestState> {
        return when (val res = apiClient.safeDiscoveryCall { it.requestReviewOtp(RequestReviewOtpPayload(orderItemId)) }) {
            is ApiResult.Success -> {
                val d = res.data.data
                if (d != null) {
                    ApiResult.Success(
                        ReviewOtpRequestState(
                            otpRequestId = d.otpRequestId,
                            orderItemId = d.orderItemId,
                            productId = d.productId,
                            sentTo = d.sentTo,
                            expiresInSeconds = d.expiresInSeconds,
                            resendCooldownSeconds = d.resendCooldownSeconds
                        )
                    )
                } else {
                    ApiResult.Error(res.data.message ?: "Unable to request review OTP")
                }
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun verifyReviewOtp(otpRequestId: Int, otp: String): ApiResult<Boolean> {
        return when (val res = apiClient.safeDiscoveryCall { it.verifyReviewOtp(VerifyReviewOtpPayload(otpRequestId, otp)) }) {
            is ApiResult.Success -> ApiResult.Success(res.data.data?.verified == true)
            is ApiResult.Error -> res
        }
    }

    suspend fun createVerifiedReview(
        orderItemId: Int,
        otpRequestId: Int,
        rating: Int,
        title: String,
        comment: String,
        mediaUrls: List<String>
    ): ApiResult<String> {
        return when (val res = apiClient.safeDiscoveryCall {
            it.createVerifiedReview(
                CreateVerifiedReviewPayload(
                    orderItemId = orderItemId,
                    otpRequestId = otpRequestId,
                    rating = rating,
                    title = title,
                    comment = comment,
                    mediaUrls = mediaUrls
                )
            )
        }) {
            is ApiResult.Success -> ApiResult.Success(res.data.message ?: "Review submitted")
            is ApiResult.Error -> res
        }
    }

    suspend fun toggleReviewHelpful(reviewId: Int): ApiResult<Pair<Int, Boolean>> {
        return when (val res = apiClient.safeDiscoveryCall { it.toggleReviewHelpful(HelpfulReviewPayload(reviewId)) }) {
            is ApiResult.Success -> {
                val d = res.data.data
                ApiResult.Success((d?.helpfulCount ?: 0) to (d?.hasVotedHelpful == true))
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun uploadImageUri(uri: Uri, isSupportEvidence: Boolean): ApiResult<String> {
        val part = runCatching {
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return ApiResult.Error("Unable to read selected photo")
            val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("image", "upload_${System.currentTimeMillis()}.jpg", body)
        }.getOrElse {
            return ApiResult.Error("Could not read selected image")
        }

        return when (val res = apiClient.safeDiscoveryCall {
            if (isSupportEvidence) it.uploadSupportMedia(part) else it.uploadReviewMedia(part)
        }) {
            is ApiResult.Success -> {
                val url = res.data.data?.mediaUrl.orEmpty()
                if (url.isNotBlank()) ApiResult.Success(url)
                else ApiResult.Error("Upload succeeded without URL")
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchHelpTopicsAndFaqs(): ApiResult<Pair<List<HelpTopic>, List<FaqItem>>> {
        return when (val res = apiClient.safeDiscoveryCall { it.getHelpTopics() }) {
            is ApiResult.Success -> {
                val d = res.data.data
                ApiResult.Success(
                    (d?.topics?.map { it.toDomain() }.orEmpty()) to
                        (d?.faqs?.map { it.toDomain() }.orEmpty())
                )
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun fetchHelpFlow(topicId: Int?, slug: String?): ApiResult<HelpFlowDetail> {
        return when (val res = apiClient.safeDiscoveryCall { it.getHelpFlow(topicId, slug) }) {
            is ApiResult.Success -> {
                val mapped = res.data.data?.toDomain()
                if (mapped != null) ApiResult.Success(mapped)
                else ApiResult.Error(res.data.message ?: "Help flow unavailable")
            }
            is ApiResult.Error -> res
        }
    }

    suspend fun submitHelpSession(request: HelpSessionRequest): ApiResult<HelpSessionResult> {
        return when (val res = apiClient.safeDiscoveryCall { it.submitHelpSession(request) }) {
            is ApiResult.Success -> {
                val mapped = res.data.data?.toDomain()
                if (mapped != null) ApiResult.Success(mapped)
                else ApiResult.Error(res.data.message ?: "Help session failed")
            }
            is ApiResult.Error -> res
        }
    }
}

