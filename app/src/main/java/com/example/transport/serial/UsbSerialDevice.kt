package com.example.transport.serial

import android.hardware.usb.UsbDevice

object UsbSerialIdentifiers {
  // Vendor IDs
  const val VID_ARDUINO = 0x2341
  const val VID_STMICROELECTRONICS = 0x0483
  const val VID_FTDI = 0x0403
  const val VID_SILICON_LABS = 0x10C4
  const val VID_WCH = 0x1A86
  const val VID_RASPBERRY_PI = 0x2E8A
  const val VID_TEENSY = 0x16C0

  // Known Arduino / STM32 Product IDs
  const val PID_ARDUINO_UNO_Q = 0x0068 // Arduino UNO Q dual core ARM64 / STM32
  const val PID_ARDUINO_UNO = 0x0043
  const val PID_ARDUINO_MEGA_2560 = 0x0042
  const val PID_ARDUINO_DUE = 0x003E
  const val PID_ARDUINO_NANO_EVERY = 0x0058
  const val PID_STM32_VIRTUAL_COM_PORT = 0x5740
  const val PID_STM32_BOOTLOADER = 0xDF11
  const val PID_STM32_CDC = 0x374B
  const val PID_CH340 = 0x7523
  const val PID_CP2102 = 0xEA60

  fun identifyDevice(vid: Int, pid: Int): com.example.model.SerialDeviceInfo {
    val productName = when (vid) {
      VID_ARDUINO -> when (pid) {
        PID_ARDUINO_UNO_Q -> "Arduino UNO Q / STM32 Bridge (CDC-ACM)"
        PID_ARDUINO_UNO -> "Arduino UNO R3/R4 (CDC-ACM)"
        PID_ARDUINO_MEGA_2560 -> "Arduino Mega 2560 (CDC-ACM)"
        PID_ARDUINO_DUE -> "Arduino Due Native/Prog USB"
        else -> "Arduino USB Device (${String.format("0x%04X:0x%04X", vid, pid)})"
      }
      VID_STMICROELECTRONICS -> when (pid) {
        PID_STM32_VIRTUAL_COM_PORT -> "STM32 Virtual COM Port (CDC-ACM)"
        PID_STM32_CDC -> "STM32 ST-LINK VCP"
        else -> "STM32 Microcontroller (${String.format("0x%04X:0x%04X", vid, pid)})"
      }
      VID_WCH -> "CH340 USB-to-Serial Converter"
      VID_SILICON_LABS -> "Silicon Labs CP210x USB-UART"
      VID_FTDI -> "FTDI FT232 / FT2232 USB-UART"
      VID_RASPBERRY_PI -> "RP2040 / RP2350 (CDC-ACM)"
      VID_TEENSY -> "Teensy USB Serial"
      else -> "USB Serial Device (${String.format("0x%04X:0x%04X", vid, pid)})"
    }

    val manufacturer = when (vid) {
      VID_ARDUINO -> "Arduino LLC"
      VID_STMICROELECTRONICS -> "STMicroelectronics"
      VID_WCH -> "WCH"
      VID_SILICON_LABS -> "Silicon Labs"
      VID_FTDI -> "FTDI"
      VID_RASPBERRY_PI -> "Raspberry Pi"
      else -> "Generic USB"
    }

    val isCnc = (vid == VID_ARDUINO && (pid == PID_ARDUINO_UNO_Q || pid == PID_ARDUINO_UNO || pid == PID_ARDUINO_MEGA_2560)) ||
                (vid == VID_STMICROELECTRONICS)

    return com.example.model.SerialDeviceInfo(
      deviceName = "usb_port",
      vendorId = vid,
      productId = pid,
      productName = productName,
      manufacturerName = manufacturer,
      driverType = "CDC-ACM",
      isKnownCncHardware = isCnc
    )
  }

