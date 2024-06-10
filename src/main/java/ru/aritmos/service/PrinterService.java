package ru.aritmos.service;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.events.clients.PrinterClient;
import ru.aritmos.model.Visit;

@Singleton
@Slf4j
public class PrinterService {
    @Inject
    PrinterClient printerClient;

    public void print(String id, Visit visit){
        printerClient.print(id,visit);
    }

}
