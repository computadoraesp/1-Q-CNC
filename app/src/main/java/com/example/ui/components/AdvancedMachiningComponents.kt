package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.abs

/**
 * 1. JOB TIME ESTIMATOR & KINEMATIC STATS CARD
 */
@Composable
fun JobTimeEstimatorCard(
  estimate: JobTimeEstimate,
  onRecalculate: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }

  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(12.dp),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.primaryContainer
          ) {
            Icon(
              imageVector = Icons.Default.Timer,
              contentDescription = "Job Time Estimator",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(6.dp).size(20.dp)
            )
          }
          Column {
            Text(
              text = "Estimador Cinemático de Tiempo",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Cálculo de trayectorias G0/G1/G2/G3 y avances",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.sp
            )
          }
        }

        IconButton(
          onClick = { isExpanded = !isExpanded },
          modifier = Modifier.testTag("expand_job_estimator_btn")
        ) {
          Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = "Expand Estimator"
          )
        }
      }

      // Summary Highlight Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "TIEMPO ESTIMADO TOTAL",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = estimate.formattedTotalTime,
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "Corte Activo (Feed)",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 9.sp,
              color = DroGreen
            )
            Text(
              text = estimate.formattedCutTime,
              style = MaterialTheme.typography.bodyMedium,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = "Rápido (G0)",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 9.sp,
              color = DroAmber
            )
            Text(
              text = estimate.formattedRapidTime,
              style = MaterialTheme.typography.bodyMedium,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }

      // Expanded Details
      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

          // Distance and Feed Breakdown Grid
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            KinematicMetricItem(
              label = "Distancia Total",
              value = String.format(Locale.US, "%.1f mm", estimate.totalDistanceMm),
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.weight(1f)
            )
            KinematicMetricItem(
              label = "Dist. Corte (G1/G2)",
              value = String.format(Locale.US, "%.1f mm", estimate.cutDistanceMm),
              color = DroGreen,
              modifier = Modifier.weight(1f)
            )
            KinematicMetricItem(
              label = "Dist. Rápida (G0)",
              value = String.format(Locale.US, "%.1f mm", estimate.rapidDistanceMm),
              color = DroAmber,
              modifier = Modifier.weight(1f)
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            KinematicMetricItem(
              label = "Recorrido Eje X",
              value = String.format(Locale.US, "%.1f mm", estimate.axisDistanceX),
              color = DroRed,
              modifier = Modifier.weight(1f)
            )
            KinematicMetricItem(
              label = "Recorrido Eje Y",
              value = String.format(Locale.US, "%.1f mm", estimate.axisDistanceY),
              color = DroGreen,
              modifier = Modifier.weight(1f)
            )
            KinematicMetricItem(
              label = "Recorrido Eje Z",
              value = String.format(Locale.US, "%.1f mm", estimate.axisDistanceZ),
              color = DroCyan,
              modifier = Modifier.weight(1f)
            )
          }

          // Bounds Envelope
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = CardDefaults.outlinedCardBorder()
          ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "VOLUMEN DE TRABAJO (ENVELOPE BBOX)",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "X: [${String.format(Locale.US, "%.1f", estimate.boundsMinX)} .. ${String.format(Locale.US, "%.1f", estimate.boundsMaxX)}]",
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp,
                  color = DroRed
                )
                Text(
                  text = "Y: [${String.format(Locale.US, "%.1f", estimate.boundsMinY)} .. ${String.format(Locale.US, "%.1f", estimate.boundsMaxY)}]",
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp,
                  color = DroGreen
                )
                Text(
                  text = "Z: [${String.format(Locale.US, "%.1f", estimate.boundsMinZ)} .. ${String.format(Locale.US, "%.1f", estimate.boundsMaxZ)}]",
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp,
                  color = DroCyan
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            OutlinedButton(
              onClick = onRecalculate,
              modifier = Modifier.height(34.dp).testTag("recalculate_estimate_btn")
            ) {
              Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Recalcular Cinemática", fontSize = 11.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun KinematicMetricItem(
  label: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
  ) {
    Column(
      modifier = Modifier.padding(6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 8.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
      )
      Text(
        text = value,
        style = MaterialTheme.typography.labelMedium,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        color = color
      )
    }
  }
}

