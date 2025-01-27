package ru.aritmos.model.visit;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
/* Перечень состояний окончания транзакции */
public enum TransactionCompletionStatus {
  OK,
  NO_SHOW,
  PLACED_IN_QUEUE,
  STOP_SERVING,
  REMOVED_BY_EMP,
  REMOVED_BY_CUSTOMER,
  REMOVED_BY_RESET,
  TRANSFER_TO_QUEUE,

  TRANSFER_TO_SERVICE_POINT_POOL,

  TRANSFER_TO_USER_POOL,
  LOGOUT,
  CLOSE_SP
}
