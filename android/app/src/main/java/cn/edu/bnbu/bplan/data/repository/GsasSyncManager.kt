package cn.edu.bnbu.bplan.data.repository

import cn.edu.bnbu.bplan.data.model.Course
import cn.edu.bnbu.bplan.data.model.WeekType
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class GsasSyncManager(private val repository: BPlanRepository? = null) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun syncTimetable(token: String): Result<List<Course>> = withContext(Dispatchers.IO) {
        try {
            val bearerToken = if (token.startsWith("Bearer ")) token else "Bearer $token"
            val request = Request.Builder()
                .url("https://gsas.bnbu.edu.cn/api/student/es/myTimetable?type=")
                .addHeader("pro__Access-Token", bearerToken)
                .addHeader("Access-Token", bearerToken)
                .addHeader("Authorization", bearerToken)
                .addHeader("User-Agent", "Mozilla/5.0 B-Plan-Android/1.0")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val json = JsonParser.parseString(body).asJsonObject

            // 学号一致性强校验：确保获取的课表确实归属于当前绑定的学号，防止多账号或残留 Token 串用
            val resultObj = if (json.has("result")) json.getAsJsonObject("result") else json
            val fetchedStudentNo = resultObj.get("studentNo")?.asString?.trim() ?: ""
            val expectedStudentId = repository?.studentId?.value?.trim() ?: ""
            if (expectedStudentId.isNotBlank() && fetchedStudentNo.isNotBlank()) {
                val cleanExpected = expectedStudentId.filter { it.isDigit() }
                val cleanFetched = fetchedStudentNo.filter { it.isDigit() }
                if (cleanExpected.isNotBlank() && cleanFetched.isNotBlank() &&
                    !cleanFetched.endsWith(cleanExpected) && !cleanExpected.endsWith(cleanFetched)) {
                    return@withContext Result.failure(
                        SecurityException("身份校验未通过：教务系统登录学号 ($fetchedStudentNo) 与当前绑定学号 ($expectedStudentId) 不一致！已阻止同步以防数据串用。")
                    )
                }
            }

            val courses = parseTimetableJson(json)

            if (courses.isNotEmpty()) {
                repository?.setCourses(courses)
            }
            Result.success(courses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private data class RawCourseSlot(
        val rawId: String?,
        val name: String,
        val code: String?,
        val room: String,
        val teacher: String?,
        val dayOfWeek: Int,
        val period: Int,
        val weekType: WeekType
    )

    fun parseTimetableJson(root: JsonObject): List<Course> {
        val result = if (root.has("result")) root.getAsJsonObject("result") else root
        val myTimetable = result.getAsJsonArray("myTimetable") ?: return emptyList()
        val slots = mutableListOf<RawCourseSlot>()

        for (rowElement in myTimetable) {
            val row = rowElement.asJsonObject
            val timeId = row.get("timeId")?.asInt ?: 8
            val period = timeId - 7
            val dataList = row.getAsJsonArray("dataList") ?: continue

            for (dayIdx in 0 until dataList.size()) {
                val dayOfWeek = dayIdx + 1
                val dayArray = dataList.get(dayIdx).asJsonArray ?: continue

                for (itemElem in dayArray) {
                    val item = itemElem.asJsonObject
                    val tts = item.getAsJsonObject("ttsSection") ?: JsonObject()
                    val courseObj = tts.getAsJsonObject("course") ?: JsonObject()
                    val name = tts.get("nameEn")?.asString ?: "Course"
                    val code = courseObj.get("normal")?.asString ?: courseObj.get("full")?.asString
                    val room = item.get("place")?.asString ?: "TBD"
                    val teacher = tts.get("teachers")?.asString
                    val weekStr = item.get("week")?.asString?.lowercase() ?: "all"

                    val weekType = when {
                        weekStr.contains("odd") -> WeekType.ODD
                        weekStr.contains("even") -> WeekType.EVEN
                        else -> WeekType.ALL
                    }
                    val id = item.get("id")?.asString

                    slots.add(
                        RawCourseSlot(
                            rawId = id,
                            name = name,
                            code = code,
                            room = room,
                            teacher = teacher,
                            dayOfWeek = dayOfWeek,
                            period = period,
                            weekType = weekType
                        )
                    )
                }
            }
        }

        val grouped = slots.groupBy {
            "${it.code ?: it.name}_${it.dayOfWeek}_${it.room}_${it.weekType}_${it.teacher ?: ""}"
        }

        val resultCourses = mutableListOf<Course>()

        for ((_, slotList) in grouped) {
            val sortedPeriods = slotList.map { it.period }.distinct().sorted()
            if (sortedPeriods.isEmpty()) continue

            val segments = mutableListOf<Pair<Int, Int>>()
            var segStart = sortedPeriods[0]
            var segEnd = sortedPeriods[0]

            for (i in 1 until sortedPeriods.size) {
                val p = sortedPeriods[i]
                if (p == segEnd + 1) {
                    segEnd = p
                } else {
                    segments.add(Pair(segStart, segEnd))
                    segStart = p
                    segEnd = p
                }
            }
            segments.add(Pair(segStart, segEnd))

            val sample = slotList[0]
            val color = pickColor(sample.code ?: sample.name)
            val baseId = sample.rawId ?: "c_${resultCourses.size + 1}"

            for (segment in segments) {
                val courseId = if (segments.size > 1) "${baseId}_p${segment.first}" else baseId
                resultCourses.add(
                    Course(
                        id = courseId,
                        name = sample.name,
                        code = sample.code,
                        classroom = sample.room,
                        teacher = sample.teacher,
                        dayOfWeek = sample.dayOfWeek,
                        startPeriod = segment.first,
                        endPeriod = segment.second,
                        startWeek = 1,
                        endWeek = 16,
                        weekType = sample.weekType,
                        color = color
                    )
                )
            }
        }

        return resultCourses
    }

    private fun pickColor(key: String): String {
        val colors = listOf("#2563EB", "#7C3AED", "#059669", "#D97706", "#DC2626", "#DB2777", "#0891B2")
        val idx = Math.abs(key.hashCode()) % colors.size
        return colors[idx]
    }
}
