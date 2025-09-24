package ru.aritmos.exceptions;

import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;

/**
 * Подключение настроек локализации к {@link BusinessException}.
 */
@Context
public class BusinessExceptionLocalizationConfigurer {

  private final BusinessExceptionLocalization localization;

  public BusinessExceptionLocalizationConfigurer(BusinessExceptionLocalization localization) {
    this.localization = localization;
  }

  @PostConstruct
  void configure() {
    BusinessException.configureLocalization(localization);
  }
}
