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
import com.example.engine.SchedulerEngine
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.TaskType
import com.example.receiver.RoutineAlarmReceiver
import java.util.Calendar

object AutomationManager {

  const val ACTION_START_TASK = "com.example.ACTION_START_TASK"
  const val ACTION_END_TASK = "com.example.ACTION_END_TASK"
  const val ACTION_APPLY_DND = "com.example.ACTION_APPLY_DND"
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
  const val EXTRA_CATEGORY_EMOJI = "extra_category_emoji"
  const val EXTRA_CATEGORY_NAME = "extra_category_name"
  const val EXTRA_FORMATTED_DURATION = "extra_formatted_duration"
  const val EXTRA_FORMATTED_END_TIME = "extra_formatted_end_time"
  const val EXTRA_DND_DELAY_MINUTES = "extra_dnd_delay_minutes"

  fun resolveCategoryInfo(task: RoutineTask, activeDay: RoutineDay = RoutineDay.today()): Pair<String, String> {
    return when {
      task.type == TaskType.ANCHOR_SLEEP -> "🌙" to "Night Sleep"
      task.type == TaskType.ANCHOR_ELASTIC -> "🛡️" to "Rest Buffer"
      task.type == TaskType.ANCHOR_STANDARD -> when (task.slotNumber) {
        2 -> "🌅" to "Morning Routine"
        3 -> if (activeDay == RoutineDay.FRIDAY) "🕌" to "Friday Jumu'ah" else "☀️" to "Midday Focus"
        4 -> "☕" to "Afternoon Reset"
        6 -> "🌆" to "Evening Wind-Down"
        7 -> "🌌" to "Night Reflection"
        else -> "⚓" to "Fixed Anchor"
      }
      else -> when (task.category?.lowercase()?.trim()) {
        "work" -> "💼" to "Productive Work"
        "studying" -> "👓" to "Studying"
        "reading" -> "📖" to "Reading"
        "training" -> "🏃" to "Training"
        "break" -> "☕" to "Break"
        "eating" -> "🍴" to "Eating"
        "sleep" -> "☁️" to "Sleep"
        "commuting" -> "🚗" to "Commuting"
        "family" -> "🏠" to "Home & Family"
        "internet" -> "🖥️" to "Internet"
        "goals" -> "🎯" to "Personal Goals"
        "relationship" -> "❤️" to "Relationship"
        "tv" -> "📺" to "TV"
        else -> "⏱️" to "Dynamic Focus"
      }
    }
  }

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

  private const val PREFS_DND = "chronos_dnd_prefs"
  private const val KEY_PREVIOUS_DND_FILTER = "key_previous_dnd_filter"

