package com.example.data

import com.example.model.OutageInterval
import com.example.model.ScheduleData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class EnergyUaPayload(
    val today: List<EnergyUaSlot> = emptyList(),
    val tomorrow: List<EnergyUaSlot> = emptyList(),
    val tomorrowAvailable: Boolean = false,
    val currentTime: Long = 0L,
    val dates: EnergyUaDates? = null,
    val nextEventTimestamp: Long = 0L,
    val isCurrentlyOff: Boolean = false,
    val isEmergency: Boolean = false
)

data class EnergyUaSlot(
    val from: Int = 0,
    val to: Int = 0,
    val status: String = ""
)

data class EnergyUaDates(
    val today: String = "",
    val tomorrow: String = ""
)

object PowerScheduleParser {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val payloadAdapter = moshi.adapter(EnergyUaPayload::class.java)
    private val scheduleDataAdapter = moshi.adapter(ScheduleData::class.java)

    private val PAYLOAD_SCRIPT_REGEX = Regex(
        """<script\b[^>]*\bid\s*=\s*["']schedulePayload["'][^>]*>([\s\S]*?)</script>""",
        RegexOption.IGNORE_CASE
    )

    private val ANY_SCRIPT_REGEX = Regex(
        """<script\b[^>]*>([\s\S]*?)</script>""",
        RegexOption.IGNORE_CASE
    )

