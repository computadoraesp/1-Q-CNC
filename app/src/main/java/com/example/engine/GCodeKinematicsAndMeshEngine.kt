package com.example.engine

import com.example.model.*
import kotlin.math.*

object GCodeKinematicsAndMeshEngine {

  /**
   * Kinematic analyzer for full G-Code inspection and job time estimation
   */
  fun calculateJobEstimate(
    lines: List<String>,
    rapidFeedRateMmMin: Double = 4500.0,
    defaultCuttingFeedRateMmMin: Double = 1200.0
  ): JobTimeEstimate {
    var curX = 0.0
    var curY = 0.0
    var curZ = 15.0
    var curA = 0.0
    var currentFeedRate = defaultCuttingFeedRateMmMin

    var totalRapidSeconds = 0.0
    var totalCutSeconds = 0.0
    var totalDwellSeconds = 0.0

    var rapidDist = 0.0
    var cutDist = 0.0
    var distX = 0.0
    var distY = 0.0
    var distZ = 0.0
    var distA = 0.0

    var minF = Double.MAX_VALUE
    var maxF = 0.0
    var toolChanges = 0
    var cutLines = 0
    var rapidLines = 0

    var minX = Double.MAX_VALUE
    var maxX = -Double.MAX_VALUE
    var minY = Double.MAX_VALUE
    var maxY = -Double.MAX_VALUE
    var minZ = Double.MAX_VALUE
    var maxZ = -Double.MAX_VALUE

    var isAbsolute = true // G90 vs G91

    for (rawLine in lines) {
      val line = rawLine.substringBefore(";").substringBefore("(").trim().uppercase()
      if (line.isEmpty()) continue

      if (line.contains("G90")) isAbsolute = true
      if (line.contains("G91")) isAbsolute = false
      if (line.contains("M6") || line.matches(Regex(".*T\\d+.*"))) toolChanges++

      // Dwell G4 P...
      if (line.contains("G4")) {
        val pMatch = Regex("P([0-9.]+)").find(line)
        val sMatch = Regex("S([0-9.]+)").find(line)
        val dwellSec = pMatch?.groupValues?.get(1)?.toDoubleOrNull()
          ?: sMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.5
        totalDwellSeconds += dwellSec
      }

      // Check feedrate F...
      val fMatch = Regex("F([0-9.]+)").find(line)
      if (fMatch != null) {
        val fVal = fMatch.groupValues[1].toDoubleOrNull()
        if (fVal != null && fVal > 0.0) {
          currentFeedRate = fVal
          if (fVal < minF) minF = fVal
          if (fVal > maxF) maxF = fVal
        }
      }

      val isRapid = line.contains("G0") || line.startsWith("G00")
      val isCut = line.contains("G1") || line.contains("G2") || line.contains("G3") ||
          line.startsWith("G01") || line.startsWith("G02") || line.startsWith("G03")

      if (!isRapid && !isCut) continue

      val xMatch = Regex("X(-?[0-9.]+)").find(line)
      val yMatch = Regex("Y(-?[0-9.]+)").find(line)
      val zMatch = Regex("Z(-?[0-9.]+)").find(line)
      val aMatch = Regex("A(-?[0-9.]+)").find(line)

      val targetX = if (xMatch != null) {
        val v = xMatch.groupValues[1].toDoubleOrNull() ?: curX
        if (isAbsolute) v else curX + v
      } else curX

      val targetY = if (yMatch != null) {
        val v = yMatch.groupValues[1].toDoubleOrNull() ?: curY
        if (isAbsolute) v else curY + v
      } else curY

      val targetZ = if (zMatch != null) {
        val v = zMatch.groupValues[1].toDoubleOrNull() ?: curZ
        if (isAbsolute) v else curZ + v
      } else curZ

      val targetA = if (aMatch != null) {
        val v = aMatch.groupValues[1].toDoubleOrNull() ?: curA
        if (isAbsolute) v else curA + v
      } else curA

      val dx = abs(targetX - curX)
      val dy = abs(targetY - curY)
      val dz = abs(targetZ - curZ)
      val da = abs(targetA - curA)

      val moveDist3D = sqrt(dx * dx + dy * dy + dz * dz)

      distX += dx
      distY += dy
      distZ += dz
      distA += da

      if (targetX < minX) minX = targetX
      if (targetX > maxX) maxX = targetX
      if (targetY < minY) minY = targetY
      if (targetY > maxY) maxY = targetY
      if (targetZ < minZ) minZ = targetZ
      if (targetZ > maxZ) maxZ = targetZ

      if (isRapid) {
        rapidLines++
        rapidDist += moveDist3D
        val timeSec = if (rapidFeedRateMmMin > 0) (moveDist3D / rapidFeedRateMmMin) * 60.0 else 0.0
        totalRapidSeconds += timeSec
      } else if (isCut) {
        cutLines++
        cutDist += moveDist3D
        val effectiveFeed = if (currentFeedRate > 0) currentFeedRate else defaultCuttingFeedRateMmMin
        val timeSec = (moveDist3D / effectiveFeed) * 60.0
        totalCutSeconds += timeSec
      }

      curX = targetX
      curY = targetY
      curZ = targetZ
      curA = targetA
    }

    if (minF == Double.MAX_VALUE) minF = defaultCuttingFeedRateMmMin
    if (minX == Double.MAX_VALUE) { minX = 0.0; maxX = 0.0; minY = 0.0; maxY = 0.0; minZ = 0.0; maxZ = 0.0 }

    return JobTimeEstimate(
      totalTimeSeconds = totalRapidSeconds + totalCutSeconds + totalDwellSeconds,
      rapidTimeSeconds = totalRapidSeconds,
      cutTimeSeconds = totalCutSeconds,
      dwellTimeSeconds = totalDwellSeconds,
      rapidDistanceMm = rapidDist,
      cutDistanceMm = cutDist,
      totalDistanceMm = rapidDist + cutDist,
      axisDistanceX = distX,
      axisDistanceY = distY,
      axisDistanceZ = distZ,
      axisDistanceA = distA,
      minFeedRate = minF,
      maxFeedRate = max(maxF, minF),
      toolChangeCount = toolChanges,
      totalLines = lines.size,
      cutLinesCount = cutLines,
      rapidLinesCount = rapidLines,
      boundsMinX = minX,
      boundsMaxX = maxX,
      boundsMinY = minY,
      boundsMaxY = maxY,
      boundsMinZ = minZ,
      boundsMaxZ = maxZ
    )
  }

