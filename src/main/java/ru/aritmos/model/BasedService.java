package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import java.util.HashMap;
import lombok.*;

/** Базовая услуга */
@EqualsAndHashCode(callSuper = false)
@Data
@Serdeable
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("unused")
public class BasedService extends BranchEntity implements Cloneable {
  HashMap<String, Outcome> possibleOutcomes = new HashMap<>();

  /** Итог обслуживания */
  Outcome outcome;

  public BasedService(String id, String name) {
    super(id, name);
  }

  @Override
  public BasedService clone() {
    BasedService clone = (BasedService) super.clone();
    clone.possibleOutcomes.putAll(this.possibleOutcomes);
    if (this.outcome != null) {
      clone.outcome = this.outcome.clone();
    }
    return clone;
  }
}
