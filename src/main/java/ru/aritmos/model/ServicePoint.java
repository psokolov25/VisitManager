package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Точка обслуживания
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable
@Introspected

public class ServicePoint extends BranchEntity {
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
}
