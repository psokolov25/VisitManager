package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Пометка о визите */
@Serdeable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("unused")
public class Mark {
  /** Идентификатор пометки */
  String id;

  /** Содержимое пометки */
  String value;

  /** Дата и время пометки */
  ZonedDateTime markDate;

  User author;
}
