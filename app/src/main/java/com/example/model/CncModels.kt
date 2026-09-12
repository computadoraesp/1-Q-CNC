package com.example.model

enum class ConnectionStatus {
  DISCONNECTED,
  CONNECTING,
  CONNECTED_WIFI,
  CONNECTED_ETHERNET,
  CONNECTED_USB,
  CONNECTED_BLUETOOTH,
  SIMULATED
}

enum class TransportMode {
  SIMULATION,
  WIFI_ETHERNET,
  USB_SERIAL,
  BLUETOOTH_SERIAL
}

enum class MachineMode {
  MANUAL_JOG,
  AUTO_GCODE,
  MDI,
  HOMING,
  ESTOP_ALARM,
  MAINTENANCE
}

enum class SpindleDirection {
  OFF,
  CW, // M3
  CCW // M4
}

enum class CoolantState {
  OFF,
  FLOOD, // M8
  MIST,  // M7
  BOTH
}

enum class UserRole {
  OPERATOR,
  MAINTENANCE_ENGINEER
}

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
  EN("en", "English", "🇺🇸"),
  ES("es", "Español", "🇪🇸"),
  DE("de", "Deutsch", "🇩🇪"),
  FR("fr", "Français", "🇫🇷"),
  JA("ja", "日本語", "🇯🇵"),
  KO("ko", "한국어", "🇰🇷")
}

data class MachineState(
  val connectionStatus: ConnectionStatus = ConnectionStatus.SIMULATED,
  val ipAddress: String = "192.168.1.150",
  val port: Int = 5005,
  val isEStopActive: Boolean = false,
  val isDriverEnabled: Boolean = true,
  val mode: MachineMode = MachineMode.MANUAL_JOG,
  val userRole: UserRole = UserRole.OPERATOR,
  
  // Coordinates
  val wcsName: String = "G54",
  val posX: Double = 0.000,
  val posY: Double = 0.000,
  val posZ: Double = 15.000,
  val posA: Double = 0.000,
  
  val machinePosX: Double = 0.000,
  val machinePosY: Double = 0.000,
  val machinePosZ: Double = 15.000,
  val machinePosA: Double = 0.000,
  
  val isHomedX: Boolean = true,
  val isHomedY: Boolean = true,
  val isHomedZ: Boolean = true,
  val isHomedA: Boolean = false,
  
  val limitXPos: Boolean = false,
  val limitXNeg: Boolean = false,
  val limitYPos: Boolean = false,
  val limitYNeg: Boolean = false,
  val limitZPos: Boolean = false,
  val limitZNeg: Boolean = false,
  val probeTripped: Boolean = false,
  
  // Spindle & Motion
  val spindleDirection: SpindleDirection = SpindleDirection.OFF,
  val targetRpm: Int = 12000,
  val actualRpm: Int = 0,
  val spindleLoadPct: Float = 0f,
  val coolantState: CoolantState = CoolantState.OFF,
  
  // Overrides
  val feedOverridePct: Int = 100, // 0 - 200%
  val rapidOverridePct: Int = 100, // 25, 50, 100%
  val spindleOverridePct: Int = 100, // 50 - 150%
  
  val feedRateCurrent: Double = 0.0, // mm/min
  val currentToolNumber: Int = 1,
  val activeGCodeLine: Int = 0,
  val totalGCodeLines: Int = 0,
  val gcodeFileName: String = "face_pocket_bracket.ngc",
  val isGcodeRunning: Boolean = false,
  val isGcodePaused: Boolean = false,
  val gcodeProgressPct: Float = 0f,
  
  val activeAlarms: List<String> = emptyList(),
  val systemMessage: String = "LinuxCNC Core & STM32 Motion ready."
)

data class JogConfig(
  val isIncremental: Boolean = true,
  val stepIncrementMm: Double = 1.0, // 0.001, 0.01, 0.1, 1.0, 10.0
  val jogSpeedMmMin: Double = 1200.0,
  val isHighSpeedRapid: Boolean = false
)

data class ToolpathPoint(
  val x: Float,
  val y: Float,
  val z: Float,
  val isRapid: Boolean = false,
  val isArc: Boolean = false,
  val lineNumber: Int = 0
)

data class GCodeFileItem(
  val name: String,
  val sizeBytes: Long,
  val lineCount: Int,
  val estimatedTimeMinutes: Double,
  val sampleLines: List<String>,
  val toolpathPoints: List<ToolpathPoint>
)

data class MdiHistoryItem(
  val command: String,
  val timestamp: String,
  val isSuccess: Boolean,
  val responseText: String
)

data class MacroPreset(
  val id: String,
  val label: String,
  val description: String,
  val gcodeSnippet: String,
  val iconName: String
)

