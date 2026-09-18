package cn.edu.bnbu.bplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cn.edu.bnbu.bplan.data.model.Course
import cn.edu.bnbu.bplan.data.model.WeekType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCourseDialog(
    course: Course,
    onDismiss: () -> Unit,
    onSaveCourse: (Course) -> Unit
) {
    val guardAction = LocalAgreementGuard.current
    var name by remember { mutableStateOf(course.name) }
    var code by remember { mutableStateOf(course.code ?: course.displayCode) }
    var classroom by remember { mutableStateOf(course.classroom ?: "") }
    var teacher by remember { mutableStateOf(course.teacher ?: "") }
    var assistant by remember { mutableStateOf(course.assistant ?: "") }
    var dayOfWeek by remember { mutableStateOf(course.dayOfWeek) }
    var startPeriod by remember { mutableStateOf(course.startPeriod) }
    var endPeriod by remember { mutableStateOf(course.endPeriod) }

    // 自定义上课时间与时长
    var isCustomTime by remember {
        mutableStateOf(!course.startTime.isNullOrBlank() || !course.customTime.isNullOrBlank() || course.durationMinutes != null)
    }
    var startTime by remember {
        mutableStateOf(course.startTime ?: (if (course.isTutorial) "18:00" else defaultStartTimeForPeriod(course.startPeriod)))
    }
    var endTime by remember {
        mutableStateOf(course.endTime ?: (if (course.isTutorial) "19:00" else defaultEndTimeForPeriod(course.endPeriod)))
    }
    var durationMinutes by remember {
        mutableStateOf(course.effectiveDurationMinutes)
    }

    var weekType by remember { mutableStateOf(course.weekType) }
    var selectedColor by remember { mutableStateOf(course.color) }

    val colors = listOf("#2563EB", "#7C3AED", "#059669", "#D97706", "#DC2626", "#DB2777", "#0891B2")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "编辑课程与时间安排",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("课程名称 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("代码 (如 DSBS7030)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = classroom,
                        onValueChange = { classroom = it },
                        label = { Text("教室 (如 T4-305)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("授课教师 (GSAS)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = assistant,
                        onValueChange = { assistant = it },
                        label = { Text("助教 (仅TA课)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "星期选择", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..7).forEach { day ->
                        val isSelected = dayOfWeek == day
                        val dayLabel = when (day) {
                            1 -> "一"; 2 -> "二"; 3 -> "三"; 4 -> "四"; 5 -> "五"; 6 -> "六"; else -> "日"
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF1F5F9))
                                .clickable { dayOfWeek = day },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "周$dayLabel",
                                color = if (isSelected) Color.White else Color.Black,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                // --- 自定义上课时间与时长区域 ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "自定义上课时间与时长", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "本科生不同时段/TA课1小时自由设定",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isCustomTime,
                        onCheckedChange = { isCustomTime = it },
                        modifier = Modifier.height(24.dp)
                    )
                }

                if (isCustomTime) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "快捷时长预设:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            50 to "50m (单节)",
                            60 to "1h (TA课)",
                            105 to "105m (两节)",
                            170 to "170m (大课)"
                        ).forEach { (m, label) ->
                            FilterChip(
                                selected = durationMinutes == m,
                                onClick = {
                                    durationMinutes = m
                                    // 自动计算结束时间
                                    endTime = addMinutesToTime(startTime, m)
                                },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = {
                                startTime = it
                                endTime = addMinutesToTime(it, durationMinutes)
                            },
                            label = { Text("开始时间 (如 18:00)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("结束时间 (如 19:00)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = durationMinutes.toString(),
                            onValueChange = {
                                val d = it.toIntOrNull() ?: 50
                                durationMinutes = d
                                endTime = addMinutesToTime(startTime, d)
                            },
                            label = { Text("课程时长 (分钟)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = "$startPeriod - $endPeriod",
                            onValueChange = { text ->
                                val parts = text.split("-")
                                if (parts.size == 2) {
                                    startPeriod = parts[0].trim().toIntOrNull() ?: startPeriod
                                    endPeriod = parts[1].trim().toIntOrNull() ?: endPeriod
                                }
                            },
                            label = { Text("对应节次 (如 11-11)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startPeriod.toString(),
                            onValueChange = { startPeriod = it.toIntOrNull() ?: 1 },
                            label = { Text("起始节次 (1-14)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endPeriod.toString(),
                            onValueChange = { endPeriod = it.toIntOrNull() ?: startPeriod },
                            label = { Text("结束节次 (1-14)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "周次模式", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(WeekType.ALL to "每周", WeekType.ODD to "单周", WeekType.EVEN to "双周").forEach { (type, label) ->
                        FilterChip(
                            selected = weekType == type,
                            onClick = { weekType = type },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "卡片颜色", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.forEach { cHex ->
                        val col = Color(android.graphics.Color.parseColor(cHex))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedColor = cHex }
                                .padding(if (selectedColor == cHex) 3.dp else 0.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            guardAction {
                                if (name.isNotBlank()) {
                                    val customTimeStr = if (isCustomTime) {
                                        "$startTime - $endTime (${durationMinutes}分钟)"
                                    } else null

                                    val updatedCourse = course.copy(
                                        name = name.trim(),
                                        code = code.trim().ifEmpty { course.displayCode },
                                        classroom = classroom.trim().ifEmpty { null },
                                        teacher = teacher.trim().ifEmpty { null },
                                        assistant = assistant.trim().ifEmpty { null },
                                        dayOfWeek = dayOfWeek,
                                        startPeriod = startPeriod.coerceIn(1, 15),
                                        endPeriod = endPeriod.coerceIn(startPeriod, 15),
                                        startTime = if (isCustomTime) startTime else null,
                                        endTime = if (isCustomTime) endTime else null,
                                        durationMinutes = if (isCustomTime) durationMinutes else null,
                                        customTime = customTimeStr ?: course.customTime,
                                        weekType = weekType,
                                        color = selectedColor
                                    )
                                    onSaveCourse(updatedCourse)
                                    onDismiss()
                                }
                            }
                        }
                    ) {
                        Text("保存更改")
                    }
                }
            }
        }
    }
}

private fun defaultStartTimeForPeriod(period: Int): String = when (period) {
    1 -> "08:00"; 2 -> "09:00"; 3 -> "10:00"; 4 -> "11:00"
    5 -> "12:00"; 6 -> "13:00"; 7 -> "14:00"; 8 -> "15:00"
    9 -> "16:00"; 10 -> "17:00"; 11 -> "18:00"; 12 -> "19:00"
    13 -> "20:00"; else -> "21:00"
}

private fun defaultEndTimeForPeriod(period: Int): String = when (period) {
    1 -> "08:50"; 2 -> "09:50"; 3 -> "10:50"; 4 -> "11:50"
    5 -> "12:50"; 6 -> "13:50"; 7 -> "14:50"; 8 -> "15:50"
    9 -> "16:50"; 10 -> "17:50"; 11 -> "18:50"; 12 -> "19:50"
    13 -> "20:50"; else -> "21:50"
}

private fun addMinutesToTime(timeStr: String, minutes: Int): String {
    try {
        val parts = timeStr.trim().split(":")
        if (parts.size == 2) {
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val total = h * 60 + m + minutes
            val newH = (total / 60) % 24
            val newM = total % 60
            return String.format("%02d:%02d", newH, newM)
        }
    } catch (_: Exception) {}
    return timeStr
}
