"""Train a tiny neural language model without external dependencies.

This script builds a miniature neural network language model that learns to
predict the next word in a sentence generated from a handcrafted grammar. The
dataset is created on the fly so the script has no external data dependencies.

Usage:
    python train_ai.py --epochs 500 --hidden-size 64 --context-size 3
"""

from __future__ import annotations

import argparse
import random
from dataclasses import dataclass
from math import exp, log, tanh
from typing import Iterable, List, Sequence, Tuple


Vector = List[float]
Matrix = List[Vector]


@dataclass
class TrainingHistory:
    losses: List[float]

    def __iter__(self) -> Iterable[float]:
        return iter(self.losses)


def softmax(logits: Sequence[float]) -> List[float]:
    # Shift by max logit for numerical stability.
    max_logit = max(logits)
    exps = [exp(logit - max_logit) for logit in logits]
    total = sum(exps)
    return [value / total for value in exps]


def cross_entropy(probs: Sequence[float], target_index: int) -> float:
    eps = 1e-12
    prob = min(max(probs[target_index], eps), 1.0 - eps)
    return -log(prob)


class TwoLayerLanguageModel:
    """A very small neural language model with a single hidden layer."""

    def __init__(
        self,
        vocab_size: int,
        context_size: int,
        hidden_dim: int,
        learning_rate: float = 0.1,
        seed: int | None = None,
    ) -> None:
        rng = random.Random(seed)
        self.learning_rate = learning_rate
        input_dim = vocab_size * context_size
        self.vocab_size = vocab_size
        self.context_size = context_size
        self.W1: Matrix = [
            [rng.uniform(-0.5, 0.5) for _ in range(input_dim)] for _ in range(hidden_dim)
        ]
        self.b1: Vector = [0.0 for _ in range(hidden_dim)]
        self.W2: Matrix = [
            [rng.uniform(-0.5, 0.5) for _ in range(hidden_dim)] for _ in range(vocab_size)
        ]
        self.b2: Vector = [0.0 for _ in range(vocab_size)]

    def forward(self, x: Sequence[float]) -> Tuple[Vector, List[float]]:
        hidden: Vector = []
        for weights, bias in zip(self.W1, self.b1):
            activation = sum(w * xi for w, xi in zip(weights, x)) + bias
            hidden.append(tanh(activation))
        logits: List[float] = []
        for weights, bias in zip(self.W2, self.b2):
            logits.append(sum(w * h for w, h in zip(weights, hidden)) + bias)
        probs = softmax(logits)
        return hidden, probs

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
            total_loss = 0.0
            grad_W2 = [[0.0 for _ in weights] for weights in self.W2]
            grad_b2 = [0.0 for _ in self.b2]
            grad_W1 = [[0.0 for _ in weights] for weights in self.W1]
            grad_b1 = [0.0 for _ in self.b1]

            for sample, target in zip(X, y):
                hidden, probs = self.forward(sample)
                loss = cross_entropy(probs, target)
                total_loss += loss

                grad_logits = probs[:]
                grad_logits[target] -= 1.0

                for v in range(self.vocab_size):
                    for h in range(len(hidden)):
                        grad_W2[v][h] += grad_logits[v] * hidden[h]
                    grad_b2[v] += grad_logits[v]

                grad_hidden = [0.0 for _ in hidden]
                for h in range(len(hidden)):
                    for v in range(self.vocab_size):
                        grad_hidden[h] += self.W2[v][h] * grad_logits[v]
                    grad_hidden[h] *= 1 - hidden[h] ** 2

                for h in range(len(hidden)):
                    for i in range(len(sample)):
                        grad_W1[h][i] += grad_hidden[h] * sample[i]
                    grad_b1[h] += grad_hidden[h]

            avg_loss = total_loss / n_samples
            losses.append(avg_loss)

            lr = self.learning_rate / n_samples
            for v in range(self.vocab_size):
                for h in range(len(self.W2[v])):
                    self.W2[v][h] -= lr * grad_W2[v][h]
                self.b2[v] -= lr * grad_b2[v]

            for h in range(len(self.W1)):
                for i in range(len(self.W1[h])):
                    self.W1[h][i] -= lr * grad_W1[h][i]
                self.b1[h] -= lr * grad_b1[h]

            if verbose_every and epoch % verbose_every == 0:
                print(f"Epoch {epoch:4d} | Loss: {avg_loss:.4f}")
        return TrainingHistory(losses)

    def predict_next(self, sample: Sequence[float]) -> int:
        _, probs = self.forward(sample)
        best = max(range(len(probs)), key=lambda idx: probs[idx])
        return best

    def sample(
        self,
        stoi: dict[str, int],
        itos: dict[int, str],
        max_tokens: int = 15,
    ) -> List[str]:
        context = [stoi["<bos>"]] * self.context_size
        generated: List[str] = []
        for _ in range(max_tokens):
            encoded = encode_context(context, len(itos), self.context_size)
            next_token = self.predict_next(encoded)
            if itos[next_token] == "<eos>":
                break
            generated.append(itos[next_token])
            context = context[1:] + [next_token]
        return generated
