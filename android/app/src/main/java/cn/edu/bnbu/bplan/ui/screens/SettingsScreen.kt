package cn.edu.bnbu.bplan.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import cn.edu.bnbu.bplan.data.model.StudentType
import cn.edu.bnbu.bplan.data.repository.BPlanRepository
import cn.edu.bnbu.bplan.data.repository.IcsExporter
import cn.edu.bnbu.bplan.ui.components.*
import cn.edu.bnbu.bplan.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repository: BPlanRepository) {
    val context = LocalContext.current
    val courses by repository.courses.collectAsState()
    val ddls by repository.ddls.collectAsState()
    val events by repository.scheduleEvents.collectAsState()
    val currentWeek by repository.currentWeek.collectAsState()
    val periodSlots by repository.periodSlots.collectAsState()
    val appLanguage by repository.appLanguage.collectAsState()

    val academicCalendar by repository.academicCalendar.collectAsState()
    val calendarSyncStatus by repository.calendarSyncStatus.collectAsState()

    val lastGsasSyncTime by repository.lastGsasSyncTime.collectAsState()
    val lastIspaceSyncTime by repository.lastIspaceSyncTime.collectAsState()
    val isSsoAuthorized by repository.isSsoAuthorized.collectAsState()
    val hasSecureCredentials by repository.hasSecureCredentials.collectAsState()
    val campusUsername by repository.campusAccountUsername.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val guardAction = LocalAgreementGuard.current

    var isSyncingGsas by remember { mutableStateOf(false) }
    var isSyncingIspace by remember { mutableStateOf(false) }

    var showCampusCredentialDialog by remember { mutableStateOf(false) }
    var showCampusWebAuthDialog by remember { mutableStateOf(false) }
    var webAuthInitialTarget by remember { mutableStateOf(WebSyncTarget.GSAS) }
    var webAuthForceReauth by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showWeekPickerDialog by remember { mutableStateOf(false) }
    var showPeriodSlotsDialog by remember { mutableStateOf(false) }
    var showAcademicCalendarDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showUserAgreementDialog by remember { mutableStateOf(false) }
    val studentType by repository.studentType.collectAsState()
    var showStudentTypeDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold,
                        color = BnbuNavy,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sync Center: 教务系统与教学平台数据同步
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Sync, contentDescription = null, tint = BnbuBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_sync_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = BnbuNavy
                            )
                        }

                        if (isSsoAuthorized) {
                            TextButton(
                                onClick = { guardAction { showLogoutConfirmDialog = true } },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "退出登录",
                                    color = Color(0xFFDC2626),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 校园统一身份认证 (SSO) 硬件加密安全卡片
                    Surface(
                        color = if (hasSecureCredentials) BnbuBlue.copy(alpha = 0.08f) else Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { guardAction { showCampusCredentialDialog = true } }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (hasSecureCredentials) Icons.Default.Shield else Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = if (hasSecureCredentials) BnbuNavy else Color(0xFFD97706),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (hasSecureCredentials) "校园统一认证 (SSO已绑定)" else "绑定校园统一认证账号",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = BnbuNavy
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (hasSecureCredentials) SuccessGreen.copy(alpha = 0.15f) else Color(0xFFFEF3C7),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (hasSecureCredentials) "TEE 硬件芯片加密" else "推荐配置",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (hasSecureCredentials) SuccessGreen else Color(0xFFD97706)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (hasSecureCredentials) {
                                            val displayId = campusUsername.ifBlank { repository.studentId.value }
                                            "学号: $displayId · 芯片物理隔离（不上传服务器，数据仅在本地加密保存）"
                                        } else {
                                            "硬件加密存储学号密码（不上传服务器，数据仅在本地加密保存）"
                                        },
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 快捷网页登录同步入口
                    Surface(
                        color = BnbuNavy.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    guardAction {
                                        webAuthInitialTarget = WebSyncTarget.GSAS
                                        showCampusWebAuthDialog = true
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = BnbuBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isSsoAuthorized) "网页登录凭据已就绪" else "网页辅助登录与验证",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = BnbuNavy
                                    )
                                    Text(
                                        text = if (isSsoAuthorized) {
                                            "日常数据均在后台全自动静默同步；若遇验证码或密码修改可点此打开网页辅助登录"
                                        } else {
                                            "保存学号密码后系统将全自动后台静默登录；若遇验证码可点此人工辅助登录"
                                        },
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // GSAS Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = stringResource(R.string.settings_gsas_title),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(BnbuBlue.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_gsas_courses_count, courses.size),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = BnbuBlue
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(
                                    R.string.settings_last_sync,
                                    formatSyncTime(lastGsasSyncTime, stringResource(R.string.settings_sync_never))
                                ),
                                fontSize = 11.sp,
                                color = if (lastGsasSyncTime > 0L) SuccessGreen else TextMuted
                            )
                        }
                        Button(
                            onClick = {
                                guardAction {
                                    isSyncingGsas = true
                                    scope.launch {
                                        if (repository.gsasToken.isBlank()) {
                                            if (hasSecureCredentials) {
                                                val silentRes = repository.triggerSilentAuthAndSync(forceReauth = false)
                                                isSyncingGsas = false
                                                if (silentRes.isSuccess) {
                                                    snackbarHostState.showSnackbar("GSAS 课表已全自动静默同步！")
                                                } else {
                                                    webAuthInitialTarget = WebSyncTarget.GSAS
                                                    showCampusWebAuthDialog = true
                                                }
                                            } else {
                                                isSyncingGsas = false
                                                webAuthInitialTarget = WebSyncTarget.GSAS
                                                showCampusWebAuthDialog = true
                                            }
                                        } else {
                                            val res = repository.syncGsasTimetable()
                                            if (res.isSuccess) {
                                                isSyncingGsas = false
                                                snackbarHostState.showSnackbar("GSAS 课表同步完成！")
                                            } else {
                                                if (hasSecureCredentials) {
                                                    val silentRes = repository.triggerSilentAuthAndSync(forceReauth = false)
                                                    isSyncingGsas = false
                                                    if (silentRes.isSuccess) {
                                                        snackbarHostState.showSnackbar("已自动重登并更新 GSAS 课表！")
                                                    } else {
                                                        val err = res.exceptionOrNull()?.message ?: "同步失败"
                                                        snackbarHostState.showSnackbar(err)
                                                    }
                                                } else {
                                                    isSyncingGsas = false
                                                    val err = res.exceptionOrNull()?.message ?: "同步失败"
                                                    snackbarHostState.showSnackbar(err)
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isSyncingGsas,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BnbuNavy)
                        ) {
                            if (isSyncingGsas) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (repository.gsasToken.isBlank() && !hasSecureCredentials) "登录同步" else stringResource(R.string.settings_gsas_sync_btn),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))

                    // iSpace Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = stringResource(R.string.settings_ispace_title),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_ispace_ddls_count, ddls.size),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(
                                    R.string.settings_last_sync,
                                    formatSyncTime(lastIspaceSyncTime, stringResource(R.string.settings_sync_never))
                                ),
                                fontSize = 11.sp,
                                color = if (lastIspaceSyncTime > 0L) SuccessGreen else TextMuted
                            )
                        }
                        Button(
                            onClick = {
                                guardAction {
                                    isSyncingIspace = true
                                    scope.launch {
                                        if (repository.ispaceCookie.isBlank()) {
                                            if (hasSecureCredentials) {
                                                val silentRes = repository.triggerSilentAuthAndSync(forceReauth = false)
                                                isSyncingIspace = false
                                                if (silentRes.isSuccess) {
                                                    snackbarHostState.showSnackbar("iSpace 待办已全自动静默同步！")
                                                } else {
                                                    webAuthInitialTarget = WebSyncTarget.ISPACE
                                                    showCampusWebAuthDialog = true
                                                }
                                            } else {
                                                isSyncingIspace = false
                                                webAuthInitialTarget = WebSyncTarget.ISPACE
                                                showCampusWebAuthDialog = true
                                            }
                                        } else {
                                            val res = repository.syncIspaceData()
                                            if (res.isSuccess) {
                                                isSyncingIspace = false
                                                snackbarHostState.showSnackbar("iSpace 作业列表同步成功！")
                                            } else {
                                                if (hasSecureCredentials) {
                                                    val silentRes = repository.triggerSilentAuthAndSync(forceReauth = false)
                                                    isSyncingIspace = false
                                                    if (silentRes.isSuccess) {
                                                        snackbarHostState.showSnackbar("已自动重登并更新 iSpace 待办！")
                                                    } else {
                                                        val err = res.exceptionOrNull()?.message ?: "同步失败"
                                                        snackbarHostState.showSnackbar(err)
                                                    }
                                                } else {
                                                    isSyncingIspace = false
                                                    val err = res.exceptionOrNull()?.message ?: "同步失败"
                                                    snackbarHostState.showSnackbar(err)
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isSyncingIspace,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                        ) {
                            if (isSyncingIspace) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = if (repository.ispaceCookie.isBlank() && !hasSecureCredentials) "登录同步" else stringResource(R.string.settings_ispace_sync_btn),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Academic Calendar Card (校历展示)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = BnbuNavy, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_calendar_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = BnbuNavy
                            )
                        }
                        Text(
                            text = academicCalendar.semesterId,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BnbuBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = academicCalendar.shortTitle.ifBlank { academicCalendar.title },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.settings_calendar_weeks_format,
                            academicCalendar.startDate,
                            academicCalendar.endDate,
                            academicCalendar.totalWeeks
                        ),
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = calendarSyncStatus,
                        fontSize = 11.sp,
                        color = if (calendarSyncStatus.contains("已同步") || calendarSyncStatus.contains("已加载")) SuccessGreen else TextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showAcademicCalendarDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BnbuNavy),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_calendar_view_btn), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Timetable Settings Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_rules_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BnbuNavy
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val singleOrDouble = if (currentWeek % 2 != 0) stringResource(R.string.timetable_week_single) else stringResource(R.string.timetable_week_double)
                    SettingItem(
                        icon = Icons.Default.DateRange,
                        title = stringResource(R.string.settings_current_teaching_week),
                        value = stringResource(R.string.settings_teaching_week_format, currentWeek, singleOrDouble),
                        onClick = {
                            showWeekPickerDialog = true
                        }
                    )

                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                    SettingItem(
                        icon = Icons.Default.Schedule,
                        title = stringResource(R.string.settings_period_slots),
                        value = stringResource(R.string.settings_period_slots_count, periodSlots.size),
                        onClick = {
                            showPeriodSlotsDialog = true
                        }
                    )

                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                    SettingItem(
                        icon = Icons.Default.School,
                        title = stringResource(R.string.student_type_title),
                        value = if (studentType == StudentType.UNDERGRADUATE) "本科生 (UG) · 享读书周放假" else "研究生 (PG) · 读书周正常排课",
                        onClick = {
                            showStudentTypeDialog = true
                        }
                    )
                }
            }

            // General & Multi-Language Preferences Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "通用与语言 / General & Language",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BnbuNavy
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingItem(
                        icon = Icons.Default.Language,
                        title = "语言设置 / Language",
                        value = when (appLanguage) {
                            cn.edu.bnbu.bplan.util.AppLanguage.SIMPLIFIED_CHINESE -> "简体中文 / Simplified Chinese"
                            cn.edu.bnbu.bplan.util.AppLanguage.TRADITIONAL_CHINESE_HK -> "繁體中文 / Traditional Chinese"
                            cn.edu.bnbu.bplan.util.AppLanguage.ENGLISH -> "English / 英语"
                        },
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            // Calendar Export Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_export_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BnbuNavy
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_export_desc),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            guardAction {
                                try {
                                    val icsContent = IcsExporter.exportCoursesAndEventsToIcs(
                                        courses = courses,
                                        events = events,
                                        semesterStartEpochMillis = repository.semesterStartMillis
                                    )

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/calendar"
                                        putExtra(Intent.EXTRA_SUBJECT, "B-Plan 课程表.ics")
                                        putExtra(Intent.EXTRA_TEXT, icsContent)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "分享或导出日历文件"))
                                    scope.launch {
                                        snackbarHostState.showSnackbar("已生成 .ics 日历文件并调用系统分享！")
                                    }
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("导出失败: ${e.message}")
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BnbuBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_export_btn))
                    }
                }
            }

            // Clean Data Reset Card (无废话，直接重置按钮)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = { guardAction { showResetConfirmDialog = true } },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "重置本地数据 / Reset Local Data",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // About & Privacy Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_about_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BnbuNavy
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_about_desc),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.settings_version_info),
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { showUserAgreementDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BnbuNavy),
                        border = BorderStroke(1.dp, BnbuNavy.copy(alpha = 0.35f))
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = BnbuNavy)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("查看用户须知与免责协议", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = BnbuNavy)
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentLanguage = appLanguage,
            onDismiss = { showLanguageDialog = false },
            onSelectLanguage = { newLang ->
                repository.setAppLanguage(newLang)
            }
        )
    }

    if (showWeekPickerDialog) {
        TeachingWeekPickerDialog(
            currentWeek = currentWeek,
            onDismiss = { showWeekPickerDialog = false },
            onConfirm = { newWeek ->
                repository.selectWeek(newWeek)
                scope.launch {
                    snackbarHostState.showSnackbar("已切换至第 $newWeek 周！")
                }
            }
        )
    }

    if (showPeriodSlotsDialog) {
        PeriodSlotListDialog(
            repository = repository,
            onDismiss = { showPeriodSlotsDialog = false }
        )
    }

    if (showAcademicCalendarDialog) {
        AcademicCalendarDialog(
            repository = repository,
            onDismiss = { showAcademicCalendarDialog = false }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "确认重置本地数据？\nConfirm Data Reset?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BnbuNavy
                )
            },
            text = {
                Text(
                    text = "将清空所有已识别的课表、待办作业与邮件日程，保留账号认证与登录信息。\n\n重置后可点击上方的【同步课表】与【同步作业】重新同步。\n\nAll locally identified courses, assignments, and schedule events will be cleared. Account login credentials will be preserved.",
                    fontSize = 12.5.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        repository.resetAllSyncedData()
                        scope.launch {
                            snackbarHostState.showSnackbar("本地数据已重置，账号认证已保留！")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("确认重置 / Reset", fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetConfirmDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("取消 / Cancel", fontSize = 12.sp)
                }
            }
        )
    }

    if (showCampusCredentialDialog) {
        CampusCredentialDialog(
            repository = repository,
            onDismiss = { showCampusCredentialDialog = false },
            onSaveAndSync = {
                showCampusCredentialDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("账号凭据已安全硬件加密保存！正在全自动静默同步...")
                    repository.triggerSilentAuthAndSync(forceReauth = false)
                }
            },
            onCleared = {
                showCampusCredentialDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("已解除账号绑定并清除本地加密凭据")
                }
            }
        )
    }

    if (showCampusWebAuthDialog) {
        CampusWebAuthDialog(
            repository = repository,
            initialTarget = webAuthInitialTarget,
            forceReauth = webAuthForceReauth,
            onDismiss = {
                showCampusWebAuthDialog = false
                webAuthForceReauth = false
            },
            onSyncSuccess = { msg ->
                scope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        )
    }

    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "退出校园账号登录？\nLog out of Campus Account?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BnbuNavy
                )
            },
            text = {
                Text(
                    text = "退出后将清除保存在本地的教务系统与教学平台会话凭据、独立安全芯片中硬件加密的账号密码及 WebView 缓存 Cookie。本地已同步的课表与作业仍将保留，直到您点击重置数据。\n\nLogging out will clear stored GSAS/iSpace session tokens, hardware-encrypted credentials, and WebView cookies. Your local timetable and assignments will remain intact until you reset data.",
                    fontSize = 12.5.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        repository.logoutCampusAccount()
                        scope.launch {
                            snackbarHostState.showSnackbar("已退出登录并清除会话凭据")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("确认退出 / Log out", fontSize = 12.sp)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutConfirmDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("取消 / Cancel", fontSize = 12.sp)
                }
            }
        )
    }

    if (showUserAgreementDialog) {
        UserAgreementDialog(
            isFirstLaunch = false,
            onAccept = { showUserAgreementDialog = false },
            onBrowseOnly = { showUserAgreementDialog = false },
            onDismiss = { showUserAgreementDialog = false }
        )
    }

    if (showStudentTypeDialog) {
        StudentTypePickerDialog(
            currentType = studentType,
            onDismiss = { showStudentTypeDialog = false },
            onSelect = { newType ->
                guardAction {
                    repository.setStudentType(newType)
                    scope.launch {
                        snackbarHostState.showSnackbar("已更新身份为：${newType.displayNameZh}")
                    }
                }
            }
        )
    }
}

