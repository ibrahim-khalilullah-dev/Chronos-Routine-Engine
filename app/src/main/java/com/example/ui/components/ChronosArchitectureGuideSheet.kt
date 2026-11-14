package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RoutineTask
import com.example.ui.theme.AnchorGradientStart
import com.example.ui.theme.ElasticGradientStart
import com.example.ui.theme.SleepGradientStart
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

/**
 * Editorial Newspaper Architectural Guide Sheet:
 * Broad-sheet journalistic aesthetic featuring traditional masthead, double-hairline rules,
 * authoritative editorial dispatches, real-time anchor slot tables, and typewriter codex.
 * Reflects the precise, 100% factual mathematical and telemetry architecture of Chronos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChronosArchitectureGuideSheet(
  anchors: List<RoutineTask> = emptyList(),
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = true
  )

  // Consume all nested scroll dispatch so vertical scrolling inside the guide never collapses or dismisses the sheet
  val consumeNestedScrollConnection = remember {
    object : NestedScrollConnection {
      override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
      override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
      override suspend fun onPreFling(available: Velocity): Velocity = Velocity.Zero
      override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    sheetGesturesEnabled = false, // CRITICAL: Disables accidental swipe-to-dismiss during vertical text reading
    dragHandle = null,             // Eliminates the gesture-consuming default drag handle
    containerColor = Color(0xFF080A0F), // True AMOLED Obsidian Black
    contentColor = TextPrimary,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
  ) {
    @OptIn(ExperimentalFoundationApi::class)
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
      Column(
        modifier = modifier
          .fillMaxWidth()
          .nestedScroll(consumeNestedScrollConnection)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 6.dp)
          .padding(bottom = 44.dp)
          .testTag("chronos_architecture_guide_sheet")
      ) {
        // Plain, static, non-draggable visual indicator pill
        Box(
          modifier = Modifier
            .padding(vertical = 12.dp)
            .size(width = 44.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.25f))
            .align(Alignment.CenterHorizontally)
        )

        // ==============================================================
        // 1. THE EDITORIAL MASTHEAD (Newspaper Banner Header)
        // ==============================================================
        HorizontalDivider(thickness = 2.dp, color = Color(0xFF3B82F6)) // Cobalt Blue Rule
        Spacer(modifier = Modifier.height(3.dp))
        HorizontalDivider(thickness = 1.dp, color = Color(0xFF1F2637)) // Hairline Inner Rule
        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(38.dp).testTag("dismiss_guide_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = TextPrimary
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "THE CHRONOS GAZETTE",
              fontSize = 22.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Serif,
              letterSpacing = 1.5.sp,
              color = Color(0xFFF8FAFC)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "VOL. VIII • OFFICIAL ARCHITECTURAL SPECIFICATION",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 0.5.sp,
              color = Color(0xFF94A3B8)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(thickness = 1.dp, color = Color(0xFF1F2637))
        Spacer(modifier = Modifier.height(3.dp))
        HorizontalDivider(thickness = 1.dp, color = Color(0xFF1F2637))

        Spacer(modifier = Modifier.height(18.dp))

        // ==============================================================
        // 2. FRONT PAGE HEADLINE & LEAD EDITORIAL
        // ==============================================================
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF3B82F6).copy(alpha = 0.15f))
            .border(BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "SYSTEM BLUEPRINT: DETERMINISTIC ENGINE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF60A5FA)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "THE SEVEN IMMUTABLE PILLARS OF CIRCADIAN EQUILIBRIUM",
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Serif,
          lineHeight = 26.sp,
          color = Color(0xFFF1F5F9)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "An authoritative engineering specification on zero-encroachment anchors, reverse sleep math, dynamic elastic buffers, hydraulic communicating vessels, delayed DND automation, and Samsung deep-sleep immunity.",
          fontSize = 12.sp,
          fontStyle = FontStyle.Italic,
          color = Color(0xFF94A3B8),
          lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(thickness = 1.dp, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(18.dp))

        // ==============================================================
        // 3. EDITORIAL DISPATCH I: THE 7 IMMUTABLE ANCHORS
        // ==============================================================
        EditorialArticleCard(
          dispatchNumber = "DISPATCH I",
          title = "The 7 Immutable Anchors",
          lead = "Zero-Encroachment Daily Circadian Foundations",
          icon = Icons.Default.Lock,
          accentColor = AnchorGradientStart
        ) {
          Text(
            text = "Chronos is structured upon seven immutable pillars that govern human circadian biology. Dynamic routine tasks will never overwrite, compress, or shift these hard boundaries under any scheduling operation.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
          )

          Spacer(modifier = Modifier.height(12.dp))

          val anchor1 = anchors.find { it.slotNumber == 1 }
          val anchor2 = anchors.find { it.slotNumber == 2 }
          val anchor3 = anchors.find { it.slotNumber == 3 }
          val anchor4 = anchors.find { it.slotNumber == 4 }
          val anchor5 = anchors.find { it.slotNumber == 5 }
          val anchor6 = anchors.find { it.slotNumber == 6 }
          val anchor7 = anchors.find { it.slotNumber == 7 }

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NewspaperAnchorRow(
              slot = 1,
              task = anchor1,
              defaultTitle = "Sleep",
              role = "11:00 PM (360m / 6h) • Reverse modulo math ((Slot 2 - Sleep + 1440) % 1440) derived backwards from Slot 2 wake time. DND active (0m delay).",
              accent = SleepGradientStart
            )
            NewspaperAnchorRow(
              slot = 2,
              task = anchor2,
              defaultTitle = "Fajr",
              role = "05:00 AM (35m) • Morning metabolic baseline & wakefulness anchor. 8m delayed DND timer. Voice: 'ফজরের সময় হয়েছে।'",
              accent = AnchorGradientStart
            )
            NewspaperAnchorRow(
              slot = 3,
              task = anchor3,
              defaultTitle = "Dhuhr",
              role = "01:15 PM (35m) • Midday focus boundary (8m DND delay). Features Friday Jumu'ah Dual Mode (12:30 PM, 120m, 5m DND delay).",
              accent = AnchorGradientStart
            )
            NewspaperAnchorRow(
              slot = 4,
              task = anchor4,
              defaultTitle = "Asr",
              role = "04:30 PM (25m) • Afternoon reset anchor defining the upper boundary of Slot 5 (10m DND delay). Voice: 'আসরের সময় হয়েছে।'",
              accent = AnchorGradientStart
            )
            NewspaperAnchorRow(
              slot = 5,
              task = anchor5,
              defaultTitle = "Break",
              role = "04:55 PM → 06:00 PM (65m) • Accordion rubber buffer spanning Slot 4 End to Slot 6 Start with a strict 15m safety floor and 7-day recurrence selector.",
              accent = ElasticGradientStart
            )
            NewspaperAnchorRow(
              slot = 6,
              task = anchor6,
              defaultTitle = "Maghrib",
              role = "06:00 PM (30m) • Evening decompression threshold marking transition to family time (5m DND delay). Voice: 'মাগরিবের সময় হয়েছে।'",
              accent = AnchorGradientStart
            )
            NewspaperAnchorRow(
              slot = 7,
              task = anchor7,
              defaultTitle = "Isha",
              role = "08:00 PM (35m) • Melatonin onset preparation and pre-sleep sanctuary (10m DND delay). Voice: 'এশার সময় হয়েছে।'",
              accent = AnchorGradientStart
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .border(BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)), RoundedCornerShape(10.dp)),
            color = Color(0xFF10B981).copy(alpha = 0.08f)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "🕌 Slot 3 Friday Jumu'ah Dual Mode",
                color = Color(0xFF34D399),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "On Fridays, Slot 3 seamlessly switches into dedicated Jumu'ah mode without altering Saturday–Thursday schedules:\n" +
                  "• Dedicated Time Window: Starts at 12:30 PM with an expanded duration of 2 hours (120m).\n" +
                  "• Autonomous Voice Annunciation: Custom speech ('জুমার সময় হয়েছে।') with audio ducking and a 5m DND delay timer.\n" +
                  "• Dynamic Non-Encroachment: Friday dynamic tasks automatically conform around the extended Jumu'ah window with zero manual rescheduling.",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                lineHeight = 15.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ==============================================================
        // 4. EDITORIAL DISPATCH II: SLOT 5 ACCORDION BUFFER
        // ==============================================================
        EditorialArticleCard(
          dispatchNumber = "DISPATCH II",
          title = "Slot 5: Accordion Elastic Buffer",
          lead = "Self-Balancing Mathematical Shock Absorber & Day Recurrence",
          icon = Icons.Default.Shield,
          accentColor = ElasticGradientStart
        ) {
          Text(
            text = "Slot 5 functions as an accordion buffer mathematically calibrated to bridge the exact duration between Slot 4 (Asr End) and Slot 6 (Maghrib Start):\n\n" +
              "• Dynamic Recalculation: When Slot 4 or Slot 6 moves, Slot 5 automatically stretches or contracts to absorb routine drift without shifting surrounding dynamic tasks.\n" +
              "• Protected 15-Minute Floor: Slot 5 can never be compressed below 15 minutes under routine expansion, guaranteeing physiological decompression time.\n" +
              "• Weekly Recurrence Selector: Slot 5 includes a granular 7-day selector (Saturday through Friday). When deactivated on any designated day, 100% of the Slot 4 → Slot 6 interval is fully liberated for high-leverage dynamic tasks with zero artificial buffers.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ==============================================================
        // 5. EDITORIAL DISPATCH III: FLUID TWINS
        // ==============================================================
        EditorialArticleCard(
          dispatchNumber = "DISPATCH III",
          title = "Fluid Twins: Communicating Vessels",
          lead = "Hydraulic Two-Session Duration Pooling",
          icon = Icons.AutoMirrored.Filled.CallSplit,
          accentColor = Color(0xFF8B5CF6)
        ) {
          Text(
            text = "Fluid Twins link two separate routine sessions (such as Morning Architecture and Afternoon Review) under a unified total duration pool:\n\n" +
              "• Communicating Vessels: Adding time to Session A proportionally contracts Session B while maintaining total pool integrity (P = D1 + D2), supporting pools from 2m up to 12h (including <45m short sprints).\n" +
              "• Per-Session Safety Floors: Each twin part supports an optional custom safety floor (fluidSafetyFloorMinutes, 0 = unconstrained down to 1m) while strictly respecting immovable anchor walls and per-part rest buffers.\n" +
              "• Surplus Flow: If one session is compressed by an anchor boundary, surplus minutes automatically flow to the partner twin.\n" +
              "• Autonomous Telemetry: Each twin session maintains independent voice toggles, speech messages, and volume sliders alongside shared alerts, category, flexibility, buffer, and DND rules.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ==============================================================
        // 6. EDITORIAL DISPATCH IV: HIGH PRIORITY FENCE & FLEXIBILITY
        // ==============================================================
        EditorialArticleCard(
          dispatchNumber = "DISPATCH IV",
          title = "High Priority Fence & Custom Flexibility",
          lead = "Anti-Jumping Anchor Wall Bounds & Rest Buffers",
          icon = Icons.Default.Security,
          accentColor = Color(0xFFEC4899)
        ) {
          Text(
            text = "Chronos introduces rigid compartmentalization rules to prevent timetable drift:\n\n" +
              "• High Priority Anchor Wall Fence: When marked High Priority (isHighPriority = true), a task is strictly confined to the inter-anchor gap enclosing its preferred start time. If space is insufficient, it moves to the Overflow Pool rather than jumping across anchor walls into unrelated time zones.\n" +
              "• Birth-Time Auto-Return: Displaced dynamic and Fluid Twin tasks preserve their original birth start time and automatically snap back to their preferred slot once blocking tasks are removed or paused.\n" +
              "• Custom Flexibility (0 to 30m): User-defined tolerance margin allows tasks to gently compress their duration to fit into tight circadian gaps without overflowing.\n" +
              "• Inter-Task Rest Buffer (0 to 30m, default 0m): Enforces an optional per-task cognitive cool-down buffer after scheduled tasks.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ==============================================================
        // 7. EDITORIAL DISPATCH V: TASK PAUSE & VACATION ENGINE
        // ==============================================================
        EditorialArticleCard(
          dispatchNumber = "DISPATCH V",
          title = "3-Mode Task Pause & Vacation Engine",
          lead = "Temporary Routine Suspension without Data Loss",
          icon = Icons.Default.PauseCircle,
          accentColor = Color(0xFFF59E0B)
        ) {
          Text(
            text = "In Settings, Chronos provides a non-destructive vacation and pause management engine for any task (including Slot 5):\n\n" +
              "• Pause for Today Only: Deactivates the task for the remainder of today; automatically resumes at midnight.\n" +
              "• Pause for N Days: Configurable day countdown (e.g. 5 days) that persists across app restarts and auto-reactivates on day N+1.\n" +
              "• Pause Indefinitely: Keeps the task suspended indefinitely until manually resumed via a single tap.\n" +
              "• Gap Liberation: Pausing any task opens up 100% of its scheduled interval as an available open gap for other tasks.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ==============================================================
        // 8. EDITORIAL DISPATCH VI: HARDWARE TELEMETRY & SLEEP IMMUNITY
        // ==============================================================
        EditorialArticleCard(
          dispatchNumber = "DISPATCH VI",
          title = "Hardware Telemetry & Deep Sleep Immunity",
          lead = "AlarmClock Engine, Delayed DND & Audio Ducking",
          icon = Icons.Default.Security,
          accentColor = Color(0xFF10B981)
        ) {
          Text(
            text = "Engineered specifically for complete immunity against aggressive OEM task killers (such as Samsung One UI Device Care deep sleep):\n\n" +
              "• AlarmManager.setAlarmClock: Start alarms and DND-enabled end alarms use system clock alarm architecture, scheduling the earliest upcoming non-overflow occurrence over a 7-day horizon.\n" +
              "• Task-Owned Priority DND & Overlap Guard: Tracks the active DND owner task ID synchronously, rejects delayed DND triggers for paused or deleted tasks, and skips teardown when a zero-delay follow-up DND task starts immediately at boundary.\n" +
              "• Smart Bengali/English TTS & Audio Ducking: Sound chimes, haptic vibration, and TTS voice briefings run independently with a cancellable 1500ms delay, dynamic bn-BD/en-US language switching, Google TTS fallback, and transient audio ducking while keeping app access protected behind your device lock.\n" +
              "• 4-Tier Offline Vault: Routines automatically persist to chronos_vault.json across external, public documents, and internal storage, enabling zero-click restoration on fresh reinstallation.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ==============================================================
        // 9. EDITORIAL DISPATCH VII: THE BULK TERMINAL CODEX
        // ==============================================================
        EditorialArticleCard(
          dispatchNumber = "DISPATCH VII",
          title = "The Monospace Pipe Syntax Codex",
          lead = "Power-User Command Line Grammar & Ghost Prediction",
          icon = Icons.Default.Code,
          accentColor = Color(0xFF06B6D4)
        ) {
          Text(
            text = "The Bulk Terminal Console accepts clean, pipe-separated directives for rapid bulk scheduling without repetitive UI clicks:",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
          )

          Spacer(modifier = Modifier.height(8.dp))

          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(10.dp)),
            color = Color(0xFF121620)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "1. ANCHOR | Slot(1-7) | Title | Start | Dur | DND | Delay | Voice | Msg | [FriOverrides] | [Days | Sound | VoiceVol]",
                color = Color(0xFF06B6D4),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "2. TWIN | Pool | P1Title | P1Start | P1Dur | P1Voice | P2Title | P2Start | P2Dur | P2Voice | DND | Days | Flex | Rest | [Category | P1Safety | P2Safety | P1VoiceYN | P2VoiceYN | P1Vol | P2Vol | Sound]",
                color = Color(0xFF38BDF8),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "3. DYNAMIC | Name | Dur | Start | DND | Days | Voice | Msg | Flex | Rest | Priority | Category | [VoiceVol | Sound]",
                color = Color(0xFF34D399),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Example:\nDeep Work | 90 | 09:30 | Y | Mon,Wed,Fri | Y | Focus session starting now | 15 | 0 | Y | work | 100 | Y",
                color = Color(0xFFE2E8F0),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                lineHeight = 14.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Ghost Autocomplete & Clean Backups: The terminal predicts upcoming syntax fields as you type, and one-tap Backup exports purely live ANCHOR, TWIN, and dynamic lines using each task's birth start time with zero comment clutter.",
            color = TextTertiary,
            fontSize = 11.sp,
            lineHeight = 15.sp
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ==============================================================
        // 10. EDITORIAL DISPATCH VIII: QUANTIFIED SELF & VISUAL ANALYTICS
        // ==============================================================
        EditorialArticleCard(
          dispatchNumber = "DISPATCH VIII",
          title = "Quantified Self & Visual Analytics",
          lead = "Deterministic Focus Metrics & Circadian Dial",
          icon = Icons.Default.Star,
          accentColor = Color(0xFFF59E0B)
        ) {
          Text(
            text = "Chronos incorporates a high-fidelity quantitative tracking engine to audit daily productivity, cognitive energy, and execution fidelity:",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
          )

          Spacer(modifier = Modifier.height(10.dp))

          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(10.dp)),
            color = Color(0xFF0F141E)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "⭐ Two-Step Focus Completion & Permanent 5-Star Badges",
                color = Color(0xFFFBBF24),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Upon task completion, Chronos prompts a 2-step evaluation from [0%] to [100%] in 25% increments (Poor, Fair, Good, Great, Masterful). Completed tasks lock in an immutable 5-Star rating badge (★☆☆☆☆ through ★★★★★) displayed right on the timeline card.",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                lineHeight = 15.sp
              )

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = "🍩 24-Hour Circular Donut Dial",
                color = Color(0xFF60A5FA),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "In Settings Analytics, the 24-Hour Donut Chart dynamically maps today's complete 1,440-minute circadian distribution: sleep (purple), fixed anchors (emerald), fluid twins (indigo), and dynamic focus tasks (cyan) with touch-to-inspect segment readouts.",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                lineHeight = 15.sp
              )

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = "📈 90-Day Historical Rating Engine (Zero Fake Data)",
                color = Color(0xFFA78BFA),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "The analytics engine calculates actual rolling metrics across Day, Week, Month, and 3-Month horizons strictly based on verified user ratings. Fresh installations present clean, uncluttered zero-state indicators without synthetic filler data.",
                color = Color(0xFFCBD5E1),
                fontSize = 11.sp,
                lineHeight = 15.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ==============================================================
        // 11. EDITORIAL COLOPHON (Footer)
        // ==============================================================
        HorizontalDivider(thickness = 1.dp, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "PRINTED & COMPILED OFFLINE FOR SAMSUNG GALAXY A24 (SM-A245N) • HELIO G99 ENGINE\nCHRONOS DETERMINISTIC TIME SYSTEM • 100% PRIVATE • ZERO ANALYTICS",
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace,
          color = Color(0xFF64748B),
          textAlign = TextAlign.Center,
          lineHeight = 13.sp,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

@Composable
private fun EditorialArticleCard(
  dispatchNumber: String,
  title: String,
  lead: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  content: @Composable () -> Unit
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(16.dp)),
    color = Color(0xFF121620)
  ) {
    Column(
      modifier = Modifier
        .animateContentSize()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = dispatchNumber,
          fontSize = 10.sp,
          fontWeight = FontWeight.Black,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.sp,
          color = accentColor
        )

        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(15.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = title,
        color = Color(0xFFF1F5F9),
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Serif
      )

      Text(
        text = lead,
        color = Color(0xFF94A3B8),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
      )

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(thickness = 1.dp, color = Color(0xFF1E293B))
      Spacer(modifier = Modifier.height(10.dp))

      content()
    }
  }
}

@Composable
private fun NewspaperAnchorRow(
  slot: Int,
  task: RoutineTask?,
  defaultTitle: String,
  role: String,
  accent: Color
) {
  val title = task?.title ?: defaultTitle
  val timeInfo = task?.let { "${it.formattedStartTime} → ${it.formattedEndTime} (${it.formattedDuration})" } ?: ""

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .border(BorderStroke(1.dp, Color(0xFF1E293B)), RoundedCornerShape(10.dp)),
    color = Color(0xFF0F141E)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(26.dp)
          .clip(CircleShape)
          .background(accent),
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

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Slot $slot: $title",
            color = Color(0xFFF8FAFC),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          if (timeInfo.isNotBlank()) {
            Text(
              text = timeInfo,
              color = Color(0xFFA5B4FC),
              fontFamily = FontFamily.Monospace,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = role,
          color = Color(0xFF94A3B8),
          fontSize = 10.sp,
          lineHeight = 14.sp
        )
      }
    }
  }
}
