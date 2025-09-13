package ru.aritmos.keycloack.customsecurity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.micronaut.security.rules.SecurityRuleResult;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import org.junit.jupiter.api.Disabled;

class CustomSecurityRuleTest {

    private final CustomSecurityRule rule = new CustomSecurityRule();

    @Test
    void returnsHighOrder() {
        assertEquals(100000, rule.getOrder());
    }

    @Test
    void checkAlwaysAllowsAccess() {
        SecurityRuleResult result = Mono.from(rule.check(null, null)).block();
        assertEquals(SecurityRuleResult.ALLOWED, result);
    }
    @Disabled("Not yet implemented")
    @Test
    void getOrderTest() {
        // TODO implement
    }

    @Disabled("Not yet implemented")
    @Test
    void checkTest() {
        // TODO implement
    }

}

