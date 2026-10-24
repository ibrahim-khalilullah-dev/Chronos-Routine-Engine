package com.example.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import com.example.data.RoutineRepository
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.receiver.RoutineAlarmReceiver
import java.util.Calendar

object AutomationManager {

  const val ACTION_START_TASK = "com.example.ACTION_START_TASK"
  const val ACTION_END_TASK = "com.example.ACTION_END_TASK"
  const val EXTRA_TASK_ID = "extra_task_id"
  const val EXTRA_TASK_TITLE = "extra_task_title"
  const val EXTRA_VOICE_MESSAGE = "extra_voice_message"
  const val EXTRA_ENABLE_DND = "extra_enable_dnd"
  const val EXTRA_ENABLE_HAPTIC = "extra_enable_haptic"
  const val EXTRA_IS_VOICE_ENABLED = "extra_is_voice_enabled"
  const val EXTRA_IS_SOUND_ALERT_ENABLED = "extra_is_sound_alert_enabled"
  const val EXTRA_CUSTOM_RINGTONE_URI = "extra_custom_ringtone_uri"
  const val EXTRA_NOTIFICATION_TITLE = "extra_notification_title"
  const val EXTRA_NOTIFICATION_BODY = "extra_notification_body"
  const val EXTRA_VOICE_VOLUME = "extra_voice_volume"

