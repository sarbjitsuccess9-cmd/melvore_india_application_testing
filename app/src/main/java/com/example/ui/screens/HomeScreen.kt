package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.model.BrandProfileItem
import com.example.model.CampaignItem
import com.example.model.HomeSection
import com.example.model.HomeSectionItem
import com.example.model.OccasionTheme
import com.example.model.Product
import com.example.model.WorldVectorAsset
import com.example.model.parseHexColor
import androidx.compose.ui.graphics.luminance
import com.example.ui.components.CampaignAtmosphericEffect
import com.example.ui.components.FrontSlider
import com.example.ui.components.HomeFeedSkeleton
import com.example.ui.components.MelvoreAnimatedEntrance
import com.example.ui.components.MelvoreAnimationKey
import com.example.ui.components.MelvoreErrorState
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.components.MetallicShineButton
import com.example.ui.components.OfflineCacheBanner
import com.example.ui.components.HomeOccasionTabItem
import com.example.ui.components.ProductCard
import com.example.ui.components.QuickCommerceCategoryTileGrid
import com.example.ui.components.QuickCommerceFestivalHeroBanner
import com.example.ui.components.QuickCommerceOccasionTabStrip
import com.example.ui.components.QuickCommerceOfferStrip
import com.example.ui.components.QuickCommerceProductRail
import com.example.ui.components.QuickCommerceStickyHeader
import com.example.ui.components.RemoteDesignRegistry
import com.example.ui.components.RemoteSvgDivider
import com.example.ui.components.RemoteSvgSkinImage
import com.example.ui.components.WorldMapDiscoverySection
import com.example.ui.components.WorldVectorArtLayer
import com.example.ui.design.AnimatedSvg
import com.example.ui.theme.LocalDesignKit
import com.example.ui.theme.LocalFashionWorldTheme
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreDesignManifest
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.LocalWorld
import com.example.ui.theme.MelvoreBrandFontFamily
import com.example.ui.theme.contrastOnColor
import com.example.ui.theme.ensureContrast
import com.example.ui.theme.fontFamilyForStyleKey
import com.example.ui.theme.parseHexColor as parseThemeHexColor
import com.example.ui.viewmodel.DiscoveryUiState
import com.example.ui.viewmodel.MelvoreUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: MelvoreUiState,
    discoveryState: DiscoveryUiState,
    onRetry: () -> Unit,
    onOpenSearch: (launchVoice: Boolean) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    onNavigateShop: (category: String, collection: String, query: String) -> Unit,
    onNavigateBrand: (brandId: Int, slug: String) -> Unit,
    onNavigateStyleWorld: (styleId: Int, slug: String) -> Unit,
    onNavigateCategories: () -> Unit,
    onNavigateDiscover: () -> Unit,
    onNavigateVideos: () -> Unit,
    onNavigateElite: () -> Unit,
    onNavigateBlog: () -> Unit,
    onLoadMoreProducts: () -> Unit,
    reducedMotion: Boolean = false,
    onScrollPreviewWorld: (String?) -> Unit = {},
    onNavigateShopBrand: (Int) -> Unit = { bid -> onNavigateBrand(bid, "") },
    onIncrementCart: (Product) -> Unit = onQuickAdd,
    onDecrementCart: (Int) -> Unit = {},
    onPincodeClick: () -> Unit = {},
    onOpenWorldPicker: () -> Unit = {},
    onWishlistClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSelectHomeOccasionTab: (String?) -> Unit = {},
    onApplySmartOffer: (com.example.model.SmartOffer) -> Unit = {},
    onOpenDesignAudit: () -> Unit = {},
    onOpenConnectionDiagnostics: () -> Unit = {},
    onPullToRefresh: () -> Unit = onRetry,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val animationsEnabled = rememberAnimationsEnabled() && !reducedMotion
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key.toString().startsWith("editorial_world_") }
                ?.key
                ?.toString()
                ?.removePrefix("editorial_world_")
        }
            .distinctUntilChanged()
            .collect { visibleWorldSlug ->
                onScrollPreviewWorld(visibleWorldSlug)
            }
    }

    val homeFeed = discoveryState.homeFeed
    val heroSlides = remember(homeFeed.heroSlides, uiState.design.heroSlides) {
        homeFeed.heroSlides.ifEmpty { uiState.design.heroSlides }
    }
    val categories = remember(homeFeed.categories, uiState.categories) {
        homeFeed.categories.ifEmpty { uiState.categories }
    }
    val sections = homeFeed.sections
    val campaign = homeFeed.activeCampaign
    val promoBanners = remember(homeFeed.promoBanners, uiState.design.promoBanners) {
        homeFeed.promoBanners.ifEmpty { uiState.design.promoBanners }
    }
    val freeShipMin = remember(homeFeed.freeShippingMinimum, uiState.design.siteSettings.freeShippingMinimum) {
        homeFeed.freeShippingMinimum.takeIf { it > 0.0 } ?: uiState.design.siteSettings.freeShippingMinimum
    }
    val gridProducts = remember(homeFeed.discoveryProducts, discoveryState.extraFeedProducts, uiState.products) {
        val base = homeFeed.discoveryProducts.ifEmpty { uiState.products }
        (base + discoveryState.extraFeedProducts).distinctBy { it.id }
    }
    val mergedBrands = remember(homeFeed.brands, discoveryState.directoryBrands, uiState.brands) {
        homeFeed.brands
            .ifEmpty { discoveryState.directoryBrands }
            .ifEmpty {
                uiState.brands.map { b ->
                    BrandProfileItem(
                        brandId = b.id,
                        name = b.name,
                        slug = b.slug,
                        description = b.description,
                        logoUrl = b.logoUrl,
                        originCountry = b.originCountry,
                        productCount = b.productCount
                    )
                }
            }
    }

    val newArrivals = remember(homeFeed.newArrivals, gridProducts) {
        homeFeed.newArrivals.ifEmpty {
            gridProducts.filter { it.badge.equals("NEW", ignoreCase = true) }.ifEmpty { gridProducts.take(8) }
        }
    }
    val bestsellers = remember(homeFeed.bestsellers, gridProducts) {
        homeFeed.bestsellers.ifEmpty {
            gridProducts.filter { it.badge.equals("BESTSELLER", ignoreCase = true) }
                .ifEmpty { gridProducts.sortedByDescending { it.reviewCount }.take(4) }
        }
    }
    val trendingProducts = remember(homeFeed.trendingProducts, gridProducts) {
        homeFeed.trendingProducts.ifEmpty {
            gridProducts.filter { it.badge.equals("HOT", ignoreCase = true) || it.badge.equals("TREND", ignoreCase = true) }
                .ifEmpty { gridProducts.drop(4).take(8).ifEmpty { gridProducts.take(6) } }
        }
    }
    val mostWishlisted = remember(homeFeed.mostWishlisted, gridProducts) {
        homeFeed.mostWishlisted.ifEmpty {
            gridProducts.sortedByDescending { it.rating }.take(6)
        }
    }
    val signaturePieces = remember(gridProducts) {
        val outerwear = gridProducts.filter {
            it.category.contains("jacket", ignoreCase = true) ||
                it.name.contains("jacket", ignoreCase = true) ||
                it.name.contains("coat", ignoreCase = true) ||
                it.name.contains("leather", ignoreCase = true)
        }
        outerwear.ifEmpty { gridProducts.take(5) }
    }
    val budget999 = remember(homeFeed.budget999, gridProducts) {
        homeFeed.budget999.ifEmpty { gridProducts.filter { it.effectivePrice <= 999.0 }.take(8) }
    }
    val budget1999 = remember(homeFeed.budget1999, gridProducts) {
        homeFeed.budget1999.ifEmpty { gridProducts.filter { it.effectivePrice <= 1999.0 }.take(8) }
    }
    val editorialStories = remember(homeFeed.editorialStories, uiState.blogs) {
        homeFeed.editorialStories.ifEmpty { uiState.blogs }
    }
    val previewVideos = remember(homeFeed.previewVideos, discoveryState.videos) {
        homeFeed.previewVideos.ifEmpty { discoveryState.videos }.filter { it.hasValidStreamUrl() }
    }

    var activeCategoryKey by remember { mutableStateOf("all") }
    var activeCuratedGridTab by remember { mutableStateOf("bestsellers") }

    if ((uiState.isHomeLoading || discoveryState.isFeedLoading) && gridProducts.isEmpty() && heroSlides.isEmpty()) {
        HomeFeedSkeleton(modifier = modifier.fillMaxSize())
        return
    }

    if (uiState.homeError != null && gridProducts.isEmpty() && heroSlides.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MelvoreErrorState(message = uiState.homeError, onRetry = onRetry)
                OutlinedButton(
                    onClick = onOpenConnectionDiagnostics,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("home_error_connection_diagnostics_button")
                ) {
                    Text(text = "Connection diagnostics")
                }
            }
        }
        return
    }

    val routeFromLink: (String) -> Unit = remember(onNavigateShop, onNavigateBrand, onNavigateStyleWorld, onNavigateCategories, onNavigateElite) {
        { rawLink ->
            val link = rawLink.trim()
            val lower = link.lowercase()
            when {
                lower.contains("elite") || lower.contains("luxe") -> onNavigateElite()
                lower.contains("categories") -> onNavigateCategories()
                lower.contains("style=") -> onNavigateStyleWorld(0, link.substringAfter("style=").substringBefore("&"))
                else -> {
                    val qp = link.substringAfter("?", "")
                    val params = qp.split("&").mapNotNull { kv ->
                        val parts = kv.split("=", limit = 2)
                        if (parts.size == 2) parts[0].lowercase() to parts[1] else null
                    }.toMap()
                    val bid = params["brand_id"]?.toIntOrNull() ?: 0
                    if (bid > 0) {
                        onNavigateBrand(bid, "")
                    } else {
                        onNavigateShop(
                            params["category"].orEmpty().ifBlank { "all" },
                            params["collection"].orEmpty(),
                            params["search"] ?: params["q"].orEmpty()
                        )
                    }
                }
            }
        }
    }

    val manifest = LocalMelvoreDesignManifest.current
    val designKit = LocalDesignKit.current
    val worldKit = LocalWorld.current
    val worldTheme = LocalFashionWorldTheme.current
    val homeOrder = remember(manifest) {
        RemoteDesignRegistry.resolveOrderedHomeSectionIds(manifest)
    }
    val horizontalPad = manifest.tokens.spacing.screenHorizontalDp.coerceIn(10, 24).dp
    val sectionGap = manifest.tokens.spacing.sectionGapDp.coerceIn(14, 36).dp
    val activeOccasion = remember(
        discoveryState.selectedHomeOccasionSlug,
        discoveryState.availableOccasions,
        discoveryState.bootstrapConfig.activeOccasion,
        manifest
    ) {
        val selectedSlug = discoveryState.selectedHomeOccasionSlug
        if (!selectedSlug.isNullOrBlank()) {
            discoveryState.availableOccasions.find { it.slug.equals(selectedSlug, ignoreCase = true) }
                ?: discoveryState.bootstrapConfig.activeOccasion?.takeIf { it.slug.equals(selectedSlug, ignoreCase = true) }
                ?: manifest.occasionOverrides.occasions[selectedSlug]?.let { kit ->
                    val label = selectedSlug.replace("-", " ").replace("_", " ").replaceFirstChar { it.uppercase() }
                    OccasionTheme(
                        slug = selectedSlug,
                        name = label,
                        headline = label,
                        primaryColor = kit.colors.primary.ifBlank { "#D4AF37" },
                        accentColor = kit.colors.accent.ifBlank { "#B45309" },
                        backgroundColor = kit.colors.background.ifBlank { "#FAF5EB" },
                        motifKey = kit.motif
                    )
                }
        } else {
            discoveryState.bootstrapConfig.activeOccasion
        }
    }

    // Build scrollable occasion / collection tabs from backend data
    val homeTabs = remember(discoveryState.availableOccasions, manifest.occasionOverrides.occasions, uiState.collections) {
        val list = mutableListOf(HomeOccasionTabItem(slug = null, label = "All"))
        val seen = mutableSetOf<String>()
        discoveryState.availableOccasions.forEach { occ ->
            val key = occ.slug.lowercase()
            if (key.isNotBlank() && seen.add(key)) {
                list += HomeOccasionTabItem(
                    slug = occ.slug,
                    label = occ.name.ifBlank { occ.slug.replaceFirstChar { it.uppercase() } },
                    accentHex = occ.accentColor
                )
            }
        }
        manifest.occasionOverrides.occasions.forEach { (slug, kit) ->
            val key = slug.lowercase()
            if (key.isNotBlank() && seen.add(key)) {
                list += HomeOccasionTabItem(
                    slug = slug,
                    label = slug.replace("-", " ").replace("_", " ").replaceFirstChar { it.uppercase() },
                    accentHex = kit.colors.accent
                )
            }
        }
        if (list.size == 1) {
            uiState.collections.take(5).forEach { col ->
                val key = col.slug.lowercase()
                if (key.isNotBlank() && seen.add(key)) {
                    list += HomeOccasionTabItem(
                        slug = col.slug,
                        label = col.name
                    )
                }
            }
        }
        list
    }

    // Smooth 300ms animated top backdrop gradient reacting to occasion tab / world selection
    val gradientAnimDuration = manifest.tokens.motion.durationMs.coerceIn(180, 450)
    val targetTopTint = parseThemeHexColor(
        activeOccasion?.accentColor?.takeIf { it.isNotBlank() }
            ?: worldTheme.accentHex,
        MaterialTheme.colorScheme.primary
    )
    val animatedTopTint by animateColorAsState(
        targetValue = targetTopTint,
        animationSpec = tween(gradientAnimDuration),
        label = "homeTopGradientTint"
    )
    val activeWorldDisplayName = remember(discoveryState.preferredWorldSlug, discoveryState.bootstrapConfig.fashionWorlds, worldTheme) {
        val slug = discoveryState.preferredWorldSlug ?: worldTheme.worldSlug
        discoveryState.bootstrapConfig.fashionWorlds
            .find { it.slug.equals(slug, ignoreCase = true) }
            ?.name
            ?: worldTheme.worldName.takeIf { it.isNotBlank() && worldTheme.isWorldActive }
            ?: "Worlds"
    }
    val homeLayoutBackdropUrl = remember(worldKit, designKit) {
        designKit.url(worldKit?.layoutPath("home") ?: worldKit?.widgetPath("bg_ambient"))
    }

    PullToRefreshBox(
        isRefreshing = uiState.isHomeLoading || discoveryState.isFeedLoading,
        onRefresh = onPullToRefresh,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Optional v3 animated SVG world backdrop behind home content
        if (!homeLayoutBackdropUrl.isNullOrBlank()) {
            AnimatedSvg(
                url = homeLayoutBackdropUrl,
                contentDescription = null,
                reducedMotion = reducedMotion,
                modifier = Modifier.fillMaxSize()
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen"),
            contentPadding = PaddingValues(top = 0.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(sectionGap)
        ) {
            // Quick-commerce Sticky Top Zone: ETA + Pincode + World Picker + Search + Occasion Tabs + Festival Hero Banner
            item(key = "home_qc_header_zone") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    animatedTopTint.copy(alpha = 0.22f),
                                    animatedTopTint.copy(alpha = 0.08f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickCommerceStickyHeader(
                        savedPincode = discoveryState.savedPincode,
                        savedCity = discoveryState.savedCity,
                        deliveryCheck = discoveryState.deliveryCheck,
                        freeShippingMinimum = freeShipMin,
                        activeWorldLabel = activeWorldDisplayName,
                        activeWorldAccentHex = worldTheme.accentHex,
                        wishlistCount = uiState.wishlistIds.size,
                        isLoggedIn = uiState.user != null,
                        trendingTags = uiState.trendingTags,
                        onPincodeClick = onPincodeClick,
                        onWorldPickerClick = onOpenWorldPicker,
                        onWishlistClick = onWishlistClick,
                        onProfileClick = onProfileClick,
                        onSearchClick = { onOpenSearch(false) }
                    )

                    if (homeTabs.size > 1) {
                        QuickCommerceOccasionTabStrip(
                            tabs = homeTabs,
                            selectedSlug = discoveryState.selectedHomeOccasionSlug,
                            onSelectTab = onSelectHomeOccasionTab
                        )
                    }

                    val leadHero = heroSlides.firstOrNull()
                    if (activeOccasion != null || heroSlides.isNotEmpty() || promoBanners.isNotEmpty()) {
                        FrontSlider(
                            activeOccasion = activeOccasion,
                            heroSlides = heroSlides,
                            promoBanners = promoBanners,
                            reducedMotion = reducedMotion,
                            onSlideClick = { link ->
                                if (link.isNotBlank()) routeFromLink(link)
                                else onNavigateShop("all", "", "")
                            }
                        )
                    } else {
                        QuickCommerceFestivalHeroBanner(
                            occasion = activeOccasion,
                            fallbackTitle = leadHero?.title?.ifBlank { "Fresh Drops in Your World" } ?: "Fresh Drops in Your World",
                            fallbackSubtitle = leadHero?.subtitle?.ifBlank { "Curated silhouettes & quick dispatch from Melvore ateliers" }
                                ?: "Curated silhouettes & quick dispatch from Melvore ateliers",
                            fallbackCta = leadHero?.ctaText?.ifBlank { "Shop Now" } ?: "Shop Now",
                            fallbackImageUrl = leadHero?.imageUrl,
                            reducedMotion = reducedMotion,
                            onCtaClick = {
                                val occSlug = activeOccasion?.ctaCollectionSlug?.ifBlank { activeOccasion.slug }
                                when {
                                    !occSlug.isNullOrBlank() -> onNavigateShop("all", occSlug, "")
                                    !leadHero?.ctaLink.isNullOrBlank() -> routeFromLink(leadHero!!.ctaLink)
                                    else -> onNavigateShop("all", "", "")
                                }
                            }
                        )
                    }

                    val isNonRemoteDesign = discoveryState.manifestSource != com.example.model.ManifestSource.REMOTE
                    val hasFallbackNotice = !discoveryState.manifestFallbackReason.isNullOrBlank() ||
                        discoveryState.manifestWarnings.isNotEmpty() ||
                        isNonRemoteDesign
                    if (BuildConfig.DEBUG || hasFallbackNotice) {
                        val surfaces = LocalMelvoreSurfaces.current
                        val sourceLabel = discoveryState.manifestSource.wireName
                        val etagShort = discoveryState.manifestEtag.trim('"').take(8).ifBlank { "none" }
                        val warnCount = discoveryState.manifestWarnings.size
                        val statusText = buildString {
                            append("DESIGN ")
                            append(sourceLabel.uppercase())
                            append(" · v")
                            append(discoveryState.manifestVersion.ifBlank { manifest.designVersion })
                            if (BuildConfig.DEBUG) {
                                append(" · etag:")
                                append(etagShort)
                            }
                            if (warnCount > 0) append(" · $warnCount warn")
                            if (!discoveryState.manifestFallbackReason.isNullOrBlank()) {
                                append(" · ")
                                append(discoveryState.manifestFallbackReason.take(36))
                            } else if (isNonRemoteDesign) {
                                append(" · Tap for Connection Diagnostics")
                            }
                        }
                        val hasFallback = !discoveryState.manifestFallbackReason.isNullOrBlank() ||
                            discoveryState.manifestSource == com.example.model.ManifestSource.BUNDLED
                        Surface(
                            onClick = if (isNonRemoteDesign || !BuildConfig.DEBUG) onOpenConnectionDiagnostics else onOpenDesignAudit,
                            shape = RoundedCornerShape(999.dp),
                            color = surfaces.surfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (hasFallback || warnCount > 0) surfaces.sale.copy(alpha = 0.6f)
                                else surfaces.border
                            ),
                            modifier = Modifier
                                .padding(horizontal = horizontalPad)
                                .heightIn(min = 32.dp)
                                .testTag("home_debug_manifest_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                hasFallback -> surfaces.sale
                                                warnCount > 0 || isNonRemoteDesign -> surfaces.accent
                                                else -> surfaces.success
                                            }
                                        )
                                )
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = surfaces.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isShowingCachedData) {
                item(key = "home_offline_cache_banner") {
                    OfflineCacheBanner(
                        cachedItemCount = uiState.cachedProductCount.takeIf { it > 0 } ?: gridProducts.size,
                        onRetry = onRetry,
                        modifier = Modifier.padding(horizontal = horizontalPad)
                    )
                }
            }

            if (BuildConfig.DEBUG && homeFeed.diagnostics.hasWarnings) {
                item(key = "home_debug_diagnostics") {
                    DebugFeedDiagnosticsBanner(
                        diagnostics = homeFeed.diagnostics,
                        modifier = Modifier.padding(horizontal = horizontalPad)
                    )
                }
            }

            homeOrder.forEach { secId ->
                when (secId) {
                    "search_bar" -> {
                        // Rendered inside the sticky QuickCommerceStickyHeader zone above
                    }

                    "category_strip" -> {
                        item(key = "home_qc_category_grid") {
                            QuickCommerceCategoryTileGrid(
                                categories = categories,
                                collections = uiState.collections,
                                onCategoryClick = { cat ->
                                    activeCategoryKey = cat.slug
                                    onNavigateShop(cat.slug, "", "")
                                },
                                onCollectionClick = { col ->
                                    onNavigateShop("all", col.slug, "")
                                },
                                onSeeAllClick = onNavigateCategories
                            )
                        }
                        item(key = "home_qc_offer_strip") {
                            QuickCommerceOfferStrip(
                                smartOffers = discoveryState.bootstrapConfig.smartOffers,
                                marqueeItems = uiState.design.marqueeItems,
                                freeShippingMinimum = freeShipMin,
                                currency = uiState.currency,
                                onApplyOffer = onApplySmartOffer
                            )
                        }
                    }

                    "occasion_banner" -> {
                        // Rendered inside the animated QuickCommerceFestivalHeroBanner above
                    }

                "hero_carousel" -> {
                    if (heroSlides.isNotEmpty()) {
                        item(key = "home_hero_slider") {
                            EditorialHeroSlider(
                                slides = heroSlides,
                                animationsEnabled = animationsEnabled,
                                onSlideCta = { slide -> routeFromLink(slide.ctaLink) }
                            )
                        }
                    }
                    if (promoBanners.isNotEmpty() || freeShipMin > 0.0) {
                        item(key = "home_secondary_promo") {
                            SecondaryPromoBannerStrip(
                                promoBanners = promoBanners,
                                freeShippingMinimum = freeShipMin,
                                currency = uiState.currency,
                                onBannerClick = { banner ->
                                    when {
                                        banner.ctaLink.isNotBlank() -> routeFromLink(banner.ctaLink)
                                        banner.filterTag.isNotBlank() -> onNavigateShop("all", banner.filterTag, "")
                                        else -> onNavigateShop("all", "", "")
                                    }
                                },
                                onOpenShopAll = { onNavigateShop("all", "", "") }
                            )
                        }
                    }
                    if (campaign != null) {
                        item(key = "home_active_campaign") {
                            MelvoreAnimatedEntrance(delayMs = 40) {
                                ActiveCampaignSpotlightCard(
                                    campaign = campaign,
                                    onExplore = {
                                        if (campaign.ctaLink.isNotBlank()) routeFromLink(campaign.ctaLink)
                                        else onNavigateShop("all", "", campaign.festivalKey)
                                    },
                                    modifier = Modifier.padding(horizontal = horizontalPad)
                                )
                            }
                        }
                    }
                    item(key = "home_editorial_campaigns") {
                        CuratedEditorialCampaignShowcase(
                            onNavigateShop = onNavigateShop,
                            onNavigateStyleWorld = onNavigateStyleWorld
                        )
                    }
                    if (promoBanners.size >= 2) {
                        item(key = "home_asymmetrical_campaign") {
                            AsymmetricalCampaignGrid(
                                banners = promoBanners,
                                spotlightProduct = gridProducts.firstOrNull(),
                                currency = uiState.currency,
                                onBannerClick = { banner ->
                                    if (banner.ctaLink.isNotBlank()) routeFromLink(banner.ctaLink)
                                    else onNavigateShop("all", banner.filterTag, "")
                                },
                                onProductClick = onOpenProduct,
                                modifier = Modifier.padding(horizontal = horizontalPad)
                            )
                        }
                    }
                }

                "style_worlds" -> {
                    item(key = "home_style_worlds") {
                        StyleWorldsShowcaseSection(
                            dbStyleWorlds = homeFeed.styleWorlds.ifEmpty { discoveryState.styleWorlds },
                            onOpenStyleWorld = onNavigateStyleWorld,
                            onDiscoverAll = onNavigateDiscover
                        )
                    }
                    val forYou = discoveryState.forYou
                    if (forYou.products.isNotEmpty()) {
                        item(key = "home_for_you") {
                            HomeForYouEditorialSection(
                                forYou = forYou,
                                currency = uiState.currency,
                                wishlistIds = uiState.wishlistIds,
                                onDiscoverAll = onNavigateDiscover,
                                onOpenBrand = onNavigateBrand,
                                onOpenStyleWorld = onNavigateStyleWorld,
                                onOpenProduct = onOpenProduct,
                                onQuickAdd = onQuickAdd,
                                onToggleWishlist = onToggleWishlist
                            )
                        }
                    }
                }

                "trending_rail" -> {
                    // Quick-Commerce horizontal rails with instant ADD -> stepper controls
                    if (bestsellers.isNotEmpty()) {
                        item(key = "home_qc_frequently_bought") {
                            QuickCommerceProductRail(
                                title = "Frequently bought",
                                subtitle = "Most ordered Melvore essentials",
                                products = bestsellers,
                                currency = uiState.currency,
                                cartQuantityForProduct = { uiState.cartQuantityForProduct(it) },
                                onProductClick = { pid ->
                                    bestsellers.find { it.id == pid }?.let(onOpenProduct)
                                },
                                onIncrementCart = onIncrementCart,
                                onDecrementCart = onDecrementCart,
                                onSeeAllClick = { onNavigateShop("all", "bestsellers", "") }
                            )
                        }
                    }
                    if (trendingProducts.isNotEmpty()) {
                        item(key = "home_qc_trending_world") {
                            QuickCommerceProductRail(
                                title = if (activeWorldDisplayName != "Worlds") {
                                    "Trending in $activeWorldDisplayName"
                                } else {
                                    "Trending in your world"
                                },
                                subtitle = "Picked from live drops & community saves",
                                products = trendingProducts,
                                currency = uiState.currency,
                                cartQuantityForProduct = { uiState.cartQuantityForProduct(it) },
                                onProductClick = { pid ->
                                    trendingProducts.find { it.id == pid }?.let(onOpenProduct)
                                },
                                onIncrementCart = onIncrementCart,
                                onDecrementCart = onDecrementCart,
                                onSeeAllClick = { onNavigateShop("all", "trending", "") }
                            )
                        }
                    }
                    if (newArrivals.isNotEmpty()) {
                        item(key = "home_qc_new_arrivals") {
                            QuickCommerceProductRail(
                                title = "New Arrivals",
                                subtitle = "Just landed on Melvore",
                                products = newArrivals,
                                currency = uiState.currency,
                                cartQuantityForProduct = { uiState.cartQuantityForProduct(it) },
                                onProductClick = { pid ->
                                    newArrivals.find { it.id == pid }?.let(onOpenProduct)
                                },
                                onIncrementCart = onIncrementCart,
                                onDecrementCart = onDecrementCart,
                                onSeeAllClick = { onNavigateShop("all", "new-arrivals", "") }
                            )
                        }
                    }
                    if (homeFeed.recentlyViewed.isNotEmpty()) {
                        item(key = "home_recently_viewed") {
                            ProductMerchandisingShelf(
                                eyebrow = "CONTINUE BROWSING",
                                title = "Recently Viewed",
                                products = homeFeed.recentlyViewed,
                                currency = uiState.currency,
                                wishlistIds = uiState.wishlistIds,
                                actionLabel = "Catalog →",
                                onAction = { onNavigateShop("all", "", "") },
                                onOpenProduct = onOpenProduct,
                                onQuickAdd = onQuickAdd,
                                onToggleWishlist = onToggleWishlist,
                                testTagPrefix = "home_recent"
                            )
                        }
                    }
                    if (signaturePieces.isNotEmpty()) {
                        item(key = "home_signature_outerwear") {
                            SignatureOuterwearShowcase(
                                heroProduct = signaturePieces.first(),
                                companionProducts = signaturePieces.drop(1).take(6),
                                currency = uiState.currency,
                                wishlistIds = uiState.wishlistIds,
                                onOpenShopOuterwear = { onNavigateShop("jackets", "", "") },
                                onOpenProduct = onOpenProduct,
                                onQuickAdd = onQuickAdd,
                                onToggleWishlist = onToggleWishlist
                            )
                        }
                    }
                    if (bestsellers.isNotEmpty() || mostWishlisted.isNotEmpty()) {
                        item(key = "home_bestsellers_header") {
                            Column(
                                modifier = Modifier.padding(horizontal = horizontalPad),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                EditorialSectionHeader(
                                    eyebrow = "COMMUNITY FAVOURITES",
                                    title = if (activeCuratedGridTab == "bestsellers") "Bestsellers" else "Most Wishlisted",
                                    actionLabel = "Shop All →",
                                    onAction = {
                                        onNavigateShop("all", activeCuratedGridTab, "")
                                    }
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        "bestsellers" to "Bestsellers",
                                        "most-wishlisted" to "Top Rated & Saved"
                                    ).forEach { (tabKey, tabLabel) ->
                                        val selected = activeCuratedGridTab == tabKey
                                        Surface(
                                            onClick = { activeCuratedGridTab = tabKey },
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(0.75.dp, if (selected) MaterialTheme.colorScheme.primary else ext.borderSubtle),
                                            modifier = Modifier.heightIn(min = 38.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                            ) {
                                                Text(
                                                    text = tabLabel,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        val gridList = if (activeCuratedGridTab == "bestsellers") {
                            bestsellers.ifEmpty { mostWishlisted }
                        } else {
                            mostWishlisted.ifEmpty { bestsellers }
                        }
                        val bestsellerRows = gridList.take(4).chunked(2)
                        items(bestsellerRows.size, key = { "bestseller_row_${activeCuratedGridTab}_$it" }) { idx ->
                            val pair = bestsellerRows[idx]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = horizontalPad),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { prod ->
                                    ProductCard(
                                        product = prod,
                                        currency = uiState.currency,
                                        isWishlisted = uiState.wishlistIds.contains(prod.id),
                                        onProductClick = { onOpenProduct(prod) },
                                        onWishlistClick = { onToggleWishlist(prod.id) },
                                        onAddToCartClick = { onQuickAdd(prod) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    if (trendingProducts.isNotEmpty()) {
                        item(key = "home_trending_lookbook") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                EditorialSectionHeader(
                                    eyebrow = "ON RADAR",
                                    title = "Trending Lookbook",
                                    actionLabel = "All Trending →",
                                    onAction = { onNavigateShop("all", "trending", "") },
                                    modifier = Modifier.padding(horizontal = horizontalPad)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = horizontalPad),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.testTag("home_trending_rail")
                                ) {
                                    items(trendingProducts.size, key = { "tr_look_${trendingProducts[it].id}" }) { idx ->
                                        val prod = trendingProducts[idx]
                                        RunwayLookbookCard(
                                            product = prod,
                                            currency = uiState.currency,
                                            isFeaturedCenter = idx == 0,
                                            animationsEnabled = animationsEnabled,
                                            onOpenProduct = { onOpenProduct(prod) },
                                            onQuickAdd = { onQuickAdd(prod) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (budget999.isNotEmpty() || budget1999.isNotEmpty()) {
                        item(key = "home_budget_edits") {
                            BudgetEditsSection(
                                budget999 = budget999,
                                budget1999 = budget1999,
                                currency = uiState.currency,
                                wishlistIds = uiState.wishlistIds,
                                onOpenBudget = { slug -> onNavigateShop("all", slug, "") },
                                onOpenProduct = onOpenProduct,
                                onQuickAdd = onQuickAdd,
                                onToggleWishlist = onToggleWishlist
                            )
                        }
                    }
                    sections.forEach { sec ->
                        if (sec.items.isNotEmpty() || sec.products.isNotEmpty()) {
                            item(key = "home_sec_${sec.sectionId}_${sec.sectionKey}") {
                                DynamicHomeSectionBlock(
                                    section = sec,
                                    currency = uiState.currency,
                                    wishlistIds = uiState.wishlistIds,
                                    onOpenProduct = onOpenProduct,
                                    onQuickAdd = onQuickAdd,
                                    onToggleWishlist = onToggleWishlist,
                                    onItemClick = { item ->
                                        when (item.targetType.lowercase()) {
                                            "brand" -> onNavigateBrand(item.targetId ?: 0, item.targetValue)
                                            "style", "style_world" -> onNavigateStyleWorld(item.targetId ?: 0, item.targetValue)
                                            "collection" -> onNavigateShop("all", item.targetValue, "")
                                            "product" -> item.targetId?.let { id ->
                                                gridProducts.find { it.id == id }?.let(onOpenProduct)
                                            }
                                            else -> onNavigateShop(item.targetValue.ifBlank { "all" }, "", "")
                                        }
                                    },
                                    onViewAll = {
                                        if (sec.ctaLink.isNotBlank()) routeFromLink(sec.ctaLink)
                                        else onNavigateShop("all", "", "")
                                    }
                                )
                            }
                        }
                    }
                }

                "world_atelier_map" -> {
                    if (mergedBrands.isNotEmpty()) {
                        item(key = "home_brand_discovery") {
                            HomeBrandDiscoverySection(
                                brands = mergedBrands,
                                onSelectBrand = { b -> onNavigateBrand(b.brandId, b.slug) },
                                onDiscoverAll = onNavigateDiscover
                            )
                        }
                    }
                    val verifiedMapOrigins = discoveryState.worldMapOrigins.filter { it.isVerified }
                    if (verifiedMapOrigins.isNotEmpty()) {
                        item(key = "home_world_map_discovery") {
                            WorldMapDiscoverySection(
                                origins = verifiedMapOrigins,
                                worlds = discoveryState.bootstrapConfig.fashionWorlds,
                                onExploreBrand = onNavigateBrand,
                                onShopBrand = onNavigateShopBrand
                            )
                        }
                    }
                    if (homeFeed.sponsoredProducts.isNotEmpty()) {
                        item(key = "home_sponsored_picks") {
                            ProductMerchandisingShelf(
                                eyebrow = "SPONSORED",
                                title = "Partner House Highlights",
                                products = homeFeed.sponsoredProducts,
                                currency = uiState.currency,
                                wishlistIds = uiState.wishlistIds,
                                actionLabel = "Explore →",
                                onAction = { onNavigateShop("all", "sponsored", "") },
                                onOpenProduct = onOpenProduct,
                                onQuickAdd = onQuickAdd,
                                onToggleWishlist = onToggleWishlist,
                                testTagPrefix = "home_sponsored"
                            )
                        }
                    }
                }

                "elite_teaser" -> {
                    if (editorialStories.isNotEmpty()) {
                        item(key = "home_editorial_journal") {
                            HomeEditorialJournalStrip(
                                stories = editorialStories,
                                onOpenJournal = onNavigateBlog
                            )
                        }
                    }
                    if (previewVideos.isNotEmpty()) {
                        item(key = "home_video_discovery") {
                            HomeVideoDiscoveryStrip(
                                videos = previewVideos,
                                onOpenVideos = onNavigateVideos
                            )
                        }
                    }
                    item(key = "home_enter_luxe_spotlight") {
                        EnterLuxeSpotlightCard(
                            onEnterLuxe = onNavigateElite,
                            modifier = Modifier.padding(horizontal = horizontalPad)
                        )
                    }
                }

                "curated_grid" -> {
                    if (gridProducts.isNotEmpty()) {
                        item(key = "home_catalog_grid_header") {
                            EditorialSectionHeader(
                                eyebrow = "FULL ARCHIVE",
                                title = "Curated Drops For You",
                                actionLabel = "Full Catalog →",
                                onAction = { onNavigateShop("all", "", "") },
                                modifier = Modifier.padding(horizontal = horizontalPad)
                            )
                        }
                        val rows = gridProducts.chunked(2)
                        items(rows.size, key = { "home_grid_row_$it" }) { rowIndex ->
                            val pair = rows[rowIndex]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = horizontalPad),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { prod ->
                                    ProductCard(
                                        product = prod,
                                        currency = uiState.currency,
                                        isWishlisted = uiState.wishlistIds.contains(prod.id),
                                        onProductClick = { onOpenProduct(prod) },
                                        onWishlistClick = { onToggleWishlist(prod.id) },
                                        onAddToCartClick = { onQuickAdd(prod) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        if (discoveryState.canLoadMoreFeed) {
                            item(key = "home_load_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = horizontalPad),
                                    contentAlignment = Alignment.Center
                                ) {
                                    OutlinedButton(
                                        onClick = onLoadMoreProducts,
                                        enabled = !discoveryState.isLoadingMoreFeed,
                                        shape = RemoteDesignRegistry.resolveButtonShape(manifest),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("home_load_more_button")
                                    ) {
                                        if (discoveryState.isLoadingMoreFeed) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Text("Load More Pieces")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun SearchTriggerBar(
    animationsEnabled: Boolean,
    onOpenSearch: () -> Unit,
    onVoiceSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val manifest = LocalMelvoreDesignManifest.current
    val worldTheme = com.example.ui.theme.LocalFashionWorldTheme.current
    val worldSearchSpec = remember(manifest, worldTheme) {
        RemoteDesignRegistry.activeWorldKit(manifest, worldTheme)?.components?.searchBar
    }
    val searchSpec = worldSearchSpec ?: manifest.components.searchBar
    val isUnderlinedVariant = searchSpec.variant.equals("underlined_editorial", ignoreCase = true)
    val hints = remember(searchSpec.placeholder) {
        buildList {
            if (searchSpec.placeholder.isNotBlank()) add(searchSpec.placeholder)
            add("Search \"Leather Jackets\"")
            add("Search \"Oversized T-Shirts\"")
            add("Search \"Korean Fashion\"")
            add("Search \"Co-Ord Sets\"")
            add("Search \"Streetwear\"")
        }.distinct()
    }
    var hintIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(animationsEnabled, hints) {
        if (animationsEnabled && hints.size > 1) {
            while (true) {
                delay(2800)
                hintIndex = (hintIndex + 1) % hints.size
            }
        }
    }

    val radiusDp = if (isUnderlinedVariant) searchSpec.cornerRadiusDp.coerceIn(4, 18) else searchSpec.cornerRadiusDp.coerceIn(4, 999)
    val barHeight = searchSpec.heightDp.coerceIn(48, 60).dp

    Surface(
        onClick = onOpenSearch,
        shape = RoundedCornerShape(radiusDp.dp),
        color = if (isUnderlinedVariant) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.borderSubtle),
        shadowElevation = if (isUnderlinedVariant) 0.dp else 3.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = barHeight)
            .testTag("home_search_trigger")
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = RemoteDesignRegistry.iconForKey(searchSpec.leadingIconKey.ifBlank { "search" }),
                contentDescription = stringResource(R.string.cd_search),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                if (animationsEnabled) {
                    AnimatedContent(
                        targetState = hints[hintIndex % hints.size],
                        transitionSpec = {
                            (slideInVertically(tween(260)) { it / 2 } + fadeIn(tween(260))) togetherWith
                                (slideOutVertically(tween(220)) { -it / 2 } + fadeOut(tween(180)))
                        },
                        label = "search_hint_ticker"
                    ) { currentHint ->
                        Text(
                            text = currentHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = hints.firstOrNull() ?: "Search Melvore…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (searchSpec.showVisualHintIcon) {
                Surface(
                    onClick = onVoiceSearch,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(38.dp)
                        .testTag("home_voice_search_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = stringResource(R.string.cd_voice_search),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveCampaignSpotlightCard(
    campaign: CampaignItem,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaces = LocalMelvoreSurfaces.current
    val manifest = LocalMelvoreDesignManifest.current
    val rawBg = Color(parseHexColor(campaign.backgroundColor))
    val primary = Color(parseHexColor(campaign.primaryColor))
    val rawAccent = Color(parseHexColor(campaign.accentColor))
    val bg = if (!surfaces.isDark && rawBg.luminance() < 0.20f) surfaces.surfaceElevated else rawBg
    val hasPhoto = !(campaign.mobileBannerUrl ?: campaign.bannerImageUrl).isNullOrBlank()
    val titleColor = if (hasPhoto) surfaces.onImageOverlay else ensureContrast(rawAccent, bg)
    val eyebrowColor = if (hasPhoto) ensureContrast(primary, surfaces.imageOverlay) else ensureContrast(primary, bg)
    val ctaContent = ensureContrast(surfaces.onAccent, primary)
    val animKey = MelvoreAnimationKey.fromBackendKey(campaign.animationKey)

    CampaignAtmosphericEffect(
        animationKey = animKey,
        accentColor = primary,
        durationMs = campaign.animationDurationMs,
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            onClick = onExplore,
            shape = RoundedCornerShape(manifest.tokens.radii.cardDp.coerceIn(4, 18).dp),
            color = bg,
            border = BorderStroke(0.75.dp, primary.copy(alpha = 0.38f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                val imgUrl = campaign.mobileBannerUrl ?: campaign.bannerImageUrl
                if (!imgUrl.isNullOrBlank()) {
                    MelvoreNetworkImage(
                        url = imgUrl,
                        contentDescription = campaign.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(184.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        surfaces.imageOverlay.copy(alpha = 0.88f),
                                        surfaces.imageOverlay.copy(alpha = 0.34f)
                                    )
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = campaign.festivalKey.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Bold,
                        color = eyebrowColor
                    )
                    if (campaign.title.isNotBlank()) {
                        Text(
                            text = campaign.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = MelvoreBrandFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = titleColor
                        )
                    }
                    if (campaign.subtitle.isNotBlank()) {
                        Text(
                            text = campaign.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = titleColor.copy(alpha = 0.86f),
                            maxLines = 2
                        )
                    }
                    if (campaign.ctaText.isNotBlank()) {
                        MetallicShineButton(
                            onClick = onExplore,
                            containerColor = primary,
                            contentColor = ctaContent
                        ) {
                            Text(campaign.ctaText.uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicHomeSectionBlock(
    section: HomeSection,
    currency: String,
    wishlistIds: List<Int>,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    onItemClick: (HomeSectionItem) -> Unit,
    onViewAll: () -> Unit
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (section.title.isNotBlank()) {
            EditorialSectionHeader(
                eyebrow = section.eyebrow.ifBlank { section.sectionKey.replace("_", " ").uppercase() },
                title = section.title,
                actionLabel = section.ctaText.ifBlank { "Explore →" },
                onAction = onViewAll,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }

        if (section.items.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(section.items, key = { "sec_item_${it.itemId}" }) { tile ->
                    Surface(
                        onClick = { onItemClick(tile) },
                        shape = RoundedCornerShape(4.dp),
                        color = surfaces.surface,
                        border = BorderStroke(0.5.dp, ext.borderSubtle.copy(alpha = 0.65f)),
                        modifier = Modifier.width(216.dp)
                    ) {
                        Box(modifier = Modifier.height(148.dp)) {
                            val img = tile.mobileImageUrl ?: tile.imageUrl
                            MelvoreNetworkImage(
                                url = img,
                                contentDescription = tile.title,
                                fallbackDrawable = R.drawable.img_hero_look,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, surfaces.imageOverlay.copy(alpha = 0.85f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (tile.badgeText.isNotBlank()) {
                                    Text(
                                        text = tile.badgeText.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ensureContrast(ext.goldAccent, surfaces.imageOverlay),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = tile.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = surfaces.onImageOverlay,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (tile.subtitle.isNotBlank()) {
                                    Text(
                                        text = tile.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = surfaces.onImageOverlay.copy(alpha = 0.84f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (section.products.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(section.products, key = { "sec_${section.sectionId}_p_${it.id}" }) { prod ->
                    ProductCard(
                        product = prod,
                        currency = currency,
                        isWishlisted = wishlistIds.contains(prod.id),
                        onProductClick = { onOpenProduct(prod) },
                        onWishlistClick = { onToggleWishlist(prod.id) },
                        onAddToCartClick = { onQuickAdd(prod) },
                        modifier = Modifier.width(172.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveOccasionSpotlightCard(
    occasion: OccasionTheme,
    assets: List<WorldVectorAsset>,
    reducedMotion: Boolean,
    onExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val manifest = LocalMelvoreDesignManifest.current
    val occKit = remember(manifest, occasion.slug) {
        RemoteDesignRegistry.activeOccasionKit(manifest, occasion.slug)
    }
    val bg = parseThemeHexColor(
        occKit?.colors?.background?.takeIf { it.isNotBlank() } ?: occasion.backgroundColor,
        MaterialTheme.colorScheme.surfaceVariant
    )
    val accent = parseThemeHexColor(
        occKit?.colors?.accent?.takeIf { it.isNotBlank() } ?: occasion.accentColor,
        MaterialTheme.colorScheme.secondary
    )
    val primary = parseThemeHexColor(
        occKit?.colors?.ctaFace?.takeIf { it.isNotBlank() }
            ?: occKit?.colors?.primary?.takeIf { it.isNotBlank() }
            ?: occasion.primaryColor,
        MaterialTheme.colorScheme.primary
    )
    val onBg = contrastOnColor(bg)
    val bannerSvg = occKit?.svg?.banner?.takeIf { it.isNotBlank() }
    val cornerSvg = occKit?.svg?.corner?.takeIf { it.isNotBlank() }
    val dividerSvg = occKit?.svg?.divider?.takeIf { it.isNotBlank() }
    val buttonSvg = occKit?.svg?.button?.takeIf { it.isNotBlank() }
    val buttonGhostSvg = occKit?.svg?.buttonGhost?.takeIf { it.isNotBlank() }
    val overlayAlpha = (occKit?.overlayOpacity ?: manifest.media.occasionOverlayOpacity).coerceIn(0.08f, 0.35f)

    Surface(
        onClick = onExplore,
        shape = RoundedCornerShape(manifest.tokens.radii.heroDp.coerceIn(4, 20).dp),
        color = bg,
        border = BorderStroke(0.75.dp, accent.copy(alpha = 0.45f)),
        shadowElevation = manifest.occasionOverrides.maxBannerElevationDp.coerceIn(0, 6).dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_occasion_spotlight")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (bannerSvg != null) {
                RemoteSvgSkinImage(
                    relativeOrAbsoluteUrl = bannerSvg,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = (1f - overlayAlpha).coerceIn(0.55f, 0.92f),
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(bg.copy(alpha = 0.88f), bg.copy(alpha = 0.36f))
                            )
                        )
                )
            }
            WorldVectorArtLayer(
                assets = assets,
                worldSlug = occasion.slug,
                fallbackPreset = occKit?.motif?.ifBlank { occasion.motifKey } ?: occasion.motifKey,
                primaryAccent = primary,
                secondaryAccent = accent,
                reducedMotion = reducedMotion,
                modifier = Modifier.matchParentSize()
            )
            if (cornerSvg != null) {
                RemoteSvgSkinImage(
                    relativeOrAbsoluteUrl = cornerSvg,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(44.dp)
                )
            }
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = occasion.eyebrow.ifBlank { stringResource(R.string.home_occasion_badge) }.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = occasion.headline.ifBlank { occasion.name },
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = fontFamilyForStyleKey(occasion.fontStyle),
                    fontWeight = FontWeight.Bold,
                    color = onBg
                )
                if (occasion.subtitle.isNotBlank()) {
                    Text(
                        text = occasion.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = onBg.copy(alpha = 0.84f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (dividerSvg != null) {
                    RemoteSvgDivider(
                        svgPath = dividerSvg,
                        fallbackColor = accent.copy(alpha = 0.3f),
                        height = 10.dp
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onExplore,
                        shape = RemoteDesignRegistry.resolveButtonShape(manifest),
                        color = if (buttonSvg != null) Color.Transparent else primary,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("home_occasion_cta")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (buttonSvg != null) {
                                RemoteSvgSkinImage(
                                    relativeOrAbsoluteUrl = buttonSvg,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier.matchParentSize()
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = occasion.ctaText.ifBlank { stringResource(R.string.home_occasion_explore) },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = contrastOnColor(primary)
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = contrastOnColor(primary),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (buttonGhostSvg != null) {
                        Surface(
                            onClick = onExplore,
                            shape = RemoteDesignRegistry.resolveButtonShape(manifest),
                            color = Color.Transparent,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("home_occasion_ghost_cta")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                RemoteSvgSkinImage(
                                    relativeOrAbsoluteUrl = buttonGhostSvg,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillBounds,
                                    modifier = Modifier.matchParentSize()
                                )
                                Text(
                                    text = "VIEW LOOKS",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = onBg,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
