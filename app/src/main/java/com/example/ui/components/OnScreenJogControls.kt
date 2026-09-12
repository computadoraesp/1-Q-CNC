package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.JogConfig
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

/**
 * On-Screen Jog Controls (X+, X-, Y+, Y-, Z+, Z-, A+, A-)
 * Transmits real-time movement commands ($J=G91 G21... / LinuxCNC jog)
 * through the active Serial Communication Manager.
 */
@Composable
fun OnScreenJogControls(
  jogConfig: JogConfig,
  onJogAxis: (axis: String, positive: Boolean) -> Unit,
  onStopJog: (axis: String) -> Unit,
  onSetIncrement: (Double) -> Unit,
  onToggleContinuous: (Boolean) -> Unit,
  onSetSpeed: (Double) -> Unit,
  isLocked: Boolean,
  modifier: Modifier = Modifier,
  onSafeZRetract: (() -> Unit)? = null,
  activeSerialCommand: String? = null
) {
  var lastSentCommand by remember { mutableStateOf<String?>(null) }
  var isActivelyTransmitting by remember { mutableStateOf(false) }

  fun triggerJog(axis: String, positive: Boolean) {
    if (isLocked) return
    val step = if (jogConfig.isIncremental) {
      if (positive) jogConfig.stepIncrementMm else -jogConfig.stepIncrementMm
    } else {
      if (positive) 5.0 else -5.0
    }
    val cmd = String.format(Locale.US, "\$J=G91 G21 %s%+.3f F%.0f", axis.uppercase(), step, jogConfig.jogSpeedMmMin)
    lastSentCommand = cmd
    isActivelyTransmitting = true
    onJogAxis(axis, positive)
  }

  fun triggerStop(axis: String) {
    if (isLocked) return
    lastSentCommand = "JOG CANCEL (0x85)"
    isActivelyTransmitting = false
    onStopJog(axis)
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("on_screen_jog_controls"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Header: Title, Lock Status, and Mode Toggle (Step vs Continuous)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
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
              imageVector = Icons.Default.Gamepad,
              contentDescription = "Jog Controller",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(18.dp)
            )
          }

          Column {
            Text(
              text = "MANUAL JOG CONTROLS",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.onSurface,
              letterSpacing = 0.5.sp
            )
            Text(
              text = if (isLocked) "MOTORS DISABLED / E-STOP ENGAGED" else "DIRECT SERIAL USB CDC-ACM LINK",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (isLocked) IndDarkEmergency else DroGreen
            )
          }
        }

        // STEP vs CONT Mode Toggle
        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp)
        ) {
          val isInc = jogConfig.isIncremental
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(if (isInc) MaterialTheme.colorScheme.primary else Color.Transparent)
              .clickable(enabled = !isLocked) { onToggleContinuous(false) }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("jog_mode_step")
          ) {
            Text(
              text = "STEP",
              style = MaterialTheme.typography.labelSmall,
              color = if (isInc) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Black,
              fontSize = 11.sp
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(if (!isInc) MaterialTheme.colorScheme.secondary else Color.Transparent)
              .clickable(enabled = !isLocked) { onToggleContinuous(true) }
              .padding(horizontal = 10.dp, vertical = 6.dp)
              .testTag("jog_mode_cont")
          ) {
            Text(
              text = "CONTINUOUS",
              style = MaterialTheme.typography.labelSmall,
              color = if (!isInc) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Black,
              fontSize = 11.sp
            )
          }
        }
      }

      // Step Increments Selection Pills (Visible when in STEP mode)
      AnimatedVisibility(visible = jogConfig.isIncremental) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "STEP INCREMENT (DISTANCE):",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )

          ScrollableCarouselWithArrows(
            modifier = Modifier.fillMaxWidth(),
            scrollStepDp = 120.dp,
            arrowSize = 28.dp,
            iconSize = 16.dp,
            spacing = 4.dp,
            testTagPrefix = "jog_inc"
          ) {
            val increments = listOf(0.001, 0.01, 0.1, 1.0, 10.0, 50.0)
            increments.forEach { inc ->
              val isSelected = jogConfig.stepIncrementMm == inc
              FilterChip(
                selected = isSelected,
                onClick = { onSetIncrement(inc) },
                enabled = !isLocked,
                label = {
                  Text(
                    text = when (inc) {
                      0.001 -> "0.001mm"
                      0.01 -> "0.01mm"
                      0.1 -> "0.1mm"
                      1.0 -> "1.0mm"
                      10.0 -> "10mm"
                      50.0 -> "50mm"
                      else -> "${inc}mm"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal
                  )
                },
                shape = RoundedCornerShape(6.dp),
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = Color.Black
                ),
                modifier = Modifier.testTag("inc_chip_$inc")
              )
            }
          }
        }
      }

      // Jog Feed Rate Preset Selector
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "JOG FEED VELOCITY:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${jogConfig.jogSpeedMmMin.toInt()} mm/min",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = DroCyan,
            fontWeight = FontWeight.Bold
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          val speeds = listOf(
            Pair("FINE (100)", 100.0),
            Pair("NORM (500)", 500.0),
            Pair("FAST (1200)", 1200.0),
            Pair("RAPID (3000)", 3000.0)
          )
          speeds.forEach { (label, speedVal) ->
            val isSelected = jogConfig.jogSpeedMmMin == speedVal
            OutlinedButton(
              onClick = { onSetSpeed(speedVal) },
              enabled = !isLocked,
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
              ),
              border = BorderStroke(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
              ),
              modifier = Modifier
                .weight(1f)
                .height(30.dp)
                .testTag("jog_speed_${speedVal.toInt()}")
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              )
            }
          }
        }
      }

      // PRIMARY ON-SCREEN JOG MATRIX:
      // Adaptive layout:
      // - Compact (< 580dp): XY Cross-Pad on top with full width (X-, STOP, X+ perfectly visible),
      //   followed by Z-Axis and A-Axis columns side-by-side.
      // - Wide (>= 580dp): 3 columns side-by-side (XY Cross-Pad | Z-Axis | A-Axis) with fluid weights.
      BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isWideLayout = maxWidth >= 580.dp

        if (isWideLayout) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(DroBackground, RoundedCornerShape(10.dp))
              .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            XyPadMatrix(
              jogConfig = jogConfig,
              isLocked = isLocked,
              onTriggerJog = { a, p -> triggerJog(a, p) },
              onTriggerStop = { a -> triggerStop(a) },
              isCompact = false,
              modifier = Modifier.weight(1.4f)
            )

            ZAxisElevationColumn(
              jogConfig = jogConfig,
              isLocked = isLocked,
              onSafeZRetract = onSafeZRetract,
              onTriggerJog = { a, p -> triggerJog(a, p) },
              onTriggerStop = { a -> triggerStop(a) },
              modifier = Modifier.weight(0.9f)
            )

            AAxisRotaryColumn(
              jogConfig = jogConfig,
              isLocked = isLocked,
              onTriggerJog = { a, p -> triggerJog(a, p) },
              onTriggerStop = { a -> triggerStop(a) },
              modifier = Modifier.weight(0.9f)
            )
          }
        } else {
          // Compact Mobile Portrait Layout:
          // XY Pad takes full width so X- and X+ are broad, clear, and never clipped!
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .background(DroBackground, RoundedCornerShape(10.dp))
              .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            XyPadMatrix(
              jogConfig = jogConfig,
              isLocked = isLocked,
              onTriggerJog = { a, p -> triggerJog(a, p) },
              onTriggerStop = { a -> triggerStop(a) },
              isCompact = true,
              modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(
              modifier = Modifier.padding(vertical = 2.dp),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              ZAxisElevationColumn(
                jogConfig = jogConfig,
                isLocked = isLocked,
                onSafeZRetract = onSafeZRetract,
                onTriggerJog = { a, p -> triggerJog(a, p) },
                onTriggerStop = { a -> triggerStop(a) },
                modifier = Modifier.weight(1f)
              )

              AAxisRotaryColumn(
                jogConfig = jogConfig,
                isLocked = isLocked,
                onTriggerJog = { a, p -> triggerJog(a, p) },
                onTriggerStop = { a -> triggerStop(a) },
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }

      // Live Serial Command Output & TX Indicator Bar
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
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
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isActivelyTransmitting) DroGreen else MaterialTheme.colorScheme.outline)
            )
            Text(
              text = "SERIAL TX:",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = lastSentCommand ?: activeSerialCommand ?: "IDLE - Ready for jog input",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = if (lastSentCommand != null) DroAmber else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Text(
            text = if (jogConfig.isIncremental) "MODE: STEP (${jogConfig.stepIncrementMm}mm)" else "MODE: CONTINUOUS",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun XyPadMatrix(
  jogConfig: JogConfig,
  isLocked: Boolean,
  onTriggerJog: (String, Boolean) -> Unit,
  onTriggerStop: (String) -> Unit,
  isCompact: Boolean,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Icon(
        imageVector = Icons.Default.ControlCamera,
        contentDescription = null,
        tint = DroCyan,
        modifier = Modifier.size(16.dp)
      )
      Text(
        text = "PLANO X-Y (DESPLAZAMIENTO)",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        fontSize = 11.sp,
        color = DroCyan
      )
    }

    // Y+ Button (North / Table Back)
    DiscreteOrContinuousJogButton(
      axisName = "Y",
      positive = true,
      label = "Y+",
      subLabel = "ATRÁS",
      icon = Icons.Default.ArrowUpward,
      accentColor = DroCyan,
      isContinuous = !jogConfig.isIncremental,
      enabled = !isLocked,
      onStart = { onTriggerJog("Y", true) },
      onEnd = { onTriggerStop("Y") },
      modifier = Modifier
        .width(if (isCompact) 140.dp else 100.dp)
        .height(52.dp)
    )

    // Middle Row: X- (West / Izquierda) | STOP | X+ (East / Derecha)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .then(if (isCompact) Modifier.padding(horizontal = 4.dp) else Modifier),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // X- Button (West)
      DiscreteOrContinuousJogButton(
        axisName = "X",
        positive = false,
        label = "X-",
        subLabel = "IZQUIERDA",
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        accentColor = DroAmber,
        isContinuous = !jogConfig.isIncremental,
        enabled = !isLocked,
        onStart = { onTriggerJog("X", false) },
        onEnd = { onTriggerStop("X") },
        modifier = Modifier
          .weight(1f)
          .height(52.dp)
      )

      // Center Origin Indicator / Stop Target
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant)
          .border(1.5.dp, IndDarkEmergency.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
          .clickable(enabled = !isLocked) {
            onTriggerStop("ALL")
          },
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.GpsFixed,
            contentDescription = "XY Center Stop",
            tint = IndDarkEmergency,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = "STOP",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = IndDarkEmergency
          )
        }
      }

      // X+ Button (East) - Prominent, High Contrast, Always Visible!
      DiscreteOrContinuousJogButton(
        axisName = "X",
        positive = true,
        label = "X+",
        subLabel = "DERECHA",
        icon = Icons.AutoMirrored.Filled.ArrowForward,
        accentColor = DroAmber,
        isContinuous = !jogConfig.isIncremental,
        enabled = !isLocked,
        onStart = { onTriggerJog("X", true) },
        onEnd = { onTriggerStop("X") },
        modifier = Modifier
          .weight(1f)
          .height(52.dp)
      )
    }

    // Y- Button (South / Table Forward)
    DiscreteOrContinuousJogButton(
      axisName = "Y",
      positive = false,
      label = "Y-",
      subLabel = "ADELANTE",
      icon = Icons.Default.ArrowDownward,
      accentColor = DroCyan,
      isContinuous = !jogConfig.isIncremental,
      enabled = !isLocked,
      onStart = { onTriggerJog("Y", false) },
      onEnd = { onTriggerStop("Y") },
      modifier = Modifier
        .width(if (isCompact) 140.dp else 100.dp)
        .height(52.dp)
    )
  }
}

