package com.example

import com.example.model.MachineMode
import com.example.transport.serial.SerialProtocolParser
import com.example.transport.serial.UsbSerialIdentifiers
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testUsbDeviceIdentification() {
    val unoQ = UsbSerialIdentifiers.identifyDevice(0x2341, 0x0068)
    assertEquals("Arduino UNO Q / STM32 Bridge (CDC-ACM)", unoQ.productName)
    assertEquals("Arduino LLC", unoQ.manufacturerName)
    assertTrue(unoQ.isKnownCncHardware)

    val stm32Vcp = UsbSerialIdentifiers.identifyDevice(0x0483, 0x5740)
    assertEquals("STM32 Virtual COM Port (CDC-ACM)", stm32Vcp.productName)
    assertTrue(stm32Vcp.isKnownCncHardware)

    val ch340 = UsbSerialIdentifiers.identifyDevice(0x1A86, 0x7523)
    assertEquals("CH340 USB-to-Serial Converter", ch340.productName)
  }

  @Test
  fun testCrc16Ccitt() {
    val data = "G0 X10 Y20 Z5\n".toByteArray(Charsets.UTF_8)
    val crc = UsbSerialIdentifiers.calculateCrc16Ccitt(data)
    assertTrue(crc in 0..0xFFFF)
  }

  @Test
  fun testGrblStatusReportParsing() {
    val statusLine = "<Idle|WPos:12.500,25.000,-4.750,0.000|FS:1500,12000|Pn:XYZP>"
    val parsed = SerialProtocolParser.parseLine(statusLine)

    assertNotNull(parsed)
    assertEquals(12.500, parsed?.posX ?: 0.0, 0.001)
    assertEquals(25.000, parsed?.posY ?: 0.0, 0.001)
    assertEquals(-4.750, parsed?.posZ ?: 0.0, 0.001)
    assertEquals(1500.0, parsed?.feedRate ?: 0.0, 0.001)
    assertEquals(12000, parsed?.actualRpm)
    assertEquals(true, parsed?.probeTripped)
  }

  @Test
  fun testJsonTelemetryParsing() {
    val jsonLine = """{"state":"Run","pos":{"x":50.2,"y":30.1,"z":-2.5},"rpm":18000,"feed":2200,"mcu_temp":41.5,"jitter_us":1.8}"""
    val parsed = SerialProtocolParser.parseLine(jsonLine)

    assertNotNull(parsed)
    assertEquals(MachineMode.AUTO_GCODE, parsed?.mode)
    assertEquals(50.2, parsed?.posX ?: 0.0, 0.001)
    assertEquals(30.1, parsed?.posY ?: 0.0, 0.001)
    assertEquals(-2.5, parsed?.posZ ?: 0.0, 0.001)
    assertEquals(18000, parsed?.actualRpm)
    assertEquals(2200.0, parsed?.feedRate ?: 0.0, 0.001)
    assertEquals(41.5f, parsed?.mcuTempC ?: 0f, 0.1f)
    assertEquals(1.8f, parsed?.jitterUs ?: 0f, 0.1f)
  }

  @Test
  fun testAlarmAndHalParsing() {
    val alarmLine = "ALARM:3 (Reset while in motion)"
    val alarmParsed = SerialProtocolParser.parseLine(alarmLine)
    assertNotNull(alarmParsed)
    assertEquals(true, alarmParsed?.isEStop)

    val halLine = "HAL:stm32.stepgen.0.freq-khz=125.4"
    val halParsed = SerialProtocolParser.parseLine(halLine)
    assertNotNull(halParsed)
    assertEquals("125.4", halParsed?.halPins?.get("stm32.stepgen.0.freq-khz"))
  }
}
