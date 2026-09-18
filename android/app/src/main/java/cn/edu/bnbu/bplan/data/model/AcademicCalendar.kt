package cn.edu.bnbu.bplan.data.model

import com.google.gson.annotations.SerializedName

/**
 * 结构化学期校历模型 (Academic Calendar)
 * 适配任何学期，支持 JSON 序列化与反序列化，从 GitHub 自动获取并缓存
 */
data class AcademicCalendar(
    @SerializedName("schemaVersion") val schemaVersion: Int = 1,
    @SerializedName("semesterId") val semesterId: String = "2026-2027-1",
    @SerializedName("academicYear") val academicYear: String = "2026/27",
    @SerializedName("semesterNumber") val semesterNumber: Int = 1,
    @SerializedName("university") val university: String = "BEIJING NORMAL-HONG KONG BAPTIST UNIVERSITY",
    @SerializedName("title") val title: String = "Academic Calendar of 2026/27 (Semester I)",
    @SerializedName("shortTitle") val shortTitle: String = "2026/27 第一学期校历",
    @SerializedName("approvalInfo") val approvalInfo: String = "Approved by Senate via Circulation on 26 May 2026",
    @SerializedName("startDate") val startDate: String = "2026-08-31",
    @SerializedName("endDate") val endDate: String = "2027-01-11",
    @SerializedName("firstDayOfClasses") val firstDayOfClasses: String = "2026-09-01",
    @SerializedName("lastDayOfClasses") val lastDayOfClasses: String = "2026-12-11",
    @SerializedName("totalWeeks") val totalWeeks: Int = 16,
    @SerializedName("holidays") val holidays: List<CalendarHoliday> = emptyList(),
    @SerializedName("makeupDays") val makeupDays: List<MakeupDay> = emptyList(),
    @SerializedName("events") val events: List<CalendarEvent> = emptyList(),
    @SerializedName("remarks") val remarks: List<String> = emptyList(),
    @SerializedName("originalImageUrl") val originalImageUrl: String = "https://raw.githubusercontent.com/fall2wish/B-Plan/main/calendars/academic_calendar_2026_s1.png",
    @SerializedName("originalImageAsset") val originalImageAsset: String = "calendar/academic_calendar_2026_s1.png"
)

/**
 * 学生身份类型：本科生 (UG) vs 研究生 (PG)
 * 用于适配 BNBU 校历规则差异（例如第 8 周自主阅读周仅本科生放假，研究生正常上课）
 */
enum class StudentType(val code: String, val displayNameZh: String, val displayNameEn: String) {
    UNDERGRADUATE("UG", "本科生 (UG)", "Undergraduate (UG)"),
    POSTGRADUATE("PG", "研究生 (PG)", "Postgraduate (PG)");

    companion object {
        fun fromCode(code: String?): StudentType {
            return if (code.equals("PG", ignoreCase = true) || code.equals("POSTGRADUATE", ignoreCase = true)) {
                POSTGRADUATE
            } else {
                UNDERGRADUATE
            }
        }
    }
}

data class CalendarHoliday(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("startDate") val startDate: String,
    @SerializedName("endDate") val endDate: String,
    @SerializedName("isClassSuspended") val isClassSuspended: Boolean = true,
    @SerializedName("targetStudentType") val targetStudentType: String? = "ALL", // "ALL", "UG", "PG"
    @SerializedName("ugSuspended") val ugSuspended: Boolean? = true,
    @SerializedName("pgSuspended") val pgSuspended: Boolean? = true,
    @SerializedName("description") val description: String? = null
) {
    val isUgOnly: Boolean
        get() = isClassSuspended && (targetStudentType?.equals("UG", ignoreCase = true) == true || (pgSuspended == false && ugSuspended != false))

    /**
     * 根据学生身份精准判定该节假日是否停课
     */
    fun isSuspendedFor(type: StudentType): Boolean {
        if (!isClassSuspended) return false
        val target = targetStudentType ?: "ALL"
        return when (type) {
            StudentType.UNDERGRADUATE -> target.equals("ALL", ignoreCase = true) || target.equals("UG", ignoreCase = true)
            StudentType.POSTGRADUATE -> (target.equals("ALL", ignoreCase = true) || target.equals("PG", ignoreCase = true)) && !target.equals("UG", ignoreCase = true) && pgSuspended != false
        }
    }
}

data class MakeupDay(
    @SerializedName("date") val date: String,
    @SerializedName("sourceDayOfWeek") val sourceDayOfWeek: Int, // 1..7 (周一..周日)
    @SerializedName("targetDayOfWeek") val targetDayOfWeek: Int, // 1..7 (周一..周日)
    @SerializedName("targetDayName") val targetDayName: String, // e.g. "周五", "周一"
    @SerializedName("targetDate") val targetDate: String? = null, // e.g. "2026-09-25", "2026-10-05"
    @SerializedName("targetWeek") val targetWeek: Int? = null, // e.g. 4, 6
    @SerializedName("name") val name: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("description") val description: String
)

data class CalendarEvent(
    @SerializedName("date") val date: String,
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("title") val title: String,
    @SerializedName("category") val category: String = "GENERAL",
    @SerializedName("weekNumber") val weekNumber: Int? = null
)
