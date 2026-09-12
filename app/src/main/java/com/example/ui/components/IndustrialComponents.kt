package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.*
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun TopMachineStatusBar(
  machineState: MachineState,
  mcuDiagnostics: McuDiagnostics,
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  onEStopToggle: () -> Unit,
  onFeedHoldToggle: () -> Unit = {},
  onDriverToggle: () -> Unit = {},
  onClearAlarms: () -> Unit,
  modifier: Modifier = Modifier,
  isLandscape: Boolean = false,
  isEcoMode: Boolean = false
) {
  // Battery & GPU Optimization: Only animate watchdog when Eco Mode is off
  val watchdogPulseAlpha: Float = if (isEcoMode) {
    1.0f
  } else {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
      initialValue = 0.4f,
      targetValue = 1.0f,
      animationSpec = infiniteRepeatable(
        animation = tween(800, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "watchdogAlpha"
    )
    alpha
  }

  // Battery Optimization: Only run emergency blinking loop when E-Stop is actively engaged
  val estopBlinkAlpha: Float = if (!machineState.isEStopActive || isEcoMode) {
    1.0f
  } else {
    val infiniteTransition = rememberInfiniteTransition(label = "estop_pulse")
    val alpha by infiniteTransition.animateFloat(
      initialValue = 0.75f,
      targetValue = 1.0f,
      animationSpec = infiniteRepeatable(
        animation = tween(400, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "estopBlink"
    )
    alpha
  }

  val stateColor = when {
    machineState.isEStopActive -> IndDarkEmergency
    machineState.isGcodeRunning -> DroGreen
    machineState.isGcodePaused -> IndDarkWarning
    else -> MaterialTheme.colorScheme.primary
  }
  val stateText = when {
    machineState.isEStopActive -> "E-STOP"
    machineState.isGcodeRunning -> "RUNNING"
    machineState.isGcodePaused -> "PAUSED"
    else -> "READY"
  }

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp,
    shadowElevation = 4.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = if (isLandscape) 4.dp else 6.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      if (isLandscape) {
        // Compact Single-Row Layout for Landscape Mode
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Logo & Machine Status Pill
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Image(
              painter = painterResource(id = R.drawable.q1_droid_logo),
              contentDescription = "Q1 Droid CNC Logo",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
            Text(
              text = "1 Q CNC",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Black
            )
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(stateColor)
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = stateText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 9.sp
              )
            }
          }

          // 1. PRIMARY EMERGENCY STOP BUTTON
          Button(
            onClick = onEStopToggle,
            modifier = Modifier
              .weight(1.3f)
              .height(38.dp)
              .testTag("top_emergency_stop_button"),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (machineState.isEStopActive) IndDarkSecondary else IndDarkEmergency.copy(alpha = estopBlinkAlpha),
              contentColor = Color.White
            ),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
          ) {
            Icon(
              imageVector = if (machineState.isEStopActive) Icons.Default.LockReset else Icons.Default.Dangerous,
              contentDescription = "Parada de Emergencia",
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (machineState.isEStopActive) "RESET E-STOP" else "PARADA DE EMERGENCIA",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Black,
              fontSize = 10.sp
            )
          }

          // 2. FEED HOLD / RESUME BUTTON
          Button(
            onClick = onFeedHoldToggle,
            modifier = Modifier
              .weight(0.9f)
              .height(38.dp)
              .testTag("top_feed_hold_button"),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (machineState.isGcodePaused) DroGreen else if (machineState.isGcodeRunning) IndDarkWarning else MaterialTheme.colorScheme.surfaceVariant,
              contentColor = if (machineState.isGcodePaused || machineState.isGcodeRunning) Color.Black else MaterialTheme.colorScheme.onSurface
            ),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            enabled = !machineState.isEStopActive
          ) {
            Icon(
              imageVector = if (machineState.isGcodePaused) Icons.Default.PlayArrow else Icons.Default.Pause,
              contentDescription = "Pausa",
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (machineState.isGcodePaused) "REANUDAR" else "PAUSA (HOLD)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              fontSize = 9.5.sp
            )
          }

          // 3. DRIVERS TOGGLE
          Surface(
            onClick = onDriverToggle,
            modifier = Modifier
              .height(38.dp)
              .testTag("top_drivers_toggle"),
            shape = RoundedCornerShape(6.dp),
            color = if (machineState.isDriverEnabled) MaterialTheme.colorScheme.surfaceVariant else IndDarkEmergency.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, if (machineState.isDriverEnabled) DroGreen.copy(alpha = 0.5f) else IndDarkEmergency)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(if (machineState.isDriverEnabled) DroGreen else DroRed)
              )
              Text(
                text = if (machineState.isDriverEnabled) "MOTORES ON" else "MOTORES OFF",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          // RT Latency badge
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .padding(horizontal = 6.dp, vertical = 4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (mcuDiagnostics.watchdogHealthy) DroGreen.copy(alpha = watchdogPulseAlpha) else DroRed)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "${mcuDiagnostics.worstLatencyUs}µs",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold
            )
          }

          // Theme toggle button
          IconButton(
            onClick = onToggleTheme,
            modifier = Modifier
              .size(32.dp)
              .testTag("theme_toggle_button")
          ) {
            Icon(
              imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
              contentDescription = "Toggle Light/Dark Theme",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      } else {
        // Standard 2-Row Portrait Layout
        // Top Navigation / Brand / Status Summary Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Logo & Platform indicator
          Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
              painter = painterResource(id = R.drawable.q1_droid_logo),
              contentDescription = "Q1 Droid CNC Logo",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "1 Q CNC",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                  Text(
                    text = "LinuxCNC + STM32",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
              Text(
                text = if (machineState.connectionStatus == ConnectionStatus.SIMULATED) "Virtual Controller (Direct)" else "${machineState.ipAddress}:${machineState.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
              )
            }
          }

          // Status badges & Actions
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Watchdog heartbeat LED
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(7.dp)
                  .clip(CircleShape)
                  .background(
                    if (mcuDiagnostics.watchdogHealthy) DroGreen.copy(alpha = watchdogPulseAlpha) else DroRed
                  )
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "RT ${mcuDiagnostics.worstLatencyUs}µs",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              )
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(stateColor)
                .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
              Text(
                text = stateText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 10.sp
              )
            }

            // Theme toggle button
            IconButton(
              onClick = onToggleTheme,
              modifier = Modifier
                .size(32.dp)
                .testTag("theme_toggle_button")
            ) {
              Icon(
                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle Light/Dark Theme",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        // PERMANENT TOP EMERGENCY STOP & SAFETY CONTROL BAR (ALWAYS PRESENT ON ALL SCREENS)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("permanent_top_safety_bar"),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // 1. PRIMARY PROMINENT EMERGENCY STOP BUTTON (PARADA DE EMERGENCIA)
          Button(
            onClick = onEStopToggle,
            modifier = Modifier
              .weight(1.4f)
              .height(46.dp)
              .testTag("top_emergency_stop_button"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (machineState.isEStopActive) IndDarkSecondary else IndDarkEmergency.copy(alpha = estopBlinkAlpha),
              contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(
              defaultElevation = if (machineState.isEStopActive) 2.dp else 6.dp,
              pressedElevation = 2.dp
            ),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = if (machineState.isEStopActive) Icons.Default.LockReset else Icons.Default.Dangerous,
                contentDescription = "Parada de Emergencia",
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column(horizontalAlignment = Alignment.Start) {
                Text(
                  text = if (machineState.isEStopActive) "RESET E-STOP" else "PARADA DE EMERGENCIA",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Black,
                  fontSize = 11.sp,
                  letterSpacing = 0.3.sp
                )
                Text(
                  text = if (machineState.isEStopActive) "DESBLOQUEAR MOTORES" else "E-STOP (CORTE INMEDIATO)",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 7.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (machineState.isEStopActive) Color.White.copy(alpha = 0.8f) else Color.Yellow
                )
              }
            }
          }

          // 2. FEED HOLD / RESUME BUTTON (PAUSA DE AVANCE)
          Button(
            onClick = onFeedHoldToggle,
            modifier = Modifier
              .weight(0.9f)
              .height(46.dp)
              .testTag("top_feed_hold_button"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (machineState.isGcodePaused) DroGreen else if (machineState.isGcodeRunning) IndDarkWarning else MaterialTheme.colorScheme.surfaceVariant,
              contentColor = if (machineState.isGcodePaused || machineState.isGcodeRunning) Color.Black else MaterialTheme.colorScheme.onSurface
            ),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            enabled = !machineState.isEStopActive
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = if (machineState.isGcodePaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = "Feed Hold / Pausa",
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Column(horizontalAlignment = Alignment.Start) {
                Text(
                  text = if (machineState.isGcodePaused) "REANUDAR" else "PAUSA (HOLD)",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Black,
                  fontSize = 10.sp
                )
                Text(
                  text = if (machineState.isGcodePaused) "CYCLE START" else "FEED HOLD (!)",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 7.5.sp
                )
              }
            }
          }

          // 3. STEP/DIR DRIVERS ENABLE / POWER TOGGLE
          Surface(
            onClick = onDriverToggle,
            modifier = Modifier
              .height(46.dp)
              .testTag("top_drivers_toggle"),
            shape = RoundedCornerShape(8.dp),
            color = if (machineState.isDriverEnabled) MaterialTheme.colorScheme.surfaceVariant else IndDarkEmergency.copy(alpha = 0.2f),
            border = BorderStroke(
              1.dp,
              if (machineState.isDriverEnabled) DroGreen.copy(alpha = 0.5f) else IndDarkEmergency
            )
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(9.dp)
                  .clip(CircleShape)
                  .background(if (machineState.isDriverEnabled) DroGreen else DroRed)
              )
              Column {
                Text(
                  text = "DRIVERS",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = if (machineState.isDriverEnabled) "ON" else "OFF",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Black,
                  fontSize = 10.sp,
                  color = if (machineState.isDriverEnabled) DroGreen else DroRed
                )
              }
            }
          }
        }
      }

      // Active Alarms / System Message Ticker
      if (machineState.activeAlarms.isNotEmpty()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(IndDarkEmergency.copy(alpha = 0.2f))
            .border(1.dp, IndDarkEmergency, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = "Alarm",
              tint = IndDarkEmergency,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = machineState.activeAlarms.first(),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1
            )
          }
          TextButton(
            onClick = onClearAlarms,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.height(26.dp)
          ) {
            Text("LIMPIAR ALARMA", style = MaterialTheme.typography.labelSmall, color = IndDarkEmergency, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun AxisDroCard(
  axisName: String,
  posValue: Double,
  machinePosValue: Double,
  isHomed: Boolean,
  isLimitActive: Boolean,
  accentColor: Color,
  onZeroAxis: () -> Unit,
  onHomeAxis: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.testTag("dro_card_$axisName"),
    colors = CardDefaults.cardColors(
      containerColor = DroBackground
    ),
    shape = RoundedCornerShape(10.dp),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.horizontalGradient(listOf(accentColor.copy(alpha = 0.6f), Color.DarkGray))
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Axis Label & Indicators
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(accentColor.copy(alpha = 0.2f))
            .border(1.dp, accentColor, RoundedCornerShape(6.dp)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = axisName,
            style = MaterialTheme.typography.titleLarge,
            color = accentColor,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isHomed) DroGreen else Color.Gray)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isHomed) "HOMED" else "UNHOMED",
              style = MaterialTheme.typography.labelSmall,
              color = if (isHomed) DroGreen else Color.Gray,
              fontSize = 9.sp
            )
          }
          Text(
            text = "MCS: ${String.format(Locale.US, "%.3f", machinePosValue)}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray.copy(alpha = 0.7f),
            fontSize = 10.sp
          )
        }
      }

      // Digital Position Readout (High contrast large display)
      Text(
        text = String.format(Locale.US, "%+08.3f", posValue),
        style = MaterialTheme.typography.displayMedium,
        color = DroWhite,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        fontSize = 24.sp
      )

      // Quick Zero & Home buttons
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
          onClick = onZeroAxis,
          modifier = Modifier
            .height(34.dp)
            .testTag("zero_button_$axisName"),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
          shape = RoundedCornerShape(6.dp),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = accentColor
          )
        ) {
          Text(
            text = "ZERO $axisName",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
          )
        }

        FilledTonalIconButton(
          onClick = onHomeAxis,
          modifier = Modifier
            .size(34.dp)
            .testTag("home_button_$axisName"),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Home,
            contentDescription = "Home $axisName",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
fun IndustrialJogPad(
  jogConfig: JogConfig,
  onJogAxis: (axis: String, positive: Boolean) -> Unit,
  onStopJog: (axis: String) -> Unit,
  onSetIncrement: (Double) -> Unit,
  onToggleContinuous: (Boolean) -> Unit,
  onSetSpeed: (Double) -> Unit,
  isLocked: Boolean,
  modifier: Modifier = Modifier
) {
  OnScreenJogControls(
    jogConfig = jogConfig,
    onJogAxis = onJogAxis,
    onStopJog = onStopJog,
    onSetIncrement = onSetIncrement,
    onToggleContinuous = onToggleContinuous,
    onSetSpeed = onSetSpeed,
    isLocked = isLocked,
    modifier = modifier
  )
}

@Composable
fun JogButton(
  label: String,
  icon: ImageVector,
  accentColor: Color,
  enabled: Boolean,
  onClick: () -> Unit,
  onRelease: () -> Unit,
  modifier: Modifier = Modifier
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier
      .size(width = 54.dp, height = 48.dp)
      .testTag("jog_btn_$label"),
    shape = RoundedCornerShape(8.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant,
      contentColor = accentColor
    ),
    contentPadding = PaddingValues(0.dp)
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(18.dp))
      Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontSize = 9.sp)
    }
  }
}

