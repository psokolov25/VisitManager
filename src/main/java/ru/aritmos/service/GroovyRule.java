package ru.aritmos.service;

import ru.aritmos.model.GroovyScript;
import ru.aritmos.service.rules.Rule;
@SuppressWarnings("unused")
public interface GroovyRule extends Rule {
    GroovyScript getScript();
}
