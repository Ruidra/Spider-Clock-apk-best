package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpiderClockCanvas(
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFFECEFF1),
    secondaryColor: Color = Color(0xFF90A4AE),
    accentColor: Color = Color(0xFFFF5252),
    backgroundColor: Color = Color.Transparent,
    showInteractiveThread: Boolean = true,
    isSmoothSecond: Boolean = true,
    showCogs: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Live Clock ticker
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            if (isSmoothSecond) {
                delay(30)
            } else {
                delay(200)
            }
        }
    }

    // Interactive silk pull offset
    val silkDragY = remember { Animatable(0f) }
    val silkVibrate = remember { Animatable(0f) }

    // Gears continuous rotation
    val infiniteTransition = rememberInfiniteTransition(label = "gearRotation")
    val gearAngle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gearAngle1"
    )
    val gearAngle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(19000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gearAngle2"
    )

    // Spider idle breathing / leg twitch
    val spiderTwitch by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spiderTwitch"
    )

    val calendar = remember(currentTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    }

    val hours = calendar.get(Calendar.HOUR_OF_DAY)
    val minutes = calendar.get(Calendar.MINUTE)
    val seconds = calendar.get(Calendar.SECOND)
    val millis = calendar.get(Calendar.MILLISECOND)

    val secondAngle = if (isSmoothSecond) {
        (seconds * 6f) + (millis * 0.006f)
    } else {
        seconds * 6f
    }
    val minuteAngle = (minutes * 6f) + (seconds * 0.1f)
    val hourAngle = ((hours % 12) * 30f) + (minutes * 0.5f)

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("spider_clock_canvas")
            .pointerInput(showInteractiveThread) {
                if (!showInteractiveThread) return@pointerInput
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            silkDragY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val currentY = silkDragY.value
                        val newY = (currentY + dragAmount.y).coerceIn(-40f, 180f)
                        coroutineScope.launch {
                            silkDragY.snapTo(newY)
                        }
                    }
                )
            }
            .pointerInput(showInteractiveThread) {
                if (!showInteractiveThread) return@pointerInput
                detectTapGestures(
                    onTap = {
                        coroutineScope.launch {
                            silkVibrate.animateTo(1f, animationSpec = tween(120))
                            silkVibrate.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy))
                        }
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val radius = (minOf(width, height) / 2f) * 0.78f
        val center = Offset(width / 2f, height / 2f + silkDragY.value)

        // Draw background if specified
        if (backgroundColor != Color.Transparent) {
            drawRect(color = backgroundColor)
        }

        // 1. Draw Spider Silk Thread hanging from top
        if (showInteractiveThread) {
            val threadEndX = center.x + (silkVibrate.value * 6f * sin(currentTimeMillis * 0.05f))
            drawLine(
                color = primaryColor.copy(alpha = 0.5f),
                start = Offset(width / 2f, 0f),
                end = Offset(threadEndX, center.y),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 2. Draw rotating gothic mechanical cogs in background
        if (showCogs) {
            drawCogs(
                center = center,
                radius = radius,
                gearAngle1 = gearAngle1,
                gearAngle2 = gearAngle2,
                color = secondaryColor.copy(alpha = 0.12f)
            )
        }

        // 3. Draw Spider Web Clock Face (Dial & Numerals)
        drawWebDial(
            center = center,
            radius = radius,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            accentColor = accentColor
        )

        // 4. Draw Spider Body & Legs (Clock Hands)
        drawSpiderClockHands(
            center = center,
            radius = radius,
            hourAngle = hourAngle,
            minuteAngle = minuteAngle,
            secondAngle = secondAngle,
            spiderTwitch = spiderTwitch,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            accentColor = accentColor
        )
    }
}

/**
 * Draws background mechanical gears/cogs inspired by original ps-spider-clock
 */
private fun DrawScope.drawCogs(
    center: Offset,
    radius: Float,
    gearAngle1: Float,
    gearAngle2: Float,
    color: Color
) {
    // Cog 1: Top-Right Gear (24 teeth)
    rotate(gearAngle1, pivot = Offset(center.x + radius * 0.35f, center.y - radius * 0.28f)) {
        drawGear(
            center = Offset(center.x + radius * 0.35f, center.y - radius * 0.28f),
            outerRadius = radius * 0.55f,
            innerRadius = radius * 0.44f,
            teeth = 24,
            color = color
        )
    }

    // Cog 2: Bottom-Left Gear (20 teeth)
    rotate(gearAngle2, pivot = Offset(center.x - radius * 0.38f, center.y + radius * 0.25f)) {
        drawGear(
            center = Offset(center.x - radius * 0.38f, center.y + radius * 0.25f),
            outerRadius = radius * 0.48f,
            innerRadius = radius * 0.38f,
            teeth = 20,
            color = color.copy(alpha = color.alpha * 0.85f)
        )
    }

    // Cog 3: Small Center-Top Gear (12 teeth)
    rotate(-gearAngle1 * 1.5f, pivot = Offset(center.x - radius * 0.2f, center.y - radius * 0.45f)) {
        drawGear(
            center = Offset(center.x - radius * 0.2f, center.y - radius * 0.45f),
            outerRadius = radius * 0.28f,
            innerRadius = radius * 0.22f,
            teeth = 12,
            color = color.copy(alpha = color.alpha * 0.6f)
        )
    }
}

private fun DrawScope.drawGear(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    teeth: Int,
    color: Color
) {
    val toothPath = Path()
    val totalSteps = teeth * 2
    val angleStep = (2 * PI / totalSteps).toFloat()

    for (i in 0 until totalSteps) {
        val angle = i * angleStep
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) {
            toothPath.moveTo(x, y)
        } else {
            toothPath.lineTo(x, y)
        }
    }
    toothPath.close()

    drawPath(path = toothPath, color = color, style = Stroke(width = 2.5f))
    // Gear hub and axle
    drawCircle(color = color, radius = innerRadius * 0.3f, center = center, style = Stroke(width = 2f))
    drawCircle(color = color, radius = innerRadius * 0.12f, center = center, style = Fill)
}

