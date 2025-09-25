package ru.aritmos.exceptions;

import io.micronaut.context.annotation.ConfigurationProperties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Настройки локализации сообщений {@link BusinessException}.
 */
@ConfigurationProperties("business-exception.localization")
public class BusinessExceptionLocalizationProperties {

  private static final Logger LOG =
      LoggerFactory.getLogger(BusinessExceptionLocalizationProperties.class);

  private static final Map<String, ChannelLocalization> CONFIG_DEFAULTS = loadConfiguredDefaults();

  private ChannelLocalization http = defaultChannel("http", ChannelLocalization.httpDefaults());
  private ChannelLocalization log = defaultChannel("log", ChannelLocalization.logDefaults());
  private ChannelLocalization event = defaultChannel("event", ChannelLocalization.eventDefaults());

  private static ChannelLocalization defaultChannel(String key, ChannelLocalization fallback) {
    ChannelLocalization configured = CONFIG_DEFAULTS.get(key);
    if (configured == null) {
      return fallback;
    }
    ChannelLocalization channel = new ChannelLocalization();
    channel.setLanguage(configured.getLanguage());
    channel.setDefaultLanguage(configured.getDefaultLanguage());
    channel.setMessages(configured.getMessages());
    channel.setResources(configured.getResources());
    return channel;
  }

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

  private static Map<String, ChannelLocalization> loadConfiguredDefaults() {
    Map<String, ChannelLocalization> defaults = new ConcurrentHashMap<>();
    Map<String, String> languageSettings = readLanguagesFromApplicationYaml();

    ChannelLocalization http = ChannelLocalization.httpDefaults();
    applyLanguageOverride(http, languageSettings.get("http.language"));
    applyDefaultLanguageOverride(http, languageSettings.get("http.default-language"));
    defaults.put("http", http);

    ChannelLocalization log = ChannelLocalization.logDefaults();
    applyLanguageOverride(log, languageSettings.get("log.language"));
    applyDefaultLanguageOverride(log, languageSettings.get("log.default-language"));
    defaults.put("log", log);

    ChannelLocalization event = ChannelLocalization.eventDefaults();
    applyLanguageOverride(event, languageSettings.get("event.language"));
    applyDefaultLanguageOverride(event, languageSettings.get("event.default-language"));
    defaults.put("event", event);

    return defaults;
  }

