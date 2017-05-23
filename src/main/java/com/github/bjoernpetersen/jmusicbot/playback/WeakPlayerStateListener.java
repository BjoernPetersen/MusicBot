package com.github.bjoernpetersen.jmusicbot.playback;

import java.lang.ref.WeakReference;
import javax.annotation.Nonnull;

public final class WeakPlayerStateListener implements PlayerStateListener {

  @Nonnull
  private final WeakReference<PlayerStateListener> listener;

  public WeakPlayerStateListener(@Nonnull PlayerStateListener listener) {
    this.listener = new WeakReference<>(listener);
  }

  @Override
  public void onChanged(@Nonnull PlayerState state) {
    PlayerStateListener listener = this.listener.get();
    if (listener != null) {
      listener.onChanged(state);
    }
  }
}
