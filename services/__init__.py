"""
B-Plan Services Package
"""

from .models import (
    Course,
    TimeSlot,
    WeekType,
    ScheduleEvent,
    AssignmentDdl,
    EmailMessage,
)

__all__ = [
    "Course",
    "TimeSlot",
    "WeekType",
    "ScheduleEvent",
    "AssignmentDdl",
    "EmailMessage",
]
