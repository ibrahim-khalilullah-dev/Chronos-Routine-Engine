package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.RoutineTask
import com.example.service.AutomationManager
import com.example.ui.NavigationTab
import com.example.ui.RoutineViewModel
import com.example.ui.components.ChronosArchitectureGuideSheet
import com.example.ui.components.TaskModalBottomSheet
import com.example.ui.screens.AutomationCenterScreen
import com.example.ui.screens.ConsoleTerminalScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.screens.TrashBinScreen
import com.example.ui.theme.AnchorGradientStart
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OneUiBackground
import com.example.ui.theme.OneUiBorder
import com.example.ui.theme.OneUiSurface
import com.example.ui.theme.OneUiSurfaceElevated
import com.example.ui.theme.OneUiSurfaceHigh
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainScreen()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: RoutineViewModel = viewModel()) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current

  // State collectors
  val activeTab by viewModel.activeTab.collectAsState()
  val activeDay by viewModel.activeDay.collectAsState()
  val scheduleResult by viewModel.scheduleResult.collectAsState()
  val sleepDurationMinutes by viewModel.sleepDurationMinutes.collectAsState()
  val bufferMinutes by viewModel.bufferMinutes.collectAsState()
  val isDndGranted by viewModel.isDndGranted.collectAsState()
  val isBatteryOptimizationIgnored by viewModel.isBatteryOptimizationIgnored.collectAsState()
  val activeAutomation by viewModel.activeAutomation.collectAsState()
  val anchors by viewModel.anchors.collectAsState()
  val dynamicTasks by viewModel.dynamicTasks.collectAsState()
  val sortMode by viewModel.sortMode.collectAsState()
  val deletedTasks by viewModel.deletedTasks.collectAsState()
  val currentMinute by viewModel.currentMinute.collectAsState()
  val taskRatingHistory by viewModel.taskRatingHistory.collectAsState()

  // Modal Sheet State
  var isModalOpen by remember { mutableStateOf(false) }
  var editingTask by remember { mutableStateOf<RoutineTask?>(null) }
  var showAddChooserSheet by remember { mutableStateOf(false) }
  var showArchitectureGuideSheet by remember { mutableStateOf(false) }

  // Runtime Notification Permission for Android 13+
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { /* Permission handled */ }
  )

  LaunchedEffect(Unit) {
    viewModel.checkAndAutoRestoreVault()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
      ) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
    viewModel.refreshDndPermission()
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = OneUiBackground,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    bottomBar = {
      NavigationBar(
        modifier = Modifier
          .windowInsetsPadding(WindowInsets.navigationBars)
          .testTag("one_ui_bottom_navigation"),
        containerColor = OneUiSurface,
        contentColor = TextPrimary,
        tonalElevation = 6.dp
      ) {
        // Tab 1: Timeline
        NavigationBarItem(
          selected = activeTab == NavigationTab.TIMELINE,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            showAddChooserSheet = false
            viewModel.selectTab(NavigationTab.TIMELINE)
          },
          icon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Timeline") },
          label = { Text("Timeline", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White,
            selectedTextColor = TextPrimary,
            indicatorColor = AnchorGradientStart,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextSecondary
          ),
          modifier = Modifier.testTag("nav_tab_timeline")
        )

        // Tab 2: Single Unified "Add Task" Navigation Button (Requirement 1)
        NavigationBarItem(
          selected = showAddChooserSheet,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            showAddChooserSheet = !showAddChooserSheet
          },
          icon = {
            Icon(
              imageVector = Icons.Default.AddCircle,
              contentDescription = "Add Task",
              modifier = Modifier.size(24.dp)
            )
          },
          label = { Text("Add Task", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White,
            selectedTextColor = Color(0xFF67E8F9),
            indicatorColor = AnchorGradientStart,
            unselectedIconColor = Color(0xFF67E8F9),
            unselectedTextColor = Color(0xFF67E8F9)
          ),
          modifier = Modifier.testTag("nav_tab_add_task")
        )

        // Tab 3: Automation Center
        NavigationBarItem(
          selected = activeTab == NavigationTab.AUTOMATION,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            showAddChooserSheet = false
            viewModel.selectTab(NavigationTab.AUTOMATION)
            viewModel.refreshPermissions()
          },
          icon = { Icon(Icons.Default.Security, contentDescription = "Automation") },
          label = { Text("Automation", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White,
            selectedTextColor = TextPrimary,
            indicatorColor = AnchorGradientStart,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextSecondary
          ),
          modifier = Modifier.testTag("nav_tab_automation")
        )

        // Tab 4: Sleep & Engine
        NavigationBarItem(
          selected = activeTab == NavigationTab.SETTINGS,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            showAddChooserSheet = false
            viewModel.selectTab(NavigationTab.SETTINGS)
          },
          icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
          label = { Text("Engine", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.White,
            selectedTextColor = TextPrimary,
            indicatorColor = AnchorGradientStart,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextSecondary
          ),
          modifier = Modifier.testTag("nav_tab_settings")
        )
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = paddingValues.calculateBottomPadding())
    ) {
      when (activeTab) {
        NavigationTab.TIMELINE -> {
          TimelineScreen(
            activeDay = activeDay,
            onSelectDay = { viewModel.setActiveDay(it) },
            scheduleResult = scheduleResult,
            sleepDurationMinutes = sleepDurationMinutes,
            currentMinute = currentMinute,
            isDndGranted = isDndGranted,
            activeAutomation = activeAutomation,
            sortMode = sortMode,
            deletedCount = deletedTasks.size,
            onSelectSortMode = { viewModel.setSortMode(it) },
            onQuickAdjustAnchor = { slotNum, delta ->
              viewModel.quickAdjustAnchor(slotNum, delta)
            },
            onEditTask = { task ->
              val canonicalTask = if (task.slotNumber != null) {
                anchors.firstOrNull { it.slotNumber == task.slotNumber } ?: task
              } else {
                task
              }
              editingTask = canonicalTask
              isModalOpen = true
            },
            onMoveToTrash = { taskId ->
              viewModel.moveToTrash(taskId)
            },
            onOpenTrashBin = {
              viewModel.selectTab(NavigationTab.TRASH)
            },
            onStopAutomation = {
              viewModel.stopActiveAutomation()
            },
            onRefreshDndPermission = {
              viewModel.refreshDndPermission()
            },
            onRateTask = { task, rating ->
              viewModel.recordTaskRating(task, rating)
            },
            onDeleteBothFluidTwinParts = { twinId ->
              viewModel.deleteBothFluidTwinParts(twinId)
            },
            onDeleteTwinAndAbsorbTime = { toDeleteId, partnerId ->
              viewModel.deleteTwinAndAbsorbTime(toDeleteId, partnerId)
            }
          )
        }

        NavigationTab.CONSOLE -> {
          ConsoleTerminalScreen(
            anchors = anchors,
            dynamicTasks = dynamicTasks,
            sleepDurationMinutes = sleepDurationMinutes,
            bufferMinutes = bufferMinutes,
            onApplyTasks = { newTasks, replaceAll, parsedAnchors, sleepMins, bufMins ->
              viewModel.applyBulkConsoleTasks(newTasks, replaceAll, parsedAnchors, sleepMins, bufMins)
              viewModel.selectTab(NavigationTab.TIMELINE)
            },
            onBack = {
              viewModel.selectTab(NavigationTab.TIMELINE)
            }
          )
        }

        NavigationTab.AUTOMATION -> {
          AutomationCenterScreen(
            isDndGranted = isDndGranted,
            isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
            activeAutomation = activeAutomation,
            onRefreshDndPermission = { viewModel.refreshPermissions() },
            onRequestBatteryOptimization = {
              viewModel.requestBatteryOptimizationExemption()
              viewModel.refreshPermissions()
            },
            onTestVoice = { task ->
              AutomationManager.startAutomationService(context, task)
            },
            onStopAutomation = { viewModel.stopActiveAutomation() }
          )
        }

        NavigationTab.SETTINGS -> {
          val slot1 = anchors.firstOrNull { it.slotNumber == 1 }
          val slot2 = anchors.firstOrNull { it.slotNumber == 2 }
          val allTasks = (anchors + dynamicTasks).filter { !it.isDeleted }
          SettingsScreen(
            sleepDurationMinutes = sleepDurationMinutes,
            bufferMinutes = bufferMinutes,
            slot1Task = slot1,
            slot2Task = slot2,
            deletedCount = deletedTasks.size,
            scheduledTasks = scheduleResult.scheduledTasks,
            allTasks = allTasks,
            onUpdateSleepDuration = { viewModel.updateSleepDuration(it) },
            onUpdateBufferMinutes = { viewModel.updateBufferMinutes(it) },
            onOpenTrashBin = { viewModel.selectTab(NavigationTab.TRASH) },
            onPauseTask = { id, isAnchor, slot, days, todayOnly, indef ->
              viewModel.pauseTask(id, isAnchor, slot, days, todayOnly, indef)
            },
            onResumeTask = { id, isAnchor, slot ->
              viewModel.resumeTask(id, isAnchor, slot)
            },
            taskRatingHistory = taskRatingHistory
          )
        }

        NavigationTab.TRASH -> {
          BackHandler { viewModel.selectTab(NavigationTab.TIMELINE) }
          TrashBinScreen(
            deletedTasks = deletedTasks,
            onRestoreTask = { viewModel.restoreFromTrash(it) },
            onPurgePermanently = { viewModel.purgePermanently(it) },
            onEmptyTrash = { viewModel.emptyTrash() },
            onBack = { viewModel.selectTab(NavigationTab.TIMELINE) }
          )
        }
      }

      // Luxury Minimalist Floating Speed-Dial Menu (Anchored above Bottom Navigation Bar)
      BackHandler(enabled = showAddChooserSheet) {
        showAddChooserSheet = false
      }

      if (showAddChooserSheet) {
        // Dimmed backdrop overlay
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) {
              showAddChooserSheet = false
            }
        )
      }

      AnimatedVisibility(
        visible = showAddChooserSheet,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 12.dp, start = 20.dp, end = 20.dp)
      ) {
        Surface(
          modifier = Modifier
            .widthIn(max = 280.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(20.dp)),
          color = Color(0xFF0A0D14),
          tonalElevation = 16.dp,
          shadowElevation = 16.dp,
          shape = RoundedCornerShape(20.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            // 1. Manual Form Pill
            Surface(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showAddChooserSheet = false
                editingTask = null
                isModalOpen = true
              },
              shape = RoundedCornerShape(14.dp),
              color = Color(0xFF121622),
              border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.35f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("choose_manual_form_option")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF06B6D4).copy(alpha = 0.15f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                  )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                  text = "Manual Form",
                  color = Color(0xFFF8FAFC),
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
              }
            }

            // 2. Terminal Pill
            Surface(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showAddChooserSheet = false
                viewModel.selectTab(NavigationTab.CONSOLE)
              },
              shape = RoundedCornerShape(14.dp),
              color = Color(0xFF121622),
              border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("choose_terminal_console_option")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = Color(0xFFC4B5FD),
                    modifier = Modifier.size(18.dp)
                  )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                  text = "Terminal",
                  color = Color(0xFFF8FAFC),
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
              }
            }

            // 3. Documentation Pill
            Surface(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showAddChooserSheet = false
                showArchitectureGuideSheet = true
              },
              shape = RoundedCornerShape(14.dp),
              color = Color(0xFF121622),
              border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("choose_architecture_guide_option")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)
              ) {
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    tint = Color(0xFFFCD34D),
                    modifier = Modifier.size(18.dp)
                  )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                  text = "Documentation",
                  color = Color(0xFFF8FAFC),
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
              }
            }
          }
        }
      }

      // Visual GUI Form Modal Sheet
      if (isModalOpen) {
        val slot2Start = anchors.firstOrNull { it.slotNumber == 2 }?.startTimeMinute ?: 300
        TaskModalBottomSheet(
          initialTask = editingTask,
          onDismiss = {
            isModalOpen = false
            editingTask = null
          },
          onSaveTask = { task ->
            viewModel.saveDynamicTask(task)
          },
          onSaveFluidTwin = { request ->
            viewModel.saveFluidTwin(request)
          },
          onSaveAnchorDetails = { slot, title, start, duration, voice, dnd, haptic, days, isVoice, notifTitle, notifBody, ringUri, ringTitle, isSoundAlert, friTitle, friStart, friDur, friVoice, volume, dndDelay ->
            viewModel.updateAnchorDetails(
              slotNumber = slot,
              title = title,
              startMinute = start,
              durationMinutes = duration,
              voiceMessage = voice,
              enableDnd = dnd,
              enableHaptic = haptic,
              selectedDays = days,
              isVoiceEnabled = isVoice,
              customNotificationTitle = notifTitle,
              customNotificationBody = notifBody,
              customRingtoneUri = ringUri,
              customRingtoneTitle = ringTitle,
              isSoundAlertEnabled = isSoundAlert,
              fridayTitle = friTitle,
              fridayStartTimeMinute = friStart,
              fridayDurationMinutes = friDur,
              fridayVoiceMessage = friVoice,
              voiceVolume = volume,
              dndDelayMinutes = dndDelay
            )
          },
          onSaveSlot5 = { title, voice, days, dnd, dndDelay ->
            viewModel.updateSlot5Protected(title, voice, days, dnd, dndDelay)
          },
          onMoveToTrash = { taskId ->
            viewModel.moveToTrash(taskId)
          },
          slot2StartMinute = slot2Start,
          allDynamicTasks = dynamicTasks.filter { !it.isDeleted },
          onAbsorbTwin = { toDelete, partner ->
            viewModel.deleteTwinAndAbsorbTime(toDelete, partner)
          },
          onDeleteTwinBoth = { twinId ->
            viewModel.deleteBothFluidTwinParts(twinId)
          }
        )
      }

      // In-App Architecture Guide Modal Sheet (Requirement 9)
      if (showArchitectureGuideSheet) {
        ChronosArchitectureGuideSheet(
          anchors = anchors,
          onDismiss = { showArchitectureGuideSheet = false }
        )
      }
    }
  }
}
