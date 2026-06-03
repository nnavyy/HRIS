# HRIS Mobile Application

A native Android application for Human Resource Information System (HRIS) built with Jetpack Compose and Firebase. The application provides role-based dashboards and self-service features for managing employees, attendance, leave, payroll, and KPI across an organization.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Firebase Setup](#firebase-setup)
- [Build and Run](#build-and-run)
- [Role-Based Access](#role-based-access)
- [Firestore Collections](#firestore-collections)
- [Security Notes](#security-notes)

---

## Overview

The HRIS Mobile Application serves five distinct user roles within an organization. Each role has its own dashboard, navigation structure, and set of accessible features. Authentication is handled exclusively through Firebase Authentication using email and password credentials. All application data is stored in Cloud Firestore.

---

## Features

### Authentication
- Email and password login with input validation and whitespace trimming
- Persistent session management across app restarts
- Password reset via Firebase email link (Forgot Password)
- Role-based post-login routing to the appropriate dashboard

### Super Admin
- Full system management dashboard with live metrics (active accounts, active roles, automation rules, audit events)
- Create and manage employee accounts
- Role and access management per user
- Office location management for GPS-based attendance validation
- Audit log viewer for all system events

### HR / Admin
- HR dashboard with total employee count, pending approvals, and attendance metrics
- Employee master data management (create, edit, view)
- Leave request approval workflow
- KPI configuration per employee
- KPI scoring submission
- Payroll generation and management
- Access to system audit log

### Finance
- Finance dashboard focused on payroll processing metrics
- Payroll generation and approval management
- Salary slip generation and viewing

### Manager
- Manager dashboard showing team member count and pending leave requests
- Leave approval for direct reports
- Payroll approval
- Team attendance monitoring

### Employee (Self-Service)
- Personal dashboard with check-in status, remaining leave quota, KPI score, and unread notifications
- GPS and photo-based daily attendance (check-in and check-out)
- Leave request submission with quota validation
- Leave request history with status tracking (pending, approved, rejected)
- Salary slip viewing
- Personal KPI result viewing
- Profile management with profile photo upload to Firebase Storage
- Notification center

### Automation Rules
- Configurable system automation including leave quota enforcement, payroll auto-generation trigger, and audit logging

---

## Architecture

The application follows a standard Android MVVM architecture:

- **UI Layer**: Jetpack Compose screens and composables organized by feature module
- **ViewModel Layer**: `ViewModel` classes with `StateFlow` for reactive UI state management
- **Repository Layer**: Repository classes that interface with Firebase Firestore and Firebase Auth
- **Model Layer**: Kotlin data classes representing Firestore document schemas

Navigation is handled by a single `NavHost` with two destinations: the login screen and the main scaffold. The main scaffold uses route-based rendering (`currentRoute` state) rather than a nested `NavHost` to keep navigation state simple and predictable.

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Navigation | Navigation Compose |
| Authentication | Firebase Authentication |
| Database | Cloud Firestore |
| Storage | Firebase Storage |
| Image Loading | Coil |
| Location | Google Play Services Location |
| Camera | CameraX |
| Async | Kotlin Coroutines |
| Build System | Gradle (Kotlin DSL) |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

---

## Project Structure

```
app/src/main/java/com/ptniger/hris/
├── data/
│   ├── model/          # Firestore data models (User, Employee, LeaveRequest, etc.)
│   └── repository/     # Firebase data access layer
├── ui/
│   ├── admin/          # Super Admin screens (account management, automation, roles)
│   ├── attendance/     # Attendance check-in/check-out and monitoring screens
│   ├── audit/          # Audit log screen
│   ├── auth/           # Login screen and forgot password dialog
│   ├── dashboard/      # Role-specific dashboard screens and shared components
│   ├── employee/       # Employee list and form screens
│   ├── kpi/            # KPI configuration, scoring, and result screens
│   ├── leave/          # Leave request and approval screens
│   ├── navigation/     # AppNavigation, MainScaffold, and BottomNavBar
│   ├── notification/   # Notification center screen
│   ├── payroll/        # Payroll generation, approval, and salary slip screens
│   ├── profile/        # Profile screen with photo upload
│   ├── report/         # Report screen
│   ├── superadmin/     # Super Admin user creation ViewModel
│   └── theme/          # Color palette, typography, and Material theme
└── utils/
    ├── AutomationEngine.kt  # Rule-based automation processor
    ├── Constants.kt         # App-wide constants (roles, collections, statuses)
    ├── DateUtils.kt         # Date formatting utilities
    ├── KpiCalculator.kt     # KPI scoring and bonus calculation logic
    └── RoleManager.kt       # Role-to-navigation mapping
```

---

## Firebase Setup

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com).
2. Add an Android app with package name `com.ptniger.hris`.
3. Register your debug signing certificate SHA-1 fingerprint in Project Settings. To obtain the SHA-1, run:
   ```
   ./gradlew signingReport
   ```
4. Download the `google-services.json` file and place it in the `app/` directory.
5. Enable **Email/Password** as a sign-in provider under Authentication.
6. Create a Firestore database in production or test mode.
7. Set up Firestore security rules appropriate for your environment.
8. Enable Firebase Storage for profile photo uploads.

### Initial Admin Account

Because the application does not expose a public registration screen, the first administrator account must be created manually:

1. Go to Firebase Console > Authentication > Users > Add User.
2. Create the account with an email and password.
3. Copy the generated User UID.
4. Go to Firestore > Create collection `users` > Add document with the copied UID as the document ID.
5. Add the following fields to the document:
   - `email` (string)
   - `name` (string)
   - `role` (string): `super_admin`
   - `roles` (array): `["super_admin"]`
   - `primaryRole` (string): `super_admin`

---

## Build and Run

### Prerequisites

- Android Studio Hedgehog or later
- JDK 11
- Android SDK with platform 35 or 36 installed
- A physical Android device or emulator running API 24 or higher

### Steps

1. Clone the repository.
2. Place `google-services.json` in the `app/` directory.
3. Open the project in Android Studio.
4. Sync Gradle dependencies.
5. Connect a device via USB with USB Debugging enabled, or start an emulator.
6. Run the application using:
   ```
   ./gradlew installDebug
   ```

---

## Role-Based Access

| Feature | Super Admin | HR | Finance | Manager | Employee |
|---|:---:|:---:|:---:|:---:|:---:|
| Dashboard | Yes | Yes | Yes | Yes | Yes |
| Employee Management | Yes | Yes | No | No | No |
| Leave Request | No | No | No | No | Yes |
| Leave Approval | No | Yes | No | Yes | No |
| Attendance (Self) | No | No | No | No | Yes |
| Attendance Monitor | No | Yes | No | Yes | No |
| KPI Configuration | No | Yes | No | No | No |
| KPI Scoring | No | Yes | No | No | No |
| KPI Results (Self) | No | No | No | No | Yes |
| Payroll Generation | No | Yes | Yes | No | No |
| Payroll Approval | No | No | No | Yes | No |
| Salary Slip | No | No | No | No | Yes |
| Role Management | Yes | No | No | No | No |
| Automation Rules | Yes | No | No | No | No |
| Office Locations | Yes | No | No | No | No |
| Audit Log | Yes | Yes | Yes | No | No |
| Profile | Yes | Yes | Yes | Yes | Yes |

---

## Firestore Collections

| Collection | Purpose |
|---|---|
| `users` | Authentication-linked user records with role and profile data |
| `employees` | Employee master data including department, branch, and leave quota |
| `attendance` | Daily attendance check-in and check-out records |
| `leave_requests` | Leave submissions with status tracking |
| `kpi_configs` | KPI metric configurations per employee or department |
| `kpi_scores` | KPI scoring results per employee per period |
| `payrolls` | Payroll records including salary components and approval status |
| `notifications` | In-app notifications per user |
| `audit_logs` | System-wide event audit trail |
| `automation_rules` | Configurable automation rule states |
| `office_locations` | GPS coordinates for geofencing-based attendance validation |

---

## Security Notes

- All write operations should be protected by Firestore security rules that verify the caller's role stored in the `users` collection.
- The `google-services.json` file contains the Firebase API key and project identifiers. Do not commit this file to a public repository.
- Firebase Email Enumeration Protection is active by default on newer projects. Authentication errors are intentionally generic to prevent user enumeration.
- The debug keystore SHA-1 must be registered in Firebase Project Settings for authentication to work on development builds. Release builds require a separate release keystore registration.
