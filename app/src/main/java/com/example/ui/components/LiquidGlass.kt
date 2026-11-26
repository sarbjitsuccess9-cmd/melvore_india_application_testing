package com.example.ui.components

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalMelvoreDesignManifest
import com.example.ui.theme.LocalMelvoreSurfaces

/**
 * Applies Android 12+ (API 31) RenderEffect backdrop blur ONLY to a dedicated background
 * layer sitting behind a [LiquidGlassBox] or [LiquidGlassBar].
 *
 * IMPORTANT: Never apply this to the container wrapping the glass surface itself — only to
 * the underlying content layer being blurred behind a modal sheet, dialog, or overlay.
 * Below API 31 this is a no-op and [LiquidGlassBox] renders its translucent frosted fallback.
 */
fun Modifier.glassBackdropSource(
    enabled: Boolean,
    blurRadius: Dp = 18.dp
): Modifier = composed {
    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.blur(radius = blurRadius)
    } else {
        this
    }
}

/**
 * Liquid Glass surface powered strictly by [LocalMelvoreSurfaces]:
 * - Uses semantic `glassTint` (adapts cleanly to light and dark modes with zero black leaks).
 * - Renders a specular top-edge rim and subtle diagonal glass sheen.
 * - Tracks pointer position for a subtle finger-following specular highlight and spring press scale.
 * - Falls back gracefully to a frosted translucent surface below Android 12 or when [reducedMotion] is on.
 */
@Composable
fun LiquidGlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LocalMelvoreDesignManifest.current.tokens.radii.cardDp.coerceAtLeast(10).dp),
    tintColor: Color? = null,
    borderColor: Color? = null,
    elevation: Dp = 0.dp,
    reducedMotion: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val surfaces = LocalMelvoreSurfaces.current
    val supportsNativeBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val baseTint = tintColor ?: if (supportsNativeBlur) {
        surfaces.glassTint
    } else {
        // Frosted fallback below Android 12: slightly richer opacity for readability without RenderEffect blur
        surfaces.glassTint.copy(alpha = (surfaces.glassTint.alpha + 0.12f).coerceAtMost(0.95f))
    }

    val rimHighlight = if (surfaces.isDark) {
        surfaces.onSurface.copy(alpha = 0.24f)
    } else {
        surfaces.surface.copy(alpha = 0.88f)
    }
    val rimEdge = borderColor ?: surfaces.border.copy(alpha = if (surfaces.isDark) 0.55f else 0.72f)
    val rimBrush = remember(rimHighlight, rimEdge, surfaces.accent) {
        Brush.linearGradient(
            colors = listOf(
                rimHighlight,
                rimEdge,
                surfaces.accent.copy(alpha = if (surfaces.isDark) 0.22f else 0.14f)
            )
        )
    }
    val sheenBrush = remember(surfaces.isDark, surfaces.surface, surfaces.accent) {
        Brush.verticalGradient(
            colors = listOf(
                if (surfaces.isDark) surfaces.onSurface.copy(alpha = 0.08f) else surfaces.surface.copy(alpha = 0.42f),
                Color.Transparent,
                surfaces.accent.copy(alpha = if (surfaces.isDark) 0.06f else 0.03f)
            )
        )
    }

    var isPressed by remember { mutableStateOf(false) }
    var touchOffset by remember { mutableStateOf<Offset?>(null) }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion && onClick != null) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquidGlassScale"
    )

    val pointerModifier = if (!reducedMotion && onClick != null) {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                touchOffset = down.position
                val up = waitForUpOrCancellation()
                if (up != null) {
                    touchOffset = up.position
                }
                isPressed = false
                touchOffset = null
            }
        }
    } else {
        Modifier
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .then(pointerModifier)
            .then(clickModifier),
        shape = shape,
        color = baseTint,
        contentColor = surfaces.onSurface,
        shadowElevation = elevation
    ) {
        Box(
            modifier = Modifier
                .background(sheenBrush)
                .border(BorderStroke(1.dp, rimBrush), shape)
                .drawWithContent {
                    drawContent()
                    val pos = touchOffset
                    if (pos != null && !reducedMotion) {
                        val radius = (size.maxDimension * 0.45f).coerceAtLeast(64f)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    surfaces.onSurface.copy(alpha = if (surfaces.isDark) 0.14f else 0.08f),
                                    Color.Transparent
                                ),
                                center = pos,
                                radius = radius
                            ),
                            radius = radius,
                            center = pos
                        )
                    }
                },
            content = content
        )
    }
}

/**
 * Convenience Liquid Glass chrome wrapper for sticky headers, top search bars,
 * floating action bars, and ProductDetails bottom action bars.
 */
@Composable
fun LiquidGlassBar(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LocalMelvoreDesignManifest.current.tokens.radii.searchBarDp.coerceAtMost(24).dp),
    elevation: Dp = 4.dp,
    reducedMotion: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    LiquidGlassBox(
        modifier = modifier,
        shape = shape,
        elevation = elevation,
        reducedMotion = reducedMotion,
        onClick = onClick,
        content = content
    )
}

/**
 * Convenience Liquid Glass container for bottom sheets and floating dialogs.
 */
@Composable
fun LiquidGlassSheetContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(
        topStart = LocalMelvoreDesignManifest.current.tokens.radii.sheetDp.coerceAtLeast(16).dp,
        topEnd = LocalMelvoreDesignManifest.current.tokens.radii.sheetDp.coerceAtLeast(16).dp
    ),
    reducedMotion: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val surfaces = LocalMelvoreSurfaces.current
    LiquidGlassBox(
        modifier = modifier,
        shape = shape,
        tintColor = surfaces.glassTint.copy(alpha = (surfaces.glassTint.alpha + 0.10f).coerceAtMost(0.96f)),
        elevation = 8.dp,
        reducedMotion = reducedMotion,
        content = content
    )
}
