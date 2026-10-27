package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val scaleAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.8f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2200)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFE2E8F0),
                        Color(0xFFFFFBEB)
                    )
                )
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .scale(scaleAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Top: Logo & App Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.moneymap_logo),
                        contentDescription = "MoneyMap Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "MoneyMap",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MoneyMapNavy,
                    fontSize = 32.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Track • Plan • Grow",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp
                )
            }

            // Middle: Financial Growth Illustration (Compose Canvas)
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0x66FFFFFF))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Background growth chart bars
                    val barWidth = 24f
                    val barSpacing = 16f
                    val startX = w * 0.2f
                    
                    // Bar 1
                    drawRect(
                        color = Color(0xFF93C5FD).copy(alpha = 0.5f),
                        topLeft = Offset(startX, h * 0.6f),
                        size = Size(barWidth, h * 0.3f)
                    )
                    // Bar 2
                    drawRect(
                        color = Color(0xFF60A5FA).copy(alpha = 0.7f),
                        topLeft = Offset(startX + barWidth + barSpacing, h * 0.45f),
                        size = Size(barWidth, h * 0.45f)
                    )
                    // Bar 3
                    drawRect(
                        color = Color(0xFF3B82F6),
                        topLeft = Offset(startX + (barWidth + barSpacing) * 2, h * 0.25f),
                        size = Size(barWidth, h * 0.65f)
                    )

                    // Stacked Gold Coins at bottom center
                    val coinCenterX = w * 0.5f
                    val coinCenterY = h * 0.75f
                    val coinRadiusX = 50f
                    val coinRadiusY = 18f

                    // Coin 3 (Bottom)
                    drawOval(
                        color = Color(0xFFD97706),
                        topLeft = Offset(coinCenterX - coinRadiusX - 10f, coinCenterY + 10f),
                        size = Size((coinRadiusX + 10f) * 2, coinRadiusY * 2)
                    )
                    drawOval(
                        color = Color(0xFFF59E0B),
                        topLeft = Offset(coinCenterX - coinRadiusX - 10f, coinCenterY + 6f),
                        size = Size((coinRadiusX + 10f) * 2, coinRadiusY * 2)
                    )

                    // Coin 2 (Middle)
                    drawOval(
                        color = Color(0xFFB45309),
                        topLeft = Offset(coinCenterX - coinRadiusX, coinCenterY - 2f),
                        size = Size(coinRadiusX * 2, coinRadiusY * 2)
                    )
                    drawOval(
                        color = Color(0xFFFBBF24),
                        topLeft = Offset(coinCenterX - coinRadiusX, coinCenterY - 6f),
                        size = Size(coinRadiusX * 2, coinRadiusY * 2)
                    )

                    // Coin 1 (Top)
                    drawOval(
                        color = Color(0xFFD97706),
                        topLeft = Offset(coinCenterX - coinRadiusX + 10f, coinCenterY - 18f),
                        size = Size((coinRadiusX - 10f) * 2, coinRadiusY * 2)
                    )
                    drawOval(
                        color = Color(0xFFFDE68A),
                        topLeft = Offset(coinCenterX - coinRadiusX + 10f, coinCenterY - 22f),
                        size = Size((coinRadiusX - 10f) * 2, coinRadiusY * 2)
                    )

                    // Growing Green Plant Sprouting from center
                    val stemStartX = coinCenterX
                    val stemStartY = coinCenterY - 22f
                    val stemTopX = coinCenterX
                    val stemTopY = coinCenterY - 65f

                    // Draw stem line
                    drawLine(
                        color = Color(0xFF059669),
                        start = Offset(stemStartX, stemStartY),
                        end = Offset(stemTopX, stemTopY),
                        strokeWidth = 6f
                    )

                    // Left leaf
                    drawArc(
                        color = Color(0xFF10B981),
                        startAngle = 180f,
                        sweepAngle = 130f,
                        useCenter = true,
                        topLeft = Offset(stemTopX - 45f, stemTopY - 25f),
                        size = Size(45f, 30f)
                    )

                    // Right leaf
                    drawArc(
                        color = Color(0xFF34D399),
                        startAngle = 270f,
                        sweepAngle = 130f,
                        useCenter = true,
                        topLeft = Offset(stemTopX, stemTopY - 35f),
                        size = Size(45f, 30f)
                    )
                }
            }

            // Bottom: Loading indicator, Subtext, & Made in India
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(180.dp)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = MoneyMapBlue,
                    trackColor = Color(0xFFCBD5E1)
                )

                Text(
                    text = "Building a better money tomorrow...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MoneyMapNavy,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(text = "🇮🇳 ", fontSize = 16.sp)
                    Text(
                        text = "Made in India",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "❤️", fontSize = 14.sp)
                }
            }
        }
    }
}
