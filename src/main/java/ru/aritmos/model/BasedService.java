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
  /** Возможные исходы оказания услуги (по коду исхода). */
  HashMap<String, Outcome> possibleOutcomes = new HashMap<>();

  /** Текущий исход оказания услуги. */
  Outcome outcome;

  /**
   * Конструктор услуги с заданным идентификатором и именем.
   *
   * @param id идентификатор услуги
   * @param name наименование услуги
   */
  public BasedService(String id, String name) {
    super(id, name);
  }

  /**
   * Создаёт копию базовой услуги.
   *
   * @return глубокая копия услуги с перенесёнными исходами
   */
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
