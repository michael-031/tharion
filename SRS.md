<div align="center">

### Version Control

---

# Eltharion
## Software Requirements Specification
### Version 01.00

</div>

---

| | |
| :--- | :--- |
| **Eltharion** | **Version:** 1.00 |
| **Software Requirements Specification** | **Date:** 11/15/2026 |
| **Document Identifier:** SRS-ELTHARION-01.00 | **Platform:** Android (Kotlin / Jetpack Compose) |

## Revision History

| Date | Version | Description | Author |
| :--- | :--- | :--- | :--- |
| 11/15/2026 | 01.00 | Document Creation | Eltharion Engineering |

---

## Table of Contents

- [1. Introduction](#1-introduction)
  - [1.1 Purpose](#11-purpose)
  - [1.2 Scope](#12-scope)
  - [1.3 Definitions, Acronyms, and Abbreviations](#13-definitions-acronyms-and-abbreviations)
  - [1.4 References](#14-references)
  - [1.5 Overview](#15-overview)
- [2. Overall Description](#2-overall-description)
- [3. Specific Requirements](#3-specific-requirements)
  - [3.1 Functionality](#31-functionality)
    - [3.1.1 Background RSSI Signal Guard & Two-Strike Auto-Pause/Resume](#311-background-rssi-signal-guard--two-strike-auto-pauseresume)
    - [3.1.2 Dual-Method Portal Communication & Smart Retry](#312-dual-method-portal-communication--smart-retry)
    - [3.1.3 Pause Expiration Tracking, Alerts & Background Priority](#313-pause-expiration-tracking-alerts--background-priority)
    - [3.1.4 Live Signal Visualization & Practice Mode](#314-live-signal-visualization--practice-mode)
    - [3.1.5 Monthly Time-Saved Tracking](#315-monthly-time-saved-tracking)
    - [3.1.6 Accessibility Modes & Quick Settings Tile Control](#316-accessibility-modes--quick-settings-tile-control)
  - [3.2 Usability](#32-usability)
    - [3.2.1 Adaptive Bottom Navigation & Navigation Rail](#321-adaptive-bottom-navigation--navigation-rail)
    - [3.2.2 High-Contrast Matte Monochrome Presentation](#322-high-contrast-matte-monochrome-presentation)
    - [3.2.3 Simple Mode & Beginner-Friendly Controls](#323-simple-mode--beginner-friendly-controls)
  - [3.3 Reliability & Availability](#33-reliability--availability)
    - [3.3.1 Persistent Foreground Service & Battery Priority](#331-persistent-foreground-service--battery-priority)
    - [3.3.2 Local-Only Preference & Monthly Savings Persistence](#332-local-only-preference--monthly-savings-persistence)
  - [3.4 Performance](#34-performance)
  - [3.5 Security & Privacy](#35-security--privacy)
    - [3.5.1 Local Storage and Session Integrity](#351-local-storage-and-session-integrity)
    - [3.5.2 Least-Privilege Permissions & WiFi Network Binding](#352-least-privilege-permissions--wifi-network-binding)
  - [3.6 Design Constraints](#36-design-constraints)
    - [3.6.1 Native Android Toolchain & Adaptive Layout Constraints](#361-native-android-toolchain--adaptive-layout-constraints)
    - [3.6.2 Android Foreground Location & Background Service Compliance](#362-android-foreground-location--background-service-compliance)
  - [3.7 Interfaces](#37-interfaces)
    - [3.7.1 Captive Portal HTTP & DOM Interface](#371-captive-portal-http--dom-interface)
    - [3.7.2 Android WiFi & Network Binding Interface](#372-android-wifi--network-binding-interface)
    - [3.7.3 Android Quick Settings, Notification & Power Management Interface](#373-android-quick-settings-notification--power-management-interface)
    - [3.7.4 Device Haptics & Local Persistence Interface](#374-device-haptics--local-persistence-interface)

---

# Software Requirements Specification

## 1. Introduction

Public coin-operated hotspots such as LPB Piso WiFi vendos grant paid internet access measured strictly in countdown minutes. When users walk out of signal range, lock their phones, or forget to manually tap **Pause Time** on the captive portal (`http://10.0.0.1`), their remaining paid minutes continue counting down and go to waste. Manual pausing also requires opening a browser tab and navigating the portal UI every time the user steps away.

In order to prevent wasted hotspot time without interrupting daily smartphone usage, **Eltharion** is proposed as a native Android utility that continuously monitors WiFi signal strength (RSSI) in the background, automatically pauses and resumes the user's captive-portal session when signal drops or recovers, tracks paused time saved locally, and provides one-tap access directly from Android's Quick Settings shade.

### 1.1 Purpose

The purpose of this document is to provide a detailed specification of the requirements governing the design and implementation of the **Eltharion** Android application (`com.aistudio.lpbguard.kqwmzx`). Throughout this document, the general product perspective is presented alongside the functional, usability, reliability, performance, security, design, and external interface requirements.

The subsequent sections specify the core subsystems of Eltharion—including the foreground RSSI polling service, the two-strike debounce guard engine, the dual-method portal controller with exponential backoff smart retry, pause expiration notifications, local-only monthly savings analytics, Simple Mode, Practice Mode, and the Android Quick Settings Tile.

### 1.2 Scope

This document covers the functional, non-functional, interface, and design requirements for **Eltharion (Version 1.0)**. The application runs natively on Android devices (Android 7.0 Nougat / API 24 through Android 16 / API 36) and is built primarily for LPB Piso WiFi (`http://10.0.0.1`) users as well as compatible local captive-portal WiFi systems whose gateway URL and HTML/DOM pause-resume actions can be configured by the user. All telemetry, settings, and monthly savings statistics remain strictly on the local device with zero cloud backend dependency.

### 1.3 Definitions, Acronyms, and Abbreviations

| Term / Acronym | Definition |
| :--- | :--- |
| **RSSI** | Received Signal Strength Indicator, measured in decibel-milliwatts (`dBm`) to represent WiFi signal quality. |
| **LPB Piso WiFi** | Common Philippine coin-operated captive-portal WiFi vendo system, typically hosted locally at `http://10.0.0.1`. |
| **Two-Strike Debounce** | Signal evaluation rule requiring two consecutive RSSI readings beyond a threshold before triggering a portal action, preventing single-packet jitter false triggers. |
| **FGS** | Android Foreground Service (`WifiGuardService`) with persistent ongoing notification and `location` foreground service type. |
| **Method A / Method B** | Dual portal control pipeline: **Method A** uses WiFi-bound HTTP GET + regex HTML link parsing; **Method B** uses a hidden `1x1` `WebView` DOM/JavaScript click fallback. |
| **Smart Retry** | Exponential backoff cooldown policy (`4s -> 8s -> 16s -> 32s -> 64s -> 120s` cap) applied when the portal gateway is unreachable. |
| **QS Tile** | Android Quick Settings Tile (`EltharionTileService`) pinned in the system notification shade for toggling protection without opening the app. |
| **DataStore** | Jetpack Preferences DataStore used for strictly local on-device persistence of user settings and monthly savings summaries. |

### 1.4 References

- Eltharion Android Application Source Code (`com.aistudio.lpbguard.kqwmzx`)
- Android Developers Documentation: Foreground Services (`FOREGROUND_SERVICE_TYPE_LOCATION`), Quick Settings Tiles (`TileService`), `ConnectivityManager`, and `WifiManager`
- Jetpack Compose & Material Design 3 Guidelines

### 1.5 Overview

This document outlines the problem of unpaused Piso WiFi timer loss and details the engineering architecture implemented in Eltharion to solve it cleanly for both everyday non-technical users and power users. Section 2 summarizes the overall product behavior and operating environment, while Section 3 enumerates specific functional, quality, design, and hardware/system interface requirements.

---

## 2. Overall Description

**Eltharion** is a native Android Kotlin and Jetpack Compose mobile application engineered to protect paid Piso WiFi minutes automatically and offline-first. When activated—either from the main dashboard, **Simple Mode**, or the **Eltharion Quick Settings Tile**—a foreground monitoring service (`WifiGuardService`) reads live WiFi RSSI every two seconds and feeds each reading into a stateful debounce engine (`RssiGuardEngine`).

When signal strength stays at or below the user's configured pause distance threshold for two consecutive readings, Eltharion binds its network traffic directly to the active WiFi network interface (`WifiNetworkBinder`) and invokes `LpbPortalController` to pause the user's session via fast HTTP HTML parsing (**Method A**) or an invisible `WebView` DOM injector (**Method B**). If the captive-portal endpoint (`10.0.0.1`) is unreachable, `PortalRetryPolicy` enforces exponential backoff so the background service does not drain battery on repeated failed network attempts. While paused, Eltharion tracks remaining pause allowance, delivers an urgent five-minute expiration alert, records total time saved per calendar month locally in DataStore, and supports optional Auto-Resume when the user walks back into strong signal range.

---

## 3. Specific Requirements

### 3.1 Functionality

#### 3.1.1 Background RSSI Signal Guard & Two-Strike Auto-Pause/Resume

##### 3.1.1.1 Foreground Signal Polling
**3.1.1.1.1** The system shall run a persistent Foreground Service (`WifiGuardService`) that samples the device's live WiFi SSID, BSSID, connection status, and RSSI (`dBm`) every 2,000 ms.

##### 3.1.1.2 Two-Strike Debounce Evaluation
**3.1.1.2.1** The signal engine (`RssiGuardEngine`) shall require two consecutive RSSI readings meeting the pause or resume threshold before issuing a state-change trigger, resetting strike counters immediately if signal recovers during intermediate samples.

##### 3.1.1.3 Automatic Session Pause
**3.1.1.3.1** Upon two consecutive readings at or below the configured pause threshold (`pauseThresholdDbm`, default `-75 dBm`) while the session is `ACTIVE`, the system shall trigger an automated portal **Pause** command and provide double-pulse haptic feedback on success.

##### 3.1.1.4 Optional Auto-Resume
**3.1.1.4.1** When `autoResumeEnabled` is enabled and the session is `PAUSED`, two consecutive readings at or above the resume threshold (`resumeThresholdDbm`, default `-65 dBm`) shall trigger an automated portal **Resume** command and provide single-pulse haptic feedback.

#### 3.1.2 Dual-Method Portal Communication & Smart Retry

##### 3.1.2.1 Method A: WiFi-Bound HTTP & Regex Parsing
**3.1.2.1.1** The portal controller (`LpbPortalController`) shall bind requests to the active `Network` (`ConnectivityManager`), fetch the portal HTML with a 2.5-second timeout, extract dynamic session action URLs (`$pausesessionlink` / `$resumesessionlink`) via `LpbHtmlParser`, and fall back to standard `/index.php?pause=1` or `?resume=1` query paths.

##### 3.1.2.2 Method B: Hidden WebView JS Fallback
**3.1.2.2.1** When `useWebViewFallback` is enabled and the portal gateway responds over HTTP without a conclusive Method A transition, the system shall dispatch the action to a hidden `1dp x 1dp` `WebView` (`HiddenPortalWebView`) to execute DOM button/link JavaScript clicks.

##### 3.1.2.3 Smart Retry with Exponential Backoff
**3.1.2.3.1** When the portal endpoint (`http://10.0.0.1` or custom URL) is unreachable at the TCP/HTTP level, the controller shall perform at most a brief bounded in-call micro-retry (`1s -> 2s`), skip unnecessary `WebView` fallback execution on dead endpoints, and enforce an exponential cross-cycle cooldown (`4s -> 8s -> 16s -> 32s -> 64s -> 120s` cap) via `PortalRetryPolicy` before attempting further automated background retries.

##### 3.1.2.4 Automatic & Manual Cooldown Reset
**3.1.2.4.1** The system shall clear active exponential backoff immediately upon any successful portal command, WiFi reconnection or SSID change, portal URL update, service stop, or manual user tap on **Pause Now**, **Resume Now**, or **Retry Now**.

#### 3.1.3 Pause Expiration Tracking, Alerts & Background Priority

##### 3.1.3.1 Pause Countdown Timer
**3.1.3.1.1** While a session is `PAUSED`, the system shall run a one-second countdown timer against the configured LPB pause limit (`pauseExpiryMinutes`, default `60` minutes) and display remaining time in both the UI and foreground notification.

##### 3.1.3.2 5-Minute Expiry Warning Alert
**3.1.3.2.1** When remaining pause time reaches 5 minutes (`<= 300s`) or less for the first time in a pause cycle, the system shall post a high-priority notification (`CHANNEL_ALERT_ID`) and double-vibrate to warn the user before paused WiFi time expires.

##### 3.1.3.3 Battery Optimization Guidance & Background Priority Modal
**3.1.3.3.1** When battery optimization is not disabled for Eltharion, the system shall provide an actionable notification (`CHANNEL_BATTERY_ID`) and an in-app **Background Priority** modal sheet (`BackgroundPrioritySheet`) with direct links to Android Battery Optimization Settings and App Battery Info.

#### 3.1.4 Live Signal Visualization & Practice Mode

##### 3.1.4.1 Circular RSSI Signal Ring Gauge
**3.1.4.1.1** The Home dashboard and Simple Mode shall render a circular ring gauge (`RssiSignalGauge`) displaying WiFi signal strength as both an easy-to-read percentage (`0%–100%`) and raw `dBm`, along with plain-English proximity status and live strike indicators.

##### 3.1.4.2 60-Second Trend Line Chart & Zone Distribution
**3.1.4.2.1** The application shall retain a strictly in-memory trailing 60-second window of RSSI samples (`MiniRssiTrendChart60s` and `RssiHistoryLineChart`) and concentric safe/weak zone distribution rings (`SignalZoneConcentricRings`) relative to the active pause threshold.

##### 3.1.4.3 Offline Practice / Simulation Mode
**3.1.4.3.1** The **Signal** tab shall provide an offline **Practice Mode** (`SimulationLabCard`) featuring a pretend RSSI slider (`-95` to `-40 dBm`), quick range presets (**Near**, **Walk Far**, **Come Back**), a **Test 5-Min Expiry Alert** fast-forward button, and a **Pretend 10.0.0.1 Unreachable** switch to test Smart Retry backoff without a live router.

#### 3.1.5 Monthly Time-Saved Tracking

##### 3.1.5.1 Local Pause Duration Recording
**3.1.5.1.1** The preferences repository (`GuardPreferencesRepository`) shall record pause start timestamps locally and accumulate elapsed paused duration (clamped to the active session expiry limit) into the corresponding `yyyy-MM` monthly record whenever a pause concludes or expires.

##### 3.1.5.2 Monthly Summary Dashboard & 6-Month Overview
**3.1.5.2.1** The **Saved** tab (`MonthlySavingsTab`) shall display the active month's total time saved (including a live in-progress ticker during active pauses), a 2×2 breakdown (**Auto-Pauses**, **All Pauses**, **Avg Per Pause**, **Longest Pause**), and a 6-month historical bar chart.

##### 3.1.5.3 Local Savings Reset
**3.1.5.3.1** Users shall be able to clear all locally stored monthly savings statistics at any time via a confirmation dialog on the **Saved** tab.

#### 3.1.6 Accessibility Modes & Quick Settings Tile Control

##### 3.1.6.1 Simple Mode Toggle
**3.1.6.1.1** Users shall be able to enable **Simple Mode** (`simpleModeEnabled`), which hides navigation bars, charts, logs, and advanced configuration to present only connection status, the large 264dp signal gauge, a prominent **Pause WiFi Time / Resume WiFi Time** button, and an **Auto-Pause Protection** toggle.

##### 3.1.6.2 One-Tap Distance Presets & Beginner Help Sheet
**3.1.6.2.1** The **Settings** tab shall provide one-tap range presets (**Close** `-68 dBm`, **Balanced** `-75 dBm`, **Far** `-82 dBm`) alongside fine-tuning buttons and collapsible **Advanced Portal Settings**, while Home provides a **How Eltharion Works (`?`)** modal sheet (`QuickHelpSheet`).

##### 3.1.6.3 Android Quick Settings Centaur Tile & Pinning
**3.1.6.3.1** The system shall expose an Android Quick Settings Tile (`EltharionTileService`) displaying Eltharion's transparent centaur emblem (`ic_qs_eltharion`), live active/inactive state, and status subtitle so users can start or stop protection from the swipe-down shade, supplemented by an in-app **Add Quick Tile** prompt (`StatusBarManager.requestAddTileService` on Android 13+ and step-by-step instructions on Android 7–12).

##### 3.1.6.4 Portal Configuration & Activity History
**3.1.6.4.1** Users shall be able to customize the portal gateway URL (default `http://10.0.0.1`), pause expiration limit (`10–240` minutes), and WebView fallback toggle, as well as inspect and clear recent chronological event logs in the **History** tab.

---

### 3.2 Usability

#### 3.2.1 Adaptive Bottom Navigation & Navigation Rail
**3.2.1.1** In full mode, the application shall provide a thumb-friendly bottom icon bar (`MinimalBottomBar`) on compact screens (`< 720dp` width) and automatically adapt to a side `NavigationRail` on wide tablet or foldable displays (`>= 720dp`), constraining content width (`<= 620dp`) for readability.

#### 3.2.2 High-Contrast Matte Monochrome Presentation
**3.2.2.1** The user interface shall apply a unified Material Design 3 dark monochrome palette (`AbyssBlack` / `ObsidianBg` charcoal surfaces with crisp `PureWhite` primary controls and semantic `AmberCaution` / `RoseDanger` alerts) paired with bundled Google Fonts (`Syne`, `Plus Jakarta Sans`, `Space Grotesk`, `Inter`, and `JetBrains Mono`).

#### 3.2.3 Simple Mode & Beginner-Friendly Controls
**3.2.3.1** The application shall prioritize plain-English status terminology (`TIME RUNNING`, `TIME PAUSED`, signal percentages, and `Close / Balanced / Far` range presets) and provide **Simple Mode** so non-technical users can operate core pause/resume and auto-guard actions effortlessly.

---

### 3.3 Reliability & Availability

#### 3.3.1 Persistent Foreground Service & Battery Priority
**3.3.1.1** `WifiGuardService` shall operate as a `START_STICKY` Foreground Service with a capped `PARTIAL_WAKE_LOCK` and active notification so Android OS background restrictions do not silently interrupt RSSI monitoring during locked-screen walks.

#### 3.3.2 Local-Only Preference & Monthly Savings Persistence
**3.3.2.1** User thresholds, portal URL, Simple Mode state, and monthly time-saved records shall persist reliably across application closures and device reboots in local Jetpack DataStore (`GuardPreferencesRepository`).

---

### 3.4 Performance

- **Minimal Polling Footprint:** Live WiFi RSSI polling runs on a 2-second interval using a rolling in-memory buffer capped at 60 seconds (`<= 60` samples) and 150 activity log entries to prevent unbounded memory growth.
- **Bounded HTTP Timeouts:** Method A portal checks enforce strict 2,500 ms connect/read/write timeouts over OkHttp with `Connection: close` headers.
- **Zero Radio Churn on Dead Portals:** When `10.0.0.1` is unreachable, `PortalRetryPolicy` eliminates repeated background HTTP/WebView attempts during exponential cooldown windows (`4s` up to `120s`), minimizing CPU wakeups and battery drain.

---

### 3.5 Security & Privacy

#### 3.5.1 Local Storage and Session Integrity
**3.5.1.1** All signal history, settings, activity logs, and monthly savings analytics shall remain strictly on the user's device with no remote analytics, cloud database uploads, or third-party account requirements.

#### 3.5.2 Least-Privilege Permissions & WiFi Network Binding
**3.5.2.1** The application shall request only runtime permissions strictly required by Android to read WiFi SSID/RSSI (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`) and post alerts (`POST_NOTIFICATIONS`), and shall bind portal HTTP calls exclusively to the active WiFi `Network` object (`WifiNetworkBinder`) rather than routing over cellular data.

---

### 3.6 Design Constraints

#### 3.6.1 Native Android Toolchain & Adaptive Layout Constraints
**3.6.1.1** The application is implemented exclusively in Kotlin using Jetpack Compose and Material 3 (`minSdk = 24`, `compileSdk = 36`, `targetSdk = 36`), enforcing `enableEdgeToEdge()`, `WindowInsets.safeDrawing`, and minimum `48dp` interactive touch targets across all screens.

#### 3.6.2 Android Foreground Location & Background Service Compliance
**3.6.2.1** Because Android requires `FOREGROUND_SERVICE_TYPE_LOCATION` for live WiFi RSSI monitoring and restricts starting while-in-use location services from the background on Android 14+, `EltharionTileService` shall gracefully collapse the notification shade and launch `MainActivity` (`EXTRA_AUTO_START_GUARD`) or post a one-tap notification fallback whenever OS background restrictions or missing permissions prevent silent service startup.

---

### 3.7 Interfaces

The application communicates across local hotspot gateways and native Android framework APIs to execute its monitoring and automation operations.

#### 3.7.1 Captive Portal HTTP & DOM Interface
Interfaces with LPB Piso WiFi and compatible captive portals (default `http://10.0.0.1`) via `OkHttpClient` (`LpbPortalController` + `LpbHtmlParser`) and an embedded Android `WebView` (`HiddenPortalWebView`) to detect and trigger session pause/resume endpoints.

#### 3.7.2 Android WiFi & Network Binding Interface
Connects to `WifiManager` and `ConnectivityManager` (`WifiTelemetryReader` and `WifiNetworkBinder`) to read live RSSI (`dBm`), SSID/BSSID metadata, and bind socket traffic directly to `NetworkCapabilities.TRANSPORT_WIFI`.

#### 3.7.3 Android Quick Settings, Notification & Power Management Interface
Integrates with Android `TileService` (`EltharionTileService`), `StatusBarManager.requestAddTileService` (API 33+), `NotificationManager` (`DeviceFeedbackHelper` channels for ongoing guard telemetry, 5-minute expiry alerts, and battery optimization guidance), and `PowerManager` (partial wake lock and `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`).

#### 3.7.4 Device Haptics & Local Persistence Interface
Connects to `Vibrator` / `VibratorManager` for distinct single- and double-pulse pause/resume haptic confirmation, and to Jetpack `DataStore<Preferences>` (`lpb_guard_prefs`) for strictly local on-device storage.