/**
 * 2. PCB AUTO-LEVELING & SURFACE MESH PROBE CARD
 */
@Composable
fun AutoLevelMeshCard(
  meshState: AutoLevelMeshState,
  onStartProbing: () -> Unit,
  onStopProbing: () -> Unit,
  onApplyMeshToGCode: () -> Unit,
  onClearMesh: () -> Unit,
  onUpdateConfig: (MeshGridConfig) -> Unit,
  modifier: Modifier = Modifier
) {
  var showConfigDialog by remember { mutableStateOf(false) }

  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(12.dp),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = DroCyan.copy(alpha = 0.2f)
          ) {
            Icon(
              imageVector = Icons.Default.Grid4x4,
              contentDescription = "Auto Leveling Mesh",
              tint = DroCyan,
              modifier = Modifier.padding(6.dp).size(20.dp)
            )
          }
          Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = "Auto-Leveling & Malla PCB",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
              )
              if (meshState.isMeshAppliedToGCode) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = DroGreen.copy(alpha = 0.2f)
                ) {
                  Text(
                    text = "ACTIVO EN G-CODE",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = DroGreen,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                  )
                }
              }
            }
            Text(
              text = "Compensación de relieve Z para fresado de placas y superficies",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.sp
            )
          }
        }

        IconButton(
          onClick = { showConfigDialog = true },
          modifier = Modifier.testTag("mesh_config_settings_btn")
        ) {
          Icon(imageVector = Icons.Default.Settings, contentDescription = "Config Malla")
        }
      }

      // Topography Heatmap Canvas Visualizer
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(140.dp)
          .clip(RoundedCornerShape(8.dp)),
        color = Color(0xFF10151C),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          MeshHeatMapCanvas(
            meshState = meshState,
            modifier = Modifier.fillMaxSize()
          )

          // Top Overlay stats
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color.Black.copy(alpha = 0.6f)
            ) {
              Text(
                text = "Grid: ${meshState.config.gridCols}x${meshState.config.gridRows} (${meshState.config.totalPoints} pts)",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = DroCyan,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }

            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color.Black.copy(alpha = 0.6f)
            ) {
              Text(
                text = "ΔZ P2P: ${String.format(Locale.US, "%+.3f mm", meshState.peakToPeakZ)}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (meshState.peakToPeakZ > 0.3) DroAmber else DroGreen,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }
          }
        }
      }

      // Stats Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        KinematicMetricItem(
          label = "Z Mínimo",
          value = String.format(Locale.US, "%+.3f mm", meshState.minZOffset),
          color = DroCyan,
          modifier = Modifier.weight(1f)
        )
        KinematicMetricItem(
          label = "Z Promedio",
          value = String.format(Locale.US, "%+.3f mm", meshState.avgZOffset),
          color = DroCyan,
          modifier = Modifier.weight(1f)
        )
        KinematicMetricItem(
          label = "Z Máximo",
          value = String.format(Locale.US, "%+.3f mm", meshState.maxZOffset),
          color = DroAmber,
          modifier = Modifier.weight(1f)
        )
      }

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (meshState.isProbingActive) {
          Button(
            onClick = onStopProbing,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.weight(1f).height(38.dp).testTag("stop_probe_mesh_btn")
          ) {
            Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Detener Sondeo (${meshState.currentProbeIndex + 1}/${meshState.config.totalPoints})", fontSize = 11.sp)
          }
        } else {
          Button(
            onClick = onStartProbing,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f).height(38.dp).testTag("start_probe_mesh_btn")
          ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Escanear Malla (G38.2)", fontSize = 11.sp)
          }
        }

        OutlinedButton(
          onClick = onApplyMeshToGCode,
          enabled = meshState.points.any { it.isProbed } && !meshState.isProbingActive,
          modifier = Modifier.weight(1f).height(38.dp).testTag("apply_mesh_gcode_btn")
        ) {
          Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (meshState.isMeshAppliedToGCode) "Re-aplicar a G-Code" else "Aplicar a G-Code", fontSize = 11.sp)
        }
      }
    }
  }

  // Config Dialog
  if (showConfigDialog) {
    MeshConfigDialog(
      currentConfig = meshState.config,
      onDismiss = { showConfigDialog = false },
      onSave = { newCfg ->
        onUpdateConfig(newCfg)
        showConfigDialog = false
      }
    )
  }
}

