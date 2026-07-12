package org.example.gateway.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gateway.exception.DeviceAuthException;
import org.example.gateway.exception.DeviceRegistryException;
import org.example.gateway.service.DeviceRegistryService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

@Component
@Order(1)
@AllArgsConstructor
@Slf4j
public class DeviceAuthFilter extends OncePerRequestFilter {

    private final DeviceRegistryService deviceRegistry;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("=== DeviceAuthFilter START ===");
        log.info("Request path: {}", request.getRequestURI());

        X509Certificate[] certs = (X509Certificate[])
                request.getAttribute("jakarta.servlet.request.X509Certificate");

        if (certs == null) {
            certs = (X509Certificate[])
                    request.getAttribute("javax.servlet.request.X509Certificate");
        }

        log.info("Certificates found: {}", certs != null ? certs.length : "null");

        if (certs == null || certs.length == 0) {
            log.error("NO CERTIFICATE - returning 401");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No certificate");
            return;
        }

        try {
            X509Certificate cert = certs[0];
            String dn = cert.getSubjectX500Principal().getName();
            log.info("Certificate DN: {}", dn);

            String deviceId = dn.split(",")[0].split("=")[1].trim();
            log.info("Extracted deviceId: {}", deviceId);

            request.setAttribute("deviceId", deviceId);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Error processing certificate", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid certificate");
        }
    }
}