package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Serdeable
public class Outcome extends BranchEntity{
    Long code;
}
