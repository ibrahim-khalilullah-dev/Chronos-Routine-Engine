# CHRONOS ENGINE: COMPREHENSIVE ARCHITECTURAL CONTEXT MAP & BLUEPRINT

**Platform Target:** Android 8.0+ (API Level 26–36)  
**Core Language:** Kotlin 2.0.21 (JVM Toolchain 21)  
**UI Toolkit:** Jetpack Compose (Material Design 3 v1.4.0, Samsung One UI 8.5 Ergonomics)  
**Architecture Pattern:** Clean Architecture + MVVM (Unidirectional Data Flow) + Reactive StateFlow + Mutex Concurrency  
**Data Persistence:** SharedPreferences (Fast JSON serialization) + Multi-Tier Offline File Vault (Zero-Click Recovery)  
**Hardware & System APIs:** AlarmManager (`setAlarmClock`, exact RTC `ACTION_APPLY_DND`), NotificationManager (Priority DND Filter), TextToSpeech (Transient Audio Ducking), PowerManager (Partial WakeLock), VibratorManager  
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
                                            |
                                            v
                        +----------------------------------------+
                        |        [ AutomationManager.kt ]        |
                        |   - setAlarmClock() scheduling         |
                        |   - Exact RTC ACTION_APPLY_DND alarms  |
                        |   - DND Interruption Filter Policy     |
                        |   - Battery Whitelist Management       |
                        +---------+--------------------+---------+
                                  |                    |
                                  v                    v
                      [ RoutineAlarmReceiver.kt ]      |
                      - ACTION_START_TASK              |
                      - ACTION_END_TASK                |
                      - ACTION_APPLY_DND               |
                                  |                    |
                                  v                    |
                 [ RoutineAutomationService.kt ]       |
                 - MediaPlayback Foreground Service    |
                 - TextToSpeech (Audio Ducking)        |
                 - Deep-Sleep Immune DND triggers      |
                 - Lockscreen Notification with EndsAt |
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
|    - Bottom Navigation (Timeline, Add Task, Automation, Settings)      |
|                                                                        |
|  [ RoutineViewModel.kt ]                                              |
|    - Holds reactive UI state (StateFlow)                               |
|    - Decoupled rawScheduleResult on Dispatchers.Default from 15s clock |
|    - Lightweight sorting StateFlow derived without re-scheduling       |
|    - Enforces thread-safe mutations via Mutex (scheduleMutex)          |
|    - Coordinates SchedulerEngine and RoutineRepository                |
|    - Midnight date rollover auto-shift monitor                         |
|    - Fluid Twin Absorption engine (Delete Both / Absorb Time)         |
|                                                                        |
|  Screens:                                                              |
|    ├── TimelineScreen.kt        (Stable task.id recycling, 60/120 FPS) |
|    ├── AutomationCenterScreen.kt(Hardware telemetry & diagnostics)     |
|    ├── SettingsScreen.kt        (24h Donut dial, 90d ratings, pause)   |
|    ├── ConsoleTerminalScreen.kt (540dp Obsidian Studio, 1-click Copy, metrics)|
|    └── TrashBinScreen.kt        (Soft-delete bin & 30-day purge)       |
|                                                                        |
|  Sheets & Components:                                                  |
|    ├── TaskModalBottomSheet.kt  (Header Priority/Split pills, unified voice) |
|    ├── AnchorEditBottomSheet.kt (Circadian fine-tuner, voice toggle switch)  |
|    ├── ChronosArchitectureGuideSheet.kt (In-app broadsheet doc)        |
|    └── OneUiCards.kt            (Anchor/Dynamic/Overflow cards)        |
+-----------------------------------+------------------------------------+
                                    |
                                    v
