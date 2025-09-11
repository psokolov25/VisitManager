package ru.aritmos.keycloack.model;

import io.micronaut.serde.annotation.Serdeable;
import java.util.HashMap;
import java.util.List;
import lombok.Data;

/**
 * Сокращённая информация о пользователе из Keycloak.
 */
@Data
@Serdeable
@SuppressWarnings("unused")
public class KeyCloackUser {
  /** Электронная почта пользователя. */
  private String email;
  /** Отображаемое имя пользователя. */
  private String name;
  /** Доступ к ресурсам клиента (role mappings). */
  private HashMap<String, HashMap<String, List<String>>> resource_access;
  /** Предпочитаемое имя пользователя (username). */
  private String preferred_username;
  /** Роли в realm. */
  private List<String> roles;
  /** Произвольные атрибуты пользователя. */
  private HashMap<String, Object> attributes;
  /** Список групп, в которых состоит пользователь. */
  private List<String> groups;
}
