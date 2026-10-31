<div align="center">

# ⚡ Chronos: Smart Dynamic Routine & Device Automation Engine

**Circadian Anchors • Auto-Balancing Fluid Twins • Hardware DND & Lock-Screen TTS • Samsung One UI 8.5 Aesthetic**

<br/>

<a href="https://github.com/ibrahim-khalilullah-dev/Chronos-Routine-Engine/raw/main/chronos.apk">
  <img src="https://img.shields.io/badge/INSTALL%20CHRONOS-ANDROID%20APK-10B981?style=for-the-badge&logo=android&logoColor=white" height="54" alt="Install Chronos APK" />
</a>
<p><sub>Tap the button above to instantly download <b>chronos.apk</b> directly to your phone.</sub></p>

<br/>

[![Android 14/15/16 Ready](https://img.shields.io/badge/Android-API%2034%20%7C%2035%20%7C%2036-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin 2.0](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-M3%201.4.0-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%7C%20Room%20%7C%20MVI%2FMVVM-000000?style=flat-square)](https://developer.android.com/topic/architecture)
[![100% Offline & Private](https://img.shields.io/badge/Privacy-100%25%20Offline%20%26%20Zero%20Analytics-EF4444?style=flat-square)](https://github.com/ibrahim-khalilullah-dev/Chronos-Routine-Engine)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=flat-square)](LICENSE)

<br/>

*Engineered specifically for low-overhead, zero-polling execution on memory-constrained devices (Samsung Galaxy A24 / MediaTek Helio G99, 4GB RAM).*

</div>

---

## 📖 Overview

**Chronos** (ক্রোনস) is an ultra-precise, offline mathematical routine engine and hardware automation platform designed around circadian biology and cognitive peak management. It enforces **mathematical non-encroachment boundaries**, **hydraulic communicating duration pools**, and **system-level hardware telemetry automations** (Do Not Disturb suppression, exact alarm scheduling, audio-ducking Text-to-Speech announcements, and dynamic foreground notifications).

The user interface adheres to **Samsung One UI 8.5 ergonomics**: a pure **AMOLED Obsidian Dark Palette (`#080A0F`)**, 20dp smooth squircles, hairline structure borders (`#1F2637`), and a comfortable 60/40 thumb-interaction reach ratio.

---

## 🏛️ Core Architectural Pillars

### 1. The 7 Immutable Anchors & Reverse Sleep Math
Chronos anchors human circadian rhythms across 7 immovable daily boundaries. Dynamic tasks conform and wrap around these slots and will **never** overwrite, compress, or push them:

| Slot | Anchor Name | Default Time | Duration | Circadian Function |
| :---: | :--- | :---: | :---: | :--- |
| **Slot 1** | **Reverse Sleep Tracker** | 10:00 PM | 420m (7h) | Backward modulo calculation (`% 1440`) derived from Slot 2 wake time. |
| **Slot 2** | **Morning Routine (Fajr)** | 05:00 AM | 30m | Circadian metabolic baseline & morning wakefulness anchor. |
| **Slot 3** | **Afternoon Focus (Dhuhr)** | 01:15 PM | 30m | Midday cognitive focus boundary. Features Friday Jumu'ah Dual Mode. |
| **Slot 4** | **Late Afternoon Reset (Asr)** | 04:30 PM | 30m | Afternoon restoration anchor defining the upper bound of Slot 5. |
| **Slot 5** | **Elastic Rubber Buffer** | 05:00 PM → 06:00 PM | 60m | Dynamic shock absorber bridging Slot 4 and Slot 6 with a 15m safety floor. |
| **Slot 6** | **Evening Wind-Down (Maghrib)** | 06:00 PM | 30m | Evening cognitive decompression and family rhythm threshold. |
| **Slot 7** | **Night Reflection (Isha)** | 08:00 PM | 30m | Melatonin onset preparation and pre-sleep sanctuary. |

### 2. Slot 3 Friday Jumu'ah Dual Mode 🕌
On Fridays, Slot 3 seamlessly switches into dedicated Jumu'ah mode:
- **Autonomous Timing:** Custom start time (default `11:30 AM`) and expanded duration (default `2h 30m`).
- **Autonomous Voice Annunciation:** Custom speech announcement (*"Friday Jumu'ah prayer and spiritual reflection."*) with audio ducking.
- **Dynamic Re-alignment:** Friday dynamic tasks automatically conform around the extended Jumu'ah window.

### 3. Slot 5 Accordion Elastic Buffer & Weekly Recurrence
Slot 5 acts as a self-balancing shock absorber bridging Slot 4 and Slot 6:
- **Mathematical Sizing:** Dynamically resizes when Slot 4 or Slot 6 moves, maintaining a strict 15-minute safety floor.
- **7-Day Recurrence Selector:** When deactivated on any specific day, **100% of the Slot 4 → Slot 6 interval is liberated** for dynamic tasks with zero artificial buffers.

### 4. Fluid Twin Tasks (Communicating Vessels)
Link two distinct routine sessions under a unified duration pool:
- **Hydraulic Adjustment:** Extending Session A proportionally contracts Session B in real time without manual re-calculation.
- **Anchor Wall Protection:** Both twins respect immovable anchor boundaries and enforce a 45-minute safety floor.

### 5. 3-Mode Task Pause & Vacation Engine (In Engine Settings)
Manage temporary routine suspensions without deleting tasks:
- **Pause for Today Only:** Temporarily deactivates the task for the remainder of today; auto-resumes at midnight.
- **Pause for N Days:** Enter a custom day count (e.g. 5 days); automatically counts down and re-engages on day N+1.
- **Pause Indefinitely:** Holds the task in suspension until manually resumed via a 1-tap Amber badge.
- **Dynamic Gap Liberation:** Pausing any task (including Slot 5) opens up 100% of its scheduled interval as an open gap for other tasks.

### 6. Quantified Self, 90-Day History & Circadian Visualizer
- **Two-Step Completion Evaluation:** Grade task quality from [0%] to [100%] in 25% increments with permanent 5-Star badges.
- **90-Day Rating History:** Historical ratings persist across restarts and task modifications for 3 months with automatic expiration.
- **24-Hour Circadian Donut Dial:** Interactive SVG canvas dial displaying today's 1,440-minute distribution with tap-to-inspect segment readouts.
- **Interactive Progress Accordion:** Expand any category in progress view to inspect individual task ratings, durations, and timestamps.

### 7. Samsung Deep Sleep Immunity & Hardware Automations
- **AlarmManager.setAlarmClock Engine:** Alarms are scheduled via system clock alarm architecture, granting complete immunity against Samsung One UI aggressive deep-sleep suspension and Doze suppression.
- **Priority Do Not Disturb (DND):** Automatically switches system interruption filters on task start and restores original status upon completion.
- **Audio-Ducking Lock-Screen TTS:** Announces custom task briefings through TextToSpeech with transient audio focus ducking.
- **Offline Storage Vault:** Multi-tier automated snapshots restore routines with zero clicks upon fresh reinstallation.

---

## 🛠️ Tech Stack

- **Language:** 100% Kotlin (Kotlin 2.0.21)
- **UI Framework:** Jetpack Compose (Material 3 v1.4.0, Samsung One UI 8.5)
- **Local Persistence:** Room Database (SQLite), SharedPreferences, Multi-tier Offline JSON Vault
- **Reactive State:** Kotlin Coroutines, StateFlow, collectAsState
- **Background Execution:** Android Foreground Services, AlarmManager (setAlarmClock), Broadcast Receivers
- **Audio & Telemetry:** Android TextToSpeech, RingtoneManager, AudioManager (Transient Ducking), NotificationManager

---

## 🚀 Building from Source

Clone the repository and build using Gradle:

```bash
git clone https://github.com/ibrahim-khalilullah-dev/Chronos-Routine-Engine.git
cd Chronos-Routine-Engine
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

The compiled APK will be located at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 Hardware & OS Requirements

- **Minimum SDK:** Android 8.0 (API Level 26)
- **Target / Tested SDK:** Android 15 / 16 (API Level 35 / 36 Ready)
- **Memory Footprint:** < 65MB runtime memory footprint (zero background leaks)
- **Tested Reference Device:** Samsung Galaxy A24 (SM-A245N), One UI 6.x / 7.x / 8.5

---

## 📄 License

Apache License 2.0. Copyright 2026 Ibrahim Khalilullah.
