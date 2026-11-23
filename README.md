# Thrift Manager

A native Android business-management app for thrift shops, built with Kotlin,
Jetpack Compose, Material 3, Room, Coroutines/Flow, Hilt, and DataStore.

## Features

- Local-first inventory, supplier, customer, purchase, sale, and expense records.
- Transaction-safe sales and purchase stock updates with stock/amount validation.
- Dashboard totals and basic revenue, cost, expense, and profit reporting backed
  by persisted records.
- JSON backup and restore, and CSV export through Android's Storage Access
  Framework.
- A GitHub Actions workflow that builds and uploads a debug APK.

## Build

Open the project in Android Studio (JDK 17; Android SDK 35), or run:

```shell
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Data and backups

Business records remain on-device in Room. Use **More → Backup** to export or
restore a JSON backup. Restoring replaces the local database only after an
explicit confirmation.
