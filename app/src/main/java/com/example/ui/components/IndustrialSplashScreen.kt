package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Industrial-grade animated splash screen for 1 Q CNC.
 * Features:
 * - CNC Spindle / Optic reticle target animation with rotating cutter flutes.
 * - Subsystem hardware initialization progress bar (Kinematics, USB/BT, WCS registers, HAL).
 * - High-tech dark terminal theme with smooth dismiss transition.
 */
@Composable
fun IndustrialSplashScreen(
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  var currentStepIndex by remember { mutableIntStateOf(0) }
  var bootProgress by remember { mutableFloatStateOf(0.08f) }
  var isCompleted by remember { mutableStateOf(false) }

  val bootSteps = remember {
    listOf(
      "1 Q Kernel Initializing...",
      "Configuring QRB2210 + STM32 Motion Bridge...",
      "Scanning USB Serial OTG & Bluetooth SPP Buses...",
      "Calibrating Sub-Micron Kinematics Engine...",
      "Loading Tool Geometries & WCS Offsets (G54-G59)...",
      "HAL Pin Diagnostics Ready — 1 Q CNC Standby"
    )
  }

  // Continuous rotation for CNC spindle cutter
  val infiniteTransition = rememberInfiniteTransition(label = "spindle_rotation")
  val cutterRotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "cutter_angle"
  )

  // Pulsing laser reticle
  val laserPulse by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "laser_pulse"
  )

  // Progress animation
  val animatedProgress by animateFloatAsState(
    targetValue = bootProgress,
    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
    label = "progress"
  )

  // Initialization sequence runner
  LaunchedEffect(Unit) {
    for (i in bootSteps.indices) {
      currentStepIndex = i
      bootProgress = ((i + 1).toFloat() / bootSteps.size)
      delay(420)
    }
    isCompleted = true
    delay(400)
    onDismiss()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF090D16),
            Color(0xFF030712)
          )
        )
      )
      .testTag("industrial_splash_screen"),
    contentAlignment = Alignment.Center
  ) {
    // Background Subtle Coordinate Grid
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.07f)) {
      val step = 40.dp.toPx()
      var x = 0f
      while (x < size.width) {
        drawLine(
          color = Color(0xFF00E5FF),
          start = Offset(x, 0f),
          end = Offset(x, size.height),
          strokeWidth = 1f
        )
        x += step
      }
      var y = 0f
      while (y < size.height) {
        drawLine(
          color = Color(0xFF00E5FF),
          start = Offset(0f, y),
          end = Offset(size.width, y),
          strokeWidth = 1f
        )
        y += step
      }
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 32.dp)
    ) {
      // Reticle & CNC Tool Head
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(140.dp)
      ) {
        // Outer Radar Reticle Ring
        Canvas(modifier = Modifier.size(130.dp).rotate(cutterRotation)) {
          drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.35f),
            style = Stroke(
              width = 2.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
            )
          )
        }

        // Inner Laser Pulse Reticle
        Canvas(
          modifier = Modifier
            .size(90.dp)
            .scale(laserPulse)
        ) {
          drawCircle(
            color = Color(0xFF00E5FF).copy(alpha = 0.5f),
            style = Stroke(width = 1.5.dp.toPx())
          )
          // Crosshairs
          val center = Offset(size.width / 2, size.height / 2)
          val len = 14.dp.toPx()
          drawLine(Color(0xFF00E5FF), Offset(center.x - len, center.y), Offset(center.x + len, center.y), 2f)
          drawLine(Color(0xFF00E5FF), Offset(center.x, center.y - len), Offset(center.x, center.y + len), 2f)
        }

        // Central CNC Cutter / Emblem Icon
        Surface(
          shape = CircleShape,
          color = Color(0xFF1E293B),
          border = BorderStroke(2.dp, Color(0xFF00E5FF)),
          shadowElevation = 8.dp,
          modifier = Modifier.size(62.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.PrecisionManufacturing,
              contentDescription = "CNC Spindle Icon",
              tint = Color(0xFF00E5FF),
              modifier = Modifier.size(36.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Main Brand Name
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "1 Q",
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.Black,
          color = Color(0xFF00E5FF),
          letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "CNC",
          style = MaterialTheme.typography.headlineLarge,
          fontWeight = FontWeight.ExtraBold,
          color = Color.White,
          letterSpacing = 3.sp
        )
      }

      Text(
        text = "INDUSTRIAL HMI CONTROLLER",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF94A3B8),
        letterSpacing = 2.5.sp,
        modifier = Modifier.padding(top = 4.dp)
      )

      Spacer(modifier = Modifier.height(36.dp))

      // Progress Bar
      LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier
          .fillMaxWidth(0.82f)
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = Color(0xFF00E5FF),
        trackColor = Color(0xFF1E293B)
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Subsystem Initialization Status Log
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(0.9f)
      ) {
        if (isCompleted) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "System Ready",
            tint = Color(0xFF10B981),
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
          text = if (isCompleted) "SYSTEM READY" else bootSteps.getOrElse(currentStepIndex) { "Booting..." },
          style = MaterialTheme.typography.bodySmall,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          color = if (isCompleted) Color(0xFF10B981) else Color(0xFF38BDF8),
          maxLines = 1
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Architecture Subtitle
      Text(
        text = "Dual-Core QRB2210 Linux + STM32 Motion Coprocessor",
        style = MaterialTheme.typography.labelSmall,
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        color = Color(0xFF64748B)
      )
    }

    // Skip Button in bottom right corner
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(20.dp),
      contentAlignment = Alignment.BottomEnd
    ) {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("skip_splash_button")
      ) {
        Text(
          text = "SKIP >>",
          style = MaterialTheme.typography.labelMedium,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF64748B)
        )
      }
    }
  }
}
