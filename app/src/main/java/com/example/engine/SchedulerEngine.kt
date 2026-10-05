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

object SchedulerEngine {

  const val MIN_SPLIT_MINUTES = 15
  const val SPLIT_THRESHOLD_MINUTES = 60
  const val MIN_FLUID_TWIN_MINUTES = 45
  const val MIN_ELASTIC_BUFFER_MINUTES = 15

  /**
   * Recalculates Slot 1 (Reverse Sleep) and Slot 5 (Elastic Buffer) based on
   * Anchor 2, Anchor 4, and Anchor 6 with complete midnight rollover safety.
   */
  fun calculateAnchors(
    anchors: List<RoutineTask>,
    sleepDurationMinutes: Int = 420 // 7 hours default
  ): List<RoutineTask> {
    val map = anchors.associateBy { it.slotNumber ?: 0 }.toMutableMap()

    // 1. Slot 2: Wake-up Anchor (Default 05:00 AM = 300 min)
    val slot2 = map[2] ?: RoutineTask(
      id = "anchor_slot_2",
      title = "Wake Up & Morning Anchor",
      type = TaskType.ANCHOR_STANDARD,
      slotNumber = 2,
      startTimeMinute = 300, // 05:00 AM
      durationMinutes = 45,
      voiceMessage = "Good morning! Time to start the day with focus.",
      enableDnd = true
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
      title = "Reverse Sleep Recovery",
      type = TaskType.ANCHOR_SLEEP,
      slotNumber = 1,
      startTimeMinute = bedtimeMinute,
      durationMinutes = safeSleepDuration,
      voiceMessage = "Bedtime reached. Entering recovery mode.",
      enableDnd = true
    )).copy(
      type = TaskType.ANCHOR_SLEEP,
      slotNumber = 1,
      startTimeMinute = bedtimeMinute,
      durationMinutes = safeSleepDuration
    )
    map[1] = slot1

    // 3. Slot 3: Deep Work (Default 09:00 AM = 540 min)
    if (!map.containsKey(3)) {
      map[3] = RoutineTask(
        id = "anchor_slot_3",
        title = "Deep Work & Study Engine",
        type = TaskType.ANCHOR_STANDARD,
        slotNumber = 3,
        startTimeMinute = 540, // 09:00 AM
        durationMinutes = 180, // 3 hours -> 12:00 PM
        voiceMessage = "Deep work block initiated. DND engaged.",
        enableDnd = true
      )
    }

    // 4. Slot 4: Health / Fitness (Default 02:00 PM = 840 min)
    if (!map.containsKey(4)) {
      map[4] = RoutineTask(
        id = "anchor_slot_4",
        title = "Health & Fitness Engine",
        type = TaskType.ANCHOR_STANDARD,
        slotNumber = 4,
        startTimeMinute = 840, // 02:00 PM
        durationMinutes = 60,  // 03:00 PM
        voiceMessage = "Workout session starting now. Hydrate!",
        enableDnd = true
      )
    }

    // 5. Slot 6: Evening Reset (Default 07:30 PM = 1170 min)
    if (!map.containsKey(6)) {
      map[6] = RoutineTask(
        id = "anchor_slot_6",
        title = "Evening Reset & Family Anchor",
        type = TaskType.ANCHOR_STANDARD,
        slotNumber = 6,
        startTimeMinute = 1170, // 07:30 PM
        durationMinutes = 60,   // 08:30 PM
        voiceMessage = "Evening transition initiated.",
        enableDnd = false
      )
    }

