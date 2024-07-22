package ru.aritmos.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Data
@Serdeable
@NoArgsConstructor

@AllArgsConstructor
public class BasedService extends BranchEntity{
    public BasedService(String id,String name)
    {
        super(id,name);

    }
    HashMap<String,Outcome> possibleOutcomes=new HashMap<>();
    Outcome outcome;
}
