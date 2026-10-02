package com.example.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SpiderClockDatabase
import com.example.model.AlarmItem
import com.example.model.WakeChallenge
import com.example.ui.components.OriginalSpiderClockView
import com.example.ui.components.SpiderClockCanvas
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn screen on and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                AlarmRingingScreen(
                    alarmId = alarmId,
                    onDismiss = {
                        dismissAlarm(alarmId)
                    },
                    onSnooze = { minutes ->
                        snoozeAlarm(alarmId, minutes)
                    }
                )
            }
        }
    }

    private fun dismissAlarm(alarmId: Long) {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_DISMISS
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        sendBroadcast(intent)
        finish()
    }

    private fun snoozeAlarm(alarmId: Long, minutes: Int) {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmScheduler.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_SNOOZE_MINUTES, minutes)
        }
        sendBroadcast(intent)
        finish()
    }
}

@Composable
fun AlarmRingingScreen(
    alarmId: Long,
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit
) {
    val context = LocalContext.current
    var alarmItem by remember { mutableStateOf<AlarmItem?>(null) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(alarmId) {
        val db = SpiderClockDatabase.getDatabase(context)
        alarmItem = db.alarmDao().getAlarmById(alarmId)
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    val timeFormat = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val currentAlarm = alarmItem
    val challenge = currentAlarm?.challenge ?: WakeChallenge.NONE
    val title = currentAlarm?.title ?: "Wake Up!"
    val snoozeMinutes = currentAlarm?.snoozeMinutes ?: 5

    // Challenge States
    var tapsRemaining by remember { mutableIntStateOf(5) }
    var spiderOffsetX by remember { mutableIntStateOf(0) }
    var spiderOffsetY by remember { mutableIntStateOf(0) }

    // Math Challenge state
    val num1 = remember { Random.nextInt(15, 60) }
    val num2 = remember { Random.nextInt(12, 45) }
    val expectedAnswer = remember { num1 + num2 }
    var mathInput by remember { mutableStateOf("") }
    var mathError by remember { mutableStateOf(false) }

    // Swipe / untangle challenge state
    var untangleProgress by remember { mutableIntStateOf(0) }

    val isChallengeCompleted = when (challenge) {
        WakeChallenge.NONE -> true
        WakeChallenge.TAP_SPIDER -> tapsRemaining <= 0
        WakeChallenge.MATH_PUZZLE -> mathInput.trim() == expectedAnswer.toString()
        WakeChallenge.WEB_UNTANGLE -> untangleProgress >= 3
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("alarm_ringing_surface"),
        color = Color(0xFF07090E)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle glowing red/amber spider web ambiance
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x33E53935),
                                Color(0x11E53935),
                                Color(0xFF07090E)
                            ),
                            radius = 900f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top header: Alarm Alert badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x33E53935)),
                        modifier = Modifier.border(1.dp, Color(0xFFE53935), RoundedCornerShape(20.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SPIDER ALARM ACTIVE",
                                color = Color(0xFFFF5252),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = timeFormat.format(Date(currentTime)),
                        color = Color(0xFFFF8A80),
                        fontSize = 32.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = dateFormat.format(Date(currentTime)),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }

                // Middle: Spider animation or Challenge interactive box
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .scale(if (challenge == WakeChallenge.NONE) pulseScale else 1f),
                    contentAlignment = Alignment.Center
                ) {
                    when (challenge) {
                        WakeChallenge.NONE -> {
                            OriginalSpiderClockView(
                                modifier = Modifier.size(240.dp)
                            )
                        }

                        WakeChallenge.TAP_SPIDER -> {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131824)),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, Color(0xFF2C354D), RoundedCornerShape(24.dp))
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                                    ) {
                                        Text(
                                            text = "Tap the quick spider!",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = if (tapsRemaining > 0) "$tapsRemaining taps remaining" else "Spider caught!",
                                            color = if (tapsRemaining > 0) Color(0xFFFF5252) else Color(0xFF69F0AE),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    if (tapsRemaining > 0) {
                                        Box(
                                            modifier = Modifier
                                                .offset { IntOffset(spiderOffsetX, spiderOffsetY) }
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x33FF5252))
                                                .border(2.dp, Color(0xFFFF5252), CircleShape)
                                                .clickable {
                                                    tapsRemaining--
                                                    spiderOffsetX = Random.nextInt(-70, 70)
                                                    spiderOffsetY = Random.nextInt(-40, 60)
                                                }
                                                .testTag("scurrying_spider_tap_target"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🕷️", fontSize = 34.sp)
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Success",
                                            tint = Color(0xFF69F0AE),
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                }
                            }
                        }

                        WakeChallenge.MATH_PUZZLE -> {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131824)),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, Color(0xFF2C354D), RoundedCornerShape(24.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Spider Wake Challenge",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "$num1 + $num2 = ?",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 28.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))

                                    OutlinedTextField(
                                        value = mathInput,
                                        onValueChange = {
                                            mathInput = it
                                            mathError = false
                                        },
                                        placeholder = { Text("Enter sum") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFFFF5252),
                                            unfocusedBorderColor = Color(0xFF3B4866),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .testTag("math_challenge_input")
                                    )

                                    if (isChallengeCompleted) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Solved! You are awake.",
                                            color = Color(0xFF69F0AE),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        WakeChallenge.WEB_UNTANGLE -> {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF131824)),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, Color(0xFF2C354D), RoundedCornerShape(24.dp))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Text(
                                        text = "Untangle Silk Web",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Touch checkpoints to awaken (${untangleProgress}/3)",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (step in 1..3) {
                                            val isDone = untangleProgress >= step
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isDone) Color(0xFF00E676) else Color(0x33FF5252))
                                                    .border(
                                                        2.dp,
                                                        if (isDone) Color(0xFF69F0AE) else Color(0xFFFF5252),
                                                        CircleShape
                                                    )
                                                    .clickable {
                                                        if (untangleProgress == step - 1) {
                                                            untangleProgress = step
                                                        }
                                                    }
                                                    .testTag("untangle_step_$step"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isDone) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                } else {
                                                    Text("🕸️", fontSize = 20.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom actions: Snooze & Dismiss buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Snooze Button
                    Button(
                        onClick = { onSnooze(snoozeMinutes) },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(54.dp)
                            .testTag("alarm_snooze_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = null,
                            tint = Color(0xFFFFD54F)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Snooze ($snoozeMinutes minutes)",
                            color = Color(0xFFFFD54F),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dismiss Button (gated by challenge if active)
                    Button(
                        onClick = {
                            if (isChallengeCompleted) {
                                onDismiss()
                            }
                        },
                        enabled = isChallengeCompleted,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(56.dp)
                            .testTag("alarm_dismiss_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isChallengeCompleted) Color(0xFFE53935) else Color(0xFF424242),
                            disabledContainerColor = Color(0xFF2A2A2A)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isChallengeCompleted) Color.White else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isChallengeCompleted) "Dismiss Alarm" else "Complete Challenge to Dismiss",
                            color = if (isChallengeCompleted) Color.White else Color.Gray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
