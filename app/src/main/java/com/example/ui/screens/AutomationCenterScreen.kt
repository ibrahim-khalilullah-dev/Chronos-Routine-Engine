package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.model.RoutineTask
import com.example.model.TaskType
import com.example.service.ActiveAutomationState
import com.example.service.AutomationManager
import com.example.ui.theme.AnchorGradientStart
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DynamicGradientEnd
import com.example.ui.theme.DynamicGradientStart
import com.example.ui.theme.OneUiBackground
import com.example.ui.theme.OneUiBorder
import com.example.ui.theme.OneUiSurfaceElevated
import com.example.ui.theme.OneUiSurfaceHigh
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber

@Composable
fun AutomationCenterScreen(
  isDndGranted: Boolean,
  isBatteryOptimizationIgnored: Boolean = true,
  activeAutomation: ActiveAutomationState = ActiveAutomationState(),
  onRefreshDndPermission: () -> Unit = {},
  onRequestBatteryOptimization: () -> Unit = {},
  onTestVoice: (RoutineTask) -> Unit = {},
  onStopAutomation: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(OneUiBackground)
      .statusBarsPadding()
      .animateContentSize()
      .verticalScroll(rememberScrollState())
      .padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    Column {
      Text(
        text = "Device Automation Engine",
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
      Text(
        text = "Hardware DND automation, background execution, and Exact Alarm Engine",
        fontSize = 12.sp,
        color = TextSecondary
      )
    }

    // Streamlined DND Automation Card
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .border(
          BorderStroke(
            1.dp,
            if (isDndGranted) Color(0xFF10B981).copy(alpha = 0.4f) else WarningAmber.copy(alpha = 0.4f)
          ),
          RoundedCornerShape(22.dp)
        ),
      color = OneUiSurfaceElevated
    ) {
      if (isDndGranted) {
        // Compact single-row status card matching Alarm and Battery Whitelist cards
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.DoNotDisturbOn,
              contentDescription = null,
              tint = Color(0xFFA5B4FC),
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Do Not Disturb (DND) Policy",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "Granted • Automation Active",
                fontSize = 11.sp,
                color = Color(0xFF10B981),
                fontWeight = FontWeight.Medium
              )
            }
          }

          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Granted",
            tint = Color(0xFF10B981),
            modifier = Modifier.size(22.dp)
          )
        }
      } else {
        // Permission not granted: show explanation and button to grant
        Column(modifier = Modifier.padding(18.dp)) {
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
                imageVector = Icons.Default.DoNotDisturbOn,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Do Not Disturb (DND) Policy",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                Text(
                  text = "Permission Required",
                  fontSize = 11.sp,
                  color = WarningAmber,
                  fontWeight = FontWeight.Medium
                )
              }
            }

            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = WarningAmber,
              modifier = Modifier.size(22.dp)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "Chronos engages PRIORITY DND mode automatically when an enabled task starts, and restores normal interruption settings immediately when it concludes.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
          )

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = {
              AutomationManager.openDndSettings(context)
              onRefreshDndPermission()
            },
            modifier = Modifier.fillMaxWidth().testTag("open_dnd_settings_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = WarningAmber,
              contentColor = Color.Black
            ),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Grant Permission",
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
          }
        }
      }
    }

    // Exact Alarm Capabilities Card
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(22.dp)),
      color = OneUiSurfaceElevated
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Alarm,
            contentDescription = null,
            tint = WarningAmber,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Exact Alarm Scheduling",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "SCHEDULE_EXACT_ALARM • RTC_WAKEUP",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }

        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "Active",
          tint = Color(0xFF10B981),
          modifier = Modifier.size(22.dp)
        )
      }
    }

    // Samsung One UI Background Optimization & Battery Whitelist Card
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .border(
          BorderStroke(
            1.dp,
            if (isBatteryOptimizationIgnored) Color(0xFF10B981).copy(alpha = 0.4f) else WarningAmber.copy(alpha = 0.4f)
          ),
          RoundedCornerShape(22.dp)
        ),
      color = OneUiSurfaceElevated
    ) {
      Column(modifier = Modifier.padding(18.dp)) {
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
              imageVector = Icons.Default.BatteryChargingFull,
              contentDescription = null,
              tint = if (isBatteryOptimizationIgnored) Color(0xFF10B981) else WarningAmber,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Samsung One UI Deep Sleep Whitelist",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = if (isBatteryOptimizationIgnored) {
                  "Unrestricted • Immune to Deep Sleep suspension"
                } else {
                  "Optimized • May be throttled in background"
                },
                fontSize = 11.sp,
                color = if (isBatteryOptimizationIgnored) Color(0xFF10B981) else WarningAmber
              )
            }
          }

          Icon(
            imageVector = if (isBatteryOptimizationIgnored) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isBatteryOptimizationIgnored) Color(0xFF10B981) else WarningAmber,
            modifier = Modifier.size(22.dp)
          )
        }

        if (!isBatteryOptimizationIgnored) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Samsung One UI and Android 14+ aggressively put routine apps to sleep. Whitelist Chronos to ensure lock-screen alarms and TTS reminders never get suppressed.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = onRequestBatteryOptimization,
            colors = ButtonDefaults.buttonColors(
              containerColor = WarningAmber,
              contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("whitelist_battery_button")
          ) {
            Text("Whitelist for One UI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(30.dp))
  }
}
