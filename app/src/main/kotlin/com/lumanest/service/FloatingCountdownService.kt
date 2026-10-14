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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    private var wingArcView: WingArcProgressView? = null

    // Preview & Calibration state
    private var isPreviewActive = false
    private var isCustomizing = false
    private var skyForegroundSeenDuringCustomize = false
    private var customizeStartTime = 0L

    companion object {
        private const val CHANNEL_ID = "sky_running_overlay_channel"
        private const val NOTIFICATION_ID = 9001
        const val ACTION_PREVIEW = "com.lumanest.action.PREVIEW_OVERLAY"
        const val ACTION_CUSTOMIZE = "com.lumanest.action.CUSTOMIZE_OVERLAY"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunningFlow = _isServiceRunning.asStateFlow()

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

        fun triggerCustomize(context: Context) {
            val intent = Intent(context, FloatingCountdownService::class.java).apply {
                action = ACTION_CUSTOMIZE
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
        _isServiceRunning.value = true
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

        setupOverlayView()
        startMonitoringLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CUSTOMIZE -> showInteractiveCustomizer()
            ACTION_PREVIEW -> showPreviewPulse()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Event Progress Bar",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live countdown and progress bar while playing Sky"
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

    private var currentPositionMode: String? = null
    private var wasCustomizing: Boolean = false

    private fun setupOverlayView() {
        if (!Settings.canDrawOverlays(this)) return

        val position = prefs.overlayPosition // "TOP" or "BOTTOM"
        if (overlayView != null && currentPositionMode == position && wasCustomizing == isCustomizing) {
            return
        }

        // Remove old view if switching positions or entering/leaving customization mode
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (_: Exception) {}
            overlayView = null
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        currentPositionMode = position
        wasCustomizing = isCustomizing

        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val density = resources.displayMetrics.density

        if (position == "TOP") {
            // OPTION 2: Wing Progress Arc + Compact Floating Countdown
            val savedArcOffsetY = prefs.wingArcOffsetY // Custom user arc offset from top (dp/px)
            val savedSizeDp = prefs.wingArcSizeDp // Custom user arc size in dp
            val arcSizePx = (savedSizeDp * density).toInt()

            val flags = if (isCustomizing) {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            } else {
                // In game mode: 100% pass-through! Zero touch blocking on the wing icon or game controls!
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            }

            val windowWidth = (320 * density).toInt() // Compact 320dp centered box
            val windowHeight = (145 * density).toInt() // Top 145dp strip

            val params = WindowManager.LayoutParams(
                windowWidth,
                windowHeight,
                layoutParamsType,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                x = 0
                y = 0 // Anchored to top
            }

            // Root layout: FrameLayout so arc and text can be independently positioned without pushing each other down!
            val rootLayout = FrameLayout(this).apply {
                background = null
                layoutParams = FrameLayout.LayoutParams(windowWidth, windowHeight)
            }

            // Fixed Countdown Text: Pinned right at the top edge, unaffected by arc drag
            tvClockText = TextView(this).apply {
                text = "🐢 09:42 left"
                setTextColor(Color.parseColor("#FFEF4444")) // Coral red
                textSize = 11.5f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setShadowLayer(6f, 0f, 1f, Color.BLACK)
                setPadding((8 * density).toInt(), (2 * density).toInt(), (8 * density).toInt(), 0)
                maxLines = 1
                isSingleLine = true
                background = null
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.CENTER_HORIZONTAL
                ).apply {
                    topMargin = (2 * density).toInt()
                }
            }
            rootLayout.addView(tvClockText)

            // Curved Progress Arc Container (Independently draggable & resizable)
            // Zero padding in all modes so saved position never jumps!
            val arcContainer = FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.CENTER_HORIZONTAL
                ).apply {
                    topMargin = savedArcOffsetY // User-defined vertical position!
                }
            }

            wingArcView = WingArcProgressView(this).apply {
                layoutParams = FrameLayout.LayoutParams(arcSizePx, FrameLayout.LayoutParams.WRAP_CONTENT)
                background = null
            }
            arcContainer.addView(wingArcView)
            rootLayout.addView(arcContainer)

            // Interactive Editor Toolbar (Only shown during calibration!)
            if (isCustomizing) {
                val toolbar = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    ).apply {
                        topMargin = (72 * density).toInt()
                    }
                }

                fun createEditButton(label: String, onClick: () -> Unit): TextView {
                    return TextView(this).apply {
                        text = label
                        setTextColor(Color.WHITE)
                        textSize = 11.5f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        gravity = Gravity.CENTER
                        setPadding((11 * density).toInt(), (7 * density).toInt(), (11 * density).toInt(), (7 * density).toInt())
                        val btnBg = android.graphics.drawable.GradientDrawable().apply {
                            setColor(Color.parseColor("#E60F172A"))
                            cornerRadius = 8 * density
                            setStroke((1 * density).toInt(), Color.parseColor("#60FFA51F"))
                        }
                        background = btnBg
                        setOnClickListener { onClick() }
                    }
                }

                // [ Size − ]
                val btnSmaller = createEditButton("Size −") {
                    val newSize = maxOf(36, prefs.wingArcSizeDp - 4)
                    prefs.wingArcSizeDp = newSize
                    val newPx = (newSize * density).toInt()
                    wingArcView?.layoutParams = FrameLayout.LayoutParams(newPx, FrameLayout.LayoutParams.WRAP_CONTENT)
                    wingArcView?.requestLayout()
                }
                toolbar.addView(btnSmaller)

                val space1 = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams((5 * density).toInt(), 1)
                }
                toolbar.addView(space1)

                // [ Size + ]
                val btnBigger = createEditButton("Size +") {
                    val newSize = minOf(150, prefs.wingArcSizeDp + 4)
                    prefs.wingArcSizeDp = newSize
                    val newPx = (newSize * density).toInt()
                    wingArcView?.layoutParams = FrameLayout.LayoutParams(newPx, FrameLayout.LayoutParams.WRAP_CONTENT)
                    wingArcView?.requestLayout()
                }
                toolbar.addView(btnBigger)

                val space2 = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams((5 * density).toInt(), 1)
                }
                toolbar.addView(space2)

                // [ ↺ Reset ]
                val btnReset = createEditButton("↺ Reset") {
                    val defaultOffset = (18 * density).toInt()
                    val defaultSize = 76
                    prefs.wingArcOffsetY = defaultOffset
                    prefs.wingArcSizeDp = defaultSize
                    val newPx = (defaultSize * density).toInt()
                    wingArcView?.layoutParams = FrameLayout.LayoutParams(newPx, FrameLayout.LayoutParams.WRAP_CONTENT)
                    val lp = arcContainer.layoutParams as FrameLayout.LayoutParams
                    lp.topMargin = defaultOffset
                    arcContainer.layoutParams = lp
                    wingArcView?.requestLayout()
                }
                toolbar.addView(btnReset)

                val space3 = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams((6 * density).toInt(), 1)
                }
                toolbar.addView(space3)

                // [ ✓ Save & Set ]
                val btnSave = TextView(this).apply {
                    text = "✓ Save & Set"
                    setTextColor(Color.parseColor("#FF0B0F17"))
                    textSize = 11.5f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setPadding((13 * density).toInt(), (7 * density).toInt(), (13 * density).toInt(), (7 * density).toInt())
                    val saveBg = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor("#FFFFA51F"))
                        cornerRadius = 8 * density
                    }
                    background = saveBg
                    setOnClickListener {
                        isCustomizing = false
                        isPreviewActive = false
                        setupOverlayView()
                        updateCountdownState()
                    }
                }
                toolbar.addView(btnSave)

                rootLayout.addView(toolbar)

                // Drag Up/Down listener strictly clamped so the arc NEVER disappears!
                var initialTouchY = 0f
                var initialMarginTop = 0
                arcContainer.setOnTouchListener { _, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            initialTouchY = event.rawY
                            val lp = arcContainer.layoutParams as FrameLayout.LayoutParams
                            initialMarginTop = lp.topMargin
                            true
                        }
                        android.view.MotionEvent.ACTION_MOVE -> {
                            val dy = (event.rawY - initialTouchY).toInt()
                            val minMargin = 0
                            val maxMargin = (48 * density).toInt()
                            val newMargin = (initialMarginTop + dy).coerceIn(minMargin, maxMargin)
                            val lp = arcContainer.layoutParams as FrameLayout.LayoutParams
                            lp.topMargin = newMargin
                            arcContainer.layoutParams = lp
                            prefs.wingArcOffsetY = newMargin
                            true
                        }
                        else -> false
                    }
                }
            }

            barBackground = null
            progressFill = null

            overlayView = rootLayout
            overlayView?.visibility = View.GONE
            try {
                windowManager?.addView(overlayView, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (position == "EMOJI") {
            // OPTION 3: Floating Emoji Timers Only (Mid Top - No Bar / No Arc)
            wingArcView = null
            barBackground = null
            progressFill = null

            val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutParamsType,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                x = 0
                y = (4 * density).toInt() // Pinned cleanly at top edge (mid top)
            }

            val pillContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding((6 * density).toInt(), (2 * density).toInt(), (6 * density).toInt(), (2 * density).toInt())
                background = null // Pure transparent container, no background box or border
            }

            tvClockText = TextView(this).apply {
                text = "👵 09:42 left"
                setTextColor(Color.parseColor("#FFEF4444"))
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setShadowLayer(8f, 0f, 2f, Color.BLACK)
                maxLines = 1
                isSingleLine = true
            }
            pillContainer.addView(tvClockText)

            overlayView = pillContainer
            overlayView?.visibility = View.GONE
            try {
                windowManager?.addView(overlayView, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            wingArcView = null
            // OPTION 1: Bottom Edge (Full Width)
            val barHeightPx = (6 * density).toInt()
            val cornerRadiusPx = 6 * density

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
                y = 0
            }

            val rootLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val clockRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                setPadding(0, 0, (20 * density).toInt(), (4 * density).toInt())
            }

            tvClockText = TextView(this).apply {
                text = "07:46 left"
                setTextColor(Color.parseColor("#FFEF4444"))
                textSize = 13f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setShadowLayer(6f, 0f, 0f, Color.BLACK)
            }
            clockRow.addView(tvClockText)
            rootLayout.addView(clockRow)

            barBackground = FrameLayout(this).apply {
                val trackBg = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#801E293B"))
                }
                background = trackBg
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    barHeightPx
                )
            }

            progressFill = View(this).apply {
                val fillDrawable = android.graphics.drawable.GradientDrawable().apply {
                    orientation = android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT
                    colors = intArrayOf(
                        Color.parseColor("#FFF97316"),
                        Color.parseColor("#FFEF4444")
                    )
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

    private fun showInteractiveCustomizer() {
        isCustomizing = true
        isPreviewActive = true
        skyForegroundSeenDuringCustomize = false
        customizeStartTime = System.currentTimeMillis()
        setupOverlayView()
        overlayView?.visibility = View.VISIBLE
        tvClockText?.text = "🐢 09:42 left"
        tvClockText?.setTextColor(Color.parseColor("#FFEF4444"))
        updateBarWidth(1.0f) // Full 100% half circle from Right (0°) to Left (180°)!
        // Does NOT disappear automatically! Stays until user taps Save & Set.
    }

    private fun showPreviewPulse() {
        isPreviewActive = true
        setupOverlayView()
        overlayView?.visibility = View.VISIBLE
        tvClockText?.text = when (prefs.overlayPosition) {
            "EMOJI" -> "👵 09:42 left   ·   🐢 in 04:30"
            "TOP" -> "🐢 09:42 left"
            else -> "Test Preview: 10:00 left"
        }
        tvClockText?.setTextColor(Color.parseColor("#FFFB923C"))
        updateBarWidth(1.0f)

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
        // Look back 30 minutes so we still find Sky's ACTIVITY_RESUMED even if
        // the user hasn't switched apps at all (no new resume events fired while Sky runs)
        val lookbackMs = 30 * 60 * 1000L

        return try {
            val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager ?: return true
            val events = usageStatsManager.queryEvents(now - lookbackMs, now)

            var lastResumedPackage: String? = null
            var skyExplicitlyPaused = false
            val event = android.app.usage.UsageEvents.Event()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED -> {
                        lastResumedPackage = event.packageName
                        // If Sky is resuming, it is definitely in the foreground
                        if (event.packageName == "com.tgc.sky.android") {
                            skyExplicitlyPaused = false
                        }
                    }
                    android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED -> {
                        // If Sky was explicitly backgrounded, clear our cached foreground flag
                        if (event.packageName == "com.tgc.sky.android") {
                            skyExplicitlyPaused = true
                        }
                    }
                }
            }

            // Sky was explicitly paused (another app resumed after Sky) → hide bar
            if (skyExplicitlyPaused && lastResumedPackage != "com.tgc.sky.android") {
                lastKnownSkyForeground = false
                return false
            }

            if (lastResumedPackage != null) {
                val isSky = lastResumedPackage == "com.tgc.sky.android"
                lastKnownSkyForeground = isSky
                lastForegroundDetectionTime = now
                isSky
            } else {
                // No usage events at all in the last 30 min — use cached value with a
                // generous 10-minute grace period so brief polling gaps never hide the bar
                if (lastKnownSkyForeground && (now - lastForegroundDetectionTime) < 10 * 60 * 1000L) {
                    return true
                }
                false
            }
        } catch (_: Exception) {
            // On any error, trust our cached value rather than wrongly hiding the bar
            if (lastKnownSkyForeground && (now - lastForegroundDetectionTime) < 10 * 60 * 1000L) {
                true
            } else {
                true // Default to visible if we genuinely can't tell
            }
        }
    }

    private fun getEventEmojiSpannable(eventId: String, suffixText: String): CharSequence {
        val shardRes = when (eventId) {
            "daily_shard_red" -> com.lumanest.app.R.drawable.emoji_shard_red
            "daily_shard_black" -> com.lumanest.app.R.drawable.emoji_shard_black
            else -> null
        }

        if (shardRes != null) {
            val ssb = android.text.SpannableStringBuilder("  $suffixText")
            val drawable = androidx.core.content.ContextCompat.getDrawable(this, shardRes)
            if (drawable != null) {
                val sizePx = (16 * resources.displayMetrics.density).toInt()
                drawable.setBounds(0, 0, sizePx, sizePx)
                val span = android.text.style.ImageSpan(drawable, android.text.style.ImageSpan.ALIGN_CENTER)
                ssb.setSpan(span, 0, 1, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            return ssb
        }

        val emoji = when (eventId) {
            "daily_grandma" -> "👵"
            "daily_geyser" -> "🌋"
            "daily_turtle" -> "🐢"
            "daily_shard_red" -> "💎"
            "daily_shard_black" -> "🖤"
            else -> "✨"
        }
        return "$emoji $suffixText"
    }

    private fun updateCountdownState() {
        if (!prefs.skyRunningEnabled) {
            stopSelf()
            return
        }

        if (isCustomizing) {
            val isSky = isSkyGameForeground()
            if (isSky) {
                skyForegroundSeenDuringCustomize = true
            }

            // Only auto-dismiss if Sky was already active and then the user left Sky (pressed Home button)
            // Or safety timeout of 60 seconds if Sky never opened
            val elapsed = System.currentTimeMillis() - customizeStartTime
            if (skyForegroundSeenDuringCustomize && !isSky) {
                isCustomizing = false
                isPreviewActive = false
                setupOverlayView()
                overlayView?.visibility = View.GONE
                return
            } else if (!skyForegroundSeenDuringCustomize && elapsed > 60000L) {
                isCustomizing = false
                isPreviewActive = false
                setupOverlayView()
                overlayView?.visibility = View.GONE
                return
            }
            return
        }

        if (isPreviewActive) return
        setupOverlayView()

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

        val maxEarlyMs = 5 * 60 * 1000L // Strictly max 5 minutes early display rule!
        val thresholdSeconds = prefs.skyRunningSeconds
        val thresholdMs = if (prefs.overlayPosition == "EMOJI") {
            minOf(thresholdSeconds * 1000L, maxEarlyMs)
        } else {
            thresholdSeconds * 1000L
        }

        if (prefs.overlayPosition == "EMOJI") {
            val activeEvents = validOccurrences.filter { it.isActiveNow }
            val upcomingEvents = validOccurrences.filter { !it.isActiveNow && it.millisUntilStart <= thresholdMs }

            if (activeEvents.isNotEmpty() || upcomingEvents.isNotEmpty()) {
                overlayView?.visibility = View.VISIBLE
                val pillParts = mutableListOf<CharSequence>()

                for (act in activeEvents) {
                    val secRemaining = act.millisRemaining / 1000
                    val min = secRemaining / 60
                    val sec = secRemaining % 60
                    pillParts.add(getEventEmojiSpannable(act.event.id, String.format("%02d:%02d left", min, sec)))
                }

                for (nxt in upcomingEvents) {
                    val secUntil = (nxt.millisUntilStart / 1000) + 1
                    val countdownDisplay = if (secUntil >= 60) {
                        String.format("%02d:%02d", secUntil / 60, secUntil % 60)
                    } else {
                        "${secUntil}s"
                    }
                    pillParts.add(getEventEmojiSpannable(nxt.event.id, "in $countdownDisplay"))
                }

                tvClockText?.setTextColor(if (activeEvents.isNotEmpty()) Color.parseColor("#FFEF4444") else Color.parseColor("#FFF97316"))
                val ssb = android.text.SpannableStringBuilder()
                for (i in pillParts.indices) {
                    if (i > 0) ssb.append("   ·   ")
                    ssb.append(pillParts[i])
                }
                tvClockText?.text = ssb
            } else {
                overlayView?.visibility = View.GONE
            }
        } else {
            val active = validOccurrences.firstOrNull { it.isActiveNow }
            val next = validOccurrences.filter { !it.isActiveNow }.minByOrNull { it.millisUntilStart }

            if (active != null) {
                // Event currently in progress! Show depleting progress bar & remaining clock with emoji
                overlayView?.visibility = View.VISIBLE

                val totalDurationMs = maxOf(1L, active.endEpochMillis - active.startEpochMillis)
                val fraction = (active.millisRemaining.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                updateBarWidth(fraction)

                val secRemaining = active.millisRemaining / 1000
                val min = secRemaining / 60
                val sec = secRemaining % 60

                tvClockText?.setTextColor(Color.parseColor("#FFEF4444"))
                tvClockText?.text = getEventEmojiSpannable(active.event.id, String.format("%02d:%02d left", min, sec))
            } else if (next != null && next.millisUntilStart <= thresholdMs) {
                // Within early countdown travel window (e.g. 5m, 2m, 1m, 30s before event starts)
                overlayView?.visibility = View.VISIBLE

                val fraction = (next.millisUntilStart.toFloat() / thresholdMs.toFloat()).coerceIn(0f, 1f)
                updateBarWidth(fraction)

                val secUntil = (next.millisUntilStart / 1000) + 1
                val countdownDisplay = if (secUntil >= 60) {
                    String.format("%02d:%02d", secUntil / 60, secUntil % 60)
                } else {
                    "${secUntil}s"
                }

                tvClockText?.setTextColor(Color.parseColor("#FFF97316"))
                val suffix = if (prefs.overlayPosition == "TOP") {
                    "in $countdownDisplay"
                } else {
                    "${next.event.name} in $countdownDisplay"
                }
                tvClockText?.text = getEventEmojiSpannable(next.event.id, suffix)
            } else {
                // Outside of active event and countdown window -> hide bar
                overlayView?.visibility = View.GONE
            }
        }
    }

    private fun updateBarWidth(fraction: Float) {
        // Update Wing Arc Progress (flows right to left)
        wingArcView?.progressFraction = fraction

        // Update Bottom Bar Progress
        val totalTrackWidth = barBackground?.width?.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val targetWidth = (totalTrackWidth * fraction).toInt()
        val lp = progressFill?.layoutParams as? FrameLayout.LayoutParams ?: FrameLayout.LayoutParams(targetWidth, FrameLayout.LayoutParams.MATCH_PARENT)
        lp.width = targetWidth
        progressFill?.layoutParams = lp
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
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
