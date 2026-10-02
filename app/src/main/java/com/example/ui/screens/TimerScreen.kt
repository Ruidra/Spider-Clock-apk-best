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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
                            Text("Spider Timer", fontWeight = FontWeight.Bold)
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

            Spacer(modifier = Modifier.height(12.dp))

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

    var showCustomDialog by remember { mutableStateOf(false) }

    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f
    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60

    val timeFormatted = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Custom duration controls & presets
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with "Set Custom Time" button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timer Duration",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Custom Time Input Button
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showCustomDialog = true }
                        .border(1.dp, theme.accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .testTag("button_open_custom_timer_dialog")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Custom Time",
                            tint = theme.accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Set Custom Time",
                            color = theme.accentColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Direct Stepper Controls for Hours, Minutes, Seconds (when not running)
            if (!isRunning) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, theme.secondaryColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hours Stepper
                        TimeStepperColumn(
                            label = "HOURS",
                            value = totalSeconds / 3600,
                            themeAccent = theme.accentColor,
                            onIncrement = { viewModel.addTimerTime(3600) },
                            onDecrement = {
                                if (totalSeconds >= 3600) viewModel.addTimerTime(-3600)
                            },
                            tagPrefix = "hours"
                        )

                        Text(":", color = Color.White.copy(alpha = 0.5f), fontSize = 24.sp, fontWeight = FontWeight.Bold)

                        // Minutes Stepper
                        TimeStepperColumn(
                            label = "MINUTES",
                            value = (totalSeconds % 3600) / 60,
                            themeAccent = theme.accentColor,
                            onIncrement = { viewModel.addTimerTime(60) },
                            onDecrement = {
                                if (totalSeconds >= 60) viewModel.addTimerTime(-60)
                            },
                            tagPrefix = "minutes"
                        )

                        Text(":", color = Color.White.copy(alpha = 0.5f), fontSize = 24.sp, fontWeight = FontWeight.Bold)

                        // Seconds Stepper
                        TimeStepperColumn(
                            label = "SECONDS",
                            value = totalSeconds % 60,
                            themeAccent = theme.accentColor,
                            onIncrement = { viewModel.addTimerTime(5) },
                            onDecrement = {
                                if (totalSeconds >= 5) viewModel.addTimerTime(-5)
                            },
                            tagPrefix = "seconds"
                        )
                    }
                }
            } else {
                // If running, show quick +1m, +5m buttons to extend on the fly
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add time while running:",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    listOf(30 to "+30s", 60 to "+1m", 300 to "+5m").forEach { (secs, label) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.cardBackground)
                                .border(1.dp, theme.accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { viewModel.addTimerTime(secs) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(label, color = theme.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }
            }
        }

        // Circular Web Countdown Canvas
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f - 10.dp.toPx()

                // Spider web background ring
                drawCircle(
                    color = theme.cardBackground,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 10.dp.toPx())
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
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )

                // Spider indicator positioned at the end of the arc
                val angleRad = ((-90f + sweepAngle) * PI / 180f).toFloat()
                val spiderX = center.x + radius * cos(angleRad)
                val spiderY = center.y + radius * sin(angleRad)
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(spiderX, spiderY))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🕷️", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeFormatted,
                    color = Color.White,
                    fontSize = if (hours > 0) 32.sp else 38.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.testTag("timer_countdown_text")
                )
                Text(
                    text = if (isRunning) "Weaving countdown..." else if (remainingSeconds == 0) "Timer Complete!" else "Ready to start",
                    color = theme.secondaryColor,
                    fontSize = 11.sp
                )
            }
        }

        // Action Buttons: Play/Pause, Reset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { viewModel.resetTimer() },
                modifier = Modifier
                    .size(54.dp)
                    .testTag("timer_reset_button"),
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(28.dp))

            Button(
                onClick = {
                    if (isRunning) viewModel.pauseTimer() else viewModel.startTimer()
                },
                modifier = Modifier
                    .size(68.dp)
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

    // Modal Custom Time Dialog
    if (showCustomDialog) {
        CustomTimerInputDialog(
            currentTotalSeconds = totalSeconds,
            themeCardBg = theme.cardBackground,
            themeAccent = theme.accentColor,
            onDismiss = { showCustomDialog = false },
            onSave = { h, m, s ->
                viewModel.setCustomTimer(h, m, s)
                showCustomDialog = false
            }
        )
    }
}

@Composable
fun TimeStepperColumn(
    label: String,
    value: Int,
    themeAccent: Color,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    tagPrefix: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )

        IconButton(
            onClick = onIncrement,
            modifier = Modifier
                .size(30.dp)
                .testTag("${tagPrefix}_increment")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add $label", tint = themeAccent, modifier = Modifier.size(16.dp))
        }

        Text(
            text = String.format("%02d", value),
            color = Color.White,
            fontSize = 22.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.testTag("${tagPrefix}_value_text")
        )

        IconButton(
            onClick = onDecrement,
            modifier = Modifier
                .size(30.dp)
                .testTag("${tagPrefix}_decrement")
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Minus $label", tint = themeAccent, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun CustomTimerInputDialog(
    currentTotalSeconds: Int,
    themeCardBg: Color,
    themeAccent: Color,
    onDismiss: () -> Unit,
    onSave: (hours: Int, minutes: Int, seconds: Int) -> Unit
) {
    var hoursInput by remember { mutableStateOf((currentTotalSeconds / 3600).toString()) }
    var minutesInput by remember { mutableStateOf(((currentTotalSeconds % 3600) / 60).toString()) }
    var secondsInput by remember { mutableStateOf((currentTotalSeconds % 60).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = themeCardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⏳", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Input Custom Timer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter your exact custom duration:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours input
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Hours", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = hoursInput,
                            onValueChange = { if (it.length <= 2 && it.all { char -> char.isDigit() }) hoursInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeAccent,
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("custom_timer_input_hours")
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Minutes input
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Minutes", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = minutesInput,
                            onValueChange = { if (it.length <= 2 && it.all { char -> char.isDigit() }) minutesInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeAccent,
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("custom_timer_input_minutes")
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Seconds input
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Seconds", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = secondsInput,
                            onValueChange = { if (it.length <= 2 && it.all { char -> char.isDigit() }) secondsInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = themeAccent,
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.testTag("custom_timer_input_seconds")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick presets
                Text("Quick presets:", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("1m" to (0 to 1), "5m" to (0 to 5), "10m" to (0 to 10), "25m" to (0 to 25), "1h" to (1 to 0)).forEach { (label, pair) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(themeAccent.copy(alpha = 0.15f))
                                .border(1.dp, themeAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable {
                                    hoursInput = pair.first.toString()
                                    minutesInput = pair.second.toString()
                                    secondsInput = "0"
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(label, color = themeAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val h = hoursInput.toIntOrNull() ?: 0
                    val m = minutesInput.toIntOrNull() ?: 0
                    val s = secondsInput.toIntOrNull() ?: 0
                    onSave(h, m, s)
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeAccent),
                modifier = Modifier.testTag("custom_timer_save_button")
            ) {
                Text("Set Timer", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
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