+------------------------------------------------------------------------+
|                            DOMAIN ENGINE                               |
|                                                                        |
|  [ SchedulerEngine.kt ]                                                |
|    - 7 Immutable Circadian Anchors with hard mathematical boundaries    |
|    - Slot 1 Reverse Sleep Calculator: ((Slot 2 - Sleep + 1440) % 1440)  |
|    - Slot 3 Friday Jumu'ah Dual Mode (12:30 PM, 120m, 5m DND delay)    |
|    - Slot 5 Accordion Elastic Buffer (bridges Slot 4 End -> Slot 6)    |
|    - High Priority Anchor Wall Fence (restricts to enclosing gap)      |
|    - Fluid Twin Tasks (Communicating vessels hydraulic duration pool)  |
|    - Custom Schedule Flexibility (0..30m) & Rest Buffer (0..30m)       |
|    - Unfragmented Placement & Overflow Pool Routing (zero auto-split)  |
|                                                                        |
|  [ ConsoleParser.kt ]                                                  |
|    - Grammatical pipe parser for ANCHOR, TWIN, and DYNAMIC directives  |
|    - Pre-packaged authentic Bengali circadian template                 |
|    - Real-time line diagnostics and inline ghost prediction            |
|                                                                        |
|  [ RoutineTask.kt ]                                                    |
|    - Core domain entities: RoutineTask, TaskType, RoutineDay,          |
|      TaskRatingRecord, ScheduleGap                                     |
+-----------------------------------+------------------------------------+
                                    |
                                    v
+------------------------------------------------------------------------+
|                             DATA LAYER                                 |
|                                                                        |
|  [ RoutineRepository.kt ]                                              |
|    - SharedPreferences (chronos_routine_prefs): fast JSON storage      |
|    - 4-Tier Offline Vault (chronos_vault.json):                        |
|        1. App-specific external files root                             |
|        2. Primary Public Documents directory (/Documents/ChronosBackup)|
|        3. App-specific external documents directory                    |
|        4. Internal app private storage (/data/user/0/.../files)        |
|    - Zero-click automatic restore on fresh installation                |
|    - 90-Day Task Rating History with rolling decay auto-purge          |
|    - 30-Day Trash Bin soft-delete auto-purge                           |
+------------------------------------------------------------------------+
```

---

## 2. EXHAUSTIVE FILE-BY-FILE CONTEXT MAP

### Root & Build Configuration

#### `/.gitignore`
- **Purpose:** Specifies untracked files and build artifacts ignored by Git.
- **Key Specifications:** Contains `!chronos.apk` to ensure the release APK at the root directory is tracked for direct one-tap installation and raw CDN distribution.

#### `/chronos.apk`
- **Purpose:** Production-ready debug/release APK placed directly at the repository root for zero-click downloads.
- **Key Specifications:** Directly accessible via raw GitHub CDN (`https://raw.githubusercontent.com/ibrahim-khalilullah-dev/Chronos-Routine-Engine/main/chronos.apk`) and exempted from gitignore (`!chronos.apk`), enabling direct browser downloads and 1-tap installation without 404 errors.

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
  - `versions`: AGP (8.9.0), Kotlin (2.0.21), Compose BOM (2025.02.00), Room (2.6.1), Retrofit (2.11.0), Moshi (1.15.2), Robolectric (4.14.1).
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
- **Purpose:** Public-facing comprehensive project documentation, feature manual, and luxury One UI 8.5 download portal.

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
  - `buildFeatures`: `compose = true`, `buildConfig = true`
  - `signingConfigs`: Configured with `debugConfig` using `${rootDir}/debug.keystore`.

#### `/app/proguard-rules.pro`
- **Purpose:** ProGuard and R8 shrinking/obfuscation rules for release compilation.

