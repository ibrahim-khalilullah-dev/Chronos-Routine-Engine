# CHRONOS ENGINE: COMPREHENSIVE ARCHITECTURAL CONTEXT MAP & BLUEPRINT

**Version:** 1.1 (`versionCode = 2`)  
**Platform Target:** Android 7.0+ (API Level 24–36)  
**Core Language:** Kotlin 2.2.10 (AGP 9.1.1, JVM Toolchain 11/21)  
**UI Toolkit:** Jetpack Compose (BOM 2024.09.00, Material Design 3 v1.4.0, Samsung One UI 8.5 Ergonomics)  
**Architecture Pattern:** Clean Architecture + MVVM (Unidirectional Data Flow) + Reactive StateFlow + Mutex Concurrency  
**Data Persistence:** SharedPreferences (Fast JSON serialization) + 400ms Debounced 4-Tier Offline Vault (Zero-Click Recovery)  
**Hardware & System APIs:** AlarmManager (Single-Earliest `setAlarmClock()` Promotion + `setExactAndAllowWhileIdle()` 7-day horizon), `goAsync()` BroadcastReceivers, NotificationManager (Silent Channel `chronos_automation_silent_channel` & Priority DND Filter), Lazy Self-Releasing `TextToSpeech` + `RingtoneManager` (`USAGE_ALARM` + `MediaPlayer` fallback), PowerManager (`25s` Partial WakeLock cap), VibratorManager  
**Privacy & Network:** 100% Offline, Zero Cloud Dependencies, Zero Analytics, No Hardcoded Secrets  

---

## 1. HIGH-LEVEL SYSTEM ARCHITECTURE & DATA FLOW

```
                        +----------------------------------------+
                        |           Android OS Events            |
                        | (Boot, Package Replaced, Time Change)  |
                        +-------------------+--------------------+
                                            |
                                            v
                                  [ BootReceiver.kt ]
                                  (goAsync() Coroutine Offload)
                                            |
                                            v
                        +----------------------------------------+
                        |        [ AutomationManager.kt ]        |
                        |   - Single-earliest setAlarmClock()    |
                        |   - setExactAndAllowWhileIdle() 7-day  |
                        |   - Task-owned DND + Stale Reconcile   |
                        |   - 12 Bilingual Voice Personas        |
                        +---------+--------------------+---------+
                                  |                    |
                                  v                    v
                      [ RoutineAlarmReceiver.kt ]      |
                      - goAsync() Alarm Promotion      |
                      - ACTION_START_TASK / END_TASK   |
                      - Stale-Guarded ACTION_APPLY_DND |
                                  |                    |
                                  v                    |
                 [ RoutineAutomationService.kt ]       |
                 - START_NOT_STICKY Foreground Service |
                 - Silent Notification Channel         |
                 - USAGE_ALARM Ringtone / MediaPlayer  |
                 - Lazy Self-Releasing TTS + 25s Wake  |
                                  |                    |
                                  v                    |
                 +----------------+----------------+   |
                 | ActiveAutomationState StateFlow |   |
                 +----------------+----------------+   |
                                  |                    |
                                  v                    v
+------------------------------------------------------------------------+
|                          PRESENTATION LAYER                            |
|                                                                        |
|  [ MainActivity.kt ]                                                   |
|    - Single-activity container, WindowInsets edge-to-edge              |
|    - Bottom Navigation + Floating Speed-Dial (Manual / Terminal / Doc) |
|    - Canonical task resolution & drag preview/commit/discard wiring    |
|                                                                        |
|  [ RoutineViewModel.kt ]                                              |
|    - Reactive StateFlow + Dispatchers.IO off-Main-Thread mutations     |
|    - Decoupled rawScheduleResult + distinctUntilChanged active sort    |
|    - Non-destructive _pendingDraftTasks + pendingImpactDiff StateFlows |
|    - Mutex-serialized drag scrub / vertical drop / Fluid Twin absorb   |
|    - Midnight date rollover monitor & PausedTodayOnly auto-reset       |
|                                                                        |
|  Screens:                                                              |
|    ├── TimelineScreen.kt        (Outer PointerEventPass.Initial drag,  |
|    │                             Source/Dest Shadows, Corner Gap Pill, |
|    │                             Header Countdown, 60/120 FPS glide)   |
|    ├── AutomationCenterScreen.kt(Hardware telemetry & diagnostics)     |
|    ├── SettingsScreen.kt        (24h Donut dial, Voice Persona, Pause) |
|    ├── ConsoleTerminalScreen.kt (540dp Obsidian Studio, metrics, CLI)  |
|    └── TrashBinScreen.kt        (Soft-delete bin & 30-day purge)       |
|                                                                        |
|  Sheets & Components:                                                  |
|    ├── TaskModalBottomSheet.kt  (Unified Obsidian Studio, Split, TTS)  |
|    ├── AnchorEditBottomSheet.kt (Circadian fine-tuner, voice switch)   |
|    ├── ChronosArchitectureGuideSheet.kt (VOL. IX Broadsheet Doc)       |
|    └── OneUiCards.kt            (Anchor/Dynamic/Overflow + Source Ghost)|
+-----------------------------------+------------------------------------+
                                    |
                                    v
+------------------------------------------------------------------------+
|                            DOMAIN ENGINE                               |
|                                                                        |
|  [ SchedulerEngine.kt ]                                                |
|    - 7 Immutable Anchors + Slot 1 Reverse Sleep + Friday Jumu'ah       |
|    - Slot 5 Elastic Buffer (15m floor, full alerts & 0-25m DND delay)  |
|    - High Priority & Fluid Twin Anchor Wall Fencing + Birth-Time Return|
|    - Net Free Gap math, Scrub Bounds, Vertical Drop & Impact Diff      |
|                                                                        |
|  [ ConsoleParser.kt ]                                                  |
|    - Pipe parser for ANCHOR, TWIN, and DYNAMIC (v2) directives         |
|    - Clean headerless birth-time backup script exporter                |
|                                                                        |
|  [ RoutineTask.kt ]                                                    |
|    - Domain models incl. originalStartTimeMinute & manualPlacementOrder|
+-----------------------------------+------------------------------------+
                                    |
                                    v
+------------------------------------------------------------------------+
|                             DATA LAYER                                 |
|                                                                        |
|  [ RoutineRepository.kt ]                                              |
|    - SharedPreferences (chronos_routine_prefs): fast JSON storage      |
|    - 400ms Debounced ioScope 4-Tier Offline Vault (chronos_vault.json) |
|    - Guarded one-time hasCheckedStartupVault restore on launch         |
|    - 90-Day Task Rating History & 30-Day Trash Bin auto-purge          |
+------------------------------------------------------------------------+
```

