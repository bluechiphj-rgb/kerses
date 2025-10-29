"""Pure Python example that trains a tiny neural network.

The script demonstrates how to train a simple two-layer neural network on a
synthetic XOR-like dataset without relying on external machine learning
libraries. It is intentionally lightweight so it can run in most restricted
Python environments.

Usage:
    python train_ai.py --epochs 4000 --hidden-size 4 --learning-rate 0.3
"""

from __future__ import annotations

import argparse
import math
import random
from dataclasses import dataclass
from typing import Iterable, List, Sequence, Tuple


Vector = List[float]
Matrix = List[Vector]


@dataclass
class TrainingHistory:
    losses: List[float]

    def __iter__(self) -> Iterable[float]:
        return iter(self.losses)


def sigmoid(x: float) -> float:
    # Prevent overflow when x is a large negative value.
    if x >= 0:
        z = math.exp(-x)
        return 1.0 / (1.0 + z)
    z = math.exp(x)
    return z / (1.0 + z)


class TwoLayerNN:
    """A very small neural network with one hidden layer."""

    def __init__(
        self,
        input_dim: int,
        hidden_dim: int,
        learning_rate: float = 0.1,
        seed: int | None = None,
    ) -> None:
        rng = random.Random(seed)
        self.learning_rate = learning_rate
        self.W1: Matrix = [
            [rng.uniform(-1.0, 1.0) for _ in range(input_dim)] for _ in range(hidden_dim)
        ]
        self.b1: Vector = [0.0 for _ in range(hidden_dim)]
        self.W2: Vector = [rng.uniform(-1.0, 1.0) for _ in range(hidden_dim)]
        self.b2: float = 0.0

    def forward(self, x: Sequence[float]) -> Tuple[Vector, float]:
        hidden: Vector = []
        for weights, bias in zip(self.W1, self.b1):
            activation = sum(w * xi for w, xi in zip(weights, x)) + bias
            hidden.append(math.tanh(activation))
        output_activation = sum(w * h for w, h in zip(self.W2, hidden)) + self.b2
        return hidden, sigmoid(output_activation)

    def compute_loss(self, predictions: Sequence[float], targets: Sequence[int]) -> float:
        eps = 1e-12
        total = 0.0
        for pred, target in zip(predictions, targets):
            pred = min(max(pred, eps), 1 - eps)
            total += -(target * math.log(pred) + (1 - target) * math.log(1 - pred))
        return total / len(predictions)

    def train(
        self,
        X: Sequence[Sequence[float]],
        y: Sequence[int],
        epochs: int = 2000,
        verbose_every: int | None = 200,
    ) -> TrainingHistory:
        losses: List[float] = []
        n_samples = len(X)
        for epoch in range(1, epochs + 1):
            predictions: List[float] = []
            grad_W2 = [0.0 for _ in self.W2]
            grad_b2 = 0.0
            grad_W1 = [[0.0 for _ in weights] for weights in self.W1]
            grad_b1 = [0.0 for _ in self.b1]

            for sample, target in zip(X, y):
                hidden, output = self.forward(sample)
                predictions.append(output)
                error = output - target

                for h in range(len(self.W2)):
                    grad_W2[h] += error * hidden[h]
                grad_b2 += error

                for h, (weights, bias) in enumerate(zip(self.W1, self.b1)):
                    hidden_derivative = (1 - hidden[h] ** 2) * self.W2[h] * error
                    grad_b1[h] += hidden_derivative
                    for i in range(len(weights)):
                        grad_W1[h][i] += hidden_derivative * sample[i]

            loss = self.compute_loss(predictions, y)
            losses.append(loss)

            lr = self.learning_rate / n_samples
            for h in range(len(self.W2)):
                self.W2[h] -= lr * grad_W2[h]
            self.b2 -= lr * grad_b2

            for h in range(len(self.W1)):
                for i in range(len(self.W1[h])):
                    self.W1[h][i] -= lr * grad_W1[h][i]
                self.b1[h] -= lr * grad_b1[h]

            if verbose_every and epoch % verbose_every == 0:
                print(f"Epoch {epoch:4d} | Loss: {loss:.4f}")
        return TrainingHistory(losses)

    def predict(self, X: Sequence[Sequence[float]]) -> List[int]:
        return [int(self.forward(sample)[1] >= 0.5) for sample in X]

    def evaluate(self, X: Sequence[Sequence[float]], y: Sequence[int]) -> float:
        predictions = self.predict(X)
        matches = sum(int(pred == target) for pred, target in zip(predictions, y))
        return matches / len(y)


def generate_xor_dataset(
    n_samples: int,
    noise: float = 0.1,
    seed: int | None = None,
) -> Tuple[List[Vector], List[int]]:
    rng = random.Random(seed)
    X: List[Vector] = []
    y: List[int] = []
    for _ in range(n_samples):
        x1 = rng.uniform(-1.0, 1.0)
        x2 = rng.uniform(-1.0, 1.0)
        if noise > 0:
            x1 += rng.gauss(0.0, noise)
            x2 += rng.gauss(0.0, noise)
        X.append([x1, x2])
        y.append(int(x1 * x2 < 0))
    return X, y


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train a simple neural network on a synthetic XOR dataset.")
    parser.add_argument("--epochs", type=int, default=3000, help="Number of training epochs (default: 3000)")
    parser.add_argument("--learning-rate", type=float, default=0.3, help="Gradient descent learning rate (default: 0.3)")
    parser.add_argument("--hidden-size", type=int, default=5, help="Number of hidden units (default: 5)")
    parser.add_argument("--samples", type=int, default=400, help="Number of synthetic samples to generate (default: 400)")
    parser.add_argument("--noise", type=float, default=0.15, help="Standard deviation of Gaussian noise (default: 0.15)")
    parser.add_argument("--seed", type=int, default=7, help="Random seed for reproducibility (default: 7)")
    parser.add_argument(
        "--verbose-every",
        type=int,
        default=300,
        help="Print loss every N epochs (set to 0 to disable, default: 300)",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    X, y = generate_xor_dataset(args.samples, noise=args.noise, seed=args.seed)

    split_idx = int(0.8 * len(X))
    X_train, X_test = X[:split_idx], X[split_idx:]
    y_train, y_test = y[:split_idx], y[split_idx:]

    model = TwoLayerNN(
        input_dim=2,
        hidden_dim=args.hidden_size,
        learning_rate=args.learning_rate,
        seed=args.seed,
    )

    history = model.train(
        X_train,
        y_train,
        epochs=args.epochs,
        verbose_every=args.verbose_every if args.verbose_every > 0 else None,
    )

    train_accuracy = model.evaluate(X_train, y_train)
    test_accuracy = model.evaluate(X_test, y_test)

    print("Training complete!")
    print(f"Final training loss: {history.losses[-1]:.4f}")
    print(f"Training accuracy: {train_accuracy * 100:.2f}%")
    print(f"Test accuracy: {test_accuracy * 100:.2f}%")


if __name__ == "__main__":
    main()
