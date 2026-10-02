package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.AlarmReceiver
import com.example.alarm.AlarmRingingActivity
import com.example.alarm.AlarmScheduler
import com.example.alarm.NotificationHelper
import com.example.ui.theme.SpiderThemePreset
import com.example.viewmodel.SpiderClockViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SpiderClockViewModel,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentTheme by viewModel.theme.collectAsState()
    val is24Hour by viewModel.is24Hour.collectAsState()
    val isSmoothSecond by viewModel.isSmoothSecond.collectAsState()
    val showCogs by viewModel.showCogs.collectAsState()
    val defaultSnooze by viewModel.defaultSnooze.collectAsState()

    var hasNotificationPerm by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPerm = granted
        coroutineScope.launch {
            if (granted) {
                snackbarHostState.showSnackbar("Push notification permission granted!")
            } else {
                snackbarHostState.showSnackbar("Notification permission is required for alarm alerts.")
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        color = currentTheme.backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "Spider Settings",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Notification permissions, alarm tests & arachnid theme",
                    color = currentTheme.secondaryColor,
                    fontSize = 13.sp
                )
            }

            // 1. Push Notification Status & Test
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = currentTheme.cardBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, currentTheme.secondaryColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (hasNotificationPerm) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (hasNotificationPerm) Color(0xFF00E676) else Color(0xFFFF9100),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Push Notification Support",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (hasNotificationPerm) "Active & High Priority" else "Permission Required",
                                    color = if (hasNotificationPerm) Color(0xFF00E676) else Color(0xFFFF9100),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (!hasNotificationPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Button(
                                onClick = {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor),
                                modifier = Modifier.testTag("request_notification_perm_button")
                            ) {
                                Text("Grant", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Test your device's push notifications to verify you never miss wake-ups or events.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.triggerTestNotification()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("📢 Spider push notification sent!")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_push_notification_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.cardBackground.copy(alpha = 0.8f))
                        ) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Push Alert", fontSize = 12.sp, color = currentTheme.primaryColor)
                        }

                        Button(
                            onClick = {
                                val intent = Intent(context, AlarmRingingActivity::class.java).apply {
                                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, 9999L)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("test_alarm_ringing_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accentColor)
                        ) {
                            Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Ringing UI", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 2. Theme Selection
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = currentTheme.cardBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, currentTheme.secondaryColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ColorLens, contentDescription = null, tint = currentTheme.accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Spider Color Theme", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SpiderThemePreset.values().forEach { preset ->
                            val isSelected = currentTheme == preset
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) currentTheme.backgroundColor else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) preset.accentColor else currentTheme.secondaryColor.copy(alpha = 0.2f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { viewModel.setTheme(preset) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("theme_preset_${preset.name}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(preset.primaryColor)
                                            .border(1.dp, preset.accentColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = preset.title,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }

                                if (isSelected) {
                                    Text("Active", color = preset.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Clock & Display Customization
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = currentTheme.cardBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, currentTheme.secondaryColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Animation, contentDescription = null, tint = currentTheme.accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clock Animation & Dial", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 24 Hour Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("24-Hour Time Format", color = Color.White, fontSize = 14.sp)
                            Text("Use 00:00 to 23:59 display", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                        Switch(
                            checked = is24Hour,
                            onCheckedChange = { viewModel.set24Hour(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = currentTheme.accentColor),
                            modifier = Modifier.testTag("24hour_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Smooth Sweep Hand Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Smooth Second Hand Sweep", color = Color.White, fontSize = 14.sp)
                            Text("Fluid arachnid motion vs standard tick", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                        Switch(
                            checked = isSmoothSecond,
                            onCheckedChange = { viewModel.setSmoothSecond(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = currentTheme.accentColor),
                            modifier = Modifier.testTag("smooth_second_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mechanical Cogs Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Mechanical Background Cogs", color = Color.White, fontSize = 14.sp)
                            Text("Rotating gothic gears from original web clock", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                        Switch(
                            checked = showCogs,
                            onCheckedChange = { viewModel.setShowCogs(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = currentTheme.accentColor),
                            modifier = Modifier.testTag("cogs_switch")
                        )
                    }
                }
            }

            // 4. About Project
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = currentTheme.cardBackground.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = currentTheme.secondaryColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("About Spider Clock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Original spider clock concept by Piyush Soni (ps-spider-clock), converted into a native Android mobile application featuring interactive silk physics, high-priority push notifications, wake-up challenges, Room persistence, and reliable wake alarms.",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
