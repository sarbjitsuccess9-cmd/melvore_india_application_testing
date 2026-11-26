package com.example.ui.screens

import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.BuildConfig
import com.example.R
import com.example.model.Product
import com.example.model.formatPrice
import com.example.ui.components.LiquidGlassMediaIconButton
import com.example.ui.components.LiquidGlassSheetContainer
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.MelvoreLoadingState
import com.example.ui.components.MelvoreNetworkImage
import com.example.ui.components.PremiumQuickAddButton
import com.example.ui.theme.LocalMelvoreColors
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.MelvoreSerifFontFamily
import com.example.ui.theme.ensureContrast
import com.example.ui.viewmodel.DiscoveryUiState

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@OptIn(UnstableApi::class)
@Composable
fun VideosScreen(
    discoveryState: DiscoveryUiState,
    currency: String,
    onLoadVideos: () -> Unit,
    onLoadMoreVideos: () -> Unit = {},
    onRefreshVideos: () -> Unit = onLoadVideos,
    onToggleLike: (Int) -> Unit,
    onToggleSave: (Int) -> Unit,
    onRecordVideoEvent: (Int, String, Int) -> Unit,
    onOpenBrand: (Int, String) -> Unit,
    onOpenProduct: (Product) -> Unit,
    onAddToBag: (Product) -> Unit,
    onVideoAudioActiveChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (discoveryState.videos.isEmpty()) {
            onLoadVideos()
        }
    }

    if (discoveryState.isVideosLoading && discoveryState.videos.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(surfaces.background),
            contentAlignment = Alignment.Center
        ) {
            MelvoreLoadingState("Loading fashion reels…")
        }
        return
    }

    val reels = discoveryState.videos
    if (reels.isEmpty()) {
        VideosEmptyOrErrorState(
            errorMessage = discoveryState.videosError,
            diagnosticsReason = discoveryState.videosDiagnosticsReason,
            tableExists = discoveryState.videosTableExists,
            onRetry = onRefreshVideos,
            modifier = modifier
        )
        return
    }

    val pagerState = rememberPagerState(pageCount = { reels.size })
    var isMuted by remember { mutableStateOf(false) }
    var userPaused by remember { mutableStateOf(false) }
    var firstFrameRendered by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var retryNonce by remember { mutableIntStateOf(0) }
    var shopLookProduct by remember { mutableStateOf<Product?>(null) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    playbackError = null
                }
            }

            override fun onRenderedFirstFrame() {
                firstFrameRendered = true
                isBuffering = false
                playbackError = null
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playbackError = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "Network interrupted while streaming this reel."
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                        "This video stream URL is currently unreachable."
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                    PlaybackException.ERROR_CODE_DECODING_FAILED ->
                        "Unsupported video format on this device."
                    else -> "Playback unavailable (${error.errorCodeName})."
                }
                Log.w("MelvoreVideos", "ExoPlayer error code=${error.errorCodeName}: ${error.message}")
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            onVideoAudioActiveChanged(false)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        exoPlayer.pause()
        onVideoAudioActiveChanged(false)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (!userPaused && playbackError == null) {
            exoPlayer.playWhenReady = true
            onVideoAudioActiveChanged(!isMuted)
        }
    }

    LaunchedEffect(isMuted, userPaused, playbackError) {
        exoPlayer.volume = if (isMuted) 0f else 1f
        onVideoAudioActiveChanged(!isMuted && !userPaused && playbackError == null)
    }

    val settledIndex = pagerState.settledPage

    // End-to-end pagination trigger when approaching the end of loaded reels
    LaunchedEffect(settledIndex, reels.size, discoveryState.canLoadMoreVideos, discoveryState.isLoadingMoreVideos) {
        if (settledIndex >= (reels.size - 3).coerceAtLeast(0) &&
            discoveryState.canLoadMoreVideos &&
            !discoveryState.isLoadingMoreVideos
        ) {
            onLoadMoreVideos()
        }
    }

    LaunchedEffect(settledIndex, reels.size, retryNonce) {
        val currentReel = reels.getOrNull(settledIndex) ?: return@LaunchedEffect
        userPaused = false
        firstFrameRendered = false
        playbackError = null
        if (currentReel.hasValidStreamUrl()) {
            isBuffering = true
            val url = currentReel.videoUrl!!.trim()
            runCatching {
                exoPlayer.setMediaItem(MediaItem.fromUri(url))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }.onFailure { err ->
                isBuffering = false
                playbackError = err.localizedMessage ?: "Unable to initialize stream."
            }
        } else {
            isBuffering = false
            exoPlayer.stop()
            playbackError = "Stream URL missing or invalid for this reel."
        }
        if (retryNonce == 0) {
            onRecordVideoEvent(currentReel.videoId, "view", 0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surfaces.imageOverlay)
            .testTag("videos_screen")
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val reel = reels[page]
            val isCurrentSettled = page == settledIndex
            val hasValidUrl = reel.hasValidStreamUrl()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(surfaces.imageOverlay)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isCurrentSettled && hasValidUrl && playbackError == null) {
                            userPaused = !userPaused
                            exoPlayer.playWhenReady = !userPaused
                        }
                    }
            ) {
                if (isCurrentSettled && hasValidUrl && playbackError == null) {
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

                // Poster / Thumbnail overlay while buffering, before first frame, or on stream error
                if (!isCurrentSettled || !firstFrameRendered || !hasValidUrl || playbackError != null) {
                    MelvoreNetworkImage(
                        url = reel.posterUrl ?: reel.product?.imageUrl,
                        contentDescription = reel.title.ifBlank { "Style reel" },
                        fallbackDrawable = R.drawable.img_hero_seoul,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Editorial top & bottom readability scrims
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to surfaces.imageOverlay.copy(alpha = 0.50f),
                                    0.22f to Color.Transparent,
                                    0.60f to Color.Transparent,
                                    1f to surfaces.imageOverlay.copy(alpha = 0.92f)
                                )
                            )
                        )
                )

                // Sleek buffering indicator before first frame renders
                if (isCurrentSettled && isBuffering && !firstFrameRendered && playbackError == null) {
                    LiquidGlassSurface(
                        darkGlass = true,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(56.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = surfaces.onImageOverlay,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Paused indicator
                if (isCurrentSettled && userPaused && playbackError == null && hasValidUrl) {
                    LiquidGlassSurface(
                        darkGlass = true,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(66.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Resume reel",
                                tint = surfaces.onImageOverlay,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                // Non-blocking Liquid Glass Playback Error Card (swipe to next reel still works)
                if (isCurrentSettled && playbackError != null) {
                    LiquidGlassSurface(
                        darkGlass = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 28.dp)
                            .testTag("reel_playback_error_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = ext.goldAccent,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = playbackError ?: "Unable to play this reel",
                                style = MaterialTheme.typography.bodySmall,
                                color = surfaces.onImageOverlay,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Swipe up for the next look or retry stream",
                                style = MaterialTheme.typography.labelSmall,
                                color = surfaces.onImageOverlay.copy(alpha = 0.72f),
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { retryNonce++ },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = surfaces.surface,
                                    contentColor = surfaces.onSurface
                                ),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("reel_retry_button")
                            ) {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    contentDescription = stringResource(R.string.cd_retry_video),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Retry Stream", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Top Floating Liquid Glass Bar (Title + Sponsored Tag + Refresh + Mute)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LiquidGlassSurface(
                            darkGlass = true,
                            shape = RoundedCornerShape(999.dp),
                            elevation = 3.dp
                        ) {
                            Text(
                                text = "MELVORE REELS",
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 1.6.sp,
                                fontWeight = FontWeight.Bold,
                                color = surfaces.onImageOverlay,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        if (reel.isSponsored) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = ext.goldAccent
                            ) {
                                Text(
                                    text = "SPONSORED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ensureContrast(surfaces.onAccent, ext.goldAccent),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LiquidGlassMediaIconButton(
                            onClick = onRefreshVideos,
                            contentDescription = "Refresh reels",
                            modifier = Modifier.testTag("reel_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh reels",
                                tint = surfaces.onImageOverlay,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        LiquidGlassMediaIconButton(
                            onClick = { isMuted = !isMuted },
                            contentDescription = stringResource(R.string.cd_mute_toggle),
                            modifier = Modifier.testTag("reel_mute_button")
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                                contentDescription = stringResource(R.string.cd_mute_toggle),
                                tint = surfaces.onImageOverlay,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Bottom Overlay: Brand, Creator, Caption & Liquid Glass Shop Card + Right Action Rail
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        if (!reel.brandName.isNullOrBlank()) {
                            LiquidGlassSurface(
                                onClick = { onOpenBrand(reel.brandId ?: 0, reel.brandSlug.orEmpty()) },
                                darkGlass = true,
                                shape = RoundedCornerShape(999.dp),
                                elevation = 2.dp,
                                modifier = Modifier.heightIn(min = 40.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    if (!reel.brandLogo.isNullOrBlank()) {
                                        MelvoreNetworkImage(
                                            url = reel.brandLogo,
                                            contentDescription = reel.brandName,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                        )
                                    }
                                    Text(
                                        text = reel.brandName.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ext.goldAccent,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        }

                        if (reel.creatorName.isNotBlank() || reel.creatorHandle.isNotBlank()) {
                            Text(
                                text = reel.creatorHandle.ifBlank { reel.creatorName },
                                style = MaterialTheme.typography.labelMedium,
                                color = surfaces.onImageOverlay.copy(alpha = 0.88f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (reel.title.isNotBlank()) {
                            Text(
                                text = reel.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = MelvoreSerifFontFamily,
                                color = surfaces.onImageOverlay,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2
                            )
                        }

                        if (reel.caption.isNotBlank()) {
                            Text(
                                text = reel.caption,
                                style = MaterialTheme.typography.bodySmall,
                                color = surfaces.onImageOverlay.copy(alpha = 0.84f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val taggedProduct = reel.product
                        if (taggedProduct != null) {
                            LiquidGlassSurface(
                                onClick = { shopLookProduct = taggedProduct },
                                darkGlass = true,
                                shape = RoundedCornerShape(12.dp),
                                elevation = 4.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .testTag("reel_shop_look_${reel.videoId}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    MelvoreNetworkImage(
                                        url = taggedProduct.imageUrl,
                                        contentDescription = taggedProduct.name,
                                        fallbackDrawable = R.drawable.img_biker_jacket,
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "SHOP THIS LOOK",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            letterSpacing = 1.sp,
                                            color = ext.goldAccent,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = taggedProduct.name,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = surfaces.onImageOverlay,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = formatPrice(taggedProduct.effectivePrice, currency),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = FontFamily.Monospace,
                                            color = surfaces.onImageOverlay.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Surface(
                                        onClick = { onAddToBag(taggedProduct) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = surfaces.surface,
                                        modifier = Modifier.heightIn(min = 40.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = surfaces.onSurface, modifier = Modifier.size(14.dp))
                                            Text("Add", style = MaterialTheme.typography.labelMedium, color = surfaces.onSurface, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Right-Side Liquid Glass Interaction Rail (48dp+ touch targets)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ReelActionIcon(
                            icon = if (reel.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            countText = reel.likesCount.toString(),
                            contentDescription = stringResource(R.string.cd_like_video),
                            tint = if (reel.isLiked) surfaces.sale else surfaces.onImageOverlay,
                            active = reel.isLiked,
                            onClick = { onToggleLike(reel.videoId) },
                            testTag = "reel_like_${reel.videoId}"
                        )
                        ReelActionIcon(
                            icon = if (reel.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            countText = reel.savesCount.toString(),
                            contentDescription = stringResource(R.string.cd_save_video),
                            tint = if (reel.isSaved) ext.goldAccent else surfaces.onImageOverlay,
                            active = reel.isSaved,
                            onClick = { onToggleSave(reel.videoId) },
                            testTag = "reel_save_${reel.videoId}"
                        )
                        ReelActionIcon(
                            icon = Icons.Outlined.Share,
                            countText = "Share",
                            contentDescription = stringResource(R.string.cd_share_video),
                            tint = surfaces.onImageOverlay,
                            active = false,
                            onClick = {
                                onRecordVideoEvent(reel.videoId, "share", 0)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Watch ${reel.title.ifBlank { "this look" }} on Melvore: https://melvore.co.in"
                                    )
                                }
                                runCatching {
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Reel"))
                                }
                            },
                            testTag = "reel_share_${reel.videoId}"
                        )
                    }
                }
            }
        }

        // Bottom pagination loading pill
        if (discoveryState.isLoadingMoreVideos) {
            LiquidGlassSurface(
                darkGlass = true,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    CircularProgressIndicator(color = surfaces.onImageOverlay, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    Text("Loading more reels…", style = MaterialTheme.typography.labelSmall, color = surfaces.onImageOverlay)
                }
            }
        }
    }

    val lookProduct = shopLookProduct
    if (lookProduct != null) {
        ModalBottomSheet(
            onDismissRequest = { shopLookProduct = null },
            containerColor = Color.Transparent,
            dragHandle = null
        ) {
            LiquidGlassSheetContainer(
                modifier = Modifier.fillMaxWidth()
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "SHOP THIS LOOK",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.6.sp,
                    fontWeight = FontWeight.Bold,
                    color = ext.goldAccent
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MelvoreNetworkImage(
                        url = lookProduct.imageUrl,
                        contentDescription = lookProduct.name,
                        fallbackDrawable = R.drawable.img_biker_jacket,
                        modifier = Modifier
                            .size(92.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (lookProduct.brandName.isNotBlank()) {
                            Text(lookProduct.brandName.uppercase(), style = MaterialTheme.typography.labelSmall, color = ext.muted)
                        }
                        Text(lookProduct.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatPrice(lookProduct.effectivePrice, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            shopLookProduct = null
                            onOpenProduct(lookProduct)
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Text("View Details")
                    }
                    PremiumQuickAddButton(
                        inStock = lookProduct.inStock,
                        productName = lookProduct.name,
                        onAddClick = {
                            onAddToBag(lookProduct)
                            shopLookProduct = null
                        },
                        compact = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            }
        }
    }
}

@Composable
private fun VideosEmptyOrErrorState(
    errorMessage: String?,
    diagnosticsReason: String,
    tableExists: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMelvoreColors.current
    val surfaces = LocalMelvoreSurfaces.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surfaces.background)
            .padding(24.dp)
            .testTag("videos_empty_state"),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(
            darkGlass = false,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ext.goldAccent.copy(alpha = 0.16f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.PlayCircle,
                            contentDescription = null,
                            tint = ext.goldAccent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Text(
                    text = if (errorMessage != null) "Unable to Load Reels" else "Runway Reels Coming Online",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = MelvoreSerifFontFamily,
                    color = surfaces.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = errorMessage
                        ?: "Curated vertical fashion films and creator edits appear here as soon as active stream URLs are published in Melvore Studio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = surfaces.onSurfaceMuted,
                    textAlign = TextAlign.Center
                )
                if (BuildConfig.DEBUG && (!tableExists || diagnosticsReason.isNotBlank())) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = surfaces.surfaceElevated,
                        border = BorderStroke(1.dp, ext.goldAccent.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "DEV DIAGNOSTIC: table_exists=$tableExists · reason=${diagnosticsReason.ifBlank { "no_valid_mp4_rows" }}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = surfaces.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = surfaces.accent,
                        contentColor = surfaces.onAccent
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("videos_retry_button")
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Refresh Feed", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReelActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    countText: String,
    contentDescription: String,
    tint: Color,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val surfaces = LocalMelvoreSurfaces.current
    val scale by animateFloatAsState(
        targetValue = if (active) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "reel_action_bounce"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        LiquidGlassMediaIconButton(
            onClick = onClick,
            contentDescription = contentDescription,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .testTag(testTag)
        ) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(
            text = countText,
            style = MaterialTheme.typography.labelSmall,
            color = surfaces.onImageOverlay,
            fontWeight = FontWeight.SemiBold
        )
    }
}
