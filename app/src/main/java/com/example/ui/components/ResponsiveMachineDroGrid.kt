package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun ResponsiveMachineDroGrid(
  machineState: MachineState,
  onZeroAxis: (String) -> Unit,
  onHomeAxis: (String) -> Unit,
  onZeroAll: () -> Unit,
  onHomeAll: () -> Unit,
  modifier: Modifier = Modifier,
  isWideLayout: Boolean = false
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("machine_status_dro_grid"),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Header: Coordinates Title, Active Units, and Zero/Home All
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Grid4x4,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(18.dp)
            )
          }

          Column {
            Text(
              text = "DIGITAL READOUT (DRO)",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Black,
              letterSpacing = 0.5.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Active WCS: ${machineState.wcsName} • Feed: ${String.format(Locale.US, "%.0f", machineState.feedRateCurrent)} mm/min",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Global Zero & Home All Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Button(
            onClick = onHomeAll,
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier.height(34.dp).testTag("dro_home_all_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = Color.Black
            )
          ) {
            Icon(Icons.Default.Home, contentDescription = "Home All", modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("HOME ALL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = onZeroAll,
            shape = RoundedCornerShape(6.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier.height(34.dp).testTag("dro_zero_all_button")
          ) {
            Text("ZERO ALL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
          }
        }
      }

      // Live Hardware Limit & Probe Sensor Ticker Bar
      LimitAndProbeStatusBar(machineState = machineState)

      // 4-Axis Displays: Responsive Split (2x2 grid if wide, or stacked)
      if (isWideLayout) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            SophisticatedDroCard(
              axisName = "X",
              workPos = machineState.posX,
              machinePos = machineState.machinePosX,
              isHomed = machineState.isHomedX,
              isLimitActive = machineState.limitXPos || machineState.limitXNeg,
              accentColor = DroAmber,
              onZeroAxis = { onZeroAxis("X") },
              onHomeAxis = { onHomeAxis("X") },
              modifier = Modifier.weight(1f)
            )

            SophisticatedDroCard(
              axisName = "Y",
              workPos = machineState.posY,
              machinePos = machineState.machinePosY,
              isHomed = machineState.isHomedY,
              isLimitActive = machineState.limitYPos || machineState.limitYNeg,
              accentColor = DroCyan,
              onZeroAxis = { onZeroAxis("Y") },
              onHomeAxis = { onHomeAxis("Y") },
              modifier = Modifier.weight(1f)
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            SophisticatedDroCard(
              axisName = "Z",
              workPos = machineState.posZ,
              machinePos = machineState.machinePosZ,
              isHomed = machineState.isHomedZ,
              isLimitActive = machineState.limitZPos || machineState.limitZNeg,
              accentColor = DroGreen,
              onZeroAxis = { onZeroAxis("Z") },
              onHomeAxis = { onHomeAxis("Z") },
              modifier = Modifier.weight(1f)
            )

            SophisticatedDroCard(
              axisName = "A",
              workPos = machineState.posA,
              machinePos = machineState.machinePosA,
              isHomed = machineState.isHomedA,
              isLimitActive = false,
              accentColor = IndDarkWarning,
              unitLabel = "°",
              onZeroAxis = { onZeroAxis("A") },
              onHomeAxis = { onHomeAxis("A") },
              modifier = Modifier.weight(1f)
            )
          }
        }
      } else {
        // Stacked Cards for Compact screens
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SophisticatedDroCard(
            axisName = "X",
            workPos = machineState.posX,
            machinePos = machineState.machinePosX,
            isHomed = machineState.isHomedX,
            isLimitActive = machineState.limitXPos || machineState.limitXNeg,
            accentColor = DroAmber,
            onZeroAxis = { onZeroAxis("X") },
            onHomeAxis = { onHomeAxis("X") }
          )

          SophisticatedDroCard(
            axisName = "Y",
            workPos = machineState.posY,
            machinePos = machineState.machinePosY,
            isHomed = machineState.isHomedY,
            isLimitActive = machineState.limitYPos || machineState.limitYNeg,
            accentColor = DroCyan,
            onZeroAxis = { onZeroAxis("Y") },
            onHomeAxis = { onHomeAxis("Y") }
          )

          SophisticatedDroCard(
            axisName = "Z",
            workPos = machineState.posZ,
            machinePos = machineState.machinePosZ,
            isHomed = machineState.isHomedZ,
            isLimitActive = machineState.limitZPos || machineState.limitZNeg,
            accentColor = DroGreen,
            onZeroAxis = { onZeroAxis("Z") },
            onHomeAxis = { onHomeAxis("Z") }
          )

          SophisticatedDroCard(
            axisName = "A",
            workPos = machineState.posA,
            machinePos = machineState.machinePosA,
            isHomed = machineState.isHomedA,
            isLimitActive = false,
            accentColor = IndDarkWarning,
            unitLabel = "°",
            onZeroAxis = { onZeroAxis("A") },
            onHomeAxis = { onHomeAxis("A") }
          )
        }
      }
    }
  }
}

