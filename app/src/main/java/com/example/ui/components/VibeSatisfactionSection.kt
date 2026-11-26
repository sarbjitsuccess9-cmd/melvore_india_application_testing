package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.StyleWorldFeedbackResult
import com.example.model.UnlockedCoupon
import com.example.ui.theme.LocalFashionWorldTheme
import com.example.ui.theme.LocalMelvoreDesignManifest
import com.example.ui.theme.fontFamilyForStyleKey
import kotlin.math.roundToInt

/**
 * Pure helper controlling whether the Vibe Satisfaction Questionnaire may appear for this world.
 * Only hides after confirmed submission for this world + actor.
 */
fun shouldShowVibeSatisfactionSection(
    hasSubmittedBackendOrLocal: Boolean,
    justUnlockedResult: StyleWorldFeedbackResult?
): Boolean {
    if (justUnlockedResult != null && justUnlockedResult.hasSubmitted) {
        return true // Keep showing the unlocked coupon popup until user dismisses or leaves
    }
    return !hasSubmittedBackendOrLocal
}

private data class VibeStage(
    val score: Int,
    val emoji: String,
    val label: String
)

private val VIBE_STAGES = listOf(
    VibeStage(1, "😕", "Not really"),
    VibeStage(2, "🙂", "It's okay"),
    VibeStage(3, "😍", "Love it"),
    VibeStage(4, "🤩", "Obsessed!")
)

/**
 * Minimal, slim, borderless editorial horizontal bar for one-time Vibe satisfaction.
 * Auto-submits upon drag release or tapping any emoji stage—no bulky card box or big submit button.
 */
@Composable
fun VibeSatisfactionSection(
    worldName: String,
    fontStyle: String,
    buttonStyle: String,
    surfaceColor: Color,
    accentColor: Color,
    secondaryAccentColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    isSubmitting: Boolean,
    unlockedResult: StyleWorldFeedbackResult?,
    onSubmitScore: (Int) -> Unit,
    onCopyCoupon: (String) -> Unit,
    onApplyCouponToBag: (String) -> Unit,
    onDismissReward: () -> Unit,
    modifier: Modifier = Modifier
) {
    // When unlockedResult arrives, popup dialog handles coupon presentation; hide inline slider bar cleanly
    if (unlockedResult != null && unlockedResult.hasSubmitted) {
        VibeRewardCouponDialog(
            worldName = worldName,
            coupon = unlockedResult.coupon,
            feedbackLabel = unlockedResult.feedback?.feedbackLabel ?: "Saved",
            fontStyle = fontStyle,
            buttonStyle = buttonStyle,
            surfaceColor = surfaceColor,
            accentColor = accentColor,
            secondaryAccentColor = secondaryAccentColor,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onCopyCoupon = onCopyCoupon,
            onApplyCouponToBag = onApplyCouponToBag,
            onDismissReward = onDismissReward
        )
        return
    }

    var selectedScore by remember { mutableIntStateOf(3) }
    val selectedStage = VIBE_STAGES.firstOrNull { it.score == selectedScore } ?: VIBE_STAGES[2]
    val targetFraction = (selectedScore - 1) / 3f

    val animatedFill by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "vibe_slider_fill"
    )

    val displayFont = fontFamilyForStyleKey(fontStyle)
    val manifest = LocalMelvoreDesignManifest.current
    val activeWorldTheme = LocalFashionWorldTheme.current
    val sliderSvgPath = remember(manifest, activeWorldTheme) {
        RemoteDesignRegistry.activeWorldKit(manifest, activeWorldTheme)?.svg?.slider?.takeIf { it.isNotBlank() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vibe_satisfaction_section")
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = textSecondary.copy(alpha = 0.18f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.vibe_prompt_title),
                    fontFamily = displayFont,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                    color = textPrimary
                )
                Text(
                    text = if (isSubmitting) "Unlocking reward…" else "${selectedStage.emoji} ${selectedStage.label}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("vibe_selected_label")
                )
            }

            // Slim interactive track + thumb + inline emoji stage taps
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("vibe_satisfaction_slider")
                ) {
                    val density = LocalDensity.current
                    val trackWidthPx = with(density) { maxWidth.toPx() }
                    val thumbDiameter = 24.dp
                    val thumbPx = with(density) { thumbDiameter.toPx() }
                    val usablePx = (trackWidthPx - thumbPx).coerceAtLeast(1f)

                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .pointerInput(usablePx, isSubmitting) {
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { change, _ ->
                                        if (isSubmitting) return@detectHorizontalDragGestures
                                        change.consume()
                                        val frac = ((change.position.x - thumbPx * 0.5f) / usablePx).coerceIn(0f, 1f)
                                        selectedScore = ((frac * 3f).roundToInt() + 1).coerceIn(1, 4)
                                    },
                                    onDragEnd = {
                                        if (!isSubmitting) onSubmitScore(selectedScore)
                                    }
                                )
                            }
                    ) {
                        RemoteSvgSliderTrack(
                            sliderSvgPath = sliderSvgPath,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(textSecondary.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((0.12f + animatedFill * 0.88f).coerceIn(0.12f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Brush.horizontalGradient(listOf(secondaryAccentColor, accentColor)))
                                )
                            }
                        }

                        val thumbOffsetPx = (animatedFill * usablePx).roundToInt()
                        Surface(
                            shape = CircleShape,
                            color = accentColor,
                            shadowElevation = 3.dp,
                            border = BorderStroke(1.5.dp, com.example.ui.theme.LocalMelvoreSurfaces.current.surface),
                            modifier = Modifier
                                .offset { IntOffset(thumbOffsetPx, 0) }
                                .size(thumbDiameter)
                        ) {}
                    }
                }

                // Inline compact stage buttons (>=48.dp touch height)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VIBE_STAGES.forEach { st ->
                        val active = st.score == selectedScore
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(width = 40.dp, height = 48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) accentColor.copy(alpha = 0.16f) else Color.Transparent
                                )
                                .clickable(enabled = !isSubmitting) {
                                    selectedScore = st.score
                                    onSubmitScore(st.score)
                                }
                                .testTag("vibe_stage_${st.score}")
                        ) {
                            Text(
                                text = st.emoji,
                                fontSize = if (active) 20.sp else 16.sp
                            )
                        }
                    }
                }
            }

            Text(
                text = "Slide or tap an emoji to unlock your exclusive Melvore reward.",
                style = MaterialTheme.typography.labelSmall,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        HorizontalDivider(thickness = 0.5.dp, color = textSecondary.copy(alpha = 0.18f))
    }
}

