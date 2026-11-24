"""Train, evaluate and save the care-risk model.

Usage:  python -m training.train [--out app/artifacts] [--samples 6000] [--seed 42]

Two candidates are evaluated on a stratified hold-out split:
- logistic regression (standardised inputs) - interpretable, per-feature contributions;
- histogram gradient boosting - a non-linear baseline.
The logistic regression is deployed because each prediction must be explainable to a
clinician; the gradient-boosting score is kept in the metadata to show what accuracy the
interpretable model gives up (if any).
"""
from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path

import joblib
import sklearn
from sklearn.ensemble import HistGradientBoostingClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, brier_score_loss, roc_auc_score
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

from app.config import DEFAULT_ARTIFACT_DIR
from app.features import FEATURE_NAMES
from training.synthetic import generate

MODEL_VERSION = "care-risk-lr-1.0.0"
THRESHOLDS = {"moderate": 0.30, "high": 0.60}
TARGET = "Next autonomy assessment shows a decline of at least 2 points (1-10 scale)"
DISCLAIMER = (
    "Demonstration model trained on synthetic data. Not clinically validated; "
    "do not use for diagnosis or treatment decisions."
)


def _evaluate(model, X_test, y_test) -> dict:
    proba = model.predict_proba(X_test)[:, 1]
    return {
        "roc_auc": round(float(roc_auc_score(y_test, proba)), 4),
        "brier": round(float(brier_score_loss(y_test, proba)), 4),
        "accuracy_at_0_5": round(float(accuracy_score(y_test, proba >= 0.5)), 4),
    }


def train(out_dir: Path, samples: int = 6000, seed: int = 42) -> dict:
    X, y = generate(samples, seed)
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=seed, stratify=y
    )

    logreg = Pipeline([
        ("scale", StandardScaler()),
        ("clf", LogisticRegression(max_iter=1000, C=1.0)),
    ]).fit(X_train, y_train)
    boosting = HistGradientBoostingClassifier(random_state=seed).fit(X_train, y_train)

    metadata = {
        "model_version": MODEL_VERSION,
        "algorithm": "StandardScaler + LogisticRegression (scikit-learn)",
        "target": TARGET,
        "features": FEATURE_NAMES,
        "trained_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "training_data": (
            f"Synthetic: {samples} generated patients (seed {seed}), 80/20 stratified split; "
            f"positive rate {y.mean():.2f}. See training/synthetic.py."
        ),
        "metrics": {
            "deployed_logistic_regression": _evaluate(logreg, X_test, y_test),
            "baseline_gradient_boosting": _evaluate(boosting, X_test, y_test),
            "test_samples": int(len(y_test)),
        },
        "thresholds": THRESHOLDS,
        "disclaimer": DISCLAIMER,
        "sklearn_version": sklearn.__version__,
    }

    out_dir.mkdir(parents=True, exist_ok=True)
    joblib.dump(logreg, out_dir / "model.joblib")
    (out_dir / "metadata.json").write_text(json.dumps(metadata, indent=2))
    return metadata


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--out", type=Path, default=DEFAULT_ARTIFACT_DIR)
    parser.add_argument("--samples", type=int, default=6000)
    parser.add_argument("--seed", type=int, default=42)
    args = parser.parse_args()
    metadata = train(args.out, args.samples, args.seed)
    print(json.dumps(metadata["metrics"], indent=2))


if __name__ == "__main__":
    main()
