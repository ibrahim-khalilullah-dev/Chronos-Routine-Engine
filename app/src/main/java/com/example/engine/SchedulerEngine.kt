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

object SchedulerEngine {

  const val MIN_FLUID_TWIN_MINUTES = 45
  const val MIN_ELASTIC_BUFFER_MINUTES = 15

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
   * by the 7 immutable anchors or already committed tasks.
   */
  fun findAvailableGaps(
    committedTasks: List<RoutineTask>,
    interTaskBufferMinutes: Int = 0
  ): List<ScheduleGap> {
    val occupied = BooleanArray(1440) { false }

    for (task in committedTasks) {
      val start = ((task.startTimeMinute % 1440) + 1440) % 1440
      val taskBuffer = if (task.type == TaskType.DYNAMIC) {
        task.interTaskBufferMinutes.coerceIn(0, 30)
      } else {
        0 // Anchors possess hard mathematical boundaries; do NOT bleed rest buffers across adjacent anchors
      }
      val totalBlock = task.durationMinutes + taskBuffer

      for (i in 0 until totalBlock) {
        val min = (start + i) % 1440
        occupied[min] = true
      }
    }

    val gaps = mutableListOf<ScheduleGap>()
    var inGap = false
    var gapStart = 0

    for (m in 0 until 1440) {
      if (!occupied[m]) {
        if (!inGap) {
          inGap = true
          gapStart = m
        }
      } else {
        if (inGap) {
          inGap = false
          val gapDuration = m - gapStart
          if (gapDuration > 0) {
            gaps.add(ScheduleGap(gapStart, m, gapDuration))
          }
        }
      }
    }
    if (inGap) {
      val gapDuration = 1440 - gapStart
      if (gapDuration > 0) {
        gaps.add(ScheduleGap(gapStart, 1440, gapDuration))
      }
    }

    return gaps
  }

