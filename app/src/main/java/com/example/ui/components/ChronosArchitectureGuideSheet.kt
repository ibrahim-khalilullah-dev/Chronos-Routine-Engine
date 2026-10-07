package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoutineTask
import com.example.ui.theme.AnchorGradientStart
import com.example.ui.theme.ElasticGradientStart
import com.example.ui.theme.OneUiBorder
import com.example.ui.theme.OneUiSurface
import com.example.ui.theme.OneUiSurfaceElevated
import com.example.ui.theme.OneUiSurfaceHigh
import com.example.ui.theme.SleepGradientStart
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChronosArchitectureGuideSheet(
  anchors: List<RoutineTask> = emptyList(),
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = OneUiSurface,
    contentColor = TextPrimary,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(vertical = 12.dp)
          .size(width = 38.dp, height = 4.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.25f))
      )
    }
  ) {
    Column(
      modifier = modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 8.dp)
        .padding(bottom = 40.dp)
        .testTag("chronos_architecture_guide_sheet")
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(AnchorGradientStart.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.MenuBook,
              contentDescription = null,
              tint = AnchorGradientStart,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "How Chronos Works",
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "Architecture & Engine Guide",
              fontSize = 12.sp,
              color = TextSecondary
            )
          }
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("dismiss_guide_button")
        ) {
          Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Section 1: The 7 Immutable Anchors (Dynamically renders real-time titles and timings)
      GuideSectionCard(
        icon = Icons.Default.Lock,
        iconTint = AnchorGradientStart,
        title = "1. The 7 Immutable Anchors",
        subtitle = "Zero-Encroachment Daily Pillars"
      ) {
        Text(
          text = "Chronos is built around 7 core daily pillars that establish the circadian rhythm of your day. Dynamic tasks will NEVER overwrite or compress these anchors.",
          color = TextSecondary,
          fontSize = 13.sp,
          lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        val anchor1 = anchors.find { it.slotNumber == 1 }
        val anchor2 = anchors.find { it.slotNumber == 2 }
        val anchor3 = anchors.find { it.slotNumber == 3 }
        val anchor4 = anchors.find { it.slotNumber == 4 }
        val anchor5 = anchors.find { it.slotNumber == 5 }
        val anchor6 = anchors.find { it.slotNumber == 6 }
        val anchor7 = anchors.find { it.slotNumber == 7 }

        DynamicAnchorPillRow(
          slot = 1,
          task = anchor1,
          defaultName = "Reverse Sleep",
          desc = "Auto-links backwards from Slot 2 wake time with midnight rollover math (% 1440).",
          color = SleepGradientStart
        )
        DynamicAnchorPillRow(
          slot = 2,
          task = anchor2,
          defaultName = "Wake & Morning",
          desc = "Fixed morning anchor initiating circadian activation.",
          color = AnchorGradientStart
        )
        DynamicAnchorPillRow(
          slot = 3,
          task = anchor3,
          defaultName = "Deep Focus 1",
          desc = "High-energy morning productivity block.",
          color = AnchorGradientStart
        )
        DynamicAnchorPillRow(
          slot = 4,
          task = anchor4,
          defaultName = "Midday Reset",
          desc = "Nutritional & cognitive restorative window.",
          color = AnchorGradientStart
        )
        DynamicAnchorPillRow(
          slot = 5,
          task = anchor5,
          defaultName = "Elastic Rubber Buffer",
          desc = "Auto-scales between Slot 4 & 6 with a strict 15m minimum floor.",
          color = ElasticGradientStart
        )
        DynamicAnchorPillRow(
          slot = 6,
          task = anchor6,
          defaultName = "Evening Reset",
          desc = "Day closure and decompression transition.",
          color = AnchorGradientStart
        )
        DynamicAnchorPillRow(
          slot = 7,
          task = anchor7,
          defaultName = "Night Rest",
          desc = "Pre-sleep buffer establishing biological sleep hygiene.",
          color = AnchorGradientStart
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Section 2: Slot 5 Elastic Buffer
      GuideSectionCard(
        icon = Icons.Default.Shield,
        iconTint = ElasticGradientStart,
        title = "2. Slot 5: Elastic Rubber Buffer",
        subtitle = "Dynamic Auto-Sizing Shock Absorber"
      ) {
        Text(
          text = "Slot 5 acts as an accordion buffer mathematically fixed to span the exact gap between Slot 4 (Midday Reset) and Slot 6 (Evening Reset).\n\n" +
            "• As you adjust Slot 4 or Slot 6, Slot 5 dynamically resizes to fill the difference.\n" +
            "• Strict 15-minute floor: Slot 5 can never shrink below 15 minutes to guarantee psychological breathing room.",
          color = TextSecondary,
          fontSize = 13.sp,
          lineHeight = 18.sp
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Section 3: 2-Session Fluid Twins
      GuideSectionCard(
        icon = Icons.Default.CallSplit,
        iconTint = Color(0xFF8B5CF6),
        title = "3. Fluid Twins: Communicating Vessels",
        subtitle = "Linked 2-Session Duration Pool"
      ) {
        Text(
          text = "Fluid Twins link two distinct sessions (e.g., Morning Deep Work & Afternoon Review) that share a unified duration pool.\n\n" +
            "• Communicating Vessels: Adjusting the duration of one session balances against the total pool.\n" +
            "• Anchor Boundary Protection: Both sessions are strictly bounded by anchor walls and respect a 45-minute safety floor.\n" +
            "• Custom Voice Prompts: Each part features independent voice reminders and ringtone tones.",
          color = TextSecondary,
          fontSize = 13.sp,
          lineHeight = 18.sp
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Section 4: Bulk Terminal Console Syntax
      GuideSectionCard(
        icon = Icons.Default.Code,
        iconTint = Color(0xFF06B6D4),
        title = "4. Bulk Terminal Console Syntax",
        subtitle = "Pipe-Separated Power User Interface"
      ) {
        Text(
          text = "The Bulk Terminal Console lets you create, edit, and back up routines in plain text using pipe (|) formatting:",
          color = TextSecondary,
          fontSize = 13.sp,
          lineHeight = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(10.dp)),
          color = Color(0xFF121620)
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "Task Name | Duration(m) | Start(HH:mm) | DND | Days | Voice | Message",
              color = Color(0xFF06B6D4),
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Deep Coding | 90 | 14:00 | Y | Mon,Wed,Fri | Y | Focus sprint now\n" +
                "Reading | 45 | 19:30 | N | ALL | Y | Evening reading time",
              color = Color(0xFFE2E8F0),
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp,
              lineHeight = 16.sp
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Note: Exporting routine copies only your dynamic tasks and fluid twins, preserving the system's hardcoded anchors cleanly.",
          color = TextTertiary,
          fontSize = 12.sp,
          lineHeight = 16.sp
        )
      }
    }
  }
}

@Composable
private fun GuideSectionCard(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconTint: Color,
  title: String,
  subtitle: String,
  content: @Composable () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(20.dp)),
    color = OneUiSurfaceElevated
  ) {
    Column(
      modifier = Modifier
        .animateContentSize()
        .padding(16.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(iconTint.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(text = title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text(text = subtitle, color = TextTertiary, fontSize = 11.sp)
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      content()
    }
  }
}

@Composable
private fun DynamicAnchorPillRow(
  slot: Int,
  task: RoutineTask?,
  defaultName: String,
  desc: String,
  color: Color
) {
  val title = task?.title ?: defaultName
  val timeInfo = task?.let { "${it.formattedStartTime} → ${it.formattedEndTime} (${it.formattedDuration})" } ?: ""

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .clip(RoundedCornerShape(14.dp)),
    color = OneUiSurfaceHigh
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .background(color),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = if (slot == 1) "🌙" else if (slot == 5) "🛡️" else "$slot",
          color = Color.White,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Slot $slot: $title",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
          )
          if (timeInfo.isNotBlank()) {
            Text(
              text = timeInfo,
              color = Color(0xFFA5B4FC),
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = desc, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
      }
    }
  }
}
