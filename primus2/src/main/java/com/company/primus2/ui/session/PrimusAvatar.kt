package com.company.primus2.ui.session

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.company.primus2.ui.state.AiState

@Composable
fun PrimusAvatar(
    aiState: AiState?,
    modifier: Modifier = Modifier
) {
    val disposition = aiState?.disposition

    val targetColor = when {
        disposition == null -> Color.Gray
        disposition.warmth > 0.7f -> Color(0xFFFF8C00) // OrangeRed
        disposition.warmth < 0.3f -> Color(0xFF1E90FF) // DodgerBlue
        else -> Color(0xFF9370DB) // MediumPurple
    }

    val color by animateColorAsState(targetValue = targetColor, animationSpec = tween(1500))

    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500 + (2000 * (1 - (disposition?.energy ?: 0.5f))).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = modifier.size(100.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.5f), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 2 * pulse
                )
            )
            drawCircle(
                color = color,
                radius = size.minDimension / 4
            )
        }
    }
}