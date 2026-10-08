package com.lumanest.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.lumanest.data.local.PreferencesManager
import com.lumanest.eventengine.SkyEventEngine

/**
 * FloatingCountdownService renders a sleek glowing in-game bottom loading bar
 * matching the user's screenshot at the bottom edge of the screen, with a
 * countdown / time-remaining clock on the right side.
 */
class FloatingCountdownService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: PreferencesManager

    // In-game bottom bar elements
    private var barBackground: FrameLayout? = null
    private var progressFill: View? = null
    private var tvClockText: TextView? = null

    // Preview state (to test in Settings)
    private var isPreviewActive = false

    companion object {
        private const val CHANNEL_ID = "sky_running_overlay_channel"
        private const val NOTIFICATION_ID = 9001
        const val ACTION_PREVIEW = "com.lumanest.action.PREVIEW_OVERLAY"

        fun start(context: Context) {
            val intent = Intent(context, FloatingCountdownService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun triggerPreview(context: Context) {
            val intent = Intent(context, FloatingCountdownService::class.java).apply {
                action = ACTION_PREVIEW
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingCountdownService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(this)
        createNotificationChannel()

        val notification = buildForegroundNotification("Monitoring Sky daily events in background...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } catch (_: Exception) {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        setupBottomOverlayView()
        startMonitoringLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PREVIEW) {
            showPreviewPulse()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sky Running Mode",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live bottom countdown and progress bar while playing Sky"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sky Running Mode Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun setupBottomOverlayView() {
        if (!Settings.canDrawOverlays(this)) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val density = resources.displayMetrics.density
        val barHeightPx = (6 * density).toInt() // 6dp clean slim bar
        val cornerRadiusPx = 6 * density // Rounded cap on depleted end

        // Full width container pinned directly to the absolute bottom of the screen
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            x = 0
            y = 0 // Locked directly flush to the very bottom edge of the display!
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Digital clock row on the right (above the bar)
        val clockRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, 0, (20 * density).toInt(), (4 * density).toInt())
        }

        tvClockText = TextView(this).apply {
            text = "07:46 left"
            setTextColor(Color.parseColor("#FFEF4444")) // Vibrant warm red/orange clock
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setShadowLayer(6f, 0f, 0f, Color.BLACK)
        }
        clockRow.addView(tvClockText)
        rootLayout.addView(clockRow)

        // Slim Progress Bar Track
        barBackground = FrameLayout(this).apply {
            val trackBg = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#801E293B")) // Crisp visible translucent dark track
            }
            background = trackBg
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                barHeightPx
            )
        }

        // Filled glowing gradient bar with smooth rounded cap so the ending is never harsh
        progressFill = View(this).apply {
            val fillDrawable = android.graphics.drawable.GradientDrawable().apply {
                orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                colors = intArrayOf(
                    Color.parseColor("#FFF97316"), // Vibrant glowing orange
                    Color.parseColor("#FFEF4444")  // Radiant coral red
                )
                // Smooth rounded corners on top-right and bottom-right so the depleting head looks smooth
                cornerRadii = floatArrayOf(
                    0f, 0f,
                    cornerRadiusPx, cornerRadiusPx,
                    cornerRadiusPx, cornerRadiusPx,
                    0f, 0f
                )
            }
            background = fillDrawable
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        barBackground?.addView(progressFill)
        rootLayout.addView(barBackground)

        overlayView = rootLayout
        overlayView?.visibility = View.GONE
        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val tickerRunnable = object : Runnable {
        override fun run() {
            updateCountdownState()
            handler.postDelayed(this, 1000)
        }
    }

    private fun startMonitoringLoop() {
        handler.post(tickerRunnable)
    }

    private fun showPreviewPulse() {
        isPreviewActive = true
        if (overlayView == null && Settings.canDrawOverlays(this)) {
            setupBottomOverlayView()
        }
        overlayView?.visibility = View.VISIBLE
        tvClockText?.text = "Test Preview: 10:00 left"
        tvClockText?.setTextColor(Color.parseColor("#FFFB923C"))
        updateBarWidth(0.75f)

        handler.postDelayed({
            isPreviewActive = false
            updateCountdownState()
        }, 10000L) // Show preview for 10 seconds
    }

    private fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = getSystemService(APP_OPS_SERVICE) as? android.app.AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    packageName
                )
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    private var lastKnownSkyForeground: Boolean = false
    private var lastForegroundDetectionTime: Long = 0L

    private fun isSkyGameForeground(): Boolean {
        // If Usage Access is NOT granted by user, gracefully show the overlay so the feature isn't broken
        if (!hasUsageStatsPermission()) {
            return true
        }

        val now = System.currentTimeMillis()

        return try {
            val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager ?: return true
            val events = usageStatsManager.queryEvents(now - 1000 * 15, now)
            var lastForegroundPackage: String? = null
            val event = android.app.usage.UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastForegroundPackage = event.packageName
                }
            }

            if (lastForegroundPackage != null) {
                val isSky = lastForegroundPackage == "com.tgc.sky.android"
                lastKnownSkyForeground = isSky
                lastForegroundDetectionTime = now
                isSky
            } else {
                // Check if within 15s grace period of previously detected Sky session
                if (lastKnownSkyForeground && (now - lastForegroundDetectionTime) < 15_000L) {
                    return true
                }

                // Fallback to queryUsageStats
                val stats = usageStatsManager.queryUsageStats(
                    android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                    now - 1000 * 60,
                    now
                )
                val currentApp = stats?.maxByOrNull { it.lastTimeUsed }?.packageName
                val isSky = currentApp == "com.tgc.sky.android"
                if (isSky) {
                    lastKnownSkyForeground = true
                    lastForegroundDetectionTime = now
                }
                isSky
            }
        } catch (_: Exception) {
            true
        }
    }

    private fun updateCountdownState() {
        if (!prefs.skyRunningEnabled) {
            stopSelf()
            return
        }

        if (isPreviewActive) return

        if (overlayView == null && Settings.canDrawOverlays(this)) {
            setupBottomOverlayView()
        }

        // Strictly check if Sky game is in the foreground
        if (!isSkyGameForeground()) {
            overlayView?.visibility = View.GONE
            return
        }

        val nowEpoch = System.currentTimeMillis()
        val allOccurrences = SkyEventEngine.calculateDailyOccurrences(nowEpoch)

        // STRICT FILTER RULES:
        // 1. Only track fast recurring social wax events: Geyser, Grandma, Turtle
        // 2. EXCLUDE Daily Reset, TS Reveal, and Shards (per Option 1: no long countdowns)
        val validOccurrences = allOccurrences.filter { occ ->
            val id = occ.event.id
            when (id) {
                "daily_geyser" -> prefs.filterGeyser
                "daily_grandma" -> prefs.filterGrandma
                "daily_turtle" -> prefs.filterTurtle
                else -> false // Exclude Shards, Reset, TS Reveal from floating progress bar
            }
        }

        val active = validOccurrences.firstOrNull { it.isActiveNow }
        val next = validOccurrences.filter { !it.isActiveNow }.minByOrNull { it.millisUntilStart }

        val thresholdSeconds = prefs.skyRunningSeconds // 300s (5m), 120s (2m), 60s (1m), 30s
        val thresholdMs = thresholdSeconds * 1000L

        if (active != null) {
            // Event currently in progress! Show depleting progress bar & remaining clock with emoji
            overlayView?.visibility = View.VISIBLE

            val totalDurationMs = maxOf(1L, active.endEpochMillis - active.startEpochMillis)
            val fraction = (active.millisRemaining.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
            updateBarWidth(fraction)

            val secRemaining = active.millisRemaining / 1000
            val min = secRemaining / 60
            val sec = secRemaining % 60

            val emoji = when (active.event.id) {
                "daily_grandma" -> "👵"
                "daily_geyser" -> "🌋"
                "daily_turtle" -> "🐢"
                "daily_shard_red" -> "🔴"
                "daily_shard_black" -> "⚫"
                else -> "✨"
            }

            tvClockText?.setTextColor(Color.parseColor("#FFEF4444"))
            tvClockText?.text = String.format("%s %02d:%02d left", emoji, min, sec)
        } else if (next != null && next.millisUntilStart <= thresholdMs) {
            // Within early countdown travel window (e.g. 2 min before Geyser starts)
            overlayView?.visibility = View.VISIBLE

            val secUntil = (next.millisUntilStart / 1000) + 1
            val fraction = (next.millisUntilStart.toFloat() / thresholdMs.toFloat()).coerceIn(0f, 1f)
            updateBarWidth(fraction)

            val countdownDisplay = if (secUntil >= 60) {
                String.format("%02d:%02d", secUntil / 60, secUntil % 60)
            } else {
                "${secUntil}s"
            }

            val nextEmoji = when (next.event.id) {
                "daily_grandma" -> "👵"
                "daily_geyser" -> "🌋"
                "daily_turtle" -> "🐢"
                "daily_shard_red" -> "🔴"
                "daily_shard_black" -> "⚫"
                else -> "✨"
            }

            tvClockText?.setTextColor(Color.parseColor("#FFF97316"))
            tvClockText?.text = "$nextEmoji ${next.event.name} in $countdownDisplay"
        } else {
            // Outside of active event and countdown window -> hide bar
            overlayView?.visibility = View.GONE
        }
    }

    private fun updateBarWidth(fraction: Float) {
        val displayWidth = resources.displayMetrics.widthPixels
        val targetWidth = (displayWidth * fraction).toInt()
        val lp = progressFill?.layoutParams as? FrameLayout.LayoutParams ?: FrameLayout.LayoutParams(targetWidth, FrameLayout.LayoutParams.MATCH_PARENT)
        lp.width = targetWidth
        progressFill?.layoutParams = lp
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(tickerRunnable)
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (_: Exception) {}
            overlayView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