@Composable
private fun MeshHeatMapCanvas(
  meshState: AutoLevelMeshState,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val pad = 24f

    val config = meshState.config
    val cols = config.gridCols
    val rows = config.gridRows

    if (cols <= 1 || rows <= 1) return@Canvas

    val cellW = (w - 2 * pad) / (cols - 1)
    val cellH = (h - 2 * pad) / (rows - 1)

    // Draw background grid lines
    for (c in 0 until cols) {
      val x = pad + c * cellW
      drawLine(
        color = Color.White.copy(alpha = 0.1f),
        start = Offset(x, pad),
        end = Offset(x, h - pad),
        strokeWidth = 1f
      )
    }
    for (r in 0 until rows) {
      val y = pad + r * cellH
      drawLine(
        color = Color.White.copy(alpha = 0.1f),
        start = Offset(pad, y),
        end = Offset(w - pad, y),
        strokeWidth = 1f
      )
    }

    // Draw probed points and heatmap cells
    val points = meshState.points
    for (pt in points) {
      val px = pad + pt.gridX * cellW
      val py = pad + pt.gridY * cellH

      val pointColor = if (pt.isProbed) {
        val z = pt.zOffsetMm
        when {
          z > 0.05 -> DroAmber
          z < -0.05 -> DroCyan
          else -> DroGreen
        }
      } else {
        Color.Gray.copy(alpha = 0.4f)
      }

      drawCircle(
        color = pointColor,
        radius = if (pt.isProbed) 6f else 4f,
        center = Offset(px, py)
      )

      if (meshState.isProbingActive && pt.gridX + pt.gridY * cols == meshState.currentProbeIndex) {
        drawCircle(
          color = Color.White,
          radius = 10f,
          center = Offset(px, py),
          style = Stroke(width = 2f)
        )
      }
    }
  }
}

