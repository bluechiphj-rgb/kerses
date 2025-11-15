"""Simple rhythm game implemented with pygame.

Controls
========
Press the D F J K keys when the notes reach the judgement line.
Hit notes with good timing to build combo and score points.

Requirements
============
* Python 3.9+
* pygame 2.x

Install dependencies:
    pip install pygame

Run the game:
    python rhythm_game.py
"""
from __future__ import annotations

import math
import random
from array import array
from dataclasses import dataclass
from typing import Dict, List, Tuple

import pygame

# Screen configuration
WINDOW_WIDTH = 480
WINDOW_HEIGHT = 720
FPS = 60

# Play field configuration
LANE_COUNT = 4
LANE_WIDTH = WINDOW_WIDTH // (LANE_COUNT + 2)
LANE_GAP = LANE_WIDTH // 4
JUDGEMENT_LINE_Y = WINDOW_HEIGHT - 160
NOTE_SPEED = 420  # pixels per second
NOTE_COLOR = (234, 76, 137)
LANE_COLORS = [
    (40, 120, 200),
    (40, 160, 120),
    (200, 140, 60),
    (150, 80, 200),
]
BACKGROUND_COLOR = (16, 18, 30)
TEXT_COLOR = (230, 230, 230)

# Timing windows in milliseconds
PERFECT_WINDOW = 60
GREAT_WINDOW = 90
GOOD_WINDOW = 140
BAD_WINDOW = 200

KEY_BINDINGS = {
    pygame.K_d: 0,
    pygame.K_f: 1,
    pygame.K_j: 2,
    pygame.K_k: 3,
}


@dataclass
class Note:
    lane: int
    time_ms: int
    y: float = -120.0
    active: bool = True
    judged: bool = False
    judgement: str | None = None

    def update_position(self, elapsed_ms: int) -> bool:
        if not self.active:
            return False
        delta_time = elapsed_ms / 1000.0
        self.y += NOTE_SPEED * delta_time
        if self.y > WINDOW_HEIGHT + 120:
            self.active = False
            self.judged = True
            self.judgement = "Miss"
            return True
        return False


class Chart:
    """Defines the arrangement of notes for the rhythm game."""

    def __init__(self, bpm: int = 120, measures: int = 16, seed: int | None = None) -> None:
        self.bpm = bpm
        self.measures = measures
        self.seed = seed if seed is not None else random.randrange(1_000_000)
        self.notes = self.generate_chart()

    def beat_duration(self) -> float:
        return 60000.0 / self.bpm

    def generate_chart(self) -> List[Note]:
        notes: List[Note] = []
        beat_ms = self.beat_duration()
        # Four beats per measure, simple 4/4 rhythm with occasional triplets.
        time_ms = 0
        rng = random.Random(self.seed)
        for _ in range(self.measures):
            for _ in range(4):
                lane = rng.randrange(LANE_COUNT)
                notes.append(Note(lane=lane, time_ms=int(time_ms)))
                # Add a chance for grace notes
                if rng.random() < 0.2:
                    lane = rng.randrange(LANE_COUNT)
                    notes.append(Note(lane=lane, time_ms=int(time_ms + beat_ms * 0.5)))
                if rng.random() < 0.1:
                    lane = rng.randrange(LANE_COUNT)
                    notes.append(Note(lane=lane, time_ms=int(time_ms + beat_ms * (2 / 3))))
                time_ms += beat_ms
        return sorted(notes, key=lambda note: note.time_ms)


