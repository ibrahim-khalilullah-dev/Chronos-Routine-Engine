package com.example.engine

import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.TaskType
import java.util.UUID

data class ConsoleLineDiagnostic(
  val lineNumber: Int,
  val rawLine: String,
  val isCommentOrEmpty: Boolean = false,
  val isValid: Boolean = true,
  val errorMessage: String? = null,
  val parsedTask: RoutineTask? = null
)

data class ConsoleParseResult(
  val diagnostics: List<ConsoleLineDiagnostic>,
  val validTasks: List<RoutineTask>,
  val parsedAnchors: List<RoutineTask> = emptyList(),
  val backupSleepMinutes: Int? = null,
  val backupBufferMinutes: Int? = null,
  val errorCount: Int,
  val successCount: Int
)

object ConsoleParser {

  // Default is clean and empty so no dummy tasks are accidentally committed
  const val DEFAULT_CONSOLE_TEMPLATE = ""

  // Sample template available on-demand via "Quick-Copy Template"
  val SAMPLE_SNIPPET = """
# Chronos Routine Terminal Console
# Syntax: Task Name | Duration(m) | Preferred Start(HH:mm) | DND(Y/N) | Days | Voice(Y/N) | Custom Notification/Voice
# Days: Sat,Sun,Mon,Tue,Wed,Thu,Fri or ALL, WEEKDAYS, WEEKENDS

Quick Morning Review | 30 | 06:15 | N | ALL | Y | Time for morning review and daily priorities
Client Strategy Sprint | 90 | 10:00 | Y | Mon,Tue,Wed,Thu | Y | Strategy sprint initiated. DND engaged.
Speed Cardio & Stretch | 45 | 16:30 | Y | ALL | N | High intensity cardio block starting now
Algorithm Practice | 60 | 17:30 | N | Mon,Wed,Fri | Y | Time for coding algorithms and problem solving
Weekly Budget Sync | 40 | 21:00 | N | Sat,Sun | N | Review weekly budget and financial balance
""".trimIndent()