def generate_corpus(size: int, seed: int | None = None) -> List[List[str]]:
    rng = random.Random(seed)
    subjects = ["소년", "소녀", "마법사", "용", "로봇"]
    verbs = ["만난다", "찾는다", "지킨다", "도와준다", "연습한다"]
    objects = ["친구", "보물", "마을", "비밀", "모험"]
    modifiers = ["용감한", "작은", "신비한", "은빛", "별빛"]
    locations = ["숲에서", "성에서", "하늘에서", "바다에서", "도시에서"]

    corpus: List[List[str]] = []
    for _ in range(size):
        sentence = [
            rng.choice(modifiers),
            rng.choice(subjects),
            rng.choice(verbs),
            rng.choice(modifiers),
            rng.choice(objects),
            rng.choice(locations),
        ]
        corpus.append(sentence)
    return corpus


def build_vocabulary(corpus: Sequence[Sequence[str]]) -> Tuple[dict[str, int], dict[int, str]]:
    vocab = {"<bos>", "<eos>"}
    for sentence in corpus:
        vocab.update(sentence)
    sorted_vocab = sorted(vocab)
    stoi = {token: idx for idx, token in enumerate(sorted_vocab)}
    itos = {idx: token for token, idx in stoi.items()}
    return stoi, itos


def encode_context(context: Sequence[int], vocab_size: int, context_size: int) -> List[float]:
    vector = [0.0 for _ in range(vocab_size * context_size)]
    for position, token_idx in enumerate(context):
        offset = position * vocab_size + token_idx
        vector[offset] = 1.0
    return vector


def build_training_data(
    corpus: Sequence[Sequence[str]],
    stoi: dict[str, int],
    context_size: int,
) -> Tuple[List[List[float]], List[int]]:
    X: List[List[float]] = []
    y: List[int] = []
    for sentence in corpus:
        context = [stoi["<bos>"]] * context_size
        for token in sentence + ["<eos>"]:
            token_idx = stoi[token]
            X.append(encode_context(context, len(stoi), context_size))
            y.append(token_idx)
            context = context[1:] + [token_idx]
    return X, y


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Train a miniature neural language model on a synthetic corpus."
    )
    parser.add_argument("--epochs", type=int, default=600, help="Number of training epochs (default: 600)")
    parser.add_argument(
        "--learning-rate",
        type=float,
        default=0.4,
        help="Gradient descent learning rate (default: 0.4)",
    )
    parser.add_argument(
        "--hidden-size", type=int, default=64, help="Number of hidden units (default: 64)"
    )
    parser.add_argument(
        "--context-size",
        type=int,
        default=3,
        help="Number of preceding tokens the model conditions on (default: 3)",
    )
    parser.add_argument(
        "--corpus-size",
        type=int,
        default=500,
        help="Number of synthetic sentences to generate for training (default: 500)",
    )
    parser.add_argument("--seed", type=int, default=7, help="Random seed for reproducibility (default: 7)")
    parser.add_argument(
        "--verbose-every",
        type=int,
        default=50,
        help="Print loss every N epochs (set to 0 to disable, default: 50)",
    )
    parser.add_argument(
        "--sample-length",
        type=int,
        default=12,
        help="Number of tokens to sample from the trained model (default: 12)",
    )
    parser.add_argument(
        "--history-out",
        type=str,
        default=None,
        help="Optional path to write a CSV log of epoch losses",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    corpus = generate_corpus(args.corpus_size, seed=args.seed)
    stoi, itos = build_vocabulary(corpus)
    X, y = build_training_data(corpus, stoi, args.context_size)

    model = TwoLayerLanguageModel(
        vocab_size=len(stoi),
        context_size=args.context_size,
        hidden_dim=args.hidden_size,
        learning_rate=args.learning_rate,
        seed=args.seed,
    )

    history = model.train(
        X,
        y,
        epochs=args.epochs,
        verbose_every=args.verbose_every if args.verbose_every > 0 else None,
    )

    print("Training complete!")
    print(f"Final training loss: {history.losses[-1]:.4f}")

    if args.history_out:
        with open(args.history_out, "w", encoding="utf-8") as history_file:
            history_file.write("epoch,loss\n")
            for epoch, loss in enumerate(history.losses, start=1):
                history_file.write(f"{epoch},{loss}\n")
        print(f"Saved training history to {args.history_out}")

    generated = model.sample(stoi, itos, max_tokens=args.sample_length)
    print("Generated sentence:", " ".join(generated))


if __name__ == "__main__":
    main()
