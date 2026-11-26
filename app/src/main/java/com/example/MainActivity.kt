package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.model.Product
import com.example.model.RazorpayCheckoutPayload
import com.example.model.requiresVariantSelection
import com.example.ui.components.CartBottomSheet
import com.example.ui.components.MelvoreBottomBar
import com.example.ui.components.MelvoreTopBar
import com.example.ui.components.PincodeSelectorDialog
import com.example.ui.components.ProfileMenuBottomSheet
import com.example.ui.components.QuickVariationSelectorBottomSheet
import com.example.ui.components.SearchExperienceModal
import com.example.ui.components.SensorySettingsBottomSheet
import com.example.ui.components.SmartContextualOfferBanner
import com.example.ui.components.WorldAudioConsentDialog
import com.example.ui.components.WorldContextQuickBar
import com.example.ui.navigation.AboutRoute
import com.example.ui.navigation.AuthRoute
import com.example.ui.navigation.BagRoute
import com.example.ui.navigation.BlogRoute
import com.example.ui.navigation.BrandDetailRoute
import com.example.ui.navigation.CategoriesRoute
import com.example.ui.navigation.CheckoutRoute
import com.example.ui.navigation.ConnectionDiagnosticsRoute
import com.example.ui.navigation.ContactRoute
import com.example.ui.navigation.DesignAuditRoute
import com.example.ui.navigation.DiscoverRoute
import com.example.ui.navigation.ElitePackRoute
import com.example.ui.navigation.EliteRoute
import com.example.ui.navigation.FaqRoute
import com.example.ui.navigation.HelpSupportRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.OrderTrackingRoute
import com.example.ui.navigation.ProductDetailsRoute
import com.example.ui.navigation.ProfileRoute
import com.example.ui.navigation.ShopRoute
import com.example.ui.navigation.StyleWorldRoute
import com.example.ui.navigation.VideosRoute
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BagScreen
import com.example.ui.screens.BlogScreen
import com.example.ui.screens.BrandDetailScreen
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.ContactScreen
import com.example.ui.screens.DesignAuditScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.ElitePackDetailScreen
import com.example.ui.screens.EliteScreen
import com.example.ui.screens.FaqScreen
import com.example.ui.screens.HelpSupportScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrderTrackingScreen
import com.example.ui.screens.ProductDetailsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ShopScreen
import com.example.ui.screens.StyleWorldScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.components.glassBackdropSource
import com.example.ui.theme.LocalMelvoreApiBaseUrl
import com.example.ui.theme.LocalMelvoreDesignManifest
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.MelvoreDynamicTheme
import com.example.ui.viewmodel.MelvoreViewModel
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    private val melvoreViewModel: MelvoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MelvoreApp(
                viewModel = melvoreViewModel,
                onLaunchRazorpay = { payload -> launchRazorpayCheckout(payload) }
            )
        }
    }

    private fun launchRazorpayCheckout(payload: RazorpayCheckoutPayload) {
        try {
            val checkout = Checkout()
            checkout.setKeyID(payload.keyId)
            val options = JSONObject().apply {
                put("name", "Melvore")
                put("description", "Order #${payload.orderNumber}")
                put("order_id", payload.razorpayOrderId)
                put("currency", payload.currency.ifBlank { "INR" })
                put("amount", payload.amountPaise)
                put(
                    "prefill",
                    JSONObject().apply {
                        if (payload.prefillName.isNotBlank()) put("name", payload.prefillName)
                        if (payload.prefillEmail.isNotBlank()) put("email", payload.prefillEmail)
                        if (payload.prefillContact.isNotBlank()) put("contact", payload.prefillContact)
                    }
                )
                put("theme", JSONObject().apply { put("color", "#0D0D10") })
            }
            checkout.open(this, options)
        } catch (e: Exception) {
            melvoreViewModel.handleRazorpaySdkFailure(
                orderId = payload.orderId,
                reason = e.message ?: "Unable to launch Razorpay checkout."
            )
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val orderId = melvoreViewModel.activeRazorpayOrderId ?: return
        val rzpOrderId = paymentData?.orderId?.takeIf { it.isNotBlank() }
            ?: melvoreViewModel.activeRazorpayGatewayOrderId.orEmpty()
        val rzpPaymentId = paymentData?.paymentId?.takeIf { it.isNotBlank() }
            ?: razorpayPaymentId.orEmpty()
        val rzpSignature = paymentData?.signature.orEmpty()

        if (rzpPaymentId.isBlank() || rzpSignature.isBlank()) {
            melvoreViewModel.handleRazorpaySdkFailure(
                orderId = orderId,
                reason = "Incomplete Razorpay verification signature returned."
            )
            return
        }

        melvoreViewModel.verifyRazorpayPayment(
            orderId = orderId,
            razorpayOrderId = rzpOrderId,
            razorpayPaymentId = rzpPaymentId,
            razorpaySignature = rzpSignature
        )
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        val orderId = melvoreViewModel.activeRazorpayOrderId ?: return
        val cleanReason = response?.takeIf { it.isNotBlank() }
            ?: "Razorpay payment was cancelled or failed (code $code)."
        melvoreViewModel.handleRazorpaySdkFailure(orderId = orderId, reason = cleanReason)
    }
}

