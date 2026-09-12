package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ToolpathPoint
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun ToolpathVisualizer(
  toolpathPoints: List<ToolpathPoint>,
  currentToolX: Double,
  currentToolY: Double,
  currentToolZ: Double,
  activeLineIndex: Int,
  modifier: Modifier = Modifier
) {
  var scale by remember { mutableFloatStateOf(2.2f) }
  var offsetX by remember { mutableFloatStateOf(60f) }
  var offsetY by remember { mutableFloatStateOf(240f) }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(260.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(DroBackground)
      .pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
          scale = (scale * zoom).coerceIn(0.5f, 6.0f)
          offsetX += pan.x
          offsetY += pan.y
        }
      }
      .testTag("toolpath_visualizer_canvas")
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val canvasWidth = size.width
      val canvasHeight = size.height

      // Draw CNC Machine Grid
      drawCncGrid(scale, offsetX, offsetY, canvasWidth, canvasHeight)

      // Draw Toolpath Lines
      if (toolpathPoints.isNotEmpty()) {
        for (i in 0 until toolpathPoints.size - 1) {
          val p1 = toolpathPoints[i]
          val p2 = toolpathPoints[i + 1]

          val x1 = offsetX + p1.x * scale
          val y1 = offsetY - p1.y * scale // Invert Y for CNC coordinates
          val x2 = offsetX + p2.x * scale
          val y2 = offsetY - p2.y * scale

          val isExecuted = i < activeLineIndex
          val lineColor = when {
            p2.isRapid -> DroCyan.copy(alpha = if (isExecuted) 0.8f else 0.4f)
            p2.z < 0 -> if (isExecuted) DroAmber else DroGreen
            else -> Color.White.copy(alpha = 0.5f)
          }

          val strokeWidth = if (p2.isRapid) 1.5f else (if (isExecuted) 3.5f else 2.5f)
          val pathEffect = if (p2.isRapid) PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f) else null

          drawLine(
            color = lineColor,
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = strokeWidth,
            pathEffect = pathEffect
          )
        }
      }

      // Draw WCS Origin (0,0) Marker
      val originScreenX = offsetX
      val originScreenY = offsetY
      drawLine(
        color = IndDarkEmergency,
        start = Offset(originScreenX - 15f, originScreenY),
        end = Offset(originScreenX + 15f, originScreenY),
        strokeWidth = 2f
      )
      drawLine(
        color = IndDarkEmergency,
        start = Offset(originScreenX, originScreenY - 15f),
        end = Offset(originScreenX, originScreenY + 15f),
        strokeWidth = 2f
      )
      drawCircle(
        color = IndDarkEmergency,
        radius = 4f,
        center = Offset(originScreenX, originScreenY)
      )

      // Draw Active Live Spindle/Tool Position Indicator
      val toolScreenX = offsetX + currentToolX.toFloat() * scale
      val toolScreenY = offsetY - currentToolY.toFloat() * scale

      // Outer targeting ring
      drawCircle(
        color = IndDarkPrimary,
        radius = 12f,
        center = Offset(toolScreenX, toolScreenY),
        style = Stroke(width = 2f)
      )
      // Inner glowing core
      drawCircle(
        color = IndDarkTertiary,
        radius = 5f,
        center = Offset(toolScreenX, toolScreenY)
      )
      // Crosshairs
      drawLine(
        color = IndDarkPrimary,
        start = Offset(toolScreenX - 18f, toolScreenY),
        end = Offset(toolScreenX + 18f, toolScreenY),
        strokeWidth = 1.5f
      )
      drawLine(
        color = IndDarkPrimary,
        start = Offset(toolScreenX, toolScreenY - 18f),
        end = Offset(toolScreenX, toolScreenY + 18f),
        strokeWidth = 1.5f
      )
    }

    // Overlay controls & Legend
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .align(Alignment.TopStart),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Coordinate overlay badge
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(Color.Black.copy(alpha = 0.7f))
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = "TOOL: X ${String.format(Locale.US, "%.2f", currentToolX)}  Y ${String.format(Locale.US, "%.2f", currentToolY)}  Z ${String.format(Locale.US, "%.2f", currentToolZ)}",
          style = MaterialTheme.typography.labelSmall,
          color = DroGreen,
          fontFamily = FontFamily.Monospace
        )
      }

      // Zoom reset & pan buttons
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(Color.Black.copy(alpha = 0.7f)),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        IconButton(
          onClick = { scale = (scale * 1.25f).coerceAtMost(6.0f) },
          modifier = Modifier.size(28.dp)
        ) {
          Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(16.dp))
        }
        IconButton(
          onClick = { scale = (scale / 1.25f).coerceAtLeast(0.5f) },
          modifier = Modifier.size(28.dp)
        ) {
          Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(16.dp))
        }
        IconButton(
          onClick = {
            scale = 2.2f
            offsetX = 60f
            offsetY = 240f
          },
          modifier = Modifier.size(28.dp)
        ) {
          Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset View", tint = Color.White, modifier = Modifier.size(16.dp))
        }
      }
    }

    // Legend on bottom right
    Row(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(8.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(Color.Black.copy(alpha = 0.7f))
        .padding(horizontal = 6.dp, vertical = 3.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(DroCyan))
        Spacer(modifier = Modifier.width(3.dp))
        Text("G0 Rapid", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.LightGray)
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(DroGreen))
        Spacer(modifier = Modifier.width(3.dp))
        Text("G1 Feed", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.LightGray)
      }
    }
  }
}

private fun DrawScope.drawCncGrid(
  scale: Float,
  offsetX: Float,
  offsetY: Float,
  width: Float,
  height: Float
) {
  val gridStepMm = 10f
  val gridStepPx = gridStepMm * scale
  val gridColor = Color(0xFF1E293B).copy(alpha = 0.4f)
  val majorGridColor = Color(0xFF334155).copy(alpha = 0.7f)

  // Vertical grid lines
  var x = offsetX % gridStepPx
  var mmX = ((-offsetX) / scale)
  while (x < width) {
    val isMajor = ((x - offsetX) / gridStepPx).toInt() % 5 == 0
    drawLine(
      color = if (isMajor) majorGridColor else gridColor,
      start = Offset(x, 0f),
      end = Offset(x, height),
      strokeWidth = if (isMajor) 1f else 0.5f
    )
    x += gridStepPx
  }

  // Horizontal grid lines
  var y = offsetY % gridStepPx
  while (y < height) {
    val isMajor = ((y - offsetY) / gridStepPx).toInt() % 5 == 0
    drawLine(
      color = if (isMajor) majorGridColor else gridColor,
      start = Offset(0f, y),
      end = Offset(width, y),
      strokeWidth = if (isMajor) 1f else 0.5f
    )
    y += gridStepPx
  }
}
