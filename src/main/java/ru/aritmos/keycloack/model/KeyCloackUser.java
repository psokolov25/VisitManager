package ru.aritmos.keycloack.model;

import io.micronaut.serde.annotation.Serdeable;
import java.util.HashMap;
import java.util.List;
import lombok.Data;

@Data
@Serdeable
public class KeyCloackUser {
  private String email;
  private String name;
  private HashMap<String, HashMap<String, List<String>>> resource_access;
  private String preferred_username;
  private List<String> roles;
  private HashMap<String, Object> attributes;
  private List<String> groups;
}
