package ru.aritmos.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.model.EventHandler;
import ru.aritmos.events.services.EventService;
import ru.aritmos.events.services.KafkaListener;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.keycloak.UserSession;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Configuration;
import ru.aritmos.service.VisitService;

/** Контекст регистрации обработчиков событий и их привязки к KafkaListener. */
@Slf4j
@Requires(notEnv = "test")
@Context
public class EventHandlerContext {
  /** Сервис конфигурации отделений. */
  @Inject Configuration configuration;

  /** Сервис работы с визитами. */
  @Inject VisitService visitService;

  /** Сервис отправки событий. */
  @Inject EventService eventService;

  /** Сервис управления отделениями. */
  @Inject BranchService branchService;

  /** Регистрация обработчиков событий и публикация начальной конфигурации. */
  @Singleton
  @PostConstruct
  void AddHandlers() {

    BusinesErrorHandler businesErrorHandler = new BusinesErrorHandler();
    SystemErrorHandler systemErrorHandler = new SystemErrorHandler();
    EntityChangedHandler entityChangedHandler = new EntityChangedHandler();
    BranchPublicHandler branchPublicHandler =
        new BranchPublicHandler(visitService, eventService, branchService, configuration);
    ForceUserLogoutHandler forceUserLogoutHandler =
        new ForceUserLogoutHandler(visitService, eventService);
    NotForceUserLogoutHandler notForceUserLogoutHandler =
        new NotForceUserLogoutHandler(visitService, eventService);
    KafkaListener.addAllEventHandler("BUSINESS_ERROR", businesErrorHandler);
    KafkaListener.addAllEventHandler("SYSTEM_ERROR", systemErrorHandler);
    KafkaListener.addAllEventHandler("ENTITY_CHANGED", entityChangedHandler);
    KafkaListener.addServiceEventHandler("ENTITY_CHANGED", entityChangedHandler);
    KafkaListener.addServiceEventHandler("BRANCH_PUBLIC", branchPublicHandler);
    KafkaListener.addServiceEventHandler("PROCESSING_USER_LOGOUT_FORCE", forceUserLogoutHandler);
    KafkaListener.addServiceEventHandler(
        "PROCESSING_USER_LOGOUT_NOT_FORCE", notForceUserLogoutHandler);

    configuration.createBranchConfiguration(configuration.createDemoBranch());
  }

  /** Обработчик бизнес-ошибок. */
  static class BusinesErrorHandler implements EventHandler {

    @Override
    @ExecuteOn(TaskExecutors.IO)
    public void Handle(Event event) {
      log.info("Event {} of Business error handled!", event);
    }
  }

  /** Обработчик системных ошибок. */
  static class SystemErrorHandler implements EventHandler {

    @Override
    @ExecuteOn(TaskExecutors.IO)
    public void Handle(Event event) {
      log.info("Event {} of System error handled!", event);
    }
  }

  /** Обработчик событий изменения сущностей. */
  static class EntityChangedHandler implements EventHandler {

    @Override
    @ExecuteOn(TaskExecutors.IO)
    public void Handle(Event event) {
      log.info("Event {} of  entity changed!", event);
    }
  }

  /** Обработчик мягкого завершения пользовательской сессии. */
  static class NotForceUserLogoutHandler implements EventHandler {
    /** Сервис визитов. */
    VisitService visitService;

    /** Сервис событий. */
    EventService eventService;

    /**
     * Конструктор.
     *
     * @param visitService сервис визитов
     * @param eventService сервис событий
     */
    NotForceUserLogoutHandler(VisitService visitService, EventService eventService) {
      this.visitService = visitService;
      this.eventService = eventService;
    }

    /**
     * Обработка события о мягком завершении сессии пользователя.
     *
     * @param event событие
     * @throws JsonProcessingException ошибка сериализации/десериализации
     */
    @Override
    @ExecuteOn(TaskExecutors.IO)
    public void Handle(Event event) throws JsonProcessingException {
      ObjectMapper objectMapper = new ObjectMapper();
      String eventBody = objectMapper.writeValueAsString(event.getBody());
      UserSession userSession = objectMapper.readValue(eventBody, UserSession.class);
      log.info("Not Force user logged out: {}", userSession);
      visitService
          .getBranchService()
          .getDetailedBranches()
          .values()
          .forEach(
              f -> {
                Optional<ServicePoint> servicePoint =
                    f.getServicePoints().values().stream()
                        .filter(
                            f2 ->
                                f2.getUser() != null
                                    && f2.getUser().getName().equals(userSession.getLogin()))
                        .findFirst();
                if (servicePoint.isPresent()) {
                  if (servicePoint.get().getVisit() != null) {

                    eventService.send(
                        "frontend",
                        false,
                        Event.builder()
                            .eventDate(ZonedDateTime.now())
                            .eventType("PROCESSING_USER_LOGOUT_FORCE")
                            .params(new HashMap<>())
                            .body(userSession)
                            .build());
                    visitService.visitEnd(
                        servicePoint.get().getBranchId(),
                        servicePoint.get().getId(),
                        true,
                        "USER_SESSION_KILLED");
                  }
                  visitService
                      .getBranchService()
                      .closeServicePoint(
                          servicePoint.get().getBranchId(),
                          servicePoint.get().getId(),
                          visitService,
                          false,
                          false,
                          "",
                          false,
                          "");
                }
              });
    }
  }

