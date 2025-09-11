package ru.aritmos.events.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;

/** Модель изменённой сущности для событий шины данных. */
@Data
@Serdeable
@Builder
public class ChangedObject {
  /** Предыдущее значение сущности. */
  Object oldValue;
  /** Новое значение сущности. */
  Object newValue;
  /** Полное имя класса изменённой сущности. */
  String className;
  /** Действие (создание, обновление, удаление). */
  String action;
}
