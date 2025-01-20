package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Итог обслуживания */
@Data
@EqualsAndHashCode(callSuper = false)
@Serdeable
public class Outcome extends BranchEntity implements Cloneable {
  /** Код итога обслуживания */
  Long code;

  public Outcome(String id, String name) {
    super(id, name);
  }

    @Override
    public Outcome clone() {
        Outcome clone = (Outcome) super.clone();
        clone.code = this.code;
        clone.name = this.name;

        return clone;
    }
}
