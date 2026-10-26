package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoutineTask
import com.example.model.TaskType
import com.example.ui.components.taskRatingCache
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OneUiBackground
import com.example.ui.theme.OneUiBorder
import com.example.ui.theme.OneUiSurfaceElevated
import com.example.ui.theme.OneUiSurfaceHigh
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Analytics Category definition for 24-Hour Donut and Progress Bars.
 */
data class AnalyticsCategory(
  val id: String,
  val icon: String,
  val name: String,
  val color: Color
)

val ANALYTICS_CATEGORIES = mapOf(
  "sleep" to AnalyticsCategory("sleep", "☁️", "Sleep Recovery", Color(0xFF6366F1)),
  "work" to AnalyticsCategory("work", "💼", "Productive Work", Color(0xFF3B82F6)),
  "studying" to AnalyticsCategory("studying", "👓", "Deep Study", Color(0xFF06B6D4)),
  "break" to AnalyticsCategory("break", "☕", "Rest & Break", Color(0xFFF97316)),
  "training" to AnalyticsCategory("training", "🏃", "Fitness & Health", Color(0xFF10B981)),
  "eating" to AnalyticsCategory("eating", "🍴", "Nutrition & Meals", Color(0xFFF59E0B)),
  "goals" to AnalyticsCategory("goals", "🎯", "Personal Goals", Color(0xFFA855F7)),
  "reading" to AnalyticsCategory("reading", "📖", "Deep Reading", Color(0xFF14B8A6)),
  "family" to AnalyticsCategory("family", "🏠", "Home & Family", Color(0xFFEC4899)),
  "relationship" to AnalyticsCategory("relationship", "❤️", "Relationships", Color(0xFFF43F5E)),
  "commuting" to AnalyticsCategory("commuting", "🚗", "Commute & Transit", Color(0xFF64748B)),
  "internet" to AnalyticsCategory("internet", "🖥️", "Web & Research", Color(0xFF8B5CF6)),
  "tv" to AnalyticsCategory("tv", "📺", "Leisure & Media", Color(0xFF6B7280)),
  "anchor_elastic" to AnalyticsCategory("anchor_elastic", "🛡️", "Elastic Buffer", Color(0xFF8B5CF6)),
  "anchor_standard" to AnalyticsCategory("anchor_standard", "⚓", "Anchor Pillar", Color(0xFF3B82F6))
)

fun resolveTaskCategory(task: RoutineTask): AnalyticsCategory {
  if (task.type == TaskType.ANCHOR_SLEEP || task.slotNumber == 1) {
    return ANALYTICS_CATEGORIES["sleep"]!!
  }
  if (task.type == TaskType.ANCHOR_ELASTIC || task.slotNumber == 5) {
    return ANALYTICS_CATEGORIES["anchor_elastic"]!!
  }
  val catId = task.category?.lowercase()
  if (catId != null && ANALYTICS_CATEGORIES.containsKey(catId)) {
    return ANALYTICS_CATEGORIES[catId]!!
  }
  val titleLower = task.title.lowercase()
  return when {
    titleLower.contains("sleep") || titleLower.contains("bed") || titleLower.contains("nap") ->
      ANALYTICS_CATEGORIES["sleep"]!!
    titleLower.contains("work") || titleLower.contains("code") || titleLower.contains("job") ->
      ANALYTICS_CATEGORIES["work"]!!
    titleLower.contains("study") || titleLower.contains("learn") || titleLower.contains("exam") ->
      ANALYTICS_CATEGORIES["studying"]!!
    titleLower.contains("break") || titleLower.contains("coffee") || titleLower.contains("tea") ->
      ANALYTICS_CATEGORIES["break"]!!
    titleLower.contains("train") || titleLower.contains("gym") || titleLower.contains("run") || titleLower.contains("workout") ->
      ANALYTICS_CATEGORIES["training"]!!
    titleLower.contains("eat") || titleLower.contains("lunch") || titleLower.contains("dinner") || titleLower.contains("breakfast") || titleLower.contains("meal") ->
      ANALYTICS_CATEGORIES["eating"]!!
    titleLower.contains("read") || titleLower.contains("book") ->
      ANALYTICS_CATEGORIES["reading"]!!
    titleLower.contains("goal") ->
      ANALYTICS_CATEGORIES["goals"]!!
    titleLower.contains("family") || titleLower.contains("home") ->
      ANALYTICS_CATEGORIES["family"]!!
    task.type == TaskType.ANCHOR_STANDARD ->
      ANALYTICS_CATEGORIES["anchor_standard"]!!
    else ->
      AnalyticsCategory(id = "general", icon = "⚡", name = task.title.take(18), color = Color(0xFF06B6D4))
  }
}

