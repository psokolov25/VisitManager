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

    boolean fallbackToLogChannel = shouldFallbackToLogChannel();
    String localizedClient = localizeHttp(clientMessage);
    localizedClient =
        fallbackToLogChannel
            ? fallbackToLogMessageIfNotTranslated(clientMessage, localizedClient, logMessage)
            : localizedClient;
    Object localizedResponse =
        localizeResponseBody(responseBody, localizedClient, logMessage, fallbackToLogChannel);
    String localizedLog = localizeLog(logMessage);
    String localizedEvent = localizeEvent(eventMessage);
    return new LocalizedMessages(localizedResponse, localizedClient, localizedLog, localizedEvent);
  }

  private Object localizeResponseBody(
      @Nullable Object responseBody,
      @Nullable String localizedClient,
      @Nullable String logMessage,
      boolean fallbackToLogChannel) {
    if (responseBody instanceof CharSequence sequence) {
      String localized = localizeHttp(sequence.toString());
      if (fallbackToLogChannel
          && sequence.length() > 0
          && Objects.equals(localized, sequence.toString())) {
        return logMessage != null ? logMessage : localized;
      }
      return localized;
    }
    if (responseBody instanceof Map<?, ?> map) {
      return localizeMapResponseBody(map, localizedClient, logMessage, fallbackToLogChannel);
    }
    if (responseBody == null) {
      return localizedClient;
    }
    return responseBody;
  }

  private Object localizeMapResponseBody(
      Map<?, ?> responseBody,
      @Nullable String localizedClient,
      @Nullable String logMessage,
      boolean fallbackToLogChannel) {
    boolean requiresCopy = false;
    Map<Object, Object> localized = new LinkedHashMap<>(responseBody.size());
    for (Map.Entry<?, ?> entry : responseBody.entrySet()) {
      Object key = entry.getKey();
      Object value = entry.getValue();
      if ("message".equals(key) && value instanceof CharSequence sequence) {
        String original = sequence.toString();
        String translated = localizedClient != null ? localizedClient : localizeHttp(original);
        if (fallbackToLogChannel
            && Objects.equals(translated, original)
            && logMessage != null) {
          translated = logMessage;
        }
        if (!Objects.equals(original, translated)) {
          value = translated;
          requiresCopy = true;
        }
      }
      localized.put(key, value);
    }
    return requiresCopy ? localized : responseBody;
  }

  private String localizeHttp(@Nullable String message) {
    return properties.getHttp().resolve(message);
  }

  private String localizeLog(@Nullable String message) {
    BusinessExceptionLocalizationProperties.ChannelLocalization channel = properties.getLog();
    String localized = channel.resolve(message);
    if (localized == null && message != null) {
      LOG.warn("Не удалось локализовать сообщение лога '{}', используется исходный текст", message);
    }
    return localized;
  }

  private String localizeEvent(@Nullable String message) {
    BusinessExceptionLocalizationProperties.ChannelLocalization channel = properties.getEvent();
    String localized = channel.resolve(message);
    if (localized == null && message != null) {
      LOG.warn("Не удалось локализовать сообщение события '{}', используется исходный текст", message);
    }
    return localized;
  }

  private String fallbackToLogMessageIfNotTranslated(
      @Nullable String original,
      @Nullable String localized,
      @Nullable String logMessage) {
    if (original == null || localized == null) {
      return localized;
    }
    if (logMessage == null) {
      return localized;
    }
    if (Objects.equals(original, localized)) {
      return logMessage;
    }
    return localized;
  }

  private boolean shouldFallbackToLogChannel() {
    return Objects.equals(effectiveLanguage(properties.getHttp()), effectiveLanguage(properties.getLog()));
  }

  private static String effectiveLanguage(BusinessExceptionLocalizationProperties.ChannelLocalization channel) {
    if (channel == null) {
      return null;
    }
    String language = channel.getLanguage();
    return language != null ? language : channel.getDefaultLanguage();
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
