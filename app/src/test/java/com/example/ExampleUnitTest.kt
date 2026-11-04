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
}
