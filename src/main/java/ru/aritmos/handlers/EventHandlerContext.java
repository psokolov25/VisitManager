package ru.aritmos.handlers;

import io.micronaut.context.annotation.Context;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import ru.aritmos.events.model.Event;
import ru.aritmos.events.model.EventHandler;
import ru.aritmos.events.services.EventService;
import ru.aritmos.events.services.KaffkaListener;
import ru.aritmos.service.Configuration;
import ru.aritmos.service.VisitService;

@Slf4j
@Context
public class EventHandlerContext {
@Inject
    Configuration configuration;
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

    @Inject
    VisitService visitService;
    @Inject
    EventService eventService;


    static class EntityChangedHandler implements EventHandler {


        @Override
        @ExecuteOn(TaskExecutors.IO)
        public void Handle(Event event) {
            log.info("Event {} of  entity changed!", event);


        }
    }

    @Singleton


    @PostConstruct
    void AddHandlers() {

        BusinesErrorHandler businesErrorHandler = new BusinesErrorHandler();
        SystemErrorHandler systemErrorHandler = new SystemErrorHandler();
        EntityChangedHandler entityChangedHandler = new EntityChangedHandler();
        KaffkaListener.addAllEventHandler("BUSINESS_ERROR", businesErrorHandler);
        KaffkaListener.addAllEventHandler("SYSTEM_ERROR", systemErrorHandler);
        KaffkaListener.addAllEventHandler("ENTITY_CHANGED", entityChangedHandler);
        configuration.getConfiguration();

    }
}
