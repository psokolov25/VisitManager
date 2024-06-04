package ru.aritmos;

import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import ru.aritmos.events.model.Event;
import ru.aritmos.events.model.EventHandler;
import ru.aritmos.events.services.KaffkaListener;

@Slf4j
@Context
public class EventHandlerContext {
    static class BusinesErrorHandler implements EventHandler {

        @Override
        public void Handle(Event event) {
            log.info("Event {} of Business error handled!",event);
        }
    }
    static class VisitCallHandler implements EventHandler {

        @Override
        public void Handle(Event event) {
            log.info("Event {} of call of visit handled!",event);
        }
    }

    @PostConstruct
    void AddHandlers() {
        BusinesErrorHandler businesErrorHandler = new BusinesErrorHandler();
        KaffkaListener.addAllEventHandler("BUSINESS_ERROR", businesErrorHandler);
        KaffkaListener.addServiceEventHandler("VISIT_CALLED", businesErrorHandler);
    }
}
