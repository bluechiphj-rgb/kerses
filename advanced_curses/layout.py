"""Simple layout helpers for widgets."""
from __future__ import annotations

from dataclasses import dataclass
from typing import Tuple


@dataclass
class Rect:
    y: int
    x: int
    height: int
    width: int


class BaseLayout:
    """Common base class for layouts."""

    def __init__(self) -> None:
        self.children: list[Tuple[object, float]] = []

    def add(self, widget: object, weight: float = 1.0) -> None:
        self.children.append((widget, weight))

    def compute(self, rect: Rect) -> list[Tuple[object, Rect]]:
        raise NotImplementedError


class HLayout(BaseLayout):
    """Horizontal layout that divides width between children."""

    def compute(self, rect: Rect) -> list[Tuple[object, Rect]]:
        total_weight = sum(weight for _, weight in self.children)
        width_unit = rect.width / total_weight if total_weight else rect.width
        x = rect.x
        result = []
        for widget, weight in self.children:
            width = int(round(width_unit * weight))
            result.append((widget, Rect(rect.y, x, rect.height, width)))
            x += width
        return result


class VLayout(BaseLayout):
    """Vertical layout that divides height between children."""

    def compute(self, rect: Rect) -> list[Tuple[object, Rect]]:
        total_weight = sum(weight for _, weight in self.children)
        height_unit = rect.height / total_weight if total_weight else rect.height
        y = rect.y
        result = []
        for widget, weight in self.children:
            height = int(round(height_unit * weight))
            result.append((widget, Rect(y, rect.x, height, rect.width)))
            y += height
        return result


class GridLayout(BaseLayout):
    """2D grid layout for arranging widgets in rows and columns."""

    def __init__(self, rows: int, cols: int) -> None:
        super().__init__()
        self.rows = rows
        self.cols = cols

    def add(self, widget: object, row: int, col: int) -> None:  # type: ignore[override]
        index = row * self.cols + col
        while len(self.children) <= index:
            self.children.append((None, 0))
        self.children[index] = (widget, 1)

    def compute(self, rect: Rect) -> list[Tuple[object, Rect]]:
        cell_height = rect.height // self.rows
        cell_width = rect.width // self.cols
        result: list[Tuple[object, Rect]] = []
        for index, (widget, _) in enumerate(self.children):
            if widget is None:
                continue
            row, col = divmod(index, self.cols)
            cell_rect = Rect(
                rect.y + row * cell_height,
                rect.x + col * cell_width,
                cell_height,
                cell_width,
            )
            result.append((widget, cell_rect))
        return result

