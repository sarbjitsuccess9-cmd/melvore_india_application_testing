package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Product
import com.example.model.RunwayConfig
import com.example.ui.components.CampaignAtmosphericEffect
import com.example.ui.components.GothBatSwarmOverlay
import com.example.ui.components.LiquidGlassBox
import com.example.ui.components.MelvoreAnimationKey
import com.example.ui.components.MelvoreErrorState
import com.example.ui.components.MelvoreLoadingState
import com.example.ui.components.ProductCard
import com.example.ui.components.ProductEditorialSpotlightCard
import com.example.ui.components.RemoteDesignRegistry
import com.example.ui.components.RemoteSvgDivider
import com.example.ui.components.VibeSatisfactionSection
import com.example.ui.components.WorldAmbientSvgSpritesLayer
import com.example.ui.components.WorldHeroVideoSurface
import com.example.ui.components.WorldVectorArtLayer
import com.example.ui.components.shouldShowVibeSatisfactionSection
import com.example.ui.theme.LocalFashionWorldTheme
import com.example.ui.theme.LocalMelvoreApiBaseUrl
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreDesignManifest
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.contrastOnColor
import com.example.ui.theme.fontFamilyForStyleKey
import com.example.ui.theme.parseHexColor
import com.example.ui.theme.styleWorldVisualSpec
import com.example.ui.viewmodel.DiscoveryUiState
import com.example.ui.viewmodel.MelvoreUiState

