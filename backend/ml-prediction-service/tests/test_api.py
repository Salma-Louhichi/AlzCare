from tests.conftest import DOCTOR_ID, OTHER_DOCTOR_ID, PATIENT_USER_ID

PATIENT_URL = f"/api/v1/ml/risk/patients/{PATIENT_USER_ID}"

FEATURES = {
    "ageYears": 84, "hygieneLevel": "ASSISTED", "medicationLevel": "DEPENDENT",
    "mobilityLevel": "ASSISTED", "feedingLevel": "INDEPENDENT",
    "scoreChange": -3, "monthsSincePrevious": 4, "hasPrevious": True,
}


def test_health_is_public_and_reports_model(client):
    body = client.get("/health").json()
    assert body["status"] == "UP" and body["modelLoaded"] is True


def test_model_info(client, auth):
    body = client.get("/api/v1/ml/model", headers=auth()).json()
    assert body["modelVersion"].startswith("care-risk-lr")
    assert "deployed_logistic_regression" in body["metrics"]
    assert body["features"][0] == "age_years"


def test_predict_from_features(client, auth):
    response = client.post("/api/v1/ml/risk/predict", json=FEATURES, headers=auth())
    assert response.status_code == 200
    body = response.json()
    assert 0 <= body["probability"] <= 1
    assert body["riskLevel"] in ("LOW", "MODERATE", "HIGH")
    assert body["overallScore"] == 5
    assert body["topFactors"] and body["disclaimer"]


def test_predict_validates_input(client, auth):
    response = client.post("/api/v1/ml/risk/predict", json={**FEATURES, "medicationLevel": "SOMETIMES"},
                           headers=auth())
    assert response.status_code == 422


def test_patient_prediction_uses_the_two_latest_assessments(client, auth, platform):
    response = client.get(PATIENT_URL, headers=auth())
    assert response.status_code == 200, response.text
    body = response.json()
    assert body["patientUserId"] == PATIENT_USER_ID
    assert body["basedOnAssessmentDate"] == "2026-10-01"
    assert body["assessmentsUsed"] == 2
    assert body["overallScore"] == 5


def test_caller_token_is_forwarded_downstream(client, auth, platform):
    headers = auth()
    client.get(PATIENT_URL, headers=headers)
    assert platform["requests"]
    assert all(r.headers["Authorization"] == headers["Authorization"] for r in platform["requests"])


def test_doctor_cannot_score_another_doctors_patient(client, auth, platform):
    platform["doctor_id"] = OTHER_DOCTOR_ID
    assert client.get(PATIENT_URL, headers=auth()).status_code == 403


def test_doctor_cannot_score_unassigned_patient(client, auth, platform):
    platform["doctor_id"] = None
    assert client.get(PATIENT_URL, headers=auth()).status_code == 403


def test_admin_can_score_any_patient(client, auth, platform):
    platform["doctor_id"] = OTHER_DOCTOR_ID
    response = client.get(PATIENT_URL, headers=auth(sub="admin-1", roles=("ADMIN",)))
    assert response.status_code == 200


def test_patient_without_assessments_gets_a_clear_422(client, auth, platform):
    platform["history"] = []
    response = client.get(PATIENT_URL, headers=auth())
    assert response.status_code == 422
    assert "No autonomy assessment" in response.json()["detail"]


def test_unreachable_identity_service_gives_503(client, auth, platform):
    platform["down"] = True
    response = client.get(PATIENT_URL, headers=auth())
    assert response.status_code == 503
    assert response.headers["content-type"].startswith("application/problem+json")
