package ru.aritmos.events.services;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import ru.aritmos.events.clients.DataBusClient;
import ru.aritmos.events.model.Event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class EventService {
    @Inject
    DataBusClient dataBusClient;
    String getDateString(Date date) throws ParseException {

        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        String d = format.format(date);
        return d;
    }
    public void send(String destinationServices,Boolean sendToOtherBus,Event event) throws ParseException {

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
