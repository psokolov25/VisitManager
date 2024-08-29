package ru.aritmos.keycloack.service;

import io.micronaut.core.async.annotation.SingleResult;
import io.micronaut.http.annotation.*;
import io.micronaut.http.client.annotation.Client;
import org.reactivestreams.Publisher;
import ru.aritmos.keycloack.model.KeyCloackUser;

import java.util.HashMap;
import java.util.Map;


@Client("http://192.168.8.45:9090")
public interface KeyCloackClient {
    @SingleResult
    @Post(value = "/realms/Aritmos/protocol/openid-connect/token/introspect",produces = "application/x-www-form-urlencoded")
    Publisher<HashMap<String,Object>> get(@Body Map<String, Object> attrs, @Header String authorization);
    @SingleResult
    @Post(value = "/realms/{realm}/protocol/openid-connect/userinfo",produces = "application/json")
    Publisher<KeyCloackUser> getUserInfo(@PathVariable String realm, @Body Map<String,Object> attrs, @Header String authorization);
    @SingleResult
    @Get(value = "/admin/realms/{realm}/users/{userId}",produces = "application/json")
    Publisher<HashMap<String,Object>> getUserInfo(@PathVariable String realm,  @PathVariable String userId, @Header String authorization);
}
