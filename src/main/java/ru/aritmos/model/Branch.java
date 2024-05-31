package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.micronaut.serde.annotation.Serdeable;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.security.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@Serdeable


@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)


public  class Branch extends BranchEntity {
    public Branch(String name) {
        super(name);
    }

    public Branch(String key, String name) {
        super(key, name);
    }
    ArrayList<Service> services = new ArrayList<>();
    HashMap<String,Queue> queues = new HashMap<>();

}
