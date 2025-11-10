from __future__ import annotations

import argparse
import json
import logging
from typing import Any

from email_service import EmailService, get_email_service

logging.basicConfig(level=logging.INFO)


def main(limit: int) -> None:
    service: EmailService = get_email_service()
    messages = service.fetch_inbox(limit=limit)
    print(json.dumps(messages, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Fetch emails from INBOX only")
    parser.add_argument("--limit", type=int, default=10, help="Number of emails to fetch")
    args = parser.parse_args()
    main(args.limit)
