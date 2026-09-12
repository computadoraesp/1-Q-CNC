# UNO Q DROID CNC

**Industrial CNC Controller & HMI for Arduino UNO Q (QRB2210 LinuxCNC + STM32U585 Real-Time Motion Control)**

---

## 0. Project Metadata & Governance

- **Application Name**: UNO Q CNC (`com.aistudio.unoqcnc.rzxwtk`)
- **Target Audience**: CNC machine operators, machinists, embedded engineers, PCB fabricators, and makers using Arduino UNO Q, GRBL, grblHAL, or LinuxCNC.
- **Platform Architecture**: Clean MVVM with Unidirectional Data Flow (UDF) in 100% Kotlin and Jetpack Compose (Material 3).
- **Supported SDK**:
  - `minSdk`: 24 (Android 7.0 Nougat)
  - `targetSdk`: 36 (Android 16)
  - `compileSdk`: 36

---

## 1. Architecture & Package Structure

```
com.example/
├── MainActivity.kt                # Single activity container, edge-to-edge, responsive navigation
├── model/
│   └── CncModels.kt               # Immutable domain models, WCS, Grbl parameters, Kinematics, Mesh & Calibration models
├── transport/
│   ├── CncTransport.kt            # Core transport interface & real-time GRBL parser (<Status|WPos|FS|Pn|Ov>)
│   ├── MockCncEngine.kt           # Mock engine for instant simulation & sandbox testing
│   └── serial/
│       └── UsbSerialCncTransport.kt # USB-OTG CDC serial driver (CH340, CP2102, FTDI, PL2303)
├── engine/
│   └── GCodeKinematicsAndMeshEngine.kt # Kinematic time estimator, Bilinear Z mesh interpolation, Multipoint calibration stats
├── viewmodel/
│   └── CncViewModel.kt            # StateFlow holder, coroutine execution, UDF actions
├── ui/
│   ├── components/                # Modular composables (DRO, Jog, Toolpath, Fixtures, Calibration, Mesh, E-Stop)
│   ├── screens/                   # High-level screens (Control, G-Code, Diagnostics, Tools/WCS, Commissioning, Settings)
│   └── theme/                     # Industrial high-contrast Material 3 Dark/Light palettes and typography
```

---

## 2. Key Features

1. **Digital Readout (DRO) & 4-Axis Jogging**:
   - Real-time $X, Y, Z, A$ position tracking (WPos and MPos).
   - Touch jog pad with configurable step increments ($0.01\text{mm} - 50\text{mm}$) and continuous jog.
2. **Kinematic Job Time Estimator**:
   - Full parser for $G0$ (rapid), $G1/G2/G3$ (feedrate), $G4$ (dwells), and $M6$ (tool changes).
   - Envelope bounding box and metric distance breakdown per axis.
3. **PCB Auto-Leveling & Surface Mesh (Bilinear Z Compensation)**:
   - Configurable $N \times M$ probe grid with $G38.2$ probing.
   - Real-time heat map visualizer and direct transformation of G-Code with Z height compensation.
4. **Multipoint Linear Axis Calibration & Uncertainty**:
   - Guided calibration every $10\%$ of travel stroke for $X, Y, Z, A$.
   - Live error curve, maximum deviation, standard deviation ($\sigma$), lost step calculation, and direct EEPROM writing ($100..$103).
5. **Fail-Safe Industrial Controls**:
   - Prominent E-Stop ($0x18$), Feed Hold (`!`), Cycle Resume (`~`), Driver Enable/Disable, Spindle Overrides, and Coolant relays.

---

## 3. Build & Test Commands

- **Build Debug APK**: `gradle :app:assembleDebug`
- **Run Unit Tests (Robolectric / JUnit)**: `gradle :app:testDebugUnitTest`
- **Verify Screenshot Tests (Roborazzi)**: `gradle :app:verifyRoborazziDebug`
- **Record Baseline Screenshots**: `gradle :app:recordRoborazziDebug`
