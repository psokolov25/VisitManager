package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Serdeable
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("unused")
public class UserInfo {
  String sub;
  RealmAccess realm_access;
  String name;
  String email;
  String description;

}
