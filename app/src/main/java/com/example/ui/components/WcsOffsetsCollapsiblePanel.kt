package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MachineState
import com.example.model.WcsOffset
import com.example.ui.theme.*
import java.util.Locale

/**
 * Collapsible Work Coordinate Offsets (G54 - G59) Panel for CNC Dashboard.
 * Allows live inspection, switching, individual axis zeroing, touch-off,
 * and batch reset of fixture offsets via direct serial G-code commands (G10 L2 / G10 L20).
 */
@Composable
fun WcsOffsetsCollapsiblePanel(
  machineState: MachineState,
  wcsList: List<WcsOffset>,
  onSelectWcs: (String) -> Unit,
  onResetWcsOffset: (String) -> Unit,
  onResetWcsAxis: (String, String) -> Unit,
  onTouchOffAxis: (String, String, Double) -> Unit,
  onResetAllWcs: () -> Unit,
  onQueryControllerOffsets: () -> Unit,
  modifier: Modifier = Modifier,
  initiallyExpanded: Boolean = false
) {
  var isExpanded by remember { mutableStateOf(initiallyExpanded) }
  var inspectedWcsCode by remember(machineState.wcsName) { mutableStateOf(machineState.wcsName) }
  var showResetAllConfirmDialog by remember { mutableStateOf(false) }

  // Rotation animation for collapse/expand chevron icon
  val chevronRotation by animateFloatAsState(
    targetValue = if (isExpanded) 180f else 0f,
    label = "chevronRotation"
  )

  val inspectedWcs = wcsList.find { it.code.equals(inspectedWcsCode, ignoreCase = true) }
    ?: wcsList.firstOrNull()
    ?: WcsOffset("G54", "Main Fixture", 0.0, 0.0, 0.0, 0.0, true)

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag("wcs_offsets_collapsible_panel"),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp)
    ) {
      // 1. COLLAPSIBLE PANEL HEADER (Clickable to expand/collapse)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .clickable { isExpanded = !isExpanded }
          .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.GridOn,
              contentDescription = "WCS Offsets",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(18.dp)
            )
          }

          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "WORK COORDINATE OFFSETS",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.5.sp
              )
              Spacer(modifier = Modifier.width(6.dp))
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
              ) {
                Text(
                  text = "G54 - G59",
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
              }
            }

            Text(
              text = "ACTIVE: ${machineState.wcsName}  |  OFFSETS: X${formatCoord(inspectedWcs.offsetX)} Y${formatCoord(inspectedWcs.offsetY)} Z${formatCoord(inspectedWcs.offsetZ)}",
              style = MaterialTheme.typography.bodySmall,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.SemiBold,
              color = DroCyan
            )
          }
        }

        // Action Header Icons: Query $# + Expand/Collapse Chevron
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          IconButton(
            onClick = onQueryControllerOffsets,
            modifier = Modifier
              .size(32.dp)
              .testTag("wcs_query_offsets_button")
          ) {
            Icon(
              imageVector = Icons.Default.Sync,
              contentDescription = "Read offsets from CNC (\$#)",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }

          IconButton(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier
              .size(32.dp)
              .testTag("wcs_toggle_expand_button")
          ) {
            Icon(
              imageVector = Icons.Default.KeyboardArrowDown,
              contentDescription = if (isExpanded) "Collapse" else "Expand",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier
                .size(22.dp)
                .rotate(chevronRotation)
            )
          }
        }
      }

      // Compact Quick Selector Chips (Shown even when collapsed or expanded)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        wcsList.take(6).forEach { wcs ->
          val isCurrentlyActive = wcs.code.equals(machineState.wcsName, ignoreCase = true)
          val isCurrentlyInspected = wcs.code.equals(inspectedWcsCode, ignoreCase = true)

          Surface(
            onClick = {
              inspectedWcsCode = wcs.code
              onSelectWcs(wcs.code)
            },
            shape = RoundedCornerShape(6.dp),
            color = when {
              isCurrentlyActive -> MaterialTheme.colorScheme.primary
              isCurrentlyInspected -> MaterialTheme.colorScheme.primaryContainer
              else -> MaterialTheme.colorScheme.surfaceVariant
            },
            border = BorderStroke(
              1.dp,
              if (isCurrentlyActive) DroGreen else if (isCurrentlyInspected) MaterialTheme.colorScheme.primary else Color.Transparent
            ),
            modifier = Modifier
              .height(30.dp)
              .testTag("wcs_chip_${wcs.code}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              if (isCurrentlyActive) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                )
              }
              Text(
                text = wcs.code,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = when {
                  isCurrentlyActive -> Color.Black
                  isCurrentlyInspected -> MaterialTheme.colorScheme.onPrimaryContainer
                  else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
              )
            }
          }
        }
      }

      // 2. EXPANDED WORK COORDINATE OFFSETS MANAGEMENT AREA
      AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          // Sub-Header for Inspected Fixture
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(if (inspectedWcs.code == machineState.wcsName) DroGreen else MaterialTheme.colorScheme.primaryContainer)
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = inspectedWcs.code,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Black,
                  color = if (inspectedWcs.code == machineState.wcsName) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer
                )
              }

              Text(
                text = inspectedWcs.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            // Quick G-Code command label
            Text(
              text = "G10 L2 / G10 L20",
              style = MaterialTheme.typography.labelSmall,
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              color = IndDarkTextMuted
            )
          }

          // 4-AXIS OFFSET READOUT & DEDICATED ZERO / TOUCH-OFF ACTIONS
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            WcsAxisOffsetRow(
              axisName = "X",
              offsetValue = inspectedWcs.offsetX,
              currentPos = machineState.posX,
              accentColor = DroAmber,
              onZeroOffset = { onResetWcsAxis(inspectedWcs.code, "X") },
              onTouchOff = { onTouchOffAxis(inspectedWcs.code, "X", 0.0) }
            )

            WcsAxisOffsetRow(
              axisName = "Y",
              offsetValue = inspectedWcs.offsetY,
              currentPos = machineState.posY,
              accentColor = DroCyan,
              onZeroOffset = { onResetWcsAxis(inspectedWcs.code, "Y") },
              onTouchOff = { onTouchOffAxis(inspectedWcs.code, "Y", 0.0) }
            )

            WcsAxisOffsetRow(
              axisName = "Z",
              offsetValue = inspectedWcs.offsetZ,
              currentPos = machineState.posZ,
              accentColor = DroGreen,
              onZeroOffset = { onResetWcsAxis(inspectedWcs.code, "Z") },
              onTouchOff = { onTouchOffAxis(inspectedWcs.code, "Z", 0.0) }
            )

            WcsAxisOffsetRow(
              axisName = "A",
              offsetValue = inspectedWcs.offsetA,
              currentPos = machineState.posA,
              accentColor = IndDarkWarning,
              unit = "°",
              onZeroOffset = { onResetWcsAxis(inspectedWcs.code, "A") },
              onTouchOff = { onTouchOffAxis(inspectedWcs.code, "A", 0.0) }
            )
          }

          // BATCH RESET & ACTION BUTTONS
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // 1. Reset current inspected WCS (X/Y/Z/A = 0.000)
            Button(
              onClick = { onResetWcsOffset(inspectedWcs.code) },
              shape = RoundedCornerShape(8.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
              ),
              modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .testTag("reset_wcs_${inspectedWcs.code}_button"),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
              Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "RESET ${inspectedWcs.code} (X0 Y0 Z0 A0)",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black
              )
            }

            // 2. Touch Off All at Current Spindle Location
            FilledTonalButton(
              onClick = {
                onTouchOffAxis(inspectedWcs.code, "X", 0.0)
                onTouchOffAxis(inspectedWcs.code, "Y", 0.0)
                onTouchOffAxis(inspectedWcs.code, "Z", 0.0)
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .testTag("touch_off_all_${inspectedWcs.code}_button"),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
              Icon(
                imageVector = Icons.Default.GpsFixed,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "TOUCH-OFF ALL (G10 L20)",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black
              )
            }
          }

          // 3. COMPLETE FIXTURE OVERVIEW TABLE (G54 - G59)
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = DroBackground,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "ALL FIXTURES OFFSET TABLE (G54 - G59)",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  fontSize = 9.sp,
                  color = IndDarkTextMuted
                )

                // Master Reset All Button
                TextButton(
                  onClick = { showResetAllConfirmDialog = true },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                  modifier = Modifier.height(24.dp)
                ) {
                  Text(
                    text = "RESET ALL G54-G59",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = IndDarkEmergency
                  )
                }
              }

              // Mini fixture rows
              wcsList.take(6).forEach { item ->
                val isActive = item.code.equals(machineState.wcsName, ignoreCase = true)
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isActive) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                    .clickable {
                      inspectedWcsCode = item.code
                      onSelectWcs(item.code)
                    }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Text(
                      text = item.code,
                      style = MaterialTheme.typography.labelSmall,
                      fontFamily = FontFamily.Monospace,
                      fontWeight = FontWeight.Black,
                      fontSize = 10.sp,
                      color = if (isActive) DroGreen else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = item.name,
                      style = MaterialTheme.typography.labelSmall,
                      fontSize = 9.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Text(
                      text = "X:${formatCoord(item.offsetX)} Y:${formatCoord(item.offsetY)} Z:${formatCoord(item.offsetZ)}",
                      style = MaterialTheme.typography.labelSmall,
                      fontSize = 9.sp,
                      fontFamily = FontFamily.Monospace,
                      color = if (isActive) DroCyan else IndDarkTextMuted
                    )

                    // Quick Zero Offset Button for this item
                    IconButton(
                      onClick = { onResetWcsOffset(item.code) },
                      modifier = Modifier.size(20.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Zero ${item.code}",
                        tint = IndDarkTextMuted,
                        modifier = Modifier.size(12.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Safety Confirmation Dialog for Resetting ALL WCS (G54 - G59)
  if (showResetAllConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showResetAllConfirmDialog = false },
      icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = IndDarkEmergency) },
      title = { Text("Reset All Coordinate Offsets (G54 - G59)?") },
      text = {
        Text(
          "This will reset all fixture offset tables (G54, G55, G56, G57, G58, G59) to X0 Y0 Z0 A0 on the CNC controller. Existing part origin calibrations will be cleared."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onResetAllWcs()
            showResetAllConfirmDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = IndDarkEmergency)
        ) {
          Text("RESET ALL FIXTURES", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetAllConfirmDialog = false }) {
          Text("CANCEL")
        }
      }
    )
  }
}

