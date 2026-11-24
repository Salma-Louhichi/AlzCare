# ml-prediction-service

Python/FastAPI service that estimates a patient's **care risk**: the probability that their next
autonomy assessment shows a decline. Port **8094**, reached through the gateway at
`/api/v1/ml/**`. Read [MODEL_CARD.md](MODEL_CARD.md) first: the model is a demonstration trained
on synthetic data.

## Endpoints

| Method | Path | Who | Description |
|---|---|---|---|
| GET | `/health` | public | Liveness and model status (Docker health check) |
| GET | `/api/v1/ml/model` | DOCTOR, ADMIN | Model version, features, metrics, thresholds |
| POST | `/api/v1/ml/risk/predict` | DOCTOR, ADMIN | Predict from explicit features |
| GET | `/api/v1/ml/risk/patients/{patientUserId}` | DOCTOR (own patients), ADMIN | Predict from the patient's stored assessments |
| GET | `/api/v1/ml/docs` | — | Swagger UI |

### Patient prediction flow
1. The JWT is verified against Keycloak's JWKS (RS256); the caller must be DOCTOR or ADMIN.
2. For a doctor, care-team-service must report them as the patient's active doctor (else 403).
3. identity-service provides the profile (date of birth) and the autonomy history.
4. Features come from the two latest assessments; the response contains the probability, the
   risk level, the three main factors, the model version and a disclaimer.

The caller's token is forwarded to both services. Missing assessments → 422; unreachable
services → 503 (`application/problem+json`).

## Configuration

| Variable | Default |
|---|---|
| `KEYCLOAK_JWKS_URL` | `http://keycloak:8080/realms/alzcare/protocol/openid-connect/certs` |
| `KEYCLOAK_ISSUER` | unset (issuer not checked) |
| `IDENTITY_SERVICE_URL` | `http://identity-service:8001` |
| `CARE_TEAM_SERVICE_URL` | `http://care-team-service:8008` |
| `ARTIFACT_DIR` | `app/artifacts` |

## Development
```bash
python -m venv .venv && source .venv/bin/activate
pip install -r requirements-dev.txt
python -m training.train
pytest
uvicorn app.main:app --port 8094 --reload
```