enum class AnalyticsTimeRange(val label: String, val multiplier: Int, val headerLabel: String) {
  DAY("Day", 1, "TODAY 24H"),
  WEEK("Week", 7, "WEEK TOTAL"),
  MONTH("Month", 30, "MONTH TOTAL"),
  THREE_MONTHS("3 Months", 90, "3-MO TOTAL")
}

enum class AnalyticsViewMode {
  DONUT_CHART,
  PROGRESS_BARS
}

data class CategoryAggregate(
  val category: AnalyticsCategory,
  val totalMinutes: Int,
  val percentage: Float,
  val taskCount: Int,
  val avgRating: Int?
)

data class OverviewCategoryItem(
  val id: String,
  val icon: String,
  val name: String,
  val count: Int
)

/**
 * PHASE 5B / 6C / PART 2: 24-Hour Circular Donut & Dual-Mode Analytics Chart Engine in SettingsScreen.
 * Pure real-rating driven historical metrics & categorized overview list with task length counts.
 */
@Composable
fun SettingsScreen(
  sleepDurationMinutes: Int = 420,
  bufferMinutes: Int,
  slot1Task: RoutineTask? = null,
  slot2Task: RoutineTask? = null,
  deletedCount: Int = 0,
  scheduledTasks: List<RoutineTask> = emptyList(),
  onUpdateSleepDuration: (Float) -> Unit = {},
  onUpdateBufferMinutes: (Int) -> Unit,
  onOpenTrashBin: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var restBufferInput by remember(bufferMinutes) {
    mutableStateOf(bufferMinutes.toString())
  }

  // Analytics State
  var selectedRange by remember { mutableStateOf(AnalyticsTimeRange.DAY) }
  var viewMode by remember { mutableStateOf(AnalyticsViewMode.DONUT_CHART) }
  var inspectedTaskId by remember { mutableStateOf<String?>(null) }

  // Effective tasks to visualize
  val effectiveTasks = remember(scheduledTasks, slot1Task, slot2Task) {
    if (scheduledTasks.isNotEmpty()) {
      scheduledTasks.filter { !it.isDeleted }
    } else {
      listOfNotNull(slot1Task, slot2Task)
    }
  }

  // Calculate day metrics
  val totalScheduledDayMinutes = remember(effectiveTasks) {
    effectiveTasks.sumOf { it.durationMinutes }.coerceAtMost(1440)
  }

  // Check if real user completion ratings exist for historical modes (Week, Month, 3 Months)
  val hasHistoricalData = remember(taskRatingCache) {
    taskRatingCache.isNotEmpty()
  }

  // Aggregated Categories (pure user-rating driven for historical modes; scheduled routine for Day mode)
  val categoryAggregates = remember(effectiveTasks, selectedRange, hasHistoricalData) {
    val multiplier = selectedRange.multiplier
    val isHistorical = selectedRange != AnalyticsTimeRange.DAY
    val relevantTasks = if (isHistorical) {
      effectiveTasks.filter { taskRatingCache.containsKey(it.id) }
    } else {
      effectiveTasks
    }

    val totalTimePool = if (isHistorical) {
      relevantTasks.sumOf { it.durationMinutes } * multiplier
    } else {
      1440 * multiplier
    }

    val grouped = relevantTasks.groupBy { resolveTaskCategory(it) }
    grouped.map { (category, tasks) ->
      val totalMins = tasks.sumOf { it.durationMinutes } * multiplier
      val pct = if (totalTimePool > 0) (totalMins.toFloat() / totalTimePool.toFloat()) * 100f else 0f
      val ratings = tasks.mapNotNull { taskRatingCache[it.id] }
      val avgRating = if (ratings.isNotEmpty()) ratings.average().toInt() else null
      CategoryAggregate(
        category = category,
        totalMinutes = totalMins,
        percentage = pct,
        taskCount = tasks.size * multiplier,
        avgRating = avgRating
      )
    }.sortedByDescending { it.totalMinutes }
  }

  // Overall Focus Rating average from all tasks with ratings
  val overallAvgRating = remember(effectiveTasks) {
    val ratings = effectiveTasks.mapNotNull { taskRatingCache[it.id] }
    if (ratings.isNotEmpty()) ratings.average().toInt() else null
  }

  // Overview Category Filter State
  var selectedOverviewCategoryId by remember { mutableStateOf<String?>("all") }

  val overviewCategoryChips = remember(effectiveTasks) {
    effectiveTasks
      .groupBy { resolveTaskCategory(it) }
      .map { (cat, tasks) ->
        OverviewCategoryItem(
          id = cat.id,
          icon = cat.icon,
          name = cat.name,
          count = tasks.size
        )
      }
      .sortedByDescending { it.count }
  }

  val filteredOverviewTasks = remember(effectiveTasks, selectedOverviewCategoryId) {
    if (selectedOverviewCategoryId == null || selectedOverviewCategoryId == "all") {
      effectiveTasks
    } else {
      effectiveTasks.filter { resolveTaskCategory(it).id == selectedOverviewCategoryId }
    }
  }

  // Currently inspected task
  val inspectedTask = remember(inspectedTaskId, effectiveTasks) {
    effectiveTasks.firstOrNull { it.id == inspectedTaskId }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF080A0F)) // Pure AMOLED Deep Obsidian
      .statusBarsPadding()
      .animateContentSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 16.dp)
      .padding(bottom = 36.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // ==============================================================
    // 1. HEADER
    // ==============================================================
    Column {
      Text(
        text = "Engine Analytics & Routine Insights",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "24-hour circadian visualizer, category balance & engine buffers",
        fontSize = 12.sp,
        color = TextSecondary
      )
    }

    // ==============================================================
    // 2. 24-HOUR CIRCULAR DONUT & DUAL-MODE ANALYTICS CARD
    // ==============================================================
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(22.dp))
        .testTag("analytics_engine_card"),
      color = Color(0xFF121620)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        // Card Top Bar: Title & View Mode Toggle
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Timelapse,
                contentDescription = null,
                tint = Color(0xFF60A5FA),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Circadian Distribution",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8FAFC)
              )
              Text(
                text = if (viewMode == AnalyticsViewMode.DONUT_CHART) "24h Donut Clock" else "Category Progress",
                fontSize = 11.sp,
                color = TextSecondary
              )
            }
          }

          // Dual-View Mode Segmented Toggle Buttons
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF1A2232))
              .border(1.dp, Color(0xFF1F2637), RoundedCornerShape(12.dp))
              .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            // View 1: Donut Chart
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                  if (viewMode == AnalyticsViewMode.DONUT_CHART) Color(0xFF3B82F6) else Color.Transparent
                )
                .clickable { viewMode = AnalyticsViewMode.DONUT_CHART }
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("view_mode_donut_button")
            ) {
              Icon(
                imageVector = Icons.Default.Timelapse,
                contentDescription = "Donut View",
                tint = if (viewMode == AnalyticsViewMode.DONUT_CHART) Color.White else TextSecondary,
                modifier = Modifier.size(16.dp)
              )
            }

            // View 2: Horizontal Progress Bars
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                  if (viewMode == AnalyticsViewMode.PROGRESS_BARS) Color(0xFF3B82F6) else Color.Transparent
                )
                .clickable { viewMode = AnalyticsViewMode.PROGRESS_BARS }
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("view_mode_progress_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = "List View",
                tint = if (viewMode == AnalyticsViewMode.PROGRESS_BARS) Color.White else TextSecondary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Time Range Filter Chips: [ Day ] | [ Week ] | [ Month ] | [ Year ]
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          AnalyticsTimeRange.entries.forEach { range ->
            val isSelected = selectedRange == range
            val chipBg by animateColorAsState(
              targetValue = if (isSelected) Color(0xFF3B82F6).copy(alpha = 0.2f) else Color(0xFF161B26),
              label = "chip_bg"
            )
            val chipBorder by animateColorAsState(
              targetValue = if (isSelected) Color(0xFF3B82F6) else Color(0xFF1F2637),
              label = "chip_border"
            )
            val chipText by animateColorAsState(
              targetValue = if (isSelected) Color(0xFF93C5FD) else TextSecondary,
              label = "chip_text"
            )

            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(chipBg)
                .border(1.dp, chipBorder, RoundedCornerShape(12.dp))
                .clickable {
                  selectedRange = range
                  inspectedTaskId = null
                }
                .padding(vertical = 8.dp)
                .testTag("filter_range_${range.name.lowercase()}"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = range.label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = chipText
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (selectedRange != AnalyticsTimeRange.DAY && !hasHistoricalData) {
          // Sleek Samsung One UI Empty State for Historical Analytics (Fresh Install)
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(20.dp))
              .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(20.dp))
              .testTag("analytics_empty_history_card"),
            color = Color(0xFF161B26)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF3B82F6).copy(alpha = 0.15f))
                  .border(BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.35f)), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Timeline,
                  contentDescription = "Analytics Timeline",
                  tint = Color(0xFF60A5FA),
                  modifier = Modifier.size(28.dp)
                )
              }

              Spacer(modifier = Modifier.height(14.dp))

              Text(
                text = "No Completion History Yet",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8FAFC),
                textAlign = TextAlign.Center
              )

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = "Historical focus trends update as you complete routine tasks and log focus ratings.",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
              )
            }
          }
        } else {
          // ==========================================================
          // VIEW 1: 24-HOUR CIRCULAR DONUT CHART CANVAS
          // ==========================================================
          if (viewMode == AnalyticsViewMode.DONUT_CHART) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
              .height(285.dp),
            contentAlignment = Alignment.Center
          ) {
            Canvas(
              modifier = Modifier
                .size(270.dp)
                .pointerInput(effectiveTasks) {
                  detectTapGestures { tapOffset ->
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val dx = tapOffset.x - centerX
                    val dy = tapOffset.y - centerY
                    val dist = sqrt(dx * dx + dy * dy)
                    val chartDiameterPx = size.width - 48.dp.toPx()
                    val strokePx = 30.dp.toPx()
                    val outerR = (chartDiameterPx / 2f) + (strokePx / 2f)
                    val innerR = (chartDiameterPx / 2f) - (strokePx / 2f)

                    if (dist in (innerR - 10f)..(outerR + 10f)) {
                      // Tap inside donut ring: calculate angle from 00:00 (top, -90 deg)
                      val rawAngle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90.0 + 360.0) % 360.0
                      val tapMinute = ((rawAngle / 360.0) * 1440.0).toInt() % 1440

                      // Find matching task
                      val matched = effectiveTasks.firstOrNull { t ->
                        val start = t.startTimeMinute
                        val end = (start + t.durationMinutes)
                        if (end <= 1440) {
                          tapMinute in start until end
                        } else {
                          // Spans midnight
                          tapMinute >= start || tapMinute < (end % 1440)
                        }
                      }
                      inspectedTaskId = if (inspectedTaskId == matched?.id) null else matched?.id
                    } else {
                      // Tapped in hole or outside ring: unselect
                      inspectedTaskId = null
                    }
                  }
                }
                .testTag("circular_donut_canvas")
            ) {
              val strokeWidthPx = 28.dp.toPx()
              val paddingPx = 24.dp.toPx()
              val chartDiameter = size.minDimension - paddingPx * 2
              val topLeft = Offset(
                x = (size.width - chartDiameter) / 2f,
                y = (size.height - chartDiameter) / 2f
              )
              val arcSize = Size(chartDiameter, chartDiameter)

              // 1. Draw 360-degree subtle background track
              drawArc(
                color = Color(0xFF161C28),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx)
              )

              // 2. Draw 24-hour dial hour markers (00h, 06h, 12h, 18h)
              val center = Offset(size.width / 2f, size.height / 2f)
              val markerRadiusOuter = (chartDiameter / 2f) + (strokeWidthPx / 2f) + 4.dp.toPx()
              val markerRadiusInner = markerRadiusOuter - 6.dp.toPx()

              for (hour in listOf(0, 6, 12, 18)) {
                val hourAngle = Math.toRadians((hour * 15.0 - 90.0))
                val p1 = Offset(
                  x = center.x + (markerRadiusInner * kotlin.math.cos(hourAngle)).toFloat(),
                  y = center.y + (markerRadiusInner * kotlin.math.sin(hourAngle)).toFloat()
                )
                val p2 = Offset(
                  x = center.x + (markerRadiusOuter * kotlin.math.cos(hourAngle)).toFloat(),
                  y = center.y + (markerRadiusOuter * kotlin.math.sin(hourAngle)).toFloat()
                )
                drawLine(
                  color = Color(0xFF475569),
                  start = p1,
                  end = p2,
                  strokeWidth = 2.dp.toPx()
                )
              }

              // 3. Draw active task segments
              effectiveTasks.forEach { task ->
                val isSelected = task.id == inspectedTaskId
                val category = resolveTaskCategory(task)
                val startAngle = ((task.startTimeMinute / 1440f) * 360f) - 90f
                val sweepAngle = ((task.durationMinutes / 1440f) * 360f).coerceAtLeast(1f)

                val effectiveStroke = if (isSelected) strokeWidthPx + 6.dp.toPx() else strokeWidthPx
                val effectiveColor = if (inspectedTaskId == null || isSelected) {
                  category.color
                } else {
                  category.color.copy(alpha = 0.35f)
                }

                drawArc(
                  color = effectiveColor,
                  startAngle = startAngle,
                  sweepAngle = sweepAngle,
                  useCenter = false,
                  topLeft = topLeft,
                  size = arcSize,
                  style = Stroke(
                    width = effectiveStroke,
                    cap = if (sweepAngle > 4f) StrokeCap.Round else StrokeCap.Butt
                  )
                )

                // Highlight border when inspected
                if (isSelected) {
                  drawArc(
                    color = Color.White,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = 2.dp.toPx())
                  )
                }
              }
            }

            // Central Hole Readout
            Column(
              modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(Color(0xFF0C1017))
                .border(1.dp, Color(0xFF1F2637), CircleShape)
                .padding(10.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              if (inspectedTask != null) {
                val cat = resolveTaskCategory(inspectedTask)
                Text(
                  text = "${cat.icon} ${cat.name}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = cat.color,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val h = inspectedTask.durationMinutes / 60
                val m = inspectedTask.durationMinutes % 60
                Text(
                  text = if (h > 0 && m > 0) "${h}h ${m}m" else if (h > 0) "${h}h" else "${m}m",
                  fontSize = 19.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White
                )
                val share = ((inspectedTask.durationMinutes.toFloat() / 1440f) * 100f).toInt()
                Text(
                  text = "${inspectedTask.formattedStartTime} • $share%",
                  fontSize = 10.sp,
                  color = Color(0xFF94A3B8)
                )
              } else {
                Text(
                  text = selectedRange.headerLabel,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp,
                  color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(2.dp))
                val totalH = (totalScheduledDayMinutes * selectedRange.multiplier) / 60
                val totalM = (totalScheduledDayMinutes * selectedRange.multiplier) % 60
                Text(
                  text = "${totalH}h ${totalM}m",
                  fontSize = 19.sp,
                  fontWeight = FontWeight.Black,
                  color = Color.White
                )
                val activePct = ((totalScheduledDayMinutes.toFloat() / 1440f) * 100f).toInt()
                Text(
                  text = if (totalScheduledDayMinutes > 0) "$activePct% Scheduled" else "0% Pristine",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Medium,
                  color = Color(0xFF10B981)
                )
              }
            }
          }

          // Inline Floating Inspection Badge below Donut
          AnimatedVisibility(visible = inspectedTask != null) {
            inspectedTask?.let { task ->
              val cat = resolveTaskCategory(task)
              Spacer(modifier = Modifier.height(10.dp))
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .border(1.5.dp, cat.color.copy(alpha = 0.8f), RoundedCornerShape(14.dp)),
                color = Color(0xFF161C28)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(text = cat.icon, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text = task.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text = "${task.formattedStartTime} → ${task.formattedEndTime} • ${task.formattedDuration}",
                        fontSize = 11.sp,
                        color = cat.color
                      )
                    }
                  }

                  IconButton(
                    onClick = { inspectedTaskId = null },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Clear Inspection",
                      tint = TextSecondary,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }
              }
            }
          }

          if (inspectedTask == null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Tap on any circular dial segment to inspect duration and time",
              fontSize = 11.sp,
              color = Color(0xFF64748B),
              textAlign = TextAlign.Center,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }

        // ==========================================================
        // VIEW 2: HORIZONTAL CATEGORY PROGRESS BARS
        // ==========================================================
        if (viewMode == AnalyticsViewMode.PROGRESS_BARS) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            if (categoryAggregates.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "No scheduled tasks in pristine schedule",
                  color = TextTertiary,
                  fontSize = 13.sp
                )
              }
            } else {
              categoryAggregates.forEach { item ->
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF161B26))
                    .border(1.dp, Color(0xFF1F2637), RoundedCornerShape(12.dp))
                    .padding(12.dp)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(text = item.category.icon, fontSize = 16.sp)
                      Spacer(modifier = Modifier.width(8.dp))
                      Text(
                        text = item.category.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                      )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                      val h = item.totalMinutes / 60
                      val m = item.totalMinutes % 60
                      val durStr = if (h > 0 && m > 0) "${h}h ${m}m" else if (h > 0) "${h}h" else "${m}m"
                      Text(
                        text = "$durStr • ${String.format("%.1f", item.percentage)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = item.category.color
                      )

                      if (item.avgRating != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                          modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                          Text(
                            text = "★ ${item.avgRating}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                          )
                        }
                      }
                    }
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  // Linear Progress Bar
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(7.dp)
                      .clip(RoundedCornerShape(4.dp))
                      .background(Color(0xFF222B3D))
                  ) {
                    Box(
                      modifier = Modifier
                        .fillMaxWidth(fraction = (item.percentage / 100f).coerceIn(0.02f, 1f))
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(item.category.color)
                    )
                  }
                }
              }

              // Free Unscheduled Gap Bar
              val freeMinutes = (1440 - totalScheduledDayMinutes).coerceAtLeast(0)
              val freePct = (freeMinutes.toFloat() / 1440f) * 100f
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xFF161B26).copy(alpha = 0.6f))
                  .border(1.dp, Color(0xFF1F2637), RoundedCornerShape(12.dp))
                  .padding(12.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "⚡", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Free Inter-Task Buffer",
                      fontSize = 13.sp,
                      fontWeight = FontWeight.Medium,
                      color = TextSecondary
                    )
                  }

                  val freeH = freeMinutes / 60
                  val freeM = freeMinutes % 60
                  Text(
                    text = "${freeH}h ${freeM}m • ${String.format("%.1f", freePct)}%",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                  )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF222B3D))
                ) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth(fraction = (freePct / 100f).coerceIn(0.01f, 1f))
                      .height(6.dp)
                      .clip(RoundedCornerShape(4.dp))
                      .background(Color(0xFF475569))
                  )
                }
              }
            }
          }
        }
      }

        // ==========================================================
        // HISTORICAL METRIC SUMMARY (Week, Month, 3 Months mode)
        // ==========================================================
        if (selectedRange != AnalyticsTimeRange.DAY && hasHistoricalData) {
          Spacer(modifier = Modifier.height(14.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Card A: Total Focus Rating
            Surface(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFF1F2637), RoundedCornerShape(14.dp)),
              color = Color(0xFF161B26)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "AVG FOCUS RATING",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextTertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (overallAvgRating != null) "$overallAvgRating%" else "Unrated",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (overallAvgRating != null) Color(0xFFF59E0B) else TextSecondary
                  )
                }
              }
            }

            // Card B: Routine Efficiency & Timeframe
            Surface(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFF1F2637), RoundedCornerShape(14.dp)),
              color = Color(0xFF161B26)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = when (selectedRange) {
                    AnalyticsTimeRange.WEEK -> "7-DAY EFFICIENCY"
                    AnalyticsTimeRange.MONTH -> "30-DAY EFFICIENCY"
                    AnalyticsTimeRange.THREE_MONTHS -> "90-DAY EFFICIENCY"
                    AnalyticsTimeRange.DAY -> "SCHEDULE EFFICIENCY"
                  },
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextTertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                val effPct = ((totalScheduledDayMinutes.toFloat() / 1440f) * 100f).toInt()
                Text(
                  text = "$effPct% Active",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF10B981)
                )
              }
            }
          }
        }
      }
    }

    // ==============================================================
    // 3. PRESERVED INTER-TASK REST BUFFER CARD
    // ==============================================================
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(22.dp)),
      color = Color(0xFF121620)
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
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFF10B981).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Timelapse,
              contentDescription = null,
              tint = Color(0xFF10B981),
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text(
              text = "Inter-Task Rest Buffer",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "Gap between consecutive dynamic tasks",
              fontSize = 11.sp,
              color = TextSecondary
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        OutlinedTextField(
          value = restBufferInput,
          onValueChange = { newVal ->
            val clean = newVal.filter { it.isDigit() }.take(3)
            restBufferInput = clean
            val parsed = clean.toIntOrNull() ?: 0
            onUpdateBufferMinutes(parsed)
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          trailingIcon = {
            Text(
              text = "min",
              color = Color(0xFF10B981),
              fontWeight = FontWeight.SemiBold,
              fontSize = 12.sp,
              modifier = Modifier.padding(end = 8.dp)
            )
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = OneUiSurfaceHigh,
            unfocusedContainerColor = OneUiSurfaceHigh,
            focusedBorderColor = Color(0xFF10B981),
            unfocusedBorderColor = OneUiBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          ),
          textStyle = LocalTextStyle.current.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
          ),
          modifier = Modifier
            .width(105.dp)
            .testTag("settings_rest_buffer_input")
        )
      }
    }

    // ==============================================================
    // 4. PRESERVED TRASH BIN NAVIGATION CARD
    // ==============================================================
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(22.dp))
        .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(22.dp))
        .clickable { onOpenTrashBin() }
        .testTag("settings_trash_bin_card"),
      color = Color(0xFF121620)
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
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(DangerRed.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = null,
              tint = DangerRed,
              modifier = Modifier.size(22.dp)
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text(
              text = "Trash Bin & Recovery",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = if (deletedCount > 0) "$deletedCount deleted task(s) • Auto-purged after 30d" else "No deleted tasks • Auto-purges after 30d",
              fontSize = 11.sp,
              color = if (deletedCount > 0) WarningAmber else TextSecondary
            )
          }
        }

        Icon(
          imageVector = Icons.Default.ChevronRight,
          contentDescription = null,
          tint = TextSecondary
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // ==============================================================
    // 5. ROUTINE TASKS OVERVIEW (4-Item Task List)
    // ==============================================================
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("routine_tasks_overview_section")
    ) {
      Text(
        text = "Routine Tasks Overview",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = Color(0xFFA5B4FC),
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
      )

      // Horizontal Scrollable Category Filter Chips with Task Length Counts
      if (effectiveTasks.isNotEmpty()) {
        LazyRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .testTag("overview_category_filter_row"),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // 1. [ All (TotalCount) ] Chip
          item {
            val isAllSelected = selectedOverviewCategoryId == null || selectedOverviewCategoryId == "all"
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(
                  BorderStroke(
                    1.dp,
                    if (isAllSelected) Color(0xFF10B981) else Color(0xFF1F2637)
                  ),
                  RoundedCornerShape(12.dp)
                )
                .clickable { selectedOverviewCategoryId = "all" }
                .testTag("overview_chip_all"),
              color = if (isAllSelected) Color(0xFF10B981).copy(alpha = 0.18f) else Color(0xFF161B26),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text(
                text = "All (${effectiveTasks.size})",
                fontSize = 12.sp,
                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isAllSelected) Color(0xFF6EE7B7) else Color(0xFF94A3B8),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
              )
            }
          }

          // 2. Dynamic Category Chips present in the active schedule with Task Counts
          items(overviewCategoryChips, key = { it.id }) { catItem ->
            val isSelected = selectedOverviewCategoryId == catItem.id
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(
                  BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFF38BDF8) else Color(0xFF1F2637)
                  ),
                  RoundedCornerShape(12.dp)
                )
                .clickable { selectedOverviewCategoryId = catItem.id }
                .testTag("overview_chip_${catItem.id}"),
              color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.18f) else Color(0xFF161B26),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text(
                text = "${catItem.icon} ${catItem.name} (${catItem.count})",
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF7DD3FC) else Color(0xFF94A3B8),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
              )
            }
          }
        }
      }

      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(22.dp))
          .border(BorderStroke(1.dp, Color(0xFF1F2637)), RoundedCornerShape(22.dp))
          .testTag("routine_tasks_overview_card"),
        color = Color(0xFF121620)
      ) {
        if (effectiveTasks.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No active routine tasks scheduled for today",
              fontSize = 13.sp,
              color = TextTertiary
            )
          }
        } else if (filteredOverviewTasks.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No tasks found in this category.",
              fontSize = 13.sp,
              color = TextTertiary
            )
          }
        } else {
          Column(modifier = Modifier.fillMaxWidth()) {
            filteredOverviewTasks.forEachIndexed { index, task ->
              val cat = resolveTaskCategory(task)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("overview_task_row_${task.id}")
                  .padding(horizontal = 16.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                // Left & Center: 1. Start Time + 2. Task Name / Title + 3. Duration Badge
                Row(
                  modifier = Modifier.weight(1f),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  // 1. Start Time (Formatted 12h e.g. "09:00 AM", Cyan accent)
                  Text(
                    text = task.formattedStartTime,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF67E8F9),
                    modifier = Modifier.width(74.dp)
                  )

                  Spacer(modifier = Modifier.width(8.dp))

                  // 2. Task Name / Title + 3. Duration Badge
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = task.title,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFFF8FAFC),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = task.formattedDuration,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Medium,
                      color = Color(0xFF94A3B8)
                    )
                  }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 4. Category Badge (Compact One UI squircle pill with icon + name)
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = cat.color.copy(alpha = 0.15f),
                  border = BorderStroke(1.dp, cat.color.copy(alpha = 0.35f))
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(text = cat.icon, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = cat.name,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = cat.color,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }

              if (index < filteredOverviewTasks.lastIndex) {
                HorizontalDivider(
                  color = Color(0xFF1F2637),
                  thickness = 1.dp,
                  modifier = Modifier.padding(horizontal = 16.dp)
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }
}
