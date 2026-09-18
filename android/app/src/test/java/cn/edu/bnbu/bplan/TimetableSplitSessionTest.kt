package cn.edu.bnbu.bplan

import cn.edu.bnbu.bplan.data.model.Course
import cn.edu.bnbu.bplan.data.model.WeekType
import cn.edu.bnbu.bplan.data.repository.BPlanRepository
import cn.edu.bnbu.bplan.data.repository.GsasSyncManager
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableSplitSessionTest {

    @Test
    fun testParseTimetableJsonSplitsMorningAndAfternoonSessions() {
        val manager = GsasSyncManager()

        val root = JsonObject()
        val result = JsonObject()
        val myTimetable = JsonArray()

        // Periods 2, 3, 4 (morning) and Periods 7, 8, 9 (afternoon) on Friday
        val periods = listOf(2, 3, 4, 7, 8, 9)
        for (p in periods) {
            val row = JsonObject()
            row.addProperty("timeId", p + 7)
            val dataList = JsonArray()
            for (dayIdx in 0..6) {
                val dayArray = JsonArray()
                if (dayIdx == 4) { // Friday
                    val item = JsonObject()
                    item.addProperty("id", "course_dsbs7130")
                    item.addProperty("place", "T5-401")
                    item.addProperty("week", "odd")

                    val ttsSection = JsonObject()
                    ttsSection.addProperty("nameEn", "Survey Sampling and Experimental Design")
                    ttsSection.addProperty("teachers", "Prof. Shan LI")

                    val courseObj = JsonObject()
                    courseObj.addProperty("normal", "DSBS7130")
                    ttsSection.add("course", courseObj)

                    item.add("ttsSection", ttsSection)
                    dayArray.add(item)
                }
                dataList.add(dayArray)
            }
            row.add("dataList", dataList)
            myTimetable.add(row)
        }
        result.add("myTimetable", myTimetable)
        root.add("result", result)

        val courses = manager.parseTimetableJson(root)

        assertEquals("Should produce 2 courses for morning and afternoon", 2, courses.size)

        val morningCourse = courses.find { it.startPeriod == 2 }
        val afternoonCourse = courses.find { it.startPeriod == 7 }

        assertTrue("Morning course should exist", morningCourse != null)
        assertEquals(2, morningCourse!!.startPeriod)
        assertEquals(4, morningCourse.endPeriod)
        assertEquals("DSBS7130", morningCourse.code)
        assertEquals(5, morningCourse.dayOfWeek)
        assertEquals("T5-401", morningCourse.classroom)
        assertTrue(morningCourse.id.contains("p2"))

        assertTrue("Afternoon course should exist", afternoonCourse != null)
        assertEquals(7, afternoonCourse!!.startPeriod)
        assertEquals(9, afternoonCourse.endPeriod)
        assertEquals("DSBS7130", afternoonCourse.code)
        assertEquals(5, afternoonCourse.dayOfWeek)
        assertEquals("T5-401", afternoonCourse.classroom)
        assertTrue(afternoonCourse.id.contains("p7"))
    }

    @Test
    fun testSanitizeCoursesSplitsMergedCourseAcrossLunch() {
        val mergedCourse = Course(
            id = "dsbs7130_raw",
            name = "Survey Sampling and Experimental Design",
            code = "DSBS7130",
            classroom = "T5-401",
            teacher = "Prof. Shan LI",
            dayOfWeek = 5,
            startPeriod = 2,
            endPeriod = 9,
            startWeek = 1,
            endWeek = 16,
            weekType = WeekType.ODD,
            color = "#DC2626"
        )

        val sanitized = BPlanRepository.sanitizeCourses(listOf(mergedCourse))

        assertEquals(2, sanitized.size)
        assertEquals(2, sanitized[0].startPeriod)
        assertEquals(4, sanitized[0].endPeriod)
        assertTrue(sanitized[0].id.contains("_am"))

        assertEquals(7, sanitized[1].startPeriod)
        assertEquals(9, sanitized[1].endPeriod)
        assertTrue(sanitized[1].id.contains("_pm"))
    }

    @Test
    fun testSanitizeCoursesPurgesBogusTutorialsAndKeepsLegitimateOnes() {
        val courses = listOf(
            Course(id = "_tutorial", name = "Python (Tutorial)", dayOfWeek = 2, startPeriod = 11, endPeriod = 12),
            Course(id = "dsbs_tutorial", name = "Academic Integrity Online Tutorial (Tutorial)", dayOfWeek = 2, startPeriod = 12, endPeriod = 12),
            Course(id = "dsbs7040_tutorial", name = "Simulation Modelling (Tutorial)", dayOfWeek = 2, startPeriod = 12, endPeriod = 12),
            Course(id = "dsbs7300_tutorial", name = "Data Analytics with Programming (Tutorial)", dayOfWeek = 2, startPeriod = 12, endPeriod = 12),
            Course(id = "dsbs7130_tutorial", name = "Survey Sampling and Experimental Design (Tutorial)", dayOfWeek = 2, startPeriod = 12, endPeriod = 12),
            Course(id = "dsbs7030_tutorial", name = "Python for Data Science (Tutorial)", code = "DSBS7030", dayOfWeek = 2, startPeriod = 11, endPeriod = 12),
            Course(id = "dsbs7290_tutorial", name = "Regression Analysis (Tutorial)", code = "DSBS7290", dayOfWeek = 2, startPeriod = 12, endPeriod = 12)
        )

        val sanitized = BPlanRepository.sanitizeCourses(courses)

        assertEquals("Should only retain exactly 2 legitimate tutorials", 2, sanitized.size)
        assertTrue(sanitized.any { it.code == "DSBS7030" && it.id == "dsbs7030_tutorial" })
        assertTrue(sanitized.any { it.code == "DSBS7290" && it.id == "dsbs7290_tutorial" })
    }
}
