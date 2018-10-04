package com.github.bjoernpetersen.jmusicbot.playback;

import javax.annotation.Nonnull;

enum DoneCall {
  MarkDone(AbstractPlayback::markDone), Close(AbstractPlayback::close);

  private final Callable<AbstractPlayback> call;

  DoneCall(Callable<AbstractPlayback> call) {
    this.call = call;
  }

  public void call(AbstractPlayback playback) throws Exception {
    call.call(playback);
  }

  interface Callable<T> {

    void call(@Nonnull T t) throws Exception;
  }
}

