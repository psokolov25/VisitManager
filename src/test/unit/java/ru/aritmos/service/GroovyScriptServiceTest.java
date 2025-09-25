package ru.aritmos.service;

import org.junit.jupiter.api.Test;
import ru.aritmos.model.GroovyScript;
import org.junit.jupiter.api.DisplayName;

import static ru.aritmos.test.LoggingAssertions.*;

/** Unit tests for {@link GroovyScriptService}. */
class GroovyScriptServiceTest {

    @DisplayName("Executes Script And Exposes Variables")
    @Test
    void executesScriptAndExposesVariables() {
        GroovyScriptService service = new GroovyScriptService();
        GroovyScript script = GroovyScript.builder()
                .ruleCode("z = x + y")
                .build();
        script.getInputParameters().put("x", 2);
        script.getInputParameters().put("y", 3);

        service.Execute(script);

        assertEquals(5, script.getOutputParameters().get("z"));
    }

    /**
     * Бросает исключение при ошибке синтаксиса в сценарии.
     */
    @DisplayName("Execute Throws On Compilation Error")
    @Test
    void executeThrowsOnCompilationError() {
        GroovyScriptService service = new GroovyScriptService();
        GroovyScript script = GroovyScript.builder()
                .ruleCode("z = ")
                .build();

        assertThrows(RuntimeException.class, () -> service.Execute(script));
    }
}
