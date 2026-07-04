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
