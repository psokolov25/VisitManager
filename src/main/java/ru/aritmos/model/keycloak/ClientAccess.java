package ru.aritmos.model.keycloak;

import io.micronaut.serde.annotation.Serdeable;
import java.util.HashMap;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Serdeable
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientAccess {
  HashMap<String, List<String>> roles;
}
