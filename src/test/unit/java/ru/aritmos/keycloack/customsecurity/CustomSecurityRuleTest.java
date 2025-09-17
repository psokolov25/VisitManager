package ru.aritmos.keycloack.customsecurity;

import static ru.aritmos.test.LoggingAssertions.assertEquals;

import io.micronaut.security.rules.SecurityRuleResult;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

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
}

