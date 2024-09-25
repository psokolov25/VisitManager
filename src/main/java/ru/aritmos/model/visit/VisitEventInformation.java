package ru.aritmos.model.visit;

import io.micronaut.serde.annotation.Serdeable;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Serdeable
public class VisitEventInformation {
  VisitEvent visitEvent;
  ZonedDateTime eventDateTime;

  Map<String, String> parameters;
  TransactionCompletionStatus transactionCompletionStatus;
}
