package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.WorldVectorAsset
import com.example.ui.theme.parseHexColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Original GPU-friendly Jetpack Compose Canvas / Path motif library for Fashion Worlds & Occasions.
 * Pauses animation automatically when offscreen, lifecycle-stopped (ON_STOP), or reducedMotion is enabled.
 */
@Composable
fun WorldVectorArtLayer(
    assets: List<WorldVectorAsset>,
    worldSlug: String,
    fallbackPreset: String = "",
    primaryAccent: Color = Color(0xFFD4AF37),
    secondaryAccent: Color = Color(0xFFC8CCD4),
    reducedMotion: Boolean = false,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
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

    val effectiveAssets = remember(assets, worldSlug, fallbackPreset, primaryAccent, secondaryAccent) {
        val matching = assets.filter {
            (worldSlug.isNotBlank() && it.worldSlug.equals(worldSlug, ignoreCase = true)) ||
                (worldSlug.isNotBlank() && it.occasionSlug.equals(worldSlug, ignoreCase = true))
        }
        matching.ifEmpty {
            defaultMotifsForSlug(worldSlug.ifBlank { fallbackPreset }, primaryAccent, secondaryAccent)
        }
    }

    if (effectiveAssets.isEmpty()) return

    val animateNow = isVisible && isStarted && !reducedMotion
    val phase = if (animateNow) {
        val transition = rememberInfiniteTransition(label = "worldVectorArt")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "vectorPhase"
        )
        p
    } else {
        0.22f
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            effectiveAssets.take(4).forEachIndexed { index, asset ->
                val pColor = parseHexColor(asset.primaryHex, primaryAccent).copy(alpha = asset.opacity.coerceIn(0.08f, 0.72f))
                val sColor = parseHexColor(asset.secondaryHex, secondaryAccent).copy(alpha = (asset.opacity * 0.75f).coerceIn(0.05f, 0.55f))
                val driftY = if (reducedMotion) 0f else sin((phase + index * 0.25f) * 2f * PI.toFloat()) * (h * 0.03f)
                val driftX = if (reducedMotion) 0f else cos((phase + index * 0.31f) * 2f * PI.toFloat()) * (w * 0.025f)

                val anchor = when (index % 4) {
                    0 -> Offset(w * 0.84f + driftX, h * 0.22f + driftY)
                    1 -> Offset(w * 0.14f - driftX, h * 0.76f - driftY)
                    2 -> Offset(w * 0.88f - driftX * 0.6f, h * 0.72f + driftY * 0.7f)
                    else -> Offset(w * 0.20f + driftX * 0.5f, h * 0.24f - driftY * 0.6f)
                }

                drawMotif(
                    vectorType = asset.vectorType,
                    center = anchor,
                    scale = minOf(w, h) * when (asset.layerRole) {
                        "background" -> 0.19f
                        "foreground" -> 0.11f
                        else -> 0.14f
                    },
                    primaryColor = pColor,
                    secondaryColor = sColor,
                    phase = phase + index * 0.17f
                )
            }
        }
    }
}

private fun DrawScope.drawMotif(
    vectorType: String,
    center: Offset,
    scale: Float,
    primaryColor: Color,
    secondaryColor: Color,
    phase: Float
) {
    val angle = (phase % 1f) * 360f
    when (vectorType.lowercase()) {
        "pixel_heart" -> drawPixelHeart(center, scale, primaryColor, secondaryColor)
        "pixel_star" -> drawPixelStar(center, scale, primaryColor, secondaryColor)
        "chrome_sparkle" -> drawChromeSparkle(center, scale, primaryColor, secondaryColor, angle * 0.35f)
        "goth_raven" -> drawGothRaven(center, scale, primaryColor, secondaryColor)
        "goth_bat" -> drawGothBat(center, scale, primaryColor, secondaryColor)
        "crescent_moon" -> drawCrescentMoon(center, scale, primaryColor, secondaryColor)
        "gothic_mist" -> drawGothicMist(center, scale, primaryColor, secondaryColor)
        "ribbon_bow" -> drawRibbonBow(center, scale, primaryColor, secondaryColor)
        "flower_petal" -> drawFlowerPetal(center, scale, primaryColor, secondaryColor, angle * 0.4f)
        "street_bolt" -> drawStreetBolt(center, scale, primaryColor, secondaryColor)
        "editorial_grid" -> drawEditorialGrid(center, scale, primaryColor, secondaryColor)
        "atelier_frame" -> drawAtelierFrame(center, scale, primaryColor, secondaryColor)
        "diya_flame" -> drawDiyaFlame(center, scale, primaryColor, secondaryColor, phase)
        "snowflake" -> drawSnowflake(center, scale, primaryColor, secondaryColor, angle * 0.25f)
        "firework" -> drawFirework(center, scale, primaryColor, secondaryColor, phase)
        "confetti" -> drawConfetti(center, scale, primaryColor, secondaryColor, angle)
        "sale_sticker" -> drawSaleSticker(center, scale, primaryColor, secondaryColor, angle * 0.1f)
        else -> drawChromeSparkle(center, scale, primaryColor, secondaryColor, angle * 0.2f)
    }
}

