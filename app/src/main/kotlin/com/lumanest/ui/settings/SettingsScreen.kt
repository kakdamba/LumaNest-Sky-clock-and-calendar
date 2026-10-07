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
    val prefs = remember { com.lumanest.data.local.PreferencesManager(context) }

    var quietHoursEnabled by remember { mutableStateOf(prefs.quietHoursEnabled) }
    var startHour by remember { mutableStateOf(prefs.quietStartHour) }
    var startMinute by remember { mutableStateOf(prefs.quietStartMinute) }
    var endHour by remember { mutableStateOf(prefs.quietEndHour) }
    var endMinute by remember { mutableStateOf(prefs.quietEndMinute) }

    var skyRunningDetection by remember { mutableStateOf(prefs.skyRunningEnabled) }
    var countdownSeconds by remember { mutableStateOf(prefs.skyRunningSeconds) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var showUsageAccessDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), DeepMidnight, Color(0xFF090D14))
                )
            )
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
                            onCheckedChange = { onTimeFormatChanged(!it) },
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

        // 2. Quiet Hours Settings (Customizable with native TimePickerDialog)
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

        // 3. Sky Running Mode (Interactive with uniform 30s, 20s, 10s selector)
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
                                text = "Sky Running Mode",
                                style = MaterialTheme.typography.titleMedium,
                                color = DogFluffWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (skyRunningDetection)
                                    "Active: alerts when Sky starts with final ${countdownSeconds}s banner"
                                else
                                    "Companion audio/haptic countdown before event starts",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (skyRunningDetection) Color(0xFF4ADE80) else MutedSlate
                            )
                        }
                        Switch(
                            checked = skyRunningDetection,
                            onCheckedChange = { isChecked ->
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

                        // Preview In-Game Bar Button
                        OutlinedButton(
                            onClick = {
                                if (!android.provider.Settings.canDrawOverlays(context)) {
                                    showOverlayPermissionDialog = true
                                } else {
                                    com.lumanest.service.FloatingCountdownService.triggerPreview(context)
                                    android.widget.Toast.makeText(context, "Showing 10-second test bar at bottom of screen!", android.widget.Toast.LENGTH_LONG).show()
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

        // 4. In-App GitHub Updater Card
        item {
            var updateInfo by remember { mutableStateOf<com.lumanest.updater.AppUpdateInfo?>(null) }
            var isChecking by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                updateInfo = com.lumanest.updater.AppUpdater.checkForUpdate("V.2")
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
                                text = "Current version: V.2",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlate
                            )
                        }

                        if (updateInfo?.hasUpdate != true) {
                            OutlinedButton(
                                onClick = {
                                    isChecking = true
                                    coroutineScope.launch {
                                        updateInfo = com.lumanest.updater.AppUpdater.checkForUpdate("1.0.0")
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
                                Text(
                                    text = "✨ New Update Available: v${updateInfo?.latestVersionName}",
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

                    // 3. Source Code (Ready for repository link)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "💻 Source Code", style = MaterialTheme.typography.bodyMedium, color = MutedSlate)
                        Text(text = "Coming Soon", style = MaterialTheme.typography.labelSmall, color = MutedSlate)
                    }

                    // 4. Report a Problem (Opens Gmail / Email directly)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                        data = android.net.Uri.parse("mailto:yin-fade-clamshell@duck.com")
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "[LumaNest V.2] Bug Report / Issue")
                                        putExtra(android.content.Intent.EXTRA_TEXT, "Hello,\n\nI encountered the following issue in LumaNest V.2:\n\n[Please describe the issue here]\n\nDevice info: Android")
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
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "[LumaNest V.2] Feature Suggestion")
                                        putExtra(android.content.Intent.EXTRA_TEXT, "Hello,\n\nI would like to suggest a feature for LumaNest V.2:\n\n[Describe your idea here]")
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
                            text = "LumaNest V.2",
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
