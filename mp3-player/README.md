# Aurora MP3 Player

Aurora MP3 Player는 JavaFX로 제작한 현대적인 디자인의 MP3 재생기입니다. 부드러운 그라데이션 배경과 글래스모픽 카드, 원형 버튼 등을 통해 고급스러운 사용자 경험을 제공합니다.

## 주요 기능

- MP3 파일 열기 및 재생/일시정지/정지 제어
- 현재 시간과 전체 재생 시간 표시
- 슬라이더를 통한 직관적인 탐색 및 진행 상황 시각화
- 재생 상태(READY/PLAYING/PAUSED/STOPPED/FINISHED)를 즉시 확인

## 실행 방법

1. [Temurin JDK 17](https://adoptium.net/) 이상을 설치합니다.
2. 프로젝트 루트에서 다음 명령으로 의존성을 다운로드하고 애플리케이션을 실행합니다.

   ```bash
   mvn -f mp3-player/pom.xml javafx:run
   ```

3. 실행 후 **열기** 버튼을 눌러 원하는 MP3 파일을 선택하면 재생이 시작됩니다.

## 빌드

실행 가능한 JAR 아카이브를 생성하려면 다음 명령을 실행하세요.

```bash
mvn -f mp3-player/pom.xml clean package
```

생성된 아카이브는 `mp3-player/target/modern-mp3-player-1.0.0.jar` 경로에서 확인할 수 있습니다.
