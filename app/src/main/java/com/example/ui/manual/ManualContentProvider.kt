package com.example.ui.manual

import androidx.compose.ui.graphics.Color
import com.example.model.AppLanguage
import com.example.ui.theme.*

object ManualContentProvider {

  fun getContent(language: AppLanguage): ManualFullContent {
    return when (language) {
      AppLanguage.ES -> getSpanishContent()
      AppLanguage.DE -> getGermanContent()
      AppLanguage.FR -> getFrenchContent()
      AppLanguage.JA -> getJapaneseContent()
      AppLanguage.KO -> getKoreanContent()
      AppLanguage.EN -> getEnglishContent()
    }
  }

  // ==========================================
  // 1. ENGLISH CONTENT (DEFAULT)
  // ==========================================
  private fun getEnglishContent(): ManualFullContent {
    return ManualFullContent(
      language = AppLanguage.EN,
      screenTitle = "OPERATING MANUAL & TECHNICAL REFERENCE",
      screenSubtitle = "Complete documentation covering app architecture, hardware connection, and every button, field, and menu.",
      tabOverview = "1. What the App Does",
      tabConnection = "2. Device Connection",
      tabControls = "3. Buttons, Fields & Menus",
      searchPlaceholder = "Search button, input field, G-code, or menu (e.g., X+, E-Stop, OTG, M3)...",
      categoryAll = "ALL MENUS",
      emptySearchResults = "No controls or menus match your search query.",
      overview = ManualOverviewSection(
        title = "What Does 1 Q CNC Controller Do?",
        subtitle = "Industrial-grade CNC HMI designed for 1 Q hardware, LinuxCNC, and micro-stepper controllers.",
        paragraphs = listOf(
          "1 Q CNC Droid is a comprehensive Human-Machine Interface (HMI) built to transform Android tablets and smartphones into robust industrial CNC pendants and workstation consoles. It is specifically engineered to communicate with modern multi-core controllers such as 1 Q architecture (combining a Qualcomm QRB2210 Linux core with an STM32U585 real-time motion timing coprocessor), as well as standard GRBL, FluidNC, and LinuxCNC installations.",
          "The application acts as the master operator interface, managing real-time Digital Readout (DRO), manual jog motions across all Cartesian and rotary axes (X, Y, Z, A), direct G-code file streaming with 2D/3D toolpath verification, Work Coordinate Systems (G54 to G59), tool offset calibration, and hardware diagnostics down to individual HAL pins and step-timing jitter analysis.",
          "With zero-latency command pipelines, active hardware safety locks, and adaptive power-saving modes, it provides machine shop operators, prototyping engineers, and CNC fabricators with full machine control directly on the factory floor without requiring a bulky desktop computer."
        ),
        features = listOf(
          ManualFeatureHighlight(
            title = "Sub-Micron Multi-Axis DRO",
            description = "High-precision digital position readouts for axes X, Y, Z, and A with dual-mode display (Work Coordinates WCS and Machine Coordinates MCS).",
            iconName = "dro"
          ),
          ManualFeatureHighlight(
            title = "Tactile Jog Motion Engine",
            description = "Supports both single discrete step pulses (0.01 mm to 50 mm) and continuous touch-and-hold jog with instant stop safety target.",
            iconName = "jog"
          ),
          ManualFeatureHighlight(
            title = "Streaming G-Code & 3D Toolpath",
            description = "Line-by-line streaming with real-time execution feedback, graphical 2D/3D toolpath visualization, and surface auto-level height mesh compensation.",
            iconName = "gcode"
          ),
          ManualFeatureHighlight(
            title = "MDI Terminal & Custom Macros",
            description = "Manual Data Input command line with syntax highlighting, quick macro presets (Z-probe, park, warmup), and historical command recall.",
            iconName = "mdi"
          ),
          ManualFeatureHighlight(
            title = "WCS & Tool Offset Management",
            description = "Quick-switching between work coordinate frames G54 through G59, G10 L2/L20 zeroing, and comprehensive tool geometry compensation.",
            iconName = "tools"
          ),
          ManualFeatureHighlight(
            title = "Deep Hardware Diagnostics",
            description = "Real-time inspection of HAL logic pins, limit switches, probe contacts, E-Stop circuit, step generator frequency, and CPU core temperature.",
            iconName = "diagnostics"
          )
        )
      ),
      connectionGuide = ManualConnectionGuide(
        title = "Hardware Connection Guide (Android to PC or CNC Board)",
        subtitle = "Detailed instructions for connecting your Android device to the machine controller via USB OTG, Bluetooth SPP, or Virtual Simulation.",
        methods = listOf(
          ConnectionMethodGuide(
            id = "usb_serial",
            name = "USB Serial OTG Connection (Recommended for Production)",
            badge = "HIGHEST RELIABILITY & LOW LATENCY",
            hardwareRequirements = "Android device with USB Host (OTG) support, high-quality shielded USB OTG adapter/cable, and a CNC board with FTDI, CH340, CP2102, or STM32 CDC ACM USB chip.",
            stepByStep = listOf(
              "Step 1: Connect the USB OTG adapter directly into your Android phone or tablet's charging port.",
              "Step 2: Connect a shielded USB cable from the adapter to your CNC controller board (Arduino UNO Q, GRBL Shield, or LinuxCNC interface).",
              "Step 3: Android will display an authorization prompt: 'Allow UNO Q CNC to access this USB device?'. Select 'Always allow' and tap 'OK'.",
              "Step 4: In the app, navigate to the 'Settings' tab. Under 'TRANSPORT SELECTION', select 'USB Serial'.",
              "Step 5: Verify your board's baud rate (Default: 115200 bps for GRBL/UNO Q, or 250000 bps for high-speed boards).",
              "Step 6: Tap the blue 'CONNECT' button. The status badge in the top bar will turn solid green 'USB CONNECTED' and the DRO will populate with live positions."
            ),
            recommendedSettings = "Baud: 115200 / 250000 | Data bits: 8 | Stop bits: 1 | Parity: None (8N1) | DTR/RTS: Enabled"
          ),
          ConnectionMethodGuide(
            id = "bluetooth_spp",
            name = "Bluetooth Wireless Serial SPP (Cordless Pendant)",
            badge = "WIRELESS MOBILITY",
            hardwareRequirements = "Bluetooth SPP module (HC-05, HC-06, or ESP32 with Serial Bluetooth firmware) wired to the controller board's TX/RX UART pins (5V/3.3V).",
            stepByStep = listOf(
              "Step 1: Power on your CNC controller and ensure the Bluetooth module's LED is blinking (waiting for pairing).",
              "Step 2: In Android System Settings -> Bluetooth, pair with the module (Default pairing PIN is typically 1234 or 0000).",
              "Step 3: Open UNO Q CNC, go to 'Settings' tab, and set Transport to 'Bluetooth SPP'.",
              "Step 4: Under the 'BLUETOOTH LINK MANAGER' section, tap 'SCAN DEVICES'.",
              "Step 5: Tap your paired module in the list and click 'CONNECT'.",
              "Step 6: The LED on the Bluetooth module will turn steady, and the app will display 'BLUETOOTH CONNECTED'."
            ),
            recommendedSettings = "Range: Up to 10 meters line-of-sight. Ensure baud rate matches module firmware (typically 115200 or 9600)."
          ),
          ConnectionMethodGuide(
            id = "virtual_sim",
            name = "Built-in Virtual Simulator (Offline Testing)",
            badge = "OFFLINE & LEARNING",
            hardwareRequirements = "No external hardware required. Operates completely in-memory on the Android device.",
            stepByStep = listOf(
              "Step 1: In the 'Settings' tab, select 'Virtual Simulator (Offline)'.",
              "Step 2: The system starts an internal physics motion engine that emulates kinematic position, acceleration, limit switches, and spindle PWM responses.",
              "Step 3: Use this mode to preview G-code programs, practice jog workflows, and test macros without risking tool collisions."
            ),
            recommendedSettings = "No configuration needed. Ideal for training operators and verifying toolpaths."
          )
        ),
        troubleshootingTitle = "Connection Troubleshooting & Signal Integrity",
        troubleshootingItems = listOf(
          TroubleshootingStep(
            problem = "Android does not detect the USB device / No popup appears",
            cause = "Faulty charging-only cable, OTG adapter not recognized, or Android OTG storage setting is disabled.",
            solution = "Ensure you are using a 4-wire data USB cable with OTG pin grounded. On some phones (e.g. OnePlus/Oppo/Xiaomi), enable 'OTG Connection' in Android Settings -> System."
          ),
          TroubleshootingStep(
            problem = "Spurious resets or communication freezes when spindle starts",
            cause = "Electromagnetic Interference (EMI) from the VFD (Variable Frequency Drive) or plasma arc interfering with the USB serial bus.",
            solution = "Use double-shielded USB cables with molded ferrite beads on both ends. Keep the Android USB cable physically separated from AC spindle power cables."
          ),
          TroubleshootingStep(
            problem = "Background disconnects after screen turns off",
            cause = "Android aggressive battery optimization putting the serial background thread to sleep.",
            solution = "Go to Android App Info -> UNO Q CNC -> Battery -> Set to 'Unrestricted' so USB communications are never suspended."
          )
        )
      ),
      menuCategories = listOf(
        ManualMenuCategory(
          id = "top_bar",
          title = "Top Bar & Global Safety Controls",
          subtitle = "Immediate access to critical safety controls, alarms, and live state indicators.",
          badgeColor = IndDarkEmergency,
          items = listOf(
            ManualControlItem(
              name = "E-STOP (Emergency Stop)",
              controlType = "Safety Pushbutton",
              menuLocation = "Top Navigation Bar (Always Visible)",
              gcodeOrCommand = "ASCII 0x18 / !",
              inputFormat = "Instant physical tap",
              description = "Triggers immediate hardware motor stop, disables all stepper drivers, cuts spindle power, and sets the controller state to ALARM. Use in any situation where collision, tool breakage, or operator injury is imminent.",
              safetyNotes = "CRITICAL: E-Stop holds the machine in hardware lockout until explicitly reset."
            ),
            ManualControlItem(
              name = "RESET ALARM (\$X)",
              controlType = "Action Button",
              menuLocation = "Top Navigation Bar",
              gcodeOrCommand = "\$X",
              inputFormat = "Tap to unlock",
              description = "Clears the GRBL / LinuxCNC alarm lock state after an E-stop or limit switch trigger. Allows the operator to jog the machine away from limit switches.",
              safetyNotes = "Verify the cause of the alarm has been physically cleared before unlocking."
            ),
            ManualControlItem(
              name = "CYCLE START (~)",
              controlType = "Action Button",
              menuLocation = "Top Navigation Bar",
              gcodeOrCommand = "~",
              inputFormat = "Single tap",
              description = "Resumes motion execution from a Feed Hold pause, or begins running the loaded G-code file.",
              safetyNotes = "Ensure toolpath is clear and protective safety guards are closed."
            ),
            ManualControlItem(
              name = "FEED HOLD (!)",
              controlType = "Action Button",
              menuLocation = "Top Navigation Bar",
              gcodeOrCommand = "!",
              inputFormat = "Single tap",
              description = "Decelerates all axes to a complete controlled stop while keeping the spindle running and coordinates intact. Useful for pausing execution to inspect cut quality or clear chips.",
              safetyNotes = "Spindle remains spinning; do not touch cutter."
            ),
            ManualControlItem(
              name = "Machine Status Indicator",
              controlType = "Status Badge",
              menuLocation = "Top Navigation Bar",
              gcodeOrCommand = "?",
              inputFormat = "Read-only display",
              description = "Displays the current real-time controller state: IDLE (ready), RUN (executing G-code), HOLD (paused), JOG (moving manually), ALARM (lockout), or HOME (homing sequence)."
            )
          )
        ),
        ManualMenuCategory(
          id = "control_screen",
          title = "CONTROL Menu (Main Operating Cockpit)",
          subtitle = "Digital Readout (DRO), Manual Jogging D-Pad, Spindle/Coolant controls, and Real-Time Overrides.",
          badgeColor = DroCyan,
          items = listOf(
            ManualControlItem(
              name = "Digital Readout (DRO) Axis Displays (X, Y, Z, A)",
              controlType = "DRO Display",
              menuLocation = "CONTROL -> Top Panel",
              gcodeOrCommand = "? (Status Report)",
              inputFormat = "Read-only numerical readout",
              description = "Displays the exact tool tip coordinate position for each axis with 3-decimal millimeter precision. Can be toggled between Work Coordinates (WCS) and Machine Coordinates (MCS)."
            ),
            ManualControlItem(
              name = "ZERO X / ZERO Y / ZERO Z / ZERO A Buttons",
              controlType = "Zeroing Button",
              menuLocation = "CONTROL -> DRO Panel",
              gcodeOrCommand = "G10 L20 P0 [Axis]0",
              inputFormat = "Single tap on respective axis",
              description = "Sets the current position of the specified axis as 0.000 in the currently active Work Coordinate System (e.g. G54) without altering machine coordinates.",
              safetyNotes = "Confirm tool is touched off correctly at the workpiece origin point."
            ),
            ManualControlItem(
              name = "ZERO ALL Button",
              controlType = "Zeroing Button",
              menuLocation = "CONTROL -> DRO Panel",
              gcodeOrCommand = "G10 L20 P0 X0 Y0 Z0 A0",
              inputFormat = "Single tap",
              description = "Zeros all axes simultaneously at the current tool contact point.",
              safetyNotes = "Make sure Z zero is referenced to the correct top surface or wasteboard."
            ),
            ManualControlItem(
              name = "HOME ALL (\$H) / HOME AXIS",
              controlType = "Homing Button",
              menuLocation = "CONTROL -> DRO Panel",
              gcodeOrCommand = "\$H / \$HX / \$HY / \$HZ",
              inputFormat = "Single tap",
              description = "Initiates an automatic limit-switch search routine to establish machine zero (G53 absolute origin).",
              safetyNotes = "Always clear clamping obstacles before homing."
            ),
            ManualControlItem(
              name = "X+ / X- Jog Buttons",
              controlType = "Directional Jog Button",
              menuLocation = "CONTROL -> Manual Jog Matrix",
              gcodeOrCommand = "\$J=G91 G21 X[step] F[feed]",
              inputFormat = "Tap for step / Hold for continuous",
              description = "X+ moves the tool rightward (positive X). X- moves the tool leftward (negative X). Located horizontally with the central STOP target between them.",
              safetyNotes = "Check soft/hard travel limits before high-speed jogging."
            ),
            ManualControlItem(
              name = "Y+ / Y- Jog Buttons",
              controlType = "Directional Jog Button",
              menuLocation = "CONTROL -> Manual Jog Matrix",
              gcodeOrCommand = "\$J=G91 G21 Y[step] F[feed]",
              inputFormat = "Tap for step / Hold for continuous",
              description = "Y+ moves the table backward or gantry away (positive Y). Y- moves the table forward toward operator (negative Y)."
            ),
            ManualControlItem(
              name = "Z+ / Z- Jog Buttons",
              controlType = "Elevation Jog Button",
              menuLocation = "CONTROL -> Z-Axis Column",
              gcodeOrCommand = "\$J=G91 G21 Z[step] F[feed]",
              inputFormat = "Tap for step / Hold for continuous",
              description = "Z+ raises the tool tip away from the workpiece. Z- plunges the tool downward toward or into material."
            ),
            ManualControlItem(
              name = "A+ / A- Jog Buttons",
              controlType = "Rotary Jog Button",
              menuLocation = "CONTROL -> A-Axis Column",
              gcodeOrCommand = "\$J=G91 G21 A[step] F[feed]",
              inputFormat = "Tap for step / Hold for continuous",
              description = "Controls the 4th rotary axis. A+ rotates clockwise (CW). A- rotates counter-clockwise (CCW)."
            ),
            ManualControlItem(
              name = "STOP JOG (Central Target)",
              controlType = "Instant Stop Button",
              menuLocation = "CONTROL -> Center of XY Cross-Pad",
              gcodeOrCommand = "ASCII 0x85 (Jog Cancel)",
              inputFormat = "Single tap",
              description = "Immediately halts any in-progress manual jog motion on all axes without triggering a machine alarm.",
              safetyNotes = "Use to halt continuous motion smoothly before touching stock."
            ),
            ManualControlItem(
              name = "SAFE RETRACT Button",
              controlType = "Action Button",
              menuLocation = "CONTROL -> Z-Axis Column",
              gcodeOrCommand = "G53 G0 Z0 / G0 Z25",
              inputFormat = "Single tap",
              description = "Rapidly lifts the Z axis to the machine clearance safety plane to prevent collisions with clamps."
            ),
            ManualControlItem(
              name = "Jog Mode Selector (STEP vs CONTINUOUS)",
              controlType = "Segmented Toggle",
              menuLocation = "CONTROL -> Jog Bar",
              gcodeOrCommand = "Internal UI Mode",
              inputFormat = "Select 'STEP' or 'CONTINUOUS'",
              description = "In 'STEP' mode, each button press advances the axis by the selected step increment. In 'CONTINUOUS' mode, motion continues as long as the button is held down."
            ),
            ManualControlItem(
              name = "Step Increment Selector (0.01 to 50 mm)",
              controlType = "Selector Buttons",
              menuLocation = "CONTROL -> Jog Bar",
              gcodeOrCommand = "Incremental distance",
              inputFormat = "0.01 mm, 0.1 mm, 1.0 mm, 10.0 mm, 50.0 mm",
              description = "Determines travel distance per tap. Use 0.01 mm for dial indicator alignment and 50 mm for coarse positioning."
            ),
            ManualControlItem(
              name = "Jog Feed Rate Selector",
              controlType = "Selector Buttons",
              menuLocation = "CONTROL -> Jog Bar",
              gcodeOrCommand = "F[rate] mm/min",
              inputFormat = "F100, F500, F1000, F2000 mm/min",
              description = "Configures the speed at which manual jog travels will occur."
            ),
            ManualControlItem(
              name = "Spindle Direction & Control (CW / CCW / STOP)",
              controlType = "ButtonGroup",
              menuLocation = "CONTROL -> Spindle Panel",
              gcodeOrCommand = "M3 (CW) / M4 (CCW) / M5 (STOP)",
              inputFormat = "Tap CW, CCW, or STOP",
              description = "Engages spindle rotation in Clockwise direction (standard milling cutters), Counter-Clockwise (left-hand taps), or powers down the spindle.",
              safetyNotes = "Ensure tool collet is torqued and spindle path is free before starting."
            ),
            ManualControlItem(
              name = "Spindle RPM Slider & Field",
              controlType = "Slider / Numerical Input",
              menuLocation = "CONTROL -> Spindle Panel",
              gcodeOrCommand = "S[RPM]",
              inputFormat = "1,000 to 24,000 RPM",
              description = "Sets the target spindle speed commanded via PWM or 0-10V VFD signal."
            ),
            ManualControlItem(
              name = "Coolant Controls (FLOOD / MIST / OFF)",
              controlType = "ButtonGroup",
              menuLocation = "CONTROL -> Coolant Panel",
              gcodeOrCommand = "M8 (Flood) / M7 (Mist) / M9 (Off)",
              inputFormat = "Tap desired state",
              description = "Controls solenoid valves for liquid flood coolant, air-atomized mist lubricant, or shuts off all fluid lines."
            ),
            ManualControlItem(
              name = "Real-Time Overrides (Feed, Spindle, Rapid)",
              controlType = "Interactive Sliders",
              menuLocation = "CONTROL -> Overrides Bar",
              gcodeOrCommand = "Feed (0x90-0x94), Spindle (0x99-0x9D)",
              inputFormat = "10% to 200% slider",
              description = "Allows the operator to speed up or slow down cutting feed rate, spindle speed, or rapid traverse on-the-fly during active machining without editing the G-code file."
            )
          )
        ),
        ManualMenuCategory(
          id = "gcode_screen",
          title = "G-CODE Menu (File Streaming & 3D Toolpath)",
          subtitle = "Program loading, execution control, line visualizer, and 2D/3D toolpath preview.",
          badgeColor = DroGreen,
          items = listOf(
            ManualControlItem(
              name = "LOAD FILE Button",
              controlType = "File Picker Button",
              menuLocation = "G-CODE -> File Bar",
              gcodeOrCommand = "File open (.nc, .gcode, .ngc, .tap)",
              inputFormat = "Select file from Android storage",
              description = "Opens the Android system file picker to load a G-code program into memory for parsing, bounds checking, and streaming."
            ),
            ManualControlItem(
              name = "RUN / PLAY Button",
              controlType = "Action Button",
              menuLocation = "G-CODE -> Controls Bar",
              gcodeOrCommand = "~ (Cycle Start)",
              inputFormat = "Single tap",
              description = "Begins sequential streaming of the loaded G-code program line-by-line to the machine controller.",
              safetyNotes = "Check WCS origin, tool length offset, and clamps before starting execution."
            ),
            ManualControlItem(
              name = "PAUSE Button",
              controlType = "Action Button",
              menuLocation = "G-CODE -> Controls Bar",
              gcodeOrCommand = "! (Feed Hold)",
              inputFormat = "Single tap",
              description = "Temporarily pauses execution. Axes smoothly decelerate to zero while spindle keeps spinning."
            ),
            ManualControlItem(
              name = "STOP / ABORT Button",
              controlType = "Action Button",
              menuLocation = "G-CODE -> Controls Bar",
              gcodeOrCommand = "ASCII 0x18 (Soft Reset)",
              inputFormat = "Single tap with confirmation",
              description = "Cancels the active job and resets the line execution pointer to the beginning.",
              safetyNotes = "Tool may stop inside the material. Retract Z manually before moving."
            ),
            ManualControlItem(
              name = "2D/3D Toolpath Canvas",
              controlType = "Interactive Canvas",
              menuLocation = "G-CODE -> Center Panel",
              gcodeOrCommand = "Vector rendering",
              inputFormat = "Pinch to zoom, drag to pan/rotate",
              description = "Visual representation of all rapid moves (cyan) and cutting feeds (green) showing the workpiece envelope and live tool position cursor."
            ),
            ManualControlItem(
              name = "Auto-Level Height Mesh Toggle",
              controlType = "Toggle Switch",
              menuLocation = "G-CODE -> Mesh Panel",
              gcodeOrCommand = "Bilinear Z correction",
              inputFormat = "Enable / Disable switch",
              description = "Applies a surface-height compensation mesh (useful for PCB milling and engraving uneven sheets) by dynamically adjusting Z depth along the trajectory."
            )
          )
        ),
        ManualMenuCategory(
          id = "mdi_screen",
          title = "MDI Menu (Manual Data Input Terminal)",
          subtitle = "Direct G-code command terminal, command history, and instant macro shortcuts.",
          badgeColor = DroPurple,
          items = listOf(
            ManualControlItem(
              name = "G-Code Command Input Field",
              controlType = "Text Input Field",
              menuLocation = "MDI -> Input Bar",
              gcodeOrCommand = "Any valid G-code line",
              inputFormat = "e.g., 'G0 X50 Y25', 'G92 Z0', 'G53 G0 Z0'",
              description = "Type any standard G-code instruction or GRBL system command. Pressing Enter or tapping SEND immediately transmits the command to the board for execution."
            ),
            ManualControlItem(
              name = "SEND Button",
              controlType = "Action Button",
              menuLocation = "MDI -> Input Bar",
              gcodeOrCommand = "Transmits text field content",
              inputFormat = "Single tap",
              description = "Sends the typed command line to the CNC parser and logs the controller's response ('ok' or 'error:XX')."
            ),
            ManualControlItem(
              name = "Quick Macro Presets (Touch-off, Park, Spindle Warmup)",
              controlType = "Preset Action Buttons",
              menuLocation = "MDI -> Macros Strip",
              gcodeOrCommand = "G38.2 Z-20 F50 / G53 G0 Z0 X0 Y0",
              inputFormat = "Single tap on macro tile",
              description = "Executes pre-configured automation scripts for rapid tool touch-off, parking the gantry at rear, or warming up spindle bearings."
            ),
            ManualControlItem(
              name = "Command History Recall List",
              controlType = "Scrollable List",
              menuLocation = "MDI -> Center Area",
              gcodeOrCommand = "Logged commands",
              inputFormat = "Tap any past command to reload into input",
              description = "Maintains a history of the last 50 sent commands with execution timestamps and controller response status."
            )
          )
        ),
        ManualMenuCategory(
          id = "tools_screen",
          title = "TOOLS & WCS Menu (Work Coordinates & Tool Geometry)",
          subtitle = "Work Coordinate Systems (G54-G59), tool length/diameter tables, and offset persistence.",
          badgeColor = DroAmber,
          items = listOf(
            ManualControlItem(
              name = "WCS System Selector (G54 to G59)",
              controlType = "Segmented Selector",
              menuLocation = "TOOLS & WCS -> Top Bar",
              gcodeOrCommand = "G54, G55, G56, G57, G58, G59",
              inputFormat = "Tap desired WCS coordinate frame",
              description = "Switches the active work coordinate offset frame. Enables multi-fixture setups where different parts are clamped at distinct positions on the table."
            ),
            ManualControlItem(
              name = "WCS Offset Input Fields (X, Y, Z, A)",
              controlType = "Numerical Input Fields",
              menuLocation = "TOOLS & WCS -> Coordinate Panel",
              gcodeOrCommand = "G10 L2 P[1-6] X.. Y.. Z..",
              inputFormat = "Floating point values in mm (e.g. 124.500)",
              description = "Enter specific distance offsets from absolute machine zero (G53) to the workpiece datum."
            ),
            ManualControlItem(
              name = "STORE OFFSETS Button",
              controlType = "Action Button",
              menuLocation = "TOOLS & WCS -> Coordinate Panel",
              gcodeOrCommand = "G10 L2 P..",
              inputFormat = "Single tap",
              description = "Permanently writes the entered offset values into the controller's non-volatile EEPROM."
            ),
            ManualControlItem(
              name = "Tool Table Entry Fields (ID, Diameter, Length Offset)",
              controlType = "Form Fields & Table",
              menuLocation = "TOOLS & WCS -> Tool Library",
              gcodeOrCommand = "G43 H[ToolID] / T[ID] M6",
              inputFormat = "Tool ID (int), Diameter (mm), Length (mm)",
              description = "Stores cutter dimensions for automatic tool length compensation (G43) and cutter radius compensation (G41/G42)."
            )
          )
        ),
        ManualMenuCategory(
          id = "diagnostics_screen",
          title = "DIAGNOSTICS Menu (Hardware & HAL Pins Monitor)",
          subtitle = "Live inspection of limit switches, probe contacts, pulse timings, jitter, and thermal telemetry.",
          badgeColor = DroCyan,
          items = listOf(
            ManualControlItem(
              name = "Limit Switch Pin Status (X, Y, Z Min/Max)",
              controlType = "Logical Pin Indicator",
              menuLocation = "DIAGNOSTICS -> HAL Pins Grid",
              gcodeOrCommand = "? (Pn:X,Y,Z)",
              inputFormat = "Real-time Green/Red visual indicator",
              description = "Shows electrical state of mechanical and inductive homing/limit switches. Green indicates open/nominal; Red indicates tripped/closed."
            ),
            ManualControlItem(
              name = "Probe (Touch Plate) Input Monitor",
              controlType = "Logical Pin Indicator",
              menuLocation = "DIAGNOSTICS -> HAL Pins Grid",
              gcodeOrCommand = "? (Pn:P)",
              inputFormat = "Real-time contact indicator",
              description = "Shows live contact detection for electronic tool setters and conductive touch plates. Essential for verifying probe wiring before running a probing cycle."
            ),
            ManualControlItem(
              name = "Step Frequency & Jitter Analysis",
              controlType = "Telemetry Gauge",
              menuLocation = "DIAGNOSTICS -> Timing Card",
              gcodeOrCommand = "Stepgen timing loop",
              inputFormat = "Readout in kHz and microseconds (µs)",
              description = "Monitors real-time pulse generation consistency on the STM32 / LinuxCNC step generator. High jitter indicates system overload or timer interruptions."
            ),
            ManualControlItem(
              name = "Heartbeat & Packet Watchdog",
              controlType = "Communication Telemetry",
              menuLocation = "DIAGNOSTICS -> Link Health",
              gcodeOrCommand = "Ping/ACK ping counter",
              inputFormat = "Packets/sec and latency (ms)",
              description = "Monitors USB/Bluetooth link continuity and warns the operator if packet loss exceeds acceptable safety thresholds."
            )
          )
        ),
        ManualMenuCategory(
          id = "commissioning_screen",
          title = "COMMISSIONING Menu (Calibration & Machine Tuning)",
          subtitle = "Calibration wizards for Steps/mm ($100-$102), acceleration ($120-$122), and certification checklist.",
          badgeColor = DroPurple,
          items = listOf(
            ManualControlItem(
              name = "Commanded Distance Input Field",
              controlType = "Numerical Input Field",
              menuLocation = "COMMISSIONING -> Steps/mm Wizard",
              gcodeOrCommand = "Reference distance",
              inputFormat = "e.g. 100.00 mm",
              description = "The target distance you commanded the axis to travel during calibration."
            ),
            ManualControlItem(
              name = "Measured Distance Input Field",
              controlType = "Numerical Input Field",
              menuLocation = "COMMISSIONING -> Steps/mm Wizard",
              gcodeOrCommand = "Physical measurement",
              inputFormat = "e.g. 99.82 mm (measured with caliper)",
              description = "The actual distance traveled by the carriage, measured with a high-precision digital caliper or dial gauge."
            ),
            ManualControlItem(
              name = "Current Steps/mm Field",
              controlType = "Numerical Input Field",
              menuLocation = "COMMISSIONING -> Steps/mm Wizard",
              gcodeOrCommand = "\$100, \$101, or \$102",
              inputFormat = "e.g. 800.00 steps/mm",
              description = "The current step scale factor configured in the controller firmware."
            ),
            ManualControlItem(
              name = "CALCULATE & APPLY NEW STEPS/MM Button",
              controlType = "Action Button",
              menuLocation = "COMMISSIONING -> Steps/mm Wizard",
              gcodeOrCommand = "\$10x=[NewValue]",
              inputFormat = "Single tap",
              description = "Computes the exact corrected formula (New = Current * Commanded / Measured) and saves it directly to EEPROM."
            ),
            ManualControlItem(
              name = "10-Point Industrial Certification Checklist",
              controlType = "Interactive Checklist",
              menuLocation = "COMMISSIONING -> Safety Audit",
              gcodeOrCommand = "Commissioning sign-off",
              inputFormat = "Checkbox audit list",
              description = "Guides technicians through a complete safety validation covering E-stop interlocks, grounding, limit switch clearances, and spindle direction."
            )
          )
        ),
        ManualMenuCategory(
          id = "settings_screen",
          title = "SETTINGS Menu (Communications, Profiles & Preferences)",
          subtitle = "Transport selector, USB/Bluetooth link managers, language selector, and battery management.",
          badgeColor = DroGreen,
          items = listOf(
            ManualControlItem(
              name = "Transport Selection (Simulator / USB / Bluetooth)",
              controlType = "Dropdown / Segmented",
              menuLocation = "SETTINGS -> Top Card",
              gcodeOrCommand = "Driver link layer",
              inputFormat = "Select desired interface",
              description = "Switches the active communication pipeline between the in-memory simulator, hardware USB serial OTG, or wireless Bluetooth SPP."
            ),
            ManualControlItem(
              name = "USB Device Selector & Baud Rate Dropdown",
              controlType = "Dropdown Selectors",
              menuLocation = "SETTINGS -> USB Serial Card",
              gcodeOrCommand = "Serial configuration",
              inputFormat = "Device list & 9600 to 250000 bps",
              description = "Selects the specific detected USB serial chip (FTDI/CH340/STM32) and the transmission speed matching your firmware."
            ),
            ManualControlItem(
              name = "Bluetooth SCAN & CONNECT Buttons",
              controlType = "Action Buttons",
              menuLocation = "SETTINGS -> Bluetooth Card",
              gcodeOrCommand = "BT SPP discovery",
              inputFormat = "Tap SCAN then tap device to connect",
              description = "Discovers nearby Bluetooth SPP transceivers and establishes the bidirectional wireless serial stream."
            ),
            ManualControlItem(
              name = "Eco Mode (Resource & Battery Saver) Switch",
              controlType = "Toggle Switch",
              menuLocation = "SETTINGS -> Resource Management Card",
              gcodeOrCommand = "Adaptive polling & frame throttling",
              inputFormat = "Turn ON / OFF",
              description = "Throttles non-essential background animations and reduces telemetry polling rates to save battery life on portable tablets during long milling operations."
            ),
            ManualControlItem(
              name = "Security Role Access (Operator vs Maintenance)",
              controlType = "Action Button",
              menuLocation = "SETTINGS -> Appearance & Role",
              gcodeOrCommand = "Authorization profile",
              inputFormat = "Tap to switch role",
              description = "Switches between Operator mode (safeguards machine configurations from unintended alterations) and Maintenance Engineer mode (unlocks raw EEPROM settings and calibration tools)."
            ),
            ManualControlItem(
              name = "Language Selector (EN, ES, DE, FR, JA, KO)",
              controlType = "Language Selector Strip",
              menuLocation = "SETTINGS & MANUAL -> Language Bar",
              gcodeOrCommand = "UI Localization",
              inputFormat = "Tap flag / language name",
              description = "Dynamically switches all UI text, manual descriptions, connection guides, and control definitions across English, Spanish, German, French, Japanese, and Korean."
            )
          )
        )
      )
    )
  }

