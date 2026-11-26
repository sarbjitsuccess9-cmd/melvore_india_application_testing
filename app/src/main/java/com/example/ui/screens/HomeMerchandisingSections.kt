package com.example.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.model.BlogPost
import com.example.model.BrandProfileItem
import com.example.model.Category
import com.example.model.FeedDiagnostics
import com.example.model.ForYouBundle
import com.example.model.Product
import com.example.model.PromoBanner
import com.example.model.StyleWorld
import com.example.model.VideoReel
import com.example.model.formatPrice
import com.example.ui.components.GoldShimmerEyebrow
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.components.MetallicShineButton
import com.example.ui.components.ProductCard
import com.example.ui.components.ProductEditorialSpotlightCard
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreDesignManifest
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.MelvoreBrandFontFamily
import com.example.ui.theme.MelvoreSerifFontFamily
import com.example.ui.theme.contrastOnColor
import com.example.ui.theme.ensureContrast
import com.example.ui.theme.parseHexColor as parseThemeHexColor
import com.example.ui.theme.styleWorldVisualSpec

data class HomeCategoryShortcut(
    val key: String,
    val label: String,
    val categorySlug: String,
    val collectionSlug: String = "",
    val imageUrl: String? = null,
    @DrawableRes val fallbackDrawable: Int? = null,
    val editorialBadge: String? = null,
    val editorialSubtitle: String? = null,
    val accentHex: String = "#141414",
    val bgHex: String = "#F8F7F4",
    val motifKey: String = "editorial_frame",
    val tileSpan: String = "regular",
    val itemCount: Int = 0
)

/**
 * Compact Magazine-Style Circular Category Shortcut Strip placed discreetly above the Hero Slider.
 * Small circular photographic thumbnails (~58.dp) + clean single-line label keep Hero photography above the fold.
 */
