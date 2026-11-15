package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
}
