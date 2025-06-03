package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.util.HashMap;
import lombok.*;

/** Услуга */
@Serdeable
@EqualsAndHashCode(callSuper = true)
@Data
@Introspected
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("unused")
public class Service extends BasedService implements Cloneable {

  /** Нормативное время обслуживания */
  Integer servingSL;

  /** Связанная очередь */
  String linkedQueueId;

  /** Флаг доступности */
  Boolean isAvailable;

  String serviceGroupId;

  /** Список идентификаторов оказанных услуг */
  HashMap<String, DeliveredService> deliveredServices = new HashMap<>();

  public Service(String key, String name, Integer servingSL, String linkedQueueId) {

    super(key, name);
    this.servingSL = servingSL;
    this.linkedQueueId = linkedQueueId;
    this.isAvailable = true;
  }

  @Override
  public Service clone() {
    Service clone = (Service) super.clone();
    clone.servingSL = this.servingSL;
    clone.linkedQueueId = this.linkedQueueId;
    clone.isAvailable = this.isAvailable;
    clone.deliveredServices.putAll(this.deliveredServices);
    if (this.outcome != null) {
      clone.outcome = this.outcome.clone();
    }

    return clone;
  }
}