  /**
   * Generates initial mesh grid points for probing
   */
  fun generateInitialMeshGrid(config: MeshGridConfig): List<MeshGridPoint> {
    val list = mutableListOf<MeshGridPoint>()
    val stepX = config.stepX
    val stepY = config.stepY

    for (r in 0 until config.gridRows) {
      val y = config.yMin + r * stepY
      for (c in 0 until config.gridCols) {
        val x = config.xMin + c * stepX
        list.add(
          MeshGridPoint(
            gridX = c,
            gridY = r,
            worldX = x,
            worldY = y,
            zOffsetMm = 0.0,
            isProbed = false
          )
        )
      }
    }
    return list
  }

  /**
   * Generates G-Code probing sequence for the mesh grid
   */
  fun generateProbeGCodeSequence(config: MeshGridConfig): List<String> {
    val gcode = mutableListOf<String>()
    gcode.add("(--- UNO Q DROID AUTO-LEVELING MESH PROBE ---)")
    gcode.add("G21 G90 (Metric, Absolute)")
    gcode.add("G0 Z${config.safeZMm} (Move to safe clearance Z)")

    val stepX = config.stepX
    val stepY = config.stepY

    for (r in 0 until config.gridRows) {
      val y = config.yMin + r * stepY
      // Serpentine path for optimal probe travel
      val colRange = if (r % 2 == 0) (0 until config.gridCols) else ((config.gridCols - 1) downTo 0)
      for (c in colRange) {
        val x = config.xMin + c * stepX
        gcode.add("G0 X${String.format(java.util.Locale.US, "%.3f", x)} Y${String.format(java.util.Locale.US, "%.3f", y)}")
        gcode.add("G38.2 Z${config.probeDepthLimitZMm} F${config.probeFeedRateMmMin}")
        gcode.add("G0 Z${config.safeZMm}")
      }
    }
    gcode.add("(--- MESH PROBING COMPLETE ---)")
    return gcode
  }

