package com.sigtam.ms_turnos.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_PROFILE_PATH = "/api/auth/me";

    private final RestClient authServiceRestClient;

    public AuthenticationFilter(RestClient authServiceRestClient) {
        this.authServiceRestClient = authServiceRestClient;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(request.getContextPath() + "/api/")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)
                || authorization.length() == BEARER_PREFIX.length()) {
            reject(response, HttpStatus.UNAUTHORIZED);
            return;
        }

        try {
            Map<?, ?> profile = authServiceRestClient.get()
                    .uri(USER_PROFILE_PATH)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(Map.class);
            if (profile == null || !(profile.get("id") instanceof Number id)
                    || !(profile.get("role") instanceof String role)) {
                reject(response, HttpStatus.SERVICE_UNAVAILABLE);
                return;
            }

            AuthenticatedUser user = new AuthenticatedUser(id.intValue(), role);
            if (!isAuthorized(request, user)) {
                reject(response, HttpStatus.FORBIDDEN);
                return;
            }
            request.setAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE, user);
        } catch (HttpStatusCodeException error) {
            HttpStatus status = error.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()
                    || error.getStatusCode().value() == HttpStatus.FORBIDDEN.value()
                    ? HttpStatus.UNAUTHORIZED
                    : HttpStatus.SERVICE_UNAVAILABLE;
            reject(response, status);
            return;
        } catch (ResourceAccessException error) {
            reject(response, HttpStatus.SERVICE_UNAVAILABLE);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthorized(HttpServletRequest request, AuthenticatedUser user) {
        if (!user.isStaff() && !user.isPatient()) {
            return false;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();

        if ("POST".equalsIgnoreCase(method) && "/api/disponibilidad".equals(path)) {
            return user.isStaff();
        }
        if ("PUT".equalsIgnoreCase(method) && "/api/turnos/siguiente".equals(path)) {
            return user.isStaff();
        }
        if ("POST".equalsIgnoreCase(method) && "/api/turnos/solicitar".equals(path)) {
            return user.isPatient();
        }
        String patientPath = "/api/turnos/paciente/";
        if ("GET".equalsIgnoreCase(method) && path.startsWith(patientPath)) {
            String patientId = path.substring(patientPath.length());
            return user.isStaff() || (user.isPatient() && patientId.equals(Integer.toString(user.id())));
        }
        return true;
    }

    private void reject(HttpServletResponse response, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, BEARER_PREFIX.trim());
        }
    }
}
