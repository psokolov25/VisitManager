package ru.aritmos.model.visit;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public enum TransactionCompletionStatus {
    OK, NO_SHOW, REMOVED_BY_EMP, REMOVED_BY_CUSTOMER, REMOVED_BY_RESET, BACK_TO_QUEUE, TRANSFER_TO_QUEUE, TRANSFER_TO_SERVICE_POINT, TRANSFER_TO_SERVICE, TRANSFER_TO_STAFF, BY_LOGOUT
}
