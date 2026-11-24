package com.alzcare.eventingestion.security;

import com.alzcare.eventingestion.domain.CameraDevice;
import com.alzcare.eventingestion.repository.CameraDeviceRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Authenticates cameras with the {@code X-Device-Id} / {@code X-Device-Key} headers.
 * An unknown device and a wrong key are rejected the same way, so callers cannot probe ids.
 */
public class DeviceAuthenticationFilter extends OncePerRequestFilter {

    public static final String DEVICE_ID_HEADER = "X-Device-Id";
    public static final String DEVICE_KEY_HEADER = "X-Device-Key";

    private final CameraDeviceRepository cameras;

    public DeviceAuthenticationFilter(CameraDeviceRepository cameras) {
        this.cameras = cameras;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<UUID> cameraId = parse(request.getHeader(DEVICE_ID_HEADER));
        String key = request.getHeader(DEVICE_KEY_HEADER);
        Optional<CameraDevice> camera = cameraId.flatMap(cameras::findById)
                .filter(c -> DeviceKeys.matches(key, c.getDeviceKeyHash()));
        camera.ifPresent(c -> SecurityContextHolder.getContext().setAuthentication(new DeviceAuthentication(c.getId())));
        chain.doFilter(request, response);
    }

    private static Optional<UUID> parse(String value) {
        try {
            return value == null ? Optional.empty() : Optional.of(UUID.fromString(value.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
