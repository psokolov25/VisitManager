package ru.aritmos.model.visit;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public enum VisitState {
    QUEUE_DETECTING, WAITING_IN_QUEUE, CALLED, SERVING, WAITING_IN_USER_POOL, WAITING_IN_SERVICE_POOL, WAITING_IN_SP_POOL, END

}
