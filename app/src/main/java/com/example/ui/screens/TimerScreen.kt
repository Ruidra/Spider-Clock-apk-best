package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.SpiderClockViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TimerScreen(viewModel: SpiderClockViewModel) {
    val theme by viewModel.theme.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Timer, 1: Stopwatch

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("timer_screen"),
        color = theme.backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = theme.cardBackground,
                contentColor = theme.accentColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = theme.accentColor
                    )
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, theme.cardBackground, RoundedCornerShape(16.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Web Timer", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_web_timer")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Stopwatch", fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_stopwatch")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                WebTimerContent(viewModel = viewModel)
            } else {
                StopwatchContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun WebTimerContent(viewModel: SpiderClockViewModel) {
    val theme by viewModel.theme.collectAsState()
    val totalSeconds by viewModel.timerTotalSeconds.collectAsState()
    val remainingSeconds by viewModel.timerRemainingSeconds.collectAsState()
    val isRunning by viewModel.timerRunning.collectAsState()

    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    val presetDurations = listOf(
        Pair("1m", 60),
        Pair("3m", 180),
        Pair("5m", 300),
        Pair("10m", 600),
        Pair("15m", 900),
        Pair("25m", 1500)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Quick preset chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            presetDurations.forEach { (label, secs) ->
                val isSelected = totalSeconds == secs
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) theme.accentColor else theme.cardBackground)
                        .border(1.dp, if (isSelected) theme.accentColor else theme.secondaryColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable { viewModel.setTimerSeconds(secs) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("timer_preset_$label")
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Circular Web Countdown Canvas
        Box(
            modifier = Modifier
                .size(260.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f - 12.dp.toPx()

                // Spider web background ring
                drawCircle(
                    color = theme.cardBackground,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 12.dp.toPx())
                )

                // Concentric inner web lines
                for (r in listOf(radius * 0.4f, radius * 0.7f)) {
                    drawCircle(
                        color = theme.secondaryColor.copy(alpha = 0.15f),
                        radius = r,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Active progress arc
                val sweepAngle = progress * 360f
                drawArc(
                    color = theme.accentColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )

                // Spider positioned at the end of the arc
                val angleRad = ((-90f + sweepAngle) * PI / 180f).toFloat()
                val spiderX = center.x + radius * cos(angleRad)
                val spiderY = center.y + radius * sin(angleRad)
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(spiderX, spiderY))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🕷️", fontSize = 28.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeFormatted,
                    color = Color.White,
                    fontSize = 42.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.testTag("timer_countdown_text")
                )
                Text(
                    text = if (isRunning) "Weaving time..." else if (remainingSeconds == 0) "Web Complete!" else "Ready",
                    color = theme.secondaryColor,
                    fontSize = 12.sp
                )
            }
        }

        // Action Buttons: Play/Pause, Reset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { viewModel.resetTimer() },
                modifier = Modifier
                    .size(56.dp)
                    .testTag("timer_reset_button"),
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(24.dp))

            Button(
                onClick = {
                    if (isRunning) viewModel.pauseTimer() else viewModel.startTimer()
                },
                modifier = Modifier
                    .size(72.dp)
                    .testTag("timer_play_pause_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun StopwatchContent(viewModel: SpiderClockViewModel) {
    val theme by viewModel.theme.collectAsState()
    val isRunning by viewModel.stopwatchRunning.collectAsState()
    val elapsedMillis by viewModel.stopwatchElapsed.collectAsState()
    val laps by viewModel.stopwatchLaps.collectAsState()

    val totalSeconds = elapsedMillis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millisPart = (elapsedMillis % 1000) / 10

    val timeFormatted = String.format("%02d:%02d.%02d", minutes, seconds, millisPart)

    val infiniteTransition = rememberInfiniteTransition(label = "stopwatchGear")
    val gearAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stopwatchGearAngle"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gear & Time readout
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .border(1.dp, theme.secondaryColor.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mini rotating spider gear
                Canvas(modifier = Modifier.size(48.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val rot = if (isRunning) gearAngle else 0f
                    rotate(rot, pivot = center) {
                        drawCircle(
                            color = theme.accentColor,
                            radius = size.width / 2f - 4f,
                            center = center,
                            style = Stroke(width = 3f)
                        )
                        drawLine(
                            color = theme.accentColor,
                            start = Offset(center.x - 14f, center.y),
                            end = Offset(center.x + 14f, center.y),
                            strokeWidth = 2f
                        )
                        drawLine(
                            color = theme.accentColor,
                            start = Offset(center.x, center.y - 14f),
                            end = Offset(center.x, center.y + 14f),
                            strokeWidth = 2f
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = timeFormatted,
                    color = Color.White,
                    fontSize = 44.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.testTag("stopwatch_time_text")
                )

                Text(
                    text = if (isRunning) "Silk tracking in motion" else "Stopped",
                    color = theme.secondaryColor,
                    fontSize = 12.sp
                )
            }
        }

        // Stopwatch Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { viewModel.resetStopwatch() },
                modifier = Modifier
                    .size(54.dp)
                    .testTag("stopwatch_reset_button"),
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
            }

            Button(
                onClick = {
                    if (isRunning) viewModel.pauseStopwatch() else viewModel.startStopwatch()
                },
                modifier = Modifier
                    .size(68.dp)
                    .testTag("stopwatch_play_pause_button"),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            OutlinedButton(
                onClick = { viewModel.recordLap() },
                enabled = isRunning,
                modifier = Modifier
                    .size(54.dp)
                    .testTag("stopwatch_lap_button"),
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Flag, contentDescription = "Lap", tint = if (isRunning) theme.accentColor else Color.Gray)
            }
        }

        // Laps List
        if (laps.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.cardBackground)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(laps) { index, lapMillis ->
                    val lapTotalSeconds = lapMillis / 1000
                    val lm = lapTotalSeconds / 60
                    val ls = lapTotalSeconds % 60
                    val lms = (lapMillis % 1000) / 10
                    val lapStr = String.format("%02d:%02d.%02d", lm, ls, lms)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lap #${laps.size - index}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = lapStr,
                            color = theme.accentColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
