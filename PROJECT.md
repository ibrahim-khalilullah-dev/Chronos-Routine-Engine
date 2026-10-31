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
│   │   └── ConsoleParser.kt             -> Pipe syntax parser, real-time ghost text autocomplete generator
│   ├── service/
│   │   ├── AutomationManager.kt         -> Exact alarms (setAlarmClock), DND policy, TTS configuration, haptics
│   │   └── RoutineAutomationService.kt  -> MediaPlayback foreground service, transient audio ducking, lock-screen speech
│   ├── receiver/
│   │   ├── RoutineAlarmReceiver.kt      -> Exact alarm broadcast receiver (starts/ends routine services)
│   │   └── BootReceiver.kt              -> Reschedules all alarms on reboot, update, and timezone change
│   └── ui/
│       ├── RoutineViewModel.kt          -> Reactive state flows, Mutex concurrency, timeline sort persistence
│       ├── components/
│       │   ├── OneUiCards.kt            -> AnchorSlotCard, DynamicTaskCard, TaskCompletionRatingBar, live progress
│       │   ├── TaskModalBottomSheet.kt  -> Routine Studio form, 13 categories, dual-session twin auto-balancing
│       │   ├── AnchorEditBottomSheet.kt -> Precision anchor editor (Slot 1 reverse sleep, Slot 5 recurrence)
│       │   └── ChronosArchitectureGuideSheet.kt -> In-app editorial newspaper specification broadsheet
│       ├── screens/
│       │   ├── TimelineScreen.kt        -> Main timeline, Real Today vs Inspected Day duality, floating jump pills
│       │   ├── SettingsScreen.kt        -> 24h Donut & Progress bars, Rest buffer input & confirmation, 3-mode pause engine
│       │   ├── AutomationCenterScreen.kt-> DND status, Battery optimization whitelist helper, Exact alarm readiness
│       │   ├── ConsoleTerminalScreen.kt -> Monospace Obsidian bulk editor with ghost auto-prediction
│       │   └── TrashBinScreen.kt        -> Soft delete recovery & 30-day auto-purge engine
│       └── theme/                       -> Color tokens (#080A0F, #121620, #1F2637), Brushes, Typography

---

## 2. CORE ENGINES & MATHEMATICAL INVARIANTS

1. The 7 Immutable Anchors:
   - Slot 1 (Reverse Sleep): Bedtime auto-calculated backwards: ((Slot 2 Wake Minute - Sleep Duration + 1440) % 1440). Sleep duration default: 420m (7h).
   - Slots 2, 3, 4, 6, 7: Fixed anchors with 30-minute standard duration (Slot 2: 05:00 AM, Slot 3: 01:15 PM, Slot 4: 04:30 PM, Slot 6: 06:00 PM, Slot 7: 08:00 PM).
   - Slot 3 Friday Jumu'ah Dual Mode: On Fridays, automatically evaluates dedicated start time (11:30 AM), duration (150m), and custom voice message.
   - Slot 5 (Elastic Rubber Buffer): Bridges Slot 4 End (05:00 PM) to Slot 6 Start (06:00 PM). Minimum 15m safety floor. Includes 7-day weekly recurrence selector; when deactivated, liberates 100% of the interval for dynamic tasks.

2. Dynamic Task Scheduling Engine:
   - Dynamic tasks wrap around anchors.
   - Tasks > 60m with splitting enabled break across gaps with a strict >= 15m safety chunk limit.
   - Unplaceable tasks are isolated in the Overflow pool.
   - Inter-Task Rest Buffer: Clamped to 0..30m; enforced after every task (anchors and dynamic).

3. Fluid Twin Tasks (Communicating Vessels):
   - 2 linked sessions sharing a unified duration pool (totalPool = S1 + S2).
   - Increasing S1 dynamically shrinks S2 while respecting a 45m safety floor per session.
   - Immovable anchor walls are impenetrable boundaries.

4. Task Pause & Vacation Engine:
   - Tasks (including Slot 5 and standard anchors; excluding Slot 1) support 3 pause modes:
     1. Pause for Today Only (auto-resumes tomorrow at 00:00).
     2. Pause for N Days (persists timestamp, auto-resumes on day N+1).
     3. Pause Indefinitely (manual resumption).
   - Pausing any task liberates 100% of its time gap for dynamic tasks.

5. Hardware Telemetry & Samsung Deep Sleep Immunity:
   - Uses AlarmManager.setAlarmClock() with AlarmClockInfo, ensuring alarms are never killed by Samsung One UI Device Care deep sleep.
   - DND managed via NotificationManager.INTERRUPTION_FILTER_PRIORITY.
   - Audio focus ducking via AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK in RoutineAutomationService.

6. Offline Vault & Zero-Click Auto-Restore:
   - Multi-tier backup saved on every commit to Documents/ChronosBackup/chronos_vault.json and internal storage.
   - On fresh reinstall, checkAndAutoRestoreVault() auto-detects the vault and restores all routines without user intervention.

7. Quantified Self & 90-Day History:
   - Two-step 5-star rating system ([0%] to [100%]).
   - TaskRatingRecord persisted permanently for 90 days across restarts and task deletions.
   - Interactive 24-hour circular Donut clock and expandable progress bar accordions.

---

## 3. AUTOMATED MAINTENANCE PROTOCOL ("ALL DONE / অল ডান")
When the user indicates a work session is finished (e.g. "All Done", "অল ডান", "সবকিছু হয়ে গেছে", "ওকে", "কাজ শেষ"):
1. Recompile and update root chronos.apk.
2. Sync README.md with any new user-facing features or installation updates.
3. Sync PROJECT.md with any modified files, schema changes, or logic additions.
4. Run tests (compile_applet & testDebugUnitTest) and confirm green status.
