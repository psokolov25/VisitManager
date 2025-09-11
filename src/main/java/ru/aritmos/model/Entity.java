package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.*;

/**
 * Базовая сущность с идентификатором и именем.
 */
@SuppressWarnings("unused")
@Data
@Serdeable
@Builder
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
public class Entity {
  /** Идентификатор. */
  String id;
  /** Имя. */
  String name;
}
