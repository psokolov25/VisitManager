package ru.aritmos.docs;

import static ru.aritmos.test.LoggingAssertions.*;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
}

