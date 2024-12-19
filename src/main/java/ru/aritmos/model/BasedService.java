package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** Базовая услуга */
@EqualsAndHashCode(callSuper = false)
@Data
@Serdeable
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("unused")
public class BasedService extends BranchEntity {
  HashMap<String, Outcome> possibleOutcomes = new HashMap<>();

  /** Итог обслуживания */
  Outcome outcome;

  public BasedService(String id, String name) {
    super(id, name);
  }
}
