package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ScheduleImpactItem
import com.example.engine.ScheduleResult
import com.example.engine.SchedulerEngine
import com.example.engine.VerticalDropPreview
import com.example.engine.VerticalDropTarget
import com.example.model.RoutineDay
import com.example.model.RoutineTask
import com.example.model.TaskType
import com.example.service.ActiveAutomationState
import com.example.service.AutomationManager
import com.example.ui.TimelineSortMode
import com.example.ui.components.AnchorSlotCard
import com.example.ui.components.DynamicTaskCard
import com.example.ui.components.OverflowTaskCard
import com.example.ui.components.isTaskActiveAt
import com.example.ui.theme.AnchorGradientStart
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DynamicGradientEnd
import com.example.ui.theme.DynamicGradientStart
import com.example.ui.theme.ElasticGradientStart
import com.example.ui.theme.OneUiBackground
import com.example.ui.theme.OneUiBorder
import com.example.ui.theme.OneUiSurfaceElevated
import com.example.ui.theme.SleepGradientStart
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class TimelineDragAxis {
  UNDECIDED,
  HORIZONTAL_SCRUB,
  VERTICAL_REORDER
}

private data class HoveredDropVisual(
  val target: VerticalDropTarget,
  val preview: VerticalDropPreview,
  val dropShadowCenterY: Float,
  val splitIndex: Int
)

