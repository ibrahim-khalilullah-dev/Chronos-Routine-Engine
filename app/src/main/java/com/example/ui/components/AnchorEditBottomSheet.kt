package com.example.ui.components

import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.ui.theme.AnchorGradientStart
import com.example.ui.theme.OneUiBackground
import com.example.ui.theme.OneUiBorder
import com.example.ui.theme.OneUiSurfaceElevated
import com.example.ui.theme.OneUiSurfaceHigh
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber
import java.util.Locale

/**
 * Extension property to seamlessly access the 1-based slot index of an anchor task.
 */
val RoutineTask.slotIndex: Int
  get() = slotNumber ?: 1

/**
 * Phase 2: Dedicated Anchor Edit Bottom Sheet with strict slot-specific architectural rules.
 *
 * Rules:
 * - Slot 1 (Reverse Sleep Tracker): Start time hidden (reverse-computed from Slot 2 wake-up).
 *   Duration slider and chips removed; only numeric [ Hours ] and [ Minutes ] retained for Sleep Duration.
 * - Slot 5 (Protected Elastic Buffer): Start time and Duration sections completely removed
 *   (auto-spans 100% between Slot 4 completion and Slot 6 start).
 * - Slots 2, 3, 4, 6, 7 (Standard Fixed Anchors): Start time retained with clock picker.
 *   Duration slider and preset chips removed; only side-by-side numeric [ Hours ] and [ Minutes ] retained.
 * - Inline TTS Voice Preview: In the Voice Announcement card, a compact speaker icon allows in-place testing.
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
  val isSlot5 = slotIdx == 5

  // Reliable Initial Snapshot Tracking
  val initialTitle = remember(anchor) { anchor.title }
  val initialDuration = remember(anchor) { anchor.durationMinutes }
  val initialStartMin = remember(anchor) { anchor.startTimeMinute }
  val initialVoice = remember(anchor) { anchor.voiceMessage }
  val initialDnd = remember(anchor) { anchor.enableDnd }
  val initialSound = remember(anchor) { anchor.isSoundAlertEnabled }
  val initialHaptic = remember(anchor) { anchor.enableHaptic }
  val initialDays = remember(anchor) { anchor.selectedDays }

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
  var isSoundAlertEnabled by remember { mutableStateOf(anchor.isSoundAlertEnabled) }
  var enableHaptic by remember { mutableStateOf(anchor.enableHaptic) }
  var isVoiceEnabled by remember { mutableStateOf(anchor.isVoiceEnabled) }
  var selectedDays by remember { mutableStateOf(anchor.selectedDays) }

  var showTimePickerDialog by remember { mutableStateOf(false) }
  var showUnsavedChangesDialog by remember { mutableStateOf(false) }

  val hasUnsavedChanges by remember {
    derivedStateOf {
      title != initialTitle ||
      (durationHours * 60 + durationMins) != initialDuration ||
      (startHour * 60 + startMinute) != initialStartMin ||
      voiceMessage != initialVoice ||
      isDndEnabled != initialDnd ||
      isSoundAlertEnabled != initialSound ||
      enableHaptic != initialHaptic ||
      selectedDays != initialDays
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
          anchor.customRingtoneUri,
          anchor.customRingtoneTitle,
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
        anchor.customRingtoneUri,
        anchor.customRingtoneTitle,
        isSoundAlertEnabled
      )
    }

    onSave?.invoke(
      anchor.copy(
        title = finalTitle,
        startTimeMinute = if (isSlot1 || isSlot5) anchor.startTimeMinute else calculatedStart,
        durationMinutes = if (isSlot5) anchor.durationMinutes else calculatedDuration,
        voiceMessage = voiceMessage,
        enableDnd = isDndEnabled,
        enableHaptic = enableHaptic,
        isVoiceEnabled = isVoiceEnabled,
        isSoundAlertEnabled = isSoundAlertEnabled,
        selectedDays = selectedDays
      )
    )

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

  // Context & In-Place TTS Engine Setup
  val context = LocalContext.current
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
    }
  }

  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true
  )

  ModalBottomSheet(
    onDismissRequest = { handleDismissRequest() },
    sheetState = sheetState,
    sheetGesturesEnabled = false, // CRITICAL: Completely disables drag & swipe-to-dismiss gestures
    dragHandle = null,             // Eliminates the gesture-consuming default drag handle
    containerColor = OneUiBackground,
    scrimColor = Color.Black.copy(alpha = 0.7f),
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
        // Plain, static, non-draggable visual indicator pill
        Box(
          modifier = Modifier
            .padding(vertical = 12.dp)
            .size(width = 44.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.25f))
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
              .size(40.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(AnchorGradientStart.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = when {
                isSlot1 -> Icons.Default.Bedtime
                isSlot5 -> Icons.Default.Shield
                else -> Icons.Default.Lock
              },
              contentDescription = null,
              tint = Color(0xFFA5B4FC),
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
              color = TextPrimary
            )
            Text(
              text = when {
                isSlot1 -> "Reverse mathematics • Bedtime computed from Slot 2"
                isSlot5 -> "Dynamically spans 100% between Slot 4 & Slot 6"
                else -> "Core Daily Pillar • Protected from dynamic override"
              },
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }

        IconButton(
          onClick = handleDismissRequest,
          modifier = Modifier.size(36.dp).testTag("dismiss_anchor_modal_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = TextSecondary
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Anchor Title Field
      Text(
        text = "Anchor Title",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
      )
      Spacer(modifier = Modifier.height(6.dp))

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        placeholder = { Text("Anchor name", color = TextTertiary) },
        modifier = Modifier.fillMaxWidth().testTag("anchor_title_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = OneUiSurfaceElevated,
          unfocusedContainerColor = OneUiSurfaceElevated,
          focusedBorderColor = AnchorGradientStart,
          unfocusedBorderColor = OneUiBorder,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
      )

      // ========================================================================
      // START TIME SECTION (Rule 1 & Rule 2: Completely hidden for Slot 1 and 5)
      // ========================================================================
      if (!isSlot1 && !isSlot5) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Anchor Start Time",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(16.dp))
            .clickable { showTimePickerDialog = true }
            .testTag("open_anchor_time_picker_button"),
          color = OneUiSurfaceElevated
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = AnchorGradientStart,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = RoutineTask.formatTime(startHour * 60 + startMinute),
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = "24h: ${String.format("%02d:%02d", startHour, startMinute)} • Tap to open clock picker",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(AnchorGradientStart.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text(
                text = "Change 🕒",
                color = Color(0xFFA5B4FC),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // Slot 1 informational note regarding bedtime calculation
      if (isSlot1) {
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E1B4B).copy(alpha = 0.4f))
            .border(BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.3f)), RoundedCornerShape(14.dp)),
          color = Color.Transparent
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Bedtime,
              contentDescription = null,
              tint = Color(0xFFA5B4FC),
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Bedtime is mathematically computed backwards from Slot 2 wake time minus your Sleep Duration.",
              fontSize = 11.sp,
              color = Color(0xFFE0E7FF),
              lineHeight = 16.sp
            )
          }
        }
      }

      // Slot 5 informational note regarding 100% elastic spanning
      if (isSlot5) {
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WarningAmber.copy(alpha = 0.1f))
            .border(BorderStroke(1.dp, WarningAmber.copy(alpha = 0.3f)), RoundedCornerShape(14.dp)),
          color = Color.Transparent
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = WarningAmber,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Slot 5 Start Time and Duration are locked. The engine automatically spans 100% of the duration between Slot 4 completion and Slot 6 commencement.",
              fontSize = 11.sp,
              color = Color(0xFFFEF3C7),
              lineHeight = 16.sp
            )
          }
        }
      }

      // ========================================================================
      // DURATION SECTION (Rule 1, 2, 3: Hidden for Slot 5; Only numeric [H] & [M] inputs for others)
      // ========================================================================
      if (!isSlot5) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = if (isSlot1) "Sleep Duration" else "Duration",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(16.dp)),
          color = OneUiSurfaceElevated
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            // Live Readout Bar
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.AccessTime,
                  contentDescription = null,
                  tint = if (isSlot1) Color(0xFFA5B4FC) else AnchorGradientStart,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val totalMins = durationHours * 60 + durationMins
                Text(
                  text = "${durationHours}h ${durationMins}m ($totalMins mins)",
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Side-by-Side Numeric Input Fields for [ Hours ] and [ Minutes ]
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedTextField(
                value = hoursInput,
                onValueChange = { input ->
                  val clean = input.filter { it.isDigit() }.take(2)
                  hoursInput = clean
                  durationHours = clean.toIntOrNull()?.coerceIn(0, 23) ?: 0
                },
                label = { Text("Hours", fontSize = 11.sp) },
                placeholder = { Text("0", color = TextTertiary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("anchor_duration_hours_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = OneUiSurfaceHigh,
                  unfocusedContainerColor = OneUiSurfaceHigh,
                  focusedBorderColor = AnchorGradientStart,
                  unfocusedBorderColor = OneUiBorder,
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
              )

              OutlinedTextField(
                value = minsInput,
                onValueChange = { input ->
                  val clean = input.filter { it.isDigit() }.take(2)
                  minsInput = clean
                  durationMins = clean.toIntOrNull()?.coerceIn(0, 59) ?: 0
                },
                label = { Text("Minutes", fontSize = 11.sp) },
                placeholder = { Text("0", color = TextTertiary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("anchor_duration_minutes_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = OneUiSurfaceHigh,
                  unfocusedContainerColor = OneUiSurfaceHigh,
                  focusedBorderColor = AnchorGradientStart,
                  unfocusedBorderColor = OneUiBorder,
                  focusedTextColor = TextPrimary,
                  unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // ========================================================================
      // VOICE MESSAGE ANNOUNCEMENT WITH INLINE TTS PREVIEW (Rule 4)
      // ========================================================================
      Text(
        text = "Voice Message Announcement",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
      )
      Spacer(modifier = Modifier.height(6.dp))

      OutlinedTextField(
        value = voiceMessage,
        onValueChange = { voiceMessage = it },
        placeholder = { Text("e.g., Anchor starting now. Prepare your workspace.", color = TextTertiary) },
        trailingIcon = {
          IconButton(
            onClick = {
              val textToSpeak = voiceMessage.ifBlank {
                "${title.ifBlank { "Anchor slot $slotIdx" }} starting now."
              }
              try {
                ttsInstance?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "anchor_voice_preview")
              } catch (_: Exception) {}
            },
            modifier = Modifier.testTag("preview_voice_tts_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = "Preview voice message",
              tint = Color(0xFF67E8F9),
              modifier = Modifier.size(22.dp)
            )
          }
        },
        modifier = Modifier.fillMaxWidth().testTag("anchor_voice_message_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = OneUiSurfaceElevated,
          unfocusedContainerColor = OneUiSurfaceElevated,
          focusedBorderColor = AnchorGradientStart,
          unfocusedBorderColor = OneUiBorder,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(14.dp),
        maxLines = 2
      )

      Spacer(modifier = Modifier.height(16.dp))

      // ========================================================================
      // HARDWARE CONTROLS: DND & SOUND ALERT SWITCHES
      // ========================================================================
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(16.dp)),
        color = OneUiSurfaceElevated
      ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
          // DND Mode Switch
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.DoNotDisturbOn,
                contentDescription = null,
                tint = if (isDndEnabled) Color(0xFFA5B4FC) else TextSecondary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Do Not Disturb (DND)",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextPrimary
                )
                Text(
                  text = "Enforces priority silence during anchor window",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }

            Switch(
              checked = isDndEnabled,
              onCheckedChange = { isDndEnabled = it },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AnchorGradientStart,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = OneUiSurfaceHigh
              ),
              modifier = Modifier.testTag("anchor_dnd_switch")
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Sound Alert Switch
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (isSoundAlertEnabled) Color(0xFF10B981) else TextSecondary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "Sound Alert",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextPrimary
                )
                Text(
                  text = "Play custom chime when anchor begins",
                  fontSize = 11.sp,
                  color = TextSecondary
                )
              }
            }

            Switch(
              checked = isSoundAlertEnabled,
              onCheckedChange = { isSoundAlertEnabled = it },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF10B981),
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = OneUiSurfaceHigh
              ),
              modifier = Modifier.testTag("anchor_sound_switch")
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // ========================================================================
      // SAVE & APPLY BUTTON
      // ========================================================================
      Button(
        onClick = { executeSave() },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("save_anchor_button"),
        colors = ButtonDefaults.buttonColors(
          containerColor = AnchorGradientStart,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp)
      ) {
        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Save & Apply Schedule", fontSize = 15.sp, fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.height(28.dp))
    }
  }
}

  // Interactive TimePicker Dialog for Anchors
  if (showTimePickerDialog) {
    val timePickerState = rememberTimePickerState(
      initialHour = startHour,
      initialMinute = startMinute,
      is24Hour = false
    )

    AlertDialog(
      onDismissRequest = { showTimePickerDialog = false },
      title = {
        Text("Select Anchor Start Time", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
      },
      text = {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          TimePicker(
            state = timePickerState,
            colors = TimePickerDefaults.colors(
              clockDialColor = OneUiSurfaceElevated,
              selectorColor = AnchorGradientStart,
              timeSelectorSelectedContainerColor = AnchorGradientStart,
              timeSelectorUnselectedContainerColor = OneUiSurfaceElevated,
              timeSelectorSelectedContentColor = Color.White,
              timeSelectorUnselectedContentColor = TextPrimary
            )
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            startHour = timePickerState.hour
            startMinute = timePickerState.minute
            showTimePickerDialog = false
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = AnchorGradientStart,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Set Time", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showTimePickerDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      },
      containerColor = OneUiBackground,
      shape = RoundedCornerShape(20.dp)
    )
  }

  // In-Place Unsaved Changes Dismissal Guard Dialog
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
