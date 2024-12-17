package ru.aritmos.keycloack.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

@Data
@Serdeable
@SuppressWarnings("unused")
public class Credentials {
  String username;
  String password;
}
