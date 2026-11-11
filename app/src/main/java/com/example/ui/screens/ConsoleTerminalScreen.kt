package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ConsoleParseResult
import com.example.engine.ConsoleParser
import com.example.model.RoutineTask
import com.example.ui.theme.DangerRed
import com.example.ui.theme.OneUiBorder
import com.example.ui.theme.OneUiSurfaceElevated
import com.example.ui.theme.OneUiSurfaceHigh
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConsoleTerminalScreen(
  anchors: List<RoutineTask>,
  dynamicTasks: List<RoutineTask> = emptyList(),
  sleepDurationMinutes: Int = 360,
  bufferMinutes: Int = 0,
  onApplyTasks: (tasks: List<RoutineTask>, replaceAll: Boolean, parsedAnchors: List<RoutineTask>) -> Unit,
  onBack: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val haptic = LocalHapticFeedback.current
  val clipboardManager = LocalClipboardManager.current
  BackHandler { onBack() }

  // Starts clean and empty with no preloaded dummy tasks
  var consoleTextFieldValue by remember {
    mutableStateOf(TextFieldValue(ConsoleParser.DEFAULT_CONSOLE_TEMPLATE))
  }
  val consoleText = consoleTextFieldValue.text

  // 1-Click Copy feedback state
  var showCopiedFeedback by remember { mutableStateOf(false) }
  LaunchedEffect(showCopiedFeedback) {
    if (showCopiedFeedback) {
      delay(2000)
      showCopiedFeedback = false
    }
  }

  // Debounced parsing (250ms) offloaded to Dispatchers.Default to eliminate typing latency
  var parseResult by remember {
    mutableStateOf(ConsoleParser.parse(consoleText))
  }
  LaunchedEffect(consoleText) {
    delay(250)
    val parsed = withContext(Dispatchers.Default) {
      ConsoleParser.parse(consoleText)
    }
    parseResult = parsed
  }

  // Error accordion collapsed/expanded state
  var isErrorAccordionExpanded by remember { mutableStateOf(true) }

  // Metrics derivation
  val totalTasksCount = parseResult.validTasks.size + parseResult.parsedAnchors.size
  val anchorsCount = parseResult.parsedAnchors.size
  val priorityCount = parseResult.validTasks.count { it.isHighPriority }
  val syntaxErrors = parseResult.errorCount
  val isSyntaxValid = syntaxErrors == 0 && totalTasksCount > 0

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0B0E14)) // Deep AMOLED obsidian background
      .statusBarsPadding()
      .verticalScroll(rememberScrollState())
      .padding(20.dp)
  ) {
    // Header Bar with Back Button & Interactive Backup Button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onBack()
          },
          modifier = Modifier.size(38.dp).testTag("terminal_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Timeline",
            tint = TextPrimary
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column {
          Text(
            text = "Bulk Terminal Console",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "Power-user pipe editor & schedule engine",
            fontSize = 11.sp,
            color = TextSecondary
          )
        }
      }

      // Interactive Header "Backup" Button
      Surface(
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          val template = ConsoleParser.generateRoutineScript(
            anchors = anchors,
            dynamicTasks = dynamicTasks,
            sleepMinutes = sleepDurationMinutes,
            bufferMinutes = bufferMinutes
          )
          consoleTextFieldValue = TextFieldValue(template, TextRange(template.length))
        },
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .testTag("terminal_header_backup_button")
          .testTag("quick_copy_template_button"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF080A0F),
        border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.6f))
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.ContentPaste,
            contentDescription = "Load Backup",
            tint = Color(0xFF06B6D4),
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = "Backup",
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF06B6D4),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // ==============================================================
    // 1. ACTIVE METRICS ROW: ONE UI DIAGNOSTICS CARD
    // ==============================================================
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(16.dp))
        .testTag("terminal_active_metrics_card"),
      color = OneUiSurfaceElevated
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Metric 1: Total Tasks
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
          Text(
            text = "$totalTasksCount",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF38BDF8)
          )
          Text(
            text = "Total Tasks",
            fontSize = 10.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
          )
        }

        Box(modifier = Modifier.width(1.dp).height(24.dp).background(OneUiBorder))

        // Metric 2: Anchors
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
          Text(
            text = "$anchorsCount",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFA78BFA)
          )
          Text(
            text = "Anchors",
            fontSize = 10.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
          )
        }

        Box(modifier = Modifier.width(1.dp).height(24.dp).background(OneUiBorder))

        // Metric 3: Priority
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
          Text(
            text = "$priorityCount",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFF59E0B)
          )
          Text(
            text = "Priority",
            fontSize = 10.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
          )
        }

        Box(modifier = Modifier.width(1.dp).height(24.dp).background(OneUiBorder))

        // Metric 4: Syntax Status
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
          Text(
            text = if (syntaxErrors > 0) "$syntaxErrors Err" else if (totalTasksCount > 0) "Valid" else "Empty",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (syntaxErrors > 0) DangerRed else if (totalTasksCount > 0) Color(0xFF10B981) else TextTertiary
          )
          Text(
            text = "Syntax",
            fontSize = 10.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // ==============================================================
    // 2. COMPACT CLI GRAMMAR BANNER (ANCHOR • TWIN • DYNAMIC)
    // ==============================================================
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .border(BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.35f)), RoundedCornerShape(14.dp))
        .testTag("compact_syntax_format_banner"),
      color = OneUiSurfaceElevated
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Code,
            contentDescription = null,
            tint = Color(0xFF06B6D4),
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "CLI GRAMMAR: ANCHOR | TWIN | DYNAMIC (v2)",
            color = Color(0xFF67E8F9),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "• ANCHOR | Slot(1-7) | Title | Start(HH:mm) | Dur(m) | DND(Y/N) | Delay(0-25) | Voice(Y/N) | Msg\n• TWIN | Pool(m) | P1Title | P1Start | P1Dur | P1Voice | P2Title | P2Start | P2Dur | P2Voice | DND | Days\n• DYNAMIC: Name | Dur(m) | Start(HH:mm) | DND(Y/N) | Days | Voice(Y/N) | Msg | Flex(0-30) | Rest(0-30) | Priority(Y/N)",
          color = Color(0xFF94A3B8),
          fontFamily = FontFamily.Monospace,
          fontSize = 10.sp,
          lineHeight = 15.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Action Bar: 1-Click Copy & Clear Buttons (Shown only when editor has text)
    if (consoleText.isNotBlank()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // 1-Click Copy Button
        OutlinedButton(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            clipboardManager.setText(AnnotatedString(consoleText))
            showCopiedFeedback = true
          },
          modifier = Modifier.testTag("copy_console_button"),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.5f)),
          colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF06B6D4).copy(alpha = 0.08f)
          )
        ) {
          Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy Script",
            modifier = Modifier.size(14.dp),
            tint = Color(0xFF06B6D4)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = if (showCopiedFeedback) "Copied! ✓" else "Copy",
            color = Color(0xFF06B6D4),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }

        // Clear Button
        OutlinedButton(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            consoleTextFieldValue = TextFieldValue("", TextRange.Zero)
          },
          modifier = Modifier.testTag("clear_console_button"),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, OneUiBorder)
        ) {
          Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = "Clear Console",
            modifier = Modifier.size(14.dp),
            tint = TextSecondary
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Clear", color = TextSecondary, fontSize = 11.sp)
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
    }

    // ==============================================================
    // 3. ULTRA-SPACIOUS OBSIDIAN CODE STUDIO (Clean Monospace Text)
    // ==============================================================
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(18.dp)),
      color = Color(0xFF121620)
    ) {
      Column(modifier = Modifier.padding(10.dp)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Obsidian Terminal Studio",
            color = Color(0xFF67E8F9),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "${consoleText.lines().count { it.isNotBlank() }} active line(s)",
            color = TextTertiary,
            fontSize = 11.sp
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
          // Compact Gutter (Line Numbers): snugly 24.dp width, 4.dp right margin, lineHeight = 20.sp
          val lineCount = consoleText.lines().size.coerceAtLeast(1)
          Column(
            modifier = Modifier
              .padding(top = 16.dp, end = 4.dp)
              .width(24.dp),
            horizontalAlignment = Alignment.End
          ) {
            for (i in 1..lineCount) {
              Text(
                text = "$i",
                color = Color(0xFF475569),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 20.sp
              )
            }
          }

          OutlinedTextField(
            value = consoleTextFieldValue,
            onValueChange = { consoleTextFieldValue = it },
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 540.dp)
              .testTag("console_text_input"),
            textStyle = TextStyle(
              fontFamily = FontFamily.Monospace,
              fontSize = 13.sp,
              lineHeight = 20.sp,
              color = Color(0xFFF8FAFC)
            ),
            placeholder = {
              Text(
                text = "# Enter dynamic routine tasks here...\n# Syntax: Name | Duration | Start | DND | Days | Voice | Message | Flex | Rest | Priority\n# Tap 'Backup' in header to load routine script.",
                color = TextTertiary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 20.sp
              )
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = Color(0xFF121620),
              unfocusedContainerColor = Color(0xFF121620),
              focusedBorderColor = Color.Transparent,
              unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // ==============================================================
    // 4. ERROR ACCORDION (Collapsible Diagnostic Panel)
    // ==============================================================
    if (syntaxErrors > 0) {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .border(BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)), RoundedCornerShape(14.dp))
          .testTag("syntax_error_accordion"),
        color = Color(0xFF1C1318)
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { isErrorAccordionExpanded = !isErrorAccordionExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = DangerRed,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "$syntaxErrors Syntax Error(s) Detected",
                color = DangerRed,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              )
            }
            Icon(
              imageVector = if (isErrorAccordionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
              contentDescription = if (isErrorAccordionExpanded) "Collapse" else "Expand",
              tint = DangerRed,
              modifier = Modifier.size(20.dp)
            )
          }

          AnimatedVisibility(visible = isErrorAccordionExpanded) {
            Column(
              modifier = Modifier.padding(top = 10.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              parseResult.diagnostics.filter { !it.isValid }.forEach { diag ->
                Surface(
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(8.dp),
                  color = Color.Black.copy(alpha = 0.3f),
                  border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.25f))
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                  ) {
                    Text(
                      text = "Line ${diag.lineNumber}: ",
                      color = DangerRed,
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp,
                      fontFamily = FontFamily.Monospace
                    )
                    Text(
                      text = diag.errorMessage ?: "Invalid syntax format",
                      color = Color(0xFFE2E8F0),
                      fontSize = 11.sp
                    )
                  }
                }
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }

    // ==============================================================
    // 5. DUAL SCHEDULE ACTION BUTTONS (Append & Replace)
    // ==============================================================
    val isProcessEnabled = isSyntaxValid

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // 1. [ Append to Schedule ] (Cyan/Indigo theme)
      Surface(
        modifier = Modifier
          .weight(1f)
          .height(54.dp)
          .clip(RoundedCornerShape(16.dp))
          .then(
            if (isProcessEnabled) {
              Modifier.clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onApplyTasks(parseResult.validTasks, false, parseResult.parsedAnchors)
              }
            } else Modifier
          )
          .testTag("append_to_schedule_button")
          .testTag("apply_console_tasks_button"),
        color = if (isProcessEnabled) Color.Transparent else OneUiSurfaceElevated,
        shape = RoundedCornerShape(16.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              if (isProcessEnabled) Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF6366F1)))
              else Brush.linearGradient(listOf(OneUiSurfaceElevated, OneUiSurfaceElevated))
            ),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              tint = if (isProcessEnabled) Color.White else TextTertiary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isProcessEnabled) "Append (${parseResult.successCount})" else "Append Tasks",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = if (isProcessEnabled) Color.White else TextTertiary
            )
          }
        }
      }

      // 2. [ Replace Dynamic Tasks ] (Warning Amber/Destructive tone)
      Surface(
        modifier = Modifier
          .weight(1f)
          .height(54.dp)
          .clip(RoundedCornerShape(16.dp))
          .then(
            if (isProcessEnabled) {
              Modifier.clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onApplyTasks(parseResult.validTasks, true, parseResult.parsedAnchors)
              }
            } else Modifier
          )
          .testTag("replace_dynamic_tasks_button"),
        color = if (isProcessEnabled) Color.Transparent else OneUiSurfaceElevated,
        shape = RoundedCornerShape(16.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(
              if (isProcessEnabled) Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFFEF4444)))
              else Brush.linearGradient(listOf(OneUiSurfaceElevated, OneUiSurfaceElevated))
            ),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = null,
              tint = if (isProcessEnabled) Color.White else TextTertiary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isProcessEnabled) "Replace (${parseResult.successCount})" else "Replace All",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp,
              color = if (isProcessEnabled) Color.White else TextTertiary
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(32.dp))
  }
}
