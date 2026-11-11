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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.TaskType
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

/**
 * PHASE 5A & 6A: High-end AMOLED Dark "Routine Studio Form" & Preserved Anchor Editor.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskModalBottomSheet(
  initialTask: RoutineTask? = null,
  onDismiss: () -> Unit,
  onSaveTask: (RoutineTask) -> Unit,
  onSaveFluidTwin: (
    title: String,
    totalPoolMinutes: Int,
    part1Title: String,
    part1StartMinute: Int,
    part1Duration: Int,
    part1VoiceMessage: String,
    part2Title: String,
    part2StartMinute: Int,
    part2Duration: Int,
    part2VoiceMessage: String,
    enableDnd: Boolean,
    selectedDays: Set<RoutineDay>,
    isVoiceEnabled: Boolean,
    customNotificationTitle: String,
    customNotificationBody: String,
    customRingtoneUri: String?,
    customRingtoneTitle: String?,
    isSoundAlertEnabled: Boolean
  ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
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
  onSaveSlot5: (title: String, voiceMessage: String, selectedDays: Set<RoutineDay>) -> Unit,
  onMoveToTrash: (taskId: String) -> Unit = {}
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

  // Slot 3 Friday Dual Mode State
  var activeSlot3Tab by remember { mutableIntStateOf(0) } // 0 = Regular Days, 1 = Friday Mode 🕌
  var fridayTitle by remember { mutableStateOf(initialTask?.fridayTitle ?: "Jumu'ah") }
  val initialFridayStart = initialTask?.fridayStartTimeMinute ?: (12 * 60 + 30) // 12:30 PM default (750m)
  var fridayStartHour by remember { mutableIntStateOf(initialFridayStart / 60) }
  var fridayStartMinute by remember { mutableIntStateOf(initialFridayStart % 60) }
  val initialFridayDuration = initialTask?.fridayDurationMinutes ?: 120 // 2h default
  var fridayDurationHours by remember { mutableIntStateOf(initialFridayDuration / 60) }
  var fridayDurationMins by remember { mutableIntStateOf(initialFridayDuration % 60) }
  var fridayDurationHoursInput by remember { mutableStateOf((initialFridayDuration / 60).toString()) }
  var fridayDurationMinsInput by remember { mutableStateOf((initialFridayDuration % 60).toString()) }
  var fridayVoiceMessage by remember { mutableStateOf(initialTask?.fridayVoiceMessage ?: "জুমার সময় হয়েছে।") }

  // Tone Preview Player State
  var isPlayingTonePreview by remember { mutableStateOf(false) }
  var activeRingtone by remember { mutableStateOf<Ringtone?>(null) }

  // 13-Category Selector State
  var selectedCategory by remember {
    val initialCat = ROUTINE_CATEGORIES.firstOrNull { it.id == initialTask?.category }
      ?: ROUTINE_CATEGORIES.firstOrNull { initialTask?.title?.contains(it.title, ignoreCase = true) == true }
      ?: ROUTINE_CATEGORIES[0]
    mutableStateOf(initialCat)
  }
  var showCategoryGridModal by remember { mutableStateOf(false) }

  // 2-Session Mode Switcher State
  var isFluidTwinMode by remember { mutableStateOf(initialTask?.isFluidTwin ?: false) }

  // Total Routine Duration Controller (for Dual Fluid Twin Mode)
  val initialTotalPool = initialTask?.fluidTotalPoolMinutes ?: ((initialTask?.durationMinutes ?: 90) * 2).coerceAtLeast(180)
  var totalPoolHours by remember { mutableIntStateOf(initialTotalPool / 60) }
  var totalPoolMinutes by remember { mutableIntStateOf(initialTotalPool % 60) }
  var totalPoolHoursInput by remember { mutableStateOf((initialTotalPool / 60).toString()) }
  var totalPoolMinutesInput by remember { mutableStateOf((initialTotalPool % 60).toString()) }

  // Single Task / Master State
  var title by remember { mutableStateOf(initialTask?.title ?: "") }
  var startHour by remember { mutableIntStateOf((initialTask?.startTimeMinute ?: 540) / 60) }
  var startMinute by remember { mutableIntStateOf((initialTask?.startTimeMinute ?: 540) % 60) }
  var durationHours by remember { mutableIntStateOf((initialTask?.durationMinutes ?: 60) / 60) }
  var durationMins by remember { mutableIntStateOf((initialTask?.durationMinutes ?: 60) % 60) }
  var durationHoursInput by remember { mutableStateOf(((initialTask?.durationMinutes ?: 60) / 60).toString()) }
  var durationMinsInput by remember { mutableStateOf(((initialTask?.durationMinutes ?: 60) % 60).toString()) }
  var toleranceMinutes by remember { mutableIntStateOf(initialTask?.toleranceMinutes ?: 0) }
  var customFlexibilityMinutes by remember { mutableIntStateOf(initialTask?.customFlexibilityMinutes ?: 0) }
  var customFlexInput by remember { mutableStateOf((initialTask?.customFlexibilityMinutes ?: 0).toString()) }
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
      current in 5..25 -> (current / 5 + 1) * 5 // 5->10, 10->15, 15->20, 20->25, 25->30
      else -> 30
    }.coerceIn(0, 30)
    interTaskBufferMinutes = nextVal
    bufferInput = nextVal.toString()
  }
  var isHighPriority by remember { mutableStateOf(initialTask?.isHighPriority ?: false) }
  var isVoiceEnabled by remember { mutableStateOf(initialTask?.isVoiceEnabled ?: true) }
  var voiceMessage by remember { mutableStateOf(initialTask?.voiceMessage ?: "") }
  var voiceVolume by remember { mutableFloatStateOf(initialTask?.voiceVolume ?: 1f) }

  // Dual Session 1 (Cyan Theme #38BDF8)
  var part1Title by remember {
    mutableStateOf(if (initialTask?.isFluidTwin == true && initialTask.fluidTwinPart == 1) initialTask.title else "")
  }
  var part1StartHour by remember { mutableIntStateOf(startHour) }
  var part1StartMinute by remember { mutableIntStateOf(startMinute) }
  var part1DurationHours by remember { mutableIntStateOf(1) }
  var part1DurationMins by remember { mutableIntStateOf(30) }
  var part1SafetyFloor by remember { mutableIntStateOf(45) }
  var part1VoiceMessage by remember { mutableStateOf(voiceMessage) }

  // Dual Session 2 (Violet Theme #8B5CF6)
  var part2Title by remember {
    mutableStateOf(if (initialTask?.isFluidTwin == true && initialTask.fluidTwinPart == 2) initialTask.title else "")
  }
  var part2StartHour by remember { mutableIntStateOf(14) }
  var part2StartMinute by remember { mutableIntStateOf(0) }
  var part2DurationHours by remember { mutableIntStateOf(1) }
  var part2DurationMins by remember { mutableIntStateOf(30) }
  var part2SafetyFloor by remember { mutableIntStateOf(45) }
  var part2VoiceMessage by remember { mutableStateOf("") }

  // System Integrations & Recurrence
  var isSoundAlertEnabled by remember { mutableStateOf(initialTask?.isSoundAlertEnabled ?: true) }
  var customRingtoneUri by remember { mutableStateOf(initialTask?.customRingtoneUri) }
  var customRingtoneTitle by remember { mutableStateOf(initialTask?.customRingtoneTitle ?: "Default Notification Tone") }
  var enableDnd by remember { mutableStateOf(initialTask?.enableDnd ?: false) }
  var dndDelayMinutes by remember(initialTask) { mutableIntStateOf(initialTask?.dndDelayMinutes ?: 0) }
  var enableHaptic by remember { mutableStateOf(initialTask?.enableHaptic ?: true) }
  var selectedDays by remember { mutableStateOf(initialTask?.selectedDays ?: RoutineDay.allDays()) }
  var isDailyRecurrence by remember { mutableStateOf(selectedDays.size == 7) }

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

  DisposableEffect(context) {
    var instance: TextToSpeech? = null
    instance = TextToSpeech(context) { status ->
      if (status == TextToSpeech.SUCCESS) {
        isTtsReady = true
        try {
          instance?.language = Locale.getDefault()
        } catch (_: Exception) {}
      }
    }
    ttsInstance = instance
    onDispose {
      try {
        instance?.stop()
        instance?.shutdown()
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
  val initialDuration = remember(initialTask) { initialTask?.durationMinutes ?: 60 }
  val initialStartMin = remember(initialTask) { initialTask?.startTimeMinute ?: 540 }
  val initialVoice = remember(initialTask) { initialTask?.voiceMessage ?: "" }
  val initialDnd = remember(initialTask) { initialTask?.enableDnd ?: false }
  val initialSound = remember(initialTask) { initialTask?.isSoundAlertEnabled ?: true }

  // Unsaved Changes Dismissal Guard
  var showUnsavedChangesDialog by remember { mutableStateOf(false) }

  val hasUnsavedChanges by remember {
    derivedStateOf {
      if (initialTask == null) {
        title.isNotBlank() || voiceMessage.isNotBlank() || (durationHoursInput != "1" && durationHoursInput.isNotBlank())
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
          isHighPriority != (initialTask.isHighPriority ?: false) ||
          customFlexibilityMinutes != (initialTask.customFlexibilityMinutes ?: 0) ||
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
    if (isAnchor) {
      val calculatedDuration = durationHours * 60 + durationMins
      val calculatedStart = startHour * 60 + startMinute
      val calculatedFriDuration = fridayDurationHours * 60 + fridayDurationMins
      val calculatedFriStart = fridayStartHour * 60 + fridayStartMinute
      if (isSlot5) {
        onSaveSlot5(title.ifBlank { "Elastic Rubber Buffer" }, voiceMessage, selectedDays)
      } else if (slotNum != null) {
        if (isSlot1) {
          val finalSleepDuration = calculatedDuration.coerceIn(240, 720) // 4h to 12h
          val slot2WakeMinute = initialTask?.endTimeMinute ?: 300 // default 05:00 AM = 300m
          val calculatedBedtime = ((slot2WakeMinute - finalSleepDuration) % 1440 + 1440) % 1440
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
      val p1Dur = (part1DurationHours * 60 + part1DurationMins).coerceAtLeast(part1SafetyFloor)
      val p2Dur = (part2DurationHours * 60 + part2DurationMins).coerceAtLeast(part2SafetyFloor)
      val totalPool = (p1Dur + p2Dur).coerceAtLeast(part1SafetyFloor + part2SafetyFloor)
      val p1Start = part1StartHour * 60 + part1StartMinute
      val p2Start = part2StartHour * 60 + part2StartMinute
      val masterTitle = selectedCategory.title

      onSaveFluidTwin(
        masterTitle,
        totalPool,
        part1Title.ifBlank { "${selectedCategory.title} (Part 1)" },
        p1Start,
        p1Dur,
        part1VoiceMessage,
        part2Title.ifBlank { "${selectedCategory.title} (Part 2)" },
        p2Start,
        p2Dur,
        part2VoiceMessage,
        enableDnd,
        selectedDays,
        isVoiceEnabled,
        masterTitle,
        "",
        customRingtoneUri,
        customRingtoneTitle,
        isSoundAlertEnabled
      )
    } else {
      val calculatedDuration = durationHours * 60 + durationMins
      val calculatedStart = startHour * 60 + startMinute
      val actualTitle = title.ifBlank { "${selectedCategory.icon} ${selectedCategory.defaultName}" }

      val task = RoutineTask(
        id = initialTask?.id ?: "dynamic_${UUID.randomUUID().toString().take(8)}",
        title = actualTitle,
        type = TaskType.DYNAMIC,
        startTimeMinute = calculatedStart,
        durationMinutes = calculatedDuration,
        voiceMessage = voiceMessage,
        enableDnd = enableDnd,
        enableHaptic = enableHaptic,
        allowSplitting = false,
        isHighPriority = isHighPriority,
        customFlexibilityMinutes = customFlexibilityMinutes,
        selectedDays = selectedDays,
        isVoiceEnabled = isVoiceEnabled,
        customNotificationTitle = actualTitle,
        customNotificationBody = voiceMessage,
        customRingtoneUri = customRingtoneUri,
        customRingtoneTitle = customRingtoneTitle,
        isSoundAlertEnabled = isSoundAlertEnabled,
        toleranceMinutes = toleranceMinutes,
        interTaskBufferMinutes = interTaskBufferMinutes,
        category = selectedCategory.id
      )
      onSaveTask(task)
    }
    onDismiss()
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
            .padding(bottom = 96.dp) // Room for sticky bottom bar
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
          Row(verticalAlignment = Alignment.CenterVertically) {
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
            Column {
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
                color = TextPrimary
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
            // Header Pill Controls (Priority & Split)
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // 1. Priority Pill Button
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
                  .clickable { isHighPriority = !isHighPriority }
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

              // 2. Split Pill Button
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
                  .clickable { isFluidTwinMode = !isFluidTwinMode }
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
              if (isTtsReady && voiceMessage.isNotBlank()) {
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
              if (isTtsReady && fridayVoiceMessage.isNotBlank()) {
                val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceVolume.coerceIn(0f, 1f)) }
                ttsInstance?.speak(fridayVoiceMessage, TextToSpeech.QUEUE_FLUSH, params, "friday_tts")
              }
            },
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

          if (!isFluidTwinMode) {
            // Task Title Label & Domain Pill Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "TASK TITLE",
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
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Start Time Box
              Surface(
                modifier = Modifier
                  .weight(1.3f)
                  .clip(RoundedCornerShape(14.dp))
                  .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(14.dp))
                  .clickable {
                    clockTarget = 0
                    showAnalogClockModal = true
                  }
                  .testTag("open_time_picker_button"),
                color = Color(0xFF0A0F1D)
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = "START TIME",
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      fontFamily = FontFamily.Monospace,
                      letterSpacing = 0.5.sp,
                      color = Color(0xFF94A3B8)
                    )
                    Icon(
                      imageVector = Icons.Default.Schedule,
                      contentDescription = null,
                      tint = Color(0xFF06B6D4),
                      modifier = Modifier.size(13.dp)
                    )
                  }
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = RoutineTask.formatTime(startHour * 60 + startMinute),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC)
                  )
                  Text(
                    text = String.format(Locale.US, "%02d:%02d 24h", startHour, startMinute),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B)
                  )
                }
              }

              // Duration Hours Box
              Surface(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(14.dp)),
                color = Color(0xFF0A0F1D)
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(
                    text = "HOURS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF94A3B8)
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  OutlinedTextField(
                    value = durationHoursInput,
                    onValueChange = { raw ->
                      val (clean, parsed) = sanitizeNumberInput(raw, 2)
                      durationHoursInput = clean
                      durationHours = parsed.coerceIn(0, 12)
                    },
                    suffix = { Text("h", color = Color(0xFF64748B), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("duration_hours_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedContainerColor = Color.Transparent,
                      unfocusedContainerColor = Color.Transparent,
                      focusedBorderColor = Color(0xFF06B6D4),
                      unfocusedBorderColor = Color.Transparent,
                      focusedTextColor = Color(0xFFF8FAFC),
                      unfocusedTextColor = Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                  )
                }
              }

              // Duration Minutes Box
              Surface(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(14.dp)),
                color = Color(0xFF0A0F1D)
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(
                    text = "MINS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF94A3B8)
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  OutlinedTextField(
                    value = durationMinsInput,
                    onValueChange = { raw ->
                      val (clean, parsed) = sanitizeNumberInput(raw, 2)
                      durationMinsInput = clean
                      durationMins = parsed.coerceIn(0, 59)
                    },
                    suffix = { Text("m", color = Color(0xFF64748B), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("duration_mins_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedContainerColor = Color.Transparent,
                      unfocusedContainerColor = Color.Transparent,
                      focusedBorderColor = Color(0xFF06B6D4),
                      unfocusedBorderColor = Color.Transparent,
                      focusedTextColor = Color(0xFFF8FAFC),
                      unfocusedTextColor = Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Duration Badge Pill
            val currentTotalMins = durationHours * 60 + durationMins
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "DURATION PREVIEW",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF64748B)
              )
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFF06B6D4).copy(alpha = 0.12f))
                  .border(BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                  .padding(horizontal = 8.dp, vertical = 3.dp)
              ) {
                Text(
                  text = "${durationHours}h ${durationMins}m ($currentTotalMins mins total)",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  color = Color(0xFF22D3EE)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Side-by-Side Micro-Tiles: [FLEXIBLE] & [BUFFER TIME]
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Box 1 (Left - Flexible)
              Surface(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(14.dp)),
                color = Color(0xFF0A0F1D)
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF06B6D4).copy(alpha = 0.15f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF06B6D4),
                        modifier = Modifier.size(13.dp)
                      )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                      Text(
                        text = "FLEXIBLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE2E8F0)
                      )
                      Text(
                        text = "Compression tolerance",
                        fontSize = 9.sp,
                        color = Color(0xFF64748B)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    IconButton(
                      onClick = {
                        customFlexibilityMinutes = (customFlexibilityMinutes - 5).coerceAtLeast(0)
                        customFlexInput = customFlexibilityMinutes.toString()
                      },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    }
                    OutlinedTextField(
                      value = customFlexInput,
                      onValueChange = { raw ->
                        val (clean, parsed) = sanitizeNumberInput(raw, 2)
                        customFlexInput = clean
                        customFlexibilityMinutes = parsed.coerceIn(0, 30)
                      },
                      prefix = { Text("±", color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                      suffix = { Text("m", color = Color(0xFF94A3B8), fontSize = 10.sp) },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      modifier = Modifier
                        .width(72.dp)
                        .testTag("custom_flexibility_input"),
                      colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF1E2C45),
                        focusedTextColor = Color(0xFFF8FAFC),
                        unfocusedTextColor = Color(0xFFF8FAFC)
                      ),
                      shape = RoundedCornerShape(10.dp),
                      singleLine = true,
                      textStyle = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                      )
                    )
                    IconButton(
                      onClick = {
                        customFlexibilityMinutes = (customFlexibilityMinutes + 5).coerceAtMost(30)
                        customFlexInput = customFlexibilityMinutes.toString()
                      },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    }
                  }
                }
              }

              // Box 2 (Right - Buffer Time)
              Surface(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(14.dp)),
                color = Color(0xFF0A0F1D)
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = Icons.Default.Timelapse,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(13.dp)
                      )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                      Text(
                        text = "BUFFER TIME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE2E8F0)
                      )
                      Text(
                        text = "Trailing rest gap",
                        fontSize = 9.sp,
                        color = Color(0xFF64748B)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    // Minus Stepper [-]
                    Surface(
                      onClick = { decrementBuffer() },
                      shape = CircleShape,
                      color = Color(0xFF1E293B),
                      border = BorderStroke(1.dp, Color(0xFF334155)),
                      modifier = Modifier
                        .size(32.dp)
                        .testTag("decrement_buffer_button")
                    ) {
                      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                      }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Editable Numeric Display
                    OutlinedTextField(
                      value = bufferInput,
                      onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(2)
                        bufferInput = digits
                        val parsed = digits.toIntOrNull() ?: 0
                        interTaskBufferMinutes = parsed.coerceIn(0, 30)
                      },
                      suffix = { Text("m", color = Color(0xFF94A3B8), fontSize = 10.sp) },
                      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                      modifier = Modifier
                        .width(66.dp)
                        .testTag("custom_buffer_input"),
                      colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF1E2C45),
                        focusedTextColor = Color(0xFFF8FAFC),
                        unfocusedTextColor = Color(0xFFF8FAFC)
                      ),
                      shape = RoundedCornerShape(10.dp),
                      singleLine = true,
                      textStyle = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                      )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Plus Stepper [+]
                    Surface(
                      onClick = { incrementBuffer() },
                      shape = CircleShape,
                      color = Color(0xFF1E293B),
                      border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                      modifier = Modifier
                        .size(32.dp)
                        .testTag("increment_buffer_button")
                    ) {
                      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                      }
                    }
                  }
                }
              }
            }



            // Voice Announcement Studio Card
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(18.dp)),
              color = Color(0xFF0A0F1D)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                      )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text = "Voice Announcement",
                        color = Color(0xFFF8FAFC),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                      )
                      Text(
                        text = "Hardware speech synthesis at start trigger",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                      )
                    }
                  }

                  Switch(
                    checked = isVoiceEnabled,
                    onCheckedChange = { isVoiceEnabled = it },
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = Color.White,
                      checkedTrackColor = Color(0xFF10B981),
                      uncheckedThumbColor = Color(0xFF94A3B8),
                      uncheckedTrackColor = Color(0xFF1E2C45)
                    )
                  )
                }

                if (isVoiceEnabled) {
                  Spacer(modifier = Modifier.height(12.dp))
                  OutlinedTextField(
                    value = voiceMessage,
                    onValueChange = { voiceMessage = it },
                    placeholder = { Text("e.g. Focus sprint starting now", color = Color(0xFF475569)) },
                    trailingIcon = {
                      IconButton(
                        onClick = {
                          val msg = voiceMessage.ifBlank { "${title.ifBlank { "Task" }} starting now" }
                          val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceVolume.coerceIn(0f, 1f)) }
                          ttsInstance?.speak(msg, TextToSpeech.QUEUE_FLUSH, params, "voice_test")
                        },
                        modifier = Modifier.testTag("test_voice_button")
                      ) {
                        Icon(
                          imageVector = Icons.Default.PlayArrow,
                          contentDescription = "Test Voice",
                          tint = Color(0xFF10B981)
                        )
                      }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("voice_message_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedContainerColor = Color(0xFF0F172A),
                      unfocusedContainerColor = Color(0xFF0F172A),
                      focusedBorderColor = Color(0xFF10B981),
                      unfocusedBorderColor = Color(0xFF1E2C45),
                      focusedTextColor = Color(0xFFF8FAFC),
                      unfocusedTextColor = Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(12.dp)
                  )

                  Spacer(modifier = Modifier.height(10.dp))

                  // Volume Slider
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
                      onValueChange = { voiceVolume = it },
                      valueRange = 0f..1f,
                      modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                      colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF10B981),
                        activeTrackColor = Color(0xFF10B981),
                        inactiveTrackColor = Color(0xFF1E2C45)
                      )
                    )
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                      contentDescription = null,
                      tint = Color(0xFF10B981),
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
                }
              }
            }
          } else {
            // -----------------------------------------------------------
            // MODE 2: SPLIT TOGGLE IS ON (Dual Session Auto-Balancing Fluid Twin)
            // -----------------------------------------------------------

            // Top Controller: "TOTAL ROUTINE DURATION"
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)), RoundedCornerShape(18.dp)),
              color = Color(0xFF121620)
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

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  OutlinedTextField(
                    value = totalPoolHoursInput,
                    onValueChange = { raw ->
                      val (clean, parsed) = sanitizeNumberInput(raw, 2)
                      totalPoolHoursInput = clean
                      val newH = if (clean.isEmpty()) 1 else parsed.coerceIn(1, 12)
                      totalPoolHours = newH
                      // Auto-balance S2
                      val pool = newH * 60 + totalPoolMinutes
                      val s1 = part1DurationHours * 60 + part1DurationMins
                      val s2 = (pool - s1).coerceAtLeast(part2SafetyFloor)
                      part2DurationHours = s2 / 60
                      part2DurationMins = s2 % 60
                    },
                    label = { Text("Total Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedContainerColor = Color(0xFF0F141E),
                      unfocusedContainerColor = Color(0xFF0F141E),
                      focusedBorderColor = Color(0xFF8B5CF6),
                      unfocusedBorderColor = Color(0xFF1F2637),
                      focusedTextColor = Color(0xFFF8FAFC),
                      unfocusedTextColor = Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                  )

                  OutlinedTextField(
                    value = totalPoolMinutesInput,
                    onValueChange = { raw ->
                      val (clean, parsed) = sanitizeNumberInput(raw, 2)
                      totalPoolMinutesInput = clean
                      val newM = parsed.coerceIn(0, 59)
                      totalPoolMinutes = newM
                      val pool = totalPoolHours * 60 + newM
                      val s1 = part1DurationHours * 60 + part1DurationMins
                      val s2 = (pool - s1).coerceAtLeast(part2SafetyFloor)
                      part2DurationHours = s2 / 60
                      part2DurationMins = s2 % 60
                    },
                    label = { Text("Total Mins") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedContainerColor = Color(0xFF0F141E),
                      unfocusedContainerColor = Color(0xFF0F141E),
                      focusedBorderColor = Color(0xFF8B5CF6),
                      unfocusedBorderColor = Color(0xFF1F2637),
                      focusedTextColor = Color(0xFFF8FAFC),
                      unfocusedTextColor = Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Connected Dual Session Cards
            // -----------------------------
            // Session 1 Card (Cyan Theme #38BDF8)
            DualSessionCard(
              sessionNumber = 1,
              accentColor = Color(0xFF38BDF8),
              title = part1Title,
              onTitleChange = { part1Title = it },
              startHour = part1StartHour,
              startMinute = part1StartMinute,
              onOpenClock = {
                clockTarget = 1
                showAnalogClockModal = true
              },
              durationHours = part1DurationHours,
              durationMins = part1DurationMins,
              onDurationChange = { h, m ->
                val pool = totalPoolHours * 60 + totalPoolMinutes
                val requestedS1 = (h * 60 + m).coerceAtLeast(part1SafetyFloor)
                val maxS1 = (pool - part2SafetyFloor).coerceAtLeast(part1SafetyFloor)
                val clampedS1 = requestedS1.coerceIn(part1SafetyFloor, maxS1)
                part1DurationHours = clampedS1 / 60
                part1DurationMins = clampedS1 % 60
                val remainingS2 = pool - clampedS1
                part2DurationHours = remainingS2 / 60
                part2DurationMins = remainingS2 % 60
              },
              safetyFloor = part1SafetyFloor,
              onSafetyFloorChange = { part1SafetyFloor = it },
              voiceMessage = part1VoiceMessage,
              onVoiceMessageChange = { part1VoiceMessage = it },
              onTestVoice = {
                val msg = part1VoiceMessage.ifBlank { "${part1Title.ifBlank { "Session 1" }} starting now" }
                val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceVolume.coerceIn(0f, 1f)) }
                ttsInstance?.speak(msg, TextToSpeech.QUEUE_FLUSH, params, "s1_tts")
              }
            )

            // Vertical Glowing Dashed Connector Line linking S1 and S2
            VerticalTwinConnector()

            // Session 2 Card (Violet Theme #8B5CF6)
            DualSessionCard(
              sessionNumber = 2,
              accentColor = Color(0xFF8B5CF6),
              title = part2Title,
              onTitleChange = { part2Title = it },
              startHour = part2StartHour,
              startMinute = part2StartMinute,
              onOpenClock = {
                clockTarget = 2
                showAnalogClockModal = true
              },
              durationHours = part2DurationHours,
              durationMins = part2DurationMins,
              onDurationChange = { h, m ->
                val pool = totalPoolHours * 60 + totalPoolMinutes
                val requestedS2 = (h * 60 + m).coerceAtLeast(part2SafetyFloor)
                val maxS2 = (pool - part1SafetyFloor).coerceAtLeast(part2SafetyFloor)
                val clampedS2 = requestedS2.coerceIn(part2SafetyFloor, maxS2)
                part2DurationHours = clampedS2 / 60
                part2DurationMins = clampedS2 % 60
                val remainingS1 = pool - clampedS2
                part1DurationHours = remainingS1 / 60
                part1DurationMins = remainingS1 % 60
              },
              safetyFloor = part2SafetyFloor,
              onSafetyFloorChange = { part2SafetyFloor = it },
              voiceMessage = part2VoiceMessage,
              onVoiceMessageChange = { part2VoiceMessage = it },
              onTestVoice = {
                val msg = part2VoiceMessage.ifBlank { "${part2Title.ifBlank { "Session 2" }} starting now" }
                val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceVolume.coerceIn(0f, 1f)) }
                ttsInstance?.speak(msg, TextToSpeech.QUEUE_FLUSH, params, "s2_tts")
              }
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          // ==============================================================
          // 4. 2-COLUMN SYSTEM INTEGRATIONS GRID
          // ==============================================================
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Column 1: Task Alerts Card
            Surface(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(16.dp)),
              color = Color(0xFF0A0F1D)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
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
                    onCheckedChange = { isSoundAlertEnabled = it },
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = Color.White,
                      checkedTrackColor = Color(0xFF06B6D4),
                      uncheckedThumbColor = Color(0xFF94A3B8),
                      uncheckedTrackColor = Color(0xFF1E2C45)
                    )
                  )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                  text = "TASK ALERTS",
                  color = Color(0xFFE2E8F0),
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Task Sound")
                        customRingtoneUri?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it)) }
                      }
                      ringtonePickerLauncher.launch(intent)
                    },
                  color = Color(0xFF0F172A)
                ) {
                  Text(
                    text = customRingtoneTitle.take(18),
                    color = Color(0xFF06B6D4),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                  )
                }
              }
            }

            // Column 2: Auto DND Card
            Surface(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(16.dp)),
              color = Color(0xFF0A0F1D)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
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
                    onCheckedChange = { enableDnd = it },
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = Color.White,
                      checkedTrackColor = Color(0xFFA855F7),
                      uncheckedThumbColor = Color(0xFF94A3B8),
                      uncheckedTrackColor = Color(0xFF1E2C45)
                    )
                  )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                  text = "AUTO DND",
                  color = Color(0xFFE2E8F0),
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
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
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // ==============================================================
          // 5. RECURRENCE SCHEDULE CARD
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
            text = "13 FIXED CHRONOS DOMAINS",
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
                      if (title.isBlank()) {
                        title = "${cat.icon} ${cat.defaultName}"
                      }
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
            part1StartHour = h
            part1StartMinute = m
          }
          2 -> {
            part2StartHour = h
            part2StartMinute = m
          }
          3 -> {
            fridayStartHour = h
            fridayStartMinute = m
          }
          else -> {
            startHour = h
            startMinute = m
          }
        }
        showAnalogClockModal = false
      },
      onDismiss = { showAnalogClockModal = false }
    )
  }

  // Delete Confirmation Dialog
  if (showDeleteConfirmDialog && initialTask != null) {
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
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Start Time Box
        Surface(
          modifier = Modifier
            .weight(1.3f)
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(14.dp))
            .clickable { onOpenFridayClock() }
            .testTag("open_friday_time_picker_button"),
          color = Color(0xFF0A0F1D)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "FRIDAY START",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                color = Color(0xFF94A3B8)
              )
              Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(14.dp)
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = RoutineTask.formatTime(fridayStartHour * 60 + fridayStartMinute),
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFF8FAFC)
            )
            Text(
              text = "24h: ${String.format(Locale.US, "%02d:%02d", fridayStartHour, fridayStartMinute)} • Tap to dial",
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              color = Color(0xFF64748B)
            )
          }
        }

        // Hours Box
        Surface(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(14.dp)),
          color = Color(0xFF0A0F1D)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "HOURS",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.5.sp,
              color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = friHoursInput,
              onValueChange = { raw ->
                val (clean, parsed) = sanitizeNumberInput(raw, 2)
                friHoursInput = clean
                onFridayDurationHoursChange(parsed.coerceIn(0, 12))
              },
              suffix = { Text("h", color = Color(0xFF64748B), fontSize = 11.sp) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.fillMaxWidth().testTag("friday_duration_hours_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color(0xFFF8FAFC),
                unfocusedTextColor = Color(0xFFF8FAFC)
              ),
              shape = RoundedCornerShape(8.dp),
              singleLine = true
            )
          }
        }

        // Minutes Box
        Surface(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(14.dp)),
          color = Color(0xFF0A0F1D)
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "MINS",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.5.sp,
              color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
              value = friMinsInput,
              onValueChange = { raw ->
                val (clean, parsed) = sanitizeNumberInput(raw, 2)
                friMinsInput = clean
                onFridayDurationMinsChange(parsed.coerceIn(0, 59))
              },
              suffix = { Text("m", color = Color(0xFF64748B), fontSize = 11.sp) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.fillMaxWidth().testTag("friday_duration_mins_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color(0xFFF8FAFC),
                unfocusedTextColor = Color(0xFFF8FAFC)
              ),
              shape = RoundedCornerShape(8.dp),
              singleLine = true
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Friday Live Duration Preview Pill
      val totalFriMins = fridayDurationHours * 60 + fridayDurationMins
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "FRIDAY DURATION PREVIEW",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Monospace,
          color = Color(0xFF64748B)
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF10B981).copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = "${fridayDurationHours}h ${fridayDurationMins}m ($totalFriMins mins total)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF6EE7B7)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Friday Voice Announcement Card
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
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                  contentDescription = null,
                  tint = Color(0xFF10B981),
                  modifier = Modifier.size(15.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "FRIDAY VOICE ANNOUNCEMENT",
                color = Color(0xFFE2E8F0),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
              )
            }

            Switch(
              checked = isVoiceEnabled,
              onCheckedChange = onIsVoiceEnabledChange,
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF10B981),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E2C45)
              ),
              modifier = Modifier.testTag("friday_voice_switch")
            )
          }

          if (isVoiceEnabled) {
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
              value = fridayVoiceMessage,
              onValueChange = onFridayVoiceMessageChange,
              placeholder = { Text("কাস্টম ভয়েস মেসেজ লিখুন...", color = Color(0xFF475569)) },
              trailingIcon = {
                IconButton(onClick = onTestFridayVoice, modifier = Modifier.testTag("test_friday_voice_button")) {
                  Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Test Friday Voice",
                    tint = Color(0xFF6EE7B7)
                  )
                }
              },
              modifier = Modifier.fillMaxWidth().testTag("friday_voice_message_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F172A),
                unfocusedContainerColor = Color(0xFF0F172A),
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color(0xFF1E2C45),
                focusedTextColor = Color(0xFFF8FAFC),
                unfocusedTextColor = Color(0xFFF8FAFC)
              ),
              shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Volume Slider
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
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                  thumbColor = Color(0xFF10B981),
                  activeTrackColor = Color(0xFF10B981),
                  inactiveTrackColor = Color(0xFF1E2C45)
                )
              )
              Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = Color(0xFF10B981),
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
          } else {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Spoken reminders disabled for this anchor",
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              color = Color(0xFF64748B)
            )
          }
        }
      }
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
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Start Time Box
          Surface(
            modifier = Modifier
              .weight(1.3f)
              .clip(RoundedCornerShape(14.dp))
              .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(14.dp))
              .clickable { onOpenClock() }
              .testTag("open_time_picker_button"),
            color = Color(0xFF0A0F1D)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "START TIME",
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
                  modifier = Modifier.size(14.dp)
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = RoutineTask.formatTime(startHour * 60 + startMinute),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8FAFC)
              )
              Text(
                text = "24h: ${String.format(Locale.US, "%02d:%02d", startHour, startMinute)} • Tap to dial",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF64748B)
              )
            }
          }

          // Duration Hours Box
          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(14.dp)),
            color = Color(0xFF0A0F1D)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "HOURS",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                color = Color(0xFF94A3B8)
              )
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = hoursInput,
                onValueChange = { raw ->
                  val (clean, parsed) = sanitizeNumberInput(raw, 2)
                  hoursInput = clean
                  onDurationHoursChange(parsed.coerceIn(0, 12))
                },
                suffix = { Text("h", color = Color(0xFF64748B), fontSize = 11.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("duration_hours_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = Color.Transparent,
                  unfocusedContainerColor = Color.Transparent,
                  focusedBorderColor = accentColor,
                  unfocusedBorderColor = Color.Transparent,
                  focusedTextColor = Color(0xFFF8FAFC),
                  unfocusedTextColor = Color(0xFFF8FAFC)
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
              )
            }
          }

          // Duration Minutes Box
          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(14.dp)),
            color = Color(0xFF0A0F1D)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "MINS",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                color = Color(0xFF94A3B8)
              )
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = minsInput,
                onValueChange = { raw ->
                  val (clean, parsed) = sanitizeNumberInput(raw, 2)
                  minsInput = clean
                  onDurationMinsChange(parsed.coerceIn(0, 59))
                },
                suffix = { Text("m", color = Color(0xFF64748B), fontSize = 11.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("duration_mins_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = Color.Transparent,
                  unfocusedContainerColor = Color.Transparent,
                  focusedBorderColor = accentColor,
                  unfocusedBorderColor = Color.Transparent,
                  focusedTextColor = Color(0xFFF8FAFC),
                  unfocusedTextColor = Color(0xFFF8FAFC)
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Dynamic Duration Preview Pill
        val currentTotalMins = durationHours * 60 + durationMins
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "DURATION PREVIEW",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B)
          )
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(accentColor.copy(alpha = 0.12f))
              .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Text(
              text = "${durationHours}h ${durationMins}m ($currentTotalMins mins total)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              color = accentLight
            )
          }
        }
      }

      // Slot 1 Informational Note Regarding Bedtime Calculation & Sleep Duration Numeric Box
      if (isSlot1) {
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0F1D))
            .border(BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f)), RoundedCornerShape(16.dp)),
          color = Color.Transparent
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Bedtime,
                contentDescription = null,
                tint = Color(0xFFA5B4FC),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "REVERSE BEDTIME CALCULATION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFA5B4FC)
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Bedtime is mathematically computed backwards from Slot 2 wake time minus your Sleep Duration.",
                fontSize = 11.sp,
                color = Color(0xFFE0E7FF),
                lineHeight = 16.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Side-by-Side Numeric Inputs for Sleep Duration
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(14.dp)),
            color = Color(0xFF0A0F1D)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "SLEEP HOURS",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                color = Color(0xFF94A3B8)
              )
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = hoursInput,
                onValueChange = { raw ->
                  val (clean, parsed) = sanitizeNumberInput(raw, 2)
                  hoursInput = clean
                  onDurationHoursChange(parsed.coerceIn(0, 12))
                },
                suffix = { Text("h", color = Color(0xFF64748B), fontSize = 11.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("duration_hours_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = Color.Transparent,
                  unfocusedContainerColor = Color.Transparent,
                  focusedBorderColor = Color(0xFF6366F1),
                  unfocusedBorderColor = Color.Transparent,
                  focusedTextColor = Color(0xFFF8FAFC),
                  unfocusedTextColor = Color(0xFFF8FAFC)
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
              )
            }
          }

          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(14.dp)),
            color = Color(0xFF0A0F1D)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "SLEEP MINS",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
                color = Color(0xFF94A3B8)
              )
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = minsInput,
                onValueChange = { raw ->
                  val (clean, parsed) = sanitizeNumberInput(raw, 2)
                  minsInput = clean
                  onDurationMinsChange(parsed.coerceIn(0, 59))
                },
                suffix = { Text("m", color = Color(0xFF64748B), fontSize = 11.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("duration_mins_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = Color.Transparent,
                  unfocusedContainerColor = Color.Transparent,
                  focusedBorderColor = Color(0xFF6366F1),
                  unfocusedBorderColor = Color.Transparent,
                  focusedTextColor = Color(0xFFF8FAFC),
                  unfocusedTextColor = Color(0xFFF8FAFC)
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val totalSleepMins = durationHours * 60 + durationMins
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "TARGET SLEEP QUOTA",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF64748B)
          )
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF6366F1).copy(alpha = 0.15f))
              .border(BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f)), RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Text(
              text = "${durationHours}h ${durationMins}m ($totalSleepMins mins)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              color = Color(0xFFA5B4FC)
            )
          }
        }
      }

      // Slot 5 Informational Note Regarding 100% Elastic Spanning
      if (isSlot5) {
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0F1D))
            .border(BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)), RoundedCornerShape(16.dp)),
          color = Color.Transparent
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "ACCORDION BUFFER GUARANTEE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFF59E0B)
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Slot 5 Start Time and Duration are locked. The engine automatically spans 100% of the duration between Slot 4 completion and Slot 6 commencement on active days.",
                fontSize = 11.sp,
                color = Color(0xFFFEF3C7),
                lineHeight = 16.sp
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Voice Message Announcement Card with inline TTS preview
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
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .clip(CircleShape)
                  .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                  contentDescription = null,
                  tint = accentColor,
                  modifier = Modifier.size(15.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "VOICE ANNOUNCEMENT",
                color = Color(0xFFE2E8F0),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
              )
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
              modifier = Modifier.testTag("anchor_voice_switch")
            )
          }

          if (isVoiceEnabled) {
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
              value = voiceMessage,
              onValueChange = onVoiceMessageChange,
              placeholder = { Text("কাস্টম ভয়েস মেসেজ লিখুন...", color = Color(0xFF475569)) },
              trailingIcon = {
                IconButton(onClick = onTestVoice, modifier = Modifier.testTag("test_voice_button")) {
                  Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Test Voice",
                    tint = accentLight
                  )
                }
              },
              modifier = Modifier.fillMaxWidth().testTag("voice_message_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F172A),
                unfocusedContainerColor = Color(0xFF0F172A),
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color(0xFF1E2C45),
                focusedTextColor = Color(0xFFF8FAFC),
                unfocusedTextColor = Color(0xFFF8FAFC)
              ),
              shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Volume Slider
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
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                  thumbColor = accentColor,
                  activeTrackColor = accentColor,
                  inactiveTrackColor = Color(0xFF1E2C45)
                )
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
          } else {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Spoken reminders disabled for this anchor",
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              color = Color(0xFF64748B)
            )
          }
        }
      }
    }

    // ==============================================================
    // 2. 2-COLUMN HARDWARE QUICK ACTION GRID (ALL ANCHORS)
    // ==============================================================
    Spacer(modifier = Modifier.height(14.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Column 1: Task Alerts Card
      Surface(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(16.dp))
          .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(16.dp)),
        color = Color(0xFF0A0F1D)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
              modifier = Modifier.testTag("anchor_sound_switch")
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "TASK ALERTS",
            color = Color(0xFFE2E8F0),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
          )

          Spacer(modifier = Modifier.height(6.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onPickRingtone() }
                .testTag("anchor_pick_ringtone_button"),
              color = Color(0xFF0F172A)
            ) {
              Text(
                text = customRingtoneTitle.take(16),
                color = Color(0xFF06B6D4),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
              )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
              onClick = onToggleTonePreview,
              modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                  if (isPlayingTonePreview) Color(0xFFEF4444).copy(alpha = 0.2f)
                  else Color(0xFF06B6D4).copy(alpha = 0.15f)
                )
                .border(
                  BorderStroke(
                    1.dp,
                    if (isPlayingTonePreview) Color(0xFFEF4444) else Color(0xFF06B6D4)
                  ),
                  RoundedCornerShape(8.dp)
                )
                .testTag("anchor_preview_tone_button")
            ) {
              Icon(
                imageVector = if (isPlayingTonePreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPlayingTonePreview) "Stop Preview" else "Play Preview",
                tint = if (isPlayingTonePreview) Color(0xFFEF4444) else Color(0xFF06B6D4),
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }

      // Column 2: Auto DND Card
      Surface(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(16.dp))
          .border(BorderStroke(1.dp, Color(0xFF1E2C45)), RoundedCornerShape(16.dp)),
        color = Color(0xFF0A0F1D)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.DoNotDisturbOn,
                contentDescription = null,
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.size(16.dp)
              )
            }
            Switch(
              checked = enableDnd,
              onCheckedChange = onEnableDndChange,
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF8B5CF6),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E2C45)
              ),
              modifier = Modifier.testTag("anchor_dnd_switch")
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "AUTO DND",
            color = Color(0xFFE2E8F0),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
          )

          Spacer(modifier = Modifier.height(4.dp))

          if (isSlot1 || isSlot5) {
            Text(
              text = "Locked: 0m (Immediate)",
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              color = Color(0xFF94A3B8)
            )
          } else if (enableDnd) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "DELAY",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFC4B5FD)
              )

              var delayInputText by remember(dndDelayMinutes) { mutableStateOf(dndDelayMinutes.toString()) }
              OutlinedTextField(
                value = delayInputText,
                onValueChange = { raw ->
                  val (clean, parsed) = sanitizeNumberInput(raw, 2)
                  delayInputText = clean
                  onDndDelayMinutesChange(parsed.coerceIn(0, 25))
                },
                modifier = Modifier
                  .width(68.dp)
                  .testTag("anchor_dnd_delay_input"),
                suffix = {
                  Text(
                    text = "m",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5CF6)
                  )
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFFF8FAFC),
                  textAlign = TextAlign.Center
                ),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = Color(0xFF0F172A),
                  unfocusedContainerColor = Color(0xFF0F172A),
                  focusedBorderColor = Color(0xFF8B5CF6),
                  unfocusedBorderColor = Color(0xFF1E2C45),
                  focusedTextColor = Color(0xFFF8FAFC),
                  unfocusedTextColor = Color(0xFFF8FAFC)
                ),
                shape = RoundedCornerShape(8.dp)
              )
            }
          } else {
            Text(
              text = "Alerts allowed",
              fontSize = 10.sp,
              color = Color(0xFF64748B)
            )
          }
        }
      }
    }

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

/**
 * Dual Session Card for Fluid Twin Sessions.
 */
