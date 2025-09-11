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

/** Параметры визита. */
@Data
@Introspected
@Serdeable
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VisitParameters {
  /** Идентификаторы услуг. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Builder.Default
  ArrayList<String> serviceIds = new ArrayList<>();

  /** Параметры визита. */
  @Builder.Default
  @JsonInclude(JsonInclude.Include.NON_NULL)
  HashMap<String, String> parameters = new HashMap<>();
}
