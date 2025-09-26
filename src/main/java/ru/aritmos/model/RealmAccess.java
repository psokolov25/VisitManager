package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.representations.idm.GroupRepresentation;

/** Доступы пользователя: роли, группы и модули. */
@Data
@Serdeable
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("unused")
public class RealmAccess {
  /** Список ролей пользователя в Keycloak realm. */
  List<String> roles;

  /** Привязка ролей к модулям: роль -> список модулей. */
  Map<String, List<String>> rolesWithModules;

  /** Филиалы, к которым относится пользователь. */
  List<GroupRepresentation> branches;

  /** Группы пользователя. */
  List<GroupRepresentation> groups;

  /** Наименования доступных модулей. */
  List<String> modules;
}
