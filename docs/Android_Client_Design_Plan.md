# Android Client Planning for Cross Device Screen Time Tracker

## Current Project Status

### Backend API

Completed: - ASP.NET Core Backend API - PostgreSQL database - User JWT
authentication - Device JWT authentication - Device linking flow -
Device token expiry handling - Device revocation handling - Time log
APIs - Desktop linking token flow

### Windows Desktop Client

Completed: - Foreground application tracking - Session-based tracking
engine - Local SQLite storage - Offline-first synchronization - Device
linking - Device JWT authentication - Device revocation handling -
System tray/background execution - Alpha release published

## Decision: Build Android Client

The project goal is a true cross-device screen time tracker.

### Technology Decision

Chosen stack: - Language: Kotlin - IDE: Android Studio - UI: Jetpack
Compose - Networking: Retrofit - Local database: Room - Background jobs:
WorkManager - Architecture: MVVM

Rejected: - .NET MAUI Reason: - Native Android APIs are required for
screen tracking. - Kotlin is the official Android language. - Better
resume value and industry relevance.

## Development Environment Decision

Laptop: - CPU: Intel i5-6300U (2 cores / 4 threads) - RAM: 16GB -
Storage: SSD

Decision: - Develop on Windows. - Use Android Studio. - DO NOT use
Android Emulator. - Use a real Android phone connected through USB
debugging.

Reason: - Emulator performance would be poor on this hardware. - Real
device testing is faster and more accurate.

## Android Device Setup Completed

Completed: - Installed Android Studio - Installed Android SDK - Enabled
Developer Mode - Enabled USB Debugging - Verified ADB connection -
Successfully deployed first Android app to physical device

ADB status: Device detected successfully.

## Core Design Decision

Question: Should Android send aggregated screen time or exact sessions?

Decision: Send exact sessions.

Example:

Instagram: 10:00 -\> 10:30

Instagram: 14:00 -\> 14:15

YouTube: 11:00 -\> 12:20

Reason: The backend architecture is already designed around session
objects.

Backend expects:

-   AppName
-   StartTime
-   EndTime
-   DurationSeconds

## Android Tracking Strategy

Android will use:

UsageStatsManager

Flow:

User Login ↓ User JWT ↓ Register Device ↓ Get Device JWT ↓ Request Usage
Access Permission ↓ Read UsageStatsManager events ↓ Reconstruct sessions
↓ Store locally ↓ Sync to backend

## Session Reconstruction Strategy

Android usage events:

10:00 Instagram FOREGROUND 10:30 Instagram BACKGROUND

becomes:

Instagram 10:00 -\> 10:30

### Missing Closing Event Handling

Example:

10:00 Instagram opened 10:20 Phone battery died

Decision: Assume the session ended when the phone shut down.

Reason: This matches the existing Windows tracker behavior.

## Unified Architecture

                Backend API
                     ↑
                     │
            Same TimeLog DTO
                     │
         ┌───────────┴───────────┐
         │                       │
         ▼                       ▼
    Windows Client         Android Client
         │                       │
         ▼                       ▼
    Session Engine        Session Engine
         │                       │
         ▼                       ▼
        SQLite              Room DB

The backend should not care whether a session came from: - Windows -
Android - macOS - Linux

## Next Steps

1.  Learn Kotlin basics.
2.  Learn Jetpack Compose basics.
3.  Build login screen.
4.  Connect Android app to existing backend.
5.  Implement device registration.
6.  Request Usage Access permission.
7.  Read UsageStatsManager events.
8.  Reconstruct sessions.
9.  Store sessions locally using Room.
10. Sync sessions to backend API.

## Important Principle

Windows: GetForegroundWindow() -\> Session

Android: UsageStatsManager -\> Session

Both clients must produce the same backend TimeLog model.
