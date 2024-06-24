package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable
@Introspected
@AllArgsConstructor
public class User extends BranchEntity{
    public User(String id,String name){
        super(id,name);
    }
    public User(String name){
        super(name);
    }
    String firstName;
    String lastName;
    String email;
    String currentWorkProfileId;
    List<String> workProfileIds=new ArrayList<>();
}
