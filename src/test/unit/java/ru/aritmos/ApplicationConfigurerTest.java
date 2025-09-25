package ru.aritmos;

import static org.mockito.Mockito.*;

import io.micronaut.context.ApplicationContextBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class ApplicationConfigurerTest {

    private String previousMicronautEnvironments;
    private String previousRedisUri;
    private String previousKafkaBootstrap;

    @BeforeEach
    void rememberSystemProperties() {
        previousMicronautEnvironments = System.getProperty("micronaut.environments");
        previousRedisUri = System.getProperty("redis.uri");
        previousKafkaBootstrap = System.getProperty("kafka.bootstrap.servers");

        System.clearProperty("micronaut.environments");
        System.clearProperty("redis.uri");
        System.clearProperty("kafka.bootstrap.servers");
    }

    @AfterEach
    void restoreSystemProperties() {
        restoreProperty("micronaut.environments", previousMicronautEnvironments);
        restoreProperty("redis.uri", previousRedisUri);
        restoreProperty("kafka.bootstrap.servers", previousKafkaBootstrap);
    }

    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @DisplayName("проверяется сценарий «sets local defaults when infra absent»")
    @Test
    void setsLocalDefaultsWhenInfraAbsent() {
        Application.Configurer configurer = new Application.Configurer();
        ApplicationContextBuilder builder = mock(ApplicationContextBuilder.class);

        configurer.configure(builder);

        verify(builder).defaultEnvironments("dev", "local-no-docker");
        verifyNoMoreInteractions(builder);
    }

    @DisplayName("проверяется сценарий «enables infra by default when remote endpoints available»")
    @Test
    void enablesInfraByDefaultWhenRemoteEndpointsAvailable() {
        System.setProperty("redis.uri", "redis://example");

        Application.Configurer configurer = new Application.Configurer();
        ApplicationContextBuilder builder = mock(ApplicationContextBuilder.class);

        configurer.configure(builder);

        verify(builder).defaultEnvironments("dev", "infra");
        verifyNoMoreInteractions(builder);
    }

    @DisplayName("проверяется сценарий «appends infra when explicit environments provided»")
    @Test
    void appendsInfraWhenExplicitEnvironmentsProvided() {
        System.setProperty("micronaut.environments", "dev");
        System.setProperty("kafka.bootstrap.servers", "kafka:9092");

        Application.Configurer configurer = new Application.Configurer();
        ApplicationContextBuilder builder = mock(ApplicationContextBuilder.class);

        configurer.configure(builder);

        verify(builder).environments("dev", "infra");
        verifyNoMoreInteractions(builder);
    }

    @DisplayName("проверяется сценарий «keeps explicit local profile even with infra signals»")
    @Test
    void keepsExplicitLocalProfileEvenWithInfraSignals() {
        System.setProperty("micronaut.environments", "local-no-docker");
        System.setProperty("redis.uri", "redis://example");

        Application.Configurer configurer = new Application.Configurer();
        ApplicationContextBuilder builder = mock(ApplicationContextBuilder.class);

        configurer.configure(builder);

        verify(builder).environments("local-no-docker");
        verify(builder, never()).defaultEnvironments(any());
        verifyNoMoreInteractions(builder);
    }
}