@Composable
fun HomeCategoryNavigationStrip(
    dbCategories: List<Category>,
    selectedKey: String,
    onSelectShortcut: (HomeCategoryShortcut) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val manifest = LocalMelvoreDesignManifest.current
    val stripSpec = manifest.components.categoryStrip
    val isPortraitTile = stripSpec.variant.equals("portrait_tiles", ignoreCase = true)
    val tileShape = if (isPortraitTile) {
        RoundedCornerShape(stripSpec.tileRadiusDp.coerceIn(4, 24).dp)
    } else {
        CircleShape
    }
    val shortcuts = remember(dbCategories) {
        val dbBySlug = dbCategories.associateBy { it.slug.lowercase() }
        val dbByName = dbCategories.associateBy { it.name.lowercase() }
        fun findDb(slug: String, vararg aliases: String): Category? {
            return dbBySlug[slug.lowercase()]
                ?: aliases.firstNotNullOfOrNull { dbBySlug[it.lowercase()] ?: dbByName[it.lowercase()] }
        }

        val curatedSpecs = listOf(
            Triple("all", "All Edits", R.drawable.img_hero_look),
            Triple("men", "Men", R.drawable.img_hybrid_jacket),
            Triple("women", "Women", R.drawable.img_white_cropped),
            Triple("jackets", "Jackets", R.drawable.img_biker_jacket),
            Triple("co-ords", "Co-ords", R.drawable.img_loungewear),
            Triple("dresses", "Dresses", R.drawable.img_hero_atelier),
            Triple("t-shirts", "T-Shirts", R.drawable.img_white_cropped),
            Triple("shirts", "Shirts", R.drawable.img_seoul_jacket),
            Triple("jeans", "Jeans", R.drawable.img_villain_jacket),
            Triple("bottomwear", "Bottomwear", R.drawable.img_loungewear),
            Triple("accessories", "Accessories", R.drawable.img_studded_jacket),
            Triple("new-arrivals", "New Drops", R.drawable.img_hero_seoul),
            Triple("sale", "Archive Sale", R.drawable.img_studded_jacket)
        )

        val coreList = curatedSpecs.map { (slug, label, fallbackRes) ->
            val matched = findDb(
                slug,
                label,
                label.replace("-", ""),
                label.removeSuffix("s")
            )
            when (slug) {
                "all" -> HomeCategoryShortcut(
                    key = "all",
                    label = label,
                    categorySlug = "all",
                    imageUrl = matched?.coverImageUrl ?: matched?.imageUrl,
                    fallbackDrawable = fallbackRes
                )
                "new-arrivals", "sale" -> HomeCategoryShortcut(
                    key = slug,
                    label = label,
                    categorySlug = "all",
                    collectionSlug = slug,
                    imageUrl = matched?.coverImageUrl ?: matched?.imageUrl,
                    fallbackDrawable = fallbackRes
                )
                else -> HomeCategoryShortcut(
                    key = matched?.slug ?: slug,
                    label = matched?.name ?: label,
                    categorySlug = matched?.slug ?: slug,
                    imageUrl = matched?.coverImageUrl ?: matched?.imageUrl,
                    fallbackDrawable = fallbackRes
                )
            }
        }

        val usedKeys = coreList.map { it.categorySlug.lowercase() }.toSet()
        val extraDb = dbCategories
            .filter { it.slug.lowercase() !in usedKeys }
            .map {
                HomeCategoryShortcut(
                    key = it.slug,
                    label = it.name,
                    categorySlug = it.slug,
                    imageUrl = it.coverImageUrl ?: it.imageUrl,
                    fallbackDrawable = R.drawable.img_hero_look
                )
            }
        coreList + extraDb
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_category_strip"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(shortcuts, key = { "hcat_${it.key}" }) { item ->
            val isActive = item.key == selectedKey
            val ringColor by animateColorAsState(
                targetValue = if (isActive) MaterialTheme.colorScheme.primary else ext.borderSubtle.copy(alpha = 0.65f),
                animationSpec = tween(200),
                label = "cat_circle_ring"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .width(if (isPortraitTile) 76.dp else 68.dp)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectShortcut(item) }
                    .padding(vertical = 2.dp)
                    .testTag("home_cat_chip_${item.key}")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = (if (isPortraitTile) {
                        Modifier
                            .width(74.dp)
                            .height(92.dp)
                    } else {
                        Modifier.size(58.dp)
                    })
                        .clip(tileShape)
                        .border(if (isActive) 2.dp else 0.75.dp, ringColor, tileShape)
                        .padding(if (isActive) 2.5.dp else 0.dp)
                ) {
                    MelvoreNetworkImage(
                        url = item.imageUrl,
                        contentDescription = item.label,
                        fallbackDrawable = item.fallbackDrawable,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(tileShape)
                    )
                }

                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SecondaryPromoBannerStrip(
    promoBanners: List<PromoBanner>,
    freeShippingMinimum: Double,
    currency: String,
    onBannerClick: (PromoBanner) -> Unit,
    onOpenShopAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    if (promoBanners.isEmpty() && freeShippingMinimum <= 0.0) return

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_secondary_promo_strip"),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (freeShippingMinimum > 0.0) {
            item(key = "promo_free_ship") {
                Surface(
                    onClick = onOpenShopAll,
                    shape = RoundedCornerShape(10.dp),
                    color = ext.warmIvory,
                    border = BorderStroke(1.dp, ext.goldAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.heightIn(min = 56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ext.softChampagne,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.LocalShipping,
                                    contentDescription = null,
                                    tint = ext.goldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "COMPLIMENTARY DELIVERY",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.goldAccent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Free shipping on orders above ${formatPrice(freeShippingMinimum, currency)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        items(promoBanners, key = { "sec_promo_${it.id}_${it.title}" }) { banner ->
            Surface(
                onClick = { onBannerClick(banner) },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle),
                modifier = Modifier.heightIn(min = 56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (!banner.imageUrl.isNullOrBlank()) {
                        MelvoreNetworkImage(
                            url = banner.imageUrl,
                            contentDescription = banner.title,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }
                    Column(modifier = Modifier.width(196.dp)) {
                        if (banner.tag.isNotBlank()) {
                            Text(
                                text = banner.tag.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.crimson,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = banner.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = ext.muted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeBrandDiscoverySection(
    brands: List<BrandProfileItem>,
    onSelectBrand: (BrandProfileItem) -> Unit,
    onDiscoverAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    if (brands.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EditorialSectionHeader(
            eyebrow = "CURATED HOUSES",
            title = "Brand Discovery",
            actionLabel = "All Houses →",
            onAction = onDiscoverAll,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag("home_brands_strip")
        ) {
            items(brands, key = { "hb_${it.brandId}_${it.slug}" }) { brand ->
                Surface(
                    onClick = { onSelectBrand(brand) },
                    shape = RoundedCornerShape(4.dp),
                    color = surfaces.surface,
                    border = BorderStroke(0.5.dp, surfaces.border),
                    modifier = Modifier
                        .width(172.dp)
                        .testTag("home_brand_card_${brand.brandId}")
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(112.dp)
                                .background(surfaces.surfaceElevated)
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
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, surfaces.imageOverlay.copy(alpha = 0.62f))
                                        )
                                    )
                            )

                            val badgeText = when {
                                brand.isSponsored -> "SPONSORED"
                                brand.isNew -> "NEW LABEL"
                                brand.isFeatured -> "FEATURED"
                                else -> ""
                            }
                            if (badgeText.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (brand.isSponsored) surfaces.imageOverlay.copy(alpha = 0.80f) else surfaces.surface.copy(alpha = 0.94f),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp,
                                        color = if (brand.isSponsored) ensureContrast(ext.goldAccent, surfaces.imageOverlay) else surfaces.onSurface,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = surfaces.surface,
                                border = BorderStroke(1.dp, surfaces.border),
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
                                            style = MaterialTheme.typography.labelLarge,
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
                                text = brand.tagline.ifBlank { brand.description }.ifBlank {
                                    brand.originCountry.ifBlank { "Contemporary Fashion Label" }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Storefront,
                                    contentDescription = null,
                                    tint = ext.goldAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (brand.productCount > 0) "${brand.productCount} curated styles" else "Visit Brand Store",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ext.goldAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class EditorialStoryTile(
    val key: String,
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val imageRes: Int,
    val categorySlug: String = "all",
    val collectionSlug: String = "",
    val query: String = "",
    val styleSlug: String = ""
)

/**
 * Art-directed Editorial Fashion Campaigns mixing:
 * 1. Full-bleed feature banner with floating warm-ivory editorial card (The Winter Edit)
 * 2. Two-column portrait diptych (Streetwear Stories & Korean Fashion)
 * 3. Asymmetric leather & statement outerwear showcase
 * 4. Horizontal lookbook capsule rail (Everyday Essentials, Weekend Wardrobe, Night Out, Minimal, New Season)
 */
@Composable
fun CuratedEditorialCampaignShowcase(
    onNavigateShop: (category: String, collection: String, query: String) -> Unit,
    onNavigateStyleWorld: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    val duoStories = remember {
        listOf(
            EditorialStoryTile(
                key = "streetwear-stories",
                eyebrow = "URBAN DROP",
                title = "Streetwear Stories",
                subtitle = "Boxy silhouettes & heavyweight jersey",
                imageRes = R.drawable.img_hero_seoul,
                styleSlug = "streetwear",
                query = "streetwear"
            ),
            EditorialStoryTile(
                key = "korean-fashion",
                eyebrow = "SEOUL EDIT",
                title = "Korean Fashion",
                subtitle = "Clean tailoring meets effortless drape",
                imageRes = R.drawable.img_seoul_jacket,
                styleSlug = "k-fashion",
                query = "korean"
            )
        )
    }

    val capsuleRail = remember {
        listOf(
            EditorialStoryTile(
                key = "everyday-essentials",
                eyebrow = "DAILY UNIFORM",
                title = "Everyday Essentials",
                subtitle = "Crisp tees, co-ords & loungewear",
                imageRes = R.drawable.img_loungewear,
                collectionSlug = "everyday-essentials",
                query = "essentials"
            ),
            EditorialStoryTile(
                key = "weekend-wardrobe",
                eyebrow = "OFF-DUTY",
                title = "The Weekend Wardrobe",
                subtitle = "Relaxed fits for daylight plans",
                imageRes = R.drawable.img_white_cropped,
                collectionSlug = "weekend-wardrobe",
                query = "casual"
            ),
            EditorialStoryTile(
                key = "night-out",
                eyebrow = "AFTER HOURS",
                title = "Night Out",
                subtitle = "Sculpted dark & statement hardware",
                imageRes = R.drawable.img_studded_jacket,
                styleSlug = "party-wear",
                query = "party"
            ),
            EditorialStoryTile(
                key = "minimal-style",
                eyebrow = "CLEAN LINES",
                title = "Minimal Style",
                subtitle = "Quiet neutral wardrobe foundations",
                imageRes = R.drawable.img_hero_atelier,
                styleSlug = "minimal",
                query = "minimal"
            ),
            EditorialStoryTile(
                key = "new-season-looks",
                eyebrow = "JUST ARRIVED",
                title = "New Season Looks",
                subtitle = "Fresh seasonal silhouettes",
                imageRes = R.drawable.img_hero_look,
                collectionSlug = "new-arrivals"
            )
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EditorialSectionHeader(
            eyebrow = "EDITORIAL STORIES",
            title = "Curated Fashion Edits",
            actionLabel = "Shop All →",
            onAction = { onNavigateShop("all", "", "") },
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        // 1. Full-Width Magazine Campaign Feature: The Winter Edit
        Surface(
            onClick = { onNavigateShop("all", "winter-edit", "winter") },
            shape = RoundedCornerShape(2.dp),
            color = surfaces.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(256.dp)
                .testTag("campaign_winter_edit")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                MelvoreNetworkImage(
                    url = null,
                    contentDescription = "The Winter Edit",
                    fallbackDrawable = R.drawable.img_villain_jacket,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    surfaces.imageOverlay.copy(alpha = 0.78f),
                                    surfaces.imageOverlay.copy(alpha = 0.14f)
                                )
                            )
                        )
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = surfaces.surface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, ext.goldAccent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                        .width(264.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "SEASONAL CAMPAIGN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ensureContrast(ext.goldAccent, surfaces.surface),
                            letterSpacing = 1.4.sp
                        )
                        Text(
                            text = "The Winter Edit",
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = MelvoreSerifFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = surfaces.onSurface
                        )
                        Text(
                            text = "Heavyweight coats, moto leather & layered knits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = surfaces.onSurfaceMuted,
                            maxLines = 2
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = "SHOP THE EDIT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = surfaces.onSurface
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = surfaces.onSurface,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Two-Column Split Portrait Editorial Cards: Streetwear Stories + Korean Fashion
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            duoStories.forEach { tile ->
                Surface(
                    onClick = {
                        if (tile.styleSlug.isNotBlank()) {
                            onNavigateStyleWorld(0, tile.styleSlug)
                        } else {
                            onNavigateShop(tile.categorySlug, tile.collectionSlug, tile.query)
                        }
                    },
                    shape = RoundedCornerShape(4.dp),
                    color = surfaces.surface,
                    modifier = Modifier
                        .weight(1f)
                        .height(246.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        MelvoreNetworkImage(
                            url = null,
                            contentDescription = tile.title,
                            fallbackDrawable = tile.imageRes,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Transparent,
                                            0.5f to surfaces.imageOverlay.copy(alpha = 0.22f),
                                            1.0f to surfaces.imageOverlay.copy(alpha = 0.88f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(13.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = tile.eyebrow,
                                style = MaterialTheme.typography.labelSmall,
                                color = ensureContrast(ext.goldAccent, surfaces.imageOverlay),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            )
                            Text(
                                text = tile.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = MelvoreSerifFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = surfaces.onImageOverlay
                            )
                            Text(
                                text = tile.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = surfaces.onImageOverlay.copy(alpha = 0.84f),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // 3. Asymmetric Outerwear Pairing: The Leather Collection (wide) + Statement Jackets (compact)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                onClick = { onNavigateShop("jackets", "", "leather") },
                shape = RoundedCornerShape(4.dp),
                color = surfaces.surfaceElevated,
                modifier = Modifier
                    .weight(1.25f)
                    .height(198.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MelvoreNetworkImage(
                        url = null,
                        contentDescription = "The Leather Collection",
                        fallbackDrawable = R.drawable.img_biker_jacket,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, surfaces.imageOverlay.copy(alpha = 0.86f))
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "SIGNATURE CRAFT",
                            style = MaterialTheme.typography.labelSmall,
                            color = ensureContrast(ext.goldAccent, surfaces.imageOverlay),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "The Leather Collection",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = MelvoreSerifFontFamily,
                            color = surfaces.onImageOverlay
                        )
                        Text(
                            text = "Moto cuts & matte hardware →",
                            style = MaterialTheme.typography.labelSmall,
                            color = surfaces.onImageOverlay.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Surface(
                onClick = { onNavigateShop("jackets", "", "") },
                shape = RoundedCornerShape(4.dp),
                color = ext.warmIvory,
                border = BorderStroke(0.5.dp, surfaces.border),
                modifier = Modifier
                    .weight(0.85f)
                    .height(198.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    MelvoreNetworkImage(
                        url = null,
                        contentDescription = "Statement Jackets",
                        fallbackDrawable = R.drawable.img_hybrid_jacket,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                    )
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "OUTERWEAR",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ext.goldAccent
                        )
                        Text(
                            text = "Statement Jackets",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = surfaces.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Explore layerables →",
                            style = MaterialTheme.typography.labelSmall,
                            color = surfaces.onSurfaceMuted,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // 4. Horizontal Capsule Lookbook Rail
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(capsuleRail, key = { "cap_${it.key}" }) { tile ->
                Surface(
                    onClick = {
                        if (tile.styleSlug.isNotBlank()) {
                            onNavigateStyleWorld(0, tile.styleSlug)
                        } else {
                            onNavigateShop(tile.categorySlug, tile.collectionSlug, tile.query)
                        }
                    },
                    shape = RoundedCornerShape(4.dp),
                    color = surfaces.surface,
                    border = BorderStroke(0.5.dp, surfaces.border),
                    modifier = Modifier.width(164.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(142.dp)
                        ) {
                            MelvoreNetworkImage(
                                url = null,
                                contentDescription = tile.title,
                                fallbackDrawable = tile.imageRes,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = surfaces.surface.copy(alpha = 0.94f),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = tile.eyebrow,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = surfaces.onSurface,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.padding(11.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = tile.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tile.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.muted,
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

/**
 * Style Worlds / Shop by Aesthetic using distinct per-world typography, shape, border,
 * badge, and mood palette from `styleWorldVisualSpec(...)`.
 */
@Composable
fun StyleWorldsShowcaseSection(
    dbStyleWorlds: List<StyleWorld>,
    onOpenStyleWorld: (Int, String) -> Unit,
    onDiscoverAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mergedWorlds = remember(dbStyleWorlds) {
        val dbByKey = dbStyleWorlds.associateBy { it.slug.lowercase() }
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
            val match = dbByKey[preset.slug]
                ?: dbStyleWorlds.find {
                    it.name.equals(preset.name, ignoreCase = true) ||
                        it.slug.contains(preset.slug.substringBefore("-"), ignoreCase = true)
                }
            match ?: preset
        }
        val extra = dbStyleWorlds.filter { db ->
            combined.none { it.styleWorldId == db.styleWorldId && db.styleWorldId > 0 }
        }
        (combined + extra).distinctBy { it.slug.lowercase() }
    }

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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EditorialSectionHeader(
            eyebrow = "SHOP BY AESTHETIC",
            title = "Style Worlds",
            actionLabel = "All Worlds →",
            onAction = onDiscoverAll,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag("home_style_worlds_row")
        ) {
            items(mergedWorlds.size, key = { "sw_home_${mergedWorlds[it].slug}" }) { index ->
                val surfaces = LocalMelvoreSurfaces.current
                val world = mergedWorlds[index]
                val spec = styleWorldVisualSpec(world.slug.ifBlank { world.themeKey })
                val fallbackRes = drawables[world.slug.lowercase()] ?: R.drawable.img_hero_look

                Surface(
                    onClick = { onOpenStyleWorld(world.styleWorldId, world.slug) },
                    shape = RoundedCornerShape(4.dp),
                    color = surfaces.surfaceElevated,
                    border = BorderStroke(0.5.dp, spec.borderColor.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .width(spec.cardWidth)
                        .height(spec.cardHeight)
                        .testTag("home_style_tile_${world.slug}")
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
                                            0.46f to Color.Transparent,
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
                            Text(
                                text = spec.vibeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.5.sp,
                                color = ensureContrast(spec.badgeTextColor, spec.badgeBgColor),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(13.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = world.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = spec.fontFamily,
                                fontWeight = FontWeight.Bold,
                                color = surfaces.onImageOverlay,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = world.tagline.ifBlank { "Explore curated pieces" },
                                style = MaterialTheme.typography.labelSmall,
                                color = surfaces.onImageOverlay.copy(alpha = 0.84f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Personalized "For You" module in a warm ivory editorial surface,
 * surfacing user signals (favourite styles & recent brands) or an honest Trending fallback.
 */
@Composable
fun HomeForYouEditorialSection(
    forYou: ForYouBundle,
    currency: String,
    wishlistIds: List<Int>,
    onDiscoverAll: () -> Unit,
    onOpenBrand: (Int, String) -> Unit,
    onOpenStyleWorld: (Int, String) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (forYou.products.isEmpty()) return
    val ext = LocalMelvoreColors.current

    Surface(
        shape = RoundedCornerShape(0.dp),
        color = ext.warmIvory,
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_for_you_container")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EditorialSectionHeader(
                eyebrow = if (forYou.isPersonalized) "TAILORED TO YOUR TASTE" else "DISCOVERY RADAR",
                title = forYou.title.ifBlank {
                    if (forYou.isPersonalized) "Picked For You" else "Trending Across Melvore"
                },
                actionLabel = "Explore →",
                onAction = onDiscoverAll,
                modifier = Modifier.padding(horizontal = 14.dp)
            )

            if (forYou.subtitle.isNotBlank()) {
                Text(
                    text = forYou.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
            }

            val displayedWorlds = forYou.favouriteStyles
                .ifEmpty { forYou.preferredWorlds }
                .ifEmpty { forYou.recentlyExploredWorlds }
            if (displayedWorlds.isNotEmpty() || forYou.recentBrands.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedWorlds, key = { "fy_sw_${it.slug}" }) { sw ->
                        Surface(
                            onClick = { onOpenStyleWorld(sw.styleWorldId, sw.slug) },
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, ext.goldAccent.copy(alpha = 0.5f)),
                            modifier = Modifier.heightIn(min = 36.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = ext.goldAccent, modifier = Modifier.size(13.dp))
                                Text(
                                    text = sw.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    items(forYou.recentBrands, key = { "fy_br_${it.brandId}" }) { br ->
                        Surface(
                            onClick = { onOpenBrand(br.brandId, br.slug) },
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, ext.borderSubtle),
                            modifier = Modifier.heightIn(min = 36.dp)
                        ) {
                            Text(
                                text = br.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.testTag("home_for_you_rail")
            ) {
                items(forYou.products, key = { "fy_prod_${it.id}" }) { prod ->
                    ProductCard(
                        product = prod,
                        currency = currency,
                        isWishlisted = wishlistIds.contains(prod.id),
                        onProductClick = { onOpenProduct(prod) },
                        onWishlistClick = { onToggleWishlist(prod.id) },
                        onAddToCartClick = { onQuickAdd(prod) },
                        modifier = Modifier.width(168.dp)
                    )
                }
            }
        }
    }
}

/**
 * Editorial Spotlight + Companion Rail for Signature Outerwear / Hero Silhouette
 * so product merchandising formats vary visibly across the homepage.
 */
@Composable
fun SignatureOuterwearShowcase(
    heroProduct: Product,
    companionProducts: List<Product>,
    currency: String,
    wishlistIds: List<Int>,
    onOpenShopOuterwear: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EditorialSectionHeader(
            eyebrow = "ATELIER SPOTLIGHT",
            title = "Signature Silhouettes",
            actionLabel = "Shop Edit →",
            onAction = onOpenShopOuterwear,
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        ProductEditorialSpotlightCard(
            eyebrow = "EDITOR'S HERO PIECE",
            product = heroProduct,
            currency = currency,
            isWishlisted = wishlistIds.contains(heroProduct.id),
            onOpenProduct = { onOpenProduct(heroProduct) },
            onWishlistClick = { onToggleWishlist(heroProduct.id) },
            onQuickAdd = { onQuickAdd(heroProduct) },
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        if (companionProducts.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(companionProducts, key = { "sig_comp_${it.id}" }) { prod ->
                    ProductCard(
                        product = prod,
                        currency = currency,
                        isWishlisted = wishlistIds.contains(prod.id),
                        onProductClick = { onOpenProduct(prod) },
                        onWishlistClick = { onToggleWishlist(prod.id) },
                        onAddToCartClick = { onQuickAdd(prod) },
                        modifier = Modifier.width(164.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProductMerchandisingShelf(
    eyebrow: String,
    title: String,
    products: List<Product>,
    currency: String,
    wishlistIds: List<Int>,
    actionLabel: String,
    onAction: () -> Unit,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        EditorialSectionHeader(
            eyebrow = eyebrow,
            title = title,
            actionLabel = actionLabel,
            onAction = onAction,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.testTag("${testTagPrefix}_rail")
        ) {
            items(products, key = { "${testTagPrefix}_${it.id}" }) { prod ->
                ProductCard(
                    product = prod,
                    currency = currency,
                    isWishlisted = wishlistIds.contains(prod.id),
                    onProductClick = { onOpenProduct(prod) },
                    onWishlistClick = { onToggleWishlist(prod.id) },
                    onAddToCartClick = { onQuickAdd(prod) },
                    modifier = Modifier.width(168.dp)
                )
            }
        }
    }
}

@Composable
fun BudgetEditsSection(
    budget999: List<Product>,
    budget1999: List<Product>,
    currency: String,
    wishlistIds: List<Int>,
    onOpenBudget: (collectionSlug: String) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    var selectedTier by remember(budget999.isEmpty()) {
        mutableStateOf(if (budget999.isNotEmpty()) "under-999" else "under-1999")
    }
    val activeProducts = remember(selectedTier, budget999, budget1999) {
        when (selectedTier) {
            "under-999" -> budget999.ifEmpty { budget1999 }
            else -> budget1999.ifEmpty { budget999 }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EditorialSectionHeader(
            eyebrow = "CURATED VALUE",
            title = "Smart Price Edits",
            actionLabel = "Full Edit →",
            onAction = { onOpenBudget(selectedTier) },
            modifier = Modifier.padding(horizontal = 14.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                Triple("under-999", "Under ₹999", "${budget999.size} styles") to ext.emerald,
                Triple("under-1999", "Under ₹1,999", "${budget1999.size} styles") to ext.electricBlue
            ).forEach { (info, accent) ->
                val (slug, label, countText) = info
                val isSelected = selectedTier == slug
                Surface(
                    onClick = {
                        if (selectedTier == slug) onOpenBudget(slug) else selectedTier = slug
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else accent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .testTag("budget_card_$slug")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (countText.startsWith("0")) "Tap to browse" else countText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else accent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (activeProducts.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(activeProducts.take(10), key = { "bdg_${selectedTier}_${it.id}" }) { prod ->
                    ProductCard(
                        product = prod,
                        currency = currency,
                        isWishlisted = wishlistIds.contains(prod.id),
                        onProductClick = { onOpenProduct(prod) },
                        onWishlistClick = { onToggleWishlist(prod.id) },
                        onAddToCartClick = { onQuickAdd(prod) },
                        modifier = Modifier.width(164.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeEditorialJournalStrip(
    stories: List<BlogPost>,
    onOpenJournal: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (stories.isEmpty()) return
    val ext = LocalMelvoreColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EditorialSectionHeader(
            eyebrow = "THE JOURNAL",
            title = "Fashion Notes & Lookbooks",
            actionLabel = "Read All →",
            onAction = onOpenJournal,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag("home_journal_strip")
        ) {
            items(stories, key = { "hblog_${it.id}" }) { post ->
                Surface(
                    onClick = onOpenJournal,
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.5.dp, ext.borderSubtle.copy(alpha = 0.65f)),
                    modifier = Modifier.width(248.dp)
                ) {
                    Column {
                        MelvoreNetworkImage(
                            url = post.imageUrl,
                            contentDescription = post.title,
                            fallbackDrawable = R.drawable.img_hero_atelier,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(136.dp)
                        )
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = post.category.ifBlank { "EDITORIAL" }.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.2.sp,
                                color = ext.goldAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = post.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = MelvoreSerifFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (post.excerpt.isNotBlank()) {
                                Text(
                                    text = post.excerpt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ext.muted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Read Story →",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeVideoDiscoveryStrip(
    videos: List<VideoReel>,
    onOpenVideos: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (videos.isEmpty()) return
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        EditorialSectionHeader(
            eyebrow = "IN MOTION · 9:16",
            title = "Runway & Street Clips",
            actionLabel = "Watch Reels →",
            onAction = onOpenVideos,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.testTag("home_videos_strip")
        ) {
            items(videos, key = { "hvid_${it.videoId}" }) { reel ->
                Surface(
                    onClick = onOpenVideos,
                    shape = RoundedCornerShape(4.dp),
                    color = surfaces.surfaceElevated,
                    modifier = Modifier.width(154.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(9f / 16f)
                    ) {
                        MelvoreNetworkImage(
                            url = reel.posterUrl ?: reel.product?.imageUrl,
                            contentDescription = reel.title,
                            fallbackDrawable = R.drawable.img_hero_seoul,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0f to surfaces.imageOverlay.copy(alpha = 0.18f),
                                            0.5f to Color.Transparent,
                                            1f to surfaces.imageOverlay.copy(alpha = 0.88f)
                                        )
                                    )
                                )
                        )
                        if (reel.durationSeconds > 0) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = surfaces.imageOverlay.copy(alpha = 0.65f),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "0:${reel.durationSeconds.coerceIn(1, 59).toString().padStart(2, '0')}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = surfaces.onImageOverlay,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        LiquidGlassSurface(
                            darkGlass = true,
                            shape = CircleShape,
                            elevation = 4.dp,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(44.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Watch ${reel.title}", tint = surfaces.onImageOverlay)
                            }
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (!reel.brandName.isNullOrBlank()) {
                                Text(
                                    text = reel.brandName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = ensureContrast(ext.goldAccent, surfaces.imageOverlay),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = reel.title.ifBlank { reel.creatorName },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = surfaces.onImageOverlay,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Editorial Portal Card inviting users into the Melvore Luxe Showroom,
 * built cleanly from semantic `MelvoreSurfaceColors` + photo `imageOverlay` scrim.
 */
@Composable
fun EnterLuxeSpotlightCard(
    onEnterLuxe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    val gold = ext.goldAccent
    val goldTextOnPhoto = ensureContrast(gold, surfaces.imageOverlay)
    val ctaFg = ensureContrast(surfaces.onAccent, gold)

    Surface(
        onClick = onEnterLuxe,
        shape = RoundedCornerShape(4.dp),
        color = surfaces.surfaceElevated,
        border = BorderStroke(0.75.dp, gold.copy(alpha = 0.50f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_enter_luxe_card")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            MelvoreNetworkImage(
                url = null,
                contentDescription = "Melvore Luxe Showroom",
                fallbackDrawable = R.drawable.img_hero_atelier,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(238.dp)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to surfaces.imageOverlay.copy(alpha = 0.42f),
                                0.5f to surfaces.imageOverlay.copy(alpha = 0.78f),
                                1f to surfaces.imageOverlay.copy(alpha = 0.94f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Diamond, contentDescription = null, tint = goldTextOnPhoto, modifier = Modifier.size(16.dp))
                    GoldShimmerEyebrow(
                        text = "MELVORE LUXE · PRIVATE ATELIER",
                        baseColor = goldTextOnPhoto,
                        letterSpacing = 2.sp
                    )
                }
                Text(
                    text = "Step Inside The Luxe Showroom",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = MelvoreSerifFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = surfaces.onImageOverlay
                )
                Text(
                    text = "Collector capsules, architectural leather, and runway campaign films curated inside our luxury destination.",
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaces.onImageOverlay.copy(alpha = 0.86f)
                )
                Spacer(Modifier.height(4.dp))
                MetallicShineButton(
                    onClick = onEnterLuxe,
                    containerColor = gold,
                    contentColor = ctaFg,
                    borderColor = gold.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.testTag("home_enter_luxe_button")
                ) {
                    Text(
                        text = "ENTER LUXE",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = MelvoreBrandFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * Debug-only Feed Diagnostics Banner (`BuildConfig.DEBUG`) surfacing missing migration tables
 * or unseeded feed modules cleanly to developers without fabricating storefront data.
 */
@Composable
fun DebugFeedDiagnosticsBanner(
    diagnostics: FeedDiagnostics,
    modifier: Modifier = Modifier
) {
    if (!BuildConfig.DEBUG || !diagnostics.hasWarnings) return
    val ext = LocalMelvoreColors.current
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ext.warmIvory,
        border = BorderStroke(1.dp, ext.amber.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("debug_feed_diagnostics_banner")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = ext.amber, modifier = Modifier.size(16.dp))
                    Text(
                        text = "DEV DIAGNOSTICS · ${diagnostics.missingTables.size} missing tables, ${diagnostics.emptyModules.size} empty modules",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = "Toggle diagnostics",
                    tint = ext.muted,
                    modifier = Modifier.size(18.dp)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (diagnostics.missingTables.isNotEmpty()) {
                        Text(
                            text = "Missing DB tables (run melvore_customer_app_experience_migration.sql): ${diagnostics.missingTables.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = ext.muted
                        )
                    }
                    if (diagnostics.emptyModules.isNotEmpty()) {
                        Text(
                            text = "Unseeded modules (run melvore_sample_editorial_seed.sql): ${diagnostics.emptyModules.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = ext.muted
                        )
                    }
                }
            }
        }
    }
}
