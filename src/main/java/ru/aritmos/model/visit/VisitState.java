package ru.aritmos.model.visit;

import io.micronaut.serde.annotation.Serdeable;

/** Перечень возможных состояний визита. */
@Serdeable
public enum VisitState {
  /** Создан. */
  CREATED,
  /** Ожидает в очереди. */
  WAITING_IN_QUEUE,
  /** Вызван. */
  CALLED,
  /** Обслуживается. */
  SERVING,
  /** Ожидает в пуле сотрудника. */
  WAITING_IN_USER_POOL,
  /** Ожидает в пуле точки обслуживания. */
  WAITING_IN_SERVICE_POOL,
  /** Завершён. */
  END
}
