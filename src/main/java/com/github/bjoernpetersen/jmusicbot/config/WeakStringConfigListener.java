package com.github.bjoernpetersen.jmusicbot.config;

import java.lang.ref.WeakReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class WeakStringConfigListener implements StringConfigListener {

  private final WeakReference<StringConfigListener> listener;

  public WeakStringConfigListener(@Nonnull StringConfigListener listener) {
    this.listener = new WeakReference<>(listener);
  }

  @Override
  public void onChange(@Nullable String before, @Nullable String after) {
    StringConfigListener listener = this.listener.get();
    if (listener != null) {
      listener.onChange(before, after);
    }
  }
}
