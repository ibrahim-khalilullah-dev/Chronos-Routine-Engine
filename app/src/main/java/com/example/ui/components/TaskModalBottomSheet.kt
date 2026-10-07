package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.TaskType
import com.example.ui.theme.AnchorGradientStart
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OneUiBorder
import com.example.ui.theme.OneUiSurface
import com.example.ui.theme.OneUiSurfaceElevated
import com.example.ui.theme.OneUiSurfaceHigh
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.util.UUID

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

  // 2-Session Switchable Fluid Twin Toggle state
  var isFluidTwinMode by remember { mutableStateOf(initialTask?.isFluidTwin ?: false) }
  var fluidTwinSelectedTab by remember { mutableIntStateOf(0) } // 0 = Session 1, 1 = Session 2

  // Single / Base Task State
  var title by remember { mutableStateOf(initialTask?.title ?: "") }
  var startHour by remember { mutableIntStateOf((initialTask?.startTimeMinute ?: 540) / 60) }
  var startMinute by remember { mutableIntStateOf((initialTask?.startTimeMinute ?: 540) % 60) }
  var durationHours by remember { mutableIntStateOf((initialTask?.durationMinutes ?: 60) / 60) }
  var durationMins by remember { mutableIntStateOf((initialTask?.durationMinutes ?: 60) % 60) }
  var voiceMessage by remember { mutableStateOf(initialTask?.voiceMessage ?: "") }

  // Fluid Twin Session 1 Specific State
  var part1Title by remember {
    mutableStateOf(if (initialTask?.isFluidTwin == true && initialTask.fluidTwinPart == 1) initialTask.title else "")
  }
  var part1StartHour by remember { mutableIntStateOf(startHour) }
  var part1StartMinute by remember { mutableIntStateOf(startMinute) }
  var part1DurationHours by remember { mutableIntStateOf(if (durationHours > 0) durationHours else 1) }
  var part1DurationMins by remember { mutableIntStateOf(if (durationHours == 0 && durationMins < 45) 45 else durationMins) }
  var part1VoiceMessage by remember { mutableStateOf(voiceMessage) }

  // Fluid Twin Session 2 Specific State
  var part2Title by remember {
    mutableStateOf(if (initialTask?.isFluidTwin == true && initialTask.fluidTwinPart == 2) initialTask.title else "")
  }
  var part2StartHour by remember { mutableIntStateOf(14) } // Default 2:00 PM
  var part2StartMinute by remember { mutableIntStateOf(0) }
  var part2DurationHours by remember { mutableIntStateOf(2) }
  var part2DurationMins by remember { mutableIntStateOf(0) }
  var part2VoiceMessage by remember { mutableStateOf("") }

  // Common Controls State
  var enableDnd by remember { mutableStateOf(initialTask?.enableDnd ?: false) }
  var enableHaptic by remember { mutableStateOf(initialTask?.enableHaptic ?: true) }
  var allowSplitting by remember { mutableStateOf(initialTask?.allowSplitting ?: true) }
  var isVoiceEnabled by remember { mutableStateOf(initialTask?.isVoiceEnabled ?: true) }
  var isSoundAlertEnabled by remember { mutableStateOf(initialTask?.isSoundAlertEnabled ?: true) }
  var customNotificationTitle by remember { mutableStateOf(initialTask?.customNotificationTitle ?: "") }
  var customNotificationBody by remember { mutableStateOf(initialTask?.customNotificationBody ?: "") }

  // Notification Sound State & Dropdown
  var customRingtoneUri by remember { mutableStateOf(initialTask?.customRingtoneUri) }
  var customRingtoneTitle by remember { mutableStateOf(initialTask?.customRingtoneTitle ?: "Default Notification Tone") }
  var isPreviewPlaying by remember { mutableStateOf(false) }
  var activeRingtonePlayer by remember { mutableStateOf<Ringtone?>(null) }
  var soundDropdownExpanded by remember { mutableStateOf(false) }

  // Confirmation dialog for delete
  var showDeleteConfirmDialog by remember { mutableStateOf(false) }

  var selectedDays by remember {
    mutableStateOf(initialTask?.selectedDays ?: RoutineDay.allDays())
  }

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
        customRingtoneTitle = r?.getTitle(context) ?: "Selected Device Ringtone"
      } else {
        customRingtoneUri = null
        customRingtoneTitle = "Silent / None"
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      try {
        activeRingtonePlayer?.stop()
      } catch (_: Exception) {}
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = OneUiSurface,
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
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 8.dp)
        .padding(bottom = 36.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = when {
              isSlot1 -> "Slot 1: Reverse Sleep Settings"
              isSlot5 -> "Slot 5: Elastic Rubber Buffer"
              isAnchor -> "Anchor Slot $slotNum"
              isFluidTwinMode -> "Fluid Twin (2 Communicating Sessions)"
              isEditing -> "Edit Dynamic Task"
              else -> "New Routine Task"
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = when {
              isSlot1 -> "Bedtime is auto-calculated: Slot 2 Start minus Sleep Duration"
              isSlot5 -> "Times are mathematically fixed to fill the gap between Slot 4 & 6"
              isAnchor -> "Core daily pillar • Protected from dynamic override"
              isFluidTwinMode -> "Communicating vessels: Part 1 & 2 share duration pool within strict anchor walls"
              else -> "Dynamic task will auto-fit into available gap"
            },
            fontSize = 12.sp,
            color = TextSecondary
          )
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.testTag("dismiss_modal_button")
        ) {
          Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // ==============================================================
      // 1. FLUID TWIN SWITCHER (Toggle Switch)
      // ==============================================================
      if (!isAnchor) {
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
              BorderStroke(1.dp, if (isFluidTwinMode) Color(0xFF8B5CF6).copy(alpha = 0.6f) else OneUiBorder),
              RoundedCornerShape(16.dp)
            ),
          color = OneUiSurfaceElevated
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
                  .background(if (isFluidTwinMode) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.CallSplit,
                  contentDescription = null,
                  tint = if (isFluidTwinMode) Color(0xFFC4B5FD) else TextSecondary,
                  modifier = Modifier.size(18.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Link as Fluid Twin (2 Communicating Sessions)",
                  color = TextPrimary,
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 14.sp
                )
                Text(
                  text = "Share duration pool dynamically across 2 separate sessions",
                  color = TextSecondary,
                  fontSize = 11.sp
                )
              }
            }
            Switch(
              checked = isFluidTwinMode,
              onCheckedChange = { isFluidTwinMode = it },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF8B5CF6)
              ),
              modifier = Modifier.testTag("toggle_fluid_twin_switch")
            )
          }
        }
        Spacer(modifier = Modifier.height(14.dp))
      }

      // ==============================================================
      // 2. FORM FIELDS: SINGLE TASK BASELINE vs 2-SESSION FLUID TWIN
      // ==============================================================
      if (isFluidTwinMode) {
        // Shared Twin Routine Group Title
        Text(
          text = "Twin Routine Master Name",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          placeholder = { Text("e.g., Deep Work & Synthesis", color = TextTertiary) },
          modifier = Modifier.fillMaxWidth().testTag("twin_master_title_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = OneUiSurfaceElevated,
            unfocusedContainerColor = OneUiSurfaceElevated,
            focusedBorderColor = Color(0xFF8B5CF6),
            unfocusedBorderColor = OneUiBorder
          ),
          shape = RoundedCornerShape(14.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Two-Tab Switchable View: [Session 1] | [Session 2]
        TabRow(
          selectedTabIndex = fluidTwinSelectedTab,
          containerColor = OneUiSurfaceElevated,
          contentColor = Color(0xFF8B5CF6),
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              Modifier.tabIndicatorOffset(tabPositions[fluidTwinSelectedTab]),
              color = Color(0xFF8B5CF6)
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(14.dp))
        ) {
          Tab(
            selected = fluidTwinSelectedTab == 0,
            onClick = { fluidTwinSelectedTab = 0 },
            text = { Text("Session 1 (Part 1)", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
            modifier = Modifier.testTag("fluid_twin_tab_session_1")
          )
          Tab(
            selected = fluidTwinSelectedTab == 1,
            onClick = { fluidTwinSelectedTab = 1 },
            text = { Text("Session 2 (Part 2)", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
            modifier = Modifier.testTag("fluid_twin_tab_session_2")
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // TAB CONTENT
        if (fluidTwinSelectedTab == 0) {
          // SESSION 1 CONFIGURATION
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .border(BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f)), RoundedCornerShape(16.dp)),
            color = OneUiSurfaceElevated
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(text = "Session 1: Morning/Day Block", fontWeight = FontWeight.Bold, color = Color(0xFFC4B5FD), fontSize = 14.sp)
              Spacer(modifier = Modifier.height(10.dp))

              Text(text = "Session 1 Title", fontSize = 12.sp, color = TextSecondary)
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = part1Title,
                onValueChange = { part1Title = it },
                placeholder = { Text(if (title.isNotBlank()) "$title (Part 1)" else "e.g., Deep Coding (Part 1)", color = TextTertiary) },
                modifier = Modifier.fillMaxWidth().testTag("session_1_title_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = OneUiSurface,
                  unfocusedContainerColor = OneUiSurface,
                  focusedBorderColor = Color(0xFF8B5CF6),
                  unfocusedBorderColor = OneUiBorder
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
              )

              Spacer(modifier = Modifier.height(12.dp))

              // Session 1 Start Time
              Text(text = "Preferred Start Time", fontSize = 12.sp, color = TextSecondary)
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(text = String.format("%02d:%02d", part1StartHour, part1StartMinute), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  OutlinedButton(
                    onClick = { part1StartHour = (part1StartHour + 1) % 24 },
                    shape = RoundedCornerShape(8.dp)
                  ) { Text("+1h", fontSize = 11.sp, color = TextPrimary) }
                  OutlinedButton(
                    onClick = { part1StartMinute = (part1StartMinute + 15) % 60 },
                    shape = RoundedCornerShape(8.dp)
                  ) { Text("+15m", fontSize = 11.sp, color = TextPrimary) }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Session 1 Duration (Floor: 45m limit)
              Text(text = "Duration (Min Safety Floor: 45m)", fontSize = 12.sp, color = TextSecondary)
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                val p1MinsTotal = part1DurationHours * 60 + part1DurationMins
                Text(text = "${part1DurationHours}h ${part1DurationMins}m ($p1MinsTotal mins)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  OutlinedButton(
                    onClick = { part1DurationHours = (part1DurationHours + 1).coerceAtMost(8) },
                    shape = RoundedCornerShape(8.dp)
                  ) { Text("+1h", fontSize = 11.sp, color = TextPrimary) }
                  OutlinedButton(
                    onClick = {
                      val totalMins = part1DurationHours * 60 + part1DurationMins + 15
                      val bounded = totalMins.coerceAtLeast(45)
                      part1DurationHours = bounded / 60
                      part1DurationMins = bounded % 60
                    },
                    shape = RoundedCornerShape(8.dp)
                  ) { Text("+15m", fontSize = 11.sp, color = TextPrimary) }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Session 1 Voice Message
              Text(text = "Custom Voice Reminder", fontSize = 12.sp, color = TextSecondary)
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = part1VoiceMessage,
                onValueChange = { part1VoiceMessage = it },
                placeholder = { Text("e.g., Session 1 starting. Focus on architecture.", color = TextTertiary) },
                modifier = Modifier.fillMaxWidth().testTag("session_1_voice_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = OneUiSurface,
                  unfocusedContainerColor = OneUiSurface,
                  focusedBorderColor = Color(0xFF8B5CF6),
                  unfocusedBorderColor = OneUiBorder
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 2
              )
            }
          }
        } else {
          // SESSION 2 CONFIGURATION
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .border(BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f)), RoundedCornerShape(16.dp)),
            color = OneUiSurfaceElevated
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(text = "Session 2: Afternoon/Evening Block", fontWeight = FontWeight.Bold, color = Color(0xFFC4B5FD), fontSize = 14.sp)
              Spacer(modifier = Modifier.height(10.dp))

              Text(text = "Session 2 Title", fontSize = 12.sp, color = TextSecondary)
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = part2Title,
                onValueChange = { part2Title = it },
                placeholder = { Text(if (title.isNotBlank()) "$title (Part 2)" else "e.g., Code Review & Synthesis (Part 2)", color = TextTertiary) },
                modifier = Modifier.fillMaxWidth().testTag("session_2_title_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = OneUiSurface,
                  unfocusedContainerColor = OneUiSurface,
                  focusedBorderColor = Color(0xFF8B5CF6),
                  unfocusedBorderColor = OneUiBorder
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
              )

              Spacer(modifier = Modifier.height(12.dp))

              // Session 2 Start Time
              Text(text = "Preferred Start Time", fontSize = 12.sp, color = TextSecondary)
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(text = String.format("%02d:%02d", part2StartHour, part2StartMinute), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  OutlinedButton(
                    onClick = { part2StartHour = (part2StartHour + 1) % 24 },
                    shape = RoundedCornerShape(8.dp)
                  ) { Text("+1h", fontSize = 11.sp, color = TextPrimary) }
                  OutlinedButton(
                    onClick = { part2StartMinute = (part2StartMinute + 15) % 60 },
                    shape = RoundedCornerShape(8.dp)
                  ) { Text("+15m", fontSize = 11.sp, color = TextPrimary) }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Session 2 Duration (Floor: 45m limit)
              Text(text = "Duration (Min Safety Floor: 45m)", fontSize = 12.sp, color = TextSecondary)
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                val p2MinsTotal = part2DurationHours * 60 + part2DurationMins
                Text(text = "${part2DurationHours}h ${part2DurationMins}m ($p2MinsTotal mins)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  OutlinedButton(
                    onClick = { part2DurationHours = (part2DurationHours + 1).coerceAtMost(8) },
                    shape = RoundedCornerShape(8.dp)
                  ) { Text("+1h", fontSize = 11.sp, color = TextPrimary) }
                  OutlinedButton(
                    onClick = {
                      val totalMins = part2DurationHours * 60 + part2DurationMins + 15
                      val bounded = totalMins.coerceAtLeast(45)
                      part2DurationHours = bounded / 60
                      part2DurationMins = bounded % 60
                    },
                    shape = RoundedCornerShape(8.dp)
                  ) { Text("+15m", fontSize = 11.sp, color = TextPrimary) }
                }
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Session 2 Voice Message
              Text(text = "Custom Voice Reminder", fontSize = 12.sp, color = TextSecondary)
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = part2VoiceMessage,
                onValueChange = { part2VoiceMessage = it },
                placeholder = { Text("e.g., Session 2 starting. Synthesis sprint.", color = TextTertiary) },
                modifier = Modifier.fillMaxWidth().testTag("session_2_voice_input"),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = OneUiSurface,
                  unfocusedContainerColor = OneUiSurface,
                  focusedBorderColor = Color(0xFF8B5CF6),
                  unfocusedBorderColor = OneUiBorder
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 2
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Total Combined Pool Banner
        val combinedTotalMins = (part1DurationHours * 60 + part1DurationMins) + (part2DurationHours * 60 + part2DurationMins)
        Surface(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
          color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
          border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f))
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(imageVector = Icons.Default.CallSplit, contentDescription = null, tint = Color(0xFFC4B5FD), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Communicating Vessels Pool: ${combinedTotalMins / 60}h ${combinedTotalMins % 60}m ($combinedTotalMins mins total) • Bounded by 7 Core Anchors",
              color = Color(0xFFE2E8F0),
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      } else {
        // ==============================================================
        // SIMPLE BASELINE FORM (Title, Start Time, Duration)
        // ==============================================================
        Text(
          text = if (isAnchor) "Anchor Title" else "Task Title",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          placeholder = { Text("e.g., Deep Coding Sprint", color = TextTertiary) },
          modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = OneUiSurfaceElevated,
            unfocusedContainerColor = OneUiSurfaceElevated,
            focusedBorderColor = AnchorGradientStart,
            unfocusedBorderColor = OneUiBorder
          ),
          shape = RoundedCornerShape(14.dp),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Preferred Start Time
        Text(
          text = if (isAnchor) "Anchor Start Time" else "Preferred Start Time",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        Surface(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
          color = OneUiSurfaceElevated
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = AnchorGradientStart, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = String.format("%02d:%02d", startHour, startMinute),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { startHour = (startHour + 1) % 24 },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, OneUiBorder)
              ) { Text("+1h", fontSize = 12.sp, color = TextPrimary) }
              OutlinedButton(
                onClick = { startMinute = (startMinute + 15) % 60 },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, OneUiBorder)
              ) { Text("+15m", fontSize = 12.sp, color = TextPrimary) }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Duration Pickers
        Text(
          text = "Duration",
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          color = TextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))

        Surface(
          modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
          color = OneUiSurfaceElevated
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Default.AccessTime, contentDescription = null, tint = AnchorGradientStart, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "${durationHours}h ${durationMins}m",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { durationHours = ((durationHours + 1) % 12).coerceAtLeast(0) },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, OneUiBorder)
              ) { Text("+1h", fontSize = 12.sp, color = TextPrimary) }
              OutlinedButton(
                onClick = { durationMins = (durationMins + 15) % 60 },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, OneUiBorder)
              ) { Text("+15m", fontSize = 12.sp, color = TextPrimary) }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Voice Message Input
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
          placeholder = { Text("e.g., Focus sprint initiated. Start your priority tasks.", color = TextTertiary) },
          modifier = Modifier.fillMaxWidth().testTag("task_voice_message_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = OneUiSurfaceElevated,
            unfocusedContainerColor = OneUiSurfaceElevated,
            focusedBorderColor = AnchorGradientStart,
            unfocusedBorderColor = OneUiBorder
          ),
          shape = RoundedCornerShape(14.dp),
          maxLines = 2
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // ==============================================================
      // 3. NOTIFICATION SOUND SELECTOR & MASTER SOUND ALERT TOGGLE
      // ==============================================================
      // Master Sound Alert Toggle (ON/OFF)
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .border(
            BorderStroke(1.dp, if (isSoundAlertEnabled) Color(0xFF10B981).copy(alpha = 0.5f) else OneUiBorder),
            RoundedCornerShape(16.dp)
          ),
        color = OneUiSurfaceElevated
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
                .background(if (isSoundAlertEnabled) Color(0xFF10B981).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (isSoundAlertEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeMute,
                contentDescription = null,
                tint = if (isSoundAlertEnabled) Color(0xFF10B981) else TextSecondary,
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Notification Sound Alert",
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
              )
              Text(
                text = if (isSoundAlertEnabled) "Plays chosen system sound & vibration" else "Silent Mode: Alarms & DND fire silently without audio",
                color = if (isSoundAlertEnabled) Color(0xFF10B981) else TextSecondary,
                fontSize = 11.sp
              )
            }
          }
          Switch(
            checked = isSoundAlertEnabled,
            onCheckedChange = { isSoundAlertEnabled = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = Color(0xFF10B981)
            ),
            modifier = Modifier.testTag("toggle_sound_alert_master_switch")
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // System Ringtone Dropdown Selector (Active when sound is enabled)
      AnimatedVisibility(visible = isSoundAlertEnabled) {
        Column {
          Text(
            text = "System Notification Tone",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
          )
          Spacer(modifier = Modifier.height(6.dp))

          Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(16.dp))
                .clickable { soundDropdownExpanded = true }
                .testTag("open_sound_dropdown_button"),
              color = OneUiSurfaceElevated
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(34.dp)
                      .clip(CircleShape)
                      .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(18.dp))
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = customRingtoneTitle,
                      color = TextPrimary,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 14.sp,
                      maxLines = 1
                    )
                    Text(
                      text = "Tap to choose or browse system sounds",
                      color = TextTertiary,
                      fontSize = 11.sp
                    )
                  }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  // Preview Play / Stop Button
                  IconButton(
                    onClick = {
                      try {
                        if (isPreviewPlaying) {
                          activeRingtonePlayer?.stop()
                          isPreviewPlaying = false
                        } else {
                          val ringtoneUri = if (customRingtoneUri != null) {
                            Uri.parse(customRingtoneUri)
                          } else {
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                          }
                          val r = RingtoneManager.getRingtone(context, ringtoneUri)
                          activeRingtonePlayer = r
                          r?.play()
                          isPreviewPlaying = true
                        }
                      } catch (_: Exception) {
                        isPreviewPlaying = false
                      }
                    },
                    modifier = Modifier.size(36.dp).testTag("preview_ringtone_button")
                  ) {
                    Icon(
                      imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                      contentDescription = if (isPreviewPlaying) "Stop" else "Preview",
                      tint = if (isPreviewPlaying) DangerRed else Color(0xFF10B981)
                    )
                  }

                  Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = TextSecondary)
                }
              }
            }

            // Dropdown menu with standard choices + browse launcher
            DropdownMenu(
              expanded = soundDropdownExpanded,
              onDismissRequest = { soundDropdownExpanded = false },
              modifier = Modifier.background(OneUiSurfaceElevated).border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(12.dp))
            ) {
              DropdownMenuItem(
                text = { Text("Default Notification Tone", color = TextPrimary) },
                onClick = {
                  customRingtoneUri = null
                  customRingtoneTitle = "Default Notification Tone"
                  soundDropdownExpanded = false
                }
              )
              DropdownMenuItem(
                text = { Text("Silent / No Sound", color = TextSecondary) },
                onClick = {
                  customRingtoneUri = null
                  customRingtoneTitle = "Silent / None"
                  soundDropdownExpanded = false
                }
              )
              DropdownMenuItem(
                text = { Text("🎵 Browse All Device Ringtones...", color = AnchorGradientStart, fontWeight = FontWeight.Bold) },
                onClick = {
                  soundDropdownExpanded = false
                  val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select System Sound")
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    if (customRingtoneUri != null) {
                      putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(customRingtoneUri))
                    }
                  }
                  ringtonePickerLauncher.launch(intent)
                }
              )
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
        }
      }

      // Hardware DND Toggle
      Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        color = OneUiSurfaceElevated
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.DoNotDisturbOn, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(text = "Do Not Disturb (DND)", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
              Text(text = "Automatically mutes all phone interruptions during this session", color = TextSecondary, fontSize = 11.sp)
            }
          }
          Switch(
            checked = enableDnd,
            onCheckedChange = { enableDnd = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF6366F1)),
            modifier = Modifier.testTag("toggle_dnd_switch")
          )
        }
      }

      // Voice Enabled Toggle (TTS)
      Spacer(modifier = Modifier.height(8.dp))
      Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
        color = OneUiSurfaceElevated
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(text = "Voice Announcement (TTS)", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(text = "Read reminder aloud over lock screen with audio ducking", color = TextSecondary, fontSize = 11.sp)
          }
          Switch(
            checked = isVoiceEnabled,
            onCheckedChange = { isVoiceEnabled = it },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF06B6D4)),
            modifier = Modifier.testTag("toggle_voice_switch")
          )
        }
      }

      // Weekly Recurrence: Sat-Fri
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = "Weekly Recurrence (Sat - Fri)",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
      )
      Spacer(modifier = Modifier.height(6.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        RoutineDay.entries.forEach { day ->
          val isSelected = selectedDays.contains(day)
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(if (isSelected) AnchorGradientStart else OneUiSurfaceElevated)
              .border(
                BorderStroke(1.dp, if (isSelected) Color.White.copy(alpha = 0.3f) else OneUiBorder),
                CircleShape
              )
              .clickable {
                selectedDays = if (isSelected) {
                  if (selectedDays.size > 1) selectedDays - day else selectedDays
                } else {
                  selectedDays + day
                }
              },
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = day.shortName.take(2),
              color = if (isSelected) Color.White else TextSecondary,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Primary Save Button
      Button(
        onClick = {
          if (title.isBlank()) {
            title = if (isAnchor) "Anchor Slot $slotNum" else "New Task"
          }

          val calculatedDuration = durationHours * 60 + durationMins
          val calculatedStart = startHour * 60 + startMinute

          if (isSlot5) {
            onSaveSlot5(title, voiceMessage)
          } else if (isAnchor && slotNum != null) {
            onSaveAnchorDetails(
              slotNum,
              title,
              calculatedStart,
              calculatedDuration,
              voiceMessage,
              enableDnd,
              enableHaptic,
              selectedDays,
              isVoiceEnabled,
              customNotificationTitle,
              customNotificationBody,
              customRingtoneUri,
              customRingtoneTitle,
              isSoundAlertEnabled
            )
          } else if (isFluidTwinMode) {
            val p1Dur = (part1DurationHours * 60 + part1DurationMins).coerceAtLeast(45)
            val p2Dur = (part2DurationHours * 60 + part2DurationMins).coerceAtLeast(45)
            val totalPool = (p1Dur + p2Dur).coerceAtLeast(90)
            val p1Start = part1StartHour * 60 + part1StartMinute
            val p2Start = part2StartHour * 60 + part2StartMinute

            onSaveFluidTwin(
              title,
              totalPool,
              part1Title,
              p1Start,
              p1Dur,
              part1VoiceMessage,
              part2Title,
              p2Start,
              p2Dur,
              part2VoiceMessage,
              enableDnd,
              selectedDays,
              isVoiceEnabled,
              customNotificationTitle,
              customNotificationBody,
              customRingtoneUri,
              customRingtoneTitle,
              isSoundAlertEnabled
            )
          } else {
            val task = RoutineTask(
              id = initialTask?.id ?: "dynamic_${UUID.randomUUID().toString().take(8)}",
              title = title,
              type = TaskType.DYNAMIC,
              startTimeMinute = calculatedStart,
              durationMinutes = calculatedDuration,
              voiceMessage = voiceMessage,
              enableDnd = enableDnd,
              enableHaptic = enableHaptic,
              allowSplitting = allowSplitting,
              selectedDays = selectedDays,
              isVoiceEnabled = isVoiceEnabled,
              customNotificationTitle = customNotificationTitle,
              customNotificationBody = customNotificationBody,
              customRingtoneUri = customRingtoneUri,
              customRingtoneTitle = customRingtoneTitle,
              isSoundAlertEnabled = isSoundAlertEnabled
            )
            onSaveTask(task)
          }
          onDismiss()
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("save_task_button"),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isFluidTwinMode) Color(0xFF8B5CF6) else AnchorGradientStart,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp)
      ) {
        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Save & Apply Schedule", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }

      // ==============================================================
      // 4. MOVE TO TRASH ACTION (Inside Edit Form Only, Requirement 4)
      // ==============================================================
      if (isEditing && !isAnchor && initialTask != null) {
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedButton(
          onClick = { showDeleteConfirmDialog = true },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("move_to_trash_edit_form_button"),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
        ) {
          Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "Move Task to Trash Bin", color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }

  // Delete Confirmation Dialog inside Edit Form
  if (showDeleteConfirmDialog && initialTask != null) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmDialog = false },
      title = { Text("Delete Task?", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
      containerColor = OneUiSurfaceElevated,
      shape = RoundedCornerShape(24.dp)
    )
  }
}
