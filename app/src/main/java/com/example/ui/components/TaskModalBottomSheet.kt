package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.TaskType
import com.example.service.AutomationManager
import com.example.ui.theme.AnchorGradientStart
import com.example.ui.theme.DangerRed
import com.example.ui.theme.ElasticGradientStart
import com.example.ui.theme.OneUiBorder
import com.example.ui.theme.OneUiSurface
import com.example.ui.theme.OneUiSurfaceElevated
import com.example.ui.theme.OneUiSurfaceHigh
import com.example.ui.theme.SleepGradientStart
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber
import java.util.Locale
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 13 Fixed Categories for Routine Studio Form.
 */
data class RoutineCategory(
  val id: String,
  val icon: String,
  val title: String,
  val defaultName: String
)

val ROUTINE_CATEGORIES = listOf(
  RoutineCategory("general", "⚡", "General", "Routine Task"),
  RoutineCategory("break", "☕", "Break", "Coffee Break"),
  RoutineCategory("commuting", "🚗", "Commuting", "Commute & Travel"),
  RoutineCategory("eating", "🍴", "Eating", "Meal & Nutrition"),
  RoutineCategory("family", "🏠", "Home & family", "Family & Home Care"),
  RoutineCategory("internet", "🖥️", "Internet", "Web & Research"),
  RoutineCategory("goals", "🎯", "Personal goals", "Personal Goal Sprint"),
  RoutineCategory("reading", "📖", "Reading", "Deep Reading"),
  RoutineCategory("relationship", "❤️", "Relationship", "Quality Connection"),
  RoutineCategory("sleep", "☁️", "Sleep", "Power Nap"),
  RoutineCategory("studying", "👓", "Studying", "Focused Study"),
  RoutineCategory("training", "🏃", "Training", "Workout & Training"),
  RoutineCategory("tv", "📺", "TV", "Screen & Entertainment"),
  RoutineCategory("work", "💼", "Work", "Productive Work Sprint")
)

/**
 * Sanitizes numeric string inputs preventing 0-prepending glitch (e.g. typing 5 stays 5, not 50 or 05).
 */
fun sanitizeNumberInput(input: String, maxDigits: Int = 2): Pair<String, Int> {
  val digits = input.filter { it.isDigit() }.take(maxDigits)
  val clean = if (digits.isEmpty()) "" else if (digits.all { it == '0' }) "0" else digits.trimStart('0')
  val parsed = clean.toIntOrNull() ?: 0
  return Pair(clean, parsed)
}

data class FluidTwinSaveRequest(
  val title: String,
  val totalPoolMinutes: Int,
  val part1Title: String,
  val part1StartMinute: Int,
  val part1Duration: Int,
  val part1VoiceMessage: String,
  val part2Title: String,
  val part2StartMinute: Int,
  val part2Duration: Int,
  val part2VoiceMessage: String,
  val enableDnd: Boolean,
  val selectedDays: Set<RoutineDay>,
  val isVoiceEnabled: Boolean,
  val customNotificationTitle: String,
  val customNotificationBody: String,
  val customRingtoneUri: String? = null,
  val customRingtoneTitle: String? = null,
  val isSoundAlertEnabled: Boolean = true,
  val existingTwinId: String? = null,
  val sourceSingleTaskId: String? = null,
  val interTaskBufferMinutes: Int = 0,
  val customFlexibilityMinutes: Int = 0,
  val part1IsVoiceEnabled: Boolean = isVoiceEnabled,
  val part2IsVoiceEnabled: Boolean = isVoiceEnabled,
  val part1VoiceVolume: Float = 1.0f,
  val part2VoiceVolume: Float = 1.0f,
  val part1SafetyFloorMinutes: Int = 0,
  val part2SafetyFloorMinutes: Int = 0,
  val enableHaptic: Boolean = true,
  val dndDelayMinutes: Int = 0,
  val category: String? = null,
  val part1OriginalStartMinute: Int? = null,
  val part2OriginalStartMinute: Int? = null
)

