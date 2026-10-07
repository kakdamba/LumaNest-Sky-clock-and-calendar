package com.lumanest.eventengine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class SkyEventEngineTest {

    @Test
    fun testGeyserOccurrencesAtEvenHour() {
        // Oct 6, 2026 at 02:00:00 PT (Even hour)
        val testTimePt = ZonedDateTime.of(2026, 10, 6, 2, 0, 0, 0, ZoneId.of("America/Los_Angeles"))
        val testEpoch = testTimePt.toInstant().toEpochMilli()

        val dailyEvents = SkyEventEngine.calculateDailyOccurrences(testEpoch)
        val geyser = dailyEvents.firstOrNull { it.event.id == "daily_geyser" }

        assertNotNull(geyser)
        // Geyser starts at 02:05 PT -> 5 minutes (300,000 ms) away
        assertEquals(300_000L, geyser!!.millisUntilStart)
        assertFalse(geyser.isActiveNow)
    }

    @Test
    fun testGeyserActiveWindow() {
        // Oct 6, 2026 at 02:07:00 PT (Active window 02:05 - 02:15)
        val testTimePt = ZonedDateTime.of(2026, 10, 6, 2, 7, 0, 0, ZoneId.of("America/Los_Angeles"))
        val testEpoch = testTimePt.toInstant().toEpochMilli()

        val dailyEvents = SkyEventEngine.calculateDailyOccurrences(testEpoch)
        val geyser = dailyEvents.firstOrNull { it.event.id == "daily_geyser" }

        assertNotNull(geyser)
        assertTrue(geyser!!.isActiveNow)
        assertEquals(0L, geyser.millisUntilStart)
        // Remaining time: 8 minutes (480,000 ms)
        assertEquals(480_000L, geyser.millisRemaining)
    }

    @Test
    fun testGrandmaAndTurtleOrder() {
        // Oct 6, 2026 at 02:00:00 PT
        val testTimePt = ZonedDateTime.of(2026, 10, 6, 2, 0, 0, 0, ZoneId.of("America/Los_Angeles"))
        val testEpoch = testTimePt.toInstant().toEpochMilli()

        val dailyEvents = SkyEventEngine.calculateDailyOccurrences(testEpoch)
        val grandma = dailyEvents.firstOrNull { it.event.id == "daily_grandma" }
        val turtle = dailyEvents.firstOrNull { it.event.id == "daily_turtle" }

        assertNotNull(grandma)
        assertNotNull(turtle)
        // Grandma at 02:35 -> 35 mins (2,100,000 ms)
        assertEquals(2_100_000L, grandma!!.millisUntilStart)
        // Turtle at 02:50 -> 50 mins (3,000,000 ms)
        assertEquals(3_000_000L, turtle!!.millisUntilStart)
    }

    @Test
    fun testShardCalculationWithPlutoyDevRules() {
        // Oct 6, 2026 is Tuesday. dayOfMonth=6 -> 6 % 5 = pattern 1 (Black Shard B)
        // Black Shard B has no shards on Sun(7), Mon(1). Tuesday is active!
        val testTime = ZonedDateTime.of(2026, 10, 6, 10, 0, 0, 0, ZoneId.of("America/Los_Angeles"))
        val occurrences = SkyEventEngine.calculateDailyOccurrences(testTime.toInstant().toEpochMilli())
        val shard = occurrences.firstOrNull { it.event.category == com.lumanest.models.EventCategory.SHARD_ERUPTION }

        assertNotNull(shard)
        assertTrue(shard!!.event.name.contains("Shard"))
        assertTrue(shard.event.description.contains("Rewards"))
    }
}
