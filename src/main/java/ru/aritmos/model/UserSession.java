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
  String login;
  Long create_session;
  Long last_update;
  UserToken userToken;
    String sid;
    @Builder.Default
    HashMap<String, String> params = new HashMap<>();
}