@Composable
fun SophisticatedDroCard(
  axisName: String,
  workPos: Double,
  machinePos: Double,
  isHomed: Boolean,
  isLimitActive: Boolean,
  accentColor: Color,
  onZeroAxis: () -> Unit,
  onHomeAxis: () -> Unit,
  modifier: Modifier = Modifier,
  unitLabel: String = "mm"
) {
  Surface(
    modifier = modifier.testTag("dro_card_$axisName"),
    color = DroBackground,
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(
      1.dp,
      Brush.horizontalGradient(
        listOf(
          accentColor.copy(alpha = if (isLimitActive) 1f else 0.7f),
          if (isLimitActive) IndDarkEmergency else MaterialTheme.colorScheme.outlineVariant
        )
      )
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Axis Badge & State Flags
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .border(1.5.dp, accentColor, RoundedCornerShape(8.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = axisName,
            style = MaterialTheme.typography.titleLarge,
            color = accentColor,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isHomed) DroGreen else IndDarkTextMuted)
            )
            Text(
              text = if (isHomed) "HOMED" else "UNHOMED",
              style = MaterialTheme.typography.labelSmall,
              color = if (isHomed) DroGreen else IndDarkTextMuted,
              fontWeight = FontWeight.Bold,
              fontSize = 9.sp
            )

            if (isLimitActive) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(3.dp))
                  .background(IndDarkEmergency)
                  .padding(horizontal = 4.dp, vertical = 1.dp)
              ) {
                Text(
                  text = "LIMIT",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 8.sp
                )
              }
            }
          }

          Text(
            text = "MCS: ${String.format(Locale.US, "%.3f", machinePos)}",
            style = MaterialTheme.typography.labelSmall,
            color = IndDarkTextMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }

      // Coordinate Number in High-Contrast Glowing Digital Readout
      Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = String.format(Locale.US, "%+08.3f", workPos),
          style = MaterialTheme.typography.displayMedium,
          color = DroWhite,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Black,
          letterSpacing = 1.sp,
          fontSize = 22.sp
        )
        Text(
          text = unitLabel,
          style = MaterialTheme.typography.labelSmall,
          color = accentColor,
          fontSize = 9.sp,
          modifier = Modifier.padding(bottom = 3.dp)
        )
      }

      // Action Buttons: Touch-Off Zero & Home
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(
          onClick = onZeroAxis,
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
          modifier = Modifier
            .height(32.dp)
            .testTag("dro_zero_btn_$axisName")
        ) {
          Text("ZERO", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        FilledTonalIconButton(
          onClick = onHomeAxis,
          shape = RoundedCornerShape(6.dp),
          modifier = Modifier
            .size(32.dp)
            .testTag("dro_home_btn_$axisName")
        ) {
          Icon(Icons.Default.Home, contentDescription = "Home $axisName", modifier = Modifier.size(14.dp))
        }
      }
    }
  }
}

@Composable
private fun LimitAndProbeStatusBar(machineState: MachineState) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = DroBackground,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "LIMIT SWITCHES:",
          style = MaterialTheme.typography.labelSmall,
          fontSize = 9.sp,
          color = IndDarkTextMuted,
          fontWeight = FontWeight.Bold
        )

        LimitIndicator(name = "X+", isActive = machineState.limitXPos)
        LimitIndicator(name = "X-", isActive = machineState.limitXNeg)
        LimitIndicator(name = "Y+", isActive = machineState.limitYPos)
        LimitIndicator(name = "Y-", isActive = machineState.limitYNeg)
        LimitIndicator(name = "Z+", isActive = machineState.limitZPos)
        LimitIndicator(name = "Z-", isActive = machineState.limitZNeg)
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Box(
          modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(if (machineState.probeTripped) DroGreen else IndDarkTextMuted)
        )
        Text(
          text = if (machineState.probeTripped) "PROBE TRIPPED" else "PROBE READY",
          style = MaterialTheme.typography.labelSmall,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = if (machineState.probeTripped) DroGreen else IndDarkTextMuted
        )
      }
    }
  }
}

@Composable
private fun LimitIndicator(name: String, isActive: Boolean) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(3.dp)
  ) {
    Box(
      modifier = Modifier
        .size(6.dp)
        .clip(CircleShape)
        .background(if (isActive) IndDarkEmergency else Color(0xFF43474E))
    )
    Text(
      text = name,
      style = MaterialTheme.typography.labelSmall,
      fontSize = 8.sp,
      color = if (isActive) IndDarkEmergency else IndDarkTextMuted,
      fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
    )
  }
}
