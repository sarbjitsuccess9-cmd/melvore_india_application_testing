package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.Product
import com.example.model.RunwayConfig
import com.example.model.User
import com.example.model.formatPrice
import com.example.ui.theme.LocalFashionWorldTheme
import com.example.ui.theme.LocalMelvoreApiBaseUrl
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreDesignManifest
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.MelvoreBrandFontFamily
import com.example.ui.theme.MelvoreSerifFontFamily
import com.example.ui.theme.ensureContrast
import com.example.ui.theme.parseHexColor

/**
 * Android-native Liquid Glass surface with background-aware translucent fill,
 * specular perimeter border highlight, subtle top sheen, and crisp contrast.
 * Delegates to [LiquidGlassBox] using [LocalMelvoreSurfaces] so light screens
 * never leak dark glass fills.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    darkGlass: Boolean = LocalMelvoreSurfaces.current.isDark,
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val surfaces = LocalMelvoreSurfaces.current
    val resolvedTint = if (darkGlass) {
        if (surfaces.isDark) surfaces.glassTint else surfaces.imageOverlay.copy(alpha = 0.72f)
    } else {
        surfaces.glassTint
    }
    val resolvedBorder = if (darkGlass) {
        surfaces.onImageOverlay.copy(alpha = 0.28f)
    } else {
        surfaces.border
    }
    LiquidGlassBox(
        modifier = modifier,
        shape = shape,
        tintColor = resolvedTint,
        borderColor = resolvedBorder,
        elevation = elevation,
        onClick = onClick,
        content = content
    )
}

@Composable
fun MelvoreWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    color: Color = MaterialTheme.colorScheme.onBackground
) {
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontFamily = MelvoreBrandFontFamily,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-0.4).sp
                )
            ) {
                append("Melv")
            }
            withStyle(
                SpanStyle(
                    fontFamily = MelvoreBrandFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )
            ) {
                append("ore")
            }
        },
        fontFamily = MelvoreBrandFontFamily,
        fontSize = fontSize,
        color = color,
        modifier = modifier
    )
}

@Composable
fun MelvoreNetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    colorFilter: ColorFilter? = null,
    @DrawableRes fallbackDrawable: Int? = null
) {
    val ext = LocalMelvoreColors.current
    if (url.isNullOrBlank()) {
        if (fallbackDrawable != null) {
            Image(
                painter = painterResource(fallbackDrawable),
                contentDescription = contentDescription,
                contentScale = contentScale,
                colorFilter = colorFilter,
                modifier = modifier
            )
        } else {
            Box(
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ImageNotSupported,
                    contentDescription = contentDescription,
                    tint = ext.muted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        colorFilter = colorFilter,
        modifier = modifier,
        onError = { errorState ->
            val reason = errorState.result.throwable.message
                ?: errorState.result.throwable.javaClass.simpleName
            com.example.network.NetworkDiagnosticsMonitor.recordImageFailure(
                url = url,
                reason = reason
            )
        },
        loading = {
            MelvoreSkeletonBox(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp)
            )
        },
        error = {
            if (fallbackDrawable != null) {
                Image(
                    painter = painterResource(fallbackDrawable),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ImageNotSupported,
                        contentDescription = contentDescription,
                        tint = ext.muted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    )
}

@Composable
fun MelvoreLoadingState(
    message: String = "Syncing with Melvore…",
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val manifest = LocalMelvoreDesignManifest.current
    val worldTheme = LocalFashionWorldTheme.current
    val worldKit = RemoteDesignRegistry.activeWorldKit(manifest, worldTheme)
    val animationsEnabled = rememberAnimationsEnabled()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RemoteSvgLoader(
            loaderSvgPath = worldKit?.svg?.loader,
            reducedMotion = !animationsEnabled,
            fallbackColor = MaterialTheme.colorScheme.primary
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = ext.muted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MelvoreErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val manifest = LocalMelvoreDesignManifest.current
    val worldTheme = LocalFashionWorldTheme.current
    val worldKit = RemoteDesignRegistry.activeWorldKit(manifest, worldTheme)
    val statesSpec = worldKit?.components?.states ?: manifest.components.states
    Surface(
        shape = RoundedCornerShape(RemoteDesignRegistry.resolveProductCardRadiusDp(manifest, worldTheme).dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.crimson.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = ext.crimson.copy(alpha = 0.14f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.CloudOff,
                        contentDescription = null,
                        tint = ext.crimson,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Text(
                text = "Connection Issue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = ext.muted,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                shape = RemoteDesignRegistry.resolveButtonShape(manifest, worldTheme),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("error_retry_button")
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(statesSpec.errorCtaLabel.ifBlank { "Retry" })
            }
        }
    }
}

@Composable
fun LiquidGlassMediaIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    LiquidGlassSurface(
        onClick = onClick,
        darkGlass = true,
        shape = CircleShape,
        elevation = 4.dp,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(48.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

/**
 * Compact Fashion-Editorial Header (ZERO Hamburger Menu).
 * Clean white surface on normal storefront, obsidian surface in Luxe.
 * Contains Melvore wordmark, greeting, pincode selector, wishlist badge, bag/cart badge, and profile button.
 */
