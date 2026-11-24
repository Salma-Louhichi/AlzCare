"""Loads the trained model and turns features into an explained prediction."""
from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path

import joblib
import numpy as np

from app.features import FEATURE_NAMES, LEVEL_VALUE, overall_score, to_vector
from app.schemas import RiskFactor, RiskFeatures, RiskLevel


class ModelNotAvailableError(RuntimeError):
    """Raised when no trained artifact is present (run `python -m training.train`)."""


@dataclass
class Explained:
    probability: float
    risk_level: RiskLevel
    overall_score: int
    top_factors: list[RiskFactor]


class RiskModel:
    def __init__(self, artifact_dir: Path):
        model_path = artifact_dir / "model.joblib"
        meta_path = artifact_dir / "metadata.json"
        if not model_path.exists() or not meta_path.exists():
            raise ModelNotAvailableError(f"No trained model in {artifact_dir}")
        self.pipeline = joblib.load(model_path)
        self.metadata: dict = json.loads(meta_path.read_text())
        if self.metadata.get("features") != FEATURE_NAMES:
            raise ModelNotAvailableError("Model features do not match the service's feature list")
        self._scaler = self.pipeline.named_steps["scale"]
        self._coef = self.pipeline.named_steps["clf"].coef_[0]

    @property
    def version(self) -> str:
        return self.metadata["model_version"]

    def risk_level(self, probability: float) -> RiskLevel:
        t = self.metadata["thresholds"]
        if probability >= t["high"]:
            return RiskLevel.HIGH
        if probability >= t["moderate"]:
            return RiskLevel.MODERATE
        return RiskLevel.LOW

    def predict(self, features: RiskFeatures, top_n: int = 3) -> Explained:
        x = to_vector(features)
        probability = float(self.pipeline.predict_proba(x)[0, 1])

        # Logistic regression: each feature adds coef * standardised value to the log-odds
        # (relative to an average patient of the training data).
        contributions = self._coef * self._scaler.transform(x)[0]
        order = np.argsort(-np.abs(contributions))
        factors = [
            RiskFactor(
                feature=FEATURE_NAMES[i],
                label=_label(FEATURE_NAMES[i], features),
                impact=round(float(contributions[i]), 3),
                direction="increases risk" if contributions[i] > 0 else "decreases risk",
            )
            for i in order[:top_n]
            if abs(contributions[i]) >= 0.01
        ]
        levels = [LEVEL_VALUE[getattr(features, f"{a}_level")] for a in ("hygiene", "medication", "mobility", "feeding")]
        return Explained(
            probability=round(probability, 4),
            risk_level=self.risk_level(probability),
            overall_score=overall_score(*levels),
            top_factors=factors,
        )


def _label(feature: str, f: RiskFeatures) -> str:
    if feature == "age_years":
        return f"Age {f.age_years:.0f}"
    if feature in ("hygiene", "medication", "mobility", "feeding"):
        level = getattr(f, f"{feature}_level").value.lower()
        return f"{feature.capitalize()}: {level}"
    if feature == "score_change":
        if not f.has_previous:
            return "No earlier assessment to compare"
        return f"Overall score changed by {f.score_change:+d} since the previous assessment"
    if feature == "months_since_previous":
        if not f.has_previous:
            return "No earlier assessment to compare"
        return f"{f.months_since_previous:.1f} months between the last two assessments"
    if feature == "has_previous":
        return "Previous assessment available" if f.has_previous else "Only one assessment recorded"
    return feature
