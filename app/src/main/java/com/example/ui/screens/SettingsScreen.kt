package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.AppLanguage
import com.example.model.ConnectionStatus
import com.example.model.TransportMode
import com.example.model.UserRole
import com.example.ui.components.BluetoothManagerSection
import com.example.ui.components.ScrollableCarouselWithArrows
import com.example.ui.components.SerialManagerSection
import com.example.ui.theme.*
import com.example.viewmodel.CncViewModel

@Composable
fun SettingsScreen(
  viewModel: CncViewModel,
  modifier: Modifier = Modifier
) {
  val machineState by viewModel.machineState.collectAsStateWithLifecycle()
  val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
  val transportMode by viewModel.transportMode.collectAsStateWithLifecycle()
  val isEcoMode by viewModel.isEcoMode.collectAsStateWithLifecycle()
  val currentLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()

  var ipAddress by remember { mutableStateOf(machineState.ipAddress) }
  var portString by remember { mutableStateOf(machineState.port.toString()) }
  var selectedTransportMode by remember(transportMode) {
    mutableStateOf(
      when (transportMode) {
        TransportMode.SIMULATION -> 0
        TransportMode.WIFI_ETHERNET -> 1
        TransportMode.USB_SERIAL -> 2
        TransportMode.BLUETOOTH_SERIAL -> 3
      }
    )
  }

  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // UNO Q DROID System Identity Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(12.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Image(
          painter = painterResource(id = R.drawable.q1_droid_logo),
          contentDescription = "Q1 Droid CNC Logo",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )

        Column {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "1 Q CNC DROID",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = MaterialTheme.colorScheme.primaryContainer
            ) {
              Text(
                text = "v2.4 RT",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
          Text(
            text = "4-Axis Industrial CNC Motion Controller for Android",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "1 Q Dual-Core / STM32 / LinuxCNC / FluidNC / GRBL",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = DroCyan
          )
        }
      }
    }

    // Transport Mode Tab Selector Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(10.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = "DATA LINK & CONTROLLER INTERFACE",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "Select communication transport to 1 Q Hardware (QRB2210 Linux + STM32)",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        TabRow(
          selectedTabIndex = selectedTransportMode,
          containerColor = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.clip(RoundedCornerShape(8.dp))
        ) {
          Tab(
            selected = selectedTransportMode == 0,
            onClick = {
              selectedTransportMode = 0
              viewModel.setTransportMode(TransportMode.SIMULATION)
            },
            text = { Text("SIMULACIÓN", style = MaterialTheme.typography.labelSmall) }
          )
          Tab(
            selected = selectedTransportMode == 1,
            onClick = {
              selectedTransportMode = 1
              viewModel.setTransportMode(TransportMode.WIFI_ETHERNET)
            },
            text = { Text("WI-FI / LAN", style = MaterialTheme.typography.labelSmall) }
          )
          Tab(
            selected = selectedTransportMode == 2,
            onClick = {
              selectedTransportMode = 2
              viewModel.setTransportMode(TransportMode.USB_SERIAL)
            },
            text = { Text("USB SERIAL", style = MaterialTheme.typography.labelSmall) }
          )
          Tab(
            selected = selectedTransportMode == 3,
            onClick = {
              selectedTransportMode = 3
              viewModel.setTransportMode(TransportMode.BLUETOOTH_SERIAL)
            },
            text = { Text("BLUETOOTH", style = MaterialTheme.typography.labelSmall) }
          )
        }

        if (selectedTransportMode == 1) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = ipAddress,
              onValueChange = { ipAddress = it },
              label = { Text("1 Q Host IP Address") },
              modifier = Modifier.weight(2f),
              singleLine = true,
              shape = RoundedCornerShape(8.dp)
            )
            OutlinedTextField(
              value = portString,
              onValueChange = { portString = it },
              label = { Text("Port") },
              modifier = Modifier.weight(1f),
              singleLine = true,
              shape = RoundedCornerShape(8.dp)
            )
          }
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                val port = portString.toIntOrNull() ?: 5005
                viewModel.connectToMachine(ipAddress, port, isSimulated = false)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).height(46.dp)
            ) {
              Icon(Icons.Default.Link, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("CONNECT TCP / REST")
            }

            OutlinedButton(
              onClick = { viewModel.disconnect() },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.height(46.dp)
            ) {
              Text("DISCONNECT")
            }
          }
        } else if (selectedTransportMode == 0) {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                viewModel.connectToMachine("127.0.0.1", 5005, isSimulated = true)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).height(46.dp)
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("START VIRTUAL CNC ENGINE")
            }
          }
        }
      }
    }

    // If USB Serial mode selected, render the dedicated Serial Communication Manager
    if (selectedTransportMode == 2) {
      SerialManagerSection(viewModel = viewModel)
    }

    // If Bluetooth mode selected, render the dedicated Bluetooth Wireless Manager
    if (selectedTransportMode == 3) {
      BluetoothManagerSection(viewModel = viewModel)
    }

    // Language & Internationalization Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(10.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text(
            text = "INTERNATIONALIZATION & LANGUAGE (IDIOMA)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Text(
          text = "Select UI and Manual documentation language. English is default.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ScrollableCarouselWithArrows(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
          scrollStepDp = 160.dp,
          arrowSize = 30.dp,
          iconSize = 18.dp,
          spacing = 8.dp,
          testTagPrefix = "settings_lang"
        ) {
          AppLanguage.entries.forEach { lang ->
            val isSelected = currentLanguage == lang
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier
                .width(82.dp)
                .clickable { viewModel.setLanguage(lang) }
                .testTag("settings_lang_${lang.code}")
            ) {
              Column(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
              ) {
                Text(text = lang.flag, fontSize = 16.sp)
                Text(
                  text = lang.displayName,
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 10.sp,
                  fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }
    }

    // UI Theme & Industrial Preferences Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(10.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = "APPEARANCE & OPERATOR ROLE",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Dark / Light Industrial Theme Toggle Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text("Industrial Color Scheme", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
              if (isDarkTheme) "High-Contrast Cockpit Dark (Default)" else "Shopfloor Sunlight Light",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Switch(
            checked = isDarkTheme,
            onCheckedChange = { viewModel.setDarkTheme(it) }
          )
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Operator vs Maintenance Mode Toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text("Security Role Access", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
              "Current: ${machineState.userRole.name.replace('_', ' ')}",
              style = MaterialTheme.typography.bodySmall,
              color = if (machineState.userRole == UserRole.MAINTENANCE_ENGINEER) DroAmber else DroGreen
            )
          }

          Button(
            onClick = { viewModel.toggleUserRole() },
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (machineState.userRole == UserRole.MAINTENANCE_ENGINEER) DroAmber else MaterialTheme.colorScheme.surfaceVariant,
              contentColor = if (machineState.userRole == UserRole.MAINTENANCE_ENGINEER) Color.Black else MaterialTheme.colorScheme.onSurface
            )
          ) {
            Text("SWITCH ROLE", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }

    // Resource, Battery & Memory Optimization Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("resource_management_card"),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
      ),
      shape = RoundedCornerShape(10.dp),
      border = BorderStroke(
        1.dp,
        if (isEcoMode) DroGreen.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline
      )
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = if (isEcoMode) Icons.Default.BatterySaver else Icons.Default.BatteryChargingFull,
              contentDescription = "Battery & Resource Saver",
              tint = if (isEcoMode) DroGreen else MaterialTheme.colorScheme.primary
            )
            Column {
              Text(
                text = "GESTIÓN DE RECURSOS Y BATERÍA",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isEcoMode) DroGreen else MaterialTheme.colorScheme.primary
              )
              Text(
                text = if (isEcoMode) "Modo Eco Activo (Ahorro Máximo)" else "Modo Rendimiento Normal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Switch(
            checked = isEcoMode,
            onCheckedChange = { viewModel.setEcoMode(it) },
            modifier = Modifier.testTag("eco_mode_switch")
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = if (isEcoMode) {
            "El modo Eco reduce la tasa de actualización de telemetría, detiene las animaciones continuas de GPU para ahorrar batería y limita el tamaño de los búferes de memoria RAM."
          } else {
            "Optimizaciones activas por defecto: pausa de flujos de fondo con Lifecycle, búferes circulares sin fugas de memoria y sondeo adaptativo en reposo."
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Resource Metrics Grid
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            SettingRowInfo("Tasa de Telemetría", if (isEcoMode) "650 ms (Bajo Consumo CPU)" else "120-350 ms (Adaptativo)")
            SettingRowInfo("Animaciones GPU", if (isEcoMode) "Estáticas (0% CPU de render)" else "Pulsos dinámicos")
            SettingRowInfo("Sondeo Watchdog", if (isEcoMode) "500 ms (Ahorro de enlaces)" else "200 ms (5 Hz)")
            SettingRowInfo("Búfer de Terminal", if (isEcoMode) "60 líneas (Baja RAM)" else "120 líneas")
            SettingRowInfo("Consumo Estimado", if (isEcoMode) "BAJO (~60-75% menos ciclos)" else "ESTÁNDAR")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Memory Cleanup Action Button
        OutlinedButton(
          onClick = {
            viewModel.clearAllTerminalLogs()
            viewModel.clearMdiHistory()
          },
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("clear_buffers_button"),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = DroCyan
          )
        ) {
          Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("LIBERAR MEMORIA RAM (VACIAR BÚFERES Y LOGS)", style = MaterialTheme.typography.labelSmall)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Machine Axis Calibration Specs
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(10.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = "MACHINE GEOMETRY & KINEMATICS",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        SettingRowInfo("X-Axis Travel", "0.0 to 400.0 mm (Scale: 400.0 steps/mm)")
        SettingRowInfo("Y-Axis Travel", "0.0 to 300.0 mm (Scale: 400.0 steps/mm)")
        SettingRowInfo("Z-Axis Travel", "-50.0 to 120.0 mm (Scale: 800.0 steps/mm)")
        SettingRowInfo("A-Axis Rotary", "0.0 to 360.0 deg (Scale: 88.88 steps/deg)")
        SettingRowInfo("Max Rapid Feed", "6000.0 mm/min (100 mm/s)")
        SettingRowInfo("Max Acceleration", "1500.0 mm/s² (S-Curve)")
        SettingRowInfo("Spindle Max RPM", "24000 RPM (2.2kW VFD Modbus/PWM)")
        SettingRowInfo("Safety Watchdog Timeout", "50 ms (Hardware Interlock)")
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Google Play Policy & Privacy Compliance Card
    var showPrivacyDialog by remember { mutableStateOf(false) }

    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(10.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = "PRIVACIDAD Y POLÍTICA DE SEGURIDAD INDUSTRIAL",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "1 Q CNC opera 100% de manera local y fuera de línea (offline). No recopila, no transmite ni almacena datos personales ni telemetría en servidores externos. Cumple con las directrices para desarrolladores de Google Play.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = { showPrivacyDialog = true },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("privacy_policy_button"),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("VER POLÍTICA DE PRIVACIDAD COMPLETA (PLAY STORE)", style = MaterialTheme.typography.labelSmall)
        }
      }
    }

    if (showPrivacyDialog) {
      AlertDialog(
        onDismissRequest = { showPrivacyDialog = false },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Política de Privacidad - 1 Q CNC")
          }
        },
        text = {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "Última actualización: Septiembre 2026",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "1. Alcance de Datos: La aplicación '1 Q CNC' es una herramienta técnica para el control de máquinas CNC. NO recopila información de identificación personal, cuentas, correos, contraseñas ni datos biométricos.",
              style = MaterialTheme.typography.bodySmall
            )
            Text(
              text = "2. Permisos del Dispositivo:\n• USB Host: Acceso exclusivo para transferir paquetes G-code y comandos de movimiento al microcontrolador conectado vía cable OTG.\n• Bluetooth (BLUETOOTH_CONNECT / SCAN): Utilizado únicamente para comunicación serie con módulos SPP en la máquina herramienta. Se declara 'neverForLocation' para garantizar que nunca se rastrea la ubicación física.\n• Almacenamiento: Se utiliza exclusivamente el selector seguro de archivos de Android (Photo/Document Picker) sin solicitar permisos invasivos de lectura global.",
              style = MaterialTheme.typography.bodySmall
            )
            Text(
              text = "3. Operación Offline: Todos los programas G-code, configuraciones de ejes y calibración de herramientas se guardan de forma cifrada en la base de datos Room local del dispositivo.",
              style = MaterialTheme.typography.bodySmall
            )
            Text(
              text = "4. Enlace Público Play Store: Documento oficial disponible en el repositorio del proyecto (PRIVACY_POLICY.md).",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
          }
        },
        confirmButton = {
          TextButton(onClick = { showPrivacyDialog = false }) {
            Text("CERRAR")
          }
        }
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Open Source Licenses & Attributions (Apache 2.0 / MIT / BSD)
    var showLicensesDialog by remember { mutableStateOf(false) }

    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(10.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Code,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Text(
            text = "LICENCIAS DE SOFTWARE LIBRE Y ATRIBUCIONES",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "1 Q CNC está construido utilizando componentes de código abierto con licencias permisivas (Apache 2.0, MIT, BSD 3-Clause).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = { showLicensesDialog = true },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("open_source_licenses_button"),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("VER LICENCIAS DE TERCEROS (APACHE 2.0 / MIT)", style = MaterialTheme.typography.labelSmall)
        }
      }
    }

    if (showLicensesDialog) {
      AlertDialog(
        onDismissRequest = { showLicensesDialog = false },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Avisos de Software de Terceros")
          }
        },
        text = {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "Esta aplicación incluye las siguientes bibliotecas de código abierto:",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold
            )

            HorizontalDivider()

            Text("• Android Open Source Project (AOSP) & Jetpack Compose\n  Licencia: Apache License 2.0\n  Copyright (c) The Android Open Source Project", style = MaterialTheme.typography.bodySmall)

            Text("• Kotlin Standard Library & Coroutines\n  Licencia: Apache License 2.0\n  Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.", style = MaterialTheme.typography.bodySmall)

            Text("• Square OkHttp & Retrofit & Moshi\n  Licencia: Apache License 2.0\n  Copyright 2013-2026 Square, Inc.", style = MaterialTheme.typography.bodySmall)

            Text("• AndroidX Room Persistence Library\n  Licencia: Apache License 2.0\n  Copyright (c) The Android Open Source Project", style = MaterialTheme.typography.bodySmall)

            Text("• Material Symbols & Material Design 3\n  Licencia: Apache License 2.0\n  Copyright Google LLC", style = MaterialTheme.typography.bodySmall)

            HorizontalDivider()

            Text(
              text = "Licencia Apache 2.0 Resumida: Se concede permiso para usar, reproducir y distribuir el software sin regalías, sujeto a la preservación de los avisos de derechos de autor y licencias originales.",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        },
        confirmButton = {
          TextButton(onClick = { showLicensesDialog = false }) {
            Text("CERRAR")
          }
        }
      )
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
fun SettingRowInfo(label: String, value: String) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(text = value, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
  }
}