@Composable
fun VibeRewardCouponDialog(
    worldName: String,
    coupon: UnlockedCoupon?,
    feedbackLabel: String,
    fontStyle: String,
    buttonStyle: String,
    surfaceColor: Color,
    accentColor: Color,
    secondaryAccentColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onCopyCoupon: (String) -> Unit,
    onApplyCouponToBag: (String) -> Unit,
    onDismissReward: () -> Unit
) {
    val displayFont = fontFamilyForStyleKey(fontStyle)
    val code = coupon?.code.orEmpty()

    AlertDialog(
        onDismissRequest = onDismissReward,
        containerColor = surfaceColor,
        titleContentColor = textPrimary,
        textContentColor = textSecondary,
        modifier = Modifier.testTag("vibe_unlocked_coupon_card"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VIBE SAVED · ${feedbackLabel.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "$worldName Reward Unlocked",
                        fontFamily = displayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textPrimary
                    )
                }
                IconButton(
                    onClick = onDismissReward,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = textSecondary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (code.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, secondaryAccentColor.copy(alpha = 0.65f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = code,
                                    fontFamily = displayFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = textPrimary,
                                    letterSpacing = 1.4.sp
                                )
                                if (coupon?.discountSummary?.isNotBlank() == true) {
                                    Text(
                                        text = coupon.discountSummary,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = accentColor
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = accentColor.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onCopyCoupon(code) }
                                    .testTag("vibe_copy_coupon_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = "Copy coupon $code",
                                        tint = accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = "Copy",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                            }
                        }
                    }
                    if (coupon?.conditionsText?.isNotBlank() == true) {
                        Text(
                            text = coupon.conditionsText,
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                } else {
                    Text(
                        text = "Thank you for shaping your personalized Melvore feed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }
            }
        },
        confirmButton = {
            if (code.isNotBlank()) {
                MelvoreWorldActionButton(
                    text = "Apply to Bag",
                    buttonStyleOverride = buttonStyle,
                    compact = true,
                    onClick = { onApplyCouponToBag(code) },
                    modifier = Modifier.testTag("vibe_apply_coupon_button")
                )
            } else {
                TextButton(
                    onClick = onDismissReward,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Done", color = accentColor)
                }
            }
        },
        dismissButton = {
            if (code.isNotBlank()) {
                MelvoreWorldActionButton(
                    text = "Done",
                    buttonStyleOverride = buttonStyle,
                    compact = true,
                    ghost = true,
                    onClick = onDismissReward
                )
            }
        }
    )
}
