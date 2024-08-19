package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Serdeable
@Data
@Builder
@AllArgsConstructor
public class Mark {
    String id;
    String value;
    ZonedDateTime markDate;

}
