"""AlzCare ML prediction service.

Routes (behind the gateway at /api/v1/ml/**):
- GET  /health                                 liveness + model status (public, used by Docker)
- GET  /api/v1/ml/model                        model card: version, metrics, features
- POST /api/v1/ml/risk/predict                 predict from explicit features
- GET  /api/v1/ml/risk/patients/{userId}       predict from the patient's autonomy assessments
"""
from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import Depends, FastAPI, HTTPException, Request, status
from fastapi.responses import JSONResponse

from app.clients import PlatformClient, UpstreamError, parse_date
from app.config import Settings, get_settings
from app.features import derive_from_assessments
from app.model import ModelNotAvailableError, RiskModel
from app.schemas import ModelInfo, RiskFeatures, RiskPrediction
from app.security import Principal, require_clinician

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")
log = logging.getLogger("ml-prediction")


@asynccontextmanager
async def lifespan(app: FastAPI):
    settings = get_settings()
    try:
        app.state.model = RiskModel(settings.artifact_dir)
        log.info("Loaded model %s from %s", app.state.model.version, settings.artifact_dir)
    except ModelNotAvailableError as exc:
        app.state.model = None
        log.error("Model not loaded: %s", exc)
    yield


app = FastAPI(
    title="AlzCare ML Prediction Service",
    version="1.0.0",
    description="Care-risk prediction from autonomy assessments (demonstration model, synthetic data).",
    lifespan=lifespan,
    docs_url="/api/v1/ml/docs",
    openapi_url="/api/v1/ml/openapi.json",
)


def get_model(request: Request) -> RiskModel:
    model = getattr(request.app.state, "model", None)
    if model is None:
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, "Model not loaded")
    return model


def get_client(settings: Settings = Depends(get_settings)) -> PlatformClient:
    return PlatformClient(settings)


def _problem(status_code: int, title: str, detail: str) -> JSONResponse:
    return JSONResponse(
        status_code=status_code,
        media_type="application/problem+json",
        content={"type": "about:blank", "title": title, "status": status_code, "detail": detail},
    )


@app.exception_handler(HTTPException)
async def http_exception_handler(_: Request, exc: HTTPException):
    response = _problem(exc.status_code, _title(exc.status_code), str(exc.detail))
    if exc.headers:
        response.headers.update(exc.headers)
    return response


@app.exception_handler(UpstreamError)
async def upstream_exception_handler(_: Request, exc: UpstreamError):
    return _problem(exc.status_code, _title(exc.status_code), exc.detail)


def _title(code: int) -> str:
    return {401: "Unauthorized", 403: "Forbidden", 404: "Not Found", 422: "Unprocessable Entity",
            502: "Bad Gateway", 503: "Service Unavailable"}.get(code, "Error")


@app.get("/health", tags=["ops"])
def health(request: Request):
    model = getattr(request.app.state, "model", None)
    return {
        "status": "UP" if model else "DEGRADED",
        "modelLoaded": model is not None,
        "modelVersion": model.version if model else None,
    }


@app.get("/api/v1/ml/model", response_model=ModelInfo, response_model_by_alias=True, tags=["model"])
def model_info(model: RiskModel = Depends(get_model), _: Principal = Depends(require_clinician)):
    m = model.metadata
    return ModelInfo(
        model_version=m["model_version"], algorithm=m["algorithm"], target=m["target"],
        features=m["features"], trained_at=m["trained_at"], training_data=m["training_data"],
        metrics=m["metrics"], thresholds=m["thresholds"], disclaimer=m["disclaimer"],
    )


@app.post("/api/v1/ml/risk/predict", response_model=RiskPrediction, response_model_by_alias=True, tags=["risk"])
def predict(features: RiskFeatures, model: RiskModel = Depends(get_model),
            principal: Principal = Depends(require_clinician)):
    result = model.predict(features)
    log.info("prediction source=features user=%s p=%.3f level=%s model=%s",
             principal.subject, result.probability, result.risk_level.value, model.version)
    return RiskPrediction(
        probability=result.probability, risk_level=result.risk_level, overall_score=result.overall_score,
        top_factors=result.top_factors, model_version=model.version,
        disclaimer=model.metadata["disclaimer"],
    )


@app.get("/api/v1/ml/risk/patients/{patient_user_id}", response_model=RiskPrediction,
         response_model_by_alias=True, tags=["risk"])
def predict_for_patient(patient_user_id: str, model: RiskModel = Depends(get_model),
                        principal: Principal = Depends(require_clinician),
                        client: PlatformClient = Depends(get_client)):
    # A doctor may only score patients assigned to them; admins may score anyone.
    if not principal.is_admin:
        doctor_id = client.active_doctor_id(patient_user_id, principal.token)
        if doctor_id != principal.subject:
            raise HTTPException(status.HTTP_403_FORBIDDEN, "Patient is not assigned to you")

    profile = client.patient_profile(patient_user_id, principal.token)
    if not profile.get("dateOfBirth"):
        raise HTTPException(422, "Patient has no date of birth")
    history = client.autonomy_history(str(profile["id"]), principal.token)

    derived = derive_from_assessments(parse_date(profile["dateOfBirth"]), history)
    if derived is None:
        raise HTTPException(422,
                            "No autonomy assessment recorded yet: record one to get a risk estimate")

    result = model.predict(derived.features)
    log.info("prediction source=patient patient=%s user=%s p=%.3f level=%s model=%s",
             patient_user_id, principal.subject, result.probability, result.risk_level.value, model.version)
    return RiskPrediction(
        patient_user_id=patient_user_id, probability=result.probability, risk_level=result.risk_level,
        overall_score=result.overall_score, top_factors=result.top_factors,
        based_on_assessment_date=derived.latest_assessment_date, assessments_used=derived.assessments_used,
        model_version=model.version, disclaimer=model.metadata["disclaimer"],
    )
