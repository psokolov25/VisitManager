package ru.aritmos.events.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import ru.aritmos.exceptions.SystemException;

/** Обработчик событий шины данных. */
public interface EventHandler {
  /**
   * Обработка события.
   *
   * @param event событие
   * @throws JsonProcessingException ошибка сериализации/десериализации
   * @throws SystemException системная ошибка обработки
   * @throws IllegalAccessException ошибка доступа при рефлексии
   */
  @ExecuteOn(TaskExecutors.IO)
  void Handle(Event event) throws JsonProcessingException, SystemException, IllegalAccessException;
}