---

## 2. EXHAUSTIVE FILE-BY-FILE CONTEXT MAP

### Root & Build Configuration

#### `/.gitignore`
- **Purpose:** Specifies untracked files and build artifacts ignored by Git.
- **Key Specifications:** Contains `!chronos.apk` to ensure the release APK at the root directory is tracked for direct one-tap installation and raw CDN distribution.

#### `/chronos.apk`
- **Purpose:** Compiled v1.1 APK placed directly at the repository root for zero-click downloads (`versionCode = 2`, `versionName = "1.1"`).
- **Key Specifications:** Directly accessible via raw GitHub CDN (`https://raw.githubusercontent.com/ibrahim-khalilullah-dev/Chronos-Routine-Engine/main/chronos.apk`) and exempted from gitignore (`!chronos.apk`), enabling seamless in-place upgrades over v1.0.

#### `/build.gradle.kts`
- **Purpose:** Top-level project Gradle build script.
- **Key Contents:** Declares plugins applied without execution (`apply false`): Android Application, Kotlin Compose compiler, Google DevTools KSP, Secrets Gradle Plugin, Google Services.

#### `/settings.gradle.kts`
- **Purpose:** Gradle multi-project and plugin repository settings.
- **Key Contents:** Configures repositories (`google()`, `mavenCentral()`, `gradlePluginPortal()`), Foojay toolchain resolver, assigns project name `rootProject.name = "Chronos"`, and includes the `:app` module.

#### `/gradle.properties`
- **Purpose:** JVM options and global Gradle build flags.
- **Key Contents:** Configures Kotlin code style, JVM arguments (`-Xmx2048m`), AndroidX flags (`android.useAndroidX=true`), and non-transitive R classes.

#### `/gradle/libs.versions.toml`
- **Purpose:** Centralized Version Catalog managing all dependencies and plugins.
- **Key Elements:**
  - `versions`: AGP (9.1.1), Kotlin (2.2.10), Compose BOM (2024.09.00), Material3 (1.4.0), Room (2.7.0), Retrofit (2.12.0), Moshi (1.15.2), Robolectric (4.16.1).
  - `libraries`: Compose UI/Material3/Icons, Lifecycle, Room KTX/Compiler, Coroutines, OkHttp, Retrofit.
  - `plugins`: Android Application, Kotlin Compose, KSP, Secrets, Google Services.

#### `/.env.example`
- **Purpose:** Contract placeholder file required by the Secrets Gradle Plugin.
- **Key Contents:** Contains environment variable documentation. Kept clean to avoid generating malformed Java constants in `BuildConfig.java`.

#### `/metadata.json`
- **Purpose:** AI Studio platform metadata file.
- **Key Contents:** App display name (`Chronos`), description, capability flags (`MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`).

#### `/generate_icons.sh`
- **Purpose:** Shell script utilizing ImageMagick `convert` to produce multi-density squircle and round launcher icons (`mdpi` to `xxxhdpi`).

#### `/README.md`
- **Purpose:** Public-facing comprehensive project documentation, v1.1 feature manual, and direct APK download portal.

