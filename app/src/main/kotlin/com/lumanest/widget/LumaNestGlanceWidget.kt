package com.lumanest.app.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.lumanest.eventengine.SkyEventEngine
import com.lumanest.timezone.TimezoneHelper

class LumaNestGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val now = System.currentTimeMillis()
        val dailyEvents = SkyEventEngine.calculateDailyOccurrences(now)
        val featured = SkyEventEngine.getPrimaryFeaturedOccurrence(dailyEvents)
        val deviceZone = TimezoneHelper.getDeviceZoneId()

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF0F141C)))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "☁ LumaNest",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFFFB74D)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.height(6.dp))

                if (featured != null) {
                    Text(
                        text = featured.event.name,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFF8FAFC)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    val remainingSecs = if (featured.isActiveNow) featured.millisRemaining / 1000 else featured.millisUntilStart / 1000
                    val mm = (remainingSecs % 3600) / 60
                    val ss = remainingSecs % 60
                    val statusText = if (featured.isActiveNow) "Active now ($mm:${if (ss < 10) "0$ss" else "$ss"})" else "Starts in $mm:${if (ss < 10) "0$ss" else "$ss"}"

                    Text(
                        text = statusText,
                        style = TextStyle(
                            color = ColorProvider(if (featured.isActiveNow) Color(0xFF4ADE80) else Color(0xFF90CAF9)),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    )
                } else {
                    Text(
                        text = "All clear",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF94A3B8)),
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}

class LumaNestWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LumaNestGlanceWidget()
}
