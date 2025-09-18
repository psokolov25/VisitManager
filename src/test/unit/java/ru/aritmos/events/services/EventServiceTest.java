package ru.aritmos.events.services;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.aritmos.events.model.ChangedObject;
import ru.aritmos.events.model.Event;
import ru.aritmos.config.LocalNoDockerDataBusClientStub;

@MicronautTest
class EventServiceTest {

    @Inject EventService eventService;
    @Inject LocalNoDockerDataBusClientStub dataBusClient;

    @Test
    void getDateStringFormatsAsRfc1123() {
        ZonedDateTime date = ZonedDateTime.of(2008, 4, 9, 23, 55, 38, 0, ZoneId.of("GMT"));
        String formatted = eventService.getDateString(date);
        ru.aritmos.test.LoggingAssertions.assertEquals("Wed, 09 Apr 2008 23:55:38 GMT", formatted);
    }

    @Test
    void sendChangedEventBuildsEntityChangedEvent() {
        dataBusClient.clearInvocations();
        Map<String, String> params = Map.of("id", "42");
        Map<String, String> oldValue = Map.of("field", "old");
        Map<String, String> newValue = Map.of("field", "new");

        eventService.sendChangedEvent("dest", false, oldValue, newValue, params, "UPDATE");

        List<LocalNoDockerDataBusClientStub.InvocationRecord> invocations = dataBusClient.getInvocations();
        ru.aritmos.test.LoggingAssertions.assertEquals(1, invocations.size());
        LocalNoDockerDataBusClientStub.InvocationRecord invocation = invocations.get(0);
        ru.aritmos.test.LoggingAssertions.assertEquals("dest", invocation.destinationServices());
        ru.aritmos.test.LoggingAssertions.assertFalse(invocation.sendToOtherBus());
        ru.aritmos.test.LoggingAssertions.assertEquals(eventService.applicationName, invocation.senderService());
        ru.aritmos.test.LoggingAssertions.assertEquals("ENTITY_CHANGED", invocation.type());
        ChangedObject changed = (ChangedObject) invocation.body();
        ru.aritmos.test.LoggingAssertions.assertEquals("UPDATE", changed.getAction());
        ru.aritmos.test.LoggingAssertions.assertEquals(newValue, changed.getNewValue());
        ru.aritmos.test.LoggingAssertions.assertEquals(oldValue, changed.getOldValue());
    }

    @Test
    void sendSetsSenderAndCallsClient() {
        dataBusClient.clearInvocations();

        Event event = Event.builder().eventType("PING").body(Map.of()).build();
        eventService.applicationName = "vm";

        eventService.send("dest", false, event);

        List<LocalNoDockerDataBusClientStub.InvocationRecord> invocations = dataBusClient.getInvocations();
        ru.aritmos.test.LoggingAssertions.assertEquals(1, invocations.size());
        LocalNoDockerDataBusClientStub.InvocationRecord invocation = invocations.get(0);
        ru.aritmos.test.LoggingAssertions.assertEquals("dest", invocation.destinationServices());
        ru.aritmos.test.LoggingAssertions.assertEquals("vm", invocation.senderService());
        ru.aritmos.test.LoggingAssertions.assertEquals("PING", invocation.type());
        ru.aritmos.test.LoggingAssertions.assertEquals("vm", event.getSenderService());
    }

    @Test
    void sendToMultipleDestinationsInvokesClientForEachService() {
        dataBusClient.clearInvocations();

        Event event = Event.builder().eventType("PING").body(Map.of()).build();
        eventService.applicationName = "vm";

        eventService.send(List.of("dest-1", "dest-2"), true, event);

        List<LocalNoDockerDataBusClientStub.InvocationRecord> invocations = dataBusClient.getInvocations();
        ru.aritmos.test.LoggingAssertions.assertEquals(2, invocations.size());
        ru.aritmos.test.LoggingAssertions.assertEquals("dest-1", invocations.get(0).destinationServices());
        ru.aritmos.test.LoggingAssertions.assertEquals("dest-2", invocations.get(1).destinationServices());
        invocations.forEach(
            i -> {
              ru.aritmos.test.LoggingAssertions.assertTrue(i.sendToOtherBus());
              ru.aritmos.test.LoggingAssertions.assertEquals("vm", i.senderService());
              ru.aritmos.test.LoggingAssertions.assertEquals("PING", i.type());
            });
    }

    @Test
    void sendChangedEventToMultipleDestinationsBuildsEventForEachService() {
        dataBusClient.clearInvocations();

        List<String> destinations = List.of("alpha", "beta");
        Map<String, String> params = Map.of("id", "42");
        String oldValue = "old";
        String newValue = "new";
        eventService.applicationName = "vm";

        eventService.sendChangedEvent(destinations, false, oldValue, newValue, params, "UPDATE");

        List<LocalNoDockerDataBusClientStub.InvocationRecord> invocations = dataBusClient.getInvocations();
        ru.aritmos.test.LoggingAssertions.assertEquals(destinations.size(), invocations.size());
        ru.aritmos.test.LoggingAssertions.assertEquals(destinations, invocations.stream().map(LocalNoDockerDataBusClientStub.InvocationRecord::destinationServices).toList());
        invocations.forEach(
            invocation -> {
              ru.aritmos.test.LoggingAssertions.assertFalse(invocation.sendToOtherBus());
              ru.aritmos.test.LoggingAssertions.assertEquals("vm", invocation.senderService());
              ru.aritmos.test.LoggingAssertions.assertEquals("ENTITY_CHANGED", invocation.type());
              ChangedObject changed = (ChangedObject) invocation.body();
              ru.aritmos.test.LoggingAssertions.assertEquals("UPDATE", changed.getAction());
              ru.aritmos.test.LoggingAssertions.assertEquals(oldValue, changed.getOldValue());
              ru.aritmos.test.LoggingAssertions.assertEquals(newValue, changed.getNewValue());
              ru.aritmos.test.LoggingAssertions.assertEquals(String.class.getName(), changed.getClassName());
            });
    }
}
