# 완성형 자체 호스팅 메일 서버와 Python 이메일 서비스

이 프로젝트는 Docker 기반 메일 서버 스택(Postfix, Dovecot, Rspamd)과 Python 3.10+ 이메일 서비스 코드를 함께 제공하여, 스팸함 없이 INBOX만 사용하는 완전한 메일 발송/수신 환경을 재현할 수 있도록 구성했습니다. 모든 설정 파일과 실행 방법을 단계별로 안내하므로, 초보자도 따라 할 수 있습니다.

## ① 전체 아키텍처 설명

1. **Postfix (SMTP)** – 외부 및 내부 메일 송수신을 담당합니다. Dovecot과 연동하여 LMTP로 메일을 전달하고, Rspamd와 연계해 스팸 점수를 헤더에 추가합니다.
2. **Dovecot (IMAP/LMTP)** – Maildir 형식으로 메일을 저장하고 IMAP 접근을 제공합니다. 스팸 폴더는 생성하지 않도록 설정되어 INBOX만 유지합니다.
3. **Rspamd** – 모든 메일을 검사하여 `X-Spam-*` 헤더에 점수를 기록합니다. 스팸 폴더 이동은 절대 수행하지 않습니다.
4. **DKIM/SPF/DMARC** – Postfix에서 OpenDKIM을 사용해 서명하며, DNS 레코드 예시를 통해 신뢰성을 확보할 수 있습니다.
5. **Python 이메일 서비스** – 로컬 Postfix를 통해 HTML 메일을 발송하고, Dovecot IMAP에서 INBOX 메일을 조회합니다. FastAPI 기반 REST API와 Jinja2 템플릿을 포함합니다.
6. **Docker Compose 오케스트레이션** – 모든 서비스가 단일 브릿지 네트워크에서 동작하며, 볼륨으로 데이터를 영구 저장합니다. `.env` 파일로 도메인, 계정, 타임존 등을 손쉽게 변경할 수 있습니다.

> ⚠️ 참고: 본 구성은 개발·테스트 목적에 적합하며, 운영 환경에서는 실제 TLS 인증서와 보안 정책을 적용해야 합니다.

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

루트 디렉터리의 [`docker-compose.yml`](./docker-compose.yml) 파일에 Postfix, Dovecot, Rspamd, DKIM, FastAPI 서비스 구성이 모두 포함되어 있습니다. 환경 변수는 `.env` 파일을 통해 주입됩니다.

## ④ 모든 설정 파일 제공

Postfix, Dovecot, Rspamd의 세부 설정은 `mailserver/` 하위 디렉터리에 모두 포함되어 있습니다. 필요 시 해당 파일을 수정하여 사용자 수, 도메인, TLS 경로 등을 확장할 수 있습니다.

## ⑤ DNS 레코드 예시

아래 표는 `example.com` 도메인과 `mail.example.com` 호스트를 기준으로 작성된 예시입니다. 운영 도메인에 맞게 값만 변경하면 됩니다.

| 타입 | 이름 | 값 |
|------|------|----|
| MX | @ | `10 mail.example.com.` |
| A | mail | `203.0.113.10` *(메일 서버 공인 IP)* |
| TXT | @ | `v=spf1 mx a:mail.example.com ~all` |
| TXT | _dmarc | `v=DMARC1; p=quarantine; rua=mailto:dmarc@example.com; ruf=mailto:dmarc@example.com; fo=1` |
| TXT | default._domainkey | `mailserver/dkim/default.selector` 파일 내용 |

TTL은 DNS 제공업체 권장 값으로 설정하면 됩니다.

## ⑥ Python 이메일 발송 코드

`app/email_service.py`와 `app/fastapi_app.py` 파일에 SMTP 발송 로직과 FastAPI REST 엔드포인트가 구현되어 있습니다. Jinja2 템플릿(`app/templates/*.j2`)을 사용해 인증 코드 메일과 알림 메일을 HTML로 렌더링합니다.

## ⑦ Python 이메일 수신(스팸 폴더 제외) 코드

