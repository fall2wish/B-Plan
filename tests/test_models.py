"""
Unit tests for B-Plan Core Models
"""

from datetime import datetime, timedelta
from services.models import Course, WeekType, ScheduleEvent, AssignmentDdl


def test_course_week_filtering():
    # 奇数周课程
    odd_course = Course(
        id="c1",
        name="Operating Systems",
        day_of_week=1,
        start_period=1,
        end_period=2,
        start_week=1,
        end_week=16,
        week_type=WeekType.ODD,
    )
    assert odd_course.is_active_in_week(1) is True
    assert odd_course.is_active_in_week(2) is False
    assert odd_course.is_active_in_week(3) is True

    # 偶数周课程
    even_course = Course(
        id="c2",
        name="Database Systems",
        day_of_week=2,
        start_period=3,
        end_period=4,
        start_week=1,
        end_week=16,
        week_type=WeekType.EVEN,
    )
    assert even_course.is_active_in_week(1) is False
    assert even_course.is_active_in_week(2) is True

    # 自定义指定周课程
    custom_course = Course(
        id="c3",
        name="Campus Lecture",
        day_of_week=3,
        start_period=5,
        end_period=6,
        start_week=1,
        end_week=16,
        week_type=WeekType.CUSTOM,
        active_weeks=[2, 4, 6, 8],
    )
    assert custom_course.is_active_in_week(2) is True
    assert custom_course.is_active_in_week(3) is False


def test_assignment_ddl_remaining_hours():
    future_due = datetime.now() + timedelta(hours=10)
    ddl = AssignmentDdl(
        id="d1",
        course_name="COMP3033",
        title="Lab 1",
        due_date=future_due,
    )
    assert ddl.is_expired is False
    assert 9.9 < ddl.remaining_hours <= 10.0
