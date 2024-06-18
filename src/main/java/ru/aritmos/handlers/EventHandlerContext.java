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
import ru.aritmos.exceptions.SystemException;
import ru.aritmos.model.Visit;
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

    @Singleton
    class VisitCallHandler implements EventHandler {


        @Override
        @ExecuteOn(TaskExecutors.IO)
        public void Handle(Event event) throws SystemException {
            log.info("Event {} of call of visit handled!", event);
            try {
                Visit visit = (Visit) event.getBody();
                visitService.visitCall(event.getParams().get("branchId"),event.getParams().get("servicePointId"), visit);
            } catch (Exception ex) {
                throw new SystemException(ex.getMessage(), eventService);
            }

        }
    }
    class VisitDeleteHandler implements EventHandler {


        @Override
        @ExecuteOn(TaskExecutors.IO)
        public void Handle(Event event) throws SystemException {
            log.info("Event {} of delete of visit handled!", event);
            try {
                Visit visit = (Visit) event.getBody();
                visitService.deleteVisit(event.getParams().get("branchId"),event.getParams().get("servicePointId"), visit);
            } catch (Exception ex) {
                throw new SystemException(ex.getMessage(), eventService);
            }

        }
    }
    class EntityChangedHandler implements EventHandler {


        @Override
        @ExecuteOn(TaskExecutors.IO)
        public void Handle(Event event) {
            log.info("Event {} of  entity changed!", event);


        }
    }

    @Singleton
    class VisitTransferHandler implements EventHandler {


        @Override
        @ExecuteOn(TaskExecutors.IO)
        public void Handle(Event event) throws SystemException {
            log.info("Event {} of transfer of visit handled!", event);
            try {
                Visit visit = (Visit) event.getBody();
                visitService.visitTransfer(event.getParams().get("branchId"),event.getParams().get("servicePointId"), event.getParams().get("queueId"), visit);
            } catch (Exception ex) {
                throw new SystemException(ex.getMessage(), eventService);
            }

        }
    }

    @PostConstruct
    void AddHandlers() {

        BusinesErrorHandler businesErrorHandler = new BusinesErrorHandler();
        SystemErrorHandler systemErrorHandler = new SystemErrorHandler();
        VisitCallHandler visitCallHandler = new VisitCallHandler();
        VisitTransferHandler visitTransferHandler = new VisitTransferHandler();
        VisitDeleteHandler visitDeleteHandler = new VisitDeleteHandler();
        EntityChangedHandler entityChangedHandler = new EntityChangedHandler();
        KaffkaListener.addAllEventHandler("BUSINESS_ERROR", businesErrorHandler);
        KaffkaListener.addAllEventHandler("SYSTEM_ERROR", systemErrorHandler);
        KaffkaListener.addServiceEventHandler("VISIT_CALLED", visitCallHandler);
        KaffkaListener.addServiceEventHandler("VISIT_DELETED", visitDeleteHandler);
        KaffkaListener.addServiceEventHandler("VISIT_TRANSFER", visitTransferHandler);
        KaffkaListener.addAllEventHandler("ENTITY_CHANGED", entityChangedHandler);
        configuration.getConfiguration();
    }
}
