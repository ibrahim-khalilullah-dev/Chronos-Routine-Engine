# CHRONOS ENGINE: AI & DEVELOPER ARCHITECTURAL SPECIFICATION

Target Platform: Android 8.0+ (API 26-36) | Language: Kotlin 2.0.21 | UI: Jetpack Compose (M3 1.4.0, One UI 8.5)
Design Philosophy: Samsung Galaxy One UI AMOLED Dark (#080A0F), 60/40 thumb ergonomics, 100% offline & zero analytics.

---

## 1. FOLDER & COMPONENT MAP

app/src/main/
├── AndroidManifest.xml                  -> Permissions, Services, Receivers, Auto-backup configs
├── java/com/example/
│   ├── MainActivity.kt                  -> Navigation host, bottom bar, startup vault auto-restore
│   ├── model/
│   │   └── RoutineTask.kt               -> Data models: RoutineTask, TaskType, RoutineDay, TaskRatingRecord
│   ├── data/
│   │   └── RoutineRepository.kt         -> SharedPreferences persistence, 90-day rating history, offline vault
│   ├── engine/
│   │   ├── SchedulerEngine.kt           -> Gap calculation, auto-splitting, fluid twin pooling, pause handling
│   │   └── ConsoleParser.kt             -> Multi-directive parser (ANCHOR, TWIN, DYNAMIC), ghost autocomplete
│   ├── service/
│   │   ├── AutomationManager.kt         -> Exact alarms (setAlarmClock), DND policy & delays, TTS, haptics
│   │   └── RoutineAutomationService.kt  -> MediaPlayback foreground service, delayed DND Handler, lock-screen speech
│   ├── receiver/
│   │   ├── RoutineAlarmReceiver.kt      -> Exact alarm broadcast receiver (starts/ends routine services)
│   │   └── BootReceiver.kt              -> Reschedules all alarms on reboot, update, and timezone change
│   └── ui/
│       ├── RoutineViewModel.kt          -> Reactive state flows, Mutex concurrency, timeline sort persistence
│       ├── components/
│       │   ├── OneUiCards.kt            -> AnchorSlotCard, DynamicTaskCard, TaskCompletionRatingBar, live progress
│       │   ├── TaskModalBottomSheet.kt  -> Routine Studio form, 13 categories, twin balancing, anchor delayed DND
│       │   ├── AnchorEditBottomSheet.kt -> Precision anchor editor (Slot 1 reverse sleep, Slot 5 recurrence)
│       │   └── ChronosArchitectureGuideSheet.kt -> In-app editorial newspaper specification broadsheet
│       ├── screens/
│       │   ├── TimelineScreen.kt        -> Main timeline, Real Today vs Inspected Day duality, floating jump pills
│       │   ├── SettingsScreen.kt        -> 24h Donut & Progress bars, Rest buffer input & confirmation, 3-mode pause engine
│       │   ├── AutomationCenterScreen.kt-> DND status, Battery optimization whitelist helper, Exact alarm readiness
│       │   ├── ConsoleTerminalScreen.kt -> Monospace Obsidian bulk editor with ghost auto-prediction & CLI grammar
│       │   └── TrashBinScreen.kt        -> Soft delete recovery & 30-day auto-purge engine
│       └── theme/                       -> Color tokens (#080A0F, #121620, #1F2637), Brushes, Typography

---

## 2. CORE ENGINES & MATHEMATICAL INVARIANTS

1. The 7 Immutable Anchors & Delayed DND:
   - Slot 1 (Reverse Sleep): Bedtime auto-calculated backwards: ((Slot 2 Wake Minute - Sleep Duration + 1440) % 1440). Sleep duration default: 420m (7h).
   - Slots 2, 3, 4, 6, 7: Fixed anchors with 30-minute standard duration (Slot 2: 05:00 AM, Slot 3: 01:15 PM, Slot 4: 04:30 PM, Slot 6: 06:00 PM, Slot 7: 08:00 PM).
   - 0-25m Delayed DND: For Anchor Slots 2, 3, 4, 6, 7, when DND is enabled, users can specify a delay timer (0 to 25 mins). 0 = immediate DND upon task start; 1-25 = Handler delay before DND engages. Slots 1 and 5 remain immediate (delay clamped to 0).
   - Slot 3 Friday Jumu'ah Dual Mode: On Fridays, automatically evaluates dedicated start time (11:30 AM), duration (150m), and custom voice message.
   - Slot 5 (Elastic Rubber Buffer): Bridges Slot 4 End (05:00 PM) to Slot 6 Start (06:00 PM). Minimum 15m safety floor. Includes 7-day weekly recurrence selector; when deactivated, liberates 100% of the interval for dynamic tasks.

2. Supercharged Bulk Terminal CLI Engine:
   - Directive 1: `ANCHOR | Slot(1-7) | Title | Start(HH:mm) | Duration(m) | DND(Y/N) | DndDelay(0-25) | Voice(Y/N) | Message` (Optional Friday overrides for Slot 3: `| FriTitle | FriStart | FriDuration | FriMessage`).
   - Directive 2: `TWIN | TotalPool(m) | Part1Title | Part1Start(HH:mm) | Part1Dur(m) | Part1Voice | Part2Title | Part2Start(HH:mm) | Part2Dur(m) | Part2Voice | DND(Y/N) | Days | Flexibility | RestBuffer`.
   - Directive 3: `DYNAMIC | Name | Duration(m) | Start(HH:mm) | DND(Y/N) | Days | Voice(Y/N) | Message | Flexibility(0/15/30) | RestBuffer(0-30)` (or standard pipe syntax without prefix).
   - Context-aware inline ghost prediction: Dynamically suggests completions based on typing directive (`ANCHOR`, `TWIN`, or dynamic tasks).

3. Dynamic Task Scheduling Engine & High Priority Fence:
   - Dynamic tasks wrap around anchors.
   - High Priority Anchor Wall Fence (`isHighPriority = true`): Strictly confines task to the inter-anchor gap enclosing its preferred start time (`startTimeMinute`). If it cannot fit inside this gap, it is immediately directed to the Overflow pool rather than jumping across anchor walls ("timetable jumping").
   - Custom Flexibility (`customFlexibilityMinutes` 0..30m): Fine-tunes duration compression allowance within available gap space.
   - Task Splitting: Default disabled (`allowSplitting = false`). Tasks > 60m with splitting explicitly enabled break across gaps with a strict >= 15m safety chunk limit.
   - Inter-Task Rest Buffer: Clamped to 0..30m; enforced after every task (anchors and dynamic).
   - Alarm Recurrence Precision (`calculateNextOccurrenceMillis`): Iterates 0..7 day offsets to find the exact next matching day in `selectedDays`, preventing firing on inactive days and synchronizing end alarms to `startMillis + durationMillis`. Ghost DND leaks are actively eliminated when transitioning to non-DND tasks.

4. Fluid Twin Tasks (Communicating Vessels):
   - 2 linked sessions sharing a unified duration pool (totalPool = S1 + S2).
   - Increasing S1 dynamically shrinks S2 while respecting a 45m safety floor per session.
   - Immovable anchor walls are impenetrable boundaries.

5. Task Pause & Vacation Engine:
   - Tasks (including Slot 5 and standard anchors; excluding Slot 1) support 3 pause modes:
     1. Pause for Today Only (auto-resumes tomorrow at 00:00).
     2. Pause for N Days (persists timestamp, auto-resumes on day N+1).
     3. Pause Indefinitely (manual resumption).
   - Pausing any task liberates 100% of its time gap for dynamic tasks.

6. Hardware Telemetry & Samsung Deep Sleep Immunity:
   - Uses AlarmManager.setAlarmClock() with AlarmClockInfo, ensuring alarms are never killed by Samsung One UI Device Care deep sleep.
   - DND managed via NotificationManager.INTERRUPTION_FILTER_PRIORITY with optional Handler delay timers.
   - Audio focus ducking via AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK in RoutineAutomationService.

7. Offline Vault & Zero-Click Auto-Restore:
   - Multi-tier backup saved on every commit to Documents/ChronosBackup/chronos_vault.json and internal storage.
   - On fresh reinstall, checkAndAutoRestoreVault() auto-detects the vault and restores all routines without user intervention.

8. Quantified Self & 90-Day History:
   - Two-step 5-star rating system ([0%] to [100%]).
   - TaskRatingRecord persisted permanently for 90 days across restarts and task deletions.
   - Interactive 24-hour circular Donut clock and expandable progress bar accordions.

---

## 3. AUTOMATED MAINTENANCE PROTOCOL ("ALL DONE / অল ডান")
When the user indicates a work session is finished (e.g. "All Done", "অল ডান", "সবকিছু হয়ে গেছে", "ওকে", "কাজ শেষ"):
1. Recompile and update root chronos.apk via gradle assembleDebug.
2. Sync README.md with any new user-facing features or installation updates.
3. Sync PROJECT.md with any modified files, schema changes, or logic additions.
4. Run tests (compile_applet & testDebugUnitTest) and confirm green status.
