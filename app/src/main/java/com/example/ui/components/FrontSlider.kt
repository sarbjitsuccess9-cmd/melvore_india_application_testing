package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HeroSlide
import com.example.model.OccasionTheme
import com.example.model.PromoBanner
import com.example.ui.design.AnimatedSvg
import com.example.ui.theme.LocalDesignKit
import com.example.ui.theme.LocalFashionWorldTheme
import com.example.ui.theme.LocalMelvoreApiBaseUrl
import com.example.ui.theme.LocalMelvoreDesignManifest
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.ensureContrast
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

data class FrontSliderItem(
    val id: String,
    val eyebrow: String,
    val title: String,
    val subtitle: String,
    val ctaLabel: String,
    val ctaLink: String,
    val mediaUrl: String?,
    val isSvgBanner: Boolean = false,
    val isOccasionSlide: Boolean = false
)

/**
 * Quick-commerce top Front Slider combining:
 * 1. Active festival / occasion banner (SMIL animated SVG kit or festival artwork)
 * 2. Backend hero slides (`homeFeed.heroSlides` / `design.heroSlides`)
 * 3. Backend promo banners (`homeFeed.promoBanners`)
 *
 * Features horizontal parallax, manifest-driven auto-play interval, Liquid Glass caption
 * bar, semantic `imageOverlay` scrim (no mixed-black leaks), and reduced-motion support.
 */
