package com.sigtam.ms_turnos.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationFilterTests {
    @Test
    void protectedEndpointRejectsRequestsWithoutBearerToken() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://auth.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AuthenticationFilter filter = new AuthenticationFilter(builder.build());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/disponibilidad");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> forwarded.set(true));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertFalse(forwarded.get());
        server.verify();
    }

    @Test
    void protectedEndpointProceedsOnlyAfterIdentityServiceAcceptsToken() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://auth.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(MockRestRequestMatchers.requestTo("http://auth.test/api/auth/me"))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
                .andExpect(MockRestRequestMatchers.header("Authorization", "Bearer valid-token"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"id\":42,\"role\":\"paciente\",\"is_active\":true}", MediaType.APPLICATION_JSON));
        AuthenticationFilter filter = new AuthenticationFilter(builder.build());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/disponibilidad");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> forwarded.set(true));

        assertTrue(forwarded.get());
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        server.verify();
    }

    @Test
    void patientCannotCreateAvailability() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://auth.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(MockRestRequestMatchers.requestTo("http://auth.test/api/auth/me"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"id\":42,\"role\":\"paciente\"}", MediaType.APPLICATION_JSON));
        AuthenticationFilter filter = new AuthenticationFilter(builder.build());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/disponibilidad");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> forwarded.set(true));

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertFalse(forwarded.get());
        server.verify();
    }

    @Test
    void patientCannotReadAnotherPatientsTurns() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://auth.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(MockRestRequestMatchers.requestTo("http://auth.test/api/auth/me"))
                .andRespond(MockRestResponseCreators.withSuccess(
                        "{\"id\":42,\"role\":\"paciente\"}", MediaType.APPLICATION_JSON));
        AuthenticationFilter filter = new AuthenticationFilter(builder.build());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/turnos/paciente/43");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> forwarded.set(true));

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertFalse(forwarded.get());
        server.verify();
    }

    @Test
    void invalidTokenIsRejectedWithoutCallingController() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://auth.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(MockRestRequestMatchers.requestTo("http://auth.test/api/auth/me"))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.UNAUTHORIZED));
        AuthenticationFilter filter = new AuthenticationFilter(builder.build());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/disponibilidad");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> forwarded.set(true));

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertFalse(forwarded.get());
        server.verify();
    }

    @Test
    void unavailableIdentityServiceFailsClosed() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://auth.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(MockRestRequestMatchers.requestTo("http://auth.test/api/auth/me"))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        AuthenticationFilter filter = new AuthenticationFilter(builder.build());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/disponibilidad");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> forwarded.set(true));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getStatus());
        assertFalse(forwarded.get());
        server.verify();
    }

    @Test
    void authFailureDoesNotBlockCorsPreflight() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://auth.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AuthenticationFilter filter = new AuthenticationFilter(builder.build());
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.OPTIONS.name(), "/api/disponibilidad");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> forwarded.set(true));

        assertTrue(forwarded.get());
        server.verify();
    }
}
