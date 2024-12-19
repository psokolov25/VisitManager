package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Точка входа */
@SuppressWarnings("unused")
@EqualsAndHashCode(callSuper = false)
@Data
@Serdeable
@Introspected
public class EntryPoint extends BranchEntity {
  /** Идентификатор принтера */
  Entity printer;
}
