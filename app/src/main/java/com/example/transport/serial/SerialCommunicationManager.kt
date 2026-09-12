package com.example.transport.serial

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*
import android.os.Build
import android.util.Log
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SerialCommunicationManager(
  private val context: Context,
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
  private val tag = "SerialCommManager"
  private val actionUsbPermission = "com.example.USB_PERMISSION"

  private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager

  // State flows
  private val _connectionState = MutableStateFlow(ConnectionStatus.DISCONNECTED)
  val connectionState: StateFlow<ConnectionStatus> = _connectionState.asStateFlow()

  private val _connectedDeviceInfo = MutableStateFlow<SerialDeviceInfo?>(null)
  val connectedDeviceInfo: StateFlow<SerialDeviceInfo?> = _connectedDeviceInfo.asStateFlow()

  private val _availableDevices = MutableStateFlow<List<SerialDeviceInfo>>(emptyList())
  val availableDevices: StateFlow<List<SerialDeviceInfo>> = _availableDevices.asStateFlow()

  private val _linkStats = MutableStateFlow(SerialLinkStats())
  val linkStats: StateFlow<SerialLinkStats> = _linkStats.asStateFlow()

  private val _serialLogs = MutableStateFlow<List<SerialLogEntry>>(emptyList())
  val serialLogs: StateFlow<List<SerialLogEntry>> = _serialLogs.asStateFlow()

  private val _portConfig = MutableStateFlow(SerialPortConfig())
  val portConfig: StateFlow<SerialPortConfig> = _portConfig.asStateFlow()

  // Hardware handles
  private var activeDevice: UsbDevice? = null
  private var activeConnection: UsbDeviceConnection? = null
  private var dataInterface: UsbInterface? = null
  private var controlInterface: UsbInterface? = null
  private var inEndpoint: UsbEndpoint? = null
  private var outEndpoint: UsbEndpoint? = null

  private var readJob: Job? = null
  private var watchdogJob: Job? = null
  private var statsJob: Job? = null

  private val writeMutex = Mutex()
  var isEcoMode: Boolean = false
  private val maxLogEntries: Int get() = if (isEcoMode) 60 else 120
  private val logBuffer = ArrayDeque<SerialLogEntry>(120)
  private val logLock = Any()

  // Callback for parsed lines / telemetry
  var onLineReceived: ((String) -> Unit)? = null
  var onStatusReceived: ((SerialProtocolParser.ParsedStatusUpdate) -> Unit)? = null

  private val usbReceiver = object : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
      when (intent.action) {
        actionUsbPermission -> {
          synchronized(this) {
            val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
              @Suppress("DEPRECATION")
              intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
              device?.let {
                addLog(SerialDirection.INFO, "USB Permission granted for ${it.deviceName}")
                openDeviceInternal(it)
              }
            } else {
              addLog(SerialDirection.ERROR, "USB Permission denied by user for ${device?.deviceName}")
              _connectionState.value = ConnectionStatus.DISCONNECTED
            }
          }
        }
        UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
          addLog(SerialDirection.INFO, "USB Device Attached. Refreshing devices...")
          scanDevices()
        }
        UsbManager.ACTION_USB_DEVICE_DETACHED -> {
          val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
          } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
          }
          if (device != null && device == activeDevice) {
            addLog(SerialDirection.ERROR, "Active USB Hardware detached! Triggering Safe Disconnect.")
            disconnect()
          }
          scanDevices()
        }
      }
    }
  }

  init {
    registerUsbReceiver()
    scanDevices()
    startStatsMonitoring()
  }

  private fun registerUsbReceiver() {
    try {
      val filter = IntentFilter().apply {
        addAction(actionUsbPermission)
        addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
      } else {
        context.registerReceiver(usbReceiver, filter)
      }
    } catch (e: Exception) {
      Log.w(tag, "Failed to register USB receiver: ${e.message}")
    }
  }

  fun unregister() {
    try {
      context.unregisterReceiver(usbReceiver)
    } catch (e: Exception) {
      // Ignore if not registered
    }
    disconnect()
  }

  /**
   * Scan for attached USB Serial hardware (Arduino UNO Q, STM32, FTDI, CH340, CP2102)
   */
  fun scanDevices(): List<SerialDeviceInfo> {
    val manager = usbManager ?: return emptyList()
    val list = mutableListOf<SerialDeviceInfo>()
    
    val deviceList = manager.deviceList
    for ((_, device) in deviceList) {
      val isSupported = UsbSerialIdentifiers.isSupportedDevice(device)
      val driverDesc = UsbSerialIdentifiers.identifyDriver(device)
      
      val info = SerialDeviceInfo(
        deviceName = device.deviceName,
        deviceId = device.deviceId,
        vendorId = device.vendorId,
        productId = device.productId,
        manufacturerName = try { device.manufacturerName ?: "Generic" } catch (_: Exception) { "Generic" },
        productName = try { device.productName ?: driverDesc } catch (_: Exception) { driverDesc },
        serialNumber = try { device.serialNumber ?: "N/A" } catch (_: Exception) { "N/A" },
        driverType = driverDesc,
        interfaceCount = device.interfaceCount
      )
      list.add(info)
    }
    _availableDevices.value = list
    return list
  }

  /**
   * Update baud rate and port configuration
   */
  fun updatePortConfig(newConfig: SerialPortConfig) {
    _portConfig.value = newConfig
    if (_connectionState.value == ConnectionStatus.CONNECTED_USB) {
      applyLineCoding(newConfig)
    }
  }

  /**
   * Connect to specified USB device or the first available supported hardware
   */
  fun connect(deviceInfo: SerialDeviceInfo? = null, config: SerialPortConfig = _portConfig.value) {
    _portConfig.value = config
    val manager = usbManager
    if (manager == null) {
      addLog(SerialDirection.ERROR, "UsbManager unavailable on this device.")
      return
    }

    val deviceToOpen = if (deviceInfo != null) {
      manager.deviceList.values.firstOrNull { it.deviceId == deviceInfo.deviceId || it.deviceName == deviceInfo.deviceName }
    } else {
      manager.deviceList.values.firstOrNull { UsbSerialIdentifiers.isSupportedDevice(it) }
        ?: manager.deviceList.values.firstOrNull()
    }

    if (deviceToOpen == null) {
      addLog(SerialDirection.ERROR, "No USB Serial / Arduino / STM32 hardware found on USB bus.")
      _connectionState.value = ConnectionStatus.DISCONNECTED
      return
    }

    _connectionState.value = ConnectionStatus.CONNECTING
    addLog(SerialDirection.INFO, "Found target: ${deviceToOpen.productName ?: deviceToOpen.deviceName} (VID: 0x${Integer.toHexString(deviceToOpen.vendorId).uppercase()})")

    if (!manager.hasPermission(deviceToOpen)) {
      addLog(SerialDirection.INFO, "Requesting USB permission for ${deviceToOpen.deviceName}...")
      val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      } else {
        PendingIntent.FLAG_UPDATE_CURRENT
      }
      val permIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(actionUsbPermission),
        flags
      )
      manager.requestPermission(deviceToOpen, permIntent)
    } else {
      openDeviceInternal(deviceToOpen)
    }
  }

  private fun openDeviceInternal(device: UsbDevice) {
    val manager = usbManager ?: return
    try {
      val connection = manager.openDevice(device)
      if (connection == null) {
        addLog(SerialDirection.ERROR, "Failed to open USB Device Connection (returned null).")
        _connectionState.value = ConnectionStatus.DISCONNECTED
        return
      }

      activeDevice = device
      activeConnection = connection

      // Find communication & data interfaces
      var claimedDataIface: UsbInterface? = null
      var foundInEp: UsbEndpoint? = null
      var foundOutEp: UsbEndpoint? = null

      for (i in 0 until device.interfaceCount) {
        val iface = device.getInterface(i)
        // Check for CDC Data interface or generic vendor interface with bulk endpoints
        for (j in 0 until iface.endpointCount) {
          val ep = iface.getEndpoint(j)
          if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
            if (ep.direction == UsbConstants.USB_DIR_IN && foundInEp == null) {
              foundInEp = ep
              claimedDataIface = iface
            } else if (ep.direction == UsbConstants.USB_DIR_OUT && foundOutEp == null) {
              foundOutEp = ep
              claimedDataIface = iface
            }
          }
        }
      }

      if (claimedDataIface == null || foundInEp == null || foundOutEp == null) {
        // Fallback: claim interface 0 or first available
        for (i in 0 until device.interfaceCount) {
          val iface = device.getInterface(i)
          connection.claimInterface(iface, true)
          for (j in 0 until iface.endpointCount) {
            val ep = iface.getEndpoint(j)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK || ep.type == UsbConstants.USB_ENDPOINT_XFER_INT) {
              if (ep.direction == UsbConstants.USB_DIR_IN && foundInEp == null) foundInEp = ep
              if (ep.direction == UsbConstants.USB_DIR_OUT && foundOutEp == null) foundOutEp = ep
            }
          }
          if (foundInEp != null && foundOutEp != null) {
            claimedDataIface = iface
            break
          }
        }
      } else {
        connection.claimInterface(claimedDataIface, true)
      }

      dataInterface = claimedDataIface
      inEndpoint = foundInEp
      outEndpoint = foundOutEp

      // Also claim control interface if separate
      if (device.interfaceCount > 1) {
        controlInterface = device.getInterface(0)
        connection.claimInterface(controlInterface, true)
      }

      // Configure Serial Parameters (Baud rate, 8N1, DTR/RTS)
      applyLineCoding(_portConfig.value)

      val devInfo = SerialDeviceInfo(
        deviceName = device.deviceName,
        deviceId = device.deviceId,
        vendorId = device.vendorId,
        productId = device.productId,
        manufacturerName = try { device.manufacturerName ?: "Arduino/STMicroelectronics" } catch (_: Exception) { "Arduino/STMicroelectronics" },
        productName = try { device.productName ?: UsbSerialIdentifiers.identifyDriver(device) } catch (_: Exception) { UsbSerialIdentifiers.identifyDriver(device) },
        serialNumber = try { device.serialNumber ?: "SN-UNOQ-001" } catch (_: Exception) { "SN-UNOQ-001" },
        driverType = UsbSerialIdentifiers.identifyDriver(device),
        interfaceCount = device.interfaceCount
      )
      _connectedDeviceInfo.value = devInfo
      _connectionState.value = ConnectionStatus.CONNECTED_USB

      addLog(SerialDirection.INFO, "Serial link established: ${devInfo.productName} @ ${_portConfig.value.baudRate} bps (8N1)")

      // Start asynchronous reader and watchdog loops
      startReaderLoop()
      startWatchdogHeartbeatLoop()

      // Query controller ID / status
      coroutineScope.launch {
        writeString("?\n", isInternalTelemetry = true)
        writeString("$$\n")
      }

    } catch (e: Exception) {
      Log.e(tag, "Error opening USB serial device", e)
      addLog(SerialDirection.ERROR, "Exception opening USB serial: ${e.message}")
      disconnect()
    }
  }

  /**
   * Apply CDC-ACM Line Coding & Control Line state (DTR/RTS)
   */
  private fun applyLineCoding(config: SerialPortConfig) {
    val conn = activeConnection ?: return
    try {
      // 1. SET_LINE_CODING (CDC class-specific request 0x20)
      // 7-byte structure: [Baud (4 bytes LE)][StopBits (1 byte)][Parity (1 byte)][DataBits (1 byte)]
      val lineCoding = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN).apply {
        putInt(config.baudRate)
        put(config.stopBits.code.toByte())
        put(config.parity.code.toByte())
        put(config.dataBits.toByte())
      }.array()

      conn.controlTransfer(0x21, UsbSerialIdentifiers.REQ_SET_LINE_CODING, 0, 0, lineCoding, lineCoding.size, 500)

      // 2. SET_CONTROL_LINE_STATE (0x22): Bit 0 = DTR, Bit 1 = RTS
      var ctrlValue = 0
      if (config.dtrEnable) ctrlValue = ctrlValue or 0x01
      if (config.rtsEnable) ctrlValue = ctrlValue or 0x02

      conn.controlTransfer(0x21, UsbSerialIdentifiers.REQ_SET_CONTROL_LINE_STATE, ctrlValue, 0, null, 0, 500)

    } catch (e: Exception) {
      Log.w(tag, "Failed to apply line coding: ${e.message}")
    }
  }

  /**
   * Toggle DTR / RTS hardware lines
   */
  fun setDtrRts(dtr: Boolean, rts: Boolean) {
    _portConfig.value = _portConfig.value.copy(dtrEnable = dtr, rtsEnable = rts)
    val conn = activeConnection ?: return
    try {
      var ctrlValue = 0
      if (dtr) ctrlValue = ctrlValue or 0x01
      if (rts) ctrlValue = ctrlValue or 0x02
      conn.controlTransfer(0x21, UsbSerialIdentifiers.REQ_SET_CONTROL_LINE_STATE, ctrlValue, 0, null, 0, 300)
      addLog(SerialDirection.INFO, "Hardware lines updated: DTR=$dtr, RTS=$rts")
    } catch (e: Exception) {
      addLog(SerialDirection.ERROR, "Failed to set DTR/RTS: ${e.message}")
    }
  }

  /**
   * Asynchronous USB Bulk IN reader loop with circular stream slicing
   */
  private fun startReaderLoop() {
    readJob?.cancel()
    readJob = coroutineScope.launch {
      val inEp = inEndpoint ?: return@launch
      val conn = activeConnection ?: return@launch

      val readBuffer = ByteArray(4096)
      val lineStreamBuffer = ByteArrayOutputStream()

      while (isActive && _connectionState.value == ConnectionStatus.CONNECTED_USB) {
        val bytesRead = try {
          conn.bulkTransfer(inEp, readBuffer, readBuffer.size, _portConfig.value.readTimeoutMs)
        } catch (e: Exception) {
          -1
        }

        if (bytesRead > 0) {
          _linkStats.value = _linkStats.value.copy(
            bytesReceived = _linkStats.value.bytesReceived + bytesRead,
            packetsReceived = _linkStats.value.packetsReceived + 1,
            lastHeartbeatTimestamp = System.currentTimeMillis(),
            isLinkActive = true
          )

          // Process incoming stream
          for (i in 0 until bytesRead) {
            val b = readBuffer[i]
            if (b == '\n'.code.toByte() || b == '\r'.code.toByte()) {
              if (lineStreamBuffer.size() > 0) {
                val line = lineStreamBuffer.toString("UTF-8").trim()
                lineStreamBuffer.reset()
                if (line.isNotEmpty()) {
                  handleIncomingLine(line)
                }
              }
            } else {
              lineStreamBuffer.write(b.toInt())
              if (lineStreamBuffer.size() > 2048) { // Safety overflow protection
                lineStreamBuffer.reset()
              }
            }
          }
        } else {
          // Idle delay to prevent CPU spinning
          delay(10)
        }
      }
    }
  }

  private fun handleIncomingLine(line: String) {
    addLog(SerialDirection.RX, line)
    onLineReceived?.invoke(line)
    
    val parsed = SerialProtocolParser.parseLine(line)
    if (parsed != null) {
      onStatusReceived?.invoke(parsed)
    }
  }

  /**
   * Watchdog pet sender loop to keep the Arduino UNO Q / STM32 safety relay open
   */
  private fun startWatchdogHeartbeatLoop() {
    watchdogJob?.cancel()
    watchdogJob = coroutineScope.launch {
      while (isActive && _connectionState.value == ConnectionStatus.CONNECTED_USB) {
        val watchdogDelay = if (isEcoMode) 500L else 200L
        delay(watchdogDelay) // Adaptive status poll & watchdog heartbeat
        writeString("?\n", isInternalTelemetry = true)
      }
    }
  }

  private fun startStatsMonitoring() {
    statsJob?.cancel()
    statsJob = coroutineScope.launch {
      while (isActive) {
        delay(1000)
        val now = System.currentTimeMillis()
        val lastHb = _linkStats.value.lastHeartbeatTimestamp
        val isLinkAlive = (_connectionState.value == ConnectionStatus.CONNECTED_USB) && (now - lastHb < 3000)
        _linkStats.value = _linkStats.value.copy(
          isLinkActive = isLinkAlive,
          connectedDeviceSummary = _connectedDeviceInfo.value?.productName ?: "No serial device connected"
        )
      }
    }
  }

  /**
   * Send a raw string or G-code block to the Arduino UNO Q / STM32
   */
  suspend fun writeString(data: String, isInternalTelemetry: Boolean = false): Boolean {
    return writeBytes(data.toByteArray(Charsets.UTF_8), logText = if (!isInternalTelemetry) data.trim() else null)
  }

  /**
   * Send raw byte array over USB Bulk OUT endpoint
   */
  suspend fun writeBytes(bytes: ByteArray, logText: String? = null): Boolean = writeMutex.withLock {
    val outEp = outEndpoint
    val conn = activeConnection

    if (conn == null || outEp == null || _connectionState.value != ConnectionStatus.CONNECTED_USB) {
      if (logText != null) {
        addLog(SerialDirection.ERROR, "Cannot send: Serial link not connected.")
      }
      return false
    }

    return try {
      val transferred = conn.bulkTransfer(outEp, bytes, bytes.size, _portConfig.value.writeTimeoutMs)
      if (transferred >= 0) {
        _linkStats.value = _linkStats.value.copy(
          bytesSent = _linkStats.value.bytesSent + transferred,
          packetsSent = _linkStats.value.packetsSent + 1
        )
        if (logText != null) {
          addLog(SerialDirection.TX, logText, UsbSerialIdentifiers.bytesToHex(bytes))
        }
        true
      } else {
        addLog(SerialDirection.ERROR, "USB write failed (bulkTransfer returned $transferred)")
        false
      }
    } catch (e: Exception) {
      addLog(SerialDirection.ERROR, "USB Write exception: ${e.message}")
      false
    }
  }

  /**
   * Emergency immediate soft-reset / Feed Hold character bypass
   */
  fun sendRealtimeChar(char: Char) {
    coroutineScope.launch {
      writeBytes(byteArrayOf(char.code.toByte()), logText = "<REALTIME_CHAR: '$char' (0x${Integer.toHexString(char.code)})>")
    }
  }

  /**
   * Send real-time feedrate speed override command over serial link
   */
  fun sendFeedOverride(percentage: Int) {
    coroutineScope.launch {
      // 1. Send standard modal M220 S{pct} speed override
      writeString("M220 S$percentage\n")
      // 2. Grbl v1.1 real-time single byte feed override commands:
      // 0x90 = 100% reset, 0x91 = +10%, 0x92 = -10%, 0x93 = +1%, 0x94 = -1%
      if (percentage == 100) {
        sendRealtimeChar('\u0090')
      }
    }
  }

  private fun addLog(direction: SerialDirection, text: String, hexRep: String = "") {
    val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    val entry = SerialLogEntry(
      timestamp = time,
      direction = direction,
      text = text,
      hexRepresentation = hexRep
    )
    val snapshot = synchronized(logLock) {
      if (logBuffer.size >= maxLogEntries) {
        logBuffer.removeFirst()
      }
      logBuffer.addLast(entry)
      logBuffer.toList()
    }
    _serialLogs.value = snapshot
  }

  fun clearLogs() {
    synchronized(logLock) {
      logBuffer.clear()
    }
    _serialLogs.value = emptyList()
  }

  /**
   * Safely disconnect and release USB interfaces
   */
  fun disconnect() {
    readJob?.cancel()
    watchdogJob?.cancel()

    try {
      dataInterface?.let { activeConnection?.releaseInterface(it) }
      controlInterface?.let { activeConnection?.releaseInterface(it) }
      activeConnection?.close()
    } catch (e: Exception) {
      Log.w(tag, "Error closing connection: ${e.message}")
    }

    activeConnection = null
    activeDevice = null
    dataInterface = null
    controlInterface = null
    inEndpoint = null
    outEndpoint = null

    _connectionState.value = ConnectionStatus.DISCONNECTED
    _connectedDeviceInfo.value = null
    _linkStats.value = _linkStats.value.copy(isLinkActive = false)
    addLog(SerialDirection.INFO, "Serial link closed.")
  }
}
