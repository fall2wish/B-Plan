package cn.edu.bnbu.bplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cn.edu.bnbu.bplan.data.model.Course
import cn.edu.bnbu.bplan.ui.theme.BnbuBlue

@Composable
fun CourseDetailDialog(
    course: Course,
    onDismiss: () -> Unit,
    onEdit: (Course) -> Unit = {},
    onDelete: (String) -> Unit
) {
    val themeColor = try {
        Color(android.graphics.Color.parseColor(course.color))
    } catch (_: Exception) {
        Color(0xFF3B82F6)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(themeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = course.displayCode,
                            color = themeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = course.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // 如果为周二晚间冲突 TA 课，显示显著提示横幅
                if (course.isTutorial && course.dayOfWeek == 2) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "时段重叠提示：该 TA 课程在周二 19:00 - 19:50 与另一门 TA 课时间冲突，课表已错开叠加显示。",
                                fontSize = 11.5.sp,
                                color = Color(0xFF92400E),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                DetailRow(label = "课程代码", value = course.displayCode)
                DetailRow(label = "上课教室", value = course.classroom ?: "未分配教室")
                DetailRow(label = "授课教师", value = course.teacher ?: "未录入")
                // 正课不显示助教，TA课显示助教
                if (course.isTutorial && !course.assistant.isNullOrBlank()) {
                    DetailRow(label = "课程助教", value = course.assistant)
                }
                val timeDesc = if (!course.customTime.isNullOrBlank()) {
                    "第 ${course.startPeriod} - ${course.endPeriod} 节 (${course.customTime} · 星期${dayOfWeekName(course.dayOfWeek)})"
                } else {
                    "第 ${course.startPeriod} - ${course.endPeriod} 节 (星期${dayOfWeekName(course.dayOfWeek)})"
                }
                DetailRow(label = "节次安排", value = timeDesc)
                DetailRow(label = "周次范围", value = "第 ${course.startWeek} - ${course.endWeek} 周 (${weekTypeDescription(course)})")
                DetailRow(label = "学分/学时", value = "${course.units} 学分")

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            onDelete(course.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("删除课程")
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onEdit(course)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BnbuBlue)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("编辑课程 / 时间")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(text = value, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}

private fun dayOfWeekName(day: Int): String = when (day) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    7 -> "日"
    else -> "一"
}

private fun weekTypeDescription(course: Course): String = when (course.weekType) {
    cn.edu.bnbu.bplan.data.model.WeekType.ALL -> "全周"
    cn.edu.bnbu.bplan.data.model.WeekType.ODD -> "单周"
    cn.edu.bnbu.bplan.data.model.WeekType.EVEN -> "双周"
    cn.edu.bnbu.bplan.data.model.WeekType.CUSTOM -> "指定周"
}
