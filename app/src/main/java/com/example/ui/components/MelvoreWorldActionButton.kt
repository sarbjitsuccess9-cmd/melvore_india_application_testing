package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.lerp
import com.example.ui.theme.LocalFashionWorldTheme
import com.example.ui.theme.LocalMelvoreDesignManifest
import com.example.ui.theme.LocalMelvoreSurfaces
import com.example.ui.theme.MelvoreBodyFontFamily
import com.example.ui.theme.MelvoreGothicFontFamily
import com.example.ui.theme.MelvorePixelFontFamily
import com.example.ui.theme.MelvoreSerifFontFamily
import com.example.ui.theme.ensureContrast
import com.example.ui.theme.parseHexColor

enum class MelvoreWorldButtonState {
    Default,
    Loading,
    Disabled,
    Success,
    Error
}

@Composable
fun MelvoreWorldActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonStyleOverride: String? = null,
    state: MelvoreWorldButtonState = MelvoreWorldButtonState.Default,
    leadingIcon: ImageVector? = null,
    compact: Boolean = false,
    fillWidth: Boolean = false,
    ghost: Boolean = false,
    svgPathOverride: String? = null
) {
    val worldTheme = LocalFashionWorldTheme.current
    val manifest = LocalMelvoreDesignManifest.current
    val surfaces = LocalMelvoreSurfaces.current
    val worldKit = RemoteDesignRegistry.activeWorldKit(manifest, worldTheme)
    val svgButtonPath = remember(svgPathOverride, worldKit, ghost, state) {
        if (state != MelvoreWorldButtonState.Default) null
        else svgPathOverride?.takeIf { it.isNotBlank() }
            ?: (if (ghost) worldKit?.svg?.buttonGhost else worldKit?.svg?.button)?.takeIf { it.isNotBlank() }
    }
    val styleKey = remember(buttonStyleOverride, worldTheme.resolvedActionButtonStyle) {
        (buttonStyleOverride?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            ?: worldTheme.resolvedActionButtonStyle)
    }

    val animationsEnabled = rememberAnimationsEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && state == MelvoreWorldButtonState.Default) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "world_btn_press"
    )

    val sweepProgress = if (animationsEnabled &&
        (styleKey == "y2k_pixel_chrome" || styleKey == "metallic_pill" || styleKey == "gold_foil" || styleKey == "diwali_gold" || styleKey == "halloween_pumpkin")
    ) {
        val transition = rememberInfiniteTransition(label = "world_btn_sweep")
        val p by transition.animateFloat(
            initialValue = -0.3f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sweep"
        )
        p
    } else -1f

    val accentColor = parseHexColor(worldTheme.accentHex, surfaces.accent)
    val primaryColor = parseHexColor(worldTheme.primaryHex, surfaces.accent)
    val secondaryColor = parseHexColor(worldTheme.secondaryAccentHex, surfaces.surfaceElevated)
    val baseRadius = manifest.tokens.radii.buttonDp.coerceIn(0, 999).dp

    val shape: Shape = when (styleKey) {
        "pixel_block" -> RoundedCornerShape(0.dp)
        "y2k_pixel_chrome", "metallic_pill" -> RoundedCornerShape(999.dp)
        "goth_fang", "bat_wing" -> CutCornerShape(topStart = 8.dp, topEnd = 2.dp, bottomEnd = 8.dp, bottomStart = 2.dp)
        "coquette_ribbon", "ribbon_scallop", "soft_pill", "gold_foil" -> RoundedCornerShape(999.dp)
        "halloween_pumpkin" -> RoundedCornerShape(18.dp)
        "diwali_gold" -> RoundedCornerShape(12.dp)
        "streetwear_utility", "street_block" -> RoundedCornerShape(0.dp)
        "kfashion_minimal", "flat_mono", "torn_paper", "ticket_crest" -> RoundedCornerShape(4.dp)
        else -> RoundedCornerShape(baseRadius)
    }

    val (bgStart, bgEnd, rawContent, borderColor) = when {
        state == MelvoreWorldButtonState.Success -> {
            val sEnd = lerp(surfaces.success, surfaces.surfaceElevated, 0.14f)
            Quad(surfaces.success, sEnd, surfaces.onAccent, surfaces.success)
        }
        state == MelvoreWorldButtonState.Error -> {
            val eEnd = lerp(surfaces.sale, surfaces.surfaceElevated, 0.14f)
            Quad(surfaces.sale, eEnd, surfaces.onAccent, surfaces.sale)
        }
        styleKey == "coquette_ribbon" || styleKey == "ribbon_scallop" -> {
            val softStart = lerp(surfaces.surfaceElevated, accentColor, if (surfaces.isDark) 0.32f else 0.24f)
            val softEnd = lerp(surfaces.surface, accentColor, if (surfaces.isDark) 0.42f else 0.34f)
            Quad(softStart, softEnd, surfaces.onSurface, accentColor)
        }
        styleKey == "y2k_pixel_chrome" || styleKey == "metallic_pill" -> {
            val end = lerp(accentColor, secondaryColor, 0.45f)
            Quad(accentColor, end, surfaces.onAccent, surfaces.border)
        }
        styleKey == "goth_fang" || styleKey == "bat_wing" || styleKey == "gold_foil" ||
            styleKey == "pixel_block" || styleKey == "halloween_pumpkin" ||
            styleKey == "streetwear_utility" || styleKey == "street_block" -> {
            // Honor dark filled buttons only in dark mode; in light mode use vibrant world accent or elevated surface
            if (surfaces.isDark) {
                val start = lerp(surfaces.surfaceElevated, accentColor, 0.18f)
                val end = lerp(surfaces.surface, accentColor, 0.10f)
                Quad(start, end, accentColor, accentColor)
            } else {
                val start = primaryColor
                val end = lerp(primaryColor, accentColor, 0.35f)
                Quad(start, end, surfaces.onAccent, accentColor)
            }
        }
        styleKey == "diwali_gold" -> {
            val end = lerp(accentColor, primaryColor, 0.35f)
            Quad(accentColor, end, surfaces.onAccent, surfaces.border)
        }
        styleKey == "kfashion_minimal" -> {
            val start = if (surfaces.isDark) surfaces.surfaceElevated else primaryColor
            val end = lerp(start, accentColor, 0.22f)
            Quad(start, end, if (surfaces.isDark) surfaces.onSurface else surfaces.onAccent, surfaces.border)
        }
        else -> {
            val start = primaryColor
            val end = lerp(primaryColor, accentColor, 0.30f)
            Quad(start, end, surfaces.onAccent, accentColor.copy(alpha = 0.45f))
        }
    }

    val bgMid = lerp(bgStart, bgEnd, 0.5f)
    val contentColor = ensureContrast(rawContent, bgMid)
    val bgBrush = Brush.horizontalGradient(listOf(bgStart, bgEnd))

    val isPixelBtn = styleKey == "pixel_block" || styleKey == "y2k_pixel_chrome" || worldTheme.pixelText
    val fontFamily = when {
        isPixelBtn -> MelvorePixelFontFamily
        styleKey == "goth_fang" || styleKey == "bat_wing" -> MelvoreGothicFontFamily
        styleKey == "coquette_ribbon" || styleKey == "ribbon_scallop" || styleKey == "gold_foil" -> MelvoreSerifFontFamily
        else -> MelvoreBodyFontFamily
    }

    val isEnabled = state != MelvoreWorldButtonState.Disabled && state != MelvoreWorldButtonState.Loading
    val effectiveContentColor = if (ghost && state == MelvoreWorldButtonState.Default) {
        ensureContrast(if (surfaces.isDark) surfaces.onSurface else accentColor, surfaces.background)
    } else {
        contentColor
    }
    val effectiveBackgroundModifier = if (ghost && state == MelvoreWorldButtonState.Default) {
        Modifier.background(Color.Transparent, shape)
    } else {
        Modifier.background(bgBrush, shape)
    }

    Surface(
        shape = shape,
        color = Color.Transparent,
        border = if (svgButtonPath != null) null else BorderStroke(
            if (isPixelBtn || styleKey == "goth_fang" || styleKey == "bat_wing") 2.dp else 1.dp,
            borderColor
        ),
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = if (compact) 48.dp else 52.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .alpha(if (state == MelvoreWorldButtonState.Disabled) 0.48f else 1f)
            .clip(shape)
            .then(effectiveBackgroundModifier)
            .clickable(
                enabled = isEnabled,
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = borderColor),
                role = Role.Button,
                onClick = onClick
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (svgButtonPath != null) {
                RemoteSvgSkinImage(
                    relativeOrAbsoluteUrl = svgButtonPath,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize()
                )
            }
            if (sweepProgress > -0.25f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colorStops = arrayOf(
                                    (sweepProgress - 0.22f).coerceIn(0f, 1f) to Color.Transparent,
                                    sweepProgress.coerceIn(0f, 1f) to surfaces.onAccent.copy(alpha = 0.22f),
                                    (sweepProgress + 0.22f).coerceIn(0f, 1f) to Color.Transparent
                                )
                            )
                        )
                )
            }

            if (styleKey == "goth_fang" && state == MelvoreWorldButtonState.Default) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val toothW = 8.dp.toPx()
                    val toothH = 5.dp.toPx()
                    val path = Path().apply {
                        moveTo(w * 0.14f, 0f)
                        lineTo(w * 0.14f + toothW * 0.5f, toothH)
                        lineTo(w * 0.14f + toothW, 0f)
                        close()

                        moveTo(w * 0.86f - toothW, 0f)
                        lineTo(w * 0.86f - toothW * 0.5f, toothH)
                        lineTo(w * 0.86f, 0f)
                        close()
                    }
                    drawPath(path, borderColor)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(
                    PaddingValues(
                        horizontal = if (compact) 14.dp else 20.dp,
                        vertical = if (compact) 10.dp else 13.dp
                    )
                )
            ) {
                when (state) {
                    MelvoreWorldButtonState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = effectiveContentColor
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    MelvoreWorldButtonState.Success -> {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Success",
                            tint = effectiveContentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    MelvoreWorldButtonState.Error -> {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = "Error",
                            tint = effectiveContentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    else -> {
                        if (styleKey == "halloween_pumpkin") {
                            PumpkinCrestMini(
                                color = accentColor,
                                stemColor = surfaces.success,
                                cutoutColor = bgMid
                            )
                            Spacer(Modifier.width(7.dp))
                        } else if (styleKey == "coquette_ribbon") {
                            RibbonBowMini(color = accentColor, centerColor = surfaces.surface)
                            Spacer(Modifier.width(7.dp))
                        } else if (leadingIcon != null) {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = null,
                                tint = effectiveContentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                    }
                }

                val displayLabel = when {
                    isPixelBtn || styleKey == "streetwear_utility" || styleKey == "street_block" -> text.uppercase()
                    else -> text
                }

                Text(
                    text = displayLabel,
                    color = effectiveContentColor,
                    fontFamily = fontFamily,
                    fontWeight = if (isPixelBtn) FontWeight.Normal else FontWeight.Bold,
                    fontSize = if (isPixelBtn) 11.sp else if (compact) 12.sp else 13.sp,
                    letterSpacing = when {
                        isPixelBtn || styleKey == "goth_fang" || styleKey == "bat_wing" || styleKey == "streetwear_utility" || styleKey == "street_block" -> 1.1.sp
                        else -> 0.4.sp
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun PumpkinCrestMini(color: Color, stemColor: Color, cutoutColor: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        drawOval(
            color = color,
            topLeft = Offset(w * 0.08f, h * 0.25f),
            size = Size(w * 0.84f, h * 0.68f)
        )
        drawRect(
            color = stemColor,
            topLeft = Offset(w * 0.44f, h * 0.06f),
            size = Size(w * 0.14f, h * 0.22f)
        )
        drawCircle(color = cutoutColor, radius = w * 0.07f, center = Offset(w * 0.35f, h * 0.52f))
        drawCircle(color = cutoutColor, radius = w * 0.07f, center = Offset(w * 0.65f, h * 0.52f))
    }
}

@Composable
private fun RibbonBowMini(color: Color, centerColor: Color) {
    Canvas(modifier = Modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val leftWing = Path().apply {
            moveTo(w * 0.5f, h * 0.48f)
            lineTo(w * 0.08f, h * 0.2f)
            lineTo(w * 0.08f, h * 0.72f)
            close()
        }
        val rightWing = Path().apply {
            moveTo(w * 0.5f, h * 0.48f)
            lineTo(w * 0.92f, h * 0.2f)
            lineTo(w * 0.92f, h * 0.72f)
            close()
        }
        drawPath(leftWing, color)
        drawPath(rightWing, color)
        drawCircle(color = centerColor, radius = w * 0.12f, center = Offset(w * 0.5f, h * 0.48f))
    }
}
