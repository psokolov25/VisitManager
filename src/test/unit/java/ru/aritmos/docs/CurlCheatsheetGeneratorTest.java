package ru.aritmos.docs;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import ru.aritmos.test.TestLoggingExtension;

/**
 * Набор тестов для {@link CurlCheatsheetGenerator}, проверяющий извлечение примеров curl и
 * корректное формирование HTML.
 */
@ExtendWith(TestLoggingExtension.class)
class CurlCheatsheetGeneratorTest {

    /**
     * Убеждаемся, что экранирование спецсимволов HTML работает корректно для угловых скобок и
     * амперсанда.
     */
    @DisplayName("Экранирование ХТМЛ заменяет спецсимволы угловых скобок и амперсанда")
    @Test
    void escapeHtml() throws Exception {
        Method escape = CurlCheatsheetGenerator.class.getDeclaredMethod("escape", String.class);
        escape.setAccessible(true);
        String result = (String) escape.invoke(null, "<tag>&");
        assertEquals("&lt;tag&gt;&amp;", result);
    }

    /**
     * Проверяет, что секция контроллера с комментариями «Пример curl» добавляется в итоговый HTML
     * и содержит исходный пример запроса.
     */
    @DisplayName("Добавление секции контроллера переносит пример курл в разметку ХТМЛ")
    @Test
    void appendControllerSectionAddsExample() throws Exception {
        Path file = Files.createTempFile("Controller", ".java");
        Files.writeString(
            file,
            """
            /**
             * Пример curl
             * <pre><code>
             * curl http://localhost/test
             * </code></pre>
             */
            @Get("/test")
            class Sample {}
            """,
            StandardCharsets.UTF_8);

        StringBuilder html = new StringBuilder();
        Method append = CurlCheatsheetGenerator.class.getDeclaredMethod("appendControllerSection", StringBuilder.class, Path.class);
        append.setAccessible(true);
        append.invoke(null, html, file);

        String output = html.toString();
        assertTrue(output.contains("curl http://localhost/test"));
        assertTrue(output.contains("<section>"));
    }

    /**
     * Убеждаемся, что главный метод создаёт HTML-файл со сведениями из контроллеров.
     */
    @DisplayName("Главный метод формирует шпаргалку в формате ХТМЛ по запросам курл")
    @Test
    void mainGeneratesCheatsheet() throws Exception {
        Path projectRoot = Paths.get("").toAbsolutePath();
        Path controllersDir = projectRoot.resolve("src/main/java/ru/aritmos/api");
        Path sampleController = controllersDir.resolve("CurlCheatsheetGeneratorTestController.java");
        String sampleSource =
                """
                package ru.aritmos.api;

                import io.micronaut.http.annotation.Get;

                /**
                 * Пример curl
                 * <pre><code>
                 * curl http://localhost/api/test-cheatsheet
                 * </code></pre>
                 */
                public class CurlCheatsheetGeneratorTestController {
                    @Get("/api/test-cheatsheet")
                    void sample() {}
                }
                """;

        Path outputFile = projectRoot.resolve("docs/site/index.html");
        String originalHtml = Files.exists(outputFile) ? Files.readString(outputFile, StandardCharsets.UTF_8) : null;

        Files.createDirectories(controllersDir);
        Files.writeString(sampleController, sampleSource, StandardCharsets.UTF_8);
        try {
            CurlCheatsheetGenerator.main(new String[0]);

            String html = Files.readString(outputFile, StandardCharsets.UTF_8);
            assertTrue(html.contains("curl http://localhost/api/test-cheatsheet"));
            assertTrue(html.contains("CurlCheatsheetGeneratorTestController.java"));
        } finally {
            Files.deleteIfExists(sampleController);
            if (originalHtml != null) {
                Files.writeString(outputFile, originalHtml, StandardCharsets.UTF_8);
            } else {
                Files.deleteIfExists(outputFile);
            }
        }
    }


    /**
     * Проверяет, что генератор извлекает метод и URI из аннотации даже при многострочном описании
     * с параметром {@code uri} на отдельной строке.
     */
    @DisplayName("Секция контроллера извлекает универсальный идентификатор ресурса из многострочной аннотации")
    @Test
    void appendControllerSectionResolvesUriFromAttributeBlock() throws Exception {
        Path file = Files.createTempFile("ControllerMulti", ".java");
        Files.writeString(
                file,
                """
                package example;

                import io.micronaut.http.MediaType;
                import io.micronaut.http.annotation.Post;

                /**
                 * Пример curl
                 * <pre><code>
                 * curl -X POST http://localhost/api/multi
                 * </code></pre>
                 */
                @Post(
                    consumes = MediaType.APPLICATION_JSON,
                    produces = MediaType.APPLICATION_JSON,
                    uri = "/api/multi")
                final class Sample {}
                """,
                StandardCharsets.UTF_8);

        StringBuilder html = new StringBuilder();
        Method append = CurlCheatsheetGenerator.class.getDeclaredMethod("appendControllerSection", StringBuilder.class, Path.class);
        append.setAccessible(true);

        try {
            append.invoke(null, html, file);
            String output = html.toString();
            assertTrue(output.contains("<h3>POST /api/multi</h3>"));
            assertTrue(output.contains("curl -X POST http://localhost/api/multi"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    /**
     * Убеждаемся, что неполные примеры без закрывающего тега {@literal </code></pre>} пропускаются
     * и не попадают в HTML.
     */
    @DisplayName("Секция контроллера пропускает незавершённый пример без закрывающего тега")
    @Test
    void appendControllerSectionSkipsIncompleteExample() throws Exception {
        Path file = Files.createTempFile("ControllerBroken", ".java");
        Files.writeString(
                file,
                """
                package example;

                import io.micronaut.http.annotation.Get;

                /**
                 * Пример curl
                 * <pre><code>
                 * curl http://localhost/api/broken
                 */
                @Get("/api/broken")
                final class Sample {}
                """,
                StandardCharsets.UTF_8);

        StringBuilder html = new StringBuilder();
        Method append = CurlCheatsheetGenerator.class.getDeclaredMethod("appendControllerSection", StringBuilder.class, Path.class);
        append.setAccessible(true);

        try {
            append.invoke(null, html, file);
            String output = html.toString();
            assertFalse(output.contains("curl http://localhost/api/broken"));
            assertFalse(output.contains("<article>"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

}

