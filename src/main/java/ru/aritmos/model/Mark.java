package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Serdeable
@Data
@Builder
@AllArgsConstructor
public class Mark {
  String id;
  String value;
  ZonedDateTime markDate;
}
