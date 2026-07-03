# Android Client Design Plan

## Current Status of the Android Client

The Android client in this repository is already implemented as a working Kotlin app skeleton with Compose UI, authentication, device registration, usage permission handling, and basic session reconstruction.

### What is already implemented

- Kotlin + Jetpack Compose UI
- Login screen in MainActivity
- Post-login home screen in HomeActivity
- Retrofit-based auth and device registration APIs
- SharedPreferences storage for:
  - access token
  - device JWT
  - installation ID
- Usage access permission check and settings redirect
- Basic UsageStatsManager-based app usage reading
- Session reconstruction logic based on usage events
- Unit tests for auth request validation and session reconstruction

### Current app entry points

- MainActivity: handles boot flow, token check, and login screen
- HomeActivity: shows installation ID, permission status, and recent app usage data

## Architecture Status

### Completed

- Authentication
- Device Registration
- InstallationId Strategy
- Usage Permission Flow
- Usage Event Reading
- Session Reconstruction
- Deterministic Session IDs
- Room Persistence
- Repository Layer
- Crash-safe Transactions
- Checkpoint Persistence
- End-to-End Persistence Tests

### In Progress

- Session Synchronization Layer

### Planned

- Manual Sync
- WorkManager Background Sync
- Retry Strategy
- Sync Dashboard
- Analytics UI

## Current Architecture Snapshot

### UI layer

- Compose screens are defined directly in activity classes
- The current structure is simple and functional, but it is not yet a full MVVM architecture

### Networking

- AuthApi handles login via POST /api/auth/token
- DeviceApi handles device registration via POST /api/devices
- Retrofit is used directly from the UI layer

### Storage

- TokenStore manages user JWT persistence
- DeviceTokenStore manages device JWT persistence
- InstallationIdStore generates and stores a stable installation ID

### Usage tracking

- UsagePermissionHelper checks whether the app has PACKAGE_USAGE_STATS access
- UsageStatsReader reads recent foreground app usage events
- SessionReconstructor converts usage events into session-like objects

## Current Design Decisions Confirmed by the Codebase

### Platform choices

- Language: Kotlin
- UI: Jetpack Compose
- Networking: Retrofit + Gson
- Concurrency: Kotlin Coroutines
- Permissions: Android usage stats permission and internet permission

### Tracking strategy

- The app uses Android usage events rather than aggregate screen-time totals
- Sessions are reconstructed from usage events and represented in a simple Session model
- The current implementation is already aligned with a session-based backend model

### Authentication flow

1. User logs in with email and password
2. App stores the returned access token
3. App registers the device with the backend using installation ID
4. App stores the returned device JWT

## Current Implementation Gaps

The codebase is a solid foundation, but several parts still need refinement:

- No background sync worker for uploading sessions
- No dedicated ViewModel layer (Repository layer has been implemented)
- Network calls are still performed directly from the UI layer
- Error handling and retry logic are minimal
- The session reconstruction logic is basic and should be expanded for reliability

## Persistence Layer (Implemented)

The Android client now implements an offline-first persistence architecture using Room.

### Architecture

```
UsageEvents
      ↓
SessionReconstructor
      ↓
SessionCaptureService
      ↓
SessionRepository
      ↓
RoomSessionRepository
      ↓
Room Database
```

### Database

Database name: `cross_device_tracker.db`

Tables:

#### sessions
- `id` (TEXT PRIMARY KEY)
- `packageName`
- `appName`
- `startTimeUtc`
- `endTimeUtc`
- `durationSeconds`
- `syncStatus`
- `createdAtUtc`
- `errorMessage`

Indexes:
- `packageName`
- `startTimeUtc`
- `syncStatus`
- (`startTimeUtc`, `syncStatus`)

#### tracker_metadata
- `key` (TEXT PRIMARY KEY)
- `value` (TEXT)

Examples:
- `last_processed_event`
- `last_successful_sync`
- `database_version`

### Session IDs

Session IDs are deterministic and generated using:
```
SHA256(
    packageName +
    startTimeUtc +
    endTimeUtc
)
```

This ensures:
- idempotent reconstruction
- duplicate prevention
- crash recovery support

### Crash Safety

Session persistence and checkpoint updates occur within a single Room transaction:

```
BEGIN TRANSACTION
    insert sessions
    update last_processed_event
COMMIT
```

This guarantees:
- no lost sessions
- duplicate-tolerant recovery
- atomic checkpoint updates

### Repository Layer

The `SessionRepository` abstraction isolates the domain layer from Room.

Implemented components:
- `SessionRepository`
- `RoomSessionRepository`
- `SessionMapper`
- `SessionCaptureService`

### Testing

An end-to-end persistence test verifies:
- session reconstruction
- deterministic IDs
- duplicate prevention
- checkpoint persistence
- crash recovery behavior


## Next Major Milestone

### Session Synchronization Layer

Components:
- `SessionSyncService`
- `SessionApi`
- `SessionSyncResult`
- `SessionUploadDto`
- `SessionBatchUploader`
- Retry Strategy
- Sync Status Update Logic

Goals:
- Upload pending sessions
- Mark SENT/FAILED
- Preserve offline-first behavior
- Never lose data
- Support future WorkManager integration

## Session Synchronization Layer Design

### Design Goals
- ✅ **Offline-first**: Always persist sessions locally before attempting sync.
- ✅ **Never lose sessions**: Session data is preserved until successfully uploaded.
- ✅ **Retry failed uploads**: Robust handling of temporary errors.
- ✅ **Idempotent uploads**: Avoid duplicates using deterministic IDs.
- ✅ **Support manual sync**: Expose trigger for user-initiated sync.
- ✅ **Support WorkManager later**: Built with decoupled interfaces for easy scheduling.
- ✅ **Support partial failures**: Batch failures do not block the entire queue.

