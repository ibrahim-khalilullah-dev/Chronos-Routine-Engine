# CHRONOS ENGINE: COMPREHENSIVE ARCHITECTURAL CONTEXT MAP & BLUEPRINT

**Platform Target:** Android 8.0+ (API Level 26–36)  
**Core Language:** Kotlin 2.0.21 (JVM Toolchain 21)  
**UI Toolkit:** Jetpack Compose (Material Design 3 v1.4.0, Samsung One UI 8.5 Ergonomics)  
**Architecture Pattern:** Clean Architecture + MVVM (Unidirectional Data Flow) + Reactive StateFlow + Mutex Concurrency  
**Data Persistence:** SharedPreferences (Fast JSON serialization) + Multi-Tier Offline File Vault (Zero-Click Recovery)  
**Hardware & System APIs:** AlarmManager (`setAlarmClock`), NotificationManager (Priority DND Filter), TextToSpeech (Transient Audio Ducking), PowerManager (Partial WakeLock), VibratorManager  
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
                        |   - DND Interruption Filter Policy     |
                        |   - Battery Whitelist Management       |
                        +---------+--------------------+---------+
                                  |                    |
                                  v                    v
                      [ RoutineAlarmReceiver.kt ]      |
                                  |                    |
                                  v                    |
                 [ RoutineAutomationService.kt ]       |
                 - MediaPlayback Foreground Service    |
                 - TextToSpeech (Audio Ducking)        |
                 - Delayed DND execution               |
                 - Lockscreen Notification             |
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
|    - Enforces thread-safe mutations via Mutex (scheduleMutex)          |
|    - Coordinates SchedulerEngine and RoutineRepository                |
|                                                                        |
|  Screens:                                                              |
|    ├── TimelineScreen.kt        (Interactive schedule, task cards)     |
|    ├── AutomationCenterScreen.kt(Hardware telemetry & diagnostics)     |
|    ├── SettingsScreen.kt        (24h Donut dial, 90d ratings, pause)   |
|    ├── ConsoleTerminalScreen.kt (Monospace CLI with ghost prediction)  |
|    └── TrashBinScreen.kt        (Soft-delete bin & 30-day purge)       |
|                                                                        |
|  Sheets & Components:                                                  |
|    ├── TaskModalBottomSheet.kt  (Visual task creator / editor)         |
|    ├── AnchorEditBottomSheet.kt (Anchor parameter fine-tuner)          |
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
|    - Task Auto-Splitting (>= 15m chunks for tasks > 60m)               |
|                                                                        |
|  [ ConsoleParser.kt ]                                                  |
|    - Grammatical pipe parser for ANCHOR, TWIN, and DYNAMIC directives  |
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
- **Key Contents:** Ignores `.gradle/`, `build/`, `*.apk`, local environment files, and IDE workspace caches.

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
- **Key Contents:** Generates adaptive launcher graphics featuring Chronos dark dial and glowing gradient arcs.

#### `/README.md`
- **Purpose:** Public-facing comprehensive project documentation, feature manual, and APK installation hub.
- **Connections:** Links directly to downloadable `chronos.apk`.

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
  - `secrets`: Configured with `propertiesFileName = ".env"` and `defaultPropertiesFileName = ".env.example"`.

#### `/app/proguard-rules.pro`
- **Purpose:** ProGuard and R8 shrinking/obfuscation rules for release compilation.

