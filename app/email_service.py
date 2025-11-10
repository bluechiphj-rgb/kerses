from __future__ import annotations

import imaplib
import logging
import smtplib
from email.message import EmailMessage
from email.utils import formataddr, make_msgid
from pathlib import Path
from typing import Iterable, Optional

from jinja2 import Environment, FileSystemLoader, select_autoescape

from settings import MailSettings, get_settings

logger = logging.getLogger(__name__)

TEMPLATE_ENV = Environment(
    loader=FileSystemLoader(Path(__file__).parent / "templates"),
    autoescape=select_autoescape(["html", "xml"]),
)


class EmailService:
    """Utility class for sending and receiving email via the local stack."""

    def __init__(self, settings: Optional[MailSettings] = None) -> None:
        self.settings = settings or get_settings()

    # ------------------------------------------------------------------
    # Sending
    # ------------------------------------------------------------------
    def render_template(self, template_name: str, **context: object) -> str:
        template = TEMPLATE_ENV.get_template(template_name)
        return template.render(**context)

    def _build_message(
        self,
        subject: str,
        html_body: str,
        to_addresses: Iterable[str],
    ) -> EmailMessage:
        msg = EmailMessage()
        msg["Subject"] = subject
        msg["From"] = formataddr(("No Reply", self.settings.from_email))
        msg["To"] = ", ".join(to_addresses)
        msg["Message-ID"] = make_msgid(domain=self.settings.mail_domain)
        msg.set_content("This email requires an HTML-capable client.")
        msg.add_alternative(html_body, subtype="html")
        return msg

    def send_html_email(
        self,
        subject: str,
        to_addresses: Iterable[str],
        template_name: str,
        **context: object,
    ) -> str:
        html_body = self.render_template(template_name, **context)
        message = self._build_message(subject, html_body, to_addresses)

        logger.info("Connecting to SMTP host %s:%s", self.settings.smtp_host, self.settings.smtp_port)
        with smtplib.SMTP(self.settings.smtp_host, self.settings.smtp_port) as smtp:
            if self.settings.smtp_use_tls:
                smtp.starttls()
            smtp.login(self.settings.smtp_user, self.settings.smtp_password)
            smtp.send_message(message)
        logger.info("Email sent to %s", to_addresses)
        return message["Message-ID"]

    # ------------------------------------------------------------------
    # Receiving
    # ------------------------------------------------------------------
    def fetch_inbox(self, limit: int = 10) -> list[dict[str, object]]:
        logger.info("Connecting to IMAP host %s:%s", self.settings.imap_host, self.settings.imap_port)
        mailbox = imaplib.IMAP4(self.settings.imap_host, self.settings.imap_port)
        try:
            mailbox.login(self.settings.imap_user, self.settings.imap_password)
            mailbox.select("INBOX")
            status, message_numbers = mailbox.search(None, "ALL")
            if status != "OK":
                raise RuntimeError("Failed to search mailbox")
            ids = message_numbers[0].split()
            latest_ids = ids[-limit:][::-1]
            results: list[dict[str, object]] = []
            for msg_id in latest_ids:
                status, data = mailbox.fetch(msg_id, "(BODY.PEEK[HEADER])")
                if status != "OK":
                    logger.warning("Failed to fetch message %s", msg_id)
                    continue
                headers = data[0][1].decode("utf-8", errors="replace")
                results.append(self._parse_headers(headers))
            return results
        finally:
            mailbox.logout()

    def _parse_headers(self, raw_headers: str) -> dict[str, object]:
        headers: dict[str, object] = {}
        for line in raw_headers.splitlines():
            if not line or line.startswith("\t") or line.startswith(" "):
                continue
            if ":" not in line:
                continue
            key, value = line.split(":", 1)
            headers[key.strip()] = value.strip()
        score_header = self.settings.rspamd_score_header
        status_header = self.settings.rspamd_status_header
        headers["spam_score"] = float(headers.get(score_header, "0")) if headers.get(score_header) else 0.0
        headers["spam_status"] = headers.get(status_header, "unknown")
        return headers


def get_email_service(settings: Optional[MailSettings] = None) -> EmailService:
    return EmailService(settings=settings)
