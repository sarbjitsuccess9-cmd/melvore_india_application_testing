package com.example.ui.screens

import androidx.annotation.OptIn
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.model.Product
import com.example.ui.components.MelvoreErrorState
import com.example.ui.components.MelvoreLoadingState
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.components.ProductCard
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.viewmodel.DiscoveryUiState
import com.example.ui.viewmodel.MelvoreUiState

@OptIn(UnstableApi::class)
@Composable
fun BrandDetailScreen(
    brandId: Int,
    slug: String,
    uiState: MelvoreUiState,
    discoveryState: DiscoveryUiState,
    onLoadBrand: (Int?, String?) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onQuickAdd: (Product) -> Unit,
    onToggleWishlist: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    val context = LocalContext.current

    LaunchedEffect(brandId, slug) {
        onLoadBrand(brandId.takeIf { it > 0 }, slug.takeIf { it.isNotBlank() })
    }

    if (discoveryState.isBrandDetailLoading && discoveryState.brandDetail == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MelvoreLoadingState("Loading brand maison…")
        }
        return
    }

    val bundle = discoveryState.brandDetail
    if (bundle == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            MelvoreErrorState(
                message = discoveryState.brandDetailError ?: "Brand details unavailable.",
                onRetry = { onLoadBrand(brandId.takeIf { it > 0 }, slug.takeIf { it.isNotBlank() }) }
            )
        }
        return
    }

    val brand = bundle.brand
    val hasVideo = !brand.brandVideoUrl.isNullOrBlank()
    var isMuted by remember(brand.brandId) { mutableStateOf(true) }
    var isVideoReady by remember(brand.brandId) { mutableStateOf(false) }

    val exoPlayer = remember(brand.brandVideoUrl) {
        if (!hasVideo) null else ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
            setMediaItem(MediaItem.fromUri(brand.brandVideoUrl!!))
            prepare()
        }
    }

    if (exoPlayer != null) {
        DisposableEffect(exoPlayer) {
            val listener = object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    isVideoReady = true
                }
            }
            exoPlayer.addListener(listener)
            onDispose {
                exoPlayer.removeListener(listener)
                exoPlayer.release()
            }
        }
        LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
            exoPlayer.pause()
        }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            exoPlayer.playWhenReady = true
        }
        LaunchedEffect(isMuted) {
            exoPlayer.volume = if (isMuted) 0f else 1f
        }
    }

    var selectedTab by remember(brand.brandId) { mutableStateOf("featured") }
    val tabs = listOf(
        "featured" to "Featured (${bundle.featuredProducts.size})",
        "new" to "New Arrivals (${bundle.newArrivals.size})",
        "bestsellers" to "Bestsellers (${bundle.bestsellers.size})",
        "sponsored" to "Sponsored (${bundle.sponsoredProducts.size})"
    )

    val displayProducts = when (selectedTab) {
        "new" -> bundle.newArrivals.ifEmpty { bundle.featuredProducts }
        "bestsellers" -> bundle.bestsellers.ifEmpty { bundle.featuredProducts }
        "sponsored" -> bundle.sponsoredProducts
        else -> bundle.featuredProducts
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("brand_detail_screen"),
        contentPadding = PaddingValues(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brand Hero Cover / Muted Editorial Video
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .background(surfaces.surfaceElevated)
            ) {
                if (exoPlayer != null && isVideoReady) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                player = exoPlayer
                            }
                        },
                        update = { view ->
                            if (view.player != exoPlayer) view.player = exoPlayer
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (!isVideoReady) {
                    MelvoreNetworkImage(
                        url = brand.mobileCoverUrl ?: brand.coverImageUrl ?: brand.logoUrl,
                        contentDescription = brand.name,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, surfaces.imageOverlay.copy(alpha = 0.88f))
                            )
                        )
                )

                if (exoPlayer != null) {
                    Surface(
                        onClick = { isMuted = !isMuted },
                        shape = RoundedCornerShape(2.dp),
                        color = surfaces.imageOverlay.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, surfaces.onImageOverlay.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .testTag("brand_video_mute")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                                contentDescription = stringResource(R.string.cd_mute_toggle),
                                tint = surfaces.onImageOverlay,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!brand.logoUrl.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = surfaces.surface,
                                modifier = Modifier.size(44.dp)
                            ) {
                                MelvoreNetworkImage(
                                    url = brand.logoUrl,
                                    contentDescription = "${brand.name} logo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Column {
                            Text(
                                text = brand.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = surfaces.onImageOverlay
                            )
                            if (brand.originCountry.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Outlined.Public, contentDescription = null, tint = ext.goldAccent, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = brand.originCountry,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.goldAccent
                                    )
                                }
                            }
                        }
                    }
                    if (brand.tagline.isNotBlank()) {
                        Text(
                            text = brand.tagline,
                            style = MaterialTheme.typography.titleMedium,
                            color = ext.chromeSilver
                        )
                    }
                }
            }
        }

        // Brand Story Card
        val storyCopy = brand.story.ifBlank { brand.description }
        if (storyCopy.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.borderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "HOUSE STORY",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.4.sp,
                            color = ext.goldAccent
                        )
                        Text(
                            text = storyCopy,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.muted
                        )
                    }
                }
            }
        }

        // Segmented Catalog Tabs
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs, key = { it.first }) { (key, label) ->
                    val selected = selectedTab == key
                    Surface(
                        onClick = { selectedTab = key },
                        shape = RoundedCornerShape(2.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else ext.borderSubtle
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("brand_detail_tab_$key")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
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
        }

        if (displayProducts.isEmpty()) {
            item {
                Text(
                    text = "No pieces currently listed in this section.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ext.muted,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(displayProducts.chunked(2)) { pair ->
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