#### `/PROJECT.md` (This File)
- **Purpose:** Authoritative AI context map, architectural specification, and engineering blueprint.

---

### App Module Configuration

#### `/app/build.gradle.kts`
- **Purpose:** Module-level build script defining SDK targets, compile options, dependencies, and build types.
- **Key Specifications:**
  - `namespace`: `com.example`
  - `applicationId`: `com.aistudio.chronosroutine.mzyqpa`
  - `compileSdk`: 36 (minor API level 1)
  - `minSdk`: 24 (Android 7.0)
  - `targetSdk`: 36 (Android 16 Ready)
  - `versionCode`: 2 / `versionName`: `"1.1"`
  - `buildFeatures`: `compose = true`, `buildConfig = true`
  - `signingConfigs`: Configured with `debugConfig` using `${rootDir}/debug.keystore`.

#### `/app/proguard-rules.pro`
- **Purpose:** ProGuard and R8 shrinking/obfuscation rules for release compilation.

#### `/app/src/main/AndroidManifest.xml`
- **Purpose:** Core Android application manifest declaring permissions, activities, services, receivers, and hardware capabilities.
- **Key Declarations:**
  - Permissions & Queries: `POST_NOTIFICATIONS`, `ACCESS_NOTIFICATION_POLICY` (DND), `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `WAKE_LOCK`, `VIBRATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `RECEIVE_BOOT_COMPLETED`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, plus `<queries>` for `android.intent.action.TTS_SERVICE` enabling Android 11+ TTS engine & Bengali voice-pack discovery.
  - Application: `allowBackup="true"`, custom app icon and round icon, theme `@style/Theme.MyApplication`.
  - Activity: `MainActivity` with standard lock-screen security (no `showWhenLocked`/`turnScreenOn`) and `LAUNCHER` intent-filter; lockscreen notifications remain visible via `RoutineAutomationService` (`VISIBILITY_PUBLIC`).
  - Service: `RoutineAutomationService` with `foregroundServiceType="mediaPlayback"`.
  - Receivers: `RoutineAlarmReceiver` (actions `ACTION_START_TASK`, `ACTION_END_TASK`, `ACTION_APPLY_DND`), `BootReceiver` (boot, update, and timezone events).

---

### Core Data Models (`app/src/main/java/com/example/model/`)

#### `RoutineTask.kt`
- **Purpose:** Immutable data classes, enums, and utility functions defining tasks, anchors, and schedule entities.
- **Key Classes & Enums:**
  - `RoutineDay`: Days of the week (`SATURDAY` through `FRIDAY`), helper functions `fromString()`, `allDays()`, and `today()` mapping from `java.util.Calendar`.
  - `TaskType`: `ANCHOR_SLEEP` (Slot 1), `ANCHOR_STANDARD` (Slots 2, 3, 4, 6, 7), `ANCHOR_ELASTIC` (Slot 5), `DYNAMIC` (user-created tasks).
  - `RoutineTask`: Complete data model representing scheduled and unscheduled routine tasks.
    * Core fields: `id`, `title`, `type`, `slotNumber`, `startTimeMinute` (0..1439), `originalStartTimeMinute` (nullable birth time preserved across displacements), `durationMinutes`, `selectedDays`, `isDeleted`, `deletedTimestamp`.
    * Telemetry & Voice: `voiceMessage`, `enableDnd`, `dndDelayMinutes` (0..25m), `enableHaptic`, `isVoiceEnabled`, `voiceVolume`, `isSoundAlertEnabled`, `customRingtoneUri`, `customNotificationTitle`, `customNotificationBody`.
    * Fluid Twins: `isFluidTwin`, `fluidTwinId`, `fluidTwinPart` (1 or 2), `fluidTotalPoolMinutes`, `fluidSafetyFloorMinutes`.
    * Flexibility, Priority & Drag Scheduling: `allowSplitting`, `isHighPriority`, `customFlexibilityMinutes` (0..30m), `toleranceMinutes`, `interTaskBufferMinutes` (0..30m), `manualPlacementOrder` (`Long` monotonic tie-breaker updated on manual drag placement).
    * Friday Slot 3 Overrides: `fridayTitle`, `fridayStartTimeMinute`, `fridayDurationMinutes`, `fridayVoiceMessage`.
    * Vacation & Pause: `isPausedTodayOnly`, `pausedUntilDateMillis`, `isPausedIndefinitely`.
    * Computed properties: `endTimeMinute`, `formattedStartTime`, `formattedEndTime`, `formattedDuration`, `notificationDisplayTitle`, `notificationDisplayBody`, `daysRemainingInTrash`, `isCurrentlyPaused()`.

---

### Data & Persistence Layer (`app/src/main/java/com/example/data/`)

