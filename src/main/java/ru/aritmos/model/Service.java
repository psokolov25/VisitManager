package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Serdeable
@EqualsAndHashCode(callSuper = true)
@Data
@Introspected

public class Service extends BranchEntity{
    public Service(String name, Integer standardWaitingTime, String linkedQueueId) {
        super(name);
        this.standardWaitingTime = standardWaitingTime;
        this.linkedQueueId = linkedQueueId;
        this.isAvailable=true;
    }
    Integer standardWaitingTime;
    String linkedQueueId;
    Boolean isAvailable ;


}
