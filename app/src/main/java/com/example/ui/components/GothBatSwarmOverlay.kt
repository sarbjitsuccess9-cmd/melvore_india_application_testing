package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.ui.theme.LocalFashionWorldTheme
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.parseHexColor
import kotlin.math.PI
import kotlin.math.sin

private data class BatFlightPath(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val scale: Float,
    val flapPhase: Float,
    val delayFraction: Float
)

/**
 * Brief, atmospheric Bat Swarm Transition Overlay played ONCE when entering the Goth world.
 * Never intercepts pointer events and stops cleanly without replaying during scroll/recomposition.
 */
@Composable
fun GothBatSwarmOverlay(
    entryKey: Any,
    enabled: Boolean,
    reducedMotion: Boolean = false,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = rememberAnimationsEnabled() && !reducedMotion
    if (!enabled || !animationsEnabled) return

    val worldTheme = LocalFashionWorldTheme.current
    val surfaces = LocalMelvoreSurfaces.current
    val crimson = parseHexColor(worldTheme.accentHex, surfaces.sale)
    var finished by remember(entryKey) { mutableStateOf(false) }
    val progress = remember(entryKey) { Animatable(0f) }

    LaunchedEffect(entryKey) {
        finished = false
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2500, easing = FastOutSlowInEasing)
        )
        finished = true
    }

    if (finished) return

    val bats = remember {
        listOf(
            BatFlightPath(-0.08f, 0.34f, 1.12f, 0.14f, 1.15f, 0.0f, 0.00f),
            BatFlightPath(1.08f, 0.24f, -0.10f, 0.08f, 0.92f, 1.4f, 0.06f),
            BatFlightPath(-0.06f, 0.56f, 1.08f, 0.28f, 0.85f, 2.2f, 0.10f),
            BatFlightPath(1.10f, 0.44f, -0.08f, 0.19f, 1.05f, 3.1f, 0.14f),
            BatFlightPath(0.18f, 0.86f, 0.92f, 0.06f, 0.72f, 4.0f, 0.18f),
            BatFlightPath(0.84f, 0.82f, 0.08f, 0.04f, 0.78f, 5.2f, 0.22f),
            BatFlightPath(-0.05f, 0.18f, 0.95f, 0.42f, 0.68f, 2.7f, 0.26f)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val tGlobal = progress.value

        bats.forEach { bat ->
            val localT = ((tGlobal - bat.delayFraction) / (1f - bat.delayFraction)).coerceIn(0f, 1f)
            if (localT <= 0f || localT >= 1f) return@forEach

            val alphaEnv = when {
                localT < 0.16f -> localT / 0.16f
                localT > 0.78f -> (1f - localT) / 0.22f
                else -> 1f
            }.coerceIn(0f, 1f)

            val cx = w * (bat.startX + (bat.endX - bat.startX) * localT)
            val arcY = sin(localT * PI).toFloat() * -0.07f * h
            val cy = h * (bat.startY + (bat.endY - bat.startY) * localT) + arcY

            val wingFlap = sin((localT * 18f + bat.flapPhase).toDouble()).toFloat()
            val wingLift = wingFlap * 14f * bat.scale
            val baseSpan = 28f * bat.scale

            val tiltDeg = if (bat.endX > bat.startX) -10f else 10f
            rotate(degrees = tiltDeg, pivot = Offset(cx, cy)) {
                val batPath = Path().apply {
                    // Center body
                    moveTo(cx, cy - 6f * bat.scale)
                    // Left ear + upper wing
                    lineTo(cx - 4f * bat.scale, cy - 11f * bat.scale)
                    lineTo(cx - 7f * bat.scale, cy - 4f * bat.scale)
                    cubicTo(
                        cx - baseSpan * 0.55f, cy - 14f * bat.scale - wingLift,
                        cx - baseSpan * 0.9f, cy - 4f * bat.scale - wingLift,
                        cx - baseSpan, cy + 4f * bat.scale - wingLift * 0.6f
                    )
                    // Scalloped left underwing
                    quadraticTo(cx - baseSpan * 0.52f, cy + 1f * bat.scale, cx - baseSpan * 0.28f, cy + 6f * bat.scale)
                    lineTo(cx, cy + 9f * bat.scale)
                    // Scalloped right underwing
                    lineTo(cx + baseSpan * 0.28f, cy + 6f * bat.scale)
                    quadraticTo(cx + baseSpan * 0.52f, cy + 1f * bat.scale, cx + baseSpan, cy + 4f * bat.scale - wingLift * 0.6f)
                    // Right upper wing + ear
                    cubicTo(
                        cx + baseSpan * 0.9f, cy - 4f * bat.scale - wingLift,
                        cx + baseSpan * 0.55f, cy - 14f * bat.scale - wingLift,
                        cx + 7f * bat.scale, cy - 4f * bat.scale
                    )
                    lineTo(cx + 4f * bat.scale, cy - 11f * bat.scale)
                    close()
                }
                drawPath(batPath, color = surfaces.imageOverlay.copy(alpha = 0.88f * alphaEnv))
                drawPath(
                    batPath,
                    color = crimson.copy(alpha = 0.45f * alphaEnv),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.6f)
                )
                // Tiny crimson glint eyes
                drawCircle(
                    color = crimson.copy(alpha = 0.78f * alphaEnv),
                    radius = 1.6f * bat.scale,
                    center = Offset(cx - 2.2f * bat.scale, cy - 3.5f * bat.scale)
                )
                drawCircle(
                    color = crimson.copy(alpha = 0.78f * alphaEnv),
                    radius = 1.6f * bat.scale,
                    center = Offset(cx + 2.2f * bat.scale, cy - 3.5f * bat.scale)
                )
            }
        }
    }
}
