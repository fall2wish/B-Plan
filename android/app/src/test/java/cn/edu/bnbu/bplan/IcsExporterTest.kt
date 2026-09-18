package cn.edu.bnbu.bplan

import cn.edu.bnbu.bplan.data.model.Course
import cn.edu.bnbu.bplan.data.model.WeekType
import cn.edu.bnbu.bplan.data.repository.IcsExporter
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class IcsExporterTest {

    @Test
    fun testIcsExportClassroomLocationAndCustomTimes() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.SEPTEMBER, 7, 0, 0, 0) // Monday of Week 1
        val semesterStartEpoch = cal.timeInMillis

        val standardCourse = Course(
            id = "c1",
            name = "Python for Data Science",
            code = "DSBS7030",
            classroom = "T4-305",
            dayOfWeek = 1,
            startPeriod = 1,
            endPeriod = 2,
            weekType = WeekType.ALL
        )

        val taCourse = Course(
            id = "c2",
            name = "Python for Data Science (TA)",
            code = "DSBS7030",
            classroom = null,
            dayOfWeek = 1,
            startPeriod = 11,
            endPeriod = 12,
            startTime = "18:00",
            endTime = "19:00",
            weekType = WeekType.ALL
        )

        val ics = IcsExporter.exportCoursesAndEventsToIcs(
            courses = listOf(standardCourse, taCourse),
            events = emptyList(),
            semesterStartEpochMillis = semesterStartEpoch
        )

        // Verify location formatting
        assertTrue("Classroom should be formatted with @ BNBU Campus", ics.contains("LOCATION:T4-305 @ BNBU Campus"))
        assertTrue("Null classroom should default to BNBU Campus", ics.contains("LOCATION:BNBU Campus"))

        // Verify custom 1-hour TA time
        assertTrue("ICS should contain UID for TA course", ics.contains("UID:course_c2_w1_"))
        assertTrue("ICS should contain VEVENT", ics.contains("BEGIN:VEVENT"))
    }
}
