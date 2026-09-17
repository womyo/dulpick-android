package com.dulpick.app.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.dulpick.app.ui.theme.Colors

private const val SHIMMER_DURATION_MS = 1300
private const val BAND_WIDTH_RATIO = 0.6f
private const val TRAVEL_RATIO = 1.3f
private const val HIGHLIGHT_ALPHA = 0.9f

// 로딩 자리를 채우는 쉬머 (iOS ShimmerBlock 대응).
// 단색 바탕 위에 흰 반투명 띠를 좌→우로 평행하게 흘린다. 띠 폭은 박스 폭의 60%
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, baseColor: Color = Colors.gray300) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val phase by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerPhase",
    )
    Box(
        modifier = modifier
            .background(baseColor)
            .drawWithCache {
                val bandWidth = size.width * BAND_WIDTH_RATIO
                val startX = phase * size.width * TRAVEL_RATIO
                val brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0f),
                        Color.White.copy(alpha = HIGHLIGHT_ALPHA),
                        Color.White.copy(alpha = 0f),
                    ),
                    startX = startX,
                    endX = startX + bandWidth,
                )
                onDrawBehind { drawRect(brush) }
            },
    )
}
