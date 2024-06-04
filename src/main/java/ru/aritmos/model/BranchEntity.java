package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Builder
@Data
@Introspected
@Jacksonized
@NoArgsConstructor
public class BranchEntity {
     String id;
     String name;

    public BranchEntity(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }
    public BranchEntity(String key,String name) {
        this.id = key;
        this.name = name;
    }
}