@Composable
fun StyleWorldScreen(
    styleWorldId: Int,
    slug: String,
    uiState: MelvoreUiState,
    discoveryState: DiscoveryUiState,
    onLoadStyleWorld: (Int?, String?) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    onNavigateBack: () -> Unit = {},
    onToggleSound: () -> Unit = {},
    onVideoAudioActiveChanged: (Boolean) -> Unit = {},
    onSubmitVibeScore: (styleWorldId: Int, slug: String, score: Int) -> Unit = { _, _, _ -> },
    onCopyCoupon: (String) -> Unit = {},
    onApplyCoupon: (String) -> Unit = {},
    onDismissVibeReward: () -> Unit = {},
    onOpenCollection: (String) -> Unit = {},
    onSaveScrollPosition: (slug: String, index: Int, offset: Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    val resolvedTheme = LocalFashionWorldTheme.current

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(styleWorldId, slug) {
        onLoadStyleWorld(styleWorldId.takeIf { it > 0 }, slug.takeIf { it.isNotBlank() })
    }

    DisposableEffect(Unit) {
        onDispose {
            onVideoAudioActiveChanged(false)
        }
    }

    if (discoveryState.isStyleDetailLoading && discoveryState.styleDetail == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            MelvoreLoadingState("Entering aesthetic universe…")
            ImmersiveWorldFloatingBar(
                accentColor = MaterialTheme.colorScheme.secondary,
                isDark = resolvedTheme.isDark,
                ambientAudioEnabled = uiState.preferences.ambientAudioEnabled,
                onBack = onNavigateBack,
                onToggleSound = onToggleSound,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        return
    }

    val detail = discoveryState.styleDetail
    if (detail == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            MelvoreErrorState(
                message = discoveryState.styleDetailError ?: "Unable to open this aesthetic world.",
                onRetry = { onLoadStyleWorld(styleWorldId.takeIf { it > 0 }, slug.takeIf { it.isNotBlank() }) }
            )
            ImmersiveWorldFloatingBar(
                accentColor = MaterialTheme.colorScheme.secondary,
                isDark = resolvedTheme.isDark,
                ambientAudioEnabled = uiState.preferences.ambientAudioEnabled,
                onBack = onNavigateBack,
                onToggleSound = onToggleSound,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        return
    }

    val world = detail.styleWorld
    val worldKey = world.slug.ifBlank { slug }.lowercase()
    val spec = styleWorldVisualSpec(worldKey.ifBlank { world.themeKey })
    val animationKey = MelvoreAnimationKey.fromBackendKey(world.animationKey)

    val worldBgColor = parseHexColor(
        resolvedTheme.backgroundHex.takeIf { resolvedTheme.isWorldActive } ?: world.backgroundColor,
        MaterialTheme.colorScheme.background
    )
    val worldSurfaceColor = parseHexColor(
        resolvedTheme.surfaceHex.takeIf { resolvedTheme.isWorldActive },
        MaterialTheme.colorScheme.surface
    )
    val worldAccentColor = parseHexColor(world.accentColor, spec.borderColor)
    val worldPrimaryColor = parseHexColor(world.primaryColor, MaterialTheme.colorScheme.primary)
    val worldTextPrimary = parseHexColor(
        resolvedTheme.textPrimaryHex.takeIf { resolvedTheme.isWorldActive },
        contrastOnColor(worldBgColor)
    )
    val worldTextSecondary = parseHexColor(
        resolvedTheme.textSecondaryHex.takeIf { resolvedTheme.isWorldActive },
        worldTextPrimary.copy(alpha = 0.76f)
    )
    val manifest = LocalMelvoreDesignManifest.current
    val apiBaseUrl = LocalMelvoreApiBaseUrl.current
    val worldKit = remember(manifest, worldKey) { manifest.worldOverride(worldKey)?.kit }
    val isY2k = worldKey.contains("y2k") || world.themeKey.contains("y2k", ignoreCase = true)
    val isPixel = worldKey.contains("pixel") || worldKit?.typography?.pixelText == true
    val isGoth = worldKey.contains("goth") || world.themeKey.contains("goth", ignoreCase = true)
    val displayFont = remember(world.fontStyle, isY2k, isPixel, worldKit) {
        when {
            isPixel -> fontFamilyForStyleKey("pixel")
            !worldKit?.typography?.displayFont.isNullOrBlank() -> fontFamilyForStyleKey(worldKit!!.typography!!.displayFont)
            isY2k && world.fontStyle.isBlank() -> fontFamilyForStyleKey("pixel")
            else -> fontFamilyForStyleKey(world.fontStyle)
        }
    }

    val savedPos = discoveryState.styleWorldScrollPositions[worldKey]
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = savedPos?.first ?: 0,
        initialFirstVisibleItemScrollOffset = savedPos?.second ?: 0
    )

    DisposableEffect(worldKey, listState) {
        onDispose {
            onSaveScrollPosition(
                worldKey,
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset
            )
        }
    }

    val drawables = remember {
        mapOf(
            "pixel" to R.drawable.img_hero_seoul,
            "y2k" to R.drawable.img_hero_seoul,
            "goth" to R.drawable.img_studded_jacket,
            "coquette" to R.drawable.img_white_cropped,
            "streetwear" to R.drawable.img_hybrid_jacket,
            "k-fashion" to R.drawable.img_seoul_jacket,
            "quiet-luxury" to R.drawable.img_hero_atelier,
            "minimal" to R.drawable.img_loungewear,
            "grunge" to R.drawable.img_biker_jacket,
            "old-money" to R.drawable.img_hero_look,
            "casual" to R.drawable.img_loungewear,
            "party-wear" to R.drawable.img_villain_jacket
        )
    }
    val fallbackHero = drawables[world.slug.lowercase()] ?: R.drawable.img_hero_look
    val kitHeroUrl = remember(worldKit, apiBaseUrl) {
        RemoteDesignRegistry.resolveAssetUrl(worldKit?.heroImage, apiBaseUrl)
    }

    var selectedCatSlug by remember(world.styleWorldId, world.slug) { mutableStateOf("all") }
    val filteredProducts = remember(detail.products, selectedCatSlug) {
        if (selectedCatSlug == "all") {
            detail.products
        } else {
            detail.products.filter {
                it.categorySlug.equals(selectedCatSlug, ignoreCase = true) ||
                    it.category.equals(selectedCatSlug, ignoreCase = true)
            }
        }
    }

    val promoVideoUrl = world.promoVideoUrl?.takeIf { it.isNotBlank() } ?: world.videoUrl
    val effectiveRunwayConfig: RunwayConfig? = remember(world.runwayConfig, isY2k, world.slug) {
        when {
            world.runwayConfig != null -> world.runwayConfig
            isY2k -> RunwayConfig(
                enabled = true,
                title = "CHROME PIXEL RUNWAY",
                subtitle = "2000s cyber silhouettes calibrated live from the Y2K archive",
                animationPreset = "pixel_walk_loop",
                speed = 1.0f,
                backgroundHex = "#160D2D",
                displayPosition = "after_promo",
                collectionSlug = world.slug
            )
            else -> null
        }
    }

    val showVibePrompt = shouldShowVibeSatisfactionSection(
        hasSubmittedBackendOrLocal = discoveryState.hasActiveWorldFeedbackSubmitted,
        justUnlockedResult = discoveryState.unlockedWorldFeedbackResult
    )

    val worldSectionOrder = remember(manifest, worldKey) {
        RemoteDesignRegistry.resolveStyleWorldSectionOrder(manifest, worldKey)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(worldBgColor)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("style_world_screen"),
            contentPadding = PaddingValues(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            worldSectionOrder.forEach { worldSecId ->
                when (worldSecId) {
                    "world_hero" -> {
                        item(key = "style_world_hero") {
                            CampaignAtmosphericEffect(
                                animationKey = animationKey,
                                accentColor = worldAccentColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp)
                            ) {
                                WorldHeroVideoSurface(
                                    worldName = world.name,
                                    videoUrl = promoVideoUrl,
                                    posterUrl = world.mobileImageUrl ?: world.coverImageUrl ?: world.promoVideoPosterUrl ?: kitHeroUrl,
                                    fallbackDrawable = fallbackHero,
                                    isSponsored = world.promoVideoIsSponsored,
                                    accentColor = worldAccentColor,
                                    autoplayMuted = uiState.preferences.videoAutoplayMuted,
                                    dataSaverMode = uiState.preferences.dataSaverMode,
                                    onVideoAudioActiveChanged = onVideoAudioActiveChanged,
                                    modifier = Modifier.fillMaxSize()
                                )

                                val overlayBrush = if (isY2k || isPixel) {
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0f to surfaces.imageOverlay.copy(alpha = 0.48f),
                                            0.45f to worldAccentColor.copy(alpha = 0.12f),
                                            0.78f to worldPrimaryColor.copy(alpha = 0.16f),
                                            1f to worldBgColor
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0f to surfaces.imageOverlay.copy(alpha = spec.scrimTopAlpha.coerceAtLeast(0.32f)),
                                            0.52f to Color.Transparent,
                                            1f to worldBgColor
                                        )
                                    )
                                }
                                Box(modifier = Modifier.fillMaxSize().background(overlayBrush))

                                WorldVectorArtLayer(
                                    assets = discoveryState.bootstrapConfig.worldAssets,
                                    worldSlug = world.slug,
                                    fallbackPreset = spec.vibeLabel,
                                    primaryAccent = worldAccentColor,
                                    secondaryAccent = parseHexColor(resolvedTheme.secondaryAccentHex, spec.badgeBgColor),
                                    reducedMotion = uiState.preferences.reducedMotion,
                                    modifier = Modifier.fillMaxSize()
                                )

                                WorldAmbientSvgSpritesLayer(
                                    worldKit = worldKit,
                                    reducedMotion = uiState.preferences.reducedMotion,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 18.dp, end = 118.dp, bottom = 20.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = spec.vibeLabel.ifBlank { "${world.slug.uppercase()} EDITION" },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = if (isY2k || isPixel) fontFamilyForStyleKey("pixel") else MaterialTheme.typography.labelSmall.fontFamily,
                                        letterSpacing = 1.6.sp,
                                        color = worldAccentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = world.name,
                                        style = MaterialTheme.typography.displaySmall,
                                        fontFamily = displayFont,
                                        fontWeight = FontWeight.Bold,
                                        color = worldTextPrimary
                                    )
                                    if (world.tagline.isNotBlank()) {
                                        Text(
                                            text = world.tagline,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = worldTextPrimary.copy(alpha = 0.88f),
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                        if (!worldKit?.svg?.divider.isNullOrBlank()) {
                            item(key = "style_world_hero_divider") {
                                RemoteSvgDivider(
                                    svgPath = worldKit?.svg?.divider,
                                    fallbackColor = worldAccentColor.copy(alpha = 0.35f),
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                            }
                        }
                    }

                    "vibe_satisfaction" -> {
                        if (showVibePrompt) {
                            item(key = "style_world_vibe_satisfaction") {
                                VibeSatisfactionSection(
                                    worldName = world.name,
                                    fontStyle = world.fontStyle,
                                    buttonStyle = resolvedTheme.resolvedActionButtonStyle,
                                    surfaceColor = worldSurfaceColor,
                                    accentColor = worldAccentColor,
                                    secondaryAccentColor = worldPrimaryColor,
                                    textPrimary = worldTextPrimary,
                                    textSecondary = worldTextSecondary,
                                    isSubmitting = discoveryState.isSubmittingWorldFeedback,
                                    unlockedResult = discoveryState.unlockedWorldFeedbackResult,
                                    onSubmitScore = { score ->
                                        onSubmitVibeScore(world.styleWorldId, world.slug, score)
                                    },
                                    onCopyCoupon = onCopyCoupon,
                                    onApplyCouponToBag = onApplyCoupon,
                                    onDismissReward = onDismissVibeReward,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                            }
                        }
                    }

                    "world_subcultures" -> {
                        if (detail.categories.isNotEmpty()) {
                            item(key = "style_world_filters") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        val active = selectedCatSlug == "all"
                                        Surface(
                                            onClick = { selectedCatSlug = "all" },
                                            shape = RoundedCornerShape(if (isY2k) 4.dp else 999.dp),
                                            color = if (active) worldAccentColor else worldSurfaceColor,
                                            border = BorderStroke(1.dp, if (active) worldAccentColor else worldTextSecondary.copy(alpha = 0.28f)),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("world_filter_all")
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                                                Text(
                                                    "All (${detail.products.size})",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (active) contrastOnColor(worldAccentColor) else worldTextPrimary
                                                )
                                            }
                                        }
                                    }
                                    items(detail.categories, key = { "sw_cat_${it.id}" }) { cat ->
                                        val active = selectedCatSlug == cat.slug
                                        Surface(
                                            onClick = { selectedCatSlug = cat.slug },
                                            shape = RoundedCornerShape(if (isY2k) 4.dp else 999.dp),
                                            color = if (active) worldAccentColor else worldSurfaceColor,
                                            border = BorderStroke(1.dp, if (active) worldAccentColor else worldTextSecondary.copy(alpha = 0.28f)),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("world_filter_${cat.slug}")
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                                                Text(
                                                    cat.name,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (active) contrastOnColor(worldAccentColor) else worldTextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "world_products" -> {
                        if (filteredProducts.isEmpty()) {
                            item(key = "style_world_empty") {
                                val kitEmptyTitle = worldKit?.components?.states?.emptyTitle?.takeIf { it.isNotBlank() }
                                val kitEmptySub = worldKit?.components?.states?.emptySubtitle?.takeIf { it.isNotBlank() }
                                Surface(
                                    shape = RoundedCornerShape(if (isPixel) 0.dp else 10.dp),
                                    color = worldSurfaceColor,
                                    border = BorderStroke(1.dp, worldAccentColor.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (kitEmptyTitle != null) {
                                            Text(
                                                text = kitEmptyTitle,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontFamily = displayFont,
                                                fontWeight = FontWeight.Bold,
                                                color = worldTextPrimary
                                            )
                                        }
                                        Text(
                                            text = kitEmptySub ?: "No pieces matched this filter inside ${world.name}. Select All to view the full aesthetic edit.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = worldTextSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            val spotlightPiece = if (selectedCatSlug == "all" && filteredProducts.size >= 3) filteredProducts.first() else null
                            val gridList = if (spotlightPiece != null) filteredProducts.drop(1) else filteredProducts

                            if (spotlightPiece != null) {
                                item(key = "sw_spotlight_${spotlightPiece.id}") {
                                    ProductEditorialSpotlightCard(
                                        eyebrow = "${world.name.uppercase()} SIGNATURE",
                                        product = spotlightPiece,
                                        currency = uiState.currency,
                                        isWishlisted = uiState.wishlistIds.contains(spotlightPiece.id),
                                        onOpenProduct = { onOpenProduct(spotlightPiece) },
                                        onWishlistClick = { onToggleWishlist(spotlightPiece.id) },
                                        onQuickAdd = { onQuickAdd(spotlightPiece) },
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        runwayConfig = effectiveRunwayConfig
                                    )
                                }
                            }

                            val rows = gridList.chunked(2)
                            items(rows.size, key = { "sw_row_$it" }) { rowIdx ->
                                val pair = rows[rowIdx]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp),
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
                                            modifier = Modifier.weight(1f),
                                            runwayConfig = effectiveRunwayConfig
                                        )
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item(key = "style_world_bottom_inset") {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }

        // One-shot atmospheric Bat Swarm overlay when entering Goth world (non-blocking)
        GothBatSwarmOverlay(
            entryKey = "${world.styleWorldId}_${world.slug}",
            enabled = isGoth,
            reducedMotion = uiState.preferences.reducedMotion,
            modifier = Modifier.fillMaxSize()
        )

        // Minimal, discreet immersive world floating controls (Back/Exit + Sound Mute/Unmute ONLY)
        ImmersiveWorldFloatingBar(
            accentColor = worldAccentColor,
            isDark = resolvedTheme.isDark,
            ambientAudioEnabled = uiState.preferences.ambientAudioEnabled,
            onBack = onNavigateBack,
            onToggleSound = onToggleSound,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun ImmersiveWorldFloatingBar(
    accentColor: Color,
    isDark: Boolean,
    ambientAudioEnabled: Boolean,
    onBack: () -> Unit,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaces = LocalMelvoreSurfaces.current
    val iconTint = surfaces.onSurface
    val activeSoundTint = if (ambientAudioEnabled) accentColor else iconTint.copy(alpha = 0.72f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LiquidGlassBox(
            shape = CircleShape
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("world_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_world_back),
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        LiquidGlassBox(
            shape = CircleShape
        ) {
            IconButton(
                onClick = onToggleSound,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("world_sound_toggle")
            ) {
                Icon(
                    imageVector = if (ambientAudioEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = stringResource(R.string.cd_world_sound_toggle),
                    tint = activeSoundTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
