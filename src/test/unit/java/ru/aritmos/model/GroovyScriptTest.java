package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.serde.annotation.Serdeable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GroovyScriptTest {

    @DisplayName("по умолчанию билдер создает пустые изменяемые карты входных и выходных параметров")
    @Test
    void builderCreatesEmptyMapsByDefault() {
        GroovyScript script = GroovyScript.builder().build();

        assertNotNull(script.getInputParameters());
        assertNotNull(script.getOutputParameters());
        assertTrue(script.getInputParameters().isEmpty());
        assertTrue(script.getOutputParameters().isEmpty());

        script.getInputParameters().put("ключ", "значение");
        script.getOutputParameters().put("результат", 42);

        assertEquals("значение", script.getInputParameters().get("ключ"));
        assertEquals(42, script.getOutputParameters().get("результат"));
    }

    @DisplayName("билдер позволяет задать код правила и собственные параметры")
    @Test
    void builderSupportsPopulatingData() {
        HashMap<Object, Object> output = new HashMap<>();
        output.put("result", "готово");

        GroovyScript script = GroovyScript.builder()
                .ruleCode("return 1")
                .inputParameters(Map.of("input", 5))
                .outputParameters(output)
                .build();

        assertEquals("return 1", script.getRuleCode());
        assertEquals(5, script.getInputParameters().get("input"));
        assertSame(output, script.getOutputParameters());
    }

    @DisplayName("модель GroovyScript помечена Serdeable и использует JsonInclude.ALWAYS для карт")
    @Test
    void verifiesAnnotations() throws NoSuchFieldException {
        assertTrue(GroovyScript.class.isAnnotationPresent(Serdeable.class));

        Field input = GroovyScript.class.getDeclaredField("inputParameters");
        Field output = GroovyScript.class.getDeclaredField("outputParameters");

        JsonInclude inputAnnotation = input.getAnnotation(JsonInclude.class);
        JsonInclude outputAnnotation = output.getAnnotation(JsonInclude.class);

        assertNotNull(inputAnnotation);
        assertNotNull(outputAnnotation);
        assertEquals(JsonInclude.Include.ALWAYS, inputAnnotation.value());
        assertEquals(JsonInclude.Include.ALWAYS, outputAnnotation.value());
    }
}
