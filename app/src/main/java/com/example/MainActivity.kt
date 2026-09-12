package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.IndustrialSplashScreen
import com.example.ui.components.ScrollableCarouselWithArrows
import com.example.ui.components.TopMachineStatusBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CncViewModel

enum class MainNavTab(val label: String, val icon: ImageVector) {
  CONTROL("Control", Icons.Default.Tune),
  GCODE("G-Code", Icons.Default.PrecisionManufacturing),
  MDI("MDI", Icons.Default.Terminal),
  DIAGNOSTICS("Diagnostics", Icons.Default.Analytics),
  TOOLS("Tools/WCS", Icons.Default.Construction),
  COMMISSIONING("Commissioning", Icons.AutoMirrored.Filled.FactCheck),
  SETTINGS("Settings", Icons.Default.Settings),
  MANUAL("Manual", Icons.Default.MenuBook)
}

class MainActivity : ComponentActivity() {

  private val viewModel: CncViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
      val machineState by viewModel.machineState.collectAsStateWithLifecycle()
      val mcuDiagnostics by viewModel.mcuDiagnostics.collectAsStateWithLifecycle()
      val isEcoMode by viewModel.isEcoMode.collectAsStateWithLifecycle()

      var currentTab by remember { mutableStateOf(MainNavTab.CONTROL) }
      var showSplashScreen by remember { mutableStateOf(true) }

      MyApplicationTheme(darkTheme = isDarkTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
          BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          val isWideScreen = maxWidth >= 840.dp
          val isLandscape = maxWidth > maxHeight
          val useSideNav = isLandscape || isWideScreen

          Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
              TopMachineStatusBar(
                machineState = machineState,
                mcuDiagnostics = mcuDiagnostics,
                isDarkTheme = isDarkTheme,
                onToggleTheme = { viewModel.toggleTheme() },
                onEStopToggle = {
                  if (machineState.isEStopActive) viewModel.resetEStop() else viewModel.triggerEStop()
                },
                onFeedHoldToggle = {
                  if (machineState.isGcodePaused) viewModel.resumeGCode() else viewModel.pauseGCode()
                },
                onDriverToggle = { viewModel.toggleDriverEnable() },
                onClearAlarms = { viewModel.clearAlarms() },
                isLandscape = isLandscape,
                isEcoMode = isEcoMode,
                modifier = Modifier.statusBarsPadding()
              )
            },
            bottomBar = {
              if (!useSideNav) {
                // Horizontally Scrollable Bottom Bar for Portrait Mode
                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                  color = MaterialTheme.colorScheme.surface,
                  tonalElevation = 6.dp
                ) {
                  ScrollableCarouselWithArrows(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 6.dp, vertical = 6.dp),
                    scrollStepDp = 180.dp,
                    arrowSize = 30.dp,
                    iconSize = 18.dp,
                    spacing = 6.dp,
                    testTagPrefix = "nav_bar"
                  ) {
                    MainNavTab.entries.forEach { tab ->
                      val isSelected = currentTab == tab
                      FilterChip(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        leadingIcon = {
                          Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(18.dp)
                          )
                        },
                        label = {
                          Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                            fontSize = 11.sp
                          )
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                          selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                          selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                          .height(38.dp)
                          .testTag("nav_tab_${tab.name.lowercase()}")
                      )
                    }
                  }
                }
              }
            }
          ) { innerPadding ->
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
            ) {
              if (useSideNav) {
                // Vertically Scrollable Side Navigation Rail for Landscape & Tablets
                Surface(
                  modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 72.dp, max = 86.dp)
                    .testTag("nav_rail"),
                  color = MaterialTheme.colorScheme.surface,
                  tonalElevation = 4.dp
                ) {
                  Column(
                    modifier = Modifier
                      .fillMaxHeight()
                      .verticalScroll(rememberScrollState())
                      .padding(vertical = 6.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                  ) {
                    MainNavTab.entries.forEach { tab ->
                      val isSelected = currentTab == tab
                      NavigationRailItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                          Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            modifier = Modifier.size(20.dp)
                          )
                        },
                        label = {
                          Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                          )
                        },
                        modifier = Modifier.testTag("nav_rail_tab_${tab.name.lowercase()}"),
                        colors = NavigationRailItemDefaults.colors(
                          indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                          selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                          selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                      )
                    }
                  }
                }
              }

              Box(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
              ) {
                when (currentTab) {
                  MainNavTab.CONTROL -> ControlScreen(viewModel = viewModel)
                  MainNavTab.GCODE -> GCodeScreen(viewModel = viewModel)
                  MainNavTab.MDI -> MdiScreen(viewModel = viewModel)
                  MainNavTab.DIAGNOSTICS -> DiagnosticsScreen(viewModel = viewModel)
                  MainNavTab.TOOLS -> ToolsWcsScreen(viewModel = viewModel)
                  MainNavTab.COMMISSIONING -> CommissioningScreen(viewModel = viewModel)
                  MainNavTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                  MainNavTab.MANUAL -> ManualScreen(viewModel = viewModel)
                }
              }
            }
          }
        }

          // Industrial Splash Screen with Smooth Fade Transition
          AnimatedVisibility(
            visible = showSplashScreen,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(400))
          ) {
            IndustrialSplashScreen(
              onDismiss = { showSplashScreen = false },
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }
  }
}
