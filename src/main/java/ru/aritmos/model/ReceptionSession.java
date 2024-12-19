package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Data;
/* Сеанс работы приемной */
@Data
@Builder
@Serdeable
@SuppressWarnings("unused")
public class ReceptionSession {
  User user;
  ZonedDateTime startTime;
  ZonedDateTime endTime;
}