@Composable
private fun DualSessionCard(
  sessionNumber: Int,
  accentColor: Color,
  title: String,
  onTitleChange: (String) -> Unit,
  startHour: Int,
  startMinute: Int,
  onOpenClock: () -> Unit,
  durationHours: Int,
  durationMins: Int,
  onDurationChange: (h: Int, m: Int) -> Unit,
  safetyFloor: Int,
  onSafetyFloorChange: (Int) -> Unit,
  voiceMessage: String,
  onVoiceMessageChange: (String) -> Unit,
  onTestVoice: () -> Unit
) {
  var hoursInput by remember(durationHours) { mutableStateOf(durationHours.toString()) }
  var minsInput by remember(durationMins) { mutableStateOf(durationMins.toString()) }
  var safetyFloorInput by remember(safetyFloor) { mutableStateOf(safetyFloor.toString()) }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)), RoundedCornerShape(18.dp)),
    color = Color(0xFF121620)
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

        Text(
          text = "${durationHours}h ${durationMins}m",
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
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = Color(0xFF0F141E),
          unfocusedContainerColor = Color(0xFF0F141E),
          focusedBorderColor = accentColor,
          unfocusedBorderColor = Color(0xFF1F2637),
          focusedTextColor = Color(0xFFF8FAFC),
          unfocusedTextColor = Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Start Time Row
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(12.dp))
          .clickable { onOpenClock() },
        color = Color(0xFF0F141E)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Start: ${RoutineTask.formatTime(startHour * 60 + startMinute)}",
              color = Color(0xFFF8FAFC),
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
          Text(text = "Change 🕒", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Duration & Safety Floor Inputs
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedTextField(
          value = hoursInput,
          onValueChange = { raw ->
            val (clean, parsed) = sanitizeNumberInput(raw, 2)
            hoursInput = clean
            onDurationChange(parsed.coerceIn(0, 12), durationMins)
          },
          label = { Text("Hours") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.weight(1f),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF0F141E),
            unfocusedContainerColor = Color(0xFF0F141E),
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color(0xFF1F2637),
            focusedTextColor = Color(0xFFF8FAFC),
            unfocusedTextColor = Color(0xFFF8FAFC)
          ),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        OutlinedTextField(
          value = minsInput,
          onValueChange = { raw ->
            val (clean, parsed) = sanitizeNumberInput(raw, 2)
            minsInput = clean
            onDurationChange(durationHours, parsed.coerceIn(0, 59))
          },
          label = { Text("Minutes") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.weight(1f),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF0F141E),
            unfocusedContainerColor = Color(0xFF0F141E),
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color(0xFF1F2637),
            focusedTextColor = Color(0xFFF8FAFC),
            unfocusedTextColor = Color(0xFFF8FAFC)
          ),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )

        OutlinedTextField(
          value = safetyFloorInput,
          onValueChange = { raw ->
            val (clean, parsed) = sanitizeNumberInput(raw, 3)
            safetyFloorInput = clean
            val floor = if (clean.isEmpty()) 45 else parsed.coerceIn(15, 120)
            onSafetyFloorChange(floor)
          },
          label = { Text("🛡️ Safety") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.weight(1.1f),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF0F141E),
            unfocusedContainerColor = Color(0xFF0F141E),
            focusedBorderColor = Color(0xFF10B981),
            unfocusedBorderColor = Color(0xFF1F2637),
            focusedTextColor = Color(0xFFF8FAFC),
            unfocusedTextColor = Color(0xFFF8FAFC)
          ),
          shape = RoundedCornerShape(12.dp),
          singleLine = true
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Voice Audio Sub-card with TTS test button
      OutlinedTextField(
        value = voiceMessage,
        onValueChange = onVoiceMessageChange,
        placeholder = { Text("Session $sessionNumber voice announcement", color = TextTertiary) },
        trailingIcon = {
          IconButton(onClick = onTestVoice) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Test Voice", tint = accentColor)
          }
        },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = Color(0xFF0F141E),
          unfocusedContainerColor = Color(0xFF0F141E),
          focusedBorderColor = accentColor,
          unfocusedBorderColor = Color(0xFF1F2637),
          focusedTextColor = Color(0xFFF8FAFC),
          unfocusedTextColor = Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(12.dp)
      )
    }
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

            // Rotating Clock Hand Canvas with Pointer Drag/Tap
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
                .pointerInput(isSelectingHour) {
                  detectTapGestures { offset ->
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val angleRad = atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble())
                    var deg = Math.toDegrees(angleRad).toFloat() + 90f
                    if (deg < 0) deg += 360f

                    if (isSelectingHour) {
                      val h = ((deg / 30f).roundToInt() % 12).let { if (it == 0) 12 else it }
                      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      selectedHour12 = h
                      hourInput = String.format(Locale.US, "%02d", h)
                      isSelectingHour = false // Automatically transition to minute
                      minuteFocusRequester.requestFocus()
                    } else {
                      val m = ((deg / 6f).roundToInt() % 60)
                      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                      selectedMinute = m
                      minuteInput = String.format(Locale.US, "%02d", m)
                    }
                  }
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
