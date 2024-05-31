package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.micronaut.serde.annotation.Serdeable;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Builder
@Data
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
