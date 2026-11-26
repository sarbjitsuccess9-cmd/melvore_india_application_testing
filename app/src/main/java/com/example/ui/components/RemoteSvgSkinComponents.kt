package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.SubcomposeAsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.example.model.WorldKitSpec
import com.example.ui.design.AnimatedSvg
import com.example.ui.theme.LocalDesignKit
import com.example.ui.theme.LocalMelvoreApiBaseUrl
import com.example.ui.theme.LocalMelvoreSurfaces
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Resolves any manifest asset path against `asset_base_url` from [LocalDesignKit],
 * falling back to [LocalMelvoreApiBaseUrl].
 */
@Composable
fun rememberResolvedDesignAssetUrl(relativeOrAbsoluteUrl: String?): String? {
    val designKit = LocalDesignKit.current
    val apiBase = LocalMelvoreApiBaseUrl.current
    val base = designKit.assetBaseUrl.ifBlank { apiBase }
    return remember(relativeOrAbsoluteUrl, base) {
        RemoteDesignRegistry.resolveAssetUrl(relativeOrAbsoluteUrl, base)
    }
}

/**
 * Declarative remote SVG renderer:
 * - Resolves paths using `asset_base_url` (falling back to API base).
 * - Routes SMIL-animated SVG assets (`animateSmil = true`, `!reducedMotion`, no `colorFilter`)
 *   through the sandboxed [AnimatedSvg] WebView renderer with automatic static Coil fallback.
 * - Uses Coil [SvgDecoder] for static or tinted SVGs and silently falls back on error/offline.
 */
@Composable
fun RemoteSvgSkinImage(
    relativeOrAbsoluteUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillBounds,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null,
    animateSmil: Boolean = false,
    reducedMotion: Boolean = false,
    fallback: @Composable () -> Unit = {}
) {
    val resolvedUrl = rememberResolvedDesignAssetUrl(relativeOrAbsoluteUrl)
    if (resolvedUrl.isNullOrBlank()) {
        fallback()
        return
    }
    val isSvgUrl = resolvedUrl.substringBefore('?').endsWith(".svg", ignoreCase = true)
    if (animateSmil && isSvgUrl && !reducedMotion && colorFilter == null) {
        AnimatedSvg(
            url = resolvedUrl,
            contentDescription = contentDescription,
            modifier = modifier.alpha(alpha),
            reducedMotion = false,
            contentScale = contentScale,
            fallback = fallback
        )
        return
    }
    val context = LocalContext.current
    val request = remember(resolvedUrl, context) {
        ImageRequest.Builder(context)
            .data(resolvedUrl)
            .decoderFactory(SvgDecoder.Factory())
            .crossfade(160)
            .build()
    }
    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        modifier = modifier,
        loading = { fallback() },
        error = { fallback() }
    )
}

/**
 * Remote SVG navigation tab icon with instant Material vector icon fallback.
 */
@Composable
fun RemoteSvgTabIcon(
    svgUrl: String?,
    fallbackVector: ImageVector,
    contentDescription: String?,
    tint: Color = LocalContentColor.current,
    size: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    val fallbackIcon: @Composable () -> Unit = {
        Icon(
            imageVector = fallbackVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size)
        )
    }
    if (svgUrl.isNullOrBlank()) {
        fallbackIcon()
        return
    }
    RemoteSvgSkinImage(
        relativeOrAbsoluteUrl = svgUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
        fallback = fallbackIcon
    )
}

/**
 * Remote SVG decorative divider for Fashion Worlds and Occasion cards with native HorizontalDivider fallback.
 */
@Composable
fun RemoteSvgDivider(
    svgPath: String?,
    fallbackColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
    height: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    val fallbackBar: @Composable () -> Unit = {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height),
            contentAlignment = Alignment.Center
        ) {
            HorizontalDivider(thickness = 0.75.dp, color = fallbackColor)
        }
    }
    if (svgPath.isNullOrBlank()) {
        fallbackBar()
        return
    }
    RemoteSvgSkinImage(
        relativeOrAbsoluteUrl = svgPath,
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        fallback = fallbackBar
    )
}

/**
 * Non-interactive SVG frame overlay applied on top of product photography when a world kit specifies card_frame.svg.
 */
