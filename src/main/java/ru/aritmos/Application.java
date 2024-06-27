package ru.aritmos;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;

@OpenAPIDefinition(
        info = @Info(
                title = "VisitManagement",
                version = "0.1"
        ),
        tags = {
        @Tag(name = "Зона обслуживания", description = "Рест АПИ отвечающие вызов и обслуживание визита"),
        @Tag(name = "Информация об отделении", description = "Рест АПИ отвечающие за отображение состояния отделения"),
        @Tag(name = "Зона ожидания", description = "Рест АПИ отвечающие за создание визита")


}
)
public class Application {


    @ContextConfigurer
    public static class Configurer implements ApplicationContextConfigurer {
        @Override
        public void configure(@NonNull ApplicationContextBuilder builder) {


            builder.defaultEnvironments("dev");
        }

    }

    @PostConstruct
    public void getConfiguration() {

    }

    public static void main(String[] args) {



        Micronaut.run(Application.class, args);


    }
}