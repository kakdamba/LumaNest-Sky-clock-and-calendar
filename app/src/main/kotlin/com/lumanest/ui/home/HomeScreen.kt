package com.lumanest.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.lumanest.eventengine.SkyEventEngine
import com.lumanest.models.EventOccurrence
import com.lumanest.models.Friend
import com.lumanest.timezone.TimezoneHelper
import com.lumanest.ui.theme.*
import kotlinx.coroutines.delay
import java.time.ZoneId


@Composable
fun HomeScreen(
    calendarEvents: List<com.lumanest.models.SkyEvent> = emptyList(),
    userZoneId: ZoneId,
    is24Hour: Boolean,
    onToggleFavorite: (String, Boolean) -> Unit
) {
    // Lifecycle-aware 1-second countdown ticker.
    // Uses repeatOnLifecycle(STARTED) so it automatically resumes whenever the app
    // comes back to foreground — fixing the disappearing progress bar bug.
    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                currentMillis = System.currentTimeMillis()
                delay(1000)
            }
        }
    }


    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { com.lumanest.data.local.PreferencesManager(context) }

    // Sanctuary Ambiance background effect state (refreshes on resume)
    var sanctuaryAmbianceEffect by remember { mutableStateOf(prefs.sanctuaryAmbianceEffect) }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                sanctuaryAmbianceEffect = prefs.sanctuaryAmbianceEffect
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Customizable Event Visibility State (Persisted in SharedPreferences)
    var showGeyser by remember { mutableStateOf(prefs.filterGeyser) }
    var showGrandma by remember { mutableStateOf(prefs.filterGrandma) }
    var showTurtle by remember { mutableStateOf(prefs.filterTurtle) }
    var showShards by remember { mutableStateOf(prefs.filterShards) }
    var showReset by remember { mutableStateOf(prefs.filterReset) }
    var showTsReveal by remember { mutableStateOf(prefs.filterTsReveal) }
    var showCustomizeSheet by remember { mutableStateOf(false) }

    val rawDailyOccurrences = remember(currentMillis) {
        SkyEventEngine.calculateDailyOccurrences(currentMillis)
    }

    val extraOccurrences = remember(currentMillis, calendarEvents) {
        val staticExtras = overlayTravelingSpiritNames(
            SkyEventEngine.calculateExtraActivities(currentMillis),
            calendarEvents
        )
        // Also include active or upcoming Double Seasonal Light & Double Treasure Candles from calendar database
        val doubleCurrencyExtras = calendarEvents.filter {
            it.category == com.lumanest.models.EventCategory.DOUBLE_CURRENCY
        }.mapNotNull { event ->
            try {
                val start = java.time.Instant.parse(event.startIso).toEpochMilli()
                val end = java.time.Instant.parse(event.endIso).toEpochMilli()
                if (currentMillis < end) {
                    val isActive = currentMillis in start..end
                    com.lumanest.models.EventOccurrence(
                        event = event,
                        startEpochMillis = start,
                        endEpochMillis = end,
                        isActiveNow = isActive,
                        millisUntilStart = if (isActive) 0L else (start - currentMillis).coerceAtLeast(0L),
                        millisRemaining = if (isActive) (end - currentMillis).coerceAtLeast(0L) else 0L
                    )
                } else null
            } catch (_: Exception) {
                null
            }
        }
        (staticExtras + doubleCurrencyExtras).sortedWith(
            compareByDescending<EventOccurrence> { it.isActiveNow }
                .thenBy { it.millisUntilStart }
        )
    }
    var isExtraActivitiesExpanded by remember { mutableStateOf(false) }

    // Filter occurrences based on user preferences and sort chronologically (active first, then closest start)
    val tsNamedOccurrences = remember(rawDailyOccurrences, calendarEvents) {
        overlayTravelingSpiritNames(rawDailyOccurrences, calendarEvents)
    }
    val dailyOccurrences = remember(tsNamedOccurrences, showGeyser, showGrandma, showTurtle, showShards, showReset, showTsReveal) {
        tsNamedOccurrences
            .filter { occ ->
                when (occ.event.id) {
                    "daily_geyser" -> showGeyser
                    "daily_grandma" -> showGrandma
                    "daily_turtle" -> showTurtle
                    "daily_shard_red", "daily_shard_black" -> showShards
                    "daily_reset" -> showReset
                    "daily_ts_reveal" -> showTsReveal && (occ.isActiveNow || occ.millisUntilStart <= 24 * 3600 * 1000L)
                    "extra_traveling_spirit" -> occ.isActiveNow || occ.millisUntilStart <= 48 * 3600 * 1000L
                    else -> true
                }
            }
            .sortedWith(
                compareByDescending<EventOccurrence> { it.isActiveNow }
                    .thenBy { it.millisUntilStart }
            )
    }

    // Determine current active event or next upcoming event
    // User rule: Traveling Spirit is shown in the top Big Hero Card for the first 30 minutes of arrival.
    // Afterwards, it steps aside so regular wax/shard events get the Hero spotlight, while TS stays in Today's Schedule.
    val activeEvent = remember(dailyOccurrences, currentMillis) {
        dailyOccurrences.firstOrNull { occ ->
            if (!occ.isActiveNow) return@firstOrNull false
            if (occ.event.id == "extra_traveling_spirit") {
                val elapsedSinceArrival = currentMillis - occ.startEpochMillis
                elapsedSinceArrival in 0..(30 * 60 * 1000L)
            } else {
                true
            }
        }
    }
    val nextEvent = remember(dailyOccurrences, activeEvent) {
        val upcomingOnly = dailyOccurrences.filter { !it.isActiveNow }
        if (activeEvent != null) {
            upcomingOnly.minByOrNull { it.millisUntilStart }
        } else {
            val regularUpcoming = upcomingOnly.filter { it.event.id != "extra_traveling_spirit" }
            regularUpcoming.minByOrNull { it.millisUntilStart } ?: upcomingOnly.minByOrNull { it.millisUntilStart }
        }
    }

    // Top 3 Active & Upcoming Calendar Events (excluding Shard eruptions and Double Currency bonuses)
    val topUpcomingCalendarEvents = remember(calendarEvents) {
        val now = System.currentTimeMillis()
        calendarEvents
            .filter { e ->
                e.category != com.lumanest.models.EventCategory.SHARD_ERUPTION &&
                e.category != com.lumanest.models.EventCategory.DOUBLE_CURRENCY &&
                try { java.time.Instant.parse(e.endIso).toEpochMilli() >= now } catch (_: Exception) { false }
            }
            .sortedWith(
                compareBy<com.lumanest.models.SkyEvent> { e ->
                    val startEpoch = try { java.time.Instant.parse(e.startIso).toEpochMilli() } catch (_: Exception) { 0L }
                    val endEpoch = try { java.time.Instant.parse(e.endIso).toEpochMilli() } catch (_: Exception) { Long.MAX_VALUE }
                    val isActive = now in startEpoch..endEpoch
                    if (isActive) 0 else 1
                }.thenBy { e ->
                    try { java.time.Instant.parse(e.startIso).toEpochMilli() } catch (_: Exception) { Long.MAX_VALUE }
                }
            )
            .take(3)
    }

    val showAeroShards = sanctuaryAmbianceEffect == "STARLIGHT_DRIFT" || sanctuaryAmbianceEffect == "AERO_SHARDS"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), DeepMidnight, Color(0xFF090D14))
                )
            )
    ) {
        if (showAeroShards) {
            com.lumanest.ui.components.AeroShardsBackground()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // 1. Top App Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            TopHeaderBar(userZoneId = userZoneId, is24Hour = is24Hour)
        }

        // 2. Current Event Showcase Card
        item {
            val heroOccurrence = activeEvent ?: nextEvent
            if (heroOccurrence != null) {
                HeroEventCard(
                    occurrence = heroOccurrence,
                    userZoneId = userZoneId,
                    is24Hour = is24Hour,
                    onToggleFavorite = onToggleFavorite
                )
            }
        }

        // 3. Next Event (if there's an active one, show next event card)
        if (activeEvent != null && nextEvent != null) {
            item {
                NextEventPreviewCard(
                    occurrence = nextEvent,
                    userZoneId = userZoneId,
                    is24Hour = is24Hour
                )
            }
        }

        // 4. Today's Schedule Section Header with Customize Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Today's Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        color = DogFluffWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${dailyOccurrences.size} events tracked",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NightSurfaceVariant,
                    onClick = { showCustomizeSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⚙️", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Filter",
                            style = MaterialTheme.typography.labelSmall,
                            color = CozyWarmAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (dailyOccurrences.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NightSurface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "👀", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "All events hidden via filter.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSlate
                        )
                        TextButton(onClick = {
                            showGeyser = true
                            showGrandma = true
                            showTurtle = true
                            showShards = true
                            showReset = true
                            showTsReveal = true
                            prefs.filterGeyser = true
                            prefs.filterGrandma = true
                            prefs.filterTurtle = true
                            prefs.filterShards = true
                            prefs.filterReset = true
                            prefs.filterTsReveal = true
                        }) {
                            Text("Reset Filters", color = CozyWarmAmber)
                        }
                    }
                }
            }
        } else {
            items(dailyOccurrences, key = { it.event.id }) { item ->
                ScheduleRowCard(
                    occurrence = item,
                    userZoneId = userZoneId,
                    is24Hour = is24Hour
                )
            }
        }

        // 4b. Collapsible More Activities & Shows
        item {
            ExtraActivitiesDropdownCard(
                occurrences = extraOccurrences,
                isExpanded = isExtraActivitiesExpanded,
                onToggleExpand = { isExtraActivitiesExpanded = !isExtraActivitiesExpanded },
                userZoneId = userZoneId,
                is24Hour = is24Hour
            )
        }

        // 5. Top 3 Upcoming Calendar Events (Replaces Play Together)
        if (topUpcomingCalendarEvents.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upcoming Calendar Events",
                        style = MaterialTheme.typography.titleMedium,
                        color = DogFluffWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Top 3",
                        style = MaterialTheme.typography.labelSmall,
                        color = CozyWarmAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            items(topUpcomingCalendarEvents, key = { it.id }) { calEvent ->
                // Read the ticker state so countdowns / ACTIVE NOW badges refresh
                // every second (they used to freeze until an unrelated recomposition).
                val nowMillis = currentMillis
                val startEpoch = try { java.time.Instant.parse(calEvent.startIso).toEpochMilli() } catch (_: Exception) { 0L }
                val endEpoch = try { java.time.Instant.parse(calEvent.endIso).toEpochMilli() } catch (_: Exception) { Long.MAX_VALUE }
                val isCurrentlyActive = nowMillis in startEpoch..endEpoch

                val startDateStr = try {
                    TimezoneHelper.formatEpochDateForZone(startEpoch, userZoneId)
                } catch (_: Exception) { calEvent.startIso }
                val endDateStr = try {
                    TimezoneHelper.formatEpochDateForZone(endEpoch, userZoneId)
                } catch (_: Exception) { calEvent.endIso }

                val (tagTextColor, tagBgColor) = when {
                    isCurrentlyActive -> Color.White to Color(0xFF16A34A)
                    calEvent.category == com.lumanest.models.EventCategory.SEASON -> Color.White to Color(0xFFEA580C)
                    calEvent.category == com.lumanest.models.EventCategory.DOUBLE_CURRENCY -> Color.White to Color(0xFFDC2626)
                    calEvent.category == com.lumanest.models.EventCategory.TRAVELING_SPIRIT -> Color.White to Color(0xFF0284C7)
                    calEvent.category == com.lumanest.models.EventCategory.MAJOR_EVENT -> Color.White to Color(0xFF9333EA)
                    calEvent.category == com.lumanest.models.EventCategory.SHARD_ERUPTION -> Color.White to Color(0xFFE11D48)
                    calEvent.category == com.lumanest.models.EventCategory.MAINTENANCE -> Color.White to Color(0xFFD97706)
                    else -> Color.White to StarlightBlue
                }

                val artDrawableRes = when {
                    calEvent.id.contains("returning_spirit", ignoreCase = true) || calEvent.name.contains("Returning", ignoreCase = true) ->
                        com.lumanest.app.R.drawable.art_returning_spirit_group_nov
                    calEvent.id.contains("dancing_performer", ignoreCase = true) || calEvent.name.contains("Dancing Performer", ignoreCase = true) ->
                        com.lumanest.app.R.drawable.art_dancing_performer
                    calEvent.id.contains("pearl", ignoreCase = true) || calEvent.name.contains("Pearl", ignoreCase = true) ->
                        com.lumanest.app.R.drawable.art_season_pearl
                    calEvent.id.contains("mischief", ignoreCase = true) || calEvent.name.contains("Mischief", ignoreCase = true) ->
                        com.lumanest.app.R.drawable.art_days_mischief
                    calEvent.id.contains("double_hearts", ignoreCase = true) || calEvent.category == com.lumanest.models.EventCategory.DOUBLE_CURRENCY ->
                        com.lumanest.app.R.drawable.art_double_candles
                    calEvent.category == com.lumanest.models.EventCategory.TRAVELING_SPIRIT ->
                        com.lumanest.app.R.drawable.art_traveling_spirit
                    else -> null
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NightSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(if (isCurrentlyActive) Color(0xFF16A34A).copy(alpha = 0.6f) else CardBorder)
                    )
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (artDrawableRes != null) {
                            // DEC-008: Visual Contract - locked at 1.dp blur + 0.85 alpha + 94%->65%->20% scrim
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = artDrawableRes),
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .blur(radius = 1.dp)
                                    .graphicsLayer { alpha = 0.85f },
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                NightSurface.copy(alpha = 0.94f),
                                                NightSurface.copy(alpha = 0.65f),
                                                NightSurface.copy(alpha = 0.20f)
                                            )
                                        )
                                    )
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = tagBgColor
                            ) {
                                Text(
                                    text = if (isCurrentlyActive) "● ACTIVE NOW" else calEvent.category.name.replace('_', ' '),
                                    color = tagTextColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = calEvent.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (calEvent.category == com.lumanest.models.EventCategory.SEASON) Color(0xFFFB923C) else DogFluffWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = if (isCurrentlyActive) {
                                        "Ends: $endDateStr (${formatExtraCountdown(endEpoch - nowMillis)} left)"
                                    } else {
                                        "Starts: $startDateStr (in ${formatExtraCountdown(startEpoch - nowMillis)})"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCurrentlyActive) Color(0xFF4ADE80) else MutedSlate
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    }

    // Modal Sheet for Customizing which events to see
    if (showCustomizeSheet) {
        AlertDialog(
            onDismissRequest = { showCustomizeSheet = false },
            containerColor = Color(0xFF1E293B),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Customize Home Events",
                    style = MaterialTheme.typography.titleMedium,
                    color = DogFluffWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose which recurring events appear in Today's Schedule:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    EventToggleRow(label = "🌋 Polluted Geyser", checked = showGeyser, onCheckedChange = {
                        showGeyser = it
                        prefs.filterGeyser = it
                    })
                    EventToggleRow(label = "👵 Grandma's Dinner", checked = showGrandma, onCheckedChange = {
                        showGrandma = it
                        prefs.filterGrandma = it
                    })
                    EventToggleRow(label = "🐢 Sanctuary Turtle", checked = showTurtle, onCheckedChange = {
                        showTurtle = it
                        prefs.filterTurtle = it
                    })
                    EventToggleRow(label = "☄️ Shard Eruptions", checked = showShards, onCheckedChange = {
                        showShards = it
                        prefs.filterShards = it
                    })
                    EventToggleRow(label = "🎭 Traveling Spirit Reveal", checked = showTsReveal, onCheckedChange = {
                        showTsReveal = it
                        prefs.filterTsReveal = it
                    })
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomizeSheet = false }) {
                    Text("Done", color = CozyWarmAmber, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun EventToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = DogFluffWhite, style = MaterialTheme.typography.bodyMedium)
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = CozyWarmAmber,
                checkmarkColor = DeepMidnight,
                uncheckedColor = MutedSlate
            )
        )
    }
}