@Composable
fun TimelineScreen(
  activeDay: RoutineDay,
  onSelectDay: (RoutineDay) -> Unit,
  scheduleResult: ScheduleResult,
  sleepDurationMinutes: Int,
  currentMinute: Int,
  isDndGranted: Boolean,
  activeAutomation: ActiveAutomationState,
  sortMode: TimelineSortMode,
  deletedCount: Int,
  onSelectSortMode: (TimelineSortMode) -> Unit,
  onQuickAdjustAnchor: (slotNumber: Int, deltaMinutes: Int) -> Unit,
  onEditTask: (RoutineTask) -> Unit,
  onMoveToTrash: (taskId: String) -> Unit,
  onOpenTrashBin: () -> Unit,
  onStopAutomation: () -> Unit,
  onRefreshDndPermission: () -> Unit,
  onRateTask: ((RoutineTask, Int) -> Unit)? = null,
  onDeleteBothFluidTwinParts: ((String) -> Unit)? = null,
  onDeleteTwinAndAbsorbTime: ((String, String) -> Unit)? = null,
  anchors: List<RoutineTask> = emptyList(),
  hasPendingEdits: Boolean = false,
  pendingImpactDiff: List<ScheduleImpactItem> = emptyList(),
  onPreviewDragTaskMove: ((taskId: String, targetStartMinute: Int, onAnchorWallHit: () -> Unit) -> Unit)? = null,
  onPreviewVerticalDropMove: ((taskId: String, dropTarget: VerticalDropTarget, onAnchorWallHit: () -> Unit) -> Unit)? = null,
  onCommitPendingEdits: (() -> Unit)? = null,
  onDiscardPendingEdits: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val density = LocalDensity.current
  val haptic = LocalHapticFeedback.current
  val coroutineScope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  // Real device calendar day dynamically updated with currentMinute / midnight rollover
  val systemToday = remember(currentMinute) {
    when (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)) {
      java.util.Calendar.SATURDAY -> RoutineDay.SATURDAY
      java.util.Calendar.SUNDAY -> RoutineDay.SUNDAY
      java.util.Calendar.MONDAY -> RoutineDay.MONDAY
      java.util.Calendar.TUESDAY -> RoutineDay.TUESDAY
      java.util.Calendar.WEDNESDAY -> RoutineDay.WEDNESDAY
      java.util.Calendar.THURSDAY -> RoutineDay.THURSDAY
      java.util.Calendar.FRIDAY -> RoutineDay.FRIDAY
      else -> RoutineDay.SATURDAY
    }
  }

  val isToday = (activeDay == systemToday)

  // State for Confirmation Dialog (Requirement 1: Safe Delete)
  var taskToDeleteConfirm by remember { mutableStateOf<RoutineTask?>(null) }

  // Header offset in LazyColumn items:
  // Item 0: OneUiHeader
  // Item 1 (if !isDndGranted): DndPermissionBanner
  // Item: Section Title "Daily Timeline & Anchors"
  val headerItemsOffset = 1 + (if (!isDndGranted) 1 else 0) + 1

  // Real-time active running task (strictly evaluated only when viewing today)
  val activeTaskIndex = remember(scheduleResult.scheduledTasks, currentMinute, isToday) {
    if (!isToday) {
      -1
    } else {
      scheduleResult.scheduledTasks.indexOfFirst {
        isTaskActiveAt(it, currentMinute, isToday = true)
      }
    }
  }
  val activeLazyItemIndex = if (activeTaskIndex >= 0) headerItemsOffset + activeTaskIndex else -1

  // Auto-scroll to currently active running task on initial launch or day shift once (Requirement 7)
  var hasAutoScrolledInitially by remember(activeDay) { mutableStateOf(false) }
  LaunchedEffect(activeDay, activeLazyItemIndex) {
    if (!hasAutoScrolledInitially && activeLazyItemIndex >= 0) {
      hasAutoScrolledInitially = true
      try {
        if (activeLazyItemIndex < listState.layoutInfo.totalItemsCount) {
          listState.animateScrollToItem(activeLazyItemIndex)
        }
      } catch (_: Exception) {}
    }
  }

  // Floating Jump Pill visibility states
  val showScrollToTop by remember(activeLazyItemIndex) {
    derivedStateOf {
      val firstVisible = listState.firstVisibleItemIndex
      if (activeLazyItemIndex >= 0) {
        firstVisible > activeLazyItemIndex
      } else {
        firstVisible > 2
      }
    }
  }

  val showScrollToActive by remember(activeLazyItemIndex) {
    derivedStateOf {
      val firstVisible = listState.firstVisibleItemIndex
      activeLazyItemIndex >= 0 && firstVisible < activeLazyItemIndex
    }
  }

  // Launcher-Grade Outer Overlay Drag-and-Drop State (Zero LazyColumn layout jumping)
  var draggedTask by remember { mutableStateOf<RoutineTask?>(null) }
  var draggedCardHeightPx by remember { mutableIntStateOf(0) }
  var fingerOffsetInCardY by remember { mutableFloatStateOf(0f) }
  var dragOverlayY by remember { mutableFloatStateOf(0f) }
  var pointerViewportY by remember { mutableFloatStateOf(0f) }
  var dragAxis by remember { mutableStateOf(TimelineDragAxis.UNDECIDED) }
  var horizontalScrubMinute by remember { mutableIntStateOf(0) }
  var activeScrubBounds by remember { mutableStateOf(0..1439) }
  var outerBoxWidthPx by remember { mutableIntStateOf(0) }
  var outerBoxHeightPx by remember { mutableIntStateOf(0) }
  var outerBoxTopInRootPx by remember { mutableFloatStateOf(-1f) }
  var lazyColumnTopInBoxPx by remember { mutableFloatStateOf(0f) }
  var listViewportHeightPx by remember { mutableIntStateOf(0) }
  val cardHeightsById = remember { mutableStateMapOf<String, Int>() }
  val cardTopsById = remember { mutableMapOf<String, Float>() }
  var openSlotSplitIndex by remember { mutableIntStateOf(-1) }
  var openSlotTopPx by remember { mutableFloatStateOf(-1f) }
  var lockedHoveredDrop by remember { mutableStateOf<HoveredDropVisual?>(null) }
  var isSettlingVerticalDrop by remember { mutableStateOf(false) }
  var settlingBaselineTasks by remember { mutableStateOf<List<RoutineTask>?>(null) }
  var lastClearWasCommit by remember { mutableStateOf(false) }
  var anchorWallToastVisible by remember { mutableStateOf(false) }
  var anchorWallHitCounter by remember { mutableIntStateOf(0) }
  var showImpactDialog by remember { mutableStateOf(false) }

  val latestScheduledTasks by rememberUpdatedState(scheduleResult.scheduledTasks)
  val latestAnchors by rememberUpdatedState(anchors)
  val latestActiveDay by rememberUpdatedState(activeDay)
  val latestHeaderOffset by rememberUpdatedState(headerItemsOffset)
  val latestSortMode by rememberUpdatedState(sortMode)

  // Clear settling drag state the exact frame the updated schedule arrives (zero 1-frame snap-back blink)
  LaunchedEffect(isSettlingVerticalDrop, scheduleResult.scheduledTasks) {
    if (isSettlingVerticalDrop && scheduleResult.scheduledTasks !== settlingBaselineTasks) {
      lastClearWasCommit = true
      isSettlingVerticalDrop = false
      settlingBaselineTasks = null
      draggedTask = null
      dragAxis = TimelineDragAxis.UNDECIDED
      lockedHoveredDrop = null
      openSlotSplitIndex = -1
      openSlotTopPx = -1f
    }
  }
  LaunchedEffect(isSettlingVerticalDrop) {
    if (isSettlingVerticalDrop) {
      delay(150L)
      if (isSettlingVerticalDrop) {
        lastClearWasCommit = true
        isSettlingVerticalDrop = false
        settlingBaselineTasks = null
        draggedTask = null
        dragAxis = TimelineDragAxis.UNDECIDED
        lockedHoveredDrop = null
        openSlotSplitIndex = -1
        openSlotTopPx = -1f
      }
    }
  }

  // Hover Slot Calculation with Anti-Ping-Pong Hysteresis
  LaunchedEffect(draggedTask?.id, dragAxis) {
    if (draggedTask == null || dragAxis != TimelineDragAxis.VERTICAL_REORDER) {
      if (!isSettlingVerticalDrop) {
        lockedHoveredDrop = null
        openSlotSplitIndex = -1
        openSlotTopPx = -1f
      }
      return@LaunchedEffect
    }

    val spacingPx = with(density) { 14.dp.toPx() }
    val hysteresisPx = with(density) { 8.dp.toPx() }
    val dropPaddingPx = with(density) { 4.dp.toPx() }
    val anchorDropPaddingPx = with(density) { 6.dp.toPx() }

    snapshotFlow {
      Triple(
        dragOverlayY,
        pointerViewportY,
        listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
      )
    }.collect {
      if (isSettlingVerticalDrop) return@collect
      val task = draggedTask ?: return@collect
      val scheduled = latestScheduledTasks
      val currAnchors = latestAnchors
      val day = latestActiveDay
      val headerOff = latestHeaderOffset
      val layoutInfo = listState.layoutInfo
      val beforePad = layoutInfo.beforeContentPadding.toFloat()
      val floatingCenterY = dragOverlayY + draggedCardHeightPx / 2f

      val visibleScheduled = layoutInfo.visibleItemsInfo.filter { info ->
        info.index in headerOff until (headerOff + scheduled.size)
      }
      if (visibleScheduled.isEmpty()) {
        lockedHoveredDrop = null
        return@collect
      }

      val isFenced = task.isHighPriority || task.isFluidTwin
      val fenceBounds = if (isFenced) {
        SchedulerEngine.findEnclosingAnchorBounds(
          currAnchors,
          task.originalStartTimeMinute ?: task.startTimeMinute,
          day
        )
      } else {
        0 to 1440
      }

      val candidateInfos = if (isFenced) {
        visibleScheduled.filter { info ->
          val itemTask = scheduled[info.index - headerOff]
          itemTask.type == TaskType.DYNAMIC && itemTask.startTimeMinute in fenceBounds.first until fenceBounds.second
        }
      } else {
        visibleScheduled
      }

      var isOutsideFence = false
      if (isFenced) {
        val pointerInListY = pointerViewportY - lazyColumnTopInBoxPx - beforePad
        val hitRaw = visibleScheduled.firstOrNull { info ->
          pointerInListY >= info.offset && pointerInListY <= info.offset + info.size
        }
        if (hitRaw != null) {
          val hitRawTask = scheduled[hitRaw.index - headerOff]
          if (hitRawTask.startTimeMinute !in fenceBounds.first until fenceBounds.second) {
            isOutsideFence = true
          }
        }
        if (candidateInfos.isNotEmpty()) {
          val firstTask = scheduled[candidateInfos.first().index - headerOff]
          val lastTask = scheduled[candidateInfos.last().index - headerOff]
          val firstTop = cardTopsById[firstTask.id] ?: (lazyColumnTopInBoxPx + beforePad + candidateInfos.first().offset)
          val lastTop = cardTopsById[lastTask.id] ?: (lazyColumnTopInBoxPx + beforePad + candidateInfos.last().offset)
          val lastH = (cardHeightsById[lastTask.id] ?: candidateInfos.last().size).toFloat()
          if (floatingCenterY < firstTop - hysteresisPx || floatingCenterY > lastTop + lastH + hysteresisPx) {
            isOutsideFence = true
          }
        }
      }

      // Keep splitIndex locked while floatingCenterY stays within [openedSlotTop - 8dp .. openedSlotBottom + 8dp]
      val existingLocked = lockedHoveredDrop
      if (existingLocked != null && existingLocked.splitIndex >= 0 && !isOutsideFence) {
        val fullSlotHeight = draggedCardHeightPx.toFloat() + spacingPx
        val slotTop: Float? = if (existingLocked.splitIndex == openSlotSplitIndex && openSlotTopPx >= 0f) {
          openSlotTopPx
        } else if (existingLocked.splitIndex < scheduled.size) {
          visibleScheduled.firstOrNull { it.index - headerOff == existingLocked.splitIndex }
            ?.let { lazyColumnTopInBoxPx + beforePad + it.offset }
        } else if (scheduled.isNotEmpty()) {
          visibleScheduled.firstOrNull { it.index - headerOff == scheduled.lastIndex }
            ?.let { lazyColumnTopInBoxPx + beforePad + it.offset + it.size - fullSlotHeight }
        } else {
          null
        }
        if (slotTop != null) {
          val slotBottom = slotTop + fullSlotHeight
          if (floatingCenterY in (slotTop - hysteresisPx)..(slotBottom + hysteresisPx)) {
            return@collect
          }
        }
      }

      val hoveredInfo = candidateInfos.firstOrNull { info ->
        val itemTask = scheduled[info.index - headerOff]
        val cardTop = cardTopsById[itemTask.id] ?: (lazyColumnTopInBoxPx + beforePad + info.offset)
        val cardH = (cardHeightsById[itemTask.id] ?: info.size).toFloat()
        floatingCenterY >= cardTop && floatingCenterY <= cardTop + cardH
      } ?: candidateInfos.minByOrNull { info ->
        val itemTask = scheduled[info.index - headerOff]
        val cardTop = cardTopsById[itemTask.id] ?: (lazyColumnTopInBoxPx + beforePad + info.offset)
        val cardH = (cardHeightsById[itemTask.id] ?: info.size).toFloat()
        abs((cardTop + cardH / 2f) - floatingCenterY)
      }

      if (hoveredInfo == null) {
        if (isOutsideFence) {
          val minDur = if (task.isFluidTwin) task.fluidSafetyFloorMinutes.coerceAtLeast(1) else task.durationMinutes
          val minBlock = minDur + task.interTaskBufferMinutes.coerceIn(0, 30)
          val edgeStart = if (floatingCenterY < lazyColumnTopInBoxPx + listViewportHeightPx / 2f) {
            fenceBounds.first
          } else {
            (fenceBounds.second - minBlock).coerceAtLeast(fenceBounds.first)
          }
          val clampedSplit = if (candidateInfos.isNotEmpty()) {
            if (floatingCenterY < lazyColumnTopInBoxPx + listViewportHeightPx / 2f) {
              candidateInfos.first().index - headerOff
            } else {
              (candidateInfos.last().index - headerOff) + 1
            }
          } else {
            -1
          }
          val target = VerticalDropTarget.IntoGap(edgeStart)
          val preview = SchedulerEngine.previewVerticalDrop(
            allDynamicTasks = scheduled,
            scheduledTasks = scheduled,
            taskId = task.id,
            dropTarget = target,
            anchors = currAnchors,
            day = day
          ).copy(hitAnchorWall = true)
          lockedHoveredDrop = HoveredDropVisual(target, preview, floatingCenterY, clampedSplit)
        } else {
          lockedHoveredDrop = null
        }
        return@collect
      }

      val schedIdx = hoveredInfo.index - headerOff
      val hoveredTask = scheduled[schedIdx]
      val draggedIdxInSched = scheduled.indexOfFirst { it.id == task.id }
      if (hoveredTask.id == task.id && !isOutsideFence) {
        lockedHoveredDrop = null
        return@collect
      }

      val cardTop = cardTopsById[hoveredTask.id] ?: (lazyColumnTopInBoxPx + beforePad + hoveredInfo.offset)
      val cardHeight = (cardHeightsById[hoveredTask.id] ?: hoveredInfo.size).toFloat()
      val cardMid = cardTop + cardHeight / 2f
      val nextTask = scheduled.getOrNull(schedIdx + 1)
      val netGap = if (latestSortMode == TimelineSortMode.EARLIEST_START &&
        hoveredTask.type == TaskType.DYNAMIC &&
        nextTask != null
      ) {
        SchedulerEngine.computeNetFreeGapMinutes(hoveredTask, nextTask)
      } else {
        0
      }

      val computed: Triple<VerticalDropTarget, Float, Int>? = when {
        hoveredTask.type != TaskType.DYNAMIC -> {
          if (floatingCenterY < cardMid) {
            Triple(
              VerticalDropTarget.InsertBeforeTask(hoveredTask.id),
              cardTop - anchorDropPaddingPx,
              schedIdx
            )
          } else {
            Triple(
              VerticalDropTarget.InsertAfterTask(hoveredTask.id),
              cardTop + cardHeight + anchorDropPaddingPx,
              schedIdx + 1
            )
          }
        }
        netGap > 0 && floatingCenterY > cardTop + cardHeight -> {
          val effEnd = hoveredTask.startTimeMinute +
            hoveredTask.durationMinutes +
            hoveredTask.interTaskBufferMinutes.coerceIn(0, 30)
          val gapCenter = cardTop + cardHeight + ((hoveredInfo.size - cardHeight).coerceAtLeast(24f) / 2f)
          Triple(
            VerticalDropTarget.IntoGap(effEnd.coerceIn(0, 1439)),
            gapCenter,
            schedIdx + 1
          )
        }
        draggedIdxInSched >= 0 && schedIdx == draggedIdxInSched + 1 &&
          (hoveredTask.startTimeMinute - (task.startTimeMinute + task.durationMinutes + task.interTaskBufferMinutes.coerceIn(0, 30))) <= 15 -> {
          if (floatingCenterY >= cardTop + cardHeight * 0.30f) {
            Triple(
              VerticalDropTarget.InsertAfterTask(hoveredTask.id),
              cardTop + cardHeight + dropPaddingPx,
              schedIdx + 1
            )
          } else {
            null
          }
        }
        draggedIdxInSched >= 0 && schedIdx == draggedIdxInSched - 1 &&
          (task.startTimeMinute - (hoveredTask.startTimeMinute + hoveredTask.durationMinutes + hoveredTask.interTaskBufferMinutes.coerceIn(0, 30))) <= 15 -> {
          if (floatingCenterY <= cardTop + cardHeight * 0.70f) {
            Triple(
              VerticalDropTarget.InsertBeforeTask(hoveredTask.id),
              cardTop - dropPaddingPx,
              schedIdx
            )
          } else {
            null
          }
        }
        floatingCenterY < cardMid -> {
          Triple(
            VerticalDropTarget.InsertBeforeTask(hoveredTask.id),
            cardTop - dropPaddingPx,
            schedIdx
          )
        }
        else -> {
          Triple(
            VerticalDropTarget.InsertAfterTask(hoveredTask.id),
            cardTop + cardHeight + dropPaddingPx,
            schedIdx + 1
          )
        }
      }

      if (computed == null) {
        lockedHoveredDrop = null
        return@collect
      }

      val (target, shadowCenterY, splitIdx) = computed
      val rawPreview = SchedulerEngine.previewVerticalDrop(
        allDynamicTasks = scheduled,
        scheduledTasks = scheduled,
        taskId = task.id,
        dropTarget = target,
        anchors = currAnchors,
        day = day
      )
      val finalPreview = if (isOutsideFence) rawPreview.copy(hitAnchorWall = true) else rawPreview
      lockedHoveredDrop = HoveredDropVisual(
        target = target,
        preview = finalPreview,
        dropShadowCenterY = shadowCenterY,
        splitIndex = splitIdx
      )
    }
  }
  val latestHoveredDrop by rememberUpdatedState(lockedHoveredDrop)

  LaunchedEffect(anchorWallHitCounter) {
    if (anchorWallHitCounter > 0) {
      anchorWallToastVisible = true
      delay(1800L)
      anchorWallToastVisible = false
    }
  }

  // Frame-Rate Independent Smooth Edge Auto-Scroll during Vertical Drag Reordering (60Hz/90Hz/120Hz)
  LaunchedEffect(draggedTask?.id, dragAxis) {
    if (draggedTask == null || dragAxis != TimelineDragAxis.VERTICAL_REORDER) return@LaunchedEffect
    val edgeThresholdPx = with(density) { 96.dp.toPx() }
    val minSpeedPxPerSec = with(density) { 260.dp.toPx() }
    val maxSpeedPxPerSec = with(density) { 980.dp.toPx() }
    var lastNanos = 0L
    while (draggedTask != null && dragAxis == TimelineDragAxis.VERTICAL_REORDER) {
      val frameNanos = withFrameNanos { it }
      val dtSeconds = if (lastNanos == 0L) {
        0f
      } else {
        ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
      }
      lastNanos = frameNanos
      if (dtSeconds > 0f) {
        val yInList = pointerViewportY - lazyColumnTopInBoxPx
        val h = listViewportHeightPx.toFloat()
        if (h > 0f) {
          when {
            yInList < edgeThresholdPx -> {
              val proximity = ((edgeThresholdPx - yInList) / edgeThresholdPx).coerceIn(0f, 1f)
              val speed = minSpeedPxPerSec + proximity * (maxSpeedPxPerSec - minSpeedPxPerSec)
              listState.scrollBy(-(speed * dtSeconds))
            }
            yInList > h - edgeThresholdPx -> {
              val proximity = ((yInList - (h - edgeThresholdPx)) / edgeThresholdPx).coerceIn(0f, 1f)
              val speed = minSpeedPxPerSec + proximity * (maxSpeedPxPerSec - minSpeedPxPerSec)
              listState.scrollBy(speed * dtSeconds)
            }
          }
        }
      }
    }
  }

  val dragEnabled = onPreviewDragTaskMove != null || onPreviewVerticalDropMove != null
  val outerGestureModifier = if (dragEnabled) {
    Modifier.pointerInput(dragEnabled) {
      awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (isSettlingVerticalDrop) return@awaitEachGesture
        val scheduled = latestScheduledTasks
        val layoutInfo = listState.layoutInfo
        val headerOff = latestHeaderOffset
        val beforePad = layoutInfo.beforeContentPadding.toFloat()
        val pointerInListY = down.position.y - lazyColumnTopInBoxPx - beforePad

        val hitInfo = layoutInfo.visibleItemsInfo.firstOrNull { info ->
          info.index in headerOff until (headerOff + scheduled.size) &&
            pointerInListY >= info.offset &&
            pointerInListY <= info.offset + info.size
        } ?: return@awaitEachGesture

        val schedIdx = hitInfo.index - headerOff
        val hitTask = scheduled.getOrNull(schedIdx) ?: return@awaitEachGesture
        if (hitTask.type != TaskType.DYNAMIC) return@awaitEachGesture

        val hitCardTopInBox = cardTopsById[hitTask.id] ?: (lazyColumnTopInBoxPx + beforePad + hitInfo.offset)
        val actualCardHeight = cardHeightsById[hitTask.id] ?: hitInfo.size
        val touchLocalY = down.position.y - hitCardTopInBox
        if (touchLocalY < 0f || touchLocalY > actualCardHeight) return@awaitEachGesture

        // Never intercept taps on the top-right Edit pencil IconButton
        val editZoneLeftPx = outerBoxWidthPx - with(density) { 80.dp.toPx() }
        val editZoneBottomPx = with(density) { 64.dp.toPx() }
        if (outerBoxWidthPx > 0 && down.position.x >= editZoneLeftPx && touchLocalY <= editZoneBottomPx) {
          return@awaitEachGesture
        }

        // Wait for long press without consuming events so taps & scrolling work 100% natively
        var cancelledBeforeLongPress = false
        val completedWait = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
          while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id }
            if (change == null || !change.pressed || (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
              cancelledBeforeLongPress = true
              break
            }
          }
        }
        if (cancelledBeforeLongPress || completedWait != null) {
          return@awaitEachGesture
        }

        // Long-press confirmed
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        lastClearWasCommit = false
        isSettlingVerticalDrop = false
        settlingBaselineTasks = null

        val localScrubBounds = SchedulerEngine.computeHorizontalScrubBounds(
          task = hitTask,
          scheduledTasks = scheduled,
          anchors = latestAnchors,
          day = latestActiveDay
        )
        activeScrubBounds = localScrubBounds
        draggedTask = hitTask
        draggedCardHeightPx = actualCardHeight
        fingerOffsetInCardY = touchLocalY.coerceIn(0f, actualCardHeight.toFloat())
        pointerViewportY = down.position.y
        dragOverlayY = hitCardTopInBox
        dragAxis = TimelineDragAxis.UNDECIDED
        horizontalScrubMinute = hitTask.startTimeMinute

        var totalDx = 0f
        var totalDy = 0f
        var scrubAccumMin = 0f
        val axisLockPx = with(density) { 10.dp.toPx() }

        try {
          while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id }
            if (change == null || !change.pressed) {
              change?.consume()
              break
            }
            val delta = change.position - change.previousPosition
            change.consume()

            totalDx += delta.x
            totalDy += delta.y

            if (dragAxis == TimelineDragAxis.UNDECIDED) {
              if (abs(totalDx) > axisLockPx && abs(totalDx) > abs(totalDy) * 1.15f) {
                dragAxis = TimelineDragAxis.HORIZONTAL_SCRUB
              } else if (abs(totalDy) > axisLockPx && abs(totalDy) > abs(totalDx) * 1.15f) {
                dragAxis = TimelineDragAxis.VERTICAL_REORDER
              }
            }

            if (dragAxis == TimelineDragAxis.HORIZONTAL_SCRUB) {
              if (localScrubBounds.last > localScrubBounds.first) {
                val speedPx = abs(delta.x)
                val sensitivity = when {
                  speedPx < 6f -> 0.18f
                  speedPx < 18f -> 0.48f
                  else -> 0.95f
                }
                scrubAccumMin += (delta.x / density.density) * sensitivity
                horizontalScrubMinute = (hitTask.startTimeMinute + scrubAccumMin.roundToInt())
                  .coerceIn(localScrubBounds.first, localScrubBounds.last)
              } else {
                horizontalScrubMinute = hitTask.startTimeMinute
              }
            } else {
              pointerViewportY = change.position.y
              val maxOverlayY = (outerBoxHeightPx - actualCardHeight).coerceAtLeast(0).toFloat()
              dragOverlayY = (change.position.y - fingerOffsetInCardY).coerceIn(0f, maxOverlayY)
            }
          }
        } finally {
          val releasedAxis = dragAxis
          val releasedScrubMin = horizontalScrubMinute
          val releasedDrop = latestHoveredDrop

          if (releasedAxis == TimelineDragAxis.HORIZONTAL_SCRUB) {
            draggedTask = null
            dragAxis = TimelineDragAxis.UNDECIDED
            lockedHoveredDrop = null
            if (releasedScrubMin != hitTask.startTimeMinute) {
              onPreviewDragTaskMove?.invoke(hitTask.id, releasedScrubMin) {
                anchorWallHitCounter++
              }
            }
          } else if (releasedAxis == TimelineDragAxis.VERTICAL_REORDER && releasedDrop != null) {
            if (releasedDrop.preview.hitAnchorWall) {
              anchorWallHitCounter++
            }
            settlingBaselineTasks = latestScheduledTasks
            isSettlingVerticalDrop = true
            if (onPreviewVerticalDropMove != null) {
              onPreviewVerticalDropMove.invoke(hitTask.id, releasedDrop.target) {
                anchorWallHitCounter++
              }
            } else {
              onPreviewDragTaskMove?.invoke(hitTask.id, releasedDrop.preview.previewStartMinute) {
                anchorWallHitCounter++
              }
            }
          } else {
            draggedTask = null
            dragAxis = TimelineDragAxis.UNDECIDED
            lockedHoveredDrop = null
          }
        }
      }
    }
  } else {
    Modifier
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(OneUiBackground)
      .statusBarsPadding()
      .onGloballyPositioned { coords ->
        outerBoxTopInRootPx = coords.positionInRoot().y
      }
      .onSizeChanged {
        outerBoxWidthPx = it.width
        outerBoxHeightPx = it.height
      }
      .then(outerGestureModifier)
  ) {
    LazyColumn(
      state = listState,
      modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned { coords ->
          lazyColumnTopInBoxPx = coords.positionInParent().y
        }
        .onSizeChanged { listViewportHeightPx = it.height },
      contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // One UI Reachability Header with 24h Ribbon & Sorting Controls
      item {
        OneUiHeader(
          activeDay = activeDay,
          systemToday = systemToday,
          onSelectDay = { day ->
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelectDay(day)
          },
          totalScheduledMinutes = scheduleResult.totalScheduledMinutes,
          scheduledCount = scheduleResult.scheduledTasks.size,
          overflowCount = scheduleResult.overflowTasks.size,
          deletedCount = deletedCount,
          scheduledTasks = scheduleResult.scheduledTasks,
          currentMinute = currentMinute,
          isToday = isToday,
          hasPendingEdits = hasPendingEdits,
          onShowImpactDialog = { showImpactDialog = true },
          onCommitPendingEdits = { onCommitPendingEdits?.invoke() },
          onDiscardPendingEdits = { onDiscardPendingEdits?.invoke() },
          sortMode = sortMode,
          onSelectSortMode = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelectSortMode(it)
          },
          onOpenTrashBin = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onOpenTrashBin()
          }
        )
      }

      // DND Permission Alert Banner
      if (!isDndGranted) {
        item {
          DndPermissionBanner(
            onOpenSettings = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              AutomationManager.openDndSettings(context)
              onRefreshDndPermission()
            }
          )
        }
      }

      // Section Title: Daily Timeline
      item {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Daily Timeline & Anchors",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "${scheduleResult.totalScheduledMinutes / 60}h ${scheduleResult.totalScheduledMinutes % 60}m active",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }
      }

      // Scheduled items: 7 Anchors and fitted Dynamic Tasks
      itemsIndexed(
        items = scheduleResult.scheduledTasks,
        key = { _, task -> task.id }
      ) { index, task ->
        val activeDrag = draggedTask
        val hoveredDrop = lockedHoveredDrop
        val isVerticalDropActive = activeDrag != null && dragAxis == TimelineDragAxis.VERTICAL_REORDER
        val isHorizontalScrubActive = activeDrag != null && dragAxis == TimelineDragAxis.HORIZONTAL_SCRUB
        val targetCardHeightDp = with(density) { draggedCardHeightPx.coerceAtLeast(0).toDp() }
        val isDropBeforeThisItem = isVerticalDropActive && hoveredDrop != null && hoveredDrop.splitIndex == index
        val isDropAtEndAfterLastItem = isVerticalDropActive &&
          index == scheduleResult.scheduledTasks.lastIndex &&
          hoveredDrop != null &&
          hoveredDrop.splitIndex == scheduleResult.scheduledTasks.size

        fun liveTaskFor(t: RoutineTask): RoutineTask {
          return if (isHorizontalScrubActive && activeDrag?.id == t.id) {
            t.copy(startTimeMinute = horizontalScrubMinute)
          } else {
            t
          }
        }

        val liveCurrentTask = liveTaskFor(task)
        val isScrubbingThisCard = isHorizontalScrubActive && activeDrag?.id == task.id
        val horizontalNudgePx = if (isScrubbingThisCard && activeScrubBounds.last > activeScrubBounds.first) {
          val span = (activeScrubBounds.last - activeScrubBounds.first).coerceAtLeast(1)
          val maxNudgePx = with(density) { 10.dp.toPx() }
          val progress = ((horizontalScrubMinute - task.startTimeMinute).toFloat() / minOf(span.toFloat(), 30f))
            .coerceIn(-1f, 1f)
          progress * maxNudgePx
        } else {
          0f
        }

        val trackCardPositionModifier = Modifier
          .onGloballyPositioned { coords ->
            if (coords.isAttached) {
              cardHeightsById[task.id] = coords.size.height
              if (outerBoxTopInRootPx >= 0f) {
                cardTopsById[task.id] = coords.positionInRoot().y - outerBoxTopInRootPx
              }
            }
          }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .animateItem(
              fadeInSpec = tween(200),
              fadeOutSpec = tween(200),
              placementSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioLowBouncy
              )
            )
        ) {
          // Top Full-Sized Destination Drop Shadow Slot (expands cleanly at splitIndex == index)
          DestinationDropShadowSlot(
            visible = isDropBeforeThisItem,
            targetCardHeightDp = targetCardHeightDp,
            snapClose = lastClearWasCommit,
            draggedTitle = activeDrag?.title ?: "",
            preview = hoveredDrop?.preview,
            durationMinutes = activeDrag?.durationMinutes ?: 0,
            onSlotTopPositioned = { slotRootY ->
              if (outerBoxTopInRootPx >= 0f) {
                openSlotSplitIndex = index
                openSlotTopPx = slotRootY - outerBoxTopInRootPx
              }
            }
          )

          when (liveCurrentTask.type) {
            TaskType.ANCHOR_SLEEP,
            TaskType.ANCHOR_STANDARD,
            TaskType.ANCHOR_ELASTIC -> {
              AnchorSlotCard(
                task = liveCurrentTask,
                sleepDurationMinutes = sleepDurationMinutes,
                currentMinute = currentMinute,
                isToday = isToday,
                onQuickAdjust = { delta ->
                  liveCurrentTask.slotNumber?.let { slot -> onQuickAdjustAnchor(slot, delta) }
                },
                onEditClick = { onEditTask(task) },
                onRateTask = { _, rating -> onRateTask?.invoke(task, rating) },
                modifier = trackCardPositionModifier
              )
            }
            TaskType.DYNAMIC -> {
              val isGhostSource = isVerticalDropActive && activeDrag?.id == task.id
              DynamicTaskCard(
                task = liveCurrentTask,
                currentMinute = currentMinute,
                isToday = isToday,
                isDragging = isScrubbingThisCard,
                isDragSourceGhost = isGhostSource,
                onEditClick = { onEditTask(task) },
                onDeleteClick = { taskToDeleteConfirm = task },
                onRateTask = { _, rating -> onRateTask?.invoke(task, rating) },
                modifier = trackCardPositionModifier.graphicsLayer {
                  translationX = horizontalNudgePx
                }
              )
            }
          }

          // Corner Stopwatch Free-Time Gap Pill (Right-aligned capsule, net gap excluding interTaskBufferMinutes, live during scrub)
          if (sortMode == TimelineSortMode.EARLIEST_START && index < scheduleResult.scheduledTasks.lastIndex) {
            val nextLiveTask = liveTaskFor(scheduleResult.scheduledTasks[index + 1])
            val gapMinutes = SchedulerEngine.computeNetFreeGapMinutes(liveCurrentTask, nextLiveTask)
            if (gapMinutes > 0) {
              val gh = gapMinutes / 60
              val gm = gapMinutes % 60
              val gapLabel = when {
                gh > 0 && gm > 0 -> "${gh}h ${gm}m"
                gh > 0 -> "${gh}h"
                else -> "${gm}m"
              }
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                Surface(
                  shape = RoundedCornerShape(50),
                  color = Color(0xFF161E2E),
                  border = BorderStroke(1.dp, Color(0xFF334155)),
                  modifier = Modifier.testTag("free_gap_indicator_${task.id}")
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Timer,
                      contentDescription = null,
                      tint = Color(0xFFCBD5E1),
                      modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                      text = gapLabel,
                      color = Color(0xFFE2E8F0),
                      fontSize = 12.sp,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }
              }
            }
          }

          // Bottom Full-Sized Destination Drop Shadow Slot (expands cleanly below last item when splitIndex == scheduled.size)
          if (index == scheduleResult.scheduledTasks.lastIndex) {
            DestinationDropShadowSlot(
              visible = isDropAtEndAfterLastItem,
              targetCardHeightDp = targetCardHeightDp,
              snapClose = lastClearWasCommit,
              draggedTitle = activeDrag?.title ?: "",
              preview = hoveredDrop?.preview,
              durationMinutes = activeDrag?.durationMinutes ?: 0,
              isBottomSlot = true,
              onSlotTopPositioned = { slotRootY ->
                if (outerBoxTopInRootPx >= 0f) {
                  openSlotSplitIndex = scheduleResult.scheduledTasks.size
                  openSlotTopPx = slotRootY - outerBoxTopInRootPx
                }
              }
            )
          }
        }
      }

      // Empty State for Dynamic Tasks
      val dynamicCount = scheduleResult.scheduledTasks.count { it.type == TaskType.DYNAMIC }
      if (dynamicCount == 0) {
        item {
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(22.dp))
              .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(22.dp)),
            color = OneUiSurfaceElevated
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF67E8F9),
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Open Schedule on ${activeDay.fullName}",
                  color = TextPrimary,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Your 7 immutable anchors are running. Tap 'Add Task' in the bottom navigation bar to schedule dynamic blocks via Form or Terminal.",
                  color = TextSecondary,
                  fontSize = 11.sp,
                  lineHeight = 16.sp
                )
              }
            }
          }
        }
      }

      // Overflow Pool Section
      if (scheduleResult.overflowTasks.isNotEmpty()) {
        item {
          Spacer(modifier = Modifier.height(10.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = WarningAmber,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Overflow / Pending Tasks Pool (${scheduleResult.overflowTasks.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = WarningAmber
              )
            }
          }
        }

        itemsIndexed(
          items = scheduleResult.overflowTasks,
          key = { index, task -> "overflow_${task.id}_${task.startTimeMinute}_$index" }
        ) { _, task ->
          OverflowTaskCard(
            task = task,
            onEditClick = { onEditTask(task) }
          )
        }
      }

      // Bottom padding for navigation bar
      item {
        Spacer(modifier = Modifier.height(32.dp))
      }
    }

    // Pure Icon Floating Jump Pills
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(end = 20.dp, bottom = 24.dp),
      contentAlignment = Alignment.BottomEnd
    ) {
      // 1. Pure Icon "Scroll to Top" Button: displayed when scrolled down past active task
      androidx.compose.animation.AnimatedVisibility(
        visible = showScrollToTop,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 }
      ) {
        Surface(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            coroutineScope.launch {
              listState.animateScrollToItem(0)
            }
          },
          shape = CircleShape,
          color = OneUiSurfaceElevated,
          border = BorderStroke(1.dp, AnchorGradientStart.copy(alpha = 0.6f)),
          shadowElevation = 8.dp,
          modifier = Modifier
            .size(48.dp)
            .testTag("jump_to_top_button")
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
          ) {
            Icon(
              imageVector = Icons.Default.KeyboardArrowUp,
              contentDescription = "Scroll to Top",
              tint = AnchorGradientStart,
              modifier = Modifier.size(24.dp)
            )
          }
        }
      }

      // 2. Pure Icon "Active Task" Button: displayed when viewing top items while active task is running
      androidx.compose.animation.AnimatedVisibility(
        visible = showScrollToActive && !showScrollToTop,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 }
      ) {
        Surface(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            coroutineScope.launch {
              if (activeLazyItemIndex >= 0) {
                listState.animateScrollToItem(activeLazyItemIndex)
              }
            }
          },
          shape = CircleShape,
          color = OneUiSurfaceElevated,
          border = BorderStroke(1.dp, Color(0xFF67E8F9).copy(alpha = 0.6f)),
          shadowElevation = 8.dp,
          modifier = Modifier
            .size(48.dp)
            .testTag("jump_to_active_button")
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
          ) {
            Icon(
              imageVector = Icons.Default.GpsFixed,
              contentDescription = "Jump to Active Task",
              tint = Color(0xFF67E8F9),
              modifier = Modifier.size(22.dp)
            )
          }
        }
      }
    }

    // Floating Elevated Card Overlay (only during vertical reorder so horizontal scrub keeps in-list gap pills unobscured)
    val hoveredDropVisual = lockedHoveredDrop
    val currentActiveDrag = draggedTask
    if (currentActiveDrag != null && dragAxis == TimelineDragAxis.VERTICAL_REORDER) {
      val previewStart = hoveredDropVisual?.preview?.previewStartMinute ?: currentActiveDrag.startTimeMinute
      val displayFloatingTask = if (previewStart != currentActiveDrag.startTimeMinute) {
        currentActiveDrag.copy(startTimeMinute = previewStart)
      } else {
        currentActiveDrag
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .offset { IntOffset(0, dragOverlayY.roundToInt()) }
          .graphicsLayer {
            scaleX = 1.025f
            scaleY = 1.025f
          }
          .shadow(elevation = 18.dp, shape = RoundedCornerShape(24.dp), clip = false)
          .testTag("floating_drag_card_overlay")
      ) {
        DynamicTaskCard(
          task = displayFloatingTask,
          currentMinute = currentMinute,
          isToday = isToday,
          isDragging = true,
          onEditClick = {},
          onDeleteClick = null,
          onRateTask = null
        )
      }
    }

    // Top Floating Live Time Banner & Anchor Wall Status Pill during drag
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 10.dp, start = 20.dp, end = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      AnimatedVisibility(
        visible = currentActiveDrag != null,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 }
      ) {
        val activeDragSafe = currentActiveDrag ?: return@AnimatedVisibility
        val shownStart = when {
          dragAxis == TimelineDragAxis.HORIZONTAL_SCRUB -> horizontalScrubMinute
          dragAxis == TimelineDragAxis.VERTICAL_REORDER && hoveredDropVisual != null ->
            hoveredDropVisual.preview.previewStartMinute
          else -> activeDragSafe.startTimeMinute
        }
        val shownEnd = (shownStart + activeDragSafe.durationMinutes) % 1440
        val showFencePill = hoveredDropVisual?.preview?.hitAnchorWall == true || anchorWallToastVisible

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFF0F172A).copy(alpha = 0.96f),
          border = BorderStroke(1.5.dp, if (showFencePill) WarningAmber else CyanAccent),
          shadowElevation = 10.dp,
          modifier = Modifier.testTag("drag_live_time_overlay")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = CyanAccent,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = activeDragSafe.title,
              color = TextPrimary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${RoutineTask.formatTime(shownStart)} -> ${RoutineTask.formatTime(shownEnd)}",
              color = CyanAccent,
              fontSize = 12.sp,
              fontWeight = FontWeight.ExtraBold
            )
            if (showFencePill) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = WarningAmber.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.7f)),
                modifier = Modifier.testTag("anchor_wall_toast")
              ) {
                Text(
                  text = "Anchor Wall",
                  color = WarningAmber,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }
          }
        }
      }

      AnimatedVisibility(
        visible = currentActiveDrag == null && anchorWallToastVisible,
        enter = fadeIn() + slideInVertically { -it / 2 },
        exit = fadeOut() + slideOutVertically { -it / 2 }
      ) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = Color(0xFF2B1215).copy(alpha = 0.95f),
          border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.8f)),
          shadowElevation = 8.dp,
          modifier = Modifier.testTag("anchor_wall_toast")
        ) {
          Text(
            text = "Anchor Wall",
            color = WarningAmber,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
          )
        }
      }
    }
  }

  // Schedule Impact Preview AlertDialog (History icon in Pending Edits Header)
  if (showImpactDialog) {
    AlertDialog(
      onDismissRequest = { showImpactDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = CyanAccent,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Schedule Impact Preview",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 18.sp
          )
        }
      },
      text = {
        if (pendingImpactDiff.isEmpty()) {
          Text(
            text = "No cascading timeline shifts detected.",
            color = TextSecondary,
            fontSize = 13.sp
          )
        } else {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 320.dp)
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            for (impact in pendingImpactDiff) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, OneUiBorder)
              ) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                  Text(
                    text = impact.title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = impact.statusChange,
                    color = if (impact.newStartMinute == null) WarningAmber else CyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showImpactDialog = false
            onCommitPendingEdits?.invoke()
          },
          colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.Black),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("impact_dialog_apply_button")
        ) {
          Text("Apply", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showImpactDialog = false },
          modifier = Modifier.testTag("impact_dialog_close_button")
        ) {
          Text("Close", color = TextSecondary)
        }
      },
      containerColor = OneUiSurfaceElevated,
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier.testTag("schedule_impact_dialog")
    )
  }

  // Samsung One UI Styled Safe Delete / Fluid Twin Action Dialog
  taskToDeleteConfirm?.let { task ->
    val partnerTask = if (task.isFluidTwin && !task.fluidTwinId.isNullOrBlank()) {
      (scheduleResult.scheduledTasks + scheduleResult.overflowTasks).firstOrNull {
        it.fluidTwinId == task.fluidTwinId && it.id != task.id && !it.isDeleted
      }
    } else null

    if (task.isFluidTwin && !task.fluidTwinId.isNullOrBlank() && partnerTask != null) {
      // Specialized Fluid Twin Dialog
      AlertDialog(
        onDismissRequest = { taskToDeleteConfirm = null },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.CallSplit,
              contentDescription = null,
              tint = CyanAccent,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Fluid Twin Task Action",
              fontWeight = FontWeight.Bold,
              color = TextPrimary,
              fontSize = 18.sp
            )
          }
        },
        text = {
          Column {
            Text(
              text = "“${task.title}” is linked with “${partnerTask.title}” (Total Pool: ${task.fluidTotalPoolMinutes ?: (task.durationMinutes + partnerTask.durationMinutes)}m).",
              color = TextPrimary,
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Would you like to absorb this task's ${task.durationMinutes}m into “${partnerTask.title}”, or delete both parts completely?",
              color = TextSecondary,
              fontSize = 13.sp,
              lineHeight = 18.sp
            )
          }
        },
        confirmButton = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val targetId = task.id
                val partnerId = partnerTask.id
                taskToDeleteConfirm = null
                if (onDeleteTwinAndAbsorbTime != null) {
                  onDeleteTwinAndAbsorbTime(targetId, partnerId)
                } else {
                  onMoveToTrash(targetId)
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = CyanAccent,
                contentColor = Color.Black
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("absorb_twin_duration_button")
            ) {
              Text("Absorb Time into Partner", fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val twinId = task.fluidTwinId!!
                taskToDeleteConfirm = null
                if (onDeleteBothFluidTwinParts != null) {
                  onDeleteBothFluidTwinParts(twinId)
                } else {
                  onMoveToTrash(task.id)
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = DangerRed,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("delete_both_twin_parts_button")
            ) {
              Text("Delete Both Parts", fontWeight = FontWeight.Bold)
            }

            TextButton(
              onClick = { taskToDeleteConfirm = null },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("cancel_fluid_twin_dialog_button")
            ) {
              Text("Cancel", color = TextSecondary)
            }
          }
        },
        dismissButton = null,
        containerColor = OneUiSurfaceElevated,
        shape = RoundedCornerShape(24.dp)
      )
    } else {
      AlertDialog(
        onDismissRequest = { taskToDeleteConfirm = null },
        title = {
          Text(
            text = "Delete Task?",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 18.sp
          )
        },
        text = {
          Text(
            text = "This task will be moved to the Trash Bin. You can restore it within 30 days.",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
          )
        },
        confirmButton = {
          Button(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              val id = task.id
              taskToDeleteConfirm = null
              onMoveToTrash(id)
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = DangerRed,
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("confirm_move_to_trash_button")
          ) {
            Text("Move to Trash", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(
            onClick = { taskToDeleteConfirm = null },
            modifier = Modifier.testTag("cancel_delete_task_button")
          ) {
            Text("Cancel", color = TextSecondary)
          }
        },
        containerColor = OneUiSurfaceElevated,
        shape = RoundedCornerShape(24.dp)
      )
    }
  }
}

