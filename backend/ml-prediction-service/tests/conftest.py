"""Shared fixtures: a model trained once per session, RSA-signed test tokens and fake services."""
from __future__ import annotations

import time
from pathlib import Path

import httpx
import jwt
import pytest
from cryptography.hazmat.primitives.asymmetric import rsa
from fastapi.testclient import TestClient

from app import main, security
from app.clients import PlatformClient
from app.config import Settings, get_settings
from training.train import train

DOCTOR_ID = "11111111-1111-1111-1111-111111111111"
OTHER_DOCTOR_ID = "22222222-2222-2222-2222-222222222222"
PATIENT_USER_ID = "33333333-3333-3333-3333-333333333333"
PATIENT_PROFILE_ID = "44444444-4444-4444-4444-444444444444"


@pytest.fixture(scope="session")
def artifact_dir(tmp_path_factory) -> Path:
    out = tmp_path_factory.mktemp("artifacts")
    train(out, samples=3000, seed=7)
    return out


@pytest.fixture(scope="session")
def rsa_key():
    return rsa.generate_private_key(public_exponent=65537, key_size=2048)


@pytest.fixture
def make_token(rsa_key):
    def _make(sub: str = DOCTOR_ID, roles=("DOCTOR",), expires_in: int = 300, key=None) -> str:
        now = int(time.time())
        claims = {"sub": sub, "iat": now, "exp": now + expires_in,
                  "realm_access": {"roles": list(roles)}}
        return jwt.encode(claims, key or rsa_key, algorithm="RS256")
    return _make


@pytest.fixture
def platform():
    """In-memory stand-in for identity-service and care-team-service."""
    state = {
        "doctor_id": DOCTOR_ID,
        "profile": {"id": PATIENT_PROFILE_ID, "userId": PATIENT_USER_ID, "dateOfBirth": "1944-05-10"},
        "history": [
            {"assessmentDate": "2026-06-01", "hygieneLevel": "INDEPENDENT", "medicationLevel": "ASSISTED",
             "mobilityLevel": "INDEPENDENT", "feedingLevel": "INDEPENDENT", "overallScore": 8},
            {"assessmentDate": "2026-10-01", "hygieneLevel": "ASSISTED", "medicationLevel": "DEPENDENT",
             "mobilityLevel": "ASSISTED", "feedingLevel": "INDEPENDENT", "overallScore": 5},
        ],
        "down": False,
        "requests": [],
    }

    def handler(request: httpx.Request) -> httpx.Response:
        state["requests"].append(request)
        if state["down"]:
            raise httpx.ConnectError("down", request=request)
        path = request.url.path
        if path.endswith(f"/patients/{PATIENT_USER_ID}/doctor"):
            if state["doctor_id"] is None:
                return httpx.Response(500, json={"error": "No active doctor assignment"})
            return httpx.Response(200, json={"doctorId": state["doctor_id"], "patientId": PATIENT_USER_ID})
        if path.endswith(f"/patients/{PATIENT_USER_ID}"):
            return httpx.Response(200, json=state["profile"])
        if path.endswith(f"/patients/{PATIENT_PROFILE_ID}/autonomy/history"):
            return httpx.Response(200, json=state["history"])
        return httpx.Response(404, json={"detail": "not found"})

    state["transport"] = httpx.MockTransport(handler)
    return state


@pytest.fixture
def client(artifact_dir, rsa_key, platform, monkeypatch):
    settings = Settings(artifact_dir=artifact_dir, identity_service_url="http://identity",
                        care_team_service_url="http://care-team")
    monkeypatch.setattr(security, "signing_key_for", lambda token, s: rsa_key.public_key())
    main.app.dependency_overrides[get_settings] = lambda: settings
    main.app.dependency_overrides[main.get_client] = lambda: PlatformClient(settings, platform["transport"])
    monkeypatch.setattr(main, "get_settings", lambda: settings)  # used by the lifespan
    with TestClient(main.app) as test_client:
        yield test_client
    main.app.dependency_overrides.clear()


@pytest.fixture
def auth(make_token):
    def _auth(**kwargs) -> dict:
        return {"Authorization": f"Bearer {make_token(**kwargs)}"}
    return _auth
