package ru.aritmos.model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Пользователь
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Serdeable
@Introspected

public class User extends BranchEntityWithVisits{
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
    String servicePointId;
    /**
     * Идентификатор текущего отделения
     */
    String branchId;


}
