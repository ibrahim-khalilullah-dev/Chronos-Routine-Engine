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
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.ui.theme.DynamicGradientStart
import com.example.ui.theme.OneUiBorder
import com.example.ui.theme.OneUiSurface
import com.example.ui.theme.OneUiSurfaceElevated
import com.example.ui.theme.OneUiSurfaceHigh
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber
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
    part1StartMinute: Int,
    part1Duration: Int,
    part2StartMinute: Int,
    part2Duration: Int,
    enableDnd: Boolean,
    selectedDays: Set<RoutineDay>,
    isVoiceEnabled: Boolean,
    customNotificationTitle: String,
    customNotificationBody: String,
    customRingtoneUri: String?,
    customRingtoneTitle: String?
  ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
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
    customRingtoneTitle: String?
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

  // Mode: Standard Task vs Fluid Twin (when creating new task)
  var isFluidTwinMode by remember { mutableStateOf(initialTask?.isFluidTwin ?: false) }

  var title by remember { mutableStateOf(initialTask?.title ?: "") }
  var startHour by remember { mutableIntStateOf((initialTask?.startTimeMinute ?: 540) / 60) }
  var startMinute by remember { mutableIntStateOf((initialTask?.startTimeMinute ?: 540) % 60) }
  var durationHours by remember { mutableIntStateOf((initialTask?.durationMinutes ?: 60) / 60) }
  var durationMins by remember { mutableIntStateOf((initialTask?.durationMinutes ?: 60) % 60) }

  // Fluid Twin Specific Settings
  var fluidPoolHours by remember { mutableIntStateOf((initialTask?.fluidTotalPoolMinutes ?: 360) / 60) }
  var fluidPoolMins by remember { mutableIntStateOf((initialTask?.fluidTotalPoolMinutes ?: 360) % 60) }
  var part2StartHour by remember { mutableIntStateOf(14) } // Default 2:00 PM
  var part2StartMinute by remember { mutableIntStateOf(0) }
  var part2DurationHours by remember { mutableIntStateOf(2) }
  var part2DurationMins by remember { mutableIntStateOf(30) }

  var voiceMessage by remember { mutableStateOf(initialTask?.voiceMessage ?: "") }
  var enableDnd by remember { mutableStateOf(initialTask?.enableDnd ?: false) }
  var enableHaptic by remember { mutableStateOf(initialTask?.enableHaptic ?: true) }
  var allowSplitting by remember { mutableStateOf(initialTask?.allowSplitting ?: true) }
  var isVoiceEnabled by remember { mutableStateOf(initialTask?.isVoiceEnabled ?: true) }
  var customNotificationTitle by remember { mutableStateOf(initialTask?.customNotificationTitle ?: "") }
  var customNotificationBody by remember { mutableStateOf(initialTask?.customNotificationBody ?: "") }

  // Ringtone / Notification Sound State
  var customRingtoneUri by remember { mutableStateOf(initialTask?.customRingtoneUri) }
  var customRingtoneTitle by remember { mutableStateOf(initialTask?.customRingtoneTitle ?: "Default Notification") }
  var isPreviewPlaying by remember { mutableStateOf(false) }
  var activeRingtonePlayer by remember { mutableStateOf<Ringtone?>(null) }

  // Confirmation dialog for delete
  var showDeleteConfirmDialog by remember { mutableStateOf(false) }

  var selectedDays by remember {
    mutableStateOf(initialTask?.selectedDays ?: RoutineDay.allDays())
  }

  // Ringtone Picker Activity Result Launcher
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
        customRingtoneTitle = r?.getTitle(context) ?: "Custom Notification Sound"
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
              isFluidTwinMode -> "Fluid Twin (2 Linked Sessions)"
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

      // Workflow Mode Selector (Only when creating a new dynamic task)
      if (!isEditing && !isAnchor) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          FilterChip(
            selected = !isFluidTwinMode,
            onClick = { isFluidTwinMode = false },
            label = { Text("Standard Session") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = AnchorGradientStart,
              selectedLabelColor = Color.White,
              containerColor = OneUiSurfaceElevated,
              labelColor = TextSecondary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          )

          FilterChip(
            selected = isFluidTwinMode,
            onClick = { isFluidTwinMode = true },
            label = { Text("Fluid Twin (2 Sessions)") },
            leadingIcon = {
              Icon(imageVector = Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(14.dp))
            },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFF8B5CF6),
              selectedLabelColor = Color.White,
              containerColor = OneUiSurfaceElevated,
              labelColor = TextSecondary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
          )
        }
        Spacer(modifier = Modifier.height(14.dp))
      }

      // Title Input
      Text(
        text = if (isFluidTwinMode) "Twin Routine Name" else "Task Title",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
      )
      Spacer(modifier = Modifier.height(6.dp))

      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        placeholder = { Text(if (isFluidTwinMode) "e.g., Deep Focus & Synthesis" else "e.g., Deep Coding Sprint", color = TextTertiary) },
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

      // Timing controls for Standard Tasks / Anchors vs Fluid Twins
      if (!isFluidTwinMode) {
        // Standard Session Time Picker & Duration
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
      } else {
        // ==========================================================
        // FLUID TWIN DURATION POOL & SESSIONS CONFIGURATION
        // ==========================================================
        Surface(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)), RoundedCornerShape(18.dp)),
          color = OneUiSurfaceElevated
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Default.CallSplit, contentDescription = null, tint = Color(0xFFC4B5FD), modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = "Combined Duration Pool", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Total Pool: ${fluidPoolHours}h ${fluidPoolMins}m (${fluidPoolHours * 60 + fluidPoolMins} mins total)",
              color = Color(0xFFC4B5FD),
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = { fluidPoolHours = (fluidPoolHours + 1).coerceIn(1, 10) },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, OneUiBorder)
              ) { Text("+1h Pool", fontSize = 11.sp, color = TextPrimary) }
              OutlinedButton(
                onClick = { fluidPoolMins = (fluidPoolMins + 30) % 60 },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, OneUiBorder)
              ) { Text("+30m Pool", fontSize = 11.sp, color = TextPrimary) }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Part 1
            Text(text = "Part 1: Morning/Day Block (Min 45m)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Start: ${String.format("%02d:%02d", startHour, startMinute)} • Target: ${durationHours}h ${durationMins}m",
                color = TextSecondary,
                fontSize = 12.sp
              )
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                  onClick = { startHour = (startHour + 1) % 24 },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) { Text("+1h", fontSize = 10.sp, color = TextPrimary) }
                OutlinedButton(
                  onClick = { durationMins = if (durationMins == 45) 0 else 45; if (durationMins == 0) durationHours = (durationHours + 1).coerceAtMost(5) },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) { Text("+45m", fontSize = 10.sp, color = TextPrimary) }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Part 2
            Text(text = "Part 2: Afternoon/Evening Block (Min 45m)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Start: ${String.format("%02d:%02d", part2StartHour, part2StartMinute)} • Target: ${part2DurationHours}h ${part2DurationMins}m",
                color = TextSecondary,
                fontSize = 12.sp
              )
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                  onClick = { part2StartHour = (part2StartHour + 1) % 24 },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) { Text("+1h", fontSize = 10.sp, color = TextPrimary) }
                OutlinedButton(
                  onClick = { part2DurationMins = if (part2DurationMins == 45) 0 else 45; if (part2DurationMins == 0) part2DurationHours = (part2DurationHours + 1).coerceAtMost(5) },
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) { Text("+45m", fontSize = 10.sp, color = TextPrimary) }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Notice: Fluid Twins share duration. If schedule pressure compresses one part against an anchor boundary, surplus flows to the other part. Neither part will cross an anchor slot.",
              color = TextTertiary,
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // ==============================================================
      // 2. ANDROID SYSTEM NOTIFICATION SOUND / RINGTONE PICKER
      // ==============================================================
      Text(
        text = "System Notification Sound",
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
                .size(36.dp)
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
                text = "Plays when task automation fires",
                color = TextTertiary,
                fontSize = 11.sp
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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

            // Choose Ringtone Button
            OutlinedButton(
              onClick = {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                  putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                  putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound")
                  putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                  putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                  if (customRingtoneUri != null) {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(customRingtoneUri))
                  }
                }
                ringtonePickerLauncher.launch(intent)
              },
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, AnchorGradientStart.copy(alpha = 0.6f)),
              modifier = Modifier.testTag("open_ringtone_picker_button")
            ) {
              Text("Change", fontSize = 11.sp, color = AnchorGradientStart, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

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

      // Custom Visual Notification Popup Fields
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = "Visual Popup Notification (Heads-Up Banner)",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
      )
      Spacer(modifier = Modifier.height(6.dp))

      OutlinedTextField(
        value = customNotificationTitle,
        onValueChange = { customNotificationTitle = it },
        placeholder = { Text("Custom Notification Title (defaults to task title)", color = TextTertiary, fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth().testTag("custom_notif_title_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = OneUiSurfaceElevated,
          unfocusedContainerColor = OneUiSurfaceElevated,
          focusedBorderColor = AnchorGradientStart,
          unfocusedBorderColor = OneUiBorder
        ),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedTextField(
        value = customNotificationBody,
        onValueChange = { customNotificationBody = it },
        placeholder = { Text("Custom Notification Body / Heads-Up Message", color = TextTertiary, fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth().testTag("custom_notif_body_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = OneUiSurfaceElevated,
          unfocusedContainerColor = OneUiSurfaceElevated,
          focusedBorderColor = AnchorGradientStart,
          unfocusedBorderColor = OneUiBorder
        ),
        shape = RoundedCornerShape(14.dp),
        maxLines = 2
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Weekly Recurrence: Sat-Fri
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
              customRingtoneTitle
            )
          } else if (isFluidTwinMode) {
            val totalPool = (fluidPoolHours * 60 + fluidPoolMins).coerceAtLeast(90)
            val p1Dur = calculatedDuration.coerceAtLeast(45)
            val p2Start = part2StartHour * 60 + part2StartMinute
            val p2Dur = (part2DurationHours * 60 + part2DurationMins).coerceAtLeast(45)
            onSaveFluidTwin(
              title,
              totalPool,
              calculatedStart,
              p1Dur,
              p2Start,
              p2Dur,
              enableDnd,
              selectedDays,
              isVoiceEnabled,
              customNotificationTitle,
              customNotificationBody,
              customRingtoneUri,
              customRingtoneTitle
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
              customRingtoneTitle = customRingtoneTitle
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
          containerColor = AnchorGradientStart,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp)
      ) {
        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Save & Apply Schedule", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }

      // ==============================================================
      // 5. MOVE TO TRASH ACTION (Inside Edit Form, Requirement 5)
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
