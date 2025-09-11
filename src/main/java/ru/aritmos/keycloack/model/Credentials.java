package ru.aritmos.keycloack.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

/** Учетные данные пользователя для авторизации. */
@Data
@Serdeable
@SuppressWarnings("unused")
public class Credentials {
  /** Логин пользователя. */
  String username;
  /** Пароль пользователя. */
  String password;
}
