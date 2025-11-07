"""Color theme management for :mod:`advanced_curses`."""
from __future__ import annotations

import curses
from dataclasses import dataclass, field
from typing import Dict, Tuple


@dataclass
class ColorTheme:
    """Represents color pairs and attributes for the application."""

    pairs: Dict[int, Tuple[int, int]] = field(default_factory=dict)
    active_attribute: int = curses.A_NORMAL

    def register_pair(self, pair_id: int, fg: int, bg: int) -> None:
        self.pairs[pair_id] = (fg, bg)


class ThemeManager:
    """Manage color themes and provide helpers for attributes."""

    def __init__(self, theme: ColorTheme) -> None:
        self.theme = theme
        self.active_attribute = theme.active_attribute

    @classmethod
    def default_theme(cls) -> "ThemeManager":
        theme = ColorTheme()
        theme.register_pair(1, curses.COLOR_WHITE, curses.COLOR_BLUE)
        theme.active_attribute = curses.color_pair(1)
        return cls(theme)

    def apply(self) -> None:
        for pair_id, (fg, bg) in self.theme.pairs.items():
            curses.init_pair(pair_id, fg, bg)

    def attribute(self, pair_id: int, *, bold: bool = False) -> int:
        attr = curses.color_pair(pair_id)
        if bold:
            attr |= curses.A_BOLD
        return attr

