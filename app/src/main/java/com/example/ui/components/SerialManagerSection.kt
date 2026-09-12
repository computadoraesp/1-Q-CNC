package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.ui.theme.*
import com.example.viewmodel.CncViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SerialManagerSection(
  viewModel: CncViewModel,
  modifier: Modifier = Modifier
) {
  val availableDevices by viewModel.availableUsbDevices.collectAsStateWithLifecycle()
  val connectedDevice by viewModel.connectedUsbDevice.collectAsStateWithLifecycle()
  val linkStats by viewModel.serialLinkStats.collectAsStateWithLifecycle()
  val portConfig by viewModel.serialPortConfig.collectAsStateWithLifecycle()
  val serialLogs by viewModel.serialLogs.collectAsStateWithLifecycle()
  val commandInput by viewModel.serialCommandInput.collectAsStateWithLifecycle()
  val machineState by viewModel.machineState.collectAsStateWithLifecycle()

  var selectedDeviceIndex by remember { mutableStateOf(0) }
  var deviceDropdownExpanded by remember { mutableStateOf(false) }
  var baudDropdownExpanded by remember { mutableStateOf(false) }
  var showHexDump by remember { mutableStateOf(false) }
  var showAdvancedConfig by remember { mutableStateOf(false) }

  val baudRateOptions = listOf(9600, 19200, 38400, 57600, 115200, 230400, 250000, 460800, 500000, 921600, 1000000, 2000000)
  val isConnected = machineState.connectionStatus == ConnectionStatus.CONNECTED_USB

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // 1. Hardware Link Setup Card
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
              text = "USB SERIAL HARDWARE LINK",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = "Direct USB CDC-ACM link to 1 Q / STM32 Motion Coprocessor",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Link Active Status Pill
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isConnected) DroGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isConnected) DroGreen else MaterialTheme.colorScheme.outlineVariant)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (isConnected) DroGreen else DroAmber)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isConnected) "LINK ACTIVE" else "DISCONNECTED",
                style = MaterialTheme.typography.labelSmall,
                color = if (isConnected) DroGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Device Selection Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Device Dropdown
          Box(modifier = Modifier.weight(1f)) {
            OutlinedCard(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { deviceDropdownExpanded = true }
                .testTag("usb_device_selector"),
              shape = RoundedCornerShape(8.dp),
              colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("USB Target Device", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  val selected = availableDevices.getOrNull(selectedDeviceIndex)
                  Text(
                    text = selected?.let { "${it.productName} (0x${Integer.toHexString(it.vendorId).uppercase()})" }
                      ?: if (availableDevices.isNotEmpty()) availableDevices.first().productName else "No hardware detected (Tap Scan)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                  )
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Device")
              }
            }

            DropdownMenu(
              expanded = deviceDropdownExpanded,
              onDismissRequest = { deviceDropdownExpanded = false }
            ) {
              if (availableDevices.isEmpty()) {
                DropdownMenuItem(
                  text = { Text("No USB devices found on bus") },
                  onClick = { deviceDropdownExpanded = false }
                )
              } else {
                availableDevices.forEachIndexed { index, dev ->
                  DropdownMenuItem(
                    text = {
                      Column {
                        Text(dev.productName, fontWeight = FontWeight.Bold)
                        Text(
                          "VID: 0x${Integer.toHexString(dev.vendorId).uppercase()} PID: 0x${Integer.toHexString(dev.productId).uppercase()} (${dev.driverType})",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    },
                    onClick = {
                      selectedDeviceIndex = index
                      deviceDropdownExpanded = false
                    }
                  )
                }
              }
            }
          }

          // Scan Button
          IconButton(
            onClick = { viewModel.scanUsbDevices() },
            modifier = Modifier
              .size(44.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .testTag("scan_usb_devices_button")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Scan USB Devices")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Baud Rate & Port Config Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Baud rate dropdown
          Box(modifier = Modifier.weight(1f)) {
            OutlinedCard(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { baudDropdownExpanded = true },
              shape = RoundedCornerShape(8.dp),
              colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("Baud Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text("${portConfig.baudRate} bps", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Baud")
              }
            }

            DropdownMenu(
              expanded = baudDropdownExpanded,
              onDismissRequest = { baudDropdownExpanded = false }
            ) {
              baudRateOptions.forEach { baud ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = "$baud bps ${if (baud == 115200) "(Standard CNC)" else if (baud == 250000) "(Fast STM32)" else ""}",
                      fontWeight = if (baud == portConfig.baudRate) FontWeight.Bold else FontWeight.Normal
                    )
                  },
                  onClick = {
                    viewModel.updateSerialBaudRate(baud)
                    baudDropdownExpanded = false
                  }
                )
              }
            }
          }

          // Framing Info Box (8N1)
          OutlinedCard(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
          ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
              Text("Framing / Format", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text("8 Data, 1 Stop, No Parity (8N1)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
          }
        }

        // Advanced Port Settings Toggle
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showAdvancedConfig = !showAdvancedConfig }
            .padding(vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = if (showAdvancedConfig) "Hide Advanced Hardware Handshaking" else "Show Advanced Hardware Handshaking (DTR / RTS / Parity)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
          )
          Icon(
            imageVector = if (showAdvancedConfig) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
        }

        AnimatedVisibility(visible = showAdvancedConfig) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
              .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                  checked = portConfig.dtrEnable,
                  onCheckedChange = { viewModel.toggleDtr(it) }
                )
                Text("Assert DTR (Data Terminal Ready)", style = MaterialTheme.typography.bodySmall)
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                  checked = portConfig.rtsEnable,
                  onCheckedChange = { viewModel.toggleRts(it) }
                )
                Text("Assert RTS (Ready To Send)", style = MaterialTheme.typography.bodySmall)
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedButton(
                onClick = {
                  // Pulse DTR low then high to reboot Arduino / STM32 MCU bootloader
                  viewModel.toggleDtr(false)
                  viewModel.toggleRts(false)
                  android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    viewModel.toggleDtr(true)
                    viewModel.toggleRts(true)
                  }, 150)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pulse Hardware Reset", style = MaterialTheme.typography.labelSmall)
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Connection Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              val target = availableDevices.getOrNull(selectedDeviceIndex)
              viewModel.connectUsbSerial(target, portConfig.baudRate)
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .weight(1f)
              .height(46.dp)
              .testTag("connect_usb_serial_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isConnected) DroGreen else MaterialTheme.colorScheme.primary
            )
          ) {
            Icon(Icons.Default.Usb, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isConnected) "CONNECTED (RE-SYNC)" else "CONNECT USB SERIAL")
          }

          OutlinedButton(
            onClick = { viewModel.disconnectUsbSerial() },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .height(46.dp)
              .testTag("disconnect_usb_serial_button"),
            enabled = isConnected
          ) {
            Text("DISCONNECT")
          }
        }
      }
    }

    // 2. Serial Link Telemetry & Statistics Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(10.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = "DATA LINK TELEMETRY & PACKET METRICS",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          MetricTile("Bytes Transmitted (TX)", "${linkStats.bytesSent} B", Modifier.weight(1f))
          MetricTile("Bytes Received (RX)", "${linkStats.bytesReceived} B", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          MetricTile("Packets TX / RX", "${linkStats.packetsSent} / ${linkStats.packetsReceived}", Modifier.weight(1f))
          MetricTile("Watchdog Status", if (linkStats.isLinkActive) "HEALTHY (12ms)" else "INACTIVE", Modifier.weight(1f), isGreen = linkStats.isLinkActive)
        }
      }
    }

    // 3. Quick Realtime Controller Commands Bar
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(10.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = "REAL-TIME SERIAL PROTOCOL ACTIONS",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "Single-byte real-time bypass commands sent directly to hardware",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          ActionChipButton("Status '?'", isDanger = false) { viewModel.sendRealtimeSerialChar('?') }
          ActionChipButton("Unlock '\$X'", isDanger = false) { viewModel.sendSerialCommand("\$X") }
          ActionChipButton("Cycle '~'", isDanger = false) { viewModel.sendRealtimeSerialChar('~') }
          ActionChipButton("Hold '!'", isDanger = true) { viewModel.sendRealtimeSerialChar('!') }
          ActionChipButton("Soft Reset", isDanger = true) { viewModel.sendRealtimeSerialChar('\u0018') }
        }
      }
    }

    // 4. Interactive Serial Monitor & Live Traffic Console
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
            Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "SERIAL TRAFFIC MONITOR",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
              onClick = { showHexDump = !showHexDump },
              contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
              Text(if (showHexDump) "ASCII MODE" else "HEX DUMP", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(
              onClick = { viewModel.clearSerialLogs() },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Logs", modifier = Modifier.size(18.dp))
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Monospace Console Box
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(DroBackground)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
            .padding(8.dp)
        ) {
          val listState = rememberLazyListState()
          
          if (serialLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text(
                text = "Serial traffic console idle. Transmitted and received frames will appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = IndDarkTextMuted,
                fontFamily = FontFamily.Monospace
              )
            }
          } else {
            LazyColumn(
              state = listState,
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              items(serialLogs) { log ->
                val (color, prefix) = when (log.direction) {
                  SerialDirection.TX -> Pair(DroAmber, "[TX] >>")
                  SerialDirection.RX -> Pair(DroGreen, "[RX] <<")
                  SerialDirection.INFO -> Pair(DroCyan, "[SYS] -")
                  SerialDirection.ERROR -> Pair(DroRed, "[ERR] !")
                }

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Text(
                    text = log.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = IndDarkTextMuted,
                    fontFamily = FontFamily.Monospace
                  )
                  Text(
                    text = prefix,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontFamily = FontFamily.Monospace
                  )
                  Text(
                    text = if (showHexDump && log.hexRepresentation.isNotBlank()) "${log.text} (${log.hexRepresentation})" else log.text,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = if (log.direction == SerialDirection.TX) DroWhite else color,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Raw Command Input & Send
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = commandInput,
            onValueChange = { viewModel.updateSerialCommandInput(it) },
            modifier = Modifier
              .weight(1f)
              .testTag("serial_command_input"),
            placeholder = { Text("Send raw command (e.g., G0 X10 Y10, $$, M3 S12000)", fontSize = 11.sp) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            shape = RoundedCornerShape(8.dp)
          )

          Button(
            onClick = { viewModel.sendSerialCommand() },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .height(52.dp)
              .testTag("send_serial_command_button")
          ) {
            Icon(Icons.Default.Send, contentDescription = "Send")
          }
        }
      }
    }
  }
}

@Composable
private fun MetricTile(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  isGreen: Boolean = false
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(6.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = if (isGreen) DroGreen else MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
private fun RowScope.ActionChipButton(
  label: String,
  isDanger: Boolean,
  onClick: () -> Unit
) {
  Button(
    onClick = onClick,
    modifier = Modifier
      .weight(1f)
      .height(36.dp),
    shape = RoundedCornerShape(6.dp),
    contentPadding = PaddingValues(horizontal = 2.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = if (isDanger) DroRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
      contentColor = if (isDanger) DroRed else MaterialTheme.colorScheme.onSurface
    ),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isDanger) DroRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
    )
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace
    )
  }
}
