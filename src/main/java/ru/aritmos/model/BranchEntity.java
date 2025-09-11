package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/** Сущность отделения */
@Data
@Introspected
@Jacksonized
@SuperBuilder(builderMethodName = "baseBuilder")
@NonFinal
@AllArgsConstructor
@NoArgsConstructor
@Serdeable
public class BranchEntity implements Cloneable {
  /** Идентификатор сущности. */
  String id;

  /** Наименование. */
  String name;

  /** Идентификатор филиала. */
  String branchId;

  /**
   * Конструктор сущности с авто‑генерацией идентификатора.
   *
   * @param name наименование сущности
   */
  public BranchEntity(String name) {
    this.id = UUID.randomUUID().toString();
    this.name = name;
  }

  /**
   * Конструктор сущности.
   *
   * @param id идентификатор
   * @param name наименование сущности
   */
  public BranchEntity(String id, String name) {
    this.id = id;
    this.name = name;
  }

  @Override
  public BranchEntity clone() {
    try {
      BranchEntity clone = (BranchEntity) super.clone();
      clone.id = this.getId();
      clone.name = this.getName();
      clone.branchId = this.getBranchId();

      return clone;
    } catch (CloneNotSupportedException e) {
      throw new AssertionError();
    }
  }
}
