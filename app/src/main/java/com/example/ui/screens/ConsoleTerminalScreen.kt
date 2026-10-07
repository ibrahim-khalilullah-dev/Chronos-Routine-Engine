package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ConsoleParser
import com.example.model.RoutineTask
import com.example.model.TaskType
import com.example.ui.theme.AnchorGradientStart
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DynamicGradientEnd
import com.example.ui.theme.ElasticGradientStart
import com.example.ui.theme.OneUiBorder
import com.example.ui.theme.OneUiSurfaceElevated
import com.example.ui.theme.OneUiSurfaceHigh
import com.example.ui.theme.SleepGradientStart
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConsoleTerminalScreen(
  anchors: List<RoutineTask>,
  onApplyTasks: (tasks: List<RoutineTask>, replaceAll: Boolean) -> Unit,
  onExportRoutine: () -> String = { "" },
  onBack: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  BackHandler { onBack() }

  // Starts clean and empty with no preloaded dummy tasks
  var consoleText by remember { mutableStateOf(ConsoleParser.DEFAULT_CONSOLE_TEMPLATE) }
  var replaceExisting by remember { mutableStateOf(false) }
  var isAnchorsPanelExpanded by remember { mutableStateOf(true) }
  var isGuideExpanded by remember { mutableStateOf(false) }

  // Real-time parsed syntax analysis
  val parseResult = remember(consoleText) {
    ConsoleParser.parse(consoleText)
  }

  // Cyan-to-Emerald gradient for primary action button
  val processButtonGradient = Brush.horizontalGradient(
    listOf(Color(0xFF06B6D4), Color(0xFF10B981))
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0A0D14)) // Deep AMOLED obsidian background
      .statusBarsPadding()
      .verticalScroll(rememberScrollState())
      .padding(20.dp)
  ) {
    // Header Bar with Back Button
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

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF080A0F))
          .border(BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.4f)), RoundedCornerShape(12.dp))
          .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Text(
          text = ">_ TERMINAL",
          fontFamily = FontFamily.Monospace,
          color = Color(0xFF06B6D4),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // ==============================================================
    // 2. 7 PROTECTED ANCHORS OVERVIEW PANEL (Prominent & Expandable)
    // ==============================================================
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .border(BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f)), RoundedCornerShape(20.dp))
        .clickable { isAnchorsPanelExpanded = !isAnchorsPanelExpanded }
        .testTag("protected_anchors_overview_card"),
      color = OneUiSurfaceElevated
    ) {
      Column(
        modifier = Modifier
          .animateContentSize()
          .padding(16.dp)
      ) {
        // Panel Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xFFA5B4FC),
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Protected Anchors Overview",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "7 Core Pillars • Mathematically locked",
                color = TextSecondary,
                fontSize = 11.sp
              )
            }
          }

          Icon(
            imageVector = if (isAnchorsPanelExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
          )
        }

        // Expanded Anchors Content
        AnimatedVisibility(visible = isAnchorsPanelExpanded) {
          Column(modifier = Modifier.padding(top = 12.dp)) {
            // Immutability Notice Banner
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
              color = WarningAmber.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.35f))
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
              ) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = null,
                  tint = WarningAmber,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Protected Anchors Notice: These 7 core slots are mathematically locked and active on your timeline. They cannot be created, modified, or deleted via the terminal. Dynamic tasks entered below will automatically schedule around these slots.",
                  color = TextPrimary,
                  fontSize = 11.sp,
                  lineHeight = 16.sp
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7 Anchors Timeline Grid
            val sortedAnchors = anchors.sortedBy { it.slotNumber ?: 0 }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              sortedAnchors.forEach { anchor ->
                val slot = anchor.slotNumber ?: 0
                val slotAccent = when (anchor.type) {
                  TaskType.ANCHOR_SLEEP -> SleepGradientStart
                  TaskType.ANCHOR_ELASTIC -> ElasticGradientStart
                  else -> AnchorGradientStart
                }

                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                  color = OneUiSurfaceHigh
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.weight(1f)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(24.dp)
                          .clip(CircleShape)
                          .background(slotAccent),
                        contentAlignment = Alignment.Center
                      ) {
                        Text(
                          text = if (slot == 1) "🌙" else if (slot == 5) "🛡️" else "$slot",
                          color = Color.White,
                          fontSize = 10.sp,
                          fontWeight = FontWeight.Bold
                        )
                      }
                      Spacer(modifier = Modifier.width(10.dp))
                      Column {
                        Text(
                          text = anchor.title,
                          color = TextPrimary,
                          fontSize = 13.sp,
                          fontWeight = FontWeight.SemiBold
                        )
                        Text(
                          text = when (slot) {
                            1 -> "Reverse Sleep Tracker (Bedtime to Wake)"
                            5 -> "Rubber Elastic Buffer (Auto-Scaled)"
                            else -> "Anchor Pillar Slot $slot"
                          },
                          color = TextTertiary,
                          fontSize = 10.sp
                        )
                      }
                    }

                    // Time Interval
                    Box(
                      modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                      Text(
                        text = "${anchor.formattedStartTime} → ${anchor.formattedEndTime} (${anchor.formattedDuration})",
                        color = Color(0xFFA5B4FC),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // ==============================================================
    // 3. ADVANCED IN-APP DOCUMENTATION & TEMPLATES (Accordion)
    // ==============================================================
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .border(BorderStroke(1.dp, OneUiBorder), RoundedCornerShape(18.dp))
        .clickable { isGuideExpanded = !isGuideExpanded }
        .testTag("interactive_syntax_guide_card"),
      color = OneUiSurfaceElevated
    ) {
      Column(
        modifier = Modifier
          .animateContentSize()
          .padding(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.HelpOutline,
              contentDescription = null,
              tint = Color(0xFF06B6D4),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Syntax Guide & Scheduling Rules",
              color = TextPrimary,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Icon(
            imageVector = if (isGuideExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Format: Name | Duration(m) | Start(HH:mm) | DND(Y/N) | Days | Voice(Y/N) | Message",
          color = Color(0xFF06B6D4),
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold
        )

        AnimatedVisibility(visible = isGuideExpanded) {
          Column(modifier = Modifier.padding(top = 10.dp)) {
            Text(
              text = "Parameter Standards:",
              color = TextPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            SyntaxFieldDesc("1. Name", "Task title, e.g., 'Client Review', 'Deep Coding'")
            SyntaxFieldDesc("2. Duration", "Duration in minutes, e.g. 45, 90")
            SyntaxFieldDesc("3. Start Time", "Preferred time: 24-hr (14:30) or 12-hr (02:30 PM)")
            SyntaxFieldDesc("4. DND Mode", "Y (engage priority Do Not Disturb) or N (normal)")
            SyntaxFieldDesc("5. Days", "ALL, WEEKDAYS, WEEKENDS, or comma-separated: Mon,Wed,Fri")
            SyntaxFieldDesc("6. Voice (TTS)", "Y (read aloud over lock screen) or N (silent popup banner)")
            SyntaxFieldDesc("7. Custom Text", "Message displayed in popup and announced by TTS")

            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Mathematical Engine Rules:",
              color = Color(0xFF10B981),
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp
            )
            Text(
              text = "• Auto-Splitting: Tasks > 60m are dynamically split across open gaps with a strict 15m minimum chunk threshold.\n" +
                "• Buffer Guard: An automatic buffer is preserved before/after each scheduled task.\n" +
                "• Immutability: Dynamic tasks will NEVER override any of the 7 core anchors.",
              color = Color(0xFFCBD5E1),
              fontSize = 11.sp,
              lineHeight = 16.sp
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Quick Action Bar: "Quick-Copy Template", "Export / Copy", & "Clear"
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedButton(
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          consoleText = ConsoleParser.SAMPLE_SNIPPET
        },
        modifier = Modifier.weight(1f).testTag("quick_copy_template_button"),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.6f))
      ) {
        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF06B6D4))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Template", color = Color(0xFF06B6D4), fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }

      OutlinedButton(
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          val script = onExportRoutine()
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
          val clip = ClipData.newPlainText("Chronos Routine", script)
          clipboard?.setPrimaryClip(clip)
          Toast.makeText(context, "Dynamic routine copied! (Anchors excluded)", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.weight(1.2f).testTag("export_routine_button"),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f))
      ) {
        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF10B981))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Export Routine", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }

      if (consoleText.isNotBlank()) {
        OutlinedButton(
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            consoleText = ""
          },
          modifier = Modifier.testTag("clear_console_button"),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, OneUiBorder)
        ) {
          Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(15.dp), tint = TextSecondary)
          Spacer(modifier = Modifier.width(4.dp))
          Text("Clear", color = TextSecondary, fontSize = 11.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // ==============================================================
    // 4. ULTRA-CLEAN OBSIDIAN CODE EDITOR (#121620 with Gutter Numbers)
    // ==============================================================
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(18.dp)),
      color = Color(0xFF121620)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Obsidian Terminal Editor",
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

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
          // Gutter Line Numbers
          val lineCount = consoleText.lines().size.coerceAtLeast(1)
          Column(
            modifier = Modifier
              .padding(top = 16.dp, end = 10.dp)
              .width(24.dp),
            horizontalAlignment = Alignment.End
          ) {
            for (i in 1..lineCount.coerceAtMost(40)) {
              Text(
                text = "$i",
                color = Color(0xFF475569),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
              )
            }
          }

          OutlinedTextField(
            value = consoleText,
            onValueChange = { consoleText = it },
            modifier = Modifier
              .weight(1f)
              .height(240.dp)
              .testTag("console_text_input"),
            textStyle = TextStyle(
              fontFamily = FontFamily.Monospace,
              fontSize = 13.sp,
              lineHeight = 18.sp,
              color = Color(0xFFF8FAFC)
            ),
            placeholder = {
              Text(
                text = "# Enter dynamic routine tasks here...\n# Syntax: Name | Duration | Start | DND | Days | Voice | Custom Message\n# Tap 'Template' above to load a ready-to-edit template.",
                color = TextTertiary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
              )
            },
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = Color(0xFF121620),
              unfocusedContainerColor = Color(0xFF121620),
              focusedBorderColor = Color(0xFF06B6D4),
              unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Real-Time Syntax Validation Card
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .border(
          BorderStroke(
            1.dp,
            if (parseResult.errorCount > 0) DangerRed.copy(alpha = 0.4f) else OneUiBorder
          ),
          RoundedCornerShape(16.dp)
        ),
      color = OneUiSurfaceElevated
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = if (parseResult.errorCount == 0) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            tint = if (parseResult.errorCount == 0) Color(0xFF10B981) else DangerRed,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (parseResult.errorCount == 0 && parseResult.successCount > 0) {
              "Syntax Valid (${parseResult.successCount} dynamic tasks ready to schedule)"
            } else if (parseResult.errorCount > 0) {
              "${parseResult.errorCount} syntax error(s) detected"
            } else {
              "Awaiting task entries..."
            },
            color = if (parseResult.errorCount == 0) TextPrimary else DangerRed,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
          )
        }

        Text(
          text = "${parseResult.diagnostics.size} total lines",
          color = TextSecondary,
          fontSize = 11.sp
        )
      }
    }

    // Line-by-Line Syntax Error Suggestions
    if (parseResult.errorCount > 0) {
      Spacer(modifier = Modifier.height(10.dp))
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .border(BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)), RoundedCornerShape(14.dp))
          .background(Color.Black.copy(alpha = 0.25f))
          .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        parseResult.diagnostics.filter { !it.isValid }.forEach { diag ->
          Row(verticalAlignment = Alignment.Top) {
            Text(
              text = "Line ${diag.lineNumber}: ",
              color = DangerRed,
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = diag.errorMessage ?: "Syntax error",
              color = TextSecondary,
              fontSize = 11.sp
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Application Mode Toggle: Append vs Replace
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      FilterChip(
        selected = !replaceExisting,
        onClick = { replaceExisting = false },
        label = { Text("Append to Schedule") },
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
        selected = replaceExisting,
        onClick = { replaceExisting = true },
        label = { Text("Replace Dynamic Tasks") },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = AnchorGradientStart,
          selectedLabelColor = Color.White,
          containerColor = OneUiSurfaceElevated,
          labelColor = TextSecondary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Process & Schedule Action Button (Vibrant Cyan-to-Emerald Gradient)
    val isProcessEnabled = parseResult.validTasks.isNotEmpty() && parseResult.errorCount == 0

    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .clip(RoundedCornerShape(16.dp))
        .then(
          if (isProcessEnabled) {
            Modifier.clickable {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onApplyTasks(parseResult.validTasks, replaceExisting)
            }
          } else {
            Modifier
          }
        )
        .testTag("apply_console_tasks_button"),
      color = if (isProcessEnabled) Color.Transparent else OneUiSurfaceElevated,
      shape = RoundedCornerShape(16.dp)
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(if (isProcessEnabled) processButtonGradient else Brush.linearGradient(listOf(OneUiSurfaceElevated, OneUiSurfaceElevated))),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = if (isProcessEnabled) Color.White else TextTertiary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isProcessEnabled) {
              "Process & Schedule (${parseResult.successCount} Tasks)"
            } else {
              "Process & Schedule"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = if (isProcessEnabled) Color.White else TextTertiary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(32.dp))
  }
}

@Composable
private fun SyntaxFieldDesc(title: String, desc: String) {
  Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
    Text(
      text = "$title: ",
      color = TextPrimary,
      fontWeight = FontWeight.Bold,
      fontSize = 11.sp,
      fontFamily = FontFamily.Monospace
    )
    Text(
      text = desc,
      color = TextSecondary,
      fontSize = 11.sp
    )
  }
}
