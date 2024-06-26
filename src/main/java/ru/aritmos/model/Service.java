package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Услуга
 */
@Serdeable
@EqualsAndHashCode(callSuper = true)
@Data
@Introspected

public class Service extends BranchEntity{

    public Service(String key,String name, Integer standardWaitingTime, String linkedQueueId) {

        super(key,name);
        this.standardServiceTime = standardWaitingTime;
        this.linkedQueueId = linkedQueueId;
        this.isAvailable=true;
    }

    /**
     * Стандартное время обслуживания
     */
    Integer standardServiceTime;
    /**
     * Связанная очередь
     */
    String linkedQueueId;
    /**
     * Флаг доступности
     */
    Boolean isAvailable ;


}
