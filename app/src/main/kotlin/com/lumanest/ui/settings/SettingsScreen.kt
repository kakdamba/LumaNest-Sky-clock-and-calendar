package com.lumanest.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.lumanest.timezone.TimezoneHelper
import com.lumanest.ui.theme.*
import java.time.ZoneId

@Composable
fun SettingsScreen(
    currentZoneId: ZoneId,
    is24Hour: Boolean,
    onTimeFormatChanged: (Boolean) -> Unit,
    onZoneIdChanged: (ZoneId) -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val view = androidx.compose.ui.platform.LocalView.current
    val performHaptic = {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }
    val prefs = remember { com.lumanest.data.local.PreferencesManager(context) }

    var quietHoursEnabled by remember { mutableStateOf(prefs.quietHoursEnabled) }
    var startHour by remember { mutableStateOf(prefs.quietStartHour) }
    var startMinute by remember { mutableStateOf(prefs.quietStartMinute) }
    var endHour by remember { mutableStateOf(prefs.quietEndHour) }
    var endMinute by remember { mutableStateOf(prefs.quietEndMinute) }

    var skyRunningDetection by remember { mutableStateOf(prefs.skyRunningEnabled) }
    var countdownSeconds by remember { mutableStateOf(prefs.skyRunningSeconds) }
    var overlayPosition by remember { mutableStateOf(prefs.overlayPosition) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var showUsageAccessDialog by remember { mutableStateOf(false) }

    // Event Reminders state
    var notificationsEnabled by remember { mutableStateOf(prefs.notificationsEnabled) }
    var notifyLeadMinutes by remember { mutableStateOf(prefs.notifyLeadMinutes) }
    var notifyGeyser by remember { mutableStateOf(prefs.notifyGeyser) }
    var notifyGrandma by remember { mutableStateOf(prefs.notifyGrandma) }
    var notifyTurtle by remember { mutableStateOf(prefs.notifyTurtle) }
    var notifyShards by remember { mutableStateOf(prefs.notifyShards) }
    var notifyReset by remember { mutableStateOf(prefs.notifyReset) }
    var sanctuaryAmbianceEffect by remember { mutableStateOf(prefs.sanctuaryAmbianceEffect) }
    var isEventsDropdownExpanded by remember { mutableStateOf(false) }

    val currentVersionName = remember {
        com.lumanest.updater.AppUpdater.getInstalledVersionName(context)
    }
    val displayVersion = remember(currentVersionName) {
        if (currentVersionName.startsWith("V.", ignoreCase = true) || currentVersionName.startsWith("v", ignoreCase = true)) {
            "LumaNest $currentVersionName"
        } else {
            "LumaNest V.$currentVersionName"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), DeepMidnight, Color(0xFF090D14))
                )
            )
    ) {
        val isStarlightActive = sanctuaryAmbianceEffect == "STARLIGHT_DRIFT" || sanctuaryAmbianceEffect == "AERO_SHARDS"
        if (isStarlightActive) {
            com.lumanest.ui.components.AeroShardsBackground()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = DogFluffWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configure time format, quiet hours, and companion preferences",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSlate
            )
        }

        // 1. Time Format Selection Card (12h vs 24h)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "TIME FORMAT",
                        style = MaterialTheme.typography.labelMedium,
                        color = CozyWarmAmber,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "12-Hour Format (AM / PM)",
                                style = MaterialTheme.typography.titleMedium,
                                color = DogFluffWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (!is24Hour) "Active: e.g. 02:35 PM" else "Toggle for AM/PM display",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlate
                            )
                        }
                        Switch(
                            checked = !is24Hour,
                            onCheckedChange = {
                                performHaptic()
                                onTimeFormatChanged(!it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepMidnight,
                                checkedTrackColor = CozyWarmAmber
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = NightSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Current preview: ${TimezoneHelper.getCurrentTimeFormatted(currentZoneId, is24Hour)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = StarlightBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 2. Event Notifications Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "EVENT NOTIFICATIONS",
                        style = MaterialTheme.typography.labelMedium,
                        color = CozyWarmAmber,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Event Reminders",
                                style = MaterialTheme.typography.titleMedium,
                                color = DogFluffWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (notificationsEnabled) "Remind before daily events start" else "Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlate
                            )
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = {
                                performHaptic()
                                notificationsEnabled = it
                                prefs.notificationsEnabled = it
                                if (it) {
                                    com.lumanest.notification.EventNotificationReceiver.scheduleNextAlarms(context)
                                } else {
                                    com.lumanest.notification.EventNotificationReceiver.cancelAllAlarms(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepMidnight,
                                checkedTrackColor = CozyWarmAmber
                            )
                        )
                    }

                    if (notificationsEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = NightSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "REMIND ME BEFORE EVENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = CozyWarmAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5, 10, 15).forEach { mins ->
                                val isSelected = notifyLeadMinutes == mins
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) CozyWarmAmber else NightSurfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            performHaptic()
                                            notifyLeadMinutes = mins
                                            prefs.notifyLeadMinutes = mins
                                            com.lumanest.notification.EventNotificationReceiver.scheduleNextAlarms(context)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${mins}m before",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) DeepMidnight else DogFluffWhite
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Expandable Dropdown Header
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NightSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    performHaptic()
                                    isEventsDropdownExpanded = !isEventsDropdownExpanded
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tracked Events Options",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DogFluffWhite,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isEventsDropdownExpanded) "Tap to collapse options" else "Tap to choose which events notify you",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedSlate
                                    )
                                }
                                Text(
                                    text = if (isEventsDropdownExpanded) "▲" else "▼",
                                    color = CozyWarmAmber,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        @Composable
                        fun EventReminderRow(
                            emoji: String,
                            title: String,
                            subtitle: String,
                            checked: Boolean,
                            onToggle: (Boolean) -> Unit
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = emoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = DogFluffWhite,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MutedSlate
                                        )
                                    }
                                }
                                Switch(
                                    checked = checked,
                                    onCheckedChange = {
                                        performHaptic()
                                        onToggle(it)
                                        com.lumanest.notification.EventNotificationReceiver.scheduleNextAlarms(context)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DeepMidnight,
                                        checkedTrackColor = CozyWarmAmber
                                    )
                                )
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isEventsDropdownExpanded,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text(
                                    text = "SELECT EVENTS TO TRACK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CozyWarmAmber,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                EventReminderRow(
                                    emoji = "♨️",
                                    title = "Polluted Geyser",
                                    subtitle = "Every 2 hours at XX:05 PT",
                                    checked = notifyGeyser,
                                    onToggle = { notifyGeyser = it; prefs.notifyGeyser = it }
                                )

                                EventReminderRow(
                                    emoji = "🍞",
                                    title = "Grandma's Dinner",
                                    subtitle = "Every 2 hours at XX:35 PT",
                                    checked = notifyGrandma,
                                    onToggle = { notifyGrandma = it; prefs.notifyGrandma = it }
                                )

                                EventReminderRow(
                                    emoji = "🐢",
                                    title = "Sanctuary Turtle",
                                    subtitle = "Every 2 hours at XX:50 PT",
                                    checked = notifyTurtle,
                                    onToggle = { notifyTurtle = it; prefs.notifyTurtle = it }
                                )

                                EventReminderRow(
                                    emoji = "🌋",
                                    title = "Shard Eruptions",
                                    subtitle = "Red & Black shard falling windows",
                                    checked = notifyShards,
                                    onToggle = { notifyShards = it; prefs.notifyShards = it }
                                )

                                EventReminderRow(
                                    emoji = "🌅",
                                    title = "Daily Reset",
                                    subtitle = "00:00 PT Sky server daily reset",
                                    checked = notifyReset,
                                    onToggle = { notifyReset = it; prefs.notifyReset = it }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NightSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🎮 Smart Rule: Notifications are automatically silenced while playing Sky with the Floating Countdown active.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StarlightBlue,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Quiet Hours Settings (Customizable with native TimePickerDialog)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
                )
            ) {
                fun formatTime(hour: Int, minute: Int): String {
                    return if (!is24Hour) {
                        val h12 = if (hour % 12 == 0) 12 else hour % 12
                        val amPm = if (hour < 12) "AM" else "PM"
                        "%02d:%02d %s".format(h12, minute, amPm)
                    } else {
                        "%02d:%02d".format(hour, minute)
                    }
                }

                fun showTimePicker(initialHour: Int, initialMinute: Int, onTimeSelected: (Int, Int) -> Unit) {
                    android.app.TimePickerDialog(
                        context,
                        { _, h, m -> onTimeSelected(h, m) },
                        initialHour,
                        initialMinute,
                        is24Hour
                    ).show()
                }

                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Quiet Hours",
                                style = MaterialTheme.typography.titleMedium,
                                color = DogFluffWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (quietHoursEnabled)
                                    "Mute reminders (${formatTime(startHour, startMinute)} → ${formatTime(endHour, endMinute)})"
                                else
                                    "Notifications enabled 24/7",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlate
                            )
                        }
                        Switch(
                            checked = quietHoursEnabled,
                            onCheckedChange = {
                                performHaptic()
                                quietHoursEnabled = it
                                prefs.quietHoursEnabled = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepMidnight,
                                checkedTrackColor = CozyWarmAmber
                            )
                        )
                    }

                    if (quietHoursEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = NightSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "TAP TIME TO SET ALARM CLOCK",
                            style = MaterialTheme.typography.labelSmall,
                            color = CozyWarmAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Start Time (Tap to open Clock Dialog)
                            Column {
                                Text("Quiet From", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = NightSurfaceVariant,
                                    onClick = {
                                        showTimePicker(startHour, startMinute) { h, m ->
                                            startHour = h
                                            startMinute = m
                                            prefs.quietStartHour = h
                                            prefs.quietStartMinute = m
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "⏰", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = formatTime(startHour, startMinute),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = CozyWarmAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // End Time (Tap to open Clock Dialog)
                            Column {
                                Text("Quiet Until", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = NightSurfaceVariant,
                                    onClick = {
                                        showTimePicker(endHour, endMinute) { h, m ->
                                            endHour = h
                                            endMinute = m
                                            prefs.quietEndHour = h
                                            prefs.quietEndMinute = m
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "☀️", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = formatTime(endHour, endMinute),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = CozyWarmAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Live Event Progress Bar (Interactive with uniform selector & position options)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Live Event Progress Bar",
                                style = MaterialTheme.typography.titleMedium,
                                color = DogFluffWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (skyRunningDetection)
                                    "Active: floating on-screen bar ($overlayPosition) while playing Sky"
                                else
                                    "Floating countdown & progress bar at the bottom of your screen while playing Sky",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (skyRunningDetection) Color(0xFF4ADE80) else MutedSlate
                            )
                        }
                        Switch(
                            checked = skyRunningDetection,
                            onCheckedChange = { isChecked ->
                                performHaptic()
                                if (isChecked) {
                                    if (!android.provider.Settings.canDrawOverlays(context)) {
                                        showOverlayPermissionDialog = true
                                    } else {
                                        skyRunningDetection = true
                                        prefs.skyRunningEnabled = true
                                        com.lumanest.service.FloatingCountdownService.start(context)

                                        // Check if Usage Access is granted; if not, open Usage Access screen
                                        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
                                        val hasUsage = if (appOps != null) {
                                            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                appOps.unsafeCheckOpNoThrow(
                                                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                                                    android.os.Process.myUid(),
                                                    context.packageName
                                                )
                                            } else {
                                                @Suppress("DEPRECATION")
                                                appOps.checkOpNoThrow(
                                                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                                                    android.os.Process.myUid(),
                                                    context.packageName
                                                )
                                            }
                                            mode == android.app.AppOpsManager.MODE_ALLOWED
                                        } else false

                                        if (!hasUsage) {
                                            showUsageAccessDialog = true
                                        }
                                    }
                                } else {
                                    skyRunningDetection = false
                                    prefs.skyRunningEnabled = false
                                    com.lumanest.service.FloatingCountdownService.stop(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepMidnight,
                                checkedTrackColor = CozyWarmAmber,
                                uncheckedThumbColor = MutedSlate,
                                uncheckedTrackColor = NightSurfaceVariant
                            )
                        )
                    }

                    if (skyRunningDetection) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = NightSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "ON-SCREEN STYLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = CozyWarmAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "TOP" to "🕊️ Wing Arc",
                                "EMOJI" to "⏱️ Emoji Timers",
                                "BOTTOM" to "📏 Bottom"
                            ).forEach { (posKey, label) ->
                                val isSelected = (overlayPosition == posKey)
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) CozyWarmAmber else NightSurfaceVariant,
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                    onClick = {
                                        performHaptic()
                                        overlayPosition = posKey
                                        prefs.overlayPosition = posKey
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) DeepMidnight else DogFluffWhite,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = NightSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "PRE-EVENT COUNTDOWN DURATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = CozyWarmAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Clean uniform row with equal proportions: 5m, 2m, 1m, 30s
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                300 to "5 min",
                                120 to "2 min",
                                60 to "1 min",
                                30 to "30 s"
                            ).forEach { (seconds, label) ->
                                val isSelected = (countdownSeconds == seconds)
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) CozyWarmAmber else NightSurfaceVariant,
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                    onClick = {
                                        performHaptic()
                                        countdownSeconds = seconds
                                        prefs.skyRunningSeconds = seconds
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected) DeepMidnight else DogFluffWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Usage Access (Game Detection) status row
                        val hasUsageStats = remember {
                            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
                            if (appOps != null) {
                                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    appOps.unsafeCheckOpNoThrow(
                                        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                                        android.os.Process.myUid(),
                                        context.packageName
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    appOps.checkOpNoThrow(
                                        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                                        android.os.Process.myUid(),
                                        context.packageName
                                    )
                                }
                                mode == android.app.AppOpsManager.MODE_ALLOWED
                            } else false
                        }

                        if (!hasUsageStats) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFEA580C).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEA580C).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    try {
                                        context.startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        })
                                    } catch (_: Exception) {}
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🛡️", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Hide Outside of Sky (Usage Access)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFFFB923C),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Tap to grant access so the bar auto-hides when using other apps.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DogFluffWhite.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        if (overlayPosition == "TOP") {
                            Button(
                                onClick = {
                                    if (!android.provider.Settings.canDrawOverlays(context)) {
                                        showOverlayPermissionDialog = true
                                    } else {
                                        // Launch or switch to Sky so the player aligns directly over their wing meter
                                        val skyIntent = context.packageManager.getLaunchIntentForPackage("com.tgc.sky.android")
                                        if (skyIntent != null) {
                                            skyIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            try {
                                                context.startActivity(skyIntent)
                                            } catch (_: Exception) {}
                                        }
                                        com.lumanest.service.FloatingCountdownService.triggerCustomize(context)
                                        android.widget.Toast.makeText(context, "Drag Up/Down to align with wing meter, then tap [Save & Set]!", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CozyWarmAmber)
                            ) {
                                Text(
                                    text = "🎯 Adjust & Align with Wing Meter",
                                    color = DeepMidnight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (overlayPosition == "EMOJI") {
                            OutlinedButton(
                                onClick = {
                                    if (!android.provider.Settings.canDrawOverlays(context)) {
                                        showOverlayPermissionDialog = true
                                    } else {
                                        com.lumanest.service.FloatingCountdownService.triggerPreview(context)
                                        android.widget.Toast.makeText(context, "Showing 10-second emoji timer test!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "👀 Preview Emoji Timers (10s Test)", color = CozyWarmAmber, fontWeight = FontWeight.Bold)
                            }
                        } else if (overlayPosition == "BOTTOM") {
                            OutlinedButton(
                                onClick = {
                                    if (!android.provider.Settings.canDrawOverlays(context)) {
                                        showOverlayPermissionDialog = true
                                    } else {
                                        com.lumanest.service.FloatingCountdownService.triggerPreview(context)
                                        android.widget.Toast.makeText(context, "Showing 10-second test bar!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = "👀 Preview In-Game Bar (10s Test)", color = CozyWarmAmber, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4. Sanctuary Ambiance Card (Ambient visuals with smart overlay auto-pause)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "🕯️ SANCTUARY AMBIANCE",
                        style = MaterialTheme.typography.labelMedium,
                        color = CozyWarmAmber,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Off by default to maximize battery saver. Select Starlight Drift to enjoy gentle luminous crystal shards flowing across your screens.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Effect Selector Row (extensible for future effects)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NightSurfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isOff = sanctuaryAmbianceEffect == "OFF"
                        val isStarlight = sanctuaryAmbianceEffect == "STARLIGHT_DRIFT" || sanctuaryAmbianceEffect == "AERO_SHARDS"

                        // Off Option
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    performHaptic()
                                    sanctuaryAmbianceEffect = "OFF"
                                    prefs.sanctuaryAmbianceEffect = "OFF"
                                },
                            color = if (isOff) CozyWarmAmber else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "❌ Off",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isOff) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isOff) DeepMidnight else MutedSlate
                                )
                            }
                        }

                        // Starlight Drift Option
                        Surface(
                            modifier = Modifier
                                .weight(1.3f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    performHaptic()
                                    sanctuaryAmbianceEffect = "STARLIGHT_DRIFT"
                                    prefs.sanctuaryAmbianceEffect = "STARLIGHT_DRIFT"
                                },
                            color = if (isStarlight) CozyWarmAmber else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✨ Starlight Drift",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isStarlight) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isStarlight) DeepMidnight else MutedSlate
                                )
                            }
                        }
                    }

                    if (sanctuaryAmbianceEffect == "STARLIGHT_DRIFT" || sanctuaryAmbianceEffect == "AERO_SHARDS") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✨ Active: Luminous crystal shards floating peacefully across Home, Calendar, and Settings screens. Automatically freezes when the app is minimized or screen is locked (0% battery drain).",
                            style = MaterialTheme.typography.bodySmall,
                            color = StarlightBlue,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 5. In-App GitHub Updater Card
        item {
            var updateInfo by remember { mutableStateOf<com.lumanest.updater.AppUpdateInfo?>(null) }
            var isChecking by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(currentVersionName) {
                updateInfo = com.lumanest.updater.AppUpdater.checkForUpdate(currentVersionName)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (updateInfo?.hasUpdate == true) Color(0xFFEA580C) else CardBorder
                    )
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "App Updates",
                                style = MaterialTheme.typography.titleMedium,
                                color = DogFluffWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Current version: $currentVersionName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlate
                            )
                        }

                        if (updateInfo?.hasUpdate != true) {
                            OutlinedButton(
                                onClick = {
                                    isChecking = true
                                    coroutineScope.launch {
                                        updateInfo = com.lumanest.updater.AppUpdater.checkForUpdate(currentVersionName)
                                        isChecking = false
                                        if (updateInfo?.hasUpdate != true) {
                                            android.widget.Toast.makeText(context, "LumaNest is up to date! ✨", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(text = if (isChecking) "Checking..." else "Check", color = CozyWarmAmber)
                            }
                        }
                    }

                    if (updateInfo?.hasUpdate == true) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF431407), // Cozy deep amber container
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEA580C))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                val cleanLatest = updateInfo?.latestVersionName?.trimStart('v', 'V', ' ') ?: ""
                                Text(
                                    text = "✨ New Update Available: V.$cleanLatest",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFFFB923C),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = updateInfo?.releaseNotes ?: "Bug fixes and improvements",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DogFluffWhite
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Bright Orange Update Button
                                Button(
                                    onClick = {
                                        val apkUrl = updateInfo?.downloadUrl ?: ""
                                        if (apkUrl.isNotBlank()) {
                                            android.widget.Toast.makeText(context, "Starting download...", android.widget.Toast.LENGTH_SHORT).show()
                                            com.lumanest.updater.AppUpdater.downloadAndInstallApk(context, apkUrl)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFEA580C) // Bright Orange
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "📥 Download & Update Now",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Expandable About LumaNest & Community Support
        item {
            var isAboutExpanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NightSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // About Header (Always open, clean and visible)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.lumanest.app.R.drawable.ic_app_logo),
                            contentDescription = "LumaNest Logo",
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "About LumaNest",
                            style = MaterialTheme.typography.titleMedium,
                            color = DogFluffWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "LumaNest is an unofficial companion app made for Sky: Children of the Light players.\n\nTrack events, check timezones, stay connected with friends, and never miss an important event.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DogFluffWhite,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = NightSurfaceVariant)
                    Spacer(modifier = Modifier.height(18.dp))

                    // 🌻 Support LumaNest Section
                    Text(
                        text = "🌻 Support LumaNest",
                        style = MaterialTheme.typography.titleMedium,
                        color = DogFluffWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Enjoying LumaNest?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSlate
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://buymeacoffee.com/kakdamba")
                            )
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                android.widget.Toast.makeText(context, "Thank you for supporting LumaNest! 💛", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CozyWarmAmber),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🥣 Buy me a Kheer",
                            color = DeepMidnight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your support helps LumaNest stay free and grow. 🧡",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = NightSurfaceVariant)
                    Spacer(modifier = Modifier.height(18.dp))

                    // 🔗 Links Section
                    Text(
                        text = "🔗 Links",
                        style = MaterialTheme.typography.titleMedium,
                        color = DogFluffWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Website
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://www.youtube.com/@kakdambaa?sub_confirmation=1")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🌐 Website", style = MaterialTheme.typography.bodyMedium, color = StarlightBlue)
                        Text(text = "→", color = MutedSlate)
                    }

                    // 2. Privacy Policy (In-App Window)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPrivacyPolicy() }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🛡️ Privacy Policy", style = MaterialTheme.typography.bodyMedium, color = StarlightBlue)
                        Text(text = "→", color = MutedSlate)
                    }

                    // 3. Source Code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://github.com/kakdamba/LumaNest-Sky-clock-and-calendar")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💻 Source Code", style = MaterialTheme.typography.bodyMedium, color = StarlightBlue)
                        Text(text = "→", color = MutedSlate)
                    }

                    // 4. Report a Problem (Opens Gmail / Email directly)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                        data = android.net.Uri.parse("mailto:yin-fade-clamshell@duck.com")
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "[$displayVersion] Bug Report / Issue")
                                        putExtra(android.content.Intent.EXTRA_TEXT, "Hello,\n\nI encountered the following issue in $displayVersion:\n\n[Please describe the issue here]\n\nDevice info: Android")
                                    }
                                    context.startActivity(android.content.Intent.createChooser(emailIntent, "Send Bug Report"))
                                } catch (_: Exception) {
                                    android.widget.Toast.makeText(context, "No email client found", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🐛 Report a Problem", style = MaterialTheme.typography.bodyMedium, color = StarlightBlue)
                        Text(text = "→", color = MutedSlate)
                    }

                    // 5. Suggest a Feature (Opens Gmail / Email directly)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                        data = android.net.Uri.parse("mailto:yin-fade-clamshell@duck.com")
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "[$displayVersion] Feature Suggestion")
                                        putExtra(android.content.Intent.EXTRA_TEXT, "Hello,\n\nI would like to suggest a feature for $displayVersion:\n\n[Describe your idea here]")
                                    }
                                    context.startActivity(android.content.Intent.createChooser(emailIntent, "Send Feature Suggestion"))
                                } catch (_: Exception) {
                                    android.widget.Toast.makeText(context, "No email client found", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💡 Suggest a Feature", style = MaterialTheme.typography.bodyMedium, color = StarlightBlue)
                        Text(text = "→", color = MutedSlate)
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = NightSurfaceVariant)
                    Spacer(modifier = Modifier.height(18.dp))

                    // ⚠️ Disclaimer Section
                    Text(
                        text = "⚠️ Disclaimer",
                        style = MaterialTheme.typography.titleSmall,
                        color = DogFluffWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "LumaNest is an unofficial fan-made application and is not affiliated with, sponsored by, or endorsed by thatgamecompany.\n\nSky: Children of the Light and related trademarks belong to their respective owners.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = NightSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Footer
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = displayVersion,
                            style = MaterialTheme.typography.labelMedium,
                            color = CozyWarmAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Made with 🍊 and 🌻",
                            style = MaterialTheme.typography.bodySmall,
                            color = DogFluffWhite
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Built by KAKDAMBA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSlate,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    }

    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showOverlayPermissionDialog = false },
            title = {
                Text(text = "Display Over Other Apps", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Sky Running Mode requires 'Display over other apps' permission so it can show the live countdown loading bar while you are playing Sky.\n\nPlease tap 'Grant Permission' and turn on the switch for LumaNest.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverlayPermissionDialog = false
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}")
                        ).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CozyWarmAmber)
                ) {
                    Text(text = "Grant Permission", color = DeepMidnight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayPermissionDialog = false }) {
                    Text(text = "Not Now", color = MutedSlate)
                }
            }
        )
    }

    if (showUsageAccessDialog) {
        AlertDialog(
            onDismissRequest = { showUsageAccessDialog = false },
            title = {
                Text(text = "🛡️ Hide Outside of Sky", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "To ensure the bottom progress bar only shows inside the Sky game (and automatically hides on your Home screen or in other apps), Android requires 'Usage Access'.\n\nPlease tap 'Enable' and grant access to LumaNest.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUsageAccessDialog = false
                        try {
                            context.startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        } catch (_: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CozyWarmAmber)
                ) {
                    Text(text = "Enable", color = DeepMidnight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsageAccessDialog = false }) {
                    Text(text = "Later", color = MutedSlate)
                }
            }
        )
    }
}

@Composable
fun StatusRow(label: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MutedSlate)
        Text(text = status, style = MaterialTheme.typography.bodySmall, color = DogFluffWhite, fontWeight = FontWeight.Medium)
    }
}
