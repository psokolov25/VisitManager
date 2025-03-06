package ru.aritmos.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Context;
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

@Slf4j
@Context
public class EventHandlerContext {
  @Inject Configuration configuration;
  @Inject VisitService visitService;
  @Inject EventService eventService;
  @Inject BranchService branchService;

  @Singleton
  @PostConstruct
  void AddHandlers() {

    BusinesErrorHandler businesErrorHandler = new BusinesErrorHandler();
    SystemErrorHandler systemErrorHandler = new SystemErrorHandler();
    EntityChangedHandler entityChangedHandler = new EntityChangedHandler();
    BranchPublicHandler branchPublicHandler = new BranchPublicHandler(visitService, eventService,branchService);
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

    configuration.getConfiguration();
  }

  static class BusinesErrorHandler implements EventHandler {

    @Override
    @ExecuteOn(TaskExecutors.IO)
    public void Handle(Event event) {
      log.info("Event {} of Business error handled!", event);
    }
  }

  static class SystemErrorHandler implements EventHandler {

    @Override
    @ExecuteOn(TaskExecutors.IO)
    public void Handle(Event event) {
      log.info("Event {} of System error handled!", event);
    }
  }

  static class EntityChangedHandler implements EventHandler {

    @Override
    @ExecuteOn(TaskExecutors.IO)
    public void Handle(Event event) {
      log.info("Event {} of  entity changed!", event);
    }
  }

  static class NotForceUserLogoutHandler implements EventHandler {
    VisitService visitService;
    EventService eventService;

    NotForceUserLogoutHandler(VisitService visitService, EventService eventService) {
      this.visitService = visitService;
      this.eventService = eventService;
    }

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
                            .eventType("WARNING_PROCESSING_USER_LOGOUT")
                            .params(new HashMap<>())
                            .body(userSession)
                            .build());
                  } else {
                    visitService
                        .getBranchService()
                        .closeServicePoint(
                            servicePoint.get().getBranchId(),
                            servicePoint.get().getId(),
                            visitService,
                            false,
                            false,
                            "");
                  }
                }
              });
    }
  }

  static class ForceUserLogoutHandler implements EventHandler {
    VisitService visitService;
    EventService eventService;

    public ForceUserLogoutHandler(VisitService visitService, EventService eventService) {
      this.visitService = visitService;
      this.eventService = eventService;
    }

    @Override
    @ExecuteOn(TaskExecutors.IO)
    public void Handle(Event event) throws JsonProcessingException {
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
                                ""));
              });
    }
  }

  static class BranchPublicHandler implements EventHandler {
    VisitService visitService;
    EventService eventService;
    BranchService branchService;

    BranchPublicHandler(
        VisitService visitService, EventService eventService, BranchService branchService) {
      this.visitService = visitService;
      this.eventService = eventService;
      this.branchService = branchService;
    }
    private Map<String, Object> convertUsingReflection(Object object) throws IllegalAccessException {
      Map<String, Object> map = new HashMap<>();
      Field[] fields = object.getClass().getDeclaredFields();

      for (Field field: fields) {
        field.setAccessible(true);
        map.put(field.getName(), field.get(object));
      }

      return map;
    }
    @Override
    public void Handle(Event event)  {
      try {
      Map<String, Object> branchHashMap = convertUsingReflection(event.getBody());

        eventService.send(
            "branchconfigurer",
            false,
            Event.builder()
                .eventType("PUBLIC_STARTED")
                .body(branchHashMap)
                .eventDate(ZonedDateTime.now())
                .build());
        branchService
            .getBranches()
            .forEach(
                (key, value) -> {
                  if (!branchHashMap.containsKey(key)) {
                    branchService.delete(key);
                  }
                });
        branchHashMap.forEach(
            (key, value) -> {
              branchService.add(key, (Branch) value);
              eventService.send(
                  "branchconfigurer",
                  false,
                  Event.builder()
                      .eventType("BRANCH_PUBLIC_COMPLETE")
                      .body(value)
                      .eventDate(ZonedDateTime.now())
                      .build());
            });
        eventService.send(
            "branchconfigurer",
            false,
            Event.builder()
                .eventType("PUBLIC_COMPLETE")
                .body(branchHashMap)
                .eventDate(ZonedDateTime.now())
                .build());
      } catch (Exception e) {
        eventService.send(
            "branchconfigurer",
            false,
            Event.builder()
                .eventType("PUBLIC_ERROR")
                .body(e.getMessage())
                .eventDate(ZonedDateTime.now())
                .build());

      }
    }
  }
}
