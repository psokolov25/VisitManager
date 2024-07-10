package ru.aritmos.service.rules;

import java.util.UUID;

public interface Rule {
    String id = UUID.randomUUID().toString();
    String name="";
}
