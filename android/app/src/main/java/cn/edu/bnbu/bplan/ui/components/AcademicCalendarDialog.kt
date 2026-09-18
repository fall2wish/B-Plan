package cn.edu.bnbu.bplan.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.edu.bnbu.bplan.R
import cn.edu.bnbu.bplan.data.repository.AcademicCalendarManager
import cn.edu.bnbu.bplan.data.repository.BPlanRepository
import cn.edu.bnbu.bplan.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AcademicCalendarDialog(
    repository: BPlanRepository,
    onDismiss: () -> Unit
) {
    val calendar by repository.academicCalendar.collectAsState()
    val syncStatus by repository.calendarSyncStatus.collectAsState()
    val isFetching by repository.isFetchingCalendar.collectAsState()
    var showImageViewer by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceLight,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BnbuNavy.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = BnbuNavy,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.calendar_dialog_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BnbuNavy,
                                fontSize = 16.sp
                            )
                            Text(
                                text = stringResource(R.string.calendar_dialog_subtitle),
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sync status banner with refresh button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceVariantLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = BnbuBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = syncStatus,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }

                        IconButton(
                            onClick = {
                                scope.launch {
                                    repository.fetchCalendarFromGitHub()
                                }
                            },
                            enabled = !isFetching,
                            modifier = Modifier.size(24.dp)
                        ) {
                            if (isFetching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 1.5.dp,
                                    color = BnbuBlue
                                )
                            } else {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "从 GitHub 刷新",
                                    tint = BnbuBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Button to inspect official calendar image (用于学生核对结构化校历识别准确性)
                OutlinedButton(
                    onClick = { showImageViewer = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BnbuNavy),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = BnbuBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.calendar_view_original_image),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BnbuNavy
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "查看核准件 >",
                        fontSize = 10.5.sp,
                        color = BnbuBlue
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // --- 1. 补课安排特别提醒 (Make-up Days) ---
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ChangeCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.calendar_makeup_section_title),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.calendar_makeup_section_desc),
                                    fontSize = 11.5.sp,
                                    color = TextPrimary,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                calendar.makeupDays.forEach { makeup ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = makeup.date,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                    color = TextPrimary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                val targetDayStr = when (makeup.targetDayOfWeek) {
                                                    1 -> stringResource(R.string.day_mon)
                                                    2 -> stringResource(R.string.day_tue)
                                                    3 -> stringResource(R.string.day_wed)
                                                    4 -> stringResource(R.string.day_thu)
                                                    5 -> stringResource(R.string.day_fri)
                                                    6 -> stringResource(R.string.day_sat)
                                                    7 -> stringResource(R.string.day_sun)
                                                    else -> makeup.targetDayName
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.timetable_makeup_course_badge, targetDayStr),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFB45309)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = makeup.description,
                                                fontSize = 10.5.sp,
                                                color = TextSecondary,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- 2. 法定节假日与停课 (Holidays & Class Suspended) ---
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.BeachAccess,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.calendar_holiday_section_title),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                calendar.holidays.forEach { holiday ->
                                    val isUgOnly = holiday.isUgOnly
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Text(
                                                    text = holiday.name,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.5.sp,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = if (holiday.startDate == holiday.endDate) holiday.startDate else "${holiday.startDate} 至 ${holiday.endDate}",
                                                    fontSize = 10.5.sp,
                                                    color = TextMuted
                                                )
                                            }
                                            if (isUgOnly) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                                                        .border(0.5.dp, Color(0xFFF59E0B), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.calendar_holiday_ug_only_badge),
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFB45309)
                                                    )
                                                }
                                            } else if (holiday.isClassSuspended) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFFEE2E2), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.calendar_holiday_all_badge),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFDC2626)
                                                    )
                                                }
                                            }
                                        }
                                        if (!holiday.description.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = holiday.description,
                                                fontSize = 10.5.sp,
                                                color = if (isUgOnly) Color(0xFFB45309) else TextSecondary,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = DividerColor.copy(alpha = 0.5f), thickness = 0.5.dp)
                                }
                            }
                        }
                    }

                    // --- 3. 学期全景关键节点时间线 (Key Academic Milestones) ---
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Timeline,
                                        contentDescription = null,
                                        tint = BnbuNavy,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.calendar_milestones_title),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                val milestoneEvents = calendar.events.filter {
                                    it.category in listOf("CLASSES_START", "COURSE_SELECTION", "READING_WEEK", "CLASSES_END", "EXAM", "GRADE_RELEASE")
                                }

                                milestoneEvents.forEach { ev ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(BnbuBlue)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = ev.title,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = if (ev.endDate != null) "${ev.date} ~ ${ev.endDate}" else ev.date,
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- 4. Remarks ---
                    if (calendar.remarks.isNotEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceVariantLight.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = stringResource(R.string.calendar_remarks_title),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    calendar.remarks.forEach { r ->
                                        Text(
                                            text = r,
                                            fontSize = 10.sp,
                                            color = TextMuted,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Close Button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BnbuNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.calendar_btn_close), fontSize = 13.5.sp)
                }
            }
        }
    }

    if (showImageViewer) {
        CalendarImageViewerDialog(
            repository = repository,
            onDismiss = { showImageViewer = false }
        )
    }
}

@Composable
fun CalendarImageViewerDialog(
    repository: BPlanRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val calendar by repository.academicCalendar.collectAsState()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        bitmap = repository.loadCalendarOriginalImage()
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceLight,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Photo,
                            contentDescription = null,
                            tint = BnbuNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.calendar_image_viewer_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = BnbuNavy
                            )
                            Text(
                                text = stringResource(R.string.calendar_image_viewer_desc),
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Image display container (可滑动缩放核对原图)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BackgroundLight)
                        .border(0.5.dp, DividerColor, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(color = BnbuBlue, modifier = Modifier.size(32.dp))
                        }
                        bitmap != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Image(
                                    bitmap = bitmap!!.asImageBitmap(),
                                    contentDescription = "官方核准校历原图",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Icon(Icons.Default.BrokenImage, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("暂未加载到校历原图，请确认网络连接", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom bar with GitHub external link & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val url = calendar.originalImageUrl.ifBlank { AcademicCalendarManager.DEFAULT_GITHUB_URL }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("在浏览器打开原图", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BnbuNavy)
                    ) {
                        Text("关闭", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
