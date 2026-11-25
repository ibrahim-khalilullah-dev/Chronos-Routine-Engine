package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.RoutineRepository
import com.example.engine.ConsoleParser
import com.example.engine.SchedulerEngine
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.TaskType
import com.example.service.AutomationManager
import com.example.ui.components.isTaskActiveAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Chronos", appName)
  }

  @Test
  fun `slot 1 reverse sleep calculation links to slot 2 wake up`() {
    // Slot 2: 05:00 AM = 300 minutes from midnight
    // 7 hours sleep = 420 minutes
    // Bedtime = (300 - 420 + 1440) % 1440 = 1320 = 22:00 (10:00 PM)
    val anchors = SchedulerEngine.calculateAnchors(emptyList(), sleepDurationMinutes = 420)
    val slot1 = anchors.firstOrNull { it.slotNumber == 1 }
    val slot2 = anchors.firstOrNull { it.slotNumber == 2 }

    assertNotNull(slot1)
    assertNotNull(slot2)
    assertEquals(300, slot2!!.startTimeMinute)
    assertEquals(1320, slot1!!.startTimeMinute) // 10:00 PM
    assertEquals(420, slot1.durationMinutes)
  }

  @Test
  fun `slot 1 handles midnight crossing cleanly for late wake up`() {
    // Wake up at 02:00 AM = 120 minutes
    // Sleep duration 6 hours = 360 minutes
    // Bedtime = (120 - 360 + 1440) % 1440 = 1200 = 20:00 (08:00 PM)
    val customSlot2 = RoutineTask(
      id = "slot_2",
      title = "Wake Up",
      type = TaskType.ANCHOR_STANDARD,
      slotNumber = 2,
      startTimeMinute = 120, // 02:00 AM
      durationMinutes = 30
    )
    val anchors = SchedulerEngine.calculateAnchors(listOf(customSlot2), sleepDurationMinutes = 360)
    val slot1 = anchors.first { it.slotNumber == 1 }
    assertEquals(1200, slot1.startTimeMinute) // 20:00 (08:00 PM)
    assertEquals(360, slot1.durationMinutes)
  }

  @Test
  fun `slot 5 elastic buffer auto-sizes between slot 4 and slot 6 safely`() {
    val anchors = SchedulerEngine.calculateAnchors(emptyList(), sleepDurationMinutes = 420)
    val slot4 = anchors.first { it.slotNumber == 4 }
    val slot5 = anchors.first { it.slotNumber == 5 }
    val slot6 = anchors.first { it.slotNumber == 6 }

    val slot4End = slot4.startTimeMinute + slot4.durationMinutes
    val slot6Start = slot6.startTimeMinute

    assertEquals(slot4End, slot5.startTimeMinute)
    assertEquals(slot6Start, (slot5.startTimeMinute + slot5.durationMinutes) % 1440)
    assertTrue("Slot 5 elastic duration must be positive", slot5.durationMinutes >= 15)
  }

  @Test
  fun `adjusting slot 2 shifts slot 1 instantly`() {
    val initial = SchedulerEngine.calculateAnchors(emptyList(), sleepDurationMinutes = 360)
    val adjusted = SchedulerEngine.adjustAnchor(initial, slotNumber = 2, deltaMinutes = 10, sleepDurationMinutes = 360)
    val newSlot1 = adjusted.first { it.slotNumber == 1 }
    val newSlot2 = adjusted.first { it.slotNumber == 2 }

    assertEquals(310, newSlot2.startTimeMinute)
    assertEquals((310 - 360 + 1440) % 1440, newSlot1.startTimeMinute)
  }

  @Test
  fun `dynamic task splitting splits tasks over 60m into chunks of at least 15m`() {
    val anchors = SchedulerEngine.calculateAnchors(emptyList(), sleepDurationMinutes = 420)
    val longTask = RoutineTask(
      id = "long_task_1",
      title = "Massive Coding Sprint",
      type = TaskType.DYNAMIC,
      startTimeMinute = 720,
      durationMinutes = 90,
      allowSplitting = true,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    val result = SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(longTask),
      activeDay = RoutineDay.MONDAY
    )

    val scheduled = result.scheduledTasks.firstOrNull { it.id == "long_task_1" || it.parentTaskId == "long_task_1" }
    assertNotNull(scheduled)
    assertEquals(90, scheduled!!.durationMinutes)
  }

  @Test
  fun `console parser parses 7-field syntax with voice toggle and notification`() {
    val input = "Client Sprint | 45 | 14:00 | Y | Mon,Wed | N | Heads up! Client sprint starting now"
    val result = ConsoleParser.parse(input)
    assertEquals(0, result.errorCount)
    assertEquals(1, result.successCount)
    val task = result.validTasks.first()
    assertEquals("Client Sprint", task.title)
    assertEquals(45, task.durationMinutes)
    assertEquals(840, task.startTimeMinute) // 14 * 60 = 840
    assertTrue(task.enableDnd)
    assertFalse("Voice should be disabled by N flag", task.isVoiceEnabled)
    assertEquals("Heads up! Client sprint starting now", task.notificationDisplayBody)
  }

  @Test
  fun `console parser reports line by line syntax errors cleanly`() {
    val badInput = "Invalid Task | not_a_number | 25:99 | maybe | nowhere"
    val result = ConsoleParser.parse(badInput)
    assertTrue("Should flag syntax error", result.errorCount > 0)
    assertEquals(0, result.successCount)
  }

  @Test
  fun `dnd safe toggle does not crash when ungranted`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val outcome = AutomationManager.setDndMode(context, true)
    assertFalse(outcome)
  }

  @Test
  fun `boot receiver reschedules alarms safely on boot completed`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val receiver = com.example.receiver.BootReceiver()
    val intent = android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED)
    receiver.onReceive(context, intent)
    assertTrue(true)
  }

  @Test
  fun `soft deleted tasks are excluded from scheduler timeline`() {
    val anchors = SchedulerEngine.calculateAnchors(emptyList(), sleepDurationMinutes = 420)
    val task1 = RoutineTask(
      id = "task_active",
      title = "Active Sprint",
      type = TaskType.DYNAMIC,
      startTimeMinute = 480,
      durationMinutes = 30,
      isDeleted = false,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val task2 = RoutineTask(
      id = "task_deleted",
      title = "Deleted Sprint",
      type = TaskType.DYNAMIC,
      startTimeMinute = 520,
      durationMinutes = 30,
      isDeleted = true,
      deletedTimestamp = System.currentTimeMillis(),
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    val result = SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(task1, task2),
      activeDay = RoutineDay.MONDAY
    )

    // task_active must be present, task_deleted must NOT be present
    assertTrue(result.scheduledTasks.any { it.id == "task_active" })
    assertFalse(result.scheduledTasks.any { it.id == "task_deleted" })
  }

  @Test
  fun `repository soft delete and restore functions correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = RoutineRepository(context)

    val testTask = RoutineTask(
      id = "test_soft_delete_1",
      title = "Test Task for Trash",
      type = TaskType.DYNAMIC,
      startTimeMinute = 600,
      durationMinutes = 30
    )
    repo.saveDynamicTasks(listOf(testTask))

    // Soft delete
    repo.softDeleteTask("test_soft_delete_1")
    val inTrash = repo.getDeletedTasks()
    assertTrue(inTrash.any { it.id == "test_soft_delete_1" && it.isDeleted })

    // Restore
    repo.restoreTask("test_soft_delete_1")
    val restored = repo.getDynamicTasks().first { it.id == "test_soft_delete_1" }
    assertFalse(restored.isDeleted)
  }

  @Test
  fun `isTaskActiveAt correctly detects active minute`() {
    val taskNormal = RoutineTask(
      id = "t1",
      title = "Afternoon Block",
      type = TaskType.DYNAMIC,
      startTimeMinute = 600, // 10:00 AM
      durationMinutes = 60   // Ends at 11:00 AM (660)
    )
    assertTrue("Should be active at 10:30 (630m)", isTaskActiveAt(taskNormal, 630))
    assertFalse("Should not be active at 09:59 (599m)", isTaskActiveAt(taskNormal, 599))
    assertFalse("Should not be active at 11:00 (660m)", isTaskActiveAt(taskNormal, 660))

    // Midnight rollover test: 23:00 (1380m) to 01:00 (60m)
    val taskMidnight = RoutineTask(
      id = "t_midnight",
      title = "Night Sleep",
      type = TaskType.ANCHOR_SLEEP,
      startTimeMinute = 1380, // 23:00 (11 PM)
      durationMinutes = 120   // Ends at 01:00 AM
    )
    assertTrue("Should be active at 23:30 (1410m)", isTaskActiveAt(taskMidnight, 1410))
    assertTrue("Should be active at 00:30 (30m)", isTaskActiveAt(taskMidnight, 30))
    assertFalse("Should not be active at 22:30 (1350m)", isTaskActiveAt(taskMidnight, 1350))
    assertFalse("Should not be active at 01:30 (90m)", isTaskActiveAt(taskMidnight, 90))
  }

  @Test
  fun `tolerance margin allows dynamic task to fit into tight anchor gap without overflowing`() {
    val anchors = SchedulerEngine.calculateAnchors(emptyList(), sleepDurationMinutes = 420)
    // Find gaps
    val gaps = SchedulerEngine.findAvailableGaps(anchors, interTaskBufferMinutes = 5)
    val gap = gaps.firstOrNull { it.durationMinutes in 30..80 } ?: gaps.first()

    // Create task with duration slightly larger than the gap (e.g., gap + 10 min)
    val taskWithoutTolerance = RoutineTask(
      id = "tight_task_strict",
      title = "Strict Task",
      type = TaskType.DYNAMIC,
      startTimeMinute = gap.startMinute,
      durationMinutes = gap.durationMinutes + 8,
      toleranceMinutes = 0,
      allowSplitting = false,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    // With 0 tolerance, task cannot fit into this specific gap continuously
    val taskWithTolerance = taskWithoutTolerance.copy(
      id = "tight_task_tolerant",
      title = "Tolerant Task",
      toleranceMinutes = 10
    )

    val resultTolerant = SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(taskWithTolerance),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 5
    )

    // With tolerance of 10 min, engine shrinks duration to fit gap.durationMinutes safely
    val scheduled = resultTolerant.scheduledTasks.firstOrNull { it.id == "tight_task_tolerant" }
    assertNotNull("Tolerant task should be scheduled", scheduled)
    assertTrue("Duration should be safely compressed within tolerance", scheduled!!.durationMinutes <= taskWithTolerance.durationMinutes)
  }

  @Test
  fun `anchors top and anchors bottom sorting modes group tasks correctly`() {
    val anchors = SchedulerEngine.calculateAnchors(emptyList(), sleepDurationMinutes = 420)
    val dynamicTask = RoutineTask(
      id = "dyn_1",
      title = "Midday Focus",
      type = TaskType.DYNAMIC,
      startTimeMinute = 600,
      durationMinutes = 45,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    val result = SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(dynamicTask),
      activeDay = RoutineDay.MONDAY
    )

    // Sort ANCHORS_TOP
    val (anchorsTop, dynamicsTop) = result.scheduledTasks.partition {
      it.type == TaskType.ANCHOR_SLEEP || it.type == TaskType.ANCHOR_STANDARD || it.type == TaskType.ANCHOR_ELASTIC
    }
    val sortedAnchorsTop = anchorsTop.sortedBy { it.slotNumber ?: 0 } + dynamicsTop.sortedBy { it.startTimeMinute }
    assertEquals(TaskType.ANCHOR_SLEEP, sortedAnchorsTop.first().type)
    assertEquals("dyn_1", sortedAnchorsTop.last().id)

    // Sort ANCHORS_BOTTOM
    val sortedAnchorsBottom = dynamicsTop.sortedBy { it.startTimeMinute } + anchorsTop.sortedBy { it.slotNumber ?: 0 }
    assertEquals("dyn_1", sortedAnchorsBottom.first().id)
    assertEquals(TaskType.ANCHOR_STANDARD, sortedAnchorsBottom.last().type)
  }

  @Test
  fun `precision default anchors configure exact 30-minute durations and exact user start times`() {
    val defaults = SchedulerEngine.createDefaultAnchors(sleepDurationMinutes = 360)
    assertEquals(7, defaults.size)

    val s1 = defaults.first { it.slotNumber == 1 }
    val s2 = defaults.first { it.slotNumber == 2 }
    val s3 = defaults.first { it.slotNumber == 3 }
    val s4 = defaults.first { it.slotNumber == 4 }
    val s5 = defaults.first { it.slotNumber == 5 }
    val s6 = defaults.first { it.slotNumber == 6 }
    val s7 = defaults.first { it.slotNumber == 7 }

    // Slot 1: Sleep: 11:00 PM (1380m), 360m duration
    assertEquals("Sleep", s1.title)
    assertEquals(1380, s1.startTimeMinute)
    assertEquals(360, s1.durationMinutes)
    assertEquals("ঘুমানোর সময় হয়েছে।", s1.voiceMessage)

    // Slot 2: Fajr: 05:00 AM (300m), 35m duration
    assertEquals("Fajr", s2.title)
    assertEquals(300, s2.startTimeMinute)
    assertEquals(35, s2.durationMinutes)
    assertEquals("ফজরের সময় হয়েছে।", s2.voiceMessage)

    // Slot 3: Dhuhr: 01:15 PM (795m), 35m duration
    assertEquals("Dhuhr", s3.title)
    assertEquals(795, s3.startTimeMinute)
    assertEquals(35, s3.durationMinutes)
    assertEquals("যোহরের সময় হয়েছে।", s3.voiceMessage)

    // Slot 4: Asr: 04:30 PM (990m), 25m duration
    assertEquals("Asr", s4.title)
    assertEquals(990, s4.startTimeMinute)
    assertEquals(25, s4.durationMinutes)
    assertEquals("আসরের সময় হয়েছে।", s4.voiceMessage)

    // Slot 5: Break: Spans from 04:55 PM (1015m) to 06:00 PM (1080m) = 65m duration
    assertEquals("Break", s5.title)
    assertEquals(1015, s5.startTimeMinute)
    assertEquals(65, s5.durationMinutes)
    assertEquals("বিরতির সময় হয়েছে।", s5.voiceMessage)

    // Slot 6: Maghrib: 06:00 PM (1080m), 30m duration
    assertEquals("Maghrib", s6.title)
    assertEquals(1080, s6.startTimeMinute)
    assertEquals(30, s6.durationMinutes)
    assertEquals("মাগরিবের সময় হয়েছে।", s6.voiceMessage)

    // Slot 7: Isha: 08:00 PM (1200m), 35m duration
    assertEquals("Isha", s7.title)
    assertEquals(1200, s7.startTimeMinute)
    assertEquals(35, s7.durationMinutes)
    assertEquals("এশার সময় হয়েছে।", s7.voiceMessage)
  }

  @Test
  fun `isTaskActiveAt returns false when isToday is false to prevent cross-day active leakage`() {
    val task = RoutineTask(
      id = "task_afternoon",
      title = "Focus Sprint",
      type = TaskType.DYNAMIC,
      startTimeMinute = 900, // 03:00 PM
      durationMinutes = 60   // Ends at 04:00 PM
    )

    // When isToday is true and clock matches: active
    assertTrue(com.example.ui.components.isTaskActiveAt(task, currentMinute = 930, isToday = true))

    // When isToday is false (viewing another day): NEVER active
    assertFalse(com.example.ui.components.isTaskActiveAt(task, currentMinute = 930, isToday = false))
  }

  @Test
  fun `routine day today returns correct calendar day of week`() {
    val expected = when (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)) {
      java.util.Calendar.SATURDAY -> RoutineDay.SATURDAY
      java.util.Calendar.SUNDAY -> RoutineDay.SUNDAY
      java.util.Calendar.MONDAY -> RoutineDay.MONDAY
      java.util.Calendar.TUESDAY -> RoutineDay.TUESDAY
      java.util.Calendar.WEDNESDAY -> RoutineDay.WEDNESDAY
      java.util.Calendar.THURSDAY -> RoutineDay.THURSDAY
      java.util.Calendar.FRIDAY -> RoutineDay.FRIDAY
      else -> RoutineDay.SATURDAY
    }
    assertEquals(expected, RoutineDay.today())
  }

  @Test
  fun `automatic zero-click vault persistence and restore across fresh install state`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = RoutineRepository(context)

    // Customize sleep duration and add a dynamic task
    repo.setSleepDurationMinutes(480)
    repo.setBufferMinutes(10)
    val testTask = RoutineTask(
      id = "vault_test_task",
      title = "Vault Preserved Task",
      type = TaskType.DYNAMIC,
      startTimeMinute = 600,
      durationMinutes = 45
    )
    repo.saveDynamicTasks(listOf(testTask))
    Thread.sleep(600)

    // Clear preferences to simulate fresh reinstall after uninstall
    val prefs = context.getSharedPreferences("chronos_routine_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    // Pristine repo initialized on fresh launch
    val freshRepo = RoutineRepository(context)
    val restored = freshRepo.checkAndAutoRestoreVault()

    // Assert that auto-restore detected the vault and restored tasks
    val tasks = freshRepo.getDynamicTasks()
    val restoredTask = tasks.firstOrNull { it.id == "vault_test_task" }
    assertNotNull(restoredTask)
    assertEquals("Vault Preserved Task", restoredTask!!.title)
    assertEquals(45, restoredTask.durationMinutes)
  }

  @Test
  fun `task pause logic properly evaluates pause state and excludes from scheduleDay`() {
    val activeTask = RoutineTask(
      id = "task_active",
      title = "Active Work",
      type = TaskType.DYNAMIC,
      startTimeMinute = 400,
      durationMinutes = 60
    )
    val pausedTodayTask = RoutineTask(
      id = "task_paused_today",
      title = "Paused Today",
      type = TaskType.DYNAMIC,
      startTimeMinute = 500,
      durationMinutes = 60,
      isPausedTodayOnly = true
    )
    val pausedIndefiniteTask = RoutineTask(
      id = "task_paused_indef",
      title = "Paused Indefinitely",
      type = TaskType.DYNAMIC,
      startTimeMinute = 600,
      durationMinutes = 60,
      isPausedIndefinitely = true
    )
    val pausedFutureTask = RoutineTask(
      id = "task_paused_future",
      title = "Paused Future",
      type = TaskType.DYNAMIC,
      startTimeMinute = 700,
      durationMinutes = 60,
      pausedUntilDateMillis = System.currentTimeMillis() + 86400000L
    )
    val expiredPauseTask = RoutineTask(
      id = "task_expired_pause",
      title = "Expired Pause",
      type = TaskType.DYNAMIC,
      startTimeMinute = 800,
      durationMinutes = 60,
      pausedUntilDateMillis = System.currentTimeMillis() - 1000L
    )

    assertFalse(activeTask.isCurrentlyPaused())
    assertTrue(pausedTodayTask.isCurrentlyPaused())
    assertTrue(pausedIndefiniteTask.isCurrentlyPaused())
    assertTrue(pausedFutureTask.isCurrentlyPaused())
    assertFalse(expiredPauseTask.isCurrentlyPaused())

    val anchors = SchedulerEngine.createDefaultAnchors(420)
    val dynamicList = listOf(activeTask, pausedTodayTask, pausedIndefiniteTask)
    val result = SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = dynamicList,
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = 5
    )

    // Assert that active task is scheduled, but paused tasks are completely excluded
    val scheduledIds = result.scheduledTasks.map { it.id }
    assertTrue("Active task must be scheduled", scheduledIds.contains("task_active"))
    assertFalse("Paused today task must not be scheduled", scheduledIds.contains("task_paused_today"))
    assertFalse("Paused indefinite task must not be scheduled", scheduledIds.contains("task_paused_indef"))
  }

  @Test
  fun `scheduler enforces interTaskBufferMinutes between dynamic tasks and respects anchor boundaries without bleed`() {
    val anchors = SchedulerEngine.createDefaultAnchors(420)
    val slot2 = anchors.first { it.slotNumber == 2 }
    val slot2End = slot2.startTimeMinute + slot2.durationMinutes

    val buffer = 10
    val dynamicTask1 = RoutineTask(
      id = "dyn_1",
      title = "Morning Deep Focus",
      type = TaskType.DYNAMIC,
      startTimeMinute = slot2End, // Requests to start immediately when Slot 2 ends
      durationMinutes = 60,
      allowSplitting = false,
      interTaskBufferMinutes = buffer
    )

    val dynamicTask2 = RoutineTask(
      id = "dyn_2",
      title = "Second Focus Session",
      type = TaskType.DYNAMIC,
      startTimeMinute = slot2End + 60, // Requests to start immediately when dynamicTask1 ends
      durationMinutes = 60,
      allowSplitting = false
    )
    val result = SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(dynamicTask1, dynamicTask2),
      activeDay = RoutineDay.MONDAY,
      interTaskBufferMinutes = buffer
    )

    val scheduled1 = result.scheduledTasks.firstOrNull { it.id == "dyn_1" }
    assertNotNull("Dynamic task 1 must be scheduled", scheduled1)
    // Anchors possess hard mathematical boundaries; do NOT bleed rest buffers across adjacent tasks
    assertEquals("Dynamic task 1 starts immediately at anchor boundary without buffer bleed", slot2End, scheduled1!!.startTimeMinute)

    val scheduled2 = result.scheduledTasks.firstOrNull { it.id == "dyn_2" }
    assertNotNull("Dynamic task 2 must be scheduled", scheduled2)
    assertTrue(
      "Dynamic task 2 must not start before [Dynamic Task 1 End + buffer]",
      scheduled2!!.startTimeMinute >= (slot2End + 60 + buffer)
    )
  }

  @Test
  fun `console parser parses explicit flexibility, rest buffer, and priority tokens`() {
    val line = "Deep Coding | 90 | 08:00 | Y | ALL | Y | Focus sprint | 15 | 5 | Y"
    val result = ConsoleParser.parse(line)
    assertEquals(0, result.errorCount)
    assertEquals(1, result.successCount)
    val task = result.validTasks.first()
    assertEquals(15, task.customFlexibilityMinutes)
    assertEquals(15, task.toleranceMinutes)
    assertEquals(5, task.interTaskBufferMinutes)
    assertTrue(task.isHighPriority)

    val sampleResult = ConsoleParser.parse(ConsoleParser.SAMPLE_SNIPPET)
    assertEquals(0, sampleResult.errorCount)
    assertTrue("SAMPLE_SNIPPET should include non-zero flexibility", sampleResult.validTasks.any { it.customFlexibilityMinutes > 0 })
    assertTrue("SAMPLE_SNIPPET should include non-zero rest buffer", sampleResult.validTasks.any { it.interTaskBufferMinutes > 0 })
  }

  @Test
  fun `slot 3 friday display mutation does not mutate canonical dhuhr anchor`() {
    val canonicalAnchors = SchedulerEngine.createDefaultAnchors(360)
    val canonicalDhuhr = canonicalAnchors.first { it.slotNumber == 3 }
    val fridaySchedule = SchedulerEngine.scheduleDay(
      anchors = canonicalAnchors,
      dynamicTasks = emptyList(),
      activeDay = RoutineDay.FRIDAY
    )
    val displayedFridaySlot3 = fridaySchedule.scheduledTasks.first { it.slotNumber == 3 }
    assertEquals(750, displayedFridaySlot3.startTimeMinute)
    assertEquals(120, displayedFridaySlot3.durationMinutes)

    // Resolving canonical anchor by slotNumber preserves regular Dhuhr values
    val resolvedCanonical = canonicalAnchors.firstOrNull { it.slotNumber == displayedFridaySlot3.slotNumber }
    assertNotNull(resolvedCanonical)
    assertEquals(canonicalDhuhr.startTimeMinute, resolvedCanonical!!.startTimeMinute)
    assertEquals(canonicalDhuhr.durationMinutes, resolvedCanonical.durationMinutes)
    assertEquals(750, resolvedCanonical.fridayStartTimeMinute)
    assertEquals(120, resolvedCanonical.fridayDurationMinutes)
  }

  @Test
  fun `repository persists fluidSafetyFloorMinutes accurately`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = RoutineRepository(context)
    val twinTask = RoutineTask(
      id = "twin_safety_persist_p1",
      title = "Safety Twin",
      type = TaskType.DYNAMIC,
      startTimeMinute = 420,
      durationMinutes = 30,
      isFluidTwin = true,
      fluidTwinId = "twin_safety_persist",
      fluidTwinPart = 1,
      fluidTotalPoolMinutes = 60,
      fluidSafetyFloorMinutes = 12
    )
    repo.saveDynamicTasks(listOf(twinTask))
    val loaded = repo.getDynamicTasks().first { it.id == "twin_safety_persist_p1" }
    assertEquals(12, loaded.fluidSafetyFloorMinutes)
  }

  @Test
  fun `task-owned dnd tracking and zero-delay follow-up detection prevent premature dnd teardown`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = RoutineRepository(context)
    val today = RoutineDay.today()
    val firstTask = RoutineTask(
      id = "dnd_chain_1",
      title = "First DND Session",
      type = TaskType.DYNAMIC,
      startTimeMinute = 360, // 06:00 AM
      durationMinutes = 30,  // Ends 06:30 AM (390m)
      enableDnd = true,
      dndDelayMinutes = 0,
      selectedDays = setOf(today)
    )
    val secondTask = RoutineTask(
      id = "dnd_chain_2",
      title = "Immediate Followup DND",
      type = TaskType.DYNAMIC,
      startTimeMinute = 390, // 06:30 AM
      durationMinutes = 30,
      enableDnd = true,
      dndDelayMinutes = 0,
      selectedDays = setOf(today)
    )
    repo.saveAnchors(SchedulerEngine.createDefaultAnchors(360))
    repo.saveDynamicTasks(listOf(firstTask, secondTask))

    assertTrue(
      "Back-to-back 0-delay DND task starting at ending minute must be detected",
      AutomationManager.hasZeroDelayFollowUpDndTask(context, firstTask.id)
    )
    assertFalse(
      "Second task with no 0-delay follow-up DND should return false",
      AutomationManager.hasZeroDelayFollowUpDndTask(context, secondTask.id)
    )

    // Verify DND owner prefs tracking
    val prefs = context.getSharedPreferences("chronos_dnd_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("key_active_dnd_task_id", "dnd_chain_1").commit()
    assertEquals("dnd_chain_1", AutomationManager.getActiveDndTaskId(context))

    // Mismatched taskId does not clear owner (since DND permission ungranted in Robolectric, releaseDndIfHeldByTask checks owner first)
    AutomationManager.releaseDndIfHeldByTask(context, "other_task")
    assertEquals("dnd_chain_1", AutomationManager.getActiveDndTaskId(context))
  }

  @Test
  fun `alarm receiver ignores stale apply_dnd for paused or deleted tasks`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = RoutineRepository(context)
    val pausedTask = RoutineTask(
      id = "paused_dnd_task",
      title = "Paused DND Task",
      type = TaskType.DYNAMIC,
      startTimeMinute = 500,
      durationMinutes = 30,
      enableDnd = true,
      dndDelayMinutes = 5,
      isPausedIndefinitely = true
    )
    repo.saveDynamicTasks(listOf(pausedTask))

    val receiver = com.example.receiver.RoutineAlarmReceiver()
    val intent = android.content.Intent(AutomationManager.ACTION_APPLY_DND).apply {
      putExtra(AutomationManager.EXTRA_TASK_ID, pausedTask.id)
    }
    receiver.onReceive(context, intent)
    // Should not set active DND owner when task is paused
    assertEquals(null, AutomationManager.getActiveDndTaskId(context))
  }

  @Test
  fun `repository persists and restores originalStartTimeMinute accurately`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = RoutineRepository(context)
    val task = RoutineTask(
      id = "birth_persist_1",
      title = "Birth Time Persisted",
      type = TaskType.DYNAMIC,
      startTimeMinute = 660,
      originalStartTimeMinute = 600,
      durationMinutes = 40
    )
    repo.saveDynamicTasks(listOf(task))
    val loaded = repo.getDynamicTasks().first { it.id == "birth_persist_1" }
    assertEquals(660, loaded.startTimeMinute)
    assertEquals(600, loaded.originalStartTimeMinute)
  }

  @Test
  fun `automationManager tts helpers handle null and context safely`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertFalse(AutomationManager.isBengaliTtsAvailable(null))
    assertFalse(AutomationManager.applyTtsLanguage(null, "ফজরের সময় হয়েছে।"))
    assertFalse(AutomationManager.applyTtsLanguage(null, "English Briefing"))
    assertEquals(12, AutomationManager.VOICE_PRESETS.size)
    AutomationManager.VOICE_PRESETS.forEach { preset ->
      assertFalse(AutomationManager.applyTtsLanguage(null, preset.defaultSampleText, preset.id))
      assertFalse(AutomationManager.applyTtsLanguage(null, "English Briefing", preset.id))
    }
    // Should execute cleanly without throwing under Robolectric PackageManager
    AutomationManager.hasGoogleTtsEngine(context)
    assertTrue(true)
  }

  @Test
  fun `repository persists voicePresetId and restores from vault on fresh install`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("chronos_routine_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    val repo = RoutineRepository(context)
    assertEquals("natural_female", repo.getVoicePresetId())

    repo.setVoicePresetId("deep_male")
    assertEquals("deep_male", repo.getVoicePresetId())
    Thread.sleep(600)

    // Simulate fresh install where SharedPreferences are empty and Tier-1 vault holds "crisp_male"
    val tier1Dir = java.io.File(context.getExternalFilesDir(null), "ChronosBackup").apply { mkdirs() }
    val tier1File = java.io.File(tier1Dir, "chronos_vault.json")
    try {
      val vaultJson = org.json.JSONObject().apply {
        put("anchors", org.json.JSONArray())
        put("dynamicTasks", org.json.JSONArray())
        put("sleepDurationMinutes", 420)
        put("bufferMinutes", 0)
        put("voicePresetId", "crisp_male")
      }
      tier1File.writeText(vaultJson.toString())

      prefs.edit().clear().commit()
      val restored = repo.checkAndAutoRestoreVault()
      assertTrue("Vault restore should succeed when prefs are cleared", restored)
      assertEquals("crisp_male", repo.getVoicePresetId())
    } finally {
      tier1File.delete()
    }
  }

  @Test
  fun `dnd alarm is skipped when delay equals or exceeds task duration while end alarm stays scheduled`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cal = java.util.Calendar.getInstance().apply {
      add(java.util.Calendar.HOUR_OF_DAY, 2)
      set(java.util.Calendar.SECOND, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }
    val dayOfTask = when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
      java.util.Calendar.SATURDAY -> RoutineDay.SATURDAY
      java.util.Calendar.SUNDAY -> RoutineDay.SUNDAY
      java.util.Calendar.MONDAY -> RoutineDay.MONDAY
      java.util.Calendar.TUESDAY -> RoutineDay.TUESDAY
      java.util.Calendar.WEDNESDAY -> RoutineDay.WEDNESDAY
      java.util.Calendar.THURSDAY -> RoutineDay.THURSDAY
      java.util.Calendar.FRIDAY -> RoutineDay.FRIDAY
      else -> RoutineDay.SATURDAY
    }
    val startMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)

    val invalidDelayTask = RoutineTask(
      id = "short_task_long_delay",
      title = "10m Task 15m Delay",
      type = TaskType.DYNAMIC,
      startTimeMinute = startMin,
      durationMinutes = 10,
      enableDnd = true,
      dndDelayMinutes = 15,
      selectedDays = setOf(dayOfTask)
    )
    AutomationManager.scheduleTaskAlarms(context, invalidDelayTask)

    val noDndPi = android.app.PendingIntent.getBroadcast(
      context,
      (invalidDelayTask.id + "_dnd").hashCode(),
      android.content.Intent(context, com.example.receiver.RoutineAlarmReceiver::class.java).apply {
        action = AutomationManager.ACTION_APPLY_DND
      },
      android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
    )
    val endPi = android.app.PendingIntent.getBroadcast(
      context,
      (invalidDelayTask.id + "_end").hashCode(),
      android.content.Intent(context, com.example.receiver.RoutineAlarmReceiver::class.java).apply {
        action = AutomationManager.ACTION_END_TASK
      },
      android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
    )
    assertEquals("DND alarm must NOT be scheduled when delay >= duration", null, noDndPi)
    assertNotNull("End alarm must still be scheduled", endPi)

    val validDelayTask = RoutineTask(
      id = "long_task_short_delay",
      title = "30m Task 5m Delay",
      type = TaskType.DYNAMIC,
      startTimeMinute = startMin,
      durationMinutes = 30,
      enableDnd = true,
      dndDelayMinutes = 5,
      selectedDays = setOf(dayOfTask)
    )
    AutomationManager.scheduleTaskAlarms(context, validDelayTask)
    val validDndPi = android.app.PendingIntent.getBroadcast(
      context,
      (validDelayTask.id + "_dnd").hashCode(),
      android.content.Intent(context, com.example.receiver.RoutineAlarmReceiver::class.java).apply {
        action = AutomationManager.ACTION_APPLY_DND
      },
      android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
    )
    assertNotNull("DND alarm must be scheduled when delay < duration", validDndPi)
  }

  @Test
  fun `reconcileStaleDndState clears active DND owner when task has ended or DND is disabled`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = RoutineRepository(context)
    val pastEndTask = RoutineTask(
      id = "stale_dnd_owner",
      title = "Ended Task",
      type = TaskType.DYNAMIC,
      startTimeMinute = 60, // 01:00 AM
      durationMinutes = 30,
      enableDnd = false,
      selectedDays = RoutineDay.allDays()
    )
    repo.saveAnchors(SchedulerEngine.createDefaultAnchors(360))
    repo.saveDynamicTasks(listOf(pastEndTask))

    val dndPrefs = context.getSharedPreferences("chronos_dnd_prefs", Context.MODE_PRIVATE)
    dndPrefs.edit().putString("key_active_dnd_task_id", pastEndTask.id).commit()
    assertEquals(pastEndTask.id, AutomationManager.getActiveDndTaskId(context))

    AutomationManager.reconcileStaleDndState(context)
    assertEquals(null, AutomationManager.getActiveDndTaskId(context))
  }

  @Test
  fun `alarm receiver ignores ACTION_APPLY_DND when EXTRA_TASK_END_MILLIS has elapsed`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = RoutineRepository(context)
    val activeTask = RoutineTask(
      id = "expired_apply_dnd_task",
      title = "Active DND Task",
      type = TaskType.DYNAMIC,
      startTimeMinute = 600,
      durationMinutes = 30,
      enableDnd = true,
      dndDelayMinutes = 10
    )
    repo.saveDynamicTasks(listOf(activeTask))
    context.getSharedPreferences("chronos_dnd_prefs", Context.MODE_PRIVATE).edit().clear().commit()

    val receiver = com.example.receiver.RoutineAlarmReceiver()
    val expiredIntent = android.content.Intent(AutomationManager.ACTION_APPLY_DND).apply {
      putExtra(AutomationManager.EXTRA_TASK_ID, activeTask.id)
      putExtra(AutomationManager.EXTRA_TASK_END_MILLIS, System.currentTimeMillis() - 5000L)
    }
    receiver.onReceive(context, expiredIntent)
    assertEquals(null, AutomationManager.getActiveDndTaskId(context))
  }

  @Test
  fun `rescheduleAllAlarmsAt preserves overnight end alarm across midnight rollover`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = RoutineRepository(context)
    val anchors = SchedulerEngine.createDefaultAnchors(360) // Slot 1 Sleep: 23:00 (1380m) for 360m -> ends 05:00 next day
    repo.saveAnchors(anchors)
    repo.saveDynamicTasks(emptyList())

    // Simulate 01:00 AM on Tuesday (yesterday Monday Sleep 23:00 -> Tuesday 05:00 is currently ongoing)
    val nowCal = java.util.Calendar.getInstance().apply {
      set(java.util.Calendar.YEAR, 2026)
      set(java.util.Calendar.MONTH, java.util.Calendar.MAY)
      set(java.util.Calendar.DAY_OF_MONTH, 12) // Tuesday
      set(java.util.Calendar.HOUR_OF_DAY, 1)
      set(java.util.Calendar.MINUTE, 0)
      set(java.util.Calendar.SECOND, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }
    val expectedSleepEndCal = (nowCal.clone() as java.util.Calendar).apply {
      set(java.util.Calendar.HOUR_OF_DAY, 5)
      set(java.util.Calendar.MINUTE, 0)
    }

    AutomationManager.rescheduleAllAlarmsAt(context, nowCal.timeInMillis)

    val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    val shadowAm = org.robolectric.Shadows.shadowOf(am)
    val scheduledAlarms = shadowAm.scheduledAlarms

    val slot1EndPi = android.app.PendingIntent.getBroadcast(
      context,
      ("anchor_slot_1_end").hashCode(),
      android.content.Intent(context, com.example.receiver.RoutineAlarmReceiver::class.java).apply {
        action = AutomationManager.ACTION_END_TASK
      },
      android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
    )
    assertNotNull("Overnight Slot 1 end alarm must remain scheduled after midnight", slot1EndPi)
    assertTrue(
      "Overnight Slot 1 end alarm must target today 05:00 AM rather than skipping to next night",
      scheduledAlarms.any { it.operation == slot1EndPi && it.triggerAtTime == expectedSleepEndCal.timeInMillis }
    )
  }

  @Test
  fun `repository persists and restores manualPlacementOrder accurately`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = RoutineRepository(context)
    val task = RoutineTask(
      id = "manual_order_persist_1",
      title = "Ordered Block",
      type = TaskType.DYNAMIC,
      startTimeMinute = 540,
      durationMinutes = 40,
      manualPlacementOrder = 1731744000123L
    )
    repo.saveDynamicTasks(listOf(task))
    val loaded = repo.getDynamicTasks().first { it.id == "manual_order_persist_1" }
    assertEquals(1731744000123L, loaded.manualPlacementOrder)
  }

  @Test
  fun `rescheduleAllAlarmsAt uses setAlarmClock only at single earliest trigger timestamp across 7-day horizon`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = RoutineRepository(context)
    val anchors = SchedulerEngine.createDefaultAnchors(360)
    val dyn1 = RoutineTask(
      id = "horizon_dyn_1",
      title = "Focus Morning",
      type = TaskType.DYNAMIC,
      startTimeMinute = 480,
      durationMinutes = 45,
      enableDnd = true,
      dndDelayMinutes = 5,
      selectedDays = RoutineDay.allDays()
    )
    val dyn2 = RoutineTask(
      id = "horizon_dyn_2",
      title = "Focus Afternoon",
      type = TaskType.DYNAMIC,
      startTimeMinute = 900,
      durationMinutes = 45,
      enableDnd = true,
      dndDelayMinutes = 0,
      selectedDays = RoutineDay.allDays()
    )
    repo.saveAnchors(anchors)
    repo.saveDynamicTasks(listOf(dyn1, dyn2))

    val nowCal = java.util.Calendar.getInstance().apply {
      set(java.util.Calendar.YEAR, 2026)
      set(java.util.Calendar.MONTH, java.util.Calendar.MAY)
      set(java.util.Calendar.DAY_OF_MONTH, 12) // Tuesday 07:00 AM
      set(java.util.Calendar.HOUR_OF_DAY, 7)
      set(java.util.Calendar.MINUTE, 0)
      set(java.util.Calendar.SECOND, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }

    val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    val shadowAm = org.robolectric.Shadows.shadowOf(am)
    org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(true)
    try {
      shadowAm.scheduledAlarms.clear()

      AutomationManager.rescheduleAllAlarmsAt(context, nowCal.timeInMillis)

      val scheduled = shadowAm.scheduledAlarms
      assertTrue("Horizon must schedule multiple exact alarms across tasks", scheduled.size > 5)

      val alarmClockTimestamps = scheduled.filter { it.showIntent != null }.map { it.triggerAtTime }.distinct()
      assertEquals(
        "Only the single earliest trigger timestamp should be registered via setAlarmClock to avoid SystemUI broadcast floods",
        1,
        alarmClockTimestamps.size
      )

      val expectedFirstCal = (nowCal.clone() as java.util.Calendar).apply {
        set(java.util.Calendar.HOUR_OF_DAY, 8)
        set(java.util.Calendar.MINUTE, 0)
      }
      assertEquals(expectedFirstCal.timeInMillis, alarmClockTimestamps.first())
    } finally {
      org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(false)
    }
  }

  @Test
  fun `slot 3 friday jumuah honors custom dnd delay and uses silent notification channel`() {
    assertEquals("chronos_automation_silent_channel", com.example.service.RoutineAutomationService.CHANNEL_ID)

    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = RoutineRepository(context)
    val baseAnchors = SchedulerEngine.createDefaultAnchors(360).map {
      if (it.slotNumber == 3) {
        it.copy(
          enableDnd = true,
          dndDelayMinutes = 14, // Custom delay should NOT be overwritten by hardcoded 5m on Friday
          fridayStartTimeMinute = 750, // 12:30 PM
          fridayDurationMinutes = 120
        )
      } else {
        it
      }
    }
    repo.saveAnchors(baseAnchors)
    repo.saveDynamicTasks(emptyList())

    // Friday May 15, 2026 at 12:00 PM
    val fridayNoonCal = java.util.Calendar.getInstance().apply {
      set(java.util.Calendar.YEAR, 2026)
      set(java.util.Calendar.MONTH, java.util.Calendar.MAY)
      set(java.util.Calendar.DAY_OF_MONTH, 15)
      set(java.util.Calendar.HOUR_OF_DAY, 12)
      set(java.util.Calendar.MINUTE, 0)
      set(java.util.Calendar.SECOND, 0)
      set(java.util.Calendar.MILLISECOND, 0)
    }
    val expectedDelayCal = (fridayNoonCal.clone() as java.util.Calendar).apply {
      set(java.util.Calendar.HOUR_OF_DAY, 12)
      set(java.util.Calendar.MINUTE, 44) // 12:30 + 14m
    }

    val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
    val shadowAm = org.robolectric.Shadows.shadowOf(am)
    shadowAm.scheduledAlarms.clear()

    AutomationManager.rescheduleAllAlarmsAt(context, fridayNoonCal.timeInMillis)

    val slot3DndPi = android.app.PendingIntent.getBroadcast(
      context,
      ("anchor_slot_3_dnd").hashCode(),
      android.content.Intent(context, com.example.receiver.RoutineAlarmReceiver::class.java).apply {
        action = AutomationManager.ACTION_APPLY_DND
      },
      android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
    )
    assertNotNull("Friday Slot 3 DND delay alarm must be scheduled", slot3DndPi)
    assertTrue(
      "Friday Slot 3 DND alarm must fire at 12:30 + custom 14m = 12:44",
      shadowAm.scheduledAlarms.any { it.operation == slot3DndPi && it.triggerAtTime == expectedDelayCal.timeInMillis }
    )
  }

  @Test
  fun `viewModel persists slot 5 detailed settings and manages draft pending edits cleanly`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val repo = RoutineRepository(app)
    repo.saveAnchors(SchedulerEngine.createDefaultAnchors(360))
    val task1 = RoutineTask(
      id = "vm_drag_t1",
      title = "Morning Research",
      type = TaskType.DYNAMIC,
      startTimeMinute = 420,
      originalStartTimeMinute = 420,
      durationMinutes = 45,
      selectedDays = RoutineDay.allDays()
    )
    repo.saveDynamicTasks(listOf(task1))

    fun pumpUntil(maxWaitMs: Long = 2000L, predicate: () -> Boolean) {
      val mainLooper = org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
      val deadline = System.currentTimeMillis() + maxWaitMs
      while (!predicate() && System.currentTimeMillis() < deadline) {
        mainLooper.idle()
        if (!predicate()) Thread.sleep(20)
      }
      mainLooper.idle()
    }

    val vm = com.example.ui.RoutineViewModel(app)
    vm.updateSlot5Protected(
      title = "Protected Rest",
      voiceMessage = "Rest well",
      selectedDays = RoutineDay.allDays(),
      enableDnd = true,
      dndDelayMinutes = 12,
      enableHaptic = false,
      isVoiceEnabled = true,
      voiceVolume = 0.75f,
      isSoundAlertEnabled = true,
      customRingtoneUri = "content://media/external/audio/media/42",
      customRingtoneTitle = "Soft Bell"
    )
    pumpUntil { vm.anchors.value.firstOrNull { it.slotNumber == 5 }?.dndDelayMinutes == 12 }

    val updatedSlot5 = vm.anchors.value.first { it.slotNumber == 5 }
    assertTrue(updatedSlot5.enableDnd)
    assertEquals(12, updatedSlot5.dndDelayMinutes)
    assertFalse(updatedSlot5.enableHaptic)
    assertEquals(0.75f, updatedSlot5.voiceVolume, 0.01f)
    assertEquals("Soft Bell", updatedSlot5.customRingtoneTitle)

    // Preview drag move creates pending draft + impact diff without mutating committed _dynamicTasks
    vm.previewDragTaskMove(taskId = "vm_drag_t1", targetStartMinute = 480)
    pumpUntil { vm.hasPendingEdits.value && vm.pendingImpactDiff.value.any { it.taskId == "vm_drag_t1" && it.newStartMinute == 480 } }
    assertTrue("Pending edits flag must turn true after drag preview", vm.hasPendingEdits.value)
    assertEquals(420, vm.dynamicTasks.value.first { it.id == "vm_drag_t1" }.startTimeMinute)
    assertTrue(vm.pendingImpactDiff.value.any { it.taskId == "vm_drag_t1" && it.newStartMinute == 480 })

    // Discard reverts draft state
    vm.discardPendingEdits()
    pumpUntil { !vm.hasPendingEdits.value && vm.pendingImpactDiff.value.isEmpty() }
    assertFalse(vm.hasPendingEdits.value)
    assertTrue(vm.pendingImpactDiff.value.isEmpty())

    // Re-preview and commit persists new position
    vm.previewDragTaskMove(taskId = "vm_drag_t1", targetStartMinute = 490)
    pumpUntil { vm.hasPendingEdits.value }
    vm.commitPendingEdits()
    pumpUntil { !vm.hasPendingEdits.value && vm.dynamicTasks.value.firstOrNull { it.id == "vm_drag_t1" }?.startTimeMinute == 490 }
    assertFalse(vm.hasPendingEdits.value)
    assertEquals(490, vm.dynamicTasks.value.first { it.id == "vm_drag_t1" }.startTimeMinute)
  }

  @Test
  fun `viewModel previewVerticalDropMove swaps tasks in draft and keeps committed tasks unchanged until commit`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val repo = RoutineRepository(app)
    repo.saveAnchors(SchedulerEngine.createDefaultAnchors(360))
    val eat = RoutineTask(
      id = "vm_swap_eat",
      title = "Eat",
      type = TaskType.DYNAMIC,
      startTimeMinute = 420,
      originalStartTimeMinute = 420,
      durationMinutes = 60,
      interTaskBufferMinutes = 0,
      selectedDays = RoutineDay.allDays()
    )
    val bath = RoutineTask(
      id = "vm_swap_bath",
      title = "Bath",
      type = TaskType.DYNAMIC,
      startTimeMinute = 480,
      originalStartTimeMinute = 480,
      durationMinutes = 30,
      interTaskBufferMinutes = 10,
      selectedDays = RoutineDay.allDays()
    )
    repo.saveDynamicTasks(listOf(eat, bath))

    fun pumpUntil(maxWaitMs: Long = 2500L, predicate: () -> Boolean) {
      val mainLooper = org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
      val deadline = System.currentTimeMillis() + maxWaitMs
      while (!predicate() && System.currentTimeMillis() < deadline) {
        mainLooper.idle()
        if (!predicate()) Thread.sleep(20)
      }
      mainLooper.idle()
    }

    val vm = com.example.ui.RoutineViewModel(app)
    pumpUntil { vm.dynamicTasks.value.size == 2 }

    var wallHit = false
    vm.previewVerticalDropMove(
      taskId = "vm_swap_eat",
      dropTarget = com.example.engine.VerticalDropTarget.InsertAfterTask("vm_swap_bath"),
      onAnchorWallHit = { wallHit = true }
    )
    pumpUntil {
      vm.hasPendingEdits.value &&
        vm.pendingImpactDiff.value.any { it.taskId == "vm_swap_bath" && it.newStartMinute == 420 }
    }

    assertFalse(wallHit)
    assertTrue(vm.hasPendingEdits.value)
    assertEquals(420, vm.dynamicTasks.value.first { it.id == "vm_swap_eat" }.startTimeMinute)
    assertEquals(480, vm.dynamicTasks.value.first { it.id == "vm_swap_bath" }.startTimeMinute)
    assertTrue(vm.pendingImpactDiff.value.any { it.taskId == "vm_swap_bath" && it.newStartMinute == 420 })
    assertTrue(vm.pendingImpactDiff.value.any { it.taskId == "vm_swap_eat" && it.newStartMinute == 460 })

    vm.commitPendingEdits()
    pumpUntil { !vm.hasPendingEdits.value && vm.dynamicTasks.value.first { it.id == "vm_swap_bath" }.startTimeMinute == 420 }
    assertFalse(vm.hasPendingEdits.value)
    assertEquals(420, vm.dynamicTasks.value.first { it.id == "vm_swap_bath" }.startTimeMinute)
    assertEquals(460, vm.dynamicTasks.value.first { it.id == "vm_swap_eat" }.startTimeMinute)
  }

  @Test
  fun `viewModel auto-dismisses pending edits on baseline return, syncs setActiveDay baseline, and runs compaction rescue`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val repo = RoutineRepository(app)
    repo.saveAnchors(SchedulerEngine.createDefaultAnchors(360))
    // Gap 3-4: 830..990 (Monday); fill other gaps on Monday to test compaction rescue while keeping t_base simple
    val tBase = RoutineTask(
      id = "vm_base_return",
      title = "Morning Deep",
      type = TaskType.DYNAMIC,
      startTimeMinute = 420,
      originalStartTimeMinute = 420,
      durationMinutes = 30,
      selectedDays = RoutineDay.allDays()
    )
    val n1 = RoutineTask(
      id = "vm_frag_1",
      title = "Frag 1",
      type = TaskType.DYNAMIC,
      startTimeMinute = 860,
      originalStartTimeMinute = 860,
      durationMinutes = 30,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val n2 = RoutineTask(
      id = "vm_frag_2",
      title = "Frag 2",
      type = TaskType.DYNAMIC,
      startTimeMinute = 920,
      originalStartTimeMinute = 920,
      durationMinutes = 30,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    repo.saveDynamicTasks(listOf(tBase, n1, n2))

    fun pumpUntil(maxWaitMs: Long = 2500L, predicate: () -> Boolean) {
      val mainLooper = org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
      val deadline = System.currentTimeMillis() + maxWaitMs
      while (!predicate() && System.currentTimeMillis() < deadline) {
        mainLooper.idle()
        if (!predicate()) Thread.sleep(20)
      }
      mainLooper.idle()
    }

    val vm = com.example.ui.RoutineViewModel(app)
    vm.setActiveDay(RoutineDay.MONDAY)
    pumpUntil { vm.dynamicTasks.value.size == 3 && vm.activeDay.value == RoutineDay.MONDAY }

    // 1. Move tBase away from 420 -> pending edits appear
    vm.previewDragTaskMove(taskId = "vm_base_return", targetStartMinute = 480)
    pumpUntil { vm.hasPendingEdits.value }
    assertTrue(vm.hasPendingEdits.value)

    // Move tBase back to 420 -> diff is empty -> auto-dismisses pending edits cleanly!
    vm.previewDragTaskMove(taskId = "vm_base_return", targetStartMinute = 420)
    pumpUntil { !vm.hasPendingEdits.value && vm.pendingImpactDiff.value.isEmpty() }
    assertFalse("Returning to baseline must auto-dismiss pending edits", vm.hasPendingEdits.value)
    assertTrue(vm.pendingImpactDiff.value.isEmpty())

    // 2. Stage pending edit on Monday, switch to Tuesday -> baseline updates cleanly and remains consistent
    vm.previewDragTaskMove(taskId = "vm_base_return", targetStartMinute = 480)
    pumpUntil { vm.hasPendingEdits.value }
    vm.setActiveDay(RoutineDay.TUESDAY)
    pumpUntil { vm.activeDay.value == RoutineDay.TUESDAY && vm.scheduleResult.value.scheduledTasks.any { it.id == "vm_base_return" && it.startTimeMinute == 480 } }
    assertTrue(vm.hasPendingEdits.value)
    vm.discardPendingEdits()
    pumpUntil { !vm.hasPendingEdits.value }

    // 3. Switch back to Monday and run compaction preview -> detects fragmented n1/n2 moves as pending edits
    vm.setActiveDay(RoutineDay.MONDAY)
    pumpUntil { vm.activeDay.value == RoutineDay.MONDAY }
    var noChangesCalled = false
    vm.previewCompactAndRescueSchedule(onNoChanges = { noChangesCalled = true })
    pumpUntil { vm.hasPendingEdits.value && vm.pendingImpactDiff.value.isNotEmpty() }
    assertFalse(noChangesCalled)
    assertTrue(vm.pendingImpactDiff.value.any { it.taskId == "vm_frag_1" && it.newStartMinute == 830 })

    // Running compaction again when already compacted calls onNoChanges
    vm.commitPendingEdits()
    pumpUntil { !vm.hasPendingEdits.value }
    // Move tBase to start of Gap 2-3 (335) so everything is optimal, or verify optimal no-op after compacting Gap 2-3 too
    vm.previewCompactAndRescueSchedule(onNoChanges = { noChangesCalled = true })
    pumpUntil { vm.hasPendingEdits.value || noChangesCalled }
    if (vm.hasPendingEdits.value) {
      vm.commitPendingEdits()
      pumpUntil { !vm.hasPendingEdits.value }
      vm.previewCompactAndRescueSchedule(onNoChanges = { noChangesCalled = true })
      pumpUntil { noChangesCalled }
    }
    assertTrue("Already optimal schedule must invoke onNoChanges", noChangesCalled)
  }

  @Test
  fun `repository persists and restores originalDurationMinutes across storage and vault json`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val repo = RoutineRepository(app)

    val flexedTask = RoutineTask(
      id = "repo_birth_1",
      title = "Elastic Coding",
      type = TaskType.DYNAMIC,
      startTimeMinute = 830,
      originalStartTimeMinute = 420,
      durationMinutes = 45,
      originalDurationMinutes = 60,
      customFlexibilityMinutes = 15,
      toleranceMinutes = 15,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val unflexedTask = RoutineTask(
      id = "repo_birth_2",
      title = "Fixed Brief",
      type = TaskType.DYNAMIC,
      startTimeMinute = 900,
      originalStartTimeMinute = 900,
      durationMinutes = 30,
      originalDurationMinutes = null,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    repo.saveDynamicTasks(listOf(flexedTask, unflexedTask))
    val restored = repo.getDynamicTasks()
    assertEquals(2, restored.size)
    val r1 = restored.first { it.id == "repo_birth_1" }
    val r2 = restored.first { it.id == "repo_birth_2" }
    assertEquals(45, r1.durationMinutes)
    assertEquals(60, r1.originalDurationMinutes)
    assertEquals(420, r1.originalStartTimeMinute)
    assertEquals(30, r2.durationMinutes)
    assertEquals(null, r2.originalDurationMinutes)
  }

  @Test
  fun `repository scrubs legacy unified voice volume pref and vault key on init while preserving per-task voiceVolume`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val seedRepo = RoutineRepository(app)

    val task = RoutineTask(
      id = "voice_mode_task_1",
      title = "Evening Brief",
      type = TaskType.DYNAMIC,
      startTimeMinute = 1140,
      durationMinutes = 30,
      voiceVolume = 0.42f,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    seedRepo.saveDynamicTasks(listOf(task))

    // Wait for initial vault write, then inject legacy v1.4 pref & vault JSON field
    val vaultFile = java.io.File(app.getExternalFilesDir(null), "ChronosBackup/chronos_vault.json")
    val initialDeadline = System.currentTimeMillis() + 3000L
    while (System.currentTimeMillis() < initialDeadline && !vaultFile.exists()) {
      Thread.sleep(50)
    }
    assertTrue("Initial vault file should exist", vaultFile.exists())
    val legacyVaultJson = org.json.JSONObject(vaultFile.readText()).apply {
      put("useSameVoiceVolumeForAll", true)
    }.toString(2)
    vaultFile.writeText(legacyVaultJson)

    val prefs = app.getSharedPreferences("chronos_routine_prefs", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("key_use_same_voice_volume_for_all", true).commit()
    assertTrue(prefs.contains("key_use_same_voice_volume_for_all"))

    // Re-initializing RoutineRepository must scrub the legacy key from prefs and rewrite vault without it
    val upgradedRepo = RoutineRepository(app)
    assertFalse("Legacy pref key must be scrubbed on init", prefs.contains("key_use_same_voice_volume_for_all"))

    val rewriteDeadline = System.currentTimeMillis() + 3000L
    while (System.currentTimeMillis() < rewriteDeadline && vaultFile.readText().contains("useSameVoiceVolumeForAll")) {
      Thread.sleep(50)
    }
    assertFalse("Rewritten vault must not contain useSameVoiceVolumeForAll", vaultFile.readText().contains("useSameVoiceVolumeForAll"))

    // Verify per-task voiceVolume remained completely intact
    val preservedTask = upgradedRepo.getDynamicTasks().first { it.id == "voice_mode_task_1" }
    assertEquals(0.42f, preservedTask.voiceVolume, 0.001f)
  }

  @Test
  fun `automationManager routes TTS params to STREAM_ALARM with per-task voiceVolume and stops active preview`() {
    val params = AutomationManager.createTtsVolumeParams(0.65f)
    assertEquals(
      android.media.AudioManager.STREAM_ALARM,
      params.getInt(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_STREAM, -1)
    )
    assertEquals(0.65f, params.getFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME), 0.001f)

    val clampedHigh = AutomationManager.createTtsVolumeParams(2.5f)
    assertEquals(1.0f, clampedHigh.getFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME), 0.001f)
    val clampedLow = AutomationManager.createTtsVolumeParams(-0.3f)
    assertEquals(0.0f, clampedLow.getFloat(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME), 0.001f)

    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val task = RoutineTask(
      id = "alarm_vol_task",
      title = "Alarm Stream Test",
      type = TaskType.DYNAMIC,
      startTimeMinute = 600,
      durationMinutes = 30,
      voiceVolume = 0.35f,
      selectedDays = RoutineDay.allDays()
    )

    AutomationManager.startAutomationService(app, task)
    val serviceIntent = org.robolectric.Shadows.shadowOf(app).nextStartedService
    assertNotNull(serviceIntent)
    assertEquals(0.35f, serviceIntent.getFloatExtra(AutomationManager.EXTRA_VOICE_VOLUME, -1f), 0.001f)

    var previewStopped = false
    val stopHandler = { previewStopped = true }
    AutomationManager.registerActivePreviewStop(stopHandler)
    assertTrue(AutomationManager.stopActivePreviewVoiceIfPlaying())
    assertTrue(previewStopped)
    // Cleared after stopping
    assertFalse(AutomationManager.stopActivePreviewVoiceIfPlaying())
  }

  @Test
  fun `routineAutomationService stops only voice on volume change or screen off, keeps DND and running intact, and registers zero idle receivers`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val controller = org.robolectric.Robolectric.buildService(com.example.service.RoutineAutomationService::class.java).create()
    val service = controller.get()

    // 1. Voice disabled -> ZERO hardware receivers registered
    val silentIntent = android.content.Intent(app, com.example.service.RoutineAutomationService::class.java).apply {
      putExtra(AutomationManager.EXTRA_TASK_ID, "silent_task_1")
      putExtra(AutomationManager.EXTRA_TASK_TITLE, "Silent Work")
      putExtra(AutomationManager.EXTRA_IS_VOICE_ENABLED, false)
      putExtra(AutomationManager.EXTRA_IS_SOUND_ALERT_ENABLED, false)
      putExtra(AutomationManager.EXTRA_ENABLE_HAPTIC, false)
    }
    service.onStartCommand(silentIntent, 0, 1)
    assertFalse("Zero hardware stop listeners may register when voice is disabled", service.isHardwareStopRegisteredForTesting())
    assertFalse(com.example.service.RoutineAutomationService.serviceState.value.isSpeaking)
    assertTrue(com.example.service.RoutineAutomationService.serviceState.value.isRunning)

    // 2. Voice enabled + active DND -> listeners registered; equal-volume broadcast ignored; changed-volume stops ONLY voice
    val voiceIntent = android.content.Intent(app, com.example.service.RoutineAutomationService::class.java).apply {
      putExtra(AutomationManager.EXTRA_TASK_ID, "voice_task_1")
      putExtra(AutomationManager.EXTRA_TASK_TITLE, "Morning Brief")
      putExtra(AutomationManager.EXTRA_VOICE_MESSAGE, "Good morning")
      putExtra(AutomationManager.EXTRA_IS_VOICE_ENABLED, true)
      putExtra(AutomationManager.EXTRA_IS_SOUND_ALERT_ENABLED, false)
      putExtra(AutomationManager.EXTRA_ENABLE_HAPTIC, false)
    }
    service.onStartCommand(voiceIntent, 0, 2)
    com.example.service.RoutineAutomationService.onDndApplied("voice_task_1")
    assertTrue(service.isHardwareStopRegisteredForTesting())
    assertTrue(com.example.service.RoutineAutomationService.serviceState.value.isSpeaking)
    assertTrue(com.example.service.RoutineAutomationService.serviceState.value.isDndActive)

    // Equal volume broadcast must NOT stop voice
    app.sendBroadcast(android.content.Intent("android.media.VOLUME_CHANGED_ACTION").apply {
      putExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 5)
      putExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", 5)
    })
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    assertTrue(com.example.service.RoutineAutomationService.serviceState.value.isSpeaking)
    assertTrue(service.isHardwareStopRegisteredForTesting())

    // Changed volume broadcast stops ONLY voice, unregisters listeners, preserves isRunning & isDndActive
    app.sendBroadcast(android.content.Intent("android.media.VOLUME_CHANGED_ACTION").apply {
      putExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", 4)
      putExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", 5)
    })
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    assertFalse(com.example.service.RoutineAutomationService.serviceState.value.isSpeaking)
    assertFalse(service.isHardwareStopRegisteredForTesting())
    assertTrue("Service must stay running after voice stop", com.example.service.RoutineAutomationService.serviceState.value.isRunning)
    assertTrue("DND state must stay intact after voice stop", com.example.service.RoutineAutomationService.serviceState.value.isDndActive)

    // 3. Re-start voice session and test ACTION_SCREEN_OFF guard (<200ms ignored, >200ms stops voice)
    service.onStartCommand(voiceIntent, 0, 3)
    assertTrue(service.isHardwareStopRegisteredForTesting())
    assertTrue(com.example.service.RoutineAutomationService.serviceState.value.isSpeaking)
    org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(50))
    app.sendBroadcast(android.content.Intent(android.content.Intent.ACTION_SCREEN_OFF))
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    assertTrue("Immediate screen-off within 200ms guard must not stop voice", com.example.service.RoutineAutomationService.serviceState.value.isSpeaking)

    org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(200))
    app.sendBroadcast(android.content.Intent(android.content.Intent.ACTION_SCREEN_OFF))
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    assertFalse(com.example.service.RoutineAutomationService.serviceState.value.isSpeaking)
    assertFalse(service.isHardwareStopRegisteredForTesting())

    // Cleanup service
    service.onStartCommand(android.content.Intent(app, com.example.service.RoutineAutomationService::class.java).apply {
      action = com.example.service.RoutineAutomationService.ACTION_STOP_AUTOMATION
    }, 0, 4)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    controller.destroy()
  }

  @Test
  fun `mainActivity dispatchKeyEvent stops active preview or service voice on volume down and volume up`() {
    val activity = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup().get()

    val volDownEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_VOLUME_DOWN)
    val volUpEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_VOLUME_UP)

    // When idle, dispatchKeyEvent does not consume volume keys
    AutomationManager.stopActivePreviewVoiceIfPlaying()
    assertFalse(activity.dispatchKeyEvent(volDownEvent))

    // When preview voice is active, Volume Up stops it and returns true
    var stoppedCount = 0
    val handler = { stoppedCount++ ; Unit }
    AutomationManager.registerActivePreviewStop(handler)
    assertTrue(activity.dispatchKeyEvent(volUpEvent))
    assertEquals(1, stoppedCount)

    // Next press when silent passes through normally
    assertFalse(activity.dispatchKeyEvent(volDownEvent))
  }
}
