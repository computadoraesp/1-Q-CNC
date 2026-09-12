package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MacroPreset
import com.example.model.MdiHistoryItem
import com.example.ui.theme.*
import com.example.viewmodel.CncViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MdiScreen(
  viewModel: CncViewModel,
  modifier: Modifier = Modifier
) {
  val machineState by viewModel.machineState.collectAsStateWithLifecycle()
  val mdiInput by viewModel.mdiInput.collectAsStateWithLifecycle()
  val mdiHistory by viewModel.mdiHistory.collectAsStateWithLifecycle()

  val macros = remember {
    listOf(
      MacroPreset("m1", "Z-Probe Touch-off", "G38.2 Z-50 F80", "G38.2 Z-50 F80 ; G92 Z15.0", "Precision probe"),
      MacroPreset("m2", "Park Position", "G53 G0 Z0 ; G53 G0 X0 Y250", "G53 G0 Z0\nG53 G0 X0 Y250", "Safe park"),
      MacroPreset("m3", "Work Zero (All)", "G10 L20 P1 X0 Y0 Z0", "G10 L20 P1 X0 Y0 Z0", "Set G54 origin"),
      MacroPreset("m4", "Spindle 12k Warmup", "M3 S12000", "M3 S12000\nG4 P5\nM5", "Warmup cycle"),
      MacroPreset("m5", "Safe Retract", "G0 Z25.0", "G0 Z25.000", "Retract Z safe"),
      MacroPreset("m6", "Cancel Offsets", "G92.1", "G92.1", "Clear temp offsets")
    )
  }

  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // MDI Input Bar Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(10.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = "MANUAL DATA INPUT (MDI) TERMINAL",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = "Direct G-Code / M-Code interpreter for LinuxCNC & STM32 motion pipeline",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = mdiInput,
            onValueChange = { viewModel.updateMdiInput(it) },
            placeholder = { Text("e.g. G0 X50 Y25 Z10 F1200 or M3 S15000", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
            modifier = Modifier
              .weight(1f)
              .testTag("mdi_text_field"),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { viewModel.submitMdiCommand() }),
            shape = RoundedCornerShape(8.dp)
          )

          Spacer(modifier = Modifier.width(8.dp))

          Button(
            onClick = { viewModel.submitMdiCommand() },
            modifier = Modifier
              .height(56.dp)
              .testTag("mdi_execute_btn"),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = Color.Black
            )
          ) {
            Icon(Icons.Default.Send, contentDescription = "Execute MDI")
            Spacer(modifier = Modifier.width(4.dp))
            Text("EXEC", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Quick Macros Grid
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(10.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = "OPERATOR QUICK MACROS",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 2x3 Grid of Macro Buttons
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          for (chunk in macros.chunked(2)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              for (macro in chunk) {
                OutlinedButton(
                  onClick = { viewModel.executeMacro(macro.gcodeSnippet) },
                  modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("macro_btn_${macro.id}"),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = macro.label,
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1
                    )
                    Text(
                      text = macro.description,
                      style = MaterialTheme.typography.labelSmall,
                      fontFamily = FontFamily.Monospace,
                      fontSize = 9.sp,
                      color = DroCyan,
                      maxLines = 1
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // MDI Execution History Terminal
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .height(260.dp),
      colors = CardDefaults.cardColors(containerColor = DroBackground),
      shape = RoundedCornerShape(10.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "LINUXCNC HAL COMMAND LOG",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "${mdiHistory.size} EVENTS",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = DroGreen
          )
        }

        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(mdiHistory) { item ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF111827))
                .padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.Top
            ) {
              Text(
                text = item.timestamp,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.width(55.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "> ${item.command}",
                  style = MaterialTheme.typography.labelSmall,
                  fontFamily = FontFamily.Monospace,
                  color = DroWhite,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = item.responseText,
                  style = MaterialTheme.typography.labelSmall,
                  fontFamily = FontFamily.Monospace,
                  color = if (item.isSuccess) DroGreen else IndDarkEmergency,
                  fontSize = 10.sp
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}