  fun createTtsVolumeParams(volume: Float): Bundle {
    val clamped = volume.coerceIn(0.0f, 1.0f)
    return Bundle().apply {
      putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, clamped)
    }
  }

  fun isDndPermissionGranted(context: Context): Boolean {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    return nm?.isNotificationPolicyAccessGranted == true
  }

  fun openDndSettings(context: Context) {
    try {
      val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
    } catch (_: Exception) {}
  }

  fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
    return pm.isIgnoringBatteryOptimizations(context.packageName)
  }

  fun requestIgnoreBatteryOptimizations(context: Context) {
    try {
      val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
    } catch (_: Exception) {
      try {
        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(fallbackIntent)
      } catch (_: Exception) {}
    }
  }

  fun canScheduleExactAlarms(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
      am?.canScheduleExactAlarms() == true
    } else {
      true
    }
  }

  fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      try {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
      } catch (_: Exception) {}
    }
  }

  fun setDndMode(context: Context, enabled: Boolean): Boolean {
    if (!isDndPermissionGranted(context)) return false
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
    return try {
      if (enabled) {
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
      } else {
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
      }
      true
    } catch (_: SecurityException) {
      false
    } catch (_: Exception) {
      false
    }
  }

  fun triggerHaptic(context: Context) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        val vibrator = vm?.defaultVibrator
        val effect = VibrationEffect.createWaveform(
          longArrayOf(0, 120, 80, 200),
          intArrayOf(0, 255, 0, 200),
          -1
        )
        vibrator?.vibrate(effect)
      } else {
        @Suppress("DEPRECATION")
        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          v?.vibrate(
            VibrationEffect.createWaveform(
              longArrayOf(0, 120, 80, 200),
              -1
            )
          )
        } else {
          @Suppress("DEPRECATION")
          v?.vibrate(longArrayOf(0, 120, 80, 200), -1)
        }
      }
    } catch (_: Exception) {}
  }

  fun scheduleTaskAlarms(context: Context, task: RoutineTask) {
    if (task.isDeleted) return
    val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    var effectiveStartMin = task.startTimeMinute
    var effectiveDurationMin = task.durationMinutes
    var effectiveTitle = task.title
    var effectiveVoiceMessage = task.voiceMessage

    val startCal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, effectiveStartMin / 60)
      set(Calendar.MINUTE, effectiveStartMin % 60)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      if (timeInMillis <= System.currentTimeMillis()) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }

    val targetRoutineDay = when (startCal.get(Calendar.DAY_OF_WEEK)) {
      Calendar.SATURDAY -> RoutineDay.SATURDAY
      Calendar.SUNDAY -> RoutineDay.SUNDAY
      Calendar.MONDAY -> RoutineDay.MONDAY
      Calendar.TUESDAY -> RoutineDay.TUESDAY
      Calendar.WEDNESDAY -> RoutineDay.WEDNESDAY
      Calendar.THURSDAY -> RoutineDay.THURSDAY
      Calendar.FRIDAY -> RoutineDay.FRIDAY
      else -> RoutineDay.SATURDAY
    }

    // Slot 5 deactivation check: if target day is not in selectedDays, do not schedule
    if (task.slotNumber == 5 && !task.selectedDays.contains(targetRoutineDay)) {
      return
    }

    // Slot 3 Friday dual-mode check: if target day is Friday, use Friday overrides
    if (task.slotNumber == 3 && targetRoutineDay == RoutineDay.FRIDAY) {
      effectiveTitle = task.fridayTitle?.ifBlank { null } ?: task.title
      effectiveStartMin = task.fridayStartTimeMinute ?: task.startTimeMinute
      effectiveDurationMin = task.fridayDurationMinutes ?: task.durationMinutes
      effectiveVoiceMessage = task.fridayVoiceMessage?.ifBlank { null } ?: task.voiceMessage

      startCal.apply {
        set(Calendar.HOUR_OF_DAY, effectiveStartMin / 60)
        set(Calendar.MINUTE, effectiveStartMin % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
    }

    val startIntent = Intent(context, RoutineAlarmReceiver::class.java).apply {
      action = ACTION_START_TASK
      putExtra(EXTRA_TASK_ID, task.id)
      putExtra(EXTRA_TASK_TITLE, effectiveTitle)
      putExtra(EXTRA_VOICE_MESSAGE, effectiveVoiceMessage)
      putExtra(EXTRA_ENABLE_DND, task.enableDnd)
      putExtra(EXTRA_ENABLE_HAPTIC, task.enableHaptic)
      putExtra(EXTRA_IS_VOICE_ENABLED, task.isVoiceEnabled)
      putExtra(EXTRA_IS_SOUND_ALERT_ENABLED, task.isSoundAlertEnabled)
      putExtra(EXTRA_CUSTOM_RINGTONE_URI, task.customRingtoneUri)
      putExtra(EXTRA_NOTIFICATION_TITLE, if (task.customNotificationTitle.isNotBlank()) task.customNotificationTitle else effectiveTitle)
      putExtra(EXTRA_NOTIFICATION_BODY, if (task.customNotificationBody.isNotBlank()) task.customNotificationBody else effectiveVoiceMessage.ifBlank { "$effectiveTitle starting now." })
      putExtra(EXTRA_VOICE_VOLUME, task.voiceVolume)
    }

    val pendingStart = PendingIntent.getBroadcast(
      context,
      task.id.hashCode(),
      startIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (am.canScheduleExactAlarms()) {
          am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startCal.timeInMillis, pendingStart)
        } else {
          am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startCal.timeInMillis, pendingStart)
        }
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startCal.timeInMillis, pendingStart)
      } else {
        am.setExact(AlarmManager.RTC_WAKEUP, startCal.timeInMillis, pendingStart)
      }
    } catch (_: SecurityException) {
      try {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, startCal.timeInMillis, pendingStart)
      } catch (_: Exception) {}
    } catch (_: Exception) {}

    // Also schedule end alarm to automatically stop DND & automation when task completes
    val endMinute = (effectiveStartMin + effectiveDurationMin) % 1440
    val endCal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, endMinute / 60)
      set(Calendar.MINUTE, endMinute % 60)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      if (timeInMillis <= System.currentTimeMillis()) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }

    val endIntent = Intent(context, RoutineAlarmReceiver::class.java).apply {
      action = ACTION_END_TASK
      putExtra(EXTRA_TASK_ID, task.id)
    }
    val pendingEnd = PendingIntent.getBroadcast(
      context,
      (task.id + "_end").hashCode(),
      endIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    try {
      am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endCal.timeInMillis, pendingEnd)
    } catch (_: Exception) {}
  }

  fun cancelTaskAlarms(context: Context, taskId: String) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val startIntent = Intent(context, RoutineAlarmReceiver::class.java).apply {
      action = ACTION_START_TASK
    }
    val pendingStart = PendingIntent.getBroadcast(
      context,
      taskId.hashCode(),
      startIntent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (pendingStart != null) {
      am.cancel(pendingStart)
    }

    val endIntent = Intent(context, RoutineAlarmReceiver::class.java).apply {
      action = ACTION_END_TASK
    }
    val pendingEnd = PendingIntent.getBroadcast(
      context,
      (taskId + "_end").hashCode(),
      endIntent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (pendingEnd != null) {
      am.cancel(pendingEnd)
    }
  }

  fun rescheduleAllAlarms(context: Context) {
    try {
      val repository = RoutineRepository(context)
      val anchors = repository.getAnchors().filter { !it.isDeleted }
      val dynamicTasks = repository.getDynamicTasks().filter { !it.isDeleted }
      for (task in anchors) {
        scheduleTaskAlarms(context, task)
      }
      for (task in dynamicTasks) {
        scheduleTaskAlarms(context, task)
      }
    } catch (_: Exception) {}
  }

  fun startAutomationService(context: Context, task: RoutineTask) {
    if (task.isDeleted) return
    val serviceIntent = Intent(context, RoutineAutomationService::class.java).apply {
      action = ACTION_START_TASK
      putExtra(EXTRA_TASK_ID, task.id)
      putExtra(EXTRA_TASK_TITLE, task.title)
      putExtra(EXTRA_VOICE_MESSAGE, task.voiceMessage.ifEmpty { "${task.title} starting now." })
      putExtra(EXTRA_ENABLE_DND, task.enableDnd)
      putExtra(EXTRA_ENABLE_HAPTIC, task.enableHaptic)
      putExtra(EXTRA_IS_VOICE_ENABLED, task.isVoiceEnabled)
      putExtra(EXTRA_IS_SOUND_ALERT_ENABLED, task.isSoundAlertEnabled)
      putExtra(EXTRA_CUSTOM_RINGTONE_URI, task.customRingtoneUri)
      putExtra(EXTRA_NOTIFICATION_TITLE, task.notificationDisplayTitle)
      putExtra(EXTRA_NOTIFICATION_BODY, task.notificationDisplayBody)
      putExtra(EXTRA_VOICE_VOLUME, task.voiceVolume)
    }
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(serviceIntent)
      } else {
        context.startService(serviceIntent)
      }
    } catch (e: Exception) {
      try {
        context.startService(serviceIntent)
      } catch (_: Exception) {}
    }
  }
}
