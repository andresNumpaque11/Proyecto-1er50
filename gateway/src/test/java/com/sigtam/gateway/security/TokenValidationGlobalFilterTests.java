package com.sigtam.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenValidationGlobalFilterTests {
    @Test
    void protectedRequestWithoutBearerTokenIsRejected() {
        WebClient client = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK).build()))
                .build();
        TokenValidationGlobalFilter filter = new TokenValidationGlobalFilter(client);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/inventory/medications").build());
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
            forwarded.set(true);
            return Mono.empty();
        }).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertFalse(forwarded.get());
    }

    @Test
    void protectedRequestIsForwardedOnlyAfterUserServiceAcceptsToken() {
        WebClient client = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK).build()))
                .build();
        TokenValidationGlobalFilter filter = new TokenValidationGlobalFilter(client);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/inventory/medications")
                        .header("Authorization", "Bearer valid-token")
                        .build());
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
            forwarded.set(true);
            return Mono.empty();
        }).block();

        assertTrue(forwarded.get());
    }

    @Test
    void invalidTokenIsRejectedByUserServiceResponse() {
        WebClient client = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED).build()))
                .build();
        TokenValidationGlobalFilter filter = new TokenValidationGlobalFilter(client);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/inventory/medications")
                        .header("Authorization", "Bearer invalid-token")
                        .build());
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
            forwarded.set(true);
            return Mono.empty();
        }).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertFalse(forwarded.get());
    }

    @Test
    void unavailableIdentityServiceFailsClosed() {
        WebClient client = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build()))
                .build();
        TokenValidationGlobalFilter filter = new TokenValidationGlobalFilter(client);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/inventory/medications")
                        .header("Authorization", "Bearer valid-token")
                        .build());
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.filter(exchange, ignored -> {
            forwarded.set(true);
            return Mono.empty();
        }).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
        assertFalse(forwarded.get());
    }

    @Test
    void publicAuthEndpointsAndCorsPreflightBypassValidation() {
        WebClient client = WebClient.builder()
                .exchangeFunction(request -> {
                    throw new AssertionError("El endpoint público no debe invocar autenticación");
                })
                .build();
        TokenValidationGlobalFilter filter = new TokenValidationGlobalFilter(client);
        MockServerWebExchange login = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/login").build());
        MockServerWebExchange preflight = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/inventory/medications").build());

        filter.filter(login, ignored -> Mono.empty()).block();
        filter.filter(preflight, ignored -> Mono.empty()).block();

        assertEquals(null, login.getResponse().getStatusCode());
        assertEquals(null, preflight.getResponse().getStatusCode());
    }
}
