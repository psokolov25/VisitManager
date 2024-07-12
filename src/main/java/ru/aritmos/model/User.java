package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Пользователь
 */
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

    /**
     * Имя
     */
    String firstName;
    /**
     * Фамилия
     */
    String lastName;
    /**
     * Электронная почта
     */
    String email;
    /**
     * Идентификатор текущего рабочего профиля
     */
    String currentWorkProfileId;
    /**
     * Идентификатор точки обслуживания
     */
    String servicePoinrtId;
    /**
     * Идентификатор текущего отделения
     */
    String branchId;

    /**
     * Идентификаторы рабочих профилей сотрудника
     */
    List<String> workProfileIds=new ArrayList<>();
}
