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
public class DeliveredService extends BasedService {
  /** Идентификаторы подходящих услуг */
  List<String> serviceIds = new ArrayList<>();

  public DeliveredService(String id, String name) {
    super(id, name);
  }
}