#### `/app/src/main/AndroidManifest.xml`
- **Purpose:** Core Android application manifest declaring permissions, activities, services, receivers, and hardware capabilities.
- **Key Declarations:**
  - Permissions: `POST_NOTIFICATIONS`, `ACCESS_NOTIFICATION_POLICY` (DND), `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `WAKE_LOCK`, `VIBRATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `RECEIVE_BOOT_COMPLETED`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
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
    * Core fields: `id`, `title`, `type`, `slotNumber`, `startTimeMinute` (0..1439), `durationMinutes`, `selectedDays`, `isDeleted`, `deletedTimestamp`.
    * Telemetry & Voice: `voiceMessage`, `enableDnd`, `dndDelayMinutes` (0..25m), `enableHaptic`, `isVoiceEnabled`, `voiceVolume`, `isSoundAlertEnabled`, `customRingtoneUri`, `customNotificationTitle`, `customNotificationBody`.
    * Fluid Twins: `isFluidTwin`, `fluidTwinId`, `fluidTwinPart` (1 or 2), `fluidTotalPoolMinutes`, `fluidSafetyFloorMinutes`.
    * Flexibility & Splitting: `allowSplitting`, `isHighPriority`, `customFlexibilityMinutes` (0..30m), `toleranceMinutes`, `interTaskBufferMinutes` (0..30m).
    * Friday Slot 3 Overrides: `fridayTitle`, `fridayStartTimeMinute`, `fridayDurationMinutes`, `fridayVoiceMessage`.
    * Vacation & Pause: `isPausedTodayOnly`, `pausedUntilDateMillis`, `isPausedIndefinitely`.
    * Computed properties: `endTimeMinute`, `formattedStartTime`, `formattedEndTime`, `formattedDuration`, `notificationDisplayTitle`, `notificationDisplayBody`, `daysRemainingInTrash`, `isCurrentlyPaused()`.

---

### Data & Persistence Layer (`app/src/main/java/com/example/data/`)

#### `RoutineRepository.kt`
- **Purpose:** Central data repository managing active routines, persistence in `SharedPreferences`, and multi-tier offline disk backups.
- **Key Responsibilities:**
  - Fast Memory-Mapped Storage: Stores JSON arrays for anchors (`key_anchors_json`), dynamic tasks (`key_dynamic_tasks_json`), ratings (`key_task_rating_history_json`), and preferences in private `SharedPreferences`.
  - 4-Tier Offline Vault (`chronos_vault.json`):
    1. App external files directory (`context.getExternalFilesDir(null)/ChronosBackup/`).
    2. Shared public Documents directory (`/storage/emulated/0/Documents/ChronosBackup/`).
    3. Fallback app external documents directory (`context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)/ChronosBackup/`).
    4. Private internal storage (`context.filesDir/ChronosBackup/`).
  - Zero-Click Auto-Restore (`checkAndAutoRestoreVault()`): Automatically scans for an existing vault on app launch; if preferences are empty or missing, restores all anchors, dynamic tasks, ratings, and sleep/buffer settings without user intervention.
  - Soft-Delete Management: `softDeleteTask()`, `restoreTask()`, `purgeTaskPermanently()`, `purgeAllDeletedTasks()`, and auto-purging tasks older than 30 days (`PURGE_THRESHOLD_MILLIS`).
  - Quantified Self Retention: `recordTaskRating()` and `getTaskRatingHistory()` managing 90-day rating records with rolling expiration.

---

### Scheduling & Parsing Engines (`app/src/main/java/com/example/engine/`)

#### `SchedulerEngine.kt`
- **Purpose:** Pure mathematical scheduling engine calculating gaps, placing dynamic tasks, managing fluid twins, and enforcing circadian anchor invariants.
- **Key Invariants:**
  - `fluidSafetyFloorMinutes` (per-session): Optional minimum duration floor (`0`..session duration, 1m minimum for non-zero sessions), supporting `<45m` Fluid Twins down to 2m pools while respecting per-part rest buffers.
  - `MIN_ELASTIC_BUFFER_MINUTES = 15`: Safety floor for the Slot 5 elastic buffer.
  - **Zero Auto-Splitting Architecture:** Tasks are never fragmented or split automatically into chunks. If a dynamic task cannot fit into available circadian gaps (taking into account user flexibility tolerance), it is routed intact to the Overflow Pool for user inspection and manual rearrangement.
