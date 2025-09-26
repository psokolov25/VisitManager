package ru.aritmos.model.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Информация о пользователе из Keycloak. */
@Data
@Serdeable
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfo {
  /** Уникальный идентификатор пользователя (sub). */
  String sub;

  /** Роли пользователя в реалме. */
  RealmAccess realm_access;

  /** Полное имя пользователя. */
  String name;

  /** Электронная почта пользователя. */
  String email;

  /** Дополнительное описание пользователя. */
  String description;
}
