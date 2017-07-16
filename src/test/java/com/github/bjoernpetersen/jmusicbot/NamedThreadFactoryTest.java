package com.github.bjoernpetersen.jmusicbot;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NamedThreadFactoryTest {

  @Test
  void nullName() {
    assertThrows(NullPointerException.class, () -> new NamedThreadFactory((String) null, false));
  }

  @Test
  void nullSupplier() {
    assertThrows(NullPointerException.class, () -> new NamedThreadFactory((Supplier<String>) null, false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"testName", "otherName", "t", "123"})
  void nameContained(String name) {
    ThreadFactory factory = new NamedThreadFactory(name, false);
    Thread created = factory.newThread(() -> {
    });

    assertTrue(created.getName().contains(name));
  }

  @Test
  void isNotStarted() {
    assertFalse(new NamedThreadFactory("test", false).newThread(() -> {
    }).isAlive());
  }

  @Test
  void supplierIncreasingNumber() {
    AtomicInteger integer = new AtomicInteger(0);
    ThreadFactory factory = new NamedThreadFactory(() -> "testName" + integer.getAndIncrement(), false);

    Runnable runnable = () -> {
    };

    Stream<Executable> executables = Stream.of(0, 1, 2)
        .map(i -> {
          Thread thread = factory.newThread(runnable);
          return () -> assertTrue(thread.getName().contains("testName" + i));
        });

    assertAll(executables);
  }

}
