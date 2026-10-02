package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.alarm.AlarmSoundType
import com.example.alarm.SoundManager
import com.example.model.AlarmCategory
import com.example.model.AlarmItem
import com.example.model.WakeChallenge
import com.example.ui.theme.SpiderThemePreset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditAlarmDialog(
    initialAlarm: AlarmItem? = null,
    theme: SpiderThemePreset,
    is24Hour: Boolean = false,
    onDismissRequest: () -> Unit,
    onSave: (AlarmItem) -> Unit
) {
    val context = LocalContext.current
    val isEdit = initialAlarm != null
    val initialHour = initialAlarm?.hour ?: 7
    val initialMinute = initialAlarm?.minute ?: 30

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour
    )

    var title by remember { mutableStateOf(initialAlarm?.title ?: "Wake Up") }
    var category by remember { mutableStateOf(initialAlarm?.category ?: AlarmCategory.WAKEUP) }
    var daysMask by remember { mutableIntStateOf(initialAlarm?.daysMask ?: 0) }
    var challenge by remember { mutableStateOf(initialAlarm?.challenge ?: WakeChallenge.NONE) }
    var snoozeMinutes by remember { mutableIntStateOf(initialAlarm?.snoozeMinutes ?: 5) }
    var soundType by remember { mutableStateOf(initialAlarm?.soundType ?: AlarmSoundType.SYSTEM_ALARM) }
    var vibrate by remember { mutableStateOf(initialAlarm?.vibrate ?: true) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            SoundManager.stopSound()
        }
    }

    val daysLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val snoozeOptions = listOf(5, 10, 15, 20, 30)

    Dialog(
        onDismissRequest = {
            SoundManager.stopSound()
            onDismissRequest()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, theme.cardBackground.copy(alpha = 0.8f), RoundedCornerShape(28.dp))
                .testTag("add_edit_alarm_dialog"),
            color = theme.backgroundColor
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🕷️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEdit) "Edit Spider Alarm" else "New Spider Alarm",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = {
                            SoundManager.stopSound()
                            onDismissRequest()
                        },
                        modifier = Modifier.testTag("close_alarm_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Time Picker Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TimePicker(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                clockDialColor = theme.backgroundColor,
                                clockDialSelectedContentColor = Color.Black,
                                clockDialUnselectedContentColor = Color.White,
                                selectorColor = theme.accentColor,
                                containerColor = theme.cardBackground,
                                periodSelectorBorderColor = theme.accentColor,
                                periodSelectorSelectedContainerColor = theme.accentColor,
                                periodSelectorUnselectedContainerColor = theme.backgroundColor,
                                periodSelectorSelectedContentColor = Color.White,
                                periodSelectorUnselectedContentColor = Color.White.copy(alpha = 0.7f),
                                timeSelectorSelectedContainerColor = theme.accentColor,
                                timeSelectorUnselectedContainerColor = theme.backgroundColor,
                                timeSelectorSelectedContentColor = Color.White,
                                timeSelectorUnselectedContentColor = Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Alarm / Event Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accentColor,
                        unfocusedBorderColor = theme.secondaryColor.copy(alpha = 0.4f),
                        focusedLabelColor = theme.accentColor,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alarm_title_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Recurring Options (Daily, Weekly, Weekdays, Custom)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = theme.accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recurring Schedule",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            Pair("Once", 0),
                            Pair("Daily (Every Day)", 127),
                            Pair("Weekdays (Mon-Fri)", 62),
                            Pair("Weekends (Sat-Sun)", 65)
                        )
                        presets.forEach { (label, mask) ->
                            val isSelected = daysMask == mask
                            FilterChip(
                                selected = isSelected,
                                onClick = { daysMask = mask },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.accentColor,
                                    selectedLabelColor = Color.White,
                                    containerColor = theme.cardBackground,
                                    labelColor = Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Custom Days Circles
                    Text(
                        text = "Or select custom days:",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 0..6) {
                            val isSelected = (daysMask and (1 shl i)) != 0
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) theme.accentColor else theme.cardBackground)
                                    .clickable {
                                        daysMask = if (isSelected) {
                                            daysMask and (1 shl i).inv()
                                        } else {
                                            daysMask or (1 shl i)
                                        }
                                    }
                                    .testTag("day_chip_${daysLabels[i]}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = daysLabels[i].first().toString(),
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Alarm Sound Variety Selection
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = theme.accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Alarm Sound Tone",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Preview Audio Button
                        Button(
                            onClick = {
                                if (isPreviewPlaying) {
                                    SoundManager.stopSound()
                                    isPreviewPlaying = false
                                } else {
                                    SoundManager.playSound(context, soundType, loop = true)
                                    isPreviewPlaying = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPreviewPlaying) Color(0xFFFF5252) else theme.cardBackground
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("preview_sound_button")
                        ) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPreviewPlaying) "Stop" else "Preview", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AlarmSoundType.values().forEach { st ->
                            val isSelected = soundType == st
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) theme.accentColor.copy(alpha = 0.2f) else theme.cardBackground)
                                    .border(
                                        1.dp,
                                        if (isSelected) theme.accentColor else theme.secondaryColor.copy(alpha = 0.2f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        soundType = st
                                        if (isPreviewPlaying) {
                                            SoundManager.playSound(context, st, loop = true)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("sound_${st.id}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = st.displayName,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = st.description,
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 10.sp
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = theme.accentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Snooze Options (5m, 10m, 15m, 20m, 30m)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = null,
                            tint = theme.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Snooze Duration",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        snoozeOptions.forEach { mins ->
                            val isSelected = snoozeMinutes == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) theme.accentColor else theme.cardBackground)
                                    .border(
                                        1.dp,
                                        if (isSelected) theme.accentColor else theme.secondaryColor.copy(alpha = 0.2f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { snoozeMinutes = mins }
                                    .padding(vertical = 8.dp)
                                    .testTag("snooze_${mins}m"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins}m",
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Wake-Up Challenge (Never Miss Guarantee)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = theme.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Wake-Up Mission Challenge",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WakeChallenge.values().forEach { c ->
                            val isSelected = challenge == c
                            FilterChip(
                                selected = isSelected,
                                onClick = { challenge = c },
                                label = { Text(c.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = theme.accentColor,
                                    selectedLabelColor = Color.White,
                                    containerColor = theme.cardBackground,
                                    labelColor = Color.White.copy(alpha = 0.8f)
                                ),
                                modifier = Modifier.testTag("challenge_${c.name}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vibration switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vibration", color = Color.White, fontSize = 14.sp)
                    }
                    Switch(
                        checked = vibrate,
                        onCheckedChange = { vibrate = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = theme.accentColor
                        ),
                        modifier = Modifier.testTag("vibrate_switch")
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            SoundManager.stopSound()
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("cancel_alarm_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.8f))
                    }

                    Button(
                        onClick = {
                            SoundManager.stopSound()
                            val updatedAlarm = AlarmItem(
                                id = initialAlarm?.id ?: 0L,
                                hour = timePickerState.hour,
                                minute = timePickerState.minute,
                                title = title.ifBlank { "Wake Up" },
                                category = category,
                                daysMask = daysMask,
                                isEnabled = true,
                                vibrate = vibrate,
                                challenge = challenge,
                                snoozeMinutes = snoozeMinutes,
                                soundType = soundType
                            )
                            onSave(updatedAlarm)
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("save_alarm_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Alarm", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
