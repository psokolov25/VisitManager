package ru.aritmos.model.visit;

import com.fasterxml.jackson.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.model.EntryPoint;
import ru.aritmos.model.Service;
import ru.aritmos.service.BranchService;

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

    private Transaction currentTransaction;


    public void setTransaction(VisitEvent event, EventService eventService, BranchService branchService) {
        ArrayList<VisitEvent> events = new ArrayList<>();
        events.add(event);
        if (this.currentTransaction != null) {

            if (this.transactions == null) {
                this.transactions = new ArrayList<>();
            }
            this.transactions.add(currentTransaction);


        }

        this.currentTransaction = (new Transaction(this));
        this.currentTransaction.getVisitEvents().addAll(events);
        //this.currentTransaction.addEvent(event, eventService);

        this.status = event.getState().name();
        eventService.send("stat", false, Event.builder()
                .eventDate(ZonedDateTime.now())
                .eventType("TRANSACTION_" + event.name())
                .body(new VisitForTransaction(this, branchService))
                .build());

    }

    public void updateTransaction(VisitEvent event, EventService eventService, BranchService branchService) {


        if (this.currentTransaction == null) {

            this.setCurrentTransaction(new Transaction(this));
            List<VisitEvent> events = this.getCurrentTransaction().getVisitEvents();
            this.getCurrentTransaction().getVisitEvents().addAll(events);
            this.getCurrentTransaction().addEvent(event, eventService);

            this.status = event.getState().name();
        } else {
            this.getCurrentTransaction().startDateTime = this.getCreateDateTime();
            if (this.getQueueId() != null) {
                this.getCurrentTransaction().queueId = this.getQueueId();
            }
            if (this.currentService != null) {
                this.getCurrentTransaction().service = this.getCurrentService();
            }
            if (this.getServicePointId() != null) {
                this.getCurrentTransaction().servicePointId = this.getServicePointId();
            }
            this.getCurrentTransaction().startServingDateTime = this.getStartServingDateTime();
            this.getCurrentTransaction().callDateTime = this.getCallDateTime();
            this.getCurrentTransaction().endDateTime = this.getEndDateTime();
            this.getCurrentTransaction().employeeId = this.getUserId();

            //this.currentTransaction.getVisitEvents().addAll(events);
            this.getCurrentTransaction().addEvent(event, eventService);

            this.status = event.getState().name();
        }

        eventService.send("stat", false, Event.builder()
                .eventDate(ZonedDateTime.now())
                .eventType("TRANSACTION_" + event.name())
                .body(new VisitForTransaction(this, branchService))
                .build());

    }

    @JsonIgnore
    ArrayList<Transaction> transactions;
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

    public Long getTotalWaitingTime() {
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
            if(VisitEvent.isNewOfTransaction(f)) {
                subresult = new ArrayList<>();
                result.add(subresult);
            }


            VisitEventDateTime visitEventDateTime = VisitEventDateTime.builder()
                    .visitEvent(f)
                    .eventDateTime(f.dateTime)
                    .parameters(f.getParameters())
                    .build();
            subresult.add(visitEventDateTime);


        }
        return result;
    }

}