@Composable
fun ProminentEStopBar(
  isEStopActive: Boolean,
  isDriverEnabled: Boolean,
  onEStopClick: () -> Unit,
  onDriverToggle: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Big Emergency Stop Button
    Button(
      onClick = onEStopClick,
      modifier = Modifier
        .weight(1f)
        .height(56.dp)
        .testTag("emergency_stop_button"),
      shape = RoundedCornerShape(10.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = if (isEStopActive) IndDarkSecondary else IndDarkEmergency,
        contentColor = Color.White
      ),
      elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = if (isEStopActive) Icons.Default.LockReset else Icons.Default.Dangerous,
          contentDescription = "E-Stop",
          modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (isEStopActive) "RESET E-STOP" else "EMERGENCY STOP",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Black,
          letterSpacing = 0.5.sp
        )
      }
    }

    // Driver Enable / Disable Toggle Card
    Surface(
      onClick = onDriverToggle,
      modifier = Modifier
        .height(56.dp)
        .testTag("driver_enable_toggle"),
      shape = RoundedCornerShape(10.dp),
      color = if (isDriverEnabled) MaterialTheme.colorScheme.surfaceVariant else IndDarkEmergency.copy(alpha = 0.2f),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(if (isDriverEnabled) DroGreen else DroRed)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "STEP/DIR DRIVERS",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp
          )
          Text(
            text = if (isDriverEnabled) "ENABLED" else "DISABLED",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isDriverEnabled) DroGreen else DroRed
          )
        }
      }
    }
  }
}

@Composable
fun OverrideSliderRow(
  label: String,
  currentPct: Int,
  minPct: Int,
  maxPct: Int,
  accentColor: Color,
  onValueChange: (Int) -> Unit,
  onReset100: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.width(70.dp)
    )

    Slider(
      value = currentPct.toFloat(),
      onValueChange = { onValueChange(it.toInt()) },
      valueRange = minPct.toFloat()..maxPct.toFloat(),
      modifier = Modifier
        .weight(1f)
        .testTag("override_slider_$label"),
      colors = SliderDefaults.colors(
        thumbColor = accentColor,
        activeTrackColor = accentColor
      )
    )

    Spacer(modifier = Modifier.width(8.dp))

    Box(
      modifier = Modifier
        .width(52.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .padding(vertical = 4.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "$currentPct%",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = accentColor
      )
    }

    Spacer(modifier = Modifier.width(6.dp))

    IconButton(
      onClick = onReset100,
      modifier = Modifier
        .size(32.dp)
        .testTag("reset_100_$label")
    ) {
      Icon(
        imageVector = Icons.Default.RestartAlt,
        contentDescription = "Reset to 100%",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp)
      )
    }
  }
}