private fun formatSyncTime(timestamp: Long, neverText: String): String {
    if (timestamp <= 0L) return neverText
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
private fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = title, fontSize = 14.sp, color = TextPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, fontSize = 13.sp, color = BnbuBlue, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun StudentTypePickerDialog(
    currentType: StudentType,
    onDismiss: () -> Unit,
    onSelect: (StudentType) -> Unit
) {
    var selectedType by remember { mutableStateOf(currentType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.School, contentDescription = null, tint = BnbuNavy, modifier = Modifier.size(28.dp))
        },
        title = {
            Text(
                text = stringResource(R.string.student_type_dialog_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BnbuNavy
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.student_type_dialog_desc),
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Option 1: Undergraduate
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedType == StudentType.UNDERGRADUATE) BnbuBlue.copy(alpha = 0.1f) else BackgroundLight,
                    border = if (selectedType == StudentType.UNDERGRADUATE) BorderStroke(1.5.dp, BnbuBlue) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedType = StudentType.UNDERGRADUATE }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = selectedType == StudentType.UNDERGRADUATE,
                            onClick = { selectedType = StudentType.UNDERGRADUATE },
                            colors = RadioButtonDefaults.colors(selectedColor = BnbuBlue)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.student_type_ug),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BnbuNavy
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("享读书周放假", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.student_type_ug_desc),
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: Postgraduate
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedType == StudentType.POSTGRADUATE) BnbuBlue.copy(alpha = 0.1f) else BackgroundLight,
                    border = if (selectedType == StudentType.POSTGRADUATE) BorderStroke(1.5.dp, BnbuBlue) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedType = StudentType.POSTGRADUATE }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = selectedType == StudentType.POSTGRADUATE,
                            onClick = { selectedType = StudentType.POSTGRADUATE },
                            colors = RadioButtonDefaults.colors(selectedColor = BnbuBlue)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.student_type_pg),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BnbuNavy
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFEFF6FF), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("读书周照常排课", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BnbuBlue)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.student_type_pg_desc),
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSelect(selectedType)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BnbuBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.btn_confirm), fontSize = 12.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.btn_cancel), fontSize = 12.sp)
            }
        }
    )
}