@Composable
fun MelvoreTopBar(
    user: User?,
    savedPincode: String,
    wishlistCount: Int = 0,
    bagCount: Int = 0,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onPincodeClick: () -> Unit,
    onWishlistClick: () -> Unit = {},
    onBagClick: () -> Unit = {},
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val navSpec = LocalMelvoreDesignManifest.current.navigation
    val firstName = user?.name
        ?.trim()
        ?.split(" ")
        ?.firstOrNull { it.isNotBlank() }
        .orEmpty()
    val defaultSubtitle = navSpec.topBarSubtitle.takeIf {
        it.isNotBlank() && !it.equals("FASHION HOUSE", ignoreCase = true)
    } ?: "Delivering across India"
    val greetingLabel = if (firstName.isNotEmpty()) "Hi, $firstName" else defaultSubtitle
    val initials = user?.name
        ?.trim()
        ?.split(" ")
        ?.filter { it.isNotBlank() }
        ?.take(2)
        ?.joinToString("") { it.first().uppercaseChar().toString() }
        .orEmpty()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("melvore_top_bar")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (showBackButton) {
                        Surface(
                            onClick = onBackClick,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, ext.borderSubtle),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(48.dp)
                                .testTag("top_back_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (navSpec.topBarTitle.isBlank() || navSpec.topBarTitle.equals("MELVORE", ignoreCase = true)) {
                                MelvoreWordmark(
                                    fontSize = 21.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    text = navSpec.topBarTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = MelvoreBrandFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (com.example.ui.theme.LocalFashionWorldTheme.current.isLuxeIsolated) {
                                Text(
                                    text = "LUXE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.8.sp,
                                    color = ext.goldAccent
                                )
                            }
                        }
                        Text(
                            text = greetingLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = onPincodeClick,
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, ext.borderSubtle),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("top_pincode_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 9.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = stringResource(R.string.cd_pincode),
                                tint = LocalMelvoreSurfaces.current.let { s -> if (s.isDark) s.accent else s.onSurface },
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (savedPincode.isNotBlank()) savedPincode else "PIN",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val topSurfaces = LocalMelvoreSurfaces.current
                    Surface(
                        onClick = onWishlistClick,
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .testTag("top_wishlist_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            BadgedBox(
                                badge = {
                                    if (wishlistCount > 0) {
                                        Badge(
                                            containerColor = topSurfaces.sale,
                                            contentColor = ensureContrast(topSurfaces.onAccent, topSurfaces.sale)
                                        ) {
                                            Text(wishlistCount.coerceAtMost(99).toString(), fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FavoriteBorder,
                                    contentDescription = stringResource(R.string.cd_wishlist),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onBagClick,
                        shape = CircleShape,
                        color = Color.Transparent,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .testTag("top_bag_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            BadgedBox(
                                badge = {
                                    if (bagCount > 0) {
                                        val badgeBg = topSurfaces.accent
                                        Badge(
                                            containerColor = badgeBg,
                                            contentColor = ensureContrast(topSurfaces.onAccent, badgeBg)
                                        ) {
                                            Text(bagCount.coerceAtMost(99).toString(), fontSize = 9.sp)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ShoppingBag,
                                    contentDescription = stringResource(R.string.cd_cart),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onProfileClick,
                        shape = CircleShape,
                        color = if (initials.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, ext.borderSubtle),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .testTag("top_profile_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (initials.isNotEmpty()) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = stringResource(R.string.cd_profile),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ext.borderSubtle)
            )
        }
    }
}

/**
 * Floating Liquid Glass 5-Destination Bottom Navigation:
 * Home · Discover · Videos · Luxe (Elite) · Bag (No hamburger menu).
 */
@Composable
fun MelvoreBottomBar(
    currentSection: String,
    bagCount: Int,
    onHomeClick: () -> Unit,
    onDiscoverClick: () -> Unit,
    onVideosClick: () -> Unit,
    onEliteClick: () -> Unit,
    onBagClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    val manifest = LocalMelvoreDesignManifest.current
    val worldTheme = LocalFashionWorldTheme.current
    val apiBaseUrl = LocalMelvoreApiBaseUrl.current
    val worldKit = RemoteDesignRegistry.activeWorldKit(manifest, worldTheme)
    val navSpec = manifest.navigation
    val isVideoOverlayBar = currentSection == "videos"
    val useDarkGlass = surfaces.isDark || isVideoOverlayBar
    val barBg = if (isVideoOverlayBar && !surfaces.isDark) {
        surfaces.imageOverlay.copy(alpha = 0.86f)
    } else {
        surfaces.background.copy(alpha = 0.94f)
    }
    val activeColor = if (isVideoOverlayBar && !surfaces.isDark) {
        surfaces.onImageOverlay
    } else {
        ensureContrast(
            if (worldTheme.isWorldActive) parseHexColor(worldTheme.accentHex, surfaces.accent) else surfaces.accent,
            surfaces.surface
        )
    }
    val inactiveColor = if (isVideoOverlayBar && !surfaces.isDark) {
        surfaces.onImageOverlay.copy(alpha = 0.68f)
    } else {
        surfaces.onSurfaceMuted
    }
    val luxeGold = ext.goldAccent
    val effectiveVariant = worldKit?.navigation?.bottomBarVariant?.takeIf { it.isNotBlank() }
        ?: navSpec.bottomBarVariant
    val effectiveRadiusDp = worldKit?.navigation?.bottomBarRadiusDp ?: navSpec.bottomBarRadiusDp
    val isFloatingDock = effectiveVariant.equals("floating_dock", ignoreCase = true)
    val dockShape = RoundedCornerShape(
        when {
            effectiveVariant.equals("flat_editorial", ignoreCase = true) && effectiveRadiusDp == 0 -> 0.dp
            effectiveRadiusDp > 0 -> effectiveRadiusDp.coerceIn(0, 36).dp
            isFloatingDock -> 28.dp
            else -> 20.dp
        }
    )
    val designKit = com.example.ui.theme.LocalDesignKit.current
    val resolvedAssetBase = designKit.assetBaseUrl.ifBlank { apiBaseUrl }
    val tabs = remember(manifest, worldTheme, resolvedAssetBase) {
        RemoteDesignRegistry.resolveEffectiveNavTabs(manifest, worldTheme, resolvedAssetBase)
    }
    val navbarSvgPath = worldKit?.componentPath("navbar")
        ?: worldKit?.widgetPath("nav_live")
        ?: worldKit?.svg?.navbar?.takeIf { it.isNotBlank() }
    val navIndicatorSvgPath = worldKit?.componentPath("nav_indicator")
        ?: worldKit?.componentPath("nav_indicator_pill")
        ?: worldKit?.svg?.navIndicator?.takeIf { it.isNotBlank() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(barBg)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(
                horizontal = if (isFloatingDock) 14.dp else if (navbarSvgPath != null) 0.dp else 10.dp,
                vertical = if (isFloatingDock) 8.dp else if (navbarSvgPath != null) 0.dp else 6.dp
            )
    ) {
        LiquidGlassSurface(
            darkGlass = useDarkGlass,
            shape = dockShape,
            elevation = if (isFloatingDock) navSpec.bottomBarElevationDp.coerceIn(4, 16).dp else 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (navbarSvgPath != null) {
                    RemoteSvgSkinImage(
                        relativeOrAbsoluteUrl = navbarSvgPath,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.matchParentSize()
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    tabs.forEach { tab ->
                        val routeKey = tab.route
                        val selected = currentSection == routeKey
                        val onClickAction = when (routeKey) {
                            "home" -> onHomeClick
                            "discover" -> onDiscoverClick
                            "videos" -> onVideosClick
                            "elite" -> onEliteClick
                            "bag" -> onBagClick
                            else -> onHomeClick
                        }
                        val iconVec = RemoteDesignRegistry.iconForKey(
                            key = if (selected) tab.activeIconKey else tab.iconKey,
                            filled = selected
                        )
                        val tabSvgUrl = if (selected) tab.activeIconSvgUrl ?: tab.iconSvgUrl else tab.iconSvgUrl
                        val slotActiveColor = if (routeKey == "elite" && !worldTheme.isWorldActive) luxeGold else activeColor
                        val slotInactiveColor = if (routeKey == "elite" && useDarkGlass && !worldTheme.isWorldActive) luxeGold.copy(alpha = 0.72f) else inactiveColor
                        val slotPillBg = if (routeKey == "elite" && !worldTheme.isWorldActive) luxeGold.copy(alpha = 0.16f)
                        else if (useDarkGlass) surfaces.onImageOverlay.copy(alpha = 0.14f)
                        else surfaces.accent.copy(alpha = 0.10f)

                        BottomNavSlot(
                            selected = selected,
                            label = tab.label,
                            onClick = onClickAction,
                            activeColor = slotActiveColor,
                            inactiveColor = slotInactiveColor,
                            pillBg = slotPillBg,
                            navIndicatorSvgPath = navIndicatorSvgPath,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("nav_$routeKey")
                        ) {
                            if (routeKey == "bag" && navSpec.showBagBadge && bagCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = surfaces.sale,
                                            contentColor = ensureContrast(surfaces.onAccent, surfaces.sale)
                                        ) {
                                            Text(bagCount.toString(), fontSize = 9.sp)
                                        }
                                    }
                                ) {
                                    RemoteSvgTabIcon(
                                        svgUrl = tabSvgUrl,
                                        fallbackVector = iconVec,
                                        contentDescription = stringResource(R.string.cd_cart),
                                        size = 20.dp
                                    )
                                }
                            } else if (tab.badgeText.isNotBlank()) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = luxeGold,
                                            contentColor = ensureContrast(surfaces.onAccent, luxeGold)
                                        ) {
                                            Text(tab.badgeText.take(5), fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    RemoteSvgTabIcon(
                                        svgUrl = tabSvgUrl,
                                        fallbackVector = iconVec,
                                        contentDescription = tab.label,
                                        size = 20.dp
                                    )
                                }
                            } else {
                                RemoteSvgTabIcon(
                                    svgUrl = tabSvgUrl,
                                    fallbackVector = iconVec,
                                    contentDescription = tab.label,
                                    size = 20.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavSlot(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    pillBg: Color,
    navIndicatorSvgPath: String? = null,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 44.dp else 28.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "nav_pill_width"
    )
    val resolvedColor by animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        animationSpec = tween(200),
        label = "nav_slot_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(pillWidth)
                .height(26.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (selected && navIndicatorSvgPath == null) pillBg else Color.Transparent)
        ) {
            if (selected && !navIndicatorSvgPath.isNullOrBlank()) {
                RemoteSvgSkinImage(
                    relativeOrAbsoluteUrl = navIndicatorSvgPath,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize()
                )
            }
            CompositionLocalProvider(LocalContentColor provides resolvedColor) {
                icon()
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = resolvedColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Editorial Fashion Product Card:
 * Large 3:4 portrait apparel photography, floating Liquid Glass wishlist toggle,
 * brand eyebrow, clean title, monospace pricing + real discount, size preview strip, and tactile quick-add.
 */
@Composable
fun ProductCard(
    product: Product,
    currency: String,
    isWishlisted: Boolean,
    onProductClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onAddToCartClick: () -> Unit,
    modifier: Modifier = Modifier,
    runwayConfig: RunwayConfig? = null
) {
    val ext = LocalMelvoreColors.current
    val worldTheme = LocalFashionWorldTheme.current
    val manifest = LocalMelvoreDesignManifest.current
    val worldKit = RemoteDesignRegistry.activeWorldKit(manifest, worldTheme)
    val cardSpec = worldKit?.components?.productCard ?: manifest.components.productCard
    val cardRadiusDp = RemoteDesignRegistry.resolveProductCardRadiusDp(manifest, worldTheme)
    val imageAspect = RemoteDesignRegistry.resolveProductCardAspectRatio(manifest)
    val runwayAccent = remember(worldTheme.accentHex, ext.goldAccent) {
        parseHexColor(worldTheme.accentHex.takeIf { worldTheme.isWorldActive }, ext.goldAccent)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.982f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "product_card_press"
    )
    val imageScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 1.015f
            isHovered -> 1.035f
            else -> 1.0f
        },
        animationSpec = tween(durationMillis = 240),
        label = "product_card_image_scale"
    )

    val badgeLabel = when (product.badge?.uppercase()) {
        "NEW" -> "NEW"
        "HOT" -> "POPULAR"
        "BESTSELLER" -> "BESTSELLER"
        "EXCLUSIVE" -> "ATELIER"
        "LUXURY" -> "LUXURY"
        "TREND" -> "TRENDING"
        else -> product.badge?.takeIf { it.isNotBlank() }?.uppercase()
    }
    val brandTag = product.brandName.takeIf { it.isNotBlank() }
        ?: if (product.isImported) product.originCountry.ifBlank { "IMPORTED" } else ""

    Surface(
        shape = RoundedCornerShape(cardRadiusDp.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (cardSpec.variant == "gallery_bordered") 1.dp else 0.5.dp, ext.borderSubtle.copy(alpha = 0.7f)),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .testTag("product_card_${product.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(imageAspect)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .editorialRunwayFrame(runwayConfig = runwayConfig, accentColor = runwayAccent)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onProductClick
                    )
            ) {
                MelvoreNetworkImage(
                    url = product.imageUrl,
                    contentDescription = product.name,
                    fallbackDrawable = R.drawable.img_biker_jacket,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = imageScale
                            scaleY = imageScale
                        }
                )
                RemoteSvgCardFrameOverlay(
                    cardFrameSvgPath = worldKit?.svg?.cardFrame,
                    modifier = Modifier.matchParentSize()
                )

                val cardSurfaces = LocalMelvoreSurfaces.current
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    if (product.hasDiscount) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = cardSurfaces.sale
                        ) {
                            Text(
                                text = "${product.discountPercent}% OFF",
                                color = ensureContrast(cardSurfaces.onAccent, cardSurfaces.sale),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    } else if (badgeLabel != null) {
                        val badgeBg = if (cardSurfaces.isDark) cardSurfaces.accent else cardSurfaces.imageOverlay.copy(alpha = 0.86f)
                        val badgeFg = if (cardSurfaces.isDark) ensureContrast(cardSurfaces.onAccent, badgeBg) else cardSurfaces.onImageOverlay
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = badgeBg
                        ) {
                            Text(
                                text = badgeLabel,
                                color = badgeFg,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (product.isSponsored) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = cardSurfaces.imageOverlay.copy(alpha = 0.72f)
                        ) {
                            Text(
                                text = "AD",
                                color = cardSurfaces.onImageOverlay.copy(alpha = 0.92f),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                WishlistHeartOverlayButton(
                    isWishlisted = isWishlisted,
                    onClick = onWishlistClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .minimumInteractiveComponentSize()
                        .size(48.dp)
                        .testTag("wishlist_btn_${product.id}")
                ) {
                    LiquidGlassSurface(
                        darkGlass = true,
                        shape = CircleShape,
                        elevation = 2.dp,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isWishlisted) "Remove from wishlist" else "Add to wishlist",
                                tint = if (isWishlisted) cardSurfaces.sale else cardSurfaces.onImageOverlay,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (product.expressDelivery) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ext.electricBlue
                        ) {
                            Text(
                                text = "EXPRESS",
                                color = ensureContrast(cardSurfaces.onAccent, ext.electricBlue),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }

                    if (cardSpec.showRating && product.rating > 0.0 && product.reviewCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = cardSurfaces.surfaceElevated.copy(alpha = 0.94f)
                        ) {
                            Text(
                                text = "${"%.1f".format(product.rating)} ★ (${product.reviewCount})",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = cardSurfaces.onSurface,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(
                    horizontal = manifest.tokens.spacing.cardPaddingDp.coerceIn(8, 16).dp,
                    vertical = manifest.tokens.spacing.cardPaddingDp.coerceIn(8, 16).dp
                ),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val infoSurfaces = LocalMelvoreSurfaces.current
                if (cardSpec.showBrand) {
                    Text(
                        text = if (brandTag.isNotBlank()) brandTag.uppercase() else product.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (infoSurfaces.isDark) infoSurfaces.accent else infoSurfaces.onSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onProductClick)
                )

                if (product.sizes.isNotEmpty()) {
                    Text(
                        text = "Sizes: ${product.sizes.take(4).joinToString(" · ")}${if (product.sizes.size > 4) " +" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = ext.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = formatPrice(product.effectivePrice, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (product.hasDiscount) {
                            Text(
                                text = formatPrice(product.regularPrice, currency),
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = ext.muted,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }

                    if (cardSpec.showQuickAdd) {
                        PremiumQuickAddButton(
                            inStock = product.inStock,
                            productName = product.name,
                            onAddClick = onAddToCartClick,
                            compact = true,
                            modifier = Modifier.testTag("add_to_cart_${product.id}")
                        )
                    }
                }
            }
        }
    }
}

/**
 * Large Full-Width Editorial Product Spotlight Card for mixing high-fashion photography
 * with immediate shoppability on Home, Discover, and Luxe.
 */
@Composable
fun ProductEditorialSpotlightCard(
    eyebrow: String,
    product: Product,
    currency: String,
    isWishlisted: Boolean,
    onOpenProduct: () -> Unit,
    onWishlistClick: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
    runwayConfig: RunwayConfig? = null
) {
    val ext = LocalMelvoreColors.current
    val worldTheme = LocalFashionWorldTheme.current
    val runwayAccent = remember(worldTheme.accentHex, ext.goldAccent) {
        parseHexColor(worldTheme.accentHex.takeIf { worldTheme.isWorldActive }, ext.goldAccent)
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, ext.borderSubtle.copy(alpha = 0.7f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("spotlight_product_${product.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(0.48f)
                    .aspectRatio(3f / 4f)
                    .editorialRunwayFrame(runwayConfig = runwayConfig, accentColor = runwayAccent)
                    .clickable(onClick = onOpenProduct)
            ) {
                MelvoreNetworkImage(
                    url = product.imageUrl,
                    contentDescription = product.name,
                    fallbackDrawable = R.drawable.img_villain_jacket,
                    modifier = Modifier.fillMaxSize()
                )
                WishlistHeartOverlayButton(
                    isWishlisted = isWishlisted,
                    onClick = onWishlistClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .minimumInteractiveComponentSize()
                        .size(48.dp)
                ) {
                    LiquidGlassSurface(
                        darkGlass = true,
                        shape = CircleShape,
                        elevation = 2.dp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            val spotSurfaces = LocalMelvoreSurfaces.current
                            Icon(
                                imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Wishlist ${product.name}",
                                tint = if (isWishlisted) spotSurfaces.sale else spotSurfaces.onImageOverlay,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.52f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Bold,
                    color = ext.goldAccent
                )
                if (product.brandName.isNotBlank()) {
                    Text(
                        text = product.brandName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.muted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = MelvoreSerifFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onOpenProduct)
                )
                if (product.shortDescription.isNotBlank()) {
                    Text(
                        text = product.shortDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatPrice(product.effectivePrice, currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (product.hasDiscount) {
                        Text(
                            text = formatPrice(product.regularPrice, currency),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = ext.muted,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                PremiumQuickAddButton(
                    inStock = product.inStock,
                    productName = product.name,
                    onAddClick = onQuickAdd,
                    compact = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Refined kinetic editorial sweep around product photography when a world enables runway effects.
 * Preserves unobstructed garment photography while removing cartoonish standalone pixel cards.
 */
@Composable
private fun Modifier.editorialRunwayFrame(
    runwayConfig: RunwayConfig?,
    accentColor: Color
): Modifier {
    if (runwayConfig == null || !runwayConfig.enabled) return this
    val speed = runwayConfig.speed.coerceIn(0.4f, 2.5f)
    val cycleMs = (2800f / speed).toInt().coerceIn(1100, 4800)
    val transition = rememberInfiniteTransition(label = "editorial_runway_frame")
    val sweep by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "runway_light_sweep"
    )
    val highlightTint = com.example.ui.theme.LocalMelvoreSurfaces.current.onImageOverlay
    return this.drawWithContent {
        drawContent()
        val w = size.width
        val h = size.height
        val bandStart = w * (sweep - 0.18f)
        val bandEnd = w * (sweep + 0.18f)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    accentColor.copy(alpha = 0.08f),
                    highlightTint.copy(alpha = 0.14f),
                    accentColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                start = Offset(bandStart, 0f),
                end = Offset(bandEnd, h)
            ),
            size = Size(w, h)
        )
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.55f),
                    highlightTint.copy(alpha = 0.35f),
                    accentColor.copy(alpha = 0.18f)
                ),
                start = Offset(bandStart, 0f),
                end = Offset(bandEnd, h)
            ),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
