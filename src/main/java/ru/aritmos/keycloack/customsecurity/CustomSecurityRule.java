package ru.aritmos.keycloack.customsecurity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.rules.SecurityRuleResult;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Реализация ручной проверки доступа к ресурсу
 */
@Singleton
public class CustomSecurityRule implements SecurityRule<HttpRequest<Object>> {


    /**
     * Возвращение порядкового номера проверки, обеспечивающего порядок применения правил проверки
     * @return порядковый номер
     */
    @Override
    public int getOrder() {
        return 100000;
    }

    /**
     * Реализация логики проверки доступа к ресурсу (на пример разрешение ресурса по лицензионному соглашения)
     * @param request данные рест запроса, который проверяется на соответствие требованиям
     * @param authentication данные пользователя
     * @return результат проверки
     * SecurityRuleResult.UNKNOWN - результат игнорируется и переходит на следующее правило проверки
     * SecurityRuleResult.REJECTED - результат отрицательный, передается отказ от доступа к ресурсу
     * SecurityRuleResult.ALLOWED - результат положительный, передается разрешение доступа к ресурсу
     */
    @Override
    public Publisher<SecurityRuleResult> check(@Nullable HttpRequest<Object> request, @Nullable Authentication authentication) {
        return Mono.just(SecurityRuleResult.ALLOWED);
    }
}
