# CompanyLife

CompanyLife는 PaperMC 1.21.10/Folia 서버에서 "회사놀이 Enterprise++"를 구현하기 위한 모듈형 비즈니스 시뮬레이션 플러그인입니다. Vault/LuckPerms/PlaceholderAPI 연동과 SQLite/MySQL 지원, 비동기 트랜잭션, 감사/보안 로깅을 기본 제공합니다.

## 주요 특징

- **모듈 아키텍처**: core, economy, company, hr, payroll, tax, store, warehouse, logistics 등 30개 이상의 모듈을 서비스 레지스트리에 등록하여 필요에 따라 토글 가능.
- **비동기/트랜잭션 I/O**: HikariCP + Flyway 기반 DB, 모든 Repository 비동기 `CompletableFuture` API.
- **Folia 지원**: `FoliaScheduler`로 글로벌/지역/엔티티 안전 작업 실행.
- **감사 및 보안**: AuditService 배치 기록, LuckPerms/Vault 브릿지, PlaceholderAPI 확장, Discord Webhook.
- **대표 시나리오**: 회사 설립→지점/상점→재고→판매→회계/세무, 급여 사이클, 입찰/조달, 물류 배차를 `ScenarioOrchestrator`에서 실행.
- **GUI**: 각 모듈에 `ModuleDashboardGui` 기반 인벤토리 대시보드 제공.

## 설치

1. Gradle 빌드: `./gradlew build`
2. 생성된 `build/libs/CompanyLife-0.1.0-SNAPSHOT.jar`를 `plugins/`에 배치
3. 서버 최초 실행 후 `plugins/CompanyLife/config.yml`을 필요에 맞게 수정

## 의존성

- Paper 1.21.10 이상 (Folia 호환)
- Vault (경제) / LuckPerms (권한) / PlaceholderAPI (선택)
- SQLite 기본, MySQL 옵션

## 주요 설정 (`config.yml`)

```yaml
database:
  type: sqlite
  file: plugins/CompanyLife/data.db
performance:
  async_db: true
  rate_limits:
    money_transfer_per_min: 20
    contract_per_hour: 10
analytics:
  enable_webhook: true
  discord_webhook_url: ""
```

## 명령어

| 명령어 | 설명 |
| --- | --- |
| `/company create <이름> <형태>` | 회사를 생성합니다. |
| `/company branch <companyId> <world> <x> <y> <z>` | 지점을 등록합니다. |
| `/company gui [module]` | 모듈 대시보드를 엽니다. |
| `/company simulate` | 샘플 시나리오 실행 |
| `/admin reload` | config 재로드 |
| `/admin flush` | 감사 로그 플러시 |

## 권한

- `complife.*` : 전체 기능 (기본 op)
- `complife.company.use` : `/company` 명령
- `complife.admin` : 관리자 명령

## 테스트

- 단위 테스트: 전표 합산, 급여 계산, EOQ, 입찰 스코어, 경로 계산
- 통합 테스트: 시나리오 오케스트레이션, Discord Webhook degrade

`./gradlew test` 실행 시 모두 검증됩니다.

## PlaceholderAPI

- `%complife_company_name%`
- `%complife_balance_company%`
- `%complife_reputation%`

## Discord Webhook

`config.yml`에 URL을 지정하면 `AnalyticsService`가 KPI를 전송합니다.

## 문제 해결

- **DB 연결 오류**: `config.yml`의 database 설정 확인 후 `plugins/CompanyLife/data.db` 권한 확인
- **Folia 작업 중 예외**: `debug: true` 로 설정 후 로그 확인, FoliaScheduler는 모든 예외를 future로 반환
- **Webhook 실패**: 테스트 명령 또는 로그에서 HTTP 응답 코드 확인

## FAQ

- **Vault가 없으면?** `VaultBridge`가 자동으로 무시하며 로그에만 기록합니다.
- **MySQL 사용?** `config.yml`의 `database.type`을 `mysql`로 변경하고 접속 정보를 입력합니다.
- **PlaceholderAPI 미설치?** 확장은 비활성 상태로 유지되며 경고만 출력합니다.

## 개발

- Kotlin 1.9 + Java 21
- Gradle Kotlin DSL + paperweight + shadow 플러그인
- 패키지 구조: `common`(공통 서비스/유틸), `modules`(기능 모듈), `api`

기여 및 확장은 `ServiceRegistry`에 서비스 등록 후 모듈을 추가하면 됩니다.
