package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

/**
 * Сущность отделения
 */
@Data
@Introspected
@Jacksonized
@Builder

@NoArgsConstructor
@Serdeable
public class BranchEntity {
    /**
     * Идентификатор
     */
    String id;
    /**
     * Название
     */
    String name;

    public BranchEntity(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public BranchEntity(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