@Composable
fun TopHeaderBar(userZoneId: ZoneId, is24Hour: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // LumaNest App Logo
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.lumanest.app.R.drawable.ic_app_logo),
                contentDescription = "LumaNest Logo",
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "LumaNest",
                    style = MaterialTheme.typography.titleLarge,
                    color = DogFluffWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your cozy companion for Sky",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate
                )
            }
        }

        // Time indicator pill (Option A: Clean two-line stack)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = NightSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = TimezoneHelper.getCurrentTimeFormatted(userZoneId, is24Hour),
                    style = MaterialTheme.typography.titleMedium,
                    color = CozyWarmAmber,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = userZoneId.id.substringAfterLast('/').replace('_', ' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun EventEmojiIcon(
    eventId: String,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    fallbackEmoji: String = "✨"
) {
    val shardSize = size * 1.8f
    when (eventId) {
        "daily_shard_red" -> {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.lumanest.app.R.drawable.emoji_shard_red),
                contentDescription = "Red Shard",
                modifier = modifier.size(shardSize)
            )
        }
        "daily_shard_black" -> {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.lumanest.app.R.drawable.emoji_shard_black),
                contentDescription = "Black Shard",
                modifier = modifier.size(shardSize)
            )
        }
        else -> {
            Text(
                text = fallbackEmoji,
                fontSize = (size.value * 0.95f).sp,
                modifier = modifier
            )
        }
    }
}

