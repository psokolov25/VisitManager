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

  /**
   * Конструктор рабочего профиля с автогенерацией идентификатора.
   *
   * @param name наименование профиля
   */
  public WorkProfile(String name) {
    super(name);
  }

  /**
   * Конструктор рабочего профиля.
   *
   * @param id идентификатор профиля
   * @param name наименование профиля
   */
  public WorkProfile(String id, String name) {
    super(id, name);
  }
}
