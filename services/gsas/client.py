"""
BNBU GSAS (Graduate & Student Academic System) Client
教务系统客户端：https://gsas.bnbu.edu.cn
"""

import logging
from typing import List, Optional, Dict, Any
import httpx
from ..models import Course

logger = logging.getLogger(__name__)


class GsasClient:
    """BNBU 教务系统接口与抓取客户端"""

    BASE_URL = "https://gsas.bnbu.edu.cn"

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
                "Accept": "application/json, text/plain, */*",
                "Referer": f"{self.BASE_URL}/",
            },
            follow_redirects=True,
            timeout=self.timeout,
        )

    async def check_connection(self) -> bool:
        """检查教务系统联通性"""
        try:
            resp = await self.session.get("/")
            return resp.status_code == 200
        except Exception as e:
            logger.error(f"GSAS connection error: {e}")
            return False

    async def login_with_credentials(self, username: str, password: str) -> Dict[str, Any]:
        """
        通过账号密码登录教务系统
        注：若教务系统前端采用了 SPA + 动态 token 或图形验证码，
        可切换至 WebView 捕获的 Cookie/Token 会话导入。
        """
        raise NotImplementedError("真实登录逻辑待逆向抓包或由 WebView 注入 Session")

    def set_access_token(self, token: str):
        """设置 GSAS Bearer Token"""
        if not token.startswith("Bearer "):
            token = f"Bearer {token}"
        self.session.headers["pro__Access-Token"] = token
        self.session.headers["Access-Token"] = token
        self.session.headers["Authorization"] = token

    def set_session_cookie(self, cookies: Dict[str, str]):
        """导入通过 WebView 或浏览器抓包获取的 Cookie"""
        self.session.cookies.update(cookies)

    def parse_timetable_payload(self, data: Dict[str, Any]) -> List[Course]:
        """
        解析 GSAS /student/es/myTimetable 返回的 JSON 报文并构造统一 Course 列表
        """
        result = data.get("result") or data
        my_timetable = result.get("myTimetable") or []
        courses: List[Course] = []
        active_courses: Dict[str, Course] = {}

        # 遍历 15 个时间节次数组 (timeId: 8 ~ 22)，按时间升序处理
        sorted_timetable = sorted(my_timetable, key=lambda r: r.get("timeId", 8))
        for row in sorted_timetable:
            time_id = row.get("timeId", 8)
            period = time_id - 7  # timeId 8 -> period 1, timeId 9 -> period 2, etc.
            data_list = row.get("dataList", [])

            # dataList 包含 7 天 (索引 0: 周一, 6: 周日)
            for day_idx, day_courses in enumerate(data_list):
                day_of_week = day_idx + 1  # 1: Monday, 7: Sunday
                if not isinstance(day_courses, list):
                    continue

                for item in day_courses:
                    if not isinstance(item, dict):
                        continue
                    tts_section = item.get("ttsSection") or {}
                    course_info = tts_section.get("course") or {}
                    name = tts_section.get("nameEn") or item.get("name") or "Unknown Course"
                    code = course_info.get("normal") or course_info.get("full")
                    classroom = item.get("place")
                    teacher = tts_section.get("teachers")
                    week_str = str(item.get("week", "All")).lower()

                    from ..models import WeekType
                    if "odd" in week_str:
                        week_type = WeekType.ODD
                    elif "even" in week_str:
                        week_type = WeekType.EVEN
                    else:
                        week_type = WeekType.ALL

                    # 唯一键识别同一时段的连堂课程
                    key = f"{code or name}_{day_of_week}_{classroom}_{week_type.value}"

                    existing = active_courses.get(key)
                    if existing and period == existing.end_period + 1:
                        existing.end_period = period
                    else:
                        raw_id = item.get("id")
                        course_id = f"{raw_id}_p{period}" if raw_id and existing else (raw_id or f"course_{len(courses)+1}")
                        c = Course(
                            id=course_id,
                            name=name,
                            code=code,
                            classroom=classroom,
                            teacher=teacher,
                            day_of_week=day_of_week,
                            start_period=period,
                            end_period=period,
                            start_week=1,
                            end_week=16,
                            week_type=week_type,
                        )
                        courses.append(c)
                        active_courses[key] = c

        return courses

    async def fetch_timetable(self, semester_id: Optional[str] = None) -> List[Course]:
        """获取并解析学期课程表"""
        url = "/api/student/es/myTimetable?type="
        resp = await self.session.get(url)
        resp.raise_for_status()
        data = resp.json()
        return self.parse_timetable_payload(data)

    async def close(self):
        await self.session.aclose()

