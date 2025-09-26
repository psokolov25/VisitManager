package ru.aritmos.test;

import java.util.Arrays;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;

/**
 * Обертки над стандартными {@link org.junit.jupiter.api.Assertions}, добавляющие логирование шагов
 * и проверок.
 *
 * <p>Класс помогает получать подробные сообщения о ходе выполнения теста даже в случае падения
 * утверждения. Все методы повторяют сигнатуры JUnit, поэтому могут использоваться как прямой
 * заменитель статических методов {@code Assertions}.
 */
public final class LoggingAssertions {

  private static final StackWalker STACK_WALKER =
      StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

  private LoggingAssertions() {}

  public static void assertEquals(Object expected, Object actual) {
    String base = formatExpectedActual(expected, actual);
    runCheck("assertEquals", () -> base, null, () -> Assertions.assertEquals(expected, actual));
  }

  public static void assertEquals(Object expected, Object actual, String message) {
    String base = joinDetails(formatExpectedActual(expected, actual), formatMessage(message));
    runCheck(
        "assertEquals", () -> base, null, () -> Assertions.assertEquals(expected, actual, message));
  }

  public static void assertEquals(
      Object expected, Object actual, Supplier<String> messageSupplier) {
    String base = formatExpectedActual(expected, actual);
    runCheck(
        "assertEquals",
        () -> joinDetails(base, formatLazyMessage()),
        () -> joinDetails(base, formatMessage(safeMessage(messageSupplier))),
        () -> Assertions.assertEquals(expected, actual, messageSupplier));
  }

  public static void assertTrue(boolean condition) {
    runCheck(
        "assertTrue", () -> "condition=" + condition, null, () -> Assertions.assertTrue(condition));
  }

  public static void assertTrue(boolean condition, String message) {
    runCheck(
        "assertTrue",
        () -> joinDetails("condition=" + condition, formatMessage(message)),
        null,
        () -> Assertions.assertTrue(condition, message));
  }

  public static void assertTrue(boolean condition, Supplier<String> messageSupplier) {
    runCheck(
        "assertTrue",
        () -> joinDetails("condition=" + condition, formatLazyMessage()),
        () -> joinDetails("condition=" + condition, formatMessage(safeMessage(messageSupplier))),
        () -> Assertions.assertTrue(condition, messageSupplier));
  }

  public static void assertTrue(BooleanSupplier condition) {
    runCheck("assertTrue", () -> "conditionSupplier", null, () -> Assertions.assertTrue(condition));
  }

  public static void assertTrue(BooleanSupplier condition, String message) {
    runCheck(
        "assertTrue",
        () -> joinDetails("conditionSupplier", formatMessage(message)),
        null,
        () -> Assertions.assertTrue(condition, message));
  }

  public static void assertTrue(BooleanSupplier condition, Supplier<String> messageSupplier) {
    runCheck(
        "assertTrue",
        () -> joinDetails("conditionSupplier", formatLazyMessage()),
        () -> joinDetails("conditionSupplier", formatMessage(safeMessage(messageSupplier))),
        () -> Assertions.assertTrue(condition, messageSupplier));
  }

  public static void assertFalse(boolean condition) {
    runCheck(
        "assertFalse",
        () -> "condition=" + condition,
        null,
        () -> Assertions.assertFalse(condition));
  }

  public static void assertFalse(boolean condition, String message) {
    runCheck(
        "assertFalse",
        () -> joinDetails("condition=" + condition, formatMessage(message)),
        null,
        () -> Assertions.assertFalse(condition, message));
  }

  public static void assertFalse(boolean condition, Supplier<String> messageSupplier) {
    runCheck(
        "assertFalse",
        () -> joinDetails("condition=" + condition, formatLazyMessage()),
        () -> joinDetails("condition=" + condition, formatMessage(safeMessage(messageSupplier))),
        () -> Assertions.assertFalse(condition, messageSupplier));
  }

  public static void assertFalse(BooleanSupplier condition) {
    runCheck(
        "assertFalse", () -> "conditionSupplier", null, () -> Assertions.assertFalse(condition));
  }

  public static void assertFalse(BooleanSupplier condition, String message) {
    runCheck(
        "assertFalse",
        () -> joinDetails("conditionSupplier", formatMessage(message)),
        null,
        () -> Assertions.assertFalse(condition, message));
  }

