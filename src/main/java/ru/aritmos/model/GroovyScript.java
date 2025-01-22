package ru.aritmos.model;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import io.micronaut.serde.annotation.Serdeable;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Serdeable
@AllArgsConstructor
@Builder(toBuilder = true)
@SuppressWarnings({"unused"})
public class GroovyScript {
  Map<String, Object> inputParameters;
  Map<Object, Object> outputParameters;
  String ruleCode;

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
  }
}
