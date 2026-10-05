package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ScheduleResult
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.TaskType
import com.example.service.ActiveAutomationState
import com.example.service.AutomationManager
import com.example.ui.TimelineSortMode
import com.example.ui.components.AnchorSlotCard
import com.example.ui.components.DynamicTaskCard
import com.example.ui.components.OverflowTaskCard
import com.example.ui.components.isTaskActiveAt
import com.example.ui.theme.AnchorGradientStart
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DynamicGradientEnd
import com.example.ui.theme.DynamicGradientStart
import com.example.ui.theme.ElasticGradientStart
import com.example.ui.theme.OneUiBackground
import com.example.ui.theme.OneUiBorder
import com.example.ui.theme.OneUiSurfaceElevated
import com.example.ui.theme.SleepGradientStart
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber

@Composable
fun TimelineScreen(
  activeDay: RoutineDay,
  onSelectDay: (RoutineDay) -> Unit,
  scheduleResult: ScheduleResult,
  sleepDurationMinutes: Int,
  currentMinute: Int,
  isDndGranted: Boolean,
  activeAutomation: ActiveAutomationState,
  sortMode: TimelineSortMode,
  deletedCount: Int,
  onSelectSortMode: (TimelineSortMode) -> Unit,
  onQuickAdjustAnchor: (slotNumber: Int, deltaMinutes: Int) -> Unit,
  onEditTask: (RoutineTask) -> Unit,
  onMoveToTrash: (taskId: String) -> Unit,
  onOpenTrashBin: () -> Unit,
  onStopAutomation: () -> Unit,
  onRefreshDndPermission: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val listState = rememberLazyListState()

  // State for Confirmation Dialog (Requirement 1: Safe Delete)
  var taskToDeleteConfirm by remember { mutableStateOf<RoutineTask?>(null) }

  // Auto-scroll to currently active task on launch or day shift (Requirement 3)
  LaunchedEffect(activeDay, currentMinute) {
    val activeIndex = scheduleResult.scheduledTasks.indexOfFirst {
      isTaskActiveAt(it, currentMinute)
    }
    if (activeIndex >= 0) {
      try {
        listState.animateScrollToItem((activeIndex + 1).coerceAtLeast(0))
      } catch (_: Exception) {}
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(OneUiBackground)
      .statusBarsPadding()
  ) {
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // One UI Reachability Header with 24h Ribbon & Sorting Controls
      item {
        OneUiHeader(
          activeDay = activeDay,
          onSelectDay = { day ->
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelectDay(day)
          },
          totalScheduledMinutes = scheduleResult.totalScheduledMinutes,
          scheduledCount = scheduleResult.scheduledTasks.size,
          overflowCount = scheduleResult.overflowTasks.size,
          deletedCount = deletedCount,
          scheduledTasks = scheduleResult.scheduledTasks,
          sortMode = sortMode,
          onSelectSortMode = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelectSortMode(it)
          },
          onOpenTrashBin = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onOpenTrashBin()
          }
        )
      }

      // Active Automation Banner (Hardware DND or TTS Running)
      if (activeAutomation.isRunning) {
        item {
          ActiveAutomationBanner(
            automationState = activeAutomation,
            onStop = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onStopAutomation()
            }
          )
        }
      }

      // DND Permission Alert Banner
      if (!isDndGranted) {
        item {
          DndPermissionBanner(
            onOpenSettings = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              AutomationManager.openDndSettings(context)
              onRefreshDndPermission()
            }
          )
        }
      }

      // Section Title: Daily Timeline
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Daily Timeline & Anchors",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "${scheduleResult.totalScheduledMinutes / 60}h ${scheduleResult.totalScheduledMinutes % 60}m active",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }
      }

      // Scheduled items: 7 Anchors and fitted Dynamic Tasks
      items(
        items = scheduleResult.scheduledTasks,
        key = { it.id }
      ) { task ->
        when (task.type) {
          TaskType.ANCHOR_SLEEP,
          TaskType.ANCHOR_STANDARD,
          TaskType.ANCHOR_ELASTIC -> {
            AnchorSlotCard(
              task = task,
              sleepDurationMinutes = sleepDurationMinutes,
              currentMinute = currentMinute,
              onQuickAdjust = { delta ->
                task.slotNumber?.let { slot -> onQuickAdjustAnchor(slot, delta) }
              },
              onEditClick = { onEditTask(task) }
            )
          }
          TaskType.DYNAMIC -> {
            DynamicTaskCard(
              task = task,
              currentMinute = currentMinute,
              onEditClick = { onEditTask(task) },
              onDeleteClick = {
                // Safe Delete: opens Samsung One UI styled confirmation modal
                taskToDeleteConfirm = task
              }
            )
          }
        }
      }

      // Empty State for Dynamic Tasks
      val dynamicCount = scheduleResult.scheduledTasks.count { it.type == TaskType.DYNAMIC }
      if (dynamicCount == 0) {
        item {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(22.dp))
              .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(22.dp)),
            color = OneUiSurfaceElevated
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF67E8F9),
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Open Schedule on ${activeDay.fullName}",
                  color = TextPrimary,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Your 7 immutable anchors are running. Tap 'Add Task' in the bottom navigation bar to schedule dynamic blocks via Form or Terminal.",
                  color = TextSecondary,
                  fontSize = 11.sp,
                  lineHeight = 16.sp
                )
              }
            }
          }
        }
      }

      // Overflow Pool Section
      if (scheduleResult.overflowTasks.isNotEmpty()) {
        item {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Overflow / Pending Tasks Pool (${scheduleResult.overflowTasks.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = WarningAmber
              )
            }
          }
        }

        items(
          items = scheduleResult.overflowTasks,
          key = { "overflow_${it.id}" }
        ) { task ->
          OverflowTaskCard(
            task = task,
            onEditClick = { onEditTask(task) },
            onDeleteClick = {
              taskToDeleteConfirm = task
            }
          )
        }
      }

      // Bottom padding for navigation bar
      item {
        Spacer(modifier = Modifier.height(32.dp))
      }
    }
  }

  // Samsung One UI Styled Safe Delete Confirmation Dialog
  taskToDeleteConfirm?.let { task ->
    AlertDialog(
      onDismissRequest = { taskToDeleteConfirm = null },
      title = {
        Text(
          text = "Delete Task?",
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          fontSize = 18.sp
        )
      },
      text = {
        Text(
          text = "This task will be moved to the Trash Bin. You can restore it within 30 days.",
          color = TextSecondary,
          fontSize = 13.sp,
          lineHeight = 18.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val id = task.id
            taskToDeleteConfirm = null
            onMoveToTrash(id)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = DangerRed,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("confirm_move_to_trash_button")
        ) {
          Text("Move to Trash", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { taskToDeleteConfirm = null },
          modifier = Modifier.testTag("cancel_delete_task_button")
        ) {
          Text("Cancel", color = TextSecondary)
        }
      },
      containerColor = OneUiSurfaceElevated,
      shape = RoundedCornerShape(24.dp)
    )
  }
}

