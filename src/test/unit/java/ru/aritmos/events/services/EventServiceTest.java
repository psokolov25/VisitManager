package ru.aritmos.events.services;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import reactor.core.publisher.Mono;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.aritmos.events.clients.DataBusClient;
import ru.aritmos.events.model.ChangedObject;
import ru.aritmos.events.model.Event;

@MicronautTest
class EventServiceTest {

    @Inject EventService eventService;
    @Inject DataBusClient dataBusClient;

    @Test
    void getDateStringFormatsAsRfc1123() {
        ZonedDateTime date = ZonedDateTime.of(2008, 4, 9, 23, 55, 38, 0, ZoneId.of("GMT"));
        String formatted = eventService.getDateString(date);
        org.junit.jupiter.api.Assertions.assertEquals("Wed, 09 Apr 2008 23:55:38 GMT", formatted);
    }

    @Test
    void sendChangedEventBuildsEntityChangedEvent() {
        clearInvocations(dataBusClient);
        Map<String, String> params = Map.of("id", "42");
        Map<String, String> oldValue = Map.of("field", "old");
        Map<String, String> newValue = Map.of("field", "new");

        eventService.sendChangedEvent("dest", false, oldValue, newValue, params, "UPDATE");

        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);

        verify(dataBusClient)
                .send(eq("dest"), eq(false), anyString(), eq(eventService.applicationName), typeCaptor.capture(), bodyCaptor.capture());

        org.junit.jupiter.api.Assertions.assertEquals("ENTITY_CHANGED", typeCaptor.getValue());
        ChangedObject changed = (ChangedObject) bodyCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("UPDATE", changed.getAction());
        org.junit.jupiter.api.Assertions.assertEquals(newValue, changed.getNewValue());
        org.junit.jupiter.api.Assertions.assertEquals(oldValue, changed.getOldValue());
    }

    @Test
    void sendSetsSenderAndCallsClient() {
        clearInvocations(dataBusClient);
        when(dataBusClient.send(anyString(), anyBoolean(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Mono.just(Map.of()));

        Event event = Event.builder().eventType("PING").body(Map.of()).build();
        eventService.applicationName = "vm";

        eventService.send("dest", false, event);

        verify(dataBusClient)
                .send(eq("dest"), eq(false), anyString(), eq("vm"), eq("PING"), any());
        org.junit.jupiter.api.Assertions.assertEquals("vm", event.getSenderService());
    }

    @Test
    void sendToMultipleDestinationsInvokesClientForEachService() {
        clearInvocations(dataBusClient);
        when(dataBusClient.send(anyString(), anyBoolean(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Mono.just(Map.of()));

        Event event = Event.builder().eventType("PING").body(Map.of()).build();
        eventService.applicationName = "vm";

        eventService.send(List.of("dest-1", "dest-2"), true, event);

        verify(dataBusClient)
                .send(eq("dest-1"), eq(true), anyString(), eq("vm"), eq("PING"), any());
        verify(dataBusClient)
                .send(eq("dest-2"), eq(true), anyString(), eq("vm"), eq("PING"), any());
    }

    @Test
    void sendChangedEventToMultipleDestinationsBuildsEventForEachService() {
        clearInvocations(dataBusClient);
        when(dataBusClient.send(anyString(), anyBoolean(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Mono.just(Map.of()));

        List<String> destinations = List.of("alpha", "beta");
        Map<String, String> params = Map.of("id", "42");
        String oldValue = "old";
        String newValue = "new";
        eventService.applicationName = "vm";

        eventService.sendChangedEvent(destinations, false, oldValue, newValue, params, "UPDATE");

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);

        verify(dataBusClient, times(destinations.size()))
                .send(
                        destinationCaptor.capture(),
                        eq(false),
                        anyString(),
                        eq("vm"),
                        typeCaptor.capture(),
                        bodyCaptor.capture());

        org.junit.jupiter.api.Assertions.assertEquals(destinations, destinationCaptor.getAllValues());
        typeCaptor.getAllValues().forEach(type -> org.junit.jupiter.api.Assertions.assertEquals("ENTITY_CHANGED", type));
        bodyCaptor
                .getAllValues()
                .forEach(
                        body -> {
                            ChangedObject changed = (ChangedObject) body;
                            org.junit.jupiter.api.Assertions.assertEquals("UPDATE", changed.getAction());
                            org.junit.jupiter.api.Assertions.assertEquals(oldValue, changed.getOldValue());
                            org.junit.jupiter.api.Assertions.assertEquals(newValue, changed.getNewValue());
                            org.junit.jupiter.api.Assertions.assertEquals(String.class.getName(), changed.getClassName());
                        });
    }
}
