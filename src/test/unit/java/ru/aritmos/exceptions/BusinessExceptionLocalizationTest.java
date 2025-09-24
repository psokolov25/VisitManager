package ru.aritmos.exceptions;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.aritmos.test.LoggingAssertions.*;

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

    @Test
    void appliesLocalizationFromConfiguration() {
        BusinessExceptionLocalizationProperties properties = new BusinessExceptionLocalizationProperties();

        properties.getHttp().setLanguage("ru");
        properties.getHttp().setDefaultLanguage("en");
        properties.getHttp().setMessages(
                Map.of("Visit not found", Map.of("ru", "Визит не найден", "en", "Visit not found")));

        properties.getLog().setLanguage("en");
        properties.getLog().setDefaultLanguage("ru");
        properties.getLog().setMessages(
                Map.of("Визит не найден", Map.of("en", "Visit not found", "ru", "Визит не найден")));

        properties.getEvent().setLanguage("kk");
        properties.getEvent().setDefaultLanguage("ru");
        properties.getEvent().setMessages(
                Map.of("Визит не найден", Map.of("kk", "Қатысу табылмады", "ru", "Визит не найден")));

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
                                    "Visit not found", "Визит не найден", eventService, HttpStatus.NOT_FOUND));
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

    @Test
    void fallsBackToSourceLanguageWhenDictionaryIsEmpty() {
        BusinessExceptionLocalizationProperties properties = new BusinessExceptionLocalizationProperties();

        properties.getHttp().setLanguage("ru");
        properties.getHttp().setDefaultLanguage("en");
        properties.getLog().setLanguage("ru");

        BusinessException.configureLocalization(new BusinessExceptionLocalization(properties));

        EventService eventService = mock(EventService.class);

        HttpStatusException thrown =
                assertThrows(
                        HttpStatusException.class,
                        () -> new BusinessException(
                                "Branch not found", "Отделение не найдено", eventService, HttpStatus.NOT_FOUND));

        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
        assertEquals("Отделение не найдено", thrown.getMessage());
        assertTrue(thrown.getBody()::isPresent);
        assertEquals("Отделение не найдено", thrown.getBody().orElseThrow());
    }

    @Test
    void resolvesMessagesByDictionaryCodes() {
        BusinessExceptionLocalizationProperties properties = new BusinessExceptionLocalizationProperties();

        properties.getHttp().setLanguage("ru");
        properties.getHttp().setDefaultLanguage("en");
        properties.getHttp()
                .setMessages(
                        Map.of(
                                "branch.not.found",
                                Map.of("en", "Branch not found", "ru", "Отделение не найдено")));

        BusinessException.configureLocalization(new BusinessExceptionLocalization(properties));

        EventService eventService = mock(EventService.class);

        HttpStatusException thrown =
                assertThrows(
                        HttpStatusException.class,
                        () -> new BusinessException(
                                "Branch not found", "Отделение не найдено", eventService, HttpStatus.NOT_FOUND));

        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatus());
        assertEquals("Отделение не найдено", thrown.getMessage());
        assertTrue(thrown.getBody()::isPresent);
        assertEquals("Отделение не найдено", thrown.getBody().orElseThrow());
    }
}
