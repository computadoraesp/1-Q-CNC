package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppLanguage
import com.example.ui.components.ScrollableCarouselWithArrows
import com.example.ui.manual.*
import com.example.ui.theme.*
import com.example.viewmodel.CncViewModel

@Composable
fun ManualScreen(
  viewModel: CncViewModel,
  modifier: Modifier = Modifier
) {
  val currentLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
  val manualContent = remember(currentLanguage) {
    ManualContentProvider.getContent(currentLanguage)
  }

  var selectedTab by remember { mutableIntStateOf(0) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("ALL") }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Top Bar: Language Selector Strip
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
      ScrollableCarouselWithArrows(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 6.dp, vertical = 6.dp),
        scrollStepDp = 150.dp,
        arrowSize = 28.dp,
        iconSize = 16.dp,
        spacing = 6.dp,
        testTagPrefix = "manual_lang"
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.padding(end = 4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Translate,
            contentDescription = "Language",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = "LANGUAGE:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        AppLanguage.entries.forEach { lang ->
          val isSelected = currentLanguage == lang
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
              .clickable { viewModel.setLanguage(lang) }
              .testTag("lang_btn_${lang.code}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(text = lang.flag, fontSize = 12.sp)
              Text(
                text = "${lang.displayName}${if (lang == AppLanguage.EN) " (Def)" else ""}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
                fontSize = 11.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }

    // Title Card Banner
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(10.dp),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = manualContent.screenTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = manualContent.screenSubtitle,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // Main 3-Pillar Section Tabs
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = MaterialTheme.colorScheme.surface,
      contentColor = MaterialTheme.colorScheme.primary
    ) {
      Tab(
        selected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        text = { Text(manualContent.tabOverview, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        icon = { Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp)) }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = { Text(manualContent.tabConnection, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        icon = { Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(18.dp)) }
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        text = { Text(manualContent.tabControls, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        icon = { Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(18.dp)) }
      )
    }

    // Content Based on Selected Tab
    when (selectedTab) {
      0 -> ManualOverviewView(overview = manualContent.overview)
      1 -> ManualConnectionGuideView(guide = manualContent.connectionGuide)
      2 -> ManualControlsView(
        content = manualContent,
        searchQuery = searchQuery,
        onSearchChange = { searchQuery = it },
        selectedCategory = selectedCategory,
        onCategoryChange = { selectedCategory = it }
      )
    }
  }
}

// ==========================================
// PILLAR 1: OVERVIEW COMPOSABLE
// ==========================================
@Composable
private fun ManualOverviewView(overview: ManualOverviewSection) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = overview.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = overview.subtitle,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = DroAmber
          )

          overview.paragraphs.forEach { paragraph ->
            Text(
              text = paragraph,
              style = MaterialTheme.typography.bodyMedium,
              fontSize = 12.sp,
              lineHeight = 18.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }

    item {
      Text(
        text = "CORE ARCHITECTURE & CAPABILITIES",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    items(overview.features) { feature ->
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(DroCyan.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = when (feature.iconName) {
                "dro" -> Icons.Default.Speed
                "jog" -> Icons.Default.Gamepad
                "gcode" -> Icons.Default.PlayCircle
                "mdi" -> Icons.Default.Terminal
                "tools" -> Icons.Default.PrecisionManufacturing
                else -> Icons.Default.Build
              },
              contentDescription = null,
              tint = DroCyan,
              modifier = Modifier.size(20.dp)
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = feature.title,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = feature.description,
              style = MaterialTheme.typography.bodySmall,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

// ==========================================
// PILLAR 2: CONNECTION GUIDE COMPOSABLE
// ==========================================
@Composable
private fun ManualConnectionGuideView(guide: ManualConnectionGuide) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = guide.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Black,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = guide.subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    items(guide.methods, key = { it.id }) { method ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = method.name,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Black,
              color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = DroGreen.copy(alpha = 0.15f)
            ) {
              Text(
                text = method.badge,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = DroGreen,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          // Hardware specs
          Text(
            text = "HARDWARE REQUIREMENTS:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = DroCyan
          )
          Text(
            text = method.hardwareRequirements,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
          )

          // Step by step
          Text(
            text = "STEP-BY-STEP PROCEDURE:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = DroCyan
          )
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            method.stepByStep.forEach { step ->
              Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ArrowRight,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Text(
                  text = step,
                  style = MaterialTheme.typography.bodySmall,
                  fontSize = 11.sp,
                  lineHeight = 15.sp,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }

          // Recommended parameters
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "CONFIG: ${method.recommendedSettings}",
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp,
              color = DroAmber,
              modifier = Modifier.padding(8.dp)
            )
          }
        }
      }
    }

    // Troubleshooting Section
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, IndDarkWarning.copy(alpha = 0.5f))
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = IndDarkWarning)
            Text(
              text = guide.troubleshootingTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Black,
              color = IndDarkWarning
            )
          }

          guide.troubleshootingItems.forEach { item ->
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text(
                  text = "• ${item.problem}",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "Causa: ${item.cause}",
                  style = MaterialTheme.typography.bodySmall,
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                  text = "Solución: ${item.solution}",
                  style = MaterialTheme.typography.bodySmall,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Medium,
                  color = DroGreen
                )
              }
            }
          }
        }
      }
    }
  }
}

