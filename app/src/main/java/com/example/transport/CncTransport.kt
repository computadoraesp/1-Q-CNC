package com.example.transport

import com.example.model.*
import kotlinx.coroutines.flow.StateFlow

interface CncTransport {
  val machineState: StateFlow<MachineState>
  val mcuDiagnostics: StateFlow<McuDiagnostics>
  val halPins: StateFlow<List<HalPin>>
  
  suspend fun connect(ip: String, port: Int, useSimulation: Boolean)
  suspend fun disconnect()
  
  // Motion & Control
  fun sendJog(axis: String, stepMm: Double, speedMmMin: Double)
  fun stopJog(axis: String)
  fun homeAxis(axis: String) // "ALL", "X", "Y", "Z", "A"
  fun zeroWorkCoordinate(axis: String) // "ALL", "X", "Y", "Z", "A"
  fun setWorkCoordinateSystem(wcsCode: String)
  
  // Spindle & Aux
  fun setSpindle(direction: SpindleDirection, targetRpm: Int)
  fun setCoolant(state: CoolantState)
  fun setFeedOverride(pct: Int)
  fun setRapidOverride(pct: Int)
  fun setSpindleOverride(pct: Int)
  
  // Safety
  fun triggerEStop()
  fun resetEStop()
  fun setDriverEnabled(enabled: Boolean)
  fun clearAlarms()
  
  // G-Code Program
  fun loadGCodeProgram(fileName: String)
  fun startGCode()
  fun pauseGCode()
  fun resumeGCode()
  fun stopGCode()
  fun stepGCode()
  
  // MDI
  suspend fun executeMdi(command: String): MdiHistoryItem
  
  // Commissioning Tests
  suspend fun runPhaseSelfTest(phaseNumber: Int): Pair<Boolean, String>
}
