package ru.aritmos;

import static ru.aritmos.test.LoggingAssertions.*;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import ru.aritmos.model.visit.Visit;

class EntrypointTest {

    private Script parseScript(Binding binding) throws IOException {
        GroovyShell shell = new GroovyShell(binding);
        try (InputStream is = Objects.requireNonNull(
            getClass().getClassLoader().getResourceAsStream("test.groovy"))) {
            String code = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return shell.parse(code);
        }
    }

    @DisplayName("проверяется сценарий «groovy script selects visit by params»")
    @Test
    void groovyScriptSelectsVisitByParams() throws Exception {
        Binding binding = new Binding();
        ZonedDateTime now = ZonedDateTime.now();
        binding.setVariable("visits", List.of(
            Visit.builder()
                .id("v1")
                .queueId("123")
                .createDateTime(now.minusSeconds(10))
                .startServingDateTime(now)
                .build(),
            Visit.builder()
                .id("v2")
                .poolUserId("123")
                .createDateTime(now.minusSeconds(5))
                .startServingDateTime(now)
                .build(),
            Visit.builder()
                .id("v3")
                .servicePointId("123")
                .createDateTime(now.minusSeconds(1))
                .startServingDateTime(now)
                .build()
        ));

        binding.setVariable("params", Map.of(
            "queueId", "123",
            "userPoolId", "123",
            "servicePointId", "123"
        ));

        parseScript(binding).run();
        Object resultObj = binding.getVariable("result");
        Optional<Visit> visit = ((Optional<?>) resultObj).map(Visit.class::cast);
        assertTrue(visit.isPresent());
        assertEquals("v1", visit.get().getId());
    }

    @DisplayName("проверяется сценарий «groovy script returns empty when no match»")
    @Test
    void groovyScriptReturnsEmptyWhenNoMatch() throws Exception {
        Binding binding = new Binding();
        ZonedDateTime now = ZonedDateTime.now();
        binding.setVariable("visits", List.of(
            Visit.builder()
                .id("v1")
                .queueId("123")
                .createDateTime(now.minusSeconds(10))
                .startServingDateTime(now)
                .build()
        ));
        binding.setVariable("params", Map.of("queueId", "999"));

        parseScript(binding).run();
        Object resultObj = binding.getVariable("result");
        assertTrue(((Optional<?>) resultObj).isEmpty());
    }
}