@Composable
fun RemoteSvgCardFrameOverlay(
    cardFrameSvgPath: String?,
    modifier: Modifier = Modifier
) {
    if (cardFrameSvgPath.isNullOrBlank()) return
    RemoteSvgSkinImage(
        relativeOrAbsoluteUrl = cardFrameSvgPath,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Remote SVG loading indicator with CircularProgressIndicator fallback.
 */
@Composable
fun RemoteSvgLoader(
    loaderSvgPath: String?,
    reducedMotion: Boolean = false,
    size: Dp = 44.dp,
    fallbackColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val fallbackSpinner: @Composable () -> Unit = {
        CircularProgressIndicator(
            color = fallbackColor,
            strokeWidth = 2.dp,
            modifier = Modifier.size(32.dp)
        )
    }
    if (loaderSvgPath.isNullOrBlank()) {
        fallbackSpinner()
        return
    }
    val pulseAlpha = if (!reducedMotion) {
        val transition = rememberInfiniteTransition(label = "remote_svg_loader")
        val a by transition.animateFloat(
            initialValue = 0.68f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 850, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "loader_pulse"
        )
        a
    } else 1f

    RemoteSvgSkinImage(
        relativeOrAbsoluteUrl = loaderSvgPath,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        alpha = pulseAlpha,
        animateSmil = true,
        reducedMotion = reducedMotion,
        modifier = modifier.size(size),
        fallback = fallbackSpinner
    )
}

/**
 * Remote SVG track overlay for interactive sliders (worldKit.svg.slider) with graceful fallback.
 */
@Composable
fun RemoteSvgSliderTrack(
    sliderSvgPath: String?,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit
) {
    if (sliderSvgPath.isNullOrBlank()) {
        fallback()
        return
    }
    RemoteSvgSkinImage(
        relativeOrAbsoluteUrl = sliderSvgPath,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier,
        fallback = fallback
    )
}

/**
 * Remote SVG pager indicator row (worldKit.svg.pager) for Hero & Product media carousels.
 * Renders the world's pager.svg per page (full alpha when selected, dimmed when inactive)
 * with an instant editorial pill fallback when blank or offline.
 */
@Composable
fun RemoteSvgPagerIndicator(
    pageCount: Int,
    currentPage: Int,
    pagerSvgPath: String?,
    activeColor: Color = LocalMelvoreSurfaces.current.onImageOverlay,
    inactiveColor: Color = LocalMelvoreSurfaces.current.onImageOverlay.copy(alpha = 0.42f),
    modifier: Modifier = Modifier
) {
    if (pageCount <= 1) return
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { idx ->
            val selected = idx == currentPage
            val fallbackDot: @Composable () -> Unit = {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .height(3.dp)
                        .size(width = if (selected) 24.dp else 9.dp, height = 3.dp)
                        .alpha(if (selected) 1f else 0.72f)
                ) {
                    androidx.compose.material3.Surface(
                        color = if (selected) activeColor else inactiveColor,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {}
                }
            }
            if (pagerSvgPath.isNullOrBlank()) {
                fallbackDot()
            } else {
                RemoteSvgSkinImage(
                    relativeOrAbsoluteUrl = pagerSvgPath,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alpha = if (selected) 1f else 0.38f,
                    modifier = Modifier.size(width = if (selected) 22.dp else 12.dp, height = 10.dp),
                    fallback = fallbackDot
                )
            }
        }
    }
}

/**
 * Lightweight budget-capped remote SVG ambient sprite layer for v1.2.0 World Kits.
 * Enforces budget.maxAmbientParticles (0 when reducedMotion) and pauses automatically on ON_STOP.
 */
@Composable
fun WorldAmbientSvgSpritesLayer(
    worldKit: WorldKitSpec?,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    if (worldKit == null || worldKit.ambient.isEmpty() || reducedMotion) return

    val designKit = LocalDesignKit.current
    val apiBase = designKit.assetBaseUrl.ifBlank { LocalMelvoreApiBaseUrl.current }
    val budgetCap = worldKit.budget.maxAmbientParticles.coerceIn(0, 8)
    if (budgetCap == 0) return

    val spriteUrls = remember(worldKit, budgetCap, apiBase) {
        buildList {
            for (item in worldKit.ambient) {
                val url = RemoteDesignRegistry.resolveWorldSpriteUrl(item.assetKey, apiBase) ?: continue
                val takeCount = item.maxCount.coerceIn(1, 3)
                repeat(takeCount) {
                    if (size < budgetCap) add(url)
                }
                if (size >= budgetCap) break
            }
        }
    }
    if (spriteUrls.isEmpty()) return

    val lifecycleOwner = LocalLifecycleOwner.current
    var isStarted by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> isStarted = true
                Lifecycle.Event.ON_STOP -> isStarted = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val phase = if (isStarted) {
        val transition = rememberInfiniteTransition(label = "world_ambient_svg_sprites")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 8400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sprite_phase"
        )
        p
    } else 0.15f

    val anchors = remember {
        listOf(
            0.84f to 0.18f,
            0.12f to 0.30f,
            0.78f to 0.56f,
            0.22f to 0.68f,
            0.88f to 0.36f,
            0.16f to 0.14f,
            0.66f to 0.24f,
            0.34f to 0.44f
        )
    }

    BoxWithConstraints(modifier = modifier) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        if (wPx <= 0f || hPx <= 0f) return@BoxWithConstraints

        spriteUrls.forEachIndexed { idx, spriteUrl ->
            val (ax, ay) = anchors[idx % anchors.size]
            val angle = (phase + idx * 0.21f) * 2f * PI.toFloat()
            val dx = cos(angle) * (wPx * 0.018f)
            val dy = sin(angle) * (hPx * 0.024f)
            val xPx = (wPx * ax + dx).roundToInt()
            val yPx = (hPx * ay + dy).roundToInt()

            RemoteSvgSkinImage(
                relativeOrAbsoluteUrl = spriteUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alpha = 0.62f,
                modifier = Modifier
                    .offset { IntOffset(xPx - 28, yPx - 28) }
                    .size(if (idx % 2 == 0) 28.dp else 22.dp)
            )
        }
    }
}
