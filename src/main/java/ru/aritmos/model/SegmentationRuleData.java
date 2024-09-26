package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.util.HashMap;
import lombok.*;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@Introspected
@SuperBuilder
@Serdeable
@AllArgsConstructor
@NoArgsConstructor
public class SegmentationRuleData extends BranchEntity {
  /** Перечень свойств визита, которые должны присутствовать в визите для срабатывания правила */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  HashMap<String, String> visitProperty;

  /** Идентификатор группы услуг */
  String serviceGroupId;

  /** Идентификатор очереди */
  String queueId;
}
