package com.lumanest.eventengine

import com.lumanest.models.EventCategory
import com.lumanest.models.EventOccurrence
import com.lumanest.models.SkyEvent
import com.lumanest.timezone.TimezoneHelper
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * SkyEventEngine is completely decoupled from UI.
 * Handles deterministic event intervals for daily events:
 * - Polluted Geyser
 * - Grandma's Dinner
 * - Sanctuary Turtle
 * - Red & Black Shard Eruptions (predictable rotating calendar cycles)
 * - Daily Reset
 */
object SkyEventEngine {

    /**
     * Standard Daily Sky Events based on Pacific Time (Sky standard time):
     * - Geyser: XX:05 PT (even hours, duration 10 min)
     * - Grandma: XX:35 PT (even hours, duration 10 min)
     * - Turtle: XX:50 PT (even hours, duration 10 min)
     * - Red Shard Eruptions: 3 daily windows on active days
     * - Daily Reset: 00:00 PT
     */
    fun calculateDailyOccurrences(nowEpochMillis: Long): List<EventOccurrence> {
        val nowPt = Instant.ofEpochMilli(nowEpochMillis).atZone(TimezoneHelper.SKY_RESET_ZONE)
        val occurrences = mutableListOf<EventOccurrence>()

        // 1. Geyser (every 2 hours at XX:05 PT for 10 mins)
        occurrences.add(calculateTwoHourRecurring(
            id = "daily_geyser",
            name = "Polluted Geyser",
            description = "Erupts dark wax every 2 hours in Sanctuary Islands.",
            minuteOffset = 5,
            durationMinutes = 10,
            nowPt = nowPt,
            nowEpoch = nowEpochMillis
        ))

        // 2. Grandma (every 2 hours at XX:35 PT for 10 mins)
        occurrences.add(calculateTwoHourRecurring(
            id = "daily_grandma",
            name = "Grandma's Dinner",
            description = "Belonging Elder bakes warm light buns in Hidden Forest.",
            minuteOffset = 35,
            durationMinutes = 10,
            nowPt = nowPt,
            nowEpoch = nowEpochMillis
        ))

        // 3. Turtle (every 2 hours at XX:50 PT for 10 mins)
        occurrences.add(calculateTwoHourRecurring(
            id = "daily_turtle",
            name = "Sanctuary Turtle",
            description = "Sunset swimming turtle shedding light shells in Sanctuary Islands.",
            minuteOffset = 50,
            durationMinutes = 10,
            nowPt = nowPt,
            nowEpoch = nowEpochMillis
        ))

        // 4. Shard Eruptions (Red Shards on odd days, Black Shards on even days)
        val shardOccurrence = calculateShardOccurrence(nowPt, nowEpochMillis)
        if (shardOccurrence != null) {
            occurrences.add(shardOccurrence)
        }

        // 5. Daily Reset (every 24 hours at 00:00 PT)
        occurrences.add(calculateDailyReset(nowPt, nowEpochMillis))

        // 6. Traveling Spirit Official Reveal (Tuesday 12:00 PM PT)
        occurrences.add(calculateTravelingSpiritReveal(nowPt, nowEpochMillis))

        return occurrences
    }

