package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.util.HashMap;
/**
 * Базовая услуга
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Serdeable
@NoArgsConstructor

@AllArgsConstructor

public class BasedService extends BranchEntity{
    public BasedService(String id,String name)
    {
        super(id,name);

    }
    HashMap<String,Outcome>possibleOutcomes=new HashMap<>();
    /**
     * Итог обслуживания
     */
    Outcome outcome;
}
