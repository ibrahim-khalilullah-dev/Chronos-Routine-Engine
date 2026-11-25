<div align="center">

# ⚡ Chronos: Smart Dynamic Routine & Device Automation Engine

**Circadian Anchors • Auto-Balancing Fluid Twins • Hardware DND & Lock-Screen TTS • Samsung One UI 8.5 Ergonomics**

<div align="center">
  <br/>
  <a href="https://raw.githubusercontent.com/ibrahim-khalilullah-dev/Chronos-Routine-Engine/main/chronos.apk" download="chronos.apk">
    <img src="https://img.shields.io/badge/Download-APK%20(Direct%20Install)-10B981?style=for-the-badge&logo=android&logoColor=white&labelColor=0B0E14" height="54" alt="Download Chronos APK" style="border-radius: 20px; box-shadow: 0 4px 14px rgba(16, 185, 129, 0.4);" />
  </a>
  <p><sub>v1.5 • Direct one-tap APK installation • Android 7.0 to 16 • 100% Offline</sub></p>
  <br/>
</div>

*Engineered specifically for low-overhead, zero-polling execution on memory-constrained devices (Samsung Galaxy A24 / MediaTek Helio G99, 4GB RAM) with complete immunity against aggressive OEM battery killer heuristics.*

</div>

---

## 📖 Executive Summary

**Chronos** is a high-precision, offline-first mathematical routine scheduler and hardware telemetry automation platform built natively in Kotlin and Jetpack Compose. Unlike conventional todo lists or calendar apps that permit unrealistic task overlapping and schedule drift, Chronos structures daily life around **biological circadian boundaries**, **zero-encroachment mathematical anchors**, **hydraulic communicating duration pools**, and **deep Android hardware integrations** (Do Not Disturb suppression, system alarm clock scheduling, audio-ducking Text-to-Speech announcements, and dynamic foreground services).

The user interface adheres strictly to **Samsung One UI 8.5 ergonomics**: a pure **AMOLED Obsidian Dark Palette (`#0B0E14`)**, 20dp smooth squircles, subtle hairline borders (`0x1AFFFFFF`), and a thumb-friendly 60/40 reach ratio designed for effortless one-handed operation.

---

## 🏛️ Core Architectural Invariants & Features

### 1. The 7 Immutable Anchors & Reverse Sleep Arithmetic
Chronos anchors human circadian rhythms across 7 immovable daily boundaries. Dynamic tasks wrap around these slots and will **never** overwrite, compress, or push them:

| Slot | Anchor Name | Default Time | Duration | Hardware Automations & Circadian Function |
| :---: | :--- | :---: | :---: | :--- |
| **Slot 1** | **Sleep** | 11:00 PM | 360m (6h) | **Reverse Sleep Tracker:** Automatically calculated backwards from Slot 2 wake time: `((Slot 2 - Sleep + 1440) % 1440)`. DND active (0m delay; customizable 0–25m). Voice: *"ঘুমানোর সময় হয়েছে।"* |
| **Slot 2** | **Fajr** | 05:00 AM | 35m | Circadian metabolic baseline & morning wakefulness anchor. 8m DND delay timer. Voice: *"ফজরের সময় হয়েছে।"* |
| **Slot 3** | **Dhuhr** | 01:15 PM | 35m | Midday cognitive peak boundary. 8m DND delay timer. Features **Friday Jumu'ah Dual Mode**. Voice: *"যোহরের সময় হয়েছে।"* |
| **Slot 4** | **Asr** | 04:30 PM | 25m | Afternoon restoration anchor defining the upper bound of Slot 5. 10m DND delay timer. Voice: *"আসরের সময় হয়েছে।"* |
| **Slot 5** | **Break** | 04:55 PM → 06:00 PM | 65m | **Accordion Rubber Buffer:** Dynamically absorbs drift between Slot 4 and Slot 6 with a strict 15m safety floor, full hardware alerts/ringtones, customizable 0–25m DND delay, and 7-day recurrence selector. Voice: *"বিরতির সময় হয়েছে।"* |
| **Slot 6** | **Maghrib** | 06:00 PM | 30m | Evening cognitive decompression threshold. 5m DND delay timer. Voice: *"মাগরিবের সময় হয়েছে।"* |
| **Slot 7** | **Isha** | 08:00 PM | 35m | Melatonin onset preparation and pre-sleep sanctuary. 10m DND delay timer. Voice: *"এশার সময় হয়েছে।"* |