@Composable
private fun WcsAxisOffsetRow(
  axisName: String,
  offsetValue: Double,
  currentPos: Double,
  accentColor: Color,
  unit: String = "mm",
  onZeroOffset: () -> Unit,
  onTouchOff: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = DroBackground,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Axis Badge & Label
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(accentColor.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = axisName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            color = accentColor
          )
        }

        Column {
          Text(
            text = "$axisName OFFSET",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = IndDarkTextMuted
          )
          Text(
            text = "${formatCoord(offsetValue)} $unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = DroWhite
          )
        }
      }

      // Action Buttons: Touch-Off & Zero Offset (G10 L2)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Touch-off at current position
        OutlinedButton(
          onClick = onTouchOff,
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
          modifier = Modifier
            .height(28.dp)
            .testTag("touch_off_${axisName}_button"),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
          Icon(
            imageVector = Icons.Default.GpsFixed,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = DroCyan
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = "TOUCH-OFF",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        // Reset Offset to 0.000
        FilledTonalButton(
          onClick = onZeroOffset,
          shape = RoundedCornerShape(6.dp),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
          modifier = Modifier
            .height(28.dp)
            .testTag("zero_offset_${axisName}_button"),
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
          )
        ) {
          Icon(
            imageVector = Icons.Default.RestartAlt,
            contentDescription = null,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(2.dp))
          Text(
            text = "ZERO (0.0)",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

private fun formatCoord(value: Double): String {
  return String.format(Locale.US, "%+08.3f", value)
}