#### `/app/src/main/AndroidManifest.xml`
- **Purpose:** Core Android application manifest declaring permissions, activities, services, receivers, and hardware capabilities.
- **Key Declarations:**
  - Permissions: `POST_NOTIFICATIONS`, `ACCESS_NOTIFICATION_POLICY` (DND), `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `WAKE_LOCK`, `VIBRATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `RECEIVE_BOOT_COMPLETED`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
  - Application: `allowBackup="true"`, custom app icon and round icon, theme `@style/Theme.MyApplication`.
  - Activity: `MainActivity` with `showWhenLocked="true"`, `turnScreenOn="true"`, and `LAUNCHER` intent-filter.
  - Service: `RoutineAutomationService` with `foregroundServiceType="mediaPlayback"`.
  - Receivers: `RoutineAlarmReceiver` (actions `ACTION_START_TASK`, `ACTION_END_TASK`), `BootReceiver` (boot, update, and timezone events).

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
    * Fluid Twins: `isFluidTwin`, `fluidTwinId`, `fluidTwinPart` (1 or 2), `fluidTotalPoolMinutes`.
    * Flexibility & Splitting: `allowSplitting`, `isHighPriority`, `customFlexibilityMinutes` (0..30m), `toleranceMinutes`, `interTaskBufferMinutes` (0..30m).
    * Friday Slot 3 Overrides: `fridayTitle`, `fridayStartTimeMinute`, `fridayDurationMinutes`, `fridayVoiceMessage`.
    * Vacation & Pause: `isPausedTodayOnly`, `pausedUntilDateMillis`, `isPausedIndefinitely`.
    * Computed properties: `endTimeMinute`, `formattedStartTime`, `formattedEndTime`, `formattedDuration`, `notificationDisplayTitle`, `notificationDisplayBody`, `daysRemainingInTrash`, `isCurrentlyPaused()`.
    * Companion methods: `formatTime()`, `formatTime24()`, `parseTime24()`.
  - `ScheduleGap`: Represents an available time window `[startMinute, endMinute]` with `durationMinutes`.
  - `TaskRatingRecord`: Historical rating entry recording `taskId`, `taskTitle`, `category`, `durationMinutes`, `ratingPct` (0..100%), and `timestamp`.

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
- **Key Constants:**
  - `MIN_SPLIT_MINUTES = 15`: Minimum allowable chunk size for split tasks.
  - `SPLIT_THRESHOLD_MINUTES = 60`: Minimum task duration required to trigger splitting.
  - `MIN_FLUID_TWIN_MINUTES = 45`: Strict safety floor for any fluid twin session.
  - `MIN_ELASTIC_BUFFER_MINUTES = 15`: Safety floor for the Slot 5 elastic buffer.
