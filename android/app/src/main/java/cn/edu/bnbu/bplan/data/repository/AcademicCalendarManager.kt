package cn.edu.bnbu.bplan.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import cn.edu.bnbu.bplan.data.model.AcademicCalendar
import cn.edu.bnbu.bplan.data.model.CalendarHoliday
import cn.edu.bnbu.bplan.data.model.MakeupDay
import cn.edu.bnbu.bplan.data.model.StudentType
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 校历管理器 (AcademicCalendarManager)
 * 深度支持 BNBU 双独立校历体系：
 * 1. 本科生 (UG)：16 周标准校历，含第 8 周自主阅读周及 9/20、10/10 调休补课日
 * 2. 研究生 (PG)：14 周专属周表，第 5 周跨国庆假期切分，无调休补课，无 Reading Week，12/13 结课
 */
class AcademicCalendarManager(private val context: Context) {

    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences("academic_calendar_prefs", Context.MODE_PRIVATE)

    private val _ugCalendar = MutableStateFlow(AcademicCalendar())
    val ugCalendar: StateFlow<AcademicCalendar> = _ugCalendar.asStateFlow()

    private val _pgCalendar = MutableStateFlow(AcademicCalendar())
    val pgCalendar: StateFlow<AcademicCalendar> = _pgCalendar.asStateFlow()

    private var currentStudentType: StudentType = StudentType.UNDERGRADUATE

    private val _calendar = MutableStateFlow(AcademicCalendar())
    val calendar: StateFlow<AcademicCalendar> = _calendar.asStateFlow()

    private val _syncStatus = MutableStateFlow("正在加载校历...")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isFetching = MutableStateFlow(false)
    val isFetching: StateFlow<Boolean> = _isFetching.asStateFlow()

    companion object {
        private const val TAG = "AcademicCalendar"
        private const val KEY_LAST_SEMESTER = "last_fetched_calendar_semester"
        private const val KEY_LAST_SYNC_TIME = "last_calendar_sync_time"
        const val DEFAULT_GITHUB_URL = "https://raw.githubusercontent.com/fall2wish/B-Plan/main/calendars/academic_calendar_2026_s1.json"
        const val DEFAULT_PG_GITHUB_URL = "https://raw.githubusercontent.com/fall2wish/B-Plan/main/calendars/academic_calendar_pg_2026_s1.json"
        private const val LOCAL_CACHE_FILE = "academic_calendar_active.json"
        private const val LOCAL_PG_CACHE_FILE = "academic_calendar_pg_active.json"
        private const val ASSET_FILE = "calendar/academic_calendar_2026_s1.json"
        private const val ASSET_PG_FILE = "calendar/academic_calendar_pg_2026_s1.json"
    }

    init {
        loadInitialCalendars()
        checkAndAutoFetchFromGitHub()
    }

    /**
     * 设置当前生效的学生身份类型并切换对应校历
     */
    fun setStudentType(type: StudentType) {
        currentStudentType = type
        _calendar.value = if (type == StudentType.POSTGRADUATE) {
            _pgCalendar.value
        } else {
            _ugCalendar.value
        }
    }

    /**
     * 初始加载校历：分别加载本科生与研究生校历预设或缓存
     */
    private fun loadInitialCalendars() {
        // 1. 本科生校历
        try {
            val cacheFile = File(context.filesDir, LOCAL_CACHE_FILE)
            if (cacheFile.exists()) {
                val json = cacheFile.readText()
                val parsed = gson.fromJson(json, AcademicCalendar::class.java)
                if (parsed != null && parsed.semesterId.isNotBlank()) {
                    _ugCalendar.value = parsed
                }
            } else {
                loadUgFromAssets()
            }
        } catch (_: Exception) {
            loadUgFromAssets()
        }

        // 2. 研究生周历
        try {
            val cacheFile = File(context.filesDir, LOCAL_PG_CACHE_FILE)
            if (cacheFile.exists()) {
                val json = cacheFile.readText()
                val parsed = gson.fromJson(json, AcademicCalendar::class.java)
                if (parsed != null && parsed.semesterId.isNotBlank()) {
                    _pgCalendar.value = parsed
                }
            } else {
                loadPgFromAssets()
            }
        } catch (_: Exception) {
            loadPgFromAssets()
        }

        // 默认激活对应身份校历
        _calendar.value = if (currentStudentType == StudentType.POSTGRADUATE) _pgCalendar.value else _ugCalendar.value
        val syncTime = prefs.getString(KEY_LAST_SYNC_TIME, null)
        _syncStatus.value = if (syncTime != null) "已同步最新校历 ($syncTime)" else "已加载官方校历预设"
    }

