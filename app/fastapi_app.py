from __future__ import annotations

import logging
from datetime import datetime
from typing import Any

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, EmailStr

from email_service import EmailService, get_email_service

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="Mail Service API", version="1.0.0")
service: EmailService = get_email_service()


class VerificationRequest(BaseModel):
    to: EmailStr
    code: str


class AlertRequest(BaseModel):
    to: EmailStr
    title: str
    message: str


@app.post("/send/verification")
def send_verification(body: VerificationRequest) -> dict[str, Any]:
    try:
        message_id = service.send_html_email(
            subject="Your verification code",
            to_addresses=[body.to],
            template_name="verification_email.html.j2",
            code=body.code,
        )
    except Exception as exc:  # noqa: BLE001 - surface error to API
        logger.exception("Failed to send verification email")
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    return {"message_id": message_id}


@app.post("/send/alert")
def send_alert(body: AlertRequest) -> dict[str, Any]:
    try:
        message_id = service.send_html_email(
            subject=body.title,
            to_addresses=[body.to],
            template_name="alert_email.html.j2",
            title=body.title,
            message=body.message,
            timestamp=datetime.utcnow().isoformat(),
        )
    except Exception as exc:  # noqa: BLE001
        logger.exception("Failed to send alert email")
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    return {"message_id": message_id}


@app.get("/inbox")
def list_inbox(limit: int = 10) -> dict[str, Any]:
    try:
        messages = service.fetch_inbox(limit=limit)
    except Exception as exc:  # noqa: BLE001
        logger.exception("Failed to fetch inbox")
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    return {"count": len(messages), "messages": messages}
