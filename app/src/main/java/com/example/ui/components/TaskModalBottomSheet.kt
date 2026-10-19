package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
 * PHASE 5A: High-end AMOLED Dark "Routine Studio Form" & Preserved Anchor Editor.
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
    isSoundAlertEnabled: Boolean
  ) -> Unit,
  onSaveSlot5: (title: String, voiceMessage: String) -> Unit,
  onMoveToTrash: (taskId: String) -> Unit = {}
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  val isEditing = initialTask != null
  val isAnchor = initialTask?.type == TaskType.ANCHOR_STANDARD ||
    initialTask?.type == TaskType.ANCHOR_SLEEP ||
    initialTask?.type == TaskType.ANCHOR_ELASTIC
  val slotNum = initialTask?.slotNumber
  val isSlot1 = slotNum == 1
  val isSlot5 = slotNum == 5

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

  // Single Task / Master State
  var title by remember { mutableStateOf(initialTask?.title ?: "") }
  var startHour by remember { mutableIntStateOf((initialTask?.startTimeMinute ?: 540) / 60) }
  var startMinute by remember { mutableIntStateOf((initialTask?.startTimeMinute ?: 540) % 60) }
  var durationHours by remember { mutableIntStateOf((initialTask?.durationMinutes ?: 60) / 60) }
  var durationMins by remember { mutableIntStateOf((initialTask?.durationMinutes ?: 60) % 60) }
  var toleranceMinutes by remember { mutableIntStateOf(initialTask?.toleranceMinutes ?: 0) }
  var isVoiceEnabled by remember { mutableStateOf(initialTask?.isVoiceEnabled ?: true) }
  var voiceMessage by remember { mutableStateOf(initialTask?.voiceMessage ?: "") }
  var voiceVolume by remember { mutableFloatStateOf(1f) }

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
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color(0xFF080A0F), // True AMOLED Deep Dark
    contentColor = TextPrimary,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(vertical = 12.dp)
          .size(width = 38.dp, height = 4.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.25f))
      )
    }
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 6.dp)
          .padding(bottom = 96.dp) // Room for sticky bottom bar
      ) {
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
              onClick = onDismiss,
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

          // Subtle Glowing Badge
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(Color(0xFF3B82F6).copy(alpha = 0.15f))
              .border(BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = if (isAnchor) "ANCHOR" else "STUDIO",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              color = if (isAnchor) Color(0xFFA5B4FC) else Color(0xFF60A5FA)
            )
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
                ttsInstance?.speak(voiceMessage, TextToSpeech.QUEUE_FLUSH, null, "anchor_tts")
              }
            },
            enableDnd = enableDnd,
            onEnableDndChange = { enableDnd = it }
          )
        } else {
          // -----------------------------------------------------------
          // LUXURY ROUTINE STUDIO FORM (DYNAMIC TASKS)
          // -----------------------------------------------------------

          // 2. 13-CATEGORY SELECTOR CARD
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(18.dp))
              .clickable { showCategoryGridModal = true }
              .testTag("category_selector_card"),
            color = Color(0xFF121620)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.18f)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(text = selectedCategory.icon, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = "Active Domain",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                  )
                  Text(
                    text = "${selectedCategory.icon} ${selectedCategory.title}",
                    color = Color(0xFFF8FAFC),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "Change",
                  color = Color(0xFF60A5FA),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.Default.KeyboardArrowDown,
                  contentDescription = null,
                  tint = Color(0xFF60A5FA),
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 3. "SPLIT INTO 2 SESSIONS" MODE SWITCHER (Samsung-style animated toggle)
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .border(
                BorderStroke(
                  1.dp,
                  if (isFluidTwinMode) Color(0xFF8B5CF6).copy(alpha = 0.6f) else Color(0xFF1F2637)
                ),
                RoundedCornerShape(18.dp)
              ),
            color = Color(0xFF121620)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                      if (isFluidTwinMode) Color(0xFF8B5CF6).copy(alpha = 0.25f)
                      else Color(0xFF1E293B)
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.CallSplit,
                    contentDescription = null,
                    tint = if (isFluidTwinMode) Color(0xFFC4B5FD) else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                  )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = "Split into 2 Sessions",
                    color = Color(0xFFF8FAFC),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  )
                  Text(
                    text = "Auto-balancing communicating vessels linked to total duration pool",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                  )
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              Switch(
                checked = isFluidTwinMode,
                onCheckedChange = { isFluidTwinMode = it },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = Color(0xFF8B5CF6),
                  uncheckedThumbColor = Color(0xFF94A3B8),
                  uncheckedTrackColor = Color(0xFF1E293B)
                ),
                modifier = Modifier.testTag("toggle_fluid_twin_switch")
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // -----------------------------------------------------------
          // MODE 1: SPLIT TOGGLE IS OFF (Standard Single Session)
          // -----------------------------------------------------------
          if (!isFluidTwinMode) {
            // Master Task Title Input
            Text(
              text = "Task Title",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = title,
              onValueChange = { title = it },
              placeholder = { Text("Enter routine task title...", color = TextTertiary) },
              modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF121620),
                unfocusedContainerColor = Color(0xFF121620),
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFF1F2637),
                focusedTextColor = Color(0xFFF8FAFC),
                unfocusedTextColor = Color(0xFFF8FAFC)
              ),
              shape = RoundedCornerShape(16.dp),
              singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Start Time Picker Card
            Text(
              text = "Preferred Start Time",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(16.dp))
                .clickable {
                  clockTarget = 0
                  showAnalogClockModal = true
                }
                .testTag("open_time_picker_button"),
              color = Color(0xFF121620)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = RoutineTask.formatTime(startHour * 60 + startMinute),
                      fontSize = 18.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFF8FAFC)
                    )
                    Text(
                      text = "24h: ${String.format(Locale.US, "%02d:%02d", startHour, startMinute)} • Tap to open watch dial",
                      fontSize = 11.sp,
                      color = Color(0xFF94A3B8)
                    )
                  }
                }

                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                  Text(
                    text = "Change 🕒",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Planned Duration (Numeric Hours & Minutes)
            Text(
              text = "Planned Duration",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(16.dp)),
              color = Color(0xFF121620)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                val totalMins = durationHours * 60 + durationMins
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.AccessTime,
                      contentDescription = null,
                      tint = Color(0xFF10B981),
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "${durationHours}h ${durationMins}m ($totalMins mins)",
                      fontSize = 16.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFF8FAFC)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                  OutlinedTextField(
                    value = durationHours.toString(),
                    onValueChange = { durationHours = it.toIntOrNull()?.coerceIn(0, 12) ?: 0 },
                    label = { Text("Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("duration_hours_input"),
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

                  OutlinedTextField(
                    value = durationMins.toString(),
                    onValueChange = { durationMins = it.toIntOrNull()?.coerceIn(0, 59) ?: 0 },
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("duration_mins_input"),
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
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Schedule Flexibility Chips
            Text(
              text = "Schedule Flexibility",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              listOf(
                0 to "Strict (±0m)",
                15 to "Balanced (±15m)",
                30 to "Flexible (±30m)"
              ).forEach { (tol, label) ->
                val isSelected = toleranceMinutes == tol
                Surface(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                      BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF3B82F6) else Color(0xFF1F2637)
                      ),
                      RoundedCornerShape(12.dp)
                    )
                    .clickable { toleranceMinutes = tol },
                  color = if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color(0xFF121620)
                ) {
                  Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF60A5FA) else Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Master Voice Announcement Card with TTS test & volume slider
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(18.dp)),
              color = Color(0xFF121620)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                      contentDescription = null,
                      tint = Color(0xFF38BDF8),
                      modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text = "Voice Announcement",
                        color = Color(0xFFF8FAFC),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                      )
                      Text(
                        text = "TTS announcement at task initiation",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                      )
                    }
                  }

                  Switch(
                    checked = isVoiceEnabled,
                    onCheckedChange = { isVoiceEnabled = it },
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = Color.White,
                      checkedTrackColor = Color(0xFF38BDF8),
                      uncheckedThumbColor = Color(0xFF94A3B8),
                      uncheckedTrackColor = Color(0xFF1E293B)
                    )
                  )
                }

                if (isVoiceEnabled) {
                  Spacer(modifier = Modifier.height(12.dp))
                  OutlinedTextField(
                    value = voiceMessage,
                    onValueChange = { voiceMessage = it },
                    placeholder = { Text("e.g., Time for deep work session", color = TextTertiary) },
                    trailingIcon = {
                      IconButton(
                        onClick = {
                          val msg = voiceMessage.ifBlank { "${title.ifBlank { "Task" }} starting now" }
                          ttsInstance?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "voice_test")
                        },
                        modifier = Modifier.testTag("test_voice_button")
                      ) {
                        Icon(
                          imageVector = Icons.Default.PlayArrow,
                          contentDescription = "Test Voice",
                          tint = Color(0xFF38BDF8)
                        )
                      }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("voice_message_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedContainerColor = Color(0xFF0F141E),
                      unfocusedContainerColor = Color(0xFF0F141E),
                      focusedBorderColor = Color(0xFF38BDF8),
                      unfocusedBorderColor = Color(0xFF1F2637),
                      focusedTextColor = Color(0xFFF8FAFC),
                      unfocusedTextColor = Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(12.dp)
                  )

                  Spacer(modifier = Modifier.height(10.dp))

                  // Volume Slider (0% to 100%)
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.VolumeMute,
                      contentDescription = null,
                      tint = Color(0xFF94A3B8),
                      modifier = Modifier.size(16.dp)
                    )
                    Slider(
                      value = voiceVolume,
                      onValueChange = { voiceVolume = it },
                      valueRange = 0f..1f,
                      modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                      colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF38BDF8),
                        inactiveTrackColor = Color(0xFF1E293B)
                      )
                    )
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                      contentDescription = null,
                      tint = Color(0xFF38BDF8),
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
                    value = totalPoolHours.toString(),
                    onValueChange = {
                      val newH = it.toIntOrNull()?.coerceIn(1, 12) ?: 1
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
                    value = totalPoolMinutes.toString(),
                    onValueChange = {
                      val newM = it.toIntOrNull()?.coerceIn(0, 59) ?: 0
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
                ttsInstance?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "s1_tts")
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
                ttsInstance?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "s2_tts")
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
                .clip(RoundedCornerShape(18.dp))
                .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(18.dp)),
              color = Color(0xFF121620)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                  )
                  Switch(
                    checked = isSoundAlertEnabled,
                    onCheckedChange = { isSoundAlertEnabled = it },
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = Color.White,
                      checkedTrackColor = Color(0xFF38BDF8),
                      uncheckedThumbColor = Color(0xFF94A3B8),
                      uncheckedTrackColor = Color(0xFF1E293B)
                    )
                  )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                  text = "Task Alerts",
                  color = Color(0xFFF8FAFC),
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
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
                  color = Color(0xFF0F141E)
                ) {
                  Text(
                    text = customRingtoneTitle.take(18),
                    color = Color(0xFF60A5FA),
                    fontSize = 10.sp,
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
                .clip(RoundedCornerShape(18.dp))
                .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(18.dp)),
              color = Color(0xFF121620)
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(20.dp)
                  )
                  Switch(
                    checked = enableDnd,
                    onCheckedChange = { enableDnd = it },
                    colors = SwitchDefaults.colors(
                      checkedThumbColor = Color.White,
                      checkedTrackColor = Color(0xFF8B5CF6),
                      uncheckedThumbColor = Color(0xFF94A3B8),
                      uncheckedTrackColor = Color(0xFF1E293B)
                    )
                  )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                  text = "Auto DND",
                  color = Color(0xFFF8FAFC),
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F141E))
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
                    fontWeight = FontWeight.SemiBold
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // ==============================================================
          // 5. RECURRENCE SCHEDULE CARD
          // ==============================================================
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(18.dp)),
            color = Color(0xFF121620)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Recurrence Schedule",
                  color = Color(0xFFF8FAFC),
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Surface(
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        isDailyRecurrence = true
                        selectedDays = RoutineDay.allDays()
                      },
                    color = if (isDailyRecurrence) Color(0xFF3B82F6) else Color(0xFF0F141E)
                  ) {
                    Text(
                      text = "Daily",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isDailyRecurrence) Color.White else Color(0xFF94A3B8),
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                  }

                  Surface(
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        isDailyRecurrence = false
                      },
                    color = if (!isDailyRecurrence) Color(0xFF3B82F6) else Color(0xFF0F141E)
                  ) {
                    Text(
                      text = "Specific Days",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (!isDailyRecurrence) Color.White else Color(0xFF94A3B8),
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                    RoutineDay.MONDAY to "M",
                    RoutineDay.TUESDAY to "T",
                    RoutineDay.WEDNESDAY to "W",
                    RoutineDay.THURSDAY to "T",
                    RoutineDay.FRIDAY to "F",
                    RoutineDay.SATURDAY to "S",
                    RoutineDay.SUNDAY to "S"
                  ).forEach { (day, label) ->
                    val isDaySelected = selectedDays.contains(day)
                    Box(
                      modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                          if (isDaySelected) Color(0xFF3B82F6)
                          else Color(0xFF0F141E)
                        )
                        .border(
                          BorderStroke(
                            1.dp,
                            if (isDaySelected) Color(0xFF60A5FA) else Color(0xFF1F2637)
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
                        color = if (isDaySelected) Color.White else Color(0xFF94A3B8)
                      )
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
        border = BorderStroke(1.dp, Color(0xFF1F2637))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Luxury Gradient Save CTA Button
          Button(
            onClick = {
              if (isAnchor) {
                val calculatedDuration = durationHours * 60 + durationMins
                val calculatedStart = startHour * 60 + startMinute
                if (isSlot5) {
                  onSaveSlot5(title.ifBlank { "Elastic Rubber Buffer" }, voiceMessage)
                } else if (slotNum != null) {
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
                    isSoundAlertEnabled
                  )
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
                  allowSplitting = true,
                  selectedDays = selectedDays,
                  isVoiceEnabled = isVoiceEnabled,
                  customNotificationTitle = actualTitle,
                  customNotificationBody = voiceMessage,
                  customRingtoneUri = customRingtoneUri,
                  customRingtoneTitle = customRingtoneTitle,
                  isSoundAlertEnabled = isSoundAlertEnabled,
                  toleranceMinutes = toleranceMinutes,
                  category = selectedCategory.id
                )
                onSaveTask(task)
              }
              onDismiss()
            },
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
                  if (isFluidTwinMode) Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6)))
                  else Brush.horizontalGradient(listOf(Color(0xFF3B82F6), Color(0xFF06B6D4))),
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
      else -> startHour
    }
    val initialM = when (clockTarget) {
      1 -> part1StartMinute
      2 -> part2StartMinute
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
}

