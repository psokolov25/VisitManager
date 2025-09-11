package ru.aritmos.events.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/** Событие шины данных между сервисами. */
@Data
@Builder
@Serdeable
@Introspected
public class Event {
  /** Имя сервиса-отправителя события. */
  String senderService;
  /** Дата и время события. */
  ZonedDateTime eventDate;
  /** Тип события. */
  String eventType;
  /** Дополнительные параметры события. */
  Map<String, String> params;
  /** Тело события (произвольный объект). */
  Object body;
}