  fun identifyDriver(device: UsbDevice): String {
    val vid = device.vendorId
    val pid = device.productId
    return when (vid) {
      VID_ARDUINO -> when (pid) {
        PID_ARDUINO_UNO_Q -> "Arduino UNO Q (CDC-ACM / STM32U585)"
        PID_ARDUINO_UNO -> "Arduino UNO R3/R4 (CDC-ACM)"
        PID_ARDUINO_MEGA_2560 -> "Arduino Mega 2560 (CDC-ACM)"
        PID_ARDUINO_DUE -> "Arduino Due Native/Prog USB"
        else -> "Arduino USB Device (${String.format("0x%04X:0x%04X", vid, pid)})"
      }
      VID_STMICROELECTRONICS -> when (pid) {
        PID_STM32_VIRTUAL_COM_PORT -> "STM32 Virtual COM Port (CDC-ACM)"
        PID_STM32_CDC -> "STM32 ST-LINK VCP"
        else -> "STM32 Microcontroller (${String.format("0x%04X:0x%04X", vid, pid)})"
      }
      VID_WCH -> "CH340 / CH341 USB-UART"
      VID_SILICON_LABS -> "Silicon Labs CP210x USB-UART"
      VID_FTDI -> "FTDI FT232 / FT2232 USB-UART"
      VID_RASPBERRY_PI -> "RP2040 / RP2350 (CDC-ACM)"
      VID_TEENSY -> "Teensy USB Serial"
      else -> {
        // Check if device has CDC interface
        val isCdc = (0 until device.interfaceCount).any { i ->
          val iface = device.getInterface(i)
          iface.interfaceClass == 0x02 || iface.interfaceClass == 0x0A
        }
        if (isCdc) "Standard USB CDC-ACM Serial" else "Generic USB Device"
      }
    }
  }

  fun isSupportedDevice(device: UsbDevice): Boolean {
    val vid = device.vendorId
    if (vid in listOf(VID_ARDUINO, VID_STMICROELECTRONICS, VID_FTDI, VID_SILICON_LABS, VID_WCH, VID_RASPBERRY_PI, VID_TEENSY)) {
      return true
    }
    // Check USB classes
    for (i in 0 until device.interfaceCount) {
      val iface = device.getInterface(i)
      if (iface.interfaceClass == 0x02 || iface.interfaceClass == 0x0A || iface.interfaceClass == 0xFF) {
        return true
      }
    }
    return false
  }

  // CDC-ACM Control Request Constants
  const val REQ_SET_LINE_CODING = 0x20
  const val REQ_GET_LINE_CODING = 0x21
  const val REQ_SET_CONTROL_LINE_STATE = 0x22
  const val REQ_SEND_BREAK = 0x23

  // CP210x Specific
  const val CP210X_IFC_ENABLE = 0x00
  const val CP210X_SET_BAUDDIV = 0x01
  const val CP210X_SET_LINE_CTL = 0x03
  const val CP210X_SET_MHS = 0x07
  const val CP210X_SET_BAUDRATE = 0x1E

  // CH340 Specific
  const val CH341_REQ_WRITE_REG = 0x9A
  const val CH341_REQ_READ_REG = 0x95
  const val CH341_REG_BREAK1 = 0x05
  const val CH341_REG_BREAK2 = 0x18
  const val CH341_NBREAK_BITS = 0x01

  // Utilities
  fun bytesToHex(bytes: ByteArray, length: Int = bytes.size): String {
    val sb = StringBuilder()
    val limit = minOf(bytes.size, length)
    for (i in 0 until limit) {
      sb.append(String.format("%02X ", bytes[i]))
    }
    return sb.toString().trim()
  }

  fun calculateCrc16Ccitt(data: ByteArray, offset: Int = 0, length: Int = data.size): Int {
    var crc = 0xFFFF
    for (i in offset until (offset + length)) {
      crc = ((crc ushr 8) or (crc shl 8)) and 0xFFFF
      crc = crc xor (data[i].toInt() and 0xFF)
      crc = crc xor ((crc and 0xFF) ushr 4)
      crc = crc xor ((crc shl 12) and 0xFFFF)
      crc = crc xor (((crc and 0xFF) shl 5) and 0xFFFF)
    }
    return crc and 0xFFFF
  }
}
