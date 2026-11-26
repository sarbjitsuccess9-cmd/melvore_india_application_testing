package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalMelvoreColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Controlled allowlist of backend-configurable animation keys.
 * Unknown or unapproved backend keys gracefully degrade to NONE (static presentation).
 */
enum class MelvoreAnimationKey {
    SHIMMER_BUTTON,
    METALLIC_SHINE,
    SMOOTH_REVEAL,
    PARALLAX_SCROLL,
    DIWALI_GLOW,
    HALLOWEEN_SMOKE,
    CHRISTMAS_SNOW,
    NEWYEAR_TRAILS,
    THANKSGIVING_WARMTH,
    COQUETTE_PETALS,
    Y2K_CHROME,
    GOTHIC_MIST,
    NONE;

    companion object {
        fun fromBackendKey(raw: String?): MelvoreAnimationKey {
            val normalized = raw?.trim()?.uppercase()?.replace("-", "_") ?: return NONE
            return entries.firstOrNull { it.name == normalized } ?: NONE
        }
    }
}

@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        }.getOrDefault(true)
    }
}

/**
 * Subtle editorial image scale settle (`1.025 -> 1.0`) when a slide or hero card enters view.
 */
@Composable
fun SlowEditorialZoomBox(
    triggerKey: Any?,
    modifier: Modifier = Modifier,
    initialScale: Float = 1.028f,
    durationMs: Int = 950,
    content: @Composable BoxScope.() -> Unit
) {
    val animationsEnabled = rememberAnimationsEnabled()
    var settled by remember(triggerKey) { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(triggerKey, animationsEnabled) {
        if (animationsEnabled) {
            settled = false
            settled = true
        }
    }
    val scale by animateFloatAsState(
        targetValue = if (!animationsEnabled || settled) 1.0f else initialScale,
        animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing),
        label = "editorial_zoom_settle"
    )
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        content = content
    )
}

/**
 * Luxury gold shimmer eyebrow label for Luxe & Enter Luxe destinations.
 */
@Composable
fun GoldShimmerEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    baseColor: Color = Color(0xFFD4AF37),
    letterSpacing: TextUnit = 2.sp,
    style: TextStyle = MaterialTheme.typography.labelSmall
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val sweep = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "gold_eyebrow_shimmer")
        val p by transition.animateFloat(
            initialValue = -0.4f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "gold_sweep"
        )
        p
    } else -1f

    val brush = if (animationsEnabled && sweep > -0.35f) {
        Brush.linearGradient(
            colorStops = arrayOf(
                (sweep - 0.25f).coerceIn(0f, 1f) to baseColor,
                sweep.coerceIn(0f, 1f) to Color(0xFFFFF3C4),
                (sweep + 0.25f).coerceIn(0f, 1f) to baseColor
            )
        )
    } else null

    Text(
        text = text,
        style = if (brush != null) style.copy(brush = brush) else style.copy(color = baseColor),
        fontWeight = FontWeight.Bold,
        letterSpacing = letterSpacing,
        modifier = modifier
    )
}

@Composable
fun MetallicShineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    borderColor: Color = Color(0x55FFFFFF),
    shape: Shape = RoundedCornerShape(4.dp),
    durationMs: Int = 2600,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val progress = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "metallic_shine")
        val anim by transition.animateFloat(
            initialValue = -0.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMs.coerceIn(1400, 6000), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shine_sweep"
        )
        anim
    } else -1f

    Surface(
        onClick = onClick,
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            val shimmerTint = com.example.ui.theme.LocalMelvoreSurfaces.current.onImageOverlay
            if (animationsEnabled && progress > -0.45f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colorStops = arrayOf(
                                    (progress - 0.25f).coerceIn(0f, 1f) to Color.Transparent,
                                    progress.coerceIn(0f, 1f) to shimmerTint.copy(alpha = 0.24f),
                                    (progress + 0.25f).coerceIn(0f, 1f) to Color.Transparent
                                )
                            )
                        )
                )
            }
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(contentPadding),
                    content = content
                )
            }
        }
    }
}

@Composable
fun MelvoreAnimatedEntrance(
    visible: Boolean = true,
    delayMs: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animationsEnabled = rememberAnimationsEnabled()
    var shown by remember { mutableStateOf(!animationsEnabled || delayMs == 0) }

    LaunchedEffect(visible, animationsEnabled) {
        if (!animationsEnabled) {
            shown = true
        } else if (visible) {
            if (delayMs > 0) delay(delayMs.toLong())
            shown = true
        }
    }

    if (!animationsEnabled) {
        Box(modifier = modifier) { content() }
    } else {
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn(tween(360)) + slideInVertically(tween(360)) { it / 12 },
            modifier = modifier
        ) {
            content()
        }
    }
}

/**
 * Quick-add button with tactile spring feedback and brief emerald "Added" confirmation.
 */
