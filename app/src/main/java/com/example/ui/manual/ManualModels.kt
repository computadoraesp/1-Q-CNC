package com.example.ui.manual

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.model.AppLanguage

data class ManualOverviewSection(
  val title: String,
  val subtitle: String,
  val paragraphs: List<String>,
  val features: List<ManualFeatureHighlight>
)

data class ManualFeatureHighlight(
  val title: String,
  val description: String,
  val iconName: String
)

data class ManualConnectionGuide(
  val title: String,
  val subtitle: String,
  val methods: List<ConnectionMethodGuide>,
  val troubleshootingTitle: String,
  val troubleshootingItems: List<TroubleshootingStep>
)

data class ConnectionMethodGuide(
  val id: String,
  val name: String,
  val badge: String,
  val hardwareRequirements: String,
  val stepByStep: List<String>,
  val recommendedSettings: String
)

data class TroubleshootingStep(
  val problem: String,
  val cause: String,
  val solution: String
)

data class ManualControlItem(
  val name: String,
  val controlType: String, // Button, Input Field, Slider, DRO Display, Toggle, Dropdown, Indicator
  val menuLocation: String, // Top Bar, Control, G-Code, MDI, Tools/WCS, Diagnostics, Commissioning, Settings
  val gcodeOrCommand: String?,
  val inputFormat: String? = null,
  val description: String,
  val safetyNotes: String? = null
)

data class ManualMenuCategory(
  val id: String,
  val title: String,
  val subtitle: String,
  val badgeColor: Color,
  val items: List<ManualControlItem>
)

data class ManualFullContent(
  val language: AppLanguage,
  val screenTitle: String,
  val screenSubtitle: String,
  val tabOverview: String,
  val tabConnection: String,
  val tabControls: String,
  val searchPlaceholder: String,
  val categoryAll: String,
  val emptySearchResults: String,
  val overview: ManualOverviewSection,
  val connectionGuide: ManualConnectionGuide,
  val menuCategories: List<ManualMenuCategory>
)
