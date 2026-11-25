package com.example

import com.example.engine.ConsoleParser
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testAnchorParsingWithDndDelay() {
    val script = """
      ANCHOR | 2 | Morning Routine | 05:00 | 30 | Y | 15 | Y | Morning routine starting now
      ANCHOR | 3 | Work Setup | 08:30 | 45 | Y | 5 | Y | Work starting | Friday Reflection | 11:30 | 150 | Friday Jumuah
    """.trimIndent()

    val result = ConsoleParser.parse(script)
    assertEquals(0, result.errorCount)
    assertEquals(2, result.parsedAnchors.size)

    val slot2 = result.parsedAnchors[0]
    assertEquals(2, slot2.slotNumber)
    assertEquals("Morning Routine", slot2.title)
    assertEquals(300, slot2.startTimeMinute) // 05:00 = 300m
    assertEquals(30, slot2.durationMinutes)
    assertTrue(slot2.enableDnd)
    assertEquals(15, slot2.dndDelayMinutes)
    assertTrue(slot2.isVoiceEnabled)

    val slot3 = result.parsedAnchors[1]
    assertEquals(3, slot3.slotNumber)
    assertEquals(5, slot3.dndDelayMinutes)
    assertEquals("Friday Reflection", slot3.fridayTitle)
    assertEquals(690, slot3.fridayStartTimeMinute) // 11:30 = 690m
    assertEquals(150, slot3.fridayDurationMinutes)
  }

  @Test
  fun testTwinParsingCreatesLinkedFluidTasks() {
    val script = """
      TWIN | 180 | Morning Deep Work | 09:00 | 90 | Sprint 1 starting | Afternoon Review | 14:00 | 90 | Sprint 2 starting | Y | Mon,Tue,Wed,Thu | 15 | 10
    """.trimIndent()

    val result = ConsoleParser.parse(script)
    assertEquals(0, result.errorCount)
    assertEquals(2, result.validTasks.size)

    val p1 = result.validTasks[0]
    val p2 = result.validTasks[1]

    assertTrue(p1.isFluidTwin)
    assertTrue(p2.isFluidTwin)
    assertEquals(1, p1.fluidTwinPart)
    assertEquals(2, p2.fluidTwinPart)
    assertEquals(p1.fluidTwinId, p2.fluidTwinId)
    assertEquals(180, p1.fluidTotalPoolMinutes)
    assertEquals(180, p2.fluidTotalPoolMinutes)
    assertEquals(15, p1.toleranceMinutes)
    assertEquals(10, p1.interTaskBufferMinutes)
    assertTrue(p1.enableDnd)
    assertTrue(p2.enableDnd)
    assertEquals(4, p1.selectedDays.size)
  }

  @Test
  fun testDynamicParsingWithFlexibilityAndBuffer() {
    val script = """
      Speed Cardio | 45 | 16:30 | Y | ALL | Y | Cardio block | 15 | 10
      DYNAMIC | Algorithm Coding | 60 | 17:30 | N | Mon,Wed,Fri | Y | Coding practice | 0 | 5
    """.trimIndent()

    val result = ConsoleParser.parse(script)
    assertEquals(0, result.errorCount)
    assertEquals(2, result.validTasks.size)

    val t1 = result.validTasks[0]
    assertEquals("Speed Cardio", t1.title)
    assertEquals(45, t1.durationMinutes)
    assertEquals(990, t1.startTimeMinute) // 16:30 = 990m
    assertTrue(t1.enableDnd)
    assertEquals(15, t1.toleranceMinutes)
    assertEquals(10, t1.interTaskBufferMinutes)

    val t2 = result.validTasks[1]
    assertEquals("Algorithm Coding", t2.title)
    assertEquals(60, t2.durationMinutes)
    assertFalse(t2.enableDnd)
    assertEquals(0, t2.toleranceMinutes)
    assertEquals(5, t2.interTaskBufferMinutes)
  }

  @Test
  fun testSyntaxPredictionsForDirectives() {
    val anchorPred = ConsoleParser.getSyntaxPrediction("ANCHOR")
    assertNotNull(anchorPred)
    assertTrue(anchorPred!!.contains("2"))

    val twinPred = ConsoleParser.getSyntaxPrediction("TWIN")
    assertNotNull(twinPred)
    assertTrue(twinPred!!.contains("180"))

    val dynamicPred = ConsoleParser.getSyntaxPrediction("Study")
    assertNotNull(dynamicPred)
    assertTrue(dynamicPred!!.contains("45"))
  }

  @Test
  fun testSampleSnippetParsesWithZeroErrors() {
    val result = ConsoleParser.parse(ConsoleParser.SAMPLE_SNIPPET)
    assertEquals("Should have 0 errors", 0, result.errorCount)
    assertEquals("7 anchors parsed", 7, result.parsedAnchors.size)
    assertTrue("Should have multiple dynamic tasks", result.validTasks.size >= 20)
  }

  @Test
  fun testHighPriorityAnchorWallFencePreventsTimetableJumping() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(420)
    // Anchor 2 is 05:00 - 05:30 (300m - 330m)
    // Anchor 3 is 13:15 - 13:45 (795m - 825m)
    // Gap between Anchor 2 and 3 is 330m to 795m (465 minutes)
    // Anchor 6 is 18:00 - 18:30 (1080m - 1110m)
    // Anchor 7 is 20:00 - 20:30 (1200m - 1230m)
    // Gap between 6 and 7 is 1110m to 1200m (90 minutes)

    // Create a high priority task preferring 18:40 (1120m, inside Gap 6-7) but requiring 120 minutes
    // Since Gap 6-7 is only 90 minutes, a high-priority task MUST NOT jump back to morning Gap 2-3 (which has 465 minutes)
    val task = RoutineTask(
      id = "dyn_high_priority",
      title = "Evening Deep Focus",
      type = TaskType.DYNAMIC,
      startTimeMinute = 1120, // 06:40 PM
      durationMinutes = 120,  // 2 hours - cannot fit in 90m gap
      isHighPriority = true,
      allowSplitting = false,
      toleranceMinutes = 0,
      customFlexibilityMinutes = 0
    )

    val result = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(task),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )

    assertEquals(1, result.overflowTasks.size)
    val overflow = result.overflowTasks[0]
    assertTrue(overflow.isOverflow)
    assertTrue(overflow.overflowReason?.contains("anchor boundaries") == true)
    // Ensure it was NOT scheduled in morning gap
    assertFalse(result.scheduledTasks.any { it.id == "dyn_high_priority" })
  }

  @Test
  fun testCalculateNextOccurrenceMillisSkipsInactiveDays() {
    // Test that for a task only active on Friday, if today is Saturday, next occurrence is on Friday (offset 6 days)
    val fridayOnly = setOf(RoutineDay.FRIDAY)
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SATURDAY)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 10)
    cal.set(java.util.Calendar.MINUTE, 0)
    val nowMillis = cal.timeInMillis

    val nextMillis = com.example.service.AutomationManager.calculateNextOccurrenceMillis(
      startMinuteOfDay = 600, // 10:00 AM
      selectedDays = fridayOnly,
      currentMillis = nowMillis
    )

    assertTrue(nextMillis > nowMillis)
    val targetCal = java.util.Calendar.getInstance().apply { timeInMillis = nextMillis }
    assertEquals(java.util.Calendar.FRIDAY, targetCal.get(java.util.Calendar.DAY_OF_WEEK))
    assertEquals(10, targetCal.get(java.util.Calendar.HOUR_OF_DAY))
    assertEquals(0, targetCal.get(java.util.Calendar.MINUTE))
  }

  @Test
  fun testNewProductionDefaultAnchors() {
    val defaults = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    assertEquals(7, defaults.size)

    val slot1 = defaults[0]
    assertEquals(1, slot1.slotNumber)
    assertEquals("Sleep", slot1.title)
    assertEquals(360, slot1.durationMinutes)
    assertEquals(1380, slot1.startTimeMinute) // 11:00 PM
    assertEquals("ঘুমানোর সময় হয়েছে।", slot1.voiceMessage)
    assertTrue(slot1.enableDnd)
    assertEquals(0, slot1.dndDelayMinutes)

    val slot2 = defaults[1]
    assertEquals(2, slot2.slotNumber)
    assertEquals("Fajr", slot2.title)
    assertEquals(300, slot2.startTimeMinute) // 05:00 AM
    assertEquals(35, slot2.durationMinutes)
    assertEquals("ফজরের সময় হয়েছে।", slot2.voiceMessage)
    assertTrue(slot2.enableDnd)
    assertEquals(8, slot2.dndDelayMinutes)

    val slot3 = defaults[2]
    assertEquals(3, slot3.slotNumber)
    assertEquals("Dhuhr", slot3.title)
    assertEquals(795, slot3.startTimeMinute) // 01:15 PM
    assertEquals(35, slot3.durationMinutes)
    assertEquals("যোহরের সময় হয়েছে।", slot3.voiceMessage)
    assertTrue(slot3.enableDnd)
    assertEquals(8, slot3.dndDelayMinutes)
    assertEquals("Jumu'ah", slot3.fridayTitle)
    assertEquals(750, slot3.fridayStartTimeMinute)
    assertEquals(120, slot3.fridayDurationMinutes)
    assertEquals("জুমার সময় হয়েছে।", slot3.fridayVoiceMessage)

    val slot4 = defaults[3]
    assertEquals(4, slot4.slotNumber)
    assertEquals("Asr", slot4.title)
    assertEquals(990, slot4.startTimeMinute) // 04:30 PM
    assertEquals(25, slot4.durationMinutes)
    assertEquals("আসরের সময় হয়েছে।", slot4.voiceMessage)
    assertTrue(slot4.enableDnd)
    assertEquals(10, slot4.dndDelayMinutes)

    val slot5 = defaults[4]
    assertEquals(5, slot5.slotNumber)
    assertEquals("Break", slot5.title)
    assertEquals(1015, slot5.startTimeMinute) // 04:55 PM
    assertEquals(65, slot5.durationMinutes)
    assertEquals("বিরতির সময় হয়েছে।", slot5.voiceMessage)
    assertFalse(slot5.enableDnd)

    val slot6 = defaults[5]
    assertEquals(6, slot6.slotNumber)
    assertEquals("Maghrib", slot6.title)
    assertEquals(1080, slot6.startTimeMinute) // 06:00 PM
    assertEquals(30, slot6.durationMinutes)
    assertEquals("মাগরিবের সময় হয়েছে।", slot6.voiceMessage)
    assertTrue(slot6.enableDnd)
    assertEquals(5, slot6.dndDelayMinutes)

    val slot7 = defaults[6]
    assertEquals(7, slot7.slotNumber)
    assertEquals("Isha", slot7.title)
    assertEquals(1200, slot7.startTimeMinute) // 08:00 PM
    assertEquals(35, slot7.durationMinutes)
    assertEquals("এশার সময় হয়েছে।", slot7.voiceMessage)
    assertTrue(slot7.enableDnd)
    assertEquals(10, slot7.dndDelayMinutes)
  }

  @Test
  fun testDynamicParsingWithPriorityToken() {
    val script = """
      High Priority Task | 45 | 10:00 | Y | ALL | Y | High focus session | 15 | 5 | Y
      Standard Task | 30 | 11:00 | N | ALL | Y | Normal task | 0 | 5 | N
      Legacy Backward Compatible Task | 20 | 12:00 | N | ALL | Y | Legacy task | 0 | 5
    """.trimIndent()

    val result = ConsoleParser.parse(script)
    assertEquals(0, result.errorCount)
    assertEquals(3, result.validTasks.size)

    val t1 = result.validTasks[0]
    assertTrue(t1.isHighPriority)

    val t2 = result.validTasks[1]
    assertFalse(t2.isHighPriority)

    val t3 = result.validTasks[2]
    assertFalse(t3.isHighPriority)
  }

  @Test
  fun testZeroAndShortDurationDynamicTasksScheduleWithoutCrash() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    // Gap between Fajr (05:00-05:35 -> 335m) and Dhuhr (13:15 -> 795m)
    val zeroSignalTask = RoutineTask(
      id = "signal_0m",
      title = "Medication Ping",
      type = TaskType.DYNAMIC,
      startTimeMinute = 420, // 07:00 AM
      durationMinutes = 0,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val shortFlexTask = RoutineTask(
      id = "short_5m",
      title = "Quick Stretch",
      type = TaskType.DYNAMIC,
      startTimeMinute = 450, // 07:30 AM
      durationMinutes = 5,
      toleranceMinutes = 10,
      customFlexibilityMinutes = 10,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    val result = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(zeroSignalTask, shortFlexTask),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )

    val scheduledZero = result.scheduledTasks.firstOrNull { it.id == "signal_0m" }
    val scheduledShort = result.scheduledTasks.firstOrNull { it.id == "short_5m" }
    assertNotNull("0-minute signal task should be scheduled", scheduledZero)
    assertEquals(0, scheduledZero!!.durationMinutes)
    assertEquals(420, scheduledZero.startTimeMinute)
    assertNotNull("<15m task with tolerance should schedule cleanly", scheduledShort)
    assertEquals(5, scheduledShort!!.durationMinutes)
  }

  @Test
  fun testFluidTwinCustomShortSessionsScheduleCleanly() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    val twinId = "fluid_test1234"
    val p1 = RoutineTask(
      id = "${twinId}_p1",
      title = "Twin Morning",
      type = TaskType.DYNAMIC,
      startTimeMinute = 360, // 06:00 AM
      durationMinutes = 25,  // < 45m custom duration
      isFluidTwin = true,
      fluidTwinId = twinId,
      fluidTwinPart = 1,
      fluidTotalPoolMinutes = 50,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val p2 = RoutineTask(
      id = "${twinId}_p2",
      title = "Twin Evening",
      type = TaskType.DYNAMIC,
      startTimeMinute = 1120, // 06:40 PM
      durationMinutes = 25,
      isFluidTwin = true,
      fluidTwinId = twinId,
      fluidTwinPart = 2,
      fluidTotalPoolMinutes = 50,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    val result = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(p1, p2),
      activeDay = RoutineDay.MONDAY
    )

    assertEquals(0, result.overflowTasks.size)
    assertTrue(result.scheduledTasks.any { it.id == "${twinId}_p1" })
    assertTrue(result.scheduledTasks.any { it.id == "${twinId}_p2" })
  }

  @Test
  fun testConsoleParserCategoryRoundTripAndZeroDuration() {
    val script = """
      Signal Reminder | 0 | 07:00 | N | ALL | Y | Take vitamins | 0 | 0 | N | eating
      Default Category Task | 30 | 09:00 | Y | Mon,Wed | N | Focus | 5 | 0 | Y
    """.trimIndent()

    val parsed = ConsoleParser.parse(script)
    assertEquals(0, parsed.errorCount)
    assertEquals(2, parsed.validTasks.size)

    val t1 = parsed.validTasks[0]
    assertEquals(0, t1.durationMinutes)
    assertEquals("eating", t1.category)

    val t2 = parsed.validTasks[1]
    assertEquals("general", t2.category)

    val exported = ConsoleParser.generateRoutineScript(
      anchors = emptyList(),
      dynamicTasks = parsed.validTasks,
      sleepMinutes = 360,
      bufferMinutes = 0
    )
    assertTrue(exported.contains("| eating"))
    assertTrue(exported.contains("| general"))

    val reparsed = ConsoleParser.parse(exported)
    assertEquals(0, reparsed.errorCount)
    assertEquals(2, reparsed.validTasks.size)
    assertEquals("eating", reparsed.validTasks[0].category)
    assertEquals(0, reparsed.validTasks[0].durationMinutes)
  }

  @Test
  fun testConsoleParserV2TwinAnchorDynamicRoundTripAndSafetyFloorValidation() {
    val script = """
      ANCHOR | 2 | Fajr | 05:00 | 35 | Y | 8 | Y | ফজরের সময় হয়েছে। | Sat,Sun,Mon,Tue,Wed,Thu,Fri | N | 85
      TWIN | 20 | Quick Sprint A | 07:00 | 10 | Part 1 go | Quick Sprint B | 14:00 | 10 | Part 2 go | Y | Mon,Wed | 5 | 2 | studying | 5 | 8 | Y | N | 90 | 40 | N
      DYNAMIC | Voice Custom | 30 | 09:00 | Y | ALL | Y | Go now | 10 | 5 | Y | work | 65 | N
    """.trimIndent()

    val parsed = ConsoleParser.parse(script)
    assertEquals(0, parsed.errorCount)
    assertEquals(1, parsed.parsedAnchors.size)
    assertEquals(3, parsed.validTasks.size)

    val anchor2 = parsed.parsedAnchors[0]
    assertEquals("anchor_slot_2", anchor2.id)
    assertFalse(anchor2.isSoundAlertEnabled)
    assertEquals(0.85f, anchor2.voiceVolume, 0.01f)

    val twinP1 = parsed.validTasks[0]
    val twinP2 = parsed.validTasks[1]
    assertEquals("studying", twinP1.category)
    assertEquals(5, twinP1.fluidSafetyFloorMinutes)
    assertEquals(8, twinP2.fluidSafetyFloorMinutes)
    assertTrue(twinP1.isVoiceEnabled)
    assertFalse(twinP2.isVoiceEnabled)
    assertEquals(0.90f, twinP1.voiceVolume, 0.01f)
    assertEquals(0.40f, twinP2.voiceVolume, 0.01f)
    assertFalse(twinP1.isSoundAlertEnabled)

    val dyn = parsed.validTasks[2]
    assertEquals(0.65f, dyn.voiceVolume, 0.01f)
    assertFalse(dyn.isSoundAlertEnabled)

    val exported = ConsoleParser.generateRoutineScript(
      anchors = parsed.parsedAnchors,
      dynamicTasks = parsed.validTasks,
      sleepMinutes = 360,
      bufferMinutes = 0
    )
    val reparsed = ConsoleParser.parse(exported)
    assertEquals(0, reparsed.errorCount)
    assertEquals(3, reparsed.validTasks.size)
    assertEquals(5, reparsed.validTasks[0].fluidSafetyFloorMinutes)
    assertEquals(8, reparsed.validTasks[1].fluidSafetyFloorMinutes)
    assertFalse(reparsed.validTasks[1].isVoiceEnabled)
    assertEquals(0.65f, reparsed.validTasks[2].voiceVolume, 0.01f)

    // Invalid safety floor (> duration) should trigger diagnostic error
    val invalidTwin = "TWIN | 20 | A | 07:00 | 10 | Go | B | 14:00 | 10 | Go | N | ALL | 0 | 0 | work | 15 | 5"
    val invalidParsed = ConsoleParser.parse(invalidTwin)
    assertTrue(invalidParsed.errorCount > 0)
  }

  @Test
  fun testFluidTwinRespectsCustomSafetyFloorAndBufferWithoutHard45mFloor() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    // Gap between Maghrib (18:00-18:30 -> 1080..1110) and Isha (20:00-20:35 -> 1200..1235) is 90 minutes (1110..1200)
    // Put a blocking 60m high priority task from 18:30 to 19:30 (1110..1170), leaving 30m (1170..1200)
    val blocker = RoutineTask(
      id = "blocker_1",
      title = "Evening Blocker",
      type = TaskType.DYNAMIC,
      startTimeMinute = 1110,
      durationMinutes = 60,
      isHighPriority = true,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    // Twin with P1=20m (1170..1190, floor=15, buffer=5) and P2=20m (06:00, floor=10, buffer=0)
    // Remaining gap 1170..1200 = 30m. With 5m buffer needed before Isha, P1 gets 25m cap (>= 15m floor, fits 20m!).
    val twinId = "fluid_floor_ok"
    val p1 = RoutineTask(
      id = "${twinId}_p1",
      title = "Evening Twin",
      type = TaskType.DYNAMIC,
      startTimeMinute = 1170,
      durationMinutes = 20,
      isFluidTwin = true,
      fluidTwinId = twinId,
      fluidTwinPart = 1,
      fluidTotalPoolMinutes = 40,
      fluidSafetyFloorMinutes = 15,
      interTaskBufferMinutes = 5,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val p2 = RoutineTask(
      id = "${twinId}_p2",
      title = "Morning Twin",
      type = TaskType.DYNAMIC,
      startTimeMinute = 360,
      durationMinutes = 20,
      isFluidTwin = true,
      fluidTwinId = twinId,
      fluidTwinPart = 2,
      fluidTotalPoolMinutes = 40,
      fluidSafetyFloorMinutes = 10,
      interTaskBufferMinutes = 5,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    val result = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(blocker, p1, p2),
      activeDay = RoutineDay.MONDAY
    )
    assertEquals(0, result.overflowTasks.size)
    assertTrue(result.scheduledTasks.any { it.id == p1.id && it.durationMinutes == 20 })
    assertTrue(result.scheduledTasks.any { it.id == p2.id && it.durationMinutes == 20 })

    // Now test high safety floor causing clean overflow instead of violating floor
    val p1HighFloor = p1.copy(durationMinutes = 35, fluidTotalPoolMinutes = 55, fluidSafetyFloorMinutes = 30)
    val overflowResult = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(blocker, p1HighFloor, p2.copy(fluidTotalPoolMinutes = 55)),
      activeDay = RoutineDay.MONDAY
    )
    assertTrue(
      "Twin exceeding available cap above its safety floor must not violate floor",
      overflowResult.scheduledTasks.none { it.id == p1HighFloor.id && it.durationMinutes < 30 }
    )
  }

  @Test
  fun testSchedulerBirthTimeAutoReturnWhenBlockerRemoved() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    // Fajr ends at 05:35 AM (335m). Place blocker at 335..395 so birthStart=335 has no earlier gap and must shift to >= 395.
    val blocker = RoutineTask(
      id = "hp_blocker",
      title = "Blocking Task",
      type = TaskType.DYNAMIC,
      startTimeMinute = 335, // 05:35 AM
      durationMinutes = 60,
      isHighPriority = true,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val displacedTask = RoutineTask(
      id = "birth_task",
      title = "Deep Reading",
      type = TaskType.DYNAMIC,
      startTimeMinute = 335, // Born at 05:35 AM
      originalStartTimeMinute = 335,
      durationMinutes = 45,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    val withBlocker = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(blocker, displacedTask),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    val pushed = withBlocker.scheduledTasks.firstOrNull { it.id == "birth_task" }
    assertNotNull(pushed)
    assertTrue("Task should be pushed past blocker", pushed!!.startTimeMinute >= 395)
    assertEquals("Birth start time must be preserved on scheduled copy", 335, pushed.originalStartTimeMinute)

    // Remove blocker and reschedule using the displaced task (whose startTimeMinute reflects pushed time)
    val withoutBlocker = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(pushed),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    val returned = withoutBlocker.scheduledTasks.firstOrNull { it.id == "birth_task" }
    assertNotNull(returned)
    assertEquals("Task must snap back to its birth time when blocker is removed", 335, returned!!.startTimeMinute)
    assertEquals(335, returned.originalStartTimeMinute)
  }

  @Test
  fun testFluidTwinBirthTimePreservedAcrossDisplacement() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    val twinId = "fluid_birth_test"
    // Dhuhr is 13:15-13:50 (795..830) Sat-Thu, but Friday Jumu'ah is 12:30-14:30 (750..870).
    // Birth at 12:40 PM (760m, 30m) is displaced on Friday, then snaps back to 760m on Saturday.
    val p1 = RoutineTask(
      id = "${twinId}_p1",
      title = "Twin A",
      type = TaskType.DYNAMIC,
      startTimeMinute = 760,
      originalStartTimeMinute = 760,
      durationMinutes = 30,
      isFluidTwin = true,
      fluidTwinId = twinId,
      fluidTwinPart = 1,
      fluidTotalPoolMinutes = 60,
      selectedDays = RoutineDay.allDays()
    )
    val p2 = RoutineTask(
      id = "${twinId}_p2",
      title = "Twin B",
      type = TaskType.DYNAMIC,
      startTimeMinute = 1120, // 06:40 PM
      originalStartTimeMinute = 1120,
      durationMinutes = 30,
      isFluidTwin = true,
      fluidTwinId = twinId,
      fluidTwinPart = 2,
      fluidTotalPoolMinutes = 60,
      selectedDays = RoutineDay.allDays()
    )

    val fridaySchedule = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(p1, p2),
      activeDay = RoutineDay.FRIDAY
    )
    val displacedP1 = fridaySchedule.scheduledTasks.first { it.id == "${twinId}_p1" }
    assertTrue("Friday Jumu'ah should displace 12:40 PM twin", displacedP1.startTimeMinute != 760)
    assertEquals(760, displacedP1.originalStartTimeMinute)

    val saturdaySchedule = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(displacedP1, p2),
      activeDay = RoutineDay.SATURDAY
    )
    val returnedP1 = saturdaySchedule.scheduledTasks.first { it.id == "${twinId}_p1" }
    assertEquals("Fluid Twin must snap back to birth time on Saturday", 760, returnedP1.startTimeMinute)
    assertEquals(760, returnedP1.originalStartTimeMinute)
  }

  @Test
  fun testGenerateRoutineScriptIsHeaderlessAndExportsBirthTimes() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    val shiftedDynamic = RoutineTask(
      id = "shifted_dyn",
      title = "Birth Export Task",
      type = TaskType.DYNAMIC,
      startTimeMinute = 720, // Currently displaced to 12:00
      originalStartTimeMinute = 600, // Born at 10:00
      durationMinutes = 45,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    val exported = ConsoleParser.generateRoutineScript(
      anchors = anchors,
      dynamicTasks = listOf(shiftedDynamic),
      sleepMinutes = 360,
      bufferMinutes = 0
    )

    val lines = exported.lines()
    assertTrue("Exported script should not be empty", lines.isNotEmpty())
    assertFalse("Exported script must not contain # comment lines", lines.any { it.trimStart().startsWith("#") })
    assertFalse("Exported script must not contain blank lines", lines.any { it.isBlank() })
    assertTrue("Dynamic task line must export birth time 10:00", exported.contains("Birth Export Task | 45 | 10:00"))

    val reparsed = ConsoleParser.parse(exported)
    assertEquals(0, reparsed.errorCount)
    assertEquals(7, reparsed.parsedAnchors.size)
    val reparsedTask = reparsed.validTasks.first { it.title == "Birth Export Task" }
    assertEquals(600, reparsedTask.startTimeMinute)
    assertEquals(600, reparsedTask.originalStartTimeMinute)
  }

  @Test
  fun testAutomationManagerContainsBengaliDetection() {
    assertTrue(com.example.service.AutomationManager.containsBengali("ফজরের সময় হয়েছে।"))
    assertTrue(com.example.service.AutomationManager.containsBengali("Morning ফজর"))
    assertFalse(com.example.service.AutomationManager.containsBengali("Deep Work Session 123!"))
    assertFalse(com.example.service.AutomationManager.containsBengali(""))
  }

  @Test
  fun testAutomationManagerVoicePresetsAndFallback() {
    val presets = com.example.service.AutomationManager.VOICE_PRESETS
    assertEquals(12, presets.size)
    assertEquals(
      listOf(
        "natural_female", "calm_female", "sweet_melodic_female", "executive_female", "night_sanctuary_female",
        "teen_girl", "cheerful_kid_girl", "teen_boy",
        "deep_male", "crisp_male", "mentor_male", "neutral_studio"
      ),
      presets.map { it.id }
    )
    assertEquals(5, presets.count { it.category == "female" })
    assertEquals(3, presets.count { it.category == "youth" })
    assertEquals(4, presets.count { it.category == "male" })
    assertTrue(
      "Each preset should have a non-blank label, description, and defaultSampleText",
      presets.all { it.label.isNotBlank() && it.description.isNotBlank() && it.defaultSampleText.isNotBlank() }
    )

    val teenBoy = com.example.service.AutomationManager.getVoicePreset("teen_boy")
    assertEquals("youth", teenBoy.category)
    assertEquals("male", teenBoy.preferredGender)
    assertEquals(1.34f, teenBoy.nativeMalePitch ?: 0f, 0.001f)

    val deepMale = com.example.service.AutomationManager.getVoicePreset("deep_male")
    assertEquals("deep_male", deepMale.id)
    assertEquals("male", deepMale.preferredGender)

    val fallback = com.example.service.AutomationManager.getVoicePreset("nonexistent_preset")
    assertEquals("natural_female", fallback.id)
  }

  @Test
  fun testConsoleParserDndDelaySyncForDynamicAndTwin() {
    val script = """
      DYNAMIC | Delayed Focus | 45 | 09:00 | Y | ALL | Y | Go | 10 | 5 | Y | work | 80 | Y | 12
      DYNAMIC | Clamped & Disabled DND | 30 | 11:00 | N | ALL | Y | Wait | 0 | 0 | N | general | 100 | Y | 20
      TWIN | 60 | Twin A | 07:00 | 30 | Go A | Twin B | 14:00 | 30 | Go B | Y | Mon,Wed | 5 | 2 | studying | 10 | 10 | Y | Y | 90 | 90 | Y | 15
      TWIN | 40 | Twin Off A | 15:00 | 20 | Off A | Twin Off B | 16:00 | 20 | Off B | N | Tue | 0 | 0 | work | 0 | 0 | Y | Y | 100 | 100 | Y | 18
    """.trimIndent()

    val parsed = ConsoleParser.parse(script)
    assertEquals(0, parsed.errorCount)
    assertEquals(6, parsed.validTasks.size)

    val dynDelayed = parsed.validTasks[0]
    assertTrue(dynDelayed.enableDnd)
    assertEquals(12, dynDelayed.dndDelayMinutes)

    val dynDisabled = parsed.validTasks[1]
    assertFalse(dynDisabled.enableDnd)
    assertEquals(0, dynDisabled.dndDelayMinutes)

    val twinA = parsed.validTasks[2]
    val twinB = parsed.validTasks[3]
    assertTrue(twinA.enableDnd)
    assertEquals(15, twinA.dndDelayMinutes)
    assertEquals(15, twinB.dndDelayMinutes)

    val twinOffA = parsed.validTasks[4]
    assertFalse(twinOffA.enableDnd)
    assertEquals(0, twinOffA.dndDelayMinutes)

    val exported = ConsoleParser.generateRoutineScript(
      anchors = emptyList(),
      dynamicTasks = parsed.validTasks,
      sleepMinutes = 360,
      bufferMinutes = 0
    )
    assertTrue(exported.contains("| 80 | Y | 12"))
    assertTrue(exported.contains("| 90 | 90 | Y | 15"))

    val reparsed = ConsoleParser.parse(exported)
    assertEquals(0, reparsed.errorCount)
    assertEquals(12, reparsed.validTasks[0].dndDelayMinutes)
    assertEquals(0, reparsed.validTasks[1].dndDelayMinutes)
    assertEquals(15, reparsed.validTasks[2].dndDelayMinutes)
    assertEquals(15, reparsed.validTasks[3].dndDelayMinutes)
    assertEquals(0, reparsed.validTasks[4].dndDelayMinutes)
  }

  @Test
  fun testConsoleParserUnlocksDndDelayForSlot1AndSlot5() {
    val script = """
      ANCHOR | 1 | Night Sleep | 23:00 | 360 | Y | 15 | Y | Sleep now
      ANCHOR | 5 | Break | 16:55 | 65 | Y | 20 | Y | Break time
      ANCHOR | 2 | Fajr | 05:00 | 35 | N | 12 | Y | Fajr
    """.trimIndent()

    val parsed = ConsoleParser.parse(script)
    assertEquals(0, parsed.errorCount)
    assertEquals(3, parsed.parsedAnchors.size)

    val slot1 = parsed.parsedAnchors.first { it.slotNumber == 1 }
    val slot5 = parsed.parsedAnchors.first { it.slotNumber == 5 }
    val slot2 = parsed.parsedAnchors.first { it.slotNumber == 2 }

    assertTrue(slot1.enableDnd)
    assertEquals("Slot 1 must preserve custom DND delay when DND is enabled", 15, slot1.dndDelayMinutes)
    assertTrue(slot5.enableDnd)
    assertEquals("Slot 5 must preserve custom DND delay when DND is enabled", 20, slot5.dndDelayMinutes)
    assertFalse(slot2.enableDnd)
    assertEquals("Disabled DND must zero out dndDelayMinutes", 0, slot2.dndDelayMinutes)
  }

  @Test
  fun testSchedulerHorizontalScrubBoundsProtectsAnchorsAndNeighbors() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    // Anchor 2 ends at 05:35 (335m); Anchor 3 starts at 13:15 (795m)
    val neighbor = RoutineTask(
      id = "neighbor_1",
      title = "Morning Blocker",
      type = TaskType.DYNAMIC,
      startTimeMinute = 480, // 08:00 AM
      durationMinutes = 60,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val target = RoutineTask(
      id = "scrub_target",
      title = "Scrubbable Task",
      type = TaskType.DYNAMIC,
      startTimeMinute = 390, // 06:30 AM
      durationMinutes = 40,
      interTaskBufferMinutes = 10, // needs 50m block total
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val scheduled = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(target, neighbor),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    ).scheduledTasks

    val scheduledTarget = scheduled.first { it.id == "scrub_target" }
    val bounds = com.example.engine.SchedulerEngine.computeHorizontalScrubBounds(
      task = scheduledTarget,
      scheduledTasks = scheduled,
      anchors = anchors,
      day = RoutineDay.MONDAY
    )

    assertEquals("Min bound must respect Slot 2 end (335m)", 335, bounds.first)
    assertEquals("Max bound must stop before neighbor at 480m leaving 40m + 10m buffer", 430, bounds.last)
  }

  @Test
  fun testSchedulerDragRepositionFencesHighPriorityAndFluidTwinsAndAllowsNormalCrossAnchor() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    // Gap 2-3: 335..795; Gap 3-4: 830..990 (Monday)
    val highPriority = RoutineTask(
      id = "hp_drag",
      title = "High Priority Focus",
      type = TaskType.DYNAMIC,
      startTimeMinute = 420,
      originalStartTimeMinute = 420,
      durationMinutes = 45,
      isHighPriority = true,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val fluidTwin = RoutineTask(
      id = "twin_drag_p1",
      title = "Twin Focus",
      type = TaskType.DYNAMIC,
      startTimeMinute = 480,
      originalStartTimeMinute = 480,
      durationMinutes = 30,
      isFluidTwin = true,
      fluidTwinId = "twin_drag",
      fluidTwinPart = 1,
      fluidTotalPoolMinutes = 60,
      fluidSafetyFloorMinutes = 15,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val normalTask = RoutineTask(
      id = "norm_drag",
      title = "Normal Flexible",
      type = TaskType.DYNAMIC,
      startTimeMinute = 540,
      originalStartTimeMinute = 540,
      durationMinutes = 30,
      isHighPriority = false,
      isFluidTwin = false,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val list = listOf(highPriority, fluidTwin, normalTask)

    // Attempt to drag High Priority past Slot 3 (into 860m = 14:20) -> fenced inside 335..795
    val (hpUpdated, hpHitWall) = com.example.engine.SchedulerEngine.applyDragReposition(
      allDynamicTasks = list,
      taskId = "hp_drag",
      targetStartMinute = 860,
      newManualOrder = 100L,
      anchors = anchors,
      day = RoutineDay.MONDAY
    )
    assertTrue("High-Priority task must signal Anchor Wall hit", hpHitWall)
    assertEquals(750, hpUpdated.first { it.id == "hp_drag" }.startTimeMinute) // 795 - 45

    // Attempt to drag single Fluid Twin (no active partner in Gap 3-4) past Slot 3 into 860m -> allowed across anchors!
    val (twinUpdated, twinHitWall) = com.example.engine.SchedulerEngine.applyDragReposition(
      allDynamicTasks = list,
      taskId = "twin_drag_p1",
      targetStartMinute = 860,
      newManualOrder = 101L,
      anchors = anchors,
      day = RoutineDay.MONDAY
    )
    assertFalse("Fluid Twin without partner in destination gap can cross anchors freely", twinHitWall)
    assertEquals(860, twinUpdated.first { it.id == "twin_drag_p1" }.startTimeMinute)

    // Attempt to drag Normal task across Slot 3 into Gap 3-4 (860m) -> allowed across anchors
    val (normUpdated, normHitWall) = com.example.engine.SchedulerEngine.applyDragReposition(
      allDynamicTasks = list,
      taskId = "norm_drag",
      targetStartMinute = 860,
      newManualOrder = 102L,
      anchors = anchors,
      day = RoutineDay.MONDAY
    )
    assertFalse("Normal task can jump across anchors without hitting fence", normHitWall)
    val movedNorm = normUpdated.first { it.id == "norm_drag" }
    assertEquals(860, movedNorm.startTimeMinute)
    assertEquals(102L, movedNorm.manualPlacementOrder)
  }

  @Test
  fun testSchedulerComputeScheduleImpactDiffReportsShiftsAndOverflow() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    val t1 = RoutineTask(
      id = "impact_1",
      title = "First Sprint",
      type = TaskType.DYNAMIC,
      startTimeMinute = 420,
      originalStartTimeMinute = 420,
      durationMinutes = 60,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val t2 = RoutineTask(
      id = "impact_2",
      title = "Second Sprint",
      type = TaskType.DYNAMIC,
      startTimeMinute = 500,
      originalStartTimeMinute = 500,
      durationMinutes = 45,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    val baseline = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(t1, t2),
      activeDay = RoutineDay.MONDAY
    )

    // Move t1 right onto 500m with higher manualPlacementOrder so t2 shifts around it
    val movedList = listOf(
      t1.copy(startTimeMinute = 500, originalStartTimeMinute = 500, manualPlacementOrder = 999L),
      t2
    )
    val draft = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = movedList,
      activeDay = RoutineDay.MONDAY
    )

    val diffs = com.example.engine.SchedulerEngine.computeScheduleImpactDiff(baseline, draft)
    assertTrue("Impact diff must detect both moved and displaced tasks", diffs.size >= 2)
    val diff1 = diffs.first { it.taskId == "impact_1" }
    val diff2 = diffs.first { it.taskId == "impact_2" }
    assertEquals(420, diff1.oldStartMinute)
    assertEquals(500, diff1.newStartMinute)
    assertEquals(500, diff2.oldStartMinute)
    assertTrue("Displaced task should no longer start at 500", diff2.newStartMinute != 500)
  }

  @Test
  fun testComputeNetFreeGapMinutesExcludesInterTaskBuffer() {
    val taskWithBuffer = RoutineTask(
      id = "gap_buf_1",
      title = "Buffered Work",
      type = TaskType.DYNAMIC,
      startTimeMinute = 360, // 06:00 AM
      durationMinutes = 60,  // Ends 07:00 AM (420m)
      interTaskBufferMinutes = 15, // Effective end = 435m (07:15 AM)
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val nextExactAfterBuffer = RoutineTask(
      id = "gap_buf_2",
      title = "Immediately After Buffer",
      type = TaskType.DYNAMIC,
      startTimeMinute = 435, // 07:15 AM -> distance 15m covered completely by buffer
      durationMinutes = 30,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val nextOverlap = nextExactAfterBuffer.copy(id = "gap_buf_3", startTimeMinute = 430)
    val nextWithRealGap = nextExactAfterBuffer.copy(id = "gap_buf_4", startTimeMinute = 495) // 08:15 AM -> 60m net gap

    assertEquals(
      "Gap completely covered by interTaskBufferMinutes must return 0",
      0,
      com.example.engine.SchedulerEngine.computeNetFreeGapMinutes(taskWithBuffer, nextExactAfterBuffer)
    )
    assertEquals(
      0,
      com.example.engine.SchedulerEngine.computeNetFreeGapMinutes(taskWithBuffer, nextOverlap)
    )
    assertEquals(
      "Net free gap must subtract reserved buffer time cleanly",
      60,
      com.example.engine.SchedulerEngine.computeNetFreeGapMinutes(taskWithBuffer, nextWithRealGap)
    )
  }

  @Test
  fun testApplyVerticalDropRepositionSwapsEatAndBathInBothDirections() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    // Gap 2-3: 335..795
    val eat = RoutineTask(
      id = "eat_task",
      title = "Eat",
      type = TaskType.DYNAMIC,
      startTimeMinute = 420, // 07:00 - 08:00
      originalStartTimeMinute = 420,
      durationMinutes = 60,
      interTaskBufferMinutes = 0,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val bath = RoutineTask(
      id = "bath_task",
      title = "Bath",
      type = TaskType.DYNAMIC,
      startTimeMinute = 480, // 08:00 - 08:30 (+10m buffer)
      originalStartTimeMinute = 480,
      durationMinutes = 30,
      interTaskBufferMinutes = 10,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    val baseline = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(eat, bath),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )

    // Direction 1: Drag UP (Bath InsertBeforeTask Eat) -> [Bath 420..450, Eat 460..520]
    val (swappedUpList, hitWallUp) = com.example.engine.SchedulerEngine.applyVerticalDropReposition(
      allDynamicTasks = listOf(eat, bath),
      scheduledTasks = baseline.scheduledTasks,
      taskId = "bath_task",
      dropTarget = com.example.engine.VerticalDropTarget.InsertBeforeTask("eat_task"),
      baseManualOrder = 500L,
      anchors = anchors,
      day = RoutineDay.MONDAY
    )
    assertFalse(hitWallUp)
    val scheduledUp = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = swappedUpList,
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    val upBath = scheduledUp.scheduledTasks.first { it.id == "bath_task" }
    val upEat = scheduledUp.scheduledTasks.first { it.id == "eat_task" }
    assertEquals(420, upBath.startTimeMinute)
    assertEquals(460, upEat.startTimeMinute)

    // Direction 2: Drag DOWN (Eat InsertAfterTask Bath) -> [Bath 420..450, Eat 460..520]
    val (swappedDownList, hitWallDown) = com.example.engine.SchedulerEngine.applyVerticalDropReposition(
      allDynamicTasks = listOf(eat, bath),
      scheduledTasks = baseline.scheduledTasks,
      taskId = "eat_task",
      dropTarget = com.example.engine.VerticalDropTarget.InsertAfterTask("bath_task"),
      baseManualOrder = 600L,
      anchors = anchors,
      day = RoutineDay.MONDAY
    )
    assertFalse(hitWallDown)
    val scheduledDown = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = swappedDownList,
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    val downBath = scheduledDown.scheduledTasks.first { it.id == "bath_task" }
    val downEat = scheduledDown.scheduledTasks.first { it.id == "eat_task" }
    assertEquals(420, downBath.startTimeMinute)
    assertEquals(460, downEat.startTimeMinute)

    val impactDiff = com.example.engine.SchedulerEngine.computeScheduleImpactDiff(baseline, scheduledDown)
    assertTrue("Impact diff must report both swapped Eat and Bath tasks", impactDiff.any { it.taskId == "bath_task" } && impactDiff.any { it.taskId == "eat_task" })
  }

  @Test
  fun testApplyVerticalDropRepositionCrossAnchorCascadingAndFencing() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    // Gap 2-3: 335..795; Gap 3-4: 830..990 (Monday)
    val highPriority = RoutineTask(
      id = "hp_vdrop",
      title = "Locked High Priority",
      type = TaskType.DYNAMIC,
      startTimeMinute = 420,
      originalStartTimeMinute = 420,
      durationMinutes = 45,
      isHighPriority = true,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val fluidTwin = RoutineTask(
      id = "twin_vdrop_p1",
      title = "Locked Fluid Twin",
      type = TaskType.DYNAMIC,
      startTimeMinute = 500,
      originalStartTimeMinute = 500,
      durationMinutes = 30,
      isFluidTwin = true,
      fluidTwinId = "twin_vdrop",
      fluidTwinPart = 1,
      fluidTotalPoolMinutes = 60,
      fluidSafetyFloorMinutes = 15,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val normalTask = RoutineTask(
      id = "norm_vdrop",
      title = "Movable Task",
      type = TaskType.DYNAMIC,
      startTimeMinute = 600,
      originalStartTimeMinute = 600,
      durationMinutes = 30,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val afternoonTarget = RoutineTask(
      id = "gap34_blocker",
      title = "Afternoon Session",
      type = TaskType.DYNAMIC,
      startTimeMinute = 840, // 02:00 PM in Gap 3-4 (830..990)
      originalStartTimeMinute = 840,
      durationMinutes = 45,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    val allTasks = listOf(highPriority, fluidTwin, normalTask, afternoonTarget)
    val baseline = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = allTasks,
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )

    // 1. High priority vertical drop across Slot 3 -> clamped inside Gap 2-3 with hitAnchorWall = true
    val (hpUpdated, hpHitWall) = com.example.engine.SchedulerEngine.applyVerticalDropReposition(
      allDynamicTasks = allTasks,
      scheduledTasks = baseline.scheduledTasks,
      taskId = "hp_vdrop",
      dropTarget = com.example.engine.VerticalDropTarget.InsertBeforeTask("gap34_blocker"),
      baseManualOrder = 100L,
      anchors = anchors,
      day = RoutineDay.MONDAY
    )
    assertTrue("High-priority cross-anchor vertical drop must hit Anchor Wall", hpHitWall)
    val hpEnd = hpUpdated.first { it.id == "hp_vdrop" }.startTimeMinute + 45
    assertTrue("High-priority must remain inside Gap 2-3 (<= 795)", hpEnd <= 795)

    // 2. Single Fluid Twin (no partner in Gap 3-4) vertical drop across Slot 3 -> allowed cross-anchor!
    val (twinUpdated, twinHitWall) = com.example.engine.SchedulerEngine.applyVerticalDropReposition(
      allDynamicTasks = allTasks,
      scheduledTasks = baseline.scheduledTasks,
      taskId = "twin_vdrop_p1",
      dropTarget = com.example.engine.VerticalDropTarget.InsertBeforeTask("gap34_blocker"),
      baseManualOrder = 200L,
      anchors = anchors,
      day = RoutineDay.MONDAY
    )
    assertFalse("Fluid Twin without partner in destination gap can cross anchors vertically", twinHitWall)
    assertEquals(840, twinUpdated.first { it.id == "twin_vdrop_p1" }.startTimeMinute)

    // 3. Normal task cross-anchor drop before gap34_blocker -> lands at 840m and cascades afternoonTarget down!
    val (normUpdated, normHitWall) = com.example.engine.SchedulerEngine.applyVerticalDropReposition(
      allDynamicTasks = allTasks,
      scheduledTasks = baseline.scheduledTasks,
      taskId = "norm_vdrop",
      dropTarget = com.example.engine.VerticalDropTarget.InsertBeforeTask("gap34_blocker"),
      baseManualOrder = 300L,
      anchors = anchors,
      day = RoutineDay.MONDAY
    )
    assertFalse("Normal task can cross anchors freely", normHitWall)
    val reScheduled = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = normUpdated,
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    val movedNorm = reScheduled.scheduledTasks.first { it.id == "norm_vdrop" }
    val cascadedTarget = reScheduled.scheduledTasks.first { it.id == "gap34_blocker" }
    assertEquals(840, movedNorm.startTimeMinute)
    assertTrue(
      "Existing afternoon task at 840m must cascade past dropped normal task (>= 870m)",
      cascadedTarget.startTimeMinute >= 870
    )
  }

  @Test
  fun testLiveNetFreeGapRecalculationDuringHorizontalScrub() {
    val prevTask = RoutineTask(
      id = "scrub_prev",
      title = "Prev Session",
      type = TaskType.DYNAMIC,
      startTimeMinute = 360, // 06:00 - 07:00
      durationMinutes = 60,
      interTaskBufferMinutes = 0,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val middleTask = RoutineTask(
      id = "scrub_mid",
      title = "Sliding Task",
      type = TaskType.DYNAMIC,
      startTimeMinute = 420, // 07:00 - 07:30
      durationMinutes = 30,
      interTaskBufferMinutes = 0,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val nextTask = RoutineTask(
      id = "scrub_next",
      title = "Next Session",
      type = TaskType.DYNAMIC,
      startTimeMinute = 453, // 07:33 -> 3m gap below middleTask initially
      durationMinutes = 30,
      interTaskBufferMinutes = 0,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    // Forward horizontal scrub across 3m slack: bottom shrinks 3->2->1->0 while top grows 0->1->2->3
    val expectedTopForward = listOf(0, 1, 2, 3)
    val expectedBottomForward = listOf(3, 2, 1, 0)
    for (offset in 0..3) {
      val liveMid = middleTask.copy(startTimeMinute = 420 + offset)
      assertEquals(
        expectedTopForward[offset],
        com.example.engine.SchedulerEngine.computeNetFreeGapMinutes(prevTask, liveMid)
      )
      assertEquals(
        expectedBottomForward[offset],
        com.example.engine.SchedulerEngine.computeNetFreeGapMinutes(liveMid, nextTask)
      )
    }

    // Reverse horizontal scrub back: top shrinks 3->2->1->0 while bottom grows 0->1->2->3
    for (offset in 3 downTo 0) {
      val liveMid = middleTask.copy(startTimeMinute = 420 + offset)
      assertEquals(offset, com.example.engine.SchedulerEngine.computeNetFreeGapMinutes(prevTask, liveMid))
      assertEquals(3 - offset, com.example.engine.SchedulerEngine.computeNetFreeGapMinutes(liveMid, nextTask))
    }
  }

  @Test
  fun testApplyVerticalDropRepositionWideGapDownwardInsertBeforeTask() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    // Gap 2-3: 335..795
    val morningTask = RoutineTask(
      id = "wide_morning",
      title = "Morning Task",
      type = TaskType.DYNAMIC,
      startTimeMinute = 360, // 06:00 - 06:30
      originalStartTimeMinute = 360,
      durationMinutes = 30,
      interTaskBufferMinutes = 0,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val noonTask = RoutineTask(
      id = "wide_noon",
      title = "Noon Task",
      type = TaskType.DYNAMIC,
      startTimeMinute = 660, // 11:00 - 11:30 (270m wide gap after Morning Task)
      originalStartTimeMinute = 660,
      durationMinutes = 30,
      interTaskBufferMinutes = 0,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    val baseline1 = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(morningTask, noonTask),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )

    // Drag morningTask DOWN across 270m wide gap onto InsertBeforeTask(wide_noon):
    // Must land right BEFORE wide_noon at 10:30 (630m), NOT snap back to 06:00 (360m)!
    val (updatedOpenRoom, hitWall1) = com.example.engine.SchedulerEngine.applyVerticalDropReposition(
      allDynamicTasks = listOf(morningTask, noonTask),
      scheduledTasks = baseline1.scheduledTasks,
      taskId = "wide_morning",
      dropTarget = com.example.engine.VerticalDropTarget.InsertBeforeTask("wide_noon"),
      baseManualOrder = 800L,
      anchors = anchors,
      day = RoutineDay.MONDAY
    )
    assertFalse(hitWall1)
    val reschedOpen = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = updatedOpenRoom,
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    val movedMorning = reschedOpen.scheduledTasks.first { it.id == "wide_morning" }
    val keptNoon = reschedOpen.scheduledTasks.first { it.id == "wide_noon" }
    assertEquals("Wide-gap downward InsertBeforeTask must place task directly before target", 630, movedMorning.startTimeMinute)
    assertEquals("Noon target remains untouched when open room above fits", 660, keptNoon.startTimeMinute)

    // Now place a blocker at 10:35-10:50 (635..650), leaving only 10m above wide_noon (< 30m needed):
    // Dragging morningTask DOWN onto InsertBeforeTask(wide_noon) must place morningTask at 11:00 (660) and push noonTask to 11:30 (690).
    val blockerAboveNoon = RoutineTask(
      id = "pre_noon_blocker",
      title = "Pre-Noon Checkin",
      type = TaskType.DYNAMIC,
      startTimeMinute = 635,
      originalStartTimeMinute = 635,
      durationMinutes = 15,
      interTaskBufferMinutes = 0,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val threeTasks = listOf(morningTask, blockerAboveNoon, noonTask)
    val baseline2 = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = threeTasks,
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    val (updatedTightRoom, hitWall2) = com.example.engine.SchedulerEngine.applyVerticalDropReposition(
      allDynamicTasks = threeTasks,
      scheduledTasks = baseline2.scheduledTasks,
      taskId = "wide_morning",
      dropTarget = com.example.engine.VerticalDropTarget.InsertBeforeTask("wide_noon"),
      baseManualOrder = 900L,
      anchors = anchors,
      day = RoutineDay.MONDAY
    )
    assertFalse(hitWall2)
    val reschedTight = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = updatedTightRoom,
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    assertEquals(660, reschedTight.scheduledTasks.first { it.id == "wide_morning" }.startTimeMinute)
    assertEquals(690, reschedTight.scheduledTasks.first { it.id == "wide_noon" }.startTimeMinute)
  }

  @Test
  fun testFindAvailableGapsIntervalMergingHandlesWrapAroundTouchingAndZeroDuration() {
    val wrapAnchor = RoutineTask(
      id = "wrap_sleep",
      title = "Sleep",
      type = TaskType.ANCHOR_SLEEP,
      slotNumber = 1,
      startTimeMinute = 1380, // 23:00 -> wraps to 05:00 (300m)
      durationMinutes = 360
    )
    val zeroSignal = RoutineTask(
      id = "signal_0m",
      title = "Marker",
      type = TaskType.DYNAMIC,
      startTimeMinute = 600,
      durationMinutes = 0,
      interTaskBufferMinutes = 0
    )
    val t1 = RoutineTask(
      id = "touch_1",
      title = "Block 1",
      type = TaskType.DYNAMIC,
      startTimeMinute = 600,
      durationMinutes = 60,
      interTaskBufferMinutes = 10 // covers 600..670
    )
    val t2 = RoutineTask(
      id = "touch_2",
      title = "Block 2",
      type = TaskType.DYNAMIC,
      startTimeMinute = 670,
      durationMinutes = 50,
      interTaskBufferMinutes = 0 // covers 670..720 (touching Block 1)
    )

    val gaps = com.example.engine.SchedulerEngine.findAvailableGaps(
      committedTasks = listOf(wrapAnchor, zeroSignal, t1, t2),
      interTaskBufferMinutes = 0
    )
    assertEquals(2, gaps.size)
    assertEquals(300 to 600, gaps[0].startMinute to gaps[0].endMinute)
    assertEquals(720 to 1380, gaps[1].startMinute to gaps[1].endMinute)
  }

  @Test
  fun testGetInterAnchorWindowKeyRespectsFridaySlot3DisabledSlot5AndMidnightWrap() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    // Friday Slot 3 Jumu'ah is 12:30 (750) .. 14:30 (870)
    val monMorningKey = com.example.engine.SchedulerEngine.getInterAnchorWindowKey(anchors, 760, RoutineDay.MONDAY)
    val friJumuahAfterKey = com.example.engine.SchedulerEngine.getInterAnchorWindowKey(anchors, 880, RoutineDay.FRIDAY)
    assertEquals(335, monMorningKey) // On Monday, 760 is still inside Gap 2-3 (335..795)
    assertEquals(870, friJumuahAfterKey) // On Friday, Slot 3 ends at 870

    // Midnight wrap-around: 23:00 (1380) -> 05:00 (300); e.g. if Slot 1 is 1380 for 300m -> 04:00 (240),
    // or standard Slot 7 ends at 1235 (20:35) and Slot 1 starts at 1380 (23:00): [1235..1380) is separate from Slot 1..Slot 2.
    // Test Slot 1 disabled (so [1235..1440) and [0..300) merge across midnight):
    val noSleepAnchors = anchors.filterNot { it.slotNumber == 1 }
    val preMidnightKey = com.example.engine.SchedulerEngine.getInterAnchorWindowKey(noSleepAnchors, 1400, RoutineDay.MONDAY)
    val postMidnightKey = com.example.engine.SchedulerEngine.getInterAnchorWindowKey(noSleepAnchors, 60, RoutineDay.MONDAY)
    assertEquals("Wrap-around window across midnight shares canonical key", postMidnightKey, preMidnightKey)

    // Slot 5 disabled on Monday merges Gap 4-5 (1015..1015) and Gap 5-6 into single Gap 4-6 (1015..1080)
    val slot5DisabledAnchors = anchors.map {
      if (it.slotNumber == 5) it.copy(selectedDays = setOf(RoutineDay.TUESDAY)) else it
    }
    val kEarly = com.example.engine.SchedulerEngine.getInterAnchorWindowKey(slot5DisabledAnchors, 1020, RoutineDay.MONDAY)
    val kLate = com.example.engine.SchedulerEngine.getInterAnchorWindowKey(slot5DisabledAnchors, 1070, RoutineDay.MONDAY)
    assertEquals(1015, kEarly)
    assertEquals(kEarly, kLate)
  }

  @Test
  fun testFluidTwinCrossAnchorAllowedAndSameGapPartnerExclusionEnforced() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360).map {
      if (it.slotNumber == 5) it.copy(selectedDays = RoutineDay.allDays() - RoutineDay.MONDAY) else it
    }
    val twinP1 = RoutineTask(
      id = "ex_twin_p1",
      title = "Study A",
      type = TaskType.DYNAMIC,
      startTimeMinute = 420, // Gap 2-3 (335..795)
      originalStartTimeMinute = 420,
      durationMinutes = 40,
      isFluidTwin = true,
      fluidTwinId = "ex_twin",
      fluidTwinPart = 1,
      fluidTotalPoolMinutes = 80,
      fluidSafetyFloorMinutes = 15,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val twinP2 = RoutineTask(
      id = "ex_twin_p2",
      title = "Study B",
      type = TaskType.DYNAMIC,
      startTimeMinute = 840, // Gap 3-4 (830..990)
      originalStartTimeMinute = 840,
      durationMinutes = 40,
      isFluidTwin = true,
      fluidTwinId = "ex_twin",
      fluidTwinPart = 2,
      fluidTotalPoolMinutes = 80,
      fluidSafetyFloorMinutes = 15,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val blockerInGap46 = RoutineTask(
      id = "gap46_item",
      title = "Evening Drill",
      type = TaskType.DYNAMIC,
      startTimeMinute = 1020, // Inside merged Gap 4-6 (1015..1080) since Slot 5 is disabled on Monday
      originalStartTimeMinute = 1020,
      durationMinutes = 30,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    val all = listOf(twinP1, twinP2, blockerInGap46)
    val baseline = com.example.engine.SchedulerEngine.scheduleDay(anchors, all, RoutineDay.MONDAY)

    // 1. Drag P1 into Partner P2's gap (Gap 3-4, 840m) -> BLOCKED (Same-Gap Partner Exclusion)
    val (blockedDrop, blockedWall) = com.example.engine.SchedulerEngine.applyVerticalDropReposition(
      allDynamicTasks = all,
      scheduledTasks = baseline.scheduledTasks,
      taskId = "ex_twin_p1",
      dropTarget = com.example.engine.VerticalDropTarget.InsertBeforeTask("ex_twin_p2"),
      baseManualOrder = 100L,
      anchors = anchors,
      day = RoutineDay.MONDAY
    )
    assertTrue("Dropping Twin P1 into P2's gap must trigger Same-Gap Partner Exclusion", blockedWall)
    assertTrue(blockedDrop.first { it.id == "ex_twin_p1" }.startTimeMinute < 795)

    // 2. Drag P1 across Slot 3 & 4 into merged Gap 4-6 (no partner there) -> ALLOWED!
    val (allowedDrop, allowedWall) = com.example.engine.SchedulerEngine.applyVerticalDropReposition(
      allDynamicTasks = all,
      scheduledTasks = baseline.scheduledTasks,
      taskId = "ex_twin_p1",
      dropTarget = com.example.engine.VerticalDropTarget.InsertBeforeTask("gap46_item"),
      baseManualOrder = 200L,
      anchors = anchors,
      day = RoutineDay.MONDAY
    )
    assertFalse("Dropping Twin P1 into unoccupied Gap 4-6 must be allowed", allowedWall)
    assertEquals(1020, allowedDrop.first { it.id == "ex_twin_p1" }.startTimeMinute)

    // 3. Recovery: if BOTH start in the same gap, dragging P1 out into Gap 4-6 must be ALLOWED
    val bothInGap23 = listOf(
      twinP1,
      twinP2.copy(startTimeMinute = 500, originalStartTimeMinute = 500),
      blockerInGap46
    )
    val (_, recoveryWall) = com.example.engine.SchedulerEngine.applyDragReposition(
      allDynamicTasks = bothInGap23,
      taskId = "ex_twin_p1",
      targetStartMinute = 1020,
      newManualOrder = 300L,
      anchors = anchors,
      day = RoutineDay.MONDAY
    )
    assertFalse("Dragging twin out when both started in the same gap must succeed", recoveryWall)
  }

  @Test
  fun testScheduleDayPlacesHighPriorityBeforeFluidTwinsEnforcesSameGapExclusionAndLocalCompacts() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    // Gap 3-4: 830..990 (160m total).
    // Twin P1 at 420 (Gap 2-3); Twin P2 requests 830 (Gap 3-4) for 90m.
    // High Priority task (created AFTER Twin P2) requests 830 (Gap 3-4) for 80m.
    // Even though Twin P2 was earlier in list, High Priority must schedule BEFORE Twin P2 and claim 830..910;
    // Twin P2 compresses/places in remaining 910..990 (or moves cleanly)!
    val twinP1 = RoutineTask(
      id = "hp_vs_twin_p1",
      title = "Twin Part 1",
      type = TaskType.DYNAMIC,
      startTimeMinute = 420,
      durationMinutes = 60,
      isFluidTwin = true,
      fluidTwinId = "hp_vs_twin",
      fluidTwinPart = 1,
      fluidTotalPoolMinutes = 150,
      fluidSafetyFloorMinutes = 20,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val twinP2 = RoutineTask(
      id = "hp_vs_twin_p2",
      title = "Twin Part 2",
      type = TaskType.DYNAMIC,
      startTimeMinute = 830,
      durationMinutes = 90,
      isFluidTwin = true,
      fluidTwinId = "hp_vs_twin",
      fluidTwinPart = 2,
      fluidTotalPoolMinutes = 150,
      fluidSafetyFloorMinutes = 20,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val highPri = RoutineTask(
      id = "hp_first",
      title = "Critical Deadline",
      type = TaskType.DYNAMIC,
      startTimeMinute = 830,
      durationMinutes = 80,
      isHighPriority = true,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    val result = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(twinP1, twinP2, highPri),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    val scheduledHp = result.scheduledTasks.firstOrNull { it.id == "hp_first" }
    assertNotNull("High Priority task must be scheduled in its fenced Gap 3-4", scheduledHp)
    assertEquals(830, scheduledHp!!.startTimeMinute)

    // Test Same-Gap Partner Exclusion during scheduleDay: if both twins request Gap 2-3 and no other free gaps exist,
    // wait - Gap 3-4 IS open, so Part 2 relocates out of Gap 2-3 into another gap instead of sharing Gap 2-3!
    val sameGapP1 = twinP1.copy(id = "sg_p1", fluidTwinId = "sg", startTimeMinute = 400, durationMinutes = 40)
    val sameGapP2 = twinP2.copy(id = "sg_p2", fluidTwinId = "sg", startTimeMinute = 500, durationMinutes = 40)
    val sameGapRes = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(sameGapP1, sameGapP2),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    val placedP1 = sameGapRes.scheduledTasks.firstOrNull { it.id == "sg_p1" }
    val placedP2 = sameGapRes.scheduledTasks.firstOrNull { it.id == "sg_p2" }
    assertNotNull(placedP1)
    assertNotNull(placedP2)
    val win1 = com.example.engine.SchedulerEngine.getInterAnchorWindowKey(anchors, placedP1!!.startTimeMinute, RoutineDay.MONDAY)
    val win2 = com.example.engine.SchedulerEngine.getInterAnchorWindowKey(anchors, placedP2!!.startTimeMinute, RoutineDay.MONDAY)
    assertTrue("Scheduled Fluid Twin parts must reside in separate inter-anchor windows", win1 != win2)

    // Test targeted local compaction inside candidate window:
    // Gap 3-4 (830..990 = 160m) has two normal tasks at 850..890 (40m) and 920..950 (30m) -> split slack (20m + 30m + 40m = 90m).
    // Adding an 80m High Priority task at 830m inside Gap 3-4 packs existing tasks to fit ALL THREE without overflowing!
    val normA = RoutineTask(
      id = "compact_a",
      title = "Normal A",
      type = TaskType.DYNAMIC,
      startTimeMinute = 850,
      durationMinutes = 40,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val normB = RoutineTask(
      id = "compact_b",
      title = "Normal B",
      type = TaskType.DYNAMIC,
      startTimeMinute = 920,
      durationMinutes = 30,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val hpFitsViaCompaction = RoutineTask(
      id = "hp_local_compact",
      title = "Priority 80m",
      type = TaskType.DYNAMIC,
      startTimeMinute = 830,
      durationMinutes = 80,
      isHighPriority = true,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val localCompRes = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(normA, normB, hpFitsViaCompaction),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    assertTrue(
      "All 3 tasks (80 + 40 + 30 = 150m <= 160m) must fit via targeted local window compaction",
      localCompRes.overflowTasks.isEmpty()
    )
  }

  @Test
  fun testCompactAndRescueSchedulePacksGapsRescuesOverflowAndDisplacesForHighPriority() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    // Scenario 1: Split slack inside Gap 3-4 (830..990 = 160m) prevents a Fluid Twin part (40m floor, 50m dur)
    // from fitting in Gap 3-4 until hp1 & hp2 are compacted sequentially.
    val fill23 = RoutineTask(
      id = "f23",
      title = "Fill 2-3",
      type = TaskType.DYNAMIC,
      startTimeMinute = 335,
      durationMinutes = 460,
      isHighPriority = true,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val fill71 = RoutineTask(
      id = "f71",
      title = "Fill 7-1",
      type = TaskType.DYNAMIC,
      startTimeMinute = 1235,
      durationMinutes = 145,
      isHighPriority = true,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val hp1 = RoutineTask(
      id = "cr_hp1",
      title = "Priority 1",
      type = TaskType.DYNAMIC,
      startTimeMinute = 855, // leaves 25m before (830..855)
      durationMinutes = 70,
      isHighPriority = true,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val hp2 = RoutineTask(
      id = "cr_hp2",
      title = "Priority 2",
      type = TaskType.DYNAMIC,
      startTimeMinute = 950, // leaves 25m between (925..950)
      durationMinutes = 40,
      isHighPriority = true,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val twinP1 = RoutineTask(
      id = "cr_twin1",
      title = "Twin Part 1",
      type = TaskType.DYNAMIC,
      startTimeMinute = 1110, // fills Gap 6-7 (1110..1200)
      durationMinutes = 90,
      isFluidTwin = true,
      fluidTwinId = "twin_rescue_pair",
      fluidTotalPoolMinutes = 140,
      fluidSafetyFloorMinutes = 45,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val twinP2 = RoutineTask(
      id = "cr_twin2",
      title = "Twin Part 2",
      type = TaskType.DYNAMIC,
      startTimeMinute = 830, // needs >= 40m in Gap 3-4 (max uncompacted gap is 25m -> overflows!)
      durationMinutes = 50,
      isFluidTwin = true,
      fluidTwinId = "twin_rescue_pair",
      fluidTotalPoolMinutes = 140,
      fluidSafetyFloorMinutes = 40,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    // v1.3 Two-Tier Human-Level Solver automatically compacts Gap 3-4 inside scheduleDay() to fit twinP2!
    val autoSolved = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(fill23, fill71, hp1, hp2, twinP1, twinP2),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    assertTrue("v1.3 scheduleDay must auto-compact Gap 3-4 and rescue twinP2 with zero overflow", autoSolved.overflowTasks.isEmpty())
    assertTrue(autoSolved.scheduledTasks.any { it.id == "cr_twin2" })

    // Scenario 1: Compact fragmented non-overflowing tasks into tight sequential blocks
    val frag1 = RoutineTask(
      id = "cr_frag1",
      title = "Frag 1",
      type = TaskType.DYNAMIC,
      startTimeMinute = 855, // leaves 25m before (830..855)
      durationMinutes = 70,
      isHighPriority = true,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val frag2 = RoutineTask(
      id = "cr_frag2",
      title = "Frag 2",
      type = TaskType.DYNAMIC,
      startTimeMinute = 950, // leaves 25m between (925..950)
      durationMinutes = 40,
      isHighPriority = true,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val outcome = com.example.engine.SchedulerEngine.compactAndRescueSchedule(
      anchors = anchors,
      allDynamicTasks = listOf(fill23, fill71, frag1, frag2, twinP1),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    assertTrue("Sequential compaction must move fragmented tasks", outcome.movedCount >= 2)
    assertTrue("No overflow tasks should remain after compaction", outcome.scheduleResult.overflowTasks.isEmpty())

    // Scenario 2: Optimal schedule returns movedCount == 0 && rescuedCount == 0 && flexedCount == 0
    val optimalOutcome = com.example.engine.SchedulerEngine.compactAndRescueSchedule(
      anchors = anchors,
      allDynamicTasks = outcome.updatedDynamicTasks,
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    assertEquals(0, optimalOutcome.movedCount)
    assertEquals(0, optimalOutcome.rescuedCount)
    assertEquals(0, optimalOutcome.flexedCount)
  }

  @Test
  fun testSolverFairShareEqualAndAsymmetricFlexCompression() {
    // Single 240m window (360..600) bounded by two anchors (0..360 and 600..1440)
    val windowAnchors = listOf(
      RoutineTask(id = "a1", title = "Early Block", type = TaskType.ANCHOR_STANDARD, slotNumber = 2, startTimeMinute = 0, durationMinutes = 360, selectedDays = RoutineDay.allDays()),
      RoutineTask(id = "a2", title = "Late Block", type = TaskType.ANCHOR_SLEEP, slotNumber = 1, startTimeMinute = 600, durationMinutes = 840, selectedDays = RoutineDay.allDays())
    )

    // Case A: 5 x 60m tasks (each flex=15) in 240m -> 300m total, 60m deficit -> ALL 5 become 48m with originalDurationMinutes=60
    val fiveTasks = (1..5).map { idx ->
      RoutineTask(
        id = "fs_eq_$idx",
        title = "Equal Task $idx",
        type = TaskType.DYNAMIC,
        startTimeMinute = 360 + (idx - 1) * 45,
        originalStartTimeMinute = 360 + (idx - 1) * 45,
        durationMinutes = 60,
        originalDurationMinutes = 60,
        customFlexibilityMinutes = 15,
        toleranceMinutes = 15,
        selectedDays = setOf(RoutineDay.MONDAY)
      )
    }
    val eqRes = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = windowAnchors,
      dynamicTasks = fiveTasks,
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    assertTrue("All 5 equal-flex tasks must fit cleanly in 240m window", eqRes.overflowTasks.isEmpty())
    val eqScheduled = eqRes.scheduledTasks.filter { it.type == TaskType.DYNAMIC }.sortedBy { it.startTimeMinute }
    assertEquals(5, eqScheduled.size)
    eqScheduled.forEach { t ->
      assertEquals("Each equal-flex task must compress fairly to 48m", 48, t.durationMinutes)
      assertEquals("Canonical birth duration must stay 60m", 60, t.originalDurationMinutes)
    }

    // Case B: 4 x 60m flex=15 + 1 x 60m flex=0 -> 45m x 4 + 60m x 1 (tested with 0-flex both early and late)
    for (zeroIdx in listOf(1, 5)) {
      val asymTasks = (1..5).map { idx ->
        val flex = if (idx == zeroIdx) 0 else 15
        RoutineTask(
          id = "fs_asym_${zeroIdx}_$idx",
          title = "Asym Task $idx",
          type = TaskType.DYNAMIC,
          startTimeMinute = 360 + (idx - 1) * 45,
          originalStartTimeMinute = 360 + (idx - 1) * 45,
          durationMinutes = 60,
          originalDurationMinutes = 60,
          customFlexibilityMinutes = flex,
          toleranceMinutes = flex,
          selectedDays = setOf(RoutineDay.MONDAY)
        )
      }
      val asymRes = com.example.engine.SchedulerEngine.scheduleDay(
        anchors = windowAnchors,
        dynamicTasks = asymTasks,
        activeDay = RoutineDay.MONDAY,
        interTaskBufferMinutes = 0
      )
      assertTrue("All 5 asymmetric tasks must fit cleanly (zeroIdx=$zeroIdx)", asymRes.overflowTasks.isEmpty())
      val asymById = asymRes.scheduledTasks.filter { it.type == TaskType.DYNAMIC }.associateBy { it.id }
      for (idx in 1..5) {
        val t = asymById["fs_asym_${zeroIdx}_$idx"]!!
        val expected = if (idx == zeroIdx) 60 else 45
        assertEquals("Task $idx with zeroIdx=$zeroIdx expected ${expected}m", expected, t.durationMinutes)
        assertEquals(60, t.originalDurationMinutes)
      }
    }
  }

  @Test
  fun testSolverMicroDeficitNeighborBorrowing() {
    // 240m window (360..600): Task A (222m, flex=10) + Task B (20m, flex=0) -> 2m deficit covered by A -> A=220m, B=20m!
    val windowAnchors = listOf(
      RoutineTask(id = "a1", title = "Early Block", type = TaskType.ANCHOR_STANDARD, slotNumber = 2, startTimeMinute = 0, durationMinutes = 360, selectedDays = RoutineDay.allDays()),
      RoutineTask(id = "a2", title = "Late Block", type = TaskType.ANCHOR_SLEEP, slotNumber = 1, startTimeMinute = 600, durationMinutes = 840, selectedDays = RoutineDay.allDays())
    )
    val donorA = RoutineTask(
      id = "micro_donor_a",
      title = "Flexible Block",
      type = TaskType.DYNAMIC,
      startTimeMinute = 360,
      originalStartTimeMinute = 360,
      durationMinutes = 222,
      originalDurationMinutes = 222,
      customFlexibilityMinutes = 10,
      toleranceMinutes = 10,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val rigidB = RoutineTask(
      id = "micro_rigid_b",
      title = "Rigid 20m Task",
      type = TaskType.DYNAMIC,
      startTimeMinute = 580,
      originalStartTimeMinute = 580,
      durationMinutes = 20,
      originalDurationMinutes = 20,
      customFlexibilityMinutes = 0,
      toleranceMinutes = 0,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    val res = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = windowAnchors,
      dynamicTasks = listOf(donorA, rigidB),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    assertTrue("Micro-deficit 2m must be absorbed without overflow", res.overflowTasks.isEmpty())
    val schedA = res.scheduledTasks.first { it.id == "micro_donor_a" }
    val schedB = res.scheduledTasks.first { it.id == "micro_rigid_b" }
    assertEquals("Donor A must lend 2m (222 -> 220)", 220, schedA.durationMinutes)
    assertEquals("Rigid B must stay at full 20m", 20, schedB.durationMinutes)
    assertEquals(222, schedA.originalDurationMinutes)
  }

  @Test
  fun testSolverCrossAnchorMigrationAndFlexToShrinkRelocate() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    // Fill Gap 2-3 (460m), Gap 6-7 (90m), and leave 35m in Gap 7-1 (145m = 110m filler + 35m open).
    // Gap 3-4 is 160m (830..990):
    // - Movable M (40m, flex=10, min=30m) at 830
    // - Rigid filler R (100m, flex=0, HP) at 870
    // - New HP B (60m, flex=0) at 830 inside Gap 3-4!
    // Initially Gap 3-4 has 140m used -> 20m free. B needs 60m (needs 40m more).
    // Strategy 1 migrates M (40m) to Gap 7-1 if Gap 7-1 has >= 40m; if Gap 7-1 only has 35m,
    // Strategy 3B shrinks M from 40m -> 35m within M's 10m flex and relocates M to Gap 7-1, fitting B at full 60m!
    val fill23 = RoutineTask(id = "m_f23", title = "Fill 2-3", type = TaskType.DYNAMIC, startTimeMinute = 335, durationMinutes = 460, isHighPriority = true, selectedDays = setOf(RoutineDay.MONDAY))
    val fill67 = RoutineTask(id = "m_f67", title = "Fill 6-7", type = TaskType.DYNAMIC, startTimeMinute = 1110, durationMinutes = 90, isHighPriority = true, selectedDays = setOf(RoutineDay.MONDAY))

    // Test Strategy 1 (Cross-Anchor Migration at full 40m when Gap 7-1 has 45m open)
    val fill71Strategy1 = RoutineTask(id = "m_f71_s1", title = "Fill 7-1", type = TaskType.DYNAMIC, startTimeMinute = 1235, durationMinutes = 100, isHighPriority = true, selectedDays = setOf(RoutineDay.MONDAY))
    val movable1 = RoutineTask(id = "m_mov1", title = "Movable 40m", type = TaskType.DYNAMIC, startTimeMinute = 830, originalStartTimeMinute = 830, durationMinutes = 40, originalDurationMinutes = 40, customFlexibilityMinutes = 10, toleranceMinutes = 10, selectedDays = setOf(RoutineDay.MONDAY))
    val hpFiller = RoutineTask(id = "m_hp_fill", title = "HP Filler", type = TaskType.DYNAMIC, startTimeMinute = 870, durationMinutes = 100, isHighPriority = true, selectedDays = setOf(RoutineDay.MONDAY))
    val hpIncoming = RoutineTask(id = "m_hp_in", title = "HP Incoming 60m", type = TaskType.DYNAMIC, startTimeMinute = 830, durationMinutes = 60, isHighPriority = true, selectedDays = setOf(RoutineDay.MONDAY))

    val s1Res = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(fill23, fill67, fill71Strategy1, movable1, hpFiller, hpIncoming),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    assertTrue("Strategy 1 migration must eliminate all overflow", s1Res.overflowTasks.isEmpty())
    val s1Moved = s1Res.scheduledTasks.first { it.id == "m_mov1" }
    val s1Incoming = s1Res.scheduledTasks.first { it.id == "m_hp_in" }
    assertEquals("Migrated movable keeps full 40m duration", 40, s1Moved.durationMinutes)
    assertTrue("Migrated movable lands in Gap 7-1 (1235..1380)", s1Moved.startTimeMinute in 1235 until 1380)
    assertEquals("Original birth start preserved for telemetry", 830, s1Moved.originalStartTimeMinute)
    assertTrue("HP incoming fits inside fenced Gap 3-4 (830..990)", s1Incoming.startTimeMinute in 830 until 990)

    // Test Strategy 3B (Flex-to-Shrink & Relocate when Gap 7-1 only has 35m open)
    val fill71Strategy3B = RoutineTask(id = "m_f71_s3b", title = "Fill 7-1 Tight", type = TaskType.DYNAMIC, startTimeMinute = 1235, durationMinutes = 110, isHighPriority = true, selectedDays = setOf(RoutineDay.MONDAY))
    val s3bRes = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(fill23, fill67, fill71Strategy3B, movable1, hpFiller, hpIncoming),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    assertTrue("Strategy 3B flex-shrink-relocate must eliminate all overflow", s3bRes.overflowTasks.isEmpty())
    val s3bMoved = s3bRes.scheduledTasks.first { it.id == "m_mov1" }
    val s3bIncoming = s3bRes.scheduledTasks.first { it.id == "m_hp_in" }
    assertEquals("Movable M shrinks within flex to 35m in destination Gap 7-1", 35, s3bMoved.durationMinutes)
    assertEquals("Canonical birth duration stays 40m", 40, s3bMoved.originalDurationMinutes)
    assertTrue("Relocated M lands in Gap 7-1", s3bMoved.startTimeMinute in 1235 until 1380)
    assertEquals("HP incoming gets full 60m inside fenced Gap 3-4", 60, s3bIncoming.durationMinutes)
  }

  @Test
  fun testBirthDurationAutoReturnAntiDoubleFlexAndConsoleSync() {
    val windowAnchors = listOf(
      RoutineTask(id = "a1", title = "Early Block", type = TaskType.ANCHOR_STANDARD, slotNumber = 2, startTimeMinute = 0, durationMinutes = 360, selectedDays = RoutineDay.allDays()),
      RoutineTask(id = "a2", title = "Late Block", type = TaskType.ANCHOR_SLEEP, slotNumber = 1, startTimeMinute = 600, durationMinutes = 840, selectedDays = RoutineDay.allDays())
    )
    // 240m window: Blocker 195m (HP, flex=0) + FlexTask 60m (flex=15, min=45) -> FlexTask becomes 45m, originalDurationMinutes=60
    val blocker = RoutineTask(
      id = "ad_blocker",
      title = "Blocker",
      type = TaskType.DYNAMIC,
      startTimeMinute = 360,
      originalStartTimeMinute = 360,
      durationMinutes = 195,
      originalDurationMinutes = 195,
      isHighPriority = true,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val flexTask = RoutineTask(
      id = "ad_flex",
      title = "Elastic Study",
      type = TaskType.DYNAMIC,
      startTimeMinute = 555,
      originalStartTimeMinute = 555,
      durationMinutes = 60,
      originalDurationMinutes = 60,
      customFlexibilityMinutes = 15,
      toleranceMinutes = 15,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    val day1 = com.example.engine.SchedulerEngine.scheduleDay(windowAnchors, listOf(blocker, flexTask), RoutineDay.MONDAY)
    val flexed1 = day1.scheduledTasks.first { it.id == "ad_flex" }
    assertEquals(45, flexed1.durationMinutes)
    assertEquals(60, flexed1.originalDurationMinutes)

    // Anti-double-flexing: passing already-flexed scheduled output back into scheduleDay MUST NOT shrink 45m -> 30m!
    val extraPressure = RoutineTask(
      id = "ad_pressure",
      title = "Extra 15m",
      type = TaskType.DYNAMIC,
      startTimeMinute = 540,
      originalStartTimeMinute = 540,
      durationMinutes = 15,
      originalDurationMinutes = 15,
      isHighPriority = true,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val dayReEvaluated = com.example.engine.SchedulerEngine.scheduleDay(
      windowAnchors,
      listOf(blocker, flexed1, extraPressure),
      RoutineDay.MONDAY
    )
    assertFalse("Anti-double-flexing must refuse shrinking below birthDuration - flex (45m)",
      dayReEvaluated.scheduledTasks.any { it.id == "ad_flex" && it.durationMinutes < 45 })

    // Birth-Duration Auto-Return: pausing/deleting blocker restores Elastic Study to full 60m automatically!
    val dayRecovered = com.example.engine.SchedulerEngine.scheduleDay(
      windowAnchors,
      listOf(blocker.copy(isPausedTodayOnly = true), flexed1),
      RoutineDay.MONDAY
    )
    val restored = dayRecovered.scheduledTasks.first { it.id == "ad_flex" }
    assertEquals("Birth duration 60m must auto-restore once blocker is paused", 60, restored.durationMinutes)
    assertEquals(60, restored.originalDurationMinutes)

    // Console export/parse must preserve canonical birth duration (60m, not 45m)
    val exported = ConsoleParser.generateRoutineScript(windowAnchors, listOf(flexed1), sleepMinutes = 360, bufferMinutes = 0)
    assertTrue("Exported CLI script must emit birth duration 60m", exported.contains("| 60 |"))
    val reparsed = ConsoleParser.parse(exported)
    assertEquals(0, reparsed.errorCount)
    val parsedTask = reparsed.validTasks.first()
    assertEquals(60, parsedTask.durationMinutes)
    assertEquals(60, parsedTask.originalDurationMinutes)
  }

  @Test
  fun testSolverEnforcesPriorityWallsTwinExclusionBuffersDiffAndDropPreview() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)

    // 1. High Priority Tier-2 slack is NOT touched while Tier-1 non-HP slack can satisfy deficit,
    //    and HP never crosses its enclosing anchor wall even when compressed.
    val hpIn34 = RoutineTask(
      id = "tier_hp",
      title = "HP Flexible",
      type = TaskType.DYNAMIC,
      startTimeMinute = 830,
      originalStartTimeMinute = 830,
      durationMinutes = 80,
      originalDurationMinutes = 80,
      isHighPriority = true,
      customFlexibilityMinutes = 20,
      toleranceMinutes = 20,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val norm1In34 = RoutineTask(
      id = "tier_n1",
      title = "Normal 1",
      type = TaskType.DYNAMIC,
      startTimeMinute = 910,
      originalStartTimeMinute = 910,
      durationMinutes = 50,
      originalDurationMinutes = 50,
      customFlexibilityMinutes = 20,
      toleranceMinutes = 20,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val norm2In34 = RoutineTask(
      id = "tier_n2",
      title = "Normal 2",
      type = TaskType.DYNAMIC,
      startTimeMinute = 830,
      originalStartTimeMinute = 830,
      durationMinutes = 40,
      originalDurationMinutes = 40,
      customFlexibilityMinutes = 20,
      toleranceMinutes = 20,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    // Gap 3-4 is 160m. Fill other gaps so Migration won't move norm1In34 out.
    val f23 = RoutineTask(id = "c_f23", title = "F23", type = TaskType.DYNAMIC, startTimeMinute = 335, durationMinutes = 460, isHighPriority = true, selectedDays = setOf(RoutineDay.MONDAY))
    val f67 = RoutineTask(id = "c_f67", title = "F67", type = TaskType.DYNAMIC, startTimeMinute = 1110, durationMinutes = 90, isHighPriority = true, selectedDays = setOf(RoutineDay.MONDAY))
    val f71 = RoutineTask(id = "c_f71", title = "F71", type = TaskType.DYNAMIC, startTimeMinute = 1235, durationMinutes = 145, isHighPriority = true, selectedDays = setOf(RoutineDay.MONDAY))

    val tierRes = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(f23, f67, f71, hpIn34, norm1In34, norm2In34),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    assertTrue(tierRes.overflowTasks.isEmpty())
    val schedHp = tierRes.scheduledTasks.first { it.id == "tier_hp" }
    assertEquals("Tier-2 High Priority duration must remain untouched when Tier-1 slack covers deficit", 80, schedHp.durationMinutes)
    assertTrue("HP must stay inside Gap 3-4", schedHp.startTimeMinute in 830 until 990)

    // 2. Fluid Twin Same-Gap Partner Exclusion enforced during Cross-Anchor Migration / 3B
    // Put twinP1 in Gap 7-1 (1235..1380), and twinP2 in Gap 3-4; force an HP blocker into Gap 3-4
    // so twinP2 is migrated: twinP2 MUST NOT migrate into Gap 7-1 even though Gap 7-1 has room;
    // it must pick Gap 6-7 (1110..1200) or Gap 2-3 instead!
    val twin1 = RoutineTask(
      id = "ex_m_p1", title = "Twin 1", type = TaskType.DYNAMIC, startTimeMinute = 1235, originalStartTimeMinute = 1235,
      durationMinutes = 60, originalDurationMinutes = 60, isFluidTwin = true, fluidTwinId = "ex_m", fluidTwinPart = 1,
      fluidTotalPoolMinutes = 120, fluidSafetyFloorMinutes = 30, selectedDays = setOf(RoutineDay.MONDAY)
    )
    val twin2 = RoutineTask(
      id = "ex_m_p2", title = "Twin 2", type = TaskType.DYNAMIC, startTimeMinute = 830, originalStartTimeMinute = 830,
      durationMinutes = 60, originalDurationMinutes = 60, isFluidTwin = true, fluidTwinId = "ex_m", fluidTwinPart = 2,
      fluidTotalPoolMinutes = 120, fluidSafetyFloorMinutes = 30, selectedDays = setOf(RoutineDay.MONDAY)
    )
    val hpBlock34 = RoutineTask(
      id = "ex_hp_34", title = "HP 34", type = TaskType.DYNAMIC, startTimeMinute = 830, originalStartTimeMinute = 830,
      durationMinutes = 130, originalDurationMinutes = 130, isHighPriority = true, selectedDays = setOf(RoutineDay.MONDAY)
    )
    val exRes = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(f23, twin1, twin2, hpBlock34),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    val resP1 = exRes.scheduledTasks.first { it.id == "ex_m_p1" }
    val resP2 = exRes.scheduledTasks.first { it.id == "ex_m_p2" }
    val w1 = com.example.engine.SchedulerEngine.getInterAnchorWindowKey(anchors, resP1.startTimeMinute, RoutineDay.MONDAY)
    val w2 = com.example.engine.SchedulerEngine.getInterAnchorWindowKey(anchors, resP2.startTimeMinute, RoutineDay.MONDAY)
    assertTrue("Migrated twin must respect Same-Gap Partner Exclusion", w1 != w2)

    // 3. Buffer preservation + Recovered impact diff format + previewVerticalDrop recovery duration
    val bufTask = RoutineTask(
      id = "buf_keep", title = "Buffered", type = TaskType.DYNAMIC, startTimeMinute = 1110, originalStartTimeMinute = 1110,
      durationMinutes = 80, originalDurationMinutes = 80, interTaskBufferMinutes = 10,
      customFlexibilityMinutes = 10, toleranceMinutes = 10, selectedDays = setOf(RoutineDay.MONDAY)
    )
    val incomingFor90 = RoutineTask(
      id = "buf_in", title = "Incoming", type = TaskType.DYNAMIC, startTimeMinute = 1110, originalStartTimeMinute = 1110,
      durationMinutes = 10, originalDurationMinutes = 10, isHighPriority = true, selectedDays = setOf(RoutineDay.MONDAY)
    )
    val bufRes = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(f23, f71, hpBlock34, bufTask, incomingFor90),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 0
    )
    assertTrue(bufRes.overflowTasks.isEmpty())
    val schedBuf = bufRes.scheduledTasks.first { it.id == "buf_keep" }
    val schedIn = bufRes.scheduledTasks.first { it.id == "buf_in" }
    assertTrue("10m interTaskBufferMinutes must remain preserved between tasks",
      schedBuf.startTimeMinute + schedBuf.durationMinutes + 10 <= schedIn.startTimeMinute ||
        schedIn.startTimeMinute + schedIn.durationMinutes <= schedBuf.startTimeMinute)

    // Impact diff format: "Recovered at <time> (60m → 45m)"
    val ovBase = com.example.engine.ScheduleResult(
      scheduledTasks = emptyList(),
      overflowTasks = listOf(flexTaskStub(60)),
      availableGaps = emptyList(),
      totalScheduledMinutes = 0,
      totalGapsMinutes = 0
    )
    val recDraft = com.example.engine.ScheduleResult(
      scheduledTasks = listOf(flexTaskStub(45).copy(startTimeMinute = 830, isOverflow = false)),
      overflowTasks = emptyList(),
      availableGaps = emptyList(),
      totalScheduledMinutes = 45,
      totalGapsMinutes = 0
    )
    val diff = com.example.engine.SchedulerEngine.computeScheduleImpactDiff(ovBase, recDraft)
    assertEquals(1, diff.size)
    assertEquals("Recovered at ${RoutineTask.formatTime(830)} (60m → 45m)", diff.first().statusChange)

    // Vertical drop preview recovers full birth duration when hovering spacious target window
    val flexedSource = RoutineTask(
      id = "drop_flexed", title = "Drop Flexed", type = TaskType.DYNAMIC,
      startTimeMinute = 1110, originalStartTimeMinute = 1110,
      durationMinutes = 45, originalDurationMinutes = 60,
      customFlexibilityMinutes = 15, selectedDays = setOf(RoutineDay.MONDAY)
    )
    val spaciousAnchorTarget = com.example.engine.VerticalDropTarget.IntoGap(400) // Gap 2-3 (460m wide!)
    val dropPreview = com.example.engine.SchedulerEngine.previewVerticalDrop(
      allDynamicTasks = listOf(flexedSource),
      scheduledTasks = anchors + flexedSource,
      taskId = "drop_flexed",
      dropTarget = spaciousAnchorTarget,
      anchors = anchors,
      day = RoutineDay.MONDAY
    )
    assertEquals("Hovering spacious Gap 2-3 must preview recovered 60m duration", 60, dropPreview.previewEndMinute - dropPreview.previewStartMinute)
  }

  private fun flexTaskStub(duration: Int) = RoutineTask(
    id = "impact_stub",
    title = "Impact Stub",
    type = TaskType.DYNAMIC,
    startTimeMinute = 830,
    originalStartTimeMinute = 830,
    durationMinutes = duration,
    originalDurationMinutes = 60,
    selectedDays = setOf(RoutineDay.MONDAY)
  )
}
