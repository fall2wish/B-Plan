package cn.edu.bnbu.bplan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import cn.edu.bnbu.bplan.R
import cn.edu.bnbu.bplan.data.model.AssignmentDdl
import cn.edu.bnbu.bplan.data.model.Course
import cn.edu.bnbu.bplan.data.model.PeriodSlot
import cn.edu.bnbu.bplan.data.model.ScheduleEvent
import cn.edu.bnbu.bplan.data.repository.BPlanRepository
import cn.edu.bnbu.bplan.ui.components.*
import cn.edu.bnbu.bplan.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    repository: BPlanRepository,
    onNavigateToDdl: () -> Unit
) {
    val context = LocalContext.current
    val courses by repository.courses.collectAsState()
    val ddls by repository.ddls.collectAsState()
    val events by repository.scheduleEvents.collectAsState()
    val currentWeek by repository.currentWeek.collectAsState()
    val periodSlots by repository.periodSlots.collectAsState()
    val guardAction = LocalAgreementGuard.current

    var selectedCourse by remember { mutableStateOf<Course?>(null) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showWeekPickerDialog by remember { mutableStateOf(false) }
    var showAcademicCalendarDialog by remember { mutableStateOf(false) }

    var deleteConfirmDdl by remember { mutableStateOf<AssignmentDdl?>(null) }
    var deleteConfirmEvent by remember { mutableStateOf<ScheduleEvent?>(null) }
    var submissionDialogDdl by remember { mutableStateOf<AssignmentDdl?>(null) }

    val openCalendarIntentDdl: (AssignmentDdl) -> Unit = { target ->
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

    val openCalendarIntentEvent: (ScheduleEvent) -> Unit = { target ->
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, target.title)
                putExtra(CalendarContract.Events.DESCRIPTION, target.notes ?: "校园活动日程")
                putExtra(CalendarContract.Events.EVENT_LOCATION, target.location ?: "BNBU Campus")
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, target.startTimestamp)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, target.endTimestamp)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Toast.makeText(context, "已调起系统日历，可设置日程提醒或闹钟", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "未找到日历应用: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Sliding drawer state for linked schedule
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    BottomSheetScaffold(
        scaffoldState = bottomSheetScaffoldState,
        sheetPeekHeight = 36.dp,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContainerColor = SurfaceLight,
        sheetShadowElevation = 8.dp,
        sheetContent = {
            // Sliding linked list: DDLs + Personal Schedule Events (Excluding regular classes)
            LinkedScheduleSheet(
                ddls = ddls,
                events = events,
                onToggleDdl = { ddlId -> guardAction { repository.toggleDdlSubmitted(ddlId) } },
                onAddEvent = { showAddEventDialog = true },
                onViewAllDdls = onNavigateToDdl,
                onAddToCalendarDdl = openCalendarIntentDdl,
                onDeleteDdl = { deleteConfirmDdl = it },
                onViewSubmissionDdl = { submissionDialogDdl = it },
                onAddToCalendarEvent = openCalendarIntentEvent,
                onDeleteEvent = { deleteConfirmEvent = it }
            )
        }
    ) { paddingValues ->
        Scaffold(
            topBar = {
                TimetableTopBar(
                    currentWeek = currentWeek,
                    onSelectWeek = { repository.selectWeek(it) },
                    onOpenWeekPicker = { showWeekPickerDialog = true },
                    onOpenAcademicCalendar = { showAcademicCalendarDialog = true },
                    onAddCourse = { showAddCourseDialog = true }
                )
            },
            modifier = Modifier.padding(paddingValues)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(BackgroundLight)
            ) {
                TimetableGrid(
                    repository = repository,
                    courses = courses,
                    currentWeek = currentWeek,
                    periodSlots = periodSlots,
                    onCourseClick = { selectedCourse = it },
                    onOpenWeekPicker = { showWeekPickerDialog = true }
                )
            }
        }
    }

    // Submission Detail Dialog
    submissionDialogDdl?.let { ddl ->
        SubmissionDetailDialog(
            ddl = ddl,
            onDismiss = { submissionDialogDdl = null },
            onAddToCalendar = { openCalendarIntentDdl(it) }
        )
    }

    // Delete DDL Confirmation Dialog (提示再也不会自动添加到日程中)
    deleteConfirmDdl?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteConfirmDdl = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
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

    // Delete Event Confirmation Dialog (提示再也不会自动添加到日程中)
    deleteConfirmEvent?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteConfirmEvent = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = UrgentRed,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "确认删除日程",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "确定要删除日程【${target.title}】吗？\n\n⚠️ 删除后该项将从待办与周课表中永久移除，【再也不会自动添加到日程中】。",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        guardAction {
                            repository.deleteEventPermanently(target.id)
                            deleteConfirmEvent = null
                            Toast.makeText(context, "日程已永久移除", Toast.LENGTH_SHORT).show()
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
                    onClick = { deleteConfirmEvent = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("取消")
                }
            }
        )
    }

    selectedCourse?.let { course ->
        CourseDetailDialog(
            course = course,
            onDismiss = { selectedCourse = null },
            onEdit = { c ->
                selectedCourse = null
                editingCourse = c
            },
            onDelete = { courseId ->
                guardAction {
                    repository.deleteCourse(courseId)
                    selectedCourse = null
                }
            }
        )
    }

    editingCourse?.let { course ->
        EditCourseDialog(
            course = course,
            onDismiss = { editingCourse = null },
            onSaveCourse = { updated ->
                guardAction {
                    repository.addOrUpdateCourse(updated)
                    editingCourse = null
                    Toast.makeText(context, "已保存课程【${updated.name}】的时间安排", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showAddCourseDialog) {
        AddCourseDialog(
            onDismiss = { showAddCourseDialog = false },
            onAddCourse = { newCourse ->
                guardAction {
                    repository.addCourse(newCourse)
                    showAddCourseDialog = false
                }
            }
        )
    }

    if (showAddEventDialog) {
        AddEventDialog(
            onDismiss = { showAddEventDialog = false },
            onAddEvent = { newEvent ->
                guardAction {
                    repository.addEvent(newEvent)
                    showAddEventDialog = false
                }
            }
        )
    }

    if (showWeekPickerDialog) {
        cn.edu.bnbu.bplan.ui.components.TeachingWeekPickerDialog(
            currentWeek = currentWeek,
            onDismiss = { showWeekPickerDialog = false },
            onConfirm = { repository.selectWeek(it) }
        )
    }

    if (showAcademicCalendarDialog) {
        cn.edu.bnbu.bplan.ui.components.AcademicCalendarDialog(
            repository = repository,
            onDismiss = { showAcademicCalendarDialog = false }
        )
    }
}

@Composable
private fun TimetableTopBar(
    currentWeek: Int,
    onSelectWeek: (Int) -> Unit,
    onOpenWeekPicker: () -> Unit,
    onOpenAcademicCalendar: () -> Unit,
    onAddCourse: () -> Unit
) {
    Surface(
        color = SurfaceLight,
        shadowElevation = 1.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 2.dp)
            ) {
                Text(
                    text = "BNBU",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BnbuNavy
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "S1",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {

                // Smooth Week Selector Chip (Click to open TeachingWeekPickerDialog)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceVariantLight,
                    modifier = Modifier.clickable { onOpenWeekPicker() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val weekType = if (currentWeek % 2 != 0) {
                            stringResource(R.string.timetable_week_single)
                        } else {
                            stringResource(R.string.timetable_week_double)
                        }
                        Text(
                            text = stringResource(R.string.timetable_week_format, currentWeek, weekType),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BnbuNavy
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = BnbuNavy,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(2.5.dp))

                // Academic Calendar Shortcut Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceVariantLight,
                    modifier = Modifier.clickable { onOpenAcademicCalendar() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = stringResource(R.string.timetable_calendar_btn),
                            tint = BnbuNavy,
                            modifier = Modifier.size(11.5.dp)
                        )
                        Spacer(modifier = Modifier.width(1.5.dp))
                        Text(
                            text = stringResource(R.string.timetable_calendar_btn),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BnbuNavy
                        )
                    }
                }

                Spacer(modifier = Modifier.width(1.5.dp))

                IconButton(
                    onClick = onAddCourse,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.timetable_add_course),
                        tint = BnbuNavy,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

private data class DateColInfo(
    val dayOfWeek: Int,
    val effectiveDayOfWeek: Int,
    val effectiveWeek: Int,
    val dayName: String,
    val dateStr: String,
    val fullDate: String,
    val makeup: cn.edu.bnbu.bplan.data.model.MakeupDay?,
    val holiday: cn.edu.bnbu.bplan.data.model.CalendarHoliday?,
    val isSuspended: Boolean
)

@Composable
private fun TimetableGrid(
    repository: BPlanRepository,
    courses: List<Course>,
    currentWeek: Int,
    periodSlots: List<PeriodSlot>,
    onCourseClick: (Course) -> Unit,
    onOpenWeekPicker: () -> Unit
) {
    val cal = Calendar.getInstance()
    val studentType by repository.studentType.collectAsState()
    // BNBU 2026-2027 S1: Week 1 begins Monday, Aug 31, 2026.
    val startCal = Calendar.getInstance().apply {
        set(2026, Calendar.AUGUST, 31, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.DAY_OF_YEAR, (currentWeek - 1) * 7)
    }

    val dayNames = listOf(
        stringResource(R.string.day_mon),
        stringResource(R.string.day_tue),
        stringResource(R.string.day_wed),
        stringResource(R.string.day_thu),
        stringResource(R.string.day_fri),
        stringResource(R.string.day_sat),
        stringResource(R.string.day_sun)
    )
    val weekDates = (0..6).map { dayOffset ->
        val c = (startCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH) + 1
        val d = c.get(Calendar.DAY_OF_MONTH)
        val fullDate = String.format("%04d-%02d-%02d", y, m, d)
        val makeup = repository.getMakeupDay(fullDate)
        val holiday = repository.getHoliday(fullDate)
        val isSuspended = repository.isClassSuspended(fullDate, studentType)
        val dayOfWeek = dayOffset + 1
        val effectiveDay = repository.getEffectiveDayOfWeek(fullDate, dayOfWeek)
        val effectiveWeek = repository.getEffectiveWeekNumber(fullDate, currentWeek)

        DateColInfo(
            dayOfWeek = dayOfWeek,
            effectiveDayOfWeek = effectiveDay,
            effectiveWeek = effectiveWeek,
            dayName = dayNames[dayOffset],
            dateStr = "$m/$d",
            fullDate = fullDate,
            makeup = makeup,
            holiday = holiday,
            isSuspended = isSuspended
        )
    }

    val currentMonth = cal.get(Calendar.MONTH) + 1
    val currentDay = cal.get(Calendar.DAY_OF_MONTH)
    val todayDateStr = "$currentMonth/$currentDay"

    val leftColWidth = 34.dp

    Column(modifier = Modifier.fillMaxSize()) {
        // --- 1. Weekday Header Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceLight)
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left corner button: "W3 ▼" (Click to open TeachingWeekPickerDialog)
            Box(
                modifier = Modifier
                    .width(leftColWidth)
                    .clickable { onOpenWeekPicker() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "W$currentWeek",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BnbuNavy
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = BnbuNavy,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            // 7 Days Columns with Dates & Makeup/Holiday Badges
            weekDates.forEach { info ->
                val isToday = (info.dateStr == todayDateStr)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 1.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isToday) BnbuBlue.copy(alpha = 0.12f) else Color.Transparent)
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = info.dayName,
                        fontSize = 10.5.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (isToday) BnbuBlue else TextPrimary
                    )
                    Text(
                        text = info.dateStr,
                        fontSize = 9.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) BnbuBlue else TextMuted
                    )
                    if (info.makeup != null) {
                        val targetDayStr = when (info.makeup.targetDayOfWeek) {
                            1 -> stringResource(R.string.day_mon)
                            2 -> stringResource(R.string.day_tue)
                            3 -> stringResource(R.string.day_wed)
                            4 -> stringResource(R.string.day_thu)
                            5 -> stringResource(R.string.day_fri)
                            6 -> stringResource(R.string.day_sat)
                            7 -> stringResource(R.string.day_sun)
                            else -> info.makeup.targetDayName
                        }
                        Text(
                            text = stringResource(R.string.timetable_makeup_badge, targetDayStr),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                            maxLines = 1
                        )
                    } else if (info.isSuspended) {
                        Text(
                            text = stringResource(R.string.timetable_holiday_badge),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            maxLines = 1
                        )
                    } else if (info.holiday?.id == "reading-week") {
                        Text(
                            text = stringResource(R.string.timetable_reading_week_pg_badge),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BnbuBlue,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

        if (courses.isEmpty()) {
            Surface(
                color = BnbuBlue.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = BnbuBlue, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.timetable_empty_desc),
                        fontSize = 11.sp,
                        color = BnbuNavy,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // --- 全景自适应三段网格 (一屏免滑，纯净时间刻度轴，无生硬序号) ---
        val morningSlots = periodSlots.filter { it.startMinutes < 12 * 60 + 30 }
        val afternoonSlots = periodSlots.filter { it.startMinutes in (12 * 60 + 30)..(17 * 60 + 30) }
        val eveningSlots = periodSlots.filter { it.startMinutes >= 17 * 60 + 30 }

        val morningStartMin = morningSlots.minOfOrNull { it.startMinutes } ?: (9 * 60)
        val morningEnd = morningSlots.maxOfOrNull { it.endTime } ?: "11:50"

        val afternoonStartMin = afternoonSlots.minOfOrNull { it.startMinutes } ?: (14 * 60)
        val afternoonEnd = afternoonSlots.maxOfOrNull { it.endTime } ?: "16:50"

        val eveningStartMin = eveningSlots.minOfOrNull { it.startMinutes } ?: (18 * 60)
        val eveningEnd = "21:00"

        // Session 1: 上午 (09:00, 10:00, 11:00 -> 11:50)
        SessionBlockRow(
            modifier = Modifier.weight(1f),
            slotStartTimes = listOf(
                String.format(Locale.getDefault(), "%02d:00", morningStartMin / 60),
                String.format(Locale.getDefault(), "%02d:00", morningStartMin / 60 + 1),
                String.format(Locale.getDefault(), "%02d:00", morningStartMin / 60 + 2)
            ),
            endTime = morningEnd,
            sessionStartMinutes = morningStartMin,
            courses = courses,
            currentWeek = currentWeek,
            dayColInfos = weekDates,
            matchesSession = { c ->
                val (cStart, cEnd) = getCourseStartAndEndMinutes(c)
                cStart < 12 * 60 + 30 && cEnd > 8 * 60
            },
            leftColWidth = leftColWidth,
            onCourseClick = onCourseClick
        )

        // Break 1: 午休
        BreakDividerRow(label = stringResource(R.string.timetable_break_lunch))

        // Session 2: 下午 (14:00, 15:00, 16:00 -> 16:50)
        SessionBlockRow(
            modifier = Modifier.weight(1f),
            slotStartTimes = listOf(
                String.format(Locale.getDefault(), "%02d:00", afternoonStartMin / 60),
                String.format(Locale.getDefault(), "%02d:00", afternoonStartMin / 60 + 1),
                String.format(Locale.getDefault(), "%02d:00", afternoonStartMin / 60 + 2)
            ),
            endTime = afternoonEnd,
            sessionStartMinutes = afternoonStartMin,
            courses = courses,
            currentWeek = currentWeek,
            dayColInfos = weekDates,
            matchesSession = { c ->
                val (cStart, cEnd) = getCourseStartAndEndMinutes(c)
                cStart < 17 * 60 + 30 && cEnd > 12 * 60 + 30
            },
            leftColWidth = leftColWidth,
            onCourseClick = onCourseClick
        )

        // Break 2: 晚休
        BreakDividerRow(label = stringResource(R.string.timetable_break_dinner))

        // Session 3: 晚上 (18:00, 19:00, 20:00 -> 21:00)
        SessionBlockRow(
            modifier = Modifier.weight(1f),
            slotStartTimes = listOf(
                String.format(Locale.getDefault(), "%02d:00", eveningStartMin / 60),
                String.format(Locale.getDefault(), "%02d:00", eveningStartMin / 60 + 1),
                String.format(Locale.getDefault(), "%02d:00", eveningStartMin / 60 + 2)
            ),
            endTime = eveningEnd,
            sessionStartMinutes = eveningStartMin,
            courses = courses,
            currentWeek = currentWeek,
            dayColInfos = weekDates,
            matchesSession = { c ->
                val (cStart, cEnd) = getCourseStartAndEndMinutes(c)
                cStart < 22 * 60 && cEnd > 17 * 60 + 30
            },
            leftColWidth = leftColWidth,
            onCourseClick = onCourseClick
        )
    }
}

private data class SlotBlock(
    val span: Int,
    val course: Course?,
    val extraCourses: List<Course> = emptyList()
)

@Composable
private fun SessionBlockRow(
    modifier: Modifier,
    slotStartTimes: List<String>,
    endTime: String,
    sessionStartMinutes: Int,
    courses: List<Course>,
    currentWeek: Int,
    dayColInfos: List<DateColInfo>,
    matchesSession: (Course) -> Boolean,
    leftColWidth: androidx.compose.ui.unit.Dp,
    onCourseClick: (Course) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        // Left Column: Pure time ticks (去序号化，清晰刻度)
        Box(
            modifier = Modifier
                .width(leftColWidth)
                .fillMaxHeight()
                .background(SurfaceVariantLight.copy(alpha = 0.45f))
                .border(0.25.dp, DividerColor.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                slotStartTimes.forEachIndexed { idx, time ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = time,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary.copy(alpha = 0.85f),
                            lineHeight = 9.sp,
                            softWrap = false,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
            // Bottom end time pinned at the bottom edge
            Text(
                text = endTime,
                fontSize = 7.sp,
                fontWeight = FontWeight.Normal,
                color = TextMuted,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                softWrap = false
            )
        }

        // 7 Day Cells with Calendar & Makeup Mapping
        for (dayIdx in 0..6) {
            val colInfo = dayColInfos.getOrNull(dayIdx)
            val effectiveDay = colInfo?.effectiveDayOfWeek ?: (dayIdx + 1)
            val effectiveWeek = colInfo?.effectiveWeek ?: currentWeek
            val isSuspended = colInfo?.isSuspended ?: false

            val matchingCourses = if (isSuspended) {
                emptyList()
            } else {
                courses.filter {
                    it.dayOfWeek == effectiveDay && it.isActiveInWeek(effectiveWeek) && matchesSession(it)
                }
            }

            // Check if there is any time overlap between matching courses in this session
            val hasConflict = matchingCourses.size > 1 && matchingCourses.indices.any { i ->
                val (s1, e1) = getCourseStartAndEndMinutes(matchingCourses[i])
                (i + 1 until matchingCourses.size).any { j ->
                    val (s2, e2) = getCourseStartAndEndMinutes(matchingCourses[j])
                    maxOf(s1, s2) < minOf(e1, e2)
                }
            }

            if (hasConflict) {
                // 重叠时段错开叠加布局 (Staggered horizontal offset & layered overlay)
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(0.25.dp, DividerColor.copy(alpha = 0.5f))
                        .padding(1.dp)
                ) {
                    val totalHeight = maxHeight
                    val totalWidth = maxWidth
                    val sessionDurationMin = 180f

                    // Subtle background placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceVariantLight.copy(alpha = 0.20f))
                    )

                    // 排序：开始时间早的先排，时长长的先排
                    val sortedCourses = matchingCourses.sortedWith(
                        compareBy<Course> { getCourseStartAndEndMinutes(it).first }
                            .thenByDescending { val (s, e) = getCourseStartAndEndMinutes(it); e - s }
                    )

                    sortedCourses.forEachIndexed { cIdx, c ->
                        val (cStart, cEnd) = getCourseStartAndEndMinutes(c)
                        val relStart = (cStart - sessionStartMinutes).coerceIn(0, 180)
                        val relEnd = (cEnd - sessionStartMinutes).coerceIn(0, 180)
                        val duration = (relEnd - relStart).coerceAtLeast(35)

                        val topOffset = totalHeight * (relStart.toFloat() / sessionDurationMin)
                        val cardHeight = (totalHeight * (duration.toFloat() / sessionDurationMin) - 1.dp).coerceAtLeast(26.dp)

                        val isCurrentConflicted = sortedCourses.any { other ->
                            if (other.id == c.id) false
                            else {
                                val (oS, oE) = getCourseStartAndEndMinutes(other)
                                maxOf(cStart, oS) < minOf(cEnd, oE)
                            }
                        }

                        // 水平错开：第一张课卡居左占 85% 宽度；第二张课卡向右错开 15% 叠放，占 85% 宽度并贴合右侧
                        val (offsetX, cardWidth) = if (isCurrentConflicted) {
                            val shift = if (cIdx == 0) 0f else 0.15f
                            Pair(totalWidth * shift, totalWidth * 0.85f)
                        } else {
                            Pair(0.dp, totalWidth)
                        }

                        Box(
                            modifier = Modifier
                                .offset(x = offsetX, y = topOffset)
                                .size(width = cardWidth, height = cardHeight)
                                .padding(0.5.dp)
                        ) {
                            CourseCard(
                                course = c,
                                hasConflict = isCurrentConflicted,
                                onClick = { onCourseClick(c) }
                            )
                        }
                    }
                }
            } else {
                // 3 sub-slots mapping (1 hour each: 0, 1, 2)
                val slotCourses = (0..2).map { slotIdx ->
                    val slotStart = sessionStartMinutes + slotIdx * 60
                    val slotEnd = sessionStartMinutes + (slotIdx + 1) * 60
                    matchingCourses.filter { c ->
                        val (cStart, cEnd) = getCourseStartAndEndMinutes(c)
                        cStart < slotEnd && cEnd > slotStart
                    }
                }

                // Group contiguous identical courses or empty slots into blocks
                val blocks = mutableListOf<SlotBlock>()
                var sIdx = 0
                while (sIdx < 3) {
                    val curList = slotCourses[sIdx]
                    if (curList.isEmpty()) {
                        var span = 1
                        while (sIdx + span < 3 && slotCourses[sIdx + span].isEmpty()) {
                            span++
                        }
                        blocks.add(SlotBlock(span = span, course = null))
                        sIdx += span
                    } else {
                        val c = curList[0]
                        var span = 1
                        while (sIdx + span < 3 && slotCourses[sIdx + span].isNotEmpty() && slotCourses[sIdx + span][0].id == c.id) {
                            span++
                        }
                        blocks.add(SlotBlock(span = span, course = c))
                        sIdx += span
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(0.25.dp, DividerColor.copy(alpha = 0.5f))
                        .padding(1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        blocks.forEach { block ->
                            Box(
                                modifier = Modifier
                                    .weight(block.span.toFloat())
                                    .fillMaxWidth()
                                    .padding(0.5.dp)
                            ) {
                                val c = block.course
                                if (c != null) {
                                    CourseCard(
                                        course = c,
                                        hasConflict = false,
                                        onClick = { onCourseClick(c) }
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SurfaceVariantLight.copy(alpha = 0.20f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakDividerRow(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .background(SurfaceVariantLight.copy(alpha = 0.7f))
            .border(0.25.dp, DividerColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 9.5.sp,
            color = TextMuted,
            letterSpacing = 2.sp
        )
    }
}

private fun getCourseStartAndEndMinutes(c: Course): Pair<Int, Int> {
    // 1. Explicit startTime & endTime
    if (!c.startTime.isNullOrBlank() && !c.endTime.isNullOrBlank()) {
        return Pair(parseMinutesFromTime(c.startTime), parseMinutesFromTime(c.endTime))
    }
    // 2. Parse from customTime if available (e.g. "19:00 - 20:00 (1小时课)")
    if (!c.customTime.isNullOrBlank()) {
        val regex = Regex("""(\d{1,2}:\d{2})\s*[-~至到]\s*(\d{1,2}:\d{2})""")
        val match = regex.find(c.customTime)
        if (match != null) {
            return Pair(parseMinutesFromTime(match.groupValues[1]), parseMinutesFromTime(match.groupValues[2]))
        }
    }
    // 3. BNBU 14 Standard periods
    val startMin = when (c.startPeriod) {
        1 -> 8 * 60
        2 -> 9 * 60
        3 -> 10 * 60
        4 -> 11 * 60
        5 -> 12 * 60
        6 -> 13 * 60
        7 -> 14 * 60
        8 -> 15 * 60
        9 -> 16 * 60
        10 -> 17 * 60
        11 -> 18 * 60
        12 -> 19 * 60
        13 -> 20 * 60
        else -> 21 * 60
    }
    val endMin = if (c.isTutorial || c.customTime?.contains("1小时") == true) {
        startMin + 60
    } else {
        when (c.endPeriod) {
            1 -> 8 * 60 + 50
            2 -> 9 * 60 + 50
            3 -> 10 * 60 + 50
            4 -> 11 * 60 + 50
            5 -> 12 * 60 + 50
            6 -> 13 * 60 + 50
            7 -> 14 * 60 + 50
            8 -> 15 * 60 + 50
            9 -> 16 * 60 + 50
            10 -> 17 * 60 + 50
            11 -> 18 * 60 + 50
            12 -> 19 * 60 + 50
            13 -> 20 * 60 + 50
            else -> 21 * 60 + 50
        }
    }
    return Pair(startMin, endMin)
}

private fun parseMinutesFromTime(time: String): Int {
    val parts = time.trim().split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return h * 60 + m
}

@Composable
private fun LinkedScheduleSheet(
    ddls: List<cn.edu.bnbu.bplan.data.model.AssignmentDdl>,
    events: List<ScheduleEvent>,
    onToggleDdl: (String) -> Unit,
    onAddEvent: () -> Unit,
    onViewAllDdls: () -> Unit,
    onAddToCalendarDdl: (cn.edu.bnbu.bplan.data.model.AssignmentDdl) -> Unit,
    onDeleteDdl: (cn.edu.bnbu.bplan.data.model.AssignmentDdl) -> Unit,
    onViewSubmissionDdl: (cn.edu.bnbu.bplan.data.model.AssignmentDdl) -> Unit,
    onAddToCalendarEvent: (ScheduleEvent) -> Unit,
    onDeleteEvent: (ScheduleEvent) -> Unit
) {
    val pendingDdls = ddls.filter { !it.isSubmitted && !it.isExpired }.sortedBy { it.dueTimestamp }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Drag handle indicator
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(3.5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFCBD5E1))
        )

        // Collapsed Peek Pill Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📅 周联动待办 (${pendingDdls.size + events.size}) · 上滑展开 ∧",
                fontSize = 11.5.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = BnbuNavy,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "周联动日程与待办",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BnbuNavy
                )
                if (pendingDdls.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(UrgentRed)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${pendingDdls.size}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row {
                TextButton(onClick = onAddEvent) {
                    Text("+ 日程", fontSize = 12.sp)
                }
                TextButton(onClick = onViewAllDdls) {
                    Text("全部作业 >", fontSize = 12.sp)
                }
            }
        }

        // Swipe Tip
        Text(
            text = "💡 右划加日历/设闹钟 · 左划删除",
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Independent Events
        if (events.isNotEmpty()) {
            Text(
                text = "近期学术讲座与课题组会",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            events.take(2).forEach { event ->
                val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.CHINA)
                BidirectionalSwipeBox(
                    onSwipeRightCalendar = { onAddToCalendarEvent(event) },
                    onSwipeLeftDelete = { onDeleteEvent(event) },
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF4C1D95)
                                )
                                Text(
                                    text = "${sdf.format(Date(event.startTimestamp))} @${event.location ?: "BNBU Campus"}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6D28D9)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Upcoming iSpace DDLs (Excluded regular courses)
        Text(
            text = "临近 iSpace 作业 DDL 预警",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (pendingDdls.isEmpty()) {
            Text(
                text = "太棒了，近期没有未完成的作业！",
                fontSize = 13.sp,
                color = TextMuted,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            pendingDdls.take(3).forEach { ddl ->
                DdlCard(
                    ddl = ddl,
                    onToggleSubmitted = { onToggleDdl(ddl.id) },
                    onViewSubmission = { onViewSubmissionDdl(it) },
                    onAddToCalendar = { onAddToCalendarDdl(it) },
                    onDeleteWithConfirm = { onDeleteDdl(it) }
                )
            }
        }
    }
}

