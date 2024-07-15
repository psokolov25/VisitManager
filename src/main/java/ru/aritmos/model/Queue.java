package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Очередь
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable
@Introspected


public final class Queue extends BranchEntityWithVisits {
    /**
     * Счетчик талонов
     */
    Integer ticketCounter = 0;
    /**
     * Буква талона
     */
    final String ticketPrefix;


    public Queue(String name, String ticketPrefix) {
        super(name);
        this.ticketPrefix = ticketPrefix;
    }

    public Queue(String id, String name, String ticketPrefix) {
        super(id, name);
        this.ticketPrefix = ticketPrefix;
    }

}
