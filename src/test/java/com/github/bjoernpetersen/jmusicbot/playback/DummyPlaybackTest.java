package com.github.bjoernpetersen.jmusicbot.playback;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DummyPlaybackTest {

  private DummyPlayback instance;

  @BeforeEach
  void init() {
    instance = DummyPlayback.getInstance();
  }

  @Test
  void getInstanceNotNull() {
    assertNotNull(DummyPlayback.getInstance());
  }

  @Test
  void pause() {
    assertTimeout(ofMillis(50), () -> instance.pause());
  }

  @Test
  void play() {
    assertTimeout(ofMillis(50), () -> instance.play());
  }

  @Test
  void close() throws Exception {
    instance.close();
  }

  @Test
  void waitForFinish() {
    assertTimeout(ofSeconds(3), () -> instance.waitForFinish());
  }
}