@Composable
fun OneUiHeader(
  activeDay: RoutineDay,
  onSelectDay: (RoutineDay) -> Unit,
  totalScheduledMinutes: Int,
  scheduledCount: Int,
  overflowCount: Int,
  deletedCount: Int,
  scheduledTasks: List<RoutineTask>,
  sortMode: TimelineSortMode,
  onSelectSortMode: (TimelineSortMode) -> Unit,
  onOpenTrashBin: () -> Unit
) {
  val haptic = LocalHapticFeedback.current
  var sortMenuExpanded by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 8.dp, bottom = 8.dp)
      .animateContentSize()
  ) {
    // Top Row: App Title + Trash Bin Shortcut
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Chronos",
          fontSize = 32.sp,
          fontWeight = FontWeight.ExtraBold,
          color = TextPrimary
        )
        Text(
          text = "Smart Dynamic Routine & Device Automation",
          fontSize = 13.sp,
          color = TextSecondary,
          fontWeight = FontWeight.Medium
        )
      }

      // Trash Bin Button with badge
      IconButton(
        onClick = onOpenTrashBin,
        modifier = Modifier
          .clip(RoundedCornerShape(14.dp))
          .background(OneUiSurfaceElevated)
          .testTag("open_trash_bin_button")
      ) {
        Box(contentAlignment = Alignment.TopEnd) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Trash Bin",
            tint = if (deletedCount > 0) WarningAmber else TextSecondary,
            modifier = Modifier.size(22.dp)
          )
          if (deletedCount > 0) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(WarningAmber)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Day Pills Selector (Sat to Fri)
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(RoutineDay.entries) { day ->
        val isSelected = day == activeDay
        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
              BorderStroke(1.dp, if (isSelected) AnchorGradientStart else OneUiBorder),
              RoundedCornerShape(16.dp)
            )
            .clickable {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onSelectDay(day)
            }
            .testTag("day_pill_${day.name.lowercase()}"),
          color = if (isSelected) AnchorGradientStart else OneUiSurfaceElevated,
          shape = RoundedCornerShape(16.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = day.shortName,
              color = if (isSelected) Color.White else TextSecondary,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              fontSize = 13.sp
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 24h Timeline Visual Ribbon
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp)),
      color = OneUiSurfaceElevated
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "24-Hour Cycle Map", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

          // Sorting Dropdown Chip (Requirement 2)
          Box {
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .clickable { sortMenuExpanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("timeline_sort_chip"),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(imageVector = Icons.Default.Sort, contentDescription = null, tint = AnchorGradientStart, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = sortMode.label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            }

            DropdownMenu(
              expanded = sortMenuExpanded,
              onDismissRequest = { sortMenuExpanded = false },
              modifier = Modifier.background(OneUiSurfaceElevated)
            ) {
              TimelineSortMode.entries.forEach { mode ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = mode.label,
                      color = if (mode == sortMode) AnchorGradientStart else TextPrimary,
                      fontWeight = if (mode == sortMode) FontWeight.Bold else FontWeight.Normal,
                      fontSize = 12.sp
                    )
                  },
                  onClick = {
                    sortMenuExpanded = false
                    onSelectSortMode(mode)
                  }
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Visual Ribbon Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color.White.copy(alpha = 0.08f))
        ) {
          for (task in scheduledTasks) {
            val weight = (task.durationMinutes.toFloat() / 1440f).coerceAtLeast(0.01f)
            val segmentColor = when (task.type) {
              TaskType.ANCHOR_SLEEP -> SleepGradientStart
              TaskType.ANCHOR_ELASTIC -> ElasticGradientStart
              TaskType.ANCHOR_STANDARD -> AnchorGradientStart
              TaskType.DYNAMIC -> DynamicGradientStart
            }
            Box(
              modifier = Modifier
                .weight(weight)
                .height(10.dp)
                .background(segmentColor)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Quick Metrics Strip
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp)),
      color = OneUiSurfaceElevated
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround
      ) {
        MetricItem(label = "7 Anchors", value = "Locked 🔒", color = Color(0xFFA5B4FC))
        MetricItem(label = "Scheduled", value = "$scheduledCount tasks", color = DynamicGradientEnd)
        MetricItem(
          label = "Overflow",
          value = if (overflowCount == 0) "Clear ✓" else "$overflowCount pending",
          color = if (overflowCount == 0) Color(0xFF10B981) else WarningAmber
        )
      }
    }
  }
}

