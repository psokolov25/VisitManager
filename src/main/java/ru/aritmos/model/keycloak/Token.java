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
  /** Токен доступа. */
  String access_token;

  /** Время жизни access‑token в секундах. */
  Long expires_at;

  /** Время жизни refresh‑token в секундах. */
  Long refresh_expires_in;

  /** Refresh‑token. */
  String refresh_token;

  /** Идентификатор сессии для синхронизации. */
  String session_state;

  /** Признак активности токена. */
  Boolean enabled;

  /** ID‑token. */
  String id_token;
}
