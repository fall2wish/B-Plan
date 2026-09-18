package cn.edu.bnbu.bplan.data.repository

import cn.edu.bnbu.bplan.data.model.ScheduleEvent
import java.util.Calendar
import java.util.UUID
import java.util.regex.Pattern

object EmailParser {

    /**
     * 正向活动类型指标 (Positive Event Type Indicators)
     * 邮件标题或正文中必须包含明确的校园活动、讲座、学术研讨、会议、比赛或仪式特征词
     */
    private val POSITIVE_EVENT_INDICATORS = listOf(
        // 学术与讲座类
        Pattern.compile("""(?:讲座|大讲堂|报告会|学术报告|学术讲座|分享会|研讨会|工作坊|公开课|思政课|学术沙龙|Lecture|Seminar|Workshop|Conference|Symposium|Colloquium|Forum|Talk)""", Pattern.CASE_INSENSITIVE),
        // 组会与会议类
        Pattern.compile("""(?:组会|课题组会|班会|例会|全体会议|工作会议|答辩|开题|结题|Meeting|Briefing|Presentation)""", Pattern.CASE_INSENSITIVE),
        // 校园活动与竞赛类
        Pattern.compile("""(?:比赛|竞赛|选拔赛|决赛|开学典礼|毕业典礼|迎新晚会|音乐会|演出|培训班|训练营|Competition|Contest|Hackathon|Ceremony|Training)""", Pattern.CASE_INSENSITIVE),
        // 结构化活动议程标签 (Structured Agenda Labels)
        Pattern.compile("""(?:活动时间|讲座时间|会议时间|比赛时间|演出时间|上课时间)[:：]|(?:活动地点|讲座地点|会议地点|比赛地点|Venue|Location)[:：]""", Pattern.CASE_INSENSITIVE)
    )

    /**
     * 正向校园地点模式 (Positive Location Patterns)
     */
    private val LOCATION_PATTERNS = listOf(
        Pattern.compile("""(?:地点|教室|Room|Location|Venue)[:：\s]+([^\n,，。；;\(\)（）]{2,25})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""\b([TV]\d{1,2}[-_]\d{3,4})\b""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""\b(CC[-_]?\d{3})\b""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(大学会堂|演艺厅|学生活动中心|体育馆|行政楼|LRC\s*\d{0,3}|图书馆|Learning Resource Centre|University Hall|Gymnasium|Admin Building|Auditorium|Online|Zoom|腾讯会议)""", Pattern.CASE_INSENSITIVE)
    )

    /**
     * 正向日期模式
     */
    private val FULL_DATE_PATTERN = Pattern.compile("""(?:(\d{4})[年\-\/\.])?\s*(\d{1,2})[月\-\/\.]\s*(\d{1,2})[日号]?""")

    /**
     * 正向具体时间段模式 (如 14:00 - 16:30, 14:00~15:30, 9:00 至 11:00)
     */
    private val TIME_RANGE_PATTERN = Pattern.compile("""(?:(?:时间|Time)[:：\s]*)?(\d{1,2})[:：](\d{2})\s*(?:-|–|~|至|to)\s*(\d{1,2})[:：](\d{2})""")

    /**
     * 正向前缀声明的活动时间模式 (如 "时间：14:00", "讲座时间: 15:30")
     */
    private val LABELED_TIME_PATTERN = Pattern.compile("""(?:活动时间|讲座时间|会议时间|演出时间|时间|Time)[:：\s]+(?:[^\d\n]{0,10}?)(\d{1,2})[:：](\d{2})""", Pattern.CASE_INSENSITIVE)

