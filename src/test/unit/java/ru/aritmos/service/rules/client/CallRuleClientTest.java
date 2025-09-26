package ru.aritmos.service.rules.client;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.context.ApplicationContext;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.runtime.server.EmbeddedServer;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;

/**
 * Тест HTTP‑клиента правила вызова визита.
 */
class CallRuleClientTest {

    /**
     * Клиент отправляет запрос и получает визит.
     */
    @DisplayName("Клиент правила вызова возвращает визит")
    @Test
    void callRuleReturnsVisit() {
        Map<String, Object> config = Map.of(
                "micronaut.application.rules.callRuleApiUrl", "/callrule",
                "micronaut.server.port", -1);
        try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class, config, "integration")) {
            CallRuleClient client = server.getApplicationContext().getBean(CallRuleClient.class);
            Branch branch = new Branch("b1", "Branch");
            ServicePoint servicePoint = new ServicePoint("sp1", "SP1");

            Optional<Visit> result = client.callRule(branch, servicePoint);

            assertTrue(result.isPresent(), "ответ должен содержать визит");
            assertEquals("b1", result.get().getBranchId());
        }
    }

    /**
     * Заглушка сервера CallRule.
     */
    @Controller("/callrule")
    static class TestController {
        @Post
        Optional<Visit> callRule(@Body Object body) {
            return Optional.of(Visit.builder().id("v1").branchId("b1").build());
        }
    }
}

