package ru.aritmos.model.visit;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.micronaut.serde.annotation.Serdeable;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Перечень событий жизненного цикла визита. */
@Serdeable
@JsonFormat
@AllArgsConstructor
@NoArgsConstructor
public enum VisitEvent {
  /** Визит создан */
  CREATED,
  /** Визит размещен в очередь после создания */
  PLACED_IN_QUEUE,
  /** Визит вызове */
  CALLED,
  /** Визит повторно вызван */
  RECALLED,
  /** Визит начал обслуживаться (текущая услуга) */
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
  /** В визит добавлена марки */
  ADDED_MARK,
  /** В визит добавлена заметки */
  ADDED_NOTE,
  /** В визит удалена пометка */
  DELETED_MARK,
  /** В визит удален итог фактической услуги */
  DELETED_DELIVERED_SERVICE_RESULT,
  /** В визит удален итог услуги */
  DELETED_SERVICE_RESULT,
  /** У визита закончена транзакция */
  VISIT_END_TRANSACTION,
  /** Визит переведен из очереди */
  VISIT_TRANSFER_FROM_QUEUE,
  /** Визит удален */
  DELETED;

  /** Список событий, которые оповещают фронт енд */
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
          ADDED_DELIVERED_SERVICE_RESULT,
          ADDED_SERVICE_RESULT,
          ADDED_MARK,
          DELETED_DELIVERED_SERVICE,
          DELETED_MARK,
          DELETED_DELIVERED_SERVICE_RESULT,
          DELETED_SERVICE_RESULT,
          DELETED);

  /** Список событий, после которых начинается новая транзакция */
  private static final List<VisitEvent> newTransactionEvents =
      List.of(
          STOP_SERVING,
          NO_SHOW,
          PLACED_IN_QUEUE,
          TRANSFER_TO_SERVICE_POINT_POOL,
          DELETED,
          TRANSFER_TO_USER_POOL);

  /** Список событий завершающих транзакцию, с указанием статуса завершенной транзакции */
  private static final Map<VisitEvent, TransactionCompletionStatus> transactionStatus =
      Map.ofEntries(
          Map.entry(STOP_SERVING, TransactionCompletionStatus.STOP_SERVING),
          Map.entry(NO_SHOW, TransactionCompletionStatus.NO_SHOW),
          Map.entry(DELETED, TransactionCompletionStatus.REMOVED_BY_EMP),
          Map.entry(PLACED_IN_QUEUE, TransactionCompletionStatus.TRANSFER_TO_QUEUE),
          Map.entry(TRANSFER_TO_QUEUE, TransactionCompletionStatus.TRANSFER_TO_QUEUE),
          Map.entry(
              TRANSFER_TO_SERVICE_POINT_POOL,
              TransactionCompletionStatus.TRANSFER_TO_SERVICE_POINT_POOL),
          Map.entry(TRANSFER_TO_USER_POOL, TransactionCompletionStatus.TRANSFER_TO_USER_POOL));

  /* Список событий, которые не должны отправляться в топики статистики */
  private static final List<VisitEvent> notSendToStat = List.of();

  /**
   * Список разрешенных следующих состояний, где ключ - состояние, значение - список разрешенных
   * состояний быть следующими после состояния в ключе.
   */
  private static final Map<VisitState, List<VisitState>> nextStates =
      Map.ofEntries(
          Map.entry(VisitState.CREATED, List.of(VisitState.WAITING_IN_QUEUE, VisitState.CALLED)),
          Map.entry(
              VisitState.WAITING_IN_QUEUE,
              List.of(
                  VisitState.CALLED,
                  VisitState.WAITING_IN_USER_POOL,
                  VisitState.WAITING_IN_SERVICE_POOL,
                  VisitState.END)),
          Map.entry(
              VisitState.CALLED,
              List.of(
                  VisitState.WAITING_IN_QUEUE,
                  VisitState.SERVING,
                  VisitState.END,
                  VisitState.WAITING_IN_SERVICE_POOL,
                  VisitState.WAITING_IN_USER_POOL)),
          Map.entry(
              VisitState.SERVING,
              List.of(
                  VisitState.CALLED,
                  VisitState.WAITING_IN_QUEUE,
                  VisitState.WAITING_IN_USER_POOL,
                  VisitState.WAITING_IN_SERVICE_POOL,
                  VisitState.END)),
          Map.entry(
              VisitState.WAITING_IN_USER_POOL,
              List.of(VisitState.CALLED, VisitState.WAITING_IN_SERVICE_POOL, VisitState.END)),
          Map.entry(
              VisitState.WAITING_IN_SERVICE_POOL,
              List.of(VisitState.CALLED, VisitState.WAITING_IN_USER_POOL, VisitState.END)),
          Map.entry(VisitState.END, List.of()));

  /** Список событий, с указанием, какое состояние визита наступает после него */
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
          Map.entry(ADDED_NOTE, VisitState.SERVING),
          Map.entry(DELETED_MARK, VisitState.SERVING),
          Map.entry(STOP_SERVING, VisitState.SERVING),
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
          Map.entry(DELETED_DELIVERED_SERVICE, VisitState.SERVING),
          Map.entry(DELETED_DELIVERED_SERVICE_RESULT, VisitState.SERVING),
          Map.entry(DELETED_SERVICE_RESULT, VisitState.SERVING));

  /** Дополнительные параметры события. */
  @Getter final Map<String, String> parameters = new HashMap<>();
  /** Время наступления события. */
  @JsonFormat public ZonedDateTime dateTime;
  /** Состояние визита, соответствующее данному событию. */
  VisitState visitState;

  /**
   * Проверка: является ли событие началом новой транзакции.
   *
   * @param visitEvent событие визита
   * @return истина, если после события начинается новая транзакция
   */
  @SuppressWarnings("unused")
  public static Boolean isNewOfTransaction(VisitEvent visitEvent) {
    return VisitEvent.newTransactionEvents.stream().anyMatch(am -> am.equals(visitEvent));
  }

  /**
   * Проверка, исключается ли событие из статистики.
   *
   * @param visitEvent событие визита
   * @return истина, если событие не отправляется в статистику
   */
  public static Boolean isIgnoredInStat(VisitEvent visitEvent) {
    return notSendToStat.contains(visitEvent);
  }

  /**
   * Проверка необходимости отправки события на фронтенд.
   *
   * @param visitEvent событие визита
   * @return истина, если событие отправляется на фронтенд
   */
  public static Boolean isFrontEndEvent(VisitEvent visitEvent) {
    return frontEndVisitEvents.contains(visitEvent);
  }

  /**
   * Получить статус завершения транзакции для события, если применимо.
   *
   * @param visitEvent событие визита
   * @return статус завершения или null
   */
  @SuppressWarnings("unused")
  public static TransactionCompletionStatus getStatus(VisitEvent visitEvent) {
    if (transactionStatus.containsKey(visitEvent)) {
      return transactionStatus.get(visitEvent);
    }
    return null;
  }

  /**
   * Проверка на возможность следующего события после текущего.
   *
   * @param next следующее событие
   * @return истина, если событие допустимо
   */
  public boolean canBeNext(VisitEvent next) {
    if (nextStates.containsKey(this.visitState)) {
      return nextStates.get(this.getState()).stream().anyMatch(e -> e.equals(next.getState()))
          || this.getState().equals(next.getState());
    }
    return true;
  }

  /**
   * Получение состояния визита, соответствующего текущему событию.
   *
   * @return состояние визита
   */
  public VisitState getState() {
    visitState = visitStateMap.get(this);
    return visitState;
  }
}
