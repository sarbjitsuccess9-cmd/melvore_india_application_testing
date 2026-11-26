package com.example.ui.screens

import android.content.Context
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BlogPost
import com.example.model.BrandItem
import com.example.model.Category
import com.example.model.CollectionItem
import com.example.model.HeroSlide
import com.example.model.MarqueeItem
import com.example.model.Product
import com.example.model.PromoBanner
import com.example.model.TrendingTag
import com.example.model.formatPrice
import com.example.model.parseHexColor
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.components.MelvoreWordmark
import com.example.ui.components.SlowEditorialZoomBox
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.MelvoreBrandFontFamily
import com.example.ui.theme.MelvoreSerifFontFamily
import com.example.ui.theme.ensureContrast
import kotlinx.coroutines.delay

@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        isSystemAnimatorEnabled(context)
    }
}

private fun isSystemAnimatorEnabled(context: Context): Boolean {
    return runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) != 0f
    }.getOrDefault(true)
}

@Composable
fun EditorialRevealSection(
    animationsEnabled: Boolean,
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var revealed by remember { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(animationsEnabled) {
        if (animationsEnabled && !revealed) {
            if (delayMillis > 0) delay(delayMillis.toLong())
            revealed = true
        } else if (!animationsEnabled) {
            revealed = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = if (animationsEnabled) 420 else 0, easing = FastOutSlowInEasing),
        label = "reveal_alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (revealed) 0f else 18f,
        animationSpec = tween(durationMillis = if (animationsEnabled) 420 else 0, easing = FastOutSlowInEasing),
        label = "reveal_offset"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = offsetY
        }
    ) {
        content()
    }
}

