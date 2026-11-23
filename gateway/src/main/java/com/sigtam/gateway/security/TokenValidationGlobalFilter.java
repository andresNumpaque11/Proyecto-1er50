package com.sigtam.gateway.security;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TokenValidationGlobalFilter implements GlobalFilter, Ordered {
    private static final String BEARER_PREFIX = "Bearer ";

    private final WebClient authServiceClient;

    public TokenValidationGlobalFilter(WebClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!requiresAuthentication(path) || "OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)
                || authorization.length() == BEARER_PREFIX.length()) {
            return reject(exchange, HttpStatus.UNAUTHORIZED);
        }

        return authServiceClient.get()
                .uri("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .toBodilessEntity()
                .flatMap(ignored -> chain.filter(exchange))
                .onErrorResume(WebClientResponseException.class, error -> {
                    HttpStatus status = error.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value()
                            || error.getStatusCode().value() == HttpStatus.FORBIDDEN.value()
                            ? HttpStatus.UNAUTHORIZED
                            : HttpStatus.SERVICE_UNAVAILABLE;
                    return reject(exchange, status);
                })
                .onErrorResume(WebClientRequestException.class,
                        error -> reject(exchange, HttpStatus.SERVICE_UNAVAILABLE));
    }

    private boolean requiresAuthentication(String path) {
        return isPathUnder(path, "/api/inventory")
                || isPathUnder(path, "/api/v1");
    }

    private boolean isPathUnder(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
