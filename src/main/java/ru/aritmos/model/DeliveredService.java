package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Оказанная услуга */
@EqualsAndHashCode(callSuper = false)
@Data
@Serdeable
@SuppressWarnings("unused")
public class DeliveredService extends BasedService implements Cloneable {
  /** Идентификаторы подходящих услуг */
  List<String> serviceIds = new ArrayList<>();

  /**
   * Создаёт описание оказанной услуги.
   *
   * @param id идентификатор услуги
   * @param name наименование услуги
   */
  public DeliveredService(String id, String name) {
    super(id, name);
  }

  @Override
  public DeliveredService clone() {
    DeliveredService clone = (DeliveredService) super.clone();

    clone.serviceIds = new ArrayList<>();
    if (this.outcome != null) {
      clone.outcome = this.outcome.clone();
    }
    return clone;
  }
}
