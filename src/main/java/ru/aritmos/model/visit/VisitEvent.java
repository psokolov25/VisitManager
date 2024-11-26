package ru.aritmos.model.visit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import io.micronaut.serde.annotation.Serdeable;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Serdeable
@JsonFormat
public enum VisitEvent {
  /** Визит создан */
  CREATED,
  /** Визит размещен в очередь после создания */
  PLACED_IN_QUEUE,
  /** Визит вызыве */
  CALLED,
  /** Визит повторно вызван */
  RECALLED,
  /** Визит натат обслуживаться (текущая услуга) */
  START_SERVING,
  /** Визит завершен обслуживаться (текущая услуга) */
  STOP_SERVING,
  /** Визит завершен так как клиент не пришел */
  NO_SHOW,
  /** Визит завершен */
  END,
  /** Визит переведен в пул сотрудника */
  TRANSFER_TO_USER_POOL,
  /** Визит переведен в пул точки обслуживания */
  TRANSFER_TO_SERVICE_POINT_POOL,
  /** Визит переведен в очередь */
  TRANSFER_TO_QUEUE,
  /** Визит возвращен в пул сотрудника */
  BACK_TO_USER_POOL,
  /** Визит возвращен в очередь */
  BACK_TO_QUEUE,
  /** Визит возвращен в пул точки обслуживания */
  BACK_TO_SERVICE_POINT_POOL,
  /** В визит добавлена услуга */
  ADD_SERVICE,
  /** В визит добавлена фактическая услуга */
  ADDED_DELIVERED_SERVICE,
  /** В визите удалена фактическая услуга */
  DELETED_DELIVERED_SERVICE,
  /** В визит добавлен итог фактической услуги */
  ADDED_DELIVERED_SERVICE_RESULT,
  /** В визит добавлен итог услуги */
  ADDED_SERVICE_RESULT,
  /** В визит добавлена пометка */
  ADDED_MARK,
  /** В визит удалена пометка */
  DELETED_MARK,
  /** В визит удален итог фактической услуги */
  DELETED_DELIVERED_SERVICE_RESULT,
  /** В визит удален итог услуги */
  DELETED_SERVICE_RESULT,
  /** У визита закночена транзакция */
  VISIT_END_TRANSACTION,
  /** Визит переведен из очереди */
  VISIT_TRANSFER_FROM_QUEUE,
  /** Визит удален */
  DELETED;

  private static final List<VisitEvent> frontEndVisitEvents =
      List.of(
          CREATED,
          PLACED_IN_QUEUE,
          CALLED,
          RECALLED,
          START_SERVING,
          STOP_SERVING,
          NO_SHOW,
          END,
          TRANSFER_TO_USER_POOL,
          TRANSFER_TO_SERVICE_POINT_POOL,
          BACK_TO_USER_POOL,
          BACK_TO_QUEUE,
          BACK_TO_SERVICE_POINT_POOL,
          TRANSFER_TO_QUEUE,
          ADD_SERVICE,
          ADDED_DELIVERED_SERVICE,
          DELETED_DELIVERED_SERVICE,
          ADDED_DELIVERED_SERVICE_RESULT,
          ADDED_SERVICE_RESULT,
          ADDED_MARK,
          DELETED_MARK,
          DELETED_DELIVERED_SERVICE_RESULT,
          DELETED_SERVICE_RESULT,
          DELETED);
  private static final List<VisitEvent> newTransactionEvents =
      List.of(
          STOP_SERVING,
          NO_SHOW,
          PLACED_IN_QUEUE,
          TRANSFER_TO_SERVICE_POINT_POOL,
          DELETED,
          TRANSFER_TO_USER_POOL);