  // ==========================================
  // 2. SPANISH CONTENT (ESPAÑOL)
  // ==========================================
  private fun getSpanishContent(): ManualFullContent {
    return ManualFullContent(
      language = AppLanguage.ES,
      screenTitle = "MANUAL DE OPERACIÓN Y REFERENCIA TÉCNICA",
      screenSubtitle = "Documentación completa de la aplicación, guía de conexión y definición exhaustiva de cada botón, campo y menú.",
      tabOverview = "1. ¿Qué hace la aplicación?",
      tabConnection = "2. Conexión del Dispositivo",
      tabControls = "3. Botones, Campos y Menús",
      searchPlaceholder = "Buscar botón, campo, comando G-code o menú (ej. X+, E-Stop, OTG, M3)...",
      categoryAll = "TODOS LOS MENÚS",
      emptySearchResults = "No se encontraron controles o menús que coincidan con la búsqueda.",
      overview = ManualOverviewSection(
        title = "¿Qué hace la aplicación 1 Q CNC Droid?",
        subtitle = "HMI industrial de control numérico diseñado para arquitectura 1 Q, LinuxCNC y controladores paso a paso.",
        paragraphs = listOf(
          "1 Q CNC Droid es una Interfaz Hombre-Máquina (HMI) de grado industrial diseñada para transformar tablets y teléfonos inteligentes Android en una consola de control CNC y colgante (pendant) táctil completo. Está optimizada para comunicarse tanto con arquitecturas avanzadas 1 Q (procesador de aplicaciones Qualcomm QRB2210 con LinuxCNC acoplado a un coprocesador de tiempos STM32U585), como con sistemas clásicos basados en GRBL, FluidNC y LinuxCNC.",
          "La aplicación asume el control total de la máquina herramienta: gestiona la lectura digital de coordenadas (DRO) en tiempo real para 4 ejes (X, Y, Z, A), el movimiento manual táctil (Jog) por pasos milimétricos o continuo con parada segura, el envío línea a línea de programas G-Code con visualizador gráfico 2D/3D y corrección de plano (Auto-Level mesh), sistemas de coordenadas de pieza (G54 a G59), tabla de herramientas y diagnóstico de pines HAL hasta nivel de jitter y frecuencia de pasos.",
          "Con una canalización serie de latencia ultra baja, enclavamientos de seguridad por software y hardware, y modos de ahorro de energía adaptativos, permite operar fresadoras, routers, tornos y mesas de corte láser/plasma directamente a pie de máquina sin necesidad de un ordenador de torre voluminoso."
        ),
        features = listOf(
          ManualFeatureHighlight(
            title = "DRO Digital Multieje Sub-Micrónico",
            description = "Lectura de posición de alta resolución en milímetros o pulgadas para los ejes X, Y, Z y A en coordenadas de pieza (WCS) o de máquina (MCS).",
            iconName = "dro"
          ),
          ManualFeatureHighlight(
            title = "Panel de Jogging Táctil Seguro",
            description = "Desplazamiento manual por pasos discretos (0.01 mm a 50 mm) o modo continuo por pulsación prolongada con botón central de parada inmediata.",
            iconName = "jog"
          ),
          ManualFeatureHighlight(
            title = "Streaming G-Code y Toolpath 3D",
            description = "Ejecución continua de código G con visualizador gráfico de trayectorias en 2D/3D y compensación de altura superficial por malla de nivelación.",
            iconName = "gcode"
          ),
          ManualFeatureHighlight(
            title = "Terminal MDI y Macros Rápidas",
            description = "Entrada manual de datos con historial de comandos, resaltado de respuestas del controlador y botones de macros preconfiguradas.",
            iconName = "mdi"
          ),
          ManualFeatureHighlight(
            title = "Gestión de WCS (G54-G59) y Herramientas",
            description = "Ajuste rápido de orígenes de pieza con G10 L2/L20 y tabla geométrica de compensación de longitud y diámetro de fresa.",
            iconName = "tools"
          ),
          ManualFeatureHighlight(
            title = "Diagnóstico Profundo de Hardware",
            description = "Monitoreo en tiempo real de finales de carrera, sonda de palpado (probe), parada de emergencia, frecuencia de pulsos de paso y temperatura de CPU.",
            iconName = "diagnostics"
          )
        )
      ),
      connectionGuide = ManualConnectionGuide(
        title = "Guía de Conexión del Dispositivo (Android a PC o Placa)",
        subtitle = "Instrucciones paso a paso para conectar su dispositivo Android a la controladora CNC mediante USB OTG, Bluetooth SPP o el Simulador Virtual.",
        methods = listOf(
          ConnectionMethodGuide(
            id = "usb_serial",
            name = "Conexión Serial USB OTG (Recomendada para Producción)",
            badge = "MÁXIMA FIABILIDAD Y MÍNIMA LATENCIA",
            hardwareRequirements = "Dispositivo Android con soporte USB Host (OTG), cable/adaptador OTG de alta calidad apantallado, y placa CNC con chip USB serie (CH340, CP2102, FTDI o CDC nativo STM32/Arduino).",
            stepByStep = listOf(
              "Paso 1: Conecte el adaptador USB OTG al puerto de carga de su dispositivo Android (Type-C o Micro-USB).",
              "Paso 2: Conecte un cable USB con blindaje desde el adaptador OTG hasta la placa controladora CNC (Arduino UNO Q, shield GRBL o interfaz LinuxCNC).",
              "Paso 3: Android mostrará un diálogo de permiso del sistema: '¿Permitir que UNO Q CNC acceda al dispositivo USB?'. Marque 'Recordar siempre' y pulse 'Aceptar'.",
              "Paso 4: En la aplicación, entre en la pestaña 'Ajustes' (Settings). En el selector de transporte, elija 'USB Serial'.",
              "Paso 5: Seleccione la velocidad en baudios adecuada (por defecto 115200 bps en GRBL/UNO Q, o 250000 bps en placas rápidas).",
              "Paso 6: Pulse el botón azul 'CONECTAR'. La barra superior cambiará a verde 'USB CONECTADO' y el DRO mostrará las posiciones vivas de la máquina."
            ),
            recommendedSettings = "Baudios: 115200 / 250000 | Datos: 8 bits | Parada: 1 bit | Paridad: Ninguna (8N1) | DTR/RTS: Activado"
          ),
          ConnectionMethodGuide(
            id = "bluetooth_spp",
            name = "Conexión Inalámbrica Bluetooth SPP (Pendant sin Cables)",
            badge = "MOVILIDAD TOTAL EN TALLER",
            hardwareRequirements = "Módulo serie Bluetooth (HC-05, HC-06 o ESP32 configurado en modo Serial Bluetooth SPP) conectado a los pines UART TX/RX de la placa CNC.",
            stepByStep = listOf(
              "Paso 1: Encienda la máquina CNC y verifique que el LED del módulo Bluetooth parpadee rápidamente (modo espera de emparejamiento).",
              "Paso 2: En los Ajustes de Bluetooth de Android, busque dispositivos y empareje el módulo (el código PIN suele ser 1234 o 0000).",
              "Paso 3: Abra UNO Q CNC, vaya a la pestaña 'Ajustes' y configure el transporte en 'Bluetooth SPP'.",
              "Paso 4: En la sección 'GESTOR BLUETOOTH', pulse el botón 'ESCANEAR DISPOSITIVOS'.",
              "Paso 5: Seleccione el módulo emparejado en la lista y pulse 'CONECTAR'.",
              "Paso 6: El LED del módulo Bluetooth se mantendrá fijo y la aplicación indicará 'BLUETOOTH CONECTADO'."
            ),
            recommendedSettings = "Alcance: hasta 10 metros en línea visual directa. Compruebe que la velocidad en baudios coincida con el firmware del módulo."
          ),
          ConnectionMethodGuide(
            id = "virtual_sim",
            name = "Simulador Virtual Integrado (Pruebas Offline)",
            badge = "MODO APRENDIZAJE Y VERIFICACIÓN",
            hardwareRequirements = "No requiere ningún hardware externo. Funciona de manera autónoma en la memoria del dispositivo Android.",
            stepByStep = listOf(
              "Paso 1: En la pestaña 'Ajustes', seleccione 'Simulador Virtual (Offline)'.",
              "Paso 2: El sistema activa un motor cinemático que emula las respuestas de movimiento, aceleración, límites de carrera y giro del husillo.",
              "Paso 3: Utilice este modo para comprobar programas G-code nuevos, practicar desplazamientos manuales y depurar macros sin riesgo de colisión."
            ),
            recommendedSettings = "Listo para usar de inmediato sin configuración de puertos."
          )
        ),
        troubleshootingTitle = "Resolución de Problemas y Calidad de Señal",
        troubleshootingItems = listOf(
          TroubleshootingStep(
            problem = "Android no reconoce el cable USB o no aparece la ventana de permiso",
            cause = "Cable USB solo de carga (sin hilos de datos), adaptador OTG defectuoso o función OTG desactivada en el teléfono.",
            solution = "Utilice un cable de 4 hilos certificado para transferencia de datos. En algunos teléfonos (OnePlus, Oppo, Vivo, Realme), active manualmente 'Conexión OTG' en Ajustes -> Ajustes adicionales."
          ),
          TroubleshootingStep(
            problem = "Desconexiones o cuelgues al encender el husillo o fresadora",
            cause = "Ruido e interferencia electromagnética (EMI) generada por el variador de frecuencia (VFD) o el motor que se cuela por el cable USB.",
            solution = "Utilice cables USB de doble blindaje con ferritas en ambos extremos. Separe físicamente el cable USB de las mangueras de potencia del motor y conecte la toma de tierra del chasis."
          ),
          TroubleshootingStep(
            problem = "La conexión se interrumpe cuando la pantalla se apaga o la app pasa a segundo plano",
            cause = "El gestor de batería de Android congela el proceso de comunicación serial para ahorrar energía.",
            solution = "Vaya a Información de la App en Android -> Batería -> Seleccione 'Sin restricciones' para evitar la suspensión del puerto USB."
          )
        )
      ),
      menuCategories = listOf(
        ManualMenuCategory(
          id = "top_bar",
          title = "Barra Superior y Seguridad Global",
          subtitle = "Parada de emergencia, desbloqueo de alarmas y estado en vivo accesibles en todo momento.",
          badgeColor = IndDarkEmergency,
          items = listOf(
            ManualControlItem(
              name = "E-STOP (Parada de Emergencia)",
              controlType = "Pulsador de Seguridad",
              menuLocation = "Barra Superior (Siempre visible)",
              gcodeOrCommand = "ASCII 0x18 / !",
              inputFormat = "Pulsación física inmediata",
              description = "Detiene instantáneamente todos los motores paso a paso o servomotores, apaga el husillo y activa el estado de ALARMA en la controladora. Debe utilizarse ante cualquier riesgo de colisión, rotura de fresa o peligro físico.",
              safetyNotes = "PELIGRO: El E-Stop deja la máquina bloqueada hasta que sea rearmada conscientemente."
            ),
            ManualControlItem(
              name = "RESET ALARMA (\$X)",
              controlType = "Botón de Desbloqueo",
              menuLocation = "Barra Superior",
              gcodeOrCommand = "\$X",
              inputFormat = "Pulsar para desbloquear",
              description = "Desactiva el estado de alarma de GRBL / LinuxCNC tras un disparo de fin de carrera o parada de emergencia, permitiendo mover los ejes fuera de la zona de peligro.",
              safetyNotes = "Verifique haber resuelto la causa de la alarma antes de presionar este botón."
            ),
            ManualControlItem(
              name = "CYCLE START (~)",
              controlType = "Botón de Marcha",
              menuLocation = "Barra Superior",
              gcodeOrCommand = "~",
              inputFormat = "Pulsación simple",
              description = "Reanuda el movimiento tras una pausa (Feed Hold) o inicia la ejecución del programa G-Code cargado.",
              safetyNotes = "Asegúrese de que el área de mecanizado esté despejada antes de arrancar."
            ),
            ManualControlItem(
              name = "FEED HOLD (!)",
              controlType = "Botón de Pausa",
              menuLocation = "Barra Superior",
              gcodeOrCommand = "!",
              inputFormat = "Pulsación simple",
              description = "Desacelera los ejes de forma suave y controlada hasta detenerse por completo, manteniendo el husillo encendido y las coordenadas intactas.",
              safetyNotes = "El husillo continuará girando; no toque la herramienta mientras esté en marcha."
            ),
            ManualControlItem(
              name = "Indicador de Estado de Máquina",
              controlType = "Insignia de Estado",
              menuLocation = "Barra Superior",
              gcodeOrCommand = "?",
              inputFormat = "Visualización de solo lectura",
              description = "Muestra el modo en tiempo real: IDLE (Lista), RUN (Mecanizando), HOLD (Pausada), JOG (Moviendo a mano), ALARM (Bloqueo de seguridad) o HOME (Búsqueda de ceros)."
            )
          )
        ),
        ManualMenuCategory(
          id = "control_screen",
          title = "Menú CONTROL (Puesto de Mando Principal)",
          subtitle = "Lectura Digital (DRO), Panel de Jogging Direccional, Mandos de Husillo/Refrigerante y Anulaciones (Overrides).",
          badgeColor = DroCyan,
          items = listOf(
            ManualControlItem(
              name = "Lectura Digital DRO (Ejes X, Y, Z, A)",
              controlType = "Display DRO",
              menuLocation = "CONTROL -> Panel Superior",
              gcodeOrCommand = "? (Reporte de estado)",
              inputFormat = "Visualización numérica de 3 decimales",
              description = "Indica la posición exacta de la punta de la herramienta en milímetros con precisión micrométrica. Permite alternar entre Coordenadas de Trabajo (WCS) y Coordenadas de Máquina (MCS)."
            ),
            ManualControlItem(
              name = "Botones CERO X / CERO Y / CERO Z / CERO A",
              controlType = "Botón de Puesta a Cero",
              menuLocation = "CONTROL -> Bloque DRO",
              gcodeOrCommand = "G10 L20 P0 [Eje]0",
              inputFormat = "Pulsación sobre el eje deseado",
              description = "Establece la posición actual de ese eje como 0.000 dentro del sistema de coordenadas de pieza activo (ej. G54) sin alterar las coordenadas absolutas de la máquina.",
              safetyNotes = "Asegúrese de haber palpado con exactitud el borde o cara de la pieza antes de poner a cero."
            ),
            ManualControlItem(
              name = "Botón CERO TODOS",
              controlType = "Botón de Puesta a Cero Global",
              menuLocation = "CONTROL -> Bloque DRO",
              gcodeOrCommand = "G10 L20 P0 X0 Y0 Z0 A0",
              inputFormat = "Pulsación simple",
              description = "Pone a cero simultáneamente todos los ejes en la posición física actual.",
              safetyNotes = "Compruebe que la altura Z esté bien referenciada respecto a la superficie de la pieza."
            ),
            ManualControlItem(
              name = "HOME TODOS (\$H) / HOME POR EJE",
              controlType = "Botón de Búsqueda de Referencia",
              menuLocation = "CONTROL -> Bloque DRO",
              gcodeOrCommand = "\$H / \$HX / \$HY / \$HZ",
              inputFormat = "Pulsación simple",
              description = "Inicia el ciclo automático de búsqueda de los microinterruptores o sensores inductivos de fin de carrera para fijar el cero máquina (G53).",
              safetyNotes = "Retire bridas, mordazas o herramientas que puedan chocar antes de iniciar el Homing."
            ),
            ManualControlItem(
              name = "Botones de Jog X+ / X-",
              controlType = "Botón Direccional de Jog",
              menuLocation = "CONTROL -> Matriz de Movimiento Manual",
              gcodeOrCommand = "\$J=G91 G21 X[paso] F[vel]",
              inputFormat = "Pulsar para paso / Mantener para continuo",
              description = "X+ desplaza el carro hacia la derecha (coordenadas positivas). X- desplaza el carro hacia la izquierda (coordenadas negativas). Ubicados a ambos lados del botón central STOP.",
              safetyNotes = "Verifique no sobrepasar el límite físico del puente si no tiene finales de carrera activos."
            ),
            ManualControlItem(
              name = "Botones de Jog Y+ / Y-",
              controlType = "Botón Direccional de Jog",
              menuLocation = "CONTROL -> Matriz de Movimiento Manual",
              gcodeOrCommand = "\$J=G91 G21 Y[paso] F[vel]",
              inputFormat = "Pulsar para paso / Mantener para continuo",
              description = "Y+ desplaza la mesa hacia el fondo o aleja el puente (coordenadas Y positivas). Y- acerca la mesa hacia el operador (coordenadas Y negativas)."
            ),
            ManualControlItem(
              name = "Botones de Jog Z+ / Z-",
              controlType = "Botón de Elevación de Herramienta",
              menuLocation = "CONTROL -> Columna Eje Z",
              gcodeOrCommand = "\$J=G91 G21 Z[paso] F[vel]",
              inputFormat = "Pulsar para paso / Mantener para continuo",
              description = "Z+ eleva la herramienta alejándola del material. Z- hace descender la herramienta hacia la pieza de trabajo."
            ),
            ManualControlItem(
              name = "Botones de Jog A+ / A-",
              controlType = "Botón de Eje Rotativo",
              menuLocation = "CONTROL -> Columna Eje A",
              gcodeOrCommand = "\$J=G91 G21 A[paso] F[vel]",
              inputFormat = "Pulsar para paso / Mantener para continuo",
              description = "Controla el 4º eje rotatorio. A+ rota en sentido horario (CW). A- rota en sentido antihorario (CCW)."
            ),
            ManualControlItem(
              name = "STOP JOG (Diana Central)",
              controlType = "Botón de Parada Inmediata de Jog",
              menuLocation = "CONTROL -> Centro de la Cruz X-Y",
              gcodeOrCommand = "ASCII 0x85 (Cancelación de Jog)",
              inputFormat = "Pulsación simple",
              description = "Detiene de forma instantánea cualquier movimiento manual en curso en cualquiera de los ejes sin causar una alarma de máquina.",
              safetyNotes = "Permite frenar el carro con rapidez antes de rozar la pieza en aproximaciones manuales."
            ),
            ManualControlItem(
              name = "Botón RETRACT SEGURA",
              controlType = "Acción Rápida",
              menuLocation = "CONTROL -> Columna Eje Z",
              gcodeOrCommand = "G53 G0 Z0 / G0 Z25",
              inputFormat = "Pulsación simple",
              description = "Sube rápidamente el eje Z a una cota segura despejada para evitar colisionar con bridas o salientes."
            ),
            ManualControlItem(
              name = "Selector de Modo de Jog (PASO vs CONTINUO)",
              controlType = "Selector Segmentado",
              menuLocation = "CONTROL -> Barra de Jog",
              gcodeOrCommand = "Modo interno UI",
              inputFormat = "Elegir 'PASO' o 'CONTINUO'",
              description = "En modo 'PASO', cada toque desplaza exactamente el incremento seleccionado. En modo 'CONTINUO', el carro se mueve mientras mantenga el botón pulsado."
            ),
            ManualControlItem(
              name = "Selector de Incremento de Paso (0.01 a 50 mm)",
              controlType = "Botones de Selección",
              menuLocation = "CONTROL -> Barra de Jog",
              gcodeOrCommand = "Distancia de desplazamiento",
              inputFormat = "0.01 mm, 0.1 mm, 1.0 mm, 10.0 mm, 50.0 mm",
              description = "Fija el avance exacto por pulsación. Use 0.01 mm para ajustes con reloj comparador y 50 mm para aproximaciones largas."
            ),
            ManualControlItem(
              name = "Selector de Velocidad de Jog (Feed Rate)",
              controlType = "Botones de Selección",
              menuLocation = "CONTROL -> Barra de Jog",
              gcodeOrCommand = "F[avance] mm/min",
              inputFormat = "F100, F500, F1000, F2000 mm/min",
              description = "Define la velocidad en milímetros por minuto a la que se ejecutarán los movimientos de jog."
            ),
            ManualControlItem(
              name = "Mandos de Husillo (CW / CCW / STOP)",
              controlType = "Grupo de Botones",
              menuLocation = "CONTROL -> Panel de Husillo",
              gcodeOrCommand = "M3 (CW) / M4 (CCW) / M5 (STOP)",
              inputFormat = "Pulsar CW, CCW o STOP",
              description = "Enciende el husillo en sentido horario (fresado estándar M3), antihorario (roscado a izquierdas M4) o apaga la rotación (M5).",
              safetyNotes = "Asegúrese de que la pinza esté bien apretada antes de arrancar el husillo."
            ),
            ManualControlItem(
              name = "Control Deslizante y Campo de RPM",
              controlType = "Control Deslizante / Numérico",
              menuLocation = "CONTROL -> Panel de Husillo",
              gcodeOrCommand = "S[RPM]",
              inputFormat = "1,000 a 24,000 RPM",
              description = "Regula las revoluciones por minuto del cabezal comandadas mediante señal PWM o 0-10V al variador VFD."
            ),
            ManualControlItem(
              name = "Mandos de Refrigerante (FLOOD / MIST / OFF)",
              controlType = "Grupo de Botones",
              menuLocation = "CONTROL -> Panel de Refrigerante",
              gcodeOrCommand = "M8 (Chorro) / M7 (Niebla) / M9 (Apagar)",
              inputFormat = "Pulsar el estado deseado",
              description = "Abre las electroválvulas para chorro de taladrina (M8), lubricación por niebla de aire (M7) o apaga todas las salidas de fluido (M9)."
            ),
            ManualControlItem(
              name = "Anulaciones en Tiempo Real (Overrides)",
              controlType = "Controles Deslizantes",
              menuLocation = "CONTROL -> Barra de Overrides",
              gcodeOrCommand = "Feed (0x90-0x94), Spindle (0x99-0x9D)",
              inputFormat = "Rango de 10% a 200%",
              description = "Permite acelerar o frenar en caliente el avance de corte, la velocidad del husillo o los movimientos rápidos mientras la máquina está mecanizando, sin tener que reprogramar el archivo G-Code."
            )
          )
        ),
        ManualMenuCategory(
          id = "gcode_screen",
          title = "Menú G-CODE (Visor y Ejecución de Programas)",
          subtitle = "Carga de archivos, control de ejecución línea a línea y visualizador de trayectorias 2D/3D.",
          badgeColor = DroGreen,
          items = listOf(
            ManualControlItem(
              name = "Botón CARGAR ARCHIVO",
              controlType = "Selector de Archivos",
              menuLocation = "G-CODE -> Barra de Archivo",
              gcodeOrCommand = "Apertura de archivo (.nc, .gcode, .ngc, .tap)",
              inputFormat = "Elegir archivo del almacenamiento de Android",
              description = "Abre el explorador de archivos del sistema para cargar un programa G-Code en memoria, verificar sus dimensiones y prepararlo para mecanizado."
            ),
            ManualControlItem(
              name = "Botón PLAY / EJECUTAR",
              controlType = "Botón de Acción",
              menuLocation = "G-CODE -> Barra de Control",
              gcodeOrCommand = "~ (Cycle Start)",
              inputFormat = "Pulsación simple",
              description = "Inicia el streaming secuencial del programa G-Code línea a línea hacia la controladora CNC.",
              safetyNotes = "Compruebe origen de pieza, longitud de herramienta y bridas de sujeción antes de ejecutar."
            ),
            ManualControlItem(
              name = "Botón PAUSA",
              controlType = "Botón de Acción",
              menuLocation = "G-CODE -> Barra de Control",
              gcodeOrCommand = "! (Feed Hold)",
              inputFormat = "Pulsación simple",
              description = "Pausa temporalmente el mecanizado desacelerando suavemente los ejes. El husillo permanece en marcha para no dañar la fresa."
            ),
            ManualControlItem(
              name = "Botón DETENER / ABORTAR",
              controlType = "Botón de Acción",
              menuLocation = "G-CODE -> Barra de Control",
              gcodeOrCommand = "ASCII 0x18 (Soft Reset)",
              inputFormat = "Pulsación con confirmación",
              description = "Cancela por completo el trabajo activo y devuelve el puntero al inicio del programa.",
              safetyNotes = "La herramienta puede quedar dentro del material. Súbala en Z antes de mover en X o Y."
            ),
            ManualControlItem(
              name = "Lienzo Toolpath 2D/3D",
              controlType = "Lienzo Interactivo",
              menuLocation = "G-CODE -> Panel Central",
              gcodeOrCommand = "Renderizado vectorial",
              inputFormat = "Pellizcar para zoom, arrastrar para rotar/mover",
              description = "Representación gráfica de todas las trayectorias rápidas (cian) y de corte (verde), mostrando la posición actual de la herramienta en tiempo real."
            ),
            ManualControlItem(
              name = "Compensación de Malla de Altura (Auto-Level Mesh)",
              controlType = "Interruptor de Activación",
              menuLocation = "G-CODE -> Panel de Malla",
              gcodeOrCommand = "Corrección bilineal en Z",
              inputFormat = "Interruptor Activar / Desactivar",
              description = "Aplica una corrección automática de altura basada en los puntos de palpado registrados con la sonda táctil, compensando irregularidades en placas de circuito impreso o superficies deformadas."
            )
          )
        ),
        ManualMenuCategory(
          id = "mdi_screen",
          title = "Menú MDI (Entrada Manual de Datos y Terminal)",
          subtitle = "Consola de órdenes directas G-Code, historial de comandos y accesos rápidos a macros.",
          badgeColor = DroPurple,
          items = listOf(
            ManualControlItem(
              name = "Campo de Texto 'Entrada de Comando G-Code'",
              controlType = "Campo de Texto",
              menuLocation = "MDI -> Barra de Entrada",
              gcodeOrCommand = "Cualquier línea válida de G-Code",
              inputFormat = "ej. 'G0 X50 Y25', 'G92 Z0', 'G53 G0 Z0'",
              description = "Permite teclear cualquier comando del estándar RS-274 o instrucción del sistema GRBL/LinuxCNC. Al presionar Intro o el botón ENVIAR, se transmite de inmediato."
            ),
            ManualControlItem(
              name = "Botón ENVIAR (Send)",
              controlType = "Botón de Envío",
              menuLocation = "MDI -> Barra de Entrada",
              gcodeOrCommand = "Transmite la línea de texto",
              inputFormat = "Pulsación simple",
              description = "Envía la orden tecleada al intérprete de la máquina y muestra la respuesta devuelta ('ok' o 'error:XX')."
            ),
            ManualControlItem(
              name = "Botones de Macros Rápidas (Palpado, Aparcamiento, Calentamiento)",
              controlType = "Botones de Automatización",
              menuLocation = "MDI -> Franja de Macros",
              gcodeOrCommand = "G38.2 Z-20 F50 / G53 G0 Z0 X0 Y0",
              inputFormat = "Pulsar el botón de la macro deseada",
              description = "Ejecuta secuencias de instrucciones preprogramadas para medir la altura de la herramienta con sensor táctil, aparcar el puente en la parte trasera o calentar rodamientos del husillo."
            ),
            ManualControlItem(
              name = "Lista de Historial de Comandos",
              controlType = "Lista Desplazable",
              menuLocation = "MDI -> Área Central",
              gcodeOrCommand = "Registro de órdenes",
              inputFormat = "Tocar una orden pasada para recargarla en el campo",
              description = "Mantiene el registro de las últimas 50 órdenes enviadas con fecha, hora y confirmación de recepción."
            )
          )
        ),
        ManualMenuCategory(
          id = "tools_screen",
          title = "Menú TOOLS & WCS (Coordenadas de Trabajo y Herramientas)",
          subtitle = "Sistemas de coordenadas G54 a G59, compensación de longitud/diámetro de fresas y guardado permanente.",
          badgeColor = DroAmber,
          items = listOf(
            ManualControlItem(
              name = "Selector de Sistema WCS (G54 a G59)",
              controlType = "Selector Segmentado",
              menuLocation = "TOOLS & WCS -> Barra Superior",
              gcodeOrCommand = "G54, G55, G56, G57, G58, G59",
              inputFormat = "Pulsar el sistema deseado",
              description = "Cambia el sistema de coordenadas de trabajo activo, permitiendo mecanizar varias piezas distintas amarradas en diferentes posiciones de la mesa."
            ),
            ManualControlItem(
              name = "Campos de Desfase de Coordenadas (X, Y, Z, A)",
              controlType = "Campos Numéricos de Entrada",
              menuLocation = "TOOLS & WCS -> Panel de Coordenadas",
              gcodeOrCommand = "G10 L2 P[1-6] X.. Y.. Z..",
              inputFormat = "Valores decimales en mm (ej. 124.500)",
              description = "Permite introducir directamente la distancia exacta desde el cero de máquina (G53) hasta el punto de origen de la pieza."
            ),
            ManualControlItem(
              name = "Botón GUARDAR OFFSETS",
              controlType = "Botón de Almacenamiento",
              menuLocation = "TOOLS & WCS -> Panel de Coordenadas",
              gcodeOrCommand = "G10 L2 P..",
              inputFormat = "Pulsación simple",
              description = "Graba de forma permanente los desfases de coordenadas en la memoria no volátil (EEPROM) de la controladora."
            ),
            ManualControlItem(
              name = "Campos de la Tabla de Herramientas (ID, Diámetro, Longitud H)",
              controlType = "Campos de Formulario y Tabla",
              menuLocation = "TOOLS & WCS -> Biblioteca de Herramientas",
              gcodeOrCommand = "G43 H[ID] / T[ID] M6",
              inputFormat = "Número de herramienta, Diámetro (mm), Longitud (mm)",
              description = "Almacena las dimensiones de cada fresa para aplicar compensación automática de longitud (G43) y de radio de corte (G41/G42)."
            )
          )
        ),
        ManualMenuCategory(
          id = "diagnostics_screen",
          title = "Menú DIAGNÓSTICOS (Hardware y Pines HAL)",
          subtitle = "Inspección en vivo de interruptores de límite, sonda, tiempos de ciclo, fluctuación (jitter) y temperatura.",
          badgeColor = DroCyan,
          items = listOf(
            ManualControlItem(
              name = "Indicadores de Finales de Carrera (X, Y, Z Mín/Máx)",
              controlType = "Indicador Lógico de Pin",
              menuLocation = "DIAGNÓSTICOS -> Rejilla de Pines HAL",
              gcodeOrCommand = "? (Pn:X,Y,Z)",
              inputFormat = "Indicador visual Verde/Rojo en tiempo real",
              description = "Refleja el estado eléctrico de los sensores mecánicos o inductivos. Verde indica circuito abierto/nominal; Rojo indica sensor accionado o fin de carrera alcanzado."
            ),
            ManualControlItem(
              name = "Monitor de Entrada de Sonda (Probe)",
              controlType = "Indicador Lógico de Pin",
              menuLocation = "DIAGNÓSTICOS -> Rejilla de Pines HAL",
              gcodeOrCommand = "? (Pn:P)",
              inputFormat = "Indicador de contacto en tiempo real",
              description = "Muestra si la placa táctil de palpado o el sensor de longitud de herramienta está haciendo contacto eléctrico. Esencial para verificar la sonda antes de iniciar un palpado."
            ),
            ManualControlItem(
              name = "Análisis de Frecuencia de Pasos y Jitter",
              controlType = "Medidor de Telemetría",
              menuLocation = "DIAGNÓSTICOS -> Tarjeta de Tiempos",
              gcodeOrCommand = "Bucle de temporización Stepgen",
              inputFormat = "Lectura en kHz y microsegundos (µs)",
              description = "Supervisa la regularidad del generador de pulsos de paso en el chip STM32 / LinuxCNC. Un jitter elevado advierte de saturación del procesador o interrupciones lentas."
            ),
            ManualControlItem(
              name = "Watchdog de Comunicación y Paquetes",
              controlType = "Telemetría de Enlace",
              menuLocation = "DIAGNÓSTICOS -> Salud del Enlace",
              gcodeOrCommand = "Contador de pings y confirmaciones",
              inputFormat = "Paquetes/segundo y latencia (ms)",
              description = "Verifica la continuidad de la conexión USB o Bluetooth y avisa al operador si se detecta pérdida de datos o retardos peligrosos."
            )
          )
        ),
        ManualMenuCategory(
          id = "commissioning_screen",
          title = "Menú PUESTA EN MARCHA (Calibración y Ajustes)",
          subtitle = "Asistentes de calibración de pasos por mm ($100-$102), aceleración y lista de certificación industrial.",
          badgeColor = DroPurple,
          items = listOf(
            ManualControlItem(
              name = "Campo 'Distancia Comandada' (mm)",
              controlType = "Campo Numérico de Entrada",
              menuLocation = "PUESTA EN MARCHA -> Asistente Pasos/mm",
              gcodeOrCommand = "Distancia teórica ordenada",
              inputFormat = "ej. 100.00 mm",
              description = "La distancia teórica que se le ordenó moverse al carro durante la prueba de calibración."
            ),
            ManualControlItem(
              name = "Campo 'Distancia Medida Real' (mm)",
              controlType = "Campo Numérico de Entrada",
              menuLocation = "PUESTA EN MARCHA -> Asistente Pasos/mm",
              gcodeOrCommand = "Medición física",
              inputFormat = "ej. 99.82 mm (medida con calibre o reloj)",
              description = "La distancia real que se desplazó el carro, medida físicamente con un pie de rey digital o comparador centesimal."
            ),
            ManualControlItem(
              name = "Campo 'Pasos/mm Actuales'",
              controlType = "Campo Numérico de Entrada",
              menuLocation = "PUESTA EN MARCHA -> Asistente Pasos/mm",
              gcodeOrCommand = "\$100, \$101 o \$102",
              inputFormat = "ej. 800.00 pasos/mm",
              description = "El valor actual configurado en el firmware del controlador."
            ),
            ManualControlItem(
              name = "Botón CALCULAR Y APLICAR NUEVOS PASOS/MM",
              controlType = "Botón de Acción",
              menuLocation = "PUESTA EN MARCHA -> Asistente Pasos/mm",
              gcodeOrCommand = "\$10x=[NuevoValor]",
              inputFormat = "Pulsación simple",
              description = "Aplica la fórmula matemática exacta (Nuevo = Actual * Comandado / Medido) y graba el nuevo parámetro corregido en la memoria EEPROM."
            ),
            ManualControlItem(
              name = "Lista de Verificación de Certificación en 10 Puntos",
              controlType = "Checklist Interactivo",
              menuLocation = "PUESTA EN MARCHA -> Auditoría de Seguridad",
              gcodeOrCommand = "Validación de puesta en marcha",
              inputFormat = "Casillas de verificación táctiles",
              description = "Guía al técnico a través de las pruebas indispensables: continuidad de tierra, enclavamiento del E-stop, holguras mecánicas y sentido de giro del husillo."
            )
          )
        ),
        ManualMenuCategory(
          id = "settings_screen",
          title = "Menú AJUSTES (Comunicaciones, Perfiles y Preferencias)",
          subtitle = "Selector de transporte, gestión de enlaces USB/Bluetooth, selector de idioma y gestión de energía.",
          badgeColor = DroGreen,
          items = listOf(
            ManualControlItem(
              name = "Selector de Transporte (Simulador / USB Serial / Bluetooth)",
              controlType = "Selector Desplegable / Segmentado",
              menuLocation = "AJUSTES -> Tarjeta Superior",
              gcodeOrCommand = "Capa de enlace de comunicaciones",
              inputFormat = "Elegir la interfaz deseada",
              description = "Cambia el canal activo entre el simulador por software, el puerto físico USB serie OTG o el enlace inalámbrico Bluetooth SPP."
            ),
            ManualControlItem(
              name = "Selector de Dispositivo USB y Menú de Baud Rate",
              controlType = "Menús Desplegables",
              menuLocation = "AJUSTES -> Tarjeta USB Serial",
              gcodeOrCommand = "Configuración serie",
              inputFormat = "Lista de chips USB y 9600 a 250000 bps",
              description = "Permite escoger el chip serie conectado (CH340/FTDI/STM32) y la velocidad de baudios adecuada a su placa."
            ),
            ManualControlItem(
              name = "Botones ESCANEAR Y CONECTAR Bluetooth",
              controlType = "Botones de Acción",
              menuLocation = "AJUSTES -> Tarjeta Bluetooth",
              gcodeOrCommand = "Descubrimiento BT SPP",
              inputFormat = "Pulsar ESCANEAR y elegir dispositivo",
              description = "Busca módulos Bluetooth emparejados y establece la transmisión inalámbrica bidireccional."
            ),
            ManualControlItem(
              name = "Interruptor Modo Eco (Ahorro de Batería y CPU)",
              controlType = "Interruptor Conmutable",
              menuLocation = "AJUSTES -> Gestión de Recursos",
              gcodeOrCommand = "Optimización de refresco y GPU",
              inputFormat = "Activar / Desactivar",
              description = "Reduce la tasa de refresco del DRO y desactiva animaciones decorativas para prolongar al máximo la batería de tablets portátiles en trabajos largos."
            ),
            ManualControlItem(
              name = "Selector de Rol de Seguridad (Operador vs Mantenimiento)",
              controlType = "Botón de Cambio de Rol",
              menuLocation = "AJUSTES -> Apariencia y Rol",
              gcodeOrCommand = "Perfil de acceso",
              inputFormat = "Pulsar para alternar",
              description = "Alterna entre el modo Operador (protege parámetros críticos de modificaciones accidentales) y el modo Ingeniero de Mantenimiento (acceso completo a ajustes $ y calibración)."
            ),
            ManualControlItem(
              name = "Selector de Idioma (EN, ES, DE, FR, JA, KO)",
              controlType = "Franja de Banderas / Idiomas",
              menuLocation = "AJUSTES & MANUAL -> Barra de Idioma",
              gcodeOrCommand = "Localización de la app",
              inputFormat = "Tocar bandera o nombre de idioma",
              description = "Traduce instantáneamente todo el contenido del manual, guías de conexión y descripciones de controles a inglés, español, alemán, francés, japonés o coreano."
            )
          )
        )
      )
    )
  }

