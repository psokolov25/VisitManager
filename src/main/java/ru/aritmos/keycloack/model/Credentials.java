package ru.aritmos.keycloack.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Serdeable
public class Credentials {
  String login;
  String password;
}
