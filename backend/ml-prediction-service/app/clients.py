"""HTTP clients for the services that own the data the model needs.

The caller's bearer token is forwarded so downstream services see the real user.
"""
from __future__ import annotations

from datetime import date

import httpx

from app.config import Settings


class UpstreamError(RuntimeError):
    def __init__(self, status_code: int, detail: str):
        super().__init__(detail)
        self.status_code = status_code
        self.detail = detail


class PlatformClient:
    def __init__(self, settings: Settings, transport: httpx.BaseTransport | None = None):
        self._settings = settings
        self._transport = transport

    def _client(self, token: str) -> httpx.Client:
        return httpx.Client(
            timeout=self._settings.http_timeout_seconds,
            headers={"Authorization": f"Bearer {token}", "Accept": "application/json"},
            transport=self._transport,
        )

    def patient_profile(self, patient_user_id: str, token: str) -> dict:
        url = f"{self._settings.identity_service_url}/api/v1/patients/{patient_user_id}"
        with self._client(token) as client:
            response = self._get(client, url)
        if response.status_code == 404:
            raise UpstreamError(404, "Patient not found")
        self._raise_for_status(response, "identity-service")
        return response.json()

    def autonomy_history(self, patient_profile_id: str, token: str) -> list[dict]:
        url = f"{self._settings.identity_service_url}/api/v1/patients/{patient_profile_id}/autonomy/history"
        with self._client(token) as client:
            response = self._get(client, url)
        self._raise_for_status(response, "identity-service")
        return response.json()

    def active_doctor_id(self, patient_user_id: str, token: str) -> str | None:
        """Doctor currently assigned to the patient, or None (care-team answers an error then)."""
        url = f"{self._settings.care_team_service_url}/api/v1/care-team/patients/{patient_user_id}/doctor"
        with self._client(token) as client:
            response = self._get(client, url)
        if response.status_code != 200:
            return None
        return (response.json() or {}).get("doctorId")

    @staticmethod
    def _get(client: httpx.Client, url: str) -> httpx.Response:
        try:
            return client.get(url)
        except httpx.HTTPError as exc:
            raise UpstreamError(503, f"Upstream service unreachable: {exc.__class__.__name__}") from exc

    @staticmethod
    def _raise_for_status(response: httpx.Response, service: str) -> None:
        if response.status_code >= 400:
            raise UpstreamError(502, f"{service} answered {response.status_code}")


def parse_date(value: str) -> date:
    return date.fromisoformat(value[:10])
