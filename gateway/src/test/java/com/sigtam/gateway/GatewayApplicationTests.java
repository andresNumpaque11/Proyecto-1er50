package com.sigtam.gateway;

import com.sigtam.gateway.security.TokenValidationGlobalFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class GatewayApplicationTests {
    @Autowired
    private TokenValidationGlobalFilter tokenValidationGlobalFilter;

    @Test
    void authenticationFilterIsRegistered() {
        assertNotNull(tokenValidationGlobalFilter);
    }
}
