package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.aritmos.model.visit.Visit;

/**
 * Точка обслуживания
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable
@Introspected

public class ServicePoint extends BranchEntityWithVisits {
    public  ServicePoint(String id,String name)
    {
        super(id,name);
    }
    public  ServicePoint(String name)
    {
        super(name);
    }

    /**
     * Визит
     */
    Visit visit;
    /**
     * Обслуживающий пользователь
     */
    User user;
    /**
     * Режим автоматического вызова при создании визита
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    Boolean autoCallMode;
    /**
     * Режим подтверждения вызова
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Boolean isConfirmRequired=false;

}