@Composable
private fun MetricItem(label: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = label, color = TextTertiary, fontSize = 11.sp)
    Spacer(modifier = Modifier.height(2.dp))
    Text(text = value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
  }
}

@Composable
fun ActiveAutomationBanner(
  automationState: ActiveAutomationState,
  onStop: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .border(BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)), RoundedCornerShape(20.dp))
      .testTag("active_automation_banner"),
    color = Color(0xFF1E1B4B).copy(alpha = 0.85f)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.RecordVoiceOver,
          contentDescription = null,
          tint = Color(0xFFA5B4FC),
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = automationState.taskTitle ?: "Automation Active",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
            if (automationState.isDndActive) {
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "• DND 🌙",
                color = WarningAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
          Text(
            text = automationState.notificationBody ?: automationState.voiceMessage ?: "Running routine task",
            color = Color(0xFFCBD5E1),
            fontSize = 11.sp,
            maxLines = 1
          )
        }
      }

      Button(
        onClick = onStop,
        colors = ButtonDefaults.buttonColors(
          containerColor = DangerRed,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.testTag("stop_automation_button")
      ) {
        Text("Stop & Revert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun DndPermissionBanner(
  onOpenSettings: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .border(BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)), RoundedCornerShape(20.dp)),
    color = WarningAmber.copy(alpha = 0.12f)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Security,
          contentDescription = null,
          tint = WarningAmber,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "DND Policy Access Needed",
            color = WarningAmber,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
          Text(
            text = "Grant permission for automatic Do Not Disturb mode switching",
            color = TextSecondary,
            fontSize = 11.sp
          )
        }
      }

      Button(
        onClick = onOpenSettings,
        colors = ButtonDefaults.buttonColors(
          containerColor = WarningAmber,
          contentColor = Color.Black
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = Modifier.testTag("grant_dnd_permission_button")
      ) {
        Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
