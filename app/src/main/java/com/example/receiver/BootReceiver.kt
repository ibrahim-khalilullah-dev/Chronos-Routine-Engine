package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.service.AutomationManager

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    if (action == Intent.ACTION_BOOT_COMPLETED ||
      action == Intent.ACTION_MY_PACKAGE_REPLACED ||
      action == "android.intent.action.QUICKBOOT_POWERON" ||
      action == Intent.ACTION_TIME_CHANGED ||
      action == Intent.ACTION_TIMEZONE_CHANGED
    ) {
      AutomationManager.rescheduleAllAlarms(context)
    }
  }
}
