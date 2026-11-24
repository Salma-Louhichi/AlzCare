"""JWT verification against Keycloak (defence in depth: the gateway checks too).

Tokens are RS256-signed by the `alzcare` realm. The public keys are fetched from the
realm's JWKS endpoint and cached by PyJWKClient.
"""
from __future__ import annotations

from dataclasses import dataclass
from functools import lru_cache

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.config import Settings, get_settings

CLINICAL_ROLES = {"DOCTOR", "ADMIN"}

_bearer = HTTPBearer(auto_error=False)


@dataclass(frozen=True)
class Principal:
    subject: str
    roles: frozenset[str]
    token: str

    @property
    def is_admin(self) -> bool:
        return "ADMIN" in self.roles


@lru_cache
def _jwks_client(url: str) -> jwt.PyJWKClient:
    return jwt.PyJWKClient(url, cache_keys=True, lifespan=300)


def signing_key_for(token: str, settings: Settings):
    """Separated so tests can provide their own keys."""
    return _jwks_client(settings.keycloak_jwks_url).get_signing_key_from_jwt(token).key


def _roles(claims: dict) -> frozenset[str]:
    roles = claims.get("roles")
    if not roles:
        roles = (claims.get("realm_access") or {}).get("roles", [])
    if isinstance(roles, str):
        roles = [roles]
    return frozenset(roles)


def current_principal(
    credentials: HTTPAuthorizationCredentials | None = Depends(_bearer),
    settings: Settings = Depends(get_settings),
) -> Principal:
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Missing bearer token",
                            headers={"WWW-Authenticate": "Bearer"})
    token = credentials.credentials
    try:
        key = signing_key_for(token, settings)
        claims = jwt.decode(
            token,
            key,
            algorithms=["RS256"],
            issuer=settings.keycloak_issuer,
            options={"verify_aud": False, "verify_iss": settings.keycloak_issuer is not None,
                     "require": ["exp", "sub"]},
        )
    except (jwt.PyJWTError, jwt.PyJWKClientError) as exc:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, f"Invalid token: {exc}",
                            headers={"WWW-Authenticate": "Bearer"}) from exc
    return Principal(subject=claims["sub"], roles=_roles(claims), token=token)


def require_clinician(principal: Principal = Depends(current_principal)) -> Principal:
    if not principal.roles & CLINICAL_ROLES:
        raise HTTPException(status.HTTP_403_FORBIDDEN, "Requires the DOCTOR or ADMIN role")
    return principal
