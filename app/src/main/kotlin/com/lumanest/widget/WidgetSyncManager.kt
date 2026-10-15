package com.lumanest.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.glance.appwidget.updateAll
import com.lumanest.eventengine.SkyEventEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages synchronizing and auto-refreshing LumaNest Home Screen widgets
 * precisely at event transition boundaries (e.g. when an event starts or ends).
 */
object WidgetSyncManager {

    private const val REQUEST_CODE_WIDGET_TRANSITION = 7701

    fun refreshWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EventBannerGlanceWidget().updateAll(context)
                EventCardGlanceWidget().updateAll(context)
                SeasonBannerGlanceWidget().updateAll(context)
                SeasonCardGlanceWidget().updateAll(context)
            } catch (_: Exception) {
                // Ignore widget update errors if no instances are currently placed
            }
        }
    }

    fun scheduleNextTransition(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()

        // 1. Gather all transition boundary timestamps (start & end) for tracked events
        val occurrences = try {
            SkyEventEngine.calculateDailyOccurrences(now)
        } catch (_: Exception) {
            emptyList()
        }

        val transitionTimestamps = mutableListOf<Long>()
        for (occ in occurrences) {
            if (occ.startEpochMillis > now) {
                transitionTimestamps.add(occ.startEpochMillis)
            }
            if (occ.endEpochMillis > now) {
                transitionTimestamps.add(occ.endEpochMillis)
            }
        }

        // Find the earliest upcoming transition
        val nextTransitionTime = transitionTimestamps.minOrNull() ?: return

        val intent = Intent(context, WidgetUpdateReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_WIDGET_TRANSITION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTransitionTime + 1000L, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTransitionTime + 1000L, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextTransitionTime + 1000L, pendingIntent)
        }
    }
}

/**
 * BroadcastReceiver triggered by exact AlarmManager alarms at event start/end boundaries
 * to refresh Glance widgets immediately.
 */
class WidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WidgetSyncManager.refreshWidgets(context)
        WidgetSyncManager.scheduleNextTransition(context)
    }
}