    private val INTERVAL_REGEX = Regex(
        """(\d{1,2}:\d{2})\s*(?:-|—|–|до)\s*(\d{1,2}:\d{2})""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Parses HTML from energy-ua.info/cherga/<slug>.
     * Prioritizes the new <script id="schedulePayload"> JSON structure,
     * falling back to regex text extraction if legacy HTML is encountered.
     */
    fun parseHtml(queueCode: String, html: String): ScheduleData? {
        val payloadJson = extractSchedulePayloadJson(html)
        if (payloadJson != null) {
            parsePayloadJson(queueCode, payloadJson)?.let { return it }
        }

        // Fallback to legacy text-based parsing if schedulePayload is absent
        val (primary, secondary) = parseSchedulesFromText(html)
        if (primary.isNotEmpty() || secondary.isNotEmpty()) {
            return ScheduleData(
                queueCode = queueCode,
                primaryIntervals = primary,
                secondaryIntervals = secondary,
                lastUpdated = System.currentTimeMillis(),
                isLiveFromNetwork = true,
                note = "Оновлено з energy-ua.info"
            )
        }

        return null
    }

    fun parsePayloadJson(queueCode: String, payloadJson: String): ScheduleData? {
        val payload = try {
            payloadAdapter.fromJson(payloadJson)
        } catch (_: Exception) {
            null
        } ?: return null

        val (todayPrimary, todaySecondary) = resolveSlots(payload.today)
        val (tomorrowPrimary, tomorrowSecondary) = if (payload.tomorrowAvailable || payload.tomorrow.isNotEmpty()) {
            resolveSlots(payload.tomorrow)
        } else {
            Pair(emptyList(), emptyList())
        }

        val statusNote = when {
            payload.isEmergency -> "⚠️ Екстрені відключення (energy-ua.info)"
            todayPrimary.isEmpty() && todaySecondary.isEmpty() -> "Світло є весь день (energy-ua.info)"
            else -> "Оновлено з energy-ua.info"
        }

        return ScheduleData(
            queueCode = queueCode,
            primaryIntervals = todayPrimary,
            secondaryIntervals = todaySecondary,
            tomorrowPrimaryIntervals = tomorrowPrimary,
            tomorrowSecondaryIntervals = tomorrowSecondary,
            todayDate = payload.dates?.today.orEmpty(),
            tomorrowDate = payload.dates?.tomorrow.orEmpty(),
            tomorrowAvailable = payload.tomorrowAvailable || tomorrowPrimary.isNotEmpty(),
            isEmergency = payload.isEmergency,
            lastUpdated = System.currentTimeMillis(),
            isLiveFromNetwork = true,
            note = statusNote
        )
    }

    fun extractSchedulePayloadJson(html: String): String? {
        val directMatch = PAYLOAD_SCRIPT_REGEX.find(html)?.groupValues?.getOrNull(1)?.trim()
        if (!directMatch.isNullOrEmpty() && directMatch.startsWith("{")) {
            return directMatch
        }

        // Secondary scan: look for inline JSON script containing "today" and "dates"
        for (match in ANY_SCRIPT_REGEX.findAll(html)) {
            val content = match.groupValues.getOrNull(1)?.trim() ?: continue
            if (content.startsWith("{") && content.contains("\"today\"") && content.contains("\"dates\"")) {
                return content
            }
        }
        return null
    }

    /**
     * Replicates schedule-live.js segment resolution:
     * Priority: red > yellow > green.
     * Returns Pair(guaranteedRedIntervals, combinedRedAndYellowIntervalsIfYellowExists).
     */
    fun resolveSlots(slots: List<EnergyUaSlot>): Pair<List<OutageInterval>, List<OutageInterval>> {
        if (slots.isEmpty()) return Pair(emptyList(), emptyList())

        val points = sortedSetOf(0, 1440)
        for (slot in slots) {
            points.add(slot.from.coerceIn(0, 1440))
            points.add(slot.to.coerceIn(0, 1440))
        }

        val sortedPoints = points.toList()
        val resolved = mutableListOf<Triple<Int, Int, String>>()

        for (i in 0 until sortedPoints.size - 1) {
            val start = sortedPoints[i]
            val end = sortedPoints[i + 1]
            if (end <= start) continue

            var status = "green"
            for (slot in slots) {
                val sFrom = slot.from.coerceIn(0, 1440)
                val sTo = slot.to.coerceIn(0, 1440)
                if (sFrom < end && sTo > start) {
                    val slotStatus = slot.status.lowercase()
                    if (slotStatus == "red") {
                        status = "red"
                        break
                    }
                    if (slotStatus == "yellow") {
                        status = "yellow"
                    }
                }
            }

            val last = resolved.lastOrNull()
            if (last != null && last.third == status && last.second == start) {
                resolved[resolved.lastIndex] = Triple(last.first, end, status)
            } else {
                resolved.add(Triple(start, end, status))
            }
        }

        val redIntervals = resolved
            .filter { it.third == "red" }
            .map { minutesToInterval(it.first, it.second) }

        val hasYellow = resolved.any { it.third == "yellow" }
        val secondaryIntervals = if (hasYellow) {
            val merged = mutableListOf<Pair<Int, Int>>()
            for ((start, end, status) in resolved) {
                if (status == "red" || status == "yellow") {
                    val last = merged.lastOrNull()
                    if (last != null && last.second == start) {
                        merged[merged.lastIndex] = Pair(last.first, end)
                    } else {
                        merged.add(Pair(start, end))
                    }
                }
            }
            merged.map { minutesToInterval(it.first, it.second) }
        } else {
            emptyList()
        }

        return Pair(redIntervals, secondaryIntervals)
    }

    fun minutesToInterval(startMin: Int, endMin: Int): OutageInterval {
        val clampedStart = startMin.coerceIn(0, 1439)
        val clampedEnd = endMin.coerceIn(clampedStart + 1, 1440)
        return OutageInterval(
            startMin = clampedStart,
            endMin = clampedEnd,
            startStr = minutesToClock(clampedStart),
            endStr = minutesToClock(clampedEnd)
        )
    }

    fun minutesToClock(minutes: Int): String {
        if (minutes >= 1440) return "24:00"
        val h = minutes / 60
        val m = minutes % 60
        return String.format("%02d:%02d", h, m)
    }

    fun serializeScheduleData(data: ScheduleData): String {
        return scheduleDataAdapter.toJson(data)
    }

    fun deserializeScheduleData(json: String): ScheduleData? {
        return try {
            scheduleDataAdapter.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses raw text or legacy HTML and separates into (PrimarySchedule, SecondarySchedule)
     * preventing overlapping outages from merging into a single corrupted schedule.
     */
    fun parseSchedulesFromText(rawText: String): Pair<List<OutageInterval>, List<OutageInterval>> {
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

        val allIntervals = parseSingleSchedule(rawText)
        return partitionNonOverlapping(allIntervals)
    }

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
}
