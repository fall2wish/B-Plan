package cn.edu.bnbu.bplan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cn.edu.bnbu.bplan.data.model.ScheduleEvent
import java.util.UUID

@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onAddEvent: (ScheduleEvent) -> Unit
) {
    val guardAction = LocalAgreementGuard.current
    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var hoursLater by remember { mutableStateOf(2) }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "新建个人日程 / 活动",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("日程名称 (如 讲座/组会) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("地点 (如 T1-101 / 研讨室)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = hoursLater.toString(),
                    onValueChange = { hoursLater = it.toIntOrNull() ?: 1 },
                    label = { Text("几小时后开始") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注信息") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            guardAction {
                                if (title.isNotBlank()) {
                                    val start = System.currentTimeMillis() + (hoursLater * 3600 * 1000L)
                                    val end = start + (2 * 3600 * 1000L)
                                    val event = ScheduleEvent(
                                        id = "evt_${UUID.randomUUID().toString().substring(0, 8)}",
                                        title = title.trim(),
                                        startTimestamp = start,
                                        endTimestamp = end,
                                        location = location.ifBlank { null },
                                        notes = notes.ifBlank { null },
                                        source = "manual"
                                    )
                                    onAddEvent(event)
                                    onDismiss()
                                }
                            }
                        }
                    ) {
                        Text("保存日程")
                    }
                }
            }
        }
    }
}
