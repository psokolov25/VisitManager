package ru.aritmos.model;

import static ru.aritmos.test.LoggingAssertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutcomeTest {
  @DisplayName("Клонирование исхода возвращает независимую копию")
  @Test
  void cloneCreatesIndependentCopy() {
    Outcome original = new Outcome("1", "name");
    original.setCode(42L);

    Outcome clone = original.clone();

    assertNotSame(original, clone);
    assertEquals(original.getCode(), clone.getCode());
    assertEquals(original.getName(), clone.getName());

    original.setCode(100L);
    assertNotEquals(original.getCode(), clone.getCode());
  }
}
