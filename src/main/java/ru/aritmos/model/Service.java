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

    public Service(String key,String name, Integer servingSL, String linkedQueueId) {

        super(key,name);
        this.servingSL = servingSL;
        this.linkedQueueId = linkedQueueId;
        this.isAvailable=true;
    }

    /**
     * Нормативное время обслуживания
     */
    Integer servingSL;
    /**
     * Связанная очередь
     */
    String linkedQueueId;
    /**
     * Флаг доступности
     */
    Boolean isAvailable ;


}
