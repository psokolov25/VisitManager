package ru.aritmos.model.visit;

import io.micronaut.serde.annotation.Serdeable;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Информация о событии визита.
 */
@Data
@Builder
@Serdeable
public class VisitEventInformation {
  /** Тип произошедшего события визита. */
  VisitEvent visitEvent;
  /** Дата и время фиксации события. */
  ZonedDateTime eventDateTime;

  /** Дополнительные параметры события. */
  Map<String, String> parameters;
  /** Статус завершения транзакции, если применимо. */
  TransactionCompletionStatus transactionCompletionStatus;
}
