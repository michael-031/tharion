# Eltharion

**Eltharion** is an open-source Android app (Kotlin + Jetpack Compose) that protects your paid **LPB Piso WiFi** (`http://10.0.0.1`) and compatible captive-portal WiFi minutes automatically. It monitors live WiFi signal strength (RSSI) in the background, pauses your session when you walk out of range, alerts you before paused time expires, and tracks every minute saved strictly on your device.

---

## Features

- **Automatic Signal Guard (2-Strike Debounce):** Polls live WiFi RSSI every 2 seconds in a Foreground Service and triggers **Pause** only after two consecutive weak readings—eliminating false triggers from momentary signal dips.
- **Dual-Method Portal Automation:** Supports fast WiFi-bound HTTP + HTML link parsing (**Method A**) and a hidden `1x1` `WebView` DOM click fallback (**Method B**) targeting `http://10.0.0.1` (configurable for other captive portals).
- **Smart Retry Exponential Backoff:** Automatically backs off (`4s → 8s → 16s → 32s → 64s → 120s` cap) when `10.0.0.1` is unreachable so failed background requests never drain your battery.
- **Pause Countdown & 5-Min Expiry Alert:** Tracks remaining pause allowance and sends a high-priority notification + double vibration 5 minutes before paused time expires.
- **Quick Settings Centaur Tile:** Toggle Eltharion protection on or off directly from Android's swipe-down Quick Settings shade without opening the app.
- **Simple Mode:** Hides charts, logs, and advanced controls to display only connection status, a large signal gauge, and a prominent **Pause / Resume WiFi Time** button.
- **Battery Optimization Helper:** Interactive per-brand checklists tailored for **Samsung (One UI)**, **Xiaomi / Redmi / POCO (HyperOS / MIUI)**, **OPPO / Realme (ColorOS)**, **Vivo / iQOO**, and **Pixel / Stock Android** with direct shortcuts to system battery settings.
- **Monthly Time-Saved Tracker & 60s Trend Chart:** Displays real paused time saved per month and a 60-second RSSI line chart stored strictly in local DataStore and device memory—zero cloud uploads or accounts.
- **Offline Practice Mode:** Test auto-pause thresholds, 5-minute expiry alerts, and unreachable-portal Smart Retry backoff safely with a pretend signal slider.

---

## Download & Install

### Option 1: Install the Pre-Built APK (Recommended for Users)

1. Open the repository's **Releases** page on GitHub (`Releases → Latest`) and download `eltharion-v1.0.apk` (or `app-release.apk`) onto your Android phone (Android 7.0+ / API 24 or higher).
2. Tap the downloaded `.apk` file in your browser or **Files** app.
3. If prompted, allow **Install unknown apps** for your browser/file manager, then tap **Install**.
4. Open **Eltharion** and complete quick first-time setup:
   - Grant **Location** permission (required by Android to read live WiFi SSID and signal strength) and **Notifications** permission (for live guard status & 5-minute expiry alerts).
   - Tap **Battery Helper** on Home (or in **Settings**) and follow your phone brand's checklist so Android does not pause background monitoring.
   - *(Optional)* Tap **Settings → Add Quick Tile** to pin Eltharion's Centaur tile into your top Quick Settings panel.

### Option 2: Build & Install from Source (Developers)

#### Prerequisites
- **Android Studio** (Ladybug or newer) or **Gradle 8+**
- **JDK 11+** (JDK 17 recommended)
- **Android SDK** with **API Level 36** (`minSdk = 24`, `targetSdk = 36`)

#### Steps
```bash
# 1. Clone the repository
git clone https://github.com/<your-username>/<your-repo>.git
cd <your-repo>

# 2. Run unit & Robolectric tests
gradle :app:testDebugUnitTest

# 3. Build a debug APK
gradle :app:assembleDebug
```
The compiled APK will be output to `app/build/outputs/apk/debug/app-debug.apk`, or you can click **Run ▶** directly in Android Studio with your Android phone connected via USB Debugging.

---

## Documentation

- **[Software Requirements Specification (SRS.md)](./SRS.md)**
- **[Software Design Document (SDD.md)](./SDD.md)**

---

## Contributing & Repository Security

This is a public open-source project licensed under the **MIT License**.
- Direct pushes to the `main` branch are restricted by repository branch protection rules.
- Anyone can fork the repository, make changes on a feature branch, and open a **Pull Request** for maintainer review before merging.

---

## License

Licensed under the [MIT License](./LICENSE).
