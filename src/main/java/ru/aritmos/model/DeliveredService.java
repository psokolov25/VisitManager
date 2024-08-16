package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Идентификаторы подходящих услуг
     */
    List<String> servviceIds=new ArrayList<>();
}
