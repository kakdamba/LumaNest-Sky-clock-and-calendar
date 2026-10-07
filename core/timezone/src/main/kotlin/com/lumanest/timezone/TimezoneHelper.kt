package com.lumanest.timezone

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TimezoneEntry(
    val id: String,
    val countryName: String,
    val flag: String,
    val cityName: String
)

object TimezoneHelper {
    val SKY_RESET_ZONE: ZoneId = ZoneId.of("America/Los_Angeles")

    fun getDeviceZoneId(): ZoneId {
        return try {
            ZoneId.systemDefault()
        } catch (_: Exception) {
            ZoneId.of("UTC")
        }
    }

    fun isValidZoneId(zoneIdStr: String): Boolean {
        return try {
            ZoneId.of(zoneIdStr)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Formats an epoch timestamp for a given ZoneId with 12h (AM/PM) or 24h format
     */
    fun formatEpochForZone(
        epochMillis: Long,
        zoneId: ZoneId,
        is24Hour: Boolean = false
    ): String {
        val zonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val pattern = if (is24Hour) "HH:mm" else "hh:mm a"
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        return zonedDateTime.format(formatter)
    }

    fun formatEpochDateForZone(
        epochMillis: Long,
        zoneId: ZoneId,
        pattern: String = "MMM dd, yyyy"
    ): String {
        val zonedDateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        return zonedDateTime.format(formatter)
    }

    fun getCurrentTimeFormatted(zoneId: ZoneId, is24Hour: Boolean = false): String {
        return formatEpochForZone(System.currentTimeMillis(), zoneId, is24Hour)
    }

    fun getGmtOffsetString(zoneId: ZoneId): String {
        val now = ZonedDateTime.now(zoneId)
        return now.offset.id
    }

    /**
     * Exhaustive curated world timezone registry with flags and friendly names,
     * covering Japan, Russia, USA, UK, India, and all major global regions.
     */
    val WORLD_TIMEZONES: List<TimezoneEntry> by lazy {
        listOf(
            TimezoneEntry("Asia/Tokyo", "Japan", "🇯🇵", "Tokyo"),
            TimezoneEntry("Europe/Moscow", "Russia", "🇷🇺", "Moscow"),
            TimezoneEntry("Asia/Yekaterinburg", "Russia", "🇷🇺", "Yekaterinburg"),
            TimezoneEntry("Asia/Vladivostok", "Russia", "🇷🇺", "Vladivostok"),
            TimezoneEntry("Asia/Novosibirsk", "Russia", "🇷🇺", "Novosibirsk"),
            TimezoneEntry("Asia/Kolkata", "India", "🇮🇳", "New Delhi / Kolkata"),
            TimezoneEntry("America/New_York", "United States", "🇺🇸", "New York (Eastern)"),
            TimezoneEntry("America/Chicago", "United States", "🇺🇸", "Chicago (Central)"),
            TimezoneEntry("America/Denver", "United States", "🇺🇸", "Denver (Mountain)"),
            TimezoneEntry("America/Los_Angeles", "United States", "🇺🇸", "Los Angeles (Pacific)"),
            TimezoneEntry("Europe/London", "United Kingdom", "🇬🇧", "London (GMT/BST)"),
            TimezoneEntry("Europe/Paris", "France", "🇫🇷", "Paris"),
            TimezoneEntry("Europe/Berlin", "Germany", "🇩🇪", "Berlin"),
            TimezoneEntry("Asia/Seoul", "South Korea", "🇰🇷", "Seoul"),
            TimezoneEntry("Asia/Shanghai", "China", "🇨🇳", "Beijing / Shanghai"),
            TimezoneEntry("Asia/Hong_Kong", "Hong Kong", "🇭🇰", "Hong Kong"),
            TimezoneEntry("Asia/Taipei", "Taiwan", "🇹🇼", "Taipei"),
            TimezoneEntry("Asia/Singapore", "Singapore", "🇸🇬", "Singapore"),
            TimezoneEntry("Asia/Manila", "Philippines", "🇵🇭", "Manila"),
            TimezoneEntry("Asia/Jakarta", "Indonesia", "🇮🇩", "Jakarta"),
            TimezoneEntry("Asia/Bangkok", "Thailand", "🇹🇭", "Bangkok"),
            TimezoneEntry("Asia/Ho_Chi_Minh", "Vietnam", "🇻🇳", "Ho Chi Minh City"),
            TimezoneEntry("Australia/Sydney", "Australia", "🇦🇺", "Sydney"),
            TimezoneEntry("Australia/Melbourne", "Australia", "🇦🇺", "Melbourne"),
            TimezoneEntry("Australia/Perth", "Australia", "🇦🇺", "Perth"),
            TimezoneEntry("Pacific/Auckland", "New Zealand", "🇳🇿", "Auckland"),
            TimezoneEntry("America/Toronto", "Canada", "🇨🇦", "Toronto"),
            TimezoneEntry("America/Vancouver", "Canada", "🇨🇦", "Vancouver"),
            TimezoneEntry("America/Sao_Paulo", "Brazil", "🇧🇷", "São Paulo"),
            TimezoneEntry("America/Mexico_City", "Mexico", "🇲🇽", "Mexico City"),
            TimezoneEntry("America/Buenos_Aires", "Argentina", "🇦🇷", "Buenos Aires"),
            TimezoneEntry("Asia/Dubai", "United Arab Emirates", "🇦🇪", "Dubai"),
            TimezoneEntry("Asia/Riyadh", "Saudi Arabia", "🇸🇦", "Riyadh"),
            TimezoneEntry("Europe/Rome", "Italy", "🇮🇹", "Rome"),
            TimezoneEntry("Europe/Madrid", "Spain", "🇪🇸", "Madrid"),
            TimezoneEntry("Europe/Amsterdam", "Netherlands", "🇳🇱", "Amsterdam"),
            TimezoneEntry("Europe/Kyiv", "Ukraine", "🇺🇦", "Kyiv"),
            TimezoneEntry("Europe/Warsaw", "Poland", "🇵🇱", "Warsaw"),
            TimezoneEntry("Europe/Istanbul", "Turkey", "🇹🇷", "Istanbul"),
            TimezoneEntry("Africa/Cairo", "Egypt", "🇪🇬", "Cairo"),
            TimezoneEntry("Africa/Johannesburg", "South Africa", "🇿🇦", "Johannesburg")
        ).sortedBy { it.countryName }
    }
}