@Composable
private fun ZAxisElevationColumn(
  jogConfig: JogConfig,
  isLocked: Boolean,
  onSafeZRetract: (() -> Unit)?,
  onTriggerJog: (String, Boolean) -> Unit,
  onTriggerStop: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(6.dp),
    modifier = modifier
  ) {
    Text(
      text = "EJE Z (ALTURA)",
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Black,
      fontSize = 11.sp,
      color = DroGreen
    )

    // Z+ Button (UP / Subir herramienta)
    DiscreteOrContinuousJogButton(
      axisName = "Z",
      positive = true,
      label = "Z+",
      subLabel = "SUBIR (+Z)",
      icon = Icons.Default.KeyboardDoubleArrowUp,
      accentColor = DroGreen,
      isContinuous = !jogConfig.isIncremental,
      enabled = !isLocked,
      onStart = { onTriggerJog("Z", true) },
      onEnd = { onTriggerStop("Z") },
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
    )

    // Safe Retract Shortcut
    OutlinedButton(
      onClick = {
        if (onSafeZRetract != null) {
          onSafeZRetract()
        } else {
          onTriggerJog("Z", true)
        }
      },
      enabled = !isLocked,
      shape = RoundedCornerShape(8.dp),
      contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(36.dp)
        .testTag("jog_safe_z_retract")
    ) {
      Text(
        text = "RETRACT SEGURA",
        style = MaterialTheme.typography.labelSmall,
        fontSize = 8.sp,
        fontWeight = FontWeight.Black,
        color = DroGreen
      )
    }

    // Z- Button (DOWN / Bajar herramienta)
    DiscreteOrContinuousJogButton(
      axisName = "Z",
      positive = false,
      label = "Z-",
      subLabel = "BAJAR (-Z)",
      icon = Icons.Default.KeyboardDoubleArrowDown,
      accentColor = DroGreen,
      isContinuous = !jogConfig.isIncremental,
      enabled = !isLocked,
      onStart = { onTriggerJog("Z", false) },
      onEnd = { onTriggerStop("Z") },
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
    )
  }
}

