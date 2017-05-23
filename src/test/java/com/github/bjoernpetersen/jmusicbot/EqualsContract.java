package com.github.bjoernpetersen.jmusicbot;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

public interface EqualsContract<T> extends Testable<T> {

  int getEqualRelevantValueCount();

  @Nonnull
  T createNotEqualValue(int valueIndex);

  @Test
  default void valueEqualsItself() {
    T value = createValue();
    assertEquals(value, value);
  }

  @Test
  default void valueDoesNotEqualNull() {
    T value = createValue();
    assertFalse(value.equals(null));
  }

  @Test
  default void valueDoesNotEqualDifferentValue() {
    T value = createValue();

    int count = getEqualRelevantValueCount();
    Executable[] asserts = new Executable[count];
    for (int i = 0; i < count; i++) {
      final int index = i;
      asserts[index] = () -> valuesAreNotEqual(value, createNotEqualValue(index));
    }

    assertAll(asserts);
  }

  default void valuesAreNotEqual(@Nonnull T value, @Nonnull T differentValue) {
    assertNotEquals(value, differentValue);
    assertNotEquals(differentValue, value);
  }
}
