package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@Data
@Serdeable

@Introspected
public class EntryPoint extends BranchEntity{
    public EntryPoint(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }
    public EntryPoint(String id,String name) {
        this.id = id;
        this.name = name;
    }
    String printerId;

}