  /**
   * Interpolate Z offset at any (x, y) coordinate using Bilinear Interpolation
   */
  fun interpolateZOffset(x: Double, y: Double, mesh: AutoLevelMeshState): Double {
    val points = mesh.points
    if (points.isEmpty()) return 0.0

    val config = mesh.config
    val clampedX = x.coerceIn(config.xMin, config.xMax)
    val clampedY = y.coerceIn(config.yMin, config.yMax)

    val stepX = if (config.stepX > 0.0) config.stepX else 1.0
    val stepY = if (config.stepY > 0.0) config.stepY else 1.0

    val colF = (clampedX - config.xMin) / stepX
    val rowF = (clampedY - config.yMin) / stepY

    val col0 = colF.toInt().coerceIn(0, config.gridCols - 1)
    val col1 = (col0 + 1).coerceIn(0, config.gridCols - 1)
    val row0 = rowF.toInt().coerceIn(0, config.gridRows - 1)
    val row1 = (row0 + 1).coerceIn(0, config.gridRows - 1)

    val u = (colF - col0).coerceIn(0.0, 1.0)
    val v = (rowF - row0).coerceIn(0.0, 1.0)

    fun getZ(c: Int, r: Int): Double {
      return points.firstOrNull { it.gridX == c && it.gridY == r }?.zOffsetMm ?: 0.0
    }

    val z00 = getZ(col0, row0)
    val z10 = getZ(col1, row0)
    val z01 = getZ(col0, row1)
    val z11 = getZ(col1, row1)

    // Bilinear formula
    return (1.0 - u) * (1.0 - v) * z00 +
        u * (1.0 - v) * z10 +
        (1.0 - u) * v * z01 +
        u * v * z11
  }

  /**
   * Transforms raw GCode lines with Bilinear Z Auto-Leveling compensation
   */
  fun applyMeshCompensationToGCode(
    originalLines: List<String>,
    meshState: AutoLevelMeshState
  ): List<String> {
    var curX = 0.0
    var curY = 0.0
    var isAbsolute = true

    val compensatedLines = mutableListOf<String>()
    compensatedLines.add("(--- UNO Q DROID AUTO-LEVELED GCODE ---)")
    compensatedLines.add("(Mesh Area: X[${meshState.config.xMin}..${meshState.config.xMax}], Y[${meshState.config.yMin}..${meshState.config.yMax}])")
    compensatedLines.add("(Max Z-Dev: ${String.format(java.util.Locale.US, "%.3f", meshState.peakToPeakZ)}mm)")

    for (raw in originalLines) {
      val trimmed = raw.trim()
      if (trimmed.startsWith("(") || trimmed.startsWith(";")) {
        compensatedLines.add(raw)
        continue
      }
      val upper = trimmed.uppercase()
      if (upper.contains("G90")) isAbsolute = true
      if (upper.contains("G91")) isAbsolute = false

      val hasX = upper.contains("X")
      val hasY = upper.contains("Y")
      val hasZ = upper.contains("Z")

      if (hasX) {
        val match = Regex("X(-?[0-9.]+)").find(upper)
        if (match != null) {
          val v = match.groupValues[1].toDoubleOrNull() ?: curX
          curX = if (isAbsolute) v else curX + v
        }
      }
      if (hasY) {
        val match = Regex("Y(-?[0-9.]+)").find(upper)
        if (match != null) {
          val v = match.groupValues[1].toDoubleOrNull() ?: curY
          curY = if (isAbsolute) v else curY + v
        }
      }

      if (hasZ && isAbsolute) {
        val match = Regex("Z(-?[0-9.]+)").find(upper)
        if (match != null) {
          val originalZ = match.groupValues[1].toDoubleOrNull()
          if (originalZ != null) {
            val zOffset = interpolateZOffset(curX, curY, meshState)
            val compensatedZ = originalZ + zOffset
            val replaced = upper.replace(
              Regex("Z-?[0-9.]+"),
              "Z${String.format(java.util.Locale.US, "%.3f", compensatedZ)}"
            )
            compensatedLines.add(replaced)
            continue
          }
        }
      }

      compensatedLines.add(raw)
    }
    return compensatedLines
  }

