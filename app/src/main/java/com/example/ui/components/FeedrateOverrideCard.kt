package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MachineState
import com.example.ui.theme.*
import java.util.Locale

/**
 * Dedicated Real-Time Feedrate & Speed Override Slider UI Component
 * Sends speed override commands (M220 S{pct} / Grbl 0x90-0x94)
 * through the Serial Communication Manager.
 */
@Composable
fun FeedrateOverrideCard(
  machineState: MachineState,
  onSetFeedOverride: (Int) -> Unit,
  onSetSpindleOverride: (Int) -> Unit,
  onSetRapidOverride: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("feedrate_speed_override_card"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Header: Title and Live Effective Feed Speed Indicator
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
              imageVector = Icons.Default.Speed,
              contentDescription = "Feedrate Speed",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(18.dp)
            )
          }

          Column {
            Text(
              text = "REAL-TIME SPEED OVERRIDES",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.onSurface,
              letterSpacing = 0.5.sp
            )
            Text(
              text = "DIRECT SERIAL M220 / M221 COMMAND DISPATCH",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = DroCyan
            )
          }
        }

        // Live Calculated Effective Speed Badge
        val effectiveFeed = machineState.feedRateCurrent * (machineState.feedOverridePct / 100.0)
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = DroBackground,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "ACTUAL:",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 9.sp,
              color = IndDarkTextMuted,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = String.format(Locale.US, "%.0f mm/min", effectiveFeed),
              style = MaterialTheme.typography.labelSmall,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Black,
              color = DroWhite
            )
          }
        }
      }

      // 1. REAL-TIME FEEDRATE OVERRIDE SLIDER (0% to 200%)
      EnhancedOverrideSlider(
        label = "FEEDRATE",
        subLabel = "CUTTING FEED SPEED (0% - 200%)",
        currentPct = machineState.feedOverridePct,
        minPct = 0,
        maxPct = 200,
        accentColor = DroCyan,
        icon = Icons.Default.Speed,
        onValueChange = onSetFeedOverride,
        onReset100 = { onSetFeedOverride(100) },
        quickPresets = listOf(25, 50, 100, 150, 200)
      )

      // 2. SPINDLE RPM OVERRIDE SLIDER (50% to 150%)
      EnhancedOverrideSlider(
        label = "SPINDLE",
        subLabel = "RPM SPEED OVERRIDE (50% - 150%)",
        currentPct = machineState.spindleOverridePct,
        minPct = 50,
        maxPct = 150,
        accentColor = DroGreen,
        icon = Icons.Default.ElectricBolt,
        onValueChange = onSetSpindleOverride,
        onReset100 = { onSetSpindleOverride(100) },
        quickPresets = listOf(50, 75, 100, 125, 150)
      )

      // 3. RAPID TRAVERSE OVERRIDE SLIDER (25% to 100%)
      EnhancedOverrideSlider(
        label = "RAPID (G0)",
        subLabel = "NON-CUTTING TRANSIT (25% - 100%)",
        currentPct = machineState.rapidOverridePct,
        minPct = 25,
        maxPct = 100,
        accentColor = DroAmber,
        icon = Icons.Default.FastForward,
        onValueChange = onSetRapidOverride,
        onReset100 = { onSetRapidOverride(100) },
        quickPresets = listOf(25, 50, 75, 100)
      )
    }
  }
}

@Composable
private fun EnhancedOverrideSlider(
  label: String,
  subLabel: String,
  currentPct: Int,
  minPct: Int,
  maxPct: Int,
  accentColor: Color,
  icon: ImageVector,
  onValueChange: (Int) -> Unit,
  onReset100: () -> Unit,
  quickPresets: List<Int>,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(DroBackground, RoundedCornerShape(8.dp))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    // Header line: Label + Percentage Box + 100% Reset
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = accentColor,
          modifier = Modifier.size(16.dp)
        )
        Column {
          Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = subLabel,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp,
            color = IndDarkTextMuted
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Percentage Value Tag
        Box(
          modifier = Modifier
            .width(58.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 2.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "$currentPct%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = accentColor
          )
        }

        // Reset to 100% Button
        FilledTonalButton(
          onClick = onReset100,
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
          modifier = Modifier
            .height(28.dp)
            .testTag("reset_100_$label"),
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (currentPct == 100) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (currentPct == 100) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
          )
        ) {
          Icon(Icons.Default.RestartAlt, contentDescription = "100%", modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(2.dp))
          Text("100%", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Interactive Slider Component
    Slider(
      value = currentPct.toFloat(),
      onValueChange = { onValueChange(it.toInt()) },
      valueRange = minPct.toFloat()..maxPct.toFloat(),
      modifier = Modifier
        .fillMaxWidth()
        .height(26.dp)
        .testTag("override_slider_$label"),
      colors = SliderDefaults.colors(
        thumbColor = accentColor,
        activeTrackColor = accentColor,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
      )
    )

    // Quick Preset Chips (e.g. 25%, 50%, 100%, 150%, 200%)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      quickPresets.forEach { preset ->
        val isSelected = currentPct == preset
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onValueChange(preset) }
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag("override_${label}_preset_$preset"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "$preset%",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
