package ru.aritmos.model.tiny;

import com.fasterxml.jackson.annotation.JsonGetter;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import ru.aritmos.model.Service;

/** Сокращенное представление визита */
@Data
@EqualsAndHashCode
@Serdeable
@Builder(toBuilder = true)
@Introspected
@ToString
public class TinyVisit {
  /** Дата и время создания визита */
  ZonedDateTime createDate;

  /** Идентификатор визита */
  String id;

  /** Номер талона */
  String ticketId;

  /** Текущая услуга */
  Service currentService;

  /** Дата и время перевода */
  ZonedDateTime transferDate;

  /** Время ожидания в секундах */
  Long waitingTime;

  /** Суммарное время ожидания в секундах. */
  Long totalWaitingTime;

  /**
   * Расчёт текущего времени ожидания в секундах.
   *
   * @return время ожидания в секундах
   */
  @JsonGetter
  public Long getWaitingTime() {
    final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());
    waitingTime =
        unit.between(transferDate != null ? transferDate : createDate, ZonedDateTime.now());
    return waitingTime;
  }

  /**
   * Расчёт суммарного времени ожидания в секундах.
   *
   * @return суммарное время ожидания
   */
  @JsonGetter
  public Long getTotalWaitingTime() {
    final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());
    totalWaitingTime = unit.between(getCreateDate(), ZonedDateTime.now());
    return totalWaitingTime;
  }
}
