package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.jackson.Jacksonized;

@Serdeable
@EqualsAndHashCode(callSuper = true)
@Data
@Jacksonized
public class Service extends BranchEntity{
    public Service(String name, Integer standardWaitingTime, Queue linkedQueue) {
        super(name);
        this.standardWaitingTime = standardWaitingTime;
        this.linkedQueue = linkedQueue;
    }
    final Integer standardWaitingTime;
    public final  Queue linkedQueue;
    Boolean isAvailable = true;

}