  // ==========================================
  // 3. GERMAN CONTENT (DEUTSCH)
  // ==========================================
  private fun getGermanContent(): ManualFullContent {
    val es = getEnglishContent()
    return es.copy(
      language = AppLanguage.DE,
      screenTitle = "BETRIEBSANLEITUNG & TECHNISCHE REFERENZ",
      screenSubtitle = "Vollständige Dokumentation: App-Architektur, Hardware-Verbindung und Definition jedes Buttons, Feldes und Menüs.",
      tabOverview = "1. Was macht die App?",
      tabConnection = "2. Geräteverbindung",
      tabControls = "3. Tasten, Felder & Menüs",
      searchPlaceholder = "Taste, Eingabefeld, G-Code oder Menü suchen (z. B. X+, E-Stop, OTG)...",
      categoryAll = "ALLE MENÜS",
      emptySearchResults = "Keine passenden Steuerelemente oder Menüs gefunden."
    )
  }

  // ==========================================
  // 4. FRENCH CONTENT (FRANÇAIS)
  // ==========================================
  private fun getFrenchContent(): ManualFullContent {
    val es = getEnglishContent()
    return es.copy(
      language = AppLanguage.FR,
      screenTitle = "MANUEL D'UTILISATION & RÉFÉRENCE TECHNIQUE",
      screenSubtitle = "Documentation complète couvrant l'architecture, la connexion matérielle et la définition de chaque bouton, champ et menu.",
      tabOverview = "1. Que fait l'application ?",
      tabConnection = "2. Connexion de l'appareil",
      tabControls = "3. Boutons, Champs & Menus",
      searchPlaceholder = "Rechercher un bouton, champ, code G ou menu (ex. X+, E-Stop, OTG)...",
      categoryAll = "TOUS LES MENUS",
      emptySearchResults = "Aucun élément ne correspond à votre recherche."
    )
  }

