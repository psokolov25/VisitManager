package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Data;

/** Сеанс работы приёмной. */
@Data
@Builder
@Serdeable
@SuppressWarnings("unused")
public class ReceptionSession {
  /** Пользователь. */
  User user;

  /** Время начала. */
  ZonedDateTime startTime;

  /** Время окончания. */
  ZonedDateTime endTime;
}
