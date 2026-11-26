package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import com.example.data.DiscoveryRepository
import com.example.data.MelvoreRepository
import com.example.data.RepoResult
import com.example.model.BootstrapConfigBundle
import com.example.model.BrandDetailBundle
import com.example.model.BrandProfileItem
import com.example.model.DeliveryServiceability
import com.example.model.DesignKit
import com.example.model.DesignManifest
import com.example.model.EliteHomeBundle
import com.example.model.ElitePackDetailBundle
import com.example.model.FaqItem
import com.example.model.FashionWorldContext
import com.example.model.FashionWorldTheme
import com.example.model.ForYouBundle
import com.example.model.HelpFlowDetail
import com.example.model.HelpSessionResult
import com.example.model.HelpTopic
import com.example.model.HomeFeedBundle
import com.example.model.LocalPreferences
import com.example.model.ManifestSource
import com.example.model.OccasionTheme
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
import com.example.model.UserDynamicPreferences
import com.example.model.VideoReel
import com.example.model.WorldMapOriginPin
import com.example.network.ApiResult
import com.example.network.HelpSessionRequest
import com.example.ui.effects.WorldAudioManager
import com.example.ui.theme.CampaignExperienceController
import com.example.ui.theme.FashionWorldThemeResolver
import com.example.ui.theme.FashionWorldTransitionController
import com.example.ui.theme.ResolvedFashionWorldTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoveryUiState(
    val savedPincode: String = "",
    val savedCity: String = "",
    val isFeedLoading: Boolean = false,
    val homeFeed: HomeFeedBundle = HomeFeedBundle(),
    val extraFeedProducts: List<Product> = emptyList(),
    val feedPage: Int = 1,
    val canLoadMoreFeed: Boolean = true,
    val isLoadingMoreFeed: Boolean = false,

    val bootstrapConfig: BootstrapConfigBundle = BootstrapConfigBundle(),
    val activeManifest: DesignManifest = DesignManifest(),
    val designKit: DesignKit = DesignKit(),
    val activeDesignFlag: Int = 0,
    val manifestSource: ManifestSource = ManifestSource.BUNDLED,
    val manifestVersion: String = "",
    val manifestEtag: String = "",
    val manifestHash: String = "",
    val manifestFallbackReason: String? = null,
    val manifestWarnings: List<String> = emptyList(),
    val worldContext: FashionWorldContext = FashionWorldContext(),
    val preferredWorldSlug: String? = null,
    val selectedHomeOccasionSlug: String? = null,
    val availableOccasions: List<OccasionTheme> = emptyList(),
    val isWorldPickerOpen: Boolean = false,
    val isDarkMode: Boolean = false,
    val resolvedTheme: ResolvedFashionWorldTheme = ResolvedFashionWorldTheme(),
    val worldMapOrigins: List<WorldMapOriginPin> = emptyList(),
    val eligibleOffer: SmartOffer? = null,
    val pendingAudioConsentWorldName: String? = null,
    val isSensorySheetOpen: Boolean = false,
    val isAppForeground: Boolean = true,
    val isVideoMediaPlayingAudio: Boolean = false,
    val currentRouteName: String = "HomeRoute",

    val isDiscoverLoading: Boolean = false,
    val forYou: ForYouBundle = ForYouBundle(),
    val activeBrandTab: String = "featured",
    val directoryBrands: List<BrandProfileItem> = emptyList(),
    val styleWorlds: List<StyleWorld> = emptyList(),

    val isBrandDetailLoading: Boolean = false,
    val brandDetailError: String? = null,
    val brandDetail: BrandDetailBundle? = null,

    val isStyleDetailLoading: Boolean = false,
    val styleDetailError: String? = null,
    val styleDetail: StyleWorldDetail? = null,
    val isSubmittingWorldFeedback: Boolean = false,
    val unlockedWorldFeedbackResult: StyleWorldFeedbackResult? = null,
    val hasActiveWorldFeedbackSubmitted: Boolean = false,
    val styleWorldScrollPositions: Map<String, Pair<Int, Int>> = emptyMap(),

    val isVideosLoading: Boolean = false,
    val isLoadingMoreVideos: Boolean = false,
    val videosError: String? = null,
    val videos: List<VideoReel> = emptyList(),
    val videosPage: Int = 1,
    val videosTotalPages: Int = 1,
    val canLoadMoreVideos: Boolean = false,
    val videosDiagnosticsReason: String = "",
    val videosTableExists: Boolean = true,

    val recommendedSimilarProducts: List<Product> = emptyList(),
    val recommendedBrandProducts: List<Product> = emptyList(),

    val isEliteLoading: Boolean = false,
    val eliteError: String? = null,
    val eliteHome: EliteHomeBundle = EliteHomeBundle(),
    val isElitePackLoading: Boolean = false,
    val elitePackDetail: ElitePackDetailBundle? = null,

    val searchQuery: String = "",
    val searchFilters: SearchFilterState = SearchFilterState(),
    val isSearchLoading: Boolean = false,
    val searchSuggestions: SearchSuggestionsBundle = SearchSuggestionsBundle(),
    val searchResults: SearchResultBundle = SearchResultBundle(),

    val isCheckingDelivery: Boolean = false,
    val deliveryCheck: DeliveryServiceability? = null,

    val isReviewsLoading: Boolean = false,
    val reviewList: ReviewListBundle = ReviewListBundle(),
    val isReviewOtpBusy: Boolean = false,
    val reviewOtpState: ReviewOtpRequestState? = null,
    val isReviewOtpVerified: Boolean = false,
    val isReviewUploading: Boolean = false,
    val uploadedReviewPhotoUrls: List<String> = emptyList(),
    val isSubmittingReview: Boolean = false,

    val isHelpLoading: Boolean = false,
    val helpTopics: List<HelpTopic> = emptyList(),
    val helpFaqs: List<FaqItem> = emptyList(),
    val isHelpFlowLoading: Boolean = false,
    val activeHelpFlow: HelpFlowDetail? = null,
    val isHelpSessionBusy: Boolean = false,
    val activeHelpSession: HelpSessionResult? = null,
    val isSupportPhotoUploading: Boolean = false,
    val supportPhotoUrls: List<String> = emptyList()
)

