package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Пользователь */
@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable
@Introspected
@SuppressWarnings({"unused", "RedundantSuppression"})
public class User extends BranchEntityWithVisits {
  /** Имя */
  @JsonInclude(JsonInclude.Include.ALWAYS)
  String firstName;

  /** Фамилия */
  @JsonInclude(JsonInclude.Include.ALWAYS)
  String lastName;

  /** Электронная почта */
  String email;

  /** Идентификатор текущего рабочего профиля */
  String currentWorkProfileId;

  /** Идентификатор точки обслуживания */
  String servicePointId;

  /** Идентификатор текущего отделения */
  String branchId;

  public User(String id, String name) {
    super(id, name);
  }

  public User(String name) {
    super(name);
  }
}
