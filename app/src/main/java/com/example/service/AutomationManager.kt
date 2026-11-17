package com.example.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import java.util.Locale

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
  const val EXTRA_TASK_END_MILLIS = "extra_task_end_millis"

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

  private val bengaliTtsLocales: List<Locale> = listOf(
    Locale.forLanguageTag("bn-BD"),
    Locale.forLanguageTag("bn-IN"),
    Locale("bn")
  )

  fun containsBengali(text: String): Boolean {
    return text.any { it in '\u0980'..'\u09FF' }
  }

  fun isBengaliTtsAvailable(tts: TextToSpeech?): Boolean {
    if (tts == null) return false
    return try {
      val localeAvailable = bengaliTtsLocales.any { locale ->
        val res = tts.isLanguageAvailable(locale)
        res >= TextToSpeech.LANG_AVAILABLE
      }
      if (localeAvailable) return true
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val voices = tts.voices
        voices?.any { voice ->
          voice.locale?.language.equals("bn", ignoreCase = true) &&
            !voice.isNetworkConnectionRequired
        } == true
      } else {
        false
      }
    } catch (_: Exception) {
      false
    }
  }

  data class VoicePreset(
    val id: String,
    val label: String,
    val emoji: String,
    val description: String,
    val category: String,
    val preferredGender: String,
    val pitch: Float,
    val nativeMalePitch: Float? = null,
    val speechRate: Float,
    val defaultSampleText: String
  )

  val VOICE_PRESETS: List<VoicePreset> = listOf(
    // Female Collection
    VoicePreset(
      id = "natural_female",
      label = "Natural Female",
      emoji = "🌸",
      description = "Warm, clear & balanced studio voice",
      category = "female",
      preferredGender = "female",
      pitch = 1.04f,
      speechRate = 0.98f,
      defaultSampleText = "স্বাগতম! আপনার নির্ধারিত রুটিন কাজ শুরু করার সময় হয়েছে।"
    ),
    VoicePreset(
      id = "calm_female",
      label = "Calm Soft Female",
      emoji = "🌙",
      description = "Soft, gentle & unhurried cadence",
      category = "female",
      preferredGender = "female",
      pitch = 1.10f,
      speechRate = 0.88f,
      defaultSampleText = "শান্ত মনে আপনার পরবর্তী কাজটি এখনই শুরু করুন।"
    ),
    VoicePreset(
      id = "sweet_melodic_female",
      label = "Sweet Melodic Female",
      emoji = "✨",
      description = "Bright, friendly & uplifting tone",
      category = "female",
      preferredGender = "female",
      pitch = 1.18f,
      speechRate = 1.02f,
      defaultSampleText = "নতুন সেশনের সময় হয়ে গেছে, মনোযোগ দিয়ে শুরু করুন।"
    ),
    VoicePreset(
      id = "executive_female",
      label = "Crisp Executive Female",
      emoji = "💼",
      description = "Articulate, confident & focused pace",
      category = "female",
      preferredGender = "female",
      pitch = 0.98f,
      speechRate = 1.07f,
      defaultSampleText = "আপনার ফোকাস টাইম শুরু হয়েছে, এখন কাজ শুরু করুন।"
    ),
    VoicePreset(
      id = "night_sanctuary_female",
      label = "Night Sanctuary Female",
      emoji = "🕯️",
      description = "Extra soft, slow & peaceful bedtime tone",
      category = "female",
      preferredGender = "female",
      pitch = 1.06f,
      speechRate = 0.80f,
      defaultSampleText = "রাতের বিশ্রামের সময় হয়েছে, সারাদিনের কাজ গুছিয়ে নিন।"
    ),
    // Youth 10–15 yrs Collection
    VoicePreset(
      id = "teen_girl",
      label = "Young Girl (10–15 yrs)",
      emoji = "🎀",
      description = "Light, bright & youthful girl voice",
      category = "youth",
      preferredGender = "female",
      pitch = 1.36f,
      speechRate = 1.05f,
      defaultSampleText = "হ্যালো! আপনার রুটিনের নতুন কাজ শুরু করার সময় হয়েছে।"
    ),
    VoicePreset(
      id = "cheerful_kid_girl",
      label = "Sweet Kid Girl (10–12 yrs)",
      emoji = "🦋",
      description = "Airy, cheerful & sweet young girl tone",
      category = "youth",
      preferredGender = "female",
      pitch = 1.48f,
      speechRate = 1.08f,
      defaultSampleText = "সময় হয়ে গেছে! এখন সুন্দরভাবে কাজটি শুরু করা যাক।"
    ),
    VoicePreset(
      id = "teen_boy",
      label = "Young Boy (10–15 yrs)",
      emoji = "🧢",
      description = "Clear, lively & youthful boy voice",
      category = "youth",
      preferredGender = "male",
      pitch = 1.14f,
      nativeMalePitch = 1.34f,
      speechRate = 1.05f,
      defaultSampleText = "হ্যালো! এখন আপনার নির্ধারিত রুটিন সেশন শুরু করার সময়।"
    ),
    // Male & Studio Collection
    VoicePreset(
      id = "deep_male",
      label = "Deep Calm Male",
      emoji = "🎙️",
      description = "Rich, resonant & commanding baritone",
      category = "male",
      preferredGender = "male",
      pitch = 0.74f,
      nativeMalePitch = 0.88f,
      speechRate = 0.93f,
      defaultSampleText = "এখন আপনার নির্ধারিত গুরুত্বপূর্ণ কাজ শুরু করার সময় হয়েছে।"
    ),
    VoicePreset(
      id = "crisp_male",
      label = "Crisp Focus Male",
      emoji = "⚡",
      description = "Energetic, articulate & brisk delivery",
      category = "male",
      preferredGender = "male",
      pitch = 0.83f,
      nativeMalePitch = 0.97f,
      speechRate = 1.04f,
      defaultSampleText = "সময় হয়ে গেছে, পূর্ণ মনোযোগে কাজটি শুরু করুন।"
    ),
    VoicePreset(
      id = "mentor_male",
      label = "Warm Mentor Male",
      emoji = "📖",
      description = "Steady, wise & unhurried narrator",
      category = "male",
      preferredGender = "male",
      pitch = 0.78f,
      nativeMalePitch = 0.92f,
      speechRate = 0.86f,
      defaultSampleText = "ধীরস্থিরভাবে আপনার পরবর্তী রুটিন ধাপটি শুরু করুন।"
    ),
    VoicePreset(
      id = "neutral_studio",
      label = "Neutral Studio",
      emoji = "🎧",
      description = "Balanced default reference tone",
      category = "male",
      preferredGender = "neutral",
      pitch = 1.0f,
      speechRate = 1.0f,
      defaultSampleText = "আপনার রুটিন অ্যালার্ট এখন সক্রিয় হয়েছে।"
    )
  )

  fun getVoicePreset(id: String?): VoicePreset {
    return VOICE_PRESETS.firstOrNull { it.id == id } ?: VOICE_PRESETS.first()
  }

  private fun matchesPreferredGender(voiceName: String, preferredGender: String): Boolean {
    val lower = voiceName.lowercase(Locale.ROOT)
    val isFemale = lower.contains("female") ||
      lower.contains("woman") ||
      lower.contains("bnf") ||
      lower.contains("bdf") ||
      lower.contains("sfg") ||
      lower.contains("tpf") ||
      lower.contains("iob")
    val isMale = !lower.contains("female") &&
      !lower.contains("woman") &&
      (lower.contains("male") ||
        lower.contains("man") ||
        lower.contains("bnm") ||
        lower.contains("bdm") ||
        lower.contains("tpm") ||
        lower.contains("iom") ||
        lower.contains("sfs") ||
        lower.contains("bnc"))
    return when (preferredGender) {
      "female" -> isFemale
      "male" -> isMale
      else -> false
    }
  }

  fun applyTtsLanguage(
    tts: TextToSpeech?,
    text: String,
    voicePresetId: String = "natural_female"
  ): Boolean {
    if (tts == null) return false
    return try {
      val preset = getVoicePreset(voicePresetId)
      var selectedDistinctMaleVoice = false
      val languageOk = if (containsBengali(text)) {
        val matchedLocale = bengaliTtsLocales.firstOrNull { locale ->
          tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE
        }
        if (matchedLocale != null) {
          tts.language = matchedLocale
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val bnVoices = tts.voices?.filter { voice ->
              voice.locale?.language.equals("bn", ignoreCase = true)
            }.orEmpty()
            val matchingVoice = if (preset.preferredGender != "neutral") {
              bnVoices.firstOrNull { !it.isNetworkConnectionRequired && matchesPreferredGender(it.name, preset.preferredGender) }
                ?: bnVoices.firstOrNull { matchesPreferredGender(it.name, preset.preferredGender) }
                ?: bnVoices.firstOrNull { !it.isNetworkConnectionRequired }
                ?: bnVoices.firstOrNull()
            } else {
              bnVoices.firstOrNull { !it.isNetworkConnectionRequired } ?: bnVoices.firstOrNull()
            }
            if (matchingVoice != null) {
              tts.voice = matchingVoice
              if (preset.preferredGender == "male" && matchesPreferredGender(matchingVoice.name, "male")) {
                selectedDistinctMaleVoice = true
              }
            }
          }
          true
        } else {
          tts.language = Locale.forLanguageTag("bn-BD")
          false
        }
      } else {
        val englishCandidates = listOf(Locale.US, Locale.ENGLISH, Locale.getDefault())
        val matchedEnglish = englishCandidates.firstOrNull { locale ->
          tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE
        } ?: Locale.US
        tts.language = matchedEnglish
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          val enVoices = tts.voices?.filter { voice ->
            voice.locale?.language.equals("en", ignoreCase = true)
          }.orEmpty()
          val matchingVoice = if (preset.preferredGender != "neutral") {
            enVoices.firstOrNull { !it.isNetworkConnectionRequired && matchesPreferredGender(it.name, preset.preferredGender) }
              ?: enVoices.firstOrNull { matchesPreferredGender(it.name, preset.preferredGender) }
          } else {
            null
          }
          if (matchingVoice != null) {
            tts.voice = matchingVoice
            if (preset.preferredGender == "male" && matchesPreferredGender(matchingVoice.name, "male")) {
              selectedDistinctMaleVoice = true
            }
          }
        }
        true
      }

      val effectivePitch = if (preset.preferredGender == "male" && selectedDistinctMaleVoice) {
        preset.nativeMalePitch ?: (preset.pitch + 0.14f).coerceAtMost(0.98f)
      } else {
        preset.pitch
      }
      tts.setPitch(effectivePitch)
      tts.setSpeechRate(preset.speechRate)
      languageOk
    } catch (_: Exception) {
      false
    }
  }

  fun hasGoogleTtsEngine(context: Context): Boolean {
    val googlePkg = "com.google.android.tts"
    val pm = context.packageManager ?: return false
    try {
      val ttsIntent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
      val services = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentServices(ttsIntent, PackageManager.ResolveInfoFlags.of(0L))
      } else {
        @Suppress("DEPRECATION")
        pm.queryIntentServices(ttsIntent, 0)
      }
      if (services.any { it.serviceInfo?.packageName == googlePkg }) {
        return true
      }
    } catch (_: Exception) {}

    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(googlePkg, PackageManager.PackageInfoFlags.of(0L))
      } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(googlePkg, 0)
      }
      true
    } catch (_: Exception) {
      false
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
  private const val KEY_ACTIVE_DND_TASK_ID = "key_active_dnd_task_id"

  fun getActiveDndTaskId(context: Context): String? {
    val prefs = context.getSharedPreferences(PREFS_DND, Context.MODE_PRIVATE)
    return prefs.getString(KEY_ACTIVE_DND_TASK_ID, null)
  }

  fun setDndMode(context: Context, enabled: Boolean, taskId: String? = null): Boolean {
    if (!isDndPermissionGranted(context)) return false
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
    val prefs = context.getSharedPreferences(PREFS_DND, Context.MODE_PRIVATE)
    return try {
      if (enabled) {
        val currentFilter = nm.currentInterruptionFilter
        val editor = prefs.edit()
        if (currentFilter != NotificationManager.INTERRUPTION_FILTER_PRIORITY) {
          editor.putInt(KEY_PREVIOUS_DND_FILTER, currentFilter)
        } else if (!prefs.contains(KEY_PREVIOUS_DND_FILTER)) {
          editor.putInt(KEY_PREVIOUS_DND_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        }
        if (!taskId.isNullOrBlank()) {
          editor.putString(KEY_ACTIVE_DND_TASK_ID, taskId)
        }
        editor.commit()
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
      } else {
        val savedFilter = prefs.getInt(KEY_PREVIOUS_DND_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        prefs.edit().remove(KEY_ACTIVE_DND_TASK_ID).commit()
        nm.setInterruptionFilter(savedFilter)
      }
      true
    } catch (_: SecurityException) {
      false
    } catch (_: Exception) {
      false
    }
  }

  fun releaseDndIfHeldByTask(context: Context, taskId: String) {
    if (taskId.isBlank()) return
    val activeDndTaskId = getActiveDndTaskId(context)
    if (activeDndTaskId == taskId) {
      setDndMode(context, false, taskId)
    }
  }

  fun hasZeroDelayFollowUpDndTask(context: Context, endingTaskId: String?): Boolean {
    if (endingTaskId.isNullOrBlank()) return false
    return try {
      val repository = RoutineRepository(context)
      val anchors = repository.getAnchors().filter { !it.isDeleted }
      val dynamicTasks = repository.getDynamicTasks().filter { !it.isDeleted }
      val sleepDuration = repository.getSleepDurationMinutes()
      val bufferMinutes = repository.getBufferMinutes()
      val calculatedAnchors = SchedulerEngine.calculateAnchors(anchors, sleepDuration)
      val todaySchedule = SchedulerEngine.scheduleDay(
        calculatedAnchors,
        dynamicTasks,
        RoutineDay.today(),
        bufferMinutes
      )
      val endingTask = todaySchedule.scheduledTasks.firstOrNull { it.id == endingTaskId } ?: return false
      val endMinuteOfDay = ((endingTask.startTimeMinute + endingTask.durationMinutes) % 1440 + 1440) % 1440
      todaySchedule.scheduledTasks.any { other ->
        other.id != endingTaskId &&
          other.enableDnd &&
          other.dndDelayMinutes == 0 &&
          ((other.startTimeMinute % 1440) + 1440) % 1440 == endMinuteOfDay
      }
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
    if (task.slotNumber == 5 && task.selectedDays.isEmpty()) {
      cancelTaskAlarms(context, task.id)
      return
    }

    val startMillis = calculateNextOccurrenceMillis(task)
    if (startMillis <= 0L) {
      cancelTaskAlarms(context, task.id)
      return
    }

    val targetCal = Calendar.getInstance().apply { timeInMillis = startMillis }
    val targetRoutineDay = calendarToRoutineDay(targetCal)
    scheduleTaskAlarmsForOccurrence(context, task, startMillis, targetRoutineDay)
  }

  private fun calendarToRoutineDay(cal: Calendar): RoutineDay {
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
      Calendar.SATURDAY -> RoutineDay.SATURDAY
      Calendar.SUNDAY -> RoutineDay.SUNDAY
      Calendar.MONDAY -> RoutineDay.MONDAY
      Calendar.TUESDAY -> RoutineDay.TUESDAY
      Calendar.WEDNESDAY -> RoutineDay.WEDNESDAY
      Calendar.THURSDAY -> RoutineDay.THURSDAY
      Calendar.FRIDAY -> RoutineDay.FRIDAY
      else -> RoutineDay.SATURDAY
    }
  }

  private data class PlannedAlarmItem(
    val triggerMillis: Long,
    val pendingIntent: PendingIntent,
    val eligibleForAlarmClock: Boolean
  )

  private fun formatDurationMinutes(durationMinutes: Int): String {
    val safeMins = durationMinutes.coerceAtLeast(0)
    val h = safeMins / 60
    val m = safeMins % 60
    return when {
      h > 0 && m > 0 -> "${h}h ${m}m"
      h > 0 -> "${h}h"
      else -> "${m}m"
    }
  }

  private fun buildTaskAlarmsForOccurrence(
    context: Context,
    task: RoutineTask,
    startMillis: Long,
    targetRoutineDay: RoutineDay,
    now: Long = System.currentTimeMillis()
  ): List<PlannedAlarmItem> {
    if (task.isDeleted || task.isCurrentlyPaused() || task.isOverflow) {
      return emptyList()
    }
    if (task.slotNumber == 5 && task.selectedDays.isEmpty()) {
      return emptyList()
    }

    var effectiveDurationMin = task.durationMinutes
    var effectiveTitle = task.title
    var effectiveVoiceMessage = task.voiceMessage
    val effectiveDndDelay = task.dndDelayMinutes.coerceIn(0, 25)

    // Slot 3 Friday dual-mode check: if target day is Friday, use Friday title/duration/voice overrides while respecting user's custom DND delay
    if (task.slotNumber == 3 && targetRoutineDay == RoutineDay.FRIDAY) {
      effectiveTitle = task.fridayTitle?.ifBlank { null } ?: task.title
      effectiveDurationMin = task.fridayDurationMinutes ?: task.durationMinutes
      effectiveVoiceMessage = task.fridayVoiceMessage?.ifBlank { null } ?: task.voiceMessage
    }

    val targetCal = Calendar.getInstance().apply { timeInMillis = startMillis }
    val (catEmoji, catName) = resolveCategoryInfo(task, targetRoutineDay)

    val endHourMin = ((targetCal.get(Calendar.HOUR_OF_DAY) * 60 + targetCal.get(Calendar.MINUTE) + effectiveDurationMin) % 1440 + 1440) % 1440
    val formattedEndTime = RoutineTask.formatTime(endHourMin)
    val formattedEffectiveDuration = formatDurationMinutes(effectiveDurationMin)
    val endMillis = startMillis + (effectiveDurationMin * 60L * 1000L)
    val items = mutableListOf<PlannedAlarmItem>()

    // 1. Task Start Alarm (only if start time is in the future)
    if (startMillis > now + 1500L) {
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
        putExtra(EXTRA_FORMATTED_DURATION, formattedEffectiveDuration)
        putExtra(EXTRA_FORMATTED_END_TIME, formattedEndTime)
        putExtra(EXTRA_DND_DELAY_MINUTES, effectiveDndDelay)
      }

      val pendingStart = PendingIntent.getBroadcast(
        context,
        task.id.hashCode(),
        startIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      items.add(PlannedAlarmItem(startMillis, pendingStart, eligibleForAlarmClock = true))
    }

    // Deep Sleep Immune Delayed DND via AlarmManager (only if trigger precedes task end)
    if (task.enableDnd && effectiveDndDelay > 0) {
      val dndTriggerMillis = startMillis + (effectiveDndDelay * 60L * 1000L)
      if (dndTriggerMillis > now && dndTriggerMillis < endMillis) {
        val dndIntent = Intent(context, RoutineAlarmReceiver::class.java).apply {
          action = ACTION_APPLY_DND
          putExtra(EXTRA_TASK_ID, task.id)
          putExtra(EXTRA_ENABLE_DND, task.enableDnd)
          putExtra(EXTRA_DND_DELAY_MINUTES, effectiveDndDelay)
          putExtra(EXTRA_TASK_END_MILLIS, endMillis)
        }
        val pendingDnd = PendingIntent.getBroadcast(
          context,
          (task.id + "_dnd").hashCode(),
          dndIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        items.add(PlannedAlarmItem(dndTriggerMillis, pendingDnd, eligibleForAlarmClock = true))
      }
    }

    // Synchronize end alarm
    if (endMillis > now) {
      val endIntent = Intent(context, RoutineAlarmReceiver::class.java).apply {
        action = ACTION_END_TASK
        putExtra(EXTRA_TASK_ID, task.id)
        putExtra(EXTRA_ENABLE_DND, task.enableDnd)
        putExtra(EXTRA_DND_DELAY_MINUTES, effectiveDndDelay)
      }
      val pendingEnd = PendingIntent.getBroadcast(
        context,
        (task.id + "_end").hashCode(),
        endIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      items.add(PlannedAlarmItem(endMillis, pendingEnd, eligibleForAlarmClock = task.enableDnd))
    }

    return items
  }

  private fun scheduleTaskAlarmsForOccurrence(
    context: Context,
    task: RoutineTask,
    startMillis: Long,
    targetRoutineDay: RoutineDay,
    now: Long = System.currentTimeMillis(),
    skipCancel: Boolean = false
  ) {
    if (task.isDeleted || task.isCurrentlyPaused() || task.isOverflow) {
      if (!skipCancel) cancelTaskAlarms(context, task.id)
      return
    }
    val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    if (!skipCancel) {
      cancelTaskAlarms(context, task.id)
    }

    val items = buildTaskAlarmsForOccurrence(context, task, startMillis, targetRoutineDay, now)
    val earliestAlarmClockMillis = items.filter { it.eligibleForAlarmClock }.minOfOrNull { it.triggerMillis }
    for (item in items) {
      val useAlarmClock = item.eligibleForAlarmClock &&
        earliestAlarmClockMillis != null &&
        item.triggerMillis == earliestAlarmClockMillis
      scheduleExactOrAlarmClock(am, item.triggerMillis, item.pendingIntent, useAlarmClock = useAlarmClock)
    }
  }

  private fun scheduleExactOrAlarmClock(
    am: AlarmManager,
    triggerMillis: Long,
    pendingIntent: PendingIntent,
    useAlarmClock: Boolean
  ) {
    try {
      if (useAlarmClock) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          if (am.canScheduleExactAlarms()) {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerMillis, pendingIntent), pendingIntent)
          } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
          }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerMillis, pendingIntent), pendingIntent)
        } else {
          am.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
      } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && am.canScheduleExactAlarms()) {
          am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
          am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
          am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
      }
    } catch (_: SecurityException) {
      try {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
      } catch (_: Exception) {}
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
      pendingStart.cancel()
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
      pendingEnd.cancel()
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
      pendingDnd.cancel()
    }
  }

  fun reconcileStaleDndState(context: Context, nowMillis: Long = System.currentTimeMillis()) {
    try {
      val activeDndTaskId = getActiveDndTaskId(context) ?: return
      val repository = RoutineRepository(context)
      val anchors = repository.getAnchors().filter { !it.isDeleted }
      val dynamicTasks = repository.getDynamicTasks().filter { !it.isDeleted }
      val sleepDuration = repository.getSleepDurationMinutes()
      val bufferMinutes = repository.getBufferMinutes()
      val calculatedAnchors = SchedulerEngine.calculateAnchors(anchors, sleepDuration)

      val baseCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
      var hasValidOngoingDndOccurrence = false

      for (dayOffset in -1..0) {
        val dayCal = (baseCal.clone() as Calendar).apply {
          add(Calendar.DAY_OF_YEAR, dayOffset)
        }
        val routineDay = calendarToRoutineDay(dayCal)
        val daySchedule = SchedulerEngine.scheduleDay(
          calculatedAnchors,
          dynamicTasks,
          routineDay,
          bufferMinutes
        )

        val matchingTask = daySchedule.scheduledTasks.firstOrNull { it.id == activeDndTaskId }
        if (matchingTask != null && !matchingTask.isDeleted && !matchingTask.isCurrentlyPaused() && matchingTask.enableDnd) {
          val startCal = (dayCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, matchingTask.startTimeMinute / 60)
            set(Calendar.MINUTE, matchingTask.startTimeMinute % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
          }
          val startMillis = startCal.timeInMillis
          val endMillis = startMillis + matchingTask.durationMinutes * 60L * 1000L
          val effectiveDndDelay = matchingTask.dndDelayMinutes.coerceIn(0, 25)
          val effectiveDndStartMillis = startMillis + effectiveDndDelay * 60L * 1000L
          if (nowMillis in effectiveDndStartMillis until endMillis) {
            hasValidOngoingDndOccurrence = true
            break
          }
        }
      }

      if (!hasValidOngoingDndOccurrence) {
        if (!setDndMode(context, false, activeDndTaskId)) {
          val prefs = context.getSharedPreferences(PREFS_DND, Context.MODE_PRIVATE)
          if (prefs.getString(KEY_ACTIVE_DND_TASK_ID, null) == activeDndTaskId) {
            prefs.edit().remove(KEY_ACTIVE_DND_TASK_ID).commit()
          }
        }
      }
    } catch (_: Exception) {}
  }

  fun rescheduleAllAlarms(context: Context) {
    rescheduleAllAlarmsAt(context, System.currentTimeMillis())
  }

  internal fun rescheduleAllAlarmsAt(context: Context, now: Long) {
    try {
      reconcileStaleDndState(context, now)
      val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
      val repository = RoutineRepository(context)
      val anchors = repository.getAnchors().filter { !it.isDeleted }
      val dynamicTasks = repository.getDynamicTasks().filter { !it.isDeleted }
      val sleepDuration = repository.getSleepDurationMinutes()
      val bufferMinutes = repository.getBufferMinutes()
      val calculatedAnchors = SchedulerEngine.calculateAnchors(anchors, sleepDuration)

      // Purge any legacy slot_* alarms and stale alarms before re-registering upcoming horizon
      for (slot in 1..7) {
        cancelTaskAlarms(context, "slot_$slot")
      }
      for (anchor in calculatedAnchors) {
        cancelTaskAlarms(context, anchor.id)
      }
      for (dyn in dynamicTasks) {
        cancelTaskAlarms(context, dyn.id)
      }

      val baseCal = Calendar.getInstance().apply { timeInMillis = now }
      val scheduledTaskIds = mutableSetOf<String>()
      val allPlannedAlarms = mutableListOf<PlannedAlarmItem>()

      for (dayOffset in -1..6) {
        val dayCal = (baseCal.clone() as Calendar).apply {
          add(Calendar.DAY_OF_YEAR, dayOffset)
        }
        val routineDay = calendarToRoutineDay(dayCal)
        val daySchedule = SchedulerEngine.scheduleDay(
          calculatedAnchors,
          dynamicTasks,
          routineDay,
          bufferMinutes
        )

        for (task in daySchedule.scheduledTasks) {
          if (task.id in scheduledTaskIds) continue
          val startCal = (dayCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, task.startTimeMinute / 60)
            set(Calendar.MINUTE, task.startTimeMinute % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
          }
          val startMillis = startCal.timeInMillis
          val endMillis = startMillis + task.durationMinutes * 60L * 1000L

          val isUpcomingOrOngoing = if (dayOffset <= 0) {
            endMillis > now
          } else {
            startMillis > now
          }

          if (isUpcomingOrOngoing) {
            allPlannedAlarms.addAll(
              buildTaskAlarmsForOccurrence(context, task, startMillis, routineDay, now)
            )
            scheduledTaskIds.add(task.id)
          }
        }
      }

      // Single earliest upcoming alarm timestamp across Start / Delayed-DND / DND-End uses setAlarmClock()
      // to pierce Deep Doze without flooding SystemUI; all subsequent future alarms use setExactAndAllowWhileIdle().
      val earliestAlarmClockMillis = allPlannedAlarms
        .filter { it.eligibleForAlarmClock }
        .minOfOrNull { it.triggerMillis }

      for (item in allPlannedAlarms) {
        val useAlarmClock = item.eligibleForAlarmClock &&
          earliestAlarmClockMillis != null &&
          item.triggerMillis == earliestAlarmClockMillis
        scheduleExactOrAlarmClock(am, item.triggerMillis, item.pendingIntent, useAlarmClock = useAlarmClock)
      }
    } catch (_: Exception) {}
  }

  fun rescheduleCompletedTaskNextCycle(context: Context, taskId: String) {
    try {
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