  /**
   * Main Dynamic Scheduling Function:
   * Places dynamic tasks using preferred time / auto-fit / auto-splitting (>= 15m),
   * applies inter-task buffer, and moves unplaceable tasks to the overflow pool.
   */
  fun scheduleDay(
    anchors: List<RoutineTask>,
    dynamicTasks: List<RoutineTask>,
    activeDay: RoutineDay,
    interTaskBufferMinutes: Int = 0
  ): ScheduleResult {
    // 1. Anchors active on this day (excluding soft-deleted and paused)
    val dayAnchors = anchors
      .filter { !it.isDeleted && !it.isCurrentlyPaused() && it.selectedDays.contains(activeDay) }
      .map { anchor ->
        if (anchor.slotNumber == 3 && activeDay == RoutineDay.FRIDAY) {
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

    // List of placed tasks starting with anchors
    val scheduledTasks = ArrayList(dayAnchors)
    val overflowTasks = mutableListOf<RoutineTask>()

    // Filter dynamic tasks for the active day (excluding soft-deleted and paused)
    val dayDynamicTasks = dynamicTasks.filter { !it.isDeleted && !it.isCurrentlyPaused() && it.selectedDays.contains(activeDay) }

    // 2. Separate Fluid Twins and Standard Dynamic Tasks
    val (fluidTwinTasks, standardTasks) = dayDynamicTasks.partition { it.isFluidTwin && !it.fluidTwinId.isNullOrEmpty() }
    val fluidTwinGroups = fluidTwinTasks.groupBy { it.fluidTwinId!! }

    for ((_, twinGroup) in fluidTwinGroups) {
      val part1 = twinGroup.find { it.fluidTwinPart == 1 } ?: twinGroup.getOrNull(0)
      val part2 = twinGroup.find { it.fluidTwinPart == 2 } ?: twinGroup.getOrNull(1)

      if (part1 == null || part2 == null) {
        val lone = part1 ?: part2
        if (lone != null) {
          // Lone twin treated as normal dynamic task
          val birthStart = lone.originalStartTimeMinute ?: lone.startTimeMinute
          val gaps = findAvailableGaps(scheduledTasks, interTaskBufferMinutes)
          val fitGap = gaps.find { birthStart in it.startMinute until it.endMinute && it.durationMinutes >= lone.durationMinutes }
            ?: gaps.find { it.durationMinutes >= lone.durationMinutes }
          if (fitGap != null) {
            val start = if (birthStart in fitGap.startMinute..(fitGap.endMinute - lone.durationMinutes)) birthStart else fitGap.startMinute
            scheduledTasks.add(
              lone.copy(
                startTimeMinute = start,
                originalStartTimeMinute = birthStart,
                isSplitPart = false,
                isOverflow = false
              )
            )
          } else {
            overflowTasks.add(
              lone.copy(
                originalStartTimeMinute = birthStart,
                isOverflow = true,
                overflowReason = "Fluid twin partner missing and no single gap available."
              )
            )
          }
        }
      } else {
        val p1Birth = part1.originalStartTimeMinute ?: part1.startTimeMinute
        val p2Birth = part2.originalStartTimeMinute ?: part2.startTimeMinute

        // Strict Anchor Boundary Enforcement: anchors act as impenetrable walls
        val anchorGaps = findAvailableGaps(scheduledTasks, interTaskBufferMinutes)
        val gap1 = anchorGaps.find { p1Birth in it.startMinute until it.endMinute }
          ?: anchorGaps.minByOrNull { Math.abs((it.startMinute + it.endMinute) / 2 - p1Birth) }
        val gap2 = anchorGaps.find { p2Birth in it.startMinute until it.endMinute }
          ?: anchorGaps.minByOrNull { Math.abs((it.startMinute + it.endMinute) / 2 - p2Birth) }

        val pool = part1.fluidTotalPoolMinutes ?: (part1.durationMinutes + part2.durationMinutes)
        val floor1 = if (part1.durationMinutes <= 0) 0 else part1.fluidSafetyFloorMinutes.coerceIn(1, part1.durationMinutes)
        val floor2 = if (part2.durationMinutes <= 0) 0 else part2.fluidSafetyFloorMinutes.coerceIn(1, part2.durationMinutes)
        val b1 = part1.interTaskBufferMinutes.coerceIn(0, 30)
        val b2 = part2.interTaskBufferMinutes.coerceIn(0, 30)

        if (part1.durationMinutes == 0 && part2.durationMinutes == 0) {
          scheduledTasks.add(
            part1.copy(
              startTimeMinute = p1Birth.coerceIn(0, 1439),
              originalStartTimeMinute = p1Birth,
              durationMinutes = 0,
              isSplitPart = false,
              isOverflow = false
            )
          )
          scheduledTasks.add(
            part2.copy(
              startTimeMinute = p2Birth.coerceIn(0, 1439),
              originalStartTimeMinute = p2Birth,
              durationMinutes = 0,
              isSplitPart = false,
              isOverflow = false
            )
          )
          continue
        }

        if (gap1 == null || gap2 == null || gap1.durationMinutes < floor1 + b1 || gap2.durationMinutes < floor2 + b2) {
          overflowTasks.add(part1.copy(originalStartTimeMinute = p1Birth, isOverflow = true, overflowReason = "Anchor boundary prevents minimum fluid twin duration."))
          overflowTasks.add(part2.copy(originalStartTimeMinute = p2Birth, isOverflow = true, overflowReason = "Anchor boundary prevents minimum fluid twin duration."))
          continue
        } else if (gap1.startMinute == gap2.startMinute && gap1.endMinute == gap2.endMinute) {
          // Both twins in same inter-anchor gap
          val available = (gap1.durationMinutes - b1 - b2).coerceAtLeast(0)
          if (available < floor1 + floor2) {
            overflowTasks.add(part1.copy(originalStartTimeMinute = p1Birth, isOverflow = true, overflowReason = "Anchor gap too small to host both fluid twin sessions."))
            overflowTasks.add(part2.copy(originalStartTimeMinute = p2Birth, isOverflow = true, overflowReason = "Anchor gap too small to host both fluid twin sessions."))
            continue
          } else {
            val maxD1 = (available - floor2).coerceAtLeast(floor1)
            var d1 = part1.durationMinutes.coerceIn(floor1, maxD1)
            val maxD2 = (available - d1).coerceAtLeast(floor2)
            var d2 = (pool - d1).coerceIn(floor2, maxD2)
            if (d1 + d2 > available) {
              val extra = (available - floor1 - floor2).coerceAtLeast(0)
              d1 = floor1 + extra / 2
              d2 = available - d1
            }
            val minStart1 = gap1.startMinute
            val maxStart1 = gap1.endMinute - (d1 + b1 + d2 + b2)
            val start1 = if (maxStart1 >= minStart1) {
              p1Birth.coerceIn(minStart1, maxStart1)
            } else {
              minStart1
            }
            val minStart2 = start1 + d1 + b1
            val maxStart2 = gap1.endMinute - (d2 + b2)
            val start2 = if (maxStart2 >= minStart2) {
              if (p2Birth >= minStart2) {
                p2Birth.coerceIn(minStart2, maxStart2)
              } else {
                minStart2
              }
            } else {
              minStart2
            }
            scheduledTasks.add(part1.copy(startTimeMinute = start1, originalStartTimeMinute = p1Birth, durationMinutes = d1, isSplitPart = false, isOverflow = false))
            scheduledTasks.add(part2.copy(startTimeMinute = start2, originalStartTimeMinute = p2Birth, durationMinutes = d2, isSplitPart = false, isOverflow = false))
          }
        } else {
          // Distinct inter-anchor gaps: Communicating vessels liquid time sharing
          val cap1 = (gap1.durationMinutes - b1).coerceAtLeast(0)
          val cap2 = (gap2.durationMinutes - b2).coerceAtLeast(0)
          if (cap1 < floor1 || cap2 < floor2) {
            overflowTasks.add(part1.copy(originalStartTimeMinute = p1Birth, isOverflow = true, overflowReason = "Anchor boundary prevents minimum fluid twin duration."))
            overflowTasks.add(part2.copy(originalStartTimeMinute = p2Birth, isOverflow = true, overflowReason = "Anchor boundary prevents minimum fluid twin duration."))
            continue
          }

          var d1 = part1.durationMinutes.coerceIn(floor1, cap1)
          var d2 = (pool - d1).coerceAtLeast(floor2)

          // If Part 2 is compressed by anchor wall in Gap 2, surplus flows to Part 1
          if (d2 > cap2) {
            d2 = cap2
            d1 = (pool - d2).coerceIn(floor1, cap1)
          }
          // If Part 1 is compressed by anchor wall in Gap 1, surplus flows to Part 2
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

          scheduledTasks.add(part1.copy(startTimeMinute = start1, originalStartTimeMinute = p1Birth, durationMinutes = d1, isSplitPart = false, isOverflow = false))
          scheduledTasks.add(part2.copy(startTimeMinute = start2, originalStartTimeMinute = p2Birth, durationMinutes = d2, isSplitPart = false, isOverflow = false))
        }
      }
    }

    // 3. Schedule Standard Dynamic Tasks
    val sortedStandardTasks = standardTasks.sortedWith(
      compareByDescending<RoutineTask> { it.isHighPriority }
        .thenByDescending { it.manualPlacementOrder }
        .thenBy { it.originalStartTimeMinute ?: it.startTimeMinute }
    )
    val anchorGaps = findAvailableGaps(dayAnchors, interTaskBufferMinutes)

    for (dynamicTask in sortedStandardTasks) {
      val requiredDuration = dynamicTask.durationMinutes.coerceAtLeast(0)
      val birthStart = dynamicTask.originalStartTimeMinute ?: dynamicTask.startTimeMinute
      val prefStart = birthStart

      // 0-minute instant notification / memory signal: allocate momentary trigger marker without failing gap checks
      if (requiredDuration == 0) {
        scheduledTasks.add(
          dynamicTask.copy(
            startTimeMinute = ((prefStart % 1440) + 1440) % 1440,
            originalStartTimeMinute = birthStart,
            durationMinutes = 0,
            isSplitPart = false,
            isOverflow = false
          )
        )
        continue
      }

      val gaps = findAvailableGaps(scheduledTasks, interTaskBufferMinutes)
      val taskBuffer = dynamicTask.interTaskBufferMinutes.coerceIn(0, 30)
      val requiredBlock = requiredDuration + taskBuffer
      val effectiveTolerance = maxOf(dynamicTask.toleranceMinutes, dynamicTask.customFlexibilityMinutes).coerceIn(0, 30)

      // High Priority Anchor Wall Fence: strictly confine to inter-anchor gap enclosing prefStart
      if (dynamicTask.isHighPriority) {
        val enclosingAnchorGap = anchorGaps.find { prefStart in it.startMinute until it.endMinute }
          ?: anchorGaps.find { prefStart in it.startMinute..it.endMinute }
          ?: anchorGaps.minByOrNull { Math.abs(prefStart - (it.startMinute + it.endMinute) / 2) }
        if (enclosingAnchorGap == null) {
          overflowTasks.add(
            dynamicTask.copy(
              originalStartTimeMinute = birthStart,
              isOverflow = true,
              overflowReason = "High Priority task preferred time conflicts directly with an anchor wall."
            )
          )
          continue
        }

        val fencedGaps = gaps.filter {
          it.startMinute >= enclosingAnchorGap.startMinute && it.endMinute <= enclosingAnchorGap.endMinute
        }

        // Try continuous placement in fencedGaps
        var bestGap: ScheduleGap? = null
        var bestPlacementStart = -1
        var minDistance = Int.MAX_VALUE

        for (gap in fencedGaps) {
          if (gap.durationMinutes >= requiredBlock) {
            val possibleStart = when {
              prefStart in gap.startMinute..(gap.endMinute - requiredBlock) -> prefStart
              prefStart < gap.startMinute -> gap.startMinute
              else -> gap.endMinute - requiredBlock
            }
            val distance = Math.abs(possibleStart - prefStart)
            if (distance < minDistance) {
              minDistance = distance
              bestGap = gap
              bestPlacementStart = possibleStart
            }
          }
        }

        if (bestGap != null && bestPlacementStart != -1) {
          scheduledTasks.add(
            dynamicTask.copy(
              startTimeMinute = bestPlacementStart,
              originalStartTimeMinute = birthStart,
              durationMinutes = requiredDuration,
              isSplitPart = false,
              isOverflow = false
            )
          )
          continue
        }

        // Try tolerance / custom flexibility within fencedGaps
        if (effectiveTolerance > 0) {
          val minFloor = if (requiredDuration >= 15) 15 else 1
          val minViable = (requiredDuration - effectiveTolerance).coerceIn(minFloor, requiredDuration)
          val minBlock = minViable + taskBuffer
          var flexGap: ScheduleGap? = null
          var flexStart = -1
          var flexDuration = -1
          var minFlexDist = Int.MAX_VALUE

          for (gap in fencedGaps) {
            if (gap.durationMinutes >= minBlock) {
              val fittedDuration = (gap.durationMinutes - taskBuffer).coerceIn(minViable, requiredDuration)
              val fittedBlock = fittedDuration + taskBuffer
              val possibleStart = when {
                prefStart in gap.startMinute..(gap.endMinute - fittedBlock) -> prefStart
                prefStart < gap.startMinute -> gap.startMinute
                else -> gap.endMinute - fittedBlock
              }
              val distance = Math.abs(possibleStart - prefStart)
              if (distance < minFlexDist) {
                minFlexDist = distance
                flexGap = gap
                flexStart = possibleStart
                flexDuration = fittedDuration
              }
            }
          }

          if (flexGap != null && flexStart != -1 && flexDuration >= minViable) {
            scheduledTasks.add(
              dynamicTask.copy(
                startTimeMinute = flexStart,
                originalStartTimeMinute = birthStart,
                durationMinutes = flexDuration,
                isSplitPart = false,
                isOverflow = false
              )
            )
            continue
          }
        }

        // High priority task cannot fit between assigned anchor boundaries without overleaping walls
        overflowTasks.add(
          dynamicTask.copy(
            originalStartTimeMinute = birthStart,
            isOverflow = true,
            overflowReason = "High Priority task cannot fit between assigned anchor boundaries without overleaping walls."
          )
        )
        continue
      }

      // Try continuous placement first near preferred start time across all gaps
      var bestGap: ScheduleGap? = null
      var bestPlacementStart = -1
      var minDistance = Int.MAX_VALUE

      for (gap in gaps) {
        if (gap.durationMinutes >= requiredBlock) {
          val possibleStart = when {
            prefStart in gap.startMinute..(gap.endMinute - requiredBlock) -> prefStart
            prefStart < gap.startMinute -> gap.startMinute
            else -> gap.endMinute - requiredBlock
          }
          val distance = Math.abs(possibleStart - prefStart)
          if (distance < minDistance) {
            minDistance = distance
            bestGap = gap
            bestPlacementStart = possibleStart
          }
        }
      }

      if (bestGap != null && bestPlacementStart != -1) {
        // Continuous placement success
        val placed = dynamicTask.copy(
          startTimeMinute = bestPlacementStart,
          originalStartTimeMinute = birthStart,
          durationMinutes = requiredDuration,
          isSplitPart = false,
          isOverflow = false
        )
        scheduledTasks.add(placed)
        continue
      }

      // Schedule Float / Tolerance Margin / Custom Flexibility (±5m to ±30m flexibility)
      if (effectiveTolerance > 0) {
        val minFloor = if (requiredDuration >= 15) 15 else 1
        val minViable = (requiredDuration - effectiveTolerance).coerceIn(minFloor, requiredDuration)
        val minBlock = minViable + taskBuffer
        var flexGap: ScheduleGap? = null
        var flexStart = -1
        var flexDuration = -1
        var minFlexDist = Int.MAX_VALUE

        for (gap in gaps) {
          if (gap.durationMinutes >= minBlock) {
            val fittedDuration = (gap.durationMinutes - taskBuffer).coerceIn(minViable, requiredDuration)
            val fittedBlock = fittedDuration + taskBuffer
            val possibleStart = when {
              prefStart in gap.startMinute..(gap.endMinute - fittedBlock) -> prefStart
              prefStart < gap.startMinute -> gap.startMinute
              else -> gap.endMinute - fittedBlock
            }
            val distance = Math.abs(possibleStart - prefStart)
            if (distance < minFlexDist) {
              minFlexDist = distance
              flexGap = gap
              flexStart = possibleStart
              flexDuration = fittedDuration
            }
          }
        }

        if (flexGap != null && flexStart != -1 && flexDuration >= minViable) {
          val placed = dynamicTask.copy(
            startTimeMinute = flexStart,
            originalStartTimeMinute = birthStart,
            durationMinutes = flexDuration,
            isSplitPart = false,
            isOverflow = false
          )
          scheduledTasks.add(placed)
          continue
        }
      }

      // If we reach here, it could not be scheduled continuously
      val reason = "No available open slot can accommodate ${requiredDuration}m without violating anchors"
      overflowTasks.add(
        dynamicTask.copy(
          originalStartTimeMinute = birthStart,
          isOverflow = true,
          overflowReason = reason
        )
      )
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
   * Resolves effective anchor tasks for [day] (accounting for Friday Jumu'ah override on Slot 3).
   */
  private fun resolveEffectiveDayAnchors(anchors: List<RoutineTask>, day: RoutineDay): List<RoutineTask> {
    val isFriday = day == RoutineDay.FRIDAY
    return anchors.map { anchor ->
      if (isFriday && anchor.slotNumber == 3) {
        anchor.copy(
          title = anchor.fridayTitle?.ifBlank { null } ?: anchor.title,
          startTimeMinute = anchor.fridayStartTimeMinute ?: anchor.startTimeMinute,
          durationMinutes = anchor.fridayDurationMinutes ?: anchor.durationMinutes
        )
      } else {
        anchor
      }
    }
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
   * - High Priority & Fluid Twin tasks are strictly fenced inside their enclosing anchor gap.
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

    val finalStart = if (target.isHighPriority || target.isFluidTwin) {
      val currentAnchorMinute = target.originalStartTimeMinute ?: target.startTimeMinute
      val (gapStart, gapEnd) = findEnclosingAnchorBounds(anchors, currentAnchorMinute, day)
      val minRequiredDur = if (target.isFluidTwin) {
        target.fluidSafetyFloorMinutes.coerceAtLeast(1)
      } else {
        target.durationMinutes.coerceAtLeast(0)
      }
      val block = minRequiredDur + target.interTaskBufferMinutes.coerceIn(0, 30)
      val maxAllowedStart = (gapEnd - block).coerceAtLeast(gapStart)

      if (clampedGlobalStart < gapStart || clampedGlobalStart > maxAllowedStart) {
        hitAnchorWall = true
      }
      clampedGlobalStart.coerceIn(gapStart, maxAllowedStart)
    } else {
      // Normal dynamic tasks can jump across anchors; if landing inside an anchor body, snap to nearest anchor gap edge
      val dayAnchors = resolveEffectiveDayAnchors(anchors, day)
      val anchorGaps = findAvailableGaps(dayAnchors, 0)
      val block = target.durationMinutes.coerceAtLeast(0) + target.interTaskBufferMinutes.coerceIn(0, 30)
      val insideGap = anchorGaps.firstOrNull { clampedGlobalStart in it.startMinute until it.endMinute }
      if (insideGap != null) {
        val maxInGap = (insideGap.endMinute - block).coerceAtLeast(insideGap.startMinute)
        clampedGlobalStart.coerceIn(insideGap.startMinute, maxInGap)
      } else {
        val viableGap = anchorGaps.filter { it.durationMinutes >= block }
          .minByOrNull { gap ->
            val maxS = (gap.endMinute - block).coerceAtLeast(gap.startMinute)
            val edge = clampedGlobalStart.coerceIn(gap.startMinute, maxS)
            Math.abs(edge - clampedGlobalStart)
          } ?: anchorGaps.minByOrNull { gap ->
          Math.abs(clampedGlobalStart.coerceIn(gap.startMinute, gap.endMinute) - clampedGlobalStart)
        }
        if (viableGap != null) {
          val maxS = (viableGap.endMinute - block).coerceAtLeast(viableGap.startMinute)
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
    val isFenced = draggedSource.isHighPriority || draggedSource.isFluidTwin
    val draggedCurrStart = draggedScheduled?.startTimeMinute
      ?: draggedSource.originalStartTimeMinute
      ?: draggedSource.startTimeMinute
    val draggedGap = findEnclosingAnchorBounds(anchors, draggedCurrStart, day)
    val minDraggedDur = if (draggedSource.isFluidTwin) {
      draggedSource.fluidSafetyFloorMinutes.coerceAtLeast(1)
    } else {
      draggedSource.durationMinutes.coerceAtLeast(0)
    }
    val draggedMinBlock = minDraggedDur + draggedSource.interTaskBufferMinutes.coerceIn(0, 30)
    val draggedFullBlock = draggedSource.durationMinutes.coerceAtLeast(0) + draggedSource.interTaskBufferMinutes.coerceIn(0, 30)

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
        if (isFenced && targetGap != draggedGap) {
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
                  task.copy(
                    startTimeMinute = pair.first,
                    originalStartTimeMinute = pair.first,
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

        // Case B: Cross-Anchor Drop (Normal task moved into a different gap onto/before/after targetTask)
        val (targetGapStart, targetGapEnd) = targetGap
        val rawStart = if (dropTarget is VerticalDropTarget.InsertBeforeTask) {
          targetScheduled.startTimeMinute
        } else {
          targetScheduled.startTimeMinute + targetScheduled.durationMinutes + targetScheduled.interTaskBufferMinutes.coerceIn(0, 30)
        }
        val maxInTargetGap = (targetGapEnd - draggedFullBlock).coerceAtLeast(targetGapStart)
        val clampedStart = rawStart.coerceIn(targetGapStart, maxInTargetGap)

        val updated = allDynamicTasks.map { task ->
          if (task.id == taskId) {
            task.copy(
              startTimeMinute = clampedStart,
              originalStartTimeMinute = clampedStart,
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
   * using the exact same rules as [applyVerticalDropReposition].
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
    val endMin = (startMin + moved.durationMinutes) % 1440
    return VerticalDropPreview(
      previewStartMinute = startMin,
      previewEndMinute = endMin,
      hitAnchorWall = hitWall
    )
  }
}
