package cn.edu.bnbu.bplan

import cn.edu.bnbu.bplan.data.model.AcademicCalendar
import cn.edu.bnbu.bplan.data.model.StudentType
import cn.edu.bnbu.bplan.data.repository.BPlanRepository
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AcademicCalendarTest {

    private val gson = Gson()

    private fun loadCalendarFromJson(): AcademicCalendar {
        // Test loads the calendar json from calendars/ or app/src/main/assets/
        val assetFile = File("src/main/assets/calendar/academic_calendar_2026_s1.json")
        val json = if (assetFile.exists()) {
            assetFile.readText()
        } else {
            val rootFile = File("../../calendars/academic_calendar_2026_s1.json")
            if (rootFile.exists()) rootFile.readText() else ""
        }
        assertFalse("Calendar JSON should not be empty", json.isBlank())
        return gson.fromJson(json, AcademicCalendar::class.java)
    }

    private fun loadPgCalendarFromJson(): AcademicCalendar {
        val assetFile = File("src/main/assets/calendar/academic_calendar_pg_2026_s1.json")
        val json = if (assetFile.exists()) {
            assetFile.readText()
        } else {
            val rootFile = File("../../calendars/academic_calendar_pg_2026_s1.json")
            if (rootFile.exists()) rootFile.readText() else ""
        }
        assertFalse("PG Calendar JSON should not be empty", json.isBlank())
        return gson.fromJson(json, AcademicCalendar::class.java)
    }

    @Test
    fun testCalendarStructureAndIntegrity() {
        val calendar = loadCalendarFromJson()
        assertEquals("2026-2027-1", calendar.semesterId)
        assertEquals("2026-08-31", calendar.startDate)
        assertEquals("2027-01-11", calendar.endDate)
        assertEquals(16, calendar.totalWeeks)
        assertTrue(calendar.makeupDays.isNotEmpty())
        assertTrue(calendar.holidays.isNotEmpty())
        assertTrue(calendar.events.isNotEmpty())
    }

    @Test
    fun testMakeupDaysMapping() {
        val calendar = loadCalendarFromJson()

        // 9/20 Sunday makes up for Friday (Week 4 / 25 Sep)
        val sep20Makeup = calendar.makeupDays.find { it.date == "2026-09-20" }
        assertNotNull("2026-09-20 should be a makeup day", sep20Makeup)
        assertEquals(7, sep20Makeup?.sourceDayOfWeek) // Sunday
        assertEquals(5, sep20Makeup?.targetDayOfWeek)   // Friday
        assertEquals("周五", sep20Makeup?.targetDayName)
        assertEquals("2026-09-25", sep20Makeup?.targetDate)
        assertEquals(4, sep20Makeup?.targetWeek)

        // 10/10 Saturday makes up for Monday (Week 6 / 5 Oct)
        val oct10Makeup = calendar.makeupDays.find { it.date == "2026-10-10" }
        assertNotNull("2026-10-10 should be a makeup day", oct10Makeup)
        assertEquals(6, oct10Makeup?.sourceDayOfWeek) // Saturday
        assertEquals(1, oct10Makeup?.targetDayOfWeek)   // Monday
        assertEquals("周一", oct10Makeup?.targetDayName)
        assertEquals("2026-10-05", oct10Makeup?.targetDate)
        assertEquals(6, oct10Makeup?.targetWeek)
    }

    @Test
    fun testHolidaysSuspended() {
        val calendar = loadCalendarFromJson()

        // National Day holiday
        val nationalDay = calendar.holidays.find { it.id == "national-day" }
        assertNotNull(nationalDay)
        assertEquals("2026-10-01", nationalDay?.startDate)
        assertEquals("2026-10-07", nationalDay?.endDate)
        assertTrue(nationalDay?.isClassSuspended == true)

        // Mid Autumn
        val midAutumn = calendar.holidays.find { it.id == "mid-autumn" }
        assertNotNull(midAutumn)
        assertEquals("2026-09-25", midAutumn?.startDate)
        assertTrue(midAutumn?.isClassSuspended == true)
    }

    @Test
    fun testReadingWeekUgVsPgSuspended() {
        val calendar = loadCalendarFromJson()

        val readingWeek = calendar.holidays.find { it.id == "reading-week" }
        assertNotNull("Reading week holiday must exist", readingWeek)
        assertEquals("2026-10-19", readingWeek?.startDate)
        assertEquals("2026-10-25", readingWeek?.endDate)

        // Reading week must specify targetStudentType == UG, ugSuspended == true, pgSuspended == false
        assertEquals("UG", readingWeek?.targetStudentType)
        assertTrue(readingWeek?.ugSuspended == true)
        assertFalse(readingWeek?.pgSuspended == true)

        // For Undergraduate (UG), reading week suspends classes
        assertTrue(readingWeek!!.isSuspendedFor(StudentType.UNDERGRADUATE))

        // For Postgraduate (PG), reading week DOES NOT suspend classes (研究生不放假)
        assertFalse(readingWeek.isSuspendedFor(StudentType.POSTGRADUATE))
    }

    @Test
    fun testNationalDayAllStudentsSuspended() {
        val calendar = loadCalendarFromJson()
        val nationalDay = calendar.holidays.find { it.id == "national-day" }
        assertNotNull(nationalDay)

        // National Day suspends for both UG and PG
        assertTrue(nationalDay!!.isSuspendedFor(StudentType.UNDERGRADUATE))
        assertTrue(nationalDay.isSuspendedFor(StudentType.POSTGRADUATE))
    }

    @Test
    fun testStudentTypeInference() {
        // Master programme -> Postgraduate
        assertEquals(
            StudentType.POSTGRADUATE,
            BPlanRepository.inferStudentType(
                programme = "Master of Science in Data Science"
            )
        )

        // Bachelor programme -> Undergraduate
        assertEquals(
            StudentType.UNDERGRADUATE,
            BPlanRepository.inferStudentType(
                programme = "Bachelor of Science (Hons) in Computer Science and Technology"
            )
        )

        // Postgraduate student ID prefix (p, r, t, 212...)
        assertEquals(
            StudentType.POSTGRADUATE,
            BPlanRepository.inferStudentType(studentId = "2123456789")
        )
        assertEquals(
            StudentType.UNDERGRADUATE,
            BPlanRepository.inferStudentType(studentId = "s123456789")
        )

        // Course code 7xxx -> Postgraduate
        assertEquals(
            StudentType.POSTGRADUATE,
            BPlanRepository.inferStudentType(courseCodes = listOf("DSBS7030"))
        )
        assertEquals(
            StudentType.UNDERGRADUATE,
            BPlanRepository.inferStudentType(courseCodes = listOf("COMP2013"))
        )
    }

    @Test
    fun testCalendarOriginalImageConfiguration() {
        val calendar = loadCalendarFromJson()
        assertTrue(calendar.originalImageUrl.isNotBlank())
        assertTrue(calendar.originalImageUrl.endsWith(".png"))
        assertEquals("calendar/academic_calendar_2026_s1.png", calendar.originalImageAsset)
    }

    @Test
    fun testEffectiveDayOfWeekCalculation() {
        val calendar = loadCalendarFromJson()

        fun getEffectiveDay(dateStr: String, naturalDayOfWeek: Int): Int {
            val makeup = calendar.makeupDays.find { it.date == dateStr }
            return makeup?.targetDayOfWeek ?: naturalDayOfWeek
        }

        // On 2026-09-20 (Sunday=7), effective day must be 5 (Friday)
        assertEquals(5, getEffectiveDay("2026-09-20", 7))

        // On 2026-09-21 (Monday=1), effective day remains 1 (Monday)
        assertEquals(1, getEffectiveDay("2026-09-21", 1))

        // On 2026-10-10 (Saturday=6), effective day must be 1 (Monday)
        assertEquals(1, getEffectiveDay("2026-10-10", 6))
    }

    @Test
    fun testEffectiveWeekNumberAndCourseFiltering() {
        val calendar = loadCalendarFromJson()

        fun getEffectiveWeek(dateStr: String, naturalWeek: Int): Int {
            val makeup = calendar.makeupDays.find { it.date == dateStr }
            return makeup?.targetWeek ?: naturalWeek
        }

        // On 2026-09-20 (natural week 3), makeup is for Week 4 (even/double week, 25 Sep)
        assertEquals(4, getEffectiveWeek("2026-09-20", 3))

        // On regular days, natural week is preserved
        assertEquals(3, getEffectiveWeek("2026-09-18", 3))
        assertEquals(4, getEffectiveWeek("2026-09-21", 4))

        // On 2026-10-10 (natural week 6), makeup is for Week 6 Monday (5 Oct)
        assertEquals(6, getEffectiveWeek("2026-10-10", 6))

        // Verify Course.isActiveInWeek behavior with effective week:
        // A Friday course active only on EVEN weeks (e.g. Week 2, 4, 6, 8)
        val evenWeekFridayCourse = cn.edu.bnbu.bplan.data.model.Course(
            id = "c_even_fri",
            name = "Advanced Statistics",
            dayOfWeek = 5,
            weekType = cn.edu.bnbu.bplan.data.model.WeekType.EVEN,
            startPeriod = 2,
            endPeriod = 4
        )

        // A Friday course active only on ODD weeks (e.g. Week 1, 3, 5, 7)
        val oddWeekFridayCourse = cn.edu.bnbu.bplan.data.model.Course(
            id = "c_odd_fri",
            name = "Computer Science Seminar",
            dayOfWeek = 5,
            weekType = cn.edu.bnbu.bplan.data.model.WeekType.ODD,
            startPeriod = 2,
            endPeriod = 4
        )

        val effectiveWeekSep20 = getEffectiveWeek("2026-09-20", 3) // 4
        // The even-week course MUST be active on 9/20 because it makes up for Week 4 (双数周)
        assertTrue("Even week course should be active on 9/20 makeup day (Week 4)", evenWeekFridayCourse.isActiveInWeek(effectiveWeekSep20))
        // The odd-week course MUST NOT be active on 9/20
        org.junit.Assert.assertFalse("Odd week course should not be active on 9/20 makeup day", oddWeekFridayCourse.isActiveInWeek(effectiveWeekSep20))
    }

    @Test
    fun testPgCalendarStructureAndIntegrity() {
        val calendar = loadPgCalendarFromJson()
        assertEquals("2026-2027-1-PG", calendar.semesterId)
        assertEquals("2026-09-01", calendar.startDate)
        assertEquals("2026-12-13", calendar.endDate)
        assertEquals(14, calendar.totalWeeks)
        // Postgraduate schedule has 0 makeup days (no 9/20 or 10/10 makeups)
        assertTrue("PG calendar must have zero makeup days", calendar.makeupDays.isEmpty())
        assertTrue("PG calendar must contain holidays", calendar.holidays.isNotEmpty())

        // Original image asset
        assertEquals("calendar/academic_calendar_pg_2026_s1.png", calendar.originalImageAsset)

        // National Day holiday is present and suspends classes for PG
        val nationalDay = calendar.holidays.find { it.id == "national-day" }
        assertNotNull(nationalDay)
        assertEquals("2026-10-01", nationalDay?.startDate)
        assertEquals("2026-10-07", nationalDay?.endDate)
        assertTrue(nationalDay?.isClassSuspended == true)

        // Reading Week is not present for PG
        val readingWeek = calendar.holidays.find { it.id == "reading-week" }
        assertNull("PG calendar has no Reading Week holiday", readingWeek)
    }

    @Test
    fun testPgWeekCalculationsAndWeek5Split() {
        fun calculatePgWeek(dateStr: String): Int {
            val date = dateStr.trim()
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

        // Week 1: 9.1 - 9.6
        assertEquals(1, calculatePgWeek("2026-09-01"))
        assertEquals(1, calculatePgWeek("2026-09-06"))

        // Week 3: 9.14 - 9.20 (9/20 is normal Sunday for PG, NOT Week 4 Friday makeup)
        assertEquals(3, calculatePgWeek("2026-09-20"))

        // Week 4: 9.21 - 9.27
        assertEquals(4, calculatePgWeek("2026-09-21"))
        assertEquals(4, calculatePgWeek("2026-09-27"))

        // Week 5 split across National Day:
        // Pre-holiday portion: 9.28 (Mon), 9.29 (Tue), 9.30 (Wed)
        assertEquals(5, calculatePgWeek("2026-09-28"))
        assertEquals(5, calculatePgWeek("2026-09-30"))
        // National Day holiday period (10.1 - 10.7) still falls into Week 5 block
        assertEquals(5, calculatePgWeek("2026-10-01"))
        assertEquals(5, calculatePgWeek("2026-10-07"))
        // Post-holiday portion: 10.8 (Thu), 10.9 (Fri), 10.10 (Sat), 10.11 (Sun)
        assertEquals(5, calculatePgWeek("2026-10-08"))
        assertEquals(5, calculatePgWeek("2026-10-10"))
        assertEquals(5, calculatePgWeek("2026-10-11"))

        // Week 6 resumes normally on 10.12 (Mon)
        assertEquals(6, calculatePgWeek("2026-10-12"))
        assertEquals(6, calculatePgWeek("2026-10-18"))

        // Week 7: 10.19 - 10.25 (When UG has Reading Week, PG continues classes normally!)
        assertEquals(7, calculatePgWeek("2026-10-19"))
        assertEquals(7, calculatePgWeek("2026-10-25"))

        // Final Week 14: 12.7 - 12.13
        assertEquals(14, calculatePgWeek("2026-12-07"))
        assertEquals(14, calculatePgWeek("2026-12-13"))
    }
}
