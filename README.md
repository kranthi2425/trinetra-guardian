# TRINETRA - Your Digital Guardian

Privacy-first Android child-safety and mobile-security prototype for CYBER Helps Technology Pvt Ltd.

## Functional v0

- Kotlin + Jetpack Compose family dashboard
- APK Shield using Android `PackageManager` APIs, a bundled public package-name IOC snapshot, permission-cluster heuristics, installer-source signals and explicit unavailable states
- User-triggered SOS SMS composer with location, battery and timestamp
- Read-only `UsageStatsManager` screen-time view with Android's usage-access flow
- Digital Arrest Shield education and checklist

APK Shield reuses the IOC dataset and scoring concepts from [`android-stalkerware-scan`](https://github.com/kranthi2425/android-stalkerware-scan), not its desktop Python/privileged ADB scanner. Every app signal is reimplemented through documented Android APIs. A signal unavailable on a device/API remains unavailable and is never converted to false.

`KNOWN IOC MATCH` means an exact package-name match in the bundled public indicator snapshot. It is not forensic proof. Permission-only findings can be legitimate parental-control, accessibility, antivirus, device-management or OEM apps. Review the app, developer, install context and user expectations before acting. Removing suspected monitoring software can alert an abuser or destroy evidence.

Android 11+ limits package visibility. APK Shield declares `QUERY_ALL_PACKAGES` because inventory is its core security function, but Play distribution requires the Google Play permission declaration and review. Coverage can still vary by device and OEM.

## Build and test

Open the project in Android Studio with JDK 17 and Android SDK 35, or use GitHub Actions. CI runs app-native unit tests, then builds debug and prototype release APKs:

```bash
gradle testDebugUnitTest
gradle assembleDebug assembleRelease
```

Tests cover unavailable installer/permissions, an exact IOC match, a benign Play-installed app, legitimate parental-control-style permissions, a risky surveillance cluster and Android 11+ visibility messaging. The bundled IOC count is dataset size, not validation of APK Shield. Emulator and real-device smoke tests are still required for inventory completeness, OEM behavior, Usage Access, location and SMS handoff.

The release APK uses prototype signing only. Production requires a private release key stored in encrypted CI secrets.

## Privacy

No message reading, Accessibility control, hidden app locking, background surveillance, analytics, ads or backend account. The privacy policy is at https://kranthi2425.github.io/trinetra-guardian/.
