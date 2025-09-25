package ru.aritmos.config;

import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.filter.ServerFilterChain;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Проверки {@link SwaggerYamlCharsetFilter}, гарантирующие корректное указание кодировки UTF-8
 * для YAML-спецификаций.
 */
@ExtendWith(TestLoggingExtension.class)
class SwaggerYamlCharsetFilterTest {

    private static final Logger LOG = LoggerFactory.getLogger(SwaggerYamlCharsetFilterTest.class);

    @DisplayName("Добавление базового заголовка Content-Type для YAML без заголовка и логирование всех шагов")
    @Test
    void addsDefaultYamlContentTypeWhenHeaderMissing() {
        LOG.info("Шаг 1: создаём ответ без заголовка Content-Type для YAML-спецификации.");
        MutableHttpResponse<?> response = HttpResponse.ok();
        response.getHeaders().remove(HttpHeaders.CONTENT_TYPE);

        LOG.info("Шаг 2: прогоняем фильтр для пути /swagger/openapi.yml.");
        MutableHttpResponse<?> result = applyFilter("/swagger/openapi.yml", response);

        LOG.info("Шаг 3: проверяем, что фильтр выставил text/yaml с кодировкой UTF-8.");
        assertEquals("text/yaml; charset=UTF-8", result.getHeaders().get(HttpHeaders.CONTENT_TYPE));
    }

    @DisplayName("Добавление кодировки UTF-8 к YAML-ответу без charset при обработке swagger-файла")
    @Test
    void appendsCharsetForYamlWithoutEncoding() {
        LOG.info("Шаг 1: подготавливаем ответ с типом application/yaml без charset.");
        MutableHttpResponse<?> response = HttpResponse.ok();
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/yaml");

        LOG.info("Шаг 2: прогоняем фильтр для файла со строкой .yaml.");
        MutableHttpResponse<?> result = applyFilter("/swagger/openapi.yaml", response);

        LOG.info("Шаг 3: убеждаемся, что к типу добавлена кодировка UTF-8.");
        assertEquals("application/yaml; charset=UTF-8", result.getHeaders().get(HttpHeaders.CONTENT_TYPE));
    }

    @DisplayName("Сохранение исходной кодировки YAML-ответа при повторном прохождении фильтра")
    @Test
    void keepsExistingCharsetIntact() {
        LOG.info("Шаг 1: настраиваем ответ с уже указанной кодировкой.");
        MutableHttpResponse<?> response = HttpResponse.ok();
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/yaml; charset=UTF-8");

        LOG.info("Шаг 2: применяем фильтр повторно для YAML.");
        MutableHttpResponse<?> result = applyFilter("/swagger/openapi.yaml", response);

        LOG.info("Шаг 3: проверяем, что значение заголовка не изменилось.");
        assertEquals("application/yaml; charset=UTF-8", result.getHeaders().get(HttpHeaders.CONTENT_TYPE));
    }

    @DisplayName("Пропуск нерелевантных ответов: не-YAML содержимое и не-YAML запросы остаются неизменными")
    @Test
    void skipsNonYamlResponsesAndRequests() {
        LOG.info("Шаг 1: ответ с типом application/json для запроса не к YAML-спецификации.");
        MutableHttpResponse<?> jsonResponse = HttpResponse.ok();
        jsonResponse.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");

        LOG.info("Шаг 2: фильтр не должен менять заголовок для /swagger/index.html.");
        MutableHttpResponse<?> jsonResult = applyFilter("/swagger/index.html", jsonResponse);
        assertEquals("application/json", jsonResult.getHeaders().get(HttpHeaders.CONTENT_TYPE));

        LOG.info("Шаг 3: ответ с типом text/plain для YAML-пути также не содержит признака yaml.");
        MutableHttpResponse<?> plainResponse = HttpResponse.ok();
        plainResponse.getHeaders().set(HttpHeaders.CONTENT_TYPE, "text/plain");

        LOG.info("Шаг 4: после фильтра Content-Type остаётся без изменений.");
        MutableHttpResponse<?> plainResult = applyFilter("/swagger/openapi.yaml", plainResponse);
        assertEquals("text/plain", plainResult.getHeaders().get(HttpHeaders.CONTENT_TYPE));
    }

    private MutableHttpResponse<?> applyFilter(String path, MutableHttpResponse<?> response) {
        SwaggerYamlCharsetFilter filter = new SwaggerYamlCharsetFilter();
        HttpRequest<?> request = HttpRequest.GET(path);
        ServerFilterChain chain = req -> {
            LOG.info("Пробрасываем запрос {} через тестовую цепочку фильтров.", req.getPath());
            return Publishers.just(response);
        };
        return Mono.from(filter.doFilter(request, chain)).block();
    }
}
