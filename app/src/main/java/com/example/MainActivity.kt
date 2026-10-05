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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.RoutineTask
import com.example.service.AutomationManager
import com.example.ui.NavigationTab
import com.example.ui.RoutineViewModel
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
  val sortMode by viewModel.sortMode.collectAsState()
  val deletedTasks by viewModel.deletedTasks.collectAsState()
  val currentMinute by viewModel.currentMinute.collectAsState()

  // Modal Sheet State
  var isModalOpen by remember { mutableStateOf(false) }
  var editingTask by remember { mutableStateOf<RoutineTask?>(null) }
  var showAddChooserSheet by remember { mutableStateOf(false) }

  // Runtime Notification Permission for Android 13+
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { /* Permission handled */ }
  )

  LaunchedEffect(Unit) {
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
            viewModel.selectTab(NavigationTab.TIMELINE)
          },
          icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Timeline") },
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
            showAddChooserSheet = true
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
              editingTask = task
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
            }
          )
        }

        NavigationTab.CONSOLE -> {
          ConsoleTerminalScreen(
            anchors = anchors,
            onApplyTasks = { newTasks, replaceAll ->
              viewModel.applyBulkConsoleTasks(newTasks, replaceAll)
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
          SettingsScreen(
            sleepDurationMinutes = sleepDurationMinutes,
            bufferMinutes = bufferMinutes,
            slot1Task = slot1,
            slot2Task = slot2,
            deletedCount = deletedTasks.size,
            onUpdateSleepDuration = { viewModel.updateSleepDuration(it) },
            onUpdateBufferMinutes = { viewModel.updateBufferMinutes(it) },
            onOpenTrashBin = { viewModel.selectTab(NavigationTab.TRASH) }
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

      // Unified "Add Task" Chooser Modal (Requirement 1: Single Unified Entry Point)
      if (showAddChooserSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
          onDismissRequest = { showAddChooserSheet = false },
          sheetState = sheetState,
          containerColor = OneUiSurfaceElevated,
          contentColor = TextPrimary,
          shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp, vertical = 12.dp)
              .padding(bottom = 36.dp)
          ) {
            Text(
              text = "Create Routine Task",
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "Choose your preferred scheduling method",
              fontSize = 12.sp,
              color = TextSecondary
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Option 1: Manual Visual Form (GUI)
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(18.dp))
                .clickable {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  showAddChooserSheet = false
                  editingTask = null
                  isModalOpen = true
                }
                .testTag("choose_manual_form_option"),
              color = OneUiSurfaceHigh
            ) {
              Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AnchorGradientStart),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(imageVector = Icons.Default.EditNote, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                  Text(
                    text = "Manual Form (GUI)",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Visual creator with duration pickers, DND toggles, and recurrence chips",
                    color = TextSecondary,
                    fontSize = 12.sp
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: Bulk Terminal Console
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(18.dp))
                .clickable {
                  haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                  showAddChooserSheet = false
                  viewModel.selectTab(NavigationTab.CONSOLE)
                }
                .testTag("choose_terminal_console_option"),
              color = OneUiSurfaceHigh
            ) {
              Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF080A0F))
                    .border(BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.5f)), CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = Color(0xFF06B6D4))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                  Text(
                    text = "Bulk Terminal Console",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Power-user pipe syntax with 7 protected anchors overview and validator",
                    color = TextSecondary,
                    fontSize = 12.sp
                  )
                }
              }
            }
          }
        }
      }

      // Visual GUI Form Modal Sheet
      if (isModalOpen) {
        TaskModalBottomSheet(
          initialTask = editingTask,
          onDismiss = {
            isModalOpen = false
            editingTask = null
          },
          onSaveTask = { task ->
            viewModel.saveDynamicTask(task)
          },
          onSaveFluidTwin = { title, pool, p1Start, p1Dur, p2Start, p2Dur, dnd, days, isVoice, nTitle, nBody, ringUri, ringTitle ->
            viewModel.saveFluidTwin(title, pool, p1Start, p1Dur, p2Start, p2Dur, dnd, days, isVoice, nTitle, nBody, ringUri, ringTitle)
          },
          onSaveAnchorDetails = { slot, title, start, duration, voice, dnd, haptic, days, isVoice, notifTitle, notifBody, ringUri, ringTitle ->
            viewModel.updateAnchorDetails(slot, title, start, duration, voice, dnd, haptic, days, isVoice, notifTitle, notifBody, ringUri, ringTitle)
          },
          onSaveSlot5 = { title, voice ->
            viewModel.updateSlot5Protected(title, voice)
          },
          onMoveToTrash = { taskId ->
            viewModel.moveToTrash(taskId)
          }
        )
      }
    }
  }
}
