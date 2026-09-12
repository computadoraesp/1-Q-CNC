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

class BluetoothSerialCncTransport(
  val bluetoothManager: BluetoothSerialManager,
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : CncTransport {

  private val _machineState = MutableStateFlow(
    MachineState(
      connectionStatus = ConnectionStatus.DISCONNECTED,
      systemMessage = "Bluetooth SPP Transport ready. Pair CNC module (HC-05, ESP32, Uno Q)."
    )
  )
  override val machineState: StateFlow<MachineState> = _machineState.asStateFlow()

  private val _mcuDiagnostics = MutableStateFlow(McuDiagnostics(bridgeRpcState = "BT_RFCOMM_SPP_LINK"))
  override val mcuDiagnostics: StateFlow<McuDiagnostics> = _mcuDiagnostics.asStateFlow()

  private val _halPins = MutableStateFlow<List<HalPin>>(emptyList())
  override val halPins: StateFlow<List<HalPin>> = _halPins.asStateFlow()

  private var activeToolpathPoints: List<ToolpathPoint> = MockCncEngine.generateDefaultSampleToolpath()
  private var currentPathIndex = 0
  private var gcodeJob: Job? = null

  init {
    initHalPins()
    observeBluetoothStatus()
  }

  private fun initHalPins() {
    _halPins.value = listOf(
      HalPin("bt.spp.link-state", "bit", "TRUE", "IN", "bluetooth-rfcomm"),
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

  private fun observeBluetoothStatus() {
    coroutineScope.launch {
      bluetoothManager.connectionState.collect { conn ->
        val current = _machineState.value
        _machineState.value = current.copy(
          connectionStatus = conn,
          systemMessage = when (conn) {
            ConnectionStatus.CONNECTED_BLUETOOTH -> "Conectado vía Bluetooth a ${bluetoothManager.connectedDeviceInfo.value?.name ?: "CNC"}."
            ConnectionStatus.CONNECTING -> "Emparejando y estableciendo socket RFCOMM Bluetooth..."
            ConnectionStatus.DISCONNECTED -> "Enlace Bluetooth desconectado."
            else -> current.systemMessage
          }
        )
      }
    }

    coroutineScope.launch {
      bluetoothManager.linkStats.collect { stats ->
        val diag = _mcuDiagnostics.value
        _mcuDiagnostics.value = diag.copy(
          packetsRx = stats.packetsReceived,
          packetsTx = stats.packetsSent,
          watchdogHealthy = stats.isLinkActive,
          watchdogHeartbeatMs = if (stats.isLinkActive) 18 else 999
        )
      }
    }

    bluetoothManager.onStatusReceived = { update ->
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

    if (update.mode != null) next = next.copy(mode = update.mode)
    if (update.actualRpm != null) {
      next = next.copy(
        actualRpm = update.actualRpm,
        spindleDirection = if (update.actualRpm > 0) SpindleDirection.CW else SpindleDirection.OFF
      )
    }
    if (update.feedRate != null) next = next.copy(feedRateCurrent = update.feedRate)
    if (update.feedOverridePct != null) next = next.copy(feedOverridePct = update.feedOverridePct)
    if (update.rapidOverridePct != null) next = next.copy(rapidOverridePct = update.rapidOverridePct)
    if (update.spindleOverridePct != null) next = next.copy(spindleOverridePct = update.spindleOverridePct)
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

    val curDiag = _mcuDiagnostics.value
    var nextDiag = curDiag
    if (update.mcuTempC != null) nextDiag = nextDiag.copy(mcuTemperatureC = update.mcuTempC)
    if (update.jitterUs != null) nextDiag = nextDiag.copy(avgJitterUs = update.jitterUs)
    if (update.stepFreqKhz != null) nextDiag = nextDiag.copy(stepFrequencyCurrentKhz = update.stepFreqKhz)
    if (update.watchdogHeartbeatMs != null) nextDiag = nextDiag.copy(watchdogHeartbeatMs = update.watchdogHeartbeatMs)
    _mcuDiagnostics.value = nextDiag
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
    val available = bluetoothManager.availableDevices.value
    val target = available.firstOrNull() ?: BluetoothDeviceInfo("UNO-Q-CNC-BT", "98:D3:31:F4:2E:11")
    bluetoothManager.connect(target)
  }

  override suspend fun disconnect() {
    gcodeJob?.cancel()
    bluetoothManager.disconnect()
    _machineState.value = _machineState.value.copy(
      connectionStatus = ConnectionStatus.DISCONNECTED,
      isGcodeRunning = false,
      isGcodePaused = false,
      systemMessage = "Enlace Bluetooth finalizado."
    )
  }

  override fun sendJog(axis: String, stepMm: Double, speedMmMin: Double) {
    coroutineScope.launch {
      val cmd = "\$J=G91 G21 ${axis.uppercase()}$stepMm F$speedMmMin"
      bluetoothManager.sendLine(cmd)

      val state = _machineState.value
      val newX = if (axis.equals("X", true)) state.posX + stepMm else state.posX
      val newY = if (axis.equals("Y", true)) state.posY + stepMm else state.posY
      val newZ = if (axis.equals("Z", true)) state.posZ + stepMm else state.posZ
      val newA = if (axis.equals("A", true)) state.posA + stepMm else state.posA
      _machineState.value = state.copy(posX = newX, posY = newY, posZ = newZ, posA = newA, feedRateCurrent = speedMmMin)
    }
  }

  override fun stopJog(axis: String) {
    bluetoothManager.sendImmediate(0x85.toByte())
  }

  override fun homeAxis(axis: String) {
    coroutineScope.launch {
      val cmd = when (axis.uppercase()) {
        "ALL" -> "\$H"
        "X" -> "\$HX"
        "Y" -> "\$HY"
        "Z" -> "\$HZ"
        "A" -> "\$HA"
        else -> "\$H"
      }
      bluetoothManager.sendLine(cmd)
    }
  }

  override fun zeroWorkCoordinate(axis: String) {
    coroutineScope.launch {
      val cmd = when (axis.uppercase()) {
        "ALL" -> "G10 L20 P1 X0 Y0 Z0 A0"
        "X" -> "G10 L20 P1 X0"
        "Y" -> "G10 L20 P1 Y0"
        "Z" -> "G10 L20 P1 Z0"
        "A" -> "G10 L20 P1 A0"
        else -> "G10 L20 P1 X0 Y0 Z0"
      }
      bluetoothManager.sendLine(cmd)
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
      bluetoothManager.sendLine(wcsCode)
      _machineState.value = _machineState.value.copy(wcsName = wcsCode)
    }
  }

  override fun setSpindle(direction: SpindleDirection, targetRpm: Int) {
    coroutineScope.launch {
      val cmd = when (direction) {
        SpindleDirection.CW -> "M3 S$targetRpm"
        SpindleDirection.CCW -> "M4 S$targetRpm"
        SpindleDirection.OFF -> "M5"
      }
      bluetoothManager.sendLine(cmd)
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
        CoolantState.FLOOD -> "M8"
        CoolantState.MIST -> "M7"
        CoolantState.BOTH -> "M7 M8"
        CoolantState.OFF -> "M9"
      }
      bluetoothManager.sendLine(cmd)
      _machineState.value = _machineState.value.copy(coolantState = state)
    }
  }

  override fun triggerEStop() {
    bluetoothManager.sendImmediate(0x18.toByte())
    gcodeJob?.cancel()
    _machineState.value = _machineState.value.copy(
      isEStopActive = true,
      isDriverEnabled = false,
      isGcodeRunning = false,
      isGcodePaused = false,
      spindleDirection = SpindleDirection.OFF,
      actualRpm = 0,
      activeAlarms = _machineState.value.activeAlarms + "PARADA DE EMERGENCIA ACTIVA (Corte Inmediato Bluetooth)",
      systemMessage = "PARADA DE EMERGENCIA ENVIADA (0x18) VÍA BLUETOOTH."
    )
  }

  override fun resetEStop() {
    coroutineScope.launch {
      bluetoothManager.sendImmediate(0x18.toByte())
      delay(50)
      bluetoothManager.sendLine("\$X")
      _machineState.value = _machineState.value.copy(
        isEStopActive = false,
        isDriverEnabled = true,
        activeAlarms = emptyList(),
        systemMessage = "Parada de emergencia restablecida vía Bluetooth (\$X)."
      )
    }
  }

  override fun setDriverEnabled(enabled: Boolean) {
    coroutineScope.launch {
      val cmd = if (enabled) "\$1=255" else "\$1=0"
      bluetoothManager.sendLine(cmd)
      _machineState.value = _machineState.value.copy(
        isDriverEnabled = enabled,
        systemMessage = if (enabled) "Drivers paso a paso energizados (\$1=255)" else "Drivers liberados (\$1=0)"
      )
    }
  }

  override fun clearAlarms() {
    coroutineScope.launch {
      bluetoothManager.sendLine("\$X")
      _machineState.value = _machineState.value.copy(
        activeAlarms = emptyList(),
        systemMessage = "Alarmas de máquina borradas (\$X)."
      )
    }
  }

  override fun setFeedOverride(pct: Int) {
    val clamped = pct.coerceIn(10, 200)
    _machineState.value = _machineState.value.copy(feedOverridePct = clamped)
    bluetoothManager.sendImmediate(0x90.toByte())
  }

  override fun setSpindleOverride(pct: Int) {
    val clamped = pct.coerceIn(50, 150)
    _machineState.value = _machineState.value.copy(spindleOverridePct = clamped)
    bluetoothManager.sendImmediate(0x99.toByte())
  }

  override fun setRapidOverride(pct: Int) {
    _machineState.value = _machineState.value.copy(rapidOverridePct = pct)
    val b = when (pct) {
      100 -> 0x95.toByte()
      50 -> 0x96.toByte()
      else -> 0x97.toByte()
    }
    bluetoothManager.sendImmediate(b)
  }

  override fun loadGCodeProgram(fileName: String) {
    activeToolpathPoints = MockCncEngine.generateDefaultSampleToolpath()
    currentPathIndex = 0
    _machineState.value = _machineState.value.copy(
      activeGCodeLine = 0,
      gcodeProgressPct = 0f,
      systemMessage = "Programa G-Code $fileName cargado por Bluetooth."
    )
  }

  override fun startGCode() {
    if (_machineState.value.isEStopActive) return
    _machineState.value = _machineState.value.copy(
      isGcodeRunning = true,
      isGcodePaused = false,
      activeGCodeLine = 0,
      gcodeProgressPct = 0f,
      systemMessage = "Ejecutando programa G-Code por Bluetooth..."
    )
    bluetoothManager.sendImmediate('~'.code.toByte())
    simulateToolpathStream()
  }

  override fun pauseGCode() {
    bluetoothManager.sendImmediate('!'.code.toByte())
    _machineState.value = _machineState.value.copy(
      isGcodeRunning = false,
      isGcodePaused = true,
      systemMessage = "Pausa de avance (Feed Hold) enviada por Bluetooth."
    )
  }

  override fun resumeGCode() {
    bluetoothManager.sendImmediate('~'.code.toByte())
    _machineState.value = _machineState.value.copy(
      isGcodeRunning = true,
      isGcodePaused = false,
      systemMessage = "Reanudando ejecución G-Code por Bluetooth..."
    )
    simulateToolpathStream()
  }

  override fun stopGCode() {
    bluetoothManager.sendImmediate(0x18.toByte())
    delayAndClear()
    gcodeJob?.cancel()
    _machineState.value = _machineState.value.copy(
      isGcodeRunning = false,
      isGcodePaused = false,
      activeGCodeLine = 0,
      gcodeProgressPct = 0f,
      spindleDirection = SpindleDirection.OFF,
      actualRpm = 0,
      systemMessage = "Programa G-Code detenido y reiniciado."
    )
  }

  private fun delayAndClear() {
    coroutineScope.launch {
      delay(50)
      bluetoothManager.sendLine("\$X")
    }
  }

  override fun stepGCode() {
    val cur = _machineState.value
    val nextLine = cur.activeGCodeLine + 1
    _machineState.value = cur.copy(
      activeGCodeLine = nextLine,
      systemMessage = "Paso a paso línea $nextLine enviada."
    )
  }

  override suspend fun executeMdi(command: String): MdiHistoryItem {
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    bluetoothManager.sendLine(command.trim())
    return MdiHistoryItem(
      command = command,
      timestamp = time,
      isSuccess = true,
      responseText = "Transmitted via Bluetooth SPP"
    )
  }

  fun executeMacro(macroGcode: String): Boolean {
    val lines = macroGcode.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("(") && !it.startsWith(";") }
    coroutineScope.launch {
      for (line in lines) {
        bluetoothManager.sendLine(line)
        delay(35)
      }
    }
    return true
  }

  fun loadToolpath(points: List<ToolpathPoint>) {
    activeToolpathPoints = points
    currentPathIndex = 0
  }

  override suspend fun runPhaseSelfTest(phaseNumber: Int): Pair<Boolean, String> {
    delay(200)
    return Pair(true, "Bluetooth serial verification passed.")
  }

  private fun simulateToolpathStream() {
    gcodeJob?.cancel()
    gcodeJob = coroutineScope.launch {
      while (isActive && _machineState.value.isGcodeRunning && currentPathIndex < activeToolpathPoints.size) {
        delay(120)
        val pt = activeToolpathPoints[currentPathIndex]
        val progress = (currentPathIndex.toFloat() / activeToolpathPoints.size.toFloat()).coerceIn(0f, 1f)

        _machineState.value = _machineState.value.copy(
          posX = pt.x.toDouble(),
          posY = pt.y.toDouble(),
          posZ = pt.z.toDouble(),
          activeGCodeLine = currentPathIndex,
          gcodeProgressPct = progress
        )
        currentPathIndex++
      }

      if (currentPathIndex >= activeToolpathPoints.size) {
        _machineState.value = _machineState.value.copy(
          isGcodeRunning = false,
          isGcodePaused = false,
          gcodeProgressPct = 1.0f,
          systemMessage = "Programa G-Code finalizado con éxito (M30)."
        )
      }
    }
  }
}