    fun parseEventsFromEmail(
        emailId: String,
        subject: String,
        body: String,
        emailTimestamp: Long
    ): List<ScheduleEvent> {
        // 1. 提取当前邮件核心正文（剥离邮件客户端自动附加的回复引用历史，获取发信人撰写的本次内容）
        val cleanBody = stripQuotedHistory(body)
        val fullText = "$subject\n$cleanBody"

        // 2. 正向规则校验一：活动特征指标 (Positive Event Indicator)
        var matchedEventIndicator = false
        for (pattern in POSITIVE_EVENT_INDICATORS) {
            if (pattern.matcher(fullText).find()) {
                matchedEventIndicator = true
                break
            }
        }

        // 3. 正向规则校验二：明确活动时间 (Positive Scheduled Time)
        val timeRangeMatcher = TIME_RANGE_PATTERN.matcher(fullText)
        val labeledTimeMatcher = LABELED_TIME_PATTERN.matcher(fullText)

        var startHour: Int? = null
        var startMin: Int? = null
        var endHour: Int? = null
        var endMin: Int? = null

        if (timeRangeMatcher.find()) {
            startHour = timeRangeMatcher.group(1)?.toIntOrNull()
            startMin = timeRangeMatcher.group(2)?.toIntOrNull()
            endHour = timeRangeMatcher.group(3)?.toIntOrNull()
            endMin = timeRangeMatcher.group(4)?.toIntOrNull()
        } else if (labeledTimeMatcher.find()) {
            startHour = labeledTimeMatcher.group(1)?.toIntOrNull()
            startMin = labeledTimeMatcher.group(2)?.toIntOrNull()
            endHour = if (startHour != null) (startHour + 1).coerceAtMost(23) else null
            endMin = startMin
        }

        val hasExplicitTime = startHour != null && startMin != null

        // 4. 正向判定逻辑 (Pure Positive Matching)：
        // 判定准则：必须同时具备【正向活动特征】与【明确的活动时间】；或者具备结构化议程【明确活动时间 + 明确具体地点 + 具体日期】
        // 绝不使用任何黑名单负向过滤，若不满足正向活动要素则自然判定为非日程邮件
        val location = extractLocation(fullText)
        val dateMatcher = FULL_DATE_PATTERN.matcher(fullText)
        val hasFullDate = dateMatcher.find()

        val isPositiveEvent = (matchedEventIndicator && hasExplicitTime) ||
                (hasExplicitTime && location != null && hasFullDate)

        if (!isPositiveEvent) {
            return emptyList()
        }

        // 5. 组装正向识别出的日程事件
        val cal = Calendar.getInstance()
        cal.timeInMillis = emailTimestamp

        dateMatcher.reset()
        if (dateMatcher.find()) {
            val year = dateMatcher.group(1)?.toIntOrNull()
            val month = dateMatcher.group(2)?.toIntOrNull()
            val day = dateMatcher.group(3)?.toIntOrNull()
            if (month != null && day != null) {
                if (year != null) cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month - 1)
                cal.set(Calendar.DAY_OF_MONTH, day)
            }
        }

        val finalStartHour = startHour ?: 14
        val finalStartMin = startMin ?: 0
        val finalEndHour = endHour ?: (finalStartHour + 1).coerceAtMost(23)
        val finalEndMin = endMin ?: finalStartMin

        cal.set(Calendar.HOUR_OF_DAY, finalStartHour)
        cal.set(Calendar.MINUTE, finalStartMin)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, finalEndHour)
        cal.set(Calendar.MINUTE, finalEndMin)
        val endTime = cal.timeInMillis

        val title = cleanTitle(subject)
        val finalLocation = location ?: "BNBU Campus"

        return listOf(
            ScheduleEvent(
                id = "evt_${UUID.randomUUID().toString().substring(0, 8)}",
                title = title,
                startTimestamp = startTime,
                endTimestamp = endTime,
                location = finalLocation,
                notes = "从邮件【$subject】智能识别提取",
                source = "email",
                color = "#EC4899"
            )
        )
    }

    /**
     * 剥离回复/转发引用的历史邮件内容（避免匹配到引用头部的 Date: 12:15 PM 等伪时间）
     */
    fun stripQuotedHistory(text: String): String {
        val markers = listOf(
            "------------------ Original",
            "--- Original Message ---",
            "-----Original Message-----",
            "------------------ 原始邮件 ------------------",
            "________________________________",
            "\nFrom: ",
            "\n发件人: "
        )
        var clean = text
        for (marker in markers) {
            val idx = clean.indexOf(marker, ignoreCase = true)
            if (idx != -1) {
                clean = clean.substring(0, idx)
            }
        }
        return clean.trim()
    }

    fun extractLocation(text: String): String? {
        for (pattern in LOCATION_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val loc = if (matcher.groupCount() >= 1) matcher.group(1) else matcher.group(0)
                if (!loc.isNullOrBlank()) {
                    return loc.trim()
                }
            }
        }
        return null
    }

    private fun cleanTitle(subject: String): String {
        return subject
            .replace(Regex("""^(Fwd|Re|回复|转发)[:：\s]+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""^\[BNBU\]\s*""", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}
