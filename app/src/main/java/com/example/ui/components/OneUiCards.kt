package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.ScheduleGap
import com.example.ui.theme.AnchorBrush
import com.example.ui.theme.AnchorGradientEnd
import com.example.ui.theme.AnchorGradientStart
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DynamicBrush
import com.example.ui.theme.DynamicGradientEnd
import com.example.ui.theme.DynamicGradientStart
import com.example.ui.theme.DynamicSubtleBrush
import com.example.ui.theme.ElasticBrush
import com.example.ui.theme.ElasticGradientEnd
import com.example.ui.theme.ElasticGradientStart
import com.example.ui.theme.OneUiBorder
import com.example.ui.theme.OneUiSurfaceElevated
import com.example.ui.theme.OneUiSurfaceHigh
import com.example.ui.theme.OverflowBrush
import com.example.ui.theme.OverflowSubtleBrush
import com.example.ui.theme.SleepBrush
import com.example.ui.theme.SleepGradientEnd
import com.example.ui.theme.SleepGradientStart
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.gradientBrushWithAlpha

fun isTaskActiveAt(task: RoutineTask, currentMinute: Int): Boolean {
  if (currentMinute < 0) return false
  val start = task.startTimeMinute
  val end = task.startTimeMinute + task.durationMinutes
  return if (end <= 1440) {
    currentMinute in start until end
  } else {
    currentMinute >= start || currentMinute < (end % 1440)
  }
}

fun isTaskCompletedAt(task: RoutineTask, currentMinute: Int): Boolean {
  if (currentMinute < 0) return false
  val start = task.startTimeMinute
  val end = task.startTimeMinute + task.durationMinutes
  return if (end <= 1440) {
    currentMinute >= end && currentMinute >= start
  } else {
    currentMinute >= (end % 1440) && currentMinute < start
  }
}

val taskRatingCache = mutableStateMapOf<String, Int>()

