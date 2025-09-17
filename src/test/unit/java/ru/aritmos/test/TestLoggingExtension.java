package ru.aritmos.test;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

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

    private String displayName(ExtensionContext context) {
        return context.getRequiredTestClass().getSimpleName() + "#" + context.getRequiredTestMethod().getName();
    }
}
