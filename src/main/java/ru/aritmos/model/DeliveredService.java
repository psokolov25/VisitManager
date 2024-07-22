package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Serdeable
public class DeliveredService extends BasedService{
    public DeliveredService(String id,String name)
    {
        super(id, name);
    }
    List<String> servviceIds=new ArrayList<>();
}
