"""High-level curses replacement with additional features."""

from .core import CursesApp, Screen, Window
from .layout import HLayout, VLayout, GridLayout, Rect
from .widgets import Widget, Label, TextInput, ProgressBar, WidgetManager
from .events import EventLoop, KeyEvent, TimerHandle
from .theme import ColorTheme, ThemeManager

__version__ = "0.1.0"

__all__ = [
    "CursesApp",
    "Screen",
    "Window",
    "HLayout",
    "VLayout",
    "GridLayout",
    "Rect",
    "Widget",
    "Label",
    "TextInput",
    "ProgressBar",
    "WidgetManager",
    "EventLoop",
    "KeyEvent",
    "TimerHandle",
    "ColorTheme",
    "ThemeManager",
    "__version__",
]