/**
 * Draws the Spider Web dial with radial silk spokes and concentric web arcs
 */
private fun DrawScope.drawWebDial(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color
) {
    // Outer web ring
    drawCircle(
        color = secondaryColor.copy(alpha = 0.25f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.5f)
    )

    // Concentric web spiral / circular rings
    val webRings = listOf(0.28f, 0.48f, 0.68f, 0.86f, 1.0f)
    for (ringRatio in webRings) {
        val r = radius * ringRatio
        val ringPath = Path()
        val segments = 12
        for (i in 0 until segments) {
            val angle1 = (i * 30.0 * PI / 180.0).toFloat()
            val angle2 = ((i + 1) * 30.0 * PI / 180.0).toFloat()
            val midAngle = ((i + 0.5) * 30.0 * PI / 180.0).toFloat()

            val p1 = Offset(center.x + r * cos(angle1), center.y + r * sin(angle1))
            val p2 = Offset(center.x + r * cos(angle2), center.y + r * sin(angle2))
            // Concave web sag towards center
            val sagRadius = r * 0.94f
            val control = Offset(center.x + sagRadius * cos(midAngle), center.y + sagRadius * sin(midAngle))

            if (i == 0) ringPath.moveTo(p1.x, p1.y)
            ringPath.quadraticTo(control.x, control.y, p2.x, p2.y)
        }
        ringPath.close()
        drawPath(
            path = ringPath,
            color = secondaryColor.copy(alpha = 0.16f),
            style = Stroke(width = 1.2f)
        )
    }

    // 12 Radial silk threads radiating from center
    for (i in 0 until 12) {
        val angleDeg = i * 30.0
        val angleRad = (angleDeg * PI / 180.0).toFloat()
        val end = Offset(
            center.x + radius * cos(angleRad),
            center.y + radius * sin(angleRad)
        )
        drawLine(
            color = secondaryColor.copy(alpha = 0.22f),
            start = center,
            end = end,
            strokeWidth = 1.2f
        )

        // Web intersection nodes
        drawCircle(
            color = if (i % 3 == 0) accentColor.copy(alpha = 0.85f) else primaryColor.copy(alpha = 0.5f),
            radius = if (i % 3 == 0) 3.5f else 2.2f,
            center = end
        )
    }

    // Gothic Numerals or Hour markers
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        alpha = 210
        textSize = radius * 0.11f
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD)
        isAntiAlias = true
    }

    val cardinalNumbers = arrayOf("12", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11")
    for (i in cardinalNumbers.indices) {
        // -90 degrees puts 12 at the top
        val angleDeg = (i * 30.0) - 90.0
        val angleRad = (angleDeg * PI / 180.0).toFloat()
        val textRadius = radius * 0.90f
        val tx = center.x + textRadius * cos(angleRad)
        val ty = center.y + textRadius * sin(angleRad) + (textPaint.textSize * 0.35f)

        drawContext.canvas.nativeCanvas.drawText(
            cardinalNumbers[i],
            tx,
            ty,
            textPaint
        )
    }
}