  private static final Map<VisitEvent, TransactionCompletionStatus> transactionStatus =
      Map.ofEntries(
          Map.entry(STOP_SERVING, TransactionCompletionStatus.STOP_SERVING),
          Map.entry(NO_SHOW, TransactionCompletionStatus.NO_SHOW),
          Map.entry(DELETED, TransactionCompletionStatus.REMOVED_BY_EMP),
          Map.entry(PLACED_IN_QUEUE, TransactionCompletionStatus.TRANSFER_TO_QUEUE),
          Map.entry(TRANSFER_TO_QUEUE, TransactionCompletionStatus.TRANSFER_TO_QUEUE),
          Map.entry(BACK_TO_USER_POOL, TransactionCompletionStatus.TRANSFER_TO_STAFF),
          Map.entry(
              TRANSFER_TO_SERVICE_POINT_POOL,
              TransactionCompletionStatus.TRANSFER_TO_SERVICE_POINT_POOL),
          Map.entry(TRANSFER_TO_USER_POOL, TransactionCompletionStatus.TRANSFER_TO_USER_POOL));
  private static final List<VisitEvent> notSendToStat = List.of(RECALLED);
  private static final Map<VisitEvent, List<VisitEvent>> nextEvents =
      Map.ofEntries(
          Map.entry(CREATED, List.of(PLACED_IN_QUEUE, CALLED, RECALLED, ADDED_MARK, DELETED_MARK)),
          Map.entry(
              PLACED_IN_QUEUE,
              List.of(
                  CALLED,
                  RECALLED,
                  DELETED,
                  ADDED_MARK,
                  DELETED_MARK,
                  TRANSFER_TO_SERVICE_POINT_POOL,
                  TRANSFER_TO_USER_POOL,
                  TRANSFER_TO_QUEUE)),
          Map.entry(
              CALLED,
              List.of(
                  RECALLED,
                  CALLED,
                  START_SERVING,
                  NO_SHOW,
                  BACK_TO_QUEUE,
                  ADDED_MARK,
                  DELETED_MARK,
                  PLACED_IN_QUEUE)),
          Map.entry(
              RECALLED,
              List.of(
                  RECALLED,
                  STOP_SERVING,
                  START_SERVING,
                  NO_SHOW,
                  BACK_TO_QUEUE,
                  ADDED_MARK,
                  DELETED_MARK,
                  PLACED_IN_QUEUE)),
          Map.entry(
              START_SERVING,
              List.of(
                  RECALLED,
                  NO_SHOW,
                  BACK_TO_USER_POOL,
                  BACK_TO_QUEUE,
                  BACK_TO_SERVICE_POINT_POOL,
                  ADDED_MARK,
                  DELETED_MARK,
                  STOP_SERVING,
                  END,
                  TRANSFER_TO_SERVICE_POINT_POOL,
                  TRANSFER_TO_USER_POOL,
                  TRANSFER_TO_QUEUE,
                  ADD_SERVICE,
                  ADDED_DELIVERED_SERVICE,
                  DELETED_DELIVERED_SERVICE,
                  ADDED_SERVICE_RESULT,
                  ADDED_MARK,
                  DELETED_MARK)),
          Map.entry(
              STOP_SERVING,
              List.of(
                  RECALLED,
                  BACK_TO_USER_POOL,
                  PLACED_IN_QUEUE,
                  BACK_TO_SERVICE_POINT_POOL,
                  TRANSFER_TO_SERVICE_POINT_POOL,
                  TRANSFER_TO_USER_POOL,
                  ADDED_MARK,
                  DELETED_MARK,
                  END,
                  VISIT_END_TRANSACTION,
                  BACK_TO_QUEUE,
                  ADDED_MARK,
                  DELETED_MARK)),
          Map.entry(NO_SHOW, List.of(ADDED_MARK, DELETED_MARK)),
          Map.entry(END, List.of(ADDED_MARK, DELETED_MARK)),
          Map.entry(
              BACK_TO_SERVICE_POINT_POOL,
              List.of(
                  PLACED_IN_QUEUE,
                  CALLED,
                  RECALLED,
                  DELETED,
                  ADDED_MARK,
                  DELETED_MARK,
                  TRANSFER_TO_SERVICE_POINT_POOL)),
          Map.entry(
              BACK_TO_USER_POOL,
              List.of(
                  PLACED_IN_QUEUE,
                  CALLED,
                  RECALLED,
                  DELETED,
                  ADDED_MARK,
                  DELETED_MARK,
                  TRANSFER_TO_SERVICE_POINT_POOL)),
          Map.entry(
              BACK_TO_QUEUE,
              List.of(
                  PLACED_IN_QUEUE,
                  CALLED,
                  RECALLED,
                  DELETED,
                  ADDED_MARK,
                  DELETED_MARK,
                  TRANSFER_TO_SERVICE_POINT_POOL)),
          Map.entry(
              TRANSFER_TO_SERVICE_POINT_POOL,
              List.of(
                  PLACED_IN_QUEUE,
                  CALLED,
                  RECALLED,
                  DELETED,
                  ADDED_MARK,
                  DELETED_MARK,
                  TRANSFER_TO_USER_POOL,
                  TRANSFER_TO_QUEUE,
                  TRANSFER_TO_SERVICE_POINT_POOL)),
          Map.entry(
              TRANSFER_TO_USER_POOL,
              List.of(
                  PLACED_IN_QUEUE,
                  CALLED,
                  RECALLED,
                  DELETED,
                  ADDED_MARK,
                  DELETED_MARK,
                  TRANSFER_TO_USER_POOL,
                  TRANSFER_TO_QUEUE,
                  TRANSFER_TO_SERVICE_POINT_POOL)),
          Map.entry(
              TRANSFER_TO_QUEUE,
              List.of(
                  PLACED_IN_QUEUE,
                  CALLED,
                  RECALLED,
                  DELETED,
                  ADDED_MARK,
                  DELETED_MARK,
                  TRANSFER_TO_USER_POOL,
                  TRANSFER_TO_QUEUE,
                  TRANSFER_TO_SERVICE_POINT_POOL)),
          Map.entry(VISIT_END_TRANSACTION, List.of(PLACED_IN_QUEUE, ADDED_MARK, DELETED_MARK)),
          Map.entry(VISIT_TRANSFER_FROM_QUEUE, List.of(CALLED, ADDED_MARK, DELETED_MARK)),
          Map.entry(
              DELETED_DELIVERED_SERVICE,
              List.of(
                  ADDED_DELIVERED_SERVICE,
                  ADDED_MARK,
                  DELETED_MARK,
                  STOP_SERVING,
                  ADDED_SERVICE_RESULT,
                  ADDED_DELIVERED_SERVICE_RESULT,
                  DELETED_DELIVERED_SERVICE)),
          Map.entry(
              ADDED_DELIVERED_SERVICE,
              List.of(
                  ADDED_DELIVERED_SERVICE,
                  ADDED_MARK,
                  DELETED_MARK,
                  STOP_SERVING,
                  ADDED_SERVICE_RESULT,
                  ADDED_DELIVERED_SERVICE_RESULT,
                  DELETED_DELIVERED_SERVICE)),
          Map.entry(
              ADDED_SERVICE_RESULT,
              List.of(
                  ADDED_DELIVERED_SERVICE,
                  ADDED_MARK,
                  DELETED_MARK,
                  STOP_SERVING,
                  ADDED_SERVICE_RESULT,
                  ADDED_DELIVERED_SERVICE)),
          Map.entry(
              ADDED_MARK,
              List.of(
                  ADDED_DELIVERED_SERVICE,
                  START_SERVING,
                  STOP_SERVING,
                  ADDED_SERVICE_RESULT,
                  ADDED_DELIVERED_SERVICE,
                  ADDED_MARK,
                  DELETED_MARK)),
          Map.entry(
              DELETED_MARK,
              List.of(
                  ADDED_DELIVERED_SERVICE,
                  START_SERVING,
                  ADDED_MARK,
                  DELETED_MARK,
                  STOP_SERVING,
                  ADDED_SERVICE_RESULT,
                  ADDED_DELIVERED_SERVICE,
                  ADDED_MARK,
                  DELETED_MARK)),
          Map.entry(
              ADDED_DELIVERED_SERVICE_RESULT,
              List.of(
                  ADDED_DELIVERED_SERVICE,
                  ADDED_MARK,
                  DELETED_MARK,
                  STOP_SERVING,
                  ADDED_SERVICE_RESULT,
                  ADDED_DELIVERED_SERVICE,
                  ADDED_MARK,
                  DELETED_MARK)),
          // TRANSFER_TO_SERVICE_POOL, List.of(CALLED),
          // TRANSFER_TO_SERVICE_POINT_POOL, List.of(CALLED),
          // BACK_TO_QUEUE, List.of(CALLED),
          //  TRANSFER_TO_QUEUE, List.of(CALLED),
          Map.entry(
              ADD_SERVICE,
              List.of(
                  PLACED_IN_QUEUE,
                  BACK_TO_QUEUE,
                  STOP_SERVING,
                  END,
                  TRANSFER_TO_SERVICE_POINT_POOL,
                  TRANSFER_TO_USER_POOL,
                  TRANSFER_TO_QUEUE,
                  ADD_SERVICE,
                  ADDED_MARK,
                  DELETED_MARK)));
  private static final Map<VisitEvent, VisitState> visitStateMap =
      Map.ofEntries(
          Map.entry(CREATED, VisitState.CREATED),
          Map.entry(PLACED_IN_QUEUE, VisitState.WAITING_IN_QUEUE),
          Map.entry(CALLED, VisitState.CALLED),
          Map.entry(RECALLED, VisitState.CALLED),
          Map.entry(START_SERVING, VisitState.SERVING),
          Map.entry(ADDED_DELIVERED_SERVICE, VisitState.SERVING),
          Map.entry(ADDED_SERVICE_RESULT, VisitState.SERVING),
          Map.entry(ADD_SERVICE, VisitState.SERVING),
          Map.entry(ADDED_MARK, VisitState.SERVING),
          Map.entry(DELETED_MARK, VisitState.SERVING),
          Map.entry(STOP_SERVING, VisitState.CREATED),
          Map.entry(NO_SHOW, VisitState.END),
          Map.entry(END, VisitState.END),
          Map.entry(BACK_TO_QUEUE, VisitState.WAITING_IN_QUEUE),
          Map.entry(BACK_TO_SERVICE_POINT_POOL, VisitState.WAITING_IN_SERVICE_POOL),
          Map.entry(TRANSFER_TO_SERVICE_POINT_POOL, VisitState.WAITING_IN_SERVICE_POOL),
          Map.entry(TRANSFER_TO_USER_POOL, VisitState.WAITING_IN_USER_POOL),
          Map.entry(TRANSFER_TO_QUEUE, VisitState.WAITING_IN_QUEUE),
          Map.entry(DELETED, VisitState.END),
          Map.entry(BACK_TO_USER_POOL, VisitState.WAITING_IN_USER_POOL),
          Map.entry(ADDED_DELIVERED_SERVICE_RESULT, VisitState.SERVING),
          Map.entry(DELETED_DELIVERED_SERVICE, VisitState.SERVING));
  @Getter final Map<String, String> parameters = new HashMap<>();
  @JsonFormat public ZonedDateTime dateTime;
  VisitState visitState;

