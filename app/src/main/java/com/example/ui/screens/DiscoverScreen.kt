package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BrandProfileItem
import com.example.model.Category
import com.example.model.CollectionItem
import com.example.model.Product
import com.example.model.StyleWorld
import com.example.ui.components.LiquidGlassBar
import com.example.ui.components.MelvoreAnimatedEntrance
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.components.ProductCard
import com.example.ui.components.ProductGridSkeleton
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.MelvoreBrandFontFamily
import com.example.ui.theme.MelvoreSerifFontFamily
import com.example.ui.theme.ensureContrast
import com.example.ui.theme.styleWorldVisualSpec
import com.example.ui.viewmodel.DiscoveryUiState
import com.example.ui.viewmodel.MelvoreUiState

@Composable
fun DiscoverScreen(
    uiState: MelvoreUiState,
    discoveryState: DiscoveryUiState,
    onLoadDiscover: () -> Unit,
    onSelectBrandTab: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    onOpenBrand: (brandId: Int, slug: String) -> Unit,
    onOpenStyleWorld: (styleId: Int, slug: String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectCollection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current

    LaunchedEffect(Unit) {
        onLoadDiscover()
    }

    val brandTabs = listOf(
        "featured" to "Featured",
        "trending" to "Trending",
        "sponsored" to "Sponsored",
        "new" to "New Drops",
        "recent" to "Recently Viewed",
        "all" to "All Houses"
    )

    val mergedStyleWorlds = remember(discoveryState.styleWorlds, discoveryState.homeFeed.styleWorlds) {
        val dbList = discoveryState.styleWorlds.ifEmpty { discoveryState.homeFeed.styleWorlds }
        val dbBySlug = dbList.associateBy { it.slug.lowercase() }
        val presets = listOf(
            StyleWorld(0, "y2k", "Y2K", "Metallic layers & low-slung cuts", "", "y2k_chrome", "y2k_chrome", "y2k"),
            StyleWorld(0, "goth", "Goth", "Architectural obsidian & hardware", "", "goth", "smooth_reveal", "gothic"),
            StyleWorld(0, "coquette", "Coquette", "Romantic ribbons & delicate draping", "", "coquette", "smooth_reveal", "coquette"),
            StyleWorld(0, "streetwear", "Streetwear", "Oversized proportions & utility", "", "streetwear", "smooth_reveal", "street"),
            StyleWorld(0, "k-fashion", "K-Fashion", "Seoul-inspired crisp tonal layering", "", "k_fashion", "smooth_reveal", "k_fashion"),
            StyleWorld(0, "quiet-luxury", "Quiet Luxury", "Understated wool, silk & restraint", "", "quiet_luxury", "smooth_reveal", "old_money"),
            StyleWorld(0, "minimal", "Minimal", "Pure monochrome wardrobe architecture", "", "minimal", "smooth_reveal", "minimal"),
            StyleWorld(0, "grunge", "Grunge", "Distressed denim & moto attitude", "", "grunge", "smooth_reveal", "grunge"),
            StyleWorld(0, "old-money", "Old Money", "Heritage knitwear & timeless poise", "", "old_money", "smooth_reveal", "old_money"),
            StyleWorld(0, "casual", "Casual", "Relaxed daily silhouettes", "", "casual", "smooth_reveal", "minimal"),
            StyleWorld(0, "party-wear", "Party Wear", "After-hours statements & shine", "", "party_wear", "smooth_reveal", "party_wear")
        )
        val combined = presets.map { preset ->
            dbBySlug[preset.slug]
                ?: dbList.find { it.name.equals(preset.name, ignoreCase = true) }
                ?: preset
        }
        val extra = dbList.filter { db ->
            combined.none { it.styleWorldId == db.styleWorldId && db.styleWorldId > 0 }
        }
        (combined + extra).distinctBy { it.slug.lowercase() }
    }

    val categories = discoveryState.homeFeed.categories.ifEmpty { uiState.categories }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("discover_screen"),
        contentPadding = PaddingValues(top = 10.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Editorial Magazine Masthead & Search Trigger
        item(key = "discover_masthead") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "MELVORE EDITION · DISCOVERY",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.8.sp,
                    fontWeight = FontWeight.Bold,
                    color = ext.goldAccent
                )
                Text(
                    text = "Aesthetic Worlds, Houses & Curated Edits",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = MelvoreSerifFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                LiquidGlassBar(
                    onClick = onOpenSearch,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("discover_search_bar")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = "Search Y2K, Korean Minimal, Leather, Houses…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Aesthetic Style Worlds Directory (Distinct Typography & Palette per World)
        item(key = "discover_style_worlds") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EditorialSectionHeader(
                    eyebrow = "CURATED MOVEMENTS",
                    title = "Aesthetic Worlds",
                    actionLabel = "",
                    onAction = {},
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                if (discoveryState.isDiscoverLoading && discoveryState.styleWorlds.isEmpty() && mergedStyleWorlds.isEmpty()) {
                    ProductGridSkeleton(rows = 1)
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(mergedStyleWorlds, key = { "sw_${it.slug}" }) { world ->
                            DiscoverStyleWorldCard(
                                world = world,
                                onClick = { onOpenStyleWorld(world.styleWorldId, world.slug) }
                            )
                        }
                    }
                }
            }
        }

        // Bento Department Mosaic (Categories)
        if (categories.isNotEmpty()) {
            item(key = "discover_categories_bento") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorialSectionHeader(
                        eyebrow = "WARDROBE ARCHITECTURE",
                        title = "Departments",
                        actionLabel = "Shop All →",
                        onAction = { onSelectCategory("all") },
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    DiscoverCategoryBento(
                        categories = categories,
                        onSelectCategory = onSelectCategory
                    )
                }
            }
        }

        // Explore Brands Directory with Segmented Filter Pills
        item(key = "discover_brands_directory") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EditorialSectionHeader(
                    eyebrow = "GLOBAL ATELIERS",
                    title = "Explore Houses",
                    actionLabel = "",
                    onAction = {},
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(brandTabs, key = { it.first }) { (key, label) ->
                        val selected = discoveryState.activeBrandTab == key
                        Surface(
                            onClick = { onSelectBrandTab(key) },
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else ext.borderSubtle
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("brand_tab_$key")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                val brands = discoveryState.directoryBrands
                if (brands.isEmpty() && !discoveryState.isDiscoverLoading) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ext.warmIvory,
                        border = BorderStroke(1.dp, ext.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = if (discoveryState.activeBrandTab == "recent") {
                                "Visit any fashion house to build your recently viewed labels list here."
                            } else {
                                "No labels matched this filter right now. Switch tabs to browse active houses."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.muted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(brands, key = { "br_${it.brandId}" }) { brand ->
                            DiscoverBrandCard(
                                brand = brand,
                                onClick = { onOpenBrand(brand.brandId, brand.slug) }
                            )
                        }
                    }
                }
            }
        }

        // Personalized or Honest Trending Editorial Picks
        val forYou = discoveryState.forYou
        if (forYou.products.isNotEmpty()) {
            item(key = "discover_for_you") {
                MelvoreAnimatedEntrance {
                    HomeForYouEditorialSection(
                        forYou = forYou,
                        currency = uiState.currency,
                        wishlistIds = uiState.wishlistIds,
                        onDiscoverAll = { onSelectCategory("all") },
                        onOpenBrand = onOpenBrand,
                        onOpenStyleWorld = onOpenStyleWorld,
                        onOpenProduct = onOpenProduct,
                        onQuickAdd = onQuickAdd,
                        onToggleWishlist = onToggleWishlist
                    )
                }
            }
        }

        // Seasonal Collections
        if (uiState.collections.isNotEmpty()) {
            item(key = "discover_collections") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorialSectionHeader(
                        eyebrow = "SEASONAL CAPSULES",
                        title = "Curated Collections",
                        actionLabel = "",
                        onAction = {},
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    DiscoverCollectionRow(
                        collections = uiState.collections,
                        onSelectCollection = onSelectCollection
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverStyleWorldCard(
    world: StyleWorld,
    onClick: () -> Unit
) {
    val spec = styleWorldVisualSpec(world.slug.ifBlank { world.themeKey })
    val drawables = remember {
        mapOf(
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
    val fallbackRes = drawables[world.slug.lowercase()] ?: R.drawable.img_hero_look
    val surfaces = LocalMelvoreSurfaces.current
    val badgeInk = ensureContrast(spec.badgeTextColor, spec.badgeBgColor)

    Surface(
        onClick = onClick,
        shape = spec.shape,
        color = surfaces.surfaceElevated,
        border = BorderStroke(1.2.dp, spec.borderColor),
        modifier = Modifier
            .width(218.dp)
            .height(272.dp)
            .testTag("style_world_card_${world.slug}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MelvoreNetworkImage(
                url = world.mobileImageUrl ?: world.coverImageUrl,
                contentDescription = world.name,
                fallbackDrawable = fallbackRes,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to surfaces.imageOverlay.copy(alpha = spec.scrimTopAlpha),
                                0.45f to Color.Transparent,
                                1f to surfaces.imageOverlay.copy(alpha = 0.88f)
                            )
                        )
                    )
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = spec.badgeBgColor,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = badgeInk, modifier = Modifier.size(11.dp))
                    Text(
                        text = spec.vibeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = badgeInk,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = world.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = spec.fontFamily,
                    fontWeight = FontWeight.Bold,
                    color = surfaces.onImageOverlay,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (world.tagline.isNotBlank()) {
                    Text(
                        text = world.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = surfaces.onImageOverlay.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = if (world.productCount > 0) "${world.productCount} Pieces" else "Enter World",
                        style = MaterialTheme.typography.labelMedium,
                        color = surfaces.onImageOverlay,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = surfaces.onImageOverlay, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
private fun DiscoverCategoryBento(
    categories: List<Category>,
    onSelectCategory: (String) -> Unit
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    val fallbackList = listOf(
        R.drawable.img_biker_jacket,
        R.drawable.img_seoul_jacket,
        R.drawable.img_white_cropped,
        R.drawable.img_loungewear,
        R.drawable.img_hybrid_jacket,
        R.drawable.img_hero_atelier
    )

    Column(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (categories.size >= 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                categories.take(2).forEachIndexed { idx, cat ->
                    Surface(
                        onClick = { onSelectCategory(cat.slug) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, ext.borderSubtle),
                        modifier = Modifier
                            .weight(if (idx == 0) 1.15f else 0.85f)
                            .height(168.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            MelvoreNetworkImage(
                                url = cat.imageUrl,
                                contentDescription = cat.name,
                                fallbackDrawable = fallbackList[idx % fallbackList.size],
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, surfaces.imageOverlay.copy(alpha = 0.82f))))
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = MelvoreSerifFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = surfaces.onImageOverlay
                                )
                                if (cat.itemCount > 0) {
                                    Text(
                                        text = "${cat.itemCount} styles →",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = surfaces.onImageOverlay.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        val remaining = if (categories.size >= 2) categories.drop(2) else categories
        if (remaining.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(remaining.size, key = { "disc_cat_${remaining[it].id}" }) { idx ->
                    val cat = remaining[idx]
                    Surface(
                        onClick = { onSelectCategory(cat.slug) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, ext.borderSubtle),
                        modifier = Modifier.width(124.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MelvoreNetworkImage(
                                url = cat.imageUrl,
                                contentDescription = cat.name,
                                fallbackDrawable = fallbackList[(idx + 2) % fallbackList.size],
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                            )
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverBrandCard(
    brand: BrandProfileItem,
    onClick: () -> Unit
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.borderSubtle),
        modifier = Modifier
            .width(204.dp)
            .testTag("brand_card_${brand.brandId}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(122.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                MelvoreNetworkImage(
                    url = brand.mobileCoverUrl ?: brand.coverImageUrl ?: brand.logoUrl,
                    contentDescription = brand.name,
                    fallbackDrawable = R.drawable.img_hero_atelier,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, surfaces.imageOverlay.copy(alpha = 0.65f)))
                        )
                )
                if (brand.isSponsored) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = surfaces.imageOverlay.copy(alpha = 0.80f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "SPONSORED",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.goldAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (brand.isNew) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = surfaces.surface,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "NEW HOUSE",
                            style = MaterialTheme.typography.labelSmall,
                            color = surfaces.onSurface,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = surfaces.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .size(38.dp)
                ) {
                    if (!brand.logoUrl.isNullOrBlank()) {
                        MelvoreNetworkImage(
                            url = brand.logoUrl,
                            contentDescription = "${brand.name} logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = brand.name.take(2).uppercase(),
                                fontFamily = MelvoreBrandFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = surfaces.onSurface
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = brand.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = brand.tagline.ifBlank { brand.description }.ifBlank { "Contemporary Fashion Label" },
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (brand.originCountry.isNotBlank()) {
                        Icon(Icons.Outlined.Public, contentDescription = null, tint = ext.muted, modifier = Modifier.size(12.dp))
                        Text(brand.originCountry, style = MaterialTheme.typography.labelSmall, color = ext.muted)
                        Text("·", color = ext.muted, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        text = if (brand.productCount > 0) "${brand.productCount} styles" else "Explore Store",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.goldAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverCollectionRow(
    collections: List<CollectionItem>,
    onSelectCollection: (String) -> Unit
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(collections, key = { "disc_col_${it.id}" }) { col ->
            Surface(
                onClick = { onSelectCollection(col.slug) },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle),
                modifier = Modifier.width(216.dp)
            ) {
                Box(modifier = Modifier.height(132.dp)) {
                    MelvoreNetworkImage(
                        url = col.bannerUrl,
                        contentDescription = col.name,
                        fallbackDrawable = R.drawable.img_hero_look,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, surfaces.imageOverlay.copy(alpha = 0.84f))))
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = col.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = MelvoreSerifFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = surfaces.onImageOverlay,
                            maxLines = 1
                        )
                        if (col.subtitle.isNotBlank()) {
                            Text(
                                text = col.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = surfaces.onImageOverlay.copy(alpha = 0.82f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
