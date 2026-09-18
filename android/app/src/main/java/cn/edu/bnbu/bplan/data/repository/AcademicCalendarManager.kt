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
import java.util.Date
import java.util.Locale

class AcademicCalendarManager(private val context: Context) {

    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences("academic_calendar_prefs", Context.MODE_PRIVATE)

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
        private const val LOCAL_CACHE_FILE = "academic_calendar_active.json"
        private const val ASSET_FILE = "calendar/academic_calendar_2026_s1.json"
    }

    init {
        loadInitialCalendar()
        checkAndAutoFetchFromGitHub()
    }

    /**
     * 初始加载校历：优先读取本地持久化缓存，若不存在则读取 assets 内置预设
     */
    private fun loadInitialCalendar() {
        try {
            val cacheFile = File(context.filesDir, LOCAL_CACHE_FILE)
            if (cacheFile.exists()) {
                val json = cacheFile.readText()
                val parsed = gson.fromJson(json, AcademicCalendar::class.java)
                if (parsed != null && parsed.semesterId.isNotBlank()) {
                    _calendar.value = parsed
                    val syncTime = prefs.getString(KEY_LAST_SYNC_TIME, null)
                    _syncStatus.value = if (syncTime != null) "已同步最新校历 ($syncTime)" else "已加载官方校历预设"
                    Log.d(TAG, "Loaded calendar from cache file: ${parsed.semesterId}")
                    return
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load cached calendar, fallback to asset: ${e.message}")
        }

        // Fallback to assets
        loadFromAssets()
    }

    private fun loadFromAssets() {
        try {
            context.assets.open(ASSET_FILE).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val parsed = gson.fromJson(reader, AcademicCalendar::class.java)
                    if (parsed != null) {
                        _calendar.value = parsed
                        _syncStatus.value = "已加载官方核准校历"
                        Log.d(TAG, "Loaded calendar from assets: ${parsed.semesterId}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading asset calendar: ${e.message}", e)
            _syncStatus.value = "已启用内置校历规则"
        }
    }

    /**
     * 每学期初次打开软件时，自动从 GitHub 获取最新校历
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

    /**
     * 手动或自动从 GitHub 拉取校历
     */
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
                    // Cache to internal storage
                    val cacheFile = File(context.filesDir, LOCAL_CACHE_FILE)
                    cacheFile.writeText(json)

                    val timeStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date())
                    prefs.edit()
                        .putString(KEY_LAST_SEMESTER, parsed.semesterId)
                        .putString(KEY_LAST_SYNC_TIME, timeStr)
                        .apply()

                    _calendar.value = parsed
                    _syncStatus.value = "已同步最新校历 ($timeStr)"
                    Log.d(TAG, "Successfully fetched and updated calendar from GitHub: ${parsed.semesterId}")
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
            _syncStatus.value = if (syncTime != null) {
                "云端暂未连通，使用已同步校历 ($syncTime)"
            } else {
                "使用官方核准校历"
            }
            _isFetching.value = false
            return Result.failure(e)
        }
    }

    /**
     * 判断指定日期是否为调休补课日
     */
    fun getMakeupDay(dateStr: String): MakeupDay? {
        val cleanDate = dateStr.trim()
        return _calendar.value.makeupDays.find { it.date == cleanDate }
    }

    /**
     * 判断指定日期是否为法定节假日或停课安排
     */
    fun getHoliday(dateStr: String): CalendarHoliday? {
        val cleanDate = dateStr.trim()
        return _calendar.value.holidays.find { holiday ->
            cleanDate in holiday.startDate..holiday.endDate
        }
    }

    /**
     * 判断指定日期对于特定学生身份是否停课
     */
    fun isClassSuspended(dateStr: String, studentType: StudentType = StudentType.UNDERGRADUATE): Boolean {
        val holiday = getHoliday(dateStr) ?: return false
        return holiday.isSuspendedFor(studentType)
    }

    /**
     * 获取官方校历原图 Bitmap（供学生在端内查看并与结构化校历核对）
     * 优先读取本地缓存文件 -> 其次读取 assets 离线内置原图 -> 若无则尝试下载
     */
    suspend fun loadCalendarOriginalImage(): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // 1. Check local cache file
            val cacheImg = File(context.filesDir, "academic_calendar_active.png")
            if (cacheImg.exists() && cacheImg.length() > 0) {
                val bmp = BitmapFactory.decodeFile(cacheImg.absolutePath)
                if (bmp != null) return@withContext bmp
            }

            // 2. Check assets built-in image
            val assetPath = _calendar.value.originalImageAsset.ifBlank { "calendar/academic_calendar_2026_s1.png" }
            try {
                context.assets.open(assetPath).use { inputStream ->
                    val bmp = BitmapFactory.decodeStream(inputStream)
                    if (bmp != null) return@withContext bmp
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed loading calendar image from assets: ${e.message}")
            }

            // 3. Fallback download from GitHub if URL present
            val urlStr = _calendar.value.originalImageUrl
            if (urlStr.isNotBlank()) {
                val url = URL(urlStr)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                }
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    conn.inputStream.use { stream ->
                        val bytes = stream.readBytes()
                        cacheImg.writeBytes(bytes)
                        return@withContext BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading calendar original image: ${e.message}", e)
        }
        null
    }

    /**
     * 计算某天的有效排课星期 (1..7)。
     * 如果是补课日 (如 9/20 周日补周五)，则返回补课的目标星期 (5)；
     * 否则返回原星期。
     */
    fun getEffectiveDayOfWeek(dateStr: String, naturalDayOfWeek: Int): Int {
        val makeup = getMakeupDay(dateStr)
        if (makeup != null) {
            return makeup.targetDayOfWeek
        }
        return naturalDayOfWeek
    }

    /**
     * 计算某天的有效排课教学周 (1..16)。
     * 如果是补课日且指定了 targetWeek (如 9/20 周日补第 4 周 9/25 课程)，则返回目标周 (4)；
     * 否则返回自然周。
     */
    fun getEffectiveWeekNumber(dateStr: String, naturalWeek: Int): Int {
        val makeup = getMakeupDay(dateStr)
        if (makeup?.targetWeek != null) {
            return makeup.targetWeek
        }
        return naturalWeek
    }
}
