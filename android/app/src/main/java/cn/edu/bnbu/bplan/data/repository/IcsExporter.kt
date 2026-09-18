package cn.edu.bnbu.bplan.data.repository

import cn.edu.bnbu.bplan.data.model.Course
import cn.edu.bnbu.bplan.data.model.ScheduleEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object IcsExporter {

    private val UTC_FORMAT = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun exportCoursesAndEventsToIcs(
        courses: List<Course>,
        events: List<ScheduleEvent>,
        semesterStartEpochMillis: Long
    ): String {
        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//BNBU B-Plan//Campus Schedule Assistant//EN\r\n")
        sb.append("CALSCALE:GREGORIAN\r\n")
        sb.append("METHOD:PUBLISH\r\n")
        sb.append("X-WR-CALNAME:BNBU B-Plan Schedule\r\n")
        sb.append("X-WR-TIMEZONE:Asia/Shanghai\r\n")

        val nowStr = UTC_FORMAT.format(Date())

        // 1. Export Courses (for 16 weeks)
        val semesterCal = Calendar.getInstance()
        semesterCal.timeInMillis = semesterStartEpochMillis
        // Align to Monday of Week 1
        semesterCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        semesterCal.set(Calendar.HOUR_OF_DAY, 0)
        semesterCal.set(Calendar.MINUTE, 0)
        semesterCal.set(Calendar.SECOND, 0)
        semesterCal.set(Calendar.MILLISECOND, 0)

        for (week in 1..16) {
            for (course in courses) {
                if (!course.isActiveInWeek(week)) continue

                val courseCal = Calendar.getInstance()
                courseCal.timeInMillis = semesterCal.timeInMillis
                courseCal.add(Calendar.WEEK_OF_YEAR, week - 1)
                // dayOfWeek: 1 (Mon) -> Monday
                val calDay = when (course.dayOfWeek) {
                    1 -> Calendar.MONDAY
                    2 -> Calendar.TUESDAY
                    3 -> Calendar.WEDNESDAY
                    4 -> Calendar.THURSDAY
                    5 -> Calendar.FRIDAY
                    6 -> Calendar.SATURDAY
                    7 -> Calendar.SUNDAY
                    else -> Calendar.MONDAY
                }
                courseCal.set(Calendar.DAY_OF_WEEK, calDay)

                // Period 1 is 08:00, Period 2 is 09:00, etc.
                val startParts = parseTime(course.startTime)
                val endParts = parseTime(course.endTime)

                val dtStart: String
                val dtEnd: String

                if (startParts != null && endParts != null) {
                    courseCal.set(Calendar.HOUR_OF_DAY, startParts.first)
                    courseCal.set(Calendar.MINUTE, startParts.second)
                    courseCal.set(Calendar.SECOND, 0)
                    dtStart = UTC_FORMAT.format(courseCal.time)

                    courseCal.set(Calendar.HOUR_OF_DAY, endParts.first)
                    courseCal.set(Calendar.MINUTE, endParts.second)
                    courseCal.set(Calendar.SECOND, 0)
                    dtEnd = UTC_FORMAT.format(courseCal.time)
                } else {
                    val startHour = 7 + course.startPeriod
                    val endHour = 7 + course.endPeriod
                    courseCal.set(Calendar.HOUR_OF_DAY, startHour)
                    courseCal.set(Calendar.MINUTE, 0)
                    courseCal.set(Calendar.SECOND, 0)
                    dtStart = UTC_FORMAT.format(courseCal.time)

                    courseCal.set(Calendar.HOUR_OF_DAY, endHour)
                    courseCal.set(Calendar.MINUTE, 50)
                    courseCal.set(Calendar.SECOND, 0)
                    dtEnd = UTC_FORMAT.format(courseCal.time)
                }

                val location = if (!course.classroom.isNullOrBlank()) {
                    "${course.classroom} @ BNBU Campus"
                } else {
                    "BNBU Campus"
                }

                sb.append("BEGIN:VEVENT\r\n")
                sb.append("UID:course_${course.id}_w${week}_${nowStr}@bnbu.bplan\r\n")
                sb.append("DTSTAMP:$nowStr\r\n")
                sb.append("DTSTART:$dtStart\r\n")
                sb.append("DTEND:$dtEnd\r\n")
                sb.append("SUMMARY:${course.name} (${course.code ?: ""})\r\n")
                sb.append("LOCATION:$location\r\n")
                sb.append("DESCRIPTION:教师: ${course.teacher ?: "N/A"} | 第 ${week} 周 (周次: ${course.weekType})\r\n")
                sb.append("END:VEVENT\r\n")
            }
        }

        // 2. Export Independent Schedule Events
        for (event in events) {
            val dtStart = UTC_FORMAT.format(Date(event.startTimestamp))
            val dtEnd = UTC_FORMAT.format(Date(event.endTimestamp))

            sb.append("BEGIN:VEVENT\r\n")
            sb.append("UID:event_${event.id}@bnbu.bplan\r\n")
            sb.append("DTSTAMP:$nowStr\r\n")
            sb.append("DTSTART:$dtStart\r\n")
            sb.append("DTEND:$dtEnd\r\n")
            sb.append("SUMMARY:${event.title}\r\n")
            if (!event.location.isNullOrBlank()) {
                sb.append("LOCATION:${event.location}\r\n")
            }
            if (!event.notes.isNullOrBlank()) {
                sb.append("DESCRIPTION:${event.notes}\r\n")
            }
            sb.append("END:VEVENT\r\n")
        }

        sb.append("END:VCALENDAR\r\n")
        return sb.toString()
    }

    private fun parseTime(timeStr: String?): Pair<Int, Int>? {
        if (timeStr.isNullOrBlank()) return null
        val parts = timeStr.trim().split(":")
        if (parts.size == 2) {
            val hour = parts[0].trim().toIntOrNull() ?: return null
            val minute = parts[1].trim().toIntOrNull() ?: return null
            return Pair(hour, minute)
        }
        return null
    }
}
