package com.lumanest.models

import kotlinx.serialization.Serializable

@Serializable
enum class EventCategory {
    DAILY_RECURRING,
    SEASON,
    TRAVELING_SPIRIT,
    RETURNING_SPIRIT,
    MAJOR_EVENT,
    DOUBLE_CURRENCY,
    SHARD_ERUPTION,
    MAINTENANCE,
    COMMUNITY
}

@Serializable
enum class VerificationStatus {
    CONFIRMED_TGC,
    COMMUNITY_VERIFIED,
    PREDICTED,
    POSTPONED,
    CANCELLED
}

@Serializable
data class SkyEvent(
    val id: String,
    val name: String,
    val description: String = "",
    val category: EventCategory,
    // ISO-8601 strings (e.g. 2026-10-23T00:00:00Z) or recurrence patterns
    val startIso: String,
    val endIso: String,
    val timezone: String = "America/Los_Angeles", // Default Sky reset timezone (PT)
    val status: VerificationStatus = VerificationStatus.CONFIRMED_TGC,
    val source: String = "Thatgamecompany",
    val sourceUrl: String = "https://www.thatskygame.com/",
    val lastVerified: String = "2026-10-06",
    val isFavorite: Boolean = false,
    val recurrencePattern: String? = null // e.g. "DAILY_ODD_HOURS_05" for Geyser
)

@Serializable
data class EventOccurrence(
    val event: SkyEvent,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val isActiveNow: Boolean,
    val millisUntilStart: Long,
    val millisRemaining: Long
)

@Serializable
data class Friend(
    val id: String,
    val name: String,
    val timezoneId: String, // IANA identifier e.g. "Asia/Tokyo"
    val countryFlag: String = "🌍",
    val notes: String = "",
    val sortOrder: Int = 0
)

@Serializable
data class EventDatabaseMeta(
    val version: String, // e.g. "2026.10.06"
    val schemaVersion: Int = 1,
    val generatedAt: String,
    val events: List<SkyEvent>
)