/**
 * PHASE 5A & 6A: High-end AMOLED Dark "Routine Studio Form" & Preserved Anchor Editor.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskModalBottomSheet(
  initialTask: RoutineTask? = null,
  onDismiss: () -> Unit,
  onSaveTask: (RoutineTask) -> Unit,
  onSaveFluidTwin: (FluidTwinSaveRequest) -> Unit = {},
  onSaveAnchorDetails: (
    slotNumber: Int,
    title: String,
    startMinute: Int,
    durationMinutes: Int,
    voiceMessage: String,
    enableDnd: Boolean,
    enableHaptic: Boolean,
    selectedDays: Set<RoutineDay>,
    isVoiceEnabled: Boolean,
    customNotificationTitle: String,
    customNotificationBody: String,
    customRingtoneUri: String?,
    customRingtoneTitle: String?,
    isSoundAlertEnabled: Boolean,
    fridayTitle: String?,
    fridayStartTimeMinute: Int?,
    fridayDurationMinutes: Int?,
    fridayVoiceMessage: String?,
    voiceVolume: Float,
    dndDelayMinutes: Int
  ) -> Unit,
  onSaveSlot5: (title: String, voiceMessage: String, selectedDays: Set<RoutineDay>, enableDnd: Boolean, dndDelayMinutes: Int) -> Unit,
  onMoveToTrash: (taskId: String) -> Unit = {},
  slot2StartMinute: Int = 300,
  allDynamicTasks: List<RoutineTask> = emptyList(),
  onAbsorbTwin: ((taskToDeleteId: String, partnerTaskId: String) -> Unit)? = null,
  onDeleteTwinBoth: ((twinId: String) -> Unit)? = null,
  activeVoicePresetId: String = "natural_female"
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true
  )

  val isEditing = initialTask != null
  val isAnchor = initialTask?.type == TaskType.ANCHOR_STANDARD ||
    initialTask?.type == TaskType.ANCHOR_SLEEP ||
    initialTask?.type == TaskType.ANCHOR_ELASTIC
  val slotNum = initialTask?.slotNumber
  val isSlot1 = slotNum == 1
  val isSlot3 = slotNum == 3
  val isSlot5 = slotNum == 5

  // Slot 3 Friday Dual Mode State (strictly decoupled from Regular Dhuhr)
  var activeSlot3Tab by remember(initialTask) { mutableIntStateOf(0) } // 0 = Regular Days, 1 = Friday Mode 🕌
  var fridayTitle by remember(initialTask) { mutableStateOf(initialTask?.fridayTitle ?: "Jumu'ah") }
  val initialFridayStart = initialTask?.fridayStartTimeMinute ?: 750 // 12:30 PM default (750m)
  var fridayStartHour by remember(initialTask) { mutableIntStateOf(initialFridayStart / 60) }
  var fridayStartMinute by remember(initialTask) { mutableIntStateOf(initialFridayStart % 60) }
  val initialFridayDuration = initialTask?.fridayDurationMinutes ?: 120 // 2h default
  var fridayDurationHours by remember(initialTask) { mutableIntStateOf(initialFridayDuration / 60) }
  var fridayDurationMins by remember(initialTask) { mutableIntStateOf(initialFridayDuration % 60) }
  var fridayVoiceMessage by remember(initialTask) { mutableStateOf(initialTask?.fridayVoiceMessage ?: "জুমার সময় হয়েছে।") }

  // Tone Preview Player State
  var isPlayingTonePreview by remember { mutableStateOf(false) }
  var activeRingtone by remember { mutableStateOf<Ringtone?>(null) }

  // 13-Category Selector State
  var selectedCategory by remember(initialTask) {
    val initialCat = ROUTINE_CATEGORIES.firstOrNull { it.id == initialTask?.category }
      ?: ROUTINE_CATEGORIES.firstOrNull { initialTask?.title?.contains(it.title, ignoreCase = true) == true }
      ?: ROUTINE_CATEGORIES[0]
    mutableStateOf(initialCat)
  }
  var showCategoryGridModal by remember { mutableStateOf(false) }

  // 2-Session Mode Switcher State
  val isEditingFluidTwin = initialTask?.isFluidTwin == true
  var isFluidTwinMode by remember(initialTask) { mutableStateOf(isEditingFluidTwin) }

  // Fluid Twin Partner Discovery & Initialization (strictly exclude deleted tasks)
  val existingTwinPartner = remember(initialTask, allDynamicTasks) {
    if (initialTask?.isFluidTwin == true && initialTask.fluidTwinId != null) {
      allDynamicTasks.firstOrNull { !it.isDeleted && it.fluidTwinId == initialTask.fluidTwinId && it.id != initialTask.id }
    } else null
  }

  val resolvedP1Task = if (initialTask?.isFluidTwin == true) {
    if (initialTask.fluidTwinPart == 1) initialTask else existingTwinPartner
  } else null

  val resolvedP2Task = if (initialTask?.isFluidTwin == true) {
    if (initialTask.fluidTwinPart == 2) initialTask else existingTwinPartner
  } else null

  // Total Routine Duration Controller (for Dual Fluid Twin Mode)
  val initialTotalPool = if (isEditingFluidTwin) {
    initialTask?.fluidTotalPoolMinutes
      ?: ((resolvedP1Task?.durationMinutes ?: 0) + (resolvedP2Task?.durationMinutes ?: 0)).coerceAtLeast(2)
  } else {
    0
  }
  var totalPoolHours by remember(initialTask) { mutableIntStateOf(initialTotalPool / 60) }
  var totalPoolMinutes by remember(initialTask) { mutableIntStateOf(initialTotalPool % 60) }
  var totalPoolHoursInput by remember(initialTask) {
    mutableStateOf(if (isEditingFluidTwin && initialTotalPool > 0) (initialTotalPool / 60).toString() else "")
  }
  var totalPoolMinutesInput by remember(initialTask) {
    mutableStateOf(if (isEditingFluidTwin && initialTotalPool > 0) (initialTotalPool % 60).toString() else "")
  }
  var splitValidationError by remember(initialTask) { mutableStateOf<String?>(null) }

  // Single Task / Regular Anchor State (strictly reads initialTask?.startTimeMinute & durationMinutes)
  val initialRegularStart = initialTask?.startTimeMinute ?: 540
  val birthStartMinute = remember(initialTask) {
    initialTask?.originalStartTimeMinute ?: initialTask?.startTimeMinute ?: 540
  }
  var hasUserEditedStartTime by remember(initialTask) { mutableStateOf(false) }
  val initialRegularDuration = initialTask?.durationMinutes ?: 0
  var title by remember(initialTask) { mutableStateOf(initialTask?.title ?: "") }
  var startHour by remember(initialTask) { mutableIntStateOf(initialRegularStart / 60) }
  var startMinute by remember(initialTask) { mutableIntStateOf(initialRegularStart % 60) }
  var durationHours by remember(initialTask) { mutableIntStateOf(initialRegularDuration / 60) }
  var durationMins by remember(initialTask) { mutableIntStateOf(initialRegularDuration % 60) }
  var durationHoursInput by remember(initialTask) {
    mutableStateOf(if (initialTask == null) "" else (initialRegularDuration / 60).toString())
  }
  var durationMinsInput by remember(initialTask) {
    mutableStateOf(if (initialTask == null) "" else (initialRegularDuration % 60).toString())
  }
  var toleranceMinutes by remember(initialTask) { mutableIntStateOf(initialTask?.toleranceMinutes ?: 0) }
  var customFlexibilityMinutes by remember(initialTask) { mutableIntStateOf(initialTask?.customFlexibilityMinutes ?: 0) }
  var customFlexInput by remember(initialTask) { mutableStateOf((initialTask?.customFlexibilityMinutes ?: 0).toString()) }
  var interTaskBufferMinutes by remember(initialTask) {
    mutableIntStateOf(initialTask?.interTaskBufferMinutes ?: 0)
  }
  var bufferInput by remember(initialTask) {
    mutableStateOf((initialTask?.interTaskBufferMinutes ?: 0).toString())
  }

  val decrementBuffer = {
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    val current = interTaskBufferMinutes
    val nextVal = when {
      current > 5 -> ((current - 1) / 5) * 5 // 30->25, 25->20, 20->15, 15->10, 10->5
      current in 1..5 -> current - 1          // 5->4, 4->3, 3->2, 2->1, 1->0
      else -> 0
    }.coerceIn(0, 30)
    interTaskBufferMinutes = nextVal
    bufferInput = nextVal.toString()
  }

  val incrementBuffer = {
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    val current = interTaskBufferMinutes
    val nextVal = when {
      current < 5 -> current + 1                // 0->1, 1->2, 2->3, 3->4, 4->5
      current in 5..29 -> ((current / 5) + 1) * 5 // 5->10, 10->15, 15->20, 20->25, 25->30
      else -> 30
    }.coerceIn(0, 30)
    interTaskBufferMinutes = nextVal
    bufferInput = nextVal.toString()
  }

  val decrementFlexibility = {
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    val current = customFlexibilityMinutes
    val nextVal = if (current > 0) ((current - 1) / 5) * 5 else 0
    customFlexibilityMinutes = nextVal.coerceIn(0, 30)
    customFlexInput = customFlexibilityMinutes.toString()
  }

  val incrementFlexibility = {
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    val current = customFlexibilityMinutes
    val nextVal = if (current < 30) ((current / 5) + 1) * 5 else 30
    customFlexibilityMinutes = nextVal.coerceIn(0, 30)
    customFlexInput = customFlexibilityMinutes.toString()
  }

  var isHighPriority by remember(initialTask) { mutableStateOf(initialTask?.isHighPriority ?: false) }
  var isVoiceEnabled by remember(initialTask) { mutableStateOf(initialTask?.isVoiceEnabled ?: true) }
  var voiceMessage by remember(initialTask) { mutableStateOf(initialTask?.voiceMessage ?: "") }
  var voiceVolume by remember(initialTask) { mutableFloatStateOf(initialTask?.voiceVolume ?: 1f) }

  val p1BirthStart = remember(initialTask, resolvedP1Task) {
    resolvedP1Task?.originalStartTimeMinute ?: resolvedP1Task?.startTimeMinute ?: initialRegularStart
  }
  val p2BirthStart = remember(initialTask, resolvedP2Task) {
    resolvedP2Task?.originalStartTimeMinute ?: resolvedP2Task?.startTimeMinute ?: (14 * 60)
  }
  var hasUserEditedP1Start by remember(initialTask) { mutableStateOf(false) }
  var hasUserEditedP2Start by remember(initialTask) { mutableStateOf(false) }

  // Dual Session 1 (Cyan Theme #38BDF8) - starts at 0/empty when creating or converting single task
  var part1Title by remember(initialTask) {
    mutableStateOf(if (isEditingFluidTwin) (resolvedP1Task?.title ?: initialTask?.title.orEmpty()) else "")
  }
  var part1StartHour by remember(initialTask) {
    mutableIntStateOf(if (isEditingFluidTwin) (resolvedP1Task?.let { it.startTimeMinute / 60 } ?: startHour) else startHour)
  }
  var part1StartMinute by remember(initialTask) {
    mutableIntStateOf(if (isEditingFluidTwin) (resolvedP1Task?.let { it.startTimeMinute % 60 } ?: startMinute) else startMinute)
  }
  var part1DurationHours by remember(initialTask) {
    mutableIntStateOf(if (isEditingFluidTwin) ((resolvedP1Task?.durationMinutes ?: 0) / 60) else 0)
  }
  var part1DurationMins by remember(initialTask) {
    mutableIntStateOf(if (isEditingFluidTwin) ((resolvedP1Task?.durationMinutes ?: 0) % 60) else 0)
  }
  var part1SafetyFloor by remember(initialTask) {
    mutableIntStateOf(if (isEditingFluidTwin) (resolvedP1Task?.fluidSafetyFloorMinutes ?: 0) else 0)
  }
  var part1VoiceMessage by remember(initialTask) {
    mutableStateOf(if (isEditingFluidTwin) (resolvedP1Task?.voiceMessage ?: "") else "")
  }
  var isVoiceEnabledS1 by remember(initialTask) {
    mutableStateOf(if (isEditingFluidTwin) (resolvedP1Task?.isVoiceEnabled ?: true) else true)
  }
  var voiceVolumeS1 by remember(initialTask) {
    mutableFloatStateOf(if (isEditingFluidTwin) (resolvedP1Task?.voiceVolume ?: 1f) else 1f)
  }

  // Dual Session 2 (Violet Theme #8B5CF6) - starts at 0/empty when creating or converting single task
  var part2Title by remember(initialTask) {
    mutableStateOf(if (isEditingFluidTwin) (resolvedP2Task?.title ?: "") else "")
  }
  var part2StartHour by remember(initialTask) {
    mutableIntStateOf(if (isEditingFluidTwin) (resolvedP2Task?.let { it.startTimeMinute / 60 } ?: 14) else 14)
  }
  var part2StartMinute by remember(initialTask) {
    mutableIntStateOf(if (isEditingFluidTwin) (resolvedP2Task?.let { it.startTimeMinute % 60 } ?: 0) else 0)
  }
  var part2DurationHours by remember(initialTask) {
    mutableIntStateOf(if (isEditingFluidTwin) ((resolvedP2Task?.durationMinutes ?: 0) / 60) else 0)
  }
  var part2DurationMins by remember(initialTask) {
    mutableIntStateOf(if (isEditingFluidTwin) ((resolvedP2Task?.durationMinutes ?: 0) % 60) else 0)
  }
  var part2SafetyFloor by remember(initialTask) {
    mutableIntStateOf(if (isEditingFluidTwin) (resolvedP2Task?.fluidSafetyFloorMinutes ?: 0) else 0)
  }
  var part2VoiceMessage by remember(initialTask) {
    mutableStateOf(if (isEditingFluidTwin) (resolvedP2Task?.voiceMessage ?: "") else "")
  }
  var isVoiceEnabledS2 by remember(initialTask) {
    mutableStateOf(if (isEditingFluidTwin) (resolvedP2Task?.isVoiceEnabled ?: true) else true)
  }
  var voiceVolumeS2 by remember(initialTask) {
    mutableFloatStateOf(if (isEditingFluidTwin) (resolvedP2Task?.voiceVolume ?: 1f) else 1f)
  }

  // System Integrations & Recurrence
  var isSoundAlertEnabled by remember(initialTask) { mutableStateOf(initialTask?.isSoundAlertEnabled ?: true) }
  var customRingtoneUri by remember(initialTask) { mutableStateOf(initialTask?.customRingtoneUri) }
  var customRingtoneTitle by remember(initialTask) { mutableStateOf(initialTask?.customRingtoneTitle ?: "Default Notification Tone") }
  var enableDnd by remember(initialTask) { mutableStateOf(initialTask?.enableDnd ?: false) }
  var dndDelayMinutes by remember(initialTask) { mutableIntStateOf(initialTask?.dndDelayMinutes ?: 0) }
  var enableHaptic by remember(initialTask) { mutableStateOf(initialTask?.enableHaptic ?: true) }
  var selectedDays by remember(initialTask) { mutableStateOf(initialTask?.selectedDays ?: RoutineDay.allDays()) }
  var isDailyRecurrence by remember(initialTask) { mutableStateOf((initialTask?.selectedDays ?: RoutineDay.allDays()).size == 7) }

  // Custom Circular Analog Clock Dialog State
  var showAnalogClockModal by remember { mutableStateOf(false) }
  var clockTarget by remember { mutableIntStateOf(0) } // 0 = Single / Anchor, 1 = S1, 2 = S2

  // Delete Confirmation Dialog state
  var showDeleteConfirmDialog by remember { mutableStateOf(false) }

  // Ringtone Picker Activity Launcher
  val ringtonePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val uri: Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
      } else {
        @Suppress("DEPRECATION")
        result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
      }
      if (uri != null) {
        customRingtoneUri = uri.toString()
        val r = RingtoneManager.getRingtone(context, uri)
        customRingtoneTitle = r?.getTitle(context) ?: "Selected Ringtone"
      } else {
        customRingtoneUri = null
        customRingtoneTitle = "Silent / None"
      }
    }
  }

  // TTS Engine Setup
  var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }
  var isTtsReady by remember { mutableStateOf(false) }
  var isBengaliVoiceReady by remember { mutableStateOf(true) }
  var isSpeakingVoicePreview by remember { mutableStateOf(false) }
  var activeVoiceUtteranceId by remember { mutableStateOf<String?>(null) }

  DisposableEffect(context) {
    var currentTts: TextToSpeech? = null
    var triedGoogleTtsFallback = false
    var disposed = false

    val progressListener = object : UtteranceProgressListener() {
      override fun onStart(utteranceId: String?) {
        isSpeakingVoicePreview = true
        activeVoiceUtteranceId = utteranceId
      }

      override fun onDone(utteranceId: String?) {
        if (activeVoiceUtteranceId == null || activeVoiceUtteranceId == utteranceId) {
          isSpeakingVoicePreview = false
          activeVoiceUtteranceId = null
        }
      }

      @Deprecated("Deprecated in Java")
      override fun onError(utteranceId: String?) {
        isSpeakingVoicePreview = false
        activeVoiceUtteranceId = null
      }

      override fun onError(utteranceId: String?, errorCode: Int) {
        isSpeakingVoicePreview = false
        activeVoiceUtteranceId = null
      }
    }

    fun createTts(enginePackage: String? = null) {
      var created: TextToSpeech? = null
      val listener = TextToSpeech.OnInitListener { status ->
        if (disposed) {
          try {
            created?.shutdown()
          } catch (_: Exception) {}
          return@OnInitListener
        }
        val active = created ?: currentTts
        if (status == TextToSpeech.SUCCESS && active != null) {
          val bnAvailable = AutomationManager.isBengaliTtsAvailable(active)
          if (!bnAvailable && !triedGoogleTtsFallback && AutomationManager.hasGoogleTtsEngine(context)) {
            triedGoogleTtsFallback = true
            try {
              active.shutdown()
            } catch (_: Exception) {}
            createTts("com.google.android.tts")
            return@OnInitListener
          }
          AutomationManager.applyTtsLanguage(active, "", activeVoicePresetId)
          active.setOnUtteranceProgressListener(progressListener)
          currentTts = active
          ttsInstance = active
          isBengaliVoiceReady = bnAvailable
          isTtsReady = true
        } else if (!triedGoogleTtsFallback && AutomationManager.hasGoogleTtsEngine(context)) {
          triedGoogleTtsFallback = true
          try {
            active?.shutdown()
          } catch (_: Exception) {}
          createTts("com.google.android.tts")
        } else {
          isBengaliVoiceReady = false
          isTtsReady = false
        }
      }
      created = try {
        if (enginePackage != null) {
          TextToSpeech(context, listener, enginePackage)
        } else {
          TextToSpeech(context, listener)
        }
      } catch (_: Exception) {
        null
      }
      if (created != null) {
        created.setOnUtteranceProgressListener(progressListener)
        currentTts = created
        ttsInstance = created
      }
    }

    createTts(null)
    onDispose {
      disposed = true
      isSpeakingVoicePreview = false
      activeVoiceUtteranceId = null
      try {
        currentTts?.stop()
        currentTts?.shutdown()
      } catch (_: Exception) {}
      try {
        activeRingtone?.stop()
      } catch (_: Exception) {}
    }
  }

  val toggleTonePreview: () -> Unit = {
    if (isPlayingTonePreview) {
      try {
        activeRingtone?.stop()
      } catch (_: Exception) {}
      isPlayingTonePreview = false
      activeRingtone = null
    } else {
      try {
        val uri = customRingtoneUri?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val r = RingtoneManager.getRingtone(context, uri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          r?.isLooping = false
          r?.volume = voiceVolume.coerceIn(0f, 1f)
        }
        r?.play()
        activeRingtone = r
        isPlayingTonePreview = true
      } catch (_: Exception) {
        isPlayingTonePreview = false
      }
    }
  }

  // Reliable Initial Snapshot Tracking
  val initialTitle = remember(initialTask) { initialTask?.title ?: "" }
  val initialDuration = remember(initialTask) { initialTask?.durationMinutes ?: 0 }
  val initialStartMin = remember(initialTask) { initialTask?.startTimeMinute ?: 540 }
  val initialVoice = remember(initialTask) { initialTask?.voiceMessage ?: "" }
  val initialDnd = remember(initialTask) { initialTask?.enableDnd ?: false }
  val initialSound = remember(initialTask) { initialTask?.isSoundAlertEnabled ?: true }

  // Unsaved Changes Dismissal Guard
  var showUnsavedChangesDialog by remember { mutableStateOf(false) }

  val hasUnsavedChanges by remember {
    derivedStateOf {
      if (initialTask == null) {
        if (isFluidTwinMode) {
          part1Title.isNotBlank() ||
            part2Title.isNotBlank() ||
            (totalPoolHours * 60 + totalPoolMinutes) > 0 ||
            (part1DurationHours * 60 + part1DurationMins) > 0 ||
            (part2DurationHours * 60 + part2DurationMins) > 0 ||
            part1SafetyFloor > 0 ||
            part2SafetyFloor > 0 ||
            part1VoiceMessage.isNotBlank() ||
            part2VoiceMessage.isNotBlank() ||
            enableDnd ||
            !isSoundAlertEnabled ||
            customFlexibilityMinutes > 0 ||
            interTaskBufferMinutes > 0
        } else {
          title.isNotBlank() ||
            voiceMessage.isNotBlank() ||
            (durationHours * 60 + durationMins) > 0 ||
            enableDnd ||
            !isSoundAlertEnabled ||
            isHighPriority ||
            customFlexibilityMinutes > 0 ||
            interTaskBufferMinutes > 0
        }
      } else if (isEditingFluidTwin) {
        val initPool = initialTask.fluidTotalPoolMinutes
          ?: ((resolvedP1Task?.durationMinutes ?: 0) + (resolvedP2Task?.durationMinutes ?: 0))
        val initP1Dur = resolvedP1Task?.durationMinutes ?: 0
        val initP2Dur = resolvedP2Task?.durationMinutes ?: 0
        val initP1Start = resolvedP1Task?.startTimeMinute ?: 0
        val initP2Start = resolvedP2Task?.startTimeMinute ?: (14 * 60)
        (totalPoolHours * 60 + totalPoolMinutes) != initPool ||
          part1Title != (resolvedP1Task?.title ?: "") ||
          part2Title != (resolvedP2Task?.title ?: "") ||
          (part1StartHour * 60 + part1StartMinute) != initP1Start ||
          (part2StartHour * 60 + part2StartMinute) != initP2Start ||
          (part1DurationHours * 60 + part1DurationMins) != initP1Dur ||
          (part2DurationHours * 60 + part2DurationMins) != initP2Dur ||
          part1SafetyFloor != (resolvedP1Task?.fluidSafetyFloorMinutes ?: 0) ||
          part2SafetyFloor != (resolvedP2Task?.fluidSafetyFloorMinutes ?: 0) ||
          part1VoiceMessage != (resolvedP1Task?.voiceMessage ?: "") ||
          part2VoiceMessage != (resolvedP2Task?.voiceMessage ?: "") ||
          isVoiceEnabledS1 != (resolvedP1Task?.isVoiceEnabled ?: true) ||
          isVoiceEnabledS2 != (resolvedP2Task?.isVoiceEnabled ?: true) ||
          voiceVolumeS1 != (resolvedP1Task?.voiceVolume ?: 1f) ||
          voiceVolumeS2 != (resolvedP2Task?.voiceVolume ?: 1f) ||
          enableDnd != initialDnd ||
          isSoundAlertEnabled != initialSound ||
          (initialTask.category != null && selectedCategory.id != initialTask.category) ||
          selectedDays != initialTask.selectedDays ||
          customFlexibilityMinutes != initialTask.customFlexibilityMinutes ||
          interTaskBufferMinutes != initialTask.interTaskBufferMinutes
      } else {
        title != initialTitle ||
          (durationHours * 60 + durationMins) != initialDuration ||
          (startHour * 60 + startMinute) != initialStartMin ||
          voiceMessage != initialVoice ||
          enableDnd != initialDnd ||
          isSoundAlertEnabled != initialSound ||
          isFluidTwinMode != initialTask.isFluidTwin ||
          (initialTask.category != null && selectedCategory.id != initialTask.category) ||
          selectedDays != initialTask.selectedDays ||
          voiceVolume != initialTask.voiceVolume ||
          isHighPriority != initialTask.isHighPriority ||
          customFlexibilityMinutes != initialTask.customFlexibilityMinutes ||
          interTaskBufferMinutes != initialTask.interTaskBufferMinutes ||
          (isSlot3 && (
            fridayTitle != (initialTask.fridayTitle ?: "Jumu'ah") ||
            (fridayDurationHours * 60 + fridayDurationMins) != (initialTask.fridayDurationMinutes ?: 120) ||
            (fridayStartHour * 60 + fridayStartMinute) != (initialTask.fridayStartTimeMinute ?: (12 * 60 + 30)) ||
            fridayVoiceMessage != (initialTask.fridayVoiceMessage ?: "জুমার সময় হয়েছে।")
          ))
      }
    }
  }

  val handleDismissRequest: () -> Unit = {
    if (hasUnsavedChanges) {
      showUnsavedChangesDialog = true
    } else {
      onDismiss()
    }
  }

  BackHandler {
    handleDismissRequest()
  }

  val triggerSave: () -> Unit = {
    var canDismiss = true
    if (isAnchor) {
      val calculatedDuration = (durationHours * 60 + durationMins).coerceAtLeast(15)
      val calculatedStart = startHour * 60 + startMinute
      val calculatedFriDuration = (fridayDurationHours * 60 + fridayDurationMins).coerceAtLeast(15)
      val calculatedFriStart = fridayStartHour * 60 + fridayStartMinute
      if (isSlot5) {
        onSaveSlot5(title.ifBlank { "Elastic Rubber Buffer" }, voiceMessage, selectedDays, enableDnd, 0)
      } else if (slotNum != null) {
        if (isSlot1) {
          val finalSleepDuration = calculatedDuration.coerceIn(240, 720) // 4h to 12h
          val calculatedBedtime = ((slot2StartMinute - finalSleepDuration) % 1440 + 1440) % 1440
          onSaveAnchorDetails(
            1,
            title.ifBlank { "Reverse Sleep Recovery" },
            calculatedBedtime,
            finalSleepDuration,
            voiceMessage,
            enableDnd,
            enableHaptic,
            selectedDays,
            isVoiceEnabled,
            title.ifBlank { "Reverse Sleep Recovery" },
            voiceMessage,
            customRingtoneUri,
            customRingtoneTitle,
            isSoundAlertEnabled,
            null,
            null,
            null,
            null,
            voiceVolume,
            0
          )
        } else {
          onSaveAnchorDetails(
            slotNum,
            title.ifBlank { "Anchor Slot $slotNum" },
            calculatedStart,
            calculatedDuration,
            voiceMessage,
            enableDnd,
            enableHaptic,
            selectedDays,
            isVoiceEnabled,
            title,
            voiceMessage,
            customRingtoneUri,
            customRingtoneTitle,
            isSoundAlertEnabled,
            if (isSlot3) fridayTitle.ifBlank { null } else null,
            if (isSlot3) calculatedFriStart else null,
            if (isSlot3) calculatedFriDuration else null,
            if (isSlot3) fridayVoiceMessage.ifBlank { null } else null,
            voiceVolume,
            if (isSlot1 || isSlot5) 0 else dndDelayMinutes.coerceIn(0, 25)
          )
        }
      }
    } else if (isFluidTwinMode) {
      val pool = totalPoolHours * 60 + totalPoolMinutes
      val p1Dur = part1DurationHours * 60 + part1DurationMins
      val p2Dur = part2DurationHours * 60 + part2DurationMins
      val validationError = when {
        pool < 2 -> "Total routine pool must be at least 2 minutes."
        p1Dur < 1 || p2Dur < 1 -> "Both Session 1 and Session 2 must be at least 1 minute."
        part1SafetyFloor > p1Dur -> "Session 1 safety floor (${part1SafetyFloor}m) cannot exceed Session 1 duration (${p1Dur}m)."
        part2SafetyFloor > p2Dur -> "Session 2 safety floor (${part2SafetyFloor}m) cannot exceed Session 2 duration (${p2Dur}m)."
        else -> null
      }
      if (validationError != null) {
        splitValidationError = validationError
        canDismiss = false
      } else {
        splitValidationError = null
        val totalPool = pool.coerceAtLeast(p1Dur + p2Dur)
        val p1Start = part1StartHour * 60 + part1StartMinute
        val p2Start = part2StartHour * 60 + part2StartMinute
        val finalP1Start = if (isEditingFluidTwin && !hasUserEditedP1Start) p1BirthStart else p1Start
        val finalP2Start = if (isEditingFluidTwin && !hasUserEditedP2Start) p2BirthStart else p2Start
        val masterTitle = selectedCategory.title

        onSaveFluidTwin(
          FluidTwinSaveRequest(
            title = masterTitle,
            totalPoolMinutes = totalPool,
            part1Title = part1Title.ifBlank { "${selectedCategory.title} (Part 1)" },
            part1StartMinute = finalP1Start,
            part1Duration = p1Dur,
            part1VoiceMessage = part1VoiceMessage,
            part2Title = part2Title.ifBlank { "${selectedCategory.title} (Part 2)" },
            part2StartMinute = finalP2Start,
            part2Duration = p2Dur,
            part2VoiceMessage = part2VoiceMessage,
            enableDnd = enableDnd,
            selectedDays = selectedDays,
            isVoiceEnabled = isVoiceEnabledS1 || isVoiceEnabledS2,
            customNotificationTitle = masterTitle,
            customNotificationBody = "",
            customRingtoneUri = customRingtoneUri,
            customRingtoneTitle = customRingtoneTitle,
            isSoundAlertEnabled = isSoundAlertEnabled,
            existingTwinId = initialTask?.fluidTwinId,
            sourceSingleTaskId = if (initialTask != null && !initialTask.isFluidTwin) initialTask.id else null,
            interTaskBufferMinutes = interTaskBufferMinutes.coerceIn(0, 30),
            customFlexibilityMinutes = customFlexibilityMinutes.coerceIn(0, 30),
            part1IsVoiceEnabled = isVoiceEnabledS1,
            part2IsVoiceEnabled = isVoiceEnabledS2,
            part1VoiceVolume = voiceVolumeS1.coerceIn(0f, 1f),
            part2VoiceVolume = voiceVolumeS2.coerceIn(0f, 1f),
            part1SafetyFloorMinutes = part1SafetyFloor.coerceIn(0, p1Dur),
            part2SafetyFloorMinutes = part2SafetyFloor.coerceIn(0, p2Dur),
            enableHaptic = enableHaptic,
            dndDelayMinutes = dndDelayMinutes.coerceIn(0, 25),
            category = selectedCategory.id,
            part1OriginalStartMinute = finalP1Start,
            part2OriginalStartMinute = finalP2Start
          )
        )
      }
    } else {
      val calculatedDuration = (durationHours * 60 + durationMins).coerceAtLeast(0)
      val calculatedStart = startHour * 60 + startMinute
      val finalStart = if (isEditing && !hasUserEditedStartTime) birthStartMinute else calculatedStart
      val actualTitle = title.ifBlank { "${selectedCategory.icon} ${selectedCategory.defaultName}" }

      val clampedBuffer = interTaskBufferMinutes.coerceIn(0, 30)
      val clampedFlex = customFlexibilityMinutes.coerceIn(0, 30)

      val task = RoutineTask(
        id = initialTask?.id ?: "dynamic_${UUID.randomUUID().toString().take(8)}",
        title = actualTitle,
        type = TaskType.DYNAMIC,
        startTimeMinute = finalStart,
        originalStartTimeMinute = finalStart,
        durationMinutes = calculatedDuration,
        voiceMessage = voiceMessage,
        enableDnd = enableDnd,
        enableHaptic = enableHaptic,
        allowSplitting = false,
        isHighPriority = isHighPriority,
        customFlexibilityMinutes = clampedFlex,
        selectedDays = selectedDays,
        isVoiceEnabled = isVoiceEnabled,
        voiceVolume = voiceVolume.coerceIn(0f, 1f),
        customNotificationTitle = actualTitle,
        customNotificationBody = voiceMessage,
        customRingtoneUri = customRingtoneUri,
        customRingtoneTitle = customRingtoneTitle,
        isSoundAlertEnabled = isSoundAlertEnabled,
        toleranceMinutes = clampedFlex,
        interTaskBufferMinutes = clampedBuffer,
        category = selectedCategory.id
      )
      onSaveTask(task)
    }
    if (canDismiss) {
      onDismiss()
    }
  }

  // Consume all nested scroll dispatch so vertical scrolling inside the form never collapses or dismisses the sheet
  val consumeNestedScrollConnection = remember {
    object : NestedScrollConnection {
      override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
      override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
      override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero
      override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
    }
  }

  ModalBottomSheet(
    onDismissRequest = { handleDismissRequest() },
    sheetState = sheetState,
    sheetGesturesEnabled = false, // CRITICAL: Completely disables drag & swipe-to-dismiss gestures
    dragHandle = null,             // Eliminates the gesture-consuming default drag handle
    containerColor = Color(0xFF080A0F), // True AMOLED Deep Dark
    contentColor = TextPrimary,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      @OptIn(ExperimentalFoundationApi::class)
      CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .nestedScroll(consumeNestedScrollConnection)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .padding(bottom = 112.dp) // Room for sticky bottom bar
        ) {
          // Plain, static, non-draggable visual indicator pill
          Box(
            modifier = Modifier
              .padding(vertical = 12.dp)
              .size(width = 44.dp, height = 4.dp)
              .clip(RoundedCornerShape(2.dp))
              .background(Color.White.copy(alpha = 0.25f))
              .align(Alignment.CenterHorizontally)
          )
        // ==============================================================
        // 1. TOP BAR
        // ==============================================================
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = handleDismissRequest,
              modifier = Modifier.size(38.dp).testTag("dismiss_modal_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = when {
                  isSlot1 -> "Slot 1: Reverse Sleep Settings"
                  isSlot5 -> "Slot 5: Elastic Rubber Buffer"
                  isAnchor -> "Anchor Slot $slotNum"
                  isEditing -> "Edit Routine Task"
                  else -> "New Routine Task"
                },
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "GALAXY ONE UI ENGINE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                color = Color(0xFF94A3B8)
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          if (isAnchor) {
            // Subtle Glowing Badge for Anchors
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF3B82F6).copy(alpha = 0.15f))
                .border(BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "ANCHOR",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFA5B4FC)
              )
            }
          } else {
            // Header Pill Controls (Mutually Exclusive: Priority vs Split; Split locked when editing an existing Fluid Twin)
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // 1. Priority Pill Button
              AnimatedVisibility(visible = !isFluidTwinMode) {
                Surface(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                      BorderStroke(
                        1.dp,
                        if (isHighPriority) Color(0xFFF59E0B) else Color(0xFF334155)
                      ),
                      RoundedCornerShape(8.dp)
                    )
                    .clickable {
                      val next = !isHighPriority
                      isHighPriority = next
                      if (next) isFluidTwinMode = false
                    }
                    .testTag("header_priority_pill"),
                  color = if (isHighPriority) Color(0xFFF59E0B).copy(alpha = 0.22f) else Color(0xFF1E293B)
                ) {
                  Text(
                    text = "Priority",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isHighPriority) Color(0xFFFDE68A) else Color(0xFF94A3B8),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }

              // 2. Split Pill (Locked static badge when editing an existing Fluid Twin)
              if (isEditingFluidTwin) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.22f))
                    .border(BorderStroke(1.dp, Color(0xFF8B5CF6)), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = "TWIN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFDDD6FE)
                  )
                }
              } else {
                AnimatedVisibility(visible = !isEditing && !isHighPriority) {
                  Surface(
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .border(
                        BorderStroke(
                          1.dp,
                          if (isFluidTwinMode) Color(0xFF8B5CF6) else Color(0xFF334155)
                        ),
                        RoundedCornerShape(8.dp)
                      )
                      .clickable {
                        val next = !isFluidTwinMode
                        isFluidTwinMode = next
                        if (next) {
                          isHighPriority = false
                          totalPoolHours = 0
                          totalPoolMinutes = 0
                          totalPoolHoursInput = ""
                          totalPoolMinutesInput = ""
                          part1DurationHours = 0
                          part1DurationMins = 0
                          part1SafetyFloor = 0
                          part2DurationHours = 0
                          part2DurationMins = 0
                          part2SafetyFloor = 0
                          splitValidationError = null
                        }
                      }
                      .testTag("header_split_pill"),
                    color = if (isFluidTwinMode) Color(0xFF8B5CF6).copy(alpha = 0.22f) else Color(0xFF1E293B)
                  ) {
                    Text(
                      text = "Split",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace,
                      color = if (isFluidTwinMode) Color(0xFFDDD6FE) else Color(0xFF94A3B8),
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==============================================================
        // BRANCH: ANCHOR EDIT (PRESERVES PHASE 2 RULES) vs ROUTINE STUDIO
        // ==============================================================
        if (isAnchor) {
          // -----------------------------------------------------------
          // PRESERVED ANCHOR EDIT FORM (Slot 1, Slot 5, Standard Anchors)
          // -----------------------------------------------------------
          AnchorEditSection(
            slotNum = slotNum ?: 1,
            isSlot1 = isSlot1,
            isSlot5 = isSlot5,
            title = title,
            onTitleChange = { title = it },
            startHour = startHour,
            startMinute = startMinute,
            onOpenClock = {
              clockTarget = 0
              showAnalogClockModal = true
            },
            durationHours = durationHours,
            durationMins = durationMins,
            onDurationHoursChange = { durationHours = it },
            onDurationMinsChange = { durationMins = it },
            voiceMessage = voiceMessage,
            onVoiceMessageChange = { voiceMessage = it },
            onTestVoice = {
              if (isSpeakingVoicePreview) {
                ttsInstance?.stop()
                isSpeakingVoicePreview = false
                activeVoiceUtteranceId = null
              } else if (isTtsReady && voiceMessage.isNotBlank()) {
                AutomationManager.applyTtsLanguage(ttsInstance, voiceMessage, activeVoicePresetId)
                val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceVolume.coerceIn(0f, 1f)) }
                ttsInstance?.speak(voiceMessage, TextToSpeech.QUEUE_FLUSH, params, "anchor_tts")
              }
            },
            isSlot3 = isSlot3,
            activeSlot3Tab = activeSlot3Tab,
            onActiveSlot3TabChange = { activeSlot3Tab = it },
            fridayTitle = fridayTitle,
            onFridayTitleChange = { fridayTitle = it },
            fridayStartHour = fridayStartHour,
            fridayStartMinute = fridayStartMinute,
            onOpenFridayClock = {
              clockTarget = 3
              showAnalogClockModal = true
            },
            fridayDurationHours = fridayDurationHours,
            fridayDurationMins = fridayDurationMins,
            onFridayDurationHoursChange = { fridayDurationHours = it },
            onFridayDurationMinsChange = { fridayDurationMins = it },
            fridayVoiceMessage = fridayVoiceMessage,
            onFridayVoiceMessageChange = { fridayVoiceMessage = it },
            onTestFridayVoice = {
              if (isSpeakingVoicePreview) {
                ttsInstance?.stop()
                isSpeakingVoicePreview = false
                activeVoiceUtteranceId = null
              } else if (isTtsReady && fridayVoiceMessage.isNotBlank()) {
                AutomationManager.applyTtsLanguage(ttsInstance, fridayVoiceMessage, activeVoicePresetId)
                val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceVolume.coerceIn(0f, 1f)) }
                ttsInstance?.speak(fridayVoiceMessage, TextToSpeech.QUEUE_FLUSH, params, "friday_tts")
              }
            },
            isSpeakingVoicePreview = isSpeakingVoicePreview,
            isBengaliVoiceMissing = isTtsReady && !isBengaliVoiceReady,
            voiceVolume = voiceVolume,
            onVoiceVolumeChange = { voiceVolume = it },
            enableDnd = enableDnd,
            onEnableDndChange = { enableDnd = it },
            dndDelayMinutes = dndDelayMinutes,
            onDndDelayMinutesChange = { dndDelayMinutes = it },
            isSoundAlertEnabled = isSoundAlertEnabled,
            onIsSoundAlertEnabledChange = { isSoundAlertEnabled = it },
            customRingtoneTitle = customRingtoneTitle,
            onPickRingtone = {
              val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Anchor Alert Sound")
                customRingtoneUri?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it)) }
              }
              ringtonePickerLauncher.launch(intent)
            },
            isPlayingTonePreview = isPlayingTonePreview,
            onToggleTonePreview = toggleTonePreview,
            isVoiceEnabled = isVoiceEnabled,
            onIsVoiceEnabledChange = { isVoiceEnabled = it },
            selectedDays = selectedDays,
            onSelectedDaysChange = { selectedDays = it }
          )
        } else {
          // -----------------------------------------------------------
          // LUXURY ROUTINE STUDIO FORM (DYNAMIC TASKS - OBSIDIAN STUDIO)
          // -----------------------------------------------------------

          // Shared Header & Domain Category Pill Row (Rendered for BOTH Single & Split modes)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (isFluidTwinMode) "FLUID TWIN DOMAIN" else "TASK TITLE",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 1.sp,
              color = Color(0xFF94A3B8)
            )

            // Interactive Category Pill Dropdown
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(20.dp))
                .clickable { showCategoryGridModal = true }
                .testTag("category_selector_card"),
              color = Color(0xFF0F172A)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = selectedCategory.icon, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = selectedCategory.title,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFFE2E8F0)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.Default.KeyboardArrowDown,
                  contentDescription = null,
                  tint = Color(0xFF94A3B8),
                  modifier = Modifier.size(14.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (!isFluidTwinMode) {

            // Task Title Input Field
            OutlinedTextField(
              value = title,
              onValueChange = { title = it },
              placeholder = { Text("e.g. Deep Work Session", color = Color(0xFF475569)) },
              modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0A0F1D),
                unfocusedContainerColor = Color(0xFF0A0F1D),
                focusedBorderColor = Color(0xFF06B6D4),
                unfocusedBorderColor = Color(0xFF1E2C45),
                focusedTextColor = Color(0xFFF8FAFC),
                unfocusedTextColor = Color(0xFFF8FAFC)
              ),
              shape = RoundedCornerShape(14.dp),
              singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Box Start Time & Duration Matrix
            LuxuryTimeDurationRow(
              accentColor = Color(0xFF06B6D4),
              hoursValue = durationHoursInput,
              onHoursValueChange = { raw ->
                val (clean, parsed) = sanitizeNumberInput(raw, 2)
                durationHoursInput = clean
                durationHours = parsed.coerceIn(0, 12)
              },
              hoursTestTag = "duration_hours_input",
              minsValue = durationMinsInput,
              onMinsValueChange = { raw ->
                val (clean, parsed) = sanitizeNumberInput(raw, 2)
                durationMinsInput = clean
                durationMins = parsed.coerceIn(0, 59)
              },
              minsTestTag = "duration_mins_input",
              showStartTime = true,
              startLabel = "START TIME",
              startTimeMinute = startHour * 60 + startMinute,
              birthStartMinute = if (isEditing && !isAnchor && !hasUserEditedStartTime) birthStartMinute else null,
              onOpenClock = {
                clockTarget = 0
                showAnalogClockModal = true
              },
              startTimeTestTag = "open_time_picker_button"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Ending Time Preview Row
            val currentTotalMins = durationHours * 60 + durationMins
            val currentEndMinute = ((startHour * 60 + startMinute + currentTotalMins) % 1440 + 1440) % 1440
            LuxuryBottomSummaryRow(
              label = "ENDING TIME",
              valueText = RoutineTask.formatTime(currentEndMinute),
              accentColor = Color(0xFF06B6D4),
              badgeTextColor = Color(0xFF22D3EE)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ==============================================================
            // 3. VOICE ANNOUNCEMENT STUDIO CARD
            // ==============================================================
            LuxuryVoiceStudioCard(
              title = "Voice Announcement",
              subtitle = "Hardware speech synthesis at start trigger",
              accentColor = Color(0xFF10B981),
              playTint = Color(0xFF10B981),
              isVoiceEnabled = isVoiceEnabled,
              onIsVoiceEnabledChange = { isVoiceEnabled = it },
              switchTestTag = null,
              voiceMessage = voiceMessage,
              onVoiceMessageChange = { voiceMessage = it },
              placeholderText = "e.g. Focus sprint starting now",
              messageTestTag = "voice_message_input",
              onTestVoice = {
                if (isSpeakingVoicePreview) {
                  ttsInstance?.stop()
                  isSpeakingVoicePreview = false
                  activeVoiceUtteranceId = null
                } else {
                  val msg = voiceMessage.ifBlank { "${title.ifBlank { "Task" }} starting now" }
                  AutomationManager.applyTtsLanguage(ttsInstance, msg, activeVoicePresetId)
                  val params = Bundle().apply {
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceVolume.coerceIn(0f, 1f))
                  }
                  ttsInstance?.speak(msg, TextToSpeech.QUEUE_FLUSH, params, "voice_test_preview")
                }
              },
              testButtonTestTag = "test_voice_button",
              isSpeakingVoicePreview = isSpeakingVoicePreview,
              isBengaliVoiceMissing = isTtsReady && !isBengaliVoiceReady,
              voiceVolume = voiceVolume,
              onVoiceVolumeChange = { voiceVolume = it }
            )

            Spacer(modifier = Modifier.height(14.dp))
          } else {
            // -----------------------------------------------------------
            // MODE 2: SPLIT TOGGLE IS ON (Dual Session Auto-Balancing Fluid Twin)
            // -----------------------------------------------------------

            // Top Controller: "TOTAL ROUTINE DURATION"
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                  Brush.verticalGradient(
                    listOf(Color(0xFF101727), Color(0xFF0A0E18))
                  )
                )
                .border(BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)), RoundedCornerShape(18.dp))
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(
                      text = "TOTAL ROUTINE DURATION",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace,
                      color = Color(0xFFC4B5FD)
                    )
                    Text(
                      text = "Opponent auto-balancing pool",
                      fontSize = 11.sp,
                      color = Color(0xFF94A3B8)
                    )
                  }

                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                      .border(BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                      .padding(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    Text(
                      text = "⚡ OPPONENT SYNC ACTIVE",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace,
                      color = Color(0xFFC4B5FD)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LuxuryTimeDurationRow(
                  accentColor = Color(0xFF8B5CF6),
                  hoursLabel = "TOTAL HOURS",
                  hoursValue = totalPoolHoursInput,
                  onHoursValueChange = { raw ->
                    val (clean, parsed) = sanitizeNumberInput(raw, 2)
                    totalPoolHoursInput = clean
                    val newH = parsed.coerceIn(0, 12)
                    totalPoolHours = newH
                    splitValidationError = null
                    val pool = newH * 60 + totalPoolMinutes
                    val s1 = part1DurationHours * 60 + part1DurationMins
                    val s2 = part2DurationHours * 60 + part2DurationMins
                    if ((s1 > 0 || s2 > 0) && pool >= 2) {
                      val newS1 = if (s1 > 0) s1.coerceIn(1, pool - 1) else (pool - s2.coerceIn(1, pool - 1)).coerceAtLeast(1)
                      val newS2 = (pool - newS1).coerceAtLeast(1)
                      part1DurationHours = newS1 / 60
                      part1DurationMins = newS1 % 60
                      part2DurationHours = newS2 / 60
                      part2DurationMins = newS2 % 60
                    }
                  },
                  hoursTestTag = "split_total_hours_input",
                  minsLabel = "TOTAL MINS",
                  minsValue = totalPoolMinutesInput,
                  onMinsValueChange = { raw ->
                    val (clean, parsed) = sanitizeNumberInput(raw, 2)
                    totalPoolMinutesInput = clean
                    val newM = parsed.coerceIn(0, 59)
                    totalPoolMinutes = newM
                    splitValidationError = null
                    val pool = totalPoolHours * 60 + newM
                    val s1 = part1DurationHours * 60 + part1DurationMins
                    val s2 = part2DurationHours * 60 + part2DurationMins
                    if ((s1 > 0 || s2 > 0) && pool >= 2) {
                      val newS1 = if (s1 > 0) s1.coerceIn(1, pool - 1) else (pool - s2.coerceIn(1, pool - 1)).coerceAtLeast(1)
                      val newS2 = (pool - newS1).coerceAtLeast(1)
                      part1DurationHours = newS1 / 60
                      part1DurationMins = newS1 % 60
                      part2DurationHours = newS2 / 60
                      part2DurationMins = newS2 % 60
                    }
                  },
                  minsTestTag = "split_total_mins_input",
                  showStartTime = false
                )
              }
            }

            if (splitValidationError != null) {
              Spacer(modifier = Modifier.height(10.dp))
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .border(BorderStroke(1.dp, DangerRed.copy(alpha = 0.6f)), RoundedCornerShape(12.dp)),
                color = DangerRed.copy(alpha = 0.14f)
              ) {
                Text(
                  text = splitValidationError ?: "",
                  color = Color(0xFFFCA5A5),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Connected Dual Session Cards
            // -----------------------------
            // Session 1 Card (Cyan Theme #38BDF8)
            DualSessionCard(
              sessionNumber = 1,
              resetKey = isFluidTwinMode,
              isEditingTwin = isEditingFluidTwin,
              accentColor = Color(0xFF38BDF8),
              title = part1Title,
              onTitleChange = { part1Title = it },
              startHour = part1StartHour,
              startMinute = part1StartMinute,
              birthStartMinute = if (isEditingFluidTwin && !hasUserEditedP1Start) p1BirthStart else null,
              onOpenClock = {
                clockTarget = 1
                showAnalogClockModal = true
              },
              durationHours = part1DurationHours,
              durationMins = part1DurationMins,
              onDurationChange = { h, m ->
                splitValidationError = null
                val pool = totalPoolHours * 60 + totalPoolMinutes
                val requested = (h * 60 + m).coerceAtLeast(0)
                if (requested == 0) {
                  part1DurationHours = 0
                  part1DurationMins = 0
                } else if (pool >= 2) {
                  val clamped = requested.coerceIn(1, pool - 1)
                  part1DurationHours = clamped / 60
                  part1DurationMins = clamped % 60
                  val remainingS2 = (pool - clamped).coerceAtLeast(1)
                  part2DurationHours = remainingS2 / 60
                  part2DurationMins = remainingS2 % 60
                } else {
                  part1DurationHours = requested / 60
                  part1DurationMins = requested % 60
                }
              },
              safetyFloor = part1SafetyFloor,
              onSafetyFloorChange = {
                splitValidationError = null
                part1SafetyFloor = it
              },
              isVoiceEnabled = isVoiceEnabledS1,
              onIsVoiceEnabledChange = { isVoiceEnabledS1 = it },
              voiceMessage = part1VoiceMessage,
              onVoiceMessageChange = { part1VoiceMessage = it },
              voiceVolume = voiceVolumeS1,
              onVoiceVolumeChange = { voiceVolumeS1 = it },
              onTestVoice = {
                if (isSpeakingVoicePreview && activeVoiceUtteranceId == "s1_tts") {
                  ttsInstance?.stop()
                  isSpeakingVoicePreview = false
                  activeVoiceUtteranceId = null
                } else {
                  val msg = part1VoiceMessage.ifBlank { "${part1Title.ifBlank { "Session 1" }} starting now" }
                  AutomationManager.applyTtsLanguage(ttsInstance, msg, activeVoicePresetId)
                  val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceVolumeS1.coerceIn(0f, 1f)) }
                  ttsInstance?.speak(msg, TextToSpeech.QUEUE_FLUSH, params, "s1_tts")
                }
              },
              isSpeakingVoicePreview = isSpeakingVoicePreview && activeVoiceUtteranceId == "s1_tts",
              isBengaliVoiceMissing = isTtsReady && !isBengaliVoiceReady
            )

            // Vertical Glowing Dashed Connector Line linking S1 and S2
            VerticalTwinConnector()

            // Session 2 Card (Violet Theme #8B5CF6)
            DualSessionCard(
              sessionNumber = 2,
              resetKey = isFluidTwinMode,
              isEditingTwin = isEditingFluidTwin,
              accentColor = Color(0xFF8B5CF6),
              title = part2Title,
              onTitleChange = { part2Title = it },
              startHour = part2StartHour,
              startMinute = part2StartMinute,
              birthStartMinute = if (isEditingFluidTwin && !hasUserEditedP2Start) p2BirthStart else null,
              onOpenClock = {
                clockTarget = 2
                showAnalogClockModal = true
              },
              durationHours = part2DurationHours,
              durationMins = part2DurationMins,
              onDurationChange = { h, m ->
                splitValidationError = null
                val pool = totalPoolHours * 60 + totalPoolMinutes
                val requested = (h * 60 + m).coerceAtLeast(0)
                if (requested == 0) {
                  part2DurationHours = 0
                  part2DurationMins = 0
                } else if (pool >= 2) {
                  val clamped = requested.coerceIn(1, pool - 1)
                  part2DurationHours = clamped / 60
                  part2DurationMins = clamped % 60
                  val remainingS1 = (pool - clamped).coerceAtLeast(1)
                  part1DurationHours = remainingS1 / 60
                  part1DurationMins = remainingS1 % 60
                } else {
                  part2DurationHours = requested / 60
                  part2DurationMins = requested % 60
                }
              },
              safetyFloor = part2SafetyFloor,
              onSafetyFloorChange = {
                splitValidationError = null
                part2SafetyFloor = it
              },
              isVoiceEnabled = isVoiceEnabledS2,
              onIsVoiceEnabledChange = { isVoiceEnabledS2 = it },
              voiceMessage = part2VoiceMessage,
              onVoiceMessageChange = { part2VoiceMessage = it },
              voiceVolume = voiceVolumeS2,
              onVoiceVolumeChange = { voiceVolumeS2 = it },
              onTestVoice = {
                if (isSpeakingVoicePreview && activeVoiceUtteranceId == "s2_tts") {
                  ttsInstance?.stop()
                  isSpeakingVoicePreview = false
                  activeVoiceUtteranceId = null
                } else {
                  val msg = part2VoiceMessage.ifBlank { "${part2Title.ifBlank { "Session 2" }} starting now" }
                  AutomationManager.applyTtsLanguage(ttsInstance, msg, activeVoicePresetId)
                  val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceVolumeS2.coerceIn(0f, 1f)) }
                  ttsInstance?.speak(msg, TextToSpeech.QUEUE_FLUSH, params, "s2_tts")
                }
              },
              isSpeakingVoicePreview = isSpeakingVoicePreview && activeVoiceUtteranceId == "s2_tts",
              isBengaliVoiceMissing = isTtsReady && !isBengaliVoiceReady
            )

            Spacer(modifier = Modifier.height(14.dp))
          }

          // ==============================================================
          // UNIFIED LUXURY 2x2 ROW 1: [ FLEXIBLE ] & [ BUFFER TIME ]
          // (Rendered for BOTH Single Dynamic Tasks and Split Fluid Twins)
          // ==============================================================
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Card 1: FLEXIBLE Card
            Box(
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 128.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                  Brush.verticalGradient(
                    listOf(Color(0xFF101727), Color(0xFF0A0E18))
                  )
                )
                .border(
                  BorderStroke(
                    1.dp,
                    if (customFlexibilityMinutes > 0) Color(0xFF06B6D4).copy(alpha = 0.45f) else Color(0xFF1E2B42)
                  ),
                  RoundedCornerShape(16.dp)
                )
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 128.dp)
                  .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .background(Color(0xFF06B6D4).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.AccessTime,
                      contentDescription = null,
                      tint = Color(0xFF06B6D4),
                      modifier = Modifier.size(15.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                  text = "FLEXIBLE",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  color = Color(0xFFE2E8F0),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                  Surface(
                    onClick = { decrementFlexibility() },
                    shape = CircleShape,
                    color = Color(0xFF141D2E),
                    border = BorderStroke(1.dp, Color(0xFF283A57)),
                    modifier = Modifier
                      .size(30.dp)
                      .testTag("decrement_flexibility_button")
                  ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                      Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    }
                  }

                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .height(36.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(Color(0xFF070B13))
                      .border(
                        1.dp,
                        if (customFlexibilityMinutes > 0) Color(0xFF06B6D4).copy(alpha = 0.45f) else Color(0xFF1E2B42),
                        RoundedCornerShape(10.dp)
                      )
                      .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    BasicTextField(
                      value = customFlexInput,
                      onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(2)
                        customFlexInput = digits
                        val parsed = digits.toIntOrNull() ?: 0
                        customFlexibilityMinutes = parsed.coerceIn(0, 30)
                      },
                      singleLine = true,
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      textStyle = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC),
                        textAlign = TextAlign.Center
                      ),
                      cursorBrush = SolidColor(Color(0xFF06B6D4)),
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_flexibility_input"),
                      decorationBox = { innerTextField ->
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.Center
                        ) {
                          Text("±", color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                          Spacer(modifier = Modifier.width(2.dp))
                          Box(contentAlignment = Alignment.Center) {
                            innerTextField()
                          }
                          Spacer(modifier = Modifier.width(2.dp))
                          Text("m", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        }
                      }
                    )
                  }

                  Surface(
                    onClick = { incrementFlexibility() },
                    shape = CircleShape,
                    color = Color(0xFF141D2E),
                    border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.5f)),
                    modifier = Modifier
                      .size(30.dp)
                      .testTag("increment_flexibility_button")
                  ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                      Text("+", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                    }
                  }
                }
              }
            }

            // Card 2: BUFFER TIME Card
            Box(
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 128.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                  Brush.verticalGradient(
                    listOf(Color(0xFF101727), Color(0xFF0A0E18))
                  )
                )
                .border(
                  BorderStroke(
                    1.dp,
                    if (interTaskBufferMinutes > 0) Color(0xFF10B981).copy(alpha = 0.45f) else Color(0xFF1E2B42)
                  ),
                  RoundedCornerShape(16.dp)
                )
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 128.dp)
                  .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(28.dp)
                      .clip(CircleShape)
                      .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = Icons.Default.Timelapse,
                      contentDescription = null,
                      tint = Color(0xFF10B981),
                      modifier = Modifier.size(15.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                  text = "BUFFER TIME",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  color = Color(0xFFE2E8F0),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                  Surface(
                    onClick = { decrementBuffer() },
                    shape = CircleShape,
                    color = Color(0xFF141D2E),
                    border = BorderStroke(1.dp, Color(0xFF283A57)),
                    modifier = Modifier
                      .size(30.dp)
                      .testTag("decrement_buffer_button")
                  ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                      Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    }
                  }

                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .height(36.dp)
                      .clip(RoundedCornerShape(10.dp))
                      .background(Color(0xFF070B13))
                      .border(
                        1.dp,
                        if (interTaskBufferMinutes > 0) Color(0xFF10B981).copy(alpha = 0.45f) else Color(0xFF1E2B42),
                        RoundedCornerShape(10.dp)
                      )
                      .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    BasicTextField(
                      value = bufferInput,
                      onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(2)
                        bufferInput = digits
                        val parsed = digits.toIntOrNull() ?: 0
                        interTaskBufferMinutes = parsed.coerceIn(0, 30)
                      },
                      singleLine = true,
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      textStyle = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF8FAFC),
                        textAlign = TextAlign.Center
                      ),
                      cursorBrush = SolidColor(Color(0xFF10B981)),
                      modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_buffer_input"),
                      decorationBox = { innerTextField ->
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.Center
                        ) {
                          Box(contentAlignment = Alignment.Center) {
                            innerTextField()
                          }
                          Spacer(modifier = Modifier.width(2.dp))
                          Text("m", color = Color(0xFF94A3B8), fontSize = 10.sp)
                        }
                      }
                    )
                  }

                  Surface(
                    onClick = { incrementBuffer() },
                    shape = CircleShape,
                    color = Color(0xFF141D2E),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                    modifier = Modifier
                      .size(30.dp)
                      .testTag("increment_buffer_button")
                  ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                      Text("+", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // ==============================================================
          // UNIFIED HARDWARE INTEGRATION GRID [ TASK ALERTS ] & [ AUTO DND ]
          // (Rendered for BOTH Single Dynamic Tasks and Split Fluid Twins)
          // ==============================================================
          LuxuryHardwareGrid(
            isSoundAlertEnabled = isSoundAlertEnabled,
            onIsSoundAlertEnabledChange = { isSoundAlertEnabled = it },
            soundSwitchTestTag = null,
            customRingtoneTitle = customRingtoneTitle,
            onPickRingtone = {
              val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Task Sound")
                customRingtoneUri?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it)) }
              }
              ringtonePickerLauncher.launch(intent)
            },
            pickRingtoneTestTag = null,
            isPlayingTonePreview = isPlayingTonePreview,
            onToggleTonePreview = toggleTonePreview,
            previewToneTestTag = "task_ringtone_preview_button",
            enableDnd = enableDnd,
            onEnableDndChange = { enableDnd = it },
            dndSwitchTestTag = null,
            isDndDelayLockedZero = false,
            supportsDndDelay = false,
            dndDelayMinutes = 0,
            onDndDelayMinutesChange = {}
          )

          Spacer(modifier = Modifier.height(14.dp))

          // ==============================================================
          // UNIFIED RECURRENCE SCHEDULE CARD
          // ==============================================================
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(16.dp)),
            color = Color(0xFF0A0F1D)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "RECURRENCE SCHEDULE",
                  color = Color(0xFF94A3B8),
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  letterSpacing = 0.5.sp,
                  fontSize = 11.sp
                )

                // Capsule Selector
                Row(
                  modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A))
                    .padding(2.dp)
                ) {
                  Surface(
                    modifier = Modifier
                      .clip(RoundedCornerShape(16.dp))
                      .clickable {
                        isDailyRecurrence = true
                        selectedDays = RoutineDay.allDays()
                      },
                    color = if (isDailyRecurrence) Color(0xFF06B6D4) else Color.Transparent
                  ) {
                    Text(
                      text = "Daily",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isDailyRecurrence) Color.Black else Color(0xFF94A3B8),
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                  }

                  Surface(
                    modifier = Modifier
                      .clip(RoundedCornerShape(16.dp))
                      .clickable {
                        isDailyRecurrence = false
                      },
                    color = if (!isDailyRecurrence) Color(0xFF06B6D4) else Color.Transparent
                  ) {
                    Text(
                      text = "Custom",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (!isDailyRecurrence) Color.Black else Color(0xFF94A3B8),
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                  }
                }
              }

              if (!isDailyRecurrence) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  listOf(
                    RoutineDay.SATURDAY to "S",
                    RoutineDay.SUNDAY to "S",
                    RoutineDay.MONDAY to "M",
                    RoutineDay.TUESDAY to "T",
                    RoutineDay.WEDNESDAY to "W",
                    RoutineDay.THURSDAY to "T",
                    RoutineDay.FRIDAY to "F"
                  ).forEach { (day, label) ->
                    val isDaySelected = selectedDays.contains(day)
                    Box(
                      modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                          if (isDaySelected) Color(0xFF06B6D4)
                          else Color(0xFF0F172A)
                        )
                        .border(
                          BorderStroke(
                            1.dp,
                            if (isDaySelected) Color(0xFF22D3EE) else Color(0xFF1E2C45)
                          ),
                          CircleShape
                        )
                        .clickable {
                          selectedDays = if (isDaySelected) {
                            if (selectedDays.size > 1) selectedDays - day else selectedDays
                          } else {
                            selectedDays + day
                          }
                        },
                      contentAlignment = Alignment.Center
                    ) {
                      Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDaySelected) Color.Black else Color(0xFF94A3B8)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
      }

      // ==============================================================
      // 7. STICKY BOTTOM ACTION BAR (Over Frosted Glass Surface)
      // ==============================================================
      Surface(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth(),
        color = Color(0xFF080A0F).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, Color(0xFF1E2C45))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Luxury Gradient Save CTA Button (Cyan to Blue)
          Button(
            onClick = { triggerSave() },
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .testTag("save_task_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(16.dp)
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  if (isFluidTwinMode) Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6)))
                  else Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF2563EB))),
                  shape = RoundedCornerShape(16.dp)
                ),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (isAnchor) "Save Anchor Changes" else "✓ Save & Apply Routine",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              }
            }
          }

          // Move to Trash (when editing existing dynamic task)
          if (isEditing && !isAnchor) {
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedButton(
              onClick = { showDeleteConfirmDialog = true },
              modifier = Modifier
                .height(52.dp)
                .testTag("move_to_trash_edit_form_button"),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
              border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
              shape = RoundedCornerShape(16.dp)
            ) {
              Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed)
            }
          }
        }
      }
    }
  }

  // ==============================================================
  // 13-CATEGORY GRID MODAL DIALOG
  // ==============================================================
  if (showCategoryGridModal) {
    AlertDialog(
      onDismissRequest = { showCategoryGridModal = false },
      title = {
        Column {
          Text(
            text = "Select Routine Domain",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF8FAFC)
          )
          Text(
            text = "14 FIXED CHRONOS DOMAINS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF60A5FA)
          )
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ROUTINE_CATEGORIES.chunked(2).forEach { pair ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              pair.forEach { cat ->
                val isSelected = selectedCategory.id == cat.id
                Surface(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                      BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF38BDF8) else Color(0xFF1F2637)
                      ),
                      RoundedCornerShape(14.dp)
                    )
                    .clickable {
                      selectedCategory = cat
                      showCategoryGridModal = false
                    },
                  color = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F141E)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(text = cat.icon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = cat.title,
                      color = if (isSelected) Color(0xFF38BDF8) else Color(0xFFF1F5F9),
                      fontSize = 12.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      maxLines = 1
                    )
                  }
                }
              }
              if (pair.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showCategoryGridModal = false }) {
          Text("Close", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
        }
      },
      containerColor = Color(0xFF080A0F),
      shape = RoundedCornerShape(24.dp)
    )
  }

  // ==============================================================
  // 6. CUSTOM CIRCULAR ANALOG CLOCK MODAL
  // ==============================================================
  if (showAnalogClockModal) {
    val initialH = when (clockTarget) {
      1 -> part1StartHour
      2 -> part2StartHour
      3 -> fridayStartHour
      else -> startHour
    }
    val initialM = when (clockTarget) {
      1 -> part1StartMinute
      2 -> part2StartMinute
      3 -> fridayStartMinute
      else -> startMinute
    }

    CustomAnalogClockModal(
      initialHour24 = initialH,
      initialMinute = initialM,
      onConfirm = { h, m ->
        when (clockTarget) {
          1 -> {
            if (h != part1StartHour || m != part1StartMinute) {
              hasUserEditedP1Start = true
            }
            part1StartHour = h
            part1StartMinute = m
          }
          2 -> {
            if (h != part2StartHour || m != part2StartMinute) {
              hasUserEditedP2Start = true
            }
            part2StartHour = h
            part2StartMinute = m
          }
          3 -> {
            fridayStartHour = h
            fridayStartMinute = m
          }
          else -> {
            if (!isAnchor && (h != startHour || m != startMinute)) {
              hasUserEditedStartTime = true
            }
            startHour = h
            startMinute = m
          }
        }
        showAnalogClockModal = false
      },
      onDismiss = { showAnalogClockModal = false }
    )
  }

  // Delete Confirmation Dialog (Dual Action for Fluid Twins)
  if (showDeleteConfirmDialog && initialTask != null) {
    val isTwin = initialTask.isFluidTwin && initialTask.fluidTwinId != null
    val twinPartner = if (isTwin) {
      allDynamicTasks.firstOrNull { !it.isDeleted && it.fluidTwinId == initialTask.fluidTwinId && it.id != initialTask.id }
    } else null

    if (isTwin && twinPartner != null) {
      AlertDialog(
        onDismissRequest = { showDeleteConfirmDialog = false },
        title = { Text("Delete Fluid Twin Session?", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
          Text(
            "“${initialTask.title}” is part of a 2-part fluid routine. Choose whether to absorb its duration into “${twinPartner.title}” or delete both parts.",
            color = TextSecondary,
            fontSize = 13.sp
          )
        },
        confirmButton = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
              onClick = {
                showDeleteConfirmDialog = false
                if (onAbsorbTwin != null) {
                  onAbsorbTwin(initialTask.id, twinPartner.id)
                } else {
                  onMoveToTrash(initialTask.id)
                }
                onDismiss()
              },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4), contentColor = Color.White),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Absorb Duration into Partner", fontWeight = FontWeight.Bold)
            }
            Button(
              onClick = {
                showDeleteConfirmDialog = false
                if (onDeleteTwinBoth != null) {
                  onDeleteTwinBoth(initialTask.fluidTwinId!!)
                } else {
                  onMoveToTrash(initialTask.id)
                  onMoveToTrash(twinPartner.id)
                }
                onDismiss()
              },
              colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Delete Both Parts", fontWeight = FontWeight.Bold)
            }
            TextButton(
              onClick = { showDeleteConfirmDialog = false },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Cancel", color = TextSecondary)
            }
          }
        },
        dismissButton = null,
        containerColor = Color(0xFF121620),
        shape = RoundedCornerShape(24.dp)
      )
    } else {
      AlertDialog(
        onDismissRequest = { showDeleteConfirmDialog = false },
        title = { Text("Delete Routine Task?", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
          Text(
            "“${initialTask.title}” will be moved to the Trash Bin. You can restore it within 30 days.",
            color = TextSecondary,
            fontSize = 13.sp
          )
        },
        confirmButton = {
          Button(
            onClick = {
              showDeleteConfirmDialog = false
              onMoveToTrash(initialTask.id)
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Move to Trash", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { showDeleteConfirmDialog = false }) {
            Text("Cancel", color = TextSecondary)
          }
        },
        containerColor = Color(0xFF121620),
        shape = RoundedCornerShape(24.dp)
      )
    }
  }

  // Unsaved Changes Dismissal Guard Dialog
  if (showUnsavedChangesDialog) {
    AlertDialog(
      onDismissRequest = { showUnsavedChangesDialog = false },
      containerColor = Color(0xFF121620),
      titleContentColor = Color(0xFFF8FAFC),
      textContentColor = Color(0xFF94A3B8),
      shape = RoundedCornerShape(22.dp),
      tonalElevation = 6.dp,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Discard Unsaved Changes?",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF8FAFC)
          )
        }
      },
      text = {
        Text(
          text = "You have unsaved edits in this routine task. What would you like to do?",
          fontSize = 13.sp,
          color = Color(0xFF94A3B8),
          lineHeight = 18.sp
        )
      },
      confirmButton = {
        // Save & Apply (Emerald Gradient)
        Button(
          onClick = {
            showUnsavedChangesDialog = false
            triggerSave()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("dialog_save_and_apply_button")
        ) {
          Text("Save & Apply", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          // Discard (Destructive Crimson)
          TextButton(
            onClick = {
              showUnsavedChangesDialog = false
              onDismiss()
            },
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444)),
            modifier = Modifier.testTag("dialog_discard_button")
          ) {
            Text("Discard", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }

          // Keep Editing (Neutral)
          TextButton(
            onClick = { showUnsavedChangesDialog = false },
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8)),
            modifier = Modifier.testTag("dialog_keep_editing_button")
          ) {
            Text("Keep Editing", fontSize = 12.sp)
          }
        }
      }
    )
  }
}

/**
 * Luxury Obsidian Studio Anchor Edit Form (Zero Functionality Loss).
 */
