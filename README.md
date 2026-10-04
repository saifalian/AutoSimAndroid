# AutoSim Android

![AutoSim Android preview](docs/screenshots/preview.svg)

## Short Description

Save tap points, read screen text, and run simple phone automation flows.

## About This Project

AutoSim Android is an Android automation project. It helps a user save screen positions, build repeatable actions, scan selected screen areas with OCR, and run actions when matching text is found.

The goal is to keep the project easy to understand, easy to run, and useful for learning or further development.

## Purpose And Idea

**Purpose:** The purpose of this project is to make phone automation easier to understand. It explores how an Android app can save tap points, scan screen text with OCR, and run small workflows controlled by the user.

**Idea:** The idea came from the need to repeat the same phone actions many times. Instead of tapping manually again and again, the app stores positions and steps so they can be tested as a sequence.

**Why I made it:** I made this to learn Android accessibility, overlays, OCR, Room database storage, and automation flow design in one project.

## Screenshots

### Real Android emulator screenshot

![Real Android emulator screenshot](docs/screenshots/real-app.png)

### Project preview

![Project preview](docs/screenshots/preview.svg)

### Real source structure

![Real source structure](docs/screenshots/source-structure.svg)

## Main Features

- Save tap positions and automation steps
- Create OCR regions for screen text checks
- Run tap, wait, and scan sequences
- Use floating overlay controls while testing
- Store automation data locally with Room
- Import/export saved automation data

## Tech Stack

- Kotlin
- Jetpack Compose
- Room
- ML Kit OCR
- Accessibility Service

## Project Location

Main local folder:

```text
D:\PROJECTS\AutoSimAndroid
```

GitHub repository:

https://github.com/saifalian/AutoSimAndroid

## Project Structure

```text
app/src/main/          Android app source
app/src/main/java/     Kotlin source files
gradle/                Gradle wrapper files
README.md              Project documentation
```

## How To Run

1. Open the project in Android Studio.
2. Let Gradle sync finish.
3. Build with gradle assembleDebug, or use Android Studio Run.
4. Install on an Android device or emulator.
5. Enable Accessibility, overlay, and screen capture permissions only when needed.

## Build Check

Build check: Gradle wrapper files were restored, a broken Kotlin screen-capture service block was fixed, assembleDebug completed successfully, and the app was installed and opened on an Android emulator for the real screenshot.

## Current Status

This project is uploaded to GitHub and prepared as a portfolio-style repository. More improvements can be added later, such as real app screenshots, demo videos, releases, and issue templates.

## Safety Note

Use this only for user-controlled automation. Check every click point before running it on important apps.

## License

No license file is included yet. Add a license before using this project as an open-source project.
