package ru.aritmos.model.visit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Serdeable
@JsonFormat
public enum VisitEvent {
    CREATED, PLACED_IN_QUEUE, CALLED, RECALLED, START_SERVING,
    STOP_SERVING, NO_SHOW, END, TRANSFER_TO_USER_POOL, TRANSFER_TO_SERVICE_POINT_POOL,
    BACK_TO_QUEUE,BACK_TO_SERVICE_POINT_POOL, TRANSFER_TO_QUEUE, ADD_SERVICE, ADDED_DELIVERED_SERVICE,
    ADDED_DELIVERED_SERVICE_RESULT, ADDED_SERVICE_RESULT,ADDED_MARK,DELETED_MARK,
    DELETED_DELIVERED_SERVICE_RESULT, DELETED_SERVICE_RESULT, VISIT_END_TRANSACTION, VISIT_TRANSFER_FROM_QUEUE, VISIT_DELETED;
    VisitState visitState;

    @JsonFormat
    public ZonedDateTime dateTime;

    @JsonGetter
    public ZonedDateTime getDateTime() {
        return dateTime;
    }

    @Getter
    final Map<String, String> parameters = new HashMap<>();
    private static final List<VisitEvent> newTransactionEvents = new ArrayList<>() {
        {

            add(BACK_TO_QUEUE);


        }
    };

    public static Boolean isNewOfTransaction(VisitEvent visitEvent) {
        return VisitEvent.newTransactionEvents.stream().anyMatch(am -> am.equals(visitEvent));
    }


    private static final Map<VisitEvent, TransactionCompletionStatus> transactionStatus = Map.ofEntries(
            Map.entry(BACK_TO_QUEUE, TransactionCompletionStatus.BACK_TO_QUEUE)

    );

    public static TransactionCompletionStatus getStatus(VisitEvent visitEvent) {
        if (transactionStatus.containsKey(visitEvent)) {
            return transactionStatus.get(visitEvent);
        }
        return null;
    }

    private static final Map<VisitEvent, List<VisitEvent>> nextEvents = Map.ofEntries(

            Map.entry(CREATED, List.of(PLACED_IN_QUEUE, CALLED, RECALLED,ADDED_MARK,DELETED_MARK)),
            Map.entry(PLACED_IN_QUEUE, List.of(CALLED,RECALLED, VISIT_DELETED,ADDED_MARK,DELETED_MARK,TRANSFER_TO_SERVICE_POINT_POOL)),
            Map.entry(CALLED, List.of(RECALLED,CALLED, START_SERVING, NO_SHOW, BACK_TO_QUEUE,ADDED_MARK,DELETED_MARK,PLACED_IN_QUEUE)),
            Map.entry(RECALLED, List.of(RECALLED, START_SERVING, NO_SHOW, BACK_TO_QUEUE,ADDED_MARK,DELETED_MARK,PLACED_IN_QUEUE)),
            Map.entry(START_SERVING, List.of(BACK_TO_QUEUE,BACK_TO_SERVICE_POINT_POOL,ADDED_MARK,DELETED_MARK, STOP_SERVING, END, TRANSFER_TO_SERVICE_POINT_POOL, TRANSFER_TO_USER_POOL, TRANSFER_TO_QUEUE, ADD_SERVICE, ADDED_DELIVERED_SERVICE, ADDED_SERVICE_RESULT,ADDED_MARK,DELETED_MARK)),
            Map.entry(STOP_SERVING, List.of(PLACED_IN_QUEUE,BACK_TO_SERVICE_POINT_POOL,ADDED_MARK,DELETED_MARK, END, VISIT_END_TRANSACTION, BACK_TO_QUEUE,ADDED_MARK,DELETED_MARK)),
            Map.entry(NO_SHOW, List.of(ADDED_MARK,DELETED_MARK)),
            Map.entry(END, List.of(ADDED_MARK,DELETED_MARK)),
            Map.entry(BACK_TO_SERVICE_POINT_POOL, List.of(CALLED,RECALLED, VISIT_DELETED,ADDED_MARK,DELETED_MARK,TRANSFER_TO_SERVICE_POINT_POOL)),
            Map.entry(TRANSFER_TO_SERVICE_POINT_POOL, List.of(CALLED,RECALLED, VISIT_DELETED,ADDED_MARK,DELETED_MARK,TRANSFER_TO_SERVICE_POINT_POOL)),
            Map.entry(TRANSFER_TO_USER_POOL, List.of(CALLED,ADDED_MARK,DELETED_MARK)),
            Map.entry(VISIT_END_TRANSACTION, List.of(PLACED_IN_QUEUE,ADDED_MARK,DELETED_MARK)),
            Map.entry(VISIT_TRANSFER_FROM_QUEUE, List.of(CALLED,ADDED_MARK,DELETED_MARK)),
            Map.entry(ADDED_DELIVERED_SERVICE, List.of(ADDED_DELIVERED_SERVICE,ADDED_MARK,DELETED_MARK, STOP_SERVING, ADDED_SERVICE_RESULT, ADDED_DELIVERED_SERVICE_RESULT)),
            Map.entry(ADDED_SERVICE_RESULT, List.of(ADDED_DELIVERED_SERVICE,ADDED_MARK,DELETED_MARK, STOP_SERVING, ADDED_SERVICE_RESULT, ADDED_DELIVERED_SERVICE)),
            Map.entry(ADDED_MARK, List.of(ADDED_DELIVERED_SERVICE,START_SERVING, STOP_SERVING, ADDED_SERVICE_RESULT, ADDED_DELIVERED_SERVICE,ADDED_MARK,DELETED_MARK)),
            Map.entry(DELETED_MARK, List.of(ADDED_DELIVERED_SERVICE,START_SERVING,ADDED_MARK,DELETED_MARK, STOP_SERVING, ADDED_SERVICE_RESULT, ADDED_DELIVERED_SERVICE,ADDED_MARK,DELETED_MARK)),
            Map.entry(ADDED_DELIVERED_SERVICE_RESULT, List.of(ADDED_DELIVERED_SERVICE,ADDED_MARK,DELETED_MARK, STOP_SERVING, ADDED_SERVICE_RESULT, ADDED_DELIVERED_SERVICE,ADDED_MARK,DELETED_MARK)),
            // TRANSFER_TO_SERVICE_POOL, List.of(CALLED),
            // TRANSFER_TO_SERVICE_POINT_POOL, List.of(CALLED),
            // BACK_TO_QUEUE, List.of(CALLED),
            //  TRANSFER_TO_QUEUE, List.of(CALLED),
            Map.entry(ADD_SERVICE, List.of(PLACED_IN_QUEUE, BACK_TO_QUEUE, STOP_SERVING, END, TRANSFER_TO_SERVICE_POINT_POOL, TRANSFER_TO_USER_POOL, TRANSFER_TO_QUEUE, ADD_SERVICE,ADDED_MARK,DELETED_MARK))
    );

