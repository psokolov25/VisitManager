package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable
@Introspected

public class Queue extends BranchEntity {
Integer ticketCounter=0;

final String ticketPrefix;
List<Visit> visits;
    public Queue(String name, String ticketPrefix) {
        super(name);
        this.ticketPrefix = ticketPrefix;
    }

}
