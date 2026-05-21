# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Native Android app written in Kotlin with Jetpack Compose. Measures EMF (magnetic field) using the device magnetometer and displays real-time readings, FFT analysis, compass heading, WiFi power density, and recorded sessions.

- `minSdk 24`, `targetSdk/compileSdk 36`
- Kotlin 2.1.20, Compose BOM 2025.03.00, Room 2.7.0
- Version catalog: `gradle/libs.versions.toml`

## Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew installDebug           # Build and install on connected device
./gradlew assembleRelease        # Build release APK
```

No test or lint scripts are configured. Type-checking is IDE-only.

## Architecture

### Single ViewModel pattern

`MainViewModel` (`app/src/main/java/com/xbjornsen/emftracker/viewmodel/MainViewModel.kt`) is the sole ViewModel and the only state owner. All screens receive it directly — there are no intermediate state holders. All mutable state is exposed as `StateFlow`.

### Data layer (`data/`)

Each manager wraps an Android system service and emits state via `StateFlow`:

- **`MagnetometerManager`** — registers `SensorEventListener`, debounces changes < 0.5 µT, keeps a 600-point history, maintains a 256-sample ring buffer for FFT, computes DC/AC separation via slow EMA (~5 s time constant), and measures live sample rate via EWMA. Switches to `SENSOR_DELAY_FASTEST` when FFT is enabled.
- **`AccelerometerManager`** — provides gravity vector for tilt-compensated compass and detects motion instability to suppress magnetometer readings.
- **`RssiManager`** — polls WiFi RSSI and converts it to power density (µW/m²) via antenna effective aperture formula.
- **`FftAnalyzer`** — stateless object; called from a `Dispatchers.Default` coroutine in `MainViewModel` to run FFT on 256-sample windows.
- **`SessionRepository`** — wraps the Room DAO; sessions are stored in `emf_tracker.db`.

### Models (`data/models/`)

- **`SensorReading`** — `x, y, z, magnitude`; magnitude computed eagerly.
- **`AlertLevel`** enum — `NORMAL` (< 20 µT), `ELEVATED` (< 50), `HIGH` (< 100), `VERY_HIGH` (≥ 100). The `Float.toAlertLevel()` extension in `Models.kt` is the single source of truth for thresholds.
- **`FftResult`** — dominant frequency, 50/60 Hz flags, bin list, peaks.
- **`MotionState`**, **`RssiState`**, **`WifiPowerEstimate`** — supporting analysis models.

### Navigation (`ui/navigation/AppNavigation.kt`)

`NavHost` with five bottom-nav tabs: **Live**, **Compass**, **Chart**, **Sessions**, **About**. Each composable receives `MainViewModel` directly.

### Screens (`ui/screens/`)

- **LiveScreen** — radial gauge, per-axis readings, feature toggles (baseline, FFT, motion, RSSI, haptic)
- **CompassScreen** — tilt-compensated heading using `SensorManager.getRotationMatrix`; degrades gracefully when tilted > ~72° from horizontal
- **ChartScreen** — history line chart, AC/DC breakdown, FFT bar chart, waveform, WiFi power density
- **SessionsScreen** — start/stop recording, list/delete saved sessions
- **AboutScreen** — calibration guide, alert level explanations

### Feature flags in ViewModel

`baselineEnabled`, `fftEnabled`, `motionEnabled`, `rssiEnabled` are `MutableStateFlow<Boolean>` in `MainViewModel`. Toggling them starts/stops the corresponding managers and side-effects (e.g., toggling FFT re-registers the magnetometer at `SENSOR_DELAY_FASTEST` and starts the FFT coroutine pipeline).

### Compass heading

Tilt-compensated when the accelerometer is running: fuses gravity from `AccelerometerManager` with the magnetic vector via `SensorManager.getRotationMatrix`. Falls back to flat-phone `atan2(-y, x)` when gravity is unavailable.

### Database

Room singleton (`SessionDatabase`). One entity: `SessionEntity` mapped to/from the domain `Session` model by `SessionRepository`. Database file: `emf_tracker.db`.
