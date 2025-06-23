package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.serde.annotation.Serdeable;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Serdeable
@Data
@Slf4j
public class GroovyScript {
  @JsonInclude(JsonInclude.Include.ALWAYS)
  Map<String, Object> inputParameters = new HashMap<>();

  @JsonInclude(JsonInclude.Include.ALWAYS)
  HashMap<Object, Object> outputParameters = new HashMap<>();

  String ruleCode;
  /* @JsonIgnore
  public void Execute() {
    Binding binding = new Binding();
    // Объект выполнения скрипта Groovy
    GroovyShell shell = new GroovyShell(binding);
    // Текст скрипта на groovy, берется из свойства scriptCode
    String scriptCode = this.ruleCode;
    // Обработка скрипта перед выполнением
    Script script = shell.parse(scriptCode);
    inputParameters.forEach(binding::setVariable);
    script.run();
    // Передача двух тестовых визитов
    outputParameters.putAll(binding.getVariables());
  }*/
}
