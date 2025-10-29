"""Event loop and keyboard handling utilities."""
from __future__ import annotations

import contextlib
import curses
import selectors
import time
from dataclasses import dataclass
from typing import Callable, Optional


@dataclass
class KeyEvent:
    """Keyboard event information returned by :class:`EventLoop`."""

    key: int
    is_mouse: bool = False
    mouse_state: Optional[curses._MouseEvent] = None


@dataclass
class TimerHandle:
    """Handle that can be used to cancel a scheduled callback."""

    callback: Callable[[float], None]
    when: float
    repeat: Optional[float] = None
    cancelled: bool = False

    def cancel(self) -> None:
        self.cancelled = True


class EventLoop:
    """Simple selector-based event loop compatible with curses."""

    def __init__(self) -> None:
        self._selector = selectors.DefaultSelector()
        self._timers: list[TimerHandle] = []
        self._running = False
        self._handler: Optional[Callable[[KeyEvent], None]] = None

    def call_later(self, delay: float, callback: Callable[[float], None]) -> TimerHandle:
        handle = TimerHandle(callback=callback, when=time.monotonic() + delay)
        self._timers.append(handle)
        return handle

    def call_repeating(self, interval: float, callback: Callable[[float], None]) -> TimerHandle:
        handle = TimerHandle(callback=callback, when=time.monotonic() + interval, repeat=interval)
        self._timers.append(handle)
        return handle

    def close(self) -> None:
        self._running = False
        self._selector.close()
        self._timers.clear()

    def set_event_handler(self, handler: Callable[[KeyEvent], None]) -> None:
        """Register a callable invoked for every input event."""

        self._handler = handler

    def run(self, stdscr: "curses._CursesWindow") -> None:
        """Run the loop until :meth:`close` is called."""

        self._running = True
        fd = stdscr.getch.__self__.fileno()  # type: ignore[attr-defined]
        self._selector.register(fd, selectors.EVENT_READ)

        try:
            while self._running:
                now = time.monotonic()
                self._run_timers(now)
                timeout = self._compute_timeout(now)
                events = self._selector.select(timeout)
                if not self._running:
                    break
                if events:
                    self._process_input(stdscr)
        finally:
            with contextlib.suppress(Exception):
                self._selector.unregister(fd)

    def _compute_timeout(self, now: float) -> Optional[float]:
        if not self._timers:
            return None
        next_timer = min(self._timers, key=lambda t: t.when)
        delay = max(0.0, next_timer.when - now)
        return delay

    def _run_timers(self, now: float) -> None:
        for timer in list(self._timers):
            if timer.cancelled:
                self._timers.remove(timer)
                continue
            if now >= timer.when:
                timer.callback(now)
                if timer.repeat and not timer.cancelled:
                    timer.when = now + timer.repeat
                else:
                    self._timers.remove(timer)

    def _process_input(self, stdscr: "curses._CursesWindow") -> None:
        stdscr.nodelay(True)
        try:
            while True:
                key = stdscr.getch()
                if key == -1:
                    break
                if key == curses.KEY_MOUSE:
                    try:
                        event = curses.getmouse()
                    except curses.error:
                        continue
                    self.handle_event(KeyEvent(key=key, is_mouse=True, mouse_state=event))
                else:
                    self.handle_event(KeyEvent(key=key))
        finally:
            stdscr.nodelay(False)

    def handle_event(self, event: KeyEvent) -> None:
        """Override in subclasses to handle keyboard input."""
        if self._handler is not None:
            self._handler(event)

    def wait_for_key(self, stdscr: "curses._CursesWindow") -> KeyEvent:
        """Blocking helper to wait for the next keyboard event."""

        key = stdscr.getch()
        if key == curses.KEY_MOUSE:
            try:
                event = curses.getmouse()
            except curses.error:
                return KeyEvent(key=key, is_mouse=True)
            return KeyEvent(key=key, is_mouse=True, mouse_state=event)
        return KeyEvent(key=key)

