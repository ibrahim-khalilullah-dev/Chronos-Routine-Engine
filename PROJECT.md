# CHRONOS ENGINE: COMPREHENSIVE ARCHITECTURAL CONTEXT MAP & BLUEPRINT

**Version:** 1.5 (`versionCode = 6`)  
**Platform Target:** Android 7.0+ (API Level 24–36)  
**Core Language:** Kotlin 2.2.10 (AGP 9.1.1, JVM Toolchain 11/21)  
**UI Toolkit:** Jetpack Compose (BOM 2024.09.00, Material Design 3 v1.4.0, Samsung One UI 8.5 Ergonomics)  
**Architecture Pattern:** Clean Architecture + MVVM (Unidirectional Data Flow) + Reactive StateFlow + Mutex Concurrency  
**Data Persistence:** SharedPreferences (Fast JSON serialization) + 400ms Debounced 4-Tier Offline Vault (Zero-Click Recovery)  
**Hardware & System APIs:** AlarmManager (Single-Earliest `setAlarmClock()` Promotion + `setExactAndAllowWhileIdle()` 7-day horizon), `goAsync()` BroadcastReceivers, NotificationManager (Silent Channel `chronos_automation_silent_channel` & Priority DND Filter), Lazy Self-Releasing `TextToSpeech` + `RingtoneManager` (`AudioAttributes.USAGE_ALARM` / `AudioManager.STREAM_ALARM` + `MediaPlayer` fallback), Power/Volume-Key Voice Stop, PowerManager (`25s` Partial WakeLock cap), VibratorManager  
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
                        |   - USAGE_ALARM/STREAM_ALARM + 12 Voices|
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
                 - Lazy STREAM_ALARM TTS + 25s Wake    |
                 - Scoped Power/Volume-Key Voice Stop  |
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
|    - Instant Volume Up/Down dispatchKeyEvent voice stop                |
|    - Bottom Navigation + Floating Speed-Dial (Manual / Terminal / Doc) |
|    - Canonical task resolution & drag preview/commit/discard wiring    |
|                                                                        |
|  [ RoutineViewModel.kt ]                                              |
|    - Reactive StateFlow + Dispatchers.IO off-Main-Thread mutations     |
|    - Decoupled rawScheduleResult + distinctUntilChanged active sort    |
|    - Foreground-only 15s clock ticker + midnight rollover observer     |
|    - Non-destructive _pendingDraftTasks + pendingImpactDiff StateFlows |
|    - Mutex-serialized drag scrub / vertical drop / Compact & Auto-Fit  |
|                                                                        |
|  Screens:                                                              |
|    ├── TimelineScreen.kt        (Outer PointerEventPass.Initial drag,  |
|    │                             Two-Phase Vertical Settling, Shadows, |
|    │                             Sticky Glass Countdown + 48dp Scrim,  |
|    │                             Corner Gap Pill, 60/120 FPS glide)    |
|    ├── AutomationCenterScreen.kt(Hardware telemetry & diagnostics)     |
|    ├── SettingsScreen.kt        (24h Donut dial, Voice Persona, Pause, |
|    │                             90-day Analytics, Trash shortcut)     |
|    ├── ConsoleTerminalScreen.kt (540dp Obsidian Studio, metrics, CLI)  |
|    └── TrashBinScreen.kt        (Soft-delete bin & 30-day purge)       |
|                                                                        |
|  Sheets & Components:                                                  |
|    ├── TaskModalBottomSheet.kt  (Unified Studio, Always-On Per-Task    |
|    │                             Sliders, Split, Modal Vol-Key Stop)   |
|    ├── AnchorEditBottomSheet.kt (Circadian fine-tuner, per-task slider)|
|    ├── ChronosArchitectureGuideSheet.kt (VOL. XI v1.5 Dispatches I-XII)|
|    └── OneUiCards.kt            (Anchor/Dynamic/Overflow, ⚡/↪ Badges) |
+-----------------------------------+------------------------------------+
                                    |
                                    v
