package ru.aritmos;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.ApplicationContextConfigurer;
import io.micronaut.context.annotation.ContextConfigurer;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.annotation.PostConstruct;

@OpenAPIDefinition(
        info = @Info(
                title = "VisitManagement",
                version = "0.2"

        ),servers = { @Server(url = "http://192.168.8.45:8080"),
        @Server(url = "http://localhost:8080") }




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