# Truck Repair Monitoring APK – Android project

This is a native Android starter project for the requested truck repair monitoring app.

## Included features
- Add repair record: plate number, repair part, date, amount (PHP)
- Edit and delete repair records (delete confirmation)
- Local Room database; records remain on the device and work offline
- Repair history grouped by truck plate number
- Total repair cost per truck
- Monthly report for each truck
- Overall fleet monthly report
- Month selection using `YYYY-MM`

## Build the APK
1. Install Android Studio (current stable version) on a computer.
2. Open this folder in Android Studio.
3. Allow Gradle to sync and download the Android dependencies.
4. Connect an Android phone or use an emulator to test.
5. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
6. Android Studio will show the generated APK location (normally `app/build/outputs/apk/debug/app-debug.apk`).

## Notes
- This deliverable is the Android Studio source project, not a precompiled APK.
- A computer with Android Studio and internet access is required for the first Gradle sync/build.
- The app stores data locally on one device. It does not currently sync across phones or back up to the cloud.
- Dates are entered as `YYYY-MM-DD`; monthly reports use `YYYY-MM`.
- Currency display is Philippine pesos (PHP).
