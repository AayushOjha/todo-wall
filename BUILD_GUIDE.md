# Taskarma Build Guide

This guide provides short commands and steps for building testing and release versions of the app.

## 1. Building for Testing (Debug APK)
Generates a debuggable APK for local testing and development.
```bash
./gradlew :app:assembleDebug
```
**Output Path:** `app/build/outputs/apk/debug/app-debug.apk`

---

## 2. Building for Release

### Release APK
Generates a production-ready APK for direct distribution.
```bash
./gradlew :app:assembleRelease
```
**Output Path:** `app/build/outputs/apk/release/app-release.apk`

### Release App Bundle (AAB)
Generates an Android App Bundle for uploading to the Google Play Store.
```bash
./gradlew :app:bundleRelease
```
**Output Path:** `app/build/outputs/bundle/release/app-release.aab`

---

## 3. Required for Release Signing
The project is configured to read signing information from `local.properties`. For a secure release, add the following to your `local.properties` file:

```properties
RELEASE_STORE_FILE=/path/to/your/keystore.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

**Important:** 
- Ensure the keystore file exists at the specified path.
- Keep `local.properties` and your `.jks` file secure and never commit them to version control.

---

## 4. Next Steps

### After Building Debug APK
- **Install on device:** Run `adb install app/build/outputs/apk/debug/app-debug.apk` or use the Play button in Android Studio.
- **Debug:** Use the "Debug 'app'" option in Android Studio for step-by-step troubleshooting.

### After Building Release APK/AAB
- **Testing:** Always test the `release` APK on a physical device before distribution, as R8/ProGuard (minification) can sometimes cause runtime issues.
- **Play Store Upload:** 
  1. Open the [Google Play Console](https://play.google.com/console/).
  2. Upload the `.aab` (App Bundle) to the appropriate track (Internal, Closed, or Production).
- **Versioning:** Remember to increment `versionCode` and `versionName` in `app/build.gradle.kts` for each new release.
