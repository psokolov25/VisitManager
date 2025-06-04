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

  @SuppressWarnings("all")
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

  public Service clone() {
    Service service = (Service) super.clone();

    Service clone = new Service();
    clone.serviceGroupId = this.serviceGroupId;
    clone.id = this.id;
    clone.name = this.name;
    clone.branchId = this.branchId;
    clone.servingSL = this.servingSL;
    clone.linkedQueueId = this.linkedQueueId;
    clone.isAvailable = this.isAvailable;
    clone.deliveredServices.putAll(this.deliveredServices);
    clone.possibleOutcomes = new HashMap<>();
    clone.possibleOutcomes.putAll(this.possibleOutcomes);

    if (this.outcome != null) {
      clone.outcome = this.outcome.clone();
    }

    return clone;
  }
}
