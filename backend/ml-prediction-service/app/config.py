"""Runtime configuration, read from environment variables."""
from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

DEFAULT_ARTIFACT_DIR = Path(__file__).resolve().parent / "artifacts"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="", case_sensitive=False)

    # Where the trained model and its metadata live (produced by `python -m training.train`)
    artifact_dir: Path = DEFAULT_ARTIFACT_DIR

    # Keycloak: tokens are verified against the realm's public keys
    keycloak_jwks_url: str = "http://keycloak:8080/realms/alzcare/protocol/openid-connect/certs"
    # Optional: reject tokens from another issuer (e.g. http://localhost:8090/realms/alzcare)
    keycloak_issuer: str | None = None

    # Downstream services (inside the Docker network, called directly)
    identity_service_url: str = "http://identity-service:8001"
    care_team_service_url: str = "http://care-team-service:8008"
    http_timeout_seconds: float = 5.0


@lru_cache
def get_settings() -> Settings:
    return Settings()