private fun DrawScope.drawPixelHeart(c: Offset, s: Float, p: Color, sec: Color) {
    val unit = s * 0.22f
    val blocks = listOf(
        -2 to -2, -1 to -2, 1 to -2, 2 to -2,
        -3 to -1, -2 to -1, -1 to -1, 0 to -1, 1 to -1, 2 to -1, 3 to -1,
        -3 to 0, -2 to 0, -1 to 0, 0 to 0, 1 to 0, 2 to 0, 3 to 0,
        -2 to 1, -1 to 1, 0 to 1, 1 to 1, 2 to 1,
        -1 to 2, 0 to 2, 1 to 2,
        0 to 3
    )
    blocks.forEach { (gx, gy) ->
        val color = if (gx == -2 && gy == -1) sec else p
        drawRect(
            color = color,
            topLeft = Offset(c.x + gx * unit - unit / 2f, c.y + gy * unit - unit / 2f),
            size = Size(unit * 0.9f, unit * 0.9f)
        )
    }
}

private fun DrawScope.drawPixelStar(c: Offset, s: Float, p: Color, sec: Color) {
    val unit = s * 0.22f
    val coords = listOf(0 to -3, 0 to -2, -1 to -1, 0 to -1, 1 to -1, -3 to 0, -2 to 0, -1 to 0, 0 to 0, 1 to 0, 2 to 0, 3 to 0, -1 to 1, 0 to 1, 1 to 1, 0 to 2, 0 to 3)
    coords.forEach { (gx, gy) ->
        drawRect(
            color = if (gx == 0 && gy == 0) sec else p,
            topLeft = Offset(c.x + gx * unit - unit / 2f, c.y + gy * unit - unit / 2f),
            size = Size(unit * 0.9f, unit * 0.9f)
        )
    }
}

private fun DrawScope.drawChromeSparkle(c: Offset, s: Float, p: Color, sec: Color, rot: Float) {
    rotate(degrees = rot, pivot = c) {
        val path = Path().apply {
            moveTo(c.x, c.y - s)
            quadraticTo(c.x + s * 0.14f, c.y - s * 0.14f, c.x + s, c.y)
            quadraticTo(c.x + s * 0.14f, c.y + s * 0.14f, c.x, c.y + s)
            quadraticTo(c.x - s * 0.14f, c.y + s * 0.14f, c.x - s, c.y)
            quadraticTo(c.x - s * 0.14f, c.y - s * 0.14f, c.x, c.y - s)
            close()
        }
        drawPath(path, brush = Brush.linearGradient(listOf(p, sec)))
    }
}

private fun DrawScope.drawGothRaven(c: Offset, s: Float, p: Color, sec: Color) {
    val path = Path().apply {
        moveTo(c.x - s, c.y + s * 0.1f)
        quadraticTo(c.x - s * 0.45f, c.y - s * 0.65f, c.x - s * 0.08f, c.y - s * 0.18f)
        lineTo(c.x + s * 0.12f, c.y - s * 0.35f)
        lineTo(c.x + s * 0.36f, c.y - s * 0.24f)
        lineTo(c.x + s * 0.18f, c.y - s * 0.10f)
        quadraticTo(c.x + s * 0.55f, c.y + s * 0.45f, c.x + s, c.y + s * 0.18f)
        quadraticTo(c.x + s * 0.25f, c.y + s * 0.52f, c.x - s * 0.20f, c.y + s * 0.35f)
        quadraticTo(c.x - s * 0.62f, c.y + s * 0.45f, c.x - s, c.y + s * 0.1f)
        close()
    }
    drawPath(path, color = p)
    drawCircle(color = sec, radius = s * 0.06f, center = Offset(c.x + s * 0.16f, c.y - s * 0.25f))
}

