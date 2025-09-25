package ru.aritmos.exceptions;

import static ru.aritmos.test.LoggingAssertions.*;

import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import org.junit.jupiter.api.Test;

class BusinessExceptionLocalizationPropertiesTest {

    @Test
    void languageIsNormalizedByRemovingWrappingQuotes() {
        BusinessExceptionLocalizationProperties.ChannelLocalization localization =
                new BusinessExceptionLocalizationProperties.ChannelLocalization();
        localization.setLanguage("`Ru`");
        localization.setDefaultLanguage("'EN'");

        assertEquals("ru", localization.getLanguage());
        assertEquals("en", localization.getDefaultLanguage());
    }

    @Test
    void placeholderWithDefaultValueReturnsIt() {
        BusinessExceptionLocalizationProperties.ChannelLocalization localization =
                new BusinessExceptionLocalizationProperties.ChannelLocalization();

        localization.setLanguage("'${BUSINESS_EXCEPTION_HTTP_LANG:`ru`}'");

        assertEquals("ru", localization.getLanguage());
    }

    @Test
    void placeholderWithoutDefaultValueClearsLanguage() {
        BusinessExceptionLocalizationProperties.ChannelLocalization localization =
                new BusinessExceptionLocalizationProperties.ChannelLocalization();

        localization.setLanguage("'${BUSINESS_EXCEPTION_HTTP_LANG}'");

        assertNull(localization.getLanguage());
    }

    @Test
    void logFallbackEnabledWhenLanguagesMatchAfterNormalization() {
        BusinessExceptionLocalizationProperties.ChannelLocalization http =
                new BusinessExceptionLocalizationProperties.ChannelLocalization();
        http.setLanguage("`ru`");
        http.setDefaultLanguage("`ru`");

        BusinessExceptionLocalizationProperties.ChannelLocalization log =
                new BusinessExceptionLocalizationProperties.ChannelLocalization();
        log.setLanguage("ru");
        log.setDefaultLanguage("ru");

        BusinessExceptionLocalizationProperties properties = new BusinessExceptionLocalizationProperties();
        properties.setHttp(http);
        properties.setLog(log);

        BusinessExceptionLocalization localization = new BusinessExceptionLocalization(properties);

        BusinessExceptionLocalization.LocalizedMessages messages = localization.localize(
                "Service not found",
                "Service not found",
                "Услуга не найдена",
                "Услуга не найдена");

        assertEquals("Услуга не найдена", messages.clientMessage());
        assertEquals("Услуга не найдена", messages.responseBody());
    }


    @Test
    void resourceLocalizationProvidesRussianTranslation() {
        BusinessExceptionLocalizationProperties.ChannelLocalization http =
                new BusinessExceptionLocalizationProperties.ChannelLocalization();
        http.setLanguage("ru");
        http.setDefaultLanguage("ru");

        BusinessExceptionLocalizationProperties properties = new BusinessExceptionLocalizationProperties();
        properties.setHttp(http);

        BusinessExceptionLocalization localization = new BusinessExceptionLocalization(properties);

        BusinessExceptionLocalization.LocalizedMessages messages = localization.localize(
                "branch_not_found",
                "branch_not_found",
                "branch_not_found",
                "branch_not_found");

        assertEquals("Отделение не найдено", messages.clientMessage());
        assertEquals("Отделение не найдено", messages.responseBody());
    }

    @Test
    void resourceLocalizationReturnsEnglishTranslationForRussianKey() {
        BusinessExceptionLocalizationProperties.ChannelLocalization http =
                new BusinessExceptionLocalizationProperties.ChannelLocalization();
        http.setLanguage("en");
        http.setDefaultLanguage("en");

        BusinessExceptionLocalizationProperties properties = new BusinessExceptionLocalizationProperties();
        properties.setHttp(http);

        BusinessExceptionLocalization localization = new BusinessExceptionLocalization(properties);

        BusinessExceptionLocalization.LocalizedMessages messages = localization.localize(
                "delayed_return_not_completed",
                "delayed_return_not_completed",
                "delayed_return_not_completed",
                "delayed_return_not_completed");

        assertEquals("Delayed return has not yet been completed", messages.clientMessage());
        assertEquals("Delayed return has not yet been completed", messages.responseBody());
    }

    @Test
    void configurerReconfiguresLocalizationAfterUpdate() {
        BusinessException.resetLocalization();
        try {
            BusinessExceptionLocalizationProperties properties = new BusinessExceptionLocalizationProperties();
            BusinessExceptionLocalizationConfigurer configurer =
                    new BusinessExceptionLocalizationConfigurer(properties);

            applyLanguage(properties, "en");
            configurer.updateLocalization();

            HttpStatusException english =
                    assertThrows(
                            HttpStatusException.class,
                            () -> new BusinessException("branch_not_found", null, HttpStatus.BAD_REQUEST));
            assertEquals("Branch not found", english.getMessage());

            applyLanguage(properties, "ru");
            configurer.updateLocalization();

            HttpStatusException russian =
                    assertThrows(
                            HttpStatusException.class,
                            () -> new BusinessException("branch_not_found", null, HttpStatus.BAD_REQUEST));
            assertEquals("Отделение не найдено", russian.getMessage());
        } finally {
            BusinessException.resetLocalization();
        }
    }

    private static void applyLanguage(BusinessExceptionLocalizationProperties properties, String language) {
        properties.getHttp().setLanguage(language);
        properties.getHttp().setDefaultLanguage(language);
        properties.getLog().setLanguage(language);
        properties.getLog().setDefaultLanguage(language);
        properties.getEvent().setLanguage(language);
        properties.getEvent().setDefaultLanguage(language);
    }
}
