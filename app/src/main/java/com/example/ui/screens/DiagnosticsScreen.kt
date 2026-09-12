package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.model.HalPin
import com.example.model.McuDiagnostics
import com.example.ui.theme.*
import com.example.viewmodel.CncViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

@Composable
fun DiagnosticsScreen(
  viewModel: CncViewModel,
  modifier: Modifier = Modifier
) {
  val machineState by viewModel.machineState.collectAsStateWithLifecycle()
  val mcuDiag by viewModel.mcuDiagnostics.collectAsStateWithLifecycle()
  val halPins by viewModel.halPins.collectAsStateWithLifecycle()

  var selectedFilter by remember { mutableStateOf("ALL") }
  var searchQuery by remember { mutableStateOf("") }

  val filteredPins = remember(halPins, selectedFilter, searchQuery) {
    halPins.filter { pin ->
      val matchesFilter = when (selectedFilter) {
        "ALL" -> true
        "STM32" -> pin.component.contains("stm32", ignoreCase = true)
        "MOTION" -> pin.component.contains("motion", ignoreCase = true)
        "IOCONTROL" -> pin.component.contains("iocontrol", ignoreCase = true)
        "GPIO" -> pin.component.contains("gpio", ignoreCase = true)
        else -> true
      }
      val matchesSearch = searchQuery.isBlank() || pin.name.contains(searchQuery, ignoreCase = true)
      matchesFilter && matchesSearch
    }
  }

  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // STM32 & QRB2210 Real-time Telemetry Card
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
          Column {
            Text(
              text = "1 Q HARDWARE & RT DIAGNOSTICS",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Qualcomm QRB2210 Linux + STM32U585 Motion Timing",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 9.sp
            )
          }
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(MaterialTheme.colorScheme.primaryContainer)
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = mcuDiag.bridgeRpcState,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              fontFamily = FontFamily.Monospace,
              fontSize = 9.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4 Key Telemetry Metric Tiles
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          DiagMetricTile(
            label = "RT LATENCY",
            value = "${mcuDiag.worstLatencyUs} µs",
            subtext = "Jitter: ${mcuDiag.avgJitterUs} µs",
            accentColor = DroGreen,
            modifier = Modifier.weight(1f)
          )
          DiagMetricTile(
            label = "STEP GEN",
            value = "${mcuDiag.stepFrequencyCurrentKhz.toInt()} kHz",
            subtext = "Max: ${mcuDiag.stepFrequencyMaxKhz.toInt()} kHz",
            accentColor = DroCyan,
            modifier = Modifier.weight(1f)
          )
          DiagMetricTile(
            label = "WATCHDOG",
            value = if (mcuDiag.watchdogHealthy) "HEALTHY" else "FAULT",
            subtext = "Pulse: ${mcuDiag.watchdogHeartbeatMs}ms",
            accentColor = if (mcuDiag.watchdogHealthy) DroGreen else DroRed,
            modifier = Modifier.weight(1f)
          )
          DiagMetricTile(
            label = "LINUX CPU",
            value = "${mcuDiag.linuxCpuLoadPct.toInt()}%",
            subtext = "RAM: ${mcuDiag.linuxRamUsedMb}MB",
            accentColor = IndDarkWarning,
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Additional Hardware Info Row
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("STM32 Temp: ${mcuDiag.mcuTemperatureC}°C", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
          Text("Linux Temp: ${mcuDiag.linuxTempC}°C", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
          Text("Comms Rx/Tx: ${mcuDiag.packetsRx} pkts (0.0% loss)", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
        }
      }
    }

    // LinuxCNC HAL Pin Inspector
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .height(340.dp),
      colors = CardDefaults.cardColors(containerColor = DroBackground),
      shape = RoundedCornerShape(10.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Header & Search
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "LINUXCNC HAL PIN & SIGNAL INSPECTOR",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "${filteredPins.size} PINS",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = FontFamily.Monospace,
              color = DroCyan
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Filter chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            listOf("ALL", "STM32", "MOTION", "IOCONTROL", "GPIO").forEach { f ->
              FilterChip(
                selected = selectedFilter == f,
                onClick = { selectedFilter = f },
                label = { Text(f, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(28.dp)
              )
            }
          }
        }

        // Live Table List of HAL pins
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          items(filteredPins) { pin ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF111827))
                .padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              // Pin Name & Component
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = pin.name,
                  style = MaterialTheme.typography.labelSmall,
                  fontFamily = FontFamily.Monospace,
                  color = DroWhite,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "${pin.component} • ${pin.type} • ${pin.direction}",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 9.sp,
                  color = Color.Gray
                )
              }

              // Value Badge
              val isBoolTrue = pin.value.equals("TRUE", true)
              val isBoolFalse = pin.value.equals("FALSE", true)
              val valueColor = when {
                isBoolTrue -> DroGreen
                isBoolFalse -> Color.Gray
                else -> DroCyan
              }

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(valueColor.copy(alpha = 0.2f))
                  .border(1.dp, valueColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
              ) {
                Text(
                  text = pin.value,
                  style = MaterialTheme.typography.labelSmall,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = valueColor
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
fun DiagMetricTile(
  label: String,
  value: String,
  subtext: String,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(DroBackground)
      .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
      .padding(8.dp)
  ) {
    Column {
      Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.Gray)
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        color = accentColor,
        fontSize = 14.sp
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = subtext, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = Color.LightGray)
    }
  }
}
