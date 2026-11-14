package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.TaskType
import com.example.ui.components.TaskModalBottomSheet
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
}