    private fun loadUgFromAssets() {
        try {
            context.assets.open(ASSET_FILE).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val parsed = gson.fromJson(reader, AcademicCalendar::class.java)
                    if (parsed != null) _ugCalendar.value = parsed
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading UG calendar from assets: ${e.message}")
        }
    }

    private fun loadPgFromAssets() {
        try {
            context.assets.open(ASSET_PG_FILE).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val parsed = gson.fromJson(reader, AcademicCalendar::class.java)
                    if (parsed != null) _pgCalendar.value = parsed
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading PG calendar from assets: ${e.message}")
        }
    }

    /**
     * 每学期初次打开软件时，自动检查并同步最新校历
     */
    fun checkAndAutoFetchFromGitHub() {
        val currentSemester = _calendar.value.semesterId
        val lastFetchedSemester = prefs.getString(KEY_LAST_SEMESTER, null)

        if (lastFetchedSemester != currentSemester) {
            Log.i(TAG, "New semester ($currentSemester) or first launch, fetching from cloud...")
            CoroutineScope(Dispatchers.IO).launch {
                fetchFromGitHubInternal(force = false)
            }
        }
    }

    suspend fun fetchFromGitHub(githubUrl: String = DEFAULT_GITHUB_URL): Result<AcademicCalendar> {
        return withContext(Dispatchers.IO) {
            fetchFromGitHubInternal(force = true, customUrl = githubUrl)
        }
    }

