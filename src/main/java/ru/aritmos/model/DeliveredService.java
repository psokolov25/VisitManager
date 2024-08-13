package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Оказанная услуга
 */
@EqualsAndHashCode(callSuper = false)
@Data
@Serdeable
public class DeliveredService extends BasedService{
    public DeliveredService(String id,String name)
    {
        super(id, name);
    }


}
