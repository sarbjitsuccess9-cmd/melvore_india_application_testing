package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.RunwayConfig
import com.example.model.RunwayModelSpec
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.MelvoreBodyFontFamily
import com.example.ui.theme.MelvorePixelFontFamily
import com.example.ui.theme.ensureContrast
import com.example.ui.theme.parseHexColor
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Designed Pixel Fashion Runway showcase section for Y2K (or any backend-enabled universe).
 * Non-blocking: placed inline in scroll flow, never covers hero or interactive shopping controls.
 */
@Composable
fun PixelFashionRunwaySection(
    config: RunwayConfig,
    accentColor: Color,
    secondaryColor: Color,
    reducedMotion: Boolean = false,
    onExploreCollection: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!config.enabled) return

    val surfaces = LocalMelvoreSurfaces.current
    val stageBg = remember(config.backgroundHex, surfaces.isDark, surfaces.surfaceElevated) {
        if (surfaces.isDark) {
            parseHexColor(config.backgroundHex, surfaces.surfaceElevated)
        } else {
            surfaces.surfaceElevated
        }
    }
    val stageInk = ensureContrast(surfaces.onSurface, stageBg)
    val stageMutedInk = ensureContrast(surfaces.onSurfaceMuted, stageBg, minRatio = 3.5f)
    val models = remember(config.models) {
        if (config.models.isNotEmpty()) config.models else defaultY2kRunwayModels()
    }

    val cycleMs = remember(config.speed) {
        (9500f / config.speed.coerceIn(0.4f, 2.5f)).roundToInt().coerceIn(3200, 22000)
    }

    val animationsEnabled = rememberAnimationsEnabled() && !reducedMotion
    val phase = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "pixel_runway_loop")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = cycleMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "runway_phase"
        )
        p
    } else 0.35f

    val activeModelIndex = ((phase * models.size).toInt()).coerceIn(0, (models.size - 1).coerceAtLeast(0))
    val activeModel = models.getOrNull(activeModelIndex)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = stageBg,
        border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(accentColor, secondaryColor))),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pixel_runway_section")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = config.title.uppercase(),
                        fontFamily = MelvorePixelFontFamily,
                        fontSize = 13.sp,
                        color = ensureContrast(secondaryColor, stageBg),
                        letterSpacing = 1.sp
                    )
                    if (config.subtitle.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = config.subtitle,
                            fontFamily = MelvoreBodyFontFamily,
                            fontSize = 11.sp,
                            color = stageMutedInk
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = accentColor.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, accentColor)
                ) {
                    Text(
                        text = "LIVE RUNWAY",
                        fontFamily = MelvorePixelFontFamily,
                        fontSize = 9.sp,
                        color = stageInk,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Animated Pixel Catwalk Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, secondaryColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            ) {
                if (!config.artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = config.artworkUrl,
                        contentDescription = config.title,
                        contentScale = ContentScale.Crop,
                        alpha = 0.26f,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Gradient cyber backdrop
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                stageBg.copy(alpha = 0.92f),
                                stageBg.copy(alpha = 0.85f),
                                surfaces.surface
                            )
                        )
                    )

                    // Overhead stage spotlights
                    val spotX1 = w * (0.28f + 0.08f * sin(phase * 2.0 * PI).toFloat())
                    val spotX2 = w * (0.72f - 0.08f * sin(phase * 2.0 * PI).toFloat())
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.25f), Color.Transparent),
                            center = Offset(spotX1, h * 0.2f),
                            radius = w * 0.34f
                        ),
                        center = Offset(spotX1, h * 0.2f),
                        radius = w * 0.34f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(secondaryColor.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(spotX2, h * 0.2f),
                            radius = w * 0.34f
                        ),
                        center = Offset(spotX2, h * 0.2f),
                        radius = w * 0.34f
                    )

                    // Runway deck & chrome perspective grid
                    val deckTop = h * 0.72f
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.22f),
                                surfaces.border.copy(alpha = 0.45f)
                            )
                        ),
                        topLeft = Offset(0f, deckTop),
                        size = Size(w, h - deckTop)
                    )
                    drawLine(
                        brush = Brush.horizontalGradient(listOf(accentColor, secondaryColor, accentColor)),
                        start = Offset(0f, deckTop),
                        end = Offset(w, deckTop),
                        strokeWidth = 2.5f
                    )
                    // Animated runway floor chevron tiles
                    val tileCount = 10
                    val stepW = w / tileCount
                    val shift = (phase * stepW * 2f) % stepW
                    for (t in 0..tileCount + 1) {
                        val xLine = t * stepW - shift
                        drawLine(
                            color = secondaryColor.copy(alpha = 0.24f),
                            start = Offset(xLine, deckTop),
                            end = Offset(xLine - w * 0.08f, h),
                            strokeWidth = 1.4f
                        )
                    }

                    // Render Pixel-Art Runway Models seamlessly looping left -> right
                    val pixel = (h / 34f).coerceIn(3.5f, 6.0f)
                    val strideWidth = w + models.size * (w * 0.36f)
                    models.forEachIndexed { idx, spec ->
                        val spacing = strideWidth / models.size.coerceAtLeast(1)
                        val rawX = ((phase * strideWidth) + idx * spacing) % strideWidth
                        val centerX = rawX - w * 0.18f
                        if (centerX < -pixel * 12 || centerX > w + pixel * 12) return@forEachIndexed

                        val walkTick = ((phase * 24f + idx * 3f).toInt() % 4)
                        val bobY = if (walkTick == 1 || walkTick == 3) -pixel * 0.65f else 0f
                        val footShift = when (walkTick) {
                            1 -> pixel
                            3 -> -pixel
                            else -> 0f
                        }

                        val c1 = parseHexColor(spec.palette.getOrNull(0), accentColor)
                        val c2 = parseHexColor(spec.palette.getOrNull(1), surfaces.border)
                        val c3 = parseHexColor(spec.palette.getOrNull(2), secondaryColor)
                        val skin = Color(0xFFF3C6A5)
                        val hair = when (idx % 3) {
                            0 -> Color(0xFF3B2A63)
                            1 -> Color(0xFFFFD6E8)
                            else -> Color(0xFF334155)
                        }

                        // Ground reflection shadow
                        drawOval(
                            color = accentColor.copy(alpha = 0.26f),
                            topLeft = Offset(centerX - pixel * 4.5f, deckTop + pixel * 0.4f),
                            size = Size(pixel * 9f, pixel * 2.1f)
                        )

                        val baseBottom = deckTop + pixel * 0.8f + bobY
                        fun px(gx: Float, gyFromBottom: Float, pw: Float = 1f, ph: Float = 1f, color: Color) {
                            drawRect(
                                color = color,
                                topLeft = Offset(centerX + gx * pixel, baseBottom - gyFromBottom * pixel),
                                size = Size(pw * pixel + 0.5f, ph * pixel + 0.5f)
                            )
                        }

                        // Platform boots / chunky sneakers (alternating walk stride)
                        drawRect(
                            color = c2,
                            topLeft = Offset(centerX - 2.6f * pixel + footShift * 0.7f, baseBottom - 2.2f * pixel),
                            size = Size(2.2f * pixel, 2.2f * pixel)
                        )
                        drawRect(
                            color = c2,
                            topLeft = Offset(centerX + 0.6f * pixel - footShift * 0.7f, baseBottom - 2.2f * pixel),
                            size = Size(2.2f * pixel, 2.2f * pixel)
                        )

                        // Legs / Cargo pants / Skirt silhouette
                        when (spec.silhouette.lowercase()) {
                            "metallic_puffer", "cargo_baby_tee" -> {
                                px(-2.4f, 8.2f, 2.0f, 6.0f, c3)
                                px(0.4f, 8.2f, 2.0f, 6.0f, c3)
                            }
                            else -> {
                                px(-1.8f, 6.0f, 1.4f, 3.8f, skin)
                                px(0.4f, 6.0f, 1.4f, 3.8f, skin)
                                // Glossy micro mini skirt
                                px(-2.8f, 8.4f, 5.6f, 2.6f, c1)
                            }
                        }

                        // Chrome belt
                        px(-2.6f, 9.1f, 5.2f, 0.9f, c2)

                        // Top / Jacket torso
                        if (spec.silhouette.equals("metallic_puffer", ignoreCase = true)) {
                            px(-3.2f, 14.5f, 6.4f, 5.4f, c1)
                            px(-4.2f, 13.8f, 1.4f, 4.4f, c2)
                            px(2.8f, 13.8f, 1.4f, 4.4f, c2)
                        } else {
                            px(-2.5f, 14.4f, 5.0f, 5.2f, c1)
                            px(-3.5f, 13.8f, 1.1f, 4.0f, skin)
                            px(2.4f, 13.8f, 1.1f, 4.0f, skin)
                        }

                        // Neck & head
                        px(-0.8f, 15.6f, 1.6f, 1.3f, skin)
                        px(-2.2f, 19.6f, 4.4f, 4.0f, skin)
                        // Hair & crown
                        px(-2.6f, 21.0f, 5.2f, 2.0f, hair)
                        px(-2.8f, 20.0f, 1.0f, 3.6f, hair)
                        px(1.8f, 20.0f, 1.0f, 3.6f, hair)
                        // Cyber Y2K visor shades
                        px(-2.0f, 18.0f, 4.0f, 1.1f, secondaryColor)
                        px(-1.4f, 18.0f, 1.0f, 0.5f, surfaces.onImageOverlay)
                    }
                }
            }

            // Active Spotlight Model Card & Collection CTA
            if (activeModel != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${activeModel.name} · ${activeModel.role}",
                            fontFamily = MelvorePixelFontFamily,
                            fontSize = 10.sp,
                            color = ensureContrast(accentColor, stageBg)
                        )
                        if (activeModel.styleNote.isNotBlank()) {
                            Text(
                                text = activeModel.styleNote,
                                fontFamily = MelvoreBodyFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = stageInk
                            )
                        }
                    }
                    if (onExploreCollection != null && config.collectionSlug.isNotBlank()) {
                        Spacer(Modifier.width(10.dp))
                        MelvoreWorldActionButton(
                            text = "Shop Look",
                            buttonStyleOverride = "y2k_pixel_chrome",
                            compact = true,
                            leadingIcon = Icons.Filled.AutoAwesome,
                            onClick = { onExploreCollection(config.collectionSlug) },
                            modifier = Modifier.testTag("pixel_runway_shop_button")
                        )
                    }
                }
            }
        }
    }
}

private fun defaultY2kRunwayModels(): List<RunwayModelSpec> = listOf(
    RunwayModelSpec(
        name = "NOVA-01",
        role = "CYBER Y2K",
        palette = listOf("#FF3D9A", "#E2E8F0", "#22D3EE"),
        styleNote = "Chrome Crop Jacket + Parachute Cargo",
        silhouette = "cargo_baby_tee"
    ),
    RunwayModelSpec(
        name = "PIXEL-02",
        role = "CLUB KRONOS",
        palette = listOf("#22D3EE", "#C084FC", "#FFFFFF"),
        styleNote = "Glossy Micro Skirt + Platform Boot",
        silhouette = "mini_skirt_platform"
    ),
    RunwayModelSpec(
        name = "CHROMA-03",
        role = "SILVER MATRIX",
        palette = listOf("#C084FC", "#FF3D9A", "#0EA5E9"),
        styleNote = "Reflective Puffer + Rimless Visor",
        silhouette = "metallic_puffer"
    )
)
