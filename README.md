# Magnetometer

A native Android app for measuring electromagnetic fields using the device's built-in magnetometer.

## Features

- **Live readings** — real-time magnetic field strength in µT with a radial gauge and per-axis (X/Y/Z) breakdown
- **Compass** — tilt-compensated heading using accelerometer + magnetometer fusion
- **FFT analysis** — frequency-domain breakdown to identify 50/60 Hz AC sources vs. static fields
- **Baseline mode** — capture and subtract ambient background to isolate field changes
- **Motion detection** — suppresses readings automatically when the phone is moving
- **WiFi power density** — estimates RF exposure from the connected network's RSSI
- **Session recording** — log readings over time, review peak/average, delete sessions

## Alert levels

| Level | Range | Meaning |
|-------|-------|---------|
| Normal | < 20 µT | Typical background |
| Elevated | 20–50 µT | Near household appliances |
| High | 50–100 µT | Close to motors or transformers |
| Very High | ≥ 100 µT | Direct contact with electrical equipment |

## Tech stack

- Kotlin + Jetpack Compose
- Single `MainViewModel` (AndroidViewModel) with `StateFlow`
- Room database for session persistence
- `minSdk 24`, `targetSdk 36`

## Building

```bash
./gradlew assembleDebug      # Build APK
./gradlew installDebug       # Build and install on connected device
```
