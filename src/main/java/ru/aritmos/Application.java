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

  /**
   * Запуск приложения Micronaut.
   *
   * @param args аргументы командной строки
   */
  public static void main(String[] args) {
    Micronaut.run(Application.class, args);
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
