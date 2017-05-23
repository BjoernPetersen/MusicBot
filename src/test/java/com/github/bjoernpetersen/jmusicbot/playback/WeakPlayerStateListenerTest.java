package com.github.bjoernpetersen.jmusicbot.playback;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class WeakPlayerStateListenerTest {

  @Test
  void onChangeIsCalled() {
    AtomicReference<PlayerState> called = new AtomicReference<>();
    PlayerStateListener listener = called::set;
    PlayerStateListener weak = new WeakPlayerStateListener(listener);

    assertNull(called.get());
    PlayerState state = PlayerState.stop();
    weak.onChanged(state);
    assertSame(state, called.get());
  }

  @Test
  void isWeak() throws InterruptedException {
    PlayerStateListener listener = new DummyListener();
    WeakReference<PlayerStateListener> weakListener = new WeakReference<>(listener);
    PlayerStateListener weak = new WeakPlayerStateListener(listener);

    listener = null;
    System.gc();
    assertNull(weakListener.get());
  }

  private static final class DummyListener implements PlayerStateListener {

    @Override
    public void onChanged(@Nonnull PlayerState state) {
    }
  }
}
