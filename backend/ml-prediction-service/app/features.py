"""Feature definitions shared by training and serving.

Keeping the feature order, the level encoding and the derivation from raw autonomy
assessments in one module guarantees that the model sees the same inputs in
production as during training.
"""
from __future__ import annotations

from dataclasses import dataclass
from datetime import date

import numpy as np

from app.schemas import AutonomyLevel, RiskFeatures

# Same encoding as identity-service (AutonomyAssessment#getLevelValue)
LEVEL_VALUE = {
    AutonomyLevel.INDEPENDENT: 3,
    AutonomyLevel.ASSISTED: 2,
    AutonomyLevel.DEPENDENT: 1,
}

# overall_score is NOT a model input: it is a deterministic function of the four levels,
# and including it makes per-feature explanations contradictory (collinearity).
FEATURE_NAMES: list[str] = [
    "age_years",
    "hygiene",
    "medication",
    "mobility",
    "feeding",
    "score_change",
    "months_since_previous",
    "has_previous",
]


def overall_score(hygiene: int, medication: int, mobility: int, feeding: int) -> int:
    """Map the 4..12 sum of level values to the 1..10 scale used by identity-service."""
    total = hygiene + medication + mobility + feeding
    return 1 + (total - 4) * 9 // 8


def to_vector(features: RiskFeatures) -> np.ndarray:
    """Convert validated API features to the model's input row (shape 1 x n_features)."""
    h = LEVEL_VALUE[features.hygiene_level]
    m = LEVEL_VALUE[features.medication_level]
    mo = LEVEL_VALUE[features.mobility_level]
    f = LEVEL_VALUE[features.feeding_level]
    row = [
        features.age_years,
        h,
        m,
        mo,
        f,
        features.score_change if features.has_previous else 0,
        features.months_since_previous if features.has_previous else 0.0,
        1.0 if features.has_previous else 0.0,
    ]
    return np.asarray([row], dtype=float)


@dataclass(frozen=True)
class DerivedFeatures:
    features: RiskFeatures
    latest_assessment_date: date
    assessments_used: int


def age_on(birth: date, on: date) -> float:
    years = on.year - birth.year - ((on.month, on.day) < (birth.month, birth.day))
    return float(years)


def _assessment_date(a: dict) -> date:
    raw = a.get("assessmentDate") or (a.get("createdAt") or "")[:10]
    return date.fromisoformat(raw)


def _score(a: dict) -> int:
    if a.get("overallScore") is not None:
        return int(a["overallScore"])
    return overall_score(*(LEVEL_VALUE[AutonomyLevel(a[k])] for k in
                           ("hygieneLevel", "medicationLevel", "mobilityLevel", "feedingLevel")))


def derive_from_assessments(date_of_birth: date, assessments: list[dict]) -> DerivedFeatures | None:
    """Build model features from identity-service autonomy assessments.

    Uses the latest assessment for the current levels and the one before it for the trend.
    Returns None when the patient has no assessment yet.
    """
    if not assessments:
        return None
    ordered = sorted(assessments, key=lambda a: (_assessment_date(a), a.get("createdAt") or ""), reverse=True)
    latest = ordered[0]
    previous = ordered[1] if len(ordered) > 1 else None
    latest_date = _assessment_date(latest)

    score_change = 0
    months = 0.0
    if previous is not None:
        score_change = _score(latest) - _score(previous)
        months = max((latest_date - _assessment_date(previous)).days, 0) / 30.44

    features = RiskFeatures(
        age_years=age_on(date_of_birth, latest_date),
        hygiene_level=latest["hygieneLevel"],
        medication_level=latest["medicationLevel"],
        mobility_level=latest["mobilityLevel"],
        feeding_level=latest["feedingLevel"],
        score_change=max(-9, min(9, score_change)),
        months_since_previous=min(months, 60.0),
        has_previous=previous is not None,
    )
    return DerivedFeatures(features=features, latest_assessment_date=latest_date,
                           assessments_used=2 if previous is not None else 1)
