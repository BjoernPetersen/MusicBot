package com.github.bjoernpetersen.jmusicbot.playback;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WeakQueueChangeListenerTest {

  private static List<BiConsumer<QueueChangeListener, QueueEntry>> getInterfaceMethods() {
    return Arrays.asList(QueueChangeListener::onAdd, QueueChangeListener::onRemove);
  }

  @ParameterizedTest
  @MethodSource(value = "getInterfaceMethods")
  void methodIsCalled(BiConsumer<QueueChangeListener, QueueEntry> method) {
    AtomicReference<QueueEntry> called = new AtomicReference<>();
    QueueChangeListener listener = new QueueChangeListener() {
      @Override
      public void onAdd(@Nonnull QueueEntry song) {
        called.set(song);
      }

      @Override
      public void onRemove(@Nonnull QueueEntry song) {
        called.set(song);
      }

      @Override
      public void onMove(@Nonnull QueueEntry entry, int fromIndex, int toIndex) {
        // TODO test this one
      }
    };
    QueueChangeListener weak = new WeakQueueChangeListener(listener);

    assertNull(called.get());
    QueueEntry song = mock(QueueEntry.class);
    method.accept(weak, song);
    assertSame(song, called.get());
  }

  @Test
  void isWeak() throws InterruptedException {
    QueueChangeListener listener = new DummyQueueChangeListener();
    WeakReference<QueueChangeListener> weakListener = new WeakReference<>(listener);
    QueueChangeListener weak = new WeakQueueChangeListener(listener);

    listener = null;
    System.gc();
    assertNull(weakListener.get());
  }
}