  private static Map<String, String> readLanguagesFromApplicationYaml() {
    Map<String, String> values = new ConcurrentHashMap<>();
    try (InputStream inputStream =
            BusinessExceptionLocalizationProperties.class
                .getClassLoader()
                .getResourceAsStream("application.yml");
        BufferedReader reader =
            inputStream != null
                ? new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                : null) {
      if (reader == null) {
        return values;
      }
      int businessExceptionIndent = -1;
      int localizationIndent = -1;
      int channelIndent = -1;
      String currentChannel = null;
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank()) {
          continue;
        }
        String trimmed = line.trim();
        if (trimmed.startsWith("#")) {
          continue;
        }
        int indent = countLeadingSpaces(line);
        if (trimmed.endsWith(":")) {
          String section = trimmed.substring(0, trimmed.length() - 1).trim();
          if (indent == 0 && "business-exception".equals(section)) {
            businessExceptionIndent = indent;
            localizationIndent = -1;
            channelIndent = -1;
            currentChannel = null;
            continue;
          }
          if (businessExceptionIndent >= 0
              && indent > businessExceptionIndent
              && "localization".equals(section)) {
            localizationIndent = indent;
            channelIndent = -1;
            currentChannel = null;
            continue;
          }
          if (localizationIndent >= 0 && indent > localizationIndent) {
            currentChannel = section;
            channelIndent = indent;
            continue;
          }
          if (indent <= localizationIndent) {
            currentChannel = null;
            channelIndent = -1;
          }
          continue;
        }
        if (localizationIndent < 0 || currentChannel == null) {
          continue;
        }
        if (indent <= channelIndent) {
          currentChannel = null;
          channelIndent = -1;
          continue;
        }
        int separator = trimmed.indexOf(':');
        if (separator < 0) {
          continue;
        }
        String key = trimmed.substring(0, separator).trim();
        if (!"language".equals(key) && !"default-language".equals(key)) {
          continue;
        }
        String value = trimmed.substring(separator + 1).trim();
        if (!value.isEmpty()) {
          values.put(currentChannel + "." + key, value);
        }
      }
    } catch (IOException ex) {
      LOG.warn("Не удалось прочитать языковые настройки BusinessException из application.yml", ex);
    }
    return values;
  }

  private static void applyLanguageOverride(ChannelLocalization channel, String language) {
    if (language != null) {
      channel.setLanguage(language);
    }
  }

  private static void applyDefaultLanguageOverride(ChannelLocalization channel, String language) {
    if (language != null) {
      channel.setDefaultLanguage(language);
    }
  }

  private static int countLeadingSpaces(String line) {
    int count = 0;
    while (count < line.length() && line.charAt(count) == ' ') {
      count++;
    }
    return count;
  }

  /**
   * Настройки отдельного канала сообщений.
   */
  public static final class ChannelLocalization {

    private static final String DEFAULT_RESOURCE_BASE_NAME = "business-exception/messages";

    private String language;
    private String defaultLanguage;
    private Map<String, Map<String, String>> messages = new LinkedHashMap<>();
    private List<String> resources = new ArrayList<>(List.of(DEFAULT_RESOURCE_BASE_NAME));

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

    public List<String> getResources() {
      return resources;
    }

    public void setResources(List<String> resources) {
      if (resources == null || resources.isEmpty()) {
        this.resources = new ArrayList<>(List.of(DEFAULT_RESOURCE_BASE_NAME));
        return;
      }
      List<String> normalized = new ArrayList<>();
      for (String resource : resources) {
        if (resource == null) {
          continue;
        }
        String trimmed = resource.trim();
        if (!trimmed.isEmpty()) {
          normalized.add(trimmed);
        }
      }
      if (normalized.isEmpty()) {
        this.resources = new ArrayList<>(List.of(DEFAULT_RESOURCE_BASE_NAME));
        return;
      }
      this.resources = normalized;
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
      translation = resolveFromResources(fallback, targetLanguage);
      if (translation != null) {
        return translation;
      }
      if (defaultLanguage != null) {
        translation = translations.get(defaultLanguage);
        if (translation != null) {
          return translation;
        }
        translation = resolveFromResources(fallback, defaultLanguage);
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
      while (true) {
        String candidate = stripWrappingQuotes(unquoted);
        if (candidate.equals(unquoted)) {
          break;
        }
        unquoted = candidate.trim();
        if (unquoted.isEmpty()) {
          return null;
        }
      }
      String resolved = resolvePlaceholderIfPresent(unquoted);
      if (resolved == null) {
        return null;
      }
      String sanitized = resolved.trim();
      if (sanitized.isEmpty()) {
        return null;
      }
      return sanitized.toLowerCase(Locale.ROOT);
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

    private static String resolvePlaceholderIfPresent(String value) {
      if (!value.startsWith("${") || !value.endsWith("}")) {
        return value;
      }
      String content = value.substring(2, value.length() - 1).trim();
      if (content.isEmpty()) {
        return null;
      }
      int separatorIndex = findPlaceholderSeparator(content);
      if (separatorIndex < 0) {
        return null;
      }
      String defaultCandidate = content.substring(separatorIndex + 1).trim();
      if (defaultCandidate.isEmpty()) {
        return null;
      }
      String unwrappedDefault = stripWrappingQuotes(defaultCandidate).trim();
      if (unwrappedDefault.isEmpty()) {
        return null;
      }
      if (unwrappedDefault.startsWith("${") && unwrappedDefault.endsWith("}")) {
        return resolvePlaceholderIfPresent(unwrappedDefault);
      }
      return unwrappedDefault;
    }

    private static int findPlaceholderSeparator(String content) {
      int depth = 0;
      for (int i = 0; i < content.length(); i++) {
        char ch = content.charAt(i);
        if (ch == '{') {
          depth++;
        } else if (ch == '}') {
          depth = Math.max(0, depth - 1);
        } else if (ch == ':' && depth == 0) {
          return i;
        }
      }
      return -1;
    }

    private String resolveFromResources(String messageKey, String languageCode) {
      if (resources.isEmpty()) {
        return null;
      }
      Locale locale = toLocale(languageCode);
      if (locale == null) {
        return null;
      }
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      for (String baseName : resources) {
        if (baseName == null || baseName.isBlank()) {
          continue;
        }
        try {
          ResourceBundle bundle =
              ResourceBundle.getBundle(
                  baseName,
                  locale,
                  classLoader,
                  ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
          if (bundle.containsKey(messageKey)) {
            String translation = bundle.getString(messageKey);
            if (translation != null) {
              return translation;
            }
          }
        } catch (MissingResourceException ignored) {
          // ресурс не найден — пробуем следующий
        }
      }
      return null;
    }

    private static Locale toLocale(String languageCode) {
      if (languageCode == null) {
        return null;
      }
      String normalized = languageCode.replace('_', '-');
      if (normalized.isBlank()) {
        return null;
      }
      Locale locale = Locale.forLanguageTag(normalized);
      return Objects.equals(locale.toLanguageTag(), "") ? null : locale;

    }
  }
}
