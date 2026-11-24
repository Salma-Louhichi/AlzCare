"""Synthetic training data for the care-risk model.

There is no labelled clinical dataset in this project, so the model is trained on data
generated from an explicit, documented process. The model can only learn the assumptions
written below; it is a demonstration of the end-to-end ML pipeline, NOT a clinically
validated predictor. See MODEL_CARD.md.

Generative process (per synthetic patient):
1. A hidden disease-progression value s ~ U(0, 1) (0 = early, 1 = advanced).
2. Age ~ N(76 + 8·s, 6), clipped to [60, 98].
3. Each care area loses autonomy as s grows, with area-specific timing taken from the
   usual ordering of functional loss (medication management early, feeding late):
   level = 3 - floor(clip(2.7·s + offset + N(0, 0.45), 0, 2.99)).
4. 75 % of patients have a previous assessment 1-12 months earlier; the score change
   since then is negative on average and larger for faster progression.
5. Label = "next assessment shows a decline of ≥ 2 points". Its log-odds rise with age,
   recent decline, medication dependence and the hidden progression value, plus noise.
   Because s is hidden, the label is only partially predictable from the features.
"""
from __future__ import annotations

import numpy as np

from app.features import FEATURE_NAMES, overall_score

AREA_OFFSETS = {"hygiene": 0.10, "medication": 0.30, "mobility": 0.0, "feeding": -0.35}


def _sigmoid(x: np.ndarray) -> np.ndarray:
    return 1.0 / (1.0 + np.exp(-x))


def generate(n: int = 6000, seed: int = 42) -> tuple[np.ndarray, np.ndarray]:
    """Return (X, y) with columns in FEATURE_NAMES order."""
    rng = np.random.default_rng(seed)
    s = rng.uniform(0, 1, n)
    age = np.clip(rng.normal(76 + 8 * s, 6), 60, 98).round()

    levels = {}
    for area, offset in AREA_OFFSETS.items():
        loss = np.clip(2.7 * s + offset + rng.normal(0, 0.45, n), 0, 2.99)
        levels[area] = 3 - np.floor(loss)

    score = np.array([
        overall_score(int(h), int(m), int(mo), int(f))
        for h, m, mo, f in zip(levels["hygiene"], levels["medication"], levels["mobility"], levels["feeding"])
    ])

    has_prev = rng.uniform(0, 1, n) < 0.75
    months = np.where(has_prev, rng.uniform(1, 12, n), 0.0).round(1)
    speed = 0.15 + 0.5 * s + rng.normal(0, 0.15, n)          # points lost per month, roughly
    change = -np.round(speed * months / 3 + rng.normal(0, 0.8, n))
    change = np.where(has_prev, np.clip(change, -9, 3), 0)

    monthly_decline = np.where(has_prev, np.maximum(-change, 0) / np.maximum(months, 1), 0)
    logit = (
        -1.1
        + 0.45 * (age - 78) / 5
        + 1.6 * monthly_decline
        + 0.7 * (levels["medication"] == 1)
        + 1.4 * (s - 0.5)
        - 0.10 * (score - 5.5)
        + rng.normal(0, 0.6, n)
    )
    y = (rng.uniform(0, 1, n) < _sigmoid(logit)).astype(int)

    X = np.column_stack([
        age, levels["hygiene"], levels["medication"], levels["mobility"], levels["feeding"],
        change, months, has_prev.astype(float),
    ]).astype(float)
    assert X.shape[1] == len(FEATURE_NAMES)
    return X, y
