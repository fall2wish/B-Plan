"""
BNBU iSpace (Moodle Learning Management System) Client
教学平台客户端：https://ispace.bnbu.edu.cn
"""

import logging
import re
from typing import List, Optional, Dict, Any
from datetime import datetime
import httpx
from bs4 import BeautifulSoup
from icalendar import Calendar
from ..models import AssignmentDdl, Course, WeekType

logger = logging.getLogger(__name__)


class IspaceClient:
    """BNBU iSpace (Moodle) 教学平台客户端"""

    BASE_URL = "https://ispace.bnbu.edu.cn"

    def __init__(self, timeout: float = 15.0):
        self.timeout = timeout
        self.session = httpx.AsyncClient(
            base_url=self.BASE_URL,
            headers={
                "User-Agent": (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) "
                    "Chrome/128.0.0.0 Safari/537.36"
                ),
            },
            follow_redirects=True,
            timeout=self.timeout,
        )

    async def check_connection(self) -> bool:
        """检查 iSpace 联通性"""
        try:
            resp = await self.session.get("/")
            return resp.status_code == 200
        except Exception as e:
            logger.error(f"iSpace connection error: {e}")
            return False

    def set_session_cookie(self, session_cookie: str):
        """设置 MoodleSession Cookie"""
        self.session.cookies.set("MoodleSession", session_cookie, domain="ispace.bnbu.edu.cn")

    def parse_ical_feed(self, ical_text: str) -> List[AssignmentDdl]:
        """
        从 Moodle 日历导出的 iCalendar (.ics) 内容中解析作业 DDL
        Moodle 的 VEVENT 包含了作业由于截止时间 (due date) 产生的事件
        """
        ddls: List[AssignmentDdl] = []
        try:
            cal = Calendar.from_ical(ical_text)
            for component in cal.walk():
                if component.name == "VEVENT":
                    summary = str(component.get("summary", ""))
                    dtend = component.get("dtend") or component.get("dtstart")
                    due_date = dtend.dt if dtend else None
                    if isinstance(due_date, datetime):
                        # 处理时区或无时区
                        due_date = due_date.replace(tzinfo=None)
                    url = str(component.get("url", ""))
                    description = str(component.get("description", ""))
                    categories = component.get("categories")
                    if categories is not None:
                        if hasattr(categories, "cats"):
                            course_name = ", ".join(str(c) for c in categories.cats)
                        elif hasattr(categories, "to_ical"):
                            course_name = categories.to_ical().decode("utf-8")
                        elif isinstance(categories, list):
                            course_name = ", ".join(str(c) for c in categories)
                        else:
                            course_name = str(categories)
                    else:
                        course_name = "iSpace Course"

                    ddl = AssignmentDdl(
                        id=str(component.get("uid", f"ddl_{len(ddls)}")),
                        course_name=course_name,
                        title=summary,
                        due_date=due_date or datetime.now(),
                        url=url,
                        description=description,
                    )
                    ddls.append(ddl)
        except Exception as e:
            logger.error(f"Failed to parse iCal feed: {e}")
        return ddls

    def parse_action_events_payload(self, data: Any) -> List[AssignmentDdl]:
        """
        解析 Moodle core_calendar_get_action_events_by_timesort 返回的作业与待办事件
        """
        ddls: List[AssignmentDdl] = []
        try:
            items = data
            if isinstance(data, list) and len(data) > 0:
                items = data[0].get("data", {})
            if isinstance(items, dict):
                events = items.get("events", [])
            else:
                events = []

            for ev in events:
                timesort = ev.get("timesort") or ev.get("timestart")
                due_date = datetime.fromtimestamp(timesort) if timesort else datetime.now()
                course_info = ev.get("course") or {}
                course_name = course_info.get("fullname") or course_info.get("shortname") or "iSpace Course"
                name = ev.get("activityname") or ev.get("name") or "Assignment"
                url = ev.get("url") or (ev.get("action") or {}).get("url")
                desc = ev.get("description") or ""

                ddl = AssignmentDdl(
                    id=str(ev.get("id")),
                    course_name=course_name,
                    title=name,
                    due_date=due_date,
                    url=url,
                    description=desc,
                )
                ddls.append(ddl)
        except Exception as e:
            logger.error(f"Failed to parse action events payload: {e}")
        return ddls

    async def fetch_timeline_assignments(self, sesskey: str, limit: int = 50) -> List[AssignmentDdl]:
        """通过 Moodle AJAX 接口获取近期待办作业与 DDL"""
        url = f"/lib/ajax/service.php?sesskey={sesskey}&info=core_calendar_get_action_events_by_timesort"
        payload = [{
            "index": 0,
            "methodname": "core_calendar_get_action_events_by_timesort",
            "args": {
                "limitnum": limit,
                "timesortfrom": int(datetime.now().timestamp()) - 86400 * 7
            }
        }]
        resp = await self.session.post(url, json=payload)
        resp.raise_for_status()
        data = resp.json()
        return self.parse_action_events_payload(data)

    def parse_course_tutorial(
        self,
        course_text: str,
        course_name: str,
        course_code: str,
        gsas_teacher: Optional[str] = "Prof. Xiaoling PENG"
    ) -> Optional[Course]:
        """从 iSpace 课程介绍/公告栏提取 Tutorial 辅导课信息并构建 Course 对象 (授课教师来自 GSAS，助教来自 iSpace)"""
        tut_pattern = (
            r"Tutorial[\s\S]*?(?:Time|时间)[:：]\s*([A-Za-z]+|周[一二三四五六日天])\s*"
            r"(\d{1,2}:\d{2})\s*[-~至到]\s*(\d{1,2}:\d{2})(?:\s*(?:From|从)?\s*Week\s*(\d+))?"
            r"[\s\S]*?(?:Location|地点|教室)[:：]\s*([A-Za-z0-9-]+)"
        )
        match = re.search(tut_pattern, course_text, re.IGNORECASE)
        if not match:
            return None

        day_str, start_time, end_time, week_str, location = match.groups()
        day_map = {
            "monday": 1, "tuesday": 2, "wednesday": 3, "thursday": 4, "friday": 5, "saturday": 6, "sunday": 7,
            "周一": 1, "周二": 2, "周三": 3, "周四": 4, "周五": 5, "周六": 6, "周日": 7, "周天": 7
        }
        day_of_week = day_map.get(day_str.lower(), 2)
        start_week = int(week_str) if week_str else 3

        sh = int(start_time.split(":")[0])
        eh = int(end_time.split(":")[0])
        start_period = max(1, sh - 7)
        end_period = max(start_period, eh - 7)

        ta_match = re.search(r"Teaching Assistant Information[\s\S]*?Mr\.\s*([A-Za-z\s]+)", course_text)
        assistant = f"Mr. {ta_match.group(1).strip()} (助教)" if ta_match else "Mr. Shengyang Xin / Mr. Qianzi Cao (助教)"

        return Course(
            id=f"{course_code.lower()}_tutorial",
            name=f"{course_name} (Tutorial)",
            code=course_code,
            classroom=location.strip(),
            teacher=gsas_teacher,
            assistant=assistant,
            day_of_week=day_of_week,
            start_period=start_period,
            end_period=end_period,
            start_week=start_week,
            end_week=16,
            week_type=WeekType.ALL,
            color="#0284C7"
        )

    async def close(self):
        await self.session.aclose()