/**
 * Preserved Phase 2 Anchor Edit Form.
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
  enableDnd: Boolean,
  onEnableDndChange: (Boolean) -> Unit
) {
  Column {
    Text(
      text = "Anchor Title",
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
      color = Color(0xFF94A3B8)
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
      value = title,
      onValueChange = onTitleChange,
      modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color(0xFF121620),
        unfocusedContainerColor = Color(0xFF121620),
        focusedBorderColor = AnchorGradientStart,
        unfocusedBorderColor = Color(0xFF1F2637),
        focusedTextColor = Color(0xFFF8FAFC),
        unfocusedTextColor = Color(0xFFF8FAFC)
      ),
      shape = RoundedCornerShape(14.dp),
      singleLine = true
    )

    // Slot 1 & Slot 5: Start time is COMPLETELY HIDDEN
    if (!isSlot1 && !isSlot5) {
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = "Anchor Start Time",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF94A3B8)
      )
      Spacer(modifier = Modifier.height(6.dp))

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(16.dp))
          .clickable { onOpenClock() }
          .testTag("open_time_picker_button"),
        color = Color(0xFF121620)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = AnchorGradientStart, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = RoutineTask.formatTime(startHour * 60 + startMinute),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "24h: ${String.format(Locale.US, "%02d:%02d", startHour, startMinute)} • Tap to open watch dial",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(AnchorGradientStart.copy(alpha = 0.2f))
              .padding(horizontal = 8.dp, vertical = 5.dp)
          ) {
            Text(text = "Change 🕒", color = Color(0xFFA5B4FC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

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
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Bedtime, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(20.dp))
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
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
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

    // Duration Section: Completely hidden for Slot 5; Only numeric [H] & [M] inputs for all other anchors
    if (!isSlot5) {
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = if (isSlot1) "Sleep Duration" else "Duration",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF94A3B8)
      )
      Spacer(modifier = Modifier.height(6.dp))

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(16.dp)),
        color = Color(0xFF121620)
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          val totalMins = durationHours * 60 + durationMins
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = AnchorGradientStart, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "${durationHours}h ${durationMins}m ($totalMins mins)",
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            OutlinedTextField(
              value = durationHours.toString(),
              onValueChange = { onDurationHoursChange(it.toIntOrNull()?.coerceIn(0, 12) ?: 0) },
              label = { Text("Hours") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f).testTag("duration_hours_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F141E),
                unfocusedContainerColor = Color(0xFF0F141E),
                focusedBorderColor = AnchorGradientStart,
                unfocusedBorderColor = Color(0xFF1F2637),
                focusedTextColor = Color(0xFFF8FAFC),
                unfocusedTextColor = Color(0xFFF8FAFC)
              ),
              shape = RoundedCornerShape(12.dp),
              singleLine = true
            )

            OutlinedTextField(
              value = durationMins.toString(),
              onValueChange = { onDurationMinsChange(it.toIntOrNull()?.coerceIn(0, 59) ?: 0) },
              label = { Text("Minutes") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f).testTag("duration_mins_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF0F141E),
                unfocusedContainerColor = Color(0xFF0F141E),
                focusedBorderColor = AnchorGradientStart,
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
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Voice Message Announcement Card with inline TTS preview
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(16.dp)),
      color = Color(0xFF121620)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = AnchorGradientStart, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Voice Announcement", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = voiceMessage,
          onValueChange = onVoiceMessageChange,
          placeholder = { Text("Enter custom speech announcement...", color = TextTertiary) },
          trailingIcon = {
            IconButton(onClick = onTestVoice, modifier = Modifier.testTag("test_voice_button")) {
              Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Test Voice", tint = Color(0xFFA5B4FC))
            }
          },
          modifier = Modifier.fillMaxWidth().testTag("voice_message_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF0F141E),
            unfocusedContainerColor = Color(0xFF0F141E),
            focusedBorderColor = AnchorGradientStart,
            unfocusedBorderColor = Color(0xFF1F2637),
            focusedTextColor = Color(0xFFF8FAFC),
            unfocusedTextColor = Color(0xFFF8FAFC)
          ),
          shape = RoundedCornerShape(12.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // DND Toggle
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(16.dp)),
      color = Color(0xFF121620)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.DoNotDisturbOn, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Text(text = "Do Not Disturb (DND)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Switch(
          checked = enableDnd,
          onCheckedChange = onEnableDndChange,
          colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF8B5CF6))
        )
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
          value = durationHours.toString(),
          onValueChange = {
            val h = it.toIntOrNull()?.coerceIn(0, 12) ?: 0
            onDurationChange(h, durationMins)
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
          value = durationMins.toString(),
          onValueChange = {
            val m = it.toIntOrNull()?.coerceIn(0, 59) ?: 0
            onDurationChange(durationHours, m)
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
          value = safetyFloor.toString(),
          onValueChange = {
            val floor = it.toIntOrNull()?.coerceIn(15, 120) ?: 45
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
  var isSelectingHour by remember { mutableStateOf(true) }
  var isPm by remember { mutableStateOf(initialHour24 >= 12) }
  var selectedHour12 by remember {
    val h = initialHour24 % 12
    mutableIntStateOf(if (h == 0) 12 else h)
  }
  var selectedMinute by remember { mutableIntStateOf(initialMinute) }

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
          text = if (isSelectingHour) "DRAG OR TAP TO SELECT HOUR" else "DRAG OR TAP TO SELECT MINUTE",
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
          // Hour Box
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
              .clickable { isSelectingHour = true },
            color = if (isSelectingHour) Color(0xFF1E293B) else Color(0xFF121620)
          ) {
            Text(
              text = String.format(Locale.US, "%02d", selectedHour12),
              fontSize = 32.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              color = if (isSelectingHour) Color(0xFF38BDF8) else Color(0xFFF8FAFC),
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
          }

          Text(
            text = ":",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(horizontal = 6.dp)
          )

          // Minute Box
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
              .clickable { isSelectingHour = false },
            color = if (!isSelectingHour) Color(0xFF1E293B) else Color(0xFF121620)
          ) {
            Text(
              text = String.format(Locale.US, "%02d", selectedMinute),
              fontSize = 32.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              color = if (!isSelectingHour) Color(0xFF38BDF8) else Color(0xFFF8FAFC),
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
          }

          Spacer(modifier = Modifier.width(16.dp))

          // AM / PM Toggle Column
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { isPm = false },
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
                .clickable { isPm = true },
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

        // Watch Dial (Custom Canvas with rotating hand & numerals)
        Box(
          modifier = Modifier
            .size(230.dp)
            .clip(CircleShape)
            .background(Color(0xFF0F141E))
            .border(BorderStroke(1.5.dp, Color(0xFF1F2637)), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          val dialRadius = 85.dp

          // Rotating Clock Hand Canvas with Pointer Drag/Tap
          Canvas(
            modifier = Modifier
              .fillMaxSize()
              .pointerInput(isSelectingHour) {
                detectTapGestures { offset ->
                  val cx = size.width / 2f
                  val cy = size.height / 2f
                  val angleRad = atan2((offset.y - cy).toDouble(), (offset.x - cx).toDouble())
                  var deg = Math.toDegrees(angleRad).toFloat() + 90f
                  if (deg < 0) deg += 360f

                  if (isSelectingHour) {
                    val h = ((deg / 30f).roundToInt() % 12).let { if (it == 0) 12 else it }
                    selectedHour12 = h
                    isSelectingHour = false // Automatically transition to minute
                  } else {
                    val m = ((deg / 6f).roundToInt() % 60)
                    selectedMinute = m
                  }
                }
              }
              .pointerInput(isSelectingHour) {
                detectDragGestures { change, _ ->
                  change.consume()
                  val cx = size.width / 2f
                  val cy = size.height / 2f
                  val angleRad = atan2((change.position.y - cy).toDouble(), (change.position.x - cx).toDouble())
                  var deg = Math.toDegrees(angleRad).toFloat() + 90f
                  if (deg < 0) deg += 360f

                  if (isSelectingHour) {
                    val h = ((deg / 30f).roundToInt() % 12).let { if (it == 0) 12 else it }
                    selectedHour12 = h
                  } else {
                    val m = ((deg / 6f).roundToInt() % 60)
                    selectedMinute = m
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
              radius = 16.dp.toPx(),
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
                .size(32.dp)
                .align(Alignment.Center)
                .offset(x = numX.dp, y = numY.dp)
                .clip(CircleShape)
                .clickable {
                  if (isSelectingHour) {
                    selectedHour12 = i
                    isSelectingHour = false
                  } else {
                    selectedMinute = (i * 5) % 60
                  }
                },
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                color = if (isCurrent) Color.White else Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val finalH = if (isPm) {
            if (selectedHour12 == 12) 12 else selectedHour12 + 12
          } else {
            if (selectedHour12 == 12) 0 else selectedHour12
          }
          onConfirm(finalH, selectedMinute)
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
