package ru.aritmos.service;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.GroovyScript;

/** Unit tests for {@link GroovyScriptService}. */
class GroovyScriptServiceTest {

  @DisplayName("Выполнение скрипта делает доступными переменные")
  @Test
  void executesScriptAndExposesVariables() {
    GroovyScriptService service = new GroovyScriptService();
    GroovyScript script = GroovyScript.builder().ruleCode("z = x + y").build();
    script.getInputParameters().put("x", 2);
    script.getInputParameters().put("y", 3);

    service.Execute(script);

    assertEquals(5, script.getOutputParameters().get("z"));
  }

  /** Бросает исключение при ошибке синтаксиса в сценарии. */
  @DisplayName("Выполнение выбрасывает исключение при ошибке компиляции")
  @Test
  void executeThrowsOnCompilationError() {
    GroovyScriptService service = new GroovyScriptService();
    GroovyScript script = GroovyScript.builder().ruleCode("z = ").build();

    assertThrows(RuntimeException.class, () -> service.Execute(script));
  }
}
