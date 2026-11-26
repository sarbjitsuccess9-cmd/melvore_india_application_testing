package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Category
import com.example.model.CollectionItem
import com.example.model.DeliveryServiceability
import com.example.model.FashionWorldTheme
import com.example.model.MarqueeItem
import com.example.model.OccasionTheme
import com.example.model.Product
import com.example.model.SmartOffer
import com.example.model.TrendingTag
import com.example.ui.design.AnimatedSvg
import com.example.ui.theme.LocalDesignKit
import com.example.ui.theme.LocalFashionWorldTheme
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreDesignManifest
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.LocalWorld
import com.example.ui.theme.ensureContrast
import com.example.ui.theme.parseHexColor
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

data class HomeOccasionTabItem(
    val slug: String?,
    val label: String,
    val accentHex: String? = null,
    val badge: String = ""
)

@Composable
fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Sticky quick-commerce header inspired by Blinkit / Swiggy Instamart / Zepto:
 * - Delivery ETA pill (strictly driven by backend deliveryCheck / siteSettings)
 * - Address/pincode selector
 * - Active Fashion World picker trigger pill
 * - Wishlist & Profile actions
 * - Full-width rounded search bar with rotating placeholder terms from backend trending tags
 */
@Composable
fun QuickCommerceStickyHeader(
    savedPincode: String,
    savedCity: String,
    deliveryCheck: DeliveryServiceability?,
    freeShippingMinimum: Double,
    activeWorldLabel: String,
    activeWorldAccentHex: String,
    wishlistCount: Int,
    isLoggedIn: Boolean,
    trendingTags: List<TrendingTag>,
    onPincodeClick: () -> Unit,
    onWorldPickerClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val manifest = LocalMelvoreDesignManifest.current
    val ext = LocalMelvoreColors.current
    val worldAccent = parseHexColor(activeWorldAccentHex, MaterialTheme.colorScheme.primary)
    val onHeaderColor = MaterialTheme.colorScheme.onBackground

    // Build ETA text strictly from backend data (never invent fake delivery minutes)
    val etaLabel = remember(deliveryCheck, savedPincode, freeShippingMinimum) {
        when {
            deliveryCheck != null && deliveryCheck.estimatedDaysMin > 0 && deliveryCheck.estimatedDaysMax > 0 -> {
                if (deliveryCheck.estimatedDaysMin == deliveryCheck.estimatedDaysMax) {
                    "${deliveryCheck.estimatedDaysMin}-day delivery"
                } else {
                    "${deliveryCheck.estimatedDaysMin}–${deliveryCheck.estimatedDaysMax} days"
                }
            }
            deliveryCheck != null && deliveryCheck.deliveryMessage.isNotBlank() -> {
                deliveryCheck.deliveryMessage.take(26)
            }
            savedPincode.isNotBlank() -> "Delivering to $savedPincode"
            freeShippingMinimum > 0.0 -> "Free shipping ₹${freeShippingMinimum.toInt()}+"
            else -> "Check delivery ETA"
        }
    }

    val locationSubtitle = remember(savedCity, savedPincode) {
        when {
            savedCity.isNotBlank() && savedPincode.isNotBlank() -> "$savedCity • $savedPincode"
            savedPincode.isNotBlank() -> "PIN $savedPincode"
            savedCity.isNotBlank() -> savedCity
            else -> "Select delivery location"
        }
    }

    val rotatingPlaceholders = remember(trendingTags, manifest.components.searchBar.placeholder) {
        val fromTags = trendingTags.mapNotNull { tag ->
            tag.name.trim().takeIf { it.isNotBlank() }?.let { "Search \"$it\"" }
        }
        val base = manifest.components.searchBar.placeholder.takeIf { it.isNotBlank() }
            ?: "Search styles, kurtas, sneakers..."
        (listOf(base) + fromTags).distinct()
    }

    var placeholderIdx by remember(rotatingPlaceholders) { mutableIntStateOf(0) }
    LaunchedEffect(rotatingPlaceholders) {
        if (rotatingPlaceholders.size > 1) {
            while (true) {
                delay(2800L)
                placeholderIdx = (placeholderIdx + 1) % rotatingPlaceholders.size
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Delivery ETA + Address/Pincode selector
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onPincodeClick)
                    .padding(vertical = 2.dp)
                    .testTag("qc_location_selector"),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = etaLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = onHeaderColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = locationSubtitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = onHeaderColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Change delivery location",
                        tint = onHeaderColor.copy(alpha = 0.78f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Right: World Picker pill + Wishlist + Profile
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = onWorldPickerClick,
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, worldAccent.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("qc_world_picker_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(worldAccent)
                        )
                        Text(
                            text = activeWorldLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = "Switch Fashion World",
                            tint = worldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onWishlistClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("qc_wishlist_button")
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = if (wishlistCount > 0) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (wishlistCount > 0) MaterialTheme.colorScheme.primary else onHeaderColor
                        )
                        if (wishlistCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onProfileClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("qc_profile_button")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, ext.borderSubtle),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isLoggedIn) Icons.Filled.Person else Icons.Outlined.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Liquid Glass Rounded Quick-Commerce Search Bar with rotating placeholder
        val searchRadius = manifest.components.searchBar.cornerRadiusDp.coerceIn(10, 28).dp
        LiquidGlassBar(
            onClick = onSearchClick,
            shape = RoundedCornerShape(searchRadius),
            elevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("qc_search_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search catalog",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                AnimatedContent(
                    targetState = rotatingPlaceholders.getOrElse(placeholderIdx) { rotatingPlaceholders.first() },
                    transitionSpec = {
                        (fadeIn(tween(240)) + slideInVertically { it / 2 })
                            .togetherWith(fadeOut(tween(200)) + slideOutVertically { -it / 2 })
                    },
                    label = "searchPlaceholderAnim",
                    modifier = Modifier.weight(1f)
                ) { hint ->
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Scrollable occasion / category tab strip under the search bar.
 * Selecting a tab cross-fades the top backdrop gradient over 300ms.
 */
@Composable
fun QuickCommerceOccasionTabStrip(
    tabs: List<HomeOccasionTabItem>,
    selectedSlug: String?,
    onSelectTab: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val manifest = LocalMelvoreDesignManifest.current
    val duration = manifest.tokens.motion.durationMs.coerceIn(150, 500)
    val ext = LocalMelvoreColors.current

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("qc_occasion_tab_strip"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs, key = { it.slug ?: "all" }) { tab ->
            val isSelected = (tab.slug == null && selectedSlug == null) ||
                (tab.slug != null && tab.slug.equals(selectedSlug, ignoreCase = true))
            val tabAccent = parseHexColor(tab.accentHex, MaterialTheme.colorScheme.primary)
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) tabAccent else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                animationSpec = tween(duration),
                label = "occasionTabBg"
            )
            val tabSurfaces = LocalMelvoreSurfaces.current
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    ensureContrast(tabSurfaces.onAccent, tabAccent)
                } else {
                    tabSurfaces.onSurface
                },
                animationSpec = tween(duration),
                label = "occasionTabContent"
            )

            Surface(
                onClick = { onSelectTab(tab.slug) },
                shape = RoundedCornerShape(999.dp),
                color = bgColor,
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) tabAccent else ext.borderSubtle
                ),
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .testTag("qc_tab_${tab.slug ?: "all"}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (tab.slug != null) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1
                    )
                    if (tab.badge.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = contentColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = tab.badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = contentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Festival / Occasion Hero Banner with optional SMIL animated SVG (`AnimatedSvg`) or image,
 * CTA button, and budgeted ambient decorative particles capped by manifest limits.
 */
@Composable
fun QuickCommerceFestivalHeroBanner(
    occasion: OccasionTheme?,
    fallbackTitle: String,
    fallbackSubtitle: String,
    fallbackCta: String,
    fallbackImageUrl: String?,
    reducedMotion: Boolean,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val manifest = LocalMelvoreDesignManifest.current
    val designKit = LocalDesignKit.current
    val worldKit = LocalWorld.current
    val worldTheme = LocalFashionWorldTheme.current
    val systemReducedMotion = rememberSystemReducedMotion()
    val disableMotion = reducedMotion || (manifest.occasionOverrides.respectReducedMotion && systemReducedMotion)

    val occKit = remember(manifest, occasion?.slug) {
        occasion?.slug?.let { manifest.occasionOverrides.occasionKit(it) }
    }

    val accentColor = parseHexColor(
        occasion?.accentColor?.takeIf { it.isNotBlank() }
            ?: occKit?.colors?.accent?.takeIf { it.isNotBlank() }
            ?: worldTheme.accentHex,
        MaterialTheme.colorScheme.primary
    )
    val surfaceTint = parseHexColor(
        occasion?.backgroundColor?.takeIf { it.isNotBlank() }
            ?: occKit?.colors?.background?.takeIf { it.isNotBlank() }
            ?: worldTheme.surfaceHex,
        MaterialTheme.colorScheme.surfaceVariant
    )

    val title = occasion?.headline?.takeIf { it.isNotBlank() }
        ?: occasion?.name?.takeIf { it.isNotBlank() }
        ?: fallbackTitle
    val subtitle = occasion?.subtitle?.takeIf { it.isNotBlank() }
        ?: fallbackSubtitle
    val ctaText = occasion?.ctaText?.takeIf { it.isNotBlank() }
        ?: fallbackCta

    val svgBannerUrl = remember(occKit, worldKit, designKit) {
        val rawPath = occKit?.svg?.banner?.takeIf { it.isNotBlank() }
            ?: worldKit?.widgetPath("banner")
            ?: worldKit?.widgetPath("hero_art")
            ?: worldKit?.componentPath("banner")
        designKit.url(rawPath)
    }
    val rasterBannerUrl = remember(occasion, fallbackImageUrl, designKit) {
        val raw = occasion?.bannerImageUrl?.takeIf { it.isNotBlank() }
            ?: fallbackImageUrl
        designKit.url(raw) ?: raw
    }

    val radius = manifest.tokens.radii.cardDp.coerceIn(14, 24).dp
    val maxParticles = manifest.occasionOverrides.maxAmbientParticles.coerceIn(0, 18)

    Surface(
        shape = RoundedCornerShape(radius),
        color = surfaceTint,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("qc_festival_hero_banner")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.18f),
                            surfaceTint,
                            accentColor.copy(alpha = 0.10f)
                        )
                    )
                )
        ) {
            // Budgeted ambient decorative particles (never human characters, disabled on reduced motion)
            if (!disableMotion && maxParticles > 0 && (occasion != null || worldTheme.isWorldActive)) {
                val transition = rememberInfiniteTransition(label = "festivalParticles")
                val phase by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = (Math.PI * 2.0).toFloat(),
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 6000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "particlePhase"
                )
                Canvas(modifier = Modifier.matchParentSize()) {
                    val count = maxParticles.coerceAtMost(12)
                    for (i in 0 until count) {
                        val seed = (i + 1) * 0.6180339f
                        val x = ((seed * 3.1f + 0.08f * sin(phase + i)) % 1f) * size.width
                        val y = (((i + 0.5f) / count) + 0.05f * cos(phase + i * 1.3f)) * size.height
                        drawCircle(
                            color = accentColor.copy(alpha = 0.14f + (i % 3) * 0.05f),
                            radius = (3f + (i % 4) * 2.2f).dp.toPx(),
                            center = Offset(x.coerceIn(0f, size.width), y.coerceIn(0f, size.height))
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (occasion != null && occasion.name.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = accentColor.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = occasion.name.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    val heroSurfaces = LocalMelvoreSurfaces.current
                    Button(
                        onClick = onCtaClick,
                        shape = RemoteDesignRegistry.resolveButtonShape(manifest, worldTheme),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = ensureContrast(heroSurfaces.onAccent, accentColor)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("qc_hero_cta")
                    ) {
                        Text(
                            text = ctaText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Right visual: SMIL AnimatedSvg or raster banner artwork
                if (!svgBannerUrl.isNullOrBlank()) {
                    AnimatedSvg(
                        url = svgBannerUrl,
                        contentDescription = title,
                        reducedMotion = disableMotion,
                        modifier = Modifier
                            .size(112.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else if (!rasterBannerUrl.isNullOrBlank()) {
                    MelvoreNetworkImage(
                        url = rasterBannerUrl,
                        contentDescription = title,
                        modifier = Modifier
                            .size(112.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

/**
 * 3–4 column quick-commerce rounded square category tile grid driven by backend categories & collections.
 */
@Composable
fun QuickCommerceCategoryTileGrid(
    categories: List<Category>,
    collections: List<CollectionItem>,
    onCategoryClick: (Category) -> Unit,
    onCollectionClick: (CollectionItem) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val tileItems: List<Pair<String, Triple<String, String?, () -> Unit>>> = remember(categories, collections) {
        if (categories.isNotEmpty()) {
            categories.take(8).map { cat ->
                "cat_${cat.id}" to Triple(cat.name, cat.imageUrl.orEmpty().ifBlank { null }) { onCategoryClick(cat) }
            }
        } else {
            collections.take(8).map { col ->
                "col_${col.id}" to Triple(col.name, col.bannerUrl.orEmpty().ifBlank { null }) { onCollectionClick(col) }
            }
        }
    }
    if (tileItems.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("qc_category_tile_grid"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Shop by category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "See all",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSeeAllClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("qc_category_see_all")
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = if (maxWidth < 330.dp) 3 else 4
            val rows = tileItems.chunked(columns)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { (key, data) ->
                            val (label, imageUrl, onClick) = data
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(onClick = onClick)
                                    .testTag("qc_cat_tile_$key"),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                    border = BorderStroke(1.dp, ext.borderSubtle),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                ) {
                                    if (!imageUrl.isNullOrBlank()) {
                                        MelvoreNetworkImage(
                                            url = imageUrl,
                                            contentDescription = label,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label.take(2).uppercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                        repeat(columns - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quick-commerce Offer Strip sourced strictly from backend smartOffers, marqueeItems, and freeShippingMinimum.
 */
@Composable
fun QuickCommerceOfferStrip(
    smartOffers: List<SmartOffer>,
    marqueeItems: List<MarqueeItem>,
    freeShippingMinimum: Double,
    currency: String,
    onApplyOffer: (SmartOffer) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val hasOffers = smartOffers.isNotEmpty() || marqueeItems.isNotEmpty() || freeShippingMinimum > 0.0
    if (!hasOffers) return

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("qc_offer_strip"),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(smartOffers.take(4), key = { "offer_${it.offerId}" }) { offer ->
            Surface(
                onClick = { onApplyOffer(offer) },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.testTag("qc_smart_offer_${offer.offerId}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalOffer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = offer.title.ifBlank { offer.badgeText.ifBlank { "Special Offer" } },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        val sub = offer.couponCode.takeIf { it.isNotBlank() }?.let { "Use $it" }
                            ?: offer.subtitle
                        if (sub.isNotBlank()) {
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.muted,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        if (freeShippingMinimum > 0.0) {
            item(key = "free_ship_pill") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = LocalMelvoreSurfaces.current.success,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Free delivery above $currency${freeShippingMinimum.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        items(marqueeItems.take(4), key = { "marquee_${it.id}_${it.text.hashCode()}" }) { item ->
            if (item.text.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle)
                ) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Compact Quick-Commerce Product Card (Blinkit / Instamart / Zepto style):
 * - Crisp product thumbnail with discount % badge
 * - Product name (2 lines max)
 * - Price + strike-through MRP
 * - Green ADD button that morphs into a `– qty +` stepper with haptic feedback, wired directly to cart state.
 */
@Composable
fun QuickCommerceProductCard(
    product: Product,
    currency: String,
    cartQuantity: Int,
    onProductClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp? = null
) {
    val manifest = LocalMelvoreDesignManifest.current
    val worldTheme = LocalFashionWorldTheme.current
    val ext = LocalMelvoreColors.current
    val haptic = LocalHapticFeedback.current
    val cardRadius = RemoteDesignRegistry.resolveProductCardRadiusDp(manifest, worldTheme).coerceIn(10, 18).dp
    val discountPct = product.discountPercent

    Surface(
        shape = RoundedCornerShape(cardRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.borderSubtle),
        shadowElevation = 1.dp,
        modifier = modifier
            .then(if (cardWidth != null) Modifier.width(cardWidth) else Modifier.fillMaxWidth())
            .clip(RoundedCornerShape(cardRadius))
            .clickable(onClick = onProductClick)
            .testTag("qc_product_card_${product.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.92f)
                    .clip(RoundedCornerShape((cardRadius - 2.dp).coerceAtLeast(8.dp)))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                MelvoreNetworkImage(
                    url = product.imageUrl ?: product.galleryUrls.firstOrNull(),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                val cardSurfaces = LocalMelvoreSurfaces.current
                val addGreen = cardSurfaces.success
                val onAddGreen = ensureContrast(cardSurfaces.onAccent, addGreen)
                if (discountPct > 0) {
                    Surface(
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        color = addGreen,
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "$discountPct% OFF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = onAddGreen,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (product.brandName.isNotBlank()) {
                Text(
                    text = product.brandName,
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = product.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$currency${product.effectivePrice.toInt()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    if (product.hasDiscount && product.regularPrice > product.effectivePrice) {
                        Text(
                            text = "$currency${product.regularPrice.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.muted,
                            textDecoration = TextDecoration.LineThrough,
                            maxLines = 1
                        )
                    }
                }

                val stepSurfaces = LocalMelvoreSurfaces.current
                val stepGreen = stepSurfaces.success
                val onStepGreen = ensureContrast(stepSurfaces.onAccent, stepGreen)
                // Morphing ADD button <-> (- qty +) stepper
                AnimatedContent(
                    targetState = cartQuantity > 0,
                    transitionSpec = {
                        (fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.9f))
                            .togetherWith(fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.9f))
                    },
                    label = "addStepperMorph"
                ) { inCart ->
                    if (!inCart) {
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onIncrement()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = stepSurfaces.surface,
                            border = BorderStroke(1.2.dp, stepGreen),
                            modifier = Modifier
                                .heightIn(min = 34.dp)
                                .widthIn(min = 66.dp)
                                .testTag("qc_add_btn_${product.id}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "ADD",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = stepGreen
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = stepGreen,
                            modifier = Modifier
                                .heightIn(min = 34.dp)
                                .testTag("qc_stepper_${product.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onDecrement()
                                        }
                                        .testTag("qc_stepper_dec_${product.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Remove,
                                        contentDescription = "Decrease quantity",
                                        tint = onStepGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "$cartQuantity",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = onStepGreen,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onIncrement()
                                        }
                                        .testTag("qc_stepper_inc_${product.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Increase quantity",
                                        tint = onStepGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Horizontal product rail with section header and quick-commerce product cards.
 */
@Composable
fun QuickCommerceProductRail(
    title: String,
    subtitle: String? = null,
    products: List<Product>,
    currency: String,
    cartQuantityForProduct: (Int) -> Int,
    onProductClick: (Int) -> Unit,
    onIncrementCart: (Product) -> Unit,
    onDecrementCart: (Int) -> Unit,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) return
    val ext = LocalMelvoreColors.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (onSeeAllClick != null) {
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSeeAllClick)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(products, key = { it.id }) { product ->
                QuickCommerceProductCard(
                    product = product,
                    currency = currency,
                    cartQuantity = cartQuantityForProduct(product.id),
                    onProductClick = { onProductClick(product.id) },
                    onIncrement = { onIncrementCart(product) },
                    onDecrement = { onDecrementCart(product.id) },
                    cardWidth = 152.dp
                )
            }
        }
    }
}

/**
 * Floating "View cart" bar that slides up above the bottom navigation bar whenever the cart has items.
 */
@Composable
fun FloatingCartBar(
    visible: Boolean,
    itemCount: Int,
    subtotal: Double,
    currency: String,
    freeShippingMinimum: Double,
    onViewCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barSurfaces = LocalMelvoreSurfaces.current
    val cartGreen = barSurfaces.success
    val onCartGreen = ensureContrast(barSurfaces.onAccent, cartGreen)
    AnimatedVisibility(
        visible = visible && itemCount > 0,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(220)),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(180)),
        modifier = modifier
    ) {
        Surface(
            onClick = onViewCartClick,
            shape = RoundedCornerShape(16.dp),
            color = cartGreen,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .heightIn(min = 56.dp)
                .testTag("floating_view_cart_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = onCartGreen.copy(alpha = 0.2f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.ShoppingBag,
                                contentDescription = null,
                                tint = onCartGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "$itemCount ${if (itemCount == 1) "item" else "items"}  •  $currency${subtotal.toInt()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = onCartGreen,
                            maxLines = 1
                        )
                        val remaining = (freeShippingMinimum - subtotal).coerceAtLeast(0.0)
                        val hint = when {
                            freeShippingMinimum > 0.0 && remaining > 0.0 ->
                                "Add $currency${remaining.toInt()} more for free delivery"
                            freeShippingMinimum > 0.0 -> "Free delivery unlocked"
                            else -> "Tap to review your bag"
                        }
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.labelSmall,
                            color = onCartGreen.copy(alpha = 0.88f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "View cart",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = onCartGreen
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open bag",
                        tint = onCartGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bottom sheet World Picker listing Default + all 12 Fashion Worlds from backend
 * (`/api/themes/config.php` & bootstrap config), persisting the user's style layer choice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldPickerBottomSheet(
    worlds: List<FashionWorldTheme>,
    selectedWorldSlug: String?,
    onSelectWorld: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ext = LocalMelvoreColors.current
    val manifest = LocalMelvoreDesignManifest.current
    val surfaces = LocalMelvoreSurfaces.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfaces.surface,
        modifier = Modifier.testTag("world_picker_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Choose your Fashion World",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Style the app palette, accents, and drops with any Melvore world — festival themes automatically layer on top when live.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "world_default") {
                    WorldPickerRow(
                        name = "Melvore Classic",
                        tagline = "Editorial warm ivory & clean quick-commerce baseline",
                        primaryHex = manifest.tokens.colors.primary,
                        accentHex = manifest.tokens.colors.accent,
                        isSelected = selectedWorldSlug.isNullOrBlank(),
                        onClick = { onSelectWorld(null) },
                        testTag = "world_option_default"
                    )
                }

                items(worlds, key = { it.slug }) { world ->
                    WorldPickerRow(
                        name = world.name,
                        tagline = world.tagline.ifBlank { world.slug.uppercase() },
                        primaryHex = world.primaryColor,
                        accentHex = world.accentColor,
                        isSelected = selectedWorldSlug.equals(world.slug, ignoreCase = true),
                        onClick = { onSelectWorld(world.slug) },
                        testTag = "world_option_${world.slug}"
                    )
                }
            }
        }
    }
}

@Composable
private fun WorldPickerRow(
    name: String,
    tagline: String,
    primaryHex: String,
    accentHex: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    val primary = parseHexColor(primaryHex, MaterialTheme.colorScheme.primary)
    val accent = parseHexColor(accentHex, MaterialTheme.colorScheme.secondary)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) accent.copy(alpha = 0.12f) else surfaces.surfaceElevated,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) accent else ext.borderSubtle
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dual swatch pill
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, ext.borderSubtle, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 18.dp, height = 36.dp)
                        .background(primary)
                )
                Box(
                    modifier = Modifier
                        .size(width = 18.dp, height = 36.dp)
                        .background(accent)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (tagline.isNotBlank()) {
                    Text(
                        text = tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = accent,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = ensureContrast(surfaces.onAccent, accent),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