@Composable
fun OneUiHeader(
  activeDay: RoutineDay,
  systemToday: RoutineDay,
  onSelectDay: (RoutineDay) -> Unit,
  totalScheduledMinutes: Int,
  scheduledCount: Int,
  overflowCount: Int,
  deletedCount: Int,
  scheduledTasks: List<RoutineTask>,
  currentMinute: Int = -1,
  isToday: Boolean = true,
  hasPendingEdits: Boolean = false,
  onShowImpactDialog: () -> Unit = {},
  onCommitPendingEdits: () -> Unit = {},
  onDiscardPendingEdits: () -> Unit = {},
  sortMode: TimelineSortMode,
  onSelectSortMode: (TimelineSortMode) -> Unit,
  onOpenTrashBin: () -> Unit
) {
  val haptic = LocalHapticFeedback.current
  var sortMenuExpanded by remember { mutableStateOf(false) }

  val nextCountdownLabel = remember(scheduledTasks, currentMinute, isToday) {
    if (!isToday || currentMinute < 0 || scheduledTasks.isEmpty()) {
      null
    } else {
      val nextTaskToday = scheduledTasks
        .filter { it.startTimeMinute > currentMinute }
        .minByOrNull { it.startTimeMinute }
      val diff = if (nextTaskToday != null) {
        nextTaskToday.startTimeMinute - currentMinute
      } else {
        val earliestTomorrow = scheduledTasks.minByOrNull { it.startTimeMinute }
        if (earliestTomorrow != null) (earliestTomorrow.startTimeMinute + 1440) - currentMinute else 0
      }
      if (diff <= 0) {
        null
      } else {
        val h = diff / 60
        val m = diff % 60
        when {
          h > 0 && m > 0 -> "${h}h ${m}m"
          h > 0 -> "${h}h"
          else -> "${m}m"
        }
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 8.dp, bottom = 8.dp)
  ) {
    // Top Row: App Title & Subtitle + Top-Right Countdown Pill or Pending Drag Controls
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Chronos",
          fontSize = 32.sp,
          fontWeight = FontWeight.ExtraBold,
          color = TextPrimary
        )
        Text(
          text = "Smart Dynamic Routine & Device Automation",
          fontSize = 13.sp,
          color = TextSecondary,
          fontWeight = FontWeight.Medium
        )
      }

      if (hasPendingEdits) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(2.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.testTag("pending_edits_header_controls")
        ) {
          IconButton(
            onClick = onShowImpactDialog,
            modifier = Modifier
              .size(48.dp)
              .testTag("pending_impact_history_button")
          ) {
            Icon(
              imageVector = Icons.Default.History,
              contentDescription = "Review Schedule Impact",
              tint = CyanAccent
            )
          }
          IconButton(
            onClick = onCommitPendingEdits,
            modifier = Modifier
              .size(48.dp)
              .testTag("pending_commit_button")
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Commit Schedule Edits",
              tint = SuccessGreen
            )
          }
          IconButton(
            onClick = onDiscardPendingEdits,
            modifier = Modifier
              .size(48.dp)
              .testTag("pending_discard_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Discard Schedule Edits",
              tint = DangerRed
            )
          }
        }
      } else if (nextCountdownLabel != null) {
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = OneUiSurfaceElevated,
          border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.4f)),
          modifier = Modifier.testTag("next_task_countdown_pill")
        ) {
          Text(
            text = nextCountdownLabel,
            color = CyanAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Day Pills Selector (Sat to Fri): Real Today vs Inspected Day Visual Duality
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      items(RoutineDay.entries) { day ->
        val isRealToday = day == systemToday
        val isSelected = day == activeDay

        // Visual Duality Styling
        val (pillBorder, pillBackground, pillTextColor) = when {
          isSelected && isRealToday -> Triple(
            BorderStroke(1.5.dp, Color(0xFF10B981)), // Emerald glow for active real today
            Color(0xFF10B981).copy(alpha = 0.22f),
            Color.White
          )
          isSelected && !isRealToday -> Triple(
            BorderStroke(1.5.dp, Color(0xFFEF4444)), // Crimson Red for active inspected day
            Color(0xFFEF4444).copy(alpha = 0.18f),
            Color.White
          )
          !isSelected && isRealToday -> Triple(
            BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)), // Subtle emerald for unselected real today
            OneUiSurfaceElevated,
            Color(0xFFE2E8F0)
          )
          else -> Triple(
            BorderStroke(1.dp, OneUiBorder),
            OneUiSurfaceElevated,
            TextSecondary
          )
        }

        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(pillBorder, RoundedCornerShape(16.dp))
            .clickable {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onSelectDay(day)
            }
            .testTag("day_pill_${day.name.lowercase()}"),
          color = pillBackground,
          shape = RoundedCornerShape(16.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = day.shortName,
              color = pillTextColor,
              fontWeight = if (isSelected || isRealToday) FontWeight.Bold else FontWeight.Medium,
              fontSize = 13.sp
            )

            if (isRealToday) {
              // Persistent small emerald green indicator dot (● #10B981) permanently marking real today
              Spacer(modifier = Modifier.width(5.dp))
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF10B981))
              )
            } else if (isSelected) {
              // Glowing red indicator dot (● #EF4444) for inspected day (not real today)
              Spacer(modifier = Modifier.width(5.dp))
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(Color(0xFFEF4444))
              )
            }
          }
        }
      }
    }

    // Inspected Day Contextual Indicator Banner (When user inspects a day other than real today)
    if (activeDay != systemToday) {
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFFEF4444).copy(alpha = 0.12f))
          .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f), RoundedCornerShape(10.dp))
          .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(Color(0xFFEF4444))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Inspecting ${activeDay.fullName} • Today is ${systemToday.fullName}",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFFFCA5A5)
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 24h Timeline Visual Ribbon
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp)),
      color = OneUiSurfaceElevated
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(text = "24-Hour Cycle Map", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

          // Sorting Dropdown Chip (Requirement 2)
          Box {
            Row(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .clickable { sortMenuExpanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .testTag("timeline_sort_chip"),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = AnchorGradientStart, modifier = Modifier.size(13.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = sortMode.label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            }

            DropdownMenu(
              expanded = sortMenuExpanded,
              onDismissRequest = { sortMenuExpanded = false },
              modifier = Modifier.background(OneUiSurfaceElevated)
            ) {
              TimelineSortMode.entries.forEach { mode ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = mode.label,
                      color = if (mode == sortMode) AnchorGradientStart else TextPrimary,
                      fontWeight = if (mode == sortMode) FontWeight.Bold else FontWeight.Normal,
                      fontSize = 12.sp
                    )
                  },
                  onClick = {
                    sortMenuExpanded = false
                    onSelectSortMode(mode)
                  }
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Visual Ribbon Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color.White.copy(alpha = 0.08f))
        ) {
          for (task in scheduledTasks) {
            val weight = (task.durationMinutes.toFloat() / 1440f).coerceAtLeast(0.01f)
            val segmentColor = when (task.type) {
              TaskType.ANCHOR_SLEEP -> SleepGradientStart
              TaskType.ANCHOR_ELASTIC -> ElasticGradientStart
              TaskType.ANCHOR_STANDARD -> AnchorGradientStart
              TaskType.DYNAMIC -> DynamicGradientStart
            }
            Box(
              modifier = Modifier
                .weight(weight)
                .height(10.dp)
                .background(segmentColor)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Quick Metrics Strip (Frosted One UI 8.5 Luxury Aesthetic)
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(20.dp)),
      color = Color(0xFF121620)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        MetricItem(
          label = "7 Anchors",
          value = "Locked 🔒",
          dotColor = Color(0xFF818CF8),
          valueColor = Color(0xFFA5B4FC)
        )
        Box(
          modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(Color.White.copy(alpha = 0.08f))
        )
        MetricItem(
          label = "Scheduled",
          value = "$scheduledCount tasks",
          dotColor = Color(0xFF38BDF8),
          valueColor = Color(0xFFE2E8F0)
        )
        Box(
          modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(Color.White.copy(alpha = 0.08f))
        )
        MetricItem(
          label = "Overflow",
          value = if (overflowCount == 0) "Clear ✓" else "$overflowCount pending",
          dotColor = if (overflowCount == 0) Color(0xFF10B981) else WarningAmber,
          valueColor = if (overflowCount == 0) Color(0xFF34D399) else WarningAmber
        )
      }
    }
  }
}

