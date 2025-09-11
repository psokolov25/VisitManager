package ru.aritmos.model;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import lombok.*;
import org.keycloak.representations.idm.GroupRepresentation;
import ru.aritmos.keycloack.service.KeyCloackClient;

/** Пользователь */
@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable
@Introspected
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@SuppressWarnings({"unused", "redundant"})
public class User extends BranchEntityWithVisits {
  /** Флаг указывающий на административные права пользователя */
  Boolean isAdmin;

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

  /** Служба получения данных о пользователе из Keycloak */
  @JsonIgnore @Getter KeyCloackClient keyCloackClient;

  /** Точка обслуживания, в которой сотрудник работал до перерыва */
  String lastServicePointId;

  /** Отделение, в которой сотрудник работал до перерыва */
  String lastBranchId;

  List<GroupRepresentation> allBranches;

  /**
   * Конструктор пользователя.
   *
   * @param id идентификатор пользователя
   * @param name имя пользователя
   * @param keyCloackClient клиент Keycloak для получения ролей/групп
   */
  @JsonIgnore
  public User(String id, String name, KeyCloackClient keyCloackClient) {

    super(id, name);
    if (keyCloackClient != null) {
      this.keyCloackClient = keyCloackClient;
      this.isAdmin = keyCloackClient.isUserModuleTypeByUserName(name, "admin");
      this.allBranches = keyCloackClient.getAllBranchesOfUser(name);
    }
  }

  /**
   * Конструктор пользователя с автогенерацией идентификатора.
   *
   * @param name имя пользователя
   * @param keyCloackClient клиент Keycloak для получения ролей/групп
   */
  @JsonIgnore
  public User(String name, KeyCloackClient keyCloackClient) {
    super(name);
    if (keyCloackClient != null) {
      this.keyCloackClient = keyCloackClient;
      this.isAdmin = keyCloackClient.isUserModuleTypeByUserName(name, "admin");
      this.allBranches = keyCloackClient.getAllBranchesOfUser(name);
    }
  }

  /**
   * Имя пользователя.
   *
   * @return имя
   */
  @Override
  @JsonProperty("name")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public String getName() {
    return this.name;
  }

  /**
   * Признак, что пользователь сейчас на перерыве.
   *
   * @return true, если перерыв начат и не завершён
   */
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
