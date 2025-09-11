package ru.aritmos.model.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Токен авторизации Keycloak. */
@Data
@Serdeable
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Token {
  String access_token;
  Long expires_at; // время жизни токена в секундах
  Long refresh_expires_in; // в секундах
  String refresh_token;
  String session_state; // идентификатор сессии по которому можно проводить синхронизации
  Boolean enabled;
  String id_token;
}
