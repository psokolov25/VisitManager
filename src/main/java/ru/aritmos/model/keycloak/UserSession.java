package ru.aritmos.model.keycloak;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Serdeable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
  String login;
  Long create_session;
  Long last_update;
  UserToken userToken;
  String sid;
}