    private fun fetchFromGitHubInternal(force: Boolean, customUrl: String = DEFAULT_GITHUB_URL): Result<AcademicCalendar> {
        _isFetching.value = true
        _syncStatus.value = "正在检查学期校历更新..."
        try {
            val url = URL(customUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "BNBU-B-Plan-App/2.0")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val parsed = gson.fromJson(json, AcademicCalendar::class.java)
                if (parsed != null && parsed.semesterId.isNotBlank()) {
                    val cacheFile = File(context.filesDir, LOCAL_CACHE_FILE)
                    cacheFile.writeText(json)

                    val timeStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())
                    prefs.edit()
                        .putString(KEY_LAST_SEMESTER, parsed.semesterId)
                        .putString(KEY_LAST_SYNC_TIME, timeStr)
                        .apply()

                    _ugCalendar.value = parsed
                    if (currentStudentType == StudentType.UNDERGRADUATE) {
                        _calendar.value = parsed
                    }
                    _syncStatus.value = "已同步最新校历 ($timeStr)"
                    _isFetching.value = false
                    return Result.success(parsed)
                } else {
                    throw IllegalStateException("解析得到的校历数据为空")
                }
            } else {
                throw IllegalStateException("HTTP 状态码错误: $responseCode")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Calendar fetch failed: ${e.message}, maintaining current calendar")
            val syncTime = prefs.getString(KEY_LAST_SYNC_TIME, null)
            _syncStatus.value = if (syncTime != null) "云端暂未连通，使用已同步校历 ($syncTime)" else "使用官方核准校历"
            _isFetching.value = false
            return Result.failure(e)
        }
    }

    /**
     * 判断指定日期是否为调休补课日
     * 注：研究生周表（PG）无调休补课安排，恒返回 null
     */
    fun getMakeupDay(dateStr: String, studentType: StudentType = currentStudentType): MakeupDay? {
        if (studentType == StudentType.POSTGRADUATE) return null
        val cleanDate = dateStr.trim()
        return _ugCalendar.value.makeupDays.find { it.date == cleanDate }
    }

    /**
     * 判断指定日期是否为法定节假日或停课安排
     */
    fun getHoliday(dateStr: String, studentType: StudentType = currentStudentType): CalendarHoliday? {
        val cleanDate = dateStr.trim()
        val cal = if (studentType == StudentType.POSTGRADUATE) _pgCalendar.value else _ugCalendar.value
        return cal.holidays.find { holiday ->
            cleanDate in holiday.startDate..holiday.endDate
        }
    }

    /**
     * 判断指定日期对于特定学生身份是否停课
     */
    fun isClassSuspended(dateStr: String, studentType: StudentType = currentStudentType): Boolean {
        val holiday = getHoliday(dateStr, studentType) ?: return false
        return holiday.isSuspendedFor(studentType)
    }

    /**
     * 计算某天的教学周次 (1..16)。
     * 研究生 (PG) 依据 14 周专属周表执行精确日期映射 (Week 5 跨国庆切分，无补课)
     */
    fun calculateWeekNumber(dateStr: String, studentType: StudentType = currentStudentType): Int {
        val date = dateStr.trim()
        if (studentType == StudentType.POSTGRADUATE) {
            return when {
                date < "2026-09-01" -> 1
                date in "2026-09-01".."2026-09-06" -> 1
                date in "2026-09-07".."2026-09-13" -> 2
                date in "2026-09-14".."2026-09-20" -> 3
                date in "2026-09-21".."2026-09-27" -> 4
                // Week 5 跨国庆假期切分：9.28~9.30 (上课), 10.1~10.7 (放假), 10.8~10.11 (上课) 同属第 5 周
                date in "2026-09-28".."2026-10-11" -> 5
                date in "2026-10-12".."2026-10-18" -> 6
                date in "2026-10-19".."2026-10-25" -> 7
                date in "2026-10-26".."2026-11-01" -> 8
                date in "2026-11-02".."2026-11-08" -> 9
                date in "2026-11-09".."2026-11-15" -> 10
                date in "2026-11-16".."2026-11-22" -> 11
                date in "2026-11-23".."2026-11-29" -> 12
                date in "2026-11-30".."2026-12-06" -> 13
                date in "2026-12-07".."2026-12-13" -> 14
                else -> 14
            }
        }

        // 本科生标准自然周计算 (以 2026-08-31 为第 1 周周一)
        val makeup = getMakeupDay(date, studentType)
        if (makeup?.targetWeek != null) {
            return makeup.targetWeek
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val start = sdf.parse(_ugCalendar.value.startDate) ?: return 1
            val target = sdf.parse(date) ?: return 1
            val diffDays = ((target.time - start.time) / (1000 * 60 * 60 * 24)).toInt()
            val week = (diffDays / 7) + 1
            week.coerceIn(1, _ugCalendar.value.totalWeeks)
        } catch (_: Exception) {
            1
        }
    }

    /**
     * 计算某天的有效排课星期 (1..7)。
     * 研究生无调休，直接返回自然星期；本科生在补课日返回目标星期。
     */
    fun getEffectiveDayOfWeek(dateStr: String, naturalDayOfWeek: Int, studentType: StudentType = currentStudentType): Int {
        if (studentType == StudentType.POSTGRADUATE) return naturalDayOfWeek
        val makeup = getMakeupDay(dateStr, studentType)
        if (makeup != null) {
            return makeup.targetDayOfWeek
        }
        return naturalDayOfWeek
    }

    /**
     * 计算某天的有效排课教学周 (1..16)。
     */
    fun getEffectiveWeekNumber(dateStr: String, naturalWeek: Int, studentType: StudentType = currentStudentType): Int {
        if (studentType == StudentType.POSTGRADUATE) {
            return calculateWeekNumber(dateStr, studentType)
        }
        val makeup = getMakeupDay(dateStr, studentType)
        if (makeup?.targetWeek != null) {
            return makeup.targetWeek
        }
        return naturalWeek
    }

    /**
     * 获取官方校历原图 Bitmap
     */
    suspend fun loadCalendarOriginalImage(): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val isPg = currentStudentType == StudentType.POSTGRADUATE
            val assetPath = if (isPg) {
                _pgCalendar.value.originalImageAsset.ifBlank { "calendar/academic_calendar_pg_2026_s1.png" }
            } else {
                _ugCalendar.value.originalImageAsset.ifBlank { "calendar/academic_calendar_2026_s1.png" }
            }

            try {
                context.assets.open(assetPath).use { inputStream ->
                    val bmp = BitmapFactory.decodeStream(inputStream)
                    if (bmp != null) return@withContext bmp
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed loading calendar image from assets: ${e.message}")
            }

            // Fallback to UG asset if PG image not found
            if (isPg) {
                try {
                    context.assets.open("calendar/academic_calendar_2026_s1.png").use { inputStream ->
                        val bmp = BitmapFactory.decodeStream(inputStream)
                        if (bmp != null) return@withContext bmp
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading calendar original image: ${e.message}", e)
        }
        null
    }
}