@Composable
private fun AnchorEditSection(
  slotNum: Int,
  isSlot1: Boolean,
  isSlot5: Boolean,
  title: String,
  onTitleChange: (String) -> Unit,
  startHour: Int,
  startMinute: Int,
  onOpenClock: () -> Unit,
  durationHours: Int,
  durationMins: Int,
  onDurationHoursChange: (Int) -> Unit,
  onDurationMinsChange: (Int) -> Unit,
  voiceMessage: String,
  onVoiceMessageChange: (String) -> Unit,
  onTestVoice: () -> Unit,
  isSlot3: Boolean,
  activeSlot3Tab: Int,
  onActiveSlot3TabChange: (Int) -> Unit,
  fridayTitle: String,
  onFridayTitleChange: (String) -> Unit,
  fridayStartHour: Int,
  fridayStartMinute: Int,
  onOpenFridayClock: () -> Unit,
  fridayDurationHours: Int,
  fridayDurationMins: Int,
  onFridayDurationHoursChange: (Int) -> Unit,
  onFridayDurationMinsChange: (Int) -> Unit,
  fridayVoiceMessage: String,
  onFridayVoiceMessageChange: (String) -> Unit,
  onTestFridayVoice: () -> Unit,
  voiceVolume: Float,
  onVoiceVolumeChange: (Float) -> Unit,
  enableDnd: Boolean,
  onEnableDndChange: (Boolean) -> Unit,
  dndDelayMinutes: Int,
  onDndDelayMinutesChange: (Int) -> Unit,
  isSoundAlertEnabled: Boolean,
  onIsSoundAlertEnabledChange: (Boolean) -> Unit,
  customRingtoneTitle: String,
  onPickRingtone: () -> Unit,
  isPlayingTonePreview: Boolean,
  onToggleTonePreview: () -> Unit,
  isVoiceEnabled: Boolean = true,
  onIsVoiceEnabledChange: (Boolean) -> Unit = {},
  isSpeakingVoicePreview: Boolean = false,
  isBengaliVoiceMissing: Boolean = false,
  selectedDays: Set<RoutineDay>,
  onSelectedDaysChange: (Set<RoutineDay>) -> Unit
) {
  var hoursInput by remember(durationHours) { mutableStateOf(durationHours.toString()) }
  var minsInput by remember(durationMins) { mutableStateOf(durationMins.toString()) }
  var friHoursInput by remember(fridayDurationHours) { mutableStateOf(fridayDurationHours.toString()) }
  var friMinsInput by remember(fridayDurationMins) { mutableStateOf(fridayDurationMins.toString()) }

  val accentColor = when (slotNum) {
    1 -> Color(0xFF6366F1) // Deep Indigo
    3 -> Color(0xFF10B981) // Emerald
    5 -> Color(0xFFF59E0B) // Amber
    else -> Color(0xFF06B6D4) // Cyan
  }
  val accentLight = when (slotNum) {
    1 -> Color(0xFFA5B4FC) // Lavender
    3 -> Color(0xFF6EE7B7) // Mint
    5 -> Color(0xFFFEF3C7) // Warm Gold
    else -> Color(0xFF38BDF8) // Sky Blue
  }

  Column {
    // ==============================================================
    // 1. ANCHOR SLOT 3 FRIDAY-SPECIFIC DUAL MODE (CAPSULE SWITCHER)
    // ==============================================================
    if (isSlot3) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(16.dp)),
        color = Color(0xFF0A0F1D)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          // Regular Days Tab (Sat-Thu)
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(if (activeSlot3Tab == 0) Color(0xFF06B6D4).copy(alpha = 0.2f) else Color.Transparent)
              .border(
                BorderStroke(1.dp, if (activeSlot3Tab == 0) Color(0xFF06B6D4) else Color.Transparent),
                RoundedCornerShape(12.dp)
              )
              .clickable { onActiveSlot3TabChange(0) }
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Regular Days (Sat-Thu)",
              fontSize = 11.sp,
              fontWeight = if (activeSlot3Tab == 0) FontWeight.Bold else FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
              color = if (activeSlot3Tab == 0) Color(0xFF38BDF8) else Color(0xFF94A3B8)
            )
          }

          // Friday Mode Tab (Jumu'ah / Reflection)
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(if (activeSlot3Tab == 1) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Transparent)
              .border(
                BorderStroke(1.dp, if (activeSlot3Tab == 1) Color(0xFF10B981) else Color.Transparent),
                RoundedCornerShape(12.dp)
              )
              .clickable { onActiveSlot3TabChange(1) }
              .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Friday Mode 🕌",
              fontSize = 11.sp,
              fontWeight = if (activeSlot3Tab == 1) FontWeight.Bold else FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
              color = if (activeSlot3Tab == 1) Color(0xFF6EE7B7) else Color(0xFF94A3B8)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
    }

    // ==============================================================
    // BRANCH: FRIDAY TAB vs REGULAR TAB
    // ==============================================================
    if (isSlot3 && activeSlot3Tab == 1) {
      // -----------------------------------------------------------
      // FRIDAY MODE FORM
      // -----------------------------------------------------------
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "FRIDAY ANCHOR TITLE",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.sp,
          color = Color(0xFF94A3B8)
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF10B981).copy(alpha = 0.15f))
            .border(BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = "FRIDAY JUMU'AH",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF6EE7B7)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedTextField(
        value = fridayTitle,
        onValueChange = onFridayTitleChange,
        placeholder = { Text("e.g., Friday Jumu'ah & Reflection", color = Color(0xFF475569)) },
        modifier = Modifier.fillMaxWidth().testTag("friday_task_title_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = Color(0xFF0A0F1D),
          unfocusedContainerColor = Color(0xFF0A0F1D),
          focusedBorderColor = Color(0xFF10B981),
          unfocusedBorderColor = Color(0xFF1E2C45),
          focusedTextColor = Color(0xFFF8FAFC),
          unfocusedTextColor = Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Friday 3-Box Start Time & Duration Matrix
      LuxuryTimeDurationRow(
        accentColor = Color(0xFF10B981),
        hoursValue = friHoursInput,
        onHoursValueChange = { raw ->
          val (clean, parsed) = sanitizeNumberInput(raw, 2)
          friHoursInput = clean
          onFridayDurationHoursChange(parsed.coerceIn(0, 12))
        },
        hoursTestTag = "friday_duration_hours_input",
        minsValue = friMinsInput,
        onMinsValueChange = { raw ->
          val (clean, parsed) = sanitizeNumberInput(raw, 2)
          friMinsInput = clean
          onFridayDurationMinsChange(parsed.coerceIn(0, 59))
        },
        minsTestTag = "friday_duration_mins_input",
        showStartTime = true,
        startLabel = "FRIDAY START",
        startTimeMinute = fridayStartHour * 60 + fridayStartMinute,
        onOpenClock = onOpenFridayClock,
        startTimeTestTag = "open_friday_time_picker_button"
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Friday Ending Time Preview Row
      val totalFriMins = fridayDurationHours * 60 + fridayDurationMins
      val friEndMinute = ((fridayStartHour * 60 + fridayStartMinute + totalFriMins) % 1440 + 1440) % 1440
      LuxuryBottomSummaryRow(
        label = "ENDING TIME",
        valueText = RoutineTask.formatTime(friEndMinute),
        accentColor = Color(0xFF10B981),
        badgeTextColor = Color(0xFF6EE7B7)
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Friday Voice Announcement Card
      LuxuryVoiceStudioCard(
        title = "Friday Voice Announcement",
        subtitle = "Hardware speech synthesis at start trigger",
        accentColor = Color(0xFF10B981),
        playTint = Color(0xFF6EE7B7),
        isVoiceEnabled = isVoiceEnabled,
        onIsVoiceEnabledChange = onIsVoiceEnabledChange,
        switchTestTag = "friday_voice_switch",
        voiceMessage = fridayVoiceMessage,
        onVoiceMessageChange = onFridayVoiceMessageChange,
        placeholderText = "কাস্টম ভয়েস মেসেজ লিখুন...",
        messageTestTag = "friday_voice_message_input",
        onTestVoice = onTestFridayVoice,
        testButtonTestTag = "test_friday_voice_button",
        isSpeakingVoicePreview = isSpeakingVoicePreview,
        isBengaliVoiceMissing = isBengaliVoiceMissing,
        voiceVolume = voiceVolume,
        onVoiceVolumeChange = onVoiceVolumeChange,
        disabledHint = "Spoken reminders disabled for this anchor"
      )
    } else {
      // -----------------------------------------------------------
      // REGULAR DAYS / STANDARD ANCHOR FORM
      // -----------------------------------------------------------
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "ANCHOR TITLE",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.sp,
          color = Color(0xFF94A3B8)
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = "SLOT $slotNum",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = accentLight
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        placeholder = { Text("e.g. Core Anchor Slot $slotNum", color = Color(0xFF475569)) },
        modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = Color(0xFF0A0F1D),
          unfocusedContainerColor = Color(0xFF0A0F1D),
          focusedBorderColor = accentColor,
          unfocusedBorderColor = Color(0xFF1E2C45),
          focusedTextColor = Color(0xFFF8FAFC),
          unfocusedTextColor = Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
      )

      // Slot 1 & Slot 5: Start time is COMPLETELY HIDDEN
      if (!isSlot1 && !isSlot5) {
        Spacer(modifier = Modifier.height(14.dp))

        // Standard 3-Box Start Time & Duration Matrix
        LuxuryTimeDurationRow(
          accentColor = accentColor,
          hoursValue = hoursInput,
          onHoursValueChange = { raw ->
            val (clean, parsed) = sanitizeNumberInput(raw, 2)
            hoursInput = clean
            onDurationHoursChange(parsed.coerceIn(0, 12))
          },
          hoursTestTag = "duration_hours_input",
          minsValue = minsInput,
          onMinsValueChange = { raw ->
            val (clean, parsed) = sanitizeNumberInput(raw, 2)
            minsInput = clean
            onDurationMinsChange(parsed.coerceIn(0, 59))
          },
          minsTestTag = "duration_mins_input",
          showStartTime = true,
          startLabel = "START TIME",
          startTimeMinute = startHour * 60 + startMinute,
          onOpenClock = onOpenClock,
          startTimeTestTag = "open_time_picker_button"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Ending Time Preview Row
        val currentTotalMins = durationHours * 60 + durationMins
        val regEndMinute = ((startHour * 60 + startMinute + currentTotalMins) % 1440 + 1440) % 1440
        LuxuryBottomSummaryRow(
          label = "ENDING TIME",
          valueText = RoutineTask.formatTime(regEndMinute),
          accentColor = accentColor,
          badgeTextColor = accentLight
        )
      }

      // Slot 1 Sleep Duration Numeric Box & Target Sleep Quota (without REVERSE BEDTIME CALCULATION box)
      if (isSlot1) {
        Spacer(modifier = Modifier.height(14.dp))

        LuxuryTimeDurationRow(
          accentColor = Color(0xFF6366F1),
          hoursLabel = "SLEEP HOURS",
          hoursValue = hoursInput,
          onHoursValueChange = { raw ->
            val (clean, parsed) = sanitizeNumberInput(raw, 2)
            hoursInput = clean
            onDurationHoursChange(parsed.coerceIn(0, 12))
          },
          hoursTestTag = "duration_hours_input",
          minsLabel = "SLEEP MINS",
          minsValue = minsInput,
          onMinsValueChange = { raw ->
            val (clean, parsed) = sanitizeNumberInput(raw, 2)
            minsInput = clean
            onDurationMinsChange(parsed.coerceIn(0, 59))
          },
          minsTestTag = "duration_mins_input",
          showStartTime = false
        )

        Spacer(modifier = Modifier.height(10.dp))

        val totalSleepMins = durationHours * 60 + durationMins
        LuxuryBottomSummaryRow(
          label = "TARGET SLEEP QUOTA",
          valueText = "${durationHours}h ${durationMins}m ($totalSleepMins mins)",
          accentColor = Color(0xFF6366F1),
          badgeTextColor = Color(0xFFA5B4FC)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Voice Message Announcement Card with inline TTS preview
      LuxuryVoiceStudioCard(
        title = "Voice Announcement",
        subtitle = "Hardware speech synthesis at start trigger",
        accentColor = accentColor,
        playTint = accentLight,
        isVoiceEnabled = isVoiceEnabled,
        onIsVoiceEnabledChange = onIsVoiceEnabledChange,
        switchTestTag = "anchor_voice_switch",
        voiceMessage = voiceMessage,
        onVoiceMessageChange = onVoiceMessageChange,
        placeholderText = "কাস্টম ভয়েস মেসেজ লিখুন...",
        messageTestTag = "voice_message_input",
        onTestVoice = onTestVoice,
        testButtonTestTag = "test_voice_button",
        isSpeakingVoicePreview = isSpeakingVoicePreview,
        isBengaliVoiceMissing = isBengaliVoiceMissing,
        voiceVolume = voiceVolume,
        onVoiceVolumeChange = onVoiceVolumeChange,
        disabledHint = "Spoken reminders disabled for this anchor"
      )
    }

    // ==============================================================
    // 2. 2-COLUMN HARDWARE QUICK ACTION GRID (ALL ANCHORS)
    // ==============================================================
    Spacer(modifier = Modifier.height(14.dp))
    LuxuryHardwareGrid(
      isSoundAlertEnabled = isSoundAlertEnabled,
      onIsSoundAlertEnabledChange = onIsSoundAlertEnabledChange,
      soundSwitchTestTag = "anchor_sound_switch",
      customRingtoneTitle = customRingtoneTitle,
      onPickRingtone = onPickRingtone,
      pickRingtoneTestTag = "anchor_pick_ringtone_button",
      isPlayingTonePreview = isPlayingTonePreview,
      onToggleTonePreview = onToggleTonePreview,
      previewToneTestTag = "anchor_preview_tone_button",
      enableDnd = enableDnd,
      onEnableDndChange = onEnableDndChange,
      dndSwitchTestTag = "anchor_dnd_switch",
      isDndDelayLockedZero = isSlot1 || isSlot5,
      supportsDndDelay = !isSlot1 && !isSlot5,
      dndDelayMinutes = dndDelayMinutes,
      onDndDelayMinutesChange = onDndDelayMinutesChange
    )

    // ==============================================================
    // 3. SLOT 5 (ELASTIC BUFFER) WEEKLY RECURRENCE SELECTOR
    // ==============================================================
    if (isSlot5) {
      Spacer(modifier = Modifier.height(14.dp))
      val isAllDaysSelected = selectedDays.size == 7

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(16.dp)),
        color = Color(0xFF0A0F1D)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "RECURRENCE SCHEDULE",
                color = Color(0xFFF8FAFC),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
              )
              Text(
                text = if (isAllDaysSelected) "Buffer active all 7 days" else "Active on ${selectedDays.size} days",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    onSelectedDaysChange(RoutineDay.allDays())
                  },
                color = if (isAllDaysSelected) Color(0xFF06B6D4) else Color(0xFF0F172A)
              ) {
                Text(
                  text = "Everyday",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (isAllDaysSelected) Color.Black else Color(0xFF94A3B8),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }

              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    if (isAllDaysSelected) {
                      onSelectedDaysChange(setOf(RoutineDay.SATURDAY, RoutineDay.SUNDAY, RoutineDay.MONDAY, RoutineDay.TUESDAY, RoutineDay.WEDNESDAY, RoutineDay.THURSDAY))
                    }
                  },
                color = if (!isAllDaysSelected) Color(0xFF06B6D4) else Color(0xFF0F172A)
              ) {
                Text(
                  text = "Specific Days",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (!isAllDaysSelected) Color.Black else Color(0xFF94A3B8),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }
          }

          if (!isAllDaysSelected) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              listOf(
                RoutineDay.SATURDAY to "S",
                RoutineDay.SUNDAY to "S",
                RoutineDay.MONDAY to "M",
                RoutineDay.TUESDAY to "T",
                RoutineDay.WEDNESDAY to "W",
                RoutineDay.THURSDAY to "T",
                RoutineDay.FRIDAY to "F"
              ).forEach { (day, label) ->
                val isDaySelected = selectedDays.contains(day)
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isDaySelected) Color(0xFF06B6D4) else Color(0xFF0F172A))
                    .border(
                      BorderStroke(1.dp, if (isDaySelected) Color(0xFF22D3EE) else Color(0xFF1E2C45)),
                      CircleShape
                    )
                    .clickable {
                      val newSet = if (isDaySelected) {
                        if (selectedDays.size > 1) selectedDays - day else selectedDays
                      } else {
                        selectedDays + day
                      }
                      onSelectedDaysChange(newSet)
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDaySelected) Color.Black else Color(0xFF94A3B8)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LuxuryTimeDurationRow(
  accentColor: Color,
  hoursValue: String,
  onHoursValueChange: (String) -> Unit,
  hoursTestTag: String,
  minsValue: String,
  onMinsValueChange: (String) -> Unit,
  minsTestTag: String,
  hoursLabel: String = "HOURS",
  minsLabel: String = "MINS",
  showStartTime: Boolean = true,
  startLabel: String = "START TIME",
  startTimeMinute: Int = 0,
  birthStartMinute: Int? = null,
  onOpenClock: () -> Unit = {},
  startTimeTestTag: String = "open_time_picker_button",
  safetyFloorValue: String? = null,
  onSafetyFloorValueChange: ((String) -> Unit)? = null,
  safetyFloorError: Boolean = false,
  safetyFloorTestTag: String? = null
) {
  val hoursFocusRequester = remember { FocusRequester() }
  val minsFocusRequester = remember { FocusRequester() }
  val safetyFocusRequester = remember { FocusRequester() }

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    if (showStartTime) {
      Box(
        modifier = Modifier
          .weight(1.3f)
          .heightIn(min = 72.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(
            Brush.verticalGradient(
              listOf(Color(0xFF101727), Color(0xFF0A0E18))
            )
          )
          .border(BorderStroke(1.dp, Color(0xFF1E2B42)), RoundedCornerShape(16.dp))
          .clickable { onOpenClock() }
          .testTag(startTimeTestTag)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = startLabel,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.5.sp,
              color = Color(0xFF94A3B8)
            )
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(13.dp)
            )
          }
          Column {
            Text(
              text = RoutineTask.formatTime(startTimeMinute),
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFF8FAFC),
              maxLines = 1
            )
            if (birthStartMinute != null && birthStartMinute != startTimeMinute) {
              Text(
                text = "Born: ${RoutineTask.formatTime(birthStartMinute)}",
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF38BDF8),
                maxLines = 1
              )
            }
          }
        }
      }
    }

    // Hours Well
    Box(
      modifier = Modifier
        .weight(1f)
        .heightIn(min = 72.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF101727), Color(0xFF0A0E18))
          )
        )
        .border(BorderStroke(1.dp, Color(0xFF1E2B42)), RoundedCornerShape(16.dp))
        .clickable { hoursFocusRequester.requestFocus() }
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 72.dp)
          .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = hoursLabel,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 0.5.sp,
          color = Color(0xFF94A3B8),
          maxLines = 1
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF070B13))
            .border(1.dp, Color(0xFF1A2538), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          BasicTextField(
            value = hoursValue,
            onValueChange = onHoursValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
              color = Color(0xFFF8FAFC),
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            ),
            cursorBrush = SolidColor(accentColor),
            modifier = Modifier
              .fillMaxWidth()
              .focusRequester(hoursFocusRequester)
              .testTag(hoursTestTag),
            decorationBox = { innerTextField ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(contentAlignment = Alignment.CenterStart) {
                  if (hoursValue.isEmpty()) {
                    Text(
                      text = "00",
                      fontSize = 17.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF475569)
                    )
                  }
                  innerTextField()
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "h",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFF64748B)
                )
              }
            }
          )
        }
      }
    }

    // Minutes Well
    Box(
      modifier = Modifier
        .weight(1f)
        .heightIn(min = 72.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF101727), Color(0xFF0A0E18))
          )
        )
        .border(BorderStroke(1.dp, Color(0xFF1E2B42)), RoundedCornerShape(16.dp))
        .clickable { minsFocusRequester.requestFocus() }
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 72.dp)
          .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = minsLabel,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 0.5.sp,
          color = Color(0xFF94A3B8),
          maxLines = 1
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF070B13))
            .border(1.dp, Color(0xFF1A2538), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          BasicTextField(
            value = minsValue,
            onValueChange = onMinsValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = TextStyle(
              color = Color(0xFFF8FAFC),
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            ),
            cursorBrush = SolidColor(accentColor),
            modifier = Modifier
              .fillMaxWidth()
              .focusRequester(minsFocusRequester)
              .testTag(minsTestTag),
            decorationBox = { innerTextField ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(contentAlignment = Alignment.CenterStart) {
                  if (minsValue.isEmpty()) {
                    Text(
                      text = "00",
                      fontSize = 17.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF475569)
                    )
                  }
                  innerTextField()
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "m",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Color(0xFF64748B)
                )
              }
            }
          )
        }
      }
    }

    // Optional Safety Floor Well (DualSessionCard)
    if (safetyFloorValue != null && onSafetyFloorValueChange != null && safetyFloorTestTag != null) {
      Box(
        modifier = Modifier
          .weight(1.05f)
          .heightIn(min = 72.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(
            Brush.verticalGradient(
              listOf(Color(0xFF101727), Color(0xFF0A0E18))
            )
          )
          .border(
            BorderStroke(1.dp, if (safetyFloorError) DangerRed else Color(0xFF1E2B42)),
            RoundedCornerShape(16.dp)
          )
          .clickable { safetyFocusRequester.requestFocus() }
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(horizontal = 10.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "🛡️ SAFETY",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp,
            color = if (safetyFloorError) Color(0xFFFCA5A5) else Color(0xFF10B981),
            maxLines = 1
          )
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF070B13))
              .border(
                1.dp,
                if (safetyFloorError) DangerRed else Color(0xFF1A2538),
                RoundedCornerShape(10.dp)
              )
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            BasicTextField(
              value = safetyFloorValue,
              onValueChange = onSafetyFloorValueChange,
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              textStyle = TextStyle(
                color = Color(0xFFF8FAFC),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
              ),
              cursorBrush = SolidColor(Color(0xFF10B981)),
              modifier = Modifier
                .fillMaxWidth()
                .focusRequester(safetyFocusRequester)
                .testTag(safetyFloorTestTag),
              decorationBox = { innerTextField ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(contentAlignment = Alignment.CenterStart) {
                    if (safetyFloorValue.isEmpty()) {
                      Text(
                        text = "00",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                      )
                    }
                    innerTextField()
                  }
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "m",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                  )
                }
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LuxuryBottomSummaryRow(
  label: String,
  valueText: String,
  accentColor: Color,
  badgeTextColor: Color = accentColor
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xFF0B111E))
      .border(BorderStroke(1.dp, Color(0xFF1D2A40)), RoundedCornerShape(12.dp))
      .padding(horizontal = 12.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = label,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      color = Color(0xFF94A3B8)
    )
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(accentColor.copy(alpha = 0.14f))
        .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)), RoundedCornerShape(8.dp))
        .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
      Text(
        text = valueText,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = badgeTextColor
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LuxuryVoiceStudioCard(
  title: String,
  subtitle: String?,
  accentColor: Color,
  playTint: Color = accentColor,
  isVoiceEnabled: Boolean,
  onIsVoiceEnabledChange: (Boolean) -> Unit,
  switchTestTag: String?,
  voiceMessage: String,
  onVoiceMessageChange: (String) -> Unit,
  placeholderText: String,
  messageTestTag: String,
  onTestVoice: () -> Unit,
  testButtonTestTag: String,
  isSpeakingVoicePreview: Boolean,
  isBengaliVoiceMissing: Boolean,
  voiceVolume: Float,
  onVoiceVolumeChange: (Float) -> Unit,
  disabledHint: String? = null
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(
        Brush.verticalGradient(
          listOf(Color(0xFF101727), Color(0xFF0A0E18))
        )
      )
      .border(
        BorderStroke(
          1.dp,
          if (isVoiceEnabled) accentColor.copy(alpha = 0.35f) else Color(0xFF1E2B42)
        ),
        RoundedCornerShape(18.dp)
      )
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(accentColor.copy(alpha = 0.15f))
              .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(16.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = title,
              color = Color(0xFFF8FAFC),
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
              Text(
                text = subtitle,
                color = Color(0xFF64748B),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        Switch(
          checked = isVoiceEnabled,
          onCheckedChange = onIsVoiceEnabledChange,
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = accentColor,
            uncheckedThumbColor = Color(0xFF94A3B8),
            uncheckedTrackColor = Color(0xFF1E2C45)
          ),
          modifier = if (switchTestTag != null) Modifier.testTag(switchTestTag) else Modifier
        )
      }

      if (isVoiceEnabled) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF070B13))
            .border(1.dp, Color(0xFF1E2B42), RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .padding(end = 8.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            if (voiceMessage.isEmpty()) {
              Text(
                text = placeholderText,
                color = Color(0xFF475569),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            BasicTextField(
              value = voiceMessage,
              onValueChange = onVoiceMessageChange,
              textStyle = TextStyle(
                color = Color(0xFFF8FAFC),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
              ),
              cursorBrush = SolidColor(accentColor),
              modifier = Modifier
                .fillMaxWidth()
                .testTag(messageTestTag)
            )
          }

          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(
                if (isSpeakingVoicePreview) Color(0xFFEF4444).copy(alpha = 0.2f)
                else accentColor.copy(alpha = 0.16f)
              )
              .border(
                1.dp,
                if (isSpeakingVoicePreview) Color(0xFFEF4444).copy(alpha = 0.65f) else accentColor.copy(alpha = 0.45f),
                CircleShape
              )
              .clickable(onClick = onTestVoice)
              .testTag(testButtonTestTag),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isSpeakingVoicePreview) Icons.Default.Stop else Icons.Default.PlayArrow,
              contentDescription = if (isSpeakingVoicePreview) "Stop Voice Preview" else "Test Voice",
              tint = if (isSpeakingVoicePreview) Color(0xFFEF4444) else playTint,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        if (isBengaliVoiceMissing && AutomationManager.containsBengali(voiceMessage)) {
          Spacer(modifier = Modifier.height(8.dp))
          BengaliVoiceMissingWarningPill()
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeMute,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(16.dp)
          )
          Slider(
            value = voiceVolume,
            onValueChange = onVoiceVolumeChange,
            valueRange = 0f..1f,
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 8.dp),
            thumb = {
              Box(
                modifier = Modifier
                  .size(20.dp)
                  .clip(CircleShape)
                  .background(accentColor.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
              ) {
                Box(
                  modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                )
              }
            },
            track = { sliderState ->
              val span = (sliderState.valueRange.endInclusive - sliderState.valueRange.start).coerceAtLeast(0.0001f)
              val fraction = ((sliderState.value - sliderState.valueRange.start) / span).coerceIn(0f, 1f)
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(Color(0xFF1E2C45))
              ) {
                Box(
                  modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                      Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.65f), accentColor)
                      )
                    )
                )
              }
            }
          )
          Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${(voiceVolume * 100).roundToInt()}%",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      } else if (disabledHint != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = disabledHint,
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace,
          color = Color(0xFF64748B)
        )
      }
    }
  }
}

