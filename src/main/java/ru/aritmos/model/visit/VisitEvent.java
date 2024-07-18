package ru.aritmos.model.visit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Serdeable

public enum VisitEvent {
    CREATED, PLACED_IN_QUEUE, CALLED, RECALLED, START_SERVING,
    STOP_SERVING, NO_SHOW, END, TRANSFER_TO_USER_POOL,  TRANSFER_TO_SERVICE_POINT_POOL,
    BACK_TO_QUEUE, TRANSFER_TO_QUEUE, ADD_SERVICE,ADDED_DELIVERED_SERVICE,
    ADDED_DELIVERED_SERVICE_RESULT,ADDED_SERVICE_RESULT,
    DELETED_DELIVERED_SERVICE_RESULT,DELETED_SERVICE_RESULT;
    VisitState visitState;

    @JsonFormat
    public ZonedDateTime dateTime;
    @JsonGetter
    public ZonedDateTime getDateTime() {
        return dateTime;
    }
    @Getter
    final Map <String, String> parameters=new HashMap<>();

    private static final Map<VisitEvent, List<VisitEvent>> nextEvents = Map.of(
            CREATED, List.of(PLACED_IN_QUEUE,CALLED),
            PLACED_IN_QUEUE, List.of(CALLED),
            CALLED, List.of(RECALLED, START_SERVING, NO_SHOW, BACK_TO_QUEUE),
            RECALLED, List.of(RECALLED, START_SERVING, NO_SHOW, BACK_TO_QUEUE),
            START_SERVING, List.of(BACK_TO_QUEUE, STOP_SERVING, END, TRANSFER_TO_SERVICE_POINT_POOL, TRANSFER_TO_USER_POOL,  TRANSFER_TO_QUEUE, ADD_SERVICE),
            STOP_SERVING, List.of(PLACED_IN_QUEUE),
            NO_SHOW, List.of(),
            END, List.of(),
            TRANSFER_TO_USER_POOL, List.of(CALLED),
           // TRANSFER_TO_SERVICE_POOL, List.of(CALLED),
           // TRANSFER_TO_SERVICE_POINT_POOL, List.of(CALLED),
           // BACK_TO_QUEUE, List.of(CALLED),
          //  TRANSFER_TO_QUEUE, List.of(CALLED),
            ADD_SERVICE, List.of(PLACED_IN_QUEUE, BACK_TO_QUEUE, STOP_SERVING, END, TRANSFER_TO_SERVICE_POINT_POOL, TRANSFER_TO_USER_POOL,  TRANSFER_TO_QUEUE, ADD_SERVICE)
    );

    private static final Map<VisitEvent, VisitState> visitStateMap = Map.of(
            CREATED, VisitState.CREATED,
            PLACED_IN_QUEUE, VisitState.WAITING_IN_QUEUE,
            CALLED, VisitState.CALLED,
            RECALLED, VisitState.CALLED,
            START_SERVING, VisitState.SERVING,
            STOP_SERVING, VisitState.CREATED,
            NO_SHOW, VisitState.END,
            END, VisitState.END,
            BACK_TO_QUEUE,VisitState.WAITING_IN_QUEUE);
            //TRANSFER_TO_SERVICE_POINT_POOL, List.of(VisitState.WAITING_IN_SP_POOL),
            //BACK_TO_QUEUE, List.of(VisitState.WAITING_IN_QUEUE)
             // TRANSFER_TO_QUEUE, List.of(VisitState.WAITING_IN_QUEUE),
            //  ADD_SERVICE, List.of()
    //);

    public boolean canBeNext(VisitEvent next){
        return nextEvents.get(this).stream().anyMatch(e -> e.equals(next));
    }

    public VisitState getState() {
        return visitState = visitStateMap.get(this);
    }

}