+------------------------------------------------------------------------+
|                            DOMAIN ENGINE                               |
|                                                                        |
|  [ SchedulerEngine.kt ]                                                |
|    - 7 Immutable Anchors + Slot 1 Reverse Sleep + Friday Jumu'ah       |
|    - Slot 5 Elastic Buffer (15m floor, full alerts & 0-25m DND delay)  |
|    - O(N log N) Interval-Merging findAvailableGaps()                   |
|    - Two-Tier Human-Level Solver & Largest-Remainder Water-Filling     |
|    - Birth-Time & Birth-Duration Auto-Return (originalDurationMinutes) |
|    - Fluid Twin Same-Gap Partner Exclusion + compactAndRescueSchedule()|
|                                                                        |
|  [ ConsoleParser.kt ]                                                  |
|    - Pipe parser for ANCHOR, TWIN, and DYNAMIC (v2) directives         |
|    - Clean headerless birth-time/duration backup script exporter       |
|                                                                        |
|  [ RoutineTask.kt ]                                                    |
|    - Domain models incl. originalStartTimeMinute,                      |
|      originalDurationMinutes, & manualPlacementOrder                   |
+-----------------------------------+------------------------------------+
                                    |
                                    v
+------------------------------------------------------------------------+
|                             DATA LAYER                                 |
|                                                                        |
|  [ RoutineRepository.kt ]                                              |
|    - SharedPreferences (chronos_routine_prefs): fast JSON storage      |
|    - Legacy unified-volume key scrub on startup + vault rewrite        |
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
- **Purpose:** Compiled v1.5 APK placed directly at the repository root for zero-click downloads (`versionCode = 6`, `versionName = "1.5"`).
- **Key Specifications:** Directly accessible via raw GitHub CDN (`https://raw.githubusercontent.com/ibrahim-khalilullah-dev/Chronos-Routine-Engine/main/chronos.apk`) and exempted from gitignore (`!chronos.apk`), enabling seamless in-place upgrades.

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
- **Purpose:** Centralized Version Catalog managing core Compose/Kotlin/Coroutine/Robolectric dependencies and pruned legacy definitions.
- **Key Elements:**
  - `versions`: AGP (9.1.1), Kotlin (2.2.10), Compose BOM (2024.09.00), Material3 (1.4.0), Robolectric (4.16.1).
  - `libraries`: Compose UI/Material3/Icons, Lifecycle Runtime/ViewModel Compose, Coroutines Core/Android, JUnit, Robolectric, Compose UI Test.

#### `/.env.example`
- **Purpose:** Contract placeholder file required by the Secrets Gradle Plugin.
- **Key Contents:** Contains environment variable documentation. Kept clean to avoid generating malformed Java constants in `BuildConfig.java`.

#### `/metadata.json`
- **Purpose:** AI Studio platform metadata file.
- **Key Contents:** App display name (`Chronos`), description, capability flags (`MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`).

#### `/generate_icons.sh`
- **Purpose:** Shell script utilizing ImageMagick `convert` to produce multi-density squircle and round launcher icons (`mdpi` to `xxxhdpi`).

#### `/README.md`
- **Purpose:** Public-facing comprehensive project documentation, v1.5 feature manual, and direct APK download portal.

#### `/PROJECT.md` (This File)
- **Purpose:** Authoritative AI context map, architectural specification, and engineering blueprint.

---

### App Module Configuration

