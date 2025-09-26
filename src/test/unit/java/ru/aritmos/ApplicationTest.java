package ru.aritmos;

import static ru.aritmos.test.LoggingAssertions.assertDoesNotThrow;
import static org.mockito.Mockito.mockStatic;
import org.junit.jupiter.api.DisplayName;

import io.micronaut.runtime.Micronaut;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class ApplicationTest {
    @DisplayName("Точка входа передаёт аргументы фреймворку Micronaut при запуске")
    @Test
    void main() {
        try (MockedStatic<Micronaut> mic = mockStatic(Micronaut.class)) {
            String[] args = {"--flag"};

            Application.main(args);

            mic.verify(() -> Micronaut.run(Application.class, args));
        }
    }

    @DisplayName("Получение конфигурации приложения не выбрасывает исключений")
    @Test
    void getConfiguration() {
        Application app = new Application();
        assertDoesNotThrow(app::getConfiguration);
    }
}
