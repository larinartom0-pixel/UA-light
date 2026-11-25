package com.example

import com.example.data.PowerScheduleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testParseNewEnergyUaSchedulePayload_exactSiteHtml() {
        val sampleHtml = """
            <!doctype html>
            <html>
            <body>
            <div class="timeline-track" id="timelineTrack"></div>
            <script id="schedulePayload" type="application/json">{"today":[{"from":630,"to":810,"status":"red"},{"from":990,"to":1170,"status":"red"},{"from":1350,"to":1440,"status":"red"}],"tomorrow":[],"tomorrowAvailable":false,"currentTime":1795626328,"dates":{"today":"25.11.2026","tomorrow":"26.11.2026"},"nextEventTimestamp":1795627800,"isCurrentlyOff":true,"isEmergency":false}</script>
            </body>
            </html>
        """.trimIndent()

        val result = PowerScheduleParser.parseHtml("4.2", sampleHtml)
        assertNotNull(result)
        val data = result!!

        assertEquals("4.2", data.queueCode)
        assertEquals("25.11.2026", data.todayDate)
        assertEquals("26.11.2026", data.tomorrowDate)
        assertFalse(data.hasTomorrow)
        assertFalse(data.hasSecondaryForDay(com.example.model.ScheduleDay.TODAY))

        assertEquals(3, data.primaryIntervals.size)
        assertEquals("10:30", data.primaryIntervals[0].startStr)
        assertEquals("13:30", data.primaryIntervals[0].endStr)
        assertEquals("3 год", data.primaryIntervals[0].durationFormatted)

        assertEquals("16:30", data.primaryIntervals[1].startStr)
        assertEquals("19:30", data.primaryIntervals[1].endStr)

        assertEquals("22:30", data.primaryIntervals[2].startStr)
        assertEquals("24:00", data.primaryIntervals[2].endStr)
        assertEquals("1 год 30 хв", data.primaryIntervals[2].durationFormatted)
    }

    @Test
    fun testParseNewEnergyUaPayload_withYellowAndTomorrow() {
        val sampleHtml = """
            <script type="application/json" id="schedulePayload">
            {"today":[{"from":600,"to":720,"status":"red"},{"from":720,"to":780,"status":"yellow"}],"tomorrow":[{"from":360,"to":480,"status":"red"}],"tomorrowAvailable":true,"dates":{"today":"25.11.2026","tomorrow":"26.11.2026"},"isEmergency":false}
            </script>
        """.trimIndent()

        val data = PowerScheduleParser.parseHtml("1.1", sampleHtml)!!
        assertEquals(1, data.primaryIntervals.size)
        assertEquals("10:00", data.primaryIntervals[0].startStr)
        assertEquals("12:00", data.primaryIntervals[0].endStr)

        assertTrue(data.hasSecondaryForDay(com.example.model.ScheduleDay.TODAY))
        assertEquals(1, data.secondaryIntervals.size)
        assertEquals("10:00", data.secondaryIntervals[0].startStr)
        assertEquals("13:00", data.secondaryIntervals[0].endStr)

        assertTrue(data.hasTomorrow)
        assertEquals(1, data.tomorrowPrimaryIntervals.size)
        assertEquals("06:00", data.tomorrowPrimaryIntervals[0].startStr)
        assertEquals("08:00", data.tomorrowPrimaryIntervals[0].endStr)
    }

    @Test
    fun testParseNewEnergyUaPayload_emptyToday_meansNoOutages() {
        val sampleHtml = """
            <script id="schedulePayload">{"today":[],"tomorrow":[],"tomorrowAvailable":false,"dates":{"today":"25.11.2026","tomorrow":"26.11.2026"},"isEmergency":false}</script>
        """.trimIndent()

        val data = PowerScheduleParser.parseHtml("2.1", sampleHtml)
        assertNotNull(data)
        assertTrue(data!!.primaryIntervals.isEmpty())
        assertTrue(data.isLiveFromNetwork)
    }

    @Test
    fun testTwoSchedulesSeparation_legacyTextFallback() {
        val sampleText = """
            06:30 - 08:00
            07:30 - 09:30
            12:30 - 15:00
            13:30 - 16:00
        """.trimIndent()

        val (primary, secondary) = PowerScheduleParser.parseSchedulesFromText(sampleText)
        assertEquals(2, primary.size)
        assertEquals(2, secondary.size)
        assertEquals("06:30", primary[0].startStr)
        assertEquals("07:30", secondary[0].startStr)
    }

    @Test
    fun testPythonUrllibHttpClient_chunkedUtf8Response() {
        val part1 = "Привіт ".toByteArray(Charsets.UTF_8)
        val part2 = "Світло!".toByteArray(Charsets.UTF_8)
        val raw = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: text/html; charset=UTF-8\r\n")
            append("Transfer-Encoding: chunked\r\n\r\n")
            append(Integer.toHexString(part1.size)).append("\r\n")
        }.toByteArray(Charsets.ISO_8859_1) +
            part1 +
            "\r\n${Integer.toHexString(part2.size)}\r\n".toByteArray(Charsets.ISO_8859_1) +
            part2 +
            "\r\n0\r\n\r\n".toByteArray(Charsets.ISO_8859_1)

        val parsed = com.example.data.PythonUrllibHttpClient.parseHttpResponse(raw.inputStream())
        assertNotNull(parsed)
        assertEquals(200, parsed!!.first)
        assertEquals("Привіт Світло!", parsed.third)
    }
}
