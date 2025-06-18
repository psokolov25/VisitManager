package ru.aritmos.service.rules;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;
@Serdeable
@Introspected
public interface Rule {
  String id = UUID.randomUUID().toString();
  String name = "";
}