  public static void assertFalse(BooleanSupplier condition, Supplier<String> messageSupplier) {
    runCheck(
        "assertFalse",
        () -> joinDetails("conditionSupplier", formatLazyMessage()),
        () -> joinDetails("conditionSupplier", formatMessage(safeMessage(messageSupplier))),
        () -> Assertions.assertFalse(condition, messageSupplier));
  }

  public static void assertSame(Object expected, Object actual) {
    String base = formatExpectedActual(expected, actual);
    runCheck("assertSame", () -> base, null, () -> Assertions.assertSame(expected, actual));
  }

  public static void assertSame(Object expected, Object actual, String message) {
    String base = joinDetails(formatExpectedActual(expected, actual), formatMessage(message));
    runCheck(
        "assertSame", () -> base, null, () -> Assertions.assertSame(expected, actual, message));
  }

  public static void assertSame(Object expected, Object actual, Supplier<String> messageSupplier) {
    String base = formatExpectedActual(expected, actual);
    runCheck(
        "assertSame",
        () -> joinDetails(base, formatLazyMessage()),
        () -> joinDetails(base, formatMessage(safeMessage(messageSupplier))),
        () -> Assertions.assertSame(expected, actual, messageSupplier));
  }

  public static void assertNotSame(Object unexpected, Object actual) {
    String base = formatExpectedActual(unexpected, actual);
    runCheck("assertNotSame", () -> base, null, () -> Assertions.assertNotSame(unexpected, actual));
  }

  public static void assertNotEquals(Object unexpected, Object actual) {
    String base = formatExpectedActual(unexpected, actual);
    runCheck(
        "assertNotEquals", () -> base, null, () -> Assertions.assertNotEquals(unexpected, actual));
  }

  public static void assertNotEquals(Object unexpected, Object actual, String message) {
    String base = joinDetails(formatExpectedActual(unexpected, actual), formatMessage(message));
    runCheck(
        "assertNotEquals",
        () -> base,
        null,
        () -> Assertions.assertNotEquals(unexpected, actual, message));
  }

  public static void assertNotNull(Object actual) {
    runCheck(
        "assertNotNull",
        () -> "value=" + formatValue(actual),
        null,
        () -> Assertions.assertNotNull(actual));
  }

  public static void assertNotNull(Object actual, String message) {
    runCheck(
        "assertNotNull",
        () -> joinDetails("value=" + formatValue(actual), formatMessage(message)),
        null,
        () -> Assertions.assertNotNull(actual, message));
  }

  public static void assertNotNull(Object actual, Supplier<String> messageSupplier) {
    runCheck(
        "assertNotNull",
        () -> joinDetails("value=" + formatValue(actual), formatLazyMessage()),
        () ->
            joinDetails(
                "value=" + formatValue(actual), formatMessage(safeMessage(messageSupplier))),
        () -> Assertions.assertNotNull(actual, messageSupplier));
  }

  public static void assertNull(Object actual) {
    runCheck(
        "assertNull",
        () -> "value=" + formatValue(actual),
        null,
        () -> Assertions.assertNull(actual));
  }

  public static void assertArrayEquals(Object[] expected, Object[] actual) {
    String base = formatExpectedActual(expected, actual);
    runCheck(
        "assertArrayEquals",
        () -> base,
        null,
        () -> Assertions.assertArrayEquals(expected, actual));
  }

