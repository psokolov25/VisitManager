package ru.aritmos.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static ru.aritmos.test.LoggingAssertions.*;
import org.junit.jupiter.api.DisplayName;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aritmos.events.services.EventService;
import ru.aritmos.keycloack.service.KeyCloackClient;
import ru.aritmos.model.ServicePoint;
import ru.aritmos.model.User;
import ru.aritmos.service.BranchService;
import ru.aritmos.service.Services;
import ru.aritmos.service.VisitService;
import ru.aritmos.test.TestLoggingExtension;

/**
 * Тесты для {@link ServicePointController#getServicePoint(String, String)} с подробными логами.
 */
@ExtendWith(TestLoggingExtension.class)
class ServicePointControllerGetServicePointTest {

    private static final Logger LOG = LoggerFactory.getLogger(ServicePointControllerGetServicePointTest.class);

    private ServicePointController controllerWith(VisitService visitService) {
        ServicePointController controller = new ServicePointController();
        controller.services = mock(Services.class);
        controller.branchService = mock(BranchService.class);
        controller.visitService = visitService;
        controller.eventService = mock(EventService.class);
        controller.keyCloackClient = mock(KeyCloackClient.class);
        return controller;
    }

    @DisplayName("Возвращает точку обслуживания с оператором на перерыве")
    @Test
    void returnsServicePointWithOperatorOnBreak() {
        LOG.info("Шаг 1: подготавливаем карту точек обслуживания без оператора");
        VisitService visitService = mock(VisitService.class);
        ServicePoint servicePoint = new ServicePoint("sp-1", "Окно 1");
        HashMap<String, ServicePoint> map = new HashMap<>();
        map.put(servicePoint.getId(), servicePoint);
        when(visitService.getServicePointHashMap("branch-1")).thenReturn(map);

        LOG.info("Шаг 2: добавляем пользователя на перерыве, связанного с точкой обслуживания");
        User pausedUser = new User("user-1", "operator", null);
        pausedUser.setLastBreakStartTime(ZonedDateTime.now().minusMinutes(15));
        pausedUser.setLastServicePointId(servicePoint.getId());
        when(visitService.getUsers("branch-1")).thenReturn(List.of(pausedUser));

        LOG.info("Шаг 3: вызываем контроллер и проверяем, что пользователь подставлен в точку");
        ServicePointController controller = controllerWith(visitService);
        Optional<ServicePoint> result = controller.getServicePoint("branch-1", servicePoint.getId());

        assertTrue(result.isPresent());
        assertSame(servicePoint, result.get());
        assertEquals(pausedUser, result.get().getUser());
        verify(visitService, atLeastOnce()).getServicePointHashMap("branch-1");
        verify(visitService).getUsers("branch-1");
    }

    @DisplayName("Возвращает пустой результат при отсутствии точки обслуживания")
    @Test
    void returnsEmptyWhenServicePointIsMissing() {
        LOG.info("Шаг 1: подготавливаем пустую карту точек обслуживания");
        VisitService visitService = mock(VisitService.class);
        when(visitService.getServicePointHashMap("branch-404")).thenReturn(new HashMap<>());

        LOG.info("Шаг 2: вызываем контроллер для несуществующей точки");
        ServicePointController controller = controllerWith(visitService);
        Optional<ServicePoint> result = controller.getServicePoint("branch-404", "sp-missing");

        LOG.info("Шаг 3: убеждаемся, что точка не найдена и пользователи не запрашивались");
        assertTrue(result.isEmpty());
        verify(visitService, atLeastOnce()).getServicePointHashMap("branch-404");
        verify(visitService, never()).getUsers(anyString());
    }
}
