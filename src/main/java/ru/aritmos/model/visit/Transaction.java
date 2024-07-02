package ru.aritmos.model.visit;

import io.micronaut.http.HttpStatus;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;
import ru.aritmos.exceptions.BusinessException;
import ru.aritmos.model.Service;
import ru.aritmos.model.Visit;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Serdeable
@Data
public class Transaction {
    String id;


    Service service;
    String queueId;
    ZonedDateTime startTime;
    ZonedDateTime callTime;
    ZonedDateTime startServingTime;
    ZonedDateTime transferTime;
    ZonedDateTime endTime;
    String servicePointId;
    String employeeId;
    int waitingSLA;
    int servingSLA;
    TransactionCompletionStatus completionStatus;

    List<VisitEvent> events;



    public Transaction(ZonedDateTime startTime, Visit visit) {
        this.id = UUID.randomUUID().toString();
        this.events = new ArrayList<>();
        this.startTime = startTime;
        if(visit!=null) {
            this.endTime = visit.getEndDate() != null ? visit.getEndDate() : null;
            this.queueId = visit.getQueueId();
            this.employeeId = visit.getUserName();
            this.service = visit.getCurrentService();
            this.servicePointId = visit.getServicePointId();
            this.startServingTime = visit.getStartServingDate();
            this.callTime = visit.getCallDate();
            this.transferTime = visit.getTransferDate();
        }

    }

    public void addEvent(VisitEvent event, EventService eventService) {
        if (events.isEmpty()) {
            if (!event.equals(VisitEvent.CREATED))
                throw new BusinessException("wasn't early created", eventService, HttpStatus.CONFLICT);
            else events.add(event);
            eventService.send("*", true, Event.builder()
                    .eventDate(ZonedDateTime.now())
                    .eventType(event.name())
                    .body(this).build()
            );
        } else {
            VisitEvent prevEvent = events.get(events.size() - 1);
            if (prevEvent.canBeNext(event)) {
                events.add(event);
                eventService.send("*", true, Event.builder()
                        .eventDate(ZonedDateTime.now())
                        .eventType(event.name())
                        .body(this).build()
                );

            } else throw new BusinessException("can't be next status", eventService, HttpStatus.CONFLICT);


        }
        event.getState();
    }

}

