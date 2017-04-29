package com.github.bjoernpetersen.jmusicbot.playback;

/**
 * A dummy implementation of Playback, only used by {@link Player}.
 */
final class DummyPlayback implements Playback {

  private static final DummyPlayback INSTANCE = new DummyPlayback();

  public static DummyPlayback getInstance() {
    return INSTANCE;
  }

  @Override
  public void play() {
  }

  @Override
  public void pause() {
  }

  @Override
  public void waitForFinish() throws InterruptedException {
  }

  @Override
  public void close() throws Exception {
  }
}
