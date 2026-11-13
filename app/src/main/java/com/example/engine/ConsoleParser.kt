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

  // Sample template showcasing user's precision circadian routine
  val SAMPLE_SNIPPET = """
# ==============================================================
# CHRONOS PRECISION CIRCADIAN ROUTINE
# ==============================================================

# 1. THE 7 CIRCADIAN ANCHORS (Daily Fixed Boundaries)
ANCHOR | 1 | রাতের ঘুম | 23:00 | 360 | Y | 0 | Y | শুভ রাত্রি।
ANCHOR | 2 | ফজরের সালাত | 05:40 | 20 | Y | 5 | Y | ফজরের সালাতের সময়।
ANCHOR | 3 | জোহরের সালাত | 13:25 | 25 | Y | 5 | Y | জোহরের সালাতের সময়। | জুমার সালাত | 12:30 | 120 | জুমার সালাতের সময়।
ANCHOR | 4 | আসরের সালাত | 16:15 | 15 | Y | 5 | Y | আসরের সালাতের সময়।
ANCHOR | 5 | ফ্রি টাইম | 16:30 | 45 | N | 0 | Y | বিকেলের ফ্রি টাইম।
ANCHOR | 6 | মাগরিবের সালাত | 17:25 | 20 | Y | 5 | Y | মাগরিবের সালাতের সময়।
ANCHOR | 7 | এশার সালাত | 19:45 | 25 | Y | 5 | Y | এশার সালাতের সময়।

# 2. SATURDAY - THURSDAY DAILY DYNAMIC ROUTINE
# Format: Name | Dur(m) | Start(HH:mm) | DND(Y/N) | Days | Voice(Y/N) | Message | Flex(0-30) | Rest(0-30) | Priority(Y/N) | Category
ঘুম থেকে ওঠা | 25 | 05:00 | N | Sat,Sun,Mon,Tue,Wed,Thu | Y | ঘুম থেকে ওঠার সময়। | 5 | 0 | N | general
ফজর প্রস্তুতি | 15 | 05:25 | N | Sat,Sun,Mon,Tue,Wed,Thu | Y | ফজরের প্রস্তুতি নিন। | 5 | 0 | N | general
শরীরচর্চা | 40 | 06:05 | N | Sat,Sun,Mon,Tue,Wed,Thu | Y | শরীরচর্চা শুরু করুন। | 10 | 5 | N | training
সকালের নাস্তা ও প্রস্তুতি | 45 | 06:50 | N | Sat,Sun,Mon,Tue,Wed,Thu | Y | সকালের নাস্তা সেরে নিন। | 10 | 5 | N | eating
কোর ফোকাস ব্লক ১ | 150 | 07:45 | Y | Sat,Sun,Mon,Tue,Wed,Thu | Y | প্রথম ফোকাস সেশন শুরু। | 15 | 5 | Y | work
হালকা রিফ্রেশমেন্ট | 20 | 10:20 | N | Sat,Sun,Mon,Tue,Wed,Thu | Y | কিছুটা বিশ্রাম নিন। | 5 | 5 | N | break
কোর ফোকাস ব্লক ২ | 160 | 10:45 | Y | Sat,Sun,Mon,Tue,Wed,Thu | Y | দ্বিতীয় ফোকাস সেশন শুরু। | 15 | 0 | Y | work
দুপুরের খাবার ও বিশ্রাম | 60 | 13:55 | N | Sat,Sun,Mon,Tue,Wed,Thu | Y | খাবার ও কাইলুলাহ বিশ্রাম। | 15 | 5 | N | eating
বিকেলের স্টাডি ও স্কিল | 75 | 15:00 | N | Sat,Sun,Mon,Tue,Wed,Thu | Y | স্কিল প্র্যাকটিস ও পড়াশোনা। | 10 | 0 | N | studying
সন্ধ্যার কোডিং ও প্রজেক্ট | 115 | 17:50 | Y | Sat,Sun,Mon,Tue,Wed,Thu | Y | সন্ধ্যার কোডিং ও প্রজেক্ট কাজ। | 15 | 0 | Y | work
রাতের খাবার ও পরিবার | 50 | 20:15 | N | Sat,Sun,Mon,Tue,Wed,Thu | Y | রাতের খাবার ও পারিবারিক সময়। | 10 | 5 | N | family
দিনের পর্যালোচনা ও পরিকল্পনা | 45 | 21:10 | N | Sat,Sun,Mon,Tue,Wed,Thu | Y | সারাদিনের পর্যালোচনা। | 10 | 5 | N | goals
ঘুমের প্রস্তুতি | 60 | 22:00 | N | Sat,Sun,Mon,Tue,Wed,Thu | Y | স্ক্রিন বন্ধ করে ঘুমের প্রস্তুতি। | 15 | 0 | N | sleep

# 3. FRIDAY SPECIAL ROUTINE
ঘুম থেকে ওঠা | 25 | 05:00 | N | Fri | Y | ঘুম থেকে ওঠার সময়। | 5 | 0 | N | general
ফজর প্রস্তুতি | 15 | 05:25 | N | Fri | Y | ফজরের প্রস্তুতি নিন। | 5 | 0 | N | general
সূরা কাহাফ তিলাওয়াত ও জিকির | 60 | 06:05 | N | Fri | Y | সূরা কাহাফ ও জিকির তিলাওয়াত। | 10 | 5 | N | reading
সকালের নাস্তা ও বিশ্রাম | 60 | 07:15 | N | Fri | Y | সকালের নাস্তা ও বিশ্রাম। | 15 | 5 | N | eating
ব্যক্তিগত প্রজেক্ট ও গবেষণা | 120 | 08:30 | Y | Fri | Y | ব্যক্তিগত প্রজেক্ট ও গবেষণা কাজ। | 15 | 5 | Y | studying
জুমার প্রস্তুতি ও গোসল | 60 | 11:15 | N | Fri | Y | জুমার সালাতের প্রস্তুতি ও গোসল। | 10 | 5 | N | general
জুমার দুপুরের খাবার ও বিশ্রাম | 60 | 14:35 | N | Fri | Y | জুমার দুপুরের খাবার ও বিশ্রাম। | 10 | 5 | N | eating
পারিবারিক আলোচনা ও বই পড়া | 35 | 15:40 | N | Fri | Y | পারিবারিক আলোচনা ও বই পড়া। | 5 | 0 | N | family
সাপ্তাহিক পর্যালোচনা ও পরিকল্পনা | 115 | 17:50 | N | Fri | Y | সাপ্তাহিক পর্যালোচনা ও পরিকল্পনা। | 15 | 0 | Y | goals
রাতের খাবার ও আড্ডা | 50 | 20:15 | N | Fri | Y | রাতের খাবার ও পারিবারিক সময়। | 10 | 5 | N | family
পরবর্তী সপ্তাহের প্রস্তুতি ও বই | 50 | 21:10 | N | Fri | Y | বই পড়া ও প্রস্তুতি। | 10 | 5 | N | reading
ঘুমের প্রস্তুতি | 55 | 22:05 | N | Fri | Y | ঘুমের প্রস্তুতি নিন। | 10 | 0 | N | sleep
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
      val directive = rawTokens[0].uppercase()

      // -------------------------------------------------------------
      // DIRECTIVE 1: ANCHOR
      // Syntax: ANCHOR | Slot(1-7) | Title | Start(HH:mm) | Duration(m) | DND(Y/N) | DndDelay(0-25) | Voice(Y/N) | Message
      // Optional Friday overrides for Slot 3: | FriTitle | FriStart | FriDuration | FriMessage
      // -------------------------------------------------------------
      if (directive == "ANCHOR") {
        if (rawTokens.size < 5) {
          diagnostics.add(
            ConsoleLineDiagnostic(
              lineNumber = lineNum,
              rawLine = rawLine,
              isValid = false,
              errorMessage = "ANCHOR requires at least: ANCHOR | Slot(1-7) | Title | Start(HH:mm) | Duration(m)"
            )
          )
          continue
        }

        val slot = rawTokens[1].toIntOrNull()
        if (slot == null || slot !in 1..7) {
          diagnostics.add(
            ConsoleLineDiagnostic(
              lineNumber = lineNum,
              rawLine = rawLine,
              isValid = false,
              errorMessage = "Invalid slot number '${rawTokens[1]}'. Must be 1 to 7."
            )
          )
          continue
        }

        val title = rawTokens[2]
        if (title.isBlank()) {
          diagnostics.add(
            ConsoleLineDiagnostic(
              lineNumber = lineNum,
              rawLine = rawLine,
              isValid = false,
              errorMessage = "Anchor title cannot be empty."
            )
          )
          continue
        }

        val startMinutes = RoutineTask.parseTime24(rawTokens[3])
        if (startMinutes == null && slot != 1 && slot != 5) {
          diagnostics.add(
            ConsoleLineDiagnostic(
              lineNumber = lineNum,
              rawLine = rawLine,
              isValid = false,
              errorMessage = "Invalid start time '${rawTokens[3]}'. Expected HH:mm (e.g. 05:00)."
            )
          )
          continue
        }

        val duration = rawTokens[4].toIntOrNull()
        if ((duration == null || duration <= 0) && slot != 5) {
          diagnostics.add(
            ConsoleLineDiagnostic(
              lineNumber = lineNum,
              rawLine = rawLine,
              isValid = false,
              errorMessage = "Invalid duration '${rawTokens[4]}'. Must be positive minutes."
            )
          )
          continue
        }

        val dndToken = rawTokens.getOrNull(5)?.uppercase() ?: "N"
        val enableDnd = dndToken in setOf("Y", "YES", "TRUE", "1")

        // Disambiguate token 6: can be DndDelay (0..25) or Voice (Y/N)
        var dndDelayMinutes = 0
        var isVoiceEnabled = true
        var message = "$title starting now."
        var nextIdx = 6

        if (rawTokens.size > 6) {
          val tok6 = rawTokens[6]
          val parsedDelay = tok6.toIntOrNull()
          if (parsedDelay != null) {
            dndDelayMinutes = parsedDelay.coerceIn(0, 25)
            nextIdx = 7
            if (rawTokens.size > nextIdx) {
              val voiceTok = rawTokens[nextIdx].uppercase()
              isVoiceEnabled = voiceTok in setOf("Y", "YES", "TRUE", "1")
              nextIdx++
            }
            if (rawTokens.size > nextIdx) {
              message = rawTokens[nextIdx].ifBlank { "$title starting now." }
              nextIdx++
            }
          } else {
            // Backward-compatible: token 6 is voice
            val voiceTok = tok6.uppercase()
            isVoiceEnabled = voiceTok in setOf("Y", "YES", "TRUE", "1")
            nextIdx = 7
            if (rawTokens.size > nextIdx) {
              message = rawTokens[nextIdx].ifBlank { "$title starting now." }
              nextIdx++
            }
          }
        }

        // Optional Friday overrides for Slot 3 (detect if token at nextIdx+1 is a valid HH:mm time)
        var friTitle: String? = null
        var friStart: Int? = null
        var friDur: Int? = null
        var friVoice: String? = null

        if (slot == 3 && rawTokens.size > nextIdx) {
          val maybeFriStart = rawTokens.getOrNull(nextIdx + 1)?.let { RoutineTask.parseTime24(it) }
          if (maybeFriStart != null) {
            friTitle = rawTokens.getOrNull(nextIdx)?.takeIf { it.isNotBlank() }
            friStart = maybeFriStart
            friDur = rawTokens.getOrNull(nextIdx + 2)?.toIntOrNull()
            friVoice = rawTokens.getOrNull(nextIdx + 3)?.takeIf { it.isNotBlank() }
            nextIdx += 4
          }
        }

        // Optional trailing fields for all anchors: Days | Sound(Y/N) | VoiceVol(0-100)
        var selectedDays = RoutineDay.allDays()
        var isSoundAlertEnabled = true
        var voiceVolume = 1.0f

        if (rawTokens.size > nextIdx) {
          val daysTok = rawTokens[nextIdx].trim()
          if (daysTok.isNotBlank()) {
            val parsedDays = parseDaysToken(daysTok)
            if (parsedDays.isEmpty()) {
              diagnostics.add(
                ConsoleLineDiagnostic(
                  lineNumber = lineNum,
                  rawLine = rawLine,
                  isValid = false,
                  errorMessage = "No valid days in '$daysTok'. Use Sat,Sun,Mon,Tue,Wed,Thu,Fri or ALL"
                )
              )
              continue
            }
            selectedDays = parsedDays
          }
          nextIdx++
        }

        if (rawTokens.size > nextIdx) {
          val soundTok = rawTokens[nextIdx].uppercase()
          if (soundTok.isNotBlank()) {
            isSoundAlertEnabled = soundTok in setOf("Y", "YES", "TRUE", "1")
          }
          nextIdx++
        }

        if (rawTokens.size > nextIdx) {
          val volTok = rawTokens[nextIdx].trim()
          if (volTok.isNotBlank()) {
            val parsedVol = volTok.toIntOrNull()
            if (parsedVol != null) {
              voiceVolume = parsedVol.coerceIn(0, 100) / 100f
            }
          }
        }

        val anchorType = when (slot) {
          1 -> TaskType.ANCHOR_SLEEP
          5 -> TaskType.ANCHOR_ELASTIC
          else -> TaskType.ANCHOR_STANDARD
        }

        val anchorTask = RoutineTask(
          id = "anchor_slot_$slot",
          title = title,
          type = anchorType,
          slotNumber = slot,
          startTimeMinute = startMinutes ?: 0,
          durationMinutes = duration ?: 60,
          voiceMessage = message,
          enableDnd = enableDnd,
          dndDelayMinutes = if (slot == 1 || slot == 5) 0 else dndDelayMinutes,
          isVoiceEnabled = isVoiceEnabled,
          voiceVolume = voiceVolume,
          isSoundAlertEnabled = isSoundAlertEnabled,
          selectedDays = selectedDays,
          fridayTitle = friTitle,
          fridayStartTimeMinute = friStart,
          fridayDurationMinutes = friDur,
          fridayVoiceMessage = friVoice
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

      // -------------------------------------------------------------
      // DIRECTIVE 2: TWIN
      // Syntax: TWIN | TotalPool(m) | P1Title | P1Start(HH:mm) | P1Dur(m) | P1Voice | P2Title | P2Start(HH:mm) | P2Dur(m) | P2Voice | DND(Y/N) | Days | Flex(0-30) | Rest(0-30) | Category | P1Safety | P2Safety | P1VoiceOn(Y/N) | P2VoiceOn(Y/N) | P1Vol(0-100) | P2Vol(0-100) | Sound(Y/N)
      // -------------------------------------------------------------
      if (directive == "TWIN") {
        if (rawTokens.size < 10) {
          diagnostics.add(
            ConsoleLineDiagnostic(
              lineNumber = lineNum,
              rawLine = rawLine,
              isValid = false,
              errorMessage = "TWIN requires: TWIN | TotalPool | P1Title | P1Start | P1Dur | P1Voice | P2Title | P2Start | P2Dur | P2Voice"
            )
          )
          continue
        }

        val requestedPool = rawTokens[1].toIntOrNull() ?: 180
        val p1Title = rawTokens[2].ifBlank { "Session 1" }
        val p1Start = RoutineTask.parseTime24(rawTokens[3])
        if (p1Start == null) {
          diagnostics.add(
            ConsoleLineDiagnostic(
              lineNumber = lineNum,
              rawLine = rawLine,
              isValid = false,
              errorMessage = "Invalid Part 1 start time '${rawTokens[3]}'. Expected HH:mm."
            )
          )
          continue
        }

        val p1Dur = rawTokens[4].toIntOrNull()
        if (p1Dur == null || p1Dur < 1) {
          diagnostics.add(
            ConsoleLineDiagnostic(
              lineNumber = lineNum,
              rawLine = rawLine,
              isValid = false,
              errorMessage = "Invalid Part 1 duration '${rawTokens[4]}'. Minimum 1m."
            )
          )
          continue
        }

        val p1Voice = rawTokens[5].ifBlank { "$p1Title starting now." }

        val p2Title = rawTokens[6].ifBlank { "Session 2" }
        val p2Start = RoutineTask.parseTime24(rawTokens[7])
        if (p2Start == null) {
          diagnostics.add(
            ConsoleLineDiagnostic(
              lineNumber = lineNum,
              rawLine = rawLine,
              isValid = false,
              errorMessage = "Invalid Part 2 start time '${rawTokens[7]}'. Expected HH:mm."
            )
          )
          continue
        }

        val p2Dur = rawTokens[8].toIntOrNull()
        if (p2Dur == null || p2Dur < 1) {
          diagnostics.add(
            ConsoleLineDiagnostic(
              lineNumber = lineNum,
              rawLine = rawLine,
              isValid = false,
              errorMessage = "Invalid Part 2 duration '${rawTokens[8]}'. Minimum 1m."
            )
          )
          continue
        }

        val p2Voice = rawTokens[9].ifBlank { "$p2Title starting now." }

        val dndToken = rawTokens.getOrNull(10)?.uppercase() ?: "N"
        val enableDnd = dndToken in setOf("Y", "YES", "TRUE", "1")

        val daysToken = rawTokens.getOrNull(11)?.takeIf { it.isNotBlank() } ?: "ALL"
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

        val flex = rawTokens.getOrNull(12)?.toIntOrNull()?.coerceIn(0, 30) ?: 0
        val rest = rawTokens.getOrNull(13)?.toIntOrNull()?.coerceIn(0, 30) ?: 0
        val category = rawTokens.getOrNull(14)?.trim()?.lowercase()?.ifBlank { null } ?: "general"

        val p1SafetyTok = rawTokens.getOrNull(15)?.trim()
        val p1Safety = if (p1SafetyTok.isNullOrEmpty()) {
          0
        } else {
          val parsed = p1SafetyTok.toIntOrNull()
          if (parsed == null || parsed < 0 || parsed > p1Dur) {
            diagnostics.add(
              ConsoleLineDiagnostic(
                lineNumber = lineNum,
                rawLine = rawLine,
                isValid = false,
                errorMessage = "Part 1 safety floor '$p1SafetyTok' cannot exceed Part 1 duration (${p1Dur}m)."
              )
            )
            continue
          }
          parsed
        }

        val p2SafetyTok = rawTokens.getOrNull(16)?.trim()
        val p2Safety = if (p2SafetyTok.isNullOrEmpty()) {
          0
        } else {
          val parsed = p2SafetyTok.toIntOrNull()
          if (parsed == null || parsed < 0 || parsed > p2Dur) {
            diagnostics.add(
              ConsoleLineDiagnostic(
                lineNumber = lineNum,
                rawLine = rawLine,
                isValid = false,
                errorMessage = "Part 2 safety floor '$p2SafetyTok' cannot exceed Part 2 duration (${p2Dur}m)."
              )
            )
            continue
          }
          parsed
        }

        val p1VoiceEnabled = rawTokens.getOrNull(17)?.takeIf { it.isNotBlank() }?.uppercase()?.let {
          it in setOf("Y", "YES", "TRUE", "1")
        } ?: true
        val p2VoiceEnabled = rawTokens.getOrNull(18)?.takeIf { it.isNotBlank() }?.uppercase()?.let {
          it in setOf("Y", "YES", "TRUE", "1")
        } ?: true
        val p1Vol = rawTokens.getOrNull(19)?.toIntOrNull()?.coerceIn(0, 100)?.div(100f) ?: 1.0f
        val p2Vol = rawTokens.getOrNull(20)?.toIntOrNull()?.coerceIn(0, 100)?.div(100f) ?: 1.0f
        val isSoundEnabled = rawTokens.getOrNull(21)?.takeIf { it.isNotBlank() }?.uppercase()?.let {
          it in setOf("Y", "YES", "TRUE", "1")
        } ?: true

        val totalPool = requestedPool.coerceAtLeast((p1Dur + p2Dur).coerceAtLeast(2))
        val twinId = "fluid_${UUID.randomUUID().toString().take(8)}"

        val part1 = RoutineTask(
          id = "${twinId}_p1",
          title = p1Title,
          type = TaskType.DYNAMIC,
          startTimeMinute = p1Start,
          durationMinutes = p1Dur,
          voiceMessage = p1Voice,
          enableDnd = enableDnd,
          enableHaptic = true,
          allowSplitting = false,
          selectedDays = parsedDays,
          isVoiceEnabled = p1VoiceEnabled,
          voiceVolume = p1Vol,
          isSoundAlertEnabled = isSoundEnabled,
          customNotificationTitle = p1Title,
          customNotificationBody = p1Voice,
          isFluidTwin = true,
          fluidTwinId = twinId,
          fluidTwinPart = 1,
          fluidTotalPoolMinutes = totalPool,
          fluidSafetyFloorMinutes = p1Safety,
          toleranceMinutes = flex,
          customFlexibilityMinutes = flex,
          interTaskBufferMinutes = rest,
          category = category
        )

        val part2 = RoutineTask(
          id = "${twinId}_p2",
          title = p2Title,
          type = TaskType.DYNAMIC,
          startTimeMinute = p2Start,
          durationMinutes = p2Dur,
          voiceMessage = p2Voice,
          enableDnd = enableDnd,
          enableHaptic = true,
          allowSplitting = false,
          selectedDays = parsedDays,
          isVoiceEnabled = p2VoiceEnabled,
          voiceVolume = p2Vol,
          isSoundAlertEnabled = isSoundEnabled,
          customNotificationTitle = p2Title,
          customNotificationBody = p2Voice,
          isFluidTwin = true,
          fluidTwinId = twinId,
          fluidTwinPart = 2,
          fluidTotalPoolMinutes = totalPool,
          fluidSafetyFloorMinutes = p2Safety,
          toleranceMinutes = flex,
          customFlexibilityMinutes = flex,
          interTaskBufferMinutes = rest,
          category = category
        )

        validTasks.add(part1)
        validTasks.add(part2)
        diagnostics.add(
          ConsoleLineDiagnostic(
            lineNumber = lineNum,
            rawLine = rawLine,
            isValid = true,
            parsedTask = part1
          )
        )
        continue
      }

      // -------------------------------------------------------------
      // DIRECTIVE 3: DYNAMIC (or default without prefix)
      // Syntax: Name | Duration(m) | Start(HH:mm) | DND(Y/N) | Days | Voice(Y/N) | Message | Flex(0-30) | Rest(0-30) | Priority(Y/N) | Category
      // -------------------------------------------------------------
      val tokens = if (directive == "DYNAMIC") {
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
      if (duration == null || duration < 0) {
        diagnostics.add(
          ConsoleLineDiagnostic(
            lineNumber = lineNum,
            rawLine = rawLine,
            isValid = false,
            errorMessage = "Invalid duration '${tokens[1]}'. Must be 0 or positive integer in minutes."
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
      val enableDnd = dndToken in setOf("Y", "YES", "TRUE", "1")

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
        isVoiceEnabled = voiceToken in setOf("Y", "YES", "TRUE", "1")
        customMessage = tokens[6].ifBlank { "$name starting now." }
      } else if (tokens.size == 6) {
        val t5Upper = tokens[5].uppercase()
        if (t5Upper in setOf("Y", "N", "YES", "NO", "TRUE", "FALSE", "1", "0")) {
          isVoiceEnabled = t5Upper in setOf("Y", "YES", "TRUE", "1")
        } else {
          isVoiceEnabled = true
          customMessage = tokens[5].ifBlank { "$name starting now." }
        }
      }

      // Flexibility (toleranceMinutes) & Rest Buffer (interTaskBufferMinutes) OR Exported Twin Tokens
      var tolerance = 0
      var restBuffer = 0
      var isFluidTwin = false
      var fluidPart: Int? = null
      var fluidPool: Int? = null
      var ringtoneUri: String? = null
      var ringtoneTitle: String? = null

      if (tokens.size > 7) {
        val t7 = tokens[7]
        if (t7.equals("Y", ignoreCase = true)) {
          // Exported Fluid Twin format
          isFluidTwin = true
          fluidPart = tokens.getOrNull(8)?.toIntOrNull()
          fluidPool = tokens.getOrNull(9)?.toIntOrNull()
          ringtoneUri = tokens.getOrNull(10)?.takeIf { it != "DEFAULT" && it.isNotBlank() }
          ringtoneTitle = tokens.getOrNull(11)?.takeIf { it != "Default" && it.isNotBlank() }
        } else {
          // Regular dynamic: Flexibility & RestBuffer
          tolerance = t7.toIntOrNull()?.coerceIn(0, 30) ?: 0
          if (tokens.size > 8) {
            restBuffer = tokens[8].toIntOrNull()?.coerceIn(0, 30) ?: 0
          }
        }
      }

      var isHighPriority = false
      var parsedCategory = "general"
      var parsedVoiceVolume = 1.0f
      var parsedSoundEnabled = true
      if (!isFluidTwin) {
        if (tokens.size > 9) {
          val prioToken = tokens[9].uppercase()
          isHighPriority = prioToken in setOf("Y", "YES", "TRUE", "1")
        }
        if (tokens.size > 10) {
          parsedCategory = tokens[10].trim().lowercase().ifBlank { "general" }
        }
        if (tokens.size > 11) {
          val volInt = tokens[11].trim().toIntOrNull()
          if (volInt != null) {
            parsedVoiceVolume = volInt.coerceIn(0, 100) / 100f
          }
        }
        if (tokens.size > 12) {
          val soundTok = tokens[12].trim().uppercase()
          if (soundTok.isNotBlank()) {
            parsedSoundEnabled = soundTok in setOf("Y", "YES", "TRUE", "1")
          }
        }
      }

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
        isHighPriority = isHighPriority,
        selectedDays = parsedDays,
        isVoiceEnabled = isVoiceEnabled,
        voiceVolume = parsedVoiceVolume,
        isSoundAlertEnabled = parsedSoundEnabled,
        customNotificationTitle = name,
        customNotificationBody = customMessage,
        isFluidTwin = isFluidTwin,
        fluidTwinId = if (isFluidTwin) "twin_${name.replace(" ", "_")}" else null,
        fluidTwinPart = fluidPart,
        fluidTotalPoolMinutes = fluidPool,
        customRingtoneUri = ringtoneUri,
        customRingtoneTitle = ringtoneTitle,
        toleranceMinutes = tolerance,
        customFlexibilityMinutes = tolerance,
        interTaskBufferMinutes = restBuffer,
        category = parsedCategory
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

  /**
   * Evaluates the directive and pipe '|' delimiters in the active typing line
   * and generates a context-aware inline syntax ghost prediction recommendation.
   */
  fun getSyntaxPrediction(currentLine: String): String? {
    val trimmed = currentLine.trim()
    if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) {
      return null
    }

    val upper = trimmed.uppercase()
    val pipeCount = currentLine.count { it == '|' }
    val endsWithPipe = currentLine.trimEnd().endsWith("|")

    // ANCHOR branch
    if (upper.startsWith("ANCHOR")) {
      return when (pipeCount) {
        0 -> " | 2 | Morning Routine | 05:00 | 30 | Y | 0 | Y | Morning time"
        1 -> if (endsWithPipe) " 2 | Morning Routine | 05:00 | 30 | Y | 0 | Y | Morning time" else " | Morning Routine | 05:00 | 30 | Y | 0 | Y | Morning time"
        2 -> if (endsWithPipe) " Morning Routine | 05:00 | 30 | Y | 0 | Y | Morning time" else " | 05:00 | 30 | Y | 0 | Y | Morning time"
        3 -> if (endsWithPipe) " 05:00 | 30 | Y | 0 | Y | Morning time" else " | 30 | Y | 0 | Y | Morning time"
        4 -> if (endsWithPipe) " 30 | Y | 0 | Y | Morning time" else " | Y | 0 | Y | Morning time"
        5 -> if (endsWithPipe) " Y | 0 | Y | Morning time" else " | 0 | Y | Morning time"
        6 -> if (endsWithPipe) " 0 | Y | Morning time" else " | Y | Morning time"
        7 -> if (endsWithPipe) " Y | Morning time" else " | Morning time"
        8 -> if (endsWithPipe) " Morning time starting now" else null
        else -> null
      }
    }

    // TWIN branch
    if (upper.startsWith("TWIN")) {
      return when (pipeCount) {
        0 -> " | 180 | Morning Focus | 09:00 | 90 | Sprint 1 | Review | 14:00 | 90 | Sprint 2 | Y | ALL"
        1 -> if (endsWithPipe) " 180 | Morning Focus | 09:00 | 90 | Sprint 1 | Review | 14:00 | 90 | Sprint 2 | Y | ALL" else " | Morning Focus | 09:00 | 90 | Sprint 1 | Review | 14:00 | 90 | Sprint 2 | Y | ALL"
        2 -> if (endsWithPipe) " Morning Focus | 09:00 | 90 | Sprint 1 | Review | 14:00 | 90 | Sprint 2 | Y | ALL" else " | 09:00 | 90 | Sprint 1 | Review | 14:00 | 90 | Sprint 2 | Y | ALL"
        3 -> if (endsWithPipe) " 09:00 | 90 | Sprint 1 | Review | 14:00 | 90 | Sprint 2 | Y | ALL" else " | 90 | Sprint 1 | Review | 14:00 | 90 | Sprint 2 | Y | ALL"
        4 -> if (endsWithPipe) " 90 | Sprint 1 | Review | 14:00 | 90 | Sprint 2 | Y | ALL" else " | Sprint 1 | Review | 14:00 | 90 | Sprint 2 | Y | ALL"
        5 -> if (endsWithPipe) " Sprint 1 | Review | 14:00 | 90 | Sprint 2 | Y | ALL" else " | Review | 14:00 | 90 | Sprint 2 | Y | ALL"
        6 -> if (endsWithPipe) " Review | 14:00 | 90 | Sprint 2 | Y | ALL" else " | 14:00 | 90 | Sprint 2 | Y | ALL"
        7 -> if (endsWithPipe) " 14:00 | 90 | Sprint 2 | Y | ALL" else " | 90 | Sprint 2 | Y | ALL"
        8 -> if (endsWithPipe) " 90 | Sprint 2 | Y | ALL" else " | Sprint 2 | Y | ALL"
        9 -> if (endsWithPipe) " Sprint 2 | Y | ALL" else " | Y | ALL"
        10 -> if (endsWithPipe) " Y | ALL" else " | ALL"
        11 -> if (endsWithPipe) " ALL" else null
        else -> null
      }
    }

    // Standard DYNAMIC branch
    val endsWithSpace = currentLine.endsWith(" ")
    return when (pipeCount) {
      0 -> " | 45 | 10:00 | N | ALL | Y | Focus session | 15 | 5 | N | work"
      1 -> if (endsWithPipe) " 45 | 10:00 | N | ALL | Y | Focus session | 15 | 5 | N | work"
           else if (endsWithSpace) "| 10:00 | N | ALL | Y | Focus session | 15 | 5 | N | work"
           else " | 10:00 | N | ALL | Y | Focus session | 15 | 5 | N | work"
      2 -> if (endsWithPipe) " 10:00 | N | ALL | Y | Focus session | 15 | 5 | N | work"
           else if (endsWithSpace) "| N | ALL | Y | Focus session | 15 | 5 | N | work"
           else " | N | ALL | Y | Focus session | 15 | 5 | N | work"
      3 -> if (endsWithPipe) " N | ALL | Y | Focus session | 15 | 5 | N | work"
           else if (endsWithSpace) "| ALL | Y | Focus session | 15 | 5 | N | work"
           else " | ALL | Y | Focus session | 15 | 5 | N | work"
      4 -> if (endsWithPipe) " ALL | Y | Focus session | 15 | 5 | N | work"
           else if (endsWithSpace) "| Y | Focus session | 15 | 5 | N | work"
           else " | Y | Focus session | 15 | 5 | N | work"
      5 -> if (endsWithPipe) " Y | Focus session | 15 | 5 | N | work"
           else if (endsWithSpace) "| Focus session | 15 | 5 | N | work"
           else " | Focus session | 15 | 5 | N | work"
      6 -> if (endsWithPipe) " Focus session | 15 | 5 | N | work"
           else if (endsWithSpace) "| 15 | 5 | N | work"
           else " | 15 | 5 | N | work"
      7 -> if (endsWithPipe) " 15 | 5 | N | work"
           else if (endsWithSpace) "| 5 | N | work"
           else " | 5 | N | work"
      8 -> if (endsWithPipe) " 5 | N | work"
           else if (endsWithSpace) "| N | work"
           else " | N | work"
      9 -> if (endsWithPipe) " N | work"
           else if (endsWithSpace) "| work"
           else " | work"
      10 -> if (endsWithPipe) " work" else null
      else -> null
    }
  }

  fun generateRoutineScript(
    anchors: List<RoutineTask>,
    dynamicTasks: List<RoutineTask>,
    sleepMinutes: Int,
    bufferMinutes: Int
  ): String {
    val sb = StringBuilder()
    sb.appendLine("# ==============================================================")
    sb.appendLine("# CHRONOS LIVE BACKUP ROUTINE SCRIPT")
    sb.appendLine("# ==============================================================")
    sb.appendLine("# SLEEP_MINUTES=$sleepMinutes")
    sb.appendLine("# BUFFER_MINUTES=$bufferMinutes")
    sb.appendLine()
    sb.appendLine("# 1. THE 7 CIRCADIAN ANCHORS (Daily Fixed Boundaries)")

    val sortedAnchors = anchors.sortedBy { it.slotNumber ?: 0 }
    for (anchor in sortedAnchors) {
      val slot = anchor.slotNumber ?: continue
      val title = anchor.title
      val timeStr = RoutineTask.formatTime24(anchor.startTimeMinute)
      val duration = anchor.durationMinutes
      val dndStr = if (anchor.enableDnd) "Y" else "N"
      val delay = anchor.dndDelayMinutes
      val voiceStr = if (anchor.isVoiceEnabled) "Y" else "N"
      val voiceMsg = anchor.voiceMessage.ifBlank { "$title starting now." }
      val daysStr = formatDaysToken(anchor.selectedDays)
      val soundStr = if (anchor.isSoundAlertEnabled) "Y" else "N"
      val volPct = (anchor.voiceVolume.coerceIn(0f, 1f) * 100).toInt()

      var line = "ANCHOR | $slot | $title | $timeStr | $duration | $dndStr | $delay | $voiceStr | $voiceMsg"

      if (slot == 3 && (anchor.fridayTitle != null || anchor.fridayStartTimeMinute != null || anchor.fridayDurationMinutes != null || anchor.fridayVoiceMessage != null)) {
        val friTitle = anchor.fridayTitle?.ifBlank { null } ?: "Jumu'ah"
        val friStart = RoutineTask.formatTime24(anchor.fridayStartTimeMinute ?: 750)
        val friDur = anchor.fridayDurationMinutes ?: 120
        val friVoice = anchor.fridayVoiceMessage?.ifBlank { null } ?: "জুমার সময় হয়েছে।"
        line += " | $friTitle | $friStart | $friDur | $friVoice"
      }
      line += " | $daysStr | $soundStr | $volPct"
      sb.appendLine(line)
    }

    val activeDynamics = dynamicTasks.filter { !it.isDeleted }
    if (activeDynamics.isNotEmpty()) {
      sb.appendLine()
      sb.appendLine("# 2. DYNAMIC & FLUID TWIN ROUTINE TASKS")
      val exportedTwinIds = mutableSetOf<String>()

      for (task in activeDynamics) {
        if (task.isFluidTwin && !task.fluidTwinId.isNullOrBlank()) {
          val twinId = task.fluidTwinId
          if (twinId in exportedTwinIds) continue
          val pair = activeDynamics.filter { it.fluidTwinId == twinId }
          val p1 = pair.firstOrNull { it.fluidTwinPart == 1 } ?: pair.getOrNull(0)
          val p2 = pair.firstOrNull { it.fluidTwinPart == 2 && it.id != p1?.id } ?: pair.firstOrNull { it.id != p1?.id }
          if (p1 != null && p2 != null) {
            exportedTwinIds.add(twinId)
            val pool = p1.fluidTotalPoolMinutes ?: (p1.durationMinutes + p2.durationMinutes)
            val p1Start = RoutineTask.formatTime24(p1.startTimeMinute)
            val p2Start = RoutineTask.formatTime24(p2.startTimeMinute)
            val p1VoiceMsg = p1.voiceMessage.ifBlank { "${p1.title} starting now." }
            val p2VoiceMsg = p2.voiceMessage.ifBlank { "${p2.title} starting now." }
            val dndStr = if (p1.enableDnd) "Y" else "N"
            val daysStr = formatDaysToken(p1.selectedDays)
            val flex = maxOf(p1.toleranceMinutes, p1.customFlexibilityMinutes).coerceIn(0, 30)
            val rest = p1.interTaskBufferMinutes.coerceIn(0, 30)
            val cat = p1.category?.trim()?.lowercase()?.ifBlank { null } ?: "general"
            val p1Safety = p1.fluidSafetyFloorMinutes.coerceIn(0, p1.durationMinutes)
            val p2Safety = p2.fluidSafetyFloorMinutes.coerceIn(0, p2.durationMinutes)
            val p1VoiceStr = if (p1.isVoiceEnabled) "Y" else "N"
            val p2VoiceStr = if (p2.isVoiceEnabled) "Y" else "N"
            val p1VolPct = (p1.voiceVolume.coerceIn(0f, 1f) * 100).toInt()
            val p2VolPct = (p2.voiceVolume.coerceIn(0f, 1f) * 100).toInt()
            val soundStr = if (p1.isSoundAlertEnabled) "Y" else "N"

            sb.appendLine(
              "TWIN | $pool | ${p1.title} | $p1Start | ${p1.durationMinutes} | $p1VoiceMsg | " +
                "${p2.title} | $p2Start | ${p2.durationMinutes} | $p2VoiceMsg | " +
                "$dndStr | $daysStr | $flex | $rest | $cat | $p1Safety | $p2Safety | $p1VoiceStr | $p2VoiceStr | $p1VolPct | $p2VolPct | $soundStr"
            )
            continue
          }
        }

        val title = task.title
        val duration = task.durationMinutes
        val timeStr = RoutineTask.formatTime24(task.startTimeMinute)
        val dndStr = if (task.enableDnd) "Y" else "N"
        val daysStr = formatDaysToken(task.selectedDays)
        val voiceStr = if (task.isVoiceEnabled) "Y" else "N"
        val voiceMsg = task.voiceMessage.ifBlank { "$title starting now." }
        val flex = task.toleranceMinutes.coerceAtLeast(task.customFlexibilityMinutes)
        val rest = task.interTaskBufferMinutes
        val prioStr = if (task.isHighPriority) "Y" else "N"
        val cat = task.category?.trim()?.lowercase()?.ifBlank { null } ?: "general"
        val volPct = (task.voiceVolume.coerceIn(0f, 1f) * 100).toInt()
        val soundStr = if (task.isSoundAlertEnabled) "Y" else "N"

        sb.appendLine("$title | $duration | $timeStr | $dndStr | $daysStr | $voiceStr | $voiceMsg | $flex | $rest | $prioStr | $cat | $volPct | $soundStr")
      }
    }

    return sb.toString().trimEnd()
  }

  private fun formatDaysToken(days: Set<RoutineDay>): String {
    return if (days.size == 7) {
      "All"
    } else {
      days.sortedBy { it.ordinal }.joinToString(",") { it.shortName }
    }
  }
}
