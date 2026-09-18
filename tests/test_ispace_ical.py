"""
Unit tests for iSpace iCal feed parser
"""

from services.ispace.client import IspaceClient

SAMPLE_ICAL = """BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Moodle Pty Ltd//NONSGML Moodle Version 2022041900//EN
CALSCALE:GREGORIAN
METHOD:PUBLISH
BEGIN:VEVENT
UID:2984928@ispace.bnbu.edu.cn
SUMMARY:Assignment 1 is due
DESCRIPTION:Please submit your assignment before deadline.
CATEGORIES:COMP3033 Operating Systems
DTSTART:20261020T155900Z
DTEND:20261020T155900Z
URL:https://ispace.bnbu.edu.cn/mod/assign/view.php?id=12345
END:VEVENT
END:VCALENDAR"""


def test_parse_ical_feed():
    client = IspaceClient()
    ddls = client.parse_ical_feed(SAMPLE_ICAL)
    assert len(ddls) == 1
    ddl = ddls[0]
    assert ddl.title == "Assignment 1 is due"
    assert "COMP3033" in ddl.course_name
    assert ddl.url == "https://ispace.bnbu.edu.cn/mod/assign/view.php?id=12345"
    assert ddl.due_date.year == 2026
    assert ddl.due_date.month == 10
    assert ddl.due_date.day == 20
