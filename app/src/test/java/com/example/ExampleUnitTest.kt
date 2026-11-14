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
}


