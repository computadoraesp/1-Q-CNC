package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.CncViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BluetoothManagerSection(
  viewModel: CncViewModel,
  modifier: Modifier = Modifier
) {
  val machineState by viewModel.machineState.collectAsStateWithLifecycle()
  val availableDevices by viewModel.availableBluetoothDevices.collectAsStateWithLifecycle()
  val connectedDevice by viewModel.connectedBluetoothDevice.collectAsStateWithLifecycle()
  val linkStats by viewModel.bluetoothLinkStats.collectAsStateWithLifecycle()
  val logs by viewModel.bluetoothLogs.collectAsStateWithLifecycle()

  val isConnected = machineState.connectionStatus == ConnectionStatus.CONNECTED_BLUETOOTH
  val isConnecting = machineState.connectionStatus == ConnectionStatus.CONNECTING

  var commandInput by remember { mutableStateOf("") }
  var selectedBaudRate by remember { mutableIntStateOf(115200) }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // 1. Connection Card with Device Scanner
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(12.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
              imageVector = Icons.Default.Bluetooth,
              contentDescription = "Bluetooth Icon",
              tint = if (isConnected) DroCyan else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Column {
              Text(
                text = "ENLACE INALÁMBRICO BLUETOOTH (SPP)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = if (isConnected) "Conectado a ${connectedDevice?.name ?: "Dispositivo"}" else "Seleccione un módulo CNC emparejado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
              )
            }
          }

          // Scan / Refresh Button
          OutlinedButton(
            onClick = { viewModel.scanBluetoothDevices() },
            modifier = Modifier.height(36.dp),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Buscar", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Buscar", style = MaterialTheme.typography.labelSmall)
          }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // Available Paired CNC Bluetooth Devices List
        Text(
          text = "DISPOSITIVOS BLUETOOTH EMPAREJADOS (${availableDevices.size})",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        if (availableDevices.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No se encontraron dispositivos emparejados. Presione 'Buscar' o empareje su módulo HC-05 / ESP32 en Ajustes de Android.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            availableDevices.forEach { dev ->
              val isThisConnected = isConnected && connectedDevice?.address == dev.address

              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = if (isThisConnected) DroCyan.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (isThisConnected) DroCyan else MaterialTheme.colorScheme.outlineVariant)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                      modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isThisConnected) DroGreen else if (isConnecting) IndDarkWarning else Color.Gray)
                    )
                    Column {
                      Text(
                        text = dev.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Text(
                        text = "${dev.address} • ${dev.deviceType}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }

                  // Connect / Disconnect Action Button
                  Button(
                    onClick = {
                      if (isThisConnected) {
                        viewModel.disconnectBluetooth()
                      } else {
                        viewModel.connectBluetooth(dev)
                      }
                    },
                    modifier = Modifier
                      .height(36.dp)
                      .testTag("bt_connect_${dev.name}"),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = if (isThisConnected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                      contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                  ) {
                    Text(
                      text = if (isThisConnected) "DESCONECTAR" else "CONECTAR",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            }
          }
        }

        // Baud Rate Selection Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Velocidad Serial (Baud):",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
          )

          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(9600, 38400, 115200).forEach { baud ->
              FilterChip(
                selected = selectedBaudRate == baud,
                onClick = { selectedBaudRate = baud },
                label = { Text("$baud") },
                shape = RoundedCornerShape(6.dp)
              )
            }
          }
        }
      }
    }

    // 2. Telemetry and Link Metrics Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(12.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "MÉTRICAS DEL ENLACE BLUETOOTH",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text("PAQUETES RX / TX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              "${linkStats.packetsReceived} / ${linkStats.packetsSent}",
              style = MaterialTheme.typography.titleMedium,
              fontFamily = FontFamily.Monospace,
              color = DroGreen
            )
          }

          Column {
            Text("BYTES RX", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              "${linkStats.bytesReceived} B",
              style = MaterialTheme.typography.titleMedium,
              fontFamily = FontFamily.Monospace,
              color = DroCyan
            )
          }

          Column {
            Text("ESTADO DEL ENLACE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
              if (linkStats.isLinkActive) "ACTIVO" else "STANDBY",
              style = MaterialTheme.typography.titleMedium,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = if (linkStats.isLinkActive) DroGreen else IndDarkEmergency
            )
          }
        }
      }
    }

    // 3. Live Bluetooth Terminal & G-Code Sender
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(12.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "TERMINAL BLUETOOTH EN TIEMPO REAL",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "${logs.size} LÍNEAS",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = DroCyan
          )
        }

        // Terminal Log Viewport
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFF0F172A),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            reverseLayout = true
          ) {
            items(logs.reversed()) { log ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
              ) {
                Text(
                  text = log.timestamp,
                  style = MaterialTheme.typography.labelSmall,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 9.sp,
                  color = Color.Gray,
                  modifier = Modifier.width(70.dp)
                )
                Text(
                  text = when (log.direction) {
                    SerialDirection.TX -> "[TX] "
                    SerialDirection.RX -> "[RX] "
                    SerialDirection.INFO -> "[INFO] "
                    SerialDirection.ERROR -> "[ERR] "
                  },
                  style = MaterialTheme.typography.labelSmall,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = when (log.direction) {
                    SerialDirection.TX -> DroCyan
                    SerialDirection.RX -> DroGreen
                    SerialDirection.INFO -> IndDarkWarning
                    SerialDirection.ERROR -> IndDarkEmergency
                  }
                )
                Text(
                  text = log.text,
                  style = MaterialTheme.typography.labelSmall,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 10.sp,
                  color = Color.White
                )
              }
            }
          }
        }

        // Quick Command Chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          listOf("?", "\$\$", "\$I", "\$G", "\$#", "\$H", "\$X").forEach { cmd ->
            OutlinedButton(
              onClick = { viewModel.sendBluetoothCommand(cmd) },
              modifier = Modifier.height(30.dp),
              shape = RoundedCornerShape(4.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
              Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }
        }

        // Terminal Input Field
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = commandInput,
            onValueChange = { commandInput = it },
            modifier = Modifier
              .weight(1f)
              .testTag("bt_terminal_input"),
            placeholder = { Text("Escribir comando Grbl / G-Code...", fontSize = 12.sp) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
              if (commandInput.isNotBlank()) {
                viewModel.sendBluetoothCommand(commandInput)
                commandInput = ""
              }
            })
          )

          Button(
            onClick = {
              if (commandInput.isNotBlank()) {
                viewModel.sendBluetoothCommand(commandInput)
                commandInput = ""
              }
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(52.dp)
          ) {
            Icon(Icons.Default.Send, contentDescription = "Enviar")
          }
        }
      }
    }
  }
}
