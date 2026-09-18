"""
Unit tests for Email Schedule Parser
"""

from datetime import datetime
from services.mail.parser import EmailScheduleParser


def test_extract_location():
    text1 = "Please attend the seminar in T1-101 this afternoon."
    assert EmailScheduleParser.extract_location(text1) == "T1-101"

    text2 = "Meeting at Room CC302 next week."
    assert EmailScheduleParser.extract_location(text2) == "CC302"

    text3 = "活动地点：大学会堂演艺厅"
    assert "大学会堂" in (EmailScheduleParser.extract_location(text3) or "")


def test_parse_from_text():
    subject = "【讲座通知】AI 与未来生活"
    body = "时间：2026-10-15 14:30\n地点：T29-301\n请准时参加。"
    event = EmailScheduleParser.parse_from_text(subject, body)
    assert event is not None
    assert event.title == subject
    assert event.location == "T29-301"
    assert event.start_time.year == 2026
    assert event.start_time.month == 10
    assert event.start_time.day == 15
    assert event.start_time.hour == 14
    assert event.start_time.minute == 30
