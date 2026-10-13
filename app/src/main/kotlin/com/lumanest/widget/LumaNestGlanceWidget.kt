package com.lumanest.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lumanest.app.R
import com.lumanest.app.ui.MainActivity
import com.lumanest.eventengine.SkyEventEngine
import com.lumanest.models.EventOccurrence

/**
 * Base implementation for LumaNest Event Widgets:
 * - Full-bleed scenic watercolor artwork.
 * - Layered high-contrast drop shadow text.
 * - Exact Shard location name shown (e.g. "Red Shard • Forgotten Ark").
 */
abstract class BaseEventGlanceWidget(private val isCard2x2: Boolean) : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val now = System.currentTimeMillis()
        val allOccurrences = SkyEventEngine.calculateDailyOccurrences(now)

        val eligibleOccurrences = allOccurrences.filter { occ ->
            val eventId = occ.event.id
            eventId == "daily_geyser" ||
            eventId == "daily_grandma" ||
            eventId == "daily_turtle" ||
            eventId == "daily_shard_red" ||
            eventId == "daily_shard_black"
        }

        val active = eligibleOccurrences.firstOrNull { it.isActiveNow }
        val next = eligibleOccurrences.filter { !it.isActiveNow }.minByOrNull { it.millisUntilStart }
        val targetOccurrence: EventOccurrence? = active ?: next

        val backgroundResId = when (targetOccurrence?.event?.id) {
            "daily_geyser" -> R.drawable.widget_bg_geyser
            "daily_grandma" -> R.drawable.widget_bg_grandma
            "daily_turtle" -> R.drawable.widget_bg_turtle
            "daily_shard_red", "daily_shard_black" -> R.drawable.widget_bg_shard
            else -> R.drawable.widget_bg_turtle
        }

        val eventEmoji = when (targetOccurrence?.event?.id) {
            "daily_geyser" -> "🌋"
            "daily_grandma" -> "👵"
            "daily_turtle" -> "🐢"
            "daily_shard_red" -> "💎"
            "daily_shard_black" -> "🖤"
            else -> "✨"
        }

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(24.dp)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                // 1. Full-bleed atmospheric artwork
                Image(
                    provider = ImageProvider(backgroundResId),
                    contentDescription = "Event Artwork",
                    modifier = GlanceModifier.fillMaxSize()
                )

                // 2. Seamless Text with Drop Shadow
                if (targetOccurrence != null) {
                    val isActive = targetOccurrence.isActiveNow
                    val remainingSecs = if (isActive) {
                        targetOccurrence.millisRemaining / 1000
                    } else {
                        targetOccurrence.millisUntilStart / 1000
                    }
                    val mm = (remainingSecs % 3600) / 60
                    val ss = remainingSecs % 60
                    val hh = remainingSecs / 3600

                    val timeFormatted = if (hh > 0) {
                        "${hh}h ${mm}m"
                    } else if (mm > 0) {
                        "${mm}m ${if (ss < 10) "0$ss" else "$ss"}s"
                    } else {
                        "${ss}s"
                    }

                    val statusBadgeText = if (isActive) "HAPPENING NOW" else "UPCOMING"
                    val statusColor = if (isActive) Color(0xFF4ADE80) else Color(0xFFFFA51F)

                    // Extract Title, Realm, and Area
                    val (displayTitle, realmText, areaText) = when (targetOccurrence.event.id) {
                        "daily_geyser" -> Triple("Polluted Geyser", "Daylight Prairie", "Sanctuary Islands")
                        "daily_grandma" -> Triple("Grandma's Dinner", "Hidden Forest", "Elevated Clearing")
                        "daily_turtle" -> Triple("Sanctuary Turtle", "Daylight Prairie", "Sanctuary Islands")
                        "daily_shard_red" -> {
                            val parts = targetOccurrence.event.name.split(" • ")
                            val title = parts.getOrNull(0) ?: "Red Shard"
                            val realm = parts.getOrNull(1) ?: ""
                            val area = parts.getOrNull(2) ?: ""
                            Triple(title, realm, area)
                        }
                        "daily_shard_black" -> {
                            val parts = targetOccurrence.event.name.split(" • ")
                            val title = parts.getOrNull(0) ?: "Black Shard"
                            val realm = parts.getOrNull(1) ?: ""
                            val area = parts.getOrNull(2) ?: ""
                            Triple(title, realm, area)
                        }
                        else -> Triple(targetOccurrence.event.name, "", "")
                    }

                    if (isCard2x2) {
                        // Compact Card (2x1) Layout: Clean stacked Realm & Area
                        Column(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalAlignment = Alignment.Start
                        ) {
                            ShadowedText(
                                text = statusBadgeText,
                                textColor = statusColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = GlanceModifier.height(1.dp))
                            ShadowedText(
                                text = displayTitle,
                                textColor = Color(0xFFF5EFE5),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (realmText.isNotEmpty()) {
                                ShadowedText(
                                    text = realmText,
                                    textColor = Color(0xFFFFA51F),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                            if (areaText.isNotEmpty()) {
                                ShadowedText(
                                    text = areaText,
                                    textColor = Color(0xFFE2E8F0),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(2.dp))
                            ShadowedText(
                                text = if (isActive) "⏳ $timeFormatted left" else "⏰ in $timeFormatted",
                                textColor = Color(0xFFFFA51F),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    } else {
                        // Wide Banner (4x2) Layout: Full-width spacious luxury layout
                        Column(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ShadowedText(
                                    text = statusBadgeText,
                                    textColor = statusColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                ShadowedText(
                                    text = "· LumaNest",
                                    textColor = Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            Spacer(modifier = GlanceModifier.height(2.dp))

                            ShadowedText(
                                text = displayTitle,
                                textColor = Color(0xFFF5EFE5),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )

                            if (realmText.isNotEmpty()) {
                                Spacer(modifier = GlanceModifier.height(1.dp))
                                ShadowedText(
                                    text = realmText,
                                    textColor = Color(0xFFFFA51F),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            if (areaText.isNotEmpty()) {
                                ShadowedText(
                                    text = areaText,
                                    textColor = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = GlanceModifier.height(3.dp))

                            ShadowedText(
                                text = if (isActive) "⏳ $timeFormatted left" else "⏰ in $timeFormatted",
                                textColor = Color(0xFFFFA51F),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        ShadowedText(
                            text = "✨ Peaceful Skies",
                            textColor = Color(0xFFF5EFE5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        ShadowedText(
                            text = "All realms quiet for now",
                            textColor = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/**
 * Drop-shadow text composable for Glance
 */
@androidx.compose.runtime.Composable
fun ShadowedText(
    text: String,
    textColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    maxLines: Int = 1
) {
    Box {
        Box(modifier = GlanceModifier.padding(start = 1.dp, top = 1.dp)) {
            Text(
                text = text,
                maxLines = maxLines,
                style = TextStyle(
                    color = ColorProvider(Color(0xD9000000)),
                    fontWeight = fontWeight,
                    fontSize = fontSize
                )
            )
        }
        Text(
            text = text,
            maxLines = maxLines,
            style = TextStyle(
                color = ColorProvider(textColor),
                fontWeight = fontWeight,
                fontSize = fontSize
            )
        )
    }
}

// 1. LumaNest Events (Banner 4x2)
class EventBannerGlanceWidget : BaseEventGlanceWidget(isCard2x2 = false)
class EventBannerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EventBannerGlanceWidget()
}

// 2. LumaNest Events (Card 2x2)
class EventCardGlanceWidget : BaseEventGlanceWidget(isCard2x2 = true)
class EventCardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = EventCardGlanceWidget()
}