data class HalPin(
  val name: String,
  val type: String, // bit, float, s32, u32
  val value: String,
  val direction: String, // IN / OUT / IO
  val component: String // stm32-stepgen, motion, iocontrol
)

data class McuDiagnostics(
  val watchdogHealthy: Boolean = true,
  val watchdogHeartbeatMs: Long = 12,
  val stepFrequencyMaxKhz: Float = 250.0f,
  val stepFrequencyCurrentKhz: Float = 0.0f,
  val worstLatencyUs: Float = 14.8f,
  val avgJitterUs: Float = 2.1f,
  val mcuTemperatureC: Float = 38.5f,
  val linuxCpuLoadPct: Float = 18.2f,
  val linuxRamUsedMb: Int = 412,
  val linuxRamTotalMb: Int = 2048,
  val linuxTempC: Float = 44.0f,
  val packetsRx: Long = 14820,
  val packetsTx: Long = 14820,
  val packetLossPct: Float = 0.0f,
  val bridgeRpcState: String = "SYNCED_PREEMPT_RT"
)

data class ToolItem(
  val toolNumber: Int,
  val diameterMm: Double,
  val lengthOffsetMm: Double,
  val wearMm: Double,
  val toolType: String,
  val description: String
)

data class WcsOffset(
  val code: String, // G54..G59.3
  val name: String,
  val offsetX: Double,
  val offsetY: Double,
  val offsetZ: Double,
  val offsetA: Double,
  val isActive: Boolean = false
)

data class CommissioningPhase(
  val phaseNumber: Int,
  val title: String,
  val description: String,
  val passCriteria: String,
  val isCompleted: Boolean,
  val testActionName: String?,
  val notes: String
)

enum class SerialDirection {
  TX, RX, INFO, ERROR
}

enum class SerialParity(val displayName: String, val code: Int) {
  NONE("None (8N1)", 0),
  ODD("Odd (8O1)", 1),
  EVEN("Even (8E1)", 2),
  MARK("Mark", 3),
  SPACE("Space", 4)
}

enum class SerialStopBits(val displayName: String, val code: Int) {
  ONE("1 Stop Bit", 0),
  ONE_POINT_FIVE("1.5 Stop Bits", 1),
  TWO("2 Stop Bits", 2)
}

enum class SerialFlowControl(val displayName: String) {
  NONE("None"),
  RTS_CTS("RTS / CTS (Hardware)"),
  DTR_DSR("DTR / DSR"),
  XON_XOFF("XON / XOFF (Software)")
}

data class SerialDeviceInfo(
  val deviceName: String = "usb_port",
  val deviceId: Int = 0,
  val vendorId: Int = 0,
  val productId: Int = 0,
  val manufacturerName: String = "",
  val productName: String = "",
  val serialNumber: String = "",
  val driverType: String = "CDC-ACM",
  val interfaceCount: Int = 1,
  val isKnownCncHardware: Boolean = false
)

data class SerialPortConfig(
  val baudRate: Int = 115200,
  val dataBits: Int = 8,
  val stopBits: SerialStopBits = SerialStopBits.ONE,
  val parity: SerialParity = SerialParity.NONE,
  val flowControl: SerialFlowControl = SerialFlowControl.NONE,
  val dtrEnable: Boolean = true,
  val rtsEnable: Boolean = true,
  val readTimeoutMs: Int = 100,
  val writeTimeoutMs: Int = 500
)

data class SerialLinkStats(
  val bytesSent: Long = 0,
  val bytesReceived: Long = 0,
  val packetsSent: Long = 0,
  val packetsReceived: Long = 0,
  val crcErrors: Long = 0,
  val framingErrors: Long = 0,
  val roundTripLatencyMs: Float = 0f,
  val lastHeartbeatTimestamp: Long = 0,
  val isLinkActive: Boolean = false,
  val connectedDeviceSummary: String = "No serial device connected"
)

data class SerialLogEntry(
  val id: Long = System.nanoTime(),
  val timestamp: String,
  val direction: SerialDirection,
  val text: String,
  val hexRepresentation: String = ""
)

data class BluetoothDeviceInfo(
  val name: String,
  val address: String,
  val isPaired: Boolean = true,
  val isConnected: Boolean = false,
  val deviceType: String = "Classic SPP (HC-05/ESP32)"
)

