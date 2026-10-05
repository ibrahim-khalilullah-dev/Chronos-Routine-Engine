package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RoutineRepository
import com.example.engine.ScheduleResult
import com.example.engine.SchedulerEngine
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.TaskType
import com.example.service.ActiveAutomationState
import com.example.service.AutomationManager
import com.example.service.RoutineAutomationService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import java.util.UUID

enum class NavigationTab(val title: String) {
  TIMELINE("Timeline"),
  CONSOLE("Bulk Terminal"),
  AUTOMATION("Automation"),
  SETTINGS("Sleep & Engine"),
  TRASH("Trash Bin")
}

enum class TimelineSortMode(val label: String) {
  EARLIEST_START("Earliest Start"),
  LATEST_START("Latest Start"),
  LONGEST_DURATION("Longest Duration"),
  SHORTEST_DURATION("Shortest Duration"),
  ACTIVE_FIRST("Active Task on Top"),
  HORIZON_REVERSE("Horizon / Last Task First")
}

class RoutineViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = RoutineRepository(application)
  private val scheduleMutex = Mutex()

  private val _activeTab = MutableStateFlow(NavigationTab.TIMELINE)
  val activeTab: StateFlow<NavigationTab> = _activeTab.asStateFlow()

  private val _activeDay = MutableStateFlow(repository.getActiveDay())
  val activeDay: StateFlow<RoutineDay> = _activeDay.asStateFlow()

  private val _sortMode = MutableStateFlow(TimelineSortMode.EARLIEST_START)
  val sortMode: StateFlow<TimelineSortMode> = _sortMode.asStateFlow()

  private val _sleepDurationMinutes = MutableStateFlow(repository.getSleepDurationMinutes())
  val sleepDurationMinutes: StateFlow<Int> = _sleepDurationMinutes.asStateFlow()

  private val _bufferMinutes = MutableStateFlow(repository.getBufferMinutes())
  val bufferMinutes: StateFlow<Int> = _bufferMinutes.asStateFlow()

  private val _anchors = MutableStateFlow(repository.getAnchors())
  val anchors: StateFlow<List<RoutineTask>> = _anchors.asStateFlow()

  private val _dynamicTasks = MutableStateFlow(repository.getDynamicTasks())
  val dynamicTasks: StateFlow<List<RoutineTask>> = _dynamicTasks.asStateFlow()

  private val _deletedTasks = MutableStateFlow(repository.getDeletedTasks())
  val deletedTasks: StateFlow<List<RoutineTask>> = _deletedTasks.asStateFlow()

  private val _currentMinute = MutableStateFlow(getCurrentMinuteOfDay())
  val currentMinute: StateFlow<Int> = _currentMinute.asStateFlow()

  private val _isDndGranted = MutableStateFlow(AutomationManager.isDndPermissionGranted(application))
  val isDndGranted: StateFlow<Boolean> = _isDndGranted.asStateFlow()

  private val _isBatteryOptimizationIgnored = MutableStateFlow(
    AutomationManager.isIgnoringBatteryOptimizations(application)
  )
  val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()

  val activeAutomation: StateFlow<ActiveAutomationState> = RoutineAutomationService.serviceState

  init {
    // Clock synchronization ticker for real-time progress bar & auto-scroll
    viewModelScope.launch {
      while (isActive) {
        _currentMinute.value = getCurrentMinuteOfDay()
        delay(15000L) // Refresh every 15 seconds
      }
    }
  }

  private fun getCurrentMinuteOfDay(): Int {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
  }

  val scheduleResult: StateFlow<ScheduleResult> = combine(
    _anchors,
    _dynamicTasks,
    _activeDay,
    _bufferMinutes,
    _sortMode,
    _currentMinute
  ) { args: Array<Any?> ->
    @Suppress("UNCHECKED_CAST")
    val currentAnchors = args[0] as List<RoutineTask>
    @Suppress("UNCHECKED_CAST")
    val currentDynamics = args[1] as List<RoutineTask>
    val day = args[2] as RoutineDay
    val buffer = args[3] as Int
    val sort = args[4] as TimelineSortMode
    val currMin = args[5] as Int

    val activeDynamics = currentDynamics.filter { !it.isDeleted }
    val rawResult = SchedulerEngine.scheduleDay(
      anchors = currentAnchors,
      dynamicTasks = activeDynamics,
      activeDay = day,
      interTaskBufferMinutes = buffer
    )

    val sortedScheduled = when (sort) {
      TimelineSortMode.EARLIEST_START -> rawResult.scheduledTasks.sortedBy { it.startTimeMinute }
      TimelineSortMode.LATEST_START -> rawResult.scheduledTasks.sortedByDescending { it.startTimeMinute }
      TimelineSortMode.LONGEST_DURATION -> rawResult.scheduledTasks.sortedByDescending { it.durationMinutes }
      TimelineSortMode.SHORTEST_DURATION -> rawResult.scheduledTasks.sortedBy { it.durationMinutes }
      TimelineSortMode.ACTIVE_FIRST -> {
        val active = rawResult.scheduledTasks.filter { com.example.ui.components.isTaskActiveAt(it, currMin) }
        val upcoming = rawResult.scheduledTasks.filter { !com.example.ui.components.isTaskActiveAt(it, currMin) && it.startTimeMinute >= currMin }.sortedBy { it.startTimeMinute }
        val completed = rawResult.scheduledTasks.filter { !com.example.ui.components.isTaskActiveAt(it, currMin) && it.startTimeMinute < currMin }.sortedBy { it.startTimeMinute }
        active + upcoming + completed
      }
      TimelineSortMode.HORIZON_REVERSE -> rawResult.scheduledTasks.sortedByDescending { it.startTimeMinute + it.durationMinutes }
      else -> rawResult.scheduledTasks
    }

    rawResult.copy(scheduledTasks = sortedScheduled)
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = SchedulerEngine.scheduleDay(
      _anchors.value,
      _dynamicTasks.value.filter { !it.isDeleted },
      _activeDay.value,
      _bufferMinutes.value
    )
  )

  fun selectTab(tab: NavigationTab) {
    _activeTab.value = tab
    if (tab == NavigationTab.TRASH) {
      refreshTrash()
    }
  }

  fun setActiveDay(day: RoutineDay) {
    _activeDay.value = day
    repository.setActiveDay(day)
  }

  fun setSortMode(mode: TimelineSortMode) {
    _sortMode.value = mode
  }

  fun refreshPermissions() {
    _isDndGranted.value = AutomationManager.isDndPermissionGranted(getApplication())
    _isBatteryOptimizationIgnored.value = AutomationManager.isIgnoringBatteryOptimizations(getApplication())
  }

  fun refreshDndPermission() = refreshPermissions()

  fun requestBatteryOptimizationExemption() {
    AutomationManager.requestIgnoreBatteryOptimizations(getApplication())
  }

  fun quickAdjustAnchor(slotNumber: Int, deltaMinutes: Int) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        val current = _anchors.value
        val updated = SchedulerEngine.adjustAnchor(
          anchors = current,
          slotNumber = slotNumber,
          deltaMinutes = deltaMinutes,
          sleepDurationMinutes = _sleepDurationMinutes.value
        )
        _anchors.value = updated
        repository.saveAnchors(updated)
      }
    }
  }

  fun updateSleepDuration(hours: Float) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        val mins = (hours * 60).toInt().coerceIn(240, 720)
        _sleepDurationMinutes.value = mins
        repository.setSleepDurationMinutes(mins)
        val recalculated = SchedulerEngine.calculateAnchors(_anchors.value, mins)
        _anchors.value = recalculated
        repository.saveAnchors(recalculated)
      }
    }
  }

  fun updateBufferMinutes(minutes: Int) {
    _bufferMinutes.value = minutes.coerceIn(0, 30)
    repository.setBufferMinutes(_bufferMinutes.value)
  }

  fun updateSlot5Protected(title: String, voiceMessage: String) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        val current = _anchors.value.map { task ->
          if (task.slotNumber == 5) {
            task.copy(
              title = title.ifEmpty { "Elastic Rubber Buffer" },
              voiceMessage = voiceMessage
            )
          } else {
            task
          }
        }
        _anchors.value = current
        repository.saveAnchors(current)
      }
    }
  }

  fun updateAnchorDetails(
    slotNumber: Int,
    title: String,
    startMinute: Int,
    durationMinutes: Int,
    voiceMessage: String,
    enableDnd: Boolean,
    enableHaptic: Boolean,
    selectedDays: Set<RoutineDay>,
    isVoiceEnabled: Boolean = true,
    customNotificationTitle: String = "",
    customNotificationBody: String = "",
    customRingtoneUri: String? = null,
    customRingtoneTitle: String? = null
  ) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        val current = _anchors.value.map { task ->
          if (task.slotNumber == slotNumber) {
            task.copy(
              title = title,
              startTimeMinute = startMinute,
              durationMinutes = durationMinutes,
              voiceMessage = voiceMessage,
              enableDnd = enableDnd,
              enableHaptic = enableHaptic,
              selectedDays = selectedDays,
              isVoiceEnabled = isVoiceEnabled,
              customNotificationTitle = customNotificationTitle,
              customNotificationBody = customNotificationBody,
              customRingtoneUri = customRingtoneUri,
              customRingtoneTitle = customRingtoneTitle
            )
          } else {
            task
          }
        }
        val recalculated = SchedulerEngine.calculateAnchors(current, _sleepDurationMinutes.value)
        _anchors.value = recalculated
        repository.saveAnchors(recalculated)
      }
    }
  }

  fun saveDynamicTask(task: RoutineTask) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        val list = _dynamicTasks.value.filter { it.id != task.id && it.parentTaskId != task.id }.toMutableList()
        list.add(task.copy(isDeleted = false, deletedTimestamp = null))
        _dynamicTasks.value = list
        repository.saveDynamicTasks(list)
        AutomationManager.scheduleTaskAlarms(getApplication(), task)
      }
    }
  }

  fun saveFluidTwin(
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
    customRingtoneUri: String? = null,
    customRingtoneTitle: String? = null
  ) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        val twinId = "fluid_${UUID.randomUUID().toString().take(8)}"
        val part1 = RoutineTask(
          id = "${twinId}_p1",
          title = "$title (Part 1)",
          type = TaskType.DYNAMIC,
          startTimeMinute = part1StartMinute,
          durationMinutes = part1Duration.coerceAtLeast(45),
          voiceMessage = "$title Part 1 starting now.",
          enableDnd = enableDnd,
          enableHaptic = true,
          allowSplitting = false,
          selectedDays = selectedDays,
          isVoiceEnabled = isVoiceEnabled,
          customNotificationTitle = customNotificationTitle.ifBlank { "$title (Part 1)" },
          customNotificationBody = customNotificationBody,
          isFluidTwin = true,
          fluidTwinId = twinId,
          fluidTwinPart = 1,
          fluidTotalPoolMinutes = totalPoolMinutes,
          customRingtoneUri = customRingtoneUri,
          customRingtoneTitle = customRingtoneTitle
        )
        val part2 = RoutineTask(
          id = "${twinId}_p2",
          title = "$title (Part 2)",
          type = TaskType.DYNAMIC,
          startTimeMinute = part2StartMinute,
          durationMinutes = part2Duration.coerceAtLeast(45),
          voiceMessage = "$title Part 2 starting now.",
          enableDnd = enableDnd,
          enableHaptic = true,
          allowSplitting = false,
          selectedDays = selectedDays,
          isVoiceEnabled = isVoiceEnabled,
          customNotificationTitle = customNotificationTitle.ifBlank { "$title (Part 2)" },
          customNotificationBody = customNotificationBody,
          isFluidTwin = true,
          fluidTwinId = twinId,
          fluidTwinPart = 2,
          fluidTotalPoolMinutes = totalPoolMinutes,
          customRingtoneUri = customRingtoneUri,
          customRingtoneTitle = customRingtoneTitle
        )

        val list = _dynamicTasks.value.toMutableList()
        list.add(part1)
        list.add(part2)
        _dynamicTasks.value = list
        repository.saveDynamicTasks(list)
        AutomationManager.scheduleTaskAlarms(getApplication(), part1)
        AutomationManager.scheduleTaskAlarms(getApplication(), part2)
      }
    }
  }

  fun moveToTrash(taskId: String) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        val updated = repository.softDeleteTask(taskId)
        _dynamicTasks.value = updated
        _deletedTasks.value = repository.getDeletedTasks()
        AutomationManager.cancelTaskAlarms(getApplication(), taskId)

        if (_activeAutomationStateIsTask(taskId)) {
          stopActiveAutomation()
        }
      }
    }
  }

  fun restoreFromTrash(taskId: String) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        val updated = repository.restoreTask(taskId)
        _dynamicTasks.value = updated
        _deletedTasks.value = repository.getDeletedTasks()
        val restoredTask = updated.firstOrNull { it.id == taskId }
        if (restoredTask != null) {
          AutomationManager.scheduleTaskAlarms(getApplication(), restoredTask)
        }
      }
    }
  }

  fun purgePermanently(taskId: String) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        val updated = repository.purgeTaskPermanently(taskId)
        _dynamicTasks.value = updated
        _deletedTasks.value = repository.getDeletedTasks()
      }
    }
  }

  fun emptyTrash() {
    viewModelScope.launch {
      scheduleMutex.withLock {
        val updated = repository.purgeAllDeletedTasks()
        _dynamicTasks.value = updated
        _deletedTasks.value = emptyList()
      }
    }
  }

  private fun refreshTrash() {
    _deletedTasks.value = repository.getDeletedTasks()
  }

  private fun _activeAutomationStateIsTask(taskId: String): Boolean {
    return activeAutomation.value.isRunning && activeAutomation.value.taskId == taskId
  }

  fun exportFullRoutineScript(): String {
    val sb = StringBuilder()
    sb.appendLine("# CHRONOS_ROUTINE_EXPORT_V1")
    sb.appendLine("# SLEEP_MINUTES=${_sleepDurationMinutes.value}")
    sb.appendLine("# BUFFER_MINUTES=${_bufferMinutes.value}")
    sb.appendLine("# ANCHOR | Slot | Title | Start(HH:mm) | Duration(m) | DND(Y/N) | Voice(Y/N)")
    for (anchor in _anchors.value.sortedBy { it.slotNumber ?: 0 }) {
      val slot = anchor.slotNumber ?: 0
      val dnd = if (anchor.enableDnd) "Y" else "N"
      val voice = if (anchor.isVoiceEnabled) "Y" else "N"
      sb.appendLine("ANCHOR | $slot | ${anchor.title} | ${anchor.formattedStartTime} | ${anchor.durationMinutes} | $dnd | $voice")
    }
    sb.appendLine("")
    sb.appendLine("# DYNAMIC | Name | Duration(m) | Start(HH:mm) | DND(Y/N) | Days | Voice(Y/N) | Message | IsFluidTwin(Y/N) | TwinPart | TwinPool | RingtoneUri | RingtoneTitle")
    for (task in _dynamicTasks.value.filter { !it.isDeleted }) {
      val dnd = if (task.enableDnd) "Y" else "N"
      val voice = if (task.isVoiceEnabled) "Y" else "N"
      val days = if (task.selectedDays.size == 7) "ALL" else task.selectedDays.joinToString(",") { it.shortName }
      val msg = task.voiceMessage.ifBlank { task.title }
      val isTwin = if (task.isFluidTwin) "Y" else "N"
      val part = task.fluidTwinPart ?: 0
      val pool = task.fluidTotalPoolMinutes ?: 0
      val rUri = task.customRingtoneUri ?: "DEFAULT"
      val rTitle = task.customRingtoneTitle ?: "Default"
      sb.appendLine("DYNAMIC | ${task.title} | ${task.durationMinutes} | ${task.formattedStartTime} | $dnd | $days | $voice | $msg | $isTwin | $part | $pool | $rUri | $rTitle")
    }
    return sb.toString()
  }

  fun applyBulkConsoleTasks(
    newTasks: List<RoutineTask>,
    replaceExisting: Boolean,
    parsedAnchors: List<RoutineTask> = emptyList(),
    backupSleepMinutes: Int? = null,
    backupBufferMinutes: Int? = null
  ) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        // If restoring backup configuration
        backupSleepMinutes?.let { mins ->
          _sleepDurationMinutes.value = mins
          repository.setSleepDurationMinutes(mins)
        }
        backupBufferMinutes?.let { buf ->
          _bufferMinutes.value = buf
          repository.setBufferMinutes(buf)
        }
        if (parsedAnchors.isNotEmpty()) {
          val recalculated = SchedulerEngine.calculateAnchors(parsedAnchors, _sleepDurationMinutes.value)
          _anchors.value = recalculated
          repository.saveAnchors(recalculated)
        }

        val list = if (replaceExisting) {
          newTasks
        } else {
          _dynamicTasks.value.filter { !it.isDeleted } + newTasks
        }
        _dynamicTasks.value = list
        repository.saveDynamicTasks(list)
        newTasks.forEach { AutomationManager.scheduleTaskAlarms(getApplication(), it) }
      }
    }
  }

  fun stopActiveAutomation() {
    AutomationManager.setDndMode(getApplication(), false)
    val intent = android.content.Intent(getApplication(), RoutineAutomationService::class.java).apply {
      action = RoutineAutomationService.ACTION_STOP_AUTOMATION
    }
    getApplication<Application>().startService(intent)
  }
}