- **Key Functions:**
  - `createDefaultAnchors(sleepDurationMinutes)`: Factory creating the 7 default circadian anchors.
  - `calculateAnchors(anchors, sleepDurationMinutes)`: Evaluates dynamic dependencies. Automatically updates Slot 1 bedtime when Slot 2 wake-up moves (with full modulo 1440 midnight-crossing safety), and recalibrates Slot 5 duration when Slot 4 or Slot 6 moves.
  - `adjustAnchor(anchors, slotNumber, deltaMinutes, sleepDurationMinutes)`: Quick-adjusts any anchor by ±5m increments and cascades recalculation.
  - `findAvailableGaps(committedTasks, interTaskBufferMinutes)`: Computes unallocated 24-hour time slots taking into account individual inter-task rest buffers.
  - `scheduleDay(anchors, dynamicTasks, activeDay, interTaskBufferMinutes)`: Primary scheduling pipeline handling Friday Jumu'ah swapping, fluid twin balancing, high-priority anchor wall fencing, schedule flexibility tolerance, and unfragmented overflow handling.

#### `ConsoleParser.kt`
- **Purpose:** Grammatical bulk CLI interpreter converting plaintext monospace pipe scripts into strongly typed `RoutineTask` collections.
- **Directives Supported:**
  - `ANCHOR | Slot(1-7) | Title | Start(HH:mm) | Duration(m) | DND(Y/N) | DndDelay(0-25) | Voice(Y/N) | Message | [FriOverrides] | [Days | Sound | VoiceVol]` (canonical `"anchor_slot_$slot"` IDs).
  - `TWIN | TotalPool(m) | Part1Title | Part1Start | Part1Dur | Part1Voice | Part2Title | Part2Start | Part2Dur | Part2Voice | DND(Y/N) | Days | Flex | Rest | [Category | P1Safety | P2Safety | P1VoiceYN | P2VoiceYN | P1Vol | P2Vol | Sound]`.
  - `DYNAMIC | Name | Duration(m) | Start(HH:mm) | DND(Y/N) | Days | Voice(Y/N) | Message | Flex | Rest | Priority | Category | [VoiceVol | Sound]` (and prefix-free equivalent).
- **Pre-Packaged Routine (`SAMPLE_SNIPPET`):** Complete Bengali circadian schedule featuring the 7 immutable circadian prayer anchors, Saturday–Thursday focus routines, and Friday special routines.
- **Diagnostics:** Emits `ConsoleLineDiagnostic` with precise line numbers and syntax validation messages for immediate visual feedback.

---

### Hardware Telemetry & Services (`app/src/main/java/com/example/service/` & `receiver/`)

#### `AutomationManager.kt`
- **Purpose:** System-level automation coordinator for alarms, DND policy, audio telemetry, and power management.
- **Key Capabilities:**
  - `setAlarmClock()`: Schedules task start alarms and DND-enabled `ACTION_END_TASK` alarms via `AlarmManager.setAlarmClock()` to bypass Samsung One UI aggressive app sleep and Doze restrictions.
  - `scheduleDndAlarm(context, task, triggerMillis)`: Schedules a dedicated exact RTC alarm (`ACTION_APPLY_DND`) for tasks with delayed DND timers (`dndDelayMinutes > 0`), ensuring reliable execution even during deep Doze sleep.
  - `cancelDndAlarm(context, task)` / `pendingIntent.cancel()`: Cancels pending alarms and DND triggers cleanly when tasks end or are rescheduled.
  - `calculateNextOccurrenceMillis(task, activeDay)`: Calculates the exact timestamp of the upcoming task occurrence, iterating forward up to 7 days to match `task.selectedDays`.
  - `setDndMode(context, enabled, taskId)` / `releaseDndIfHeldByTask()` / `hasZeroDelayFollowUpDndTask()`: Persists the active DND owner task ID synchronously (`commit()`), releases DND only for its owner, and skips teardown when a zero-delay follow-up DND task starts immediately at boundary.
  - `rescheduleAllAlarms(context)`: Evaluates active tasks across a 7-day horizon and registers each task's earliest upcoming non-overflow occurrence in `AlarmManager` (also purging legacy `slot_1..7` alarms).

