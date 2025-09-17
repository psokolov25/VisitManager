package ru.aritmos.docs;

import static ru.aritmos.test.LoggingAssertions.*;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class CurlCheatsheetGeneratorTest {

    @Test
    void escapeHtml() throws Exception {
        Method escape = CurlCheatsheetGenerator.class.getDeclaredMethod("escape", String.class);
        escape.setAccessible(true);
        String result = (String) escape.invoke(null, "<tag>&");
        assertEquals("&lt;tag&gt;&amp;", result);
    }

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
}