@Composable
fun TaskCompletionRatingBar(
  task: RoutineTask,
  onRate: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val haptic = LocalHapticFeedback.current
  val existingRating = taskRatingCache[task.id]
  var pendingPct by remember(task.id) { mutableStateOf<Int?>(null) }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(14.dp)),
    color = Color.Black.copy(alpha = 0.35f),
    shape = RoundedCornerShape(14.dp)
  ) {
    Column(
      modifier = Modifier
        .animateContentSize()
        .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
      if (existingRating != null) {
        // 5-Star Display State
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Focus Rating:",
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.width(8.dp))
            val starCount = when {
              existingRating >= 100 -> 5
              existingRating >= 75 -> 4
              existingRating >= 50 -> 3
              existingRating >= 25 -> 1
              else -> 0
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
              for (i in 1..5) {
                Icon(
                  imageVector = if (i <= starCount) Icons.Default.Star else Icons.Default.StarBorder,
                  contentDescription = null,
                  tint = if (i <= starCount) Color(0xFFFBBF24) else Color(0xFF475569),
                  modifier = Modifier.size(16.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "$existingRating%",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF10B981)
            )
          }

          IconButton(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              taskRatingCache.remove(task.id)
            },
            modifier = Modifier.size(24.dp).testTag("re_rate_task_${task.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Change Rating",
              tint = TextTertiary,
              modifier = Modifier.size(13.dp)
            )
          }
        }
      } else {
        // Selection State
        Text(
          text = "Rate Performance & Focus Level:",
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF94A3B8)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val percentages = listOf(0, 25, 50, 75, 100)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          percentages.forEach { pct ->
            val isPending = pendingPct == pct
            Surface(
              onClick = {
                if (isPending) {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  taskRatingCache[task.id] = pct
                  onRate(pct)
                  pendingPct = null
                } else {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  pendingPct = pct
                }
              },
              shape = RoundedCornerShape(10.dp),
              color = if (isPending) Color(0xFF10B981).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
              border = BorderStroke(
                width = if (isPending) 1.5.dp else 1.dp,
                color = if (isPending) Color(0xFF10B981) else Color.White.copy(alpha = 0.15f)
              ),
              modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .testTag("rate_pill_${task.id}_$pct")
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
              ) {
                Text(
                  text = if (isPending) "Save ✓" else "$pct%",
                  fontSize = 11.sp,
                  fontWeight = if (isPending) FontWeight.ExtraBold else FontWeight.SemiBold,
                  color = if (isPending) Color(0xFF10B981) else TextPrimary
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun AnchorSlotCard(
  task: RoutineTask,
  sleepDurationMinutes: Int = 420,
  currentMinute: Int = -1,
  onQuickAdjust: (deltaMinutes: Int) -> Unit,
  onEditClick: () -> Unit,
  onRateTask: ((taskId: String, ratingPct: Int) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val slotNum = task.slotNumber ?: 0
  val isSleepSlot = slotNum == 1
  val isElasticSlot = slotNum == 5
  val isStandardAnchor = !isSleepSlot && !isElasticSlot
  val isActive = isTaskActiveAt(task, currentMinute)

  val gradientBrush: Brush = when {
    isSleepSlot -> SleepBrush
    isElasticSlot -> ElasticBrush
    else -> AnchorBrush
  }

  val subtleGradientBrush: Brush = when {
    isSleepSlot -> gradientBrushWithAlpha(SleepGradientStart, SleepGradientEnd, 0.18f)
    isElasticSlot -> gradientBrushWithAlpha(ElasticGradientStart, ElasticGradientEnd, 0.18f)
    else -> gradientBrushWithAlpha(AnchorGradientStart, AnchorGradientEnd, 0.18f)
  }

  val haptic = LocalHapticFeedback.current
  var pendingDeltaMinutes by remember(task.startTimeMinute) { mutableIntStateOf(0) }

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(26.dp))
      .border(
        BorderStroke(
          width = if (isActive) 2.dp else 1.dp,
          color = if (isActive) Color(0xFF10B981) else OneUiBorder
        ),
        RoundedCornerShape(26.dp)
      )
      .testTag("anchor_card_slot_$slotNum"),
    color = OneUiSurfaceElevated,
    shape = RoundedCornerShape(26.dp),
    tonalElevation = if (isActive) 8.dp else 4.dp
  ) {
    Column(
      modifier = Modifier
        .animateContentSize()
        .background(subtleGradientBrush)
        .padding(18.dp)
    ) {
      // Header row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(gradientBrush),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (isSleepSlot) "🌙" else if (isElasticSlot) "🛡️" else "$slotNum",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = task.title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (isSleepSlot || isElasticSlot) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = "Calculated Slot",
                  tint = WarningAmber,
                  modifier = Modifier.size(15.dp)
                )
              }
            }
            Text(
              text = when {
                isSleepSlot -> "Reverse Sleep Tracker • Linked to Slot 2"
                isElasticSlot -> "100% Protected Elastic Buffer"
                else -> "Immutable Anchor Slot $slotNum"
              },
              color = TextSecondary,
              fontSize = 12.sp
            )
          }
        }

        // Live badge or edit button
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isActive) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF10B981).copy(alpha = 0.2f))
                .border(BorderStroke(1.dp, Color(0xFF10B981)), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "LIVE NOW 🟢",
                color = Color(0xFF10B981),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
          }

          // In pending adjustment state: Replace edit pencil with [ ✓ ] and [ ✕ ]
          if (pendingDeltaMinutes != 0) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              // Green Checkmark [ ✓ ]
              IconButton(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  onQuickAdjust(pendingDeltaMinutes)
                  pendingDeltaMinutes = 0
                },
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF10B981).copy(alpha = 0.15f))
                  .border(BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f)), CircleShape)
                  .testTag("save_quick_adjust_slot_$slotNum")
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Confirm shift",
                  tint = Color(0xFF10B981),
                  modifier = Modifier.size(18.dp)
                )
              }

              // Crimson Cross [ ✕ ]
              IconButton(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  pendingDeltaMinutes = 0
                },
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                  .border(BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f)), CircleShape)
                  .testTag("cancel_quick_adjust_slot_$slotNum")
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Cancel shift",
                  tint = Color(0xFFEF4444),
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          } else {
            IconButton(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onEditClick()
              },
              modifier = Modifier.size(36.dp).testTag("edit_slot_$slotNum")
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Anchor",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }
      }

      // Live progress timer if currently active, or task completion rating if completed
      if (isActive && currentMinute >= 0) {
        Spacer(modifier = Modifier.height(10.dp))
        ActiveTaskLiveProgressBar(task = task, currentMinute = currentMinute, accentColor = Color(0xFF10B981))
      } else if (isTaskCompletedAt(task, currentMinute) || taskRatingCache.containsKey(task.id)) {
        Spacer(modifier = Modifier.height(10.dp))
        TaskCompletionRatingBar(task = task, onRate = { r -> onRateTask?.invoke(task.id, r) })
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Time Schedule Details
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (isSleepSlot) Icons.Default.Bedtime else Icons.Default.Schedule,
              contentDescription = null,
              tint = Color.White.copy(alpha = 0.8f),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            val effectiveStart = (task.startTimeMinute + pendingDeltaMinutes + 1440) % 1440
            val effectiveEnd = (effectiveStart + task.durationMinutes) % 1440
            Text(
              text = if (pendingDeltaMinutes != 0) {
                "${RoutineTask.formatTime(effectiveStart)} → ${RoutineTask.formatTime(effectiveEnd)}"
              } else {
                "${task.formattedStartTime} → ${task.formattedEndTime}"
              },
              color = if (pendingDeltaMinutes != 0) WarningAmber else TextPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 19.sp,
              fontFamily = FontFamily.SansSerif
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = task.formattedDuration,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }

            if (pendingDeltaMinutes != 0) {
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (pendingDeltaMinutes > 0) Color(0xFF10B981).copy(alpha = 0.25f) else WarningAmber.copy(alpha = 0.25f))
                  .border(1.dp, if (pendingDeltaMinutes > 0) Color(0xFF10B981) else WarningAmber, RoundedCornerShape(8.dp))
                  .padding(horizontal = 8.dp, vertical = 3.dp)
              ) {
                Text(
                  text = "${if (pendingDeltaMinutes > 0) "+$pendingDeltaMinutes" else "$pendingDeltaMinutes"} min",
                  color = if (pendingDeltaMinutes > 0) Color(0xFF10B981) else WarningAmber,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.ExtraBold
                )
              }
            }

            if (task.enableDnd) {
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFF6366F1).copy(alpha = 0.25f))
                  .padding(horizontal = 6.dp, vertical = 3.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.DoNotDisturbOn,
                    contentDescription = "DND Active",
                    tint = Color(0xFFA5B4FC),
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(text = "DND", color = Color(0xFFA5B4FC), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }
            }

            if (task.enableHaptic) {
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.Default.Vibration,
                contentDescription = "Haptic Vibration",
                tint = TextTertiary,
                modifier = Modifier.size(14.dp)
              )
            }

            // Voice status icon
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = if (task.isVoiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
              contentDescription = if (task.isVoiceEnabled) "Voice On" else "Voice Muted",
              tint = if (task.isVoiceEnabled) Color(0xFF67E8F9) else TextTertiary,
              modifier = Modifier.size(14.dp)
            )
          }
        }

        // Quick Adjust +/- 5 min buttons for Standard Anchors with Accumulative Batch Save Protection
        if (isStandardAnchor) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // Minus 5m Button (accumulative: allows consecutive taps)
            Surface(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                pendingDeltaMinutes -= 5
              },
              shape = RoundedCornerShape(12.dp),
              color = Color.White.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
              modifier = Modifier
                .height(34.dp)
                .testTag("quick_minus_slot_$slotNum")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
              ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Minus 5 min", tint = TextPrimary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = "5m", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
              }
            }

            // Plus 5m Button (accumulative: allows consecutive taps)
            Surface(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                pendingDeltaMinutes += 5
              },
              shape = RoundedCornerShape(12.dp),
              color = Color.White.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
              modifier = Modifier
                .height(34.dp)
                .testTag("quick_plus_slot_$slotNum")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
              ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Plus 5 min", tint = TextPrimary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = "5m", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
              }
            }
          }
        } else if (isSleepSlot) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(Color.White.copy(alpha = 0.08f))
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = "Auto-Synced to Slot 2",
              color = Color(0xFFA5B4FC),
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        } else if (isElasticSlot) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(Color.White.copy(alpha = 0.08f))
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = "Auto-Spanned (Slot 4→6)",
              color = WarningAmber,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      // Voice Reminder quote
      if (task.voiceMessage.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = null,
              tint = TextTertiary,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "“${task.voiceMessage}”",
              color = TextSecondary,
              fontSize = 11.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }
    }
  }
}

