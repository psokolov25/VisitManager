package ru.aritmos.test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.micronaut.cache.CacheManager;
import io.micronaut.cache.SyncCache;
import io.micronaut.core.type.Argument;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpResponse;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import reactor.core.publisher.Mono;
import ru.aritmos.clients.ConfigurationClient;
import ru.aritmos.clients.PrinterClient;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.events.clients.DataBusClient;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;
import ru.aritmos.service.rules.client.CallRuleClient;

/**
 * Набор тестовых заглушек Micronaut, заменяющих внешние клиенты и кеши в
 * модульных тестах.
 *
 * <p>Компоненты объявлены как {@code @Replaces}, поэтому автоматически
 * подставляются вместо настоящих реализаций при запуске unit-тестов. Это
 * гарантирует повторяемость сценариев и отсутствие сетевых вызовов.
 */
@Factory
@Requires(notEnv = "integration")
public class MokiatoStubs {

    /**
     * Создает заглушку клиента Keycloak с предсказуемыми ответами для тестов.
     *
     * @return подмененный {@link KeyCloackClient}
     */
    @Singleton
    @Replaces(KeyCloackClient.class)
    KeyCloackClient keyCloackClient() {
        KeyCloackClient mock = mock(KeyCloackClient.class);
        when(mock.getUserInfo(anyString())).thenAnswer(invocation -> {
            String userName = invocation.getArgument(0, String.class);
            UserRepresentation u = new UserRepresentation();
            u.setId(UUID.randomUUID().toString());
            u.setUsername(userName);
            u.setFirstName("Test");
            u.setLastName("User");
            u.setEmail(userName + "@example.local");
            return Optional.of(u);
        });
        when(mock.getAllBranchesOfUser(anyString())).thenReturn(java.util.Collections.<GroupRepresentation>emptyList());
        when(mock.isUserModuleTypeByUserName(anyString(), anyString())).thenReturn(true);
        when(mock.getUserBySid(anyString())).thenAnswer(invocation -> {
            UserRepresentation u = new UserRepresentation();
            u.setId(UUID.randomUUID().toString());
            u.setUsername("test-user");
            u.setFirstName("Test");
            u.setLastName("User");
            u.setEmail("test-user@example.local");
            return Optional.of(u);
        });
        return mock;
    }

    /**
     * Предоставляет упрощенную конфигурацию отделений, доступную офлайн.
     *
     * @return заглушка {@link ConfigurationClient}
     */
    @Singleton
    @Replaces(ConfigurationClient.class)
    ConfigurationClient configurationClient() {
        ConfigurationClient mock = mock(ConfigurationClient.class);
        when(mock.getConfiguration()).thenReturn(new HashMap<>());
        return mock;
    }

    /**
     * Возвращает клиент бизнес-правил, всегда сообщающий об отсутствии
     * ограничений.
     *
     * @return заглушка {@link CallRuleClient}
     */
    @Singleton
    @Replaces(CallRuleClient.class)
    CallRuleClient callRuleClient() {
        CallRuleClient mock = mock(CallRuleClient.class);
        when(mock.callRule(any(Branch.class), any(ServicePoint.class))).thenReturn(Optional.empty());
        return mock;
    }

    /**
     * Создает клиент печати талонов, подтверждающий успешную печать без
     * взаимодействия с принтером.
     *
     * @return заглушка {@link PrinterClient}
     */
    @Singleton
    @Replaces(PrinterClient.class)
    PrinterClient printerClient() {
        PrinterClient mock = mock(PrinterClient.class);
        when(mock.print(anyString(), anyBoolean(), any(Visit.class))).thenReturn(Mono.just(HttpResponse.ok()));
        return mock;
    }

    /**
     * Возвращает клиент шины событий, имитирующий подтверждение отправки.
     *
     * @return заглушка {@link DataBusClient}
     */
    @Singleton
    @Replaces(DataBusClient.class)
    DataBusClient dataBusClient() {
        DataBusClient mock = mock(DataBusClient.class);
        when(mock.send(anyString(), anyBoolean(), anyString(), anyString(), anyString(), any()))
                .thenAnswer(
                        invocation -> {
                            String type = invocation.getArgument(4, String.class);
                            return Mono.just(Map.of("status", "stubbed", "type", type));
                        });
        return mock;
    }

    /**
     * Подменяет менеджер кешей на минимальную реализацию, чтобы исключить
     * влияние кеширования на тесты.
     *
     * @return заглушка {@link CacheManager}
     */
    @Singleton
    @Replaces(bean = io.micronaut.cache.DefaultCacheManager.class)
    @SuppressWarnings("unchecked")
    CacheManager<Object> cacheManager() {
        CacheManager<Object> manager = mock(CacheManager.class);
        SyncCache<Object> cache = mock(SyncCache.class);

        when(manager.getCache(anyString())).thenReturn(cache);
        when(manager.getCacheNames()).thenReturn(Set.of());

        when(cache.getName()).thenReturn("noop");
        when(cache.getNativeCache()).thenReturn(null);
        when(cache.get(any(), any(Class.class))).thenReturn(Optional.empty());
        when(cache.get(any(), any(Argument.class))).thenReturn(Optional.empty());
        when(cache.get(any(), any(Argument.class), any(Supplier.class)))
            .thenAnswer(invocation -> {
                Supplier<?> supplier = invocation.getArgument(2);
                return supplier.get();
            });
        when(cache.putIfAbsent(any(), any()))
            .thenAnswer(invocation -> Optional.ofNullable(invocation.getArgument(1)));
        return manager;
    }
}