    private static final Map<VisitEvent, VisitState> visitStateMap = Map.ofEntries(
            Map.entry(CREATED, VisitState.CREATED),
            Map.entry(PLACED_IN_QUEUE, VisitState.WAITING_IN_QUEUE),
            Map.entry(CALLED, VisitState.CALLED),
            Map.entry(RECALLED, VisitState.CALLED),
            Map.entry(START_SERVING, VisitState.SERVING),
            Map.entry(ADDED_DELIVERED_SERVICE, VisitState.SERVING),
            Map.entry(ADDED_SERVICE_RESULT, VisitState.SERVING),
            Map.entry(ADDED_MARK, VisitState.SERVING),
            Map.entry(DELETED_MARK, VisitState.SERVING),
            Map.entry(STOP_SERVING, VisitState.CREATED),
            Map.entry(NO_SHOW, VisitState.END),
            Map.entry(END, VisitState.END),
            Map.entry(BACK_TO_QUEUE, VisitState.WAITING_IN_QUEUE),
            Map.entry(BACK_TO_SERVICE_POINT_POOL, VisitState.WAITING_IN_SERVICE_POOL),
            Map.entry(TRANSFER_TO_SERVICE_POINT_POOL,VisitState.WAITING_IN_SERVICE_POOL),
            Map.entry(VISIT_DELETED,VisitState.END),
            Map.entry(ADDED_DELIVERED_SERVICE_RESULT, VisitState.SERVING));

    //TRANSFER_TO_SERVICE_POINT_POOL, List.of(VisitState.WAITING_IN_SP_POOL),
    //BACK_TO_QUEUE, List.of(VisitState.WAITING_IN_QUEUE)
    // TRANSFER_TO_QUEUE, List.of(VisitState.WAITING_IN_QUEUE),
    //  ADD_SERVICE, List.of()
    //);

    public boolean canBeNext(VisitEvent next) {
        return nextEvents.get(this).stream().anyMatch(e -> e.equals(next));
    }

    public VisitState getState() {
        visitState = visitStateMap.get(this);
        return visitState;
    }

}

