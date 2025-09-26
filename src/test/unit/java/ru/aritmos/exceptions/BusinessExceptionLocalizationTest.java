package ru.aritmos.exceptions;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import ru.aritmos.events.model.Event;
import ru.aritmos.events.services.EventService;

class BusinessExceptionLocalizationTest {

    @AfterEach
    void tearDown() {
        BusinessException.resetLocalization();
    }

    @DisplayName("Применяет локализацию из настроек")
    @Test
    void appliesLocalizationFromConfiguration() {
        BusinessExceptionLocalizationProperties properties = new BusinessExceptionLocalizationProperties();

        properties.getHttp().setLanguage("ru");
        properties.getHttp().setDefaultLanguage("en");
        properties.getHttp().setMessages(
                Map.of("visit_not_found", Map.of("ru", "Визит не найден", "en", "Visit not found")));

        properties.getLog().setLanguage("en");
        properties.getLog().setDefaultLanguage("ru");
        properties.getLog().setMessages(
                Map.of("visit_not_found", Map.of("en", "Visit not found", "ru", "Визит не найден")));

        properties.getEvent().setLanguage("kk");
        properties.getEvent().setDefaultLanguage("ru");
        properties.getEvent().setMessages(
                Map.of("visit_not_found", Map.of("kk", "Қатысу табылмады", "ru", "Визит не найден")));

        BusinessException.configureLocalization(new BusinessExceptionLocalization(properties));

        EventService eventService = mock(EventService.class);

        Logger logger = (Logger) LoggerFactory.getLogger(BusinessException.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        HttpStatusException thrown;
        try {
            thrown =
                    assertThrows(
                            HttpStatusException.class,
                            () -> new BusinessException(
                                    "visit_not_found", eventService, HttpStatus.NOT_FOUND));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
        assertEquals("Визит не найден", thrown.getMessage());
        assertTrue(thrown.getBody()::isPresent);
        assertEquals("Визит не найден", thrown.getBody().orElseThrow());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).send(eq("*"), eq(false), captor.capture());
        Object payload = captor.getValue().getBody();
        assertNotNull(payload);
        BusinessException.BusinessError error = (BusinessException.BusinessError) payload;
        assertEquals("Қатысу табылмады", error.getMessage());

        assertFalse(appender.list::isEmpty);
        assertEquals("Visit not found", appender.list.get(0).getFormattedMessage());
    }

    @DisplayName("Заменяет поле `message` в теле ответа-словаря")
    @Test
    void replacesMessageFieldInMapResponseBody() {
        BusinessExceptionLocalizationProperties properties = new BusinessExceptionLocalizationProperties();
        properties.getHttp().setLanguage("ru");
        properties.getLog().setLanguage("ru");

        BusinessException.configureLocalization(new BusinessExceptionLocalization(properties));

        EventService eventService = mock(EventService.class);

        Map<String, Object> responseBody =
                Map.of(
                        "message", "The service point is already busy",
                        "ticket", "A-001",
                        "servicePointId", "sp-1");

        HttpStatusException thrown =
                assertThrows(
                        HttpStatusException.class,
                        () ->
                                new BusinessException(
                                        responseBody,
                                        "Точка обслуживания sp-1 уже занята",
                                        eventService,
                                        HttpStatus.CONFLICT));

        assertEquals(HttpStatus.CONFLICT, thrown.getStatus());
        assertEquals("Точка обслуживания sp-1 уже занята", thrown.getMessage());

        assertTrue(thrown.getBody()::isPresent);
        Object body = thrown.getBody().orElseThrow();
        assertTrue(body instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> localized = (Map<String, Object>) body;
        assertEquals("Точка обслуживания sp-1 уже занята", localized.get("message"));
        assertEquals("A-001", localized.get("ticket"));
        assertEquals("sp-1", localized.get("servicePointId"));
    }
}
