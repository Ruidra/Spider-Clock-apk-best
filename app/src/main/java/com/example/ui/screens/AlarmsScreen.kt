package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.AlarmScheduler
import com.example.model.AlarmCategory
import com.example.model.AlarmItem
import com.example.model.WakeChallenge
import com.example.ui.dialogs.AddEditAlarmDialog
import com.example.viewmodel.SpiderClockViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    viewModel: SpiderClockViewModel,
    snackbarHostState: SnackbarHostState
) {
    val alarms by viewModel.allAlarms.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val is24Hour by viewModel.is24Hour.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var selectedAlarmForEdit by remember { mutableStateOf<AlarmItem?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<AlarmCategory?>(null) }

    val coroutineScope = rememberCoroutineScope()

    val filteredAlarms = remember(alarms, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) {
            alarms
        } else {
            alarms.filter { it.category == selectedCategoryFilter }
        }
    }

    Scaffold(
        containerColor = theme.backgroundColor,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedAlarmForEdit = null
                    showDialog = true
                },
                containerColor = theme.accentColor,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_alarm_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Alarm")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("alarms_screen")
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Scheduled Alarms",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Arachnid wake-up & event protection",
                        color = theme.secondaryColor,
                        fontSize = 13.sp
                    )
                }

                val activeCount = alarms.count { it.isEnabled }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
                    modifier = Modifier.border(1.dp, theme.accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                ) {
                    Text(
                        text = "$activeCount Active",
                        color = theme.accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = theme.accentColor,
                        selectedLabelColor = Color.White,
                        containerColor = theme.cardBackground,
                        labelColor = Color.White.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag("filter_all")
                )

                AlarmCategory.values().take(3).forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                        },
                        label = { Text(cat.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = theme.accentColor,
                            selectedLabelColor = Color.White,
                            containerColor = theme.cardBackground,
                            labelColor = Color.White.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // Alarms List or Empty State
            if (filteredAlarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("🕸️", fontSize = 54.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Alarms in Web",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button to weave a new wake-up alarm or important event reminder.",
                            color = theme.secondaryColor,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredAlarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            theme = theme,
                            is24Hour = is24Hour,
                            onToggle = { enabled ->
                                viewModel.toggleAlarm(alarm, enabled)
                                coroutineScope.launch {
                                    val status = if (enabled) "armed" else "disarmed"
                                    snackbarHostState.showSnackbar("Alarm '${alarm.title}' $status")
                                }
                            },
                            onEdit = {
                                selectedAlarmForEdit = alarm
                                showDialog = true
                            },
                            onDelete = {
                                viewModel.deleteAlarm(alarm)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Alarm '${alarm.title}' removed")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddEditAlarmDialog(
            initialAlarm = selectedAlarmForEdit,
            theme = theme,
            is24Hour = is24Hour,
            onDismissRequest = { showDialog = false },
            onSave = { alarmToSave ->
                viewModel.saveAlarm(alarmToSave)
                showDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Alarm '${alarmToSave.title}' saved")
                }
            }
        )
    }
}

@Composable
fun AlarmCard(
    alarm: AlarmItem,
    theme: com.example.ui.theme.SpiderThemePreset,
    is24Hour: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val nextTriggerMillis = remember(alarm) {
        AlarmScheduler.calculateNextTriggerMillis(alarm.hour, alarm.minute, alarm.daysMask)
    }
    val countdown = AlarmScheduler.formatCountdown(nextTriggerMillis)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) theme.cardBackground else theme.cardBackground.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onEdit() }
            .border(
                1.dp,
                if (alarm.isEnabled) theme.accentColor.copy(alpha = 0.4f) else Color.Transparent,
                RoundedCornerShape(22.dp)
            )
            .testTag("alarm_card_${alarm.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Time, Label, and Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = alarm.formattedTime(is24Hour),
                        color = if (alarm.isEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = alarm.title,
                        color = if (alarm.isEnabled) theme.primaryColor else theme.secondaryColor.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = theme.accentColor,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = theme.backgroundColor
                    ),
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Badges & Details (Repeat days, Challenge, Countdown)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Repeat Days Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.backgroundColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = alarm.getDaysDescription(),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Sound Tone Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.backgroundColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = alarm.soundType.displayName,
                            color = theme.primaryColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Snooze Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.backgroundColor)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "💤 ${alarm.snoozeMinutes}m",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }

                    // Challenge Pill (if any)
                    if (alarm.challenge != WakeChallenge.NONE) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.accentColor.copy(alpha = 0.15f))
                                .border(1.dp, theme.accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = null,
                                    tint = theme.accentColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = alarm.challenge.displayName,
                                    color = theme.accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Countdown and Action buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (alarm.isEnabled) {
                        Text(
                            text = countdown,
                            color = theme.accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_alarm_${alarm.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_alarm_${alarm.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF5252).copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
