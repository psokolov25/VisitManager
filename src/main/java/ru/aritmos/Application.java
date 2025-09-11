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

      builder.defaultEnvironments("dev");
    }
  }
}