@Composable
fun PremiumQuickAddButton(
    inStock: Boolean,
    productName: String,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val ext = LocalMelvoreColors.current
    var justAdded by remember { mutableStateOf(false) }

    LaunchedEffect(justAdded) {
        if (justAdded) {
            delay(1100)
            justAdded = false
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (justAdded) 1.04f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "quick_add_scale"
    )

    Button(
        onClick = {
            justAdded = true
            onAddClick()
        },
        enabled = inStock,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (justAdded) ext.emerald else containerColor,
            contentColor = if (justAdded) com.example.ui.theme.ensureContrast(
                com.example.ui.theme.LocalMelvoreSurfaces.current.onAccent,
                ext.emerald
            ) else contentColor
        ),
        contentPadding = if (compact) {
            PaddingValues(horizontal = 11.dp, vertical = 0.dp)
        } else {
            PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        },
        modifier = modifier
            .heightIn(min = 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        if (!inStock) {
            Text("Sold Out", style = MaterialTheme.typography.labelSmall)
        } else if (justAdded) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Added $productName",
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Added", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        } else {
            Icon(
                imageVector = Icons.Filled.ShoppingBag,
                contentDescription = "Add $productName to bag",
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(if (compact) "Add" else "Add to Bag", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun CampaignAtmosphericEffect(
    animationKey: MelvoreAnimationKey,
    accentColor: Color,
    durationMs: Int = 3600,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val phase = if (animationsEnabled && animationKey != MelvoreAnimationKey.NONE) {
        val transition = rememberInfiniteTransition(label = "campaign_atmosphere")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMs.coerceIn(2000, 9000), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "atmosphere_phase"
        )
        p
    } else 0f

    Box(modifier = modifier) {
        content()
        if (animationsEnabled && animationKey != MelvoreAnimationKey.NONE) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                when (animationKey) {
                    MelvoreAnimationKey.DIWALI_GLOW,
                    MelvoreAnimationKey.THANKSGIVING_WARMTH -> {
                        for (i in 0 until 14) {
                            val fx = ((i * 0.173f + phase * 0.25f) % 1f) * w
                            val fy = ((i * 0.291f + sin((phase * 2 * PI + i).toDouble()).toFloat() * 0.08f) % 1f) * h
                            val r = (6f + (i % 4) * 4f)
                            drawCircle(
                                color = accentColor.copy(alpha = 0.22f),
                                radius = r,
                                center = Offset(fx, fy)
                            )
                        }
                    }

                    MelvoreAnimationKey.CHRISTMAS_SNOW,
                    MelvoreAnimationKey.COQUETTE_PETALS -> {
                        val tint = if (animationKey == MelvoreAnimationKey.COQUETTE_PETALS) {
                            accentColor.copy(alpha = 0.65f)
                        } else accentColor.copy(alpha = 0.35f)
                        for (i in 0 until 16) {
                            val fx = ((i * 0.19f + sin((phase * 2 * PI + i).toDouble()).toFloat() * 0.05f + 1f) % 1f) * w
                            val fy = ((i * 0.13f + phase) % 1f) * h
                            drawCircle(
                                color = tint.copy(alpha = 0.26f),
                                radius = 3.5f + (i % 3) * 2.2f,
                                center = Offset(fx, fy)
                            )
                        }
                    }

                    MelvoreAnimationKey.HALLOWEEN_SMOKE,
                    MelvoreAnimationKey.GOTHIC_MIST -> {
                        for (i in 0 until 5) {
                            val cx = ((i * 0.23f + phase * 0.35f) % 1.2f - 0.1f) * w
                            val cy = h * (0.25f + (i % 3) * 0.25f)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(accentColor.copy(alpha = 0.14f), Color.Transparent),
                                    center = Offset(cx, cy),
                                    radius = w * 0.28f
                                ),
                                radius = w * 0.28f,
                                center = Offset(cx, cy)
                            )
                        }
                    }

                    MelvoreAnimationKey.Y2K_CHROME,
                    MelvoreAnimationKey.NEWYEAR_TRAILS,
                    MelvoreAnimationKey.METALLIC_SHINE,
                    MelvoreAnimationKey.SHIMMER_BUTTON -> {
                        val angleOffset = cos((phase * 2 * PI).toDouble()).toFloat() * 0.15f
                        drawLine(
                            color = accentColor.copy(alpha = 0.22f),
                            start = Offset(w * (0.1f + phase * 0.7f), 0f),
                            end = Offset(w * (0.35f + phase * 0.7f + angleOffset), h),
                            strokeWidth = 3f
                        )
                    }

                    else -> Unit
                }
            }
        }
    }
}

@Composable
fun WishlistHeartOverlayButton(
    isWishlisted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isWishlisted) 1.18f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "wishlist_bounce"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(onClick = onClick)
    ) {
        content()
    }
}
