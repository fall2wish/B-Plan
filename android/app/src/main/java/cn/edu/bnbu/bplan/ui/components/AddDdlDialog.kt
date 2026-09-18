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
import cn.edu.bnbu.bplan.data.model.AssignmentDdl
import java.util.UUID

@Composable
fun AddDdlDialog(
    onDismiss: () -> Unit,
    onAddDdl: (AssignmentDdl) -> Unit
) {
    val guardAction = LocalAgreementGuard.current
    var title by remember { mutableStateOf("") }
    var courseName by remember { mutableStateOf("") }
    var daysUntilDue by remember { mutableStateOf(3) }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "新建作业 / DDL 待办",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("作业/待办标题 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = courseName,
                    onValueChange = { courseName = it },
                    label = { Text("所属课程 (如 Data Science)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = daysUntilDue.toString(),
                    onValueChange = { daysUntilDue = it.toIntOrNull() ?: 1 },
                    label = { Text("几天后截止 (天数)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("备注要求") },
                    maxLines = 3,
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
                                    val dueTime = System.currentTimeMillis() + (daysUntilDue * 24 * 3600 * 1000L)
                                    val ddl = AssignmentDdl(
                                        id = "ddl_${UUID.randomUUID().toString().substring(0, 8)}",
                                        courseName = courseName.ifBlank { "通用日程" },
                                        title = title.trim(),
                                        dueTimestamp = dueTime,
                                        isSubmitted = false,
                                        description = description.ifBlank { null }
                                    )
                                    onAddDdl(ddl)
                                    onDismiss()
                                }
                            }
                        }
                    ) {
                        Text("保存 DDL")
                    }
                }
            }
        }
    }
}
