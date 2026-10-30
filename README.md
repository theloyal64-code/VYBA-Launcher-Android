# VYBA Launcher Complete Edition — v1.0.2 source

## Implemented in this release
- Android HOME launcher entry point and system wallpaper background.
- Swipe-up app drawer, app search, app launch and long-press add shortcut.
- Home shortcuts with persistent ordering and drag-to-reorder support.
- Shortcut rename, custom photo icon, reset and remove.
- Icon shape, size, home/drawer columns, spacing, drawer transparency and wallpaper dim controls.
- Home folders: create, rename, choose apps, open, edit and delete.
- Android home-screen widgets: provider picker, system bind/permission flow, optional provider configuration, host views and persisted widget IDs.
- Gestures: swipe up for app drawer, swipe down from top area for settings, double-tap home background for app drawer.
- Local-only preferences; no account, ads, backend or analytics.

## Build requirements
Open this project in Android Studio with JDK 17 and Android SDK platform 35 installed. Allow Gradle sync, then Build > Build APK(s). Gradle plugin 8.7.3 and Kotlin 2.0.21 are configured. Minimum SDK 23, target SDK 35.

## Validation status
This is an expanded source release, not a compiled APK. The Android SDK/Gradle build toolchain was not available in the authoring environment, so compilation and physical-device behavior remain unverified. Widget binding and configuration are mediated by Android and may vary by device/OS version. Please build and test on the target device before using as the default launcher.
