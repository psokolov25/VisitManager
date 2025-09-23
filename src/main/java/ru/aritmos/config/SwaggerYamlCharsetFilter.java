package ru.aritmos.config;

import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import org.reactivestreams.Publisher;

/**
 * Гарантирует корректное указание кодировки UTF-8 для YAML-спецификаций OpenAPI,
 * отдаваемых Swagger UI. Без явного указания браузер интерпретирует документ в ISO-8859-1,
 * из-за чего вместо русских символов отображаются «крякозябры».
 */
@Filter("/swagger/**")
public final class SwaggerYamlCharsetFilter implements HttpServerFilter {

    private static final String YAML_SHORT_EXTENSION = ".yml";
    private static final String YAML_LONG_EXTENSION = ".yaml";
    private static final String CHARSET_PREFIX = "; charset=";

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        return Publishers.map(chain.proceed(request), response -> {
            if (isYamlSpecificationRequest(request)) {
                enforceUtf8Charset(response);
            }
            return response;
        });
    }

    private boolean isYamlSpecificationRequest(HttpRequest<?> request) {
        String path = Optional.ofNullable(request.getPath()).orElse("").toLowerCase(Locale.ROOT);
        return path.endsWith(YAML_SHORT_EXTENSION) || path.endsWith(YAML_LONG_EXTENSION);
    }

    private void enforceUtf8Charset(MutableHttpResponse<?> response) {
        String currentContentType = response.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        if (currentContentType == null || currentContentType.isBlank()) {
            response.getHeaders().set(HttpHeaders.CONTENT_TYPE, yamlContentTypeWithCharset("text/yaml"));
            return;
        }

        String normalized = currentContentType.toLowerCase(Locale.ROOT);
        if (!normalized.contains("yaml")) {
            return;
        }

        if (normalized.contains("charset")) {
            return;
        }

        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, yamlContentTypeWithCharset(currentContentType));
    }

    private String yamlContentTypeWithCharset(String baseContentType) {
        return baseContentType + CHARSET_PREFIX + StandardCharsets.UTF_8.name();
    }
}
