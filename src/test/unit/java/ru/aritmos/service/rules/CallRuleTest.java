package ru.aritmos.service.rules;

import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.Branch;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.visit.Visit;

class CallRuleTest {

    @DisplayName("Call Rule Should Extend Base Rule")
    @Test
    void callRuleShouldExtendBaseRule() {
        assertTrue(Rule.class.isAssignableFrom(CallRule.class));
    }

    @DisplayName("Call Method Without Queue Filter Should Return Optional Visit")
    @Test
    void callMethodWithoutQueueFilterShouldReturnOptionalVisit() throws NoSuchMethodException {
        Method method = CallRule.class.getMethod("call", Branch.class, ServicePoint.class);

        assertEquals(Optional.class, method.getReturnType());
        assertTrue(method.getGenericReturnType() instanceof ParameterizedType);
        Type elementType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        assertEquals(Visit.class, elementType);
    }

    @DisplayName("Call Method With Queue Filter Should Return Optional Visit")
    @Test
    void callMethodWithQueueFilterShouldReturnOptionalVisit() throws NoSuchMethodException {
        Method method = CallRule.class.getMethod("call", Branch.class, ServicePoint.class, List.class);

        assertEquals(Optional.class, method.getReturnType());
        assertTrue(method.getGenericReturnType() instanceof ParameterizedType);
        Type elementType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        assertEquals(Visit.class, elementType);
    }

    @DisplayName("Get Available Service Points Should Return List Of Service Points")
    @Test
    void getAvailableServicePointsShouldReturnListOfServicePoints() throws NoSuchMethodException {
        Method method = CallRule.class.getMethod("getAvailiableServicePoints", Branch.class, Visit.class);

        assertEquals(List.class, method.getReturnType());
        assertTrue(method.getGenericReturnType() instanceof ParameterizedType);
        Type elementType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
        assertEquals(ServicePoint.class, elementType);
    }
}
