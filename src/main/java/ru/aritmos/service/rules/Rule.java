package ru.aritmos.service.rules;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.util.UUID;

/** Базовый интерфейс правила. */
@Serdeable
@Introspected
public interface Rule {
  /** Идентификатор правила. */
  String id = UUID.randomUUID().toString();

  /** Отображаемое имя правила. */
  String name = "";
}
