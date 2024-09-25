package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Рабочий профиль */
@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable
@Introspected
public class WorkProfile extends BranchEntity {
  /** Идентификаторы очередей */
  List<String> queueIds = new ArrayList<>();

  public WorkProfile(String name) {
    super(name);
  }

  public WorkProfile(String id, String name) {
    super(id, name);
  }
}
