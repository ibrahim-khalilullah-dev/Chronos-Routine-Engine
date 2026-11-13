package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.RoutineRepository
import com.example.service.AutomationManager
import com.example.service.RoutineAutomationService
import kotlinx.coroutines.launch

class RoutineAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    when (action) {
      AutomationManager.ACTION_START_TASK -> {
        val serviceIntent = Intent(context, RoutineAutomationService::class.java).apply {
          putExtras(intent)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(serviceIntent)
        } else {
          context.startService(serviceIntent)
        }
      }
      AutomationManager.ACTION_APPLY_DND -> {
        val taskId = intent.getStringExtra(AutomationManager.EXTRA_TASK_ID)
        if (!taskId.isNullOrBlank()) {
          val repository = RoutineRepository(context)
          val targetTask = (repository.getAnchors() + repository.getDynamicTasks())
            .firstOrNull { it.id == taskId }
          if (targetTask != null && !targetTask.isDeleted && !targetTask.isCurrentlyPaused() && targetTask.enableDnd) {
            AutomationManager.setDndMode(context, true, taskId)
            RoutineAutomationService.onDndApplied(taskId)
          }
        }
      }
      AutomationManager.ACTION_END_TASK -> {
        val taskId = intent.getStringExtra(AutomationManager.EXTRA_TASK_ID)
        val activeDndTaskId = AutomationManager.getActiveDndTaskId(context)
        val canReleaseByOwner = activeDndTaskId == null || (taskId != null && activeDndTaskId == taskId)
        val hasExplicitEnableDnd = intent.hasExtra(AutomationManager.EXTRA_ENABLE_DND)
        val enableDnd = intent.getBooleanExtra(AutomationManager.EXTRA_ENABLE_DND, false)

        if (canReleaseByOwner && !AutomationManager.hasZeroDelayFollowUpDndTask(context, taskId)) {
          if (activeDndTaskId != null || enableDnd || !hasExplicitEnableDnd) {
            AutomationManager.setDndMode(context, false, taskId)
          }
        }

        RoutineAutomationService.requestStop(context, taskId)

        if (!taskId.isNullOrBlank()) {
          kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            AutomationManager.rescheduleCompletedTaskNextCycle(context, taskId)
          }
        }
      }
    }
  }
}
