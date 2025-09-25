package ru.aritmos.clients;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;
import java.lang.reflect.Method;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class ConfigurationClientTest {

    @DisplayName("Interface Is Annotated With Client Using Expected Url")
    @Test
    void interfaceIsAnnotatedWithClientUsingExpectedUrl() {
        Client annotation = ConfigurationClient.class.getAnnotation(Client.class);
        assertNotNull(annotation);
        assertEquals("${micronaut.application.configurationURL}", annotation.value());
    }

    @DisplayName("Get Configuration Method Has Get Annotation")
    @Test
    void getConfigurationMethodHasGetAnnotation() throws NoSuchMethodException {
        Method method = ConfigurationClient.class.getMethod("getConfiguration");
        assertEquals(HashMap.class, method.getReturnType());
        assertTrue(method.isAnnotationPresent(Get.class));
    }
}
