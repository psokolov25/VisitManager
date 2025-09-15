package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import lombok.*;

/** Группа услуг отделения. */
@EqualsAndHashCode(callSuper = false)
@Data
@Serdeable
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("unused")
public class ServiceGroup extends BranchEntity {

  /** Идентификаторы услуг, входящих в группу */
  List<String> serviceIds;

  /** Идентификатор правила сегментации для группы услуг. */
  String segmentationRuleId;

  /** Идентификатор правила сегментации по параметрам визита. */
  String segmentationParameterRuleId;

  /**
   * Конструктор группы услуг.
   *
   * @param id идентификатор
   * @param name наименование
   * @param serviceIds идентификаторы услуг
   * @param branchId идентификатор отделения
   */
  public ServiceGroup(String id, String name, List<String> serviceIds, String branchId) {
    super(id, name, branchId);
    this.serviceIds = serviceIds;
  }
}
