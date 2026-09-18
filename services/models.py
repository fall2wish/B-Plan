"""
B-Plan Core Data Models
定义课程、日程、作业 DDL、邮件及配置的统一数据契约
"""

from datetime import datetime, time
from enum import Enum
from typing import List, Optional
from pydantic import BaseModel, Field


class WeekType(str, Enum):
    ALL = "all"        # 全周
    ODD = "odd"        # 单周
    EVEN = "even"      # 双周
    CUSTOM = "custom"  # 指定周


class TimeSlot(BaseModel):
    """单节课作息配置"""
    period: int = Field(description="第几节 (1-indexed)")
    start_time: str = Field(description="开始时间 HH:MM, 如 '08:30'")
    end_time: str = Field(description="结束时间 HH:MM, 如 '09:15'")


class Course(BaseModel):
    """课程模型"""
    id: str = Field(description="唯一标识符")
    name: str = Field(description="课程名称，如 'Operating Systems'")
    code: Optional[str] = Field(default=None, description="课程代码，如 'COMP3033'")
    classroom: Optional[str] = Field(default=None, description="上课教室，如 'T1-101'")
    teacher: Optional[str] = Field(default=None, description="授课教师 (来自 GSAS)")
    assistant: Optional[str] = Field(default=None, description="课程助教 (来自 iSpace)")
    day_of_week: int = Field(ge=1, le=7, description="星期几 (1: 周一, 7: 周日)")
    start_period: int = Field(ge=1, description="起始节次")
    end_period: int = Field(ge=1, description="结束节次")
    start_week: int = Field(default=1, ge=1, description="起始周")
    end_week: int = Field(default=16, ge=1, description="结束周")
    week_type: WeekType = Field(default=WeekType.ALL, description="周次模式")
    active_weeks: List[int] = Field(default_factory=list, description="具体生效的周次列表")
    color: Optional[str] = Field(default=None, description="颜色十六进制，如 '#3B82F6'")

    def is_active_in_week(self, week: int) -> bool:
        """判断当前周该课程是否有课"""
        if self.active_weeks:
            return week in self.active_weeks
        if not (self.start_week <= week <= self.end_week):
            return False
        if self.week_type == WeekType.ALL:
            return True
        elif self.week_type == WeekType.ODD:
            return week % 2 != 0
        elif self.week_type == WeekType.EVEN:
            return week % 2 == 0
        return True


class ScheduleEvent(BaseModel):
    """非课程的独立日程事件 (如讲座、会议、邮件提取事件等)"""
    id: str
    title: str
    start_time: datetime
    end_time: datetime
    location: Optional[str] = None
    notes: Optional[str] = None
    source: str = Field(default="manual", description="来源: manual / email / reminder")
    color: Optional[str] = None


class SubmittedFile(BaseModel):
    """已提交的作业附件文件信息"""
    file_name: str
    file_size: str
    file_url: Optional[str] = None
    upload_timestamp: Optional[int] = None


class AssignmentDdl(BaseModel):
    """iSpace 作业与截止时间 (DDL)"""
    id: str
    course_name: str
    title: str
    due_date: datetime
    is_submitted: bool = False
    url: Optional[str] = None
    description: Optional[str] = None
    submitted_at: Optional[datetime] = None
    submission_status_text: Optional[str] = None
    grading_status_text: Optional[str] = None
    submitted_files: List[SubmittedFile] = Field(default_factory=list)
    is_due_modified: bool = False

    @property
    def is_expired(self) -> bool:
        return datetime.now() > self.due_date

    @property
    def remaining_hours(self) -> float:
        delta = self.due_date - datetime.now()
        return max(0.0, delta.total_seconds() / 3600.0)



class EmailMessage(BaseModel):
    """校园邮件结构"""
    id: str
    subject: str
    sender: str
    date: datetime
    body_text: str
    body_html: Optional[str] = None
    has_attachments: bool = False
    is_read: bool = False
    extracted_events: List[ScheduleEvent] = Field(default_factory=list)
