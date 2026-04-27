# Ward Companion — Android client

The Android counterpart to your iOS ward-intern app. Talks to the same
`ward-server` you already have running on the Surface Pro 7 over
Tailscale, so you and your friend can use either iOS or Android phones.

## What's in this version (Phase 1 parity with iOS)

- **JWT login** against `ward-server`
- **Biometric lock** (fingerprint or face) when opening the app
- **Patient list** — admit, view, toggle discharged
- **Patient detail** with two tabs:
  - **Photos** — multi-pick from gallery, batch upload, full-screen pinch-zoom viewer with swipe between photos, delete
  - **Daily notes** — date-stamped timeline, create/edit/delete with date picker
- **Live updates** over Socket.IO — when your friend (on iOS or Android) changes anything, your screen refreshes within a second
- **Material 3** with dynamic color (Material You) on Android 12+, and a custom calm-teal scheme below that
- **Configurable server URL** so you can change it from the login screen if Tailscale renames the SP7

## Stack

- Kotlin 2.0 + Jetpack Compose + Material 3
- Coroutines + Flow, Hilt for DI
- Retrofit 2 + OkHttp 4 + kotlinx.serialization (no Gson)
- Socket.IO Android client 2.1.1
- Coil for photo loading (sends the JWT as `Authorization` header so private photos work)
- DataStore Preferences for the JWT
- AndroidX BiometricPrompt with PIN/pattern fallback
- Min SDK 26 (Android 8.0), Target SDK 35

---

## Setting it up (≈ 15 min)

### 1. Open in Android Studio

1. Install **Android Studio Koala (2024.1.1)** or newer.
2. File → Open → select this folder.
3. Let Gradle sync. First sync downloads ~600 MB of dependencies.
4. Plug in an Android phone (USB debugging enabled) or start an emulator.

### 2. Confirm the server URL

Open `app/build.gradle.kts` and check this line:

```kotlin
buildConfigField("String", "DEFAULT_SERVER_URL", "\"http://ward-server:3000\"")
```

This is the **default** the app starts with. The user can change it from
the login screen ("Change server URL") so you only need to edit this if
your SP7 has a different Tailscale name. Examples:

- `http://ward-server:3000` — default Tailscale machine name
- `http://surface-pro:3000` — if you didn't rename the SP7
- `http://100.x.y.z:3000` — raw Tailscale IP (also works)
- `http://192.168.1.50:3000` — same Wi-Fi only, no Tailscale

### 3. Network security

`res/xml/network_security_config.xml` already permits cleartext HTTP for
`ts.net`, `localhost`, `192.168.0.0`, `192.168.1.0`, and the bare host
name `ward-server`. **If your SP7 has a different name, add it there.**

```xml
<domain includeSubdomains="true">your-machine-name</domain>
```

### 4. Install Tailscale on the Android phone

1. Play Store → install **Tailscale**.
2. Sign in with the same account used on the SP7.
3. Toggle the connection on. The SP7 should appear in the device list.
4. Verify in Chrome: visit `http://ward-server:3000/health` — you should
   see `{"ok":true,…}`. If you don't, the rest of the app won't connect.

### 5. Build and run

In Android Studio: green **Run ▶** button. The app installs, prompts you
to log in. Use the same credentials as iOS.

If you see "Can't reach server. Is Tailscale on?" — open the Tailscale
Android app, confirm it's connected.

---

## Server-side changes needed (probably none)

The Android client speaks the **same REST + Socket.IO API** as the iOS
app. As long as your `ward-server` already supports JWT auth, multipart
photo uploads, and these endpoints, no server changes are needed:

- `POST /auth/login` → `{ token, user }`
- `GET /patients` (with `?includeDischarged=true`)
- `POST /patients`
- `GET /patients/:id`
- `PATCH /patients/:id`
- `PUT /patients/:id/discharge`
- `DELETE /patients/:id`
- `GET /patients/:id/photos`
- `POST /patients/:id/photos` (multipart, field `file`, plus `takenAt` and optional `caption`)
- `GET /photos/:id` → JPEG bytes
- `DELETE /photos/:id`
- `GET /patients/:id/notes`
- `POST /notes` body `{patientId,date,text}`
- `PATCH /notes/:id` body `{date,text}`
- `DELETE /notes/:id`

Socket.IO messages: `patient.changed`, `photo.changed`, `note.changed`,
each with `{id, op}` (op = `create | update | delete`); photo and note
events also carry `patientId`.

If your iOS-era endpoints differ in any small way, the mismatch will be
in `data/api/WardApi.kt` — that's the only file you'd need to tweak.

---

## Project layout

```
app/src/main/java/com/wardcompanion/
├── WardCompanionApp.kt        Hilt-enabled Application
├── MainActivity.kt            Compose entry, navigation, biometric gate
├── data/
│   ├── api/                   WardApi (Retrofit) + NetworkModule (Hilt)
│   ├── local/                 SettingsStore (DataStore for JWT, server URL)
│   ├── model/                 Patient, PhotoItem, DailyNote, AppUser, requests
│   ├── repo/                  WardRepository — single API entry point
│   └── socket/                LiveSocket — Socket.IO + LiveEvent flow
├── ui/
│   ├── theme/                 Material 3 + calm-teal custom palette
│   ├── auth/                  AuthViewModel + LoginScreen
│   ├── patients/              PatientList + PatientDetail (photos & notes tabs)
│   ├── photos/                PhotoViewerScreen (pager + pinch-zoom)
│   └── notes/                 DailyNoteEditor + ViewModel
└── util/
    ├── DateFormat.kt
    └── BiometricLock.kt
```

---

## Troubleshooting

- **"Can't reach server"** — Tailscale off on the phone, or SP7 powered off, or wrong server URL.
- **Login works but photos are broken** — Coil isn't sending the JWT. Check `PhotoGrid` builds the request with `addHeader("Authorization", it)` (it does).
- **Biometric prompt never appears** — device has no enrolled fingerprint and no PIN/pattern set. App falls through to unlocked state.
- **"Cleartext HTTP traffic not permitted"** — your server's hostname isn't in `network_security_config.xml`. Add it.
- **Live updates don't fire** — confirm the server emits over the websocket transport (not just polling); the client requests `transports=["websocket"]`.

## Phase 2 (next)

To match the iOS roadmap, the natural next builds are:

- Medications + alarms (foreground service + scheduled notifications)
- BP rounds with on-device ML Kit OCR (mirrors iOS Vision)
- Lab timers
- Daily checklist
- Consult log
- Special requests

The data, repo, and socket layers in this project are already shaped to
add those without restructuring — each new entity becomes a model + a
few repo methods + a screen.
