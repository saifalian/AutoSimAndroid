# AutoSim Android

AutoSim Android is an Android automation workspace for building, previewing, and running repeatable on-device interaction flows. It combines saved tap targets, draggable overlays, sequence execution, screen capture, OCR regions, phrase matching, and execution logs into a single Jetpack Compose application.

The project is designed for controlled automation scenarios where a user needs to define screen coordinates, group actions into reusable sequences, and optionally trigger actions from recognized text on the screen.

## Features

- **Click spot management**: create, edit, and store named tap targets with repeat and delay settings.
- **Overlay tooling**: display floating overlay controls for execution and preview workflows.
- **Sequence builder**: compose ordered automation steps such as clicks, waits, and OCR scans.
- **Accessibility execution**: perform taps through an Android accessibility service.
- **Screen capture support**: request MediaProjection permission for OCR-enabled workflows.
- **OCR regions**: define rectangular regions and scan only the selected area.
- **Phrase-triggered actions**: match recognized text against configured phrases and run mapped click actions.
- **Run controls**: start, pause, resume, and stop sequence execution.
- **Logs and history**: persist run events, errors, successful actions, and OCR text history with Room.
- **Import/export utilities**: supporting utilities for moving saved automation data between environments.

## How It Works

AutoSim stores automation data locally with Room. Users define click spots and OCR regions from the app UI, then combine them into sequences. When a sequence runs, `SequenceRunner` loads the ordered steps from the database and executes them through the accessibility service.

For OCR steps, the app uses Android screen capture through `MediaProjection`, crops the latest screen bitmap to the configured region, and passes it through Google ML Kit Text Recognition. Recognized text can be checked against phrase groups, and matching phrases can trigger stored click spots.

## Permissions

AutoSim uses several sensitive Android capabilities because it automates device interactions:

- **Accessibility Service**: required to perform tap gestures.
- **Display Over Other Apps**: required for floating overlay controls.
- **Media Projection / Screen Capture**: required for OCR region scanning.
- **Foreground Service**: keeps overlay and capture services active while automation is running.
- **Internet**: available for dependencies or future network-enabled workflows.

Only enable these permissions if you understand the automation being configured. The app is intended for user-controlled automation, testing, and productivity workflows.

## Tech Stack

- Kotlin
- Android Gradle Plugin
- Jetpack Compose
- Material 3
- Navigation Compose
- Room
- Kotlin Coroutines and Flow
- Google ML Kit Text Recognition
- Moshi
- Android Accessibility APIs
- Android MediaProjection APIs

## Project Structure

```text
app/src/main/java/com/example/autosim
├── accessibility/       # Accessibility service used for gesture execution
├── db/                  # Room entities, DAOs, and database setup
├── engine/              # Sequence runner, screen capture, and OCR processing
├── overlay/             # Floating overlay and coordinate utilities
├── ui/                  # Compose theme, tabs, and dialogs
├── utils/               # Utility helpers such as import/export and density conversion
├── Broadcast.kt
└── MainActivity.kt
```

There are also legacy package paths under `com.autosim` for overlay and click helper components.

## Requirements

- Android Studio
- JDK 17 or compatible Android Studio bundled JDK
- Android SDK with API 36 installed
- Gradle 8.13 compatible environment
- Android device or emulator running Android 7.0+ (`minSdk 24`)

## Build

Open the project in Android Studio and allow Gradle sync to complete.

From a terminal with Gradle available:

```powershell
gradle assembleDebug
```

If you add Gradle wrapper scripts later, the equivalent command will be:

```powershell
.\gradlew.bat assembleDebug
```

## Basic Usage

1. Install and open the app.
2. Enable the AutoSim accessibility service when prompted.
3. Grant overlay permission from the settings or overlay controls flow.
4. Request screen capture permission if OCR workflows are needed.
5. Create click spots for important screen coordinates.
6. Create OCR regions if text-triggered actions are needed.
7. Build a sequence from clicks, waits, and OCR scan steps.
8. Start the overlay or run the sequence, then monitor logs from the Logs tab.

## Current Status

This is an early Android automation project. Core click execution, OCR region scanning, phrase matching, local persistence, and logging are present. Some advanced sequence step types are represented in the model but are still planned for deeper behavior:

- Text detection sequence steps
- Repeat block steps
- Conditional branching steps
- Production migration strategy for Room schema changes

## Safety Notes

Automation apps can interact with other apps on the user's behalf. Review every configured click spot and sequence before running it, especially on screens involving payments, account settings, irreversible actions, or private data.

## License

No license has been selected yet. Add a license before distributing or accepting external contributions.

