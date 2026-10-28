package com.example

import com.example.data.PowerScheduleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testParseOutagesFromText_matchesPythonScript() {
        val sampleText = """
            Графік відключення світла на сьогодні для черги 4.2:
            Перша черга: 02:00 - 06:00
            Друга черга: з 10:00 до 14:00
            Третя черга: 18:00—22:00
        """.trimIndent()

        val intervals = PowerScheduleParser.parseOutagesFromText(sampleText)
        assertEquals(3, intervals.size)

        assertEquals("02:00", intervals[0].startStr)
        assertEquals("06:00", intervals[0].endStr)
        assertEquals(120, intervals[0].startMin)
        assertEquals(360, intervals[0].endMin)
        assertEquals("4 год", intervals[0].durationFormatted)

        assertEquals("10:00", intervals[1].startStr)
        assertEquals("14:00", intervals[1].endStr)

        assertEquals("18:00", intervals[2].startStr)
        assertEquals("22:00", intervals[2].endStr)
    }

    @Test
    fun testMidnightEnd() {
        val sampleText = "Відключення: 20:00 - 00:00"
        val intervals = PowerScheduleParser.parseOutagesFromText(sampleText)
        assertEquals(1, intervals.size)
        assertEquals(1200, intervals[0].startMin)
        assertEquals(1440, intervals[0].endMin)
        assertEquals("4 год", intervals[0].durationFormatted)
    }
}
