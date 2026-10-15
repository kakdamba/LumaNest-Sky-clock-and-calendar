package com.lumanest.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lumanest.data.local.LumaNestDatabase
import com.lumanest.data.repository.EventRepository
import com.lumanest.timezone.TimezoneHelper
import com.lumanest.ui.calendar.CalendarScreen
import com.lumanest.ui.friends.FriendsScreen
import com.lumanest.ui.home.HomeScreen
import com.lumanest.ui.settings.PrivacyPolicyScreen
import com.lumanest.ui.settings.SettingsScreen
import com.lumanest.ui.theme.DeepMidnight
import com.lumanest.ui.theme.LumaNestTheme
import com.lumanest.ui.theme.NightSurface
import kotlinx.coroutines.launch
import java.time.ZoneId

class MainActivity : ComponentActivity() {

    private lateinit var repository: EventRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = LumaNestDatabase.getInstance(this)
        repository = EventRepository(this, database.eventDao(), database.friendDao())

        lifecycleScope.launch {
            repository.initializeSeedDataIfNeeded()
        }

        // Auto-start Sky Running Mode service if user previously enabled it and has overlay permission
        val prefs = com.lumanest.data.local.PreferencesManager(this)
        if (prefs.skyRunningEnabled && android.provider.Settings.canDrawOverlays(this)) {
            com.lumanest.service.FloatingCountdownService.start(this)
        }

        // Initialize notification channel and schedule alarms if enabled
        if (prefs.notificationsEnabled) {
            com.lumanest.notification.EventNotificationReceiver.scheduleNextAlarms(this)
        }

        // Request POST_NOTIFICATIONS permission on Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        setContent {
            LumaNestTheme {
                MainAppScaffold(repository = repository)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Silent OTA Event Sync from remote GitHub feed whenever app opens or resumes
        if (::repository.isInitialized) {
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                repository.syncRemoteEvents()
            }
        }
        com.lumanest.widget.WidgetSyncManager.refreshWidgets(this)
        com.lumanest.widget.WidgetSyncManager.scheduleNextTransition(this)
    }

    override fun onStop() {
        super.onStop()
        com.lumanest.widget.WidgetSyncManager.refreshWidgets(this)
        com.lumanest.widget.WidgetSyncManager.scheduleNextTransition(this)
    }
}

@Composable
fun MainAppScaffold(repository: EventRepository) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    var userZoneId by remember { mutableStateOf(TimezoneHelper.getDeviceZoneId()) }
    var is24Hour by remember { mutableStateOf(false) } // Default 12h format with AM/PM

    val allEvents by repository.allEvents.collectAsState(initial = emptyList())
    val friends by repository.allFriends.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    var hasUpdateAvailable by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val currentVersion = com.lumanest.updater.AppUpdater.getInstalledVersionName(context)
            val updateInfo = com.lumanest.updater.AppUpdater.checkForUpdate(currentVersion)
            hasUpdateAvailable = updateInfo.hasUpdate
        } catch (_: Exception) {}
    }

    Scaffold(
        bottomBar = {
            com.lumanest.ui.components.MacDockNavigationBar(
                currentRoute = currentRoute,
                hasUpdateAvailable = hasUpdateAvailable,
                onTabSelected = { route ->
                    if (route == "home") {
                        navController.navigate("home") { popUpTo("home") { inclusive = true } }
                    } else {
                        navController.navigate(route)
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    calendarEvents = allEvents,
                    userZoneId = userZoneId,
                    is24Hour = is24Hour,
                    onToggleFavorite = { id, fav ->
                        coroutineScope.launch { repository.toggleFavorite(id, fav) }
                    }
                )
            }
            composable("friends") {
                FriendsScreen(
                    friends = friends,
                    userZoneId = userZoneId,
                    is24Hour = is24Hour,
                    onAddFriend = { name, tz, flag ->
                        coroutineScope.launch { repository.addFriend(name, tz, flag) }
                    },
                    onRemoveFriend = { id ->
                        coroutineScope.launch { repository.removeFriend(id) }
                    },
                    onReorderFriends = { reordered ->
                        coroutineScope.launch { repository.updateFriendsOrder(reordered) }
                    },
                    onUpdateFriend = { id, name, tz, flag ->
                        coroutineScope.launch { repository.updateFriend(id, name, tz, flag) }
                    }
                )
            }
            composable("calendar") {
                CalendarScreen(
                    events = allEvents,
                    userZoneId = userZoneId,
                    is24Hour = is24Hour
                )
            }
            composable("settings") {
                SettingsScreen(
                    currentZoneId = userZoneId,
                    is24Hour = is24Hour,
                    onTimeFormatChanged = { is24Hour = it },
                    onZoneIdChanged = { userZoneId = it },
                    onNavigateToPrivacyPolicy = { navController.navigate("privacy_policy") }
                )
            }
            composable("privacy_policy") {
                PrivacyPolicyScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
