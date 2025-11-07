"""Widget implementations built on top of :mod:`advanced_curses.core`."""
from __future__ import annotations

import curses
from dataclasses import dataclass
from typing import Callable, Optional

from .core import Screen, Window
from .layout import BaseLayout, Rect


class Widget:
    """Base class for simple immediate mode widgets."""

    def __init__(self, *, focusable: bool = False) -> None:
        self.focusable = focusable
        self.has_focus = False

    def render(self, window: Window) -> None:
        raise NotImplementedError

    def on_key(self, key: int) -> None:
        """Handle keyboard input. Default implementation ignores all keys."""

    def focus(self) -> None:
        self.has_focus = True

    def blur(self) -> None:
        self.has_focus = False


@dataclass
class Label(Widget):
    """Simple text label widget."""

    text: str
    align: str = "left"

    def __post_init__(self) -> None:
        super().__init__(focusable=False)

    def render(self, window: Window) -> None:  # type: ignore[override]
        available = max(1, window.width - 2)
        padded = self.text[:available]
        if self.align == "center":
            padded = padded.center(available)
        elif self.align == "right":
            padded = padded.rjust(available)
        y = 1 if window.height > 2 else 0
        x = 1 if window.width > 2 else 0
        window.write(y, x, padded)


@dataclass
class TextInput(Widget):
    """Basic single-line text input widget."""

    value: str = ""
    placeholder: str = ""
    cursor: int = 0
    on_change: Optional[Callable[[str], None]] = None

    def __post_init__(self) -> None:
        super().__init__(focusable=True)

    def render(self, window: Window) -> None:  # type: ignore[override]
        available = max(1, window.width)
        display = self.value or self.placeholder
        window.write(0, 0, display[:available])
        if self.has_focus:
            cursor_x = min(self.cursor, available - 1)
            window.inner.move(0, cursor_x)

    def on_key(self, key: int) -> None:  # type: ignore[override]
        if key in (10, 13):  # Enter
            return
        if key in (curses.KEY_LEFT, 260):
            if self.cursor > 0:
                self.cursor -= 1
            return
        if key in (curses.KEY_RIGHT, 261):
            if self.cursor < len(self.value):
                self.cursor += 1
            return
        if key in (8, 127):  # Backspace
            if self.cursor > 0:
                self.value = self.value[: self.cursor - 1] + self.value[self.cursor :]
                self.cursor -= 1
                self._notify()
            return
        if 32 <= key <= 126:
            char = chr(key)
            self.value = self.value[: self.cursor] + char + self.value[self.cursor :]
            self.cursor += 1
            self._notify()

    def _notify(self) -> None:
        if self.on_change:
            self.on_change(self.value)


@dataclass
class ProgressBar(Widget):
    """ASCII progress bar widget."""

    progress: float = 0.0
    show_percentage: bool = True

    def __post_init__(self) -> None:
        super().__init__(focusable=False)

    def render(self, window: Window) -> None:  # type: ignore[override]
        width = max(1, window.width - 2)
        filled = int(width * max(0.0, min(1.0, self.progress)))
        bar = "█" * filled + " " * (width - filled)
        window.write(0, 0, f"[{bar}]")
        if self.show_percentage:
            percent = f" {self.progress * 100:5.1f}%"
            window.write(1 if window.height > 1 else 0, 0, percent.strip()[: window.width])


class WidgetManager:
    """Manage focus and rendering for widgets."""

    def __init__(self) -> None:
        self.widgets: list[Widget] = []
        self.focus_index = 0

    def add(self, widget: Widget) -> None:
        self.widgets.append(widget)
        if widget.focusable and not any(w.focusable and w.has_focus for w in self.widgets[:-1]):
            widget.focus()
            self.focus_index = self.widgets.index(widget)

    def next_focus(self) -> None:
        focusables = [index for index, w in enumerate(self.widgets) if w.focusable]
        if not focusables:
            return
        current_focusable_idx = 0
        if self.widgets and self.widgets[self.focus_index].focusable:
            current_focusable_idx = focusables.index(self.focus_index)
            self.widgets[self.focus_index].blur()
        next_index = focusables[(current_focusable_idx + 1) % len(focusables)]
        self.widgets[next_index].focus()
        self.focus_index = next_index

    def handle_key(self, key: int) -> None:
        if key == 9:  # Tab
            self.next_focus()
            return
        if not self.widgets:
            return
        widget = self.widgets[self.focus_index]
        if not widget.focusable:
            focusables = [index for index, w in enumerate(self.widgets) if w.focusable]
            if not focusables:
                return
            self.focus_index = focusables[0]
            widget = self.widgets[self.focus_index]
        widget.on_key(key)

    def render(self, screen: Screen, layout: BaseLayout, rect: Rect) -> None:
        placements = layout.compute(rect)
        for widget, widget_rect in placements:
            if not isinstance(widget, Widget):
                continue
            window = Window(screen, widget_rect.y, widget_rect.x, widget_rect.height, widget_rect.width)
            window.clear()
            window.draw()
            widget.render(window)
            window.refresh()

