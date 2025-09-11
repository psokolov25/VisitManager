package ru.aritmos.model.visit;

import io.micronaut.serde.annotation.Serdeable;

/** Перечень состояний завершения транзакции визита. */
@Serdeable
public enum TransactionCompletionStatus {
  /** Успешное завершение. */
  OK,
  /** Не явился. */
  NO_SHOW,
  /** Размещён в очереди. */
  PLACED_IN_QUEUE,
  /** Прекращено обслуживание. */
  STOP_SERVING,
  /** Удалён сотрудником. */
  REMOVED_BY_EMP,
  /** Удалён клиентом. */
  REMOVED_BY_CUSTOMER,
  /** Сброшен системой. */
  REMOVED_BY_RESET,
  /** Переведён в очередь. */
  TRANSFER_TO_QUEUE,
  /** Переведён в пул точки обслуживания. */
  TRANSFER_TO_SERVICE_POINT_POOL,
  /** Переведён в пул сотрудника. */
  TRANSFER_TO_USER_POOL,
  /** Выход из системы. */
  LOGOUT,
  /** Закрытие точки обслуживания. */
  CLOSE_SP
}
