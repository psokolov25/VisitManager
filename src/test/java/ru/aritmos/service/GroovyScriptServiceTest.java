package ru.aritmos.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import ru.aritmos.model.GroovyScript;

class GroovyScriptServiceTest {

    @Test
    void executeShouldPopulateOutputParameters() {
        GroovyScript script = GroovyScript.builder()
                .ruleCode("result = a + b")
                .build();
        script.getInputParameters().put("a", 1);
        script.getInputParameters().put("b", 2);

        new GroovyScriptService().Execute(script);

        assertEquals(3, script.getOutputParameters().get("result"));
    }
}