class DiscoveryViewModelDelegate(
    context: Context,
    private val mainRepository: MelvoreRepository,
    private val scope: CoroutineScope,
    private val showToast: (String) -> Unit
) {
    private val repo = DiscoveryRepository(
        context = context.applicationContext,
        apiClient = mainRepository.apiClient,
        sessionStore = mainRepository.sessionStore,
        productCache = mainRepository.productCache
    )

    private val worldAudioManager = WorldAudioManager(context.applicationContext)
    private val offerSessionShownCounts = mutableMapOf<Int, Int>()
    private var lastOfferRecordedId: Int? = null

    private val initialBootstrap = repo.defaultOfflineBootstrap()
    private val initialManifestOutcome = repo.initialManifestOutcome()
    private val initialPreferredWorld = repo.preferredWorldSlug()
    private val initialDesignKit = initialManifestOutcome.toDesignKit(repo.defaultAssetBaseUrl())
    private val _state = MutableStateFlow(
        DiscoveryUiState(
            savedPincode = repo.savedPincode(),
            savedCity = repo.savedCity(),
            bootstrapConfig = initialBootstrap,
            activeManifest = initialManifestOutcome.manifest,
            designKit = initialDesignKit,
            activeDesignFlag = initialManifestOutcome.activeDesign,
            manifestSource = initialManifestOutcome.source,
            manifestVersion = initialManifestOutcome.manifestVersion,
            manifestEtag = initialManifestOutcome.etag,
            manifestHash = initialManifestOutcome.manifestHash,
            manifestFallbackReason = initialManifestOutcome.fallbackReason,
            manifestWarnings = initialManifestOutcome.warnings,
            preferredWorldSlug = initialPreferredWorld,
            availableOccasions = listOfNotNull(initialBootstrap.activeOccasion),
            resolvedTheme = FashionWorldThemeResolver.resolveWithPrecedence(
                manifest = initialBootstrap.designManifest ?: initialManifestOutcome.manifest,
                fallbackTheme = initialBootstrap.defaultTheme,
                activeOccasion = initialBootstrap.activeOccasion,
                selectedWorld = initialBootstrap.fashionWorlds.find {
                    it.slug.equals(initialPreferredWorld, ignoreCase = true)
                }
            ),
            worldMapOrigins = initialBootstrap.worldMapOrigins.filter { it.isVerified },
            searchSuggestions = SearchSuggestionsBundle(recent = repo.recentSearches())
        )
    )
    val state: StateFlow<DiscoveryUiState> = _state.asStateFlow()

    private var searchDebounceJob: Job? = null
    private var suggestDebounceJob: Job? = null
    private var periodicDesignRefreshJob: Job? = null

    fun startPeriodicDesignRefresh(intervalMs: Long = 300_000L) {
        if (periodicDesignRefreshJob?.isActive == true) return
        periodicDesignRefreshJob = scope.launch {
            while (true) {
                delay(intervalMs)
                if (_state.value.isAppForeground) {
                    refreshAllDesignEndpoints(forceManifest = false)
                }
            }
        }
    }

    /**
     * Synchronizes all DB-driven design, theme, occasion, and campaign endpoints in parallel
     * without requiring an app restart.
     */
    fun refreshAllDesignEndpoints(forceManifest: Boolean = false) {
        refreshRemoteDesignManifest(forceRefresh = forceManifest)
        loadBootstrapConfig(forceRefresh = forceManifest)
        loadOccasionsAndCampaigns()
        refreshThemesAndActiveTheme()
    }

    fun refreshThemesAndActiveTheme() {
        scope.launch {
            val themesRes = repo.fetchThemesConfig()
            val activeSlug = _state.value.worldContext.activeWorldSlug ?: _state.value.preferredWorldSlug
            val activeThemeRes = repo.fetchActiveTheme(activeSlug)
            val worldsFromConfig = (themesRes as? ApiResult.Success)?.data.orEmpty()
            val activePair = (activeThemeRes as? ApiResult.Success)?.data
            if (worldsFromConfig.isNotEmpty() || activePair != null) {
                _state.update { prev ->
                    val updatedWorlds = if (worldsFromConfig.isNotEmpty()) worldsFromConfig else prev.bootstrapConfig.fashionWorlds
                    val updatedOccasion = activePair?.first ?: prev.bootstrapConfig.activeOccasion
                    val updatedBundle = prev.bootstrapConfig.copy(
                        fashionWorlds = updatedWorlds,
                        activeOccasion = updatedOccasion
                    )
                    prev.copy(
                        bootstrapConfig = updatedBundle,
                        resolvedTheme = computeResolvedTheme(
                            bundle = updatedBundle,
                            worldContext = prev.worldContext,
                            homeFeedCampaign = prev.homeFeed.activeCampaign,
                            routeName = prev.currentRouteName
                        )
                    )
                }
            }
        }
    }

    fun setDarkMode(darkMode: Boolean) {
        if (_state.value.isDarkMode == darkMode) return
        _state.update { prev ->
            prev.copy(
                isDarkMode = darkMode,
                resolvedTheme = computeResolvedTheme(
                    bundle = prev.bootstrapConfig,
                    worldContext = prev.worldContext,
                    homeFeedCampaign = prev.homeFeed.activeCampaign,
                    routeName = prev.currentRouteName,
                    preferredWorldSlugOverride = prev.preferredWorldSlug,
                    selectedHomeOccasionSlugOverride = prev.selectedHomeOccasionSlug,
                    availableOccasionsOverride = prev.availableOccasions,
                    isDarkModeOverride = darkMode
                )
            )
        }
    }

    fun openWorldPicker() {
        _state.update { it.copy(isWorldPickerOpen = true) }
        scope.launch {
            when (val res = repo.fetchThemesConfig()) {
                is ApiResult.Success -> {
                    val worlds = res.data
                    if (worlds.isNotEmpty()) {
                        _state.update { prev ->
                            val mergedBundle = prev.bootstrapConfig.copy(
                                fashionWorlds = worlds
                            )
                            prev.copy(
                                bootstrapConfig = mergedBundle,
                                resolvedTheme = computeResolvedTheme(
                                    bundle = mergedBundle,
                                    worldContext = prev.worldContext,
                                    homeFeedCampaign = prev.homeFeed.activeCampaign,
                                    routeName = prev.currentRouteName
                                )
                            )
                        }
                    }
                }
                is ApiResult.Error -> Unit
            }
        }
    }

    fun closeWorldPicker() {
        _state.update { it.copy(isWorldPickerOpen = false) }
    }

    fun selectPreferredWorld(
        slug: String?,
        preferences: LocalPreferences = mainRepository.getPreferences(),
        onPreferenceUpdated: ((String?) -> Unit)? = null
    ) {
        val clean = slug?.trim()?.lowercase()?.takeIf { it.isNotBlank() && it != "default" }
        repo.savePreferredWorldSlug(clean)
        onPreferenceUpdated?.invoke(clean)
        _state.update { prev ->
            prev.copy(
                preferredWorldSlug = clean,
                isWorldPickerOpen = false,
                resolvedTheme = computeResolvedTheme(
                    bundle = prev.bootstrapConfig,
                    worldContext = prev.worldContext,
                    homeFeedCampaign = prev.homeFeed.activeCampaign,
                    routeName = prev.currentRouteName,
                    preferredWorldSlugOverride = clean
                )
            )
        }
        if (clean != null) {
            scope.launch {
                repo.fetchActiveTheme(clean)
                repo.recordStyleSignal(
                    styleWorldId = findWorldBySlug(_state.value.bootstrapConfig, clean)?.styleWorldId,
                    signalType = "world_select"
                )
            }
        }
        syncAudioState(preferences)
    }

    fun selectHomeOccasionTab(occasionSlug: String?) {
        val clean = occasionSlug?.trim()?.lowercase()?.takeIf { it.isNotBlank() && it != "all" }
        _state.update { prev ->
            prev.copy(
                selectedHomeOccasionSlug = clean,
                resolvedTheme = computeResolvedTheme(
                    bundle = prev.bootstrapConfig,
                    worldContext = prev.worldContext,
                    homeFeedCampaign = prev.homeFeed.activeCampaign,
                    routeName = prev.currentRouteName,
                    selectedHomeOccasionSlugOverride = clean
                )
            )
        }
    }

    fun loadOccasionsAndCampaigns() {
        scope.launch {
            val occDef = async { repo.fetchAllOccasions() }
            val activeOccDef = async { repo.fetchActiveOccasions() }
            val activeCampDef = async { repo.fetchActiveCampaigns() }
            val occList = (occDef.await() as? ApiResult.Success)?.data.orEmpty()
            val activeList = (activeOccDef.await() as? ApiResult.Success)?.data.orEmpty()
            val campaigns = (activeCampDef.await() as? ApiResult.Success)?.data.orEmpty()
            val primaryActiveOcc = activeList.firstOrNull()
            val primaryCampaign = campaigns.firstOrNull()
            _state.update { prev ->
                val mergedOccasions = buildList {
                    addAll(activeList)
                    prev.bootstrapConfig.activeOccasion?.let { add(it) }
                    addAll(occList)
                }.distinctBy { it.slug.lowercase() }
                val updatedBundle = if (primaryActiveOcc != null && prev.bootstrapConfig.activeOccasion == null) {
                    prev.bootstrapConfig.copy(activeOccasion = primaryActiveOcc)
                } else prev.bootstrapConfig
                val updatedHomeFeed = if (primaryCampaign != null && prev.homeFeed.activeCampaign == null) {
                    prev.homeFeed.copy(activeCampaign = primaryCampaign)
                } else prev.homeFeed
                prev.copy(
                    bootstrapConfig = updatedBundle,
                    homeFeed = updatedHomeFeed,
                    availableOccasions = mergedOccasions,
                    resolvedTheme = computeResolvedTheme(
                        bundle = updatedBundle,
                        worldContext = prev.worldContext,
                        homeFeedCampaign = updatedHomeFeed.activeCampaign,
                        routeName = prev.currentRouteName,
                        availableOccasionsOverride = mergedOccasions
                    )
                )
            }
        }
    }

    fun loadBootstrapConfig(
        forceRefresh: Boolean = false,
        cartSubtotal: Double = 0.0,
        localPreferences: LocalPreferences = mainRepository.getPreferences(),
        onServerPreferencesSynced: ((UserDynamicPreferences) -> Unit)? = null
    ) {
        scope.launch {
            val previousConfigVersion = _state.value.bootstrapConfig.configVersion
            when (val res = repo.fetchBootstrapConfig(forceRefresh = forceRefresh)) {
                is ApiResult.Success -> {
                    val bundle = res.data
                    bundle.userPreferences?.let { onServerPreferencesSynced?.invoke(it) }
                    _state.update { prev ->
                        val nextManifest = bundle.designManifest ?: prev.activeManifest
                        val nextKit = prev.designKit.copy(manifest = nextManifest)
                        val mergedOccasions = buildList {
                            bundle.activeOccasion?.let { add(it) }
                            addAll(prev.availableOccasions)
                        }.distinctBy { it.slug.lowercase() }
                        val nextTheme = computeResolvedTheme(
                            bundle = bundle,
                            worldContext = prev.worldContext,
                            homeFeedCampaign = prev.homeFeed.activeCampaign,
                            routeName = prev.currentRouteName,
                            manifestOverride = nextManifest,
                            availableOccasionsOverride = mergedOccasions
                        )
                        prev.copy(
                            bootstrapConfig = bundle,
                            activeManifest = nextManifest,
                            designKit = nextKit,
                            activeDesignFlag = bundle.activeDesign,
                            manifestFallbackReason = null,
                            availableOccasions = mergedOccasions,
                            worldMapOrigins = bundle.worldMapOrigins.filter { it.isVerified },
                            resolvedTheme = nextTheme
                        )
                    }
                    if (bundle.configVersion.isNotBlank() && bundle.configVersion != previousConfigVersion) {
                        refreshRemoteDesignManifest()
                    }
                    refreshEligibleOffer(cartSubtotal)
                    syncAudioState(localPreferences)
                }
                is ApiResult.Error -> {
                    if (_state.value.worldMapOrigins.isEmpty()) {
                        when (val mapRes = repo.fetchWorldMapOrigins()) {
                            is ApiResult.Success -> _state.update { it.copy(worldMapOrigins = mapRes.data) }
                            is ApiResult.Error -> Unit
                        }
                    }
                }
            }
        }
    }

    fun refreshRemoteDesignManifest(forceRefresh: Boolean = false) {
        scope.launch {
            val activeWorldSlug = _state.value.worldContext.activeWorldSlug ?: _state.value.preferredWorldSlug
            when (val res = repo.fetchDesignManifest(worldSlug = activeWorldSlug, forceRefresh = forceRefresh)) {
                is ApiResult.Success -> {
                    val outcome = res.data
                    val nextKit = outcome.toDesignKit(repo.defaultAssetBaseUrl())
                    _state.update { prev ->
                        val updatedBundle = prev.bootstrapConfig.copy(
                            activeDesign = outcome.activeDesign,
                            designSlot = outcome.slot,
                            designVersion = outcome.manifest.designVersion,
                            designManifest = outcome.manifest,
                            fashionWorlds = DiscoveryRepository.mergeManifestWorlds(
                                prev.bootstrapConfig.fashionWorlds,
                                outcome.manifest
                            )
                        )
                        prev.copy(
                            bootstrapConfig = updatedBundle,
                            activeManifest = outcome.manifest,
                            designKit = nextKit,
                            activeDesignFlag = outcome.activeDesign,
                            manifestSource = outcome.source,
                            manifestVersion = outcome.manifestVersion,
                            manifestEtag = outcome.etag,
                            manifestHash = outcome.manifestHash,
                            manifestFallbackReason = outcome.fallbackReason,
                            manifestWarnings = outcome.warnings,
                            resolvedTheme = computeResolvedTheme(
                                bundle = updatedBundle,
                                worldContext = prev.worldContext,
                                homeFeedCampaign = prev.homeFeed.activeCampaign,
                                routeName = prev.currentRouteName,
                                manifestOverride = outcome.manifest
                            )
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update { prev ->
                        prev.copy(
                            manifestFallbackReason = prev.manifestFallbackReason ?: res.message
                        )
                    }
                }
            }
        }
    }

    fun enterFashionWorld(
        slug: String,
        worldId: Int? = null,
        cartSubtotal: Double = 0.0,
        preferences: LocalPreferences = mainRepository.getPreferences()
    ) {
        val prevState = _state.value
        val nextContext = FashionWorldTransitionController.enterWorld(prevState.worldContext, slug, worldId)
        val matchedWorld = findWorldBySlug(prevState.bootstrapConfig, nextContext.activeWorldSlug)
        val nextTheme = computeResolvedTheme(
            bundle = prevState.bootstrapConfig,
            worldContext = nextContext,
            homeFeedCampaign = prevState.homeFeed.activeCampaign,
            routeName = prevState.currentRouteName
        )
        val hasAudio = !nextTheme.audioUrl.isNullOrBlank() ||
            (nextTheme.audioTrackKey.isNotBlank() && nextTheme.audioTrackKey != "none")
        val promptConsent = if (hasAudio && !preferences.audioConsentAnswered) {
            matchedWorld?.name ?: nextTheme.worldName.ifBlank { slug.uppercase() }
        } else {
            prevState.pendingAudioConsentWorldName
        }

        val resolvedWorldId = worldId ?: matchedWorld?.styleWorldId ?: 0
        val cleanWorldSlug = nextContext.activeWorldSlug.orEmpty().ifBlank { slug.trim().lowercase() }
        val alreadySubmitted = (matchedWorld?.hasSubmittedFeedback == true) ||
            (resolvedWorldId > 0 && resolvedWorldId in prevState.bootstrapConfig.submittedWorldIds) ||
            prevState.bootstrapConfig.submittedWorldSlugs.any { it.equals(cleanWorldSlug, ignoreCase = true) } ||
            repo.hasSubmittedWorldFeedbackLocally(resolvedWorldId, cleanWorldSlug)

        _state.update {
            it.copy(
                worldContext = nextContext,
                resolvedTheme = nextTheme,
                pendingAudioConsentWorldName = promptConsent,
                hasActiveWorldFeedbackSubmitted = alreadySubmitted,
                unlockedWorldFeedbackResult = null
            )
        }
        worldAudioManager.playUiSfx(
            sfxProfile = nextTheme.sfxProfile,
            sfxType = "enter_world",
            uiSfxEnabled = preferences.uiSfxEnabled,
            consentAnswered = preferences.audioConsentAnswered
        )
        syncAudioState(preferences)
        refreshEligibleOffer(cartSubtotal)
        scope.launch {
            repo.recordStyleSignal(styleWorldId = worldId ?: matchedWorld?.styleWorldId, signalType = "world_enter")
        }
    }

    fun exitFashionWorld(
        cartSubtotal: Double = 0.0,
        preferences: LocalPreferences = mainRepository.getPreferences()
    ) {
        val nextContext = FashionWorldTransitionController.exitWorld()
        _state.update { prev ->
            prev.copy(
                worldContext = nextContext,
                resolvedTheme = computeResolvedTheme(
                    bundle = prev.bootstrapConfig,
                    worldContext = nextContext,
                    homeFeedCampaign = prev.homeFeed.activeCampaign,
                    routeName = prev.currentRouteName
                ),
                pendingAudioConsentWorldName = null,
                unlockedWorldFeedbackResult = null,
                isVideoMediaPlayingAudio = false
            )
        }
        worldAudioManager.pauseAllAmbient()
        refreshEligibleOffer(cartSubtotal)
    }

    fun setScrollPreviewWorld(worldSlug: String?) {
        val prev = _state.value
        val nextContext = FashionWorldTransitionController.updateScrollPreview(prev.worldContext, worldSlug)
        if (nextContext == prev.worldContext) return
        _state.update { cur ->
            cur.copy(
                worldContext = nextContext,
                resolvedTheme = computeResolvedTheme(
                    bundle = cur.bootstrapConfig,
                    worldContext = nextContext,
                    homeFeedCampaign = cur.homeFeed.activeCampaign,
                    routeName = cur.currentRouteName
                )
            )
        }
    }

    fun onRouteChanged(
        simpleRouteName: String,
        cartSubtotal: Double,
        preferences: LocalPreferences
    ) {
        val prev = _state.value
        val nextContext = FashionWorldTransitionController.onRouteChanged(prev.worldContext, simpleRouteName)
        val nextTheme = computeResolvedTheme(
            bundle = prev.bootstrapConfig,
            worldContext = nextContext,
            homeFeedCampaign = prev.homeFeed.activeCampaign,
            routeName = simpleRouteName
        )
        _state.update {
            it.copy(
                currentRouteName = simpleRouteName,
                worldContext = nextContext,
                resolvedTheme = nextTheme
            )
        }
        syncAudioState(preferences)
        refreshEligibleOffer(cartSubtotal)
    }

    fun onLifecycleForegroundChanged(isForeground: Boolean, preferences: LocalPreferences) {
        _state.update { it.copy(isAppForeground = isForeground) }
        if (!isForeground) {
            worldAudioManager.pauseAllAmbient()
        } else {
            syncAudioState(preferences)
        }
    }

    fun setVideoMediaPlayingAudio(isPlayingAudio: Boolean, preferences: LocalPreferences = mainRepository.getPreferences()) {
        if (_state.value.isVideoMediaPlayingAudio == isPlayingAudio) return
        _state.update { it.copy(isVideoMediaPlayingAudio = isPlayingAudio) }
        syncAudioState(preferences)
    }

    fun syncPreferencesWithBackend(preferences: LocalPreferences) {
        syncAudioState(preferences)
        scope.launch {
            repo.saveUserDynamicPreferences(
                UserDynamicPreferences(
                    ambientAudioEnabled = preferences.ambientAudioEnabled,
                    uiSfxEnabled = preferences.uiSfxEnabled,
                    reducedMotion = preferences.reducedMotion,
                    videoAutoplayMuted = preferences.videoAutoplayMuted,
                    dataSaverMode = preferences.dataSaverMode,
                    audioConsentAnswered = preferences.audioConsentAnswered,
                    preferredWorldSlug = preferences.preferredWorldSlug
                )
            )
        }
    }

    fun clearAudioConsentPrompt() {
        _state.update { it.copy(pendingAudioConsentWorldName = null) }
    }

    fun setSensorySheetOpen(open: Boolean) {
        _state.update { it.copy(isSensorySheetOpen = open) }
    }

    fun playInteractionSfx(sfxType: String = "tap", preferences: LocalPreferences = mainRepository.getPreferences()) {
        worldAudioManager.playUiSfx(
            sfxProfile = _state.value.resolvedTheme.sfxProfile,
            sfxType = sfxType,
            uiSfxEnabled = preferences.uiSfxEnabled,
            consentAnswered = preferences.audioConsentAnswered
        )
    }

    fun refreshEligibleOffer(cartSubtotal: Double) {
        val cur = _state.value
        val offer = CampaignExperienceController.selectEligibleOffer(
            offers = cur.bootstrapConfig.smartOffers,
            activeWorldSlug = cur.worldContext.effectiveWorldSlug,
            activeOccasionSlug = cur.bootstrapConfig.activeOccasion?.slug,
            cartSubtotal = cartSubtotal,
            simpleRouteName = cur.currentRouteName,
            shownInSessionById = offerSessionShownCounts,
            canShowByPersistence = { candidate, sessionCount ->
                mainRepository.sessionStore.canShowOffer(
                    offerId = candidate.offerId,
                    maxPerSession = candidate.maxImpressionsPerSession,
                    sessionShownCount = sessionCount
                )
            }
        )
        _state.update { it.copy(eligibleOffer = offer) }
        if (offer != null && lastOfferRecordedId != offer.offerId) {
            lastOfferRecordedId = offer.offerId
            offerSessionShownCounts[offer.offerId] = (offerSessionShownCounts[offer.offerId] ?: 0) + 1
            scope.launch { repo.recordOfferImpression(offer.offerId, "shown") }
        }
    }

    fun dismissSmartOffer(offer: SmartOffer) {
        mainRepository.sessionStore.recordOfferCooldown(
            offerId = offer.offerId,
            cooldownHours = offer.cooldownHours,
            dismissedForever = false
        )
        _state.update { it.copy(eligibleOffer = null) }
        scope.launch { repo.recordOfferImpression(offer.offerId, "dismissed") }
    }

    fun applySmartOffer(
        offer: SmartOffer,
        preferences: LocalPreferences,
        onApplyCouponCode: (String) -> Unit
    ) {
        mainRepository.sessionStore.recordOfferCooldown(
            offerId = offer.offerId,
            cooldownHours = offer.cooldownHours,
            dismissedForever = false
        )
        worldAudioManager.playUiSfx(
            sfxProfile = _state.value.resolvedTheme.sfxProfile,
            sfxType = "offer",
            uiSfxEnabled = preferences.uiSfxEnabled,
            consentAnswered = preferences.audioConsentAnswered
        )
        _state.update { it.copy(eligibleOffer = null) }
        if (offer.couponCode.isNotBlank()) {
            onApplyCouponCode(offer.couponCode)
        }
        scope.launch { repo.recordOfferImpression(offer.offerId, "applied") }
    }

    fun release() {
        periodicDesignRefreshJob?.cancel()
        worldAudioManager.release()
    }

    private fun syncAudioState(preferences: LocalPreferences) {
        val cur = _state.value
        val theme = cur.resolvedTheme
        worldAudioManager.sync(
            audioUrl = theme.audioUrl,
            trackKey = theme.audioTrackKey,
            loop = theme.audioLoop,
            volume = theme.audioVolume,
            ambientAudioEnabled = preferences.ambientAudioEnabled,
            consentAnswered = preferences.audioConsentAnswered,
            isWorldOrOccasionActive = (theme.isWorldActive || theme.sourceLayer == "occasion") && !theme.isLuxeIsolated,
            isAppForeground = cur.isAppForeground,
            isVideoPlayingAudio = cur.isVideoMediaPlayingAudio
        )
    }

    private fun findWorldBySlug(bundle: BootstrapConfigBundle, slug: String?): FashionWorldTheme? {
        if (slug.isNullOrBlank()) return null
        return bundle.fashionWorlds.find { it.slug.equals(slug, ignoreCase = true) }
    }

    private fun computeResolvedTheme(
        bundle: BootstrapConfigBundle,
        worldContext: FashionWorldContext,
        homeFeedCampaign: com.example.model.CampaignItem?,
        routeName: String,
        manifestOverride: DesignManifest? = null,
        preferredWorldSlugOverride: String? = null,
        selectedHomeOccasionSlugOverride: String? = null,
        availableOccasionsOverride: List<OccasionTheme>? = null,
        isDarkModeOverride: Boolean? = null
    ): ResolvedFashionWorldTheme {
        val cur = _state.value
        val isLuxe = routeName == "EliteRoute" || routeName == "ElitePackRoute"
        val isNeutralRoute = routeName == "CheckoutRoute" || routeName == "AuthRoute"
        val effectivePreferredSlug = preferredWorldSlugOverride ?: cur.preferredWorldSlug
        val effectiveWorldSlug = worldContext.activeWorldSlug ?: effectivePreferredSlug
        val selectedWorld = findWorldBySlug(bundle, effectiveWorldSlug)
        val previewWorld = if (selectedWorld == null) findWorldBySlug(bundle, worldContext.scrollPreviewWorldSlug) else null
        val manifest = manifestOverride ?: bundle.designManifest ?: cur.activeManifest
        val occasions = availableOccasionsOverride ?: cur.availableOccasions
        val homeOccasionSlug = selectedHomeOccasionSlugOverride ?: cur.selectedHomeOccasionSlug
        val effectiveOccasion = if (!homeOccasionSlug.isNullOrBlank() && routeName == "HomeRoute") {
            occasions.find { it.slug.equals(homeOccasionSlug, ignoreCase = true) }
                ?: bundle.activeOccasion?.takeIf { it.slug.equals(homeOccasionSlug, ignoreCase = true) }
                ?: manifest.occasionOverrides.occasions[homeOccasionSlug]?.let { kit ->
                    val label = homeOccasionSlug.replace("-", " ").replace("_", " ")
                        .replaceFirstChar { it.uppercase() }
                    OccasionTheme(
                        slug = homeOccasionSlug,
                        name = label,
                        headline = label,
                        primaryColor = kit.colors.primary.ifBlank { bundle.defaultTheme.primaryColor },
                        accentColor = kit.colors.accent.ifBlank { bundle.defaultTheme.accentColor },
                        backgroundColor = kit.colors.background.ifBlank { bundle.defaultTheme.backgroundColor },
                        motifKey = kit.motif
                    )
                }
                ?: bundle.activeOccasion
        } else {
            bundle.activeOccasion
        }
        val darkMode = isDarkModeOverride ?: cur.isDarkMode

        val isImmersiveWorld = routeName == "StyleWorldRoute" && worldContext.activeWorldSlug != null

        // Single unified priority resolver across the entire app:
        // active occasion > selected world > remote manifest default tokens > bundled default.
        // Dark world mode only activates when user is in dark mode OR inside StyleWorldRoute.
        return FashionWorldThemeResolver.resolveWithPrecedence(
            manifest = manifest,
            fallbackTheme = bundle.defaultTheme,
            activeOccasion = effectiveOccasion,
            selectedWorld = selectedWorld,
            scrollPreviewWorld = previewWorld,
            activeCampaign = homeFeedCampaign,
            preferDarkMode = darkMode,
            isLuxeRoute = isLuxe,
            isNeutralCheckoutOrAuthRoute = isNeutralRoute,
            isImmersiveWorldRoute = isImmersiveWorld
        )
    }

    fun loadHomeFeed() {
        scope.launch {
            _state.update { it.copy(isFeedLoading = true) }
            when (val res = repo.fetchHomeFeed()) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            isFeedLoading = false,
                            homeFeed = res.data,
                            feedPage = 1,
                            canLoadMoreFeed = res.data.discoveryProducts.size >= 12,
                            resolvedTheme = computeResolvedTheme(
                                bundle = it.bootstrapConfig,
                                worldContext = it.worldContext,
                                homeFeedCampaign = res.data.activeCampaign,
                                routeName = it.currentRouteName
                            )
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isFeedLoading = false) }
                }
            }
        }
    }

    fun loadMoreHomeProducts() {
        val current = _state.value
        if (current.isLoadingMoreFeed || !current.canLoadMoreFeed) return
        scope.launch {
            _state.update { it.copy(isLoadingMoreFeed = true) }
            val nextPage = current.feedPage + 1
            when (val res = mainRepository.fetchProducts(page = nextPage, limit = 16)) {
                is RepoResult.Success -> {
                    val existingIds = (current.homeFeed.discoveryProducts + current.extraFeedProducts)
                        .map { it.id }
                        .toSet()
                    val fresh = res.data.filterNot { it.id in existingIds }
                    _state.update {
                        it.copy(
                            isLoadingMoreFeed = false,
                            extraFeedProducts = it.extraFeedProducts + fresh,
                            feedPage = nextPage,
                            canLoadMoreFeed = res.data.size >= 16
                        )
                    }
                }
                is RepoResult.Error -> _state.update { it.copy(isLoadingMoreFeed = false) }
            }
        }
    }

    fun updatePincode(pincode: String, state: String = "", city: String = "") {
        val cleanPin = pincode.filter { it.isDigit() }.take(6)
        if (cleanPin.length != 6) {
            showToast("Enter a valid 6-digit Indian pincode")
            return
        }
        scope.launch {
            _state.update { it.copy(isCheckingDelivery = true) }
            when (val res = repo.checkDeliveryServiceability(cleanPin)) {
                is ApiResult.Success -> {
                    val finalCity = res.data.city.ifBlank { city }
                    val finalState = res.data.state.ifBlank { state }
                    repo.updateRegion(cleanPin, finalState, finalCity)
                    _state.update {
                        it.copy(
                            isCheckingDelivery = false,
                            savedPincode = cleanPin,
                            savedCity = finalCity,
                            deliveryCheck = res.data
                        )
                    }
                    showToast(res.data.deliveryMessage)
                    loadHomeFeed()
                }
                is ApiResult.Error -> {
                    repo.updateRegion(cleanPin, state, city)
                    _state.update {
                        it.copy(
                            isCheckingDelivery = false,
                            savedPincode = cleanPin,
                            savedCity = city
                        )
                    }
                    showToast(res.message)
                    loadHomeFeed()
                }
            }
        }
    }

    fun checkProductDelivery(pincode: String, productId: Int) {
        val cleanPin = pincode.filter { it.isDigit() }.take(6)
        if (cleanPin.length != 6) {
            showToast("Please enter a 6-digit pincode")
            return
        }
        scope.launch {
            _state.update { it.copy(isCheckingDelivery = true) }
            when (val res = repo.checkDeliveryServiceability(cleanPin, productId)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            isCheckingDelivery = false,
                            savedPincode = cleanPin,
                            savedCity = res.data.city,
                            deliveryCheck = res.data
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isCheckingDelivery = false) }
                    showToast(res.message)
                }
            }
        }
    }

    // ==================== DISCOVER, BRANDS & STYLE WORLDS ====================

    fun loadDiscoverHub(forceRefresh: Boolean = false) {
        val current = _state.value
        if (!forceRefresh && current.styleWorlds.isNotEmpty() && current.directoryBrands.isNotEmpty()) return
        scope.launch {
            _state.update { it.copy(isDiscoverLoading = true) }
            val forYouDef = async { repo.fetchForYou() }
            val brandsDef = async { repo.fetchBrands(current.activeBrandTab) }
            val stylesDef = async { repo.fetchStyleWorlds() }

            val fy = forYouDef.await()
            val br = brandsDef.await()
            val sw = stylesDef.await()

            _state.update { prev ->
                prev.copy(
                    isDiscoverLoading = false,
                    forYou = (fy as? ApiResult.Success)?.data ?: prev.forYou,
                    directoryBrands = (br as? ApiResult.Success)?.data ?: prev.directoryBrands,
                    styleWorlds = (sw as? ApiResult.Success)?.data ?: prev.styleWorlds
                )
            }
        }
    }

    fun selectBrandsTab(tab: String) {
        _state.update { it.copy(activeBrandTab = tab, isDiscoverLoading = true) }
        scope.launch {
            when (val res = repo.fetchBrands(tab)) {
                is ApiResult.Success -> _state.update { it.copy(isDiscoverLoading = false, directoryBrands = res.data) }
                is ApiResult.Error -> {
                    _state.update { it.copy(isDiscoverLoading = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun loadBrandDetail(brandId: Int?, slug: String?) {
        scope.launch {
            _state.update { it.copy(isBrandDetailLoading = true, brandDetailError = null, brandDetail = null) }
            when (val res = repo.fetchBrandDetails(brandId, slug)) {
                is ApiResult.Success -> _state.update {
                    it.copy(isBrandDetailLoading = false, brandDetail = res.data)
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isBrandDetailLoading = false, brandDetailError = res.message)
                }
            }
        }
    }

    fun loadStyleWorldDetail(styleId: Int?, slug: String?) {
        scope.launch {
            val cleanSlug = slug?.trim()?.lowercase().orEmpty()
            val curDetailSlug = _state.value.styleDetail?.styleWorld?.slug?.lowercase().orEmpty()
            val isSameWorld = cleanSlug.isNotBlank() && cleanSlug == curDetailSlug
            _state.update {
                it.copy(
                    isStyleDetailLoading = true,
                    styleDetailError = null,
                    styleDetail = if (isSameWorld) it.styleDetail else null,
                    unlockedWorldFeedbackResult = if (isSameWorld) it.unlockedWorldFeedbackResult else null
                )
            }
            when (val res = repo.fetchStyleWorldDetails(styleId, slug)) {
                is ApiResult.Success -> {
                    val detail = res.data
                    val sw = detail.styleWorld
                    val submitted = sw.hasSubmittedFeedback ||
                        (detail.satisfactionStatus?.hasSubmitted == true) ||
                        (sw.styleWorldId > 0 && sw.styleWorldId in _state.value.bootstrapConfig.submittedWorldIds) ||
                        _state.value.bootstrapConfig.submittedWorldSlugs.any { it.equals(sw.slug, ignoreCase = true) } ||
                        repo.hasSubmittedWorldFeedbackLocally(sw.styleWorldId, sw.slug)
                    _state.update {
                        it.copy(
                            isStyleDetailLoading = false,
                            styleDetail = detail,
                            hasActiveWorldFeedbackSubmitted = submitted
                        )
                    }
                    // Double-check dedicated satisfaction endpoint in case device/cache missed server sync
                    if (!submitted) {
                        when (val fbRes = repo.fetchStyleWorldFeedback(sw.styleWorldId.takeIf { it > 0 }, sw.slug)) {
                            is ApiResult.Success -> {
                                if (fbRes.data.hasSubmitted) {
                                    _state.update { prev ->
                                        prev.copy(
                                            hasActiveWorldFeedbackSubmitted = true,
                                            bootstrapConfig = prev.bootstrapConfig.copy(
                                                submittedWorldIds = (prev.bootstrapConfig.submittedWorldIds + fbRes.data.submittedWorldIds + sw.styleWorldId).distinct(),
                                                submittedWorldSlugs = (prev.bootstrapConfig.submittedWorldSlugs + fbRes.data.submittedWorldSlugs + sw.slug.lowercase()).distinct()
                                            )
                                        )
                                    }
                                }
                            }
                            is ApiResult.Error -> Unit
                        }
                    }
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isStyleDetailLoading = false, styleDetailError = res.message)
                }
            }
        }
    }

    fun submitStyleWorldSatisfaction(
        styleWorldId: Int,
        worldSlug: String,
        score: Int,
        preferences: LocalPreferences = mainRepository.getPreferences()
    ) {
        if (_state.value.isSubmittingWorldFeedback) return
        scope.launch {
            _state.update { it.copy(isSubmittingWorldFeedback = true) }
            when (val res = repo.submitStyleWorldFeedback(styleWorldId, worldSlug, score)) {
                is ApiResult.Success -> {
                    val result = res.data
                    _state.update { prev ->
                        val nextSubmittedIds = (prev.bootstrapConfig.submittedWorldIds + result.submittedWorldIds + styleWorldId)
                            .filter { it > 0 }
                            .distinct()
                        val nextSubmittedSlugs = (prev.bootstrapConfig.submittedWorldSlugs + result.submittedWorldSlugs + worldSlug.lowercase())
                            .filter { it.isNotBlank() }
                            .distinct()
                        val updatedFashionWorlds = prev.bootstrapConfig.fashionWorlds.map { fw ->
                            if (fw.styleWorldId == styleWorldId || fw.slug.equals(worldSlug, ignoreCase = true)) {
                                fw.copy(hasSubmittedFeedback = true)
                            } else fw
                        }
                        val updatedDetail = prev.styleDetail?.let { detail ->
                            detail.copy(
                                styleWorld = detail.styleWorld.copy(hasSubmittedFeedback = true),
                                satisfactionStatus = detail.satisfactionStatus?.copy(
                                    hasSubmitted = true,
                                    feedback = result.feedback
                                ) ?: com.example.model.StyleWorldSatisfactionStatus(
                                    hasSubmitted = true,
                                    feedback = result.feedback
                                )
                            )
                        }
                        prev.copy(
                            isSubmittingWorldFeedback = false,
                            hasActiveWorldFeedbackSubmitted = true,
                            unlockedWorldFeedbackResult = result,
                            styleDetail = updatedDetail,
                            bootstrapConfig = prev.bootstrapConfig.copy(
                                fashionWorlds = updatedFashionWorlds,
                                submittedWorldIds = nextSubmittedIds,
                                submittedWorldSlugs = nextSubmittedSlugs
                            )
                        )
                    }
                    playInteractionSfx(if (result.coupon != null) "offer" else "tap", preferences)
                    val code = result.coupon?.code.orEmpty()
                    if (result.alreadySubmitted) {
                        showToast("Feedback already recorded for this Fashion World.")
                    } else if (code.isNotBlank()) {
                        showToast("Unlocked $code for your feedback!")
                    } else {
                        showToast("Thank you — recommendations updated!")
                    }
                    loadDiscoverHub(forceRefresh = true)
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isSubmittingWorldFeedback = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun dismissUnlockedFeedbackReward() {
        _state.update { it.copy(unlockedWorldFeedbackResult = null) }
    }

    fun saveStyleWorldScrollPosition(slug: String, firstVisibleIndex: Int, firstVisibleOffset: Int) {
        val clean = slug.trim().lowercase()
        if (clean.isBlank()) return
        _state.update { prev ->
            prev.copy(
                styleWorldScrollPositions = prev.styleWorldScrollPositions + (clean to (firstVisibleIndex to firstVisibleOffset))
            )
        }
    }

    fun recordProductViewSignal(product: Product) {
        scope.launch {
            repo.recordStyleSignal(
                productId = product.id,
                categoryId = product.categoryId.takeIf { it > 0 },
                signalType = "product_view"
            )
        }
    }

    fun loadProductRecommendations(productId: Int, worldSlug: String? = null) {
        if (productId <= 0) return
        val resolvedWorld = worldSlug?.takeIf { it.isNotBlank() } ?: _state.value.worldContext.activeWorldSlug
        scope.launch {
            when (val res = repo.fetchProductRecommendations(productId, resolvedWorld)) {
                is ApiResult.Success -> {
                    val (similar, moreFromBrand) = res.data
                    _state.update {
                        it.copy(
                            recommendedSimilarProducts = similar,
                            recommendedBrandProducts = moreFromBrand
                        )
                    }
                }
                is ApiResult.Error -> Unit
            }
        }
    }

    // ==================== REELS / VIDEOS ====================

    fun loadVideosFeed(forceRefresh: Boolean = false) {
        if (!forceRefresh && _state.value.videos.isNotEmpty()) return
        scope.launch {
            _state.update { it.copy(isVideosLoading = true, videosError = null) }
            when (val res = repo.fetchVideosFeedPage(page = 1)) {
                is ApiResult.Success -> {
                    val pageData = res.data
                    val deduped = pageData.videos.distinctBy { it.videoId }
                    _state.update {
                        it.copy(
                            isVideosLoading = false,
                            videos = deduped,
                            videosPage = pageData.page,
                            videosTotalPages = pageData.totalPages,
                            canLoadMoreVideos = pageData.hasMore && deduped.isNotEmpty(),
                            videosDiagnosticsReason = pageData.diagnosticsReason,
                            videosTableExists = pageData.tableExists
                        )
                    }
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isVideosLoading = false, videosError = res.message)
                }
            }
        }
    }

    fun loadMoreVideos() {
        val current = _state.value
        if (current.isVideosLoading || current.isLoadingMoreVideos || !current.canLoadMoreVideos) return
        val nextPage = current.videosPage + 1
        if (nextPage > current.videosTotalPages) return
        scope.launch {
            _state.update { it.copy(isLoadingMoreVideos = true) }
            when (val res = repo.fetchVideosFeedPage(page = nextPage)) {
                is ApiResult.Success -> {
                    val pageData = res.data
                    _state.update { prev ->
                        val merged = (prev.videos + pageData.videos).distinctBy { it.videoId }
                        prev.copy(
                            isLoadingMoreVideos = false,
                            videos = merged,
                            videosPage = pageData.page,
                            videosTotalPages = pageData.totalPages,
                            canLoadMoreVideos = pageData.hasMore && pageData.videos.isNotEmpty()
                        )
                    }
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isLoadingMoreVideos = false) }
                }
            }
        }
    }

    fun toggleVideoLike(videoId: Int) {
        val currentVideo = _state.value.videos.find { it.videoId == videoId } ?: return
        val nextLiked = !currentVideo.isLiked
        val eventType = if (nextLiked) "like" else "unlike"
        _state.update { prev ->
            prev.copy(
                videos = prev.videos.map {
                    if (it.videoId == videoId) {
                        it.copy(
                            isLiked = nextLiked,
                            likesCount = (it.likesCount + if (nextLiked) 1 else -1).coerceAtLeast(0)
                        )
                    } else it
                }
            )
        }
        scope.launch {
            repo.recordVideoEvent(videoId, eventType)
        }
    }

    fun toggleVideoSave(videoId: Int) {
        val currentVideo = _state.value.videos.find { it.videoId == videoId } ?: return
        val nextSaved = !currentVideo.isSaved
        val eventType = if (nextSaved) "save" else "unsave"
        _state.update { prev ->
            prev.copy(
                videos = prev.videos.map {
                    if (it.videoId == videoId) {
                        it.copy(
                            isSaved = nextSaved,
                            savesCount = (it.savesCount + if (nextSaved) 1 else -1).coerceAtLeast(0)
                        )
                    } else it
                }
            )
        }
        scope.launch {
            repo.recordVideoEvent(videoId, eventType)
            showToast(if (nextSaved) "Saved to style reel bookmarks" else "Removed from saved reels")
        }
    }

    fun recordVideoEvent(videoId: Int, eventType: String, watchSeconds: Int = 0) {
        scope.launch {
            repo.recordVideoEvent(videoId, eventType, watchSeconds)
        }
    }

    // ==================== ELITE SHOWROOM ====================

    fun loadEliteHome(forceRefresh: Boolean = false) {
        val current = _state.value.eliteHome
        if (!forceRefresh && (current.heroPacks.isNotEmpty() || current.packs.isNotEmpty())) return
        scope.launch {
            _state.update { it.copy(isEliteLoading = true, eliteError = null) }
            when (val res = repo.fetchEliteHome()) {
                is ApiResult.Success -> _state.update {
                    it.copy(isEliteLoading = false, eliteHome = res.data)
                }
                is ApiResult.Error -> _state.update {
                    it.copy(isEliteLoading = false, eliteError = res.message)
                }
            }
        }
    }

    fun loadElitePack(packId: Int?, slug: String?) {
        scope.launch {
            _state.update { it.copy(isElitePackLoading = true, elitePackDetail = null) }
            when (val res = repo.fetchElitePack(packId, slug)) {
                is ApiResult.Success -> _state.update {
                    it.copy(isElitePackLoading = false, elitePackDetail = res.data)
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isElitePackLoading = false) }
                    showToast(res.message)
                }
            }
        }
    }

    // ==================== ADVANCED SEARCH (DEBOUNCED + CANCELABLE) ====================

    fun openSearchSuggestions() {
        suggestDebounceJob?.cancel()
        suggestDebounceJob = scope.launch {
            when (val res = repo.fetchSearchSuggestions(_state.value.searchQuery)) {
                is ApiResult.Success -> _state.update { it.copy(searchSuggestions = res.data) }
                is ApiResult.Error -> Unit
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _state.update { it.copy(searchQuery = newQuery) }
        suggestDebounceJob?.cancel()
        suggestDebounceJob = scope.launch {
            delay(180)
            when (val res = repo.fetchSearchSuggestions(newQuery)) {
                is ApiResult.Success -> _state.update { it.copy(searchSuggestions = res.data) }
                is ApiResult.Error -> Unit
            }
        }
        searchDebounceJob?.cancel()
        if (newQuery.trim().length >= 2) {
            searchDebounceJob = scope.launch {
                delay(320)
                executeSearch(newQuery, _state.value.searchFilters)
            }
        } else if (newQuery.isBlank()) {
            _state.update { it.copy(searchResults = SearchResultBundle()) }
        }
    }

    fun updateSearchFilters(filters: SearchFilterState) {
        _state.update { it.copy(searchFilters = filters) }
        searchDebounceJob?.cancel()
        searchDebounceJob = scope.launch {
            executeSearch(_state.value.searchQuery, filters)
        }
    }

    fun submitSearch(query: String = _state.value.searchQuery) {
        _state.update { it.copy(searchQuery = query) }
        searchDebounceJob?.cancel()
        searchDebounceJob = scope.launch {
            executeSearch(query, _state.value.searchFilters)
        }
    }

    private suspend fun executeSearch(query: String, filters: SearchFilterState) {
        _state.update { it.copy(isSearchLoading = true) }
        when (val res = repo.searchCatalog(query, filters, page = 1)) {
            is ApiResult.Success -> _state.update {
                it.copy(
                    isSearchLoading = false,
                    searchResults = res.data,
                    searchSuggestions = it.searchSuggestions.copy(recent = repo.recentSearches())
                )
            }
            is ApiResult.Error -> _state.update { it.copy(isSearchLoading = false) }
        }
    }

    // ==================== VERIFIED REVIEWS WITH OTP & MEDIA ====================

    fun loadProductReviews(productId: Int) {
        scope.launch {
            _state.update {
                it.copy(
                    isReviewsLoading = true,
                    reviewOtpState = null,
                    isReviewOtpVerified = false,
                    uploadedReviewPhotoUrls = emptyList()
                )
            }
            when (val res = repo.fetchProductReviews(productId)) {
                is ApiResult.Success -> _state.update {
                    it.copy(isReviewsLoading = false, reviewList = res.data)
                }
                is ApiResult.Error -> _state.update { it.copy(isReviewsLoading = false) }
            }
        }
    }

    fun requestReviewOtp(orderItemId: Int) {
        scope.launch {
            _state.update { it.copy(isReviewOtpBusy = true) }
            when (val res = repo.requestReviewOtp(orderItemId)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            isReviewOtpBusy = false,
                            reviewOtpState = res.data,
                            isReviewOtpVerified = false
                        )
                    }
                    showToast("Verification code sent to ${res.data.sentTo}")
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isReviewOtpBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun verifyReviewOtp(otp: String) {
        val reqId = _state.value.reviewOtpState?.otpRequestId ?: return
        scope.launch {
            _state.update { it.copy(isReviewOtpBusy = true) }
            when (val res = repo.verifyReviewOtp(reqId, otp)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isReviewOtpBusy = false, isReviewOtpVerified = res.data) }
                    if (res.data) showToast("Purchase verified")
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isReviewOtpBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun uploadReviewPhoto(uri: Uri) {
        if (_state.value.uploadedReviewPhotoUrls.size >= 3) {
            showToast("Maximum 3 photos allowed per review")
            return
        }
        scope.launch {
            _state.update { it.copy(isReviewUploading = true) }
            when (val res = repo.uploadImageUri(uri, isSupportEvidence = false)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        isReviewUploading = false,
                        uploadedReviewPhotoUrls = it.uploadedReviewPhotoUrls + res.data
                    )
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isReviewUploading = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun removeReviewPhoto(url: String) {
        _state.update { it.copy(uploadedReviewPhotoUrls = it.uploadedReviewPhotoUrls - url) }
    }

    fun submitVerifiedReview(
        productId: Int,
        orderItemId: Int,
        rating: Int,
        title: String,
        comment: String,
        onDone: () -> Unit
    ) {
        val otpReqId = _state.value.reviewOtpState?.otpRequestId ?: 0
        scope.launch {
            _state.update { it.copy(isSubmittingReview = true) }
            when (val res = repo.createVerifiedReview(
                orderItemId = orderItemId,
                otpRequestId = otpReqId,
                rating = rating,
                title = title,
                comment = comment,
                mediaUrls = _state.value.uploadedReviewPhotoUrls
            )) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            isSubmittingReview = false,
                            reviewOtpState = null,
                            isReviewOtpVerified = false,
                            uploadedReviewPhotoUrls = emptyList()
                        )
                    }
                    showToast(res.data)
                    loadProductReviews(productId)
                    onDone()
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isSubmittingReview = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun toggleReviewHelpful(reviewId: Int) {
        scope.launch {
            when (val res = repo.toggleReviewHelpful(reviewId)) {
                is ApiResult.Success -> {
                    val (count, voted) = res.data
                    _state.update { prev ->
                        prev.copy(
                            reviewList = prev.reviewList.copy(
                                reviews = prev.reviewList.reviews.map { r ->
                                    if (r.id == reviewId) r.copy(helpfulCount = count, hasVotedHelpful = voted) else r
                                }
                            )
                        )
                    }
                }
                is ApiResult.Error -> showToast(res.message)
            }
        }
    }

    // ==================== GUIDED SELF-SERVICE HELP SOLVER ====================

    fun loadHelpCenter() {
        scope.launch {
            _state.update { it.copy(isHelpLoading = true) }
            when (val res = repo.fetchHelpTopicsAndFaqs()) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        isHelpLoading = false,
                        helpTopics = res.data.first,
                        helpFaqs = res.data.second
                    )
                }
                is ApiResult.Error -> _state.update { it.copy(isHelpLoading = false) }
            }
        }
    }

    fun openHelpTopic(topicId: Int?, slug: String?) {
        scope.launch {
            _state.update {
                it.copy(
                    isHelpFlowLoading = true,
                    activeHelpFlow = null,
                    activeHelpSession = null,
                    supportPhotoUrls = emptyList()
                )
            }
            when (val res = repo.fetchHelpFlow(topicId, slug)) {
                is ApiResult.Success -> _state.update {
                    it.copy(isHelpFlowLoading = false, activeHelpFlow = res.data)
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isHelpFlowLoading = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun clearActiveHelpFlow() {
        _state.update {
            it.copy(activeHelpFlow = null, activeHelpSession = null, supportPhotoUrls = emptyList())
        }
    }

    fun evaluateHelpOrder(topicId: Int, flowId: Int, orderId: Int, orderItemId: Int = 0, note: String = "") {
        scope.launch {
            _state.update { it.copy(isHelpSessionBusy = true) }
            when (val res = repo.submitHelpSession(
                HelpSessionRequest(
                    action = "evaluate",
                    topicId = topicId,
                    flowId = flowId,
                    orderId = orderId,
                    orderItemId = orderItemId,
                    note = note,
                    evidenceUrls = _state.value.supportPhotoUrls,
                    sessionToken = _state.value.activeHelpSession?.sessionToken.orEmpty()
                )
            )) {
                is ApiResult.Success -> _state.update {
                    it.copy(isHelpSessionBusy = false, activeHelpSession = res.data)
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isHelpSessionBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun uploadSupportEvidencePhoto(uri: Uri) {
        if (_state.value.supportPhotoUrls.size >= 4) {
            showToast("Maximum 4 photos per support request")
            return
        }
        scope.launch {
            _state.update { it.copy(isSupportPhotoUploading = true) }
            when (val res = repo.uploadImageUri(uri, isSupportEvidence = true)) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        isSupportPhotoUploading = false,
                        supportPhotoUrls = it.supportPhotoUrls + res.data
                    )
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isSupportPhotoUploading = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun escalateHelpToTicket(
        topicId: Int,
        flowId: Int,
        orderId: Int,
        orderItemId: Int,
        subject: String,
        category: String,
        note: String,
        onTicketCreated: () -> Unit
    ) {
        scope.launch {
            _state.update { it.copy(isHelpSessionBusy = true) }
            when (val res = repo.submitHelpSession(
                HelpSessionRequest(
                    action = "escalate_ticket",
                    topicId = topicId,
                    flowId = flowId,
                    orderId = orderId,
                    orderItemId = orderItemId,
                    subject = subject,
                    category = category,
                    note = note,
                    evidenceUrls = _state.value.supportPhotoUrls,
                    sessionToken = _state.value.activeHelpSession?.sessionToken.orEmpty()
                )
            )) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isHelpSessionBusy = false, activeHelpSession = res.data) }
                    val num = res.data.ticket?.ticketNumber.orEmpty()
                    showToast(if (num.isNotBlank()) "Support ticket $num opened" else "Support ticket opened")
                    onTicketCreated()
                }
                is ApiResult.Error -> {
                    _state.update { it.copy(isHelpSessionBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }
}
