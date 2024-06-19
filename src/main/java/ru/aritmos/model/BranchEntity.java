package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;


@Data
@Introspected
@Jacksonized
@Builder
@NoArgsConstructor

public class BranchEntity {
     String id;
     String name;

    public BranchEntity(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }
    public BranchEntity(String id,String name) {
        this.id = id;
        this.name = name;
    }
}