### High Level Flow

```
Room Database
        ↓
getPendingSessions()
        ↓
SessionSyncService
        ↓
SessionUploadMapper
        ↓
SessionApi
        ↓
Backend
        ↓
Update Sync Status
        ↓
Update lastSuccessfulSync
```

### Components

#### 1. `SessionUploadDto`
- **Purpose**: Convert local `Session` to the backend API format.
- **Example fields**:
  - `id`
  - `packageName`
  - `appName`
  - `startTimeUtc`
  - `endTimeUtc`
  - `durationSeconds`
  - `createdAtUtc`

#### 2. `SessionUploadMapper`
- **Purpose**: Maps `Session` to `SessionUploadDto`.
- **Rules**: Pure mapper (no Retrofit, Room, or Android dependencies).

#### 3. `SessionApi`
- **Purpose**: Upload session batch to backend.
- **Endpoint**: `POST /api/timelogs`
- **Input**: `List<SessionUploadDto>`
- **Output**: Success or Failure response.

#### 4. `SessionSyncResult`
- **Purpose**: Represents the outcome of a synchronization operation.
- **Example outcomes**:
  - `SUCCESS`
  - `PARTIAL_SUCCESS`
  - `NETWORK_ERROR`
  - `AUTH_ERROR`
  - `SERVER_ERROR`

#### 5. `SessionSyncService`
The heart of sync operations.
- **Public method**: `syncPendingSessions()`

### Sync Algorithm

1. **Step 1: Get pending sessions**
   - Call `repository.getPendingSessions()`
2. **Step 2: Convert to upload DTOs**
   - Call `mapper.toDto()`
3. **Step 4: Call backend**
   - Call `sessionApi.upload()`
4. **Step 5: Update status on success**
   - If upload succeeds, call `markSessionSent()` and update `lastSuccessfulSync`.
5. **Step 6: Handle failure**
   - If upload fails, call `markSessionFailed()` and save error message.

### Failure Strategy

- **Network failure**: No session deletion. Keep status as `PENDING` (network can recover).
- **Server 500**: Mark status as `FAILED` and save error message.
- **Authentication failure**: Stop sync operation immediately. Require user re-authentication.

### Batch Strategy
- **Rule**: Never upload all sessions at once. Instead, batch in groups of **50 sessions**.
- **Example**: If there are 5000 pending sessions, upload in chunks of 50 (50, 50, 50, 50...).
- **Benefits**: Less memory overhead, lower network failure impact, and easier retries.

### Idempotency Strategy
- Session IDs are deterministic:
  ```
  SHA256(packageName + startTime + endTime)
  ```
- Therefore, uploading the same session twice results in the same ID, and the backend ignores the duplicate.

### Sync Status Lifecycle

```
PENDING ──► upload success ──► SENT
PENDING ──► upload failed  ──► FAILED
FAILED  ──► retry          ──► PENDING
```

### Tracker Metadata
We store the following checkpoints in `tracker_metadata`:
- `last_processed_event`
- `last_successful_sync`
- `database_version`

### Future WorkManager Flow

```
WorkManager ──► SessionSyncService ──► syncPendingSessions()
```

> [!NOTE]
> No business logic will exist inside WorkManager. WorkManager will act purely as a scheduler trigger.

### Final Sync Architecture

```
SessionCaptureService
            ↓
Room
            ↓
SessionSyncService
            ↓
SessionUploadMapper
            ↓
SessionApi
            ↓
Backend
            ↓
Update Sync Status
            ↓
Update lastSuccessfulSync
```

### Implementation Order
*   **Task 1**: `SessionUploadDto`
*   **Task 2**: `SessionUploadMapper`
*   **Task 3**: `SessionSyncResult`
*   **Task 4**: `SessionApi`
*   **Task 5**: `SessionSyncService`
*   **Task 6**: Manual Sync Button
*   **Task 7**: Retry Logic
*   **Task 8**: WorkManager

## Recommended Next Steps

### 1. Introduce proper app architecture

- Add ViewModels for login, home, and tracking state
- Create a repository layer for auth, device registration, and session sync
- Keep the UI focused on rendering state rather than making API calls directly

### 2. Add local persistence for sessions

- Introduce Room as the local database for captured usage sessions
- Store sessions before upload so the app can recover from network failures

### 3. Implement background sync

- Use WorkManager to periodically upload pending sessions
- Handle offline/online transitions gracefully

### 4. Improve session reconstruction

- Use a more robust algorithm for foreground/background transitions
- Handle device shutdown, app restarts, and missing close events more predictably
- Ensure reconstructed sessions map cleanly to backend expectations

### 5. Expand the home experience

- Show recent tracked sessions instead of only recent app package names
- Add sync status, last upload time, and session count
- Improve permission handling and onboarding flow

## Recommended Target Architecture

The Android client should evolve toward:

- Compose UI
- ViewModel + StateFlow/SharedFlow
- Repository layer
- Room database for local session storage
- WorkManager for periodic sync
- Retrofit for backend communication

## Summary

The current codebase already includes the core Android client foundation: authentication, device registration, permissions, usage access, and basic session reconstruction. The main work ahead is to move from a prototype-style implementation to a structured production-ready architecture with persistence, background synchronization, and more reliable tracking behavior.
