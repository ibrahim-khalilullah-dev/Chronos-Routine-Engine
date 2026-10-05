package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoutineTask
import com.example.ui.theme.AnchorGradientStart
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OneUiBackground
import com.example.ui.theme.OneUiBorder
import com.example.ui.theme.OneUiSurfaceElevated
import com.example.ui.theme.OneUiSurfaceHigh
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber

@Composable
fun TrashBinScreen(
  deletedTasks: List<RoutineTask>,
  onRestoreTask: (String) -> Unit,
  onPurgePermanently: (String) -> Unit,
  onEmptyTrash: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val haptic = LocalHapticFeedback.current
  var showEmptyAllConfirmDialog by remember { mutableStateOf(false) }
  var taskToPurgeConfirm by remember { mutableStateOf<RoutineTask?>(null) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(OneUiBackground)
      .statusBarsPadding()
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
      // Header Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onBack()
            },
            modifier = Modifier.size(38.dp).testTag("trash_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = TextPrimary
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Trash Bin",
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "Auto-purges after 30 days",
              fontSize = 12.sp,
              color = TextSecondary
            )
          }
        }

        if (deletedTasks.isNotEmpty()) {
          Button(
            onClick = { showEmptyAllConfirmDialog = true },
            colors = ButtonDefaults.buttonColors(
              containerColor = DangerRed.copy(alpha = 0.2f),
              contentColor = DangerRed
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("empty_trash_all_button")
          ) {
            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Empty All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      if (deletedTasks.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = null,
              tint = TextTertiary,
              modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "Trash Bin is Empty",
              color = TextPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Deleted routine tasks are stored here for 30 days.",
              color = TextSecondary,
              fontSize = 12.sp
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          contentPadding = PaddingValues(bottom = 32.dp)
        ) {
          items(deletedTasks, key = { it.id }) { task ->
            TrashItemCard(
              task = task,
              onRestore = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onRestoreTask(task.id)
              },
              onPurge = {
                taskToPurgeConfirm = task
              }
            )
          }
        }
      }
    }
  }

  // Dialog: Empty all trash confirm
  if (showEmptyAllConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showEmptyAllConfirmDialog = false },
      title = { Text("Empty Entire Trash Bin?", fontWeight = FontWeight.Bold, color = TextPrimary) },
      text = {
        Text(
          "Permanently delete all ${deletedTasks.size} tasks? This action cannot be reversed.",
          color = TextSecondary,
          fontSize = 13.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            showEmptyAllConfirmDialog = false
            onEmptyTrash()
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Empty Trash", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showEmptyAllConfirmDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      },
      containerColor = OneUiSurfaceElevated,
      shape = RoundedCornerShape(24.dp)
    )
  }

  // Dialog: Single task purge confirm
  taskToPurgeConfirm?.let { task ->
    AlertDialog(
      onDismissRequest = { taskToPurgeConfirm = null },
      title = { Text("Permanently Delete?", fontWeight = FontWeight.Bold, color = TextPrimary) },
      text = {
        Text(
          "“${task.title}” will be permanently removed immediately.",
          color = TextSecondary,
          fontSize = 13.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val id = task.id
            taskToPurgeConfirm = null
            onPurgePermanently(id)
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Delete Permanently", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { taskToPurgeConfirm = null }) {
          Text("Cancel", color = TextSecondary)
        }
      },
      containerColor = OneUiSurfaceElevated,
      shape = RoundedCornerShape(24.dp)
    )
  }
}

@Composable
fun TrashItemCard(
  task: RoutineTask,
  onRestore: () -> Unit,
  onPurge: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(20.dp)),
    color = OneUiSurfaceElevated,
    shape = RoundedCornerShape(20.dp)
  ) {
    Column(
      modifier = Modifier
        .animateContentSize()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = task.title,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Duration: ${task.formattedDuration} • Start: ${task.formattedStartTime}",
            color = TextSecondary,
            fontSize = 12.sp
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(WarningAmber.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "${task.daysRemainingInTrash}d left",
            color = WarningAmber,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = onRestore,
          modifier = Modifier.weight(1f).testTag("restore_task_${task.id}"),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, AnchorGradientStart.copy(alpha = 0.6f))
        ) {
          Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = AnchorGradientStart, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Restore", color = AnchorGradientStart, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = onPurge,
          modifier = Modifier.weight(1f).testTag("purge_task_${task.id}"),
          colors = ButtonDefaults.buttonColors(
            containerColor = DangerRed.copy(alpha = 0.2f),
            contentColor = DangerRed
          ),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Delete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
