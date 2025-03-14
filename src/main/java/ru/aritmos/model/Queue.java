package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Очередь */
@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable
@Introspected
public final class Queue extends BranchEntityWithVisits {
  /** Буква талона */
  final String ticketPrefix;

  /** Счетчик талонов */
  Integer ticketCounter = 0;

  /** Стандартное время ожидания */
  Integer waitingSL;

  public Queue(String name, String ticketPrefix,Integer waitingSL) {
    super(name);
    this.ticketPrefix = ticketPrefix;
    this.waitingSL = waitingSL;
  }

  public Queue(String id, String name, String ticketPrefix,Integer waitingSL) {
    super(id, name);
    this.ticketPrefix = ticketPrefix;
    this.waitingSL = waitingSL;
  }
}
