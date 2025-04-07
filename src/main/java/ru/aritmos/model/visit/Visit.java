package ru.aritmos.model.visit;

import com.fasterxml.jackson.annotation.*;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import ru.aritmos.model.EntryPoint;
import ru.aritmos.model.Mark;
import ru.aritmos.model.Service;

/** Визит */
@Data
@EqualsAndHashCode
@Serdeable
@Builder(toBuilder = true)
@Introspected
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("unused")
public class Visit {
  /** Идентификатор визита */
  String id;

  /** Статус визита */
  String status;

  /** Талон */
  String ticket;

  /** Идентификатор отделения */
  String branchId;

  /** Название отделения */
  String branchName;

  /** Префикс отделения */
  @JsonInclude(JsonInclude.Include.ALWAYS)
  String branchPrefix;
  /** Путь к отделению */
  @JsonInclude(JsonInclude.Include.ALWAYS)
  String branchPath;
  /** Дата создания визита */
  ZonedDateTime createDateTime;

  /** Дата перевода */
  @Schema(nullable = true)
  ZonedDateTime transferDateTime;

  /** Дата возвращения */
  @Schema(nullable = true)
  ZonedDateTime returnDateTime;

  /** Дата вызова */
  @Schema(nullable = true)
  ZonedDateTime callDateTime;

  /** Дата начала обслуживания */
  @Schema(nullable = true)
  ZonedDateTime startServingDateTime;

  /** Дата завершения обслуживания */
  @Schema(nullable = true)
  ZonedDateTime servedDateTime;

  /** Дата завершения визита */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(nullable = true)
  ZonedDateTime endDateTime;

  /** Идентификатор точки обслуживания */
  @Schema(nullable = true)
  String servicePointId;

  /** Логин вызвавшего сотрудника */
  @Schema(nullable = true)
  String userName;

  /** Id вызвавшего сотрудника */
  @Schema(nullable = true)
  String userId;

  /** Id сотрудника, в пуле которого располагается визит */
  @Schema(nullable = true)
  String poolUserId;

  /** Id точка обслуживания, в пуле которого располагается визит */
  @Schema(nullable = true)
  String poolServicePointId;

  /** Массив не обслуженных услуг */
  List<Service> unservedServices;

  /** Обслуженные услуги */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  List<Service> servedServices;

  /** Марки визита */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  List<Mark> visitMarks;

  /** Заметки визита */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  List<Mark> visitNotes;

  /** Время ожидания в последней очереди в секундах */
  Long waitingTime;

  /** Время прошедшее от возвращения в очередь */
  @Schema(nullable = true)
  Long returningTime;

  /** Общее время с создания визита в секундах */
  @Schema(nullable = true)
  Long visitLifeTime;

  /** Время обслуживания в секундах */
  @Schema(nullable = true)
  Long servingTime;

  /** Текущая услуга */
  Service currentService;

  /** Дополнительные параметры визита */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  HashMap<String, String> parameterMap;

  /** Признак печати талона */
  Boolean printTicket;

  /** Точка создания визита талона */
  EntryPoint entryPoint;

  /** Идентификатор очереди */
  @Schema(nullable = true)
  String queueId;

  /** Массив событий */
  List<VisitEvent> visitEvents;

  @JsonIgnore List<VisitEventInformation> visitEventInformationList;

  /** Лимит ожидания после возвращения визита в очередь */
  private Long returnTimeDelay;

  @JsonGetter
  public Long getWaitingTime() {
    final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());

    waitingTime =
        unit.between(
            this.getReturnDateTime() != null
                ? this.getReturnDateTime()
                : this.transferDateTime != null
                    ? this.transferDateTime
                    : this.getCreateDateTime() != null
                        ? this.getCreateDateTime()
                        : ZonedDateTime.now(),
            this.getStartServingDateTime() != null
                ? this.getStartServingDateTime()
                : ZonedDateTime.now());
    return waitingTime;
  }

  @JsonGetter
  public Long getReturningTime() {
    final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());
    if (this.getReturnDateTime() != null) {
      returningTime = unit.between(this.getReturnDateTime(), ZonedDateTime.now());
      return returningTime;
    }
    return 0L;
  }

  @JsonGetter
  public Long getVisitLifeTime() {
    final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());

    visitLifeTime =
        unit.between(
            this.getCreateDateTime() != null ? this.getCreateDateTime() : ZonedDateTime.now(),
            this.getEndDateTime() != null ? this.getEndDateTime() : ZonedDateTime.now());
    return visitLifeTime;
  }

  @JsonGetter
  public Long getServingTime() {
    final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());

    servingTime =
        unit.between(
            this.startServingDateTime != null
                ? this.getStartServingDateTime()
                : ZonedDateTime.now(),
            this.getServedDateTime() != null ? this.getServedDateTime() : ZonedDateTime.now());
    return servingTime;
  }

  @JsonGetter
  public ArrayList<ArrayList<VisitEventInformation>> getEvents() {

    ArrayList<ArrayList<VisitEventInformation>> result = new ArrayList<>();
    ArrayList<VisitEventInformation> subresult = new ArrayList<>();
    result.add(subresult);
    for (VisitEventInformation f : this.getVisitEventInformationList()) {

      VisitEventInformation visitEventDateTime =
          VisitEventInformation.builder()
              .visitEvent(f.visitEvent)
              .eventDateTime(f.getEventDateTime())
              .parameters(f.getParameters())
              .transactionCompletionStatus(
                  VisitEvent.isNewOfTransaction(f.visitEvent)
                      ? VisitEvent.getStatus(f.visitEvent)
                      : null)
              .build();
      subresult.add(visitEventDateTime);
      if (VisitEvent.isNewOfTransaction(f.visitEvent)) {
        subresult = new ArrayList<>();
        result.add(subresult);
      }
    }
    return result;
  }
}