    private fun calculateTwoHourRecurring(
        id: String,
        name: String,
        description: String,
        minuteOffset: Int,
        durationMinutes: Int,
        nowPt: ZonedDateTime,
        nowEpoch: Long
    ): EventOccurrence {
        val baseHour = (nowPt.hour / 2) * 2
        var candidateStart = nowPt.truncatedTo(ChronoUnit.HOURS).withHour(baseHour).withMinute(minuteOffset).withSecond(0).withNano(0)
        var candidateEnd = candidateStart.plusMinutes(durationMinutes.toLong())

        if (candidateEnd.toInstant().toEpochMilli() <= nowEpoch) {
            candidateStart = candidateStart.plusHours(2)
            candidateEnd = candidateStart.plusMinutes(durationMinutes.toLong())
        }
        val prevStart = candidateStart.minusHours(2)
        val prevEnd = prevStart.plusMinutes(durationMinutes.toLong())
        if (nowEpoch in prevStart.toInstant().toEpochMilli()..prevEnd.toInstant().toEpochMilli()) {
            candidateStart = prevStart
            candidateEnd = prevEnd
        }

        val startMillis = candidateStart.toInstant().toEpochMilli()
        val endMillis = candidateEnd.toInstant().toEpochMilli()
        val isActive = nowEpoch in startMillis..endMillis
        val millisUntilStart = if (isActive) 0L else maxOf(0L, startMillis - nowEpoch)
        val millisRemaining = maxOf(0L, endMillis - nowEpoch)

        val event = SkyEvent(
            id = id,
            name = name,
            description = description,
            category = EventCategory.DAILY_RECURRING,
            startIso = candidateStart.toInstant().toString(),
            endIso = candidateEnd.toInstant().toString(),
            timezone = TimezoneHelper.SKY_RESET_ZONE.id,
            recurrencePattern = "EVERY_2_HOURS_AT_$minuteOffset"
        )

        return EventOccurrence(
            event = event,
            startEpochMillis = startMillis,
            endEpochMillis = endMillis,
            isActiveNow = isActive,
            millisUntilStart = millisUntilStart,
            millisRemaining = millisRemaining
        )
    }

    /**
     * Shard configuration model based on PlutoyDev reverse-engineered rules.
     */
    data class ShardPattern(
        val isRed: Boolean,
        val noShardWkDays: Set<Int>, // 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
        val intervalHours: Long,
        val startHour: Int,
        val startMinute: Int,
        val realmAreas: List<Pair<String, String>>, // Realm -> Specific Map Area
        val acReward: Double
    )

    private val SHARD_PATTERNS = listOf(
        // Pattern 0: Black Shard A (Sat & Sun no shards)
        ShardPattern(
            isRed = false,
            noShardWkDays = setOf(6, 7),
            intervalHours = 8,
            startHour = 1,
            startMinute = 50,
            realmAreas = listOf(
                "Daylight Prairie" to "Butterfly Field",
                "Hidden Forest" to "Forest Brook",
                "Valley of Triumph" to "Ice Rink",
                "Golden Wasteland" to "Broken Temple",
                "Vault of Knowledge" to "Starlight Desert"
            ),
            acReward = 0.0
        ),
        // Pattern 1: Black Shard B (Sun & Mon no shards)
        ShardPattern(
            isRed = false,
            noShardWkDays = setOf(7, 1),
            intervalHours = 8,
            startHour = 2,
            startMinute = 10,
            realmAreas = listOf(
                "Daylight Prairie" to "Village Islands",
                "Hidden Forest" to "Boneyard",
                "Valley of Triumph" to "Ice Rink",
                "Golden Wasteland" to "Battlefield",
                "Vault of Knowledge" to "Starlight Desert"
            ),
            acReward = 0.0
        ),
        // Pattern 2: Red Shard A (Mon & Tue no shards)
        ShardPattern(
            isRed = true,
            noShardWkDays = setOf(1, 2),
            intervalHours = 6,
            startHour = 7,
            startMinute = 40,
            realmAreas = listOf(
                "Daylight Prairie" to "Prairie Cave",
                "Hidden Forest" to "Forest Garden",
                "Valley of Triumph" to "Village of Dreams",
                "Golden Wasteland" to "Crabfield (Shipwreck)",
                "Vault of Knowledge" to "Jellyfish Cove"
            ),
            acReward = 2.5
        ),
        // Pattern 3: Red Shard B (Tue & Wed no shards)
        ShardPattern(
            isRed = true,
            noShardWkDays = setOf(2, 3),
            intervalHours = 6,
            startHour = 2,
            startMinute = 20,
            realmAreas = listOf(
                "Daylight Prairie" to "Bird Nest",
                "Hidden Forest" to "Treehouse",
                "Valley of Triumph" to "Hermit Valley",
                "Golden Wasteland" to "Forgotten Ark",
                "Vault of Knowledge" to "Jellyfish Cove"
            ),
            acReward = 2.0
        ),
        // Pattern 4: Red Shard C (Wed & Thu no shards)
        ShardPattern(
            isRed = true,
            noShardWkDays = setOf(3, 4),
            intervalHours = 6,
            startHour = 3,
            startMinute = 30,
            realmAreas = listOf(
                "Daylight Prairie" to "Sanctuary Island",
                "Hidden Forest" to "Elevated Clearing",
                "Valley of Triumph" to "Hermit Valley",
                "Golden Wasteland" to "Graveyard",
                "Vault of Knowledge" to "Jellyfish Cove"
            ),
            acReward = 3.5
        )
    )

