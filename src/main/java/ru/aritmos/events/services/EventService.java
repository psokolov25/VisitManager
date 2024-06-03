package ru.aritmos.events.services;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import ru.aritmos.events.clients.DataBusClient;
import ru.aritmos.events.model.Event;

import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Singleton
public class EventService {
    @Inject
    DataBusClient dataBusClient;
    String getDateString(Date date) {

        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        return format.format(date);
    }
    public void send(String destinationServices,Boolean sendToOtherBus,Event event)  {

        Flux.from(dataBusClient.send(destinationServices
                ,sendToOtherBus
                ,getDateString(event.getEventDate())
                ,event.getSenderService()
                ,event.getEventType()
                ,event.getBody()))
                .subscribe(s->log.info("Event {} sended!",event)
                        ,e->log.error("Error! {}",e.getMessage()));
    }
}
