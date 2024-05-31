package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;
@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable

public class Queue extends BranchEntity {
Integer ticketCounter=0;

final String ticketPrefix;
List<Visit> visits;
    public Queue(String name, String ticketPrefix) {
        super(name);
        this.ticketPrefix = ticketPrefix;
    }

}