/**
 * Draws the Spider body in the center and uses spider legs as Hour, Minute, and Second hands
 */
private fun DrawScope.drawSpiderClockHands(
    center: Offset,
    radius: Float,
    hourAngle: Float,
    minuteAngle: Float,
    secondAngle: Float,
    spiderTwitch: Float,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color
) {
    val bodyScale = radius * 0.18f

    // 1. Draw Hour Hand: Broad Jointed Arachnid Leg
    rotate(hourAngle, pivot = center) {
        val legLength = radius * 0.52f
        drawArachnidHand(
            center = center,
            length = legLength,
            baseWidth = 6.5f,
            midWidth = 4.2f,
            tipWidth = 1.5f,
            color = primaryColor,
            isHour = true
        )
    }

    // 2. Draw Minute Hand: Slender Elongated Arachnid Leg
    rotate(minuteAngle, pivot = center) {
        val legLength = radius * 0.76f
        drawArachnidHand(
            center = center,
            length = legLength,
            baseWidth = 5.0f,
            midWidth = 3.0f,
            tipWidth = 1.0f,
            color = primaryColor.copy(alpha = 0.95f),
            isHour = false
        )
    }

    // 3. Draw Second Hand: Needle-Sharp Stinger / Pointer Leg
    rotate(secondAngle, pivot = center) {
        val secondLength = radius * 0.85f
        val path = Path().apply {
            moveTo(center.x - 2.5f, center.y + radius * 0.15f)
            lineTo(center.x + 2.5f, center.y + radius * 0.15f)
            lineTo(center.x + 1.2f, center.y - secondLength * 0.65f)
            lineTo(center.x, center.y - secondLength)
            lineTo(center.x - 1.2f, center.y - secondLength * 0.65f)
            close()
        }
        drawPath(path = path, color = accentColor)
        // Red tip dot
        drawCircle(
            color = accentColor,
            radius = 3.5f,
            center = Offset(center.x, center.y - secondLength)
        )
    }

    // 4. Draw Spider Stabilizer Legs (4 side legs)
    val sideLegAngles = listOf(45f, 135f, 225f, 315f)
    for ((index, legBaseAngle) in sideLegAngles.withIndex()) {
        val twitchAngle = legBaseAngle + (if (index % 2 == 0) spiderTwitch else -spiderTwitch)
        rotate(twitchAngle, pivot = center) {
            val legPath = Path().apply {
                moveTo(center.x, center.y)
                val joint1 = Offset(center.x + bodyScale * 0.7f, center.y - bodyScale * 0.6f)
                val joint2 = Offset(center.x + bodyScale * 1.4f, center.y - bodyScale * 0.2f)
                lineTo(joint1.x, joint1.y)
                lineTo(joint2.x, joint2.y)
            }
            drawPath(
                path = legPath,
                color = primaryColor.copy(alpha = 0.8f),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )
        }
    }

    // 5. Draw Spider Central Body (Abdomen, Cephalothorax, Chelicerae/Fangs, Glowing Eyes)
    drawSpiderBody(center = center, bodyScale = bodyScale, primaryColor = primaryColor, accentColor = accentColor)
}

/**
 * Draws a realistic arachnid jointed leg serving as a clock hand
 */
