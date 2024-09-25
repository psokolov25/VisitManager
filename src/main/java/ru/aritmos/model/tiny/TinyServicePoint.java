package ru.aritmos.model.tiny;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Сокращенный вариант класса рабочей станции */
@EqualsAndHashCode(callSuper = true)
@Data
@Introspected
@Serdeable
public class TinyServicePoint extends TinyClass {
  /** Флаг доступности рабочей станциии */
  Boolean isAvailable;

  public TinyServicePoint(String id, String name, Boolean isAvailable) {
    super(id, name);
    this.isAvailable = isAvailable;
  }
}
