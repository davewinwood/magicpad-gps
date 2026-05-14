# MagicPad GPS

Prototype Android app for sharing a phone's GPS position to an Honor MagicPad over the same Wi-Fi network.

The first prototype is intentionally simple:

- Install the same app on both devices.
- On the phone, use **Phone: Share GPS**.
- On the MagicPad, enter the phone IP address and use **MagicPad: Receive GPS**.
- If **Use as tablet location for Google Maps** is enabled, the receiver attempts to inject each received fix into Android mock location.

## Current Status

Implemented:

- Single Android app project.
- Phone share mode using Android `LocationManager`.
- Local TCP GPS stream on port `42523`.
- MagicPad receive mode using manual phone IP.
- Optional Android mock-location injection targeting Google Maps.
- Foreground services and persistent notifications.
- In-app shortcut to Developer Options.

Still to add:

- Automatic device discovery with Android NSD/mDNS.
- Pairing code/QR setup.
- Stronger transport security.
- Reconnect logic.
- Better visual status and diagnostics.

## Build

Open this folder in Android Studio and let it sync the Gradle project.

The project currently targets:

- Android Gradle Plugin `8.7.3`
- `compileSdk 35`
- Java source
- No third-party runtime libraries

I could not compile it in this workspace because Java, Android Studio, and the Android SDK are not installed here.

## Build Without Local Android Tools

If you do not want to install Android Studio yet, push this folder to a GitHub repository and run the included GitHub Actions workflow:

`.github/workflows/android-build.yml`

The workflow builds a debug APK and publishes it as an artifact named `magicpad-gps-debug-apk`.

That route still needs a GitHub account/repository, but it avoids installing Java, Gradle, and the Android SDK on this PC.

See the step-by-step guide in `docs/github-build-guide.md`.

## MagicPad Google Maps Test

1. Install the app on the phone and MagicPad.
2. On the MagicPad, enable Developer Options.
3. In Developer Options, set **MagicPad GPS** as the mock location app.
4. Connect both devices to the same Wi-Fi.
5. Open the app on the phone and tap **Start sharing from this device**.
6. Note the phone IP address shown in the app.
7. Open the app on the MagicPad.
8. Enter the phone IP address.
9. Enable **Use as tablet location for Google Maps**.
10. Tap **Connect to phone**.
11. Open Google Maps on the MagicPad and check whether the blue dot follows the phone.

The first success criterion is practical: Google Maps on the MagicPad should move to the phone's location within a few seconds and continue following during a short outdoor walk.
