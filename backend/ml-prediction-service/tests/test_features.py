from datetime import date

import pytest

from app.features import FEATURE_NAMES, derive_from_assessments, overall_score, to_vector
from app.schemas import RiskFeatures


@pytest.mark.parametrize("levels,expected", [
    ((1, 1, 1, 1), 1),     # all DEPENDENT
    ((3, 3, 3, 3), 10),    # all INDEPENDENT - must stay within the DB CHECK (1..10)
    ((2, 2, 2, 2), 5),
    ((3, 2, 1, 3), 6),
])
def test_overall_score_matches_identity_service(levels, expected):
    assert overall_score(*levels) == expected


def test_vector_has_one_value_per_feature():
    f = RiskFeatures(age_years=80, hygiene_level="ASSISTED", medication_level="DEPENDENT",
                     mobility_level="INDEPENDENT", feeding_level="INDEPENDENT")
    assert to_vector(f).shape == (1, len(FEATURE_NAMES))


def test_trend_is_ignored_without_previous_assessment():
    f = RiskFeatures(age_years=80, hygiene_level="ASSISTED", medication_level="ASSISTED",
                     mobility_level="ASSISTED", feeding_level="ASSISTED",
                     score_change=-5, months_since_previous=3, has_previous=False)
    row = dict(zip(FEATURE_NAMES, to_vector(f)[0]))
    assert row["score_change"] == 0 and row["months_since_previous"] == 0 and row["has_previous"] == 0


def _assessment(day: str, *levels: str, score: int | None = None) -> dict:
    keys = ("hygieneLevel", "medicationLevel", "mobilityLevel", "feedingLevel")
    a = {"assessmentDate": day, **dict(zip(keys, levels))}
    if score is not None:
        a["overallScore"] = score
    return a


def test_no_assessment_gives_no_features():
    assert derive_from_assessments(date(1945, 1, 1), []) is None


def test_single_assessment():
    d = derive_from_assessments(date(1945, 3, 15), [
        _assessment("2026-03-14", "INDEPENDENT", "ASSISTED", "INDEPENDENT", "INDEPENDENT"),
    ])
    assert d.assessments_used == 1
    assert d.features.has_previous is False
    assert d.features.age_years == 80  # birthday is the day after the assessment


def test_latest_two_assessments_drive_the_trend_regardless_of_input_order():
    d = derive_from_assessments(date(1945, 1, 1), [
        _assessment("2026-09-01", "ASSISTED", "DEPENDENT", "ASSISTED", "INDEPENDENT", score=4),
        _assessment("2025-01-01", "INDEPENDENT", "INDEPENDENT", "INDEPENDENT", "INDEPENDENT", score=10),
        _assessment("2026-03-01", "INDEPENDENT", "ASSISTED", "INDEPENDENT", "INDEPENDENT", score=8),
    ])
    assert d.latest_assessment_date == date(2026, 9, 1)
    assert d.features.score_change == -4
    assert d.features.months_since_previous == pytest.approx(6.05, abs=0.05)
    assert d.assessments_used == 2


def test_score_is_computed_when_missing():
    d = derive_from_assessments(date(1945, 1, 1), [
        _assessment("2026-01-01", "INDEPENDENT", "INDEPENDENT", "INDEPENDENT", "INDEPENDENT"),
        _assessment("2026-04-01", "DEPENDENT", "DEPENDENT", "DEPENDENT", "DEPENDENT"),
    ])
    assert d.features.score_change == 1 - 10