// ==========================================
// PILLAR 3: CONTROLS & MENUS COMPOSABLE
// ==========================================
@Composable
private fun ManualControlsView(
  content: ManualFullContent,
  searchQuery: String,
  onSearchChange: (String) -> Unit,
  selectedCategory: String,
  onCategoryChange: (String) -> Unit
) {
  val allCategories = remember(content) {
    listOf(content.categoryAll) + content.menuCategories.map { it.title }
  }

  val filteredCategories = remember(searchQuery, selectedCategory, content) {
    content.menuCategories.mapNotNull { category ->
      val matchesCategory = selectedCategory == content.categoryAll || category.title == selectedCategory
      if (!matchesCategory) return@mapNotNull null

      val matchedItems = category.items.filter { item ->
        if (searchQuery.isBlank()) true else {
          item.name.contains(searchQuery, ignoreCase = true) ||
            item.description.contains(searchQuery, ignoreCase = true) ||
            item.controlType.contains(searchQuery, ignoreCase = true) ||
            (item.gcodeOrCommand?.contains(searchQuery, ignoreCase = true) == true) ||
            (item.inputFormat?.contains(searchQuery, ignoreCase = true) == true)
        }
      }

      if (matchedItems.isNotEmpty()) {
        category.copy(items = matchedItems)
      } else {
        null
      }
    }
  }

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Search Input Field
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchChange,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("manual_search_field"),
      placeholder = { Text(content.searchPlaceholder, fontSize = 11.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { onSearchChange("") }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear")
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(8.dp)
    )

    // Menu Category Filter Chips
    ScrollableCarouselWithArrows(
      modifier = Modifier.fillMaxWidth(),
      scrollStepDp = 160.dp,
      arrowSize = 28.dp,
      iconSize = 16.dp,
      spacing = 6.dp,
      testTagPrefix = "manual_cat"
    ) {
      allCategories.forEach { cat ->
        FilterChip(
          selected = selectedCategory == cat,
          onClick = { onCategoryChange(cat) },
          label = { Text(cat, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
          shape = RoundedCornerShape(6.dp)
        )
      }
    }

    // Results List
    if (filteredCategories.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(44.dp), tint = Color.Gray)
          Spacer(modifier = Modifier.height(8.dp))
          Text(content.emptySearchResults, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        filteredCategories.forEach { category ->
          item(key = category.id) {
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = RoundedCornerShape(10.dp),
              border = CardDefaults.outlinedCardBorder()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                // Category Header
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(28.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .background(category.badgeColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = category.badgeColor, modifier = Modifier.size(16.dp))
                  }
                  Column {
                    Text(
                      text = category.title,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Black,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = category.subtitle,
                      style = MaterialTheme.typography.labelSmall,
                      fontSize = 9.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Items inside category
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  category.items.forEach { item ->
                    ControlItemCard(item = item, categoryColor = category.badgeColor)
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

@Composable
private fun ControlItemCard(item: ManualControlItem, categoryColor: Color) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Line 1: Name, Type Badge & Location
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = item.name,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Black,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurface
        )

        Surface(
          shape = RoundedCornerShape(4.dp),
          color = categoryColor.copy(alpha = 0.18f)
        ) {
          Text(
            text = item.controlType,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = categoryColor,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
          )
        }
      }

      // Line 2: G-Code / Command Syntax & Input Format (if any)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (item.gcodeOrCommand != null) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF0F172A)
          ) {
            Text(
              text = "CMD: ${item.gcodeOrCommand}",
              fontFamily = FontFamily.Monospace,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = DroCyan,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        if (item.inputFormat != null) {
          Text(
            text = "Entrada: ${item.inputFormat}",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // Line 3: Detailed Description
      Text(
        text = item.description,
        style = MaterialTheme.typography.bodySmall,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = MaterialTheme.colorScheme.onSurface
      )

      // Line 4: Safety Warning (if present)
      if (item.safetyNotes != null) {
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = IndDarkEmergency.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, IndDarkEmergency.copy(alpha = 0.4f))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = IndDarkEmergency,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = item.safetyNotes,
              style = MaterialTheme.typography.labelSmall,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = IndDarkEmergency
            )
          }
        }
      }
    }
  }
}
