package ru.aritmos.exceptions;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.Test;

class BusinessExceptionLocalizationPropertiesTest {

    @Test
    void языкНормализуетсяСУдалениемОбрамляющихКавычек() {
        BusinessExceptionLocalizationProperties.ChannelLocalization localization =
                new BusinessExceptionLocalizationProperties.ChannelLocalization();
        localization.setLanguage("`Ru`");
        localization.setDefaultLanguage("'EN'");

        assertEquals("ru", localization.getLanguage());
        assertEquals("en", localization.getDefaultLanguage());
    }

    @Test
    void падениеКЛогуВключаетсяКогдаЯзыкиСовпадаютПослеНормализации() {
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
    void локализацияПоРесурсамПодставляетРусскийПеревод() {
        BusinessExceptionLocalizationProperties.ChannelLocalization http =
                new BusinessExceptionLocalizationProperties.ChannelLocalization();
        http.setLanguage("ru");
        http.setDefaultLanguage("ru");

        BusinessExceptionLocalizationProperties properties = new BusinessExceptionLocalizationProperties();
        properties.setHttp(http);

        BusinessExceptionLocalization localization = new BusinessExceptionLocalization(properties);

        BusinessExceptionLocalization.LocalizedMessages messages = localization.localize(
                "Branch not found",
                "Branch not found",
                "Отделение не найдено",
                "Отделение не найдено");

        assertEquals("Отделение не найдено", messages.clientMessage());
        assertEquals("Отделение не найдено", messages.responseBody());
    }

    @Test
    void локализацияПоРесурсамВозвращаетАнглийскийПереводДляРусскогоКлюча() {
        BusinessExceptionLocalizationProperties.ChannelLocalization http =
                new BusinessExceptionLocalizationProperties.ChannelLocalization();
        http.setLanguage("en");
        http.setDefaultLanguage("en");

        BusinessExceptionLocalizationProperties properties = new BusinessExceptionLocalizationProperties();
        properties.setHttp(http);

        BusinessExceptionLocalization localization = new BusinessExceptionLocalization(properties);

        BusinessExceptionLocalization.LocalizedMessages messages = localization.localize(
                "Задержка возвращения еще не завершена",
                "Задержка возвращения еще не завершена",
                "Delayed return has not yet been completed",
                "Delayed return has not yet been completed");

        assertEquals("Delayed return has not yet been completed", messages.clientMessage());
        assertEquals("Delayed return has not yet been completed", messages.responseBody());
    }
}