- **Key Functions:**
  - `createDefaultAnchors(sleepDurationMinutes)`: Factory creating the 7 default circadian anchors:
    * Slot 1: Sleep (11:00 PM / 1380m, 360m / 6h, Reverse Sleep Tracker derived from Slot 2 wake-up).
    * Slot 2: Fajr (05:00 AM / 300m, 35m, 8m DND delay).
    * Slot 3: Dhuhr (01:15 PM / 795m, 35m, 8m DND delay; Friday Jumu'ah 12:30 PM, 120m, 5m DND delay).
    * Slot 4: Asr (04:30 PM / 990m, 25m, 10m DND delay).
    * Slot 5: Break (Rubber buffer bridging Slot 4 End to Slot 6 Start = 65m default).
    * Slot 6: Maghrib (06:00 PM / 1080m, 30m, 5m DND delay).
    * Slot 7: Isha (08:00 PM / 1200m, 35m, 10m DND delay).
  - `calculateAnchors(anchors, sleepDurationMinutes)`: Evaluates dynamic dependencies. Automatically updates Slot 1 bedtime when Slot 2 wake-up moves (with full modulo 1440 midnight-crossing safety), and recalibrates Slot 5 duration when Slot 4 or Slot 6 moves.
  - `adjustAnchor(anchors, slotNumber, deltaMinutes, sleepDurationMinutes)`: Quick-adjusts any anchor by ±5m increments and cascades recalculation.
  - `findAvailableGaps(committedTasks, interTaskBufferMinutes)`: Computes unallocated 24-hour time slots taking into account individual inter-task rest buffers.
  - `scheduleDay(anchors, dynamicTasks, activeDay, interTaskBufferMinutes)`: Primary scheduling pipeline:
    1. Filters day-active anchors (swapping Friday Jumu'ah parameters for Slot 3 when active day is Friday).
    2. Pools and balances Fluid Twin tasks across inter-anchor gaps ("communicating vessels" logic).
    3. Evaluates High Priority tasks (`isHighPriority = true`), confining them strictly to the anchor gap enclosing their preferred start time.
    4. Applies Schedule Flexibility (`customFlexibilityMinutes` / `toleranceMinutes` 0..30m) to fit tight gaps.
    5. Performs task splitting if enabled and duration > 60m.
    6. Routes unplaceable tasks safely into the `overflowTasks` pool.

#### `ConsoleParser.kt`
- **Purpose:** Grammatical bulk CLI interpreter converting plaintext monospace pipe scripts into strongly typed `RoutineTask` collections.
- **Directives Supported:**
  - `ANCHOR | Slot(1-7) | Title | Start(HH:mm) | Duration(m) | DND(Y/N) | DndDelay(0-25) | Voice(Y/N) | Message` (plus Friday overrides).
  - `TWIN | TotalPool(m) | Part1Title | Part1Start | Part1Dur | Part1Voice | Part2Title | Part2Start | Part2Dur | Part2Voice | DND(Y/N) | Days | Flex | Rest`.
  - `DYNAMIC | Name | Duration(m) | Start(HH:mm) | DND(Y/N) | Days | Voice(Y/N) | Message | Flex | Rest` (and prefix-free equivalent).
- **Diagnostics:** Emits `ConsoleLineDiagnostic` with precise line numbers and syntax validation messages for immediate visual feedback.

---

### Hardware Telemetry & Services (`app/src/main/java/com/example/service/` & `receiver/`)

#### `AutomationManager.kt`
- **Purpose:** System-level automation coordinator for alarms, DND policy, audio telemetry, and power management.
- **Key Capabilities:**
  - `setAlarmClock()`: Schedules task alarms via `AlarmManager.setAlarmClock()`, using Android Alarm Clock Info to bypass Samsung One UI aggressive app sleep and Doze restrictions.
  - `calculateNextOccurrenceMillis(task, activeDay)`: Calculates the exact timestamp of the upcoming task occurrence, iterating forward up to 7 days to match `task.selectedDays`.
  - `setDndMode(context, enable)`: Manages system DND using `NotificationManager.INTERRUPTION_FILTER_PRIORITY`, restoring previous system state on completion.
  - `resolveCategoryInfo(task, activeDay)`: Resolves contextual category icons and labels.
  - `rescheduleAllAlarms(context)`: Re-registers all active anchors and dynamic task start/end alarms in `AlarmManager`.

#### `RoutineAutomationService.kt`
- **Purpose:** Foreground service with `mediaPlayback` type providing lockscreen notifications, audio-ducking TTS voice briefings, and delayed DND triggers.
- **Key Implementations:**
  - Audio Focus Ducking: Obtains `AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` so media volume gracefully lowers during voice briefings.
  - WakeLock Protection: Acquires a partial wake lock with a 3-minute safety limit.
  - Delayed DND Engine: Schedules a `Handler` runnable to activate DND after `dndDelayMinutes` (0..25m).
  - State Broadcasting: Emits `ActiveAutomationState` to `RoutineViewModel` via a static `MutableStateFlow`.

#### `RoutineAlarmReceiver.kt`
- **Purpose:** `BroadcastReceiver` invoked by `AlarmManager` for `ACTION_START_TASK` and `ACTION_END_TASK`.
- **Action:** Starts `RoutineAutomationService` as a foreground service on task start, or dispatches a stop intent on task conclusion.

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
- **Key StateFlows:** `activeTab`, `activeDay`, `sortMode`, `sleepDurationMinutes`, `bufferMinutes`, `anchors`, `dynamicTasks`, `deletedTasks`, `currentMinute`, `isDndGranted`, `isBatteryOptimizationIgnored`, `activeAutomation`.
- **Concurrency:** Uses Kotlin `Mutex` (`scheduleMutex`) to ensure scheduling re-computations and repository mutations are strictly serialized without race conditions.

#### `OneUiCards.kt` (`ui/components/`)
- **Purpose:** Visual timeline card components conforming to Samsung One UI 8.5 guidelines.
- **Key Components:**
  - `AnchorSlotCard`: Displays anchors with slot badges, quick-adjust controls (±5m), DND status, voice status, and delayed DND indicators.
  - `DynamicTaskCard`: Renders dynamic tasks with progress indicators, category badges, high-priority shields, split-part labels, and two-step completion rating bars.
  - `OverflowTaskCard`: Visualizes unscheduled tasks with conflict explanations.
  - `GapCard`: Interactive indicator showing available open rest buffer minutes between tasks.
  - `TaskCompletionRatingBar`: Interactive 5-star rating control with 25% increments (0% to 100%).

#### `TaskModalBottomSheet.kt` (`ui/components/`)
- **Purpose:** Full-featured bottom sheet form for creating and editing dynamic tasks, fluid twin pairs, and anchor settings.
- **Features:** 13 selectable categories, circular time picker, duration slider, fluid twin hydraulic linking, DND delay sliders, custom ringtone pickers, and voice announcement configuration.

#### `AnchorEditBottomSheet.kt` (`ui/components/`)
- **Purpose:** Specialized anchor editor enforcing slot-specific architectural rules.
- **Features:** Slot 1 reverse sleep duration adjustment, Slot 3 Friday Jumu'ah dual-mode settings, Slot 5 recurrence selector, and DND delay configuration (0..25m).

#### `ChronosArchitectureGuideSheet.kt` (`ui/components/`)
- **Purpose:** In-app editorial newspaper broadsheet ("The Chronos Gazette", Vol. VIII).
- **Features:** Provides an architectural reference directly within the application, displaying the 7 immutable anchors table, Slot 5 elastic buffer mechanics, fluid twin communicating vessels, and bulk CLI syntax.

#### `TimelineScreen.kt` (`ui/screens/`)
- **Purpose:** Primary application screen displaying the circadian timeline for any selected day.
- **Features:** Day-selector ribbon, Real Today vs. Inspected Day duality, floating action pills (Jump to Active, Quick Add, Sort Menu, Trash indicator), live pulsing active-task progress bar, and anchor quick-adjust buttons.

#### `SettingsScreen.kt` (`ui/screens/`)
- **Purpose:** Engine settings, circadian visualizer, task vacation manager, and Quantified Self analytics.
- **Features:**
  - 24-Hour Circadian Donut Dial: Custom canvas rendering 1,440-minute day cycle with touch-to-inspect segment popups.
  - 3-Mode Task Pause & Vacation Engine: Interface to pause tasks for today, N days, or indefinitely.
  - Quantified Self Metrics: 90-day rating history across Day, Week, Month, and 3-Month horizons with interactive category accordions.
  - Sleep duration and global rest buffer configuration.

#### `AutomationCenterScreen.kt` (`ui/screens/`)
- **Purpose:** Hardware telemetry dashboard and automation readiness center.
- **Features:** DND policy status and settings launcher, battery optimization whitelist helper, active foreground service monitor with manual kill switch, and instant TTS voice preview.

#### `ConsoleTerminalScreen.kt` (`ui/screens/`)
- **Purpose:** Power-user monospace CLI bulk routine editor.
- **Features:** Multi-directive pipe parser (`ANCHOR`, `TWIN`, `DYNAMIC`), real-time syntax error highlighter, context-aware ghost auto-prediction bar, and full routine export to CLI script.

#### `TrashBinScreen.kt` (`ui/screens/`)
- **Purpose:** Soft-delete recovery center and permanent purge manager.
- **Features:** Displays deleted tasks with a 30-day countdown timer, individual restore/purge actions, and an empty trash confirmation dialog.

#### `Color.kt`, `Theme.kt`, `Type.kt` (`ui/theme/`)
- **Purpose:** Centralized Material Design 3 and Samsung One UI 8.5 theme definition.
- **Key Tokens:** AMOLED Obsidian Background (`#0B0E14`), elevated surfaces (`#131822`, `#1C2230`, `#252C3D`), hairline borders (`0x1AFFFFFF`), semantic gradients (Sleep, Anchor, Elastic, Dynamic, Overflow), and custom typography.

---

### Test Suite (`app/src/test/java/com/example/`)

#### `ExampleRobolectricTest.kt`
- **Purpose:** Comprehensive local JVM test suite executing against Android SDK 36 via Robolectric.
- **Key Tests Covered:**
  - Slot 1 reverse sleep modulo math (`((Slot 2 - Sleep + 1440) % 1440)`).
  - Midnight rollover safety for late wake-ups.
  - Slot 5 elastic buffer auto-sizing between Slot 4 and Slot 6.
  - Dynamic task auto-splitting into chunks >= 15m.
  - Console CLI pipe parsing with voice and DND flags.
  - Repository soft-delete, restore, and 30-day purge threshold.
  - Multi-tier offline vault auto-restore on simulated fresh reinstall.
  - Active minute detection (`isTaskActiveAt`) with day-isolation protection.
  - Schedule flexibility tolerance compression.
  - 3-mode task pause evaluation and schedule exclusion.
  - Inter-task rest buffer enforcement.

#### `ExampleUnitTest.kt`
- **Purpose:** Standard JUnit unit test placeholder verifying basic JVM arithmetic and test runner execution.

---

## 3. CORE MATHEMATICAL FORMULAS & INVARIANTS

### 1. Reverse Sleep Math (Slot 1)
$$\text{Bedtime Minute} = \left(\left(\text{Slot 2 Wake Minute} - \text{Sleep Duration Minutes} + 1440\right) \pmod{1440}\right)$$
*Example:* Wake up at 05:00 AM (300m) with 360m (6h) sleep duration:
$$\text{Bedtime} = (300 - 360 + 1440) \pmod{1440} = 1380\text{m} \implies \text{11:00 PM}$$

### 2. Slot 5 Elastic Rubber Buffer
$$\text{Slot 5 Start} = \text{Slot 4 Start} + \text{Slot 4 Duration}$$
$$\text{Slot 5 Duration} = \max\left(\text{Slot 6 Start} - \text{Slot 5 Start}, 15\right)$$

### 3. Communicating Vessels Fluid Twin Balancing
For linked twin sessions $S_1$ and $S_2$ with total pool $P$:
$$D_1 + D_2 = P, \quad D_1 \ge 45\text{m}, \quad D_2 \ge 45\text{m}$$
If inter-anchor gap $G_1$ imposes a ceiling $C_1 < D_1$:
$$D_1' = C_1, \quad D_2' = \min\left(P - C_1, C_2\right)$$
Surplus minutes automatically migrate to the unconstrained twin without violating immovable anchor walls.

### 4. High Priority Anchor Wall Fence
For any task where `isHighPriority = true`, its scheduled interval $[t_{\text{start}}, t_{\text{end}}]$ must satisfy:
$$A_{k,\text{end}} + B \le t_{\text{start}} < t_{\text{end}} \le A_{k+1,\text{start}} - B$$
where $A_k$ and $A_{k+1}$ are the adjacent anchors bounding the task's requested start time, and $B$ is the inter-task rest buffer. If this inequality cannot be satisfied, the task is immediately diverted to the overflow pool. Timetable leaping across anchor walls is mathematically forbidden.

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
- **Alarms:** `AlarmManager.setAlarmClock()` requires `SCHEDULE_EXACT_ALARM` or `USE_EXACT_ALARM`.
- **DND Automation:** Requires user to grant Notification Policy Access (`ACCESS_NOTIFICATION_POLICY`).
- **Secrets:** Managed via Secrets Gradle Plugin through `.env.example`. No API keys are required for core operations.
