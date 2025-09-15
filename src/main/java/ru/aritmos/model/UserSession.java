package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Информация о сессии пользователя.
 */
@Data
@Serdeable
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("unused")
public class UserSession {
  /** Логин пользователя. */
  String login;

  /** Время создания сессии (epoch millis). */
  Long create_session;

  /** Время последнего обновления сессии (epoch millis). */
  Long last_update;

  /** Информация о токенах пользователя. */
  UserToken userToken;

  /** Идентификатор сессии Keycloak (sid). */
  String sid;

  /** Дополнительные параметры сессии. */
  @Builder.Default
  HashMap<String, String> params = new HashMap<>();
}
