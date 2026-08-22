# CrossDeviceTracker.Android

The **Android client** for the [Cross Device Screen Time Tracker](https://github.com/Harshit4847/CrossDeviceTracker.Api) system. The app measures foreground application usage on Android devices, stores sessions locally in an offline-first database, and synchronizes them to the central backend API for cross-device screen-time analytics.

> **Status:** `0.1.0-alpha` — under active development.

## How It Works

```
┌─────────────────────────────────────────────────┐
│                 Android Client                   │
│                                                  │
│  Login → Device registration → Usage permission  │
│         │                                        │
│         ▼                                        │
│  Session Capture (UsageStats events)             │
│         │ reconstruct                            │
│         ▼                                        │
│  Room Database (offline-first queue)             │
│         │ batch upload                           │
│         ▼                                        │
│  WorkManager Sync (periodic + one-time)          │
└────────────────────┬────────────────────────────┘
                     │ HTTPS / JWT
                     ▼
          CrossDeviceTracker.Api (ASP.NET Core)
```

1. **Login** with email/password to get a user JWT.
2. **Register the device** to receive a device JWT used for all tracking uploads.
3. **Grant usage access** (`PACKAGE_USAGE_STATS`) so foreground app usage can be read.
4. **Sessions** are reconstructed from usage events and persisted locally first.
5. A **background worker** uploads pending sessions in batches and marks them as synced.

## Features

- Email/password authentication via Retrofit
- Automatic device registration with a stable installation ID
- Foreground usage capture using Android UsageStats events
- Deterministic session IDs for idempotent, duplicate-free uploads
- Offline-first persistence with Room — no data loss without connectivity
- Batched background synchronization with WorkManager (periodic + on-demand)
- Dashboard with aggregated screen-time stats
- Timeline view of recent app usage

## Tech Stack

| Technology | Purpose |
|------------|---------|
| Kotlin | Primary language |
| Jetpack Compose + Material 3 | UI |
| Room | Local session persistence |
| Retrofit + Gson | REST communication |
| WorkManager | Background sync scheduling |
| Coroutines | Async operations |
| KSP | Room compiler processing |

**Toolchain:** AGP 9.2.1 · Kotlin 2.2.10 · Java 11 · minSdk 24 · target/compileSdk 36

## Project Structure

All source lives under `app/src/main/java/com/example/`:

```
app/src/main/java/com/example/
├── MainActivity.kt              # Entry point, boot flow, login UI
├── HomeActivity.kt              # Installation info, permissions, recent usage
├── DashboardActivity.kt         # Aggregated screen-time dashboard
├── TimelineActivity.kt          # Recent usage timeline
│
├── AuthApi.kt                   # Authentication endpoints
├── DeviceApi.kt                 # Device registration endpoints
├── TokenStore.kt                # User JWT persistence
├── DeviceTokenStore.kt          # Device JWT persistence
├── InstallationIdStore.kt       # Stable per-installation identifier
│
├── UsagePermissionHelper.kt     # Usage access permission flow
├── UsageStatsReader.kt          # Reads usage events from UsageStatsManager
├── SessionReconstructor.kt      # Builds sessions from raw usage events
├── SessionCaptureService.kt     # Capture orchestration
├── SessionIdGenerator.kt        # Deterministic session IDs
│
├── AppDatabase.kt               # Room database
├── SessionEntity.kt / TrackerMetadataEntity.kt
├── SessionDao.kt / TrackerMetadataDao.kt
├── SessionRepository.kt / RoomSessionRepository.kt
├── TrackerCheckpointKeys.kt     # Sync checkpoint keys (watermarks)
│
├── SessionSyncService.kt        # Upload orchestration
├── SessionApi.kt                # Session upload endpoint
├── SessionUploadDto.kt / SessionUploadMapper.kt
├── SyncManager.kt / SyncWorkManager.kt
├── OneTimeSyncWorker.kt / PeriodicSyncWorker.kt
│
├── DashboardApi.kt / DashboardDto.kt / DashboardSummaryResponse.kt
├── DashboardRepository.kt / DashboardService.kt
├── DashboardViewModel.kt / DashboardViewModelFactory.kt
│
├── TimelineRepository.kt / TimelineViewModel.kt / TimelineViewModelFactory.kt
└── NetworkConnectivityManager.kt
```

Local database schema:

- **`sessions`** — id, packageName, appName, start/end time (UTC), durationSeconds, syncStatus, createdAtUtc, errorMessage
- **`tracker_metadata`** — key/value checkpoints such as `last_processed_event` and `last_successful_sync`

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 11+
- An Android device or emulator running API 24+

### Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test
```

Or open the project in Android Studio and press **Run**.

### Configuration

The backend base URL is defined as `BASE_URL` in `MainActivity.kt`. By default it points to the Azure-hosted API instance; change it if you are running your own backend.

Release builds require a `keystore.properties` file at the project root:

```properties
storeFile=/path/to/keystore.jks
storePassword=...
keyAlias=...
keyPassword=...
```

### First Run Flow

1. Log in with an existing account (register via the web/backend if needed).
2. Grant **Usage Access** when prompted (Settings → Apps → Special app access → Usage access).
3. The device registers itself and begins capturing sessions.
4. Use the Home screen's sync action to force an immediate upload, or wait for periodic background sync.

## Permissions

| Permission | Why |
|------------|-----|
| `INTERNET`, `ACCESS_NETWORK_STATE` | API communication and connectivity-aware sync |
| `PACKAGE_USAGE_STATS` | Reading foreground app usage events (granted via Settings, not a runtime dialog) |

No data is collected beyond what is needed for session reconstruction: package name, app label, and usage timestamps.

## Documentation

- [`docs/Android_Client_Design_Plan.md`](docs/Android_Client_Design_Plan.md) — architecture plan, roadmap, and known issues
- [`docs/Document.md`](docs/Document.md) — full system documentation (backend design, API reference, data model)

## Roadmap

- Harden auth/session/sync behind repository and ViewModel abstractions
- Robust retry semantics with exponential backoff for failed sessions
- Encrypted token storage (`EncryptedSharedPreferences`)
- Expanded automated tests around persistence and sync behavior
- Release readiness (R8/minification, real applicationId)

See the [design plan](docs/Android_Client_Design_Plan.md) for the detailed issue list and milestones.

## Related Repositories

- [CrossDeviceTracker.Api](https://github.com/Harshit4847/CrossDeviceTracker.Api) — ASP.NET Core backend
