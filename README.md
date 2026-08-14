# Omok Android

A Gomoku/Omok game for Android with multiplayer support via Firebase and single-player AI opponent.

## Project Status

Early development - Core gameplay implementation (Issue #1)

## Setup

1. Clone the repository
2. Open in Android Studio
3. Ensure you have Android SDK 34+ installed
4. Build and run on emulator or device

## Architecture

- **UI**: Jetpack Compose
- **Database**: Firebase Realtime Database
- **Game Logic**: Kotlin (game rules, win detection, AI)
- **Navigation**: Jetpack Navigation Compose

## Building

```bash
./gradlew build
./gradlew installDebug
```

## Testing

```bash
./gradlew test
```
