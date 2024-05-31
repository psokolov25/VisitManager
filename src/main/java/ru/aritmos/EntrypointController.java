package ru.aritmos;

import io.micronaut.http.annotation.*;

@Controller("/entrypoint")
public class EntrypointController {

    @Get(uri = "/", produces = "text/plain")
    public String index() {
        return "Example Response";
    }
}