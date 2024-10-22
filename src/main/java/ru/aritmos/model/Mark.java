package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/** Пометка о визите */
@Serdeable
@Data
@Builder
@AllArgsConstructor
public class Mark {
  /** Идентификатор пометки */
  String id;

  /** Содержимое пометки */
  String value;

  /** Дата и время пометки */
  ZonedDateTime markDate;
}
