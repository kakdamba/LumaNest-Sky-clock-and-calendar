package com.lumanest.data.repository

import android.content.Context
import com.lumanest.data.local.EventDao
import com.lumanest.data.local.EventEntity
import com.lumanest.data.local.FriendDao
import com.lumanest.data.local.FriendEntity
import com.lumanest.models.EventDatabaseMeta
import com.lumanest.models.Friend
import com.lumanest.models.SkyEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class EventRepository(
    private val context: Context,
    private val eventDao: EventDao,
    private val friendDao: FriendDao
) {
    private val json = Json { ignoreUnknownKeys = true }

    val allEvents: Flow<List<SkyEvent>> = eventDao.getAllEvents().map { list ->
        list.map { it.toModel() }
    }

    val favoriteEvents: Flow<List<SkyEvent>> = eventDao.getFavoriteEvents().map { list ->
        list.map { it.toModel() }
    }

    val allFriends: Flow<List<Friend>> = friendDao.getAllFriends().map { list ->
        list.map {
            Friend(
                id = it.id,
                name = it.name,
                timezoneId = it.timezoneId,
                countryFlag = it.countryFlag,
                notes = it.notes,
                sortOrder = it.sortOrder
            )
        }
    }

    companion object {
        private const val REMOTE_EVENTS_URL_MAIN =
            "https://raw.githubusercontent.com/kakdamba/LumaNest-Sky-clock-and-calendar/main/app/src/main/assets/events_seed.json"
        private const val REMOTE_EVENTS_URL_MASTER =
            "https://raw.githubusercontent.com/kakdamba/LumaNest-Sky-clock-and-calendar/master/app/src/main/assets/events_seed.json"
    }

    suspend fun initializeSeedDataIfNeeded() = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("events_seed.json").bufferedReader().use { it.readText() }
            val meta = json.decodeFromString<EventDatabaseMeta>(jsonString)
            val entities = meta.events.map { EventEntity.fromModel(it) }
            eventDao.clearAll()
            eventDao.insertEvents(entities)
            initializeFriendsIfNeeded()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * DEC-002: Over-The-Air (OTA) remote event sync.
     * Silently checks GitHub raw events_seed.json on app open / resume.
     * Updates local Room DB if online while preserving user's favorite selections.
     * Offline-first (DEC-001): Never crashes or delays app if offline.
     */
    suspend fun syncRemoteEvents(): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = fetchRemoteJson(REMOTE_EVENTS_URL_MAIN)
                ?: fetchRemoteJson(REMOTE_EVENTS_URL_MASTER)
                ?: return@withContext false

            val meta = json.decodeFromString<EventDatabaseMeta>(jsonString)
            if (meta.events.isEmpty()) return@withContext false

            // Preserve user's local favorites
            val currentEvents = eventDao.getAllEventsSync()
            val favoriteIds = currentEvents.filter { it.isFavorite }.map { it.id }.toSet()

            // Filter out any unverified double currency events
            val filteredEvents = meta.events.filter {
                !it.id.contains("double_candles", ignoreCase = true) &&
                !it.name.contains("Double Treasure", ignoreCase = true)
            }

            val updatedEntities = filteredEvents.map { event ->
                val entity = EventEntity.fromModel(event)
                if (favoriteIds.contains(entity.id)) {
                    entity.copy(isFavorite = true)
                } else {
                    entity
                }
            }

            eventDao.clearAll()
            eventDao.insertEvents(updatedEntities)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun fetchRemoteJson(urlString: String): String? {
        return try {
            val url = java.net.URL(urlString)
            val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                useCaches = false
            }
            if (connection.responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun initializeFriendsIfNeeded() = withContext(Dispatchers.IO) {
        val currentFriends = friendDao.getAllFriendsSync()
        val userZone = com.lumanest.timezone.TimezoneHelper.getDeviceZoneId()
        val userFlag = com.lumanest.timezone.TimezoneHelper.WORLD_TIMEZONES.firstOrNull { it.id == userZone.id }?.flag ?: "🌍"

        if (currentFriends.none { it.id == "user_self_card" }) {
            // Only seed the user's own local device card — ZERO friend entries are seeded.
            val selfCard = FriendEntity(
                id = "user_self_card",
                name = "You",
                timezoneId = userZone.id,
                countryFlag = userFlag,
                notes = "Local Device Time",
                sortOrder = 0
            )
            friendDao.insertAllFriends(listOf(selfCard))
        }
    }

    suspend fun toggleFavorite(eventId: String, currentFavorite: Boolean) {
        eventDao.setFavorite(eventId, !currentFavorite)
    }

    suspend fun addFriend(name: String, timezoneId: String, flag: String = "🌍") {
        val friend = FriendEntity(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            timezoneId = timezoneId,
            countryFlag = flag,
            notes = "",
            sortOrder = (System.currentTimeMillis() / 1000).toInt()
        )
        friendDao.insertFriend(friend)
    }

    suspend fun updateFriendsOrder(reordered: List<Friend>) = withContext(Dispatchers.IO) {
        val updatedEntities = reordered.mapIndexed { index, friend ->
            FriendEntity(
                id = friend.id,
                name = friend.name,
                timezoneId = friend.timezoneId,
                countryFlag = friend.countryFlag,
                notes = friend.notes,
                sortOrder = index
            )
        }
        friendDao.insertAllFriends(updatedEntities)
    }

    suspend fun updateFriend(id: String, name: String, timezoneId: String, flag: String) = withContext(Dispatchers.IO) {
        val existing = friendDao.getAllFriendsSync().firstOrNull { it.id == id }
        val order = existing?.sortOrder ?: 0
        val entity = FriendEntity(
            id = id,
            name = name,
            timezoneId = timezoneId,
            countryFlag = flag,
            notes = existing?.notes ?: "",
            sortOrder = order
        )
        friendDao.insertFriend(entity)
    }

    suspend fun removeFriend(friendId: String) {
        friendDao.deleteFriend(friendId)
    }
}
