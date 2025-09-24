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
    private String sourceLanguage;
    private Map<String, Map<String, String>> messages = new LinkedHashMap<>();

    public static ChannelLocalization httpDefaults() {
      ChannelLocalization localization = new ChannelLocalization();
      localization.setLanguage("en");
      localization.setDefaultLanguage("en");
      localization.setSourceLanguage("en");
      return localization;
    }

    public static ChannelLocalization logDefaults() {
      ChannelLocalization localization = new ChannelLocalization();
      localization.setLanguage("ru");
      localization.setDefaultLanguage("ru");
      localization.setSourceLanguage("ru");
      return localization;
    }

    public static ChannelLocalization eventDefaults() {
      ChannelLocalization localization = new ChannelLocalization();
      localization.setLanguage("ru");
      localization.setDefaultLanguage("ru");
      localization.setSourceLanguage("ru");
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

    public String getSourceLanguage() {
      return sourceLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
      this.sourceLanguage = normalizeLanguage(sourceLanguage);
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
      return resolve(fallback, Collections.emptyMap());
    }

    public String resolve(String fallback, Map<String, String> baseMessages) {
      String actualFallback = fallback;
      if (actualFallback == null && baseMessages != null) {
        actualFallback =
            baseMessages.values().stream().filter(v -> v != null && !v.isBlank()).findFirst().orElse(null);
      }
      if (actualFallback == null) {
        return null;
      }

      String targetLanguage = language != null ? language : defaultLanguage;
      if (targetLanguage == null) {
        return actualFallback;
      }

      String translation = resolveFromDictionary(actualFallback, targetLanguage);
      if (translation != null) {
        return translation;
      }

      if (baseMessages != null) {
        String provided = baseMessages.get(targetLanguage);
        if (provided != null) {
          return provided;
        }
      }

      translation = resolveFromDictionary(baseMessages, targetLanguage);
      if (translation != null) {
        return translation;
      }

      if (defaultLanguage != null) {
        translation = resolveFromDictionary(actualFallback, defaultLanguage);
        if (translation != null) {
          return translation;
        }
        translation = resolveFromDictionary(baseMessages, defaultLanguage);
        if (translation != null) {
          return translation;
        }
        if (baseMessages != null) {
          String providedDefault = baseMessages.get(defaultLanguage);
          if (providedDefault != null) {
            return providedDefault;
          }
        }
      }

      return actualFallback;
    }

    private String resolveFromDictionary(String key, String languageKey) {
      if (key == null || languageKey == null) {
        return null;
      }
      Map<String, String> translations = messages.get(key);
      if (translations == null || translations.isEmpty()) {
        return null;
      }
      return translations.get(languageKey);
    }

    private String resolveFromDictionary(Map<String, String> baseMessages, String languageKey) {
      if (messages.isEmpty() || baseMessages == null || baseMessages.isEmpty() || languageKey == null) {
        return null;
      }
      for (Map.Entry<String, Map<String, String>> entry : messages.entrySet()) {
        Map<String, String> translations = entry.getValue();
        if (translations == null || translations.isEmpty()) {
          continue;
        }
        for (Map.Entry<String, String> base : baseMessages.entrySet()) {
          String baseLanguage = base.getKey();
          String baseText = base.getValue();
          if (baseLanguage == null || baseText == null) {
            continue;
          }
          String translationForBase = translations.get(baseLanguage);
          if (translationForBase != null && translationForBase.equals(baseText)) {
            String translation = translations.get(languageKey);
            if (translation != null) {
              return translation;
            }
          }
        }
      }
      return null;
    }

    private static String normalizeLanguage(String language) {
      if (language == null || language.isBlank()) {
        return null;
      }
      return language.trim().toLowerCase(Locale.ROOT);
    }
  }
}
