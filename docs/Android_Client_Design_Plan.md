# Android Client Design Plan

## Overview

The Android client for CrossDeviceTracker is already a functional Kotlin application with Compose-based UI, authentication, device registration, usage-permission handling, and local session persistence. The next phase is to turn that foundation into a more robust, production-ready client with clearer architecture, background synchronization, and a better user experience.

## Current Implementation Status

The current app already includes:

- Kotlin with Jetpack Compose UI
- Login flow in MainActivity
- Home screen in HomeActivity
- Retrofit-based authentication and device registration APIs
- SharedPreferences storage for access token, device JWT, and installation ID
- Usage access permission handling and redirect to settings
- UsageStats-based app-usage reading
- Session reconstruction from usage events
- Room persistence for reconstructed sessions
- Repository abstractions for session storage and sync
- Session sync classes such as SessionSyncService, SessionUploadMapper, SessionApi, and SessionUploadDto

## Target Architecture

The client should evolve toward a clean, layered structure:

- UI layer: Compose screens and lightweight state holders
- Presentation layer: ViewModel classes for login, home, and tracking state
- Domain layer: session capture, reconstruction, sync orchestration, and business rules
- Data layer: Room database, Retrofit APIs, token/device stores, and metadata persistence
- Background layer: WorkManager for periodic sync and retry scheduling

## Core Functional Requirements

### 1. Session capture
- Detect foreground app usage transitions from Android usage events
- Reconstruct meaningful sessions from those events
- Persist sessions locally before upload
- Ensure sessions are deterministic and deduplicated

### 2. Offline-first persistence
- Store captured sessions locally immediately
- Preserve data across app restarts and transient network failures
- Keep the app usable without an active connection

### 3. Synchronization
- Upload pending sessions to the backend in batches
- Mark sessions as sent or failed without losing them
- Retry transient failures safely
- Support both manual sync and background sync

### 4. User experience
- Show login state, permission state, and sync status clearly
- Surface errors without confusing the user
- Provide a simple dashboard for recent activity and pending uploads

## Current App Structure

### Main entry points
- MainActivity: handles boot flow, token validation, and login UI
- HomeActivity: shows installation information, permission state, and recent usage data

### Existing implementation classes
- AuthApi: authentication requests
- DeviceApi: device registration requests
- TokenStore and DeviceTokenStore: JWT persistence
- InstallationIdStore: stable installation ID generation
- UsagePermissionHelper: permission checks and guidance
- UsageStatsReader: usage event collection
- SessionReconstructor: session reconstruction logic
- SessionCaptureService: capture orchestration
- SessionRepository / RoomSessionRepository: local persistence abstraction
- SessionSyncService: upload orchestration
- SessionUploadMapper: DTO mapping

## Data Model

### sessions table
- id: deterministic session ID
- packageName
- appName
- startTimeUtc
- endTimeUtc
- durationSeconds
- syncStatus
- createdAtUtc
- errorMessage

### tracker_metadata table
- key/value pairs for checkpoints such as:
  - last_processed_event
  - last_successful_sync
  - database_version

## Sync Design

### Goals
- Remain offline-first
- Never lose sessions
- Support retries and partial failures
- Keep uploads idempotent
- Prepare for WorkManager integration

### Sync flow
1. Read pending sessions from local storage
2. Convert them to upload DTOs
3. Send them to the backend in batches
4. Update sync status based on result
5. Record the last successful sync timestamp

### Batch strategy
- Upload sessions in groups of 50 to reduce failure blast radius
- Keep the queue intact on failure
- Retry only the failed portion when feasible

### Idempotency
- Use deterministic session IDs so repeated uploads do not create duplicates

## Permission and Privacy Considerations

- Request PACKAGE_USAGE_STATS only when necessary
- Explain why usage access is required
- Avoid collecting more data than needed for session reconstruction
- Store tokens securely and never expose them in the UI

## Testing Strategy

### Unit tests
- Session reconstruction logic
- Session ID generation
- Mapper correctness
- Sync decision logic

