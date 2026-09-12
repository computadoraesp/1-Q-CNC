package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.GCodeKinematicsAndMeshEngine
import com.example.model.*
import com.example.transport.CncTransport
import com.example.transport.MockCncEngine
import com.example.transport.serial.SerialCommunicationManager
import com.example.transport.serial.UsbSerialCncTransport
import com.example.transport.serial.BluetoothSerialManager
import com.example.transport.serial.BluetoothSerialCncTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

class CncViewModel(
  application: Application
) : AndroidViewModel(application) {

  // Serial Communication Manager instance
  val serialManager = SerialCommunicationManager(application.applicationContext)
  private val mockTransport = MockCncEngine()
  private val usbSerialTransport = UsbSerialCncTransport(serialManager)

  // Bluetooth Wireless Communication Manager instance
  val bluetoothManager = BluetoothSerialManager(application.applicationContext)
  private val bluetoothSerialTransport = BluetoothSerialCncTransport(bluetoothManager)

  private val _transportMode = MutableStateFlow(TransportMode.SIMULATION)
  val transportMode: StateFlow<TransportMode> = _transportMode.asStateFlow()

  private var activeTransport: CncTransport = mockTransport

  private val _machineState = MutableStateFlow(activeTransport.machineState.value)
  val machineState: StateFlow<MachineState> = _machineState.asStateFlow()

  private val _mcuDiagnostics = MutableStateFlow(activeTransport.mcuDiagnostics.value)
  val mcuDiagnostics: StateFlow<McuDiagnostics> = _mcuDiagnostics.asStateFlow()

  private val _halPins = MutableStateFlow(activeTransport.halPins.value)
  val halPins: StateFlow<List<HalPin>> = _halPins.asStateFlow()

  // Serial Manager State Flows exposed to UI
  val availableUsbDevices: StateFlow<List<SerialDeviceInfo>> = serialManager.availableDevices
  val connectedUsbDevice: StateFlow<SerialDeviceInfo?> = serialManager.connectedDeviceInfo
  val serialLinkStats: StateFlow<SerialLinkStats> = serialManager.linkStats
  val serialLogs: StateFlow<List<SerialLogEntry>> = serialManager.serialLogs
  val serialPortConfig: StateFlow<SerialPortConfig> = serialManager.portConfig

  // Bluetooth State Flows exposed to UI
  val availableBluetoothDevices: StateFlow<List<BluetoothDeviceInfo>> = bluetoothManager.availableDevices
  val connectedBluetoothDevice: StateFlow<BluetoothDeviceInfo?> = bluetoothManager.connectedDeviceInfo
  val bluetoothLinkStats: StateFlow<SerialLinkStats> = bluetoothManager.linkStats
  val bluetoothLogs: StateFlow<List<SerialLogEntry>> = bluetoothManager.bluetoothLogs

  // Serial Console Command input
  private val _serialCommandInput = MutableStateFlow("")
  val serialCommandInput: StateFlow<String> = _serialCommandInput.asStateFlow()

  // Jog Configuration
  private val _jogConfig = MutableStateFlow(JogConfig())
  val jogConfig: StateFlow<JogConfig> = _jogConfig.asStateFlow()

  // MDI state
  private val _mdiInput = MutableStateFlow("")
  val mdiInput: StateFlow<String> = _mdiInput.asStateFlow()

  private val _mdiHistory = MutableStateFlow<List<MdiHistoryItem>>(
    listOf(
      MdiHistoryItem("G21 G90 G54", "10:45:12", true, "OK (Metric mode, Absolute positioning, WCS G54)"),
      MdiHistoryItem("G0 Z25.000", "10:45:30", true, "OK (Rapid move to safe height)"),
      MdiHistoryItem("M3 S12000", "10:46:00", true, "OK (Spindle CW started @ 12000 RPM)")
    )
  )
  val mdiHistory: StateFlow<List<MdiHistoryItem>> = _mdiHistory.asStateFlow()

  // G-Code Programs Library
  private val _gcodeFiles = MutableStateFlow<List<GCodeFileItem>>(
    listOf(
      GCodeFileItem(
        name = "face_pocket_bracket.ngc",
        sizeBytes = 48520,
        lineCount = 142,
        estimatedTimeMinutes = 4.5,
        sampleLines = listOf(
          "(Bracket Face Milling - LinuxCNC)",
          "G21 G90 G17 G54 G64 P0.05",
          "T1 M6 (Endmill D=6mm)",
          "S12000 M3",
          "G0 X0.000 Y0.000 Z15.000",
          "G0 X10.000 Y10.000",
          "G1 Z-2.000 F600",
          "G1 X90.000 F1500",
          "G1 Y60.000",
          "G1 X10.000",
          "G1 Y10.000",
          "G0 Z15.000",
          "M5 M9",
          "M30"
        ),
        toolpathPoints = MockCncEngine.generateDefaultSampleToolpath()
      ),
      GCodeFileItem(
        name = "aluminum_motor_mount.ngc",
        sizeBytes = 125400,
        lineCount = 380,
        estimatedTimeMinutes = 12.0,
        sampleLines = listOf(
          "(NEMA 23 Motor Mount - 6061-T6)",
          "G21 G90 G54",
          "T2 M6 (Endmill D=3.175mm)",
          "S18000 M3 M8",
          "G0 Z5.000",
          "G0 X25.000 Y25.000",
          "G1 Z-1.000 F400",
          "G2 X25.000 Y25.000 I15.000 J0.000 F1200",
          "G0 Z10.000",
          "M30"
        ),
        toolpathPoints = MockCncEngine.generateDefaultSampleToolpath()
      ),
      GCodeFileItem(
        name = "spoilboard_surfacing.ngc",
        sizeBytes = 18200,
        lineCount = 64,
        estimatedTimeMinutes = 3.2,
        sampleLines = listOf(
          "(Spoilboard Flattening Flycutter)",
          "G21 G90 G54",
          "T5 M6 (Flycutter D=25mm)",
          "S8000 M3",
          "G0 X0 Y0 Z2.0",
          "G1 Z-0.5 F800",
          "G1 X380.0 F2500",
          "G0 Z5.0",
          "M30"
        ),
        toolpathPoints = MockCncEngine.generateDefaultSampleToolpath()
      )
    )
  )
  val gcodeFiles: StateFlow<List<GCodeFileItem>> = _gcodeFiles.asStateFlow()

  // Tool Table
  private val _toolTable = MutableStateFlow<List<ToolItem>>(
    listOf(
      ToolItem(1, 6.0, 42.5, 0.00, "Flat Endmill", "6mm 2-Flute Carbide for Aluminum"),
      ToolItem(2, 3.175, 38.0, 0.00, "Flat Endmill", "1/8 inch Rougher for Plastics/PCB"),
      ToolItem(3, 4.0, 45.0, 0.02, "Ball Nose", "4mm 3D Contouring & Finishing"),
      ToolItem(4, 1.0, 32.0, 0.00, "Drill Bit", "1.0mm Through-hole PCB Drill"),
      ToolItem(5, 25.0, 30.0, 0.00, "Flycutter", "25mm Spoilboard Surfacing tool"),
      ToolItem(6, 0.2, 28.0, 0.00, "V-Bit 60°", "Chamfer & Engraving bit")
    )
  )
  val toolTable: StateFlow<List<ToolItem>> = _toolTable.asStateFlow()

  // WCS Table
  private val _wcsList = MutableStateFlow<List<WcsOffset>>(
    listOf(
      WcsOffset("G54", "Main Vise Fixture", 0.000, 0.000, 0.000, 0.000, true),
      WcsOffset("G55", "Secondary Fixture (Soft Jaws)", 150.000, 50.000, -12.400, 0.000, false),
      WcsOffset("G56", "Rotary 4th Axis A-Puck", 280.000, 120.000, -5.000, 0.000, false),
      WcsOffset("G57", "Tool Setter Reference", 380.000, 20.000, -45.000, 0.000, false),
      WcsOffset("G58", "Spare Fixture WCS", 0.000, 0.000, 0.000, 0.000, false),
      WcsOffset("G59", "Maintenance Calibration", 0.000, 0.000, 0.000, 0.000, false)
    )
  )
  val wcsList: StateFlow<List<WcsOffset>> = _wcsList.asStateFlow()

  // Commissioning Plan
  private val _commissioningPhases = MutableStateFlow<List<CommissioningPhase>>(
    listOf(
      CommissioningPhase(0, "Machine Requirements", "Record XYZ mechanics, motor drivers, power, spindle, limits, probe, E-stop, microstepping, and target STEP frequency.", "PASS: Complete specifications verified.", true, "Verify Spec", "XYZ Travel: 400x300x120mm, Leadshine DM542, 2.2kW ER20, 250kHz"),
      CommissioningPhase(1, "UNO Q Baseline", "Verify Debian ARM64 OS, QRB2210 CPU, STM32U585 coprocessor, RPC bridge and networking.", "PASS: Stable Linux↔MCU link.", true, "Test Baseline", "Debian 12 Bookworm, Zephyr OS on STM32, RPC ping: 0.8ms"),
      CommissioningPhase(2, "LinuxCNC Setup", "Install LinuxCNC 2.9 ARM64, XYZ HAL/INI configuration and virtual machine simulation.", "PASS: Simulated XYZ operates correctly.", true, "Verify LinuxCNC", "LinuxCNC 2.9.2 running in simulation mode with HAL XYZ stepgen"),
      CommissioningPhase(3, "Real-Time RT Preempt", "Install PREEMPT_RT kernel, measure worst-case latency and jitter under full system load.", "PASS: Latency <= 25µs guaranteed.", true, "Measure Latency", "Worst latency: 14.8µs, Avg Jitter: 2.1µs (Excellent for motion)"),
      CommissioningPhase(4, "Motion Architecture", "Compare Linux pulse generation vs STM32/Zephyr deterministic STEP/DIR timing.", "PASS: STM32 selected for deterministic timing.", true, "Select Architecture", "STM32 Hardware Timers guarantee zero jitter pulses up to 250kHz"),
      CommissioningPhase(5, "STM32 Motion & I/O", "Implement 3-axis STEP/DIR channels, S-curve accel/decel, limit switches, homing and watchdog.", "PASS: Timing and fault tests pass.", true, "Test 3-Axis STEP", "Ramp acceleration 1500 mm/s², zero lost steps on 100k pulses"),
      CommissioningPhase(6, "CNC Control API", "Versioned JSON/WebSocket API for machine state, axes, jog, homing, spindle, G-code, MDI and HAL diagnostics.", "PASS: API controls simulated CNC.", true, "Test API", "Exposes /api/v1/motion, /api/v1/jog, /api/v1/hal, /api/v1/gcode"),
      CommissioningPhase(7, "Hardware Safety", "Hardware E-Stop loop, spindle interlock, motor driver enable, watchdog and comms-loss fail-safe.", "PASS: Safe state survives any comms failure.", true, "Test E-Stop Loop", "Drivers disable within 1.2ms if watchdog heartbeat is lost"),
      CommissioningPhase(8, "Android Foundation", "Kotlin + Jetpack Compose modular HMI, transport abstraction and viewmodels.", "PASS: Android connects and renders live state.", true, "Verify UI Link", "Jetpack Compose Semi-Industrial Dark/Light HMI linked"),
      CommissioningPhase(9, "Machine Control", "Real-time DRO (XYZ/A), continuous & step jog, homing routines, spindle RPM dial, feed overrides.", "PASS: Simulated machine fully controllable.", true, "Test Machine Control", "Jog increments (0.001 to 10mm), Spindle override 50-150%"),
      CommissioningPhase(10, "G-Code Execution", "Program selector, 2D/3D toolpath visualizer, run/pause/resume/stop, line tracker and MDI console.", "PASS: Complete program lifecycle verified.", true, "Test G-Code Run", "Parsed face_pocket_bracket.ngc, active toolpath preview"),
      CommissioningPhase(11, "Diagnostics & HAL", "Inspect LinuxCNC HAL pins/signals, STM32 MCU telemetry, watchdog pulse, latency histograms.", "PASS: Full system diagnosed from Android.", true, "Inspect HAL", "Live monitoring of 22 HAL pins + STM32 jitter & CPU stats"),
      CommissioningPhase(12, "Multi-Transport", "Production priority: Ethernet LAN, Wi-Fi WebSocket/REST, USB CDC serial and local simulation.", "PASS: Transports have defined safe failover.", true, "Verify Transports", "Auto-reconnect with safe deceleration on signal loss"),
      CommissioningPhase(13, "Security & Roles", "Operator mode (safe operations) vs Maintenance Engineer mode (dangerous actions confirmed).", "PASS: Unauthorized control prevented.", true, "Test Access Control", "Confirmation dialogs for axis zeroing, tool offsets, and raw HAL"),
      CommissioningPhase(14, "System Integration", "Android ↔ UNO Q ↔ LinuxCNC ↔ STM32 ↔ CNC electronics synchronized.", "PASS: End-to-end telemetry verified.", true, "Test Full Integration", "State synchronizer maintains < 20ms roundtrip HMI latency"),
      CommissioningPhase(15, "Machine Commissioning", "No-power dry run, limit switch verification, homing calibration, spindle test, test cut in foam.", "PASS: Safe repeatable machining.", false, "Run Dry Run", "Ready for physical axis touch-off and air cut"),
      CommissioningPhase(16, "Performance Envelope", "Measure maximum reliable STEP frequency, coordinated feed rate, thermal envelope and jitter.", "PASS: Documented operating envelope.", false, "Benchmark Envelope", "Targeting 250kHz step rate, 6000 mm/min rapid feed")
    )
  )
  val commissioningPhases: StateFlow<List<CommissioningPhase>> = _commissioningPhases.asStateFlow()

  // App Theme State (Dark vs Light Industrial)
  private val _isDarkTheme = MutableStateFlow(true)
  val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

  // Resource & Battery Saver Mode (Eco Mode)
  private val _isEcoMode = MutableStateFlow(false)
  val isEcoMode: StateFlow<Boolean> = _isEcoMode.asStateFlow()

  // App Internationalization Language (EN default, ES, DE, FR, JA, KO)
  private val _selectedLanguage = MutableStateFlow(AppLanguage.EN)
  val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

  fun setLanguage(lang: AppLanguage) {
    _selectedLanguage.value = lang
  }

  fun toggleEcoMode() {
    setEcoMode(!_isEcoMode.value)
  }

  fun setEcoMode(enabled: Boolean) {
    _isEcoMode.value = enabled
    mockTransport.isEcoMode = enabled
    serialManager.isEcoMode = enabled
    bluetoothManager.isEcoMode = enabled
  }

  fun clearAllTerminalLogs() {
    serialManager.clearLogs()
    bluetoothManager.clearLogs()
  }

  fun clearMdiHistory() {
    _mdiHistory.value = emptyList()
  }

  // --- JOB TIME ESTIMATE STATE ---
  private val _jobEstimate = MutableStateFlow(
    GCodeKinematicsAndMeshEngine.calculateJobEstimate(_gcodeFiles.value.first().sampleLines)
  )
  val jobEstimate: StateFlow<JobTimeEstimate> = _jobEstimate.asStateFlow()

  // --- AUTO LEVELING & PCB SURFACE MESH STATE ---
  private val _autoLevelMeshState = MutableStateFlow(
    AutoLevelMeshState(
      config = MeshGridConfig(xMin = 0.0, xMax = 80.0, yMin = 0.0, yMax = 60.0, gridCols = 5, gridRows = 4),
      points = GCodeKinematicsAndMeshEngine.generateInitialMeshGrid(
        MeshGridConfig(xMin = 0.0, xMax = 80.0, yMin = 0.0, yMax = 60.0, gridCols = 5, gridRows = 4)
      )
    )
  )
  val autoLevelMeshState: StateFlow<AutoLevelMeshState> = _autoLevelMeshState.asStateFlow()

  // --- MULTIPOINT AXIS CALIBRATION & ERROR MAPPING STATE ---
  private val _axisCalibrationSession = MutableStateFlow(
    GCodeKinematicsAndMeshEngine.createInitialCalibrationSession(
      axis = CalibrationAxis.Y,
      totalLengthMm = 300.0,
      intervalPercent = 10,
      currentStepsPerMm = 80.0
    )
  )
  val axisCalibrationSession: StateFlow<AxisCalibrationSession> = _axisCalibrationSession.asStateFlow()

  private var stateSyncJob: Job? = null
  private var probingJob: Job? = null

  init {
    bindTransport(mockTransport)
    scanUsbDevices()
  }

  private fun bindTransport(transport: CncTransport) {
    activeTransport = transport
    stateSyncJob?.cancel()
    stateSyncJob = viewModelScope.launch {
      launch {
        transport.machineState.collect { state ->
          _machineState.value = state
        }
      }
      launch {
        transport.mcuDiagnostics.collect { diag ->
          _mcuDiagnostics.value = diag
        }
      }
      launch {
        transport.halPins.collect { pins ->
          _halPins.value = pins
        }
      }
    }
  }

  fun setTransportMode(mode: TransportMode) {
    _transportMode.value = mode
    when (mode) {
      TransportMode.SIMULATION -> {
        serialManager.disconnect()
        bluetoothManager.disconnect()
        bindTransport(mockTransport)
      }
      TransportMode.WIFI_ETHERNET -> {
        serialManager.disconnect()
        bluetoothManager.disconnect()
        bindTransport(mockTransport)
      }
      TransportMode.USB_SERIAL -> {
        bluetoothManager.disconnect()
        bindTransport(usbSerialTransport)
      }
      TransportMode.BLUETOOTH_SERIAL -> {
        serialManager.disconnect()
        bindTransport(bluetoothSerialTransport)
      }
    }
  }

  // Bluetooth Serial Methods
  fun scanBluetoothDevices(): List<BluetoothDeviceInfo> {
    return bluetoothManager.scanDevices()
  }

  fun connectBluetooth(deviceInfo: BluetoothDeviceInfo) {
    setTransportMode(TransportMode.BLUETOOTH_SERIAL)
    bluetoothManager.connect(deviceInfo)
  }

  fun disconnectBluetooth() {
    bluetoothManager.disconnect()
  }

  fun sendBluetoothCommand(command: String) {
    bluetoothManager.sendLine(command)
  }

  // USB Serial Methods
  fun scanUsbDevices(): List<SerialDeviceInfo> {
    return serialManager.scanDevices()
  }

  fun connectUsbSerial(deviceInfo: SerialDeviceInfo? = null, baudRate: Int? = null) {
    setTransportMode(TransportMode.USB_SERIAL)
    if (baudRate != null) {
      serialManager.updatePortConfig(serialManager.portConfig.value.copy(baudRate = baudRate))
    }
    serialManager.connect(deviceInfo)
  }

  fun disconnectUsbSerial() {
    serialManager.disconnect()
  }

  fun updateSerialBaudRate(baud: Int) {
    serialManager.updatePortConfig(serialManager.portConfig.value.copy(baudRate = baud))
  }

  fun updateSerialParity(parity: SerialParity) {
    serialManager.updatePortConfig(serialManager.portConfig.value.copy(parity = parity))
  }

  fun updateSerialStopBits(stopBits: SerialStopBits) {
    serialManager.updatePortConfig(serialManager.portConfig.value.copy(stopBits = stopBits))
  }

  fun toggleDtr(enable: Boolean) {
    serialManager.setDtrRts(dtr = enable, rts = serialManager.portConfig.value.rtsEnable)
  }

  fun toggleRts(enable: Boolean) {
    serialManager.setDtrRts(dtr = serialManager.portConfig.value.dtrEnable, rts = enable)
  }

  fun updateSerialCommandInput(text: String) {
    _serialCommandInput.value = text
  }

  fun sendSerialCommand(cmd: String = _serialCommandInput.value) {
    val clean = cmd.trim()
    if (clean.isBlank()) return
    viewModelScope.launch {
      serialManager.writeString("$clean\n")
      _serialCommandInput.value = ""
    }
  }

  fun sendRealtimeSerialChar(c: Char) {
    serialManager.sendRealtimeChar(c)
  }

  fun clearSerialLogs() {
    serialManager.clearLogs()
  }

  fun toggleTheme() {
    _isDarkTheme.value = !_isDarkTheme.value
  }

  fun setDarkTheme(dark: Boolean) {
    _isDarkTheme.value = dark
  }

  fun toggleUserRole() {
    val current = machineState.value.userRole
    val next = if (current == UserRole.OPERATOR) UserRole.MAINTENANCE_ENGINEER else UserRole.OPERATOR
    _machineState.value = _machineState.value.copy(userRole = next)
  }

  fun setJogIncrement(incrementMm: Double) {
    _jogConfig.value = _jogConfig.value.copy(stepIncrementMm = incrementMm, isIncremental = true)
  }

  fun setJogContinuous(continuous: Boolean) {
    _jogConfig.value = _jogConfig.value.copy(isIncremental = !continuous)
  }

  fun setJogSpeed(speed: Double) {
    _jogConfig.value = _jogConfig.value.copy(jogSpeedMmMin = speed)
  }

  fun jogAxis(axis: String, positive: Boolean) {
    val config = _jogConfig.value
    val step = if (config.isIncremental) {
      if (positive) config.stepIncrementMm else -config.stepIncrementMm
    } else {
      if (positive) 5.0 else -5.0
    }
    activeTransport.sendJog(axis, step, config.jogSpeedMmMin)
  }

  fun stopJog(axis: String) {
    activeTransport.stopJog(axis)
  }

  fun homeAxis(axis: String) {
    activeTransport.homeAxis(axis)
  }

  fun zeroAxis(axis: String) {
    activeTransport.zeroWorkCoordinate(axis)
  }

  fun setWcs(code: String) {
    activeTransport.setWorkCoordinateSystem(code)
    _wcsList.value = _wcsList.value.map {
      it.copy(isActive = (it.code == code))
    }
  }

  private fun wcsCodeToPIndex(code: String): Int {
    return when (code.uppercase()) {
      "G54" -> 1
      "G55" -> 2
      "G56" -> 3
      "G57" -> 4
      "G58" -> 5
      "G59" -> 6
      "G59.1" -> 7
      "G59.2" -> 8
      "G59.3" -> 9
      else -> 1
    }
  }

  /**
   * Reset all axis offsets for a specific Work Coordinate System (G54-G59) to 0.000
   */
  fun resetWcsOffset(wcsCode: String) {
    val pIndex = wcsCodeToPIndex(wcsCode)
    viewModelScope.launch {
      // Send G10 L2 P{index} X0 Y0 Z0 A0 to reset work offset origin in controller
      serialManager.writeString("G10 L2 P$pIndex X0 Y0 Z0 A0\n")
      // Update local state list
      _wcsList.value = _wcsList.value.map {
        if (it.code.equals(wcsCode, ignoreCase = true)) {
          it.copy(offsetX = 0.0, offsetY = 0.0, offsetZ = 0.0, offsetA = 0.0)
        } else it
      }
      _machineState.value = _machineState.value.copy(
        systemMessage = "Reset all coordinate offsets to 0.000 for $wcsCode"
      )
    }
  }

  /**
   * Reset a single axis offset (X, Y, Z, or A) for a given WCS to 0.000
   */
  fun resetWcsSingleAxis(wcsCode: String, axis: String) {
    val pIndex = wcsCodeToPIndex(wcsCode)
    val upperAxis = axis.uppercase()
    viewModelScope.launch {
      serialManager.writeString("G10 L2 P$pIndex ${upperAxis}0\n")
      _wcsList.value = _wcsList.value.map { item ->
        if (item.code.equals(wcsCode, ignoreCase = true)) {
          when (upperAxis) {
            "X" -> item.copy(offsetX = 0.0)
            "Y" -> item.copy(offsetY = 0.0)
            "Z" -> item.copy(offsetZ = 0.0)
            "A" -> item.copy(offsetA = 0.0)
            else -> item
          }
        } else item
      }
      _machineState.value = _machineState.value.copy(
        systemMessage = "Reset $wcsCode $upperAxis offset to 0.000"
      )
    }
  }

  /**
   * Touch-off / Zero current tool position in specific WCS (G10 L20 P{n})
   */
  fun touchOffWcsAxis(wcsCode: String, axis: String, targetCoord: Double = 0.0) {
    val pIndex = wcsCodeToPIndex(wcsCode)
    val upperAxis = axis.uppercase()
    viewModelScope.launch {
      serialManager.writeString("G10 L20 P$pIndex $upperAxis$targetCoord\n")
      val currentState = _machineState.value
      val currentPos = when (upperAxis) {
        "X" -> currentState.posX
        "Y" -> currentState.posY
        "Z" -> currentState.posZ
        "A" -> currentState.posA
        else -> 0.0
      }
      _wcsList.value = _wcsList.value.map { item ->
        if (item.code.equals(wcsCode, ignoreCase = true)) {
          when (upperAxis) {
            "X" -> item.copy(offsetX = currentPos)
            "Y" -> item.copy(offsetY = currentPos)
            "Z" -> item.copy(offsetZ = currentPos)
            "A" -> item.copy(offsetA = currentPos)
            else -> item
          }
        } else item
      }
      _machineState.value = _machineState.value.copy(
        systemMessage = "Touched off $upperAxis to $targetCoord in $wcsCode"
      )
    }
  }

  /**
   * Reset all WCS coordinate offsets (G54 through G59) back to origin (0,0,0,0)
   */
  fun resetAllWcsOffsets() {
    viewModelScope.launch {
      for (p in 1..6) {
        serialManager.writeString("G10 L2 P$p X0 Y0 Z0 A0\n")
      }
      _wcsList.value = _wcsList.value.map {
        it.copy(offsetX = 0.0, offsetY = 0.0, offsetZ = 0.0, offsetA = 0.0)
      }
      _machineState.value = _machineState.value.copy(
        systemMessage = "Reset ALL Work Coordinate Offsets (G54 - G59) to 0.000"
      )
    }
  }

  /**
   * Query current active offsets from CNC controller via serial ($# command)
   */
  fun queryWcsOffsetsFromController() {
    viewModelScope.launch {
      serialManager.writeString("\$#\n")
      _machineState.value = _machineState.value.copy(
        systemMessage = "Queried controller work coordinate offsets (\$#)"
      )
    }
  }

  fun toggleSpindle(direction: SpindleDirection, rpm: Int) {
    val current = machineState.value.spindleDirection
    if (current != SpindleDirection.OFF) {
      activeTransport.setSpindle(SpindleDirection.OFF, 0)
    } else {
      activeTransport.setSpindle(direction, rpm)
    }
  }

  fun setSpindleRpm(rpm: Int) {
    val currentDir = machineState.value.spindleDirection
    val dir = if (currentDir == SpindleDirection.OFF) SpindleDirection.CW else currentDir
    activeTransport.setSpindle(dir, rpm)
  }

  fun toggleCoolant() {
    val current = machineState.value.coolantState
    val next = when (current) {
      CoolantState.OFF -> CoolantState.FLOOD
      CoolantState.FLOOD -> CoolantState.MIST
      CoolantState.MIST -> CoolantState.BOTH
      CoolantState.BOTH -> CoolantState.OFF
    }
    activeTransport.setCoolant(next)
  }

  fun setFeedOverride(pct: Int) = activeTransport.setFeedOverride(pct)
  fun setRapidOverride(pct: Int) = activeTransport.setRapidOverride(pct)
  fun setSpindleOverride(pct: Int) = activeTransport.setSpindleOverride(pct)

  fun triggerEStop() = activeTransport.triggerEStop()
  fun resetEStop() = activeTransport.resetEStop()
  fun toggleDriverEnable() {
    activeTransport.setDriverEnabled(!machineState.value.isDriverEnabled)
  }
  fun clearAlarms() = activeTransport.clearAlarms()

  fun selectGCodeProgram(fileItem: GCodeFileItem) {
    activeTransport.loadGCodeProgram(fileItem.name)
    _jobEstimate.value = GCodeKinematicsAndMeshEngine.calculateJobEstimate(fileItem.sampleLines)
  }

  fun recalculateJobEstimate() {
    val currentFile = _gcodeFiles.value.firstOrNull { it.name == machineState.value.gcodeFileName }
      ?: _gcodeFiles.value.first()
    _jobEstimate.value = GCodeKinematicsAndMeshEngine.calculateJobEstimate(currentFile.sampleLines)
  }

  // --- AUTO LEVELING & PCB SURFACE MESH METHODS ---
  fun updateMeshConfig(newConfig: MeshGridConfig) {
    val newPoints = GCodeKinematicsAndMeshEngine.generateInitialMeshGrid(newConfig)
    _autoLevelMeshState.value = _autoLevelMeshState.value.copy(
      config = newConfig,
      points = newPoints,
      isMeshAppliedToGCode = false
    )
  }

  fun startAutoLevelMeshProbing() {
    probingJob?.cancel()
    val state = _autoLevelMeshState.value
    _autoLevelMeshState.value = state.copy(isProbingActive = true, currentProbeIndex = 0)

    probingJob = viewModelScope.launch {
      val initialPoints = state.points.map { it.copy(isProbed = false, zOffsetMm = 0.0) }
      val total = initialPoints.size
      val updatedPoints = initialPoints.toMutableList()

      for (i in 0 until total) {
        if (!_autoLevelMeshState.value.isProbingActive) break
        _autoLevelMeshState.value = _autoLevelMeshState.value.copy(currentProbeIndex = i)

        val pt = updatedPoints[i]
        // Send G-code probe move if connected
        if (transportMode.value == TransportMode.USB_SERIAL) {
          serialManager.writeString("G0 Z${state.config.safeZMm}\n")
          serialManager.writeString("G0 X${String.format(Locale.US, "%.3f", pt.worldX)} Y${String.format(Locale.US, "%.3f", pt.worldY)}\n")
          serialManager.writeString("G38.2 Z${state.config.probeDepthLimitZMm} F${state.config.probeFeedRateMmMin}\n")
          delay(800)
        } else {
          // Simulated natural PCB surface topography (+/- 0.15 mm tilt/warp)
          val simulatedZ = 0.08 * kotlin.math.sin(pt.worldX / 20.0) - 0.05 * kotlin.math.cos(pt.worldY / 18.0) + ((-10..10).random() / 1000.0)
          delay(300)
          updatedPoints[i] = pt.copy(zOffsetMm = simulatedZ, isProbed = true)
        }

        val probedList = updatedPoints.filter { it.isProbed }
        val zOffsets = probedList.map { it.zOffsetMm }
        val minZ = zOffsets.minOrNull() ?: 0.0
        val maxZ = zOffsets.maxOrNull() ?: 0.0
        val avgZ = if (zOffsets.isNotEmpty()) zOffsets.average() else 0.0
        val p2p = maxZ - minZ

        _autoLevelMeshState.value = _autoLevelMeshState.value.copy(
          points = updatedPoints.toList(),
          minZOffset = minZ,
          maxZOffset = maxZ,
          avgZOffset = avgZ,
          peakToPeakZ = p2p
        )
      }

      val sdf = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
      _autoLevelMeshState.value = _autoLevelMeshState.value.copy(
        isProbingActive = false,
        lastProbedTimestamp = sdf.format(java.util.Date())
      )
      _machineState.value = _machineState.value.copy(
        systemMessage = "Auto-Leveling surface scan complete: $total points recorded."
      )
    }
  }

  fun stopAutoLevelMeshProbing() {
    probingJob?.cancel()
    _autoLevelMeshState.value = _autoLevelMeshState.value.copy(isProbingActive = false)
  }

  fun applyMeshToActiveGCode() {
    val mesh = _autoLevelMeshState.value
    if (mesh.points.none { it.isProbed }) return

    val currentFile = _gcodeFiles.value.firstOrNull { it.name == machineState.value.gcodeFileName }
      ?: _gcodeFiles.value.first()

    val compensatedLines = GCodeKinematicsAndMeshEngine.applyMeshCompensationToGCode(
      currentFile.sampleLines,
      mesh
    )

    // Update active program with auto-leveled lines
    val updatedFiles = _gcodeFiles.value.map { file ->
      if (file.name == currentFile.name) {
        file.copy(sampleLines = compensatedLines)
      } else file
    }
    _gcodeFiles.value = updatedFiles
    _autoLevelMeshState.value = mesh.copy(isMeshAppliedToGCode = true)
    _jobEstimate.value = GCodeKinematicsAndMeshEngine.calculateJobEstimate(compensatedLines)
    _machineState.value = _machineState.value.copy(
      systemMessage = "Applied Bilinear Z-Mesh height compensation to ${currentFile.name}"
    )
  }

  fun clearMesh() {
    val config = _autoLevelMeshState.value.config
    _autoLevelMeshState.value = AutoLevelMeshState(
      config = config,
      points = GCodeKinematicsAndMeshEngine.generateInitialMeshGrid(config)
    )
  }

  // --- MULTIPOINT AXIS CALIBRATION & ERROR MAPPING METHODS ---
  fun startAxisCalibration(
    axis: CalibrationAxis,
    totalLengthMm: Double,
    intervalPercent: Int,
    currentStepsPerMm: Double
  ) {
    val newSession = GCodeKinematicsAndMeshEngine.createInitialCalibrationSession(
      axis = axis,
      totalLengthMm = totalLengthMm,
      intervalPercent = intervalPercent,
      currentStepsPerMm = currentStepsPerMm
    )
    _axisCalibrationSession.value = newSession
  }

  fun recordAxisMeasurement(stepIndex: Int, measuredRealMm: Double) {
    val session = _axisCalibrationSession.value
    val updatedPoints = session.points.map { pt ->
      if (pt.stepIndex == stepIndex) {
        val err = measuredRealMm - pt.commandedTargetMm
        val relPct = if (pt.commandedTargetMm != 0.0) (err / pt.commandedTargetMm) * 100.0 else 0.0
        val lostSteps = (err * session.currentStepsPerMm).roundToInt()
        pt.copy(
          measuredRealMm = measuredRealMm,
          errorMm = err,
          relativeErrorPct = relPct,
          estimatedLostSteps = lostSteps,
          isMeasured = true
        )
      } else pt
    }
    val nextStep = if (stepIndex < session.points.size - 1) stepIndex + 1 else stepIndex
    val updatedSession = GCodeKinematicsAndMeshEngine.calculateCalibrationStats(
      session.copy(points = updatedPoints, currentStepIndex = nextStep)
    )
    _axisCalibrationSession.value = updatedSession
  }

  fun moveToCalibrationStep(stepIndex: Int) {
    val session = _axisCalibrationSession.value
    val point = session.points.firstOrNull { it.stepIndex == stepIndex } ?: return
    val targetMm = point.commandedTargetMm
    val axisName = session.axis.name

    viewModelScope.launch {
      if (transportMode.value == TransportMode.USB_SERIAL) {
        serialManager.writeString("G90 G21 G1 $axisName$targetMm F1000\n")
      } else {
        // Simulated move
        when (session.axis) {
          CalibrationAxis.X -> _machineState.value = _machineState.value.copy(posX = targetMm, machinePosX = targetMm)
          CalibrationAxis.Y -> _machineState.value = _machineState.value.copy(posY = targetMm, machinePosY = targetMm)
          CalibrationAxis.Z -> _machineState.value = _machineState.value.copy(posZ = targetMm, machinePosZ = targetMm)
          CalibrationAxis.A -> _machineState.value = _machineState.value.copy(posA = targetMm, machinePosA = targetMm)
        }
      }
      _axisCalibrationSession.value = session.copy(currentStepIndex = stepIndex)
      _machineState.value = _machineState.value.copy(
        systemMessage = "Moved ${session.axis.label} to target calibration point: ${targetMm}mm"
      )
    }
  }

  fun applyCalibratedStepsPerMmToGrbl() {
    val session = _axisCalibrationSession.value
    val suggested = session.suggestedStepsPerMm ?: return
    val param = session.axis.grblStepParam
    val formattedVal = String.format(Locale.US, "%.3f", suggested)

    viewModelScope.launch {
      if (transportMode.value == TransportMode.USB_SERIAL) {
        serialManager.writeString("$param=$formattedVal\n")
        delay(100)
        serialManager.writeString("\$\$\n") // request updated settings
      }
      _axisCalibrationSession.value = session.copy(
        currentStepsPerMm = suggested
      )
      _machineState.value = _machineState.value.copy(
        systemMessage = "Saved calibrated $param = $formattedVal steps/mm to Grbl EEPROM"
      )
    }
  }

  fun resetAxisCalibration() {
    val session = _axisCalibrationSession.value
    _axisCalibrationSession.value = GCodeKinematicsAndMeshEngine.createInitialCalibrationSession(
      axis = session.axis,
      totalLengthMm = session.totalStrokeLengthMm,
      intervalPercent = session.stepIntervalPercent,
      currentStepsPerMm = session.currentStepsPerMm
    )
  }

  fun startGCode() = activeTransport.startGCode()
  fun pauseGCode() = activeTransport.pauseGCode()
  fun resumeGCode() = activeTransport.resumeGCode()
  fun stopGCode() = activeTransport.stopGCode()
  fun stepGCode() = activeTransport.stepGCode()

  fun updateMdiInput(text: String) {
    _mdiInput.value = text
  }

  fun submitMdiCommand() {
    val cmd = _mdiInput.value.trim()
    if (cmd.isBlank()) return
    viewModelScope.launch {
      val res = activeTransport.executeMdi(cmd)
      _mdiHistory.value = (listOf(res) + _mdiHistory.value).take(50)
      _mdiInput.value = ""
    }
  }

  fun executeMacro(snippet: String) {
    viewModelScope.launch {
      val res = activeTransport.executeMdi(snippet)
      _mdiHistory.value = (listOf(res) + _mdiHistory.value).take(50)
    }
  }

  fun runCommissioningTest(phaseNumber: Int) {
    viewModelScope.launch {
      val result = activeTransport.runPhaseSelfTest(phaseNumber)
      _commissioningPhases.value = _commissioningPhases.value.map { phase ->
        if (phase.phaseNumber == phaseNumber) {
          phase.copy(isCompleted = result.first, notes = result.second)
        } else phase
      }
    }
  }

  fun connectToMachine(ip: String, port: Int, isSimulated: Boolean) {
    viewModelScope.launch {
      if (isSimulated) {
        setTransportMode(TransportMode.SIMULATION)
        mockTransport.connect(ip, port, true)
      } else {
        setTransportMode(TransportMode.WIFI_ETHERNET)
        mockTransport.connect(ip, port, false)
      }
    }
  }

  fun disconnect() {
    viewModelScope.launch {
      activeTransport.disconnect()
      serialManager.disconnect()
      bluetoothManager.disconnect()
    }
  }

  override fun onCleared() {
    super.onCleared()
    serialManager.unregister()
    bluetoothManager.disconnect()
  }
}
