package ir.brazandeh.funnycode.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnimatedDeleteButton(
    modifier: Modifier = Modifier,
    onDeleteComplete: () -> Unit = {}
) {
    val text = "Delete"
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val trashShake = remember { Animatable(0f) }
    val letterProgress = remember { text.map { Animatable(0f) } }
    var isDeleting by remember { mutableStateOf(false) }

    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50.dp))
                .background(gradient)
                .clickable(
                    enabled = !isDeleting,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    isDeleting = true
                    scope.launch {
                        letterProgress.forEachIndexed { index, anim ->
                            launch {
                                anim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = 350,
                                        easing = FastOutSlowInEasing
                                    )
                                )
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                trashShake.snapTo(-8f)
                                trashShake.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
                            }
                            delay(110)
                        }

                        delay(350)
                        onDeleteComplete()
                    }
                }
                .padding(horizontal = 34.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier
                        .size(26.dp)
                        .rotate(trashShake.value)
                )

                Row {
                    text.forEachIndexed { index, char ->
                        val progress = letterProgress[index].value
                        val targetX = -((index + 1) * 22f + 25f)
                        val currentX = targetX * progress
                        val currentY = if (progress < 0.5f) {
                            -40f * (progress / 0.5f)
                        } else {
                            -40f + (45f * ((progress - 0.5f) / 0.5f))
                        }

                        Text(
                            text = char.toString(),
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.graphicsLayer {
                                translationX = currentX
                                translationY = currentY
                                scaleX = 1f - (progress * 0.8f)
                                scaleY = 1f - (progress * 0.8f)
                                rotationZ = -progress * 60f
                                alpha = 1f - progress
                            }
                        )
                    }
                }
            }
        }

        if (isDeleting) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    scope.launch {
                        letterProgress.forEach { it.snapTo(0f) }
                        trashShake.snapTo(0f)
                        isDeleting = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
            ) {
                Text(text = "Reset Animation", color = Color.White)
            }
        }
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