@Composable
private fun LuxuryHardwareGrid(
  isSoundAlertEnabled: Boolean,
  onIsSoundAlertEnabledChange: (Boolean) -> Unit,
  soundSwitchTestTag: String?,
  customRingtoneTitle: String,
  onPickRingtone: () -> Unit,
  pickRingtoneTestTag: String?,
  isPlayingTonePreview: Boolean,
  onToggleTonePreview: () -> Unit,
  previewToneTestTag: String,
  enableDnd: Boolean,
  onEnableDndChange: (Boolean) -> Unit,
  dndSwitchTestTag: String?,
  isDndDelayLockedZero: Boolean,
  supportsDndDelay: Boolean,
  dndDelayMinutes: Int,
  onDndDelayMinutesChange: (Int) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Column 1: Task Alerts Card
    Box(
      modifier = Modifier
        .weight(1f)
        .heightIn(min = 128.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF101727), Color(0xFF0A0E18))
          )
        )
        .border(
          BorderStroke(
            1.dp,
            if (isSoundAlertEnabled) Color(0xFF06B6D4).copy(alpha = 0.4f) else Color(0xFF1E2B42)
          ),
          RoundedCornerShape(16.dp)
        )
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 128.dp)
          .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(Color(0xFF06B6D4).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.MusicNote,
              contentDescription = null,
              tint = Color(0xFF06B6D4),
              modifier = Modifier.size(16.dp)
            )
          }

          Switch(
            checked = isSoundAlertEnabled,
            onCheckedChange = onIsSoundAlertEnabledChange,
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = Color(0xFF06B6D4),
              uncheckedThumbColor = Color(0xFF94A3B8),
              uncheckedTrackColor = Color(0xFF1E2C45)
            ),
            modifier = if (soundSwitchTestTag != null) Modifier.testTag(soundSwitchTestTag) else Modifier
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "TASK ALERTS",
          color = Color(0xFFE2E8F0),
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          val pickModifier = Modifier
            .weight(1f)
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF070B13))
            .border(1.dp, Color(0xFF1E2B42), RoundedCornerShape(10.dp))
            .clickable { onPickRingtone() }
          Box(
            modifier = if (pickRingtoneTestTag != null) pickModifier.testTag(pickRingtoneTestTag) else pickModifier
              .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            Text(
              text = customRingtoneTitle,
              color = Color(0xFF06B6D4),
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = if (pickRingtoneTestTag != null) Modifier.padding(horizontal = 8.dp) else Modifier
            )
          }

          Surface(
            onClick = onToggleTonePreview,
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF070B13),
            border = BorderStroke(
              1.dp,
              if (isPlayingTonePreview) Color(0xFFEF4444).copy(alpha = 0.65f) else Color(0xFF06B6D4).copy(alpha = 0.45f)
            ),
            modifier = Modifier
              .size(36.dp)
              .testTag(previewToneTestTag)
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
              Icon(
                imageVector = if (isPlayingTonePreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPlayingTonePreview) "Stop Ringtone Preview" else "Play Ringtone Preview",
                tint = if (isPlayingTonePreview) Color(0xFFEF4444) else Color(0xFF06B6D4),
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    }

    // Column 2: Auto DND Card
    Box(
      modifier = Modifier
        .weight(1f)
        .heightIn(min = 128.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF101727), Color(0xFF0A0E18))
          )
        )
        .border(
          BorderStroke(
            1.dp,
            if (enableDnd) Color(0xFFA855F7).copy(alpha = 0.4f) else Color(0xFF1E2B42)
          ),
          RoundedCornerShape(16.dp)
        )
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 128.dp)
          .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(Color(0xFFA855F7).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Bedtime,
              contentDescription = null,
              tint = Color(0xFFA855F7),
              modifier = Modifier.size(16.dp)
            )
          }

          Switch(
            checked = enableDnd,
            onCheckedChange = onEnableDndChange,
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = Color(0xFFA855F7),
              uncheckedThumbColor = Color(0xFF94A3B8),
              uncheckedTrackColor = Color(0xFF1E2C45)
            ),
            modifier = if (dndSwitchTestTag != null) Modifier.testTag(dndSwitchTestTag) else Modifier
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "AUTO DND",
          color = Color(0xFFE2E8F0),
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isDndDelayLockedZero) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF070B13))
              .border(1.dp, Color(0xFF1E2B42), RoundedCornerShape(10.dp))
              .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            Text(
              text = "🔒 0m Immediate",
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              color = Color(0xFF94A3B8),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        } else if (supportsDndDelay && enableDnd) {
          var delayInputText by remember(dndDelayMinutes) { mutableStateOf(dndDelayMinutes.toString()) }
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            Surface(
              onClick = {
                val next = (dndDelayMinutes - 1).coerceIn(0, 25)
                delayInputText = next.toString()
                onDndDelayMinutesChange(next)
              },
              shape = CircleShape,
              color = Color(0xFF141D2E),
              border = BorderStroke(1.dp, Color(0xFF283A57)),
              modifier = Modifier.size(30.dp)
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("-", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
              }
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF070B13))
                .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(horizontal = 6.dp),
              contentAlignment = Alignment.Center
            ) {
              BasicTextField(
                value = delayInputText,
                onValueChange = { raw ->
                  val (clean, parsed) = sanitizeNumberInput(raw, 2)
                  delayInputText = clean
                  onDndDelayMinutesChange(parsed.coerceIn(0, 25))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFF8FAFC),
                  textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(Color(0xFF8B5CF6)),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("anchor_dnd_delay_input"),
                decorationBox = { innerTextField ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      innerTextField()
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                      text = "m",
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF8B5CF6)
                    )
                  }
                }
              )
            }

            Surface(
              onClick = {
                val next = (dndDelayMinutes + 1).coerceIn(0, 25)
                delayInputText = next.toString()
                onDndDelayMinutesChange(next)
              },
              shape = CircleShape,
              color = Color(0xFF141D2E),
              border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)),
              modifier = Modifier.size(30.dp)
            ) {
              Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text("+", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC4B5FD))
              }
            }
          }
        } else {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF070B13))
              .border(1.dp, Color(0xFF1E2B42), RoundedCornerShape(10.dp))
              .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(if (enableDnd) Color(0xFF10B981) else Color(0xFF64748B))
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (enableDnd) "Engaged" else "Disabled",
                color = if (enableDnd) Color(0xFF10B981) else Color(0xFF64748B),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DualSessionCard(
  sessionNumber: Int,
  resetKey: Boolean,
  isEditingTwin: Boolean,
  accentColor: Color,
  title: String,
  onTitleChange: (String) -> Unit,
  startHour: Int,
  startMinute: Int,
  birthStartMinute: Int? = null,
  onOpenClock: () -> Unit,
  durationHours: Int,
  durationMins: Int,
  onDurationChange: (h: Int, m: Int) -> Unit,
  safetyFloor: Int,
  onSafetyFloorChange: (Int) -> Unit,
  isVoiceEnabled: Boolean,
  onIsVoiceEnabledChange: (Boolean) -> Unit,
  voiceMessage: String,
  onVoiceMessageChange: (String) -> Unit,
  voiceVolume: Float,
  onVoiceVolumeChange: (Float) -> Unit,
  onTestVoice: () -> Unit,
  isSpeakingVoicePreview: Boolean = false,
  isBengaliVoiceMissing: Boolean = false
) {
  var hoursInput by remember(sessionNumber, resetKey) {
    mutableStateOf(if (isEditingTwin && durationHours > 0) durationHours.toString() else "")
  }
  var minsInput by remember(sessionNumber, resetKey) {
    mutableStateOf(if (isEditingTwin && durationMins > 0) durationMins.toString() else "")
  }
  var safetyFloorInput by remember(sessionNumber, resetKey) {
    mutableStateOf(if (isEditingTwin && safetyFloor > 0) safetyFloor.toString() else "")
  }

  LaunchedEffect(durationHours, durationMins) {
    val currentH = hoursInput.toIntOrNull() ?: 0
    val currentM = minsInput.toIntOrNull() ?: 0
    if (currentH != durationHours || currentM != durationMins) {
      hoursInput = if (durationHours == 0) "" else durationHours.toString()
      minsInput = if (durationMins == 0) "" else durationMins.toString()
    }
  }

  val sessionDurationTotal = durationHours * 60 + durationMins
  val isSafetyFloorExceeded = safetyFloor > 0 && safetyFloor > sessionDurationTotal

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(
        Brush.verticalGradient(
          listOf(Color(0xFF101727), Color(0xFF0A0E18))
        )
      )
      .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)), RoundedCornerShape(18.dp))
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Header Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accentColor.copy(alpha = 0.2f))
            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = "SESSION $sessionNumber",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = accentColor
          )
        }

        val sessionEndMinute = ((startHour * 60 + startMinute + sessionDurationTotal) % 1440 + 1440) % 1440
        Text(
          text = "${durationHours}h ${durationMins}m • → ${RoutineTask.formatTime(sessionEndMinute)}",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          color = Color(0xFFF8FAFC)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Title Input
      OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        placeholder = { Text(if (sessionNumber == 1) "Morning Deep Work" else "Afternoon Synthesis", color = TextTertiary) },
        modifier = Modifier.fillMaxWidth().testTag("session_${sessionNumber}_title_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = Color(0xFF070B13),
          unfocusedContainerColor = Color(0xFF070B13),
          focusedBorderColor = accentColor,
          unfocusedBorderColor = Color(0xFF1E2B42),
          focusedTextColor = Color(0xFFF8FAFC),
          unfocusedTextColor = Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Start Time Row
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .border(BorderStroke(1.dp, Color(0xFF1E2B42)), RoundedCornerShape(14.dp))
          .clickable { onOpenClock() },
        color = Color(0xFF070B13)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Start: ${RoutineTask.formatTime(startHour * 60 + startMinute)}",
                color = Color(0xFFF8FAFC),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
              )
              val displayedStart = startHour * 60 + startMinute
              if (isEditingTwin && birthStartMinute != null && birthStartMinute != displayedStart) {
                Text(
                  text = "Born: ${RoutineTask.formatTime(birthStartMinute)}",
                  fontSize = 9.5.sp,
                  fontFamily = FontFamily.Monospace,
                  color = Color(0xFF38BDF8)
                )
              }
            }
          }
          Text(text = "Change 🕒", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Duration & Safety Floor Inputs
      LuxuryTimeDurationRow(
        accentColor = accentColor,
        hoursValue = hoursInput,
        onHoursValueChange = { raw ->
          val (clean, parsed) = sanitizeNumberInput(raw, 2)
          hoursInput = clean
          onDurationChange(parsed.coerceIn(0, 12), durationMins)
        },
        hoursTestTag = "session_${sessionNumber}_hours_input",
        minsValue = minsInput,
        onMinsValueChange = { raw ->
          val (clean, parsed) = sanitizeNumberInput(raw, 2)
          minsInput = clean
          onDurationChange(durationHours, parsed.coerceIn(0, 59))
        },
        minsTestTag = "session_${sessionNumber}_mins_input",
        showStartTime = false,
        safetyFloorValue = safetyFloorInput,
        onSafetyFloorValueChange = { raw ->
          val (clean, parsed) = sanitizeNumberInput(raw, 3)
          safetyFloorInput = clean
          val floor = parsed.coerceIn(0, 720)
          onSafetyFloorChange(floor)
        },
        safetyFloorError = isSafetyFloorExceeded,
        safetyFloorTestTag = "session_${sessionNumber}_safety_input"
      )

      if (isSafetyFloorExceeded) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Safety floor (${safetyFloor}m) exceeds Session $sessionNumber duration (${sessionDurationTotal}m)",
          color = Color(0xFFFCA5A5),
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Full Voice Studio Sub-Card
      LuxuryVoiceStudioCard(
        title = "Voice Announcement",
        subtitle = "Session $sessionNumber speech synthesis",
        accentColor = Color(0xFF10B981),
        playTint = Color(0xFF10B981),
        isVoiceEnabled = isVoiceEnabled,
        onIsVoiceEnabledChange = onIsVoiceEnabledChange,
        switchTestTag = "session_${sessionNumber}_voice_switch",
        voiceMessage = voiceMessage,
        onVoiceMessageChange = onVoiceMessageChange,
        placeholderText = "Session $sessionNumber voice announcement",
        messageTestTag = "session_${sessionNumber}_voice_input",
        onTestVoice = onTestVoice,
        testButtonTestTag = "session_${sessionNumber}_test_voice_button",
        isSpeakingVoicePreview = isSpeakingVoicePreview,
        isBengaliVoiceMissing = isBengaliVoiceMissing,
        voiceVolume = voiceVolume,
        onVoiceVolumeChange = onVoiceVolumeChange,
        disabledHint = "Spoken reminders disabled for Session $sessionNumber"
      )
    }
  }
}

