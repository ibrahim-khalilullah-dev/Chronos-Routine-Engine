package com.example.model

import androidx.compose.runtime.Immutable

enum class RoutineDay(val shortName: String, val fullName: String) {
  SATURDAY("Sat", "Saturday"),
  SUNDAY("Sun", "Sunday"),
  MONDAY("Mon", "Monday"),
  TUESDAY("Tue", "Tuesday"),
  WEDNESDAY("Wed", "Wednesday"),
  THURSDAY("Thu", "Thursday"),
  FRIDAY("Fri", "Friday");

  companion object {
    fun fromString(str: String): RoutineDay? {
      val trimmed = str.trim()
      return entries.firstOrNull {
        it.name.equals(trimmed, ignoreCase = true) ||
          it.shortName.equals(trimmed, ignoreCase = true) ||
          it.fullName.equals(trimmed, ignoreCase = true)
      }
    }

    fun allDays(): Set<RoutineDay> = entries.toSet()
  }
}

enum class TaskType {
  ANCHOR_SLEEP,      // Slot 1: Reverse Sleep Tracker
  ANCHOR_STANDARD,   // Slots 2, 3, 4, 6, 7
  ANCHOR_ELASTIC,    // Slot 5: Rubber Buffer Slot
  DYNAMIC            // User-added flexible dynamic tasks
}

@Immutable
data class RoutineTask(
  val id: String,
  val title: String,
  val type: TaskType,
  val slotNumber: Int? = null, // 1 through 7 for anchors
  val startTimeMinute: Int,    // 0..1439 (minutes from 00:00)
  val durationMinutes: Int,
  val voiceMessage: String = "",
  val enableDnd: Boolean = false,
  val enableHaptic: Boolean = true,
  val allowSplitting: Boolean = true,
  val selectedDays: Set<RoutineDay> = RoutineDay.allDays(),
  val isSplitPart: Boolean = false,
  val splitPartLabel: String? = null, // e.g. "Part 1/2 (45m)"
  val parentTaskId: String? = null,
  val isOverflow: Boolean = false,
  val overflowReason: String? = null,
  val isDeleted: Boolean = false,
  val deletedTimestamp: Long? = null,
  val isVoiceEnabled: Boolean = true,
  val customNotificationTitle: String = "",
  val customNotificationBody: String = "",
  val isFluidTwin: Boolean = false,
  val fluidTwinId: String? = null,
  val fluidTwinPart: Int? = null,
  val fluidTotalPoolMinutes: Int? = null,
  val customRingtoneUri: String? = null,
  val customRingtoneTitle: String? = null,
  val isSoundAlertEnabled: Boolean = true,
  val toleranceMinutes: Int = 0, // Schedule Flexibility (0 to 15m, default 0)
  val interTaskBufferMinutes: Int = 5 // Inter-task Rest Buffer (0 to 30m, default 5)
) {
  val endTimeMinute: Int
    get() = (startTimeMinute + durationMinutes) % 1440

  val formattedStartTime: String
    get() = formatTime(startTimeMinute)

  val formattedEndTime: String
    get() = formatTime(startTimeMinute + durationMinutes)

  val formattedDuration: String
    get() {
      val h = durationMinutes / 60
      val m = durationMinutes % 60
      return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
      }
    }

  val notificationDisplayTitle: String
    get() = customNotificationTitle.ifBlank { title }

  val notificationDisplayBody: String
    get() = customNotificationBody.ifBlank {
      voiceMessage.ifBlank { "$title starting now." }
    }

  val daysRemainingInTrash: Int
    get() {
      val ts = deletedTimestamp ?: return 30
      val elapsedMillis = System.currentTimeMillis() - ts
      val elapsedDays = (elapsedMillis / (1000L * 60 * 60 * 24)).toInt()
      return (30 - elapsedDays).coerceAtLeast(0)
    }

  companion object {
    fun formatTime(totalMinutes: Int): String {
      val normalized = ((totalMinutes % 1440) + 1440) % 1440
      val hours = normalized / 60
      val mins = normalized % 60
      val amPm = if (hours < 12) "AM" else "PM"
      val displayHour = when (val h = hours % 12) {
        0 -> 12
        else -> h
      }
      return String.format("%02d:%02d %s", displayHour, mins, amPm)
    }

    fun formatTime24(totalMinutes: Int): String {
      val normalized = ((totalMinutes % 1440) + 1440) % 1440
      val hours = normalized / 60
      val mins = normalized % 60
      return String.format("%02d:%02d", hours, mins)
    }

    fun parseTime24(input: String): Int? {
      val trimmed = input.trim()
      val isPm = trimmed.endsWith("PM", ignoreCase = true)
      val isAm = trimmed.endsWith("AM", ignoreCase = true)
      val cleanTime = trimmed
        .replace("AM", "", ignoreCase = true)
        .replace("PM", "", ignoreCase = true)
        .trim()

      val parts = cleanTime.split(":")
      if (parts.size != 2) return null
      var hours = parts[0].toIntOrNull() ?: return null
      val mins = parts[1].toIntOrNull() ?: return null

      if (mins !in 0..59) return null

      if (isAm || isPm) {
        if (hours !in 1..12) return null
        if (isAm && hours == 12) hours = 0
        else if (isPm && hours < 12) hours += 12
      } else {
        if (hours !in 0..23) return null
      }

      return hours * 60 + mins
    }
  }
}

@Immutable
data class ScheduleGap(
  val startMinute: Int,
  val endMinute: Int,
  val durationMinutes: Int = endMinute - startMinute
)