private fun DrawScope.drawGothBat(c: Offset, s: Float, p: Color, sec: Color) {
    val path = Path().apply {
        moveTo(c.x, c.y - s * 0.22f)
        lineTo(c.x + s * 0.16f, c.y - s * 0.42f)
        lineTo(c.x + s * 0.28f, c.y - s * 0.18f)
        quadraticTo(c.x + s * 0.72f, c.y - s * 0.55f, c.x + s, c.y - s * 0.08f)
        quadraticTo(c.x + s * 0.68f, c.y + s * 0.15f, c.x + s * 0.42f, c.y + s * 0.28f)
        lineTo(c.x, c.y + s * 0.15f)
        lineTo(c.x - s * 0.42f, c.y + s * 0.28f)
        quadraticTo(c.x - s * 0.68f, c.y + s * 0.15f, c.x - s, c.y - s * 0.08f)
        quadraticTo(c.x - s * 0.72f, c.y - s * 0.55f, c.x - s * 0.28f, c.y - s * 0.18f)
        lineTo(c.x - s * 0.16f, c.y - s * 0.42f)
        close()
    }
    drawPath(path, color = p)
    drawPath(path, color = sec, style = Stroke(width = 1.5f))
}

private fun DrawScope.drawCrescentMoon(c: Offset, s: Float, p: Color, sec: Color) {
    val outer = Path().apply {
        moveTo(c.x + s * 0.2f, c.y - s * 0.85f)
        cubicTo(
            c.x - s * 0.9f, c.y - s * 0.7f,
            c.x - s * 0.9f, c.y + s * 0.7f,
            c.x + s * 0.2f, c.y + s * 0.85f
        )
        cubicTo(
            c.x - s * 0.35f, c.y + s * 0.5f,
            c.x - s * 0.35f, c.y - s * 0.5f,
            c.x + s * 0.2f, c.y - s * 0.85f
        )
        close()
    }
    drawPath(outer, color = p)
    drawCircle(color = sec, radius = s * 0.08f, center = Offset(c.x + s * 0.38f, c.y - s * 0.18f))
}

private fun DrawScope.drawGothicMist(c: Offset, s: Float, p: Color, sec: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(p.copy(alpha = 0.28f), sec.copy(alpha = 0.08f), Color.Transparent),
            center = c,
            radius = s * 1.45f
        ),
        radius = s * 1.45f,
        center = c
    )
}

private fun DrawScope.drawRibbonBow(c: Offset, s: Float, p: Color, sec: Color) {
    val leftLoop = Path().apply {
        moveTo(c.x, c.y)
        cubicTo(c.x - s * 0.95f, c.y - s * 0.75f, c.x - s * 0.95f, c.y + s * 0.55f, c.x, c.y + s * 0.05f)
        close()
    }
    val rightLoop = Path().apply {
        moveTo(c.x, c.y)
        cubicTo(c.x + s * 0.95f, c.y - s * 0.75f, c.x + s * 0.95f, c.y + s * 0.55f, c.x, c.y + s * 0.05f)
        close()
    }
    drawPath(leftLoop, color = p, style = Stroke(width = s * 0.16f, cap = StrokeCap.Round))
    drawPath(rightLoop, color = p, style = Stroke(width = s * 0.16f, cap = StrokeCap.Round))
    drawLine(color = p, start = c, end = Offset(c.x - s * 0.45f, c.y + s * 0.85f), strokeWidth = s * 0.12f, cap = StrokeCap.Round)
    drawLine(color = p, start = c, end = Offset(c.x + s * 0.45f, c.y + s * 0.85f), strokeWidth = s * 0.12f, cap = StrokeCap.Round)
    drawCircle(color = sec, radius = s * 0.16f, center = c)
}

private fun DrawScope.drawFlowerPetal(c: Offset, s: Float, p: Color, sec: Color, rot: Float) {
    rotate(degrees = rot, pivot = c) {
        for (i in 0 until 5) {
            rotate(degrees = i * 72f, pivot = c) {
                drawOval(
                    color = p,
                    topLeft = Offset(c.x - s * 0.24f, c.y - s * 0.92f),
                    size = Size(s * 0.48f, s * 0.76f)
                )
            }
        }
        drawCircle(color = sec, radius = s * 0.22f, center = c)
    }
}

private fun DrawScope.drawStreetBolt(c: Offset, s: Float, p: Color, sec: Color) {
    val path = Path().apply {
        moveTo(c.x + s * 0.15f, c.y - s * 0.95f)
        lineTo(c.x - s * 0.52f, c.y + s * 0.08f)
        lineTo(c.x - s * 0.04f, c.y + s * 0.08f)
        lineTo(c.x - s * 0.22f, c.y + s * 0.95f)
        lineTo(c.x + s * 0.52f, c.y - s * 0.10f)
        lineTo(c.x + s * 0.05f, c.y - s * 0.10f)
        close()
    }
    drawPath(path, color = p)
    drawPath(path, color = sec, style = Stroke(width = 2f))
}