@Composable
private fun MetricItem(label: String, value: String, dotColor: Color, valueColor: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .clip(CircleShape)
          .background(dotColor)
      )
      Spacer(modifier = Modifier.width(5.dp))
      Text(
        text = label,
        color = Color(0xFF94A3B8),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.4.sp
      )
    }
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = value,
      color = valueColor,
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
fun ActiveAutomationBanner(
  automationState: ActiveAutomationState,
  onStop: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .border(BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f)), RoundedCornerShape(20.dp))
      .testTag("active_automation_banner"),
    color = Color(0xFF1E1B4B).copy(alpha = 0.85f)
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
        Icon(
          imageVector = Icons.Default.RecordVoiceOver,
          contentDescription = null,
          tint = Color(0xFFA5B4FC),
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = automationState.taskTitle ?: "Automation Active",
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
            if (automationState.isDndActive) {
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "• DND 🌙",
                color = WarningAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
          Text(
            text = automationState.notificationBody ?: automationState.voiceMessage ?: "Running routine task",
            color = Color(0xFFCBD5E1),
            fontSize = 11.sp,
            maxLines = 1
          )
        }
      }

      Button(
        onClick = onStop,
        colors = ButtonDefaults.buttonColors(
          containerColor = DangerRed,
          contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.testTag("stop_automation_button")
      ) {
        Text("Stop & Revert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
fun DndPermissionBanner(
  onOpenSettings: () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .border(BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)), RoundedCornerShape(20.dp)),
    color = WarningAmber.copy(alpha = 0.12f)
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
        Icon(
          imageVector = Icons.Default.Security,
          contentDescription = null,
          tint = WarningAmber,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "DND Policy Access Needed",
            color = WarningAmber,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
          Text(
            text = "Grant permission for automatic Do Not Disturb mode switching",
            color = TextSecondary,
            fontSize = 11.sp
          )
        }
      }

      Button(
        onClick = onOpenSettings,
        colors = ButtonDefaults.buttonColors(
          containerColor = WarningAmber,
          contentColor = Color.Black
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = Modifier.testTag("grant_dnd_permission_button")
      ) {
        Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun DestinationDropShadowSlot(
  visible: Boolean,
  targetCardHeightDp: Dp,
  snapClose: Boolean,
  draggedTitle: String,
  preview: VerticalDropPreview?,
  durationMinutes: Int,
  isBottomSlot: Boolean = false,
  onSlotTopPositioned: (Float) -> Unit
) {
  val spacingDp = 14.dp
  val targetTotalDp = if (visible && targetCardHeightDp > 0.dp) targetCardHeightDp + spacingDp else 0.dp
  val animatedTotalDp by animateDpAsState(
    targetValue = targetTotalDp,
    animationSpec = spring(
      stiffness = Spring.StiffnessMediumLow,
      dampingRatio = Spring.DampingRatioNoBouncy
    ),
    label = "drop_slot_height"
  )
  val displayTotalDp = if (!visible && snapClose) 0.dp else animatedTotalDp

  if (displayTotalDp > 1.dp) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .height(displayTotalDp)
        .onGloballyPositioned { coords ->
          if (visible && coords.isAttached) {
            onSlotTopPositioned(coords.positionInRoot().y)
          }
        }
    ) {
      if (isBottomSlot) {
        Spacer(modifier = Modifier.height(spacingDp))
      }
      val cardSpaceDp = (displayTotalDp - spacingDp).coerceAtLeast(0.dp)
      if (cardSpaceDp > 24.dp && preview != null) {
        val startLabel = RoutineTask.formatTime(preview.previewStartMinute)
        val endLabel = RoutineTask.formatTime(preview.previewEndMinute)
        val accentColor = if (preview.hitAnchorWall) WarningAmber else CyanAccent

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(cardSpaceDp)
            .clip(RoundedCornerShape(24.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .drawWithCache {
              val radius = 24.dp.toPx()
              val stroke = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
              )
              onDrawBehind {
                drawRoundRect(
                  color = accentColor,
                  cornerRadius = CornerRadius(radius, radius),
                  style = stroke
                )
              }
            }
            .testTag("drop_shadow_indicator"),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = draggedTitle,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
              Surface(
                shape = RoundedCornerShape(50),
                color = accentColor.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.7f))
              ) {
                Text(
                  text = if (preview.hitAnchorWall) "Anchor Wall" else "Drop here",
                  color = accentColor,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = "$startLabel → $endLabel • ${durationMinutes}m",
              color = accentColor,
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
      if (!isBottomSlot) {
        Spacer(modifier = Modifier.height(spacingDp))
      }
    }
  }
}

