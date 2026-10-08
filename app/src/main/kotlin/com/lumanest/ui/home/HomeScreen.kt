package com.lumanest.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // 1-second UI countdown ticker
    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentMillis = System.currentTimeMillis()
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { com.lumanest.data.local.PreferencesManager(context) }

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

    // Filter occurrences based on user preferences and sort chronologically (active first, then closest start)
    val dailyOccurrences = remember(rawDailyOccurrences, showGeyser, showGrandma, showTurtle, showShards, showReset, showTsReveal) {
        rawDailyOccurrences
            .filter { occ ->
                when (occ.event.id) {
                    "daily_geyser" -> showGeyser
                    "daily_grandma" -> showGrandma
                    "daily_turtle" -> showTurtle
                    "daily_shard_red", "daily_shard_black" -> showShards
                    "daily_reset" -> showReset
                    "daily_ts_reveal" -> showTsReveal && (occ.isActiveNow || occ.millisUntilStart <= 24 * 3600 * 1000L)
                    else -> true
                }
            }
            .sortedWith(
                compareByDescending<EventOccurrence> { it.isActiveNow }
                    .thenBy { it.millisUntilStart }
            )
    }

    // Determine current active event or next upcoming event
    val activeEvent = remember(dailyOccurrences) {
        dailyOccurrences.firstOrNull { it.isActiveNow }
    }
    val nextEvent = remember(dailyOccurrences, activeEvent) {
        if (activeEvent != null) {
            dailyOccurrences.filter { !it.isActiveNow }.minByOrNull { it.millisUntilStart }
        } else {
            dailyOccurrences.minByOrNull { it.millisUntilStart }
        }
    }

    // Top 3 Upcoming Calendar Events (excluding Shard eruptions which live on their own schedule)
    val topUpcomingCalendarEvents = remember(calendarEvents) {
        val now = System.currentTimeMillis()
        calendarEvents
            .filter { e ->
                e.category != com.lumanest.models.EventCategory.SHARD_ERUPTION &&
                try { java.time.Instant.parse(e.endIso).toEpochMilli() >= now } catch (_: Exception) { false }
            }
            .sortedBy { e ->
                try { java.time.Instant.parse(e.startIso).toEpochMilli() } catch (_: Exception) { Long.MAX_VALUE }
            }
            .take(3)
    }

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
                val startDateStr = try {
                    val epoch = java.time.Instant.parse(calEvent.startIso).toEpochMilli()
                    TimezoneHelper.formatEpochDateForZone(epoch, userZoneId)
                } catch (_: Exception) { calEvent.startIso }

                val (tagTextColor, tagBgColor) = when (calEvent.category) {
                    com.lumanest.models.EventCategory.SEASON -> Color.White to Color(0xFFEA580C)
                    com.lumanest.models.EventCategory.DOUBLE_CURRENCY -> Color.White to Color(0xFFDC2626)
                    com.lumanest.models.EventCategory.TRAVELING_SPIRIT -> Color.White to Color(0xFF0284C7)
                    com.lumanest.models.EventCategory.MAJOR_EVENT -> Color.White to Color(0xFF9333EA)
                    com.lumanest.models.EventCategory.SHARD_ERUPTION -> Color.White to Color(0xFFE11D48)
                    com.lumanest.models.EventCategory.MAINTENANCE -> Color.White to Color(0xFFD97706)
                    else -> Color.White to StarlightBlue
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
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = tagBgColor
                        ) {
                            Text(
                                text = calEvent.category.name.replace('_', ' '),
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
                                text = "Starts: $startDateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSlate
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
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
                    EventToggleRow(label = "🌅 Sky Daily Reset", checked = showReset, onCheckedChange = {
                        showReset = it
                        prefs.filterReset = it
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

        // Time indicator pill
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NightSurfaceVariant,
            modifier = Modifier.padding(2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = TimezoneHelper.getCurrentTimeFormatted(userZoneId, is24Hour),
                    style = MaterialTheme.typography.titleSmall,
                    color = CozyWarmAmber,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = userZoneId.id.substringAfterLast('/'),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate
                )
            }
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
        "daily_shard_red" -> "🔴"
        "daily_shard_black" -> "⚫"
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
                    horizontalArrangement = Arrangement.SpaceBetween,
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

                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.40f)
                    ) {
                        IconButton(
                            onClick = { onToggleFavorite(occurrence.event.id, occurrence.event.isFavorite) }
                        ) {
                            Icon(
                                imageVector = if (occurrence.event.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Favorite",
                                tint = if (occurrence.event.isFavorite) CozyWarmAmber else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Title & Description (Crystal clear with drop shadow)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = occurrence.event.name,
                        style = MaterialTheme.typography.headlineSmall.copy(shadow = textShadow),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    val formattedDescription = when (occurrence.event.id) {
                        "daily_geyser" -> "Erupts dark wax every\n2 hours in Sanctuary Islands."
                        "daily_grandma" -> "Belonging Elder bakes\nwarm light buns in Hidden Forest."
                        "daily_turtle" -> "Sunset swimming turtle\nshedding light shells in Sanctuary."
                        "daily_shard_red" -> "Strong Red Shard eruption\nRewards Ascended Candles."
                        "daily_shard_black" -> "Regular Black Shard eruption\nRewards regular wax."
                        else -> occurrence.event.description
                    }

                    Text(
                        text = formattedDescription,
                        style = MaterialTheme.typography.bodySmall.copy(
                            shadow = textShadow,
                            lineHeight = 16.sp
                        ),
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.Medium
                    )
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
        "daily_shard_red" -> "🔴"
        "daily_shard_black" -> "⚫"
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
            Text(text = iconEmoji, fontSize = 24.sp)
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
        "daily_shard_red" -> "🔴"
        "daily_shard_black" -> "⚫"
        "daily_reset" -> "🌅"
        "daily_ts_reveal" -> "🎭"
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
            Text(text = iconEmoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = occurrence.event.name,
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
                    text = occurrence.event.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedSlate,
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
                Text(
                    text = if (occurrence.isActiveNow) "Happening" else formatCountdown(occurrence.millisUntilStart),
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
