package ru.aritmos.service;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import ru.aritmos.model.GroovyScript;

/**
 * Сервис выполнения сценариев Groovy для пользовательских правил.
 */
public class GroovyScriptService {
  /**
   * Выполнить скрипт Groovy с передачей входных параметров и сбором результатов.
   * После выполнения значения переменных доступны через {@code outputParameters}.
   *
   * @param groovyScript объект скрипта и его параметров
   */
  public void Execute(GroovyScript groovyScript) {
    Binding binding = new Binding();
    // Объект выполнения скрипта Groovy
    GroovyShell shell = new GroovyShell(binding);
    // Текст скрипта на Groovy, берётся из свойства ruleCode
    String scriptCode = groovyScript.getRuleCode();
    // Обработка скрипта перед выполнением
    Script script = shell.parse(scriptCode);
    groovyScript.getInputParameters().forEach(binding::setVariable);
    script.run();
    // Передача результатов выполнения
    // в карту выходных параметров
    groovyScript.getOutputParameters().putAll(binding.getVariables());
  }
}
