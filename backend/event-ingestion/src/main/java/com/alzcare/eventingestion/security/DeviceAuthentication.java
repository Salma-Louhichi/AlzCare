package com.alzcare.eventingestion.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.UUID;

/** Authentication of a paired camera (not a person). */
public class DeviceAuthentication extends AbstractAuthenticationToken {

    private final UUID cameraId;

    public DeviceAuthentication(UUID cameraId) {
        super(AuthorityUtils.createAuthorityList("ROLE_DEVICE"));
        this.cameraId = cameraId;
        setAuthenticated(true);
    }

    public UUID cameraId() {
        return cameraId;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return cameraId;
    }
}
