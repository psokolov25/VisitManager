package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.*;

@SuppressWarnings("unused")
@Data
@Serdeable
@Builder
@AllArgsConstructor
@EqualsAndHashCode
@NoArgsConstructor
public class Entity {
  String id;
  String name;
}
