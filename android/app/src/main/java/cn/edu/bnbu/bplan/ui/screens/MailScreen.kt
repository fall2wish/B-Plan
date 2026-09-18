package cn.edu.bnbu.bplan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.edu.bnbu.bplan.R
import cn.edu.bnbu.bplan.data.model.EmailMessage
import cn.edu.bnbu.bplan.data.model.ScheduleEvent
import cn.edu.bnbu.bplan.data.repository.BPlanRepository
import cn.edu.bnbu.bplan.ui.components.LocalAgreementGuard
import cn.edu.bnbu.bplan.ui.components.MailSettingsDialog
import cn.edu.bnbu.bplan.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailScreen(repository: BPlanRepository) {
    val guardAction = LocalAgreementGuard.current
    val emails by repository.emails.collectAsState()
    val mailConfig by repository.mailConfig.collectAsState()
    var selectedEmail by remember { mutableStateOf<EmailMessage?>(null) }
    var showMailSettingsDialog by remember { mutableStateOf(false) }
    var isFetchingMails by remember { mutableStateOf(false) }
    var syncErrorDetails by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val performFetchMails: () -> Unit = {
        guardAction {
            isFetchingMails = true
            scope.launch {
                val result = repository.syncRealEmails()
                isFetchingMails = false
                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    snackbarHostState.showSnackbar("已成功通过 ${mailConfig.protocol} 协议同步 $count 封最新邮件！")
                } else {
                    val err = result.exceptionOrNull()?.message ?: "邮件拉取失败"
                    syncErrorDetails = err
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.mail_screen_title),
                            fontWeight = FontWeight.Bold,
                            color = BnbuNavy,
                            fontSize = 18.sp
                        )
                        Text(
                            text = stringResource(R.string.mail_screen_subtitle),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMailSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = stringResource(R.string.mail_settings_btn),
                            tint = BnbuNavy
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (mailConfig.isConnected) SuccessGreen else UrgentRed)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (mailConfig.isConnected) stringResource(R.string.mail_connected) else stringResource(R.string.mail_not_connected),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (mailConfig.isConnected) SuccessGreen else UrgentRed
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(BnbuBlue.copy(alpha = 0.1f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${mailConfig.protocol}:${mailConfig.incomingPort} ${if (mailConfig.useIncomingSsl) "SSL" else ""}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BnbuBlue
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { showMailSettingsDialog = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(13.dp), tint = BnbuNavy)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(stringResource(R.string.mail_settings_btn), fontSize = 11.sp, color = BnbuNavy, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.mail_account_prefix, mailConfig.emailAddress),
                                fontSize = 11.5.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )

                            IconButton(
                                onClick = performFetchMails,
                                enabled = !isFetchingMails,
                                modifier = Modifier.size(28.dp)
                            ) {
                                if (isFetchingMails) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.mail_sync_now), tint = BnbuBlue, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            items(emails, key = { it.id }) { email ->
                EmailCard(
                    email = email,
                    onClick = { selectedEmail = email }
                )
            }
        }
    }

    if (showMailSettingsDialog) {
        MailSettingsDialog(
            currentConfig = mailConfig,
            onDismiss = { showMailSettingsDialog = false },
            onSaveConfig = { newConfig ->
                repository.updateMailConfig(newConfig)
                performFetchMails()
            }
        )
    }

    syncErrorDetails?.let { err ->
        AlertDialog(
            onDismissRequest = { syncErrorDetails = null },
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = UrgentRed) },
            title = { Text("邮件拉取失败诊断", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = err,
                        fontSize = 12.5.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        syncErrorDetails = null
                        showMailSettingsDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BnbuBlue)
                ) {
                    Text("修改密码 / 授权码")
                }
            },
            dismissButton = {
                TextButton(onClick = { syncErrorDetails = null }) {
                    Text("知道了")
                }
            }
        )
    }

    selectedEmail?.let { email ->
        EmailDetailDialog(
            email = email,
            onDismiss = { selectedEmail = null },
            onConvertToSchedule = { event ->
                guardAction {
                    repository.convertEmailToEvent(email, event)
                    selectedEmail = null
                    scope.launch {
                        snackbarHostState.showSnackbar("已成功将【${event.title}】添加至您的日程与课表！")
                    }
                }
            }
        )
    }
}

@Composable
private fun EmailCard(
    email: EmailMessage,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Sender Avatar or Unread Dot
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (!email.isRead) BnbuNavy else Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = if (!email.isRead) Color.White else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = email.sender,
                        fontWeight = if (!email.isRead) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Text(
                        text = sdf.format(Date(email.dateTimestamp)),
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = email.subject,
                    fontWeight = if (!email.isRead) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = email.bodyText.replace("\n", " "),
                    fontSize = 12.sp,
                    color = TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (email.extractedEvents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFDF2F8))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFDB2777),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "智能识别出活动日程 · 点击查看 / 添加到日程",
                                color = Color(0xFFDB2777),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmailDetailDialog(
    email: EmailMessage,
    onDismiss: () -> Unit,
    onConvertToSchedule: (ScheduleEvent) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Fixed Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "邮件详情",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = BnbuNavy
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = email.subject, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(3.dp))
                Text(text = "发件人: ${email.sender}", fontSize = 12.sp, color = TextSecondary)
                Text(text = "日期: ${sdf.format(Date(email.dateTimestamp))}", fontSize = 11.sp, color = TextMuted)

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DividerColor)

                // Scrollable Body & Add Schedule Section
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = email.bodyText,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Smart NLP Extraction Card
                    if (email.extractedEvents.isNotEmpty()) {
                        email.extractedEvents.forEach { event ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF2F8)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFDB2777))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "已从正文智能识别到日程",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF9D174D)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "主题: ${event.title}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(
                                        text = "时间: ${sdf.format(Date(event.startTimestamp))} - ${sdf.format(Date(event.endTimestamp))}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "地点: ${event.location ?: "BNBU Campus"}",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { onConvertToSchedule(event) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB2777)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("添加到日程")
                                    }
                                }
                            }
                        }
                    } else {
                        // Manual add fallback
                        OutlinedButton(
                            onClick = {
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = email.dateTimestamp
                                }
                                val start = cal.timeInMillis
                                cal.add(Calendar.HOUR_OF_DAY, 1)
                                val end = cal.timeInMillis
                                val manualEvent = ScheduleEvent(
                                    id = UUID.randomUUID().toString(),
                                    title = email.subject.take(40),
                                    startTimestamp = start,
                                    endTimestamp = end,
                                    location = "邮件日程",
                                    notes = email.bodyText.take(100),
                                    source = "email",
                                    color = "#EC4899"
                                )
                                onConvertToSchedule(manualEvent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BnbuNavy),
                            border = BorderStroke(1.dp, BnbuNavy.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("将此邮件添加至日程")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