#### `RoutineRepository.kt`
- **Purpose:** Central data repository managing active routines, persistence in `SharedPreferences`, and debounced multi-tier offline disk backups.
- **Key Responsibilities:**
  - Fast Memory-Mapped Storage: Stores JSON arrays for anchors (`key_anchors_json`), dynamic tasks (`key_dynamic_tasks_json`), ratings (`key_task_rating_history_json`), voice persona (`key_voice_preset_id`), and preferences in private `SharedPreferences`.
  - 400ms Debounced 4-Tier Offline Vault (`chronos_vault.json`): Uses a serialized single-thread `ioScope` (`Dispatchers.IO.limitedParallelism(1)`) and `VAULT_DEBOUNCE_MS = 400L` (`persistVaultAsync()`) so rapid successive saves coalesce into a single background write across:
    1. App external files directory (`context.getExternalFilesDir(null)/ChronosBackup/`).
    2. Shared public Documents directory (`/storage/emulated/0/Documents/ChronosBackup/`).
    3. Fallback app external documents directory (`context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)/ChronosBackup/`).
    4. Private internal storage (`context.filesDir/ChronosBackup/`).
  - Guarded One-Time Auto-Restore (`hasCheckedStartupVault` / `checkAndAutoRestoreVault()`): Scans once per process launch when preferences are empty; avoids redundant disk reads on subsequent repository instances.
  - Soft-Delete Management: `softDeleteTask()`, `restoreTask()`, `purgeTaskPermanently()`, `purgeAllDeletedTasks()`, and auto-purging tasks older than 30 days (`PURGE_THRESHOLD_MILLIS`).
  - Quantified Self Retention: `recordTaskRating()` and `getTaskRatingHistory()` managing 90-day rating records with rolling expiration.

---

### Scheduling & Parsing Engines (`app/src/main/java/com/example/engine/`)

#### `SchedulerEngine.kt`
- **Purpose:** Pure mathematical scheduling engine calculating gaps, placing dynamic tasks, managing fluid twins, computing interactive drag bounds/drops, and enforcing circadian anchor invariants.
- **Key Invariants:**
  - `fluidSafetyFloorMinutes` (per-session): Optional minimum duration floor (`0`..session duration, 1m minimum for non-zero sessions), supporting `<45m` Fluid Twins down to 2m pools while respecting per-part rest buffers.
  - `MIN_ELASTIC_BUFFER_MINUTES = 15`: Safety floor for the Slot 5 elastic buffer.
  - **Zero Auto-Splitting Architecture:** Tasks are never fragmented or split automatically into chunks. If a dynamic task cannot fit into available circadian gaps (taking into account user flexibility tolerance), it is routed intact to the Overflow Pool.