  /**
   * Calculates comprehensive axis calibration statistics
   */
  fun calculateCalibrationStats(
    session: AxisCalibrationSession
  ): AxisCalibrationSession {
    val measuredPoints = session.points.filter { it.isMeasured && it.measuredRealMm != null }
    if (measuredPoints.isEmpty()) {
      return session.copy(isCalibrationComplete = false)
    }

    val updatedPoints = session.points.map { pt ->
      if (pt.measuredRealMm != null) {
        val err = pt.measuredRealMm - pt.commandedTargetMm
        val relPct = if (pt.commandedTargetMm != 0.0) (err / pt.commandedTargetMm) * 100.0 else 0.0
        val lostSteps = (err * session.currentStepsPerMm).roundToInt()
        pt.copy(
          errorMm = err,
          relativeErrorPct = relPct,
          estimatedLostSteps = lostSteps,
          isMeasured = true
        )
      } else pt
    }

    val errors = updatedPoints.mapNotNull { it.errorMm }
    val maxDev = errors.maxOfOrNull { abs(it) } ?: 0.0
    val avgDev = if (errors.isNotEmpty()) errors.average() else 0.0

    // Standard deviation
    val variance = if (errors.isNotEmpty()) {
      errors.map { (it - avgDev).pow(2) }.average()
    } else 0.0
    val stdDev = sqrt(variance)

    // Calculate corrected steps per mm
    // Formula: S_new = S_current * (Target_travel / Measured_travel)
    val maxTarget = updatedPoints.maxOfOrNull { it.commandedTargetMm } ?: session.totalStrokeLengthMm
    val matchingMeasured = updatedPoints.lastOrNull { it.commandedTargetMm == maxTarget }?.measuredRealMm
    
    val suggestedSteps: Double? = if (matchingMeasured != null && matchingMeasured > 0.0) {
      val ratio = maxTarget / matchingMeasured
      session.currentStepsPerMm * ratio
    } else if (errors.isNotEmpty()) {
      // Weighted average calculation
      val sumTarget = updatedPoints.filter { it.commandedTargetMm > 0 }.sumOf { it.commandedTargetMm }
      val sumMeasured = updatedPoints.filter { it.commandedTargetMm > 0 }.mapNotNull { it.measuredRealMm }.sum()
      if (sumMeasured > 0) session.currentStepsPerMm * (sumTarget / sumMeasured) else null
    } else null

    val isAllDone = updatedPoints.all { it.isMeasured }

    return session.copy(
      points = updatedPoints,
      maxDeviationMm = maxDev,
      avgDeviationMm = avgDev,
      stdDeviationMm = stdDev,
      suggestedStepsPerMm = suggestedSteps,
      isCalibrationComplete = isAllDone
    )
  }

  /**
   * Initializes a fresh multipoint calibration session for a chosen axis
   */
  fun createInitialCalibrationSession(
    axis: CalibrationAxis,
    totalLengthMm: Double = 300.0,
    intervalPercent: Int = 10,
    currentStepsPerMm: Double = axis.defaultStepsPerMm
  ): AxisCalibrationSession {
    val points = mutableListOf<CalibrationPoint>()
    val stepsCount = 100 / intervalPercent

    for (i in 0..stepsCount) {
      val pct = i * intervalPercent
      val targetMm = (totalLengthMm * pct) / 100.0
      points.add(
        CalibrationPoint(
          stepIndex = i,
          percentOfTravel = pct,
          commandedTargetMm = targetMm,
          measuredRealMm = if (i == 0) 0.0 else null,
          errorMm = if (i == 0) 0.0 else null,
          relativeErrorPct = if (i == 0) 0.0 else null,
          estimatedLostSteps = if (i == 0) 0 else null,
          isMeasured = (i == 0)
        )
      )
    }

    return AxisCalibrationSession(
      axis = axis,
      totalStrokeLengthMm = totalLengthMm,
      stepIntervalPercent = intervalPercent,
      currentStepIndex = 1,
      currentStepsPerMm = currentStepsPerMm,
      points = points,
      suggestedStepsPerMm = null,
      maxDeviationMm = 0.0,
      avgDeviationMm = 0.0,
      stdDeviationMm = 0.0,
      isCalibrationComplete = false
    )
  }
}
