package ru.aritmos.service;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.aritmos.clients.PrinterClient;
import ru.aritmos.model.Visit;

@Singleton
@Slf4j
public class PrinterService {
    @Inject
    PrinterClient printerClient;

    public void print(String id, Visit visit){
       log.info("Sending to printer client {}",id);
       Mono.from(printerClient.print("UTF-8",true,visit))
                .subscribe(s->log.info("Printing {}!",s)
                ,e->log.error("Error",e));
    }

}
