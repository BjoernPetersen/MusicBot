package com.github.bjoernpetersen.jmusicbot.playback;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class WeakPlayerStateListener implements PlayerStateListener {

  @Nonnull
  private final PlayerStateListener listener;

  public WeakPlayerStateListener(PlayerStateListener listener) {
    this.listener = listener;
  }

  @Override
  public void onChange(@Nonnull PlayerState old, @Nonnull PlayerState next) {
    listener.onChange(old, next);
  }

  @Override
  public void onChanged(@Nonnull PlayerState state) {
    listener.onChanged(state);
  }
}
