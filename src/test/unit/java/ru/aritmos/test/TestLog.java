package ru.aritmos.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Утилита для унифицированного логирования шагов теста и проверок.
 *
 * <p>Класс инкапсулирует два именованных логгера: для шагов ({@code TEST_STEPS}) и проверок ({@code
 * TEST_CHECKS}). Сообщения используются расширением {@link TestLoggingExtension}, а также
 * вспомогательными утверждениями.
 */
final class TestLog {

  private static final Logger STEP_LOGGER = LoggerFactory.getLogger("TEST_STEPS");
  private static final Logger CHECK_LOGGER = LoggerFactory.getLogger("TEST_CHECKS");

  private TestLog() {}

  /**
   * Фиксирует начало выполнения шага теста.
   *
   * @param displayName человекочитаемое имя шага
   */
  static void stepStart(String displayName) {
    STEP_LOGGER.info("STEP START {}", displayName);
  }

  /**
   * Сообщает об успешном завершении шага теста.
   *
   * @param displayName человекочитаемое имя шага
   */
  static void stepSuccess(String displayName) {
    STEP_LOGGER.info("STEP SUCCESS {}", displayName);
  }

  /**
   * Регистрирует неуспешное выполнение шага.
   *
   * @param displayName человекочитаемое имя шага
   * @param throwable исключение, приведшее к падению шага
   */
  static void stepFailure(String displayName, Throwable throwable) {
    STEP_LOGGER.error("STEP FAILED {} - {}", displayName, throwable.getMessage(), throwable);
  }

  /**
   * Фиксирует пропуск шага из-за прерванного теста.
   *
   * @param displayName человекочитаемое имя шага
   * @param throwable причина прерывания
   */
  static void stepAborted(String displayName, Throwable throwable) {
    STEP_LOGGER.warn("STEP ABORTED {} - {}", displayName, throwable.getMessage(), throwable);
  }

  /**
   * Регистрирует пропуск шага по заданной причине.
   *
   * @param displayName человекочитаемое имя шага
   * @param reason текстовое объяснение пропуска
   */
  static void stepSkipped(String displayName, String reason) {
    STEP_LOGGER.warn("STEP SKIPPED {} - {}", displayName, reason);
  }

  /**
   * Выводит информацию о начале проверки.
   *
   * @param location местоположение вызова утверждения
   * @param details дополнительные сведения о проверяемых данных
   */
  static void checkStart(String location, String details) {
    CHECK_LOGGER.info("CHECK START [{}]{}", location, details);
  }

  /**
   * Сообщает об успешном прохождении проверки.
   *
   * @param location местоположение вызова утверждения
   * @param details дополнительные сведения о проверенных данных
   */
  static void checkSuccess(String location, String details) {
    CHECK_LOGGER.info("CHECK PASS [{}]{}", location, details);
  }

  /**
   * Фиксирует неуспешную проверку и логирует {@link AssertionError}.
   *
   * @param location местоположение вызова утверждения
   * @param details дополнительные сведения о проверенных данных
   * @param error исключение утверждения
   */
  static void checkFailure(String location, String details, AssertionError error) {
    CHECK_LOGGER.error("CHECK FAIL [{}]{}", location, details, error);
  }

  /**
   * Логирует ситуацию, когда не удалось сформировать детальное сообщение для проверки.
   *
   * @param exception исключение, возникшее при подготовке сообщения
   */
  static void checkDetailsEvaluationFailure(Exception exception) {
    CHECK_LOGGER.warn(
        "Failed to evaluate assertion log details: {}", exception.getMessage(), exception);
  }
}