  fun setDndMode(context: Context, enabled: Boolean): Boolean {
    if (!isDndPermissionGranted(context)) return false
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
    val prefs = context.getSharedPreferences(PREFS_DND, Context.MODE_PRIVATE)
    return try {
      if (enabled) {
        val currentFilter = nm.currentInterruptionFilter
        if (currentFilter != NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
          prefs.edit().putInt(KEY_PREVIOUS_DND_FILTER, currentFilter).apply()
        }
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
      } else {
        val savedFilter = prefs.getInt(KEY_PREVIOUS_DND_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        nm.setInterruptionFilter(savedFilter)
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

  fun calculateNextOccurrenceMillis(
    task: RoutineTask,
    currentMillis: Long = System.currentTimeMillis()
  ): Long {
    if (task.selectedDays.isEmpty()) return -1L

    val nowCal = Calendar.getInstance().apply { timeInMillis = currentMillis }

    for (dayOffset in 0..7) {
      val testCal = (nowCal.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, dayOffset)
      }

      val routineDay = when (testCal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SATURDAY -> RoutineDay.SATURDAY
        Calendar.SUNDAY -> RoutineDay.SUNDAY
        Calendar.MONDAY -> RoutineDay.MONDAY
        Calendar.TUESDAY -> RoutineDay.TUESDAY
        Calendar.WEDNESDAY -> RoutineDay.WEDNESDAY
        Calendar.THURSDAY -> RoutineDay.THURSDAY
        Calendar.FRIDAY -> RoutineDay.FRIDAY
        else -> RoutineDay.SATURDAY
      }

      if (task.selectedDays.contains(routineDay)) {
        val dayStartMinute = if (task.slotNumber == 3 && routineDay == RoutineDay.FRIDAY) {
          task.fridayStartTimeMinute ?: task.startTimeMinute
        } else {
          task.startTimeMinute
        }

        testCal.apply {
          set(Calendar.HOUR_OF_DAY, dayStartMinute / 60)
          set(Calendar.MINUTE, dayStartMinute % 60)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }

        val taskStartMillis = testCal.timeInMillis
        val taskDuration = if (task.slotNumber == 3 && routineDay == RoutineDay.FRIDAY) {
          task.fridayDurationMinutes ?: task.durationMinutes
        } else {
          task.durationMinutes
        }
        val taskEndMillis = taskStartMillis + (taskDuration * 60L * 1000L)

        // If today and ongoing (started but not ended), preserve today's alarms
        if (dayOffset == 0 && taskStartMillis <= currentMillis && currentMillis < taskEndMillis) {
          return taskStartMillis
        }

        if (dayOffset > 0 || taskStartMillis > currentMillis + 1500L) {
          return taskStartMillis
        }
      }
    }
    return -1L
  }

  fun calculateNextOccurrenceMillis(
    startMinuteOfDay: Int,
    selectedDays: Set<RoutineDay>,
    currentMillis: Long = System.currentTimeMillis()
  ): Long {
    val dummyTask = RoutineTask(
      id = "temp_calc",
      title = "Temp",
      type = TaskType.DYNAMIC,
      startTimeMinute = startMinuteOfDay,
      durationMinutes = 0,
      selectedDays = selectedDays
    )
    return calculateNextOccurrenceMillis(dummyTask, currentMillis)
  }

  fun scheduleTaskAlarms(context: Context, task: RoutineTask) {
    if (task.isDeleted || task.isCurrentlyPaused() || task.isOverflow) {
      cancelTaskAlarms(context, task.id)
      return
    }
    val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    // Cancel any existing alarms for this task first to avoid stale duplicates
    cancelTaskAlarms(context, task.id)

    // Slot 5 deactivation check: if selectedDays is empty or task is paused, do not schedule
    if (task.slotNumber == 5 && task.selectedDays.isEmpty()) {
      return
    }

    var effectiveDurationMin = task.durationMinutes
    var effectiveTitle = task.title
    var effectiveVoiceMessage = task.voiceMessage
    var effectiveDndDelay = task.dndDelayMinutes

    // Find next valid occurrence millis
    val startMillis = calculateNextOccurrenceMillis(task)
    if (startMillis <= 0L) return

    val targetCal = Calendar.getInstance().apply { timeInMillis = startMillis }
    val targetRoutineDay = when (targetCal.get(Calendar.DAY_OF_WEEK)) {
      Calendar.SATURDAY -> RoutineDay.SATURDAY
      Calendar.SUNDAY -> RoutineDay.SUNDAY
      Calendar.MONDAY -> RoutineDay.MONDAY
      Calendar.TUESDAY -> RoutineDay.TUESDAY
      Calendar.WEDNESDAY -> RoutineDay.WEDNESDAY
      Calendar.THURSDAY -> RoutineDay.THURSDAY
      Calendar.FRIDAY -> RoutineDay.FRIDAY
      else -> RoutineDay.SATURDAY
    }

    // Slot 3 Friday dual-mode check: if target day is Friday, use Friday overrides
    if (task.slotNumber == 3 && targetRoutineDay == RoutineDay.FRIDAY) {
      effectiveTitle = task.fridayTitle?.ifBlank { null } ?: task.title
      effectiveDurationMin = task.fridayDurationMinutes ?: task.durationMinutes
      effectiveVoiceMessage = task.fridayVoiceMessage?.ifBlank { null } ?: task.voiceMessage
      effectiveDndDelay = 5
    }

    val finalStartMillis = startMillis
    val (catEmoji, catName) = resolveCategoryInfo(task, targetRoutineDay)

    val endHourMin = ((targetCal.get(Calendar.HOUR_OF_DAY) * 60 + targetCal.get(Calendar.MINUTE) + effectiveDurationMin) % 1440 + 1440) % 1440
    val formattedEndTime = RoutineTask.formatTime(endHourMin)

    val now = System.currentTimeMillis()

    // 1. Task Start Alarm (only if start time is in the future)
    if (finalStartMillis > now + 1500L) {
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
        putExtra(EXTRA_CATEGORY_EMOJI, catEmoji)
        putExtra(EXTRA_CATEGORY_NAME, catName)
        putExtra(EXTRA_FORMATTED_DURATION, task.formattedDuration)
        putExtra(EXTRA_FORMATTED_END_TIME, formattedEndTime)
        putExtra(EXTRA_DND_DELAY_MINUTES, effectiveDndDelay)
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
            am.setAlarmClock(AlarmManager.AlarmClockInfo(finalStartMillis, pendingStart), pendingStart)
          } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalStartMillis, pendingStart)
          }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          am.setAlarmClock(AlarmManager.AlarmClockInfo(finalStartMillis, pendingStart), pendingStart)
        } else {
          am.setExact(AlarmManager.RTC_WAKEUP, finalStartMillis, pendingStart)
        }
      } catch (_: SecurityException) {
        try {
          am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalStartMillis, pendingStart)
        } catch (_: Exception) {}
      } catch (_: Exception) {}
    }

    // Deep Sleep Immune Delayed DND via AlarmManager
    if (task.enableDnd && effectiveDndDelay > 0) {
      val dndTriggerMillis = finalStartMillis + (effectiveDndDelay * 60L * 1000L)
      if (dndTriggerMillis > now) {
        val dndIntent = Intent(context, RoutineAlarmReceiver::class.java).apply {
          action = ACTION_APPLY_DND
          putExtra(EXTRA_TASK_ID, task.id)
        }
        val pendingDnd = PendingIntent.getBroadcast(
          context,
          (task.id + "_dnd").hashCode(),
          dndIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dndTriggerMillis, pendingDnd)
          } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, dndTriggerMillis, pendingDnd)
          }
        } catch (_: Exception) {}
      }
    }

    // Synchronize end alarm: exactly finalStartMillis + (effectiveDurationMin * 60 * 1000L)
    val endMillis = finalStartMillis + (effectiveDurationMin * 60L * 1000L)
    if (endMillis > now) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
          am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMillis, pendingEnd)
        } else {
          am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endMillis, pendingEnd)
        }
      } catch (_: Exception) {}
    }
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

    val dndIntent = Intent(context, RoutineAlarmReceiver::class.java).apply {
      action = ACTION_APPLY_DND
    }
    val pendingDnd = PendingIntent.getBroadcast(
      context,
      (taskId + "_dnd").hashCode(),
      dndIntent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (pendingDnd != null) {
      am.cancel(pendingDnd)
    }
  }

  fun rescheduleAllAlarms(context: Context) {
    try {
      val repository = RoutineRepository(context)
      val anchors = repository.getAnchors().filter { !it.isDeleted }
      val dynamicTasks = repository.getDynamicTasks().filter { !it.isDeleted }
      val sleepDuration = repository.getSleepDurationMinutes()
      val bufferMinutes = repository.getBufferMinutes()
      val calculatedAnchors = SchedulerEngine.calculateAnchors(anchors, sleepDuration)
      val scheduleResult = SchedulerEngine.scheduleDay(
        calculatedAnchors,
        dynamicTasks,
        RoutineDay.today(),
        bufferMinutes
      )

      // Cancel alarms for all overflow tasks
      for (overflow in scheduleResult.overflowTasks) {
        cancelTaskAlarms(context, overflow.id)
      }

      // Schedule alarms strictly for scheduled tasks with their actual scheduled startTimeMinute
      for (task in scheduleResult.scheduledTasks) {
        scheduleTaskAlarms(context, task)
      }
    } catch (_: Exception) {}
  }

  fun rescheduleCompletedTaskNextCycle(context: Context, taskId: String) {
    try {
      val repository = RoutineRepository(context)
      val allTasks = repository.getAnchors() + repository.getDynamicTasks()
      val targetTask = allTasks.firstOrNull { it.id == taskId } ?: return
      if (targetTask.isDeleted || targetTask.isCurrentlyPaused()) return

      // Schedule the next cycle of this task
      scheduleTaskAlarms(context, targetTask)

      // Also ensure all upcoming alarms across the schedule are refreshed
      rescheduleAllAlarms(context)
    } catch (_: Exception) {}
  }

  fun startAutomationService(context: Context, task: RoutineTask) {
    if (task.isDeleted) return
    val (catEmoji, catName) = resolveCategoryInfo(task)

    val endHourMin = ((task.startTimeMinute + task.durationMinutes) % 1440 + 1440) % 1440
    val formattedEndTime = RoutineTask.formatTime(endHourMin)

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
      putExtra(EXTRA_CATEGORY_EMOJI, catEmoji)
      putExtra(EXTRA_CATEGORY_NAME, catName)
      putExtra(EXTRA_FORMATTED_DURATION, task.formattedDuration)
      putExtra(EXTRA_FORMATTED_END_TIME, formattedEndTime)
      putExtra(EXTRA_DND_DELAY_MINUTES, task.dndDelayMinutes)
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
