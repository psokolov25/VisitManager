package ru.aritmos;

import static ru.aritmos.test.LoggingAssertions.assertDoesNotThrow;
import static org.mockito.Mockito.mockStatic;

import io.micronaut.runtime.Micronaut;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class ApplicationTest {
    @Test
    void main() {
        try (MockedStatic<Micronaut> mic = mockStatic(Micronaut.class)) {
            String[] args = {"--flag"};

            Application.main(args);

            mic.verify(() -> Micronaut.run(Application.class, args));
        }
    }

    @Test
    void getConfiguration() {
        Application app = new Application();
        assertDoesNotThrow(app::getConfiguration);
    }
}
