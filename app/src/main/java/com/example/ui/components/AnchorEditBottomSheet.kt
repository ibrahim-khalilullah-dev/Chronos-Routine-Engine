package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Extension property to seamlessly access the 1-based slot index of an anchor task.
 */
val RoutineTask.slotIndex: Int
  get() = slotNumber ?: 1

private fun sanitizeAnchorNumberInput(input: String, maxDigits: Int = 2): Pair<String, Int> {
  val digitsOnly = input.filter { it.isDigit() }.take(maxDigits)
  val parsed = digitsOnly.toIntOrNull() ?: 0
  return Pair(digitsOnly, parsed)
}

/**
 * Chronos Precision Routine Engine: Anchor Edit Bottom Sheet (Luxury Obsidian Studio Overhaul).
 *
 * Strict Domain & Circadian Rules (Zero Functionality Loss):
 * - Slot 1 (Reverse Sleep Tracker): Start time completely hidden (reverse-computed from Slot 2 wake-up).
 *   Bedtime calculation explanation card. Sleep duration numeric [Hours] & [Minutes] inputs. DND delay locked to 0.
 * - Slot 3 (Dhuhr & Friday Jumu'ah Dual Mode): Modern capsule switcher between Regular Days (Sat-Thu) and Friday Mode.
 *   Full Friday mode configuration: Title, Start Time, Duration, Voice Announcement with TTS test.
 * - Slot 5 (Protected Elastic Buffer): Start time and Duration completely hidden and locked (engine dynamically
 *   spans 100% between Slot 4 completion and Slot 6 commencement). Recurrence selector with 7 circular day pills. DND delay locked to 0.
 * - Slots 2, 4, 6, 7 (Core Daily Pillars): Start time with clock picker. Side-by-side numeric [Hours] and [Minutes]
 *   inputs with live total duration preview pill. Delayed DND stepper (0-25m).
 * - Hardware Automations (All Anchors): 2-Column quick action grid for Task Alerts (ringtone picker + audio preview)
 *   and Auto DND. Voice Announcement Studio card with in-place TTS preview and smooth 0-100% speech volume slider.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnchorEditBottomSheet(
  anchor: RoutineTask,
  onDismiss: () -> Unit,
  onSaveAnchor: (
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
    isSoundAlertEnabled: Boolean
  ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
  onSaveSlot5: ((title: String, voiceMessage: String) -> Unit)? = null,
  onSave: ((RoutineTask) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val slotIdx = anchor.slotIndex
  val isSlot1 = slotIdx == 1
  val isSlot3 = slotIdx == 3
  val isSlot5 = slotIdx == 5

  // Slot-Specific Semantic Colors
  val accentColor = when (slotIdx) {
    1 -> Color(0xFF6366F1) // Deep Indigo
    3 -> Color(0xFF10B981) // Vibrant Emerald
    5 -> Color(0xFFF59E0B) // Amber
    else -> Color(0xFF06B6D4) // Cyan
  }
  val accentLight = when (slotIdx) {
    1 -> Color(0xFFA5B4FC) // Lavender
    3 -> Color(0xFF6EE7B7) // Mint
    5 -> Color(0xFFFEF3C7) // Warm Gold
    else -> Color(0xFF38BDF8) // Sky Blue
  }

  // Reliable Initial Snapshot Tracking for Unsaved Changes Guard
  val initialTitle = remember(anchor) { anchor.title }
  val initialDuration = remember(anchor) { anchor.durationMinutes }
  val initialStartMin = remember(anchor) { anchor.startTimeMinute }
  val initialVoice = remember(anchor) { anchor.voiceMessage }
  val initialDnd = remember(anchor) { anchor.enableDnd }
  val initialDndDelay = remember(anchor) { anchor.dndDelayMinutes }
  val initialSound = remember(anchor) { anchor.isSoundAlertEnabled }
  val initialHaptic = remember(anchor) { anchor.enableHaptic }
  val initialVoiceEnabled = remember(anchor) { anchor.isVoiceEnabled }
  val initialVoiceVolume = remember(anchor) { anchor.voiceVolume }
  val initialDays = remember(anchor) { anchor.selectedDays }
  val initialRingtoneUri = remember(anchor) { anchor.customRingtoneUri }
  val initialRingtoneTitle = remember(anchor) { anchor.customRingtoneTitle }
  val initialFridayTitle = remember(anchor) { anchor.fridayTitle }
  val initialFridayStartMin = remember(anchor) { anchor.fridayStartTimeMinute }
  val initialFridayDuration = remember(anchor) { anchor.fridayDurationMinutes }
  val initialFridayVoice = remember(anchor) { anchor.fridayVoiceMessage }

  // State Declarations
  var title by remember { mutableStateOf(anchor.title) }
  var startHour by remember { mutableIntStateOf(anchor.startTimeMinute / 60) }
  var startMinute by remember { mutableIntStateOf(anchor.startTimeMinute % 60) }

  var durationHours by remember { mutableIntStateOf(anchor.durationMinutes / 60) }
  var durationMins by remember { mutableIntStateOf(anchor.durationMinutes % 60) }

  var hoursInput by remember {
    mutableStateOf(if (anchor.durationMinutes / 60 > 0) (anchor.durationMinutes / 60).toString() else "")
  }
  var minsInput by remember {
    mutableStateOf(if (anchor.durationMinutes % 60 > 0) (anchor.durationMinutes % 60).toString() else "")
  }

  var voiceMessage by remember { mutableStateOf(anchor.voiceMessage) }
  var isDndEnabled by remember { mutableStateOf(anchor.enableDnd) }
  var dndDelayMinutes by remember { mutableIntStateOf(anchor.dndDelayMinutes) }
  var isSoundAlertEnabled by remember { mutableStateOf(anchor.isSoundAlertEnabled) }
  var enableHaptic by remember { mutableStateOf(anchor.enableHaptic) }
  var isVoiceEnabled by remember { mutableStateOf(anchor.isVoiceEnabled) }
  var voiceVolume by remember { mutableFloatStateOf(anchor.voiceVolume) }
  var selectedDays by remember { mutableStateOf(anchor.selectedDays) }
  var customRingtoneUri by remember { mutableStateOf(anchor.customRingtoneUri) }
  var customRingtoneTitle by remember { mutableStateOf(anchor.customRingtoneTitle ?: "Default Chime") }

  // Slot 3 Dual Mode State
  var activeSlot3Tab by remember { mutableIntStateOf(0) } // 0 = Regular Days, 1 = Friday Mode
  var fridayTitle by remember { mutableStateOf(anchor.fridayTitle ?: "Friday Jumu'ah & Reflection") }
  val defaultFridayStart = anchor.fridayStartTimeMinute ?: (12 * 60 + 45) // 12:45 PM default
  var fridayStartHour by remember { mutableIntStateOf(defaultFridayStart / 60) }
  var fridayStartMinute by remember { mutableIntStateOf(defaultFridayStart % 60) }
  val defaultFridayDuration = anchor.fridayDurationMinutes ?: 105 // 1h 45m
  var fridayDurationHours by remember { mutableIntStateOf(defaultFridayDuration / 60) }
  var fridayDurationMins by remember { mutableIntStateOf(defaultFridayDuration % 60) }
  var friHoursInput by remember {
    mutableStateOf((defaultFridayDuration / 60).toString())
  }
  var friMinsInput by remember {
    mutableStateOf((defaultFridayDuration % 60).toString())
  }
  var fridayVoiceMessage by remember {
    mutableStateOf(anchor.fridayVoiceMessage ?: "Friday Jumu'ah routine starting now.")
  }

  // Dialog Controls
  var showTimePickerDialog by remember { mutableStateOf(false) }
  var clockTarget by remember { mutableIntStateOf(0) } // 0 = regular, 1 = Friday
  var showUnsavedChangesDialog by remember { mutableStateOf(false) }

  // Ringtone Preview & Picker
  val context = LocalContext.current
  var isPlayingTonePreview by remember { mutableStateOf(false) }
  var tonePreviewPlayer by remember { mutableStateOf<Ringtone?>(null) }

  val ringtonePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
      } else {
        @Suppress("DEPRECATION")
        result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
      }
      customRingtoneUri = uri?.toString()
      customRingtoneTitle = if (uri != null) {
        RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Selected Sound"
      } else {
        "Default Chime"
      }
    }
  }

  val toggleTonePreview: () -> Unit = {
    if (isPlayingTonePreview) {
      tonePreviewPlayer?.stop()
      tonePreviewPlayer = null
      isPlayingTonePreview = false
    } else {
      try {
        val uri = customRingtoneUri?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone?.play()
        tonePreviewPlayer = ringtone
        isPlayingTonePreview = true
      } catch (_: Exception) {
        isPlayingTonePreview = false
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
        tonePreviewPlayer?.stop()
        instance?.stop()
        instance?.shutdown()
      } catch (_: Exception) {}
    }
  }

  val hasUnsavedChanges by remember {
    derivedStateOf {
      title != initialTitle ||
      (durationHours * 60 + durationMins) != initialDuration ||
      (startHour * 60 + startMinute) != initialStartMin ||
      voiceMessage != initialVoice ||
      isDndEnabled != initialDnd ||
      dndDelayMinutes != initialDndDelay ||
      isSoundAlertEnabled != initialSound ||
      enableHaptic != initialHaptic ||
      isVoiceEnabled != initialVoiceEnabled ||
      voiceVolume != initialVoiceVolume ||
      selectedDays != initialDays ||
      customRingtoneUri != initialRingtoneUri ||
      customRingtoneTitle != initialRingtoneTitle ||
      (isSlot3 && (
        fridayTitle != initialFridayTitle ||
        (fridayStartHour * 60 + fridayStartMinute) != initialFridayStartMin ||
        (fridayDurationHours * 60 + fridayDurationMins) != initialFridayDuration ||
        fridayVoiceMessage != initialFridayVoice
      ))
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

  // Unified save execution
  val executeSave: () -> Unit = {
    val finalTitle = title.ifBlank {
      when {
        isSlot1 -> "Reverse Sleep Tracker"
        isSlot5 -> "Elastic Rubber Buffer"
        else -> "Anchor Slot $slotIdx"
      }
    }
    val calculatedStart = startHour * 60 + startMinute
    val calculatedDuration = (durationHours * 60 + durationMins).coerceAtLeast(15)

    if (isSlot5) {
      if (onSaveSlot5 != null) {
        onSaveSlot5(finalTitle, voiceMessage)
      } else {
        onSaveAnchor(
          slotIdx,
          finalTitle,
          anchor.startTimeMinute,
          anchor.durationMinutes,
          voiceMessage,
          isDndEnabled,
          enableHaptic,
          selectedDays,
          isVoiceEnabled,
          anchor.customNotificationTitle,
          anchor.customNotificationBody,
          customRingtoneUri,
          customRingtoneTitle,
          isSoundAlertEnabled
        )
      }
    } else {
      onSaveAnchor(
        slotIdx,
        finalTitle,
        if (isSlot1) anchor.startTimeMinute else calculatedStart,
        calculatedDuration,
        voiceMessage,
        isDndEnabled,
        enableHaptic,
        selectedDays,
        isVoiceEnabled,
        anchor.customNotificationTitle,
        anchor.customNotificationBody,
        customRingtoneUri,
        customRingtoneTitle,
        isSoundAlertEnabled
      )
    }

    val updatedTask = anchor.copy(
      title = finalTitle,
      startTimeMinute = if (isSlot1 || isSlot5) anchor.startTimeMinute else calculatedStart,
      durationMinutes = if (isSlot5) anchor.durationMinutes else calculatedDuration,
      voiceMessage = voiceMessage,
      enableDnd = isDndEnabled,
      dndDelayMinutes = if (isSlot1 || isSlot5) 0 else dndDelayMinutes,
      enableHaptic = enableHaptic,
      isVoiceEnabled = isVoiceEnabled,
      voiceVolume = voiceVolume,
      isSoundAlertEnabled = isSoundAlertEnabled,
      customRingtoneUri = customRingtoneUri,
      customRingtoneTitle = customRingtoneTitle,
      selectedDays = selectedDays,
      fridayTitle = if (isSlot3) fridayTitle else anchor.fridayTitle,
      fridayStartTimeMinute = if (isSlot3) (fridayStartHour * 60 + fridayStartMinute) else anchor.fridayStartTimeMinute,
      fridayDurationMinutes = if (isSlot3) (fridayDurationHours * 60 + fridayDurationMins) else anchor.fridayDurationMinutes,
      fridayVoiceMessage = if (isSlot3) fridayVoiceMessage else anchor.fridayVoiceMessage
    )
    onSave?.invoke(updatedTask)

    onDismiss()
  }

  // Consume nested scrolling to prevent any sheet collapse or swipe-to-dismiss
  val consumeNestedScrollConnection = remember {
    object : NestedScrollConnection {
      override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
      override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
      override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero
      override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
    }
  }

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = { handleDismissRequest() },
    sheetState = sheetState,
    sheetGesturesEnabled = false,
    dragHandle = null,
    containerColor = Color(0xFF080A0F),
    scrimColor = Color.Black.copy(alpha = 0.75f),
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    modifier = modifier.testTag("anchor_edit_bottom_sheet")
  ) {
    @OptIn(ExperimentalFoundationApi::class)
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .nestedScroll(consumeNestedScrollConnection)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 8.dp)
      ) {
        // Plain, static visual indicator pill
        Box(
          modifier = Modifier
            .padding(vertical = 12.dp)
            .size(width = 44.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .align(Alignment.CenterHorizontally)
        )

        // Header Section
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.15f))
                .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)), RoundedCornerShape(12.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = when {
                  isSlot1 -> Icons.Default.Bedtime
                  isSlot5 -> Icons.Default.Shield
                  else -> Icons.Default.Lock
                },
                contentDescription = null,
                tint = accentLight,
                modifier = Modifier.size(22.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Text(
                text = when {
                  isSlot1 -> "Slot 1: Reverse Sleep Tracker"
                  isSlot5 -> "Slot 5: Elastic Rubber Buffer"
                  else -> "Anchor Slot $slotIdx"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8FAFC)
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

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(accentColor.copy(alpha = 0.15f))
                .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "ANCHOR",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = accentLight
              )
            }

            IconButton(
              onClick = handleDismissRequest,
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F172A))
                .border(BorderStroke(1.dp, Color(0xFF1E2C45)), CircleShape)
                .testTag("dismiss_anchor_modal_button")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ==============================================================
        // 1. SLOT 3 CAPSULE DUAL MODE SWITCHER (SAT-THU vs FRIDAY MODE)
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
                  .clickable { activeSlot3Tab = 0 }
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
                  .clickable { activeSlot3Tab = 1 }
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
        // 2. FRIDAY MODE FORM vs REGULAR ANCHOR FORM
        // ==============================================================
        if (isSlot3 && activeSlot3Tab == 1) {
          // Friday Anchor Title
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
            onValueChange = { fridayTitle = it },
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
                .clickable {
                  clockTarget = 1
                  showTimePickerDialog = true
                }
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
                    val (clean, parsed) = sanitizeAnchorNumberInput(raw, 2)
                    friHoursInput = clean
                    fridayDurationHours = parsed.coerceIn(0, 12)
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
                    val (clean, parsed) = sanitizeAnchorNumberInput(raw, 2)
                    friMinsInput = clean
                    fridayDurationMins = parsed.coerceIn(0, 59)
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
                  onCheckedChange = { isVoiceEnabled = it },
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
                  onValueChange = { fridayVoiceMessage = it },
                  placeholder = { Text("কাস্টম ভয়েস মেসেজ লিখুন...", color = Color(0xFF475569)) },
                  trailingIcon = {
                    IconButton(
                      onClick = {
                        val textToSpeak = fridayVoiceMessage.ifBlank {
                          "${fridayTitle.ifBlank { "Friday anchor" }} starting now."
                        }
                        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceVolume.coerceIn(0f, 1f)) }
                        ttsInstance?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "friday_tts_preview")
                      },
                      modifier = Modifier.testTag("test_friday_voice_button")
                    ) {
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
          // ==============================================================
          // REGULAR ANCHOR FORM (SLOTS 1, 2, 3 REGULAR, 4, 5, 6, 7)
          // ==============================================================
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
                text = "SLOT $slotIdx",
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
            onValueChange = { title = it },
            placeholder = { Text("e.g. Core Anchor Slot $slotIdx", color = Color(0xFF475569)) },
            modifier = Modifier.fillMaxWidth().testTag("anchor_title_input"),
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

          // ==============================================================
          // START TIME SECTION (Rule 1 & Rule 2: Completely hidden for Slot 1 and 5)
          // ==============================================================
          if (!isSlot1 && !isSlot5) {
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
                    showTimePickerDialog = true
                  }
                  .testTag("open_anchor_time_picker_button"),
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
                    value = hoursInput,
                    onValueChange = { raw ->
                      val (clean, parsed) = sanitizeAnchorNumberInput(raw, 2)
                      hoursInput = clean
                      durationHours = parsed.coerceIn(0, 12)
                    },
                    suffix = { Text("h", color = Color(0xFF64748B), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("anchor_duration_hours_input"),
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
                    value = minsInput,
                    onValueChange = { raw ->
                      val (clean, parsed) = sanitizeAnchorNumberInput(raw, 2)
                      minsInput = clean
                      durationMins = parsed.coerceIn(0, 59)
                    },
                    suffix = { Text("m", color = Color(0xFF64748B), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("anchor_duration_minutes_input"),
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
                      val (clean, parsed) = sanitizeAnchorNumberInput(raw, 2)
                      hoursInput = clean
                      durationHours = parsed.coerceIn(0, 12)
                    },
                    suffix = { Text("h", color = Color(0xFF64748B), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("anchor_duration_hours_input"),
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
                      val (clean, parsed) = sanitizeAnchorNumberInput(raw, 2)
                      minsInput = clean
                      durationMins = parsed.coerceIn(0, 59)
                    },
                    suffix = { Text("m", color = Color(0xFF64748B), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("anchor_duration_minutes_input"),
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
                  onCheckedChange = { isVoiceEnabled = it },
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
                  onValueChange = { voiceMessage = it },
                  placeholder = { Text("কাস্টম ভয়েস মেসেজ লিখুন...", color = Color(0xFF475569)) },
                  trailingIcon = {
                    IconButton(
                      onClick = {
                        val textToSpeak = voiceMessage.ifBlank {
                          "${title.ifBlank { "Anchor slot $slotIdx" }} starting now."
                        }
                        val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, voiceVolume.coerceIn(0f, 1f)) }
                        ttsInstance?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "anchor_voice_preview")
                      },
                      modifier = Modifier.testTag("preview_voice_tts_button")
                    ) {
                      Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Preview voice message",
                        tint = accentLight
                      )
                    }
                  },
                  modifier = Modifier.fillMaxWidth().testTag("anchor_voice_message_input"),
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
                    onValueChange = { voiceVolume = it },
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
        // 3. 2-COLUMN HARDWARE QUICK ACTION GRID (ALL ANCHORS)
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
                  onCheckedChange = { isSoundAlertEnabled = it },
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
                    .clickable {
                      val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Anchor Alert Sound")
                        customRingtoneUri?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it)) }
                      }
                      ringtonePickerLauncher.launch(intent)
                    }
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
                  onClick = toggleTonePreview,
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
                  checked = isDndEnabled,
                  onCheckedChange = { isDndEnabled = it },
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
              } else if (isDndEnabled) {
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
                      val (clean, parsed) = sanitizeAnchorNumberInput(raw, 2)
                      delayInputText = clean
                      dndDelayMinutes = parsed.coerceIn(0, 25)
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
        // 4. SLOT 5 (ELASTIC BUFFER) WEEKLY RECURRENCE SELECTOR
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
                        selectedDays = RoutineDay.allDays()
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
                          selectedDays = setOf(RoutineDay.SATURDAY, RoutineDay.SUNDAY, RoutineDay.MONDAY, RoutineDay.TUESDAY, RoutineDay.WEDNESDAY, RoutineDay.THURSDAY)
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

        Spacer(modifier = Modifier.height(20.dp))

        // ==============================================================
        // 6. LUXURY GRADIENT SAVE CTA BUTTON
        // ==============================================================
        Button(
          onClick = { executeSave() },
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("save_anchor_button"),
          colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
          shape = RoundedCornerShape(16.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF2563EB))),
                shape = RoundedCornerShape(16.dp)
              ),
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = "Save & Apply Schedule", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }
        }

        Spacer(modifier = Modifier.height(28.dp))
      }
    }
  }

  // Interactive TimePicker Dialog for Anchors
  if (showTimePickerDialog) {
    val initialHour = if (clockTarget == 1) fridayStartHour else startHour
    val initialMin = if (clockTarget == 1) fridayStartMinute else startMinute
    val timePickerState = rememberTimePickerState(
      initialHour = initialHour,
      initialMinute = initialMin,
      is24Hour = false
    )

    AlertDialog(
      onDismissRequest = { showTimePickerDialog = false },
      title = {
        Text(
          text = if (clockTarget == 1) "Select Friday Jumu'ah Start Time" else "Select Anchor Start Time",
          color = Color(0xFFF8FAFC),
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        )
      },
      text = {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          TimePicker(
            state = timePickerState,
            colors = TimePickerDefaults.colors(
              clockDialColor = Color(0xFF0F172A),
              selectorColor = if (clockTarget == 1) Color(0xFF10B981) else Color(0xFF06B6D4),
              timeSelectorSelectedContainerColor = if (clockTarget == 1) Color(0xFF10B981) else Color(0xFF06B6D4),
              timeSelectorUnselectedContainerColor = Color(0xFF0F172A),
              timeSelectorSelectedContentColor = Color.White,
              timeSelectorUnselectedContentColor = Color(0xFFF8FAFC)
            )
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (clockTarget == 1) {
              fridayStartHour = timePickerState.hour
              fridayStartMinute = timePickerState.minute
            } else {
              startHour = timePickerState.hour
              startMinute = timePickerState.minute
            }
            showTimePickerDialog = false
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = if (clockTarget == 1) Color(0xFF10B981) else Color(0xFF06B6D4),
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Set Time", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showTimePickerDialog = false }) {
          Text("Cancel", color = Color(0xFF94A3B8))
        }
      },
      containerColor = Color(0xFF0A0F1D),
      shape = RoundedCornerShape(20.dp)
    )
  }

  // In-Place Unsaved Changes Dismissal Guard Dialog
  if (showUnsavedChangesDialog) {
    AlertDialog(
      onDismissRequest = { showUnsavedChangesDialog = false },
      containerColor = Color(0xFF0A0F1D),
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
          text = "You have unsaved edits in this anchor slot. What would you like to do?",
          fontSize = 13.sp,
          color = Color(0xFF94A3B8),
          lineHeight = 18.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showUnsavedChangesDialog = false
            executeSave()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("dialog_anchor_save_and_apply_button")
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
            modifier = Modifier.testTag("dialog_anchor_discard_button")
          ) {
            Text("Discard", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }

          // Keep Editing (Neutral)
          TextButton(
            onClick = { showUnsavedChangesDialog = false },
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF94A3B8)),
            modifier = Modifier.testTag("dialog_anchor_keep_editing_button")
          ) {
            Text("Keep Editing", fontSize = 12.sp)
          }
        }
      }
    )
  }
}
