package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.TaskType
import com.example.service.AutomationManager
import com.example.ui.components.TaskModalBottomSheet
import com.example.ui.screens.SettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TaskModalBottomSheetUiTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `editing existing single dynamic task hides split pill and keeps priority pill visible`() {
    val existingTask = RoutineTask(
      id = "edit_single_1",
      title = "Morning Focus",
      type = TaskType.DYNAMIC,
      startTimeMinute = 540, // 09:00 AM
      originalStartTimeMinute = 540,
      durationMinutes = 20,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    composeTestRule.setContent {
      MaterialTheme {
        TaskModalBottomSheet(
          initialTask = existingTask,
          onDismiss = {},
          onSaveTask = {},
          onSaveAnchorDetails = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
          onSaveSlot5 = { _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("header_priority_pill").assertIsDisplayed()
    composeTestRule.onNodeWithTag("header_split_pill").assertDoesNotExist()
    composeTestRule.onNodeWithTag("duration_hours_input").assertIsDisplayed()
    composeTestRule.onNodeWithTag("duration_mins_input").assertIsDisplayed()
    composeTestRule.onNodeWithText("ENDING TIME").assertIsDisplayed()
    composeTestRule.onNodeWithText(RoutineTask.formatTime(560)).assertIsDisplayed()
  }

  @Test
  fun `creating new dynamic task keeps both priority and split pills visible`() {
    composeTestRule.setContent {
      MaterialTheme {
        TaskModalBottomSheet(
          initialTask = null,
          onDismiss = {},
          onSaveTask = {},
          onSaveAnchorDetails = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
          onSaveSlot5 = { _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("header_priority_pill").assertIsDisplayed()
    composeTestRule.onNodeWithTag("header_split_pill").assertIsDisplayed()
    composeTestRule.onNodeWithText("ENDING TIME").assertIsDisplayed()
  }

  @Test
  fun `editing slot 1 anchor renders target sleep quota without reverse bedtime info box`() {
    val slot1 = RoutineTask(
      id = "anchor_slot_1",
      title = "Reverse Sleep Recovery",
      type = TaskType.ANCHOR_SLEEP,
      slotNumber = 1,
      startTimeMinute = 1380,
      durationMinutes = 360
    )

    composeTestRule.setContent {
      MaterialTheme {
        TaskModalBottomSheet(
          initialTask = slot1,
          onDismiss = {},
          onSaveTask = {},
          onSaveAnchorDetails = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
          onSaveSlot5 = { _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithText("TARGET SLEEP QUOTA").assertIsDisplayed()
    composeTestRule.onNodeWithText("REVERSE BEDTIME CALCULATION").assertDoesNotExist()
  }

  @Test
  fun `editing slot 5 anchor renders hardware grid without accordion buffer guarantee box`() {
    val slot5 = RoutineTask(
      id = "anchor_slot_5",
      title = "Elastic Rubber Buffer",
      type = TaskType.ANCHOR_ELASTIC,
      slotNumber = 5,
      startTimeMinute = 990,
      durationMinutes = 45
    )

    composeTestRule.setContent {
      MaterialTheme {
        TaskModalBottomSheet(
          initialTask = slot5,
          onDismiss = {},
          onSaveTask = {},
          onSaveAnchorDetails = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
          onSaveSlot5 = { _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithText("RECURRENCE SCHEDULE").performScrollTo()
    composeTestRule.onNodeWithTag("anchor_sound_switch").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("TASK ALERTS").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("AUTO DND").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("ACCORDION BUFFER GUARANTEE").assertDoesNotExist()
  }

  @Test
  fun `settings screen renders collapsible voice dropdown with filters, test input, and confirmation dialog`() {
    var selectedPreset = "natural_female"

    composeTestRule.setContent {
      MaterialTheme {
        SettingsScreen(
          bufferMinutes = 0,
          onUpdateBufferMinutes = {},
          selectedVoicePresetId = selectedPreset,
          onSelectVoicePreset = { selectedPreset = it }
        )
      }
    }

    composeTestRule.onNodeWithTag("voice_persona_card").performScrollTo().assertIsDisplayed()
    // Collapsed initially: presets & test input should not be shown until trigger tapped
    composeTestRule.onNodeWithTag("voice_persona_test_input").assertDoesNotExist()
    composeTestRule.onNodeWithTag("voice_dropdown_trigger").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Scroll outer vertical Column past the horizontalScroll category Row
    composeTestRule.onNodeWithTag("voice_preset_natural_female").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("voice_persona_test_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("voice_category_all").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("voice_category_female").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("voice_category_youth").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("voice_category_male").performScrollTo().assertIsDisplayed()

    // Filter by Youth 10-15y and verify youth voices appear while male voices filter out
    composeTestRule.onNodeWithTag("voice_category_youth").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("voice_preset_teen_girl").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("voice_preset_teen_boy").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("voice_preset_deep_male").assertDoesNotExist()

    // Switch back to Male & Studio and verify clicking deep_male opens confirmation dialog
    composeTestRule.onNodeWithTag("voice_category_male").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("voice_preset_deep_male").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("voice_confirm_dialog").assertIsDisplayed()

    // Cancel keeps current preset unchanged
    composeTestRule.onNodeWithTag("voice_confirm_cancel_button").performClick()
    composeTestRule.onNodeWithTag("voice_confirm_dialog").assertDoesNotExist()
    assertEquals("natural_female", selectedPreset)

    // Re-open and confirm applies selected preset
    composeTestRule.onNodeWithTag("voice_preset_deep_male").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("voice_confirm_apply_button").performClick()
    composeTestRule.onNodeWithTag("voice_confirm_dialog").assertDoesNotExist()
    assertEquals("deep_male", selectedPreset)
  }

  @Test
  fun `single dynamic task enables DND delay stepper and displays single checkmark on save button`() {
    val existingTask = RoutineTask(
      id = "single_dnd_delay_1",
      title = "Deep Reading",
      type = TaskType.DYNAMIC,
      startTimeMinute = 540,
      originalStartTimeMinute = 540,
      durationMinutes = 45,
      enableDnd = true,
      dndDelayMinutes = 12,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    var savedTask: RoutineTask? = null

    composeTestRule.setContent {
      MaterialTheme {
        TaskModalBottomSheet(
          initialTask = existingTask,
          onDismiss = {},
          onSaveTask = { savedTask = it },
          onSaveAnchorDetails = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
          onSaveSlot5 = { _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("anchor_dnd_delay_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("✓ Save & Apply Routine").assertIsDisplayed()
    composeTestRule.onNodeWithTag("save_task_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals(12, savedTask?.dndDelayMinutes)
  }

  @Test
  fun `slot 1 and slot 5 anchors unlock custom DND delay input when DND enabled`() {
    val slot5 = RoutineTask(
      id = "anchor_slot_5",
      title = "Break",
      type = TaskType.ANCHOR_ELASTIC,
      slotNumber = 5,
      startTimeMinute = 1015,
      durationMinutes = 65,
      enableDnd = true,
      dndDelayMinutes = 14
    )
    var savedSlot5Delay = -1

    composeTestRule.setContent {
      MaterialTheme {
        TaskModalBottomSheet(
          initialTask = slot5,
          onDismiss = {},
          onSaveTask = {},
          onSaveAnchorDetails = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
          onSaveSlot5 = { _, _, _, _, _ -> },
          onSaveSlot5Detailed = { _, _, _, _, delay, _, _, _, _, _, _ -> savedSlot5Delay = delay }
        )
      }
    }

    composeTestRule.onNodeWithTag("anchor_dnd_delay_input").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("save_task_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals(14, savedSlot5Delay)
  }

  @Test
  fun `timeline screen displays countdown pill, free gap indicator in earliest mode, and pending edits dialog`() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    val t1 = RoutineTask(
      id = "ui_gap_t1",
      title = "Morning Deep Work",
      type = TaskType.DYNAMIC,
      startTimeMinute = 420, // 07:00 AM - 08:00 AM (free gap 08:00..13:15 after)
      durationMinutes = 60,
      selectedDays = RoutineDay.allDays()
    )
    val result = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(t1),
      activeDay = RoutineDay.today()
    )
    val impact = listOf(
      com.example.engine.ScheduleImpactItem(
        taskId = "ui_gap_t1",
        title = "Morning Deep Work",
        oldStartMinute = 420,
        newStartMinute = 480,
        oldDurationMinutes = 60,
        newDurationMinutes = 60,
        statusChange = "Shifted 07:00 AM → 08:00 AM"
      )
    )

    var showPending by mutableStateOf(false)
    var committed = false

    composeTestRule.setContent {
      MaterialTheme {
        com.example.ui.screens.TimelineScreen(
          activeDay = RoutineDay.today(),
          onSelectDay = {},
          scheduleResult = result,
          sleepDurationMinutes = 360,
          currentMinute = 390, // 06:30 AM -> next task t1 at 07:00 AM => "30m"
          isDndGranted = true,
          activeAutomation = com.example.service.ActiveAutomationState(),
          sortMode = com.example.ui.TimelineSortMode.EARLIEST_START,
          deletedCount = 0,
          onSelectSortMode = {},
          onQuickAdjustAnchor = { _, _ -> },
          onEditTask = {},
          onMoveToTrash = {},
          onOpenTrashBin = {},
          onStopAutomation = {},
          onRefreshDndPermission = {},
          anchors = anchors,
          hasPendingEdits = showPending,
          pendingImpactDiff = impact,
          onCommitPendingEdits = { committed = true },
          onDiscardPendingEdits = { showPending = false }
        )
      }
    }

    // Normal mode: compact countdown pill & free gap indicator visible
    composeTestRule.onNodeWithTag("next_task_countdown_pill").assertIsDisplayed()
    composeTestRule.onNode(hasText("30m") and hasAnyAncestor(hasTestTag("next_task_countdown_pill"))).assertIsDisplayed()
    composeTestRule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasTestTag("free_gap_indicator_ui_gap_t1"))
    composeTestRule.onNodeWithTag("free_gap_indicator_ui_gap_t1").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pending_edits_header_controls").assertDoesNotExist()

    // Pending mode: header swaps to 3-button History / Apply / Discard controls
    showPending = true
    composeTestRule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToIndex(0)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("next_task_countdown_pill").assertDoesNotExist()
    composeTestRule.onNodeWithTag("pending_edits_header_controls").assertIsDisplayed()

    composeTestRule.onNodeWithTag("pending_impact_history_button").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("schedule_impact_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("impact_dialog_apply_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(true, committed)
  }

  @Test
  fun `timeline screen hides free gap badge when covered by inter-task buffer and wraps late-night header countdown`() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    // t1 at 06:00-07:00 (360..420) with 15m buffer (effective end 435); t2 starts at 07:15 (435) -> net gap == 0 (NO badge!)
    // After t2 (07:15-07:45, 435..465, 0m buffer), Dhuhr starts at 13:15 (795) -> net gap == 330m ("5h 30m")!
    val t1 = RoutineTask(
      id = "buf_t1",
      title = "Morning Warmup",
      type = TaskType.DYNAMIC,
      startTimeMinute = 360,
      durationMinutes = 60,
      interTaskBufferMinutes = 15,
      selectedDays = RoutineDay.allDays()
    )
    val t2 = RoutineTask(
      id = "buf_t2",
      title = "Deep Focus",
      type = TaskType.DYNAMIC,
      startTimeMinute = 435,
      durationMinutes = 30,
      interTaskBufferMinutes = 0,
      selectedDays = RoutineDay.allDays()
    )
    val today = RoutineDay.today()
    val result = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(t1, t2),
      activeDay = today,
      interTaskBufferMinutes = 0
    )

    composeTestRule.setContent {
      MaterialTheme {
        com.example.ui.screens.TimelineScreen(
          activeDay = today,
          onSelectDay = {},
          scheduleResult = result,
          sleepDurationMinutes = 360,
          // Late at night: 23:30 (1410m). Earliest scheduled task tomorrow is Fajr at 05:00 AM (300m) -> 300 + 1440 - 1410 = 330m = "5h 30m"
          currentMinute = 1410,
          isDndGranted = true,
          activeAutomation = com.example.service.ActiveAutomationState(),
          sortMode = com.example.ui.TimelineSortMode.EARLIEST_START,
          deletedCount = 0,
          onSelectSortMode = {},
          onQuickAdjustAnchor = { _, _ -> },
          onEditTask = {},
          onMoveToTrash = {},
          onOpenTrashBin = {},
          onStopAutomation = {},
          onRefreshDndPermission = {},
          anchors = anchors
        )
      }
    }

    // Header countdown must wrap around to tomorrow's earliest task (Fajr at 05:00 -> 5h 30m)
    composeTestRule.onNodeWithTag("next_task_countdown_pill").assertIsDisplayed()
    composeTestRule.onNode(hasText("5h 30m") and hasAnyAncestor(hasTestTag("next_task_countdown_pill"))).assertIsDisplayed()

    // Scroll to buf_t2: free_gap_indicator_buf_t1 must NOT exist (15m distance is 100% covered by buf_t1's 15m buffer)
    composeTestRule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasTestTag("free_gap_indicator_buf_t2"))
    composeTestRule.onNodeWithTag("free_gap_indicator_buf_t2").assertIsDisplayed()
    composeTestRule.onNodeWithTag("free_gap_indicator_buf_t1").assertDoesNotExist()
  }

  @Test
  fun `timeline screen pins countdown and pending controls while scrolling and exposes compact and auto-fit buttons`() {
    val anchors = com.example.engine.SchedulerEngine.createDefaultAnchors(360)
    // Fill Gap 2-3 (335..795 = 460m) with a fenced 460m task so overflowTask goes into Overflow Pool
    val fullGap = RoutineTask(
      id = "ui_full_23",
      title = "Full Morning",
      type = TaskType.DYNAMIC,
      startTimeMinute = 335,
      durationMinutes = 460,
      isHighPriority = true,
      selectedDays = RoutineDay.allDays()
    )
    val overflowTask = RoutineTask(
      id = "ui_ov_task",
      title = "Overflow Candidate",
      type = TaskType.DYNAMIC,
      startTimeMinute = 360,
      durationMinutes = 30,
      isHighPriority = true,
      selectedDays = RoutineDay.allDays()
    )
    val result = com.example.engine.SchedulerEngine.scheduleDay(
      anchors = anchors,
      dynamicTasks = listOf(fullGap, overflowTask),
      activeDay = RoutineDay.today()
    )

    var compactInvokedCount = 0
    composeTestRule.setContent {
      MaterialTheme {
        com.example.ui.screens.TimelineScreen(
          activeDay = RoutineDay.today(),
          onSelectDay = {},
          scheduleResult = result,
          sleepDurationMinutes = 360,
          currentMinute = 310, // Before 05:35 -> countdown visible
          isDndGranted = true,
          activeAutomation = com.example.service.ActiveAutomationState(),
          sortMode = com.example.ui.TimelineSortMode.EARLIEST_START,
          deletedCount = 0,
          onSelectSortMode = {},
          onQuickAdjustAnchor = { _, _ -> },
          onEditTask = {},
          onMoveToTrash = {},
          onOpenTrashBin = {},
          onStopAutomation = {},
          onRefreshDndPermission = {},
          anchors = anchors,
          onCompactAndRescueSchedule = { onNoChanges ->
            compactInvokedCount++
            onNoChanges()
          }
        )
      }
    }

    // Compact button visible in 24-Hour Cycle Map header -> clicking invokes callback and shows optimal toast
    composeTestRule.onNodeWithTag("compact_schedule_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, compactInvokedCount)
    composeTestRule.onNodeWithTag("optimal_schedule_toast").assertIsDisplayed()

    // Scroll down to bottom Overflow Pool -> countdown pill remains pinned and visible, Auto-Fit button visible!
    composeTestRule.onAllNodes(hasScrollToNodeAction()).onFirst().performScrollToNode(hasTestTag("auto_fit_overflow_button"))
    composeTestRule.onNodeWithTag("auto_fit_overflow_button").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    assertEquals(2, compactInvokedCount)
    composeTestRule.onNodeWithTag("next_task_countdown_pill").assertIsDisplayed()
  }

  @Test
  fun `dynamic task card displays flexed duration and relocated badges when compressed or migrated`() {
    val flexedAndMovedTask = RoutineTask(
      id = "badge_t1",
      title = "Smart Migrated Sprint",
      type = TaskType.DYNAMIC,
      startTimeMinute = 1110, // 06:30 PM
      originalStartTimeMinute = 540, // 09:00 AM
      durationMinutes = 45,
      originalDurationMinutes = 60,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    composeTestRule.setContent {
      MaterialTheme {
        com.example.ui.components.DynamicTaskCard(
          task = flexedAndMovedTask,
          currentMinute = 300,
          isToday = false,
          onEditClick = {}
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("flexed_duration_badge_badge_t1").assertIsDisplayed()
    composeTestRule.onNodeWithText("⚡ 60m→45m").assertIsDisplayed()
    composeTestRule.onNodeWithTag("relocated_task_badge_badge_t1").assertIsDisplayed()
    composeTestRule.onNodeWithText("↪ ${RoutineTask.formatTime(540)}").assertIsDisplayed()
  }

  @Test
  fun `editing flexed task shows active flex hint, initializes birth duration, and preserves originalDurationMinutes on save`() {
    val birthTask = RoutineTask(
      id = "modal_flex_1",
      title = "Elastic Deep Work",
      type = TaskType.DYNAMIC,
      startTimeMinute = 540,
      originalStartTimeMinute = 540,
      durationMinutes = 60,
      originalDurationMinutes = 60,
      customFlexibilityMinutes = 15,
      toleranceMinutes = 15,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    var savedTask: RoutineTask? = null

    composeTestRule.setContent {
      MaterialTheme {
        TaskModalBottomSheet(
          initialTask = birthTask,
          onDismiss = {},
          onSaveTask = { savedTask = it },
          onSaveAnchorDetails = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
          onSaveSlot5 = { _, _, _, _, _ -> },
          activeFlexedMinutes = 45
        )
      }
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("modal_active_flex_hint").assertIsDisplayed()
    composeTestRule.onNodeWithText("⚡ Auto-flexed to 45m today (Birth duration: 60m preserved)").assertIsDisplayed()
    composeTestRule.onNode(hasText("1") and hasTestTag("duration_hours_input")).assertIsDisplayed()
    composeTestRule.onNode(hasText("0") and hasTestTag("duration_mins_input")).assertIsDisplayed()

    composeTestRule.onNodeWithTag("save_task_button").performClick()
    composeTestRule.waitForIdle()

    assertEquals(60, savedTask?.durationMinutes)
    assertEquals(60, savedTask?.originalDurationMinutes)
  }

  @Test
  fun `settings screen omits removed voice volume mode card and preserves persona and trash navigation`() {
    composeTestRule.setContent {
      MaterialTheme {
        SettingsScreen(
          bufferMinutes = 0,
          onUpdateBufferMinutes = {}
        )
      }
    }

    composeTestRule.onNodeWithTag("voice_persona_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_trash_bin_card").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("voice_volume_mode_card").assertDoesNotExist()
    composeTestRule.onNodeWithTag("voice_mode_per_task_option").assertDoesNotExist()
    composeTestRule.onNodeWithTag("voice_mode_same_all_option").assertDoesNotExist()
    composeTestRule.onNodeWithTag("open_sound_settings_button").assertDoesNotExist()
  }

  @Test
  fun `task modal always displays per-task voice volume slider in single mode and preserves voiceVolume`() {
    val singleTask = RoutineTask(
      id = "mode_single_1",
      title = "Focus Read",
      type = TaskType.DYNAMIC,
      startTimeMinute = 540,
      durationMinutes = 30,
      isVoiceEnabled = true,
      voiceVolume = 0.45f,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    var savedSingle: RoutineTask? = null

    composeTestRule.setContent {
      MaterialTheme {
        TaskModalBottomSheet(
          initialTask = singleTask,
          onDismiss = {},
          onSaveTask = { savedSingle = it },
          onSaveAnchorDetails = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
          onSaveSlot5 = { _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("test_voice_button_volume_slider").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("save_task_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(0.45f, savedSingle?.voiceVolume ?: -1f, 0.001f)
  }

  @Test
  fun `task modal always displays per-session and anchor voice volume sliders`() {
    val twin1 = RoutineTask(
      id = "mode_twin_p1",
      title = "Twin Part 1",
      type = TaskType.DYNAMIC,
      startTimeMinute = 540,
      durationMinutes = 30,
      isFluidTwin = true,
      fluidTwinId = "twin_mode_test",
      fluidTwinPart = 1,
      fluidTotalPoolMinutes = 60,
      isVoiceEnabled = true,
      voiceVolume = 0.5f,
      selectedDays = setOf(RoutineDay.MONDAY)
    )
    val twin2 = RoutineTask(
      id = "mode_twin_p2",
      title = "Twin Part 2",
      type = TaskType.DYNAMIC,
      startTimeMinute = 840,
      durationMinutes = 30,
      isFluidTwin = true,
      fluidTwinId = "twin_mode_test",
      fluidTwinPart = 2,
      fluidTotalPoolMinutes = 60,
      isVoiceEnabled = true,
      voiceVolume = 0.7f,
      selectedDays = setOf(RoutineDay.MONDAY)
    )

    composeTestRule.setContent {
      MaterialTheme {
        TaskModalBottomSheet(
          initialTask = twin1,
          allDynamicTasks = listOf(twin1, twin2),
          onDismiss = {},
          onSaveTask = {},
          onSaveAnchorDetails = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
          onSaveSlot5 = { _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("session_1_test_voice_button_volume_slider").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("session_2_test_voice_button_volume_slider").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `anchor modal always displays regular and friday voice volume sliders when enabled`() {
    val slot3Anchor = RoutineTask(
      id = "anchor_slot_3",
      title = "Dhuhr",
      type = TaskType.ANCHOR_STANDARD,
      slotNumber = 3,
      startTimeMinute = 795,
      durationMinutes = 35,
      isVoiceEnabled = true,
      voiceVolume = 0.65f,
      fridayTitle = "Jumu'ah",
      fridayStartTimeMinute = 750,
      fridayDurationMinutes = 120,
      fridayVoiceMessage = "জুমার সময় হয়েছে।",
      selectedDays = RoutineDay.allDays()
    )

    composeTestRule.setContent {
      MaterialTheme {
        TaskModalBottomSheet(
          initialTask = slot3Anchor,
          onDismiss = {},
          onSaveTask = {},
          onSaveAnchorDetails = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
          onSaveSlot5 = { _, _, _, _, _ -> }
        )
      }
    }

    composeTestRule.onNodeWithTag("test_voice_button_volume_slider").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithText("Friday Mode \uD83D\uDD4C").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("test_friday_voice_button_volume_slider").performScrollTo().assertIsDisplayed()
  }
}
