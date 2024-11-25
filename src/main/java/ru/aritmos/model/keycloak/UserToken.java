package ru.aritmos.model.keycloak;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Serdeable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserToken {
  UserInfo user;
  Token tokenInfo;
}