@Composable
private fun AAxisRotaryColumn(
  jogConfig: JogConfig,
  isLocked: Boolean,
  onTriggerJog: (String, Boolean) -> Unit,
  onTriggerStop: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(6.dp),
    modifier = modifier
  ) {
    Text(
      text = "EJE A (4º ROTATIVO)",
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Black,
      fontSize = 11.sp,
      color = IndDarkWarning
    )

    // A+ Button (CW / Horario)
    DiscreteOrContinuousJogButton(
      axisName = "A",
      positive = true,
      label = "A+",
      subLabel = "ROT CW",
      icon = Icons.AutoMirrored.Filled.RotateRight,
      accentColor = IndDarkWarning,
      isContinuous = !jogConfig.isIncremental,
      enabled = !isLocked,
      onStart = { onTriggerJog("A", true) },
      onEnd = { onTriggerStop("A") },
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
    )

    // Stop Jog real-time cancel
    FilledTonalButton(
      onClick = { onTriggerStop("A") },
      enabled = !isLocked,
      shape = RoundedCornerShape(8.dp),
      contentPadding = PaddingValues(0.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(36.dp)
        .testTag("jog_cancel_btn")
    ) {
      Text("PARAR JOG", style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }

    // A- Button (CCW / Antihorario)
    DiscreteOrContinuousJogButton(
      axisName = "A",
      positive = false,
      label = "A-",
      subLabel = "ROT CCW",
      icon = Icons.AutoMirrored.Filled.RotateLeft,
      accentColor = IndDarkWarning,
      isContinuous = !jogConfig.isIncremental,
      enabled = !isLocked,
      onStart = { onTriggerJog("A", false) },
      onEnd = { onTriggerStop("A") },
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
    )
  }
}

/**
 * Interactive Jog Button supporting both single step tap and continuous press-and-hold
 */
@Composable
fun DiscreteOrContinuousJogButton(
  axisName: String,
  positive: Boolean,
  label: String,
  subLabel: String,
  icon: ImageVector,
  accentColor: Color,
  isContinuous: Boolean,
  enabled: Boolean,
  onStart: () -> Unit,
  onEnd: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isPressed by remember { mutableStateOf(false) }

  // Continuous jog repeater coroutine
  LaunchedEffect(isPressed, isContinuous, enabled) {
    if (isPressed && isContinuous && enabled) {
      while (isActive) {
        onStart()
        delay(120) // Repeat pulse every 120ms
      }
    }
  }

  val backgroundColor by animateColorAsState(
    targetValue = if (!enabled) {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else if (isPressed) {
      accentColor.copy(alpha = 0.35f)
    } else {
      MaterialTheme.colorScheme.surfaceVariant
    },
    animationSpec = tween(100),
    label = "jog_btn_bg"
  )

  Surface(
    modifier = modifier
      .testTag("jog_btn_${axisName}_${if (positive) "pos" else "neg"}")
      .pointerInput(enabled, isContinuous) {
        detectTapGestures(
          onPress = {
            if (!enabled) return@detectTapGestures
            isPressed = true
            if (!isContinuous) {
              onStart()
            }
            tryAwaitRelease()
            isPressed = false
            onEnd()
          }
        )
      },
    shape = RoundedCornerShape(8.dp),
    color = backgroundColor,
    border = BorderStroke(
      1.5.dp,
      if (isPressed) accentColor else MaterialTheme.colorScheme.outlineVariant
    )
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 6.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = "$label Jog",
        tint = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.size(20.dp)
      )

      Spacer(modifier = Modifier.width(4.dp))

      Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Black,
          fontSize = 13.sp,
          color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Text(
          text = subLabel,
          style = MaterialTheme.typography.labelSmall,
          fontSize = 7.sp,
          color = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
