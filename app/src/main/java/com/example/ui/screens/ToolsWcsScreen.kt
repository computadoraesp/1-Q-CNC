package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ToolItem
import com.example.model.WcsOffset
import com.example.ui.theme.*
import com.example.viewmodel.CncViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Locale

@Composable
fun ToolsWcsScreen(
  viewModel: CncViewModel,
  modifier: Modifier = Modifier
) {
  val machineState by viewModel.machineState.collectAsStateWithLifecycle()
  val wcsList by viewModel.wcsList.collectAsStateWithLifecycle()
  val toolTable by viewModel.toolTable.collectAsStateWithLifecycle()

  var selectedTab by remember { mutableIntStateOf(0) } // 0 = WCS, 1 = Tool Table
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Tab selector (WCS vs Tool Table)
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.primary,
      modifier = Modifier.clip(RoundedCornerShape(10.dp))
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text("WORK COORDINATES (WCS)", fontWeight = FontWeight.Bold) }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text("TOOL TABLE (T1..T12)", fontWeight = FontWeight.Bold) }
      )
    }

    if (selectedTab == 0) {
      // Work Coordinate Systems (G54 - G59)
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "ACTIVE FIXTURE COORDINATE SYSTEMS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Configured in LinuxCNC var file (G54 through G59.3)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(10.dp))

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            wcsList.forEach { wcs ->
              val isActive = wcs.code == machineState.wcsName
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant)
                  .border(
                    width = if (isActive) 1.5.dp else 0.dp,
                    color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                  )
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(36.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .background(if (isActive) DroGreen else Color.DarkGray),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = wcs.code,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Black,
                      color = if (isActive) Color.Black else Color.White
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = wcs.name,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = "X: ${String.format(Locale.US, "%.3f", wcs.offsetX)}  Y: ${String.format(Locale.US, "%.3f", wcs.offsetY)}  Z: ${String.format(Locale.US, "%.3f", wcs.offsetZ)}",
                      style = MaterialTheme.typography.labelSmall,
                      fontFamily = FontFamily.Monospace,
                      color = DroCyan
                    )
                  }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  OutlinedButton(
                    onClick = { viewModel.resetWcsOffset(wcs.code) },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                  ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("ZERO", style = MaterialTheme.typography.labelSmall)
                  }

                  if (!isActive) {
                    Button(
                      onClick = { viewModel.setWcs(wcs.code) },
                      shape = RoundedCornerShape(6.dp),
                      contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                      modifier = Modifier.height(32.dp)
                    ) {
                      Text("ACTIVATE", style = MaterialTheme.typography.labelSmall)
                    }
                  } else {
                    Box(
                      modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(DroGreen)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                      Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }
    } else {
      // Tool Table
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "LINUXCNC TOOL OFFSET TABLE",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "Registered cutters and tool geometry offsets for G43 height compensation",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(10.dp))

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            toolTable.forEach { tool ->
              val isCurrentTool = tool.toolNumber == machineState.currentToolNumber
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isCurrentTool) DroCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                  .border(
                    width = if (isCurrentTool) 1.dp else 0.dp,
                    color = if (isCurrentTool) DroCyan else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                  )
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(34.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .background(if (isCurrentTool) DroCyan else Color.DarkGray),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "T${tool.toolNumber}",
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Black,
                      color = if (isCurrentTool) Color.Black else Color.White
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = "${tool.toolType} (Ø ${tool.diameterMm}mm)",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = "${tool.description} • Length: ${tool.lengthOffsetMm}mm",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                if (isCurrentTool) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(4.dp))
                      .background(DroCyan)
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text("LOADED", style = MaterialTheme.typography.labelSmall, color = Color.Black, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}
