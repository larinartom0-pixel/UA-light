package com.example.data

import com.example.model.OutageInterval

object PowerScheduleParser {

    // Regex matching "18:00 - 20:00" or "з 18:00 до 20:00" or "18:00—20:00" or "18:00–20:00"
    private val INTERVAL_REGEX = Regex(
        """(\d{1,2}:\d{2})\s*(?:-|—|–|до)\s*(\d{1,2}:\d{2})""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Parses outage intervals from raw text or HTML body.
     */
    fun parseOutagesFromText(rawText: String): List<OutageInterval> {
        val cleanText = stripHtmlTags(rawText)
        val matches = INTERVAL_REGEX.findAll(cleanText).toList()
        if (matches.isEmpty()) {
            return emptyList()
        }

        val list = mutableListOf<OutageInterval>()
        for (match in matches) {
            val startStr = match.groupValues[1].trim()
            val endStr = match.groupValues[2].trim()

            val startParts = startStr.split(":")
            val endParts = endStr.split(":")
            if (startParts.size == 2 && endParts.size == 2) {
                val sh = startParts[0].toIntOrNull() ?: continue
                val sm = startParts[1].toIntOrNull() ?: continue
                val eh = endParts[0].toIntOrNull() ?: continue
                val em = endParts[1].toIntOrNull() ?: continue

                if (sh in 0..23 && sm in 0..59 && eh in 0..24 && em in 0..59) {
                    val startMin = sh * 60 + sm
                    var endMin = eh * 60 + em

                    // If outage ends at midnight (00:00 or 24:00)
                    if (endMin == 0 && (eh == 0 || eh == 24)) {
                        endMin = 24 * 60
                    }

                    if (endMin > startMin) {
                        val interval = OutageInterval(
                            startMin = startMin,
                            endMin = endMin,
                            startStr = formatTime(sh, sm),
                            endStr = if (endMin == 1440) "24:00" else formatTime(eh, em)
                        )
                        list.add(interval)
                    }
                }
            }
        }

        // Sort by start minute and remove exact duplicates
        return list.distinctBy { "${it.startMin}-${it.endMin}" }.sortedBy { it.startMin }
    }

    private fun formatTime(hour: Int, min: Int): String {
        return String.format("%02d:%02d", hour, min)
    }

    /**
     * Strips HTML tags and entities to replicate BeautifulSoup's get_text()
     */
    fun stripHtmlTags(html: String): String {
        return html
            .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Default realistic fallback schedules for standard Ukrainian queues
     * (e.g. 1.1 through 6.2) when offline or blocked by Cloudflare anti-bot.
     */
    fun getPresetScheduleForQueue(queueCode: String): List<OutageInterval> {
        val normalized = queueCode.replace("-", ".").trim()
        return when (normalized) {
            "4.2", "4-2" -> listOf(
                createInterval("02:00", "06:00"),
                createInterval("10:00", "14:00"),
                createInterval("18:00", "22:00")
            )
            "4.1", "4-1" -> listOf(
                createInterval("00:00", "04:00"),
                createInterval("08:00", "12:00"),
                createInterval("16:00", "20:00")
            )
            "1.1", "1-1" -> listOf(
                createInterval("00:00", "03:00"),
                createInterval("07:00", "11:00"),
                createInterval("15:00", "19:00"),
                createInterval("23:00", "24:00")
            )
            "1.2", "1-2" -> listOf(
                createInterval("01:00", "05:00"),
                createInterval("09:00", "13:00"),
                createInterval("17:00", "21:00")
            )
            "2.1", "2-1" -> listOf(
                createInterval("03:00", "07:00"),
                createInterval("11:00", "15:00"),
                createInterval("19:00", "23:00")
            )
            "2.2", "2-2" -> listOf(
                createInterval("04:00", "08:00"),
                createInterval("12:00", "16:00"),
                createInterval("20:00", "24:00")
            )
            "3.1", "3-1" -> listOf(
                createInterval("05:00", "09:00"),
                createInterval("13:00", "17:00"),
                createInterval("21:00", "24:00")
            )
            "3.2", "3-2" -> listOf(
                createInterval("06:00", "10:00"),
                createInterval("14:00", "18:00"),
                createInterval("22:00", "24:00")
            )
            "5.1", "5-1" -> listOf(
                createInterval("01:00", "04:00"),
                createInterval("09:00", "13:00"),
                createInterval("17:00", "21:00")
            )
            "5.2", "5-2" -> listOf(
                createInterval("02:00", "06:00"),
                createInterval("10:00", "14:00"),
                createInterval("18:00", "22:00")
            )
            "6.1", "6-1" -> listOf(
                createInterval("03:00", "07:00"),
                createInterval("11:00", "15:00"),
                createInterval("19:00", "23:00")
            )
            "6.2", "6-2" -> listOf(
                createInterval("04:00", "08:00"),
                createInterval("12:00", "16:00"),
                createInterval("20:00", "24:00")
            )
            else -> listOf(
                createInterval("08:00", "12:00"),
                createInterval("16:00", "20:00")
            )
        }
    }

    fun createInterval(startStr: String, endStr: String): OutageInterval {
        val sp = startStr.split(":")
        val ep = endStr.split(":")
        val sh = sp[0].toInt()
        val sm = sp[1].toInt()
        val eh = ep[0].toInt()
        val em = ep[1].toInt()
        val sMin = sh * 60 + sm
        var eMin = eh * 60 + em
        if (eMin == 0 && (eh == 0 || eh == 24)) {
            eMin = 1440
        }
        return OutageInterval(
            startMin = sMin,
            endMin = eMin,
            startStr = startStr,
            endStr = endStr
        )
    }
}
