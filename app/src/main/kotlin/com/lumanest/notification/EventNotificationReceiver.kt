package com.lumanest.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.lumanest.app.R
import com.lumanest.app.ui.MainActivity
import com.lumanest.data.local.PreferencesManager
import com.lumanest.eventengine.SkyEventEngine
import java.time.Instant
import java.time.ZonedDateTime

/**
 * EventNotificationReceiver manages scheduling and dispatching notifications
 * for Sky: Children of the Light daily events (Geyser, Grandma, Turtle, Shards, Reset).
 *
 * SMART SUPPRESSION:
 * 1. Suppresses if Quiet Hours are currently active.
 * 2. Suppresses if Sky: Children of the Light is currently running in the foreground
 *    (as the in-game Floating Countdown bar is already displaying the countdown).
 */
class EventNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PreferencesManager(context)
        if (!prefs.notificationsEnabled) return

        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: return
        val eventName = intent.getStringExtra(EXTRA_EVENT_NAME) ?: "Sky Event"
        val leadMinutes = intent.getIntExtra(EXTRA_LEAD_MINUTES, 5)

        // 1. Check Quiet Hours
        if (isQuietHoursActive(prefs)) {
            // Muted by Quiet Hours; schedule next alarm and return
            scheduleNextAlarms(context)
            return
        }

        // 2. Check if Sky game is currently running in the foreground (User rule)
        if (isSkyGameForeground(context)) {
            // Suppress notification because Floating Countdown bar is already handling it
            scheduleNextAlarms(context)
            return
        }

        // 3. Post Event Reminder Notification
        showNotification(context, eventId, eventName, leadMinutes)

        // 4. Reschedule upcoming alarms
        scheduleNextAlarms(context)

        // 5. Auto-refresh Home Screen widgets
        com.lumanest.widget.WidgetSyncManager.refreshWidgets(context)
        com.lumanest.widget.WidgetSyncManager.scheduleNextTransition(context)
    }

    private fun showNotification(context: Context, eventId: String, eventName: String, leadMinutes: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel(notificationManager)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val emoji = when (eventId) {
            "daily_geyser" -> "♨️"
            "daily_grandma" -> "🍞"
            "daily_turtle" -> "🐢"
            "daily_shard_red" -> "💎"
            "daily_shard_black" -> "🖤"
            "daily_reset" -> "🌅"
            else -> "✨"
        }

        val title = "$emoji $eventName starting in $leadMinutes min!"
        val message = when (eventId) {
            "daily_geyser" -> "Get ready at Sanctuary Islands geyser."
            "daily_grandma" -> "Grandma is preparing dinner in Hidden Forest."
            "daily_turtle" -> "Sanctuary turtle is swimming up at the reef."
            "daily_shard_red" -> "Red Shard eruption imminent! Ascended Candles await."
            "daily_shard_black" -> "Black Shard falling soon. Regular wax available."
            "daily_reset" -> "Sky daily quest and candle limits have reset!"
            else -> "A new Sky event is about to begin."
        }

        val notificationId = when (eventId) {
            "daily_geyser" -> 1001
            "daily_grandma" -> 1002
            "daily_turtle" -> 1003
            "daily_shard_red", "daily_shard_black" -> 1004
            "daily_reset" -> 1005
            else -> 1000
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Missing POST_NOTIFICATIONS on Android 13+
        }
    }

    private fun isQuietHoursActive(prefs: PreferencesManager): Boolean {
        if (!prefs.quietHoursEnabled) return false
        val now = java.time.LocalTime.now()
        val currentMinutes = now.hour * 60 + now.minute
        val startMinutes = prefs.quietStartHour * 60 + prefs.quietStartMinute
        val endMinutes = prefs.quietEndHour * 60 + prefs.quietEndMinute

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            // Spans across midnight (e.g. 23:00 to 07:00)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    private fun isSkyGameForeground(context: Context): Boolean {
        val now = System.currentTimeMillis()
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return false
            val events = usageStatsManager.queryEvents(now - 15_000L, now)
            var lastForegroundPackage: String? = null
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastForegroundPackage = event.packageName
                }
            }
            if (lastForegroundPackage != null) {
                return lastForegroundPackage == "com.tgc.sky.android"
            }
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60_000L, now)
            val currentApp = stats?.maxByOrNull { it.lastTimeUsed }?.packageName
            currentApp == "com.tgc.sky.android"
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        const val CHANNEL_ID = "sky_event_reminders"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_EVENT_NAME = "extra_event_name"
        const val EXTRA_LEAD_MINUTES = "extra_lead_minutes"

        fun createChannel(manager: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val existing = manager.getNotificationChannel(CHANNEL_ID)
                if (existing == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "Sky Event Reminders",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Notifications for Geyser, Grandma, Turtle, Shards, and Daily Reset"
                        enableVibration(true)
                    }
                    manager.createNotificationChannel(channel)
                }
            }
        }

        fun scheduleNextAlarms(context: Context) {
            val prefs = PreferencesManager(context)
            if (!prefs.notificationsEnabled) {
                cancelAllAlarms(context)
                return
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val nowEpoch = System.currentTimeMillis()
            val leadMs = prefs.notifyLeadMinutes * 60 * 1000L
            val occurrences = SkyEventEngine.calculateDailyOccurrences(nowEpoch)

            for (occ in occurrences) {
                val eventId = occ.event.id
                val shouldNotify = when (eventId) {
                    "daily_geyser" -> prefs.notifyGeyser
                    "daily_grandma" -> prefs.notifyGrandma
                    "daily_turtle" -> prefs.notifyTurtle
                    "daily_shard_red", "daily_shard_black" -> prefs.notifyShards
                    "daily_reset" -> prefs.notifyReset
                    else -> false
                }
                if (!shouldNotify) continue

                // Notification trigger timestamp: event start time MINUS lead minutes
                var triggerTime = occ.startEpochMillis - leadMs

                // If trigger time has already passed for this cycle, calculate for the next cycle
                if (triggerTime <= nowEpoch) {
                    // For recurring 2-hour events
                    if (eventId in listOf("daily_geyser", "daily_grandma", "daily_turtle")) {
                        triggerTime += 2 * 60 * 60 * 1000L
                    } else if (eventId == "daily_reset") {
                        triggerTime += 24 * 60 * 60 * 1000L
                    } else {
                        continue
                    }
                }

                val requestCode = when (eventId) {
                    "daily_geyser" -> 2001
                    "daily_grandma" -> 2002
                    "daily_turtle" -> 2003
                    "daily_shard_red", "daily_shard_black" -> 2004
                    "daily_reset" -> 2005
                    else -> 2000
                }

                val intent = Intent(context, EventNotificationReceiver::class.java).apply {
                    putExtra(EXTRA_EVENT_ID, eventId)
                    putExtra(EXTRA_EVENT_NAME, occ.event.name)
                    putExtra(EXTRA_LEAD_MINUTES, prefs.notifyLeadMinutes)
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } catch (_: SecurityException) {
                    // EXACT_ALARM permission missing on Android 14+; fallback to normal set
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            }
        }

        fun cancelAllAlarms(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val requestCodes = listOf(2001, 2002, 2003, 2004, 2005)
            for (code in requestCodes) {
                val intent = Intent(context, EventNotificationReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    code,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                }
            }
        }
    }
}
