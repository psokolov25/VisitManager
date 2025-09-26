package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Пользователь и связанные с ним токены. */
@Serdeable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("unused")
public class UserToken {
  /** Информация о пользователе. */
  UserInfo user;

  /** Сведения о выданных токенах. */
  Token tokenInfo;
}
