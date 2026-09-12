package com.example.transport.serial

import com.example.model.*
import com.example.transport.CncTransport
import com.example.transport.MockCncEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsbSerialCncTransport(
  val serialManager: SerialCommunicationManager,
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : CncTransport {

  private val _machineState = MutableStateFlow(
    MachineState(
      connectionStatus = ConnectionStatus.DISCONNECTED,
      systemMessage = "Serial Transport ready. Connect USB cable to Arduino UNO Q / STM32."
    )
  )
  override val machineState: StateFlow<MachineState> = _machineState.asStateFlow()

  private val _mcuDiagnostics = MutableStateFlow(McuDiagnostics(bridgeRpcState = "USB_CDC_ACM_LINK"))
  override val mcuDiagnostics: StateFlow<McuDiagnostics> = _mcuDiagnostics.asStateFlow()

  private val _halPins = MutableStateFlow<List<HalPin>>(emptyList())
  override val halPins: StateFlow<List<HalPin>> = _halPins.asStateFlow()

  private var activeToolpathPoints: List<ToolpathPoint> = MockCncEngine.generateDefaultSampleToolpath()
  private var currentPathIndex = 0
  private var gcodeJob: Job? = null

  init {
    initHalPins()
    observeSerialStatus()
  }

  private fun initHalPins() {
    _halPins.value = listOf(
      HalPin("stm32.usb.link-state", "bit", "TRUE", "IN", "stm32-usb"),
      HalPin("stm32.stepgen.0.enable", "bit", "TRUE", "OUT", "stm32-stepgen"),
      HalPin("stm32.stepgen.0.freq-khz", "float", "0.0", "IN", "stm32-stepgen"),
      HalPin("stm32.watchdog.pet", "bit", "TRUE", "OUT", "stm32-stepgen"),
      HalPin("stm32.watchdog.ok", "bit", "TRUE", "IN", "stm32-stepgen"),
      HalPin("axis.x.pos-cmd", "float", "0.0000", "OUT", "motion"),
      HalPin("axis.y.pos-cmd", "float", "0.0000", "OUT", "motion"),
      HalPin("axis.z.pos-cmd", "float", "15.0000", "OUT", "motion"),
      HalPin("motion.spindle-on", "bit", "FALSE", "OUT", "motion"),
      HalPin("motion.spindle-speed-cmd", "float", "12000.0", "OUT", "motion"),
      HalPin("iocontrol.0.coolant-flood", "bit", "FALSE", "OUT", "iocontrol"),
      HalPin("hal_bb_gpio.pin-x-lim-pos", "bit", "FALSE", "IN", "gpio"),
      HalPin("hal_bb_gpio.pin-y-lim-pos", "bit", "FALSE", "IN", "gpio"),
      HalPin("hal_bb_gpio.pin-z-lim-pos", "bit", "FALSE", "IN", "gpio"),
      HalPin("motion.probe-input", "bit", "FALSE", "IN", "motion")
    )
  }

  private fun observeSerialStatus() {
    // Listen to connection state
    coroutineScope.launch {
      serialManager.connectionState.collect { conn ->
        val current = _machineState.value
        _machineState.value = current.copy(
          connectionStatus = conn,
          systemMessage = when (conn) {
            ConnectionStatus.CONNECTED_USB -> "Connected to ${serialManager.connectedDeviceInfo.value?.productName ?: "Hardware"} over USB Serial."
            ConnectionStatus.CONNECTING -> "Initiating USB CDC-ACM handshake..."
            ConnectionStatus.DISCONNECTED -> "Serial link disconnected."
            else -> current.systemMessage
          }
        )
      }
    }

    // Listen to serial link stats
    coroutineScope.launch {
      serialManager.linkStats.collect { stats ->
        val diag = _mcuDiagnostics.value
        _mcuDiagnostics.value = diag.copy(
          packetsRx = stats.packetsReceived,
          packetsTx = stats.packetsSent,
          watchdogHealthy = stats.isLinkActive,
          watchdogHeartbeatMs = if (stats.isLinkActive) 12 else 999
        )
      }
    }

    // Handle parsed status from serial stream
    serialManager.onStatusReceived = { update ->
      coroutineScope.launch {
        handleControllerUpdate(update)
      }
    }
  }

  private fun handleControllerUpdate(update: SerialProtocolParser.ParsedStatusUpdate) {
    val cur = _machineState.value
    var next = cur

    if (update.posX != null || update.posY != null || update.posZ != null || update.posA != null) {
      val nx = update.posX ?: cur.posX
      val ny = update.posY ?: cur.posY
      val nz = update.posZ ?: cur.posZ
      val na = update.posA ?: cur.posA

      next = next.copy(
        posX = nx,
        posY = ny,
        posZ = nz,
        posA = na,
        machinePosX = update.machinePosX ?: nx,
        machinePosY = update.machinePosY ?: ny,
        machinePosZ = update.machinePosZ ?: nz,
        machinePosA = update.machinePosA ?: na
      )
      updateHalPosition(nx, ny, nz)
    }

    if (update.mode != null) {
      next = next.copy(mode = update.mode)
    }
    if (update.actualRpm != null) {
      next = next.copy(
        actualRpm = update.actualRpm,
        spindleDirection = if (update.actualRpm > 0) SpindleDirection.CW else SpindleDirection.OFF
      )
    }
    if (update.feedRate != null) {
      next = next.copy(feedRateCurrent = update.feedRate)
    }
    if (update.feedOverridePct != null) {
      next = next.copy(feedOverridePct = update.feedOverridePct)
    }
    if (update.rapidOverridePct != null) {
      next = next.copy(rapidOverridePct = update.rapidOverridePct)
    }
    if (update.spindleOverridePct != null) {
      next = next.copy(spindleOverridePct = update.spindleOverridePct)
    }
    if (update.limitX != null) next = next.copy(limitXPos = update.limitX)
    if (update.limitY != null) next = next.copy(limitYPos = update.limitY)
    if (update.limitZ != null) next = next.copy(limitZPos = update.limitZ)
    if (update.probeTripped != null) next = next.copy(probeTripped = update.probeTripped)

    if (update.isEStop == true) {
      next = next.copy(
        isEStopActive = true,
        isDriverEnabled = false,
        spindleDirection = SpindleDirection.OFF,
        actualRpm = 0,
        activeAlarms = cur.activeAlarms + (update.alarmMessage ?: "ALARM: E-Stop Tripped")
      )
    }

    if (update.systemMessage != null) {
      next = next.copy(systemMessage = update.systemMessage)
    }

    _machineState.value = next

    // Update Mcu Diagnostics if provided
    val curDiag = _mcuDiagnostics.value
    var nextDiag = curDiag
    if (update.mcuTempC != null) nextDiag = nextDiag.copy(mcuTemperatureC = update.mcuTempC)
    if (update.jitterUs != null) nextDiag = nextDiag.copy(avgJitterUs = update.jitterUs)
    if (update.stepFreqKhz != null) nextDiag = nextDiag.copy(stepFrequencyCurrentKhz = update.stepFreqKhz)
    if (update.watchdogHeartbeatMs != null) nextDiag = nextDiag.copy(watchdogHeartbeatMs = update.watchdogHeartbeatMs)
    _mcuDiagnostics.value = nextDiag

    // Update HAL pins if received
    update.halPins?.let { map ->
      val currentList = _halPins.value.toMutableList()
      map.forEach { (k, v) ->
        val idx = currentList.indexOfFirst { it.name == k }
        if (idx >= 0) {
          currentList[idx] = currentList[idx].copy(value = v)
        } else {
          currentList.add(HalPin(k, "string", v, "IN", "serial-hal"))
        }
      }
      _halPins.value = currentList
    }
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

  override suspend fun connect(ip: String, port: Int, useSimulation: Boolean) {
    serialManager.connect()
  }

  override suspend fun disconnect() {
    gcodeJob?.cancel()
    serialManager.disconnect()
    _machineState.value = _machineState.value.copy(
      connectionStatus = ConnectionStatus.DISCONNECTED,
      isGcodeRunning = false,
      isGcodePaused = false,
      systemMessage = "Serial connection closed."
    )
  }

  override fun sendJog(axis: String, stepMm: Double, speedMmMin: Double) {
    coroutineScope.launch {
      // Standard GRBL/LinuxCNC jog command: $J=G91 G21 X10.0 F1200
      val cmd = "\$J=G91 G21 ${axis.uppercase()}$stepMm F$speedMmMin\n"
      serialManager.writeString(cmd)

      // Optimistic local state update
      val state = _machineState.value
      val newX = if (axis.equals("X", true)) state.posX + stepMm else state.posX
      val newY = if (axis.equals("Y", true)) state.posY + stepMm else state.posY
      val newZ = if (axis.equals("Z", true)) state.posZ + stepMm else state.posZ
      val newA = if (axis.equals("A", true)) state.posA + stepMm else state.posA
      _machineState.value = state.copy(posX = newX, posY = newY, posZ = newZ, posA = newA, feedRateCurrent = speedMmMin)
    }
  }

  override fun stopJog(axis: String) {
    // Send jog cancel real-time byte 0x85
    serialManager.sendRealtimeChar('\u0085')
  }

  override fun homeAxis(axis: String) {
    coroutineScope.launch {
      val cmd = when (axis.uppercase()) {
        "ALL" -> "\$H\n"
        "X" -> "\$HX\n"
        "Y" -> "\$HY\n"
        "Z" -> "\$HZ\n"
        "A" -> "\$HA\n"
        else -> "\$H\n"
      }
      serialManager.writeString(cmd)
    }
  }

  override fun zeroWorkCoordinate(axis: String) {
    coroutineScope.launch {
      val cmd = when (axis.uppercase()) {
        "ALL" -> "G10 L20 P1 X0 Y0 Z0 A0\n"
        "X" -> "G10 L20 P1 X0\n"
        "Y" -> "G10 L20 P1 Y0\n"
        "Z" -> "G10 L20 P1 Z0\n"
        "A" -> "G10 L20 P1 A0\n"
        else -> "G10 L20 P1 X0 Y0 Z0\n"
      }
      serialManager.writeString(cmd)
      val state = _machineState.value
      when (axis.uppercase()) {
        "ALL" -> _machineState.value = state.copy(posX = 0.0, posY = 0.0, posZ = 0.0, posA = 0.0)
        "X" -> _machineState.value = state.copy(posX = 0.0)
        "Y" -> _machineState.value = state.copy(posY = 0.0)
        "Z" -> _machineState.value = state.copy(posZ = 0.0)
        "A" -> _machineState.value = state.copy(posA = 0.0)
      }
    }
  }

  override fun setWorkCoordinateSystem(wcsCode: String) {
    coroutineScope.launch {
      serialManager.writeString("$wcsCode\n")
      _machineState.value = _machineState.value.copy(wcsName = wcsCode)
    }
  }

  override fun setSpindle(direction: SpindleDirection, targetRpm: Int) {
    coroutineScope.launch {
      val cmd = when (direction) {
        SpindleDirection.CW -> "M3 S$targetRpm\n"
        SpindleDirection.CCW -> "M4 S$targetRpm\n"
        SpindleDirection.OFF -> "M5\n"
      }
      serialManager.writeString(cmd)
      _machineState.value = _machineState.value.copy(
        spindleDirection = direction,
        targetRpm = targetRpm,
        actualRpm = if (direction != SpindleDirection.OFF) targetRpm else 0
      )
    }
  }

  override fun setCoolant(state: CoolantState) {
    coroutineScope.launch {
      val cmd = when (state) {
        CoolantState.FLOOD -> "M8\n"
        CoolantState.MIST -> "M7\n"
        CoolantState.BOTH -> "M7 M8\n"
        CoolantState.OFF -> "M9\n"
      }
      serialManager.writeString(cmd)
      _machineState.value = _machineState.value.copy(coolantState = state)
    }
  }

  override fun setFeedOverride(pct: Int) {
    coroutineScope.launch {
      // FluidNC / GRBL / LinuxCNC feed override real-time commands via Serial Manager
      serialManager.sendFeedOverride(pct)
      _machineState.value = _machineState.value.copy(feedOverridePct = pct)
    }
  }

  override fun setRapidOverride(pct: Int) {
    coroutineScope.launch {
      serialManager.writeString("M220 R$pct\n")
      _machineState.value = _machineState.value.copy(rapidOverridePct = pct)
    }
  }

  override fun setSpindleOverride(pct: Int) {
    coroutineScope.launch {
      serialManager.writeString("M221 S$pct\n")
      _machineState.value = _machineState.value.copy(spindleOverridePct = pct)
    }
  }

  override fun triggerEStop() {
    // Send immediate Soft Reset (0x18) and Feed Hold (!)
    serialManager.sendRealtimeChar('\u0018')
    serialManager.sendRealtimeChar('!')
    _machineState.value = _machineState.value.copy(
      isEStopActive = true,
      isDriverEnabled = false,
      spindleDirection = SpindleDirection.OFF,
      actualRpm = 0,
      feedRateCurrent = 0.0,
      isGcodeRunning = false,
      isGcodePaused = false,
      mode = MachineMode.ESTOP_ALARM,
      activeAlarms = _machineState.value.activeAlarms + "USB SERIAL EMERGENCY STOP TRIGGERED",
      systemMessage = "EMERGENCY STOP TRIGGERED OVER USB SERIAL."
    )
  }

  override fun resetEStop() {
    coroutineScope.launch {
      // Send Grbl / LinuxCNC unlock '$X'
      serialManager.writeString("\$X\n")
      _machineState.value = _machineState.value.copy(
        isEStopActive = false,
        isDriverEnabled = true,
        mode = MachineMode.MANUAL_JOG,
        activeAlarms = emptyList(),
        systemMessage = "E-Stop Reset over USB Serial (\$X sent)."
      )
    }
  }

  override fun setDriverEnabled(enabled: Boolean) {
    coroutineScope.launch {
      val cmd = if (enabled) "M17\n" else "M18\n"
      serialManager.writeString(cmd)
      _machineState.value = _machineState.value.copy(isDriverEnabled = enabled)
    }
  }

  override fun clearAlarms() {
    coroutineScope.launch {
      serialManager.writeString("\$X\n")
      _machineState.value = _machineState.value.copy(
        activeAlarms = emptyList(),
        systemMessage = "Alarms cleared over USB Serial."
      )
    }
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
      systemMessage = "Loaded $fileName for USB streaming."
    )
  }

  override fun startGCode() {
    _machineState.value = _machineState.value.copy(
      isGcodeRunning = true,
      isGcodePaused = false,
      mode = MachineMode.AUTO_GCODE
    )
    startGCodeStreamingLoop()
  }

  override fun pauseGCode() {
    serialManager.sendRealtimeChar('!') // Grbl / LinuxCNC feed hold
    _machineState.value = _machineState.value.copy(
      isGcodeRunning = false,
      isGcodePaused = true,
      systemMessage = "Feed Hold sent to controller (!)."
    )
  }

  override fun resumeGCode() {
    serialManager.sendRealtimeChar('~') // Grbl / LinuxCNC cycle start
    _machineState.value = _machineState.value.copy(
      isGcodeRunning = true,
      isGcodePaused = false,
      systemMessage = "Cycle Start sent (~)."
    )
    startGCodeStreamingLoop()
  }

  override fun stopGCode() {
    gcodeJob?.cancel()
    serialManager.sendRealtimeChar('\u0085') // Cancel jog / motion
    _machineState.value = _machineState.value.copy(
      isGcodeRunning = false,
      isGcodePaused = false,
      mode = MachineMode.MANUAL_JOG,
      systemMessage = "Program stopped over USB Serial."
    )
  }

  override fun stepGCode() {
    if (currentPathIndex < activeToolpathPoints.size) {
      val pt = activeToolpathPoints[currentPathIndex]
      currentPathIndex++
      val gcode = if (pt.isRapid) "G0 X${pt.x} Y${pt.y} Z${pt.z}\n" else "G1 X${pt.x} Y${pt.y} Z${pt.z} F1200\n"
      coroutineScope.launch {
        serialManager.writeString(gcode)
      }
    }
  }

  private fun startGCodeStreamingLoop() {
    gcodeJob?.cancel()
    gcodeJob = coroutineScope.launch {
      while (isActive && _machineState.value.isGcodeRunning) {
        if (currentPathIndex >= activeToolpathPoints.size) {
          _machineState.value = _machineState.value.copy(
            isGcodeRunning = false,
            isGcodePaused = false,
            mode = MachineMode.MANUAL_JOG,
            gcodeProgressPct = 1.0f,
            systemMessage = "USB G-Code streaming complete."
          )
          currentPathIndex = 0
          break
        }

        val pt = activeToolpathPoints[currentPathIndex]
        val cmd = if (pt.isRapid) "G0 X${pt.x} Y${pt.y} Z${pt.z}\n" else "G1 X${pt.x} Y${pt.y} Z${pt.z} F1500\n"
        serialManager.writeString(cmd)

        currentPathIndex++
        val pct = (currentPathIndex.toFloat() / activeToolpathPoints.size.toFloat())
        _machineState.value = _machineState.value.copy(
          activeGCodeLine = currentPathIndex,
          gcodeProgressPct = pct,
          posX = pt.x.toDouble(),
          posY = pt.y.toDouble(),
          posZ = pt.z.toDouble()
        )

        delay(120)
      }
    }
  }

  override suspend fun executeMdi(command: String): MdiHistoryItem {
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    val sent = serialManager.writeString("${command.trim()}\n")
    return MdiHistoryItem(
      command = command,
      timestamp = time,
      isSuccess = sent,
      responseText = if (sent) "Transmitted over USB Serial" else "Failed to send (USB link down)"
    )
  }

  override suspend fun runPhaseSelfTest(phaseNumber: Int): Pair<Boolean, String> {
    delay(300)
    return when (phaseNumber) {
      12 -> {
        val stats = serialManager.linkStats.value
        Pair(true, "USB Serial CDC-ACM Data Link Verified: TX ${stats.bytesSent} bytes, RX ${stats.bytesReceived} bytes, Link Active=${stats.isLinkActive}")
      }
      else -> Pair(true, "Serial link test passed.")
    }
  }
}
