package cn.edu.bnbu.bplan.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.bnbu.bplan.ui.theme.UrgentRed
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun BidirectionalSwipeBox(
    onSwipeRightCalendar: () -> Unit,
    onSwipeLeftDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val maxSwipePx = with(density) { 90.dp.toPx() }
    val offsetX = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        // --- Background Revealed Action Panels ---
        Row(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left panel (revealed when dragging right -> Calendar)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(90.dp)
                    .background(Color(0xFF10B981)) // Emerald Green
                    .clickable {
                        coroutineScope.launch { offsetX.animateTo(0f) }
                        onSwipeRightCalendar()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "添加到日历",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "加日历",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right panel (revealed when dragging left -> Delete)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(90.dp)
                    .background(UrgentRed)
                    .clickable {
                        coroutineScope.launch { offsetX.animateTo(0f) }
                        onSwipeLeftDelete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除待办",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "删除",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- Foreground Card Content ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                val current = offsetX.value
                                when {
                                    current > maxSwipePx * 0.5f -> {
                                        offsetX.animateTo(maxSwipePx, tween(200))
                                    }
                                    current < -maxSwipePx * 0.5f -> {
                                        offsetX.animateTo(-maxSwipePx, tween(200))
                                    }
                                    else -> {
                                        offsetX.animateTo(0f, tween(200))
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch { offsetX.animateTo(0f) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val newOffset = (offsetX.value + dragAmount)
                                    .coerceIn(-maxSwipePx, maxSwipePx)
                                offsetX.snapTo(newOffset)
                            }
                        }
                    )
                }
                .clickable {
                    if (offsetX.value != 0f) {
                        coroutineScope.launch { offsetX.animateTo(0f) }
                    }
                }
        ) {
            content()
        }
    }
}
