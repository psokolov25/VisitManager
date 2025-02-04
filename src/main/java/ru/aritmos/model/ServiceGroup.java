package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import lombok.*;

@EqualsAndHashCode(callSuper = false)
@Data
@Serdeable
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("unused")
public class ServiceGroup extends BranchEntity {

  /** Идентификаторы услуг, входящих в группу */
  List<String> serviceIds;

  String segmentationRuleId;

  public ServiceGroup(String id, String name, List<String> serviceIds, String branchId) {
    super(id, name, branchId);
    this.serviceIds = serviceIds;
  }
}