@Composable
private fun BengaliVoiceMissingWarningPill() {
  val context = LocalContext.current
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .border(BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
      .clickable {
        try {
          val installIntent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
          context.startActivity(installIntent)
        } catch (_: Exception) {
          try {
            val settingsIntent = Intent("com.android.settings.TTS_SETTINGS").apply {
              flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(settingsIntent)
          } catch (_: Exception) {}
        }
      }
      .testTag("bengali_tts_warning_pill"),
    color = Color(0xFFF59E0B).copy(alpha = 0.14f)
  ) {
    Text(
      text = "⚠️ বাংলা ভয়েস ডাটা নেই — ডাউনলোড করতে ট্যাপ করুন",
      color = Color(0xFFFDE68A),
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
    )
  }
}

/**
 * Vertical Glowing Dashed Connector Line visually linking Session 1 and Session 2.
 */
@Composable
private fun VerticalTwinConnector() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(44.dp),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val startX = size.width / 2f
      drawLine(
        brush = Brush.verticalGradient(
          listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6))
        ),
        start = Offset(startX, 0f),
        end = Offset(startX, size.height),
        strokeWidth = 2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
      )
    }

    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF080A0F))
        .border(BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
        .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
      Text(
        text = "⇅ EQUAL BALANCE",
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = Color(0xFFC4B5FD)
      )
    }
  }
}

