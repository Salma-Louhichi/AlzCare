# Keycloak Service

OAuth2/OIDC Identity Provider for AlzCare Platform.

## Overview

- **Version**: Keycloak 22.0.5
- **Port**: 8090
- **Database**: PostgreSQL 15 (port 5439)
- **Admin Console**: http://localhost:8090/admin

## Quick Start

```bash
# Make sure networks exist (created by root docker-compose.yml)
docker network create alzcare_backend-network 2>/dev/null || true
docker network create alzcare_database-network 2>/dev/null || true

# Start Keycloak and PostgreSQL
docker-compose up -d

# Wait for health check (about 60 seconds)
docker-compose ps
```

## Admin Access

- **URL**: http://localhost:8090/admin
- **Username**: `admin`
- **Password**: `admin`

## Pre-configured Realm: "alzcare"

### Roles
| Role | Description |
|------|-------------|
| `PATIENT` | Patient user with access to own profile |
| `CAREGIVER` | Caregiver with access to assigned patients |
| `DOCTOR` | Doctor with full medical access |
| `ADMIN` | System administrator with full access |

### Clients
| Client ID | Type | Description |
|-----------|------|-------------|
| `alzcare-webapp` | Public | Angular web app for patients/caregivers |
| `alzcare-admin` | Public | Admin dashboard |
| `alzcare-gateway` | Confidential | API Gateway service account |

### Pre-configured User
| Username | Password | Role |
|----------|----------|------|
| `admin` | `Admin123!` | ADMIN |

## Realm Configuration

The realm is imported from `realm-config/alzcare-realm.json` on first startup.

To update the realm:
1. Make changes in Keycloak Admin Console
2. Export: `docker exec keycloak /opt/keycloak/bin/kc.sh export --realm alzcare --file /tmp/alzcare-realm.json`
3. Copy: `docker cp keycloak:/tmp/alzcare-realm.json ./realm-config/`

## Security Notes

⚠️ **Development Only** - These credentials are for local development:
- Change default passwords in production
- Use proper secrets management (K8s Secrets, Vault)
- Enable HTTPS in production
- Update client secrets

## API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /realms/alzcare/.well-known/openid-configuration` | OIDC Discovery |
| `POST /realms/alzcare/protocol/openid-connect/token` | Token endpoint |
| `GET /realms/alzcare/protocol/openid-connect/certs` | JWKS (public keys) |
| `GET /admin/realms/alzcare/users` | Admin API - List users |

## Integration with Identity Service

Identity Service creates profiles in Keycloak via Admin API:

1. Create patient/doctor/caregiver profile in Identity Service
2. Call Keycloak Admin API to create user with role
3. Store Keycloak user ID (`keycloakId`) in profile entity
4. User receives temporary credentials

## Health Check

```bash
# Check if Keycloak is ready
curl http://localhost:8090/health/ready

# Check realm is accessible
curl http://localhost:8090/realms/alzcare/.well-known/openid-configuration
```
