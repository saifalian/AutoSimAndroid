# AutoSim Android

AutoSim Android is an Android app for making simple phone automation flows.

In easy words, this app lets a user save places on the screen, then run those taps again later. It can also read text from selected screen areas using OCR. If the app finds a matching word or phrase, it can run a saved action.

This project is useful for learning how Android automation, accessibility services, screen capture, OCR, and local app storage can work together.

## What This App Can Do

- Save tap positions on the screen.
- Group taps, waits, and OCR checks into a sequence.
- Run saved sequences from the app.
- Use a floating overlay while automation is running.
- Read text from selected parts of the screen.
- Run an action when a selected word or phrase is found.
- Save logs, OCR history, and automation data locally.
- Import or export saved automation data.

## How It Works In Simple Words

The app stores the user's saved tap points, OCR regions, and sequences in a local Room database.

When a sequence starts, the app reads the saved steps and runs them one by one. Tap actions are performed through Android's accessibility service.

For OCR steps, the app takes a screen capture, cuts out the selected area, and sends that image to Google ML Kit Text Recognition. The returned text is then checked against saved phrases.

## Permissions Used

The app needs some Android permissions because it interacts with the screen:

- **Accessibility Service**: required to perform tap gestures.
- **Display Over Other Apps**: required for floating overlay controls.
- **Media Projection / Screen Capture**: required for OCR region scanning.
- **Foreground Service**: keeps overlay and capture services active while automation is running.
- **Internet**: available for dependencies or future network-enabled workflows.

Only enable these permissions when you understand what the automation will do. This project is meant for user-controlled testing, learning, and productivity workflows.

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

This is an early Android automation project. The main ideas are already present, including tap automation, OCR scanning, phrase matching, local saving, and logs.

Some advanced features are planned or still need more work:

- Text detection sequence steps
- Repeat block steps
- Conditional branching steps
- Production migration strategy for Room schema changes

## Safety Notes

Automation apps can interact with other apps on the user's behalf. Review every configured click spot and sequence before running it, especially on screens involving payments, account settings, irreversible actions, or private data.

## License

No license has been selected yet. Add a license before distributing or accepting external contributions.
