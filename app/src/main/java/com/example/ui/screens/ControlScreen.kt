package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
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
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.CncViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

@Composable
fun ControlScreen(
  viewModel: CncViewModel,
  modifier: Modifier = Modifier
) {
  val machineState by viewModel.machineState.collectAsStateWithLifecycle()
  val jogConfig by viewModel.jogConfig.collectAsStateWithLifecycle()
  val wcsList by viewModel.wcsList.collectAsStateWithLifecycle()

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    val screenWidth = maxWidth
    val isLandscape = maxWidth > maxHeight
    val isTabletOrLandscape = screenWidth >= 640.dp || isLandscape
    val scrollState = rememberScrollState()

    if (isTabletOrLandscape) {
      // Responsive 2-Column Wide Tablet / Landscape Layout
      Row(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(scrollState)
          .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Left Pane: Machine Status Indicators & Coordinates DRO & Serial Link
        Column(
          modifier = Modifier
            .weight(1.1f)
            .fillMaxHeight(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Collapsible Work Coordinate Offsets (G54 - G59) Panel
          WcsOffsetsCollapsiblePanel(
            machineState = machineState,
            wcsList = wcsList,
            onSelectWcs = { viewModel.setWcs(it) },
            onResetWcsOffset = { viewModel.resetWcsOffset(it) },
            onResetWcsAxis = { wcs, axis -> viewModel.resetWcsSingleAxis(wcs, axis) },
            onTouchOffAxis = { wcs, axis, target -> viewModel.touchOffWcsAxis(wcs, axis, target) },
            onResetAllWcs = { viewModel.resetAllWcsOffsets() },
            onQueryControllerOffsets = { viewModel.queryWcsOffsetsFromController() },
            initiallyExpanded = false
          )

          // Machine Status Indicators: 4-Axis DRO Grid
          ResponsiveMachineDroGrid(
            machineState = machineState,
            onZeroAxis = { viewModel.zeroAxis(it) },
            onHomeAxis = { viewModel.homeAxis(it) },
            onZeroAll = { viewModel.zeroAxis("ALL") },
            onHomeAll = { viewModel.homeAxis("ALL") },
            isWideLayout = screenWidth >= 960.dp
          )

          // Serial Connection Status & Hardware Telemetry Hub
          SerialConnectionStatusCard(
            viewModel = viewModel
          )
        }

        // Right Pane: Industrial Jog Controls, Spindle & Overrides, Safety E-Stop
        Column(
          modifier = Modifier
            .weight(0.9f)
            .fillMaxHeight(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Manual Industrial On-Screen Jog Controls (X+, X-, Y+, Y-, Z+, Z-, A+, A-)
          OnScreenJogControls(
            jogConfig = jogConfig,
            onJogAxis = { axis, pos -> viewModel.jogAxis(axis, pos) },
            onStopJog = { axis -> viewModel.stopJog(axis) },
            onSetIncrement = { inc -> viewModel.setJogIncrement(inc) },
            onToggleContinuous = { cont -> viewModel.setJogContinuous(cont) },
            onSetSpeed = { spd -> viewModel.setJogSpeed(spd) },
            isLocked = machineState.isEStopActive || !machineState.isDriverEnabled,
            onSafeZRetract = { viewModel.jogAxis("Z", true) }
          )

          // Spindle & Coolant Control Card
          SpindleAndCoolantCard(
            machineState = machineState,
            onToggleSpindle = { dir, rpm -> viewModel.toggleSpindle(dir, rpm) },
            onToggleCoolant = { viewModel.toggleCoolant() }
          )

          // Real-Time Motion & Feedrate Speed Overrides Card (Slider UI)
          FeedrateOverrideCard(
            machineState = machineState,
            onSetFeedOverride = { viewModel.setFeedOverride(it) },
            onSetSpindleOverride = { viewModel.setSpindleOverride(it) },
            onSetRapidOverride = { viewModel.setRapidOverride(it) }
          )
        }
      }
    } else {
      // Compact Phone Portrait Layout (Single Column)
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(scrollState)
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Serial Connection Status Card at Top
        SerialConnectionStatusCard(
          viewModel = viewModel
        )

        // Collapsible Work Coordinate Offsets (G54 - G59) Panel
        WcsOffsetsCollapsiblePanel(
          machineState = machineState,
          wcsList = wcsList,
          onSelectWcs = { viewModel.setWcs(it) },
          onResetWcsOffset = { viewModel.resetWcsOffset(it) },
          onResetWcsAxis = { wcs, axis -> viewModel.resetWcsSingleAxis(wcs, axis) },
          onTouchOffAxis = { wcs, axis, target -> viewModel.touchOffWcsAxis(wcs, axis, target) },
          onResetAllWcs = { viewModel.resetAllWcsOffsets() },
          onQueryControllerOffsets = { viewModel.queryWcsOffsetsFromController() },
          initiallyExpanded = false
        )

        // Machine Coordinates Digital Readout (DRO)
        ResponsiveMachineDroGrid(
          machineState = machineState,
          onZeroAxis = { viewModel.zeroAxis(it) },
          onHomeAxis = { viewModel.homeAxis(it) },
          onZeroAll = { viewModel.zeroAxis("ALL") },
          onHomeAll = { viewModel.homeAxis("ALL") },
          isWideLayout = false
        )

        // Manual Industrial On-Screen Jog Controls (X+, X-, Y+, Y-, Z+, Z-, A+, A-)
        OnScreenJogControls(
          jogConfig = jogConfig,
          onJogAxis = { axis, pos -> viewModel.jogAxis(axis, pos) },
          onStopJog = { axis -> viewModel.stopJog(axis) },
          onSetIncrement = { inc -> viewModel.setJogIncrement(inc) },
          onToggleContinuous = { cont -> viewModel.setJogContinuous(cont) },
          onSetSpeed = { spd -> viewModel.setJogSpeed(spd) },
          isLocked = machineState.isEStopActive || !machineState.isDriverEnabled,
          onSafeZRetract = { viewModel.jogAxis("Z", true) }
        )

        // Spindle & Coolant Controls
        SpindleAndCoolantCard(
          machineState = machineState,
          onToggleSpindle = { dir, rpm -> viewModel.toggleSpindle(dir, rpm) },
          onToggleCoolant = { viewModel.toggleCoolant() }
        )

        // Motion & Feedrate Speed Overrides Card (Slider UI)
        FeedrateOverrideCard(
          machineState = machineState,
          onSetFeedOverride = { viewModel.setFeedOverride(it) },
          onSetSpindleOverride = { viewModel.setSpindleOverride(it) },
          onSetRapidOverride = { viewModel.setRapidOverride(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun SpindleAndCoolantCard(
  machineState: MachineState,
  onToggleSpindle: (SpindleDirection, Int) -> Unit,
  onToggleCoolant: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(10.dp),
    border = CardDefaults.outlinedCardBorder()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      Text(
        text = "SPINDLE & AUXILIARY CONTROLS",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // RPM Readout & Dial
        Column {
          Text("ACTUAL SPINDLE RPM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            text = "${machineState.actualRpm} RPM",
            style = MaterialTheme.typography.displayMedium,
            color = if (machineState.actualRpm > 0) DroGreen else MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 22.sp
          )
          // Spindle Load Bar
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LOAD:", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
            Spacer(modifier = Modifier.width(6.dp))
            LinearProgressIndicator(
              progress = { machineState.spindleLoadPct },
              modifier = Modifier
                .width(110.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = if (machineState.spindleLoadPct > 0.8f) IndDarkEmergency else DroCyan,
              trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${(machineState.spindleLoadPct * 100).toInt()}%",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 9.sp
            )
          }
        }

        // Spindle Start / Stop buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Button(
            onClick = { onToggleSpindle(SpindleDirection.CW, machineState.targetRpm) },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (machineState.spindleDirection == SpindleDirection.CW) DroGreen else MaterialTheme.colorScheme.surfaceVariant,
              contentColor = if (machineState.spindleDirection == SpindleDirection.CW) Color.Black else MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.testTag("spindle_cw_btn")
          ) {
            Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Spindle CW", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("CW (M3)", style = MaterialTheme.typography.labelSmall)
          }

          Button(
            onClick = onToggleCoolant,
            colors = ButtonDefaults.buttonColors(
              containerColor = if (machineState.coolantState != CoolantState.OFF) DroCyan else MaterialTheme.colorScheme.surfaceVariant,
              contentColor = if (machineState.coolantState != CoolantState.OFF) Color.Black else MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.testTag("coolant_toggle_btn")
          ) {
            Icon(Icons.Default.WaterDrop, contentDescription = "Coolant", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(machineState.coolantState.name, style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }
  }
}

