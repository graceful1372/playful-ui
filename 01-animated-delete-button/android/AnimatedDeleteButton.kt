package ir.brazandeh.funnycode.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Procedural Audio Synthesizer mimicking Web Audio API oscillator
 * Generates dynamic pitch-rising drop sounds without requiring external audio files.
 */
object TrashSoundSynthesizer {
    suspend fun playDropSound(index: Int) = withContext(Dispatchers.Default) {
        val sampleRate = 44100
        val durationMs = 120
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val baseFreq = 260.0 + (index * 35.0)
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / numSamples
            val currentFreq = baseFreq * (1.0 - progress * 0.5)
            val envelope = (1.0 - progress) * (1.0 - progress)
            val sample = Math.sin(2.0 * Math.PI * currentFreq * t) * envelope * Short.MAX_VALUE * 0.4
            buffer[i] = sample.toInt().toShort()
        }

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            delay(130)
            track.release()
        } catch (_: Exception) {
            // Graceful fallback if device audio is unavailable
        }
    }
}

/**
 * Custom Stroke-based Trash Bin matching the Web SVG version precisely.
 * Features separate animated Lid, Liquid level inside body, and impact shake.
 */
@Composable
fun TrashCanSvgIcon(
    modifier: Modifier = Modifier,
    lidAngle: Float = 0f,
    liquidProgress: Float = 0f,
    tint: Color = Color.White,
    liquidColor: Color = Color(0xFF38BDF8)
) {
    Canvas(modifier = modifier.size(26.dp)) {
        val strokeWidth = 2.dp.toPx()
        val w = size.width
        val h = size.height

        // Coords matching SVG viewBox 0 0 24 24
        fun x(v: Float) = (v / 24f) * w
        fun y(v: Float) = (v / 24f) * h

        // 1. Draw Liquid inside trash body clip
        val bodyClipPath = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(x(5f), y(6f), x(19f), y(22f)),
                    bottomLeft = CornerRadius(x(2f), y(2f)),
                    bottomRight = CornerRadius(x(2f), y(2f))
                )
            )
        }

        if (liquidProgress > 0f) {
            clipPath(bodyClipPath) {
                val liquidHeight = y(16f) * liquidProgress.coerceIn(0f, 1f)
                drawRect(
                    color = liquidColor,
                    topLeft = Offset(x(5f), y(22f) - liquidHeight),
                    size = Size(x(14f), liquidHeight)
                )
            }
        }

        // 2. Draw Trash Body (U-shape + vertical ribs)
        val bodyPath = Path().apply {
            moveTo(x(5f), y(6f))
            lineTo(x(5f), y(20f))
            quadraticTo(x(5f), y(22f), x(7f), y(22f))
            lineTo(x(17f), y(22f))
            quadraticTo(x(19f), y(22f), x(19f), y(20f))
            lineTo(x(19f), y(6f))
        }
        drawPath(
            path = bodyPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Vertical ribs inside
        drawLine(
            color = tint.copy(alpha = 0.85f),
            start = Offset(x(10f), y(11f)),
            end = Offset(x(10f), y(17f)),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint.copy(alpha = 0.85f),
            start = Offset(x(14f), y(11f)),
            end = Offset(x(14f), y(17f)),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // 3. Draw Lid (hinged at left: x=3, y=6)
        rotate(degrees = lidAngle, pivot = Offset(x(3f), y(6f))) {
            val lidPath = Path().apply {
                // Base horizontal bar
                moveTo(x(3f), y(6f))
                lineTo(x(21f), y(6f))
                // Handle
                moveTo(x(8f), y(6f))
                lineTo(x(8f), y(4f))
                quadraticTo(x(8f), y(2f), x(10f), y(2f))
                lineTo(x(14f), y(2f))
                quadraticTo(x(16f), y(2f), x(16f), y(4f))
                lineTo(x(16f), y(6f))
            }
            drawPath(
                path = lidPath,
                color = tint,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * Full Animated Delete Button matching the showcase animation:
 * 1. Trash tilts & lid pops open.
 * 2. Letters fly into the bin in a parabolic arc.
 * 3. Sound effect (rising pitch) + haptic feedback + impact shake per letter.
 * 4. Liquid level rises inside the bin.
 * 5. Lid shuts closed & bin straightens.
 * 6. Button collapses horizontally into a compact circle around the trash can.
 * 7. Trash bin empties out / confirms.
 * 8. Button smoothly expands back to initial state.
 */
@Composable
fun AnimatedDeleteButton(
    modifier: Modifier = Modifier,
    onDeleteComplete: () -> Unit = {}
) {
    val text = "Delete"
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Animation States
    val trashTilt = remember { Animatable(0f) }
    val trashShake = remember { Animatable(0f) }
    val trashLid = remember { Animatable(0f) }
    val liquidLevel = remember { Animatable(0f) }
    val letterProgress = remember { text.map { Animatable(0f) } }
    var isCollapsed by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }

    val buttonWidth by animateDpAsState(
        targetValue = if (isCollapsed) 64.dp else 184.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ButtonWidth"
    )

    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(buttonWidth)
                .height(60.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(50.dp),
                    spotColor = Color(0xFF6366F1),
                    ambientColor = Color(0xFF8B5CF6)
                )
                .clip(RoundedCornerShape(50.dp))
                .background(gradient)
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50.dp))
                .clickable(
                    enabled = !isAnimating,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (isAnimating) return@clickable
                    isAnimating = true

                    scope.launch {
                        // Phase 1: Tilt trash bin & open lid
                        launch {
                            trashTilt.animateTo(-12f, tween(200, easing = FastOutSlowInEasing))
                        }
                        launch {
                            trashLid.animateTo(-38f, spring(Spring.DampingRatioMediumBouncy))
                        }
                        delay(200)

                        // Phase 2: Pour letters one by one
                        val liquidSteps = listOf(0.20f, 0.40f, 0.58f, 0.75f, 0.88f, 1.0f)
                        letterProgress.forEachIndexed { index, anim ->
                            launch {
                                anim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(380, easing = FastOutSlowInEasing)
                                )
                            }

                            // When letter lands in trash bin
                            launch {
                                delay(260)
                                // Sound & Haptic
                                TrashSoundSynthesizer.playDropSound(index)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                // Liquid fill
                                liquidLevel.animateTo(
                                    liquidSteps[index],
                                    tween(100, easing = LinearEasing)
                                )

                                // Impact shake
                                trashShake.snapTo(-6f)
                                trashShake.animateTo(0f, spring(dampingRatio = 0.4f, stiffness = 600f))
                            }

                            delay(120)
                        }

                        // Phase 3: All letters landed -> Close lid & straighten trash bin
                        delay(380)
                        launch {
                            trashLid.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                        }
                        launch {
                            trashTilt.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                        }
                        delay(220)

                        // Phase 4: Button collapses into a circle around the trash can
                        isCollapsed = true
                        delay(500)

                        // Phase 5: Trash empties out (liquid drops to 0) with a mini jump
                        launch {
                            liquidLevel.animateTo(0f, tween(250))
                        }
                        trashShake.snapTo(8f)
                        trashShake.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
                        delay(400)

                        // Phase 6: Expand button back to normal
                        isCollapsed = false
                        delay(350)

                        // Reset letter progress for next time
                        letterProgress.forEach { it.snapTo(0f) }
                        isAnimating = false
                        onDeleteComplete()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                // Animated Trash Can Icon
                TrashCanSvgIcon(
                    lidAngle = trashLid.value,
                    liquidProgress = liquidLevel.value,
                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = trashTilt.value + trashShake.value
                        }
                )

                // Animated Letters (hidden when collapsed)
                if (!isCollapsed) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Row {
                        text.forEachIndexed { index, char ->
                            val progress = letterProgress[index].value
                            // Fly distance and arc toward the trash bin
                            val targetX = -((index + 1) * 20f + 24f)
                            val currentX = targetX * progress
                            val currentY = if (progress < 0.45f) {
                                -36f * (progress / 0.45f)
                            } else {
                                -36f + (40f * ((progress - 0.45f) / 0.55f))
                            }

                            Text(
                                text = char.toString(),
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.graphicsLayer {
                                    translationX = currentX
                                    translationY = currentY
                                    scaleX = 1f - (progress * 0.85f)
                                    scaleY = 1f - (progress * 0.85f)
                                    rotationZ = -progress * 55f
                                    alpha = (1f - progress).coerceIn(0f, 1f)
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isAnimating) "DELETING..." else "PRESS TO DELETE",
            color = Color(0xFF64748B),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F19)
@Composable
fun AnimatedDeleteButtonPreview() {
    Box(
        modifier = Modifier
            .size(360.dp, 240.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedDeleteButton()
    }
}
