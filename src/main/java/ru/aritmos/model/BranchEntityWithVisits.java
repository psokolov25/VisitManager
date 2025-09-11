package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import ru.aritmos.model.visit.Visit;

/** Сущность отделения, содержащая перечень визитов */
@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable
@Introspected
@AllArgsConstructor
@NoArgsConstructor
public class BranchEntityWithVisits extends BranchEntity {
  /** Визиты */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  List<Visit> visits = new ArrayList<>();

  /**
   * Конструктор сущности отделения с визитами с авто‑генерацией идентификатора.
   *
   * @param name наименование
   */
  public BranchEntityWithVisits(String name) {
    this.id = UUID.randomUUID().toString();
    this.name = name;
  }

  /**
   * Конструктор сущности отделения с визитами.
   *
   * @param id идентификатор
   * @param name наименование
   */
  public BranchEntityWithVisits(String id, String name) {
    this.id = id;
    this.name = name;
  }
}
