package com.example.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BrandProfileItem
import com.example.model.ElitePack
import com.example.model.Product
import com.example.model.formatPrice
import com.example.ui.components.GoldShimmerEyebrow
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.MelvoreErrorState
import com.example.ui.components.MelvoreLoadingState
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.components.MetallicShineButton
import com.example.ui.components.ProductCard
import com.example.ui.components.ProductEditorialSpotlightCard
import com.example.ui.components.SlowEditorialZoomBox
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.MelvoreBrandFontFamily
import com.example.ui.theme.MelvoreGothicFontFamily
import com.example.ui.theme.MelvoreSerifFontFamily
import com.example.ui.theme.ensureContrast
import com.example.ui.viewmodel.DiscoveryUiState
import com.example.ui.viewmodel.MelvoreUiState

@Composable
fun EliteScreen(
    uiState: MelvoreUiState,
    discoveryState: DiscoveryUiState,
    onLoadElite: () -> Unit,
    onOpenPack: (Int, String) -> Unit,
    onOpenBrand: (Int, String) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    onOpenVideos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current

    LaunchedEffect(Unit) {
        onLoadElite()
    }

    if (discoveryState.isEliteLoading && discoveryState.eliteHome.packs.isEmpty() && discoveryState.eliteHome.heroPacks.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(surfaces.background),
            contentAlignment = Alignment.Center
        ) {
            MelvoreLoadingState("Entering Melvore Luxe Showroom…")
        }
        return
    }

    val elite = discoveryState.eliteHome
    if (discoveryState.eliteError != null && elite.packs.isEmpty() && elite.heroPacks.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(surfaces.background)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            MelvoreErrorState(message = discoveryState.eliteError, onRetry = onLoadElite)
        }
        return
    }

    val heroSlides = remember(elite.heroPacks, elite.packs) {
        elite.heroPacks.ifEmpty { elite.packs.take(3) }
    }
    val validVideos = remember(elite.eliteVideos) {
        elite.eliteVideos.filter { it.hasValidStreamUrl() }
    }

    var activeTab by remember { mutableStateOf("home") }
    val subTabs = listOf(
        "home" to "Showroom",
        "products" to "Collector Pieces",
        "brands" to "Maisons",
        "sponsored" to "Sponsored"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(surfaces.background)
            .testTag("elite_screen"),
        contentPadding = PaddingValues(bottom = 42.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp)
    ) {
        // 1. Full-Bleed First-Screen Luxury Hero Experience
        if (heroSlides.isNotEmpty()) {
            item(key = "luxe_hero_experience") {
                LuxeHeroExperience(
                    heroPacks = heroSlides,
                    onEnterCollection = { pack -> onOpenPack(pack.packId, pack.slug) }
                )
            }
        }

        // 2. Obsidian & Champagne Gold Sub-Navigation Strip
        item(key = "luxe_sub_tabs") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subTabs, key = { it.first }) { (key, label) ->
                    val selected = activeTab == key
                    Surface(
                        onClick = { activeTab = key },
                        shape = RoundedCornerShape(2.dp),
                        color = if (selected) ext.goldAccent else surfaces.surface,
                        border = BorderStroke(
                            1.dp,
                            if (selected) ext.goldAccent else surfaces.border
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("elite_tab_$key")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 18.dp)) {
                            Text(
                                text = label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.4.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) ensureContrast(surfaces.onAccent, ext.goldAccent) else surfaces.onSurface
                            )
                        }
                    }
                }
            }
        }

        when (activeTab) {
            "home" -> {
                // Varied Luxury Editorial Pack Layouts (Not identical uniform carousels)
                elite.packs.forEachIndexed { index, pack ->
                    item(key = "elite_editorial_pack_${pack.packId}_${pack.slug}_$index") {
                        when (index % 3) {
                            0 -> LuxePortraitShowcaseBlock(
                                pack = pack,
                                currency = uiState.currency,
                                onOpenPack = { onOpenPack(pack.packId, pack.slug) },
                                onOpenProduct = onOpenProduct,
                                onOpenBrand = onOpenBrand
                            )
                            1 -> LuxeDesignerSpotlightBlock(
                                pack = pack,
                                currency = uiState.currency,
                                wishlistIds = uiState.wishlistIds,
                                onOpenPack = { onOpenPack(pack.packId, pack.slug) },
                                onOpenProduct = onOpenProduct,
                                onToggleWishlist = onToggleWishlist,
                                onQuickAdd = onQuickAdd
                            )
                            else -> LuxeSquaredArchiveGridBlock(
                                pack = pack,
                                currency = uiState.currency,
                                onOpenPack = { onOpenPack(pack.packId, pack.slug) },
                                onOpenProduct = onOpenProduct,
                                onOpenBrand = onOpenBrand
                            )
                        }
                    }
                }

                // Partner Maisons Rail
                if (elite.eliteBrands.isNotEmpty()) {
                    item(key = "luxe_partner_maisons") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LuxeSectionHeader(
                                eyebrow = "PARTNER MAISONS",
                                title = "Curated Luxury Houses",
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(elite.eliteBrands, key = { "elite_br_${it.brandId}" }) { brand ->
                                    EliteBrandTile(brand = brand, onClick = { onOpenBrand(brand.brandId, brand.slug) })
                                }
                            }
                        }
                    }
                }

                // Atelier Campaign Films (9:16)
                if (validVideos.isNotEmpty()) {
                    item(key = "luxe_campaign_films") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LuxeSectionHeader(
                                eyebrow = "ATELIER CINEMA",
                                title = "Runway & House Films",
                                actionLabel = "Watch All →",
                                onAction = onOpenVideos,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(validVideos, key = { "elite_vid_${it.videoId}" }) { reel ->
                                    Surface(
                                        onClick = onOpenVideos,
                                        shape = RoundedCornerShape(4.dp),
                                        color = surfaces.surface,
                                        border = BorderStroke(1.dp, ext.goldAccent.copy(alpha = 0.32f)),
                                        modifier = Modifier.width(168.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(9f / 16f)
                                        ) {
                                            MelvoreNetworkImage(
                                                url = reel.posterUrl ?: reel.product?.imageUrl,
                                                contentDescription = reel.title,
                                                fallbackDrawable = R.drawable.img_hero_atelier,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color.Transparent, surfaces.imageOverlay.copy(alpha = 0.88f))
                                                        )
                                                    )
                                            )
                                            LiquidGlassSurface(
                                                darkGlass = true,
                                                shape = CircleShape,
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .size(46.dp)
                                            ) {
                                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = surfaces.onImageOverlay)
                                                }
                                            }
                                            Column(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                if (!reel.brandName.isNullOrBlank()) {
                                                    Text(reel.brandName.uppercase(), style = MaterialTheme.typography.labelSmall, color = ext.goldAccent)
                                                }
                                                Text(reel.title, style = MaterialTheme.typography.labelMedium, color = surfaces.onImageOverlay, maxLines = 2)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "products" -> {
                val allPackProducts = elite.packs.flatMap { p -> p.items.mapNotNull { it.product } }
                    .ifEmpty { elite.sponsoredProducts }
                    .ifEmpty { uiState.products.sortedByDescending { it.effectivePrice }.take(12) }
                    .distinctBy { it.id }

                if (allPackProducts.isEmpty()) {
                    item {
                        Text(
                            text = "No collector pieces available right now.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.muted,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(allPackProducts.chunked(2)) { pair ->
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
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            "brands" -> {
                val brandsToShow = elite.eliteBrands.ifEmpty { discoveryState.directoryBrands }
                if (brandsToShow.isEmpty()) {
                    item {
                        Text(
                            text = "Partner luxury houses are syncing with Melvore Luxe.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.muted,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(brandsToShow, key = { "elite_tab_br_${it.brandId}" }) { brand ->
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            EliteBrandWideCard(brand = brand, onClick = { onOpenBrand(brand.brandId, brand.slug) })
                        }
                    }
                }
            }

            "sponsored" -> {
                if (elite.sponsoredProducts.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = surfaces.surface,
                            border = BorderStroke(1.dp, surfaces.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "No sponsored luxury highlights are active in this cycle.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = surfaces.onSurfaceMuted,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                } else {
                    items(elite.sponsoredProducts.chunked(2)) { pair ->
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
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LuxeHeroExperience(
    heroPacks: List<ElitePack>,
    onEnterCollection: (ElitePack) -> Unit
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    val pagerState = rememberPagerState(pageCount = { heroPacks.size })
    val fallbacks = listOf(
        R.drawable.img_hero_atelier,
        R.drawable.img_studded_jacket,
        R.drawable.img_villain_jacket
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(508.dp)
            .background(surfaces.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pack = heroPacks[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onEnterCollection(pack) }
                    .testTag("elite_hero_pack_${pack.packId}")
            ) {
                SlowEditorialZoomBox(
                    triggerKey = "${pagerState.currentPage}_$page",
                    modifier = Modifier.fillMaxSize()
                ) {
                    MelvoreNetworkImage(
                        url = pack.mobileImageUrl ?: pack.heroImageUrl,
                        contentDescription = pack.title,
                        fallbackDrawable = fallbacks[page % fallbacks.size],
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to surfaces.imageOverlay.copy(alpha = 0.25f),
                                    0.42f to Color.Transparent,
                                    0.76f to surfaces.imageOverlay.copy(alpha = 0.72f),
                                    1f to surfaces.background
                                )
                            )
                        )
                )

                // Explicit Luxe Showroom badge
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = surfaces.imageOverlay.copy(alpha = 0.76f),
                    border = BorderStroke(1.dp, ext.goldAccent.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .testTag("elite_luxe_mode_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(Icons.Outlined.Diamond, contentDescription = null, tint = ext.goldAccent, modifier = Modifier.size(12.dp))
                        Text(
                            text = "MELVORE LUXE",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.goldAccent,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp
                        )
                    }
                }

                if (pack.isSponsored) {
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = surfaces.imageOverlay.copy(alpha = 0.78f),
                        border = BorderStroke(1.dp, ext.goldAccent.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "SPONSORED SHOWCASE",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.goldAccent,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.Diamond, contentDescription = null, tint = ext.goldAccent, modifier = Modifier.size(15.dp))
                        GoldShimmerEyebrow(
                            text = pack.eyebrow.ifBlank { pack.brandName }.ifBlank { "MELVORE LUXE · PRIVATE SHOWROOM" }.uppercase(),
                            baseColor = ext.goldAccent,
                            letterSpacing = 2.2.sp
                        )
                    }
                    Text(
                        text = pack.title,
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = MelvoreGothicFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = surfaces.onImageOverlay,
                        maxLines = 2
                    )
                    if (pack.subtitle.isNotBlank()) {
                        Text(
                            text = pack.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = surfaces.onImageOverlay.copy(alpha = 0.85f),
                            maxLines = 2
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    MetallicShineButton(
                        onClick = { onEnterCollection(pack) },
                        containerColor = ext.goldAccent,
                        contentColor = ensureContrast(surfaces.onAccent, ext.goldAccent),
                        borderColor = ext.goldAccent.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = pack.ctaText.ifBlank { "ENTER COLLECTION" }.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = MelvoreBrandFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        if (heroPacks.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                heroPacks.indices.forEach { idx ->
                    val selected = idx == pagerState.currentPage
                    val w by animateDpAsState(if (selected) 28.dp else 10.dp, tween(240), label = "luxe_hero_dot")
                    Box(
                        modifier = Modifier
                            .width(w)
                            .height(2.dp)
                            .background(if (selected) ext.goldAccent else surfaces.onImageOverlay.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }
}

@Composable
private fun LuxeSectionHeader(
    eyebrow: String,
    title: String,
    actionLabel: String = "",
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.sp,
                color = ext.goldAccent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = MelvoreSerifFontFamily,
                color = surfaces.onSurface
            )
        }
        if (actionLabel.isNotBlank()) {
            Surface(
                onClick = onAction,
                shape = RoundedCornerShape(2.dp),
                color = surfaces.surface,
                border = BorderStroke(1.dp, ext.goldAccent.copy(alpha = 0.4f)),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                    Text(actionLabel, style = MaterialTheme.typography.labelMedium, color = ext.goldAccent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Layout Variant 1: Full-Width Portrait Editorial Banner + Collector Horizontal Rail
 */
@Composable
private fun LuxePortraitShowcaseBlock(
    pack: ElitePack,
    currency: String,
    onOpenPack: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onOpenBrand: (Int, String) -> Unit
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LuxeSectionHeader(
            eyebrow = pack.brandName.ifBlank { pack.eyebrow }.ifBlank { "PRIVATE EDITION" },
            title = pack.title,
            actionLabel = "Enter Capsule →",
            onAction = onOpenPack,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        Surface(
            onClick = onOpenPack,
            shape = RoundedCornerShape(4.dp),
            color = surfaces.surface,
            border = BorderStroke(1.dp, ext.goldAccent.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(228.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MelvoreNetworkImage(
                    url = pack.mobileImageUrl ?: pack.heroImageUrl,
                    contentDescription = pack.title,
                    fallbackDrawable = R.drawable.img_biker_jacket,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(surfaces.imageOverlay.copy(alpha = 0.88f), Color.Transparent)))
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp)
                        .width(260.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (pack.story.isNotBlank() || pack.subtitle.isNotBlank()) {
                        Text(
                            text = pack.story.ifBlank { pack.subtitle },
                            style = MaterialTheme.typography.bodySmall,
                            color = surfaces.onImageOverlay.copy(alpha = 0.88f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "EXPLORE EDITION →",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.Bold,
                        color = ext.goldAccent
                    )
                }
            }
        }

        if (pack.items.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pack.items, key = { "lps_${pack.packId}_${it.itemId}" }) { item ->
                    LuxeCollectorItemTile(
                        title = item.title.ifBlank { item.product?.name ?: item.brand?.name ?: "" },
                        subtitle = item.product?.let { formatPrice(it.effectivePrice, currency) } ?: item.subtitle,
                        badge = item.badgeText.ifBlank { if (item.isSponsored) "SPONSORED" else "" },
                        imageUrl = if (item.product != null) item.product.imageUrl else (item.imageUrl ?: item.brand?.coverImageUrl),
                        onClick = {
                            when {
                                item.product != null -> onOpenProduct(item.product)
                                item.brand != null -> onOpenBrand(item.brand.brandId, item.brand.slug)
                                else -> onOpenPack()
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Layout Variant 2: Designer Story Card + Hero Product Spotlight + Companion Pieces
 */
@Composable
private fun LuxeDesignerSpotlightBlock(
    pack: ElitePack,
    currency: String,
    wishlistIds: List<Int>,
    onOpenPack: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    onQuickAdd: (Product) -> Unit
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    val packProducts = remember(pack.items) { pack.items.mapNotNull { it.product } }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LuxeSectionHeader(
            eyebrow = pack.eyebrow.ifBlank { "ATELIER SPOTLIGHT" },
            title = pack.title,
            actionLabel = "View Collection →",
            onAction = onOpenPack,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        if (pack.story.isNotBlank() || pack.subtitle.isNotBlank()) {
            Surface(
                onClick = onOpenPack,
                shape = RoundedCornerShape(4.dp),
                color = surfaces.surface,
                border = BorderStroke(1.dp, surfaces.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = "“${pack.story.ifBlank { pack.subtitle }}”",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = MelvoreSerifFontFamily,
                    color = surfaces.onSurfaceMuted,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        val heroProd = packProducts.firstOrNull()
        if (heroProd != null) {
            ProductEditorialSpotlightCard(
                eyebrow = pack.brandName.ifBlank { "COLLECTOR PIECE" },
                product = heroProd,
                currency = currency,
                isWishlisted = wishlistIds.contains(heroProd.id),
                onOpenProduct = { onOpenProduct(heroProd) },
                onWishlistClick = { onToggleWishlist(heroProd.id) },
                onQuickAdd = { onQuickAdd(heroProd) },
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}

/**
 * Layout Variant 3: Squared 2-Column Editorial Archive Grid
 */
@Composable
private fun LuxeSquaredArchiveGridBlock(
    pack: ElitePack,
    currency: String,
    onOpenPack: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onOpenBrand: (Int, String) -> Unit
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LuxeSectionHeader(
            eyebrow = pack.eyebrow.ifBlank { "NOCTURNAL ARCHIVE" },
            title = pack.title,
            actionLabel = "All Pieces →",
            onAction = onOpenPack,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        pack.items.take(4).chunked(2).forEach { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { item ->
                    Surface(
                        onClick = {
                            when {
                                item.product != null -> onOpenProduct(item.product)
                                item.brand != null -> onOpenBrand(item.brand.brandId, item.brand.slug)
                                else -> onOpenPack()
                            }
                        },
                        shape = RoundedCornerShape(3.dp),
                        color = surfaces.surface,
                        border = BorderStroke(1.dp, ext.goldAccent.copy(alpha = 0.28f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column {
                            MelvoreNetworkImage(
                                url = if (item.product != null) item.product.imageUrl else item.imageUrl,
                                contentDescription = item.title,
                                fallbackDrawable = R.drawable.img_studded_jacket,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = item.title.ifBlank { item.product?.name.orEmpty() },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = surfaces.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.product?.let { formatPrice(it.effectivePrice, currency) } ?: item.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = ext.goldAccent
                                )
                            }
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LuxeCollectorItemTile(
    title: String,
    subtitle: String,
    badge: String,
    imageUrl: String?,
    onClick: () -> Unit
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(3.dp),
        color = surfaces.surface,
        border = BorderStroke(1.dp, surfaces.border),
        modifier = Modifier.width(180.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 3.8f)
            ) {
                MelvoreNetworkImage(
                    url = imageUrl,
                    contentDescription = title,
                    fallbackDrawable = R.drawable.img_villain_jacket,
                    modifier = Modifier.fillMaxSize()
                )
                if (badge.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = surfaces.imageOverlay.copy(alpha = 0.80f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = badge.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.goldAccent,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = surfaces.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = ext.goldAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EliteBrandTile(
    brand: BrandProfileItem,
    onClick: () -> Unit
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(3.dp),
        color = surfaces.surface,
        border = BorderStroke(1.dp, ext.goldAccent.copy(alpha = 0.35f)),
        modifier = Modifier.width(164.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MelvoreNetworkImage(
                url = brand.logoUrl ?: brand.coverImageUrl,
                contentDescription = brand.name,
                fallbackDrawable = R.drawable.img_hero_atelier,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
            Text(
                text = brand.name.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = surfaces.onSurface,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (brand.productCount > 0) "${brand.productCount} pieces" else "Enter Maison",
                style = MaterialTheme.typography.labelSmall,
                color = ext.goldAccent
            )
        }
    }
}

@Composable
private fun EliteBrandWideCard(
    brand: BrandProfileItem,
    onClick: () -> Unit
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(3.dp),
        color = surfaces.surface,
        border = BorderStroke(1.dp, surfaces.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MelvoreNetworkImage(
                url = brand.logoUrl ?: brand.coverImageUrl,
                contentDescription = brand.name,
                fallbackDrawable = R.drawable.img_hero_atelier,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(brand.name, style = MaterialTheme.typography.titleMedium, color = surfaces.onSurface, fontFamily = MelvoreSerifFontFamily)
                Text(
                    text = brand.tagline.ifBlank { brand.originCountry }.ifBlank { "Luxury Maison" },
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaces.onSurfaceMuted,
                    maxLines = 2
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ext.goldAccent, modifier = Modifier.size(18.dp))
        }
    }
}
