package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.util.ArrayList;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Introspected
@Serdeable
@Builder
@AllArgsConstructor
@NoArgsConstructor
/*
 * Параметры визита
 */
public class VisitParameters {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Builder.Default
  /*
   * Идентификаторы услуг
   */
  ArrayList<String> serviceIds = new ArrayList<>();

  @Builder.Default
  @JsonInclude(JsonInclude.Include.NON_NULL)
  /*
   * Параметры визита
   */
  HashMap<String, String> parameters = new HashMap<>();
}
