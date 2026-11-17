package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RoutineRepository
import com.example.engine.ScheduleImpactItem
import com.example.engine.ScheduleResult
import com.example.engine.SchedulerEngine
import com.example.engine.VerticalDropTarget
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.TaskRatingRecord
import com.example.model.TaskType
import com.example.service.ActiveAutomationState
import com.example.service.AutomationManager
import com.example.service.RoutineAutomationService
import com.example.ui.components.FluidTwinSaveRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
  HORIZON_REVERSE("Horizon / Last Task First"),
  ANCHORS_TOP("Anchors on Top"),
  ANCHORS_BOTTOM("Anchors at Bottom")
}

class RoutineViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = RoutineRepository(application)
  private val scheduleMutex = Mutex()

  private val _activeTab = MutableStateFlow(NavigationTab.TIMELINE)
  val activeTab: StateFlow<NavigationTab> = _activeTab.asStateFlow()

  private val _activeDay = MutableStateFlow(RoutineDay.today())
  val activeDay: StateFlow<RoutineDay> = _activeDay.asStateFlow()

  private val _sortMode = MutableStateFlow(repository.getSortMode())
  val sortMode: StateFlow<TimelineSortMode> = _sortMode.asStateFlow()

  private val _voicePresetId = MutableStateFlow(repository.getVoicePresetId())
  val voicePresetId: StateFlow<String> = _voicePresetId.asStateFlow()

  private val _sleepDurationMinutes = MutableStateFlow(repository.getSleepDurationMinutes())
  val sleepDurationMinutes: StateFlow<Int> = _sleepDurationMinutes.asStateFlow()

  private val _bufferMinutes = MutableStateFlow(0)
  val bufferMinutes: StateFlow<Int> = _bufferMinutes.asStateFlow()

  private val _anchors = MutableStateFlow(repository.getAnchors())
  val anchors: StateFlow<List<RoutineTask>> = _anchors.asStateFlow()

  private val _dynamicTasks = MutableStateFlow(repository.getDynamicTasks())
  val dynamicTasks: StateFlow<List<RoutineTask>> = _dynamicTasks.asStateFlow()

  private val _deletedTasks = MutableStateFlow(repository.getDeletedTasks())
  val deletedTasks: StateFlow<List<RoutineTask>> = _deletedTasks.asStateFlow()

  private val _taskRatingHistory = MutableStateFlow(repository.getTaskRatingHistory())
  val taskRatingHistory: StateFlow<List<TaskRatingRecord>> = _taskRatingHistory.asStateFlow()

  private val _currentMinute = MutableStateFlow(getCurrentMinuteOfDay())
  val currentMinute: StateFlow<Int> = _currentMinute.asStateFlow()

  private val _isDndGranted = MutableStateFlow(AutomationManager.isDndPermissionGranted(application))
  val isDndGranted: StateFlow<Boolean> = _isDndGranted.asStateFlow()

  private val _isBatteryOptimizationIgnored = MutableStateFlow(
    AutomationManager.isIgnoringBatteryOptimizations(application)
  )
  val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()

  val activeAutomation: StateFlow<ActiveAutomationState> = RoutineAutomationService.serviceState

  private val _pendingDraftTasks = MutableStateFlow<List<RoutineTask>?>(null)
  val pendingDraftTasks: StateFlow<List<RoutineTask>?> = _pendingDraftTasks.asStateFlow()

  private val _baselineSnapshotSchedule = MutableStateFlow<ScheduleResult?>(null)

  val hasPendingEdits: StateFlow<Boolean> = _pendingDraftTasks
    .map { it != null }
    .stateIn(viewModelScope, SharingStarted.Eagerly, false)

  private var nextManualPlacementOrder: Long = System.currentTimeMillis()

  init {
    // Populate permanent task ratings into cache
    val history = _taskRatingHistory.value
    for (r in history) {
      com.example.ui.components.taskRatingCache[r.taskId] = r.ratingPct
    }
    // Clock synchronization ticker for real-time progress bar, day rollover & auto-scroll
    viewModelScope.launch {
      var lastKnownDay = RoutineDay.today()
      while (isActive) {
        _currentMinute.value = getCurrentMinuteOfDay()
        val currentDay = RoutineDay.today()
        if (currentDay != lastKnownDay) {
          lastKnownDay = currentDay
          _activeDay.value = currentDay
          withContext(Dispatchers.IO) {
            repository.setActiveDay(currentDay)
          }

          scheduleMutex.withLock {
            // Reset any expired isPausedTodayOnly tasks
            val updatedDynamics = _dynamicTasks.value.map {
              if (it.isPausedTodayOnly) it.copy(isPausedTodayOnly = false) else it
            }
            val dynamicsChanged = updatedDynamics != _dynamicTasks.value
            if (dynamicsChanged) {
              _dynamicTasks.value = updatedDynamics
            }

            val updatedAnchors = _anchors.value.map {
              if (it.isPausedTodayOnly) it.copy(isPausedTodayOnly = false) else it
            }
            val anchorsChanged = updatedAnchors != _anchors.value
            if (anchorsChanged) {
              _anchors.value = updatedAnchors
            }

            withContext(Dispatchers.IO) {
              if (dynamicsChanged) repository.saveDynamicTasks(updatedDynamics)
              if (anchorsChanged) repository.saveAnchors(updatedAnchors)
              AutomationManager.rescheduleAllAlarms(getApplication())
            }
          }
        }
        delay(15000L) // Refresh every 15 seconds
      }
    }
  }

  fun checkAndAutoRestoreVault(): Boolean {
    val restored = repository.checkAndAutoRestoreVault()
    if (restored) {
      refreshFromRepository()
    }
    return restored
  }

  fun refreshFromRepository() {
    _sleepDurationMinutes.value = repository.getSleepDurationMinutes()
    _bufferMinutes.value = repository.getBufferMinutes().let { if (it == 5) 0 else it }
    _anchors.value = repository.getAnchors()
    _dynamicTasks.value = repository.getDynamicTasks()
    _deletedTasks.value = repository.getDeletedTasks()
    _activeDay.value = repository.getActiveDay()
    _sortMode.value = repository.getSortMode()
    _voicePresetId.value = repository.getVoicePresetId()
    val history = repository.getTaskRatingHistory()
    _taskRatingHistory.value = history
    for (r in history) {
      com.example.ui.components.taskRatingCache[r.taskId] = r.ratingPct
    }
  }

  private fun getCurrentMinuteOfDay(): Int {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
  }

  val rawScheduleResult: StateFlow<ScheduleResult> = combine(
    _anchors,
    _dynamicTasks,
    _pendingDraftTasks,
    _activeDay,
    _bufferMinutes
  ) { currentAnchors, currentDynamics, draftDynamics, day, buffer ->
    val activeDynamics = (draftDynamics ?: currentDynamics).filter { !it.isDeleted }
    SchedulerEngine.scheduleDay(
      anchors = currentAnchors,
      dynamicTasks = activeDynamics,
      activeDay = day,
      interTaskBufferMinutes = buffer
    )
  }.distinctUntilChanged().flowOn(Dispatchers.Default).stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = SchedulerEngine.scheduleDay(
      _anchors.value,
      _dynamicTasks.value.filter { !it.isDeleted },
      _activeDay.value,
      _bufferMinutes.value
    )
  )

  private val activeSortMinuteTicker = combine(_sortMode, _currentMinute) { sort, currMin ->
    if (sort == TimelineSortMode.ACTIVE_FIRST) currMin else -1
  }.distinctUntilChanged()

  val scheduleResult: StateFlow<ScheduleResult> = combine(
    rawScheduleResult,
    _sortMode,
    activeSortMinuteTicker,
    _activeDay
  ) { rawResult, sort, currMin, day ->
    val sortedScheduled = when (sort) {
      TimelineSortMode.EARLIEST_START -> rawResult.scheduledTasks.sortedBy { it.startTimeMinute }
      TimelineSortMode.LATEST_START -> rawResult.scheduledTasks.sortedByDescending { it.startTimeMinute }
      TimelineSortMode.LONGEST_DURATION -> rawResult.scheduledTasks.sortedByDescending { it.durationMinutes }
      TimelineSortMode.SHORTEST_DURATION -> rawResult.scheduledTasks.sortedBy { it.durationMinutes }
      TimelineSortMode.ACTIVE_FIRST -> {
        val isToday = day == RoutineDay.today()
        val active = rawResult.scheduledTasks.filter { com.example.ui.components.isTaskActiveAt(it, currMin, isToday) }
        val upcoming = rawResult.scheduledTasks.filter { !com.example.ui.components.isTaskActiveAt(it, currMin, isToday) && it.startTimeMinute >= currMin }.sortedBy { it.startTimeMinute }
        val completed = rawResult.scheduledTasks.filter { !com.example.ui.components.isTaskActiveAt(it, currMin, isToday) && it.startTimeMinute < currMin }.sortedBy { it.startTimeMinute }
        active + upcoming + completed
      }
      TimelineSortMode.HORIZON_REVERSE -> rawResult.scheduledTasks.sortedByDescending { it.startTimeMinute + it.durationMinutes }
      TimelineSortMode.ANCHORS_TOP -> {
        val (anchorsList, dynamicsList) = rawResult.scheduledTasks.partition {
          it.type == TaskType.ANCHOR_SLEEP || it.type == TaskType.ANCHOR_STANDARD || it.type == TaskType.ANCHOR_ELASTIC
        }
        anchorsList.sortedBy { it.slotNumber ?: 0 } + dynamicsList.sortedBy { it.startTimeMinute }
      }
      TimelineSortMode.ANCHORS_BOTTOM -> {
        val (anchorsList, dynamicsList) = rawResult.scheduledTasks.partition {
          it.type == TaskType.ANCHOR_SLEEP || it.type == TaskType.ANCHOR_STANDARD || it.type == TaskType.ANCHOR_ELASTIC
        }
        dynamicsList.sortedBy { it.startTimeMinute } + anchorsList.sortedBy { it.slotNumber ?: 0 }
      }
    }
    rawResult.copy(scheduledTasks = sortedScheduled)
  }.distinctUntilChanged().flowOn(Dispatchers.Default).stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = rawScheduleResult.value
  )

  val pendingImpactDiff: StateFlow<List<ScheduleImpactItem>> = combine(
    _baselineSnapshotSchedule,
    rawScheduleResult,
    _pendingDraftTasks
  ) { baseline, currentSchedule, draft ->
    if (baseline == null || draft == null) {
      emptyList()
    } else {
      SchedulerEngine.computeScheduleImpactDiff(baseline, currentSchedule)
    }
  }.distinctUntilChanged().flowOn(Dispatchers.Default).stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = emptyList()
  )

  fun previewDragTaskMove(
    taskId: String,
    targetStartMinute: Int,
    onAnchorWallHit: (() -> Unit)? = null
  ) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        if (_baselineSnapshotSchedule.value == null) {
          _baselineSnapshotSchedule.value = rawScheduleResult.value
        }
        val workingTasks = _pendingDraftTasks.value ?: _dynamicTasks.value
        val currentAnchors = _anchors.value
        val day = _activeDay.value
        nextManualPlacementOrder = maxOf(nextManualPlacementOrder + 1L, System.currentTimeMillis())
        val order = nextManualPlacementOrder

        val (updatedTasks, hitWall) = withContext(Dispatchers.Default) {
          SchedulerEngine.applyDragReposition(
            allDynamicTasks = workingTasks,
            taskId = taskId,
            targetStartMinute = targetStartMinute,
            newManualOrder = order,
            anchors = currentAnchors,
            day = day
          )
        }
        _pendingDraftTasks.value = updatedTasks
        if (hitWall) {
          onAnchorWallHit?.invoke()
        }
      }
    }
  }

  fun previewVerticalDropMove(
    taskId: String,
    dropTarget: VerticalDropTarget,
    onAnchorWallHit: (() -> Unit)? = null
  ) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        val currentSchedule = rawScheduleResult.value
        if (_baselineSnapshotSchedule.value == null) {
          _baselineSnapshotSchedule.value = currentSchedule
        }
        val workingTasks = _pendingDraftTasks.value ?: _dynamicTasks.value
        val currentAnchors = _anchors.value
        val day = _activeDay.value
        nextManualPlacementOrder = maxOf(nextManualPlacementOrder + 200L, System.currentTimeMillis())
        val baseOrder = nextManualPlacementOrder

        val (updatedTasks, hitWall) = withContext(Dispatchers.Default) {
          SchedulerEngine.applyVerticalDropReposition(
            allDynamicTasks = workingTasks,
            scheduledTasks = currentSchedule.scheduledTasks,
            taskId = taskId,
            dropTarget = dropTarget,
            baseManualOrder = baseOrder,
            anchors = currentAnchors,
            day = day
          )
        }
        _pendingDraftTasks.value = updatedTasks
        if (hitWall) {
          onAnchorWallHit?.invoke()
        }
      }
    }
  }

  fun commitPendingEdits() {
    if (_pendingDraftTasks.value == null) return
    viewModelScope.launch {
      scheduleMutex.withLock {
        val draft = _pendingDraftTasks.value ?: return@withLock
        _dynamicTasks.value = draft
        _pendingDraftTasks.value = null
        _baselineSnapshotSchedule.value = null
        withContext(Dispatchers.IO) {
          repository.saveDynamicTasks(draft)
          AutomationManager.rescheduleAllAlarms(getApplication())
        }
      }
    }
  }

  fun discardPendingEdits() {
    _pendingDraftTasks.value = null
    _baselineSnapshotSchedule.value = null
  }

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
    repository.setSortMode(mode)
  }

  fun recordTaskRating(task: RoutineTask, ratingPct: Int) {
    val normalizedCategory = when {
      task.type == TaskType.ANCHOR_SLEEP || task.slotNumber == 1 -> "sleep"
      task.type == TaskType.ANCHOR_ELASTIC || task.slotNumber == 5 -> "anchor_elastic"
      task.type == TaskType.ANCHOR_STANDARD -> "anchor_standard"
      else -> task.category?.trim()?.lowercase()?.ifBlank { null } ?: "general"
    }
    val record = TaskRatingRecord(
      taskId = task.id,
      taskTitle = task.title,
      category = normalizedCategory,
      durationMinutes = task.durationMinutes,
      ratingPct = ratingPct,
      timestamp = System.currentTimeMillis()
    )
    repository.recordTaskRating(record)
    _taskRatingHistory.value = repository.getTaskRatingHistory()
    com.example.ui.components.taskRatingCache[task.id] = ratingPct
  }

  fun getTaskRatingHistory(): List<TaskRatingRecord> = repository.getTaskRatingHistory()

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
        val isFridaySlot3 = slotNumber == 3 && _activeDay.value == RoutineDay.FRIDAY
        val updated = if (isFridaySlot3) {
          current.map { task ->
            if (task.slotNumber == 3) {
              val currentFriStart = task.fridayStartTimeMinute ?: task.startTimeMinute
              val newFriStart = ((currentFriStart + deltaMinutes) % 1440 + 1440) % 1440
              task.copy(fridayStartTimeMinute = newFriStart)
            } else {
              task
            }
          }
        } else {
          SchedulerEngine.adjustAnchor(
            anchors = current,
            slotNumber = slotNumber,
            deltaMinutes = deltaMinutes,
            sleepDurationMinutes = _sleepDurationMinutes.value
          )
        }
        _anchors.value = updated
        withContext(Dispatchers.IO) {
          repository.saveAnchors(updated)
          AutomationManager.rescheduleAllAlarms(getApplication())
        }
      }
    }
  }

  fun updateSleepDuration(hours: Float) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        val mins = (hours * 60).toInt().coerceIn(240, 720)
        _sleepDurationMinutes.value = mins
        val recalculated = SchedulerEngine.calculateAnchors(_anchors.value, mins)
        _anchors.value = recalculated
        withContext(Dispatchers.IO) {
          repository.setSleepDurationMinutes(mins)
          repository.saveAnchors(recalculated)
          AutomationManager.rescheduleAllAlarms(getApplication())
        }
      }
    }
  }

  fun updateBufferMinutes(minutes: Int) {
    _bufferMinutes.value = minutes.coerceIn(0, 30)
    repository.setBufferMinutes(_bufferMinutes.value)
  }

  fun updateVoicePreset(presetId: String) {
    val resolved = AutomationManager.getVoicePreset(presetId).id
    _voicePresetId.value = resolved
    repository.setVoicePresetId(resolved)
  }

  fun pauseTask(
    taskId: String,
    isAnchor: Boolean,
    slotNumber: Int?,
    days: Int?,
    todayOnly: Boolean,
    indefinitely: Boolean
  ) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        if (slotNumber == 1) {
          // Slot 1 (Reverse Sleep) is strictly protected and cannot be paused
          return@withLock
        }

        val now = System.currentTimeMillis()
        val pausedUntil = when {
          indefinitely -> null
          todayOnly -> {
            val cal = Calendar.getInstance().apply {
              set(Calendar.HOUR_OF_DAY, 23)
              set(Calendar.MINUTE, 59)
              set(Calendar.SECOND, 59)
              set(Calendar.MILLISECOND, 999)
            }
            cal.timeInMillis
          }
          days != null && days > 0 -> {
            now + days * 24L * 60 * 60 * 1000
          }
          else -> null
        }

        val affectedIds = mutableListOf<String>()
        val targetIsAnchor = isAnchor || slotNumber != null || _anchors.value.any { it.id == taskId }
        var anchorsToSave: List<RoutineTask>? = null
        var dynamicsToSave: List<RoutineTask>? = null

        if (targetIsAnchor) {
          val targetSlot = slotNumber ?: _anchors.value.firstOrNull { it.id == taskId }?.slotNumber
          val updated = _anchors.value.map { task ->
            if (task.id == taskId || (targetSlot != null && task.slotNumber == targetSlot)) {
              affectedIds.add(task.id)
              task.copy(
                isPausedIndefinitely = indefinitely,
                isPausedTodayOnly = todayOnly,
                pausedUntilDateMillis = pausedUntil
              )
            } else {
              task
            }
          }
          _anchors.value = updated
          anchorsToSave = updated
        } else {
          val updated = _dynamicTasks.value.map { task ->
            if (task.id == taskId || task.parentTaskId == taskId) {
              affectedIds.add(task.id)
              task.copy(
                isPausedIndefinitely = indefinitely,
                isPausedTodayOnly = todayOnly,
                pausedUntilDateMillis = pausedUntil
              )
            } else {
              task
            }
          }
          _dynamicTasks.value = updated
          dynamicsToSave = updated
        }

        withContext(Dispatchers.IO) {
          anchorsToSave?.let { repository.saveAnchors(it) }
          dynamicsToSave?.let { repository.saveDynamicTasks(it) }
          AutomationManager.cancelTaskAlarms(getApplication(), taskId)
          affectedIds.forEach { affectedId ->
            AutomationManager.releaseDndIfHeldByTask(getApplication(), affectedId)
          }
          AutomationManager.rescheduleAllAlarms(getApplication())
        }
      }
    }
  }

  fun resumeTask(taskId: String, isAnchor: Boolean, slotNumber: Int?) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        val targetIsAnchor = isAnchor || slotNumber != null || _anchors.value.any { it.id == taskId }
        var anchorsToSave: List<RoutineTask>? = null
        var dynamicsToSave: List<RoutineTask>? = null

        if (targetIsAnchor) {
          val targetSlot = slotNumber ?: _anchors.value.firstOrNull { it.id == taskId }?.slotNumber
          val updated = _anchors.value.map { task ->
            if (task.id == taskId || (targetSlot != null && task.slotNumber == targetSlot)) {
              task.copy(
                isPausedIndefinitely = false,
                isPausedTodayOnly = false,
                pausedUntilDateMillis = null
              )
            } else {
              task
            }
          }
          _anchors.value = updated
          anchorsToSave = updated
        } else {
          val updated = _dynamicTasks.value.map { task ->
            if (task.id == taskId || task.parentTaskId == taskId) {
              task.copy(
                isPausedIndefinitely = false,
                isPausedTodayOnly = false,
                pausedUntilDateMillis = null
              )
            } else {
              task
            }
          }
          _dynamicTasks.value = updated
          dynamicsToSave = updated
        }

        withContext(Dispatchers.IO) {
          anchorsToSave?.let { repository.saveAnchors(it) }
          dynamicsToSave?.let { repository.saveDynamicTasks(it) }
          AutomationManager.rescheduleAllAlarms(getApplication())
        }
      }
    }
  }

  fun updateSlot5Protected(
    title: String,
    voiceMessage: String,
    selectedDays: Set<RoutineDay> = RoutineDay.allDays(),
    enableDnd: Boolean = false,
    dndDelayMinutes: Int = 0,
    enableHaptic: Boolean = true,
    isVoiceEnabled: Boolean = true,
    voiceVolume: Float = 1.0f,
    isSoundAlertEnabled: Boolean = true,
    customRingtoneUri: String? = null,
    customRingtoneTitle: String? = null
  ) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        val current = _anchors.value.map { task ->
          if (task.slotNumber == 5) {
            task.copy(
              title = title.ifEmpty { "Elastic Rubber Buffer" },
              voiceMessage = voiceMessage,
              selectedDays = selectedDays,
              enableDnd = enableDnd,
              dndDelayMinutes = dndDelayMinutes.coerceIn(0, 25),
              enableHaptic = enableHaptic,
              isVoiceEnabled = isVoiceEnabled,
              voiceVolume = voiceVolume.coerceIn(0f, 1f),
              isSoundAlertEnabled = isSoundAlertEnabled,
              customRingtoneUri = customRingtoneUri,
              customRingtoneTitle = customRingtoneTitle
            )
          } else {
            task
          }
        }
        val recalculated = SchedulerEngine.calculateAnchors(current, _sleepDurationMinutes.value)
        _anchors.value = recalculated
        withContext(Dispatchers.IO) {
          repository.saveAnchors(recalculated)
          if (!enableDnd) {
            recalculated.firstOrNull { it.slotNumber == 5 }?.let {
              AutomationManager.releaseDndIfHeldByTask(getApplication(), it.id)
            }
          }
          AutomationManager.rescheduleAllAlarms(getApplication())
        }
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
    customRingtoneTitle: String? = null,
    isSoundAlertEnabled: Boolean = true,
    fridayTitle: String? = null,
    fridayStartTimeMinute: Int? = null,
    fridayDurationMinutes: Int? = null,
    fridayVoiceMessage: String? = null,
    voiceVolume: Float = 1.0f,
    dndDelayMinutes: Int = 0
  ) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        var activeSleepMins = _sleepDurationMinutes.value
        if (slotNumber == 1) {
          activeSleepMins = durationMinutes.coerceIn(240, 720)
          _sleepDurationMinutes.value = activeSleepMins
        }
        val current = _anchors.value.map { task ->
          if (task.slotNumber == slotNumber) {
            task.copy(
              title = title,
              startTimeMinute = startMinute,
              durationMinutes = if (slotNumber == 1) activeSleepMins else durationMinutes,
              voiceMessage = voiceMessage,
              enableDnd = enableDnd,
              enableHaptic = enableHaptic,
              selectedDays = selectedDays,
              isVoiceEnabled = isVoiceEnabled,
              customNotificationTitle = customNotificationTitle,
              customNotificationBody = customNotificationBody,
              customRingtoneUri = customRingtoneUri,
              customRingtoneTitle = customRingtoneTitle,
              isSoundAlertEnabled = isSoundAlertEnabled,
              fridayTitle = if (slotNumber == 3) fridayTitle else task.fridayTitle,
              fridayStartTimeMinute = if (slotNumber == 3) fridayStartTimeMinute else task.fridayStartTimeMinute,
              fridayDurationMinutes = if (slotNumber == 3) fridayDurationMinutes else task.fridayDurationMinutes,
              fridayVoiceMessage = if (slotNumber == 3) fridayVoiceMessage else task.fridayVoiceMessage,
              voiceVolume = voiceVolume,
              dndDelayMinutes = dndDelayMinutes.coerceIn(0, 25)
            )
          } else {
            task
          }
        }
        val recalculated = SchedulerEngine.calculateAnchors(current, activeSleepMins)
        _anchors.value = recalculated
        withContext(Dispatchers.IO) {
          if (slotNumber == 1) {
            repository.setSleepDurationMinutes(activeSleepMins)
          }
          repository.saveAnchors(recalculated)
          if (!enableDnd) {
            recalculated.firstOrNull { it.slotNumber == slotNumber }?.let {
              AutomationManager.releaseDndIfHeldByTask(getApplication(), it.id)
            }
          }
          AutomationManager.rescheduleAllAlarms(getApplication())
        }
      }
    }
  }

  fun saveDynamicTask(task: RoutineTask) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        _pendingDraftTasks.value = null
        _baselineSnapshotSchedule.value = null
        val birthStart = task.originalStartTimeMinute ?: task.startTimeMinute
        val list = _dynamicTasks.value.filter { it.id != task.id && it.parentTaskId != task.id }.toMutableList()
        list.add(task.copy(originalStartTimeMinute = birthStart, isDeleted = false, deletedTimestamp = null))
        _dynamicTasks.value = list
        withContext(Dispatchers.IO) {
          repository.saveDynamicTasks(list)
          if (!task.enableDnd || task.isCurrentlyPaused()) {
            AutomationManager.releaseDndIfHeldByTask(getApplication(), task.id)
          }
          AutomationManager.rescheduleAllAlarms(getApplication())
        }
      }
    }
  }

  fun saveFluidTwin(request: FluidTwinSaveRequest) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        _pendingDraftTasks.value = null
        _baselineSnapshotSchedule.value = null
        val twinId = request.existingTwinId?.ifBlank { null } ?: "fluid_${UUID.randomUUID().toString().take(8)}"
        val actualP1Title = request.part1Title.ifBlank { "${request.title} (Part 1)" }
        val actualP2Title = request.part2Title.ifBlank { "${request.title} (Part 2)" }
        val p1Dur = request.part1Duration.coerceAtLeast(1)
        val p2Dur = request.part2Duration.coerceAtLeast(1)
        val p1Floor = request.part1SafetyFloorMinutes.coerceIn(0, p1Dur)
        val p2Floor = request.part2SafetyFloorMinutes.coerceIn(0, p2Dur)
        val totalPool = request.totalPoolMinutes.coerceAtLeast(p1Dur + p2Dur)
        val clampedFlex = request.customFlexibilityMinutes.coerceIn(0, 30)
        val clampedDndDelay = if (request.enableDnd) request.dndDelayMinutes.coerceIn(0, 25) else 0
        val part1 = RoutineTask(
          id = "${twinId}_p1",
          title = actualP1Title,
          type = TaskType.DYNAMIC,
          startTimeMinute = request.part1StartMinute,
          originalStartTimeMinute = request.part1OriginalStartMinute ?: request.part1StartMinute,
          durationMinutes = p1Dur,
          voiceMessage = request.part1VoiceMessage.ifBlank { "$actualP1Title starting now." },
          enableDnd = request.enableDnd,
          dndDelayMinutes = clampedDndDelay,
          enableHaptic = request.enableHaptic,
          allowSplitting = false,
          selectedDays = request.selectedDays,
          isVoiceEnabled = request.part1IsVoiceEnabled,
          voiceVolume = request.part1VoiceVolume.coerceIn(0f, 1f),
          customNotificationTitle = request.customNotificationTitle.ifBlank { actualP1Title },
          customNotificationBody = request.customNotificationBody,
          isFluidTwin = true,
          fluidTwinId = twinId,
          fluidTwinPart = 1,
          fluidTotalPoolMinutes = totalPool,
          fluidSafetyFloorMinutes = p1Floor,
          customRingtoneUri = request.customRingtoneUri,
          customRingtoneTitle = request.customRingtoneTitle,
          isSoundAlertEnabled = request.isSoundAlertEnabled,
          interTaskBufferMinutes = request.interTaskBufferMinutes.coerceIn(0, 30),
          customFlexibilityMinutes = clampedFlex,
          toleranceMinutes = clampedFlex,
          category = request.category,
          isDeleted = false,
          deletedTimestamp = null
        )
        val part2 = RoutineTask(
          id = "${twinId}_p2",
          title = actualP2Title,
          type = TaskType.DYNAMIC,
          startTimeMinute = request.part2StartMinute,
          originalStartTimeMinute = request.part2OriginalStartMinute ?: request.part2StartMinute,
          durationMinutes = p2Dur,
          voiceMessage = request.part2VoiceMessage.ifBlank { "$actualP2Title starting now." },
          enableDnd = request.enableDnd,
          dndDelayMinutes = clampedDndDelay,
          enableHaptic = request.enableHaptic,
          allowSplitting = false,
          selectedDays = request.selectedDays,
          isVoiceEnabled = request.part2IsVoiceEnabled,
          voiceVolume = request.part2VoiceVolume.coerceIn(0f, 1f),
          customNotificationTitle = request.customNotificationTitle.ifBlank { actualP2Title },
          customNotificationBody = request.customNotificationBody,
          isFluidTwin = true,
          fluidTwinId = twinId,
          fluidTwinPart = 2,
          fluidTotalPoolMinutes = totalPool,
          fluidSafetyFloorMinutes = p2Floor,
          customRingtoneUri = request.customRingtoneUri,
          customRingtoneTitle = request.customRingtoneTitle,
          isSoundAlertEnabled = request.isSoundAlertEnabled,
          interTaskBufferMinutes = request.interTaskBufferMinutes.coerceIn(0, 30),
          customFlexibilityMinutes = clampedFlex,
          toleranceMinutes = clampedFlex,
          category = request.category,
          isDeleted = false,
          deletedTimestamp = null
        )

        val list = _dynamicTasks.value.filter {
          it.fluidTwinId != twinId && it.id != request.sourceSingleTaskId
        }.toMutableList()
        list.add(part1)
        list.add(part2)
        _dynamicTasks.value = list
        withContext(Dispatchers.IO) {
          repository.saveDynamicTasks(list)
          request.sourceSingleTaskId?.let { sourceId ->
            AutomationManager.cancelTaskAlarms(getApplication(), sourceId)
            AutomationManager.releaseDndIfHeldByTask(getApplication(), sourceId)
          }
          if (!request.enableDnd) {
            AutomationManager.releaseDndIfHeldByTask(getApplication(), part1.id)
            AutomationManager.releaseDndIfHeldByTask(getApplication(), part2.id)
          }
          AutomationManager.rescheduleAllAlarms(getApplication())
        }
      }
    }
  }

  fun moveToTrash(taskId: String) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        _pendingDraftTasks.value = null
        _baselineSnapshotSchedule.value = null
        val (updated, deleted) = withContext(Dispatchers.IO) {
          val u = repository.softDeleteTask(taskId)
          val d = repository.getDeletedTasks()
          AutomationManager.cancelTaskAlarms(getApplication(), taskId)
          AutomationManager.releaseDndIfHeldByTask(getApplication(), taskId)
          u to d
        }
        _dynamicTasks.value = updated
        _deletedTasks.value = deleted

        if (_activeAutomationStateIsTask(taskId)) {
          stopActiveAutomation()
        }
        withContext(Dispatchers.IO) {
          AutomationManager.rescheduleAllAlarms(getApplication())
        }
      }
    }
  }

  fun deleteBothFluidTwinParts(twinId: String) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        _pendingDraftTasks.value = null
        _baselineSnapshotSchedule.value = null
        val twins = _dynamicTasks.value.filter { it.fluidTwinId == twinId }
        twins.forEach { task ->
          if (_activeAutomationStateIsTask(task.id)) {
            stopActiveAutomation()
          }
        }
        val (updated, deleted) = withContext(Dispatchers.IO) {
          twins.forEach { task ->
            repository.softDeleteTask(task.id)
            AutomationManager.cancelTaskAlarms(getApplication(), task.id)
            AutomationManager.releaseDndIfHeldByTask(getApplication(), task.id)
          }
          val u = repository.getDynamicTasks()
          val d = repository.getDeletedTasks()
          AutomationManager.rescheduleAllAlarms(getApplication())
          u to d
        }
        _dynamicTasks.value = updated
        _deletedTasks.value = deleted
      }
    }
  }

  fun deleteTwinAndAbsorbTime(taskToDeleteId: String, partnerTaskId: String) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        _pendingDraftTasks.value = null
        _baselineSnapshotSchedule.value = null
        val taskToDelete = _dynamicTasks.value.firstOrNull { it.id == taskToDeleteId && !it.isDeleted }
        val partnerTask = _dynamicTasks.value.firstOrNull { it.id == partnerTaskId && !it.isDeleted }
        if (taskToDelete != null && partnerTask != null) {
          val absorbedDuration = (partnerTask.durationMinutes + taskToDelete.durationMinutes)
            .coerceAtLeast(partnerTask.fluidTotalPoolMinutes ?: (partnerTask.durationMinutes + taskToDelete.durationMinutes))
          val updatedPartner = partnerTask.copy(
            durationMinutes = absorbedDuration,
            isFluidTwin = false,
            fluidTwinId = null,
            fluidTwinPart = null,
            fluidTotalPoolMinutes = null,
            fluidSafetyFloorMinutes = 0
          )
          if (_activeAutomationStateIsTask(taskToDeleteId)) {
            stopActiveAutomation()
          }
          val (updatedList, deleted) = withContext(Dispatchers.IO) {
            repository.softDeleteTask(taskToDeleteId)
            AutomationManager.cancelTaskAlarms(getApplication(), taskToDeleteId)
            AutomationManager.releaseDndIfHeldByTask(getApplication(), taskToDeleteId)

            val list = repository.getDynamicTasks().map {
              if (it.id == partnerTaskId) updatedPartner else it
            }
            repository.saveDynamicTasks(list)
            val d = repository.getDeletedTasks()
            AutomationManager.rescheduleAllAlarms(getApplication())
            list to d
          }
          _dynamicTasks.value = updatedList
          _deletedTasks.value = deleted
        } else if (taskToDelete != null) {
          val (updated, deleted) = withContext(Dispatchers.IO) {
            val u = repository.softDeleteTask(taskToDeleteId)
            val d = repository.getDeletedTasks()
            AutomationManager.cancelTaskAlarms(getApplication(), taskToDeleteId)
            AutomationManager.releaseDndIfHeldByTask(getApplication(), taskToDeleteId)
            u to d
          }
          _dynamicTasks.value = updated
          _deletedTasks.value = deleted
          if (_activeAutomationStateIsTask(taskToDeleteId)) {
            stopActiveAutomation()
          }
          withContext(Dispatchers.IO) {
            AutomationManager.rescheduleAllAlarms(getApplication())
          }
        }
      }
    }
  }

  fun restoreFromTrash(taskId: String) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        _pendingDraftTasks.value = null
        _baselineSnapshotSchedule.value = null
        val (updated, deleted) = withContext(Dispatchers.IO) {
          var u = repository.restoreTask(taskId)
          val restoredTask = u.firstOrNull { it.id == taskId }
          if (restoredTask != null && restoredTask.isFluidTwin && !restoredTask.fluidTwinId.isNullOrBlank()) {
            val hasActivePartner = u.any {
              it.fluidTwinId == restoredTask.fluidTwinId && it.id != restoredTask.id && !it.isDeleted
            }
            if (!hasActivePartner) {
              u = u.map {
                if (it.id == restoredTask.id) {
                  it.copy(
                    isFluidTwin = false,
                    fluidTwinId = null,
                    fluidTwinPart = null,
                    fluidTotalPoolMinutes = null,
                    fluidSafetyFloorMinutes = 0
                  )
                } else {
                  it
                }
              }
              repository.saveDynamicTasks(u)
            }
          }
          val d = repository.getDeletedTasks()
          AutomationManager.rescheduleAllAlarms(getApplication())
          u to d
        }
        _dynamicTasks.value = updated
        _deletedTasks.value = deleted
      }
    }
  }

  fun purgePermanently(taskId: String) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        val (updated, deleted) = withContext(Dispatchers.IO) {
          val u = repository.purgeTaskPermanently(taskId)
          val d = repository.getDeletedTasks()
          u to d
        }
        _dynamicTasks.value = updated
        _deletedTasks.value = deleted
      }
    }
  }

  fun emptyTrash() {
    viewModelScope.launch {
      scheduleMutex.withLock {
        val updated = withContext(Dispatchers.IO) {
          repository.purgeAllDeletedTasks()
        }
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

  fun applyBulkConsoleTasks(
    newTasks: List<RoutineTask>,
    replaceExisting: Boolean,
    parsedAnchors: List<RoutineTask> = emptyList(),
    backupSleepMinutes: Int? = null,
    backupBufferMinutes: Int? = null
  ) {
    viewModelScope.launch {
      scheduleMutex.withLock {
        _pendingDraftTasks.value = null
        _baselineSnapshotSchedule.value = null
        val syncedSleep = when {
          backupSleepMinutes != null -> backupSleepMinutes.coerceIn(240, 720)
          parsedAnchors.any { it.slotNumber == 1 } ->
            parsedAnchors.first { it.slotNumber == 1 }.durationMinutes.coerceIn(240, 720)
          else -> null
        }
        if (syncedSleep != null) {
          _sleepDurationMinutes.value = syncedSleep
        }
        backupBufferMinutes?.let { buf ->
          _bufferMinutes.value = buf.coerceIn(0, 30)
        }

        val recalculatedAnchors = if (parsedAnchors.isNotEmpty()) {
          val currentAnchors = _anchors.value.toMutableList()
          for (parsed in parsedAnchors) {
            val idx = currentAnchors.indexOfFirst { it.slotNumber == parsed.slotNumber }
            if (idx >= 0) {
              currentAnchors[idx] = parsed
            } else {
              currentAnchors.add(parsed)
            }
          }
          SchedulerEngine.calculateAnchors(currentAnchors, _sleepDurationMinutes.value).also {
            _anchors.value = it
          }
        } else {
          null
        }

        val normalizedNewTasks = newTasks.map {
          it.copy(originalStartTimeMinute = it.originalStartTimeMinute ?: it.startTimeMinute)
        }
        val oldTasks = if (replaceExisting) _dynamicTasks.value else emptyList()
        val list = if (replaceExisting) {
          normalizedNewTasks
        } else {
          _dynamicTasks.value.filter { !it.isDeleted } + normalizedNewTasks
        }
        _dynamicTasks.value = list

        withContext(Dispatchers.IO) {
          if (syncedSleep != null) {
            repository.setSleepDurationMinutes(syncedSleep)
          }
          backupBufferMinutes?.let {
            repository.setBufferMinutes(_bufferMinutes.value)
          }
          recalculatedAnchors?.let {
            repository.saveAnchors(it)
          }
          oldTasks.forEach {
            AutomationManager.cancelTaskAlarms(getApplication(), it.id)
            AutomationManager.releaseDndIfHeldByTask(getApplication(), it.id)
          }
          repository.saveDynamicTasks(list)
          AutomationManager.rescheduleAllAlarms(getApplication())
        }
      }
    }
  }

  fun stopActiveAutomation() {
    val activeTaskId = activeAutomation.value.taskId
    if (!activeTaskId.isNullOrBlank()) {
      AutomationManager.releaseDndIfHeldByTask(getApplication(), activeTaskId)
    } else {
      AutomationManager.setDndMode(getApplication(), false)
    }
    val intent = android.content.Intent(getApplication(), RoutineAutomationService::class.java).apply {
      action = RoutineAutomationService.ACTION_STOP_AUTOMATION
    }
    getApplication<Application>().startService(intent)
  }
}