#### `RoutineAutomationService.kt`
- **Purpose:** Foreground service with `mediaPlayback` type providing lockscreen notifications, decoupled haptic/chime/TTS alerts, and task-owned DND transitions.
- **Key Implementations:**
  - Decoupled Alerts & Cancellable Delay: `enableHaptic`, `isSoundAlertEnabled`, and `isVoiceEnabled` execute independently with a cancellable `mainHandler` 1500ms delay between chime and TTS speech.
  - Lockscreen Notification with Ending Times: Displays formatted completion times (`notificationDisplayBody`, e.g. `Ends at 10:45 AM`) with `VISIBILITY_PUBLIC`.
  - Audio Focus Ducking: Obtains `AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` so media volume gracefully lowers during voice briefings.
  - WakeLock Protection: Acquires a partial wake lock with a 3-minute safety limit.
  - State Broadcasting: Emits `ActiveAutomationState` to `RoutineViewModel` via a static `MutableStateFlow`.

#### `RoutineAlarmReceiver.kt`
- **Purpose:** `BroadcastReceiver` invoked by `AlarmManager` for `ACTION_START_TASK`, `ACTION_END_TASK`, and `ACTION_APPLY_DND`.
- **Action:** Starts `RoutineAutomationService` on task start, verifies task active/DND state in `RoutineRepository` and engages Priority DND directly via `AutomationManager.setDndMode()` + `RoutineAutomationService.onDndApplied()` on `ACTION_APPLY_DND` (without calling `startService()` in the background), and guards `ACTION_END_TASK` DND teardown against owner mismatch and zero-delay follow-ups.

#### `BootReceiver.kt`
- **Purpose:** System event receiver ensuring schedule persistence across device state transitions.
- **Listened Intents:** `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `QUICKBOOT_POWERON`, `TIME_SET`, `TIMEZONE_CHANGED`.
- **Action:** Invokes `AutomationManager.rescheduleAllAlarms(context)`.

---

### UI Layer (`app/src/main/java/com/example/ui/`)

#### `MainActivity.kt`
- **Purpose:** Root Android `ComponentActivity` configuring edge-to-edge window insets, runtime notification permission requests, and the primary One UI bottom navigation shell.
- **Destinations Managed:** `TIMELINE`, `CONSOLE`, `AUTOMATION`, `SETTINGS`, `TRASH`.

#### `RoutineViewModel.kt`
- **Purpose:** Core ViewModel orchestrating UI state, concurrent task scheduling, and user interactions.
- **Key Implementations:**
  - Concurrency: Uses Kotlin `Mutex` (`scheduleMutex`) to ensure scheduling re-computations and repository mutations are strictly serialized without race conditions.
  - Midnight Rollover Monitor: Autonomous date observer in `viewModelScope` that triggers automatic day-switch to `RoutineDay.today()` at 00:00 midnight.
  - Fluid Twin Persistence & Absorption Engine:
    * `saveFluidTwin(...)`: Persists linked twin parts with shared hardware alerts, DND, recurrence days, flexibility, rest buffer, and independent per-session voice toggles/volumes (`part1IsVoiceEnabled`, `part2IsVoiceEnabled`, `part1VoiceVolume`, `part2VoiceVolume`).
    * `deleteBothFluidTwinParts(twinId)`: Moves both linked twin parts to the Trash Bin.
    * `deleteTwinAndAbsorbTime(deletedTaskId)`: Soft-deletes the selected part, liberates its allocated minutes, absorbs them into the surviving twin partner, unlinks it from the twin pool (`isFluidTwin = false`), and converts it into a standard dynamic task with the full consolidated pool duration.

#### `OneUiCards.kt` (`ui/components/`)
- **Purpose:** Visual timeline card components conforming to Samsung One UI 8.5 guidelines (`#0B0E14` AMOLED obsidian palette, `#0A0F1D` surfaces, `#1E2C45` borders).
- **Stability & Matrix Alignment:** Completely removed `Modifier.animateContentSize()` from `AnchorSlotCard`, `DynamicTaskCard`, and `OverflowTaskCard` container roots to eliminate infinite remeasure loops during fast scrolls. Displays concise flexibility (`±Xm`) and zero-floor rest buffer badges seamlessly synchronized with the task studio.

