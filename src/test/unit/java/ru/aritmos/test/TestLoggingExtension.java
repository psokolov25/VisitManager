package ru.aritmos.test;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/**
 * JUnit-расширение, интегрирующее {@link TestLog} с жизненным циклом тестов.
 *
 * <p>Регистрируется в тестах для автоматического логирования начала и окончания
 * каждого тестового метода, а также фиксации пропусков и ошибок.
 */
public class TestLoggingExtension implements BeforeTestExecutionCallback, TestWatcher {

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        TestLog.stepStart(displayName(context));
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        TestLog.stepSuccess(displayName(context));
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        TestLog.stepFailure(displayName(context), cause);
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        TestLog.stepAborted(displayName(context), cause);
    }

    @Override
    public void testDisabled(ExtensionContext context, java.util.Optional<String> reason) {
        TestLog.stepSkipped(displayName(context), reason.orElse("no reason provided"));
    }

    /**
     * Формирует человекочитаемое имя теста для журналирования.
     *
     * @param context контекст выполняемого теста
     * @return строку вида {@code ClassName#methodName}
     */
    private String displayName(ExtensionContext context) {
        return context.getRequiredTestClass().getSimpleName() + "#" + context.getRequiredTestMethod().getName();
    }
}
