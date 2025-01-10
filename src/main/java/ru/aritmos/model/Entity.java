package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@SuppressWarnings("unused")
@Data
@Serdeable
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Entity {
  String id;
  String name;
}