- **Key Functions & Types:**
  - `createDefaultAnchors(sleepDurationMinutes)` / `calculateAnchors()` / `adjustAnchor()`: Manages the 7 anchors (`11:00 PM` Sleep, `05:00 AM` Fajr 35m/8m, `01:15 PM` Dhuhr 35m/8m + Fri Jumu'ah `12:30 PM` 120m, `04:30 PM` Asr 25m/10m, `04:55 PM` Break 65m, `06:00 PM` Maghrib 30m/5m, `08:00 PM` Isha 35m/10m).
  - `findAvailableGaps(committedTasks, interTaskBufferMinutes)`: Computes unallocated 24-hour time slots accounting for per-task rest buffers without bleeding across anchor boundaries.
  - `scheduleDay(anchors, dynamicTasks, activeDay, interTaskBufferMinutes)`: Places anchors, Friday Jumu'ah overrides, Fluid Twins, and standard dynamic tasks sorted by `isHighPriority DESC`, `manualPlacementOrder DESC`, and `originalStartTimeMinute ASC` (preserving Birth-Time Auto-Return across schedule shifts).
  - `computeNetFreeGapMinutes(task, nextTask)`: Computes exact net open minutes between two consecutive scheduled tasks after subtracting the earlier task's `interTaskBufferMinutes`.
  - `findEnclosingAnchorBounds()` & `computeHorizontalScrubBounds()`: Calculates exact non-overlapping horizontal scrub `[minStart..maxStart]` limits inside enclosing anchor walls and adjacent dynamic blocks.
  - `applyDragReposition()`, `applyVerticalDropReposition()`, `previewVerticalDrop()`, & `VerticalDropTarget` (`InsertBeforeTask`, `InsertAfterTask`, `IntoGap`): Handles high-priority/twin anchor wall fencing (`hitAnchorWall`), cross-anchor placement, wide-gap insertion before/after targets, adjacent contiguous swaps/cascades, and monotonic `manualPlacementOrder` priority assignment.
  - `computeScheduleImpactDiff(baseline, draft)` -> `List<ScheduleImpactItem>`: Computes human-readable diff summaries for the Schedule Impact Preview dialog before committing pending drag edits.

#### `ConsoleParser.kt`
- **Purpose:** Grammatical bulk CLI interpreter converting plaintext monospace pipe scripts into strongly typed `RoutineTask` collections.
- **Directives Supported:**
  - `ANCHOR | Slot(1-7) | Title | Start(HH:mm) | Dur(m) | DND(Y/N) | DndDelay(0-25) | Voice(Y/N) | Message | [FriOverrides] | [Days | Sound | VoiceVol]` (canonical `"anchor_slot_$slot"` IDs).
  - `TWIN | TotalPool(m) | Part1Title | Part1Start | Part1Dur | Part1Voice | Part2Title | Part2Start | Part2Dur | Part2Voice | DND(Y/N) | Days | Flex | Rest | [Category | P1Safety | P2Safety | P1VoiceYN | P2VoiceYN | P1Vol | P2Vol | Sound | DndDelay]`.
  - `DYNAMIC | Name | Duration(m) | Start(HH:mm) | DND(Y/N) | Days | Voice(Y/N) | Message | Flex | Rest | Priority | Category | [VoiceVol | Sound | DndDelay]` (and prefix-free equivalent).
- **Clean Headerless Backup Export (`generateRoutineScript()`):** Outputs purely live `ANCHOR`, `TWIN`, and `DYNAMIC` directives with zero `#` comment headers or blank separator lines, exporting `originalStartTimeMinute ?: startTimeMinute` and `dndDelayMinutes` (`0–25m`, forced `0` when DND is off) while `parse()` retains backward-compatible handling for legacy `# SLEEP_MINUTES=` / `# BUFFER_MINUTES=` headers and populates `originalStartTimeMinute` on parsed tasks.
- **Pre-Packaged Routine (`SAMPLE_SNIPPET`):** Complete Bengali circadian schedule featuring the 7 immutable circadian prayer anchors, Saturday–Thursday focus routines, and Friday special routines.
- **Diagnostics:** Emits `ConsoleLineDiagnostic` with precise line numbers and syntax validation messages for immediate visual feedback.

---

### Hardware Telemetry & Services (`app/src/main/java/com/example/service/` & `receiver/`)

#### `AutomationManager.kt`
- **Purpose:** System-level automation coordinator for alarms, DND policy, audio telemetry, and power management.
- **Key Capabilities:**
  - **Single-Earliest `setAlarmClock()` Promotion + Exact Horizon:** Evaluates active tasks across `dayOffset in -1..6` inside `rescheduleAllAlarmsAt()`, collects all `PlannedAlarmItem` entries, and promotes **only the single earliest upcoming alarm** (`earliestAlarmClockMillis` across Start, bounded Delayed DND, and DND-End) to `AlarmManager.setAlarmClock()`, registering all subsequent future triggers via `setExactAndAllowWhileIdle()` to prevent SystemUI alarm broadcast storms while still guaranteeing midnight-crossing accuracy and Doze immunity.
  - **Slot 3 Friday Lockscreen Fidelity:** `buildTaskAlarmsForOccurrence()` applies Friday title, duration (`fridayDurationMinutes`), voice message, and accurate Friday end time while honoring the user's configured `dndDelayMinutes` (`0–25m`).
  - `cancelTaskAlarms(context, taskId)` / `pendingIntent.cancel()`: Cancels pending start, end, and DND alarms cleanly when tasks end or are rescheduled.
  - `calculateNextOccurrenceMillis(task, activeDay)`: Calculates upcoming task occurrence timestamps across up to 7 days matching `task.selectedDays`.
  - `setDndMode()` / `reconcileStaleDndState()` / `releaseDndIfHeldByTask()` / `hasZeroDelayFollowUpDndTask()`: Persists active DND owner ID synchronously (`commit()`), reconciles stale DND owners across `dayOffset = -1..0`, and skips teardown when a zero-delay follow-up DND task starts immediately at boundary.
  - `VOICE_PRESETS` & 12 Bilingual Voice Personas: 12 offline bilingual BN/EN personas (`female`, `youth`, `male`) with pitch/speechRate profiles, `nativeMalePitch` compensation, `containsBengali()`, and `"com.google.android.tts"` fallback discovery.

#### `RoutineAutomationService.kt`
- **Purpose:** `START_NOT_STICKY` foreground service (`mediaPlayback`) providing silent-channel lockscreen notifications, `USAGE_ALARM` ringtones, haptic pulses, lazy self-releasing TTS, and task-owned DND transitions.
- **Key Implementations:**
  - **Lazy Self-Releasing TTS & 25s WakeLock Cap:** Does NOT allocate `TextToSpeech` in `onCreate()`. Instantiates `TextToSpeech(applicationContext, this)` strictly when `isVoiceEnabled == true` via `triggerLazyTtsSpeech()`, then immediately executes `releaseTtsEngine()` (`stop()`, `shutdown()`, `tts = null`, abandon audio focus, release WakeLock) in `UtteranceProgressListener.onDone` / `onError`, when `!isVoiceEnabled`, or on a `20_000L` (`TTS_SAFETY_TIMEOUT_MS`) handler watchdog—ensuring `com.google.android.tts` RAM unbinds immediately. WakeLocks are capped at `WAKELOCK_MAX_TIMEOUT_MS = 25_000L` and null-intent restarts immediately release and `stopSelf()`.
  - **Silent Channel & `USAGE_ALARM` Ringtone Playback:** Creates `chronos_automation_silent_channel` with `setSound(null, null)` and `enableVibration(false)` (`VISIBILITY_PUBLIC`), preventing OS double-chimes while `playTaskRingtone()` plays custom user ringtones directly via `RingtoneManager` on `AudioAttributes.USAGE_ALARM` with automatic `MediaPlayer` fallback.
  - **Decoupled Alerts & Audio Ducking:** Haptic vibration, ringtone chimes, and delayed TTS (`1500ms` cancellable delay when sound alert is enabled) execute independently with `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`.

#### `RoutineAlarmReceiver.kt`
- **Purpose:** `BroadcastReceiver` invoked by `AlarmManager` for `ACTION_START_TASK`, `ACTION_END_TASK`, and `ACTION_APPLY_DND`.
- **Action:** Starts `RoutineAutomationService` on task start and offloads `AutomationManager.rescheduleAllAlarms()` (as well as `handleApplyDnd()` and `handleEndTask()`) to `CoroutineScope(Dispatchers.Default)` inside `goAsync()` (`PendingResult.finish()`), guarding `ACTION_APPLY_DND` against expired `EXTRA_TASK_END_MILLIS` and guarding DND teardown against owner mismatch or zero-delay follow-ups.

#### `BootReceiver.kt`
- **Purpose:** System event receiver ensuring schedule persistence across device state transitions.
- **Listened Intents:** `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `QUICKBOOT_POWERON`, `TIME_SET`, `TIMEZONE_CHANGED`.
- **Action:** Uses `goAsync()` + `CoroutineScope(Dispatchers.Default)` to invoke `AutomationManager.rescheduleAllAlarms(appContext)` safely off the main thread.

---

### UI Layer (`app/src/main/java/com/example/ui/`)

#### `MainActivity.kt`
- **Purpose:** Root Android `ComponentActivity` configuring edge-to-edge window insets, runtime notification permission requests, the One UI bottom navigation shell, Floating Speed-Dial menu (`Manual Form`, `Terminal`, `Documentation`), canonical task resolution (for all 7 anchors and `originalStartTimeMinute`), and non-destructive timeline drag callbacks (`previewDragTaskMove`, `previewVerticalDropMove`, `commitPendingEdits`, `discardPendingEdits`).
- **Destinations Managed:** `TIMELINE`, `CONSOLE`, `AUTOMATION`, `SETTINGS`, `TRASH`.

#### `RoutineViewModel.kt`
- **Purpose:** Core ViewModel orchestrating UI state, concurrent task scheduling, non-destructive drag previews, and user interactions.
- **Key Implementations:**
  - **Off-Main-Thread Mutex Mutations & `distinctUntilChanged` Pipeline:** Wraps mutations under `scheduleMutex` with repository/alarm persistence executing on `Dispatchers.IO`; `rawScheduleResult` and `scheduleResult` evaluate on `Dispatchers.Default` with `distinctUntilChanged()`, and `activeSortMinuteTicker` emits clock updates only when `TimelineSortMode.ACTIVE_FIRST` is active.
  - **Interactive Drag Preview & Impact Diff State:** Holds `_pendingDraftTasks`, `_baselineSnapshotSchedule`, `hasPendingEdits`, and derived `pendingImpactDiff`; exposes `previewDragTaskMove()`, `previewVerticalDropMove()`, `commitPendingEdits()`, and `discardPendingEdits()` alongside monotonic `nextManualPlacementOrder`.
  - **Midnight Rollover Monitor & Full Slot 5 Hardware Updates:** Autonomous date observer in `viewModelScope` switches `activeDay` at 00:00 and clears `isPausedTodayOnly`; `updateSlot5Protected()` persists Slot 5 ringtone URI/title, sound alert, voice toggle/volume, haptic, and `0–25m` DND delay.
  - **Fluid Twin Persistence & Absorption Engine:** Coordinates `saveFluidTwin()`, `deleteBothFluidTwinParts()`, and `deleteTwinAndAbsorbTime()`.

#### `OneUiCards.kt` (`ui/components/`)
- **Purpose:** Visual timeline card components conforming to Samsung One UI 8.5 guidelines (`#0B0E14` AMOLED obsidian palette, `#0A0F1D` surfaces, `#1E2C45` borders).
- **Drag Visuals & Stability:** Avoids `animateContentSize()` on card roots; supports `isDragging` glow emphasis and `isDragSourceGhost` on `DynamicTaskCard` to render the dimmed Source Shadow (`alpha = 0.28f`) + `drawWithCache` dashed `CyanAccent` border at the origin index during vertical reordering.

#### `TaskModalBottomSheet.kt` (`ui/components/`)
- **Purpose:** Full-featured creation and editing bottom sheet for Dynamic Tasks, Fluid Twin pairs, and all 7 Circadian Anchors (using `applicationContext` `TextToSpeech` for zero-leak previews).
- **Unified Obsidian Glass Architecture & Clean Anchors:** Normal Tasks, Split Twins (`DualSessionCard`), and all 7 Anchors share the `#101727` → `#0A0E18` obsidian glass gradient, recessed `#070B13` wells, universal `0–25m` Auto DND Delay across all modes (including Slot 1 & Slot 5), Ringtone Preview (`MediaPlayer`), and interactive `UtteranceProgressListener` TTS briefing player synced to `activeVoicePresetId`.

#### `AnchorEditBottomSheet.kt` (`ui/components/`)
- **Purpose:** Dedicated modal editor for fine-tuning circadian anchor properties and hardware automations (using `applicationContext` `TextToSpeech` with zero leak on dismiss).

#### `ChronosArchitectureGuideSheet.kt` (`ui/components/`)
- **Purpose:** `"THE CHRONOS GAZETTE"` VOL. IX (`v1.1`) in-app newspaper broadsheet documentation displaying live anchor status + Dispatches I through X (including Launcher-Grade Drag Scheduling and Minimalist Countdown & Net Free-Time Gap Telemetry).

#### `TimelineScreen.kt` (`ui/screens/`)
- **Purpose:** Primary application screen displaying the circadian timeline, minimalist header countdown, corner gap pills, and launcher-grade drag-and-drop editing.
- **Minimalist Header Countdown Pill (`OneUiHeader`):** Replaces the deprecated lower `LiveCountdownTimerBanner`; displays a single top-right pill opposite `"Chronos"` (wrapping across midnight to tomorrow's earliest task late at night) and swaps seamlessly to the 3-button confirmation bar (`pending_impact_history_button`, `pending_commit_button`, `pending_discard_button`) + `schedule_impact_dialog` when edits are staged.
- **Corner Stopwatch Free-Time Gap Pills:** Under `EARLIEST_START`, renders right-aligned obsidian capsules (`#161E2E`, `Icons.Outlined.Timer`, `testTag = "free_gap_indicator_${task.id}"`) displaying `SchedulerEngine.computeNetFreeGapMinutes()`, hidden when `<= 0m` and updating frame-by-frame during horizontal scrubbing.
- **Outer-Container Launcher Drag (`PointerEventPass.Initial`):** Tracks horizontal speed-sensitive scrubbing (locked when free room is `0m`) and vertical reordering with `withFrameNanos` edge auto-scroll, elevated `floating_drag_card_overlay`, dimmed Source Shadow + full-card-height animated `DestinationDropShadowSlot` (`±8dp` anti-ping-pong hysteresis), `Modifier.animateItem` placement animations, `drag_live_time_overlay`, `anchor_wall_toast`, and zero-blink settling guard (`isSettlingVerticalDrop`).
- **Crash-Proof Recycling:** Uses stable `key = { _, task -> task.id }` in `LazyColumn` and keyed `derivedStateOf` jump pills (`jump_to_top_button`, `jump_to_active_button`).

#### `ConsoleTerminalScreen.kt` (`ui/screens/`)
- **Purpose:** Power-user monospace CLI bulk routine editor (using headerless birth-time exports for live backup sync).

#### `SettingsScreen.kt` (`ui/screens/`)
- **Features:** 24-Hour Circadian Donut Dial, collapsible **Voice Persona Studio** dropdown (`voice_dropdown_trigger`, `applicationContext` `TextToSpeech` previews with `com.google.android.tts` fallback, custom BN/EN test field `voice_persona_test_input`, 12 voice profiles, and `Confirm Voice Persona?` `AlertDialog`), 3-Mode Task Pause & Vacation Engine, and Quantified Self 90-day history.

#### `AutomationCenterScreen.kt` (`ui/screens/`)
- **Features:** Hardware telemetry dashboard, DND permission status, battery optimization whitelist helper, and TTS voice preview.

#### `TrashBinScreen.kt` (`ui/screens/`)
- **Features:** Displays deleted tasks with a 30-day countdown timer, individual restore/purge actions, and an empty trash confirmation dialog.

#### `Color.kt`, `Theme.kt`, `Type.kt` (`ui/theme/`)
- **Purpose:** Centralized Material Design 3 and Samsung One UI 8.5 theme definition with AMOLED Obsidian palette (`#0B0E14`).

---

## 3. CORE MATHEMATICAL FORMULAS & INVARIANTS

### 1. Reverse Sleep Math (Slot 1)
$$\text{Bedtime Minute} = \left(\left(\text{Slot 2 Wake Minute} - \text{Sleep Duration Minutes} + 1440\right) \pmod{1440}\right)$$
*Example:* Wake up at 05:40 AM (340m) with 360m (6h) sleep duration:
$$\text{Bedtime} = (340 - 360 + 1440) \pmod{1440} = 1420\text{m} \implies \text{11:40 PM}$$

### 2. Slot 5 Elastic Rubber Buffer
$$\text{Slot 5 Start} = \text{Slot 4 Start} + \text{Slot 4 Duration}$$
$$\text{Slot 5 Duration} = \max\left(\text{Slot 6 Start} - \text{Slot 5 Start}, 15\right)$$

### 3. Communicating Vessels Fluid Twin Balancing & Absorption
For linked twin sessions $S_1$ and $S_2$ with total pool $P$ and per-session safety floors $F_1, F_2 \ge 0$:
$$D_1 + D_2 = P, \quad D_1 \ge \max(1, F_1), \quad D_2 \ge \max(1, F_2)$$
When deleting one twin part via absorption:
$$D_{\text{surviving}} = P, \quad \text{isFluidTwin} = \text{false}, \quad \text{fluidTwinId} = \text{null}, \quad \text{fluidSafetyFloorMinutes} = 0$$

### 4. High Priority Anchor Wall Fence
For any task where `isHighPriority = true`, its scheduled interval $[t_{\text{start}}, t_{\text{end}}]$ must satisfy:
$$A_{k,\text{end}} + B \le t_{\text{start}} < t_{\text{end}} \le A_{k+1,\text{start}} - B$$
where $A_k$ and $A_{k+1}$ are the adjacent anchors bounding the task's requested start time, and $B$ is the inter-task rest buffer.

### 5. Net Free-Time Gap Telemetry (Buffer-Excluded)
For consecutive scheduled tasks $T_i$ and $T_{i+1}$ in `EARLIEST_START` order:
$$B_i = \begin{cases} \operatorname{clamp}(T_i.\text{interTaskBufferMinutes}, 0, 30) & \text{if } T_i.\text{type} = \text{DYNAMIC} \\ 0 & \text{otherwise} \end{cases}$$
$$E_{\text{eff}}(T_i) = T_i.\text{startTimeMinute} + T_i.\text{durationMinutes} + B_i$$
$$\text{NetGap}(T_i, T_{i+1}) = \max\left(0, \; T_{i+1}.\text{startTimeMinute} - E_{\text{eff}}(T_i)\right)$$
Gap pills render exclusively when $\text{NetGap} > 0$.

### 6. Horizontal Scrub Bounds & Drag Priority Invariant
For dragged dynamic task $T$ inside enclosing anchor bounds $[A_{\text{start}}, A_{\text{end}}]$ and adjacent free gap $[G_{\text{start}}, G_{\text{end}}]$ excluding anchors and other dynamic tasks' duration + buffer blocks:
$$\text{ScrubMin} = \max(G_{\text{start}}, A_{\text{start}}), \quad \text{ScrubMax} = \max\left(\text{ScrubMin}, \; \min(G_{\text{end}}, A_{\text{end}}) - (\text{dur}_T + B_T)\right)$$
Manual drag placement updates both `startTimeMinute` and `originalStartTimeMinute` to the confirmed target time and assigns a monotonically increasing `manualPlacementOrder` so displaced non-dragged tasks yield cleanly while retaining their own birth-time auto-return targets.

---

## 4. PERSISTENCE & OFFLINE VAULT PROTOCOL

1. **Active Storage:** Android `SharedPreferences` (`chronos_routine_prefs`) stores serialized JSON arrays.
2. **Multi-Tier File Vault:** Every mutation asynchronously updates `chronos_vault.json` across 4 storage locations to guarantee survivability against app updates and device uninstalls.
3. **Zero-Click Reinstallation Recovery:** On fresh app launch, `checkAndAutoRestoreVault()` inspects these locations. If preferences are uninitialized, the vault is deserialized and restored synchronously before UI rendering begins.
4. **Data Retention Policies:**
   - Deleted Tasks: Retained in the Trash Bin for 30 days before permanent auto-purge.
   - Task Ratings: Retained in Quantified Self history for 90 days before rolling expiration.

---

## 5. SUMMARY OF DEPENDENCY & ENVIRONMENT CONSTRAINTS

- **No Remote Backend Required:** Operates 100% locally on device.
- **Audio Ducking:** Implemented via Android `AudioManager` and `TextToSpeech` (no external audio libraries required).
- **Alarms:** `AlarmManager.setAlarmClock()` and exact RTC `ACTION_APPLY_DND` alarms require `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`.
- **DND Automation:** Requires user to grant Notification Policy Access (`ACCESS_NOTIFICATION_POLICY`).
- **Secrets:** Managed via Secrets Gradle Plugin through `.env.example`. No API keys are required for core operations.
