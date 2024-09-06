package ru.aritmos.keycloack.customsecurity;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.security.rules.SecurityRuleResult;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@Singleton
public class CustomSecurityRule implements SecurityRule<HttpRequest<Object>> {



    @Override
    public int getOrder() {
        return 100000;
    }




    @Override
    public Publisher<SecurityRuleResult> check(@Nullable HttpRequest<Object> request, @Nullable Authentication authentication) {
        return Mono.just(SecurityRuleResult.UNKNOWN);
    }
}
