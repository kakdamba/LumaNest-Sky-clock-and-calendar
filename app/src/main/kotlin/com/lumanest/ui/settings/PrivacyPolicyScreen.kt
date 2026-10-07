package com.lumanest.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumanest.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy Policy",
                        color = DogFluffWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DogFluffWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NightSurface
                )
            )
        },
        containerColor = DeepMidnight
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(NightSurface, DeepMidnight, Color(0xFF090D14))
                    )
                )
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NightSurfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Last Updated: October 7, 2026",
                            style = MaterialTheme.typography.labelMedium,
                            color = CozyWarmAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "LumaNest is an independent, unofficial companion application created for players of Sky: Children of the Light.\n\nLumaNest is not affiliated with, sponsored by, or endorsed by thatgamecompany. Sky: Children of the Light and related names, trademarks, and intellectual property belong to their respective owners.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DogFluffWhite,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            item {
                PolicySection(
                    title = "1. Information We Handle",
                    content = "LumaNest is designed to provide event schedules, time conversions, notifications, widgets, calendar features, and other companion functionality.\n\nDepending on the features you choose to use, LumaNest may process information such as:\n• Your device timezone\n• Friend names and timezone information that you manually enter\n• Notification settings\n• Calendar information when you explicitly use a calendar feature\n• App settings and preferences\n• Technical information necessary to operate, secure, troubleshoot, or improve the application, if such information is collected by the final version of the app\n\nWe only request access to information that is necessary for the feature you choose to use."
                )
            }

            item {
                PolicySection(
                    title = "2. Timezone",
                    content = "LumaNest can use your device's configured timezone to display event times in your local time.\n\nYou may also choose a timezone manually.\n\nLumaNest does not require access to your precise physical location simply to determine your timezone."
                )
            }

            item {
                PolicySection(
                    title = "3. Friend Timezones",
                    content = "You may optionally create local friend entries containing information such as a nickname and timezone.\n\nThese entries are intended to help you compare event times with friends.\n\nUnless a future LumaNest feature explicitly provides otherwise and clearly informs you, friend information is stored locally on your device and is not uploaded as part of a LumaNest account."
                )
            }

            item {
                PolicySection(
                    title = "4. Notifications",
                    content = "Notifications are optional.\n\nIf you grant notification permission, LumaNest may use it to remind you about Sky events according to your settings.\n\nYou can control or revoke notification permission through Android."
                )
            }

            item {
                PolicySection(
                    title = "5. Calendar Features",
                    content = "Calendar functionality is optional.\n\nIf you choose to add an event to a calendar, LumaNest may interact with the calendar service or Android calendar interface required to perform that action.\n\nLumaNest does not need calendar access for its basic event-clock functionality.\n\nWe will request only the calendar access required by the implemented feature."
                )
            }

            item {
                PolicySection(
                    title = "6. Event Information",
                    content = "LumaNest may use publicly available information from official and community sources to provide event schedules.\n\nSources may include official announcements from thatgamecompany as well as publicly available community-maintained schedules.\n\nEvent information may be stored locally on your device so that portions of LumaNest can continue to function when you are offline.\n\nEvent information is provided for informational purposes and may occasionally change or contain errors.\n\nWhere practical, LumaNest will identify the source and/or verification status of event information."
                )
            }

            item {
                PolicySection(
                    title = "7. Internet and Updates",
                    content = "LumaNest may connect to the internet to obtain updated event information, access external source links, or provide other online features.\n\nThe application is designed to retain previously available event information so that the core experience can continue when an internet connection is unavailable."
                )
            }

            item {
                PolicySection(
                    title = "8. Third-Party Services",
                    content = "Some optional features may interact with third-party services, such as calendar providers or websites containing event information.\n\nWhen you choose to use a third-party service, information handled by that service is subject to that service's own terms and privacy policy.\n\nLumaNest does not control the privacy practices of third-party services."
                )
            }

            item {
                PolicySection(
                    title = "9. Analytics, Diagnostics, and Crash Reporting",
                    content = "LumaNest operates with an offline-first privacy model and does not collect or transmit personal identifying analytics or diagnostics. If third-party diagnostic or crash-reporting services are ever introduced, this policy will identify the relevant services and explain what information they receive and why."
                )
            }

            item {
                PolicySection(
                    title = "10. Advertising",
                    content = "LumaNest's intended product model is to provide the core companion experience without intrusive advertising.\n\nIf advertising or an advertising provider is introduced in the future, this Privacy Policy and the applicable Google Play disclosures will be updated to describe the relevant data practices before the feature is released."
                )
            }

            item {
                PolicySection(
                    title = "11. Data Storage and Security",
                    content = "LumaNest will use reasonable technical and organizational measures appropriate to the information handled by the application.\n\nHowever, no software, device, network, or method of electronic storage can be guaranteed to be completely secure."
                )
            }

            item {
                PolicySection(
                    title = "12. Data Deletion",
                    content = "Where information is stored locally by LumaNest, users should be able to remove it through the application's available settings or by removing the application, subject to how Android and any third-party services handle that information.\n\nInformation stored by a third-party service must generally be managed through that service.\n\nIf LumaNest introduces accounts or server-side user data in the future, the deletion process and this Privacy Policy will be updated accordingly."
                )
            }

            item {
                PolicySection(
                    title = "13. Children's Privacy",
                    content = "LumaNest is not designed to knowingly collect personal information from children.\n\nThe application does not require a LumaNest account for its basic functionality.\n\nIf the product's audience, features, or data practices change in a way that affects children's privacy requirements, the appropriate policies and safeguards will be reviewed before release."
                )
            }

            item {
                PolicySection(
                    title = "14. Changes to This Policy",
                    content = "We may update this Privacy Policy when LumaNest's features, data practices, services, or applicable requirements change.\n\nThe Last Updated date will indicate when the policy was most recently revised."
                )
            }

            item {
                PolicySection(
                    title = "15. Contact",
                    content = "For privacy questions or concerns:\nDeveloper: KAKDAMBA\nApplication: LumaNest\nPrivacy contact: yearning-self-sulk@duck.com"
                )
            }

            item {
                PolicySection(
                    title = "16. Unofficial Fan Project",
                    content = "LumaNest is an independent fan-made companion application.\nIt is not an official Sky application and is not affiliated with, sponsored by, or endorsed by thatgamecompany.\nLumaNest does not provide access to, modify, or require your Sky account."
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = NightSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌻 LumaNest",
                        style = MaterialTheme.typography.titleMedium,
                        color = CozyWarmAmber,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Made with 🍊 and 🌻",
                        style = MaterialTheme.typography.bodySmall,
                        color = DogFluffWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Built by KAKDAMBA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSlate
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PolicySection(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NightSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(CardBorder)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = CozyWarmAmber,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall,
                color = DogFluffWhite.copy(alpha = 0.90f),
                lineHeight = 20.sp
            )
        }
    }
}
