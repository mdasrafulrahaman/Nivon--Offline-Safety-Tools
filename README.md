# Nivon

Offline-first emergency and safety assistant for India, developed by **MD Asraful Rahaman**.

## What it does

- Keeps emergency contacts and a personal safety card on the device using Room.
- Provides offline first-aid guidance and offline search.
- Opens the system dialer for 112 and configurable India-focused emergency numbers; it never calls automatically.
- Offers user-controlled torch, siren, current-location capture and standard Android sharing.
- Uses contextual location permission requests only. It does not request background location, contacts, SMS, call-log, microphone, or storage permissions.
- Supports light/dark appearance and locally persisted onboarding/settings with DataStore.

## Privacy and safety

Nivon has no account system, advertisements, analytics, backend, or network data collection. Medical information and contacts remain in the local application database. Location is requested only when the user asks to get or share it and is not retained as history.

Nivon is not a Government of India application or emergency-service provider. Emergency number availability varies by location. First-aid content is general education, not a substitute for professional care.

## Build

Install Android SDK Platform 36 and use JDK 17+:

```powershell
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Release / Play Console

1. Create a private upload keystore (`keytool -genkeypair ...`) and keep it outside this repository.
2. Add a local signing configuration through `keystore.properties` (never commit it), then wire its values into the `release` signing config.
3. Build the bundle with `./gradlew :app:bundleRelease`.
4. Before submission, replace the legal URL placeholders with hosted privacy-policy, terms and about pages; complete Play Console Data safety and sensitive-permission declarations accurately.

The repository deliberately contains no signing key, secret, tracker, or debug-only emergency behavior.
