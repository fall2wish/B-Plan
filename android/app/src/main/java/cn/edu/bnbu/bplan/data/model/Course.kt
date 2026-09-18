package cn.edu.bnbu.bplan.data.model

enum class WeekType {
    ALL,
    ODD,
    EVEN,
    CUSTOM
}

data class Course(
    val id: String,
    val name: String,
    val code: String? = null,
    val classroom: String? = null,
    val teacher: String? = null, // 授课教师 (来自 GSAS 教务系统)
    val assistant: String? = null, // 课程助教 (来自 iSpace 教学平台，仅在 TA 辅导课显示)
    val customTime: String? = null, // 自定义上课时段描述 (如 "18:00 - 19:00 (1小时课)")
    val startTime: String? = null, // 自定义开始时间点 (如 "08:00", "18:00")
    val endTime: String? = null, // 自定义结束时间点 (如 "09:50", "19:00")
    val durationMinutes: Int? = null, // 自定义课程时长分钟 (如 50, 60, 105, 170)
    val dayOfWeek: Int, // 1: Monday, ..., 7: Sunday
    val startPeriod: Int, // 1-indexed (e.g. 1 is 08:00, 2 is 09:00)
    val endPeriod: Int,
    val startWeek: Int = 1,
    val endWeek: Int = 16,
    val weekType: WeekType = WeekType.ALL,
    val activeWeeks: List<Int> = emptyList(),
    val color: String = "#2563EB",
    val units: String = "3.0"
) {
    // 课程有效时长 (分钟)
    val effectiveDurationMinutes: Int
        get() {
            if (durationMinutes != null && durationMinutes > 0) return durationMinutes
            if (isTutorial || (customTime?.contains("1小时") == true)) return 60
            val periods = (endPeriod - startPeriod + 1).coerceAtLeast(1)
            return when (periods) {
                1 -> 50
                2 -> 105
                3 -> 170
                else -> periods * 50
            }
        }

    // 格式化时间显示
    val displayTimeString: String
        get() {
            if (!customTime.isNullOrBlank()) return customTime
            if (!startTime.isNullOrBlank() && !endTime.isNullOrBlank()) return "$startTime - $endTime"
            return "第 $startPeriod - $endPeriod 节"
        }
    val isTutorial: Boolean
        get() = id.contains("tutorial", ignoreCase = true) ||
                name.contains("Tutorial", ignoreCase = true) ||
                name.contains("(TA)") ||
                name.contains("TA课") ||
                name.startsWith("TA ") ||
                name.endsWith(" TA") ||
                name.contains("辅导")

    val displayCode: String
        get() {
            if (!code.isNullOrBlank()) return code
            val regex = Regex("""[A-Za-z]{3,4}\s*\d{4}""")
            val match = regex.find(name) ?: regex.find(id)
            if (match != null) return match.value.uppercase().replace(" ", "")
            return "BNBU 课程"
        }

    fun isActiveInWeek(week: Int): Boolean {
        if (activeWeeks.isNotEmpty()) {
            return week in activeWeeks
        }
        if (week !in startWeek..endWeek) {
            return false
        }
        return when (weekType) {
            WeekType.ALL -> true
            WeekType.ODD -> week % 2 != 0
            WeekType.EVEN -> week % 2 == 0
            WeekType.CUSTOM -> true
        }
    }
}
