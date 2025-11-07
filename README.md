# advanced_curses

`advanced_curses`는 Python의 `curses` 모듈을 기반으로 보다 현대적인 터미널 UI 개발 경험을 제공하는 고수준 라이브러리입니다. 기본적인 화면 초기화/정리 기능은 물론, 다음과 같은 확장 기능을 제공합니다.

- **이벤트 루프**: 타이머, 반복 작업, 키보드 및 마우스 이벤트를 하나의 루프에서 관리합니다.
- **레이아웃 시스템**: 수평/수직 및 그리드 레이아웃으로 위젯을 배치할 수 있습니다.
- **위젯 컬렉션**: 라벨, 텍스트 입력, 프로그레스 바 등 기본 위젯을 제공하며 초점 전환을 지원합니다.
- **테마 관리**: 색상 페어와 속성을 손쉽게 정의하고 적용할 수 있는 테마 매니저를 제공합니다.

## 빠른 시작

### 설치

```bash
pip install .
```

윈도우 환경에서는 추가적으로 `windows` 익스트라를 사용하여 curses 호환 패키지를 함께 설치할 수 있습니다.

```bash
pip install .[windows]
```

### 사용 예시

```python
from advanced_curses import CursesApp, Label, HLayout, Rect, WidgetManager

def render(screen):
    layout = HLayout()
    manager = WidgetManager()
    manager.add(Label(text="왼쪽", align="left"))
    manager.add(Label(text="가운데", align="center"))
    manager.add(Label(text="오른쪽", align="right"))
    layout.add(manager.widgets[0])
    layout.add(manager.widgets[1])
    layout.add(manager.widgets[2])
    height, width = screen.size()
    manager.render(screen, layout, Rect(0, 0, height, width))

with CursesApp() as app:
    app.run(render)

# 입력 처리 예시

```python
from advanced_curses import CursesApp, Label, Rect, VLayout, WidgetManager

manager = WidgetManager()
manager.add(Label(text="키를 눌러보세요"))

def render(screen):
    height, width = screen.size()
    layout = VLayout()
    for widget in manager.widgets:
        layout.add(widget)
    manager.render(screen, layout, Rect(0, 0, height, width))

def on_key(event):
    if event.is_mouse:
        manager.widgets[0].text = f"마우스: {event.mouse_state}"  # type: ignore[attr-defined]
    else:
        manager.widgets[0].text = f"키코드: {event.key}"

with CursesApp() as app:
    app.run(render, handle_input=on_key)
```
```

## 라이선스

이 프로젝트는 [LICENSE](LICENSE)에 명시된 MIT 라이선스를 따릅니다.
