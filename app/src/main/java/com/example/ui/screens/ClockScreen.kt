package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.ui.components.OriginalSpiderClockView
import com.example.ui.components.SpiderClockCanvas
import com.example.viewmodel.SpiderClockViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClockScreen(
    viewModel: SpiderClockViewModel,
    onNavigateToAlarms: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val theme by viewModel.theme.collectAsState()
    val is24Hour by viewModel.is24Hour.collectAsState()
    val isSmoothSecond by viewModel.isSmoothSecond.collectAsState()
    val showCogs by viewModel.showCogs.collectAsState()
    val nextAlarm by viewModel.nextUpcomingAlarm.collectAsState()
    val useOriginalRepoClock by viewModel.useOriginalRepoClock.collectAsState()

    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(500)
        }
    }

    val timeFormatter = remember(is24Hour) {
        SimpleDateFormat(if (is24Hour) "HH:mm:ss" else "hh:mm:ss a", Locale.getDefault())
    }
    val dateFormatter = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("clock_screen"),
        color = theme.backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Next Alarm pill & Mode Toggle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                // Next Alarm Pill Banner
                if (nextAlarm != null) {
                    val alarm = nextAlarm!!
                    val nextTrigger = AlarmScheduler.calculateNextTriggerMillis(alarm.hour, alarm.minute, alarm.daysMask)
                    val countdownStr = AlarmScheduler.formatCountdown(nextTrigger)

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onNavigateToAlarms() }
                            .border(1.dp, theme.accentColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .testTag("next_alarm_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Alarm",
                                tint = theme.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Next Alarm: ${alarm.formattedTime(is24Hour)}",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "($countdownStr)",
                                color = theme.accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = theme.cardBackground.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onNavigateToAlarms() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "No active alarms · Tap to set",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Digital Time Display
                Text(
                    text = timeFormatter.format(Date(currentTimeMillis)),
                    color = Color.White,
                    fontSize = 30.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.testTag("digital_time_display")
                )

                Text(
                    text = dateFormatter.format(Date(currentTimeMillis)),
                    color = theme.secondaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Middle: Interactive Spider Clock
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                OriginalSpiderClockView(
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom Section: Interaction hint and Quick Nap buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                // Interactive guidance
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = theme.secondaryColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🕷️ Spider Mechanical Clock · Silk & Morphing Hands",
                        color = theme.secondaryColor.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                // Quick Power Nap Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = theme.accentColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Quick Power Nap Wake-Up",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val napDurations = listOf(15, 30, 45, 60)
                            for (dur in napDurations) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(theme.backgroundColor)
                                        .border(1.dp, theme.secondaryColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            viewModel.scheduleQuickNap(dur)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("⚡ Nap alarm scheduled for $dur minutes!")
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 5.dp)
                                        .testTag("quick_nap_${dur}m"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+${dur}m",
                                        color = theme.primaryColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