    // 6. Slot 7: Night Reflection (Default 10:00 PM = 1320 min)
    if (!map.containsKey(7)) {
      map[7] = RoutineTask(
        id = "anchor_slot_7",
        title = "Night Reflection & Wind-Down",
        type = TaskType.ANCHOR_STANDARD,
        slotNumber = 7,
        startTimeMinute = 1320, // 10:00 PM
        durationMinutes = 45,   // 10:45 PM
        voiceMessage = "Night wind-down starting. Dim screens.",
        enableDnd = true
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
      title = "Elastic Rubber Buffer",
      type = TaskType.ANCHOR_ELASTIC,
      slotNumber = 5,
      startTimeMinute = slot4End,
      durationMinutes = elasticDuration,
      voiceMessage = "Entering protected elastic buffer time.",
      enableDnd = false
    )).copy(
      type = TaskType.ANCHOR_ELASTIC,
      slotNumber = 5,
      startTimeMinute = slot4End,
      durationMinutes = elasticDuration
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
      // For dynamic tasks, include the interTaskBufferMinutes to guarantee rest gap
      val totalBlock = if (task.type == TaskType.DYNAMIC) {
        task.durationMinutes + interTaskBufferMinutes
      } else {
        task.durationMinutes
      }

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
    interTaskBufferMinutes: Int = 5
  ): ScheduleResult {
    // 1. Anchors active on this day (excluding soft-deleted)
    val dayAnchors = anchors.filter { !it.isDeleted && it.selectedDays.contains(activeDay) }

    // List of placed tasks starting with anchors
    val scheduledTasks = ArrayList(dayAnchors)
    val overflowTasks = mutableListOf<RoutineTask>()

    // Filter dynamic tasks for the active day (excluding soft-deleted)
    val dayDynamicTasks = dynamicTasks.filter { !it.isDeleted && it.selectedDays.contains(activeDay) }

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
          val gaps = findAvailableGaps(scheduledTasks, interTaskBufferMinutes)
          val fitGap = gaps.find { it.durationMinutes >= lone.durationMinutes }
          if (fitGap != null) {
            val start = fitGap.startMinute
            scheduledTasks.add(lone.copy(startTimeMinute = start, isSplitPart = false, isOverflow = false))
          } else {
            overflowTasks.add(lone.copy(isOverflow = true, overflowReason = "Fluid twin partner missing and no single gap available."))
          }
        }
      } else {
        // Strict Anchor Boundary Enforcement: anchors act as impenetrable walls
        val anchorGaps = findAvailableGaps(dayAnchors, interTaskBufferMinutes)
        val gap1 = anchorGaps.find { part1.startTimeMinute in it.startMinute until it.endMinute }
          ?: anchorGaps.minByOrNull { Math.abs((it.startMinute + it.endMinute) / 2 - part1.startTimeMinute) }
        val gap2 = anchorGaps.find { part2.startTimeMinute in it.startMinute until it.endMinute }
          ?: anchorGaps.minByOrNull { Math.abs((it.startMinute + it.endMinute) / 2 - part2.startTimeMinute) }

        val pool = part1.fluidTotalPoolMinutes ?: (part1.durationMinutes + part2.durationMinutes)

        if (gap1 == null || gap2 == null || gap1.durationMinutes < MIN_FLUID_TWIN_MINUTES || gap2.durationMinutes < MIN_FLUID_TWIN_MINUTES) {
          overflowTasks.add(part1.copy(isOverflow = true, overflowReason = "Anchor boundary prevents 45m minimum fluid twin duration."))
          overflowTasks.add(part2.copy(isOverflow = true, overflowReason = "Anchor boundary prevents 45m minimum fluid twin duration."))
        } else if (gap1.startMinute == gap2.startMinute && gap1.endMinute == gap2.endMinute) {
          // Both twins in same inter-anchor gap
          val available = gap1.durationMinutes - interTaskBufferMinutes
          if (available < MIN_FLUID_TWIN_MINUTES * 2) {
            overflowTasks.add(part1.copy(isOverflow = true, overflowReason = "Anchor gap too small to host two 45m fluid twin sessions."))
            overflowTasks.add(part2.copy(isOverflow = true, overflowReason = "Anchor gap too small to host two 45m fluid twin sessions."))
          } else {
            var d1 = part1.durationMinutes.coerceIn(MIN_FLUID_TWIN_MINUTES, available - MIN_FLUID_TWIN_MINUTES)
            var d2 = (pool - d1).coerceIn(MIN_FLUID_TWIN_MINUTES, available - d1)
            if (d1 + d2 > available) {
              d1 = available / 2
              d2 = available - d1
            }
            val start1 = gap1.startMinute
            val start2 = start1 + d1 + interTaskBufferMinutes
            scheduledTasks.add(part1.copy(startTimeMinute = start1, durationMinutes = d1, isSplitPart = false, isOverflow = false))
            scheduledTasks.add(part2.copy(startTimeMinute = start2, durationMinutes = d2, isSplitPart = false, isOverflow = false))
          }
        } else {
          // Distinct inter-anchor gaps: Communicating vessels liquid time sharing
          val cap1 = gap1.durationMinutes
          val cap2 = gap2.durationMinutes

          var d1 = part1.durationMinutes.coerceIn(MIN_FLUID_TWIN_MINUTES, cap1)
          var d2 = (pool - d1).coerceAtLeast(MIN_FLUID_TWIN_MINUTES)

          // If Part 2 is compressed by anchor wall in Gap 2, surplus flows to Part 1
          if (d2 > cap2) {
            d2 = cap2
            d1 = (pool - d2).coerceIn(MIN_FLUID_TWIN_MINUTES, cap1)
          }
          // If Part 1 is compressed by anchor wall in Gap 1, surplus flows to Part 2
          if (d1 > cap1) {
            d1 = cap1
            d2 = (pool - d1).coerceIn(MIN_FLUID_TWIN_MINUTES, cap2)
          }

          val start1 = when {
            part1.startTimeMinute in gap1.startMinute..(gap1.endMinute - d1) -> part1.startTimeMinute
            part1.startTimeMinute < gap1.startMinute -> gap1.startMinute
            else -> gap1.endMinute - d1
          }

          val start2 = when {
            part2.startTimeMinute in gap2.startMinute..(gap2.endMinute - d2) -> part2.startTimeMinute
            part2.startTimeMinute < gap2.startMinute -> gap2.startMinute
            else -> gap2.endMinute - d2
          }

          scheduledTasks.add(part1.copy(startTimeMinute = start1, durationMinutes = d1, isSplitPart = false, isOverflow = false))
          scheduledTasks.add(part2.copy(startTimeMinute = start2, durationMinutes = d2, isSplitPart = false, isOverflow = false))
        }
      }
    }

    // 3. Schedule Standard Dynamic Tasks
    for (dynamicTask in standardTasks) {
      val gaps = findAvailableGaps(scheduledTasks, interTaskBufferMinutes)
      val requiredDuration = dynamicTask.durationMinutes

      // Try continuous placement first near preferred start time
      val prefStart = dynamicTask.startTimeMinute
      var bestGap: ScheduleGap? = null
      var bestPlacementStart = -1
      var minDistance = Int.MAX_VALUE

      for (gap in gaps) {
        if (gap.durationMinutes >= requiredDuration) {
          val possibleStart = when {
            prefStart in gap.startMinute..(gap.endMinute - requiredDuration) -> prefStart
            prefStart < gap.startMinute -> gap.startMinute
            else -> gap.endMinute - requiredDuration
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
          durationMinutes = requiredDuration,
          isSplitPart = false,
          isOverflow = false
        )
        scheduledTasks.add(placed)
        continue
      }

      // If continuous placement failed, check if task splitting is allowed and duration > 60m
      if (dynamicTask.allowSplitting && requiredDuration > SPLIT_THRESHOLD_MINUTES) {
        // Only consider viable gaps that meet the minimum 15-minute threshold
        val viableGaps = gaps.filter { it.durationMinutes >= MIN_SPLIT_MINUTES }
        val totalCapacity = viableGaps.sumOf { it.durationMinutes }

        if (totalCapacity >= requiredDuration) {
          var remaining = requiredDuration
          val splitParts = mutableListOf<RoutineTask>()
          var partIndex = 1

          val sortedGaps = viableGaps.sortedBy { gap ->
            val center = (gap.startMinute + gap.endMinute) / 2
            Math.abs(center - prefStart)
          }

          for (gap in sortedGaps) {
            if (remaining <= 0) break

            val availableInGap = gap.durationMinutes
            val chunkDuration = when {
              remaining <= availableInGap -> remaining
              else -> availableInGap
            }

            if (chunkDuration < MIN_SPLIT_MINUTES) continue

            // Guard against leaving an illegal < 15m remainder
            val actualChunk = if (remaining - chunkDuration in 1 until MIN_SPLIT_MINUTES) {
              val adjusted = chunkDuration - (MIN_SPLIT_MINUTES - (remaining - chunkDuration))
              if (adjusted >= MIN_SPLIT_MINUTES) adjusted else chunkDuration
            } else {
              chunkDuration
            }

            if (actualChunk < MIN_SPLIT_MINUTES) continue

            val part = dynamicTask.copy(
              id = "${dynamicTask.id}_part_${partIndex}",
              title = "${dynamicTask.title} (Part $partIndex)",
              startTimeMinute = gap.startMinute,
              durationMinutes = actualChunk,
              isSplitPart = true,
              splitPartLabel = "Part $partIndex (${actualChunk}m)",
              parentTaskId = dynamicTask.id,
              isOverflow = false
            )
            splitParts.add(part)
            remaining -= actualChunk
            partIndex++
          }

          if (remaining == 0) {
            scheduledTasks.addAll(splitParts)
            continue
          }
        }
      }

      // If we reach here, it could not be scheduled continuously or split
      val reason = if (!dynamicTask.allowSplitting && requiredDuration > 60) {
        "Needs ${requiredDuration}m continuous gap (Splitting disabled)"
      } else {
        "No available open slot can accommodate ${requiredDuration}m without violating anchors"
      }
      overflowTasks.add(
        dynamicTask.copy(
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
}
