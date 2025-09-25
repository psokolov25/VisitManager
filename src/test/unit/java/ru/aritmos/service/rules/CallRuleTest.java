package ru.aritmos.service.rules;

import static ru.aritmos.test.LoggingAssertions.*;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;

class CallRuleTest {

    @DisplayName("проверяется сценарий «call rule should extend base rule»")
    @Test
    void callRuleShouldExtendBaseRule() {
        assertTrue(Rule.class.isAssignableFrom(CallRule.class));
    }

    @DisplayName("проверяется сценарий «call method without queue filter should return optional visit»")
    @Test
    void callMethodWithoutQueueFilterShouldReturnOptionalVisit() throws NoSuchMethodException {
        Method method = CallRule.class.getMethod("call", Branch.class, ServicePoint.class);

        assertEquals(Optional.class, method.getReturnType());
        assertTrue(method.getGenericReturnType() instanceof ParameterizedType);
        Type elementType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        assertEquals(Visit.class, elementType);
    }

    @DisplayName("проверяется сценарий «call method with queue filter should return optional visit»")
    @Test
    void callMethodWithQueueFilterShouldReturnOptionalVisit() throws NoSuchMethodException {
        Method method = CallRule.class.getMethod("call", Branch.class, ServicePoint.class, List.class);

        assertEquals(Optional.class, method.getReturnType());
        assertTrue(method.getGenericReturnType() instanceof ParameterizedType);
        Type elementType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        assertEquals(Visit.class, elementType);
    }

    @DisplayName("проверяется сценарий «get available service points should return list of service points»")
    @Test
    void getAvailableServicePointsShouldReturnListOfServicePoints() throws NoSuchMethodException {
        Method method = CallRule.class.getMethod("getAvailiableServicePoints", Branch.class, Visit.class);

        assertEquals(List.class, method.getReturnType());
        assertTrue(method.getGenericReturnType() instanceof ParameterizedType);
        Type elementType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        assertEquals(ServicePoint.class, elementType);
    }
}