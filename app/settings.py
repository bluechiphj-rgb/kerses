from __future__ import annotations

from pydantic import BaseSettings, EmailStr, Field


class MailSettings(BaseSettings):
    smtp_host: str = Field("postfix", alias="SMTP_HOST")
    smtp_port: int = Field(587, alias="SMTP_PORT")
    smtp_user: str = Field(..., alias="MAIL_USER")
    smtp_password: str = Field(..., alias="MAIL_PASSWORD")
    smtp_use_tls: bool = Field(True, alias="SMTP_USE_TLS")
    imap_host: str = Field("dovecot", alias="IMAP_HOST")
    imap_port: int = Field(143, alias="IMAP_PORT")
    imap_user: str = Field(..., alias="MAIL_USER")
    imap_password: str = Field(..., alias="MAIL_PASSWORD")
    mail_domain: str = Field(..., alias="MAIL_DOMAIN")
    from_email: EmailStr = Field(..., alias="MAIL_FROM")
    rspamd_score_header: str = Field("X-Spam-Score", alias="RSPAMD_SCORE_HEADER")
    rspamd_status_header: str = Field("X-Spam-Status", alias="RSPAMD_STATUS_HEADER")

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False


def get_settings() -> MailSettings:
    return MailSettings()
