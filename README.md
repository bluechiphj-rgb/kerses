# Complete Self-Hosted Mail Server and Python Email Service

This project provides a reproducible setup for a Docker-based mail server stack (Postfix, Dovecot, Rspamd) together with a Python 3.10+ email service for sending and receiving HTML email without relying on a spam folder.

## ① 전체 아키텍처 설명

1. **Postfix (SMTP)** – Handles outbound and inbound SMTP delivery, integrates with Dovecot for user authentication and mailbox delivery via LMTP, and pipes messages through Rspamd for spam scoring.
2. **Dovecot (IMAP/LMTP)** – Stores mail in Maildir format under a shared volume, exposes IMAPs/IMAP ports for client access, and receives mail from Postfix via LMTP while remaining configured not to create or use any spam folder.
3. **Rspamd** – Inspects every message received by Postfix, annotates X-Spam headers with scores, but does not move mail to a spam folder. Administrators can tune thresholds in the provided configuration.
4. **DKIM/DMARC/SPF** – Postfix signs outgoing mail with OpenDKIM using the provided key volume. DNS records are included in this documentation so the setup can pass authentication checks.
5. **Python Email Service** – A FastAPI application that exposes REST endpoints for sending templated email and listing received mail from INBOX only. It uses smtplib for delivery via Postfix and imaplib for fetching. Helper utilities parse Rspamd headers to expose spam scores.
6. **Docker Compose Orchestration** – All services share a bridged network. Volumes ensure persistent mail storage, configuration, and DKIM keys. Environment variables allow customization of mail domains and credentials.

The stack is intended for development or small deployments; TLS certificates can be swapped with real ones for production.

## ② 폴더 구조

```
/
├── README.md
├── docker-compose.yml
├── mailserver/
│   ├── postfix/
│   │   ├── Dockerfile
│   │   ├── entrypoint.sh
│   │   ├── main.cf
│   │   ├── master.cf
│   │   ├── sasl_passwd
│   │   └── transport
│   ├── dovecot/
│   │   ├── Dockerfile
│   │   ├── conf.d/
│   │   │   ├── 10-auth.conf
│   │   │   ├── 10-mail.conf
│   │   │   ├── 10-master.conf
│   │   │   ├── 10-ssl.conf
│   │   │   └── 20-imap.conf
│   │   └── dovecot.conf
│   ├── rspamd/
│   │   ├── Dockerfile
│   │   └── local.d/
│   │       ├── classifier-bayes.conf
│   │       ├── logging.conf
│   │       └── worker-controller.inc
│   └── dkim/
│       ├── default.selector
│       └── keys/
│           └── mail.private
├── app/
│   ├── email_service.py
│   ├── fastapi_app.py
│   ├── templates/
│   │   ├── alert_email.html.j2
│   │   └── verification_email.html.j2
│   ├── receive_email.py
│   ├── requirements.txt
│   └── settings.py
```

## ③ docker-compose.yml

See [`docker-compose.yml`](./docker-compose.yml) for the orchestrated services.

## ④ 모든 설정 파일 제공

All Postfix, Dovecot, and Rspamd configuration files are located under `mailserver/`.

## ⑤ DNS 레코드 예시

DNS records are provided in the [DNS section](#dns-레코드-예시).

## ⑥ Python 이메일 발송 코드

See [`app/email_service.py`](app/email_service.py) and [`app/fastapi_app.py`](app/fastapi_app.py) for the sending logic.

## ⑦ Python 이메일 수신 코드

See [`app/receive_email.py`](app/receive_email.py).

## ⑧ FastAPI 구현본

Optional REST API implementation can be found in [`app/fastapi_app.py`](app/fastapi_app.py).

## ⑨ 실행 방법 및 테스트 방법

Detailed instructions are listed in the [Usage](#usage) section.

---

## Docker Images Overview

Each service uses a purpose-built image with minimal configuration to satisfy the requirements. Adjust domain names and passwords in `.env` (see sample below) before starting the stack.

### Environment Variables

Create a `.env` file (or export in your shell) before running Docker Compose:

```
MAIL_DOMAIN=example.com
MAIL_HOSTNAME=mail.example.com
MAIL_USER=dev
MAIL_PASSWORD=StrongPassw0rd!
POSTMASTER=postmaster@example.com
DKIM_SELECTOR=default
TZ=Asia/Seoul
```

These variables are referenced across service configurations for consistency.

## Usage

1. **Generate DKIM Key (once):**
   ```bash
   docker compose run --rm postfix opendkim-genkey -s "$DKIM_SELECTOR" -d "$MAIL_DOMAIN"
   mv mail.private mailserver/dkim/keys/mail.private
   mv mail.txt mailserver/dkim/default.selector
   chmod 600 mailserver/dkim/keys/mail.private
   ```
2. **Create virtual user password hash for Dovecot:**
   ```bash
   docker compose run --rm dovecot doveadm pw -s SHA512-CRYPT
   ```
   Paste the generated hash into `mailserver/dovecot/conf.d/10-auth.conf` replacing the placeholder.
3. **Start the stack:**
   ```bash
   docker compose up -d --build
   ```
4. **Verify service health:**
   ```bash
   docker compose ps
   docker compose logs postfix
   docker compose logs dovecot
   docker compose logs rspamd
   ```
5. **Run the Python email service locally:**
   ```bash
   cd app
   python -m venv .venv
   source .venv/bin/activate
   pip install -r requirements.txt
   uvicorn fastapi_app:app --reload
   ```
6. **Send a test email:**
   ```bash
   curl -X POST http://127.0.0.1:8000/send/verification \
     -H 'Content-Type: application/json' \
     -d '{"to": "recipient@example.com", "code": "123456"}'
   ```
7. **Fetch recent mail:**
   ```bash
   python receive_email.py --limit 5
   ```

### Testing Headers

Open any retrieved message from `receive_email.py` and confirm that `X-Spam-Status` / `X-Spam-Score` headers are present while the mail remains in `INBOX`.

## DNS 레코드 예시

Replace `example.com` with your domain and `mail.example.com` with your host.

| Type | Name | Value |
|------|------|-------|
| MX | @ | `10 mail.example.com.` |
| A | mail | `203.0.113.10` *(your server IP)* |
| TXT | @ | `v=spf1 mx a:mail.example.com ~all` |
| TXT | _dmarc | `v=DMARC1; p=quarantine; rua=mailto:dmarc@example.com; ruf=mailto:dmarc@example.com; fo=1` |
| TXT | default._domainkey | *(contents of `mailserver/dkim/default.selector`)* |

Update TTL values according to your DNS provider.

## Maintenance Notes

- **Spam Folder**: Disabled by ensuring Dovecot does not create `Spam` mailboxes and by leaving mailbox subscriptions empty. Clients reading IMAP will only see `INBOX` unless they create additional folders manually.
- **TLS**: Replace self-signed cert paths in the Dovecot/Postfix config with your certificates for production deployments.
- **Backups**: Volume `maildata` stores Maildir; ensure you back up this volume regularly.
- **Scaling**: For multiple domains/users, extend the virtual users table and update Postfix/Dovecot configs accordingly.

