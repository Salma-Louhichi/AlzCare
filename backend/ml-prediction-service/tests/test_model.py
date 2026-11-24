import json

import joblib
import numpy as np
import pytest

from app.model import RiskModel
from app.schemas import RiskFeatures, RiskLevel
from training.train import train


@pytest.fixture(scope="module")
def model(artifact_dir):
    return RiskModel(artifact_dir)


def _features(**overrides) -> RiskFeatures:
    base = dict(age_years=80, hygiene_level="ASSISTED", medication_level="ASSISTED",
                mobility_level="ASSISTED", feeding_level="INDEPENDENT",
                score_change=0, months_since_previous=6, has_previous=True)
    base.update(overrides)
    return RiskFeatures(**base)


def test_training_writes_artifacts_and_beats_chance(artifact_dir):
    meta = json.loads((artifact_dir / "metadata.json").read_text())
    assert (artifact_dir / "model.joblib").exists()
    assert meta["metrics"]["deployed_logistic_regression"]["roc_auc"] >= 0.70
    assert "synthetic" in meta["training_data"].lower()
    assert "not clinically validated" in meta["disclaimer"].lower()


def test_training_is_reproducible(tmp_path):
    train(tmp_path / "a", samples=1500, seed=3)
    train(tmp_path / "b", samples=1500, seed=3)
    coef_a = joblib.load(tmp_path / "a" / "model.joblib").named_steps["clf"].coef_
    coef_b = joblib.load(tmp_path / "b" / "model.joblib").named_steps["clf"].coef_
    np.testing.assert_allclose(coef_a, coef_b)


def test_recent_decline_increases_risk(model):
    stable = model.predict(_features(score_change=0)).probability
    declining = model.predict(_features(score_change=-4)).probability
    assert declining > stable


def test_older_age_increases_risk(model):
    assert model.predict(_features(age_years=90)).probability > model.predict(_features(age_years=68)).probability


def test_medication_dependence_increases_risk(model):
    assert (model.predict(_features(medication_level="DEPENDENT")).probability
            > model.predict(_features(medication_level="INDEPENDENT")).probability)


def test_risk_levels_follow_thresholds(model):
    assert model.risk_level(0.10) is RiskLevel.LOW
    assert model.risk_level(0.30) is RiskLevel.MODERATE
    assert model.risk_level(0.59) is RiskLevel.MODERATE
    assert model.risk_level(0.60) is RiskLevel.HIGH


def test_prediction_is_explained(model):
    result = model.predict(_features(age_years=91, medication_level="DEPENDENT", score_change=-5))
    assert 1 <= len(result.top_factors) <= 3
    labels = [f.label for f in result.top_factors]
    assert any("Age 91" in label or "Medication: dependent" in label or "-5" in label for label in labels)
    assert all(f.direction in ("increases risk", "decreases risk") for f in result.top_factors)


def test_missing_artifact_is_reported(tmp_path):
    from app.model import ModelNotAvailableError
    with pytest.raises(ModelNotAvailableError):
        RiskModel(tmp_path)