@Composable
private fun MeshConfigDialog(
  currentConfig: MeshGridConfig,
  onDismiss: () -> Unit,
  onSave: (MeshGridConfig) -> Unit
) {
  var xMinStr by remember { mutableStateOf(currentConfig.xMin.toString()) }
  var xMaxStr by remember { mutableStateOf(currentConfig.xMax.toString()) }
  var yMinStr by remember { mutableStateOf(currentConfig.yMin.toString()) }
  var yMaxStr by remember { mutableStateOf(currentConfig.yMax.toString()) }
  var colsStr by remember { mutableStateOf(currentConfig.gridCols.toString()) }
  var rowsStr by remember { mutableStateOf(currentConfig.gridRows.toString()) }
  var feedStr by remember { mutableStateOf(currentConfig.probeFeedRateMmMin.toString()) }
  var safeZStr by remember { mutableStateOf(currentConfig.safeZMm.toString()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Configuración de Malla de Sondeo") },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = xMinStr,
            onValueChange = { xMinStr = it },
            label = { Text("X Mín (mm)") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
          )
          OutlinedTextField(
            value = xMaxStr,
            onValueChange = { xMaxStr = it },
            label = { Text("X Máx (mm)") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
          )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = yMinStr,
            onValueChange = { yMinStr = it },
            label = { Text("Y Mín (mm)") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
          )
          OutlinedTextField(
            value = yMaxStr,
            onValueChange = { yMaxStr = it },
            label = { Text("Y Máx (mm)") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
          )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = colsStr,
            onValueChange = { colsStr = it },
            label = { Text("Puntos X (Cols)") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
          )
          OutlinedTextField(
            value = rowsStr,
            onValueChange = { rowsStr = it },
            label = { Text("Puntos Y (Filas)") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
          )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = safeZStr,
            onValueChange = { safeZStr = it },
            label = { Text("Z Seguro (mm)") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
          )
          OutlinedTextField(
            value = feedStr,
            onValueChange = { feedStr = it },
            label = { Text("Avance F (mm/min)") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val newConfig = MeshGridConfig(
            xMin = xMinStr.toDoubleOrNull() ?: currentConfig.xMin,
            xMax = xMaxStr.toDoubleOrNull() ?: currentConfig.xMax,
            yMin = yMinStr.toDoubleOrNull() ?: currentConfig.yMin,
            yMax = yMaxStr.toDoubleOrNull() ?: currentConfig.yMax,
            gridCols = (colsStr.toIntOrNull() ?: currentConfig.gridCols).coerceIn(2, 20),
            gridRows = (rowsStr.toIntOrNull() ?: currentConfig.gridRows).coerceIn(2, 20),
            safeZMm = safeZStr.toDoubleOrNull() ?: currentConfig.safeZMm,
            probeFeedRateMmMin = feedStr.toDoubleOrNull() ?: currentConfig.probeFeedRateMmMin
          )
          onSave(newConfig)
        }
      ) {
        Text("Guardar Malla")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancelar") }
    }
  )
}

/**
 * 3. MULTIPOINT AXIS CALIBRATION & ERROR UNCERTAINTY WIZARD
 */
@Composable
fun AxisMultipointCalibrationCard(
  session: AxisCalibrationSession,
  onStartSession: (CalibrationAxis, Double, Int, Double) -> Unit,
  onMoveToStep: (Int) -> Unit,
  onRecordMeasurement: (Int, Double) -> Unit,
  onApplyStepsToGrbl: () -> Unit,
  onResetSession: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedAxis by remember { mutableStateOf(session.axis) }
  var strokeLengthStr by remember { mutableStateOf(session.totalStrokeLengthMm.toString()) }
  var currentStepsStr by remember { mutableStateOf(session.currentStepsPerMm.toString()) }
  var measurementInputStr by remember { mutableStateOf("") }
  var activeMeasureStep by remember { mutableStateOf(session.currentStepIndex) }

  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(12.dp),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = DroAmber.copy(alpha = 0.2f)
          ) {
            Icon(
              imageVector = Icons.Default.Straighten,
              contentDescription = "Axis Multipoint Calibration",
              tint = DroAmber,
              modifier = Modifier.padding(6.dp).size(20.dp)
            )
          }
          Column {
            Text(
              text = "Calibración Multipunto e Incertidumbre",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Mapeo de error lineal, holgura y corrección de pasos/mm",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.sp
            )
          }
        }
      }

      // Axis Selector & Setup Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        CalibrationAxis.entries.forEach { axis ->
          val isSel = selectedAxis == axis
          FilterChip(
            selected = isSel,
            onClick = {
              selectedAxis = axis
              currentStepsStr = axis.defaultStepsPerMm.toString()
              onStartSession(
                axis,
                strokeLengthStr.toDoubleOrNull() ?: 300.0,
                10,
                axis.defaultStepsPerMm
              )
            },
            label = { Text(axis.label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
            modifier = Modifier.height(34.dp).testTag("calib_axis_${axis.name.lowercase()}")
          )
        }
      }

      // Setup inputs
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = strokeLengthStr,
          onValueChange = { strokeLengthStr = it },
          label = { Text("Longitud Carrera (mm)", fontSize = 10.sp) },
          textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
          modifier = Modifier.weight(1f).height(54.dp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
          value = currentStepsStr,
          onValueChange = { currentStepsStr = it },
          label = { Text("Pasos/mm Actuales", fontSize = 10.sp) },
          textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
          modifier = Modifier.weight(1f).height(54.dp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
      }

      // Step-by-Step Operator Calibration Flow
      val currentPoint = session.points.getOrNull(session.currentStepIndex)
      if (currentPoint != null) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "PASO ${session.currentStepIndex}/${session.points.size - 1} (${currentPoint.percentOfTravel}% de Carrera)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Objetivo Teórico: ${String.format(Locale.US, "%.2f mm", currentPoint.commandedTargetMm)}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              )
            }

            Text(
              text = "1. Mueva el eje al punto de prueba. 2. Mida con su instrumento de precisión. 3. Ingrese el valor real observado:",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Button(
                onClick = { onMoveToStep(session.currentStepIndex) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(42.dp).testTag("move_to_calib_step_btn")
              ) {
                Icon(imageVector = Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mover a ${currentPoint.percentOfTravel}%", fontSize = 11.sp)
              }

              OutlinedTextField(
                value = measurementInputStr,
                onValueChange = { measurementInputStr = it },
                label = { Text("Medida Real (mm)", fontSize = 10.sp) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f).height(54.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
              )

              Button(
                onClick = {
                  val realVal = measurementInputStr.toDoubleOrNull()
                  if (realVal != null) {
                    onRecordMeasurement(session.currentStepIndex, realVal)
                    measurementInputStr = ""
                  }
                },
                enabled = measurementInputStr.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DroGreen),
                modifier = Modifier.height(42.dp).testTag("record_measurement_btn")
              ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Guardar", fontSize = 11.sp)
              }
            }
          }
        }
      }

      // Uncertainty & Deviation Chart
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .height(130.dp)
          .clip(RoundedCornerShape(8.dp)),
        color = Color(0xFF10151C),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
      ) {
        ErrorDeviationChartCanvas(
          points = session.points,
          modifier = Modifier.fillMaxSize()
        )
      }

      // Summary of Results & Corrections
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
      ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text("DESVIACIÓN MÁXIMA", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(String.format(Locale.US, "%+.3f mm", session.maxDeviationMm), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (session.maxDeviationMm > 0.05) DroAmber else DroGreen)
            }
            Column {
              Text("ERROR PROMEDIO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(String.format(Locale.US, "%+.3f mm", session.avgDeviationMm), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Column {
              Text("INCERTIDUMBRE (σ)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(String.format(Locale.US, "±%.3f mm", session.stdDeviationMm), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = DroCyan)
            }
          }

          if (session.suggestedStepsPerMm != null) {
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("PASOS/MM CORREGIDOS (${session.axis.grblStepParam}):", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DroGreen)
                Text(
                  text = "${String.format(Locale.US, "%.3f", session.suggestedStepsPerMm)} pasos/mm",
                  style = MaterialTheme.typography.titleMedium,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Black,
                  color = DroGreen
                )
              }

              Button(
                onClick = onApplyStepsToGrbl,
                colors = ButtonDefaults.buttonColors(containerColor = DroGreen),
                modifier = Modifier.height(36.dp).testTag("apply_grbl_steps_btn")
              ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Grabar a EEPROM", fontSize = 11.sp)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ErrorDeviationChartCanvas(
  points: List<CalibrationPoint>,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val padX = 30f
    val padY = 20f

    val zeroY = h / 2f

    // Draw Zero Error centerline
    drawLine(
      color = Color.White.copy(alpha = 0.3f),
      start = Offset(padX, zeroY),
      end = Offset(w - padX, zeroY),
      strokeWidth = 1.5f,
      pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )

    if (points.isEmpty()) return@Canvas

    val maxTravel = points.maxOfOrNull { it.commandedTargetMm } ?: 300.0
    val scaleX = (w - 2 * padX) / if (maxTravel > 0) maxTravel.toFloat() else 1f
    val scaleY = (h / 2f - padY) / 0.2f // scale +/- 0.2mm range

    val measuredPoints = points.filter { it.isMeasured && it.errorMm != null }

    for (i in 0 until measuredPoints.size - 1) {
      val p1 = measuredPoints[i]
      val p2 = measuredPoints[i + 1]

      val x1 = padX + (p1.commandedTargetMm.toFloat() * scaleX)
      val y1 = (zeroY - (p1.errorMm!!.toFloat() * scaleY)).coerceIn(padY, h - padY)

      val x2 = padX + (p2.commandedTargetMm.toFloat() * scaleX)
      val y2 = (zeroY - (p2.errorMm!!.toFloat() * scaleY)).coerceIn(padY, h - padY)

      drawLine(
        color = DroCyan,
        start = Offset(x1, y1),
        end = Offset(x2, y2),
        strokeWidth = 3f
      )
    }

    // Draw dots
    for (pt in points) {
      val px = padX + (pt.commandedTargetMm.toFloat() * scaleX)
      val err = pt.errorMm
      if (err != null && pt.isMeasured) {
        val py = (zeroY - (err.toFloat() * scaleY)).coerceIn(padY, h - padY)
        val color = if (abs(err) > 0.05) DroAmber else DroGreen
        drawCircle(color = color, radius = 5f, center = Offset(px, py))
      } else {
        drawCircle(color = Color.Gray.copy(alpha = 0.3f), radius = 3f, center = Offset(px, zeroY))
      }
    }
  }
}
