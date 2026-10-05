package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.service.AutomationManager
import com.example.service.RoutineAutomationService

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
      AutomationManager.ACTION_END_TASK -> {
        val stopIntent = Intent(context, RoutineAutomationService::class.java).apply {
          this.action = RoutineAutomationService.ACTION_STOP_AUTOMATION
        }
        context.startService(stopIntent)
      }
    }
  }
}
