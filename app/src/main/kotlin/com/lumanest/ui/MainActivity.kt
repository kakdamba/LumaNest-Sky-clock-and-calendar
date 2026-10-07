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

        setContent {
            LumaNestTheme {
                MainAppScaffold(repository = repository)
            }
        }
    }
}

@Composable
fun MainAppScaffold(repository: EventRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    var userZoneId by remember { mutableStateOf(TimezoneHelper.getDeviceZoneId()) }
    var is24Hour by remember { mutableStateOf(false) } // Default 12h format with AM/PM

    val allEvents by repository.allEvents.collectAsState(initial = emptyList())
    val friends by repository.allFriends.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = NightSurface) {
                NavigationBarItem(
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = currentRoute == "friends",
                    onClick = { navController.navigate("friends") },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Friends") },
                    label = { Text("Friends") }
                )
                NavigationBarItem(
                    selected = currentRoute == "calendar",
                    onClick = { navController.navigate("calendar") },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar") },
                    label = { Text("Calendar") }
                )
                NavigationBarItem(
                    selected = currentRoute == "settings",
                    onClick = { navController.navigate("settings") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
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