/**
 * Custom Circular Analog Clock Modal.
 * Digital readout [ HH : MM ] [ AM / PM ] on top.
 * Interactive circular watch dial with rotating clock hand for selecting hours and minutes.
 */
@Composable
private fun CustomAnalogClockModal(
  initialHour24: Int,
  initialMinute: Int,
  onConfirm: (hour24: Int, minute: Int) -> Unit,
  onDismiss: () -> Unit
) {
  val haptic = LocalHapticFeedback.current
  var isSelectingHour by remember { mutableStateOf(true) }
  var isPm by remember { mutableStateOf(initialHour24 >= 12) }
  var selectedHour12 by remember {
    val h = initialHour24 % 12
    mutableIntStateOf(if (h == 0) 12 else h)
  }
  var selectedMinute by remember { mutableIntStateOf(initialMinute) }

  var hourInput by remember {
    mutableStateOf(String.format(Locale.US, "%02d", selectedHour12))
  }
  var minuteInput by remember {
    mutableStateOf(String.format(Locale.US, "%02d", selectedMinute))
  }

  val hourFocusRequester = remember { FocusRequester() }
  val minuteFocusRequester = remember { FocusRequester() }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Select Time",
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFF8FAFC)
        )
        Text(
          text = if (isSelectingHour) "TYPE DIGITS OR DRAG/TAP DIAL" else "TYPE MINUTES OR DRAG/TAP DIAL",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          color = Color(0xFF38BDF8)
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Digital Readout Bar: [ HH : MM ] & [ AM / PM ]
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Hour Editable Box
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .border(
                BorderStroke(
                  1.5.dp,
                  if (isSelectingHour) Color(0xFF38BDF8) else Color(0xFF1F2637)
                ),
                RoundedCornerShape(12.dp)
              )
              .clickable {
                isSelectingHour = true
                hourFocusRequester.requestFocus()
              },
            color = if (isSelectingHour) Color(0xFF1E293B) else Color(0xFF121620)
          ) {
            Box(
              modifier = Modifier
                .width(66.dp)
                .padding(vertical = 6.dp, horizontal = 4.dp),
              contentAlignment = Alignment.Center
            ) {
              BasicTextField(
                value = hourInput,
                onValueChange = { raw ->
                  isSelectingHour = true
                  val digits = raw.filter { it.isDigit() }.take(2)
                  hourInput = digits
                  val parsed = digits.toIntOrNull()
                  if (parsed != null && parsed > 0) {
                    val validH = if (parsed > 12) 12 else parsed
                    if (validH != selectedHour12) {
                      selectedHour12 = validH
                      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                  }
                },
                textStyle = TextStyle(
                  fontSize = 30.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  textAlign = TextAlign.Center,
                  color = if (isSelectingHour) Color(0xFF38BDF8) else Color(0xFFF8FAFC)
                ),
                cursorBrush = SolidColor(Color(0xFF38BDF8)),
                keyboardOptions = KeyboardOptions(
                  keyboardType = KeyboardType.Number,
                  imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .focusRequester(hourFocusRequester)
                  .onFocusChanged { focusState ->
                    if (focusState.isFocused && !isSelectingHour) {
                      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      isSelectingHour = true
                    }
                  }
              )
            }
          }

          Text(
            text = ":",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(horizontal = 6.dp)
          )

          // Minute Editable Box
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .border(
                BorderStroke(
                  1.5.dp,
                  if (!isSelectingHour) Color(0xFF38BDF8) else Color(0xFF1F2637)
                ),
                RoundedCornerShape(12.dp)
              )
              .clickable {
                isSelectingHour = false
                minuteFocusRequester.requestFocus()
              },
            color = if (!isSelectingHour) Color(0xFF1E293B) else Color(0xFF121620)
          ) {
            Box(
              modifier = Modifier
                .width(66.dp)
                .padding(vertical = 6.dp, horizontal = 4.dp),
              contentAlignment = Alignment.Center
            ) {
              BasicTextField(
                value = minuteInput,
                onValueChange = { raw ->
                  isSelectingHour = false
                  val digits = raw.filter { it.isDigit() }.take(2)
                  minuteInput = digits
                  val parsed = digits.toIntOrNull()
                  if (parsed != null) {
                    val validM = if (parsed > 59) 59 else parsed
                    if (validM != selectedMinute) {
                      selectedMinute = validM
                      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                  }
                },
                textStyle = TextStyle(
                  fontSize = 30.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  textAlign = TextAlign.Center,
                  color = if (!isSelectingHour) Color(0xFF38BDF8) else Color(0xFFF8FAFC)
                ),
                cursorBrush = SolidColor(Color(0xFF38BDF8)),
                keyboardOptions = KeyboardOptions(
                  keyboardType = KeyboardType.Number,
                  imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .focusRequester(minuteFocusRequester)
                  .onFocusChanged { focusState ->
                    if (focusState.isFocused && isSelectingHour) {
                      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      isSelectingHour = false
                    }
                  }
              )
            }
          }

          Spacer(modifier = Modifier.width(16.dp))

          // AM / PM Toggle Column
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  isPm = false
                },
              color = if (!isPm) Color(0xFF3B82F6) else Color(0xFF1E293B)
            ) {
              Text(
                text = "AM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (!isPm) Color.White else Color(0xFF94A3B8),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }

            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  isPm = true
                },
              color = if (isPm) Color(0xFF3B82F6) else Color(0xFF1E293B)
            ) {
              Text(
                text = "PM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPm) Color.White else Color(0xFF94A3B8),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Watch Dial Row: [ - ] (38dp) + Spacer(8dp) + Dial (200dp) + Spacer(8dp) + [ + ] (38dp) = 292dp
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Left: Minus Button [-]
          Surface(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              if (isSelectingHour) {
                val prevH = if (selectedHour12 <= 1) 12 else selectedHour12 - 1
                selectedHour12 = prevH
                hourInput = String.format(Locale.US, "%02d", prevH)
              } else {
                val prevM = if (selectedMinute <= 0) 59 else selectedMinute - 1
                selectedMinute = prevM
                minuteInput = String.format(Locale.US, "%02d", prevM)
              }
            },
            shape = CircleShape,
            color = Color(0xFF161E31),
            border = BorderStroke(1.dp, Color(0xFF334155)),
            modifier = Modifier.size(38.dp)
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
              Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Center: Circular Watch Dial Canvas (Box size 200.dp)
          Box(
            modifier = Modifier
              .size(200.dp)
              .clip(CircleShape)
              .background(Color(0xFF0F141E))
              .border(BorderStroke(1.5.dp, Color(0xFF1F2637)), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            val dialRadius = 74.dp

            // Rotating Clock Hand Canvas with Unified Pointer Input
            Canvas(
              modifier = Modifier
                .fillMaxSize()
                .pointerInput(isSelectingHour) {
                  detectDragGestures(
                    onDragStart = { offset ->
                      val cx = size.width / 2f
                      val cy = size.height / 2f
                      val angleRad = atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble())
                      var deg = Math.toDegrees(angleRad).toFloat() + 90f
                      if (deg < 0) deg += 360f

                      if (isSelectingHour) {
                        val h = ((deg / 30f).roundToInt() % 12).let { if (it == 0) 12 else it }
                        if (h != selectedHour12) {
                          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                          selectedHour12 = h
                          hourInput = String.format(Locale.US, "%02d", h)
                        }
                      } else {
                        val m = ((deg / 6f).roundToInt() % 60)
                        if (m != selectedMinute) {
                          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                          selectedMinute = m
                          minuteInput = String.format(Locale.US, "%02d", m)
                        }
                      }
                    },
                    onDragEnd = {
                      if (isSelectingHour) {
                        isSelectingHour = false
                        minuteFocusRequester.requestFocus()
                      }
                    },
                    onDragCancel = {},
                    onDrag = { change, _ ->
                      change.consume()
                      val cx = size.width / 2f
                      val cy = size.height / 2f
                      val angleRad = atan2((change.position.y - cy).toDouble(), (change.position.x - cx).toDouble())
                      var deg = Math.toDegrees(angleRad).toFloat() + 90f
                      if (deg < 0) deg += 360f

                      if (isSelectingHour) {
                        val h = ((deg / 30f).roundToInt() % 12).let { if (it == 0) 12 else it }
                        if (h != selectedHour12) {
                          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                          selectedHour12 = h
                          hourInput = String.format(Locale.US, "%02d", h)
                        }
                      } else {
                        val m = ((deg / 6f).roundToInt() % 60)
                        if (m != selectedMinute) {
                          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                          selectedMinute = m
                          minuteInput = String.format(Locale.US, "%02d", m)
                        }
                      }
                    }
                  )
                }
            ) {
              val center = Offset(size.width / 2f, size.height / 2f)
              val rPx = dialRadius.toPx()

              val angleDeg = if (isSelectingHour) {
                (selectedHour12 * 30f) - 90f
              } else {
                (selectedMinute * 6f) - 90f
              }

              val rad = Math.toRadians(angleDeg.toDouble())
              val handX = center.x + rPx * cos(rad).toFloat()
              val handY = center.y + rPx * sin(rad).toFloat()

              // Draw line from center to selected position
              drawLine(
                color = Color(0xFF38BDF8),
                start = center,
                end = Offset(handX, handY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
              )

              // Draw selection knob
              drawCircle(
                color = Color(0xFF38BDF8),
                radius = 14.dp.toPx(),
                center = Offset(handX, handY)
              )

              // Center Pivot
              drawCircle(
                color = Color(0xFF38BDF8),
                radius = 5.dp.toPx(),
                center = center
              )
            }

            // 12 Dial Numerals
            for (i in 1..12) {
              val numeralAngle = Math.toRadians(((i * 30f) - 90f).toDouble())
              val numX = dialRadius.value * cos(numeralAngle).toFloat()
              val numY = dialRadius.value * sin(numeralAngle).toFloat()

              val text = if (isSelectingHour) {
                "$i"
              } else {
                val m = (i * 5) % 60
                String.format(Locale.US, "%02d", m)
              }

              val isCurrent = if (isSelectingHour) {
                selectedHour12 == i
              } else {
                selectedMinute == (i * 5) % 60
              }

              Box(
                modifier = Modifier
                  .size(28.dp)
                  .align(Alignment.Center)
                  .offset(x = numX.dp, y = numY.dp)
                  .clip(CircleShape)
                  .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (isSelectingHour) {
                      selectedHour12 = i
                      hourInput = String.format(Locale.US, "%02d", i)
                      isSelectingHour = false
                      minuteFocusRequester.requestFocus()
                    } else {
                      val m = (i * 5) % 60
                      selectedMinute = m
                      minuteInput = String.format(Locale.US, "%02d", m)
                    }
                  },
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = text,
                  fontSize = 11.sp,
                  fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                  color = if (isCurrent) Color.White else Color(0xFF94A3B8),
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Right: Plus Button [+]
          Surface(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              if (isSelectingHour) {
                val nextH = if (selectedHour12 >= 12) 1 else selectedHour12 + 1
                selectedHour12 = nextH
                hourInput = String.format(Locale.US, "%02d", nextH)
              } else {
                val nextM = if (selectedMinute >= 59) 0 else selectedMinute + 1
                selectedMinute = nextM
                minuteInput = String.format(Locale.US, "%02d", nextM)
              }
            },
            shape = CircleShape,
            color = Color(0xFF161E31),
            border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.5f)),
            modifier = Modifier.size(38.dp)
          ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
              Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsedH = hourInput.toIntOrNull()?.let { if (it in 1..12) it else selectedHour12 } ?: selectedHour12
          val parsedM = minuteInput.toIntOrNull()?.let { if (it in 0..59) it else selectedMinute } ?: selectedMinute
          val finalH = if (isPm) {
            if (parsedH == 12) 12 else parsedH + 12
          } else {
            if (parsedH == 12) 0 else parsedH
          }
          onConfirm(finalH, parsedM)
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFF3B82F6),
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text("Confirm Time", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = Color(0xFF94A3B8))
      }
    },
    containerColor = Color(0xFF080A0F),
    shape = RoundedCornerShape(24.dp)
  )
}