@Composable
fun MelvoreApp(
    viewModel: MelvoreViewModel = viewModel(),
    onLaunchRazorpay: (RazorpayCheckoutPayload) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val discoveryState by viewModel.discovery.state.collectAsStateWithLifecycle()
    val isCartOpen by viewModel.isCartOpen.collectAsStateWithLifecycle()
    val isSearchOpen by viewModel.isSearchOpen.collectAsStateWithLifecycle()
    val isProfileSheetOpen by viewModel.isProfileSheetOpen.collectAsStateWithLifecycle()

    var launchVoiceSearch by remember { mutableStateOf(false) }
    var isPincodeDialogOpen by remember { mutableStateOf(false) }
    var quickAddProduct by remember { mutableStateOf<Product?>(null) }

    val handleQuickAdd: (Product) -> Unit = remember(viewModel) {
        { product ->
            if (requiresVariantSelection(product)) {
                quickAddProduct = product
            } else {
                viewModel.addToCart(product)
            }
        }
    }

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    LaunchedEffect(Unit) {
        viewModel.toastMessages.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.razorpayLaunchRequests.collect { payload ->
            onLaunchRazorpay(payload)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateAfterOrder.collect { orderNo ->
            navController.navigate(OrderTrackingRoute(orderNo)) {
                popUpTo(HomeRoute)
            }
        }
    }

    val currentSection = when {
        currentDest?.hasRoute<HomeRoute>() == true -> "home"
        currentDest?.hasRoute<DiscoverRoute>() == true ||
            currentDest?.hasRoute<ShopRoute>() == true ||
            currentDest?.hasRoute<CategoriesRoute>() == true ||
            currentDest?.hasRoute<StyleWorldRoute>() == true ||
            currentDest?.hasRoute<BrandDetailRoute>() == true -> "discover"
        currentDest?.hasRoute<VideosRoute>() == true -> "videos"
        currentDest?.hasRoute<EliteRoute>() == true ||
            currentDest?.hasRoute<ElitePackRoute>() == true -> "elite"
        currentDest?.hasRoute<BagRoute>() == true -> "bag"
        else -> ""
    }

    val isPrimaryRootTab = currentDest?.hasRoute<HomeRoute>() == true ||
        currentDest?.hasRoute<DiscoverRoute>() == true ||
        currentDest?.hasRoute<VideosRoute>() == true ||
        currentDest?.hasRoute<EliteRoute>() == true ||
        currentDest?.hasRoute<BagRoute>() == true

    val isLuxeRoute = currentDest?.hasRoute<EliteRoute>() == true ||
        currentDest?.hasRoute<ElitePackRoute>() == true
    val isStyleWorldRoute = currentDest?.hasRoute<StyleWorldRoute>() == true
    val activeWorldSlugForProduct = discoveryState.worldContext.activeWorldSlug.orEmpty()

    val simpleRouteName = when {
        currentDest?.hasRoute<HomeRoute>() == true -> "HomeRoute"
        currentDest?.hasRoute<DiscoverRoute>() == true -> "DiscoverRoute"
        currentDest?.hasRoute<VideosRoute>() == true -> "VideosRoute"
        currentDest?.hasRoute<EliteRoute>() == true -> "EliteRoute"
        currentDest?.hasRoute<ElitePackRoute>() == true -> "ElitePackRoute"
        currentDest?.hasRoute<BagRoute>() == true -> "BagRoute"
        currentDest?.hasRoute<BrandDetailRoute>() == true -> "BrandDetailRoute"
        currentDest?.hasRoute<StyleWorldRoute>() == true -> "StyleWorldRoute"
        currentDest?.hasRoute<HelpSupportRoute>() == true -> "HelpSupportRoute"
        currentDest?.hasRoute<ShopRoute>() == true -> "ShopRoute"
        currentDest?.hasRoute<CategoriesRoute>() == true -> "CategoriesRoute"
        currentDest?.hasRoute<ProductDetailsRoute>() == true -> "ProductDetailsRoute"
        currentDest?.hasRoute<CheckoutRoute>() == true -> "CheckoutRoute"
        currentDest?.hasRoute<OrderTrackingRoute>() == true -> "OrderTrackingRoute"
        currentDest?.hasRoute<AuthRoute>() == true -> "AuthRoute"
        currentDest?.hasRoute<ProfileRoute>() == true -> "ProfileRoute"
        currentDest?.hasRoute<AboutRoute>() == true -> "AboutRoute"
        currentDest?.hasRoute<BlogRoute>() == true -> "BlogRoute"
        currentDest?.hasRoute<FaqRoute>() == true -> "FaqRoute"
        currentDest?.hasRoute<ContactRoute>() == true -> "ContactRoute"
        currentDest?.hasRoute<DesignAuditRoute>() == true -> "DesignAuditRoute"
        currentDest?.hasRoute<ConnectionDiagnosticsRoute>() == true -> "ConnectionDiagnosticsRoute"
        else -> "HomeRoute"
    }

    LaunchedEffect(simpleRouteName, uiState.cartSubtotal, uiState.preferences) {
        viewModel.discovery.onRouteChanged(
            simpleRouteName = simpleRouteName,
            cartSubtotal = uiState.cartSubtotal,
            preferences = uiState.preferences
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val currentUi = viewModel.uiState.value
                    viewModel.discovery.onLifecycleForegroundChanged(true, currentUi.preferences)
                    viewModel.discovery.refreshAllDesignEndpoints(forceManifest = false)
                }
                Lifecycle.Event.ON_STOP -> {
                    viewModel.discovery.onLifecycleForegroundChanged(false, viewModel.uiState.value.preferences)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val effectiveDarkMode = when (uiState.themeMode.lowercase()) {
        "dark" -> true
        "system" -> isSystemDark
        else -> false
    }
    LaunchedEffect(effectiveDarkMode) {
        viewModel.discovery.setDarkMode(effectiveDarkMode)
    }

    val activeWorldKit = remember(discoveryState.designKit, discoveryState.resolvedTheme.worldSlug, discoveryState.preferredWorldSlug) {
        val slug = discoveryState.resolvedTheme.worldSlug ?: discoveryState.preferredWorldSlug
        discoveryState.designKit.world(slug)
    }
    val isHomeRoute = currentDest?.hasRoute<HomeRoute>() == true
    val isVideosRoute = currentDest?.hasRoute<VideosRoute>() == true
    val isAnyOverlayOpen = isCartOpen || isSearchOpen || isProfileSheetOpen ||
        isPincodeDialogOpen || quickAddProduct != null ||
        discoveryState.isWorldPickerOpen || discoveryState.isSensorySheetOpen
    val showFloatingCartBar = uiState.cartCount > 0 &&
        !isStyleWorldRoute &&
        currentDest?.hasRoute<BagRoute>() != true &&
        currentDest?.hasRoute<CheckoutRoute>() != true &&
        currentDest?.hasRoute<AuthRoute>() != true &&
        currentDest?.hasRoute<DesignAuditRoute>() != true &&
        currentDest?.hasRoute<ConnectionDiagnosticsRoute>() != true

    val currentDensity = LocalDensity.current
    val clampedDensity = remember(currentDensity) {
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale.coerceIn(0.9f, 1.15f)
        )
    }

    CompositionLocalProvider(
        LocalDensity provides clampedDensity,
        LocalMelvoreDesignManifest provides discoveryState.activeManifest,
        com.example.ui.theme.LocalDesignKit provides discoveryState.designKit,
        com.example.ui.theme.LocalWorld provides activeWorldKit,
        LocalMelvoreApiBaseUrl provides uiState.apiBaseUrl
    ) {
    MelvoreDynamicTheme(
        resolvedTheme = discoveryState.resolvedTheme,
        reducedMotion = uiState.preferences.reducedMotion
    ) {
        // Compute system bar icon polarity from the ACTUAL top/bottom surface luminance
        val currentSurfaces = LocalMelvoreSurfaces.current
        val topSystemBg = if (isVideosRoute && discoveryState.videos.isNotEmpty()) {
            currentSurfaces.imageOverlay
        } else {
            currentSurfaces.background
        }
        val bottomSystemBg = if (isStyleWorldRoute) currentSurfaces.background else currentSurfaces.surface
        val lightStatusIcons = topSystemBg.luminance() >= 0.5f
        val lightNavIcons = bottomSystemBg.luminance() >= 0.5f
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as? ComponentActivity)?.window ?: return@SideEffect
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightStatusIcons
                    isAppearanceLightNavigationBars = lightNavIcons
                }
            }
        }

        Scaffold(
            modifier = Modifier.glassBackdropSource(
                enabled = isAnyOverlayOpen && !uiState.preferences.reducedMotion
            ),
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (!isHomeRoute && !isStyleWorldRoute && !isVideosRoute) {
                    MelvoreTopBar(
                        user = uiState.user,
                        savedPincode = discoveryState.savedPincode,
                        wishlistCount = uiState.wishlistIds.size,
                        bagCount = uiState.cartCount,
                        showBackButton = !isPrimaryRootTab,
                        onBackClick = { navController.popBackStack() },
                        onPincodeClick = { isPincodeDialogOpen = true },
                        onWishlistClick = {
                            navController.navigate(
                                if (uiState.user != null) ProfileRoute("wishlist") else AuthRoute("login")
                            )
                        },
                        onBagClick = { viewModel.setCartOpen(true) },
                        onProfileClick = { viewModel.setProfileSheetOpen(true) }
                    )
                }
            },
            bottomBar = {
                if (!isStyleWorldRoute) {
                    MelvoreBottomBar(
                        currentSection = currentSection,
                        bagCount = uiState.cartCount,
                        onHomeClick = {
                            navController.navigate(HomeRoute) {
                                popUpTo(HomeRoute) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onDiscoverClick = {
                            navController.navigate(DiscoverRoute) {
                                popUpTo(HomeRoute)
                                launchSingleTop = true
                            }
                        },
                        onVideosClick = {
                            navController.navigate(VideosRoute) {
                                popUpTo(HomeRoute)
                                launchSingleTop = true
                            }
                        },
                        onEliteClick = {
                            navController.navigate(EliteRoute) {
                                popUpTo(HomeRoute)
                                launchSingleTop = true
                            }
                        },
                        onBagClick = {
                            navController.navigate(BagRoute) {
                                popUpTo(HomeRoute)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = if (isStyleWorldRoute) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                }
            ) {
                if (!isLuxeRoute && !isStyleWorldRoute && currentDest?.hasRoute<VideosRoute>() != true) {
                    WorldContextQuickBar(
                        resolvedTheme = discoveryState.resolvedTheme,
                        ambientAudioEnabled = uiState.preferences.ambientAudioEnabled,
                        onExitWorld = {
                            viewModel.discovery.exitFashionWorld(uiState.cartSubtotal, uiState.preferences)
                        },
                        onToggleMute = {
                            viewModel.toggleAmbientAudio(!uiState.preferences.ambientAudioEnabled)
                        },
                        onOpenSensorySheet = {
                            viewModel.discovery.setSensorySheetOpen(true)
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable<HomeRoute> {
                            HomeScreen(
                                uiState = uiState,
                                discoveryState = discoveryState,
                                onRetry = { viewModel.refreshAll() },
                                onOpenSearch = { voice ->
                                    launchVoiceSearch = voice
                                    viewModel.discovery.openSearchSuggestions()
                                    viewModel.setSearchOpen(true)
                                },
                                onOpenProduct = { p -> navController.navigate(ProductDetailsRoute(p.id, activeWorldSlugForProduct)) },
                                onQuickAdd = handleQuickAdd,
                                onToggleWishlist = { pid ->
                                    if (!viewModel.toggleWishlist(pid) && uiState.user == null) {
                                        navController.navigate(AuthRoute("login"))
                                    }
                                },
                                onNavigateShop = { cat, col, q ->
                                    navController.navigate(ShopRoute(category = cat, collection = col, query = q))
                                },
                                onNavigateBrand = { bid, slug ->
                                    navController.navigate(BrandDetailRoute(brandId = bid, slug = slug))
                                },
                                onNavigateStyleWorld = { sid, slug ->
                                    viewModel.discovery.enterFashionWorld(
                                        slug = slug,
                                        worldId = sid.takeIf { it > 0 },
                                        cartSubtotal = uiState.cartSubtotal,
                                        preferences = uiState.preferences
                                    )
                                    navController.navigate(StyleWorldRoute(styleWorldId = sid, slug = slug))
                                },
                                onNavigateCategories = { navController.navigate(CategoriesRoute) },
                                onNavigateDiscover = { navController.navigate(DiscoverRoute) },
                                onNavigateVideos = {
                                    navController.navigate(VideosRoute) {
                                        popUpTo(HomeRoute)
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateElite = {
                                    navController.navigate(EliteRoute) {
                                        popUpTo(HomeRoute)
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateBlog = { navController.navigate(BlogRoute) },
                                onLoadMoreProducts = { viewModel.discovery.loadMoreHomeProducts() },
                                reducedMotion = uiState.preferences.reducedMotion,
                                onScrollPreviewWorld = { slug -> viewModel.discovery.setScrollPreviewWorld(slug) },
                                onNavigateShopBrand = { bid -> navController.navigate(ShopRoute(brandId = bid)) },
                                onIncrementCart = { p -> viewModel.incrementProductInCart(p) },
                                onDecrementCart = { pid -> viewModel.decrementProductInCart(pid) },
                                onPincodeClick = { isPincodeDialogOpen = true },
                                onOpenWorldPicker = { viewModel.discovery.openWorldPicker() },
                                onWishlistClick = {
                                    navController.navigate(
                                        if (uiState.user != null) ProfileRoute("wishlist") else AuthRoute("login")
                                    )
                                },
                                onProfileClick = { viewModel.setProfileSheetOpen(true) },
                                onSelectHomeOccasionTab = { slug -> viewModel.discovery.selectHomeOccasionTab(slug) },
                                onApplySmartOffer = { offer -> viewModel.applySmartOfferCoupon(offer) },
                                onOpenDesignAudit = { navController.navigate(DesignAuditRoute) },
                                onOpenConnectionDiagnostics = { navController.navigate(ConnectionDiagnosticsRoute) },
                                onPullToRefresh = { viewModel.forcePullToRefresh() }
                            )
                        }

                        composable<DiscoverRoute> {
                            DiscoverScreen(
                                uiState = uiState,
                                discoveryState = discoveryState,
                                onLoadDiscover = { viewModel.discovery.loadDiscoverHub() },
                                onSelectBrandTab = { tab -> viewModel.discovery.selectBrandsTab(tab) },
                                onOpenSearch = {
                                    launchVoiceSearch = false
                                    viewModel.discovery.openSearchSuggestions()
                                    viewModel.setSearchOpen(true)
                                },
                                onOpenProduct = { p -> navController.navigate(ProductDetailsRoute(p.id, activeWorldSlugForProduct)) },
                                onQuickAdd = handleQuickAdd,
                                onToggleWishlist = { pid ->
                                    if (!viewModel.toggleWishlist(pid) && uiState.user == null) {
                                        navController.navigate(AuthRoute("login"))
                                    }
                                },
                                onOpenBrand = { bid, slug ->
                                    navController.navigate(BrandDetailRoute(brandId = bid, slug = slug))
                                },
                                onOpenStyleWorld = { sid, slug ->
                                    viewModel.discovery.enterFashionWorld(
                                        slug = slug,
                                        worldId = sid.takeIf { it > 0 },
                                        cartSubtotal = uiState.cartSubtotal,
                                        preferences = uiState.preferences
                                    )
                                    navController.navigate(StyleWorldRoute(styleWorldId = sid, slug = slug))
                                },
                                onSelectCategory = { slug -> navController.navigate(ShopRoute(category = slug)) },
                                onSelectCollection = { slug -> navController.navigate(ShopRoute(collection = slug)) }
                            )
                        }

                composable<StyleWorldRoute> { entry ->
                    val route = entry.toRoute<StyleWorldRoute>()
                    val clipboardManager = LocalClipboardManager.current
                    StyleWorldScreen(
                        styleWorldId = route.styleWorldId,
                        slug = route.slug,
                        uiState = uiState,
                        discoveryState = discoveryState,
                        onLoadStyleWorld = { id, sl -> viewModel.discovery.loadStyleWorldDetail(id, sl) },
                        onOpenProduct = { p ->
                            val targetSlug = route.slug.ifBlank { activeWorldSlugForProduct }
                            navController.navigate(ProductDetailsRoute(p.id, targetSlug))
                        },
                        onQuickAdd = handleQuickAdd,
                        onToggleWishlist = { pid ->
                            if (!viewModel.toggleWishlist(pid) && uiState.user == null) {
                                navController.navigate(AuthRoute("login"))
                            }
                        },
                        onNavigateBack = {
                            viewModel.discovery.exitFashionWorld(uiState.cartSubtotal, uiState.preferences)
                            navController.popBackStack()
                        },
                        onToggleSound = {
                            viewModel.toggleAmbientAudio(!uiState.preferences.ambientAudioEnabled)
                        },
                        onVideoAudioActiveChanged = { active ->
                            viewModel.discovery.setVideoMediaPlayingAudio(active, uiState.preferences)
                        },
                        onSubmitVibeScore = { worldId, worldSlug, score ->
                            viewModel.discovery.submitStyleWorldSatisfaction(worldId, worldSlug, score, uiState.preferences)
                        },
                        onCopyCoupon = { code ->
                            clipboardManager.setText(AnnotatedString(code))
                            viewModel.showToast("Coupon $code copied")
                        },
                        onApplyCoupon = { code ->
                            viewModel.validateCoupon(code, "online") { }
                        },
                        onDismissVibeReward = {
                            viewModel.discovery.dismissUnlockedFeedbackReward()
                        },
                        onOpenCollection = { col ->
                            navController.navigate(ShopRoute(collection = col))
                        },
                        onSaveScrollPosition = { sl, index, offset ->
                            viewModel.discovery.saveStyleWorldScrollPosition(sl, index, offset)
                        }
                    )
                }

                composable<BrandDetailRoute> { entry ->
                    val route = entry.toRoute<BrandDetailRoute>()
                    BrandDetailScreen(
                        brandId = route.brandId,
                        slug = route.slug,
                        uiState = uiState,
                        discoveryState = discoveryState,
                        onLoadBrand = { id, sl -> viewModel.discovery.loadBrandDetail(id, sl) },
                        onOpenProduct = { p -> navController.navigate(ProductDetailsRoute(p.id, activeWorldSlugForProduct)) },
                        onQuickAdd = handleQuickAdd,
                        onToggleWishlist = { pid ->
                            if (!viewModel.toggleWishlist(pid) && uiState.user == null) {
                                navController.navigate(AuthRoute("login"))
                            }
                        }
                    )
                }

                composable<VideosRoute> {
                    VideosScreen(
                        discoveryState = discoveryState,
                        currency = uiState.currency,
                        onLoadVideos = { viewModel.discovery.loadVideosFeed() },
                        onLoadMoreVideos = { viewModel.discovery.loadMoreVideos() },
                        onRefreshVideos = { viewModel.discovery.loadVideosFeed(forceRefresh = true) },
                        onToggleLike = { vid -> viewModel.discovery.toggleVideoLike(vid) },
                        onToggleSave = { vid -> viewModel.discovery.toggleVideoSave(vid) },
                        onRecordVideoEvent = { vid, evt, sec ->
                            viewModel.discovery.recordVideoEvent(vid, evt, sec)
                        },
                        onOpenBrand = { bid, slug ->
                            navController.navigate(BrandDetailRoute(brandId = bid, slug = slug))
                        },
                        onOpenProduct = { p -> navController.navigate(ProductDetailsRoute(p.id, activeWorldSlugForProduct)) },
                        onAddToBag = handleQuickAdd,
                        onVideoAudioActiveChanged = { active ->
                            viewModel.discovery.setVideoMediaPlayingAudio(active, uiState.preferences)
                        }
                    )
                }

                composable<EliteRoute> {
                    EliteScreen(
                        uiState = uiState,
                        discoveryState = discoveryState,
                        onLoadElite = { viewModel.discovery.loadEliteHome() },
                        onOpenPack = { pid, slug ->
                            navController.navigate(ElitePackRoute(packId = pid, slug = slug))
                        },
                        onOpenBrand = { bid, slug ->
                            navController.navigate(BrandDetailRoute(brandId = bid, slug = slug))
                        },
                        onOpenProduct = { p -> navController.navigate(ProductDetailsRoute(p.id)) },
                        onQuickAdd = handleQuickAdd,
                        onToggleWishlist = { pid ->
                            if (!viewModel.toggleWishlist(pid) && uiState.user == null) {
                                navController.navigate(AuthRoute("login"))
                            }
                        },
                        onOpenVideos = { navController.navigate(VideosRoute) }
                    )
                }

                composable<ElitePackRoute> { entry ->
                    val route = entry.toRoute<ElitePackRoute>()
                    ElitePackDetailScreen(
                        packId = route.packId,
                        slug = route.slug,
                        uiState = uiState,
                        discoveryState = discoveryState,
                        onLoadPack = { pid, sl -> viewModel.discovery.loadElitePack(pid, sl) },
                        onOpenProduct = { p -> navController.navigate(ProductDetailsRoute(p.id)) },
                        onQuickAdd = handleQuickAdd,
                        onToggleWishlist = { pid ->
                            if (!viewModel.toggleWishlist(pid) && uiState.user == null) {
                                navController.navigate(AuthRoute("login"))
                            }
                        },
                        onOpenBrand = { bid, sl ->
                            navController.navigate(BrandDetailRoute(brandId = bid, slug = sl))
                        },
                        onOpenRelatedPack = { pid, sl ->
                            navController.navigate(ElitePackRoute(packId = pid, slug = sl))
                        }
                    )
                }

                composable<BagRoute> {
                    BagScreen(
                        uiState = uiState,
                        onUpdateQty = { cid, delta -> viewModel.updateCartQty(cid, delta) },
                        onRemoveItem = { cid -> viewModel.removeCartItem(cid) },
                        onBrowseProducts = { navController.navigate(DiscoverRoute) },
                        onProceedCheckout = { navController.navigate(CheckoutRoute) }
                    )
                }

                composable<HelpSupportRoute> { entry ->
                    val route = entry.toRoute<HelpSupportRoute>()
                    HelpSupportScreen(
                        initialTopicSlug = route.topicSlug,
                        uiState = uiState,
                        discoveryState = discoveryState,
                        onLoadHelp = { viewModel.discovery.loadHelpCenter() },
                        onOpenTopic = { tid, sl -> viewModel.discovery.openHelpTopic(tid, sl) },
                        onClearFlow = { viewModel.discovery.clearActiveHelpFlow() },
                        onEvaluateOrder = { tid, fid, oid, oiid, note ->
                            viewModel.discovery.evaluateHelpOrder(tid, fid, oid, oiid, note)
                        },
                        onUploadEvidencePhoto = { uri -> viewModel.discovery.uploadSupportEvidencePhoto(uri) },
                        onEscalateTicket = { tid, fid, oid, oiid, sub, cat, note, done ->
                            viewModel.discovery.escalateHelpToTicket(tid, fid, oid, oiid, sub, cat, note) {
                                viewModel.refreshAccountState()
                                done()
                            }
                        },
                        onNavigateTickets = { navController.navigate(ProfileRoute("tickets")) },
                        onNavigateAuth = { navController.navigate(AuthRoute("login")) }
                    )
                }

                composable<ShopRoute> { entry ->
                    val route = entry.toRoute<ShopRoute>()
                    ShopScreen(
                        uiState = uiState,
                        initialCategory = route.category,
                        initialCollection = route.collection,
                        initialQuery = route.query,
                        initialBrandId = route.brandId,
                        onFetchCatalog = { cat, col, q, sort, bid ->
                            viewModel.loadCatalog(cat, col, q, sort, bid)
                        },
                        onOpenProduct = { p -> navController.navigate(ProductDetailsRoute(p.id, activeWorldSlugForProduct)) },
                        onQuickAdd = handleQuickAdd,
                        onToggleWishlist = { pid ->
                            if (!viewModel.toggleWishlist(pid) && uiState.user == null) {
                                navController.navigate(AuthRoute("login"))
                            }
                        }
                    )
                }

                composable<CategoriesRoute> {
                    CategoriesScreen(
                        uiState = uiState,
                        onRetry = { viewModel.refreshAll() },
                        onSelectCategory = { slug -> navController.navigate(ShopRoute(category = slug)) },
                        onSelectCollection = { slug -> navController.navigate(ShopRoute(collection = slug)) },
                        onOpenProduct = { p -> navController.navigate(ProductDetailsRoute(p.id, activeWorldSlugForProduct)) },
                        onIncrementCart = { p -> viewModel.incrementProductInCart(p) },
                        onDecrementCart = { pid -> viewModel.decrementProductInCart(pid) }
                    )
                }

                composable<ProductDetailsRoute> { entry ->
                    val route = entry.toRoute<ProductDetailsRoute>()
                    LaunchedEffect(route.worldSlug) {
                        if (route.worldSlug.isNotBlank() && discoveryState.worldContext.activeWorldSlug != route.worldSlug) {
                            viewModel.discovery.enterFashionWorld(
                                slug = route.worldSlug,
                                cartSubtotal = uiState.cartSubtotal,
                                preferences = uiState.preferences
                            )
                        }
                    }
                    ProductDetailsScreen(
                        productId = route.productId,
                        worldSlug = route.worldSlug,
                        uiState = uiState,
                        discoveryState = discoveryState,
                        onLoadDetails = { id -> viewModel.loadProductDetails(id) },
                        onLoadRecommendations = { id, wSlug -> viewModel.discovery.loadProductRecommendations(id, wSlug) },
                        onBack = { navController.popBackStack() },
                        onOpenProduct = { p ->
                            navController.navigate(ProductDetailsRoute(p.id, route.worldSlug.ifBlank { activeWorldSlugForProduct }))
                        },
                        onOpenBrand = { bid, sl ->
                            navController.navigate(BrandDetailRoute(brandId = bid, slug = sl))
                        },
                        onToggleWishlist = { pid ->
                            if (!viewModel.toggleWishlist(pid) && uiState.user == null) {
                                navController.navigate(AuthRoute("login"))
                            }
                        },
                        onAddToCart = { p, color, size, qty -> viewModel.addToCart(p, color, size, qty) },
                        onBuyNow = { p, color, size, qty ->
                            viewModel.addToCart(p, color, size, qty) {
                                navController.navigate(CheckoutRoute)
                            }
                        },
                        onCheckDelivery = { pin, pid -> viewModel.discovery.checkProductDelivery(pin, pid) },
                        onRequestReviewOtp = { oiid -> viewModel.discovery.requestReviewOtp(oiid) },
                        onVerifyReviewOtp = { otp -> viewModel.discovery.verifyReviewOtp(otp) },
                        onUploadReviewPhoto = { uri -> viewModel.discovery.uploadReviewPhoto(uri) },
                        onRemoveReviewPhoto = { url -> viewModel.discovery.removeReviewPhoto(url) },
                        onSubmitVerifiedReview = { pid, oiid, r, t, c, done ->
                            viewModel.discovery.submitVerifiedReview(pid, oiid, r, t, c, done)
                        },
                        onToggleReviewHelpful = { rid -> viewModel.discovery.toggleReviewHelpful(rid) },
                        onNavigateAuth = { navController.navigate(AuthRoute("login")) }
                    )
                }

                composable<CheckoutRoute> {
                    CheckoutScreen(
                        uiState = uiState,
                        onBackToShop = { navController.navigate(ShopRoute()) },
                        onNavigateLogin = { navController.navigate(AuthRoute("login")) },
                        onValidateCoupon = { code, method, onRes ->
                            viewModel.validateCoupon(code, method, onRes)
                        },
                        onPlaceOrder = { addrId, draft, method, coupon, notes ->
                            viewModel.submitCheckoutOrder(addrId, draft, method, coupon, notes)
                        },
                        onRetryRazorpay = { oid -> viewModel.retryRazorpayPayment(oid) },
                        onCancelUnpaidOrder = { oid -> viewModel.cancelUnpaidOrder(oid) }
                    )
                }

                composable<OrderTrackingRoute> { entry ->
                    val route = entry.toRoute<OrderTrackingRoute>()
                    OrderTrackingScreen(
                        initialOrderNumber = route.orderNumber,
                        uiState = uiState,
                        onTrackOrder = { orderNo, pin, mob -> viewModel.trackOrder(orderNo, pin, mob) },
                        onRetryRazorpay = { oid -> viewModel.retryRazorpayPayment(oid) },
                        onCancelUnpaidOrder = { oid -> viewModel.cancelUnpaidOrder(oid) },
                        onNavigateContact = { navController.navigate(HelpSupportRoute()) }
                    )
                }

                composable<AuthRoute> { entry ->
                    val route = entry.toRoute<AuthRoute>()
                    AuthScreen(
                        initialTab = route.initialTab,
                        isAuthBusy = uiState.isAuthBusy,
                        onLogin = { e, p, ok -> viewModel.login(e, p, ok) },
                        onRegister = { n, e, ph, p, ok -> viewModel.register(n, e, ph, p, ok) },
                        onGoogleIdToken = { idToken, ok -> viewModel.loginWithGoogleIdToken(idToken, ok) },
                        onSendOtp = { e, sent -> viewModel.sendEmailOtp(e, sent) },
                        onVerifyOtp = { e, otp, ok -> viewModel.verifyEmailOtp(e, otp, ok) },
                        onRequestPasswordReset = { e, sent -> viewModel.requestPasswordReset(e, sent) },
                        onResetPassword = { e, otp, np, ok -> viewModel.resetPassword(e, otp, np, ok) },
                        onAuthSuccess = {
                            navController.navigate(ProfileRoute("overview")) {
                                popUpTo(HomeRoute)
                            }
                        }
                    )
                }

                composable<ProfileRoute> { entry ->
                    val route = entry.toRoute<ProfileRoute>()
                    ProfileScreen(
                        initialTab = route.tab,
                        uiState = uiState,
                        onNavigateAuth = { tab -> navController.navigate(AuthRoute(tab)) },
                        onNavigateShop = { navController.navigate(ShopRoute()) },
                        onOpenProduct = { p -> navController.navigate(ProductDetailsRoute(p.id)) },
                        onAddToCart = handleQuickAdd,
                        onToggleWishlist = { pid -> viewModel.toggleWishlist(pid) },
                        onSaveProfile = { n, ph, alt -> viewModel.updateProfile(n, ph, alt) },
                        onTrackOrder = { ordNo -> navController.navigate(OrderTrackingRoute(ordNo)) },
                        onCancelOrder = { oid -> viewModel.cancelOrder(oid) },
                        onRetryRazorpay = { oid -> viewModel.retryRazorpayPayment(oid) },
                        onAddAddress = { addr, ok -> viewModel.saveAddress(addr) { if (it != null) ok() } },
                        onSetDefaultAddress = { aid -> viewModel.setDefaultAddress(aid) },
                        onDeleteAddress = { aid -> viewModel.deleteAddress(aid) },
                        onCreateTicket = { sub, ordNo, cat, prio, msg, ok ->
                            viewModel.createTicket(sub, ordNo, cat, prio, msg, ok)
                        },
                        onReplyTicket = { tid, msg, done -> viewModel.replyTicket(tid, msg, done) },
                        onSetTheme = { theme -> viewModel.setTheme(theme) },
                        onUpdatePreferences = { patch -> viewModel.updateLocalPreferences(patch) },
                        onRequestPasswordReset = { e, sent -> viewModel.requestPasswordReset(e, sent) },
                        onResetPassword = { e, otp, np, ok -> viewModel.resetPassword(e, otp, np, ok) },
                        onOpenHelpCenter = { navController.navigate(HelpSupportRoute()) },
                        onLogout = {
                            viewModel.logout()
                            navController.navigate(HomeRoute) {
                                popUpTo(HomeRoute) { inclusive = true }
                            }
                        },
                        discoveryState = discoveryState,
                        onOpenDesignAudit = { navController.navigate(DesignAuditRoute) },
                        onOpenConnectionDiagnostics = { navController.navigate(ConnectionDiagnosticsRoute) },
                        onRefreshDesignManifest = {
                            viewModel.discovery.refreshAllDesignEndpoints(forceManifest = true)
                            viewModel.showToast("Syncing design system from server…")
                        }
                    )
                }

                composable<AboutRoute> {
                    AboutScreen(
                        uiState = uiState,
                        onNavigateShop = { navController.navigate(ShopRoute()) },
                        onNavigateContact = { navController.navigate(HelpSupportRoute()) },
                        onOpenConnectionDiagnostics = { navController.navigate(ConnectionDiagnosticsRoute) }
                    )
                }

                composable<BlogRoute> {
                    BlogScreen(
                        uiState = uiState,
                        onOpenPost = { id -> viewModel.loadBlogDetails(id) },
                        onClearPost = { viewModel.clearSelectedBlogPost() }
                    )
                }

                composable<FaqRoute> {
                    FaqScreen(
                        uiState = uiState,
                        onNavigateContact = { navController.navigate(HelpSupportRoute()) }
                    )
                }

                composable<ContactRoute> {
                    ContactScreen(
                        uiState = uiState,
                        onSubmitContact = { n, e, ph, sub, msg, done ->
                            viewModel.submitContact(n, e, ph, sub, msg, done)
                        }
                    )
                }

                composable<DesignAuditRoute> {
                    DesignAuditScreen(
                        discoveryState = discoveryState,
                        themeMode = uiState.themeMode,
                        onSetTheme = { theme -> viewModel.setTheme(theme) },
                        onForceRefreshManifest = {
                            viewModel.discovery.refreshAllDesignEndpoints(forceManifest = true)
                            viewModel.showToast("Force-refreshing remote manifest & themes…")
                        }
                    )
                }

                composable<ConnectionDiagnosticsRoute> {
                    com.example.ui.screens.ConnectionDiagnosticsScreen(
                        uiState = uiState,
                        discoveryState = discoveryState,
                        apiClient = viewModel.apiClient,
                        sessionStore = viewModel.sessionStore,
                        onResetApiBaseUrl = { viewModel.resetApiBaseUrlToDefault() },
                        onForceSyncAll = {
                            viewModel.forcePullToRefresh()
                            viewModel.showToast("Force-syncing all backend endpoints…")
                        },
                        onCopyToast = { msg -> viewModel.showToast(msg) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        SmartContextualOfferBanner(
                            offer = discoveryState.eligibleOffer,
                            cartSubtotal = uiState.cartSubtotal,
                            currency = uiState.currency,
                            onApplyOffer = { offer -> viewModel.applySmartOfferCoupon(offer) },
                            onDismissOffer = { offer -> viewModel.discovery.dismissSmartOffer(offer) }
                        )
                        com.example.ui.components.FloatingCartBar(
                            visible = showFloatingCartBar,
                            itemCount = uiState.cartCount,
                            subtotal = uiState.cartSubtotal,
                            currency = uiState.currency,
                            freeShippingMinimum = discoveryState.homeFeed.freeShippingMinimum
                                .takeIf { it > 0.0 } ?: uiState.design.siteSettings.freeShippingMinimum,
                            onViewCartClick = {
                                navController.navigate(BagRoute) {
                                    popUpTo(HomeRoute)
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }

        if (isSearchOpen) {
            SearchExperienceModal(
                discoveryState = discoveryState,
                categories = discoveryState.homeFeed.categories.ifEmpty { uiState.categories },
                currency = uiState.currency,
                initialLaunchVoice = launchVoiceSearch,
                onClose = {
                    launchVoiceSearch = false
                    viewModel.setSearchOpen(false)
                },
                onQueryChange = { q -> viewModel.discovery.onSearchQueryChanged(q) },
                onSubmitQuery = { q -> viewModel.discovery.submitSearch(q) },
                onUpdateFilters = { filters -> viewModel.discovery.updateSearchFilters(filters) },
                onSelectProduct = { p ->
                    viewModel.setSearchOpen(false)
                    navController.navigate(ProductDetailsRoute(p.id, activeWorldSlugForProduct))
                },
                onSelectCategory = { slug ->
                    viewModel.setSearchOpen(false)
                    navController.navigate(ShopRoute(category = slug))
                },
                onViewAllResults = { q ->
                    viewModel.setSearchOpen(false)
                    navController.navigate(ShopRoute(query = q))
                },
                onVoiceError = { msg -> viewModel.showToast(msg) }
            )
        }

        if (isPincodeDialogOpen) {
            PincodeSelectorDialog(
                currentPincode = discoveryState.savedPincode,
                isChecking = discoveryState.isCheckingDelivery,
                onDismiss = { isPincodeDialogOpen = false },
                onApplyPincode = { pin -> viewModel.discovery.updatePincode(pin) }
            )
        }

        if (isCartOpen) {
            CartBottomSheet(
                cartItems = uiState.cartItems,
                cartCount = uiState.cartCount,
                cartSummary = uiState.cartSummary,
                isCartBusy = uiState.isCartBusy,
                currency = uiState.currency,
                onDismiss = { viewModel.setCartOpen(false) },
                onUpdateQty = { cid, delta -> viewModel.updateCartQty(cid, delta) },
                onRemoveItem = { cid -> viewModel.removeCartItem(cid) },
                onBrowseProducts = {
                    viewModel.setCartOpen(false)
                    navController.navigate(DiscoverRoute)
                },
                onCheckout = {
                    viewModel.setCartOpen(false)
                    navController.navigate(CheckoutRoute)
                }
            )
        }

        quickAddProduct?.let { targetProduct ->
            QuickVariationSelectorBottomSheet(
                product = targetProduct,
                currency = uiState.currency,
                isCartBusy = uiState.isCartBusy,
                onDismiss = { quickAddProduct = null },
                onConfirmAdd = { color, size ->
                    viewModel.addToCart(targetProduct, color = color, size = size) {
                        quickAddProduct = null
                    }
                },
                onOpenFullDetails = {
                    quickAddProduct = null
                    navController.navigate(ProductDetailsRoute(targetProduct.id, activeWorldSlugForProduct))
                }
            )
        }

        if (isProfileSheetOpen) {
            ProfileMenuBottomSheet(
                user = uiState.user,
                pendingOrdersCount = uiState.pendingOrdersCount,
                wishlistCount = uiState.wishlistIds.size,
                onDismiss = { viewModel.setProfileSheetOpen(false) },
                onNavigateProfileTab = { tab ->
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(ProfileRoute(tab))
                },
                onNavigateAuth = { tab ->
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(AuthRoute(tab))
                },
                onNavigateHome = {
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(HomeRoute)
                },
                onNavigateShop = {
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(DiscoverRoute)
                },
                onNavigateCategories = {
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(CategoriesRoute)
                },
                onNavigateElite = {
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(EliteRoute) {
                        popUpTo(HomeRoute)
                        launchSingleTop = true
                    }
                },
                onNavigateTrack = {
                    viewModel.setProfileSheetOpen(false)
                    val latest = uiState.orders.firstOrNull()?.orderNumber ?: ""
                    navController.navigate(OrderTrackingRoute(latest))
                },
                onNavigateBlog = {
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(BlogRoute)
                },
                onNavigateFaq = {
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(HelpSupportRoute())
                },
                onNavigateContact = {
                    viewModel.setProfileSheetOpen(false)
                    navController.navigate(ContactRoute)
                },
                onLogout = {
                    viewModel.logout()
                    navController.navigate(HomeRoute) {
                        popUpTo(HomeRoute) { inclusive = true }
                    }
                }
            )
        }

        val pendingAudioWorld = discoveryState.pendingAudioConsentWorldName
        if (pendingAudioWorld != null && !uiState.preferences.audioConsentAnswered) {
            WorldAudioConsentDialog(
                worldName = pendingAudioWorld,
                onAllow = { viewModel.answerAudioConsent(allowAmbient = true) },
                onKeepMuted = { viewModel.answerAudioConsent(allowAmbient = false) }
            )
        }

        if (discoveryState.isWorldPickerOpen) {
            com.example.ui.components.WorldPickerBottomSheet(
                worlds = discoveryState.bootstrapConfig.fashionWorlds,
                selectedWorldSlug = discoveryState.preferredWorldSlug,
                onSelectWorld = { slug -> viewModel.selectPreferredWorld(slug) },
                onDismiss = { viewModel.discovery.closeWorldPicker() }
            )
        }

        if (discoveryState.isSensorySheetOpen) {
            SensorySettingsBottomSheet(
                preferences = uiState.preferences,
                onToggleAmbientAudio = { enabled -> viewModel.toggleAmbientAudio(enabled) },
                onToggleUiSfx = { enabled -> viewModel.toggleUiSfx(enabled) },
                onToggleReducedMotion = { enabled -> viewModel.toggleReducedMotion(enabled) },
                onToggleVideoAutoplayMuted = { enabled -> viewModel.toggleVideoAutoplayMuted(enabled) },
                onToggleDataSaverMode = { enabled -> viewModel.toggleDataSaverMode(enabled) },
                onDismiss = { viewModel.discovery.setSensorySheetOpen(false) }
            )
        }
    }
    }
}