@Composable
fun FrontSlider(
    activeOccasion: OccasionTheme?,
    heroSlides: List<HeroSlide>,
    promoBanners: List<PromoBanner>,
    reducedMotion: Boolean = false,
    height: Dp = 248.dp,
    onSlideClick: (ctaLink: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val manifest = LocalMelvoreDesignManifest.current
    val resolvedTheme = LocalFashionWorldTheme.current
    val designKit = LocalDesignKit.current
    val apiBase = designKit.assetBaseUrl.ifBlank { LocalMelvoreApiBaseUrl.current }
    val surfaces = LocalMelvoreSurfaces.current

    val slides = remember(activeOccasion, heroSlides, promoBanners, manifest, apiBase) {
        buildList {
            if (activeOccasion != null) {
                val occKit = manifest.occasionOverrides.occasionKit(activeOccasion.slug)
                val svgBanner = occKit?.svg?.banner?.takeIf { it.isNotBlank() }?.let {
                    RemoteDesignRegistry.resolveAssetUrl(it, apiBase)
                }
                val imgBanner = activeOccasion.bannerImageUrl?.takeIf { it.isNotBlank() }?.let {
                    RemoteDesignRegistry.resolveAssetUrl(it, apiBase)
                }
                add(
                    FrontSliderItem(
                        id = "occasion_${activeOccasion.slug}",
                        eyebrow = activeOccasion.eyebrow.ifBlank { activeOccasion.name.uppercase() },
                        title = activeOccasion.headline.ifBlank { activeOccasion.name },
                        subtitle = activeOccasion.subtitle,
                        ctaLabel = activeOccasion.ctaText.ifBlank { "Shop Edit" },
                        ctaLink = activeOccasion.ctaCollectionSlug.ifBlank { activeOccasion.slug },
                        mediaUrl = imgBanner ?: svgBanner,
                        isSvgBanner = (imgBanner == null && svgBanner != null),
                        isOccasionSlide = true
                    )
                )
            }
            heroSlides.forEachIndexed { idx, slide ->
                add(
                    FrontSliderItem(
                        id = "hero_${slide.id}_$idx",
                        eyebrow = slide.subtitle.takeIf { it.length <= 28 }?.uppercase()
                            ?: manifest.navigation.topBarSubtitle.ifBlank { "FEATURED DROP" },
                        title = slide.title.ifBlank { manifest.navigation.topBarTitle },
                        subtitle = slide.subtitle,
                        ctaLabel = slide.ctaText.ifBlank { "Explore" },
                        ctaLink = slide.ctaLink,
                        mediaUrl = RemoteDesignRegistry.resolveAssetUrl(slide.mobileImageUrl ?: slide.imageUrl, apiBase)
                            ?: slide.mobileImageUrl ?: slide.imageUrl
                    )
                )
            }
            promoBanners.take(4).forEachIndexed { idx, promo ->
                add(
                    FrontSliderItem(
                        id = "promo_${promo.id}_$idx",
                        eyebrow = promo.tag.ifBlank { "LIMITED OFFER" }.uppercase(),
                        title = promo.title,
                        subtitle = promo.subtitle,
                        ctaLabel = promo.ctaText.ifBlank { "Shop Now" },
                        ctaLink = promo.ctaLink,
                        mediaUrl = RemoteDesignRegistry.resolveAssetUrl(promo.imageUrl, apiBase) ?: promo.imageUrl
                    )
                )
            }
        }
    }

    if (slides.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val autoPlayMs = manifest.tokens.motion.carouselIntervalMs.coerceIn(3000, 9000).toLong()
    val parallaxEnabled = manifest.tokens.motion.heroParallax && !reducedMotion

    LaunchedEffect(slides.size, isDragged, reducedMotion, autoPlayMs) {
        if (slides.size > 1 && !isDragged && !reducedMotion) {
            while (true) {
                delay(autoPlayMs)
                val next = (pagerState.currentPage + 1) % slides.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    val heroRadius = manifest.tokens.radii.heroDp.coerceIn(8, 22).dp
    val horizontalPad = manifest.tokens.spacing.screenHorizontalDp.coerceIn(12, 20).dp
    val activeWorldKit = resolvedTheme.worldSlug?.let { manifest.worldOverride(it)?.kit }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_front_slider")
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPad)
                .height(height)
        ) { page ->
            val item = slides[page]
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
            val cardShape = RoundedCornerShape(heroRadius)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(cardShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                surfaces.accent.copy(alpha = 0.22f),
                                surfaces.surfaceElevated
                            )
                        )
                    )
                    .clickable { onSlideClick(item.ctaLink) }
                    .testTag("front_slider_item_${item.id}")
            ) {
                val mediaModifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (parallaxEnabled) {
                            translationX = pageOffset * 48f
                            val scale = 1f + (pageOffset.absoluteValue * 0.04f).coerceAtMost(0.06f)
                            scaleX = scale
                            scaleY = scale
                        }
                    }

                val isSvgUrl = item.isSvgBanner ||
                    item.mediaUrl?.substringBefore('?')?.endsWith(".svg", ignoreCase = true) == true
                if (!item.mediaUrl.isNullOrBlank()) {
                    if (isSvgUrl) {
                        AnimatedSvg(
                            url = item.mediaUrl,
                            contentDescription = item.title,
                            reducedMotion = reducedMotion,
                            contentScale = ContentScale.Crop,
                            modifier = mediaModifier
                        )
                    } else {
                        MelvoreNetworkImage(
                            url = item.mediaUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = mediaModifier
                        )
                    }
                }

                // Pure photo scrim using semantic imageOverlay (keeps white text readable over any photograph)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to surfaces.imageOverlay.copy(alpha = 0.14f),
                                0.45f to Color.Transparent,
                                1.0f to surfaces.imageOverlay.copy(alpha = 0.88f)
                            )
                        )
                )

                // Top-left festival/drop eyebrow pill
                if (item.eyebrow.isNotBlank()) {
                    Surface(
                        color = if (item.isOccasionSlide) surfaces.accent else surfaces.imageOverlay.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(manifest.tokens.radii.chipDp.coerceAtMost(999).dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            val pillFg = if (item.isOccasionSlide) surfaces.onAccent else surfaces.onImageOverlay
                            if (item.isOccasionSlide) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = pillFg,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = item.eyebrow,
                                style = MaterialTheme.typography.labelSmall,
                                color = pillFg,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = manifest.tokens.typography.letterSpacingEyebrow.coerceIn(0.6f, 2.0f).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Bottom Liquid Glass caption card
                LiquidGlassBox(
                    shape = RoundedCornerShape(14.dp),
                    tintColor = surfaces.imageOverlay.copy(alpha = 0.56f),
                    borderColor = surfaces.onImageOverlay.copy(alpha = 0.28f),
                    reducedMotion = reducedMotion,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = surfaces.onImageOverlay,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.subtitle.isNotBlank() && !item.subtitle.equals(item.title, ignoreCase = true)) {
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = surfaces.onImageOverlay.copy(alpha = 0.86f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        val ctaBg = surfaces.accent
                        val ctaFg = ensureContrast(surfaces.onAccent, ctaBg)
                        Surface(
                            color = ctaBg,
                            shape = RoundedCornerShape(manifest.tokens.radii.buttonDp.coerceIn(6, 999).dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = item.ctaLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ctaFg,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = ctaFg,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (slides.size > 1) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                RemoteSvgPagerIndicator(
                    pageCount = slides.size,
                    currentPage = pagerState.currentPage,
                    pagerSvgPath = activeWorldKit?.svg?.pager,
                    activeColor = surfaces.accent,
                    inactiveColor = surfaces.border
                )
            }
        }
    }
}