    /**
     * Resolves the active or next upcoming Shard occurrence adhering to PlutoyDev's
     * rotation pattern, landing offset (+8m 40s), 4-hour duration, and realm mappings.
     */
    private fun calculateShardOccurrence(nowPt: ZonedDateTime, nowEpoch: Long): EventOccurrence? {
        val dayOfMonth = nowPt.dayOfMonth
        val patternIndex = Math.floorMod(dayOfMonth, SHARD_PATTERNS.size)
        val pattern = SHARD_PATTERNS[patternIndex]

        val dayOfWeek = nowPt.dayOfWeek.value // 1 (Mon) .. 7 (Sun)
        val dayStart = nowPt.truncatedTo(ChronoUnit.DAYS)
        val realmIndex = Math.floorMod(dayOfMonth, pattern.realmAreas.size)
        val (realmName, mapName) = pattern.realmAreas[realmIndex]

        val landingOffsetMillis = 8 * 60_000L + 40_000L // 8m 40s
        val durationMillis = 4 * 60 * 60_000L // 4 hours

        // Check if today has no shards for this pattern
        val isNoShardToday = pattern.noShardWkDays.contains(dayOfWeek)

        if (!isNoShardToday) {
            val occurrencesCount = if (pattern.intervalHours == 8L) 3 else 4
            for (i in 0 until occurrencesCount) {
                val gateStart = dayStart.plusHours(pattern.startHour + (i * pattern.intervalHours))
                    .plusMinutes(pattern.startMinute.toLong())
                val gateStartMillis = gateStart.toInstant().toEpochMilli()
                val landMillis = gateStartMillis + landingOffsetMillis
                val endMillis = gateStartMillis + durationMillis

                if (nowEpoch <= endMillis) {
                    val isActive = nowEpoch in landMillis..endMillis
                    val untilStart = if (isActive) 0L else maxOf(0L, landMillis - nowEpoch)
                    val remaining = maxOf(0L, endMillis - nowEpoch)

                    val event = SkyEvent(
                        id = if (pattern.isRed) "daily_shard_red" else "daily_shard_black",
                        name = if (pattern.isRed) "Red Shard • $realmName" else "Black Shard • $realmName",
                        description = if (pattern.isRed) {
                            "Strong eruption at $mapName. Rewards ~${pattern.acReward} Ascended Candles ✦."
                        } else {
                            "Regular eruption at $mapName. Rewards standard candle wax."
                        },
                        category = EventCategory.SHARD_ERUPTION,
                        startIso = gateStart.toInstant().toString(),
                        endIso = gateStart.plusHours(4).toInstant().toString(),
                        timezone = TimezoneHelper.SKY_RESET_ZONE.id
                    )

                    return EventOccurrence(
                        event = event,
                        startEpochMillis = landMillis,
                        endEpochMillis = endMillis,
                        isActiveNow = isActive,
                        millisUntilStart = untilStart,
                        millisRemaining = remaining
                    )
                }
            }
        }

        // If today has no shards or all windows today passed, find next active shard day
        for (dayOffset in 1..7) {
            val targetDay = dayStart.plusDays(dayOffset.toLong())
            val targetDayOfMonth = targetDay.dayOfMonth
            val targetPattern = SHARD_PATTERNS[Math.floorMod(targetDayOfMonth, SHARD_PATTERNS.size)]
            val targetDayOfWeek = targetDay.dayOfWeek.value

            if (!targetPattern.noShardWkDays.contains(targetDayOfWeek)) {
                val targetRealmIndex = Math.floorMod(targetDayOfMonth, targetPattern.realmAreas.size)
                val (tRealm, tMap) = targetPattern.realmAreas[targetRealmIndex]
                val gateStart = targetDay.plusHours(targetPattern.startHour.toLong())
                    .plusMinutes(targetPattern.startMinute.toLong())
                val gateStartMillis = gateStart.toInstant().toEpochMilli()
                val landMillis = gateStartMillis + landingOffsetMillis
                val endMillis = gateStartMillis + durationMillis

                val event = SkyEvent(
                    id = if (targetPattern.isRed) "daily_shard_red" else "daily_shard_black",
                    name = if (targetPattern.isRed) "Red Shard • $tRealm" else "Black Shard • $tRealm",
                    description = if (targetPattern.isRed) {
                        "Upcoming strong eruption at $tMap. Rewards ~${targetPattern.acReward} Ascended Candles ✦."
                    } else {
                        "Upcoming regular eruption at $tMap. Rewards standard candle wax."
                    },
                    category = EventCategory.SHARD_ERUPTION,
                    startIso = gateStart.toInstant().toString(),
                    endIso = gateStart.plusHours(4).toInstant().toString(),
                    timezone = TimezoneHelper.SKY_RESET_ZONE.id
                )

                return EventOccurrence(
                    event = event,
                    startEpochMillis = landMillis,
                    endEpochMillis = endMillis,
                    isActiveNow = false,
                    millisUntilStart = maxOf(0L, landMillis - nowEpoch),
                    millisRemaining = maxOf(0L, endMillis - nowEpoch)
                )
            }
        }

        return null
    }

