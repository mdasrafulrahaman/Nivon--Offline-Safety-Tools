# Implementation Plan - Animated Splash Screen

The goal is to implement an animated splash screen for Nivon that keeps the logo centered and provides a "cool" transition into the app. We will use the `androidx.core:core-splashscreen` API to achieve this.

## Proposed Changes

### Logic & UI Improvements

#### [MODIFY] [MainActivity.kt](file:///D:/Android%20Studio%20Apps/Nivon/app/src/main/java/com/asraful/nivon/MainActivity.kt)
- **Splash Screen Control**:
    - Store the `SplashScreen` instance returned by `installSplashScreen()`.
    - Use `setKeepOnScreenCondition` to keep the splash screen visible until the initial data (onboarding status, theme) is loaded from DataStore.
    - Implement `setOnExitAnimationListener` to add a "cool" exit animation. The animation will involve:
        - Scaling up the icon.
        - Fading out the icon and the background.
        - Using an `AnticipateInterpolator` or `OvershootInterpolator` for a more dynamic feel.

### Resource Verification

#### [VERIFY] [themes.xml](file:///D:/Android%20Studio%20Apps/Nivon/app/src/main/res/values/themes.xml)
- Ensure `Theme.Nivon.Starting` is correctly configured with `windowSplashScreenAnimatedIcon` pointing to the logo. (Already done in previous task, but will double-check).

## Verification Plan

### Automated Tests
- Build the project using `./gradlew assembleDebug`.

### Manual Verification
- **Splash Screen Appearance**: Verify the logo is perfectly centered.
- **Loading State**: Verify the splash screen stays on screen until the background is ready (no flickering white screen).
- **Exit Animation**: Verify the "cool" scale-up and fade-out transition when the app content appears.