  /** Обработчик принудительного завершения пользовательской сессии. */
  static class ForceUserLogoutHandler implements EventHandler {
    /** Сервис визитов. */
    VisitService visitService;

    /** Сервис событий. */
    EventService eventService;

    /**
     * Конструктор.
     *
     * @param visitService сервис визитов
     * @param eventService сервис событий
     */
    public ForceUserLogoutHandler(VisitService visitService, EventService eventService) {
      this.visitService = visitService;
      this.eventService = eventService;
    }

    /**
     * Обработка события о принудительном завершении сессии пользователя.
     *
     * @param event событие
     * @throws JsonProcessingException ошибка сериализации/десериализации
     */
    @Override
    @ExecuteOn(TaskExecutors.IO)
    public void Handle(Event event) throws JsonProcessingException {
      if (!event.getSenderService().equals("visitmanager")) {
        ObjectMapper objectMapper = new ObjectMapper();
        String eventBody = objectMapper.writeValueAsString(event.getBody());
        UserSession userSession = objectMapper.readValue(eventBody, UserSession.class);
        log.info("Force user logged out: {}", userSession);
        visitService
            .getBranchService()
            .getDetailedBranches()
            .values()
            .forEach(
                f -> {
                  Optional<ServicePoint> servicePoint =
                      f.getServicePoints().values().stream()
                          .filter(
                              f2 ->
                                  f2.getUser() != null
                                      && f2.getUser().getName().equals(userSession.getLogin()))
                          .findFirst();
                  servicePoint.ifPresent(
                      point ->
                          visitService
                              .getBranchService()
                              .closeServicePoint(
                                  point.getBranchId(),
                                  point.getId(),
                                  visitService,
                                  false,
                                  false,
                                  "",
                                  true,
                                  "USER_SESSION_KILLED"));
                });
      }
    }
  }

  /** Обработчик публикации конфигурации отделения. */
  static class BranchPublicHandler implements EventHandler {
    /** Сервис визитов. */
    VisitService visitService;

    /** Сервис событий. */
    EventService eventService;

    /** Сервис отделений. */
    BranchService branchService;

    /** Сервис конфигурации. */
    Configuration configuration;

    /**
     * Конструктор.
     *
     * @param visitService сервис визитов
     * @param eventService сервис событий
     * @param branchService сервис отделений
     * @param configuration сервис конфигурации
     */
    BranchPublicHandler(
        VisitService visitService,
        EventService eventService,
        BranchService branchService,
        Configuration configuration) {
      this.visitService = visitService;
      this.eventService = eventService;
      this.branchService = branchService;
      this.configuration = configuration;
    }

    /**
     * Конвертация объекта в карту при помощи рефлексии.
     *
     * @param object объект для конвертации
     * @return карта "поле -> значение"
     * @throws IllegalAccessException ошибка доступа к приватным полям
     */
    private Map<String, Object> convertUsingReflection(Object object)
        throws IllegalAccessException {
      Map<String, Object> map = new HashMap<>();
      Field[] fields = object.getClass().getDeclaredFields();

      for (Field field : fields) {
        field.setAccessible(true);
        map.put(field.getName(), field.get(object));
      }

      return map;
    }

    /**
     * Обработка события публикации конфигурации отделений.
     *
     * @param event событие
     * @throws IllegalAccessException ошибка доступа при конвертации
     */
    @Override
    public void Handle(Event event) throws IllegalAccessException {

      Map<String, Object> branchHashMap = convertUsingReflection(event.getBody());
      Map<String, Branch> branchMap = new HashMap<>();
      branchHashMap.forEach((key, value) -> branchMap.put(key, (Branch) value));
      configuration.createBranchConfiguration(branchMap);
    }
  }
}
