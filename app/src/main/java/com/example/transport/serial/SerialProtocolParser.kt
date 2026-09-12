package com.example.transport.serial

import com.example.model.*
import java.util.Locale

object SerialProtocolParser {

  data class ParsedStatusUpdate(
    val posX: Double? = null,
    val posY: Double? = null,
    val posZ: Double? = null,
    val posA: Double? = null,
    val machinePosX: Double? = null,
    val machinePosY: Double? = null,
    val machinePosZ: Double? = null,
    val machinePosA: Double? = null,
    val mode: MachineMode? = null,
    val actualRpm: Int? = null,
    val feedRate: Double? = null,
    val feedOverridePct: Int? = null,
    val rapidOverridePct: Int? = null,
    val spindleOverridePct: Int? = null,
    val limitX: Boolean? = null,
    val limitY: Boolean? = null,
    val limitZ: Boolean? = null,
    val probeTripped: Boolean? = null,
    val isEStop: Boolean? = null,
    val mcuTempC: Float? = null,
    val jitterUs: Float? = null,
    val stepFreqKhz: Float? = null,
    val watchdogHeartbeatMs: Long? = null,
    val halPins: Map<String, String>? = null,
    val responseAck: String? = null,
    val systemMessage: String? = null,
    val isAlarm: Boolean = false,
    val alarmMessage: String? = null
  )

  /**
   * Parse a single line from the CNC controller (GRBL, LinuxCNC HAL bridge, STM32 firmware, or JSON)
   */
  fun parseLine(line: String): ParsedStatusUpdate? {
    val trimmed = line.trim()
    if (trimmed.isEmpty()) return null

    // 1. Status frame: <Idle|MPos:0.000,0.000,15.000|FS:0,0|Ov:100,100,100|Pn:P|...>
    if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
      return parseStatusReport(trimmed.substring(1, trimmed.length - 1))
    }

    // 2. HAL pin update: [HAL:axis.x.pos-cmd=12.450] or HAL:axis.x.pos-cmd=12.450
    if ((trimmed.startsWith("[HAL:") && trimmed.endsWith("]")) || trimmed.startsWith("HAL:")) {
      val inner = if (trimmed.startsWith("[HAL:")) trimmed.substring(5, trimmed.length - 1) else trimmed.substring(4)
      val parts = inner.split("=", limit = 2)
      if (parts.size == 2) {
        val pinName = parts[0].trim()
        val pinVal = parts[1].trim()
        return ParsedStatusUpdate(
          halPins = mapOf(pinName to pinVal),
          systemMessage = "HAL Updated: $pinName = $pinVal"
        )
      }
    }

    // 3. Alarm response: ALARM: 1 (Hard limit triggered)
    if (trimmed.startsWith("ALARM:", ignoreCase = true) || trimmed.startsWith("ALARM ", ignoreCase = true)) {
      val msg = if (trimmed.startsWith("ALARM:", true)) trimmed.substringAfter("ALARM:").trim() else trimmed.substringAfter("ALARM ").trim()
      return ParsedStatusUpdate(
        isAlarm = true,
        alarmMessage = msg,
        isEStop = true,
        mode = MachineMode.ESTOP_ALARM,
        systemMessage = "CRITICAL HARDWARE ALARM: $msg"
      )
    }

    // 4. Feedback messages: [MSG: ...]
    if (trimmed.startsWith("[MSG:") && trimmed.endsWith("]")) {
      val msg = trimmed.substring(5, trimmed.length - 1).trim()
      return ParsedStatusUpdate(systemMessage = msg)
    }

