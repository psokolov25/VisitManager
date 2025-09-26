package ru.aritmos.exceptions;

import io.micronaut.context.annotation.Context;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

/** Подключение настроек локализации к {@link BusinessException}. */
@Context
@Singleton
public class BusinessExceptionLocalizationConfigurer {

  private final BusinessExceptionLocalizationProperties properties;

  public BusinessExceptionLocalizationConfigurer(
      BusinessExceptionLocalizationProperties properties) {
    this.properties = properties;
  }

  @PostConstruct
  void configure() {
    updateLocalization();
  }

  @EventListener
  void refreshLocalization(RefreshEvent event) {
    updateLocalization();
  }

  void updateLocalization() {
    BusinessException.configureLocalization(new BusinessExceptionLocalization(properties));
  }
}
