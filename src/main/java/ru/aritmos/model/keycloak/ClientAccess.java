package ru.aritmos.model.keycloak;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Доступы клиента Keycloak: роли по клиентам. */
@Data
@Serdeable
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("unused")
public class ClientAccess {
  /** Карта ролей клиента: id клиента -> список ролей. */
  Map<String, List<String>> roles;
}
