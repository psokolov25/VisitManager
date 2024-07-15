package ru.aritmos.model.visit;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
@Serdeable
public class VisitEventDateTime {
    VisitEvent visitEvent;
    ZonedDateTime eventDateTime;
}
