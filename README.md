# CompanyLife

CompanyLife는 PaperMC 1.21.10/Folia 서버에서 "회사놀이 Enterprise++" 비즈니스 시뮬레이션을 제공하는 모듈형 Kotlin 플러그인입니다. 30개 이상의 도메인 모듈과 서비스 레지스트리를 기반으로 대규모(200+ 동시) 환경에서 확장성과 안정성을 확보하며, Vault/LuckPerms/PlaceholderAPI 연동, 감사/보안 로깅, Discord Webhook, 비동기 DB 트랜잭션을 기본 제공합니다.

## 아키텍처 하이라이트

- **모듈 토글**: `modules/` 하위에 core, economy, company, hr, payroll, tax, store, warehouse, logistics, analytics 등 도메인 모듈을 정의하고 ServiceRegistry를 통해 동적으로 로드/비활성화합니다.
- **비동기/트랜잭션 I/O**: `DatabaseManager`는 HikariCP 풀과 Flyway 마이그레이션을 초기화하고, Repository는 모두 `CompletableFuture` 기반 비동기 API를 노출합니다.
- **Folia 스케줄링**: `FoliaScheduler`가 글로벌/지역/엔티티 경계를 준수하며 장시간 작업은 워크플로 큐로 위임합니다.
- **보안 & 감사**: `AuditService`가 재무/재고/인사 이벤트를 배치로 기록하고, LuckPerms/Vault 브릿지는 미설치 환경에서도 안전하게 degrade 됩니다.
- **대표 시나리오**: `ScenarioOrchestrator`는 회사 설립→지점/상점 개설→재고 입고→판매→회계/세무 처리, 급여 사이클, 입찰/조달, 물류 배차 시나리오를 오토메이션합니다.
- **GUI 대시보드**: `GuiManager`와 `ModuleDashboardGui`가 각 모듈별 인벤토리 UI, 툴팁, 페이지네이션, 확인 다이얼로그를 제공합니다.

## 설치 및 빌드

1. 필수 도구: JDK 21, Apache Maven 3.9 이상을 설치합니다.
2. 빌드: `mvn -B clean package`
3. 산출물: `target/CompanyLife-0.1.0.jar`
4. 플러그인 배포: 생성된 JAR를 서버 `plugins/` 폴더에 배치하고 서버를 실행합니다.

> **표준 JAR 배포**: 외부 라이브러리는 서버 런타임에 의해 제공되므로 섀도우/리로케이션 없이 생성된 JAR을 그대로 사용합니다.

## 런타임 의존성

- Paper 1.21.10 이상 (Folia 지원)
- Vault (경제), LuckPerms (권한), PlaceholderAPI (선택)
- SQLite 기본, `config.yml`에서 MySQL 전환 가능

## 기본 구성 (`config.yml`)

```yaml
database:
  type: sqlite
  file: plugins/CompanyLife/data.db
performance:
  async_db: true
  cache_sizes:
    companies: 2000
    products: 5000
  rate_limits:
    money_transfer_per_min: 20
    contract_per_hour: 10
economy:
  vault: true
  starting_company_balance: 10000
legal:
  permits_required: true
  anti_abuse_thresholds:
    max_transfer: 500000
    daily_withdraw_limit: 1000000
analytics:
  enable_webhook: true
  discord_webhook_url: ""
```

## 핵심 명령어

| 명령어 | 설명 |
| --- | --- |
| `/company create <이름> <형태>` | 회사를 생성하고 법인 유형을 설정합니다. |
| `/company branch create <companyId> <world> <x> <y> <z>` | 지점을 등록합니다. |
| `/company store open <branchId>` | 상점 모듈 대시보드를 열어 재고/판매를 관리합니다. |
| `/company warehouse audit <branchId>` | 창고 입출고/EOQ 상태를 확인합니다. |
| `/company payroll run <companyId>` | 급여 사이클을 실행하고 Ledger에 반영합니다. |
| `/company logistics dispatch <companyId>` | 배송 경로를 산출하고 SLA를 검증합니다. |
| `/company simulate` | 대표 비즈니스 시나리오를 실행합니다. |
| `/admin reload` | 구성/메시지 리로드 |
| `/admin migrate` | Flyway 마이그레이션 실행 |
| `/admin flush` | 감사 로그를 즉시 플러시 |

