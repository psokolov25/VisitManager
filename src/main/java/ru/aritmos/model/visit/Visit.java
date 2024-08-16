package ru.aritmos.model.visit;

import com.fasterxml.jackson.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import ru.aritmos.model.EntryPoint;
import ru.aritmos.model.Service;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Визит
 */
@Data
@EqualsAndHashCode
@Serdeable
@Builder(toBuilder = true)

@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Visit {
    /**
     * Идентификатор визита
     */
    String id;
    /**
     * Статус визита
     */
    String status;
    /**
     * Талон
     */
    String ticket;
    /**
     * Идентификатор отделения
     */
    String branchId;

    /**
     * Название отделения
     */
    String branchName;
    /**
     * Дата создания визита
     */
    ZonedDateTime createDateTime;

    /**
     * Дата перевода
     */
    ZonedDateTime transferDateTime;

    /**
     * Дата возвращения
     */
    ZonedDateTime returnDateTime;

    /**
     * Дата вызова
     */
    ZonedDateTime callDateTime;
    /**
     * Дата начала обслуживания
     */
    ZonedDateTime startServingDateTime;
    /**
     * Дата завершения обслуживания
     */
    ZonedDateTime servedDateTime;
    /**
     * Дата завершения визита
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    ZonedDateTime endDateTime;

    /**
     * Идентификатор точки обслуживания
     */
    String servicePointId;
    /**
     * Логин вызвавшего сотрудника
     */
    String userName;
    /**
     * Id вызвавшего сотрудника
     */
    String userId;

    /**
     * Лимит ожидания после возвращения визита в очередь
     */
    Long returnTimeDelay;
    /**
     * Массив не обслуженных услуг
     */

    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<Service> unservedServices;
    /**
     * Обслуженные услуги
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<Service> servedServices;






    /**
     * Время ожидания в последней очереди в секундах
     */
    Long waitingTime;

    @JsonGetter

    public Long getWaitingTime() {
        final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());

        waitingTime = unit.between(this.getReturnDateTime() != null ? this.getReturnDateTime() : this.getCreateDateTime() != null ? this.getCreateDateTime() : ZonedDateTime.now(), this.getStartServingDateTime() != null ? this.getStartServingDateTime() : ZonedDateTime.now());
        return waitingTime;

    }

    /**
     * Время прошедшее от возвращения в очередь
     */
    Long returningTime;

    @JsonGetter
    public Long getReturningTime() {
        final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());
        if (this.returnDateTime != null) {
            returningTime = unit.between(this.returnDateTime, ZonedDateTime.now());
            return returningTime;
        }
        return 0L;

    }


    /**
     * Общее время с создания визита в секундах
     */
    Long visitLifeTime;

    @JsonGetter

    public Long getVisitLifeTime() {
        final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());

        visitLifeTime = unit.between(this.getCreateDateTime() != null ? this.getCreateDateTime() : ZonedDateTime.now(), this.getEndDateTime() != null ? this.getEndDateTime() : ZonedDateTime.now());
        return visitLifeTime;
    }

    /**
     * Время обслуживания в секундах
     */
    Long servingTime;


    @JsonGetter
    public Long getServingTime() {
        final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());

        servingTime = unit.between(this.startServingDateTime != null ? this.getStartServingDateTime() : ZonedDateTime.now(), this.getServedDateTime() != null ? this.getServedDateTime() : ZonedDateTime.now());
        return servingTime;

    }

    /**
     * Текущая услуга
     */

    Service currentService;

    /**
     * Дополнительные параметры визита
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    HashMap<String, Object> parameterMap;
    /**
     * Признак печати талона
     */
    Boolean printTicket;
    /**
     * Точка создания визита талона
     */
    EntryPoint entryPoint;
    /**
     * Идентификатор очереди
     */
    String queueId;

    List<VisitEvent> visitEvents;

    @JsonGetter
    public ArrayList<ArrayList<VisitEventDateTime>> getEvents() {

        ArrayList<ArrayList<VisitEventDateTime>> result = new ArrayList<>();
        ArrayList<VisitEventDateTime> subresult =new ArrayList<>();
        result.add(subresult);
        for (VisitEvent f:this.visitEvents) {

            VisitEventDateTime visitEventDateTime = VisitEventDateTime.builder()
                    .visitEvent(f)
                    .eventDateTime(f.dateTime)
                    .parameters(f.getParameters())
                    .transactionCompletionStatus(VisitEvent.isNewOfTransaction(f)?VisitEvent.getStatus(f):null)
                    .build();
            subresult.add(visitEventDateTime);
            if(VisitEvent.isNewOfTransaction(f)) {
                subresult = new ArrayList<>();
                result.add(subresult);
            }




        }
        return result;
    }

}

