package com.example.data

import com.example.model.OutageInterval

object PowerScheduleParser {

    private val INTERVAL_REGEX = Regex(
        """(\d{1,2}:\d{2})\s*(?:-|—|–|до)\s*(\d{1,2}:\d{2})""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Parses raw text or HTML and separates into (PrimarySchedule, SecondarySchedule)
     * preventing overlapping outages from merging into a single corrupted schedule.
     */
    fun parseSchedulesFromText(rawText: String): Pair<List<OutageInterval>, List<OutageInterval>> {
        // 1. First check if explicit section headers exist (e.g. "запасний", "можливі", "графік 2", "завтра")
        val lower = rawText.lowercase()
        val splitIndex = listOf(
            lower.indexOf("запасн"),
            lower.indexOf("можлив"),
            lower.indexOf("графік 2"),
            lower.indexOf("завтра")
        ).filter { it > 0 }.minOrNull()

        if (splitIndex != null) {
            val part1 = rawText.substring(0, splitIndex)
            val part2 = rawText.substring(splitIndex)
            val list1 = parseSingleSchedule(part1)
            val list2 = parseSingleSchedule(part2)
            if (list1.isNotEmpty() && list2.isNotEmpty()) {
                return Pair(list1, list2)
            }
        }

        // 2. Parse all intervals and partition them if any overlap occurs
        val allIntervals = parseSingleSchedule(rawText)
        return partitionNonOverlapping(allIntervals)
    }

    /**
     * Parses all intervals from a block of text.
     */
    fun parseSingleSchedule(text: String): List<OutageInterval> {
        val cleanText = stripHtmlTags(text)
        val matches = INTERVAL_REGEX.findAll(cleanText).toList()
        if (matches.isEmpty()) return emptyList()

        val list = mutableListOf<OutageInterval>()
        for (match in matches) {
            val startStr = match.groupValues[1].trim()
            val endStr = match.groupValues[2].trim()

            val sp = startStr.split(":")
            val ep = endStr.split(":")
            if (sp.size == 2 && ep.size == 2) {
                val sh = sp[0].toIntOrNull() ?: continue
                val sm = sp[1].toIntOrNull() ?: continue
                val eh = ep[0].toIntOrNull() ?: continue
                val em = ep[1].toIntOrNull() ?: continue

                if (sh in 0..23 && sm in 0..59 && eh in 0..24 && em in 0..59) {
                    val sMin = sh * 60 + sm
                    var eMin = eh * 60 + em

                    if (eMin == 0 && (eh == 0 || eh == 24)) {
                        eMin = 1440
                    }

                    if (eMin > sMin) {
                        list.add(
                            OutageInterval(
                                startMin = sMin,
                                endMin = eMin,
                                startStr = formatTime(sh, sm),
                                endStr = if (eMin == 1440) "24:00" else formatTime(eh, em)
                            )
                        )
                    }
                }
            }
        }

        return list.distinctBy { "${it.startMin}-${it.endMin}" }
    }

    /**
     * Splits intervals into primary and secondary lists if overlaps occur.
     */
    fun partitionNonOverlapping(intervals: List<OutageInterval>): Pair<List<OutageInterval>, List<OutageInterval>> {
        if (intervals.size <= 1) return Pair(intervals, emptyList())

        val sorted = intervals.sortedWith(compareBy({ it.startMin }, { it.endMin }))
        val primary = mutableListOf<OutageInterval>()
        val secondary = mutableListOf<OutageInterval>()

        for (interval in sorted) {
            val overlapsPrimary = primary.any { existing ->
                interval.startMin < existing.endMin && existing.startMin < interval.endMin
            }

            if (!overlapsPrimary) {
                primary.add(interval)
            } else {
                secondary.add(interval)
            }
        }

        return Pair(primary.sortedBy { it.startMin }, secondary.sortedBy { it.startMin })
    }

    private fun formatTime(hour: Int, min: Int): String {
        return String.format("%02d:%02d", hour, min)
    }

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
     * Default realistic fallback schedules for all 12 Ukrainian queues (1.1 - 6.2).
     */
    fun getPresetScheduleForQueue(queueCode: String): Pair<List<OutageInterval>, List<OutageInterval>> {
        val normalized = queueCode.replace("-", ".").trim()
        val primary = when (normalized) {
            "4.2" -> listOf(
                createInterval("06:30", "08:00"),
                createInterval("12:30", "15:00"),
                createInterval("18:30", "21:30")
            )
            "4.1" -> listOf(
                createInterval("05:30", "07:30"),
                createInterval("11:30", "14:00"),
                createInterval("17:30", "20:30")
            )
            "1.1" -> listOf(
                createInterval("00:00", "03:00"),
                createInterval("07:00", "10:30"),
                createInterval("15:00", "18:30"),
                createInterval("22:30", "24:00")
            )
            "1.2" -> listOf(
                createInterval("01:00", "04:30"),
                createInterval("08:30", "12:00"),
                createInterval("16:00", "19:30")
            )
            "2.1" -> listOf(
                createInterval("02:30", "06:00"),
                createInterval("10:00", "13:30"),
                createInterval("17:30", "21:00")
            )
            "2.2" -> listOf(
                createInterval("03:30", "07:00"),
                createInterval("11:00", "14:30"),
                createInterval("18:30", "22:00")
            )
            "3.1" -> listOf(
                createInterval("04:30", "08:00"),
                createInterval("12:00", "15:30"),
                createInterval("19:30", "23:00")
            )
            "3.2" -> listOf(
                createInterval("05:00", "08:30"),
                createInterval("12:30", "16:00"),
                createInterval("20:00", "23:30")
            )
            "5.1" -> listOf(
                createInterval("01:30", "05:00"),
                createInterval("09:00", "12:30"),
                createInterval("16:30", "20:00")
            )
            "5.2" -> listOf(
                createInterval("02:00", "05:30"),
                createInterval("09:30", "13:00"),
                createInterval("17:00", "20:30")
            )
            "6.1" -> listOf(
                createInterval("03:00", "06:30"),
                createInterval("10:30", "14:00"),
                createInterval("18:00", "21:30")
            )
            "6.2" -> listOf(
                createInterval("04:00", "07:30"),
                createInterval("11:30", "15:00"),
                createInterval("19:00", "22:30")
            )
            else -> listOf(
                createInterval("08:00", "12:00"),
                createInterval("16:00", "20:00")
            )
        }

        val secondary = when (normalized) {
            "4.2" -> listOf(
                createInterval("07:30", "09:30"),
                createInterval("13:30", "16:00"),
                createInterval("19:30", "21:00")
            )
            "4.1" -> listOf(
                createInterval("06:30", "08:30"),
                createInterval("12:30", "15:00"),
                createInterval("18:30", "20:00")
            )
            else -> emptyList()
        }

        return Pair(primary, secondary)
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
