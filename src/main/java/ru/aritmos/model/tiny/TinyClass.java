package ru.aritmos.model.tiny;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Класс для формирования списков объектов
 */
@Serdeable
@Data
@AllArgsConstructor
public class TinyClass {
    /**
     * Идентификатора объекта
     */
    private String id;
    /**
     * Название объекта
     */
    private String name;
}
