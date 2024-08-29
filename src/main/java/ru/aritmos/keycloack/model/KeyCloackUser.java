package ru.aritmos.keycloack.model;

import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
@Data
@Serdeable
public class KeyCloackUser {
    private String email;
    private String name;

    private String preferred_username;
    private List<String> roles;
    private HashMap<String,Object> attributes;
    private List<String> groups;
}
