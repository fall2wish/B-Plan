package cn.edu.bnbu.bplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.bnbu.bplan.data.model.Course

@Composable
fun CourseCard(
    course: Course,
    modifier: Modifier = Modifier,
    hasConflict: Boolean = false,
    conflictWithCourseName: String? = null,
    onClick: () -> Unit
) {
    val bgColor = try {
        Color(android.graphics.Color.parseColor(course.color))
    } catch (_: Exception) {
        Color(0xFF3B82F6)
    }

    val displayName = when {
        course.name.contains("Survey Sampling", ignoreCase = true) -> {
            if (course.startPeriod <= 4 || course.name.contains("Morning", ignoreCase = true) || (course.startTime ?: "").startsWith("09")) {
                "Survey Sampling (AM)"
            } else {
                "Survey Sampling (PM)"
            }
        }
        course.name.contains("Regression Analysis") && !course.isTutorial -> "Regression\nAnalysis"
        course.name.contains("Data Analytics") && !course.isTutorial -> "Data Analytics\nw/ Prog"
        course.isTutorial -> {
            when {
                course.name.contains("Python", ignoreCase = true) -> "Python (TA)"
                course.name.contains("Regression", ignoreCase = true) -> "Regress (TA)"
                course.name.contains("Data Analytics", ignoreCase = true) -> "Analytics (TA)"
                course.name.contains("Simulation", ignoreCase = true) -> "Simul (TA)"
                else -> course.name.replace("Tutorial", "(TA)").replace(" (Tutorial)", " (TA)")
            }
        }
        else -> course.name
    }
    val isShortCourse = course.effectiveDurationMinutes <= 65

    val cardBorder = if (hasConflict) {
        androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B))
    } else null

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(if (hasConflict) Modifier.shadow(4.dp, RoundedCornerShape(5.dp)) else Modifier)
            .clip(RoundedCornerShape(5.dp))
            .then(if (cardBorder != null) Modifier.border(cardBorder, RoundedCornerShape(5.dp)) else Modifier)
            .background(bgColor.copy(alpha = 0.95f))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = if (isShortCourse || hasConflict) 1.dp else 2.5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val titleText = if (hasConflict) {
                        when {
                            course.name.contains("Python", ignoreCase = true) -> "Python"
                            course.name.contains("Regression", ignoreCase = true) -> "Regress"
                            else -> displayName
                        }
                    } else displayName

                    Text(
                        text = titleText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (hasConflict) 6.8.sp else (if (isShortCourse) 7.2.sp else 8.5.sp),
                        letterSpacing = if (hasConflict) (-0.3).sp else 0.sp,
                        maxLines = if (hasConflict) 1 else (if (isShortCourse) 2 else 4),
                        overflow = TextOverflow.Clip,
                        lineHeight = 8.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (hasConflict) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.padding(start = 1.dp)
                        ) {
                            Text(
                                text = "重叠",
                                color = Color.White,
                                fontSize = 5.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.5.dp)
                            )
                        }
                    }
                }
                if (hasConflict && !isShortCourse) {
                    Text(
                        text = course.startTime ?: "TA",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 6.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 7.5.sp,
                        maxLines = 1
                    )
                } else if (course.isTutorial && !isShortCourse) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.22f))
                            .padding(horizontal = 2.dp, vertical = 0.5.dp)
                    ) {
                        Text(
                            text = if (course.customTime != null) "18:00-19:00 (1h)" else "1h 辅导课",
                            color = Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.4).sp,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }

            if (!course.classroom.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Black.copy(alpha = 0.30f))
                        .padding(horizontal = 1.dp, vertical = if (isShortCourse || hasConflict) 0.5.dp else 1.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (hasConflict) course.classroom else "@${course.classroom}",
                        color = Color.White,
                        fontSize = if (hasConflict) 6.8.sp else (if (isShortCourse) 7.2.sp else 8.sp),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}