#### `/app/build.gradle.kts`
- **Purpose:** Module-level build script defining SDK targets, compile options, pruned dependencies, and build types.
- **Key Specifications:**
  - `namespace`: `com.example`
  - `applicationId`: `com.aistudio.chronosroutine.mzyqpa`
  - `compileSdk`: 36 (minor API level 1)
  - `minSdk`: 24 (Android 7.0)
  - `targetSdk`: 36 (Android 16 Ready)
  - `versionCode`: 6 / `versionName`: `"1.5"`
  - `buildFeatures`: `compose = true`, `buildConfig = true`
  - `signingConfigs`: Configured with `debugConfig` using `${rootDir}/debug.keystore`.
  - **Dependency Pruning:** Unused plugins/SDKs (`firebase-*`, `room`, `retrofit`, `moshi`, `okhttp`, `accompanist`, `camera`, `datastore`, `navigation`, `play-services-location`, `ksp`, `google-services`) are disabled/commented out so zero unnecessary `ContentProvider` instances initialize at cold start or alarm wakeups on 4GB RAM devices.

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
  - `RoutineTask`: Complete `@Immutable` data model representing scheduled and unscheduled routine tasks.
    * Core fields: `id`, `title`, `type`, `slotNumber`, `startTimeMinute` (0..1439), `originalStartTimeMinute` (nullable birth time preserved across scheduling shifts), `durationMinutes`, `originalDurationMinutes` (nullable birth duration preserved across elastic auto-flexing), `selectedDays`, `isDeleted`, `deletedTimestamp`.
    * Telemetry & Voice: `voiceMessage`, `enableDnd`, `dndDelayMinutes` (0..25m), `enableHaptic`, `isVoiceEnabled`, `voiceVolume` (`0.0f..1.0f` per-task alarm-stream volume), `isSoundAlertEnabled`, `customRingtoneUri`, `customNotificationTitle`, `customNotificationBody`.
    * Fluid Twins: `isFluidTwin`, `fluidTwinId`, `fluidTwinPart` (1 or 2), `fluidTotalPoolMinutes`, `fluidSafetyFloorMinutes`.
    * Flexibility, Priority & Drag Scheduling: `allowSplitting`, `isHighPriority`, `customFlexibilityMinutes` (0..30m), `toleranceMinutes`, `interTaskBufferMinutes` (0..30m), `manualPlacementOrder` (`Long` monotonic tie-breaker updated on manual drag placement).
    * Friday Slot 3 Overrides: `fridayTitle`, `fridayStartTimeMinute`, `fridayDurationMinutes`, `fridayVoiceMessage`.
    * Vacation & Pause: `isPausedTodayOnly`, `pausedUntilDateMillis`, `isPausedIndefinitely`.
    * Computed properties: `endTimeMinute`, `formattedStartTime`, `formattedEndTime`, `formattedDuration`, `notificationDisplayTitle`, `notificationDisplayBody`, `daysRemainingInTrash`, `isCurrentlyPaused()`.

---

### Data & Persistence Layer (`app/src/main/java/com/example/data/`)

#### `RoutineRepository.kt`
- **Purpose:** Central data repository managing active routines, persistence in `SharedPreferences`, legacy key cleanup, and debounced multi-tier offline disk backups.
- **Key Responsibilities:**
  - Fast Memory-Mapped Storage: Stores JSON arrays for anchors (`key_anchors_json`), dynamic tasks (`key_dynamic_tasks_json`), ratings (`key_task_rating_history_json`), voice persona (`key_voice_preset_id`), and preferences in private `SharedPreferences` (`chronos_routine_prefs`), normalizing `originalStartTimeMinute` and `originalDurationMinutes`.
  - Upgrade Legacy Key Scrubbing: On `init`, detects and removes deprecated `"key_use_same_voice_volume_for_all"` from `SharedPreferences` via `.apply()` and triggers `persistVaultAsync()`, while omitting `"useSameVoiceVolumeForAll"` from vault read/write payloads so v1.4 backups never reinstate the deleted toggle and every task's saved `voiceVolume` stays intact.
  - 400ms Debounced 4-Tier Offline Vault (`chronos_vault.json`): Uses a serialized single-thread `ioScope` (`Dispatchers.IO.limitedParallelism(1)`), hash-skip deduplication (`lastSavedVaultHash`), and `VAULT_DEBOUNCE_MS = 400L` (`persistVaultAsync()`) across:
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
- **Purpose:** Pure deterministic `O(N log N)` mathematical scheduling engine (`< 1–3ms/day`) calculating gaps, placing dynamic tasks & Fluid Twins, running the Human-Level Smart Solver, computing interactive drag bounds/drops, and enforcing circadian anchor invariants.
- **Key Invariants & Helpers:**
  - `birthDurationOf(task)`, `effectiveFlexOf(task)` (`0..30m`), `minAllowedDurationOf(task)`, `minDonorDurationOf(task)`, `availableFlexSlackOf(task)`, and `distributeDeficitWaterFilling(slacks, deficit)` (Largest-Remainder Fair-Share Water-Filling).
  - `MIN_ELASTIC_BUFFER_MINUTES = 15`: Safety floor for the Slot 5 elastic buffer; `fluidSafetyFloorMinutes` supports `<45m` Fluid Twins down to 2m pools.
  - **Zero Auto-Splitting Architecture:** Tasks are never fragmented automatically into chunks; unplaceable tasks route cleanly to `overflowTasks`.