    /**
     * Projects upcoming Shard Eruptions for the next [daysAhead] days for the Calendar screen.
     */
    fun calculateUpcomingShards(nowEpoch: Long, daysAhead: Int = 7): List<SkyEvent> {
        val nowPt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowEpoch), TimezoneHelper.SKY_RESET_ZONE)
        val dayStart = nowPt.truncatedTo(ChronoUnit.DAYS)
        val landingOffsetMillis = 8 * 60_000L + 40_000L
        val durationMillis = 4 * 60 * 60_000L
        val result = mutableListOf<SkyEvent>()

        for (dayOffset in 0 until daysAhead) {
            val targetDay = dayStart.plusDays(dayOffset.toLong())
            val targetDayOfMonth = targetDay.dayOfMonth
            val patternIndex = Math.floorMod(targetDayOfMonth, SHARD_PATTERNS.size)
            val pattern = SHARD_PATTERNS[patternIndex]
            val dayOfWeek = targetDay.dayOfWeek.value

            if (!pattern.noShardWkDays.contains(dayOfWeek)) {
                val realmIndex = Math.floorMod(targetDayOfMonth, pattern.realmAreas.size)
                val (realmName, mapName) = pattern.realmAreas[realmIndex]
                val occurrencesCount = if (pattern.intervalHours == 8L) 3 else 4

                for (i in 0 until occurrencesCount) {
                    val gateStart = targetDay.plusHours(pattern.startHour + (i * pattern.intervalHours))
                        .plusMinutes(pattern.startMinute.toLong())
                    val endInstant = gateStart.plusHours(4)
                    val endMillis = gateStart.toInstant().toEpochMilli() + durationMillis

                    // Only include active or upcoming shards
                    if (endMillis >= nowEpoch) {
                        result.add(
                            SkyEvent(
                                id = "shard_${targetDay.toLocalDate()}_$i",
                                name = if (pattern.isRed) "Red Shard • $realmName" else "Black Shard • $realmName",
                                description = if (pattern.isRed) {
                                    "Strong eruption at $mapName. Rewards ~${pattern.acReward} Ascended Candles ✦."
                                } else {
                                    "Regular eruption at $mapName. Rewards standard candle wax."
                                },
                                category = EventCategory.SHARD_ERUPTION,
                                startIso = gateStart.toInstant().toString(),
                                endIso = endInstant.toInstant().toString(),
                                timezone = TimezoneHelper.SKY_RESET_ZONE.id
                            )
                        )
                    }
                }
            }
        }
        return result
    }

    private fun calculateDailyReset(nowPt: ZonedDateTime, nowEpoch: Long): EventOccurrence {
        val todayReset = nowPt.truncatedTo(ChronoUnit.DAYS)
        var nextReset = todayReset.plusDays(1)
        if (nowPt.isBefore(todayReset)) {
            nextReset = todayReset
        }
        val startMillis = nextReset.toInstant().toEpochMilli()
        val endMillis = startMillis + 60_000

        val event = SkyEvent(
            id = "daily_reset",
            name = "Sky Daily Reset",
            description = "Daily quest refresh and candle wax reset.",
            category = EventCategory.DAILY_RECURRING,
            startIso = nextReset.toInstant().toString(),
            endIso = nextReset.plusMinutes(1).toInstant().toString(),
            timezone = TimezoneHelper.SKY_RESET_ZONE.id
        )

        return EventOccurrence(
            event = event,
            startEpochMillis = startMillis,
            endEpochMillis = endMillis,
            isActiveNow = (nowEpoch in startMillis..endMillis),
            millisUntilStart = maxOf(0L, startMillis - nowEpoch),
            millisRemaining = maxOf(0L, endMillis - nowEpoch)
        )
    }

    /**
     * Calculates Traveling Spirit official identity reveal timing:
     * Officially revealed every Tuesday (12:00 PM PT, 12 hours after daily reset)
     * prior to Thursday in-game arrival.
     */
    private fun calculateTravelingSpiritReveal(nowPt: ZonedDateTime, nowEpoch: Long): EventOccurrence {
        // Target: Tuesday at 11:30 AM - 12:00 PM PT (TGC official social post announcement)
        var candidate = nowPt.with(java.time.DayOfWeek.TUESDAY)
            .truncatedTo(ChronoUnit.DAYS)
            .withHour(11)
            .withMinute(30)
            .withSecond(0)
            .withNano(0)

        // Active reveal announcement window: 5 minutes max!
        val candidateEnd = candidate.plusMinutes(5)
        if (candidateEnd.toInstant().toEpochMilli() <= nowEpoch) {
            candidate = candidate.plusWeeks(2)
        }

        val startMillis = candidate.toInstant().toEpochMilli()
        val endMillis = candidate.plusMinutes(5).toInstant().toEpochMilli()
        val isActive = nowEpoch in startMillis..endMillis

        val event = SkyEvent(
            id = "daily_ts_reveal",
            name = "Traveling Spirit Reveal",
            description = "TGC officially reveals the Traveling Spirit identity & cosmetics.",
            category = EventCategory.TRAVELING_SPIRIT,
            startIso = candidate.toInstant().toString(),
            endIso = candidate.plusMinutes(5).toInstant().toString(),
            timezone = TimezoneHelper.SKY_RESET_ZONE.id,
            recurrencePattern = "WEEKLY_TUESDAY_1130AM_PT"
        )

        return EventOccurrence(
            event = event,
            startEpochMillis = startMillis,
            endEpochMillis = endMillis,
            isActiveNow = isActive,
            millisUntilStart = if (isActive) 0L else maxOf(0L, startMillis - nowEpoch),
            millisRemaining = maxOf(0L, endMillis - nowEpoch)
        )
    }

    fun resolveCalendarOccurrences(events: List<SkyEvent>, nowEpochMillis: Long): List<EventOccurrence> {
        return events.mapNotNull { event ->
            try {
                val startInstant = Instant.parse(event.startIso)
                val endInstant = Instant.parse(event.endIso)
                val startMillis = startInstant.toEpochMilli()
                val endMillis = endInstant.toEpochMilli()

                val isActive = nowEpochMillis in startMillis..endMillis
                val isFuture = nowEpochMillis < startMillis
                if (!isActive && !isFuture) {
                    null
                } else {
                    EventOccurrence(
                        event = event,
                        startEpochMillis = startMillis,
                        endEpochMillis = endMillis,
                        isActiveNow = isActive,
                        millisUntilStart = if (isActive) 0L else (startMillis - nowEpochMillis),
                        millisRemaining = maxOf(0L, endMillis - nowEpochMillis)
                    )
                }
            } catch (_: Exception) {
                null
            }
        }.sortedBy { if (it.isActiveNow) 0L else it.millisUntilStart }
    }

    fun getPrimaryFeaturedOccurrence(allOccurrences: List<EventOccurrence>): EventOccurrence? {
        val active = allOccurrences.firstOrNull { it.isActiveNow }
        if (active != null) return active
        return allOccurrences.minByOrNull { it.millisUntilStart }
    }
}
