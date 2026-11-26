package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun MelvoreSkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(2.dp)
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    val brush = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
        val offset by transition.animateFloat(
            initialValue = -300f,
            targetValue = 900f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "skeleton_offset"
        )
        Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(offset, 0f),
            end = Offset(offset + 350f, 200f)
        )
    } else {
        Brush.linearGradient(listOf(base, base))
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

@Composable
fun HomeFeedSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Search bar skeleton
        MelvoreSkeletonBox(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(50.dp)
        )
        // Edge-to-edge hero skeleton
        MelvoreSkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 4.4f),
            shape = RoundedCornerShape(0.dp)
        )
        // Horizontal category pills skeleton
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(4) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MelvoreSkeletonBox(modifier = Modifier.size(72.dp))
                    MelvoreSkeletonBox(modifier = Modifier.width(64.dp).height(12.dp))
                }
            }
        }
        // 2-col product grid skeleton
        ProductGridSkeleton(rows = 2)
    }
}

@Composable
fun ProductGridSkeleton(
    rows: Int = 2,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(2) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MelvoreSkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 3.6f)
                        )
                        MelvoreSkeletonBox(modifier = Modifier.width(70.dp).height(10.dp))
                        MelvoreSkeletonBox(modifier = Modifier.fillMaxWidth(0.85f).height(14.dp))
                        Spacer(Modifier.height(2.dp))
                        MelvoreSkeletonBox(modifier = Modifier.width(90.dp).height(16.dp))
                    }
                }
            }
        }
    }
}
