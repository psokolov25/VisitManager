package ru.aritmos.model.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Информация о сессии пользователя. */
@Data
@Serdeable
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSession {
  /** Логин пользователя. */
  String login;

  /** Время создания сессии (epoch millis). */
  Long create_session;

  /** Время последнего обновления сессии (epoch millis). */
  Long last_update;

  /** Привязанные к сессии данные пользователя и токенов. */
  UserToken userToken;

  /** Идентификатор сессии (sid). */
  String sid;
}
