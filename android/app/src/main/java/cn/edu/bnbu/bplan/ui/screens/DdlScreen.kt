package cn.edu.bnbu.bplan.ui.screens

import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.bnbu.bplan.R
import cn.edu.bnbu.bplan.data.model.AssignmentDdl
import cn.edu.bnbu.bplan.data.repository.BPlanRepository
import cn.edu.bnbu.bplan.ui.components.AddDdlDialog
import cn.edu.bnbu.bplan.ui.components.DdlCard
import cn.edu.bnbu.bplan.ui.components.LocalAgreementGuard
import cn.edu.bnbu.bplan.ui.components.SubmissionDetailDialog
import cn.edu.bnbu.bplan.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DdlScreen(repository: BPlanRepository) {
    val guardAction = LocalAgreementGuard.current
    val context = LocalContext.current
    val ddls by repository.ddls.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var submissionDialogDdl by remember { mutableStateOf<AssignmentDdl?>(null) }
    var deleteConfirmDdl by remember { mutableStateOf<AssignmentDdl?>(null) }

    val pendingCount = ddls.count { !it.isSubmitted && !it.isExpired }
    val urgentCount = ddls.count { it.isUrgent }
    val completedCount = ddls.count { it.isSubmitted }

    val filteredDdls = remember(ddls, selectedTab, searchQuery) {
        ddls.filter { ddl ->
            val matchesTab = when (selectedTab) {
                0 -> true // All
                1 -> !ddl.isSubmitted // Pending
                2 -> ddl.isSubmitted // Completed
                else -> true
            }
            val matchesQuery = if (searchQuery.isBlank()) true else {
                ddl.title.contains(searchQuery, ignoreCase = true) ||
                        ddl.courseName.contains(searchQuery, ignoreCase = true)
            }
            matchesTab && matchesQuery
        }.sortedWith(
            compareBy<AssignmentDdl> { it.isSubmitted }
                .thenBy { it.dueTimestamp }
        )
    }

    val openCalendarIntent: (AssignmentDdl) -> Unit = { target ->
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "${target.title} (${target.courseName})")
                putExtra(CalendarContract.Events.DESCRIPTION, target.description ?: "iSpace DDL")
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, target.dueTimestamp - 3600 * 1000L)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, target.dueTimestamp)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Toast.makeText(context, "已调起系统日历，可设置日程提醒或闹钟", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "未找到日历应用: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.ddl_title),
                            fontWeight = FontWeight.Bold,
                            color = BnbuNavy,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "待办 $pendingCount · 紧急 $urgentCount · 已交 $completedCount",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            guardAction {
                                val updated = repository.checkAllSubmissions()
                                Toast.makeText(context, "已检查提交状态 (刷新了 $updated 项)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "检查提交状态",
                            tint = BnbuNavy
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BnbuNavy,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建待办")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索课程或作业名称...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceLight,
                    unfocusedContainerColor = SurfaceLight
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                val allStr = stringResource(R.string.ddl_filter_all)
                val pendingStr = stringResource(R.string.ddl_filter_pending)
                val submittedStr = stringResource(R.string.ddl_filter_submitted)
                val tabs = listOf("$allStr (${ddls.size})", "$pendingStr ($pendingCount)", "$submittedStr ($completedCount)")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) BnbuNavy else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Swipe tip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "💡 右划加日历/设闹钟 · 左划删除",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (filteredDdls.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = TextMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "暂无待办作业" else "没有找到相关的作业与待办",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.ddl_empty_desc),
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredDdls, key = { it.id }) { ddl ->
                        DdlCard(
                            ddl = ddl,
                            onToggleSubmitted = { guardAction { repository.toggleDdlSubmitted(ddl.id) } },
                            onViewSubmission = { submissionDialogDdl = it },
                            onAddToCalendar = { openCalendarIntent(it) },
                            onDeleteWithConfirm = { guardAction { deleteConfirmDdl = it } }
                        )
                    }
                }
            }
        }
    }

    // Add DDL Dialog
    if (showAddDialog) {
        AddDdlDialog(
            onDismiss = { showAddDialog = false },
            onAddDdl = { repository.addDdl(it) }
        )
    }

    // Submission Detail Dialog
    submissionDialogDdl?.let { ddl ->
        SubmissionDetailDialog(
            ddl = ddl,
            onDismiss = { submissionDialogDdl = null },
            onAddToCalendar = { openCalendarIntent(it) }
        )
    }

    // Delete Confirmation Dialog (提示再也不会自动添加到日程中)
    deleteConfirmDdl?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteConfirmDdl = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = UrgentRed,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "确认删除待办",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "确定要删除待办【${target.title}】吗？\n\n⚠️ 删除后该项将移入永久排除名单，后续教务与 iSpace 无论如何自动同步，都【再也不会自动添加到日程中】。",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        guardAction {
                            repository.deleteDdlPermanently(target.id)
                            deleteConfirmDdl = null
                            Toast.makeText(context, "已永久删除，后续同步将不再自动添加", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UrgentRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("确认删除", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { deleteConfirmDdl = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("取消")
                }
            }
        )
    }
}

