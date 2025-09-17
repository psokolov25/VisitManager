package ru.aritmos;

import static org.mockito.Mockito.*;

import io.micronaut.context.ApplicationContextBuilder;
import org.junit.jupiter.api.Test;

class ApplicationConfigurerTest {

    @Test
    void setsDefaultEnvironmentDev() {
        Application.Configurer configurer = new Application.Configurer();
        ApplicationContextBuilder builder = mock(ApplicationContextBuilder.class);

        configurer.configure(builder);

        verify(builder).defaultEnvironments("dev");
    }
}

