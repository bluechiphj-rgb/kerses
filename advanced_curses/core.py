"""Core interfaces for the advanced curses replacement."""
from __future__ import annotations

import curses
import contextlib
from dataclasses import dataclass
from typing import TYPE_CHECKING, Callable, Optional, Tuple

from .events import EventLoop
from .theme import ThemeManager

if TYPE_CHECKING:
    from .events import KeyEvent


@dataclass
class Screen:
    """Represents the root screen managed by :class:`CursesApp`."""

    stdscr: "curses._CursesWindow"
    theme: ThemeManager

    def size(self) -> Tuple[int, int]:
        """Return the current height and width of the terminal."""
        height, width = self.stdscr.getmaxyx()
        return int(height), int(width)

    def clear(self) -> None:
        self.stdscr.erase()

    def refresh(self) -> None:
        self.stdscr.noutrefresh()
        curses.doupdate()

    def write(self, y: int, x: int, text: str, *, attr: Optional[int] = None) -> None:
        """Write *text* at the given position."""
        if attr is None:
            attr = self.theme.active_attribute
        try:
            self.stdscr.addnstr(y, x, text, len(text), attr)
        except curses.error:
            # Ignore drawing errors when text overflows, similar to curses behavior.
            pass


class Window:
    """A higher level window abstraction with optional borders and title."""

    def __init__(
        self,
        screen: Screen,
        y: int,
        x: int,
        height: int,
        width: int,
        *,
        border: bool = False,
        title: Optional[str] = None,
    ) -> None:
        self.screen = screen
        self.y = y
        self.x = x
        self._height = height
        self._width = width
        self.border = border
        self.title = title
        self._window = curses.newwin(height, width, y, x)

    @property
    def inner(self) -> "curses._CursesWindow":
        """Return the underlying curses window."""
        return self._window

    def draw(self) -> None:
        if self.border:
            self._window.box()
        if self.title:
            title = f" {self.title} "
            self._window.addnstr(0, 2, title, max(0, self._width - 4))

    def refresh(self) -> None:
        self._window.noutrefresh()

    def clear(self) -> None:
        self._window.erase()

    def write(self, y: int, x: int, text: str, *, attr: Optional[int] = None) -> None:
        if attr is None:
            attr = self.screen.theme.active_attribute
        try:
            self._window.addnstr(y, x, text, len(text), attr)
        except curses.error:
            pass

    @property
    def height(self) -> int:
        return self._height

    @property
    def width(self) -> int:
        return self._width


class CursesApp:
    """Application wrapper that manages curses setup, teardown and event loop."""

    def __init__(
        self,
        *,
        enable_mouse: bool = True,
        theme: Optional[ThemeManager] = None,
    ) -> None:
        self.enable_mouse = enable_mouse
        self.loop = EventLoop()
        self.theme = theme or ThemeManager.default_theme()
        self._screen: Optional[Screen] = None
        self._cleanup_actions: list[Callable[[], None]] = []

    def __enter__(self) -> "CursesApp":
        self.start()
        return self

    def __exit__(self, exc_type, exc, tb) -> None:  # type: ignore[override]
        self.stop()

    def start(self) -> Screen:
        """Initialize curses and return the :class:`Screen` object."""
        stdscr = curses.initscr()
        curses.noecho()
        curses.cbreak()
        stdscr.keypad(True)
        if self.enable_mouse:
            curses.mousemask(curses.ALL_MOUSE_EVENTS | curses.REPORT_MOUSE_POSITION)
        if curses.has_colors():
            curses.start_color()
            self.theme.apply()
        curses.curs_set(0)
        screen = Screen(stdscr=stdscr, theme=self.theme)
        self._screen = screen
        return screen

    def stop(self) -> None:
        """Tear down curses and cancel the event loop."""
        self.loop.close()
        self.loop = EventLoop()
        if self._screen is None:
            return
        stdscr = self._screen.stdscr
        stdscr.keypad(False)
        with contextlib.suppress(curses.error):
            curses.curs_set(1)
        curses.echo()
        curses.nocbreak()
        curses.endwin()
        for cleanup in reversed(self._cleanup_actions):
            cleanup()
        self._cleanup_actions.clear()
        self._screen = None

    @property
    def screen(self) -> Screen:
        if self._screen is None:
            raise RuntimeError("CursesApp.start() must be called first")
        return self._screen

    def add_cleanup(self, func: Callable[[], None]) -> None:
        self._cleanup_actions.append(func)

    def run(
        self,
        render: Callable[[Screen], None],
        *,
        fps: int = 30,
        handle_input: Optional[Callable[["KeyEvent"], None]] = None,
    ) -> None:
        """Run the application event loop.

        Parameters
        ----------
        render:
            Callable that receives the :class:`Screen` and performs drawing.
        fps:
            Number of redraws per second. Defaults to 30.
        handle_input:
            Optional callback to receive :class:`advanced_curses.events.KeyEvent` objects.
        """

        screen = self.screen
        interval = 1 / max(1, fps)

        def tick(_: float) -> None:
            screen.clear()
            render(screen)
            screen.refresh()

        self.loop.call_repeating(interval, tick)
        if handle_input is not None:
            self.loop.set_event_handler(handle_input)
        self.loop.run(screen.stdscr)

