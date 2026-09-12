package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Reusable horizontal scrollable carousel with subtle, intuitive navigation arrows (< and >).
 * Gives users immediate visual feedback that the menu is horizontally scrollable,
 * while allowing both manual swipe gestures and tap-to-scroll navigation.
 */
@Composable
fun ScrollableCarouselWithArrows(
  modifier: Modifier = Modifier,
  scrollState: ScrollState = rememberScrollState(),
  scrollStepDp: Dp = 160.dp,
  arrowSize: Dp = 28.dp,
  iconSize: Dp = 16.dp,
  spacing: Dp = 6.dp,
  testTagPrefix: String = "carousel",
  content: @Composable RowScope.() -> Unit
) {
  val coroutineScope = rememberCoroutineScope()
  val density = LocalDensity.current

  val canScrollLeft by remember {
    derivedStateOf { scrollState.value > 1 }
  }
  val canScrollRight by remember {
    derivedStateOf { scrollState.value < scrollState.maxValue - 1 }
  }

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Left Arrow (<)
    Surface(
      shape = RoundedCornerShape(6.dp),
      color = if (canScrollLeft) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
      } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
      },
      border = BorderStroke(
        1.dp,
        if (canScrollLeft) {
          MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        } else {
          MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        }
      ),
      modifier = Modifier
        .size(arrowSize)
        .testTag("${testTagPrefix}_arrow_left"),
      onClick = {
        if (canScrollLeft) {
          coroutineScope.launch {
            val px = with(density) { scrollStepDp.toPx() }
            scrollState.animateScrollTo((scrollState.value - px).toInt().coerceAtLeast(0))
          }
        }
      },
      enabled = canScrollLeft
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Default.ChevronLeft,
          contentDescription = "Desplazar a la izquierda",
          tint = if (canScrollLeft) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
          },
          modifier = Modifier.size(iconSize)
        )
      }
    }

    Spacer(modifier = Modifier.width(4.dp))

    // Scrollable Content
    Row(
      modifier = Modifier
        .weight(1f)
        .horizontalScroll(scrollState),
      horizontalArrangement = Arrangement.spacedBy(spacing),
      verticalAlignment = Alignment.CenterVertically,
      content = content
    )

    Spacer(modifier = Modifier.width(4.dp))

    // Right Arrow (>)
    Surface(
      shape = RoundedCornerShape(6.dp),
      color = if (canScrollRight) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
      } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
      },
      border = BorderStroke(
        1.dp,
        if (canScrollRight) {
          MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        } else {
          MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        }
      ),
      modifier = Modifier
        .size(arrowSize)
        .testTag("${testTagPrefix}_arrow_right"),
      onClick = {
        if (canScrollRight) {
          coroutineScope.launch {
            val px = with(density) { scrollStepDp.toPx() }
            scrollState.animateScrollTo((scrollState.value + px).toInt().coerceAtMost(scrollState.maxValue))
          }
        }
      },
      enabled = canScrollRight
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Default.ChevronRight,
          contentDescription = "Desplazar a la derecha",
          tint = if (canScrollRight) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
          },
          modifier = Modifier.size(iconSize)
        )
      }
    }
  }
}