*All 7 Anchors—including Slot 1 (Sleep) and Slot 5 (Elastic Buffer)—support configurable `0–25m` Auto DND Delay timers.*

---

### 2. Slot 3 Friday Jumu'ah Dual Mode 🕌
On Fridays, Slot 3 autonomously transforms into dedicated Jumu'ah mode without disrupting Saturday–Thursday configurations:
- **Autonomous Timing & Lockscreen Accuracy:** Custom start time (default `12:30 PM` / 750m), expanded duration (default `2h` = 120m), and accurate Friday duration & end-time rendering on lock-screen notifications (`Ends at 02:30 PM`).
- **Dedicated Voice Annunciation & Honored DND Delay:** Audio-ducked Bengali voice briefing (*"জুমার সময় হয়েছে।"*) while strictly honoring the user's configured DND delay (`0–25m`).
- **Dynamic Re-alignment:** Friday dynamic tasks automatically conform around the expanded Jumu'ah window with zero schedule collision.

---

### 3. Slot 5 Accordion Elastic Buffer, Hardware Alerts & Weekly Recurrence
Slot 5 functions as a self-balancing mathematical shock absorber spanning from Slot 4 End to Slot 6 Start:
- **Dynamic Recalculation:** When Slot 4 or Slot 6 moves, Slot 5 automatically stretches or contracts, absorbing daily shifts without displacing dynamic tasks.
- **Protected 15-Minute Floor:** Can never be squeezed below 15 minutes, guaranteeing decompression time.
- **Full Hardware Alert Persistence:** Slot 5 persists custom phone ringtones, sound alert & voice toggles, voice volume, haptic pulse, and `0–25m` Auto DND Delay seamlessly.
- **7-Day Recurrence Selector:** Enable or disable Slot 5 per day. When deactivated on any designated day, **100% of the Slot 4 → Slot 6 interval is liberated** for high-leverage dynamic tasks with zero artificial buffers.

---

### 4. High Priority Anchor Wall Fence & Custom Flexibility 🛡️
- **High Priority Anchor Wall Fence (`isHighPriority = true`):** Confines critical tasks strictly inside the inter-anchor gap enclosing their requested start time. If space is insufficient, Chronos diverts them safely into the **Overflow Pool** rather than permitting jarring timetable jumps across anchor walls ("timetable leaping").
- **Birth/Original Time Auto-Return (`originalStartTimeMinute`):** Every dynamic and Fluid Twin task remembers its original birth start time across schedule displacements and non-time edits, automatically snapping back to its preferred slot as soon as blocking tasks are removed, paused, or deleted.
- **Custom Schedule Flexibility (0 to 30m):** User-configurable compression allowance enabling tasks to cleanly fit into tight circadian windows without overflowing.
- **Inter-Task Rest Buffer (0 to 30m):** Autonomous per-task rest buffer (default 0m; `0..5m` in 1m steps, `5..30m` in 5m steps) with zero global floor override.

---

### 5. Fluid Twin Tasks & Absorption Engine 💧
Link two distinct routine sessions under a shared total duration pool (2m up to 12h, supporting `<45m` short sprints):
- **Hydraulic Communicating Vessels:** Extending Session A proportionally contracts Session B in real time without manual re-calculation (`P = D1 + D2`). Both twins respect immovable anchor boundaries, per-part rest buffers, and optional per-session custom safety floors (`fluidSafetyFloorMinutes`, `0` = unconstrained down to `1m`).
- **Fluid Twin Absorption Engine:** When deleting a twin part, users are provided two safe paths:
  1. **Delete Both Parts:** Completely purges both linked sessions into the Trash Bin.
  2. **Absorb Duration:** Absorbs the allocated pool into the surviving partner, decoupling it from the fluid link and cleanly converting it into an unconstrained standard dynamic task with the full consolidated duration.
- **Shared Controls & Autonomous Voice:** Twin pairs share Category, Flexibility, Buffer Time, Task Alerts (with Ringtone Preview), Auto DND, and Recurrence while each session maintains independent voice toggles, speech messages, and volume sliders.

---

