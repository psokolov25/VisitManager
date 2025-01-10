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

@OpenAPIDefinition(
    info = @Info(title = "VisitManagement", version = "0.8"),
    tags = {
      @Tag(name = "Зона обслуживания"),
      @Tag(name = "Зона ожидания"),
      @Tag(name = "Информация об отделении"),
      @Tag(name = "Конфигурация отделений")
    })
public class Application {

  public static void main(String[] args) {
    Micronaut.run(Application.class, args);
  }

  @PostConstruct
  public void getConfiguration() {}

  @ContextConfigurer
  public static class Configurer implements ApplicationContextConfigurer {
    @Override
    public void configure(@NonNull ApplicationContextBuilder builder) {

      builder.defaultEnvironments("dev");
    }
  }
}
