package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.*
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
import com.example.viewmodel.CncViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SerialConnectionStatusCard(
  viewModel: CncViewModel,
  modifier: Modifier = Modifier,
  onNavigateToSettings: (() -> Unit)? = null
) {
  val machineState by viewModel.machineState.collectAsStateWithLifecycle()
  val transportMode by viewModel.transportMode.collectAsStateWithLifecycle()
  val connectedDevice by viewModel.connectedUsbDevice.collectAsStateWithLifecycle()
  val linkStats by viewModel.serialLinkStats.collectAsStateWithLifecycle()
  val portConfig by viewModel.serialPortConfig.collectAsStateWithLifecycle()
  val mcuDiag by viewModel.mcuDiagnostics.collectAsStateWithLifecycle()
  val connectedBtDevice by viewModel.connectedBluetoothDevice.collectAsStateWithLifecycle()
  val isEcoMode by viewModel.isEcoMode.collectAsStateWithLifecycle()

  val isUsbConnected = machineState.connectionStatus == ConnectionStatus.CONNECTED_USB
  val isBtConnected = machineState.connectionStatus == ConnectionStatus.CONNECTED_BLUETOOTH
  val isSimulated = machineState.connectionStatus == ConnectionStatus.SIMULATED
  val isLanConnected = machineState.connectionStatus == ConnectionStatus.CONNECTED_WIFI

  // Heartbeat pulse animation: Only active when connected and not in Eco Mode
  val pulseAlpha = if ((isUsbConnected || isBtConnected) && !isEcoMode) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_serial")
    val alpha by infiniteTransition.animateFloat(
      initialValue = 0.45f,
      targetValue = 1.0f,
      animationSpec = infiniteRepeatable(
        animation = tween(700, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "pulseAlpha"
    )
    alpha
  } else {
    1.0f
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("serial_connection_status_card"),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(
      1.dp,
      if (isUsbConnected || isBtConnected) DroGreen.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Top Header: Title, Icon & Status Pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(
                if (isUsbConnected || isBtConnected) DroGreen.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.primaryContainer
              )
              .border(
                1.dp,
                if (isUsbConnected || isBtConnected) DroGreen else MaterialTheme.colorScheme.primary,
                RoundedCornerShape(8.dp)
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = when (transportMode) {
                TransportMode.USB_SERIAL -> Icons.Default.Usb
                TransportMode.BLUETOOTH_SERIAL -> Icons.Default.Bluetooth
                else -> Icons.Default.Sensors
              },
              contentDescription = "Serial Status Icon",
              tint = if (isUsbConnected || isBtConnected) DroGreen else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
          }

          Column {
            Text(
              text = if (transportMode == TransportMode.BLUETOOTH_SERIAL) "BLUETOOTH WIRELESS LINK" else "SERIAL HARDWARE LINK",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Black,
              letterSpacing = 0.5.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = when (transportMode) {
                TransportMode.USB_SERIAL -> if (isUsbConnected) "Direct USB CDC-ACM link active" else "USB Serial interface ready"
                TransportMode.BLUETOOTH_SERIAL -> if (isBtConnected) "Bluetooth SPP active (${connectedBtDevice?.name ?: "Enlazado"})" else "Bluetooth Wireless interface ready"
                TransportMode.WIFI_ETHERNET -> "TCP/IP WebSocket Link (${machineState.ipAddress})"
                TransportMode.SIMULATION -> "Internal Virtual CNC Simulation"
              },
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        // Live Connection State Pill
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = when {
            isUsbConnected -> DroGreen.copy(alpha = 0.15f)
            isBtConnected -> DroCyan.copy(alpha = 0.15f)
            isLanConnected -> DroCyan.copy(alpha = 0.15f)
            isSimulated -> IndDarkPrimary.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.surfaceVariant
          },
          border = BorderStroke(
            1.dp,
            when {
              isUsbConnected -> DroGreen
              isBtConnected -> DroCyan
              isLanConnected -> DroCyan
              isSimulated -> IndDarkPrimary
              else -> MaterialTheme.colorScheme.outlineVariant
            }
          )
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                  when {
                    isUsbConnected -> DroGreen.copy(alpha = pulseAlpha)
                    isLanConnected -> DroCyan.copy(alpha = pulseAlpha)
                    isSimulated -> IndDarkPrimary
                    else -> DroAmber
                  }
                )
            )
            Text(
              text = when {
                isUsbConnected -> "CDC ONLINE"
                isLanConnected -> "LAN ONLINE"
                isSimulated -> "SIMULATED"
                else -> "OFFLINE"
              },
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              fontSize = 10.sp,
              color = when {
                isUsbConnected -> DroGreen
                isLanConnected -> DroCyan
                isSimulated -> IndDarkPrimary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
              }
            )
          }
        }
      }

      // Hardware Device & Parameter Tiles
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Target Hardware Chipset Tile
        Surface(
          modifier = Modifier.weight(1.3f),
          shape = RoundedCornerShape(8.dp),
          color = DroBackground,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "TARGET CONTROLLER",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 9.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = connectedDevice?.productName ?: if (isUsbConnected) "1 Q Motion / STM32" else "1 Q Bridge (CDC-ACM)",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = DroWhite,
              maxLines = 1,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "VID: 0x${Integer.toHexString(connectedDevice?.vendorId ?: 0x2341).uppercase()} PID: 0x${Integer.toHexString(connectedDevice?.productId ?: 0x0068).uppercase()}",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 10.sp,
              color = IndDarkTextMuted,
              fontFamily = FontFamily.Monospace
            )
          }
        }

        // Baud Rate & Line Coding Tile
        Surface(
          modifier = Modifier.weight(0.9f),
          shape = RoundedCornerShape(8.dp),
          color = DroBackground,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "PORT CONFIG",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 9.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "${portConfig.baudRate} bps",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = DroCyan,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "8N1 ${if (portConfig.dtrEnable) "DTR" else ""} ${if (portConfig.rtsEnable) "RTS" else ""}",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 10.sp,
              color = IndDarkTextMuted,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      // Telemetry Metrics Row (TX/RX, Jitter, Watchdog)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        TelemetryPill(
          label = "TX / RX",
          value = "${linkStats.packetsSent} / ${linkStats.packetsReceived}",
          modifier = Modifier.weight(1f),
          color = DroAmber
        )

        TelemetryPill(
          label = "RT JITTER",
          value = "${mcuDiag.avgJitterUs} µs",
          modifier = Modifier.weight(0.9f),
          color = DroCyan
        )

        TelemetryPill(
          label = "WATCHDOG",
          value = if (linkStats.isLinkActive) "12 ms OK" else "STABLE",
          modifier = Modifier.weight(1f),
          color = DroGreen
        )
      }

      // Quick Control Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (!isUsbConnected) {
          Button(
            onClick = {
              viewModel.connectUsbSerial(baudRate = portConfig.baudRate)
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .weight(1f)
              .height(40.dp)
              .testTag("serial_quick_connect_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = Color.Black
            )
          ) {
            Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("CONNECT USB", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          }
        } else {
          OutlinedButton(
            onClick = { viewModel.disconnectUsbSerial() },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .weight(1f)
              .height(40.dp)
              .testTag("serial_quick_disconnect_button"),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = DroRed
            ),
            border = BorderStroke(1.dp, DroRed.copy(alpha = 0.6f))
          ) {
            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("DISCONNECT", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
          }
        }

        // Send Status Query '?'
        FilledTonalButton(
          onClick = { viewModel.sendRealtimeSerialChar('?') },
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.height(40.dp),
          contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
          Text("POLL '?'", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        // Pulse Reset DTR/RTS
        FilledTonalIconButton(
          onClick = {
            viewModel.toggleDtr(false)
            viewModel.toggleRts(false)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
              viewModel.toggleDtr(true)
              viewModel.toggleRts(true)
            }, 120)
          },
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.size(40.dp)
        ) {
          Icon(Icons.Default.RestartAlt, contentDescription = "Pulse MCU Reset", modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@Composable
private fun TelemetryPill(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.onSurface
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(6.dp),
    color = DroBackground,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.Start
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 8.sp,
        color = IndDarkTextMuted,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = color,
        fontSize = 11.sp,
        maxLines = 1
      )
    }
  }
}
