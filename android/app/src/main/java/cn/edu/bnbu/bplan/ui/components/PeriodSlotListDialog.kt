package cn.edu.bnbu.bplan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import cn.edu.bnbu.bplan.data.model.PeriodSlot
import cn.edu.bnbu.bplan.data.repository.BPlanRepository
import cn.edu.bnbu.bplan.ui.theme.*

@Composable
fun PeriodSlotListDialog(
    repository: BPlanRepository,
    onDismiss: () -> Unit
) {
    val guardAction = LocalAgreementGuard.current
    val periodSlots by repository.periodSlots.collectAsState()

    var editingSlot by remember { mutableStateOf<PeriodSlot?>(null) }
    var showAddSlotDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = BnbuBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "课程节次时间列表",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BnbuNavy
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "逐节自定义起止时间，支持多节时间重叠 (按时间升序编号 1, 2, 3...)",
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Reset to Standard 3 Big Sessions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    AssistChip(
                        onClick = { guardAction { repository.resetToThreeSessionsPreset() } },
                        label = { Text("恢复默认三大节 (上/下/晚)", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hint card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BnbuBlue.copy(alpha = 0.07f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = BnbuBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "同一时间段可并存长课与短课 (如18:00-19:00 TA辅导与18:00-20:50晚间大课)，周表纵坐标将自适应展示。",
                            fontSize = 10.5.sp,
                            color = BnbuNavy,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Slot List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(periodSlots, key = { it.id }) { slot ->
                        SlotItemCard(
                            slot = slot,
                            onEdit = { editingSlot = slot },
                            onDelete = { guardAction { repository.deletePeriodSlot(slot.id) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Add Button
                Button(
                    onClick = { showAddSlotDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BnbuBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("添加自定义节次")
                }
            }
        }
    }

    // Edit Slot Sub-Dialog
    editingSlot?.let { targetSlot ->
        EditSlotDialog(
            slot = targetSlot,
            onDismiss = { editingSlot = null },
            onSave = { updated ->
                guardAction {
                    val currentList = periodSlots.map { if (it.id == updated.id) updated else it }
                    repository.updatePeriodSlots(currentList)
                    editingSlot = null
                }
            }
        )
    }

    // Add Slot Sub-Dialog
    if (showAddSlotDialog) {
        val nextNumber = periodSlots.size + 1
        EditSlotDialog(
            slot = PeriodSlot(
                periodNumber = nextNumber,
                label = "第 $nextNumber 节",
                startTime = "08:00",
                endTime = "08:50",
                isCustom = true
            ),
            isAdding = true,
            onDismiss = { showAddSlotDialog = false },
            onSave = { newSlot ->
                guardAction {
                    repository.addPeriodSlot(newSlot)
                    showAddSlotDialog = false
                }
            }
        )
    }
}

@Composable
private fun SlotItemCard(
    slot: PeriodSlot,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Period Number Badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(BnbuBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${slot.periodNumber}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = slot.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = BnbuBlue.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${slot.durationMinutes}m",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BnbuBlue,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${slot.startTime} - ${slot.endTime}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = BnbuBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "删除",
                        tint = UrgentRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditSlotDialog(
    slot: PeriodSlot,
    isAdding: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (PeriodSlot) -> Unit
) {
    var label by remember { mutableStateOf(slot.label) }
    var startTime by remember { mutableStateOf(slot.startTime) }
    var endTime by remember { mutableStateOf(slot.endTime) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = if (isAdding) "添加新课程节次" else "编辑节次时间安排",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BnbuNavy
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("节次名称 / 说明") },
                    placeholder = { Text("例如: 第 1 节 / TA 课辅导") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("开始时间 (HH:mm)") },
                        placeholder = { Text("08:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("结束时间 (HH:mm)") },
                        placeholder = { Text("08:50") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick preset durations
                Text(text = "快捷设置结束时间:", fontSize = 11.sp, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(50 to "50m", 60 to "1h(TA)", 105 to "105m", 170 to "170m(大课)").forEach { (mins, tag) ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                endTime = addMinutes(startTime, mins)
                            },
                            label = { Text("+$tag", fontSize = 10.5.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            val formattedStart = normalizeTime(startTime)
                            val formattedEnd = normalizeTime(endTime)
                            onSave(
                                slot.copy(
                                    label = label.ifBlank { "第 ${slot.periodNumber} 节" },
                                    startTime = formattedStart,
                                    endTime = formattedEnd,
                                    isCustom = true
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BnbuBlue)
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

private fun normalizeTime(raw: String): String {
    val trimmed = raw.trim().replace("：", ":")
    val parts = trimmed.split(":")
    if (parts.size == 2) {
        val h = parts[0].toIntOrNull() ?: 8
        val m = parts[1].toIntOrNull() ?: 0
        return String.format("%02d:%02d", h.coerceIn(0, 23), m.coerceIn(0, 59))
    }
    return if (trimmed.length == 4 && trimmed.all { it.isDigit() }) {
        "${trimmed.substring(0, 2)}:${trimmed.substring(2, 4)}"
    } else raw
}

private fun addMinutes(timeStr: String, mins: Int): String {
    val norm = normalizeTime(timeStr)
    val parts = norm.split(":")
    if (parts.size == 2) {
        val h = parts[0].toIntOrNull() ?: 8
        val m = parts[1].toIntOrNull() ?: 0
        val total = h * 60 + m + mins
        val newH = (total / 60) % 24
        val newM = total % 60
        return String.format("%02d:%02d", newH, newM)
    }
    return timeStr
}
