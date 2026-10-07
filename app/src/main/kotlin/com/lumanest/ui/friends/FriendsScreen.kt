package com.lumanest.ui.friends

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import com.lumanest.models.Friend
import com.lumanest.timezone.TimezoneEntry
import com.lumanest.timezone.TimezoneHelper
import com.lumanest.ui.theme.*
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    friends: List<Friend>,
    userZoneId: ZoneId,
    is24Hour: Boolean,
    onAddFriend: (String, String, String) -> Unit,
    onRemoveFriend: (String) -> Unit,
    onReorderFriends: (List<Friend>) -> Unit,
    onUpdateFriend: (String, String, String, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingFriend by remember { mutableStateOf<Friend?>(null) }

    // Live second ticker for live wall-clock time
    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val selfFriendId = "user_self_card"

    // Derive user's matching country flag from TimezoneHelper or default to 🇮🇳 / 🌍
    val userCountryFlag = remember(userZoneId) {
        TimezoneHelper.WORLD_TIMEZONES.firstOrNull { it.id == userZoneId.id }?.flag ?: "🇮🇳"
    }

    val selfFriend = remember(userZoneId, userCountryFlag) {
        Friend(
            id = selfFriendId,
            name = "You",
            timezoneId = userZoneId.id,
            countryFlag = userCountryFlag,
            notes = "Local Device Time",
            sortOrder = 0
        )
    }

    // Database sync: keep local draggable state synchronized when DB emits unless actively dragging
    val currentList = remember { mutableStateListOf<Friend>() }
    var isDraggingActive by remember { mutableStateOf(false) }

    LaunchedEffect(friends) {
        if (!isDraggingActive) {
            currentList.clear()
            if (friends.isEmpty()) {
                currentList.add(selfFriend)
            } else {
                currentList.addAll(friends)
            }
        }
    }

    // Live Drag State Tracking
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val currentOnReorderFriends by rememberUpdatedState(onReorderFriends)
    val itemHeightPx = 220f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), DeepMidnight, Color(0xFF090D14))
                )
            )
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Friend Timezones",
                        style = MaterialTheme.typography.headlineMedium,
                        color = DogFluffWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hold & drag to reorder • Double-tap to edit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CozyWarmAmber),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Friend", tint = DeepMidnight)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Add", color = DeepMidnight, fontWeight = FontWeight.Bold)
                }
            }
        }

        itemsIndexed(currentList, key = { _, friend -> friend.id }) { index, friend ->
            val isBeingDragged = draggingIndex == index

            val elevation by animateDpAsState(
                targetValue = if (isBeingDragged) 16.dp else 0.dp,
                label = "dragElevation"
            )
            val scale by animateFloatAsState(
                targetValue = if (isBeingDragged) 1.04f else 1f,
                label = "dragScale"
            )

            Box(
                modifier = Modifier
                    .zIndex(if (isBeingDragged) 10f else 1f)
                    .graphicsLayer {
                        translationY = if (isBeingDragged) dragOffsetY else 0f
                        scaleX = scale
                        scaleY = scale
                    }
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                isDraggingActive = true
                                draggingIndex = index
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y

                                val currentIndex = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                val offsetSteps = (dragOffsetY / itemHeightPx).toInt()
                                val targetIndex = (currentIndex + offsetSteps).coerceIn(0, currentList.lastIndex)

                                if (targetIndex != currentIndex && targetIndex in 0..currentList.lastIndex) {
                                    val item = currentList.removeAt(currentIndex)
                                    currentList.add(targetIndex, item)
                                    draggingIndex = targetIndex
                                    dragOffsetY = 0f
                                }
                            },
                            onDragEnd = {
                                isDraggingActive = false
                                draggingIndex = null
                                dragOffsetY = 0f
                                // Persist snapshot of entire reordered list directly to Room
                                currentOnReorderFriends(currentList.toList())
                            },
                            onDragCancel = {
                                isDraggingActive = false
                                draggingIndex = null
                                dragOffsetY = 0f
                            }
                        )
                    }
            ) {
                PureDragFriendCard(
                    friend = friend,
                    currentMillis = currentMillis,
                    is24Hour = is24Hour,
                    isSelf = friend.id == selfFriendId,
                    isBeingDragged = isBeingDragged,
                    elevation = elevation,
                    onDoubleTap = { editingFriend = friend },
                    onRemove = {
                        if (friend.id != selfFriendId) {
                            onRemoveFriend(friend.id)
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showAddDialog) {
        FullWorldTimezoneDialog(
            title = "Add Sky Friend",
            initialName = "",
            initialTzId = TimezoneHelper.WORLD_TIMEZONES.first().id,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, tz, flag ->
                onAddFriend(name, tz, flag)
                showAddDialog = false
            }
        )
    }

    if (editingFriend != null) {
        val f = editingFriend!!
        FullWorldTimezoneDialog(
            title = "Edit Friend",
            initialName = f.name,
            initialTzId = f.timezoneId,
            onDismiss = { editingFriend = null },
            onConfirm = { name, tz, flag ->
                onUpdateFriend(f.id, name, tz, flag)
                // Update local memory list in place
                val targetIdx = currentList.indexOfFirst { it.id == f.id }
                if (targetIdx != -1) {
                    currentList[targetIdx] = currentList[targetIdx].copy(name = name, timezoneId = tz, countryFlag = flag)
                }
                editingFriend = null
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PureDragFriendCard(
    friend: Friend,
    currentMillis: Long,
    is24Hour: Boolean,
    isSelf: Boolean,
    isBeingDragged: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    onDoubleTap: () -> Unit,
    onRemove: () -> Unit
) {
    val friendZone = remember(friend.timezoneId) {
        try { ZoneId.of(friend.timezoneId) } catch (_: Exception) { ZoneId.of("UTC") }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {},
                onDoubleClick = onDoubleTap
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isBeingDragged -> Color(0xFF1E293B)
                isSelf -> NightSurfaceVariant
                else -> NightSurface
            }
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                when {
                    isBeingDragged -> CozyWarmAmber
                    isSelf -> CozyWarmAmber.copy(alpha = 0.5f)
                    else -> CardBorder
                }
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isSelf) NightSurface else NightSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = friend.countryFlag, fontSize = 24.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = friend.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = DogFluffWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${friend.timezoneId.substringAfterLast('/')} • ${TimezoneHelper.getGmtOffsetString(friendZone)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSlate
                    )
                }
            }

            // Time & Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = TimezoneHelper.formatEpochForZone(currentMillis, friendZone, is24Hour),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isSelf) CozyWarmAmber else StarlightBlue,
                    fontWeight = FontWeight.Bold
                )
                if (!isSelf) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Friend",
                            tint = MutedSlate.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Searchable World Timezone Picker & Editor covering Japan, Russia, USA, and all global regions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullWorldTimezoneDialog(
    title: String = "Add Sky Friend",
    initialName: String = "",
    initialTzId: String = TimezoneHelper.WORLD_TIMEZONES.first().id,
    onDismiss: () -> Unit,
    onConfirm: (name: String, timezoneId: String, flag: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedEntry by remember {
        mutableStateOf(
            TimezoneHelper.WORLD_TIMEZONES.firstOrNull { it.id == initialTzId } ?: TimezoneHelper.WORLD_TIMEZONES.first()
        )
    }

    val filteredList = remember(searchQuery) {
        if (searchQuery.isBlank()) TimezoneHelper.WORLD_TIMEZONES
        else TimezoneHelper.WORLD_TIMEZONES.filter {
            it.countryName.contains(searchQuery, ignoreCase = true) ||
            it.cityName.contains(searchQuery, ignoreCase = true) ||
            it.id.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Friend's Name") },
                    placeholder = { Text("e.g. Orange, Janet, Haru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Country or City") },
                    placeholder = { Text("e.g. Russia, Japan, USA, London") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = CozyWarmAmber)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Selected: ${selectedEntry.flag} ${selectedEntry.countryName} (${selectedEntry.cityName})",
                    style = MaterialTheme.typography.bodySmall,
                    color = CozyWarmAmber,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredList.size) { index ->
                        val entry = filteredList[index]
                        val isSelected = entry.id == selectedEntry.id

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) CozyWarmAmber.copy(alpha = 0.2f) else NightSurface,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CozyWarmAmber) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedEntry = entry }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = entry.flag, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${entry.countryName} — ${entry.cityName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) CozyWarmAmber else DogFluffWhite,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = entry.id,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedSlate
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (name.isBlank()) selectedEntry.countryName else name.trim()
                    onConfirm(finalName, selectedEntry.id, selectedEntry.flag)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CozyWarmAmber),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Save", color = DeepMidnight, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = MutedSlate)
            }
        }
    )
}
