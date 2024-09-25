package ru.aritmos.model.tiny;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

/** Класс для формирования списков объектов */
@Data
@Introspected
@Jacksonized
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Serdeable
public class TinyClass {
  /** Идентификатора объекта */
  private String id;

  /** Название объекта */
  private String name;
}