  public static Boolean isNewOfTransaction(VisitEvent visitEvent) {
    return VisitEvent.newTransactionEvents.stream().anyMatch(am -> am.equals(visitEvent));
  }

  public static Boolean isIgnoredInStat(VisitEvent visitEvent) {
    return notSendToStat.contains(visitEvent);
  }

  public static Boolean isFrontEndEvent(VisitEvent visitEvent) {
    return frontEndVisitEvents.contains(visitEvent);
  }

  public static TransactionCompletionStatus getStatus(VisitEvent visitEvent) {
    if (transactionStatus.containsKey(visitEvent)) {
      return transactionStatus.get(visitEvent);
    }
    return null;
  }

  @JsonGetter
  public ZonedDateTime getDateTime() {
    return dateTime;
  }

  // TRANSFER_TO_SERVICE_POINT_POOL, List.of(VisitState.WAITING_IN_SP_POOL),
  // BACK_TO_QUEUE, List.of(VisitState.WAITING_IN_QUEUE)
  // TRANSFER_TO_QUEUE, List.of(VisitState.WAITING_IN_QUEUE),
  //  ADD_SERVICE, List.of()
  // );

  public boolean canBeNext(VisitEvent next) {
    return nextEvents.get(this).stream().anyMatch(e -> e.equals(next));
  }

  public VisitState getState() {
    visitState = visitStateMap.get(this);
    return visitState;
  }
}
