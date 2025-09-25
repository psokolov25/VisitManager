package ru.aritmos.keycloack.customsecurity;

import static ru.aritmos.test.LoggingAssertions.assertEquals;

import io.micronaut.security.rules.SecurityRuleResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import reactor.core.publisher.Mono;

class CustomSecurityRuleTest {

    private final CustomSecurityRule rule = new CustomSecurityRule();

    @DisplayName("проверяется сценарий «returns high order»")
    @Test
    void returnsHighOrder() {
        assertEquals(100000, rule.getOrder());
    }

    @DisplayName("проверяется сценарий «check always allows access»")
    @Test
    void checkAlwaysAllowsAccess() {
        SecurityRuleResult result = Mono.from(rule.check(null, null)).block();
        assertEquals(SecurityRuleResult.ALLOWED, result);
    }
}