@Composable
fun HeroEventCard(
    occurrence: EventOccurrence,
    userZoneId: ZoneId,
    is24Hour: Boolean,
    onToggleFavorite: (String, Boolean) -> Unit
) {
    val isHappening = occurrence.isActiveNow
    val badgeColor = if (isHappening) Color(0xFF4ADE80) else CozyWarmAmber
    val iconEmoji = when (occurrence.event.id) {
        "daily_turtle" -> "🐢"
        "daily_geyser" -> "🌋"
        "daily_grandma" -> "👵"
        "daily_shard_red" -> "💎"
        "daily_shard_black" -> "🖤"
        "daily_ts_reveal" -> "🎭"
        else -> "✨"
    }

    val artDrawableRes = when (occurrence.event.id) {
        "daily_grandma" -> com.lumanest.app.R.drawable.art_grandma
        "daily_turtle" -> com.lumanest.app.R.drawable.art_turtle
        "daily_geyser" -> com.lumanest.app.R.drawable.art_geyser
        "daily_shard_red" -> com.lumanest.app.R.drawable.art_shard_red
        "daily_shard_black" -> com.lumanest.app.R.drawable.art_shard_black
        "daily_ts_reveal" -> com.lumanest.app.R.drawable.art_traveling_spirit
        "extra_traveling_spirit" -> com.lumanest.app.R.drawable.art_traveling_spirit
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = DeepMidnight
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                colors = listOf(badgeColor.copy(alpha = 0.8f), badgeColor.copy(alpha = 0.2f))
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
        ) {
            // Watercolor Illustration Backdrop - 100% full artwork filling the card
            if (artDrawableRes != null) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = artDrawableRes),
                    contentDescription = occurrence.event.name,
                    modifier = Modifier.matchParentSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    alignment = Alignment.Center
                )

                // Smooth gradient overlay spanning the card so bottom text is perfectly legible with zero harsh cutout line
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.25f),
                                0.35f to Color.Transparent,
                                0.60f to Color.Black.copy(alpha = 0.30f),
                                0.82f to Color.Black.copy(alpha = 0.65f),
                                1.0f to Color.Black.copy(alpha = 0.85f)
                            )
                        )
                )
            }

            val textShadow = androidx.compose.ui.graphics.Shadow(
                color = Color.Black.copy(alpha = 0.90f),
                offset = androidx.compose.ui.geometry.Offset(2f, 2f),
                blurRadius = 6f
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                // Header Row: Status Badge & Favorite (Shifted right to the top)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Dark Frosted Pill Badge with vibrant colored border & text
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Black.copy(alpha = 0.60f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.75f))
                    ) {
                        Text(
                            text = if (isHappening) "● HAPPENING NOW" else "NEXT EVENT",
                            color = badgeColor,
                            style = MaterialTheme.typography.labelSmall.copy(shadow = textShadow),
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title and Location (Realm + Area stacked cleanly on top-left)
                val (heroTitle, heroRealm, heroArea) = when (occurrence.event.id) {
                    "daily_geyser" -> Triple("Polluted Geyser", "Daylight Prairie", "Sanctuary Islands")
                    "daily_grandma" -> Triple("Grandma's Dinner", "Hidden Forest", "Elevated Clearing")
                    "daily_turtle" -> Triple("Sanctuary Turtle", "Daylight Prairie", "Sanctuary Islands")
                    "daily_shard_red" -> {
                        val parts = occurrence.event.name.split(" • ")
                        val title = parts.getOrNull(0) ?: "Red Shard"
                        val realm = parts.getOrNull(1) ?: ""
                        val area = parts.getOrNull(2) ?: ""
                        Triple(title, realm, area)
                    }
                    "daily_shard_black" -> {
                        val parts = occurrence.event.name.split(" • ")
                        val title = parts.getOrNull(0) ?: "Black Shard"
                        val realm = parts.getOrNull(1) ?: ""
                        val area = parts.getOrNull(2) ?: ""
                        Triple(title, realm, area)
                    }
                    else -> Triple(occurrence.event.name, "", "")
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = heroTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(shadow = textShadow),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )

                    if (heroRealm.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = heroRealm,
                            style = MaterialTheme.typography.titleSmall.copy(shadow = textShadow),
                            color = CozyWarmAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (heroArea.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = heroArea,
                            style = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow),
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(38.dp))

            // Bottom Countdown & Exact Time with AM/PM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (isHappening) "Ends in" else "Until start",
                        style = MaterialTheme.typography.labelMedium.copy(shadow = textShadow),
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                    val remainingMs = if (isHappening) occurrence.millisRemaining else occurrence.millisUntilStart
                    Text(
                        text = formatCountdown(remainingMs),
                        style = MaterialTheme.typography.headlineMedium.copy(shadow = textShadow),
                        color = if (isHappening) Color(0xFF4ADE80) else CozyWarmAmber,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Your time",
                        style = MaterialTheme.typography.labelMedium.copy(shadow = textShadow),
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = TimezoneHelper.formatEpochForZone(occurrence.startEpochMillis, userZoneId, is24Hour),
                        style = MaterialTheme.typography.titleMedium.copy(shadow = textShadow),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Glowing depleting progress bar when event is running (exclude reset and TS reveal)
            val isEligibleWaxEvent = occurrence.event.id != "daily_reset" && occurrence.event.id != "daily_ts_reveal"
            if (isHappening && isEligibleWaxEvent) {
                val totalDurationMs = maxOf(1L, occurrence.endEpochMillis - occurrence.startEpochMillis)
                val progressFraction = (occurrence.millisRemaining.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)

                Spacer(modifier = Modifier.height(14.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(NightSurfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFB923C), Color(0xFFEF4444))
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Event Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFB923C),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${formatCountdown(occurrence.millisRemaining)} left",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
fun NextEventPreviewCard(
    occurrence: EventOccurrence,
    userZoneId: ZoneId,
    is24Hour: Boolean
) {
    val iconEmoji = when (occurrence.event.id) {
        "daily_geyser" -> "🌋"
        "daily_turtle" -> "🐢"
        "daily_grandma" -> "👵"
        "daily_shard_red" -> "💎"
        "daily_shard_black" -> "🖤"
        "daily_reset" -> "🌅"
        "daily_ts_reveal" -> "🎭"
        else -> "🕯"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NightSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EventEmojiIcon(
                eventId = occurrence.event.id,
                size = 28.dp,
                fallbackEmoji = iconEmoji
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Next: ${occurrence.event.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = DogFluffWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "Starts at ${TimezoneHelper.formatEpochForZone(occurrence.startEpochMillis, userZoneId, is24Hour)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSlate,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = formatCountdown(occurrence.millisUntilStart),
                style = MaterialTheme.typography.titleMedium,
                color = StarlightBlue,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun ScheduleRowCard(
    occurrence: EventOccurrence,
    userZoneId: ZoneId,
    is24Hour: Boolean
) {
    val iconEmoji = when (occurrence.event.id) {
        "daily_turtle" -> "🐢"
        "daily_geyser" -> "🌋"
        "daily_grandma" -> "👵"
        "daily_shard_red" -> "💎"
        "daily_shard_black" -> "🖤"
        "daily_reset" -> "🌅"
        "daily_ts_reveal" -> "🎭"
        "extra_traveling_spirit" -> "🧭"
        else -> "✨"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NightSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EventEmojiIcon(
                eventId = occurrence.event.id,
                size = 28.dp,
                fallbackEmoji = iconEmoji
            )
            Spacer(modifier = Modifier.width(12.dp))
            val (rowTitle, rowLocation) = when (occurrence.event.id) {
                "daily_geyser" -> Pair("Polluted Geyser", "Daylight Prairie • Sanctuary Islands")
                "daily_grandma" -> Pair("Grandma's Dinner", "Hidden Forest • Elevated Clearing")
                "daily_turtle" -> Pair("Sanctuary Turtle", "Daylight Prairie • Sanctuary Islands")
                "daily_shard_red" -> {
                    val parts = occurrence.event.name.split(" • ")
                    val title = parts.getOrNull(0) ?: "Red Shard"
                    val loc = if (parts.size >= 3) "${parts[1]} • ${parts[2]}" else parts.drop(1).joinToString(" • ")
                    Pair(title, loc)
                }
                "daily_shard_black" -> {
                    val parts = occurrence.event.name.split(" • ")
                    val title = parts.getOrNull(0) ?: "Black Shard"
                    val loc = if (parts.size >= 3) "${parts[1]} • ${parts[2]}" else parts.drop(1).joinToString(" • ")
                    Pair(title, loc)
                }
                "extra_traveling_spirit" -> Pair(
                    occurrence.event.name,
                    "Home & Aviary Village • 4-Day Visit"
                )
                else -> Pair(occurrence.event.name, "")
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rowTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DogFluffWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (occurrence.isActiveNow) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4ADE80))
                        )
                    }
                }
                Text(
                    text = if (rowLocation.isNotEmpty()) rowLocation else occurrence.event.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (rowLocation.isNotEmpty()) CozyWarmAmber else MutedSlate,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = TimezoneHelper.formatEpochForZone(occurrence.startEpochMillis, userZoneId, is24Hour),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (occurrence.isActiveNow) Color(0xFF4ADE80) else CozyWarmAmber,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                val countdownText = if (occurrence.isActiveNow) {
                    if (occurrence.millisRemaining > 3600 * 1000L) {
                        "${formatExtraCountdown(occurrence.millisRemaining)} left"
                    } else {
                        "Happening"
                    }
                } else {
                    formatCountdown(occurrence.millisUntilStart)
                }
                Text(
                    text = countdownText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (occurrence.isActiveNow) Color(0xFF4ADE80) else StarlightBlue,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun FriendPreviewCard(
    friend: Friend,
    is24Hour: Boolean
) {
    val friendZone = remember(friend.timezoneId) {
        try { ZoneId.of(friend.timezoneId) } catch (_: Exception) { ZoneId.of("UTC") }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NightSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = friend.countryFlag, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = friend.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DogFluffWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = friend.timezoneId.substringAfterLast('/'),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                }
            }

            Text(
                text = TimezoneHelper.getCurrentTimeFormatted(friendZone, is24Hour),
                style = MaterialTheme.typography.titleMedium,
                color = StarlightBlue,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

/**
 * Overlays verified Traveling Spirit names/descriptions from the OTA feed (Room DB)
 * onto the engine's deterministic bi-weekly TS occurrence. The engine intentionally
 * stays generic — the feed is the single source of truth for who is actually
 * visiting. If no feed entry overlaps the window (not pushed yet / offline),
 * the generic "Traveling Spirit" name is kept.
 */
fun overlayTravelingSpiritNames(
    occurrences: List<EventOccurrence>,
    calendarEvents: List<com.lumanest.models.SkyEvent>
): List<EventOccurrence> = occurrences.map { occ ->
    if (occ.event.id != "extra_traveling_spirit") {
        occ
    } else {
        val dbTs = calendarEvents.firstOrNull { e ->
            e.category == com.lumanest.models.EventCategory.TRAVELING_SPIRIT && try {
                val s = java.time.Instant.parse(e.startIso).toEpochMilli()
                val en = java.time.Instant.parse(e.endIso).toEpochMilli()
                s <= occ.endEpochMillis && en >= occ.startEpochMillis
            } catch (_: Exception) {
                false
            }
        }
        if (dbTs != null) {
            occ.copy(event = occ.event.copy(name = dbTs.name, description = dbTs.description))
        } else {
            occ
        }
    }
}

@Composable
fun ExtraActivitiesDropdownCard(
    occurrences: List<EventOccurrence>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    userZoneId: ZoneId,
    is24Hour: Boolean
) {
    val activeCount = occurrences.count { it.isActiveNow }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevronRotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) NightSurface else NightSurfaceVariant.copy(alpha = 0.7f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isExpanded) CozyWarmAmber.copy(alpha = 0.4f) else CardBorder
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (activeCount > 0) Color(0xFF14532D) else DeepMidnight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🌌", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "More Activities & Shows",
                            style = MaterialTheme.typography.titleSmall,
                            color = DogFluffWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isExpanded) "Tap to collapse" else "Skater, Passage, Nest Sunset, Aurora & more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedSlate,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (activeCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF14532D)
                        ) {
                            Text(
                                text = "● $activeCount Active",
                                color = Color(0xFF4ADE80),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DeepMidnight.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "${occurrences.size}",
                                color = CozyWarmAmber,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = CozyWarmAmber,
                        modifier = Modifier.rotate(rotationAngle)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(
                        color = CardBorder.copy(alpha = 0.5f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    occurrences.forEach { occ ->
                        ExtraActivityRowItem(
                            occurrence = occ,
                            userZoneId = userZoneId,
                            is24Hour = is24Hour
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExtraActivityRowItem(
    occurrence: EventOccurrence,
    userZoneId: ZoneId,
    is24Hour: Boolean
) {
    val (emoji, title, locationText) = when {
        occurrence.event.id == "daily_reset" -> Triple("🌅", "Daily Reset", "Sky Clock • Daily at 00:00 PT")
        occurrence.event.id == "extra_traveling_spirit" -> Triple("🧭", occurrence.event.name, "Home & Aviary • Every 2 weeks (Thu–Sun)")
        occurrence.event.id == "extra_dreams_skater" -> Triple("⛸️", "Dreams Skater", "Village of Dreams • Fri–Sun every 2h")
        occurrence.event.id == "extra_passage_quests" -> Triple("📜", "Passage Quests", "Passage Area • Every 15 min")
        occurrence.event.id == "extra_nest_sunset" -> Triple("🌇", "Nest Sunset", "Player's Nest • Hourly at :40")
        occurrence.event.id == "extra_aurora_concert" -> Triple("🎶", "AURORA Concert", "Colosseum • Every 4h at :10")
        occurrence.event.id == "extra_weekly_reset" -> Triple("🔄", "Weekly Eden Reset", "Eye of Eden • Sundays 00:00 PT")
        occurrence.event.id == "extra_store_rotation" -> Triple("🛍️", "Store Item Rotation", "Aviary Shop • Mondays 00:00 PT")
        occurrence.event.id == "extra_spell_shop" -> Triple("🔮", "Spell Shop Expanded", "Forgotten Ark & Aviary • Fri–Sun")
        occurrence.event.id == "extra_fireworks_festival" -> Triple("🎆", "Fireworks Festival", "Aviary Village • 1st of month")
        occurrence.event.category == com.lumanest.models.EventCategory.DOUBLE_CURRENCY || occurrence.event.id.contains("double", ignoreCase = true) -> {
            if (occurrence.event.name.contains("Candle", ignoreCase = true)) {
                Triple("🕯️", occurrence.event.name, occurrence.event.description.ifEmpty { "Daily Bonus • 2x Treasure Candles in realm" })
            } else {
                Triple("✨", occurrence.event.name, occurrence.event.description.ifEmpty { "Seasonal Bonus • 2x Seasonal Light in realm" })
            }
        }
        else -> Triple("✨", occurrence.event.name, occurrence.event.description)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DeepMidnight.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(NightSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DogFluffWhite,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    if (occurrence.isActiveNow) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4ADE80))
                        )
                    }
                }
                Text(
                    text = locationText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                if (occurrence.isActiveNow) {
                    Text(
                        text = "Active Now",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF4ADE80),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${formatExtraCountdown(occurrence.millisRemaining)} left",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4ADE80).copy(alpha = 0.8f)
                    )
                } else {
                    val isWithin24Hours = occurrence.millisUntilStart < 24 * 3600 * 1000L
                    Text(
                        text = if (isWithin24Hours) {
                            TimezoneHelper.formatEpochForZone(occurrence.startEpochMillis, userZoneId, is24Hour)
                        } else {
                            val zdt = java.time.Instant.ofEpochMilli(occurrence.startEpochMillis).atZone(userZoneId)
                            val dayShort = zdt.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                            val timeStr = TimezoneHelper.formatEpochForZone(occurrence.startEpochMillis, userZoneId, is24Hour)
                            "$dayShort $timeStr"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = CozyWarmAmber,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "in ${formatExtraCountdown(occurrence.millisUntilStart)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = StarlightBlue,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

fun formatExtraCountdown(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val totalMinutes = totalSeconds / 60
    val totalHours = totalMinutes / 60
    val days = totalHours / 24
    val hours = totalHours % 24
    val minutes = totalMinutes % 60
    val seconds = totalSeconds % 60

    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