private fun DrawScope.drawEditorialGrid(c: Offset, s: Float, p: Color, sec: Color) {
    val half = s * 0.9f
    for (i in -1..1) {
        val offset = i * (half * 0.55f)
        drawLine(p, Offset(c.x - half, c.y + offset), Offset(c.x + half, c.y + offset), strokeWidth = 1.5f)
        drawLine(sec, Offset(c.x + offset, c.y - half), Offset(c.x + offset, c.y + half), strokeWidth = 1.5f)
    }
}

private fun DrawScope.drawAtelierFrame(c: Offset, s: Float, p: Color, sec: Color) {
    val arm = s * 0.45f
    drawLine(p, Offset(c.x - s, c.y - s), Offset(c.x - s + arm, c.y - s), strokeWidth = 2.5f)
    drawLine(p, Offset(c.x - s, c.y - s), Offset(c.x - s, c.y - s + arm), strokeWidth = 2.5f)
    drawLine(sec, Offset(c.x + s, c.y + s), Offset(c.x + s - arm, c.y + s), strokeWidth = 2.5f)
    drawLine(sec, Offset(c.x + s, c.y + s), Offset(c.x + s, c.y + s - arm), strokeWidth = 2.5f)
}

private fun DrawScope.drawDiyaFlame(c: Offset, s: Float, p: Color, sec: Color, phase: Float) {
    val sway = sin(phase * 4f * PI.toFloat()) * (s * 0.08f)
    val flame = Path().apply {
        moveTo(c.x, c.y + s * 0.22f)
        cubicTo(c.x - s * 0.32f, c.y - s * 0.15f, c.x - s * 0.18f + sway, c.y - s * 0.65f, c.x + sway, c.y - s * 0.92f)
        cubicTo(c.x + s * 0.22f + sway, c.y - s * 0.62f, c.x + s * 0.32f, c.y - s * 0.15f, c.x, c.y + s * 0.22f)
        close()
    }
    drawPath(flame, color = sec)
    val bowl = Path().apply {
        moveTo(c.x - s * 0.72f, c.y + s * 0.24f)
        quadraticTo(c.x, c.y + s * 0.85f, c.x + s * 0.72f, c.y + s * 0.24f)
        close()
    }
    drawPath(bowl, color = p)
}

private fun DrawScope.drawSnowflake(c: Offset, s: Float, p: Color, sec: Color, rot: Float) {
    rotate(degrees = rot, pivot = c) {
        for (i in 0 until 3) {
            rotate(degrees = i * 60f, pivot = c) {
                drawLine(p, Offset(c.x, c.y - s * 0.9f), Offset(c.x, c.y + s * 0.9f), strokeWidth = 2.5f, cap = StrokeCap.Round)
            }
        }
        drawCircle(sec, radius = s * 0.15f, center = c)
    }
}

