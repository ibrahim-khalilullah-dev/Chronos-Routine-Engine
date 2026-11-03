<div align="center">

# ⚡ Chronos: Smart Dynamic Routine & Device Automation Engine

**Circadian Anchors • Auto-Balancing Fluid Twins • Hardware DND & Lock-Screen TTS • Samsung One UI 8.5 Ergonomics**

<br/>

<a href="https://raw.githubusercontent.com/ibrahim-khalilullah-dev/Chronos-Routine-Engine/main/chronos.apk">
  <img src="https://img.shields.io/badge/INSTALL%20CHRONOS-ANDROID%20APK-10B981?style=for-the-badge&logo=android&logoColor=white" height="54" alt="Install Chronos APK" />
</a>

<p>
  <sub>
    📲 <b>Download Links:</b> 
    <a href="https://raw.githubusercontent.com/ibrahim-khalilullah-dev/Chronos-Routine-Engine/main/chronos.apk"><b>Direct APK Download (Raw CDN)</b></a> &nbsp;•&nbsp; 
    <a href="https://github.com/ibrahim-khalilullah-dev/Chronos-Routine-Engine/raw/main/chronos.apk"><b>GitHub Mirror</b></a> &nbsp;•&nbsp; 
    <a href="chronos.apk"><b>Repo File</b></a>
  </sub>
</p>

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
| **Slot 1** | **Sleep** | 11:00 PM | 360m (6h) | **Reverse Sleep Tracker:** Automatically calculated backwards from Slot 2 wake time: `((Slot 2 - Sleep + 1440) % 1440)`. DND active (0m delay). Voice: *"ঘুমানোর সময় হয়েছে।"* |
| **Slot 2** | **Fajr** | 05:00 AM | 35m | Circadian metabolic baseline & morning wakefulness anchor. 8m DND delay timer. Voice: *"ফজরের সময় হয়েছে।"* |
| **Slot 3** | **Dhuhr** | 01:15 PM | 35m | Midday cognitive peak boundary. 8m DND delay timer. Features **Friday Jumu'ah Dual Mode**. Voice: *"যোহরের সময় হয়েছে।"* |
| **Slot 4** | **Asr** | 04:30 PM | 25m | Afternoon restoration anchor defining the upper bound of Slot 5. 10m DND delay timer. Voice: *"আসরের সময় হয়েছে।"* |
| **Slot 5** | **Break** | 04:55 PM → 06:00 PM | 65m | **Accordion Rubber Buffer:** Dynamically absorbs drift between Slot 4 and Slot 6 with a strict 15m safety floor. 7-day weekly recurrence selector. Voice: *"বিরতির সময় হয়েছে।"* |
| **Slot 6** | **Maghrib** | 06:00 PM | 30m | Evening cognitive decompression threshold. 5m DND delay timer. Voice: *"মাগরিবের সময় হয়েছে।"* |
| **Slot 7** | **Isha** | 08:00 PM | 35m | Melatonin onset preparation and pre-sleep sanctuary. 10m DND delay timer. Voice: *"এশার সময় হয়েছে।"* |

---

### 2. Slot 3 Friday Jumu'ah Dual Mode 🕌
On Fridays, Slot 3 autonomously transforms into dedicated Jumu'ah mode without disrupting Saturday–Thursday configurations:
- **Autonomous Timing:** Custom start time (default `12:30 PM` / 750m) and expanded duration (default `2h` = 120m).
- **Dedicated Voice Annunciation:** Audio-ducked Bengali voice briefing (*"জুমার সময় হয়েছে।"*) with a 5-minute delayed DND timer.
- **Dynamic Re-alignment:** Friday dynamic tasks automatically conform around the expanded Jumu'ah window with zero schedule collision.

---

### 3. Slot 5 Accordion Elastic Buffer & Weekly Recurrence
Slot 5 functions as a self-balancing mathematical shock absorber spanning from Slot 4 End to Slot 6 Start:
- **Dynamic Recalculation:** When Slot 4 or Slot 6 moves, Slot 5 automatically stretches or contracts, absorbing daily shifts without displacing dynamic tasks.
- **Protected 15-Minute Floor:** Can never be squeezed below 15 minutes, guaranteeing decompression time.
- **7-Day Recurrence Selector:** Enable or disable Slot 5 per day. When deactivated on any designated day, **100% of the Slot 4 → Slot 6 interval is liberated** for high-leverage dynamic tasks with zero artificial buffers.

---

### 4. High Priority Anchor Wall Fence & Custom Flexibility 🛡️
- **Anchor Fence Enforcement (`isHighPriority = true`):** Confines critical tasks strictly inside the inter-anchor gap enclosing their requested start time. If space is insufficient, Chronos diverts them safely into the **Overflow Pool** rather than permitting jarring timetable jumps across anchor walls ("timetable leaping").
- **Custom Schedule Flexibility (0 to 30m):** User-configurable compression allowance enabling tasks to cleanly fit into tight circadian windows without overflowing.
- **Inter-Task Rest Buffer (0 to 30m):** Enforces a mandatory rest buffer (default 5m) between tasks to eliminate cognitive exhaustion.

---

### 5. Fluid Twin Tasks (Communicating Vessels)
Link two distinct routine sessions under a shared total duration pool:
- **Hydraulic Adjustment:** Extending Session A proportionally contracts Session B in real time without manual re-calculation (`P = D1 + D2`).
- **Anchor Wall Protection:** Both twins respect immovable anchor boundaries and maintain a strict 45-minute safety floor (`MIN_FLUID_TWIN_MINUTES = 45`).
- **Autonomous Alarms:** Each twin session maintains independent voice messages, ringtones, and notification policies.

---

