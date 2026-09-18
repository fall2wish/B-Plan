package cn.edu.bnbu.bplan

import cn.edu.bnbu.bplan.data.repository.EmailParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class EmailParserTest {

    @Test
    fun testHuaweiConsultationEmailIsNotRecognizedAsEvent() {
        val subject = "Re:关于“华为杯”研究生数学建模竞赛报名信息填写的咨询"
        val body = """
            1. 可填写“理工科技学院”
            2. 可填写“020208 【统计学】”

            ------------------ Original ------------------
            From: "Zhang San"<student@mail.bnbu.edu.cn>;
            Date: Thu, Sep 17, 2026 12:15 PM
            To: "teacher"<teacher@bnbu.edu.cn>;
            Subject: 关于“华为杯”研究生数学建模竞赛报名信息填写的咨询

            老师您好：
            请问报名华为杯数学建模时，培养单位和专业代码该如何选择？
        """.trimIndent()

        val events = EmailParser.parseEventsFromEmail(
            emailId = "test-1",
            subject = subject,
            body = body,
            emailTimestamp = 1789630000000L
        )

        assertTrue("华为杯咨询回复邮件不应被识别为日程活动", events.isEmpty())
    }

    @Test
    fun testLoginReminderEmailIsNotRecognizedAsEvent() {
        val subject = "企业微信邮箱登录提醒"
        val body = "你好，Zhang San 系统检测到你的邮箱账号通过应用登录。"

        val events = EmailParser.parseEventsFromEmail(
            emailId = "test-2",
            subject = subject,
            body = body,
            emailTimestamp = 1789630000000L
        )

        assertTrue("系统登录提醒邮件不应被识别为日程活动", events.isEmpty())
    }

    @Test
    fun testLectureEmailIsRecognizedAsEventWithCorrectLocationAndTime() {
        val subject = "提醒: 【思政课】Contemporary World and China 专题讲座"
        val body = """
            亲爱的同学们：
            大家好！本周思政大讲堂专题讲座安排如下：
            讲座时间：2026年9月18日 14:00 - 16:30
            讲座地点：大学会堂
            主讲嘉宾：李教授
            请各位同学按时到场并签到。
        """.trimIndent()

        val events = EmailParser.parseEventsFromEmail(
            emailId = "test-3",
            subject = subject,
            body = body,
            emailTimestamp = 1789630000000L
        )

        assertEquals(1, events.size)
        val event = events[0]
        assertEquals("大学会堂", event.location)

        val cal = Calendar.getInstance()
        cal.timeInMillis = event.startTimestamp
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(8, cal.get(Calendar.MONTH)) // 9月 (0-indexed = 8)
        assertEquals(18, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testStripQuotedHistory() {
        val original = """
            好的，收到。
            ------------------ Original ------------------
            From: someone
            Date: 12:15 PM
        """.trimIndent()

        val clean = EmailParser.stripQuotedHistory(original)
        assertEquals("好的，收到。", clean)
    }
}
