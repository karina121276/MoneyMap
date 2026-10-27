package com.thecorporatedeveloper.moneymap.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thecorporatedeveloper.moneymap.ui.theme.*

@Composable
fun MoneySproutIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "leaf_sway")
    val swayAngle by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.toPx()
            val h = size.toPx()

            // Golden coin pot
            val potWidth = w * 0.65f
            val potHeight = h * 0.45f
            val potLeft = (w - potWidth) / 2f
            val potTop = h * 0.55f

            // Pot rim
            drawRoundRect(
                color = Color(0xFFF59E0B),
                topLeft = Offset(potLeft - 4f, potTop - 6f),
                size = Size(potWidth + 8f, 12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )

            // Pot body
            drawRect(
                color = Color(0xFFD97706),
                topLeft = Offset(potLeft, potTop + 6f),
                size = Size(potWidth, potHeight - 6f)
            )

            // Plant stem
            drawLine(
                color = Color(0xFF059669),
                start = Offset(w / 2f, potTop),
                end = Offset(w / 2f, h * 0.25f),
                strokeWidth = 6f
            )
        }

        // Swaying leaves
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(swayAngle)
        ) {
            val w = size.toPx()
            val h = size.toPx()
            val stemTopX = w / 2f
            val stemTopY = h * 0.25f

            // Left leaf
            drawArc(
                color = Color(0xFF10B981),
                startAngle = 180f,
                sweepAngle = 130f,
                useCenter = true,
                topLeft = Offset(stemTopX - 32f, stemTopY - 18f),
                size = Size(32f, 22f)
            )

            // Right leaf
            drawArc(
                color = Color(0xFF34D399),
                startAngle = 270f,
                sweepAngle = 130f,
                useCenter = true,
                topLeft = Offset(stemTopX, stemTopY - 24f),
                size = Size(32f, 22f)
            )
        }
    }
}

@Composable
fun PennyIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val bounceAnim = rememberInfiniteTransition(label = "penny_bounce")
    val offsetY by bounceAnim.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    Box(
        modifier = modifier
            .size(size)
            .offset(y = offsetY.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.toPx()
            val h = size.toPx()
            val radius = w / 2f - 4f

            // Outer coin ring
            drawCircle(
                color = Color(0xFFD97706),
                radius = radius,
                center = Offset(w / 2f, h / 2f)
            )

            // Inner coin face
            drawCircle(
                color = Color(0xFFFBBF24),
                radius = radius - 4f,
                center = Offset(w / 2f, h / 2f)
            )

            // Tiny green leaf on top of coin
            drawArc(
                color = Color(0xFF10B981),
                startAngle = 220f,
                sweepAngle = 100f,
                useCenter = true,
                topLeft = Offset(w / 2f - 8f, 2f),
                size = Size(16f, 12f)
            )
        }
    }
}
