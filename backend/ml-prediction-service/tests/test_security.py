from cryptography.hazmat.primitives.asymmetric import rsa

URL = "/api/v1/ml/risk/predict"
BODY = {"ageYears": 80, "hygieneLevel": "ASSISTED", "medicationLevel": "ASSISTED",
        "mobilityLevel": "ASSISTED", "feedingLevel": "ASSISTED"}


def test_missing_token_is_rejected(client):
    assert client.post(URL, json=BODY).status_code == 401


def test_token_signed_by_another_key_is_rejected(client, make_token):
    other_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    token = make_token(key=other_key)
    assert client.post(URL, json=BODY, headers={"Authorization": f"Bearer {token}"}).status_code == 401


def test_expired_token_is_rejected(client, auth):
    assert client.post(URL, json=BODY, headers=auth(expires_in=-60)).status_code == 401


def test_non_clinical_roles_are_forbidden(client, auth):
    for role in ("PATIENT", "CAREGIVER"):
        assert client.post(URL, json=BODY, headers=auth(roles=(role,))).status_code == 403


def test_doctor_and_admin_are_allowed(client, auth):
    assert client.post(URL, json=BODY, headers=auth(roles=("DOCTOR",))).status_code == 200
    assert client.post(URL, json=BODY, headers=auth(roles=("ADMIN",))).status_code == 200


def test_model_info_requires_clinician(client, auth):
    assert client.get("/api/v1/ml/model").status_code == 401
    assert client.get("/api/v1/ml/model", headers=auth(roles=("PATIENT",))).status_code == 403
