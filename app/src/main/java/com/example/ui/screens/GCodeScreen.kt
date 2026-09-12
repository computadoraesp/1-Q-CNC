package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.AutoLevelMeshCard
import com.example.ui.components.JobTimeEstimatorCard
import com.example.ui.components.ToolpathVisualizer
import com.example.ui.theme.*
import com.example.viewmodel.CncViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

@Composable
fun GCodeScreen(
  viewModel: CncViewModel,
  modifier: Modifier = Modifier
) {
  val machineState by viewModel.machineState.collectAsStateWithLifecycle()
  val gcodeFiles by viewModel.gcodeFiles.collectAsStateWithLifecycle()
  val jobEstimate by viewModel.jobEstimate.collectAsStateWithLifecycle()
  val autoLevelMeshState by viewModel.autoLevelMeshState.collectAsStateWithLifecycle()

  val activeFile = gcodeFiles.find { it.name == machineState.gcodeFileName } ?: gcodeFiles.first()

  val listState = rememberLazyListState()
  val scrollState = rememberScrollState()

  // Auto-scroll G-Code viewer to active line
  LaunchedEffect(machineState.activeGCodeLine) {
    if (machineState.activeGCodeLine > 0 && machineState.activeGCodeLine < activeFile.sampleLines.size) {
      listState.animateScrollToItem((machineState.activeGCodeLine - 2).coerceAtLeast(0))
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // File selector & Program header
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(10.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.InsertDriveFile,
              contentDescription = "G-Code File",
              tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = activeFile.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )
              Text(
                text = "${activeFile.lineCount} lines • Est: ${jobEstimate.formattedTotalTime} • Tool T${machineState.currentToolNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // File picker dropdown or switch
          var showFileMenu by remember { mutableStateOf(false) }
          Box {
            OutlinedButton(
              onClick = { showFileMenu = true },
              shape = RoundedCornerShape(6.dp),
              modifier = Modifier.testTag("select_program_btn")
            ) {
              Text("PROGRAMS", style = MaterialTheme.typography.labelSmall)
              Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
              expanded = showFileMenu,
              onDismissRequest = { showFileMenu = false }
            ) {
              gcodeFiles.forEach { file ->
                DropdownMenuItem(
                  text = { Text(file.name, fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                  onClick = {
                    viewModel.selectGCodeProgram(file)
                    showFileMenu = false
                  }
                )
              }
            }
          }
        }

        // Progress bar
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          LinearProgressIndicator(
            progress = { machineState.gcodeProgressPct },
            modifier = Modifier
              .weight(1f)
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp)),
            color = DroCyan,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "${(machineState.gcodeProgressPct * 100).toInt()}% [Line ${machineState.activeGCodeLine}/${machineState.totalGCodeLines}]",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = DroCyan
          )
        }
      }
    }

    // 1. Kinematic Job Time Estimator Card
    JobTimeEstimatorCard(
      estimate = jobEstimate,
      onRecalculate = { viewModel.recalculateJobEstimate() }
    )

    // 2. 2D/3D Toolpath Visualizer
    ToolpathVisualizer(
      toolpathPoints = activeFile.toolpathPoints,
      currentToolX = machineState.posX,
      currentToolY = machineState.posY,
      currentToolZ = machineState.posZ,
      activeLineIndex = machineState.activeGCodeLine
    )

    // 3. PCB Auto-Leveling Mesh & Surface Probe Card
    AutoLevelMeshCard(
      meshState = autoLevelMeshState,
      onStartProbing = { viewModel.startAutoLevelMeshProbing() },
      onStopProbing = { viewModel.stopAutoLevelMeshProbing() },
      onApplyMeshToGCode = { viewModel.applyMeshToActiveGCode() },
      onClearMesh = { viewModel.clearMesh() },
      onUpdateConfig = { newCfg -> viewModel.updateMeshConfig(newCfg) }
    )

    // Program Execution Action Bar (Cycle Start, Pause, Stop, Step)
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(10.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Cycle Start / Resume Button
        Button(
          onClick = {
            if (machineState.isGcodePaused) viewModel.resumeGCode() else viewModel.startGCode()
          },
          enabled = !machineState.isGcodeRunning && !machineState.isEStopActive,
          colors = ButtonDefaults.buttonColors(
            containerColor = DroGreen,
            contentColor = Color.Black
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("cycle_start_btn")
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = "Cycle Start")
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (machineState.isGcodePaused) "RESUME" else "CYCLE START",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Feed Hold / Pause
        Button(
          onClick = { viewModel.pauseGCode() },
          enabled = machineState.isGcodeRunning,
          colors = ButtonDefaults.buttonColors(
            containerColor = IndDarkWarning,
            contentColor = Color.Black
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("feed_hold_btn")
        ) {
          Icon(Icons.Default.Pause, contentDescription = "Feed Hold")
          Spacer(modifier = Modifier.width(4.dp))
          Text("FEED HOLD", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Stop Program
        Button(
          onClick = { viewModel.stopGCode() },
          enabled = machineState.isGcodeRunning || machineState.isGcodePaused,
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("stop_program_btn")
        ) {
          Icon(Icons.Default.Stop, contentDescription = "Stop Program")
          Spacer(modifier = Modifier.width(4.dp))
          Text("STOP", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Single Step
        OutlinedButton(
          onClick = { viewModel.stepGCode() },
          enabled = !machineState.isGcodeRunning && !machineState.isEStopActive,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .height(48.dp)
            .testTag("single_step_btn")
        ) {
          Icon(Icons.Default.SkipNext, contentDescription = "Single Step")
          Spacer(modifier = Modifier.width(2.dp))
          Text("STEP", style = MaterialTheme.typography.labelSmall)
        }
      }
    }

    // G-Code Text Viewer (Live Line-by-Line Highlight)
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .height(200.dp),
      colors = CardDefaults.cardColors(containerColor = DroBackground),
      shape = RoundedCornerShape(10.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "G-CODE BLOCK EXECUTION STREAM",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "F=${machineState.feedRateCurrent.toInt()} mm/min",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = DroGreen
          )
        }

        LazyColumn(
          state = listState,
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          itemsIndexed(activeFile.sampleLines) { index, line ->
            val isCurrentLine = (index + 1) == machineState.activeGCodeLine
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(if (isCurrentLine) DroCyan.copy(alpha = 0.25f) else Color.Transparent)
                .border(
                  width = if (isCurrentLine) 1.dp else 0.dp,
                  color = if (isCurrentLine) DroCyan else Color.Transparent,
                  shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = String.format(Locale.US, "%03d", index + 1),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = if (isCurrentLine) DroCyan else Color.Gray,
                modifier = Modifier.width(36.dp)
              )
              Text(
                text = line,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = if (isCurrentLine) DroWhite else Color.LightGray,
                fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}