## 권한 체계

- `complife.*` : 전체 권한 (기본 OP)
- `complife.company.*` : 회사/지점/상점 명령
- `complife.hr.*` : HR/급여 모듈
- `complife.finance.*` : 회계/세무/재무 모듈
- `complife.admin.*` : 관리 명령 (reload/migrate/rollback 등)

## 시뮬레이션 워크플로

1. **회사 설립 & 지점 개설** – `CompanyService`와 `BranchRepository`가 엔티티를 생성하고 감사 로그 기록.
2. **재고 & 판매 사이클** – `WarehouseService`가 EOQ와 안전재고를 계산, `StoreService`가 판매/할인/회계 분개를 처리.
3. **급여 처리** – `PayrollService`가 근태 로그와 KPI를 기반으로 세전/세후 급여를 산정하고 Vault 연동으로 지급.
4. **세무 & 회계** – `TaxService`가 누진세 구간과 신고 기한을 계산, `LedgerService`가 복식부기 원장을 유지.
5. **물류 배차** – `LogisticsService`가 Dijkstra 기반 경로/ETA를 산출하고 SLA 위반 시 페널티를 적용.
6. **입찰/조달** – `ProcurementService`가 공고→제안→스코어링→낙찰→계약 확정을 지원.
7. **분석 & 알림** – `AnalyticsService`가 KPI 스냅샷을 생성하고 Discord Webhook/PlaceholderAPI 지표를 전송.

## 테스트

- **단위 테스트**: Ledger 집계, 급여 계산, EOQ/안전재고, 입찰 스코어, 물류 경로.
- **통합 테스트**: 대표 비즈니스 시나리오, Discord Webhook degrade, 가짜 Economy 어댑터.
- 실행: `mvn test`

## 통합 기능

- **PlaceholderAPI**: `%complife_company_name%`, `%complife_balance_company%`, `%complife_reputation%`, `%complife_kpi_profit_7d%` 등 지표 제공.
- **Discord Webhook**: `config.yml`에 URL을 설정하면 KPI/경보를 비동기 전송.
- **Vault/LuckPerms**: 없을 경우 안전하게 비활성화되며 로그만 남깁니다.

## 트러블슈팅

| 증상 | 해결 |
| --- | --- |
| DB 연결 실패 | `config.yml` 데이터베이스 설정 및 파일 권한 확인, 필요 시 MySQL 접속 정보 재검토 |
| Folia 지역 스레드 예외 | `debug: true` 설정 후 로그 확인, FoliaScheduler를 통해 스케줄링 여부 점검 |
| Webhook 전송 실패 | Discord URL 및 HTTP 응답 코드 확인, 재시도 로직이 자동 적용됨 |
| Vault 미탐지 | VaultBridge가 자동 degrade, 경제 기능은 비활성 상태로 유지 |

## 개발 가이드

- **언어/도구**: Kotlin 1.9, Java 21, Apache Maven 3.9+.
- **패키지 구조**: `api/`(내부 API), `common/`(공통 서비스/유틸/인프라), `modules/`(도메인 모듈).
- **확장 방법**: 신규 모듈을 `Module` 인터페이스로 구현 후 `ModuleLoader`에 등록하고 ServiceRegistry에 필요한 서비스를 추가합니다.
- **코딩 스타일**: Adventure MiniMessage 메시지, Result/Either 패턴, NPE 예방, 명시적 로그 레벨.

기여와 모듈 확장은 이슈/PR을 통해 환영합니다. Folia-safe 비동기 코드를 유지하며, 모든 재무/인사 이벤트는 AuditService를 통해 감사 로그로 남겨야 합니다.