private fun DrawScope.drawFirework(c: Offset, s: Float, p: Color, sec: Color, phase: Float) {
    val burst = 0.45f + 0.55f * ((phase * 2f) % 1f)
    for (i in 0 until 8) {
        val rad = (i * 45.0) * PI / 180.0
        val start = Offset(c.x + cos(rad).toFloat() * s * 0.25f, c.y + sin(rad).toFloat() * s * 0.25f)
        val end = Offset(c.x + cos(rad).toFloat() * s * burst, c.y + sin(rad).toFloat() * s * burst)
        drawLine(if (i % 2 == 0) p else sec, start, end, strokeWidth = 2.5f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawConfetti(c: Offset, s: Float, p: Color, sec: Color, rot: Float) {
    translate(left = c.x, top = c.y) {
        rotate(degrees = rot, pivot = Offset.Zero) {
            drawRect(p, topLeft = Offset(-s * 0.55f, -s * 0.35f), size = Size(s * 0.42f, s * 0.18f))
            drawCircle(sec, radius = s * 0.14f, center = Offset(s * 0.32f, s * 0.24f))
        }
    }
}

private fun DrawScope.drawSaleSticker(c: Offset, s: Float, p: Color, sec: Color, rot: Float) {
    rotate(degrees = rot, pivot = c) {
        val path = Path()
        val points = 12
        for (i in 0 until points) {
            val r = if (i % 2 == 0) s * 0.9f else s * 0.68f
            val a = (i * (360f / points)) * PI / 180.0
            val px = c.x + cos(a).toFloat() * r
            val py = c.y + sin(a).toFloat() * r
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        drawPath(path, color = p, style = Stroke(width = 2.5f))
        drawCircle(sec, radius = s * 0.3f, center = c)
    }
}

private fun defaultMotifsForSlug(slug: String, primary: Color, secondary: Color): List<WorldVectorAsset> {
    val key = slug.lowercase()
    val pHex = colorToHex(primary)
    val sHex = colorToHex(secondary)
    return when {
        key.contains("y2k") -> listOf(
            WorldVectorAsset(assetKey = "y2k_1", vectorType = "pixel_heart", layerRole = "accent", primaryHex = pHex, secondaryHex = sHex, opacity = 0.48f),
            WorldVectorAsset(assetKey = "y2k_2", vectorType = "chrome_sparkle", layerRole = "accent", primaryHex = sHex, secondaryHex = pHex, opacity = 0.42f),
            WorldVectorAsset(assetKey = "y2k_3", vectorType = "pixel_star", layerRole = "foreground", primaryHex = pHex, secondaryHex = sHex, opacity = 0.35f)
        )
        key.contains("goth") -> listOf(
            WorldVectorAsset(assetKey = "goth_1", vectorType = "goth_raven", layerRole = "accent", primaryHex = pHex, secondaryHex = sHex, opacity = 0.42f),
            WorldVectorAsset(assetKey = "goth_2", vectorType = "crescent_moon", layerRole = "accent", primaryHex = sHex, secondaryHex = pHex, opacity = 0.36f),
            WorldVectorAsset(assetKey = "goth_3", vectorType = "gothic_mist", layerRole = "background", primaryHex = pHex, secondaryHex = sHex, opacity = 0.35f)
        )
        key.contains("coquette") -> listOf(
            WorldVectorAsset(assetKey = "coq_1", vectorType = "ribbon_bow", layerRole = "accent", primaryHex = pHex, secondaryHex = sHex, opacity = 0.45f),
            WorldVectorAsset(assetKey = "coq_2", vectorType = "flower_petal", layerRole = "accent", primaryHex = sHex, secondaryHex = pHex, opacity = 0.36f)
        )
        key.contains("street") -> listOf(
            WorldVectorAsset(assetKey = "st_1", vectorType = "street_bolt", layerRole = "accent", primaryHex = pHex, secondaryHex = sHex, opacity = 0.44f),
            WorldVectorAsset(assetKey = "st_2", vectorType = "editorial_grid", layerRole = "background", primaryHex = sHex, secondaryHex = pHex, opacity = 0.25f)
        )
        key.contains("k_fashion") || key.contains("k-fashion") || key.contains("seoul") -> listOf(
            WorldVectorAsset(assetKey = "kf_1", vectorType = "editorial_grid", layerRole = "background", primaryHex = pHex, secondaryHex = sHex, opacity = 0.28f),
            WorldVectorAsset(assetKey = "kf_2", vectorType = "atelier_frame", layerRole = "accent", primaryHex = sHex, secondaryHex = pHex, opacity = 0.35f)
        )
        key.contains("diwali") -> listOf(
            WorldVectorAsset(assetKey = "diw_1", vectorType = "diya_flame", layerRole = "accent", primaryHex = pHex, secondaryHex = sHex, opacity = 0.48f),
            WorldVectorAsset(assetKey = "diw_2", vectorType = "firework", layerRole = "foreground", primaryHex = sHex, secondaryHex = pHex, opacity = 0.38f)
        )
        key.contains("winter") || key.contains("christmas") -> listOf(
            WorldVectorAsset(assetKey = "win_1", vectorType = "snowflake", layerRole = "accent", primaryHex = pHex, secondaryHex = sHex, opacity = 0.42f),
            WorldVectorAsset(assetKey = "win_2", vectorType = "chrome_sparkle", layerRole = "foreground", primaryHex = sHex, secondaryHex = pHex, opacity = 0.34f)
        )
        key.contains("sale") -> listOf(
            WorldVectorAsset(assetKey = "sal_1", vectorType = "sale_sticker", layerRole = "accent", primaryHex = pHex, secondaryHex = sHex, opacity = 0.44f),
            WorldVectorAsset(assetKey = "sal_2", vectorType = "confetti", layerRole = "foreground", primaryHex = sHex, secondaryHex = pHex, opacity = 0.36f)
        )
        else -> emptyList()
    }
}

private fun colorToHex(color: Color): String {
    val r = (color.red * 255f).toInt().coerceIn(0, 255)
    val g = (color.green * 255f).toInt().coerceIn(0, 255)
    val b = (color.blue * 255f).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}
