package com.lumanest.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lumanest_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SKY_RUNNING_ENABLED = "sky_running_mode_enabled"
        private const val KEY_SKY_RUNNING_SECONDS = "sky_running_countdown_seconds"
        private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
        private const val KEY_QUIET_START_HOUR = "quiet_start_hour"
        private const val KEY_QUIET_START_MINUTE = "quiet_start_minute"
        private const val KEY_QUIET_END_HOUR = "quiet_end_hour"
        private const val KEY_QUIET_END_MINUTE = "quiet_end_minute"

        // Home schedule event filters
        private const val KEY_FILTER_GEYSER = "filter_geyser"
        private const val KEY_FILTER_GRANDMA = "filter_grandma"
        private const val KEY_FILTER_TURTLE = "filter_turtle"
        private const val KEY_FILTER_SHARDS = "filter_shards"
        private const val KEY_FILTER_RESET = "filter_reset"
        private const val KEY_FILTER_TS_REVEAL = "filter_ts_reveal"
        private const val KEY_OVERLAY_OFFSET_Y = "overlay_offset_y"

        // Event Reminder Notification preferences
        private const val KEY_NOTIFICATIONS_ENABLED = "event_notifications_enabled"
        private const val KEY_NOTIFY_LEAD_MINUTES = "event_notify_lead_minutes"
        private const val KEY_NOTIFY_GEYSER = "notify_event_geyser"
        private const val KEY_NOTIFY_GRANDMA = "notify_event_grandma"
        private const val KEY_NOTIFY_TURTLE = "notify_event_turtle"
        private const val KEY_NOTIFY_SHARDS = "notify_event_shards"
        private const val KEY_NOTIFY_RESET = "notify_event_reset"
        private const val KEY_OVERLAY_POSITION = "overlay_position" // "BOTTOM" or "TOP"
        private const val KEY_WING_ARC_OFFSET_Y = "wing_arc_offset_y"
        private const val KEY_WING_ARC_SIZE_DP = "wing_arc_size_dp"
        private const val KEY_SANCTUARY_AMBIANCE_EFFECT = "sanctuary_ambiance_effect"
    }

    var skyRunningEnabled: Boolean
        get() = prefs.getBoolean(KEY_SKY_RUNNING_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SKY_RUNNING_ENABLED, value).apply()

    var skyRunningSeconds: Int
        get() = prefs.getInt(KEY_SKY_RUNNING_SECONDS, 10)
        set(value) = prefs.edit().putInt(KEY_SKY_RUNNING_SECONDS, value).apply()

    var quietHoursEnabled: Boolean
        get() = prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_QUIET_HOURS_ENABLED, value).apply()

    var quietStartHour: Int
        get() = prefs.getInt(KEY_QUIET_START_HOUR, 23)
        set(value) = prefs.edit().putInt(KEY_QUIET_START_HOUR, value).apply()

    var quietStartMinute: Int
        get() = prefs.getInt(KEY_QUIET_START_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_QUIET_START_MINUTE, value).apply()

    var quietEndHour: Int
        get() = prefs.getInt(KEY_QUIET_END_HOUR, 8)
        set(value) = prefs.edit().putInt(KEY_QUIET_END_HOUR, value).apply()

    var quietEndMinute: Int
        get() = prefs.getInt(KEY_QUIET_END_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_QUIET_END_MINUTE, value).apply()

    var filterGeyser: Boolean
        get() = prefs.getBoolean(KEY_FILTER_GEYSER, true)
        set(value) = prefs.edit().putBoolean(KEY_FILTER_GEYSER, value).apply()

    var filterGrandma: Boolean
        get() = prefs.getBoolean(KEY_FILTER_GRANDMA, true)
        set(value) = prefs.edit().putBoolean(KEY_FILTER_GRANDMA, value).apply()

    var filterTurtle: Boolean
        get() = prefs.getBoolean(KEY_FILTER_TURTLE, true)
        set(value) = prefs.edit().putBoolean(KEY_FILTER_TURTLE, value).apply()

    var filterShards: Boolean
        get() = prefs.getBoolean(KEY_FILTER_SHARDS, true)
        set(value) = prefs.edit().putBoolean(KEY_FILTER_SHARDS, value).apply()

    var filterReset: Boolean
        get() = prefs.getBoolean(KEY_FILTER_RESET, true)
        set(value) = prefs.edit().putBoolean(KEY_FILTER_RESET, value).apply()

    var filterTsReveal: Boolean
        get() = prefs.getBoolean(KEY_FILTER_TS_REVEAL, true)
        set(value) = prefs.edit().putBoolean(KEY_FILTER_TS_REVEAL, value).apply()

    var overlayOffsetY: Int
        get() = prefs.getInt(KEY_OVERLAY_OFFSET_Y, 0)
        set(value) = prefs.edit().putInt(KEY_OVERLAY_OFFSET_Y, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var notifyLeadMinutes: Int
        get() = prefs.getInt(KEY_NOTIFY_LEAD_MINUTES, 5)
        set(value) = prefs.edit().putInt(KEY_NOTIFY_LEAD_MINUTES, value).apply()

    var notifyGeyser: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_GEYSER, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_GEYSER, value).apply()

    var notifyGrandma: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_GRANDMA, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_GRANDMA, value).apply()

    var notifyTurtle: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_TURTLE, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_TURTLE, value).apply()

    var notifyShards: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_SHARDS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_SHARDS, value).apply()

    var notifyReset: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_RESET, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_RESET, value).apply()

    var overlayPosition: String
        get() = prefs.getString(KEY_OVERLAY_POSITION, "TOP") ?: "TOP"
        set(value) = prefs.edit().putString(KEY_OVERLAY_POSITION, value).apply()

    var wingArcOffsetY: Int
        get() = prefs.getInt(KEY_WING_ARC_OFFSET_Y, 0)
        set(value) = prefs.edit().putInt(KEY_WING_ARC_OFFSET_Y, value).apply()

    var wingArcSizeDp: Int
        get() = prefs.getInt(KEY_WING_ARC_SIZE_DP, 76)
        set(value) = prefs.edit().putInt(KEY_WING_ARC_SIZE_DP, value).apply()

    var sanctuaryAmbianceEffect: String
        get() = prefs.getString(KEY_SANCTUARY_AMBIANCE_EFFECT, "OFF") ?: "OFF"
        set(value) = prefs.edit().putString(KEY_SANCTUARY_AMBIANCE_EFFECT, value).apply()

    var hasCustomFriendOrder: Boolean
        get() = prefs.getBoolean("has_custom_friend_order", false)
        set(value) = prefs.edit().putBoolean("has_custom_friend_order", value).apply()
}
