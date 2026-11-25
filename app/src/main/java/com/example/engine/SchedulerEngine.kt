package com.example.engine

import androidx.compose.runtime.Immutable
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.ScheduleGap
import com.example.model.TaskType

@Immutable
data class ScheduleResult(
  val scheduledTasks: List<RoutineTask>,
  val overflowTasks: List<RoutineTask>,
  val availableGaps: List<ScheduleGap>,
  val totalScheduledMinutes: Int,
  val totalGapsMinutes: Int
)

@Immutable
data class ScheduleImpactItem(
  val taskId: String,
  val title: String,
  val oldStartMinute: Int?,
  val newStartMinute: Int?,
  val oldDurationMinutes: Int,
  val newDurationMinutes: Int,
  val statusChange: String
)

sealed interface VerticalDropTarget {
  data class InsertBeforeTask(val targetTaskId: String) : VerticalDropTarget
  data class InsertAfterTask(val targetTaskId: String) : VerticalDropTarget
  data class IntoGap(val targetStartMinute: Int) : VerticalDropTarget
}

@Immutable
data class VerticalDropPreview(
  val previewStartMinute: Int,
  val previewEndMinute: Int,
  val hitAnchorWall: Boolean
)

@Immutable
data class CompactionOutcome(
  val updatedDynamicTasks: List<RoutineTask>,
  val scheduleResult: ScheduleResult,
  val movedCount: Int,
  val rescuedCount: Int,
  val flexedCount: Int = 0
)

object SchedulerEngine {

  const val MIN_FLUID_TWIN_MINUTES = 45
  const val MIN_ELASTIC_BUFFER_MINUTES = 15

  fun birthDurationOf(task: RoutineTask): Int =
    (task.originalDurationMinutes ?: task.durationMinutes).coerceAtLeast(0)

  fun effectiveFlexOf(task: RoutineTask): Int =
    maxOf(task.toleranceMinutes, task.customFlexibilityMinutes).coerceIn(0, 30)

  fun minAllowedDurationOf(task: RoutineTask): Int {
    val birthDur = birthDurationOf(task)
    if (birthDur <= 0) return 0
    if (task.isFluidTwin) {
      return task.fluidSafetyFloorMinutes.coerceIn(1, birthDur)
    }
    val flex = effectiveFlexOf(task)
    if (flex <= 0) return birthDur
    val minFloor = if (birthDur >= 15) 15 else 1
    return (birthDur - flex).coerceIn(minFloor, birthDur)
  }

  private fun minDonorDurationOf(task: RoutineTask): Int {
    val birthDur = birthDurationOf(task)
    if (birthDur <= 0) return 0
    val flex = effectiveFlexOf(task)
    if (flex <= 0) return birthDur
    val minFloor = if (task.isFluidTwin) {
      task.fluidSafetyFloorMinutes.coerceIn(1, birthDur)
    } else if (birthDur >= 15) {
      15
    } else {
      1
    }
    return (birthDur - flex).coerceIn(minFloor, birthDur)
  }

  private fun availableFlexSlackOf(task: RoutineTask): Int =
    (task.durationMinutes - minDonorDurationOf(task)).coerceAtLeast(0)

  private fun distributeDeficitWaterFilling(slacks: IntArray, deficit: Int): IntArray {
    val k = slacks.size
    val cuts = IntArray(k)
    if (deficit <= 0 || k == 0) return cuts
    val totalSlack = slacks.sum()
    if (totalSlack < deficit) return cuts

    var remaining = deficit
    while (remaining > 0) {
      val activeIndices = (0 until k).filter { slacks[it] - cuts[it] > 0 }
      if (activeIndices.isEmpty()) break
      val remTotal = activeIndices.sumOf { slacks[it] - cuts[it] }
      if (remTotal <= 0) break

      var assignedThisRound = 0
      val remainders = ArrayList<Pair<Int, Long>>(activeIndices.size)
      for (idx in activeIndices) {
        val remSlack = slacks[idx] - cuts[idx]
        val num = remaining.toLong() * remSlack.toLong()
        val baseCut = (num / remTotal.toLong()).toInt().coerceAtMost(remSlack)
        val frac = num % remTotal.toLong()
        cuts[idx] += baseCut
        assignedThisRound += baseCut
        if (cuts[idx] < slacks[idx]) {
          remainders.add(idx to frac)
        }
      }
      remaining -= assignedThisRound

      if (remaining > 0 && remainders.isNotEmpty()) {
        remainders.sortWith(
          compareByDescending<Pair<Int, Long>> { it.second }
            .thenByDescending { slacks[it.first] - cuts[it.first] }
            .thenBy { it.first }
        )
        for ((idx, _) in remainders) {
          if (remaining <= 0) break
          if (cuts[idx] < slacks[idx]) {
            cuts[idx] += 1
            remaining -= 1
          }
        }
      }
    }
    return cuts
  }

  /**
   * Generates the precision default anchors for a fresh installation.
   * Slot 1: Sleep (11:00 PM = 1380m, 360m / 6h duration, Reverse Sleep Tracker)
   * Slot 2: Fajr (05:00 AM = 300m, 35m duration, 8m DND delay)
   * Slot 3: Dhuhr (01:15 PM = 795m, 35m duration, 8m DND delay; Friday Jumu'ah 12:30 PM, 120m, 5m delay)
   * Slot 4: Asr (04:30 PM = 990m, 25m duration, 10m DND delay)
   * Slot 5: Break (Auto-spans Slot 4 End to Slot 6 Start, Elastic Buffer)
   * Slot 6: Maghrib (06:00 PM = 1080m, 30m duration, 5m DND delay)
   * Slot 7: Isha (08:00 PM = 1200m, 35m duration, 10m DND delay)
   */
  fun createDefaultAnchors(sleepDurationMinutes: Int = 360, slot5Dnd: Boolean = false): List<RoutineTask> {
    val safeSleepDuration = sleepDurationMinutes.coerceIn(240, 720)
    val slot2Start = 300 // 05:00 AM
    val rawBedtime = slot2Start - safeSleepDuration
    val bedtimeMinute = ((rawBedtime % 1440) + 1440) % 1440

    val slot1 = RoutineTask(
      id = "anchor_slot_1",
      title = "Sleep",
      type = TaskType.ANCHOR_SLEEP,
      slotNumber = 1,
      startTimeMinute = bedtimeMinute,
      durationMinutes = safeSleepDuration,
      voiceMessage = "ঘুমানোর সময় হয়েছে।",
      enableDnd = true,
      dndDelayMinutes = 0
    )

    val slot2 = RoutineTask(
      id = "anchor_slot_2",
      title = "Fajr",
      type = TaskType.ANCHOR_STANDARD,
      slotNumber = 2,
      startTimeMinute = slot2Start, // 05:00 AM
      durationMinutes = 35,         // 35 mins (05:00 AM -> 05:35 AM)
      voiceMessage = "ফজরের সময় হয়েছে।",
      enableDnd = true,
      dndDelayMinutes = 8
    )

    val slot3 = RoutineTask(
      id = "anchor_slot_3",
      title = "Dhuhr",
      type = TaskType.ANCHOR_STANDARD,
      slotNumber = 3,
      startTimeMinute = 795, // 01:15 PM
      durationMinutes = 35,  // 35 mins (01:15 PM -> 01:50 PM)
      voiceMessage = "যোহরের সময় হয়েছে।",
      enableDnd = true,
      dndDelayMinutes = 8,
      fridayTitle = "Jumu'ah",
      fridayStartTimeMinute = 750, // 12:30 PM
      fridayDurationMinutes = 120, // 2 hours
      fridayVoiceMessage = "জুমার সময় হয়েছে।"
    )

    val slot4 = RoutineTask(
      id = "anchor_slot_4",
      title = "Asr",
      type = TaskType.ANCHOR_STANDARD,
      slotNumber = 4,
      startTimeMinute = 990, // 04:30 PM
      durationMinutes = 25,  // 25 mins (04:30 PM -> 04:55 PM)
      voiceMessage = "আসরের সময় হয়েছে।",
      enableDnd = true,
      dndDelayMinutes = 10
    )

    val slot6 = RoutineTask(
      id = "anchor_slot_6",
      title = "Maghrib",
      type = TaskType.ANCHOR_STANDARD,
      slotNumber = 6,
      startTimeMinute = 1080, // 06:00 PM
      durationMinutes = 30,   // 30 mins (06:00 PM -> 06:30 PM)
      voiceMessage = "মাগরিবের সময় হয়েছে।",
      enableDnd = true,
      dndDelayMinutes = 5
    )

    val slot7 = RoutineTask(
      id = "anchor_slot_7",
      title = "Isha",
      type = TaskType.ANCHOR_STANDARD,
      slotNumber = 7,
      startTimeMinute = 1200, // 08:00 PM
      durationMinutes = 35,   // 35 mins (08:00 PM -> 08:35 PM)
      voiceMessage = "এশার সময় হয়েছে।",
      enableDnd = true,
      dndDelayMinutes = 10
    )

    // Slot 5: Spans between Slot 4 End and Slot 6 Start
    val slot4End = slot4.startTimeMinute + slot4.durationMinutes
    val elasticDuration = (slot6.startTimeMinute - slot4End).coerceAtLeast(MIN_ELASTIC_BUFFER_MINUTES)
    val slot5 = RoutineTask(
      id = "anchor_slot_5",
      title = "Break",
      type = TaskType.ANCHOR_ELASTIC,
      slotNumber = 5,
      startTimeMinute = slot4End,
      durationMinutes = elasticDuration,
      voiceMessage = "বিরতির সময় হয়েছে।",
      enableDnd = slot5Dnd,
      dndDelayMinutes = 0,
      selectedDays = RoutineDay.allDays()
    )

    return listOf(slot1, slot2, slot3, slot4, slot5, slot6, slot7)
  }

  /**
   * Recalculates Slot 1 (Reverse Sleep) and Slot 5 (Elastic Buffer) based on
   * Anchor 2, Anchor 4, and Anchor 6 with complete midnight rollover safety.
   */
  fun calculateAnchors(
    anchors: List<RoutineTask>,
    sleepDurationMinutes: Int = 360 // 6 hours default
  ): List<RoutineTask> {
    if (anchors.isEmpty()) {
      return createDefaultAnchors(sleepDurationMinutes)
    }

    val map = anchors.associateBy { it.slotNumber ?: 0 }.toMutableMap()

    // 1. Slot 2: Wake-up Anchor (Default 05:00 AM = 300 min)
    val slot2 = map[2] ?: RoutineTask(
      id = "anchor_slot_2",
      title = "Fajr",
      type = TaskType.ANCHOR_STANDARD,
      slotNumber = 2,
      startTimeMinute = 300, // 05:00 AM
      durationMinutes = 35,  // 35 mins
      voiceMessage = "ফজরের সময় হয়েছে।",
      enableDnd = true,
      dndDelayMinutes = 8
    )
    map[2] = slot2

    // 2. Slot 1: Reverse Sleep Tracker
    // Bedtime = [Slot 2 Start Time] - [Sleep Duration]
    // Normalized to 0..1439 with full midnight-crossing rollback safety
    val safeSleepDuration = sleepDurationMinutes.coerceIn(240, 720) // 4h to 12h
    val rawBedtime = slot2.startTimeMinute - safeSleepDuration
    val bedtimeMinute = ((rawBedtime % 1440) + 1440) % 1440

    val existingSlot1 = map[1]
    val slot1 = (existingSlot1 ?: RoutineTask(
      id = "anchor_slot_1",
      title = "Sleep",
      type = TaskType.ANCHOR_SLEEP,
      slotNumber = 1,
      startTimeMinute = bedtimeMinute,
      durationMinutes = safeSleepDuration,
      voiceMessage = "ঘুমানোর সময় হয়েছে।",
      enableDnd = true,
      dndDelayMinutes = 0
    )).copy(
      type = TaskType.ANCHOR_SLEEP,
      slotNumber = 1,
      startTimeMinute = bedtimeMinute,
      durationMinutes = safeSleepDuration
    )
    map[1] = slot1

    // 3. Slot 3: Afternoon Focus (Default 01:15 PM = 795 min)
    if (!map.containsKey(3)) {
      map[3] = RoutineTask(
        id = "anchor_slot_3",
        title = "Dhuhr",
        type = TaskType.ANCHOR_STANDARD,
        slotNumber = 3,
        startTimeMinute = 795, // 01:15 PM
        durationMinutes = 35,  // 35 mins
        voiceMessage = "যোহরের সময় হয়েছে।",
        enableDnd = true,
        dndDelayMinutes = 8,
        fridayTitle = "Jumu'ah",
        fridayStartTimeMinute = 750,
        fridayDurationMinutes = 120,
        fridayVoiceMessage = "জুমার সময় হয়েছে।"
      )
    }

    // 4. Slot 4: Late Afternoon Reset (Default 04:30 PM = 990 min)
    if (!map.containsKey(4)) {
      map[4] = RoutineTask(
        id = "anchor_slot_4",
        title = "Asr",
        type = TaskType.ANCHOR_STANDARD,
        slotNumber = 4,
        startTimeMinute = 990, // 04:30 PM
        durationMinutes = 25,  // 25 mins
        voiceMessage = "আসরের সময় হয়েছে।",
        enableDnd = true,
        dndDelayMinutes = 10
      )
    }

    // 5. Slot 6: Evening Wind-Down (Default 06:00 PM = 1080 min)
    if (!map.containsKey(6)) {
      map[6] = RoutineTask(
        id = "anchor_slot_6",
        title = "Maghrib",
        type = TaskType.ANCHOR_STANDARD,
        slotNumber = 6,
        startTimeMinute = 1080, // 06:00 PM
        durationMinutes = 30,   // 30 mins
        voiceMessage = "মাগরিবের সময় হয়েছে।",
        enableDnd = true,
        dndDelayMinutes = 5
      )
    }

    // 6. Slot 7: Night Reflection (Default 08:00 PM = 1200 min)
    if (!map.containsKey(7)) {
      map[7] = RoutineTask(
        id = "anchor_slot_7",
        title = "Isha",
        type = TaskType.ANCHOR_STANDARD,
        slotNumber = 7,
        startTimeMinute = 1200, // 08:00 PM
        durationMinutes = 35,   // 35 mins
        voiceMessage = "এশার সময় হয়েছে।",
        enableDnd = true,
        dndDelayMinutes = 10
      )
    }

    // 7. Slot 5: Elastic / Rubber Buffer Slot
    // Positioned strictly between Task/Anchor 4 End and Anchor 6 Start
    var slot4 = map[4]!!
    var slot6 = map[6]!!

    val slot4End = slot4.startTimeMinute + slot4.durationMinutes
    // Ensure Slot 6 starts safely after Slot 4 ends (at least MIN_ELASTIC_BUFFER_MINUTES)
    if (slot6.startTimeMinute <= slot4End + MIN_ELASTIC_BUFFER_MINUTES) {
      val adjustedSlot6Start = (slot4End + MIN_ELASTIC_BUFFER_MINUTES).coerceAtMost(1300)
      slot6 = slot6.copy(startTimeMinute = adjustedSlot6Start)
      map[6] = slot6
    }

    val slot6Start = slot6.startTimeMinute
    val rawElasticGap = slot6Start - slot4End
    val elasticDuration = rawElasticGap.coerceAtLeast(MIN_ELASTIC_BUFFER_MINUTES)

    val existingSlot5 = map[5]
    val slot5 = (existingSlot5 ?: RoutineTask(
      id = "anchor_slot_5",
      title = "Break",
      type = TaskType.ANCHOR_ELASTIC,
      slotNumber = 5,
      startTimeMinute = slot4End,
      durationMinutes = elasticDuration,
      voiceMessage = "বিরতির সময় হয়েছে।",
      enableDnd = false,
      dndDelayMinutes = 0,
      selectedDays = RoutineDay.allDays()
    )).copy(
      type = TaskType.ANCHOR_ELASTIC,
      slotNumber = 5,
      startTimeMinute = slot4End,
      durationMinutes = elasticDuration,
      enableDnd = existingSlot5?.enableDnd ?: false,
      dndDelayMinutes = existingSlot5?.dndDelayMinutes ?: 0,
      selectedDays = existingSlot5?.selectedDays ?: RoutineDay.allDays()
    )
    map[5] = slot5

    return (1..7).mapNotNull { map[it] }
  }

