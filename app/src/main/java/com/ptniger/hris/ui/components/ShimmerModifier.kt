package com.ptniger.hris.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Modifier untuk efek animasi Shimmer (Skeleton Loading).
 * Sangat ringan, 60fps tanpa memory overhead atau dependensi eksternal.
 */
fun Modifier.shimmerLoading(
    isLoading: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp),
    baseColor: Color = Color(0xFFE2E8F0),
    highlightColor: Color = Color(0xFFF8FAFC)
): Modifier = composed {
    if (!isLoading) return@composed this

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    val shimmerColors = remember(baseColor, highlightColor) {
        listOf(
            baseColor.copy(alpha = 0.85f),
            highlightColor.copy(alpha = 0.95f),
            baseColor.copy(alpha = 0.85f)
        )
    }

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim - 600f, y = translateAnim - 600f),
        end = Offset(x = translateAnim, y = translateAnim)
    )

    this.background(brush = brush, shape = shape)
}
