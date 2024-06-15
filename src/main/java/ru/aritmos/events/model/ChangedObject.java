package ru.aritmos.events.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

@Data
@Serdeable
@Builder
public class ChangedObject {
    Object oldValue;
    Object newValue;
}