  public static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable) {
    String base = "expected=" + expectedType.getName();
    return runCheckWithResult(
        "assertThrows",
        () -> base,
        null,
        () -> Assertions.assertThrows(expectedType, executable),
        thrown -> "actual=" + thrown.getClass().getName());
  }

  public static <T extends Throwable> T assertThrows(
      Class<T> expectedType, Executable executable, String message) {
    String base = joinDetails("expected=" + expectedType.getName(), formatMessage(message));
    return runCheckWithResult(
        "assertThrows",
        () -> base,
        null,
        () -> Assertions.assertThrows(expectedType, executable, message),
        thrown -> "actual=" + thrown.getClass().getName());
  }

  public static <T extends Throwable> T assertThrows(
      Class<T> expectedType, Executable executable, Supplier<String> messageSupplier) {
    String expected = "expected=" + expectedType.getName();
    return runCheckWithResult(
        "assertThrows",
        () -> joinDetails(expected, formatLazyMessage()),
        () -> joinDetails(expected, formatMessage(safeMessage(messageSupplier))),
        () -> Assertions.assertThrows(expectedType, executable, messageSupplier),
        thrown -> "actual=" + thrown.getClass().getName());
  }

  public static void assertDoesNotThrow(Executable executable) {
    runCheck(
        "assertDoesNotThrow",
        () -> "executable=" + describeExecutable(executable),
        null,
        () -> Assertions.assertDoesNotThrow(executable));
  }

  public static void assertDoesNotThrow(Executable executable, String message) {
    runCheck(
        "assertDoesNotThrow",
        () -> joinDetails("executable=" + describeExecutable(executable), formatMessage(message)),
        null,
        () -> Assertions.assertDoesNotThrow(executable, message));
  }

  public static void assertDoesNotThrow(Executable executable, Supplier<String> messageSupplier) {
    runCheck(
        "assertDoesNotThrow",
        () -> joinDetails("executable=" + describeExecutable(executable), formatLazyMessage()),
        () ->
            joinDetails(
                "executable=" + describeExecutable(executable),
                formatMessage(safeMessage(messageSupplier))),
        () -> Assertions.assertDoesNotThrow(executable, messageSupplier));
  }

  public static <T> T assertDoesNotThrow(ThrowingSupplier<T> supplier) {
    return runCheckWithResult(
        "assertDoesNotThrow",
        () -> "supplier=" + describeExecutable(supplier),
        null,
        () -> Assertions.assertDoesNotThrow(supplier),
        result -> result == null ? "result=null" : "result=" + formatValue(result));
  }

  public static <T> T assertDoesNotThrow(ThrowingSupplier<T> supplier, String message) {
    return runCheckWithResult(
        "assertDoesNotThrow",
        () -> joinDetails("supplier=" + describeExecutable(supplier), formatMessage(message)),
        null,
        () -> Assertions.assertDoesNotThrow(supplier, message),
        result -> result == null ? "result=null" : "result=" + formatValue(result));
  }

  public static <T> T assertDoesNotThrow(
      ThrowingSupplier<T> supplier, Supplier<String> messageSupplier) {
    return runCheckWithResult(
        "assertDoesNotThrow",
        () -> joinDetails("supplier=" + describeExecutable(supplier), formatLazyMessage()),
        () ->
            joinDetails(
                "supplier=" + describeExecutable(supplier),
                formatMessage(safeMessage(messageSupplier))),
        () -> Assertions.assertDoesNotThrow(supplier, messageSupplier),
        result -> result == null ? "result=null" : "result=" + formatValue(result));
  }

  /**
   * Выполняет проверку без возвращаемого значения и фиксирует подробности в логах тестов.
   *
   * @param assertionName название утверждения (используется в сообщениях лога)
   * @param startDetailsSupplier ленивый источник описания входных данных
   * @param failureDetailsSupplier ленивый источник деталей о провале
   * @param assertion действие, выполняющее собственно проверку
   */
  private static void runCheck(
      String assertionName,
      Supplier<String> startDetailsSupplier,
      Supplier<String> failureDetailsSupplier,
      Runnable assertion) {
    String location = callerLocation();
    String startDetails = safeGet(startDetailsSupplier);
    TestLog.checkStart(location, formatDetails(assertionName, startDetails));
    try {
      assertion.run();
      TestLog.checkSuccess(location, formatDetails(assertionName, null));
    } catch (AssertionError error) {
      String failureDetails =
          failureDetailsSupplier != null ? safeGet(failureDetailsSupplier) : startDetails;
      TestLog.checkFailure(location, formatDetails(assertionName, failureDetails), error);
      throw error;
    }
  }

  /**
   * Выполняет проверку с возвращаемым значением и формирует сообщения об исходных и итоговых данных
   * проверки.
   *
   * @param assertionName название утверждения (используется в сообщениях лога)
   * @param startDetailsSupplier ленивый источник описания входных данных
   * @param failureDetailsSupplier ленивый источник деталей о провале
   * @param assertionSupplier действие, выполняющее проверку и возвращающее результат
   * @param successDetailsSupplier функция подготовки сообщения об успешном результате
   * @param <R> тип возвращаемого значения
   * @return результат выполнения {@code assertionSupplier}
   */
  private static <R> R runCheckWithResult(
      String assertionName,
      Supplier<String> startDetailsSupplier,
      Supplier<String> failureDetailsSupplier,
      Supplier<R> assertionSupplier,
      Function<R, String> successDetailsSupplier) {
    String location = callerLocation();
    String startDetails = safeGet(startDetailsSupplier);
    TestLog.checkStart(location, formatDetails(assertionName, startDetails));
    try {
      R result = assertionSupplier.get();
      String successDetails =
          successDetailsSupplier != null ? safeApply(successDetailsSupplier, result) : null;
      TestLog.checkSuccess(location, formatDetails(assertionName, successDetails));
      return result;
    } catch (AssertionError error) {
      String failureDetails =
          failureDetailsSupplier != null ? safeGet(failureDetailsSupplier) : startDetails;
      TestLog.checkFailure(location, formatDetails(assertionName, failureDetails), error);
      throw error;
    }
  }

  /**
   * Определяет исходную точку вызова утверждения для последующего вывода в журнале проверок.
   *
   * @return строку в формате {@code класс#метод:строка}
   */
  private static String callerLocation() {
    return STACK_WALKER.walk(
        stream ->
            stream
                .dropWhile(frame -> frame.getClassName().equals(LoggingAssertions.class.getName()))
                .findFirst()
                .map(
                    frame ->
                        frame.getClassName()
                            + "#"
                            + frame.getMethodName()
                            + ":"
                            + frame.getLineNumber())
                .orElse("unknown"));
  }

  private static String safeGet(Supplier<String> supplier) {
    if (supplier == null) {
      return "";
    }
    try {
      String value = supplier.get();
      return value == null ? "" : value;
    } catch (Exception exception) {
      TestLog.checkDetailsEvaluationFailure(exception);
      return "<details evaluation failed>";
    }
  }

  private static <T> String safeApply(Function<T, String> function, T value) {
    if (function == null) {
      return "";
    }
    try {
      String result = function.apply(value);
      return result == null ? "" : result;
    } catch (Exception exception) {
      TestLog.checkDetailsEvaluationFailure(exception);
      return "<details evaluation failed>";
    }
  }

  private static String formatDetails(String assertionName, String details) {
    if (details == null || details.isBlank()) {
      return " - " + assertionName;
    }
    return " - " + assertionName + " :: " + details;
  }

  private static String formatExpectedActual(Object expected, Object actual) {
    return "expected=" + formatValue(expected) + ", actual=" + formatValue(actual);
  }

  private static String formatValue(Object value) {
    if (value == null) {
      return "null";
    }
    Class<?> type = value.getClass();
    if (!type.isArray()) {
      return String.valueOf(value);
    }
    if (value instanceof Object[] objects) {
      return Arrays.deepToString(objects);
    }
    if (value instanceof int[] ints) {
      return Arrays.toString(ints);
    }
    if (value instanceof long[] longs) {
      return Arrays.toString(longs);
    }
    if (value instanceof double[] doubles) {
      return Arrays.toString(doubles);
    }
    if (value instanceof float[] floats) {
      return Arrays.toString(floats);
    }
    if (value instanceof short[] shorts) {
      return Arrays.toString(shorts);
    }
    if (value instanceof byte[] bytes) {
      return Arrays.toString(bytes);
    }
    if (value instanceof char[] chars) {
      return Arrays.toString(chars);
    }
    if (value instanceof boolean[] booleans) {
      return Arrays.toString(booleans);
    }
    return String.valueOf(value);
  }

  private static String joinDetails(String base, String extra) {
    if (isBlank(base)) {
      return extra == null ? "" : extra;
    }
    if (isBlank(extra)) {
      return base;
    }
    return base + ", " + extra;
  }

  private static String formatMessage(String message) {
    if (isBlank(message)) {
      return "";
    }
    return "message=" + message;
  }

  private static String formatLazyMessage() {
    return "message=<lazy>";
  }

  private static String safeMessage(Supplier<String> messageSupplier) {
    if (messageSupplier == null) {
      return null;
    }
    try {
      return messageSupplier.get();
    } catch (Exception exception) {
      TestLog.checkDetailsEvaluationFailure(exception);
      return "<message evaluation failed>";
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private static String describeExecutable(Object executable) {
    if (executable == null) {
      return "null";
    }
    return executable.getClass().getName();
  }
}
