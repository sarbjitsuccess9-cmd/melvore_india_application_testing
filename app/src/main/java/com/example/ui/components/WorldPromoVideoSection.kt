package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.fontFamilyForStyleKey

/**
 * Full-bleed Hero Campaign Video + Poster Surface embedded directly inside the main
 * Fashion World hero header. Avoids duplicate separate video cards on the scroll feed.
 */
@Composable
fun WorldHeroVideoSurface(
    worldName: String,
    videoUrl: String?,
    posterUrl: String?,
    @DrawableRes fallbackDrawable: Int? = null,
    isSponsored: Boolean,
    accentColor: Color,
    autoplayMuted: Boolean,
    dataSaverMode: Boolean,
    onVideoAudioActiveChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanUrl = videoUrl?.trim()?.takeIf {
        it.isNotBlank() && (it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true))
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val surfaces = LocalMelvoreSurfaces.current

    var isMuted by remember(cleanUrl, autoplayMuted) { mutableStateOf(autoplayMuted) }
    var userRequestedPlay by remember(cleanUrl, dataSaverMode) { mutableStateOf(!dataSaverMode && cleanUrl != null) }
    var hasError by remember(cleanUrl) { mutableStateOf(false) }

    val player = remember(cleanUrl, userRequestedPlay) {
        if (cleanUrl == null || !userRequestedPlay) null
        else ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = if (isMuted) 0f else 1f
            setMediaItem(MediaItem.fromUri(cleanUrl))
            prepare()
            playWhenReady = true
        }
    }

    LaunchedEffect(player, isMuted, userRequestedPlay, hasError) {
        player?.volume = if (isMuted) 0f else 1f
        val audioActive = player != null && userRequestedPlay && !isMuted && !hasError
        onVideoAudioActiveChanged(audioActive)
    }

    DisposableEffect(player) {
        if (player == null) {
            return@DisposableEffect onDispose {
                onVideoAudioActiveChanged(false)
            }
        }
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                hasError = true
                onVideoAudioActiveChanged(false)
            }
        }
        player.addListener(listener)
        onDispose {
            onVideoAudioActiveChanged(false)
            player.removeListener(listener)
            player.release()
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        if (player == null) return@DisposableEffect onDispose { }
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    player.pause()
                    onVideoAudioActiveChanged(false)
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (userRequestedPlay && !hasError) {
                        player.play()
                        onVideoAudioActiveChanged(!isMuted)
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Poster / Editorial Hero Photography Fallback
        MelvoreNetworkImage(
            url = posterUrl,
            contentDescription = "$worldName editorial lookbook",
            fallbackDrawable = fallbackDrawable,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Seamless Full-Bleed ExoPlayer Video Layer
        if (player != null && !hasError && userRequestedPlay) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        this.player = player
                    }
                },
                update = { view ->
                    if (view.player !== player) {
                        view.player = player
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("world_hero_video_player")
            )
        }

        // Discreet Film Controls (bottom-end inside hero, >= 48.dp touch target)
        if (cleanUrl != null && !hasError) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 14.dp, bottom = 18.dp)
            ) {
                if (isSponsored) {
                    Text(
                        text = "FILM · SPONSORED",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = surfaces.onImageOverlay.copy(alpha = 0.85f),
                        modifier = Modifier
                            .background(surfaces.imageOverlay.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }
                IconButton(
                    onClick = {
                        if (!userRequestedPlay) {
                            userRequestedPlay = true
                        } else if (player != null) {
                            if (player.isPlaying) {
                                player.pause()
                                onVideoAudioActiveChanged(false)
                            } else {
                                player.play()
                                onVideoAudioActiveChanged(!isMuted)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(surfaces.imageOverlay.copy(alpha = 0.42f), CircleShape)
                        .testTag("world_promo_play_toggle")
                ) {
                    Icon(
                        imageVector = if (userRequestedPlay && player?.isPlaying != false) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (userRequestedPlay) "Pause campaign video" else "Play campaign video",
                        tint = surfaces.onImageOverlay,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (userRequestedPlay) {
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(48.dp)
                            .background(surfaces.imageOverlay.copy(alpha = 0.42f), CircleShape)
                            .testTag("world_promo_mute_toggle")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute campaign video" else "Mute campaign video",
                            tint = if (isMuted) surfaces.onImageOverlay else accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Legacy helper retained for API compatibility if referenced externally.
 */
@Composable
fun WorldPromoVideoSection(
    worldName: String,
    videoUrl: String?,
    posterUrl: String?,
    isSponsored: Boolean,
    fontStyle: String,
    accentColor: Color,
    surfaceColor: Color,
    textPrimary: Color,
    autoplayMuted: Boolean,
    dataSaverMode: Boolean,
    onVideoAudioActiveChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanUrl = videoUrl?.trim()?.takeIf {
        it.isNotBlank() && (it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true))
    }
    if (cleanUrl == null && posterUrl.isNullOrBlank()) return

    val surfaces = LocalMelvoreSurfaces.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("world_promo_video_section")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(surfaces.imageOverlay)
        ) {
            WorldHeroVideoSurface(
                worldName = worldName,
                videoUrl = cleanUrl,
                posterUrl = posterUrl,
                isSponsored = isSponsored,
                accentColor = accentColor,
                autoplayMuted = autoplayMuted,
                dataSaverMode = dataSaverMode,
                onVideoAudioActiveChanged = onVideoAudioActiveChanged,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, surfaces.imageOverlay.copy(alpha = 0.35f))
                        )
                    )
            )
        }
        Text(
            text = "$worldName Editorial Film",
            fontFamily = fontFamilyForStyleKey(fontStyle),
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = textPrimary,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
