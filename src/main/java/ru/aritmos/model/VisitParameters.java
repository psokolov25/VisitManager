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
public class VisitParameters {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  ArrayList<String> serviceIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  HashMap<String, String> parameters = new HashMap<>();
}
