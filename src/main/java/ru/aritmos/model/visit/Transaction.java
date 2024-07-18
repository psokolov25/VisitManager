package ru.aritmos.model.visit;

import com.fasterxml.jackson.annotation.JsonGetter;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.http.HttpStatus;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.Service;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Serdeable
@Data
@Introspected
public class Transaction {
    String id;


    Service service;
    String queueId;
    ZonedDateTime startDateTime;
    ZonedDateTime callDateTime;
    ZonedDateTime startServingDateTime;
    ZonedDateTime endDateTime;
    String servicePointId;
    String employeeId;
    int waitingSL;
    int servingSL;
    TransactionCompletionStatus completionStatus;


    List<VisitEvent> visitEvents;

    @JsonGetter
    public ArrayList<VisitEventDateTime> getEvents() {

        ArrayList<VisitEventDateTime> result = new ArrayList<>();
        this.visitEvents.forEach(f -> {
            VisitEventDateTime visitEventDateTime = VisitEventDateTime.builder()
                    .visitEvent(f)
                    .eventDateTime(f.dateTime)
                    .build();
            result.add(visitEventDateTime);
        });
        return result;
    }

    Long waitingTime;
    @JsonGetter

    public Long getWaitingTime() {
        final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());

        waitingTime = unit.between(this.getStartDateTime()!=null?this.getStartDateTime():ZonedDateTime.now(), this.getStartServingDateTime()!=null?this.getStartServingDateTime():ZonedDateTime.now());
        return waitingTime;

    }


    /**
     * Время c создания визита
     */
    Long visitLifeTime;

    @JsonGetter

    public Long getVisitLifeTime() {
        final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());
        if(startDateTime!=null) {
            visitLifeTime = unit.between(startDateTime, Objects.requireNonNullElseGet(this.endDateTime, ZonedDateTime::now));
            return visitLifeTime;
        }
        else
        {
            return 0L;
        }
    }


    /**
     * Время обслуживания в секундах
     */
    Long servingTime;


    @JsonGetter
    public Long getServingTime() {
        final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());

        servingTime = unit.between(this.startServingDateTime!=null?this.getStartServingDateTime():ZonedDateTime.now(),this.getEndDateTime()!=null?this.getEndDateTime():ZonedDateTime.now());
        return servingTime;

    }
    public Transaction(Visit visit) {
        //Branch currentBranch=branchService.getBranch(visit.getBranchId());


        this.id = UUID.randomUUID().toString();
        this.visitEvents = new ArrayList<>();



        if (visit != null) {
            this.startDateTime = visit.getCreateDateTime();
            this.queueId = visit.getQueueId();

            this.service = visit.getCurrentService();
            this.servicePointId = visit.getServicePointId();
            this.startServingDateTime = visit.getStartServingDateTime();
            this.callDateTime = visit.getCallDateTime();
            this.endDateTime = visit.getEndDateTime();
            this.employeeId=visit.getUserId();
        }

    }

    public void addEvent(VisitEvent event, EventService eventService) {
        if (visitEvents.isEmpty()) {
            if (!event.equals(VisitEvent.CREATED))
                throw new BusinessException("wasn't early created", eventService, HttpStatus.CONFLICT);
            else visitEvents.add(event);

        } else {
            VisitEvent prevEvent = visitEvents.get(visitEvents.size() - 1);
            if (prevEvent.canBeNext(event)) {
                visitEvents.add(event);


            } else throw new BusinessException("can't be next status", eventService, HttpStatus.CONFLICT);


        }
        event.getState();
    }

}