### 6. Single-Earliest `setAlarmClock()` Promotion & Task-Owned DND (`ACTION_APPLY_DND`) 🌙
- **Zero Broadcast Storm Hardware Wakeups:** Instead of registering dozens of simultaneous `setAlarmClock()` SystemUI triggers, `AutomationManager.rescheduleAllAlarms()` promotes **only the single earliest upcoming alarm** across Start, bounded Delayed DND (`dndDelayMinutes` strictly before `endMillis`), and DND-End to `AlarmManager.setAlarmClock()`, while registering all remaining alarms across `dayOffset in -1..6` via `setExactAndAllowWhileIdle()`.
- **Async Receiver Execution (`goAsync()`):** `RoutineAlarmReceiver` and `BootReceiver` offload alarm promotion and rescheduling onto coroutine workers under `goAsync()`, preventing main-thread broadcast stalls or ANRs on low-memory devices.
- **Task-Owned DND, Stale Trigger Rejection & Reconciliation:** Persists the active DND owner task ID synchronously (`commit()`), rejects stale `ACTION_APPLY_DND` triggers when `EXTRA_TASK_END_MILLIS` has already elapsed or the task is deleted/paused, reconciles stale DND locks across `dayOffset = -1..0`, restores previous interruption filter cleanly on task end, and skips teardown when a zero-delay follow-up DND task starts immediately at boundary.

---

### 7. Sticky Top-Right Obsidian Glass Capsule & Auto-Day Transition ⏱️
- **Pinned Top-Right Countdown Pill & Scroll Scrim:** Opposite the `"Chronos"` header title (and pinned inside a sleek frosted obsidian glass capsule above a subtle `48.dp` vertical fade scrim while scrolling), a compact countdown pill ticks down to the next scheduled task or anchor—wrapping smoothly past midnight to tomorrow's earliest slot late at night (`+1440 - currentMinute`).
- **Sticky 3-Button Confirmation Bar & Auto-Dismiss:** Whenever timeline drag or compaction edits are staged, the pinned glass capsule cleanly transforms into the 3-button confirmation bar (`History`, `Apply`, `Discard`) and automatically dismisses itself if edits return the schedule to baseline. During active dragging, it auto-hides so the top live time pill owns full horizontal width.
- **Zero-Click Midnight Day-Shift:** Foreground-resumed lifecycle clock automatically shifts the inspected day at 00:00 and resets `isPausedTodayOnly` flags cleanly.

---

