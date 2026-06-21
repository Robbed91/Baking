# BakeBook

BakeBook is a native Android baking recipe organiser for Google Pixel devices. It is built with Kotlin, Jetpack Compose, Material 3, Room, Navigation Compose, Coroutines, MVVM, and the repository pattern.

## Features

- Create, duplicate, favourite, search, filter, sort, share, and delete baking recipes.
- Store ingredients in `oz`, with half, normal, and double recipe scaling.
- Convert imperial baking measurements to metric with the built-in unit calculator.
- Save online recipe links with `http://` and `https://` validation.
- Store bake photos from gallery or camera preview with notes and linked recipe metadata.
- Browse photos in a grid and open a zoomable full-screen viewer with swipe-style previous/next navigation.
- Generate shopping list items from recipe ingredients, add manual items, tick items complete, delete items, and clear completed items.
- Use a separate Timers tab with independent bake countdown and cooling clocks, custom minute input, presets, and completion notifications.
- Export and import app data as JSON for backup and restore.
- Works offline using a local Room database.

## Screenshots

Add screenshots here after installing the generated APK on a Pixel device:

- Recipes tab
- Saved Links tab
- Photo Library tab
- Timers tab
- Recipe Detail screen
- Shopping List and Timers

## Build Instructions

Requirements:

- Android Studio
- JDK 17
- Android SDK 35 or newer

Build locally:

```bash
./gradlew build
./gradlew assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Build release APK:

```bash
./gradlew assembleRelease
```

The unsigned release APK is generated at:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

## Local Development

Open this repository in Android Studio, let Gradle sync, then run the `app` configuration on a Pixel emulator or physical Pixel device.

The application ID and package name are:

```text
com.robbiebedford.bakebook
```

## Download APK From GitHub

After a push or pull request build completes:

```text
Repository -> Actions -> Latest Workflow -> Artifacts -> Download APK
```

Available artifacts:

- `BakeBook-debug-apk`
- `BakeBook-release-apk`
