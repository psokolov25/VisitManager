package ru.aritmos.api;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import ru.aritmos.model.Branch;
import ru.aritmos.model.EntryPoint;
import ru.aritmos.model.Queue;
import ru.aritmos.model.Service;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** End-to-end test for creating and cancelling a visit. */
@MicronautTest(environments = {"integration", "local-no-docker"})
class VisitLifecycleE2EIT {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void createsAndCancelsVisit() {
        Branch branch = new Branch("b1", "Branch");
        branch.getQueues().put("q1", new Queue("q1", "Queue", "A", 1));
        branch.getServices().put("s1", new Service("s1", "Service", 1, "q1"));
        EntryPoint entryPoint = new EntryPoint();
        entryPoint.setId("e1");
        entryPoint.setName("Entry");
        branch.getEntryPoints().put("e1", entryPoint);
        Map<String, Branch> payload = Map.of("b1", branch);
        client.toBlocking().exchange(HttpRequest.POST("/configuration/branches", payload));

        ArrayList<String> serviceIds = new ArrayList<>();
        serviceIds.add("s1");
        Map<?, ?> visit = client.toBlocking().retrieve(
                HttpRequest.POST("/entrypoint/branches/b1/entryPoints/e1/visit", serviceIds), Map.class);

        HttpResponse<?> deleteResponse = client.toBlocking().exchange(
                HttpRequest.DELETE("/servicepoint/branches/b1/visits/" + visit.get("id")));
        assertEquals(HttpStatus.OK, deleteResponse.getStatus());

        HttpClientResponseException ex = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().retrieve(
                        HttpRequest.GET("/servicepoint/branches/b1/visits/" + visit.get("id")), Map.class));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}