### 8. Alarm-Stream TTS, Per-Task Voice Sliders & Instant Hardware Voice Stop 🔔
- **Silent Notification Channel (`chronos_automation_silent_channel`):** Foreground service notifications explicitly configure `setSound(null, null)` and `enableVibration(false)` so the OS notification channel never double-chimes alongside task audio, while displaying lock-screen timing (`VISIBILITY_PUBLIC`, e.g., `Ends at 10:45 AM`) and respecting device PIN/biometric locks.
- **Strict `USAGE_ALARM` / `STREAM_ALARM` TTS Speech Routing:** ALL live Voice Announcements (`RoutineAutomationService`) and in-app TTS previews (Voice Persona Studio, Single Tasks, Fluid Twins, and all 7 Anchors) explicitly bind `AudioAttributes.USAGE_ALARM` + `CONTENT_TYPE_SPEECH`, pass `AudioManager.STREAM_ALARM`, and request transient ducking audio focus (`AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`), remaining clearly audible even when Media volume is `0%` or muted.
- **Always-On Per-Task `0%–100%` Voice Volume Sliders:** Every voice-enabled routine—Dynamic Tasks, Fluid Twin Session 1 & Session 2, and all 7 Circadian Anchors (including Slot 3 Regular & Friday Jumu'ah mode)—exposes a dedicated `0%–100%` alarm-stream voice volume slider directly inside its editor modal (`test_voice_button_volume_slider`), scaled directly against Android's Alarm channel with clean upgrade migration from legacy settings.
- **Zero-Overhead Hardware-Button Voice Stop (`Power`, `Volume Up`, `Volume Down`):** Pressing Power (`ACTION_SCREEN_OFF` / `ACTION_SCREEN_ON` with 200ms guard) or Volume Up/Down (both in-app via Activity/Dialog `dispatchKeyEvent` and on lockscreen/screen-off via scoped `VOLUME_CHANGED_ACTION` receiver & `Settings.System` observer) immediately silences ONLY active Voice Announcement playback (and companion chime), preserving active automation state, Task-Owned DND, notifications, and future alarms—with listeners registered **strictly** while voice playback is active and zero idle overhead otherwise.
- **`USAGE_ALARM` Custom Ringtone & `MediaPlayer` Fallback:** User-selected phone ringtones play directly on `AudioAttributes.USAGE_ALARM` via `RingtoneManager`, falling back seamlessly to `MediaPlayer` if a vendor URI refuses direct Ringtone playback.
- **Lazy Self-Releasing TTS & 25s WakeLock Cap:** `RoutineAutomationService` initializes `TextToSpeech(applicationContext)` strictly when `isVoiceEnabled == true`, immediately executes `stop()` and `shutdown()` inside `UtteranceProgressListener.onDone` / `onError` or a 20-second safety timeout so Google TTS (~150MB RAM) unbinds immediately, caps WakeLocks at `25_000L` (25 seconds), and runs with `START_NOT_STICKY` lifecycle safety.
- **Zero-Leak 12-Persona Bilingual TTS & Studio Previews:** Choose from 12 offline BN/EN voice profiles in Settings (`Female (5)`, `Youth 10–15y (3)`, `Male & Studio (4)`), with custom BN/EN test text, `bn-BD`/`en-US` auto-detection, Google TTS (`com.google.android.tts`) fallback, and safe `applicationContext` previews in Task Studio and Settings.

---

### 9. 3-Mode Task Pause & Vacation Engine 🌴
Manage temporary routine suspensions without deleting configured tasks:
- **Pause for Today Only:** Suspends the task for the remainder of today; automatically reactivates tomorrow at 00:00.
- **Pause for N Days:** Enter a custom day count (e.g. 5 days); counts down and automatically re-engages on day N+1.
- **Pause Indefinitely:** Holds the task in suspension until manually resumed via a 1-tap Amber badge.
- **Dynamic Gap Liberation:** Pausing any task opens up 100% of its scheduled interval as an open gap for other tasks.

---

### 10. Deep-Sleep 7-Day Perpetual Alarm Engine 🛡️
- **Overnight & Non-Overflow Scheduling:** `AutomationManager.rescheduleAllAlarms()` reconciles stale DND state and evaluates each active task across `dayOffset in -1..6` (`endMillis > now` for yesterday/today so overnight tasks like `23:00 → 05:00` retain their morning `ACTION_END_TASK` across midnight rollover), registering its earliest upcoming non-overflow occurrence in `AlarmManager`.
- **Single-Earliest System Alarm + Exact Horizon:** Promotes the earliest upcoming occurrence to `setAlarmClock()` for Doze piercing while keeping all subsequent horizon alarms on `setExactAndAllowWhileIdle()`, preventing alarm broadcast storms on 4GB RAM Samsung One UI devices.
- **Universal Clock & Boot Resynchronization:** Resynchronizes alarms asynchronously via `goAsync()` immediately upon reboot (`BOOT_COMPLETED`), application update (`MY_PACKAGE_REPLACED`), time change (`ACTION_TIME_SET`), and timezone change (`ACTION_TIMEZONE_CHANGED`).

---

### 11. 60/120 FPS Fluid Engine & 4GB RAM Performance Architecture ⚡
- **Zero Unused Background SDKs & Lifecycle-Aware Ticker:** Unused networking/cloud libraries are pruned so zero stray `ContentProvider`s spin up on cold boot or alarm wakeup, the 15-second UI clock ticker runs strictly while foregrounded (`ON_START`/`ON_STOP`), `TextToSpeech` binds lazily only upon explicit preview taps using `applicationContext`, and `activeRingtone` self-releases via a bounded 10s timeout.
- **Minimal Recomposition & Single-Canvas Cycle Map:** Rating persistence runs on `Dispatchers.IO`, the 24-Hour Cycle Map renders in a single `Canvas` pass, non-active cards skip 15s tick recompositions, and sorted interval-merging in `SchedulerEngine.findAvailableGaps()` eliminates `1440`-element per-minute allocations.
- **Off-Main-Thread Mutations & Throttled Vault Persistence:** `RoutineViewModel` executes repository saves and serialized alarm rescheduling inside `scheduleMutex` on `Dispatchers.IO`, skips redundant 4-tier `chronos_vault.json` writes when content is unchanged, and guards startup vault auto-restore.

---

### 12. Human-Level Smart Solver, Fair-Share Water-Filling & Birth-Duration Auto-Return 🧠
- **Two-Tier Constraint Solver (`SchedulerEngine.scheduleDay`):**
  - **Pass 1 (Baseline Fit):** Places High Priority, Fluid Twin, and Normal dynamic tasks at full birth duration without premature flexing or overflow.
  - **Pass 2 (4-Strategy Elastic Rescue):** Resolves blocked tasks deterministically in `< 1–3ms` (`O(N log N)`):
    1. **Cross-Anchor Smart Task Migration:** Moves smaller non-priority tasks out of blocked windows into open space in other anchor windows at full duration (respecting Fluid Twin Same-Gap Partner Exclusion).
    2. **Micro-Deficit Neighbor Borrowing (`1–5m`):** Covers small deficits from 1–2 flexible donors inside the window without shrinking the incoming task.
    3. **Collective Fair-Share Elastic Compression:** Pools available flexibility inside the window using **Largest-Remainder Water-Filling** (exhausting non-priority Tier-1 flex before touching High Priority Tier-2 tasks).
    4. **Flex-to-Shrink & Relocate:** Self-shrinks flexible tasks or shrinks-and-relocates movable neighbors within their flex margins to unlock tight slots.
- **Birth-Duration Auto-Return (`originalDurationMinutes`) & Anti-Double-Flexing:** Canonical birth durations persist across JSON vault, Console CLI, Task Studio edits, and drag previews (`⚡ Auto-flexed to Xm today`), automatically snapping tasks back to full duration when blocking tasks move or pause.
- **Transparent Telemetry Badges & 1-Tap `Compact` / `Auto-Fit`:** Timeline cards surface live `⚡ 60m→45m` flex pills and `↪ 09:00 AM` relocation markers alongside non-destructive `Compact` and `Auto-Fit` impact diff previews.

---

### 13. Corner Stopwatch Net Free-Time Gap Telemetry ⏱️
- **Right-Aligned Obsidian Pill (`EARLIEST_START`):** Between consecutive cards with open breathing room, a sleek compact capsule (`#161E2E`, `1.dp` `#334155` border) with a crisp stopwatch icon displays exact open free time (e.g., `3m`, `1h 10m`).
- **Buffer-Excluded Precision Math:** Powered by `SchedulerEngine.computeNetFreeGapMinutes()`, reserved `interTaskBufferMinutes` on dynamic tasks are cleanly subtracted from raw chronological spacing and the pill automatically hides whenever `netGapMinutes <= 0`.
- **Real-Time Live Scrub Recalculation:** During horizontal scrubbing, adjacent corner gap pills recalculate live frame-by-frame (`3m` → `2m` → `1m` → hidden at `0m`) while the surrounding list order remains locked with zero layout jumping.

---

### 14. Interactive Launcher-Grade Drag Scheduling & Schedule Impact Diff 🎯
- **Outer-Container Gesture Architecture (`PointerEventPass.Initial`):** Long-press any dynamic task card (safe tap margin preserves the top-right Edit pencil and native scrolling) to enter dual-axis drag mode:
  - **Horizontal Scrubbing:** Speed-sensitive fine scrubbing gliders move the card minute-by-minute within computed free bounds (`computeHorizontalScrubBounds`), strictly locking horizontal motion if surrounding gap is `0m`.
  - **Two-Phase Silky-Smooth Vertical Settling:** Drag vertically across the timeline with an elevated floating overlay card, `withFrameNanos`-driven velocity-scaled edge auto-scroll, dimmed dashed **Source Shadow** (`alpha = 0.28`) at origin, animated full-card-height **Destination Drop Shadow** protected by `±8dp` anti-ping-pong hysteresis, neighbor card gliding via `Modifier.animateItem`, and a two-phase settling handoff that glides the floating overlay directly onto the card's settled bounds while smoothly tapering scale (`1.025x → 1.0x`) and shadow elevation.
- **High Priority Anchor Wall vs. Fluid Twin Cross-Anchor Exclusion:** Strictly non-twin High Priority tasks clamp at enclosing **Anchor Walls** (`Anchor Wall`), whereas Fluid Twin sessions can jump freely across anchors into unoccupied windows—enforcing **Same-Gap Partner Exclusion** (`"Twin parts must stay in separate anchor gaps"`) whenever an active twin partner already occupies the destination inter-anchor gap.
- **Non-Destructive Preview & Impact Diff Modal:** Staged drag or compaction changes show the pinned 3-button review bar (`[History]` diff modal listing exact task shifts & overflows, `[Check]` Apply & reschedule alarms, `[Close]` Discard cleanly).

---

### 15. Monospace Obsidian Terminal Studio CLI Engine (v2) 💻
A developer-first, high-throughput monospace Obsidian terminal console with real-time debounced syntax parsing (250ms), active metrics diagnostic card, collapsible error accordion, and clickable inline context-aware ghost auto-prediction:
- **`ANCHOR` Directive:** `ANCHOR | Slot(1-7) | Title | Start(HH:mm) | Dur(m) | DND(Y/N) | Delay(0-25) | Voice(Y/N) | Message | [FriTitle | FriStart | FriDur | FriMsg] | [Days | Sound(Y/N) | VoiceVol(0-100)]`.
- **`TWIN` Directive:** `TWIN | TotalPool(m) | P1Title | P1Start | P1Dur | P1Voice | P2Title | P2Start | P2Dur | P2Voice | DND(Y/N) | Days | Flex | Rest | [Category | P1Safety | P2Safety | P1Voice(Y/N) | P2Voice(Y/N) | P1Vol | P2Vol | Sound | DndDelay(0-25)]`.
- **`DYNAMIC` Directive (v2):** `Task Name | Dur(m) | Start(HH:mm) | DND(Y/N) | Days | Voice(Y/N) | Message | Flex(0-30) | Rest(0-30) | Priority(Y/N) | Category | [VoiceVol(0-100) | Sound(Y/N) | DndDelay(0-25)]`.
- **Spacious 540dp Studio Layout & Clean Headerless Backup:** High-contrast AMOLED obsidian dark styling (`#0B0E14`), hairline border (`#1E293B`), synchronized line-numbered gutter (24dp), and a header-integrated **"Backup"** button that exports purely live `ANCHOR`, `TWIN`, and dynamic lines using each task's birth start time (zero `#` comments or blank separator lines).
- **One UI Active Metrics Row:** Real-time diagnostic cards tracking `Total Tasks`, `Anchors`, `Priority`, and `Syntax Status (Valid/Errors)`.
- **Collapsible Error Accordion:** Clean chip-style error reporting preventing layout explosion during active typing.
- **Dual Schedule Actions:** Gradient Cyan-to-Indigo `[ Append Tasks ]` and Gradient Amber-to-Red `[ Replace All ]`.

---

### 16. Unified Obsidian Task Studio & Dual-Session Voice Architecture 🎙️
- **Unified Luxury Parity Across All Modes:** Normal Tasks, Fluid Twin Split sessions, and all 7 Circadian Anchors share one cohesive Samsung One UI 8.5 Obsidian Studio aesthetic (`#101727` → `#0A0E18` glass gradient cards, `#070B13` recessed time/duration wells, glowing Material 3 custom `16.dp` circular slider thumbs, `36.dp` stepper badges, and streamlined Slot 1 / Slot 5 editors free of redundant description clutter).
- **Header Quick-Pill Ergonomics & Mutual Exclusivity:** Direct header-integrated `Priority` and `Split` interactive toggle pills use `AnimatedVisibility` mutual exclusivity during task creation—enabling `Priority` smoothly hides `Split`, and engaging `Split` cleanly hides `Priority`—while editing an existing single task hides `Split` and keeps `Priority` editable.
- **Compact 3-Box Matrix, `00` Defaults & Anti-Clipping Stepper Wells:** Uniform `START TIME`, `HOURS`, `MINS`, and `SAFETY` wells use `widthIn`-constrained `BasicTextField` inputs (`00` placeholders) with external `h`/`m`/`±` unit suffixes so symbols never clip, `"Born: <time>"` markers when displaced, a `"ENDING TIME"` (or `"TARGET SLEEP QUOTA"` for Slot 1) summary row, and a clean single-checkmark Save CTA.
- **Unified Hardware Grid, `0–25m` DND Delay & Ringtone Preview:** `Category` selector, `FLEXIBLE` & `BUFFER TIME` row, `TASK ALERTS` (with Play/Stop ringtone preview), `AUTO DND` (with `0–25m` Delay stepper across Single Dynamic Tasks, Split Twins, and all 7 Anchors), and `RECURRENCE SCHEDULE` render uniformly across all modes.
- **Full Dual-Session Voice Studio & Split Typing Safety:** Both Session 1 and Session 2 inside Split mode feature independent voice ON/OFF switches, custom per-session Safety Floors, collapsible speech message inputs, interactive Play/Stop TTS preview toggles synced to the active Settings Voice Persona, and 0%–100% volume sliders, while preserving manual empty/zero editing without jarring `pool / 2` auto-fills or mid-keystroke snap-backs.
- **Zero Auto-Splitting Architecture:** Tasks fit intact within available circadian gaps or cleanly route to the Overflow Pool without unexpected fragmentation.

---

## 🛠️ Technology Stack

| Layer | Technology | Rationale & Responsibility |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.2.10 | Modern type-safe language with coroutines and immutable data structures |
| **Build System** | AGP 9.1.1 (Gradle Kotlin DSL) | Android 16 (API 36) compilation toolchain with Compose & KSP |
| **UI Framework** | Jetpack Compose (BOM 2024.09.00 / M3 1.4.0) | Declarative UI following Samsung One UI 8.5 ergonomics and AMOLED palette |
| **Concurrency** | Coroutines & Mutex | Thread-safe async dispatch (`Dispatchers.IO`/`Default`) and serialized `scheduleMutex` execution |
| **Reactive State** | StateFlow & collectAsState | Unidirectional Data Flow (UDF) delivering instantaneous screen updates |
| **Local Storage** | SharedPreferences & Multi-Tier Vault | Memory-mapped JSON storage backed by 400ms debounced 4-tier `chronos_vault.json` |
| **Background Work** | Foreground Service (`START_NOT_STICKY`) | Uninterrupted execution with 25s WakeLock cap and self-releasing TTS |
| **Alarm Precision** | `setAlarmClock()` + `setExactAndAllowWhileIdle()` | Single-earliest Doze-piercing wakeups + 7-day horizon without broadcast storms |
| **Audio Telemetry** | Lazy `TextToSpeech` & `RingtoneManager` | Strict `USAGE_ALARM` / `STREAM_ALARM` speech routing, per-task `0%–100%` voice volume sliders, hardware-key stop, 12 BN/EN personas |
| **Testing** | Robolectric 4.16.1 & JUnit | Fast JVM-level Compose UI, drag/drop math, and CUJ integration testing |

---

## 🚀 Building & Testing from Source

### Prerequisites
- JDK 17 or JDK 21 (Temurin recommended)
- Android SDK 36 (Build Tools 36.0.0)
- Gradle 8.9+ / Gradle 9.x

### Build & Release Synchronization Commands

```bash
# Clone the repository
git clone https://github.com/ibrahim-khalilullah-dev/Chronos-Routine-Engine.git
cd Chronos-Routine-Engine

# Run comprehensive Robolectric unit tests
gradle :app:testDebugUnitTest

# Assemble and synchronize root APK for mobile direct downloads
gradle :app:assembleDebug && cp app/build/outputs/apk/debug/app-debug.apk chronos.apk
git add chronos.apk README.md && git commit -m "release: update chronos.apk and documentation" && git push
```

The compiled APK will be generated and synchronized at:
```text
chronos.apk
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 System & Device Specifications

- **App Release Version:** v1.5 (`versionCode = 6`)
- **Minimum SDK:** Android 7.0 (API Level 24)
- **Target / Tested SDK:** Android 15 / 16 (API Level 35 & 36 Ready)
- **Runtime Memory Footprint:** < 65MB RAM (lazy self-releasing TTS, single-earliest `setAlarmClock()`, zero background leaks)
- **Primary Reference Hardware:** Samsung Galaxy A24 (SM-A245N), MediaTek Helio G99, 4GB RAM, One UI 6.x / 7.x / 8.5

---

## 📄 License

Licensed under the Apache License, Version 2.0. Copyright 2026 Ibrahim Khalilullah.
See [LICENSE](LICENSE) for full details.
