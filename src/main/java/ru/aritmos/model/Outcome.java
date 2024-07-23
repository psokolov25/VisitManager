package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Итог обслуживания
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Serdeable
public class Outcome extends BranchEntity{
    public Outcome(String id,String name)
    {
        super(id,name);
    }
    /**
     * Код итога обслуживания
     */
    Long code;
}