- **Key Functions & Types:**
  - `createDefaultAnchors(sleepDurationMinutes)` / `calculateAnchors()` / `adjustAnchor()`: Manages all 7 circadian anchors (`11:00 PM` Sleep, `05:00 AM` Fajr 35m/8m, `01:15 PM` Dhuhr 35m/8m + Fri Jumu'ah `12:30 PM` 120m, `04:30 PM` Asr 25m/10m, `04:55 PM` Break 65m, `06:00 PM` Maghrib 30m/5m, `08:00 PM` Isha 35m/10m).
  - `findAvailableGaps(committedTasks, interTaskBufferMinutes)`: Sorted `O(N log N)` interval-merging gap finder with zero `1440`-element per-minute array allocation.
  - `scheduleDay(...)`: Two-Tier Human-Level Solver:
    1. **Pass 1 (Baseline Fit):** Places anchors, Friday overrides, Fluid Twins, and dynamic tasks at full birth duration sorted by `isHighPriority DESC`, `manualPlacementOrder DESC`, `originalStartTimeMinute ASC`.
    2. **Pass 2 (Bounded 4-Strategy Elastic Rescue — `trySolveStandardPendingTask`):**
       - `tryCrossAnchorMigration()`: Relocates smaller non-priority tasks into open alternate inter-anchor windows (honoring Fluid Twin Same-Gap Partner Exclusion).
       - `tryMicroDeficitBorrowing()`: Covers `1–5m` deficits from 1–2 flexible donors inside the window.
       - `tryCollectiveFairShareCompression()`: Pools window donors via Largest-Remainder Water-Filling (non-priority Tier-1 exhausted before High Priority Tier-2 donors).
       - `tryFlexToShrinkAndRelocate()`: Self-shrinks flexible tasks or shrinks-and-relocates movable neighbors within flex limits.
  - `compactAndRescueSchedule()`: Non-destructive timeline compaction + overflow rescue helper powering 1-tap `Compact` and `Auto-Fit` impact previews.
  - `getInterAnchorWindowKey()` & `findActiveTwinPartnerWindow()`: Enforces **Fluid Twin Same-Gap Partner Exclusion** across scheduling, compaction, and cross-anchor vertical drags.
  - `computeNetFreeGapMinutes(task, nextTask)`, `findEnclosingAnchorBounds()`, `computeHorizontalScrubBounds()`, `applyDragReposition()`, `applyVerticalDropReposition()`, `previewVerticalDrop()`, `computeScheduleImpactDiff()` (`ScheduleImpactItem`).

#### `ConsoleParser.kt`
- **Purpose:** Grammatical bulk CLI interpreter converting plaintext monospace pipe scripts into strongly typed `RoutineTask` collections.
- **Directives Supported:**
  - `ANCHOR | Slot(1-7) | Title | Start(HH:mm) | Dur(m) | DND(Y/N) | DndDelay(0-25) | Voice(Y/N) | Message | [FriOverrides] | [Days | Sound | VoiceVol]` (canonical `"anchor_slot_$slot"` IDs).
  - `TWIN | TotalPool(m) | Part1Title | Part1Start | Part1Dur | Part1Voice | Part2Title | Part2Start | Part2Dur | Part2Voice | DND(Y/N) | Days | Flex | Rest | [Category | P1Safety | P2Safety | P1VoiceYN | P2VoiceYN | P1Vol | P2Vol | Sound | DndDelay]`.
  - `DYNAMIC | Name | Duration(m) | Start(HH:mm) | DND(Y/N) | Days | Voice(Y/N) | Message | Flex | Rest | Priority | Category | [VoiceVol | Sound | DndDelay]` (and prefix-free equivalent).
- **Clean Headerless Backup Export (`generateRoutineScript()`):** Outputs purely live `ANCHOR`, `TWIN`, and `DYNAMIC` directives using each task's `originalStartTimeMinute` and `originalDurationMinutes` (`dndDelayMinutes` forced `0` when DND is off), while `parse()` populates both canonical birth fields cleanly.
- **Diagnostics:** Emits `ConsoleLineDiagnostic` with line numbers and syntax validation messages.

---

### Hardware Telemetry & Services (`app/src/main/java/com/example/service/` & `receiver/`)

#### `AutomationManager.kt`
- **Purpose:** System-level automation coordinator for alarms, DND policy, `USAGE_ALARM`/`STREAM_ALARM` audio telemetry, active preview coordination, and power management.
- **Key Capabilities:**
  - **Single-Earliest `setAlarmClock()` Promotion + Exact Horizon:** Evaluates active tasks across `dayOffset in -1..6` inside `rescheduleAllAlarmsAt()`, collects all `PlannedAlarmItem` entries, and promotes **only the single earliest upcoming alarm** (`earliestAlarmClockMillis` across Start, bounded Delayed DND, and DND-End) to `AlarmManager.setAlarmClock()`, registering all remaining horizon alarms via `setExactAndAllowWhileIdle()`.
  - **Strict Alarm-Stream Voice Helpers:** `configureAlarmTtsAttributes()` (`USAGE_ALARM` + `CONTENT_TYPE_SPEECH`), `createTtsVolumeParams(voiceVolume)` (`STREAM_ALARM` + clamped `0f..1f`), `requestTransientAlarmAudioFocus()`, and `abandonTransientAlarmAudioFocus()`. Always dispatches per-task `task.voiceVolume` cleanly (`EXTRA_VOICE_VOLUME`).
  - **Active Preview Stop Coordinator:** Thread-safe `registerActivePreviewStop()`, `clearActivePreviewStop()`, and `stopActivePreviewVoiceIfPlaying()` invoked immediately when Volume Up/Down is pressed inside `MainActivity` or open modal sheets.
  - **Slot 3 Friday Lockscreen Fidelity:** `buildTaskAlarmsForOccurrence()` applies Friday title, `fridayDurationMinutes`, `fridayVoiceMessage`, and accurate Friday end time while honoring `dndDelayMinutes` (`0–25m`).
  - **Task-Owned DND & Voice Personas:** `setDndMode()`, `reconcileStaleDndState()`, `releaseDndIfHeldByTask()`, `hasZeroDelayFollowUpDndTask()`, plus 12 offline bilingual BN/EN `VOICE_PRESETS` (`female`, `youth`, `male`) and `"com.google.android.tts"` discovery.

#### `RoutineAutomationService.kt`
- **Purpose:** `START_NOT_STICKY` foreground service (`mediaPlayback`) providing silent-channel lockscreen notifications, `USAGE_ALARM` ringtones, haptic pulses, lazy `STREAM_ALARM` self-releasing TTS, instant hardware-button voice stop, and task-owned DND transitions.
- **Key Implementations:**
  - **Strict `STREAM_ALARM` TTS Playback & Lazy Release:** Does NOT allocate `TextToSpeech` in `onCreate()`. Instantiates `TextToSpeech(applicationContext, this)` strictly when `isVoiceEnabled == true`, binds `USAGE_ALARM` + `STREAM_ALARM` with per-task `voiceVolume` (`0.0f..1.0f`) and `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`, then immediately calls `releaseTtsEngine()` inside `UtteranceProgressListener.onDone` / `onError` or a `20_000L` (`TTS_SAFETY_TIMEOUT_MS`) watchdog. WakeLocks are capped at `25_000L`.
  - **Zero-Overhead Scoped Hardware-Button Voice Stop:** Registers `ACTION_SCREEN_OFF`, `ACTION_SCREEN_ON`, `VOLUME_CHANGED_ACTION` (`BroadcastReceiver`), and `Settings.System` `ContentObserver` **strictly while active voice playback (`isSpeaking == true`) runs**, unregistering immediately when speech stops or completes (`stopVoicePlaybackOnly()` / `releaseTtsEngine()`). Includes a `200ms` screen-off debounce guard (`HARDWARE_STOP_DEBOUNCE_MS`) and volume-value change check so unrelated broadcasts never misfire; silences **ONLY** active voice (and companion chime) while keeping `isRunning`, active DND (`onDndApplied`), notifications, and future alarms completely untouched. Exposes `ACTION_STOP_VOICE_ONLY` and `isHardwareStopRegisteredForTesting()`.
  - **Silent Channel & `USAGE_ALARM` Ringtone Playback:** Creates `chronos_automation_silent_channel` (`setSound(null, null)`, `enableVibration(false)`, `VISIBILITY_PUBLIC`), playing custom user ringtones on `AudioAttributes.USAGE_ALARM` with `MediaPlayer` fallback.

#### `RoutineAlarmReceiver.kt`
- **Purpose:** `BroadcastReceiver` invoked by `AlarmManager` for `ACTION_START_TASK`, `ACTION_END_TASK`, and `ACTION_APPLY_DND`.
- **Action:** Starts `RoutineAutomationService` on task start and offloads `AutomationManager.rescheduleAllAlarms()`, `handleApplyDnd()`, and `handleEndTask()` onto `CoroutineScope(Dispatchers.Default)` inside `goAsync()`, rejecting stale `ACTION_APPLY_DND` triggers when `EXTRA_TASK_END_MILLIS` has expired.

#### `BootReceiver.kt`
- **Purpose:** System event receiver ensuring schedule persistence across `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `QUICKBOOT_POWERON`, `TIME_SET`, and `TIMEZONE_CHANGED` via `goAsync()` + `CoroutineScope(Dispatchers.Default)`.

---

### UI Layer (`app/src/main/java/com/example/ui/`)

#### `MainActivity.kt`
- **Purpose:** Root `ComponentActivity` configuring edge-to-edge window insets, runtime notification permissions, One UI bottom navigation (`TIMELINE`, `CONSOLE`, `AUTOMATION`, `SETTINGS`, `TRASH`), Floating Speed-Dial menu, canonical task resolution, drag preview wiring, and hardware Volume Up/Down voice stop (`dispatchKeyEvent` checks `AutomationManager.stopActivePreviewVoiceIfPlaying()` and active `RoutineAutomationService.ACTION_STOP_VOICE_ONLY`).

#### `RoutineViewModel.kt`
- **Purpose:** Core ViewModel orchestrating UI state, off-Main-Thread mutex mutations (`Dispatchers.IO` + `Dispatchers.Default`), distinct-sorted `rawScheduleResult` / `scheduleResult`, foreground-only 15s clock ticking, midnight rollover, non-destructive drag & `Compact`/`Auto-Fit` preview state (`_pendingDraftTasks`, `pendingImpactDiff`), Fluid Twin absorption, and full Slot 5 hardware persistence.

#### `OneUiCards.kt` (`ui/components/`)
- **Purpose:** Samsung One UI 8.5 AMOLED timeline cards (`AnchorTaskCard`, `DynamicTaskCard`, `OverflowPoolContainer`, `OverflowTaskRow`).
- **Key Features:** Displays live `⚡ ${birthDur}m→${durationMinutes}m` auto-flex badges and `↪ ${birthStart}` relocation markers, Source Ghost shadow styling (`alpha = 0.28f` + dashed `CyanAccent` border), and avoids `animateContentSize()` on card roots for glitch-free drag animations.

#### `TaskModalBottomSheet.kt` & `AnchorEditBottomSheet.kt` (`ui/components/`)
- **Purpose:** Unified Obsidian Studio sheets for creating and editing Single Dynamic Tasks, Fluid Twin pairs, and all 7 Circadian Anchors (including Slot 3 Friday Jumu'ah Dual Mode).
- **Key Features:** Always-on `0%–100%` per-task voice volume sliders whenever `isVoiceEnabled == true` (`test_voice_button_volume_slider`, `session_1_test_voice_button_volume_slider`, `session_2_test_voice_button_volume_slider`, `test_friday_voice_button_volume_slider`), strict `USAGE_ALARM`/`STREAM_ALARM` TTS previews via `applicationContext` (`com.google.android.tts` fallback + active modal Volume-key interception), `"⚡ Auto-flexed to Xm today (Birth duration: Ym preserved)"` hint, `"Born: <time>"` marker, and `0–25m` Auto DND Delay across every mode.

#### `ChronosArchitectureGuideSheet.kt` (`ui/components/`)
- **Purpose:** `"THE CHRONOS GAZETTE"` VOL. XI (`v1.5`) in-app newspaper broadsheet displaying live anchor status and Dispatches I through XII (covering Solver, Birth Auto-Return, Alarm-Stream Voice, and Hardware-Button Stop).

#### `TimelineScreen.kt` (`ui/screens/`)
- **Purpose:** Primary circadian timeline screen featuring the pinned top-right frosted obsidian glass countdown capsule above a subtle `48.dp` vertical fade scrim (`OneUiHeader`), 3-button staged confirmation bar with automatic baseline auto-dismiss, corner stopwatch net free-gap pills (`free_gap_indicator_${task.id}`), Single `Canvas` quick-jump / time cursor visuals, and outer-container `PointerEventPass.Initial` drag scheduling with Two-Phase Vertical Settling (`isSettlingVerticalDrop`), `DestinationDropShadowSlot` (`±8dp` hysteresis), Fluid Twin cross-anchor drops, `anchor_wall_toast`, and `Compact` / `Auto-Fit` action controls.

#### `SettingsScreen.kt` (`ui/screens/`)
- **Features:** 24-Hour Circadian Donut Dial (`Canvas`), collapsible **Voice Persona Studio** (`12` offline BN/EN profiles with `STREAM_ALARM` previews & custom BN/EN test field, flowing directly into **Trash Bin & Recovery**), **3-Mode Task Pause & Vacation Engine**, and **Quantified Self 90-day Analytics**.

#### `ConsoleTerminalScreen.kt`, `AutomationCenterScreen.kt`, `TrashBinScreen.kt` (`ui/screens/`)
- **Features:** Power-user 540dp Obsidian CLI console with metrics, collapsible error accordion, ghost autocomplete & clean headerless backup; hardware DND/battery/TTS diagnostics center; 30-day soft-delete Trash Bin.

#### `Color.kt`, `Theme.kt`, `Type.kt` (`ui/theme/`)
- **Purpose:** Centralized Material Design 3 and Samsung One UI 8.5 theme definition with AMOLED Obsidian palette (`#0B0E14`).

---

## 3. CORE MATHEMATICAL FORMULAS & INVARIANTS

### 1. Reverse Sleep Math (Slot 1)
$$\text{Bedtime Minute} = \left(\left(\text{Slot 2 Wake Minute} - \text{Sleep Duration Minutes} + 1440\right) \pmod{1440}\right)$$

### 2. Slot 5 Elastic Rubber Buffer
$$\text{Slot 5 Start} = \text{Slot 4 Start} + \text{Slot 4 Duration}$$
$$\text{Slot 5 Duration} = \max\left(\text{Slot 6 Start} - \text{Slot 5 Start}, 15\right)$$

### 3. Communicating Vessels Fluid Twin Balancing & Absorption
For linked twin sessions $S_1$ and $S_2$ with total pool $P$ and per-session safety floors $F_1, F_2 \ge 0$:
$$D_1 + D_2 = P, \quad D_1 \ge \max(1, F_1), \quad D_2 \ge \max(1, F_2)$$
When deleting one twin part via absorption:
$$D_{\text{surviving}} = P, \quad \text{isFluidTwin} = \text{false}, \quad \text{fluidTwinId} = \text{null}, \quad \text{fluidSafetyFloorMinutes} = 0$$

### 4. Collective Fair-Share Compression (Water-Filling) & Birth-Duration Auto-Return
For any task $T$ with birth duration $D_{\text{birth}}(T) = \text{originalDurationMinutes} \text{ or } \text{durationMinutes}$ and flexibility $\phi(T) = \max(\text{toleranceMinutes}, \text{customFlexibilityMinutes}) \in [0, 30]$:
$$D_{\min}(T) = \begin{cases} \operatorname{clamp}(F_T, 1, D_{\text{birth}}(T)) & \text{if } T.\text{isFluidTwin} \\ \operatorname{clamp}\left(D_{\text{birth}}(T) - \phi(T), \; \mathbb{I}[D_{\text{birth}}(T) \ge 15] \cdot 14 + 1, \; D_{\text{birth}}(T)\right) & \text{otherwise} \end{cases}$$
$$\text{Slack}(T) = \max\left(0, \; D_{\text{curr}}(T) - D_{\min}(T)\right)$$
To resolve a deficit $\Delta > 0$ inside an anchor window without starving any single task, `distributeDeficitWaterFilling()` assigns integer reductions proportional to remaining slack:
$$c_i = \left\lfloor \frac{\Delta_{\text{rem}} \cdot s_i}{\sum_j s_j} \right\rfloor$$
distributing remaining 1-minute units by largest fractional remainder (non-priority Tier-1 donors exhausted before High Priority Tier-2 donors). On subsequent unblocked evaluations, $T$ starts from $D_{\text{birth}}(T)$ cleanly (Birth-Duration Auto-Return) so repeated solver passes never double-flex tasks.

### 5. High Priority Anchor Wall & Fluid Twin Same-Gap Exclusion
For any non-twin task where `isHighPriority = true`, its scheduled interval $[t_{\text{start}}, t_{\text{end}}]$ must stay strictly inside the inter-anchor gap enclosing its requested start time:
$$A_{k,\text{end}} + B \le t_{\text{start}} < t_{\text{end}} \le A_{k+1,\text{start}} - B$$
For Fluid Twins, partner sessions $P_1, P_2$ must satisfy $\text{WinKey}(P_1) \neq \text{WinKey}(P_2)$ across scheduling, compaction, and interactive cross-anchor drags.

### 6. Net Free-Time Gap Telemetry (Buffer-Excluded)
For consecutive scheduled tasks $T_i$ and $T_{i+1}$ in `EARLIEST_START` order:
$$B_i = \begin{cases} \operatorname{clamp}(T_i.\text{interTaskBufferMinutes}, 0, 30) & \text{if } T_i.\text{type} = \text{DYNAMIC} \\ 0 & \text{otherwise} \end{cases}$$
$$\text{NetGap}(T_i, T_{i+1}) = \max\left(0, \; T_{i+1}.\text{startTimeMinute} - \left(T_i.\text{startTimeMinute} + T_i.\text{durationMinutes} + B_i\right)\right)$$
Gap pills render exclusively when $\text{NetGap} > 0$.

### 7. Horizontal Scrub Bounds & Drag Priority Invariant
$$\text{ScrubMin} = \max(G_{\text{start}}, A_{\text{start}}), \quad \text{ScrubMax} = \max\left(\text{ScrubMin}, \; \min(G_{\text{end}}, A_{\text{end}}) - (\text{dur}_T + B_T)\right)$$
Manual drag confirmation updates `startTimeMinute` and `originalStartTimeMinute` to the confirmed target time and assigns a monotonic `manualPlacementOrder`.

---

## 4. PERSISTENCE & OFFLINE VAULT PROTOCOL

1. **Active Storage:** Android `SharedPreferences` (`chronos_routine_prefs`) stores serialized JSON arrays (`originalStartTimeMinute` & `originalDurationMinutes` intact; legacy unified-volume key scrubbed on startup).
2. **Multi-Tier File Vault:** Mutations asynchronously update `chronos_vault.json` across 4 storage locations (`VAULT_DEBOUNCE_MS = 400L`, content-hash deduplicated) for survivability across updates and uninstalls.
3. **Zero-Click Reinstallation Recovery:** `checkAndAutoRestoreVault()` restores uninitialized installs once per launch before UI rendering begins.
4. **Data Retention Policies:** Deleted tasks auto-purge after 30 days; rating history rolls over at 90 days.

---

## 5. SUMMARY OF DEPENDENCY & ENVIRONMENT CONSTRAINTS

- **No Remote Backend Required:** Operates 100% locally on device with zero analytics or unused Firebase/Room/Retrofit runtime overhead.
- **Alarm-Stream Audio & Voice:** Uses Android `AudioManager` (`USAGE_ALARM` / `STREAM_ALARM`), `RingtoneManager` / `MediaPlayer`, and lazy self-releasing `TextToSpeech` with zero-overhead hardware-key interruption.
- **Alarms & DND Automation:** Single-earliest `AlarmManager.setAlarmClock()` + `setExactAndAllowWhileIdle()` (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`) and Notification Policy Access (`ACCESS_NOTIFICATION_POLICY`).
- **Secrets:** Managed via Secrets Gradle Plugin through `.env.example`. No API keys are required for core operations.
