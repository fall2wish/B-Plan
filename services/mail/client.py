"""
BNBU Student Email Client (IMAP / SMTP)
校园邮箱客户端协议集成
"""

import imaplib
import email
from email.header import decode_header
import logging
from typing import List, Optional
from datetime import datetime
from ..models import EmailMessage
from .parser import EmailScheduleParser

logger = logging.getLogger(__name__)


class MailClient:
    """校园邮箱客户端"""

    def __init__(
        self,
        server: str = "imap.exmail.qq.com",
        port: int = 993,
        use_ssl: bool = True,
        smtp_server: str = "smtp.exmail.qq.com",
        smtp_port: int = 465,
    ):
        self.server = server
        self.port = port
        self.use_ssl = use_ssl
        self.smtp_server = smtp_server
        self.smtp_port = smtp_port
        self._connection: Optional[imaplib.IMAP4_SSL] = None

    def connect(self, username: str, password: str) -> bool:
        """连接并登录邮箱"""
        try:
            if self.use_ssl:
                self._connection = imaplib.IMAP4_SSL(self.server, self.port)
            else:
                self._connection = imaplib.IMAP4(self.server, self.port)
            self._connection.login(username, password)
            return True
        except Exception as e:
            logger.error(f"Email login error: {e}")
            return False

    def fetch_recent_messages(self, limit: int = 20) -> List[EmailMessage]:
        """抓取最新邮件并自动分析日程"""
        if not self._connection:
            raise RuntimeError("Email client not connected")

        messages: List[EmailMessage] = []
        try:
            self._connection.select("INBOX")
            status, data = self._connection.search(None, "ALL")
            if status != "OK":
                return messages

            msg_ids = data[0].split()
            for msg_id in msg_ids[-limit:]:
                res, msg_data = self._connection.fetch(msg_id, "(RFC822)")
                if res != "OK":
                    continue

                raw_email = msg_data[0][1]
                msg = email.message_from_bytes(raw_email)

                # 解析标题
                subject_header = decode_header(msg.get("Subject", ""))[0]
                subject = (
                    subject_header[0].decode(subject_header[1] or "utf-8", errors="ignore")
                    if isinstance(subject_header[0], bytes)
                    else str(subject_header[0])
                )

                # 解析发件人
                from_header = msg.get("From", "")

                # 提取正文
                body = ""
                if msg.is_multipart():
                    for part in msg.walk():
                        if part.get_content_type() == "text/plain":
                            body = part.get_payload(decode=True).decode("utf-8", errors="ignore")
                            break
                else:
                    body = msg.get_payload(decode=True).decode("utf-8", errors="ignore")

                email_obj = EmailMessage(
                    id=msg_id.decode("utf-8", errors="ignore"),
                    subject=subject,
                    sender=from_header,
                    date=datetime.now(),
                    body_text=body,
                )

                # 智能识别邮件中的日程
                suggested_event = EmailScheduleParser.parse_from_text(subject, body)
                if suggested_event:
                    email_obj.extracted_events.append(suggested_event)

                messages.append(email_obj)
        except Exception as e:
            logger.error(f"Error fetching emails: {e}")

        return list(reversed(messages))

    def disconnect(self):
        if self._connection:
            try:
                self._connection.close()
                self._connection.logout()
            except Exception:
                pass