// --- JOB TIME & KINEMATIC ESTIMATE MODELS ---
data class JobTimeEstimate(
  val totalTimeSeconds: Double = 0.0,
  val rapidTimeSeconds: Double = 0.0,
  val cutTimeSeconds: Double = 0.0,
  val dwellTimeSeconds: Double = 0.0,
  val rapidDistanceMm: Double = 0.0,
  val cutDistanceMm: Double = 0.0,
  val totalDistanceMm: Double = 0.0,
  val axisDistanceX: Double = 0.0,
  val axisDistanceY: Double = 0.0,
  val axisDistanceZ: Double = 0.0,
  val axisDistanceA: Double = 0.0,
  val minFeedRate: Double = 0.0,
  val maxFeedRate: Double = 0.0,
  val toolChangeCount: Int = 0,
  val totalLines: Int = 0,
  val cutLinesCount: Int = 0,
  val rapidLinesCount: Int = 0,
  val boundsMinX: Double = 0.0,
  val boundsMaxX: Double = 0.0,
  val boundsMinY: Double = 0.0,
  val boundsMaxY: Double = 0.0,
  val boundsMinZ: Double = 0.0,
  val boundsMaxZ: Double = 0.0
) {
  val formattedTotalTime: String
    get() {
      val hours = (totalTimeSeconds / 3600).toInt()
      val minutes = ((totalTimeSeconds % 3600) / 60).toInt()
      val seconds = (totalTimeSeconds % 60).toInt()
      return if (hours > 0) {
        String.format(java.util.Locale.US, "%dh %02dm %02ds", hours, minutes, seconds)
      } else {
        String.format(java.util.Locale.US, "%02dm %02ds", minutes, seconds)
      }
    }
  val formattedCutTime: String
    get() {
      val minutes = (cutTimeSeconds / 60).toInt()
      val seconds = (cutTimeSeconds % 60).toInt()
      return String.format(java.util.Locale.US, "%02dm %02ds", minutes, seconds)
    }
  val formattedRapidTime: String
    get() {
      val minutes = (rapidTimeSeconds / 60).toInt()
      val seconds = (rapidTimeSeconds % 60).toInt()
      return String.format(java.util.Locale.US, "%02dm %02ds", minutes, seconds)
    }
}

// --- PCB AUTO-LEVELING & SURFACE MESH MODELS ---
data class MeshGridPoint(
  val gridX: Int,
  val gridY: Int,
  val worldX: Double,
  val worldY: Double,
  val zOffsetMm: Double = 0.0,
  val isProbed: Boolean = false
)

data class MeshGridConfig(
  val xMin: Double = 0.0,
  val xMax: Double = 100.0,
  val yMin: Double = 0.0,
  val yMax: Double = 75.0,
  val gridCols: Int = 5, // X division points
  val gridRows: Int = 4, // Y division points
  val probeFeedRateMmMin: Double = 150.0,
  val safeZMm: Double = 4.0,
  val probeDepthLimitZMm: Double = -3.0
) {
  val totalPoints: Int get() = gridCols * gridRows
  val stepX: Double get() = if (gridCols > 1) (xMax - xMin) / (gridCols - 1) else 0.0
  val stepY: Double get() = if (gridRows > 1) (yMax - yMin) / (gridRows - 1) else 0.0
}

data class AutoLevelMeshState(
  val config: MeshGridConfig = MeshGridConfig(),
  val points: List<MeshGridPoint> = emptyList(),
  val isProbingActive: Boolean = false,
  val currentProbeIndex: Int = 0,
  val minZOffset: Double = 0.0,
  val maxZOffset: Double = 0.0,
  val avgZOffset: Double = 0.0,
  val peakToPeakZ: Double = 0.0,
  val isMeshAppliedToGCode: Boolean = false,
  val lastProbedTimestamp: String? = null
)

// --- MULTIPOINT AXIS CALIBRATION & ERROR MAPPING MODELS ---
enum class CalibrationAxis(val label: String, val grblStepParam: String, val defaultStepsPerMm: Double) {
  X("Eje X", "$100", 80.0),
  Y("Eje Y", "$101", 80.0),
  Z("Eje Z", "$102", 400.0),
  A("Eje A (Rotary)", "$103", 26.667)
}

data class CalibrationPoint(
  val stepIndex: Int,
  val percentOfTravel: Int, // e.g. 0%, 10%, 20%, ..., 100%
  val commandedTargetMm: Double, // Valor teórico ordenado
  val measuredRealMm: Double? = null, // Medición del instrumento
  val errorMm: Double? = null, // Real - Teórico
  val relativeErrorPct: Double? = null,
  val estimatedLostSteps: Int? = null,
  val isMeasured: Boolean = false
)

data class AxisCalibrationSession(
  val axis: CalibrationAxis = CalibrationAxis.Y,
  val totalStrokeLengthMm: Double = 300.0,
  val stepIntervalPercent: Int = 10, // cada 10%
  val currentStepIndex: Int = 0,
  val currentStepsPerMm: Double = 80.0,
  val points: List<CalibrationPoint> = emptyList(),
  val suggestedStepsPerMm: Double? = null,
  val maxDeviationMm: Double = 0.0,
  val avgDeviationMm: Double = 0.0,
  val stdDeviationMm: Double = 0.0,
  val backlashEstimatedMm: Double = 0.0,
  val isCalibrationComplete: Boolean = false
)

