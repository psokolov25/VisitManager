package ru.aritmos.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Пользователь */
@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable
@Introspected
@AllArgsConstructor
@Builder(toBuilder = true)
@SuppressWarnings("unused")
public class User extends BranchEntityWithVisits {
  /** Имя */
  @JsonInclude(value = ALWAYS)
  String firstName;

  /** Фамилия */
  @JsonInclude(value = ALWAYS)
  String lastName;

  /** Электронная почта */
  String email;

  /** Идентификатор текущего рабочего профиля */
  String currentWorkProfileId;

  /** Идентификатор точки обслуживания */
  String servicePointId;

  /** Идентификатор текущего отделения */
  String branchId;

  /** Время начала последнего перерыва */
  ZonedDateTime lastBreakStartTime;

  /** Время окончания последнего перерыва */
  ZonedDateTime lastBreakEndTime;

  /** Причина перерыва */
  String lastBreakReason;

  /** Точка обслуживания, в которой сотрудник работал до перерыва */
  String lastServicePointId;

  /** Отделение, в которой сотрудник работал до перерыва */
  String lastBranchId;

  @Override
  @JsonProperty("username")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getName() {
    return this.name;
  }

  public User(String id, String name) {
    super(id, name);
  }

  public User(String name) {
    super(name);
  }

  @JsonProperty
  public Boolean isOnBreak() {
    return this.lastBreakStartTime != null && this.lastBreakEndTime == null;
  }

  /**
   * Время длительности последнего перерыва в секундах, если перерыв еще не закончен - показывается
   * разницу между началом перерыва и текущем времени
   *
   * @return время длительности последнего перерыва
   */
  @JsonGetter
  Long getLastBreakDuration() {
    final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());
    if (lastBreakStartTime != null) {
      return unit.between(
          this.lastBreakStartTime,
          Objects.requireNonNullElseGet(lastBreakEndTime, ZonedDateTime::now));
    } else {
      return 0L;
    }
  }
}
