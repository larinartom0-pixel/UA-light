package com.example

import com.example.data.PowerScheduleParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTwoSchedulesSeparation_screenshotCase() {
        val sampleText = """
            06:30 - 08:00
            07:30 - 09:30
            12:30 - 15:00
            13:30 - 16:00
            18:30 - 21:30
            19:30 - 21:00
        """.trimIndent()

        val (primary, secondary) = PowerScheduleParser.parseSchedulesFromText(sampleText)
        assertEquals(3, primary.size)
        assertEquals(3, secondary.size)

        // Primary schedule
        assertEquals("06:30", primary[0].startStr)
        assertEquals("08:00", primary[0].endStr)
        assertEquals("12:30", primary[1].startStr)
        assertEquals("15:00", primary[1].endStr)
        assertEquals("18:30", primary[2].startStr)
        assertEquals("21:30", primary[2].endStr)

        // Secondary schedule (alternative/reserve)
        assertEquals("07:30", secondary[0].startStr)
        assertEquals("09:30", secondary[0].endStr)
        assertEquals("13:30", secondary[1].startStr)
        assertEquals("16:00", secondary[1].endStr)
        assertEquals("19:30", secondary[2].startStr)
        assertEquals("21:00", secondary[2].endStr)
    }

    @Test
    fun testSingleScheduleParsing() {
        val sampleText = """
            02:00 - 06:00
            з 10:00 до 14:00
            18:00—22:00
        """.trimIndent()

        val intervals = PowerScheduleParser.parseSingleSchedule(sampleText)
        assertEquals(3, intervals.size)
        assertEquals("02:00", intervals[0].startStr)
        assertEquals("06:00", intervals[0].endStr)
        assertEquals("4 год", intervals[0].durationFormatted)
    }

    @Test
    fun testMidnightEnd() {
        val sampleText = "Відключення: 20:00 - 00:00"
        val intervals = PowerScheduleParser.parseSingleSchedule(sampleText)
        assertEquals(1, intervals.size)
        assertEquals(1200, intervals[0].startMin)
        assertEquals(1440, intervals[0].endMin)
        assertEquals("4 год", intervals[0].durationFormatted)
    }
}