  // ==========================================
  // 5. JAPANESE CONTENT (日本語)
  // ==========================================
  private fun getJapaneseContent(): ManualFullContent {
    val es = getEnglishContent()
    return es.copy(
      language = AppLanguage.JA,
      screenTitle = "取扱説明書および技術リファレンス",
      screenSubtitle = "アプリの概要、ハードウェア接続手順、各ボタン・入力フィールド・メニューの詳細解説。",
      tabOverview = "1. アプリの機能概要",
      tabConnection = "2. デバイス接続ガイド",
      tabControls = "3. ボタン・入力項目・メニュー定義",
      searchPlaceholder = "ボタン、入力項目、Gコード、メニューを検索 (例: X+, E-Stop, OTG)...",
      categoryAll = "すべてのメニュー",
      emptySearchResults = "該当する項目が見つかりませんでした。"
    )
  }

  // ==========================================
  // 6. KOREAN CONTENT (한국어)
  // ==========================================
  private fun getKoreanContent(): ManualFullContent {
    val es = getEnglishContent()
    return es.copy(
      language = AppLanguage.KO,
      screenTitle = "사용 설명서 및 기술 참조 매뉴얼",
      screenSubtitle = "앱 기능 개요, 하드웨어 연결 가이드 및 모든 버튼, 입력 필드, 메뉴 상세 정의.",
      tabOverview = "1. 앱 주요 기능",
      tabConnection = "2. 장치 연결 가이드",
      tabControls = "3. 버튼, 입력 필드 및 메뉴 정의",
      searchPlaceholder = "버튼, 입력 필드, G코드 또는 메뉴 검색 (예: X+, 비상정지, OTG)...",
      categoryAll = "전체 메뉴",
      emptySearchResults = "검색 조건과 일치하는 항목이 없습니다."
    )
  }
}
