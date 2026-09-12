package com.example.transport

import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*
import kotlin.random.Random

class MockCncEngine(
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : CncTransport {

  private val _machineState = MutableStateFlow(MachineState())
  override val machineState: StateFlow<MachineState> = _machineState.asStateFlow()

  private val _mcuDiagnostics = MutableStateFlow(McuDiagnostics())
  override val mcuDiagnostics: StateFlow<McuDiagnostics> = _mcuDiagnostics.asStateFlow()

  private val _halPins = MutableStateFlow<List<HalPin>>(emptyList())
  override val halPins: StateFlow<List<HalPin>> = _halPins.asStateFlow()

  private var simulationJob: Job? = null
  private var gcodeExecutionJob: Job? = null

  private var activeToolpathPoints: List<ToolpathPoint> = generateDefaultSampleToolpath()
  private var currentPathIndex = 0

  init {
    initHalPins()
    startTelemetrySimulation()
  }

  private fun initHalPins() {
    _halPins.value = listOf(
      HalPin("axis.x.is-homed", "bit", "TRUE", "OUT", "motion"),
      HalPin("axis.x.pos-cmd", "float", "0.0000", "OUT", "motion"),
      HalPin("axis.x.pos-fb", "float", "0.0000", "IN", "motion"),
      HalPin("axis.y.is-homed", "bit", "TRUE", "OUT", "motion"),
      HalPin("axis.y.pos-cmd", "float", "0.0000", "OUT", "motion"),
      HalPin("axis.z.is-homed", "bit", "TRUE", "OUT", "motion"),
      HalPin("axis.z.pos-cmd", "float", "15.0000", "OUT", "motion"),
      HalPin("stm32.stepgen.0.enable", "bit", "TRUE", "OUT", "stm32-stepgen"),
      HalPin("stm32.stepgen.0.freq-khz", "float", "0.0", "IN", "stm32-stepgen"),
      HalPin("stm32.watchdog.pet", "bit", "TRUE", "OUT", "stm32-stepgen"),
      HalPin("stm32.watchdog.ok", "bit", "TRUE", "IN", "stm32-stepgen"),
      HalPin("motion.spindle-on", "bit", "FALSE", "OUT", "motion"),
      HalPin("motion.spindle-speed-cmd", "float", "12000.0", "OUT", "motion"),
      HalPin("motion.spindle-speed-fb", "float", "0.0", "IN", "motion"),
      HalPin("iocontrol.0.coolant-flood", "bit", "FALSE", "OUT", "iocontrol"),
      HalPin("iocontrol.0.coolant-mist", "bit", "FALSE", "OUT", "iocontrol"),
      HalPin("iocontrol.0.emc-enable-in", "bit", "TRUE", "IN", "iocontrol"),
      HalPin("iocontrol.0.user-enable-out", "bit", "TRUE", "OUT", "iocontrol"),
      HalPin("motion.probe-input", "bit", "FALSE", "IN", "motion"),
      HalPin("hal_bb_gpio.pin-x-lim-pos", "bit", "FALSE", "IN", "gpio"),
      HalPin("hal_bb_gpio.pin-y-lim-pos", "bit", "FALSE", "IN", "gpio"),
      HalPin("hal_bb_gpio.pin-z-lim-pos", "bit", "FALSE", "IN", "gpio")
    )
  }

  var isEcoMode: Boolean = false

  private fun startTelemetrySimulation() {
    simulationJob?.cancel()
    simulationJob = coroutineScope.launch {
      var counter = 0
      while (isActive) {
        val state = _machineState.value
        val isMoving = state.isGcodeRunning || state.feedRateCurrent > 0
        val isSpindleActive = state.spindleDirection != SpindleDirection.OFF && state.actualRpm > 0

        // Dynamic battery & resource throttling:
        // Idle + Eco: 650ms (drastic CPU & battery savings)
        // Idle normal: 350ms (avoids burning CPU when machine is standing still)
        // Active/Moving + Eco: 200ms
        // Active/Moving normal: 120ms
        val loopDelay = when {
          isEcoMode && !isMoving && !isSpindleActive -> 650L
          isEcoMode -> 200L
          !isMoving && !isSpindleActive -> 350L
          else -> 120L
        }
        delay(loopDelay)
        counter++
        
        // Jitter and telemetry updates
        val currentDiag = _mcuDiagnostics.value
        val jitterNoise = Random.nextFloat() * 0.8f - 0.4f
        val tempNoise = Random.nextFloat() * 0.4f - 0.2f
        
        val newStepFreq = if (isMoving) {
          (50f + Random.nextFloat() * 80f)
        } else {
          0.0f
        }

        _mcuDiagnostics.value = currentDiag.copy(
          watchdogHeartbeatMs = 10 + (counter % 5).toLong(),
          worstLatencyUs = max(14.8f, 14.8f + jitterNoise),
          avgJitterUs = max(1.2f, 2.1f + jitterNoise * 0.5f),
          mcuTemperatureC = (38.5f + tempNoise).coerceIn(35f, 50f),
          linuxCpuLoadPct = if (isMoving) (28.4f + Random.nextFloat() * 6f) else (14.2f + Random.nextFloat() * 4f),
          linuxTempC = (44.0f + tempNoise).coerceIn(40f, 60f),
          stepFrequencyCurrentKhz = newStepFreq,
          packetsRx = currentDiag.packetsRx + 1,
          packetsTx = currentDiag.packetsTx + 1
        )

        // Spindle RPM ramping simulation
        if (state.spindleDirection != SpindleDirection.OFF && state.isDriverEnabled && !state.isEStopActive) {
          val targetWithOverride = (state.targetRpm * (state.spindleOverridePct / 100f)).roundToInt()
          val currentRpm = state.actualRpm
          val delta = (targetWithOverride - currentRpm) * 0.35f
          val nextRpm = if (abs(targetWithOverride - currentRpm) < 50) targetWithOverride else (currentRpm + delta).roundToInt()
          val load = if (nextRpm > 0) (0.25f + (sin(counter * 0.2) * 0.15f).toFloat()).coerceIn(0.05f, 0.95f) else 0f
          _machineState.value = state.copy(actualRpm = nextRpm, spindleLoadPct = load)
        } else if (state.actualRpm > 0) {
          val nextRpm = (state.actualRpm * 0.7f).roundToInt().coerceAtLeast(0)
          _machineState.value = state.copy(actualRpm = nextRpm, spindleLoadPct = 0f)
        }
      }
    }
  }

  override suspend fun connect(ip: String, port: Int, useSimulation: Boolean) {
    _machineState.value = _machineState.value.copy(
      connectionStatus = ConnectionStatus.CONNECTING,
      ipAddress = ip,
      port = port
    )
    delay(500)
    _machineState.value = _machineState.value.copy(
      connectionStatus = if (useSimulation) ConnectionStatus.SIMULATED else ConnectionStatus.CONNECTED_WIFI,
      systemMessage = if (useSimulation) "Connected to Virtual Arduino UNO Q Simulation." else "Connected to LinuxCNC on $ip:$port"
    )
  }

  override suspend fun disconnect() {
    gcodeExecutionJob?.cancel()
    _machineState.value = _machineState.value.copy(
      connectionStatus = ConnectionStatus.DISCONNECTED,
      isGcodeRunning = false,
      isGcodePaused = false,
      systemMessage = "Disconnected from machine controller."
    )
  }

  override fun sendJog(axis: String, stepMm: Double, speedMmMin: Double) {
    val state = _machineState.value
    if (state.isEStopActive || !state.isDriverEnabled) return

    val newX = if (axis.equals("X", true)) (state.posX + stepMm).round(3) else state.posX
    val newY = if (axis.equals("Y", true)) (state.posY + stepMm).round(3) else state.posY
    val newZ = if (axis.equals("Z", true)) (state.posZ + stepMm).round(3) else state.posZ
    val newA = if (axis.equals("A", true)) (state.posA + stepMm).round(3) else state.posA

    _machineState.value = state.copy(
      posX = newX,
      posY = newY,
      posZ = newZ,
      posA = newA,
      machinePosX = newX,
      machinePosY = newY,
      machinePosZ = newZ,
      machinePosA = newA,
      feedRateCurrent = speedMmMin,
      systemMessage = "Jog $axis by ${if (stepMm >= 0) "+$stepMm" else stepMm} mm at $speedMmMin mm/min"
    )

    updateHalPosition(newX, newY, newZ)
  }

  override fun stopJog(axis: String) {
    _machineState.value = _machineState.value.copy(
      feedRateCurrent = 0.0
    )
  }

  override fun homeAxis(axis: String) {
    val state = _machineState.value
    when (axis.uppercase()) {
      "ALL" -> {
        _machineState.value = state.copy(
          posX = 0.0, posY = 0.0, posZ = 25.0, posA = 0.0,
          machinePosX = 0.0, machinePosY = 0.0, machinePosZ = 25.0, machinePosA = 0.0,
          isHomedX = true, isHomedY = true, isHomedZ = true, isHomedA = true,
          systemMessage = "All Axes Homed successfully (G28 Reference reached)."
        )
      }
      "X" -> _machineState.value = state.copy(posX = 0.0, machinePosX = 0.0, isHomedX = true, systemMessage = "X-Axis Homed.")
      "Y" -> _machineState.value = state.copy(posY = 0.0, machinePosY = 0.0, isHomedY = true, systemMessage = "Y-Axis Homed.")
      "Z" -> _machineState.value = state.copy(posZ = 25.0, machinePosZ = 25.0, isHomedZ = true, systemMessage = "Z-Axis Homed to safe height.")
      "A" -> _machineState.value = state.copy(posA = 0.0, machinePosA = 0.0, isHomedA = true, systemMessage = "A-Axis Homed.")
    }
  }

  override fun zeroWorkCoordinate(axis: String) {
    val state = _machineState.value
    when (axis.uppercase()) {
      "ALL" -> _machineState.value = state.copy(posX = 0.0, posY = 0.0, posZ = 0.0, posA = 0.0, systemMessage = "Work Coordinates X/Y/Z Zeroed in ${state.wcsName}")
      "X" -> _machineState.value = state.copy(posX = 0.0, systemMessage = "X Zeroed in ${state.wcsName}")
      "Y" -> _machineState.value = state.copy(posY = 0.0, systemMessage = "Y Zeroed in ${state.wcsName}")
      "Z" -> _machineState.value = state.copy(posZ = 0.0, systemMessage = "Z Zeroed in ${state.wcsName} (Touch-off)")
      "A" -> _machineState.value = state.copy(posA = 0.0, systemMessage = "A Zeroed in ${state.wcsName}")
    }
  }

  override fun setWorkCoordinateSystem(wcsCode: String) {
    _machineState.value = _machineState.value.copy(
      wcsName = wcsCode,
      systemMessage = "Active Coordinate System set to $wcsCode"
    )
  }

  override fun setSpindle(direction: SpindleDirection, targetRpm: Int) {
    _machineState.value = _machineState.value.copy(
      spindleDirection = direction,
      targetRpm = targetRpm,
      systemMessage = if (direction == SpindleDirection.OFF) "Spindle Stopped (M5)." else "Spindle Started $direction @ $targetRpm RPM (M3/M4)."
    )
  }

  override fun setCoolant(state: CoolantState) {
    _machineState.value = _machineState.value.copy(
      coolantState = state,
      systemMessage = "Coolant set to: $state"
    )
  }

  override fun setFeedOverride(pct: Int) {
    _machineState.value = _machineState.value.copy(feedOverridePct = pct.coerceIn(0, 200))
  }

  override fun setRapidOverride(pct: Int) {
    _machineState.value = _machineState.value.copy(rapidOverridePct = pct)
  }

  override fun setSpindleOverride(pct: Int) {
    _machineState.value = _machineState.value.copy(spindleOverridePct = pct.coerceIn(50, 150))
  }

  override fun triggerEStop() {
    gcodeExecutionJob?.cancel()
    _machineState.value = _machineState.value.copy(
      isEStopActive = true,
      isDriverEnabled = false,
      spindleDirection = SpindleDirection.OFF,
      actualRpm = 0,
      feedRateCurrent = 0.0,
      isGcodeRunning = false,
      isGcodePaused = false,
      mode = MachineMode.ESTOP_ALARM,
      activeAlarms = _machineState.value.activeAlarms + "HARDWARE E-STOP TRIPPED (Watchdog interlock open)",
      systemMessage = "EMERGENCY STOP TRIGGERED! Drivers disabled."
    )
  }

  override fun resetEStop() {
    _machineState.value = _machineState.value.copy(
      isEStopActive = false,
      isDriverEnabled = true,
      mode = MachineMode.MANUAL_JOG,
      activeAlarms = emptyList(),
      systemMessage = "E-Stop Reset. Drivers re-enabled and machine ready."
    )
  }

  override fun setDriverEnabled(enabled: Boolean) {
    _machineState.value = _machineState.value.copy(
      isDriverEnabled = enabled,
      systemMessage = if (enabled) "Motor Drivers Enabled (STEP/DIR charge pump active)." else "Motor Drivers Disabled (Freewheel mode)."
    )
  }

  override fun clearAlarms() {
    _machineState.value = _machineState.value.copy(
      activeAlarms = emptyList(),
      systemMessage = "Alarms cleared."
    )
  }

  override fun loadGCodeProgram(fileName: String) {
    currentPathIndex = 0
    _machineState.value = _machineState.value.copy(
      gcodeFileName = fileName,
      activeGCodeLine = 1,
      totalGCodeLines = activeToolpathPoints.size,
      gcodeProgressPct = 0f,
      isGcodeRunning = false,
      isGcodePaused = false,
      systemMessage = "Loaded program $fileName (${activeToolpathPoints.size} blocks)."
    )
  }

  override fun startGCode() {
    val state = _machineState.value
    if (state.isEStopActive || !state.isDriverEnabled) {
      _machineState.value = state.copy(systemMessage = "Cannot start: E-Stop active or Drivers disabled.")
      return
    }

    _machineState.value = state.copy(
      isGcodeRunning = true,
      isGcodePaused = false,
      mode = MachineMode.AUTO_GCODE,
      spindleDirection = SpindleDirection.CW,
      systemMessage = "Executing G-Code: ${state.gcodeFileName}"
    )

    startGCodeLoop()
  }

  override fun pauseGCode() {
    _machineState.value = _machineState.value.copy(
      isGcodeRunning = false,
      isGcodePaused = true,
      feedRateCurrent = 0.0,
      systemMessage = "FEED HOLD (Program Paused at line ${_machineState.value.activeGCodeLine})."
    )
  }

  override fun resumeGCode() {
    _machineState.value = _machineState.value.copy(
      isGcodeRunning = true,
      isGcodePaused = false,
      systemMessage = "Resuming execution (Cycle Start)."
    )
    startGCodeLoop()
  }

  override fun stopGCode() {
    gcodeExecutionJob?.cancel()
    _machineState.value = _machineState.value.copy(
      isGcodeRunning = false,
      isGcodePaused = false,
      mode = MachineMode.MANUAL_JOG,
      feedRateCurrent = 0.0,
      systemMessage = "Program stopped (M2 / M30 Program End)."
    )
  }

  override fun stepGCode() {
    if (currentPathIndex < activeToolpathPoints.size) {
      val pt = activeToolpathPoints[currentPathIndex]
      currentPathIndex++
      val pct = (currentPathIndex.toFloat() / activeToolpathPoints.size.toFloat())
      _machineState.value = _machineState.value.copy(
        posX = pt.x.toDouble().round(3),
        posY = pt.y.toDouble().round(3),
        posZ = pt.z.toDouble().round(3),
        activeGCodeLine = currentPathIndex,
        gcodeProgressPct = pct,
        feedRateCurrent = if (pt.isRapid) 3000.0 else 1200.0,
        systemMessage = "Single-step G-Code block $currentPathIndex: X${pt.x} Y${pt.y} Z${pt.z}"
      )
      updateHalPosition(pt.x.toDouble(), pt.y.toDouble(), pt.z.toDouble())
    }
  }

  private fun startGCodeLoop() {
    gcodeExecutionJob?.cancel()
    gcodeExecutionJob = coroutineScope.launch {
      while (isActive && _machineState.value.isGcodeRunning) {
        val delayTime = (120L * (100f / _machineState.value.feedOverridePct.coerceAtLeast(10))).toLong()
        delay(delayTime)

        if (currentPathIndex >= activeToolpathPoints.size) {
          // Program complete
          _machineState.value = _machineState.value.copy(
            isGcodeRunning = false,
            isGcodePaused = false,
            mode = MachineMode.MANUAL_JOG,
            gcodeProgressPct = 1.0f,
            feedRateCurrent = 0.0,
            systemMessage = "Program Completed Successfully! (M30 - Rewound to top)"
          )
          currentPathIndex = 0
          break
        }

        val pt = activeToolpathPoints[currentPathIndex]
        currentPathIndex++
        val pct = (currentPathIndex.toFloat() / activeToolpathPoints.size.toFloat())

        _machineState.value = _machineState.value.copy(
          posX = pt.x.toDouble().round(3),
          posY = pt.y.toDouble().round(3),
          posZ = pt.z.toDouble().round(3),
          activeGCodeLine = currentPathIndex,
          gcodeProgressPct = pct,
          feedRateCurrent = if (pt.isRapid) 3600.0 else (1500.0 * (_machineState.value.feedOverridePct / 100.0))
        )
        updateHalPosition(pt.x.toDouble(), pt.y.toDouble(), pt.z.toDouble())
      }
    }
  }

  override suspend fun executeMdi(command: String): MdiHistoryItem {
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    val upper = command.trim().uppercase()

    if (_machineState.value.isEStopActive) {
      return MdiHistoryItem(command, timeStr, false, "ERR: Machine is in E-Stop state.")
    }

    delay(200)

    // Handle common MDI commands
    when {
      upper.startsWith("G0") || upper.startsWith("G00") || upper.startsWith("G1") || upper.startsWith("G01") -> {
        parseAndMoveGcode(upper)
        return MdiHistoryItem(command, timeStr, true, "OK (Motion executed to target coordinates)")
      }
      upper.startsWith("G28") || upper.startsWith("G30") -> {
        homeAxis("ALL")
        return MdiHistoryItem(command, timeStr, true, "OK (Moved to Home/Reference point)")
      }
      upper.startsWith("G92") -> {
        zeroWorkCoordinate("ALL")
        return MdiHistoryItem(command, timeStr, true, "OK (G92 Temporary Coordinate offset applied)")
      }
      upper.startsWith("G54") || upper.startsWith("G55") || upper.startsWith("G56") || upper.startsWith("G57") -> {
        setWorkCoordinateSystem(upper.substring(0, 3))
        return MdiHistoryItem(command, timeStr, true, "OK (Switched to $upper)")
      }
      upper.startsWith("M3") || upper.startsWith("M03") -> {
        val rpm = parseRpm(upper) ?: 12000
        setSpindle(SpindleDirection.CW, rpm)
        return MdiHistoryItem(command, timeStr, true, "OK (Spindle CW at $rpm RPM)")
      }
      upper.startsWith("M4") || upper.startsWith("M04") -> {
        val rpm = parseRpm(upper) ?: 12000
        setSpindle(SpindleDirection.CCW, rpm)
        return MdiHistoryItem(command, timeStr, true, "OK (Spindle CCW at $rpm RPM)")
      }
      upper.startsWith("M5") || upper.startsWith("M05") -> {
        setSpindle(SpindleDirection.OFF, 0)
        return MdiHistoryItem(command, timeStr, true, "OK (Spindle Stopped)")
      }
      upper.startsWith("M8") -> {
        setCoolant(CoolantState.FLOOD)
        return MdiHistoryItem(command, timeStr, true, "OK (Flood Coolant ON)")
      }
      upper.startsWith("M9") -> {
        setCoolant(CoolantState.OFF)
        return MdiHistoryItem(command, timeStr, true, "OK (Coolant OFF)")
      }
      upper.startsWith("T") && upper.contains("M6") -> {
        val toolNum = upper.filter { it.isDigit() }.toIntOrNull() ?: 1
        _machineState.value = _machineState.value.copy(currentToolNumber = toolNum)
        return MdiHistoryItem(command, timeStr, true, "OK (Tool changed to T$toolNum)")
      }
      else -> {
        return MdiHistoryItem(command, timeStr, true, "OK (Command parsed and accepted by LinuxCNC HAL)")
      }
    }
  }

  private fun parseAndMoveGcode(cmd: String) {
    val xMatch = Regex("""X(-?\d+\.?\d*)""").find(cmd)?.groupValues?.get(1)?.toDoubleOrNull()
    val yMatch = Regex("""Y(-?\d+\.?\d*)""").find(cmd)?.groupValues?.get(1)?.toDoubleOrNull()
    val zMatch = Regex("""Z(-?\d+\.?\d*)""").find(cmd)?.groupValues?.get(1)?.toDoubleOrNull()
    val state = _machineState.value
    val newX = xMatch ?: state.posX
    val newY = yMatch ?: state.posY
    val newZ = zMatch ?: state.posZ
    _machineState.value = state.copy(posX = newX, posY = newY, posZ = newZ)
    updateHalPosition(newX, newY, newZ)
  }

  private fun parseRpm(cmd: String): Int? {
    return Regex("""S(\d+)""").find(cmd)?.groupValues?.get(1)?.toIntOrNull()
  }

  private fun updateHalPosition(x: Double, y: Double, z: Double) {
    val currentList = _halPins.value.toMutableList()
    val idxX = currentList.indexOfFirst { it.name == "axis.x.pos-cmd" }
    if (idxX >= 0) currentList[idxX] = currentList[idxX].copy(value = String.format(Locale.US, "%.4f", x))
    val idxY = currentList.indexOfFirst { it.name == "axis.y.pos-cmd" }
    if (idxY >= 0) currentList[idxY] = currentList[idxY].copy(value = String.format(Locale.US, "%.4f", y))
    val idxZ = currentList.indexOfFirst { it.name == "axis.z.pos-cmd" }
    if (idxZ >= 0) currentList[idxZ] = currentList[idxZ].copy(value = String.format(Locale.US, "%.4f", z))
    _halPins.value = currentList
  }

  override suspend fun runPhaseSelfTest(phaseNumber: Int): Pair<Boolean, String> {
    delay(400)
    return when (phaseNumber) {
      0 -> Pair(true, "Machine specification registered: XYZ Travel (400x300x120mm), NEMA 23, Leadshine DM542, 2.2kW ER20 Spindle.")
      1 -> Pair(true, "UNO Q Debian ARM64 & STM32U585 RPC link operational. Bridge latency: 0.8ms.")
      2 -> Pair(true, "LinuxCNC 2.9.2 ARM64 installed, XYZ HAL & INI generated and verified in simulation.")
      3 -> Pair(true, "PREEMPT_RT kernel validated. Measured worst latency: 14.8µs (< 25µs requirement).")
      4 -> Pair(true, "STM32/Zephyr deterministic STEP/DIR generator selected. Max pulse rate: 250 kHz.")
      5 -> Pair(true, "3-Channel STEP/DIR timing test passed. Zero lost steps detected during 100k pulse ramp.")
      6 -> Pair(true, "UNO Q CNC REST/WebSocket API endpoints verified with full telemetry synchronization.")
      7 -> Pair(true, "Hardware E-Stop loop & Watchdog verified: Motor disable within 1.2ms of signal drop.")
      8 -> Pair(true, "Android Jetpack Compose UI architecture linked to CNC Transport layer.")
      9 -> Pair(true, "XYZ Jogging, Homing, Spindle RPM, and Feed Overrides functional.")
      10 -> Pair(true, "G-Code parser, 3D path visualizer, and cycle execution engine operational.")
      11 -> Pair(true, "Live HAL inspector and STM32 jitter telemetry stream verified.")
      12 -> Pair(true, "Ethernet (Primary) / Wi-Fi / USB CDC failover protocol verified.")
      13 -> Pair(true, "Operator vs Maintenance Engineer role access controls validated.")
      14 -> Pair(true, "End-to-End integration test successful (Android ↔ UNO Q Linux ↔ STM32 ↔ CNC Drivers).")
      15 -> Pair(true, "Commissioning dry run and air cut completed without alarm triggers.")
      16 -> Pair(true, "Performance verified: 250 kHz STEP rate, 14.8µs latency, CPU load 18.2%, Temp 38.5°C.")
      else -> Pair(true, "Self-test passed.")
    }
  }

  private fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return (this * multiplier).roundToInt() / multiplier
  }

  companion object {
    fun generateDefaultSampleToolpath(): List<ToolpathPoint> {
      val points = mutableListOf<ToolpathPoint>()
      var line = 1
      
      // Start with rapid to safe height
      points.add(ToolpathPoint(0f, 0f, 15f, isRapid = true, lineNumber = line++))
      points.add(ToolpathPoint(10f, 10f, 15f, isRapid = true, lineNumber = line++))
      points.add(ToolpathPoint(10f, 10f, 1f, isRapid = true, lineNumber = line++))
      
      // Plunge
      points.add(ToolpathPoint(10f, 10f, -2f, isRapid = false, lineNumber = line++))
      
      // Outer profile (Pocket milling simulation)
      points.add(ToolpathPoint(90f, 10f, -2f, isRapid = false, lineNumber = line++))
      points.add(ToolpathPoint(90f, 60f, -2f, isRapid = false, lineNumber = line++))
      points.add(ToolpathPoint(10f, 60f, -2f, isRapid = false, lineNumber = line++))
      points.add(ToolpathPoint(10f, 10f, -2f, isRapid = false, lineNumber = line++))
      
      // Inner spiral pocket
      for (r in 5..35 step 5) {
        val radius = r.toFloat()
        val cx = 50f
        val cy = 35f
        for (deg in 0..360 step 30) {
          val rad = Math.toRadians(deg.toDouble()).toFloat()
          val px = cx + radius * cos(rad)
          val py = cy + radius * sin(rad)
          points.add(ToolpathPoint(px, py, -2.5f, isRapid = false, lineNumber = line++))
        }
      }
      
      // Retract
      points.add(ToolpathPoint(50f, 35f, 15f, isRapid = true, lineNumber = line++))
      points.add(ToolpathPoint(0f, 0f, 15f, isRapid = true, lineNumber = line++))
      
      return points
    }
  }
}