`app/receive_email.py`는 Dovecot IMAP 서버의 INBOX만 조회하며, 존재하더라도 스팸 폴더는 무시합니다. Rspamd가 작성한 `X-Spam-Score` 헤더 값을 파싱하여 점수를 함께 출력합니다.

## ⑧ FastAPI 구현본

`app/fastapi_app.py`는 `/send/verification`, `/send/alert`, `/inbox` 등의 엔드포인트를 제공해 외부 시스템에서 HTTP 요청만으로 메일 발송 및 수신 목록 확인을 수행할 수 있습니다.

## ⑨ 실행 방법 및 테스트 방법

아래 순서를 따라 최초 설정과 실행을 완료하세요.

### 1. 환경 변수 파일 생성

`.env` 파일을 프로젝트 루트에 생성한 후 아래 예시 값을 입력합니다.

```
MAIL_DOMAIN=example.com
MAIL_HOSTNAME=mail.example.com
MAIL_USER=dev
MAIL_PASSWORD=StrongPassw0rd!
POSTMASTER=postmaster@example.com
DKIM_SELECTOR=default
TZ=Asia/Seoul
```

### 2. DKIM 키 생성 (최초 1회)

```bash
docker compose run --rm postfix opendkim-genkey -s "$DKIM_SELECTOR" -d "$MAIL_DOMAIN"
mv mail.private mailserver/dkim/keys/mail.private
mv mail.txt mailserver/dkim/default.selector
chmod 600 mailserver/dkim/keys/mail.private
```

### 3. Dovecot 가상 사용자 비밀번호 해시 생성

```bash
docker compose run --rm dovecot doveadm pw -s SHA512-CRYPT
```

생성된 해시를 `mailserver/dovecot/conf.d/10-auth.conf` 파일의 플레이스홀더 자리에 붙여넣습니다.

### 4. 스택 빌드 및 실행

```bash
docker compose up -d --build
```

### 5. 서비스 정상 동작 확인

```bash
docker compose ps
docker compose logs postfix
docker compose logs dovecot
docker compose logs rspamd
```

### 6. Python 애플리케이션 설치 및 실행

```bash
cd app
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn fastapi_app:app --reload
```

### 7. 테스트 메일 발송

```bash
curl -X POST http://127.0.0.1:8000/send/verification \
  -H 'Content-Type: application/json' \
  -d '{"to": "recipient@example.com", "code": "123456"}'
```

### 8. INBOX 메일 조회

```bash
python receive_email.py --limit 5
```

조회된 메일의 헤더를 확인하면 `X-Spam-Status`, `X-Spam-Score`가 추가되어 있지만 메일은 INBOX에 그대로 남아 있어야 합니다.

## Docker 이미지 요약

- **Postfix 이미지**: 커스텀 Dockerfile로 빌드되며, SASL 인증과 OpenDKIM 연계를 위한 엔트리포인트 스크립트를 포함합니다.
- **Dovecot 이미지**: IMAP, LMTP 설정이 포함되어 있으며 스팸 폴더 생성을 막기 위한 설정이 적용되어 있습니다.
- **Rspamd 이미지**: `local.d` 디렉터리의 설정 파일을 사용해 로그 레벨, 베이지안 학습기 등을 초기화합니다.
- **FastAPI 앱**: `app/Dockerfile`에서 빌드되어 SMTP/IMAP 기능을 호출합니다.

## 유지보수 팁

- **스팸 폴더 금지 유지**: 클라이언트에서 수동으로 스팸 폴더를 만들지 않도록 안내하고, 필요 시 Dovecot 설정을 추가로 제한하세요.
- **TLS 인증서**: 실제 서비스에선 Let’s Encrypt 등으로 발급한 인증서를 `mailserver/postfix`와 `mailserver/dovecot` 설정에 반영하세요.
- **백업**: `maildata`, `mailstate` 볼륨은 주기적으로 백업하여 데이터 손실을 대비합니다.
- **확장**: 다중 도메인이나 추가 계정이 필요하면 Postfix의 가상 도메인 매핑과 Dovecot 사용자 테이블을 확장하세요.

