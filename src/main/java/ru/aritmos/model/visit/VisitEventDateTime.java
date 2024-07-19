package ru.aritmos.model.visit;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@Serdeable
public class VisitEventDateTime {
    VisitEvent visitEvent;
    ZonedDateTime eventDateTime;

    Map<String, String> parameters=new HashMap<>();
}
