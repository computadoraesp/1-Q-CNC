package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.model.CommissioningPhase
import com.example.ui.components.AxisMultipointCalibrationCard
import com.example.ui.theme.*
import com.example.viewmodel.CncViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

enum class CommissioningSubTab(val label: String) {
  CALIBRATION("Calibración Multipunto por Eje"),
  ROADMAP_PHASES("Fases de Puesta en Marcha")
}

@Composable
fun CommissioningScreen(
  viewModel: CncViewModel,
  modifier: Modifier = Modifier
) {
  val machineState by viewModel.machineState.collectAsStateWithLifecycle()
  val phases by viewModel.commissioningPhases.collectAsStateWithLifecycle()
  val calibrationSession by viewModel.axisCalibrationSession.collectAsStateWithLifecycle()

  var subTab by remember { mutableStateOf(CommissioningSubTab.CALIBRATION) }

  val completedCount = phases.count { it.isCompleted }
  val totalCount = phases.size
  val progressPct = completedCount.toFloat() / totalCount.toFloat()

  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Top Tab Selector (Calibración vs Fases)
    TabRow(
      selectedTabIndex = subTab.ordinal,
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.primary,
      modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
    ) {
      CommissioningSubTab.entries.forEach { tab ->
        Tab(
          selected = subTab == tab,
          onClick = { subTab = tab },
          text = {
            Text(
              text = tab.label,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (subTab == tab) FontWeight.Bold else FontWeight.Normal
            )
          }
        )
      }
    }

    if (subTab == CommissioningSubTab.CALIBRATION) {
      // Multipoint Axis Calibration Wizard & Uncertainty Graph
      AxisMultipointCalibrationCard(
        session = calibrationSession,
        onStartSession = { axis, length, interval, steps ->
          viewModel.startAxisCalibration(axis, length, interval, steps)
        },
        onMoveToStep = { stepIdx -> viewModel.moveToCalibrationStep(stepIdx) },
        onRecordMeasurement = { stepIdx, measuredVal -> viewModel.recordAxisMeasurement(stepIdx, measuredVal) },
        onApplyStepsToGrbl = { viewModel.applyCalibratedStepsPerMmToGrbl() },
        onResetSession = { viewModel.resetAxisCalibration() }
      )
    } else {
      // Header & Summary Card for 16-Phase Commissioning
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
            Text(
              text = "MASTER WORK PLAN & COMMISSIONING",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(DroGreen.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "$completedCount / $totalCount PASSED",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = DroGreen,
                fontFamily = FontFamily.Monospace
              )
            }
          }

          Text(
            text = "Arduino UNO Q (QRB2210 Linux + STM32U585/Zephyr) CNC Integration Roadmap",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Progress Bar
          LinearProgressIndicator(
            progress = { progressPct },
            modifier = Modifier
              .fillMaxWidth()
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp)),
            color = DroGreen,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )
        }
      }

      // 16-Phase Accordion / Cards List
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        phases.forEach { phase ->
          CommissioningPhaseCard(
            phase = phase,
            onRunTest = { viewModel.runCommissioningTest(phase.phaseNumber) }
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
fun CommissioningPhaseCard(
  phase: CommissioningPhase,
  onRunTest: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }

  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(8.dp),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          // Phase number badge
          Box(
            modifier = Modifier
              .size(30.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(if (phase.isCompleted) DroGreen else Color.DarkGray),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "P${phase.phaseNumber}",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Black,
              color = if (phase.isCompleted) Color.Black else Color.White
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = "PHASE ${phase.phaseNumber}: ${phase.title.uppercase()}",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = phase.passCriteria,
              style = MaterialTheme.typography.labelSmall,
              color = if (phase.isCompleted) DroGreen else MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 10.sp
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (phase.testActionName != null) {
            Button(
              onClick = onRunTest,
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
              modifier = Modifier
                .height(30.dp)
                .testTag("test_phase_${phase.phaseNumber}"),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (phase.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                contentColor = if (phase.isCompleted) MaterialTheme.colorScheme.onSurface else Color.Black
              )
            ) {
              Text(phase.testActionName, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
            }
          }

          IconButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.size(30.dp)
          ) {
            Icon(
              imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = "Expand details"
            )
          }
        }
      }

      if (isExpanded) {
        Spacer(modifier = Modifier.height(8.dp))
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
        ) {
          Text(
            text = "Description:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = phase.description,
            style = MaterialTheme.typography.bodySmall
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Verification Proof & Notes:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = DroCyan
          )
          Text(
            text = phase.notes,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = Color.LightGray
          )
        }
      }
    }
  }
}