private fun DrawScope.drawArachnidHand(
    center: Offset,
    length: Float,
    baseWidth: Float,
    midWidth: Float,
    tipWidth: Float,
    color: Color,
    isHour: Boolean
) {
    // Joint 1: Coxa/Femur
    val j1Y = center.y - length * 0.42f
    val j1XOffset = if (isHour) 6f else 4f

    // Joint 2: Tibia
    val j2Y = center.y - length * 0.78f
    val j2XOffset = if (isHour) -3f else -2f

    // Tip: Tarsus / claw
    val tipY = center.y - length

    val path = Path().apply {
        moveTo(center.x - baseWidth, center.y + 6f)
        lineTo(center.x - midWidth + j1XOffset, j1Y)
        lineTo(center.x - tipWidth + j2XOffset, j2Y)
        lineTo(center.x, tipY)
        lineTo(center.x + tipWidth + j2XOffset, j2Y)
        lineTo(center.x + midWidth + j1XOffset, j1Y)
        lineTo(center.x + baseWidth, center.y + 6f)
        close()
    }

    drawPath(path = path, color = color)

    // Highlight ridge along leg
    drawLine(
        color = Color.White.copy(alpha = 0.5f),
        start = Offset(center.x, center.y),
        end = Offset(center.x + j2XOffset, j2Y),
        strokeWidth = 1.2f
    )
}

/**
 * Draws the central spider anatomy
 */
private fun DrawScope.drawSpiderBody(
    center: Offset,
    bodyScale: Float,
    primaryColor: Color,
    accentColor: Color
) {
    // 1. Abdomen (rear oval body)
    val abdomenCenter = Offset(center.x, center.y + bodyScale * 0.52f)
    val abdomenWidth = bodyScale * 0.72f
    val abdomenHeight = bodyScale * 0.96f

    drawOval(
        color = primaryColor,
        topLeft = Offset(abdomenCenter.x - abdomenWidth / 2f, abdomenCenter.y - abdomenHeight / 2f),
        size = androidx.compose.ui.geometry.Size(abdomenWidth, abdomenHeight)
    )

    // Abdomen gothic marking (inverted hourglass or skull cross)
    val markingPath = Path().apply {
        val my = abdomenCenter.y
        moveTo(center.x - 3.5f, my - 7f)
        lineTo(center.x + 3.5f, my - 7f)
        lineTo(center.x, my)
        lineTo(center.x + 3.5f, my + 7f)
        lineTo(center.x - 3.5f, my + 7f)
        lineTo(center.x, my)
        close()
    }
    drawPath(path = markingPath, color = accentColor)

    // 2. Cephalothorax (central thorax)
    val thoraxCenter = Offset(center.x, center.y - bodyScale * 0.12f)
    val thoraxWidth = bodyScale * 0.58f
    val thoraxHeight = bodyScale * 0.62f

    drawOval(
        color = primaryColor,
        topLeft = Offset(thoraxCenter.x - thoraxWidth / 2f, thoraxCenter.y - thoraxHeight / 2f),
        size = androidx.compose.ui.geometry.Size(thoraxWidth, thoraxHeight)
    )

    // 3. Head & Mandibles / Chelicerae (Fangs)
    val headCenter = Offset(center.x, center.y - bodyScale * 0.50f)
    drawCircle(
        color = primaryColor,
        radius = bodyScale * 0.22f,
        center = headCenter
    )

    // Fangs
    val leftFang = Path().apply {
        moveTo(headCenter.x - 4f, headCenter.y - 2f)
        lineTo(headCenter.x - 7f, headCenter.y - 9f)
        lineTo(headCenter.x - 2f, headCenter.y - 6f)
        close()
    }
    val rightFang = Path().apply {
        moveTo(headCenter.x + 4f, headCenter.y - 2f)
        lineTo(headCenter.x + 7f, headCenter.y - 9f)
        lineTo(headCenter.x + 2f, headCenter.y - 6f)
        close()
    }
    drawPath(leftFang, color = primaryColor)
    drawPath(rightFang, color = primaryColor)

    // 4. Glowing Red/Accent Spider Eyes (4 or 8 clustered eyes)
    drawCircle(color = accentColor, radius = 2.2f, center = Offset(headCenter.x - 3.2f, headCenter.y - 3.5f))
    drawCircle(color = accentColor, radius = 2.2f, center = Offset(headCenter.x + 3.2f, headCenter.y - 3.5f))
    drawCircle(color = accentColor, radius = 1.4f, center = Offset(headCenter.x - 6.2f, headCenter.y - 1.5f))
    drawCircle(color = accentColor, radius = 1.4f, center = Offset(headCenter.x + 6.2f, headCenter.y - 1.5f))

    // Center cap / axle pin
    drawCircle(
        color = accentColor,
        radius = 3.5f,
        center = center
    )
}
