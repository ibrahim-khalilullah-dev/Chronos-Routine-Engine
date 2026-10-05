package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoutineTask
import com.example.ui.theme.AnchorGradientStart
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DynamicGradientStart
import com.example.ui.theme.OneUiBackground
import com.example.ui.theme.OneUiBorder
import com.example.ui.theme.OneUiSurfaceElevated
import com.example.ui.theme.OneUiSurfaceHigh
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  sleepDurationMinutes: Int,
  bufferMinutes: Int,
  slot1Task: RoutineTask?,
  slot2Task: RoutineTask?,
  deletedCount: Int = 0,
  onUpdateSleepDuration: (Float) -> Unit,
  onUpdateBufferMinutes: (Int) -> Unit,
  onOpenTrashBin: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(OneUiBackground)
      .statusBarsPadding()
      .animateContentSize()
      .verticalScroll(rememberScrollState())
      .padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    Column {
      Text(
        text = "Sleep & Routine Engine",
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
      Text(
        text = "Reverse sleep mathematics, rest buffers, and slot protection rules",
        fontSize = 12.sp,
        color = TextSecondary
      )
    }

    // Slot 1: Reverse Sleep Settings Card
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .border(BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f)), RoundedCornerShape(22.dp)),
      color = OneUiSurfaceElevated
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Bedtime,
              contentDescription = null,
              tint = Color(0xFFA5B4FC),
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Reverse Sleep Duration",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "Directly links Slot 1 bedtime to Slot 2 wake-up",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }

          Text(
            text = "${sleepDurationMinutes / 60}h ${sleepDurationMinutes % 60}m",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFA5B4FC)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset Chips
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          val presets = listOf(6.0f, 6.5f, 7.0f, 7.5f, 8.0f, 8.5f)
          presets.forEach { hours ->
            val isSelected = (hours * 60).toInt() == sleepDurationMinutes
            FilterChip(
              selected = isSelected,
              onClick = { onUpdateSleepDuration(hours) },
              label = { Text("${hours}h") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AnchorGradientStart,
                selectedLabelColor = Color.White,
                containerColor = OneUiSurfaceHigh,
                labelColor = TextPrimary
              ),
              shape = RoundedCornerShape(12.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Slider (4h to 10h)
        Slider(
          value = sleepDurationMinutes.toFloat() / 60f,
          onValueChange = { onUpdateSleepDuration(it) },
          valueRange = 4.0f..10.0f,
          steps = 11, // 30 min intervals
          colors = SliderDefaults.colors(
            thumbColor = AnchorGradientStart,
            activeTrackColor = Color(0xFFA5B4FC),
            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
          ),
          modifier = Modifier.testTag("sleep_duration_slider")
        )

        // Live calculation preview
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(12.dp)
        ) {
          Column {
            Text(
              text = "Mathematical Linkage Preview:",
              color = Color(0xFFA5B4FC),
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Slot 2 (Wake Up): ${slot2Task?.formattedStartTime ?: "05:00 AM"}\n" +
                "Minus Sleep (${sleepDurationMinutes / 60}h ${sleepDurationMinutes % 60}m) =\n" +
                "Slot 1 Bedtime: ${slot1Task?.formattedStartTime ?: "10:00 PM"}",
              color = TextPrimary,
              fontSize = 12.sp,
              lineHeight = 18.sp
            )
          }
        }
      }
    }

    // Inter-Task Buffer Time
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(22.dp)),
      color = OneUiSurfaceElevated
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Timelapse,
              contentDescription = null,
              tint = Color(0xFF10B981),
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Inter-Task Rest Buffer",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "Gap between consecutive dynamic tasks",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }

          Text(
            text = "${bufferMinutes}m",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF10B981)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(0, 5, 10, 15).forEach { mins ->
            val isSelected = bufferMinutes == mins
            FilterChip(
              selected = isSelected,
              onClick = { onUpdateBufferMinutes(mins) },
              label = { Text("${mins} min") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFF10B981),
                selectedLabelColor = Color.Black,
                containerColor = OneUiSurfaceHigh,
                labelColor = TextPrimary
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    // Trash Bin Navigation Card
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(22.dp))
        .clickable { onOpenTrashBin() }
        .testTag("settings_trash_bin_card"),
      color = OneUiSurfaceElevated
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(DangerRed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = DangerRed, modifier = Modifier.size(22.dp))
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text(
              text = "Trash Bin & Recovery",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = if (deletedCount > 0) "$deletedCount deleted task(s) • Auto-purged after 30d" else "No deleted tasks • Auto-purges after 30d",
              fontSize = 11.sp,
              color = if (deletedCount > 0) WarningAmber else TextSecondary
            )
          }
        }

        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
      }
    }

    // Architecture Specifications Explanatory Card
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(22.dp)),
      color = OneUiSurfaceElevated
    ) {
      Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "Engine Architecture Blueprint", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Text(
          text = "• 7 Immutable Anchors: Core daily pillars cannot be deleted or overridden.\n" +
            "• Slot 5 Elastic Buffer: Dynamically spans 100% of the duration between Anchor 4 end and Anchor 6 start.\n" +
            "• Auto-Splitting (> 60m): Splits heavy tasks across open slots with a strict 15m minimum chunk safety limit.\n" +
            "• Overflow Pool: Automatically isolates tasks that exceed available day capacity without disrupting fixed anchors.",
          color = TextSecondary,
          fontSize = 12.sp,
          lineHeight = 18.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(30.dp))
  }
}