#### `TaskModalBottomSheet.kt` (`ui/components/`)
- **Purpose:** Full-featured creation and editing bottom sheet for Dynamic Tasks, Fluid Twin pairs, and Circadian Anchors.
- **Mutually Exclusive Header Pills:** Houses `Priority` (`AnimatedVisibility(visible = !isFluidTwinMode)`) and `Split` (`AnimatedVisibility(visible = !isHighPriority)`) interactive header pills so High Priority anchor-fencing and Fluid Twin splitting remain strictly mutually exclusive.
- **Anti-Clipping 2x2 Widget Hierarchy:** Standardizes Row 1 (`FLEXIBLE` & `BUFFER TIME`) and Row 2 (`TASK ALERTS` & `AUTO DND`) to uniform `heightIn(min = 124.dp)` squircle cards (`#0A0F1D`, `12.dp` padding) using a 2-row header layout (Row 1: 28dp icon + optional Switch; Row 2: full-width `11.sp` monospace title with `TextOverflow.Ellipsis`; Row 3: stepper/control). `FLEXIBLE` (0–30m in ±5m steps) and `BUFFER TIME` (0–30m precision scale: `0..5` in 1m steps, `5..30` in 5m steps) treat `0` as disabled with no redundant switches.
- **Unified Hardware Integration & Recurrence:** `TASK ALERTS`, `AUTO DND`, and `RECURRENCE SCHEDULE` render outside the single-vs-split branch so Split (Fluid Twin) sessions share the exact same hardware alert, DND, and weekly recurrence controls as single dynamic tasks.
- **Dual-Session Voice Studio (`DualSessionCard`):** Both Session 1 and Session 2 include independent voice ON/OFF switches (`isVoiceEnabledS1`/`S2`), collapsible custom voice message fields, interactive Play/Stop TTS preview toggles tracked per utterance ID (`"s1_tts"`, `"s2_tts"`) via `UtteranceProgressListener`, and 0%–100% volume sliders (`voiceVolumeS1`/`S2`).
- **Live Interactive `UtteranceProgressListener` TTS Briefing Player:** Tracks active speech playback (`isSpeakingVoicePreview` & `activeVoiceUtteranceId`) across Dynamic Tasks, Fluid Twins, and Circadian Anchors (including Friday Jumu'ah mode), toggling the preview button seamlessly between emerald Play (`Icons.Default.PlayArrow`) and crimson Stop (`Icons.Default.Stop`) with immediate `ttsInstance?.stop()` cancellation and clean disposal.

#### `AnchorEditBottomSheet.kt` (`ui/components/`)
- **Purpose:** Dedicated modal editor for fine-tuning circadian anchor properties and hardware automations.
- **Slot Invariants & Luxury Layout:** Preserves reverse sleep arithmetic for Slot 1, Friday Jumu'ah overrides for Slot 3, and elasticity bounds for Slot 5.
- **Voice Announcement Architecture:** Houses dedicated ON/OFF switches (`anchor_voice_switch` and `friday_voice_switch`), collapsing voice inputs when disabled and ensuring complete parity with `TaskModalBottomSheet`.

#### `TimelineScreen.kt` (`ui/screens/`)
- **Purpose:** Primary application screen displaying the circadian timeline for any selected day.
- **Crash-Proof Scrolling:** Uses composite unique keys in `LazyColumn` (`${task.id}_${task.slotNumber ?: ""}_${task.startTimeMinute}_$index`) to prevent duplicate key crashes, alongside keyed `derivedStateOf` for jump buttons.
- **Live Countdown Banner:** Displays active task progress and real-time countdown to the next circadian boundary.

#### `ConsoleTerminalScreen.kt` (`ui/screens/`)
- **Purpose:** Power-user monospace CLI bulk routine editor.
- **Features:** Cleaned Quick Action Bar highlighting the glowing cyan "Template" button, flex-safe Real-Time Syntax Validation Card (`weight(1f, fill = false)` with non-wrapping line count), and gradient action buttons.

#### `SettingsScreen.kt` (`ui/screens/`)
- **Features:** 24-Hour Circadian Donut Dial, 3-Mode Task Pause & Vacation Engine, Quantified Self 90-day history with category accordions.

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
