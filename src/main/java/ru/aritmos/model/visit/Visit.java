package ru.aritmos.model.visit;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.EntryPoint;
import ru.aritmos.model.Service;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Визит
 */
@Data
@EqualsAndHashCode
@Serdeable
@Builder(toBuilder = true)

@ToString
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
    String ticketId;
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
    ZonedDateTime createDate;
    /**
     * Дата обновления
     */
    ZonedDateTime updateDate;
    /**
     * Дата перевода
     */
    ZonedDateTime transferDate;
    /**
     * Дата вызова
     */
    ZonedDateTime callDate;
    /**
     * Дата начала обслуживания
     */
    ZonedDateTime startServingDate;
    /**
     * Дата завершения обслуживания
     */
    ZonedDateTime servedDate;
    /**
     * Дата завершения визита
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    ZonedDateTime endDate;
    /**
     * Версия визита
     */
    Integer version;
    /**
     * Идентификатор точки обслуживания
     */
    String servicePointId;
    /**
     * Логин вызвавшего сотрудника
     */
    String userName;
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

    Transaction currentTransaction;

    public void setTransaction(VisitEvent event, EventService eventService) {
        ArrayList<VisitEvent> events=new ArrayList<>();
        if (this.currentTransaction != null) {
            if(currentTransaction.getEvents()!=null)
            {
                events = new ArrayList<>(currentTransaction.getEvents());
            }
            this.transactions.add(currentTransaction);


        }

        this.currentTransaction=(new Transaction(ZonedDateTime.now(),this));
        this.currentTransaction.getEvents().addAll(events);
        this.currentTransaction.addEvent(event,eventService);

        this.status = event.getState().name();
        eventService.send("*", false, Event.builder()
                .eventDate(ZonedDateTime.now())
                .eventType("TRANSACTION_"+event.name())
                .body(this)
                .build());

    }

    List<Transaction> transactions;

    @JsonGetter

    public Long getWaitingTime() {
        final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());

        waitingTime = unit.between(transferDate, ZonedDateTime.now());
        return waitingTime;
    }

    /**
     * Время ожидания в последней очереди в секундах
     */
    Long waitingTime;

    @JsonGetter

    public Long getTotalWaitingTime() {
        final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());

        waitingTime = unit.between(createDate, ZonedDateTime.now());
        return waitingTime;
    }

    /**
     * Общее время с создания визита в секундах
     */
    Long totalWaitingTime;
    /**
     * Время обслуживания в секундах
     */
    Long servingTime;

    @JsonGetter
    public Long getServingTime() {
        final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());
        if (this.startServingDate != null) {
            servingTime = unit.between(this.startServingDate, Objects.requireNonNullElseGet(this.servedDate, ZonedDateTime::now));
            return servingTime;
        }
        return 0L;
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


}
