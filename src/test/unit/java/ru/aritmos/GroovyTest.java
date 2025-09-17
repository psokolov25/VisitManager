package ru.aritmos;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import ru.aritmos.test.LoggingAssertions;
import ru.aritmos.model.visit.Visit;

@Slf4j
// @MicronautTest
@SuppressWarnings({"unchecked", "unused"})
public class GroovyTest {

  // @Test
  public void testGroovy() throws IOException, URISyntaxException, InterruptedException {

    ClassLoader classLoader = getClass().getClassLoader();

    // Объект привязки скрипта со средой выполнения (для передачи значений переменных, свойств и т д
    // )
    // и для получения значений переменных и свойств после выполнения скрипта
    Binding binding = new Binding();
    // Объект выполнения скрипта Groovy
    GroovyShell shell = new GroovyShell(binding);
    // Текст скрипта на groovy, подгружается из файла (возможно так же и другое хранение скрипта)
    String scriptCode =
        Files.readString(
            Paths.get(Objects.requireNonNull(classLoader.getResource("test.groovy")).toURI()));
    // Обработка скрипта перед выполнением
    Script script = shell.parse(scriptCode);
    // Передача двух тестовых визитов
    binding.setVariable(
        "visits",
        new ArrayList<Visit>() {
          {
            add(
                Visit.builder()
                    .id("test1")
                    .createDateTime(ZonedDateTime.now())
                    .queueId("123")
                    .build());
            Thread.sleep(0);
            add(
                Visit.builder()
                    .id("test2")
                    .createDateTime(ZonedDateTime.now())
                    .servicePointId("123")
                    .build());
            Thread.sleep(10);
            add(
                Visit.builder()
                    .id("test2")
                    .createDateTime(ZonedDateTime.now())
                    .poolUserId("20")
                    .build());
          }
        });
    // Передача дополнительных параметров, в данном случае идентификаторы очереди, пула сотрудника и
    // пула
    // точки обслуживания
    // из которых нужно извлекать визиты
    binding.setVariable(
        "params",
        new HashMap<String, Object>() {
          {
            put("queueId", "123");
            put("userPoolId", "123");
            put("servicePointId", "123");
          }
        });
    // Запуск выполнения скрипта
    script.run();
    // Получение значений всех переменных скрипта
    var result = binding.getVariables();
    // Получение оптимального визита из двух, согласно алгоритму описанному в groovya crhbgnt
    Optional<Visit> optimalVisit = (Optional<Visit>) result.get("result");

    LoggingAssertions.assertTrue(optimalVisit.isPresent());
    LoggingAssertions.assertEquals("123", optimalVisit.get().getQueueId());
    log.info("Оптимальный визит:{}!", optimalVisit);
  }
}
