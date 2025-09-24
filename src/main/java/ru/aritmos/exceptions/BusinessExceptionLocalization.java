package ru.aritmos.exceptions;

import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Singleton;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Сервис локализации сообщений {@link BusinessException} на основе настроек.
 */
@Singleton
public class BusinessExceptionLocalization {

  private static final Logger LOG = LoggerFactory.getLogger(BusinessExceptionLocalization.class);

  private final BusinessExceptionLocalizationProperties properties;

  public BusinessExceptionLocalization(BusinessExceptionLocalizationProperties properties) {
    this.properties = Objects.requireNonNull(properties, "Настройки локализации не заданы");
  }

  /**
   * Создать локализацию с настройками по умолчанию.
   */
  public static BusinessExceptionLocalization defaultLocalization() {
    return new BusinessExceptionLocalization(new BusinessExceptionLocalizationProperties());
  }

  /**
   * Применить локализацию для финальных сообщений исключения.
   *
   * @param responseBody тело ответа, отправляемое клиенту
   * @param clientMessage сообщение для клиента
   * @param logMessage сообщение для лога
   * @param eventMessage сообщение для события
   * @return локализованные значения
   */
  public LocalizedMessages localize(
      @Nullable Object responseBody,
      @Nullable String clientMessage,
      @Nullable String logMessage,
      @Nullable String eventMessage) {

    Map<String, String> baseMessages = collectBaseMessages(clientMessage, logMessage, eventMessage);

    String localizedClient = localizeHttp(clientMessage, baseMessages);
    Object localizedResponse = localizeResponseBody(responseBody, localizedClient, baseMessages);
    String localizedLog = localizeLog(logMessage, baseMessages);
    String localizedEvent = localizeEvent(eventMessage, baseMessages);
    return new LocalizedMessages(localizedResponse, localizedClient, localizedLog, localizedEvent);
  }

  private Map<String, String> collectBaseMessages(
      @Nullable String clientMessage, @Nullable String logMessage, @Nullable String eventMessage) {
    Map<String, String> baseMessages = new LinkedHashMap<>();
    addBaseMessage(baseMessages, properties.getHttp().getSourceLanguage(), clientMessage);
    addBaseMessage(baseMessages, properties.getLog().getSourceLanguage(), logMessage);
    addBaseMessage(baseMessages, properties.getEvent().getSourceLanguage(), eventMessage);
    return baseMessages;
  }

  private void addBaseMessage(Map<String, String> messages, @Nullable String language, @Nullable String message) {
    if (language != null && message != null && !message.isBlank()) {
      messages.putIfAbsent(language, message);
    }
  }

  private Object localizeResponseBody(
      @Nullable Object responseBody,
      @Nullable String localizedClient,
      Map<String, String> baseMessages) {
    if (responseBody instanceof CharSequence sequence) {
      return localizeHttp(sequence.toString(), baseMessages);
    }
    if (responseBody == null) {
      return localizedClient;
    }
    return responseBody;
  }

  private String localizeHttp(@Nullable String message, Map<String, String> baseMessages) {
    return properties.getHttp().resolve(message, baseMessages);
  }

  private String localizeLog(@Nullable String message, Map<String, String> baseMessages) {
    BusinessExceptionLocalizationProperties.ChannelLocalization channel = properties.getLog();
    String localized = channel.resolve(message, baseMessages);
    if (localized == null && message != null) {
      LOG.warn("Не удалось локализовать сообщение лога '{}', используется исходный текст", message);
    }
    return localized;
  }

  private String localizeEvent(@Nullable String message, Map<String, String> baseMessages) {
    BusinessExceptionLocalizationProperties.ChannelLocalization channel = properties.getEvent();
    String localized = channel.resolve(message, baseMessages);
    if (localized == null && message != null) {
      LOG.warn("Не удалось локализовать сообщение события '{}', используется исходный текст", message);
    }
    return localized;
  }

  /**
   * Результат локализации сообщений.
   */
  public static final class LocalizedMessages {
    private final Object responseBody;
    private final String clientMessage;
    private final String logMessage;
    private final String eventMessage;

    public LocalizedMessages(
        @Nullable Object responseBody,
        @Nullable String clientMessage,
        @Nullable String logMessage,
        @Nullable String eventMessage) {
      this.responseBody = responseBody;
      this.clientMessage = clientMessage;
      this.logMessage = logMessage;
      this.eventMessage = eventMessage;
    }

    @Nullable
    public Object responseBody() {
      return responseBody;
    }

    @Nullable
    public String clientMessage() {
      return clientMessage;
    }

    @Nullable
    public String logMessage() {
      return logMessage;
    }

    @Nullable
    public String eventMessage() {
      return eventMessage;
    }
  }
}
