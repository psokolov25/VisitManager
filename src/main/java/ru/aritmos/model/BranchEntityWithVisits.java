package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.*;
import ru.aritmos.model.visit.Visit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сущность отделения, содержащая перечень визитов
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable
@Introspected
@AllArgsConstructor
@NoArgsConstructor
public class BranchEntityWithVisits extends BranchEntity {
    public BranchEntityWithVisits(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public BranchEntityWithVisits(String id, String name) {
        this.id = id;
        this.name = name;
    }
    /**
     * Визиты
     */
    List<Visit> visits = new ArrayList<>();
}
