package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.util.HashMap;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Услуга */
@Serdeable
@EqualsAndHashCode(callSuper = true)
@Data
@Introspected
public class Service extends BasedService {

  /** Нормативное время обслуживания */
  Integer servingSL;

  /** Связанная очередь */
  String linkedQueueId;

  /** Флаг доступности */
  Boolean isAvailable;

  /** Список идентификаторов оказанных услуг */
  HashMap<String, DeliveredService> deliveredServices = new HashMap<>();

  public Service(String key, String name, Integer servingSL, String linkedQueueId) {

    super(key, name);
    this.servingSL = servingSL;
    this.linkedQueueId = linkedQueueId;
    this.isAvailable = true;
  }
}
