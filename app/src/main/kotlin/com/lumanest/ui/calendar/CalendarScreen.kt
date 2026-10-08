package com.lumanest.ui.calendar

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumanest.models.EventCategory
import com.lumanest.models.SkyEvent
import com.lumanest.timezone.TimezoneHelper
import com.lumanest.ui.theme.*
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    events: List<SkyEvent>,
    userZoneId: ZoneId,
    is24Hour: Boolean
) {
    var selectedCategory by remember { mutableStateOf<EventCategory?>(null) }
    val context = LocalContext.current

    // Integrate database events + dynamically calculated upcoming Shards
    val allCalendarEvents = remember(events) {
        val nowMillis = System.currentTimeMillis()
        val shardEvents = com.lumanest.eventengine.SkyEventEngine.calculateUpcomingShards(nowMillis, daysAhead = 7)
        (events + shardEvents)
            // Filter strictly: Only include ongoing (active) or upcoming future events (end time in the future)
            .filter { event ->
                try {
                    val endEpoch = Instant.parse(event.endIso).toEpochMilli()
                    endEpoch >= nowMillis
                } catch (_: Exception) {
                    true
                }
            }
            .sortedBy { event ->
                try {
                    Instant.parse(event.startIso).toEpochMilli()
                } catch (_: Exception) {
                    0L
                }
            }
    }

    val filteredEvents = remember(allCalendarEvents, selectedCategory) {
        if (selectedCategory == null) {
            // "All Events" only shows major events, seasons, spirits, etc.
            // Shard Eruptions are kept strictly in their dedicated category!
            allCalendarEvents.filter { it.category != EventCategory.SHARD_ERUPTION }
        } else {
            allCalendarEvents.filter { it.category == selectedCategory }
        }
    }

    // Categories to show: Exclude DAILY_RECURRING (since that belongs on Home/Today schedule) and COMMUNITY
    val visibleCategories = listOf(
        EventCategory.SEASON,
        EventCategory.SHARD_ERUPTION,
        EventCategory.TRAVELING_SPIRIT,
        EventCategory.MAJOR_EVENT,
        EventCategory.DOUBLE_CURRENCY,
        EventCategory.MAINTENANCE
    )

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sky Calendar",
                        style = MaterialTheme.typography.headlineMedium,
                        color = DogFluffWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Verified upcoming seasons, spirits & shards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Privacy-first Export .ICS button
                Button(
                    onClick = { exportIcsCalendar(context, allCalendarEvents) },
                    colors = ButtonDefaults.buttonColors(containerColor = NightSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Export ICS", tint = CozyWarmAmber)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ".ics",
                        color = DogFluffWhite,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        // Clean Category Filter Chips (No Daily Recurring, No Community)
        item {
            ScrollableTabRow(
                selectedTabIndex = if (selectedCategory == null) 0 else (visibleCategories.indexOf(selectedCategory) + 1).coerceAtLeast(0),
                edgePadding = 0.dp,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All Events") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CozyWarmAmber,
                        selectedLabelColor = DeepMidnight,
                        containerColor = NightSurfaceVariant,
                        labelColor = DogFluffWhite
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
                visibleCategories.forEach { cat ->
                    val displayName = when (cat) {
                        EventCategory.SHARD_ERUPTION -> "Shard Eruption"
                        EventCategory.TRAVELING_SPIRIT -> "Traveling Spirit"
                        EventCategory.MAJOR_EVENT -> "Major Events"
                        EventCategory.DOUBLE_CURRENCY -> "Double Light"
                        EventCategory.SEASON -> "Seasons"
                        EventCategory.MAINTENANCE -> "Maintenance"
                        else -> cat.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
                    }
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CozyWarmAmber,
                            selectedLabelColor = DeepMidnight,
                            containerColor = NightSurfaceVariant,
                            labelColor = DogFluffWhite
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }

        if (filteredEvents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NightSurface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "✨", fontSize = 28.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No upcoming events in this category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSlate
                        )
                    }
                }
            }
        } else {
            items(filteredEvents, key = { it.id }) { event ->
                CalendarEventCard(event = event, userZoneId = userZoneId)
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CalendarEventCard(event: SkyEvent, userZoneId: ZoneId) {
    val nowMillis = System.currentTimeMillis()
    val startMillis = try { Instant.parse(event.startIso).toEpochMilli() } catch (_: Exception) { 0L }
    val endMillis = try { Instant.parse(event.endIso).toEpochMilli() } catch (_: Exception) { Long.MAX_VALUE }

    val isRunning = nowMillis in startMillis..endMillis
    val isUpcoming = nowMillis < startMillis

    val startDateStr = try {
        TimezoneHelper.formatEpochDateForZone(startMillis, userZoneId)
    } catch (_: Exception) { event.startIso }

    val endDateStr = try {
        TimezoneHelper.formatEpochDateForZone(endMillis, userZoneId)
    } catch (_: Exception) { event.endIso }

    // Tag Pill Colors:
    // Seasons: Solid Orange (#EA580C) with White text
    // Traveling Spirit: Solid Blue (#0284C7) with White text
    // Major Events: Solid Purple (#9333EA) with White text
    // Double Currency: Solid Ruby Red (#DC2626) with White text
    // Shard Eruption: Solid Crimson (#E11D48) with White text
    // Maintenance: Solid Amber (#D97706) with White text
    val (tagTextColor, tagBgColor) = when (event.category) {
        EventCategory.SEASON -> Color.White to Color(0xFFEA580C)
        EventCategory.DOUBLE_CURRENCY -> Color.White to Color(0xFFDC2626)
        EventCategory.SHARD_ERUPTION -> Color.White to Color(0xFFE11D48)
        EventCategory.TRAVELING_SPIRIT -> Color.White to Color(0xFF0284C7)
        EventCategory.MAJOR_EVENT -> Color.White to Color(0xFF9333EA)
        EventCategory.MAINTENANCE -> Color.White to Color(0xFFD97706)
        else -> Color.White to StarlightBlue
    }

    // Title color: Season name in Orange, others in DogFluffWhite
    val titleColor = if (event.category == EventCategory.SEASON) Color(0xFFFB923C) else DogFluffWhite

    val artDrawableRes = when {
        event.id.contains("pearl", ignoreCase = true) || event.name.contains("Pearl", ignoreCase = true) ->
            com.lumanest.app.R.drawable.art_season_pearl
        event.id.contains("mischief", ignoreCase = true) || event.name.contains("Mischief", ignoreCase = true) ->
            com.lumanest.app.R.drawable.art_days_mischief
        event.id.contains("double_candles", ignoreCase = true) || event.name.contains("Double Treasure", ignoreCase = true) ->
            com.lumanest.app.R.drawable.art_double_candles
        event.category == EventCategory.TRAVELING_SPIRIT ->
            com.lumanest.app.R.drawable.art_traveling_spirit
        event.category == EventCategory.SHARD_ERUPTION -> {
            if (event.name.contains("Red", ignoreCase = true)) com.lumanest.app.R.drawable.art_shard_red
            else com.lumanest.app.R.drawable.art_shard_black
        }
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NightSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (artDrawableRes != null) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = artDrawableRes),
                    contentDescription = event.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.TopCenter)
                        .graphicsLayer { alpha = 0.85f },
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.15f),
                                    Color.Black.copy(alpha = 0.45f),
                                    NightSurface.copy(alpha = 0.90f),
                                    NightSurface
                                )
                            )
                        )
                )
            }

            Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tagBgColor
                ) {
                    Text(
                        text = event.category.name.replace('_', ' '),
                        color = tagTextColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Status Badge: RUNNING vs UPCOMING
                if (isRunning) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF14532D)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF22C55E), shape = RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "RUNNING",
                                color = Color(0xFF4ADE80),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (isUpcoming) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NightSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏳ UPCOMING",
                                color = CozyWarmAmber,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = event.name,
                style = MaterialTheme.typography.titleLarge,
                color = titleColor,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSlate,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "$startDateStr → $endDateStr",
                style = MaterialTheme.typography.bodySmall,
                color = CozyWarmAmber,
                fontWeight = FontWeight.Medium
            )
        }
        }
    }
}

fun exportIcsCalendar(context: Context, events: List<SkyEvent>) {
    val icsContent = buildString {
        appendLine("BEGIN:VCALENDAR")
        appendLine("VERSION:2.0")
        appendLine("PRODID:-//LumaNest//Cozy Sky Companion//EN")
        events.forEach { e ->
            appendLine("BEGIN:VEVENT")
            appendLine("SUMMARY:${e.name}")
            appendLine("DESCRIPTION:${e.description}")
            appendLine("DTSTART:${e.startIso.replace("-", "").replace(":", "").substringBefore(".")}")
            appendLine("DTEND:${e.endIso.replace("-", "").replace(":", "").substringBefore(".")}")
            appendLine("STATUS:CONFIRMED")
            appendLine("END:VEVENT")
        }
        appendLine("END:VCALENDAR")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/calendar"
        putExtra(Intent.EXTRA_SUBJECT, "LumaNest Sky Events.ics")
        putExtra(Intent.EXTRA_TEXT, icsContent)
    }
    context.startActivity(Intent.createChooser(intent, "Export Sky Calendar (.ics)"))
}