### Integration tests
- Room persistence and checkpoint updates
- Sync service behavior with mocked API responses
- Recovery from partial failures

### UI tests
- Login flow
- Permission handling
- Sync status display

## Implementation Roadmap

### Phase 1: Stabilize the foundation
- Refactor UI code to use ViewModels and state flows
- Keep networking logic out of activities where possible
- Improve error handling across auth and sync flows

### Phase 2: Finish sync and recovery
- Complete pending sync state handling
- Add retry policies for transient network failures
- Add manual sync controls and clear status feedback

### Phase 3: Background reliability
- Introduce WorkManager for periodic sync
- Handle connectivity changes gracefully
- Ensure the app survives process death without losing pending work

### Phase 4: User-facing polish
- Add a dashboard for recent sessions and sync health
- Improve onboarding and permission guidance
- Prepare the app for broader testing and release validation

## Recommended Next Milestones

1. Move auth, session, and sync logic behind repository and ViewModel abstractions
2. Complete end-to-end sync handling with robust retry semantics
3. Add WorkManager-based background synchronization
4. Improve the home screen to show session health and sync metrics
5. Expand automated tests around persistence and sync behavior

## Summary

The Android client already has a solid base: authentication, permissions, usage tracking, session reconstruction, and local persistence are all present. The priority now is to harden the architecture, complete the sync workflow, and make the experience reliable for real-world use.

---

## Known Issues & Critical Security Concerns

### 🔴 Critical Security Issues

**Issue 1 — Unencrypted SharedPreferences**
- `TokenStore`, `DeviceTokenStore`, and `InstallationIdStore` use plain `MODE_PRIVATE` SharedPreferences
- Auth tokens, device JWT, and installation ID can be exfiltrated via adb backup, rooted devices, or compromised backups
- `AndroidManifest.xml` has `android:allowBackup="true"` with empty backup rules
- **Fix Required:** Migrate to `EncryptedSharedPreferences` (androidx.security:security-crypto), add one-time migration, exclude auth prefs from backup

**Issue 2 — Sensitive Data Logged to Logcat**
- `MainActivity.kt` logs full `LoginResponse` (contains accessToken) and `deviceJwt`
- `SessionSyncService.kt` logs raw upload JSON
- `RoomSessionRepository.kt` logs sessions with PII
- **Fix Required:** Remove all Log.* calls that print sensitive data, replace with non-PII events, introduce Logger wrapper gated on `BuildConfig.DEBUG`

### 🔴 Critical Bugs

**Issue 3 — SessionReconstructor Wrong appName**
- When a session is closed by a different package (e.g., phone call), the closed session gets the new package's name as `appName`
- Line 39 passes `event.packageName` instead of `currentPackage`
- **Fix Required:** Pass `currentPackage` consistently as appName in all termination branches

**Issue 4 — OnConflictStrategy.IGNORE Drops Re-captured Sessions**
- `SessionDao` uses `@Insert(onConflict = OnConflictStrategy.IGNORE)`
- Re-captured sessions after failures are silently dropped
- **Fix Required:** Change to `@Upsert` or `REPLACE`, overwrite sync status and error message on conflict

**Issue 5 — Failed Sessions Never Retried**
- Failed sessions are excluded from `getPendingSessions()` forever
- No exponential backoff, no failure count, no automatic retry
- **Fix Required:** Add `failureCount` and `nextRetryAt` columns, implement backoff strategy, modify query to include retryable failed sessions

**Issue 6 — Log.wtf Left in Production**
- `MainActivity.kt:51` and `HomeActivity.kt:36` have `Log.wtf("HARSHIT_TEST", ...)`
- Can crash app on some OEM ROMs (Samsung, MIUI)
- **Fix Required:** Remove Log.wtf calls, introduce Logger wrapper

**Issue 7 — Room Database Missing Migration Strategy**
- `AppDatabase` has `version = 1, exportSchema = false` with no migrations
- Will crash on schema upgrade
- **Fix Required:** Add `fallbackToDestructiveMigration()` or explicit migrations, enable `exportSchema = true`