### 6. 3-Mode Task Pause & Vacation Engine
Manage temporary routine suspensions without deleting configured tasks:
- **Pause for Today Only:** Suspends the task for the remainder of today; automatically reactivates tomorrow at 00:00.
- **Pause for N Days:** Enter a custom day count (e.g., 5 days); counts down and automatically re-engages on day N+1.
- **Pause Indefinitely:** Holds the task in suspension until manually resumed via a 1-tap Amber badge.
- **Dynamic Gap Liberation:** Pausing any task (including Slot 5) opens up 100% of its scheduled interval as an open gap for other tasks.

---

### 7. Quantified Self, 90-Day History & Circadian Visualizer
- **Two-Step Completion Evaluation:** Grade task execution quality from [0%] to [100%] in 25% increments (Poor, Fair, Good, Great, Masterful) with permanent 5-Star badges (★☆☆☆☆ through ★★★★★).
- **90-Day Rating History:** Task ratings persist across app restarts and task updates with rolling decay auto-purge.
- **24-Hour Circadian Donut Dial:** Interactive custom canvas dial rendering the full 1,440-minute day cycle with touch-to-inspect segment readouts.
- **Interactive Progress Accordion:** Expand any category in Settings Analytics to inspect individual task ratings, durations, and timestamps.

---

### 8. Hardware Telemetry & Samsung Deep Sleep Immunity
- **`AlarmManager.setAlarmClock` Engine:** Schedules alarms using system clock alarm architecture, granting complete immunity against Samsung One UI aggressive deep-sleep suspension and Android Doze mode.
- **Priority Do Not Disturb (DND) with 0–25m Delay Timer:** Automatically engages `NotificationManager.INTERRUPTION_FILTER_PRIORITY` and restores previous system filter status upon task completion. For anchor slots, users can configure an optional delay timer (0 to 25 mins) before DND engages.
- **Audio-Ducking Lock-Screen TTS:** Announces custom task briefings through `TextToSpeech` requesting `AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` so media playback volume ducks during briefings.
- **Multi-Tier Offline Vault:** Every mutation automatically persists to a 4-tier storage vault (`chronos_vault.json`). On a fresh installation, Chronos auto-detects the vault and restores all routines without user intervention.

---

### 9. Monospace Obsidian Bulk Terminal CLI Engine 💻
A developer-first, high-throughput monospace Obsidian terminal console with real-time syntax checking and inline context-aware ghost auto-prediction:
- **`ANCHOR` Directive:** `ANCHOR | Slot(1-7) | Title | Start(HH:mm) | Dur(m) | DND(Y/N) | Delay(0-25) | Voice(Y/N) | Message` (plus Friday overrides for Slot 3).
- **`TWIN` Directive:** `TWIN | TotalPool(m) | P1Title | P1Start | P1Dur | P1Voice | P2Title | P2Start | P2Dur | P2Voice | DND(Y/N) | Days | Flex | Rest`.
- **`DYNAMIC` Directive:** `Task Name | Dur(m) | Start(HH:mm) | DND(Y/N) | Days | Voice(Y/N) | Message | Flex(0/15/30) | Rest(0-30)`.
- **Zero Dummy Data:** Starts with an unconfigured, pristine canvas. 1-tap "Template" provides instant reference syntax, and 1-tap "Export" exports the current routine to CLI format.

---

## 🛠️ Technology Stack

| Layer | Technology | Rationale & Responsibility |
| :--- | :--- | :--- |
| **Language** | Kotlin 2.0.21 | Modern type-safe language with coroutines and immutable data structures |
| **UI Framework** | Jetpack Compose (M3 1.4.0) | Declarative UI following Samsung One UI 8.5 ergonomics and AMOLED palette |
| **Concurrency** | Coroutines & Mutex | Thread-safe async dispatch and serialized scheduling execution via `Mutex` |
| **Reactive State** | StateFlow & collectAsState | Unidirectional Data Flow (UDF) delivering instantaneous screen updates |
| **Local Storage** | SharedPreferences & Multi-Tier Vault | Rapid memory-mapped JSON storage backed by 4-tier disk file persistence |
| **Background Work** | Foreground Service (`mediaPlayback`) | Uninterrupted execution during voice briefings and DND transitions |
| **Alarm Precision** | `AlarmManager.setAlarmClock()` | Infallible wakeups immune to OEM battery optimizations and Doze suppression |
| **Audio Telemetry** | `TextToSpeech` & `AudioManager` | Audio-ducked voice notifications with localized multilingual speech support |
| **Testing** | Robolectric 4.14.1 & JUnit | Fast JVM-level integration and Critical User Journey (CUJ) testing |

---

## 🚀 Building & Testing from Source

### Prerequisites
- JDK 17 or JDK 21 (Temurin recommended)
- Android SDK 36 (Build Tools 36.0.0)
- Gradle 8.9+ / Gradle 9.x

### Build Commands

```bash
# Clone the repository
git clone https://github.com/ibrahim-khalilullah-dev/Chronos-Routine-Engine.git
cd Chronos-Routine-Engine

# Run comprehensive Robolectric unit tests
gradle :app:testDebugUnitTest

# Assemble the debug APK
gradle :app:assembleDebug
```

The compiled APK will be generated at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 System & Device Specifications

- **Minimum SDK:** Android 8.0 (API Level 24)
- **Target / Tested SDK:** Android 15 / 16 (API Level 35 & 36 Ready)
- **Runtime Memory Footprint:** < 65MB RAM (zero background leaks, zero memory polling)
- **Primary Reference Hardware:** Samsung Galaxy A24 (SM-A245N), MediaTek Helio G99, 4GB RAM, One UI 6.x / 7.x / 8.5

---

## 📄 License

Licensed under the Apache License, Version 2.0. Copyright 2026 Ibrahim Khalilullah.
See [LICENSE](LICENSE) for full details.
