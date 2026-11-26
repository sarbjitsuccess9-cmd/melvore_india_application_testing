package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.auth.GoogleCredentialHelper
import com.example.data.CartData
import com.example.data.CouponEvalResult
import com.example.data.MelvoreRepository
import com.example.data.RepoResult
import com.example.model.AboutSection
import com.example.model.Address
import com.example.model.BlogPost
import com.example.model.BrandItem
import com.example.model.CartItem
import com.example.model.CartSummary
import com.example.model.Category
import com.example.model.CollectionItem
import com.example.model.CouponInfo
import com.example.model.DesignBundle
import com.example.model.FaqItem
import com.example.model.LocalPreferences
import com.example.model.Order
import com.example.model.Product
import com.example.model.RazorpayCheckoutPayload
import com.example.model.SmartOffer
import com.example.model.SupportTicket
import com.example.model.TrendingTag
import com.example.model.User
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class MelvoreUiState(
    val isHomeLoading: Boolean = true,
    val homeError: String? = null,
    val isCatalogLoading: Boolean = false,
    val catalogError: String? = null,
    val isProductDetailsLoading: Boolean = false,
    val productDetailsError: String? = null,
    val isBlogLoading: Boolean = false,
    val blogError: String? = null,
    val isAuthBusy: Boolean = false,
    val isCartBusy: Boolean = false,
    val isCheckoutBusy: Boolean = false,
    val isTrackingBusy: Boolean = false,
    val trackingError: String? = null,
    val isProfileBusy: Boolean = false,
    val apiConnected: Boolean = false,
    val isShowingCachedData: Boolean = false,
    val cachedProductCount: Int = 0,
    val lastProductSyncTimestamp: Long? = null,
    val apiBaseUrl: String = BuildConfig.MELVORE_API_BASE,

    val user: User? = null,
    val preferences: LocalPreferences = LocalPreferences(),
    val themeMode: String = "light",
    val currency: String = "₹",

    val design: DesignBundle = DesignBundle(),
    val products: List<Product> = emptyList(),
    val catalogProducts: List<Product> = emptyList(),
    val selectedProduct: Product? = null,
    val categories: List<Category> = emptyList(),
    val brands: List<BrandItem> = emptyList(),
    val collections: List<CollectionItem> = emptyList(),
    val trendingTags: List<TrendingTag> = emptyList(),
    val blogs: List<BlogPost> = emptyList(),
    val selectedBlogPost: BlogPost? = null,
    val faqs: List<FaqItem> = emptyList(),
    val aboutSections: List<AboutSection> = emptyList(),

    val cartItems: List<CartItem> = emptyList(),
    val cartCount: Int = 0,
    val cartSummary: CartSummary = CartSummary(),

    val wishlistIds: List<Int> = emptyList(),
    val wishlistProducts: List<Product> = emptyList(),
    val addresses: List<Address> = emptyList(),
    val orders: List<Order> = emptyList(),
    val trackedOrder: Order? = null,
    val pendingPaymentOrder: Order? = null,
    val tickets: List<SupportTicket> = emptyList(),
    val activeCoupons: List<CouponInfo> = emptyList()
) {
    val cartSubtotal: Double get() = cartSummary.subtotal.takeIf { it > 0.0 } ?: cartItems.sumOf { it.lineTotal }
    val pendingOrdersCount: Int
        get() = orders.count {
            it.status.lowercase() in setOf("pending", "processing", "shipped", "out_for_delivery")
        }

    fun cartQuantityForProduct(productId: Int): Int =
        cartItems.filter { it.productId == productId }.sumOf { it.quantity }
}

class MelvoreViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MelvoreRepository(application.applicationContext)

    val apiClient: com.example.network.MelvoreApiClient get() = repository.apiClient
    val sessionStore: com.example.data.SecureSessionStore get() = repository.sessionStore

    val discovery = DiscoveryViewModelDelegate(
        context = application.applicationContext,
        mainRepository = repository,
        scope = viewModelScope,
        showToast = ::showToast
    )

    private val _uiState = MutableStateFlow(
        MelvoreUiState(
            preferences = repository.getPreferences(),
            themeMode = repository.getPreferences().themeMode.ifBlank { "light" },
            currency = repository.getPreferences().currencyOverride,
            apiBaseUrl = repository.getApiBaseUrl()
        )
    )
    val uiState: StateFlow<MelvoreUiState> = _uiState.asStateFlow()

    private val _isCartOpen = MutableStateFlow(false)
    val isCartOpen: StateFlow<Boolean> = _isCartOpen.asStateFlow()

    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()

    private val _isProfileSheetOpen = MutableStateFlow(false)
    val isProfileSheetOpen: StateFlow<Boolean> = _isProfileSheetOpen.asStateFlow()

    private val _isTopMenuOpen = MutableStateFlow(false)
    val isTopMenuOpen: StateFlow<Boolean> = _isTopMenuOpen.asStateFlow()

    private val _toastMessages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val toastMessages: SharedFlow<String> = _toastMessages.asSharedFlow()

    private val _razorpayLaunchRequests = MutableSharedFlow<RazorpayCheckoutPayload>(extraBufferCapacity = 4)
    val razorpayLaunchRequests: SharedFlow<RazorpayCheckoutPayload> = _razorpayLaunchRequests.asSharedFlow()

    private val _navigateAfterOrder = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val navigateAfterOrder: SharedFlow<String> = _navigateAfterOrder.asSharedFlow()

    // Current checkout idempotency key; regenerated on new checkout or order completion
    private var currentCheckoutIdempotencyKey: String = UUID.randomUUID().toString()
    var activeRazorpayOrderId: Int? = null
        private set
    var activeRazorpayGatewayOrderId: String? = null
        private set

    init {
        observeLocalProductCache()
        refreshAll()
    }

    private fun observeLocalProductCache() {
        viewModelScope.launch {
            repository.cachedProductsFlow.collect { cachedProducts ->
                if (cachedProducts.isNotEmpty()) {
                    _uiState.update { prev ->
                        prev.copy(
                            cachedProductCount = cachedProducts.size,
                            products = if (prev.products.isEmpty()) cachedProducts else prev.products,
                            catalogProducts = if (prev.catalogProducts.isEmpty()) cachedProducts else prev.catalogProducts,
                            isHomeLoading = if (prev.products.isEmpty()) false else prev.isHomeLoading
                        )
                    }
                } else {
                    _uiState.update { it.copy(cachedProductCount = 0) }
                }
            }
        }
        viewModelScope.launch {
            repository.cachedCategoriesFlow.collect { cachedCategories ->
                if (cachedCategories.isNotEmpty()) {
                    _uiState.update { prev ->
                        if (prev.categories.isEmpty()) prev.copy(categories = cachedCategories) else prev
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.lastProductCacheSyncFlow.collect { syncedAt ->
                _uiState.update { it.copy(lastProductSyncTimestamp = syncedAt) }
            }
        }
    }

    fun setCartOpen(open: Boolean) {
        _isCartOpen.value = open
        if (open) refreshCart()
    }

    fun setSearchOpen(open: Boolean) {
        _isSearchOpen.value = open
    }

    fun setProfileSheetOpen(open: Boolean) {
        _isProfileSheetOpen.value = open
    }

    fun setTopMenuOpen(open: Boolean) {
        _isTopMenuOpen.value = open
    }

    fun showToast(message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            _toastMessages.emit(message)
        }
    }

    fun refreshAccountState() {
        viewModelScope.launch {
            refreshAuthenticatedData()
        }
    }

    fun resetCheckoutIdempotency() {
        currentCheckoutIdempotencyKey = UUID.randomUUID().toString()
    }

    fun resetApiBaseUrlToDefault() {
        val resetUrl = repository.resetApiBaseUrlToDefault()
        _uiState.update { it.copy(apiBaseUrl = resetUrl) }
        showToast("API base URL reset to $resetUrl")
        forcePullToRefresh()
    }

    /**
     * Triggered by Home pull-to-refresh or Diagnostics sync: forces fresh fetch of
     * design manifest (ignoring ETag), bootstrap config, home feed, themes, occasions, and catalog.
     */
    fun forcePullToRefresh() {
        discovery.refreshRemoteDesignManifest(forceRefresh = true)
        refreshAll()
    }

    fun refreshAll() {
        discovery.loadBootstrapConfig(
            forceRefresh = true,
            cartSubtotal = _uiState.value.cartSubtotal,
            localPreferences = _uiState.value.preferences,
            onServerPreferencesSynced = { serverPrefs ->
                val cur = _uiState.value.preferences
                val shouldHydrateFromServer = (serverPrefs.audioConsentAnswered && !cur.audioConsentAnswered) ||
                    repository.hasAuthToken()
                if (shouldHydrateFromServer) {
                    val merged = cur.copy(
                        ambientAudioEnabled = serverPrefs.ambientAudioEnabled,
                        uiSfxEnabled = serverPrefs.uiSfxEnabled,
                        reducedMotion = serverPrefs.reducedMotion,
                        videoAutoplayMuted = serverPrefs.videoAutoplayMuted,
                        dataSaverMode = serverPrefs.dataSaverMode,
                        audioConsentAnswered = serverPrefs.audioConsentAnswered || cur.audioConsentAnswered,
                        preferredWorldSlug = serverPrefs.preferredWorldSlug ?: cur.preferredWorldSlug
                    )
                    repository.savePreferences(merged)
                    _uiState.update { it.copy(preferences = merged) }
                }
            }
        )
        discovery.loadHomeFeed()
        discovery.loadDiscoverHub()
        discovery.refreshRemoteDesignManifest()
        discovery.loadOccasionsAndCampaigns()
        discovery.refreshThemesAndActiveTheme()
        discovery.startPeriodicDesignRefresh()
        viewModelScope.launch {
            _uiState.update { it.copy(isHomeLoading = true, homeError = null) }

            val healthDeferred = async { repository.checkHealth() }
            val designDeferred = async { repository.fetchDesignHub() }
            val productsDeferred = async { repository.fetchProducts(page = 1, limit = 100) }
            val categoriesDeferred = async { repository.fetchCategories() }
            val cartDeferred = async { repository.fetchCart() }
            val couponsDeferred = async { repository.fetchActiveCoupons() }
            val blogsDeferred = async { repository.fetchBlogs() }
            val aboutDeferred = async { repository.fetchAboutSections() }
            val faqsDeferred = async { repository.fetchFaqs() }

            val healthRes = healthDeferred.await()
            val designRes = designDeferred.await()
            val productsRes = productsDeferred.await()
            val categoriesRes = categoriesDeferred.await()
            val cartRes = cartDeferred.await()
            val couponsRes = couponsDeferred.await()
            val blogsRes = blogsDeferred.await()
            val aboutRes = aboutDeferred.await()
            val faqsRes = faqsDeferred.await()

            val productsServedFromCache = (productsRes as? RepoResult.Success)?.fromCache == true
            val categoriesServedFromCache = (categoriesRes as? RepoResult.Success)?.fromCache == true
            val isOnline = healthRes is RepoResult.Success ||
                designRes is RepoResult.Success ||
                (productsRes is RepoResult.Success && !productsRes.fromCache)

            _uiState.update { prev ->
                val designData = (designRes as? RepoResult.Success)?.data
                val catBundle = (categoriesRes as? RepoResult.Success)?.data
                val loadedProducts = (productsRes as? RepoResult.Success)?.data?.takeIf { it.isNotEmpty() }
                    ?: prev.products
                val cartData = (cartRes as? RepoResult.Success)?.data
                val prefs = repository.getPreferences()

                val resolvedBrands = catBundle?.brands?.ifEmpty { designData?.brands.orEmpty() }
                    ?: designData?.brands
                    ?: prev.brands

                val resolvedCategories = catBundle?.categories?.ifEmpty { designData?.categories.orEmpty() }
                    ?: designData?.categories
                    ?: prev.categories

                val showingOfflineCache = !isOnline &&
                    (productsServedFromCache || categoriesServedFromCache || loadedProducts.isNotEmpty())

                val primaryError = if (showingOfflineCache && loadedProducts.isNotEmpty()) {
                    null
                } else {
                    when {
                        healthRes is RepoResult.Error && designRes is RepoResult.Error -> designRes.message
                        designRes is RepoResult.Error && productsRes is RepoResult.Error -> productsRes.message
                        designRes is RepoResult.Error -> designRes.message
                        productsRes is RepoResult.Error -> productsRes.message
                        else -> null
                    }
                }

                prev.copy(
                    isHomeLoading = false,
                    homeError = primaryError,
                    apiConnected = isOnline,
                    isShowingCachedData = showingOfflineCache,
                    design = designData?.design ?: prev.design,
                    products = loadedProducts,
                    catalogProducts = if (prev.catalogProducts.isEmpty() || !productsServedFromCache) loadedProducts else prev.catalogProducts,
                    categories = resolvedCategories,
                    brands = resolvedBrands,
                    collections = catBundle?.collections?.ifEmpty { designData?.collections.orEmpty() }
                        ?: designData?.collections
                        ?: prev.collections,
                    trendingTags = designData?.trendingTags ?: prev.trendingTags,
                    faqs = (faqsRes as? RepoResult.Success)?.data?.ifEmpty { designData?.faqs.orEmpty() }
                        ?: designData?.faqs
                        ?: prev.faqs,
                    blogs = (blogsRes as? RepoResult.Success)?.data ?: prev.blogs,
                    aboutSections = (aboutRes as? RepoResult.Success)?.data ?: prev.aboutSections,
                    activeCoupons = (couponsRes as? RepoResult.Success)?.data ?: prev.activeCoupons,
                    cartItems = cartData?.items ?: prev.cartItems,
                    cartCount = cartData?.count ?: prev.cartCount,
                    cartSummary = cartData?.summary ?: prev.cartSummary,
                    currency = prefs.currencyOverride.ifBlank {
                        designData?.design?.currency ?: prev.currency
                    }
                )
            }

            if (repository.hasAuthToken() && isOnline) {
                refreshAuthenticatedData()
            }
        }
    }

    private suspend fun refreshAuthenticatedData() {
        val profileRes = repository.fetchProfile()
        if (profileRes is RepoResult.Error && profileRes.httpStatus == 401) {
            repository.clearAuthToken()
            _uiState.update {
                it.copy(
                    user = null,
                    addresses = emptyList(),
                    wishlistIds = emptyList(),
                    wishlistProducts = emptyList(),
                    orders = emptyList(),
                    tickets = emptyList(),
                    pendingPaymentOrder = null
                )
            }
            return
        }

        val wishlistDeferred = viewModelScope.async { repository.fetchWishlist() }
        val ordersDeferred = viewModelScope.async { repository.fetchOrderHistory(page = 1, limit = 50) }
        val ticketsDeferred = viewModelScope.async { repository.fetchSupportTickets() }
        val cartDeferred = viewModelScope.async { repository.fetchCart() }

        val wishlistRes = wishlistDeferred.await()
        val ordersRes = ordersDeferred.await()
        val ticketsRes = ticketsDeferred.await()
        val cartRes = cartDeferred.await()

        val pendingRecoveryId = repository.getPendingPaymentOrderId()
        val recoveryOrder = if (pendingRecoveryId != null) {
            val existing = (ordersRes as? RepoResult.Success)?.data?.find { it.id == pendingRecoveryId }
                ?: (repository.fetchOrderDetails(pendingRecoveryId) as? RepoResult.Success)?.data
            if (existing != null &&
                !existing.paymentMethod.equals("cod", ignoreCase = true) &&
                existing.paymentStatus.equals("pending", ignoreCase = true) &&
                !existing.status.equals("cancelled", ignoreCase = true)
            ) {
                existing
            } else {
                repository.setPendingPaymentOrderId(null)
                null
            }
        } else null

        _uiState.update { prev ->
            val prof = (profileRes as? RepoResult.Success)?.data
            val wl = (wishlistRes as? RepoResult.Success)?.data
            val ords = (ordersRes as? RepoResult.Success)?.data
            val tkts = (ticketsRes as? RepoResult.Success)?.data
            val cart = (cartRes as? RepoResult.Success)?.data

            prev.copy(
                user = prof?.user ?: prev.user,
                addresses = prof?.addresses ?: prev.addresses,
                wishlistIds = wl?.productIds ?: prev.wishlistIds,
                wishlistProducts = wl?.products ?: prev.wishlistProducts,
                orders = ords ?: prev.orders,
                tickets = tkts ?: prev.tickets,
                cartItems = cart?.items ?: prev.cartItems,
                cartCount = cart?.count ?: prev.cartCount,
                cartSummary = cart?.summary ?: prev.cartSummary,
                pendingPaymentOrder = recoveryOrder
            )
        }
        discovery.loadBootstrapConfig(
            forceRefresh = true,
            cartSubtotal = _uiState.value.cartSubtotal,
            localPreferences = _uiState.value.preferences
        )
        discovery.loadDiscoverHub(forceRefresh = true)
    }

    // ==================== CATALOG & DETAILS ====================

    fun loadCatalog(
        category: String = "all",
        collection: String = "",
        query: String = "",
        sort: String = "featured",
        brandId: Int = 0
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCatalogLoading = true, catalogError = null) }
            val cleanCollection = collection.trim()
            val onSaleFlag = if (cleanCollection.equals("sale", ignoreCase = true) || cleanCollection.equals("up-to-90-off", ignoreCase = true)) 1 else null
            val maxPriceCap = when (cleanCollection.lowercase()) {
                "under-999" -> 999.0
                "under-1999" -> 1999.0
                else -> null
            }
            val res = repository.fetchProducts(
                page = 1,
                limit = 100,
                category = category,
                collection = cleanCollection.takeIf { it.isNotBlank() },
                brandId = brandId.takeIf { it > 0 },
                search = query.trim().takeIf { it.isNotBlank() },
                sort = sort,
                onSale = onSaleFlag,
                maxPrice = maxPriceCap
            )
            when (res) {
                is RepoResult.Success -> {
                    _uiState.update { prev ->
                        prev.copy(
                            isCatalogLoading = false,
                            catalogError = null,
                            catalogProducts = res.data,
                            apiConnected = if (!res.fromCache) true else prev.apiConnected,
                            isShowingCachedData = res.fromCache
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update { prev ->
                        prev.copy(
                            isCatalogLoading = false,
                            catalogError = res.message
                        )
                    }
                }
            }
        }
    }

    fun loadProductDetails(productId: Int) {
        val inMemory = _uiState.value.products.find { it.id == productId }
            ?: _uiState.value.catalogProducts.find { it.id == productId }
        _uiState.update {
            it.copy(
                isProductDetailsLoading = true,
                productDetailsError = null,
                selectedProduct = inMemory
            )
        }
        viewModelScope.launch {
            // Immediately hydrate from local Room cache if available (supports rich offline details & fast cold opens)
            val localCached = runCatching { repository.getCachedProductById(productId) }.getOrNull()
            if (localCached != null) {
                _uiState.update { prev ->
                    if (prev.selectedProduct == null || prev.selectedProduct.id == productId) {
                        prev.copy(selectedProduct = localCached)
                    } else prev
                }
            }

            when (val res = repository.fetchProductDetails(productId)) {
                is RepoResult.Success -> {
                    if (!res.fromCache) {
                        discovery.recordProductViewSignal(res.data)
                        discovery.loadProductReviews(res.data.id)
                    }
                    _uiState.update { prev ->
                        prev.copy(
                            isProductDetailsLoading = false,
                            productDetailsError = null,
                            selectedProduct = res.data,
                            apiConnected = if (!res.fromCache) true else prev.apiConnected,
                            isShowingCachedData = res.fromCache
                        )
                    }
                }
                is RepoResult.Error -> {
                    val fallbackProduct = localCached ?: inMemory
                    _uiState.update { prev ->
                        prev.copy(
                            isProductDetailsLoading = false,
                            selectedProduct = prev.selectedProduct ?: fallbackProduct,
                            productDetailsError = if (prev.selectedProduct == null && fallbackProduct == null) res.message else null,
                            isShowingCachedData = fallbackProduct != null || prev.isShowingCachedData
                        )
                    }
                    if (fallbackProduct != null) showToast(res.message)
                }
            }
        }
    }

    fun loadBlogDetails(postId: Int) {
        val preview = _uiState.value.blogs.find { it.id == postId }
        _uiState.update { it.copy(isBlogLoading = true, blogError = null, selectedBlogPost = preview) }
        viewModelScope.launch {
            when (val res = repository.fetchBlogDetails(postId)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(isBlogLoading = false, blogError = null, selectedBlogPost = res.data)
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isBlogLoading = false, blogError = res.message) }
                    showToast(res.message)
                }
            }
        }
    }

    fun clearSelectedBlogPost() {
        _uiState.update { it.copy(selectedBlogPost = null, blogError = null) }
    }

    // ==================== THEME & LOCAL PREFS ====================

    fun toggleTheme() {
        val current = _uiState.value.themeMode.lowercase()
        val next = if (current == "dark") "light" else "dark"
        setTheme(next)
    }

    fun setTheme(theme: String) {
        val normalized = when (theme.trim().lowercase()) {
            "dark", "system", "light" -> theme.trim().lowercase()
            else -> "light"
        }
        val nextPrefs = _uiState.value.preferences.copy(themeMode = normalized)
        repository.savePreferences(nextPrefs)
        _uiState.update { it.copy(themeMode = normalized, preferences = nextPrefs) }
    }

    fun updateLocalPreferences(patch: (LocalPreferences) -> LocalPreferences) {
        val nextPrefs = patch(_uiState.value.preferences)
        val normalizedMode = nextPrefs.themeMode.ifBlank { "light" }
        val finalPrefs = nextPrefs.copy(themeMode = normalizedMode)
        repository.savePreferences(finalPrefs)
        _uiState.update {
            it.copy(
                preferences = finalPrefs,
                themeMode = normalizedMode,
                currency = finalPrefs.currencyOverride
            )
        }
        discovery.syncPreferencesWithBackend(finalPrefs)
        showToast("Preferences saved")
    }

    fun selectPreferredWorld(slug: String?) {
        discovery.selectPreferredWorld(slug, _uiState.value.preferences) { cleanSlug ->
            val nextPrefs = _uiState.value.preferences.copy(preferredWorldSlug = cleanSlug)
            repository.savePreferences(nextPrefs)
            _uiState.update { it.copy(preferences = nextPrefs) }
            discovery.syncPreferencesWithBackend(nextPrefs)
        }
    }

    fun answerAudioConsent(allowAmbient: Boolean) {
        val nextPrefs = _uiState.value.preferences.copy(
            ambientAudioEnabled = allowAmbient,
            audioConsentAnswered = true
        )
        repository.savePreferences(nextPrefs)
        _uiState.update { it.copy(preferences = nextPrefs) }
        discovery.clearAudioConsentPrompt()
        discovery.syncPreferencesWithBackend(nextPrefs)
    }

    fun toggleAmbientAudio(enabled: Boolean) {
        val nextPrefs = _uiState.value.preferences.copy(
            ambientAudioEnabled = enabled,
            audioConsentAnswered = true
        )
        repository.savePreferences(nextPrefs)
        _uiState.update { it.copy(preferences = nextPrefs) }
        discovery.clearAudioConsentPrompt()
        discovery.syncPreferencesWithBackend(nextPrefs)
    }

    fun toggleUiSfx(enabled: Boolean) {
        val nextPrefs = _uiState.value.preferences.copy(
            uiSfxEnabled = enabled,
            audioConsentAnswered = true
        )
        repository.savePreferences(nextPrefs)
        _uiState.update { it.copy(preferences = nextPrefs) }
        discovery.syncPreferencesWithBackend(nextPrefs)
    }

    fun toggleReducedMotion(enabled: Boolean) {
        val nextPrefs = _uiState.value.preferences.copy(reducedMotion = enabled)
        repository.savePreferences(nextPrefs)
        _uiState.update { it.copy(preferences = nextPrefs) }
        discovery.syncPreferencesWithBackend(nextPrefs)
    }

    fun toggleVideoAutoplayMuted(enabled: Boolean) {
        val nextPrefs = _uiState.value.preferences.copy(videoAutoplayMuted = enabled)
        repository.savePreferences(nextPrefs)
        _uiState.update { it.copy(preferences = nextPrefs) }
        discovery.syncPreferencesWithBackend(nextPrefs)
    }

    fun toggleDataSaverMode(enabled: Boolean) {
        val nextPrefs = _uiState.value.preferences.copy(dataSaverMode = enabled)
        repository.savePreferences(nextPrefs)
        _uiState.update { it.copy(preferences = nextPrefs) }
        discovery.syncPreferencesWithBackend(nextPrefs)
    }

    fun applySmartOfferCoupon(offer: SmartOffer) {
        discovery.applySmartOffer(offer, _uiState.value.preferences) { code ->
            validateCoupon(code, "online") { }
        }
    }

    fun saveApiBaseUrl(url: String) {
        val saved = repository.setApiBaseUrl(url)
        _uiState.update { it.copy(apiBaseUrl = saved) }
        showToast("Backend endpoint set to $saved")
        refreshAll()
    }

    // ==================== AUTH ====================

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthBusy = true) }
            when (val res = repository.login(email, password)) {
                is RepoResult.Success -> {
                    _uiState.update { it.copy(user = res.data, isAuthBusy = false) }
                    refreshAuthenticatedData()
                    showToast(res.message)
                    onSuccess()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isAuthBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun register(name: String, email: String, phone: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthBusy = true) }
            when (val res = repository.register(name, email, phone, password)) {
                is RepoResult.Success -> {
                    _uiState.update { it.copy(user = res.data, isAuthBusy = false) }
                    refreshAuthenticatedData()
                    showToast(res.message)
                    onSuccess()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isAuthBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun loginWithGoogleIdToken(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthBusy = true) }
            when (val res = repository.socialGoogleLogin(idToken)) {
                is RepoResult.Success -> {
                    _uiState.update { it.copy(user = res.data, isAuthBusy = false) }
                    refreshAuthenticatedData()
                    showToast(res.message)
                    onSuccess()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isAuthBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun sendEmailOtp(email: String, onSent: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthBusy = true) }
            when (val res = repository.sendEmailOtp(email)) {
                is RepoResult.Success -> {
                    _uiState.update { it.copy(isAuthBusy = false) }
                    showToast(res.data)
                    onSent()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isAuthBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun verifyEmailOtp(email: String, otp: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthBusy = true) }
            when (val res = repository.verifyEmailOtp(email, otp)) {
                is RepoResult.Success -> {
                    _uiState.update { it.copy(user = res.data, isAuthBusy = false) }
                    refreshAuthenticatedData()
                    showToast(res.message)
                    onSuccess()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isAuthBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun requestPasswordReset(email: String, onSent: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthBusy = true) }
            when (val res = repository.requestPasswordReset(email)) {
                is RepoResult.Success -> {
                    _uiState.update { it.copy(isAuthBusy = false) }
                    showToast(res.data)
                    onSent()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isAuthBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun resetPassword(email: String, otp: String, newPassword: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthBusy = true) }
            when (val res = repository.resetPassword(email, otp, newPassword)) {
                is RepoResult.Success -> {
                    _uiState.update { it.copy(isAuthBusy = false) }
                    showToast(res.data)
                    onSuccess()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isAuthBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            GoogleCredentialHelper.clearCredentialState(getApplication())
            _isProfileSheetOpen.value = false
            _uiState.update {
                it.copy(
                    user = null,
                    wishlistIds = emptyList(),
                    wishlistProducts = emptyList(),
                    addresses = emptyList(),
                    orders = emptyList(),
                    tickets = emptyList(),
                    pendingPaymentOrder = null
                )
            }
            refreshCart()
            discovery.loadBootstrapConfig(
                forceRefresh = true,
                cartSubtotal = 0.0,
                localPreferences = _uiState.value.preferences
            )
            discovery.loadDiscoverHub(forceRefresh = true)
            showToast("Signed out of Melvore")
        }
    }

    fun updateProfile(name: String, phone: String, alternatePhone: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProfileBusy = true) }
            when (val res = repository.updateProfile(name, phone, alternatePhone)) {
                is RepoResult.Success -> {
                    _uiState.update { it.copy(user = res.data, isProfileBusy = false) }
                    showToast(res.message)
                    onDone(true)
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isProfileBusy = false) }
                    showToast(res.message)
                    onDone(false)
                }
            }
        }
    }

    // ==================== CART & WISHLIST ====================

    fun refreshCart() {
        viewModelScope.launch {
            when (val res = repository.fetchCart()) {
                is RepoResult.Success -> applyCartData(res.data)
                is RepoResult.Error -> showToast(res.message)
            }
        }
    }

    private fun applyCartData(data: CartData) {
        _uiState.update {
            it.copy(
                cartItems = data.items,
                cartCount = data.count,
                cartSummary = data.summary
            )
        }
    }

    fun addToCart(
        product: Product,
        color: String? = null,
        size: String? = null,
        quantity: Int = 1,
        onAdded: () -> Unit = {}
    ) {
        val chosenColor = color?.takeIf { it.isNotBlank() } ?: product.colors.firstOrNull()?.name
        val chosenSize = size?.takeIf { it.isNotBlank() } ?: product.sizes.firstOrNull()
        val matchedVariation = product.variations.find { v ->
            v.isActive &&
                (chosenColor == null || v.color.equals(chosenColor, ignoreCase = true) || v.color.isBlank()) &&
                (chosenSize == null || v.size.equals(chosenSize, ignoreCase = true) || v.size.isBlank())
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCartBusy = true) }
            when (val res = repository.addToCart(
                productId = product.id,
                variationId = matchedVariation?.id,
                quantity = quantity,
                color = chosenColor,
                size = chosenSize
            )) {
                is RepoResult.Success -> {
                    applyCartData(res.data)
                    _uiState.update { it.copy(isCartBusy = false) }
                    showToast("\"${product.name}\" added to cart")
                    onAdded()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isCartBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun updateCartQty(cartId: Int, delta: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCartBusy = true) }
            when (val res = repository.updateCartQty(cartId, delta)) {
                is RepoResult.Success -> {
                    applyCartData(res.data)
                    _uiState.update { it.copy(isCartBusy = false) }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isCartBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun removeCartItem(cartId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCartBusy = true) }
            when (val res = repository.removeCartItem(cartId)) {
                is RepoResult.Success -> {
                    applyCartData(res.data)
                    _uiState.update { it.copy(isCartBusy = false) }
                    showToast(res.message)
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isCartBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    /**
     * Quick-commerce stepper increment: if product already exists in cart, increment its line item;
     * otherwise add 1 unit using its default variant.
     */
    fun incrementProductInCart(product: Product) {
        val existing = _uiState.value.cartItems.firstOrNull { it.productId == product.id }
        if (existing != null) {
            updateCartQty(existing.cartId, 1)
        } else {
            addToCart(product = product, quantity = 1)
        }
    }

    /**
     * Quick-commerce stepper decrement: reduces product quantity by 1 or removes the line item at 1.
     */
    fun decrementProductInCart(productId: Int) {
        val existing = _uiState.value.cartItems.firstOrNull { it.productId == productId } ?: return
        if (existing.quantity <= 1) {
            removeCartItem(existing.cartId)
        } else {
            updateCartQty(existing.cartId, -1)
        }
    }

    /**
     * Returns false immediately if unauthenticated so UI can route to Login.
     */
    fun toggleWishlist(productId: Int): Boolean {
        if (_uiState.value.user == null) {
            showToast("Please sign in to save items to your wishlist")
            return false
        }
        val alreadySaved = _uiState.value.wishlistIds.contains(productId)
        viewModelScope.launch {
            val res = if (alreadySaved) repository.removeFromWishlist(productId)
            else repository.addToWishlist(productId)
            when (res) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            wishlistIds = res.data.productIds,
                            wishlistProducts = res.data.products
                        )
                    }
                    showToast(res.message)
                }
                is RepoResult.Error -> showToast(res.message)
            }
        }
        return true
    }

    // ==================== ADDRESSES ====================

    fun saveAddress(address: Address, onComplete: (Address?) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProfileBusy = true) }
            when (val res = repository.saveAddress(address)) {
                is RepoResult.Success -> {
                    val refreshed = (repository.fetchAddresses() as? RepoResult.Success)?.data
                    _uiState.update {
                        it.copy(
                            addresses = refreshed ?: it.addresses,
                            isProfileBusy = false
                        )
                    }
                    showToast(res.message)
                    onComplete(res.data)
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isProfileBusy = false) }
                    showToast(res.message)
                    onComplete(null)
                }
            }
        }
    }

    fun setDefaultAddress(addressId: Int) {
        val target = _uiState.value.addresses.find { it.id == addressId } ?: return
        saveAddress(target.copy(isDefault = true))
    }

    fun deleteAddress(addressId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProfileBusy = true) }
            when (val res = repository.deleteAddress(addressId)) {
                is RepoResult.Success -> {
                    val refreshed = (repository.fetchAddresses() as? RepoResult.Success)?.data
                    _uiState.update {
                        it.copy(
                            addresses = refreshed ?: it.addresses.filterNot { a -> a.id == addressId },
                            isProfileBusy = false
                        )
                    }
                    showToast(res.data)
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isProfileBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    // ==================== COUPONS, CHECKOUT & RAZORPAY ====================

    fun validateCoupon(code: String, paymentMethod: String, onResult: (CouponEvalResult) -> Unit) {
        viewModelScope.launch {
            val res = repository.validateCoupon(code, paymentMethod)
            if (code.isNotBlank()) showToast(res.message)
            if (res.valid && res.summary != null) {
                _uiState.update { it.copy(cartSummary = res.summary) }
            }
            onResult(res)
        }
    }

    /**
     * Places an order either using an existing saved addressId (>0) or by saving a new address
     * via /api/addresses/save.php first.
     * paymentMethod must be "cod" or "online".
     */
    fun submitCheckoutOrder(
        existingAddressId: Int,
        newAddressDraft: Address?,
        paymentMethod: String,
        couponCode: String,
        notes: String = ""
    ) {
        if (_uiState.value.user == null) {
            showToast("Please sign in to place an order")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckoutBusy = true) }
            val addressId = if (existingAddressId > 0) {
                existingAddressId
            } else {
                val draft = newAddressDraft ?: run {
                    _uiState.update { it.copy(isCheckoutBusy = false) }
                    showToast("Please enter or select a delivery address.")
                    return@launch
                }
                when (val savedRes = repository.saveAddress(draft)) {
                    is RepoResult.Success -> {
                        val refreshed = (repository.fetchAddresses() as? RepoResult.Success)?.data
                        if (refreshed != null) _uiState.update { it.copy(addresses = refreshed) }
                        savedRes.data.id
                    }
                    is RepoResult.Error -> {
                        _uiState.update { it.copy(isCheckoutBusy = false) }
                        showToast(savedRes.message)
                        return@launch
                    }
                }
            }

            when (val orderRes = repository.createOrder(
                shippingAddressId = addressId,
                paymentMethod = paymentMethod,
                couponCode = couponCode,
                notes = notes,
                idempotencyKey = currentCheckoutIdempotencyKey
            )) {
                is RepoResult.Success -> {
                    val created = orderRes.data
                    if (paymentMethod.equals("cod", ignoreCase = true)) {
                        resetCheckoutIdempotency()
                        refreshAuthenticatedData()
                        _uiState.update { it.copy(isCheckoutBusy = false, pendingPaymentOrder = null) }
                        showToast("Order #${created.orderNumber} confirmed (Cash on Delivery)!")
                        _navigateAfterOrder.emit(created.orderNumber)
                    } else {
                        _uiState.update { it.copy(pendingPaymentOrder = created) }
                        launchRazorpayForOrder(created.id)
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isCheckoutBusy = false) }
                    showToast(orderRes.message)
                }
            }
        }
    }

    fun retryRazorpayPayment(orderId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckoutBusy = true) }
            launchRazorpayForOrder(orderId)
        }
    }

    private suspend fun launchRazorpayForOrder(orderId: Int) {
        when (val initRes = repository.initiateRazorpayPayment(orderId)) {
            is RepoResult.Success -> {
                val payload = initRes.data
                activeRazorpayOrderId = payload.orderId
                activeRazorpayGatewayOrderId = payload.razorpayOrderId
                _uiState.update { it.copy(isCheckoutBusy = false) }
                _razorpayLaunchRequests.emit(payload)
            }
            is RepoResult.Error -> {
                _uiState.update { it.copy(isCheckoutBusy = false) }
                showToast(initRes.message)
            }
        }
    }

    fun verifyRazorpayPayment(
        orderId: Int,
        razorpayOrderId: String,
        razorpayPaymentId: String,
        razorpaySignature: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckoutBusy = true) }
            when (val res = repository.verifyRazorpayPayment(
                orderId = orderId,
                razorpayOrderId = razorpayOrderId,
                razorpayPaymentId = razorpayPaymentId,
                razorpaySignature = razorpaySignature
            )) {
                is RepoResult.Success -> {
                    val details = (repository.fetchOrderDetails(orderId) as? RepoResult.Success)?.data
                    val orderNo = details?.orderNumber ?: _uiState.value.pendingPaymentOrder?.orderNumber.orEmpty()
                    activeRazorpayOrderId = null
                    activeRazorpayGatewayOrderId = null
                    resetCheckoutIdempotency()
                    refreshAuthenticatedData()
                    _uiState.update {
                        it.copy(
                            isCheckoutBusy = false,
                            pendingPaymentOrder = null,
                            trackedOrder = details ?: it.trackedOrder
                        )
                    }
                    showToast(res.data)
                    if (orderNo.isNotBlank()) {
                        _navigateAfterOrder.emit(orderNo)
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isCheckoutBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun handleRazorpaySdkFailure(orderId: Int, reason: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckoutBusy = true) }
            val res = repository.failOrCancelPayment(orderId, cancelOrder = false, reason = reason)
            val updatedOrder = (repository.fetchOrderDetails(orderId) as? RepoResult.Success)?.data
            val tracked = updatedOrder?.let {
                (repository.trackOrder(it.orderNumber) as? RepoResult.Success)?.data ?: it
            }
            refreshAuthenticatedData()
            _uiState.update { prev ->
                prev.copy(
                    isCheckoutBusy = false,
                    pendingPaymentOrder = updatedOrder ?: prev.pendingPaymentOrder,
                    trackedOrder = if (prev.trackedOrder?.id == orderId) tracked else prev.trackedOrder
                )
            }
            when (res) {
                is RepoResult.Success -> showToast(reason?.takeIf { it.isNotBlank() } ?: res.data)
                is RepoResult.Error -> showToast(res.message)
            }
        }
    }

    fun cancelUnpaidOrder(orderId: Int, reason: String = "Customer cancelled unpaid order") {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckoutBusy = true) }
            when (val res = repository.failOrCancelPayment(orderId, cancelOrder = true, reason = reason)) {
                is RepoResult.Success -> {
                    activeRazorpayOrderId = null
                    activeRazorpayGatewayOrderId = null
                    resetCheckoutIdempotency()
                    val orderNo = _uiState.value.pendingPaymentOrder?.orderNumber
                        ?: _uiState.value.orders.find { it.id == orderId }?.orderNumber
                    refreshAuthenticatedData()
                    val refreshedTrack = orderNo?.let {
                        (repository.trackOrder(it) as? RepoResult.Success)?.data
                    }
                    _uiState.update { prev ->
                        prev.copy(
                            isCheckoutBusy = false,
                            pendingPaymentOrder = null,
                            trackedOrder = if (prev.trackedOrder?.id == orderId) refreshedTrack else prev.trackedOrder
                        )
                    }
                    showToast(res.data)
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isCheckoutBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    // ==================== ORDER TRACKING & HISTORY ====================

    fun trackOrder(orderNumber: String, pincode: String? = null, mobile: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTrackingBusy = true, trackingError = null) }
            when (val res = repository.trackOrder(orderNumber, pincode, mobile)) {
                is RepoResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isTrackingBusy = false,
                            trackingError = null,
                            trackedOrder = res.data
                        )
                    }
                }
                is RepoResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isTrackingBusy = false,
                            trackingError = res.message,
                            trackedOrder = null
                        )
                    }
                    showToast(res.message)
                }
            }
        }
    }

    fun cancelOrder(orderId: Int, reason: String = "Cancelled from Melvore Android app") {
        viewModelScope.launch {
            _uiState.update { it.copy(isProfileBusy = true) }
            when (val res = repository.cancelOrder(orderId, reason)) {
                is RepoResult.Success -> {
                    refreshAuthenticatedData()
                    val trackedNo = _uiState.value.trackedOrder?.takeIf { it.id == orderId }?.orderNumber
                    val updatedTrack = trackedNo?.let { (repository.trackOrder(it) as? RepoResult.Success)?.data }
                    _uiState.update { prev ->
                        prev.copy(
                            isProfileBusy = false,
                            trackedOrder = updatedTrack ?: prev.trackedOrder
                        )
                    }
                    showToast(res.data)
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isProfileBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    // ==================== SUPPORT & CONTACT ====================

    fun createTicket(
        subject: String,
        orderNumberOrId: String?,
        category: String = "general",
        priority: String = "medium",
        message: String,
        onSuccess: () -> Unit = {}
    ) {
        val matchedOrderId = orderNumberOrId?.trim()?.removePrefix("#")?.let { input ->
            input.toIntOrNull()
                ?: _uiState.value.orders.find { it.orderNumber.equals(input, ignoreCase = true) }?.id
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isProfileBusy = true) }
            when (val res = repository.createSupportTicket(subject, matchedOrderId, category, priority, message)) {
                is RepoResult.Success -> {
                    val refreshed = (repository.fetchSupportTickets() as? RepoResult.Success)?.data
                    _uiState.update {
                        it.copy(
                            tickets = refreshed ?: (listOf(res.data) + it.tickets),
                            isProfileBusy = false
                        )
                    }
                    showToast(res.message)
                    onSuccess()
                }
                is RepoResult.Error -> {
                    _uiState.update { it.copy(isProfileBusy = false) }
                    showToast(res.message)
                }
            }
        }
    }

    fun replyTicket(ticketId: Int, message: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            when (val res = repository.replySupportTicket(ticketId, message)) {
                is RepoResult.Success -> {
                    val refreshed = (repository.fetchSupportTickets() as? RepoResult.Success)?.data
                    if (refreshed != null) {
                        _uiState.update { it.copy(tickets = refreshed) }
                    }
                    showToast(res.data)
                    onDone()
                }
                is RepoResult.Error -> showToast(res.message)
            }
        }
    }

    fun submitContact(
        name: String,
        email: String,
        phone: String,
        subject: String,
        message: String,
        onDone: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            when (val res = repository.submitContact(name, email, phone, subject, message)) {
                is RepoResult.Success -> {
                    showToast(res.data)
                    onDone(true)
                }
                is RepoResult.Error -> {
                    showToast(res.message)
                    onDone(false)
                }
            }
        }
    }

    override fun onCleared() {
        discovery.release()
        super.onCleared()
    }
}
