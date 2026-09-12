# Privacy Policy for 1 Q CNC

**Last updated:** September 2026

1 Q CNC ("the Application") is an industrial Human-Machine Interface (HMI) designed to control Computer Numerical Control (CNC) machinery via USB Serial OTG, Bluetooth SPP, or local area network (LAN/Wi-Fi).

## 1. Zero Data Collection & Offline-First Operation
- The Application operates **100% offline and locally on your device**.
- We do **NOT** collect, store, transmit, monetize, or share any personal identity information, user accounts, telemetry, email addresses, passwords, or biometrics.
- No analytics, third-party advertising SDKs, or tracking beacons are included.

## 2. Device Permissions Used
The Application strictly requests only hardware permissions necessary to interface with machine tool electronics:

- **USB Host (`android.permission.USB_PERMISSION`)**:
  Used solely to transfer raw G-code blocks and movement commands (`G0`, `G1`, feed hold, emergency stop) via standard USB CDC-ACM, FTDI, CP2102, or CH340 serial chips to the CNC motion controller.
- **Bluetooth (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`)**:
  Used solely to establish a wireless Serial Port Profile (SPP) data bridge with CNC electronics. The permission is declared with `neverForLocation` in the Android Manifest, guaranteeing that your physical location is never tracked or accessed.
- **File Access**:
  Utilizes the standard, privacy-preserving Android Document Picker (`ActivityResultContracts.OpenDocument` / `CreateDocument`). The Application never requests broad storage access (`READ_EXTERNAL_STORAGE`). G-code programs and machine calibration data remain stored locally in the secure Room database on the device.
- **Internet / Local Network (`INTERNET`, `ACCESS_NETWORK_STATE`)**:
  Used exclusively for local LAN sockets (TCP/WebSocket) when communicating directly with local CNC controllers such as LinuxCNC or 1 Q Dual-Core Linux MPU on your local factory network. No internet telemetry is transmitted.

## 3. Data Security and Local Storage
All axis configuration presets, tool offset tables, Work Coordinate Systems (G54–G59), and leveling mesh grids are stored locally on the Android device's private application sandbox.

## 4. Contact & Source Repository
For technical inquiries or open-source issues, visit the application's project repository.
