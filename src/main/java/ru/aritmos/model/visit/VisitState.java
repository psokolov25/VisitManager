package ru.aritmos.model.visit;

import io.micronaut.serde.annotation.Serdeable;

/* Перечень возможных состояний */
@Serdeable
public enum VisitState {
  CREATED,
  WAITING_IN_QUEUE,
  CALLED,
  SERVING,
  WAITING_IN_USER_POOL,
  WAITING_IN_SERVICE_POOL,
  END



}
