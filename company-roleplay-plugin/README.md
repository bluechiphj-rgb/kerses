# CompanyRoleplay 플러그인

Minecraft Paper 서버에서 간단한 회사 역할놀이를 즐길 수 있도록 도와주는 플러그인입니다. 회사 설립, 직원 초대, 직급과 급여 관리, Vault 연동을 통한 급여 지급 기능을 제공합니다.

## 주요 기능
- `/company create <이름>`: 회사를 설립하고 CEO가 됩니다.
- `/company invite <플레이어>`: 직원을 회사에 초대합니다.
- `/company join [회사명]`: 초대를 수락하고 회사에 입사합니다.
- `/company setrole <플레이어> <직급>`: 직원 직급을 변경합니다.
- `/company setsalary <플레이어> <금액>`: 직원 급여를 설정합니다.
- `/company payday`: Vault 경제 플러그인을 통해 급여를 지급합니다.
- `/company info [회사명]`: 회사 정보를 확인합니다.
- `/company disband`, `/company leave`: 회사 해산 및 탈퇴 지원.

## 설치 방법
1. `mvn package` 명령으로 플러그인을 빌드합니다.
2. `target/company-roleplay-1.0.0.jar` 파일을 서버의 `plugins` 폴더에 넣습니다.
3. Vault 및 호환되는 경제 플러그인이 설치되어 있다면 급여 지급 기능을 사용할 수 있습니다.
4. 서버를 재시작하거나 `/reload`로 플러그인을 로드합니다.

## 데이터 저장
- 플러그인은 `plugins/CompanyRoleplay/companies.yml` 파일에 회사 데이터를 저장합니다.
- 기본 설정은 `config.yml`에서 관리할 수 있습니다.
  - `messages.prefix`: 모든 안내 메시지 앞에 붙는 접두사
  - `settings.default_salary`: 신규 직원과 CEO에게 기본으로 설정되는 급여 금액

## 권한 제어
기본적으로 회사 내 권한은 직급을 기반으로 합니다. CEO, Director, Manager 직급은 직원 관리 및 급여 지급이 가능합니다.

## 빌드 요구사항
- Java 21 이상
- Maven
- Paper API 1.21.10 (제공 스코프)
- Vault API (제공 스코프)

## 라이선스
이 플러그인은 저장소의 [LICENSE](../LICENSE) 조항을 따릅니다.
