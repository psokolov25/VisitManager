package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable

@Introspected
public class WorkProfile extends BranchEntity{
    public WorkProfile(String name){
        super(name);
    }
    List<String> queueIds=new ArrayList<>();
}
