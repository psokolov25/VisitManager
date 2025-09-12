package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Информация о пользователе.
 */
@Data
@Serdeable
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("unused")
public class UserInfo {
  /** Уникальный идентификатор пользователя (sub). */
  String sub;

  /** Роли и группы пользователя в Keycloak. */
  RealmAccess realm_access;

  /** Полное имя пользователя. */
  String name;

  /** Электронная почта пользователя. */
  String email;

  /** Дополнительное описание пользователя. */
  String description;
}