  fun parse(text: String): ConsoleParseResult {
    val lines = text.lines()
    val diagnostics = mutableListOf<ConsoleLineDiagnostic>()
    val validTasks = mutableListOf<RoutineTask>()
    val parsedAnchors = mutableListOf<RoutineTask>()
    var sleepMinutes: Int? = null
    var bufferMinutes: Int? = null

    for ((index, rawLine) in lines.withIndex()) {
      val lineNum = index + 1
      val trimmed = rawLine.trim()

      if (trimmed.isEmpty() || trimmed.startsWith("//")) {
        diagnostics.add(
          ConsoleLineDiagnostic(
            lineNumber = lineNum,
            rawLine = rawLine,
            isCommentOrEmpty = true,
            isValid = true
          )
        )
        continue
      }

      if (trimmed.startsWith("#")) {
        // Check for backup configuration directives
        if (trimmed.startsWith("# SLEEP_MINUTES=")) {
          sleepMinutes = trimmed.substringAfter("=").trim().toIntOrNull()
        } else if (trimmed.startsWith("# BUFFER_MINUTES=")) {
          bufferMinutes = trimmed.substringAfter("=").trim().toIntOrNull()
        }
        diagnostics.add(
          ConsoleLineDiagnostic(
            lineNumber = lineNum,
            rawLine = rawLine,
            isCommentOrEmpty = true,
            isValid = true
          )
        )
        continue
      }

      val rawTokens = trimmed.split("|").map { it.trim() }

      // Backup format support: ANCHOR | Slot | Title | Start | Duration | DND | Voice
      if (rawTokens[0].equals("ANCHOR", ignoreCase = true)) {
        if (rawTokens.size >= 5) {
          val slot = rawTokens[1].toIntOrNull() ?: 1
          val title = rawTokens[2]
          val startMinutes = RoutineTask.parseTime24(rawTokens[3]) ?: 480
          val duration = rawTokens[4].toIntOrNull() ?: 60
          val dnd = rawTokens.getOrNull(5)?.equals("Y", ignoreCase = true) == true
          val voice = rawTokens.getOrNull(6)?.equals("N", ignoreCase = true) != true

          val anchorType = when (slot) {
            1 -> TaskType.ANCHOR_SLEEP
            5 -> TaskType.ANCHOR_ELASTIC
            else -> TaskType.ANCHOR_STANDARD
          }

          val anchorTask = RoutineTask(
            id = "slot_$slot",
            title = title,
            type = anchorType,
            slotNumber = slot,
            startTimeMinute = startMinutes,
            durationMinutes = duration,
            enableDnd = dnd,
            isVoiceEnabled = voice
          )
          parsedAnchors.add(anchorTask)
          diagnostics.add(
            ConsoleLineDiagnostic(
              lineNumber = lineNum,
              rawLine = rawLine,
              isValid = true,
              parsedTask = anchorTask
            )
          )
          continue
        }
      }

      // Check if line is prepended with "DYNAMIC |"
      val tokens = if (rawTokens[0].equals("DYNAMIC", ignoreCase = true)) {
        rawTokens.drop(1)
      } else {
        rawTokens
      }

      if (tokens.size < 3) {
        diagnostics.add(
          ConsoleLineDiagnostic(
            lineNumber = lineNum,
            rawLine = rawLine,
            isValid = false,
            errorMessage = "Requires at least: Name | Duration(m) | Start(HH:mm)"
          )
        )
        continue
      }

      val name = tokens[0]
      if (name.isEmpty()) {
        diagnostics.add(
          ConsoleLineDiagnostic(
            lineNumber = lineNum,
            rawLine = rawLine,
            isValid = false,
            errorMessage = "Task name cannot be empty"
          )
        )
        continue
      }

      val duration = tokens[1].toIntOrNull()
      if (duration == null || duration <= 0) {
        diagnostics.add(
          ConsoleLineDiagnostic(
            lineNumber = lineNum,
            rawLine = rawLine,
            isValid = false,
            errorMessage = "Invalid duration '${tokens[1]}'. Must be positive integer in minutes."
          )
        )
        continue
      }

      val startMinutes = RoutineTask.parseTime24(tokens[2])
      if (startMinutes == null) {
        diagnostics.add(
          ConsoleLineDiagnostic(
            lineNumber = lineNum,
            rawLine = rawLine,
            isValid = false,
            errorMessage = "Invalid time '${tokens[2]}'. Format: HH:mm (e.g. 09:30, 14:15) or 12-hr (02:30 PM)."
          )
        )
        continue
      }

      val dndToken = tokens.getOrNull(3)?.uppercase() ?: "N"
      val enableDnd = dndToken == "Y" || dndToken == "YES" || dndToken == "TRUE" || dndToken == "1"

      val daysToken = tokens.getOrNull(4)?.trim() ?: "ALL"
      val parsedDays = parseDaysToken(daysToken)
      if (parsedDays.isEmpty()) {
        diagnostics.add(
          ConsoleLineDiagnostic(
            lineNumber = lineNum,
            rawLine = rawLine,
            isValid = false,
            errorMessage = "No valid days in '$daysToken'. Use Sat,Sun,Mon,Tue,Wed,Thu,Fri or ALL"
          )
        )
        continue
      }

      // Voice Enabled & Custom Message
      var isVoiceEnabled = true
      var customMessage = "$name starting now."

      if (tokens.size >= 7) {
        val voiceToken = tokens[5].uppercase()
        isVoiceEnabled = voiceToken == "Y" || voiceToken == "YES" || voiceToken == "TRUE" || voiceToken == "1"
        customMessage = tokens[6].ifBlank { "$name starting now." }
      } else if (tokens.size == 6) {
        val t5Upper = tokens[5].uppercase()
        if (t5Upper in setOf("Y", "N", "YES", "NO", "TRUE", "FALSE", "1", "0")) {
          isVoiceEnabled = t5Upper == "Y" || t5Upper == "YES" || t5Upper == "TRUE" || t5Upper == "1"
        } else {
          isVoiceEnabled = true
          customMessage = tokens[5].ifBlank { "$name starting now." }
        }
      }

      // Extended fields: Fluid Twin & Ringtone
      val isFluidTwin = tokens.getOrNull(7)?.equals("Y", ignoreCase = true) == true
      val fluidPart = tokens.getOrNull(8)?.toIntOrNull()
      val fluidPool = tokens.getOrNull(9)?.toIntOrNull()
      val ringtoneUri = tokens.getOrNull(10)?.takeIf { it != "DEFAULT" && it.isNotBlank() }
      val ringtoneTitle = tokens.getOrNull(11)?.takeIf { it != "Default" && it.isNotBlank() }

      val task = RoutineTask(
        id = "dynamic_${UUID.randomUUID().toString().take(8)}",
        title = name,
        type = TaskType.DYNAMIC,
        startTimeMinute = startMinutes,
        durationMinutes = duration,
        voiceMessage = customMessage,
        enableDnd = enableDnd,
        enableHaptic = true,
        allowSplitting = !isFluidTwin,
        selectedDays = parsedDays,
        isVoiceEnabled = isVoiceEnabled,
        customNotificationTitle = name,
        customNotificationBody = customMessage,
        isFluidTwin = isFluidTwin,
        fluidTwinId = if (isFluidTwin) "twin_${name.replace(" ", "_")}" else null,
        fluidTwinPart = fluidPart,
        fluidTotalPoolMinutes = fluidPool,
        customRingtoneUri = ringtoneUri,
        customRingtoneTitle = ringtoneTitle
      )

      validTasks.add(task)
      diagnostics.add(
        ConsoleLineDiagnostic(
          lineNumber = lineNum,
          rawLine = rawLine,
          isValid = true,
          parsedTask = task
        )
      )
    }

    val errors = diagnostics.count { !it.isValid }
    return ConsoleParseResult(
      diagnostics = diagnostics,
      validTasks = validTasks,
      parsedAnchors = parsedAnchors,
      backupSleepMinutes = sleepMinutes,
      backupBufferMinutes = bufferMinutes,
      errorCount = errors,
      successCount = validTasks.size + parsedAnchors.size
    )
  }

  private fun parseDaysToken(token: String): Set<RoutineDay> {
    val upper = token.uppercase().trim()
    if (upper == "ALL" || upper == "DAILY" || upper == "*") {
      return RoutineDay.allDays()
    }
    if (upper == "WEEKDAYS") {
      return setOf(
        RoutineDay.MONDAY,
        RoutineDay.TUESDAY,
        RoutineDay.WEDNESDAY,
        RoutineDay.THURSDAY,
        RoutineDay.FRIDAY
      )
    }
    if (upper == "WEEKENDS") {
      return setOf(RoutineDay.SATURDAY, RoutineDay.SUNDAY)
    }

    val dayStrings = token.split(",").map { it.trim() }
    val result = mutableSetOf<RoutineDay>()
    for (ds in dayStrings) {
      val matched = RoutineDay.fromString(ds)
      if (matched != null) {
        result.add(matched)
      }
    }
    return result
  }
}
