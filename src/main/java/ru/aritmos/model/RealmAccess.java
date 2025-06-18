package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import java.util.HashMap;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.representations.idm.GroupRepresentation;

@Data
@Serdeable
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("unused")
public class RealmAccess {
  List<String> roles;
  HashMap<String, List<String>> rolesWithModules;
  List<GroupRepresentation> branches;
  List<GroupRepresentation> groups;
  List<String> modules;
}
