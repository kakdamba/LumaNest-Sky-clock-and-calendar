package com.lumanest.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lumanest.models.EventCategory
import com.lumanest.models.SkyEvent
import com.lumanest.models.VerificationStatus

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val category: String,
    val startIso: String,
    val endIso: String,
    val timezone: String,
    val status: String,
    val source: String,
    val sourceUrl: String,
    val lastVerified: String,
    val isFavorite: Boolean,
    val recurrencePattern: String?
) {
    fun toModel(): SkyEvent = SkyEvent(
        id = id,
        name = name,
        description = description,
        category = try { EventCategory.valueOf(category) } catch (_: Exception) { EventCategory.MAJOR_EVENT },
        startIso = startIso,
        endIso = endIso,
        timezone = timezone,
        status = try { VerificationStatus.valueOf(status) } catch (_: Exception) { VerificationStatus.COMMUNITY_VERIFIED },
        source = source,
        sourceUrl = sourceUrl,
        lastVerified = lastVerified,
        isFavorite = isFavorite,
        recurrencePattern = recurrencePattern
    )

    companion object {
        fun fromModel(model: SkyEvent): EventEntity = EventEntity(
            id = model.id,
            name = model.name,
            description = model.description,
            category = model.category.name,
            startIso = model.startIso,
            endIso = model.endIso,
            timezone = model.timezone,
            status = model.status.name,
            source = model.source,
            sourceUrl = model.sourceUrl,
            lastVerified = model.lastVerified,
            isFavorite = model.isFavorite,
            recurrencePattern = model.recurrencePattern
        )
    }
}

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val id: String,
    val name: String,
    val timezoneId: String,
    val countryFlag: String,
    val notes: String,
    val sortOrder: Int = 0
)
