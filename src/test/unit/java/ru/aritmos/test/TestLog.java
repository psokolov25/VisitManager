package ru.aritmos.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TestLog {

    private static final Logger STEP_LOGGER = LoggerFactory.getLogger("TEST_STEPS");
    private static final Logger CHECK_LOGGER = LoggerFactory.getLogger("TEST_CHECKS");

    private TestLog() {
    }

    static void stepStart(String displayName) {
        STEP_LOGGER.info("STEP START {}", displayName);
    }

    static void stepSuccess(String displayName) {
        STEP_LOGGER.info("STEP SUCCESS {}", displayName);
    }

    static void stepFailure(String displayName, Throwable throwable) {
        STEP_LOGGER.error("STEP FAILED {} - {}", displayName, throwable.getMessage(), throwable);
    }

    static void stepAborted(String displayName, Throwable throwable) {
        STEP_LOGGER.warn("STEP ABORTED {} - {}", displayName, throwable.getMessage(), throwable);
    }

    static void stepSkipped(String displayName, String reason) {
        STEP_LOGGER.warn("STEP SKIPPED {} - {}", displayName, reason);
    }

    static void checkStart(String location, String details) {
        CHECK_LOGGER.info("CHECK START [{}]{}", location, details);
    }

    static void checkSuccess(String location, String details) {
        CHECK_LOGGER.info("CHECK PASS [{}]{}", location, details);
    }

    static void checkFailure(String location, String details, AssertionError error) {
        CHECK_LOGGER.error("CHECK FAIL [{}]{}", location, details, error);
    }

    static void checkDetailsEvaluationFailure(Exception exception) {
        CHECK_LOGGER.warn("Failed to evaluate assertion log details: {}", exception.getMessage(), exception);
    }
}
