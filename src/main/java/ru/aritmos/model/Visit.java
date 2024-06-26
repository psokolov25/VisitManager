package ru.aritmos.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;

@Data
@EqualsAndHashCode
@Serdeable
@Builder(toBuilder = true)
@Introspected
@ToString
public class Visit {
    /**
     * Идентификатор визита
     */
    String id;
    /**
     * Статус визита
     */
    String status;
    /**
     * Талон
     */
    String ticketId;
    /**
     * Идентификатор отделения
     */
    String branchId;
    /**
     * Дата создания визита
     */
    ZonedDateTime createDate;
    /**
     * Дата обновления
     */
    ZonedDateTime updateDate;
    /**
     * Дата перевода
     */
    ZonedDateTime transferDate;
    /**
     * Версия визита
     */
    Integer version;
    /**
     * Идентификатор точки обслуживания
     */
    String servicePointId;
    /**
     * Логин вызвавшего сотрудника
     */
    String userName;
    /**
     * Массив не обслуженных услуг
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<Service> unservedServices;
    /**
     * Обслуженные услуги
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<Service> servedServices;


    @JsonGetter

    public Long getWaitingTime() {
        final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());

        waitingTime = unit.between(transferDate, ZonedDateTime.now());
        return waitingTime;
    }
    /**
     * Время ожидания в последней очереди в секундах
     */
    Long waitingTime;

    @JsonGetter

    public Long getTotalWaitingTime() {
        final ChronoUnit unit = ChronoUnit.valueOf(ChronoUnit.SECONDS.name());

        waitingTime = unit.between(createDate, ZonedDateTime.now());
        return waitingTime;
    }
    /**
     * Общее время с создания визита
     */
    Long totalWaitingTime;
    /**
     * Текущая услуга
     */
    Service currentService;
    /**
     * Дополнительные параметры визита
     */
    HashMap<String, Object> parameterMap;
    /**
     * Признак печати талона
     */
    Boolean printTicket;
    /**
     * Точка создания визита талона
     */
    EntryPoint entryPoint;
    /**
     * Идентификатор очереди
     */
    String queueId;


}
