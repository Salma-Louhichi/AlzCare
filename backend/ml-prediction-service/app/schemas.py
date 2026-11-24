"""API request/response models (camelCase JSON, like the Java services)."""
from datetime import date
from enum import Enum

from pydantic import BaseModel, ConfigDict, Field
from pydantic.alias_generators import to_camel


class CamelModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class AutonomyLevel(str, Enum):
    INDEPENDENT = "INDEPENDENT"
    ASSISTED = "ASSISTED"
    DEPENDENT = "DEPENDENT"


class RiskLevel(str, Enum):
    LOW = "LOW"
    MODERATE = "MODERATE"
    HIGH = "HIGH"


class RiskFeatures(CamelModel):
    """Model inputs. Usually derived from the two latest autonomy assessments."""

    age_years: float = Field(ge=40, le=110, description="Patient age in years")
    hygiene_level: AutonomyLevel
    medication_level: AutonomyLevel
    mobility_level: AutonomyLevel
    feeding_level: AutonomyLevel
    score_change: int = Field(
        default=0, ge=-9, le=9,
        description="Overall score change since the previous assessment (0 if none)",
    )
    months_since_previous: float = Field(
        default=0, ge=0, le=60,
        description="Months between the previous and the latest assessment (0 if none)",
    )
    has_previous: bool = Field(default=False, description="Whether a previous assessment exists")


class RiskFactor(CamelModel):
    feature: str
    label: str
    impact: float = Field(description="Contribution to the log-odds (positive increases risk)")
    direction: str


class RiskPrediction(CamelModel):
    patient_user_id: str | None = None
    probability: float = Field(ge=0, le=1)
    risk_level: RiskLevel
    overall_score: int
    top_factors: list[RiskFactor]
    based_on_assessment_date: date | None = None
    assessments_used: int = 0
    model_version: str
    disclaimer: str


class ModelInfo(CamelModel):
    model_version: str
    algorithm: str
    target: str
    features: list[str]
    trained_at: str
    training_data: str
    metrics: dict
    thresholds: dict
    disclaimer: str