  /**
   * Adjusts a standard anchor (Slots 2, 3, 4, 6, 7) by deltaMinutes (+/- 5 min).
   * Automatically recalculates dependent slots (Slot 1 and Slot 5).
   * Includes boundary checks to avoid negative intervals.
   */
  fun adjustAnchor(
    anchors: List<RoutineTask>,
    slotNumber: Int,
    deltaMinutes: Int,
    sleepDurationMinutes: Int
  ): List<RoutineTask> {
    val updated = anchors.map { task ->
      if (task.slotNumber == slotNumber) {
        val newStart = ((task.startTimeMinute + deltaMinutes) % 1440 + 1440) % 1440
        task.copy(startTimeMinute = newStart)
      } else {
        task
      }
    }
    return calculateAnchors(updated, sleepDurationMinutes)
  }

  /**
   * Finds available timeline gaps on a 24-hour cycle (0..1440) that are NOT occupied
   * by the 7 immutable anchors or already committed tasks via O(N log N) sorted interval merging.
   */
  fun findAvailableGaps(
    committedTasks: List<RoutineTask>,
    interTaskBufferMinutes: Int = 0
  ): List<ScheduleGap> {
    if (committedTasks.isEmpty()) {
      return listOf(ScheduleGap(0, 1440, 1440))
    }

    val intervals = ArrayList<Pair<Int, Int>>(committedTasks.size + 2)
    for (task in committedTasks) {
      val start = ((task.startTimeMinute % 1440) + 1440) % 1440
      val taskBuffer = if (task.type == TaskType.DYNAMIC) {
        task.interTaskBufferMinutes.coerceIn(0, 30)
      } else {
        0 // Anchors possess hard mathematical boundaries; do NOT bleed rest buffers across adjacent anchors
      }
      val totalBlock = (task.durationMinutes.coerceAtLeast(0) + taskBuffer).coerceAtMost(1440)
      if (totalBlock <= 0) continue

      val end = start + totalBlock
      if (end <= 1440) {
        intervals.add(start to end)
      } else {
        intervals.add(start to 1440)
        intervals.add(0 to (end - 1440))
      }
    }

    if (intervals.isEmpty()) {
      return listOf(ScheduleGap(0, 1440, 1440))
    }

    intervals.sortWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })

    val gaps = mutableListOf<ScheduleGap>()
    var mergedStart = intervals[0].first
    var mergedEnd = intervals[0].second
    var cursor = 0

    for (i in 1 until intervals.size) {
      val (nextStart, nextEnd) = intervals[i]
      if (nextStart <= mergedEnd) {
        if (nextEnd > mergedEnd) mergedEnd = nextEnd
      } else {
        if (mergedStart > cursor) {
          gaps.add(ScheduleGap(cursor, mergedStart, mergedStart - cursor))
        }
        cursor = maxOf(cursor, mergedEnd)
        mergedStart = nextStart
        mergedEnd = nextEnd
      }
    }

    if (mergedStart > cursor) {
      gaps.add(ScheduleGap(cursor, mergedStart, mergedStart - cursor))
    }
    cursor = maxOf(cursor, mergedEnd)
    if (cursor < 1440) {
      gaps.add(ScheduleGap(cursor, 1440, 1440 - cursor))
    }

    return gaps
  }

  /**
   * Compacts dynamic tasks within a single inter-anchor window [win] so that fragmented free
   * gaps merge into one contiguous block at the tail of [win], enabling [neededBlock] placement.
   * Returns the edge-aligned startMinute if rescue succeeded (mutating [scheduledTasks] in place),
   * or null if [win] cannot host [neededBlock] even after full compaction.
   */
  private fun tryCompactWindowForTask(
    win: ScheduleGap,
    neededBlock: Int,
    prefStart: Int,
    scheduledTasks: MutableList<RoutineTask>,
    interTaskBufferMinutes: Int
  ): Int? {
    val windowNonZeroDynamics = scheduledTasks
      .filter {
        it.type == TaskType.DYNAMIC &&
          it.durationMinutes > 0 &&
          it.startTimeMinute in win.startMinute until win.endMinute
      }
      .sortedBy { it.startTimeMinute }

    if (windowNonZeroDynamics.isEmpty()) return null

    val usedMinutes = windowNonZeroDynamics.sumOf {
      it.durationMinutes.coerceAtLeast(0) + it.interTaskBufferMinutes.coerceIn(0, 30)
    }
    if (win.durationMinutes - usedMinutes < neededBlock) return null

    // Pack existing non-zero dynamic tasks from win.startMinute and check remaining contiguous tail
    val packedUpdates = HashMap<String, Int>(windowNonZeroDynamics.size)
    var cursor = win.startMinute
    for (existing in windowNonZeroDynamics) {
      val dur = existing.durationMinutes.coerceAtLeast(0)
      val buf = existing.interTaskBufferMinutes.coerceIn(0, 30)
      packedUpdates[existing.id] = cursor
      cursor += dur + buf
    }

    if (win.endMinute - cursor < neededBlock) return null

    for (i in scheduledTasks.indices) {
      val current = scheduledTasks[i]
      val newStart = packedUpdates[current.id]
      if (newStart != null && newStart != current.startTimeMinute) {
        scheduledTasks[i] = current.copy(startTimeMinute = newStart)
      }
    }

    // Check gaps inside win after compaction to handle any 0-duration markers cleanly
    val compactedGaps = findAvailableGaps(scheduledTasks, interTaskBufferMinutes)
      .filter { it.startMinute >= win.startMinute && it.endMinute <= win.endMinute && it.durationMinutes >= neededBlock }
    val targetGap = compactedGaps.minByOrNull { gap ->
      val possibleStart = when {
        prefStart in gap.startMinute..(gap.endMinute - neededBlock) -> prefStart
        prefStart < gap.startMinute -> gap.startMinute
        else -> gap.endMinute - neededBlock
      }
      Math.abs(possibleStart - prefStart)
    } ?: return cursor.coerceIn(win.startMinute, (win.endMinute - neededBlock).coerceAtLeast(win.startMinute))

    return when {
      prefStart in targetGap.startMinute..(targetGap.endMinute - neededBlock) -> prefStart
      prefStart < targetGap.startMinute -> targetGap.startMinute
      else -> targetGap.endMinute - neededBlock
    }
  }

  private sealed interface PendingItem {
    val sortIsHighPriority: Boolean
    val sortIsFluidTwin: Boolean
    val sortBirthDuration: Int
    val sortBirthStart: Int

    data class Standard(val task: RoutineTask) : PendingItem {
      override val sortIsHighPriority: Boolean = task.isHighPriority
      override val sortIsFluidTwin: Boolean = false
      override val sortBirthDuration: Int = birthDurationOf(task)
      override val sortBirthStart: Int = task.originalStartTimeMinute ?: task.startTimeMinute
    }

    data class TwinPair(val part1: RoutineTask, val part2: RoutineTask) : PendingItem {
      override val sortIsHighPriority: Boolean = false
      override val sortIsFluidTwin: Boolean = true
      override val sortBirthDuration: Int = birthDurationOf(part1) + birthDurationOf(part2)
      override val sortBirthStart: Int = part1.originalStartTimeMinute ?: part1.startTimeMinute
    }

    data class TwinPartOnly(val part: RoutineTask, val partnerWinKey: Int) : PendingItem {
      override val sortIsHighPriority: Boolean = false
      override val sortIsFluidTwin: Boolean = true
      override val sortBirthDuration: Int = birthDurationOf(part)
      override val sortBirthStart: Int = part.originalStartTimeMinute ?: part.startTimeMinute
    }
  }

  private fun enclosingAnchorGapFor(
    anchorGaps: List<ScheduleGap>,
    minute: Int
  ): ScheduleGap? {
    if (anchorGaps.isEmpty()) return null
    val safeMin = ((minute % 1440) + 1440) % 1440
    return anchorGaps.firstOrNull { safeMin in it.startMinute until it.endMinute }
      ?: anchorGaps.firstOrNull { safeMin in it.startMinute..it.endMinute }
      ?: anchorGaps.minByOrNull { Math.abs(safeMin - (it.startMinute + it.endMinute) / 2) }
  }

  private fun tryPlaceEdgeAlignedInGaps(
    task: RoutineTask,
    requiredDuration: Int,
    candidateGaps: List<ScheduleGap>,
    scheduledTasks: MutableList<RoutineTask>
  ): Boolean {
    val birthStart = task.originalStartTimeMinute ?: task.startTimeMinute
    val birthDur = birthDurationOf(task)
    val taskBuffer = task.interTaskBufferMinutes.coerceIn(0, 30)
    val requiredBlock = requiredDuration + taskBuffer

    var bestGap: ScheduleGap? = null
    var bestStart = -1
    var minDistance = Int.MAX_VALUE

    for (gap in candidateGaps) {
      if (gap.durationMinutes >= requiredBlock) {
        val possibleStart = when {
          birthStart in gap.startMinute..(gap.endMinute - requiredBlock) -> birthStart
          birthStart < gap.startMinute -> gap.startMinute
          else -> gap.endMinute - requiredBlock
        }
        val dist = Math.abs(possibleStart - birthStart)
        if (dist < minDistance) {
          minDistance = dist
          bestGap = gap
          bestStart = possibleStart
        }
      }
    }

    if (bestGap != null && bestStart != -1) {
      scheduledTasks.add(
        task.copy(
          startTimeMinute = bestStart,
          originalStartTimeMinute = birthStart,
          durationMinutes = requiredDuration,
          originalDurationMinutes = task.originalDurationMinutes ?: birthDur,
          isSplitPart = false,
          isOverflow = false
        )
      )
      return true
    }
    return false
  }

  private fun tryPass1StandardTask(
    task: RoutineTask,
    scheduledTasks: MutableList<RoutineTask>,
    overflowTasks: MutableList<RoutineTask>,
    anchorGaps: List<ScheduleGap>,
    interTaskBufferMinutes: Int
  ): Boolean {
    val birthDur = birthDurationOf(task)
    val birthStart = task.originalStartTimeMinute ?: task.startTimeMinute

    if (birthDur == 0) {
      scheduledTasks.add(
        task.copy(
          startTimeMinute = ((birthStart % 1440) + 1440) % 1440,
          originalStartTimeMinute = birthStart,
          durationMinutes = 0,
          originalDurationMinutes = task.originalDurationMinutes ?: 0,
          isSplitPart = false,
          isOverflow = false
        )
      )
      return true
    }

    val gaps = findAvailableGaps(scheduledTasks, interTaskBufferMinutes)
    val taskBuffer = task.interTaskBufferMinutes.coerceIn(0, 30)
    val requiredBlock = birthDur + taskBuffer

    val enclosingAnchorGap = if (task.isHighPriority) {
      enclosingAnchorGapFor(anchorGaps, birthStart)
    } else {
      null
    }

    if (task.isHighPriority && enclosingAnchorGap == null) {
      overflowTasks.add(
        task.copy(
          originalStartTimeMinute = birthStart,
          durationMinutes = birthDur,
          originalDurationMinutes = task.originalDurationMinutes ?: birthDur,
          isOverflow = true,
          overflowReason = "High Priority task preferred time conflicts directly with an anchor wall."
        )
      )
      return true // Handled (unrecoverable anchor wall conflict)
    }

    val candidateGaps = if (task.isHighPriority && enclosingAnchorGap != null) {
      gaps.filter { it.startMinute >= enclosingAnchorGap.startMinute && it.endMinute <= enclosingAnchorGap.endMinute }
    } else {
      gaps
    }

    if (tryPlaceEdgeAlignedInGaps(task, birthDur, candidateGaps, scheduledTasks)) {
      return true
    }

    // For High Priority tasks, also allow targeted local compaction inside fenced gap before Pass 2
    if (task.isHighPriority && enclosingAnchorGap != null) {
      val compactedStart = tryCompactWindowForTask(
        win = enclosingAnchorGap,
        neededBlock = requiredBlock,
        prefStart = birthStart,
        scheduledTasks = scheduledTasks,
        interTaskBufferMinutes = interTaskBufferMinutes
      )
      if (compactedStart != null) {
        scheduledTasks.add(
          task.copy(
            startTimeMinute = compactedStart,
            originalStartTimeMinute = birthStart,
            durationMinutes = birthDur,
            originalDurationMinutes = task.originalDurationMinutes ?: birthDur,
            isSplitPart = false,
            isOverflow = false
          )
        )
        return true
      }
    }

    return false
  }

  private fun windowUsedMinutes(snapshot: List<RoutineTask>, win: ScheduleGap): Int {
    return snapshot
      .filter {
        it.type == TaskType.DYNAMIC &&
          it.durationMinutes > 0 &&
          it.startTimeMinute in win.startMinute until win.endMinute
      }
      .sumOf { it.durationMinutes.coerceAtLeast(0) + it.interTaskBufferMinutes.coerceIn(0, 30) }
  }

  private fun windowNonZeroDynamics(snapshot: List<RoutineTask>, win: ScheduleGap): List<RoutineTask> {
    return snapshot
      .filter {
        it.type == TaskType.DYNAMIC &&
          it.durationMinutes > 0 &&
          it.startTimeMinute in win.startMinute until win.endMinute
      }
      .sortedBy { it.startTimeMinute }
  }

  /**
   * Re-packs [win] inside [snapshot] after applying [cutsById] (duration reductions), removing [evictIds],
   * and optionally placing [incomingTask] with [incomingDuration]. Returns false if capacity is exceeded.
   */
  private fun repackWindowWithChanges(
    snapshot: MutableList<RoutineTask>,
    win: ScheduleGap,
    cutsById: Map<String, Int> = emptyMap(),
    evictIds: Set<String> = emptySet(),
    incomingTask: RoutineTask? = null,
    incomingDuration: Int = 0
  ): Boolean {
    val existingInWin = windowNonZeroDynamics(snapshot, win)
      .filter { it.id !in evictIds }

    val adjustedExisting = existingInWin.map { t ->
      val cut = cutsById[t.id] ?: 0
      val newDur = (t.durationMinutes - cut).coerceAtLeast(minDonorDurationOf(t))
      val birthDur = birthDurationOf(t)
      t.copy(
        durationMinutes = newDur,
        originalDurationMinutes = t.originalDurationMinutes ?: birthDur
      )
    }

    val incomingEntry = if (incomingTask != null) {
      val bBirthStart = incomingTask.originalStartTimeMinute ?: incomingTask.startTimeMinute
      val bBirthDur = birthDurationOf(incomingTask)
      incomingTask.copy(
        originalStartTimeMinute = bBirthStart,
        durationMinutes = incomingDuration,
        originalDurationMinutes = incomingTask.originalDurationMinutes ?: bBirthDur,
        isSplitPart = false,
        isOverflow = false
      )
    } else {
      null
    }

    val combined = if (incomingEntry != null) {
      val bBirthStart = incomingEntry.originalStartTimeMinute ?: incomingEntry.startTimeMinute
      if (bBirthStart in win.startMinute until win.endMinute) {
        (adjustedExisting + incomingEntry).sortedBy { it.originalStartTimeMinute ?: it.startTimeMinute }
      } else {
        adjustedExisting + incomingEntry
      }
    } else {
      adjustedExisting
    }

    val totalBlockNeeded = combined.sumOf {
      it.durationMinutes.coerceAtLeast(0) + it.interTaskBufferMinutes.coerceIn(0, 30)
    }
    if (totalBlockNeeded > win.durationMinutes) return false

    val replaceIds = (existingInWin.map { it.id } + evictIds).toSet()
    snapshot.removeAll { it.id in replaceIds }

    var cursor = win.startMinute
    for (item in combined) {
      val buf = item.interTaskBufferMinutes.coerceIn(0, 30)
      snapshot.add(item.copy(startTimeMinute = cursor))
      cursor += item.durationMinutes.coerceAtLeast(0) + buf
    }
    return true
  }

  /**
   * Relocates [movable] (at [targetDuration]) into [destWin], compacting [destWin] if fragmented.
   */
  private fun placeTaskInOtherWindow(
    snapshot: MutableList<RoutineTask>,
    destWin: ScheduleGap,
    movable: RoutineTask,
    targetDuration: Int,
    interTaskBufferMinutes: Int
  ): Boolean {
    val mBirthStart = movable.originalStartTimeMinute ?: movable.startTimeMinute
    val mBirthDur = birthDurationOf(movable)
    val mBuf = movable.interTaskBufferMinutes.coerceIn(0, 30)
    val neededBlock = targetDuration + mBuf
    val freeGapsInDest = findAvailableGaps(snapshot, interTaskBufferMinutes)
      .filter { it.startMinute >= destWin.startMinute && it.endMinute <= destWin.endMinute && it.durationMinutes >= neededBlock }

    val directGap = freeGapsInDest.minByOrNull { gap ->
      val possibleStart = when {
        mBirthStart in gap.startMinute..(gap.endMinute - neededBlock) -> mBirthStart
        mBirthStart < gap.startMinute -> gap.startMinute
        else -> gap.endMinute - neededBlock
      }
      Math.abs(possibleStart - mBirthStart)
    }

    val placedStart = if (directGap != null) {
      when {
        mBirthStart in directGap.startMinute..(directGap.endMinute - neededBlock) -> mBirthStart
        mBirthStart < directGap.startMinute -> directGap.startMinute
        else -> directGap.endMinute - neededBlock
      }
    } else {
      tryCompactWindowForTask(
        win = destWin,
        neededBlock = neededBlock,
        prefStart = mBirthStart,
        scheduledTasks = snapshot,
        interTaskBufferMinutes = interTaskBufferMinutes
      )
    } ?: return false

    snapshot.add(
      movable.copy(
        startTimeMinute = placedStart,
        originalStartTimeMinute = mBirthStart,
        durationMinutes = targetDuration,
        originalDurationMinutes = movable.originalDurationMinutes ?: mBirthDur,
        isSplitPart = false,
        isOverflow = false
      )
    )
    return true
  }

  private fun canTwinOccupyWindow(
    task: RoutineTask,
    destWin: ScheduleGap,
    snapshot: List<RoutineTask>,
    dayAnchors: List<RoutineTask>,
    activeDay: RoutineDay
  ): Boolean {
    if (!task.isFluidTwin || task.fluidTwinId.isNullOrEmpty()) return true
    val partner = snapshot.firstOrNull {
      it.id != task.id &&
        it.fluidTwinId == task.fluidTwinId &&
        it.type == TaskType.DYNAMIC
    } ?: return true
    val partnerWinKey = getInterAnchorWindowKey(dayAnchors, partner.startTimeMinute, activeDay)
    val destWinKey = getInterAnchorWindowKey(dayAnchors, destWin.startMinute, activeDay)
    return partnerWinKey != destWinKey
  }

  /**
   * Attempts Strategy #1 (Cross-Anchor Smart Task Migration) for blocked [task] inside [win].
   * Moves smaller non-priority tasks out of [win] at full duration into free tail gaps of other windows.
   */
  private fun tryCrossAnchorMigration(
    task: RoutineTask,
    win: ScheduleGap,
    anchorGaps: List<ScheduleGap>,
    dayAnchors: List<RoutineTask>,
    activeDay: RoutineDay,
    scheduledTasks: MutableList<RoutineTask>,
    interTaskBufferMinutes: Int
  ): Boolean {
    val birthDur = birthDurationOf(task)
    val bBuf = task.interTaskBufferMinutes.coerceIn(0, 30)
    val bFullBlock = birthDur + bBuf
    val freeInW = win.durationMinutes - windowUsedMinutes(scheduledTasks, win)
    val neededToFree = bFullBlock - freeInW
    if (neededToFree <= 0) return false

    val movables = windowNonZeroDynamics(scheduledTasks, win)
      .filter { !it.isHighPriority }
      .sortedWith(
        compareBy<RoutineTask> { it.isFluidTwin }
          .thenBy { it.durationMinutes + it.interTaskBufferMinutes.coerceIn(0, 30) }
      )
    if (movables.isEmpty()) return false

    val snapshot = ArrayList(scheduledTasks)
    val otherWindows = anchorGaps
      .filter { it.startMinute != win.startMinute || it.endMinute != win.endMinute }
      .sortedBy { Math.abs((it.startMinute + it.endMinute) / 2 - (win.startMinute + win.endMinute) / 2) }

    val migratedIds = mutableSetOf<String>()
    var freedMinutes = 0

    for (m in movables) {
      if (freedMinutes >= neededToFree) break
      val mDur = m.durationMinutes.coerceAtLeast(0)
      val mBlock = mDur + m.interTaskBufferMinutes.coerceIn(0, 30)
      val destWin = otherWindows.firstOrNull { candWin ->
        val availInDest = candWin.durationMinutes - windowUsedMinutes(snapshot, candWin)
        availInDest >= mBlock && canTwinOccupyWindow(m, candWin, snapshot, dayAnchors, activeDay)
      }
      if (destWin != null) {
        snapshot.removeAll { it.id == m.id }
        if (placeTaskInOtherWindow(snapshot, destWin, m, mDur, interTaskBufferMinutes)) {
          migratedIds.add(m.id)
          freedMinutes += mBlock
        } else {
          return false
        }
      }
    }

    if (freedMinutes < neededToFree || migratedIds.isEmpty()) return false

    if (repackWindowWithChanges(snapshot, win, incomingTask = task, incomingDuration = birthDur)) {
      scheduledTasks.clear()
      scheduledTasks.addAll(snapshot)
      return true
    }
    return false
  }

  /**
   * Attempts Strategy #4 (Micro-Deficit Neighbor Borrowing for 1..5m deficits) inside [win].
   */
  private fun tryMicroDeficitBorrowing(
    task: RoutineTask,
    win: ScheduleGap,
    scheduledTasks: MutableList<RoutineTask>
  ): Boolean {
    val birthDur = birthDurationOf(task)
    val bBuf = task.interTaskBufferMinutes.coerceIn(0, 30)
    val bFullBlock = birthDur + bBuf
    val freeInW = win.durationMinutes - windowUsedMinutes(scheduledTasks, win)
    val deficit = bFullBlock - freeInW
    if (deficit !in 1..5) return false

    val donors = windowNonZeroDynamics(scheduledTasks, win)
      .map { it to availableFlexSlackOf(it) }
      .filter { it.second > 0 }
      .sortedWith(
        compareBy<Pair<RoutineTask, Int>> { it.first.isHighPriority }
          .thenByDescending { it.second }
          .thenByDescending { it.first.durationMinutes }
      )
    if (donors.isEmpty()) return false

    val cuts = HashMap<String, Int>(2)
    // Prefer 1 single donor if possible, else up to 2 donors
    val single = donors.firstOrNull { it.second >= deficit }
    if (single != null) {
      cuts[single.first.id] = deficit
    } else if (donors.size >= 2 && donors[0].second + donors[1].second >= deficit) {
      val cut0 = minOf(donors[0].second, deficit)
      cuts[donors[0].first.id] = cut0
      cuts[donors[1].first.id] = deficit - cut0
    } else {
      return false
    }

    val snapshot = ArrayList(scheduledTasks)
    if (repackWindowWithChanges(snapshot, win, cutsById = cuts, incomingTask = task, incomingDuration = birthDur)) {
      scheduledTasks.clear()
      scheduledTasks.addAll(snapshot)
      return true
    }
    return false
  }

  /**
   * Attempts Strategy #2 (Collective Fair-Share Elastic Compression via Largest-Remainder Water-Filling) inside [win].
   * Exhausts Tier-1 (non-Priority) slack before touching Tier-2 (High Priority) slack.
   */
  private fun tryCollectiveFairShareCompression(
    task: RoutineTask,
    win: ScheduleGap,
    scheduledTasks: MutableList<RoutineTask>
  ): Boolean {
    val birthDur = birthDurationOf(task)
    val bBuf = task.interTaskBufferMinutes.coerceIn(0, 30)
    val bFullBlock = birthDur + bBuf
    val freeInW = win.durationMinutes - windowUsedMinutes(scheduledTasks, win)
    val deficit = bFullBlock - freeInW
    if (deficit <= 0) return false

    val winTasks = windowNonZeroDynamics(scheduledTasks, win)
    val bSlack = (birthDur - minAllowedDurationOf(task)).coerceAtLeast(0)

    val tier1Existing = winTasks.filter { !it.isHighPriority && availableFlexSlackOf(it) > 0 }
    val tier1ExistingSlacks = tier1Existing.map { availableFlexSlackOf(it) }
    val tier1IncludeB = !task.isHighPriority && bSlack > 0
    val tier1TotalSlack = tier1ExistingSlacks.sum() + (if (tier1IncludeB) bSlack else 0)

    val tier2Existing = winTasks.filter { it.isHighPriority && availableFlexSlackOf(it) > 0 }
    val tier2ExistingSlacks = tier2Existing.map { availableFlexSlackOf(it) }
    val tier2IncludeB = task.isHighPriority && bSlack > 0
    val tier2TotalSlack = tier2ExistingSlacks.sum() + (if (tier2IncludeB) bSlack else 0)

    if (tier1TotalSlack + tier2TotalSlack < deficit) return false

    val cutsById = HashMap<String, Int>()
    var bCut = 0

    if (tier1TotalSlack >= deficit) {
      val slacks = IntArray(tier1Existing.size + (if (tier1IncludeB) 1 else 0))
      for (i in tier1Existing.indices) slacks[i] = tier1ExistingSlacks[i]
      if (tier1IncludeB) slacks[tier1Existing.size] = bSlack
      val dist = distributeDeficitWaterFilling(slacks, deficit)
      for (i in tier1Existing.indices) {
        if (dist[i] > 0) cutsById[tier1Existing[i].id] = dist[i]
      }
      if (tier1IncludeB) bCut = dist[tier1Existing.size]
    } else {
      // Exhaust all Tier-1 slack first, then distribute remaining deficit across Tier-2 via water-filling
      for (i in tier1Existing.indices) {
        if (tier1ExistingSlacks[i] > 0) cutsById[tier1Existing[i].id] = tier1ExistingSlacks[i]
      }
      if (tier1IncludeB) bCut = bSlack

      val remDeficit = deficit - tier1TotalSlack
      val t2Slacks = IntArray(tier2Existing.size + (if (tier2IncludeB) 1 else 0))
      for (i in tier2Existing.indices) t2Slacks[i] = tier2ExistingSlacks[i]
      if (tier2IncludeB) t2Slacks[tier2Existing.size] = bSlack
      val dist2 = distributeDeficitWaterFilling(t2Slacks, remDeficit)
      for (i in tier2Existing.indices) {
        if (dist2[i] > 0) cutsById[tier2Existing[i].id] = dist2[i]
      }
      if (tier2IncludeB) bCut = dist2[tier2Existing.size]
    }

    val bFinalDur = (birthDur - bCut).coerceIn(minAllowedDurationOf(task), birthDur)
    val snapshot = ArrayList(scheduledTasks)
    if (repackWindowWithChanges(snapshot, win, cutsById = cutsById, incomingTask = task, incomingDuration = bFinalDur)) {
      scheduledTasks.clear()
      scheduledTasks.addAll(snapshot)
      return true
    }
    return false
  }

  /**
   * Attempts Strategy #3 (Flex-to-Shrink & Relocate):
   * - 3A: Self-shrink [task] within its own flexibility into smaller gaps across [candidateWindows].
   * - 3B: Shrink movable non-priority neighbor [M] inside [win] within M's flex so [M] fits into a smaller
   *       tail gap in another window, freeing [win] for [task] (with optional fair-share in [win] if needed).
   */
  private fun tryFlexToShrinkAndRelocate(
    task: RoutineTask,
    candidateWindows: List<ScheduleGap>,
    anchorGaps: List<ScheduleGap>,
    dayAnchors: List<RoutineTask>,
    activeDay: RoutineDay,
    scheduledTasks: MutableList<RoutineTask>,
    interTaskBufferMinutes: Int
  ): Boolean {
    val birthStart = task.originalStartTimeMinute ?: task.startTimeMinute
    val birthDur = birthDurationOf(task)
    val bBuf = task.interTaskBufferMinutes.coerceIn(0, 30)
    val bMinDur = minAllowedDurationOf(task)
    val bMinBlock = bMinDur + bBuf

    // Strategy 3A: Self-shrink into smaller open or compacted gap across candidate windows
    if (birthDur > bMinDur) {
      val currentGaps = findAvailableGaps(scheduledTasks, interTaskBufferMinutes)
      val eligibleGaps = if (task.isHighPriority) {
        val enc = enclosingAnchorGapFor(anchorGaps, birthStart)
        if (enc != null) {
          currentGaps.filter { it.startMinute >= enc.startMinute && it.endMinute <= enc.endMinute }
        } else {
          emptyList()
        }
      } else {
        currentGaps
      }

      var bestGap: ScheduleGap? = null
      var bestStart = -1
      var bestDur = -1
      var minDist = Int.MAX_VALUE

      for (gap in eligibleGaps) {
        if (gap.durationMinutes >= bMinBlock) {
          val fittedDur = (gap.durationMinutes - bBuf).coerceIn(bMinDur, birthDur)
          val fittedBlock = fittedDur + bBuf
          val possibleStart = when {
            birthStart in gap.startMinute..(gap.endMinute - fittedBlock) -> birthStart
            birthStart < gap.startMinute -> gap.startMinute
            else -> gap.endMinute - fittedBlock
          }
          val dist = Math.abs(possibleStart - birthStart)
          if (dist < minDist) {
            minDist = dist
            bestGap = gap
            bestStart = possibleStart
            bestDur = fittedDur
          }
        }
      }

      if (bestGap != null && bestStart != -1 && bestDur >= bMinDur) {
        scheduledTasks.add(
          task.copy(
            startTimeMinute = bestStart,
            originalStartTimeMinute = birthStart,
            durationMinutes = bestDur,
            originalDurationMinutes = task.originalDurationMinutes ?: birthDur,
            isSplitPart = false,
            isOverflow = false
          )
        )
        return true
      }

      // Also check compacted window tails for self-shrink
      for (win in candidateWindows) {
        val freeInW = win.durationMinutes - windowUsedMinutes(scheduledTasks, win)
        if (freeInW >= bMinBlock) {
          val fittedDur = (freeInW - bBuf).coerceIn(bMinDur, birthDur)
          val snapshot = ArrayList(scheduledTasks)
          if (repackWindowWithChanges(snapshot, win, incomingTask = task, incomingDuration = fittedDur)) {
            scheduledTasks.clear()
            scheduledTasks.addAll(snapshot)
            return true
          }
        }
      }
    }

    // Strategy 3B: Shrink movable non-priority neighbor M inside win within M's flex to relocate M to another window
    for (win in candidateWindows) {
      val freeInW = win.durationMinutes - windowUsedMinutes(scheduledTasks, win)
      val neededToFree = (birthDur + bBuf) - freeInW
      if (neededToFree <= 0) continue

      val otherWindows = anchorGaps
        .filter { it.startMinute != win.startMinute || it.endMinute != win.endMinute }
        .sortedBy { Math.abs((it.startMinute + it.endMinute) / 2 - (win.startMinute + win.endMinute) / 2) }

      val movables = windowNonZeroDynamics(scheduledTasks, win)
        .filter { !it.isHighPriority && availableFlexSlackOf(it) > 0 }
        .sortedWith(
          compareBy<RoutineTask> { it.isFluidTwin }
            .thenBy { it.durationMinutes }
        )

      for (m in movables) {
        val mBuf = m.interTaskBufferMinutes.coerceIn(0, 30)
        val mMinDur = minDonorDurationOf(m)
        val mMinBlock = mMinDur + mBuf
        val mCurrentBlock = m.durationMinutes + mBuf

        val destTuple = otherWindows.mapNotNull { destWin ->
          if (!canTwinOccupyWindow(m, destWin, scheduledTasks, dayAnchors, activeDay)) {
            null
          } else {
            val destFree = destWin.durationMinutes - windowUsedMinutes(scheduledTasks, destWin)
            if (destFree >= mMinBlock) {
              val fittedMDur = (destFree - mBuf).coerceIn(mMinDur, m.durationMinutes)
              destWin to fittedMDur
            } else {
              null
            }
          }
        }.maxByOrNull { it.second } ?: continue

        val (destWin, fittedMDur) = destTuple
        val snapshot = ArrayList(scheduledTasks)
        snapshot.removeAll { it.id == m.id }
        if (!placeTaskInOtherWindow(snapshot, destWin, m, fittedMDur, interTaskBufferMinutes)) {
          continue
        }

        // Now check if win can host task (either directly or via collective fair-share)
        val newFreeInW = win.durationMinutes - windowUsedMinutes(snapshot, win)
        if (newFreeInW >= birthDur + bBuf) {
          if (repackWindowWithChanges(snapshot, win, incomingTask = task, incomingDuration = birthDur)) {
            scheduledTasks.clear()
            scheduledTasks.addAll(snapshot)
            return true
          }
        } else if (tryCollectiveFairShareCompression(task, win, snapshot)) {
          scheduledTasks.clear()
          scheduledTasks.addAll(snapshot)
          return true
        }
      }

      // Multi-evict combination if single movable wasn't enough (bounded, deterministic)
      val allMovables = windowNonZeroDynamics(scheduledTasks, win)
        .filter { !it.isHighPriority }
        .sortedBy { it.durationMinutes }
      if (allMovables.size >= 2) {
        val snapshot = ArrayList(scheduledTasks)
        var evictedCount = 0
        for (m in allMovables) {
          val mBuf = m.interTaskBufferMinutes.coerceIn(0, 30)
          val mMinDur = minDonorDurationOf(m)
          val mMinBlock = mMinDur + mBuf
          val destTuple = otherWindows.mapNotNull { destWin ->
            if (!canTwinOccupyWindow(m, destWin, snapshot, dayAnchors, activeDay)) {
              null
            } else {
              val destFree = destWin.durationMinutes - windowUsedMinutes(snapshot, destWin)
              if (destFree >= mMinBlock) {
                destWin to (destFree - mBuf).coerceIn(mMinDur, m.durationMinutes)
              } else {
                null
              }
            }
          }.maxByOrNull { it.second }

          if (destTuple != null) {
            snapshot.removeAll { it.id == m.id }
            if (placeTaskInOtherWindow(snapshot, destTuple.first, m, destTuple.second, interTaskBufferMinutes)) {
              evictedCount++
              val newFree = win.durationMinutes - windowUsedMinutes(snapshot, win)
              if (newFree >= birthDur + bBuf) {
                if (repackWindowWithChanges(snapshot, win, incomingTask = task, incomingDuration = birthDur)) {
                  scheduledTasks.clear()
                  scheduledTasks.addAll(snapshot)
                  return true
                }
              } else if (tryCollectiveFairShareCompression(task, win, snapshot)) {
                scheduledTasks.clear()
                scheduledTasks.addAll(snapshot)
                return true
              }
            }
          }
        }
        if (evictedCount > 0) {
          // If still couldn't fit at birthDur or fair-share, check self-shrink inside freed window
          val newFree = win.durationMinutes - windowUsedMinutes(snapshot, win)
          if (newFree >= bMinBlock) {
            val fittedDur = (newFree - bBuf).coerceIn(bMinDur, birthDur)
            if (repackWindowWithChanges(snapshot, win, incomingTask = task, incomingDuration = fittedDur)) {
              scheduledTasks.clear()
              scheduledTasks.addAll(snapshot)
              return true
            }
          }
        }
      }
    }

    return false
  }

  private fun trySolveStandardPendingTask(
    task: RoutineTask,
    anchorGaps: List<ScheduleGap>,
    dayAnchors: List<RoutineTask>,
    activeDay: RoutineDay,
    scheduledTasks: MutableList<RoutineTask>,
    overflowTasks: MutableList<RoutineTask>,
    interTaskBufferMinutes: Int
  ) {
    val birthStart = task.originalStartTimeMinute ?: task.startTimeMinute
    val birthDur = birthDurationOf(task)
    val bBuf = task.interTaskBufferMinutes.coerceIn(0, 30)
    val bFullBlock = birthDur + bBuf

    val enclosingAnchorGap = if (task.isHighPriority) {
      enclosingAnchorGapFor(anchorGaps, birthStart)
    } else {
      null
    }

    if (task.isHighPriority && enclosingAnchorGap == null) {
      overflowTasks.add(
        task.copy(
          originalStartTimeMinute = birthStart,
          durationMinutes = birthDur,
          originalDurationMinutes = task.originalDurationMinutes ?: birthDur,
          isOverflow = true,
          overflowReason = "High Priority task preferred time conflicts directly with an anchor wall."
        )
      )
      return
    }

    val candidateWindows = if (task.isHighPriority && enclosingAnchorGap != null) {
      listOf(enclosingAnchorGap)
    } else {
      val prefWin = anchorGaps.firstOrNull { birthStart in it.startMinute until it.endMinute }
      val rest = anchorGaps
        .filter { it != prefWin }
        .sortedBy { Math.abs((it.startMinute + it.endMinute) / 2 - birthStart) }
      if (prefWin != null) listOf(prefWin) + rest else rest
    }

    // 0. Check if gap opened or local compaction inside any candidate window fits task at full birthDuration
    val gaps = findAvailableGaps(scheduledTasks, interTaskBufferMinutes)
    val candidateGaps = if (task.isHighPriority && enclosingAnchorGap != null) {
      gaps.filter { it.startMinute >= enclosingAnchorGap.startMinute && it.endMinute <= enclosingAnchorGap.endMinute }
    } else {
      gaps
    }
    if (tryPlaceEdgeAlignedInGaps(task, birthDur, candidateGaps, scheduledTasks)) return

    for (win in candidateWindows) {
      val compactedStart = tryCompactWindowForTask(
        win = win,
        neededBlock = bFullBlock,
        prefStart = birthStart,
        scheduledTasks = scheduledTasks,
        interTaskBufferMinutes = interTaskBufferMinutes
      )
      if (compactedStart != null) {
        scheduledTasks.add(
          task.copy(
            startTimeMinute = compactedStart,
            originalStartTimeMinute = birthStart,
            durationMinutes = birthDur,
            originalDurationMinutes = task.originalDurationMinutes ?: birthDur,
            isSplitPart = false,
            isOverflow = false
          )
        )
        return
      }
    }

    // Strategy #1: Cross-Anchor Smart Task Migration (move smaller non-HP tasks out at full duration)
    for (win in candidateWindows) {
      if (tryCrossAnchorMigration(task, win, anchorGaps, dayAnchors, activeDay, scheduledTasks, interTaskBufferMinutes)) {
        return
      }
    }

    // Strategy #4: Micro-Deficit Neighbor Borrowing (1..5m deficit covered by 1-2 flexible donors)
    for (win in candidateWindows) {
      if (tryMicroDeficitBorrowing(task, win, scheduledTasks)) {
        return
      }
    }

    // Strategy #2: Collective Fair-Share Elastic Compression (Water-Filling across window participants)
    for (win in candidateWindows) {
      if (tryCollectiveFairShareCompression(task, win, scheduledTasks)) {
        return
      }
    }

    // Strategy #3: Flex-to-Shrink & Relocate (3A self-shrink, 3B shrink-and-relocate movable neighbor)
    if (tryFlexToShrinkAndRelocate(task, candidateWindows, anchorGaps, dayAnchors, activeDay, scheduledTasks, interTaskBufferMinutes)) {
      return
    }

    val overflowReason = if (task.isHighPriority) {
      "High Priority task cannot fit between assigned anchor boundaries without overleaping walls."
    } else {
      "No available open slot can accommodate ${birthDur}m without violating anchors"
    }
    overflowTasks.add(
      task.copy(
        originalStartTimeMinute = birthStart,
        durationMinutes = birthDur,
        originalDurationMinutes = task.originalDurationMinutes ?: birthDur,
        isOverflow = true,
        overflowReason = overflowReason
      )
    )
  }

  private fun tryEnsureWindowRoomForTwinPart(
    win: ScheduleGap,
    minNeededBlock: Int,
    anchorGaps: List<ScheduleGap>,
    dayAnchors: List<RoutineTask>,
    activeDay: RoutineDay,
    scheduledTasks: MutableList<RoutineTask>,
    interTaskBufferMinutes: Int,
    excludedWinKey: Int? = null
  ): ScheduleGap? {
    val freeNow = findAvailableGaps(scheduledTasks, interTaskBufferMinutes)
      .filter { it.startMinute >= win.startMinute && it.endMinute <= win.endMinute && it.durationMinutes >= minNeededBlock }
      .maxByOrNull { it.durationMinutes }
    if (freeNow != null) return freeNow

    // Try local compaction inside win
    val snapshotCompact = ArrayList(scheduledTasks)
    if (tryCompactWindowForTask(win, minNeededBlock, win.startMinute, snapshotCompact, interTaskBufferMinutes) != null) {
      val compacted = findAvailableGaps(snapshotCompact, interTaskBufferMinutes)
        .filter { it.startMinute >= win.startMinute && it.endMinute <= win.endMinute && it.durationMinutes >= minNeededBlock }
        .maxByOrNull { it.durationMinutes }
      if (compacted != null) {
        scheduledTasks.clear()
        scheduledTasks.addAll(snapshotCompact)
        return compacted
      }
    }

    // Try migrating non-priority non-twin tasks out of win into free space in other windows
    val freeInW = win.durationMinutes - windowUsedMinutes(scheduledTasks, win)
    val neededToFree = minNeededBlock - freeInW
    if (neededToFree > 0) {
      val movables = windowNonZeroDynamics(scheduledTasks, win)
        .filter { !it.isHighPriority && !it.isFluidTwin }
        .sortedBy { it.durationMinutes }
      if (movables.isNotEmpty()) {
        val snap = ArrayList(scheduledTasks)
        val otherWins = anchorGaps
          .filter { (it.startMinute != win.startMinute || it.endMinute != win.endMinute) &&
            (excludedWinKey == null || getInterAnchorWindowKey(dayAnchors, it.startMinute, activeDay) != excludedWinKey) }
        var freed = 0
        for (m in movables) {
          if (freed >= neededToFree) break
          val mDur = m.durationMinutes
          val mBlock = mDur + m.interTaskBufferMinutes.coerceIn(0, 30)
          val dest = otherWins.firstOrNull { cw -> cw.durationMinutes - windowUsedMinutes(snap, cw) >= mBlock }
          if (dest != null) {
            snap.removeAll { it.id == m.id }
            if (placeTaskInOtherWindow(snap, dest, m, mDur, interTaskBufferMinutes)) {
              freed += mBlock
            }
          }
        }
        if (freed >= neededToFree && repackWindowWithChanges(snap, win)) {
          val after = findAvailableGaps(snap, interTaskBufferMinutes)
            .filter { it.startMinute >= win.startMinute && it.endMinute <= win.endMinute && it.durationMinutes >= minNeededBlock }
            .maxByOrNull { it.durationMinutes }
          if (after != null) {
            scheduledTasks.clear()
            scheduledTasks.addAll(snap)
            return after
          }
        }
      }
    }

    return null
  }

  /**
   * Main Dynamic Scheduling Function (v1.3 Two-Tier Human-Level Constraint Solver):
   * - Pass 1 (Baseline Fit): Evaluates Priority -> Fluid Twins -> Normal tasks at full birth duration
   *   without premature shrinking or overflowing; queues blocked tasks into [pendingQueue].
   * - Pass 2 (Human-Level Solver): Resolves [pendingQueue] sorted by Priority/Twin/Duration/Time via:
   *   Local Compaction -> Strategy #1 Cross-Anchor Migration -> Strategy #4 Micro-Deficit Borrowing ->
   *   Strategy #2 Fair-Share Water-Filling -> Strategy #3 Flex-to-Shrink & Relocate.
   */
  fun scheduleDay(
    anchors: List<RoutineTask>,
    dynamicTasks: List<RoutineTask>,
    activeDay: RoutineDay,
    interTaskBufferMinutes: Int = 0
  ): ScheduleResult {
    // 1. Anchors active on this day (excluding soft-deleted and paused)
    val dayAnchors = resolveEffectiveDayAnchors(anchors, activeDay)
    val scheduledTasks = ArrayList(dayAnchors)
    val overflowTasks = mutableListOf<RoutineTask>()
    val anchorGaps = findAvailableGaps(dayAnchors, 0)

    // Filter dynamic tasks for the active day (excluding soft-deleted and paused)
    val dayDynamicTasks = dynamicTasks.filter { !it.isDeleted && !it.isCurrentlyPaused() && it.selectedDays.contains(activeDay) }

    val (fluidTwinTasks, standardTasks) = dayDynamicTasks.partition { it.isFluidTwin && !it.fluidTwinId.isNullOrEmpty() }
    val (highPriorityStandardTasks, normalStandardTasks) = standardTasks.partition { it.isHighPriority }
    val pendingQueue = mutableListOf<PendingItem>()

    // Pass 1A: High Priority standard tasks (baseline edge fit or fenced window compaction)
    val sortedHighPriorityTasks = highPriorityStandardTasks.sortedWith(
      compareByDescending<RoutineTask> { it.manualPlacementOrder }
        .thenBy { it.originalStartTimeMinute ?: it.startTimeMinute }
    )
    for (hpTask in sortedHighPriorityTasks) {
      if (!tryPass1StandardTask(hpTask, scheduledTasks, overflowTasks, anchorGaps, interTaskBufferMinutes)) {
        pendingQueue.add(PendingItem.Standard(hpTask))
      }
    }

    // Pass 1B: Fluid Twins (after High Priority anchor-fenced tasks)
    val fluidTwinGroups = fluidTwinTasks.groupBy { it.fluidTwinId!! }
    for ((_, twinGroup) in fluidTwinGroups) {
      val part1 = twinGroup.find { it.fluidTwinPart == 1 } ?: twinGroup.getOrNull(0)
      val part2 = twinGroup.find { it.fluidTwinPart == 2 } ?: twinGroup.getOrNull(1)

      if (part1 == null || part2 == null || part1.id == part2.id) {
        val lone = part1 ?: part2
        if (lone != null) {
          val birthStart = lone.originalStartTimeMinute ?: lone.startTimeMinute
          val birthDur = birthDurationOf(lone)
          val gaps = findAvailableGaps(scheduledTasks, interTaskBufferMinutes)
          val fitGap = gaps.find { birthStart in it.startMinute until it.endMinute && it.durationMinutes >= birthDur }
            ?: gaps.find { it.durationMinutes >= birthDur }
          if (fitGap != null) {
            val start = if (birthStart in fitGap.startMinute..(fitGap.endMinute - birthDur)) birthStart else fitGap.startMinute
            scheduledTasks.add(
              lone.copy(
                startTimeMinute = start,
                originalStartTimeMinute = birthStart,
                durationMinutes = birthDur,
                originalDurationMinutes = lone.originalDurationMinutes ?: birthDur,
                isSplitPart = false,
                isOverflow = false
              )
            )
          } else {
            overflowTasks.add(
              lone.copy(
                originalStartTimeMinute = birthStart,
                durationMinutes = birthDur,
                originalDurationMinutes = lone.originalDurationMinutes ?: birthDur,
                isOverflow = true,
                overflowReason = "Fluid twin partner missing and no single gap available."
              )
            )
          }
        }
      } else {
        val p1Birth = part1.originalStartTimeMinute ?: part1.startTimeMinute
        val p2Birth = part2.originalStartTimeMinute ?: part2.startTimeMinute
        val p1BirthDur = birthDurationOf(part1)
        val p2BirthDur = birthDurationOf(part2)

        val freeGaps = findAvailableGaps(scheduledTasks, interTaskBufferMinutes)
        val pool = part1.fluidTotalPoolMinutes ?: (p1BirthDur + p2BirthDur)
        val floor1 = if (p1BirthDur <= 0) 0 else part1.fluidSafetyFloorMinutes.coerceIn(1, p1BirthDur)
        val floor2 = if (p2BirthDur <= 0) 0 else part2.fluidSafetyFloorMinutes.coerceIn(1, p2BirthDur)
        val b1 = part1.interTaskBufferMinutes.coerceIn(0, 30)
        val b2 = part2.interTaskBufferMinutes.coerceIn(0, 30)

        if (p1BirthDur == 0 && p2BirthDur == 0) {
          scheduledTasks.add(
            part1.copy(
              startTimeMinute = p1Birth.coerceIn(0, 1439),
              originalStartTimeMinute = p1Birth,
              durationMinutes = 0,
              originalDurationMinutes = part1.originalDurationMinutes ?: 0,
              isSplitPart = false,
              isOverflow = false
            )
          )
          scheduledTasks.add(
            part2.copy(
              startTimeMinute = p2Birth.coerceIn(0, 1439),
              originalStartTimeMinute = p2Birth,
              durationMinutes = 0,
              originalDurationMinutes = part2.originalDurationMinutes ?: 0,
              isSplitPart = false,
              isOverflow = false
            )
          )
          continue
        }

        val birthGap1 = freeGaps.find { p1Birth in it.startMinute until it.endMinute }
          ?: freeGaps.minByOrNull { Math.abs((it.startMinute + it.endMinute) / 2 - p1Birth) }
        val gap1 = if (birthGap1 != null && birthGap1.durationMinutes >= floor1 + b1) {
          birthGap1
        } else {
          freeGaps.filter { it.durationMinutes >= floor1 + b1 }
            .minByOrNull { Math.abs((it.startMinute + it.endMinute) / 2 - p1Birth) }
        }

        if (gap1 == null) {
          pendingQueue.add(PendingItem.TwinPair(part1, part2))
          continue
        }

        val win1 = getInterAnchorWindowKey(dayAnchors, gap1.startMinute, activeDay)

        val preferredGap2 = freeGaps.find { p2Birth in it.startMinute until it.endMinute }
          ?: freeGaps.minByOrNull { Math.abs((it.startMinute + it.endMinute) / 2 - p2Birth) }

        val gap2 = if (
          preferredGap2 != null &&
          preferredGap2.durationMinutes >= floor2 + b2 &&
          getInterAnchorWindowKey(dayAnchors, preferredGap2.startMinute, activeDay) != win1
        ) {
          preferredGap2
        } else {
          freeGaps.filter {
            it.durationMinutes >= floor2 + b2 &&
              getInterAnchorWindowKey(dayAnchors, it.startMinute, activeDay) != win1
          }.minByOrNull { Math.abs((it.startMinute + it.endMinute) / 2 - p2Birth) }
        }

        val cap1 = (gap1.durationMinutes - b1).coerceAtLeast(0)
        if (gap2 == null) {
          val d1 = p1BirthDur.coerceIn(floor1, cap1)
          val maxStart1 = (gap1.endMinute - (d1 + b1)).coerceAtLeast(gap1.startMinute)
          val start1 = when {
            p1Birth in gap1.startMinute..maxStart1 -> p1Birth
            p1Birth < gap1.startMinute -> gap1.startMinute
            else -> maxStart1
          }
          scheduledTasks.add(
            part1.copy(
              startTimeMinute = start1,
              originalStartTimeMinute = p1Birth,
              durationMinutes = d1,
              originalDurationMinutes = part1.originalDurationMinutes ?: p1BirthDur,
              isSplitPart = false,
              isOverflow = false
            )
          )
          pendingQueue.add(PendingItem.TwinPartOnly(part2, win1))
          continue
        }

        val cap2 = (gap2.durationMinutes - b2).coerceAtLeast(0)
        if (cap1 < floor1 || cap2 < floor2) {
          pendingQueue.add(PendingItem.TwinPair(part1, part2))
          continue
        }

        var d1 = p1BirthDur.coerceIn(floor1, cap1)
        var d2 = (pool - d1).coerceAtLeast(floor2)

        if (d2 > cap2) {
          d2 = cap2
          d1 = (pool - d2).coerceIn(floor1, cap1)
        }
        if (d1 > cap1) {
          d1 = cap1
          d2 = (pool - d1).coerceIn(floor2, cap2)
        }

        val maxStart1 = gap1.endMinute - (d1 + b1)
        val start1 = when {
          maxStart1 >= gap1.startMinute && p1Birth in gap1.startMinute..maxStart1 -> p1Birth
          p1Birth < gap1.startMinute -> gap1.startMinute
          maxStart1 >= gap1.startMinute -> maxStart1
          else -> gap1.startMinute
        }

        val maxStart2 = gap2.endMinute - (d2 + b2)
        val start2 = when {
          maxStart2 >= gap2.startMinute && p2Birth in gap2.startMinute..maxStart2 -> p2Birth
          p2Birth < gap2.startMinute -> gap2.startMinute
          maxStart2 >= gap2.startMinute -> maxStart2
          else -> gap2.startMinute
        }

        scheduledTasks.add(
          part1.copy(
            startTimeMinute = start1,
            originalStartTimeMinute = p1Birth,
            durationMinutes = d1,
            originalDurationMinutes = part1.originalDurationMinutes ?: p1BirthDur,
            isSplitPart = false,
            isOverflow = false
          )
        )
        scheduledTasks.add(
          part2.copy(
            startTimeMinute = start2,
            originalStartTimeMinute = p2Birth,
            durationMinutes = d2,
            originalDurationMinutes = part2.originalDurationMinutes ?: p2BirthDur,
            isSplitPart = false,
            isOverflow = false
          )
        )
      }
    }

    // Pass 1C: Normal Standard Dynamic Tasks (full birth duration edge-fit only; blocked tasks queue for Pass 2)
    val sortedNormalTasks = normalStandardTasks.sortedWith(
      compareByDescending<RoutineTask> { it.manualPlacementOrder }
        .thenBy { it.originalStartTimeMinute ?: it.startTimeMinute }
    )
    for (dynamicTask in sortedNormalTasks) {
      if (!tryPass1StandardTask(dynamicTask, scheduledTasks, overflowTasks, anchorGaps, interTaskBufferMinutes)) {
        pendingQueue.add(PendingItem.Standard(dynamicTask))
      }
    }

    // Pass 2: Human-Level 4-Strategy Constraint Solver on pendingQueue
    val sortedPending = pendingQueue.sortedWith(
      compareByDescending<PendingItem> { it.sortIsHighPriority }
        .thenByDescending { it.sortIsFluidTwin }
        .thenByDescending { it.sortBirthDuration }
        .thenBy { it.sortBirthStart }
    )

    for (item in sortedPending) {
      when (item) {
        is PendingItem.Standard -> {
          trySolveStandardPendingTask(
            task = item.task,
            anchorGaps = anchorGaps,
            dayAnchors = dayAnchors,
            activeDay = activeDay,
            scheduledTasks = scheduledTasks,
            overflowTasks = overflowTasks,
            interTaskBufferMinutes = interTaskBufferMinutes
          )
        }

        is PendingItem.TwinPartOnly -> {
          val p2 = item.part
          val p2Birth = p2.originalStartTimeMinute ?: p2.startTimeMinute
          val p2BirthDur = birthDurationOf(p2)
          val b2 = p2.interTaskBufferMinutes.coerceIn(0, 30)
          val floor2 = if (p2BirthDur <= 0) 0 else p2.fluidSafetyFloorMinutes.coerceIn(1, p2BirthDur)

          val distinctWindows = anchorGaps
            .filter { getInterAnchorWindowKey(dayAnchors, it.startMinute, activeDay) != item.partnerWinKey }
            .sortedBy { Math.abs((it.startMinute + it.endMinute) / 2 - p2Birth) }

          var rescuedGap: ScheduleGap? = null
          for (win in distinctWindows) {
            rescuedGap = tryEnsureWindowRoomForTwinPart(
              win = win,
              minNeededBlock = floor2 + b2,
              anchorGaps = anchorGaps,
              dayAnchors = dayAnchors,
              activeDay = activeDay,
              scheduledTasks = scheduledTasks,
              interTaskBufferMinutes = interTaskBufferMinutes,
              excludedWinKey = null
            )
            if (rescuedGap != null) break
          }

          if (rescuedGap != null) {
            val cap2 = (rescuedGap.durationMinutes - b2).coerceAtLeast(floor2)
            val partnerIdx = scheduledTasks.indexOfFirst {
              it.fluidTwinId == p2.fluidTwinId && it.id != p2.id
            }
            val partner = if (partnerIdx >= 0) scheduledTasks[partnerIdx] else null
            val pool = p2.fluidTotalPoolMinutes ?: ((partner?.let { birthDurationOf(it) } ?: 0) + p2BirthDur)
            val d1Current = partner?.durationMinutes ?: 0
            val d2 = (pool - d1Current).coerceIn(floor2, cap2)
            val maxStart2 = (rescuedGap.endMinute - (d2 + b2)).coerceAtLeast(rescuedGap.startMinute)
            val start2 = when {
              p2Birth in rescuedGap.startMinute..maxStart2 -> p2Birth
              p2Birth < rescuedGap.startMinute -> rescuedGap.startMinute
              else -> maxStart2
            }
            scheduledTasks.add(
              p2.copy(
                startTimeMinute = start2,
                originalStartTimeMinute = p2Birth,
                durationMinutes = d2,
                originalDurationMinutes = p2.originalDurationMinutes ?: p2BirthDur,
                isSplitPart = false,
                isOverflow = false
              )
            )
          } else {
            overflowTasks.add(
              p2.copy(
                originalStartTimeMinute = p2Birth,
                durationMinutes = p2BirthDur,
                originalDurationMinutes = p2.originalDurationMinutes ?: p2BirthDur,
                isOverflow = true,
                overflowReason = "Twin parts must stay in separate anchor gaps; no other open gap available for Part 2."
              )
            )
          }
        }

        is PendingItem.TwinPair -> {
          val p1 = item.part1
          val p2 = item.part2
          val p1Birth = p1.originalStartTimeMinute ?: p1.startTimeMinute
          val p2Birth = p2.originalStartTimeMinute ?: p2.startTimeMinute
          val p1BirthDur = birthDurationOf(p1)
          val p2BirthDur = birthDurationOf(p2)
          val pool = p1.fluidTotalPoolMinutes ?: (p1BirthDur + p2BirthDur)
          val floor1 = if (p1BirthDur <= 0) 0 else p1.fluidSafetyFloorMinutes.coerceIn(1, p1BirthDur)
          val floor2 = if (p2BirthDur <= 0) 0 else p2.fluidSafetyFloorMinutes.coerceIn(1, p2BirthDur)
          val b1 = p1.interTaskBufferMinutes.coerceIn(0, 30)
          val b2 = p2.interTaskBufferMinutes.coerceIn(0, 30)

          val p1Wins = anchorGaps.sortedBy { Math.abs((it.startMinute + it.endMinute) / 2 - p1Birth) }
          var gap1: ScheduleGap? = null
          for (win in p1Wins) {
            gap1 = tryEnsureWindowRoomForTwinPart(win, floor1 + b1, anchorGaps, dayAnchors, activeDay, scheduledTasks, interTaskBufferMinutes)
            if (gap1 != null) break
          }

          if (gap1 == null) {
            overflowTasks.add(
              p1.copy(
                originalStartTimeMinute = p1Birth,
                durationMinutes = p1BirthDur,
                originalDurationMinutes = p1.originalDurationMinutes ?: p1BirthDur,
                isOverflow = true,
                overflowReason = "Anchor boundary prevents minimum fluid twin duration."
              )
            )
            overflowTasks.add(
              p2.copy(
                originalStartTimeMinute = p2Birth,
                durationMinutes = p2BirthDur,
                originalDurationMinutes = p2.originalDurationMinutes ?: p2BirthDur,
                isOverflow = true,
                overflowReason = "Anchor boundary prevents minimum fluid twin duration."
              )
            )
            continue
          }

          val win1Key = getInterAnchorWindowKey(dayAnchors, gap1.startMinute, activeDay)
          val cap1 = (gap1.durationMinutes - b1).coerceAtLeast(floor1)
          val p2Wins = anchorGaps
            .filter { getInterAnchorWindowKey(dayAnchors, it.startMinute, activeDay) != win1Key }
            .sortedBy { Math.abs((it.startMinute + it.endMinute) / 2 - p2Birth) }

          var gap2: ScheduleGap? = null
          for (win in p2Wins) {
            gap2 = tryEnsureWindowRoomForTwinPart(win, floor2 + b2, anchorGaps, dayAnchors, activeDay, scheduledTasks, interTaskBufferMinutes)
            if (gap2 != null) break
          }

          if (gap2 == null) {
            val d1 = p1BirthDur.coerceIn(floor1, cap1)
            val maxStart1 = (gap1.endMinute - (d1 + b1)).coerceAtLeast(gap1.startMinute)
            val start1 = when {
              p1Birth in gap1.startMinute..maxStart1 -> p1Birth
              p1Birth < gap1.startMinute -> gap1.startMinute
              else -> maxStart1
            }
            scheduledTasks.add(
              p1.copy(
                startTimeMinute = start1,
                originalStartTimeMinute = p1Birth,
                durationMinutes = d1,
                originalDurationMinutes = p1.originalDurationMinutes ?: p1BirthDur,
                isSplitPart = false,
                isOverflow = false
              )
            )
            overflowTasks.add(
              p2.copy(
                originalStartTimeMinute = p2Birth,
                durationMinutes = p2BirthDur,
                originalDurationMinutes = p2.originalDurationMinutes ?: p2BirthDur,
                isOverflow = true,
                overflowReason = "Twin parts must stay in separate anchor gaps; no other open gap available for Part 2."
              )
            )
          } else {
            val cap2 = (gap2.durationMinutes - b2).coerceAtLeast(floor2)
            var d1 = p1BirthDur.coerceIn(floor1, cap1)
            var d2 = (pool - d1).coerceAtLeast(floor2)
            if (d2 > cap2) {
              d2 = cap2
              d1 = (pool - d2).coerceIn(floor1, cap1)
            }
            if (d1 > cap1) {
              d1 = cap1
              d2 = (pool - d1).coerceIn(floor2, cap2)
            }
            val maxStart1 = (gap1.endMinute - (d1 + b1)).coerceAtLeast(gap1.startMinute)
            val start1 = when {
              p1Birth in gap1.startMinute..maxStart1 -> p1Birth
              p1Birth < gap1.startMinute -> gap1.startMinute
              else -> maxStart1
            }
            val maxStart2 = (gap2.endMinute - (d2 + b2)).coerceAtLeast(gap2.startMinute)
            val start2 = when {
              p2Birth in gap2.startMinute..maxStart2 -> p2Birth
              p2Birth < gap2.startMinute -> gap2.startMinute
              else -> maxStart2
            }
            scheduledTasks.add(
              p1.copy(
                startTimeMinute = start1,
                originalStartTimeMinute = p1Birth,
                durationMinutes = d1,
                originalDurationMinutes = p1.originalDurationMinutes ?: p1BirthDur,
                isSplitPart = false,
                isOverflow = false
              )
            )
            scheduledTasks.add(
              p2.copy(
                startTimeMinute = start2,
                originalStartTimeMinute = p2Birth,
                durationMinutes = d2,
                originalDurationMinutes = p2.originalDurationMinutes ?: p2BirthDur,
                isSplitPart = false,
                isOverflow = false
              )
            )
          }
        }
      }
    }

    scheduledTasks.sortBy { it.startTimeMinute }

    val remainingGaps = findAvailableGaps(scheduledTasks, interTaskBufferMinutes)
    val totalScheduled = scheduledTasks.sumOf { it.durationMinutes }
    val totalGaps = remainingGaps.sumOf { it.durationMinutes }

    return ScheduleResult(
      scheduledTasks = scheduledTasks,
      overflowTasks = overflowTasks,
      availableGaps = remainingGaps,
      totalScheduledMinutes = totalScheduled,
      totalGapsMinutes = totalGaps
    )
  }

  /**
   * Smart Schedule Compaction & Overflow Rescue (with Priority Displacement Rescue):
   * - Step A: Packs non-zero dynamic tasks tightly inside each inter-anchor window to merge fragmented slack.
   * - Step B: Rescues overflow tasks into open / newly merged gaps (respecting HP fencing & Twin partner exclusion),
   *           and displaces lower-priority normal tasks from a fenced High Priority window into free windows elsewhere
   *           when needed to rescue High Priority overflow tasks.
   */
  fun compactAndRescueSchedule(
    anchors: List<RoutineTask>,
    allDynamicTasks: List<RoutineTask>,
    activeDay: RoutineDay,
    interTaskBufferMinutes: Int = 0,
    baseManualOrder: Long = System.currentTimeMillis()
  ): CompactionOutcome {
    val initialResult = scheduleDay(anchors, allDynamicTasks, activeDay, interTaskBufferMinutes)
    val initialScheduledById = initialResult.scheduledTasks
      .filter { it.type == TaskType.DYNAMIC }
      .associateBy { it.id }
    val initialOverflowIds = initialResult.overflowTasks.map { it.id }.toSet()

    val dayAnchors = resolveEffectiveDayAnchors(anchors, activeDay)
    val anchorGaps = findAvailableGaps(dayAnchors, 0)
    val workingMap = allDynamicTasks.associateBy { it.id }.toMutableMap()
    var orderCursor = baseManualOrder

    // Step A: Sequential inter-anchor window compaction (skips 0-minute signal markers)
    for (win in anchorGaps) {
      val winTasks = initialResult.scheduledTasks
        .filter {
          it.type == TaskType.DYNAMIC &&
            it.durationMinutes > 0 &&
            it.startTimeMinute in win.startMinute until win.endMinute
        }
        .sortedBy { it.startTimeMinute }
      var cursor = win.startMinute
      for (task in winTasks) {
        val dur = task.durationMinutes.coerceAtLeast(0)
        val buf = task.interTaskBufferMinutes.coerceIn(0, 30)
        if (task.startTimeMinute != cursor) {
          workingMap[task.id]?.let { orig ->
            val origBirthDur = birthDurationOf(orig)
            workingMap[task.id] = orig.copy(
              startTimeMinute = cursor,
              originalStartTimeMinute = cursor,
              durationMinutes = origBirthDur,
              originalDurationMinutes = orig.originalDurationMinutes ?: origBirthDur,
              manualPlacementOrder = ++orderCursor
            )
          }
        }
        cursor += dur + buf
      }
    }

    // Step B: Overflow Rescue + Priority Displacement Rescue
    val afterStepA = scheduleDay(anchors, workingMap.values.toList(), activeDay, interTaskBufferMinutes)
    val rescueQueue = afterStepA.overflowTasks.sortedWith(
      compareByDescending<RoutineTask> { it.isHighPriority && !it.isFluidTwin }
        .thenBy { it.originalStartTimeMinute ?: it.startTimeMinute }
    )

    for (ovTask in rescueQueue) {
      val currentRes = scheduleDay(anchors, workingMap.values.toList(), activeDay, interTaskBufferMinutes)
      if (currentRes.scheduledTasks.any { it.id == ovTask.id }) continue

      val ovSource = workingMap[ovTask.id] ?: ovTask
      val ovBirth = ovSource.originalStartTimeMinute ?: ovSource.startTimeMinute
      val ovBirthDur = birthDurationOf(ovSource)
      val ovBuf = ovSource.interTaskBufferMinutes.coerceIn(0, 30)
      val ovMinDur = minAllowedDurationOf(ovSource)
      val ovFullBlock = ovBirthDur + ovBuf
      val ovMinBlock = ovMinDur + ovBuf

      val partnerWin = if (ovSource.isFluidTwin && !ovSource.fluidTwinId.isNullOrEmpty()) {
        val activePartner = currentRes.scheduledTasks.firstOrNull {
          it.id != ovSource.id &&
            it.fluidTwinId == ovSource.fluidTwinId &&
            !it.isDeleted &&
            !it.isCurrentlyPaused()
        }
        activePartner?.let { getInterAnchorWindowKey(dayAnchors, it.startTimeMinute, activeDay) }
      } else {
        null
      }

      val freeGaps = findAvailableGaps(currentRes.scheduledTasks, interTaskBufferMinutes)
      val eligibleGaps = when {
        ovSource.isHighPriority && !ovSource.isFluidTwin -> {
          val enclosing = findEnclosingAnchorBounds(anchors, ovBirth, activeDay)
          freeGaps.filter { it.startMinute >= enclosing.first && it.endMinute <= enclosing.second }
        }
        ovSource.isFluidTwin && partnerWin != null -> {
          freeGaps.filter { getInterAnchorWindowKey(dayAnchors, it.startMinute, activeDay) != partnerWin }
        }
        else -> freeGaps
      }

      val fitGap = eligibleGaps
        .filter { it.durationMinutes >= ovFullBlock }
        .minByOrNull { Math.abs((it.startMinute + it.endMinute) / 2 - ovBirth) }
        ?: eligibleGaps
          .filter { it.durationMinutes >= ovMinBlock }
          .minByOrNull { Math.abs((it.startMinute + it.endMinute) / 2 - ovBirth) }

      if (fitGap != null) {
        workingMap[ovSource.id] = ovSource.copy(
          startTimeMinute = fitGap.startMinute,
          originalStartTimeMinute = fitGap.startMinute,
          durationMinutes = ovBirthDur,
          originalDurationMinutes = ovSource.originalDurationMinutes ?: ovBirthDur,
          isOverflow = false,
          overflowReason = null,
          manualPlacementOrder = ++orderCursor
        )
        continue
      }

      // Priority Displacement Rescue for fenced High Priority overflow tasks
      if (ovSource.isHighPriority && !ovSource.isFluidTwin) {
        val enclosing = findEnclosingAnchorBounds(anchors, ovBirth, activeDay)
        val enclosingWinStart = enclosing.first
        val enclosingWinEnd = enclosing.second
        val freeInsideWin = freeGaps
          .filter { it.startMinute >= enclosingWinStart && it.endMinute <= enclosingWinEnd }
          .sumOf { it.durationMinutes }
        val neededExtra = (ovFullBlock - freeInsideWin).coerceAtLeast(0)

        if (neededExtra > 0) {
          val displaceables = currentRes.scheduledTasks
            .filter {
              it.type == TaskType.DYNAMIC &&
                !it.isHighPriority &&
                !it.isFluidTwin &&
                it.durationMinutes > 0 &&
                it.startTimeMinute in enclosingWinStart until enclosingWinEnd
            }
            .sortedBy { it.durationMinutes }

          val otherFreeMutable = freeGaps
            .filter { it.endMinute <= enclosingWinStart || it.startMinute >= enclosingWinEnd }
            .map { it.startMinute to it.endMinute }
            .toMutableList()

          val plannedDisplacements = mutableListOf<Pair<RoutineTask, Int>>()
          var freedMinutes = 0

          for (disp in displaceables) {
            if (freedMinutes >= neededExtra) break
            val dispBlock = disp.durationMinutes.coerceAtLeast(0) + disp.interTaskBufferMinutes.coerceIn(0, 30)
            val gapIdx = otherFreeMutable.indexOfFirst { (gStart, gEnd) -> (gEnd - gStart) >= dispBlock }
            if (gapIdx >= 0) {
              val (gStart, gEnd) = otherFreeMutable[gapIdx]
              plannedDisplacements.add(disp to gStart)
              freedMinutes += dispBlock
              if (gStart + dispBlock < gEnd) {
                otherFreeMutable[gapIdx] = (gStart + dispBlock) to gEnd
              } else {
                otherFreeMutable.removeAt(gapIdx)
              }
            }
          }

          if (freedMinutes >= neededExtra && plannedDisplacements.isNotEmpty()) {
            for ((dispTask, newStart) in plannedDisplacements) {
              workingMap[dispTask.id]?.let { orig ->
                val origBirthDur = birthDurationOf(orig)
                workingMap[dispTask.id] = orig.copy(
                  startTimeMinute = newStart,
                  originalStartTimeMinute = newStart,
                  durationMinutes = origBirthDur,
                  originalDurationMinutes = orig.originalDurationMinutes ?: origBirthDur,
                  manualPlacementOrder = ++orderCursor
                )
              }
            }
            val remainingInWin = currentRes.scheduledTasks
              .filter {
                it.type == TaskType.DYNAMIC &&
                  it.durationMinutes > 0 &&
                  it.startTimeMinute in enclosingWinStart until enclosingWinEnd &&
                  plannedDisplacements.none { (d, _) -> d.id == it.id }
              }
              .sortedBy { it.startTimeMinute }
            var winCursor = enclosingWinStart
            for (rem in remainingInWin) {
              workingMap[rem.id]?.let { orig ->
                if (orig.startTimeMinute != winCursor) {
                  val origBirthDur = birthDurationOf(orig)
                  workingMap[rem.id] = orig.copy(
                    startTimeMinute = winCursor,
                    originalStartTimeMinute = winCursor,
                    durationMinutes = origBirthDur,
                    originalDurationMinutes = orig.originalDurationMinutes ?: origBirthDur,
                    manualPlacementOrder = ++orderCursor
                  )
                }
              }
              winCursor += rem.durationMinutes.coerceAtLeast(0) + rem.interTaskBufferMinutes.coerceIn(0, 30)
            }
            if (enclosingWinEnd - winCursor >= ovFullBlock) {
              workingMap[ovSource.id] = ovSource.copy(
                startTimeMinute = winCursor,
                originalStartTimeMinute = winCursor,
                durationMinutes = ovBirthDur,
                originalDurationMinutes = ovSource.originalDurationMinutes ?: ovBirthDur,
                isOverflow = false,
                overflowReason = null,
                manualPlacementOrder = ++orderCursor
              )
            }
          }
        }
      }
    }

    val candidateList = allDynamicTasks.map { workingMap[it.id] ?: it }
    val finalResult = scheduleDay(anchors, candidateList, activeDay, interTaskBufferMinutes)
    val finalOverflowIds = finalResult.overflowTasks.map { it.id }.toSet()
    val rescuedCount = initialOverflowIds.count { it !in finalOverflowIds }
    val movedCount = finalResult.scheduledTasks.count { task ->
      task.type == TaskType.DYNAMIC &&
        task.id !in initialOverflowIds &&
        initialScheduledById[task.id]?.startTimeMinute != task.startTimeMinute
    }
    val flexedCount = finalResult.scheduledTasks.count { task ->
      task.type == TaskType.DYNAMIC &&
        task.id !in initialOverflowIds &&
        initialScheduledById[task.id]?.durationMinutes != task.durationMinutes
    }

    if (movedCount == 0 && rescuedCount == 0 && flexedCount == 0) {
      return CompactionOutcome(
        updatedDynamicTasks = allDynamicTasks,
        scheduleResult = initialResult,
        movedCount = 0,
        rescuedCount = 0,
        flexedCount = 0
      )
    }

    return CompactionOutcome(
      updatedDynamicTasks = candidateList,
      scheduleResult = finalResult,
      movedCount = movedCount,
      rescuedCount = rescuedCount,
      flexedCount = flexedCount
    )
  }

  /**
   * Resolves effective anchor tasks for [day] (accounting for active-day visibility, pause state,
   * and Friday Jumu'ah override on Slot 3).
   */
  private fun resolveEffectiveDayAnchors(anchors: List<RoutineTask>, day: RoutineDay): List<RoutineTask> {
    val isFriday = day == RoutineDay.FRIDAY
    return anchors
      .filter { !it.isDeleted && !it.isCurrentlyPaused() && it.selectedDays.contains(day) }
      .map { anchor ->
        if (isFriday && anchor.slotNumber == 3) {
          anchor.copy(
            title = anchor.fridayTitle?.ifBlank { null } ?: anchor.title,
            startTimeMinute = anchor.fridayStartTimeMinute ?: anchor.startTimeMinute,
            durationMinutes = anchor.fridayDurationMinutes ?: anchor.durationMinutes,
            voiceMessage = anchor.fridayVoiceMessage?.ifBlank { null } ?: anchor.voiceMessage
          )
        } else {
          anchor
        }
      }
  }

  /**
   * Returns a canonical inter-anchor window key for [minute] on [day].
   * Respects Friday Slot 3 overrides, inactive/paused anchors (e.g. Slot 5 disabled),
   * and midnight-contiguous wrap-around.
   */
  fun getInterAnchorWindowKey(
    anchors: List<RoutineTask>,
    minute: Int,
    day: RoutineDay = RoutineDay.today()
  ): Int {
    val dayAnchors = resolveEffectiveDayAnchors(anchors, day)
    val anchorGaps = findAvailableGaps(dayAnchors, 0)
    if (anchorGaps.isEmpty()) return 0

    val safeMin = ((minute % 1440) + 1440) % 1440
    val containing = anchorGaps.firstOrNull { safeMin in it.startMinute until it.endMinute }
      ?: anchorGaps.firstOrNull { safeMin in it.startMinute..it.endMinute }
      ?: anchorGaps.minByOrNull { Math.abs(safeMin - (it.startMinute + it.endMinute) / 2) }
      ?: anchorGaps.first()

    val wrapsAroundMidnight = anchorGaps.size > 1 &&
      anchorGaps.first().startMinute == 0 &&
      anchorGaps.last().endMinute == 1440

    return if (wrapsAroundMidnight && containing == anchorGaps.last()) {
      anchorGaps.first().startMinute
    } else {
      containing.startMinute
    }
  }

  private fun findActiveTwinPartnerWindow(
    anchors: List<RoutineTask>,
    allDynamicTasks: List<RoutineTask>,
    scheduledTasks: List<RoutineTask>?,
    task: RoutineTask,
    day: RoutineDay
  ): Int? {
    if (!task.isFluidTwin || task.fluidTwinId.isNullOrEmpty()) return null
    val partner = allDynamicTasks.firstOrNull {
      it.id != task.id &&
        it.fluidTwinId == task.fluidTwinId &&
        !it.isDeleted &&
        !it.isCurrentlyPaused() &&
        it.selectedDays.contains(day)
    } ?: return null
    val partnerMin = scheduledTasks?.firstOrNull { it.id == partner.id }?.startTimeMinute
      ?: partner.startTimeMinute
    return getInterAnchorWindowKey(anchors, partnerMin, day)
  }

  /**
   * Finds the open anchor gap (startMinute..endMinute within 0..1440) enclosing [targetMinute].
   */
  fun findEnclosingAnchorBounds(
    anchors: List<RoutineTask>,
    targetMinute: Int,
    day: RoutineDay = RoutineDay.today()
  ): Pair<Int, Int> {
    val dayAnchors = resolveEffectiveDayAnchors(anchors, day)
    val anchorGaps = findAvailableGaps(dayAnchors, 0)
    if (anchorGaps.isEmpty()) return 0 to 1440

    val safeMin = ((targetMinute % 1440) + 1440) % 1440
    val containing = anchorGaps.firstOrNull { safeMin in it.startMinute until it.endMinute }
      ?: anchorGaps.firstOrNull { safeMin in it.startMinute..it.endMinute }
      ?: anchorGaps.minByOrNull { Math.abs(safeMin - (it.startMinute + it.endMinute) / 2) }
      ?: anchorGaps.first()

    return containing.startMinute to containing.endMinute
  }

  /**
   * Computes exact horizontal drag scrub bounds [minStart..maxStart] for [task] inside its
   * current gap, protecting enclosing anchor walls and adjacent scheduled dynamic tasks
   * (including their inter-task rest buffers).
   */
  fun computeHorizontalScrubBounds(
    task: RoutineTask,
    scheduledTasks: List<RoutineTask>,
    anchors: List<RoutineTask>,
    day: RoutineDay = RoutineDay.today()
  ): IntRange {
    val anchorBounds = findEnclosingAnchorBounds(anchors, task.startTimeMinute, day)
    val otherTasks = scheduledTasks.filter {
      it.id != task.id && it.type == TaskType.DYNAMIC
    }
    val dayAnchors = resolveEffectiveDayAnchors(anchors, day)
    val freeGaps = findAvailableGaps(dayAnchors + otherTasks, 0)

    val taskStart = ((task.startTimeMinute % 1440) + 1440) % 1440
    val containingGap = freeGaps.firstOrNull { taskStart in it.startMinute until it.endMinute }
      ?: freeGaps.firstOrNull { taskStart in it.startMinute..it.endMinute }
      ?: return taskStart..taskStart

    val minStart = maxOf(containingGap.startMinute, anchorBounds.first)
    val maxEnd = minOf(containingGap.endMinute, anchorBounds.second)

    val taskBuffer = if (task.type == TaskType.DYNAMIC) task.interTaskBufferMinutes.coerceIn(0, 30) else 0
    val block = task.durationMinutes.coerceAtLeast(0) + taskBuffer
    val maxStart = if (block == 0) {
      (maxEnd - 1).coerceAtLeast(minStart)
    } else {
      (maxEnd - block).coerceAtLeast(minStart)
    }
    return minStart..maxStart
  }

  /**
   * Pure reposition operation for drag scrubbing / vertical reordering.
   * Returns Pair(updatedDynamicTasks, hitAnchorWall).
   * - High Priority non-twin tasks are strictly fenced inside their enclosing anchor gap.
   * - Fluid Twin tasks can cross anchor walls freely UNLESS their active twin partner occupies
   *   the destination inter-anchor window (Same-Gap Partner Exclusion).
   * - Normal Dynamic tasks can cross anchor walls vertically to reposition into other day gaps.
   */
  fun applyDragReposition(
    allDynamicTasks: List<RoutineTask>,
    taskId: String,
    targetStartMinute: Int,
    newManualOrder: Long,
    anchors: List<RoutineTask>,
    day: RoutineDay = RoutineDay.today()
  ): Pair<List<RoutineTask>, Boolean> {
    val target = allDynamicTasks.firstOrNull { it.id == taskId } ?: return allDynamicTasks to false
    var hitAnchorWall = false
    val clampedGlobalStart = targetStartMinute.coerceIn(0, 1439)
    val birthDur = birthDurationOf(target)
    val minDur = minAllowedDurationOf(target)
    val targetBuf = target.interTaskBufferMinutes.coerceIn(0, 30)

    val finalStart = if (target.isHighPriority && !target.isFluidTwin) {
      val currentAnchorMinute = target.originalStartTimeMinute ?: target.startTimeMinute
      val (gapStart, gapEnd) = findEnclosingAnchorBounds(anchors, currentAnchorMinute, day)
      val block = birthDur + targetBuf
      val maxAllowedStart = (gapEnd - block).coerceAtLeast(gapStart)

      if (clampedGlobalStart < gapStart || clampedGlobalStart > maxAllowedStart) {
        hitAnchorWall = true
      }
      clampedGlobalStart.coerceIn(gapStart, maxAllowedStart)
    } else {
      val dayAnchors = resolveEffectiveDayAnchors(anchors, day)
      val anchorGaps = findAvailableGaps(dayAnchors, 0)
      val fullBlock = birthDur + targetBuf
      val minBlock = minDur + targetBuf

      val currentAnchorMinute = target.originalStartTimeMinute ?: target.startTimeMinute
      val currentWinKey = getInterAnchorWindowKey(anchors, currentAnchorMinute, day)
      val partnerWinKey = findActiveTwinPartnerWindow(anchors, allDynamicTasks, null, target, day)

      val candidateGaps = if (target.isFluidTwin && partnerWinKey != null && currentWinKey != partnerWinKey) {
        anchorGaps.filter { getInterAnchorWindowKey(anchors, it.startMinute, day) != partnerWinKey }
      } else {
        anchorGaps
      }

      val insideGap = anchorGaps.firstOrNull { clampedGlobalStart in it.startMinute until it.endMinute }
      val destWinKey = insideGap?.let { getInterAnchorWindowKey(anchors, it.startMinute, day) }
        ?: getInterAnchorWindowKey(anchors, clampedGlobalStart, day)

      if (target.isFluidTwin && partnerWinKey != null && destWinKey == partnerWinKey && currentWinKey != partnerWinKey) {
        // Blocked by Same-Gap Partner Exclusion -> fence at current enclosing anchor boundary
        hitAnchorWall = true
        val (gapStart, gapEnd) = findEnclosingAnchorBounds(anchors, currentAnchorMinute, day)
        val maxAllowedStart = (gapEnd - minBlock).coerceAtLeast(gapStart)
        clampedGlobalStart.coerceIn(gapStart, maxAllowedStart)
      } else if (insideGap != null && candidateGaps.any { it.startMinute == insideGap.startMinute }) {
        val gapBlock = if ((target.isFluidTwin || effectiveFlexOf(target) > 0) && insideGap.durationMinutes < fullBlock) minBlock else fullBlock
        val maxInGap = (insideGap.endMinute - gapBlock).coerceAtLeast(insideGap.startMinute)
        clampedGlobalStart.coerceIn(insideGap.startMinute, maxInGap)
      } else {
        val pool = if (candidateGaps.isNotEmpty()) candidateGaps else anchorGaps
        val viableGap = pool.filter { it.durationMinutes >= minBlock }
          .minByOrNull { gap ->
            val gapBlock = if ((target.isFluidTwin || effectiveFlexOf(target) > 0) && gap.durationMinutes < fullBlock) minBlock else fullBlock
            val maxS = (gap.endMinute - gapBlock).coerceAtLeast(gap.startMinute)
            val edge = clampedGlobalStart.coerceIn(gap.startMinute, maxS)
            Math.abs(edge - clampedGlobalStart)
          } ?: pool.minByOrNull { gap ->
          Math.abs(clampedGlobalStart.coerceIn(gap.startMinute, gap.endMinute) - clampedGlobalStart)
        }
        if (viableGap != null) {
          val gapBlock = if ((target.isFluidTwin || effectiveFlexOf(target) > 0) && viableGap.durationMinutes < fullBlock) minBlock else fullBlock
          val maxS = (viableGap.endMinute - gapBlock).coerceAtLeast(viableGap.startMinute)
          clampedGlobalStart.coerceIn(viableGap.startMinute, maxS)
        } else {
          clampedGlobalStart
        }
      }
    }

    val updated = allDynamicTasks.map { task ->
      if (task.id == taskId) {
        task.copy(
          startTimeMinute = finalStart,
          originalStartTimeMinute = finalStart,
          durationMinutes = birthDur,
          originalDurationMinutes = task.originalDurationMinutes ?: birthDur,
          manualPlacementOrder = newManualOrder
        )
      } else {
        task
      }
    }
    return updated to hitAnchorWall
  }

  /**
   * Computes structural timeline impact diffs between [baseline] and [draft] for dynamic tasks.
   */
  fun computeScheduleImpactDiff(
    baseline: ScheduleResult,
    draft: ScheduleResult
  ): List<ScheduleImpactItem> {
    val baseScheduled = baseline.scheduledTasks.filter { it.type == TaskType.DYNAMIC }.associateBy { it.id }
    val baseOverflow = baseline.overflowTasks.filter { it.type == TaskType.DYNAMIC }.associateBy { it.id }
    val draftScheduled = draft.scheduledTasks.filter { it.type == TaskType.DYNAMIC }.associateBy { it.id }
    val draftOverflow = draft.overflowTasks.filter { it.type == TaskType.DYNAMIC }.associateBy { it.id }

    val allIds = (baseScheduled.keys + baseOverflow.keys + draftScheduled.keys + draftOverflow.keys).distinct()
    val impacts = mutableListOf<ScheduleImpactItem>()

    for (id in allIds) {
      val bTask = baseScheduled[id] ?: baseOverflow[id]
      val dTask = draftScheduled[id] ?: draftOverflow[id]
      if (bTask == null && dTask == null) continue

      val oldInOverflow = baseOverflow.containsKey(id)
      val newInOverflow = draftOverflow.containsKey(id)
      val oldStart = if (oldInOverflow || bTask == null) null else bTask.startTimeMinute
      val newStart = if (newInOverflow || dTask == null) null else dTask.startTimeMinute
      val oldDur = bTask?.durationMinutes ?: 0
      val newDur = dTask?.durationMinutes ?: 0
      val title = dTask?.title ?: bTask?.title ?: continue

      if (oldStart == newStart && oldDur == newDur && oldInOverflow == newInOverflow) {
        continue
      }

      val statusChange = when {
        !oldInOverflow && newInOverflow -> "Moved to Overflow"
        oldInOverflow && !newInOverflow && newStart != null && oldDur != newDur ->
          "Recovered at ${RoutineTask.formatTime(newStart)} (${oldDur}m → ${newDur}m)"
        oldInOverflow && !newInOverflow && newStart != null ->
          "Recovered at ${RoutineTask.formatTime(newStart)}"
        oldStart != null && newStart != null && oldStart != newStart && oldDur != newDur ->
          "Shifted ${RoutineTask.formatTime(oldStart)} → ${RoutineTask.formatTime(newStart)} (${oldDur}m → ${newDur}m)"
        oldStart != null && newStart != null && oldStart != newStart ->
          "Shifted ${RoutineTask.formatTime(oldStart)} → ${RoutineTask.formatTime(newStart)}"
        oldDur != newDur ->
          "Duration ${oldDur}m → ${newDur}m"
        else -> "Repositioned"
      }

      impacts.add(
        ScheduleImpactItem(
          taskId = id,
          title = title,
          oldStartMinute = oldStart,
          newStartMinute = newStart,
          oldDurationMinutes = oldDur,
          newDurationMinutes = newDur,
          statusChange = statusChange
        )
      )
    }

    return impacts.sortedBy { it.newStartMinute ?: it.oldStartMinute ?: Int.MAX_VALUE }
  }

  /**
   * Computes net free gap minutes between [task] and [nextTask] after subtracting [task]'s
   * reserved inter-task buffer time. Returns 0 when the separation is completely covered by buffer.
   */
  fun computeNetFreeGapMinutes(task: RoutineTask, nextTask: RoutineTask): Int {
    val prevBuffer = if (task.type == TaskType.DYNAMIC) task.interTaskBufferMinutes.coerceIn(0, 30) else 0
    val effectivePrevEnd = task.startTimeMinute + task.durationMinutes + prevBuffer
    if (effectivePrevEnd >= 1440 || nextTask.startTimeMinute <= effectivePrevEnd) return 0
    return nextTask.startTimeMinute - effectivePrevEnd
  }

  /**
   * Smart task swapping and cascading reposition for launcher-style vertical drops.
   * Returns Pair(updatedDynamicTasks, hitAnchorWall).
   */
  fun applyVerticalDropReposition(
    allDynamicTasks: List<RoutineTask>,
    scheduledTasks: List<RoutineTask>,
    taskId: String,
    dropTarget: VerticalDropTarget,
    baseManualOrder: Long,
    anchors: List<RoutineTask>,
    day: RoutineDay = RoutineDay.today()
  ): Pair<List<RoutineTask>, Boolean> {
    val draggedSource = allDynamicTasks.firstOrNull { it.id == taskId } ?: return allDynamicTasks to false
    val draggedScheduled = scheduledTasks.firstOrNull { it.id == taskId }
    val isStrictlyFenced = draggedSource.isHighPriority && !draggedSource.isFluidTwin
    val draggedCurrStart = draggedScheduled?.startTimeMinute
      ?: draggedSource.originalStartTimeMinute
      ?: draggedSource.startTimeMinute
    val draggedGap = findEnclosingAnchorBounds(anchors, draggedCurrStart, day)
    val draggedWinKey = getInterAnchorWindowKey(anchors, draggedCurrStart, day)
    val partnerWinKey = findActiveTwinPartnerWindow(anchors, allDynamicTasks, scheduledTasks, draggedSource, day)
    val draggedBirthDur = birthDurationOf(draggedSource)
    val minDraggedDur = minAllowedDurationOf(draggedSource)
    val draggedBuf = draggedSource.interTaskBufferMinutes.coerceIn(0, 30)
    val draggedMinBlock = minDraggedDur + draggedBuf
    val draggedFullBlock = draggedBirthDur + draggedBuf

    when (dropTarget) {
      is VerticalDropTarget.IntoGap -> {
        return applyDragReposition(
          allDynamicTasks = allDynamicTasks,
          taskId = taskId,
          targetStartMinute = dropTarget.targetStartMinute,
          newManualOrder = baseManualOrder + 100L,
          anchors = anchors,
          day = day
        )
      }

      is VerticalDropTarget.InsertBeforeTask,
      is VerticalDropTarget.InsertAfterTask -> {
        val targetTaskId = when (dropTarget) {
          is VerticalDropTarget.InsertBeforeTask -> dropTarget.targetTaskId
          is VerticalDropTarget.InsertAfterTask -> dropTarget.targetTaskId
          is VerticalDropTarget.IntoGap -> return allDynamicTasks to false
        }
        if (targetTaskId == taskId) return allDynamicTasks to false
        val targetScheduled = scheduledTasks.firstOrNull { it.id == targetTaskId }
          ?: return allDynamicTasks to false

        if (targetScheduled.type != TaskType.DYNAMIC) {
          val rawProposed = if (dropTarget is VerticalDropTarget.InsertBeforeTask) {
            (targetScheduled.startTimeMinute - draggedFullBlock).coerceAtLeast(0)
          } else {
            (targetScheduled.startTimeMinute + targetScheduled.durationMinutes) % 1440
          }
          return applyDragReposition(
            allDynamicTasks = allDynamicTasks,
            taskId = taskId,
            targetStartMinute = rawProposed,
            newManualOrder = baseManualOrder + 100L,
            anchors = anchors,
            day = day
          )
        }

        val targetGap = findEnclosingAnchorBounds(anchors, targetScheduled.startTimeMinute, day)
        val targetWinKey = getInterAnchorWindowKey(anchors, targetScheduled.startTimeMinute, day)
        val isBlockedByAnchorWall = isStrictlyFenced && targetGap != draggedGap
        val isBlockedByTwinPartner = draggedSource.isFluidTwin &&
          partnerWinKey != null &&
          targetWinKey == partnerWinKey &&
          draggedWinKey != partnerWinKey

        if (isBlockedByAnchorWall || isBlockedByTwinPartner) {
          val clampedEdge = if (targetScheduled.startTimeMinute < draggedGap.first) {
            draggedGap.first
          } else {
            (draggedGap.second - draggedMinBlock).coerceAtLeast(draggedGap.first)
          }
          val (updated, _) = applyDragReposition(
            allDynamicTasks = allDynamicTasks,
            taskId = taskId,
            targetStartMinute = clampedEdge,
            newManualOrder = baseManualOrder + 100L,
            anchors = anchors,
            day = day
          )
          return updated to true
        }

        // Case A: Same Enclosing Anchor Gap -> in-gap reordering & intuitive swap
        if (draggedGap == targetGap) {
          val (gapStart, gapEnd) = draggedGap
          val gapDynamicScheduled = scheduledTasks
            .filter { it.type == TaskType.DYNAMIC && it.startTimeMinute >= gapStart && it.startTimeMinute < gapEnd }
            .sortedBy { it.startTimeMinute }

          val oldDraggedIdx = gapDynamicScheduled.indexOfFirst { it.id == taskId }
          val oldTargetIdx = gapDynamicScheduled.indexOfFirst { it.id == targetTaskId }
          if (oldDraggedIdx >= 0 && oldTargetIdx >= 0) {
            val draggedItem = gapDynamicScheduled[oldDraggedIdx]
            val withoutDragged = gapDynamicScheduled.filterNot { it.id == taskId }
            val targetPos = withoutDragged.indexOfFirst { it.id == targetTaskId }
            val insertPos = if (dropTarget is VerticalDropTarget.InsertBeforeTask) targetPos else targetPos + 1
            val reordered = withoutDragged.toMutableList().apply { add(insertPos, draggedItem) }

            val updatesById = mutableMapOf<String, Pair<Int, Long>>()

            if (oldDraggedIdx < oldTargetIdx) {
              // Dragged DOWN toward targetScheduled
              if (dropTarget is VerticalDropTarget.InsertBeforeTask && oldTargetIdx == oldDraggedIdx + 1) {
                val draggedEnd = draggedItem.startTimeMinute +
                  draggedItem.durationMinutes.coerceAtLeast(0) +
                  draggedItem.interTaskBufferMinutes.coerceIn(0, 30)
                if (targetScheduled.startTimeMinute - draggedEnd <= 15) {
                  // Adjacent contiguous neighbor with no wide gap: Eat -> InsertBefore(Bath) is already a no-op
                  return allDynamicTasks to false
                }
              }

              // Check if consecutive tasks across [oldDraggedIdx..oldTargetIdx] are contiguous (<= 15m)
              var isContiguousSlice = true
              for (i in oldDraggedIdx until oldTargetIdx) {
                val prev = gapDynamicScheduled[i]
                val next = gapDynamicScheduled[i + 1]
                val prevEnd = prev.startTimeMinute + prev.durationMinutes.coerceAtLeast(0) + prev.interTaskBufferMinutes.coerceIn(0, 30)
                if (next.startTimeMinute - prevEnd > 15) {
                  isContiguousSlice = false
                  break
                }
              }

              if (isContiguousSlice) {
                val affectedStartIdx = oldDraggedIdx
                var cursor = gapDynamicScheduled[affectedStartIdx].startTimeMinute
                for (i in affectedStartIdx until reordered.size) {
                  val item = reordered[i]
                  val buf = item.interTaskBufferMinutes.coerceIn(0, 30)
                  val dur = item.durationMinutes.coerceAtLeast(0)
                  val shouldPlaceAtCursor = (i <= insertPos) || (item.startTimeMinute < cursor)
                  if (shouldPlaceAtCursor) {
                    val safeStart = if (item.isHighPriority || item.isFluidTwin) {
                      cursor.coerceIn(gapStart, (gapEnd - 1).coerceAtLeast(gapStart))
                    } else {
                      cursor.coerceIn(0, 1439)
                    }
                    updatesById[item.id] = safeStart to (baseManualOrder + 100L - (i - affectedStartIdx))
                    cursor = safeStart + dur + buf
                  } else {
                    cursor = maxOf(cursor, item.startTimeMinute + dur + buf)
                  }
                }
              } else if (dropTarget is VerticalDropTarget.InsertBeforeTask) {
                // Wide open gap (> 15m) between oldDraggedIdx and targetScheduled, dragged DOWN to InsertBeforeTask(targetScheduled)
                val prevBeforeTarget = if (targetPos > 0) withoutDragged[targetPos - 1] else null
                val earliestAfterPrev = if (prevBeforeTarget != null) {
                  prevBeforeTarget.startTimeMinute +
                    prevBeforeTarget.durationMinutes.coerceAtLeast(0) +
                    prevBeforeTarget.interTaskBufferMinutes.coerceIn(0, 30)
                } else {
                  gapStart
                }
                val openRoomBeforeTarget = targetScheduled.startTimeMinute - earliestAfterPrev
                if (openRoomBeforeTarget >= draggedFullBlock) {
                  val placedStart = (targetScheduled.startTimeMinute - draggedFullBlock)
                    .coerceIn(gapStart, (gapEnd - draggedMinBlock).coerceAtLeast(gapStart))
                  updatesById[draggedItem.id] = placedStart to (baseManualOrder + 100L)
                } else {
                  var cursor = targetScheduled.startTimeMinute
                    .coerceIn(gapStart, (gapEnd - draggedMinBlock).coerceAtLeast(gapStart))
                  for (i in insertPos until reordered.size) {
                    val item = reordered[i]
                    val buf = item.interTaskBufferMinutes.coerceIn(0, 30)
                    val dur = item.durationMinutes.coerceAtLeast(0)
                    val shouldPlaceAtCursor = (i == insertPos) || (item.startTimeMinute < cursor)
                    if (shouldPlaceAtCursor) {
                      val safeStart = if (item.isHighPriority || item.isFluidTwin) {
                        cursor.coerceIn(gapStart, (gapEnd - 1).coerceAtLeast(gapStart))
                      } else {
                        cursor.coerceIn(0, 1439)
                      }
                      updatesById[item.id] = safeStart to (baseManualOrder + 100L - (i - insertPos))
                      cursor = safeStart + dur + buf
                    } else {
                      cursor = maxOf(cursor, item.startTimeMinute + dur + buf)
                    }
                  }
                }
              } else {
                // Non-contiguous jump down onto InsertAfterTask(targetScheduled)
                val targetBuf = targetScheduled.interTaskBufferMinutes.coerceIn(0, 30)
                var cursor = (targetScheduled.startTimeMinute + targetScheduled.durationMinutes + targetBuf)
                  .coerceIn(gapStart, (gapEnd - draggedMinBlock).coerceAtLeast(gapStart))
                for (i in insertPos until reordered.size) {
                  val item = reordered[i]
                  val buf = item.interTaskBufferMinutes.coerceIn(0, 30)
                  val dur = item.durationMinutes.coerceAtLeast(0)
                  val shouldPlaceAtCursor = (i == insertPos) || (item.startTimeMinute < cursor)
                  if (shouldPlaceAtCursor) {
                    val safeStart = if (item.isHighPriority || item.isFluidTwin) {
                      cursor.coerceIn(gapStart, (gapEnd - 1).coerceAtLeast(gapStart))
                    } else {
                      cursor.coerceIn(0, 1439)
                    }
                    updatesById[item.id] = safeStart to (baseManualOrder + 100L - (i - insertPos))
                    cursor = safeStart + dur + buf
                  } else {
                    cursor = maxOf(cursor, item.startTimeMinute + dur + buf)
                  }
                }
              }
            } else {
              // Dragged UP toward earlier targetScheduled
              if (dropTarget is VerticalDropTarget.InsertAfterTask && oldTargetIdx == oldDraggedIdx - 1) {
                val targetEnd = targetScheduled.startTimeMinute +
                  targetScheduled.durationMinutes.coerceAtLeast(0) +
                  targetScheduled.interTaskBufferMinutes.coerceIn(0, 30)
                if (draggedItem.startTimeMinute - targetEnd <= 15) {
                  return allDynamicTasks to false
                }
              }

              val affectedStartIdx = insertPos
              val targetAnchorStart = if (dropTarget is VerticalDropTarget.InsertBeforeTask) {
                targetScheduled.startTimeMinute
              } else {
                val targetBuf = targetScheduled.interTaskBufferMinutes.coerceIn(0, 30)
                targetScheduled.startTimeMinute + targetScheduled.durationMinutes + targetBuf
              }
              val oldFirstStart = gapDynamicScheduled[affectedStartIdx].startTimeMinute
              var cursor = if (dropTarget is VerticalDropTarget.InsertAfterTask &&
                draggedItem.startTimeMinute - targetAnchorStart > 15
              ) {
                targetAnchorStart.coerceIn(gapStart, (gapEnd - 1).coerceAtLeast(gapStart))
              } else {
                minOf(oldFirstStart, targetAnchorStart).coerceIn(gapStart, (gapEnd - 1).coerceAtLeast(gapStart))
              }

              for (i in affectedStartIdx until reordered.size) {
                val item = reordered[i]
                val buf = item.interTaskBufferMinutes.coerceIn(0, 30)
                val dur = item.durationMinutes.coerceAtLeast(0)
                val shouldPlaceAtCursor = (i == affectedStartIdx) || (item.startTimeMinute < cursor)
                if (shouldPlaceAtCursor) {
                  val safeStart = if (item.isHighPriority || item.isFluidTwin) {
                    cursor.coerceIn(gapStart, (gapEnd - 1).coerceAtLeast(gapStart))
                  } else {
                    cursor.coerceIn(0, 1439)
                  }
                  updatesById[item.id] = safeStart to (baseManualOrder + 100L - (i - affectedStartIdx))
                  cursor = safeStart + dur + buf
                } else {
                  cursor = maxOf(cursor, item.startTimeMinute + dur + buf)
                }
              }
            }

            if (updatesById.isNotEmpty()) {
              val updated = allDynamicTasks.map { task ->
                val pair = updatesById[task.id]
                if (pair != null) {
                  val bDur = birthDurationOf(task)
                  task.copy(
                    startTimeMinute = pair.first,
                    originalStartTimeMinute = pair.first,
                    durationMinutes = bDur,
                    originalDurationMinutes = task.originalDurationMinutes ?: bDur,
                    manualPlacementOrder = pair.second
                  )
                } else {
                  task
                }
              }
              return updated to false
            }
          }
        }

        // Case B: Cross-Anchor Drop (Normal or Fluid Twin task moved into a distinct anchor gap)
        val (targetGapStart, targetGapEnd) = targetGap
        val rawStart = if (dropTarget is VerticalDropTarget.InsertBeforeTask) {
          targetScheduled.startTimeMinute
        } else {
          targetScheduled.startTimeMinute + targetScheduled.durationMinutes + targetScheduled.interTaskBufferMinutes.coerceIn(0, 30)
        }
        val effectiveTargetBlock = if ((draggedSource.isFluidTwin || effectiveFlexOf(draggedSource) > 0) && (targetGapEnd - targetGapStart) < draggedFullBlock) {
          draggedMinBlock
        } else {
          draggedFullBlock
        }
        val maxInTargetGap = (targetGapEnd - effectiveTargetBlock).coerceAtLeast(targetGapStart)
        val clampedStart = rawStart.coerceIn(targetGapStart, maxInTargetGap)

        val updated = allDynamicTasks.map { task ->
          if (task.id == taskId) {
            val bDur = birthDurationOf(task)
            task.copy(
              startTimeMinute = clampedStart,
              originalStartTimeMinute = clampedStart,
              durationMinutes = bDur,
              originalDurationMinutes = task.originalDurationMinutes ?: bDur,
              manualPlacementOrder = baseManualOrder + 100L
            )
          } else {
            task
          }
        }
        return updated to false
      }
    }
  }

  /**
   * Computes exact preview start/end minutes and Anchor Wall flag for the glowing drop shadow
   * using the exact same rules as [applyVerticalDropReposition], including recovered birth duration
   * when hovering a spacious target anchor window.
   */
  fun previewVerticalDrop(
    allDynamicTasks: List<RoutineTask>,
    scheduledTasks: List<RoutineTask>,
    taskId: String,
    dropTarget: VerticalDropTarget,
    anchors: List<RoutineTask>,
    day: RoutineDay = RoutineDay.today()
  ): VerticalDropPreview {
    val dragged = allDynamicTasks.firstOrNull { it.id == taskId }
      ?: scheduledTasks.firstOrNull { it.id == taskId }
      ?: return VerticalDropPreview(0, 0, false)
    val workingTasks = if (allDynamicTasks.any { it.id == taskId }) allDynamicTasks else allDynamicTasks + dragged
    val (updated, hitWall) = applyVerticalDropReposition(
      allDynamicTasks = workingTasks,
      scheduledTasks = scheduledTasks,
      taskId = taskId,
      dropTarget = dropTarget,
      baseManualOrder = 1L,
      anchors = anchors,
      day = day
    )
    val moved = updated.firstOrNull { it.id == taskId } ?: dragged
    val startMin = moved.startTimeMinute
    val birthDur = birthDurationOf(dragged)
    val previewDur = if (birthDur == 0) {
      0
    } else if (birthDur > dragged.durationMinutes && !hitWall) {
      val minDur = minAllowedDurationOf(dragged)
      val (wStart, wEnd) = findEnclosingAnchorBounds(anchors, startMin, day)
      val otherUsedInWin = scheduledTasks
        .filter {
          it.type == TaskType.DYNAMIC &&
            it.id != taskId &&
            it.durationMinutes > 0 &&
            it.startTimeMinute in wStart until wEnd
        }
        .sumOf { it.durationMinutes.coerceAtLeast(0) + it.interTaskBufferMinutes.coerceIn(0, 30) }
      val buf = dragged.interTaskBufferMinutes.coerceIn(0, 30)
      val freeWinCap = ((wEnd - wStart) - otherUsedInWin - buf).coerceAtLeast(dragged.durationMinutes)
      freeWinCap.coerceIn(minDur, birthDur)
    } else {
      moved.durationMinutes
    }
    val endMin = (startMin + previewDur) % 1440
    return VerticalDropPreview(
      previewStartMinute = startMin,
      previewEndMinute = endMin,
      hitAnchorWall = hitWall
    )
  }
}
