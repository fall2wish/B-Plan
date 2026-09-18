"""
Smart Email to Schedule Parser
用于从校园邮件正文与附件中智能提取活动时间、地点与待办事项
"""

import re
from datetime import datetime, timedelta
from typing import List, Optional
from icalendar import Calendar
from ..models import ScheduleEvent


class EmailScheduleParser:
    """邮件日程智能提取器"""

    # 常见教室/地点正则 (针对 BNBU 校园常见地点如 T1-101, T29-301, CC302, LRC, V23 等)
    LOCATION_PATTERNS = [
        re.compile(r"\b([TV]\d{1,2}[-–—\s]?\d{3,4})\b", re.IGNORECASE),
        re.compile(r"\b(CC[-–—\s]?\d{3})\b", re.IGNORECASE),
        re.compile(r"(教学楼|行政楼|演艺厅|学生活动中心|体育馆|大学会堂|LRC|图书馆|Room\s*[A-Za-z0-9-]+)", re.IGNORECASE),
    ]

    # 日期时间匹配正则 (支持 "9月18日 14:30", "2026-09-20 10:00", "This Friday at 3pm" 等)
    DATETIME_PATTERNS = [
        re.compile(r"(?:(\d{4})[年\-\/\.])?\s*(\d{1,2})[月\-\/\.]\s*(\d{1,2})[日号]?\s*(?:[上下]午)?\s*(\d{1,2})[:：](\d{2})"),
        re.compile(r"(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})"),
    ]

    @classmethod
    def extract_location(cls, text: str) -> Optional[str]:
        """从正文中提取地点"""
        for pattern in cls.LOCATION_PATTERNS:
            match = pattern.search(text)
            if match:
                return match.group(0).strip()
        return None

    @classmethod
    def extract_datetime(cls, text: str) -> Optional[datetime]:
        """从正文中提取开始时间"""
        now = datetime.now()
        for pattern in cls.DATETIME_PATTERNS:
            match = pattern.search(text)
            if match:
                groups = match.groups()
                try:
                    if len(groups) == 5:
                        year = int(groups[0]) if groups[0] else now.year
                        month = int(groups[1])
                        day = int(groups[2])
                        hour = int(groups[3])
                        minute = int(groups[4])
                        return datetime(year, month, day, hour, minute)
                    elif len(groups) == 2:
                        return datetime.strptime(f"{groups[0]} {groups[1]}", "%Y-%m-%d %H:%M")
                except ValueError:
                    continue
        return None

    @classmethod
    def parse_from_text(cls, subject: str, body: str) -> Optional[ScheduleEvent]:
        """从邮件文本中解析建议的日程事件"""
        full_text = f"{subject}\n{body}"
        dt = cls.extract_datetime(full_text)
        if not dt:
            return None

        loc = cls.extract_location(full_text)
        # 默认时长 1 小时
        end_dt = dt + timedelta(hours=1)

        return ScheduleEvent(
            id=f"mail_evt_{int(dt.timestamp())}",
            title=subject.strip() or "校园邮件提醒",
            start_time=dt,
            end_time=end_dt,
            location=loc,
            notes=body[:200].strip(),
            source="email",
        )

    @classmethod
    def parse_from_ics(cls, ics_content: str) -> List[ScheduleEvent]:
        """从邮件附件 .ics 中解析日程"""
        events: List[ScheduleEvent] = []
        try:
            cal = Calendar.from_ical(ics_content)
            for component in cal.walk():
                if component.name == "VEVENT":
                    summary = str(component.get("summary", "日程事件"))
                    dtstart = component.get("dtstart")
                    dtend = component.get("dtend")
                    loc = str(component.get("location", "")) or None
                    desc = str(component.get("description", "")) or None

                    start = dtstart.dt if dtstart else datetime.now()
                    end = dtend.dt if dtend else (start + timedelta(hours=1))

                    if isinstance(start, datetime):
                        start = start.replace(tzinfo=None)
                    if isinstance(end, datetime):
                        end = end.replace(tzinfo=None)

                    events.append(
                        ScheduleEvent(
                            id=str(component.get("uid", f"ics_evt_{len(events)}")),
                            title=summary,
                            start_time=start,
                            end_time=end,
                            location=loc,
                            notes=desc,
                            source="email_ics",
                        )
                    )
        except Exception:
            pass
        return events