**Issue 8 — Retrofit Rebuilt on Every Click**
- New Retrofit instances created in `MainActivity` and `HomeActivity` on every interaction
- No timeouts, no logging interceptor, no Authenticator for 401
- **Fix Required:** Create singleton OkHttpClient and Retrofit in Application class, configure timeouts and logging

**Issue 9 — CoroutineScope Leaks**
- Bare `CoroutineScope(Dispatchers.IO).launch` not tied to lifecycle
- Catches `Exception` (swallows CancellationException)
- **Fix Required:** Use `lifecycleScope` or `viewModelScope`, use `runCatching` for proper exception propagation

**Issue 10 — registerDevice Swallows HTTP Failure**
- All errors in `registerDevice` are caught and only logged
- User navigates to HomeActivity even if deviceJwt is null
- No recovery path for failed registration
- **Fix Required:** Surface registration failure to UI, prevent navigation on failure, trigger re-login on 401

### 🟠 High Priority Issues

**Issue 11 — Dead Code in UsageStatsReader**
- `eventProvider` declared but never used in production
- Test that depends on it likely doesn't compile
- **Fix Required:** Remove dead code or refactor to use injected source

**Issue 12 — Recent Apps UI Broken**
- `joinToString("")` produces single concatenated string with no separators
- **Fix Required:** Use `joinToString("\n")` or render in LazyColumn

**Issue 13 — Invalid applicationId**
- `applicationId = "com.example"` will be rejected by Play Store
- **Fix Required:** Change to real reverse-DNS string (e.g., com.harshit.crossdevicetracker)

**Issue 14 — R8/Minification Disabled**
- `optimization { enable = false }` in release builds
- No proguard-rules.pro file
- **Fix Required:** Enable R8, add keep rules for Retrofit, Gson, Room, Compose

**Issue 15 — SessionReconstructor Not Idempotent**
- Open session re-created with different endTime on each capture
- Will cause duplicates once Issue 4 is fixed
- **Fix Required:** Persist open session state in tracker_metadata

**Issue 16 — getRecentAppPackages Ignores Watermark**
- Reads 24h window without honoring `lastProcessedEventTimestamp`
- Shows inconsistent data
- **Fix Required:** Use same watermark as capture service or remove feature

**Issue 17 — No OkHttp Authenticator**
- No automatic re-registration on expired device JWT
- **Fix Required:** Add Authenticator to detect 401 and trigger re-registration

### 🟡 Medium/Low Priority Issues

**Issue 18 — Weak Login Validation**
- `isValidLoginInput` only checks non-blank, no email format validation, no trim
- **Fix Required:** Add email.trim(), format validation, minimum password length

**Issue 19 — Untested Serialization**
- `SessionUploadDto` uses `Instant.toString()` with no contract test
- Server format is implicit
- **Fix Required:** Add MockWebServer test to verify wire format

**Issue 20 — Outdated Dependencies**
- `coreKtx = "1.10.1"` and `lifecycleRuntimeKtx = "2.6.1"` are outdated
- **Fix Required:** Bump to latest stable versions

### Bonus Micro-Issues

- **B1:** HomeActivity uses `Spacer(Modifier.padding(...))` instead of `Spacer(Modifier.height(...))`
- **B2:** Duplicate constant `LAST_PROCESSED_EVENT_KEY` in RoomSessionRepository and TrackerCheckpointKeys
- **B3:** `UsagePermissionHelper.openUsageAccessSettings` doesn't check if activity exists
- **B4:** Log.d logs sessions with PII (package names)
- **B5:** Deprecated `ResponseBody.create(null, "")` in tests
- **B6:** Unused import `java.util.Locale` in MainActivity
- **B7:** `LoginResponse.email` returned but never used
- **B8:** No README.md exists
- **B9:** No Application subclass
- **B10:** XML theme uses old Material theme instead of Material3
