package ru.aritmos.model.visit;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder
@Serdeable
public class VisitEventInformation {
    VisitEvent visitEvent;
    ZonedDateTime eventDateTime;

    Map<String, String> parameters;
    TransactionCompletionStatus transactionCompletionStatus;
}
