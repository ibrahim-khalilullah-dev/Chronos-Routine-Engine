package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.RoutineRepository
import com.example.service.AutomationManager
import com.example.service.RoutineAutomationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoutineAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    val appContext = context.applicationContext ?: context
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
        runAsyncWithWakeLock(goAsync()) {
          AutomationManager.rescheduleAllAlarms(appContext)
        }
      }
      AutomationManager.ACTION_APPLY_DND -> {
        val taskId = intent.getStringExtra(AutomationManager.EXTRA_TASK_ID)
        val taskEndMillis = intent.getLongExtra(AutomationManager.EXTRA_TASK_END_MILLIS, -1L)
        if (!taskId.isNullOrBlank()) {
          if (taskEndMillis > 0L && System.currentTimeMillis() >= taskEndMillis - 1000L) {
            runAsyncWithWakeLock(goAsync()) {
              AutomationManager.rescheduleAllAlarms(appContext)
            }
            return
          }
          val pendingResult: PendingResult? = goAsync()
          if (pendingResult != null) {
            CoroutineScope(Dispatchers.Default).launch {
              try {
                handleApplyDnd(context, appContext, taskId)
                AutomationManager.rescheduleAllAlarms(appContext)
              } finally {
                try {
                  pendingResult.finish()
                } catch (_: Exception) {}
              }
            }
          } else {
            handleApplyDnd(context, appContext, taskId)
          }
        }
      }
      AutomationManager.ACTION_END_TASK -> {
        val taskId = intent.getStringExtra(AutomationManager.EXTRA_TASK_ID)
        val hasExplicitEnableDnd = intent.hasExtra(AutomationManager.EXTRA_ENABLE_DND)
        val enableDnd = intent.getBooleanExtra(AutomationManager.EXTRA_ENABLE_DND, false)

        val pendingResult: PendingResult? = goAsync()
        if (pendingResult != null) {
          CoroutineScope(Dispatchers.Default).launch {
            try {
              handleEndTask(context, taskId, hasExplicitEnableDnd, enableDnd)
              AutomationManager.rescheduleAllAlarms(appContext)
            } finally {
              try {
                pendingResult.finish()
              } catch (_: Exception) {}
            }
          }
        } else {
          handleEndTask(context, taskId, hasExplicitEnableDnd, enableDnd)
        }
      }
    }
  }

  private fun handleApplyDnd(context: Context, appContext: Context, taskId: String) {
    val repository = RoutineRepository(appContext)
    val targetTask = (repository.getAnchors() + repository.getDynamicTasks())
      .firstOrNull { it.id == taskId }
    if (targetTask != null && !targetTask.isDeleted && !targetTask.isCurrentlyPaused() && targetTask.enableDnd) {
      AutomationManager.setDndMode(context, true, taskId)
      RoutineAutomationService.onDndApplied(taskId)
    }
  }

  private fun handleEndTask(
    context: Context,
    taskId: String?,
    hasExplicitEnableDnd: Boolean,
    enableDnd: Boolean
  ) {
    val activeDndTaskId = AutomationManager.getActiveDndTaskId(context)
    val canReleaseByOwner = activeDndTaskId == null || (taskId != null && activeDndTaskId == taskId)

    if (canReleaseByOwner && !AutomationManager.hasZeroDelayFollowUpDndTask(context, taskId)) {
      if (activeDndTaskId != null || enableDnd || !hasExplicitEnableDnd) {
        AutomationManager.setDndMode(context, false, taskId)
      }
    }

    RoutineAutomationService.requestStop(context, taskId)
  }

  private fun runAsyncWithWakeLock(
    pendingResult: PendingResult?,
    block: () -> Unit
  ) {
    if (pendingResult == null) return
    CoroutineScope(Dispatchers.Default).launch {
      try {
        block()
      } finally {
        try {
          pendingResult.finish()
        } catch (_: Exception) {}
      }
    }
  }
}
