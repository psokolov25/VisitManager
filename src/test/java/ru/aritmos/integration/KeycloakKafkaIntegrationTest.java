package ru.aritmos.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Интеграционный тест, использующий реальные Keycloak и Kafka.
 */
@Testcontainers
class KeycloakKafkaIntegrationTest {

    @Container
    static final KeycloakContainer keycloak =
        new KeycloakContainer("quay.io/keycloak/keycloak:22.0.1")
            .withAdminUsername("admin")
            .withAdminPassword("admin");

    @Container
    static final KafkaContainer kafka =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Test
    void keycloakAndKafkaAreAccessible() throws Exception {
        assertTrue(keycloak.isRunning(), "Keycloak должен быть запущен");
        HttpResponse<String> response =
            HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(keycloak.getAuthServerUrl()))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "Keycloak отвечает");

        String topic = "test-topic";
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", kafka.getBootstrapServers());
        producerProps.put(
            "key.serializer",
            "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(
            "value.serializer",
            "org.apache.kafka.common.serialization.StringSerializer");
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {
            producer.send(new ProducerRecord<>(topic, "k", "v")).get();
        }

        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", kafka.getBootstrapServers());
        consumerProps.put("group.id", "test-group");
        consumerProps.put(
            "key.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(
            "value.deserializer",
            "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("auto.offset.reset", "earliest");
        try (Consumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(Collections.singletonList(topic));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            assertFalse(records.isEmpty(), "Kafka должен вернуть записанное сообщение");
            assertEquals("v", records.iterator().next().value());
        }
    }
}

