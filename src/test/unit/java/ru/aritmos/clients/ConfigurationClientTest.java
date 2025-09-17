package ru.aritmos.clients;

import static org.junit.jupiter.api.Assertions.*;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;
import java.lang.reflect.Method;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class ConfigurationClientTest {

    @Test
    void интерфейсОтмеченClientСОжидаемымURL() {
        Client annotation = ConfigurationClient.class.getAnnotation(Client.class);
        assertNotNull(annotation);
        assertEquals("${micronaut.application.configurationURL}", annotation.value());
    }

    @Test
    void методGetConfigurationИмеетАннотациюGet() throws NoSuchMethodException {
        Method method = ConfigurationClient.class.getMethod("getConfiguration");
        assertEquals(HashMap.class, method.getReturnType());
        assertTrue(method.isAnnotationPresent(Get.class));
    }
}