class RhythmGame:
    def __init__(self) -> None:
        pygame.init()
        pygame.display.set_caption("PyRhythm")
        self.screen = pygame.display.set_mode((WINDOW_WIDTH, WINDOW_HEIGHT))
        self.clock = pygame.time.Clock()
        self.font_small = pygame.font.SysFont("arial", 24)
        self.font_large = pygame.font.SysFont("arial", 48, bold=True)
        self.chart: Chart | None = None
        self.start_time = 0
        self.active_notes: List[Note] = []
        self.chart_index = 0
        self.combo = 0
        self.score = 0
        self.judgement_feedback: List[Tuple[str, int, float]] = []
        self.playing = False
        self.key_states: Dict[int, bool] = {lane: False for lane in range(LANE_COUNT)}
        self.sounds = self.load_sounds()
        self.judgement_counts: Dict[str, int] = {
            "Perfect": 0,
            "Great": 0,
            "Good": 0,
            "Bad": 0,
            "Miss": 0,
        }
        self.max_combo = 0
        self.total_notes = 0
        self.false_inputs = 0

    def load_sounds(self) -> Dict[str, pygame.mixer.Sound]:
        sounds: Dict[str, pygame.mixer.Sound] = {}
        try:
            pygame.mixer.init()
            sample_rate = 44100
            duration = 0.1
            volume = 0.4
            tone_cache: Dict[int, pygame.mixer.Sound] = {}
            for pitch, freq in {
                0: 329.63,  # E4
                1: 392.00,  # G4
                2: 523.25,  # C5
                3: 659.25,  # E5
            }.items():
                tone_cache[pitch] = self.generate_tone(freq, duration, sample_rate, volume)
            sounds["hit"] = tone_cache[0]
            sounds["hit_alt"] = tone_cache[2]
        except pygame.error:
            # Audio may fail to initialize in restricted environments; continue silently.
            pass
        return sounds

    @staticmethod
    def generate_tone(frequency: float, duration: float, sample_rate: int, volume: float) -> pygame.mixer.Sound:
        sample_count = int(sample_rate * duration)
        buffer = [
            int(volume * 32767 * math.sin(2 * math.pi * frequency * (i / sample_rate)))
            for i in range(sample_count)
        ]
        stereo_buffer = []
        for value in buffer:
            stereo_buffer.extend((value, value))
        sound = pygame.mixer.Sound(buffer=array("h", stereo_buffer))
        return sound

    def reset(self) -> None:
        self.chart = Chart(seed=random.randrange(1_000_000))
        self.total_notes = len(self.chart.notes)
        self.start_time = pygame.time.get_ticks()
        self.active_notes = []
        self.chart_index = 0
        self.combo = 0
        self.max_combo = 0
        self.score = 0
        self.judgement_feedback.clear()
        self.judgement_counts = {
            "Perfect": 0,
            "Great": 0,
            "Good": 0,
            "Bad": 0,
            "Miss": 0,
        }
        self.false_inputs = 0
        for lane in self.key_states:
            self.key_states[lane] = False
        for note in self.chart.notes:
            note.y = -120.0
            note.active = True
            note.judged = False
            note.judgement = None
        self.playing = True

    def spawn_notes(self, current_time: int) -> None:
        if self.chart is None:
            return
        spawn_ahead = 2000  # milliseconds ahead of play time
        while (
            self.chart_index < len(self.chart.notes)
            and self.chart.notes[self.chart_index].time_ms - spawn_ahead <= current_time
        ):
            self.active_notes.append(self.chart.notes[self.chart_index])
            self.chart_index += 1

    def process_input(self) -> None:
        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                self.playing = False
                pygame.quit()
                raise SystemExit
            if event.type == pygame.KEYDOWN:
                if event.key == pygame.K_ESCAPE:
                    self.reset()
                if not self.playing and event.key == pygame.K_SPACE:
                    self.reset()
                if self.playing and event.key in KEY_BINDINGS:
                    lane = KEY_BINDINGS[event.key]
                    self.key_states[lane] = True
                    self.evaluate_hit(lane)
            if event.type == pygame.KEYUP and event.key in KEY_BINDINGS:
                lane = KEY_BINDINGS[event.key]
                self.key_states[lane] = False

    def evaluate_hit(self, lane: int) -> None:
        current_time = pygame.time.get_ticks() - self.start_time
        target_note: Note | None = None
        min_delta = float("inf")
        for note in self.active_notes:
            if not note.judged and note.lane == lane:
                delta = abs(note.time_ms - current_time)
                if delta < min_delta:
                    min_delta = delta
                    target_note = note
        if target_note is None:
            self.register_miss()
            return

        judgement = self.determine_judgement(min_delta)
        if judgement is None:
            self.register_miss(note=target_note)
            return

        target_note.judged = True
        target_note.active = False
        target_note.judgement = judgement
        if judgement == "Bad":
            self.combo = 0
        else:
            self.combo += 1
        self.max_combo = max(self.max_combo, self.combo)
        self.score += self.judgement_score(judgement)
        self.judgement_counts[judgement] += 1
        sound_key = "hit" if lane % 2 == 0 else "hit_alt"
        if sound_key in self.sounds:
            self.sounds[sound_key].play()
        self.judgement_feedback.append((judgement, pygame.time.get_ticks(), float(lane)))

    def determine_judgement(self, delta: float) -> str | None:
        if delta <= PERFECT_WINDOW:
            return "Perfect"
        if delta <= GREAT_WINDOW:
            return "Great"
        if delta <= GOOD_WINDOW:
            return "Good"
        if delta <= BAD_WINDOW:
            return "Bad"
        return None

    def register_miss(self, note: Note | None = None) -> None:
        self.combo = 0
        lane = float(note.lane) if note is not None else -1.0
        self.judgement_feedback.append(("Miss", pygame.time.get_ticks(), lane))
        if note is not None:
            self.judgement_counts["Miss"] += 1
            note.judged = True
            note.active = False
            note.judgement = "Miss"
        else:
            self.false_inputs += 1

    @staticmethod
    def judgement_score(judgement: str) -> int:
        return {
            "Perfect": 1000,
            "Great": 700,
            "Good": 500,
            "Bad": 100,
            "Miss": 0,
        }.get(judgement, 0)

    def update_notes(self, elapsed_ms: int) -> None:
        for note in self.active_notes:
            if note.active:
                missed = note.update_position(elapsed_ms)
                if missed:
                    self.register_miss(note)
                elif note.judged and note.judgement == "Miss":
                    self.combo = 0
        self.active_notes = [note for note in self.active_notes if not note.judged]

    def update_feedback(self) -> None:
        now = pygame.time.get_ticks()
        self.judgement_feedback = [
            (text, timestamp, lane)
            for text, timestamp, lane in self.judgement_feedback
            if now - timestamp < 600
        ]

    def draw(self) -> None:
        self.screen.fill(BACKGROUND_COLOR)
        self.draw_lanes()
        self.draw_notes()
        self.draw_judgement_line()
        self.draw_feedback()
        self.draw_hud()
        pygame.display.flip()

    def draw_lanes(self) -> None:
        lane_total_width = LANE_WIDTH * LANE_COUNT + LANE_GAP * (LANE_COUNT - 1)
        left_margin = (WINDOW_WIDTH - lane_total_width) // 2
        for lane in range(LANE_COUNT):
            x = left_margin + lane * (LANE_WIDTH + LANE_GAP)
            color = LANE_COLORS[lane % len(LANE_COLORS)]
            pygame.draw.rect(
                self.screen,
                color,
                (x, 0, LANE_WIDTH, WINDOW_HEIGHT),
                width=4,
                border_radius=8,
            )
            if self.key_states[lane]:
                overlay = pygame.Surface((LANE_WIDTH - 8, WINDOW_HEIGHT))
                overlay.fill((255, 255, 255))
                overlay.set_alpha(40)
                self.screen.blit(overlay, (x + 4, 0))

    def draw_notes(self) -> None:
        lane_total_width = LANE_WIDTH * LANE_COUNT + LANE_GAP * (LANE_COUNT - 1)
        left_margin = (WINDOW_WIDTH - lane_total_width) // 2
        for note in self.active_notes:
            if not note.active:
                continue
            x = left_margin + note.lane * (LANE_WIDTH + LANE_GAP)
            pygame.draw.rect(
                self.screen,
                NOTE_COLOR,
                (x + 6, note.y, LANE_WIDTH - 12, 28),
                border_radius=6,
            )

    def draw_judgement_line(self) -> None:
        pygame.draw.rect(
            self.screen,
            (240, 240, 240),
            (40, JUDGEMENT_LINE_Y, WINDOW_WIDTH - 80, 6),
            border_radius=4,
        )

    def draw_feedback(self) -> None:
        now = pygame.time.get_ticks()
        for text, timestamp, lane in self.judgement_feedback:
            alpha = 255 - int((now - timestamp) / 600 * 255)
            alpha = max(alpha, 0)
            label = self.font_large.render(text, True, TEXT_COLOR)
            label.set_alpha(alpha)
            x = WINDOW_WIDTH // 2 - label.get_width() // 2
            y = JUDGEMENT_LINE_Y - 100
            if lane >= 0:
                lane_total_width = LANE_WIDTH * LANE_COUNT + LANE_GAP * (LANE_COUNT - 1)
                left_margin = (WINDOW_WIDTH - lane_total_width) // 2
                x = int(left_margin + lane * (LANE_WIDTH + LANE_GAP) + LANE_WIDTH // 2 - label.get_width() // 2)
                y -= 20
            self.screen.blit(label, (x, y))

    def draw_hud(self) -> None:
        combo_label = self.font_large.render(f"Combo: {self.combo}", True, TEXT_COLOR)
        score_label = self.font_small.render(f"Score: {self.score}", True, TEXT_COLOR)
        instructions = self.font_small.render("Press ESC to restart", True, TEXT_COLOR)
        self.screen.blit(combo_label, (40, 40))
        self.screen.blit(score_label, (40, 100))
        self.screen.blit(instructions, (40, WINDOW_HEIGHT - 40))
        if not self.playing and self.chart is not None:
            title = self.font_large.render("Press Space to Start", True, TEXT_COLOR)
            self.screen.blit(
                title,
                (
                    WINDOW_WIDTH // 2 - title.get_width() // 2,
                    WINDOW_HEIGHT // 2 - title.get_height() // 2,
                ),
            )
            summary_lines = [
                f"Final Score: {self.score}",
                f"Max Combo: {self.max_combo}",
            ]
            for name in ("Perfect", "Great", "Good", "Bad", "Miss"):
                summary_lines.append(f"{name}: {self.judgement_counts[name]}")
            summary_lines.append(f"False Hits: {self.false_inputs}")
            if self.total_notes:
                hit_notes = self.total_notes - self.judgement_counts["Miss"]
                accuracy = 100 * hit_notes / self.total_notes
                summary_lines.append(f"Accuracy: {accuracy:0.1f}%")
            summary_y = WINDOW_HEIGHT // 2 + 60
            for index, text in enumerate(summary_lines):
                label = self.font_small.render(text, True, TEXT_COLOR)
                self.screen.blit(
                    label,
                    (
                        WINDOW_WIDTH // 2 - label.get_width() // 2,
                        summary_y + index * 28,
                    ),
                )

    def update_game(self) -> None:
        elapsed_ms = self.clock.get_time()
        self.update_feedback()
        if not self.playing or self.chart is None:
            return
        current_time = pygame.time.get_ticks() - self.start_time
        self.spawn_notes(current_time)
        self.update_notes(elapsed_ms)
        if self.chart_index >= len(self.chart.notes) and not any(note.active for note in self.active_notes):
            self.playing = False

    def run(self) -> None:
        self.reset()
        while True:
            self.process_input()
            self.update_game()
            self.draw()
            self.clock.tick(FPS)


if __name__ == "__main__":
    game = RhythmGame()
    game.run()
