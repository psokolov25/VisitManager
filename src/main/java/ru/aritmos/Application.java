package ru.aritmos;

import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Точка входа приложения VisitManager (Micronaut).
 */
@OpenAPIDefinition(
    info = @Info(title = "VisitManagement", version = "alpha09.25"),
    tags = {
      @Tag(name = "Зона обслуживания"),
      @Tag(name = "Зона ожидания"),
      @Tag(name = "Информация об отделении"),
      @Tag(name = "Конфигурация отделений")
    })
public class Application {

  private static final Logger LOG = LoggerFactory.getLogger(Application.class);
  private static final String TEST_RESOURCES_PROPERTY = "micronaut.test.resources.enabled";
  private static final String TEST_RESOURCES_ENV = "MICRONAUT_TEST_RESOURCES_ENABLED";

  /**
   * Запуск приложения Micronaut.
   *
   * @param args аргументы командной строки
   */
  public static void main(String[] args) {
    disableMicronautTestResourcesIfNotExplicitlyConfigured();
    Micronaut.run(Application.class, args);
  }

  private static void disableMicronautTestResourcesIfNotExplicitlyConfigured() {
    boolean propertyDefined = hasNonBlankValue(System.getProperty(TEST_RESOURCES_PROPERTY));
    boolean environmentDefined = hasNonBlankValue(System.getenv(TEST_RESOURCES_ENV));
    if (propertyDefined || environmentDefined) {
      return;
    }
    System.setProperty(TEST_RESOURCES_PROPERTY, Boolean.FALSE.toString());
    LOG.info(
        "Micronaut Test Resources отключены: системный параметр '{}' не задан, используем значение по умолчанию 'false'.",
        TEST_RESOURCES_PROPERTY);
  }

  private static boolean hasNonBlankValue(String value) {
    return value != null && !value.isBlank();
  }

  /**
   * Инициализация конфигурации приложения.
   * Вызывается после создания бина приложения.
   */
  @PostConstruct
  public void getConfiguration() {}

  /** Конфигуратор контекста приложения (устанавливает профиль по умолчанию). */
  @ContextConfigurer
  @SuppressWarnings("unused")
  public static class Configurer implements ApplicationContextConfigurer {
    /**
     * Установка профиля окружения по умолчанию.
     *
     * @param builder билдер контекста приложения
     */
    @Override
    public void configure(@NonNull ApplicationContextBuilder builder) {
      Set<String> explicitEnvironments = resolveExplicitEnvironments();
      boolean infraAvailable = hasInfraSignals();

      if (!explicitEnvironments.isEmpty()) {
        if (infraAvailable
            && !explicitEnvironments.contains("infra")
            && !explicitEnvironments.contains("local-no-docker")) {
          explicitEnvironments.add("infra");
        }
        builder.environments(explicitEnvironments.toArray(String[]::new));
        return;
      }

      Set<String> defaults = new LinkedHashSet<>();
      defaults.add("dev");
      if (infraAvailable) {
        defaults.add("infra");
      } else {
        defaults.add("local-no-docker");
      }
      builder.defaultEnvironments(defaults.toArray(String[]::new));
    }

    private Set<String> resolveExplicitEnvironments() {
      Set<String> propertyDefined = parseEnvironments(System.getProperty("micronaut.environments"));
      if (!propertyDefined.isEmpty()) {
        return propertyDefined;
      }
      return parseEnvironments(System.getenv("MICRONAUT_ENVIRONMENTS"));
    }

    private Set<String> parseEnvironments(String raw) {
      Set<String> result = new LinkedHashSet<>();
      if (!hasText(raw)) {
        return result;
      }
      for (String candidate : raw.split(",")) {
        String value = candidate.trim();
        if (!value.isEmpty()) {
          result.add(value);
        }
      }
      return result;
    }

    private boolean hasInfraSignals() {
      return hasText(System.getenv("REDIS_SERVER"))
          || hasText(System.getenv("KAFKA_SERVER"))
          || hasText(System.getProperty("redis.uri"))
          || hasText(System.getProperty("kafka.bootstrap.servers"));
    }

    private boolean hasText(String value) {
      return value != null && !value.isBlank();
    }
  }
}
