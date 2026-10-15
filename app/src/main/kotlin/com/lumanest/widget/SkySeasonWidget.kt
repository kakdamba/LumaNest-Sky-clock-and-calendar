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
import com.lumanest.app.R
import com.lumanest.app.ui.MainActivity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Base implementation for LumaNest Season Widgets (Days of Mischief style):
 * - Full-bleed scenic artwork.
 * - Layered high-contrast drop shadow text.
 * - Big countdown number.
 */
abstract class BaseSeasonGlanceWidget(private val isCard2x2: Boolean) : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val now = System.currentTimeMillis()

        data class SeasonEvent(
            val name: String,
            val startMillis: Long,
            val endMillis: Long,
            val emoji: String
        )

        fun parseMs(iso: String): Long = try {
            Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) { 0L }

        val dbEvents = try {
            com.lumanest.data.local.LumaNestDatabase.getInstance(context).eventDao().getAllEventsSync()
        } catch (_: Exception) {
            emptyList()
        }

        val parsedEvents = if (dbEvents.isNotEmpty()) {
            dbEvents
                .filter { it.category == "SEASON" || it.category == "MAJOR_EVENT" }
                .map {
                    val emoji = when {
                        it.name.contains("Pearl", ignoreCase = true) -> "🐚"
                        it.name.contains("Mischief", ignoreCase = true) -> "🎃"
                        it.name.contains("Feast", ignoreCase = true) -> "❄️"
                        it.name.contains("Fortune", ignoreCase = true) -> "🧧"
                        it.name.contains("Bloom", ignoreCase = true) -> "🌸"
                        else -> "✨"
                    }
                    SeasonEvent(it.name, parseMs(it.startIso), parseMs(it.endIso), emoji)
                }
        } else {
            listOf(
                SeasonEvent("Season of Pearl", parseMs("2026-10-16T07:00:00Z"), parseMs("2026-12-31T07:59:59Z"), "🐚"),
                SeasonEvent("Days of Mischief", parseMs("2026-10-19T07:00:00Z"), parseMs("2026-11-09T07:59:59Z"), "🎃")
            )
        }

        val active = parsedEvents.filter { now in it.startMillis..it.endMillis }
            .maxByOrNull { it.endMillis - now }
        val upcoming = parsedEvents.filter { it.startMillis > now }
            .minByOrNull { it.startMillis - now }
        val target = active ?: upcoming

        val bgDrawableRes = when {
            target?.name?.contains("Pearl", ignoreCase = true) == true -> R.drawable.art_season_pearl
            target?.name?.contains("Mischief", ignoreCase = true) == true -> R.drawable.art_days_mischief
            else -> R.drawable.widget_bg_season
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
                    provider = ImageProvider(bgDrawableRes),
                    contentDescription = "Season Artwork",
                    modifier = GlanceModifier.fillMaxSize()
                )

                // 2. Seamless Text with Drop Shadow
                if (target != null) {
                    val isActive = now in target.startMillis..target.endMillis
                    val msRemaining = if (isActive) target.endMillis - now else target.startMillis - now
                    val totalHoursRemaining = (msRemaining / (1000 * 60 * 60)).coerceAtLeast(0)
                    val daysRemaining = totalHoursRemaining / 24
                    val totalMinutesRemaining = (msRemaining / (1000 * 60)).coerceAtLeast(0)

                    val (countNumber, countLabel) = when {
                        daysRemaining > 0 -> "$daysRemaining" to (if (isActive) "days left" else "days away")
                        totalHoursRemaining > 0 -> "$totalHoursRemaining" to (if (isActive) "hours left" else "hours away")
                        else -> "${maxOf(1L, totalMinutesRemaining)}" to (if (isActive) "mins left" else "mins away")
                    }

                    val dateFmt = DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.of("UTC"))
                    val startStr = dateFmt.format(Instant.ofEpochMilli(target.startMillis))
                    val endStr = dateFmt.format(Instant.ofEpochMilli(target.endMillis))
                    val dateRange = "$startStr – $endStr"

                    if (isCard2x2) {
                        // Compact Card 2x2 Layout: Perfectly balanced for half-width home screens
                        Column(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = target.emoji,
                                    style = TextStyle(fontSize = 14.sp)
                                )
                                Spacer(modifier = GlanceModifier.width(4.dp))
                                ShadowedText(
                                    text = target.name,
                                    textColor = Color(0xFFF5EFE5),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = GlanceModifier.height(2.dp))

                            ShadowedText(
                                text = dateRange,
                                textColor = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1
                            )

                            Spacer(modifier = GlanceModifier.defaultWeight())

                            Row(verticalAlignment = Alignment.Bottom) {
                                ShadowedText(
                                    text = countNumber,
                                    textColor = Color(0xFFF5EFE5),
                                    fontSize = if (countNumber.length > 2) 26.sp else 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                Column(verticalAlignment = Alignment.Bottom) {
                                    Spacer(modifier = GlanceModifier.height(14.dp))
                                    ShadowedText(
                                        text = countLabel,
                                        textColor = Color(0xFFFFA51F),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // Wide Banner 4x2 Layout: Full-width spacious luxury layout
                        Column(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = target.emoji,
                                    style = TextStyle(fontSize = 16.sp)
                                )
                                Spacer(modifier = GlanceModifier.width(6.dp))
                                ShadowedText(
                                    text = target.name,
                                    textColor = Color(0xFFF5EFE5),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = GlanceModifier.height(2.dp))

                            ShadowedText(
                                text = dateRange,
                                textColor = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1
                            )

                            Spacer(modifier = GlanceModifier.defaultWeight())

                            Row(verticalAlignment = Alignment.Bottom) {
                                ShadowedText(
                                    text = countNumber,
                                    textColor = Color(0xFFF5EFE5),
                                    fontSize = if (countNumber.length > 2) 38.sp else 46.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = GlanceModifier.width(8.dp))
                                Column(verticalAlignment = Alignment.Bottom) {
                                    Spacer(modifier = GlanceModifier.height(24.dp))
                                    ShadowedText(
                                        text = countLabel,
                                        textColor = Color(0xFFFFA51F),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
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
                            text = "✨ No Season Event",
                            textColor = Color(0xFFF5EFE5),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// 3. LumaNest Season (Banner 4x2)
class SeasonBannerGlanceWidget : BaseSeasonGlanceWidget(isCard2x2 = false)
class SeasonBannerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SeasonBannerGlanceWidget()
}

// 4. LumaNest Season (Card 2x2)
class SeasonCardGlanceWidget : BaseSeasonGlanceWidget(isCard2x2 = true)
class SeasonCardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SeasonCardGlanceWidget()
}
