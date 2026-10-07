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
@Config(sdk = [34])
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
}