@Composable
fun EditorialQuickNavBar(
    categories: List<Category>,
    collections: List<CollectionItem>,
    onOpenMegaMenu: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectCollection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onOpenMegaMenu,
            shape = RoundedCornerShape(3.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .height(38.dp)
                .testTag("home_mega_menu_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.GridView, contentDescription = null, modifier = Modifier.size(15.dp))
                Text(
                    "DIRECTORY",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = MelvoreBrandFontFamily,
                    letterSpacing = 0.8.sp
                )
            }
        }

        collections.take(3).forEach { col ->
            Surface(
                onClick = { onSelectCollection(col.slug) },
                shape = RoundedCornerShape(3.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, ext.borderSubtle),
                modifier = Modifier.height(38.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = col.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }

        categories.take(6).forEach { cat ->
            Surface(
                onClick = { onSelectCategory(cat.slug) },
                shape = RoundedCornerShape(3.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle),
                modifier = Modifier.height(38.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 5.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    if (!cat.imageUrl.isNullOrBlank()) {
                        MelvoreNetworkImage(
                            url = cat.imageUrl,
                            contentDescription = cat.name,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                cat.name.take(1).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(cat.name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
}

/**
 * Full-bleed edge-to-edge editorial Hero Slider:
 * - Zero outer container radius, zero margin, zero borders
 * - Smooth horizontal slide transition via HorizontalPager
 * - Zero Ken Burns zoom, zero fade-out, zero arrows, zero slide counter
 * - Autoplay delay: 8000ms (8s), pausing during user interaction
 * - Pure image-only rendering when DB eyebrow/title/subtitle/ctaText are blank
 */
@Composable
fun EditorialHeroSlider(
    slides: List<HeroSlide>,
    animationsEnabled: Boolean,
    onSlideCta: (HeroSlide) -> Unit,
    modifier: Modifier = Modifier
) {
    if (slides.isEmpty()) return
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    val manifest = com.example.ui.theme.LocalMelvoreDesignManifest.current
    val worldTheme = com.example.ui.theme.LocalFashionWorldTheme.current
    val worldKit = remember(manifest, worldTheme) {
        com.example.ui.components.RemoteDesignRegistry.activeWorldKit(manifest, worldTheme)
    }
    val heroFallbackSvg = manifest.media.heroFallbackImage.takeIf { it.isNotBlank() }
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val fallbackDrawables = listOf(
        R.drawable.img_hero_look,
        R.drawable.img_hero_seoul,
        R.drawable.img_hero_atelier
    )

    LaunchedEffect(slides.size, pagerState.isScrollInProgress, animationsEnabled) {
        if (slides.size > 1 && !pagerState.isScrollInProgress) {
            delay(7500L)
            val nextPage = (pagerState.currentPage + 1) % slides.size
            if (animationsEnabled) {
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing)
                )
            } else {
                pagerState.scrollToPage(nextPage)
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 600.dp
        val heroHeight = if (isWide) 520.dp else 456.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .background(surfaces.surfaceElevated)
                .testTag("hero_slider_card")
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { idx ->
                val slide = slides[idx]
                val hasText = slide.hasAnyText
                val hasCta = slide.ctaText.isNotBlank()
                val showOverlayContent = hasText || hasCta
                val resolvedImage = slide.resolveImageUrl(isWideLayout = isWide)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(enabled = slide.ctaLink.isNotBlank() || hasCta) {
                            onSlideCta(slide)
                        }
                ) {
                    SlowEditorialZoomBox(
                        triggerKey = "${pagerState.currentPage}_$idx",
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (resolvedImage.isNullOrBlank() && heroFallbackSvg != null) {
                            com.example.ui.components.RemoteSvgSkinImage(
                                relativeOrAbsoluteUrl = heroFallbackSvg,
                                contentDescription = slide.title.ifBlank {
                                    slide.eyebrow.ifBlank { "Melvore Editorial Slide ${idx + 1}" }
                                },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                fallback = {
                                    MelvoreNetworkImage(
                                        url = null,
                                        contentDescription = slide.title.ifBlank { "Melvore Editorial Slide ${idx + 1}" },
                                        contentScale = ContentScale.Crop,
                                        fallbackDrawable = fallbackDrawables[idx % fallbackDrawables.size],
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            )
                        } else {
                            MelvoreNetworkImage(
                                url = resolvedImage,
                                contentDescription = slide.title.ifBlank {
                                    slide.eyebrow.ifBlank { "Melvore Editorial Slide ${idx + 1}" }
                                },
                                contentScale = ContentScale.Crop,
                                fallbackDrawable = if (resolvedImage.isNullOrBlank()) {
                                    fallbackDrawables[idx % fallbackDrawables.size]
                                } else null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    if (showOverlayContent) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to surfaces.imageOverlay.copy(alpha = 0.08f),
                                            0.48f to Color.Transparent,
                                            0.78f to surfaces.imageOverlay.copy(alpha = 0.62f),
                                            1.0f to surfaces.imageOverlay.copy(alpha = 0.88f)
                                        )
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (slide.eyebrow.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = surfaces.surface
                                ) {
                                    Text(
                                        text = slide.eyebrow.uppercase(),
                                        color = surfaces.onSurface,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (slide.title.isNotBlank()) {
                                Text(
                                    text = slide.title,
                                    color = surfaces.onImageOverlay,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontFamily = MelvoreSerifFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (slide.subtitle.isNotBlank()) {
                                Text(
                                    text = slide.subtitle,
                                    color = surfaces.onImageOverlay.copy(alpha = 0.90f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (hasCta) {
                                Button(
                                    onClick = { onSlideCta(slide) },
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = surfaces.surface,
                                        contentColor = surfaces.onSurface
                                    ),
                                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 11.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .padding(top = if (hasText) 4.dp else 0.dp)
                                        .testTag("hero_cta_button")
                                ) {
                                    Text(
                                        slide.ctaText.uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontFamily = MelvoreBrandFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.1.sp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (slides.size > 1) {
                com.example.ui.components.RemoteSvgPagerIndicator(
                    pageCount = slides.size,
                    currentPage = pagerState.currentPage,
                    pagerSvgPath = worldKit?.svg?.pager,
                    activeColor = surfaces.onImageOverlay,
                    inactiveColor = surfaces.onImageOverlay.copy(alpha = 0.38f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )
            }
        }
    }
}

/**
 * Brand Discovery Slider directly below the Hero Slider:
 * - Displays active brands fetched dynamically from the database (`brands` / `homepage_brands`)
 * - Renders brand logos in refined monochrome + displays accurate connected brand count (0 if none yet)
 * - Automatic continuous horizontal ticker with seamless manual scroll pause/resume
 */
@Composable
fun BrandDiscoverySlider(
    brands: List<BrandItem>,
    animationsEnabled: Boolean,
    onSelectBrand: (BrandItem) -> Unit,
    onBrowseAllBrands: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val brandListState = rememberLazyListState()
    val isDragged by brandListState.interactionSource.collectIsDraggedAsState()
    val monochromeFilter = remember {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
    }

    LaunchedEffect(brands.size, animationsEnabled, isDragged, brandListState.isScrollInProgress) {
        if (brands.size > 1 && animationsEnabled && !isDragged && !brandListState.isScrollInProgress) {
            delay(900L)
            while (true) {
                val scrolled = brandListState.scrollBy(1.1f)
                if (scrolled == 0f) {
                    delay(1200L)
                    brandListState.animateScrollToItem(0)
                    delay(900L)
                }
                delay(16L)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(BorderStroke(1.dp, ext.borderSubtle))
            .padding(vertical = 16.dp)
            .testTag("brand_discovery_slider"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "INTERNATIONAL MULTI-BRAND HOUSE",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.4.sp,
                    color = ext.muted,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Connected Brands",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = MelvoreBrandFontFamily
                )
            }

            Surface(
                onClick = onBrowseAllBrands,
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle),
                modifier = Modifier.testTag("brand_count_badge")
            ) {
                Text(
                    text = if (brands.isNotEmpty()) "${brands.size} CONNECTED →" else "EXPLORE CATALOG →",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        if (brands.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBrowseAllBrands)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Public,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = ext.muted
                )
                Text(
                    text = "0 active connected brands published yet · Tap to browse curated pieces",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted
                )
            }
        } else {
            LazyRow(
                state = brandListState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(brands, key = { "brand_${it.id}_${it.slug}" }) { brand ->
                    BrandDiscoveryTile(
                        brand = brand,
                        monochromeFilter = monochromeFilter,
                        onClick = { onSelectBrand(brand) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandDiscoveryTile(
    brand: BrandItem,
    monochromeFilter: ColorFilter,
    onClick: () -> Unit
) {
    val ext = LocalMelvoreColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(160),
        label = "brand_tile_press"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(3.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.borderSubtle),
        modifier = Modifier
            .width(156.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag("brand_tile_${brand.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))
                    .border(0.75.dp, ext.borderSubtle, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!brand.logoUrl.isNullOrBlank()) {
                    MelvoreNetworkImage(
                        url = brand.logoUrl,
                        contentDescription = brand.name,
                        contentScale = ContentScale.Fit,
                        colorFilter = monochromeFilter,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                } else {
                    Text(
                        text = brand.name.take(2).uppercase(),
                        fontFamily = MelvoreBrandFontFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = brand.name.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = MelvoreBrandFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = when {
                    brand.originCountry.isNotBlank() -> brand.originCountry.uppercase()
                    brand.isImported -> "IMPORTED"
                    brand.productCount > 0 -> "${brand.productCount} PIECES"
                    else -> "HOUSE LABEL"
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.muted,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MonochromeValueRibbon(
    items: List<MarqueeItem>,
    onItemClick: (MarqueeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val surfaces = LocalMelvoreSurfaces.current
    Surface(
        shape = RoundedCornerShape(0.dp),
        color = surfaces.surfaceElevated,
        border = BorderStroke(0.5.dp, surfaces.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val icon = when {
                    item.icon.contains("gem") -> Icons.Outlined.Diamond
                    item.icon.contains("truck") || item.icon.contains("shipping") -> Icons.Outlined.LocalShipping
                    item.icon.contains("undo") -> Icons.Outlined.Refresh
                    item.icon.contains("star") -> Icons.Outlined.StarOutline
                    item.icon.contains("shield") -> Icons.Outlined.VerifiedUser
                    item.icon.contains("money") || item.icon.contains("wallet") -> Icons.Outlined.Wallet
                    else -> Icons.Outlined.AutoAwesome
                }
                Row(
                    modifier = Modifier
                        .clickable { onItemClick(item) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = surfaces.accent)
                    Text(
                        text = item.text.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.1.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = surfaces.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun AsymmetricalCampaignGrid(
    banners: List<PromoBanner>,
    spotlightProduct: Product?,
    currency: String,
    onBannerClick: (PromoBanner) -> Unit,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    if (banners.isEmpty()) return
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (banners.size >= 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CampaignPosterCard(
                    banner = banners[0],
                    accentColor = ext.campaignBerry,
                    onClick = { onBannerClick(banners[0]) },
                    modifier = Modifier
                        .weight(1.1f)
                        .height(324.dp)
                )

                Column(
                    modifier = Modifier.weight(0.9f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CampaignPosterCard(
                        banner = banners[1],
                        accentColor = surfaces.accent,
                        onClick = { onBannerClick(banners[1]) },
                        compact = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(spotlightProduct?.let { 212.dp } ?: 324.dp)
                    )

                    if (spotlightProduct != null) {
                        Surface(
                            onClick = { onProductClick(spotlightProduct) },
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, ext.borderSubtle.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(102.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MelvoreNetworkImage(
                                    url = spotlightProduct.imageUrl,
                                    contentDescription = spotlightProduct.name,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = spotlightProduct.brandName.ifBlank {
                                            spotlightProduct.badge ?: "FEATURED EDIT"
                                        }.uppercase(),
                                        color = ext.campaignCobalt,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = spotlightProduct.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatPrice(spotlightProduct.effectivePrice, currency),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = ext.muted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            CampaignPosterCard(
                banner = banners[0],
                accentColor = ext.campaignBerry,
                onClick = { onBannerClick(banners[0]) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(248.dp)
            )
        }

        banners.drop(2).forEach { extra ->
            CampaignPosterCard(
                banner = extra,
                accentColor = surfaces.accent,
                onClick = { onBannerClick(extra) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(206.dp)
            )
        }
    }
}

@Composable
private fun CampaignPosterCard(
    banner: PromoBanner,
    accentColor: Color,
    onClick: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val surfaces = LocalMelvoreSurfaces.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(180),
        label = "campaign_poster_scale"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(surfaces.surfaceElevated)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        MelvoreNetworkImage(
            url = banner.imageUrl,
            contentDescription = banner.title,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            surfaces.imageOverlay.copy(alpha = 0.14f),
                            surfaces.imageOverlay.copy(alpha = 0.86f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 14.dp else 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (banner.tag.isNotBlank()) {
                Surface(shape = RoundedCornerShape(2.dp), color = accentColor) {
                    Text(
                        text = banner.tag.uppercase(),
                        color = ensureContrast(surfaces.onAccent, accentColor),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Spacer(Modifier.height(1.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (banner.subtitle.isNotBlank()) {
                    Text(
                        text = banner.subtitle,
                        color = surfaces.onImageOverlay.copy(alpha = 0.86f),
                        style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (banner.title.isNotBlank()) {
                    Text(
                        text = banner.title,
                        color = surfaces.onImageOverlay,
                        style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                        fontFamily = MelvoreBrandFontFamily,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (banner.ctaText.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = surfaces.surface,
                        contentColor = surfaces.onSurface
                    ) {
                        Text(
                            text = "${banner.ctaText.uppercase()} →",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RunwayLookbookCard(
    product: Product,
    currency: String,
    isFeaturedCenter: Boolean,
    animationsEnabled: Boolean,
    onOpenProduct: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            !animationsEnabled -> 1.0f
            isPressed -> 0.98f
            isHovered -> 1.02f
            isFeaturedCenter -> 1.0f
            else -> 0.985f
        },
        animationSpec = tween(240),
        label = "runway_scale"
    )

    val brandOrOrigin = product.brandName.takeIf { it.isNotBlank() }
        ?: if (product.isImported) product.originCountry.ifBlank { "IMPORTED" } else product.category

    Box(
        modifier = modifier
            .width(238.dp)
            .aspectRatio(3f / 4.05f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(4.dp))
            .background(surfaces.surfaceElevated)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onOpenProduct)
            .testTag("drop_card_${product.id}")
    ) {
        MelvoreNetworkImage(
            url = product.imageUrl,
            contentDescription = product.name,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to surfaces.imageOverlay.copy(alpha = 0.22f),
                            0.48f to Color.Transparent,
                            1.0f to surfaces.imageOverlay.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!product.badge.isNullOrBlank()) {
                val badgeBg = if (product.hasDiscount) surfaces.sale else surfaces.imageOverlay.copy(alpha = 0.82f)
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = product.badge.uppercase(),
                        color = surfaces.onImageOverlay,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            if (product.inStock) {
                Surface(
                    onClick = onQuickAdd,
                    shape = RoundedCornerShape(2.dp),
                    color = surfaces.imageOverlay.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, surfaces.onImageOverlay.copy(alpha = 0.55f)),
                    modifier = Modifier.testTag("drop_quick_add_${product.id}")
                ) {
                    Text(
                        "+ ADD",
                        color = surfaces.onImageOverlay,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = brandOrOrigin.uppercase(),
                    color = surfaces.onImageOverlay.copy(alpha = 0.84f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.9.sp
                )
                if (product.colors.isNotEmpty()) {
                    product.colors.take(3).forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(parseHexColor(c.hex)))
                                .border(0.75.dp, surfaces.onImageOverlay.copy(alpha = 0.65f))
                        )
                    }
                }
            }

            Text(
                text = product.name,
                color = surfaces.onImageOverlay,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPrice(product.effectivePrice, currency),
                        color = surfaces.onImageOverlay,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (product.hasDiscount) {
                        Text(
                            text = "-${product.discountPercent}%",
                            color = ensureContrast(ext.emerald, surfaces.imageOverlay),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text("VIEW →", color = surfaces.onImageOverlay.copy(alpha = 0.88f), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * Luxury Top-Down Sliding Editorial Navigation Menu Overlay:
 * - Slides gracefully from top using AnimatedVisibility + BackHandler
 * - Large Tw Cen editorial primary navigation links
 * - Connected Brands monochrome strip with real DB brand count
 * - Categories directory + Collections + Trending tags + DB Campaign Visual Panel
 */
@Composable
fun TopEditorialNavigationOverlay(
    isOpen: Boolean,
    categories: List<Category>,
    brands: List<BrandItem>,
    collections: List<CollectionItem>,
    promoBanners: List<PromoBanner>,
    trendingTags: List<TrendingTag>,
    blogs: List<BlogPost>,
    onDismiss: () -> Unit,
    onNavigateAllShop: () -> Unit,
    onSelectBrand: (Int) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSelectCollection: (String) -> Unit,
    onSelectTagQuery: (String) -> Unit,
    onNavigateCategories: () -> Unit,
    onNavigateTrack: () -> Unit,
    onNavigateBlog: () -> Unit,
    onNavigateAbout: () -> Unit,
    onNavigateContact: () -> Unit,
    onNavigateFaq: () -> Unit
) {
    BackHandler(enabled = isOpen) {
        onDismiss()
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(durationMillis = 220)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
        ) + fadeOut(tween(durationMillis = 180))
    ) {
        val ext = LocalMelvoreColors.current
        val monochromeFilter = remember {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        }
        val featuredVisualBanner = promoBanners.firstOrNull()
        val featuredCollection = collections.firstOrNull()

        Surface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxSize()
                .testTag("top_editorial_menu")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Header Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MelvoreWordmark(fontSize = 24.sp)
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, ext.borderSubtle)
                        ) {
                            Text(
                                text = "GLOBAL EDIT",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.2.sp,
                                color = ext.muted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("top_menu_close_button")
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close editorial menu")
                    }
                }
                HorizontalDivider(color = ext.borderSubtle)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 1. Primary Luxury Editorial Links
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "INDEX",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.6.sp,
                            color = ext.muted
                        )
                        Spacer(Modifier.height(6.dp))
                        listOf(
                            Triple("All Collections", "MULTI-BRAND ARCHIVE") {
                                onDismiss()
                                onNavigateAllShop()
                            },
                            Triple("Categories", "${categories.size} DEPARTMENTS") {
                                onDismiss()
                                onNavigateCategories()
                            },
                            Triple("Editorial Journal", "${blogs.size} STORIES") {
                                onDismiss()
                                onNavigateBlog()
                            },
                            Triple("Track Order", "DISPATCH & SHIPPING") {
                                onDismiss()
                                onNavigateTrack()
                            },
                            Triple("Our Atelier", "HERITAGE & CRAFT") {
                                onDismiss()
                                onNavigateAbout()
                            }
                        ).forEach { (title, meta, action) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = action)
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    fontFamily = MelvoreBrandFontFamily,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = (-0.3).sp
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = meta,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = ext.muted
                                    )
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = ext.muted
                                    )
                                }
                            }
                            HorizontalDivider(color = ext.borderSubtle)
                        }
                    }

                    // 2. Campaign Visual Panel (DB-driven from banners / collections)
                    if (featuredVisualBanner != null || featuredCollection != null) {
                        val surfaces = LocalMelvoreSurfaces.current
                        val visualUrl = featuredVisualBanner?.imageUrl ?: featuredCollection?.bannerUrl
                        val visualLabel = featuredVisualBanner?.tag?.ifBlank { "CAMPAIGN" } ?: "FEATURED COLLECTION"
                        val visualTitle = featuredVisualBanner?.title ?: featuredCollection?.name.orEmpty()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(152.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(surfaces.surfaceElevated)
                                .clickable {
                                    onDismiss()
                                    if (featuredCollection != null) {
                                        onSelectCollection(featuredCollection.slug)
                                    } else {
                                        onNavigateAllShop()
                                    }
                                }
                        ) {
                            MelvoreNetworkImage(
                                url = visualUrl,
                                contentDescription = visualTitle,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                surfaces.imageOverlay.copy(alpha = 0.84f),
                                                surfaces.imageOverlay.copy(alpha = 0.25f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = visualLabel.uppercase(),
                                    color = surfaces.onImageOverlay.copy(alpha = 0.80f),
                                    style = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 1.4.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = visualTitle,
                                    color = surfaces.onImageOverlay,
                                    fontFamily = MelvoreBrandFontFamily,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    // 3. Connected Brands Section (Honest real DB count)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CONNECTED BRANDS",
                                style = MaterialTheme.typography.labelMedium,
                                letterSpacing = 1.4.sp,
                                color = ext.muted
                            )
                            Text(
                                text = "${brands.size} ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = ext.muted
                            )
                        }
                        if (brands.isEmpty()) {
                            Surface(
                                onClick = {
                                    onDismiss()
                                    onNavigateAllShop()
                                },
                                shape = RoundedCornerShape(3.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, ext.borderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Multi-brand label index syncing with storefront database · View full catalog →",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ext.muted,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                brands.forEach { brand ->
                                    BrandDiscoveryTile(
                                        brand = brand,
                                        monochromeFilter = monochromeFilter,
                                        onClick = {
                                            onDismiss()
                                            onSelectBrand(brand.id)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 4. Departments / Categories Grid
                    if (categories.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "SHOP BY CATEGORY",
                                style = MaterialTheme.typography.labelMedium,
                                letterSpacing = 1.4.sp,
                                color = ext.muted
                            )
                            categories.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { cat ->
                                        Surface(
                                            onClick = {
                                                onDismiss()
                                                onSelectCategory(cat.slug)
                                            },
                                            shape = RoundedCornerShape(3.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, ext.borderSubtle),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                MelvoreNetworkImage(
                                                    url = cat.imageUrl,
                                                    contentDescription = cat.name,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                )
                                                Column {
                                                    Text(
                                                        cat.name,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        "${cat.itemCount} styles",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = ext.muted,
                                                        fontFamily = FontFamily.Monospace
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

                    // 5. Collections & Trending
                    if (collections.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Outlined.Layers, contentDescription = null, modifier = Modifier.size(15.dp))
                                Text(
                                    "CURATED EDITS",
                                    style = MaterialTheme.typography.labelMedium,
                                    letterSpacing = 1.4.sp,
                                    color = ext.muted
                                )
                            }
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                collections.forEach { col ->
                                    Surface(
                                        onClick = {
                                            onDismiss()
                                            onSelectCollection(col.slug)
                                        },
                                        shape = RoundedCornerShape(2.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(
                                            text = col.name.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (trendingTags.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "TRENDING SEARCHES",
                                style = MaterialTheme.typography.labelMedium,
                                letterSpacing = 1.4.sp,
                                color = ext.muted
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                trendingTags.forEach { tag ->
                                    Surface(
                                        onClick = {
                                            onDismiss()
                                            onSelectTagQuery(tag.name)
                                        },
                                        shape = RoundedCornerShape(2.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, ext.borderSubtle)
                                    ) {
                                        Text(
                                            text = "#${tag.name}",
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 6. Client Concierge Footer Links
                    HorizontalDivider(color = ext.borderSubtle)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = {
                                onDismiss()
                                onNavigateFaq()
                            },
                            shape = RoundedCornerShape(3.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, ext.borderSubtle),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("FAQ & Care", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Surface(
                            onClick = {
                                onDismiss()
                                onNavigateContact()
                            },
                            shape = RoundedCornerShape(3.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, ext.borderSubtle),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Concierge", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditorialSectionHeader(
    eyebrow: String,
    title: String,
    actionLabel: String = "",
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (eyebrow.isNotBlank()) {
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.7.sp,
                    color = ext.goldAccent,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = MelvoreBrandFontFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (actionLabel.isNotBlank()) {
            Surface(
                onClick = onAction,
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.borderSubtle),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
