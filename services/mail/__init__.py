"""
Mail Service Package
"""

from .client import MailClient
from .parser import EmailScheduleParser

__all__ = ["MailClient", "EmailScheduleParser"]