    // 5. JSON Line format (e.g. {"state":"Run","pos":{"x":15.2,"y":30.4,"z":-1.0},"rpm":18000,...})
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
      return parseJsonStatus(trimmed)
    }

    // 6. Acknowledgements: ok or error:<code>
    if (trimmed.equals("ok", ignoreCase = true)) {
      return ParsedStatusUpdate(responseAck = "OK")
    }

    if (trimmed.startsWith("error:", ignoreCase = true)) {
      val err = trimmed.substringAfter("error:").trim()
      return ParsedStatusUpdate(
        responseAck = "ERROR: $err",
        systemMessage = "Controller Error Code: $err"
      )
    }

    return null
  }

  private fun parseStatusReport(content: String): ParsedStatusUpdate {
    val sections = content.split("|")
    var stateMode: MachineMode? = null
    var posX: Double? = null
    var posY: Double? = null
    var posZ: Double? = null
    var posA: Double? = null
    var mPosX: Double? = null
    var mPosY: Double? = null
    var mPosZ: Double? = null
    var mPosA: Double? = null
    var feed: Double? = null
    var rpm: Int? = null
    var feedOv: Int? = null
    var rapidOv: Int? = null
    var spinOv: Int? = null
    var limX = false
    var limY = false
    var limZ = false
    var probe = false
    var mcuTemp: Float? = null
    var jitter: Float? = null
    var stepFreq: Float? = null
    var heartbeat: Long? = null

    for (section in sections) {
      val s = section.trim()
      when {
        s.equals("Idle", ignoreCase = true) -> stateMode = MachineMode.MANUAL_JOG
        s.equals("Run", ignoreCase = true) -> stateMode = MachineMode.AUTO_GCODE
        s.equals("Hold", ignoreCase = true) -> stateMode = MachineMode.AUTO_GCODE
        s.equals("Jog", ignoreCase = true) -> stateMode = MachineMode.MANUAL_JOG
        s.equals("Home", ignoreCase = true) -> stateMode = MachineMode.HOMING
        s.equals("Alarm", ignoreCase = true) -> stateMode = MachineMode.ESTOP_ALARM
        
        s.startsWith("WPos:") || s.startsWith("Pos:") -> {
          val coords = s.substringAfter(":").split(",")
          if (coords.isNotEmpty()) posX = coords[0].toDoubleOrNull()
          if (coords.size > 1) posY = coords[1].toDoubleOrNull()
          if (coords.size > 2) posZ = coords[2].toDoubleOrNull()
          if (coords.size > 3) posA = coords[3].toDoubleOrNull()
        }
        s.startsWith("MPos:") -> {
          val coords = s.substringAfter(":").split(",")
          if (coords.isNotEmpty()) mPosX = coords[0].toDoubleOrNull()
          if (coords.size > 1) mPosY = coords[1].toDoubleOrNull()
          if (coords.size > 2) mPosZ = coords[2].toDoubleOrNull()
          if (coords.size > 3) mPosA = coords[3].toDoubleOrNull()
          if (posX == null) posX = mPosX
          if (posY == null) posY = mPosY
          if (posZ == null) posZ = mPosZ
          if (posA == null) posA = mPosA
        }
        s.startsWith("FS:") || s.startsWith("F:") -> {
          val vals = s.substringAfter(":").split(",")
          if (vals.isNotEmpty()) feed = vals[0].toDoubleOrNull()
          if (vals.size > 1) rpm = vals[1].toIntOrNull()
        }
        s.startsWith("Ov:") -> {
          val ovs = s.substringAfter(":").split(",")
          if (ovs.isNotEmpty()) feedOv = ovs[0].toIntOrNull()
          if (ovs.size > 1) rapidOv = ovs[1].toIntOrNull()
          if (ovs.size > 2) spinOv = ovs[2].toIntOrNull()
        }
        s.startsWith("Pn:") -> {
          val pins = s.substringAfter(":")
          if (pins.contains("X", ignoreCase = true)) limX = true
          if (pins.contains("Y", ignoreCase = true)) limY = true
          if (pins.contains("Z", ignoreCase = true)) limZ = true
          if (pins.contains("P", ignoreCase = true)) probe = true
        }
        s.startsWith("Temp:") -> {
          mcuTemp = s.substringAfter(":").toFloatOrNull()
        }
        s.startsWith("Jitter:") -> {
          jitter = s.substringAfter(":").toFloatOrNull()
        }
        s.startsWith("StepFreq:") -> {
          stepFreq = s.substringAfter(":").toFloatOrNull()
        }
        s.startsWith("HB:") -> {
          heartbeat = s.substringAfter(":").toLongOrNull()
        }
      }
    }

    return ParsedStatusUpdate(
      posX = posX,
      posY = posY,
      posZ = posZ,
      posA = posA,
      machinePosX = mPosX ?: posX,
      machinePosY = mPosY ?: posY,
      machinePosZ = mPosZ ?: posZ,
      machinePosA = mPosA ?: posA,
      mode = stateMode,
      actualRpm = rpm,
      feedRate = feed,
      feedOverridePct = feedOv,
      rapidOverridePct = rapidOv,
      spindleOverridePct = spinOv,
      limitX = limX,
      limitY = limY,
      limitZ = limZ,
      probeTripped = probe,
      mcuTempC = mcuTemp,
      jitterUs = jitter,
      stepFreqKhz = stepFreq,
      watchdogHeartbeatMs = heartbeat
    )
  }

  private fun parseJsonStatus(jsonStr: String): ParsedStatusUpdate {
    var posX: Double? = null
    var posY: Double? = null
    var posZ: Double? = null
    var posA: Double? = null
    var mcuTemp: Float? = null
    var jitter: Float? = null
    var rpm: Int? = null
    var feed: Double? = null
    var mode: MachineMode? = null

    Regex(""""state"\s*:\s*"([^"]+)"""").find(jsonStr)?.let {
      val s = it.groupValues[1]
      mode = when {
        s.equals("Run", true) -> MachineMode.AUTO_GCODE
        s.equals("Idle", true) || s.equals("Jog", true) -> MachineMode.MANUAL_JOG
        s.equals("Alarm", true) -> MachineMode.ESTOP_ALARM
        s.equals("Home", true) -> MachineMode.HOMING
        else -> null
      }
    }

    Regex(""""x"\s*:\s*(-?\d+\.?\d*)""").find(jsonStr)?.let {
      posX = it.groupValues[1].toDoubleOrNull()
    }
    Regex(""""y"\s*:\s*(-?\d+\.?\d*)""").find(jsonStr)?.let {
      posY = it.groupValues[1].toDoubleOrNull()
    }
    Regex(""""z"\s*:\s*(-?\d+\.?\d*)""").find(jsonStr)?.let {
      posZ = it.groupValues[1].toDoubleOrNull()
    }
    Regex(""""a"\s*:\s*(-?\d+\.?\d*)""").find(jsonStr)?.let {
      posA = it.groupValues[1].toDoubleOrNull()
    }
    Regex("""("temp"|"mcu_temp")\s*:\s*(-?\d+\.?\d*)""").find(jsonStr)?.let {
      mcuTemp = it.groupValues[2].toFloatOrNull()
    }
    Regex("""("jitter"|"jitter_us")\s*:\s*(-?\d+\.?\d*)""").find(jsonStr)?.let {
      jitter = it.groupValues[2].toFloatOrNull()
    }
    Regex(""""rpm"\s*:\s*(\d+)""").find(jsonStr)?.let {
      rpm = it.groupValues[1].toIntOrNull()
    }
    Regex(""""feed"\s*:\s*(-?\d+\.?\d*)""").find(jsonStr)?.let {
      feed = it.groupValues[1].toDoubleOrNull()
    }

    return ParsedStatusUpdate(
      posX = posX,
      posY = posY,
      posZ = posZ,
      posA = posA,
      mode = mode,
      actualRpm = rpm,
      feedRate = feed,
      mcuTempC = mcuTemp,
      jitterUs = jitter
    )
  }

  /**
   * Format G-Code or Jog command with standard line checksum (N<line> <cmd>*<cs>)
   */
  fun formatGcodeCommand(lineNum: Int, command: String, includeChecksum: Boolean = false): String {
    val clean = command.trim()
    if (!includeChecksum || lineNum <= 0) {
      return "$clean\n"
    }
    val payload = "N$lineNum $clean"
    var cs = 0
    for (ch in payload) {
      cs = cs xor ch.code
    }
    return "$payload*$cs\n"
  }
}
