package ru.aritmos.exceptions;

import io.micronaut.context.annotation.ConfigurationProperties;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Настройки локализации сообщений {@link BusinessException}.
 */
@ConfigurationProperties("business-exception.localization")
public class BusinessExceptionLocalizationProperties {

  private ChannelLocalization http = ChannelLocalization.httpDefaults();
  private ChannelLocalization log = ChannelLocalization.logDefaults();
  private ChannelLocalization event = ChannelLocalization.eventDefaults();

  public ChannelLocalization getHttp() {
    return http;
  }

  public void setHttp(ChannelLocalization http) {
    this.http = http != null ? http : ChannelLocalization.httpDefaults();
  }

  public ChannelLocalization getLog() {
    return log;
  }

  public void setLog(ChannelLocalization log) {
    this.log = log != null ? log : ChannelLocalization.logDefaults();
  }

  public ChannelLocalization getEvent() {
    return event;
  }

  public void setEvent(ChannelLocalization event) {
    this.event = event != null ? event : ChannelLocalization.eventDefaults();
  }

  /**
   * Настройки отдельного канала сообщений.
   */
  public static final class ChannelLocalization {

    private String language;
    private String defaultLanguage;
    private Map<String, Map<String, String>> messages = new LinkedHashMap<>();

    public static ChannelLocalization httpDefaults() {
      ChannelLocalization localization = new ChannelLocalization();
      localization.setLanguage("en");
      localization.setDefaultLanguage("en");
      return localization;
    }

    public static ChannelLocalization logDefaults() {
      ChannelLocalization localization = new ChannelLocalization();
      localization.setLanguage("ru");
      localization.setDefaultLanguage("ru");
      return localization;
    }

    public static ChannelLocalization eventDefaults() {
      ChannelLocalization localization = new ChannelLocalization();
      localization.setLanguage("ru");
      localization.setDefaultLanguage("ru");
      return localization;
    }

    public String getLanguage() {
      return language;
    }

    public void setLanguage(String language) {
      this.language = normalizeLanguage(language);
    }

    public String getDefaultLanguage() {
      return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
      this.defaultLanguage = normalizeLanguage(defaultLanguage);
    }

    public Map<String, Map<String, String>> getMessages() {
      return messages;
    }

    public void setMessages(Map<String, Map<String, String>> messages) {
      if (messages == null) {
        this.messages = new LinkedHashMap<>();
        return;
      }
      Map<String, Map<String, String>> normalized = new LinkedHashMap<>();
      for (Map.Entry<String, Map<String, String>> entry : messages.entrySet()) {
        Map<String, String> translations = entry.getValue();
        if (translations == null || translations.isEmpty()) {
          normalized.put(entry.getKey(), Collections.emptyMap());
          continue;
        }
        Map<String, String> normalizedTranslations = new LinkedHashMap<>();
        for (Map.Entry<String, String> translation : translations.entrySet()) {
          String languageKey = normalizeLanguage(translation.getKey());
          if (languageKey != null) {
            normalizedTranslations.put(languageKey, translation.getValue());
          }
        }
        normalized.put(entry.getKey(), normalizedTranslations);
      }
      this.messages = normalized;
    }

    /**
     * Найти перевод сообщения с учётом заданных языков.
     *
     * @param fallback текст по умолчанию
     * @return локализованный текст или исходное значение, если перевод не найден
     */
    public String resolve(String fallback) {
      if (fallback == null) {
        return null;
      }
      String targetLanguage = language != null ? language : defaultLanguage;
      if (targetLanguage == null) {
        return fallback;
      }
      Map<String, String> translations = messages.getOrDefault(fallback, Collections.emptyMap());
      String translation = translations.get(targetLanguage);
      if (translation != null) {
        return translation;
      }
      if (defaultLanguage != null) {
        translation = translations.get(defaultLanguage);
        if (translation != null) {
          return translation;
        }
      }
      return fallback;
    }

    private static String normalizeLanguage(String language) {
      if (language == null) {
        return null;
      }
      String trimmed = language.trim();
      if (trimmed.isEmpty()) {
        return null;
      }
      String unquoted = trimmed;
      String candidate = stripWrappingQuotes(unquoted);
      while (!candidate.equals(unquoted)) {
        unquoted = candidate.trim();
        candidate = stripWrappingQuotes(unquoted);
      }
      if (unquoted.isEmpty()) {
        return null;
      }
      return unquoted.toLowerCase(Locale.ROOT);
    }

    private static String stripWrappingQuotes(String value) {
      if (value.length() < 2) {
        return value;
      }
      char first = value.charAt(0);
      char last = value.charAt(value.length() - 1);
      if (first == last && (first == '"' || first == '\'' || first == '`')) {
        return value.substring(1, value.length() - 1);
      }
      return value;
    }
  }
}