@Composable
fun DynamicTaskCard(
  task: RoutineTask,
  currentMinute: Int = -1,
  onEditClick: () -> Unit,
  onRateTask: ((taskId: String, ratingPct: Int) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val haptic = LocalHapticFeedback.current
  val isActive = isTaskActiveAt(task, currentMinute)

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .border(
        BorderStroke(
          width = if (isActive) 2.dp else 1.dp,
          color = if (isActive) DynamicGradientEnd else OneUiBorder
        ),
        RoundedCornerShape(24.dp)
      )
      .testTag("dynamic_task_card_${task.id}"),
    color = OneUiSurfaceElevated,
    shape = RoundedCornerShape(24.dp),
    tonalElevation = if (isActive) 7.dp else 3.dp
  ) {
    Column(
      modifier = Modifier
        .animateContentSize()
        .background(DynamicSubtleBrush)
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = task.title,
              color = TextPrimary,
              fontWeight = FontWeight.SemiBold,
              fontSize = 16.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )

            if (task.isFluidTwin) {
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF8B5CF6).copy(alpha = 0.25f))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "Fluid Twin P${task.fluidTwinPart ?: 1}",
                  color = Color(0xFFC4B5FD),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            } else if (task.isSplitPart) {
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFF06B6D4).copy(alpha = 0.25f))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.CallSplit,
                    contentDescription = "Split Part",
                    tint = Color(0xFF67E8F9),
                    modifier = Modifier.size(11.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = task.splitPartLabel ?: "Split",
                    color = Color(0xFF67E8F9),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }

          Text(
            text = if (task.isFluidTwin) "Fluid Twin Liquid Session • ${task.fluidTotalPoolMinutes ?: task.durationMinutes}m Pool" else "Dynamic Scheduled Session",
            color = TextSecondary,
            fontSize = 12.sp
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isActive) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(DynamicGradientEnd.copy(alpha = 0.2f))
                .border(BorderStroke(1.dp, DynamicGradientEnd), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "LIVE NOW 🟢",
                color = DynamicGradientEnd,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
          }

          // Dedicated Edit Pencil Button (Requirement 4)
          IconButton(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onEditClick()
            },
            modifier = Modifier.size(36.dp).testTag("edit_task_${task.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Edit Task",
              tint = TextSecondary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      // Live progress timer if currently active, or task completion rating if completed
      if (isActive && currentMinute >= 0) {
        Spacer(modifier = Modifier.height(10.dp))
        ActiveTaskLiveProgressBar(task = task, currentMinute = currentMinute, accentColor = DynamicGradientEnd)
      } else if (isTaskCompletedAt(task, currentMinute) || taskRatingCache.containsKey(task.id)) {
        Spacer(modifier = Modifier.height(10.dp))
        TaskCompletionRatingBar(task = task, onRate = { r -> onRateTask?.invoke(task.id, r) })
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = TextPrimary.copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${task.formattedStartTime} → ${task.formattedEndTime}",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            fontFamily = FontFamily.SansSerif
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(Color.White.copy(alpha = 0.1f))
              .padding(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text(
              text = task.formattedDuration,
              color = TextPrimary,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }

          if (task.enableDnd) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Default.DoNotDisturbOn,
              contentDescription = "DND",
              tint = Color(0xFFA5B4FC),
              modifier = Modifier.size(14.dp)
            )
          }

          // Voice on/off indicator
          Spacer(modifier = Modifier.width(6.dp))
          Icon(
            imageVector = if (task.isVoiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeOff,
            contentDescription = if (task.isVoiceEnabled) "Voice Enabled" else "Voice Muted",
            tint = if (task.isVoiceEnabled) Color(0xFF67E8F9) else TextTertiary,
            modifier = Modifier.size(14.dp)
          )
        }
      }

      // Custom notification / voice pill preview
      if (task.customNotificationTitle.isNotBlank() || task.voiceMessage.isNotBlank() || task.customRingtoneTitle != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              tint = Color(0xFF67E8F9),
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = task.notificationDisplayBody,
              color = TextSecondary.copy(alpha = 0.85f),
              fontSize = 11.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          if (task.customRingtoneTitle != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "🔔 ${task.customRingtoneTitle}",
              color = Color(0xFFA5B4FC),
              fontSize = 10.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }
  }
}

