package ru.aritmos.service;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import ru.aritmos.model.GroovyScript;


public class GroovyScriptService {
    public void Execute(GroovyScript groovyScript)
    {
        Binding binding = new Binding();
        // Объект выполнения скрипта Groovy
        GroovyShell shell = new GroovyShell(binding);
        // Текст скрипта на groovy, берется из свойства scriptCode
        String scriptCode = groovyScript.getRuleCode();
        // Обработка скрипта перед выполнением
        Script script = shell.parse(scriptCode);
        groovyScript.getInputParameters().forEach(binding::setVariable);
        script.run();
        // Передача двух тестовых визитов
        groovyScript.getOutputParameters().putAll(binding.getVariables());
    }
}