@Composable
fun ActiveTaskLiveProgressBar(
  task: RoutineTask,
  currentMinute: Int,
  accentColor: Color
) {
  val start = task.startTimeMinute
  val duration = task.durationMinutes
  val elapsed = if (currentMinute >= start) {
    currentMinute - start
  } else {
    (1440 - start) + currentMinute
  }.coerceIn(0, duration)
  val remaining = (duration - elapsed).coerceAtLeast(0)
  val progress = if (duration > 0) (elapsed.toFloat() / duration).coerceIn(0f, 1f) else 0f
  val pct = (progress * 100).toInt()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(Color.Black.copy(alpha = 0.35f))
      .padding(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Elapsed: ${elapsed}m",
        color = TextSecondary,
        fontSize = 11.sp
      )
      Text(
        text = "Remaining: ${remaining}m ($pct%)",
        color = accentColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Progress bar track
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(3.dp))
        .background(Color.White.copy(alpha = 0.12f))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(fraction = progress.coerceIn(0.02f, 1f))
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(
            Brush.horizontalGradient(
              listOf(accentColor.copy(alpha = 0.7f), accentColor)
            )
          )
      )
    }
  }
}

@Composable
fun OverflowTaskCard(
  task: RoutineTask,
  onEditClick: () -> Unit,
  onDeleteClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val haptic = LocalHapticFeedback.current

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .border(BorderStroke(1.dp, DangerRed.copy(alpha = 0.35f)), RoundedCornerShape(22.dp))
      .clickable {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onEditClick()
      }
      .testTag("overflow_task_${task.id}"),
    color = OneUiSurfaceElevated,
    shape = RoundedCornerShape(22.dp),
    tonalElevation = 2.dp
  ) {
    Column(
      modifier = Modifier
        .animateContentSize()
        .background(OverflowSubtleBrush)
        .padding(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Pending Overflow",
            tint = WarningAmber,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = task.title,
              color = TextPrimary,
              fontWeight = FontWeight.SemiBold,
              fontSize = 15.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "Duration: ${task.formattedDuration} • Pref: ${task.formattedStartTime}",
              color = TextSecondary,
              fontSize = 11.sp
            )
          }
        }

        IconButton(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onEditClick()
          },
          modifier = Modifier.size(32.dp).testTag("edit_overflow_${task.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit Overflow Task",
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      if (task.overflowReason != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = task.overflowReason,
          color = DangerRed.copy(alpha = 0.9f),
          fontSize = 11.sp,
          lineHeight = 14.sp
        )
      }
    }
  }
}
